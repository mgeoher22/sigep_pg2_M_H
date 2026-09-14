package com.fincahernandez.gestionpecuaria.data.remote

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SyncCoordinatorTest {
    private val key=SyncKey("animales","a")
    private fun data(name:String="A")=mapOf<String,Any?>("id" to "a","nombre" to name)
    private class Memory(var rows:Map<SyncKey,SyncData>):SyncStore {
        var bases:Map<SyncKey,SyncVersion> = emptyMap()
        var pending:SyncOperation?=null
        override suspend fun read(readable:Set<String>)=SyncLocalState(rows,bases,pending)
        override suspend fun prepare(local:SyncLocalState,plan:SyncPlan,operation:SyncOperation?,validate:()->Unit) {
            validate();check(local.rows==rows)
            rows=rows.toMutableMap().also { map -> plan.pulls.forEach { (k,r)->if(r.deleted)map.remove(k) else map[k]=r.data } }
            bases=bases+plan.bases;pending=operation
        }
        override suspend fun acknowledge(operation:SyncOperation,versions:Map<SyncKey,Long>) {
            operation.changes.forEach { change ->
                rows[change.key]?.let { current ->
                    rows=rows+(change.key to (change.data+current.filter { (k,v)->v!=change.original[k] }))
                }
                bases=bases+(change.key to SyncVersion(change.data,versions.getValue(change.key)))
            }
            pending=null
        }
        override suspend fun rejected(operation:SyncOperation){pending=null}
    }
    private class Server:SyncGateway {
        var rows:Map<SyncKey,SyncVersion> = emptyMap()
        val receipts=mutableMapOf<String,Map<SyncKey,Long>>()
        val calls=mutableListOf<SyncOperation>()
        var loseResponse=false
        var reject=false
        var owner="user"
        var duringSend:(()->Unit)?=null
        override suspend fun read()=SyncRemoteState(rows,owner,setOf("animales"),"stamp")
        override fun validate(state:SyncRemoteState){}
        override suspend fun send(operation:SyncOperation,state:SyncRemoteState):Map<SyncKey,Long> {
            calls+=operation
            if(reject)throw SyncRejected("Rechazado")
            val receipt=receipts.getOrPut(operation.id) {
                operation.changes.associate { change ->
                    val version=(rows[change.key]?.version?:0)+1
                    rows=rows+(change.key to SyncVersion(change.data,version))
                    change.key to version
                }
            }
            duringSend?.invoke();duringSend=null
            if(loseResponse){loseResponse=false;throw IOException("Respuesta perdida")}
            return receipt
        }
    }
    @Test fun lostResponseReplaysIdenticalUuidAndPayloadWithoutDuplicate()=runBlocking {
        val local=Memory(mapOf(key to data()));val server=Server();server.loseResponse=true
        try {SyncCoordinator(local,server).run();fail()} catch(_:IOException){}
        val saved=local.pending!!
        assertEquals(1L,server.rows.getValue(key).version)
        SyncCoordinator(local,server).run()
        assertEquals(saved,server.calls[1]);assertEquals(1L,server.rows.getValue(key).version);assertNull(local.pending)
    }
    @Test fun changesMadeDuringUploadStayPending()=runBlocking {
        val local=Memory(mapOf(key to data()));val server=Server()
        server.duringSend={local.rows=mapOf(key to data("Editado durante envío"))}
        val result=SyncCoordinator(local,server).run()
        assertEquals("Editado durante envío",local.rows.getValue(key)["nombre"])
        assertEquals("A",server.rows.getValue(key).data["nombre"]);assertEquals(1,result.pending)
        SyncCoordinator(local,server).run()
        assertEquals("Editado durante envío",server.rows.getValue(key).data["nombre"])
    }
    @Test fun disjointMergeDoesNotUndoRemoteFieldOnReadback()=runBlocking {
        val original=data()+mapOf("estado" to "SANO")
        val local=Memory(mapOf(key to (original+("nombre" to "Local"))))
        local.bases=mapOf(key to SyncVersion(original,1))
        val server=Server();server.rows=mapOf(key to SyncVersion(original+("estado" to "REGULAR"),2))
        val result=SyncCoordinator(local,server).run()
        assertEquals(0,result.pending);assertEquals("REGULAR",local.rows.getValue(key)["estado"])
        assertEquals("Local",server.rows.getValue(key).data["nombre"])
    }
    @Test fun pendingOperationCannotBeSentByAnotherAccount()=runBlocking {
        val local=Memory(mapOf(key to data()));val server=Server();server.loseResponse=true
        try {SyncCoordinator(local,server).run()}catch(_:IOException){}
        server.owner="another"
        try {SyncCoordinator(local,server).run();fail()}catch(_:IllegalArgumentException){}
        assertEquals(1,server.calls.size);assertNotNull(local.pending)
    }
    @Test fun definitiveRejectionKeepsLocalDataAndAllowsReplan()=runBlocking {
        val local=Memory(mapOf(key to data()));val server=Server();server.reject=true
        try {SyncCoordinator(local,server).run();fail()}catch(_:SyncRejected){}
        assertEquals(data(),local.rows[key]);assertNull(local.pending);assertTrue(local.bases.isEmpty())
    }
    @Test fun conflictsDoNotUploadOrOverwriteAnything()=runBlocking {
        val local=Memory(mapOf(key to data("Local")));val server=Server();server.rows=mapOf(key to SyncVersion(data("Remoto"),1))
        val result=SyncCoordinator(local,server).run()
        assertEquals(1,result.conflicts.size);assertTrue(server.calls.isEmpty());assertEquals("Local",local.rows.getValue(key)["nombre"])
    }
    @Test fun moreThan100ChangesStopsBeforeSavingOrSending()=runBlocking {
        val local=Memory((1..101).associate { SyncKey("animales","$it") to data() });val server=Server()
        try {SyncCoordinator(local,server).run();fail()}catch(_:IllegalArgumentException){}
        assertTrue(server.calls.isEmpty());assertNull(local.pending)
    }
}

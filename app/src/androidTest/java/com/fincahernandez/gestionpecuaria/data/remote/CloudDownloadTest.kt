package com.fincahernandez.gestionpecuaria.data.remote

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudDownloadTest {
    private val id="00000000-0000-4000-8000-000000000001"
    private val other="00000000-0000-4000-8000-000000000002"
    private val permissions=setOf("animals","lots","weighings","health","finance","employees")
    private val account=CloudAccount("test@example.com",id,id,"Administrador General",permissions)
    private fun row(table:CloudTable)=JSONObject().also { j ->
        table.columns.forEach { f -> j.put(f.name,when {
            f.nullable -> JSONObject.NULL
            f.name=="id" && f.kind=="Int" -> 1
            f.name=="id" || f.name.endsWith("Id") -> id
            f.kind=="Boolean" -> true
            f.kind=="Double" -> 100.25
            f.kind=="Int" -> 1
            f.kind=="Long" -> 1700000000000L
            else -> "PRUEBA"
        }) }
    }
    private fun db()=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,GestionPecuariaDatabase::class.java).build()
    private fun count(db:GestionPecuariaDatabase,table:String)=db.openHelper.readableDatabase.query("SELECT count(*) FROM $table").use {it.moveToFirst();it.getInt(0)}
    private suspend fun fails(block:suspend()->Unit) {var failed=false;try{block()}catch(_:Exception){failed=true};assertTrue("Debe rechazar y revertir",failed)}
    @Test fun allTablesImportAndRepeatWithoutDuplicates()=runBlocking {
        val db=db()
        try {
            val snapshot=CloudDownloadSnapshot(CloudTables.all.associate {it.name to listOf(row(it))},account,0)
            val importer=CloudDataImporter(db)
            assertEquals(11,importer.apply(snapshot).added)
            assertEquals(0,importer.apply(snapshot).added)
            CloudTables.all.forEach {assertEquals(1,count(db,it.name))}
            assertEquals(0,count(db,"usuarios"))
        }finally{db.close()}
    }
    @Test fun localEditsAndPhotosArePreserved()=runBlocking {
        val db=db()
        try {
            val animal=row(CloudTables.all.first())
            val snapshot=CloudDownloadSnapshot(mapOf("animales" to listOf(animal)),account,0)
            val importer=CloudDataImporter(db);importer.apply(snapshot)
            db.openHelper.writableDatabase.execSQL("UPDATE animales SET nombre='Nombre local',fotoUri='content://foto-local'")
            animal.put("nombre","Nombre remoto").put("fotoUri","foto-remota.jpg")
            val result=importer.apply(snapshot)
            assertEquals(1,result.different);assertEquals(0,result.added)
            db.openHelper.readableDatabase.query("SELECT nombre,fotoUri FROM animales").use {it.moveToFirst();assertEquals("Nombre local",it.getString(0));assertEquals("content://foto-local",it.getString(1))}
        }finally{db.close()}
    }
    @Test fun invalidForeignKeyRollsBackEveryTable()=runBlocking {
        val db=db()
        try {
            val animal=row(CloudTables.all.first())
            val weight=row(CloudTables.all.first {it.name=="pesajes"}).put("animalId",other)
            fails {CloudDataImporter(db).apply(CloudDownloadSnapshot(mapOf("animales" to listOf(animal),"pesajes" to listOf(weight)),account,0))}
            assertEquals(0,count(db,"animales"));assertEquals(0,count(db,"pesajes"))
        }finally{db.close()}
    }
    @Test fun duplicateCodesRollBackAndUnauthorizedTablesAreRejected()=runBlocking {
        val db=db()
        try {
            val a=row(CloudTables.all.first());val b=JSONObject(a.toString()).put("id",other)
            fails {CloudDataImporter(db).apply(CloudDownloadSnapshot(mapOf("animales" to listOf(a,b)),account,0))}
            assertEquals(0,count(db,"animales"))
            fails {CloudDataImporter(db).apply(CloudDownloadSnapshot(mapOf("animales" to listOf(a)),account.copy(permissions=setOf("finance")),0))}
            assertEquals(0,count(db,"animales"))
        }finally{db.close()}
    }
    private class Memory:CloudSessionStore {
        var text:String?=null
        override fun read()=text
        override fun write(value:String){text=value}
        override fun clear(){text=null}
    }
    private fun member()="""[{"usuarioId":"$id","activo":true,"usuarioLocalId":null,"nombreCompleto":"Nombre usuario","rol":"Administrador General","permisosPersonalizados":"animals"}]"""
    private fun manager(call:(String,String,Map<String,String>,String?)->String)=CloudSessionManager(Memory(),"https://test.supabase.co","sb_publishable_test",SupabaseConnectionProbe.Transport {u,m,h,b -> when {
        u.contains("grant_type=password") -> """{"access_token":"test","refresh_token":"refresh","expires_at":9999999999,"user":{"id":"$id","email":"test@example.com"}}"""
        u.contains("miembros_finca") -> member()
        else -> call(u,m,h,b)
    }})
    @Test fun shortPagesStillContinueUntilEmptyAndUseBearer()=runBlocking {
        var pages=0
        val m=manager {url,method,headers,_ ->
            assertEquals("Bearer test",headers["Authorization"])
            when {
                url.contains("estado_sincronizacion") -> """{"cursor":10,"versionEsquema":"20260914_room16_partos"}"""
                url.contains("/animales?") -> {assertEquals("GET",method);pages++;when(pages){1->"""[{"id":"$id"}]""";2->{assertTrue(url.contains("id=gt.$id"));"""[{"id":"$other"}]"""};else->"[]"}}
                else -> "[]"
            }
        }
        assertEquals("Nombre usuario",m.signInCloud("test@example.com","123456").displayName)
        val data=m.download(id)
        assertEquals(3,pages);assertEquals(2,data.rows.getValue("animales").size)
        assertFalse(data.rows.containsKey("empleados"))
        m.clearLocalSession();fails {m.validateSnapshot(data)}
    }
    @Test fun movingCloudCursorRejectsDownload()=runBlocking {
        var cursor=0
        val m=manager {url,_,_,_->if(url.contains("estado_sincronizacion")) """{"cursor":${cursor++},"versionEsquema":"20260914_room16_partos"}""" else "[]"}
        m.signInCloud("test@example.com","123456")
        fails {m.download(id)}
    }
    @Test fun profileUpdateUsesOwnMetadataAndRestoresName()=runBlocking {
        val m=manager {url,method,_,body->
            assertTrue(url.endsWith("/auth/v1/user"));assertEquals("PUT",method)
            assertEquals("Ana Pérez",JSONObject(body!!).getJSONObject("data").getString("full_name"))
            """{"id":"$id","user_metadata":{"full_name":"Ana Pérez"}}"""
        }
        m.signInCloud("test@example.com","123456")
        m.updateDisplayName(id,"Ana Pérez")
        assertEquals("Ana Pérez",m.restore(id)!!.displayName)
    }
}

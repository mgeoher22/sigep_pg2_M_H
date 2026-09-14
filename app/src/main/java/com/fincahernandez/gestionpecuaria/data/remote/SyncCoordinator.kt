package com.fincahernandez.gestionpecuaria.data.remote

import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SyncOperation(val id: String, val owner: String, val changes: List<SyncChange>)
data class SyncLocalState(val rows: Map<SyncKey, SyncData>, val baseline: Map<SyncKey, SyncVersion>, val pending: SyncOperation?)
data class SyncRemoteState(val rows: Map<SyncKey, SyncVersion>, val owner: String, val readable: Set<String>, val stamp: Any)
data class SyncResult(val sent: Int, val received: Int, val pending: Int, val conflicts: List<SyncConflict> = emptyList())

interface SyncGateway {
    suspend fun read(): SyncRemoteState
    suspend fun send(operation: SyncOperation, state: SyncRemoteState): Map<SyncKey, Long>
    fun validate(state: SyncRemoteState)
}
interface SyncStore {
    suspend fun read(readable: Set<String>): SyncLocalState
    /** Compare-and-apply: pulls, baselines and durable outbox share one transaction. */
    suspend fun prepare(local: SyncLocalState, plan: SyncPlan, operation: SyncOperation?, validate: () -> Unit)
    suspend fun acknowledge(operation: SyncOperation, versions: Map<SyncKey, Long>)
    suspend fun rejected(operation: SyncOperation)
}
class SyncRejected(message: String): Exception(message)

class SyncCoordinator(private val store: SyncStore, private val gateway: SyncGateway) {
    companion object { private val mutex = Mutex() }
    suspend fun run(choices: List<SyncChoice> = emptyList()): SyncResult = mutex.withLock {
        var remote = gateway.read()
        var local = store.read(remote.readable)
        var sent = 0
        suspend fun send(operation: SyncOperation) {
            require(operation.owner == remote.owner) { "Hay un envío pendiente de otra cuenta. Ingresa con esa cuenta para completarlo." }
            gateway.validate(remote)
            val versions = try { gateway.send(operation, remote) }
            catch (e: SyncRejected) { store.rejected(operation); throw e }
            require(versions.keys == operation.changes.map { it.key }.toSet()) { "La nube no confirmó todos los registros. El envío queda pendiente." }
            store.acknowledge(operation, versions)
            sent += operation.changes.size
        }
        // Retry the exact saved payload and UUID before building another operation.
        local.pending?.let {
            send(it)
            remote = gateway.read(); local = store.read(remote.readable)
        }
        val plan = SyncPlanner.plan(local.rows, remote.rows, local.baseline, choices)
        if (plan.conflicts.isNotEmpty()) return@withLock SyncResult(sent, 0, plan.uploads.size, plan.conflicts)
        require(plan.uploads.size <= 100) { "Hay más de 100 cambios pendientes. Esta primera versión necesita dividir esa carga antes de enviarla; tus datos siguen guardados." }
        val operation = plan.uploads.takeIf { it.isNotEmpty() }?.let { SyncOperation(UUID.randomUUID().toString(), remote.owner, it) }
        store.prepare(local, plan, operation) { gateway.validate(remote) }
        var received = plan.pulls.size
        if (operation != null) {
            send(operation)
            // Read back trigger-generated values and edits from other devices.
            remote = gateway.read(); local = store.read(remote.readable)
            val finalPlan = SyncPlanner.plan(local.rows, remote.rows, local.baseline)
            if (finalPlan.conflicts.isNotEmpty()) return@withLock SyncResult(sent, received, finalPlan.uploads.size, finalPlan.conflicts)
            store.prepare(local, finalPlan, null) { gateway.validate(remote) }
            received += finalPlan.pulls.size
            return@withLock SyncResult(sent, received, finalPlan.uploads.size)
        }
        SyncResult(sent, received, 0)
    }
}

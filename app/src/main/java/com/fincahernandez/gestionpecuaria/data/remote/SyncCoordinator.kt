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
    companion object {
        private val mutex = Mutex()
        private const val MAX_CHANGES_PER_REQUEST = 100
        private const val MAX_BATCHES_PER_RUN = 500
    }
    suspend fun run(choices: List<SyncChoice> = emptyList()): SyncResult = mutex.withLock {
        var remote = gateway.read()
        var local = store.read(remote.readable)
        var sent = 0
        var received = 0
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
        var firstPlan = true
        repeat(MAX_BATCHES_PER_RUN) {
            val plan = SyncPlanner.plan(
                local = local.rows,
                remote = remote.rows,
                baseline = local.baseline,
                choices = if (firstPlan) choices else emptyList()
            )
            firstPlan = false
            if (plan.conflicts.isNotEmpty()) {
                return@withLock SyncResult(sent, received, plan.uploads.size, plan.conflicts)
            }
            val batch = plan.uploads.take(MAX_CHANGES_PER_REQUEST)
            val operation = batch.takeIf { it.isNotEmpty() }?.let {
                SyncOperation(UUID.randomUUID().toString(), remote.owner, it)
            }
            store.prepare(local, plan.copy(uploads = batch), operation) {
                gateway.validate(remote)
            }
            received += plan.pulls.size
            if (operation == null) return@withLock SyncResult(sent, received, 0)

            send(operation)
            // Cada confirmación libera la bandeja durable antes de preparar el siguiente lote.
            remote = gateway.read()
            local = store.read(remote.readable)
        }
        error("La sincronización excede $MAX_BATCHES_PER_RUN envíos consecutivos; los cambios restantes siguen guardados.")
    }
}

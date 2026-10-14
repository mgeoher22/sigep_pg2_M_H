package com.fincahernandez.gestionpecuaria.data.remote

import android.content.Context
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase

class AndroidSyncGateway(
    private val manager: CloudSessionManager,
    private val localId: String,
    context: Context? = null,
    database: GestionPecuariaDatabase? = null
): SyncGateway {
    private val photoSync = if (context != null && database != null) {
        AnimalPhotoCloudSync(context, database, manager, localId)
    } else null

    override suspend fun read(): SyncRemoteState {
        val snapshot = manager.download(localId, forSync = true)
        // La descarga valida primero que Supabase tenga la migración requerida.
        // Solo entonces se suben archivos y se actualiza la referencia local.
        photoSync?.uploadPendingPhotos()
        val rows = linkedMapOf<SyncKey, SyncVersion>()
        snapshot.rows.forEach { (table, records) -> records.forEach { record ->
            val data = SyncCodec.data(table, record)
            val key = SyncKey(table, data.getValue("id").toString())
            rows[key] = SyncVersion(data, record.getLong("syncVersion"), record.getBoolean("syncEliminado"))
        } }
        return SyncRemoteState(rows, snapshot.account.userId, snapshot.rows.keys, snapshot)
    }
    override suspend fun send(operation: SyncOperation, state: SyncRemoteState) =
        manager.sendSync(operation, state.stamp as CloudDownloadSnapshot)
    override fun validate(state: SyncRemoteState) = manager.validateSnapshot(state.stamp as CloudDownloadSnapshot)
}

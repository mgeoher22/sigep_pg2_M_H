package com.fincahernandez.gestionpecuaria.data.remote

class AndroidSyncGateway(private val manager: CloudSessionManager, private val localId: String): SyncGateway {
    override suspend fun read(): SyncRemoteState {
        val snapshot = manager.download(localId, forSync = true)
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

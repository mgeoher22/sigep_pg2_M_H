package com.fincahernandez.gestionpecuaria.data.remote

import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RoomSyncStore(private val database: GestionPecuariaDatabase, private val project: String): SyncStore {
    private fun readRow(db: SupportSQLiteDatabase, key: SyncKey): SyncData? {
        val table = CloudTables.all.first { it.name == key.table }
        db.query("SELECT * FROM ${table.name} WHERE id=?", arrayOf(key.id)).use { c ->
            if (!c.moveToFirst()) return null
            return table.columns.filter { it.name != "fotoUri" }.associate { f ->
                val i = c.getColumnIndexOrThrow(f.name)
                f.name to if (c.isNull(i)) null else when (f.kind) {
                    "String" -> c.getString(i)
                    "Boolean" -> c.getLong(i) != 0L
                    "Double" -> c.getDouble(i)
                    else -> c.getLong(i)
                }
            }
        }
    }
    override suspend fun read(readable: Set<String>): SyncLocalState = withContext(Dispatchers.IO) {
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            val rows = linkedMapOf<SyncKey, SyncData>()
            CloudTables.all.filter { it.name in readable }.forEach { table ->
                db.query("SELECT id FROM ${table.name}").use { c -> while (c.moveToNext()) {
                    val key = SyncKey(table.name, c.getString(0)); rows[key] = readRow(db, key)!!
                } }
            }
            val bases = linkedMapOf<SyncKey, SyncVersion>()
            db.query("SELECT tableName,recordId,payload,version,deleted FROM cloud_sync_baseline WHERE project=?", arrayOf(project)).use { c ->
                while (c.moveToNext()) if (c.getString(0) in readable) {
                    val key = SyncKey(c.getString(0), c.getString(1))
                    bases[key] = SyncVersion(SyncCodec.data(key.table, JSONObject(c.getString(2))), c.getLong(3), c.getInt(4) != 0)
                }
            }
            var pending: SyncOperation? = null
            db.query("SELECT operationId,owner,payload FROM cloud_sync_pending WHERE project=?", arrayOf(project)).use { c ->
                if (c.moveToFirst()) pending = SyncCodec.operation(c.getString(0), c.getString(1), c.getString(2))
            }
            SyncLocalState(rows, bases, pending)
        }
    }
    private fun writeRow(db: SupportSQLiteDatabase, key: SyncKey, data: SyncData) {
        val fields = CloudTables.all.first { it.name == key.table }.columns.filter { it.name != "fotoUri" }
        val values = fields.map { f -> when (val value = data[f.name]) { is Boolean -> if(value) 1L else 0L; else -> value } }
        if (readRow(db, key) == null) {
            db.execSQL("INSERT INTO ${key.table} (${fields.joinToString { "\"${it.name}\"" }}) VALUES (${fields.joinToString { "?" }})", values.toTypedArray())
        } else {
            db.execSQL("UPDATE ${key.table} SET ${fields.joinToString { "\"${it.name}\"=?" }} WHERE id=?", (values + key.id).toTypedArray())
        }
    }
    private fun baseline(db: SupportSQLiteDatabase, key: SyncKey, row: SyncVersion) {
        db.execSQL("INSERT OR REPLACE INTO cloud_sync_baseline(project,tableName,recordId,payload,version,deleted) VALUES(?,?,?,?,?,?)",
            arrayOf(project,key.table,key.id,SyncCodec.json(row.data).toString(),row.version,if(row.deleted)1 else 0))
    }
    private fun integrity(db: SupportSQLiteDatabase) {
        db.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "Faltan datos relacionados; no se aplicaron cambios locales." } }
        db.query("SELECT 1 FROM animales a LEFT JOIN animales m ON m.id=a.madreId WHERE a.madreId IS NOT NULL AND m.id IS NULL LIMIT 1").use {
            require(!it.moveToFirst()) { "Falta la madre de un animal; no se aplicaron cambios locales." }
        }
        db.query("SELECT animalId FROM lote_animales WHERE fechaSalida IS NULL GROUP BY animalId HAVING count(*)>1").use {
            require(!it.moveToFirst()) { "Un animal quedaría en dos lotes. Revisa sus asignaciones." }
        }
    }
    override suspend fun prepare(local: SyncLocalState, plan: SyncPlan, operation: SyncOperation?, validate: () -> Unit) = withContext(Dispatchers.IO) {
        // Validate payload size before committing any outbox entry.
        operation?.let(SyncCodec::request)
        database.withTransaction {
            validate()
            val db = database.openHelper.writableDatabase
            db.execSQL("PRAGMA defer_foreign_keys=ON")
            db.query("SELECT 1 FROM cloud_sync_pending WHERE project=?", arrayOf(project)).use { require(!it.moveToFirst()) { "Hay un envío pendiente por confirmar." } }
            val touched = plan.bases.keys + plan.pulls.keys + plan.uploads.map { it.key }
            touched.forEach { key -> require(readRow(db, key) == local.rows[key]) { "Se guardaron cambios durante la sincronización. Pulsa Sincronizar otra vez." } }
            val order = CloudTables.all.mapIndexed { i,t -> t.name to i }.toMap()
            plan.pulls.entries.filter { it.value.deleted }.sortedByDescending { order[it.key.table] }.forEach { (key, _) ->
                if(key.table == "lotes") db.query(
                    """
                    SELECT 1 FROM pesajes WHERE loteId=?
                    UNION ALL SELECT 1 FROM eventos_sanitarios WHERE loteIdReferencia=?
                    UNION ALL SELECT 1 FROM movimientos_financieros WHERE loteId=?
                    UNION ALL SELECT 1 FROM pagos_empleados WHERE loteId=?
                    UNION ALL SELECT 1 FROM movimientos_insumos WHERE loteId=?
                    UNION ALL SELECT 1 FROM asignaciones_insumos WHERE loteId=?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(key.id,key.id,key.id,key.id,key.id,key.id)).use {
                    require(!it.moveToFirst()) { "El lote anulado conserva registros relacionados. No se eliminará su historial local." }
                }
                db.execSQL("DELETE FROM ${key.table} WHERE id=?", arrayOf(key.id))
            }
            plan.pulls.entries.filter { !it.value.deleted }.sortedBy { order[it.key.table] }.forEach { (key,row) -> writeRow(db,key,row.data) }
            plan.bases.forEach { (key,row) -> baseline(db,key,row) }
            if(operation != null) db.execSQL("INSERT INTO cloud_sync_pending(project,operationId,owner,payload) VALUES(?,?,?,?)",
                arrayOf(project,operation.id,operation.owner,SyncCodec.operation(operation)))
            integrity(db);validate();coroutineContext.ensureActive()
        }
        database.invalidationTracker.refreshVersionsAsync()
    }
    override suspend fun acknowledge(operation: SyncOperation, versions: Map<SyncKey, Long>) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            db.execSQL("PRAGMA defer_foreign_keys=ON")
            db.query("SELECT operationId FROM cloud_sync_pending WHERE project=?", arrayOf(project)).use {
                require(it.moveToFirst() && it.getString(0)==operation.id) { "El envío pendiente cambió." }
            }
            operation.changes.forEach { change ->
                // Preserve edits made while the network request was in flight.
                val current = readRow(db,change.key)
                if(current != null) {
                    val merged = change.data.toMutableMap()
                    current.forEach { (field,value) -> if(value != change.original[field]) merged[field]=value }
                    if (merged != current) writeRow(db,change.key,merged)
                }
                baseline(db,change.key,SyncVersion(change.data,versions.getValue(change.key)))
            }
            db.execSQL("DELETE FROM cloud_sync_pending WHERE project=? AND operationId=?", arrayOf(project,operation.id))
            integrity(db);coroutineContext.ensureActive()
        }
        database.invalidationTracker.refreshVersionsAsync()
    }
    override suspend fun rejected(operation: SyncOperation) = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.openHelper.writableDatabase.execSQL("DELETE FROM cloud_sync_pending WHERE project=? AND operationId=?", arrayOf(project,operation.id))
        }
    }
}

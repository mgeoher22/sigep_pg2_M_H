package com.fincahernandez.gestionpecuaria.data.remote

import android.database.Cursor
import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CloudDownloadSnapshot(val rows: Map<String,List<JSONObject>>, val account: CloudAccount, val generation: Long)
data class CloudImportResult(val added: Int, val existing: Int, val different: Int, val photosPending: Int)

/** Descarga inicial aditiva: nunca reemplaza ni elimina registros locales. */
class CloudDataImporter(private val database: GestionPecuariaDatabase) {
    suspend fun apply(snapshot: CloudDownloadSnapshot, validateSession: () -> Unit = {}): CloudImportResult = withContext(Dispatchers.IO) {
        val allowed=CloudTables.readable(snapshot.account.permissions).map { it.name }.toSet()
        require(snapshot.rows.keys.all { it in allowed }) { "La descarga contiene una tabla no autorizada." }
        var added=0;var existing=0;var different=0;var photos=0
        database.withTransaction {
            validateSession()
            val db=database.openHelper.writableDatabase
            db.execSQL("PRAGMA defer_foreign_keys = ON")
            for(table in CloudTables.all) {
                val records=snapshot.rows[table.name] ?: continue
                val ids=mutableSetOf<String>()
                for(row in records) {
                    coroutineContext.ensureActive()
                    val values=table.columns.map { field ->
                        require(row.has(field.name)) { "Falta un campo en la descarga: ${table.name}.${field.name}" }
                        if(row.isNull(field.name)) {
                            require(field.nullable) { "La nube contiene un campo obligatorio vacío." };null
                        } else when(field.kind) {
                            "String" -> row.get(field.name).also { require(it is String) }
                            "Boolean" -> { val v=row.get(field.name);require(v is Boolean);if(v) 1L else 0L }
                            "Long", "Int" -> {
                                val v=row.get(field.name);require(v is Int || v is Long) { "Fecha o identificador numérico no válido." }
                                (v as Number).toLong().also { if(field.kind=="Int") require(it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) }
                            }
                            "Double" -> {val v=row.get(field.name);require(v is Number);v.toDouble().also {require(it.isFinite())}}
                            else -> error("Tipo no admitido")
                        }
                    }.toMutableList()
                    val id=values[table.columns.indexOfFirst { it.name=="id" }]!!.toString()
                    require(ids.add(id)) { "La descarga repite un identificador." }
                    val photo=table.columns.indexOfFirst { it.name=="fotoUri" }
                    if(photo>=0) { if(values[photo]!=null)photos++;values[photo]=null }
                    val columns=table.columns.joinToString { "\"${it.name}\"" }
                    var found=false
                    db.query("SELECT $columns FROM ${table.name} WHERE id = ?",arrayOf(id)).use { cursor ->
                        if(cursor.moveToFirst()) {
                            found=true;existing++
                            val changed=values.indices.any { i -> i!=photo && !same(cursor,i,values[i],table.columns[i].kind) }
                            if(changed)different++
                        }
                    }
                    if(!found) {
                        val placeholders=values.joinToString { "?" }
                        // ABORT on unique-code conflicts: never silently discard or replace another ID.
                        db.execSQL("INSERT INTO ${table.name} ($columns) VALUES ($placeholders)",values.toTypedArray())
                        added++
                    }
                }
            }
            db.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "Faltan registros relacionados; no se aplicó la descarga." } }
            db.query("SELECT animalId FROM lote_animales WHERE fechaSalida IS NULL GROUP BY animalId HAVING count(*) > 1 LIMIT 1").use {
                require(!it.moveToFirst()) { "Un animal quedaría en dos lotes abiertos. No se aplicó la descarga." }
            }
            validateSession()
            coroutineContext.ensureActive()
        }
        database.invalidationTracker.refreshVersionsAsync()
        CloudImportResult(added,existing,different,photos)
    }
    private fun same(c:Cursor,index:Int,value:Any?,kind:String):Boolean {
        if(c.isNull(index))return value==null
        if(value==null)return false
        return when(kind) {
            "Double" -> c.getDouble(index)==(value as Number).toDouble()
            "Long","Int","Boolean" -> c.getLong(index)==(value as Number).toLong()
            else -> c.getString(index)==value
        }
    }
}

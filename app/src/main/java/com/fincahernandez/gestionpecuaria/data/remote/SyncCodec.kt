package com.fincahernandez.gestionpecuaria.data.remote

import org.json.JSONArray
import org.json.JSONObject

/** Local image URIs never leave the device, nor overwrite remote photo paths. */
object SyncCodec {
    fun data(table: String, json: JSONObject): SyncData {
        val columns = CloudTables.all.first { it.name == table }.columns
        return columns.filter { it.name != "fotoUri" }.associate { field ->
            // Los baselines creados antes de Room 16 no incluían esta marca. Se interpretan
            // como false para poder migrar sin descartar el historial de sincronización.
            val legacyNearCalving = field.name == "proximaParto" && !json.has(field.name)
            require(json.has(field.name) || legacyNearCalving) {
                "Falta ${table}.${field.name} en la nube."
            }
            val value: Any? = if (legacyNearCalving) {
                false
            } else if (json.isNull(field.name)) {
                require(field.nullable) { "Campo obligatorio vacío en $table." }; null
            } else when (field.kind) {
                "String" -> json.get(field.name).also { require(it is String) }
                "Boolean" -> json.get(field.name).also { require(it is Boolean) }
                "Double" -> (json.get(field.name) as Number).toDouble().also { require(it.isFinite()) }
                else -> {
                    val raw = json.get(field.name)
                    require(raw is Int || raw is Long)
                    (raw as Number).toLong()
                }
            }
            field.name to value
        }
    }
    fun json(data: SyncData) = JSONObject().also { out -> data.forEach { (key, value) -> out.put(key, value ?: JSONObject.NULL) } }
    fun operation(op: SyncOperation): String = JSONArray().also { list ->
        op.changes.forEach { c -> list.put(JSONObject().put("tabla", c.key.table).put("id", c.key.id)
            .put("registro", json(c.data)).put("original", json(c.original)).put("versionEsperada", c.expected)) }
    }.toString()
    fun operation(id: String, owner: String, text: String): SyncOperation {
        val list = JSONArray(text)
        return SyncOperation(id, owner, (0 until list.length()).map { index ->
            val c = list.getJSONObject(index); val table = c.getString("tabla")
            SyncChange(SyncKey(table, c.getString("id")), data(table, c.getJSONObject("registro")),
                c.getLong("versionEsperada"), data(table, c.getJSONObject("original")))
        })
    }
    fun request(op: SyncOperation): JSONObject {
        val order = CloudTables.all.mapIndexed { i, t -> t.name to i }.toMap()
        // Close old assignments/lots before opening new ones with unique occupancy.
        val changes = op.changes.sortedWith(compareBy<SyncChange> {
            if (it.key.table == "lote_animales" && it.data["fechaSalida"] != null) -2
            else if (it.key.table == "lotes" && it.data["estado"] != "ACTIVO") -1
            else order.getValue(it.key.table)
        }.thenBy { it.key.id })
        return JSONObject().put("p_operacion_id", op.id).put("p_cambios", JSONArray().also { array ->
            changes.forEach { c -> array.put(JSONObject().put("tabla", c.key.table)
                .put("registro", json(c.data)).put("versionEsperada", c.expected)) }
        }).also { require(it.toString().toByteArray(Charsets.UTF_8).size <= 900000) { "El envío supera el tamaño permitido; los datos siguen guardados." } }
    }
}

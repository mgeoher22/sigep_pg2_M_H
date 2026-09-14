package com.fincahernandez.gestionpecuaria.data.remote

typealias SyncData = Map<String, Any?>
data class SyncKey(val table: String, val id: String)
data class SyncVersion(val data: SyncData, val version: Long, val deleted: Boolean = false)
data class SyncChange(val key: SyncKey, val data: SyncData, val expected: Long, val original: SyncData = data)
data class SyncConflict(val key: SyncKey, val local: SyncData?, val remote: SyncVersion?, val reason: String)
data class SyncChoice(val conflict: SyncConflict, val useLocal: Boolean)
data class SyncPlan(
    val uploads: List<SyncChange>,
    val pulls: Map<SyncKey, SyncVersion>,
    val bases: Map<SyncKey, SyncVersion>,
    val conflicts: List<SyncConflict>
)

/** Three-way comparison: never infer the winner from the device clock. */
object SyncPlanner {
    private fun meaningful(data: SyncData) = data.filterKeys { it != "actualizadoEn" }
    fun same(a: SyncData, b: SyncData) = meaningful(a) == meaningful(b)

    fun plan(local: Map<SyncKey, SyncData>, remote: Map<SyncKey, SyncVersion>,
        baseline: Map<SyncKey, SyncVersion>, choices: List<SyncChoice> = emptyList()): SyncPlan {
        val uploads = mutableListOf<SyncChange>()
        val pulls = linkedMapOf<SyncKey, SyncVersion>()
        val bases = linkedMapOf<SyncKey, SyncVersion>()
        val conflicts = mutableListOf<SyncConflict>()
        fun receive(key: SyncKey, row: SyncVersion) {
            bases[key] = row
            if (if (row.deleted) local[key] != null else local[key] != row.data) pulls[key] = row
        }
        fun conflict(key: SyncKey, l: SyncData?, r: SyncVersion?, reason: String) {
            val item = SyncConflict(key, l, r, reason)
            // A choice applies only to the exact pair reviewed, not later edits.
            val choice = choices.firstOrNull { it.conflict == item }
            when {
                choice == null -> conflicts += item
                !choice.useLocal && r != null -> receive(key, r)
                choice.useLocal && l != null && r != null && !r.deleted -> {
                    // Creation time is immutable on the server.
                    val data = if ("creadoEn" in r.data) l + ("creadoEn" to r.data["creadoEn"]) else l
                    uploads += SyncChange(key, data, r.version, l)
                }
                else -> conflicts += item
            }
        }
        for (key in local.keys + remote.keys + baseline.keys) {
            val l = local[key]; val r = remote[key]; val b = baseline[key]
            when {
                r == null && b != null -> conflict(key, l, null, "El registro conocido ya no está disponible en la nube.")
                r == null && l != null -> uploads += SyncChange(key, l, 0)
                r == null -> Unit
                r.deleted && l == null -> bases[key] = r
                r.deleted && b != null && !b.deleted && l != null && same(l, b.data) -> receive(key, r)
                r.deleted -> conflict(key, l, r, "Se anuló en la nube y hay datos locales que revisar.")
                l == null && (b == null || b.deleted) -> receive(key, r)
                l == null -> conflict(key, null, r, "Falta en este dispositivo. Puedes recuperar la copia de la nube.")
                same(l, r.data) -> receive(key, r)
                b == null || b.deleted -> conflict(key, l, r, "Las copias son diferentes y aún no hay una versión común guardada.")
                same(l, b.data) -> receive(key, r)
                same(r.data, b.data) -> uploads += SyncChange(key, l, r.version)
                else -> {
                    val fields = l.keys + r.data.keys + b.data.keys
                    val localChanges = fields.filter { it != "actualizadoEn" && l[it] != b.data[it] }.toSet()
                    val remoteChanges = fields.filter { it != "actualizadoEn" && r.data[it] != b.data[it] }.toSet()
                    val overlap = localChanges.intersect(remoteChanges).any { l[it] != r.data[it] }
                    if (overlap) conflict(key, l, r, "El mismo dato cambió en ambos dispositivos.")
                    else {
                        val merged = r.data.toMutableMap()
                        localChanges.forEach { merged[it] = l[it] }
                        if ("actualizadoEn" in merged) merged["actualizadoEn"] = maxOf(
                            (l["actualizadoEn"] as? Number)?.toLong() ?: 0L,
                            (r.data["actualizadoEn"] as? Number)?.toLong() ?: 0L)
                        uploads += SyncChange(key, merged, r.version, l)
                    }
                }
            }
        }
        return SyncPlan(uploads, pulls, bases, conflicts)
    }
}

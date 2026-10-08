package com.fincahernandez.gestionpecuaria.data.remote

data class CloudColumn(val name: String, val kind: String, val nullable: Boolean)
data class CloudTable(val name: String, val columns: List<CloudColumn>)

/** Campos productivos de Room; se excluyen credenciales y metadatos exclusivos del servidor. */
object CloudTables {
    val all = listOf(
        CloudTable("animales", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("codigoIdentificacion", "String", false),
            CloudColumn("nombre", "String", true),
            CloudColumn("sexo", "String", false),
            CloudColumn("raza", "String", true),
            CloudColumn("fechaNacimiento", "Long", true),
            CloudColumn("fechaIngreso", "Long", false),
            CloudColumn("categoria", "String", false),
            CloudColumn("tipoOrigen", "String", false),
            CloudColumn("madreId", "String", true),
            CloudColumn("proximaParto", "Boolean", false),
            CloudColumn("procedencia", "String", true),
            CloudColumn("estadoSalud", "String", false),
            CloudColumn("estado", "String", false),
            CloudColumn("observaciones", "String", true),
            CloudColumn("fotoUri", "String", true),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("parcelas", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("codigo", "String", false),
            CloudColumn("nombre", "String", false),
            CloudColumn("areaHectareas", "Double", false),
            CloudColumn("tipoPastura", "String", false),
            CloudColumn("capacidadAnimales", "Int", true),
            CloudColumn("limitesGeoJson", "String", true),
            CloudColumn("estado", "String", false),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("lotes", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("codigo", "String", false),
            CloudColumn("nombre", "String", false),
            CloudColumn("descripcion", "String", true),
            CloudColumn("tipo", "String", false),
            CloudColumn("fechaCreacion", "Long", false),
            CloudColumn("fechaCierre", "Long", true),
            CloudColumn("estado", "String", false),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false),
            CloudColumn("parcelaNombre", "String", true),
            CloudColumn("pesoObjetivoLibras", "Double", true),
            CloudColumn("fechaSalidaEstimada", "Long", true),
            CloudColumn("precioVenta", "Double", true),
            CloudColumn("fechaVenta", "Long", true)
        )),
        CloudTable("lote_animales", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("animalId", "String", false),
            CloudColumn("loteId", "String", false),
            CloudColumn("fechaIngreso", "Long", false),
            CloudColumn("fechaSalida", "Long", true),
            CloudColumn("motivoSalida", "String", true),
            CloudColumn("observaciones", "String", true),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("pesajes", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("animalId", "String", false),
            CloudColumn("loteId", "String", true),
            CloudColumn("pesoLibras", "Double", false),
            CloudColumn("fechaPesaje", "Long", false),
            CloudColumn("tipoRegistro", "String", false),
            CloudColumn("observaciones", "String", true),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("eventos_sanitarios", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("animalId", "String", false),
            CloudColumn("loteIdReferencia", "String", true),
            CloudColumn("tipoEvento", "String", false),
            CloudColumn("fechaEvento", "Long", false),
            CloudColumn("diagnostico", "String", false),
            CloudColumn("insumoId", "String", true),
            CloudColumn("dosisMl", "Double", true),
            CloudColumn("medicamento", "String", true),
            CloudColumn("dosis", "String", true),
            CloudColumn("estadoSalud", "String", false),
            CloudColumn("proximoControl", "Long", true),
            CloudColumn("responsable", "String", true),
            CloudColumn("observaciones", "String", true),
            CloudColumn("creadoEn", "Long", false)
        )),
        CloudTable("configuracion_leche", listOf(
            CloudColumn("id", "Int", false),
            CloudColumn("precioPorLitro", "Double", false),
            CloudColumn("frecuenciaPago", "String", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("produccion_lechera", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("fecha", "Long", false),
            CloudColumn("litros", "Double", false),
            CloudColumn("precioPorLitro", "Double", false),
            CloudColumn("fechaPagoProgramada", "Long", false),
            CloudColumn("pagoConfirmadoEn", "Long", true),
            CloudColumn("observaciones", "String", true),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("movimientos_financieros", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("tipo", "String", false),
            CloudColumn("categoria", "String", false),
            CloudColumn("monto", "Double", false),
            CloudColumn("fecha", "Long", false),
            CloudColumn("loteId", "String", true),
            CloudColumn("observaciones", "String", true),
            CloudColumn("creadoEn", "Long", false)
        )),
        CloudTable("empleados", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("nombreCompleto", "String", false),
            CloudColumn("cargo", "String", false),
            CloudColumn("fechaIngreso", "Long", false),
            CloudColumn("salarioBase", "Double", false),
            CloudColumn("frecuenciaPago", "String", false),
            CloudColumn("sectorAsignado", "String", true),
            CloudColumn("telefono", "String", true),
            CloudColumn("activo", "Boolean", false),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("pagos_empleados", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("empleadoId", "String", false),
            CloudColumn("fechaPago", "Long", false),
            CloudColumn("monto", "Double", false),
            CloudColumn("periodo", "String", false),
            CloudColumn("loteId", "String", true),
            CloudColumn("actividad", "String", true),
            CloudColumn("observaciones", "String", true),
            CloudColumn("creadoEn", "Long", false)
        )),
        CloudTable("insumos", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("codigo", "String", false),
            CloudColumn("nombre", "String", false),
            CloudColumn("categoria", "String", false),
            CloudColumn("unidadMedida", "String", false),
            CloudColumn("contenidoMlPorUnidad", "Double", true),
            CloudColumn("existenciaMinima", "Double", false),
            CloudColumn("activo", "Boolean", false),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("existencias_insumos", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("insumoId", "String", false),
            CloudColumn("cantidadInicial", "Double", false),
            CloudColumn("cantidadDisponible", "Double", false),
            CloudColumn("costoUnitario", "Double", true),
            CloudColumn("fechaVencimiento", "Long", true),
            CloudColumn("fechaIngreso", "Long", false),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("asignaciones_insumos", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("insumoId", "String", false),
            CloudColumn("loteId", "String", true),
            CloudColumn("responsable", "String", false),
            CloudColumn("cantidad", "Double", false),
            CloudColumn("fechaAsignacion", "Long", false),
            CloudColumn("fechaDevolucion", "Long", true),
            CloudColumn("estado", "String", false),
            CloudColumn("observaciones", "String", true),
            CloudColumn("creadoEn", "Long", false),
            CloudColumn("actualizadoEn", "Long", false)
        )),
        CloudTable("movimientos_insumos", listOf(
            CloudColumn("id", "String", false),
            CloudColumn("operacionId", "String", false),
            CloudColumn("insumoId", "String", false),
            CloudColumn("existenciaId", "String", false),
            CloudColumn("tipo", "String", false),
            CloudColumn("cantidad", "Double", false),
            CloudColumn("costoUnitario", "Double", true),
            CloudColumn("fecha", "Long", false),
            CloudColumn("loteId", "String", true),
            CloudColumn("responsable", "String", true),
            CloudColumn("registradoPorUsuarioId", "String", true),
            CloudColumn("observaciones", "String", true),
            CloudColumn("creadoEn", "Long", false)
        ))
    )
    fun readable(permissions: Set<String>): List<CloudTable> = all.filter { table ->
        when (table.name) {
            "animales", "parcelas", "lotes", "lote_animales" -> permissions.any { it in setOf("animals","lots","weighings","health") }
            "pesajes" -> "weighings" in permissions
            "eventos_sanitarios" -> "health" in permissions
            "configuracion_leche", "produccion_lechera" -> permissions.any { it in setOf("weighings","finance") }
            "movimientos_financieros" -> "finance" in permissions
            // Insumos necesita los nombres activos para elegir al responsable, no los pagos.
            "empleados" -> permissions.any { it in setOf("employees", "supplies") }
            "pagos_empleados" -> "employees" in permissions
            "insumos", "existencias_insumos", "movimientos_insumos" ->
                permissions.any { it in setOf("supplies", "health") }
            "asignaciones_insumos" -> "supplies" in permissions
            else -> false
        }
    }
}

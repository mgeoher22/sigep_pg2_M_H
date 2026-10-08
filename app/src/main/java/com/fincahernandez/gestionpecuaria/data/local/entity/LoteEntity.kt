package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa un lote utilizado para agrupar animales.
 *
 * Un lote puede permanecer activo o cerrarse conservando su historial.
 */
@Entity(
    tableName = "lotes",
    indices = [
        Index(value = ["codigo"], unique = true)
    ]
)
data class LoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val codigo: String,
    val nombre: String,
    val descripcion: String? = null,
    val tipo: String,
    val fechaCreacion: Long,
    val fechaCierre: Long? = null,
    val estado: String = "ACTIVO",
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),
    // Estos datos eran temporales en el prototipo; desde Sprint 2 forman parte del lote persistido.
    val parcelaNombre: String? = null,
    val pesoObjetivoLibras: Double? = null,
    val fechaSalidaEstimada: Long? = null,
    /** Datos históricos de la venta que cerró el lote. */
    val precioVenta: Double? = null,
    val fechaVenta: Long? = null
)

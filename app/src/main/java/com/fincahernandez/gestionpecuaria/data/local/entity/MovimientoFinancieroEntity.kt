package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Ingreso o egreso capturado directamente en el módulo financiero. */
@Entity(
    tableName = "movimientos_financieros",
    indices = [Index(value = ["fecha"]), Index(value = ["tipo"])]
)
data class MovimientoFinancieroEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val tipo: String,
    val categoria: String,
    val monto: Double,
    val fecha: Long,
    val observaciones: String? = null,
    val creadoEn: Long = System.currentTimeMillis()
)

package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Ingreso o egreso capturado directamente en el módulo financiero. */
@Entity(
    tableName = "movimientos_financieros",
    indices = [Index(value = ["fecha"]), Index(value = ["tipo"]), Index(value = ["loteId"])]
)
data class MovimientoFinancieroEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val tipo: String,
    val categoria: String,
    val monto: Double,
    val fecha: Long,
    /** Lote al que se imputa el ingreso o egreso; nulo para movimientos generales. */
    val loteId: String? = null,
    val observaciones: String? = null,
    val creadoEn: Long = System.currentTimeMillis()
)

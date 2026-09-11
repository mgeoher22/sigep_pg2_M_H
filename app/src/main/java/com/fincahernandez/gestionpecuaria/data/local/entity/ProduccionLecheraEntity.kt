package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Producción total de una fecha y precio realmente utilizado ese día.
 * La fecha es única porque la finca consolida un solo ordeño diario.
 */
@Entity(
    tableName = "produccion_lechera",
    indices = [Index(value = ["fecha"], unique = true)]
)
data class ProduccionLecheraEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val fecha: Long,
    val litros: Double,
    val precioPorLitro: Double,
    val observaciones: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

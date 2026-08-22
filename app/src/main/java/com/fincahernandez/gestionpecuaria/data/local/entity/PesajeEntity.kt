package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pesajes",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["loteId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animalId"]),
        Index(value = ["loteId"]),
        Index(value = ["fechaPesaje"])
    ]
)
data class PesajeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val animalId: String,
    val loteId: String? = null,
    val pesoKg: Double,
    val fechaPesaje: Long,
    val tipoRegistro: String = "INDIVIDUAL",
    val observaciones: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    init {
        require(pesoKg.isFinite() && pesoKg > 0.0) {
            "El peso debe ser un valor positivo expresado en kilogramos"
        }
    }
}

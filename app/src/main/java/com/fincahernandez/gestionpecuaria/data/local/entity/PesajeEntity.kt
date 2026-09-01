package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa una medición de peso individual.
 *
 * Todos los pesos se almacenan en libras. `loteId` es opcional porque un
 * animal puede pesarse aunque todavía no esté asignado a un lote.
 */
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
    val pesoLibras: Double,
    val fechaPesaje: Long,
    val tipoRegistro: String = "INDIVIDUAL",
    val observaciones: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    init {
        // Evita guardar valores negativos, cero, infinitos o no numéricos.
        require(pesoLibras.isFinite() && pesoLibras > 0.0) {
            "El peso debe ser un valor positivo expresado en libras"
        }
    }
}

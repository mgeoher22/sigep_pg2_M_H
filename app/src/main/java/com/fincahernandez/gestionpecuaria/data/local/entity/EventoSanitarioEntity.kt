package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Evento de la ficha clínica de un solo animal.
 *
 * `loteIdReferencia` conserva el contexto donde se localizó al animal, pero no
 * sustituye a `animalId`: una vacuna o tratamiento nunca se aplica por lote.
 */
@Entity(
    tableName = "eventos_sanitarios",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animalId"]),
        Index(value = ["fechaEvento"]),
        Index(value = ["loteIdReferencia"])
    ]
)
data class EventoSanitarioEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val animalId: String,
    val loteIdReferencia: String? = null,
    val tipoEvento: String,
    val fechaEvento: Long,
    val diagnostico: String,
    val medicamento: String? = null,
    val dosis: String? = null,
    val estadoSalud: String,
    val proximoControl: Long? = null,
    val responsable: String? = null,
    val observaciones: String? = null,
    val creadoEn: Long = System.currentTimeMillis()
)

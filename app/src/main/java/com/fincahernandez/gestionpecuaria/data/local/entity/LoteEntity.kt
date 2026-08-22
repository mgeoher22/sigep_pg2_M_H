package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
    val actualizadoEn: Long = System.currentTimeMillis()
)

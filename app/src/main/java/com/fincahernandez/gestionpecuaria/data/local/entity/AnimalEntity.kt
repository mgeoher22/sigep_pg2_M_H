package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animales",
    indices = [
        Index(value = ["codigoIdentificacion"], unique = true)
    ]
)
data class AnimalEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val codigoIdentificacion: String,
    val nombre: String? = null,
    val sexo: String,
    val raza: String? = null,
    val fechaNacimiento: Long? = null,
    val fechaIngreso: Long,
    val categoria: String,
    val procedencia: String? = null,
    val estado: String = "ACTIVO",
    val observaciones: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

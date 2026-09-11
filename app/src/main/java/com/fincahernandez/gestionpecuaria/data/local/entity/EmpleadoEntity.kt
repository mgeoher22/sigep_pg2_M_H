package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Ficha laboral persistente del colaborador. */
@Entity(
    tableName = "empleados",
    indices = [Index(value = ["nombreCompleto"]), Index(value = ["activo"])]
)
data class EmpleadoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombreCompleto: String,
    val cargo: String,
    val fechaIngreso: Long,
    val salarioBase: Double,
    val frecuenciaPago: String,
    val sectorAsignado: String? = null,
    val telefono: String? = null,
    val activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

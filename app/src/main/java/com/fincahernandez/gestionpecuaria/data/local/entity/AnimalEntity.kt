package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa un animal almacenado en la tabla `animales`.
 *
 * El código de identificación es único y puede corresponder al método de
 * identificación que decida utilizar la finca. Las fechas se guardan como
 * milisegundos para que Room pueda almacenarlas sin convertidores adicionales.
 */
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
    val tipoOrigen: String = "NACIDO_EN_FINCA",
    val procedencia: String? = null,
    val estadoSalud: String = "EXCELENTE",
    val estado: String = "ACTIVO",
    val observaciones: String? = null,
    val fotoUri: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

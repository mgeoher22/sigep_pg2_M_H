package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cuenta local del sistema. La contraseña nunca se almacena directamente:
 * solo se conserva el resultado del algoritmo derivador y su sal aleatoria.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["usuarioNormalizado"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombreCompleto: String,
    val usuario: String,
    val usuarioNormalizado: String,
    val passwordHash: String,
    val passwordSalt: String,
    val passwordAlgorithm: String,
    val passwordIterations: Int,
    val rol: String,
    val activo: Boolean,
    val creadoEn: Long,
    /** Nulo usa la plantilla del rol; cualquier valor contiene excepciones por usuario. */
    val permisosPersonalizados: String?
)

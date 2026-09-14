package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Configuración única utilizada al registrar la producción lechera.
 *
 * El identificador fijo evita crear varias configuraciones vigentes y permite
 * actualizar el precio o la frecuencia sin alterar los registros históricos.
 */
@Entity(tableName = "configuracion_leche")
data class ConfiguracionLecheEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val precioPorLitro: Double,
    val frecuenciaPago: String,
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

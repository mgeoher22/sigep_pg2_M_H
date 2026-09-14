package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Terreno productivo registrado localmente para organizar los lotes. */
@Entity(
    tableName = "parcelas",
    indices = [
        Index(value = ["codigo"], unique = true),
        Index(value = ["nombre"], unique = true)
    ]
)
data class ParcelaEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val codigo: String,
    val nombre: String,
    val areaHectareas: Double,
    val tipoPastura: String,
    val capacidadAnimales: Int? = null,
    /** Polígono WGS84 en formato GeoJSON importado desde Google Earth. */
    val limitesGeoJson: String? = null,
    val estado: String = "DISPONIBLE",
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

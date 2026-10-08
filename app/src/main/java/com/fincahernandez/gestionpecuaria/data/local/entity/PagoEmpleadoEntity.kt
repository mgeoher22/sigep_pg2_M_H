package com.fincahernandez.gestionpecuaria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Pago individual que conserva el empleado, período, fecha y monto reales. */
@Entity(
    tableName = "pagos_empleados",
    foreignKeys = [
        ForeignKey(
            entity = EmpleadoEntity::class,
            parentColumns = ["id"],
            childColumns = ["empleadoId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["empleadoId"]), Index(value = ["fechaPago"]), Index(value = ["loteId"])]
)
data class PagoEmpleadoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val empleadoId: String,
    val fechaPago: Long,
    val monto: Double,
    val periodo: String,
    /** Imputación opcional para consolidar la mano de obra de un lote. */
    val loteId: String? = null,
    val actividad: String? = null,
    val observaciones: String? = null,
    val creadoEn: Long = System.currentTimeMillis()
)

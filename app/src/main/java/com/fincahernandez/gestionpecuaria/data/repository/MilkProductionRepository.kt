package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.ConfiguracionLecheEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.ProduccionLecheraEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone

/** Opciones acordadas con quien compra la producción de leche. */
enum class MilkPaymentFrequency(val storedValue: String, val displayName: String) {
    DAILY("DIARIO", "Día a día"),
    EVERY_SATURDAY("CADA_SABADO", "Cada sábado");

    companion object {
        fun fromStoredValue(value: String): MilkPaymentFrequency =
            entries.firstOrNull { it.storedValue == value } ?: DAILY
    }
}

/** Configuración vigente; el precio se copia a cada registro para conservar el histórico. */
data class MilkConfiguration(
    val pricePerLiter: Double,
    val paymentFrequency: MilkPaymentFrequency,
    val updatedAt: Long
)

/** Valores validados que se almacenan para un único día de producción. */
data class MilkProductionDraft(
    val date: Long,
    val liters: Double,
    val pricePerLiter: Double,
    val paymentFrequency: MilkPaymentFrequency,
    val notes: String?
)

/** Fuente única de verdad de la producción lechera persistida en Room. */
class MilkProductionRepository(private val database: GestionPecuariaDatabase) {
    private val dao = database.produccionLecheraDao()
    private val configurationDao = database.configuracionLecheDao()

    fun observeRecords(): Flow<List<ProduccionLecheraEntity>> = dao.observarTodas()

    fun observeConfiguration(): Flow<MilkConfiguration?> = configurationDao.observar().map { entity ->
        entity?.let {
            MilkConfiguration(
                pricePerLiter = it.precioPorLitro,
                paymentFrequency = MilkPaymentFrequency.fromStoredValue(it.frecuenciaPago),
                updatedAt = it.actualizadoEn
            )
        }
    }

    /** Guarda la configuración vigente sin modificar precios históricos ya registrados. */
    suspend fun saveConfiguration(pricePerLiter: Double, frequency: MilkPaymentFrequency) {
        require(pricePerLiter.isFinite() && pricePerLiter > 0.0) {
            "El precio fijo por litro debe ser mayor que cero."
        }
        configurationDao.guardar(
            ConfiguracionLecheEntity(
                precioPorLitro = pricePerLiter,
                frecuenciaPago = frequency.storedValue
            )
        )
    }

    /**
     * Una segunda captura de la misma fecha corrige el registro existente y conserva
     * su identidad, en vez de sumar dos veces la producción del mismo día.
     */
    suspend fun saveDailyProduction(draft: MilkProductionDraft): String = database.withTransaction {
        validate(draft)
        val current = dao.buscarPorFecha(draft.date)
        val now = System.currentTimeMillis()
        if (current == null) {
            val record = ProduccionLecheraEntity(
                fecha = draft.date,
                litros = draft.liters,
                precioPorLitro = draft.pricePerLiter,
                fechaPagoProgramada = calculateMilkPaymentDueDate(
                    draft.date,
                    draft.paymentFrequency
                ),
                observaciones = draft.notes?.trim()?.ifBlank { null }
            )
            dao.insertar(record)
            record.id
        } else {
            dao.actualizar(
                current.copy(
                    litros = draft.liters,
                    precioPorLitro = draft.pricePerLiter,
                    fechaPagoProgramada = calculateMilkPaymentDueDate(
                        draft.date,
                        draft.paymentFrequency
                    ),
                    // Una corrección cambia el monto y debe confirmarse nuevamente.
                    pagoConfirmadoEn = null,
                    observaciones = draft.notes?.trim()?.ifBlank { null },
                    actualizadoEn = now
                )
            )
            current.id
        }
    }

    /** Solo después de esta acción el pago puede incorporarse a los ingresos. */
    suspend fun confirmPayment(paymentDueDate: Long): Int = database.withTransaction {
        require(paymentDueDate > 0L) { "La fecha de pago no es válida." }
        dao.confirmarPago(paymentDueDate, System.currentTimeMillis())
            .also { require(it > 0) { "Este pago ya fue confirmado o no tiene registros pendientes." } }
    }

    private fun validate(draft: MilkProductionDraft) {
        requireNotFuture(draft.date, "La fecha de producción")
        require(draft.liters.isFinite() && draft.liters > 0.0) {
            "Los litros producidos deben ser mayores que cero."
        }
        require(draft.pricePerLiter.isFinite() && draft.pricePerLiter >= 0.0) {
            "El precio por litro no puede ser negativo."
        }
    }
}

/**
 * Calcula la fecha de cobro sin modificar la fecha productiva.
 * En pago semanal, el sábado actual cuenta como fecha de pago del mismo periodo.
 */
fun calculateMilkPaymentDueDate(
    productionDate: Long,
    frequency: MilkPaymentFrequency,
    timeZone: TimeZone = TimeZone.getDefault()
): Long {
    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = productionDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (frequency == MilkPaymentFrequency.EVERY_SATURDAY) {
        val daysUntilSaturday =
            (Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, daysUntilSaturday)
    }
    return calendar.timeInMillis
}

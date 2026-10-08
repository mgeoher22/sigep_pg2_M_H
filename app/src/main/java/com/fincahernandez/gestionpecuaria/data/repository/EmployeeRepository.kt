package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.EmpleadoEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PagoEmpleadoEntity
import kotlinx.coroutines.flow.Flow

data class EmployeeDraft(
    val fullName: String,
    val role: String,
    val hireDate: Long,
    val salary: Double,
    val paymentFrequency: String,
    val assignedSector: String?,
    val phone: String?,
    val active: Boolean
)

data class EmployeePaymentDraft(
    val employeeId: String,
    val paymentDate: Long,
    val amount: Double,
    val period: String,
    val lotId: String? = null,
    val activity: String? = null,
    val notes: String?
)

/** Fuente de empleados y pagos; ambos permanecen relacionados en Room. */
class EmployeeRepository(private val database: GestionPecuariaDatabase) {
    private val dao = database.empleadoDao()

    fun observeEmployees(): Flow<List<EmpleadoEntity>> = dao.observarTodos()

    fun observePayments(): Flow<List<PagoEmpleadoEntity>> = dao.observarPagos()

    suspend fun saveEmployee(draft: EmployeeDraft): String {
        validateEmployee(draft)
        val employee = EmpleadoEntity(
            nombreCompleto = draft.fullName.trim(),
            cargo = draft.role.trim(),
            fechaIngreso = draft.hireDate,
            salarioBase = draft.salary,
            frecuenciaPago = draft.paymentFrequency,
            sectorAsignado = draft.assignedSector?.trim()?.ifBlank { null },
            telefono = draft.phone?.trim()?.ifBlank { null },
            activo = draft.active
        )
        dao.insertar(employee)
        return employee.id
    }

    suspend fun savePayment(draft: EmployeePaymentDraft): String = database.withTransaction {
        requireNotFuture(draft.paymentDate, "La fecha de pago")
        require(draft.amount.isFinite() && draft.amount > 0.0) {
            "El monto del pago debe ser mayor que cero."
        }
        require(draft.period.isNotBlank()) { "Ingrese el período pagado." }
        val employee = checkNotNull(dao.buscarPorId(draft.employeeId)) {
            "No se encontró el empleado."
        }
        requireOnOrAfter(
            date = draft.paymentDate,
            minimumDate = employee.fechaIngreso,
            message = "La fecha de pago no puede ser anterior al ingreso del empleado."
        )
        val lotId = draft.lotId?.trim()?.ifBlank { null }
        lotId?.let { id ->
            check(database.loteDao().buscarPorId(id) != null) { "No se encontró el lote seleccionado." }
        }
        val payment = PagoEmpleadoEntity(
            empleadoId = draft.employeeId,
            fechaPago = draft.paymentDate,
            monto = draft.amount,
            periodo = draft.period.trim(),
            loteId = lotId,
            actividad = draft.activity?.trim()?.ifBlank { null },
            observaciones = draft.notes?.trim()?.ifBlank { null }
        )
        dao.insertarPago(payment)
        payment.id
    }

    private fun validateEmployee(draft: EmployeeDraft) {
        require(draft.fullName.isNotBlank()) { "Ingrese el nombre del empleado." }
        require(draft.role.isNotBlank()) { "Seleccione el cargo del empleado." }
        requireNotFuture(draft.hireDate, "La fecha de ingreso laboral")
        require(draft.salary.isFinite() && draft.salary > 0.0) {
            "El salario base debe ser mayor que cero."
        }
        require(draft.paymentFrequency in setOf("Semanal", "Quincenal", "Mensual")) {
            "Seleccione una frecuencia de pago válida."
        }
    }
}

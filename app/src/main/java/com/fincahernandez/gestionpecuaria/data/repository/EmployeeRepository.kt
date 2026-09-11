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
        require(draft.paymentDate > 0L) { "Seleccione una fecha de pago válida." }
        require(draft.amount.isFinite() && draft.amount > 0.0) {
            "El monto del pago debe ser mayor que cero."
        }
        require(draft.period.isNotBlank()) { "Ingrese el período pagado." }
        checkNotNull(dao.buscarPorId(draft.employeeId)) { "No se encontró el empleado." }
        val payment = PagoEmpleadoEntity(
            empleadoId = draft.employeeId,
            fechaPago = draft.paymentDate,
            monto = draft.amount,
            periodo = draft.period.trim(),
            observaciones = draft.notes?.trim()?.ifBlank { null }
        )
        dao.insertarPago(payment)
        payment.id
    }

    private fun validateEmployee(draft: EmployeeDraft) {
        require(draft.fullName.isNotBlank()) { "Ingrese el nombre del empleado." }
        require(draft.role.isNotBlank()) { "Seleccione el cargo del empleado." }
        require(draft.hireDate > 0L) { "Seleccione una fecha de ingreso válida." }
        require(draft.salary.isFinite() && draft.salary > 0.0) {
            "El salario base debe ser mayor que cero."
        }
        require(draft.paymentFrequency in setOf("Semanal", "Quincenal", "Mensual")) {
            "Seleccione una frecuencia de pago válida."
        }
    }
}

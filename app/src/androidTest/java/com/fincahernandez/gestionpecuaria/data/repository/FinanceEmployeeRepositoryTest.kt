package com.fincahernandez.gestionpecuaria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Comprueba la persistencia y relación empleado-pago implementadas en HU-12. */
@RunWith(AndroidJUnit4::class)
class FinanceEmployeeRepositoryTest {
    private val databaseName = "hu12-finance-employee-test.db"
    private lateinit var context: Context
    private lateinit var database: GestionPecuariaDatabase
    private lateinit var financeRepository: FinanceRepository
    private lateinit var employeeRepository: EmployeeRepository

    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        openDatabase()
    }

    @After
    fun cleanUp() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun persistsManualMovementEmployeeAndRelatedPayment() = runBlocking {
        val date = 1_750_000_000_000
        financeRepository.saveMovement(
            FinancialMovementDraft(
                type = "EGRESO",
                category = "Alimentación",
                amount = 850.0,
                date = date,
                lotId = null,
                notes = "Concentrado"
            )
        )
        val employeeId = employeeRepository.saveEmployee(
            EmployeeDraft(
                fullName = "Juan Pérez",
                role = "Vaquero",
                hireDate = date,
                salary = 3_200.0,
                paymentFrequency = "Mensual",
                assignedSector = "General",
                phone = "5555 0101",
                active = true
            )
        )
        employeeRepository.savePayment(
            EmployeePaymentDraft(
                employeeId = employeeId,
                paymentDate = date,
                amount = 3_200.0,
                period = "Septiembre 2026",
                lotId = null,
                activity = null,
                notes = null
            )
        )

        assertEquals(850.0, financeRepository.observeMovements().first().single().monto, 0.001)
        assertEquals(employeeId, employeeRepository.observePayments().first().single().empleadoId)

        // Reabrir el archivo demuestra que no se trataba de listas temporales.
        database.close()
        openDatabase()
        assertEquals("Juan Pérez", employeeRepository.observeEmployees().first().single().nombreCompleto)
        assertEquals(3_200.0, employeeRepository.observePayments().first().single().monto, 0.001)
        assertEquals("Alimentación", financeRepository.observeMovements().first().single().categoria)
    }

    @Test
    fun deletesOnlyExistingManualMovement() = runBlocking {
        val movementId = financeRepository.saveMovement(
            FinancialMovementDraft(
                type = "INGRESO",
                category = "Venta local",
                amount = 500.0,
                date = 1_750_000_000_000,
                notes = null
            )
        )

        financeRepository.deleteManualMovement(movementId)

        assertEquals(0, financeRepository.observeMovements().first().size)
        val secondDeleteError = runCatching {
            financeRepository.deleteManualMovement(movementId)
        }.exceptionOrNull()
        assertTrue(secondDeleteError is IllegalStateException)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context,
            GestionPecuariaDatabase::class.java,
            databaseName
        ).allowMainThreadQueries().build()
        financeRepository = FinanceRepository(database)
        employeeRepository = EmployeeRepository(database)
    }
}

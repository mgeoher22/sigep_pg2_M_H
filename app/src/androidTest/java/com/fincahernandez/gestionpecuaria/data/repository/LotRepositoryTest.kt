package com.fincahernandez.gestionpecuaria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica los criterios persistentes de lotes definidos en HU-09 del Sprint 2. */
@RunWith(AndroidJUnit4::class)
class LotRepositoryTest {
    private val databaseName = "hu09-lot-test.db"
    private lateinit var context: Context
    private lateinit var database: GestionPecuariaDatabase
    private lateinit var repository: LotRepository

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
    fun createUpdateRejectSecondActiveLotPersistAndDeactivate() = runBlocking {
        database.animalDao().insertar(testAnimal("animal-1", "FH-001"))
        database.animalDao().insertar(testAnimal("animal-2", "FH-002"))
        ParcelRepository(database).createParcel(
            ParcelDraft(
                name = "Potrero Norte",
                areaHectares = 10.0,
                pastureType = "Mombasa",
                capacity = 20
            )
        )

        val lotId = repository.createLot(testDraft(listOf("animal-1")))
        var stored = repository.observeLots().first().single()
        assertEquals("LOT-001", stored.lot.codigo)
        assertEquals(listOf("animal-1"), stored.activeAnimalIds)

        var duplicateAssignmentRejected = false
        try {
            repository.createLot(testDraft(listOf("animal-1"), name = "Lote inválido"))
        } catch (_: Exception) {
            duplicateAssignmentRejected = true
        }
        assertTrue(duplicateAssignmentRejected)
        assertEquals(1, repository.observeLots().first().size)

        repository.updateLot(lotId, testDraft(listOf("animal-2"), name = "Lote corregido"))
        stored = repository.observeLots().first().single()
        assertEquals("Lote corregido", stored.lot.nombre)
        assertEquals(listOf("animal-2"), stored.activeAnimalIds)
        assertTrue("El historial debe conservar al animal retirado", "animal-1" in stored.historicalAnimalIds)

        // Abrir nuevamente el archivo demuestra que los cambios no eran una lista en memoria.
        database.close()
        openDatabase()
        stored = repository.observeLots().first().single()
        assertEquals("Lote corregido", stored.lot.nombre)
        assertEquals(listOf("animal-2"), stored.activeAnimalIds)

        repository.deactivateLot(lotId)
        stored = repository.observeLots().first().single()
        assertEquals("CERRADO", stored.lot.estado)
        assertTrue(stored.activeAnimalIds.isEmpty())
        assertTrue("animal-2" in stored.historicalAnimalIds)
    }

    @Test
    fun individualWeightKeepsAnimalAndLotTraceability() = runBlocking {
        database.animalDao().insertar(testAnimal("animal-1", "FH-001"))
        ParcelRepository(database).createParcel(
            ParcelDraft(
                name = "Potrero Norte",
                areaHectares = 10.0,
                pastureType = "Mombasa",
                capacity = 20
            )
        )
        val lotId = repository.createLot(testDraft(listOf("animal-1")))
        val weighingDate = 1_800_000_000_000

        AnimalRepository(database).registerWeight(
            animalId = "animal-1",
            weightPounds = 475.0,
            weighingDate = weighingDate,
            notes = "Pesaje de control",
            lotId = lotId,
            recordType = "LOTE"
        )

        val records = database.pesajeDao().obtenerPorLoteYPeriodo(
            loteId = lotId,
            fechaInicio = weighingDate - 1,
            fechaFin = weighingDate + 1
        )
        assertEquals(1, records.size)
        assertEquals("animal-1", records.single().animalId)
        assertEquals(lotId, records.single().loteId)
        assertEquals("LOTE", records.single().tipoRegistro)
        val average = database.pesajeDao().obtenerPesoPromedioDelLote(
            loteId = lotId,
            fechaInicio = weighingDate - 1,
            fechaFin = weighingDate + 1
        )
        assertEquals(475.0, average ?: 0.0, 0.001)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context,
            GestionPecuariaDatabase::class.java,
            databaseName
        ).allowMainThreadQueries().build()
        repository = LotRepository(database)
    }

    private fun testDraft(animalIds: List<String>, name: String = "Lote Norte") = LotDraft(
        name = name,
        type = "ENGORDE",
        parcelName = "Potrero Norte",
        targetWeightPounds = 600.0,
        estimatedExitDate = 1_800_000_000_000,
        selectedAnimalIds = animalIds
    )

    private fun testAnimal(id: String, code: String) = AnimalEntity(
        id = id,
        codigoIdentificacion = code,
        nombre = code,
        sexo = "HEMBRA",
        raza = "Criolla",
        fechaNacimiento = 1_700_000_000_000,
        fechaIngreso = 1_700_000_000_000,
        categoria = "ENGORDE",
        estadoSalud = "EXCELENTE"
    )
}

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

/** Verifica creación, edición, ocupación, descanso y baja lógica de HU-09. */
@RunWith(AndroidJUnit4::class)
class ParcelRepositoryTest {
    private val databaseName = "hu09-parcel-test.db"
    private lateinit var context: Context
    private lateinit var database: GestionPecuariaDatabase
    private lateinit var parcelRepository: ParcelRepository

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
    fun createUpdatePersistRestAndDeactivateParcel() = runBlocking {
        val parcelId = parcelRepository.createParcel(testDraft("Potrero Norte"))
        var stored = parcelRepository.observeParcels().first().single()
        assertEquals("PR-001", stored.parcel.codigo)
        assertEquals(12.5, stored.parcel.areaHectareas, 0.0)

        parcelRepository.updateParcel(parcelId, testDraft("Potrero Principal"))
        stored = parcelRepository.observeParcels().first().single()
        assertEquals("Potrero Principal", stored.parcel.nombre)

        val lotId = LotRepository(database).createLot(
            LotDraft(
                name = "Lote de prueba",
                type = "ENGORDE",
                parcelName = "Potrero Principal",
                targetWeightPounds = 500.0,
                estimatedExitDate = null,
                selectedAnimalIds = emptyList()
            )
        )
        var occupiedChangeRejected = false
        try {
            parcelRepository.setResting(parcelId, resting = true)
        } catch (_: Exception) {
            occupiedChangeRejected = true
        }
        assertTrue("Una parcela ocupada no puede entrar en descanso", occupiedChangeRejected)
        LotRepository(database).deactivateLot(lotId)

        parcelRepository.setResting(parcelId, resting = true)
        stored = parcelRepository.observeParcels().first().single()
        assertEquals("DESCANSO", stored.parcel.estado)

        // La reapertura del mismo archivo demuestra que Room conservó los cambios.
        database.close()
        openDatabase()
        stored = parcelRepository.observeParcels().first().single()
        assertEquals("DESCANSO", stored.parcel.estado)

        parcelRepository.setResting(parcelId, resting = false)
        parcelRepository.deactivateParcel(parcelId)
        stored = parcelRepository.observeParcels().first().single()
        assertEquals("INACTIVA", stored.parcel.estado)
        assertTrue(stored.currentLotName == null)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context,
            GestionPecuariaDatabase::class.java,
            databaseName
        ).allowMainThreadQueries().build()
        parcelRepository = ParcelRepository(database)
    }

    private fun testDraft(name: String) = ParcelDraft(
        name = name,
        areaHectares = 12.5,
        pastureType = "Mombasa",
        capacity = 30
    )
}

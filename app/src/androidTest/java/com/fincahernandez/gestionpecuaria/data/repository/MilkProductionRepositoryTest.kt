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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica la persistencia y las correcciones por fecha requeridas por HU-11. */
@RunWith(AndroidJUnit4::class)
class MilkProductionRepositoryTest {
    private val databaseName = "hu11-milk-production-test.db"
    private lateinit var context: Context
    private lateinit var database: GestionPecuariaDatabase
    private lateinit var repository: MilkProductionRepository

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
    fun savesHistoricalPricesUpdatesSameDateAndSurvivesReopen() = runBlocking {
        val firstDate = 1_800_000_000_000
        val secondDate = firstDate + 86_400_000

        repository.saveDailyProduction(
            MilkProductionDraft(firstDate, 100.0, 4.25, "Primer registro")
        )
        repository.saveDailyProduction(
            MilkProductionDraft(secondDate, 120.0, 4.50, null)
        )
        // La misma fecha debe corregirse, no crear un tercer registro.
        repository.saveDailyProduction(
            MilkProductionDraft(firstDate, 105.0, 4.30, "Dato corregido")
        )

        var records = repository.observeRecords().first()
        assertEquals(2, records.size)
        assertEquals(4.50, records.first { it.fecha == secondDate }.precioPorLitro, 0.001)
        assertEquals(105.0, records.first { it.fecha == firstDate }.litros, 0.001)
        assertEquals(4.30, records.first { it.fecha == firstDate }.precioPorLitro, 0.001)

        database.close()
        openDatabase()
        records = repository.observeRecords().first()
        assertEquals(2, records.size)
        assertEquals("Dato corregido", records.first { it.fecha == firstDate }.observaciones)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context,
            GestionPecuariaDatabase::class.java,
            databaseName
        ).allowMainThreadQueries().build()
        repository = MilkProductionRepository(database)
    }
}

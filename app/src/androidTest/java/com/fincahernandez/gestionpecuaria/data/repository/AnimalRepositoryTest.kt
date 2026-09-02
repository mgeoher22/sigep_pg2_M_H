package com.fincahernandez.gestionpecuaria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Prueba los criterios principales de persistencia solicitados por HU-07. */
@RunWith(AndroidJUnit4::class)
class AnimalRepositoryTest {
    private val databaseName = "hu07-animal-test.db"
    private lateinit var context: Context
    private lateinit var database: GestionPecuariaDatabase
    private lateinit var repository: AnimalRepository

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
    fun createUpdateRejectDuplicatePersistAndRemoveAnimal() = runBlocking {
        val id = UUID.randomUUID().toString()
        val animal = testAnimal(id = id, code = "FH-TEST-001", name = "Luna")

        repository.saveAnimal(animal, weightPounds = 500.0)
        var stored = repository.observeAnimals().first().single()
        assertEquals("Luna", stored.animal.nombre)
        assertEquals(500.0, stored.lastWeightPounds ?: 0.0, 0.0)

        repository.saveAnimal(animal.copy(nombre = "Luna Editada"), weightPounds = 510.0)
        stored = repository.observeAnimals().first().single()
        assertEquals("Luna Editada", stored.animal.nombre)
        assertEquals(510.0, stored.lastWeightPounds ?: 0.0, 0.0)

        var duplicateRejected = false
        try {
            repository.saveAnimal(
                testAnimal(
                    id = UUID.randomUUID().toString(),
                    code = "FH-TEST-001",
                    name = "Código repetido"
                ),
                weightPounds = null
            )
        } catch (_: Exception) {
            duplicateRejected = true
        }
        assertTrue("Room debe rechazar códigos duplicados", duplicateRejected)

        // Cierra y vuelve a abrir el archivo para demostrar que el registro no era memoria temporal.
        database.close()
        openDatabase()
        stored = repository.observeAnimals().first().single()
        assertEquals("Luna Editada", stored.animal.nombre)

        repository.removeAnimal(id)
        stored = repository.observeAnimals().first().single()
        assertEquals("INACTIVO", stored.animal.estado)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context,
            GestionPecuariaDatabase::class.java,
            databaseName
        ).allowMainThreadQueries().build()
        repository = AnimalRepository(database)
    }

    private fun testAnimal(id: String, code: String, name: String) = AnimalEntity(
        id = id,
        codigoIdentificacion = code,
        nombre = name,
        sexo = "HEMBRA",
        raza = "Criolla",
        fechaNacimiento = 1_700_000_000_000,
        fechaIngreso = 1_700_000_000_000,
        categoria = "LECHERO",
        tipoOrigen = "NACIDO_EN_FINCA",
        procedencia = "Parcela de prueba",
        estadoSalud = "EXCELENTE"
    )
}

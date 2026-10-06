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
import org.junit.Assert.assertFalse
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

    @Test
    fun operationalUpdatesPreserveAdministrativeAnimalData() = runBlocking {
        val female = testAnimal(
            id = UUID.randomUUID().toString(),
            code = "FH-TEST-002",
            name = "Estrella"
        )
        repository.saveAnimal(female, weightPounds = null)

        repository.updatePhoto(female.id, "content://photos/estrella")
        repository.updateNearCalving(female.id, true)

        val stored = repository.observeAnimals().first().single().animal
        assertEquals("Estrella", stored.nombre)
        assertEquals("FH-TEST-002", stored.codigoIdentificacion)
        assertEquals("content://photos/estrella", stored.fotoUri)
        assertTrue(stored.proximaParto)
    }

    @Test
    fun nearCalvingUpdateRejectsMaleAnimals() = runBlocking {
        val male = testAnimal(
            id = UUID.randomUUID().toString(),
            code = "FH-TEST-003",
            name = "Toro"
        ).copy(sexo = "MACHO")
        repository.saveAnimal(male, weightPounds = null)

        var rejected = false
        try {
            repository.updateNearCalving(male.id, true)
        } catch (_: IllegalStateException) {
            rejected = true
        }

        assertTrue("Un macho no puede marcarse como próximo a parto", rejected)
    }

    @Test
    fun registeringCalvesFromSameBirthClearsStatusAndAllowsTwins() = runBlocking {
        val mother = testAnimal(
            id = UUID.randomUUID().toString(),
            code = "FH-MADRE-001",
            name = "Lucera"
        ).copy(proximaParto = true)
        repository.saveAnimal(mother, weightPounds = null)

        val calf = testAnimal(
            id = UUID.randomUUID().toString(),
            code = "FH-TERNERO-001",
            name = "Lucerito"
        ).copy(madreId = mother.id)
        repository.saveAnimal(calf, weightPounds = null)

        // La primera cría desmarca a la madre, pero una segunda nacida el mismo día
        // todavía pertenece al mismo parto y debe poder registrarse.
        val twin = testAnimal(
            id = UUID.randomUUID().toString(),
            code = "FH-TERNERO-002",
            name = "Lucerita"
        ).copy(madreId = mother.id)
        repository.saveAnimal(twin, weightPounds = null)

        val storedAnimals = repository.observeAnimals().first().map { it.animal }
        val storedMother = storedAnimals.single { it.id == mother.id }
        val storedCalf = storedAnimals.single { it.id == calf.id }
        val storedTwin = storedAnimals.single { it.id == twin.id }

        assertFalse(storedMother.proximaParto)
        assertEquals(mother.id, storedCalf.madreId)
        assertEquals(mother.id, storedTwin.madreId)
    }

    @Test
    fun rejectsArrivalBeforeBirthAndWeighingBeforeArrival() = runBlocking {
        val animal = testAnimal(
            id = UUID.randomUUID().toString(),
            code = "FH-FECHA-001",
            name = "Cronología"
        )

        val invalidAnimalError = runCatching {
            repository.saveAnimal(
                animal.copy(
                    fechaNacimiento = 1_700_000_000_000,
                    fechaIngreso = 1_600_000_000_000
                ),
                weightPounds = null
            )
        }.exceptionOrNull()
        assertTrue(invalidAnimalError is IllegalArgumentException)

        repository.saveAnimal(animal, weightPounds = null)
        val invalidWeightError = runCatching {
            repository.registerWeight(
                animalId = animal.id,
                weightPounds = 250.0,
                weighingDate = animal.fechaIngreso - 86_400_000,
                notes = null
            )
        }.exceptionOrNull()
        assertTrue(invalidWeightError is IllegalArgumentException)
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

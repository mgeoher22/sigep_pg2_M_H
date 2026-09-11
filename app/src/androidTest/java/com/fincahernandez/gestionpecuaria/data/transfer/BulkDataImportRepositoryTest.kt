package com.fincahernandez.gestionpecuaria.data.transfer

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Demuestra que la importación escribe en Room y que repetir el archivo no duplica eventos. */
@RunWith(AndroidJUnit4::class)
class BulkDataImportRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: GestionPecuariaDatabase
    private lateinit var importFile: File

    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            GestionPecuariaDatabase::class.java
        ).build()
        importFile = File(context.cacheDir, "bulk-import-test.csv").apply {
            writeText("\uFEFF" + bulkImportTemplateCsv(), Charsets.UTF_8)
        }
    }

    @After
    fun cleanUp() {
        database.close()
        importFile.delete()
    }

    @Test
    fun importIsAtomicPersistentAndIdempotent() = runBlocking {
        val repository = BulkDataImportRepository(context, database)
        val uri = Uri.fromFile(importFile)

        val preview = repository.inspect(uri)
        val firstResult = repository.import(uri)
        repository.import(uri)

        assertEquals(4, preview.totalCount)
        assertEquals(1, firstResult.animalsCreated)
        assertEquals(1, database.animalDao().observarTodos().first().size)
        // La ficha ANIMAL contiene su peso actual y PESAJE conserva el historial independiente.
        assertEquals(2, database.pesajeDao().observarTodos().first().size)
        assertEquals(1, database.eventoSanitarioDao().observarTodos().first().size)
    }

    @Test
    fun currentAnimalWeightCannotBeOlderThanStoredLatestWeight() = runBlocking {
        val repository = BulkDataImportRepository(context, database)
        val uri = Uri.fromFile(importFile)
        repository.import(uri)

        val animalsOnlyCsv = bulkImportTemplateCsv(setOf(DataTransferModule.ANIMALS))
            .replace("09/09/2026", "02/01/2024")
        importFile.writeText("\uFEFF$animalsOnlyCsv", Charsets.UTF_8)

        val error = runCatching {
            repository.inspect(uri, setOf(DataTransferModule.ANIMALS))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("pesaje posterior"))
    }

    @Test
    fun initialLoadKeepsCodeAndReplacesPreviousTestHistory() = runBlocking {
        val repository = BulkDataImportRepository(context, database)
        val uri = Uri.fromFile(importFile)
        repository.import(uri)

        val initialLoadCsv = bulkImportTemplateCsv(setOf(DataTransferModule.ANIMALS))
            .replace("550.5", "600")
            .replace("09/09/2026", "02/01/2024")
        importFile.writeText("\uFEFF$initialLoadCsv", Charsets.UTF_8)

        val result = repository.import(
            uri = uri,
            modules = setOf(DataTransferModule.ANIMALS),
            mode = BulkImportMode.INITIAL_LOAD_REPLACE
        )
        val animals = database.animalDao().observarTodos().first()
        val weights = database.pesajeDao().observarTodos().first()

        assertEquals(1, result.animalHistoriesReplaced)
        assertEquals("FH-2026-100", animals.single().codigoIdentificacion)
        assertEquals(1, weights.size)
        assertEquals(600.0, weights.single().pesoLibras, 0.0)
        assertEquals(0, database.eventoSanitarioDao().observarTodos().first().size)
    }
}

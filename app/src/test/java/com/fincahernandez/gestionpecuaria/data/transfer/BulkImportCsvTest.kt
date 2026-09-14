package com.fincahernandez.gestionpecuaria.data.transfer

import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

/** Valida la plantilla que usará el Administrador General en Excel. */
class BulkImportCsvTest {
    @Test
    fun officialTemplateParsesAllSupportedRecordTypes() {
        val payload = parseBulkImportCsv(bulkImportTemplateCsv())

        assertEquals(1, payload.animals.size)
        assertEquals(1, payload.weighings.size)
        assertEquals(1, payload.sanitaryRecords.size)
        assertEquals(550.5, payload.animals.single().currentWeightPounds ?: 0.0, 0.0)
        assertEquals(
            "Fila de ejemplo; puede eliminarla",
            payload.animals.single().notes
        )
    }

    @Test
    fun invalidWeightRejectsTheWholeFile() {
        val csv = bulkImportTemplateCsv().replace("550.5", "peso incorrecto")

        assertThrows(IllegalArgumentException::class.java) {
            parseBulkImportCsv(csv)
        }
    }

    @Test
    fun onlySelectedModulesAreParsed() {
        val payload = parseBulkImportCsv(
            csv = bulkImportTemplateCsv(),
            modules = setOf(DataTransferModule.ANIMALS)
        )

        assertEquals(1, payload.animals.size)
        assertTrue(payload.weighings.isEmpty())
        assertTrue(payload.sanitaryRecords.isEmpty())
    }

    @Test
    fun exportedAnimalCanBeEditedAndImportedWithItsCurrentWeight() {
        val animal = AnimalEntity(
            id = "animal-1",
            codigoIdentificacion = "FH-001",
            nombre = "Lucera",
            sexo = "HEMBRA",
            raza = "Criolla",
            fechaNacimiento = 1_704_067_200_000,
            fechaIngreso = 1_704_067_200_000,
            categoria = "LECHERO",
            procedencia = "Finca Hernández",
            observaciones = "Animal de prueba"
        )
        val weighing = PesajeEntity(
            id = "peso-1",
            animalId = animal.id,
            pesoLibras = 620.5,
            fechaPesaje = 1_725_840_000_000
        )

        val csv = buildBulkExportCsv(
            animals = listOf(animal),
            weighings = listOf(weighing),
            sanitaryRecords = emptyList(),
            modules = setOf(DataTransferModule.ANIMALS)
        )
        val imported = parseBulkImportCsv(csv, setOf(DataTransferModule.ANIMALS)).animals.single()

        assertEquals("animal-1", imported.internalId)
        assertEquals("FH-001", imported.code)
        assertEquals("Lucera", imported.name)
        assertEquals("peso-1", imported.currentWeightId)
        assertEquals(620.5, imported.currentWeightPounds ?: 0.0, 0.0)
        assertEquals("Animal de prueba", imported.notes)
    }

    @Test
    fun exportAndImportPreserveMotherCode() {
        val mother = AnimalEntity(
            id = "mother-1",
            codigoIdentificacion = "FH-010",
            nombre = "Luna",
            sexo = "HEMBRA",
            fechaNacimiento = 1_704_067_200_000,
            fechaIngreso = 1_704_067_200_000,
            categoria = "LECHERO",
            proximaParto = true
        )
        val calf = AnimalEntity(
            id = "calf-1",
            codigoIdentificacion = "FH-011",
            nombre = "Lucero",
            sexo = "MACHO",
            fechaNacimiento = 1_725_840_000_000,
            fechaIngreso = 1_725_840_000_000,
            categoria = "ENGORDE",
            madreId = mother.id
        )

        val csv = buildBulkExportCsv(
            animals = listOf(calf, mother),
            weighings = emptyList(),
            sanitaryRecords = emptyList(),
            modules = setOf(DataTransferModule.ANIMALS)
        )
        val imported = parseBulkImportCsv(csv, setOf(DataTransferModule.ANIMALS))

        assertEquals("FH-010", imported.animals.first { it.code == "FH-011" }.motherCode)
        assertEquals(null, imported.animals.first { it.code == "FH-010" }.motherCode)
        assertTrue(imported.animals.first { it.code == "FH-010" }.nearCalving)
    }

    @Test
    fun previousCsvWithoutReproductiveColumnsRemainsCompatible() {
        val currentCsv = bulkImportTemplateCsv(setOf(DataTransferModule.ANIMALS))
        val previousCsv = currentCsv
            .replace(";codigoMadre;proximaParto;", ";")
            .replace(";Ejemplo;;NO;HEMBRA;", ";Ejemplo;HEMBRA;")

        val imported = parseBulkImportCsv(previousCsv, setOf(DataTransferModule.ANIMALS))

        assertEquals(1, imported.animals.size)
        assertEquals(null, imported.animals.single().motherCode)
        assertEquals(false, imported.animals.single().nearCalving)
    }
}

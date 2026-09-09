package com.fincahernandez.gestionpecuaria.ui.navigation

import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.repository.AnimalStoredRecord
import org.junit.Assert.assertEquals
import org.junit.Test

/** Evita que el estado de salud vuelva a confundirse con el estado del inventario. */
class AnimalItemCollectionsTest {
    @Test
    fun activeAnimalsAreFilteredUsingPersistentInventoryStatus() {
        val active = storedAnimal(id = "1", code = "FH-001", inventoryStatus = "ACTIVO")
        val inactive = storedAnimal(id = "2", code = "FH-002", inventoryStatus = "INACTIVO")

        val result = buildAnimalItemCollections(listOf(active, inactive))

        assertEquals(2, result.all.size)
        assertEquals(listOf("FH-001"), result.active.map { it.codigoIdentificacion })
        assertEquals("EXCELENTE", result.active.single().estado)
    }

    private fun storedAnimal(
        id: String,
        code: String,
        inventoryStatus: String
    ) = AnimalStoredRecord(
        animal = AnimalEntity(
            id = id,
            codigoIdentificacion = code,
            sexo = "HEMBRA",
            fechaIngreso = 1_700_000_000_000,
            categoria = "LECHERO",
            estadoSalud = "EXCELENTE",
            estado = inventoryStatus
        ),
        lastWeightPounds = null
    )
}

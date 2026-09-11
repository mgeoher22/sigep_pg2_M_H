package com.fincahernandez.gestionpecuaria.ui.navigation

import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifica que las comparaciones nunca mezclen el historial de dos animales. */
class WeighingComparisonTest {
    @Test
    fun previousWeightBelongsToSameAnimalAndEarlierMeasurement() {
        val records = listOf(
            weighing("a-new", "animal-a", 560.0, 3_000),
            weighing("b-new", "animal-b", 430.0, 4_000),
            weighing("a-old", "animal-a", 525.0, 1_000),
            weighing("b-old", "animal-b", 450.0, 2_000)
        )

        val previousById = buildPreviousWeightByRecordId(records)

        assertNull(previousById["a-old"])
        assertEquals(525.0, previousById["a-new"] ?: 0.0, 0.0)
        assertNull(previousById["b-old"])
        assertEquals(450.0, previousById["b-new"] ?: 0.0, 0.0)
    }

    private fun weighing(id: String, animalId: String, weight: Double, date: Long) =
        PesajeEntity(
            id = id,
            animalId = animalId,
            pesoLibras = weight,
            fechaPesaje = date,
            creadoEn = date
        )
}

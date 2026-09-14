package com.fincahernandez.gestionpecuaria.ui.navigation

import com.fincahernandez.gestionpecuaria.data.local.entity.LoteAnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LotWeightHistoryTest {
    @Test
    fun includesBaselineAndAssignmentPeriodButExcludesLaterWeights() {
        val assignment = LoteAnimalEntity(
            id = "assignment",
            animalId = "animal",
            loteId = "lot",
            fechaIngreso = 200L,
            fechaSalida = 400L
        )
        val weighings = listOf(
            weighing("baseline", "animal", 100L, 100.0),
            weighing("during", "animal", 300L, 125.0),
            weighing("after", "animal", 500L, 140.0),
            weighing("other", "other-animal", 300L, 300.0)
        )

        val result = lotWeighingsWithinAssignments(listOf(assignment), weighings)

        assertEquals(listOf("baseline", "during"), result.map { it.id })
    }

    private fun weighing(id: String, animalId: String, date: Long, pounds: Double) =
        PesajeEntity(
            id = id,
            animalId = animalId,
            pesoLibras = pounds,
            fechaPesaje = date
        )
}

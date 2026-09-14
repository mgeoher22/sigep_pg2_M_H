package com.fincahernandez.gestionpecuaria.ui.screens.lots

import org.junit.Assert.assertEquals
import org.junit.Test

class LotWeightGainTest {
    @Test
    fun sumsFirstToLastChangeForEachAnimal() {
        val records = listOf(
            LotWeightRecord("a", 1L, "01/01/2026", 100.0),
            LotWeightRecord("a", 2L, "02/01/2026", 120.0),
            LotWeightRecord("b", 1L, "01/01/2026", 200.0),
            LotWeightRecord("b", 2L, "02/01/2026", 190.0),
            LotWeightRecord("c", 2L, "02/01/2026", 80.0)
        )

        val result = calculateLotWeightGain(records)

        assertEquals(10.0, result.totalPounds, 0.001)
        assertEquals(2, result.comparedAnimals)
    }
}

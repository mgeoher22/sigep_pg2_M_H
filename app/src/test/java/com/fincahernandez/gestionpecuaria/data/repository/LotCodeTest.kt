package com.fincahernandez.gestionpecuaria.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class LotCodeTest {

    @Test
    fun `genera el siguiente codigo despues del mayor existente`() {
        assertEquals(
            "LOT-013",
            nextLotCode(listOf("LOT-002", "LOT-012", "LOT-004"))
        )
    }

    @Test
    fun `ignora codigos externos al formato automatico`() {
        assertEquals("LOT-001", nextLotCode(listOf("ENGORDE-A", "LOTE-99")))
    }
}

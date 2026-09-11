package com.fincahernandez.gestionpecuaria.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class ParcelCodeTest {
    @Test
    fun `genera codigo consecutivo usando el numero mayor`() {
        assertEquals("PR-011", nextParcelCode(listOf("PR-010", "PR-002", "Parcela Norte")))
    }
}

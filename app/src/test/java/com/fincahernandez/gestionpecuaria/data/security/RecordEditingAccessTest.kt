package com.fincahernandez.gestionpecuaria.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordEditingAccessTest {
    @Test
    fun onlyGeneralAdministratorCanEditExistingRecords() {
        assertTrue(canEditExistingRecords("Administrador General"))
        assertFalse(canEditExistingRecords("Administrador de Campo"))
        assertFalse(canEditExistingRecords("Auxiliar Contable"))
        assertFalse(canEditExistingRecords("Operario"))
        assertFalse(canEditExistingRecords(null))
    }
}

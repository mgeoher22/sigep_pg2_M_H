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

    @Test
    fun animalPermissionAllowsOnlyOperationalAnimalUpdates() {
        assertTrue(canUpdateAnimalFieldStatus(setOf("dashboard", "animals")))
        assertFalse(canUpdateAnimalFieldStatus(setOf("dashboard", "reports")))
        assertFalse(canUpdateAnimalFieldStatus(emptySet()))
    }

    @Test
    fun everyDefinedRoleCanOpenReportsButOnlyGeneralAdministratorCanImport() {
        definedRoles.forEach { role ->
            assertTrue("${role.name} debe poder consultar reportes", "reports" in role.permissionIds)
        }
        assertTrue(canImportApplicationData(GENERAL_ADMIN_ROLE))
        definedRoles.filterNot { it.name == GENERAL_ADMIN_ROLE }.forEach { role ->
            assertFalse(canImportApplicationData(role.name))
        }
    }

    @Test
    fun authorizedManagementRolesCanConfirmMilkPayments() {
        assertTrue(canConfirmMilkPayments(GENERAL_ADMIN_ROLE))
        assertTrue(canConfirmMilkPayments(FIELD_ADMIN_ROLE))
        assertTrue(canConfirmMilkPayments(FINANCIAL_ROLE))
        assertTrue(canConfirmMilkPayments("Financiero"))
        assertFalse(canConfirmMilkPayments("Operario"))
        assertFalse(canConfirmMilkPayments("Técnico Veterinario"))
        assertFalse(canConfirmMilkPayments(null))
    }

    @Test
    fun bothAdministratorsCanManageSupplies() {
        assertTrue(canManageSupplies(GENERAL_ADMIN_ROLE))
        assertTrue(canManageSupplies(FIELD_ADMIN_ROLE))
        assertFalse(canManageSupplies(FINANCIAL_ROLE))
        assertFalse(canManageSupplies("Operario"))
        assertFalse(canManageSupplies(null))
    }

    @Test
    fun operatorsAndAccountingAssistantsCanRecordSupplyEntriesAndExits() {
        assertTrue(canRecordSupplyMovements(GENERAL_ADMIN_ROLE))
        assertTrue(canRecordSupplyMovements(FIELD_ADMIN_ROLE))
        assertTrue(canRecordSupplyMovements("Operario"))
        assertTrue(canRecordSupplyMovements(FINANCIAL_ROLE))
        assertFalse(canRecordSupplyMovements("Técnico Veterinario"))
        assertFalse(canRecordSupplyMovements(null))
        assertTrue("supplies" in permissionsForRole("Operario"))
        assertTrue("supplies" in permissionsForRole(FINANCIAL_ROLE))
        assertTrue("supplies" in permissionsForUser("Operario", "dashboard,animals"))
        assertTrue("supplies" in permissionsForUser(FINANCIAL_ROLE, "dashboard,finance"))
    }

    @Test
    fun onlyAdministratorsAndAccountingAssistantsCanViewLotFinancials() {
        assertTrue(canViewLotFinancials(GENERAL_ADMIN_ROLE))
        assertTrue(canViewLotFinancials(FIELD_ADMIN_ROLE))
        assertTrue(canViewLotFinancials(FINANCIAL_ROLE))
        assertFalse(canViewLotFinancials("Operario"))
        assertFalse(canViewLotFinancials("Técnico Veterinario"))
        assertTrue("lots" in permissionsForUser(FINANCIAL_ROLE, "dashboard,finance"))
    }
}

package com.fincahernandez.gestionpecuaria.data.remote

import org.junit.Assert.*
import org.junit.Test

class CloudUserDraftTest {
    private fun draft()=CloudUserDraft(" Ana Pérez "," ANA@EXAMPLE.COM ","Password123!","Operario",true,setOf("dashboard","animals"))
    @Test fun normalizesNameAndEmailWithoutChangingPassword() {
        val d=draft().validated();assertEquals("Ana Pérez",d.name);assertEquals("ana@example.com",d.email);assertEquals("Password123!",d.password)
    }
    @Test fun rejectsMalformedEmailsAndAcceptsNormalAddresses() {
        assertTrue(CloudUserDraft.validEmail("ana.perez+finca@example.com"))
        for(email in listOf("ana","ana@","ana example@test.com","ana@@test.com","ana@test"))assertFalse(CloudUserDraft.validEmail(email))
    }
    @Test fun enforcesPasswordLimitInBytes() {
        assertFalse(CloudUserDraft.validPassword("short"));assertFalse(CloudUserDraft.validPassword("ñ".repeat(40)))
        assertTrue(CloudUserDraft.validPassword("a".repeat(72)))
    }
    @Test fun rejectsUnknownRoleOrPermissionsAndMissingDashboard() {
        for(d in listOf(draft().copy(role="root"),draft().copy(permissions=setOf("root","dashboard")),draft().copy(permissions=setOf("animals")))) {
            try {d.validated();fail("Debe rechazar")}catch(_:IllegalArgumentException){}
        }
    }
    @Test fun validatesExistingUserChangesWithoutRequiringCredentials() {
        val draft = CloudUserUpdateDraft(
            id = "11111111-1111-1111-1111-111111111111",
            name = " Ana Pérez ",
            role = "Operario",
            active = false,
            permissions = setOf("dashboard", "animals")
        ).validated()
        assertEquals("Ana Pérez", draft.name)
        assertFalse(draft.active)
    }
}

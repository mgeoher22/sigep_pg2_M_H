package com.fincahernandez.gestionpecuaria.data.remote

import org.junit.Assert.*
import org.junit.Test

class CloudUserDraftTest {
    private fun draft()=CloudUserDraft(" Ana Pérez "," ANA@EXAMPLE.COM ","Operario",true,setOf("dashboard","animals"))
    @Test fun normalizesNameAndEmailForAutomaticPasswordCreation() {
        val d=draft().validated();assertEquals("Ana Pérez",d.name);assertEquals("ana@example.com",d.email)
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

    @Test fun validatesOwnPasswordChange() {
        val change = CloudPasswordChange("Actual123!", "NuevaClave123!").validated()
        assertEquals("Actual123!", change.currentPassword)
        assertEquals("NuevaClave123!", change.newPassword)
    }

    @Test fun rejectsWeakOrRepeatedOwnPassword() {
        for (change in listOf(
            CloudPasswordChange("", "NuevaClave123!"),
            CloudPasswordChange("Actual123!", "corta"),
            CloudPasswordChange("Actual123!", "Actual123!")
        )) {
            try {
                change.validated()
                fail("Debe rechazar el cambio")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test fun validatesAdministrativePasswordReset() {
        val reset = CloudPasswordResetDraft(
            "11111111-1111-1111-1111-111111111111",
            "Temporal123!"
        ).validated()
        assertEquals("Temporal123!", reset.temporaryPassword)
    }

    @Test fun rejectsInvalidAdministrativePasswordReset() {
        for (reset in listOf(
            CloudPasswordResetDraft("no-es-uuid", "Temporal123!"),
            CloudPasswordResetDraft("11111111-1111-1111-1111-111111111111", "corta")
        )) {
            try {
                reset.validated()
                fail("Debe rechazar el restablecimiento")
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}

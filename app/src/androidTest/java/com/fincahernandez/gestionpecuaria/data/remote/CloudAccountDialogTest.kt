package com.fincahernandez.gestionpecuaria.data.remote

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.ui.screens.auth.CloudAccountDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudAccountDialogTest {
    @get:Rule val compose = createComposeRule()
    @Test fun connectsShowsRoleAndClosesCloudSession() {
        val id="188edc1d-cbe0-4066-9ae7-43ba820f985f"
        val uid="388edc1d-cbe0-4066-9ae7-43ba820f985f"
        val store=object: CloudSessionStore {
            var value:String?=null
            override fun read()=value
            override fun write(value:String){this.value=value}
            override fun clear(){value=null}
        }
        val manager=CloudSessionManager(store,"https://test.supabase.co","sb_publishable_test",
            SupabaseConnectionProbe.Transport {url,_,_,_-> when {
                url.contains("/token") -> """{"access_token":"fake","refresh_token":"refresh","expires_at":9999999999,"user":{"id":"$uid","email":"test@example.com"}}"""
                url.contains("/logout") -> ""
                else -> """[{"usuarioId":"$uid","usuarioLocalId":"$id","activo":true,"rol":"Operario","permisosPersonalizados":null}]"""
            }})
        compose.setContent { MaterialTheme {
            CloudAccountDialog(AuthenticatedUser(id,"Prueba","local","Operario",emptySet()),manager,{})
        }}
        compose.waitForIdle()
        compose.onNodeWithText("Correo de nube").performTextInput("test@example.com")
        compose.onNodeWithText("Contraseña de nube").performTextInput("example-password")
        compose.onNodeWithText("Conectar cuenta").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Rol en la nube: Operario").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Conectado: test@example.com").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión de nube").performScrollTo().performClick()
        compose.waitUntil(5000) {compose.onAllNodesWithText("Sesión de nube cerrada.").fetchSemanticsNodes().isNotEmpty()}
    }
}

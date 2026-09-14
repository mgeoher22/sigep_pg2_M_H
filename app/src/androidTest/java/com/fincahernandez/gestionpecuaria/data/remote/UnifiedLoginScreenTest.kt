package com.fincahernandez.gestionpecuaria.data.remote

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fincahernandez.gestionpecuaria.ui.screens.auth.LoginScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnifiedLoginScreenTest {
    @get:Rule val compose=createComposeRule()
    @Test fun mainLoginUsesEmailAndHasNoSeparateCloudLogin() {
        var captured:Triple<String,String,Boolean>?=null
        compose.setContent{MaterialTheme{LoginScreen(onLogin={e,p,r->captured=Triple(e,p,r)})}}
        compose.onNodeWithText("Correo electrónico").performScrollTo().performTextInput("test@example.com")
        compose.onNodeWithText("Contraseña").performScrollTo().performTextInput("123456")
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(Triple("test@example.com","123456",true),captured)}
        compose.onNodeWithText("Probar conexión con la nube").assertDoesNotExist()
        compose.onNodeWithText("Restablecer acceso local").assertDoesNotExist()
    }
}

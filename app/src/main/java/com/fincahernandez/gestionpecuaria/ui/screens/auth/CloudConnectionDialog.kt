package com.fincahernandez.gestionpecuaria.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.data.remote.SupabaseConnectionProbe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Prueba manual disponible desde el acceso local en compilaciones debug. */
@Composable
fun CloudConnectionDialog(onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    // No usar rememberSaveable: la contraseña no debe guardarse al recrear la actividad.
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val probe = remember { SupabaseConnectionProbe() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Probar conexión con la nube") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Usa el correo y la contraseña del usuario creado en Supabase. Esta prueba consulta el acceso; no sincroniza registros.")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; result = null },
                    label = { Text("Correo de Supabase") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; result = null },
                    label = { Text("Contraseña de Supabase") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                result?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                onClick = {
                    busy = true
                    result = null
                    val enteredPassword = password
                    password = ""
                    scope.launch {
                        try {
                            val tables = probe.check(email, enteredPassword)
                            result = "Conexión correcta. Usuario autorizado y consultas a $tables tablas completadas. No se han enviado registros."
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            result = when (error) {
                                is java.net.UnknownHostException, is java.net.ConnectException ->
                                    "No se pudo llegar a Supabase. Revisa internet y la URL del proyecto."
                                is javax.net.ssl.SSLException ->
                                    "No se pudo establecer una conexión segura. Revisa la fecha y la red del dispositivo."
                                else -> error.message ?: "No se pudo completar la prueba."
                            }
                        } finally {
                            busy = false
                        }
                    }
                }
            ) { Text(if (busy) "Comprobando..." else "Probar conexión") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

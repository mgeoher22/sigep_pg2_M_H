package com.fincahernandez.gestionpecuaria.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.data.remote.CloudAccount
import com.fincahernandez.gestionpecuaria.data.remote.CloudSessionManager
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.security.rolePermissions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CloudAccountDialog(user: AuthenticatedUser, manager: CloudSessionManager, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(true) }
    var account by remember { mutableStateOf<CloudAccount?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun error(e: Exception): String = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
            "No se pudo conectar. Tus datos locales siguen disponibles; vuelve a intentar con internet."
        is javax.net.ssl.SSLException -> "No se pudo establecer una conexión segura. Revisa la fecha y la red."
        is com.fincahernandez.gestionpecuaria.data.remote.CloudAccessDenied,
        is com.fincahernandez.gestionpecuaria.data.remote.CloudHttpException -> e.message ?: "No se pudo validar la cuenta."
        else -> "No se pudo guardar o validar la sesión. Intenta ingresar nuevamente."
    }
    LaunchedEffect(user.id) {
        try { account = manager.restore(user.id) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { message = error(e) }
        finally { busy = false }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Cuenta en la nube") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Usuario local: ${user.username}")
                val profile = account
                if (profile != null) {
                    Text("Conectado: ${profile.email}")
                    Text("Rol en la nube: ${profile.role}")
                    Text("Accesos: " + rolePermissions.filter { it.id in profile.permissions }.joinToString { it.label })
                    Text("La sesión se conserva de forma segura. La sincronización de registros todavía no está activada.")
                } else {
                    Text("Ingresa con la cuenta creada para ti en la nube. Al conectar se vincula con este usuario local y se recuerda la sesión en esta tablet.")
                    OutlinedTextField(email, { email = it }, label = { Text("Correo de nube") }, singleLine = true,
                        enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(password, { password = it }, label = { Text("Contraseña de nube") }, singleLine = true,
                        enabled = !busy, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    Text("Los registros y el acceso local se conservan.")
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it) }
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val revoked = manager.signOut()
                            account = null
                            message = if (revoked) "Sesión de nube cerrada." else "Sesión eliminada de esta tablet. No se pudo confirmar el cierre en el servidor."
                        } finally { busy = false }
                    }
                }) { Text("Cerrar sesión de nube") }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && (account != null || (email.isNotBlank() && password.isNotEmpty())), onClick = {
                if (account != null) onDismiss() else {
                    busy = true; message = null
                    val entered = password; password = ""
                    scope.launch {
                        try { account = manager.signIn(user, email, entered) }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = error(e) }
                        finally { busy = false }
                    }
                }
            }) { Text(if (account != null) "Listo" else "Conectar cuenta") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cerrar") } }
    )
}

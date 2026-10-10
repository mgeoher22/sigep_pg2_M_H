package com.fincahernandez.gestionpecuaria.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.remote.*
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CloudToolsDialog(action: String, user: AuthenticatedUser, manager: CloudSessionManager,
    syncStatus: AutoSyncStatus = AutoSyncStatus(AutoSyncPhase.WAITING),
    onClose: () -> Unit,
    onProfileSaved: suspend () -> Unit,
    onPasswordChange: suspend (currentPassword: String, newPassword: String) -> Unit
) {
    if(action=="sync") {
        CloudSyncDialog(user,manager,syncStatus,onClose)
        return
    }
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(user.fullName.takeUnless { '@' in it || it=="Usuario" }.orEmpty()) }
    // Las contraseñas nunca se guardan en el estado restaurable de Android.
    var currentPassword by remember(user.id) { mutableStateOf("") }
    var newPassword by remember(user.id) { mutableStateOf("") }
    var confirmPassword by remember(user.id) { mutableStateOf("") }
    var passwordAttempted by remember(user.id) { mutableStateOf(false) }
    val profile=action=="profile"
    val newPasswordValid = CloudUserDraft.validPassword(newPassword)
    val passwordsMatch = newPassword == confirmPassword
    val passwordFormValid = currentPassword.isNotEmpty() && newPasswordValid &&
        passwordsMatch && currentPassword != newPassword
    AlertDialog(onDismissRequest={if(!busy)onClose()},
        title={Text(if(profile) "Mi perfil" else "Descargar de la nube")},
        text={Column(
            modifier=Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if(profile) {
                Text("Este nombre aparecerá en el inicio. Necesitas internet para guardarlo.")
                OutlinedTextField(value=name,onValueChange={name=it},enabled=!busy && !done,
                    label={Text("Nombre completo")},singleLine=true, modifier=Modifier.fillMaxWidth())
                Text("Cambiar mi contraseña", style=MaterialTheme.typography.titleMedium)
                Text(
                    "Confirma la contraseña actual y escribe una nueva. La aplicación actualizará también el acceso sin internet de este dispositivo.",
                    style=MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value=currentPassword,
                    onValueChange={currentPassword=it},
                    enabled=!busy,
                    label={Text("Contraseña actual")},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
                    visualTransformation=PasswordVisualTransformation(),
                    isError=passwordAttempted && currentPassword.isEmpty(),
                    supportingText=if(passwordAttempted && currentPassword.isEmpty()) {
                        { Text("Escribe tu contraseña actual.") }
                    } else null,
                    singleLine=true,
                    modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value=newPassword,
                    onValueChange={newPassword=it},
                    enabled=!busy,
                    label={Text("Contraseña nueva")},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
                    visualTransformation=PasswordVisualTransformation(),
                    isError=passwordAttempted && (!newPasswordValid || currentPassword==newPassword),
                    supportingText={
                        Text(when {
                            passwordAttempted && currentPassword==newPassword && newPassword.isNotEmpty() -> "Debe ser diferente de la contraseña actual."
                            else -> "Mínimo 8 caracteres."
                        })
                    },
                    singleLine=true,
                    modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value=confirmPassword,
                    onValueChange={confirmPassword=it},
                    enabled=!busy,
                    label={Text("Confirmar contraseña nueva")},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
                    visualTransformation=PasswordVisualTransformation(),
                    isError=passwordAttempted && !passwordsMatch,
                    supportingText=if(passwordAttempted && !passwordsMatch) {
                        { Text("Las contraseñas nuevas no coinciden.") }
                    } else null,
                    singleLine=true,
                    modifier=Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    enabled=!busy,
                    onClick={
                        passwordAttempted=true
                        if(passwordFormValid) {
                            busy=true;message=null
                            scope.launch {
                                try {
                                    onPasswordChange(currentPassword,newPassword)
                                    currentPassword="";newPassword="";confirmPassword=""
                                    passwordAttempted=false
                                    message="Contraseña actualizada. Ya puedes usarla para ingresar."
                                } catch(e:CancellationException) { throw e }
                                catch(e:Exception) {
                                    message=when(e) {
                                        is CloudAccessDenied,is IllegalArgumentException -> e.message
                                        else -> "No se pudo cambiar la contraseña. Revisa internet y vuelve a intentarlo."
                                    }
                                } finally {busy=false}
                            }
                        }
                    },
                    modifier=Modifier.fillMaxWidth()
                ) { Text("Cambiar contraseña") }
            } else Text("Añade a este dispositivo los registros que faltan. Conserva los datos locales y avisa si hay diferencias. Requiere internet. Las fotos y el envío de cambios quedan pendientes.")
            if(busy) { LinearProgressIndicator(modifier=Modifier.fillMaxWidth());Text("Espera un momento…") }
            message?.let { Text(it, modifier=Modifier.padding(top=4.dp)) }
        }},
        confirmButton={if(!done) TextButton(enabled=!busy && (!profile || name.isNotBlank()),onClick={
            busy=true;message=null
            scope.launch {
                try {
                    if(profile) {
                        manager.updateDisplayName(user.id,name)
                        onProfileSaved()
                        message="Nombre guardado."
                    } else {
                        val snapshot=manager.download(user.id)
                        val result=CloudDataImporter(GestionPecuariaDatabase.obtenerInstancia(context)).apply(snapshot) { manager.validateSnapshot(snapshot) }
                        message="Descarga completada: ${result.added} registros añadidos; ${result.existing} ya estaban en el dispositivo. Diferencias conservadas: ${result.different}."
                    }
                    done=true
                } catch(e:CancellationException) { throw e }
                catch(e:Exception) {
                    message=when(e) {
                        is CloudAccessDenied,is IllegalArgumentException -> e.message
                        else -> "No se pudo completar. Revisa internet y vuelve a intentarlo. Si persiste, revisaremos los registros relacionados."
                    }
                } finally {busy=false}
            }
        }) {Text(if(profile) "Guardar nombre" else "Descargar")}},
        dismissButton={TextButton(enabled=!busy,onClick=onClose){Text("Cerrar")}})
}

/** Bloquea el acceso normal hasta reemplazar una contraseña temporal. */
@Composable
fun RequiredPasswordChangeDialog(
    onChange: suspend (currentPassword: String, newPassword: String) -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val newPasswordValid = CloudUserDraft.validPassword(newPassword)
    val matches = newPassword == confirmation
    val valid = currentPassword.isNotEmpty() && newPasswordValid && matches &&
        currentPassword != newPassword

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Crea tu contraseña personal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Ingresaste con una contraseña temporal. Debes cambiarla antes de utilizar la aplicación."
                )
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    enabled = !busy,
                    label = { Text("Contraseña temporal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = attempted && currentPassword.isEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    enabled = !busy,
                    label = { Text("Contraseña nueva") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = attempted && (!newPasswordValid || currentPassword == newPassword),
                    supportingText = { Text("Mínimo 8 caracteres y diferente de la temporal.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    enabled = !busy,
                    label = { Text("Confirmar contraseña nueva") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = attempted && !matches,
                    supportingText = if (attempted && !matches) {
                        { Text("Las contraseñas nuevas no coinciden.") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    attempted = true
                    if (valid) {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                onChange(currentPassword, newPassword)
                                currentPassword = ""
                                newPassword = ""
                                confirmation = ""
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                error = when (failure) {
                                    is CloudAccessDenied, is IllegalArgumentException -> failure.message
                                    else -> "No se pudo cambiar la contraseña. Revisa internet y vuelve a intentarlo."
                                }
                            } finally {
                                busy = false
                            }
                        }
                    }
                }
            ) { Text("Guardar contraseña") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onLogout) { Text("Cerrar sesión") }
        }
    )
}

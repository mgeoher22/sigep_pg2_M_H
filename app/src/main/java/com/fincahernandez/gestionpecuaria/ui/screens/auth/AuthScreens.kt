package com.fincahernandez.gestionpecuaria.ui.screens.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.FarmLogo

/** Pantalla de carga inicial inspirada en el prototipo denominado Screen. */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    var animationStarted by rememberSaveable { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.12f,
        animationSpec = tween(durationMillis = 1_600),
        label = "Progreso de inicio"
    )

    LaunchedEffect(Unit) { animationStarted = true }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val contentWidth = if (maxWidth > 600.dp) 520.dp else maxWidth
        val logoSize = if (maxWidth > 600.dp) 180.dp else 140.dp
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = contentWidth)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FarmLogo(size = logoSize)
            Text(
                "Finca Hernández",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Sistema inteligente de gestión ganadera",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "INICIANDO SISTEMA...",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Vista de acceso conectada a la autenticación local. La pantalla captura las
 * credenciales y el repositorio se encarga de comprobarlas sin exponer el hash.
 */
@Composable
fun LoginScreen(
    onLogin: (username: String, password: String, rememberSession: Boolean) -> Unit,
    onResetAccess: (() -> Unit)? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberSession by rememberSaveable { mutableStateOf(false) }
    var attemptedLogin by rememberSaveable { mutableStateOf(false) }
    var informationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val usernameInvalid = attemptedLogin && username.isBlank()
    val passwordInvalid = attemptedLogin && password.isBlank()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val loginContentWidth = if (maxWidth > 600.dp) 560.dp else maxWidth
        LazyColumn(
            // Evita que el encabezado quede debajo de la hora y los iconos del sistema.
            // LazyColumn permite desplazar todo el formulario en tabletas de poca altura.
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(" Sistema local disponible", color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                FarmLogo(size = 96.dp)
            }
            item {
                Text(
                    "Finca Hernández",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = loginContentWidth),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Acceso al sistema",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Ingrese las credenciales creadas por el administrador.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Usuario") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = usernameInvalid,
                            supportingText = if (usernameInvalid) {
                                { Text("Ingrese el usuario.") }
                            } else null,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) {
                                            "Ocultar contraseña"
                                        } else {
                                            "Mostrar contraseña"
                                        }
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    attemptedLogin = true
                                    if (!isLoading && username.isNotBlank() && password.isNotBlank()) {
                                        onLogin(username.trim(), password, rememberSession)
                                    }
                                }
                            ),
                            isError = passwordInvalid,
                            supportingText = if (passwordInvalid) {
                                { Text("Ingrese la contraseña.") }
                            } else null,
                            singleLine = true
                        )
                        errorMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberSession,
                                onCheckedChange = { rememberSession = it }
                            )
                            Text("Recordar sesión", modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    informationMessage =
                                        "La recuperación de contraseña se habilitará al crear el módulo de usuarios."
                                }
                            ) { Text("¿Olvidó su contraseña?") }
                        }
                        Button(
                            onClick = {
                                attemptedLogin = true
                                focusManager.clearFocus()
                                if (username.isNotBlank() && password.isNotBlank()) {
                                    onLogin(username.trim(), password, rememberSession)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Validando...")
                            } else {
                                Text("Iniciar sesión", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.size(8.dp))
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            }
                        }
                        Text(
                            "¿No tiene una cuenta?",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = {
                                informationMessage =
                                    "Solicite al Administrador General que cree o reactive su cuenta."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Solicitar acceso al administrador")
                        }
                        onResetAccess?.let {
                            TextButton(
                                onClick = { showResetConfirmation = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Restablecer acceso local",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("© 2026 Finca Hernández • Versión de prueba")
                    Text("Los datos locales permanecen en este dispositivo")
                }
            }
        }
    }

    informationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { informationMessage = null },
            title = { Text("Función pendiente") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { informationMessage = null }) { Text("Entendido") }
            }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Restablecer cuentas") },
            text = {
                Text(
                    "Se eliminarán solamente los usuarios y la sesión guardada. " +
                        "Los animales, pesajes y demás datos permanecerán intactos."
                )
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onResetAccess?.invoke()
                    }
                ) {
                    Text("Restablecer", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

/**
 * Configuración segura del primer inicio. Solo aparece cuando Room todavía
 * no contiene usuarios y crea la cuenta que administrará las demás.
 */
@Composable
fun InitialAdminSetupScreen(
    onCreateAdmin: (fullName: String, username: String, password: String) -> Unit,
    isSaving: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val usernameValid = username.trim().matches(Regex("[A-Za-z0-9._-]{3,30}"))
    val passwordValid = password.length >= 8
    val confirmationValid = confirmation == password
    val formValid = fullName.isNotBlank() && usernameValid && passwordValid && confirmationValid

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val contentWidth = if (maxWidth > 600.dp) 620.dp else maxWidth
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FarmLogo(size = 88.dp)
            }
            item {
                Text(
                    "Configurar Administrador General",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            item {
                Text(
                    "Esta configuración se realiza una sola vez. La cuenta podrá crear los demás usuarios y asignar sus roles.",
                    modifier = Modifier.widthIn(max = contentWidth),
                    textAlign = TextAlign.Center
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = contentWidth),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nombre completo *") },
                            isError = attemptedSave && fullName.isBlank(),
                            supportingText = if (attemptedSave && fullName.isBlank()) {
                                { Text("Ingrese el nombre del administrador.") }
                            } else null,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it.replace(" ", "")
                                onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Usuario *") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Next
                            ),
                            supportingText = {
                                Text("Use entre 3 y 30 letras, números, punto, guion o guion bajo.")
                            },
                            isError = attemptedSave && !usernameValid,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contraseña *") },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            supportingText = { Text("Debe contener al menos 8 caracteres.") },
                            isError = attemptedSave && !passwordValid,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = confirmation,
                            onValueChange = {
                                confirmation = it
                                onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirmar contraseña *") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            isError = attemptedSave && !confirmationValid,
                            supportingText = if (attemptedSave && !confirmationValid) {
                                { Text("Las contraseñas no coinciden.") }
                            } else null,
                            singleLine = true
                        )
                        errorMessage?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = {
                                attemptedSave = true
                                if (formValid) {
                                    onCreateAdmin(fullName.trim(), username.trim(), password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Creando cuenta...")
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Crear administrador", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}

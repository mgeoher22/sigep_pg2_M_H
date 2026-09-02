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
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
            Surface(
                modifier = Modifier.size(logoSize),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Agriculture,
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
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
 * Vista inicial de acceso. En esta etapa valida la captura, pero la autenticación
 * real quedará pendiente hasta contar con usuarios y permisos persistentes.
 */
@Composable
fun LoginScreen(
    onLogin: (username: String, password: String, rememberSession: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberSession by rememberSaveable { mutableStateOf(false) }
    var attemptedLogin by rememberSaveable { mutableStateOf(false) }
    var informationMessage by rememberSaveable { mutableStateOf<String?>(null) }
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
                    Text(" Estado: en línea", color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                Surface(
                    modifier = Modifier.size(96.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Agriculture,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(58.dp)
                        )
                    }
                }
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
                            "Acceso de prueba",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Ingrese un usuario y contraseña para acceder al prototipo.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Usuario o correo electrónico") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = usernameInvalid,
                            supportingText = if (usernameInvalid) {
                                { Text("Ingrese el usuario o correo.") }
                            } else null,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
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
                                    if (username.isNotBlank() && password.isNotBlank()) {
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
                                .height(54.dp)
                        ) {
                            Text("Iniciar sesión", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.size(8.dp))
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        }
                        Text(
                            "¿No tiene una cuenta?",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = {
                                informationMessage =
                                    "La solicitud de acceso será gestionada por el administrador en una historia posterior."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Solicitar acceso al administrador")
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
}

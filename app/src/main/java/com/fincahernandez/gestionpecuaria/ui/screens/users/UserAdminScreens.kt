package com.fincahernandez.gestionpecuaria.ui.screens.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.data.security.definedRoles
import com.fincahernandez.gestionpecuaria.data.security.rolePermissions
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes

/** Cuenta persistida que se presenta en la administración de accesos. */
data class UserUiModel(
    val id: String,
    val fullName: String,
    val username: String,
    val roleName: String,
    val active: Boolean
)

/** Datos validados que el formulario entrega al contenedor de navegación. */
data class UserFormData(
    val fullName: String,
    val username: String,
    val temporaryPassword: String,
    val roleName: String,
    val active: Boolean
)

/** Listado administrativo desde el cual se crean cuentas y se revisa el RBAC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    users: List<UserUiModel>,
    onMenuClick: () -> Unit,
    onCreateUser: () -> Unit,
    onViewRolePermissions: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Usuarios y accesos",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = Routes.USERS, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateUser,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Crear usuario") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("CUENTAS LOCALES", fontWeight = FontWeight.Bold)
                        Text(
                            "Los usuarios de esta pantalla controlan el acceso real a la aplicación."
                        )
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Control de acceso por roles", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Cada usuario recibe automáticamente los permisos del rol seleccionado."
                        )
                        OutlinedButton(onClick = onViewRolePermissions) {
                            Icon(Icons.Default.Key, contentDescription = null)
                            Text(" Ver permisos por rol")
                        }
                    }
                }
            }
            item {
                Text(
                    "Usuarios registrados",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (users.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Aún no existen usuarios", fontWeight = FontWeight.Bold)
                            Text("Cree la primera cuenta administrativa.")
                            OutlinedButton(onClick = onCreateUser) { Text("Crear usuario") }
                        }
                    }
                }
            } else {
                items(users, key = { it.id }) { user ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.fullName, fontWeight = FontWeight.Bold)
                                Text("@${user.username}", style = MaterialTheme.typography.bodySmall)
                                Text(user.roleName, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                if (user.active) "ACTIVO" else "INACTIVO",
                                color = if (user.active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

/** Formulario que crea una cuenta y le asigna una de las plantillas de rol del ERS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    existingUsernames: Set<String>,
    onBack: () -> Unit,
    onSubmit: (UserFormData) -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var selectedRoleName by rememberSaveable { mutableStateOf(definedRoles.first().name) }
    var active by rememberSaveable { mutableStateOf(true) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val normalizedUsername = username.trim().lowercase()
    val usernameExists = normalizedUsername in existingUsernames.map { it.lowercase() }
    val selectedRole = definedRoles.first { it.name == selectedRoleName }
    val usernameValid = normalizedUsername.matches(Regex("[a-z0-9._-]{3,30}"))
    val formValid = fullName.isNotBlank() && usernameValid &&
        !usernameExists && password.length >= 8

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Crear usuario", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Defina las credenciales iniciales y seleccione el rol del trabajador.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre completo *") },
                    isError = attemptedSave && fullName.isBlank(),
                    supportingText = if (attemptedSave && fullName.isBlank()) {
                        { Text("Ingrese el nombre completo.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        // Los espacios no son válidos dentro del identificador de acceso.
                        username = it.replace(" ", "")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Usuario *") },
                    prefix = { Text("@") },
                    isError = attemptedSave && (!usernameValid || usernameExists),
                    supportingText = when {
                        attemptedSave && !usernameValid -> {
                            { Text("Use de 3 a 30 letras, números, punto, guion o guion bajo.") }
                        }
                        attemptedSave && usernameExists -> {
                            { Text("Ese usuario ya está registrado.") }
                        }
                        else -> null
                    },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña temporal *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = attemptedSave && password.length < 8,
                    supportingText = {
                        Text("Debe contener al menos 8 caracteres.")
                    },
                    singleLine = true
                )
            }
            item {
                RoleDropdown(
                    value = selectedRoleName,
                    onSelected = { selectedRoleName = it }
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(selectedRole.name, fontWeight = FontWeight.Bold)
                        Text(selectedRole.description)
                        Text("Permisos concedidos:", fontWeight = FontWeight.SemiBold)
                        rolePermissions
                            .filter { it.id in selectedRole.permissionIds }
                            .forEach { permission ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(permission.label)
                                }
                            }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cuenta activa", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Permite iniciar sesión desde su creación.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
            item {
                saveError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Button(
                    onClick = {
                        attemptedSave = true
                        if (formValid) {
                            onSubmit(
                                UserFormData(
                                    fullName = fullName.trim(),
                                    username = normalizedUsername,
                                    temporaryPassword = password,
                                    roleName = selectedRoleName,
                                    active = active
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar usuario", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Matriz legible de permisos; funciona como referencia de las reglas RBAC del sistema. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRoleName by rememberSaveable { mutableStateOf(definedRoles.first().name) }
    val selectedRole = definedRoles.first { it.name == selectedRoleName }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Permisos por rol", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Los permisos están agrupados por los cinco actores definidos en el ERS.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    definedRoles.forEach { role ->
                        FilterChip(
                            selected = selectedRoleName == role.name,
                            onClick = { selectedRoleName = role.name },
                            label = { Text(role.name) },
                            leadingIcon = if (selectedRoleName == role.name) {
                                { Icon(Icons.Default.Groups, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            selectedRole.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(selectedRole.description)
                    }
                }
            }
            items(rolePermissions, key = { it.id }) { permission ->
                val granted = permission.id in selectedRole.permissionIds
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (granted) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint = if (granted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(permission.label, modifier = Modifier.weight(1f))
                        Text(
                            if (granted) "PERMITIDO" else "RESTRINGIDO",
                            color = if (granted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    value: String,
    onSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text("Rol del usuario *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            definedRoles.forEach { role ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(role.name, fontWeight = FontWeight.SemiBold)
                            Text(role.description, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        onSelected(role.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

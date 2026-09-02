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
import androidx.compose.material3.TopAppBar
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
import com.fincahernandez.gestionpecuaria.ui.components.DemoModeNotice
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes

/** Cuenta temporal que permite probar la administración de usuarios antes de conectarla a Room. */
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

data class RolePermission(
    val id: String,
    val label: String
)

data class DefinedRole(
    val name: String,
    val description: String,
    val permissionIds: Set<String>
)

/** Permisos que resumen las funciones descritas por los actores del ERS. */
val rolePermissions = listOf(
    RolePermission("dashboard", "Consultar el panel principal"),
    RolePermission("animals", "Consultar y registrar animales"),
    RolePermission("weighings", "Registrar pesajes y producción lechera"),
    RolePermission("lots", "Administrar lotes y parcelas"),
    RolePermission("health", "Gestionar sanidad y reproducción"),
    RolePermission("finance", "Registrar movimientos financieros"),
    RolePermission("employees", "Consultar empleados y nómina"),
    RolePermission("reports", "Consultar reportes estratégicos"),
    RolePermission("users", "Crear usuarios y asignar roles")
)

/**
 * Roles fijos definidos por el ERS. Elegir un rol concede su conjunto completo
 * de permisos y evita configuraciones inseguras permiso por permiso.
 */
val definedRoles = listOf(
    DefinedRole(
        name = "Administrador General",
        description = "Toma decisiones estratégicas y financieras.",
        permissionIds = rolePermissions.map { it.id }.toSet()
    ),
    DefinedRole(
        name = "Administrador de Campo",
        description = "Supervisa animales, lotes, parcelas y operarios.",
        permissionIds = setOf("dashboard", "animals", "weighings", "lots", "reports")
    ),
    DefinedRole(
        name = "Técnico Veterinario",
        description = "Gestiona la salud y reproducción del hato.",
        permissionIds = setOf("dashboard", "animals", "health", "reports")
    ),
    DefinedRole(
        name = "Operario",
        description = "Captura diariamente los datos primarios de campo.",
        permissionIds = setOf("dashboard", "animals", "weighings", "lots")
    ),
    DefinedRole(
        name = "Auxiliar Contable",
        description = "Gestiona ingresos, egresos, empleados y nómina.",
        permissionIds = setOf("dashboard", "finance", "employees", "reports")
    )
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
            TopAppBar(
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
            item { DemoModeNotice(compact = true) }
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
    val formValid = fullName.isNotBlank() && normalizedUsername.isNotBlank() &&
        !usernameExists && password.length >= 6

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
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
                    isError = attemptedSave && (normalizedUsername.isBlank() || usernameExists),
                    supportingText = when {
                        attemptedSave && normalizedUsername.isBlank() -> {
                            { Text("Ingrese un nombre de usuario.") }
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
                    isError = attemptedSave && password.length < 6,
                    supportingText = {
                        Text("Debe contener al menos 6 caracteres.")
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
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar usuario", fontWeight = FontWeight.Bold)
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
            TopAppBar(
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

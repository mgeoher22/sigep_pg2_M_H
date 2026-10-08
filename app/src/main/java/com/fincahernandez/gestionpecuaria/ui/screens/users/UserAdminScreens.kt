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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.remember
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
    val active: Boolean,
    val permissionIds: Set<String>,
    val permissionCount: Int,
    val hasCustomPermissions: Boolean,
    val isCurrentAccount: Boolean = false
)

/** Datos validados que el formulario entrega al contenedor de navegación. */
data class UserFormData(
    val fullName: String,
    val username: String,
    val temporaryPassword: String,
    val roleName: String,
    val active: Boolean,
    val permissionIds: Set<String>
)

/** Listado administrativo desde el cual se crean cuentas y se revisa el RBAC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    users: List<UserUiModel>,
    canManageUsers: Boolean,
    onMenuClick: () -> Unit,
    onCreateUser: () -> Unit,
    onEditUser: (String) -> Unit,
    onViewRolePermissions: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier,
    cloudMode: Boolean = false,
    cloudStatus: String = "",
    onRefresh: () -> Unit = {}
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
            if (canManageUsers) {
                ExtendedFloatingActionButton(
                    onClick = onCreateUser,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Crear usuario") },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            }
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
                        if(cloudMode) {
                            Text(cloudStatus)
                            TextButton(onClick=onRefresh) { Text("Actualizar lista") }
                        }
                        Text(if(cloudMode) "CUENTAS DE LA FINCA" else "CUENTAS LOCALES", fontWeight = FontWeight.Bold)
                        Text(
                            if(cloudMode) "Crea y administra las cuentas de la finca. Los cambios de rol, permisos y estado se guardan en Supabase." else "Los usuarios de esta pantalla controlan el acceso real a la aplicación."
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
                            "Cada rol funciona como plantilla. Al crear una cuenta puede agregar o " +
                                "retirar permisos solamente para ese usuario."
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
                            Text(if(cloudMode) "No hay cuentas cargadas" else "Aún no existen usuarios", fontWeight = FontWeight.Bold)
                            Text(if(cloudMode) "Conéctate y pulsa Actualizar lista." else "Cree la primera cuenta administrativa.")
                            if (canManageUsers) {
                                OutlinedButton(onClick = onCreateUser) { Text("Crear usuario") }
                            }
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
                                Text(if(cloudMode) user.username else "@${user.username}", style = MaterialTheme.typography.bodySmall)
                                Text(user.roleName, color = MaterialTheme.colorScheme.primary)
                                if (user.isCurrentAccount) {
                                    Text(
                                        "TU CUENTA",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (user.hasCustomPermissions) {
                                        "${user.permissionCount} permisos · PERSONALIZADO"
                                    } else {
                                        "${user.permissionCount} permisos · SEGÚN EL ROL"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (user.hasCustomPermissions) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
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
                                if (canManageUsers) {
                                    IconButton(onClick = { onEditUser(user.id) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar ${user.fullName}",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
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
    initialUser: UserUiModel? = null,
    protectOwnAccount: Boolean = false,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier,
    cloudMode: Boolean = false
) {
    val isEditing = initialUser != null
    var fullName by rememberSaveable(initialUser?.id) {
        mutableStateOf(initialUser?.fullName.orEmpty())
    }
    var username by rememberSaveable(initialUser?.id) {
        mutableStateOf(initialUser?.username.orEmpty())
    }
    var password by remember(initialUser?.id) { mutableStateOf("") }
    var selectedRoleName by rememberSaveable(initialUser?.id) {
        mutableStateOf(initialUser?.roleName ?: if(cloudMode) "Operario" else definedRoles.first().name)
    }
    // Se serializa como texto para que la selección sobreviva al giro de la tableta.
    var selectedPermissionsValue by rememberSaveable(initialUser?.id) {
        mutableStateOf(
            (initialUser?.permissionIds ?: definedRoles.first { it.name==selectedRoleName }.permissionIds)
                .sorted()
                .joinToString(",")
        )
    }
    var active by rememberSaveable(initialUser?.id) {
        mutableStateOf(initialUser?.active ?: true)
    }
    var attemptedSave by rememberSaveable(initialUser?.id) { mutableStateOf(false) }

    val normalizedUsername = username.trim().lowercase()
    val usernameExists = normalizedUsername in existingUsernames
        .filterNot { it.equals(initialUser?.username, ignoreCase = true) }
        .map { it.lowercase() }
    val selectedRole = definedRoles.first { it.name == selectedRoleName }
    val selectedPermissionIds = selectedPermissionsValue
        .split(',')
        .filterTo(linkedSetOf()) { it.isNotBlank() }
    val usernameValid = if(cloudMode) com.fincahernandez.gestionpecuaria.data.remote.CloudUserDraft.validEmail(normalizedUsername) else normalizedUsername.matches(Regex("[a-z0-9._-]{3,30}"))
    val passwordValid = if (isEditing) password.isBlank() || password.length >= 8
    else if(cloudMode) com.fincahernandez.gestionpecuaria.data.remote.CloudUserDraft.validPassword(password) else password.length >= 8
    val formValid = fullName.isNotBlank() && usernameValid &&
        !usernameExists && passwordValid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        if (isEditing) "Editar usuario" else "Crear usuario",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                    if (isEditing) {
                        "Actualice los datos, el rol y los permisos de la cuenta."
                    } else {
                        if(cloudMode) "Necesitas internet. Guarda el nombre, correo, contraseña y rol del trabajador en la nube." else "Defina las credenciales iniciales y seleccione el rol del trabajador."
                    },
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
                    enabled = !(isEditing && cloudMode),
                    label = { Text(if(cloudMode) "Correo electrónico *" else "Usuario *") },
                    prefix = if(cloudMode) null else { { Text("@") } },
                    isError = attemptedSave && (!usernameValid || usernameExists),
                    supportingText = when {
                        attemptedSave && !usernameValid -> {
                            { Text(if(cloudMode) "Escribe un correo electrónico válido." else "Use de 3 a 30 letras, números, punto, guion o guion bajo.") }
                        }
                        attemptedSave && usernameExists -> {
                            { Text("Ese usuario ya está registrado.") }
                        }
                        isEditing && cloudMode -> {
                            { Text("El correo de acceso se conserva para evitar cambiar la identidad de la cuenta.") }
                        }
                        else -> null
                    },
                    singleLine = true
                )
            }
            if (!isEditing || !cloudMode) item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (isEditing) "Nueva contraseña (opcional)" else if (cloudMode) "Contraseña inicial *" else "Contraseña temporal *")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = attemptedSave && !passwordValid,
                    supportingText = {
                        Text(
                            if (isEditing) {
                                "Déjela vacía para conservar la actual; una nueva debe tener 8 caracteres."
                            } else {
                                "Debe contener al menos 8 caracteres."
                            }
                        )
                    },
                    singleLine = true
                )
            }
            item {
                RoleDropdown(
                    value = selectedRoleName,
                    enabled = !protectOwnAccount,
                    onSelected = { roleName ->
                        selectedRoleName = roleName
                        // Elegir otro rol vuelve a cargar su plantilla antes de personalizarla.
                        selectedPermissionsValue = definedRoles
                            .first { it.name == roleName }
                            .permissionIds
                            .sorted()
                            .joinToString(",")
                    }
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
                        Text(
                            "Esta es la plantilla base. Los cambios siguientes solo afectarán " +
                                "a esta cuenta."
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Personalizar permisos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Active permisos adicionales o retire los que no necesite.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            selectedPermissionsValue = selectedRole.permissionIds
                                .sorted()
                                .joinToString(",")
                        }
                    ) {
                        Text("Restablecer rol")
                    }
                }
            }
            items(rolePermissions, key = { "user-permission-${it.id}" }) { permission ->
                val checked = permission.id in selectedPermissionIds
                val includedByRole = permission.id in selectedRole.permissionIds
                val isRequired = permission.id == "dashboard" ||
                    (protectOwnAccount && permission.id == "users")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (checked) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint = if (checked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(permission.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    permission.id == "dashboard" ->
                                        "Obligatorio para ingresar a la aplicación"
                                    isRequired -> "Obligatorio en su cuenta administrativa actual"
                                    checked && !includedByRole -> "Permiso adicional al rol"
                                    !checked && includedByRole -> "Retirado para este usuario"
                                    includedByRole -> "Incluido por el rol"
                                    else -> "No incluido por el rol"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = checked,
                            enabled = !isRequired,
                            onCheckedChange = { grant ->
                                val updated = selectedPermissionIds.toMutableSet().apply {
                                    if (grant) add(permission.id) else remove(permission.id)
                                }
                                selectedPermissionsValue = updated.sorted().joinToString(",")
                            }
                        )
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
                            if (isEditing) {
                                "Desactive la cuenta cuando ya no se necesite. Sus datos e historial se conservarán."
                            } else {
                                "Permite iniciar sesión desde su creación."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = active,
                        enabled = !protectOwnAccount,
                        onCheckedChange = { active = it }
                    )
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
                                    active = active,
                                    permissionIds = selectedPermissionIds
                                )
                            )
                            password = ""
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
                        Text(
                            if (isEditing) "Guardar cambios" else "Guardar usuario",
                            fontWeight = FontWeight.Bold
                        )
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
                    "Estos son los permisos preestablecidos de cada rol. Al crear una cuenta " +
                        "pueden personalizarse sin cambiar esta plantilla ni afectar a otros usuarios.",
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
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
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

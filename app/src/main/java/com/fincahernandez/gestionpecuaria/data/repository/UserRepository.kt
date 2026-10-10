package com.fincahernandez.gestionpecuaria.data.repository

import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.UsuarioEntity
import com.fincahernandez.gestionpecuaria.data.security.PasswordHasher
import com.fincahernandez.gestionpecuaria.data.security.GENERAL_ADMIN_ROLE
import com.fincahernandez.gestionpecuaria.data.security.definedRoles
import com.fincahernandez.gestionpecuaria.data.security.permissionsForRole
import com.fincahernandez.gestionpecuaria.data.security.permissionsForUser
import com.fincahernandez.gestionpecuaria.data.security.rolePermissions
import com.fincahernandez.gestionpecuaria.data.security.serializePermissions
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class StoredUser(
    val id: String,
    val fullName: String,
    val username: String,
    val roleName: String,
    val active: Boolean,
    val permissionIds: Set<String>,
    val hasCustomPermissions: Boolean
)

data class AuthenticatedUser(
    val id: String,
    val fullName: String,
    val username: String,
    val roleName: String,
    val permissionIds: Set<String>,
    val passwordChangeRequired: Boolean = false
)

sealed interface AuthenticationResult {
    data class Success(val user: AuthenticatedUser) : AuthenticationResult
    data object InvalidCredentials : AuthenticationResult
    data object InactiveAccount : AuthenticationResult
}

/** Coordina la creación de cuentas y la validación local de credenciales. */
class UserRepository(database: GestionPecuariaDatabase) {
    private val userDao = database.usuarioDao()

    fun observeUsers(): Flow<List<StoredUser>> = userDao.observarUsuarios().map { users ->
        users.map(UsuarioEntity::toStoredUser)
    }

    suspend fun hasUsers(): Boolean = userDao.contarUsuarios() > 0

    suspend fun createUser(
        fullName: String,
        username: String,
        password: String,
        roleName: String,
        active: Boolean = true,
        permissionIds: Set<String>? = null
    ): AuthenticatedUser = withContext(Dispatchers.Default) {
        require(fullName.isNotBlank()) { "El nombre es obligatorio." }
        val normalizedUsername = normalizeUsername(username)
        require(normalizedUsername.isNotBlank()) { "El usuario es obligatorio." }
        require(definedRoles.any { it.name == roleName }) { "El rol no es válido." }
        val validPermissionIds = rolePermissions.mapTo(mutableSetOf()) { it.id }
        val effectivePermissions = permissionIds ?: permissionsForRole(roleName)
        require(effectivePermissions.all { it in validPermissionIds }) {
            "La selección contiene un permiso no válido."
        }
        require("dashboard" in effectivePermissions) {
            "El acceso al panel principal es obligatorio."
        }
        // Si coincide con el rol se guarda NULL para que siga heredando su plantilla.
        val customizedPermissions = effectivePermissions
            .takeIf { it != permissionsForRole(roleName) }
            ?.let(::serializePermissions)
        val protectedPassword = PasswordHasher.protect(password)
        val entity = UsuarioEntity(
            id = UUID.randomUUID().toString(),
            nombreCompleto = fullName.trim(),
            usuario = username.trim(),
            usuarioNormalizado = normalizedUsername,
            passwordHash = protectedPassword.hash,
            passwordSalt = protectedPassword.salt,
            passwordAlgorithm = protectedPassword.algorithm,
            passwordIterations = protectedPassword.iterations,
            rol = roleName,
            activo = active,
            creadoEn = System.currentTimeMillis(),
            permisosPersonalizados = customizedPermissions
        )
        userDao.insertar(entity)
        entity.toAuthenticatedUser()
    }

    suspend fun authenticate(username: String, password: String): AuthenticationResult =
        withContext(Dispatchers.Default) {
            val user = userDao.buscarPorUsuario(normalizeUsername(username))
                ?: return@withContext AuthenticationResult.InvalidCredentials
            if (!user.activo) return@withContext AuthenticationResult.InactiveAccount
            val valid = PasswordHasher.verify(
                password = password,
                expectedHash = user.passwordHash,
                salt = user.passwordSalt,
                algorithm = user.passwordAlgorithm,
                iterations = user.passwordIterations
            )
            if (valid) {
                AuthenticationResult.Success(user.toAuthenticatedUser())
            } else {
                AuthenticationResult.InvalidCredentials
            }
        }

    suspend fun activeUserById(id: String): AuthenticatedUser? = userDao.buscarPorId(id)
        ?.takeIf { it.activo }
        ?.toAuthenticatedUser()

    /**
     * Actualiza datos, rol, permisos y estado de una cuenta conservando su historial.
     * Una contraseña vacía mantiene la credencial actual; solo un Administrador General
     * activo puede ejecutar esta operación.
     */
    suspend fun updateUser(
        actorUserId: String,
        userId: String,
        fullName: String,
        username: String,
        newPassword: String,
        roleName: String,
        active: Boolean,
        permissionIds: Set<String>
    ): AuthenticatedUser = withContext(Dispatchers.Default) {
        val actor = userDao.buscarPorId(actorUserId)
        require(actor?.activo == true && actor.rol == GENERAL_ADMIN_ROLE) {
            "Solo el Administrador General puede editar cuentas y permisos."
        }
        val current = userDao.buscarPorId(userId)
            ?: throw IllegalArgumentException("El usuario que desea editar ya no existe.")
        require(fullName.isNotBlank()) { "El nombre es obligatorio." }
        val normalizedUsername = normalizeUsername(username)
        require(normalizedUsername.isNotBlank()) { "El usuario es obligatorio." }
        require(definedRoles.any { it.name == roleName }) { "El rol no es válido." }
        val validPermissionIds = rolePermissions.mapTo(mutableSetOf()) { it.id }
        require(permissionIds.all { it in validPermissionIds }) {
            "La selección contiene un permiso no válido."
        }
        require("dashboard" in permissionIds) {
            "El acceso al panel principal es obligatorio."
        }
        if (current.id == actor.id) {
            require(active && roleName == GENERAL_ADMIN_ROLE && "users" in permissionIds) {
                "No puede desactivar ni retirar el acceso administrativo de su propia cuenta."
            }
        }

        val usernameOwner = userDao.buscarPorUsuario(normalizedUsername)
        require(usernameOwner == null || usernameOwner.id == current.id) {
            "Ese nombre de usuario ya está registrado."
        }
        val protectedPassword = newPassword
            .takeIf(String::isNotBlank)
            ?.let(PasswordHasher::protect)
        val customizedPermissions = permissionIds
            .takeIf { it != permissionsForRole(roleName) }
            ?.let(::serializePermissions)
        val updated = current.copy(
            nombreCompleto = fullName.trim(),
            usuario = username.trim(),
            usuarioNormalizado = normalizedUsername,
            passwordHash = protectedPassword?.hash ?: current.passwordHash,
            passwordSalt = protectedPassword?.salt ?: current.passwordSalt,
            passwordAlgorithm = protectedPassword?.algorithm ?: current.passwordAlgorithm,
            passwordIterations = protectedPassword?.iterations ?: current.passwordIterations,
            rol = roleName,
            activo = active,
            permisosPersonalizados = customizedPermissions
        )
        userDao.actualizar(updated)
        updated.toAuthenticatedUser()
    }

    /** Borra solo las credenciales para permitir configurar nuevamente el administrador. */
    suspend fun resetLocalAccess() = userDao.eliminarTodos()

    private fun normalizeUsername(value: String): String =
        value.trim().lowercase(Locale.ROOT)
}

private fun UsuarioEntity.toStoredUser() = StoredUser(
    id = id,
    fullName = nombreCompleto,
    username = usuario,
    roleName = rol,
    active = activo,
    permissionIds = permissionsForUser(rol, permisosPersonalizados),
    hasCustomPermissions = permisosPersonalizados != null
)

private fun UsuarioEntity.toAuthenticatedUser() = AuthenticatedUser(
    id = id,
    fullName = nombreCompleto,
    username = usuario,
    roleName = rol,
    permissionIds = permissionsForUser(rol, permisosPersonalizados)
)

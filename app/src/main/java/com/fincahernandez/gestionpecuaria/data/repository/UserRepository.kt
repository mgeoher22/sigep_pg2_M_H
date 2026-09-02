package com.fincahernandez.gestionpecuaria.data.repository

import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.UsuarioEntity
import com.fincahernandez.gestionpecuaria.data.security.PasswordHasher
import com.fincahernandez.gestionpecuaria.data.security.definedRoles
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
    val active: Boolean
)

data class AuthenticatedUser(
    val id: String,
    val fullName: String,
    val username: String,
    val roleName: String
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
        active: Boolean = true
    ): AuthenticatedUser = withContext(Dispatchers.Default) {
        require(fullName.isNotBlank()) { "El nombre es obligatorio." }
        val normalizedUsername = normalizeUsername(username)
        require(normalizedUsername.isNotBlank()) { "El usuario es obligatorio." }
        require(definedRoles.any { it.name == roleName }) { "El rol no es válido." }
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
            creadoEn = System.currentTimeMillis()
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
    active = activo
)

private fun UsuarioEntity.toAuthenticatedUser() = AuthenticatedUser(
    id = id,
    fullName = nombreCompleto,
    username = usuario,
    roleName = rol
)

package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticationResult
import com.fincahernandez.gestionpecuaria.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Conserva cuentas y operaciones de acceso aunque la actividad cambie de orientación. */
class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository(
        GestionPecuariaDatabase.obtenerInstancia(application)
    )

    val users = repository.observeUsers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun hasUsers(): Boolean = repository.hasUsers()

    suspend fun activeUserById(id: String): AuthenticatedUser? = repository.activeUserById(id)

    suspend fun createUser(
        fullName: String,
        username: String,
        password: String,
        roleName: String,
        active: Boolean = true,
        permissionIds: Set<String>? = null
    ): AuthenticatedUser = repository.createUser(
        fullName = fullName,
        username = username,
        password = password,
        roleName = roleName,
        active = active,
        permissionIds = permissionIds
    )

    suspend fun authenticate(username: String, password: String): AuthenticationResult =
        repository.authenticate(username, password)

    suspend fun updateUser(
        actorUserId: String,
        userId: String,
        fullName: String,
        username: String,
        newPassword: String,
        roleName: String,
        active: Boolean,
        permissionIds: Set<String>
    ): AuthenticatedUser = repository.updateUser(
        actorUserId = actorUserId,
        userId = userId,
        fullName = fullName,
        username = username,
        newPassword = newPassword,
        roleName = roleName,
        active = active,
        permissionIds = permissionIds
    )

    suspend fun resetLocalAccess() = repository.resetLocalAccess()
}

package com.fincahernandez.gestionpecuaria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica las credenciales contra una base aislada, sin tocar los datos de la finca. */
@RunWith(AndroidJUnit4::class)
class UserRepositoryTest {
    private lateinit var database: GestionPecuariaDatabase
    private lateinit var repository: UserRepository

    @Before
    fun prepare() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            GestionPecuariaDatabase::class.java
        ).build()
        repository = UserRepository(database)
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun validPasswordAuthenticatesAndPlainTextIsNotStored() = runBlocking {
        repository.createUser(
            fullName = "Administrador de prueba",
            username = "Admin.Test",
            password = "ClaveSegura123",
            roleName = "Administrador General"
        )

        val success = repository.authenticate("admin.test", "ClaveSegura123")
        val failure = repository.authenticate("admin.test", "otra-clave")
        val stored = database.usuarioDao().buscarPorUsuario("admin.test")!!

        assertTrue(success is AuthenticationResult.Success)
        assertTrue(failure is AuthenticationResult.InvalidCredentials)
        assertNotEquals("ClaveSegura123", stored.passwordHash)
        assertTrue(stored.passwordSalt.isNotBlank())
    }

    @Test
    fun inactiveAccountCannotAuthenticate() = runBlocking {
        repository.createUser(
            fullName = "Usuario inactivo",
            username = "inactivo",
            password = "ClaveSegura123",
            roleName = "Operario",
            active = false
        )

        val result = repository.authenticate("inactivo", "ClaveSegura123")

        assertTrue(result is AuthenticationResult.InactiveAccount)
    }

    @Test
    fun generalAdministratorCanEditPermissionsAndDeactivateOrReactivateUser() = runBlocking {
        val administrator = repository.createUser(
            fullName = "Administrador",
            username = "admin",
            password = "ClaveSegura123",
            roleName = "Administrador General"
        )
        val worker = repository.createUser(
            fullName = "Trabajador",
            username = "trabajador",
            password = "ClaveTrabajador123",
            roleName = "Operario"
        )
        val customizedPermissions = setOf("dashboard", "animals", "health", "reports", "lots")

        repository.updateUser(
            actorUserId = administrator.id,
            userId = worker.id,
            fullName = "Trabajador actualizado",
            username = "tecnico.campo",
            newPassword = "",
            roleName = "Técnico Veterinario",
            active = false,
            permissionIds = customizedPermissions
        )

        val inactiveResult = repository.authenticate("tecnico.campo", "ClaveTrabajador123")
        val storedInactive = database.usuarioDao().buscarPorId(worker.id)!!
        assertTrue(inactiveResult is AuthenticationResult.InactiveAccount)
        assertEquals("Trabajador actualizado", storedInactive.nombreCompleto)
        assertFalse(storedInactive.activo)

        repository.updateUser(
            actorUserId = administrator.id,
            userId = worker.id,
            fullName = "Trabajador actualizado",
            username = "tecnico.campo",
            newPassword = "",
            roleName = "Técnico Veterinario",
            active = true,
            permissionIds = customizedPermissions
        )

        val reactivated = repository.authenticate("tecnico.campo", "ClaveTrabajador123")
        assertTrue(reactivated is AuthenticationResult.Success)
        assertEquals(
            customizedPermissions,
            (reactivated as AuthenticationResult.Success).user.permissionIds
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonGeneralAdministratorCannotEditUsers() = runBlocking {
        val operator = repository.createUser(
            fullName = "Operario",
            username = "operario",
            password = "ClaveSegura123",
            roleName = "Operario"
        )

        repository.updateUser(
            actorUserId = operator.id,
            userId = operator.id,
            fullName = "Operario",
            username = "operario",
            newPassword = "",
            roleName = "Operario",
            active = false,
            permissionIds = setOf("dashboard")
        )
        Unit
    }

    @Test(expected = IllegalArgumentException::class)
    fun administratorCannotDeactivateOwnAccount() = runBlocking {
        val administrator = repository.createUser(
            fullName = "Administrador",
            username = "admin",
            password = "ClaveSegura123",
            roleName = "Administrador General"
        )

        repository.updateUser(
            actorUserId = administrator.id,
            userId = administrator.id,
            fullName = "Administrador",
            username = "admin",
            newPassword = "",
            roleName = "Administrador General",
            active = false,
            permissionIds = setOf("dashboard", "users")
        )
        Unit
    }
}

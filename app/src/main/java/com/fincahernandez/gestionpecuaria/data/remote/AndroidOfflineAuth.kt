package com.fincahernandez.gestionpecuaria.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.UsuarioEntity
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.security.ProtectedPassword
import com.fincahernandez.gestionpecuaria.data.security.serializePermissions
import java.util.Locale

fun createOfflineAuth(context: Context, manager: CloudSessionManager): OfflineFirstAuth {
    val app=context.applicationContext
    val database=GestionPecuariaDatabase.obtenerInstancia(app)
    val users=database.usuarioDao()
    return OfflineFirstAuth(
        gateway=object:OfflineFirstAuth.Gateway {
            override suspend fun login(email:String,password:String)=manager.signInCloud(email,password)
            override suspend fun restore(localId:String)=manager.restore(localId)
            override suspend fun changePassword(localId:String,change:CloudPasswordChange)=
                manager.changeOwnPassword(localId,change)
            override fun clear()=manager.clearLocalSession()
        },
        cache=EncryptedCloudSessionStore(app,"cloud-offline-user.bin"),
        catalog=object:OfflineFirstAuth.Catalog {
            override suspend fun active(id:String)=users.buscarPorId(id)?.activo==true
            override suspend fun save(account:CloudAccount,password:ProtectedPassword):AuthenticatedUser=database.withTransaction {
                val previous=users.buscarPorId(account.localUserId)
                val email=account.email.lowercase(Locale.ROOT)
                val row=UsuarioEntity(account.localUserId,account.displayName?.takeIf { it.isNotBlank() && '@' !in it }
                    ?: previous?.nombreCompleto?.takeIf { it.isNotBlank() && '@' !in it } ?: "Usuario",
                    previous?.usuario?:email,previous?.usuarioNormalizado?:email,
                    password.hash,password.salt,password.algorithm,password.iterations,account.role,true,
                    previous?.creadoEn?:System.currentTimeMillis(),serializePermissions(account.permissions))
                if(previous==null) users.insertar(row) else users.actualizar(row)
                AuthenticatedUser(
                    row.id,row.nombreCompleto,email,row.rol,account.permissions,
                    account.passwordChangeRequired
                )
            }
        },
        online={
            val connection=app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connection.getNetworkCapabilities(connection.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
        }
    )
}

package com.fincahernandez.gestionpecuaria.data.remote

import com.fincahernandez.gestionpecuaria.BuildConfig
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.security.PasswordHasher
import com.fincahernandez.gestionpecuaria.data.security.ProtectedPassword
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.NoRouteToHostException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

/** Prueba offline separada del token: solo se crea después de autenticar en Supabase. */
class OfflineFirstAuth(
    private val gateway: Gateway,
    private val cache: CloudSessionStore,
    private val catalog: Catalog,
    private val online: () -> Boolean,
    private val project: String = BuildConfig.SUPABASE_URL
) {
    interface Gateway {
        suspend fun login(email: String, password: String): CloudAccount
        suspend fun restore(localId: String): CloudAccount?
        fun clear(): () -> Boolean
    }
    interface Catalog {
        suspend fun save(account: CloudAccount, password: ProtectedPassword): AuthenticatedUser
        suspend fun active(id: String): Boolean
    }
    private val mutex = Mutex()
    private fun cached(): JSONObject? = try {
        cache.read()?.let(::JSONObject)?.takeIf { it.getString("project") == project }
    } catch (_: Exception) { cache.clear(); null }
    private fun profile(j: JSONObject): AuthenticatedUser {
        val p=j.getJSONArray("permissions")
        return AuthenticatedUser(j.getString("id"),j.getString("name").takeIf { it.isNotBlank() && '@' !in it } ?: "Usuario",j.getString("email"),j.getString("role"),
            (0 until p.length()).map { p.getString(it) }.toSet())
    }
    private fun verifier(j: JSONObject) = ProtectedPassword(j.getString("hash"),j.getString("salt"),j.getString("algorithm"),j.getInt("iterations"))
    private fun encode(user: AuthenticatedUser, proof: ProtectedPassword) = JSONObject()
        .put("project",project).put("id",user.id).put("name",user.fullName).put("email",user.username)
        .put("role",user.roleName).put("permissions",JSONArray(user.permissionIds.sorted()))
        .put("hash",proof.hash).put("salt",proof.salt).put("algorithm",proof.algorithm).put("iterations",proof.iterations)
    private fun unavailable(e: Exception) = e is UnknownHostException || e is ConnectException || e is SocketTimeoutException || e is NoRouteToHostException
    private fun revoked(e: Exception) = e is CloudAccessDenied || (e is CloudHttpException && e.status in listOf(400,401,403,422))
    private suspend fun offline(email: String, password: String): AuthenticatedUser {
        val j=cached() ?: throw CloudAccessDenied("El primer ingreso requiere internet. Conéctate e ingresa con tu correo de Supabase.")
        if (!j.getString("email").equals(email,true)) throw CloudAccessDenied("Esta cuenta todavía no se ha validado en la tablet. Necesita internet para el primer ingreso.")
        val proof=verifier(j)
        if (!PasswordHasher.verify(password,proof.hash,proof.salt,proof.algorithm,proof.iterations))
            throw CloudAccessDenied("Correo o contraseña incorrectos.")
        val user=profile(j)
        if (!catalog.active(user.id)) throw CloudAccessDenied("Esta cuenta está inactiva en la tablet.")
        return user
    }
    suspend fun login(email: String, password: String): AuthenticatedUser = withContext(Dispatchers.IO) {
        mutex.withLock {
            val normalized=email.trim().lowercase(Locale.ROOT)
            require(normalized.isNotBlank() && password.isNotEmpty()) { "Escribe el correo y la contraseña." }
            if (!online()) return@withLock offline(normalized,password)
            val account=try { gateway.login(normalized,password) } catch(e: Exception) {
                if (unavailable(e)) return@withLock offline(normalized,password)
                if (revoked(e) && cached()?.optString("email").equals(normalized,true)) {
                    cache.clear(); gateway.clear()
                }
                throw e
            }
            val proof=PasswordHasher.protectValidatedCloudPassword(password)
            val user=catalog.save(account,proof)
            cache.write(encode(user,proof).toString())
            user
        }
    }
    suspend fun remembered(id: String): AuthenticatedUser? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val j=cached()?.takeIf { it.optString("id")==id } ?: return@withLock null
            if (!catalog.active(id)) return@withLock null
            if (!online()) return@withLock profile(j)
            val account=try { gateway.restore(id) } catch(e: Exception) {
                if (unavailable(e)) return@withLock profile(j)
                if (revoked(e)) { cache.clear(); gateway.clear() }
                // A server or validation error must not be reported as offline authorization.
                return@withLock null
            } ?: return@withLock null
            val user=catalog.save(account,verifier(j))
            cache.write(encode(user,verifier(j)).toString())
            user
        }
    }
    fun signOut(): () -> Boolean = gateway.clear()
}

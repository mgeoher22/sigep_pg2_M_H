package com.fincahernandez.gestionpecuaria.data.remote

import com.fincahernandez.gestionpecuaria.BuildConfig
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.security.permissionsForUser
import java.io.IOException
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CloudAccount(val email: String, val userId: String, val localUserId: String,
    val role: String, val permissions: Set<String>, val displayName: String? = null)

/** Una sesión por dispositivo. Room nunca recibe tokens ni contraseñas remotas. */
class CloudSessionManager(
    private val store: CloudSessionStore,
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publicKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    private val transport: SupabaseConnectionProbe.Transport = SupabaseConnectionProbe.HttpsTransport(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private val stateLock = Any()
    private var generation = 0L
    private fun save(s: JSONObject, expected: Long) = synchronized(stateLock) {
        if (generation != expected) throw CancellationException("La sesión se cerró durante la solicitud")
        store.write(s.toString())
    }
    private fun root(): String {
        val uri = URI(baseUrl)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.query == null && uri.fragment == null && uri.path.orEmpty() in listOf("", "/")) { "Configuración de nube no válida." }
        require(publicKey.startsWith("sb_publishable_") && !publicKey.contains('\n') && !publicKey.contains('\r')) { "Clave pública no válida." }
        return baseUrl.trimEnd('/')
    }
    private fun request(path: String, token: String? = null, body: JSONObject? = null, method: String = if (body == null) "GET" else "POST"): String =
        transport.request(root() + path, method,
            mapOf("apikey" to publicKey, "Accept" to "application/json") +
                (token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()), body?.toString())

    private fun tokens(response: String, localId: String): JSONObject {
        val j = JSONObject(response)
        val uid = j.getJSONObject("user").getString("id")
        UUID.fromString(uid)
        require(j.getString("access_token").isNotBlank() && j.getString("refresh_token").isNotBlank())
        val expires = j.optLong("expires_at", 0).takeIf { it > 0 }
            ?: (now() / 1000 + j.getLong("expires_in"))
        return JSONObject().put("project", root()).put("localId", localId).put("uid", uid)
            .put("email", j.getJSONObject("user").optString("email", ""))
            .put("displayName", metadataName(j.getJSONObject("user")))
            .put("access", j.getString("access_token")).put("refresh", j.getString("refresh_token"))
            .put("expires", expires)
    }
    private fun load(localId: String): JSONObject? {
        val text = store.read() ?: return null
        return try {
            val j = JSONObject(text)
            if (j.getString("project") != root() || j.getString("localId") != localId) null else j
        } catch (_: Exception) { store.clear(); null }
    }
    private fun member(session: JSONObject): JSONObject {
        val uid = session.getString("uid")
        UUID.fromString(uid)
        val rows = JSONArray(request("/rest/v1/miembros_finca?usuarioId=eq.$uid&select=usuarioId,activo,usuarioLocalId,nombreCompleto,usuario,rol,permisosPersonalizados", session.getString("access")))
        if (rows.length() != 1) throw CloudAccessDenied("Esta cuenta no tiene acceso a la finca.")
        val m = rows.getJSONObject(0)
        if (m.getString("usuarioId") != uid || !m.getBoolean("activo")) throw CloudAccessDenied("La cuenta de nube está inactiva o no está autorizada.")
        return m
    }
    private fun account(s: JSONObject, m: JSONObject): CloudAccount {
        if ((m.isNull("usuarioLocalId") && s.getString("localId") != s.getString("uid")) ||
            (!m.isNull("usuarioLocalId") && m.getString("usuarioLocalId") != s.getString("localId")))
            throw CloudAccessDenied("La cuenta de nube no está vinculada a este usuario local. Solicita al administrador revisar el vínculo.")
        return CloudAccount(s.getString("email"), s.getString("uid"), s.getString("localId"),
            m.getString("rol"), permissionsForUser(m.getString("rol"),
                if (m.isNull("permisosPersonalizados")) null else m.getString("permisosPersonalizados")),
            personName(s.optString("displayName")) ?: personName(m.optString("nombreCompleto")))
    }

    suspend fun signIn(local: AuthenticatedUser, email: String, password: String): CloudAccount = login(local, email, password)

    suspend fun signInCloud(email: String, password: String): CloudAccount = login(null, email, password)

    private suspend fun login(local: AuthenticatedUser?, email: String, password: String): CloudAccount = withContext(Dispatchers.IO) {
        mutex.withLock {
            require(email.trim().isNotEmpty() && password.isNotEmpty()) { "Escribe tu correo y contraseña de nube." }
            local?.let { UUID.fromString(it.id) }
            // A transport failure must not destroy a previously saved refresh token.
            val epoch = synchronized(stateLock) { generation }
            val s = tokens(request("/auth/v1/token?grant_type=password", body = JSONObject().put("email", email.trim()).put("password", password)), local?.id.orEmpty())
            try {
                var m = member(s)
                val localId = local?.id ?: if (!m.isNull("usuarioLocalId")) m.getString("usuarioLocalId") else s.getString("uid")
                UUID.fromString(localId)
                s.put("localId", localId)
                if (m.isNull("usuarioLocalId") && local != null) {
                    val permissions = permissionsForUser(m.getString("rol"), if (m.isNull("permisosPersonalizados")) null else m.getString("permisosPersonalizados"))
                    if (m.getString("rol") != "Administrador General" || "users" !in permissions)
                        throw CloudAccessDenied("El administrador debe vincular esta cuenta de nube con el identificador: $localId")
                    // The installed RPC replaces the entire profile: preserve every current field.
                    request("/rest/v1/rpc/administrar_miembro", s.getString("access"), JSONObject()
                        .put("p_usuario_id", s.getString("uid")).put("p_rol", m.getString("rol"))
                        .put("p_activo", true).put("p_permisos", m.opt("permisosPersonalizados") ?: JSONObject.NULL)
                        .put("p_usuario_local_id", localId)
                        .put("p_nombre_completo", m.opt("nombreCompleto") ?: JSONObject.NULL)
                        .put("p_usuario", m.opt("usuario") ?: JSONObject.NULL))
                    m = member(s)
                }
                val profile = account(s, m)
                coroutineContext.ensureActive()
                save(s, epoch)
                profile
            } catch (error: Exception) {
                synchronized(stateLock) { if (generation == epoch) store.clear() }
                // Best effort: invalid membership must not leave reusable credentials on disk.
                runCatching { request("/auth/v1/logout?scope=local", s.getString("access"), JSONObject()) }
                throw error
            }
        }
    }

    /** Called on restore and before future cloud work; permission checks are always online. */
    suspend fun restore(localId: String): CloudAccount? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val epoch = synchronized(stateLock) { generation }
            var s = load(localId) ?: return@withLock null
            try {
                fun refresh() {
                    val updated = tokens(request("/auth/v1/token?grant_type=refresh_token", body = JSONObject().put("refresh_token", s.getString("refresh"))), localId)
                    if (updated.getString("uid") != s.getString("uid")) throw CloudAccessDenied("La sesión cambió de usuario; vuelve a ingresar.")
                    s = updated
                    // Persist the rotated refresh token immediately, before subsequent requests.
                    save(s, epoch)
                }
                var refreshed = false
                if (s.getLong("expires") <= now() / 1000 + 60) { refresh(); refreshed = true }
                val m = try { member(s) } catch (e: CloudHttpException) {
                    if (e.status != 401 || refreshed) throw e
                    refresh(); member(s)
                }
                synchronized(stateLock) {
                    if (generation != epoch) throw CancellationException("Sesión cerrada")
                    account(s, m)
                }
            } catch (e: CloudAccessDenied) { synchronized(stateLock) { if (generation == epoch) store.clear() }; throw e }
            catch (e: CloudHttpException) {
                if (e.status in listOf(400, 401, 403, 422)) synchronized(stateLock) { if (generation == epoch) store.clear() }
                throw e
            }
        }
    }


    private fun personName(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() && it != "null" && '@' !in it }
    private fun metadataName(user: JSONObject): String? {
        val data=user.optJSONObject("user_metadata") ?: return null
        return listOf("full_name","name","display_name").firstNotNullOfOrNull { personName(data.optString(it)) }
    }
    suspend fun updateDisplayName(localId: String, name: String) = withContext(Dispatchers.IO) {
        require(name.trim().length in 1..120 && personName(name)!=null) { "Escribe tu nombre (hasta 120 caracteres)." }
        restore(localId) ?: throw CloudAccessDenied("Vuelve a ingresar con internet.")
        mutex.withLock {
            val epoch=synchronized(stateLock) { generation }
            val s=load(localId) ?: throw CloudAccessDenied("La sesión se cerró.")
            val user=JSONObject(request("/auth/v1/user",s.getString("access"),JSONObject().put("data",JSONObject().put("full_name",name.trim())),"PUT"))
            require(user.getString("id")==s.getString("uid"))
            s.put("displayName",metadataName(user) ?: name.trim())
            save(s,epoch)
        }
    }
    fun validateSnapshot(snapshot: CloudDownloadSnapshot) = synchronized(stateLock) {
        val s=load(snapshot.account.localUserId)
        if(generation!=snapshot.generation || s?.optString("uid")!=snapshot.account.userId)
            throw CancellationException("La sesión cambió durante la descarga.")
    }
    suspend fun download(localId: String, forSync: Boolean = false): CloudDownloadSnapshot = withContext(Dispatchers.IO) {
        restore(localId) ?: throw CloudAccessDenied("Vuelve a ingresar con internet para descargar.")
        mutex.withLock {
            val epoch=synchronized(stateLock) { generation }
            val s=load(localId) ?: throw CloudAccessDenied("La sesión se cerró.")
            val profile=account(s,member(s))
            val token=s.getString("access")
            fun state()=JSONObject(request("/rest/v1/rpc/estado_sincronizacion",token,JSONObject()))
            val before=state()
            require(before.getString("versionEsquema")=="20261005_room20_insumos") {
                "La nube requiere ejecutar 18_agregar_modulo_insumos.sql antes de sincronizar."
            }
            val rows=linkedMapOf<String,List<JSONObject>>()
            var total=0
            for(table in CloudTables.readable(profile.permissions)) {
                val records=mutableListOf<JSONObject>()
                val seen=mutableSetOf<String>()
                var last:String?=null
                while(true) {
                    coroutineContext.ensureActive()
                    val fields=table.columns.joinToString(",") { it.name } + if(forSync) ",syncVersion,syncEliminado" else ""
                    val page=JSONArray(request("/rest/v1/"+table.name+"?select="+fields+(if(forSync) "" else "&syncEliminado=eq.false")+"&order=id.asc&limit=500"+(last?.let { "&id=gt."+it } ?: ""),token))
                    if(page.length()==0)break
                    for(i in 0 until page.length()) {
                        val row=page.getJSONObject(i)
                        val id=row.get("id").toString()
                        if(table.name=="configuracion_leche")require(id.toIntOrNull()!=null) else UUID.fromString(id)
                        require(seen.add(id)) { "La nube devolvió registros repetidos." }
                        records.add(row);last=id;total++
                        require(total<=50000) { "La descarga es demasiado grande para esta operación inicial." }
                    }
                }
                rows[table.name]=records
            }
            val after=state()
            require(before.getLong("cursor")==after.getLong("cursor") && before.getString("versionEsquema")==after.getString("versionEsquema")) {
                "Hubo cambios en la nube durante la descarga. Inténtalo otra vez."
            }
            val finalProfile=account(s,member(s))
            require(profile==finalProfile) { "Tus permisos cambiaron; vuelve a ingresar." }
            CloudDownloadSnapshot(rows,profile,epoch).also(::validateSnapshot)
        }
    }


    fun syncProject(): String = root()
    suspend fun sendSync(operation: SyncOperation, snapshot: CloudDownloadSnapshot): Map<SyncKey, Long> = withContext(Dispatchers.IO) {
        validateSnapshot(snapshot)
        restore(snapshot.account.localUserId) ?: throw CloudAccessDenied("Vuelve a ingresar con internet.")
        mutex.withLock {
            validateSnapshot(snapshot)
            require(operation.owner == snapshot.account.userId)
            val s = load(snapshot.account.localUserId) ?: throw CloudAccessDenied("La sesión se cerró.")
            val current = account(s, member(s))
            require(current.userId == operation.owner && current.permissions == snapshot.account.permissions && current.role == snapshot.account.role) {
                "Tus permisos cambiaron. Vuelve a ingresar antes de sincronizar."
            }
            val response = try {
                request("/rest/v1/rpc/sincronizar_cambios", s.getString("access"), SyncCodec.request(operation))
            } catch (e: CloudHttpException) {
                // Only definitive transaction rejection releases the saved UUID.
                // Timeouts and 5xx responses keep it for an identical retry.
                if(e.status in setOf(400,403,404,409,413,422)) throw SyncRejected(
                    if(e.status==409) "Los datos cambiaron en la nube o hay un código repetido. Pulsa Sincronizar otra vez para revisar."
                    else "La nube rechazó el envío. Revisa los permisos y los datos del registro; tus cambios siguen guardados en el dispositivo.")
                throw e
            }
            val list = JSONArray(response)
            val versions = linkedMapOf<SyncKey, Long>()
            for(i in 0 until list.length()) {
                val row = list.getJSONObject(i)
                val key = SyncKey(row.getString("tabla"),row.get("id").toString())
                require(!versions.containsKey(key)) { "Recibo duplicado." }
                versions[key]=row.getLong("version").also { require(it>0) }
            }
            versions
        }
    }


    private fun managedUser(row: JSONObject): CloudManagedUser {
        val custom = if(row.isNull("permissions")) null else row.getString("permissions")
        return CloudManagedUser(row.getString("id"),row.getString("name"),row.getString("email"),
            row.getString("role"),row.getBoolean("active"),permissionsForUser(row.getString("role"),custom),custom!=null)
    }
    private suspend fun manageUsers(localId: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        restore(localId) ?: throw CloudAccessDenied("Ingresa con internet para administrar cuentas.")
        mutex.withLock {
            val epoch = synchronized(stateLock) { generation }
            val s = load(localId) ?: throw CloudAccessDenied("La sesión se cerró.")
            val profile = account(s,member(s))
            require(profile.role == "Administrador General" && "users" in profile.permissions) {
                "Solo el Administrador General con permiso de Usuarios puede crear cuentas."
            }
            val result = try { JSONObject(request("/functions/v1/gestionar-usuarios",s.getString("access"),body)) }
            catch(e:CloudHttpException) {
                throw CloudAccessDenied(when(e.status) {
                    404 -> "Falta activar la función gestionar-usuarios en Supabase."
                    409 -> "Ese correo ya está registrado. Actualiza la lista antes de intentar otra alta."
                    400,422 -> "Revisa el nombre, correo, contraseña y permisos. Supabase rechazó los datos."
                    401,403 -> "La sesión no tiene autorización. Ingresa otra vez con el administrador."
                    else -> if(body.optString("action")=="create") "No se pudo confirmar el alta. Actualiza Usuarios antes de reintentar; revisa Supabase si el correo ya existe."
                        else "No se pudo cargar la lista de Supabase. Revisa internet y vuelve a intentarlo."
                })
            }
            coroutineContext.ensureActive()
            synchronized(stateLock) { if(generation!=epoch)throw CancellationException("La sesión se cerró") }
            result
        }
    }
    suspend fun listCloudUsers(localId: String): List<CloudManagedUser> {
        val rows = manageUsers(localId,JSONObject().put("action","list")).getJSONArray("users")
        return (0 until rows.length()).map {managedUser(rows.getJSONObject(it))}
    }
    suspend fun createCloudUser(localId: String, draft: CloudUserDraft): CloudManagedUser {
        val d = draft.validated()
        return managedUser(manageUsers(localId,JSONObject().put("action","create")
            .put("name",d.name).put("email",d.email).put("password",d.password)
            .put("role",d.role).put("active",d.active).put("permissionIds",JSONArray(d.permissions.sorted()))))
    }

    /** Local deletion precedes network revocation, so offline logout is effective locally. */
    fun clearLocalSession(): () -> Boolean {
        val text = synchronized(stateLock) {
            generation++
            val text = store.read()
            store.clear()
            text
        }
        // Run this returned revocation on IO; the local session is already gone.
        return revoke@{
            if (text == null) return@revoke true
            try {
                val s = JSONObject(text)
                if (s.getString("project") != root()) return@revoke true
                request("/auth/v1/logout?scope=local", s.getString("access"), JSONObject())
                true
            } catch (_: Exception) { false }
        }
    }
    suspend fun signOut(): Boolean {
        val revoke = clearLocalSession()
        return withContext(Dispatchers.IO) { revoke() }
    }
}

class CloudAccessDenied(message: String) : IOException(message)
class CloudHttpException(val status: Int, message: String) : IOException(message)

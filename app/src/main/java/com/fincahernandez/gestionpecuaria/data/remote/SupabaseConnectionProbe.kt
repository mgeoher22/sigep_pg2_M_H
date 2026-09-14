package com.fincahernandez.gestionpecuaria.data.remote

import com.fincahernandez.gestionpecuaria.BuildConfig
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Diagnóstico de lectura. No persiste credenciales ni modifica los datos productivos. */
class SupabaseConnectionProbe(
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publicKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    private val transport: Transport = HttpsTransport()
) {
    fun interface Transport {
        fun request(url: String, method: String, headers: Map<String, String>, body: String?): String
    }

    suspend fun check(email: String, password: String): Int = withContext(Dispatchers.IO) {
        val uri = runCatching { URI(baseUrl) }.getOrNull()
        require(uri != null && uri.scheme == "https" && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && uri.query == null && uri.fragment == null &&
            (uri.path.isNullOrEmpty() || uri.path == "/")) {
            "Revisa la URL base de Supabase en local.properties."
        }
        require(publicKey.startsWith("sb_publishable_") && !publicKey.contains('\n') &&
            !publicKey.contains('\r')) { "Revisa la clave pública de Supabase en local.properties." }
        require(email.trim().isNotEmpty() && password.isNotEmpty()) {
            "Escribe el correo y la contraseña de Supabase."
        }
        val root = baseUrl.trimEnd('/')
        val headers = mapOf("apikey" to publicKey, "Accept" to "application/json")
        try {
            coroutineContext.ensureActive()
            val session = JSONObject(transport.request(
                "$root/auth/v1/token?grant_type=password", "POST", headers,
                JSONObject().put("email", email.trim()).put("password", password).toString()
            ))
            val token = session.getString("access_token")
            val userId = session.getJSONObject("user").getString("id")
            require(token.isNotBlank() && userId.isNotBlank()) { "La sesión recibida no es válida." }
            val authenticatedHeaders = headers + ("Authorization" to "Bearer $token")
            coroutineContext.ensureActive()
            val members = JSONArray(transport.request(
                "$root/rest/v1/miembros_finca?select=usuarioId,activo", "GET", authenticatedHeaders, null
            ))
            val allowed = (0 until members.length()).any {
                val member = members.getJSONObject(it)
                member.optString("usuarioId") == userId && member.optBoolean("activo", false)
            }
            check(allowed) { "El usuario no está autorizado o está inactivo en miembros_finca." }

            val tables = listOf("animales", "lotes", "lote_animales", "pesajes", "eventos_sanitarios")
            for (table in tables) {
                coroutineContext.ensureActive()
                // Incluso una tabla vacía debe devolver un arreglo JSON válido.
                JSONArray(transport.request(
                    "$root/rest/v1/$table?select=id&limit=1", "GET", authenticatedHeaders, null
                ))
            }
            tables.size
        } catch (error: SocketTimeoutException) {
            throw IOException("La conexión tardó demasiado. Revisa internet e intenta otra vez.", error)
        } catch (error: JSONException) {
            throw IOException("Supabase devolvió una respuesta inesperada.", error)
        }
    }

    class HttpsTransport : Transport {
        override fun request(
            url: String, method: String, headers: Map<String, String>, body: String?
        ): String {
            val connection = URI(url).toURL().openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                if (status !in 200..299) {
                    // No mostrar cuerpos de error que puedan incluir datos sensibles.
                    val message = when {
                        url.contains("/auth/v1/token") && status in listOf(400, 401, 422) ->
                            "No se pudo iniciar sesión. Revisa el correo, la contraseña, la confirmación del usuario y la clave pública."
                        status == 401 -> "Supabase rechazó la clave pública o la sesión."
                        status == 403 -> "Acceso denegado. Revisa los permisos de las tablas."
                        status == 404 -> "No se encontró el proyecto o alguna tabla."
                        status == 429 -> "Demasiados intentos. Espera antes de volver a probar."
                        else -> "No se pudo completar la consulta a Supabase (HTTP $status)."
                    }
                    throw CloudHttpException(status, message)
                }
                return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}

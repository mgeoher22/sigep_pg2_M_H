package com.fincahernandez.gestionpecuaria.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupabaseConnectionProbeTest {
    private val base = "https://test.supabase.co"
    private val key = "sb_publishable_test"
    private val uid = "388edc1d-cbe0-4066-9ae7-43ba820f985f"
    private val session = """{"access_token":"test-session","user":{"id":"$uid"}}"""

    @Test fun authorizedUserReadsFiveTablesUsingSessionWithoutWriting() = runBlocking {
        val calls = mutableListOf<String>()
        val transport = SupabaseConnectionProbe.Transport { url, method, headers, body ->
            calls.add(url)
            assertEquals(key, headers["apikey"])
            if (calls.size == 1) {
                assertEquals("POST", method)
                assertNull(headers["Authorization"])
                assertEquals("secret-test", JSONObject(body!!).getString("password"))
                session
            } else {
                assertEquals("GET", method)
                assertNull(body)
                assertEquals("Bearer test-session", headers["Authorization"])
                if (url.contains("miembros_finca")) """[{"usuarioId":"$uid","activo":true}]""" else "[]"
            }
        }
        assertEquals(5, SupabaseConnectionProbe(base, key, transport).check("test@example.com", "secret-test"))
        assertEquals(7, calls.size)
        assertTrue(calls.last().contains("/eventos_sanitarios?"))
    }

    @Test fun inactiveAndMissingAndDifferentMembersNeverReadAnimalTables() = runBlocking {
        for (membership in listOf(
            "[]",
            """[{"usuarioId":"$uid","activo":false}]""",
            """[{"usuarioId":"another-user","activo":true}]"""
        )) {
            var calls = 0
            val probe = SupabaseConnectionProbe(base, key) { _, _, _, _ ->
                calls++
                if (calls == 1) session else membership
            }
            try {
                probe.check("test@example.com", "secret-test")
                fail("Debe rechazar al usuario sin autorización.")
            } catch (_: IllegalStateException) { }
            assertEquals(2, calls)
        }
    }

    @Test fun failedAuthenticationDoesNotQueryDatabase() = runBlocking {
        var calls = 0
        val probe = SupabaseConnectionProbe(base, key) { _, _, _, _ ->
            calls++
            throw IOException("Credenciales rechazadas")
        }
        try {
            probe.check("test@example.com", "incorrect")
            fail("Debe propagar el rechazo.")
        } catch (_: IOException) { }
        assertEquals(1, calls)
    }

    @Test fun invalidConfigurationNeverSendsCredentials() = runBlocking {
        for ((url, apiKey) in listOf(
            "http://test.supabase.co" to key,
            "$base/rest/v1/" to key,
            base to "sb_secret_not_allowed"
        )) {
            var calls = 0
            val probe = SupabaseConnectionProbe(url, apiKey) { _, _, _, _ -> calls++; session }
            try {
                probe.check("test@example.com", "secret-test")
                fail("Debe rechazar la configuración.")
            } catch (_: IllegalArgumentException) { }
            assertEquals(0, calls)
        }
    }
}

package com.fincahernandez.gestionpecuaria.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudSessionManagerTest {
    private val uid = "388edc1d-cbe0-4066-9ae7-43ba820f985f"
    private val localId = "188edc1d-cbe0-4066-9ae7-43ba820f985f"
    private val user = AuthenticatedUser(localId, "Nombre local", "admin", "Administrador General", setOf("users"))
    private class Memory : CloudSessionStore {
        var value: String? = null
        override fun read() = value
        override fun write(value: String) { this.value = value }
        override fun clear() { value = null }
    }
    private fun session(expires: Long = 5000) = """{"access_token":"access-test","refresh_token":"refresh-test","expires_at":$expires,"user":{"id":"$uid","email":"test@example.com"}}"""
    private fun member(id: String? = localId, active: Boolean = true, role: String = "Administrador General") =
        """[{"usuarioId":"$uid","activo":$active,"usuarioLocalId":${id?.let { "\"$it\"" } ?: "null"},"nombreCompleto":"Nombre remoto","usuario":"remoto","rol":"$role","permisosPersonalizados":null}]"""
    private fun manager(store: Memory, call: (String,String,Map<String,String>,String?)->String) =
        CloudSessionManager(store, "https://test.supabase.co", "sb_publishable_test", SupabaseConnectionProbe.Transport(call), {1000000L})
    private suspend fun denied(block: suspend () -> Unit) {
        try { block(); fail("Debe rechazar la solicitud") } catch (_: IOException) { }
    }
    @Test fun signInStoresTokensWithoutPasswordAndRestoresMembership() = runBlocking {
        val s = Memory(); var reads = 0
        val m = manager(s) { url, _, headers, body ->
            if (url.contains("grant_type=password")) { assertEquals("password-test", JSONObject(body!!).getString("password")); session() }
            else { reads++; assertEquals("Bearer access-test", headers["Authorization"]); member() }
        }
        assertEquals(uid, m.signIn(user,"test@example.com","password-test").userId)
        assertFalse(s.value!!.contains("password-test"))
        assertEquals("Administrador General",m.restore(localId)!!.role)
        assertEquals(2,reads)
    }
    @Test fun firstAdminLinkPreservesRemoteProfile() = runBlocking {
        val s = Memory(); var linked = false
        val m = manager(s) { url, _, _, body -> when {
            url.contains("grant_type=password") -> session()
            url.contains("administrar_miembro") -> {
                val j=JSONObject(body!!)
                assertEquals(localId,j.getString("p_usuario_local_id"))
                assertEquals("Nombre remoto",j.getString("p_nombre_completo"))
                assertEquals("remoto",j.getString("p_usuario"))
                assertTrue(j.isNull("p_permisos")); linked=true; ""
            }
            else -> member(if(linked) localId else null)
        } }
        m.signIn(user,"test@example.com","p"); assertTrue(linked); assertNotNull(s.value)
    }
    @Test fun firstCloudLoginUsesAuthIdWithoutRequiringLocalLoginOrAdministrativeBinding() = runBlocking {
        val s=Memory()
        val m=manager(s){url,_,_,_->when {
            url.contains("grant_type=password")->session()
            url.contains("miembros_finca")->member(null,role="Operario")
            else->throw AssertionError("El ingreso único no requiere escritura administrativa")
        }}
        assertEquals(uid,m.signInCloud("test@example.com","123456").localUserId)
        assertEquals(uid,m.restore(uid)!!.localUserId)
    }
    @Test fun mismatchedOrInactiveOrUnlinkedWorkerNeverPersistsSession() = runBlocking {
        for (row in listOf(member(uid),member(active=false),member(null,role="Operario"),"[]")) {
            val s=Memory()
            val m=manager(s){url,_,_,_-> when { url.contains("grant_type=password")->session();url.contains("logout")->"";else->row } }
            denied { m.signIn(user,"test@example.com","p") }; assertNull(s.value)
        }
    }
    @Test fun refreshRotatesTokenAndReadsCurrentPermissions() = runBlocking {
        val s=Memory();var refreshed=false
        val m=manager(s){url,_,_,body->when {
            url.contains("grant_type=password")->session(1)
            url.contains("grant_type=refresh_token")->{assertEquals("refresh-test",JSONObject(body!!).getString("refresh_token"));refreshed=true;session().replace("refresh-test","rotated")}
            else->member(role=if(refreshed)"Operario" else "Administrador General")
        }}
        m.signIn(user,"test@example.com","p")
        assertEquals("Operario",m.restore(localId)!!.role);assertTrue(s.value!!.contains("rotated"))
    }
    @Test fun revokedRefreshErasesSessionButNetworkFailurePreservesRetry() = runBlocking {
        for (revoked in listOf(true,false)) {
            val s=Memory()
            val m=manager(s){url,_,_,_->when {
                url.contains("grant_type=password")->session(1)
                url.contains("grant_type=refresh_token")->throw if(revoked) CloudHttpException(400,"Sesión inválida") else IOException("Sin red")
                else->member()
            }}
            m.signIn(user,"test@example.com","p");denied{m.restore(localId)}
            assertEquals(revoked,s.value==null)
        }
    }
    @Test fun differentLocalUserCannotRestoreAndDisabledMemberClearsSession() = runBlocking {
        val s=Memory();var active=true
        val m=manager(s){url,_,_,_->if(url.contains("grant_type=password"))session() else member(active=active)}
        m.signIn(user,"test@example.com","p");assertNull(m.restore(uid))
        active=false;denied{m.restore(localId)};assertNull(s.value)
    }
    @Test fun offlineLogoutClearsBeforeAttemptingRevocation() = runBlocking {
        val s=Memory()
        val m=manager(s){url,_,_,_->when {
            url.contains("grant_type=password")->session()
            url.contains("logout?scope=local")->{assertNull(s.value);throw IOException("Sin red")}
            else->member()
        }}
        m.signIn(user,"test@example.com","p");assertFalse(m.signOut());assertNull(s.value)
    }
    @Test fun expiredServerTokenRetriesOnceWithRefresh() = runBlocking {
        val s=Memory();var reject=false;var refreshes=0
        val m=manager(s){url,_,_,_->when {
            url.contains("grant_type=password")->session()
            url.contains("grant_type=refresh_token")->{refreshes++;reject=false;session()}
            reject->throw CloudHttpException(401,"Expirado")
            else->member()
        }}
        m.signIn(user,"test@example.com","p");reject=true
        assertNotNull(m.restore(localId));assertEquals(1,refreshes)
    }
    @Test fun androidKeystoreEncryptsRoundTripsAndRejectsCorruption() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val vault=EncryptedCloudSessionStore(context,"cloud-session-test.bin")
        try {
            vault.write("secret-token-fixture")
            val file=File(context.noBackupFilesDir,"cloud-session-test.bin")
            assertFalse(file.readBytes().toString(Charsets.UTF_8).contains("secret-token-fixture"))
            assertEquals("secret-token-fixture",EncryptedCloudSessionStore(context,"cloud-session-test.bin").read())
            file.writeBytes(byteArrayOf(1,2,3))
            assertNull(vault.read());assertFalse(file.exists())
        } finally {vault.clear()}
    }
    @Test fun logoutDuringRefreshCannotRestoreDeletedCredentials() = runBlocking {
        val s=Memory();val started=CountDownLatch(1);val release=CountDownLatch(1)
        val m=manager(s){url,_,_,_->when {
            url.contains("grant_type=password")->session(1)
            url.contains("grant_type=refresh_token")->{started.countDown();check(release.await(5,TimeUnit.SECONDS));session()}
            url.contains("logout")->""
            else->member()
        }}
        m.signIn(user,"test@example.com","p")
        val task=async(Dispatchers.IO){m.restore(localId)}
        assertTrue(started.await(5,TimeUnit.SECONDS))
        m.clearLocalSession()
        release.countDown()
        try {task.await();fail("El refresco pendiente debe cancelarse")}catch(_:CancellationException){}
        assertNull(s.value)
    }
}

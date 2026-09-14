package com.fincahernandez.gestionpecuaria.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.security.ProtectedPassword
import java.net.UnknownHostException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFirstAuthTest {
    private val id="188edc1d-cbe0-4066-9ae7-43ba820f985f"
    private val email="test@example.com"
    private class Store:CloudSessionStore {
        var value:String?=null
        override fun read()=value
        override fun write(value:String){this.value=value}
        override fun clear(){value=null}
    }
    private inner class Setup {
        val store=Store();var connected=true;var calls=0;var active=true;var saved=0;var failure:Exception?=null
        var remote=CloudAccount(email,id,id,"Operario",setOf("animals"))
        val auth=OfflineFirstAuth(object:OfflineFirstAuth.Gateway {
            override suspend fun login(email:String,password:String):CloudAccount {calls++;failure?.let{throw it};return remote}
            override suspend fun restore(localId:String):CloudAccount {calls++;failure?.let{throw it};return remote}
            override fun clear():()->Boolean={true}
        },store,object:OfflineFirstAuth.Catalog {
            override suspend fun active(id:String)=active
            override suspend fun save(account:CloudAccount,password:ProtectedPassword):AuthenticatedUser {
                saved++;return AuthenticatedUser(account.localUserId,"Prueba",account.email,account.role,account.permissions)
            }
        },{connected},"https://test.supabase.co")
    }
    private suspend fun denied(block:suspend()->Unit){try{block();fail("Debió rechazar el ingreso")}catch(_:CloudAccessDenied){}}
    @Test fun firstLoginOfflineIsDeniedWithoutLegacyLocalBypass()=runBlocking {
        val s=Setup();s.connected=false
        denied{s.auth.login(email,"123456")};assertEquals(0,s.calls);assertNull(s.auth.remembered(id))
    }
    @Test fun onlineThenOfflineUsesVerifiedPasswordIncludingSupabaseSixCharacterPolicy()=runBlocking {
        val s=Setup();assertEquals(id,s.auth.login(email,"123456").id)
        assertFalse(s.store.value!!.contains("123456"))
        s.connected=false
        assertEquals(id,s.auth.login(" TEST@EXAMPLE.COM ","123456").id)
        assertEquals(1,s.calls);assertEquals(1,s.saved)
    }
    @Test fun wrongPasswordOrDifferentEmailIsRejectedOffline()=runBlocking {
        val s=Setup();s.auth.login(email,"123456");s.connected=false
        denied{s.auth.login(email,"incorrecta")};denied{s.auth.login("other@example.com","123456")}
    }
    @Test fun rejectedOnlineCredentialsNeverFallbackToOffline()=runBlocking {
        val s=Setup();s.auth.login(email,"123456");s.failure=CloudHttpException(400,"Rechazado")
        try{s.auth.login(email,"123456");fail()}catch(_:CloudHttpException){}
        assertNull(s.store.value)
        s.connected=false;denied{s.auth.login(email,"123456")}
    }
    @Test fun networkLossCanUsePreviouslyValidatedAccount()=runBlocking {
        val s=Setup();s.auth.login(email,"123456");s.failure=UnknownHostException()
        assertEquals(id,s.auth.login(email,"123456").id)
        assertEquals(id,s.auth.remembered(id)!!.id)
    }
    @Test fun rememberedSessionIsBoundToValidatedUserAndRefreshesPermissions()=runBlocking {
        val s=Setup();s.auth.login(email,"123456");assertNull(s.auth.remembered("legacy-local-id"))
        s.remote=s.remote.copy(role="Técnico Veterinario",permissions=setOf("health"))
        assertEquals(setOf("health"),s.auth.remembered(id)!!.permissionIds)
        s.connected=false;assertEquals(setOf("health"),s.auth.remembered(id)!!.permissionIds)
    }
    @Test fun revokedMembershipClearsOfflineProofAndInactiveLocalAccountIsRejected()=runBlocking {
        val s=Setup();s.auth.login(email,"123456");s.failure=CloudAccessDenied("Inactivo")
        assertNull(s.auth.remembered(id));assertNull(s.store.value)
        val other=Setup();other.auth.login(email,"123456");other.connected=false;other.active=false
        denied{other.auth.login(email,"123456")};assertNull(other.auth.remembered(id))
    }
    @Test fun corruptProofCannotGrantOfflineAccess()=runBlocking {
        val s=Setup();s.store.value="corrupto";s.connected=false
        denied{s.auth.login(email,"123456")};assertNull(s.store.value)
    }
}

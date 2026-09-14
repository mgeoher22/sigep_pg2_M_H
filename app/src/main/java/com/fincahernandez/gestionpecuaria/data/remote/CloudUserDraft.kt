package com.fincahernandez.gestionpecuaria.data.remote

import com.fincahernandez.gestionpecuaria.data.security.definedRoles
import com.fincahernandez.gestionpecuaria.data.security.rolePermissions
import java.util.Locale

data class CloudManagedUser(val id:String,val name:String,val email:String,val role:String,
    val active:Boolean,val permissions:Set<String>,val customized:Boolean)

data class CloudUserDraft(val name:String,val email:String,val password:String,val role:String,
    val active:Boolean,val permissions:Set<String>) {
    companion object {
        fun validEmail(value:String) = value.length<=254 && value.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
        fun validPassword(value:String) = value.length>=8 && value.toByteArray(Charsets.UTF_8).size<=72
    }
    fun validated():CloudUserDraft {
        val cleanName=name.trim();val cleanEmail=email.trim().lowercase(Locale.ROOT)
        require(cleanName.length in 1..120 && '@' !in cleanName) { "Escribe el nombre completo (hasta 120 caracteres)." }
        require(validEmail(cleanEmail)) { "Escribe un correo electrónico válido." }
        require(validPassword(password)) { "La contraseña necesita al menos 8 caracteres y hasta 72 bytes." }
        require(definedRoles.any {it.name==role}) { "El rol no es válido." }
        require("dashboard" in permissions && permissions.all {id->rolePermissions.any {it.id==id}}) { "Revisa los permisos; el panel principal es obligatorio." }
        return copy(name=cleanName,email=cleanEmail)
    }
}

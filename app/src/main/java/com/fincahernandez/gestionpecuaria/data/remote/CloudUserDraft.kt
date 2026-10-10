package com.fincahernandez.gestionpecuaria.data.remote

import com.fincahernandez.gestionpecuaria.data.security.definedRoles
import com.fincahernandez.gestionpecuaria.data.security.rolePermissions
import java.util.Locale
import java.util.UUID

data class CloudManagedUser(val id:String,val name:String,val email:String,val role:String,
    val active:Boolean,val permissions:Set<String>,val customized:Boolean,
    val currentAccount:Boolean = false,
    val temporaryPassword: String? = null)

data class CloudUserDraft(val name:String,val email:String,val role:String,
    val active:Boolean,val permissions:Set<String>) {
    companion object {
        fun validEmail(value:String) = value.length<=254 && value.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
        fun validPassword(value:String) = value.length>=8 && value.toByteArray(Charsets.UTF_8).size<=72
    }
    fun validated():CloudUserDraft {
        val cleanName=name.trim();val cleanEmail=email.trim().lowercase(Locale.ROOT)
        require(cleanName.length in 1..120 && '@' !in cleanName) { "Escribe el nombre completo (hasta 120 caracteres)." }
        require(validEmail(cleanEmail)) { "Escribe un correo electrónico válido." }
        require(definedRoles.any {it.name==role}) { "El rol no es válido." }
        require("dashboard" in permissions && permissions.all {id->rolePermissions.any {it.id==id}}) { "Revisa los permisos; el panel principal es obligatorio." }
        return copy(name=cleanName,email=cleanEmail)
    }
}

/** Cambios permitidos sobre una cuenta existente sin alterar sus credenciales. */
data class CloudUserUpdateDraft(
    val id: String,
    val name: String,
    val role: String,
    val active: Boolean,
    val permissions: Set<String>
) {
    fun validated(): CloudUserUpdateDraft {
        UUID.fromString(id)
        val cleanName = name.trim()
        require(cleanName.length in 1..120 && '@' !in cleanName) {
            "Escribe el nombre completo (hasta 120 caracteres)."
        }
        require(definedRoles.any { it.name == role }) { "El rol no es válido." }
        require(
            "dashboard" in permissions &&
                permissions.all { permission -> rolePermissions.any { it.id == permission } }
        ) { "Revisa los permisos; el panel principal es obligatorio." }
        return copy(name = cleanName)
    }
}

/** Cambio realizado por el propietario, quien debe conocer la clave vigente. */
data class CloudPasswordChange(
    val currentPassword: String,
    val newPassword: String
) {
    fun validated(): CloudPasswordChange {
        require(currentPassword.isNotEmpty()) { "Escribe tu contraseña actual." }
        require(CloudUserDraft.validPassword(newPassword)) {
            "La contraseña nueva necesita al menos 8 caracteres y hasta 72 bytes."
        }
        require(currentPassword != newPassword) {
            "La contraseña nueva debe ser diferente de la actual."
        }
        return this
    }
}

/** Restablecimiento administrativo sin exponer ni solicitar la clave anterior. */
data class CloudPasswordResetDraft(
    val userId: String,
    val temporaryPassword: String
) {
    fun validated(): CloudPasswordResetDraft {
        UUID.fromString(userId)
        require(CloudUserDraft.validPassword(temporaryPassword)) {
            "La contraseña temporal necesita al menos 8 caracteres y hasta 72 bytes."
        }
        return this
    }
}

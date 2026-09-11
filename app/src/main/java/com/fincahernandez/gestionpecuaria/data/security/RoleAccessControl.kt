package com.fincahernandez.gestionpecuaria.data.security

data class RolePermission(
    val id: String,
    val label: String
)

data class DefinedRole(
    val name: String,
    val description: String,
    val permissionIds: Set<String>
)

const val GENERAL_ADMIN_ROLE = "Administrador General"

/** Permisos que resumen las funciones descritas por los actores del ERS. */
val rolePermissions = listOf(
    RolePermission("dashboard", "Consultar el panel principal"),
    RolePermission("animals", "Consultar y registrar animales"),
    RolePermission("weighings", "Registrar pesajes y producción lechera"),
    RolePermission("lots", "Administrar lotes y parcelas"),
    RolePermission("health", "Gestionar sanidad y reproducción"),
    RolePermission("finance", "Registrar movimientos financieros"),
    RolePermission("employees", "Consultar empleados y nómina"),
    RolePermission("reports", "Consultar reportes autorizados"),
    RolePermission("users", "Crear usuarios y asignar roles")
)

/** Las cuentas reciben una plantilla fija de permisos según su rol. */
val definedRoles = listOf(
    DefinedRole(
        name = GENERAL_ADMIN_ROLE,
        description = "Toma decisiones estratégicas y financieras.",
        permissionIds = rolePermissions.map { it.id }.toSet()
    ),
    DefinedRole(
        name = "Administrador de Campo",
        description = "Supervisa animales, lotes, parcelas y operarios.",
        permissionIds = setOf("dashboard", "animals", "weighings", "lots", "reports")
    ),
    DefinedRole(
        name = "Técnico Veterinario",
        description = "Gestiona la salud y reproducción del hato.",
        permissionIds = setOf("dashboard", "animals", "health", "reports")
    ),
    DefinedRole(
        name = "Operario",
        description = "Captura diariamente los datos primarios de campo.",
        permissionIds = setOf("dashboard", "animals", "weighings", "lots")
    ),
    DefinedRole(
        name = "Auxiliar Contable",
        description = "Gestiona ingresos, egresos, empleados y nómina.",
        permissionIds = setOf("dashboard", "finance", "employees", "reports")
    )
)

fun permissionsForRole(roleName: String): Set<String> =
    definedRoles.firstOrNull { it.name == roleName }?.permissionIds.orEmpty()

/**
 * Obtiene los permisos efectivos de una cuenta.
 * Un valor nulo conserva la plantilla del rol; incluso una cadena vacía representa
 * una personalización explícita, por lo que ambos casos no deben confundirse.
 */
fun permissionsForUser(roleName: String, customizedPermissions: String?): Set<String> {
    if (customizedPermissions == null) return permissionsForRole(roleName)
    val validIds = rolePermissions.mapTo(mutableSetOf()) { it.id }
    return customizedPermissions
        .split(',')
        .map(String::trim)
        .filterTo(linkedSetOf()) { it.isNotEmpty() && it in validIds }
}

/** Guarda una selección estable y legible dentro de una sola columna de Room. */
fun serializePermissions(permissionIds: Set<String>): String =
    permissionIds.sorted().joinToString(",")

/**
 * Editar, desactivar o retirar registros es una facultad del rol, no un permiso
 * adicional que pueda otorgarse a otra plantilla de usuario.
 */
fun canEditExistingRecords(roleName: String?): Boolean = roleName == GENERAL_ADMIN_ROLE

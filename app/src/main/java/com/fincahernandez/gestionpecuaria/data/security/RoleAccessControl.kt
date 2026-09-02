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
        name = "Administrador General",
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

package com.fincahernandez.gestionpecuaria.ui.navigation

/** Rutas principales y rutas internas disponibles en la aplicación. */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val ANIMAL_LIST = "animal_list"
    const val ANIMAL_FORM = "animal_form"
    const val ANIMAL_CONFIRMATION = "animal_confirmation"
    const val ANIMAL_DETAIL = "animal_detail"
    const val LOTS = "lots"
    const val LOT_FORM = "lot_form"
    const val LOT_ANIMAL_SELECTION = "lot_animal_selection"
    const val LOT_DETAIL = "lot_detail"
    const val PARCELS = "parcels"
    const val PARCEL_FORM = "parcel_form"
    const val PARCEL_DETAIL = "parcel_detail"
    const val WEIGHINGS = "weighings"
    const val WEIGHING_FORM = "weighing_form"
    const val MILK_PRODUCTION = "milk_production"
    const val MILK_PRODUCTION_FORM = "milk_production_form"
    const val FINANCE = "finance"
    const val FINANCE_FORM = "finance_form"
    const val EMPLOYEES = "employees"
    const val EMPLOYEE_FORM = "employee_form"
    const val REPORTS = "reports"
    const val USERS = "users"
    const val USER_FORM = "user_form"
    const val ROLE_PERMISSIONS = "role_permissions"

    /** Destinos accesibles desde el menú principal. */
    val mainDestinations = setOf(
        DASHBOARD,
        ANIMAL_LIST,
        LOTS,
        PARCELS,
        WEIGHINGS,
        MILK_PRODUCTION,
        FINANCE,
        EMPLOYEES,
        REPORTS,
        USERS
    )
}

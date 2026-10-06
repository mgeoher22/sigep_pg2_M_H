package com.fincahernandez.gestionpecuaria.ui.navigation

/** Rutas principales y rutas internas disponibles en la aplicación. */
object Routes {
    const val SPLASH = "splash"
    const val SETUP_ADMIN = "setup_admin"
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
    const val PARCEL_MAP = "parcel_map"
    const val WEIGHINGS = "weighings"
    const val WEIGHING_FORM = "weighing_form"
    const val WEIGHING_ANIMAL_DETAIL = "weighing_animal_detail"
    const val MILK_PRODUCTION = "milk_production"
    const val MILK_PRODUCTION_FORM = "milk_production_form"
    const val MILK_CONFIGURATION = "milk_configuration"
    const val SANITARY = "sanitary"
    const val SANITARY_FORM = "sanitary_form"
    const val FINANCE = "finance"
    const val FINANCE_FORM = "finance_form"
    const val SUPPLIES = "supplies"
    const val SUPPLY_FORM = "supply_form"
    const val SUPPLY_ENTRY = "supply_entry"
    const val SUPPLY_EXIT = "supply_exit"
    const val SUPPLY_ASSIGNMENTS = "supply_assignments"
    const val SUPPLY_DETAIL = "supply_detail"
    const val SUPPLY_ADJUSTMENT = "supply_adjustment"
    const val EMPLOYEES = "employees"
    const val EMPLOYEE_FORM = "employee_form"
    const val EMPLOYEE_DETAIL = "employee_detail"
    const val EMPLOYEE_PAYMENT_FORM = "employee_payment_form"
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
        SANITARY,
        FINANCE,
        SUPPLIES,
        EMPLOYEES,
        REPORTS,
        USERS
    )
}

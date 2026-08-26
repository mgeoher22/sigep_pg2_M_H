package com.fincahernandez.gestionpecuaria.ui.navigation

/** Rutas principales y rutas internas disponibles en la aplicación. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val ANIMAL_LIST = "animal_list"
    const val ANIMAL_FORM = "animal_form"
    const val ANIMAL_CONFIRMATION = "animal_confirmation"
    const val ANIMAL_DETAIL = "animal_detail"
    const val LOTS = "lots"
    const val PARCELS = "parcels"
    const val WEIGHINGS = "weighings"
    const val MILK_PRODUCTION = "milk_production"
    const val FINANCE = "finance"
    const val EMPLOYEES = "employees"

    /** Destinos accesibles desde el menú principal. */
    val mainDestinations = setOf(
        DASHBOARD,
        ANIMAL_LIST,
        LOTS,
        PARCELS,
        WEIGHINGS,
        MILK_PRODUCTION,
        FINANCE,
        EMPLOYEES
    )
}

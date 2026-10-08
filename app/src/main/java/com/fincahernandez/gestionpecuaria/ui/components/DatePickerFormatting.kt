package com.fincahernandez.gestionpecuaria.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Convierte el valor de Material DatePicker sin cambiar el día seleccionado.
 *
 * DatePicker representa cada día como la medianoche en UTC. Si ese valor se
 * formatea con la zona horaria de Guatemala, todavía corresponde a la tarde
 * del día anterior. Por eso este formateo también debe realizarse en UTC.
 */
fun formatDatePickerMillis(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

/** Fecha local actual para inicializar los formularios de eventos cotidianos. */
fun todayDateText(nowMillis: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(nowMillis))

package com.fincahernandez.gestionpecuaria.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fincahernandez.gestionpecuaria.MainActivity
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Preferencia local e individual de cada cuenta para recibir avisos de pago. */
class MilkNotificationPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun isEnabled(userId: String): Boolean =
        userId.isNotBlank() && preferences.getBoolean(userKey(userId), false)

    fun setEnabled(userId: String, enabled: Boolean) {
        if (userId.isNotBlank()) {
            preferences.edit().putBoolean(userKey(userId), enabled).apply()
        }
    }

    /** Mantiene el aviso si al menos una cuenta activa de este dispositivo lo solicitó. */
    fun anyUserEnabled(): Boolean = preferences.all.values.any { it == true }

    private fun userKey(userId: String) = "user_$userId"

    private companion object {
        const val PREFERENCES_NAME = "milk_payment_notifications"
    }
}

/** Programa un único aviso para el pago pendiente más próximo. */
object MilkPaymentNotificationScheduler {
    const val ACTION_PAYMENT_DUE =
        "com.fincahernandez.gestionpecuaria.action.MILK_PAYMENT_DUE"
    const val EXTRA_PAYMENT_DATE = "payment_date"
    private const val REQUEST_CODE = 4601
    const val NOTIFICATION_ID = 4603

    fun schedule(context: Context, paymentDueDate: Long) {
        // Si el usuario ya abrió la app, reemplaza cualquier aviso anterior desactualizado.
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = Calendar.getInstance().apply {
            timeInMillis = paymentDueDate
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis.coerceAtLeast(System.currentTimeMillis() + 2_000L)

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            alarmIntent(context, paymentDueDate)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmIntent(context, 0L))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun alarmIntent(context: Context, paymentDueDate: Long): PendingIntent {
        val intent = Intent(context, MilkPaymentNotificationReceiver::class.java).apply {
            action = ACTION_PAYMENT_DUE
            putExtra(EXTRA_PAYMENT_DATE, paymentDueDate)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/** Comprueba el monto aún pendiente antes de mostrar la notificación. */
class MilkPaymentNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = GestionPecuariaDatabase.obtenerInstancia(context).produccionLecheraDao()
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    val nearest = dao.obtenerPendientes()
                        .groupBy { it.fechaPagoProgramada }
                        .keys
                        .minOrNull()
                    if (nearest != null && MilkNotificationPreferences(context).anyUserEnabled()) {
                        MilkPaymentNotificationScheduler.schedule(context, nearest)
                    }
                } else {
                    val paymentDate = intent.getLongExtra(
                        MilkPaymentNotificationScheduler.EXTRA_PAYMENT_DATE,
                        0L
                    )
                    val amount = dao.montoPendiente(paymentDate) ?: 0.0
                    if (amount > 0.0 && MilkNotificationPreferences(context).anyUserEnabled()) {
                        showNotification(context, amount)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, amount: Double) {
        createChannel(context)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val openAppIntent = PendingIntent.getActivity(
            context,
            4602,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val formattedAmount = String.format(Locale.US, "Q %.2f", amount)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pago de leche programado")
            .setContentText("Monto pendiente: $formattedAmount")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "El pago de leche por $formattedAmount está pendiente. " +
                        "Un rol autorizado debe confirmarlo antes de aplicarlo a ingresos."
                )
            )
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(
                MilkPaymentNotificationScheduler.NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Pagos de leche",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos de pagos pendientes de la producción lechera"
                }
            )
        }
    }

    private companion object {
        const val CHANNEL_ID = "milk_payment_due"
    }
}

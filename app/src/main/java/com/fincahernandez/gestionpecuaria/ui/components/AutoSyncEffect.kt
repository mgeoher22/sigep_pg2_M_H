package com.fincahernandez.gestionpecuaria.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.room.InvalidationTracker
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.remote.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/** Observes validated internet only; a Wi-Fi connection alone is insufficient. */
private fun internet(context: Context) = callbackFlow {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    var defaultNetwork: Network? = null
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { defaultNetwork = network }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (network == defaultNetwork) trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        }
        override fun onLost(network: Network) {
            if (network == defaultNetwork) { defaultNetwork = null; trySend(false) }
        }
    }
    // Initial snapshot is read before registration; subsequent updates use callback values.
    trySend(manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
    manager.registerDefaultNetworkCallback(callback)
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

private fun productChanges(database: GestionPecuariaDatabase) = callbackFlow {
    val observer = object : InvalidationTracker.Observer(CloudTables.all.map { it.name }.toTypedArray()) {
        override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
    }
    database.invalidationTracker.addObserver(observer)
    awaitClose { database.invalidationTracker.removeObserver(observer) }
}.flowOn(Dispatchers.IO)

@Composable
fun rememberAutoSyncStatus(userId: String?, manager: CloudSessionManager, paused: Boolean): AutoSyncStatus {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var status by remember(userId) { mutableStateOf(AutoSyncStatus(AutoSyncPhase.WAITING)) }
    LaunchedEffect(userId, manager, paused, lifecycle) {
        if (userId.isNullOrBlank() || paused) return@LaunchedEffect
        val database = GestionPecuariaDatabase.obtenerInstancia(context)
        val coordinator = SyncCoordinator(RoomSyncStore(database, manager.syncProject()), AndroidSyncGateway(manager, userId))
        try {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                internet(context).collectLatest { online ->
                    if (!online) status = AutoSyncStatus(AutoSyncPhase.OFFLINE)
                    else AutoSyncLoop(sync = { coordinator.run() }, report = { status = it }).run(productChanges(database))
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            status = AutoSyncStatus(AutoSyncPhase.REVIEW)
        }
    }
    return status
}

fun AutoSyncStatus.label(): String = when (phase) {
    AutoSyncPhase.WAITING -> "Sincronización automática al conectar"
    AutoSyncPhase.OFFLINE -> "Sin internet · Datos guardados en este dispositivo"
    AutoSyncPhase.RUNNING -> "Sincronizando con la nube…"
    AutoSyncPhase.COMPLETE -> "Última sincronización: " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(completedAt ?: 0))
    AutoSyncPhase.PENDING -> "Hay cambios pendientes de enviar"
    AutoSyncPhase.REVIEW -> "Hay datos por revisar · Abre Sincronizar"
    AutoSyncPhase.RETRY -> "No se pudo confirmar · Se reintentará con internet"
}

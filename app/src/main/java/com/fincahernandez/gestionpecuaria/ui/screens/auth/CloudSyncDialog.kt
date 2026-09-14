package com.fincahernandez.gestionpecuaria.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.remote.*
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CloudSyncDialog(user: AuthenticatedUser, manager: CloudSessionManager, onClose: () -> Unit) {
    val context = LocalContext.current
    val coordinator = remember(manager,user.id) {
        SyncCoordinator(RoomSyncStore(GestionPecuariaDatabase.obtenerInstancia(context),manager.syncProject()), AndroidSyncGateway(manager,user.id))
    }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var conflicts by remember { mutableStateOf<List<SyncConflict>>(emptyList()) }
    val choices = remember { mutableStateListOf<SyncChoice>() }
    AlertDialog(
        onDismissRequest = { if(!busy)onClose() },
        title = { Text("Sincronizar") },
        text = { Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Envía tus registros y recibe los cambios de la nube. Necesitas internet. Las fotos siguen guardadas solo en su dispositivo.")
            if(busy) { LinearProgressIndicator();Text("Sincronizando…") }
            message?.let { Text(it) }
            conflicts.forEach { conflict ->
                HorizontalDivider()
                val label = conflict.local?.get("nombre") ?: conflict.local?.get("codigoIdentificacion")
                    ?: conflict.remote?.data?.get("nombre") ?: conflict.key.id.take(8)
                Text("${conflict.key.table}: $label",style=MaterialTheme.typography.titleSmall)
                Text(conflict.reason)
                val fields = (conflict.local.orEmpty().keys + conflict.remote?.data.orEmpty().keys)
                    .filter { it !in setOf("id","actualizadoEn") && conflict.local?.get(it) != conflict.remote?.data?.get(it) }
                fields.forEach { field ->
                    Text("$field\nEste dispositivo: ${conflict.local?.get(field)?.toString()?.take(100) ?: "—"}\nNube: ${conflict.remote?.data?.get(field)?.toString()?.take(100) ?: "—"}",style=MaterialTheme.typography.bodySmall)
                }
                if(conflict.remote?.deleted == true) Text("La copia de la nube está anulada. Elegirla retira este registro del dispositivo.")
                val selected = choices.firstOrNull { it.conflict==conflict }
                if(conflict.local!=null && conflict.remote!=null && !conflict.remote.deleted) {
                    TextButton(enabled=!busy,onClick={choices.removeAll {it.conflict.key==conflict.key};choices.add(SyncChoice(conflict,true))}) {
                        Text(if(selected?.useLocal==true) "✓ Conservar este dispositivo" else "Conservar este dispositivo")
                    }
                }
                if(conflict.remote!=null) TextButton(enabled=!busy,onClick={choices.removeAll {it.conflict.key==conflict.key};choices.add(SyncChoice(conflict,false))}) {
                    Text(if(selected?.useLocal==false) "✓ Usar la copia de la nube" else "Usar la copia de la nube")
                }
            }
        } },
        confirmButton = { TextButton(enabled=!busy && conflicts.all { c -> choices.any {it.conflict==c} },onClick={
            busy=true;message=null
            scope.launch {
                try {
                    val result=coordinator.run(choices.toList())
                    conflicts=result.conflicts
                    choices.removeAll { choice -> conflicts.none {it==choice.conflict} }
                    message=if(conflicts.isNotEmpty()) "Hay ${conflicts.size} diferencias para revisar. Elige qué copia conservar y pulsa Aplicar elecciones. Enviados hasta ahora: ${result.sent}."
                    else "Sincronización completada. Enviados: ${result.sent}. Actualizados en este dispositivo: ${result.received}. Pendientes: ${result.pending}."
                } catch(e:CancellationException) {throw e}
                catch(e:Exception) {
                    message=when(e) {
                        is SyncRejected,is CloudAccessDenied,is IllegalArgumentException -> e.message
                        else -> "No se pudo confirmar toda la sincronización. Tus datos siguen guardados. Revisa internet y pulsa Sincronizar otra vez; el envío pendiente se reintentará sin duplicarlo."
                    }
                } finally {busy=false}
            }
        }) {Text(if(conflicts.isEmpty()) "Sincronizar" else "Aplicar elecciones")} },
        dismissButton = {TextButton(enabled=!busy,onClick=onClose) {Text("Cerrar")}}
    )
}

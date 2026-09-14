package com.fincahernandez.gestionpecuaria.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.remote.*
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CloudToolsDialog(action: String, user: AuthenticatedUser, manager: CloudSessionManager,
    onClose: () -> Unit, onProfileSaved: suspend () -> Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(user.fullName.takeUnless { '@' in it || it=="Usuario" }.orEmpty()) }
    val profile=action=="profile"
    AlertDialog(onDismissRequest={if(!busy)onClose()},
        title={Text(if(profile) "Mi perfil" else "Descargar de la nube")},
        text={Column {
            if(profile) {
                Text("Este nombre aparecerá en el inicio. Necesitas internet para guardarlo.")
                OutlinedTextField(value=name,onValueChange={name=it},enabled=!busy && !done,
                    label={Text("Nombre completo")},singleLine=true)
            } else Text("Añade a este dispositivo los registros que faltan. Conserva los datos locales y avisa si hay diferencias. Requiere internet. Las fotos y el envío de cambios quedan pendientes.")
            if(busy) { LinearProgressIndicator();Text("Espera un momento…") }
            message?.let { Text(it) }
        }},
        confirmButton={if(!done) TextButton(enabled=!busy && (!profile || name.isNotBlank()),onClick={
            busy=true;message=null
            scope.launch {
                try {
                    if(profile) {
                        manager.updateDisplayName(user.id,name)
                        onProfileSaved()
                        message="Nombre guardado."
                    } else {
                        val snapshot=manager.download(user.id)
                        val result=CloudDataImporter(GestionPecuariaDatabase.obtenerInstancia(context)).apply(snapshot) { manager.validateSnapshot(snapshot) }
                        message="Descarga completada: ${result.added} registros añadidos; ${result.existing} ya estaban en el dispositivo. Diferencias conservadas: ${result.different}."
                    }
                    done=true
                } catch(e:CancellationException) { throw e }
                catch(e:Exception) {
                    message=when(e) {
                        is CloudAccessDenied,is IllegalArgumentException -> e.message
                        else -> "No se pudo completar. Revisa internet y vuelve a intentarlo. Si persiste, revisaremos los registros relacionados."
                    }
                } finally {busy=false}
            }
        }) {Text(if(profile) "Guardar nombre" else "Descargar")}},
        dismissButton={TextButton(enabled=!busy,onClick=onClose){Text("Cerrar")}})
}

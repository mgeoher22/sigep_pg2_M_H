package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.repository.SanitaryRepository
import com.fincahernandez.gestionpecuaria.data.repository.SanitaryStoredRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Aísla los registros veterinarios para que otras pantallas no controlen su flujo Room. */
class SanitaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SanitaryRepository(
        GestionPecuariaDatabase.obtenerInstancia(application)
    )

    val records = repository.observeRecords().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun register(record: SanitaryStoredRecord) = repository.register(record)
}

package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.repository.MilkProductionDraft
import com.fincahernandez.gestionpecuaria.data.repository.MilkProductionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Mantiene la producción de Room disponible durante toda la navegación. */
class MilkProductionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MilkProductionRepository(
        GestionPecuariaDatabase.obtenerInstancia(application)
    )

    val records = repository.observeRecords().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun saveDailyProduction(draft: MilkProductionDraft): String =
        repository.saveDailyProduction(draft)
}

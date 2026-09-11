package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.repository.ParcelDraft
import com.fincahernandez.gestionpecuaria.data.repository.ParcelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Mantiene el inventario persistente de parcelas durante la navegación. */
class ParcelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ParcelRepository(
        GestionPecuariaDatabase.obtenerInstancia(application)
    )

    val parcels = repository.observeParcels().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun createParcel(draft: ParcelDraft): String = repository.createParcel(draft)

    suspend fun updateParcel(id: String, draft: ParcelDraft) = repository.updateParcel(id, draft)

    suspend fun setResting(id: String, resting: Boolean) = repository.setResting(id, resting)

    suspend fun deactivateParcel(id: String) = repository.deactivateParcel(id)
}

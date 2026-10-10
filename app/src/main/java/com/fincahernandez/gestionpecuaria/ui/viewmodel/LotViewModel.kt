package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.repository.LotDraft
import com.fincahernandez.gestionpecuaria.data.repository.LotRepository
import com.fincahernandez.gestionpecuaria.data.repository.LotSaleDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Mantiene los lotes persistentes fuera del estado temporal de navegación. */
class LotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LotRepository(
        GestionPecuariaDatabase.obtenerInstancia(application)
    )

    val lots = repository.observeLots().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun createLot(draft: LotDraft): String = repository.createLot(draft)

    suspend fun updateLot(lotId: String, draft: LotDraft) = repository.updateLot(lotId, draft)

    suspend fun deactivateLot(lotId: String) = repository.deactivateLot(lotId)

    suspend fun sellAndCloseLot(lotId: String, draft: LotSaleDraft) =
        repository.sellAndCloseLot(lotId, draft)

    suspend fun reconcileSoldLotAnimals(): Int = repository.reconcileSoldLotAnimals()
}

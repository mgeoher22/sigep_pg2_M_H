package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.repository.FinanceRepository
import com.fincahernandez.gestionpecuaria.data.repository.FinancialMovementDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(GestionPecuariaDatabase.obtenerInstancia(application))

    val movements = repository.observeMovements().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    suspend fun saveMovement(draft: FinancialMovementDraft) = repository.saveMovement(draft)
}

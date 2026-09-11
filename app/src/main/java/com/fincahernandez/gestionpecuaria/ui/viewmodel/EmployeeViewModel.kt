package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.repository.EmployeeDraft
import com.fincahernandez.gestionpecuaria.data.repository.EmployeePaymentDraft
import com.fincahernandez.gestionpecuaria.data.repository.EmployeeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class EmployeeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmployeeRepository(GestionPecuariaDatabase.obtenerInstancia(application))

    val employees = repository.observeEmployees().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val payments = repository.observePayments().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    suspend fun saveEmployee(draft: EmployeeDraft) = repository.saveEmployee(draft)

    suspend fun savePayment(draft: EmployeePaymentDraft) = repository.savePayment(draft)
}

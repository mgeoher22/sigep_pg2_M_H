package com.fincahernandez.gestionpecuaria.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.repository.AnimalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Mantiene los datos persistentes de animales fuera de la navegación visual. */
class AnimalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnimalRepository(
        GestionPecuariaDatabase.obtenerInstancia(application)
    )

    /** Conserva la última lista para no consultar Room de nuevo en cada cambio de pantalla. */
    val animals = repository.observeAnimals().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun saveAnimal(animal: AnimalEntity, weightPounds: Double?) =
        repository.saveAnimal(animal, weightPounds)

    suspend fun removeAnimal(id: String) = repository.removeAnimal(id)

    suspend fun registerWeight(
        animalId: String,
        weightPounds: Double,
        weighingDate: Long,
        notes: String?
    ) = repository.registerWeight(animalId, weightPounds, weighingDate, notes)
}

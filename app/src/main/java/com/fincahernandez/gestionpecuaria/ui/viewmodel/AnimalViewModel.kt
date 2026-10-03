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

    /** Mantiene la lista de pesajes sincronizada con Room, incluidos los importados. */
    val weighings = repository.observeWeighings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun saveAnimal(animal: AnimalEntity, weightPounds: Double?) =
        repository.saveAnimal(animal, weightPounds)

    suspend fun removeAnimal(id: String) = repository.removeAnimal(id)

    /** Permite actualizar la foto sin exponer la edición completa del animal. */
    suspend fun updatePhoto(animalId: String, photoUri: String) =
        repository.updatePhoto(animalId, photoUri)

    /** Permite actualizar únicamente la condición de próxima a parto. */
    suspend fun updateNearCalving(animalId: String, nearCalving: Boolean) =
        repository.updateNearCalving(animalId, nearCalving)

    suspend fun registerWeight(
        animalId: String,
        weightPounds: Double,
        weighingDate: Long,
        notes: String?,
        lotId: String? = null,
        recordType: String = "INDIVIDUAL"
    ) = repository.registerWeight(
        animalId = animalId,
        weightPounds = weightPounds,
        weighingDate = weighingDate,
        notes = notes,
        lotId = lotId,
        recordType = recordType
    )
}

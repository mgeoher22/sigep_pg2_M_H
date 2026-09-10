package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Animal almacenado junto con su pesaje más reciente, si existe. */
data class AnimalStoredRecord(
    val animal: AnimalEntity,
    val lastWeightPounds: Double?
)

/**
 * Centraliza las operaciones persistentes del módulo de animales.
 * La interfaz solo consume este repositorio y no necesita conocer consultas SQL.
 */
class AnimalRepository(
    private val database: GestionPecuariaDatabase
) {
    private val animalDao = database.animalDao()
    private val weighingDao = database.pesajeDao()

    /**
     * Room emite una lista nueva después de cada inserción, edición, eliminación
     * o pesaje, por lo que Compose actualiza la pantalla de forma inmediata.
     */
    fun observeAnimals(): Flow<List<AnimalStoredRecord>> = combine(
        animalDao.observarTodos(),
        weighingDao.observarTodos()
    ) { animals, weighings ->
        animals.map { animal ->
            val lastWeight = weighings
                .asSequence()
                .filter { it.animalId == animal.id }
                .maxByOrNull { it.fechaPesaje }
                ?.pesoLibras
            AnimalStoredRecord(animal, lastWeight)
        }
    }

    /** Expone el historial persistente para listados, reportes e importaciones masivas. */
    fun observeWeighings(): Flow<List<PesajeEntity>> = weighingDao.observarTodos()

    /** Inserta o actualiza el animal y registra un pesaje solo cuando cambió. */
    suspend fun saveAnimal(animal: AnimalEntity, weightPounds: Double?) {
        database.withTransaction {
            val current = animalDao.buscarPorId(animal.id)
            if (current == null) {
                animalDao.insertar(animal)
            } else {
                animalDao.actualizar(
                    animal.copy(
                        creadoEn = current.creadoEn,
                        actualizadoEn = System.currentTimeMillis()
                    )
                )
            }

            val previousWeight = weighingDao.obtenerUltimoDelAnimal(animal.id)?.pesoLibras
            if (weightPounds != null && weightPounds != previousWeight) {
                weighingDao.insertar(
                    PesajeEntity(
                        animalId = animal.id,
                        pesoLibras = weightPounds,
                        fechaPesaje = System.currentTimeMillis(),
                        observaciones = "Peso registrado desde la ficha del animal"
                    )
                )
            }
        }
    }

    /** Agrega un pesaje y permite que el listado actualice el último peso. */
    suspend fun registerWeight(
        animalId: String,
        weightPounds: Double,
        weighingDate: Long,
        notes: String?
    ) {
        weighingDao.insertar(
            PesajeEntity(
                animalId = animalId,
                pesoLibras = weightPounds,
                fechaPesaje = weighingDate,
                observaciones = notes
            )
        )
    }

    /**
     * Retira el animal del inventario activo sin destruir su historial de pesajes.
     * Esta baja lógica evita violar las relaciones de trazabilidad de Room.
     */
    suspend fun removeAnimal(id: String) {
        check(animalDao.desactivar(id) == 1) {
            "No se encontró el animal que se desea eliminar"
        }
    }
}

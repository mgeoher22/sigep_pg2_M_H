package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import java.util.Calendar
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
        validateAnimalChronology(animal)
        require(weightPounds == null || (weightPounds.isFinite() && weightPounds > 0.0)) {
            "El peso debe ser mayor que cero."
        }
        database.withTransaction {
            val current = animalDao.buscarPorId(animal.id)
            val newMotherId = animal.madreId.takeIf { it != current?.madreId }
            val mother = newMotherId?.let { animalDao.buscarPorId(it) }
            if (newMotherId != null) {
                val childBirthDate = requireNotNull(animal.fechaNacimiento) {
                    "La cría requiere fecha de nacimiento."
                }
                val (birthDayStart, nextBirthDayStart) = localDayBounds(childBirthDate)
                val alreadyRegisteredInSameBirth = animalDao.contarOtrasCriasDelParto(
                    madreId = newMotherId,
                    inicioDia = birthDayStart,
                    finDiaExclusivo = nextBirthDayStart,
                    animalExcluidoId = animal.id
                ) > 0
                require(
                    mother != null && mother.sexo == "HEMBRA" &&
                        mother.estado == "ACTIVO" &&
                        (mother.proximaParto || alreadyRegisteredInSameBirth)
                ) {
                    "La madre debe estar próxima a dar a luz o tener otra cría registrada en esa misma fecha."
                }
                requireOnOrAfter(
                    date = childBirthDate,
                    minimumDate = animalAvailableFrom(requireNotNull(mother)),
                    message = "La cría no puede nacer antes del nacimiento o llegada de su madre."
                )
            }
            if (current == null) {
                animalDao.insertar(animal)

                // Al registrar el nacimiento, la vaca deja automáticamente la lista
                // de próximas a dar a luz. Ambas operaciones forman una sola transacción.
                if (mother != null) {
                    check(animalDao.actualizarProximaParto(mother.id, false) == 1) {
                        "No fue posible completar el control de parto de la madre."
                    }
                }
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
        notes: String?,
        lotId: String? = null,
        recordType: String = "INDIVIDUAL"
    ) = database.withTransaction {
        require(weightPounds.isFinite() && weightPounds > 0.0) {
            "El peso debe ser mayor que cero."
        }
        requireNotFuture(weighingDate, "La fecha del pesaje")
        val animal = checkNotNull(animalDao.buscarPorId(animalId)) {
            "No se encontró el animal seleccionado."
        }
        check(animal.estado == "ACTIVO") { "El animal seleccionado ya no está activo." }
        requireOnOrAfter(
            date = weighingDate,
            minimumDate = animalAvailableFrom(animal),
            message = "El pesaje no puede ser anterior al nacimiento o llegada del animal."
        )
        weighingDao.insertar(
            PesajeEntity(
                animalId = animalId,
                loteId = lotId,
                pesoLibras = weightPounds,
                fechaPesaje = weighingDate,
                tipoRegistro = recordType,
                observaciones = notes
            )
        )
    }

    /**
     * Sustituye únicamente la referencia local de la fotografía. Esta operación
     * independiente evita conceder acceso al formulario administrativo completo.
     */
    suspend fun updatePhoto(animalId: String, photoUri: String) {
        require(photoUri.isNotBlank()) { "Seleccione una fotografía válida." }
        check(animalDao.actualizarFoto(animalId, photoUri.trim()) == 1) {
            "No se encontró un animal activo para actualizar la fotografía."
        }
    }

    /**
     * Marca o desmarca una hembra activa como próxima a dar a luz. El filtro del
     * DAO impide aplicar accidentalmente esta condición a un macho o animal retirado.
     */
    suspend fun updateNearCalving(animalId: String, nearCalving: Boolean) {
        check(animalDao.actualizarProximaParto(animalId, nearCalving) == 1) {
            "Solo una hembra activa puede cambiar su control de parto."
        }
    }

    /**
     * Retira el animal del inventario activo sin destruir su historial de pesajes.
     * Esta baja lógica evita violar las relaciones de trazabilidad de Room.
     */
    suspend fun removeAnimal(id: String) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            check(animalDao.desactivar(id, now) == 1) {
                "No se encontró el animal que se desea eliminar"
            }
            // Un animal inactivo no debe seguir ocupando un cupo dentro de un lote activo.
            database.loteDao().finalizarAsignacionActiva(
                animalId = id,
                fechaSalida = now,
                motivoSalida = "Animal retirado del inventario activo",
                fechaActualizacion = now
            )
        }
    }
}

/** Delimita un día civil local para comparar partos sin depender de la hora guardada. */
private fun localDayBounds(value: Long): Pair<Long, Long> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = value
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    return start to calendar.timeInMillis
}

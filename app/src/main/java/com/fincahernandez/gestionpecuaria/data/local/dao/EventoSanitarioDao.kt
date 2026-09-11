package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fincahernandez.gestionpecuaria.data.local.entity.EventoSanitarioEntity
import kotlinx.coroutines.flow.Flow

/** Operaciones persistentes de la ficha sanitaria individual. */
@Dao
interface EventoSanitarioDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(evento: EventoSanitarioEntity)

    /** Reimportar el mismo evento no crea duplicados porque conserva un id determinista. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarImportado(evento: EventoSanitarioEntity)

    /** El historial más reciente se muestra primero. */
    @Query("SELECT * FROM eventos_sanitarios ORDER BY fechaEvento DESC, creadoEn DESC")
    fun observarTodos(): Flow<List<EventoSanitarioEntity>>

    @Query(
        "SELECT * FROM eventos_sanitarios " +
            "WHERE animalId = :animalId ORDER BY fechaEvento DESC, creadoEn DESC"
    )
    fun observarDelAnimal(animalId: String): Flow<List<EventoSanitarioEntity>>

    /** Elimina registros de prueba al reemplazar un animal durante la carga inicial. */
    @Query("DELETE FROM eventos_sanitarios WHERE animalId = :animalId")
    suspend fun eliminarPorAnimalParaCargaInicial(animalId: String): Int

    /** Mantiene sincronizado el estado visible en la ficha general del animal. */
    @Query(
        "UPDATE animales SET estadoSalud = :estadoSalud, actualizadoEn = :actualizadoEn " +
            "WHERE id = :animalId AND estado = 'ACTIVO'"
    )
    suspend fun actualizarEstadoDelAnimal(
        animalId: String,
        estadoSalud: String,
        actualizadoEn: Long = System.currentTimeMillis()
    ): Int
}

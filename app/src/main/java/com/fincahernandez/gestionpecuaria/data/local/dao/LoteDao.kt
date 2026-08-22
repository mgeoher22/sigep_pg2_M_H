package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteAnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoteDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(lote: LoteEntity)

    @Update
    suspend fun actualizar(lote: LoteEntity)

    @Query("SELECT * FROM lotes ORDER BY nombre")
    fun observarTodos(): Flow<List<LoteEntity>>

    @Query("SELECT * FROM lotes WHERE estado = 'ACTIVO' ORDER BY nombre")
    fun observarActivos(): Flow<List<LoteEntity>>

    @Query("SELECT * FROM lotes WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): LoteEntity?

    @Query("SELECT * FROM lotes WHERE codigo = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): LoteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarAsignacion(asignacion: LoteAnimalEntity)

    @Transaction
    suspend fun asignarAnimal(asignacion: LoteAnimalEntity) {
        check(buscarAsignacionActiva(asignacion.animalId) == null) {
            "El animal ya pertenece a un lote activo"
        }
        insertarAsignacion(asignacion)
    }

    @Query(
        """
        SELECT * FROM lote_animales
        WHERE animalId = :animalId AND fechaSalida IS NULL
        LIMIT 1
        """
    )
    suspend fun buscarAsignacionActiva(animalId: String): LoteAnimalEntity?

    @Query(
        """
        SELECT animales.* FROM animales
        INNER JOIN lote_animales
            ON animales.id = lote_animales.animalId
        WHERE lote_animales.loteId = :loteId
            AND lote_animales.fechaSalida IS NULL
            AND animales.estado = 'ACTIVO'
        ORDER BY animales.codigoIdentificacion
        """
    )
    fun observarAnimalesActivos(loteId: String): Flow<List<AnimalEntity>>

    @Query(
        """
        SELECT * FROM lote_animales
        WHERE animalId = :animalId
        ORDER BY fechaIngreso DESC
        """
    )
    fun observarHistorialDelAnimal(animalId: String): Flow<List<LoteAnimalEntity>>

    @Query(
        """
        UPDATE lote_animales
        SET fechaSalida = :fechaSalida,
            motivoSalida = :motivoSalida,
            actualizadoEn = :fechaActualizacion
        WHERE animalId = :animalId AND fechaSalida IS NULL
        """
    )
    suspend fun finalizarAsignacionActiva(
        animalId: String,
        fechaSalida: Long,
        motivoSalida: String,
        fechaActualizacion: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE lotes
        SET estado = 'CERRADO',
            fechaCierre = :fechaCierre,
            actualizadoEn = :fechaActualizacion
        WHERE id = :loteId AND estado = 'ACTIVO'
        """
    )
    suspend fun cerrarLote(
        loteId: String,
        fechaCierre: Long,
        fechaActualizacion: Long = System.currentTimeMillis()
    ): Int
}

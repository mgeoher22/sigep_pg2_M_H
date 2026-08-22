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

/**
 * Administra lotes y el historial de asignaciones de animales.
 */
@Dao
interface LoteDao {

    /** Crea un lote y rechaza códigos de lote duplicados. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(lote: LoteEntity)

    /** Actualiza los datos generales de un lote. */
    @Update
    suspend fun actualizar(lote: LoteEntity)

    /** Observa todos los lotes registrados. */
    @Query("SELECT * FROM lotes ORDER BY nombre")
    fun observarTodos(): Flow<List<LoteEntity>>

    /** Observa solamente los lotes que continúan abiertos. */
    @Query("SELECT * FROM lotes WHERE estado = 'ACTIVO' ORDER BY nombre")
    fun observarActivos(): Flow<List<LoteEntity>>

    /** Busca un lote mediante su identificador interno. */
    @Query("SELECT * FROM lotes WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): LoteEntity?

    /** Busca un lote mediante el código utilizado por la finca. */
    @Query("SELECT * FROM lotes WHERE codigo = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): LoteEntity?

    /** Inserta directamente un registro en el historial de asignaciones. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarAsignacion(asignacion: LoteAnimalEntity)

    /**
     * Asigna un animal dentro de una transacción.
     * La comprobación evita que pertenezca a dos lotes activos al mismo tiempo.
     */
    @Transaction
    suspend fun asignarAnimal(asignacion: LoteAnimalEntity) {
        check(buscarAsignacionActiva(asignacion.animalId) == null) {
            "El animal ya pertenece a un lote activo"
        }
        insertarAsignacion(asignacion)
    }

    /** Obtiene la asignación que todavía no tiene fecha de salida. */
    @Query(
        """
        SELECT * FROM lote_animales
        WHERE animalId = :animalId AND fechaSalida IS NULL
        LIMIT 1
        """
    )
    suspend fun buscarAsignacionActiva(animalId: String): LoteAnimalEntity?

    /** Observa los animales activos que actualmente pertenecen a un lote. */
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

    /** Observa todos los lotes por los que ha pasado un animal. */
    @Query(
        """
        SELECT * FROM lote_animales
        WHERE animalId = :animalId
        ORDER BY fechaIngreso DESC
        """
    )
    fun observarHistorialDelAnimal(animalId: String): Flow<List<LoteAnimalEntity>>

    /** Finaliza la pertenencia actual del animal sin eliminar el registro. */
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

    /** Cierra un lote y conserva toda la información asociada. */
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

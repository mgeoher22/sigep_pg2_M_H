package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fincahernandez.gestionpecuaria.data.local.entity.EmpleadoEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PagoEmpleadoEntity
import kotlinx.coroutines.flow.Flow

/** Consultas de empleados y su historial individual de pagos. */
@Dao
interface EmpleadoDao {
    @Query("SELECT * FROM empleados ORDER BY nombreCompleto")
    fun observarTodos(): Flow<List<EmpleadoEntity>>

    @Query("SELECT * FROM pagos_empleados ORDER BY fechaPago DESC, creadoEn DESC")
    fun observarPagos(): Flow<List<PagoEmpleadoEntity>>

    @Query("SELECT * FROM empleados WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): EmpleadoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(empleado: EmpleadoEntity)

    @Update
    suspend fun actualizar(empleado: EmpleadoEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarPago(pago: PagoEmpleadoEntity)
}

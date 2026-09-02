package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fincahernandez.gestionpecuaria.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

/** Operaciones necesarias para autenticar y administrar cuentas locales. */
@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios ORDER BY nombreCompleto COLLATE NOCASE")
    fun observarUsuarios(): Flow<List<UsuarioEntity>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contarUsuarios(): Int

    @Query("SELECT * FROM usuarios WHERE usuarioNormalizado = :usuario LIMIT 1")
    suspend fun buscarPorUsuario(usuario: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): UsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: UsuarioEntity)

    /** Recuperación local: elimina cuentas sin tocar ninguna tabla productiva. */
    @Query("DELETE FROM usuarios")
    suspend fun eliminarTodos()
}

package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fincahernandez.gestionpecuaria.data.local.entity.ConfiguracionLecheEntity
import kotlinx.coroutines.flow.Flow

/** Acceso a la única configuración vigente de venta y pago de la leche. */
@Dao
interface ConfiguracionLecheDao {
    @Query("SELECT * FROM configuracion_leche WHERE id = 1 LIMIT 1")
    fun observar(): Flow<ConfiguracionLecheEntity?>

    /** Reemplaza la configuración anterior sin tocar la producción histórica. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(configuracion: ConfiguracionLecheEntity)
}

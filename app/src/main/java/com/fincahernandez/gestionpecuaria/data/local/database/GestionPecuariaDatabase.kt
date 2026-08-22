package com.fincahernandez.gestionpecuaria.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fincahernandez.gestionpecuaria.data.local.dao.AnimalDao
import com.fincahernandez.gestionpecuaria.data.local.dao.LoteDao
import com.fincahernandez.gestionpecuaria.data.local.dao.PesajeDao
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteAnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity

@Database(
    entities = [
        AnimalEntity::class,
        LoteEntity::class,
        LoteAnimalEntity::class,
        PesajeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GestionPecuariaDatabase : RoomDatabase() {

    abstract fun animalDao(): AnimalDao

    abstract fun loteDao(): LoteDao

    abstract fun pesajeDao(): PesajeDao

    companion object {
        private const val DATABASE_NAME = "gestion_pecuaria.db"

        @Volatile
        private var instancia: GestionPecuariaDatabase? = null

        fun obtenerInstancia(context: Context): GestionPecuariaDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    GestionPecuariaDatabase::class.java,
                    DATABASE_NAME
                ).build().also { nuevaInstancia ->
                    instancia = nuevaInstancia
                }
            }
        }
    }
}

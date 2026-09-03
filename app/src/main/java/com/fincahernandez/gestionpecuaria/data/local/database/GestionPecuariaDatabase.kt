package com.fincahernandez.gestionpecuaria.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fincahernandez.gestionpecuaria.data.local.dao.AnimalDao
import com.fincahernandez.gestionpecuaria.data.local.dao.EventoSanitarioDao
import com.fincahernandez.gestionpecuaria.data.local.dao.LoteDao
import com.fincahernandez.gestionpecuaria.data.local.dao.PesajeDao
import com.fincahernandez.gestionpecuaria.data.local.dao.UsuarioDao
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.EventoSanitarioEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteAnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.UsuarioEntity

/**
 * Clase central de Room para la base local `gestion_pecuaria.db`.
 *
 * Declara las tablas disponibles y proporciona acceso a sus DAO. Se utiliza
 * una sola instancia para evitar abrir varias conexiones a la misma base.
 */
@Database(
    entities = [
        AnimalEntity::class,
        LoteEntity::class,
        LoteAnimalEntity::class,
        PesajeEntity::class,
        UsuarioEntity::class,
        EventoSanitarioEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class GestionPecuariaDatabase : RoomDatabase() {

    abstract fun animalDao(): AnimalDao

    abstract fun loteDao(): LoteDao

    abstract fun pesajeDao(): PesajeDao

    abstract fun usuarioDao(): UsuarioDao

    abstract fun eventoSanitarioDao(): EventoSanitarioDao

    companion object {
        private const val DATABASE_NAME = "gestion_pecuaria.db"

        /**
         * Conserva los pesajes existentes al cambiar la unidad oficial de kg a libras.
         * Primero renombra la columna y luego convierte numéricamente cada registro.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE pesajes RENAME COLUMN pesoKg TO pesoLibras")
                database.execSQL("UPDATE pesajes SET pesoLibras = pesoLibras * 2.2046226218")
            }
        }

        /** Añade los campos visuales de HU-03 sin eliminar animales existentes. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE animales ADD COLUMN tipoOrigen TEXT NOT NULL DEFAULT 'NACIDO_EN_FINCA'"
                )
                database.execSQL(
                    "ALTER TABLE animales ADD COLUMN estadoSalud TEXT NOT NULL DEFAULT 'EXCELENTE'"
                )
            }
        }

        /**
         * Incorpora las cuentas locales sin modificar las tablas productivas existentes.
         * La primera cuenta administrativa se crea desde la pantalla de configuración.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `usuarios` (
                        `id` TEXT NOT NULL,
                        `nombreCompleto` TEXT NOT NULL,
                        `usuario` TEXT NOT NULL,
                        `usuarioNormalizado` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL,
                        `passwordAlgorithm` TEXT NOT NULL,
                        `passwordIterations` INTEGER NOT NULL,
                        `rol` TEXT NOT NULL,
                        `activo` INTEGER NOT NULL,
                        `creadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_usuarios_usuarioNormalizado` " +
                        "ON `usuarios` (`usuarioNormalizado`)"
                )
            }
        }

        /** Conserva los animales existentes y permite asociarles una foto local. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE animales ADD COLUMN fotoUri TEXT")
            }
        }

        /**
         * Permite partir de un rol preestablecido y guardar excepciones para cada cuenta.
         * Los usuarios existentes reciben NULL y conservan exactamente su acceso actual.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE usuarios ADD COLUMN permisosPersonalizados TEXT"
                )
            }
        }

        /** Crea la ficha clínica individual sin modificar animales existentes. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `eventos_sanitarios` (
                        `id` TEXT NOT NULL,
                        `animalId` TEXT NOT NULL,
                        `loteIdReferencia` TEXT,
                        `tipoEvento` TEXT NOT NULL,
                        `fechaEvento` INTEGER NOT NULL,
                        `diagnostico` TEXT NOT NULL,
                        `medicamento` TEXT,
                        `dosis` TEXT,
                        `estadoSalud` TEXT NOT NULL,
                        `proximoControl` INTEGER,
                        `responsable` TEXT,
                        `observaciones` TEXT,
                        `creadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`animalId`) REFERENCES `animales`(`id`)
                            ON UPDATE CASCADE ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_eventos_sanitarios_animalId` " +
                        "ON `eventos_sanitarios` (`animalId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_eventos_sanitarios_fechaEvento` " +
                        "ON `eventos_sanitarios` (`fechaEvento`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_eventos_sanitarios_loteIdReferencia` " +
                        "ON `eventos_sanitarios` (`loteIdReferencia`)"
                )
            }
        }

        // @Volatile permite que todos los hilos observen la instancia actual.
        @Volatile
        private var instancia: GestionPecuariaDatabase? = null

        /** Crea la base la primera vez y reutiliza la misma instancia después. */
        fun obtenerInstancia(context: Context): GestionPecuariaDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    GestionPecuariaDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                    .build().also { nuevaInstancia ->
                    instancia = nuevaInstancia
                }
            }
        }
    }
}

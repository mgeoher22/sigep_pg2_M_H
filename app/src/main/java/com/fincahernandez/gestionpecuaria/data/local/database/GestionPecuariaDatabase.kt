package com.fincahernandez.gestionpecuaria.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fincahernandez.gestionpecuaria.data.local.dao.AnimalDao
import com.fincahernandez.gestionpecuaria.data.local.dao.EventoSanitarioDao
import com.fincahernandez.gestionpecuaria.data.local.dao.EmpleadoDao
import com.fincahernandez.gestionpecuaria.data.local.dao.LoteDao
import com.fincahernandez.gestionpecuaria.data.local.dao.PesajeDao
import com.fincahernandez.gestionpecuaria.data.local.dao.ParcelaDao
import com.fincahernandez.gestionpecuaria.data.local.dao.ProduccionLecheraDao
import com.fincahernandez.gestionpecuaria.data.local.dao.MovimientoFinancieroDao
import com.fincahernandez.gestionpecuaria.data.local.dao.UsuarioDao
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.EventoSanitarioEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.EmpleadoEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteAnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.ParcelaEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.ProduccionLecheraEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.MovimientoFinancieroEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PagoEmpleadoEntity
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
        EventoSanitarioEntity::class,
        ParcelaEntity::class,
        ProduccionLecheraEntity::class,
        MovimientoFinancieroEntity::class,
        EmpleadoEntity::class,
        PagoEmpleadoEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class GestionPecuariaDatabase : RoomDatabase() {

    abstract fun animalDao(): AnimalDao

    abstract fun loteDao(): LoteDao

    abstract fun pesajeDao(): PesajeDao

    abstract fun parcelaDao(): ParcelaDao

    abstract fun usuarioDao(): UsuarioDao

    abstract fun eventoSanitarioDao(): EventoSanitarioDao

    abstract fun produccionLecheraDao(): ProduccionLecheraDao

    abstract fun movimientoFinancieroDao(): MovimientoFinancieroDao

    abstract fun empleadoDao(): EmpleadoDao

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

        /**
         * Persiste los campos que el prototipo de lotes mantenía solamente en memoria.
         * Son anulables para conservar sin alteraciones cualquier lote creado previamente.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lotes ADD COLUMN parcelaNombre TEXT")
                database.execSQL("ALTER TABLE lotes ADD COLUMN pesoObjetivoLibras REAL")
                database.execSQL("ALTER TABLE lotes ADD COLUMN fechaSalidaEstimada INTEGER")
            }
        }

        /** Añade el inventario persistente de parcelas sin alterar los demás módulos. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `parcelas` (
                        `id` TEXT NOT NULL,
                        `codigo` TEXT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `areaHectareas` REAL NOT NULL,
                        `tipoPastura` TEXT NOT NULL,
                        `capacidadAnimales` INTEGER,
                        `estado` TEXT NOT NULL,
                        `creadoEn` INTEGER NOT NULL,
                        `actualizadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_parcelas_codigo` ON `parcelas` (`codigo`)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_parcelas_nombre` ON `parcelas` (`nombre`)"
                )
            }
        }

        /**
         * Activa la persistencia de HU-11 sin modificar las tablas ya existentes.
         * El precio queda en cada fecha para conservar su valor histórico.
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `produccion_lechera` (
                        `id` TEXT NOT NULL,
                        `fecha` INTEGER NOT NULL,
                        `litros` REAL NOT NULL,
                        `precioPorLitro` REAL NOT NULL,
                        `observaciones` TEXT,
                        `creadoEn` INTEGER NOT NULL,
                        `actualizadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_produccion_lechera_fecha` " +
                        "ON `produccion_lechera` (`fecha`)"
                )
            }
        }

        /** Incorpora los movimientos, empleados y pagos persistentes de HU-12. */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `movimientos_financieros` (
                        `id` TEXT NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `categoria` TEXT NOT NULL,
                        `monto` REAL NOT NULL,
                        `fecha` INTEGER NOT NULL,
                        `observaciones` TEXT,
                        `creadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_movimientos_financieros_fecha` " +
                        "ON `movimientos_financieros` (`fecha`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_movimientos_financieros_tipo` " +
                        "ON `movimientos_financieros` (`tipo`)"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `empleados` (
                        `id` TEXT NOT NULL,
                        `nombreCompleto` TEXT NOT NULL,
                        `cargo` TEXT NOT NULL,
                        `fechaIngreso` INTEGER NOT NULL,
                        `salarioBase` REAL NOT NULL,
                        `frecuenciaPago` TEXT NOT NULL,
                        `sectorAsignado` TEXT,
                        `telefono` TEXT,
                        `activo` INTEGER NOT NULL,
                        `creadoEn` INTEGER NOT NULL,
                        `actualizadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_empleados_nombreCompleto` " +
                        "ON `empleados` (`nombreCompleto`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_empleados_activo` ON `empleados` (`activo`)"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pagos_empleados` (
                        `id` TEXT NOT NULL,
                        `empleadoId` TEXT NOT NULL,
                        `fechaPago` INTEGER NOT NULL,
                        `monto` REAL NOT NULL,
                        `periodo` TEXT NOT NULL,
                        `observaciones` TEXT,
                        `creadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`empleadoId`) REFERENCES `empleados`(`id`)
                            ON UPDATE CASCADE ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pagos_empleados_empleadoId` " +
                        "ON `pagos_empleados` (`empleadoId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pagos_empleados_fechaPago` " +
                        "ON `pagos_empleados` (`fechaPago`)"
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
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11
                )
                    .build().also { nuevaInstancia ->
                    instancia = nuevaInstancia
                }
            }
        }
    }
}

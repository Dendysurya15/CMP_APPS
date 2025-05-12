package com.cbi.mobile_plantation.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cbi.markertph.data.model.JenisTPHModel
import com.cbi.mobile_plantation.data.database.InspectionDao
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionPathModel
import com.cbi.mobile_plantation.data.model.AbsensiModel
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.FlagESPBModel
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.TransporterModel
import com.cbi.mobile_plantation.data.model.UploadCMPModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.model.AfdelingModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.HektarPanenEntity
import com.cbi.mobile_plantation.data.model.EstateModel
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.cbi.mobile_plantation.utils.AppUtils
import java.util.concurrent.Executors

/**
 * Database Version History
 * Version 1 (Initial Release):
 * - Initial database setup with tables:
 *   - TPHNewModel
 *   - KemandoranModel
 *   - KaryawanModel
 *   - PanenEntity
 *   - ESPBEntity
 *   - FlagESPBModel
 *   - MillModel (columns: id, abbr, nama)
 *
 * Version 2:
 * - TestingAdded column 'status' (nullable String) to MillModel table
 * Version 3:
 * - delete again the column 'status' from table mill
 * Version 4:
 * - added column status_restan Int to panen_table table
 * Version 5:
 * - added new table named transporter
 * Version 6:
 * -added new column named status_upload for ESPBEntity
 * Version 9:
 * added new column dataisZipped in panen and espb
 */


@Database(
    entities = [
        TPHNewModel::class,
        KemandoranModel::class,
        KaryawanModel::class,
        PanenEntity::class,
        ESPBEntity::class,
        FlagESPBModel::class,
        MillModel::class,
        TransporterModel::class,
        UploadCMPModel::class,
        AbsensiModel::class,
        InspectionModel::class,
        InspectionPathModel::class,
        KendaraanModel::class,
        BlokModel::class,
        HektarPanenEntity::class,
        EstateModel::class,
        AfdelingModel::class,
        JenisTPHModel::class
    ],
    version = 39
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kemandoranDao(): KemandoranDao
    abstract fun karyawanDao(): KaryawanDao
    abstract fun panenDao(): PanenDao
    abstract fun espbDao(): ESPBDao
    abstract fun tphDao(): TPHDao
    abstract fun flagESPBModelDao(): FlagESPBDao // ✅ Add DAO
    abstract fun millDao(): MillDao
    abstract fun transporterDao(): TransporterDao
    abstract fun uploadCMPDao(): UploadCMPDao
    abstract fun absensiDao(): AbsensiDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun inspectionPathDao(): InspectionPathDao
    abstract fun kendaraanDao(): KendaraanDao
    abstract fun blokDao(): BlokDao
    abstract fun estateDao(): EstateDao
    abstract fun afdelingDao(): AfdelingDao
    abstract fun hektarPanenDao(): HektarPanenDao
    abstract fun jenisTPHDao(): JenisTPHDao

    // Function to restore data from backup tables if needed
//    fun restoreFromBackups() {
//        // Should be called within a transaction and background thread
//        runInTransaction {
//            panenDao().deleteAllPanen()
//            panenDao().restoreFromBackup()
//
//            espbDao().deleteAllESPB()
//            espbDao().restoreFromBackup()
//
//            Log.d("Database Restoration", "Successfully restored data from backup tables")
//        }
//    }


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cbi_cmp"
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_11_12,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_25_26,
                        MIGRATION_26_27,
                        MIGRATION_27_28,
                        MIGRATION_28_29,
                        MIGRATION_29_30,
                        MIGRATION_30_31,
                        MIGRATION_31_32,
                        MIGRATION_32_33,
                        MIGRATION_33_34,
                        MIGRATION_34_35,
                        MIGRATION_35_36,
                        MIGRATION_36_37,
                        MIGRATION_37_38
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("ALTER TABLE mill ADD COLUMN status TEXT")
//            }
//        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop the old table if it exists (to avoid conflicts)
                database.execSQL("DROP TABLE IF EXISTS mill")

                // Create the new mill table
                database.execSQL(
                    """
            CREATE TABLE mill (
                id INTEGER PRIMARY KEY,
                abbr TEXT,
                nama TEXT
            )
        """
                )
            }
        }


        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new column 'status_restan' with default value 0
                database.execSQL(
                    """
            ALTER TABLE panen_table 
            ADD COLUMN status_restan INTEGER NOT NULL DEFAULT 0
        """
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new transporter table
                database.execSQL(
                    """
            CREATE TABLE transporter (
                id INTEGER PRIMARY KEY,
                kode TEXT,
                nama TEXT,
                status INTEGER
            )
        """
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) { // ✅ Corrected version
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE espb_table 
                    ADD COLUMN status_upload_cmp INTEGER NULL
                    """
                )
                // Optional: Set default value for existing rows
                database.execSQL(
                    """
                    UPDATE espb_table 
                    SET status_upload = 0 
                    WHERE status_upload_cmp IS NULL
                    """
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS upload_cmp (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, 
                tracking_id INTEGER, 
                nama_file TEXT, 
                status INTEGER, 
                tanggal_upload TEXT, 
                table_ids TEXT
            )
            """
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE panen_table ADD COLUMN dataIsZipped INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE espb_table ADD COLUMN dataIsZipped INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE absensi ADD COLUMN dataIsZipped INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE panen_table ADD COLUMN wilayah TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE panen_table ADD COLUMN divisi_nama TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE panen_table ADD COLUMN blok_ppro INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE panen_table ADD COLUMN blok_nama TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE panen_table ADD COLUMN status_banjir INTEGER NOT NULL DEFAULT 0")
            }
        }


        private val MIGRATION_13_14 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE mill_table ADD COLUMN ip TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE espb_table ADD COLUMN uploaded_at_ppro_wb TEXT DEFAULT ''")
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 1: Create a temporary table with the new schema
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS upload_cmp_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                tracking_id TEXT, 
                nama_file TEXT, 
                status INTEGER,
                tanggal_upload TEXT, 
                table_ids TEXT
            )
            """
                )

                // Step 2: Copy data from the old table to the new table, converting only tracking_id to String
                database.execSQL(
                    """
            INSERT INTO upload_cmp_temp (id, tracking_id, nama_file, status, tanggal_upload, table_ids) 
            SELECT id, 
                   CAST(tracking_id AS TEXT), 
                   nama_file, 
                   status,  
                   tanggal_upload, 
                   table_ids 
            FROM ${AppUtils.DatabaseTables.UPLOADCMP}
            """
                )

                // Step 3: Drop the old table
                database.execSQL("DROP TABLE ${AppUtils.DatabaseTables.UPLOADCMP}")

                // Step 4: Rename the new table to the original name
                database.execSQL("ALTER TABLE upload_cmp_temp RENAME TO ${AppUtils.DatabaseTables.UPLOADCMP}")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 1: Create a temporary table with the new schema
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS upload_cmp_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                tracking_id INTEGER, 
                nama_file TEXT, 
                status INTEGER,
                tanggal_upload TEXT, 
                table_ids TEXT
            )
            """
                )

                // Step 2: Copy data from the old table to the new table, converting tracking_id back to INTEGER
                database.execSQL(
                    """
            INSERT INTO upload_cmp_temp (id, tracking_id, nama_file, status, tanggal_upload, table_ids) 
            SELECT id, 
                   CAST(tracking_id AS INTEGER), 
                   nama_file, 
                   status,  
                   tanggal_upload, 
                   table_ids 
            FROM ${AppUtils.DatabaseTables.UPLOADCMP}
            """
                )

                // Step 3: Drop the old table
                database.execSQL("DROP TABLE ${AppUtils.DatabaseTables.UPLOADCMP}")

                // Step 4: Rename the new table to the original name
                database.execSQL("ALTER TABLE upload_cmp_temp RENAME TO ${AppUtils.DatabaseTables.UPLOADCMP}")
            }
        }

        private val MIGRATION_27_28 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 1: Create a temporary table with the new schema
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS upload_cmp_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                tracking_id TEXT, 
                nama_file TEXT, 
                status INTEGER,
                tanggal_upload TEXT, 
                table_ids TEXT
            )
            """
                )

                // Step 2: Copy data from the old table to the new table, converting only tracking_id to String
                database.execSQL(
                    """
            INSERT INTO upload_cmp_temp (id, tracking_id, nama_file, status, tanggal_upload, table_ids) 
            SELECT id, 
                   CAST(tracking_id AS TEXT), 
                   nama_file, 
                   status,  
                   tanggal_upload, 
                   table_ids 
            FROM ${AppUtils.DatabaseTables.UPLOADCMP}
            """
                )

                // Step 3: Drop the old table
                database.execSQL("DROP TABLE ${AppUtils.DatabaseTables.UPLOADCMP}")

                // Step 4: Rename the new table to the original name
                database.execSQL("ALTER TABLE upload_cmp_temp RENAME TO ${AppUtils.DatabaseTables.UPLOADCMP}")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
            CREATE TABLE estate (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                id_ppro INTEGER,
                abbr TEXT,
                nama TEXT
            )
            """.trimIndent()
                )
            }
        }


        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS afdeling (
                id INTEGER PRIMARY KEY NOT NULL,
                id_ppro INTEGER,
                abbr TEXT,
                nama TEXT,
                estate_id INTEGER NOT NULL
            )
            """.trimIndent()
                )
            }
        }


        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add dept_nama column to TPHNewModel table
                database.execSQL("ALTER TABLE TPHNewModel ADD COLUMN dept_nama TEXT")
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add dept_nama column to TPHNewModel table
                database.execSQL("ALTER TABLE TPHNewModel ADD COLUMN company_nama TEXT")
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add dept_nama column to PanenEntity table
                database.execSQL("ALTER TABLE PanenEntity ADD COLUMN karyawan_nama TEXT")
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add status_upload column to PanenEntity table with default value 0
                database.execSQL("ALTER TABLE panen ADD COLUMN status_upload INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE espb_table ADD COLUMN status_upload INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to PanenEntity table
                database.execSQL("ALTER TABLE panen ADD COLUMN status_pengangkutan INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE panen ADD COLUMN status_insert_mpanen INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE panen ADD COLUMN status_scan_mpanen INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE panen ADD COLUMN jumlah_pemanen INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new column to PanenEntity table
                database.execSQL("ALTER TABLE panen ADD COLUMN status_uploaded_image STRING NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new column to tph table
                database.execSQL("ALTER TABLE tph ADD COLUMN jenis_tph_id STRING NOT NULL DEFAULT 1")

                // Create the JenisTPH table
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + AppUtils.DatabaseTables.JENIS_TPH + " (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "jenis_tph TEXT, " +
                            "limit INTEGER, " +
                            "keterangan TEXT)"
                )
            }
        }

        val MIGRATION_37_38 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("ALTER TABLE tph ADD COLUMN limit_tph TEXT NOT NULL DEFAULT 1")
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

package com.cbi.mobile_plantation.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.cbi.mobile_plantation.utils.AppUtils

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
    ],
    version = 25
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
                        MIGRATION_14_15
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


        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

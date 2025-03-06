package com.cbi.mobile_plantation.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    ],
    version = 11
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
                        MIGRATION_9_10
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

        private val MIGRATION_10_11 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Rename old table
                database.execSQL("ALTER TABLE ESPB RENAME TO ESPB_old")

                // Create new table with updated schema
                database.execSQL(
                    """CREATE TABLE ESPB (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                blok_jjg TEXT NOT NULL,
                created_by_id INTEGER NOT NULL,
                created_at TEXT NOT NULL,
                nopol TEXT NOT NULL,
                driver TEXT NOT NULL,
                transporter_id INTEGER NOT NULL,
                pemuat_id TEXT NOT NULL,
                mill_id INTEGER NOT NULL,
                archive INTEGER NOT NULL,
                tph0 TEXT NOT NULL,
                tph1 TEXT NOT NULL,
                update_info_sp TEXT NOT NULL DEFAULT 'NULL',
                uploaded_by_id_wb INTEGER NOT NULL DEFAULT 0,
                uploaded_at_wb TEXT NOT NULL DEFAULT 'NULL',
                uploaded_by_id_sp INTEGER NOT NULL DEFAULT 0,
                uploaded_at_sp TEXT NOT NULL DEFAULT 'NULL',
                status_upload_cmp_sp INTEGER NOT NULL DEFAULT 0,
                status_upload_cmp_wb INTEGER NOT NULL DEFAULT 0,
                status_upload_ppro_wb INTEGER NOT NULL DEFAULT 0,
                status_draft INTEGER NOT NULL DEFAULT 0,
                status_mekanisasi INTEGER NOT NULL DEFAULT 0,
                creator_info TEXT NOT NULL,
                uploader_info_sp TEXT NOT NULL DEFAULT 'NULL',
                uploader_info_wb TEXT NOT NULL DEFAULT 'NULL',
                noESPB TEXT NOT NULL,
                scan_status INTEGER NOT NULL DEFAULT 0,
                dataIsZipped INTEGER NOT NULL DEFAULT 0
            )"""
                )

                // Copy data from old table to new table
                database.execSQL(
                    """INSERT INTO ESPB (
                id, blok_jjg, created_by_id, created_at, nopol, driver, transporter_id, pemuat_id, mill_id, archive,
                tph0, tph1, update_info_sp, uploaded_by_id_wb, uploaded_at_wb, status_upload_cmp_wb, 
                status_upload_ppro_wb, status_draft, status_mekanisasi, creator_info, uploader_info_wb, 
                noESPB, scan_status, dataIsZipped
            ) 
            SELECT id, blok_jjg, created_by_id, created_at, nopol, driver, transporter_id, pemuat_id, mill_id, archive,
                   tph0, tph1, update_info, uploaded_by_id, uploaded_at, status_upload_cmp, 
                   status_upload_ppro, status_draft, status_mekanisasi, creator_info, uploader_info, 
                   noESPB, scan_status, dataIsZipped
            FROM ESPB_old"""
                )

                // Drop old table
                database.execSQL("DROP TABLE ESPB_old")
            }
        }






        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

package com.cbi.cmp_project.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cbi.cmp_project.data.model.ESPBEntity

@Database(entities = [ESPBEntity::class], version = 1, exportSchema = false)
abstract class EspbDatabase : RoomDatabase() {
    abstract fun espbDao(): EspbDao

    companion object {
        @Volatile
        private var INSTANCE: EspbDatabase? = null

        fun getDatabase(context: Context): EspbDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EspbDatabase::class.java,
                    "espb_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

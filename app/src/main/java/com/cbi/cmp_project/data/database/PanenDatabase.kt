package com.cbi.cmp_project.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cbi.cmp_project.data.model.ESPBEntity

@Database(entities = [ESPBEntity::class], version = 1, exportSchema = false)
abstract class PanenDatabase : RoomDatabase() {
    abstract fun PanenDao(): PanenDao

    companion object {
        @Volatile
        private var INSTANCE: PanenDatabase? = null

        fun getDatabase(context: Context): PanenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PanenDatabase::class.java,
                    "panen_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
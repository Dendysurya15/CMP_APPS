package com.cbi.cmp_project.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel

@Database(
    entities = [
        RegionalModel::class,
        WilayahModel::class,
        DeptModel::class,
        DivisiModel::class,
        BlokModel::class,
        TPHNewModel::class,
        KemandoranModel::class,
        KemandoranDetailModel::class,
        KaryawanModel::class,
        PanenEntity::class,
        ESPBEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun regionalDao(): RegionalDao
    abstract fun wilayahDao(): WilayahDao
    abstract fun deptDao(): DeptDao
    abstract fun divisiDao(): DivisiDao
    abstract fun blokDao(): BlokDao
    abstract fun kemandoranDao(): KemandoranDao
    abstract fun kemandoranDetailDao(): KemandoranDetailDao
    abstract fun karyawanDao(): KaryawanDao
    abstract fun panenDao(): PanenDao
    abstract fun espbDao(): ESPBDao
    abstract fun tphDao(): TPHDao



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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

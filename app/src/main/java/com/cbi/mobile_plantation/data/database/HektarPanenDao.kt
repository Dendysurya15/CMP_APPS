package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.model.AbsensiModel
import com.cbi.mobile_plantation.data.model.HektarPanenEntity

@Dao
abstract class HektarPanenDao {
    @Query("SELECT COUNT(*) FROM hektar_panen WHERE strftime('%Y-%m-%d', date_created_panen) = strftime('%Y-%m-%d', 'now', 'localtime')")
    abstract suspend fun getCountCreatedToday(): Int

    @Query("SELECT COUNT(*) FROM hektar_panen WHERE strftime('%Y-%m-%d', date_created_panen) = strftime('%Y-%m-%d', 'now', 'localtime', '-1 day')")
    abstract suspend fun getCountCreatedYesterday(): Int

    @Query("SELECT * FROM hektar_panen")
    abstract fun getAll(): List<HektarPanenEntity>

    //getalltoday
    @Query("SELECT * FROM hektar_panen WHERE strftime('%Y-%m-%d', date_created_panen) = strftime('%Y-%m-%d', 'now', 'localtime')")
    abstract fun getAllToday(): List<HektarPanenEntity>

    //getallyesterday
    @Query("SELECT * FROM hektar_panen WHERE strftime('%Y-%m-%d', date_created_panen) = strftime('%Y-%m-%d', 'now', 'localtime', '-1 day')")
    abstract fun getAllYesterday(): List<HektarPanenEntity>

    //getallbydate
    @Query("SELECT * FROM hektar_panen WHERE strftime('%Y-%m-%d', date_created_panen) = :date")
    abstract fun getAllByDate(date: String): List<HektarPanenEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insertAll(tph: List<HektarPanenEntity>)

    //update luasan by id
    @Query("UPDATE hektar_panen SET luas_panen = :luas_panen WHERE id = :id")
    abstract fun updateLuasPanenById(id: Int, luas_panen: Float)

    //get all where luas_panen is 0
    @Query("SELECT * FROM hektar_panen WHERE luas_panen = 0")
    abstract fun getAllWhereLuasPanenIsZero(): List<HektarPanenEntity>

    //get all where luas_panen is 0 now
    @Query("SELECT * FROM hektar_panen WHERE luas_panen = 0 AND strftime('%Y-%m-%d', date_created_panen) = strftime('%Y-%m-%d', 'now', 'localtime')")
    abstract fun getAllWhereLuasPanenIsZeroNow(): List<HektarPanenEntity>

    //get all where luas_panen is 0 yesterday
    @Query("SELECT * FROM hektar_panen WHERE luas_panen = 0 AND strftime('%Y-%m-%d', date_created_panen) = strftime('%Y-%m-%d', 'now', 'localtime', '-1 day')")
    abstract fun getAllWhereLuasPanenIsZeroYesterday(): List<HektarPanenEntity>


    //get all where luas_panen is 0 by date
    @Query("SELECT * FROM hektar_panen WHERE luas_panen = 0 AND strftime('%Y-%m-%d', date_created_panen) = :date")
    abstract fun getAllWhereLuasPanenIsZeroByDate(date: String): List<HektarPanenEntity>

    @Insert
    abstract fun insert(hektarPanen: HektarPanenEntity): Long

    @Query("SELECT * FROM hektar_panen WHERE nik = :nik AND blok = :blok AND date_created_panen = :date")
    abstract fun getByNikBlokDate(nik: String, blok: Int, date: String): HektarPanenEntity?

    @Update
    abstract fun update(hektarPanen: HektarPanenEntity)

    @Query("SELECT * FROM hektar_panen WHERE nik = :nik AND blok = :blokId AND date_created_panen LIKE '%' || :date || '%'")
    abstract fun getByNikAndBlokDate(nik: String, blokId: Int, date: String): HektarPanenEntity?

    //count where luas_panen is 0 and date today
    @Query("SELECT COUNT(*) FROM hektar_panen WHERE luas_panen = 0 AND strftime('%Y-%m-%d', date_created_panen) = strftime('%Y-%m-%d', 'now', 'localtime')")
    abstract fun countWhereLuasPanenIsZeroAndDateToday(): Int
}

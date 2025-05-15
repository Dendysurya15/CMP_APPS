package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cbi.mobile_plantation.data.model.HektarPanenEntity
import com.cbi.mobile_plantation.utils.AppUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    @Query("UPDATE hektar_panen SET dataIsZipped = :status WHERE id IN (:ids)")
    abstract  suspend fun updateDataIsZippedHP(ids: List<Int>, status: Int)

    @Query("UPDATE hektar_panen SET status_upload = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadHektarPanen(ids: List<Int>, status: Int)

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
    @Query("SELECT * FROM hektar_panen WHERE luas_panen = 0.0 AND strftime('%Y-%m-%d', date_created_panen) = :date")
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
    @Query("SELECT COUNT(*) FROM hektar_panen WHERE luas_panen = 0.0 AND date_created_panen LIKE '%' || :date || '%'")
    abstract fun countWhereLuasPanenIsZeroAndDate(date: String= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())): Int

    //select blok distinct from hektar_panen where date_created_panen like '%' || :date || '%'
    @Query("SELECT DISTINCT blok FROM hektar_panen WHERE date_created_panen LIKE '%' || :date || '%'")
    abstract fun getDistinctBlokByDate(date: String): List<Int>

    //select nik, luas_panen, luas_blok, dibayar from hektar_panen where date_created_panen like '%' || :date || '%' and blok = :blok
    @Query("SELECT * FROM hektar_panen WHERE date_created_panen LIKE '%' || :date || '%' AND blok = :blok")
    abstract fun getNikLuasPanenLuasBlokDibayarByDateAndBlok(date: String, blok: Int): List<HektarPanenEntity>

    //select nik, luas_panen, luas_blok, dibayar from hektar_panen where date_created_panen like '%' || :date || '%' and blok = :blok
    @Query("SELECT * FROM hektar_panen WHERE date_created_panen LIKE '%' || :date || '%'")
    abstract fun getNikLuasPanenLuasBlokDibayarByDateAndBlok(date: String): List<HektarPanenEntity>

    @Query("SELECT * FROM hektar_panen WHERE blok = :blok")
    abstract fun getNikLuasPanenLuasBlokDibayarByDateAndBlok(blok: Int): List<HektarPanenEntity>

    @Query("SELECT * FROM hektar_panen")
    abstract fun getNikLuasPanenLuasBlokDibayarByDateAndBlok(): List<HektarPanenEntity>

    //updateLuasPanenbyid
    @Query("UPDATE hektar_panen SET luas_panen = :luas_panen WHERE id = :id")
    abstract fun updateLuasPanen(id: Int, luas_panen: Float): Int

    //getluasblokbyblok
    @Query("SELECT luas_blok FROM hektar_panen WHERE blok = :blok")
    abstract fun getLuasBlokByBlok(blok: Int): Float

    @Query("SELECT COUNT(*) FROM hektar_panen WHERE luas_panen = 0.0 AND date_created_panen LIKE '%' || :date || '%' AND blok = :blok")
    abstract fun countWhereLuasPanenIsZeroAndDateAndBlok(blok: Int, date: String?= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())): Int

    //luas_blok - sum luas panen where date_created_panen like '%' || :date || '%' and blok = :blok
    @Query("SELECT SUM(luas_panen) FROM hektar_panen WHERE date_created_panen LIKE '%' || :date || '%' AND blok = :blok")
    abstract fun getSumLuasPanen(blok: Int, date: String= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())): Float

    @Query("SELECT * FROM ${AppUtils.DatabaseTables.HEKTAR_PANEN} WHERE id = :id")
    abstract suspend fun getById(id: Int): HektarPanenEntity?

    @Query("SELECT SUM(luas_panen) FROM ${AppUtils.DatabaseTables.HEKTAR_PANEN} WHERE blok = :blokId AND date_created LIKE :dateOnly AND id != :excludeId")
    abstract suspend fun getTotalLuasPanenForBlokAndDate(blokId: Int, dateOnly: String, excludeId: Int): Float

    @Query("UPDATE ${AppUtils.DatabaseTables.HEKTAR_PANEN} SET luas_panen = :newValue WHERE id = :id")
    abstract suspend fun updateLuasPanenBaru(id: Int, newValue: Float): Int

}

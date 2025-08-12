package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cbi.mobile_plantation.data.model.MutuBuahEntity

@Dao
interface MutuBuahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(mutuBuah: MutuBuahEntity): Long // Returns row ID of inserted item

    @Query("SELECT COUNT(*) FROM mutu_buah WHERE tanggal = strftime('%Y-%m-%d', 'now', 'localtime')")
    abstract suspend fun getCountCreatedToday(): Int

}
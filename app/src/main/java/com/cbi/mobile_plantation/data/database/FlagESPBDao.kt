package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cbi.mobile_plantation.data.model.FlagESPBModel

@Dao

interface FlagESPBDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(flag: FlagESPBModel)

    @Query("SELECT COUNT(*) FROM flag_espb")
    suspend fun getCount(): Int
}
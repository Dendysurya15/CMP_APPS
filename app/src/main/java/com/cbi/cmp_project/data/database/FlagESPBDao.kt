package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cbi.cmp_project.data.model.FlagESPBModel

@Dao

interface FlagESPBDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(flag: FlagESPBModel)

    @Query("SELECT COUNT(*) FROM flag_espb")
    suspend fun getCount(): Int
}
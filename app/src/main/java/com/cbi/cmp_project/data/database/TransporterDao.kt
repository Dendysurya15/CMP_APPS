package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.TransporterModel

@Dao
abstract class TransporterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(transporter: List<TransporterModel>)

    @Transaction
    open suspend fun InsertTransporter(transporter: List<TransporterModel>) {

        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(transporter)
    }

    @Query("DELETE FROM transporter")
    abstract fun deleteAll()



    @Query("SELECT COUNT(*) FROM transporter")
    abstract suspend fun getCount(): Int

    @Query(
        """
    SELECT * FROM transporter 
    WHERE id = :idTransporter
    """
    )
    abstract fun getTransporterById(
        idTransporter: Int,
    ): List<TransporterModel>
}
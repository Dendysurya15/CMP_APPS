package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.AfdelingModel
import com.cbi.mobile_plantation.data.model.KemandoranModel

@Dao
abstract class AfdelingDao {
    @Transaction
    open suspend fun updateOrInsertAfdeling(afdelings: List<AfdelingModel>) {
        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(afdelings)
    }

    @Query("SELECT COUNT(*) FROM afdeling")
    abstract suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(afdelings: List<AfdelingModel>)

    @Query("DELETE FROM afdeling")
    abstract fun deleteAll()

    @Query("SELECT * FROM afdeling WHERE id = :afdelingId LIMIT 1")
    abstract suspend fun getAfdelingById(afdelingId: Int): AfdelingModel?



    @Query(
        """
    SELECT * FROM afdeling 
    WHERE estate_id = :idEstate
    """
    )
    abstract fun getListAfdelingFromIdEstate(
        idEstate: String
    ): List<AfdelingModel>

}
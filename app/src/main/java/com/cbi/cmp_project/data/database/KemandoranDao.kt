package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.TransporterModel

@Dao
abstract class KemandoranDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(wilayah: List<KemandoranModel>)

    @Query("DELETE FROM kemandoran")
    abstract fun deleteAll()

    @Transaction
    open suspend fun updateOrInsertKemandoran(kemandoran: List<KemandoranModel>) {
        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(kemandoran)
    }

    @Query(
        """
    SELECT * FROM kemandoran 
    WHERE id IN (:idKemandoran)
    """
    )
    abstract fun getKemandoranById(
        idKemandoran: List<String>,
    ): List<KemandoranModel>

    @Query("SELECT COUNT(*) FROM kemandoran")
    abstract suspend fun getCount(): Int

    @Query(
        """
    SELECT * FROM kemandoran 
    WHERE dept = :idEstate 
    AND divisi IN (:idDivisiArray)
    """
    )
    abstract fun getKemandoranByCriteria(
        idEstate: Int,
        idDivisiArray: List<Int>
    ): List<KemandoranModel>

    @Query(
        """
    SELECT * FROM kemandoran 
    WHERE dept = :idEstate
    """
    )
    abstract fun getKemandoranEstate(
        idEstate: Int,
    ): List<KemandoranModel>

    @Query(
        """
    UPDATE kemandoran
    SET date_absen = :date_absen
    AND status_absen = :status_absen
    WHERE id IN (:idKemandoran)
    """
    )
    abstract fun updateKemandoran(
        date_absen: String,
        status_absen: String,
        idKemandoran: String,
    ): Int

    @Query(
        """
    SELECT * FROM transporter
    """
    )
    abstract fun getAllTransporter(): List<TransporterModel>




}

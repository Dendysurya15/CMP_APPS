package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.AbsensiModel

class AbsensiRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val absensiDao = database.absensiDao()

    suspend fun saveDataAbsensi(
        kemandoran_id: String,
        date_created: String,
        created_by: Int,
        karyawan_msk_id: String,
        karyawan_tdk_msk_id: String,
        foto: String,
        komentar: String,
        asistensi: Int,
        lat: Double,
        lon: Double,
        info:String,
        archive: Int,
    ): Result<Long> {
        val absensiModel = AbsensiModel(
            kemandoran_id = kemandoran_id,
            date_created = date_created,
            created_by = created_by,
            karyawan_msk_id = karyawan_msk_id,
            karyawan_tdk_msk_id = karyawan_tdk_msk_id,
            foto = foto,
            komentar = komentar,
            asistensi = asistensi,
            lat = lat,
            lon = lon,
            info = info,
            archive = archive
        )
        return absensiDao.insertWithTransaction(absensiModel)
    }
}
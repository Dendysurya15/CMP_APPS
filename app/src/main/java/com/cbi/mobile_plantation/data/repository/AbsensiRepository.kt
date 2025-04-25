package com.cbi.mobile_plantation.data.repository

import android.content.Context

import androidx.sqlite.db.SimpleSQLiteQuery
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.AbsensiKemandoranRelations
import com.cbi.mobile_plantation.data.model.AbsensiModel
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AbsensiRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val absensiDao = database.absensiDao()
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()

    suspend fun insertAbsensiData(absensiData: AbsensiModel) {
        absensiDao.insertAbsensiData(absensiData)
    }

//    suspend fun insertAbsensiDataLokal(absensiDataLokal: AbsensiModelScan) {
//        absensiDao.insertAbsensiDataLokal(absensiDataLokal)
//    }


    suspend fun getPemuatByIdList(idPemuat: List<String>): List<KaryawanModel> {
        return karyawanDao.getPemuatByIdList(idPemuat)
    }

    suspend fun getKemandoranById(idKemandoran: List<String>): List<KemandoranModel> {
        return kemandoranDao.getKemandoranById(idKemandoran)
    }

    suspend fun getAbsensiCount(): Int {
        return absensiDao.getCountAbsensi()
    }

    suspend fun getAbsensiCountArhive(load_status_scan: Int): Int {
        return absensiDao.getCountArchiveAbsensi(load_status_scan)
    }

    fun isAbsensiExist(dateAbsen: String, karyawanMskIds: List<String>): Boolean {
        return karyawanMskIds.any { karyawanId ->
            absensiDao.checkIfExists(dateAbsen, karyawanId) > 0
        }
    }

    suspend fun archiveAbsensiById(id: Int) = withContext(Dispatchers.IO) {
        absensiDao.archiveAbsensiByID(id)
    }

    suspend fun getActiveAbsensi(): Result<List<AbsensiKemandoranRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = absensiDao.getAllActiveAbsensiWithRelations()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArchivedAbsensi(): Result<List<AbsensiKemandoranRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = absensiDao.getAllArchivedAbsensiWithRelations()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllDataAbsensi(status_scan:Int): Result<List<AbsensiKemandoranRelations>> = withContext(Dispatchers.IO) {
        try {
            val data = absensiDao.getAllDataAbsensi(status_scan)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    sealed class SaveResultAbsensi {
        object Success : SaveResultAbsensi()
        object AlreadyExists : SaveResultAbsensi()
        data class Error(val exception: Exception) : SaveResultAbsensi()
    }

    suspend fun saveDataAbsensi(
        kemandoran_id: String,
        date_absen: String,
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
            date_absen = date_absen,
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

    suspend fun updateKaryawanAbsensi(
        date_absen: String,
        status_absen: String,
        karyawan_msk_id: List<String>,
    ): Int {
        return karyawanDao.updateKaryawan(date_absen, status_absen, karyawan_msk_id)
    }

    suspend fun updateKemandoranAbsensi(
        date_absen: String,
        status_absen: String,
        kemandoran_id: String,
    ): Int {
        return kemandoranDao.updateKemandoran(date_absen, status_absen, kemandoran_id)
    }
}
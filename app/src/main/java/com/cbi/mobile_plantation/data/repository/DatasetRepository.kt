package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.mobile_plantation.data.api.ApiService
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.database.KaryawanDao

import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.model.TransporterModel
import com.cbi.mobile_plantation.data.model.dataset.DatasetRequest
import com.cbi.mobile_plantation.data.network.CMPApiClient
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.markertph.data.model.TPHNewModel
import okhttp3.ResponseBody
import retrofit2.Response

class DatasetRepository(
    context: Context,
    private val apiService: ApiService = CMPApiClient.instance,
    ) {

    private val database = AppDatabase.getDatabase(context)
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()
    private val tphDao = database.tphDao()
    private val millDao = database.millDao()
    private val transporterDao = database.transporterDao()

    suspend fun updateOrInsertKaryawan(karyawans: List<KaryawanModel>) =
        karyawanDao.updateOrInsertKaryawan(karyawans)

    suspend fun updateOrInsertMill(mills: List<MillModel>) = millDao.updateOrInsertMill(mills)
    suspend fun InsertTransporter(transporter: List<TransporterModel>) =
        transporterDao.InsertTransporter(transporter)

    suspend fun updateOrInsertKemandoran(kemandorans: List<KemandoranModel>) =
        kemandoranDao.updateOrInsertKemandoran(kemandorans)

    suspend fun updateOrInsertTPH(tph: List<TPHNewModel>) = tphDao.updateOrInsertTPH(tph)


    suspend fun getDatasetCount(datasetName: String): Int {
        return when (datasetName) {
            AppUtils.DatasetNames.pemanen -> karyawanDao.getCount()
            AppUtils.DatasetNames.kemandoran -> kemandoranDao.getCount()
            AppUtils.DatasetNames.tph -> tphDao.getCount()
            AppUtils.DatasetNames.transporter -> transporterDao.getCount()
            else -> 0
        }
    }

//    suspend fun getDeptByRegionalAndEstate(estateId: String): List<DeptModel> {
//        // Fetch dept data by regionalId and estateId
//        return deptDao.getDeptByCriteria(estateId)
//    }


    suspend fun getDivisiList(idEstate: Int): List<TPHNewModel> {
        return tphDao.getDivisiByCriteria(idEstate)
    }

    suspend fun getBlokList(idEstate: Int, idDivisi: Int): List<TPHNewModel> {
        return tphDao.getBlokByCriteria(idEstate, idDivisi)
    }

    suspend fun getLatLonDivisi(idEstate: Int, idDivisi: Int): List<TPHNewModel> {
        return tphDao.getLatLonByDivisi(idEstate, idDivisi)
    }

    suspend fun getKemandoranList(idEstate: Int, idDivisiArray: List<Int>): List<KemandoranModel> {
        return kemandoranDao.getKemandoranByCriteria(idEstate, idDivisiArray)
    }

    suspend fun getKemandoranEstate(idEstate: Int): List<KemandoranModel> {
        return kemandoranDao.getKemandoranEstate(idEstate)
    }

    suspend fun getAllTransporter(): List<TransporterModel> {
        return kemandoranDao.getAllTransporter()
    }


//    suspend fun getKemandoranAbsensiList(idEstate: Int, idDivisiArray: List<Int>): List<KemandoranModel> {
//        return kemandoranDao.getKemandoranByCriteriaAbsensi(idEstate, idDivisiArray)
//    }

    suspend fun getTPHList(
        idEstate: Int,
        idDivisi: Int,
        tahunTanam: String,
        idBlok: Int
    ): List<TPHNewModel> {
        return tphDao.getTPHByCriteria(idEstate, idDivisi, tahunTanam, idBlok)
    }
//
//    suspend fun getKemandoranDetailList(idHeader:Int): List<KemandoranDetailModel> {
//        return kemandoranDetailDao.getKemandoranDetailListByCriteria(idHeader)
//    }

    suspend fun getKaryawanList(filteredId: Int): List<KaryawanModel> {
        return karyawanDao.getKaryawanByCriteria(filteredId)
    }

    suspend fun getKaryawanKemandoranList(filteredId: List<String>): List<KaryawanDao.KaryawanKemandoranData> {
        return karyawanDao.getKaryawanKemandoranList(filteredId)
    }


    suspend fun downloadDataset(request: DatasetRequest): Response<ResponseBody> {
        return apiService.downloadDataset(request)
    }

    suspend fun downloadSmallDataset(regional: Int): Response<ResponseBody> {
        return apiService.downloadSmallDataset(mapOf("regional" to regional))
    }

    suspend fun checkStatusUploadCMP(ids: List<Int>): Response<ResponseBody> {
        val idDataString = ids.joinToString(",") // Convert list to "1,2,3,4"
        return apiService.checkStatusUploadCMP(idDataString)
    }

    suspend fun downloadSettingJson(lastModified: String): Response<ResponseBody> {
        val requestBody = mapOf("last_modified" to lastModified)
        return apiService.downloadSettingJson(requestBody)
    }
}
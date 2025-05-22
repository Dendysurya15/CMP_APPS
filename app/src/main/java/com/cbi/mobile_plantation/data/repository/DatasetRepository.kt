package com.cbi.mobile_plantation.data.repository

import android.content.Context
import com.cbi.markertph.data.model.JenisTPHModel
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
import com.cbi.mobile_plantation.data.database.DepartmentInfo
import com.cbi.mobile_plantation.data.model.AfdelingModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.EstateModel
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.cbi.mobile_plantation.data.model.uploadCMP.checkStatusUploadedData
import com.cbi.mobile_plantation.data.network.TestingAPIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response

class DatasetRepository(
    context: Context,
    private val apiService: ApiService = CMPApiClient.instance,
    private val TestingApiService: ApiService = TestingAPIClient.instance,
    ) {

    private val database = AppDatabase.getDatabase(context)
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()
    private val tphDao = database.tphDao()
    private val estateDao = database.estateDao()
    private val millDao = database.millDao()
    private val transporterDao = database.transporterDao()
    private val kendaraanDao = database.kendaraanDao()
    private val blokDao = database.blokDao()
    private val afdelingDao = database.afdelingDao()
    private val jenisTPHDao = database.jenisTPHDao()


    suspend fun updateOrInsertKaryawan(karyawans: List<KaryawanModel>) =
        karyawanDao.updateOrInsertKaryawan(karyawans)

    suspend fun updateOrInsertMill(mills: List<MillModel>) = millDao.updateOrInsertMill(mills)
    suspend fun InsertKendaraan(kendaraan: List<KendaraanModel>) =
        kendaraanDao.InsertKendaraan(kendaraan)

    suspend fun updateOrInsertJenisTPH(jenisTPH: List<JenisTPHModel>) =
        jenisTPHDao.updateOrInsertJenisTPH(jenisTPH)

    suspend fun InsertTransporter(transporter: List<TransporterModel>) =
        transporterDao.InsertTransporter(transporter)

    suspend fun updateOrInsertKemandoran(kemandorans: List<KemandoranModel>) =
        kemandoranDao.updateOrInsertKemandoran(kemandorans)

    suspend fun updateOrInsertBlok(blok: List<BlokModel>) =
        blokDao.updateOrInsertBlok(blok)

    suspend fun updateOrInsertTPH(tph: List<TPHNewModel>) = tphDao.updateOrInsertTPH(tph)

    suspend fun insertTPH(tph: List<TPHNewModel>) = tphDao.insertTPHAsistensi(tph)

    suspend fun getBlokByDivisiAndDept(divisiId: String, deptId: String): KemandoranModel? {
        return kemandoranDao.getBlokByDivisiAndDept(divisiId, deptId)
    }


    suspend fun getDatasetCount(datasetName: String, deptId: Int? = null): Int {
        return when (datasetName) {
            AppUtils.DatasetNames.pemanen -> karyawanDao.getCount()
            AppUtils.DatasetNames.kemandoran -> kemandoranDao.getCount()
            AppUtils.DatasetNames.tph -> {
                if (deptId != null) {
                    tphDao.getCountByDept(deptId)
                } else {
                    tphDao.getCount()
                }
            }
            AppUtils.DatasetNames.transporter -> transporterDao.getCount()
            AppUtils.DatasetNames.kendaraan -> kendaraanDao.getCount()
            AppUtils.DatasetNames.jenisTPH -> jenisTPHDao.getCount()
            else -> 0
        }
    }

    suspend fun updateOrInsertAfdeling(afdelings: List<AfdelingModel>) =
        afdelingDao.updateOrInsertAfdeling(afdelings)

    suspend fun getAllEstates(): Result<List<EstateModel>> = withContext(Dispatchers.IO) {
        try {
            val data = estateDao.getAllEstates()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    suspend fun getDeptByRegionalAndEstate(estateId: String): List<DeptModel> {
//        // Fetch dept data by regionalId and estateId
//        return deptDao.getDeptByCriteria(estateId)
//    }


    suspend fun getDivisiList(idEstate: Int): List<TPHNewModel> {
        return tphDao.getDivisiByCriteria(idEstate)
    }

    suspend fun getDistinctDeptInfo(): Result<List<DepartmentInfo>> = withContext(Dispatchers.IO) {
        try {
            val data = tphDao.getDistinctDeptInfo()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun getKemandoranEstateExcept(idEstate: Int, idDivisiArray: List<Int>): List<KemandoranModel> {
        return kemandoranDao.getKemandoranEstateExcept(idEstate, idDivisiArray)
    }

    suspend fun getListAfdeling(idEstate: String): List<AfdelingModel> {
        return afdelingDao.getListAfdelingFromIdEstate(idEstate)
    }

    suspend fun updateOrInsertEstate(estate: List<EstateModel>) =
        estateDao.updateOrInsertEstate(estate)

    suspend fun getAllTransporter(): List<TransporterModel> {
        return transporterDao.getAllTransporter()
    }

    suspend fun getAllNopol(): List<KendaraanModel> {
        return kendaraanDao.getAll()
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

    suspend fun downloadListEstate(regional: Int): Response<ResponseBody> {
        return apiService.downloadListEstate(mapOf("regional" to regional))
    }


    suspend fun checkStatusUploadCMP(trackingId: String): Response<checkStatusUploadedData> {
        return apiService.checkStatusUploadCMP(trackingId)
    }

    suspend fun downloadSettingJson(lastModified: String): Response<ResponseBody> {
        val requestBody = mapOf("last_modified" to lastModified)
        return apiService.downloadSettingJson(requestBody)
    }

    suspend fun getTPHsByIds(tphIds: List<Int>): List<TPHNewModel> {
        return tphDao.getTPHsByIds(tphIds)
    }
}
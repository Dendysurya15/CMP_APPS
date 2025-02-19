package com.cbi.cmp_project.data.repository

import android.content.Context
import androidx.room.Query
import com.cbi.cmp_project.data.api.ApiService
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.database.TPHDao
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.model.TransporterModel
import com.cbi.cmp_project.data.model.dataset.DatasetRequest
import com.cbi.cmp_project.data.network.CMPApiClient
import com.cbi.markertph.data.model.TPHNewModel
import okhttp3.ResponseBody
import retrofit2.Response

class DatasetRepository(context: Context,  private val apiService: ApiService = CMPApiClient.instance) {

    private val database = AppDatabase.getDatabase(context)
//    private val regionalDao = database.regionalDao()
//    private val wilayahDao = database.wilayahDao()
//    private val deptDao = database.deptDao()
//    private val divisiDao = database.divisiDao()
//    private val blokDao = database.blokDao()
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()
//    private val kemandoranDetailDao = database.kemandoranDetailDao()
    private val tphDao = database.tphDao()
    private val millDao = database.millDao()
    private val transporterDao = database.transporterDao()

    suspend fun updateOrInsertKaryawan(karyawans: List<KaryawanModel>) = karyawanDao.updateOrInsertKaryawan(karyawans)
    suspend fun updateOrInsertMill(mills: List<MillModel>) = millDao.updateOrInsertMill(mills)
    suspend fun InsertTransporter(transporter: List<TransporterModel>) = transporterDao.InsertTransporter(transporter)
    suspend fun updateOrInsertKemandoran(kemandorans: List<KemandoranModel>) = kemandoranDao.updateOrInsertKemandoran(kemandorans)

    suspend fun updateOrInsertTPH(tph: List<TPHNewModel>) = tphDao.updateOrInsertTPH(tph)

//    suspend fun getDeptByRegionalAndEstate(estateId: String): List<DeptModel> {
//        // Fetch dept data by regionalId and estateId
//        return deptDao.getDeptByCriteria(estateId)
//    }

    suspend fun getDivisiList( idEstate: Int): List<TPHNewModel> {
        return tphDao.getDivisiByCriteria(idEstate)
    }

    suspend fun getBlokList( idEstate: Int, idDivisi:Int): List<TPHNewModel> {
        return tphDao.getBlokByCriteria(idEstate, idDivisi)
    }

    suspend fun getKemandoranList(idEstate: Int, idDivisiArray: List<Int>): List<KemandoranModel> {
        return kemandoranDao.getKemandoranByCriteria(idEstate, idDivisiArray)
    }

    suspend fun getTPHList(idEstate: Int, idDivisi:Int, tahunTanam : String,  idBlok :Int): List<TPHNewModel> {
        return tphDao.getTPHByCriteria(idEstate, idDivisi, tahunTanam, idBlok)
    }
//
//    suspend fun getKemandoranDetailList(idHeader:Int): List<KemandoranDetailModel> {
//        return kemandoranDetailDao.getKemandoranDetailListByCriteria(idHeader)
//    }

    suspend fun getKaryawanList(filteredId:Int): List<KaryawanModel> {
        return karyawanDao.getKaryawanByCriteria(filteredId)
    }

    suspend fun downloadDataset(request: DatasetRequest): Response<ResponseBody> {
        return apiService.downloadDataset(request)
    }
    // In Repository
    suspend fun downloadSmallDataset(regional: Int): Response<ResponseBody> {
        return apiService.downloadSmallDataset(mapOf("regional" to regional))
    }
}
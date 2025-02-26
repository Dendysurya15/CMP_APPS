package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.model.PanenEntityWithRelations
import com.cbi.cmp_project.data.model.TransporterModel
import com.cbi.markertph.data.model.TPHNewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeighBridgeRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val millDao = database.millDao()
    private val transporterDao = database.transporterDao()
    private val tphDao = database.tphDao()
    private val karyawanDao = database.karyawanDao()
    private val espbDao = database.espbDao()

    suspend fun getMill( millId: Int): List<MillModel> {
        return millDao.getMillById(millId)
    }

    suspend fun getTransporter( transporterId: Int): List<TransporterModel> {
        return transporterDao.getTransporterById(transporterId)
    }

    suspend fun getBlokById( listBlokId: List<Int>): List<TPHNewModel> {
        return tphDao.getBlokById(listBlokId)
    }

    suspend fun getPemuatByIdList( idPemuat:List<String>): List<KaryawanModel> {
        return karyawanDao.getPemuatByIdList(idPemuat)
    }

    suspend fun coundESPBUploaded(): Int {
        return espbDao.countESPBUploaded()
    }

    suspend fun loadHistoryUploadeSPB(): Result<List<ESPBEntity>> = withContext(Dispatchers.IO) {
        try {
            val data = espbDao.getAllESPBUploaded()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDataESPB(
        blok_jjg: String,
        created_by_id: Int,
        created_at: String,
        nopol: String,
        driver: String,
        transporter_id: Int,
        pemuat_id: String,
        mill_id: Int,
        archive: Int,
        tph0: String,
        tph1: String,
        update_info: String,
        uploaded_by_id:Int?,
        uploaded_at: String,
        status_upload_cmp: Int,
        status_upload_ppro: Int,
        creator_info: String,
        uploader_info: String,
        noESPB: String,
    ): Result<Long> {
        val panenEntity = ESPBEntity(
            blok_jjg= blok_jjg,
            created_by_id = created_by_id,
            created_at = created_at,
            nopol  = nopol,
            driver = driver,
            transporter_id = transporter_id,
            pemuat_id = pemuat_id,
            mill_id = mill_id,
            archive = archive,
            tph0 = tph0,
            tph1 = tph1,
            update_info = update_info,
            uploaded_by_id = uploaded_by_id!!,
            uploaded_at = uploaded_at,
            status_upload_cmp = status_upload_cmp,
            status_upload_ppro = status_upload_ppro,
            creator_info = creator_info,
            uploader_info = uploader_info,
            noESPB = noESPB,
            scan_status = 1
        )
        return espbDao.insertWithTransaction(panenEntity)
    }

}

// Fetch TPH by ID


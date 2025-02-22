package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.MillModel
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


}

// Fetch TPH by ID


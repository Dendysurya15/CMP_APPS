package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.database.MillDao
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.cmp_project.data.model.TransporterModel
import com.cbi.markertph.data.model.TPHNewModel

class WeightBridgeRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val millDao = database.millDao()
    private val transporterDao = database.transporterDao()
    private val tphDao = database.tphDao()

    suspend fun getMill( millId: Int): List<MillModel> {
        return millDao.getMillById(millId)
    }

    suspend fun getTransporter( transporterId: Int): List<TransporterModel> {
        return transporterDao.getTransporterById(transporterId)
    }

    suspend fun getBlokById( listBlokId: List<Int>): List<TPHNewModel> {
        return tphDao.getBlokById(listBlokId)
    }


}

// Fetch TPH by ID


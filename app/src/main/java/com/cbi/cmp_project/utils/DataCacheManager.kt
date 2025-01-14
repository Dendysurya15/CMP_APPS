package com.cbi.cmp_project.utils

import android.content.Context
import android.provider.Settings.Global.putString
import com.cbi.cmp_project.data.model.BUnitCodeModel
import com.cbi.cmp_project.data.model.CompanyCodeModel
import com.cbi.cmp_project.data.model.DivisionCodeModel
import com.cbi.cmp_project.data.model.FieldCodeModel
import com.cbi.cmp_project.data.model.TPHModel
import com.cbi.cmp_project.data.model.WorkerGroupModel
import com.cbi.cmp_project.data.model.WorkerInGroupModel
import com.cbi.cmp_project.data.model.WorkerModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataCacheManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("data_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDatasets(
        companyCodeList: List<CompanyCodeModel>,
        bUnitCodeList: List<BUnitCodeModel>,
        divisionCodeList: List<DivisionCodeModel>,
        fieldCodeList: List<FieldCodeModel>,
        workerList: List<WorkerModel>,
        workerGroupList: List<WorkerGroupModel>,
        workerInGroupList: List<WorkerInGroupModel>,
        tphList: List<TPHModel>
    ) {
        prefs.edit().apply {
            putString("company_codes", gson.toJson(companyCodeList))
            putString("bunit_codes", gson.toJson(bUnitCodeList))
            putString("division_codes", gson.toJson(divisionCodeList))
            putString("field_codes", gson.toJson(fieldCodeList))
            putString("workers", gson.toJson(workerList))
            putString("worker_groups", gson.toJson(workerGroupList))
            putString("worker_in_groups", gson.toJson(workerInGroupList))
            putString("tph_list", gson.toJson(tphList))
            putLong("last_update", System.currentTimeMillis())
            apply()
        }
    }

    fun getDatasets(): DataSets? {
        // Check if we have cached data
        val companyCodesJson = prefs.getString("company_codes", null) ?: return null

        return try {
            DataSets(
                companyCodeList = gson.fromJson(companyCodesJson, object : TypeToken<List<CompanyCodeModel>>() {}.type),
                bUnitCodeList = gson.fromJson(prefs.getString("bunit_codes", "[]"), object : TypeToken<List<BUnitCodeModel>>() {}.type),
                divisionCodeList = gson.fromJson(prefs.getString("division_codes", "[]"), object : TypeToken<List<DivisionCodeModel>>() {}.type),
                fieldCodeList = gson.fromJson(prefs.getString("field_codes", "[]"), object : TypeToken<List<FieldCodeModel>>() {}.type),
                workerList = gson.fromJson(prefs.getString("workers", "[]"), object : TypeToken<List<WorkerModel>>() {}.type),
                workerGroupList = gson.fromJson(prefs.getString("worker_groups", "[]"), object : TypeToken<List<WorkerGroupModel>>() {}.type),
                workerInGroupList = gson.fromJson(prefs.getString("worker_in_groups", "[]"), object : TypeToken<List<WorkerInGroupModel>>() {}.type),
                tphList = gson.fromJson(prefs.getString("tph_list", "[]"), object : TypeToken<List<TPHModel>>() {}.type)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun needsRefresh(): Boolean {
        val lastUpdate = prefs.getLong("last_update", 0)
        val oneDayInMillis = 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - lastUpdate > oneDayInMillis
    }
}

data class DataSets(
    val companyCodeList: List<CompanyCodeModel>,
    val bUnitCodeList: List<BUnitCodeModel>,
    val divisionCodeList: List<DivisionCodeModel>,
    val fieldCodeList: List<FieldCodeModel>,
    val workerList: List<WorkerModel>,
    val workerGroupList: List<WorkerGroupModel>,
    val workerInGroupList: List<WorkerInGroupModel>,
    val tphList: List<TPHModel>
)
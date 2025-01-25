package com.cbi.cmp_project.utils

import android.content.Context
import android.provider.Settings.Global.putString
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataCacheManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("data_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDatasets(
        regionalList: List<RegionalModel>,
        wilayahList: List<WilayahModel>,
        deptList: List<DeptModel>,
        divisiList: List<DivisiModel>,
        blokList: List<BlokModel>,
        tphList: List<TPHNewModel>,
        karyawanList: List<KaryawanModel>,
        kemandoranList: List<KemandoranModel>,
        kemandoranDetailList: List<KemandoranDetailModel>
    ) {
        prefs.edit().apply {
            putString("RegionalDB", gson.toJson(regionalList))
            putString("WilayahDB", gson.toJson(wilayahList))
            putString("DeptDB", gson.toJson(deptList))
            putString("DivisiDB", gson.toJson(divisiList))
            putString("BlokDB", gson.toJson(blokList))
            putString("TPHDB", gson.toJson(tphList))
            putString("KaryawanDB", gson.toJson(karyawanList))
            putString("KemandoranDB", gson.toJson(kemandoranList))
            putString("KemandoranDetailDB", gson.toJson(kemandoranDetailList))
            putLong("last_update", System.currentTimeMillis())
            apply()
        }
    }

    fun getDatasets(): DataSets? {
        val companyCodesJson = prefs.getString("RegionalDB", null) ?: return null

        return try {
            DataSets(
                regionalList = gson.fromJson(companyCodesJson, object : TypeToken<List<RegionalModel>>() {}.type),
                wilayahList = gson.fromJson(prefs.getString("WilayahDB", "[]"), object : TypeToken<List<WilayahModel>>() {}.type),
                deptList = gson.fromJson(prefs.getString("DeptDB", "[]"), object : TypeToken<List<DeptModel>>() {}.type),
                divisiList = gson.fromJson(prefs.getString("DivisiDB", "[]"), object : TypeToken<List<DivisiModel>>() {}.type),
                blokList = gson.fromJson(prefs.getString("BlokDB", "[]"), object : TypeToken<List<BlokModel>>() {}.type),
                tphList = gson.fromJson(prefs.getString("TPHDB", "[]"), object : TypeToken<List<TPHNewModel>>() {}.type),
                karyawanList = gson.fromJson(prefs.getString("KaryawanDB", "[]"), object : TypeToken<List<KaryawanModel>>() {}.type),
                kemandoranList = gson.fromJson(prefs.getString("KemandoranDB", "[]"), object : TypeToken<List<KemandoranModel>>() {}.type),
                kemandoranDetailList = gson.fromJson(prefs.getString("KemandoranDetailDB", "[]"), object : TypeToken<List<KemandoranDetailModel>>() {}.type),
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
    val regionalList: List<RegionalModel>,
    val wilayahList: List<WilayahModel>,
    val deptList: List<DeptModel>,
    val divisiList: List<DivisiModel>,
    val blokList: List<BlokModel>,
    val tphList: List<TPHNewModel>,
    val karyawanList: List<KaryawanModel>,
    val kemandoranList: List<KemandoranModel>,
    val kemandoranDetailList: List<KemandoranDetailModel>,

)
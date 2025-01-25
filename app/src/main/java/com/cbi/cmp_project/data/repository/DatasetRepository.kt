package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.database.DatabaseHelper
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.WilayahModel

class DatasetRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)

    private val regionalDao = database.regionalDao()
    private val wilayahDao = database.wilayahDao()
    private val deptDao = database.deptDao()
    private val divisiDao = database.divisiDao()
    private val blokDao = database.blokDao()
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()
    private val kemandoranDetailDao = database.kemandoranDetailDao()

    suspend fun updateOrInsertRegional(regionals: List<RegionalModel>) {
        regionalDao.updateOrInsertRegional(regionals)
    }

    suspend fun updateOrInsertWilayah(wilayah: List<WilayahModel>) {
        wilayahDao.updateOrInsertWilayah(wilayah)
    }

    suspend fun updateOrInsertDept(depts: List<DeptModel>) {
        deptDao.updateOrInsertDept(depts)
    }

    suspend fun updateOrInsertDivisi(divisions: List<DivisiModel>) {
        divisiDao.updateOrInsertDivisi(divisions)
    }

    suspend fun updateOrInsertBlok(bloks: List<BlokModel>) {
        blokDao.updateOrInsertBlok(bloks)
    }

    suspend fun updateOrInsertKaryawan(karyawans: List<KaryawanModel>) {
        karyawanDao.updateOrInsertKaryawan(karyawans)
    }

    suspend fun updateOrInsertKemandoran(kemandorans: List<KemandoranModel>) {
        kemandoranDao.updateOrInsertKemandoran(kemandorans)
    }

    suspend fun updateOrInsertKemandoranDetail(kemandoran_detail: List<KemandoranDetailModel>) {
        kemandoranDetailDao.updateOrInsertKemandoranDetail(kemandoran_detail)
    }
}
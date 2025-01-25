package com.cbi.cmp_project.data.repository

import com.cbi.cmp_project.data.database.PanenDao
import com.cbi.cmp_project.data.model.PanenEntity

class PanenRepository(private val panenDao: PanenDao) {

    suspend fun insert(panen: PanenEntity) {
        panenDao.insert(panen)
    }

    suspend fun update(panen: PanenEntity) {
        panenDao.update(panen)
    }

    suspend fun delete(panen: PanenEntity) {
        panenDao.delete(panen)
    }

    suspend fun getById(id: Int): PanenEntity? {
        return panenDao.getById(id)
    }

    suspend fun getAll(): List<PanenEntity> {
        return panenDao.getAll()
    }
}
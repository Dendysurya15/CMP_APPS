package com.cbi.cmp_project.data.repository

import com.cbi.cmp_project.data.database.EspbDao
import com.cbi.cmp_project.data.model.ESPBEntity

class EspbRepository(private val dao: EspbDao) {
    suspend fun insert(user: ESPBEntity) = dao.insert(user)
    suspend fun update(user: ESPBEntity) = dao.update(user)
    suspend fun delete(user: ESPBEntity) = dao.delete(user)
    suspend fun getAllEntries() = dao.getAllEntries()
    suspend fun getEntryById(id: Int) = dao.getEntryById(id)
    suspend fun deleteById(id: Int) = dao.deleteById(id)
}

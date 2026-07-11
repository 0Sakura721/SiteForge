package com.siteforge.app.data

import com.siteforge.app.data.model.Site
import kotlinx.coroutines.flow.Flow

class SiteRepository(private val dao: SiteDao) {

    val allSites: Flow<List<Site>> = dao.getAllSites()

    suspend fun getSiteById(id: Long): Site? = dao.getSiteById(id)

    suspend fun getSiteByName(name: String): Site? = dao.getSiteByName(name)

    suspend fun getRunningCount(): Int = dao.getRunningCount()

    suspend fun insert(site: Site): Long = dao.insert(site)

    suspend fun update(site: Site) = dao.update(site)

    suspend fun updateStatus(id: Long, status: String) = dao.updateStatus(id, status)

    suspend fun delete(site: Site) = dao.delete(site)

    suspend fun isNameTaken(name: String): Boolean = dao.getSiteByName(name) != null
}

package com.siteforge.app.data

import androidx.room.*
import com.siteforge.app.data.model.Site
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Query("SELECT * FROM sites ORDER BY id DESC")
    fun getAllSites(): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getSiteById(id: Long): Site?

    @Query("SELECT * FROM sites WHERE name = :name LIMIT 1")
    suspend fun getSiteByName(name: String): Site?

    @Query("SELECT COUNT(*) FROM sites WHERE status = 'running'")
    suspend fun getRunningCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(site: Site): Long

    @Update
    suspend fun update(site: Site)

    @Query("UPDATE sites SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(site: Site)
}

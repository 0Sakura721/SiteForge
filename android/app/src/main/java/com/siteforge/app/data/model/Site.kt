package com.siteforge.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String = "static",      // static, single-page
    val template: String = "blank",
    val port: Int = 0,
    val status: String = "stopped",   // stopped, running
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

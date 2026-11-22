package com.dukaanai.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storeId: Long = 1L,
    val name: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

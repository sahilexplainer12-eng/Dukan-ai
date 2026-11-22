package com.dukaanai.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    CREDIT,  // Customer owes shop (Udhaar)
    PAYMENT  // Customer paid shop (Jama)
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["createdAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storeId: Long = 1L,
    val customerId: Long,
    val type: TransactionType,
    val amount: Double,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

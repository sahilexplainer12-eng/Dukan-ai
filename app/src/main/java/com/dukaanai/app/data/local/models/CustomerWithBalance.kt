package com.dukaanai.app.data.local.models

import com.dukaanai.app.data.local.entities.Customer
import com.dukaanai.app.data.local.entities.TransactionEntity

data class CustomerWithBalance(
    val customer: Customer,
    val totalCredit: Double,
    val totalPayment: Double
) {
    /**
     * Outstanding balance:
     * > 0 : Customer owes money to the shop
     * < 0 : Customer has advance payment / shop credit
     * == 0 : Account settled
     */
    val balance: Double get() = totalCredit - totalPayment
}

data class TransactionWithCustomer(
    val transaction: TransactionEntity,
    val customerName: String,
    val customerPhone: String = ""
)

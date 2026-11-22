package com.dukaanai.app.data.repository

import com.dukaanai.app.data.local.dao.CustomerDao
import com.dukaanai.app.data.local.dao.StoreDao
import com.dukaanai.app.data.local.dao.TransactionDao
import com.dukaanai.app.data.local.entities.Customer
import com.dukaanai.app.data.local.entities.StoreProfile
import com.dukaanai.app.data.local.entities.TransactionEntity
import com.dukaanai.app.data.local.entities.TransactionType
import com.dukaanai.app.data.local.models.CustomerWithBalance
import com.dukaanai.app.data.local.models.TransactionWithCustomer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

data class TodaySummary(
    val todayCredit: Double = 0.0,
    val todayPayment: Double = 0.0,
    val customerCount: Int = 0
)

class LedgerRepository(
    private val storeDao: StoreDao,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {
    // 1. Store Profile Flow
    val storeProfile: Flow<StoreProfile?> = storeDao.getStoreProfile()

    suspend fun saveStoreProfile(store: StoreProfile) {
        storeDao.insertOrUpdateStore(store)
    }

    // 2. Customers with calculated balances
    val customersWithBalance: Flow<List<CustomerWithBalance>> =
        combine(customerDao.getAllCustomers(), transactionDao.getAllTransactions()) { customers, transactions ->
            val transactionsByCustomer = transactions.groupBy { it.customerId }
            customers.map { customer ->
                val customerTxns = transactionsByCustomer[customer.id] ?: emptyList()
                val totalCredit = customerTxns
                    .filter { it.type == TransactionType.CREDIT }
                    .sumOf { it.amount }
                val totalPayment = customerTxns
                    .filter { it.type == TransactionType.PAYMENT }
                    .sumOf { it.amount }
                CustomerWithBalance(
                    customer = customer,
                    totalCredit = totalCredit,
                    totalPayment = totalPayment
                )
            }
        }

    // 3. Transactions for a specific customer
    fun getCustomerTransactions(customerId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForCustomer(customerId)
    }

    fun getCustomerById(customerId: Long): Flow<Customer?> {
        return customerDao.getCustomerById(customerId)
    }

    // 4. All transactions combined with customer names
    val allTransactionsWithCustomer: Flow<List<TransactionWithCustomer>> =
        combine(customerDao.getAllCustomers(), transactionDao.getAllTransactions()) { customers, transactions ->
            val customerMap = customers.associateBy { it.id }
            transactions.map { txn ->
                val cust = customerMap[txn.customerId]
                TransactionWithCustomer(
                    transaction = txn,
                    customerName = cust?.name ?: "Unknown Customer",
                    customerPhone = cust?.phone ?: ""
                )
            }
        }

    // 5. Today's Dashboard summary
    val todaySummary: Flow<TodaySummary> =
        combine(customerDao.getAllCustomers(), transactionDao.getAllTransactions()) { customers, transactions ->
            val startOfDay = getStartOfDayMillis()
            val todayTxns = transactions.filter { it.createdAt >= startOfDay }
            val creditSum = todayTxns
                .filter { it.type == TransactionType.CREDIT }
                .sumOf { it.amount }
            val paymentSum = todayTxns
                .filter { it.type == TransactionType.PAYMENT }
                .sumOf { it.amount }

            TodaySummary(
                todayCredit = creditSum,
                todayPayment = paymentSum,
                customerCount = customers.size
            )
        }

    // Customer operations
    suspend fun addCustomer(name: String, phone: String): Long {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Customer name cannot be empty." }
        val customer = Customer(name = cleanName, phone = phone.trim())
        return customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(id: Long, name: String, phone: String) {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Customer name cannot be empty." }
        val existing = customerDao.getCustomerByIdSync(id) ?: return
        customerDao.updateCustomer(existing.copy(name = cleanName, phone = phone.trim()))
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
    }

    // Transaction operations
    suspend fun addTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        description: String = "",
        customTimestamp: Long = System.currentTimeMillis()
    ): Long {
        require(amount > 0.0) { "Transaction amount must be greater than zero." }
        val txn = TransactionEntity(
            customerId = customerId,
            type = type,
            amount = amount,
            description = description.trim(),
            createdAt = customTimestamp
        )
        return transactionDao.insertTransaction(txn)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}

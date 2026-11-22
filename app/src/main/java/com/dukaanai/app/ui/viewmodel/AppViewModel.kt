package com.dukaanai.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dukaanai.app.data.local.AppDatabase
import com.dukaanai.app.data.local.entities.Customer
import com.dukaanai.app.data.local.entities.StoreProfile
import com.dukaanai.app.data.local.entities.TransactionEntity
import com.dukaanai.app.data.local.entities.TransactionType
import com.dukaanai.app.data.local.models.CustomerWithBalance
import com.dukaanai.app.data.local.models.TransactionWithCustomer
import com.dukaanai.app.data.repository.LedgerRepository
import com.dukaanai.app.data.repository.TodaySummary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Loading : AppScreen
    data object Onboarding : AppScreen
    data object Home : AppScreen
    data object Khata : AppScreen
    data object History : AppScreen
    data class CustomerDetail(val customerId: Long) : AppScreen
    data object StoreSettings : AppScreen
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = LedgerRepository(
        storeDao = database.storeDao(),
        customerDao = database.customerDao(),
        transactionDao = database.transactionDao()
    )

    // Current navigation destination
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Loading)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Store Profile
    val storeProfile: StateFlow<StoreProfile?> = repository.storeProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Today's summary for home dashboard
    val todaySummary: StateFlow<TodaySummary> = repository.todaySummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodaySummary())

    // All customers with calculated balances
    val customers: StateFlow<List<CustomerWithBalance>> = repository.customersWithBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query for Khata screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredCustomers: StateFlow<List<CustomerWithBalance>> =
        combine(customers, _searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                val clean = query.trim().lowercase()
                list.filter {
                    it.customer.name.lowercase().contains(clean) ||
                    it.customer.phone.contains(clean)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All transactions across the store
    val allTransactions: StateFlow<List<TransactionWithCustomer>> = repository.allTransactionsWithCustomer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dialog & Interaction UI States
    var showAddCustomerDialog by androidx.compose.runtime.mutableStateOf(false)
    var editingCustomer by androidx.compose.runtime.mutableStateOf<Customer?>(null)
    var customerToDelete by androidx.compose.runtime.mutableStateOf<Customer?>(null)

    // Transaction dialog state: Pair of (Customer, TransactionType)
    var transactionDialogState by androidx.compose.runtime.mutableStateOf<Pair<Customer, TransactionType>?>(null)
    var transactionToDelete by androidx.compose.runtime.mutableStateOf<TransactionEntity?>(null)

    init {
        // Check if store profile exists to determine whether onboarding is required
        viewModelScope.launch {
            repository.storeProfile.collect { profile ->
                if (_currentScreen.value == AppScreen.Loading) {
                    if (profile == null) {
                        _currentScreen.value = AppScreen.Onboarding
                    } else {
                        _currentScreen.value = AppScreen.Home
                    }
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Store operations
    fun completeOnboarding(storeName: String, ownerName: String, phone: String, address: String) {
        val trimmed = storeName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.saveStoreProfile(
                StoreProfile(
                    storeName = trimmed,
                    ownerName = ownerName.trim(),
                    phoneNumber = phone.trim(),
                    address = address.trim()
                )
            )
            _currentScreen.value = AppScreen.Home
        }
    }

    fun updateStoreProfile(storeName: String, ownerName: String, phone: String, address: String) {
        val trimmed = storeName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.saveStoreProfile(
                StoreProfile(
                    id = 1L,
                    storeName = trimmed,
                    ownerName = ownerName.trim(),
                    phoneNumber = phone.trim(),
                    address = address.trim()
                )
            )
        }
    }

    // Customer operations
    fun addCustomer(name: String, phone: String, onComplete: () -> Unit = {}) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            repository.addCustomer(cleanName, phone.trim())
            showAddCustomerDialog = false
            onComplete()
        }
    }

    fun updateCustomer(id: Long, name: String, phone: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            repository.updateCustomer(id, cleanName, phone.trim())
            editingCustomer = null
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            customerToDelete = null
            // If currently viewing deleted customer details, navigate back to Khata
            val current = _currentScreen.value
            if (current is AppScreen.CustomerDetail && current.customerId == customer.id) {
                _currentScreen.value = AppScreen.Khata
            }
        }
    }

    // Transaction operations
    fun addTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        description: String
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.addTransaction(
                customerId = customerId,
                type = type,
                amount = amount,
                description = description
            )
            transactionDialogState = null
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            transactionToDelete = null
        }
    }

    fun getCustomerTransactions(customerId: Long): Flow<List<TransactionEntity>> {
        return repository.getCustomerTransactions(customerId)
    }

    fun getCustomer(customerId: Long): Flow<Customer?> {
        return repository.getCustomerById(customerId)
    }
}

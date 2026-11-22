package com.dukaanai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukaanai.app.data.local.entities.TransactionEntity
import com.dukaanai.app.data.local.entities.TransactionType
import com.dukaanai.app.data.local.models.TransactionWithCustomer
import com.dukaanai.app.ui.components.AddCustomerDialog
import com.dukaanai.app.ui.components.AddTransactionDialog
import com.dukaanai.app.ui.components.DeleteConfirmationDialog
import com.dukaanai.app.ui.components.EditCustomerDialog
import com.dukaanai.app.ui.components.StoreSettingsDialog
import com.dukaanai.app.ui.theme.*
import com.dukaanai.app.ui.viewmodel.AppScreen
import com.dukaanai.app.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val todaySummary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val customers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showStoreSettingsDialog by remember { mutableStateOf(false) }

    // First-Time Launch Onboarding Screen
    if (currentScreen is AppScreen.Onboarding) {
        OnboardingScreen(
            onSetupComplete = { storeName, ownerName, phone, address ->
                viewModel.completeOnboarding(storeName, ownerName, phone, address)
            }
        )
        return
    }

    // Customer Detail Screen View
    if (currentScreen is AppScreen.CustomerDetail) {
        val customerId = (currentScreen as AppScreen.CustomerDetail).customerId
        val customer by viewModel.getCustomer(customerId).collectAsStateWithLifecycle(initialValue = null)
        val customerTransactions by viewModel.getCustomerTransactions(customerId).collectAsStateWithLifecycle(initialValue = emptyList())

        if (customer != null) {
            CustomerDetailScreen(
                customer = customer!!,
                transactions = customerTransactions,
                onBack = { viewModel.navigateTo(AppScreen.Khata) },
                onEditCustomer = { viewModel.editingCustomer = it },
                onDeleteCustomer = { viewModel.customerToDelete = it },
                onAddCredit = { viewModel.transactionDialogState = it to TransactionType.CREDIT },
                onAddPayment = { viewModel.transactionDialogState = it to TransactionType.PAYMENT },
                onDeleteTransaction = { viewModel.transactionToDelete = it }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        }

        // Render Dialogs
        RenderDialogs(viewModel = viewModel, storeProfile = storeProfile, showSettings = showStoreSettingsDialog, onDismissSettings = { showStoreSettingsDialog = false })
        return
    }

    // Bottom Bar Tab selection
    val selectedTab = when (currentScreen) {
        AppScreen.Home -> 0
        AppScreen.Khata -> 1
        AppScreen.History -> 2
        else -> 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = "Store Logo",
                                tint = SurfaceCard,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = storeProfile?.storeName ?: "Dukaan AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (!storeProfile?.ownerName.isNullOrBlank()) "${storeProfile?.ownerName} • Ledger" else "Store Ledger",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showStoreSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Store Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceCard)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 0.dp,
                modifier = Modifier.border(width = 1.dp, color = BorderSubtle)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        indicatorColor = BorderDivider,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.navigateTo(AppScreen.Khata) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Khata") },
                    label = { Text("Khata", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        indicatorColor = BorderDivider,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.navigateTo(AppScreen.History) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        indicatorColor = BorderDivider,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundScreen)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeDashboardView(
                    todayCredit = "₹%.2f".format(todaySummary.todayCredit),
                    todayPayment = "₹%.2f".format(todaySummary.todayPayment),
                    customerCount = todaySummary.customerCount,
                    totalRecordCount = allTransactions.size,
                    recentTransactions = allTransactions.take(5),
                    onOpenKhata = { viewModel.navigateTo(AppScreen.Khata) },
                    onOpenHistory = { viewModel.navigateTo(AppScreen.History) },
                    onStartShopMode = {
                        // Reserved for upcoming Shop Mode feature
                    },
                    onDeleteTransaction = { viewModel.transactionToDelete = it }
                )
                1 -> KhataScreen(
                    customers = customers,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onAddCustomerClick = { viewModel.showAddCustomerDialog = true },
                    onCustomerClick = { customerId -> viewModel.navigateTo(AppScreen.CustomerDetail(customerId)) }
                )
                2 -> HistoryScreen(
                    transactions = allTransactions,
                    onDeleteTransaction = { viewModel.transactionToDelete = it }
                )
            }
        }
    }

    // Render Dialogs
    RenderDialogs(viewModel = viewModel, storeProfile = storeProfile, showSettings = showStoreSettingsDialog, onDismissSettings = { showStoreSettingsDialog = false })
}

@Composable
fun RenderDialogs(
    viewModel: AppViewModel,
    storeProfile: com.dukaanai.app.data.local.entities.StoreProfile?,
    showSettings: Boolean,
    onDismissSettings: () -> Unit
) {
    // 1. Add Customer Dialog
    if (viewModel.showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { viewModel.showAddCustomerDialog = false },
            onConfirm = { name, phone -> viewModel.addCustomer(name, phone) }
        )
    }

    // 2. Edit Customer Dialog
    viewModel.editingCustomer?.let { customer ->
        EditCustomerDialog(
            customer = customer,
            onDismiss = { viewModel.editingCustomer = null },
            onConfirm = { name, phone -> viewModel.updateCustomer(customer.id, name, phone) }
        )
    }

    // 3. Delete Customer Dialog
    viewModel.customerToDelete?.let { customer ->
        DeleteConfirmationDialog(
            title = "Delete Customer",
            message = "Are you sure you want to delete ${customer.name}? All their transactions will be permanently deleted.",
            onDismiss = { viewModel.customerToDelete = null },
            onConfirm = { viewModel.deleteCustomer(customer) }
        )
    }

    // 4. Add Transaction Dialog (Credit or Payment)
    viewModel.transactionDialogState?.let { (customer, type) ->
        AddTransactionDialog(
            customer = customer,
            type = type,
            onDismiss = { viewModel.transactionDialogState = null },
            onConfirm = { amount, description ->
                viewModel.addTransaction(
                    customerId = customer.id,
                    type = type,
                    amount = amount,
                    description = description
                )
            }
        )
    }

    // 5. Delete Transaction Dialog
    viewModel.transactionToDelete?.let { txn ->
        DeleteConfirmationDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete this ₹%.2f entry? The customer's balance will be recalculated immediately.".format(txn.amount),
            onDismiss = { viewModel.transactionToDelete = null },
            onConfirm = { viewModel.deleteTransaction(txn) }
        )
    }

    // 6. Store Settings Dialog
    if (showSettings) {
        StoreSettingsDialog(
            storeProfile = storeProfile,
            onDismiss = onDismissSettings,
            onConfirm = { name, owner, phone, addr ->
                viewModel.updateStoreProfile(name, owner, phone, addr)
                onDismissSettings()
            }
        )
    }
}

/**
 * Main Home Dashboard: Real Live Ledger Totals & Recent Transactions
 */
@Composable
fun HomeDashboardView(
    todayCredit: String,
    todayPayment: String,
    customerCount: Int,
    totalRecordCount: Int,
    recentTransactions: List<TransactionWithCustomer>,
    onOpenKhata: () -> Unit,
    onOpenHistory: () -> Unit,
    onStartShopMode: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(2.dp)) }

        // 1. Primary Action: Start Shop Mode
        item {
            StartShopModeButton(onStartClick = onStartShopMode)
        }

        // 2. Real Today's Overview
        item {
            TodayOverviewSection(
                todayCredit = todayCredit,
                todayPayments = todayPayment
            )
        }

        // 3. Quick Action Shortcuts with Live Counts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutCard(
                    title = "Customer Accounts",
                    subtitle = "$customerCount Customer${if (customerCount != 1) "s" else ""}",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenKhata
                )
                ShortcutCard(
                    title = "Transaction History",
                    subtitle = "$totalRecordCount Record${if (totalRecordCount != 1) "s" else ""}",
                    icon = Icons.Default.ReceiptLong,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHistory
                )
            }
        }

        // 4. Recent Transactions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                if (recentTransactions.isNotEmpty()) {
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onOpenHistory() }
                    )
                }
            }
        }

        // 5. Recent Transactions List or Empty State
        if (recentTransactions.isEmpty()) {
            item {
                EmptyTransactionsCard()
            }
        } else {
            items(recentTransactions, key = { it.transaction.id }) { item ->
                GlobalTransactionRow(
                    item = item,
                    onDelete = { onDeleteTransaction(item.transaction) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * Primary CTA Button for Shop Mode
 */
@Composable
fun StartShopModeButton(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Start Shop Mode",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Record sales and ledger entries",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Clean dual-card Today's Overview
 */
@Composable
fun TodayOverviewSection(
    todayCredit: String,
    todayPayments: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "Today's Overview",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Today's Credit (Given)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CreditRedBg)
                        .border(1.dp, CreditRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                tint = CreditRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Today's Credit",
                                color = CreditRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = todayCredit,
                            color = CreditRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Today's Payments (Received)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PaymentGreenBg)
                        .border(1.dp, PaymentGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                tint = PaymentGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Today's Payments",
                                color = PaymentGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = todayPayments,
                            color = PaymentGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimal shortcut card
 */
@Composable
fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandAccentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BrandAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

/**
 * Clean empty state for Recent Transactions
 */
@Composable
fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BorderDivider),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No transactions yet",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your transactions will appear here",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

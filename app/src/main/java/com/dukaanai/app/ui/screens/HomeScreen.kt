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
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dukaanai.app.ui.theme.*

// Simple data model for UI preview
data class TransactionItem(
    val id: String,
    val customerName: String,
    val details: String,
    val amount: String,
    val isUdhaar: Boolean, // true = Udhaar (credit given), false = Jama (money received)
    val timeAgo: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Sample data to make the UI look realistic for a Kirana store
    val sampleTransactions = remember {
        listOf(
            TransactionItem(
                id = "1",
                customerName = "Ramesh Kumar (रमेश)",
                details = "आटा 5kg, चीनी 1kg",
                amount = "₹260",
                isUdhaar = true,
                timeAgo = "10 min ago"
            ),
            TransactionItem(
                id = "2",
                customerName = "Sunil Bhaiya (सुनील)",
                details = "पिछला हिसाब चुकता (Cash)",
                amount = "₹500",
                isUdhaar = false,
                timeAgo = "45 min ago"
            ),
            TransactionItem(
                id = "3",
                customerName = "Pooja Sharma (शर्मा जी)",
                details = "सरसों तेल 1L, मैगी 4 pkt",
                amount = "₹220",
                isUdhaar = true,
                timeAgo = "1 hr ago"
            ),
            TransactionItem(
                id = "4",
                customerName = "Vikram Singh (विक्रम)",
                details = "दाल तुवर 2kg, नमक 1pkt",
                amount = "₹310",
                isUdhaar = true,
                timeAgo = "2 hrs ago"
            )
        )
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(KiranaGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Dukaan Icon",
                                tint = KiranaGreenPrimary
                            )
                        }
                        Column {
                            Text(
                                text = "Dukaan AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = KiranaGreenDark
                            )
                            Text(
                                text = "दुकानदार का डिजिटल मुनीम",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("होम (Home)") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KiranaGreenPrimary,
                        selectedTextColor = KiranaGreenPrimary,
                        indicatorColor = KiranaGreenLight
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Khata") },
                    label = { Text("खाता (Khata)") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KiranaGreenPrimary,
                        selectedTextColor = KiranaGreenPrimary,
                        indicatorColor = KiranaGreenLight
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("इतिहास (History)") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KiranaGreenPrimary,
                        selectedTextColor = KiranaGreenPrimary,
                        indicatorColor = KiranaGreenLight
                    )
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundOffWhite)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Prominent Start Shop Mode Button
            item {
                StartShopModeCard(
                    onStartClick = {
                        // Placeholder for Step 2+ (Voice / Shop mode interaction)
                    }
                )
            }

            // 2. Today's Udhaar & Jama Summary Card
            item {
                TodaySummaryCard(
                    todayUdhaar = "₹1,480",
                    todayJama = "₹1,100",
                    pendingCustomerCount = 6
                )
            }

            // 3. Navigation Shortcuts (Khata / History Quick Cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickNavButton(
                        title = "ग्राहक खाता (Khata)",
                        subtitle = "कुल 42 ग्राहक",
                        icon = Icons.Default.Book,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                    QuickNavButton(
                        title = "आज का हिसाब (History)",
                        subtitle = "लेन-देन देखें",
                        icon = Icons.Default.History,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 2 }
                    )
                }
            }

            // 4. Section Title: Recent Transactions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "हाल के लेन-देन (Recent Transactions)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "सभी देखें",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KiranaGreenPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { selectedTab = 2 }
                    )
                }
            }

            // 5. Transaction List Items
            items(sampleTransactions) { item ->
                TransactionCard(item = item)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Large prominent CTA card for activating "Shop Mode" (voice recording / live entry)
 */
@Composable
fun StartShopModeCard(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = KiranaGreenPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "दुकान मोड चालू करें",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Start Shop Mode • बोलकर हिसाब लिखें",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = KiranaGreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Summary card showing Today's Udhaar vs Jama
 */
@Composable
fun TodaySummaryCard(
    todayUdhaar: String,
    todayJama: String,
    pendingCustomerCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "आज का हिसाब-किताब (Today's Summary)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "$pendingCustomerCount ग्राहक बाकी",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's Udhaar (Credit given to customers)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(UdhaarRedBg)
                        .border(1.dp, UdhaarRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                tint = UdhaarRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "आज का उधार",
                                color = UdhaarRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = todayUdhaar,
                            color = UdhaarRed,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Udhaar (Given)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Today's Jama (Cash received from customers)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(JamaGreenBg)
                        .border(1.dp, JamaGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                tint = JamaGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "आज जमा हुआ",
                                color = JamaGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = todayJama,
                            color = JamaGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Jama (Received)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick navigation tile for Khata or History
 */
@Composable
fun QuickNavButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KiranaGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = KiranaGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Individual Transaction entry in the Recent Transactions list
 */
@Composable
fun TransactionCard(item: TransactionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (item.isUdhaar) UdhaarRedBg else JamaGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Customer",
                        tint = if (item.isUdhaar) UdhaarRed else JamaGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = item.timeAgo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.amount,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (item.isUdhaar) UdhaarRed else JamaGreen
                )
                Text(
                    text = if (item.isUdhaar) "उधार दिया" else "जमा हुआ",
                    fontSize = 11.sp,
                    color = if (item.isUdhaar) UdhaarRed else JamaGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    DukaanAITheme {
        HomeScreen()
    }
}

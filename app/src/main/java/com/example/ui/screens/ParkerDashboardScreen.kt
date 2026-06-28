package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.components.GlassCard
import com.example.ui.components.PremiumBackground
import com.example.viewmodel.ParkFlowViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkerDashboardScreen(
    viewModel: ParkFlowViewModel,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf("DASHBOARD") } // "DASHBOARD", "FIND", "BOOKINGS", "VEHICLES", "PROFILE", "PAYMENTS", "NOTIFICATIONS"
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser == null) {
        return
    }

    val user = currentUser!!
    val notifications by viewModel.userNotifications.collectAsState()
    val unreadNotificationsCount = notifications.count { !it.isRead }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == "DASHBOARD",
                    onClick = { activeTab = "DASHBOARD" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = activeTab == "FIND",
                    onClick = { activeTab = "FIND" },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Find Parking") },
                    label = { Text("Find") }
                )
                NavigationBarItem(
                    selected = activeTab == "BOOKINGS",
                    onClick = { activeTab = "BOOKINGS" },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Bookings") },
                    label = { Text("Bookings") }
                )
                NavigationBarItem(
                    selected = activeTab == "VEHICLES",
                    onClick = { activeTab = "VEHICLES" },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Vehicles") },
                    label = { Text("Vehicles") }
                )
                NavigationBarItem(
                    selected = activeTab == "PROFILE",
                    onClick = { activeTab = "PROFILE" },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        PremiumBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                // Top Custom App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${user.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Parker Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Wallet Button
                        IconButton(onClick = { activeTab = "PAYMENTS" }) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Notifications Badge
                        Box {
                            IconButton(onClick = { activeTab = "NOTIFICATIONS" }) {
                                Icon(
                                    imageVector = if (unreadNotificationsCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (unreadNotificationsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                            }
                            if (unreadNotificationsCount > 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(unreadNotificationsCount.toString())
                                }
                            }
                        }

                        // Logout Button
                        IconButton(onClick = {
                            viewModel.logout()
                            onLogout()
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Content switching based on active tab
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeTab) {
                        "DASHBOARD" -> ParkerHomeTab(viewModel, user) { activeTab = "FIND" }
                        "FIND" -> FindParkingTab(viewModel)
                        "BOOKINGS" -> ParkerBookingsTab(viewModel, user)
                        "VEHICLES" -> ParkerVehiclesTab(viewModel)
                        "PROFILE" -> ParkerProfileTab(viewModel, user)
                        "PAYMENTS" -> ParkerWalletTab(viewModel, user)
                        "NOTIFICATIONS" -> ParkerNotificationsTab(viewModel)
                    }
                }
            }
        }
    }
}

// ==================== HOME TAB ====================
@Composable
fun ParkerHomeTab(
    viewModel: ParkFlowViewModel,
    user: User,
    onNavigateToSearch: () -> Unit
) {
    val scrollState = rememberScrollState()
    val bookings by viewModel.userBookings.collectAsState()
    val spaces by viewModel.allActiveParkingSpaces.collectAsState()
    val favIds by viewModel.favoriteSpaceIds.collectAsState()

    val upcomingBooking = bookings.find { it.status == "CONFIRMED" || it.status == "ACTIVE" }
    val favoriteSpaces = spaces.filter { favIds.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Wallet Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Wallet Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Text(
                            text = "$${"%.2f".format(user.walletBalance)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    var showTopUpDialog by remember { mutableStateOf(false) }
                    var topUpAmount by remember { mutableStateOf("") }

                    Button(
                        onClick = { showTopUpDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Top Up")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Top Up")
                    }

                    if (showTopUpDialog) {
                        Dialog(onDismissRequest = { showTopUpDialog = false }) {
                            Card(
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("Add Funds to Wallet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    OutlinedTextField(
                                        value = topUpAmount,
                                        onValueChange = { topUpAmount = it },
                                        label = { Text("Amount ($)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        TextButton(onClick = { showTopUpDialog = false }, modifier = Modifier.weight(1f)) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = {
                                                val amt = topUpAmount.toDoubleOrNull() ?: 0.0
                                                if (amt > 0) {
                                                    viewModel.addWalletFunds(amt)
                                                }
                                                showTopUpDialog = false
                                                topUpAmount = ""
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Add")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Search Card
        Card(
            onClick = onNavigateToSearch,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Find Nearby Parking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Search by city, area, or zip code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
                Icon(Icons.Default.ArrowForward, contentDescription = "Search")
            }
        }

        // Upcoming / Active Reservation Summary
        if (upcomingBooking != null) {
            val spaceName = spaces.find { it.id == upcomingBooking.parkingSpaceId }?.name ?: "Parking Space"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE RESERVATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = upcomingBooking.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(spaceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Date: ${upcomingBooking.date} at ${upcomingBooking.time}", style = MaterialTheme.typography.bodySmall)
                    Text("Duration: ${upcomingBooking.durationHours} Hours", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    var showQrDialog by remember { mutableStateOf(false) }
                    Button(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Show QR")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Display Entry QR Code")
                    }

                    if (showQrDialog) {
                        BookingQrDialog(upcomingBooking, spaceName) { showQrDialog = false }
                    }
                }
            }
        }

        // Favorite Parking spaces
        if (favoriteSpaces.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Favorite Parking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                favoriteSpaces.forEach { space ->
                    ParkingItemCard(space, isFav = true, onToggleFav = { viewModel.toggleFavorite(space.id) }, onBook = { viewModel.selectSpace(space.id) })
                }
            }
        }

        // Recent Activity / Booking History
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Recent History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (bookings.isEmpty()) {
                Text(
                    "No recent bookings found. Start searching to park!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            } else {
                bookings.take(3).forEach { booking ->
                    val spaceName = spaces.find { it.id == booking.parkingSpaceId }?.name ?: "Parking Space"
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(spaceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${booking.date} • ${booking.time}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$${"%.2f".format(booking.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = booking.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (booking.status) {
                                        "COMPLETED" -> Color(0xFF10B981)
                                        "CANCELLED" -> Color(0xFFEF4444)
                                        "ACTIVE" -> Color(0xFF3B82F6)
                                        else -> Color(0xFFF59E0B)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== FIND PARKING TAB ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindParkingTab(viewModel: ParkFlowViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val filterCovered by viewModel.filterCoveredOnly.collectAsState()
    val filterCharging by viewModel.filterHasCharging.collectAsState()
    val filterSecurity by viewModel.filterHasSecurity.collectAsState()
    val filterType by viewModel.filterVehicleType.collectAsState()
    val sortPrice by viewModel.sortByPrice.collectAsState()

    val spaces by viewModel.allActiveParkingSpaces.collectAsState()
    val favIds by viewModel.favoriteSpaceIds.collectAsState()
    val selectedSpaceId by viewModel.selectedSpaceId.collectAsState()

    var showFiltersSheet by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(false) }

    // Apply filtering logic in-memory (in the client view layer)
    val filteredSpaces = spaces.filter { space ->
        val queryMatch = space.name.contains(query, ignoreCase = true) ||
                space.address.contains(query, ignoreCase = true)

        val coveredMatch = !filterCovered || space.isCovered
        val chargingMatch = !filterCharging || space.hasCharging
        val securityMatch = !filterSecurity || space.hasSecurity

        val typePriceMatch = when (filterType) {
            "BIKE" -> space.bikePrice > 0
            "CAR" -> space.carPrice > 0
            "SUV" -> space.suvPrice > 0
            "TRUCK" -> space.truckPrice > 0
            else -> true
        }

        queryMatch && coveredMatch && chargingMatch && securityMatch && typePriceMatch
    }.sortedWith(
        if (sortPrice) {
            compareBy {
                when (filterType) {
                    "BIKE" -> it.bikePrice
                    "CAR" -> it.carPrice
                    "SUV" -> it.suvPrice
                    "TRUCK" -> it.truckPrice
                    else -> it.carPrice
                }
            }
        } else {
            compareBy { it.id } // Default order
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by City, Area, PIN...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            IconButton(
                onClick = { showFiltersSheet = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (filterCovered || filterCharging || filterSecurity || filterType != "ANY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filters")
            }

            IconButton(
                onClick = { isMapView = !isMapView }
            ) {
                Icon(
                    imageVector = if (isMapView) Icons.Default.ViewList else Icons.Default.Map,
                    contentDescription = "Toggle Map/List"
                )
            }
        }

        if (showFiltersSheet) {
            Dialog(onDismissRequest = { showFiltersSheet = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Search Filters", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)

                        // Vehicle type dropdown simulator
                        Text("Vehicle Type", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("ANY", "BIKE", "CAR", "SUV", "TRUCK").forEach { type ->
                                FilterChip(
                                    selected = filterType == type,
                                    onClick = { viewModel.filterVehicleType.value = type },
                                    label = { Text(type, fontSize = 10.sp) }
                                )
                            }
                        }

                        // Switches
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Covered Bays Only", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = filterCovered, onCheckedChange = { viewModel.filterCoveredOnly.value = it })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("EV Charging Station Available", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = filterCharging, onCheckedChange = { viewModel.filterHasCharging.value = it })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("24x7 CCTV & Security Guard", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = filterSecurity, onCheckedChange = { viewModel.filterHasSecurity.value = it })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Sort by Price (Low to High)", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = sortPrice, onCheckedChange = { viewModel.sortByPrice.value = it })
                        }

                        Button(
                            onClick = { showFiltersSheet = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Filters")
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (isMapView) {
                // Interactive Canvas Map Simulator
                MapSimulator(
                    spaces = filteredSpaces,
                    onSelectSpace = { space -> viewModel.selectSpace(space.id) }
                )
            } else {
                // List View
                if (filteredSpaces.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "No listings", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No parking spaces match your search criteria. Try adjusting your filters.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredSpaces) { space ->
                            val isFav = favIds.contains(space.id)
                            ParkingItemCard(
                                space = space,
                                isFav = isFav,
                                onToggleFav = { viewModel.toggleFavorite(space.id) },
                                onBook = { viewModel.selectSpace(space.id) }
                            )
                        }
                    }
                }
            }

            // Booking & Parking Details bottom sheet/dialog
            if (selectedSpaceId != null) {
                val selectedSpace = spaces.find { it.id == selectedSpaceId }
                if (selectedSpace != null) {
                    ParkingDetailsSheet(
                        space = selectedSpace,
                        viewModel = viewModel,
                        onDismiss = { viewModel.selectSpace(null) }
                    )
                }
            }
        }
    }
}

@Composable
fun MapSimulator(
    spaces: List<ParkingSpace>,
    onSelectSpace: (ParkingSpace) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val gridColor = if (isDark) Color(0x11FFFFFF) else Color(0x0A000000)

    Box(modifier = Modifier.fillMaxSize()) {
        // Draw canvas grid representational map
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // draw grid lines
            val step = 40.dp.toPx()
            for (x in 0..(width / step).toInt()) {
                drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, height), strokeWidth = 1f)
            }
            for (y in 0..(height / step).toInt()) {
                drawLine(gridColor, Offset(0f, y * step), Offset(width, y * step), strokeWidth = 1f)
            }
        }

        // Draw parking lot dots
        spaces.forEachIndexed { idx, space ->
            // Distribute markers deterministically across the simulator canvas
            val xOffset = 60.dp + (80.dp * idx)
            val yOffset = 100.dp + (130.dp * idx)

            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .size(48.dp)
                    .clickable { onSelectSpace(space) }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = space.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = space.name.take(10) + "...",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Help card Overlay
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, contentDescription = "Location", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulating Leaflet/OpenStreetMap. Click on any pin indicator.", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ParkingItemCard(
    space: ParkingSpace,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onBook: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(space.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("4.8 (12 Reviews)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }

                IconButton(onClick = onToggleFav) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Text(
                text = space.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            // Amenities row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (space.isCovered) AmenityBadge("Covered")
                if (space.hasCharging) AmenityBadge("EV Charging")
                if (space.hasCCTV) AmenityBadge("CCTV")
                if (space.hasSecurity) AmenityBadge("Security Guard")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Starts From", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text("$${"%.2f".format(space.carPrice)}/hr", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = onBook,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reserve Slot")
                }
            }
        }
    }
}

@Composable
fun AmenityBadge(label: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ==================== BOOKING DETAILS SHEET ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingDetailsSheet(
    space: ParkingSpace,
    viewModel: ParkFlowViewModel,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val vehicles by viewModel.userVehicles.collectAsState()
    val slots by viewModel.selectedSpaceSlots.collectAsState()
    val reviews by viewModel.selectedSpaceReviews.collectAsState()

    var bookingDate by remember { mutableStateOf("2026-06-29") }
    var bookingTime by remember { mutableStateOf("12:00") }
    var durationHours by remember { mutableStateOf(2) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var paymentMethod by remember { mutableStateOf("WALLET") } // "WALLET", "CARD"

    var selectedRatingForReview by remember { mutableStateOf(5) }
    var newReviewComment by remember { mutableStateOf("") }

    var bookingMessage by remember { mutableStateOf<String?>(null) }
    var isBookingSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(space) {
        viewModel.selectSpace(space.id)
    }

    LaunchedEffect(vehicles) {
        if (vehicles.isNotEmpty() && selectedVehicle == null) {
            selectedVehicle = vehicles.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Parking Detail & Booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Image placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalParking, contentDescription = "Parking Space image", tint = Color.White, modifier = Modifier.size(48.dp))
                    }

                    // Metadata
                    Column {
                        Text(space.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(space.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    Text(space.description, style = MaterialTheme.typography.bodyMedium)

                    // Amenities checklist
                    Text("Facilities & Security", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (space.isCovered) Icon(Icons.Default.Roofing, contentDescription = "Covered", tint = MaterialTheme.colorScheme.primary)
                        if (space.hasCCTV) Icon(Icons.Default.Videocam, contentDescription = "CCTV", tint = MaterialTheme.colorScheme.primary)
                        if (space.hasSecurity) Icon(Icons.Default.Shield, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary)
                        if (space.hasCharging) Icon(Icons.Default.EvStation, contentDescription = "EV Charging", tint = MaterialTheme.colorScheme.primary)
                        if (space.hasWashroom) Icon(Icons.Default.Wc, contentDescription = "Washroom", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Live Available Slots Counts
                    val availableSlots = slots.count { it.status == "AVAILABLE" }
                    val totalSlots = slots.size
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Live Available Slots:", fontWeight = FontWeight.Bold)
                            Text(
                                "$availableSlots of $totalSlots Available",
                                fontWeight = FontWeight.Black,
                                color = if (availableSlots > 2) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }

                    // Pricing List
                    Text("Pricing per Hour", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🚗 Car", style = MaterialTheme.typography.bodySmall)
                            Text("$${"%.2f".format(space.carPrice)}", fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🏍️ Bike", style = MaterialTheme.typography.bodySmall)
                            Text("$${"%.2f".format(space.bikePrice)}", fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SUV", style = MaterialTheme.typography.bodySmall)
                            Text("$${"%.2f".format(space.suvPrice)}", fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider()

                    // BOOKING INPUT FORM
                    Text("Configure Booking", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = bookingDate,
                        onValueChange = { bookingDate = it },
                        label = { Text("Booking Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bookingTime,
                        onValueChange = { bookingTime = it },
                        label = { Text("Booking Time (HH:MM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Duration selector
                    Column {
                        Text("Duration: $durationHours Hours", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = durationHours.toFloat(),
                            onValueChange = { durationHours = it.toInt() },
                            valueRange = 1f..12f,
                            steps = 11
                        )
                    }

                    // Vehicle select dropdown
                    if (vehicles.isEmpty()) {
                        Text("⚠️ Add a vehicle in the 'Vehicles' tab first before booking!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Select Registered Vehicle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            vehicles.forEach { veh ->
                                FilterChip(
                                    selected = selectedVehicle?.id == veh.id,
                                    onClick = { selectedVehicle = veh },
                                    label = { Text("${veh.nickname} (${veh.licensePlate})") }
                                )
                            }
                        }
                    }

                    // Payment select dropdown
                    Text("Payment Method", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { paymentMethod = "WALLET" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paymentMethod == "WALLET") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (paymentMethod == "WALLET") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Wallet Balance")
                        }

                        Button(
                            onClick = { paymentMethod = "CARD" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paymentMethod == "CARD") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (paymentMethod == "CARD") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Mock Debit Card")
                        }
                    }

                    // Cost Calculation summary
                    val rate = when (selectedVehicle?.type) {
                        "BIKE" -> space.bikePrice
                        "SUV" -> space.suvPrice
                        "TRUCK" -> space.truckPrice
                        else -> space.carPrice // Default is car
                    }
                    val totalCost = rate * durationHours

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Estimated Amount:", fontWeight = FontWeight.Bold)
                            Text("$${"%.2f".format(totalCost)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (bookingMessage != null) {
                        Text(
                            text = bookingMessage ?: "",
                            color = if (isBookingSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val veh = selectedVehicle
                            if (veh == null) {
                                bookingMessage = "Please register/select a vehicle first."
                                return@Button
                            }
                            viewModel.createBooking(
                                space = space,
                                vehicle = veh,
                                date = bookingDate,
                                time = bookingTime,
                                duration = durationHours,
                                amount = totalCost,
                                paymentMethod = paymentMethod,
                                onSuccess = {
                                    isBookingSuccess = true
                                    bookingMessage = "Booking Placed Successfully! Pending landowner acceptance."
                                },
                                onError = { error ->
                                    isBookingSuccess = false
                                    bookingMessage = error
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedVehicle != null && !isBookingSuccess
                    ) {
                        Text("Confirm booking & Pay Now", fontWeight = FontWeight.Bold)
                    }

                    Divider()

                    // REVIEWS SECTION
                    Text("Customer Reviews", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    // Write a review form
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Write a Review", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = if (star <= selectedRatingForReview) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "$star stars",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { selectedRatingForReview = star }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = newReviewComment,
                                onValueChange = { newReviewComment = it },
                                placeholder = { Text("Comment on facilities, security...") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (newReviewComment.isNotEmpty()) {
                                        viewModel.postReview(space.id, selectedRatingForReview, newReviewComment)
                                        newReviewComment = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Post Review")
                            }
                        }
                    }

                    // Reviews List
                    if (reviews.isEmpty()) {
                        Text("No reviews yet. Be the first to review!", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        reviews.forEach { rev ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(rev.parkerName, fontWeight = FontWeight.Bold)
                                        Row {
                                            (1..rev.rating).forEach {
                                                Icon(Icons.Default.Star, contentDescription = "*", tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall)

                                    if (rev.reply.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("Landowner Reply:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                Text(rev.reply, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== MY BOOKINGS TAB ====================
@Composable
fun ParkerBookingsTab(
    viewModel: ParkFlowViewModel,
    user: User
) {
    val bookings by viewModel.userBookings.collectAsState()
    val spaces by viewModel.allActiveParkingSpaces.collectAsState()

    if (bookings.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Inbox, contentDescription = "No bookings", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No bookings history found.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Reserve a slot in 'Find' tab to see bookings here.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(bookings) { booking ->
            val space = spaces.find { it.id == booking.parkingSpaceId }
            val spaceName = space?.name ?: "Parking Space"
            val spaceAddress = space?.address ?: ""

            var showQrDialog by remember { mutableStateOf(false) }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(spaceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (booking.status) {
                                    "COMPLETED" -> Color(0xFF10B981)
                                    "CANCELLED" -> Color(0xFFEF4444)
                                    "ACTIVE" -> Color(0xFF3B82F6)
                                    else -> Color(0xFFF59E0B) // Pending/Confirmed
                                }
                            )
                        ) {
                            Text(
                                text = booking.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(spaceAddress, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Divider()

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Date & Time: ${booking.date} at ${booking.time}", style = MaterialTheme.typography.bodySmall)
                        Text("Paid: $${"%.2f".format(booking.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // QR View (if active/confirmed)
                        if (booking.status == "CONFIRMED" || booking.status == "ACTIVE" || booking.status == "PENDING") {
                            Button(
                                onClick = { showQrDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = "QR Code")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Display QR", fontSize = 11.sp)
                            }
                        }

                        // Cancel Button (if pending/confirmed)
                        if (booking.status == "PENDING" || booking.status == "CONFIRMED") {
                            Button(
                                onClick = { viewModel.cancelBooking(booking) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Cancel Booking", fontSize = 11.sp)
                            }
                        }

                        // Invoice Download Simulator for completed bookings
                        if (booking.status == "COMPLETED") {
                            var showInvoiceDialog by remember { mutableStateOf(false) }
                            Button(
                                onClick = { showInvoiceDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Invoice")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invoice PDF", fontSize = 11.sp)
                            }

                            if (showInvoiceDialog) {
                                Dialog(onDismissRequest = { showInvoiceDialog = false }) {
                                    Card(shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("Invoice Downloaded", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                            Text("PDF Invoice has been simulated and saved to device downloads.", style = MaterialTheme.typography.bodyMedium)
                                            Divider()
                                            Text("Booking ID: #${booking.id}", style = MaterialTheme.typography.labelSmall)
                                            Text("Amount Paid: $${"%.2f".format(booking.totalAmount)}", fontWeight = FontWeight.Bold)
                                            Button(onClick = { showInvoiceDialog = false }, modifier = Modifier.fillMaxWidth()) {
                                                Text("Done")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showQrDialog) {
                        BookingQrDialog(booking, spaceName) { showQrDialog = false }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingQrDialog(
    booking: Booking,
    spaceName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Verification Ticket", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(spaceName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                // Render high-fidelity QR Code representation on a custom Canvas
                Canvas(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                ) {
                    val side = size.width
                    // draw mock QR code blocks
                    val step = side / 5
                    for (i in 0..4) {
                        for (j in 0..4) {
                            // Pseudo random blocks based on booking id
                            if ((i + j + booking.id) % 2 == 0L || (i == 0 && j == 0) || (i == 4 && j == 0) || (i == 0 && j == 4)) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(i * step, j * step),
                                    size = Size(step, step)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "ID: ${booking.qrCodeData}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Text(
                    text = "Present this ticket to Landowner upon arrival to scan and check-in.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close Ticket")
                }
            }
        }
    }
}

// ==================== VEHICLES TAB ====================
@Composable
fun ParkerVehiclesTab(viewModel: ParkFlowViewModel) {
    val vehicles by viewModel.userVehicles.collectAsState()

    var showAddVehicle by remember { mutableStateOf(false) }
    var licensePlate by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("CAR") } // "BIKE", "CAR", "SUV", "TRUCK"
    var vehicleNickname by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Registered Vehicles", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showAddVehicle = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        if (showAddVehicle) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add New Vehicle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it },
                        label = { Text("License Plate Number (e.g. NY-77-P)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vehicleNickname,
                        onValueChange = { vehicleNickname = it },
                        label = { Text("Vehicle Nickname (e.g. My Tesla)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Vehicle Type chip selection
                    Text("Select Vehicle Category", style = MaterialTheme.typography.bodySmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("BIKE", "CAR", "SUV", "TRUCK").forEach { type ->
                            FilterChip(
                                selected = vehicleType == type,
                                onClick = { vehicleType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showAddVehicle = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (licensePlate.isNotEmpty() && vehicleNickname.isNotEmpty()) {
                                    viewModel.addVehicle(licensePlate, vehicleType, vehicleNickname)
                                    licensePlate = ""
                                    vehicleNickname = ""
                                    showAddVehicle = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = licensePlate.isNotEmpty() && vehicleNickname.isNotEmpty()
                        ) {
                            Text("Add Vehicle")
                        }
                    }
                }
            }
        }

        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No registered vehicles. Add your first car/bike to begin booking!", color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles) { veh ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (veh.type) {
                                        "BIKE" -> Icons.Default.TwoWheeler
                                        "SUV" -> Icons.Default.AirportShuttle
                                        "TRUCK" -> Icons.Default.LocalShipping
                                        else -> Icons.Default.DirectionsCar
                                    },
                                    contentDescription = veh.type,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(veh.nickname, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Plate: ${veh.licensePlate} • ${veh.type}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteVehicle(veh) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete vehicle", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== PROFILE TAB ====================
@Composable
fun ParkerProfileTab(viewModel: ParkFlowViewModel, user: User) {
    var name by remember { mutableStateOf(user.name) }
    var mobile by remember { mutableStateOf(user.mobile) }
    var emergencyContact by remember { mutableStateOf(user.emergencyContact) }
    var address by remember { mutableStateOf(user.address) }

    var saveMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Manage Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Pic",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = user.email,
                    onValueChange = {},
                    label = { Text("Email (Locked)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                if (saveMessage != null) {
                    Text(saveMessage!!, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.updateProfile(name, mobile, emergencyContact, address)
                        saveMessage = "Profile Updated Successfully!"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

// ==================== WALLET TAB ====================
@Composable
fun ParkerWalletTab(viewModel: ParkFlowViewModel, user: User) {
    val bookings by viewModel.userBookings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Wallet & Payments History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ParkFlow Digital Wallet", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "$${"%.2f".format(user.walletBalance)}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Text("Secure payments, automatic refunds, and direct checking.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        Text("Billing & Refund Transactions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

        if (bookings.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No payment transactions found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings) { booking ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (booking.status == "CANCELLED") "Booking Refund" else "Parking Reservation",
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Booking ID: #${booking.id} • ${booking.date}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Text(
                                text = if (booking.status == "CANCELLED") "+$${"%.2f".format(booking.totalAmount)}" else "-$${"%.2f".format(booking.totalAmount)}",
                                fontWeight = FontWeight.Black,
                                color = if (booking.status == "CANCELLED") Color(0xFF10B981) else Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== NOTIFICATIONS TAB ====================
@Composable
fun ParkerNotificationsTab(viewModel: ParkFlowViewModel) {
    val notifications by viewModel.userNotifications.collectAsState()

    LaunchedEffect(Unit) {
        val user = viewModel.currentUser.value
        if (user != null) {
            viewModel.dao.markAllNotificationsAsRead(user.id)
        }
    }

    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No notifications yet.", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notifications) { notif ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(notif.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        if (!notif.isRead) {
                            Badge { Text("NEW", fontSize = 8.sp) }
                        }
                    }
                    Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

                    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val dateStr = formatter.format(Date(notif.timestamp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

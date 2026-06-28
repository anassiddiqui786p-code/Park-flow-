package com.example.ui.screens

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
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun LandownerDashboardScreen(
    viewModel: ParkFlowViewModel,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf("DASHBOARD") } // "DASHBOARD", "SPACES", "SLOTS", "REQUESTS", "ENTRY", "ACTIVE_VEHICLES", "REPORTS", "REVIEWS", "SETTINGS"
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser == null) {
        return
    }

    val user = currentUser!!

    val landownerSpaces by viewModel.landownerParkingSpaces.collectAsState()
    val bookings by viewModel.userBookings.collectAsState()
    val pendingBookings = bookings.filter { it.status == "PENDING" }

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
                    selected = activeTab == "SPACES",
                    onClick = { activeTab = "SPACES" },
                    icon = { Icon(Icons.Default.HomeWork, contentDescription = "Parking Spaces") },
                    label = { Text("Spaces") }
                )
                NavigationBarItem(
                    selected = activeTab == "REQUESTS" || activeTab == "ENTRY" || activeTab == "ACTIVE_VEHICLES",
                    onClick = { activeTab = "REQUESTS" },
                    icon = {
                        Box {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Operations")
                            if (pendingBookings.isNotEmpty()) {
                                Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                                    Text(pendingBookings.size.toString())
                                }
                            }
                        }
                    },
                    label = { Text("Ops") }
                )
                NavigationBarItem(
                    selected = activeTab == "REPORTS",
                    onClick = { activeTab = "REPORTS" },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Reports") },
                    label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = activeTab == "SETTINGS" || activeTab == "REVIEWS",
                    onClick = { activeTab = "SETTINGS" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
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
                // Top Landowner custom app bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Landowner Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { activeTab = "REVIEWS" }) {
                            Icon(Icons.Default.RateReview, contentDescription = "Reviews", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(onClick = {
                            viewModel.logout()
                            onLogout()
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Render Landowner Tabs
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeTab) {
                        "DASHBOARD" -> LandownerHomeTab(viewModel, landownerSpaces, bookings, { activeTab = "REQUESTS" }, { activeTab = "ENTRY" }, { activeTab = "ACTIVE_VEHICLES" })
                        "SPACES" -> LandownerSpacesTab(viewModel, landownerSpaces)
                        "SLOTS" -> LandownerSlotsTab(viewModel, landownerSpaces)
                        "REQUESTS" -> LandownerBookingRequestsTab(viewModel, pendingBookings, { activeTab = "ENTRY" }, { activeTab = "ACTIVE_VEHICLES" })
                        "ENTRY" -> LandownerAllowEntryTab(viewModel, bookings, landownerSpaces)
                        "ACTIVE_VEHICLES" -> LandownerActiveVehiclesTab(viewModel)
                        "REPORTS" -> LandownerReportsTab(viewModel, bookings)
                        "REVIEWS" -> LandownerReviewsTab(viewModel)
                        "SETTINGS" -> LandownerSettingsTab(viewModel, user)
                    }
                }
            }
        }
    }
}

// ==================== HOME TAB ====================
@Composable
fun LandownerHomeTab(
    viewModel: ParkFlowViewModel,
    spaces: List<ParkingSpace>,
    bookings: List<Booking>,
    onGoToRequests: () -> Unit,
    onGoToEntry: () -> Unit,
    onGoToActive: () -> Unit
) {
    val scrollState = rememberScrollState()
    val activeTickets by viewModel.landownerActiveTickets.collectAsState()

    // Calculate revenue stats
    val completedBookings = bookings.filter { it.status == "COMPLETED" }
    val totalRevenue = completedBookings.sumOf { it.totalAmount } + activeTickets.sumOf { it.extraCharges }
    val todayRevenue = totalRevenue * 0.4 // Mock daily slice of total
    val activeBookingsCount = bookings.count { it.status == "ACTIVE" }
    val pendingCount = bookings.count { it.status == "PENDING" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Quick Operations Navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Button(
                onClick = onGoToRequests,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$pendingCount", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Requests", fontSize = 10.sp)
                }
            }
            Button(
                onClick = onGoToEntry,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Allow Entry")
                    Text("Check In", fontSize = 10.sp)
                }
            }
            Button(
                onClick = onGoToActive,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${activeTickets.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Parked", fontSize = 10.sp)
                }
            }
        }

        // Live Revenue Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text("Today's Revenue", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$${"%.2f".format(todayRevenue)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            GlassCard(modifier = Modifier.weight(1f)) {
                Column {
                    Text("Total Earnings", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$${"%.2f".format(totalRevenue)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge, color = Color(0xFF10B981))
                }
            }
        }

        // Live Occupancy Progress Canvas
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Overall Lot Occupancy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                val activeParkedCount = activeTickets.size
                val totalSlotsCount = 13 // Mock spaces capacity
                val occupancyPercent = if (totalSlotsCount > 0) (activeParkedCount.toFloat() / totalSlotsCount) else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Parked Vehicles: $activeParkedCount", style = MaterialTheme.typography.bodySmall)
                        Text("Available Slots: ${totalSlotsCount - activeParkedCount}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${(occupancyPercent * 100).toInt()}%", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                LinearProgressIndicator(
                    progress = occupancyPercent,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            }
        }

        // Monthly Earnings Graph (Custom drawn with Canvas!)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Monthly Revenue Progress", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Simulation for past 6 months ($)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                val points = listOf(150f, 320f, 250f, 480f, 620f, 550f)
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(top = 16.dp, bottom = 4.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val stepX = w / 5

                    // Draw area line
                    for (i in 0..4) {
                        val currentX = i * stepX
                        val nextX = (i + 1) * stepX
                        val currentY = h - (points[i] / 700f * h)
                        val nextY = h - (points[i + 1] / 700f * h)

                        drawLine(
                            color = Color(0xFF3B82F6),
                            start = Offset(currentX, currentY),
                            end = Offset(nextX, nextY),
                            strokeWidth = 6f
                        )

                        // Draw points
                        drawCircle(
                            color = Color(0xFF2563EB),
                            radius = 6f,
                            center = Offset(currentX, currentY)
                        )
                    }

                    // Last point circle
                    drawCircle(
                        color = Color(0xFF2563EB),
                        radius = 6f,
                        center = Offset(w, h - (points[5] / 700f * h))
                    )
                }

                // Month labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    months.forEach { month ->
                        Text(month, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent bookings feed list
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Recent Operations Activity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (bookings.isEmpty()) {
                Text("No reservations activity recorded.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            } else {
                bookings.take(3).forEach { b ->
                    val spaceName = spaces.find { it.id == b.parkingSpaceId }?.name ?: "Parking Space"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(spaceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Booking ID: #${b.id} • ${b.status}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text("$${"%.2f".format(b.totalAmount)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==================== LIST / ADD PARKING TAB ====================
@Composable
fun LandownerSpacesTab(
    viewModel: ParkFlowViewModel,
    spaces: List<ParkingSpace>
) {
    var showAddSpace by remember { mutableStateOf(false) }

    // Add Space inputs
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("40.7128") }
    var lng by remember { mutableStateOf("-74.0060") }
    var isCovered by remember { mutableStateOf(true) }
    var openTime by remember { mutableStateOf("00:00") }
    var closeTime by remember { mutableStateOf("23:59") }

    // Amenities
    var cctv by remember { mutableStateOf(true) }
    var security by remember { mutableStateOf(true) }
    var washroom by remember { mutableStateOf(false) }
    var charging by remember { mutableStateOf(false) }
    var shade by remember { mutableStateOf(false) }
    var insurance by remember { mutableStateOf(false) }

    // Pricing
    var bikePrice by remember { mutableStateOf("8.0") }
    var carPrice by remember { mutableStateOf("15.0") }
    var suvPrice by remember { mutableStateOf("25.0") }
    var truckPrice by remember { mutableStateOf("40.0") }

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
            Text("Listed Parking Spaces", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showAddSpace = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Parking")
                Spacer(modifier = Modifier.width(4.dp))
                Text("List Space")
            }
        }

        if (showAddSpace) {
            Dialog(onDismissRequest = { showAddSpace = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("List New Parking Garage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)

                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Parking Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address / City / ZIP") }, modifier = Modifier.fillMaxWidth())

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = lng, onValueChange = { lng = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f), singleLine = true)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Covered / Open Space", fontWeight = FontWeight.Bold)
                            Switch(checked = isCovered, onCheckedChange = { isCovered = it })
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = openTime, onValueChange = { openTime = it }, label = { Text("Opening Hours") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = closeTime, onValueChange = { closeTime = it }, label = { Text("Closing Hours") }, modifier = Modifier.weight(1f), singleLine = true)
                        }

                        Text("Amenities & Security Guard", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = cctv, onCheckedChange = { cctv = it })
                            Text("CCTV Camera", modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(16.dp))
                            Checkbox(checked = security, onCheckedChange = { security = it })
                            Text("Security Guard", modifier = Modifier.align(Alignment.CenterVertically))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = washroom, onCheckedChange = { washroom = it })
                            Text("Washroom", modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(16.dp))
                            Checkbox(checked = charging, onCheckedChange = { charging = it })
                            Text("EV Charging", modifier = Modifier.align(Alignment.CenterVertically))
                        }

                        Text("Pricing per Hour ($)", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = bikePrice, onValueChange = { bikePrice = it }, label = { Text("Bike") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = carPrice, onValueChange = { carPrice = it }, label = { Text("Car") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = suvPrice, onValueChange = { suvPrice = it }, label = { Text("SUV") }, modifier = Modifier.weight(1f), singleLine = true)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(onClick = { showAddSpace = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                            Button(
                                onClick = {
                                    if (name.isNotEmpty() && address.isNotEmpty()) {
                                        viewModel.addParkingSpace(
                                            name, description, address,
                                            lat.toDoubleOrNull() ?: 0.0,
                                            lng.toDoubleOrNull() ?: 0.0,
                                            isCovered, openTime, closeTime,
                                            cctv, security, washroom, charging, shade, insurance,
                                            bikePrice.toDoubleOrNull() ?: 5.0,
                                            carPrice.toDoubleOrNull() ?: 10.0,
                                            suvPrice.toDoubleOrNull() ?: 15.0,
                                            truckPrice.toDoubleOrNull() ?: 30.0
                                        )
                                        showAddSpace = false
                                        name = ""
                                        address = ""
                                        description = ""
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = name.isNotEmpty() && address.isNotEmpty()
                            ) {
                                Text("Submit Listing")
                            }
                        }
                    }
                }
            }
        }

        if (spaces.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No listed spaces. Click 'List Space' to register your lot!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(spaces) { space ->
                    var activeStatus by remember { mutableStateOf(space.status) }

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(space.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            val nextStatus = if (activeStatus == "ACTIVE") "PAUSED" else "ACTIVE"
                                            viewModel.updateParkingSpaceStatus(space, nextStatus)
                                            activeStatus = nextStatus
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (activeStatus == "ACTIVE") Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Pause listing",
                                            tint = if (activeStatus == "ACTIVE") Color(0xFFF59E0B) else Color(0xFF10B981)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deleteParkingSpace(space) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            Text(space.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Divider()
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Status: ${space.status}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Car Rate: $${"%.2f".format(space.carPrice)}/hr", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== SLOT MANAGEMENT TAB ====================
@Composable
fun LandownerSlotsTab(
    viewModel: ParkFlowViewModel,
    spaces: List<ParkingSpace>
) {
    var selectedSpace by remember { mutableStateOf<ParkingSpace?>(null) }
    val slots by viewModel.selectedSpaceSlots.collectAsState()

    var showAddCustomSlot by remember { mutableStateOf(false) }
    var customSlotNum by remember { mutableStateOf("") }
    var customSlotType by remember { mutableStateOf("CAR") }

    LaunchedEffect(spaces) {
        if (spaces.isNotEmpty() && selectedSpace == null) {
            selectedSpace = spaces.first()
        }
    }

    LaunchedEffect(selectedSpace) {
        if (selectedSpace != null) {
            viewModel.selectSpace(selectedSpace!!.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Lot Slot Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        if (spaces.isEmpty()) {
            Text("List a parking space first to manage individual slots.", color = Color.Gray)
            return
        }

        // Space dropdown selector
        Text("Select Parking Space", style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            spaces.forEach { sp ->
                FilterChip(
                    selected = selectedSpace?.id == sp.id,
                    onClick = { selectedSpace = sp },
                    label = { Text(sp.name.take(15) + "...") }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bays in Selected Garage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { showAddCustomSlot = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add custom slot")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Slot")
            }
        }

        if (showAddCustomSlot) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add Custom Bay Slot", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = customSlotNum, onValueChange = { customSlotNum = it }, label = { Text("Slot Label (e.g. C4, B5)") }, modifier = Modifier.fillMaxWidth())

                    Text("Vehicle Bay Class", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("BIKE", "CAR", "SUV", "TRUCK").forEach { type ->
                            FilterChip(
                                selected = customSlotType == type,
                                onClick = { customSlotType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showAddCustomSlot = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (customSlotNum.isNotEmpty() && selectedSpace != null) {
                                    viewModel.createCustomSlot(selectedSpace!!.id, customSlotNum, customSlotType)
                                    customSlotNum = ""
                                    showAddCustomSlot = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = customSlotNum.isNotEmpty()
                        ) {
                            Text("Create Slot")
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(slots) { slot ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (slot.vehicleType) {
                                    "BIKE" -> Icons.Default.TwoWheeler
                                    "SUV" -> Icons.Default.AirportShuttle
                                    "TRUCK" -> Icons.Default.LocalShipping
                                    else -> Icons.Default.DirectionsCar
                                },
                                contentDescription = slot.vehicleType,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Slot ${slot.slotNumber} (${slot.vehicleType})", fontWeight = FontWeight.Bold)
                        }

                        // Slot status drop selector simulator
                        var expandedStatus by remember { mutableStateOf(false) }
                        Box {
                            Button(onClick = { expandedStatus = true }, shape = RoundedCornerShape(8.dp)) {
                                Text(slot.status, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Edit status", modifier = Modifier.size(12.dp))
                            }
                            DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                listOf("AVAILABLE", "OCCUPIED", "RESERVED", "MAINTENANCE").forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        onClick = {
                                            viewModel.updateSlotStatus(slot, s)
                                            expandedStatus = false
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
}

// ==================== BOOKING REQUESTS TAB ====================
@Composable
fun LandownerBookingRequestsTab(
    viewModel: ParkFlowViewModel,
    requests: List<Booking>,
    onGoToCheckIn: () -> Unit,
    onGoToActive: () -> Unit
) {
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
            Text("Pending Booking Requests", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGoToCheckIn, shape = RoundedCornerShape(8.dp)) { Text("Check-In Panel", fontSize = 10.sp) }
                Button(onClick = onGoToActive, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Parked List", fontSize = 10.sp) }
            }
        }

        if (requests.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "All caught up", tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No pending requests found! Nice work.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests) { req ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Booking #${req.id}", fontWeight = FontWeight.Bold)
                                Text("Amount: $${"%.2f".format(req.totalAmount)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            }

                            Divider()
                            Text("Customer: John Parker (ID #${req.parkerId})", style = MaterialTheme.typography.bodySmall)
                            Text("Arrival: ${req.date} at ${req.time}", style = MaterialTheme.typography.bodySmall)
                            Text("Hours Booked: ${req.durationHours}", style = MaterialTheme.typography.bodySmall)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.rejectBooking(req) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Decline")
                                }

                                Button(
                                    onClick = { viewModel.acceptBooking(req) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Approve")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== ALLOW ENTRY CHECK-IN TAB ====================
@Composable
fun LandownerAllowEntryTab(
    viewModel: ParkFlowViewModel,
    bookings: List<Booking>,
    spaces: List<ParkingSpace>
) {
    var checkInQuery by remember { mutableStateOf("") }
    var selectedBookingForCheckIn by remember { mutableStateOf<Booking?>(null) }
    var selectedSlotForCheckIn by remember { mutableStateOf<ParkingSlot?>(null) }

    val slots by viewModel.selectedSpaceSlots.collectAsState()
    val confirmedBookings = bookings.filter { it.status == "CONFIRMED" }

    var checkInMessage by remember { mutableStateOf<String?>(null) }
    var isCheckInSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(selectedBookingForCheckIn) {
        if (selectedBookingForCheckIn != null) {
            viewModel.selectSpace(selectedBookingForCheckIn!!.parkingSpaceId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Smart Entry & Check-In", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Scan Entry Ticket or Enter ID", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = checkInQuery,
                    onValueChange = { checkInQuery = it },
                    placeholder = { Text("Booking ID or Enter Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val matchingBooking = confirmedBookings.find {
                            it.id.toString() == checkInQuery.trim() || it.qrCodeData == checkInQuery.trim()
                        }
                        if (matchingBooking != null) {
                            selectedBookingForCheckIn = matchingBooking
                            checkInMessage = "Booking Located!"
                        } else {
                            checkInMessage = "Invalid Ticket or Booking Code."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify Ticket")
                }
            }
        }

        // Confirmed bookings quick list
        Text("Upcoming Confirmed Check-ins", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        if (confirmedBookings.isEmpty()) {
            Text("No confirmed check-ins today.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        } else {
            confirmedBookings.forEach { booking ->
                val spaceName = spaces.find { it.id == booking.parkingSpaceId }?.name ?: "Parking"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedBookingForCheckIn = booking
                            checkInMessage = null
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedBookingForCheckIn?.id == booking.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(spaceName, fontWeight = FontWeight.Bold)
                        Text("Booking ID: #${booking.id} • Arriving: ${booking.time}", style = MaterialTheme.typography.bodySmall)
                        Text("Duration: ${booking.durationHours} Hours", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Assign Slot and Complete CheckIn UI
        if (selectedBookingForCheckIn != null) {
            val booking = selectedBookingForCheckIn!!
            val space = spaces.find { it.id == booking.parkingSpaceId }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Allocate Bay & Open Gate", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text("Garage: ${space?.name ?: ""}", style = MaterialTheme.typography.bodySmall)
                    Text("Required Vehicle Bay: CAR", style = MaterialTheme.typography.bodySmall)

                    // Available slots selection
                    val availableSlots = slots.filter { it.status == "AVAILABLE" }
                    if (availableSlots.isEmpty()) {
                        Text("⚠️ No vacant slots available in lot! Change a slot status to available.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Select Available Bay Slot", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            availableSlots.forEach { sl ->
                                FilterChip(
                                    selected = selectedSlotForCheckIn?.id == sl.id,
                                    onClick = { selectedSlotForCheckIn = sl },
                                    label = { Text("Slot ${sl.slotNumber}") }
                                )
                            }
                        }
                    }

                    if (checkInMessage != null) {
                        Text(checkInMessage!!, fontWeight = FontWeight.Bold, color = if (isCheckInSuccess) Color(0xFF10B981) else Color.Red)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                selectedBookingForCheckIn = null
                                selectedSlotForCheckIn = null
                                checkInMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }

                        Button(
                            onClick = {
                                val sl = selectedSlotForCheckIn
                                if (sl != null) {
                                    viewModel.checkInBooking(
                                        booking = booking,
                                        slot = sl,
                                        onSuccess = {
                                            isCheckInSuccess = true
                                            checkInMessage = "Check-in Complete! Gate Open. Slot ${sl.slotNumber} Allocated."
                                        },
                                        onError = { error ->
                                            isCheckInSuccess = false
                                            checkInMessage = error
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedSlotForCheckIn != null && !isCheckInSuccess
                        ) {
                            Text("Open Gate & Start")
                        }
                    }

                    // Ticket print/download simulation
                    if (isCheckInSuccess) {
                        var showTicketDialog by remember { mutableStateOf(false) }
                        Button(
                            onClick = { showTicketDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print Ticket")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print Entry Ticket PDF")
                        }

                        if (showTicketDialog) {
                            Dialog(onDismissRequest = { showTicketDialog = false }) {
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Entry Ticket Printed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                        Text("Entry ticket PDF with allocation details has been simulated and printed successfully.", style = MaterialTheme.typography.bodyMedium)
                                        Divider()
                                        Text("Ticket Number: TC-ENT-${System.currentTimeMillis()}", style = MaterialTheme.typography.bodySmall)
                                        Text("Assigned Slot: Slot ${selectedSlotForCheckIn?.slotNumber}", fontWeight = FontWeight.Bold)
                                        Button(onClick = { showTicketDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
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

// ==================== ACTIVE VEHICLES TAB ====================
@Composable
fun LandownerActiveVehiclesTab(viewModel: ParkFlowViewModel) {
    val activeTickets by viewModel.landownerActiveTickets.collectAsState()
    var selectedTicketForExit by remember { mutableStateOf<Ticket?>(null) }
    var selectedExitPaymentMethod by remember { mutableStateOf("WALLET") } // "WALLET", "CASH"

    var exitMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Active Parked Vehicles", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        if (activeTickets.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No vehicles currently parked in your lots.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeTickets) { ticket ->
                    // Calculate elapsed time (simulated past 1 hour)
                    val elapsedMs = System.currentTimeMillis() - ticket.entryTime
                    val elapsedMinutes = (elapsedMs / (1000 * 60)).toInt()

                    // Extra charges calculations (past expected exit, e.g. mock late fee of $10)
                    val isLate = System.currentTimeMillis() > ticket.expectedExitTime
                    val extraFee = if (isLate) 15.0 else 0.0

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ticket ID: #${ticket.id}", fontWeight = FontWeight.Bold)
                                Card(colors = CardDefaults.cardColors(containerColor = if (isLate) Color(0xFFEF4444) else Color(0xFF10B981))) {
                                    Text(
                                        text = if (isLate) "OVERDUE" else "PARKED",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Divider()
                            Text("Time Parked: $elapsedMinutes mins", style = MaterialTheme.typography.bodySmall)
                            Text("Current Cost: $${"%.2f".format(ticket.baseCharges)}", style = MaterialTheme.typography.bodySmall)
                            if (isLate) {
                                Text("Extra Delay Charges: $${"%.2f".format(extraFee)}", color = Color.Red, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    selectedTicketForExit = ticket
                                    exitMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.LocalParking, contentDescription = "Exit")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Process Exit Checkout")
                            }
                        }
                    }
                }
            }
        }

        // Exit process dialog details
        if (selectedTicketForExit != null) {
            val ticket = selectedTicketForExit!!
            val isLate = System.currentTimeMillis() > ticket.expectedExitTime
            val extraFee = if (isLate) 15.0 else 0.0
            val grandTotal = ticket.baseCharges + extraFee

            Dialog(onDismissRequest = { selectedTicketForExit = null }) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Confirm Vehicle Exit", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Calculate billing, process final payments and release slot.", style = MaterialTheme.typography.bodySmall)

                        Divider()
                        Text("Base Reservation Cost: $${"%.2f".format(ticket.baseCharges)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Overtime Extra Charges: $${"%.2f".format(extraFee)}", style = MaterialTheme.typography.bodyMedium, color = if (isLate) Color.Red else Color.Black)
                        Text("Grand Total Amount: $${"%.2f".format(grandTotal)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                        Text("Select Exit Payment Method", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { selectedExitPaymentMethod = "WALLET" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedExitPaymentMethod == "WALLET") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedExitPaymentMethod == "WALLET") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Wallet Deduct")
                            }
                            Button(
                                onClick = { selectedExitPaymentMethod = "CASH" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedExitPaymentMethod == "CASH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedExitPaymentMethod == "CASH") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Cash/UPI")
                            }
                        }

                        if (exitMessage != null) {
                            Text(exitMessage!!, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { selectedTicketForExit = null }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                            Button(
                                onClick = {
                                    viewModel.checkOutTicket(
                                        ticket = ticket,
                                        extraCharges = extraFee,
                                        paymentMethod = selectedExitPaymentMethod,
                                        onSuccess = {
                                            exitMessage = "Checkout Complete! Slot Released."
                                            selectedTicketForExit = null
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Checkout Vehicle")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== REPORTS TAB ====================
@Composable
fun LandownerReportsTab(viewModel: ParkFlowViewModel, bookings: List<Booking>) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Operations Analytics & Reports", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {}, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, contentDescription = "PDF")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export PDF", fontSize = 11.sp)
            }
            Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Default.Description, contentDescription = "Excel")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Excel", fontSize = 11.sp)
            }
        }

        // Charts
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Peak Occupancy Hours", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Occupied Slots across day slots (Hour scale)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                // Render beautiful bar chart on Canvas
                val values = listOf(30f, 60f, 95f, 80f, 40f)
                val labels = listOf("08:00", "12:00", "16:00", "20:00", "00:00")

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(vertical = 12.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val barWidth = w / 9

                    for (i in 0..4) {
                        val x = (i * (w / 5)) + (w / 10) - (barWidth / 2)
                        val barHeight = (values[i] / 100f) * h
                        val y = h - barHeight

                        drawRect(
                            color = Color(0xFF3B82F6),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    labels.forEach { l ->
                        Text(l, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }

        // Popular Vehicle Distribution
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Popular Vehicle Distribution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                // Pie indicator simulation using Canvas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawArc(
                            color = Color(0xFF3B82F6),
                            startAngle = 0f,
                            sweepAngle = 240f,
                            useCenter = false,
                            style = Stroke(width = 16f)
                        )
                        drawArc(
                            color = Color(0xFF10B981),
                            startAngle = 240f,
                            sweepAngle = 80f,
                            useCenter = false,
                            style = Stroke(width = 16f)
                        )
                        drawArc(
                            color = Color(0xFFF59E0B),
                            startAngle = 320f,
                            sweepAngle = 40f,
                            useCenter = false,
                            style = Stroke(width = 16f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LegendRow(Color(0xFF3B82F6), "Cars & SUVs (65%)")
                        LegendRow(Color(0xFF10B981), "Motorbikes (25%)")
                        LegendRow(Color(0xFFF59E0B), "Trucks & EV (10%)")
                    }
                }
            }
        }
    }
}

@Composable
fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ==================== REVIEWS TAB ====================
@Composable
fun LandownerReviewsTab(viewModel: ParkFlowViewModel) {
    val reviews by viewModel.landownerReviews.collectAsState()

    if (reviews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reviews received yet.", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(reviews) { rev ->
            var replyText by remember { mutableStateOf("") }
            var showReplyBox by remember { mutableStateOf(rev.reply.isEmpty()) }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(rev.parkerName, fontWeight = FontWeight.Bold)
                        Row {
                            (1..rev.rating).forEach {
                                Icon(Icons.Default.Star, contentDescription = "*", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Text(rev.comment, style = MaterialTheme.typography.bodyMedium)

                    if (rev.reply.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Your Reply:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(rev.reply, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (showReplyBox) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Write response to customer...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (replyText.isNotEmpty()) {
                                    viewModel.replyToReview(rev, replyText)
                                    showReplyBox = false
                                }
                            },
                            enabled = replyText.isNotEmpty()
                        ) {
                            Text("Post Reply")
                        }
                    } else if (rev.reply.isEmpty()) {
                        TextButton(onClick = { showReplyBox = true }) {
                            Text("Reply to Review")
                        }
                    }
                }
            }
        }
    }
}

// ==================== SETTINGS TAB ====================
@Composable
fun LandownerSettingsTab(viewModel: ParkFlowViewModel, user: User) {
    var bankAccount by remember { mutableStateOf("SIM-1234-BANK") }
    var gstNumber by remember { mutableStateOf("27AAAFG4213B1Z2") }
    var companyName by remember { mutableStateOf("ParkFlow Estates LLC") }

    var saveMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Landowner Host Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Business Profile Info", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = gstNumber, onValueChange = { gstNumber = it }, label = { Text("GST/Tax Registration Number") }, modifier = Modifier.fillMaxWidth())

                Divider()
                Text("Settlement Bank Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(value = bankAccount, onValueChange = { bankAccount = it }, label = { Text("Payout Account Details (IBAN/Routing)") }, modifier = Modifier.fillMaxWidth())

                if (saveMessage != null) {
                    Text(saveMessage!!, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        saveMessage = "Business settings saved successfully!"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Configuration")
                }
            }
        }
    }
}

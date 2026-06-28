package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.PremiumBackground

@Composable
fun LandingScreen(
    onNavigateToAuth: () -> Unit
) {
    val scrollState = rememberScrollState()

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalParking,
                        contentDescription = "ParkFlow Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ParkFlow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Button(
                    onClick = onNavigateToAuth,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Launch App", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hero Section
            Text(
                text = "Smart Parking\nManagement System",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Effortless parking reservations and landowner earnings in one beautifully automated ecosystem.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Graphic/Canvas Indicator
            ParkingCanvasIllustration()

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToAuth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Get Started Now", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Get Started")
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // How It Works
            SectionTitle(title = "How It Works")
            HowItWorksSection()

            Spacer(modifier = Modifier.height(48.dp))

            // Features Grid
            SectionTitle(title = "Key Features")
            FeaturesSection()

            Spacer(modifier = Modifier.height(48.dp))

            // Pricing Model
            SectionTitle(title = "Transparent Pricing")
            PricingSection()

            Spacer(modifier = Modifier.height(48.dp))

            // Testimonials
            SectionTitle(title = "User Success Stories")
            TestimonialsSection()

            Spacer(modifier = Modifier.height(48.dp))

            // FAQ
            SectionTitle(title = "Frequently Asked Questions")
            FaqSection()
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun ParkingCanvasIllustration() {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val gridColor = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
    val pColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 32.dp)
    ) {
        val width = size.width
        val height = size.height

        // Draw parking lines
        val startY = 20.dp.toPx()
        val endY = height - 20.dp.toPx()
        val columnWidth = width / 4

        for (i in 0..4) {
            val x = i * columnWidth
            drawLine(
                color = gridColor,
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        // Draw slots labels & status circles
        for (i in 0..3) {
            val cx = (i * columnWidth) + (columnWidth / 2)
            val cy = height / 2

            // Draw Slot indicators
            val statusColor = when (i) {
                0 -> Color(0xFF10B981) // Available - Emerald
                1 -> Color(0xFFEF4444) // Occupied - Red
                2 -> Color(0xFF3B82F6) // Reserved - Blue
                else -> Color(0xFF10B981)
            }

            drawCircle(
                color = statusColor.copy(alpha = 0.2f),
                radius = 32.dp.toPx(),
                center = Offset(cx, cy)
            )

            drawCircle(
                color = statusColor,
                radius = 8.dp.toPx(),
                center = Offset(cx, cy)
            )

            // Draw "P1", "P2" text helper markings
            drawLine(
                color = pColor.copy(alpha = 0.4f),
                start = Offset(cx - columnWidth / 3, startY + 15f),
                end = Offset(cx + columnWidth / 3, startY + 15f),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun HowItWorksSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val steps = listOf(
            Triple(Icons.Default.Search, "1. Search Nearest Parking", "Enter destination town or PIN. Filter by vehicle, covered bay, security, and electric charging."),
            Triple(Icons.Default.CreditCard, "2. Instantly Reserve & Pay", "Choose dates and duration, select vehicle, and make a fast, secure payment via your Wallet or Card."),
            Triple(Icons.Default.QrCodeScanner, "3. Scan and Park", "Show entry QR to the landowner. Smart ticket starts, parks safely, and finishes effortlessly.")
        )

        steps.forEach { step ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = step.first,
                        contentDescription = step.second,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(step.second, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.third, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val features = listOf(
            Pair("Automated Slot Allocation", "Get optimal slots matching your vehicle dimensions automatically upon arrival scan."),
            Pair("Interactive Map Engine", "Track exact parking distance and coordinates with direct route lists."),
            Pair("Landowner Revenue Analytics", "Professional income breakdowns, occupancy charts, and weekly reviews reports."),
            Pair("Smart Billing & Excess Tracking", "Avoid disputes. Auto-calculates base pricing and excess duration fees instantly.")
        )

        features.forEach { feature ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Feature Icon",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(feature.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(feature.second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun PricingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val pricingPlans = listOf(
            Triple(Icons.Default.TwoWheeler, "Two-Wheelers (Bikes)", "$5 - $8 / hour"),
            Triple(Icons.Default.DirectionsCar, "Sedans & Hatchbacks", "$10 - $15 / hour"),
            Triple(Icons.Default.AirportShuttle, "SUVs & Minivans", "$18 - $25 / hour"),
            Triple(Icons.Default.LocalShipping, "Heavy Trucks", "$30 - $45 / hour")
        )

        pricingPlans.forEach { plan ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(plan.first, contentDescription = plan.second, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(plan.second, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(plan.third, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun TestimonialsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val feedbacks = listOf(
            Triple("Sarah Connor", "Parker / commuter", "\"Finding a parking in Manhattan used to take me 30 minutes. With ParkFlow, I reserve a secure covered spot in 5 seconds and just drive in!\""),
            Triple("Mark Zuckerberg", "Landowner", "\"Listed my basement office slots during weekends. Easy setup, auto assigned slots, and weekly direct payouts. Made $1,200 this month!\"")
        )

        feedbacks.forEach { feedback ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(feedback.third, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "User", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(feedback.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(feedback.second, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaqSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val faqs = listOf(
            Pair("Is my vehicle secure?", "Yes, hosts list CCTV availability, professional security guard indicators, and insurance backup markers to ensure high-grade parking."),
            Pair("What happens if I delay checkout?", "ParkFlow calculates late fees automatically based on active elapsed timers. You can check out smoothly by making a micro wallet-payment."),
            Pair("How does a landowner auto-generate slots?", "Simply check 'create slots automatically' inside add-parking page. The app creates standard bike/car slots and starts listing immediately.")
        )

        faqs.forEach { faq ->
            var expanded by remember { mutableStateOf(false) }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = faq.first,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand FAQ"
                        )
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(animationSpec = tween(200)),
                        exit = shrinkVertically(animationSpec = tween(200))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = faq.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

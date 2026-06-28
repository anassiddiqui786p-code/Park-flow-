package com.example.repository

import android.content.Context
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ParkFlowRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "parkflow_database"
    ).build()

    val dao = db.parkFlowDao()

    suspend fun prepulateDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        // Check if users exist, if not, pre-populate
        val existingParker = dao.getUserByEmail("parker@example.com")
        if (existingParker == null) {
            // 1. Insert default Parker
            val parkerId = dao.insertUser(
                User(
                    name = "John Parker",
                    email = "parker@example.com",
                    mobile = "9876543210",
                    passwordHash = "password123", // Simple hash for mock/simulation
                    role = "PARKER",
                    walletBalance = 450.0,
                    emergencyContact = "999-888-7777",
                    address = "123 Main St, New York, NY 10001"
                )
            )

            // 2. Insert default Landowner
            val ownerId = dao.insertUser(
                User(
                    name = "Jane Landowner",
                    email = "owner@example.com",
                    mobile = "1234567890",
                    passwordHash = "password123",
                    role = "LANDOWNER",
                    walletBalance = 1250.0,
                    address = "456 Broadway, New York, NY 10013"
                )
            )

            // 3. Insert Vehicles for Parker
            val vehicleCarId = dao.insertVehicle(
                Vehicle(
                    userId = parkerId,
                    licensePlate = "NY-PARK-42",
                    type = "CAR",
                    nickname = "My Tesla Model 3"
                )
            )
            val vehicleBikeId = dao.insertVehicle(
                Vehicle(
                    userId = parkerId,
                    licensePlate = "NY-RIDER-7",
                    type = "BIKE",
                    nickname = "My Yamaha R6"
                )
            )

            // 4. Insert Parking Spaces for Landowner
            val space1Id = dao.insertParkingSpace(
                ParkingSpace(
                    ownerId = ownerId,
                    name = "Metro Glass Plaza Parking",
                    description = "Premium covered parking garage with 24x7 security, EV charging, and automated QR scanning entry.",
                    address = "Downtown Manhattan, New York (PIN: 10001)",
                    latitude = 40.758896,
                    longitude = -73.985130,
                    isCovered = true,
                    openingTime = "00:00",
                    closingTime = "23:59",
                    hasCCTV = true,
                    hasSecurity = true,
                    hasWashroom = true,
                    hasCharging = true,
                    hasShade = true,
                    hasInsurance = true,
                    bikePrice = 8.0,
                    carPrice = 15.0,
                    suvPrice = 25.0,
                    truckPrice = 40.0,
                    status = "ACTIVE"
                )
            )

            val space2Id = dao.insertParkingSpace(
                ParkingSpace(
                    ownerId = ownerId,
                    name = "Broadway Open Lot",
                    description = "Economical open parking space near central square. Easy in-and-out, CCTV monitored.",
                    address = "SOHO Broadway, New York (PIN: 10013)",
                    latitude = 40.722421,
                    longitude = -73.999653,
                    isCovered = false,
                    openingTime = "06:00",
                    closingTime = "22:00",
                    hasCCTV = true,
                    hasSecurity = false,
                    hasWashroom = false,
                    hasCharging = false,
                    hasShade = false,
                    hasInsurance = false,
                    bikePrice = 5.0,
                    carPrice = 10.0,
                    suvPrice = 18.0,
                    truckPrice = 30.0,
                    status = "ACTIVE"
                )
            )

            // 5. Pre-create Slots for Parking Spaces
            // Space 1 Slots (Premium)
            val space1Slots = listOf(
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "B1", vehicleType = "BIKE", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "B2", vehicleType = "BIKE", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "C1", vehicleType = "CAR", status = "OCCUPIED"),
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "C2", vehicleType = "CAR", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "C3", vehicleType = "CAR", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "S1", vehicleType = "SUV", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "S2", vehicleType = "SUV", status = "RESERVED"),
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "T1", vehicleType = "TRUCK", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space1Id, slotNumber = "E1", vehicleType = "ELECTRIC", status = "AVAILABLE")
            )
            dao.insertSlots(space1Slots)

            // Space 2 Slots (Open)
            val space2Slots = listOf(
                ParkingSlot(parkingSpaceId = space2Id, slotNumber = "B1", vehicleType = "BIKE", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space2Id, slotNumber = "C1", vehicleType = "CAR", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space2Id, slotNumber = "C2", vehicleType = "CAR", status = "AVAILABLE"),
                ParkingSlot(parkingSpaceId = space2Id, slotNumber = "S1", vehicleType = "SUV", status = "AVAILABLE")
            )
            dao.insertSlots(space2Slots)

            // 6. Pre-populate Some Bookings (Completed, Active, Upcoming)
            val bookingCompleted = Booking(
                parkingSpaceId = space1Id,
                parkerId = parkerId,
                vehicleId = vehicleCarId,
                date = "2026-06-25",
                time = "10:00",
                durationHours = 3,
                totalAmount = 45.0,
                status = "COMPLETED",
                qrCodeData = "BK-GLASS-COMPLETED-42"
            )
            val completedBookingId = dao.insertBooking(bookingCompleted)
            dao.insertPayment(Payment(bookingId = completedBookingId, amount = 45.0, method = "WALLET", status = "SUCCESS"))

            val bookingActive = Booking(
                parkingSpaceId = space1Id,
                parkerId = parkerId,
                vehicleId = vehicleCarId,
                date = "2026-06-28",
                time = "11:00",
                durationHours = 2,
                totalAmount = 30.0,
                status = "ACTIVE",
                qrCodeData = "BK-GLASS-ACTIVE-77"
            )
            val activeBookingId = dao.insertBooking(bookingActive)
            dao.insertPayment(Payment(bookingId = activeBookingId, amount = 30.0, method = "WALLET", status = "SUCCESS"))

            // Associate Active Ticket
            val activeSlot = dao.getSlotsByParkingSpace(space1Id).find { it.slotNumber == "C1" }
            if (activeSlot != null) {
                dao.insertTicket(
                    Ticket(
                        bookingId = activeBookingId,
                        slotId = activeSlot.id,
                        entryTime = System.currentTimeMillis() - (45 * 60 * 1000), // entered 45 mins ago
                        expectedExitTime = System.currentTimeMillis() + (75 * 60 * 1000),
                        baseCharges = 30.0,
                        status = "ACTIVE"
                    )
                )
            }

            val bookingUpcoming = Booking(
                parkingSpaceId = space2Id,
                parkerId = parkerId,
                vehicleId = vehicleBikeId,
                date = "2026-06-29",
                time = "14:00",
                durationHours = 2,
                totalAmount = 10.0,
                status = "CONFIRMED",
                qrCodeData = "BK-BROADWAY-UPCOMING-10"
            )
            val upcomingBookingId = dao.insertBooking(bookingUpcoming)
            dao.insertPayment(Payment(bookingId = upcomingBookingId, amount = 10.0, method = "CARD", status = "SUCCESS"))

            // 7. Reviews
            dao.insertReview(
                Review(
                    parkingSpaceId = space1Id,
                    parkerId = parkerId,
                    parkerName = "John Parker",
                    rating = 5,
                    comment = "Incredible smart setup! The QR entry was flawless and slots are very spacious.",
                    reply = "Thank you John! We strive to offer the best automated parking experience."
                )
            )
            dao.insertReview(
                Review(
                    parkingSpaceId = space1Id,
                    parkerId = parkerId,
                    parkerName = "Sarah Connor",
                    rating = 4,
                    comment = "Super safe. Very convenient location.",
                    reply = ""
                )
            )

            // 8. Notifications
            dao.insertNotification(
                Notification(
                    userId = parkerId,
                    title = "Welcome to ParkFlow!",
                    message = "Your account has been successfully created. We have credited $500 simulation funds to your wallet!"
                )
            )
            dao.insertNotification(
                Notification(
                    userId = ownerId,
                    title = "Welcome Landowner!",
                    message = "List your parking spaces and start earning daily automated revenue!"
                )
            )
        }
    }
}

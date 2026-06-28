package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val mobile: String,
    val passwordHash: String,
    val role: String, // "PARKER" or "LANDOWNER"
    val walletBalance: Double = 500.0, // Default wallet balance for simulation
    val emergencyContact: String = "",
    val address: String = "",
    val profilePhoto: String = "" // Base64 or placeholder URI
)

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val licensePlate: String,
    val type: String, // "BIKE", "CAR", "SUV", "TRUCK", "ELECTRIC"
    val nickname: String
)

@Entity(tableName = "parking_spaces")
data class ParkingSpace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: Long,
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isCovered: Boolean,
    val openingTime: String = "00:00",
    val closingTime: String = "23:59",
    val hasCCTV: Boolean = false,
    val hasSecurity: Boolean = false,
    val hasWashroom: Boolean = false,
    val hasCharging: Boolean = false,
    val hasShade: Boolean = false,
    val hasInsurance: Boolean = false,
    val bikePrice: Double = 10.0,
    val carPrice: Double = 20.0,
    val suvPrice: Double = 30.0,
    val truckPrice: Double = 50.0,
    val status: String = "ACTIVE" // "ACTIVE", "PAUSED"
)

@Entity(tableName = "parking_slots")
data class ParkingSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parkingSpaceId: Long,
    val slotNumber: String, // e.g. "B1", "C1", "S1"
    val vehicleType: String, // "BIKE", "CAR", "SUV", "TRUCK", "ELECTRIC"
    val status: String = "AVAILABLE" // "AVAILABLE", "OCCUPIED", "RESERVED", "MAINTENANCE"
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parkingSpaceId: Long,
    val parkerId: Long,
    val vehicleId: Long,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:MM
    val durationHours: Int,
    val totalAmount: Double,
    val status: String, // "PENDING", "CONFIRMED", "REJECTED", "ACTIVE", "COMPLETED", "CANCELLED"
    val qrCodeData: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val amount: Double,
    val method: String, // "UPI", "CARD", "WALLET", "CASH"
    val status: String, // "SUCCESS", "REFUNDED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val slotId: Long,
    val entryTime: Long, // timestamp
    val expectedExitTime: Long, // timestamp
    val actualExitTime: Long? = null, // timestamp
    val baseCharges: Double,
    val extraCharges: Double = 0.0,
    val status: String = "ACTIVE" // "ACTIVE", "COMPLETED"
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parkingSpaceId: Long,
    val parkerId: Long,
    val parkerName: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val reply: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parkerId: Long,
    val parkingSpaceId: Long
)

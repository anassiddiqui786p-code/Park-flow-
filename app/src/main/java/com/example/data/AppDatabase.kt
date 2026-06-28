package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Vehicle::class,
        ParkingSpace::class,
        ParkingSlot::class,
        Booking::class,
        Payment::class,
        Ticket::class,
        Review::class,
        Notification::class,
        Favorite::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkFlowDao(): ParkFlowDao
}

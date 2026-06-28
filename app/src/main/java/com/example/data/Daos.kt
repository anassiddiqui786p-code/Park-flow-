package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkFlowDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByIdFlow(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    // --- Vehicles ---
    @Query("SELECT * FROM vehicles WHERE userId = :userId")
    fun getVehiclesByUserIdFlow(userId: Long): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE userId = :userId")
    suspend fun getVehiclesByUserId(userId: Long): List<Vehicle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    // --- Parking Spaces ---
    @Query("SELECT * FROM parking_spaces WHERE status = 'ACTIVE'")
    fun getActiveParkingSpacesFlow(): Flow<List<ParkingSpace>>

    @Query("SELECT * FROM parking_spaces WHERE ownerId = :ownerId")
    fun getParkingSpacesByOwnerFlow(ownerId: Long): Flow<List<ParkingSpace>>

    @Query("SELECT * FROM parking_spaces WHERE id = :id")
    suspend fun getParkingSpaceById(id: Long): ParkingSpace?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParkingSpace(space: ParkingSpace): Long

    @Update
    suspend fun updateParkingSpace(space: ParkingSpace)

    @Delete
    suspend fun deleteParkingSpace(space: ParkingSpace)

    // --- Parking Slots ---
    @Query("SELECT * FROM parking_slots WHERE parkingSpaceId = :spaceId")
    fun getSlotsByParkingSpaceFlow(spaceId: Long): Flow<List<ParkingSlot>>

    @Query("SELECT * FROM parking_slots WHERE parkingSpaceId = :spaceId")
    suspend fun getSlotsByParkingSpace(spaceId: Long): List<ParkingSlot>

    @Query("SELECT * FROM parking_slots WHERE id = :slotId")
    suspend fun getSlotById(slotId: Long): ParkingSlot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: ParkingSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<ParkingSlot>)

    @Update
    suspend fun updateSlot(slot: ParkingSlot)

    @Query("DELETE FROM parking_slots WHERE parkingSpaceId = :spaceId")
    suspend fun deleteSlotsForSpace(spaceId: Long)

    // --- Bookings ---
    @Query("SELECT * FROM bookings WHERE parkerId = :parkerId ORDER BY timestamp DESC")
    fun getBookingsByParkerFlow(parkerId: Long): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE parkingSpaceId IN (SELECT id FROM parking_spaces WHERE ownerId = :ownerId) ORDER BY timestamp DESC")
    fun getBookingsByOwnerFlow(ownerId: Long): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: Long): Booking?

    @Query("SELECT * FROM bookings WHERE qrCodeData = :qrCodeData LIMIT 1")
    suspend fun getBookingByQrCode(qrCodeData: String): Booking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Update
    suspend fun updateBooking(booking: Booking)

    // --- Payments ---
    @Query("SELECT * FROM payments WHERE bookingId = :bookingId")
    suspend fun getPaymentsForBooking(bookingId: Long): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    // --- Tickets ---
    @Query("SELECT * FROM tickets WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getTicketForBooking(bookingId: Long): Ticket?

    @Query("SELECT * FROM tickets WHERE id = :ticketId")
    suspend fun getTicketById(ticketId: Long): Ticket?

    @Query("SELECT * FROM tickets WHERE status = 'ACTIVE' AND bookingId IN (SELECT id FROM bookings WHERE parkingSpaceId IN (SELECT id FROM parking_spaces WHERE ownerId = :ownerId))")
    fun getActiveTicketsForOwnerFlow(ownerId: Long): Flow<List<Ticket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket): Long

    @Update
    suspend fun updateTicket(ticket: Ticket)

    // --- Reviews ---
    @Query("SELECT * FROM reviews WHERE parkingSpaceId = :spaceId ORDER BY timestamp DESC")
    fun getReviewsForSpaceFlow(spaceId: Long): Flow<List<Review>>

    @Query("SELECT * FROM reviews WHERE parkingSpaceId = :spaceId ORDER BY timestamp DESC")
    suspend fun getReviewsForSpace(spaceId: Long): List<Review>

    @Query("SELECT * FROM reviews WHERE parkingSpaceId IN (SELECT id FROM parking_spaces WHERE ownerId = :ownerId) ORDER BY timestamp DESC")
    fun getReviewsForOwnerFlow(ownerId: Long): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review): Long

    @Update
    suspend fun updateReview(review: Review)

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsFlow(userId: Long): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: Long)

    // --- Favorites ---
    @Query("SELECT parkingSpaceId FROM favorites WHERE parkerId = :parkerId")
    fun getFavoriteSpaceIdsFlow(parkerId: Long): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE parkerId = :parkerId AND parkingSpaceId = :spaceId)")
    fun isFavoriteFlow(parkerId: Long, spaceId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite): Long

    @Query("DELETE FROM favorites WHERE parkerId = :parkerId AND parkingSpaceId = :spaceId")
    suspend fun deleteFavorite(parkerId: Long, spaceId: Long)
}

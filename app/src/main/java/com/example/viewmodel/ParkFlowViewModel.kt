package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.ParkFlowRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ParkFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParkFlowRepository(application)
    val dao = repository.dao

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _signupSuccess = MutableStateFlow(false)
    val signupSuccess: StateFlow<Boolean> = _signupSuccess.asStateFlow()

    // --- Active States for Dashboards ---
    // User lists & notifications (reactive to current user id)
    val userVehicles: StateFlow<List<Vehicle>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> dao.getVehiclesByUserIdFlow(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userBookings: StateFlow<List<Booking>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            if (user.role == "PARKER") {
                dao.getBookingsByParkerFlow(user.id)
            } else {
                dao.getBookingsByOwnerFlow(user.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotifications: StateFlow<List<Notification>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> dao.getNotificationsFlow(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSpaceIds: StateFlow<List<Long>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> dao.getFavoriteSpaceIdsFlow(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Parking Spaces List ---
    val allActiveParkingSpaces: StateFlow<List<ParkingSpace>> = dao.getActiveParkingSpacesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val landownerParkingSpaces: StateFlow<List<ParkingSpace>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> dao.getParkingSpacesByOwnerFlow(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Selected Space, Slot, and Review Flows ---
    private val _selectedSpaceId = MutableStateFlow<Long?>(null)
    val selectedSpaceId: StateFlow<Long?> = _selectedSpaceId.asStateFlow()

    val selectedSpaceSlots: StateFlow<List<ParkingSlot>> = _selectedSpaceId
        .filterNotNull()
        .flatMapLatest { spaceId -> dao.getSlotsByParkingSpaceFlow(spaceId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedSpaceReviews: StateFlow<List<Review>> = _selectedSpaceId
        .filterNotNull()
        .flatMapLatest { spaceId -> dao.getReviewsForSpaceFlow(spaceId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val landownerActiveTickets: StateFlow<List<Ticket>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> dao.getActiveTicketsForOwnerFlow(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val landownerReviews: StateFlow<List<Review>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> dao.getReviewsForOwnerFlow(user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filter States ---
    val searchQuery = MutableStateFlow("")
    val filterCoveredOnly = MutableStateFlow(false)
    val filterHasCharging = MutableStateFlow(false)
    val filterHasSecurity = MutableStateFlow(false)
    val filterVehicleType = MutableStateFlow("ANY") // "ANY", "BIKE", "CAR", "SUV", "TRUCK"
    val sortByPrice = MutableStateFlow(false) // false = nearest/default, true = low to high

    init {
        viewModelScope.launch {
            repository.prepulateDatabaseIfEmpty()
        }
    }

    // --- Auth Actions ---
    fun login(emailOrPhone: String, passwordText: String) {
        viewModelScope.launch {
            _loginError.value = null
            val user = if (emailOrPhone.contains("@")) {
                dao.getUserByEmail(emailOrPhone.trim())
            } else {
                dao.getUserByMobile(emailOrPhone.trim())
            }

            if (user != null && user.passwordHash == passwordText) {
                _currentUser.value = user
            } else {
                _loginError.value = "Invalid email/mobile or password."
            }
        }
    }

    fun signup(name: String, email: String, mobile: String, passwordText: String, role: String) {
        viewModelScope.launch {
            _loginError.value = null
            _signupSuccess.value = false

            val existingEmail = dao.getUserByEmail(email.trim())
            val existingMobile = dao.getUserByMobile(mobile.trim())

            if (existingEmail != null) {
                _loginError.value = "Email is already registered."
                return@launch
            }
            if (existingMobile != null) {
                _loginError.value = "Mobile number is already registered."
                return@launch
            }

            val newUser = User(
                name = name,
                email = email.trim(),
                mobile = mobile.trim(),
                passwordHash = passwordText,
                role = role,
                walletBalance = 500.0 // Starter balance
            )

            val newId = dao.insertUser(newUser)
            dao.insertNotification(
                Notification(
                    userId = newId,
                    title = "Welcome to ParkFlow!",
                    message = "Thank you for joining ParkFlow as a $role. Explore your dashboard and manage parkings."
                )
            )

            _signupSuccess.value = true
        }
    }

    fun logout() {
        _currentUser.value = null
        _signupSuccess.value = false
    }

    fun resetSignupSuccess() {
        _signupSuccess.value = false
    }

    // --- Parker Actions ---
    fun addWalletFunds(amount: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(walletBalance = user.walletBalance + amount)
            dao.updateUser(updatedUser)
            _currentUser.value = updatedUser

            dao.insertNotification(
                Notification(
                    userId = user.id,
                    title = "Funds Added Successfully",
                    message = "$${"%.2f".format(amount)} has been added to your ParkFlow Wallet. New balance is $${"%.2f".format(updatedUser.walletBalance)}."
                )
            )
        }
    }

    fun addVehicle(licensePlate: String, type: String, nickname: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            dao.insertVehicle(
                Vehicle(
                    userId = user.id,
                    licensePlate = licensePlate.uppercase(),
                    type = type,
                    nickname = nickname
                )
            )
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            dao.deleteVehicle(vehicle)
        }
    }

    fun selectSpace(spaceId: Long?) {
        _selectedSpaceId.value = spaceId
    }

    fun toggleFavorite(spaceId: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val isFav = favoriteSpaceIds.value.contains(spaceId)
            if (isFav) {
                dao.deleteFavorite(user.id, spaceId)
            } else {
                dao.insertFavorite(Favorite(parkerId = user.id, parkingSpaceId = spaceId))
            }
        }
    }

    fun createBooking(
        space: ParkingSpace,
        vehicle: Vehicle,
        date: String,
        time: String,
        duration: Int,
        amount: Double,
        paymentMethod: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (paymentMethod == "WALLET" && user.walletBalance < amount) {
                onError("Insufficient wallet balance. Please top up.")
                return@launch
            }

            // Create booking
            val qrData = "PF-BK-${System.currentTimeMillis()}-${(1000..9999).random()}"
            val newBooking = Booking(
                parkingSpaceId = space.id,
                parkerId = user.id,
                vehicleId = vehicle.id,
                date = date,
                time = time,
                durationHours = duration,
                totalAmount = amount,
                status = "PENDING", // Landowner accepts
                qrCodeData = qrData
            )

            val bookingId = dao.insertBooking(newBooking)

            // Process payment
            dao.insertPayment(
                Payment(
                    bookingId = bookingId,
                    amount = amount,
                    method = paymentMethod,
                    status = "SUCCESS"
                )
            )

            // Deduct wallet if wallet selected
            if (paymentMethod == "WALLET") {
                val updatedUser = user.copy(walletBalance = user.walletBalance - amount)
                dao.updateUser(updatedUser)
                _currentUser.value = updatedUser
            }

            // Create notification for parker
            dao.insertNotification(
                Notification(
                    userId = user.id,
                    title = "Booking Placed Successfully",
                    message = "Your booking for ${space.name} is currently PENDING landowner acceptance."
                )
            )

            // Create notification for landowner
            dao.insertNotification(
                Notification(
                    userId = space.ownerId,
                    title = "New Booking Request",
                    message = "A new booking request has been received for ${space.name} from ${user.name}."
                )
            )

            onSuccess()
        }
    }

    fun cancelBooking(booking: Booking) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            dao.updateBooking(booking.copy(status = "CANCELLED"))

            // Refund if paid via wallet
            val payments = dao.getPaymentsForBooking(booking.id)
            val walletPayment = payments.find { it.method == "WALLET" && it.status == "SUCCESS" }
            if (walletPayment != null) {
                // Refund
                dao.updatePayment(walletPayment.copy(status = "REFUNDED"))
                val updatedUser = user.copy(walletBalance = user.walletBalance + booking.totalAmount)
                dao.updateUser(updatedUser)
                _currentUser.value = updatedUser

                dao.insertNotification(
                    Notification(
                        userId = user.id,
                        title = "Booking Refund Processed",
                        message = "Your wallet has been refunded $${"%.2f".format(booking.totalAmount)} for cancelled booking."
                    )
                )
            } else {
                dao.insertNotification(
                    Notification(
                        userId = user.id,
                        title = "Booking Cancelled",
                        message = "Your booking has been cancelled successfully."
                    )
                )
            }

            // Notify space owner
            val space = dao.getParkingSpaceById(booking.parkingSpaceId)
            if (space != null) {
                dao.insertNotification(
                    Notification(
                        userId = space.ownerId,
                        title = "Booking Cancelled by Customer",
                        message = "Booking ID #${booking.id} for ${space.name} was cancelled."
                    )
                )
            }
        }
    }

    fun postReview(spaceId: Long, rating: Int, comment: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            dao.insertReview(
                Review(
                    parkingSpaceId = spaceId,
                    parkerId = user.id,
                    parkerName = user.name,
                    rating = rating,
                    comment = comment
                )
            )
        }
    }

    // --- Landowner Actions ---
    fun addParkingSpace(
        name: String,
        description: String,
        address: String,
        latitude: Double,
        longitude: Double,
        isCovered: Boolean,
        openingTime: String,
        closingTime: String,
        hasCCTV: Boolean,
        hasSecurity: Boolean,
        hasWashroom: Boolean,
        hasCharging: Boolean,
        hasShade: Boolean,
        hasInsurance: Boolean,
        bikePrice: Double,
        carPrice: Double,
        suvPrice: Double,
        truckPrice: Double
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val spaceId = dao.insertParkingSpace(
                ParkingSpace(
                    ownerId = user.id,
                    name = name,
                    description = description,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    isCovered = isCovered,
                    openingTime = openingTime,
                    closingTime = closingTime,
                    hasCCTV = hasCCTV,
                    hasSecurity = hasSecurity,
                    hasWashroom = hasWashroom,
                    hasCharging = hasCharging,
                    hasShade = hasShade,
                    hasInsurance = hasInsurance,
                    bikePrice = bikePrice,
                    carPrice = carPrice,
                    suvPrice = suvPrice,
                    truckPrice = truckPrice,
                    status = "ACTIVE"
                )
            )

            // Auto-create Slots (e.g. 2 Bike, 3 Car, 2 SUV, 1 Truck)
            val autoSlots = listOf(
                ParkingSlot(parkingSpaceId = spaceId, slotNumber = "B1", vehicleType = "BIKE"),
                ParkingSlot(parkingSpaceId = spaceId, slotNumber = "B2", vehicleType = "BIKE"),
                ParkingSlot(parkingSpaceId = spaceId, slotNumber = "C1", vehicleType = "CAR"),
                ParkingSlot(parkingSpaceId = spaceId, slotNumber = "C2", vehicleType = "CAR"),
                ParkingSlot(parkingSpaceId = spaceId, slotNumber = "C3", vehicleType = "CAR"),
                ParkingSlot(parkingSpaceId = spaceId, slotNumber = "S1", vehicleType = "SUV"),
                ParkingSlot(parkingSpaceId = spaceId, slotNumber = "S2", vehicleType = "SUV"),
                ParkingSlot(parkingSpaceId = spaceId, slotNumber = "T1", vehicleType = "TRUCK")
            )
            dao.insertSlots(autoSlots)

            dao.insertNotification(
                Notification(
                    userId = user.id,
                    title = "Parking Space Listed",
                    message = "${name} is now listed and slots B1-B2, C1-C3, S1-S2, T1 are auto-generated."
                )
            )
        }
    }

    fun updateParkingSpaceStatus(space: ParkingSpace, newStatus: String) {
        viewModelScope.launch {
            dao.updateParkingSpace(space.copy(status = newStatus))
        }
    }

    fun deleteParkingSpace(space: ParkingSpace) {
        viewModelScope.launch {
            dao.deleteSlotsForSpace(space.id)
            dao.deleteParkingSpace(space)
        }
    }

    fun createCustomSlot(spaceId: Long, slotNumber: String, vehicleType: String) {
        viewModelScope.launch {
            dao.insertSlot(
                ParkingSlot(
                    parkingSpaceId = spaceId,
                    slotNumber = slotNumber.uppercase(),
                    vehicleType = vehicleType,
                    status = "AVAILABLE"
                )
            )
        }
    }

    fun updateSlotStatus(slot: ParkingSlot, status: String) {
        viewModelScope.launch {
            dao.updateSlot(slot.copy(status = status))
        }
    }

    fun acceptBooking(booking: Booking) {
        viewModelScope.launch {
            dao.updateBooking(booking.copy(status = "CONFIRMED"))
            dao.insertNotification(
                Notification(
                    userId = booking.parkerId,
                    title = "Booking Approved",
                    message = "Your booking ID #${booking.id} has been APPROVED by the host! Open the booking details for entry QR code."
                )
            )
        }
    }

    fun rejectBooking(booking: Booking) {
        viewModelScope.launch {
            dao.updateBooking(booking.copy(status = "REJECTED"))

            // Refund payment
            val payments = dao.getPaymentsForBooking(booking.id)
            val walletPayment = payments.find { it.method == "WALLET" && it.status == "SUCCESS" }
            if (walletPayment != null) {
                dao.updatePayment(walletPayment.copy(status = "REFUNDED"))
                val parker = dao.getUserById(booking.parkerId)
                if (parker != null) {
                    dao.updateUser(parker.copy(walletBalance = parker.walletBalance + booking.totalAmount))
                }
            }

            dao.insertNotification(
                Notification(
                    userId = booking.parkerId,
                    title = "Booking Declined",
                    message = "Your booking ID #${booking.id} was declined by the landowner. Any amount paid has been refunded to your wallet."
                )
            )
        }
    }

    // Allow Entry Check-in
    fun checkInBooking(booking: Booking, slot: ParkingSlot, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (slot.status != "AVAILABLE") {
                onError("Slot ${slot.slotNumber} is not available.")
                return@launch
            }

            // Update Booking status to ACTIVE
            dao.updateBooking(booking.copy(status = "ACTIVE"))

            // Update Slot status to OCCUPIED
            dao.updateSlot(slot.copy(status = "OCCUPIED"))

            // Insert Ticket
            val expectedExit = System.currentTimeMillis() + (booking.durationHours * 60 * 60 * 1000)
            val newTicket = Ticket(
                bookingId = booking.id,
                slotId = slot.id,
                entryTime = System.currentTimeMillis(),
                expectedExitTime = expectedExit,
                baseCharges = booking.totalAmount,
                status = "ACTIVE"
            )
            dao.insertTicket(newTicket)

            // Notify Parker
            dao.insertNotification(
                Notification(
                    userId = booking.parkerId,
                    title = "Vehicle Parked",
                    message = "Your vehicle is checked in at slot ${slot.slotNumber}. Parking timer has started!"
                )
            )

            onSuccess()
        }
    }

    // Check-out / Exit Parking
    fun checkOutTicket(ticket: Ticket, extraCharges: Double, paymentMethod: String, onSuccess: () -> Unit) {
        val landownerUser = _currentUser.value ?: return
        viewModelScope.launch {
            val booking = dao.getBookingById(ticket.bookingId) ?: return@launch
            val slot = dao.getSlotById(ticket.slotId) ?: return@launch

            // 1. Update ticket to COMPLETED
            val actualExit = System.currentTimeMillis()
            val completedTicket = ticket.copy(
                actualExitTime = actualExit,
                extraCharges = extraCharges,
                status = "COMPLETED"
            )
            dao.updateTicket(completedTicket)

            // 2. Update booking to COMPLETED
            dao.updateBooking(booking.copy(status = "COMPLETED"))

            // 3. Update Slot to AVAILABLE
            dao.updateSlot(slot.copy(status = "AVAILABLE"))

            // 4. Record any extra payments
            if (extraCharges > 0) {
                dao.insertPayment(
                    Payment(
                        bookingId = booking.id,
                        amount = extraCharges,
                        method = paymentMethod,
                        status = "SUCCESS"
                    )
                )
                // If extra charge paid by wallet, deduct from parker wallet
                if (paymentMethod == "WALLET") {
                    val parker = dao.getUserById(booking.parkerId)
                    if (parker != null) {
                        dao.updateUser(parker.copy(walletBalance = parker.walletBalance - extraCharges))
                    }
                }
            }

            // Add revenue to landowner balance (total amount = base + extra)
            val revenueTotal = booking.totalAmount + extraCharges
            val updatedOwner = landownerUser.copy(walletBalance = landownerUser.walletBalance + revenueTotal)
            dao.updateUser(updatedOwner)
            _currentUser.value = updatedOwner

            // Notify parker
            dao.insertNotification(
                Notification(
                    userId = booking.parkerId,
                    title = "Parking Session Ended",
                    message = "Your vehicle has exited slot ${slot.slotNumber}. Total amount paid: $${"%.2f".format(revenueTotal)}."
                )
            )

            // Notify landowner
            dao.insertNotification(
                Notification(
                    userId = landownerUser.id,
                    title = "Revenue Earned",
                    message = "Earnings of $${"%.2f".format(revenueTotal)} credited to your account from checkout of slot ${slot.slotNumber}."
                )
            )

            onSuccess()
        }
    }

    fun replyToReview(review: Review, reply: String) {
        viewModelScope.launch {
            dao.updateReview(review.copy(reply = reply))
        }
    }

    fun updateProfile(name: String, mobile: String, emergencyContact: String, address: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(
                name = name,
                mobile = mobile,
                emergencyContact = emergencyContact,
                address = address
            )
            dao.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }
}

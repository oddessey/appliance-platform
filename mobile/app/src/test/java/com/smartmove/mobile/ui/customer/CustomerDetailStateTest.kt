package com.smartmove.mobile.ui.customer

import com.smartmove.mobile.data.model.Reservation
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class CustomerDetailStateTest {

    @Test
    fun `confirmed reservation is cancellable before technician accepts`() {
        val reservation = reservation()

        assertNotNull(stateWith(reservation).cancellableReservation)
        assertNotNull(stateWith(reservation.copy(technicianAssignmentStatus = "PENDING")).cancellableReservation)
        assertNotNull(stateWith(reservation.copy(technicianAssignmentStatus = "REJECTED")).cancellableReservation)
        assertNotNull(stateWith(reservation.copy(technicianAssignmentStatus = "CANCELLED")).cancellableReservation)
    }

    @Test
    fun `reservation is not cancellable after technician accepts`() {
        ACCEPTED_TECHNICIAN_ASSIGNMENT_STATUSES.forEach { status ->
            val state = stateWith(reservation().copy(technicianAssignmentStatus = status))

            assertNull(state.cancellableReservation)
        }
    }

    @Test
    fun `reservation is not cancellable after start time`() {
        val state = stateWith(
            reservation().copy(
                scheduledStartAt = OffsetDateTime.now().minusHours(1).toString(),
                scheduledEndAt = OffsetDateTime.now().plusHours(1).toString(),
            ),
        )

        assertNull(state.cancellableReservation)
    }

    @Test
    fun `only confirmed reservations are cancellable`() {
        listOf("PENDING", "RESCHEDULE_REQUESTED", "CANCELLED", "EXPIRED", "COMPLETED").forEach { status ->
            val state = stateWith(reservation().copy(status = status))

            assertNull(state.cancellableReservation)
        }
    }

    private fun stateWith(reservation: Reservation): CustomerDetailState =
        CustomerDetailState(reservations = listOf(reservation))

    private fun reservation(): Reservation {
        val startAt = OffsetDateTime.now().plusDays(1)
        return Reservation(
            id = "reservation-1",
            reservationNo = "RSV-1",
            status = "CONFIRMED",
            scheduledStartAt = startAt.toString(),
            scheduledEndAt = startAt.plusHours(4).toString(),
        )
    }
}

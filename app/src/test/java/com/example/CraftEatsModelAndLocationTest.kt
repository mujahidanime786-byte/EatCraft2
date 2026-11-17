package com.example

import com.example.data.LocationHelper
import com.example.model.OrderStatus
import com.example.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CraftEatsModelAndLocationTest {

    @Test
    fun `order status fallback handles unknown strings safely`() {
        assertEquals(OrderStatus.DELIVERED, OrderStatus.fromRaw("DELIVERED"))
        assertEquals(OrderStatus.PLACED, OrderStatus.fromRaw("UNKNOWN_STATE"))
        assertEquals(OrderStatus.PLACED, OrderStatus.fromRaw(null))
    }

    @Test
    fun `haversine distance calculates Bengaluru coordinates accurately`() {
        val km = LocationHelper.haversineKm(12.9784, 77.6408, 12.9352, 77.6245)
        assertTrue("Expected distance ~5.1 km, got $km", km in 4.8..5.5)
    }

    @Test
    fun `user profile defaults match Firestore schema`() {
        val profile = UserProfile()
        assertEquals("CUSTOMER", profile.role)
        assertEquals("ACTIVE", profile.accountStatus)
    }
}

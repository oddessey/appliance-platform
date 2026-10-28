package com.smartmove.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateTimesTest {
    @Test
    fun convertsLocalScheduleToIsoAndBack() {
        val date = LocalDate.now().plusDays(10)
        val time = LocalTime.of(14, 30)

        val iso = DateTimes.toIso(date, time)

        assertEquals(date, DateTimes.parseDate(iso))
        assertEquals(time, DateTimes.parseTime(iso))
        assertTrue(DateTimes.isFuture(iso))
    }

    @Test
    fun rejectsInvalidIsoValue() {
        assertNull(DateTimes.parseDate("not-a-date"))
        assertFalse(DateTimes.isFuture("not-a-date"))
    }

    @Test
    fun parsesLocalTimeWithAndWithoutSeconds() {
        assertEquals(LocalTime.of(9, 5), DateTimes.parseLocalTime("09:05"))
        assertEquals(LocalTime.of(18, 0), DateTimes.parseLocalTime("18:00:00"))
        assertNull(DateTimes.parseLocalTime("25:00"))
    }
}

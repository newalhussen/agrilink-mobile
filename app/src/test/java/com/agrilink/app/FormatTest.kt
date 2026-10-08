package com.agrilink.app

import com.agrilink.app.core.Dates
import com.agrilink.app.core.EthiopianCalendar
import com.agrilink.app.core.Format
import com.agrilink.app.core.Phone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class FormatTest {
    @Test fun `etb shows cents only when there are some`() {
        assertEquals("ETB 21,968", Format.etb(21968.0))
        assertEquals("ETB 2,760.50", Format.etb(2760.5))
        assertEquals("1,200", Format.etb(1200.0, withCurrency = false))
    }

    @Test fun `compact money for tiles`() {
        assertEquals("ETB 1.84M", Format.etbCompact(1_840_000.0))
        assertEquals("ETB 412K", Format.etbCompact(412_000.0))
        assertEquals("ETB 950", Format.etbCompact(950.0))
    }

    @Test fun `weights switch to tonnes from 1000 kg`() {
        assertEquals("400 kg", Format.kg(400.0))
        assertEquals("2.5 kg", Format.kg(2.5))
        assertEquals("1.5 t", Format.kg(1500.0))
    }

    @Test fun `quantities drop trailing zeros`() {
        assertEquals("2.5", Format.number(2.5))
        assertEquals("400", Format.number(400.0))
        assertEquals("0.33", Format.number(1.0 / 3))
    }
}

class DatesTest {
    @Test fun `countdown reads naturally`() {
        assertEquals("now", Dates.countdown(0))
        assertEquals("<1 min", Dates.countdown(30_000))
        assertEquals("5 min", Dates.countdown(5 * 60_000L))
        assertEquals("2:05", Dates.countdown((2 * 60 + 5) * 60_000L))
        assertEquals("1 d 3 h", Dates.countdown(27 * 3600_000L))
    }

    @Test fun `millisUntil is null for missing or invalid dates`() {
        assertEquals(null, Dates.millisUntil(null))
        assertEquals(null, Dates.millisUntil("not a date"))
        val now = Instant.parse("2026-10-07T10:00:00Z")
        assertEquals(3_600_000L, Dates.millisUntil("2026-10-07T11:00:00Z", now))
    }

    @Test fun `ago steps from just now to hours`() {
        val now = Instant.parse("2026-10-07T10:00:00Z")
        assertEquals("just now", Dates.ago("2026-10-07T09:59:40Z", now))
        assertEquals("5 min ago", Dates.ago("2026-10-07T09:55:00Z", now))
        assertEquals("3 h ago", Dates.ago("2026-10-07T07:00:00Z", now))
        assertEquals("—", Dates.ago(null, now))
    }

    @Test fun `times are shown in Addis Ababa time`() {
        assertEquals("13:00", Dates.time("2026-10-07T10:00:00Z"))
    }
}

class EthiopianCalendarTest {
    @Test fun `new year falls on 11 or 12 September`() {
        assertEquals(EthiopianCalendar.EthDate(2016, 1, 1), EthiopianCalendar.from(LocalDate.of(2023, 9, 12)))
        assertEquals(EthiopianCalendar.EthDate(2017, 1, 1), EthiopianCalendar.from(LocalDate.of(2024, 9, 11)))
        assertEquals(EthiopianCalendar.EthDate(2019, 1, 1), EthiopianCalendar.from(LocalDate.of(2026, 9, 11)))
    }

    @Test fun `dates inside the year`() {
        assertEquals(EthiopianCalendar.EthDate(2019, 1, 27), EthiopianCalendar.from(LocalDate.of(2026, 10, 7)))
        assertEquals(EthiopianCalendar.EthDate(2018, 13, 5), EthiopianCalendar.from(LocalDate.of(2026, 9, 10)))
    }

    @Test fun `formats with the Amharic month`() {
        assertEquals("መስከረም 27, 2019", EthiopianCalendar.from(LocalDate.of(2026, 10, 7)).format())
    }
}

class PhoneTest {
    @Test fun `accepts the usual ways of typing an Ethiopian number`() {
        assertTrue(Phone.looksValid("0911 000 001"))
        assertTrue(Phone.looksValid("+251911000001"))
        assertTrue(Phone.looksValid("911000001"))
        assertTrue(Phone.looksValid("0722123456"))
    }

    @Test fun `rejects numbers that cannot be valid`() {
        assertFalse(Phone.looksValid("091100"))
        assertFalse(Phone.looksValid("0811000001"))
        assertFalse(Phone.looksValid(""))
    }

    @Test fun `normalises to international format`() {
        assertEquals("+251911000001", Phone.normalize("0911 000 001"))
        assertEquals("+251911000001", Phone.normalize("+251 911 000 001"))
    }

    @Test fun `display and mask`() {
        assertEquals("+251 91 100 0001", Phone.display("+251911000001"))
        assertEquals("+251 91 ••• 0001", Phone.mask("+251911000001"))
        assertEquals("—", Phone.display(null))
        assertEquals("12345", Phone.display("12345"))
    }
}

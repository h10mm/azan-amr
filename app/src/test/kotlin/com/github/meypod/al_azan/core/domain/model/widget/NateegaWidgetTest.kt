package com.github.meypod.al_azan.core.domain.model.widget

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.util.NateegaWisdomProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class NateegaWidgetTest {

    @Test
    fun testWisdomProvider_returnsValidWisdom() {
        val instant = Instant.fromEpochMilliseconds(1794584683000L) // Nov 2026
        val wisdom = NateegaWisdomProvider.getWisdomForDate(instant)
        assertNotNull(wisdom)
        assertTrue(wisdom.isNotBlank())

        // Check 365 days cycling
        for (i in 0..365) {
            val date = instant + i.days
            val w = NateegaWisdomProvider.getWisdomForDate(date)
            assertTrue("Wisdom at day $i should not be blank", w.isNotBlank())
        }
    }

    @Test
    fun testNateegaWidgetData_creation() {
        val data = NateegaWidgetData(
            dayOfWeekArabic = "الإثنين",
            dayOfWeekEnglish = "MONDAY",
            gregorianDay = "٢",
            gregorianMonth = "نوفمبر",
            gregorianYear = "٢٠٢٦",
            hijriDay = "٢٢",
            hijriMonth = "جمادى الأولى",
            hijriYear = "١٤٤٨",
            copticDate = "٢٣ بابه ١٧٤٣",
            gregorianDateSummary = "2 NOVEMBER 2026",
            cityName = "القاهرة",
            fajrTime = "٤:٤١ ص",
            sunriseTime = "٦:١٠ ص",
            dhuhrTime = "١١:٣٩ ص",
            asrTime = "٢:٤٤ م",
            maghribTime = "٥:٠٧ م",
            ishaTime = "٦:٢٦ م",
            nextPrayer = Prayer.Dhuhr,
            nextPrayerTime = "١١:٣٩ ص",
            countdownTargetMillis = 1794584683000L,
            progressPercent = 65,
            wisdomText = "لكل طاغية هاوية",
            themeColor = ThemeColor.ClassicLight,
            locale = "ar",
        )

        assertEquals("الإثنين", data.dayOfWeekArabic)
        assertEquals("MONDAY", data.dayOfWeekEnglish)
        assertEquals("القاهرة", data.cityName)
        assertEquals(Prayer.Dhuhr, data.nextPrayer)
        assertEquals(ThemeColor.ClassicLight, data.themeColor)
        assertEquals(65, data.progressPercent)
        assertEquals("لكل طاغية هاوية", data.wisdomText)
        assertEquals(NateegaClockPosition.CenterLarge, data.config.clockPosition)
        assertEquals(NateegaThemeMode.MatchAppTheme, data.config.themeMode)
        assertTrue(data.config.showCopticDate)
        assertTrue(data.config.showCountdown)
        assertTrue(data.config.showWisdom)
    }
}

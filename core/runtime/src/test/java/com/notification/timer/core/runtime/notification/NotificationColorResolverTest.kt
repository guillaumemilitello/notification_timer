package com.notification.timer.core.runtime.notification

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationColorResolverTest {
    private val lightSurface = Color.rgb(248, 249, 250)
    private val darkSurface = Color.rgb(30, 31, 34)

    private val testPalette =
        listOf(
            "#3F51B5", // Indigo
            "#E91E63", // Pink
            "#00BCD4", // Cyan
            "#4CAF50", // Green
            "#FF9800", // Orange
            "#F1EC51", // Light yellow
            "#FFFFFF", // White
            "#000000", // Black
            "#424242", // Charcoal
            "#FF4336", // Red
        )

    @Test
    fun `resolved day and night accent colors meet minimum contrast against light and dark surfaces`() {
        for (hex in testPalette) {
            val color = Color.parseColor(hex)
            val resolved = NotificationColorResolver.resolve(color)

            val dayContrast = ColorUtils.calculateContrast(resolved.dayAccentColor, lightSurface)
            val nightContrast = ColorUtils.calculateContrast(resolved.nightAccentColor, darkSurface)

            assertTrue("Day contrast for $hex ($dayContrast) must be >= 3.0", dayContrast >= 3.0)
            assertTrue("Night contrast for $hex ($nightContrast) must be >= 3.0", nightContrast >= 3.0)
        }
    }

    @Test
    fun `resolved track alpha is approximately 24 percent of resolved accent`() {
        val color = Color.parseColor("#3F51B5")
        val resolved = NotificationColorResolver.resolve(color)

        val dayTrackAlpha = Color.alpha(resolved.dayTrackColor)
        val nightTrackAlpha = Color.alpha(resolved.nightTrackColor)

        // 255 * 0.24 = 61
        assertTrue("Day track alpha should be around 61, was $dayTrackAlpha", dayTrackAlpha in 58..64)
        assertTrue("Night track alpha should be around 61, was $nightTrackAlpha", nightTrackAlpha in 58..64)
    }
}

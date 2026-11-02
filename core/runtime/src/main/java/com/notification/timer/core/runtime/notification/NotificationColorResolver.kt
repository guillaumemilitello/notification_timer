package com.notification.timer.core.runtime.notification

import android.graphics.Color
import androidx.core.graphics.ColorUtils

data class ResolvedThemeColors(
    val dayAccentColor: Int,
    val nightAccentColor: Int,
    val dayTrackColor: Int,
    val nightTrackColor: Int,
)

object NotificationColorResolver {
    // Standard system notification surface colors in Light and Dark modes
    private val LIGHT_SURFACE = Color.rgb(248, 249, 250) // #F8F9FA
    private val DARK_SURFACE = Color.rgb(30, 31, 34) // #1E1F22

    private const val MIN_ACCENT_CONTRAST = 3.0
    private const val TRACK_ALPHA_RATIO = 0.24f

    fun resolve(colorInt: Int): ResolvedThemeColors {
        val dayAccent = ensureContrast(colorInt, LIGHT_SURFACE, shouldDarken = true)
        val nightAccent = ensureContrast(colorInt, DARK_SURFACE, shouldDarken = false)

        val dayTrack = ColorUtils.setAlphaComponent(dayAccent, (255 * TRACK_ALPHA_RATIO).toInt())
        val nightTrack = ColorUtils.setAlphaComponent(nightAccent, (255 * TRACK_ALPHA_RATIO).toInt())

        return ResolvedThemeColors(
            dayAccentColor = dayAccent,
            nightAccentColor = nightAccent,
            dayTrackColor = dayTrack,
            nightTrackColor = nightTrack,
        )
    }

    private fun ensureContrast(
        foreground: Int,
        background: Int,
        shouldDarken: Boolean,
    ): Int {
        val initialContrast = ColorUtils.calculateContrast(foreground, background)
        if (initialContrast >= MIN_ACCENT_CONTRAST) {
            return foreground
        }

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(foreground, hsl)

        // Iteratively adjust lightness until minimum contrast is satisfied
        var currentLightness = hsl[2]
        val step = if (shouldDarken) -0.05f else 0.05f

        for (i in 0 until 20) {
            currentLightness = (currentLightness + step).coerceIn(0.0f, 1.0f)
            hsl[2] = currentLightness
            val adjusted = ColorUtils.HSLToColor(hsl)
            if (ColorUtils.calculateContrast(adjusted, background) >= MIN_ACCENT_CONTRAST) {
                return adjusted
            }
        }

        return if (shouldDarken) Color.BLACK else Color.WHITE
    }
}

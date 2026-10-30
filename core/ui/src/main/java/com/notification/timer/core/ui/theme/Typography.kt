package com.notification.timer.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.notification.timer.core.ui.R

val RecursiveFontFamily =
    FontFamily(
        Font(R.font.recursive_light, FontWeight.Light),
        Font(R.font.recursive_medium, FontWeight.Medium),
        Font(R.font.recursive_semibold, FontWeight.SemiBold),
    )

val RecursiveMonospaceFontFamily =
    FontFamily(
        Font(R.font.recursive_monospace_light, FontWeight.Light),
        Font(R.font.recursive_monospace_regular, FontWeight.Normal),
        Font(R.font.recursive_monospace_semibold, FontWeight.SemiBold),
        Font(R.font.recursive_monospace_bold, FontWeight.Bold),
    )

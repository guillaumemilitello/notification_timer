@file:Suppress("ktlint:standard:function-naming")

package com.notification.timer.core.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    maxFontSize: TextUnit = 120.sp,
    minFontSize: TextUnit = 12.sp,
    style: TextStyle = LocalTextStyle.current,
) {
    // Key by length instead of actual text so that standard countdowns
    // using a monospace font don't trigger a total re-layout/blink on every tick.
    var resizedFontSize by remember(text.length) { mutableFloatStateOf(maxFontSize.value) }
    var readyToDraw by remember(text.length) { androidx.compose.runtime.mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        fontSize = resizedFontSize.sp,
        style = style,
        softWrap = false,
        maxLines = 1,
        modifier =
            modifier.drawWithContent {
                if (readyToDraw) {
                    drawContent()
                }
            },
        onTextLayout = { result ->
            if (result.didOverflowWidth || result.didOverflowHeight) {
                if (resizedFontSize > minFontSize.value) {
                    resizedFontSize = (resizedFontSize - 2f).coerceAtLeast(minFontSize.value)
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        },
    )
}

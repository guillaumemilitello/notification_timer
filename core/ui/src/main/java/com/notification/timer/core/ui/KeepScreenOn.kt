@file:Suppress("ktlint:standard:function-naming")

package com.notification.timer.core.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview

/**
 * Keeps the window screen on while [enabled] is true and this composable is in composition.
 * Equivalent to `FLAG_KEEP_SCREEN_ON`; cleared automatically when leaving composition.
 */
@Composable
fun KeepScreenOnEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/** Header toggle using the 1.5 icons: `ic_screen` (off) and `ic_screen_locked` (on). */
@Composable
fun KeepScreenOnToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.keepScreenOn)
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier =
            modifier.semantics {
                contentDescription = description
            },
    ) {
        Icon(
            painter = painterResource(if (checked) R.drawable.ic_screen_locked else R.drawable.ic_screen),
            contentDescription = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KeepScreenOnToggleOffPreview() {
    MaterialTheme { KeepScreenOnToggle(checked = false, onCheckedChange = {}) }
}

@Preview(showBackground = true)
@Composable
private fun KeepScreenOnToggleOnPreview() {
    MaterialTheme { KeepScreenOnToggle(checked = true, onCheckedChange = {}) }
}

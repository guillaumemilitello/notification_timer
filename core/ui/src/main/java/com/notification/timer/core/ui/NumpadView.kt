@file:Suppress("ktlint:standard:function-naming")

package com.notification.timer.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notification.timer.core.ui.theme.RecursiveMonospaceFontFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumpadView(
    onConfirm: (durationSec: Int, sets: Int?) -> Unit,
    modifier: Modifier = Modifier,
    digits: String? = null,
    onDigitsChange: ((String) -> Unit)? = null,
    setsCount: Int? = null,
    onSetsCountChange: ((Int) -> Unit)? = null,
) {
    var internalDigits by remember { mutableStateOf("") }
    val effectiveDigits = digits ?: internalDigits
    val updateDigits: (String) -> Unit = { newDigits ->
        if (onDigitsChange != null) {
            onDigitsChange(newDigits)
        } else {
            internalDigits = newDigits
        }
    }

    var internalSetsCount by remember { mutableIntStateOf(0) }
    val effectiveSetsCount = setsCount ?: internalSetsCount
    val updateSetsCount: (Int) -> Unit = { newSets ->
        if (onSetsCountChange != null) {
            onSetsCountChange(newSets)
        } else {
            internalSetsCount = newSets
        }
    }

    val totalSeconds =
        remember(effectiveDigits) {
            val padded = effectiveDigits.padStart(6, '0')
            val h = padded.substring(0, 2).toIntOrNull() ?: 0
            val m = padded.substring(2, 4).toIntOrNull() ?: 0
            val s = padded.substring(4, 6).toIntOrNull() ?: 0
            h * 3600 + m * 60 + s
        }

    val displayString =
        remember(effectiveDigits) {
            val padded = effectiveDigits.padStart(6, '0')
            val h = padded.substring(0, 2)
            val m = padded.substring(2, 4)
            val s = padded.substring(4, 6)
            "$h:$m:$s"
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AutoResizeText(
            text = displayString,
            fontFamily = RecursiveMonospaceFontFamily,
            maxFontSize = 64.sp,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        // Sets Stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            Text("Sets:", style = MaterialTheme.typography.titleMedium)
            IconButton(
                onClick = { if (effectiveSetsCount > 0) updateSetsCount(effectiveSetsCount - 1) },
                enabled = effectiveSetsCount > 0,
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease sets")
            }
            Text(
                text = if (effectiveSetsCount == 0) "∞" else effectiveSetsCount.toString(),
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(
                onClick = { if (effectiveSetsCount < 99) updateSetsCount(effectiveSetsCount + 1) },
                enabled = effectiveSetsCount < 99,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase sets")
            }
        }

        // Numpad Grid 3x4
        val buttons =
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("0", "BACKSPACE", "START"),
            )

        for (row in buttons) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (cell in row) {
                    when (cell) {
                        "BACKSPACE" -> {
                            Card(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (effectiveDigits.isNotEmpty()) updateDigits(effectiveDigits.dropLast(1))
                                            },
                                            onLongClick = {
                                                updateDigits("")
                                            },
                                        ),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                                }
                            }
                        }

                        "START" -> {
                            Button(
                                onClick = {
                                    if (totalSeconds > 0) {
                                        onConfirm(totalSeconds, if (effectiveSetsCount == 0) null else effectiveSetsCount)
                                    }
                                },
                                enabled = totalSeconds > 0,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(56.dp),
                            ) {
                                Text("Start")
                            }
                        }

                        else -> {
                            OutlinedButton(
                                onClick = {
                                    if (effectiveDigits.length < 6) {
                                        updateDigits(effectiveDigits + cell)
                                    }
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(56.dp),
                            ) {
                                Text(cell, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

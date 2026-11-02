@file:Suppress("ktlint:standard:function-naming", "ktlint:standard:max-line-length")

package com.notification.timer.feature.timers

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notification.timer.core.domain.engine.TimerStateMachine
import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.HistoryEntry
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerAction
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.model.TimerConfig
import com.notification.timer.core.domain.model.TimerState
import com.notification.timer.core.ui.AutoResizeText
import com.notification.timer.core.ui.KeepScreenOnEffect
import com.notification.timer.core.ui.KeepScreenOnToggle
import com.notification.timer.core.ui.NumpadView
import com.notification.timer.core.ui.R
import com.notification.timer.core.ui.theme.RecursiveMonospaceFontFamily
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

val TIMER_COLOR_PALETTE =
    listOf(
        "#7a82b7", // Default Indigo
        "#3F51B5", // Indigo
        "#4B89AA", // Blue
        "#3D7776", // Cyan Dark
        "#006F37", // Green Dark
        "#4CAF50", // Green
        "#FFD740", // Amber
        "#FFAB40", // Orange
        "#F57F17", // Deep Orange
        "#FF4336", // Red
        "#87378D", // Purple
        "#424242", // Charcoal
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimersScreen(viewModel: TimersViewModel = hiltViewModel()) {
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

    KeepScreenOnEffect(enabled = keepScreenOn)

    var showAddIsland by remember { mutableStateOf(false) }
    var showDraftSettings by remember { mutableStateOf(false) }
    var draftTimerName by remember { mutableStateOf("") }
    var draftColorHex by remember { mutableStateOf("#3F51B5") }
    var draftSets by remember { mutableStateOf<Int?>(null) }
    var draftSoundEnabled by remember { mutableStateOf(true) }
    var draftSoundUri by remember { mutableStateOf<String?>(null) }
    var draftVibrate by remember { mutableStateOf(true) }
    var draftSettingsModified by remember { mutableStateOf(false) }
    var draftDigits by remember { mutableStateOf("") }

    val resetDraft = {
        draftTimerName = ""
        draftColorHex = "#3F51B5"
        draftSets = null
        draftSoundEnabled = true
        draftSoundUri = null
        draftVibrate = true
        draftSettingsModified = false
        draftDigits = ""
        showDraftSettings = false
    }

    var optionsTimerId by remember { mutableStateOf<Int?>(null) }
    var settingsTimerId by remember { mutableStateOf<Int?>(null) }

    // Drag-and-drop reordering state
    var draggingTimerId by remember { mutableStateOf<Int?>(null) }
    var draggingDeltaY by remember { mutableFloatStateOf(0f) }

    // Clear options state if active timer is deleted or timers are empty
    LaunchedEffect(timers) {
        if (optionsTimerId != null && timers.none { it.id == optionsTimerId }) {
            optionsTimerId = null
        }
    }

    val listState = rememberLazyListState()
    var previousTimerCount by remember { mutableIntStateOf(timers.size) }
    LaunchedEffect(timers.size) {
        if (timers.size > previousTimerCount) {
            listState.animateScrollToItem(0)
        }
        previousTimerCount = timers.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NotificationTimer") },
                actions = {
                    KeepScreenOnToggle(
                        checked = keepScreenOn,
                        onCheckedChange = { viewModel.toggleKeepScreenOn() },
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddIsland = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Timer")
            }
        },
    ) { innerPadding ->
        if (timers.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No active timers. Tap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(timers, key = { it.id }) { timer ->
                    val isDragged = draggingTimerId == timer.id
                    val isOptionsActive = optionsTimerId == timer.id

                    val itemModifier =
                        Modifier
                            .then(if (!isDragged) Modifier.animateItem() else Modifier)
                            .zIndex(if (isDragged) 2f else 1f)
                            .graphicsLayer {
                                if (isDragged) {
                                    translationY = draggingDeltaY
                                    shadowElevation = 12.dp.toPx()
                                }
                            }

                    TimerCardItem(
                        timer = timer,
                        isOptionsActive = isOptionsActive,
                        onToggleOptions = {
                            optionsTimerId = if (optionsTimerId == timer.id) null else timer.id
                        },
                        onCloseOptions = {
                            if (optionsTimerId == timer.id) {
                                optionsTimerId = null
                            }
                        },
                        onOpenSettings = {
                            settingsTimerId = timer.id
                        },
                        onCloseTimer = {
                            optionsTimerId = null
                            viewModel.handleCommand(TimerCommand.Clear(timer.id))
                        },
                        onDragStart = {
                            draggingTimerId = timer.id
                            draggingDeltaY = 0f
                        },
                        onDrag = { dragAmountY ->
                            draggingDeltaY += dragAmountY
                            val currentTimerId = draggingTimerId ?: return@TimerCardItem

                            var currentIndex = timers.indexOfFirst { it.id == currentTimerId }
                            if (currentIndex == -1) return@TimerCardItem

                            var currentItemInfo =
                                listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == currentTimerId }
                                    ?: listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex }
                                    ?: return@TimerCardItem

                            var currentCenterY = currentItemInfo.offset + currentItemInfo.size / 2f + draggingDeltaY

                            while (draggingDeltaY > 0 && currentIndex < timers.lastIndex) {
                                val nextItemInfo =
                                    listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex + 1 } ?: break
                                val nextCenterY = nextItemInfo.offset + nextItemInfo.size / 2f
                                if (currentCenterY > nextCenterY) {
                                    val nextIndex = currentIndex + 1
                                    viewModel.handleCommand(TimerCommand.Reorder(fromIndex = currentIndex, toIndex = nextIndex))
                                    draggingDeltaY -= (nextItemInfo.offset - currentItemInfo.offset)
                                    currentIndex = nextIndex
                                    currentItemInfo = nextItemInfo
                                    currentCenterY = currentItemInfo.offset + currentItemInfo.size / 2f + draggingDeltaY
                                } else {
                                    break
                                }
                            }

                            while (draggingDeltaY < 0 && currentIndex > 0) {
                                val prevItemInfo =
                                    listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex - 1 } ?: break
                                val prevCenterY = prevItemInfo.offset + prevItemInfo.size / 2f
                                if (currentCenterY < prevCenterY) {
                                    val prevIndex = currentIndex - 1
                                    viewModel.handleCommand(TimerCommand.Reorder(fromIndex = currentIndex, toIndex = prevIndex))
                                    draggingDeltaY -= (prevItemInfo.offset - currentItemInfo.offset)
                                    currentIndex = prevIndex
                                    currentItemInfo = prevItemInfo
                                    currentCenterY = currentItemInfo.offset + currentItemInfo.size / 2f + draggingDeltaY
                                } else {
                                    break
                                }
                            }
                        },
                        onDragEnd = {
                            draggingTimerId = null
                            draggingDeltaY = 0f
                        },
                        onCommand = { viewModel.handleCommand(it) },
                        modifier = itemModifier,
                    )
                }
            }
        }
    }

    if (showAddIsland) {
        Md3CenterIslandDialog(
            onDismissRequest = {
                if (showDraftSettings) {
                    showDraftSettings = false
                } else {
                    showAddIsland = false
                    resetDraft()
                }
            },
        ) {
            if (showDraftSettings) {
                TimerSettingsIslandContent(
                    initialName = draftTimerName,
                    initialColorHex = draftColorHex,
                    initialSets = draftSets,
                    initialSoundEnabled = draftSoundEnabled,
                    initialSoundUri = draftSoundUri,
                    initialVibrate = draftVibrate,
                    onSave = { newName, newColorHex, newSets, newSoundEnabled, newSoundUri, newVibrate ->
                        draftTimerName = newName
                        draftColorHex = newColorHex
                        draftSets = newSets
                        draftSoundEnabled = newSoundEnabled
                        draftSoundUri = newSoundUri
                        draftVibrate = newVibrate
                        draftSettingsModified = true
                        showDraftSettings = false
                    },
                    onCancel = { showDraftSettings = false },
                )
            } else {
                AddTimerContent(
                    history = history,
                    timerName = draftTimerName,
                    onTimerNameChange = { draftTimerName = it },
                    digits = draftDigits,
                    onDigitsChange = { draftDigits = it },
                    setsCount = draftSets ?: 0,
                    onSetsCountChange = { draftSets = if (it == 0) null else it },
                    onOpenSettings = { showDraftSettings = true },
                    onToggleStar = { viewModel.toggleHistoryStar(it) },
                    onDismiss = {
                        showAddIsland = false
                        resetDraft()
                    },
                    onHistorySelected = { entry ->
                        val finalConfig =
                            entry.config.copy(
                                name = if (draftTimerName.isNotBlank()) draftTimerName.trim() else entry.config.name,
                                sets = draftSets ?: entry.config.sets,
                                runningColor = if (draftColorHex != "#3F51B5") draftColorHex else entry.config.runningColor,
                                done =
                                    if (draftSettingsModified) {
                                        entry.config.done.copy(
                                            sound = draftSoundUri,
                                            soundEnabled = draftSoundEnabled,
                                            vibrate = draftVibrate,
                                        )
                                    } else {
                                        entry.config.done
                                    },
                            )
                        viewModel.startTimer(finalConfig)
                        showAddIsland = false
                        resetDraft()
                    },
                    onNumpadConfirm = { name, durationSec, sets ->
                        val config =
                            TimerConfig(
                                id = "temp_${System.currentTimeMillis()}",
                                name = name.ifBlank { "Timer" },
                                durationSec = durationSec,
                                sets = sets,
                                runningColor = draftColorHex,
                                done =
                                    Alert(
                                        name = "Time out!",
                                        beforeEndSec = 0,
                                        color = "#4CAF50",
                                        sound = draftSoundUri,
                                        soundEnabled = draftSoundEnabled,
                                        vibrate = draftVibrate,
                                    ),
                            )
                        viewModel.startTimer(config)
                        showAddIsland = false
                        resetDraft()
                    },
                )
            }
        }
    }

    val activeSettingsTimer = timers.firstOrNull { it.id == settingsTimerId }
    if (activeSettingsTimer != null) {
        Md3CenterIslandDialog(onDismissRequest = { settingsTimerId = null }) {
            TimerSettingsIslandContent(
                timer = activeSettingsTimer,
                onSave = { newName, newColorHex, newSets, newSoundEnabled, newSoundUri, newVibrate ->
                    if (newName.isNotBlank() && newName != activeSettingsTimer.label) {
                        viewModel.handleCommand(TimerCommand.Rename(activeSettingsTimer.id, newName.trim()))
                    }
                    if (newColorHex != activeSettingsTimer.config.runningColor) {
                        viewModel.handleCommand(TimerCommand.UpdateColor(activeSettingsTimer.id, newColorHex))
                    }
                    if (newSets != activeSettingsTimer.config.sets) {
                        viewModel.handleCommand(TimerCommand.UpdateSets(activeSettingsTimer.id, newSets))
                    }
                    if (newSoundEnabled != activeSettingsTimer.config.done.soundEnabled ||
                        newSoundUri != activeSettingsTimer.config.done.sound ||
                        newVibrate != activeSettingsTimer.config.done.vibrate
                    ) {
                        viewModel.updateTimerAlertSettings(
                            timerId = activeSettingsTimer.id,
                            soundEnabled = newSoundEnabled,
                            soundUri = newSoundUri,
                            vibrate = newVibrate,
                        )
                    }
                    settingsTimerId = null
                },
                onCancel = { settingsTimerId = null },
            )
        }
    }
}

@Composable
fun Md3CenterIslandDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            content()
        }
    }
}

@Composable
fun AddTimerContent(
    history: List<HistoryEntry>,
    onDismiss: () -> Unit,
    onHistorySelected: (HistoryEntry) -> Unit,
    onNumpadConfirm: (name: String, durationSec: Int, sets: Int?) -> Unit,
    modifier: Modifier = Modifier,
    timerName: String = "",
    onTimerNameChange: ((String) -> Unit)? = null,
    digits: String = "",
    onDigitsChange: ((String) -> Unit)? = null,
    setsCount: Int = 0,
    onSetsCountChange: ((Int) -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
    onToggleStar: (String) -> Unit = {},
) {
    var internalTimerName by remember { mutableStateOf(timerName) }
    val effectiveTimerName = if (onTimerNameChange != null) timerName else internalTimerName
    val updateTimerName: (String) -> Unit = {
        if (onTimerNameChange != null) {
            onTimerNameChange(it)
        } else {
            internalTimerName = it
        }
    }

    var showHistory by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = if (showHistory) 12.dp else 24.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showHistory) {
                    IconButton(onClick = { showHistory = false }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
                Text(
                    text = if (showHistory) stringResource(R.string.history) else stringResource(R.string.new_timer),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.timer_settings_title),
                    )
                }
                IconButton(onClick = { showHistory = !showHistory }) {
                    Icon(
                        imageVector = if (showHistory) Icons.Default.Dialpad else Icons.Default.History,
                        contentDescription =
                            if (showHistory) {
                                stringResource(R.string.manual_timer)
                            } else {
                                stringResource(R.string.history)
                            },
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        }

        if (showHistory) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 380.dp)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (history.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_history_available),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    items(history, key = { it.timerId }) { entry ->
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onHistorySelected(entry)
                                    },
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.config.name, style = MaterialTheme.typography.titleMedium)
                                    val duration = formatTime(entry.config.durationSec * 1000L)
                                    val sets = entry.config.sets
                                    val setsText =
                                        if (sets != null) {
                                            stringResource(R.string.timer_settings_sets_count, sets)
                                        } else {
                                            stringResource(R.string.timer_settings_sets_infinite)
                                        }
                                    Text("$duration • $setsText", style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(
                                    onClick = { onToggleStar(entry.timerId) },
                                ) {
                                    Icon(
                                        imageVector = if (entry.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription =
                                            if (entry.isStarred) {
                                                stringResource(R.string.unstar_timer)
                                            } else {
                                                stringResource(R.string.star_timer)
                                            },
                                        tint =
                                            if (entry.isStarred) {
                                                androidx.compose.ui.graphics
                                                    .Color(0xFFFFB300)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = effectiveTimerName,
                    onValueChange = updateTimerName,
                    label = { Text(stringResource(R.string.timer_name)) },
                    placeholder = { Text(stringResource(R.string.default_timer_name)) },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                )
                NumpadView(
                    digits = digits,
                    onDigitsChange = onDigitsChange,
                    setsCount = setsCount,
                    onSetsCountChange = onSetsCountChange,
                    onConfirm = { durationSec, sets ->
                        onNumpadConfirm(effectiveTimerName.trim(), durationSec, sets)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerCardItem(
    timer: Timer,
    isOptionsActive: Boolean,
    onToggleOptions: () -> Unit,
    onCloseOptions: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseTimer: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onCommand: (TimerCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Subscribe to a 32ms (~30fps) ticker while running for smooth progress and countdown updates
    var tick by remember(timer.id) { mutableLongStateOf(0L) }
    LaunchedEffect(timer.id, timer.state) {
        if (timer.state is TimerState.Running) {
            while (true) {
                tick = android.os.SystemClock.elapsedRealtime()
                delay(32.milliseconds)
            }
        }
    }

    // Always evaluate current elapsed time dynamically during composition so that
    // state transitions (Ready -> Running, Paused -> Running) never read a stale timestamp.
    val nowElapsedMs =
        if (timer.state is TimerState.Running && tick >= 0) {
            android.os.SystemClock.elapsedRealtime()
        } else {
            android.os.SystemClock.elapsedRealtime()
        }

    val durationMs = timer.config.durationSec * 1000L
    val rawRemainingMs = timer.remainingMs(nowElapsedMs)
    val remainingMs =
        if (timer.state is TimerState.Running && rawRemainingMs <= 0) {
            durationMs
        } else {
            rawRemainingMs
        }
    val progress =
        if (durationMs > 0) {
            ((durationMs - remainingMs).toFloat() / durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }

    val phaseColorHex = TimerStateMachine.computePhaseColor(timer, nowElapsedMs)
    val phaseColor =
        try {
            androidx.compose.ui.graphics
                .Color(Color.parseColor(phaseColorHex))
        } catch (_: Exception) {
            MaterialTheme.colorScheme.primary
        }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (isOptionsActive) {
                                onCloseOptions()
                            }
                        },
                        onLongClick = onToggleOptions,
                    ),
            border =
                if (isOptionsActive) {
                    BorderStroke(2.dp, phaseColor)
                } else {
                    null
                },
            colors =
                CardDefaults.cardColors(
                    containerColor = phaseColor.copy(alpha = 0.14f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isOptionsActive) 4.dp else 2.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                // Dedicated Top Header Object (Transparent background and border, always full width)
                Surface(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = if (isOptionsActive) 2.dp else 4.dp,
                                ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val setInfo =
                            if (timer.config.sets != null) {
                                "Set ${timer.currentSet} of ${timer.config.sets}"
                            } else {
                                "Set ${timer.currentSet}"
                            }

                        if (!isOptionsActive) {
                            Text(
                                text = timer.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Text(
                                text = setInfo,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            // Long-press options mode:
                            // "=" drag handle close to timer name, ready to drag
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .weight(1f, fill = false)
                                        .clip(MaterialTheme.shapes.small)
                                        .pointerInput(timer.id) {
                                            detectDragGestures(
                                                onDragStart = { onDragStart() },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    onDrag(dragAmount.y)
                                                },
                                                onDragEnd = { onDragEnd() },
                                                onDragCancel = { onDragEnd() },
                                            )
                                        }.padding(horizontal = 4.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = stringResource(R.string.drag_to_reorder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = timer.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            // Right side: Set info + Settings wheel + Trash red button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = setInfo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.timer_settings_description),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                IconButton(
                                    onClick = onCloseTimer,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_timer),
                                        tint =
                                            androidx.compose.ui.graphics
                                                .Color(0xFFD32F2F),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Numbers
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AutoResizeText(
                        text = formatTime(remainingMs),
                        fontFamily = RecursiveMonospaceFontFamily,
                        maxFontSize = 120.sp,
                        minFontSize = 40.sp,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    color = phaseColor,
                    trackColor = phaseColor.copy(alpha = 0.2f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Controls
                val actions = TimerStateMachine.actionsFor(timer)
                // Three fixed slots (left, center, right) so Play/Pause is always centered.
                // A slot stays empty when its action is unavailable.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left slot: Empty so Play/Pause remains centered (terminate is on long-press)
                    ControlSlot { }

                    // Center slot: Play/Pause
                    ControlSlot {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = phaseColor,
                            modifier =
                                Modifier.clickable {
                                    when {
                                        TimerAction.START in actions -> onCommand(TimerCommand.Start(timer.id))
                                        TimerAction.PAUSE in actions -> onCommand(TimerCommand.Pause(timer.id))
                                        TimerAction.RESUME in actions -> onCommand(TimerCommand.Resume(timer.id))
                                        TimerAction.START_NEXT_SET in actions -> onCommand(TimerCommand.StartNextSet(timer.id))
                                        TimerAction.EXTRA_SET in actions -> onCommand(TimerCommand.ExtraSet(timer.id))
                                    }
                                },
                        ) {
                            val icon =
                                when {
                                    TimerAction.PAUSE in actions -> Icons.Default.Pause
                                    else -> Icons.Default.PlayArrow
                                }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    // Right slot: Next set, else Stop, else Reset
                    ControlSlot {
                        when {
                            TimerAction.NEXT_SET in actions ->
                                IconButton(onClick = { onCommand(TimerCommand.NextSet(timer.id)) }) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next Set")
                                }

                            TimerAction.STOP in actions ->
                                IconButton(onClick = { onCommand(TimerCommand.Stop(timer.id)) }) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                                }

                            TimerAction.RESET in actions ->
                                IconButton(onClick = { onCommand(TimerCommand.Reset(timer.id)) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                                }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerSettingsIslandContent(
    initialName: String,
    initialColorHex: String,
    initialSets: Int?,
    initialSoundEnabled: Boolean = true,
    initialSoundUri: String? = null,
    initialVibrate: Boolean = true,
    onSave: (newName: String, newColorHex: String, newSets: Int?, soundEnabled: Boolean, soundUri: String?, vibrate: Boolean) -> Unit,
    onCancel: () -> Unit,
    title: String = "Timer Settings",
    timerKey: Any = Unit,
) {
    var name by remember(timerKey) { mutableStateOf(initialName) }
    var selectedColorHex by remember(timerKey) { mutableStateOf(initialColorHex) }
    var setsCount by remember(timerKey) { mutableIntStateOf(initialSets ?: 0) }
    var soundEnabled by remember(timerKey) { mutableStateOf(initialSoundEnabled) }
    var soundUri by remember(timerKey) { mutableStateOf(initialSoundUri) }
    var vibrate by remember(timerKey) { mutableStateOf(initialVibrate) }

    val context = LocalContext.current
    val ringtonePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? =
                    result.data?.getParcelableExtra(
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                        Uri::class.java,
                    )
                if (uri != null) {
                    soundUri = uri.toString()
                } else {
                    soundEnabled = false
                    soundUri = null
                }
            }
        }

    val soundTitle =
        remember(soundUri, context) {
            if (soundUri.isNullOrBlank()) {
                context.getString(R.string.timer_settings_sound_default)
            } else {
                try {
                    val ringtone = RingtoneManager.getRingtone(context, Uri.parse(soundUri))
                    ringtone?.getTitle(context) ?: context.getString(R.string.timer_settings_sound_default)
                } catch (_: Exception) {
                    context.getString(R.string.timer_settings_sound_default)
                }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Section 1: Identity / Name
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "General",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Timer Name") },
                placeholder = { Text(stringResource(R.string.default_timer_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HorizontalDivider()

        // Section 2: Background / Phase Color
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Background Color",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (colorHex in TIMER_COLOR_PALETTE) {
                    val parsedColor =
                        remember(colorHex) {
                            try {
                                androidx.compose.ui.graphics
                                    .Color(Color.parseColor(colorHex))
                            } catch (_: Exception) {
                                androidx.compose.ui.graphics.Color.Gray
                            }
                        }
                    val isSelected = selectedColorHex.equals(colorHex, ignoreCase = true)

                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    shape = CircleShape,
                                ).clickable { selectedColorHex = colorHex },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected color",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // Section 3: Sets (Number of sets from 0 (infinite) to N)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.timer_settings_sets_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            if (setsCount == 0) {
                                stringResource(R.string.timer_settings_sets_infinite)
                            } else {
                                stringResource(R.string.timer_settings_sets_count, setsCount)
                            },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.timer_settings_sets_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = { if (setsCount > 0) setsCount-- },
                        enabled = setsCount > 0,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.decrease_sets),
                        )
                    }
                    Text(
                        text = if (setsCount == 0) "0 (∞)" else setsCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.widthIn(min = 36.dp),
                        textAlign = TextAlign.Center,
                    )
                    IconButton(
                        onClick = { if (setsCount < 99) setsCount++ },
                        enabled = setsCount < 99,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.increase_sets),
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Section 4: Sound and Vibration
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.timer_settings_notification_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            // Sound Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.timer_settings_sound_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.timer_settings_sound_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it },
                )
            }

            // Sound Picker Row
            if (soundEnabled) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                try {
                                    val pickerIntent =
                                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                                RingtoneManager.TYPE_NOTIFICATION,
                                            )
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                                            )
                                            val currentUri =
                                                soundUri?.let { Uri.parse(it) }
                                                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                                        }
                                    ringtonePickerLauncher.launch(pickerIntent)
                                } catch (_: Exception) {
                                    // Fallback if system ringtone picker is unavailable
                                }
                            },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = soundTitle,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.timer_settings_choose_sound),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Vibration Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.timer_settings_vibrate_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.timer_settings_vibrate_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = vibrate,
                    onCheckedChange = { vibrate = it },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val finalSets = if (setsCount == 0) null else setsCount
                    onSave(name, selectedColorHex, finalSets, soundEnabled, soundUri, vibrate)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun TimerSettingsIslandContent(
    timer: Timer,
    onSave: (newName: String, newColorHex: String, newSets: Int?, soundEnabled: Boolean, soundUri: String?, vibrate: Boolean) -> Unit,
    onCancel: () -> Unit,
) = TimerSettingsIslandContent(
    initialName = timer.label,
    initialColorHex = timer.config.runningColor,
    initialSets = timer.config.sets,
    initialSoundEnabled = timer.config.done.soundEnabled,
    initialSoundUri = timer.config.done.sound,
    initialVibrate = timer.config.done.vibrate,
    onSave = onSave,
    onCancel = onCancel,
    timerKey = timer.id,
)

@Composable
private fun RowScope.ControlSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = if (millis <= 0) 0 else (millis + 999) / 1000
    if (totalSeconds <= 0) return "00:01"
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

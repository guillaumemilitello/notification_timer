@file:Suppress("ktlint:standard:function-naming", "ktlint:standard:max-line-length")

package com.notification.timer.feature.timers

import android.graphics.Color
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notification.timer.core.domain.engine.TimerStateMachine
import com.notification.timer.core.domain.model.Preset
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerAction
import com.notification.timer.core.domain.model.TimerCommand
import com.notification.timer.core.domain.model.TimerState
import com.notification.timer.core.ui.AutoResizeText
import com.notification.timer.core.ui.theme.RecursiveMonospaceFontFamily
import com.notification.timer.feature.presets.NumpadView
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
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    var showAddIsland by remember { mutableStateOf(false) }
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
        Md3CenterIslandDialog(onDismissRequest = { showAddIsland = false }) {
            AddTimerContent(
                presets = presets,
                onDismiss = { showAddIsland = false },
                onPresetSelected = {
                    viewModel.startTimerFromPreset(it)
                    showAddIsland = false
                },
                onNumpadConfirm = { durationSec, sets ->
                    val preset =
                        Preset(
                            id = "temp_${System.currentTimeMillis()}",
                            name = "Timer",
                            durationSec = durationSec,
                            sets = sets,
                        )
                    viewModel.startTimerFromPreset(preset)
                    showAddIsland = false
                },
            )
        }
    }

    val activeSettingsTimer = timers.firstOrNull { it.id == settingsTimerId }
    if (activeSettingsTimer != null) {
        Md3CenterIslandDialog(onDismissRequest = { settingsTimerId = null }) {
            TimerSettingsIslandContent(
                timer = activeSettingsTimer,
                onSave = { newName, newColorHex ->
                    if (newName.isNotBlank() && newName != activeSettingsTimer.label) {
                        viewModel.handleCommand(TimerCommand.Rename(activeSettingsTimer.id, newName.trim()))
                    }
                    if (newColorHex != activeSettingsTimer.config.runningColor) {
                        viewModel.handleCommand(TimerCommand.UpdateColor(activeSettingsTimer.id, newColorHex))
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
    presets: List<Preset>,
    onDismiss: () -> Unit,
    onPresetSelected: (Preset) -> Unit,
    onNumpadConfirm: (Int, Int?) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Manual", "Presets")

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "New Timer",
                style = MaterialTheme.typography.headlineSmall,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        when (selectedTab) {
            0 -> {
                NumpadView(
                    onConfirm = onNumpadConfirm,
                )
            }

            1 -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 380.dp)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (presets.isEmpty()) {
                        item {
                            Text("No presets available. Create one in Settings.", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(presets, key = { it.id }) { preset ->
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onPresetSelected(preset) },
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                                    val duration = formatTime(preset.durationSec * 1000L)
                                    Text("Duration: $duration", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
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
                // Header Info
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(end = if (isOptionsActive) 36.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = timer.label,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val setInfo =
                        if (timer.config.sets != null) {
                            "Set ${timer.currentSet} of ${timer.config.sets}"
                        } else {
                            "Set ${timer.currentSet}"
                        }
                    Text(
                        text = setInfo,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Options toolbar for this specific timer: Drag handle & Settings wheel
                if (isOptionsActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Drag handle to reorder
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
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
                                        }.padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Drag to reorder",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            // Settings wheel
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Timer settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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

        // Floating red cross at the top right corner exclusively for this timer when options are active
        if (isOptionsActive) {
            SmallFloatingActionButton(
                onClick = onCloseTimer,
                containerColor =
                    androidx.compose.ui.graphics
                        .Color(0xFFD32F2F),
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = CircleShape,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Timer",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerSettingsIslandContent(
    timer: Timer,
    onSave: (newName: String, newColorHex: String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(timer.id) { mutableStateOf(timer.label) }
    var selectedColorHex by remember(timer.id) { mutableStateOf(timer.config.runningColor) }

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
                text = "Timer Settings",
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

        // Section 3: Scalable placeholder for upcoming per-timer options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "More Options",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Additional timer settings (alerts, sound, sets, vibration) will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                onClick = { onSave(name, selectedColorHex) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Save")
            }
        }
    }
}

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

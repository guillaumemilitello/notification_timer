package com.notification.timer.core.runtime.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.runtime.R
import com.notification.timer.core.runtime.receiver.TimerActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRenderer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val eventLog: EventLog,
    ) {
        private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        private val activeNotifications = ConcurrentHashMap<Int, Notification>()

        companion object {
            const val CHANNEL_RUNNING = "timer_running_v2"
            const val CHANNEL_ALERT = "timer_alert"

            const val ACTION_PLAY_PAUSE = "com.notification.timer.action.PLAY_PAUSE"
            const val ACTION_NEXT = "com.notification.timer.action.NEXT"
            const val ACTION_CLOSE = "com.notification.timer.action.CLOSE"
            const val ACTION_DISMISS = "com.notification.timer.action.DISMISS"

            const val PROGRESS_MAX = 1000
            const val MIN_TIMER_NOTIFICATION_ID = 1000
            const val CHRONOMETER_CEILING_OFFSET_MS = 999L

            fun formatDisplayTime(millis: Long): String {
                val totalSeconds = if (millis <= 0) 0 else (millis + 999) / 1000
                if (totalSeconds <= 0) return "00:00"
                val seconds = totalSeconds % 60
                val minutes = (totalSeconds / 60) % 60
                val hours = totalSeconds / 3600
                return if (hours > 0) {
                    String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.US, "%02d:%02d", minutes, seconds)
                }
            }
        }

        init {
            createChannels()
        }

        private fun createChannels() {
            runCatching {
                notificationManager.deleteNotificationChannel("timer_running")
            }

            val runningChannel =
                NotificationChannel(
                    CHANNEL_RUNNING,
                    "Timer",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

            val alertChannel =
                NotificationChannel(
                    CHANNEL_ALERT,
                    "Timer alert",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }

            notificationManager.createNotificationChannels(listOf(runningChannel, alertChannel))
        }

        fun renderTimerNotification(model: TimerNotificationModel): Notification {
            val channelId = if (model.isAlerting) CHANNEL_ALERT else CHANNEL_RUNNING
            val notificationColor =
                when (val theme = model.colorTheme) {
                    is NotificationColorTheme.Active -> theme.colorInt
                    NotificationColorTheme.SystemDefault -> NotificationCompat.COLOR_DEFAULT
                }

            val openAppIntent =
                PendingIntent.getActivity(
                    context,
                    model.timerId,
                    context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        data = Uri.parse("ntimer://open/${model.timerId}")
                    } ?: Intent(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val remoteViews = RemoteViews(context.packageName, R.layout.notification)

            configureColorAndAccent(
                remoteViews = remoteViews,
                colorTheme = model.colorTheme,
                isAlerting = model.isAlerting,
            )

            configureTextAndProgress(
                remoteViews = remoteViews,
                title = model.title,
                subtitle = model.subtitle,
                remainingMs = model.remainingMs,
                durationMs = model.durationMs,
                showProgress = model.showProgress,
                colorTheme = model.colorTheme,
            )

            configureChronometer(
                remoteViews = remoteViews,
                remainingMs = model.remainingMs,
                isRunning = model.isRunning,
                chronometerBaseElapsedMs = model.chronometerBaseElapsedMs,
            )

            val dismissPendingIntent =
                configureButtons(
                    remoteViews = remoteViews,
                    timerId = model.timerId,
                    isRunning = model.isRunning,
                    showNext = model.showNext,
                )

            val builder =
                NotificationCompat
                    .Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(openAppIntent)
                    .setDeleteIntent(dismissPendingIntent)
                    .setCustomContentView(remoteViews)
                    .setCustomBigContentView(remoteViews)
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setColor(notificationColor)
                    .setColorized(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(model.isOngoing)
                    .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                    .setOnlyAlertOnce(!model.isAlerting)
                    .setWhen(model.postedAtWallMs)
                    .setShowWhen(false)
                    .setGroup("timer_${model.timerId}")

            if (model.isAlerting) {
                builder.setCustomHeadsUpContentView(remoteViews)
            }

            val notification = builder.build()
            val displayedText =
                if (model.isRunning && model.remainingMs > 1000L && model.chronometerBaseElapsedMs != null) {
                    "Chronometer(base=${model.chronometerBaseElapsedMs})"
                } else if (model.isRunning && model.remainingMs in 1..1000L) {
                    "00:00 (frozen)"
                } else {
                    formatDisplayTime(model.remainingMs.coerceAtLeast(0L))
                }
            eventLog.d(
                "NotificationRenderer",
                "Rendered notification: id=${model.timerId}, title='${model.title}', time='$displayedText', subtitle='${model.subtitle}', running=${model.isRunning}, alerting=${model.isAlerting}, remainingMs=${model.remainingMs}",
            )
            return notification
        }

        /**
         * Backward-compatible convenience overload for callers/tests using explicit arguments.
         */
        fun renderTimerNotification(
            timerId: Int,
            title: String,
            subtitle: String,
            remainingMs: Long,
            durationMs: Long,
            isRunning: Boolean,
            showProgress: Boolean = isRunning,
            showNext: Boolean = true,
            isAlerting: Boolean = false,
            isForeground: Boolean = false,
            isOngoing: Boolean = true,
            isDone: Boolean = false,
            chronometerBaseElapsedMs: Long? = null,
            phaseColor: Int = Color.parseColor("#7a82b7"),
            useColoredNotification: Boolean = true,
            postedAtWallMs: Long = System.currentTimeMillis(),
        ): Notification {
            val colorTheme =
                if (isDone || !useColoredNotification) {
                    NotificationColorTheme.SystemDefault
                } else {
                    NotificationColorTheme.Active(phaseColor)
                }

            return renderTimerNotification(
                TimerNotificationModel(
                    timerId = timerId,
                    title = title,
                    subtitle = subtitle,
                    remainingMs = remainingMs,
                    durationMs = durationMs,
                    isRunning = isRunning,
                    showProgress = showProgress,
                    showNext = showNext,
                    isAlerting = isAlerting,
                    isOngoing = isOngoing,
                    chronometerBaseElapsedMs = chronometerBaseElapsedMs,
                    colorTheme = colorTheme,
                    postedAtWallMs = postedAtWallMs,
                ),
            )
        }

        private fun configureColorAndAccent(
            remoteViews: RemoteViews,
            colorTheme: NotificationColorTheme,
            isAlerting: Boolean,
        ) {
            when (colorTheme) {
                is NotificationColorTheme.Active -> {
                    val resolved = NotificationColorResolver.resolve(colorTheme.colorInt)
                    // Accent countdown timer text with theme-contrast-safe color
                    remoteViews.setColorInt(
                        R.id.textViewTimer,
                        "setTextColor",
                        resolved.dayAccentColor,
                        resolved.nightAccentColor,
                    )
                    remoteViews.setColorInt(
                        R.id.textViewTimerStatic,
                        "setTextColor",
                        resolved.dayAccentColor,
                        resolved.nightAccentColor,
                    )
                }

                NotificationColorTheme.SystemDefault -> {
                    val textColorRes =
                        if (isAlerting) R.color.progress_bar_get_ready else R.color.progress_bar_timer_text
                    remoteViews.setColor(R.id.textViewTimer, "setTextColor", textColorRes)
                    remoteViews.setColor(R.id.textViewTimerStatic, "setTextColor", textColorRes)
                }
            }
        }

        private fun configureTextAndProgress(
            remoteViews: RemoteViews,
            title: String,
            subtitle: String,
            remainingMs: Long,
            durationMs: Long,
            showProgress: Boolean,
            colorTheme: NotificationColorTheme,
        ) {
            if (showProgress) {
                val headerText = if (subtitle.isNotBlank()) "$title · $subtitle" else title
                remoteViews.setTextViewText(R.id.textViewSets_short, headerText)
                remoteViews.setViewVisibility(R.id.textViewSets, View.GONE)
                remoteViews.setViewVisibility(R.id.progressBarTimer, View.VISIBLE)

                val progress =
                    if (durationMs > 0) {
                        (((durationMs - remainingMs).toDouble() * PROGRESS_MAX) / durationMs)
                            .toInt()
                            .coerceIn(0, PROGRESS_MAX)
                    } else {
                        0
                    }
                remoteViews.setProgressBar(R.id.progressBarTimer, PROGRESS_MAX, progress, false)

                when (colorTheme) {
                    is NotificationColorTheme.Active -> {
                        val resolved = NotificationColorResolver.resolve(colorTheme.colorInt)
                        remoteViews.setColorStateList(
                            R.id.progressBarTimer,
                            "setProgressTintList",
                            ColorStateList.valueOf(resolved.dayAccentColor),
                            ColorStateList.valueOf(resolved.nightAccentColor),
                        )
                        remoteViews.setColorStateList(
                            R.id.progressBarTimer,
                            "setProgressBackgroundTintList",
                            ColorStateList.valueOf(resolved.dayTrackColor),
                            ColorStateList.valueOf(resolved.nightTrackColor),
                        )
                    }

                    NotificationColorTheme.SystemDefault -> {
                        remoteViews.setColor(
                            R.id.progressBarTimer,
                            "setProgressTintList",
                            R.color.progress_bar_current,
                        )
                        remoteViews.setColor(
                            R.id.progressBarTimer,
                            "setProgressBackgroundTintList",
                            R.color.progress_bar_background,
                        )
                    }
                }
            } else {
                remoteViews.setTextViewText(R.id.textViewSets_short, title)
                remoteViews.setTextViewText(R.id.textViewSets, subtitle)
                remoteViews.setViewVisibility(R.id.progressBarTimer, View.GONE)
                remoteViews.setViewVisibility(R.id.textViewSets, View.VISIBLE)
            }
        }

        private fun configureChronometer(
            remoteViews: RemoteViews,
            remainingMs: Long,
            isRunning: Boolean,
            chronometerBaseElapsedMs: Long?,
        ) {
            val now = SystemClock.elapsedRealtime()
            val stopTime = chronometerBaseElapsedMs ?: (now + remainingMs)
            val isFuture = stopTime > now

            // When remainingMs <= 1000L or chronometerBaseElapsedMs is null, freeze to static 00:00
            // so SystemUI's countdown Chronometer cannot tick past zero into negative values.
            if (isRunning && remainingMs > 1000L && chronometerBaseElapsedMs != null && isFuture) {
                // Offset by +999ms so Chronometer's integer truncation ((base - now) / 1000)
                // matches formatDisplayTime ceiling division ((remaining + 999) / 1000).
                remoteViews.setViewVisibility(R.id.textViewTimer, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.textViewTimerStatic, View.GONE)
                remoteViews.setChronometer(R.id.textViewTimer, stopTime + CHRONOMETER_CEILING_OFFSET_MS, null, true)
                remoteViews.setChronometerCountDown(R.id.textViewTimer, true)
            } else {
                remoteViews.setViewVisibility(R.id.textViewTimer, View.GONE)
                remoteViews.setChronometer(R.id.textViewTimer, 0, null, false)
                remoteViews.setViewVisibility(R.id.textViewTimerStatic, View.VISIBLE)
                val displayMs = if (isRunning && remainingMs in 1..1000L) 0L else remainingMs.coerceAtLeast(0L)
                remoteViews.setTextViewText(R.id.textViewTimerStatic, formatDisplayTime(displayMs))
            }
        }

        private fun configureButtons(
            remoteViews: RemoteViews,
            timerId: Int,
            isRunning: Boolean,
            showNext: Boolean,
        ): PendingIntent {
            // 1. Play/Pause button
            val playPauseIntent =
                Intent(context, TimerActionReceiver::class.java).apply {
                    action = ACTION_PLAY_PAUSE
                    putExtra("timerId", timerId)
                    data = Uri.parse("ntimer://timer/$timerId/play_pause")
                }
            val playPausePendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    timerId * 10 + 1,
                    playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            remoteViews.setOnClickPendingIntent(R.id.buttonPlayPause, playPausePendingIntent)
            remoteViews.setImageViewResource(
                R.id.buttonPlayPause,
                if (isRunning) R.drawable.ic_pause else R.drawable.ic_play,
            )

            // 2. Next button
            remoteViews.setImageViewResource(R.id.buttonNext, R.drawable.ic_chevron_right)
            if (showNext) {
                val nextIntent =
                    Intent(context, TimerActionReceiver::class.java).apply {
                        action = ACTION_NEXT
                        putExtra("timerId", timerId)
                        data = Uri.parse("ntimer://timer/$timerId/next")
                    }
                val nextPendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        timerId * 10 + 2,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                remoteViews.setViewVisibility(R.id.buttonNext, View.VISIBLE)
                remoteViews.setBoolean(R.id.buttonNext, "setEnabled", true)
                remoteViews.setOnClickPendingIntent(R.id.buttonNext, nextPendingIntent)
            } else {
                remoteViews.setViewVisibility(R.id.buttonNext, View.INVISIBLE)
                remoteViews.setBoolean(R.id.buttonNext, "setEnabled", false)
                remoteViews.setOnClickPendingIntent(R.id.buttonNext, null)
            }

            // 3. Close button (explicit notification close)
            val closeIntent =
                Intent(context, TimerActionReceiver::class.java).apply {
                    action = ACTION_CLOSE
                    putExtra("timerId", timerId)
                    data = Uri.parse("ntimer://timer/$timerId/close")
                }
            val closePendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    timerId * 10 + 3,
                    closeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            remoteViews.setImageViewResource(R.id.buttonClose, R.drawable.ic_timer_delete)
            remoteViews.setOnClickPendingIntent(R.id.buttonClose, closePendingIntent)

            // 4. Swipe-dismiss intent (isolated per-timer dismissal)
            val dismissIntent =
                Intent(context, TimerActionReceiver::class.java).apply {
                    action = ACTION_DISMISS
                    putExtra("timerId", timerId)
                    data = Uri.parse("ntimer://timer/$timerId/dismiss")
                }
            return PendingIntent.getBroadcast(
                context,
                timerId * 10 + 4,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun postNotification(
            timerId: Int,
            notification: Notification,
        ) {
            activeNotifications[timerId] = notification
            notificationManager.notify(timerId, notification)
            eventLog.i("NotificationRenderer", "Posted notification $timerId")
        }

        fun cancelNotification(timerId: Int) {
            activeNotifications.remove(timerId)
            notificationManager.cancel(timerId)
            eventLog.i("NotificationRenderer", "Cancelled notification $timerId")
        }

        fun getActiveNotification(timerId: Int): Notification? = activeNotifications[timerId]

        fun getSystemActiveNotificationIds(): Set<Int> =
            runCatching {
                notificationManager.activeNotifications
                    .map { it.id }
                    .filter { it >= MIN_TIMER_NOTIFICATION_ID }
                    .toSet()
            }.getOrDefault(emptySet())
    }

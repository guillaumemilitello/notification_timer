package com.notification.timer.core.runtime.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.runtime.notification.NotificationRenderer
import com.notification.timer.core.runtime.notification.TimerNotificationModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {
    @Inject
    lateinit var notificationRenderer: NotificationRenderer

    @Inject
    lateinit var eventLog: EventLog

    private var activeForegroundId: Int = -1

    companion object {
        const val EXTRA_TIMER_ID = "extra_timer_id"
        const val ACTION_PROMOTE_FOREGROUND = "com.notification.timer.service.PROMOTE_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.notification.timer.service.STOP_FOREGROUND"

        fun start(
            context: Context,
            timerId: Int,
        ) {
            val intent =
                Intent(context, TimerForegroundService::class.java).apply {
                    action = ACTION_PROMOTE_FOREGROUND
                    putExtra(EXTRA_TIMER_ID, timerId)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent =
                Intent(context, TimerForegroundService::class.java).apply {
                    action = ACTION_STOP_FOREGROUND
                }
            runCatching {
                context.startService(intent)
            }.onFailure {
                context.stopService(Intent(context, TimerForegroundService::class.java))
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP_FOREGROUND) {
            detachOrRemoveActiveForeground()
            stopSelf()
            return START_NOT_STICKY
        }

        val timerId = intent?.getIntExtra(EXTRA_TIMER_ID, -1) ?: -1
        if (timerId == -1) {
            detachOrRemoveActiveForeground()
            stopSelf()
            return START_NOT_STICKY
        }

        eventLog.i("TimerForegroundService", "onStartCommand for timer $timerId (activeForegroundId=$activeForegroundId)")

        val previousForegroundId = activeForegroundId
        if (previousForegroundId != -1 && previousForegroundId != timerId) {
            val prevActive = notificationRenderer.getActiveNotification(previousForegroundId)
            stopForeground(if (prevActive != null) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
        }

        val activeNotification = notificationRenderer.getActiveNotification(timerId)
        val notificationToStart =
            activeNotification ?: notificationRenderer.renderTimerNotification(
                TimerNotificationModel(
                    timerId = timerId,
                    title = "Timer",
                    subtitle = "",
                    remainingMs = 0L,
                    durationMs = 0L,
                    isRunning = false,
                    showProgress = false,
                    showNext = false,
                ),
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                timerId,
                notificationToStart,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(timerId, notificationToStart)
        }
        activeForegroundId = timerId

        if (activeNotification == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            activeForegroundId = -1
            stopSelf()
            return START_NOT_STICKY
        }

        // Only re-post previous foreground notification if it is still active and OS cancelled it during ID transition
        if (previousForegroundId != -1 && previousForegroundId != timerId) {
            val prevNotif = notificationRenderer.getActiveNotification(previousForegroundId)
            if (prevNotif != null && previousForegroundId !in notificationRenderer.getSystemActiveNotificationIds()) {
                notificationRenderer.postNotification(previousForegroundId, prevNotif)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        detachOrRemoveActiveForeground()
        super.onDestroy()
        eventLog.i("TimerForegroundService", "onDestroy")
    }

    private fun detachOrRemoveActiveForeground() {
        val fgId = activeForegroundId
        if (fgId == -1) return
        activeForegroundId = -1

        val existing = notificationRenderer.getActiveNotification(fgId)
        if (existing != null) {
            stopForeground(STOP_FOREGROUND_DETACH)
            if (fgId !in notificationRenderer.getSystemActiveNotificationIds()) {
                notificationRenderer.postNotification(fgId, existing)
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }
}

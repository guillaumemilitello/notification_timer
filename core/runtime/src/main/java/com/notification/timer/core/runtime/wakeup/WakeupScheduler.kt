package com.notification.timer.core.runtime.wakeup

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import com.notification.timer.core.domain.diagnostics.EventLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeupScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val eventLog: EventLog,
    ) {
        private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        private var wakeLock: PowerManager.WakeLock? = null

        companion object {
            const val ACTION_WAKEUP = "com.notification.timer.action.WAKEUP"
            const val EARLY_MARGIN_MS = 2000L
            const val WAKELOCK_TIMEOUT_MS = 4000L
        }

        @SuppressLint("MissingPermission")
        fun scheduleWakeup(eventTimeElapsedRealtimeMs: Long) {
            val triggerAtMs =
                (eventTimeElapsedRealtimeMs - EARLY_MARGIN_MS)
                    .coerceAtLeast(SystemClock.elapsedRealtime())

            val pendingIntent = createWakeupPendingIntent()

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMs,
                    pendingIntent,
                )
                eventLog.log(
                    "WakeupScheduler",
                    "Armed exact wakeup for elapsedMs=$triggerAtMs (delay=${triggerAtMs - SystemClock.elapsedRealtime()}ms)",
                )
            } catch (e: Exception) {
                eventLog.log("WakeupScheduler", "Failed to schedule exact alarm: ${e.message}")
            }
        }

        fun cancelWakeup() {
            alarmManager.cancel(createWakeupPendingIntent())
            eventLog.log("WakeupScheduler", "Cancelled scheduled wakeup")
        }

        fun acquireWakeLock(timeoutMs: Long = WAKELOCK_TIMEOUT_MS) {
            if (wakeLock == null) {
                wakeLock =
                    powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "NotificationTimer:WakeupLock",
                    )
            }
            wakeLock?.acquire(timeoutMs)
            eventLog.log("WakeupScheduler", "Acquired PARTIAL_WAKE_LOCK for ${timeoutMs}ms")
        }

        fun releaseWakeLock() {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                eventLog.log("WakeupScheduler", "Released PARTIAL_WAKE_LOCK")
            }
        }

        private fun createWakeupPendingIntent(): PendingIntent {
            val intent =
                Intent(context, com.notification.timer.core.runtime.receiver.WakeupReceiver::class.java).apply {
                    action = ACTION_WAKEUP
                    data = Uri.parse("ntimer://wakeup")
                }
            return PendingIntent.getBroadcast(
                context,
                1, // Reserved < 1000 system ID space; timer IDs start at 1000
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

package com.notification.timer.core.runtime.wakeup

import android.app.AlarmManager
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.data.diagnostics.InMemoryEventLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class WakeupSchedulerTest {
    private lateinit var context: Context
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var eventLog: InMemoryEventLog
    private lateinit var scheduler: WakeupScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        eventLog = InMemoryEventLog()
        scheduler = WakeupScheduler(context, eventLog)
    }

    @Test
    fun `scheduleWakeup arms alarm 2 seconds before event`() {
        val now = SystemClock.elapsedRealtime()
        val eventTime = now + 10_000L // 10 seconds from now

        scheduler.scheduleWakeup(eventTime)

        val nextAlarm = shadowAlarmManager.peekNextScheduledAlarm()
        assertNotNull("Alarm should be scheduled", nextAlarm)
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextAlarm!!.getType())
        // triggerAtMs should be eventTime - 2000L
        assertEquals(eventTime - WakeupScheduler.EARLY_MARGIN_MS, nextAlarm.triggerAtMs)
    }

    @Test
    fun `scheduleWakeup for immediate event clamps to current elapsedRealtime`() {
        val now = SystemClock.elapsedRealtime()
        val immediateEvent = now + 500L // 0.5s from now (< 2s margin)

        scheduler.scheduleWakeup(immediateEvent)

        val nextAlarm = shadowAlarmManager.peekNextScheduledAlarm()
        assertNotNull(nextAlarm)
        assertTrue(
            "Alarm trigger time should be at least current elapsed time",
            nextAlarm!!.triggerAtMs >= now,
        )
    }

    @Test
    fun `scheduleWakeup within early margin arms alarm for exact event time`() {
        val now = SystemClock.elapsedRealtime()
        val eventTime = now + 1200L // 1.2s from now (< 2s margin)

        scheduler.scheduleWakeup(eventTime)

        val nextAlarm = shadowAlarmManager.peekNextScheduledAlarm()
        assertNotNull("Alarm should be scheduled", nextAlarm)
        assertEquals(eventTime, nextAlarm!!.triggerAtMs)
    }

    @Test
    fun `cancelWakeup removes scheduled alarm`() {
        val now = SystemClock.elapsedRealtime()
        scheduler.scheduleWakeup(now + 10_000L)
        assertNotNull(shadowAlarmManager.peekNextScheduledAlarm())

        scheduler.cancelWakeup()
        assertNull("Alarm should be cancelled", shadowAlarmManager.peekNextScheduledAlarm())
    }

    @Test
    fun `acquireWakeLock and releaseWakeLock manage wake lock without error`() {
        scheduler.acquireWakeLock(3000L)
        val logs = eventLog.getEntries()
        assertTrue(logs.any { it.message.contains("Acquired PARTIAL_WAKE_LOCK") })

        scheduler.releaseWakeLock()
        val updatedLogs = eventLog.getEntries()
        assertTrue(updatedLogs.any { it.message.contains("Released PARTIAL_WAKE_LOCK") })
    }
}

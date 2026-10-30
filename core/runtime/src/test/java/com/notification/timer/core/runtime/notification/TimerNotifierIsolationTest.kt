package com.notification.timer.core.runtime.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.domain.engine.TimerStateMachine
import com.notification.timer.core.domain.model.Preset
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.ports.TimeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TimerNotifierIsolationTest {
    private class MutableTimeSource(
        var elapsedMs: Long = 50_000L,
        var wallMs: Long = 1_700_000_000_000L,
    ) : TimeSource {
        override fun elapsedRealtimeMs(): Long = elapsedMs

        override fun currentTimeMillis(): Long = wallMs
    }

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var renderer: NotificationRenderer
    private lateinit var clock: MutableTimeSource
    private lateinit var adapter: TimerNotifierAdapter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        renderer = NotificationRenderer(context, FakeEventLog())
        clock = MutableTimeSource()
        adapter = TimerNotifierAdapter(context, renderer, clock)
    }

    @Test
    fun `starting Timer B never resurrects dismissed Done notification of Timer A`() {
        val presetA = Preset(id = "a", name = "Eggs", durationSec = 30, sets = 2)
        val presetB = Preset(id = "b", name = "Tea", durationSec = 180)

        // 1. Timer A starts and runs to completion ("Done")
        val timerARunning =
            TimerStateMachine.start(
                Timer(id = 1000, presetId = presetA.id, config = presetA, label = "Eggs"),
                nowElapsedMs = clock.elapsedMs,
            )
        val timerBIdle = Timer(id = 1001, presetId = presetB.id, config = presetB, label = "Tea")

        adapter.updateNotifications(listOf(timerARunning, timerBIdle))
        assertNotNull("Running Timer A should post notification", shadowOf(notificationManager).getNotification(1000))
        assertNull("Unstarted Ready Timer B must not post notification", shadowOf(notificationManager).getNotification(1001))

        // Timer A completes -> Done (green Ready(isCompleted = true))
        clock.elapsedMs += 30_000L
        val (timerADone, _) = TimerStateMachine.checkAlertsAndEnd(timerARunning, clock.elapsedMs)
        adapter.updateNotifications(listOf(timerADone, timerBIdle))
        val doneNotification = shadowOf(notificationManager).getNotification(1000)
        assertNotNull("Completed Timer A should show Done notification", doneNotification)
        assertFalse(
            "Done notification must not be ongoing so user can swipe it away",
            (doneNotification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0,
        )

        // 2. User dismisses/closes Timer A's Done notification
        val timerADismissed = TimerStateMachine.dismissNotification(timerADone)
        adapter.updateNotifications(listOf(timerADismissed, timerBIdle))
        assertNull("Dismissed Timer A notification must be cancelled", shadowOf(notificationManager).getNotification(1000))

        // 3. User presses Play on Timer B -> Timer A's notification MUST NOT reappear!
        clock.elapsedMs += 5_000L
        val timerBRunning = TimerStateMachine.start(timerBIdle, clock.elapsedMs)
        adapter.updateNotifications(listOf(timerADismissed, timerBRunning))

        assertNull(
            "Pressing Play on Timer B must NEVER resurrect Timer A's Done notification",
            shadowOf(notificationManager).getNotification(1000),
        )
        assertNotNull("Timer B notification should be posted", shadowOf(notificationManager).getNotification(1001))

        // 4. Even across periodic ticks and Pause/Resume on Timer B, Timer A stays absent
        clock.elapsedMs += 10_000L
        adapter.updateNotifications(listOf(timerADismissed, timerBRunning))
        assertNull(shadowOf(notificationManager).getNotification(1000))

        val timerBPaused = TimerStateMachine.pause(timerBRunning, clock.elapsedMs)
        adapter.updateNotifications(listOf(timerADismissed, timerBPaused))
        assertNull(shadowOf(notificationManager).getNotification(1000))
        assertNotNull(shadowOf(notificationManager).getNotification(1001))
    }

    @Test
    fun `starting Timer B does not re-post or update untouched Done notification of Timer A`() {
        val presetA = Preset(id = "a", name = "Pasta", durationSec = 10)
        val presetB = Preset(id = "b", name = "Sauce", durationSec = 60)

        val timerARunning =
            TimerStateMachine.start(
                Timer(id = 1000, presetId = presetA.id, config = presetA, label = "Pasta"),
                nowElapsedMs = clock.elapsedMs,
            )
        adapter.updateNotifications(listOf(timerARunning))

        clock.elapsedMs += 10_000L
        clock.wallMs += 10_000L
        val (timerADone, _) = TimerStateMachine.checkAlertsAndEnd(timerARunning, clock.elapsedMs)
        adapter.updateNotifications(listOf(timerADone))
        val originalDoneInstance = shadowOf(notificationManager).getNotification(1000)
        assertNotNull(originalDoneInstance)

        // Even if OS cleared notification 1000 externally (e.g. Clear All before broadcast arrived),
        // starting Timer B must NOT re-post notification 1000 because Timer A's spec did not change!
        notificationManager.cancel(1000)
        assertNull(shadowOf(notificationManager).getNotification(1000))

        clock.elapsedMs += 5_000L
        clock.wallMs += 5_000L
        val timerBRunning =
            TimerStateMachine.start(
                Timer(id = 1001, presetId = presetB.id, config = presetB, label = "Sauce"),
                nowElapsedMs = clock.elapsedMs,
            )
        adapter.updateNotifications(listOf(timerADone, timerBRunning))

        assertNull(
            "Unchanged Timer A must receive zero notify() calls when Timer B starts",
            shadowOf(notificationManager).getNotification(1000),
        )
        assertNotNull(shadowOf(notificationManager).getNotification(1001))
    }

    @Test
    fun `scales to N concurrent timers with sticky foreground anchor and isolated updates`() {
        val timers =
            (0 until 8).map { i ->
                val base =
                    Timer(
                        id = 1000 + i,
                        presetId = "p_$i",
                        config = Preset(id = "p_$i", name = "Timer ${i + 1}", durationSec = 300 - i * 20),
                        label = "Timer ${i + 1}",
                    )
                when {
                    i < 3 -> TimerStateMachine.start(base, clock.elapsedMs)
                    i < 5 -> {
                        val started = TimerStateMachine.start(base, clock.elapsedMs)
                        TimerStateMachine.pause(started, clock.elapsedMs + 5_000L)
                    }
                    else -> base
                }
            }

        adapter.updateNotifications(timers)

        // Active running (1000..1002) and paused (1003..1004) have notifications; idle Ready (1005..1007) do not
        for (id in 1000..1004) {
            assertNotNull("Timer $id should have active notification", shadowOf(notificationManager).getNotification(id))
            assertEquals("timer_$id", shadowOf(notificationManager).getNotification(id).group)
        }
        for (id in 1005..1007) {
            assertNull("Idle Ready timer $id should not have notification", shadowOf(notificationManager).getNotification(id))
        }

        // Capture exact Notification instances of 1000..1004, then start timer 1007 with a 5-second duration (shortest!)
        // Sticky foreground anchor must keep existing running anchor unchanged and NOT re-post 1000..1004!
        val beforeInstances = (1000..1004).associateWith { shadowOf(notificationManager).getNotification(it) }

        clock.elapsedMs += 500L // Within progress refresh interval
        val updatedTimers =
            timers.map { t ->
                if (t.id == 1007) {
                    TimerStateMachine.start(t.copy(config = t.config.copy(durationSec = 5)), clock.elapsedMs)
                } else {
                    t
                }
            }
        adapter.updateNotifications(updatedTimers)

        for ((id, originalNotif) in beforeInstances) {
            assertTrue(
                "Starting Timer 1007 must not re-render or re-post Timer $id",
                originalNotif === shadowOf(notificationManager).getNotification(id),
            )
        }
        assertNotNull("Started Timer 1007 must post its own notification", shadowOf(notificationManager).getNotification(1007))
    }
}

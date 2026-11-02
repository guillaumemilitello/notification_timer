package com.notification.timer.core.runtime.notification

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.domain.engine.TimerStateMachine
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerConfig
import com.notification.timer.core.domain.ports.TimeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private class NoOpEventLog : com.notification.timer.core.domain.diagnostics.EventLog {
    override fun log(
        tag: String,
        message: String,
    ) {}

    override fun getEntries() = emptyList<com.notification.timer.core.domain.diagnostics.LogEntry>()

    override fun clear() {}
}

private class MutableTimeSource : TimeSource {
    var elapsedMs: Long = 100_000L

    override fun elapsedRealtimeMs(): Long = elapsedMs

    override fun currentTimeMillis(): Long = elapsedMs
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TimerNotifierIsolationTest {
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var renderer: NotificationRenderer
    private lateinit var clock: MutableTimeSource
    private lateinit var adapter: TimerNotifierAdapter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        renderer = NotificationRenderer(context, NoOpEventLog())
        clock = MutableTimeSource()
        adapter = TimerNotifierAdapter(context, renderer, clock)
    }

    @Test
    fun `starting Timer B never resurrects dismissed Done notification of Timer A`() {
        val configA = TimerConfig(id = "a", name = "Eggs", durationSec = 30, sets = 2)
        val configB = TimerConfig(id = "b", name = "Tea", durationSec = 180)

        // 1. Timer A starts and runs to completion ("Done")
        val timerARunning =
            TimerStateMachine.start(
                Timer(id = 1000, configId = configA.id, config = configA, label = "Eggs"),
                nowElapsedMs = clock.elapsedMs,
            )
        val timerBIdle = Timer(id = 1001, configId = configB.id, config = configB, label = "Tea")

        adapter.updateNotifications(listOf(timerARunning, timerBIdle))
        assertNotNull("Running Timer A should post notification", shadowOf(notificationManager).getNotification(1000))
        assertNull("Unstarted Ready Timer B must not post notification", shadowOf(notificationManager).getNotification(1001))

        // Timer A completes -> Done (green Ready(isCompleted = true))
        clock.elapsedMs += 30_000L
        val (timerADone, _) = TimerStateMachine.checkAlertsAndEnd(timerARunning, clock.elapsedMs)
        adapter.updateNotifications(listOf(timerADone, timerBIdle))
        val doneNotification = shadowOf(notificationManager).getNotification(1000)
        assertNotNull("Completed Timer A should display Done notification", doneNotification)

        // 2. User swipes away/dismisses Timer A's Done notification
        val timerADismissed = TimerStateMachine.dismissNotification(timerADone)
        adapter.updateNotifications(listOf(timerADismissed, timerBIdle))
        assertNull("Dismissed Timer A notification must be cancelled", shadowOf(notificationManager).getNotification(1000))

        // 3. User starts Timer B -> Timer B becomes Running
        val timerBRunning = TimerStateMachine.start(timerBIdle, nowElapsedMs = clock.elapsedMs)
        adapter.updateNotifications(listOf(timerADismissed, timerBRunning))

        // Invariant: Timer B is posted, but Timer A MUST NOT be resurrected!
        assertNotNull("Timer B must have active notification", shadowOf(notificationManager).getNotification(1001))
        assertNull("Dismissed Timer A MUST NOT be resurrected when Timer B starts", shadowOf(notificationManager).getNotification(1000))
    }

    @Test
    fun `closing Timer A from notification cancels its notification and keeps Timer B intact`() {
        val configA = TimerConfig(id = "a", name = "Eggs", durationSec = 60)
        val configB = TimerConfig(id = "b", name = "Tea", durationSec = 120)

        val timerA =
            TimerStateMachine.start(
                Timer(id = 1000, configId = configA.id, config = configA, label = "Eggs"),
                nowElapsedMs = clock.elapsedMs,
            )
        val timerB =
            TimerStateMachine.start(
                Timer(id = 1001, configId = configB.id, config = configB, label = "Tea"),
                nowElapsedMs = clock.elapsedMs,
            )

        adapter.updateNotifications(listOf(timerA, timerB))
        assertNotNull(shadowOf(notificationManager).getNotification(1000))
        assertNotNull(shadowOf(notificationManager).getNotification(1001))

        // User taps close cross on Timer A -> closeNotification sets isNotificationVisible = false
        val timerAClosed = TimerStateMachine.closeNotification(timerA)
        adapter.updateNotifications(listOf(timerAClosed, timerB))

        assertNull("Timer A notification must be cancelled after close cross", shadowOf(notificationManager).getNotification(1000))
        assertNotNull("Timer B notification must remain intact", shadowOf(notificationManager).getNotification(1001))
    }

    @Test
    fun `reconcileRestoredNotifications reconciles without re-alerting already completed timers`() {
        val configA = TimerConfig(id = "a", name = "Pasta", durationSec = 10)
        val configB = TimerConfig(id = "b", name = "Sauce", durationSec = 60)

        // Restored timers where Timer A completed while app was dead
        val timerACompleted =
            Timer(
                id = 1000,
                configId = configA.id,
                config = configA,
                label = "Pasta",
                state =
                    com.notification.timer.core.domain.model.TimerState
                        .Ready(isCompleted = true),
                isNotificationVisible = true,
            )
        val timerBRunning =
            TimerStateMachine.start(
                Timer(id = 1001, configId = configB.id, config = configB, label = "Sauce"),
                nowElapsedMs = clock.elapsedMs,
            )

        // 1. If timerA was NOT newly completed on this startup reconciliation (i.e. already notified earlier)
        adapter.reconcileRestoredNotifications(
            timers = listOf(timerACompleted, timerBRunning),
            newlyCompletedIds = emptySet(),
        )

        // Notification for Running Timer B should be active
        assertNotNull(shadowOf(notificationManager).getNotification(1001))
        // Notification for already-completed Timer A should NOT be re-posted to silent shade unless newly completed
        assertNull("Already-completed restored timer should not re-post notification", shadowOf(notificationManager).getNotification(1000))

        // 2. If timerA WAS newly completed during this startup's reconciliation tick
        adapter.reconcileRestoredNotifications(
            timers = listOf(timerACompleted, timerBRunning),
            newlyCompletedIds = setOf(1000),
        )
        assertNotNull("Newly completed restored timer MUST post Done notification", shadowOf(notificationManager).getNotification(1000))
    }

    @Test
    fun `renders N notifications independently with unique IDs`() {
        val timers =
            (0 until 7).map { i ->
                val timerId = 1000 + i
                val config = TimerConfig(id = "c_$i", name = "Timer ${i + 1}", durationSec = 300 - i * 20)
                TimerStateMachine.start(
                    Timer(id = timerId, configId = config.id, config = config, label = config.name),
                    nowElapsedMs = clock.elapsedMs,
                )
            }

        adapter.updateNotifications(timers)

        // Verify each timer has its own active notification with exact ID
        for (timer in timers) {
            val notification = shadowOf(notificationManager).getNotification(timer.id)
            assertNotNull("Timer ${timer.id} must have posted notification", notification)
        }

        // Cancel the 3rd timer (ID 1002)
        val remaining =
            timers.map {
                if (it.id == 1002) TimerStateMachine.closeNotification(it) else it
            }
        adapter.updateNotifications(remaining)

        assertNull("Timer 1002 should be cancelled", shadowOf(notificationManager).getNotification(1002))
        assertNotNull("Timer 1000 must remain", shadowOf(notificationManager).getNotification(1000))
        assertNotNull("Timer 1001 must remain", shadowOf(notificationManager).getNotification(1001))
        assertNotNull("Timer 1003 must remain", shadowOf(notificationManager).getNotification(1003))
        assertNotNull("Timer 1004 must remain", shadowOf(notificationManager).getNotification(1004))
        assertNotNull("Timer 1005 must remain", shadowOf(notificationManager).getNotification(1005))
        assertNotNull("Timer 1006 must remain", shadowOf(notificationManager).getNotification(1006))
    }

    @Test
    fun `notification color syncs with timer settings when running and paused and keeps default color when done`() {
        val customColor = "#E91E63" // Pink
        val parsedCustomColor = Color.parseColor(customColor)
        val config = TimerConfig(id = "c1", name = "Baking", durationSec = 60, runningColor = customColor)
        val timer = Timer(id = 2000, configId = config.id, config = config, label = "Baking")

        // 1. Running -> Notification color matches setting and is not forced colorized
        val runningTimer = TimerStateMachine.start(timer, nowElapsedMs = clock.elapsedMs)
        adapter.updateNotifications(listOf(runningTimer))
        val runningNotification = shadowOf(notificationManager).getNotification(2000)
        assertNotNull(runningNotification)
        assertEquals(parsedCustomColor, runningNotification.color)
        assertFalse(runningNotification.extras.getBoolean(NotificationCompat.EXTRA_COLORIZED, false))

        // 2. Paused -> Notification color stays in setting color and remains not forced colorized
        val pausedTimer = TimerStateMachine.pause(runningTimer, nowElapsedMs = clock.elapsedMs + 10_000L)
        adapter.updateNotifications(listOf(pausedTimer))
        val pausedNotification = shadowOf(notificationManager).getNotification(2000)
        assertNotNull(pausedNotification)
        assertEquals(parsedCustomColor, pausedNotification.color)
        assertFalse(pausedNotification.extras.getBoolean(NotificationCompat.EXTRA_COLORIZED, false))

        // 3. Update color in settings while paused -> Notification updates to new setting color
        val updatedColor = "#00BCD4" // Cyan
        val parsedUpdatedColor = Color.parseColor(updatedColor)
        val recoloredTimer = TimerStateMachine.updateColor(pausedTimer, updatedColor)
        adapter.updateNotifications(listOf(recoloredTimer))
        val recoloredNotification = shadowOf(notificationManager).getNotification(2000)
        assertNotNull(recoloredNotification)
        assertEquals(parsedUpdatedColor, recoloredNotification.color)
        assertFalse(recoloredNotification.extras.getBoolean(NotificationCompat.EXTRA_COLORIZED, false))

        // 4. Resume and Complete -> Notification keeps default color following system theme and is not colorized
        val resumedTimer = TimerStateMachine.resume(recoloredTimer, nowElapsedMs = clock.elapsedMs + 10_000L)
        clock.elapsedMs += 70_000L
        val (doneTimer, _) = TimerStateMachine.checkAlertsAndEnd(resumedTimer, nowElapsedMs = clock.elapsedMs)
        adapter.updateNotifications(listOf(doneTimer))
        val doneNotification = shadowOf(notificationManager).getNotification(2000)
        assertNotNull(doneNotification)
        assertEquals(NotificationCompat.COLOR_DEFAULT, doneNotification.color)
        assertFalse(doneNotification.extras.getBoolean(NotificationCompat.EXTRA_COLORIZED, false))
    }

    @Test
    fun `four concurrent timers with distinct colors maintain colors independently when paused one by one`() {
        val colors = listOf("#FF4336", "#3F51B5", "#00BCD4", "#FFD740")
        var currentTimers =
            colors.mapIndexed { index, hex ->
                val timerId = 1000 + index
                val config = TimerConfig(id = "c$timerId", name = "Timer $timerId", durationSec = 120, runningColor = hex)
                TimerStateMachine.start(
                    Timer(id = timerId, configId = config.id, config = config, label = config.name),
                    nowElapsedMs = clock.elapsedMs,
                )
            }

        adapter.updateNotifications(currentTimers)

        // All 4 running with their distinct colors
        for (i in colors.indices) {
            val notif = shadowOf(notificationManager).getNotification(1000 + i)
            assertNotNull(notif)
            assertEquals(Color.parseColor(colors[i]), notif.color)
        }

        // Pause each timer one by one and assert each timer retains its exact color
        for (pauseIndex in colors.indices) {
            val timerToPause = currentTimers[pauseIndex]
            val paused = TimerStateMachine.pause(timerToPause, nowElapsedMs = clock.elapsedMs + 5000L)
            currentTimers = currentTimers.map { if (it.id == paused.id) paused else it }

            adapter.updateNotifications(currentTimers)

            // Verify ALL 4 timers retain their respective colors, even as FGS transitions or detaches
            for (i in colors.indices) {
                val notif = shadowOf(notificationManager).getNotification(1000 + i)
                assertNotNull("Timer ${1000 + i} notification must be active", notif)
                assertEquals("Timer ${1000 + i} must keep its color", Color.parseColor(colors[i]), notif.color)
            }
        }
    }

    @Test
    fun `stopped notification retains timer setting color and does not fall back to default`() {
        val customColor = "#9C27B0" // Purple
        val parsedCustomColor = Color.parseColor(customColor)
        val config = TimerConfig(id = "c1", name = "Baking", durationSec = 60, runningColor = customColor)
        val timer = Timer(id = 2000, configId = config.id, config = config, label = "Baking")

        val running = TimerStateMachine.start(timer, nowElapsedMs = clock.elapsedMs)
        val stopped = TimerStateMachine.stop(running)
        val visibleStopped = stopped.copy(isNotificationVisible = true)

        adapter.updateNotifications(listOf(visibleStopped))
        val notification = shadowOf(notificationManager).getNotification(2000)
        assertNotNull(notification)
        assertEquals(parsedCustomColor, notification.color)
        assertFalse(notification.extras.getBoolean(NotificationCompat.EXTRA_COLORIZED, false))
    }
}

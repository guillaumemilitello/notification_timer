package com.notification.timer.core.domain.engine

import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.model.Preset
import com.notification.timer.core.domain.model.Timer
import com.notification.timer.core.domain.model.TimerAction
import com.notification.timer.core.domain.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateMachineTest {
    private val samplePreset =
        Preset(
            id = "test_preset",
            name = "Workout",
            durationSec = 60,
            sets = 2,
            alerts =
                listOf(
                    Alert(name = "Get ready", beforeEndSec = 15, color = "#FF9800"),
                ),
        )

    private val baseTimer =
        Timer(
            id = 1000,
            presetId = samplePreset.id,
            config = samplePreset,
            label = "Workout",
        )

    @Test
    fun `actionsFor returns correct actions per state`() {
        val readyActions = TimerStateMachine.actionsFor(baseTimer)
        assertTrue(TimerAction.START in readyActions)
        assertFalse(TimerAction.PAUSE in readyActions)

        val runningTimer = TimerStateMachine.start(baseTimer, nowElapsedMs = 100000L)
        val runningActions = TimerStateMachine.actionsFor(runningTimer)
        assertTrue(TimerAction.PAUSE in runningActions)
        assertTrue(TimerAction.ADJUST_TIME in runningActions)

        val pausedTimer = TimerStateMachine.pause(runningTimer, nowElapsedMs = 110000L)
        val pausedActions = TimerStateMachine.actionsFor(pausedTimer)
        assertTrue(TimerAction.RESUME in pausedActions)
        assertFalse(TimerAction.START in pausedActions)
    }

    @Test
    fun `start, pause, resume transition correctly`() {
        val now = 100000L
        val running = TimerStateMachine.start(baseTimer, nowElapsedMs = now)
        assertTrue(running.state is TimerState.Running)
        assertEquals(now + 60000L, (running.state as TimerState.Running).endsAtElapsedMs)

        val pauseTime = now + 10000L // 10s passed
        val paused = TimerStateMachine.pause(running, nowElapsedMs = pauseTime)
        assertTrue(paused.state is TimerState.Paused)
        assertEquals(50000L, (paused.state as TimerState.Paused).remainingMs)

        val resumeTime = pauseTime + 5000L
        val resumed = TimerStateMachine.resume(paused, nowElapsedMs = resumeTime)
        assertTrue(resumed.state is TimerState.Running)
        assertEquals(resumeTime + 50000L, (resumed.state as TimerState.Running).endsAtElapsedMs)
    }

    @Test
    fun `adjustTime adds and subtracts remaining time`() {
        val now = 100000L
        val running = TimerStateMachine.start(baseTimer, nowElapsedMs = now)

        // Add 20 seconds
        val adjusted = TimerStateMachine.adjustTime(running, deltaSec = 20, nowElapsedMs = now)
        assertEquals(80000L, adjusted.remainingMs(now))

        // Subtract 30 seconds
        val decremented = TimerStateMachine.adjustTime(adjusted, deltaSec = -30, nowElapsedMs = now)
        assertEquals(50000L, decremented.remainingMs(now))
    }

    @Test
    fun `checkAlertsAndEnd triggers alert when crossing threshold`() {
        val now = 100000L
        val running = TimerStateMachine.start(baseTimer, nowElapsedMs = now)

        // Before threshold (15001ms remaining) does not trigger alert
        val beforeAlertTime = now + 44999L
        val (_, noAlertsYet) = TimerStateMachine.checkAlertsAndEnd(running, nowElapsedMs = beforeAlertTime)
        assertTrue(noAlertsYet.isEmpty())

        // Advance time to 15s remaining (now + 45s = 145000L)
        val alertTime = now + 45000L
        val (alertedTimer, alerts) = TimerStateMachine.checkAlertsAndEnd(running, nowElapsedMs = alertTime)

        assertEquals(1, alerts.size)
        assertEquals("Get ready", alerts[0].name)
        assertTrue((alertedTimer.state as TimerState.Running).firedAlertNames.contains("Get ready"))

        // Repeating check does not re-trigger alert
        val (_, noAlerts) = TimerStateMachine.checkAlertsAndEnd(alertedTimer, nowElapsedMs = alertTime + 1000L)
        assertTrue(noAlerts.isEmpty())
    }

    @Test
    fun `checkAlertsAndEnd handles set end and all sets done`() {
        val now = 100000L
        val runningSet1 = TimerStateMachine.start(baseTimer, nowElapsedMs = now)

        // Set 1 finishes -> goes directly to Set 2 Ready, completed, color stays green
        val (doneSet1, _) = TimerStateMachine.checkAlertsAndEnd(runningSet1, nowElapsedMs = now + 60000L)
        assertTrue(doneSet1.state is TimerState.Ready)
        assertTrue((doneSet1.state as TimerState.Ready).isCompleted)
        assertEquals(2, doneSet1.currentSet)
        assertEquals("#4CAF50", TimerStateMachine.computePhaseColor(doneSet1, now + 60000L))

        // Start set 2
        val runningSet2 = TimerStateMachine.start(doneSet1, nowElapsedMs = now + 60000L)
        assertEquals(2, runningSet2.currentSet)

        // Set 2 finishes -> all sets done, goes to Ready set 1, completed, color stays green
        val (doneSet2, _) = TimerStateMachine.checkAlertsAndEnd(runningSet2, nowElapsedMs = now + 120000L)
        assertTrue(doneSet2.state is TimerState.Ready)
        assertTrue((doneSet2.state as TimerState.Ready).isCompleted)
        assertEquals(1, doneSet2.currentSet)
        assertEquals("#4CAF50", TimerStateMachine.computePhaseColor(doneSet2, now + 120000L))
    }

    @Test
    fun `checkAlertsAndEnd suppresses done alert when more than 1 minute late`() {
        val now = 100000L
        val running = TimerStateMachine.start(baseTimer, nowElapsedMs = now)

        // Expired 61 seconds ago
        val expiredLateTime = now + 60000L + 61000L
        val (doneTimer, alerts) = TimerStateMachine.checkAlertsAndEnd(running, nowElapsedMs = expiredLateTime)

        assertTrue(doneTimer.state is TimerState.Ready)
        assertTrue((doneTimer.state as TimerState.Ready).isCompleted)
        assertTrue("Alert should be suppressed when more than 1 minute late", alerts.isEmpty())
    }

    @Test
    fun `checkAlertsAndEnd plays done alert when less than 1 minute late`() {
        val now = 100000L
        val running = TimerStateMachine.start(baseTimer, nowElapsedMs = now)

        // Expired 30 seconds ago
        val expiredRecentTime = now + 60000L + 30000L
        val (doneTimer, alerts) = TimerStateMachine.checkAlertsAndEnd(running, nowElapsedMs = expiredRecentTime)

        assertTrue(doneTimer.state is TimerState.Ready)
        assertTrue((doneTimer.state as TimerState.Ready).isCompleted)
        assertEquals(1, alerts.size)
        assertEquals("Time out!", alerts[0].name)
    }

    @Test
    fun `checkAlertsAndEnd suppresses intermediate alert when more than 1 minute late`() {
        val now = 100000L
        // Duration 120s (ends at 220000L), alert at 80s before end (due at 140000L)
        val alertPreset =
            samplePreset.copy(
                durationSec = 120,
                alerts = listOf(Alert(name = "Halfway", beforeEndSec = 80, color = "#FF9800")),
            )
        val longTimer = baseTimer.copy(config = alertPreset)
        val runningLong = TimerStateMachine.start(longTimer, nowElapsedMs = now)

        // Check at 140000L + 65000L = 205000L (still running, 15s remaining, 65s late for alert)
        val checkTime = 205000L
        val (updatedTimer, alerts) = TimerStateMachine.checkAlertsAndEnd(runningLong, nowElapsedMs = checkTime)

        assertTrue(updatedTimer.state is TimerState.Running)
        assertTrue(alerts.isEmpty())
        assertTrue((updatedTimer.state as TimerState.Running).firedAlertNames.contains("Halfway"))
    }

    @Test
    fun `updateColor updates runningColor and computePhaseColor reflects it`() {
        val updated = TimerStateMachine.updateColor(baseTimer, "#FF4336")
        assertEquals("#FF4336", updated.config.runningColor)
        assertEquals("#FF4336", TimerStateMachine.computePhaseColor(updated, 100000L))
    }

    @Test
    fun `notification visibility stays isolated across lifecycle transitions`() {
        assertFalse(baseTimer.isNotificationVisible)

        val started = TimerStateMachine.start(baseTimer, nowElapsedMs = 1_000L)
        assertTrue(started.isNotificationVisible)

        val paused = TimerStateMachine.pause(started, nowElapsedMs = 10_000L)
        assertTrue(paused.isNotificationVisible)

        // Swiping away notification keeps Paused state while hiding notification
        val swipedPaused = TimerStateMachine.dismissNotification(paused)
        assertTrue(swipedPaused.state is TimerState.Paused)
        assertFalse(swipedPaused.isNotificationVisible)

        // Resuming re-activates notification for this timer
        val resumed = TimerStateMachine.resume(swipedPaused, nowElapsedMs = 15_000L)
        assertTrue(resumed.isNotificationVisible)

        // Completing a set keeps notification visible for completion notice
        val (done, _) = TimerStateMachine.checkAlertsAndEnd(resumed, nowElapsedMs = 70_000L)
        assertTrue((done.state as TimerState.Ready).isCompleted)
        assertTrue(done.isNotificationVisible)

        // Closing/dismissing Done notification keeps green Done state on main screen
        val closedDone = TimerStateMachine.closeNotification(done)
        assertTrue((closedDone.state as TimerState.Ready).isCompleted)
        assertFalse(closedDone.isNotificationVisible)

        // Explicit close on a running notification stops countdown and hides notification
        val restarted = TimerStateMachine.start(closedDone, nowElapsedMs = 80_000L)
        val closedRunning = TimerStateMachine.closeNotification(restarted)
        assertTrue(closedRunning.state is TimerState.Ready)
        assertFalse((closedRunning.state as TimerState.Ready).isCompleted)
        assertFalse(closedRunning.isNotificationVisible)
    }
}

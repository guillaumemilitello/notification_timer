package com.notification.timer.core.runtime.notification

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.runtime.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeEventLog : EventLog {
    override fun log(
        tag: String,
        message: String,
    ) {}

    override fun getEntries() = emptyList<com.notification.timer.core.domain.diagnostics.LogEntry>()

    override fun clear() {}
}

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationRendererTest {
    private lateinit var context: Context
    private lateinit var renderer: NotificationRenderer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        renderer = NotificationRenderer(context, FakeEventLog())
    }

    @Test
    fun `channels created on initialization`() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val runningChannel = manager.getNotificationChannel(NotificationRenderer.CHANNEL_RUNNING)
        val alertChannel = manager.getNotificationChannel(NotificationRenderer.CHANNEL_ALERT)

        assertNotNull(runningChannel)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, runningChannel.importance)
        assertNotNull(alertChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alertChannel.importance)
    }

    @Test
    fun `running notification inflates cleanly with visible progress bar and play-pause next close buttons`() {
        val notification =
            renderer.renderTimerNotification(
                timerId = 1000,
                title = "Pasta",
                subtitle = "Set 1/3",
                remainingMs = 45_000L,
                durationMs = 60_000L,
                isRunning = true,
                showProgress = true,
                showNext = true,
                isForeground = true,
                phaseColor = Color.parseColor("#3F51B5"),
            )

        val root = notification.contentView.apply(context, FrameLayout(context))
        val progressBar = root.findViewById<ProgressBar>(R.id.progressBarTimer)
        val shortLabel = root.findViewById<TextView>(R.id.textViewSets_short)
        val subtitleLabel = root.findViewById<TextView>(R.id.textViewSets)
        val playPauseBtn = root.findViewById<ImageButton>(R.id.buttonPlayPause)
        val nextBtn = root.findViewById<ImageButton>(R.id.buttonNext)
        val closeBtn = root.findViewById<ImageButton>(R.id.buttonClose)

        assertEquals(View.VISIBLE, progressBar.visibility)
        assertEquals(250, progressBar.progress)
        assertEquals(NotificationRenderer.PROGRESS_MAX, progressBar.max)
        assertEquals(View.GONE, subtitleLabel.visibility)
        assertEquals("Pasta · Set 1/3", shortLabel.text.toString())

        assertEquals(View.VISIBLE, playPauseBtn.visibility)
        assertEquals(View.VISIBLE, nextBtn.visibility)
        assertTrue(nextBtn.isEnabled)
        assertEquals(View.VISIBLE, closeBtn.visibility)

        // Verify left-to-right button order: Play/Pause, Next, Close
        val buttonContainer = playPauseBtn.parent as LinearLayout
        assertEquals(3, buttonContainer.childCount)
        assertEquals(R.id.buttonPlayPause, buttonContainer.getChildAt(0).id)
        assertEquals(R.id.buttonNext, buttonContainer.getChildAt(1).id)
        assertEquals(R.id.buttonClose, buttonContainer.getChildAt(2).id)

        assertEquals(Color.parseColor("#3F51B5"), notification.color)
        assertFalse(
            "Native Material 3 style never forces setColorized(true)",
            notification.extras.getBoolean(NotificationCompat.EXTRA_COLORIZED, false),
        )
    }

    @Test
    fun `paused notification keeps progress bar visible at paused progress and stays in setting color`() {
        val notification =
            renderer.renderTimerNotification(
                timerId = 1001,
                title = "Workout",
                subtitle = "Paused",
                remainingMs = 30_000L,
                durationMs = 60_000L,
                isRunning = false,
                showProgress = true,
                showNext = true,
                isForeground = false,
                isOngoing = true,
                phaseColor = Color.parseColor("#FFFFFF"),
            )

        val root = notification.contentView.apply(context, FrameLayout(context))
        val progressBar = root.findViewById<ProgressBar>(R.id.progressBarTimer)
        val timerText = root.findViewById<TextView>(R.id.textViewTimerStatic)
        val chrono = root.findViewById<android.widget.Chronometer>(R.id.textViewTimer)

        assertEquals(View.VISIBLE, progressBar.visibility)
        assertEquals(500, progressBar.progress)
        assertEquals(View.GONE, chrono.visibility)
        assertEquals(View.VISIBLE, timerText.visibility)
        assertEquals("00:30", timerText.text.toString())

        assertEquals(Color.parseColor("#FFFFFF"), notification.color)
        assertFalse(notification.extras.getBoolean(NotificationCompat.EXTRA_COLORIZED, false))
    }

    @Test
    fun `done notification keeps default system theme color and is not colorized`() {
        val notification =
            renderer.renderTimerNotification(
                timerId = 1003,
                title = "Pasta",
                subtitle = "Done",
                remainingMs = 0L,
                durationMs = 60_000L,
                isRunning = false,
                showProgress = false,
                showNext = true,
                isAlerting = false,
                isDone = true,
                isOngoing = false,
                phaseColor = Color.parseColor("#4CAF50"),
            )

        val root = notification.contentView.apply(context, FrameLayout(context))
        val progressBar = root.findViewById<ProgressBar>(R.id.progressBarTimer)
        val subtitleLabel = root.findViewById<TextView>(R.id.textViewSets)
        val shortLabel = root.findViewById<TextView>(R.id.textViewSets_short)

        assertEquals(View.GONE, progressBar.visibility)
        assertEquals(View.VISIBLE, subtitleLabel.visibility)
        assertEquals("Done", subtitleLabel.text.toString())
        assertEquals("Pasta", shortLabel.text.toString())

        assertEquals(NotificationCompat.COLOR_DEFAULT, notification.color)
        assertFalse(notification.extras.getBoolean(NotificationCompat.EXTRA_COLORIZED, false))
    }

    @Test
    fun `alerting done notification hides progress bar and next button while showing subtitle`() {
        val notification =
            renderer.renderTimerNotification(
                timerId = 1002,
                title = "Pasta",
                subtitle = "Time out!",
                remainingMs = 0L,
                durationMs = 60_000L,
                isRunning = false,
                showProgress = false,
                showNext = false,
                isAlerting = true,
                isDone = true,
            )

        val root = notification.contentView.apply(context, FrameLayout(context))
        val progressBar = root.findViewById<ProgressBar>(R.id.progressBarTimer)
        val subtitleLabel = root.findViewById<TextView>(R.id.textViewSets)
        val nextBtn = root.findViewById<ImageButton>(R.id.buttonNext)
        val closeBtn = root.findViewById<ImageButton>(R.id.buttonClose)

        assertEquals(View.GONE, progressBar.visibility)
        assertEquals(View.VISIBLE, subtitleLabel.visibility)
        assertEquals("Time out!", subtitleLabel.text.toString())
        assertEquals(View.INVISIBLE, nextBtn.visibility)
        assertFalse(nextBtn.isEnabled)
        assertEquals(View.VISIBLE, closeBtn.visibility)
    }

    @Test
    fun `postNotification caches active notification and cancelNotification clears it`() {
        val notification =
            renderer.renderTimerNotification(
                timerId = 1005,
                title = "Tea",
                subtitle = "",
                remainingMs = 120_000L,
                durationMs = 120_000L,
                isRunning = true,
            )

        renderer.postNotification(1005, notification)
        assertNotNull(renderer.getActiveNotification(1005))

        renderer.cancelNotification(1005)
        assertNull(renderer.getActiveNotification(1005))
    }

    @Test
    fun `running chronometer base is offset by 999ms so integer floor division matches formatDisplayTime ceiling`() {
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val remainingMs = 45_200L // 45.2s remaining
        val endsAt = nowElapsed + remainingMs

        val notification =
            renderer.renderTimerNotification(
                timerId = 1006,
                title = "OffsetCheck",
                subtitle = "Set 1/1",
                remainingMs = remainingMs,
                durationMs = 60_000L,
                isRunning = true,
                chronometerBaseElapsedMs = endsAt,
            )

        val root = notification.contentView.apply(context, FrameLayout(context))
        val chronometer = root.findViewById<android.widget.Chronometer>(R.id.textViewTimer)

        // The base set on chronometer must be endsAt + 999L
        assertEquals(endsAt + NotificationRenderer.CHRONOMETER_CEILING_OFFSET_MS, chronometer.base)
        assertEquals(View.VISIBLE, chronometer.visibility)
        val staticView = root.findViewById<TextView>(R.id.textViewTimerStatic)
        assertEquals(View.GONE, staticView.visibility)

        // Chronometer's internal calculation: (base - now) / 1000
        val chronometerDisplayedSeconds = (chronometer.base - nowElapsed) / 1000L
        val formattedSeconds = ((remainingMs + 999L) / 1000L)
        assertEquals(46L, chronometerDisplayedSeconds)
        assertEquals(chronometerDisplayedSeconds, formattedSeconds)
        assertEquals(NotificationRenderer.formatDisplayTime(remainingMs), "00:46")
    }

    @Test
    fun `notification when remainingMs is zero or negative never displays negative static text`() {
        val notification =
            renderer.renderTimerNotification(
                timerId = 1007,
                title = "ZeroCheck",
                subtitle = "Set 1/1",
                remainingMs = -1500L,
                durationMs = 60_000L,
                isRunning = false,
            )

        val root = notification.contentView.apply(context, FrameLayout(context))
        val staticView = root.findViewById<TextView>(R.id.textViewTimerStatic)
        assertEquals(View.VISIBLE, staticView.visibility)
        assertEquals("00:00", staticView.text.toString())
    }

    @Test
    fun `running notification within final second freezes chronometer to static 00_00`() {
        val notification =
            renderer.renderTimerNotification(
                timerId = 1008,
                title = "FreezeCheck",
                subtitle = "Set 1/1",
                remainingMs = 800L,
                durationMs = 60_000L,
                isRunning = true,
                chronometerBaseElapsedMs = null,
            )

        val root = notification.contentView.apply(context, FrameLayout(context))
        val staticView = root.findViewById<TextView>(R.id.textViewTimerStatic)
        val chronometer = root.findViewById<android.widget.Chronometer>(R.id.textViewTimer)
        assertEquals(View.VISIBLE, staticView.visibility)
        assertEquals(View.GONE, chronometer.visibility)
        assertEquals("00:00", staticView.text.toString())
    }
}

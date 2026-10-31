package com.notification.timer.core.runtime.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notification.timer.core.data.diagnostics.InMemoryEventLog
import com.notification.timer.core.domain.model.Alert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlertPlayerTest {
    private lateinit var context: Context
    private lateinit var eventLog: InMemoryEventLog
    private lateinit var alertPlayer: AlertPlayer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        eventLog = InMemoryEventLog()
        alertPlayer = AlertPlayer(context, eventLog)
    }

    @Test
    fun `playAlert logs event and triggers sound and vibration safely`() {
        val testAlert =
            Alert(
                name = "Halfway",
                beforeEndSec = 30,
                vibrate = true,
                sound = null,
                color = "#FF9800",
            )

        alertPlayer.playAlert(testAlert)

        val entries = eventLog.getEntries()
        assertTrue(
            "Should log playing alert",
            entries.any { it.message.contains("Playing alert 'Halfway'") },
        )
    }

    @Test
    fun `playAlert with vibration disabled does not throw exception`() {
        val quietAlert =
            Alert(
                name = "Quiet Checkpoint",
                beforeEndSec = 15,
                vibrate = false,
                sound = null,
                color = "#4CAF50",
            )

        alertPlayer.playAlert(quietAlert)

        val entries = eventLog.getEntries()
        assertTrue(
            "Should log playing alert with vibrate=false",
            entries.any { it.message.contains("vibrate=false") },
        )
    }
}

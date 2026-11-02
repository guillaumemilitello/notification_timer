package com.notification.timer.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimerConfigFingerprintTest {
    @Test
    fun `fingerprint is identical for same duration, name, sets, color, and alerts`() {
        val config1 =
            TimerConfig(
                id = "id1",
                name = "Pasta",
                durationSec = 600,
                sets = 1,
                runningColor = "#3F51B5",
            )
        val config2 =
            TimerConfig(
                id = "id2", // different id
                name = "Pasta",
                durationSec = 600,
                sets = 1,
                runningColor = "#3F51B5",
            )

        assertEquals(config1.fingerprint(), config2.fingerprint())
    }

    @Test
    fun `fingerprint distinguishes different duration or sets`() {
        val base = TimerConfig(id = "base", name = "Timer", durationSec = 60, sets = 1)
        val diffDuration = base.copy(durationSec = 120)
        val diffSets = base.copy(sets = 2)
        val diffName = base.copy(name = "Different")
        val diffColor = base.copy(runningColor = "#E91E63")
        val diffSound = base.copy(done = base.done.copy(sound = "content://custom/sound"))
        val diffSoundEnabled = base.copy(done = base.done.copy(soundEnabled = false))
        val diffVibrate = base.copy(done = base.done.copy(vibrate = false))

        assertNotEquals(base.fingerprint(), diffDuration.fingerprint())
        assertNotEquals(base.fingerprint(), diffSets.fingerprint())
        assertNotEquals(base.fingerprint(), diffName.fingerprint())
        assertNotEquals(base.fingerprint(), diffColor.fingerprint())
        assertNotEquals(base.fingerprint(), diffSound.fingerprint())
        assertNotEquals(base.fingerprint(), diffSoundEnabled.fingerprint())
        assertNotEquals(base.fingerprint(), diffVibrate.fingerprint())
    }
}

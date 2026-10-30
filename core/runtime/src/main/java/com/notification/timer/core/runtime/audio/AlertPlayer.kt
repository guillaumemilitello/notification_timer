package com.notification.timer.core.runtime.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import com.notification.timer.core.domain.diagnostics.EventLog
import com.notification.timer.core.domain.model.Alert
import com.notification.timer.core.domain.ports.AlertPlayerPort
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertPlayer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val eventLog: EventLog,
    ) : AlertPlayerPort {
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        @SuppressLint("MissingPermission")
        override fun playAlert(alert: Alert) {
            eventLog.log("AlertPlayer", "Playing alert '${alert.name}' (vibrate=${alert.vibrate}, sound=${alert.sound})")

            val audioAttributes =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

            val focusRequest =
                AudioFocusRequest
                    .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .build()
            audioManager.requestAudioFocus(focusRequest)

            // Play Ringtone
            try {
                val soundUri =
                    alert.sound?.let { Uri.parse(it) }
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.audioAttributes = audioAttributes
                ringtone?.play()
            } catch (e: Exception) {
                eventLog.log("AlertPlayer", "Failed to play ringtone: ${e.message}")
            }

            // Trigger Vibration
            if (alert.vibrate) {
                vibrate()
            }
        }

        @SuppressLint("MissingPermission")
        private fun vibrate() {
            try {
                val vibrator = context.getSystemService(Vibrator::class.java)
                vibrator?.vibrate(VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (e: Exception) {
                eventLog.log("AlertPlayer", "Failed to vibrate: ${e.message}")
            }
        }
    }

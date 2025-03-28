package com.cbi.mobile_plantation.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class SoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Play a sound file from the raw resources folder
     * @param soundResourceId The resource ID of the sound file (e.g., R.raw.success_sound)
     */
    fun playSound(soundResourceId: Int) {
        try {
            // Release any previous media player
            releaseMediaPlayer()

            // Create and prepare a new media player
            mediaPlayer = MediaPlayer.create(context, soundResourceId)

            // Set completion listener to release resources when done
            mediaPlayer?.setOnCompletionListener {
                releaseMediaPlayer()
            }

            // Start playback
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Error playing sound: ${e.message}")
        }
    }

    /**
     * Release media player resources
     */
    fun releaseMediaPlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Error releasing media player: ${e.message}")
        }
    }
}
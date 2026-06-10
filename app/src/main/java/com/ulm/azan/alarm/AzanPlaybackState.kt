package com.ulm.azan.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks which prayer's azan is currently playing so the UI can show a
 * Play/Stop toggle. null means nothing is playing.
 */
object AzanPlaybackState {
    private val _playing = MutableStateFlow<String?>(null)
    val playing: StateFlow<String?> = _playing

    fun setPlaying(prayerKey: String?) {
        _playing.value = prayerKey
    }
}

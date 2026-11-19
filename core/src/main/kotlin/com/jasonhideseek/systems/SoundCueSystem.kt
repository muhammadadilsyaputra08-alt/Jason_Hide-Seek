package com.jasonhideseek.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

/** Efek suara prosedural + sistem "noise" yang bisa didengar bot. */
class SoundCueSystem {
    private val names = listOf("step", "slash", "break", "gore", "scream", "heartbeat", "alert", "creak", "hide",
        "key_pickup", "key_assembly", "door_unlock", "door_open", "door_locked", "click", "win", "lose")
    private val sounds = HashMap<String, Sound>()
    private var ambient: Music? = null
    private var heartTimer = 0f
    var listenerX = 0f
    var listenerY = 0f
    var enabled = true
    val noiseListeners = ArrayList<(Float, Float, Float) -> Unit>()

    init {
        for (n in names) {
            try { sounds[n] = Gdx.audio.newSound(Gdx.files.internal("audio/$n.wav")) } catch (e: Exception) { }
        }
    }

    fun play(name: String, volume: Float = 1f) {
        if (enabled) sounds[name]?.play(volume.coerceIn(0f, 1f))
    }

    /** Suara berposisi: volume mengecil menurut jarak ke pendengar (pemain). */
    fun playAt(name: String, x: Float, y: Float, volume: Float = 1f) {
        val dx = x - listenerX; val dy = y - listenerY
        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val v = volume * (1f - d / 320f)
        if (v > 0.02f) play(name, v)
    }

    fun emitNoise(x: Float, y: Float, radius: Float) {
        for (l in noiseListeners) l(x, y, radius)
    }

    private var musicName: String? = null

    private fun startMusic(name: String, volume: Float) {
        if (ambient != null && musicName == name) return
        stopAmbient()
        try {
            ambient = Gdx.audio.newMusic(Gdx.files.internal("audio/$name.wav")).apply {
                isLooping = true; this.volume = volume; play()
            }
            musicName = name
        } catch (e: Exception) { }
    }

    fun startAmbient() = startMusic("ambient", 0.35f)
    fun startMenuMusic() = startMusic("menu_music", 0.45f)

    fun stopAmbient() { ambient?.stop(); ambient?.dispose(); ambient = null; musicName = null }

    /** Detak jantung mengikuti ketegangan (0..1). */
    fun updateHeartbeat(dt: Float, tension: Float) {
        if (tension < 0.2f) { heartTimer = 0f; return }
        heartTimer -= dt
        if (heartTimer <= 0f) {
            play("heartbeat", 0.4f + 0.6f * tension)
            heartTimer = 1.1f - 0.7f * tension
        }
    }

    fun dispose() {
        stopAmbient()
        sounds.values.forEach { it.dispose() }
        sounds.clear()
    }
}

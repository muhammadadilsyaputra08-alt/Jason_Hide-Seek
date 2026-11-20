package com.jasonhideseek.game

import com.badlogic.gdx.Game
import com.jasonhideseek.screens.SplashScreen
import com.jasonhideseek.systems.SoundCueSystem

class JasonGame : Game() {
    lateinit var assets: Assets
    lateinit var sound: SoundCueSystem
    var selectedMap = 0
    /** true = pemain mengendalikan Jason, semua survivor dikendalikan AI. */
    var playAsJason = false

    override fun create() {
        assets = Assets()
        sound = SoundCueSystem()
        setScreen(SplashScreen(this))
    }

    override fun setScreen(screen: com.badlogic.gdx.Screen?) {
        val old = this.screen
        super.setScreen(screen)
        if (old != null && old !== screen) old.dispose()
    }

    override fun dispose() {
        screen?.dispose()
        sound.dispose()
        assets.dispose()
    }
}

package com.jasonhideseek.game

import com.badlogic.gdx.Game
import com.jasonhideseek.screens.MenuScreen
import com.jasonhideseek.systems.SoundCueSystem

class JasonGame : Game() {
    lateinit var assets: Assets
    lateinit var sound: SoundCueSystem
    var selectedMap = 0

    override fun create() {
        assets = Assets()
        sound = SoundCueSystem()
        setScreen(MenuScreen(this))
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

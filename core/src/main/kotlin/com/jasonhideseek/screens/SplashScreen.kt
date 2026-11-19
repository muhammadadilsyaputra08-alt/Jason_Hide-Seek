package com.jasonhideseek.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.game.JasonGame

/** Splash logo (branding baru), fade in/out lalu masuk Menu. Sentuh untuk melewati. */
class SplashScreen(private val game: JasonGame) : ScreenAdapter() {
    private val batch = SpriteBatch()
    private val cam = OrthographicCamera()
    private val vp = ExtendViewport(GameConfig.UI_W, GameConfig.UI_H, cam)
    private var t = 0f
    private var done = false

    override fun show() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean { next(); return true }
        }
    }

    private fun next() {
        if (done) return
        done = true
        game.setScreen(MenuScreen(game))
    }

    override fun render(delta: Float) {
        t += delta
        if (t >= DURATION) { next(); return }
        val alpha = when {
            t < 0.5f -> t / 0.5f
            t > DURATION - 0.5f -> (DURATION - t) / 0.5f
            else -> 1f
        }
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        vp.apply(); cam.update()
        batch.projectionMatrix = cam.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, alpha.coerceIn(0f, 1f))
        val tex = game.assets.splashLogo
        val h = GameConfig.UI_H * 0.8f
        val w = h * tex.width / tex.height
        batch.draw(tex, GameConfig.UI_W / 2f - w / 2f, GameConfig.UI_H / 2f - h / 2f, w, h)
        batch.end()
    }

    override fun resize(width: Int, height: Int) { vp.update(width, height, true) }
    override fun hide() { Gdx.input.inputProcessor = null }
    override fun dispose() { batch.dispose() }

    companion object { const val DURATION = 2.2f }
}

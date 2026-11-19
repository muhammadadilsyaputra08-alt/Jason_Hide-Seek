package com.jasonhideseek.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.game.JasonGame
import com.jasonhideseek.world.MapDefinition

class MenuScreen(private val game: JasonGame) : ScreenAdapter() {
    private val a = game.assets
    private val batch = SpriteBatch()
    private val cam = OrthographicCamera()
    private val vp = ExtendViewport(GameConfig.UI_W, GameConfig.UI_H, cam)
    private val layout = GlyphLayout()
    private val tmp = Vector2()
    private var time = 0f

    private val iconSize = 48f
    private val iconGap = 14f
    private val iconsX get() = (GameConfig.UI_W - (5 * iconSize + 4 * iconGap)) / 2f
    private val iconsY = 78f
    private val btnW = 144f
    private val btnH = 48f
    private val btnX get() = GameConfig.UI_W / 2f - btnW / 2f
    private val btnY = 12f

    override fun show() {
        game.sound.startMenuMusic()
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val p = vp.unproject(tmp.set(screenX.toFloat(), screenY.toFloat()))
                for (i in 0 until 5) {
                    val x = iconsX + i * (iconSize + iconGap)
                    if (p.x >= x && p.x <= x + iconSize && p.y >= iconsY && p.y <= iconsY + iconSize) {
                        game.selectedMap = i; game.sound.play("click", 0.6f); return true
                    }
                }
                if (p.x >= btnX && p.x <= btnX + btnW && p.y >= btnY && p.y <= btnY + btnH) start()
                return true
            }

            override fun keyDown(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.ENTER, Input.Keys.SPACE -> start()
                    Input.Keys.LEFT -> game.selectedMap = (game.selectedMap + 4) % 5
                    Input.Keys.RIGHT -> game.selectedMap = (game.selectedMap + 1) % 5
                }
                return true
            }
        }
    }

    private fun start() {
        game.sound.play("click", 0.8f)
        game.setScreen(GameScreen(game))
    }

    private fun center(text: String, y: Float, color: Color = Color.WHITE) {
        a.font.color = color
        layout.setText(a.font, text)
        a.font.draw(batch, layout, GameConfig.UI_W / 2f - layout.width / 2f, y)
    }

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(0.04f, 0.02f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        vp.apply(); cam.update()
        batch.projectionMatrix = cam.combined
        batch.begin()
        batch.draw(a.region("ui_vignette_dark"), 0f, 0f, GameConfig.UI_W, GameConfig.UI_H)

        // Logo baru
        val lh = 124f
        val lw = lh * a.logo.width / a.logo.height
        batch.draw(a.logo, GameConfig.UI_W / 2f - lw / 2f, GameConfig.UI_H - lh - 8f, lw, lh)
        center("Kumpulkan 3 Key Frame, buka pintu, kabur dari Jason", GameConfig.UI_H - lh - 14f, Color.LIGHT_GRAY)

        for (i in 0 until 5) {
            val x = iconsX + i * (iconSize + iconGap)
            val sel = i == game.selectedMap
            if (sel) {
                batch.setColor(0.8f, 0.1f, 0.1f, 1f)
                batch.draw(a.pixel, x - 3f, iconsY - 3f, iconSize + 6f, iconSize + 6f)
            }
            batch.setColor(if (sel) Color.WHITE else Color.GRAY)
            batch.draw(a.region("ui_icon_map${i + 1}"), x, iconsY)
            batch.setColor(Color.WHITE)
        }
        center(MapDefinition.NAMES[game.selectedMap], iconsY - 6f)
        batch.draw(a.region("ui_btn_play"), btnX, btnY, btnW, btnH)
        batch.end()
    }

    override fun resize(width: Int, height: Int) { vp.update(width, height, true) }
    override fun dispose() { batch.dispose() }
    override fun hide() { Gdx.input.inputProcessor = null }
}

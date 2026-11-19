package com.jasonhideseek.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
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
    private val names = (0 until 5).map { MapDefinition.NAMES[it] }

    private val iconSize = 48f
    private val iconGap = 14f
    private val iconsX get() = (GameConfig.UI_W - (5 * iconSize + 4 * iconGap)) / 2f
    private val iconsY = 110f
    private val startX = GameConfig.UI_W / 2f - 70f
    private val startY = 30f
    private val startW = 140f
    private val startH = 36f

    override fun show() {
        game.sound.stopAmbient()
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.BACK, true)
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val p = vp.unproject(tmp.set(screenX.toFloat(), screenY.toFloat()))
                for (i in 0 until 5) {
                    val x = iconsX + i * (iconSize + iconGap)
                    if (p.x >= x && p.x <= x + iconSize && p.y >= iconsY && p.y <= iconsY + iconSize) {
                        game.selectedMap = i; return true
                    }
                }
                if (p.x >= startX && p.x <= startX + startW && p.y >= startY && p.y <= startY + startH) start()
                return true
            }

            override fun keyDown(keycode: Int): Boolean {
                when (keycode) {
                    com.badlogic.gdx.Input.Keys.ENTER, com.badlogic.gdx.Input.Keys.SPACE -> start()
                    com.badlogic.gdx.Input.Keys.LEFT -> game.selectedMap = (game.selectedMap + 4) % 5
                    com.badlogic.gdx.Input.Keys.RIGHT -> game.selectedMap = (game.selectedMap + 1) % 5
                }
                return true
            }
        }
    }

    private fun start() { game.setScreen(GameScreen(game)) }

    private fun center(text: String, y: Float) {
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
        // Jason berjalan di latar
        val jf = a.frame("jason_walk_down", time, 6f)
        batch.draw(jf, GameConfig.UI_W / 2f - 48f, 168f, 96f, 96f)
        batch.draw(a.region("ui_vignette_dark"), 0f, 0f, GameConfig.UI_W, GameConfig.UI_H)

        a.font.color = com.badlogic.gdx.graphics.Color(0.85f, 0.1f, 0.1f, 1f)
        a.font.data.setScale(2f)
        center("JASON HIDE & SEEK", GameConfig.UI_H - 16f)
        a.font.data.setScale(1f)
        a.font.color = com.badlogic.gdx.graphics.Color.WHITE
        center("Pilih map lalu sembunyi sebelum Jason datang", 168f)

        for (i in 0 until 5) {
            val x = iconsX + i * (iconSize + iconGap)
            val sel = i == game.selectedMap
            if (sel) {
                batch.setColor(0.8f, 0.1f, 0.1f, 1f)
                batch.draw(a.pixel, x - 3f, iconsY - 3f, iconSize + 6f, iconSize + 6f)
            }
            batch.setColor(if (sel) com.badlogic.gdx.graphics.Color.WHITE else com.badlogic.gdx.graphics.Color.GRAY)
            batch.draw(a.region("ui_icon_map${i + 1}"), x, iconsY)
            batch.setColor(com.badlogic.gdx.graphics.Color.WHITE)
        }
        center(names[game.selectedMap], iconsY - 10f)

        batch.setColor(0.6f, 0.08f, 0.08f, 1f)
        batch.draw(a.pixel, startX, startY, startW, startH)
        batch.setColor(com.badlogic.gdx.graphics.Color.WHITE)
        center("MULAI", startY + startH / 2f + 7f)
        batch.end()
    }

    override fun resize(width: Int, height: Int) { vp.update(width, height, true) }
    override fun dispose() { batch.dispose() }
    override fun hide() { Gdx.input.inputProcessor = null }
}

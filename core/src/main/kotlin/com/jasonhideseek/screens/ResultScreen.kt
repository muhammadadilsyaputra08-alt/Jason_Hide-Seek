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
import com.jasonhideseek.systems.RoundResult

class ResultScreen(
    private val game: JasonGame,
    private val result: RoundResult,
    private val aliveCount: Int,
    private val mapName: String
) : ScreenAdapter() {
    private val a = game.assets
    private val batch = SpriteBatch()
    private val cam = OrthographicCamera()
    private val vp = ExtendViewport(GameConfig.UI_W, GameConfig.UI_H, cam)
    private val layout = GlyphLayout()
    private val tmp = Vector2()
    private val bw = 150f
    private val bh = 32f
    private val bx = GameConfig.UI_W / 2f - bw / 2f

    override fun show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val p = vp.unproject(tmp.set(screenX.toFloat(), screenY.toFloat()))
                if (p.x in bx..(bx + bw)) {
                    if (p.y in 80f..(80f + bh)) game.setScreen(GameScreen(game))
                    else if (p.y in 36f..(36f + bh)) game.setScreen(MenuScreen(game))
                }
                return true
            }

            override fun keyDown(keycode: Int): Boolean {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) game.setScreen(GameScreen(game))
                else if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) game.setScreen(MenuScreen(game))
                return true
            }
        }
    }

    private fun center(s: String, y: Float, color: Color = Color.WHITE) {
        a.font.color = color
        layout.setText(a.font, s)
        a.font.draw(batch, layout, GameConfig.UI_W / 2f - layout.width / 2f, y)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.03f, 0.01f, 0.04f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        vp.apply(); cam.update()
        batch.projectionMatrix = cam.combined
        batch.begin()
        val win = result == RoundResult.WIN
        batch.draw(a.region(if (win) "ui_badge_win" else "ui_badge_lose"), GameConfig.UI_W / 2f - 144f, 150f, 288f, 96f)
        center(if (win) "Kamu selamat dari Jason!" else "Jason menemukanmu...", 140f,
            if (win) Color.GREEN else Color(1f, 0.3f, 0.3f, 1f))
        center("Map: $mapName   |   Penyintas hidup: $aliveCount", 120f)
        batch.setColor(0.6f, 0.08f, 0.08f, 1f)
        batch.draw(a.pixel, bx, 80f, bw, bh)
        batch.setColor(0.25f, 0.25f, 0.3f, 1f)
        batch.draw(a.pixel, bx, 36f, bw, bh)
        batch.setColor(Color.WHITE)
        center("MAIN LAGI", 80f + bh / 2f + 6f)
        center("MENU", 36f + bh / 2f + 6f)
        batch.end()
    }

    override fun resize(width: Int, height: Int) { vp.update(width, height, true) }
    override fun hide() { Gdx.input.inputProcessor = null }
    override fun dispose() { batch.dispose() }
}

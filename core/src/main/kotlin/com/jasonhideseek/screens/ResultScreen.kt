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
import com.jasonhideseek.entities.SurvivorStatus
import com.jasonhideseek.game.JasonGame
import com.jasonhideseek.systems.MatchResult
import com.jasonhideseek.systems.MatchSummary

/** Result Screen: hasil, status pemain, escaped/eliminated, status Jason, durasi, tombol main lagi / menu. */
class ResultScreen(private val game: JasonGame, private val s: MatchSummary) : ScreenAdapter() {
    private val a = game.assets
    private val batch = SpriteBatch()
    private val cam = OrthographicCamera()
    private val vp = ExtendViewport(GameConfig.UI_W, GameConfig.UI_H, cam)
    private val layout = GlyphLayout()
    private val tmp = Vector2()
    private val bw = 120f
    private val bh = 36f
    private val againX = GameConfig.UI_W / 2f - bw - 8f
    private val menuX = GameConfig.UI_W / 2f + 8f
    private val by = 14f

    private val headline = when (s.result) {
        MatchResult.PLAYER_ESCAPED -> "Player Escaped"
        MatchResult.SURVIVORS_ESCAPED -> "Survivors Escaped"
        MatchResult.JASON_WINS -> "Jason Wins"
        MatchResult.TIMES_UP -> "Time's Up"
    }
    private val playerText = when (s.playerStatus) {
        SurvivorStatus.ALIVE -> "Masih bertahan"
        SurvivorStatus.ESCAPED -> "Escaped"
        SurvivorStatus.ELIMINATED -> "Eliminated"
    }
    private val jasonText = when {
        s.eliminated >= s.total -> "Menang - semua survivor tereliminasi"
        s.escaped > 0 -> "Gagal mencegah ${s.escaped} survivor kabur"
        else -> "Bertahan - tidak ada yang kabur"
    }

    override fun show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val p = vp.unproject(tmp.set(screenX.toFloat(), screenY.toFloat()))
                if (p.y in by..(by + bh)) {
                    if (p.x in againX..(againX + bw)) game.setScreen(GameScreen(game))
                    else if (p.x in menuX..(menuX + bw)) game.setScreen(MenuScreen(game))
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

    private fun center(text: String, y: Float, color: Color = Color.WHITE) {
        a.font.color = color
        layout.setText(a.font, text)
        a.font.draw(batch, layout, GameConfig.UI_W / 2f - layout.width / 2f, y)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.03f, 0.01f, 0.04f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        vp.apply(); cam.update()
        batch.projectionMatrix = cam.combined
        batch.begin()
        batch.draw(a.region("ui_vignette_dark"), 0f, 0f, GameConfig.UI_W, GameConfig.UI_H)
        val w = GameConfig.UI_W; val h = GameConfig.UI_H
        when (s.result) {
            MatchResult.PLAYER_ESCAPED -> batch.draw(a.region("ui_badge_win"), w / 2f - 144f, h - 110f, 288f, 96f)
            MatchResult.SURVIVORS_ESCAPED -> batch.draw(a.region("ui_result_escaped"), w / 2f - 128f, h - 90f, 256f, 64f)
            MatchResult.JASON_WINS -> batch.draw(a.region("ui_result_all_eliminated"), w / 2f - 160f, h - 90f, 320f, 64f)
            MatchResult.TIMES_UP -> batch.draw(a.region("ui_badge_lose"), w / 2f - 144f, h - 110f, 288f, 96f)
        }
        val good = s.result == MatchResult.PLAYER_ESCAPED || s.result == MatchResult.SURVIVORS_ESCAPED
        center(headline, h - 118f, if (good) Color.GREEN else Color(1f, 0.3f, 0.3f, 1f))
        center("Map: ${s.mapName}", h - 136f, Color.LIGHT_GRAY)
        center("Status pemain: $playerText", h - 152f)
        center("Escaped: ${s.escaped}    Eliminated: ${s.eliminated}    (dari ${s.total} survivor)", h - 168f)
        center("Key Frame: ${s.keysCollected}/${GameConfig.KEY_FRAME_COUNT}", h - 184f)
        center("Status Jason: $jasonText", h - 200f)
        center(String.format("Durasi: %02d:%02d", s.durationSec / 60, s.durationSec % 60), h - 216f)
        batch.draw(a.region("ui_btn_play"), againX, by, bw, bh)
        batch.draw(a.region("ui_btn_exit_menu"), menuX, by, bw, bh)
        batch.end()
    }

    override fun resize(width: Int, height: Int) { vp.update(width, height, true) }
    override fun hide() { Gdx.input.inputProcessor = null }
    override fun dispose() { batch.dispose() }
}

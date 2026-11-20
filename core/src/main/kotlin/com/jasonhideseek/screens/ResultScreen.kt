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
import com.jasonhideseek.ui.PixelViewport
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
    private val vp = PixelViewport(GameConfig.UI_W, GameConfig.UI_H, cam)
    private val layout = GlyphLayout()
    private val tmp = Vector2()
    private val W get() = vp.worldWidth
    private val H get() = vp.worldHeight
    private val bw = 120f
    private val bh = 36f
    private val againX get() = W / 2f - bw - 8f
    private val menuX get() = W / 2f + 8f
    private val by = 14f

    private val headline = when (s.result) {
        MatchResult.PLAYER_ESCAPED -> "Player Escaped"
        MatchResult.SURVIVORS_ESCAPED -> "Survivors Escaped"
        MatchResult.JASON_WINS -> "Jason Wins"
        MatchResult.TIMES_UP -> "Time's Up"
    }
    private val playerText = when (s.playerStatus) {
        SurvivorStatus.ALIVE -> if (s.escaper != null) "Gagal kabur (kunci dipegang ${s.escaper})" else "Masih bertahan"
        SurvivorStatus.ESCAPED -> "Escaped"
        SurvivorStatus.ELIMINATED -> "Eliminated"
    }
    private val jasonText = when {
        s.eliminated >= s.total -> "Menang - semua survivor tereliminasi"
        s.escaped > 0 -> "Gagal mencegah ${s.escaper ?: "1 survivor"} kabur"
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
        a.font.draw(batch, layout, W / 2f - layout.width / 2f, y)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.03f, 0.01f, 0.04f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        vp.apply(); cam.update()
        batch.projectionMatrix = cam.combined
        batch.begin()
        val w = W; val h = H
        batch.draw(a.region("ui_vignette_dark"), 0f, 0f, w, h)
        // Susunan vertikal: badge -> headline -> rincian -> tombol (tidak saling menumpuk)
        val top = h - 10f
        val region = when (s.result) {
            MatchResult.PLAYER_ESCAPED -> "ui_badge_win"
            MatchResult.SURVIVORS_ESCAPED -> "ui_result_escaped"
            MatchResult.JASON_WINS -> "ui_result_all_eliminated"
            MatchResult.TIMES_UP -> "ui_badge_lose"
        }
        val r = a.region(region)
        val bwid = r.regionWidth * 2f
        val badgeH = r.regionHeight * 2f
        batch.draw(r, w / 2f - bwid / 2f, top - badgeH, bwid, badgeH)
        var y = top - badgeH - 14f
        val good = s.result == MatchResult.PLAYER_ESCAPED
        center(headline, y, if (good) Color.GREEN else Color(1f, 0.3f, 0.3f, 1f)); y -= 20f
        center("Map: ${s.mapName}", y, Color.LIGHT_GRAY); y -= 16f
        center("Status pemain: $playerText", y); y -= 16f
        if (s.escaper != null) { center("Berhasil kabur: ${s.escaper}", y, Color.GOLD); y -= 16f }
        center("Kabur: ${s.escaped}   Tereliminasi: ${s.eliminated}   (dari ${s.total} survivor)", y); y -= 16f
        center("Key Frame milikmu: ${s.keysCollected}/${GameConfig.KEY_FRAME_COUNT}", y); y -= 16f
        center("Status Jason: $jasonText", y); y -= 16f
        center(String.format("Durasi: %02d:%02d", s.durationSec / 60, s.durationSec % 60), y)
        batch.draw(a.region("ui_btn_play"), againX, by, bw, bh)
        batch.draw(a.region("ui_btn_exit_menu"), menuX, by, bw, bh)
        batch.end()
    }

    override fun resize(width: Int, height: Int) { vp.update(width, height, true) }
    override fun hide() { Gdx.input.inputProcessor = null }
    override fun dispose() { batch.dispose() }
}

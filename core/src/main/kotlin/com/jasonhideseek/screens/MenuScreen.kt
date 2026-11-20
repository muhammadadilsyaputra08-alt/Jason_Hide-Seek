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
import com.jasonhideseek.game.JasonGame
import com.jasonhideseek.systems.MapEffect
import com.jasonhideseek.world.MapDefinition

class MenuScreen(private val game: JasonGame) : ScreenAdapter() {
    private val a = game.assets
    private val batch = SpriteBatch()
    private val cam = OrthographicCamera()
    private val vp = PixelViewport(GameConfig.UI_W, GameConfig.UI_H, cam)
    private val layout = GlyphLayout()
    private val tmp = Vector2()
    private var time = 0f

    // Semua posisi dihitung dari ukuran viewport saat ini (layar penuh), ditumpuk dari bawah ke atas
    private val W get() = vp.worldWidth
    private val H get() = vp.worldHeight
    private val iconSize = 48f
    private val iconGap = 14f
    private val btnW = 144f
    private val btnH = 48f
    private val btnX get() = W / 2f - btnW / 2f
    private val btnY = 12f
    private val nameY = btnY + btnH + 32f            // baseline nama map
    private val iconsY = nameY + 8f                  // dasar deretan ikon map
    private val iconsX get() = (W - (5 * iconSize + 4 * iconGap)) / 2f
    private val subtitleY = iconsY + iconSize + 22f  // baseline subtitle
    private val logoBottom = subtitleY + 10f

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
        a.font.draw(batch, layout, W / 2f - layout.width / 2f, y)
    }

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(0.04f, 0.02f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        vp.apply(); cam.update()
        batch.projectionMatrix = cam.combined
        batch.begin()
        batch.draw(a.region("ui_vignette_dark"), 0f, 0f, W, H)

        // Logo: mengisi sisa ruang di atas, tidak pernah menimpa elemen lain
        var lh = (H - 10f - logoBottom).coerceIn(56f, 130f)
        var lw = lh * a.logo.width / a.logo.height
        if (lw > W * 0.92f) { lw = W * 0.92f; lh = lw * a.logo.height / a.logo.width }
        batch.draw(a.logo, W / 2f - lw / 2f, H - lh - 10f, lw, lh)
        center("Bawa 3 Key Frame - hanya satu survivor yang bisa kabur dari Jason", subtitleY, Color.LIGHT_GRAY)

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
        center(MapDefinition.NAMES[game.selectedMap], nameY)
        val fx = MapEffect.forMap(game.selectedMap)
        center("Efek: ${fx.title} - ${fx.desc}", nameY - 15f, Color(1f, 0.75f, 0.4f, 1f))
        batch.draw(a.region("ui_btn_play"), btnX, btnY, btnW, btnH)
        batch.end()
    }

    override fun resize(width: Int, height: Int) { vp.update(width, height, true) }
    override fun dispose() { batch.dispose() }
    override fun hide() { Gdx.input.inputProcessor = null }
}

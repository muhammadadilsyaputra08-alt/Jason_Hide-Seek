package com.jasonhideseek.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.entities.JasonState
import com.jasonhideseek.entities.Survivor
import com.jasonhideseek.game.JasonGame
import com.jasonhideseek.systems.GameWorld
import com.jasonhideseek.systems.GoreAndDebrisSystem
import com.jasonhideseek.systems.Phase
import com.jasonhideseek.ui.TouchControls
import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.TileMapRenderer

class GameScreen(private val game: JasonGame) : ScreenAdapter() {
    private val a = game.assets
    private val world = GameWorld(game, game.selectedMap)
    private val batch = SpriteBatch()
    private val cam = OrthographicCamera()
    private val worldVp = ExtendViewport(GameConfig.WORLD_W, GameConfig.WORLD_H, cam)
    private val uiCam = OrthographicCamera()
    private val uiVp = ExtendViewport(GameConfig.UI_W, GameConfig.UI_H, uiCam)
    private val controls = TouchControls(uiVp, a)
    private val tiles = TileMapRenderer(world.map, a)
    private val layout = GlyphLayout()
    private val light: Texture = buildLightTexture()
    private val drawList = ArrayList<Any>()
    private val byDepth = Comparator<Any> { p, q -> sortY(q).compareTo(sortY(p)) }
    private val fillScratch = TextureRegion()
    private var time = 0f

    override fun show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = controls
        game.sound.startAmbient()
    }

    // ---------- Update ----------
    override fun render(delta: Float) {
        val dt = Math.min(delta, 0.05f)
        time += dt
        controls.pollKeyboard()
        controls.exitMode = world.player.hidden
        controls.hideButtonVisible = world.player.alive && (world.player.hidden || world.nearbySpot != null)
        world.update(dt, controls)

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(MenuScreen(game)); return
        }
        val res = world.result
        if (res != null && world.endTimer > 1.8f) {
            game.setScreen(ResultScreen(game, res, world.survivorsAlive, world.map.name)); return
        }

        drawWorld()
        drawUi()
    }

    // ---------- Dunia ----------
    private fun sortY(o: Any): Float = when (o) {
        is HidingSpot -> o.y - 10f
        is GoreAndDebrisSystem.Corpse -> o.y + 6f
        is Survivor -> {
            val sp = o.hidingIn
            if (sp != null) sp.y - 9.5f else o.y
        }
        is com.jasonhideseek.entities.JasonEntity -> o.y
        else -> 0f
    }

    private fun survivorFrame(s: Survivor): TextureRegion? {
        if (s.hidden) {
            val f = a.frames("${s.prefix}_hide")
            if (s.hideAnim < 0.3f) return f[Math.min(f.size - 1, (s.hideAnim / 0.1f).toInt())]
            return if (s === world.player) f[f.size - 1] else null
        }
        val d = s.dir.key
        return when {
            s.moving && s.sprinting -> a.frame("${s.prefix}_sprint_$d", s.animTime, 12f)
            s.moving -> a.frame("${s.prefix}_walk_$d", s.animTime, 8f)
            else -> a.frame("${s.prefix}_idle_$d", s.animTime, 2f)
        }
    }

    private fun drawWorld() {
        val p = world.player
        val halfW = cam.viewportWidth / 2f
        val halfH = cam.viewportHeight / 2f
        val cx = if (world.map.widthPx > halfW * 2f) MathUtils.clamp(p.x, halfW, world.map.widthPx - halfW) else world.map.widthPx / 2f
        val cy = if (world.map.heightPx > halfH * 2f) MathUtils.clamp(p.y + 8f, halfH, world.map.heightPx - halfH) else world.map.heightPx / 2f
        cam.position.set(Math.round(cx).toFloat(), Math.round(cy).toFloat(), 0f)
        cam.update()

        Gdx.gl.glClearColor(0.02f, 0.01f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        worldVp.apply()
        batch.projectionMatrix = cam.combined
        batch.begin()
        batch.setColor(0.78f, 0.78f, 0.92f, 1f)  // tint atmosferik
        val l = cam.position.x - halfW; val b = cam.position.y - halfH
        tiles.render(batch, l, b, l + halfW * 2f, b + halfH * 2f)
        world.gore.renderPools(batch)

        drawList.clear()
        drawList.addAll(world.spots)
        drawList.addAll(world.gore.corpses)
        for (s in world.survivors) if (s.alive) drawList.add(s)
        drawList.add(world.jason)
        drawList.sortWith(byDepth)
        for (o in drawList) drawObject(o)

        batch.setColor(1f, 1f, 1f, 1f)
        world.gore.renderEffects(batch)
        // Masker cahaya: gelap di tepi, terang di sekitar pemain
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(light, p.x - 500f, p.y - 500f, 1000f, 1000f)
        batch.end()
    }

    private fun drawObject(o: Any) {
        when (o) {
            is HidingSpot -> batch.draw(a.region(o.regionName), o.x - 16f, o.y - 16f)
            is GoreAndDebrisSystem.Corpse -> batch.draw(o.region, o.x, o.y)
            is com.jasonhideseek.entities.JasonEntity ->
                batch.draw(o.currentFrame(a), o.x - 24f, o.y - 4f)
            is Survivor -> {
                val f = survivorFrame(o) ?: return
                if (o.hidden) batch.setColor(0.78f, 0.78f, 0.92f, 0.55f)
                batch.draw(f, o.x - 16f, o.y - 4f)
                batch.setColor(0.78f, 0.78f, 0.92f, 1f)
            }
        }
    }

    // ---------- HUD ----------
    private fun text(s: String, x: Float, y: Float, color: Color = Color.WHITE) {
        a.font.color = color
        a.font.draw(batch, s, x, y)
    }

    private fun center(s: String, y: Float, color: Color = Color.WHITE) {
        a.font.color = color
        layout.setText(a.font, s)
        a.font.draw(batch, layout, GameConfig.UI_W / 2f - layout.width / 2f, y)
    }

    private fun bar(frame: String, fill: String, x: Float, y: Float, frac: Float) {
        val fr = a.region(frame)
        batch.draw(fr, x, y)
        val f = a.region(fill)
        fillScratch.setRegion(f)
        fillScratch.regionWidth = Math.max(0, (f.regionWidth * frac.coerceIn(0f, 1f)).toInt())
        if (fillScratch.regionWidth > 0) {
            batch.draw(fillScratch, x + (fr.regionWidth - f.regionWidth) / 2f, y + (fr.regionHeight - f.regionHeight) / 2f)
        }
    }

    private fun drawUi() {
        uiVp.apply()
        batch.projectionMatrix = uiCam.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        val w = GameConfig.UI_W; val h = GameConfig.UI_H

        // Vignette ketegangan
        batch.setColor(1f, 1f, 1f, 0.85f)
        batch.draw(a.region("ui_vignette_dark"), 0f, 0f, w, h)
        if (world.tension > 0.05f) {
            batch.setColor(1f, 1f, 1f, world.tension * (0.55f + 0.15f * MathUtils.sin(time * 8f)))
            batch.draw(a.region("ui_vignette_red"), 0f, 0f, w, h)
        }
        batch.setColor(1f, 1f, 1f, 1f)

        // Panel timer
        val panel = a.region("ui_hud_panel")
        batch.draw(panel, w / 2f - 48f, h - 40f, 96f, 36f)
        val sec = Math.max(0, Math.ceil(world.phaseRemaining.toDouble()).toInt())
        val timer = String.format("%02d:%02d", sec / 60, sec % 60)
        val cdPhase = world.phase == Phase.COUNTDOWN
        batch.draw(a.region(if (cdPhase) "ui_icon_phase_countdown" else "ui_icon_phase_hunt"), w / 2f - 44f, h - 32f)
        text(timer, w / 2f - 22f, h - 14f, if (cdPhase) Color.YELLOW else Color.WHITE)

        // Penyintas
        for ((i, s) in world.survivors.withIndex()) {
            batch.draw(a.region(if (s.alive) "ui_icon_survivor_alive" else "ui_icon_survivor_dead"), 8f + i * 19f, h - 24f)
        }

        // Ketegangan
        batch.draw(a.region("ui_icon_tension_eye"), w - 104f, h - 28f)
        bar("ui_bar_tension_frame", "ui_bar_tension_fill", w - 84f, h - 24f, world.tension)

        // Stamina
        bar("ui_bar_stamina_frame", "ui_bar_stamina_fill", w - 128f, 74f,
            world.player.stamina / GameConfig.STAMINA_MAX)

        controls.render(batch)

        // Pesan status
        val p = world.player
        when {
            cdPhase && !p.hidden -> center("Cari tempat sembunyi!", h - 52f, Color.YELLOW)
            p.hidden && world.jason.state == JasonState.CHASE -> center("Tahan napas...", h - 52f, Color.GRAY)
            p.hidden -> center("Bersembunyi", h - 52f, Color.GRAY)
            world.jason.chasing -> {
                batch.draw(a.region("ui_icon_warning"), w / 2f - 24f, h - 100f)
                center("JASON MENGEJAR!", h - 104f, Color(1f, 0.25f, 0.25f, 1f))
            }
        }
        if (world.result != null) {
            val win = world.result == com.jasonhideseek.systems.RoundResult.WIN
            batch.draw(a.region(if (win) "ui_badge_win" else "ui_badge_lose"), w / 2f - 144f, h / 2f - 48f, 288f, 96f)
        }
        a.font.color = Color.WHITE
        batch.end()
    }

    private fun buildLightTexture(): Texture {
        val n = 64
        val pm = Pixmap(n, n, Pixmap.Format.RGBA8888)
        pm.blending = Pixmap.Blending.None
        for (y in 0 until n) for (x in 0 until n) {
            val dx = (x + 0.5f - n / 2f) / (n / 2f)
            val dy = (y + 0.5f - n / 2f) / (n / 2f)
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            var al = Math.min(0.92f, 0.95f * (d * 1.6f) * (d * 1.6f))
            al = Math.round(al * 7f) / 7f   // kuantisasi: gaya pixel-art
            pm.setColor(0.01f, 0f, 0.04f, al)
            pm.drawPixel(x, y)
        }
        val t = Texture(pm)
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        pm.dispose()
        return t
    }

    override fun resize(width: Int, height: Int) {
        worldVp.update(width, height, false)
        uiVp.update(width, height, true)
    }

    override fun hide() {
        game.sound.stopAmbient()
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        world.dispose()
        batch.dispose()
        light.dispose()
    }
}

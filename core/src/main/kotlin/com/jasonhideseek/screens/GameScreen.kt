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
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.MathUtils
import com.jasonhideseek.ui.PixelViewport
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.config.GameConfig.TILE
import com.jasonhideseek.entities.JasonEntity
import com.jasonhideseek.entities.JasonState
import com.jasonhideseek.entities.Survivor
import com.jasonhideseek.entities.SurvivorStatus
import com.jasonhideseek.game.JasonGame
import com.jasonhideseek.systems.ContextAction
import com.jasonhideseek.systems.FeedKind
import com.jasonhideseek.systems.GameWorld
import com.jasonhideseek.systems.GoreAndDebrisSystem
import com.jasonhideseek.systems.MatchResult
import com.jasonhideseek.systems.Phase
import com.jasonhideseek.ui.TouchControls
import com.jasonhideseek.world.ExitState
import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.PropDef
import com.jasonhideseek.world.TileMapRenderer

class GameScreen(private val game: JasonGame) : ScreenAdapter() {
    private val a = game.assets
    private val world = GameWorld(game, game.selectedMap)
    private val map = world.map
    private val batch = SpriteBatch()
    private val cam = OrthographicCamera()
    private val worldVp = PixelViewport(GameConfig.WORLD_W, GameConfig.WORLD_H, cam)
    private val uiCam = OrthographicCamera()
    private val uiVp = PixelViewport(GameConfig.UI_W, GameConfig.UI_H, uiCam)
    private val controls = TouchControls(uiVp, a)
    private val tiles = TileMapRenderer(map, a)
    private val layout = GlyphLayout()
    private val drawList = ArrayList<Any>()
    private val byDepth = Comparator<Any> { p, q -> sortY(q).compareTo(sortY(p)) }
    private val fillScratch = TextureRegion()
    private var time = 0f

    // Jason Mini Camera (render ke FrameBuffer 90x54)
    private val miniCam = OrthographicCamera(GameConfig.MINICAM_W.toFloat(), GameConfig.MINICAM_H.toFloat())
    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, GameConfig.MINICAM_W, GameConfig.MINICAM_H, false)
    private val fboRegion: TextureRegion

    // Kamera utama
    private var camX = world.player.x
    private var camY = world.player.y

    init {
        fbo.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        fboRegion = TextureRegion(fbo.colorBufferTexture)
        fboRegion.flip(false, true)
    }

    override fun show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = controls
        game.sound.startAmbient()
    }

    // ---------- Update ----------
    override fun render(delta: Float) {
        val dt = Math.min(delta, 0.05f)
        time += dt
        controls.spectatorMode = world.spectating && world.phase != Phase.ENDED
        controls.pollKeyboard()
        controls.actionIcon = when (world.playerContext) {
            ContextAction.HIDE -> "ui_btn_hide"
            ContextAction.LEAVE -> "ui_btn_exit"
            ContextAction.EXIT -> "ui_btn_interact"
            ContextAction.NONE -> null
        }
        val step = controls.consumeSpectateStep()
        if (step != 0 && world.spectating) world.cycleSpectate(step)
        world.update(dt, controls)

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(MenuScreen(game)); return
        }
        if (world.result != null && world.endTimer > 2.4f) {
            game.setScreen(ResultScreen(game, world.summary())); return
        }

        drawMiniCam()
        drawWorld(dt)
        drawUi()
    }

    // ---------- Urutan gambar (depth sorting sumbu y) ----------
    private fun sortY(o: Any): Float = when (o) {
        is HidingSpot -> o.y - 10f
        is GoreAndDebrisSystem.Corpse -> o.y + 6f
        is PropDef -> (map.tilesH - 1 - o.row) * TILE.toFloat() + 4f
        is Survivor -> {
            val sp = o.hidingIn
            if (sp != null) sp.y - 9.5f else o.y
        }
        is JasonEntity -> o.y
        else -> 0f
    }

    private fun survivorFrame(s: Survivor): TextureRegion? {
        val act = s.action
        if (act != null) {
            val f = a.frames("${s.prefix}_${act.frames}")
            return f[Math.min(f.size - 1, (s.actionTime / s.actionDuration * f.size).toInt())]
        }
        if (s.hidden) {
            if (s.hideAnim < 0.3f) {
                val f = a.frames("${s.prefix}_hide")
                return f[Math.min(f.size - 1, (s.hideAnim / 0.1f).toInt())]
            }
            return if (s === world.player) a.frame("${s.prefix}_hidden", time, 2f) else null
        }
        val d = s.dir.key
        return when {
            s.moving && s.sprinting -> a.frame("${s.prefix}_sprint_$d", s.animTime, 12f)
            s.moving -> a.frame("${s.prefix}_walk_$d", s.animTime, 8f)
            else -> a.frame("${s.prefix}_idle_$d", s.animTime, 2f)
        }
    }

    private fun drawExit() {
        val suffix = map.exitOrientation.suffix
        val region = when (world.exitState) {
            ExitState.LOCKED -> a.region("exit_locked_$suffix")
            ExitState.UNLOCKING -> {
                val f = a.frames("exit_unlock_$suffix")
                f[Math.min(f.size - 1, (world.exitAnimTime / GameConfig.EXIT_UNLOCK_TIME * f.size).toInt())]
            }
            ExitState.OPEN -> a.region("exit_open_$suffix")
        }
        batch.draw(region, map.exitCol * TILE.toFloat(), (map.tilesH - 1 - map.exitRow) * TILE.toFloat())
    }

    /** Menggambar seluruh dunia dengan kamera tertentu. Dipakai kamera utama dan Jason Mini Camera. */
    private fun renderScene(c: OrthographicCamera, tr: Float, tg: Float, tb: Float, lightAt: Survivor?) {
        val halfW = c.viewportWidth / 2f
        val halfH = c.viewportHeight / 2f
        batch.projectionMatrix = c.combined
        batch.begin()
        batch.setColor(tr, tg, tb, 1f)
        val l = c.position.x - halfW; val b = c.position.y - halfH
        tiles.render(batch, l, b, l + halfW * 2f, b + halfH * 2f)
        tiles.renderDecor(batch, "floor")
        world.gore.renderPools(batch)
        drawExit()
        tiles.renderDecor(batch, "wall")

        drawList.clear()
        drawList.addAll(world.spots)
        drawList.addAll(world.gore.corpses)
        for (p in map.props) if (p.layer == "object") drawList.add(p)
        for (s in world.survivors) if (s.status == SurvivorStatus.ALIVE) drawList.add(s)
        drawList.add(world.jason)
        drawList.sortWith(byDepth)
        for (o in drawList) drawObject(o, tr, tg, tb)

        // Key Frame hanya terlihat dari dekat (sulit ditemukan); tidak tampil di Jason Mini Camera
        if (lightAt != null) {
            for (k in world.keys) {
                if (k.holder != null) continue
                if (lightAt.dist(k.x, k.y) > GameConfig.KEY_VIEW_RANGE * world.keyViewMul) continue
                val bob = MathUtils.sin(time * 4f + k.number) * 2f
                batch.draw(a.region("key_frame_${k.number}"), k.x - 8f, k.y - 2f + bob)
            }
        }
        if (lightAt != null && world.alarmActive) {
            batch.draw(a.region("ui_icon_warning"), world.alarmX - 8f, world.alarmY + 6f + MathUtils.sin(time * 10f) * 2f)
        }
        world.gore.renderEffects(batch)
        batch.setColor(1f, 1f, 1f, 1f)
        if (lightAt != null) drawNameTags()
        batch.end()
    }

    private fun drawObject(o: Any, tr: Float, tg: Float, tb: Float) {
        when (o) {
            is HidingSpot -> batch.draw(a.region(o.regionName), o.x - 16f, o.y - 16f)
            is GoreAndDebrisSystem.Corpse -> batch.draw(o.region, o.x, o.y)
            is PropDef -> batch.draw(a.region(o.item), o.col * TILE.toFloat(), (map.tilesH - 1 - o.row) * TILE.toFloat())
            is JasonEntity -> {
                if (world.buffs.any) batch.setColor(1f, 0.72f, 0.72f, 1f)   // aura merah saat berbuff
                batch.draw(o.currentFrame(a), o.x - 32f, o.y - 13f)
                batch.setColor(tr, tg, tb, 1f)
            }   // 64x64, pivot (32,51)
            is Survivor -> {
                val f = survivorFrame(o) ?: return
                if (o.hidden) batch.setColor(tr, tg, tb, 0.55f)
                batch.draw(f, o.x - 16f, o.y - 4f)                                  // 32x32, pivot (16,28)
                batch.setColor(tr, tg, tb, 1f)
            }
        }
    }

    /** Nama survivor di atas kepala (font kecil). Pembawa Key Frame diberi penanda jumlah. */
    private fun drawNameTags() {
        val f = a.font
        f.data.setScale(0.55f)
        for (sv in world.survivors) {
            if (!sv.alive || sv.hidden) continue
            val tag = if (sv.heldKeys.isEmpty()) sv.name else "${sv.name} [${sv.heldKeys.size}]"
            layout.setText(f, tag)
            val c = when {
                sv === world.player -> Color(0.55f, 1f, 0.55f, 1f)
                sv.heldKeys.isNotEmpty() -> Color.GOLD
                else -> Color(1f, 1f, 1f, 0.8f)
            }
            f.color = c
            f.draw(batch, layout, sv.x - layout.width / 2f, sv.y + 36f)
        }
        f.data.setScale(1f)
        f.color = Color.WHITE
    }

    private fun drawMiniCam() {
        miniCam.position.set(Math.round(world.jason.x).toFloat(), Math.round(world.jason.y).toFloat() + 4f, 0f)
        miniCam.update()
        fbo.begin()
        Gdx.gl.glClearColor(0f, 0.02f, 0.02f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        renderScene(miniCam, 0.55f, 0.95f, 0.65f, null)
        fbo.end()
    }

    private fun drawWorld(dt: Float) {
        val f = world.focus
        val halfW = cam.viewportWidth / 2f
        val halfH = cam.viewportHeight / 2f
        val tx = if (map.widthPx > halfW * 2f) MathUtils.clamp(f.x, halfW, map.widthPx - halfW) else map.widthPx / 2f
        val ty = if (map.heightPx > halfH * 2f) MathUtils.clamp(f.y + 8f, halfH, map.heightPx - halfH) else map.heightPx / 2f
        if (world.spectating) {
            val k = Math.min(1f, dt * 6f)
            camX += (tx - camX) * k; camY += (ty - camY) * k
        } else { camX = tx; camY = ty }
        cam.position.set(Math.round(camX).toFloat(), Math.round(camY).toFloat(), 0f)
        cam.update()

        Gdx.gl.glClearColor(0.02f, 0.01f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        worldVp.apply()
        renderScene(cam, 1f, 1f, 1f, f)
    }

    // ---------- HUD ----------
    private fun text(s: String, x: Float, y: Float, color: Color = Color.WHITE) {
        a.font.color = color
        a.font.draw(batch, s, x, y)
    }

    private fun center(s: String, y: Float, color: Color = Color.WHITE) {
        a.font.color = color
        layout.setText(a.font, s)
        a.font.draw(batch, layout, uiVp.worldWidth / 2f - layout.width / 2f, y)
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

    private fun rtext(s: String, rx: Float, y: Float, color: Color) {
        a.font.color = color
        layout.setText(a.font, s)
        a.font.draw(batch, layout, rx - layout.width, y)
    }

    private fun scaled(sc: Float, block: () -> Unit) {
        a.font.data.setScale(sc); block(); a.font.data.setScale(1f)
    }

    private fun drawUi() {
        uiVp.apply()
        batch.projectionMatrix = uiCam.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        // Semua elemen HUD menempel ke tepi layar sebenarnya (layar penuh), bukan ke ukuran virtual tetap
        val w = uiVp.worldWidth; val h = uiVp.worldHeight
        val pad = 8f
        val p = world.player

        // Vignette (atmosfer horor)
        batch.setColor(1f, 1f, 1f, 0.3f)
        batch.draw(a.region("ui_vignette_dark"), 0f, 0f, w, h)
        if (world.tension > 0.05f) {
            batch.setColor(1f, 1f, 1f, world.tension * (0.55f + 0.15f * MathUtils.sin(time * 8f)))
            batch.draw(a.region("ui_vignette_red"), 0f, 0f, w, h)
        }
        batch.setColor(1f, 1f, 1f, 1f)

        // Overlay efek map (tipis, tidak mengotori visual)
        if (world.effect == com.jasonhideseek.systems.MapEffect.FOG) {
            batch.setColor(0.85f, 0.9f, 0.85f, 0.12f); batch.draw(a.pixel, 0f, 0f, w, h)
        }
        if (world.blackoutAlpha > 0.01f) {
            batch.setColor(0f, 0f, 0f, world.blackoutAlpha); batch.draw(a.pixel, 0f, 0f, w, h)
        }
        batch.setColor(1f, 1f, 1f, 1f)

        // ---- Tengah-atas: Escape Timer, slot Key Frame, info pembawa, status ----
        val cdPhase = world.phase == Phase.COUNTDOWN
        val timerY = h - pad - 36f
        batch.draw(a.region("ui_hud_panel"), w / 2f - 48f, timerY, 96f, 36f)
        val sec = Math.max(0, Math.ceil(world.phaseRemaining.toDouble()).toInt())
        batch.draw(a.region(if (cdPhase) "ui_icon_phase_countdown" else "ui_icon_timer"), w / 2f - 44f, timerY + 8f)
        val low = !cdPhase && sec <= 30
        text(String.format("%02d:%02d", sec / 60, sec % 60), w / 2f - 22f, timerY + 24f,
            if (cdPhase) Color.YELLOW else if (low) Color(1f, 0.3f, 0.3f, 1f) else Color.WHITE)

        val keyY = timerY - 20f
        val kx = w / 2f - 40f
        for (i in 0 until GameConfig.KEY_FRAME_COUNT) {
            val held = p.heldKeys.getOrNull(i)
            batch.draw(a.region(if (held != null) "ui_key_slot_${held.number}" else "ui_key_slot_empty"), kx + i * 18f, keyY)
        }
        if (p.hasCompleteKey) batch.draw(a.region("ui_icon_key_complete"), kx + 56f, keyY)
        text("${p.heldKeys.size}/${GameConfig.KEY_FRAME_COUNT}", kx + (if (p.hasCompleteKey) 76f else 58f), keyY + 12f,
            if (p.hasCompleteKey) Color.GOLD else Color.WHITE)

        val holder = world.keyHolder
        val holderLine = when {
            holder == null -> "Key Frame di lantai: ${world.keysOnGround}"
            holder === p -> "Kamu pembawa kunci (${p.heldKeys.size}/${GameConfig.KEY_FRAME_COUNT})"
            else -> "Pembawa: ${holder.name} (${holder.heldKeys.size}/${GameConfig.KEY_FRAME_COUNT})"
        }
        scaled(0.8f) { center(holderLine, keyY - 4f, if (holder != null) Color.GOLD else Color.LIGHT_GRAY) }

        // ---- Kiri-atas: status survivor + notifikasi (feed) ----
        for ((i, s) in world.survivors.withIndex()) {
            val icon = when (s.status) {
                SurvivorStatus.ALIVE -> "ui_icon_survivor_alive"
                SurvivorStatus.ESCAPED -> "ui_icon_survivor_escaped"
                SurvivorStatus.ELIMINATED -> "ui_icon_survivor_dead"
            }
            batch.draw(a.region(icon), pad + i * 17f, h - pad - 16f)
        }
        scaled(0.85f) {
            text("Hidup ${world.survivorsAlive}  Kabur ${world.escapedCount}  Tewas ${world.eliminatedCount}",
                pad, h - pad - 20f, Color.LIGHT_GRAY)
        }
        var fy = h - pad - 38f
        scaled(0.85f) {
            for (f in world.feed.reversed()) {
                val c = Color(when (f.kind) {
                    FeedKind.KILL -> Color(1f, 0.35f, 0.35f, 1f)
                    FeedKind.KEY -> Color.GOLD
                    FeedKind.ESCAPE -> Color(0.5f, 1f, 0.5f, 1f)
                    FeedKind.INFO -> Color.WHITE
                    FeedKind.BUFF -> Color(0.85f, 0.5f, 1f, 1f)
                })
                c.a = Math.min(1f, f.ttl / 1.5f)
                text(f.text, pad, fy, c)
                fy -= 13f
            }
        }

        // ---- Kanan-atas: ketegangan + Jason Mini Camera ----
        val rx = w - pad - 96f
        batch.draw(a.region("ui_icon_tension_eye"), rx, h - pad - 16f)
        bar("ui_bar_tension_frame", "ui_bar_tension_fill", rx + 24f, h - pad - 12f, world.tension)
        val mx = rx; val my = h - pad - 20f - 64f
        batch.draw(fboRegion, mx + 3f, my + 3f, GameConfig.MINICAM_W.toFloat(), GameConfig.MINICAM_H.toFloat())
        batch.draw(a.region("ui_minicam_frame"), mx, my)
        batch.draw(a.frame("ui_minicam_rec", time, 2f), mx + 4f, my + 55f)

        // Efek map + buff Jason aktif (rata kanan, di bawah mini camera)
        var ry = my - 8f
        scaled(0.8f) {
            rtext("Efek: ${world.effect.title}", w - pad, ry, Color.LIGHT_GRAY); ry -= 12f
            for ((b, left) in world.buffs.active) {
                rtext("${b.title} ${Math.ceil(left.toDouble()).toInt()}s", w - pad, ry, Color(1f, 0.5f, 0.5f, 1f)); ry -= 12f
            }
        }

        // ---- Kanan-bawah: stamina di atas tombol Sprint ----
        if (!world.spectating) {
            bar("ui_bar_stamina_frame", "ui_bar_stamina_fill", controls.sprintX, controls.sprintY + 52f, p.stamina / GameConfig.STAMINA_MAX)
        }
        controls.render(batch)

        // ---- Pesan status (di bawah info pembawa kunci) ----
        val statusY = keyY - 24f
        if (world.spectating) {
            batch.draw(a.region("ui_spectator_badge"), w / 2f - 44f, statusY - 12f)
            val t = world.spectateTarget
            center(if (t != null) "Mengamati: ${world.label(t)}" else "Tidak ada survivor tersisa", 48f, Color.LIGHT_GRAY)
        } else when {
            p.status == SurvivorStatus.ESCAPED -> {}
            cdPhase && !p.hidden -> center("Cari tempat sembunyi!", statusY, Color.YELLOW)
            p.hidden && world.jason.state == JasonState.CHASE -> center("Tahan napas...", statusY, Color.GRAY)
            p.hidden -> center("Bersembunyi", statusY, Color.GRAY)
            world.jason.chasing -> {
                layout.setText(a.font, "JASON MENGEJAR!")
                batch.draw(a.region("ui_icon_warning"), w / 2f - layout.width / 2f - 20f, statusY - 12f)
                center("JASON MENGEJAR!", statusY, Color(1f, 0.25f, 0.25f, 1f))
            }
        }

        // ---- Notifikasi besar: eliminasi / kunci / kabur, lalu toast ----
        val bt = world.bannerText
        if (bt != null && world.result == null) {
            val c = Color(when (world.bannerKind) {
                FeedKind.KILL -> Color(1f, 0.2f, 0.2f, 1f)
                FeedKind.KEY -> Color.GOLD
                FeedKind.ESCAPE -> Color(0.5f, 1f, 0.5f, 1f)
                FeedKind.INFO -> Color.WHITE
                FeedKind.BUFF -> Color(0.85f, 0.5f, 1f, 1f)
            })
            c.a = Math.min(1f, world.bannerTimer / 0.6f)
            scaled(1.4f) { center(bt.uppercase(), h * 0.66f, c) }
        }
        val toast = world.toast
        if (toast != null && world.result == null) center(toast, h * 0.5f + 20f, Color.GOLD)

        val res = world.result
        if (res != null) {
            when (res) {
                MatchResult.PLAYER_ESCAPED ->
                    batch.draw(a.region("ui_badge_win"), w / 2f - 144f, h / 2f - 48f, 288f, 96f)
                MatchResult.SURVIVORS_ESCAPED ->
                    batch.draw(a.region("ui_result_escaped"), w / 2f - 128f, h / 2f - 32f, 256f, 64f)
                MatchResult.JASON_WINS ->
                    batch.draw(a.region("ui_result_all_eliminated"), w / 2f - 160f, h / 2f - 32f, 320f, 64f)
                MatchResult.TIMES_UP -> center("WAKTU HABIS", h / 2f + 8f, Color(1f, 0.3f, 0.3f, 1f))
            }
        }
        a.font.color = Color.WHITE
        batch.end()
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
        fbo.dispose()
    }
}

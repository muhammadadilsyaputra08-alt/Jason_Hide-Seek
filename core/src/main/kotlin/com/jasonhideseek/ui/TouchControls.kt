package com.jasonhideseek.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.game.Assets

/** Virtual joystick (melayang, sisi kiri) + tombol Sprint / Hide-Exit (sisi kanan). Juga mendukung keyboard. */
class TouchControls(private val viewport: Viewport, private val assets: Assets) : InputAdapter() {
    var moveX = 0f
        private set
    var moveY = 0f
        private set
    val sprint get() = sprintTouch || Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
    private var sprintTouch = false
    private var actionQueued = false

    // Posisi tombol (koordinat UI)
    private val sprintX = GameConfig.UI_W - 128f
    private val sprintY = 22f
    private val actionX = GameConfig.UI_W - 66f
    private val actionY = 74f
    private val btnSize = 48f

    private var joyPointer = -1
    private var sprintPointer = -1
    private val joyOrigin = Vector2()
    private val joyKnob = Vector2()
    private val tmp = Vector2()
    private val joyRadius = 28f

    var hideButtonVisible = false
    var exitMode = false

    private fun toUi(sx: Int, sy: Int): Vector2 = viewport.unproject(tmp.set(sx.toFloat(), sy.toFloat()))

    private fun inBtn(p: Vector2, x: Float, y: Float) = p.x >= x && p.x <= x + btnSize && p.y >= y && p.y <= y + btnSize

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val p = toUi(screenX, screenY)
        when {
            inBtn(p, sprintX, sprintY) -> { sprintPointer = pointer; sprintTouch = true }
            inBtn(p, actionX, actionY) -> { if (hideButtonVisible) actionQueued = true }
            p.x < GameConfig.UI_W * 0.5f && joyPointer == -1 -> {
                joyPointer = pointer
                joyOrigin.set(p); joyKnob.set(p)
            }
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer == joyPointer) {
            val p = toUi(screenX, screenY)
            val dx = p.x - joyOrigin.x; val dy = p.y - joyOrigin.y
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val k = if (d > joyRadius) joyRadius / d else 1f
            joyKnob.set(joyOrigin.x + dx * k, joyOrigin.y + dy * k)
            moveX = dx * k / joyRadius
            moveY = dy * k / joyRadius
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == joyPointer) { joyPointer = -1; moveX = 0f; moveY = 0f }
        if (pointer == sprintPointer) { sprintPointer = -1; sprintTouch = false }
        return true
    }

    /** Dipanggil tiap frame: menggabungkan input keyboard (uji di desktop). */
    fun pollKeyboard() {
        if (joyPointer == -1) {
            var kx = 0f; var ky = 0f
            val i = Gdx.input
            if (i.isKeyPressed(Input.Keys.A) || i.isKeyPressed(Input.Keys.LEFT)) kx -= 1f
            if (i.isKeyPressed(Input.Keys.D) || i.isKeyPressed(Input.Keys.RIGHT)) kx += 1f
            if (i.isKeyPressed(Input.Keys.W) || i.isKeyPressed(Input.Keys.UP)) ky += 1f
            if (i.isKeyPressed(Input.Keys.S) || i.isKeyPressed(Input.Keys.DOWN)) ky -= 1f
            moveX = kx; moveY = ky
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) actionQueued = true
    }

    fun consumeAction(): Boolean { val a = actionQueued; actionQueued = false; return a }

    fun render(batch: SpriteBatch) {
        if (joyPointer != -1) {
            val base = assets.region("ui_joy_base")
            batch.draw(base, joyOrigin.x - 32f, joyOrigin.y - 32f)
            val knob = assets.region(if (joyPointer != -1) "ui_joy_knob_pressed" else "ui_joy_knob")
            batch.draw(knob, joyKnob.x - 14f, joyKnob.y - 14f)
        }
        batch.draw(assets.region(if (sprint) "ui_btn_sprint_pressed" else "ui_btn_sprint"), sprintX, sprintY)
        if (hideButtonVisible) {
            val n = if (exitMode) "ui_btn_exit" else "ui_btn_hide"
            batch.draw(assets.region(n), actionX, actionY)
        }
    }
}

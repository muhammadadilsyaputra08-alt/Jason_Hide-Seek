package com.jasonhideseek.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Viewport layar penuh dengan skala BULAT (1 piksel virtual = n piksel layar) sehingga pixel-art selalu tajam
 * dan bersih. Area dunia melebar mengikuti rasio layar (tidak ada bar hitam), minimal minW x minH.
 */
class PixelViewport(private val minW: Float, private val minH: Float, camera: Camera) : ScreenViewport(camera) {
    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        val s = Math.max(1, Math.floor(Math.min(screenWidth / minW, screenHeight / minH).toDouble()).toInt())
        unitsPerPixel = 1f / s
        super.update(screenWidth, screenHeight, centerCamera)
    }
}

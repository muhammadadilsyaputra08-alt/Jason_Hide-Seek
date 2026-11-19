package com.jasonhideseek.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion

/** Memuat 3 atlas (world, characters, ui) + font. Semua filter Nearest. */
class Assets {
    val world = TextureAtlas(Gdx.files.internal("world.atlas"))
    val characters = TextureAtlas(Gdx.files.internal("characters.atlas"))
    val ui = TextureAtlas(Gdx.files.internal("ui.atlas"))
    val font = BitmapFont().apply {
        region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setUseIntegerPositions(true)
    }

    /** Tekstur 1x1 putih untuk menggambar kotak berwarna. */
    val pixel = Texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
        setColor(Color.WHITE); fill()
    })

    private val atlases = listOf(world, characters, ui)
    private val regionCache = HashMap<String, AtlasRegion>()
    private val framesCache = HashMap<String, List<AtlasRegion>>()

    init {
        atlases.forEach { a -> a.textures.forEach { it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) } }
    }

    fun region(name: String): AtlasRegion = regionCache.getOrPut(name) {
        atlases.firstNotNullOfOrNull { it.findRegion(name) }
            ?: error("Region atlas tidak ditemukan: $name")
    }

    fun frames(name: String): List<AtlasRegion> = framesCache.getOrPut(name) {
        val list = atlases.firstNotNullOfOrNull { a ->
            val arr = a.findRegions(name)
            if (arr.size > 0) arr.toList() else null
        }
        list ?: error("Frames atlas tidak ditemukan: $name")
    }

    fun frame(name: String, time: Float, fps: Float): AtlasRegion {
        val f = frames(name)
        return f[((time * fps).toInt()) % f.size]
    }

    fun dispose() {
        atlases.forEach { it.dispose() }
        font.dispose()
        pixel.dispose()
    }

    private fun com.badlogic.gdx.utils.Array<AtlasRegion>.toList(): List<AtlasRegion> {
        val out = ArrayList<AtlasRegion>(size)
        for (i in 0 until size) out.add(this[i])
        return out
    }
}

package com.jasonhideseek.world

import com.badlogic.gdx.Gdx

/** Memuat dan meng-cache maps.json dari assets/maps/maps.json. */
object MapRepository {
    private var cache: List<MapDefinition>? = null

    fun load(index: Int): MapDefinition {
        val maps = cache ?: MapDefinition.parseAll(Gdx.files.internal("maps/maps.json").readString("UTF-8")).also { cache = it }
        return maps[index]
    }
}

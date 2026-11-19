package com.jasonhideseek.systems

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jasonhideseek.game.Assets
import com.jasonhideseek.world.Material
import java.util.Random

/** Mayat + genangan darah persisten, serpihan benda, dan efek tebasan. */
class GoreAndDebrisSystem(private val assets: Assets, private val rng: Random) {

    class Decal(val region: TextureRegion, val x: Float, val y: Float)
    class Corpse(val region: TextureRegion, val x: Float, val y: Float)
    private class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float,
                           var life: Float, val maxLife: Float, val region: TextureRegion)
    private class Slash(val x: Float, val y: Float, var t: Float)
    private class AnimFx(val frames: List<TextureRegion>, val cx: Float, val cy: Float, val fps: Float, var t: Float)

    val pools = ArrayList<Decal>()
    val corpses = ArrayList<Corpse>()
    private val particles = ArrayList<Particle>()
    private val slashes = ArrayList<Slash>()
    private val anims = ArrayList<AnimFx>()

    fun bloodPool(x: Float, y: Float) {
        pools.add(Decal(assets.region("fx_blood_pool_${rng.nextInt(5)}"), x - 16f + rng.nextInt(7) - 3, y - 14f + rng.nextInt(5) - 2))
    }

    fun corpse(prefix: String, x: Float, y: Float) {
        val frames = assets.frames("${prefix}_corpse")
        corpses.add(Corpse(frames[rng.nextInt(frames.size)], x - 16f, y - 4f))
    }

    /** Kematian di ruang terbuka: mayat + genangan + percikan. */
    fun openKill(prefix: String, x: Float, y: Float) {
        bloodPool(x, y)
        corpse(prefix, x, y)
        burst(x, y + 8f, "fx_debris_blood", 9)
    }

    /** Objek kosong dihancurkan: serpihan material saja, tanpa darah. */
    fun breakClean(material: Material, x: Float, y: Float) = burst(x, y + 10f, material.debris, 12)

    /** Objek berisi dihancurkan: serpihan + percikan darah + noda lantai. */
    fun breakBloody(material: Material, x: Float, y: Float) {
        burst(x, y + 10f, material.debris, 10)
        burst(x, y + 10f, "fx_debris_blood", 12)
        bloodPool(x, y - 4f)
    }

    /** Efek animasi sekali putar (fx_key_pickup, fx_key_assembly, ...) berpusat di (cx, cy). */
    fun animFx(region: String, cx: Float, cy: Float, fps: Float) {
        anims.add(AnimFx(assets.frames(region), cx, cy, fps, 0f))
    }

    fun slash(x: Float, y: Float) { slashes.add(Slash(x - 24f, y - 24f, 0f)) }

    private fun burst(x: Float, y: Float, region: String, n: Int) {
        val frames = assets.frames(region)
        repeat(n) {
            val a = rng.nextFloat() * 6.2831f
            val sp = 25f + rng.nextFloat() * 60f
            val life = 0.5f + rng.nextFloat() * 0.5f
            particles.add(Particle(x, y, Math.cos(a.toDouble()).toFloat() * sp,
                Math.sin(a.toDouble()).toFloat() * sp + 30f, life, life, frames[rng.nextInt(frames.size)]))
        }
    }

    fun update(dt: Float) {
        var i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            p.life -= dt
            if (p.life <= 0f) { particles.removeAt(i) } else {
                p.vy -= 160f * dt
                p.x += p.vx * dt; p.y += p.vy * dt
            }
            i--
        }
        var k = anims.size - 1
        while (k >= 0) {
            anims[k].t += dt
            if (anims[k].t * anims[k].fps >= anims[k].frames.size) anims.removeAt(k)
            k--
        }
        var j = slashes.size - 1
        while (j >= 0) {
            slashes[j].t += dt
            if (slashes[j].t > 0.24f) slashes.removeAt(j)
            j--
        }
    }

    fun renderPools(batch: SpriteBatch) {
        for (d in pools) batch.draw(d.region, d.x, d.y)
    }

    fun renderEffects(batch: SpriteBatch) {
        for (p in particles) {
            batch.setColor(1f, 1f, 1f, Math.min(1f, p.life / p.maxLife * 1.5f))
            batch.draw(p.region, p.x, p.y)
        }
        batch.setColor(1f, 1f, 1f, 1f)
        for (a in anims) {
            val f = a.frames[Math.min(a.frames.size - 1, (a.t * a.fps).toInt())]
            batch.draw(f, a.cx - f.regionWidth / 2f, a.cy - f.regionHeight / 2f)
        }
        for (s in slashes) {
            val f = assets.frames("fx_slash")
            batch.draw(f[Math.min(f.size - 1, (s.t / 0.08f).toInt())], s.x, s.y)
        }
    }
}

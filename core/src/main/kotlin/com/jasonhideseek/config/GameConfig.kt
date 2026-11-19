package com.jasonhideseek.config

/** Seluruh angka penyeimbang (tuning) game terkumpul di sini. */
object GameConfig {
    // Viewport (virtual resolution tetap, pixel-perfect)
    const val WORLD_W = 384f
    const val WORLD_H = 216f
    const val UI_W = 480f
    const val UI_H = 270f
    const val TILE = 32

    // Durasi ronde (detik)
    const val COUNTDOWN_TIME = 20f
    const val ESCAPE_TIME = 240f         // Escape Timer (setelah countdown)
    const val KEY_FRAME_COUNT = 3

    // Survivor
    const val PLAYER_SPEED = 62f
    const val PLAYER_SPRINT = 100f
    const val BOT_SPEED = 56f
    const val BOT_SPRINT = 92f
    const val STAMINA_MAX = 100f
    const val STAMINA_DRAIN = 34f
    const val STAMINA_REGEN = 16f
    const val STAMINA_RECOVER_AT = 25f
    const val BOT_COUNT = 9
    const val HIDE_RANGE = 26f
    const val KEY_PICKUP_RANGE = 14f
    const val EXIT_RANGE = 24f
    const val KEY_PICKUP_TIME = 0.42f
    const val EXIT_INTERACT_TIME = 0.7f
    const val EXIT_UNLOCK_TIME = 0.6f
    const val ESCAPE_ANIM_TIME = 0.8f
    const val MINICAM_W = 90
    const val MINICAM_H = 54

    // Jason
    const val JASON_PATROL_SPEED = 46f
    const val JASON_SEARCH_SPEED = 58f
    const val JASON_CHASE_SPEED = 80f
    const val JASON_DETECT_RADIUS = 56f      // R: radius deteksi HidingSpot
    const val JASON_VIEW_RANGE = 150f
    const val JASON_FOV_DEG = 130f
    const val JASON_PROXIMITY = 26f          // melihat tanpa memandang sudut
    const val JASON_ATTACK_RANGE = 22f
    const val JASON_EVAL_MIN = 0.4f
    const val JASON_EVAL_MAX = 0.8f
    const val SPOT_RECHECK_COOLDOWN = 22f
    const val SPOT_CHECK_DURATION = 1.2f
    const val JASON_EXIT_GUARD_CHANCE = 0.35f
    const val LOSE_SIGHT_TIME = 0.5f

    // Keputusan acak (bobot dasar)
    const val W_ATTACK = 0.20f
    const val W_CHECK = 0.30f
    const val W_SKIP = 0.50f
    const val SUSPICION_DECAY = 0.02f

    // Noise
    const val NOISE_RADIUS_BREAK = 170f
    const val NOISE_RADIUS_KILL = 190f
}

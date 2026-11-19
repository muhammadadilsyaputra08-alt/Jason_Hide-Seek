package com.jasonhideseek.config

enum class Dir(val key: String, val dx: Float, val dy: Float) {
    DOWN("down", 0f, -1f), LEFT("left", -1f, 0f), RIGHT("right", 1f, 0f), UP("up", 0f, 1f);

    companion object {
        fun from(dx: Float, dy: Float, fallback: Dir): Dir {
            if (dx == 0f && dy == 0f) return fallback
            return if (Math.abs(dx) > Math.abs(dy)) (if (dx > 0) RIGHT else LEFT)
            else (if (dy > 0) UP else DOWN)
        }
    }
}

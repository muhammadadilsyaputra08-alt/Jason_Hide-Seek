package com.jasonhideseek.systems

/** Efek in-game khusus tiap map (urutan = indeks map). */
enum class MapEffect(val title: String, val desc: String) {
    CREAKY_FLOORS("Lantai Berderit", "Berlari membuat suara yang terdengar Jason"),
    CAR_ALARM("Alarm Mobil", "Alarm berbunyi acak dan memancing Jason"),
    FOG("Kabut Ladang", "Pandangan Jason & Key Frame jadi pendek"),
    BLACKOUT("Listrik Padam", "Lampu padam berkala, semua jadi gelap"),
    ECHO("Gema Bawah Tanah", "Semua suara terdengar lebih jauh");

    companion object { fun forMap(i: Int) = values()[i.coerceIn(0, values().size - 1)] }
}

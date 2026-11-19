# Jason Hide & Seek

Game survival hide-and-seek horor 2D pixel-art untuk Android (offline, single-player + bot penyintas).
LibGDX + Kotlin. 5 map: Aula Rumah Tua, Garasi, Ladang & Lumbung, Gudang, Ruang Bawah Tanah.

## Struktur
- `core/` — logika game (config, screens, world, entities, ai, systems, ui) + unit test
- `android/` — launcher Android
- `assets/` — atlas pixel-art (Nearest), `hiding_spots.json`, audio sintetis (`tools/` tidak dibundel)
- `tools/gen_maps.py` — generator layout map (menulis `world/MapData.kt`)

## Kontrol
Sentuh: joystick melayang (kiri layar), tombol SPRINT & HIDE/EXIT (kanan).
Keyboard (uji): WASD/panah, Shift = sprint, E/Spasi = hide/exit.

## Build lokal
Butuh JDK 17 + Android SDK (`ANDROID_HOME`) + Gradle 8.14+:
```
gradle :core:test
gradle :android:assembleDebug      # APK di android/build/outputs/apk/debug/
```

## GitHub Actions (`.github/workflows/android-build.yml`)
Push/PR/manual menjalankan unit test lalu membangun debug APK, release APK, dan release AAB
(diunggah sebagai artifact). Tag `v*` otomatis membuat GitHub Release berisi file build.

Untuk rilis bertanda tangan, set Secrets repo: `KEYSTORE_BASE64` (`base64 -w0 release.jks`),
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Tanpa secrets, build rilis memakai debug key.

# Jason Hide & Seek

Game survival hide-and-seek horor 2D pixel-art untuk Android (offline): 1 pemain + 9 bot melawan Jason.
LibGDX + Kotlin. 5 map (dimuat dari `assets/maps/maps.json`).

## Gameplay (blueprint update)
- Kumpulkan 3 Key Frame (progres bersama) -> otomatis jadi Complete Key -> buka pintu keluar -> escape.
- Escape Timer 240 dtk (mulai setelah countdown 20 dtk). Jason Mini Camera (kanan atas) memantau Jason.
- Pemain tereliminasi -> mode penonton (kamera ikut bot, tombol prev/next), mini camera tetap aktif.
- Hasil: Player Escaped, Survivors Escaped, Jason Wins, Time's Up (+ rekap di Result Screen).
- Bot: Explore, Seek Hiding Spot, Hide, Flee, Search/Collect Key Frame, Seek Exit, Escape.

## Struktur
- `core/` logika game: `config`, `game` (Assets 4 atlas), `world` (MapDefinition dari maps.json, TileMapRenderer, HidingSpot),
  `entities`, `ai` (JasonDecisionEngine), `systems` (GameWorld, VisionSystem, GoreAndDebrisSystem, SoundCueSystem, MatchRules), `ui`, `screens`
- `android/` launcher + ikon adaptif
- `assets/` `atlases/{tiles,items,characters,ui}`, `maps/maps.json`, `hiding_spots.json`, `branding/`, `audio/`
- `tools/asset_pack_src/` generator asli paket aset v2.1 (referensi)

## Kontrol
Sentuh: joystick melayang (kiri), SPRINT dan tombol aksi kontekstual (kanan: Hide / Exit / Interact pintu).
Keyboard (uji): WASD/panah, Shift = sprint, E/Spasi = aksi. Spectator: panah kiri/kanan.

## Build
```
gradle :core:test
gradle :android:assembleDebug
```
CI: `.github/workflows/android-build.yml` (test -> debug APK, release APK, release AAB; tag `v*` membuat Release).
Rilis bertanda tangan: Secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

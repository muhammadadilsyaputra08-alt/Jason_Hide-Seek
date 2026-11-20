# Jason Hide & Seek

Game survival hide-and-seek horor 2D pixel-art untuk Android (offline): 1 pemain + 9 bot melawan Jason.
LibGDX + Kotlin. 5 map (dimuat dari `assets/maps/maps.json`).

## Gameplay (blueprint update)
- Key Frame sulit didapat: tersebar jauh dari spawn & satu sama lain, hanya terlihat dari dekat, butuh ~2,6 dtk (berisik) untuk diambil.
- Key Frame dibawa per survivor. HANYA survivor yang memegang ketiga frame yang punya kunci utuh dan bisa membuka pintu -> satu pemenang.
  Bila pembawa tereliminasi, frame jatuh di posisinya dan bisa diambil survivor lain. Ronde selesai begitu ada yang kabur.
- Semua survivor (dan Jason) spawn acak di lantai yang terhubung; Jason selalu jauh dari survivor.
- Setiap survivor punya nama; notifikasi eliminasi / kunci / kabur tampil di feed dan banner tengah.
- UI menyesuaikan layar penuh (HUD menempel ke tepi, tidak saling menumpuk).
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

## Buff Jason
Didapat dari eliminasi survivor (pasti) atau menghancurkan barang kosong (12%). Durasi 15-20 dtk, tampil di HUD kanan.
- **I Can Hear You**: jarak/sudut pandang, deteksi spot, dan respons ke suara naik
- **Bloodlust**: kecepatan kejar +18% | **Heavy Axe**: jangkauan serang + | **Stalker**: patroli/pencarian lebih cepat

## Efek map
1 Lantai Berderit (lari = bising) | 2 Alarm Mobil (alarm acak memancing Jason) | 3 Kabut Ladang (pandangan pendek)
4 Listrik Padam (gelap berkala) | 5 Gema Bawah Tanah (suara 1,6x lebih jauh)

Visual: viewport layar penuh berskala bulat (pixel tajam), tanpa tint/gelap tebal, lantai polos dengan aksen ~10% seperti contoh peta.

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
- `tools/build_maps.py` (layout redesain + penempatan aset baru -> maps.json), `tools/validate_maps.py` (grid, collision, keterjangkauan),
  `tools/render_preview.py` (pratinjau statis map)

## Schema maps.json (v2, update lima map)
Ukuran grid tiap map berbeda (31x23, 29x21, 31x23, 31x21, 29x23). Tambahan: `tile_overrides` (ganti sprite tile, visual saja),
`objects.bot_spawn_candidates` (prioritas spawn bot, fallback ke lantai terjangkau), layer decor `floor | fx | wall | object | ceiling`.
Objek > 1 sel memakai `w` + `extra_collision` (sel atas untuk sprite tinggi). Tile pintu keluar selalu dinding.
9 hiding spot baru: bear_rug, tire_stack, mechanic_cabinet, silo_gap, hay_wagon, shrink_wrap_pallet, shipping_container, pipe_recess, utility_cabinet.

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

## Mode pemain = Jason
Menu: ketuk panel MODE (atau tombol M) untuk memilih SURVIVOR / JASON. Sebagai Jason: joystick = gerak, Sprint = lari (stamina), tombol aksi = serang
(survivor terlihat dalam jangkauan, atau spot persembunyian terdekat). Menang bila semua survivor habis atau waktu habis tanpa ada yang kabur;
kalah bila satu survivor kabur. Penanda emas muncul di atas pembawa kunci utuh; buff "I Can Hear You" menandai spot yang terisi di dekatmu.

## Pengejaran pembawa frame (AI Jason)
- Tiap frame yang dibawa: jarak pandang Jason +18%, peluang patroli menuju pembawa +12%, kecurigaan spot yang dihuni +0,15.
- Jason memprioritaskan pembawa frame sebagai target kejaran.
- Begitu kunci utuh terbentuk, Jason terus memburu pembawanya (termasuk menghancurkan tempat persembunyiannya).

## Notifikasi
Banner tengah memakai antrean (satu per satu, tanpa duplikat); feed kiri maksimal 4 baris ringkas dan dipotong agar tak menimpa HUD.

## Pembaruan: risiko, ketahanan, kerja sama
- Ambil frame (1,8 dtk) bisa dibatalkan (gerak/tombol aksi), progres tersimpan; bot membatalkan bila Jason mendekat & tak mulai saat Jason mengejar di dekat frame.
- Tempat sembunyi punya ketahanan (3 hantaman) dengan bar; penghuni bot panik & kabur saat spot retak, pemain bisa keluar sendiri.
- Saat hitung mundur, Jason dan Key Frame tidak terlihat (mode Survivor).
- Pemegang kunci utuh langsung ke pintu; bila Jason dekat: lari/sembunyi (lari ke pintu bila pintu sudah dekat).
- Survivor yang lolos tidak mengakhiri ronde: pintu tetap terbuka, yang lain bisa menyusul tanpa kunci. Tiap yang lolos membuat Jason lebih agresif (+14% jangkauan/deteksi, kecepatan naik).
- Bot yang dikejar lebih memilih kabur daripada sembunyi saat Jason sangat dekat, kecuali sudah dekat spot yang aman.
- Frame terbagi ke beberapa survivor: bila semua frame sudah dipegang, mereka berkumpul/menempel (<=34 px) untuk merakit kunci, lalu ke pintu.
- Mode Jason: menang bila kurang dari separuh survivor yang kabur.

## Update 2: Jason Arrival Chamber, mini-map, lanjut setelah kabur
- `maps.json` schema v3: tiap map punya `jason_arrival` (bounds 5x5, spawn_position, door{material,type}, sequence{prep 12s / warning 5s / break 3s}). Jason selalu spawn di ruang ini (bukan acak); pintu dinamis: LOCKED -> WARNING (getar + lampu berkedip) -> BREAKING (retak) -> BROKEN (debris permanen). Material: wood/shutter/barn/steel/iron.
- Mini camera Jason + frame-nya disembunyikan sampai Jason keluar dari state COUNTDOWN_LOCKED (mulai bergerak).
- Pemain kabur TIDAK mengakhiri ronde (`MatchRules`): ronde selesai bila semua survivor selesai atau waktu habis; pemain yang sudah kabur menonton bot; HUD menampilkan Lolos/Hidup/Gugur.
- Sumber layout: `tools/layouts/map*.txt` (ASCII) -> `python3 -I tools/layouts.py build tools/layouts assets/maps/maps.json`; validasi: `tools/validate_maps.py`; pratinjau: `tools/render_preview.py <atlas_dir> <maps.json> <out_dir>`; aset ruang: `tools/gen_arrival_assets.py`.
- Belum diverifikasi di perangkat: build Gradle/Android tidak bisa dijalankan di sandbox; GameScreen/GameWorld/TileMapRenderer/JasonEntity belum dikompilasi (hanya logika inti diuji dengan stub libGDX, 22 tes OK).

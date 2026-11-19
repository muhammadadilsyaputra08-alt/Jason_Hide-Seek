# Jason Hide & Seek - Asset Pack v2.1

Struktur: assets/atlases/{tiles,items,characters,ui}.{png,atlas,json}, assets/maps/maps.json, assets/hiding_spots.json, assets/branding/, android_res/, sprites_new/ (aset baru), maps_preview/, src/ (generator).

Konvensi: tile 32x32, survivor 32x32 (pivot kaki 16,28), Jason 64x64 (pivot 32,51; sebelumnya 48x48), filter Nearest. Region animasi = nama + index; sprite tunggal index -1.

Kotlin:
  val items = TextureAtlas(Gdx.files.internal("atlases/items.atlas"))
  val chars = TextureAtlas(Gdx.files.internal("atlases/characters.atlas"))
  val walk  = Animation(0.12f, chars.findRegions("jason_walk_right"), Animation.PlayMode.LOOP)
  val spot  = items.findRegion("obj_wardrobe_intact")
Alias nama blueprint -> region: manifest.json.

maps.json v2: tile lingkungan (0 floor,1 wall,2 door_h,3 door_v) terpisah dari objek (spawn, exit+orientation, hiding_spots, props, decor, key_candidates). Prop dengan "extra_collision" menambah sel collision di luar grid asli (vehicle map 2). Fence map 3 mengganti prop solid yang sudah ada (collision tidak berubah).
Update 2.1: frame Jason diperluas ke 64x64 agar kapak tidak terpotong; semua region jason_* berubah ukuran.
Audio belum dibuat; folder assets/audio kosong.

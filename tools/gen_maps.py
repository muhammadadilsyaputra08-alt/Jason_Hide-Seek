#!/usr/bin/env python3
"""Generator layout 5 map (grid ASCII). Output: core/.../world/MapData.kt
Legend: # dinding | . lantai | D pintu | p prop solid | 1-4 titik sembunyi (indeks katalog map)
        P spawn pemain | J titik masuk Jason"""
import random, sys, collections, pathlib

def build(seed, cols, rows, rw, rh, gap, prop_rate, spots_per_room, open_walls=False):
    rnd = random.Random(seed)
    W = cols * rw + cols + 1
    H = rows * rh + rows + 1
    g = [['#'] * W for _ in range(H)]
    rooms = []
    for r in range(rows):
        for c in range(cols):
            x0 = 1 + c * (rw + 1); y0 = 1 + r * (rh + 1)
            for y in range(y0, y0 + rh):
                for x in range(x0, x0 + rw):
                    g[y][x] = '.'
            rooms.append((c, r, x0, y0))
    doors = set()
    # horizontal neighbours (wall column between rooms)
    for r in range(rows):
        for c in range(cols - 1):
            wx = 1 + (c + 1) * (rw + 1) - 1
            y0 = 1 + r * (rh + 1)
            start = rnd.randint(y0, y0 + rh - gap)
            for y in range(start, start + gap):
                g[y][wx] = 'D'; doors.add((wx, y))
    for r in range(rows - 1):
        for c in range(cols):
            wy = 1 + (r + 1) * (rh + 1) - 1
            x0 = 1 + c * (rw + 1)
            start = rnd.randint(x0, x0 + rw - gap)
            for x in range(start, start + gap):
                g[wy][x] = 'D'; doors.add((x, wy))
    if open_walls:  # pagar rendah: buka bagian dinding lain
        for (x, y) in [(x, y) for y in range(H) for x in range(W)]:
            pass
    near_door = set()
    for (dx, dy) in doors:
        for ax in range(-1, 2):
            for ay in range(-1, 2):
                near_door.add((dx + ax, dy + ay))
    # spawn & jason
    jr = rooms[cols - 1]            # kanan atas
    pr = rooms[(rows - 1) * cols]   # kiri bawah
    g[jr[3] + 1][jr[2] + rw - 2] = 'J'
    g[pr[3] + rh - 2][pr[2] + 1] = 'P'
    # props
    for (c, r, x0, y0) in rooms:
        for y in range(y0, y0 + rh):
            for x in range(x0, x0 + rw):
                if g[y][x] == '.' and (x, y) not in near_door and rnd.random() < prop_rate:
                    g[y][x] = 'p'
    # spots (jauh dari pintu & spawn), bersandar dinding bila bisa
    placed = 0
    for idx, (c, r, x0, y0) in enumerate(rooms):
        n = spots_per_room[idx % len(spots_per_room)]
        cand = [(x, y) for y in range(y0, y0 + rh) for x in range(x0, x0 + rw)
                if g[y][x] == '.' and (x, y) not in near_door
                and (x in (x0, x0 + rw - 1) or y in (y0, y0 + rh - 1))]
        rnd.shuffle(cand)
        for _ in range(n):
            if not cand: break
            x, y = cand.pop()
            g[y][x] = str(placed % 4 + 1); placed += 1
            cand = [p for p in cand if abs(p[0] - x) + abs(p[1] - y) > 3]
    return [''.join(r) for r in g]

def connected(rows):
    H, W = len(rows), len(rows[0])
    free = {(x, y) for y in range(H) for x in range(W) if rows[y][x] not in '#p'}
    start = next((x, y) for (x, y) in free if rows[y][x] == 'P')
    seen = {start}; q = collections.deque([start])
    while q:
        x, y = q.popleft()
        for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + d[0], y + d[1])
            if n in free and n not in seen: seen.add(n); q.append(n)
    return seen == free, sum(ch.isdigit() for r in rows for ch in r)

CONFIGS = [  # (seed, cols, rows, rw, rh, doorgap, prop_rate, spots/room)
    (11, 3, 2, 10, 8, 2, 0.06, [2, 1, 2, 1, 2, 2]),   # Manor
    (23, 3, 2, 10, 8, 2, 0.10, [2, 2, 1, 2, 2, 1]),   # Garasi
    (35, 3, 2, 11, 8, 5, 0.07, [2, 1, 2, 2, 1, 2]),   # Ladang (celah pagar lebar)
    (47, 3, 2, 10, 8, 2, 0.16, [2, 2, 2, 1, 2, 1]),   # Gudang (rak banyak)
    (59, 3, 2, 8, 7, 1, 0.12, [2, 1, 2, 2, 1, 2]),    # Basement (sempit)
]
out = ["// GENERATED oleh tools/gen_maps.py — jangan edit manual.",
       "package com.jasonhideseek.world", "", "object MapData {",
       "    val LAYOUTS: Array<Array<String>> = arrayOf("]
for i, cfg in enumerate(CONFIGS):
    for attempt in range(200):
        rows = build(cfg[0] + attempt * 101, *cfg[1:])
        ok, spots = connected(rows)
        if ok and spots >= 8: break
    else:
        sys.exit(f"map {i+1} gagal")
    print(f"map{i+1}: {len(rows[0])}x{len(rows)} spots={spots} seed={cfg[0]+attempt*101}")
    out.append("        arrayOf(")
    out += [f'            "{r}",' for r in rows]
    out.append("        ),")
out += ["    )", "}", ""]
p = pathlib.Path(__file__).resolve().parent.parent / "core/src/main/kotlin/com/jasonhideseek/world/MapData.kt"
p.write_text("\n".join(out)); print("ok", p)

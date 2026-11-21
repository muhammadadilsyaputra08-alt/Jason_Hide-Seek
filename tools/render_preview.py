#!/usr/bin/env python3
"""Pratinjau statis map (meniru urutan render GameScreen) untuk pemeriksaan visual cepat. Bukan pengganti uji di perangkat."""
import json, sys
from PIL import Image
A = sys.argv[1]; maps = json.load(open(sys.argv[2]))['maps']; out = sys.argv[3]
def load(a):
    im = Image.open(f'{A}/{a}.png').convert('RGBA'); R = {}; L = open(f'{A}/{a}.atlas').read().split('\n'); i = 5
    while i < len(L):
        n = L[i]
        if n and not n.startswith(' '):
            xy = [int(v) for v in L[i + 2].split(':')[1].split(',')]; sz = [int(v) for v in L[i + 3].split(':')[1].split(',')]
            idx = int(L[i + 6].split(':')[1]); R.setdefault(n, []).append((idx, im.crop((xy[0], xy[1], xy[0] + sz[0], xy[1] + sz[1]))))
            i += 7
        else: i += 1
    return {k: [x[1] for x in sorted(v, key=lambda t: t[0])] for k, v in R.items()}
T, I = load('tiles'), load('items'); R = {**T, **I}
def reg(n): return R[n][0]
for m in maps:
    W, H, TILE = m['width'], m['height'], 32; o = m['objects']; mi = m['id']
    img = Image.new('RGBA', (W * TILE, H * TILE), (0, 0, 0, 255)); ov = {(t['x'], t['y']): t['region'] for t in m.get('tile_overrides', [])}
    def put(r, x, y):
        img.alpha_composite(r, (int(x), int(y)))
    for r in range(H):
        for c in range(W):
            t = m['tiles'][r][c]; x, y = c * TILE, r * TILE
            if (c, r) in ov: put(reg(ov[(c, r)]), x, y); continue
            if t == 1: put(reg(f'tile_map{mi}_wall_top' if r + 1 < H and m['tiles'][r + 1][c] == 1 else f'tile_map{mi}_wall_face'), x, y); continue
            put(reg(f'tile_map{mi}_floor_a'), x, y)
            if t == 2: put(reg(f'tile_map{mi}_door'), x, y)
            if t == 3: put(reg(f'tile_map{mi}_door_v'), x, y)
            if r > 0 and m['tiles'][r - 1][c] == 1 and t == 0: put(reg('tile_shadow_wall'), x, y + TILE - 8 - 0)
    dec = o['decor']
    def drawdec(layer):
        for d in dec:
            if d.get('layer', 'object') != layer: continue
            g = reg(d['item']); put(g, d['x'] * TILE + (TILE - g.width) // 2 if g.width < TILE else d['x'] * TILE, d['y'] * TILE + TILE - g.height)
    for l in ('floor', 'fx'): drawdec(l)
    e = o['exit']; put(reg('exit_locked_' + ('e' if e.get('orientation') in ('e', 'w') else 's')), e['x'] * TILE, e['y'] * TILE)
    drawdec('wall'); drawdec('arrival_lamp')
    ja = m.get('jason_arrival')
    if ja and ja.get('enabled'):
        dd = ja['door']; g = reg(f"door_arrival_{dd['material']}_closed"); put(g, dd['x'] * TILE + (TILE - g.width) // 2, dd['y'] * TILE + TILE - g.height)
    objs = []
    for p in o['props']: objs.append(((p['y'] + 1) * TILE - 4 , 'p', p))
    for d in dec:
        if d.get('layer', 'object') == 'object': objs.append(((d['y'] + 1) * TILE - 4, 'p', d))
    for h in o['hiding_spots']: objs.append(((h['y'] + .5) * TILE + 10, 'h', h))
    for _, k, p in sorted(objs, key=lambda t: t[0]):
        if k == 'h': g = reg(f"obj_{p['type_id']}_intact"); put(g, p['x'] * TILE, p['y'] * TILE)
        else: g = reg(p['item']); put(g, p['x'] * TILE, p['y'] * TILE + TILE - g.height)
    drawdec('ceiling')
    img.convert('RGB').save(f'{out}/map{mi}.png')
print('ok')

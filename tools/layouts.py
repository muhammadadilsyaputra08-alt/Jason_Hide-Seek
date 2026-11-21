#!/usr/bin/env python3
"""Peta ASCII visual <-> maps.json.
  python3 tools/layouts.py build tools/layouts assets/maps/maps.json   (layout .txt -> JSON runtime)
  python3 tools/layouts.py dump  assets/maps/maps.json tools/layouts   (JSON -> .txt, dipakai sekali untuk memulai)
Format .txt: header (id,name,room), 'legend:' (karakter -> arti), 'grid:' (1 karakter = 1 tile 32x32, kiri-atas = (0,0)),
lalu daftar 'decor:' / 'overrides:' / 'exit:' / 'arrival:'. Karakter grid:
  # dinding  . lantai  = pintu horizontal  | pintu vertikal  X pintu ruang Jason (dinding, dinamis)  E pintu keluar
  K kandidat Key Frame  b kandidat spawn bot  P spawn pemain (metadata)  J spawn Jason (di dalam chamber)
  ~ sel lanjutan objek besar (diabaikan, hanya penanda visual)  huruf lain = lihat legend (prop / hiding spot)."""
import json, sys, os, re, collections

SPOT_CODE = {}
RESERVED = set('#.=|XEKbPJ~ ')
SIZE_UP = {}   # item -> (w, h) sel; dihitung dari legend

def parse(path):
    L = open(path, encoding='utf-8').read().split('\n')
    meta, legend, grid, decor, ovr, extra = {}, {}, [], [], [], {}
    sec = None
    for ln in L:
        if not ln.strip() or ln.lstrip().startswith('#!'): continue
        if re.match(r'^(legend|grid|decor|overrides|exit|arrival|hidden):\s*$', ln): sec = ln.strip()[:-1]; continue
        if sec is None:
            k, v = ln.split(':', 1); meta[k.strip()] = v.strip()
        elif sec == 'legend':
            p = ln.split(); legend[p[0]] = p[1:]
        elif sec == 'grid': grid.append(ln.rstrip('\n'))
        elif sec == 'decor': decor.append(ln.split())
        elif sec == 'overrides': ovr.append(ln.split())
        elif sec in ('exit', 'arrival'): extra[sec] = dict(t.split('=') for t in ln.split())
    return meta, legend, grid, decor, ovr, extra

def build(src, dst):
    maps = []
    for fn in sorted(f for f in os.listdir(src) if re.match(r'map\d+\.txt$', f)):
        meta, legend, grid, decor, ovr, extra = parse(os.path.join(src, fn))
        mid = int(meta['id']); H = len(grid); W = len(grid[0])
        assert all(len(r) == W for r in grid), f'{fn}: lebar baris tidak seragam'
        tiles = [[0] * W for _ in range(H)]
        o = {'spawns': {}, 'hiding_spots': [], 'props': [], 'decor': [], 'key_candidates': [], 'bot_spawn_candidates': []}
        doorcell = None
        for y, row in enumerate(grid):
            for x, ch in enumerate(row):
                if ch in '#EX': tiles[y][x] = 1
                elif ch == '=': tiles[y][x] = 2
                elif ch == '|': tiles[y][x] = 3
                if ch == 'X': doorcell = (x, y)
                if ch == 'E': o['exit'] = {'x': x, 'y': y}
                elif ch == 'K': o['key_candidates'].append({'x': x, 'y': y, 'allow_inside_hiding_spot': False})
                elif ch == 'b': o['bot_spawn_candidates'].append({'x': x, 'y': y})
                elif ch == 'P': o['spawns']['player'] = {'x': x, 'y': y}
                elif ch == 'J': o['spawns']['jason'] = {'x': x, 'y': y}
                elif ch in legend:
                    t = legend[ch]; kv = dict(a.split('=') for a in t[2:] if '=' in a)
                    if t[0] == 'spot':
                        o['hiding_spots'].append({'x': x, 'y': y, 'type_code': SPOT_CODE.get(t[1], 0), 'type_id': t[1],
                            'sprite_prefix': 'obj_' + t[1], 'collision': False, 'states': ['intact', 'checked', 'broken_clean', 'broken_bloody']})
                    else:
                        w, h, up = int(kv.get('w', 1)), int(kv.get('h', 1)), int(kv.get('up', 0))
                        col = kv.get('col', '1') == '1'
                        p = {'x': x, 'y': y, 'item': t[1], 'w': w, 'h': h, 'collision': col}
                        if col and up: p['extra_collision'] = [{'x': x + dx, 'y': y - dy} for dy in range(1, up + 1) for dx in range(w)]
                        o['props'].append(p)
                elif ch not in '#.=|XEKbPJ~': raise SystemExit(f'{fn}: karakter {ch!r} di ({x},{y}) tidak ada di legend')
        for d in decor:
            layer, item, x, y = d[0], d[1], int(d[2]), int(d[3])
            o['decor'].append({'x': x, 'y': y, 'item': item, 'layer': layer, 'collision': False, **({'w': int(d[4])} if len(d) > 4 else {})})
        ex = extra['exit']; o['exit']['orientation'] = ex['dir']
        m = {'id': mid, 'name': meta['name'], 'width': W, 'height': H, 'tiles': tiles, 'objects': o,
             'tile_overrides': [{'x': int(t[1]), 'y': int(t[2]), 'region': t[0]} for t in ovr]}
        a = extra['arrival']
        m['jason_arrival'] = {'enabled': True, 'room_id': a['room'], 'room_name': meta.get('room', ''),
            'bounds': dict(zip('xywh', map(int, a['bounds'].split(',')))),
            'spawn_position': dict(o['spawns']['jason']),
            'door': {'x': doorcell[0], 'y': doorcell[1], 'material': a['material'], 'type': a['type'], 'state': 'locked',
                     'animation': 'jason_break_door', 'final_state': 'broken'},
            'sequence': {'preparation_delay': float(a['prep']), 'warning_duration': float(a['warn']),
                         'door_break_duration': float(a['brk']), 'allow_jason_exit_after_break': True}}
        maps.append(m)
    json.dump({'schema_version': 3, 'tile_size': 32,
               'coordinate_system': {'origin': 'top_left', 'x': 'column, increases right', 'y': 'row, increases down'},
               'atlases': {'tiles': 'atlases/tiles.atlas', 'items': 'atlases/items.atlas', 'characters': 'atlases/characters.atlas', 'ui': 'atlases/ui.atlas'},
               'tile_legend': {'0': 'floor', '1': 'wall', '2': 'door_h', '3': 'door_v'},
               'object_notes': 'Dihasilkan dari tools/layouts/*.txt oleh tools/layouts.py. schema v3 = v2 + tile_overrides, bot_spawn_candidates, jason_arrival.',
               'maps': maps}, open(dst, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print('built', len(maps), 'maps ->', dst)

if __name__ == '__main__' and sys.argv[1] == 'build': build(sys.argv[2], sys.argv[3])

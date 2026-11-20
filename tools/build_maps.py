#!/usr/bin/env python3
"""Membangun assets/maps/maps.json: layout redesain (JSON referensi) + penempatan aset baru.
Pemakaian: python3 tools/build_maps.py <maps_redesign_reference.json> <out maps.json>"""
import json, sys

REF, OUT = sys.argv[1], sys.argv[2]
d = json.load(open(REF))
M = {m['id']: m for m in d['maps']}

# ukuran sprite (w,h dalam sel) untuk item > 1 sel
SIZE = {'item_fireplace': (2, 1), 'item_sofa': (2, 1), 'item_dining_table': (2, 1), 'item_bed_large': (2, 2),
        'item_staircase': (2, 2), 'item_garage_shutter': (2, 1), 'item_hydraulic_lift': (2, 1),
        'item_windmill': (2, 2), 'item_forklift': (2, 1), 'item_boiler': (1, 2)}

def prop(m, item, x, y):
    w, h = SIZE.get(item, (1, 1))
    p = {'x': x, 'y': y, 'item': item, 'w': w, 'h': 1, 'collision': True}
    extra = [{'x': x + dx, 'y': y - dy} for dy in range(1, h) for dx in range(w)]
    if extra: p['extra_collision'] = extra
    m['objects']['props'].append(p)

def decor(m, item, x, y, layer):
    dd = {'x': x, 'y': y, 'item': item, 'layer': layer, 'collision': False}
    m['objects']['decor'].append(dd)

def spot(m, tid, x, y):
    code = {'bear_rug': 8, 'tire_stack': 8, 'mechanic_cabinet': 9, 'silo_gap': 10, 'hay_wagon': 11,
            'shrink_wrap_pallet': 12, 'shipping_container': 13, 'pipe_recess': 14, 'utility_cabinet': 15}[tid]
    m['objects']['hiding_spots'].append({'x': x, 'y': y, 'type_code': code, 'type_id': tid,
        'sprite_prefix': 'obj_' + tid, 'collision': False,
        'states': ['intact', 'checked', 'broken_clean', 'broken_bloody']})

def tov(m, region, cells):
    m.setdefault('tile_overrides', [])
    for x, y in cells: m['tile_overrides'].append({'x': x, 'y': y, 'region': region})

def rm_prop(m, item, x, y):
    m['objects']['props'] = [p for p in m['objects']['props'] if not (p['item'] == item and p['x'] == x and p['y'] == y)]

# ---------------- perbaikan cacat layout bawaan JSON redesain ----------------
M[3]['tiles'][7][7] = 3     # ruang lumbung (8-10,7-8) tadinya tertutup total
M[3]['tiles'][9][23] = 3    # ruang sumur (24-27,9-10) tadinya tertutup total
for k in M[2]['objects']['key_candidates']:
    if (k['x'], k['y']) == (25, 17): k['x'], k['y'] = 26, 17   # tadinya tepat di atas hiding spot

# Pintu keluar harus berupa tile dinding (solid); JSON redesain menaruhnya di tile pintu (2/3) pada map 3,4,5.
for mm in M.values():
    e = mm['objects']['exit']; T = mm['tiles']
    if T[e['y']][e['x']] != 1:
        T[e['y']][e['x']] = 1
        horiz = e.get('orientation', 's') in ('s', 'n')
        for sgn in (1, -1):
            x, y = e['x'], e['y']
            while True:
                x, y = (x + sgn, y) if horiz else (x, y + sgn)
                if 0 <= y < mm['height'] and 0 <= x < mm['width'] and T[y][x] in (2, 3): T[y][x] = 1
                else: break

# ---------------- MAP 1: Aula Rumah Tua ----------------
m = M[1]
rm_prop(m, 'item_table', 23, 2)                      # meja biasa -> meja makan formal
prop(m, 'item_dining_table', 23, 2)
for c in [(23, 3), (24, 3), (22, 2), (25, 2)]: prop(m, 'item_dining_chair', *c)
prop(m, 'item_armchair', 2, 5); prop(m, 'item_armchair', 13, 3); prop(m, 'item_armchair', 19, 3)
prop(m, 'item_armchair', 7, 8); prop(m, 'item_armchair', 22, 20)
prop(m, 'item_fireplace', 17, 1); prop(m, 'item_sofa', 10, 4)
for c in [(26, 7), (27, 7), (28, 7)]: prop(m, 'item_kitchen_counter', *c)
prop(m, 'item_kitchen_stove', 29, 7)
prop(m, 'item_bed_large', 3, 16); prop(m, 'item_nightstand', 2, 16); prop(m, 'item_nightstand', 5, 16)
prop(m, 'item_dresser', 8, 16); prop(m, 'item_vanity_mirror', 8, 20)
prop(m, 'item_staircase', 18, 16); prop(m, 'item_railing_v', 17, 15); prop(m, 'item_railing_v', 17, 16)
prop(m, 'item_bed_large', 28, 16); prop(m, 'item_nightstand', 27, 16); prop(m, 'item_dresser', 29, 18)
for c in [(8, 1), (20, 7), (10, 15), (29, 1)]: decor(m, 'item_vase', c[0], c[1], 'object')
for c in [(8, 8), (10, 7), (20, 16)]: decor(m, 'item_grandfather_clock', c[0], c[1], 'object')
for c in [(1, 1), (29, 15), (22, 7), (1, 7)]: decor(m, 'item_cobweb', c[0], c[1], 'wall')
for c in [(11, 0), (25, 0), (12, 6), (18, 6)]: decor(m, 'item_portrait_frame', c[0], c[1], 'wall')
decor(m, 'item_arch_window', 20, 0, 'wall')
decor(m, 'item_grand_chandelier', 15, 3, 'ceiling'); decor(m, 'item_grand_chandelier', 15, 11, 'ceiling')
decor(m, 'item_grand_chandelier', 15, 18, 'ceiling')
tov(m, 'tile_map1_window', [(5, 0), (13, 0), (29, 0), (1, 14), (4, 6), (27, 14)])
spot(m, 'bear_rug', 19, 5); spot(m, 'bear_rug', 5, 8)

# ---------------- MAP 2: Garasi & Bagasi ----------------
m = M[2]
prop(m, 'item_hydraulic_lift', 7, 6); prop(m, 'item_hydraulic_lift', 15, 11)
prop(m, 'item_tool_trolley', 9, 6); prop(m, 'item_tool_trolley', 19, 2); prop(m, 'item_tool_trolley', 14, 18)
for c in [(9, 1), (10, 1), (21, 1), (22, 1), (17, 10)]: prop(m, 'item_parts_shelf', *c)
prop(m, 'item_wheel_stack', 1, 8); prop(m, 'item_wheel_stack_b', 16, 8); prop(m, 'item_wheel_stack', 19, 18)
prop(m, 'item_bumper_pile', 16, 1); prop(m, 'item_bumper_pile', 10, 17)
prop(m, 'item_mechanic_cabinet', 9, 10); prop(m, 'item_mechanic_cabinet', 26, 1)
for c in [(10, 4), (22, 6), (14, 12), (5, 17), (20, 17)]: decor(m, 'item_shop_lamp', c[0], c[1], 'ceiling')
decor(m, 'item_garage_shutter', 11, 0, 'wall'); decor(m, 'item_garage_shutter', 24, 0, 'wall')
tov(m, 'tile_map2_shutter', [(7, 0), (8, 0), (20, 0), (21, 0)])
spot(m, 'tire_stack', 16, 6); spot(m, 'tire_stack', 26, 13)
spot(m, 'mechanic_cabinet', 7, 10); spot(m, 'mechanic_cabinet', 17, 13)

# ---------------- MAP 3: Ladang & Lumbung ----------------
m = M[3]
prop(m, 'item_windmill', 12, 10)
prop(m, 'item_well', 20, 12); prop(m, 'item_animal_trough', 24, 12); prop(m, 'item_animal_trough', 25, 12)
prop(m, 'item_harvest_rack', 10, 15); prop(m, 'item_harvest_rack', 11, 15)
prop(m, 'item_farmhouse_stove', 10, 3)
prop(m, 'item_firewood_stack', 3, 7); prop(m, 'item_firewood_stack', 28, 12)
prop(m, 'item_fence_gate_h', 4, 12); prop(m, 'item_fence_h_broken', 21, 16)
tov(m, 'tile_map3_crop_rows_b', [(x, y) for y in (10,) for x in range(2, 11)] + [(x, 19) for x in range(19, 29)])
tov(m, 'tile_map3_crop_rows_c', [(x, 11) for x in range(2, 11)] + [(x, 20) for x in range(19, 29)])
spot(m, 'silo_gap', 12, 7); spot(m, 'silo_gap', 17, 6)
spot(m, 'hay_wagon', 7, 12); spot(m, 'hay_wagon', 23, 20)

# ---------------- MAP 4: Gudang Logistik ----------------
m = M[4]
prop(m, 'item_forklift', 16, 13); prop(m, 'item_industrial_scale', 22, 15)
for c in [(23, 3), (26, 3), (28, 3)]: prop(m, 'item_cargo_rack_b', *c)
prop(m, 'item_shrink_wrap_pallet', 28, 15); prop(m, 'item_shrink_wrap_pallet', 18, 8)
decor(m, 'item_shipping_manifest_board', 5, 2, 'wall'); decor(m, 'item_safety_sign', 13, 2, 'wall')
decor(m, 'item_safety_sign', 22, 2, 'wall'); decor(m, 'item_safety_sign_exit', 30, 8, 'wall')
decor(m, 'item_shipping_manifest_board', 20, 17, 'wall')
for c in [(6, 9), (15, 9), (25, 9), (15, 18), (5, 4)]: decor(m, 'item_warehouse_lamp', c[0], c[1], 'ceiling')
tov(m, 'tile_map4_dock_door', [(2, 0), (3, 0), (4, 0), (22, 0), (23, 0), (24, 0)])
tov(m, 'tile_map4_dock_platform', [(2, 1), (3, 1), (4, 1), (22, 1), (23, 1), (24, 1)])
tov(m, 'tile_map4_conveyor_h', [(x, 13) for x in range(21, 27)])
tov(m, 'tile_map4_conveyor_v', [(27, y) for y in range(10, 14)])
spot(m, 'shrink_wrap_pallet', 26, 16); spot(m, 'shrink_wrap_pallet', 9, 13)
spot(m, 'shipping_container', 1, 13); spot(m, 'shipping_container', 20, 5)

# ---------------- MAP 5: Ruang Bawah Tanah ----------------
m = M[5]
prop(m, 'item_boiler', 10, 16); prop(m, 'item_washing_machine', 9, 19); prop(m, 'item_washing_machine', 10, 19)
prop(m, 'item_laundry_basket', 8, 19); prop(m, 'item_pipe_junction', 12, 6)
decor(m, 'item_electrical_panel', 20, 2, 'wall'); decor(m, 'item_electrical_panel', 22, 2, 'wall')
decor(m, 'item_emergency_lamp', 24, 2, 'wall'); decor(m, 'item_emergency_lamp', 12, 14, 'wall')
decor(m, 'item_vent_grate', 12, 2, 'wall'); decor(m, 'item_iron_ladder', 14, 2, 'wall')
decor(m, 'item_vent_grate_floor', 13, 10, 'floor')
decor(m, 'fx_water_leak', 7, 11, 'fx'); decor(m, 'fx_water_drip', 7, 10, 'fx')
decor(m, 'fx_water_leak', 20, 13, 'fx'); decor(m, 'fx_water_drip', 20, 12, 'fx')
tov(m, 'tile_map5_wall_pipe_v', [(16, 3), (16, 4), (16, 7), (10, 4), (10, 6)])
spot(m, 'pipe_recess', 11, 6); spot(m, 'pipe_recess', 25, 6)
spot(m, 'utility_cabinet', 10, 10); spot(m, 'utility_cabinet', 24, 8)

import collections
def relocate_bots(mm):
    T, o = mm['tiles'], mm['objects']; W, H = mm['width'], mm['height']
    solid = [[T[r][c] == 1 for c in range(W)] for r in range(H)]
    for p in o['props']:
        if p.get('collision'):
            for dx in range(p.get('w', 1)):
                for dy in range(p.get('h', 1)): solid[p['y'] + dy][p['x'] + dx] = True
            for e in p.get('extra_collision', []): solid[e['y']][e['x']] = True
    taken = {(h['x'], h['y']) for h in o['hiding_spots']} | {(k['x'], k['y']) for k in o['key_candidates']}
    for b in o['bot_spawn_candidates']:
        if solid[b['y']][b['x']]:
            best = None
            for r in range(H):
                for c in range(W):
                    if not solid[r][c] and (c, r) not in taken:
                        dd = abs(c - b['x']) + abs(r - b['y'])
                        if best is None or dd < best[0]: best = (dd, c, r)
            b['x'], b['y'] = best[1], best[2]
for mm in d['maps']: relocate_bots(mm)
M[1]['design_notes'] = ['Tile grid memakai kode schema v2: 0 floor, 1 wall, 2 pintu horizontal, 3 pintu vertikal.',
    'Bear Rug aktif sebagai hiding spot (tipe bear_rug terdaftar di SpotType).',
    'tile_overrides = penggantian sprite tile visual saja (collision tetap dari kode tile).',
    'Layer decor: floor, fx (animasi), wall, object, ceiling (digambar di atas karakter).']
for mm in d['maps']:
    mm.pop('asset_plan', None)
json.dump({k: v for k, v in d.items()}, open(OUT, 'w'), ensure_ascii=False, indent=1)
print('ok', OUT)

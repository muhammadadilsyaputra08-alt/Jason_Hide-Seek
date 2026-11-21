#!/usr/bin/env python3
"""Validasi maps.json: grid, koordinat, collision, keterjangkauan, tumpang tindih. exit 1 bila gagal."""
import json, sys, collections
d = json.load(open(sys.argv[1])); bad = 0
def solid_cells(s):
    return {(c, r) for r, row in enumerate(s) for c, v in enumerate(row) if v}
def err(*a):
    global bad; bad += 1; print('ERR', *a)
for m in d['maps']:
    W, H, T, o, i = m['width'], m['height'], m['tiles'], m['objects'], m['id']
    if len(T) != H or any(len(r) != W for r in T): err(i, 'grid size')
    if any(v not in (0, 1, 2, 3) for r in T for v in r): err(i, 'tile value')
    solid = [[T[r][c] == 1 for c in range(W)] for r in range(H)]
    occ = collections.defaultdict(list)
    def mark(c, r, tag):
        if not (0 <= c < W and 0 <= r < H): err(i, 'out of bounds', tag, c, r); return
        if solid[r][c] and T[r][c] != 1: pass
        if T[r][c] == 1: pass
        solid[r][c] = True; occ[(c, r)].append(tag)
    for p in o['props']:
        if p.get('collision'):
            for dx in range(p.get('w', 1)):
                for dy in range(p.get('h', 1)): mark(p['x'] + dx, p['y'] + dy, p['item'])
            for e in p.get('extra_collision', []): mark(e['x'], e['y'], p['item'])
        else: mark(p['x'], p['y'], p['item']) if False else None
    for dd in o['decor']:
        if not (0 <= dd['x'] < W and 0 <= dd['y'] < H): err(i, 'decor oob', dd)
        if dd.get('collision'): mark(dd['x'], dd['y'], dd['item'])
    for k, v in occ.items():
        if len(v) > 1: err(i, 'prop overlap', k, v)
        if T[k[1]][k[0]] != 0: err(i, 'prop on non-floor', k, v)
    _ja = m.get('jason_arrival') or {}
    chamber = set()
    if _ja.get('enabled'):
        _b = _ja['bounds']; chamber = {(c, r) for r in range(_b['y'], _b['y'] + _b['h']) for c in range(_b['x'], _b['x'] + _b['w'])}
    for t in m.get('tile_overrides', []):
        x, y, rg = t['x'], t['y'], t['region']
        base = T[y][x]
        ok = ('chamber_floor' in rg and base == 0 and (x, y) in chamber) or ('window' in rg or 'shutter' in rg or 'dock_door' in rg or 'wall_pipe' in rg) and base == 1 or \
             ('crop_rows' in rg or 'conveyor' in rg or 'dock_platform' in rg) and base == 0 or ('archway' in rg and base in (2, 3))
        if not ok: err(i, 'tile override mismatch', t, base)
    ja = m.get('jason_arrival') or {}
    chamber = set()
    if ja.get('enabled'):
        b = ja['bounds']; chamber = {(c, r) for r in range(b['y'], b['y'] + b['h']) for c in range(b['x'], b['x'] + b['w'])}
        sp = ja['spawn_position']
        if (sp['x'], sp['y']) not in chamber or (sp['x'], sp['y']) in solid_cells(solid): err(i, 'jason spawn not in free chamber cell', sp)
        d_ = ja['door']
        if T[d_['y']][d_['x']] != 1: err(i, 'chamber door must sit on wall tile', d_)
        if solid[d_['y'] + 1][d_['x']]: err(i, 'chamber door outside blocked', d_)
        if (d_['x'], d_['y'] - 1) not in chamber: err(i, 'chamber door not adjacent to interior', d_)
        for c, r in chamber:
            if T[r][c] != 0: err(i, 'chamber cell not floor', (c, r))
        # interior harus terhubung ke pintu
        s0 = (d_['x'], d_['y'] - 1); sn = {s0}; qq = collections.deque([s0])
        while qq:
            c, r = qq.popleft()
            for dc, dr in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (c + dc, r + dr)
                if n in chamber and not solid[n[1]][n[0]] and n not in sn: sn.add(n); qq.append(n)
        stuck = {c for c in chamber if not solid[c[1]][c[0]]} - sn
        if stuck: err(i, 'chamber interior unreachable from door', sorted(stuck))
    ex = o['exit']; ori = ex.get('orientation', 's')
    ap = {'s': (ex['x'], ex['y'] - 1), 'n': (ex['x'], ex['y'] + 1), 'e': (ex['x'] - 1, ex['y']), 'w': (ex['x'] + 1, ex['y'])}[ori]
    if solid[ap[1]][ap[0]]: err(i, 'exit approach solid', ap)
    seen = {ap}; q = collections.deque([ap])
    while q:
        c, r = q.popleft()
        for dc, dr in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (c + dc, r + dr)
            if 0 <= n[0] < W and 0 <= n[1] < H and not solid[n[1]][n[0]] and n not in seen: seen.add(n); q.append(n)
    free = {(c, r) for r in range(H) for c in range(W) if not solid[r][c]}
    free -= chamber; iso = free - seen
    if iso: err(i, 'unreachable floor cells', sorted(iso)[:12], len(iso))
    spotcells = set()
    for h in o['hiding_spots']:
        c = (h['x'], h['y'])
        if c in spotcells: err(i, 'dup spot', c)
        spotcells.add(c)
        if c not in seen: err(i, 'spot unreachable/solid', h['type_id'], c)
    for k in o['key_candidates']:
        if (k['x'], k['y']) not in seen: err(i, 'key unreachable/solid', k)
    for b in o.get('bot_spawn_candidates', []):
        if (b['x'], b['y']) not in seen: err(i, 'bot spawn bad', b)
    for who, s in o['spawns'].items():
        if who == 'jason' and ja.get('enabled'): continue
        if (s['x'], s['y']) not in seen: err(i, 'spawn bad', who, s)
    # kunci di atas hiding spot dilarang (allow_inside_hiding_spot=false)
    for k in o['key_candidates']:
        if (k['x'], k['y']) in spotcells: err(i, 'key on hiding spot', k)
    # pintu: sel depan pintu harus bebas
    for r in range(H):
        for c in range(W):
            if T[r][c] in (2, 3):
                nb = [(c, r - 1), (c, r + 1)] if T[r][c] == 2 else [(c - 1, r), (c + 1, r)]
                for n in nb:
                    if 0 <= n[0] < W and 0 <= n[1] < H and T[n[1]][n[0]] == 0 and solid[n[1]][n[0]]: err(i, 'door blocked', (c, r), n)
    print('map', i, m['name'], W, H, 'spots', len(o['hiding_spots']), 'floor', len(free), 'reach', len(seen))
sys.exit(1 if bad else 0)

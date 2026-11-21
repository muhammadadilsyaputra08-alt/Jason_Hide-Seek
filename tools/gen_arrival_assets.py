#!/usr/bin/env python3
"""Menggambar & mengemas aset ruang kedatangan Jason ke atlas (idempoten).
  python3 tools/gen_arrival_assets.py assets/atlases
Sprite baru:
  tiles.atlas : tile_map{1..5}_chamber_floor
  items.atlas : door_arrival_{wood,shutter,barn,steel,iron}_{closed,cracked,broken,debris}, item_chamber_seal (96x96),
                item_chamber_lamp (2 frame: padam/menyala), item_chain_wall, item_claw_marks, item_blood_drag"""
import sys, random, re
from PIL import Image, ImageDraw

A = sys.argv[1]
MATS = ['wood', 'shutter', 'barn', 'steel', 'iron']
ORIGIN = {'tiles': 160, 'items': 432}

def c(h, a=255): h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def load_atlas(name):
    L = open(f'{A}/{name}.atlas', encoding='utf-8').read().split('\n')
    head = L[:5]; regs = []; i = 5
    while i < len(L):
        if L[i] and not L[i].startswith(' '):
            xy = [int(v) for v in L[i + 2].split(':')[1].split(',')]; sz = [int(v) for v in L[i + 3].split(':')[1].split(',')]
            idx = int(L[i + 6].split(':')[1]); regs.append((L[i], xy, sz, idx)); i += 7
        else: i += 1
    return head, regs

def crop(im, regs, n):
    for r in regs:
        if r[0] == n: return im.crop((r[1][0], r[1][1], r[1][0] + r[2][0], r[1][1] + r[2][1]))
    raise KeyError(n)

# ---------------- gambar pintu ----------------
PAL = {
 'wood':    dict(main='#6b3a22', hi='#8f5230', lo='#3d2214', frame='#24140d', metal='#d9ac3e'),
 'shutter': dict(main='#868d96', hi='#aab1b9', lo='#575e67', frame='#2c3036', metal='#d6a21e'),
 'barn':    dict(main='#7a4a2a', hi='#9a6438', lo='#47291a', frame='#2a1a10', metal='#2b2b2e'),
 'steel':   dict(main='#6f7b86', hi='#9aa7b2', lo='#434c55', frame='#23282d', metal='#c9d2d9'),
 'iron':    dict(main='#3a4046', hi='#5a636b', lo='#1f2327', frame='#101214', metal='#7a4a2a'),
}

def door_closed(mat, wall):
    p = {k: c(v) for k, v in PAL[mat].items()}; im = wall.copy(); d = ImageDraw.Draw(im)
    d.rectangle([3, 2, 28, 31], fill=p['frame'])
    if mat == 'wood':
        for x0 in (5, 16):
            d.rectangle([x0, 4, x0 + 10, 31], fill=p['main'])
            d.line([x0, 4, x0, 31], fill=p['hi']); d.line([x0 + 10, 4, x0 + 10, 31], fill=p['lo'])
            d.rectangle([x0 + 2, 7, x0 + 8, 15], outline=p['lo']); d.rectangle([x0 + 2, 18, x0 + 8, 28], outline=p['lo'])
            d.line([x0 + 2, 8, x0 + 8, 8], fill=p['hi'])
        d.rectangle([14, 17, 15, 20], fill=p['metal']); d.rectangle([17, 17, 18, 20], fill=p['metal'])
    elif mat == 'shutter':
        for y in range(3, 31):
            d.line([4, y, 27, y], fill=p['hi'] if y % 3 == 0 else (p['main'] if y % 3 == 1 else p['lo']))
        d.rectangle([4, 29, 27, 31], fill=p['lo']); d.rectangle([13, 28, 18, 30], fill=p['metal'])
        d.line([4, 3, 4, 31], fill=p['frame']); d.line([27, 3, 27, 31], fill=p['frame'])
    elif mat == 'barn':
        for x in range(4, 28, 4):
            d.rectangle([x, 3, x + 3, 31], fill=p['main'] if (x // 4) % 2 else p['hi']); d.line([x + 3, 3, x + 3, 31], fill=p['lo'])
        d.line([4, 8, 27, 26], fill=p['lo'], width=2); d.line([4, 26, 27, 8], fill=p['lo'], width=2)
        d.rectangle([4, 7, 27, 9], fill=p['lo']); d.rectangle([4, 24, 27, 26], fill=p['lo'])
        for y in (8, 25): d.rectangle([5, y - 1, 7, y + 1], fill=p['metal']); d.rectangle([24, y - 1, 26, y + 1], fill=p['metal'])
    elif mat == 'steel':
        d.rectangle([5, 4, 26, 31], fill=p['main']); d.line([5, 4, 26, 4], fill=p['hi']); d.line([15, 4, 15, 31], fill=p['lo'])
        for y in range(6, 27, 6):
            for x in (7, 12, 18, 24): d.point((x, y), fill=p['metal'])
        for i in range(22, 31, 4):
            d.polygon([(i - 8 + 5, 28), (i - 8 + 9, 28), (i - 8 + 7, 31), (i - 8 + 3, 31)], fill=c('#e0b820'))
        d.rectangle([5, 27, 26, 27], fill=p['lo']); d.rectangle([13, 12, 17, 13], fill=c('#d33a2a'))
    else:
        d.rectangle([5, 4, 26, 31], fill=p['main'])
        for x in (9, 14, 19, 24): d.line([x, 4, x, 31], fill=p['lo'])
        for y in (7, 14, 21, 28):
            for x in (6, 11, 16, 21, 26): d.point((x, y), fill=p['hi'])
        d.rectangle([12, 10, 19, 12], fill=p['frame']); d.rectangle([14, 17, 17, 19], fill=p['metal'])
        for (x, y) in ((8, 24), (21, 9), (12, 29)): d.point((x, y), fill=p['metal'])
    return im

def door_cracked(mat, wall):
    im = door_closed(mat, wall); d = ImageDraw.Draw(im); p = {k: c(v) for k, v in PAL[mat].items()}
    k = c('#0b0707'); rnd = random.Random(7)
    pts = [(16, 3)]; x, y = 16, 3
    while y < 30: y += rnd.randint(3, 5); x += rnd.randint(-3, 3); pts.append((max(5, min(26, x)), min(31, y)))
    d.line(pts, fill=k, width=1)
    for (bx, by) in pts[1:4]: d.line([bx, by, bx + rnd.choice((-5, 5)), by + 3], fill=k)
    d.ellipse([9, 13, 22, 24], outline=p['lo']) if mat != 'shutter' else d.arc([8, 12, 23, 25], 20, 200, fill=p['lo'])
    for _ in range(14): d.point((rnd.randint(5, 26), rnd.randint(6, 30)), fill=p['hi'])
    d.line([3, 31, 28, 31], fill=c('#120c0a'))
    return im

def door_broken(mat, wall):
    p = {k: c(v) for k, v in PAL[mat].items()}; im = wall.copy(); d = ImageDraw.Draw(im); rnd = random.Random(11)
    d.rectangle([3, 2, 28, 31], fill=p['frame'])
    d.rectangle([5, 4, 26, 31], fill=c('#0a0608'))
    for y in range(4, 32): d.line([5, y, 26, y], fill=(10 + y // 4, 6 + y // 6, 8 + y // 6, 255))   # ruang gelap, makin terang ke lantai
    # sisa daun pintu di kiri & kanan (miring, bergerigi)
    d.polygon([(5, 4), (11, 4), (9, 12), (11, 19), (7, 31), (5, 31)], fill=p['main'])
    d.polygon([(26, 4), (22, 4), (23, 11), (21, 17), (24, 24), (26, 31)], fill=p['lo'])
    d.line([5, 4, 11, 4], fill=p['hi']); d.line([5, 4, 5, 31], fill=p['hi'])
    d.polygon([(12, 4), (14, 4), (13, 9)], fill=p['main'])                    # serpihan tergantung
    d.polygon([(18, 4), (21, 4), (19, 8)], fill=p['lo'])
    if mat == 'shutter': d.line([5, 4, 26, 4], fill=p['hi']); d.rectangle([5, 4, 26, 7], fill=p['main'])  # shutter terangkat
    for _ in range(10): d.point((rnd.randint(6, 25), rnd.randint(8, 30)), fill=p['hi'])
    return im

def door_debris(mat):
    p = {k: c(v) for k, v in PAL[mat].items()}; im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im); rnd = random.Random(23)
    for _ in range(9):
        x, y = rnd.randint(2, 27), rnd.randint(6, 28); w, h = rnd.randint(2, 6), rnd.randint(1, 3)
        col = rnd.choice([p['main'], p['hi'], p['lo']])
        if rnd.random() < .5: d.rectangle([x, y, x + w, y + h], fill=col)
        else: d.polygon([(x, y), (x + w, y + 1), (x + 1, y + h + 2)], fill=col)
    for _ in range(12): d.point((rnd.randint(1, 30), rnd.randint(4, 30)), fill=c('#5a4e48', 200))
    if mat in ('steel', 'iron', 'shutter'): d.rectangle([20, 20, 22, 21], fill=p['metal'])
    return im

# ---------------- objek chamber ----------------
def seal():
    im = Image.new('RGBA', (96, 96), (0, 0, 0, 0)); d = ImageDraw.Draw(im); red = c('#7d0f14', 215); dk = c('#3a0508', 230)
    d.ellipse([8, 8, 87, 87], outline=dk, width=3); d.ellipse([12, 12, 83, 83], outline=red, width=1)
    d.ellipse([26, 26, 69, 69], outline=red, width=1)
    import math
    pts = [(48 + 36 * math.cos(math.radians(-90 + 72 * i)), 48 + 36 * math.sin(math.radians(-90 + 72 * i))) for i in range(5)]
    for i in range(5): d.line([pts[i], pts[(i + 2) % 5]], fill=red, width=1)
    for (x, y) in pts: d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=dk)
    rnd = random.Random(5)
    for _ in range(40):
        a = rnd.random() * 6.28; r = rnd.uniform(10, 40); d.point((48 + r * math.cos(a), 48 + r * math.sin(a)), fill=c('#2a0305', 200))
    return im

def lamp(on):
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle([13, 8, 18, 22], fill=c('#2b2f33')); d.rectangle([12, 21, 19, 23], fill=c('#15181a'))
    d.rectangle([14, 11, 17, 19], fill=c('#ff3b2a') if on else c('#5a1713'))
    if on:
        d.rectangle([14, 11, 15, 13], fill=c('#ffd2c2'))
        for r, a in ((9, 60), (13, 28)): d.ellipse([16 - r, 15 - r, 16 + r, 15 + r], outline=c('#ff3b2a', a))
    return im

def chain():
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for x in (10, 21):
        d.rectangle([x - 1, 2, x + 1, 4], fill=c('#2a2d30'))
        for y in range(6, 24, 4): d.ellipse([x - 2, y, x + 2, y + 3], outline=c('#8c949b')) if (y // 4) % 2 else d.rectangle([x - 1, y, x, y + 4], fill=c('#6f777e'))
        d.rectangle([x - 3, 24, x + 3, 27], outline=c('#5a6168')); d.rectangle([x - 1, 25, x + 1, 26], fill=c('#2a2d30'))
    return im

def claw():
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for i, x in enumerate((8, 13, 18, 23)):
        d.line([x, 6 + i % 2, x - 3, 26 - i % 2], fill=c('#120a0a', 230), width=1); d.line([x + 1, 8, x - 2, 24], fill=c('#4a1b18', 150))
    return im

def drag():
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im); rnd = random.Random(31)
    for y in range(2, 30, 2): d.line([14 + rnd.randint(-2, 2), y, 17 + rnd.randint(-2, 2), y + 2], fill=c('#5a0f12', 220), width=3)
    for _ in range(14): d.point((rnd.randint(8, 24), rnd.randint(2, 30)), fill=c('#7d1418', 230))
    return im

def chamber_floor(wall_face, floor):
    im = floor.copy().convert('RGBA'); d = ImageDraw.Draw(im, 'RGBA'); rnd = random.Random(3)
    d.rectangle([0, 0, 31, 31], fill=(12, 6, 10, 120))                                  # lebih gelap
    for _ in range(26): d.point((rnd.randint(0, 31), rnd.randint(0, 31)), fill=(0, 0, 0, 150))
    d.line([0, 15, 31, 15], fill=(0, 0, 0, 70)); d.line([15, 0, 15, 31], fill=(0, 0, 0, 70))
    for _ in range(5): d.point((rnd.randint(2, 29), rnd.randint(2, 29)), fill=(110, 18, 20, 160))
    return im

# ---------------- pengemasan ----------------
def pack(name, sprites):
    im = Image.open(f'{A}/{name}.png').convert('RGBA'); head, regs = load_atlas(name)
    names = {n for n, _ in sprites}; keep = []
    for r in regs:
        if r[0] in names:                                      # hapus versi lama (idempoten)
            im.paste((0, 0, 0, 0), (r[1][0], r[1][1], r[1][0] + r[2][0], r[1][1] + r[2][1]))
        else: keep.append(r)
    # hitung ukuran akhir
    x, y, rowh = 2, ORIGIN[name], 0; place = []
    W = im.width
    for n, spr in sprites:
        w, h = spr.size
        if x + w + 2 > W: x = 2; y += rowh + 2; rowh = 0
        place.append((n, x, y, spr)); x += w + 2; rowh = max(rowh, h)
    need = y + rowh + 2; H = im.height
    while H < need: H *= 2
    if H != im.height:
        big = Image.new('RGBA', (W, H), (0, 0, 0, 0)); big.paste(im, (0, 0)); im = big
    for n, px, py, spr in place:
        im.paste((0, 0, 0, 0), (px, py, px + spr.width, py + spr.height)); im.alpha_composite(spr, (px, py))
    head = [l if not l.startswith('size:') else f'size: {W},{H}' for l in head]
    out = list(head)
    for (n, xy, sz, idx) in keep:
        out += [n, '  rotate: false', f'  xy: {xy[0]}, {xy[1]}', f'  size: {sz[0]}, {sz[1]}', f'  orig: {sz[0]}, {sz[1]}', '  offset: 0, 0', f'  index: {idx}']
    for n, px, py, spr in place:
        idx = -1; base = n
        m = re.match(r'(.*)#(\d+)$', n)
        if m: base, idx = m.group(1), int(m.group(2))
        out += [base, '  rotate: false', f'  xy: {px}, {py}', f'  size: {spr.width}, {spr.height}', f'  orig: {spr.width}, {spr.height}', '  offset: 0, 0', f'  index: {idx}']
    open(f'{A}/{name}.atlas', 'w', encoding='utf-8').write('\n'.join(out) + '\n')
    im.save(f'{A}/{name}.png'); print(name, im.size, len(place), 'sprite baru')

tim = Image.open(f'{A}/tiles.png').convert('RGBA'); _, treg = load_atlas('tiles')
tile_sprites = []
for n in range(1, 6):
    tile_sprites.append((f'tile_map{n}_chamber_floor', chamber_floor(crop(tim, treg, f'tile_map{n}_wall_face'), crop(tim, treg, f'tile_map{n}_floor_a'))))
item_sprites = []
for n, mat in enumerate(MATS, 1):
    wall = crop(tim, treg, f'tile_map{n}_wall_face')
    item_sprites += [(f'door_arrival_{mat}_closed', door_closed(mat, wall)), (f'door_arrival_{mat}_cracked', door_cracked(mat, wall)),
                     (f'door_arrival_{mat}_broken', door_broken(mat, wall)), (f'door_arrival_{mat}_debris', door_debris(mat))]
item_sprites += [('item_chamber_seal', seal()), ('item_chamber_lamp#0', lamp(False)), ('item_chamber_lamp#1', lamp(True)),
                 ('item_chain_wall', chain()), ('item_claw_marks', claw()), ('item_blood_drag', drag())]
pack('tiles', tile_sprites); pack('items', item_sprites)

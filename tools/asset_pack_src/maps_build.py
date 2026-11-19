from collections import deque
import sys, pickle, json, random, os
sys.path.insert(0,'/home/claude/work/build')
from core import *
from pfont import *
from items import ITEMS
ROOT='/home/claude/work/out/jason_hide_seek_assets_v2'
A=pickle.load(open('/home/claude/work/all.pkl','rb')); CH,TL,IT,UI=A['CH'],A['TL'],A['IT'],A['UI']
src=json.load(open('/mnt/user-data/uploads/jason_hide_seek_maps_v1.json'))
META={n:(m,cat,fp) for n,fn,m,cat,fp in ITEMS}
def items_of(m,cat,wide=None):
    return [n for n,(mm,c,fp) in META.items() if mm==m and c==cat and (wide is None or (fp[0]>1)==wide)]
EXCL={'fence_h','fence_v','vehicle'}   # dipasang lewat post-process di bawah
WALLDECOR={1:[('painting',6,0),('wall_clock',10,0),('painting',19,0),('painting',15,8),('wall_clock',22,8)],
           5:[('pipes',6,0),('valve',10,0),('hanging_lamp',16,0),('pipes',22,0),('valve',22,8),('hanging_lamp',6,8)]}
T=32
ACC={1:['c'],2:['b'],3:['b','c'],4:[],5:['b','c']}
def build(m):
    W,H=m['width'],m['height']; g=m['grid']; mid=m['id']; rnd=random.Random(mid*101)
    wall=lambda x,y: 0<=x<W and 0<=y<H and g[y][x] in(1,10)
    # --- layer tile lingkungan
    env=[[0]*W for _ in range(H)]
    for y in range(H):
        for x in range(W):
            v=g[y][x]
            if v in(1,10): env[y][x]=1
            elif v==2: env[y][x]=2 if (wall(x-1,y) and wall(x+1,y)) else 3
    reserved=set()
    objs={'spawns':m['spawn'],'exit':dict(m['exit'],orientation='s' if m['exit']['y']==H-1 else 'e'),'hiding_spots':[],'props':[],'decor':[],'key_candidates':m['key_spawn_candidates']}
    for k in m['key_spawn_candidates']: reserved.add((k['x'],k['y']))
    for h in m['hiding_spots']: reserved.add((h['x'],h['y'])); objs['hiding_spots'].append(dict(h,sprite_prefix='obj_'+h['type_id'],collision=False,states=['intact','checked','broken_clean','broken_bloody']))
    for p in m['spawn'].values(): reserved.add((p['x'],p['y']))
    ex=m['exit']; reserved.add((ex['x'],ex['y']))
    for y in range(H):
        for x in range(W):
            if g[y][x]==2:
                for dx,dy in((0,0),(1,0),(-1,0),(0,1),(0,-1)): reserved.add((x+dx,y+dy))
    for dx,dy in((1,0),(-1,0),(0,1),(0,-1)): reserved.add((ex['x']+dx,ex['y']+dy))
    # --- solid props
    singles=[n for n in items_of(mid,'collision',False) if n not in EXCL]; wides=[n for n in items_of(mid,'collision',True) if n not in EXCL]
    placed=set(); occ={}
    for y in range(H):
        x=0
        while x<W:
            if g[y][x]==3 and (x,y) not in placed:
                run=1
                while x+run<W and g[y][x+run]==3: run+=1
                if run>=2 and wides:
                    n=wides[(x+y)%len(wides)]; objs['props'].append({'x':x,'y':y,'item':'item_'+n,'w':2,'h':1,'collision':True}); placed|={(x,y),(x+1,y)}; occ[(x,y)]=n; occ[(x+1,y)]=n; x+=2; continue
                n=singles[len([1 for q in objs['props'] if q['w']==1])%len(singles)]; objs['props'].append({'x':x,'y':y,'item':'item_'+n,'w':1,'h':1,'collision':True}); placed.add((x,y)); occ[(x,y)]=n
            x+=1
    # --- aset khusus: vehicle (2 sel, +1 sel collision) dan pagar
    def reach_ok(extra):
        sp0=(m['spawn']['player']['x'],m['spawn']['player']['y']); sn={sp0}; qq=deque([sp0])
        while qq:
            x,y=qq.popleft()
            for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
                nx,ny=x+dx,y+dy
                if 0<=nx<W and 0<=ny<H and (nx,ny) not in sn and g[ny][nx] not in(1,3,10) and (nx,ny) not in extra: sn.add((nx,ny)); qq.append((nx,ny))
        need=[(h['x'],h['y']) for h in m['hiding_spots']]+[(k['x'],k['y']) for k in m['key_spawn_candidates']]+[(m['exit']['x']+d[0],m['exit']['y']+d[1]) for d in((0,-1),(-1,0))]
        return all(c in sn or g[c[1]][c[0]] in(1,3) for c in need[:-2]) and (any(c in sn for c in need[-2:]))
    extra=set()
    if mid==2:
        n_v=0; vrows=set()
        for p in sorted(objs['props'],key=lambda p:(p['y'],p['x'])):
            if n_v>=2 or p['w']!=1 or (n_v==1 and p['y'] in vrows): continue
            for dx in(1,-1):
                nx,ny=p['x']+dx,p['y']
                if 0<=nx<W and g[ny][nx]==0 and (nx,ny) not in reserved and (nx,ny) not in occ and reach_ok(extra|{(nx,ny)}):
                    if dx==-1: p['x']=nx
                    p['item']='item_vehicle'; p['w']=2; p['extra_collision']=[{'x':nx,'y':ny}]; extra.add((nx,ny)); occ[(nx,ny)]='vehicle'; occ[(p['x'] if dx==1 else p['x'],p['y'])]='vehicle'; n_v+=1; vrows.add(p['y']); break
            if n_v and p['item']=='item_vehicle' and p.get('extra_collision') and n_v>=2: break
        # jangan pakai dua kendaraan bersebelahan di baris sama
    if mid==3:
        cnt=0
        for p in sorted(objs['props'],key=lambda p:(p['x']*13+p['y']*7)%11):
            if p['w']==1 and cnt<4: p['item']='item_fence_h' if cnt%2==0 else 'item_fence_v'; cnt+=1
    # --- dekor
    free=lambda x,y: 0<=x<W and 0<=y<H and g[y][x]==0 and (x,y) not in reserved and (x,y) not in occ
    for n,x,y in WALLDECOR.get(mid,[]): objs['decor'].append({'x':x,'y':y,'item':'item_'+n,'layer':'wall','collision':False})
    used=set()
    for n in items_of(mid,'decorative_floor'):
        w=META[n][2][0]
        for _ in range(60):
            x=rnd.randint(1,W-3); y=rnd.randint(1,H-2)
            cells=[(x+i,y) for i in range(w)]
            if all(free(*c) and c not in used for c in cells):
                used|=set(cells); objs['decor'].append({'x':x,'y':y,'item':'item_'+n,'layer':'floor','w':w,'collision':False}); break
    for n in items_of(mid,'decorative'):
        for _ in range(2):
            for _t in range(80):
                px_=rnd.choice(list(occ)) if occ else (3,3); dx,dy=rnd.choice([(1,0),(-1,0),(0,1),(0,-1)]); x,y=px_[0]+dx,px_[1]+dy
                if free(x,y) and (x,y) not in used:
                    used.add((x,y)); objs['decor'].append({'x':x,'y':y,'item':'item_'+n,'layer':'object','collision':False}); break
    # --- BFS reachability

    sp=(m['spawn']['player']['x'],m['spawn']['player']['y']); seen={sp}; q=deque([sp])
    solid=lambda x,y: g[y][x] in(1,3,10) or (x,y) in extra
    while q:
        x,y=q.popleft()
        for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
            nx,ny=x+dx,y+dy
            if 0<=nx<W and 0<=ny<H and (nx,ny) not in seen and not solid(nx,ny): seen.add((nx,ny)); q.append((nx,ny))
    unreach=[k for k in m['key_spawn_candidates'] if (k['x'],k['y']) not in seen]
    return env,objs,seen,occ,reserved,unreach
def tile(m,name): return TL[(f'tile_map{m}_{name}',-1)]
def render(m,env,objs,seen,occ,reserved,debug=False):
    W,H=m['width'],m['height']; mid=m['id']; rnd=random.Random(mid*977); g=m['grid']
    im=Image.new('RGBA',(W*T,H*T),(0,0,0,255))
    for y in range(H):
        for x in range(W):
            e=env[y][x]
            if e==0:
                r=rnd.random(); acc=ACC[mid]; t=tile(mid,'floor_'+(acc[int(r*100)%len(acc)] if (acc and r<.1) else 'a'))
            elif e==1:
                below=y+1<H and env[y+1][x]!=1
                t=tile(mid,'wall_face' if below else 'wall_top')
            elif e==2: t=tile(mid,'door')
            else: t=tile(mid,'door_v')
            im.alpha_composite(t,(x*T,y*T))
    for y in range(H):
        for x in range(W):
            if env[y][x]==1 and y+1<H and env[y+1][x]==0: im.alpha_composite(TL[('tile_shadow_wall',-1)],(x*T,(y+1)*T))
    for d in objs['decor']:
        if d['layer']=='floor': im.alpha_composite(IT[(d['item'],-1)],(d['x']*T,d['y']*T))
    for d in objs['decor']:
        if d['layer']=='wall': im.alpha_composite(IT[(d['item'],-1)],(d['x']*T,d['y']*T))
    e=objs['exit']; im.alpha_composite(IT[(f"exit_locked_{e['orientation']}",-1)],(e['x']*T,e['y']*T))
    ents=[]
    for p in objs['props']: ents.append(((p['y']+1)*T,IT[(p['item'],-1)],p['x']*T,p['y']*T))
    for d in objs['decor']:
        if d['layer']=='object': ents.append(((d['y']+1)*T,IT[(d['item'],-1)],d['x']*T,d['y']*T))
    for h in objs['hiding_spots']: ents.append(((h['y']+1)*T,IT[(h['sprite_prefix']+'_intact',-1)],h['x']*T,h['y']*T))
    # kunci (3 kandidat acak yang terjangkau)
    cands=[k for k in objs['key_candidates'] if (k['x'],k['y']) in seen]; ks=rnd.sample(cands,3)
    for i,k in enumerate(ks):
        gl=Image.new('RGBA',(32,32),(0,0,0,0)); d=ImageDraw.Draw(gl); d.ellipse([4,4,27,27],fill=(255,230,120,55)); d.ellipse([9,9,22,22],fill=(255,230,120,60))
        im.alpha_composite(gl,(k['x']*T,k['y']*T)); ents.append(((k['y']+1)*T+1,IT[(f'key_frame_{i+1}',-1)],k['x']*T+8,k['y']*T+8))
    # karakter
    sp=objs['spawns']; occp={(h['x'],h['y']) for h in objs['hiding_spots']}
    jx,jy=sp['jason']['x'],sp['jason']['y']; jim=CH[('jason_idle_down',0)]; ents.append(((jy+1)*T,jim,jx*T+16-24,jy*T+30-43))
    px_,py_=sp['player']['x'],sp['player']['y']; ents.append(((py_+1)*T,CH[('player_idle_down',0)],px_*T,py_*T+2))
    cells=[c for c in seen if c not in reserved and c not in occp]; rnd.shuffle(cells); chosen=[]
    for c in cells:
        if all(abs(c[0]-o[0])+abs(c[1]-o[1])>=4 for o in chosen) and abs(c[0]-jx)+abs(c[1]-jy)>6: chosen.append(c)
        if len(chosen)==9: break
    dirs=['down','up','left','right']
    for i,c in enumerate(chosen):
        d=dirs[i%4]; an='walk' if i%2==0 else 'idle'; fr_=CH[(f'bot{i+1}_{an}_{d}',1 if an=='walk' else 0)]
        ents.append(((c[1]+1)*T,fr_,c[0]*T,c[1]*T+2))
    for yb,sprite,x,y in sorted(ents,key=lambda e:e[0]): im.alpha_composite(sprite,(x,y))
    if debug:
        ov=Image.new('RGBA',im.size,(0,0,0,0)); d=ImageDraw.Draw(ov); texts=[]
        for x in range(W+1): d.line([x*T,0,x*T,H*T],fill=(255,255,255,28))
        for y in range(H+1): d.line([0,y*T,W*T,y*T],fill=(255,255,255,28))
        extra={(e_['x'],e_['y']) for p_ in objs['props'] for e_ in p_.get('extra_collision',[])}
        for y in range(H):
            for x in range(W):
                if g[y][x] in(1,3,10) or (x,y) in extra: d.rectangle([x*T,y*T,x*T+31,y*T+31],fill=(255,40,40,60))
        for h in objs['hiding_spots']: d.rectangle([h['x']*T+1,h['y']*T+1,h['x']*T+30,h['y']*T+30],outline=(60,230,255,255),width=2)
        for k in objs['key_candidates']:
            cx,cy=k['x']*T+16,k['y']*T+16; d.polygon([(cx,cy-9),(cx+9,cy),(cx,cy+9),(cx-9,cy)],outline=(255,225,60,255),fill=(255,225,60,70))
        for key,col,lab in(('player',(60,255,100,255),'P'),('jason',(255,60,60,255),'J')):
            c=objs['spawns'][key]; d.rectangle([c['x']*T+2,c['y']*T+2,c['x']*T+29,c['y']*T+29],outline=col,width=2); texts.append((lab,c['x']*T+13,c['y']*T+12,col))
        e=objs['exit']; d.rectangle([e['x']*T,e['y']*T,e['x']*T+31,e['y']*T+31],outline=(80,255,160,255),width=2)
        lg=[('hiding spot',(60,230,255)),('key candidate',(255,225,60)),('collision',(255,40,40)),('exit',(80,255,160))]
        d.rectangle([34,34,170,92],fill=(0,0,0,210))
        for i,(t,c) in enumerate(lg): d.rectangle([40,40+i*13,48,48+i*13],fill=c+(255,)); texts.append((t.upper(),54,41+i*13,(240,240,240,255)))
        im.alpha_composite(ov)
        for t,x,y,c in texts: draw_text(im,t,x,y,c,1,shadow=(0,0,0,255))
    return im
def main():
    out={'schema_version':2,'tile_size':32,'coordinate_system':src['coordinate_system'],
         'atlases':{'tiles':'atlases/tiles.atlas','items':'atlases/items.atlas','characters':'atlases/characters.atlas','ui':'atlases/ui.atlas'},
         'tile_legend':{'0':'floor','1':'wall (wall_face bila sel bawah bukan wall, selain itu wall_top)','2':'door_h (pintu depan di dinding horizontal)','3':'door_v (ambang pintu di dinding vertikal)'},
         'object_notes':'Tile layer hanya lingkungan. Spawn, exit, hiding spot, props, decor, dan key candidate adalah layer objek terpisah (x,y = sel grid; sprite item_* diambil dari items.atlas).','maps':[]}
    sheet=[]; report=[]
    for m in src['maps']:
        env,objs,seen,occ,res,unreach=build(m)
        out['maps'].append({'id':m['id'],'name':m['name'],'width':m['width'],'height':m['height'],'tiles':env,'objects':objs,'design_notes':m['design_notes']})
        c=render(m,env,objs,seen,occ,res); c.save(f'{ROOT}/maps_preview/map{m["id"]}_visual.png')
        render(m,env,objs,seen,occ,res,debug=True).save(f'{ROOT}/maps_preview/map{m["id"]}_debug.png')
        sheet.append(c); report.append((m['id'],len(objs['props']),len(objs['decor']),len(unreach),len(seen)))
    json.dump(out,open(f'{ROOT}/assets/maps/maps.json','w'),indent=1)
    S=Image.new('RGBA',(1600+30,3*544+40),(14,10,14,255))
    for i,c in enumerate(sheet): S.alpha_composite(c,(10+(i%2)*810,10+(i//2)*554))
    S.save(f'{ROOT}/maps_preview/all_maps_contact_sheet.png'); print(report)
main()

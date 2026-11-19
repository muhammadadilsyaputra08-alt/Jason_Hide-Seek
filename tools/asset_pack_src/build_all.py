import sys, shutil, zipfile
sys.path.insert(0,'/home/claude/work/build')
from core import *
from chars import *
from items import *
from ui_brand import *
ROOT='/home/claude/work/out/jason_hide_seek_assets_v2'
shutil.rmtree('/home/claude/work/out',ignore_errors=True)
for d in('assets/atlases','assets/maps','assets/branding','assets/audio','sprites_new','maps_preview','android_res'): os.makedirs(f'{ROOT}/{d}',exist_ok=True)
Cc=load_atlas('characters'); Wd=load_atlas('world'); Ui=load_atlas('ui')
NEW={'characters':{},'items':{},'tiles':{},'ui':{}}
# ===== CHARACTERS =====
CH={}
CH.update(Cc)
P={k:v for k,v in Cc.items() if k[0].startswith('player_')}
for b,(pal,kind) in PAL.items():
    for k,v in make_bot(P,b,pal,kind).items(): CH[k]=v; NEW['characters'][k]=v
def fr(who,n): return [CH[(f'{who}_{n}',i)] for i in range(20) if (f'{who}_{n}',i) in CH]
for who in['player']+[f'bot{i}' for i in range(1,10)]:
    F={f'{who}_{n}':fr(who,n) for n in('idle_down','idle_up','sprint_up')}
    A=anims(F,who)
    for an,lst in A.items():
        if an=='spectator' and who!='player': continue
        for i,im in enumerate(lst): CH[(f'{who}_{an}',i)]=im; NEW['characters'][(f'{who}_{an}',i)]=im
for k in [k for k in CH if k[0].startswith('jason_')]: CH[k]=pad_jason(CH[k]); NEW['characters'][k]=CH[k]
J={n:fr_ for n in {k[0] for k in Cc if k[0].startswith('jason_')} for fr_ in [[CH[(n,i)] for i in range(20) if (n,i) in CH]]}
for n,lst in jason_new(J).items():
    for i,im in enumerate(lst): CH[(n,i)]=im; NEW['characters'][(n,i)]=im
# ===== TILES =====
TL={k:v for k,v in Wd.items() if k[0].startswith('tile_')}
for m in range(1,6):
    fl=Wd[(f'tile_map{m}_floor_a',-1)].copy(); wt=Wd[(f'tile_map{m}_wall_top',-1)]
    for y in(0,1,2,29,30,31): 
        for x in range(32): fl.putpixel((x,y),wt.getpixel((x,y)))
    for x in range(32):
        for y in(3,28): 
            p=fl.getpixel((x,y)); fl.putpixel((x,y),lerp(p,(0,0,0),.35)+(255,))
    TL[(f'tile_map{m}_door_v',-1)]=fl; NEW['tiles'][(f'tile_map{m}_door_v',-1)]=fl
sh=Image.new('RGBA',(32,8),(0,0,0,0))
for y in range(8):
    for x in range(32): sh.putpixel((x,y),(0,0,0,int(95*(1-y/8))))
TL[('tile_shadow_wall',-1)]=sh; NEW['tiles'][('tile_shadow_wall',-1)]=sh
# ===== ITEMS =====
IT={k:v for k,v in Wd.items() if not k[0].startswith('tile_')}
for nm,fn,m,cat,fp in ITEMS:
    im=fn(); IT[(f'item_{nm}',-1)]=im; NEW['items'][(f'item_{nm}',-1)]=im
Pk,fo=key_assets()
for i,p in enumerate(Pk): IT[(f'key_frame_{i+1}',-1)]=p; NEW['items'][(f'key_frame_{i+1}',-1)]=p
IT[('key_complete',-1)]=fo; NEW['items'][('key_complete',-1)]=fo
for i,f in enumerate(fx_pickup()): IT[('fx_key_pickup',i)]=f; NEW['items'][('fx_key_pickup',i)]=f
for i,f in enumerate(fx_assembly(Pk,fo)): IT[('fx_key_assembly',i)]=f; NEW['items'][('fx_key_assembly',i)]=f
for side in('s','e'):
    for st in('locked','open'):
        im=exit_door(st,side); IT[(f'exit_{st}_{side}',-1)]=im; NEW['items'][(f'exit_{st}_{side}',-1)]=im
    for i in range(6):
        im=exit_door('unlock',side,i); IT[(f'exit_unlock_{side}',i)]=im; NEW['items'][(f'exit_unlock_{side}',i)]=im
# ===== UI =====
UI=dict(Ui); un=ui_new(Ui); UI.update(un); NEW['ui'].update(un)
# ===== PACK =====
def pack(R,name,W=1024):
    items=sorted(R.items(),key=lambda kv:(-kv[1].height,kv[0][0],kv[0][1]))
    x=y=2; rh=0; pos={}
    for k,im in items:
        if x+im.width+2>W: x=2; y+=rh+2; rh=0
        pos[k]=(x,y); x+=im.width+2; rh=max(rh,im.height)
    H_=y+rh+2; Hp=1
    while Hp<H_: Hp*=2
    atl=Image.new('RGBA',(W,Hp),(0,0,0,0))
    for k,(px_,py_) in pos.items(): atl.alpha_composite(R[k],(px_,py_))
    atl.save(f'{ROOT}/assets/atlases/{name}.png')
    names=sorted({k[0] for k in R}); lines=[f'{name}.png',f'size: {W},{Hp}','format: RGBA8888','filter: Nearest,Nearest','repeat: none']; js=[]
    for n in names:
        for k in sorted([k for k in R if k[0]==n],key=lambda k:k[1]):
            px_,py_=pos[k]; im=R[k]
            lines+= [n,'  rotate: false',f'  xy: {px_}, {py_}',f'  size: {im.width}, {im.height}',f'  orig: {im.width}, {im.height}','  offset: 0, 0',f'  index: {k[1]}']
            js.append({'name':n,'index':k[1],'x':px_,'y':py_,'w':im.width,'h':im.height})
    open(f'{ROOT}/assets/atlases/{name}.atlas','w').write('\n'.join(lines)+'\n')
    json.dump({'image':f'{name}.png','size':[W,Hp],'filter':'Nearest','regions':js},open(f'{ROOT}/assets/atlases/{name}.json','w'),indent=1)
    return len(R),(W,Hp)
stats={}
stats['characters']=pack(CH,'characters'); stats['tiles']=pack(TL,'tiles',512); stats['items']=pack(IT,'items'); stats['ui']=pack(UI,'ui',512)
# sprites_new (hanya aset baru)
cnt=0
for grp,R in NEW.items():
    os.makedirs(f'{ROOT}/sprites_new/{grp}',exist_ok=True)
    for (n,i),im in R.items():
        im.save(f'{ROOT}/sprites_new/{grp}/{n}_{i}.png' if i>=0 else f'{ROOT}/sprites_new/{grp}/{n}.png'); cnt+=1
print(stats,'new sprites',cnt, {g:len(r) for g,r in NEW.items()})
import pickle; pickle.dump({'CH':CH,'TL':TL,'IT':IT,'UI':UI},open('/home/claude/work/all.pkl','wb'))
# ===== BRANDING =====
B=f'{ROOT}/assets/branding'; lg=logo(); lg.save(f'{B}/logo.png')
sp=Image.new('RGBA',(720,360),(0,0,0,0)); sp.alpha_composite(lg,((720-lg.width)//2,(360-lg.height)//2)); sp.save(f'{B}/splash_logo.png')
app_icon(512).save(f'{B}/app_icon.png'); icon_fg(432,.5).save(f'{B}/adaptive_icon_foreground.png'); bg_icon(432).save(f'{B}/adaptive_icon_background.png')
dens={'mdpi':(48,108),'hdpi':(72,162),'xhdpi':(96,216),'xxhdpi':(144,324),'xxxhdpi':(192,432)}
for dn,(s,a) in dens.items():
    d=f'{ROOT}/android_res/mipmap-{dn}'; os.makedirs(d,exist_ok=True)
    ic=app_icon(512).resize((s,s),Image.NEAREST if False else Image.LANCZOS); ic.save(f'{d}/ic_launcher.png')
    m=Image.new('L',(s,s),0); ImageDraw.Draw(m).ellipse([0,0,s-1,s-1],fill=255); r=Image.new('RGBA',(s,s),(0,0,0,0)); r.paste(ic,(0,0),m); r.save(f'{d}/ic_launcher_round.png')
    icon_fg(432,.5).resize((a,a),Image.LANCZOS).save(f'{d}/ic_launcher_foreground.png'); bg_icon(432).resize((a,a),Image.LANCZOS).save(f'{d}/ic_launcher_background.png')
os.makedirs(f'{ROOT}/android_res/mipmap-anydpi-v26',exist_ok=True)
xml='<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@mipmap/ic_launcher_background"/>\n    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n</adaptive-icon>\n'
open(f'{ROOT}/android_res/mipmap-anydpi-v26/ic_launcher.xml','w').write(xml); open(f'{ROOT}/android_res/mipmap-anydpi-v26/ic_launcher_round.xml','w').write(xml)
shutil.copy(f'{Z}/hiding_spots.json',f'{ROOT}/assets/hiding_spots.json')

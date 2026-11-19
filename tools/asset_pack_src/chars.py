from core import *
DIRS=['down','up','left','right']
SRC={'hair':'f0c828','hair_s':'b4961e','hair_b':'50321e','skin':'f0c8a0','sh':'c83232','sh_d':'aa2a2a','sh_dd':'8c2323','sh_d2':'962525','stripe':'fa3e3e','pa':'324682','pa_d':'23315b','shoe':'e8e4dc'}
SRCRGB={k:hexc(v)[:3] for k,v in SRC.items()}
def mkpal(skin,hair,shirt,stripe,pants,shoes,hair_b=None):
    hs=shade(hexc(hair),.8); hb=hexc(hair_b) if hair_b else shade(hexc(hair),.6)
    s=hexc(shirt)
    return {'hair':hexc(hair),'hair_s':hs,'hair_b':hb,'skin':hexc(skin),'sh':s,'sh_d':shade(s,.84),'sh_dd':shade(s,.68),'sh_d2':shade(s,.78),
            'stripe':hexc(stripe),'pa':hexc(pants),'pa_d':shade(hexc(pants),.7),'shoe':hexc(shoes)}
PAL={
 'bot5':(mkpal('d49a72','2a1d20','e05a9c','ff9ccc','4b3a6e','e8e4dc'),'glasses'),
 'bot6':(mkpal('f3d2b4','e8e8f0','4b5058','7a8088','2a2d33','e8e4dc',hair_b='b8bccc'),'beanie'),
 'bot7':(mkpal('6e4630','1c1618','a6c43a','d6ee6a','5a4632','f0f0f0'),'afro'),
 'bot8':(mkpal('e0b08a','d8561f','2c3f8a','f4f4f4','6b6b72','c8302e'),'spikes'),
 'bot9':(mkpal('f4d6bc','c9c9cf','7a4a2a','c8a060','3b3f4a','5a3a22'),'backpack'),
}
def recolor(im,pal):
    m={SRCRGB[k]:pal[k][:3] for k in SRC}; o=im.copy(); px=o.load()
    for y in range(o.height):
        for x in range(o.width):
            p=px[x,y]
            if p[3] and p[:3] in m: px[x,y]=m[p[:3]]+(p[3],)
    return o
def masks(im):
    px=im.load(); H=set(); S=set(); T=set()
    hs={SRCRGB['hair'],SRCRGB['hair_s'],SRCRGB['hair_b']}; ts={SRCRGB[k] for k in('sh','sh_d','sh_dd','sh_d2','stripe')}
    for y in range(im.height):
        for x in range(im.width):
            p=px[x,y]
            if p[3]!=255: continue
            if p[:3] in hs: H.add((x,y))
            elif p[:3]==SRCRGB['skin']: S.add((x,y))
            elif p[:3] in ts: T.add((x,y))
    return H,S,T
def paint(im,pts,col,only=None):
    px=im.load(); new=[]
    for x,y in pts:
        if not(0<=x<im.width and 0<=y<im.height): continue
        p=px[x,y]
        if only is None:
            if p[3]==0 or p[:3]==OUTL: px[x,y]=col[:3]+(255,); new.append((x,y))
        elif (x,y) in only: px[x,y]=col[:3]+(255,); new.append((x,y))
    return new
def outline_pass(im,pts):
    px=im.load()
    for x,y in pts:
        for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
            nx,ny=x+dx,y+dy
            if 0<=nx<im.width and 0<=ny<im.height and px[nx,ny][3]==0: px[nx,ny]=OUTL+(255,)
def accessory(im,src,kind,d,pal):
    H,S,T=masks(src)
    if not H: return im
    hx0=min(x for x,y in H); hx1=max(x for x,y in H); hy0=min(y for x,y in H); hy1=max(y for x,y in H)
    cx=(hx0+hx1)//2; o=im.copy(); px=o.load(); new=[]
    if kind=='glasses':
        eyes=[(x,y) for x,y in ((x,y) for y in range(o.height) for x in range(o.width)) if src.getpixel((x,y))[:3]==OUTL and src.getpixel((x,y))[3]==255 and sum((x+dx,y+dy) in S for dx,dy in((1,0),(-1,0),(0,1),(0,-1)))>=2 and hy0<=y<=hy1+6]
        rim=hexc('3a6ea5')
        if d=='down':
            for ex,ey in eyes:
                ring=[(ex+a,ey+b) for a in(-1,0,1) for b in(-1,0,1) if (a,b)!=(0,0)]
                new+=paint(o,ring,rim,only=S)
            if len(eyes)==2:
                (a,y),(b,_)=sorted(eyes); new+=paint(o,[(x,y) for x in range(a+2,b-1)],rim,only=S)
        elif d in('left','right'):
            dx=1 if d=='right' else -1
            for ex,ey in eyes: new+=paint(o,[(ex,ey-1),(ex,ey+1),(ex+dx,ey-1),(ex+dx,ey),(ex+dx,ey+1),(ex-dx,ey)],rim,only=S)
    elif kind=='beanie':
        pts=[(x,hy0-1) for x in range(hx0,hx1+1)]; new+=paint(o,pts,hexc('e8e8f0'))
        new+=paint(o,[(x,hy0-1) for x in(hx0,hx1)],pal['hair_s'])
        if d=='down': new+=paint(o,[(x,hy1+1) for x in range(hx0,hx1+1)],pal['hair_s'],only=S)
        new+=paint(o,[(cx,hy0-2),(cx+1,hy0-2),(cx,hy0-3),(cx+1,hy0-3)],hexc('f6f6ff'))
    elif kind=='afro':
        cur=set(H)
        for _ in range(2):
            add=set()
            for x,y in cur:
                for dx,dy in((0,-1),(-1,0),(1,0)):
                    if y+dy<=hy1: add.add((x+dx,y+dy))
            cur|=add
        new+=paint(o,sorted(cur-H),pal['hair'])
    elif kind=='spikes':
        for x in range(hx0,hx1+1):
            new+=paint(o,[(x,hy0-1)],pal['hair'])
            if (x-hx0)%2==0: new+=paint(o,[(x,hy0-2),(x,hy0-3)],pal['hair']) if (x-hx0)%4==0 else paint(o,[(x,hy0-2)],pal['hair'])
    elif kind=='backpack' and T:
        tx0=min(x for x,y in T); tx1=max(x for x,y in T); ty0=min(y for x,y in T)
        bag=hexc('d9822b'); bagd=hexc('a85f1a')
        if d=='up':
            new+=paint(o,[(x,y) for x in range(tx0+1,tx1) for y in range(ty0,ty0+7)],bag,only=T)
            new+=paint(o,[(x,y) for x in range(tx0+1,tx1) for y in range(ty0+5,ty0+7)],bagd,only=T)
            new+=paint(o,[(x,ty0+2) for x in range(tx0+2,tx1-1)],bagd,only=T)
        elif d=='down':
            new+=paint(o,[(tx0+2,y) for y in range(ty0,ty0+5)]+[(tx1-2,y) for y in range(ty0,ty0+5)],bag,only=T)
            new+=paint(o,[(tx0-1,y) for y in range(ty0+2,ty0+7)]+[(tx1+1,y) for y in range(ty0+2,ty0+7)],bagd)
        else:
            if d=='right': pts=[(x,y) for x in range(tx0-3,tx0) for y in range(ty0+1,ty0+8)]
            else: pts=[(x,y) for x in range(tx1+1,tx1+4) for y in range(ty0+1,ty0+8)]
            new+=paint(o,pts,bag); new+=paint(o,[p for p in pts if p[1]>=ty0+6],bagd)
    outline_pass(o,new); return o
def make_bot(P,name,pal,kind):
    out={}
    for (n,i),im in P.items():
        if not n.startswith('player_') or i<0: continue
        rest=n[len('player_'):]; nn=f'{name}_{rest}'
        r=recolor(im,pal)
        d=next((x for x in DIRS if rest.endswith('_'+x)),None)
        if d and not rest.startswith('corpse'): r=accessory(r,im,kind,d,pal)
        out[(nn,i)]=r
    return out
# ---- animasi baru survivor ----
def spark(im,pts,col=(255,236,120)):
    px=im.load()
    for x,y in pts:
        if 0<=x<im.width and 0<=y<im.height: px[x,y]=col+(255,)
def mini_key(im,x,y):
    g=(240,200,60); d=(168,128,30); px=im.load()
    for dx,dy,c in [(0,0,g),(1,0,g),(0,1,g),(1,1,d),(2,1,g),(3,1,g),(4,1,g),(5,1,g),(4,2,d),(5,2,d)]:
        if 0<=x+dx<im.width and 0<=y+dy<im.height: px[x+dx,y+dy]=c+(255,)
def anims(F,who):
    """F: dict name->frames list (karakter ini). return dict anim->frames"""
    A={}
    idd=F[f'{who}_idle_down'][0]; idu=F[f'{who}_idle_up'][0]
    bb=split_shadow(idd)[0].getbbox(); cx=(bb[0]+bb[2])//2; top=bb[1]
    # hidden
    h=[]
    for blink in(0,1):
        im=Image.new('RGBA',(32,32),(0,0,0,0)); px=im.load()
        for ex in(12,18):
            if not blink:
                for dx,dy in((0,0),(1,0),(0,1),(1,1)): px[ex+dx,19+dy]=(245,245,245,255)
                px[ex+1,19]=OUTL+(255,); px[ex+1,20]=OUTL+(255,)
            else:
                for dx in(0,1): px[ex+dx,20]=(90,90,90,255)
        h.append(im)
    A['hidden']=h
    # key pickup
    k=[]
    b=idd
    k.append(b.copy())
    k.append(squash(b,.88)); f=squash(b,.8); spark(f,[(cx-2,27),(cx+3,26)]); k.append(f)
    f=squash(b,.9); mini_key(f,cx+5,top+7); k.append(f)
    f=b.copy(); mini_key(f,cx-3,top-5); spark(f,[(cx-5,top-6),(cx+4,top-4),(cx,top-8)]); k.append(f)
    f=shift(b,0,0); spark(f,[(cx-4,top-3),(cx+4,top-6),(cx,top-7),(cx-1,top-4)],(255,255,255)); k.append(f)
    A['key_pickup']=k
    # exit interaction (menghadap atas)
    e=[idu.copy()]
    f=shift(idu,0,-1); mini_key(f,cx+5,top+7); e.append(f)
    f=shift(idu,0,-1); mini_key(f,cx-3,top-4); e.append(f)
    f=shift(idu,0,-1); mini_key(f,cx-3,top-5); spark(f,[(cx-6,top-6),(cx+4,top-6),(cx,top-8),(cx+2,top-3)],(150,230,255)); e.append(f)
    f=shift(idu,0,-1); spark(f,[(cx-4,top-4),(cx+4,top-5)],(120,255,160)); e.append(f)
    e.append(idu.copy()); A['exit_interact']=e
    # escape
    sp=F[f'{who}_sprint_up']; es=[]
    for i in range(8):
        p=i/7; im=sp[i%4]; s=1-.55*p
        bd,sh=split_shadow(im); bb2=bd.getbbox(); crop=bd.crop(bb2)
        w=max(1,round(crop.width*s)); hh=max(1,round(crop.height*s)); c2=crop.resize((w,hh),Image.NEAREST)
        c2=set_alpha(c2,max(0.12,1-p**1.6))
        o=Image.new('RGBA',(32,32),(0,0,0,0)); xx=int((bb2[0]+bb2[2])/2-w/2); yy=bb2[3]-hh-int(7*p)
        o.alpha_composite(c2,(max(0,xx),max(0,yy))); 
        sh2=set_alpha(sh,1-p); o=Image.alpha_composite(sh2,o); es.append(o)
    A['escape']=es
    # hit
    ht=[]
    f=tint(idd,(255,255,255),.85); f=shift(f,0,-1) if False else f; spark(f,[(cx-6,top+1),(cx+6,top),(cx+7,top+3)],(200,20,30)); ht.append(f)
    f=tint(squash(idd,.92),(220,30,30),.45); f=shift(f,-1,0); spark(f,[(cx+7,top+2),(cx+8,top+5)],(170,10,20)); ht.append(f)
    f=tint(idd,(220,30,30),.25); f=shift(f,1,0); ht.append(f)
    ht.append(idd.copy()); A['hit']=ht
    # spectator (hantu)
    sp2=[]
    for i in range(6):
        t=i/5; g=tint(idd,(160,200,255),.35+.4*t,outline_amt=.5); g=set_alpha(g,max(.12,.85-.75*t))
        bd,sh=split_shadow(g); sh=set_alpha(sh,0); dx=int(round(math.sin(i*1.3)*1.5)); g=shift(bd,dx,-2*i) if (dx>=0) else shift(bd,dx,-2*i)
        sp2.append(g)
    A['spectator']=sp2
    return A
# ---- Jason ----
def jason_new(J):
    A={}
    for d in DIRS:
        w=J[f'jason_walk_{d}']; at=J[f'jason_attack_{d}']
        idle=[w[0].copy()]
        bd,sh=split_shadow(w[0]); bb=bd.getbbox(); cut=bb[1]+ (bb[3]-bb[1])//2
        SW,SH=bd.size; top=bd.crop((0,0,SW,cut)); bot=bd.crop((0,cut,SW,SH)); o=Image.new('RGBA',(SW,SH),(0,0,0,0)); o.alpha_composite(bot,(0,cut)); o.alpha_composite(top,(0,1)); o=Image.alpha_composite(sh,o); idle.append(o)
        A[f'jason_idle_{d}']=idle
        bk=[at[0],at[1],at[2],at[3]]; imp=squash(at[3],.94); imp=shift(imp,0,1) if False else imp; bk.append(imp); bk.append(at[4].copy()); 
        bk.append(squash(at[4],.97)); A[f'jason_break_{d}']=bk
        dx={'left':2,'right':-2,'up':0,'down':0}[d]; dy={'up':2,'down':-1,'left':0,'right':0}[d]
        f0=tint(w[0],(255,255,255),.85); f1=tint(squash(w[0],.93),(220,30,30),.4); f2=tint(w[0],(220,30,30),.2)
        def mv(im,a,b):
            bd,sh=split_shadow(im); r=Image.new('RGBA',im.size,(0,0,0,0)); r.paste(bd,(a,b),bd); return Image.alpha_composite(sh,r)
        A[f'jason_hit_{d}']=[mv(f0,0,0),mv(f1,dx,dy if dy>0 else 0),mv(f2,dx//2,0)]
    d0=J['jason_attack_down']; vic=[]
    for i,(b,dy) in enumerate([(d0[0],0),(d0[1],-2),(d0[0],-3),(d0[1],-1),(d0[0],0),(d0[1],-2)]):
        bd,sh=split_shadow(b); r=Image.new('RGBA',b.size,(0,0,0,0)); r.paste(bd,(0,dy),bd); vic.append(Image.alpha_composite(sh,r))
    A['jason_victory']=vic
    return A

JPAD=8
def pad_jason(im,pad=JPAD):
    """Perluas kanvas Jason (48->64) lalu tutup outline pada bentuk yang terpotong di tepi lama."""
    o=Image.new('RGBA',(im.width+2*pad,im.height+2*pad),(0,0,0,0)); o.alpha_composite(im,(pad,pad))
    px=o.load(); pts=[(x,y) for y in range(o.height) for x in range(o.width) if px[x,y][3]==255 and px[x,y][:3]!=OUTL]
    outline_pass(o,pts); return o

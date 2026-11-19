from core import *
from pfont import *
from chars import outline_pass
from items import Cv, key_assets, H
def ring_btn(R,pressed,icon=None):
    base=R[('ui_btn_hide_pressed',-1)] if pressed else R[('ui_btn_hide',-1)]
    im=base.copy(); px=im.load()
    # hapus ikon lemari: ganti piksel tengah dengan warna isi
    fill=px[24,6]; 
    for y in range(10,38):
        for x in range(10,38):
            if (x-24)**2+(y-24)**2<=14**2: px[x,y]=fill
    return im
def hand_icon():
    c=Cv(16,16); s='e8d8b8'
    c.r(4,7,12,14,s); c.r(4,3,6,8,s); c.r(7,2,9,8,s); c.r(10,3,12,8,s); c.r(2,8,5,11,s)
    px=c.im.load(); outline_pass(c.im,[(x,y) for y in range(16) for x in range(16) if px[x,y][3]>0]); return c.im
def timer_icon():
    c=Cv(16,16); c.e(2,3,14,15,'e8d8b8'); c.e(4,5,12,13,'2a2630'); c.r(7,7,9,11,'e8d8b8'); c.r(7,10,11,11,'e8d8b8'); c.r(6,1,10,3,'a89878'); c.r(7,0,9,1,'a89878')
    px=c.im.load(); outline_pass(c.im,[(x,y) for y in range(16) for x in range(16) if px[x,y][3]>0]); return c.im
def escaped_icon():
    c=Cv(16,16); c.r(2,2,9,14,'8a5a32'); c.r(3,3,8,14,'2a1a10'); c.r(7,8,8,9,'e0c040')
    c.r(8,7,14,9,'60e080'); c.d.polygon([(11,4),(15,8),(11,12)],fill=H('60e080'))
    px=c.im.load(); outline_pass(c.im,[(x,y) for y in range(16) for x in range(16) if px[x,y][3]>0]); return c.im
def slot(empty,piece=None):
    im=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(im); d.rectangle([0,0,15,15],fill=H('2a2630'),outline=H('a89878'))
    if piece is not None:
        im.alpha_composite(piece)
    else: d.rectangle([5,6,10,9],outline=H('4a4450'))
    return im
def pill(w,h,fill,edge,label,scale,col='f0e8d0',pressed=False):
    im=Image.new('RGBA',(w,h),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle([0,0,w-1,h-1],fill=H(edge)); d.rectangle([2,2,w-3,h-3],fill=H(fill)); d.rectangle([3,3,w-4,h-4],outline=shade(H(fill),1.35))
    tw=text_w(label,scale); th=7*scale
    draw_text(im,label,(w-tw)//2,(h-th)//2+(1 if pressed else 0),H(col),scale,shadow=H('000000') if scale==1 else None)
    return im
def minicam():
    w,h=96,64; im=Image.new('RGBA',(w,h),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle([0,0,w-1,h-1],outline=H('a89878')); d.rectangle([1,1,w-2,h-2],outline=H('2a2630')); d.rectangle([2,2,w-3,h-3],outline=H('a89878'))
    d.rectangle([3,3,w-4,11],fill=(20,16,24,230)); draw_text(im,'KAMERA JASON',14,4,H('f0e8d0'))
    for (x,y) in((3,12),(w-9,12),(3,h-9),(w-9,h-9)):
        d.line([x,y,x+5,y],fill=H('d83030')); d.line([x,y,x,y+5],fill=H('d83030')) if y<30 else None
    for x,y,dx,dy in((3,12,1,1),(w-4,12,-1,1),(3,h-4,1,-1),(w-4,h-4,-1,-1)):
        for k in range(6): im.putpixel((x+dx*k,y),H('d83030')); im.putpixel((x,y+dy*k),H('d83030'))
    im.putpixel((8,7),(0,0,0,0)); return im
def rec_dot(on):
    im=Image.new('RGBA',(8,8),(0,0,0,0)); d=ImageDraw.Draw(im)
    if on: d.ellipse([1,1,6,6],fill=H('e02020')); im.putpixel((2,2),H('ff9090'))
    else: d.ellipse([1,1,6,6],outline=H('702020'))
    return im
def arrow_btn(right,pressed):
    im=Image.new('RGBA',(24,24),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.ellipse([0,0,23,23],fill=H('a89878')); d.ellipse([2,2,21,21],fill=H('6a6070' if pressed else '2a2630'))
    pts=[(9,7),(16,12),(9,17)] if right else [(15,7),(8,12),(15,17)]; d.polygon(pts,fill=H('f0e8d0')); return im
def ui_new(R):
    N={}
    for pr in(0,1): N[('ui_btn_interact'+('_pressed' if pr else ''),-1)]=(lambda b:(b.alpha_composite(sc(hand_icon(),2),(8,8)),b)[1])(ring_btn(R,pr))
    N[('ui_icon_timer',-1)]=timer_icon(); N[('ui_icon_survivor_escaped',-1)]=escaped_icon(); N[('ui_icon_interact',-1)]=hand_icon()
    P,fo=key_assets()
    kc=Image.new('RGBA',(16,16),(0,0,0,0)); s=fo.resize((16,8),Image.NEAREST); kc.alpha_composite(s,(0,4)); N[('ui_icon_key_complete',-1)]=kc
    N[('ui_key_slot_empty',-1)]=slot(True)
    for i,p in enumerate(P): N[(f'ui_key_slot_{i+1}',-1)]=slot(False,p)
    N[('ui_minicam_frame',-1)]=minicam(); N[('ui_minicam_rec',0)]=rec_dot(True); N[('ui_minicam_rec',1)]=rec_dot(False)
    N[('ui_spectator_badge',-1)]=pill(88,16,'3a2a4a','a89878','PENONTON',1)
    for r,nm in((0,'prev'),(1,'next')):
        for pr in(0,1): N[(f'ui_btn_spec_{nm}'+('_pressed' if pr else ''),-1)]=arrow_btn(bool(r),pr)
    N[('ui_status_alive',-1)]=pill(80,12,'1e5a34','60b078','HIDUP',1)
    N[('ui_status_escaped',-1)]=pill(80,12,'1e4a6a','60a0c8','KABUR',1)
    N[('ui_status_eliminated',-1)]=pill(80,12,'5a1418','c86060','TERELIMINASI',1)
    for nm,lab,sc_ in(('play','MAIN',2),('settings','PENGATURAN',1),('exit_menu','KELUAR',2),('pause','JEDA',2)):
        for pr in(0,1): N[(f'ui_btn_{nm}'+('_pressed' if pr else ''),-1)]=pill(96,32,'4a4450' if pr else '2a2630','d8ccb8',lab,sc_,pressed=bool(pr))
    N[('ui_result_escaped',-1)]=pill(128,32,'1e4a6a','60a0c8','SURVIVOR KABUR',1)
    N[('ui_result_all_eliminated',-1)]=pill(160,32,'5a1418','c86060','SEMUA TERELIMINASI',1)
    return N
# ---------- BRANDING ----------
RED=H('d02020'); REDL=H('f05050'); REDD=H('7a0e14'); CREAM=H('efe6d2'); DARK=H('0f060a')
def wordmark(text,scale,col,hl,sh,gap=1,bold=False):
    m,w,h=text_mask(text,gap)
    if bold: m=m|{(a+1,b) for a,b in m}; w+=1
    W=w*scale+8; Hh=h*scale+8; im=Image.new('RGBA',(W,Hh),(0,0,0,0)); px=im.load()
    for (a,b) in m:
        for dy in range(scale):
            for dx in range(scale):
                X=4+a*scale+dx; Y=4+b*scale+dy
                c=col
                if dy<max(1,scale//4): c=hl
                elif dy>=scale-max(1,scale//4): c=sh
                px[X,Y]=c
    return im,m
def outline_big(im,t,col):
    px=im.load(); pts=[(x,y) for y in range(im.height) for x in range(im.width) if px[x,y][3]>0]
    o=Image.new('RGBA',im.size,(0,0,0,0)); op=o.load()
    for x,y in pts:
        for dx in range(-t,t+1):
            for dy in range(-t,t+1):
                X,Y=x+dx,y+dy
                if 0<=X<im.width and 0<=Y<im.height and px[X,Y][3]==0: op[X,Y]=col
    o.alpha_composite(im); return o
def eyes(sz=1):
    im=Image.new('RGBA',(40*sz,14*sz),(0,0,0,0)); d=ImageDraw.Draw(im)
    for ox in(2,24):
        d.polygon([(ox*sz,9*sz),((ox+6)*sz,3*sz),((ox+14)*sz,4*sz),((ox+12)*sz,10*sz),((ox+4)*sz,11*sz)],fill=CREAM)
        d.rectangle([(ox+6)*sz,5*sz,(ox+9)*sz,9*sz],fill=RED)
    return im
def logo(w_total=None):
    j,m=wordmark('JASON',12,RED,REDL,REDD,gap=2,bold=True); j=outline_big(j,5,DARK)
    s,_=wordmark('HIDE & SEEK',5,CREAM,H('ffffff'),H('a89a80'),gap=2); s=outline_big(s,3,DARK)
    W=max(j.width,s.width)+20; Hh=j.height+s.height+80; im=Image.new('RGBA',(W,Hh),(0,0,0,0))
    ey=eyes(2); im.alpha_composite(ey,(W//2-ey.width//2+60,6))
    jx=(W-j.width)//2; im.alpha_composite(j,(jx,30)); im.alpha_composite(s,((W-s.width)//2,30+j.height+22))
    # tetesan darah
    rnd=random.Random(7); d=ImageDraw.Draw(im); px=j.load()
    base=30+j.height-18
    for a,b in sorted(m):
        pass
    for xx in(jx+34,jx+120,jx+200,jx+292,jx+385):
        L=rnd.randint(6,16); d.rectangle([xx,base-8,xx+3,base+L],fill=RED); d.rectangle([xx,base-8,xx,base+L],fill=REDL); d.ellipse([xx-2,base+L-2,xx+5,base+L+5],fill=RED)
    return im
def jason_head():
    C=load_atlas('characters'); f=C[('jason_walk_down',0)]; im=f.crop((17,14,31,28)); bb=im.getbbox(); return im.crop(bb)
def bg_icon(sz,round_=False):
    im=Image.new('RGBA',(sz,sz),DARK); px=im.load(); cx=cy=sz/2
    for y in range(sz):
        for x in range(sz):
            r=math.hypot(x-cx,y-cy)/(sz*.7); t=max(0,1-r)**1.6
            c=lerp(DARK[:3],(120,14,22),t); n=((x//8+y//8)%2)*2
            px[x,y]=tuple(min(255,v+n) for v in c)+(255,)
    return im
def icon_fg(sz,scale_frac=.58):
    head=jason_head(); k=max(1,int(sz*scale_frac/head.width)); h2=sc(head,k); h2=outline_big(h2,max(1,k//2),DARK)
    im=Image.new('RGBA',(sz,sz),(0,0,0,0)); im.alpha_composite(h2,((sz-h2.width)//2,(sz-h2.height)//2-sz//20))
    d=ImageDraw.Draw(im); x0=(sz-h2.width)//2
    for dx in(.38,.62):
        xx=int(sz*dx); d.rectangle([xx,sz//2+sz//6,xx+k,sz//2+sz//6+k*3],fill=RED)
    return im
def app_icon(sz=512):
    b=bg_icon(sz); b.alpha_composite(icon_fg(sz,.62)); return b

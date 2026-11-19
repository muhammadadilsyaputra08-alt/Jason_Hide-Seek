from core import *
from chars import outline_pass
H=hexc
class Cv:
    def __init__(s,w=32,h=32): s.im=Image.new('RGBA',(w,h),(0,0,0,0)); s.d=ImageDraw.Draw(s.im)
    def r(s,x0,y0,x1,y1,c):
        if x1<=x0 or y1<=y0: return
        s.d.rectangle([x0,y0,x1-1,y1-1],fill=H(c) if isinstance(c,str) else c)
    def p(s,x,y,c): s.im.putpixel((x,y),H(c) if isinstance(c,str) else c)
    def e(s,x0,y0,x1,y1,c): s.d.ellipse([x0,y0,x1-1,y1-1],fill=H(c))
    def l(s,x0,y0,x1,y1,c): s.d.line([x0,y0,x1,y1],fill=H(c))
    def done(s,shadow=True):
        pts=[(x,y) for y in range(s.im.height) for x in range(s.im.width) if s.im.getpixel((x,y))[3]>0]
        outline_pass(s.im,pts)
        if shadow:
            px=s.im.load(); w,h=s.im.size; by=max(y for x,y in pts)
            for x in range(1,w-1):
                if any(s.im.getpixel((x,y))[3]>0 for y in range(by-3,by+1)):
                    for y in(by+1,by+2):
                        if y<h and px[x,y][3]==0: px[x,y]=(0,0,0,70)
        return s.im
W1,W2,W3='a8733f','8a5a32','5e3a20'
def table():
    c=Cv(); c.r(4,9,28,18,W1); c.r(4,18,28,23,W2); c.r(4,22,28,23,W3); c.r(5,23,8,28,W3); c.r(24,23,27,28,W3); c.r(8,11,14,13,'c9985a'); c.r(19,12,24,13,'c9985a'); return c.done()
def chair():
    c=Cv(); c.r(10,5,22,15,W3); [c.r(11+i*4,6,13+i*4,14,W2) for i in range(3)]; c.r(9,15,23,21,W2); c.r(9,15,23,17,W1); c.r(10,21,12,28,W3); c.r(20,21,22,28,W3); return c.done()
def bookshelf():
    c=Cv(); c.r(3,2,29,29,W3); c.r(5,4,27,27,'2c1a10')
    cols=['b03030','3060b0','d0a030','309060','8040a0','c06020']
    for row,y in enumerate((5,13,21)):
        c.r(5,y+6,27,y+8,W2); x=6
        for i in range(7):
            w=2+(i*row+i)%2; h=5-(i+row)%2; c.r(x,y+6-h,x+w,y+6,cols[(i*2+row)%6]); x+=w+1
    return c.done()
def painting():
    c=Cv(); c.r(5,5,27,23,'c9a040'); c.r(7,7,25,21,'1c2c26'); c.e(16,9,22,15,'d8d4a0'); c.r(7,16,25,21,'102018'); c.r(9,14,13,21,'0c1812'); c.r(18,13,23,21,'0c1812'); return c.done(False)
def floor_lamp():
    c=Cv(); c.r(11,26,21,29,W3); c.r(15,10,17,27,'3a3a40'); c.r(9,3,23,11,'e8c860'); c.r(9,3,23,5,'f8e890'); c.r(11,11,21,13,'a88a30'); return c.done()
def carpet():
    c=Cv(64,32); c.r(2,3,62,29,'7a1620'); c.r(4,5,60,27,'a02030'); c.r(6,7,58,25,'7a1620'); c.r(8,9,56,23,'8e1a28')
    for i in range(5): x=14+i*9; c.r(x,13,x+4,19,'d8b050'); c.r(x+1,14,x+3,18,'8e1a28')
    return c.done(False)
def wall_clock():
    c=Cv(); c.e(6,4,26,24,W3); c.e(8,6,24,22,'ece6d0'); c.r(15,8,17,15,'201810'); c.r(15,14,21,16,'201810'); c.p(16,15,'c02020')
    for a,b in((16,7),(16,22),(8,14),(23,14)): c.p(a,b,'201810')
    return c.done(False)
# map2
def vehicle():
    c=Cv(64,32); c.r(5,5,59,27,'5a6e4a'); c.r(5,5,59,12,'6f8660'); c.r(10,7,28,14,'9ad0e0'); c.r(31,7,54,14,'7ab0c0'); c.r(5,12,59,14,'3e4e34')
    c.r(3,15,61,24,'4a5c3c'); c.r(3,15,61,17,'5e744e'); c.r(26,16,30,24,'3e4e34'); c.r(8,24,14,29,'1e1e22'); c.r(48,24,54,29,'1e1e22'); c.r(8,24,14,26,'3a3a42')
    c.r(5,19,9,22,'e8c040'); c.r(55,19,59,22,'d02828'); c.r(20,20,24,21,'a02828'); return c.done()
def tires():
    c=Cv(); 
    for (x,y) in((4,15),(14,15),(9,8)):
        c.e(x,y,x+13,y+10,'22222a'); c.e(x+3,y+3,x+10,y+7,'5a5a64'); c.e(x+5,y+4,x+8,y+6,'22222a')
    return c.done()
def workbench_tools():
    c=Cv(); c.r(3,9,29,15,'7a5230'); c.r(3,9,29,11,'a07040'); c.r(3,15,29,19,'5e3a20'); c.r(4,19,7,28,'5e3a20'); c.r(25,19,28,28,'5e3a20')
    c.r(6,6,10,10,'b83030'); c.r(8,5,9,7,'6b7480'); c.r(14,7,22,9,'98a2b0'); c.r(23,6,26,9,'e0c040'); c.r(11,20,21,26,'3e2a18'); return c.done()
def tool_rack():
    c=Cv(); c.r(3,3,29,29,'414a55'); c.r(5,5,27,27,'5a6470')
    for i in range(4): c.r(7+i*5,7,9+i*5,18,['98a2b0','b83030','e0c040','98a2b0'][i]); c.r(6+i*5,17,10+i*5,20,'30363e')
    c.r(6,22,26,24,'30363e'); c.r(8,24,12,27,'98a2b0'); c.r(16,24,24,26,'b83030'); return c.done()
def oil_stack():
    c=Cv(); 
    for x,y in((3,14),(17,14),(10,3)):
        c.r(x,y,x+12,y+13,'3a3e46'); c.r(x,y+3,x+12,y+4,'565c68'); c.r(x,y+9,x+12,y+10,'565c68'); c.r(x+2,y+1,x+4,y+12,'6a7280')
    c.r(11,10,14,12,'e0a020'); return c.done()
def spare_parts():
    c=Cv(); c.r(4,12,28,28,'6a5030'); c.r(4,12,28,15,'8a6a40'); c.r(6,16,26,26,'3a2a18')
    c.e(8,17,16,25,'98a2b0'); c.e(10,19,14,23,'3a2a18'); c.e(17,18,25,25,'6a7280'); c.e(19,20,23,23,'3a2a18'); c.r(12,6,16,12,'b83030'); c.r(16,8,22,10,'98a2b0'); return c.done()
# map3
def cart():
    c=Cv(); c.r(4,8,28,19,'7a5230'); c.r(4,8,28,10,'a07040'); c.r(6,10,26,17,'5e3a20'); c.r(4,19,28,21,'3e2a18'); c.e(6,19,16,29,'3e2a18'); c.e(8,21,14,27,'8a6a40'); c.e(18,19,28,29,'3e2a18'); c.e(20,21,26,27,'8a6a40'); c.r(2,12,5,14,'3e2a18'); return c.done()
def farm_tools():
    c=Cv(); c.r(9,4,11,28,W2); c.r(6,3,14,6,'98a2b0'); c.r(6,3,7,9,'98a2b0'); c.r(9,3,10,9,'98a2b0'); c.r(13,3,14,9,'98a2b0')
    c.r(21,9,23,28,W2); c.r(19,2,25,10,'98a2b0'); c.r(20,3,24,6,'c0cad8'); c.r(15,26,17,29,W3); return c.done()
def sacks():
    c=Cv(); 
    for x,y,w in((3,14,12),(16,15,12),(9,6,12)):
        c.e(x,y,x+w,y+13,'b09060'); c.r(x+3,y,x+w-3,y+3,'c8a870'); c.r(x+4,y+2,x+w-4,y+4,'8a6a40'); c.r(x+3,y+7,x+w-3,y+8,'96774a')
    return c.done()
def hay_bale():
    c=Cv(); c.r(4,9,28,27,'d8b040'); c.r(4,9,28,13,'ecd070'); 
    for x in(11,19): c.r(x,9,x+2,27,'8a6a20')
    for i in range(6): c.l(6+i*4,15+(i%2)*4,9+i*4,17+(i%2)*4,'a88820')
    return c.done()
def bucket():
    c=Cv(); c.r(9,10,23,26,'6b7480'); c.r(9,10,23,13,'98a2b0'); c.r(11,12,21,14,'2c3a44'); c.r(10,18,22,19,'414a55'); c.l(9,10,16,3,'414a55'); c.l(23,10,16,3,'414a55'); return c.done()
def fence_h():
    c=Cv(); c.r(0,12,32,16,W2); c.r(0,12,32,13,W1); c.r(0,21,32,25,W2); c.r(0,21,32,22,W1)
    for x in(2,14,26): c.r(x,6,x+4,29,W3); c.r(x,6,x+4,8,W2)
    return c.done()
def fence_v():
    c=Cv(); c.r(13,0,19,32,W2); c.r(13,0,15,32,W1); 
    for y in(2,14,26): c.r(9,y,23,y+4,W3); c.r(9,y,23,y+1,W2)
    return c.done(False)
# map4
def cargo_rack():
    c=Cv(); c.r(3,2,6,30,'e07a20'); c.r(26,2,29,30,'e07a20'); 
    for y in(10,20,28): c.r(3,y,29,y+2,'2c5ca0')
    c.r(8,4,14,10,'b89060'); c.r(16,5,24,10,'a07a50'); c.r(8,14,18,20,'b89060'); c.r(19,13,25,20,'a07a50'); return c.done()
def pallet_stack():
    c=Cv(); c.r(4,20,28,26,'a07040'); c.r(4,20,28,22,'c09050'); 
    for x in(5,14,23): c.r(x,22,x+4,26,'6a4a28')
    c.r(6,8,15,20,'b89060'); c.r(16,10,26,20,'a07a50'); c.r(6,8,15,10,'d8b880'); c.r(8,13,12,15,'f0f0e8'); return c.done()
def container():
    c=Cv(64,32); c.r(3,4,61,28,'2f6f8a'); c.r(3,4,61,8,'4a90ac')
    for x in range(8,60,6): c.r(x,8,x+2,28,'245870')
    c.r(3,26,61,28,'1c4458'); c.r(52,12,58,20,'e8e8e0'); c.r(6,12,12,16,'e8e8e0'); return c.done()
def cardboard_stack():
    c=Cv(); c.r(3,15,17,28,'b89060'); c.r(3,15,17,18,'d8b880'); c.r(9,15,11,28,'e8d0a0'); c.r(16,19,30,28,'a07a50'); c.r(16,19,30,22,'c9a070'); c.r(8,5,22,15,'b89060'); c.r(8,5,22,8,'d8b880'); c.r(14,5,16,15,'e8d0a0'); return c.done()
def ind_drum():
    c=Cv(); c.r(7,6,25,28,'d8a820'); c.r(7,6,25,10,'f0c840'); c.r(7,14,25,16,'a88418'); c.r(7,22,25,24,'a88418'); c.r(10,8,13,27,'f0d870')
    c.r(13,16,19,21,'201810'); c.r(15,17,17,20,'e8d040'); return c.done()
def trolley():
    c=Cv(); c.r(5,10,27,20,'6b7480'); c.r(5,10,27,12,'98a2b0'); c.r(25,2,27,12,'414a55'); c.r(21,2,29,4,'414a55'); c.r(7,20,25,22,'414a55')
    for x in(7,21): c.e(x,22,x+5,28,'22222a')
    c.r(9,6,16,10,'b89060'); return c.done()
# map5
def pipes():
    c=Cv(); c.r(0,8,32,15,'6b5a3a'); c.r(0,8,32,10,'9a8458'); c.r(0,12,32,15,'4a3c24'); c.r(0,19,32,25,'565c68'); c.r(0,19,32,21,'8a92a0'); c.r(0,23,32,25,'3a3e46')
    for x in(6,22): c.r(x,6,x+3,28,'30363e')
    return c.done(False)
def valve():
    c=Cv(); c.r(14,16,18,30,'565c68'); c.r(10,12,22,17,'6b7480'); c.e(7,2,25,20,'b02828'); c.d.ellipse([10,5,21,16],fill=(0,0,0,0)); c.r(15,5,17,17,'b02828'); c.r(9,10,23,12,'b02828'); c.r(15,10,17,12,'e0e0e0'); return c.done()
def iron_bed():
    c=Cv(); c.r(4,6,28,10,'414a55'); c.r(4,6,6,28,'414a55'); c.r(26,6,28,28,'414a55'); c.r(6,12,26,24,'7a8a96'); c.r(7,13,25,17,'c8ccd0'); c.r(7,18,25,24,'5a6a76'); c.r(6,24,26,27,'30363e')
    for x in(9,13,17,21): c.r(x,6,x+1,12,'6b7480')
    return c.done()
def storage_rack():
    c=Cv(); c.r(3,3,6,30,'414a55'); c.r(26,3,29,30,'414a55')
    for y in(9,18,27): c.r(3,y,29,y+2,'6b7480')
    c.r(8,3,14,9,'7a5a38'); c.r(18,5,24,9,'5a6a76'); c.r(8,12,16,18,'3a6a56'); c.r(19,13,25,18,'7a5a38'); c.r(9,21,15,27,'5a6a76'); return c.done()
def cables():
    c=Cv(); 
    for i,col in enumerate(('b02828','e0c040','2c5ca0')):
        for x in range(0,32): c.p(x,10+i*4+int(2*math.sin(x/4+i)),col)
        for x in range(0,32): c.p(x,11+i*4+int(2*math.sin(x/4+i)),shade(H(col),.7))
    c.r(2,6,6,26,'30363e'); c.r(26,6,30,26,'30363e'); return c.done(False)
def hanging_lamp():
    c=Cv(); c.r(15,0,17,8,'30363e'); c.r(9,8,23,14,'6b7480'); c.r(9,8,23,10,'98a2b0'); c.r(12,14,20,17,'f8e890'); c.r(14,16,18,18,'fffbd0'); return c.done(False)
# meta: id -> (fn, map, category, footprint(w,h tiles), note)
ITEMS=[
('table',table,1,'collision',(1,1)),('chair',chair,1,'decorative',(1,1)),('bookshelf',bookshelf,1,'collision',(1,1)),
('painting',painting,1,'decorative_wall',(1,1)),('floor_lamp',floor_lamp,1,'collision',(1,1)),('carpet',carpet,1,'decorative_floor',(2,1)),('wall_clock',wall_clock,1,'decorative_wall',(1,1)),
('vehicle',vehicle,2,'collision',(2,1)),('tires',tires,2,'collision',(1,1)),('workbench_tools',workbench_tools,2,'collision',(1,1)),('tool_rack',tool_rack,2,'collision',(1,1)),('oil_drum_stack',oil_stack,2,'collision',(1,1)),('spare_parts',spare_parts,2,'collision',(1,1)),
('cart',cart,3,'collision',(1,1)),('farm_tools',farm_tools,3,'decorative',(1,1)),('sacks',sacks,3,'collision',(1,1)),('hay_bale',hay_bale,3,'collision',(1,1)),('bucket',bucket,3,'decorative',(1,1)),('fence_h',fence_h,3,'collision',(1,1)),('fence_v',fence_v,3,'collision',(1,1)),
('cargo_rack',cargo_rack,4,'collision',(1,1)),('pallet_stack',pallet_stack,4,'collision',(1,1)),('container',container,4,'collision',(2,1)),('cardboard_stack',cardboard_stack,4,'collision',(1,1)),('industrial_drum',ind_drum,4,'collision',(1,1)),('trolley',trolley,4,'decorative',(1,1)),
('pipes',pipes,5,'decorative_wall',(1,1)),('valve',valve,5,'decorative_wall',(1,1)),('iron_bed',iron_bed,5,'collision',(1,1)),('storage_rack',storage_rack,5,'collision',(1,1)),('cables',cables,5,'decorative_floor',(1,1)),('hanging_lamp',hanging_lamp,5,'decorative_wall',(1,1)),
]
# ---- kunci ----
def key_full():
    c=Cv(32,16); G1,G2,G3='f0c840','b8901c','fff0a0'
    c.e(1,2,13,14,G2); c.e(2,3,12,13,G1); c.d.ellipse([5,6,8,9],fill=(0,0,0,0)); c.r(3,4,5,6,G3)
    c.r(12,6,28,10,G1); c.r(12,6,28,7,G3); c.r(12,9,28,10,G2); c.r(22,10,25,14,G1); c.r(22,13,25,14,G2); c.r(27,10,30,13,G1); c.r(27,12,30,13,G2); c.r(28,6,31,9,G2)
    return c.im
CUTS=[11,21]
def zig(x,y): return x+(1 if y%2 else 0)
def key_pieces():
    full=key_full(); outl=lambda im:(outline_pass(im,[(x,y) for y in range(im.height) for x in range(im.width) if im.getpixel((x,y))[3]>0]),im)[1]
    P=[]
    for i,(a,b) in enumerate([(0,CUTS[0]),(CUTS[0],CUTS[1]),(CUTS[1],32)]):
        cvs=Image.new('RGBA',(32,16),(0,0,0,0))
        for y in range(16):
            for x in range(32):
                lo=zig(a,y) if i>0 else -1; hi=zig(b,y) if i<2 else 99
                if lo<=x<hi and full.getpixel((x,y))[3]>0: cvs.putpixel((x,y),full.getpixel((x,y)))
        bb=cvs.getbbox(); cr=cvs.crop(bb); o=Image.new('RGBA',(16,16),(0,0,0,0)); o.alpha_composite(cr,((16-cr.width)//2,(16-cr.height)//2)); P.append(outl(o))
    return P,outl(full.copy()) if False else None
def key_assets():
    P,_=key_pieces(); full=key_full(); fo=Image.new('RGBA',(34,18),(0,0,0,0)); fo.alpha_composite(full,(1,1))
    outline_pass(fo,[(x,y) for y in range(18) for x in range(34) if fo.getpixel((x,y))[3]>0])
    return P,fo
def star(im,cx,cy,r,col):
    px=im.load()
    for d in range(-r,r+1):
        for X,Y in((cx+d,cy),(cx,cy+d)):
            if 0<=X<im.width and 0<=Y<im.height: px[X,Y]=col+(255,)
def fx_pickup():
    F=[]
    for i in range(6):
        im=Image.new('RGBA',(32,32),(0,0,0,0)); r=2+i*2; a=max(60,255-i*40)
        d=ImageDraw.Draw(im)
        d.ellipse([16-r,16-r,16+r,16+r],outline=(255,236,120,a))
        for k in range(8):
            t=k*math.pi/4+i*.2; x=int(16+math.cos(t)*(r+2)); y=int(16+math.sin(t)*(r+2))
            if 0<=x<32 and 0<=y<32: im.putpixel((x,y),(255,255,255,a))
        if i<3: star(im,16,16,3-i,(255,250,200))
        F.append(im)
    return F
def fx_assembly(P_pieces,fo):
    F=[]; cx,cy=24,24
    for i in range(10):
        im=Image.new('RGBA',(48,48),(0,0,0,0))
        t=min(1,i/5)
        if i<6:
            for j,p in enumerate(P_pieces):
                ang=(-2.2+j*2.2); dist=int((1-t)*16); x=int(cx-8+math.cos(ang)*dist+ (j-1)*3*(t)); y=int(cy-8+math.sin(ang)*dist*0.8-0)
                im.alpha_composite(p,(max(0,min(32,x)),max(0,min(32,y))))
        else:
            im.alpha_composite(fo,(cx-17,cy-9))
            r=(i-5)*4
            d=ImageDraw.Draw(im); d.ellipse([cx-r,cy-r,cx+r,cy+r],outline=(255,240,150,max(0,255-(i-5)*55)))
            for k in range(8):
                tt=k*math.pi/4; x=int(cx+math.cos(tt)*(r+3)); y=int(cy+math.sin(tt)*(r+3))
                if 0<=x<48 and 0<=y<48: im.putpixel((x,y),(255,255,255,255))
        if i in(5,6): star(im,cx,cy,5,(255,255,220))
        F.append(im)
    return F
# ---- pintu keluar ----
def exit_door(state,side,f=0):
    """side 's' pintu di dinding bawah, 'e' di dinding kanan. state locked/unlock/open"""
    c=Cv(32,32)
    if side=='s':
        c.r(0,0,32,32,'1c1a1e'); c.r(2,4,30,32,'5a6470'); c.r(4,6,28,32,'1c1a1e')
        if state!='open':
            ph=0 if state=='locked' else min(1,f/5.0); w=int(24*(1-ph)*0.5)
            c.r(4,6,4+12-int(12*ph),32,'414a55') ; c.r(28-12+int(12*ph),6,28,32,'414a55')
            if ph<1: c.r(15-int(0),6,17,32,'30363e')
            for y in(10,16,22,28): c.r(4,y,16-int(12*ph),y+1,'6b7480'); c.r(16+int(12*ph),y,28,y+1,'6b7480')
        else: c.r(4,6,28,32,'0c0a10'); c.r(6,8,26,32,'14121a')
    else:
        c.r(0,0,32,32,'1c1a1e'); c.r(0,2,28,30,'5a6470'); c.r(0,4,26,28,'1c1a1e')
        if state!='open':
            ph=0 if state=='locked' else min(1,f/5.0)
            c.r(0,4,26,4+12-int(12*ph),'414a55'); c.r(0,28-12+int(12*ph),26,28,'414a55')
            for x in(4,10,16,22): c.r(x,4,x+1,16-int(12*ph),'6b7480'); c.r(x,16+int(12*ph),x+1,28,'6b7480')
        else: c.r(0,4,26,28,'0c0a10'); c.r(0,6,24,26,'14121a')
    # lampu status + gembok
    lc={'locked':'e02020','unlock':'e0c040','open':'30e060'}[state]
    if side=='s': c.r(14,0,18,4,lc); c.r(15,1,17,3,'ffffff' if state!='locked' else 'ff8080')
    else: c.r(28,14,32,18,lc); c.r(29,15,31,17,'ffffff' if state!='locked' else 'ff8080')
    if state=='locked':
        if side=='s': c.r(13,16,19,23,'d8a820'); c.r(14,17,18,22,'a88418'); c.r(14,13,15,16,'98a2b0'); c.r(17,13,18,16,'98a2b0'); c.r(15,12,17,13,'98a2b0'); c.p(16,19,'201810')
        else: c.r(9,13,16,19,'d8a820'); c.r(10,14,15,18,'a88418'); c.r(12,11,13,13,'98a2b0'); c.p(12,16,'201810')
    elif state=='unlock':
        pass
    im=c.im; return im

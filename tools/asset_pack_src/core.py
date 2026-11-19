import json, os, math, random
from PIL import Image, ImageDraw
Z='/home/claude/work/zip'
OUT='/home/claude/work/out'
def load_atlas(name):
    d=json.load(open(f'{Z}/atlas/{name}.json')); im=Image.open(f'{Z}/atlas/{d["image"]}').convert('RGBA')
    R={}
    for r in d['regions']:
        R[(r['name'],r['index'])]=im.crop((r['x'],r['y'],r['x']+r['w'],r['y']+r['h']))
    return R
def frames(R,name):
    ks=sorted(i for (n,i) in R if n==name and i>=0)
    return [R[(name,i)].copy() for i in ks]
def hexc(h,a=255):
    h=h.lstrip('#'); return (int(h[0:2],16),int(h[2:4],16),int(h[4:6],16),a)
def shade(c,f):
    return tuple(max(0,min(255,int(v*f))) for v in c[:3])+(c[3] if len(c)>3 else 255,)
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
OUTL=(0x16,0x0e,0x14)
def is_shadow(p): return p[3]>0 and p[:3]==(0,0,0) and p[3]<255
def split_shadow(im):
    sh=Image.new('RGBA',im.size,(0,0,0,0)); bd=im.copy(); px=im.load(); sp=sh.load(); bp=bd.load()
    for y in range(im.height):
        for x in range(im.width):
            if is_shadow(px[x,y]): sp[x,y]=px[x,y]; bp[x,y]=(0,0,0,0)
    return bd,sh
def shift(im,dx,dy):
    o=Image.new('RGBA',im.size,(0,0,0,0)); o.alpha_composite(im,(dx,dy)) if (dx>=0 and dy>=0) else o.paste(im,(dx,dy)); return o
def body_bbox(bd):
    return bd.getbbox()
def squash(im,fy=1.0,fx=1.0,anchor=None):
    """skala body (tanpa bayangan), anchor = kaki bawah, tengah-x."""
    bd,sh=split_shadow(im); bb=bd.getbbox()
    if not bb: return im.copy()
    crop=bd.crop(bb); w=max(1,round(crop.width*fx)); h=max(1,round(crop.height*fy))
    c2=crop.resize((w,h),Image.NEAREST)
    cx=(bb[0]+bb[2])/2; x0=int(round(cx-w/2)); y0=bb[3]-h
    o=sh.copy(); o.alpha_composite(c2,(max(0,x0),max(0,y0))) if x0>=0 and y0>=0 else o.paste(c2,(x0,y0),c2)
    return o
def tint(im,col,amt,outline_amt=None):
    o=im.copy(); px=o.load()
    for y in range(o.height):
        for x in range(o.width):
            p=px[x,y]
            if p[3]==0 or is_shadow(p): continue
            a=amt*(0.35 if (outline_amt is None and p[:3]==OUTL) else 1)
            if outline_amt is not None and p[:3]==OUTL: a=outline_amt
            px[x,y]=lerp(p,col,a)+(p[3],)
    return o
def set_alpha(im,k):
    o=im.copy(); px=o.load()
    for y in range(o.height):
        for x in range(o.width):
            p=px[x,y]
            if p[3]: px[x,y]=p[:3]+(int(p[3]*k),)
    return o
def sc(im,k): return im.resize((im.width*k,im.height*k),Image.NEAREST)

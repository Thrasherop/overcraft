TAG='final'
# Fights v2, for the final tree and the base (e6f3cef). Only commands both builds have. HARNESS INPUT: not a person.
# Nearest MOB only (projectiles excluded), mob count by /execute's answer, the hero strafes and backs off and jumps, deaths' causes from the chat's death lines.
import math,random
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_lib.py").read())
MOBS='type=#minecraft:skeletons'
SEL='@e[type=minecraft:zombie]','@e[type=minecraft:skeleton]','@e[type=minecraft:creeper]'
def count():
    n=0
    for s in SEL:
        for l in mc('/execute if entity %s'%s,0.08)[-1:]:
            m=re.search(r'Test passed\. Count: (\d+)',l)
            if m: n+=int(m.group(1))
    return n
def near(s):
    best=None
    for m in ('zombie','skeleton','creeper'):
        for l in mc('/data get entity @e[type=minecraft:%s,sort=nearest,limit=1,distance=..40] Pos'%m,0.05)[-2:]:
            r=re.search(r'\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]',l)
            if r:
                p=tuple(float(x) for x in r.groups()); d=(p[0]-s['x'])**2+(p[2]-s['z'])**2
                if best is None or d<best[0]: best=(d,p)
    return best
ABIL={'kiriko':['ability_1','ability_2'],'lucio':['ability_2','ability_1'],'brigitte':['ability_1','ability_2'],'ana':['ability_2','ability_1'],'sombra':['ability_1','ability_2'],'jetpack_cat':['ability_2'],'sigma':['ability_2','ability_1']}
random.seed(7)
mc('/gamerule minecraft:natural_health_regeneration true'); mc('/difficulty normal'); mc('/gamerule minecraft:spawn_monsters false'); c('window 1280 720')
for hero,base in (('kiriko',1000),('brigitte',1100),('ana',1200),('sombra',1300)):
  for rnd in (1,2):
    pick(hero); mc('/gamemode survival @s'); mc('/time set midnight')
    mc('/tp @s %d -60 %d'%(base,rnd*100)); sleep(1.5)
    mc('/fill %d -60 %d %d -50 %d minecraft:air'%(base-20,rnd*100-20,base+20,rnd*100+20)); mc('/fill %d -61 %d %d -61 %d minecraft:grass_block'%(base-20,rnd*100-20,base+20,rnd*100+20))
    mc('/spawnpoint @s %d -60 %d'%(base,rnd*100)); mc('/kill @e[type=!minecraft:player]')
    for (m,dx,dz) in (('zombie',13,2),('zombie',-13,4),('zombie',2,14),('skeleton',-9,-11),('skeleton',10,-10),('creeper',0,-15)):
        mc('/summon minecraft:%s %d -60 %d'%(m,base+dx,rnd*100+dz),0.05)
    n0=len(lines()); log('start',hero,rnd,'mobs',count()); c('key secondary_fire down')
    t0=time.time(); lastab=lastmove=t0; deaths=0; low=99; ab=ABIL[hero]; k=0; shot=False; move=None; cleared=None
    while time.time()-t0<30:
        s=st()
        if s['health']<=0:
            deaths+=1
            for mv in ('forward','back','left','right'): c('key %s up'%mv)
            c('key secondary_fire up'); sleep(1); c('respawn'); sleep(1.5); s=st(); log(hero,rnd,'died at %.1f, back as'%(time.time()-t0),s.get('hero'),'hp',round(s['health'],1),'mode',s.get('heroMode')); c('key secondary_fire down'); continue
        low=min(low,s['health'])
        b=near(s)
        if b:
            p=b[1]; dx=p[0]-s['x']; dz=p[2]-s['z']; dy=(p[1]+1.0)-(s['y']+1.6); yaw=math.degrees(math.atan2(-dx,dz)); pitch=-math.degrees(math.atan2(dy,math.hypot(dx,dz))); c('look %.1f %.1f'%(yaw,pitch))
        elif cleared is None and count()==0:
            cleared=time.time()-t0; break
        now=time.time()
        if now-lastmove>0.8:
            if move: c('key %s up'%move)
            # backs off when something's near, else strafes; a jump now and then
            move='back' if b and b[0]<25 else random.choice(['left','right'])
            c('key %s down'%move); lastmove=now
            if random.random()<0.3: c('key jump down'); sleep(0.08); c('key jump up')
        if now-lastab>2.5:
            lastab=now; c('key %s down'%ab[k%len(ab)]); sleep(0.12); c('key %s up'%ab[k%len(ab)]); k+=1
        if not shot and now-t0>10:
            shot=True; c('screenshot p4_%s_fight_%s_%d.png'%(TAG,hero,rnd))
        sleep(0.1)
    if move: c('key %s up'%move)
    c('key secondary_fire up'); s=st()
    left=count()
    chat=[l for l in lines()[n0:] if '[CHAT]' in l and re.search(r'(was slain|was shot|blown up|blew up|fell|hit the ground|burned|drowned|died|killed by|was killed)',l)]
    log('##',TAG,hero,'round',rnd,'mobs left',left,'of 6','cleared at',None if cleared is None else round(cleared,1),'deaths',deaths,'lowest hp %.1f'%low,'end hp %.1f/%.1f'%(s['health'],s['maxHealth']))
    for l in chat: log('   death line:',l[l.index('[CHAT]')+7:][:90])
    mc('/kill @e[type=!minecraft:player]')
mc('/time set noon'); c('window 1920 1080')

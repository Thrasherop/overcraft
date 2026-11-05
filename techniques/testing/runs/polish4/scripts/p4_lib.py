def setup():
    for k in ('/difficulty normal','/gamerule minecraft:spawn_monsters false','/gamerule minecraft:natural_health_regeneration false','/time set noon','/weather clear',
              '/team add red','/team add blue','/team join blue @s','/kill @e[type=minecraft:slime]'):
        log(k, mc(k)[-1:])
def pick(h):
    mc('/kill @s'); sleep(0.5)
    c('respawn'); sleep(1.5)
    mc('/spawnpoint @s 0 -60 0');
    c('hero '+h); sleep(0.8); c('close'); c('heromode on'); sleep(0.5)
    s=st(); log('hero',s.get('hero'),'mode',s.get('heroMode'),'screen',s.get('screen'),'hp',s.get('health'),'/',s.get('maxHealth'))
def tp(x,y,z,yaw=0,pitch=0):
    mc('/tp @s %s %s %s %s %s'%(x,y,z,yaw,pitch)); c('look %s %s'%(yaw,pitch)); sleep(0.25)
def bot(cmd): return c('bot '+cmd)
def trace_run(fn, secs=2.0):
    c('trace start'); fn(); sleep(secs); return c('trace').get('trace','')
def peak(tr):
    out=[]
    for l in tr.splitlines():
        p=l.split()
        out.append((int(p[0]),float(p[1]),float(p[3]),float(p[4]),p[5]))
    if not out: return {}
    x0=None
    return dict(hmax=max(o[1] for o in out),vmax=max(o[2] for o in out),ymax=max(o[3] for o in out),n=len(out))
def h(): return round(st()['health'],2)
def press(k,t=0.15):
    c('key %s down'%k); sleep(t); c('key %s up'%k)
def inp(n,held,pressed,extra=0): return bot('input %s %s %s %s'%(n,held,pressed,extra))

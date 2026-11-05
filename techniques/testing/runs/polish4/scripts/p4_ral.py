exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_lib.py").read())
FX=globals().get('FIX')
log('=== Rally: the bash and the shockwave on one target.', LABEL, 'Bot Brigitte (server-side, not a second client); victim: the real client and a NoAI husk; harness input.')
def add(n,hero,team,x,z,yaw,pitch=0):
    r=bot('add %s %s %s %s -60 %s %s'%(n,hero,team,x,z,yaw)); mc('/gamemode survival '+n); bot('look %s %s %s'%(n,yaw,pitch)); return r
def rally(n='brig'):
    bot('charge %s 9999'%n); bot('press %s ultimate'%n); sleep(1.2); bot('secondary %s on'%n); sleep(0.6)
def bash(n='brig'):
    inp(n,3,0); sleep(0.2); inp(n,2,0); sleep(0.4); inp(n,0,0); bot('secondary %s off'%n)
def far(tr):
    p=[l.split() for l in tr.splitlines()]
    if not p: return 'no trace'
    return dict(ticks=len(p),peakH=max(float(x[1]) for x in p),peakV=max(float(x[3]) for x in p),firstline=tr.splitlines()[0][:80])
for mode in ('hero','minecraft'):
    pick('kiriko'); mc('/gamemode survival @s'); mc('/effect clear @s'); tp(100.5,-60,0.5,0,0); 
    bot('remove brig')
    if mode=='minecraft': press('toggle_mode') if False else c('heromode off'); sleep(0.3)
    add('brig','brigitte','red',100.5,-1.5,0); sleep(0.4)
    rally(); s0=st(); h0=s0['health']; x0=(s0.get('x'),s0.get('z'))
    log('--- real client,',mode,'health',h0,'pos',s0.get('pos'),x0)
    tr=trace_run(bash,2.5); s1=st(); log('peak',far(tr)); log('health',h0,'->',s1['health'],'= OW',round((h0-s1['health'])*12.5,1),'pos',s1.get('pos'),(s1.get('x'),s1.get('z')))
    open(r"%s/p_rally_trace_%s_%s.txt"%(globals().get('OUTD',r"E:/claude/mods/overcraft_polish4/techniques/testing/runs/polish4"),globals().get('TAG','base'),mode),'w').write(tr)
    sleep(1); bot('remove brig'); c('heromode on')
# a husk
mc('/kill @e[type=minecraft:husk]'); pick('kiriko'); tp(200.5,-60,-12,0,0)
mc('/summon minecraft:husk 200.5 -60 0.5 {NoAI:1b,Tags:["x"],Health:200f,attributes:[{id:"minecraft:max_health",base:200}],equipment:{}}'); sleep(0.3)
add('brig','brigitte','red',200.5,-1.5,0); sleep(0.4)
log('--- husk, on team? red bot, husk an enemy of no one: Allies decides')
rally(); hb=hp('@e[tag=x,limit=1]'); bash(); sleep(0.6); hh=hp('@e[tag=x,limit=1]')
log('husk',hb,'->',hh,'= OW',round((hb-hh)*12.5,1) if hb and hh else None, mc('/data get entity @e[tag=x,limit=1] Pos',0.4)[-1:])
bot('remove brig'); mc('/kill @e[tag=x]')

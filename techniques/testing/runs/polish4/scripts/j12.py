XSTART = 80000
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_tenth.py", encoding="utf8").read()
O = ('none',); ROWS = 1
exec(src.replace("clear(); log('DONE')\n", ""))
log('### real Brigitte, Whiplash: she is at the spot before the wall is placed (the earlier rows placed it from spawn, in an unloaded chunk: void). G as the health read shows the hit')
def hk(k):
    for i in range(3):
        l = mc('/data get entity @e[tag=hk,sort=nearest,limit=1] %s' % k, 0.1)[-1:]
        if l and 'following' in l[0]: return l[0].split('data:')[-1].strip()[:50]
    return None
for how in ('control', 'G') * 3:
    x = spot(); xi = int(x); pick('brigitte'); mc('/gamemode survival @s'); mc('/time set midnight'); tp(x, -60, -4.5, 0, 0); c('look 0 0'); sleep(0.5); mc('/fill %d -60 6 %d -56 6 minecraft:stone' % (xi - 3, xi + 3), 0.2); mc('/summon minecraft:husk %s -60 0.5 {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); bot('perk me BRIGITTE_WHIPLASH'); sleep(0.2); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); t0 = time.time(); wh = None
    while time.time() - t0 < 0.6:
        v = hk('Health')
        if v and not v.startswith('200'): wh = time.time() - t0; break
    if how == 'G': c('heromode off')
    sleep(1.2); log('  %s: hit read at +%s; after %s health %s; mode %s' % (how, None if wh is None else round(wh, 2), hk('Pos'), hk('Health'), st().get('heroMode'))); mc('/kill @e[tag=hk]', 0.05); c('heromode on')
clear(); log('DONE')

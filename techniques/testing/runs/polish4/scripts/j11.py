XSTART = 60000
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_tenth.py", encoding="utf8").read()
O = ('none',); ROWS = 1
exec(src.replace("clear(); log('DONE')\n", ""))
log('### real Brigitte: a husk and a wall at z D, read of the husk z and health; whether the push reaches the wall inside the window')
def hk(k):
    for i in range(3):
        l = mc('/data get entity @e[tag=hk,sort=nearest,limit=1] %s' % k, 0.3)[-1:]
        if l and 'following' in l[0]: return l[0].split('data:')[-1].strip()[:50]
    return None
for how, D in (('control', 4), ('control', 3), ('G', 3), ('control', 2), ('G', 2), ('control', 2), ('G', 2), ('control', 3), ('G', 3)):
    x = spot(); xi = int(x); pick('brigitte'); mc('/gamemode survival @s'); mc('/time set midnight'); mc('/fill %d -60 %d %d -56 %d minecraft:stone' % (xi - 3, D, xi + 3, D), 0.1); mc('/summon minecraft:husk %s -60 0.5 {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); tp(x, -60, -4.5, 0, 0); c('look 0 0'); sleep(0.5); bot('perk me BRIGITTE_WHIPLASH'); sleep(0.2); q = hk('Pos'); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.2)
    if how == 'G': c('heromode off')
    sleep(1.2); log('  %s, wall %s: husk before %s; after %s health %s; mode %s' % (how, D, q, hk('Pos'), hk('Health'), st().get('heroMode'))); mc('/kill @e[tag=hk]', 0.05); c('heromode on')
clear(); log('DONE')

XSTART = 40000
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_tenth.py", encoding="utf8").read()
O = ('none',); ROWS = 1
exec(src.replace("clear(); log('DONE')\n", ""))
log('### real Brigitte, Whiplash by `bot perk me`, a husk, a wall at z 4: G 0.25 s after the press, or not; the husk\'s health read by the data command, three tries')
def hk():
    for i in range(3):
        l = mc('/data get entity @e[tag=hk,sort=nearest,limit=1] Health', 0.3)[-1:]
        m = re.search(r'([\d.]+)f', l[0]) if l else None
        if m: return float(m.group(1))
    return None
for how in ('control', 'G', 'control', 'G', 'G'):
    x = spot(); xi = int(x); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); bot('perk me BRIGITTE_WHIPLASH'); mc('/time set midnight'); mc('/fill %d -60 4 %d -56 4 minecraft:stone' % (xi - 3, xi + 3), 0.1); mc('/summon minecraft:husk %s -60 0.5 {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); tp(x, -60, -4.5, 0, 0); c('look 0 0'); sleep(0.3); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.2)
    if how == 'G': c('heromode off')
    sleep(1.2); log('  real Brigitte, %s: husk %s, mode %s' % (how, hk(), st().get('heroMode'))); mc('/kill @e[tag=hk]', 0.05); c('heromode on')
clear(); log('DONE')

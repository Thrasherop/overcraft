D = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
import math
exec(open(D + "p4_lib.py", encoding="utf8").read())
log('=== Play on tree a20c352, world p4b, survival, normal. Keys: KeyMapping.set through DevServer, aim by look, not a mouse or a person. Bots: server-side players, no second client. Mobs: real ones, /summon.')
c('window 1280 720'); sleep(1)
def add(n, hero, team, x, z, yaw, pitch=0, y=-60):
    r = bot('add %s %s %s %s %s %s %s' % (n, hero, team, x, y, z, yaw)); mc('/gamemode survival ' + n, 0.15); bot('look %s %s %s' % (n, yaw, pitch)); return r
def ground(x0, z0, r=25):
    mc('/fill %d -60 %d %d -50 %d minecraft:air' % (x0 - r, z0 - r, x0 + r, z0 + r), 0.2); mc('/fill %d -61 %d %d -61 %d minecraft:grass_block' % (x0 - r, z0 - r, x0 + r, z0 + r), 0.2)
AN=[0]
def ana(x,z,yaw,pitch=0):
    AN[0]+=1; n='ana%d'%AN[0]; add(n,'ana','red',x,z,yaw,pitch); sleep(0.5); return n
def clear():
    for n in ('anaE', 'allyB', 'kiriB', 'brig', 'som', 'weaver'):
        bot('remove ' + n)
def health(sel):
    return hp(sel)
mc('/gamerule minecraft:natural_health_regeneration true'); mc('/time set midnight'); mc('/difficulty normal')

# ---- 1. Rally by her own keys against real zombies in a row
c('close'); sleep(1); log(c('openworld p4b'))
for i in range(30):
    sleep(2)
    if st().get('screen') == 'HeroSelectScreen': break
sleep(1); c('close'); sleep(1)
mc('/gamerule minecraft:natural_health_regeneration true'); mc('/time set midnight'); mc('/difficulty normal')
log('--- 1 real Brigitte: Q, right button up, left button bash, at midnight; zombies in iron helmets (fire resistance) 2 m and 5 m ahead, a third behind her')
for fix in ('off', 'on'):
    x = 3000 if fix == 'off' else 3100
    bot('fix rally ' + fix); pick('brigitte'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); ground(x, 0); mc('/time set midnight'); mc('/kill @e[type=!minecraft:player]'); tp(x + 0.5, -60, 0.5, 0, 0)
    for tag, dz in (('a', 2.2), ('b', 5.5), ('c', -3.5)):
        mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:20f,equipment:{head:{id:"minecraft:iron_helmet",count:1}},active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (x + 0.5, 0.5 + dz, tag), 0.1)
    sleep(0.3); log('fix rally', fix, 'start', [health('@e[tag=%s,limit=1]' % t) for t in 'abc'])
    mc('/giveult'); press('ultimate', 0.15); sleep(1.2); log('  Rally', st().get('maxHealth'), 'ultActive?', st().get('ultActive'))
    c('key primary_fire down'); sleep(0.5); c('screenshot p4_play_rally_%s_up.png' % fix)
    press('secondary_fire', 0.15); sleep(0.25); c('screenshot p4_play_rally_%s_hit.png' % fix); log('  asleep', bot('asleep')); sleep(0.9); c('key primary_fire up')
    log('  after', [health('@e[tag=%s,limit=1]' % t) for t in 'abc'], '(armour: an iron helmet, 2 points)'); c('screenshot p4_play_rally_%s_after.png' % fix); log('  heard', str(c('heard'))[:200])
bot('fix rally on')

# ---- 2. A night: Kiriko, her wolf and a villager slept by an enemy, zombies coming
log('--- 2 real Kiriko at midnight: a tamed wolf and a villager slept by a bot Ana, two zombies walking at them, Suzu by the real key')
x = 3200; pick('kiriko'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); ground(x, 0); mc('/time set midnight'); mc('/kill @e[type=!minecraft:player]'); tp(x - 6.5, -60, 2.5, 0, 30)
UUID = re.search(r"\[I; ?[-\d, ]+\]", ' '.join(mc('/data get entity @s UUID', 0.4))).group(0)
mc('/summon minecraft:wolf %s -60 4.5 {Tags:["w"],Owner:%s,Health:8f,Sitting:1b,OrderedToSit:1b}' % (x + 0.5, UUID), 0.1); mc('/summon minecraft:villager %s -60 4.5 {Tags:["v"],Health:10f,attributes:[{id:"minecraft:movement_speed",base:0.0}]}' % (x + 2.5), 0.1)
mc('/summon minecraft:zombie %d -60 22 {Tags:["z"]}' % x, 0.1); mc('/summon minecraft:zombie %d -60 22 {Tags:["z"]}' % (x + 3), 0.1); sleep(0.2)
log('  wolf side', bot('side wolf')); log('  villager side', bot('side villager'))
n = ana(x + 0.5, 7.5, 180, 20); bot('press %s ability1' % n); sleep(0.7); log('  after a dart at the wolf', bot('asleep'))
n2 = ana(x + 2.5, 7.5, 180); bot('press %s ability1' % n2); sleep(0.7); log('  after one at the villager', bot('asleep'))
c('screenshot p4_play_night_asleep.png'); log('  wolf', health('@e[tag=w,limit=1]'), 'villager', health('@e[tag=v,limit=1]'))
c('look -70 12'); sleep(0.25); press('ability_2', 0.12); log('  Suzu cd2', st().get('ability2Cooldown'))
for i in range(5):
    sleep(0.25); log('   +%.2f asleep %s' % (0.25 * (i + 1), bot('asleep')))
    if i == 0: c('screenshot p4_play_night_burst.png')
log('  wolf', health('@e[tag=w,limit=1]'), 'villager', health('@e[tag=v,limit=1]')); c('screenshot p4_play_night_awake.png')
bot('remove '+n); bot('remove '+n2)
c('key secondary_fire down'); t0 = time.time(); deaths = 0
while time.time() - t0 < 14:
    p = None
    for l in mc('/data get entity @e[tag=z,sort=nearest,limit=1] Pos', 0.05)[-2:]:
        m = re.search(r'\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]', l)
        if m: p = [float(v) for v in m.groups()]
    s = st()
    if s['health'] <= 0: deaths += 1; break
    if p:
        c('look %.1f %.1f' % (math.degrees(math.atan2(-(p[0] - s['x']), p[2] - s['z'])) if False else -math.degrees(__import__('math').atan2(p[0] - s['x'], p[2] - s['z'])), 8))
    sleep(0.2)
c('key secondary_fire up')
log('  14 s of kunai: zombies left', mc('/execute if entity @e[tag=z]', 0.2)[-1:], 'health', st()['health'], 'wolf', health('@e[tag=w,limit=1]'), 'villager', health('@e[tag=v,limit=1]')); c('screenshot p4_play_night_fight.png')

# ---- 3. The real client slept among real zombies, woken by a bot Kiriko, and it fights on
log('--- 3 real Sombra asleep at midnight with zombies on her: a bot Kiriko wakes her')
x = 3300; pick('sombra'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); ground(x, 0); mc('/time set midnight'); mc('/kill @e[type=!minecraft:player]'); tp(x + 0.5, -60, 0.5, 0, 0)
mc('/summon minecraft:zombie %d -60 9 {Tags:["z"]}' % x, 0.1); mc('/summon minecraft:zombie %d -60 -9 {Tags:["z"]}' % x, 0.1)
n = ana(x + 0.5, 4, 180); add('kiriB', 'kiriko', 'blue', x - 3, 0.5, -90, 72); sleep(0.2)
bot('press %s ability1' % n); sleep(0.5); log('  slept', bot('cc me').get('asleep'), 'health', st()['health']); bot('remove ' + n); sleep(1.0); c('screenshot p4_play_sombra_asleep.png')
log('  before Suzu', bot('cc me').get('asleep'), st()['health']); bot('press kiriB ability2')
for i in range(8):
    sleep(0.2); r = bot('cc me'); log('   +%.1f asleep %s phased %s health %.1f' % (0.2 * (i + 1), r['asleep'], r['phased'], r['health']))
press('ability_1'); sleep(0.2); log('  Cyberspace by the key, cd1', st().get('ability1Cooldown')); c('screenshot p4_play_sombra_awake.png'); bot('remove kiriB'); sleep(1)

# ---- 4. Minecraft mode, Life Grip, water
log('--- 4 asleep in Minecraft mode (G off), a bot Kiriko')
x = 3400; pick('mercy'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); ground(x, 0); mc('/kill @e[type=!minecraft:player]'); tp(x + 0.5, -60, 0.5, 0, 0); c('heromode off'); sleep(0.3)
n = ana(x + 0.5, 4, 180); add('kiriB', 'kiriko', 'blue', x - 3, 0.5, -90, 72); sleep(0.2)
bot('press %s ability1' % n); sleep(0.5); log('  slept', bot('cc me').get('asleep'), 'mode', st().get('heroMode')); bot('press kiriB ability2')
for i in range(6):
    sleep(0.2); log('   +%.1f %s' % (0.2 * (i + 1), bot('cc me').get('asleep')))
c('heromode on'); clear(); bot('remove '+n); sleep(0.5)
log('--- 5 Life Grip wakes: a bot Lifeweaver grips the slept real client')
x = 3500; pick('mercy'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); ground(x, 0); mc('/kill @e[type=!minecraft:player]'); tp(x + 0.5, -60, 0.5, 180, 0)
add('weaver', 'lifeweaver', 'blue', x + 0.5, -6.5, 0); n = ana(x + 0.5, 4, 180); sleep(0.3)
bot('press %s ability1' % n); sleep(0.5); log('  slept', bot('cc me').get('asleep')); log('  grip', bot('press weaver ability2'))
for i in range(8):
    sleep(0.2); r = bot('cc me'); log('   +%.1f asleep %s phased %s' % (0.2 * (i + 1), r['asleep'], r['phased']))
log('  (which of his keys is the grip: read in the code; a miss here says nothing)'); clear(); bot('remove '+n); sleep(0.5)
log('--- 6 asleep in water, a bot Kiriko on the bank')
x = 3600; pick('mercy'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); ground(x, 0); mc('/kill @e[type=!minecraft:player]')
mc('/fill %d -62 -5 %d -61 5 minecraft:water' % (x - 5, x + 5), 0.2); tp(x + 0.5, -61, 0.5, 0, 0)
n = ana(x + 0.5, 7, 180, 12); add('kiriB', 'kiriko', 'blue', x - 7, 0.5, -90, 30); sleep(0.2)
bot('press %s ability1' % n); sleep(0.5); log('  slept', bot('cc me').get('asleep'), 'y', st().get('y')); bot('press kiriB ability2')
for i in range(8):
    sleep(0.25); r = bot('cc me'); log('   +%.2f asleep %s phased %s y %s' % (0.25 * (i + 1), r['asleep'], r['phased'], st().get('y')))
c('screenshot p4_play_water.png'); clear(); bot('remove '+n)
mc('/gamerule minecraft:natural_health_regeneration false')
log('DONE')

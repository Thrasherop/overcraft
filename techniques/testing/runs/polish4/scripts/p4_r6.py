XSTART = globals().get('XSTART', 130000)
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
setup(); mc('/gamemode survival @s')
def z(tag): return hp('@e[tag=%s,sort=nearest,limit=1]' % tag)
log('--- #122 by real keys: three runs each. Zombies with fire resistance at midnight: a bashed at 2.2 m, w behind a wall at 5 m; a red bot Brigitte 5 m off, her barrier up or not')
for i, barrier in enumerate((False, True, False, True, False, True)):
    x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0)
    mc('/fill %d -60 3 %d -56 3 minecraft:stone' % (x + 2, x + 6), 0.2)
    for tag, xz in (('a', (x, 2.7)), ('w', (x + 4, 5.5))):
        mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:20f,active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (xz[0], xz[1], tag), 0.1)
    d = add('bri', 'brigitte', 'red', x - 2.5, 5.5, 180)
    if barrier: bot('secondary %s on' % d); sleep(0.6)
    mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); h0 = cc(d)['health']; c('key secondary_fire down'); r = rows(d, 0.5, ['asleep', 'health']); c('key secondary_fire up'); ass = bot('asleep'); sleep(0.5); c('key primary_fire up')
    log('  run', i, 'barrier', barrier, 'bashed', z('a'), 'behind the wall', z('w'), 'bot', h0, '->', cc(d)['health'], 'bot stunned', span(r, 1), 'her barrier', bot('read ' + d).get('barrierHealth'))
log('--- a blast on the phased and not (the critique asks for bed blasts, #27): an ignited creeper at the real client; a bed by the key was tried and didn\'t explode for the harness')
for phased in (False, True):
    x = spot(); me('mercy', x); mc('/gamerule minecraft:mob_griefing false', 0.1); h0 = st()['health'];
    if phased: bot('phase me 2')
    mc('/summon minecraft:creeper %s -60 2 {Fuse:1s,ignited:1b,NoAI:1b}' % x, 0.1); sleep(0.8); log('  phased', phased, 'health', h0, '->', st()['health'])
    if st()['health'] <= 0: c('respawn'); sleep(1.5); c('close')
mc('/gamerule minecraft:mob_griefing true', 0.1)
log('--- phase after the whip: a whip at a bystander-free enemy, a tamed cat of a red bot; the bot Kiriko phases it inside the held push')
for phased in (False, True):
    x = spot(); me('brigitte', x, 0.5, 0, 6); ow = add('own', 'mercy', 'red', x + 8, 0.5, 0); mc('/summon minecraft:zombie %s -60 5.5 {Tags:["wz"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}],active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % x, 0.2); sleep(0.2)
    c('key ability_1 down'); sleep(0.1); c('key ability_1 up'); p0 = None; t0 = time.time(); seen = []
    while time.time() - t0 < 1.0:
        l = mc('/data get entity @e[tag=wz,sort=nearest,limit=1] Pos', 0.03)[-1:]
        m = re.search(r'\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]', l[0]) if l else None
        if m: seen.append(float(m.group(3)))
    log('  zombie z over 1 s, no phase test here (a zombie can\'t be Suzu\'d by a real Kiriko from the red side in this script)', seen[:1], seen[-1:]); mc('/kill @e[tag=wz]', 0.1); break
clear(); log('DONE')

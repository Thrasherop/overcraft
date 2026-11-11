exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
ONLY = globals().get('ONLY')
def want(k): return ONLY is None or k in ONLY
UU = lambda: re.search(r"\[I; ?[-\d, ]+\]", ' '.join(mc('/data get entity @s UUID', 0.4))).group(0)
c('close'); sleep(1)
if 'error' in bot('cc me'):
    log(c(globals().get('OPEN', 'openworld ' + WORLD)))
    for i in range(40):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    sleep(1); c('close'); sleep(1)
setup(); mc('/gamemode survival @s'); c('window 1280 720')

if want('rallypos'):
    log('### Rally by keys, first of a session first: where she ends; each run\'s shots its own')
    for i, fix in enumerate(('off', 'on', 'off', 'on', 'off')):
        log('reply', bot('fix rally ' + fix)); x = spot('grass_block'); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); mc('/kill @e[type=!minecraft:player]'); tp(x, -60, 0.5, 0, 0)
        for tag, dz in (('a', 2.2), ('b', 5.5), ('c', -3.5)):
            mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:20f,equipment:{head:{id:"minecraft:iron_helmet",count:1}},active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (x, 0.5 + dz, tag), 0.1)
        sleep(0.3); start = [hp('@e[tag=%s,sort=nearest,limit=1]' % t) for t in 'abc']; mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); z0 = st()['z']; c('key secondary_fire down'); rs = rows('me', 0.45, ['z', 'asleep']); c('key secondary_fire up'); sleep(0.7); c('key primary_fire up')
        log('run', i, 'fix', fix, 'start', start, 'after a b c', [hp('@e[tag=%s,sort=nearest,limit=1]' % t) for t in 'abc'], 'z from', round(z0, 2), 'max', round(max(q[1] for q in rs), 2), 'a at', 2.7, 'stunned', bot('asleep').get('asleep'), 'ticks read', len(rs))
    bot('fix rally on')
    log('--- a phased bot in the wave, after a bash that lands')
    x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0); mc('/summon minecraft:zombie %s -60 2.7 {NoAI:1b,Tags:["a"],Health:20f,active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % x, 0.1); p = add('red', 'mercy', 'red', x + 2, 6, 180); kr = add('krd', 'kiriko', 'red', x + 2, 8, 180, 72); sleep(0.2)
    mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); bot('press %s ability2' % kr); press('secondary_fire', 0.1); r = rows(p, 0.6, ['phased', 'asleep', 'health']); c('key primary_fire up'); log('  phase', span(r, 1), 'stun', span(r, 2), 'health', r[0][3], r[-1][3], 'bashed', hp('@e[tag=a,sort=nearest,limit=1]'))
    x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0); mc('/summon minecraft:zombie %s -60 2.7 {NoAI:1b,Tags:["a"],Health:20f,active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % x, 0.1); p = add('red', 'mercy', 'red', x + 2, 6, 180); sleep(0.2)
    mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); press('secondary_fire', 0.1); r = rows(p, 0.6, ['phased', 'asleep', 'health']); c('key primary_fire up'); log('  control, no Suzu: stun', span(r, 2), 'health', r[0][3], r[-1][3], 'bashed', hp('@e[tag=a,sort=nearest,limit=1]'))

if want('fire'):
    log('### fire: the block gone before the Suzu')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); mc('/setblock %d -60 0 minecraft:fire' % (x - 0.5), 0.3); sleep(0.8); mc('/setblock %d -60 0 minecraft:air' % (x - 0.5), 0.1); log('  burning', sh('me', 'fire', 'health')); bot('press %s ability2' % k)
    for i in range(4):
        sleep(0.2); log('   +%.1f %s' % (0.2 * (i + 1), sh('me', 'fire', 'phased', 'health')))
    x = spot(); me('kiriko', x, 0.5, 0, 62); mc('/summon minecraft:wolf %s -60 3.5 {Tags:["w"],Owner:%s,Health:8f,Sitting:1b,OrderedToSit:1b,Fire:200s}' % (x + 1, UU()), 0.2); fire = lambda: re.search(r'data: ([-\d]+)s', ' '.join(mc('/data get entity @e[tag=w,sort=nearest,limit=1] Fire', 0.3))); log('  wolf Fire', fire().group(1) if fire() else None); press('ability_2', 0.1); sleep(0.5); log('  wolf Fire after', fire().group(1) if fire() else None)

if want('creeper'):
    log('### a creeper, EMP, then a Suzu. Allies: a creeper is everyone\'s enemy, on a team or not, so the Suzu takes it for no ally')
    for suzu in (False, True):
        x = spot(); me('mercy', x, -13); mc('/gamerule minecraft:mob_griefing false', 0.1); s_ = add('som', 'sombra', 'red', x, 6.5, 0); bot('charge %s 9999' % s_); k = add('kiri', 'kiriko', 'blue', x - 3, 9, -90, 72); sleep(0.2); mc('/summon minecraft:creeper %s -60 9 {Tags:["cr"],Team:"blue",attributes:[{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.15); bot('press %s ultimate' % s_); log('  side', {k2: v for k2, v in bot('side creeper').items() if k2 in ('ally', 'enemy')}); sleep(0.9); c('screenshot p4_fin_creeper_%s_a.png' % suzu); t0 = time.time()
        if suzu: log('  Suzu', bot('press %s ability2' % k))
        for i in range(10):
            sleep(0.4); ex = mc('/execute if entity @e[tag=cr]', 0.1)[-1:]; log('   +%.2f creeper %s, Sombra %s' % (time.time() - t0, ex[0][-16:] if ex else '?', sh(s_, 'health')))
            if i == 2: c('screenshot p4_fin_creeper_%s_b.png' % suzu)
        mc('/kill @e[tag=cr]'); sleep(0.3)
    mc('/gamerule minecraft:mob_griefing true', 0.1)

if want('callers'):
    log('### the other phases: Swift Step, Translocator, Resurrect, a dart inside each')
    for go in (False, True):
        x = spot(); me('kiriko', x, 0.5, 0, 0); a = add('ally', 'mercy', 'blue', x, 6.5, 180); n = add('ana', 'ana', 'red', x + 0.5, -4, 0, 0); sleep(0.2)
        bot('press %s ability1' % n); sleep(0.03)
        if go: press('ability_1', 0.1)
        r = rows('me', 0.8, ['phased', 'asleep', 'z']); log('  Swift Step', go, 'phase', span(r, 1), 'asleep', span(r, 2), 'z', r[-1][3])
    x = spot(); me('sombra', x); c('heard'); press('ability_2', 0.1); r = rows('me', 2.0, ['phased', 'z', 'stealth']); ph = span(r, 1); log('  Translocator alone: phase', ph, 'from the press', ph[0] - r[0][0] if ph else None, 'ticks, z', r[-1][2]); landz = r[-1][2]; lead = (ph[0] - r[0][0]) / 20 if ph else 0.8; sleep(1)
    x = spot(); me('sombra', x); n = add('ana', 'ana', 'red', x, landz + 2.5, 180, 0); sleep(0.2); press('ability_2', 0.1); t0 = time.time(); th = [False]
    def ev(q):
        if not th[0] and time.time() - t0 > lead - 0.02: th[0] = True; bot('press %s ability1' % n)
    r = rows('me', 1.6, ['phased', 'asleep', 'z'], ev); log('  a dart at the landing: phase', span(r, 1), 'asleep', span(r, 2), 'z', r[-1][3], 'dart fired', th[0])
    sleep(1); x = spot(); me('sombra', x); n = add('ana', 'ana', 'red', x, landz + 2.5, 180, 0); sleep(0.2); press('ability_2', 0.1); sleep(lead + 0.6); bot('press %s ability1' % n); r = rows('me', 0.8, ['phased', 'asleep', 'z']); log('  control, a dart after the phase: phase', span(r, 1), 'asleep', span(r, 2))
    x = spot(); me('mercy', x, 0.5, 0, 25); mc('/summon minecraft:cat %s -60 2.5 {Tags:["c"],Owner:%s,Sitting:1b,OrderedToSit:1b}' % (x, UU()), 0.2); sleep(0.2); log('  killed', mc('/kill @e[tag=c]', 0.4)[-1:]); sleep(0.5); log('  gone', mc('/execute if entity @e[tag=c]', 0.2)[-1:]); press('ability_2', 0.15); sleep(0.2); log('  Resurrect, cd2', st().get('ability2Cooldown'), 'cd1', st().get('ability1Cooldown'), 'anim', st().get('anim'))
    n = add('ana', 'ana', 'red', x, 7.5, 180, 15)
    for i in range(16):
        sleep(0.25); ex = mc('/execute if entity @e[tag=c]', 0.05)[-1:]
        if ex and 'passed' in ex[0]: break
    log('  back', ex, 'phased? data:', mc('/data get entity @e[tag=c,sort=nearest,limit=1] Health', 0.2)[-1:]); bot('press %s ability1' % n); sleep(0.6); log('  a dart inside its 2.25 s: asleep', bot('asleep')); n2 = None; sleep(2.2); n2 = add('ana', 'ana', 'red', x + 0.2, 7.5, 180, 15); bot('press %s ability1' % n2); sleep(0.6); log('  a dart after it: asleep', bot('asleep'))

if want('leave'):
    log('### right after a cleanse: death, G, hero switch, leave and rejoin')
    def setup_asleep(hero='mercy'):
        x = spot(); me(hero, x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('ana', 'ana', 'red', x, 6, 180); mc('/spawnpoint @s %d -60 0' % int(x)); bot('press %s ability1' % n); sleep(0.5); log('  asleep', sh('me', 'asleep', 'held')); bot('press %s ability2' % k); sleep(0.25); log('  in the waking', sh('me', 'asleep', 'held', 'phased'), bot('held me'))
    def walks():
        s0 = st(); press('forward', 0.5); s1 = st(); return round(abs(s1['z'] - s0['z']) + abs(s1['x'] - s0['x']), 2)
    setup_asleep(); sleep(0.32); log('  phased', sh('me', 'phased', 'asleep')); mc('/kill @s'); sleep(1.2); log('  dead', st().get('health')); c('respawn'); sleep(1.5); c('close'); sleep(0.5); log('  after death', sh('me', 'asleep', 'held', 'phased', 'health'), bot('held me')['server'], st().get('hero'), 'walks', walks())
    setup_asleep(); c('heromode off'); sleep(0.7); log('  after G', sh('me', 'asleep', 'held', 'phased', 'speed'), st().get('heroMode'), 'walks', walks()); c('heromode on')
    setup_asleep(); log('  switch', c('hero kiriko')); sleep(0.8); c('close'); log('  after a hero switch', st().get('hero'), sh('me', 'asleep', 'held', 'phased')); sleep(0.7); log('  walks', walks())
    setup_asleep(); clear(); c('window 854 480'); sleep(0.5); c('press key.keyboard.escape'); sleep(0.8); log('  pause', st().get('screen')); c('hover 59 189'); c('screenclick 59 189 1'); sleep(13); log('  left', st().get('screen'))
    log(c('openworld ' + WORLD))
    for i in range(30):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    log('  back', st().get('screen'), 'hero', st().get('hero')); sleep(0.5); c('close'); sleep(1); c('window 1280 720'); sleep(0.5); log('  after leave and rejoin', sh('me', 'asleep', 'held', 'phased', 'health', 'silenced'), bot('held me'), 'walks', walks())

if want('places'):
    log('### places: asleep and woken on a ladder, in a boat, in lava (fire resistance), in shallow water; a real Kiriko in the Nether')
    def ana_for(tag, x):
        return add('ana', 'ana', 'red', x + 6, 0.5, 90, 2) if tag == 'ladder' else add('ana', 'ana', 'red', x, 6, 180, 16 if tag in ('lava', 'water') else 1)
    def wake_at(tag, setup):
        x = spot(); me('mercy', x); setup(x); sleep(0.4); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); s0 = st(); n = ana_for(tag, x); bot('press %s ability1' % n); sleep(0.6); log(' ', tag, 'y', round(s0['y'], 2), sh('me', 'asleep', 'held', 'health')); bot('press %s ability2' % k); r = rows('me', 1.0, ['asleep', 'phased', 'y']); log('    asleep', span(r, 1), 'phase', span(r, 2), 'y', r[-1][3]); c('screenshot p4_fin_place_%s.png' % tag); sleep(0.5)
    def ladder(x):
        mc('/fill %d -60 1 %d -55 1 minecraft:stone' % (x, x), 0.15); mc('/fill %d -60 0 %d -55 0 minecraft:ladder[facing=north]' % (x, x), 0.15); mc('/tp @s %s -58 0.5 0 0' % x, 0.2); c('key forward down'); sleep(0.15); c('key forward up')
    wake_at('ladder', ladder)
    def boat(x):
        mc('/summon minecraft:oak_boat %s -60 0.5 {Tags:["b"]}' % x, 0.2); mc('/tp @s %s -60 -0.8 0 45' % x, 0.1); c('look 0 45'); sleep(0.3); press('interact', 0.15); sleep(0.4); log('   riding', [l[-70:] for l in mc('/data get entity @e[tag=b,sort=nearest,limit=1] Passengers', 0.3)[-1:]])
    wake_at('boat', boat)
    def lava(x):
        mc('/effect give @s minecraft:fire_resistance 60 0', 0.1); mc('/fill %d -61 -3 %d -61 3 minecraft:lava' % (x - 3, x + 3), 0.15)
    wake_at('lava', lava); mc('/effect clear @s')
    def water(x):
        mc('/fill %d -61 -3 %d -61 3 minecraft:water' % (x - 3, x + 3), 0.15)
    wake_at('water', water)
    log('--- the Nether: poison on a real Kiriko, her own Suzu at her feet (bots live in the overworld)')
    clear(); pick('kiriko'); mc('/gamemode survival @s'); mc('/execute in minecraft:the_nether run tp @s 100 110 100', 0.3); sleep(1.5); mc('/execute in minecraft:the_nether run fill 90 109 90 110 109 110 minecraft:netherrack', 0.2); mc('/execute in minecraft:the_nether run fill 90 110 90 110 114 110 minecraft:air', 0.2); mc('/tp @s 100 110 100 0 80', 0.2); c('look 0 80'); sleep(0.5); mc('/overcraft reset'); mc('/effect give @s minecraft:poison 30 0'); mc('/effect give @s minecraft:night_vision 30 0'); log('  before', sh('me', 'effects', 'health')); press('ability_2', 0.1); sleep(0.5); log('  after', sh('me', 'effects', 'health', 'phased')); c('screenshot p4_fin_nether.png'); mc('/effect clear @s')
    mc('/execute in minecraft:overworld run tp @s 10000 -55 0', 0.5); sleep(2)

if want('sounds'):
    log('### Hack\'s and the stun\'s own sounds when cleansed: what\'s running, by name')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('som', 'sombra', 'red', x, 6, 180); bot('charge %s 9999' % n); c('heard'); bot('press %s ultimate' % n)
    t0 = time.time()
    while not cc('me')['silenced'] and time.time() - t0 < 2: sleep(0.02)
    log('  hacked: playing', bot('playing hack')['playing'], '| heard', heard('hack')); bot('press %s ability2' % k)
    for i in range(10):
        sleep(0.25); log('   +%.2f %s | %s' % (0.25 * (i + 1), sh('me', 'silenced', 'phased'), bot('playing hack')['playing']))
    sleep(1)
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 4.5, -90, 72); b = add('brig', 'brigitte', 'red', x, -1.3, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5); c('heard'); inp(b, 3, 0)
    t0 = time.time()
    while not cc('me')['asleep'] and time.time() - t0 < 2: sleep(0.02)
    inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b); log('  stunned: playing', bot('playing stun')['playing'], '| heard', heard('stun')); bot('press %s ability2' % k)
    for i in range(8):
        sleep(0.15); log('   +%.2f %s | %s' % (0.15 * (i + 1), sh('me', 'asleep', 'phased'), bot('playing stun')['playing']))
clear(); log('DONE')

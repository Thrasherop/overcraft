exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
ONLY = globals().get('ONLY')
def want(k): return ONLY is None or k in ONLY
def phase_wait(timeout=2):
    t0 = time.time()
    while not cc('me')['phased'] and time.time() - t0 < timeout: pass
    return time.time() - t0
log('=== after the critique\'s fixes. Harness keys, bots, summoned mobs: not a person.')
c('close'); sleep(1)
if 'error' in bot('cc me'):
    log(c(globals().get('OPEN', 'newworld ' + WORLD)))
    for i in range(40):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    sleep(1); c('close'); sleep(1)
setup(); mc('/gamemode survival @s'); c('window 1280 720')

if want('repro'):
    log('### the same steps as crit_repro_abde763')
    x = spot(); me('mercy', x); n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40); bot('charge %s 9999' % n)
    bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); r = rows('me', 1.8, ['asleep', 'carriedBy']); inp(n, 0, 0); on = [q for q in r if q[1]]
    first = on[0][0] - r[0][0] if on else 8; log('--- 1 control: knocked', span(r, 1), 'carried by', r[-1][2]); sleep(4)
    x = spot(); me('mercy', x); n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40); bot('charge %s 9999' % n); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72)
    bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); t0 = time.time(); th = [False]
    def ev(q):
        if not th[0] and time.time() - t0 > max(0, first / 20 - 0.42): th[0] = True; bot('press %s ability2' % k)
    c('heard'); r = rows('me', 2.5, ['phased', 'asleep', 'carriedBy', 'health'], ev); inp(n, 0, 0)
    log('--- 1 phased at the landing: phase', span(r, 1), 'knocked', span(r, 2), 'carried from', [q[0] for q in r if q[3]][:1], 'health', r[-1][4]); sleep(5)
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('luc', 'lucio', 'red', x, 3.5, 180)
    bot('press %s ability2' % k); phase_wait(); bot('secondary %s on' % n); r = rows('me', 0.5, ['phased', 'effects', 'speed', 'z', 'health']); bot('secondary %s off' % n)
    log('--- 2 Soundwave: phase', span(r, 1), 'effects', sorted(set(q[2] for q in r)), 'min speed', min(q[3] for q in r), 'z', r[0][4], r[-1][4], 'health', r[-1][5])
    x = spot(); me('sombra', x); press('ability_2', 0.1); sleep(0.5); press('ability_2', 0.1); sleep(0.7); s = st(); px, pz = s['x'], s['z']; log('--- 3 EMP: stealth', sh('me', 'stealth', 'phased'))
    k = add('kiri', 'kiriko', 'blue', px - 3, pz, -90, 72); n = add('som', 'sombra', 'red', px, pz + 6, 180); bot('charge %s 9999' % n); c('heard'); bot('press %s ultimate' % n); sleep(0.1); bot('press %s ability2' % k)
    r = rows('me', 1.5, ['phased', 'silenced', 'stealth', 'health']); log('    phase', span(r, 1), 'silenced', span(r, 2), 'stealth', span(r, 3), 'reads', r[0][0], r[-1][0], 'health', r[-1][4], 'emp.hit', heard('emp.hit')); sleep(1)
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('sig', 'sigma', 'red', x, -1.3, 0); bot('perk %s SIGMA_HYPER_STRIKE' % n); bot('strike %s' % n)
    c('heard'); bot('press %s ability2' % k); phase_wait(); inp(n, 0, 16); sleep(0.2); log('--- 4a strike, phased', sh('me', 'phased', 'asleep', 'gravity', 'health'), bot('held me'), heard('hyper_strike')); sleep(1.2)
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 3, -90, 72); n = add('sig', 'sigma', 'red', x, -1.3, 0); bot('perk %s SIGMA_HYPER_STRIKE' % n); bot('strike %s' % n)
    inp(n, 0, 16); sleep(0.1); log('--- 4b struck', sh('me', 'asleep', 'gravity'), bot('held me')); bot('press %s ability2' % k); t0 = time.time()
    for i in range(7):
        sleep(0.1); log('   +%.2f %s %s | %s' % (time.time() - t0, sh('me', 'asleep', 'gravity', 'phased'), bot('held me')['client'], bot('playing hyper_strike')['playing']))
        if i == 3: c('screenshot p4_fin_strike_after.png')
    for suzu in (False, True):
        x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 3, 3.5, -90, 72); b = add('brig', 'brigitte', 'red', x, 1.7, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5)
        g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3); bot('press %s ability2' % g); st_ = [0]
        def ev(q):
            if q['asleep'] and st_[0] == 0: st_[0] = 1; inp(b, 3, 0); st_.append(time.time())
            elif st_[0] == 1 and time.time() - st_[1] > 0.1:
                st_[0] = 2; inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b)
                if suzu: bot('press %s ability2' % k)
        r = rows(a, 3, ['asleep', 'phased', 'health'], ev); log('--- 5 Accretion, Rally, Suzu', suzu, 'held', span(r, 1), 'phase', span(r, 2), 'health', r[-1][3]); sleep(0.5)
    x = spot(); me('mercy', x + 2, 0, 180); a = add('ally', 'mercy', 'blue', x, -3, 0); k = add('kiri', 'kiriko', 'blue', x - 2.5, -5.5, 0, 72); n = add('ana', 'ana', 'red', x - 0.3, 1, 180, 18); bot('perk %s ANA_LOCAL_ANESTHETIC' % n); sleep(0.3)
    bot('press %s ability1' % n); sleep(0.6); log('--- 6 ally', sh(a, 'speed'), 'me', sh('me', 'speed'), bot('playing anesthetic')); bot('press %s ability2' % k); t0 = time.time()
    for i in range(10):
        sleep(0.15); log('   +%.2f ally %s | me %s | %s' % (time.time() - t0, sh(a, 'speed', 'phased'), sh('me', 'speed'), bot('playing anesthetic')['playing']))
    for SUZU in (False, True):
        x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x + 3, 3.5, 90, 72); b = add('brig', 'brigitte', 'red', x, 1.7, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5)
        g = add('sig', 'sigma', 'red', x, 8.5, 180, 18); bot('charge %s 9999' % g); bot('press %s ultimate' % g); sleep(1.0); inp(g, 1, 0); t0 = time.time(); stage = [0]
        def ev(q):
            if stage[0] == 0 and time.time() - t0 > 0.95: stage[0] = 1; inp(b, 3, 0)
            elif stage[0] == 1 and q['asleep']:
                stage[0] = 2; inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b)
                if SUZU: bot('press %s ability2' % k)
        r = rows(a, 2.4, ['lifted', 'asleep', 'held', 'phased'], ev); inp(g, 0, 0); log('--- 7 stun under Flux, Suzu', SUZU, 'lifted', span(r, 1), 'asleep', span(r, 2), 'held', span(r, 3), 'phase', span(r, 4))
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('ana', 'ana', 'red', x, 4, 180, 2); bot('perk %s ANA_LOCAL_ANESTHETIC' % n); c('heard'); bot('press %s ability2' % k); phase_wait(); bot('press %s ability1' % n); sleep(0.4); log('--- dart, phased', sh('me', 'asleep', 'phased', 'health'), heard('ana.')); sleep(0.4)
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); b = add('brig', 'brigitte', 'red', x, 4.5, 180); c('heard'); bot('press %s ability2' % k); bot('press %s ability1' % b); r = rows('me', 0.7, ['phased', 'z', 'health']); log('--- whip, phased: phase', span(r, 1), 'z', r[0][2], r[-1][2], 'health', r[-1][3], heard('whip'))
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); b = add('brig', 'brigitte', 'red', x, -1.3, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5); bot('press %s ability2' % k); phase_wait(); inp(b, 3, 0); sleep(0.15); inp(b, 2, 0); log('--- Rally bash, phased', sh('me', 'asleep', 'phased', 'health', 'z')); inp(b, 0, 0); bot('secondary %s off' % b)

if want('potions'):
    log('### Suzu with poison, fire and a beneficial potion on the target')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); mc('/effect clear @s')
    mc('/effect give @s minecraft:poison 30 0'); mc('/effect give @s minecraft:strength 30 0'); mc('/effect give @s minecraft:speed 30 0'); mc('/effect give @s minecraft:wither 30 0'); mc('/effect give @s minecraft:slowness 30 0'); mc('/setblock %d -60 0 minecraft:fire' % (x - 0.5), 0.3); sleep(0.7)
    log('  before', sh('me', 'effects', 'fire', 'health')); bot('press %s ability2' % k); sleep(0.5); log('  after', sh('me', 'effects', 'fire', 'health', 'phased')); mc('/setblock %d -60 0 minecraft:air' % (x - 0.5), 0.1); mc('/effect clear @s')
    a = add('ally', 'mercy', 'blue', x, 3, 0); mc('/effect give %s minecraft:poison 30 0' % a); mc('/effect give %s minecraft:regeneration 30 0' % a); log('  bot ally before', sh(a, 'effects')); sleep(1); me('kiriko', x, 0.5, 0, 62); log(' ', 'real Suzu cd', st().get('ability2Cooldown')); press('ability_2', 0.1); sleep(0.5); log('  bot ally after', sh(a, 'effects') if False else '(me() removed the bot: not read)')
    x = spot(); me('kiriko', x, 0.5, 0, 62); a = add('ally', 'mercy', 'blue', x + 1, 3.5, 0); mc('/effect give %s minecraft:poison 30 0' % a); mc('/effect give %s minecraft:regeneration 30 0' % a); mc('/effect give %s minecraft:fire_resistance 30 0' % a); log('  bot ally before', sh(a, 'effects')); press('ability_2', 0.1); sleep(0.5); log('  bot ally after', sh(a, 'effects', 'phased'))
    x = spot(); me('kiriko', x, 0.5, 0, 62); mc('/summon minecraft:wolf %s -60 3.5 {Tags:["w"],Owner:%s,Health:8f,Sitting:1b,OrderedToSit:1b,active_effects:[{id:"minecraft:poison",duration:600},{id:"minecraft:speed",duration:600}],Fire:200s}' % (x + 1, re.search(r"\[I; ?[-\d, ]+\]", ' '.join(mc('/data get entity @s UUID', 0.4))).group(0)), 0.2)
    log('  wolf before', mc('/data get entity @e[tag=w,limit=1] active_effects', 0.3)[-1:], mc('/data get entity @e[tag=w,limit=1] Fire', 0.3)[-1:]); press('ability_2', 0.1); sleep(0.5); log('  wolf after', mc('/data get entity @e[tag=w,limit=1] active_effects', 0.3)[-1:], mc('/data get entity @e[tag=w,limit=1] Fire', 0.3)[-1:])

if want('tether'):
    log('### Suzu on a tethered real client (mid-carry)')
    x = spot(); me('mercy', x); n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40); bot('charge %s 9999' % n); k = add('kiri', 'kiriko', 'blue', x - 2, 0.5, -90, 72)
    bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); sleep(0.3); inp(n, 0, 0); t0 = time.time()
    while not cc('me')['carriedBy'] and time.time() - t0 < 3: sleep(0.02)
    sleep(1.2 if False else 0.1); log('  tethered', sh('me', 'carriedBy', 'asleep', 'y')); bot('press %s ability2' % k); r = rows('me', 2.5, ['phased', 'carriedBy', 'asleep', 'y']); log('  phase', span(r, 1), 'carried', span(r, 2), 'knocked', span(r, 3), 'y end', r[-1][4]); c('screenshot p4_fin_tether.png'); sleep(2)

if want('leave'):
    log('### right after a cleanse: death, G, hero switch, leave and rejoin')
    def setup_asleep(hero='mercy'):
        x = spot(); me(hero, x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('ana', 'ana', 'red', x, 6, 180); mc('/spawnpoint @s %d -60 0' % int(x)); bot('press %s ability1' % n); sleep(0.5); log('  asleep', sh('me', 'asleep', 'held')); bot('press %s ability2' % k); sleep(0.3); log('  in the waking', sh('me', 'asleep', 'held', 'phased'), bot('held me'))
    setup_asleep(); mc('/kill @s'); sleep(1.2); c('respawn'); sleep(1.5); c('close'); sleep(0.5); log('  after death', sh('me', 'asleep', 'held', 'phased', 'health'), bot('held me')['server'], st().get('hero')); sleep(0.5); s0 = st(); press('forward', 0.5); s1 = st(); log('  walks', round(abs(s1['z'] - s0['z']) + abs(s1['x'] - s0['x']), 2))
    setup_asleep(); c('heromode off'); sleep(0.7); log('  after G', sh('me', 'asleep', 'held', 'phased', 'speed'), st().get('heroMode')); sleep(0.3); s0 = st(); press('forward', 0.5); s1 = st(); log('  walks', round(abs(s1['z'] - s0['z']) + abs(s1['x'] - s0['x']), 2)); c('heromode on')
    setup_asleep(); c('hero kiriko'); sleep(0.8); c('close'); log('  after a hero switch', st().get('hero'), sh('me', 'asleep', 'held', 'phased')); sleep(0.7); s0 = st(); press('forward', 0.5); s1 = st(); log('  walks', round(abs(s1['z'] - s0['z']) + abs(s1['x'] - s0['x']), 2))
    setup_asleep(); clear(); c('window 854 480'); sleep(0.5); c('press key.keyboard.escape'); sleep(0.8); log('  pause', st().get('screen')); c('hover 59 189'); c('screenclick 59 189 1'); sleep(13); log('  left', st().get('screen'))
    log(c('openworld ' + WORLD))
    for i in range(30):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    log('  back', st().get('screen'), 'hero', st().get('hero')); sleep(0.5); c('close'); sleep(1); c('window 1280 720'); sleep(0.5); log('  after leave and rejoin', sh('me', 'asleep', 'held', 'phased', 'health', 'silenced'), bot('held me')); s0 = st(); press('forward', 0.5); s1 = st(); log('  walks', round(abs(s1['z'] - s0['z']) + abs(s1['x'] - s0['x']), 2))

if want('grip'):
    log('### Life Grip\'s pull moving a slept ally, by tick, positions')
    for slept in (False, True):
        x = spot(); me('mercy', x, 8, 180); w = add('wea', 'lifeweaver', 'blue', x, -3, 0); n = add('ana', 'ana', 'red', x, 12.5, 180, 0)
        if slept: bot('press %s ability1' % n); sleep(0.7)
        log('  slept', slept, sh('me', 'asleep', 'z'), bot('press %s ability2' % w)); r = rows('me', 1.6, ['asleep', 'phased', 'z', 'y', 'held'])
        z0 = r[0][3]; moved = [q[0] for q in r if abs(q[3] - z0) > 0.05]; log('    asleep', span(r, 1), 'phase', span(r, 2), 'first moved tick', moved[:1], 'held', span(r, 5), 'start tick', r[0][0], 'z', z0, '->', r[-1][3]); log('    ', [(q[0], round(q[3], 2), int(bool(q[1]))) for q in r[::3]])
    x = spot(); me('kiriko', x, -14); a = add('ally', 'mercy', 'blue', x, 8, 180); w = add('wea', 'lifeweaver', 'blue', x, -3, 0); n = add('ana', 'ana', 'red', x, 12.5, 180, 0); bot('press %s ability1' % n); sleep(0.7)
    log('  a bot (a bot isn\'t moved by a client): ', sh(a, 'asleep', 'z'), bot('press %s ability2' % w)); r = rows(a, 1.6, ['asleep', 'z']); log('    asleep', span(r, 1), 'z', r[0][2], r[-1][2])

if want('mobally'):
    log('### a mob ally other than a wolf: a tamed cat; and the villager the dart passes')
    x = spot(); me('kiriko', x, -6.5, 0, 0); UU = re.search(r"\[I; ?[-\d, ]+\]", ' '.join(mc('/data get entity @s UUID', 0.4))).group(0)
    mc('/summon minecraft:cat %s -60 4.5 {Tags:["c"],Owner:%s,Health:6f,Sitting:1b,OrderedToSit:1b}' % (x, UU), 0.2); log('  side', bot('side cat')); n = add('ana', 'ana', 'red', x, 7.5, 180, 20); bot('press %s ability1' % n); sleep(0.7); log('  after a dart', bot('asleep'), 'health', hp('@e[tag=c,limit=1]')); c('screenshot p4_fin_cat_asleep.png')
    k = add('kiri', 'kiriko', 'blue', x + 3, 4.5, 90, 72); bot('press %s ability2' % k)
    for i in range(5):
        sleep(0.2); log('   +%.1f %s health %s' % (0.2 * (i + 1), bot('asleep'), hp('@e[tag=c,limit=1]')))

if want('creeper'):
    log('### a hacked creeper on the blue team, fuse cut, a Suzu, a red bot near it')
    for suzu in (False, True):
        x = spot(); me('mercy', x, -13); mc('/summon minecraft:creeper %s -60 8 {Tags:["cr"],Team:"blue",attributes:[{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); s_ = add('som', 'sombra', 'red', x + 1, 6.5, 0); bot('charge %s 9999' % s_); k = add('kiri', 'kiriko', 'blue', x - 3, 8, -90, 72)
        log('  side for Kiriko', bot('side creeper')); mc('/team join blue @e[tag=cr]', 0.2); log('  side', bot('side creeper')); sleep(0.6); bot('press %s ultimate' % s_); sleep(1.0); log('  hacked: exists', mc('/execute if entity @e[tag=cr]', 0.2)[-1:], mc('/data get entity @e[tag=cr,limit=1] ignited', 0.2)[-1:]); c('screenshot p4_fin_creeper_%s_a.png' % suzu)
        if suzu: bot('press %s ability2' % k)
        for i in range(10):
            sleep(0.35); log('   +%.2f exists %s bot %s' % (0.35 * (i + 1), mc('/execute if entity @e[tag=cr]', 0.1)[-1:][0][-20:] if mc('/execute if entity @e[tag=cr]', 0.1)[-1:] else '', sh(s_, 'health')))
            if i == 3: c('screenshot p4_fin_creeper_%s_b.png' % suzu)
        mc('/kill @e[tag=cr]')

if want('rally'):
    log('### Rally by keys: fix rally reply logged; a wall, a barrier, a phased target; distances: a 2.2 ahead, b 5.5 ahead, c 3.5 behind, helmets, midnight')
    for fix in ('off', 'on', 'off', 'on'):
        rep = bot('fix rally ' + fix); log('reply', rep); x = spot('grass_block'); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/time set midnight'); mc('/kill @e[type=!minecraft:player]'); tp(x, -60, 0.5, 0, 0)
        for tag, dz in (('a', 2.2), ('b', 5.5), ('c', -3.5)):
            mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:20f,equipment:{head:{id:"minecraft:iron_helmet",count:1}},active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (x, 0.5 + dz, tag), 0.1)
        sleep(0.3); mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); press('secondary_fire', 0.15); a1 = bot('asleep'); sleep(0.2); a2 = bot('asleep'); c('screenshot p4_fin_rally_%s_hit.png' % fix); sleep(0.9); c('key primary_fire up')
        log('fix rally', fix, 'a b c', [hp('@e[tag=%s,limit=1]' % t) for t in 'abc'], 'stunned at once', a1.get('asleep'), '0.2 s later', a2.get('asleep'))
    bot('fix rally on')
    log('--- a phased bot in the wave')
    x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0); mc('/summon minecraft:zombie %s -60 2.7 {NoAI:1b,Tags:["a"],Health:20f,active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % x, 0.1); p = add('red', 'mercy', 'red', x + 2, 6, 180); kr = add('krd', 'kiriko', 'red', x + 2, 8, 180, 72)
    mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); bot('press %s ability2' % kr); sleep(0.05); press('secondary_fire', 0.12); r = rows(p, 0.5, ['phased', 'asleep', 'health']); c('key primary_fire up'); log('  phase', span(r, 1), 'stun', span(r, 2), 'health', r[0][3], r[-1][3], 'bashed', hp('@e[tag=a,limit=1]'))

if want('places'):
    log('### places: asleep and woken on a ladder, in a boat, in lava (fire resistance), in shallow water; a real Kiriko in the Nether')
    def wake_at(tag, setup):
        x = spot(); me('mercy', x); setup(x); sleep(0.4); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('ana', 'ana', 'red', x, 6, 180, 1); s0 = st(); bot('press %s ability1' % n); sleep(0.6); log(' ', tag, 'y', round(s0['y'], 2), sh('me', 'asleep', 'held', 'health')); bot('press %s ability2' % k); r = rows('me', 1.0, ['asleep', 'phased', 'y']); log('    asleep', span(r, 1), 'phase', span(r, 2), 'y', r[-1][3]); c('screenshot p4_fin_place_%s.png' % tag); sleep(0.5)
    def ladder(x):
        mc('/fill %d -60 1 %d -55 1 minecraft:stone' % (x, x), 0.15); mc('/fill %d -60 0 %d -55 0 minecraft:ladder[facing=north]' % (x, x), 0.15); mc('/tp @s %s -58 0.5 0 0' % x, 0.2); press('forward', 0.2)
    wake_at('ladder', ladder)
    def boat(x):
        mc('/summon minecraft:oak_boat %s -60 0.5 {Tags:["b"]}' % x, 0.2); c('look 0 75'); mc('/tp @s %s -60 -0.5 0 60' % x, 0.1); c('look 0 60'); sleep(0.3); press('interact', 0.15); sleep(0.4); log('   riding', mc('/data get entity @e[tag=b,limit=1] Passengers', 0.3)[-1:][0][-60:] if mc('/data get entity @e[tag=b,limit=1] Passengers', 0.3)[-1:] else '')
    wake_at('boat', boat)
    def lava(x):
        mc('/effect give @s minecraft:fire_resistance 60 0', 0.1); mc('/fill %d -61 -3 %d -61 3 minecraft:lava' % (x - 3, x + 3), 0.15)
    wake_at('lava', lava)
    mc('/effect clear @s')
    def water(x):
        mc('/fill %d -61 -3 %d -61 3 minecraft:water' % (x - 3, x + 3), 0.15)
    wake_at('water', water)
    log('--- the Nether: poison on a real Kiriko, her own Suzu at her feet (bots live in the overworld)')
    clear(); pick('kiriko'); mc('/gamemode survival @s'); mc('/execute in minecraft:the_nether run tp @s 100 110 100', 0.3); sleep(1.5); mc('/execute in minecraft:the_nether run fill 90 109 90 110 109 110 minecraft:netherrack', 0.2); mc('/execute in minecraft:the_nether run fill 90 110 90 110 114 110 minecraft:air', 0.2); mc('/tp @s 100 110 100 0 80', 0.2); c('look 0 80'); sleep(0.5); mc('/overcraft reset'); mc('/effect give @s minecraft:poison 30 0'); mc('/effect give @s minecraft:night_vision 30 0'); log('  before', sh('me', 'effects', 'health')); press('ability_2', 0.1); sleep(0.5); log('  after', sh('me', 'effects', 'health', 'phased')); c('screenshot p4_fin_nether.png'); mc('/effect clear @s')
    mc('/execute in minecraft:overworld run tp @s 10000 -55 0', 0.5); sleep(2)

if want('sounds'):
    log('### Hack\'s and the stun\'s own sounds when cleansed: what\'s running, by name')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('som', 'sombra', 'red', x, 6, 180); bot('charge %s 9999' % n); bot('press %s ultimate' % n)
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

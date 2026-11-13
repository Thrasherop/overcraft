exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
ONLY = globals().get('ONLY')
def want(k): return ONLY is None or k in ONLY
WORLD = globals().get('WORLD', 'p4g')
c('close'); sleep(1)
if 'error' in bot('cc me'):
    log(c('newworld ' + WORLD))
    for i in range(40):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    sleep(1); c('close'); sleep(1)
setup(); mc('/gamemode survival @s'); c('window 1280 720')
log('=== the second critique, reproduced or not. Harness keys, bots with bot phase; not a person.')
def zomb(tag, x, z, hl=200):
    mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:%sf,attributes:[{id:"minecraft:max_health",base:%s}],active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (x, z, tag, hl, hl), 0.15)
def near(tag): return hp('@e[tag=%s,sort=nearest,limit=1]' % tag)
def ticks(r, i):
    return span(r, i)

if want('rock'):
    log('--- 1 Accretion at a phased bot, a zombie behind: a bot Sigma; his ult, the rock')
    for phased in (False, True):
        x = spot(); me('mercy', x, -13); t = add('tgt', 'mercy', 'blue', x, 3.5, 0); zomb('bh', x, 1.0); g = add('sig', 'sigma', 'red', x, 8.5, 180, 4); sleep(0.2); u0 = cc(g)['ult']; b0 = near('bh'); c('heard')
        if phased: bot('phase %s 3' % t)
        bot('press %s ability2' % g); r = rows(t, 1.6, ['asleep', 'health'])
        log('  phased', phased, 'target knocked', span(r, 1), 'health', r[-1][2], '| behind', b0, '->', near('bh'), '| Sigma ult', round(u0, 1), '->', round(cc(g)['ult'], 1), '| heard', heard('accretion')[:4])

if want('stack'):
    log('--- 2 stacking on a bot target, Suzu by a bot: Hyper on Accretion; dart on a knock-down; dart on a stun')
    def fresh(ally_hero='mercy'):
        x = spot(); me('mercy', x, -14); a = add('ally', ally_hero, 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 2, 3.5, -90, 72); return x, a, k
    for suzu in (False, True):
        x, a, k = fresh(); g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3); h = add('hyp', 'sigma', 'red', x, 1.7, 0); bot('perk %s SIGMA_HYPER_STRIKE' % h); bot('strike %s' % h); bot('press %s ability2' % g); st_ = [0]
        def ev(q):
            if q['asleep'] and st_[0] == 0:
                st_[0] = 1; inp(h, 0, 16)
                if suzu: bot('press %s ability2' % k)
        r = rows(a, 3, ['asleep', 'phased', 'gravity'], ev); log('  Accretion + Hyper Strike, Suzu', suzu, 'held', span(r, 1), 'phase', span(r, 2), 'gravity reads', sorted(set(round(q[3], 3) for q in r)))
    for suzu in (False, True):
        x, a, k = fresh(); g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3); n = add('ana', 'ana', 'red', x - 5, 3.5, -90, 7); bot('press %s ability2' % g); st_ = [0]
        def ev(q):
            if q['asleep'] and st_[0] == 0:
                st_[0] = 1; bot('press %s ability1' % n); st_.append(time.time())
            elif st_[0] == 1 and time.time() - st_[1] > 0.25:
                st_[0] = 2
                if suzu: bot('press %s ability2' % k)
        r = rows(a, 7, ['asleep', 'phased', 'sleeping'], ev); log('  Accretion + dart, Suzu', suzu, 'held', span(r, 1), 'sleeping', span(r, 3), 'phase', span(r, 2))
    for suzu in (False, True):
        x, a, k = fresh(); b = add('brig', 'brigitte', 'red', x, 1.7, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5); n = add('ana', 'ana', 'red', x, 8, 180, 8); inp(b, 3, 0); st_ = [0]
        def ev(q):
            if q['asleep'] and st_[0] == 0:
                st_[0] = 1; inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b); bot('press %s ability1' % n); st_.append(time.time())
            elif st_[0] == 1 and time.time() - st_[1] > 0.25:
                st_[0] = 2
                if suzu: bot('press %s ability2' % k)
        r = rows(a, 7, ['asleep', 'phased', 'sleeping'], ev); log('  Rally stun + dart, Suzu', suzu, 'held', span(r, 1), 'sleeping', span(r, 3), 'phase', span(r, 2))

if want('melee'):
    log('--- 3 quick melee: a real Cat with Claws Out at a phased bot; a real Sigma with Hyper Strike at a phased bot and a zombie in the same swing')
    def perk_first(which):
        mc('/overcraft perks @s 3', 0.15); c('key perk_menu down'); sleep(0.05)
        c('key %s down' % which); sleep(0.15); c('key %s up' % which); sleep(0.15); c('key %s down' % which); sleep(0.15); c('key %s up' % which); c('key perk_menu up'); sleep(0.2)
    for phased in (False, True):
        x = spot(); me('mercy', x); n = add('cat', 'jetpack_cat', 'red', x, -1.2, 0); bot('perk %s JETPACK_CAT_CLAWS_OUT' % n); t = add('tgt', 'mercy', 'blue', x, 0.5 + 0, 0) if False else 'me'
        if phased: bot('phase me 2')
        c('heard'); inp(n, 0, 16); sleep(0.4); r = cc(n); log('  bot Cat, Claws at real client, phased', phased, 'claws cooldown', r['claws'], sh('me', 'speed', 'health'), heard('melee')[:2]); sleep(0.8)
    for phased in (False, True):
        x = spot(); me('mercy', x, -12); t = add('tgt', 'mercy', 'blue', x, 1.0, 0); zomb('bh', x + 0.6, 1.0); g = add('sig', 'sigma', 'red', x + 0.2, -0.6, 0); bot('perk %s SIGMA_HYPER_STRIKE' % g); bot('strike %s' % g); sleep(0.2)
        mc('/team leave @e[tag=bh]', 0.05)
        if phased: bot('phase %s 3' % t)
        b0 = near('bh'); inp(g, 0, 16); sleep(0.3); log('  bot Sigma, Hyper, phased', phased, 'target', sh(t, 'health', 'gravity', 'asleep'), 'zombie', b0, '->', near('bh'), 'charge left', cc(g)['strike'], 'ult', round(cc(g)['ult'], 1))
        sleep(0.8); x2 = None
        if phased:
            inp(g, 0, 16); sleep(0.3); log('    swung again', 'zombie', near('bh'), 'charge left', cc(g)['strike'])

if want('kill'):
    log('--- 5 phased: /kill, the void, a bed blast')
    x = spot(); me('mercy', x); bot('phase me 3'); r0 = cc('me')['health']; lines_ = mc('/kill @s', 0.6); log('  /kill: chat', lines_[-1:], 'health', st()['health'], 'phased', cc('me')['phased']); sleep(3)
    if st()['health'] <= 0: c('respawn'); sleep(1.5); c('close')
    x = spot(); me('mercy', x); bot('phase me 3'); mc('/tp @s %s -200 0.5' % x, 0.1); sleep(1.2); s = st(); log('  void at y -200 for 1.2 s: health', s['health'], 'y', s['y']); sleep(2.5); s = st(); log('  later: health', s['health'], 'y', round(s['y'], 1))
    if st()['health'] <= 0: c('respawn'); sleep(1.5); c('close')
    x = spot(); me('mercy', x); mc('/execute in minecraft:the_nether run tp @s 100 110 100', 0.3); sleep(1.5); mc('/execute in minecraft:the_nether run fill 90 109 90 110 109 110 minecraft:netherrack', 0.15); mc('/execute in minecraft:the_nether run fill 90 110 90 110 114 110 minecraft:air', 0.15); mc('/tp @s 100 110 100 0 40', 0.2); mc('/effect give @s minecraft:instant_health 1 3', 0.3); mc('/setblock 100 110 102 minecraft:red_bed[part=foot,facing=south]', 0.2); mc('/setblock 100 110 103 minecraft:red_bed[part=head,facing=south]', 0.2); s0 = st()['health']; bot('phase me 3'); c('look 0 50'); sleep(0.3); press('interact', 0.15); sleep(0.5); log('  bed in the Nether, phased: health', s0, '->', st()['health'], 'block still there', mc('/execute if block 100 110 103 minecraft:red_bed', 0.2)[-1:]); sleep(3.2); mc('/execute in minecraft:the_nether run setblock 100 110 102 minecraft:red_bed[part=foot,facing=south]', 0.2); mc('/execute in minecraft:the_nether run setblock 100 110 103 minecraft:red_bed[part=head,facing=south]', 0.2); s0 = st()['health']; press('interact', 0.15); sleep(0.5); log('  control, not phased: health', s0, '->', st()['health']); sleep(0.5); c('respawn') if st()['health'] <= 0 else None; sleep(1.2); c('close'); mc('/execute in minecraft:overworld run tp @s 10000 -55 0', 0.5); sleep(2)

if want('numbers'):
    log('--- 7 the shockwave\'s numbers: real Brigitte, Rally, noon and midnight, fire resistance and not; a bot Brigitte with armour, her start read')
    for when, fr in (('noon', False), ('midnight', False), ('noon', True)):
        x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set %s' % when); tp(x, -60, 0.5, 0, 0)
        for tag, zz in (('a', 2.7), ('w', 5.5)):
            mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:20f%s}' % (x, zz, tag, ',active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]' if fr else ''), 0.1)
        d = add('bri', 'brigitte', 'red', x - 2.5, 5.5, 180); sleep(0.2); dh = cc(d)['health']; mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); press('secondary_fire', 0.15); sleep(0.05); ha = near('a'); hw = near('w'); hd = cc(d)['health']; sleep(1.3); c('key primary_fire up')
        log(' ', when, 'fire resistance', fr, 'just after: bashed', ha, 'second', hw, 'bot', dh, '->', hd, '| 1.3 s later: bashed', near('a'), 'second', near('w'), 'bot', cc(d)['health'])

if want('tags'):
    log('--- the "off 14.4": limit=1 with no sort reads any entity with the tag that\'s loaded. A stale cat tagged c at 10 health 60 m off; a fresh zombie tagged c here')
    x = spot(); me('kiriko', x); mc('/summon minecraft:cat %s -60 3 {Tags:["c"],Health:10f}' % x, 0.2); x2 = spot(); me('mercy', x2); mc('/summon minecraft:zombie %s -60 3 {NoAI:1b,Tags:["c"],Health:20f}' % x2, 0.2); sleep(0.3)
    log('  limit=1:', hp('@e[tag=c,limit=1]'), '| sort=nearest:', hp('@e[tag=c,sort=nearest,limit=1]')); log('  /kill of all non-players here, then:', mc('/kill @e[type=!minecraft:player]', 0.3)[-1:]); log('  how many left with the tag', mc('/execute if entity @e[tag=c]', 0.2)[-1:])

if want('creeper'):
    log('--- a creeper by a target it can reach, EMP or not; target logged')
    for emp in (False, True):
        mc('/gamerule minecraft:mob_griefing false', 0.05); x = spot(); me('mercy', x, -13); s_ = add('som', 'sombra', 'red', x, 6.5, 0); bot('charge %s 9999' % s_); sleep(0.2); mc('/summon minecraft:creeper %s -60 9.5 {Tags:["cr"],attributes:[{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.1)
        if emp: bot('press %s ultimate' % s_)
        t0 = time.time()
        for i in range(16):
            sleep(0.4); ex = mc('/execute if entity @e[tag=cr]', 0.05)[-1:]; sd = bot('side creeper'); log('   emp %s +%.1f %s | target %s | bot Sombra %s stealth %s' % (emp, time.time() - t0, ex[0][-18:] if ex else '?', str(sd.get('target'))[:24], sh(s_, 'health'), cc(s_)['stealth']))
            if ex and 'failed' in ex[0]: break
        mc('/kill @e[tag=cr]', 0.1)
    mc('/gamerule minecraft:mob_griefing true', 0.05)

if want('exits'):
    log('--- hold exits: death, G, hero switch, leave, from a stun, a knock-down, Flux')
    def walks():
        s0 = st(); press('forward', 0.5); s1 = st(); return round(abs(s1['z'] - s0['z']) + abs(s1['x'] - s0['x']), 2)
    def hold(kind):
        x = spot(); me('mercy', x); mc('/spawnpoint @s %d -60 0' % int(x), 0.1)
        if kind == 'stun':
            b = add('brig', 'brigitte', 'red', x, -1.3, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5); inp(b, 3, 0); t0 = time.time()
            while not cc('me')['asleep'] and time.time() - t0 < 2: sleep(0.02)
            inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b)
        elif kind == 'knockdown':
            g = add('sig', 'sigma', 'red', x, 6.5, 180, 3); bot('press %s ability2' % g); t0 = time.time()
            while not cc('me')['asleep'] and time.time() - t0 < 3: sleep(0.02)
        else:
            g = add('sig', 'sigma', 'red', x, 5.5, 180, 18); bot('charge %s 9999' % g); bot('press %s ultimate' % g); sleep(1.0); inp(g, 1, 0); t0 = time.time()
            while not cc('me')['lifted'] and time.time() - t0 < 3: sleep(0.02)
            inp(g, 0, 0)
        log('  ', kind, sh('me', 'asleep', 'lifted', 'held'))
    for kind in ('stun', 'knockdown', 'flux'):
        if SWITCH_ONLY:
            hold(kind); log('   switch', c('hero kiriko')); sleep(0.1); a = sh('me', 'asleep', 'held', 'lifted'); c('close'); sleep(1.8); log('   hero switch: hero', st().get('hero'), 'at once', a, '; on', sh('me', 'asleep', 'held', 'lifted'), 'walks', walks()); continue
        hold(kind); mc('/kill @s', 0.2); sleep(1.0); dead = st()['health']; c('respawn'); sleep(1.5); c('close'); sleep(0.5); log('   death: dead read', dead, sh('me', 'asleep', 'held', 'lifted', 'health'), 'walks', walks())
        hold(kind); c('heromode off'); sleep(0.1); a = sh('me', 'asleep', 'held', 'lifted'); sleep(1.8); log('   G: at once', a, '; 1.8 s on', sh('me', 'asleep', 'held', 'lifted'), 'walks', walks()); c('heromode on'); sleep(0.5)
        hold(kind); log('   switch', c('hero kiriko')); sleep(0.1); a = sh('me', 'asleep', 'held', 'lifted'); c('close'); sleep(1.8); log('   hero switch: hero', st().get('hero'), 'at once', a, '; on', sh('me', 'asleep', 'held', 'lifted'), 'walks', walks())
        hold(kind); clear(); c('window 854 480'); sleep(0.3); c('press key.keyboard.escape'); sleep(0.8); c('hover 59 189'); c('screenclick 59 189 1'); sleep(13); log(c('openworld ' + WORLD))
        for i in range(30):
            sleep(2)
            if st().get('screen') == 'HeroSelectScreen': break
        sleep(0.5); c('close'); sleep(1); c('window 1280 720'); sleep(0.5); log('   leave and rejoin:', sh('me', 'asleep', 'held', 'lifted', 'health'), 'walks', walks())

if want('third'):
    log('--- Anesthetic: the real client not numbed, two bots numbed, a Suzu on one: the other\'s 3p')
    x = spot(); me('mercy', x + 8, 0, 90); a = add('ally', 'mercy', 'blue', x, -3, 0); e = add('other', 'mercy', 'blue', x + 2.5, -3, 0); k = add('kiri', 'kiriko', 'blue', x - 3.5, -3, 0, 72); n = add('ana', 'ana', 'red', x + 0.2, 1, 180, 18); bot('perk %s ANA_LOCAL_ANESTHETIC' % n); sleep(0.3)
    bot('press %s ability1' % n); sleep(0.6); log('  ally', sh(a, 'speed'), 'other', sh(e, 'speed'), 'me', sh('me', 'speed'), bot('playing anesthetic')); bot('press %s ability2' % k); t0 = time.time()
    for i in range(10):
        sleep(0.2); log('   +%.2f ally %s | other %s | %s' % (time.time() - t0, sh(a, 'speed', 'phased'), sh(e, 'speed', 'phased'), bot('playing anesthetic')['playing']))

if want('grip'):
    log('--- #124: Life Grip on a tethered ally and on an ally in Flux (real client the ally; a bot Lifeweaver)')
    x = spot(); me('mercy', x); w = add('wea', 'lifeweaver', 'blue', x + 8, 0.5, 90); n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40); bot('charge %s 9999' % n); bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); sleep(0.3); inp(n, 0, 0); t0 = time.time()
    while not cc('me')['carriedBy'] and time.time() - t0 < 3: sleep(0.02)
    log('  tethered', sh('me', 'carriedBy', 'asleep', 'x', 'z')); bot('press %s ability2' % w); r = rows('me', 1.6, ['carriedBy', 'x', 'phased']); log('   gripped: carried', span(r, 1), 'x', r[0][2], '->', r[-1][2], 'phase', span(r, 3)); sleep(3)
    x = spot(); me('mercy', x); w = add('wea', 'lifeweaver', 'blue', x + 8, 0.5, 90); g = add('sig', 'sigma', 'red', x, 5.5, 180, 18); bot('charge %s 9999' % g); bot('press %s ultimate' % g); sleep(1.0); inp(g, 1, 0); t0 = time.time()
    while not cc('me')['lifted'] and time.time() - t0 < 3: sleep(0.02)
    inp(g, 0, 0); sleep(0.5); log('  lifted', sh('me', 'lifted', 'x', 'y')); bot('press %s ability2' % w); r = rows('me', 1.6, ['lifted', 'x', 'y', 'phased']); log('   gripped: lifted', span(r, 1), 'x', r[0][2], '->', r[-1][2], 'y', r[0][3], '->', r[-1][3], 'phase', span(r, 4))
clear(); log('DONE')

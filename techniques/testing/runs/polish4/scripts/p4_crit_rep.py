exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
ONLY = globals().get('ONLY')
log('=== the critique, reproduced (or not), before any fix of its')
c('close'); sleep(1)
if 'error' in bot('cc me'):
    log(c('newworld %s' % globals().get('WORLD', 'p4c')))
    for i in range(40):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    sleep(1); c('close'); sleep(1)
setup(); mc('/gamemode survival @s'); c('window 1280 720')
def want(k): return ONLY is None or k in ONLY

def cat_setup(x):
    n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40); bot('charge %s 9999' % n); return n
if want('cat'):
    log('--- 1 Catnapper landing on a phased real client')
    x = spot(); me('mercy', x); n = cat_setup(x)
    bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); t0 = time.time(); r = rows('me', 1.8, ['asleep', 'carriedBy', 'y', 'phased'])
    inp(n, 0, 0); tk = [time.time()]; on = [q for q in r if q[1]]
    log('  control, no Suzu: knocked', span(r, 1), 'carried', [q[2] for q in r][-1], 'y', r[-1][3]);
    first = on[0][0] - r[0][0] if on else 15
    log('  landing %d ticks after the first read' % first)
    sleep(4)
    x = spot(); me('mercy', x); n = cat_setup(x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72)
    bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); t0 = time.time(); thrown = [False]
    def ev(q):
        if not thrown[0] and time.time() - t0 > max(0, first / 20 - 0.42):
            thrown[0] = True; bot('press %s ability2' % k)
    c('heard'); r = rows('me', 2.5, ['phased', 'asleep', 'carriedBy', 'y', 'health'], ev); inp(n, 0, 0)
    ph = span(r, 1); log('  phase', ph, 'knocked', span(r, 2), 'carried from', [q[0] for q in r if q[3]][:1], 'by', r[-1][3], 'y at end', r[-1][4], 'health', r[-1][5])
    log('  held', bot('held me')); c('screenshot p4_rep_cat_phased.png'); sleep(5);

if want('soundwave'):
    log('--- 2 Soundwave on a phased real client')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('luc', 'lucio', 'red', x, 3.5, 180)
    bot('press %s ability2' % k); t0 = time.time()
    while not cc('me')['phased'] and time.time() - t0 < 2: pass
    bot('secondary %s on' % n); r = rows('me', 0.5, ['phased', 'effects', 'speed', 'x', 'z', 'health']); bot('secondary %s off' % n)
    log('  phase', span(r, 1), 'effects seen', sorted(set(q[2] for q in r)), 'min speed', min(q[3] for q in r), 'moved z', round(r[-1][5] - r[0][5], 2), 'health', r[-1][6])

if want('emp'):
    log('--- 3 EMP on a phased stealthed Sombra')
    x = spot(); me('sombra', x, 0.5, 0, 0)
    press('ability_2', 0.1); sleep(0.5); press('ability_2', 0.1); sleep(0.7); log('  after Translocator', sh('me', 'stealth', 'phased'), 'at', round(st()['x'], 1), round(st()['z'], 1))
    s = st(); px, pz = s['x'], s['z']
    k = add('kiri', 'kiriko', 'blue', px - 3, pz, -90, 72); n = add('som', 'sombra', 'red', px, pz + 6, 180); bot('charge %s 9999' % n)
    c('heard'); bot('press %s ultimate' % n); sleep(0.1); bot('press %s ability2' % k)
    r = rows('me', 1.5, ['phased', 'silenced', 'stealth', 'health'])
    log('  phase', span(r, 1), 'silenced', span(r, 2), 'stealth ticks', span(r, 3), 'of reads', r[0][0], r[-1][0], 'health', r[-1][4]); log('  heard emp.hit', heard('emp.hit')); sleep(1)

if want('strike'):
    log('--- 4a Hyper Strike on a phased real client')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('sig', 'sigma', 'red', x, -1.3, 0); bot('perk %s SIGMA_HYPER_STRIKE' % n); bot('strike %s' % n)
    c('heard'); bot('press %s ability2' % k); t0 = time.time()
    while not cc('me')['phased'] and time.time() - t0 < 2: pass
    inp(n, 0, 16); sleep(0.2); log('  ', sh('me', 'phased', 'asleep', 'gravity', 'health'), bot('held me')); log('  heard', heard('hyper_strike')); c('screenshot p4_rep_strike_phased.png'); sleep(1.2)
    log('--- 4b Hyper Strike then a Suzu: the look and the sound')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 3, -90, 72); n = add('sig', 'sigma', 'red', x, -1.3, 0); bot('perk %s SIGMA_HYPER_STRIKE' % n); bot('strike %s' % n)
    inp(n, 0, 16); sleep(0.1); log('  struck', sh('me', 'asleep', 'gravity'), bot('held me'), bot('playing hyper_strike')); bot('press %s ability2' % k)
    t0 = time.time()
    for i in range(10):
        sleep(0.1); log('   +%.2f %s %s %s' % (time.time() - t0, sh('me', 'asleep', 'gravity', 'phased'), bot('held me')['client'], bot('playing hyper_strike')['playing']))
        if i == 4: c('screenshot p4_rep_strike_after.png')

if want('overwrite'):
    log('--- 5 Accretion, then Rally\'s stun, then a Suzu, on a bot ally')
    for suzu in (False, True):
        x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 3, 3.5, -90, 72); b = add('brig', 'brigitte', 'red', x, 1.7, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5)
        g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3)
        bot('press %s ability2' % g); t0 = time.time(); st_ = [0]
        def ev(q):
            if q['asleep'] and st_[0] == 0:
                st_[0] = 1; inp(b, 3, 0); st_.append(time.time())
            elif st_[0] == 1 and time.time() - st_[1] > 0.1:
                st_[0] = 2; inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b)
                if suzu: bot('press %s ability2' % k)
        r = rows(a, 3, ['asleep', 'phased', 'health', 'held'], ev)
        log('  Suzu', suzu, 'held ticks', span(r, 1), 'phase', span(r, 2), 'health', r[-1][3]); sleep(0.5)

if want('anesthetic'):
    log('--- 6 two numbed, a Suzu reaches one: the other\'s sound')
    x = spot(); me('mercy', x + 2, 0, 180); a = add('ally', 'mercy', 'blue', x, -3, 0); k = add('kiri', 'kiriko', 'blue', x - 2.5, -5.5, 0, 72); n = add('ana', 'ana', 'red', x - 0.3, 1, 180, 18) ; bot('perk %s ANA_LOCAL_ANESTHETIC' % n); sleep(0.3)
    bot('press %s ability1' % n); sleep(0.6); log('  ally', sh(a, 'speed', 'asleep'), 'me', sh('me', 'speed', 'asleep'), 'held ally', bot('held ' + a)['client'], 'me', bot('held me')['client'])
    log('  playing', bot('playing anesthetic')); bot('press %s ability2' % k)
    for i in range(6):
        sleep(0.15); log('   ally %s | me %s | %s' % (sh(a, 'speed', 'phased'), sh('me', 'speed', 'phased'), bot('playing anesthetic')['playing']))

if want('flux'):
  for SUZU, BASH_AT in ((False, 0.95), (True, 0.95), (True, 0.85), (False, 0.85)):
    log('--- 7 stunned while Flux holds, a Suzu: the held attachment on a bot ally')
    x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x + 3, 3.5, 90, 72); b = add('brig', 'brigitte', 'red', x, 1.7, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5)
    g = add('sig', 'sigma', 'red', x, 8.5, 180, 18); bot('charge %s 9999' % g); bot('press %s ultimate' % g); sleep(1.0); inp(g, 1, 0); t0 = time.time(); stage = [0]
    def ev(q):
        tt = time.time() - t0
        if stage[0] == 0 and tt > BASH_AT:
            stage[0] = 1; inp(b, 3, 0)
        elif stage[0] == 1 and q['asleep']:
            stage[0] = 2; inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b)
            if SUZU: bot('press %s ability2' % k)
    r = rows(a, 2.4, ['lifted', 'asleep', 'held', 'phased'], ev); inp(g, 0, 0)
    log('  Suzu', SUZU, 'bash at', BASH_AT, 'lifted', span(r, 1), 'asleep', span(r, 2), 'held', span(r, 3), 'phase', span(r, 4))
if want('smaller'):
    log('--- dart at a phased real client: spent or passes?')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); n = add('ana', 'ana', 'red', x, 4, 180, 2); bot('perk %s ANA_LOCAL_ANESTHETIC' % n)
    c('heard'); bot('press %s ability2' % k); t0 = time.time()
    while not cc('me')['phased'] and time.time() - t0 < 2: pass
    bot('press %s ability1' % n); sleep(0.4); log('  ', sh('me', 'asleep', 'phased', 'health'), 'heard', heard('ana.')); sleep(0.5)
    log('--- bash at a phased real client: does it stop on her?')
    x = spot(); me('mercy', x); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); b = add('brig', 'brigitte', 'red', x, -1.3, 0); bot('secondary %s on' % b); sleep(0.5)
    c('heard'); bot('press %s ability2' % k); t0 = time.time()
    while not cc('me')['phased'] and time.time() - t0 < 2: pass
    inp(b, 3, 0); sleep(0.15); inp(b, 2, 0); log('  ', sh('me', 'asleep', 'phased', 'health', 'z'), 'heard', heard('bash')); inp(b, 0, 0); sleep(0.5)
    log('--- Rally by keys: a zombie behind a wall, a bot behind her raised barrier')
    x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(x, -60, 0.5, 0, 0)
    mc('/fill %d -60 3 %d -56 3 minecraft:stone' % (x + 2, x + 6), 0.2)
    for tag, xz in (('a', (x, 2.7)), ('w', (x + 4, 5.5))):
        mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:20f}' % (xz[0], xz[1], tag), 0.1)
    d = add('bri', 'brigitte', 'red', x - 2.5, 5.5, 180); bot('secondary %s on' % d); sleep(0.6)
    mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); press('secondary_fire', 0.15); r1 = bot('asleep'); sleep(0.25); log('  asleep', r1, bot('asleep')); sleep(0.5); c('key primary_fire up')
    log('  bashed', hp('@e[tag=a,limit=1]'), 'behind the wall', hp('@e[tag=w,limit=1]'), 'bot behind her barrier', sh(d, 'health'), bot('read ' + d).get('barrierHealth')); c('screenshot p4_rep_rally_wall.png')
clear(); log('DONE')

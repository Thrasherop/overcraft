# #124 / #125 play. Real client = the ally (or Brigitte); bots (server-side players) for the others. Harness keys, not a person.
import json, socket, sys, time, os
OUT = open(sys.argv[1] if len(sys.argv) > 1 else 'grip124.txt', 'a', encoding='utf8')
ONLY = sys.argv[2].split(',') if len(sys.argv) > 2 else None
MODES = sys.argv[3].split(',') if len(sys.argv) > 3 else ['off', 'on']
def log(*a):
    s = ' '.join(str(x) for x in a); print(s); OUT.write(s + '\n'); OUT.flush()
sock = socket.create_connection(('127.0.0.1', 25599), timeout=40); F = sock.makefile('r', encoding='utf8')
def c(cmd):
    sock.sendall((cmd + '\n').encode('utf8')); return json.loads(F.readline())
def mc(cmd, w=0.1):
    r = c('cmd ' + cmd); time.sleep(w); return r
def bot(cmd): return c('bot ' + cmd)
def st(): return c('state')
sleep = time.sleep
def want(k): return ONLY is None or k in ONLY
NN = [int(time.time()) % 10000]; BOTS = []
def add(b, hero, team, x, z, yaw, pitch=0, y=-60):
    NN[0] += 1; n = '%s%d' % (b, NN[0]); r = bot('add %s %s %s %s %s %s %s' % (n, hero, team, x, y, z, yaw))
    if 'error' in str(r): log('ADDERR', r)
    mc('/gamemode survival ' + n, 0.12); bot('look %s %s %s' % (n, yaw, pitch)); BOTS.append(n); sleep(0.25); return n
def clear():
    for n in BOTS: bot('remove ' + n)
    BOTS.clear()
XX = [20000]
def spot(r=15):
    XX[0] += 60; x = XX[0]
    mc('/tp @s %d -55 0' % x, 0.1); sleep(0.7)
    mc('/fill %d -60 %d %d -45 %d minecraft:air' % (x - r, -r, x + r, r), 0.15); mc('/fill %d -61 %d %d -61 %d minecraft:stone' % (x - r, -r, x + r, r), 0.15)
    return x + 0.5
def pick(h):
    mc('/kill @s'); sleep(0.5); c('respawn'); sleep(1.5); mc('/spawnpoint @s 0 -60 0')
    c('hero ' + h); sleep(0.8); c('close'); c('heromode on'); sleep(0.5)
    for _i in range(10):
        if not st().get('screen'): break
        c('close'); sleep(0.4)
def me(hero, x, z=0.5, yaw=0, pitch=0):
    clear(); pick(hero); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/tp @s %s -60 %s %s %s' % (x, z, yaw, pitch)); c('look %s %s' % (yaw, pitch)); sleep(0.3)
def cc(n='me'): return bot('cc ' + n)
def sh(n, *keys):
    r = cc(n); return ' '.join('%s=%s' % (k, round(r[k], 2) if isinstance(r.get(k), float) else r.get(k)) for k in keys)
def rows(n, secs, keys):
    out = []; t0 = time.time()
    while time.time() - t0 < secs:
        r = cc(n); out.append(tuple([r['now']] + [r.get(k) for k in keys])); sleep(0.03)
    return out
def span(rs, i):
    on = [q[0] for q in rs if q[i]]
    return (on[0] - rs[0][0], on[-1] - rs[0][0], len(on)) if on else None
def inp(n, held, pressed, extra=0): return bot('input %s %s %s %s' % (n, held, pressed, extra))
def shot(name): c('screenshot %s' % name)
AB=[0]
def absb(who):
    AB[0]+=1; mc('/data get entity %s AbsorptionAmount' % who, 0.3); shot('g125_abs_%d.png' % AB[0]); return 'see g125_abs_%d.png' % AB[0]

log('=== grip124', time.strftime('%H:%M:%S'), 'ONLY', ONLY, 'MODES', MODES)
mc('/team add red'); mc('/team add blue'); mc('/team join blue @s'); mc('/gamerule minecraft:spawn_monsters false'); mc('/time set noon')
for mode in MODES:
    log('##### bot fix 124 %s' % mode, bot('fix 124 %s' % mode))
    if want('tether'):
        log('--- Catnapper carries the ally (real client, Mercy, blue); a bot Lifeweaver grips them')
        x = spot(); me('mercy', x); w = add('wea', 'lifeweaver', 'blue', x + 8, 0.5, 90); n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40)
        bot('charge %s 9999' % n); bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); sleep(0.3); inp(n, 0, 0); t0 = time.time()
        while not cc('me')['carriedBy'] and time.time() - t0 < 3: sleep(0.02)
        log('  tethered', sh('me', 'carriedBy', 'asleep', 'x', 'y', 'z')); bot('press %s ability2' % w)
        r = rows('me', 1.8, ['carriedBy', 'x', 'y', 'phased', 'asleep']); shot('g124_tether_%s.png' % mode)
        log('  gripped: carriedBy reads', span(r, 1), 'x %.2f -> %.2f (weaver at %.1f), y %.2f -> %.2f' % (r[0][2], r[-1][2], x + 8, r[0][3], r[-1][3]), 'phase', span(r, 4), 'knocked', span(r, 5))
        log('  cat after', sh(n, 'ult'), 'its playing', bot('playing ' + n)); sleep(3)
    if want('flux'):
        log('--- Gravitic Flux lifts the ally; a bot Lifeweaver grips them')
        x = spot(); me('mercy', x); w = add('wea', 'lifeweaver', 'blue', x + 8, 0.5, 90); g = add('sig', 'sigma', 'red', x, 5.5, 180, 18)
        bot('charge %s 9999' % g); bot('press %s ultimate' % g); sleep(1.0); inp(g, 1, 0); t0 = time.time()
        while not cc('me')['lifted'] and time.time() - t0 < 3: sleep(0.02)
        inp(g, 0, 0); sleep(0.5); h0 = cc('me')['health']; log('  lifted', sh('me', 'lifted', 'x', 'y', 'speed', 'health')); bot('press %s ability2' % w)
        r = rows('me', 1.8, ['lifted', 'x', 'y', 'phased', 'speed']); shot('g124_flux_%s.png' % mode); sleep(1.5)
        log('  gripped: lifted reads', span(r, 1), 'x %.2f -> %.2f, y %.2f -> %.2f' % (r[0][2], r[-1][2], r[0][3], r[-1][3]), 'phase', span(r, 4), 'speed min', min(q[5] for q in r))
        log('  after 1.5 s more', sh('me', 'lifted', 'speed', 'health'), 'health at the grip', round(h0, 2)); sleep(2)
    if want('trap'):
        log('--- Steel Trap roots the ally; a bot Lifeweaver grips them')
        x = spot(); me('mercy', x, 9); w = add('wea', 'lifeweaver', 'blue', x + 8, 3.5, 90); jr = add('jun', 'junkrat', 'red', x, 7, 0, 60)
        mc('/kill @e[type=overcraft:steel_trap]'); bot('press %s ability2' % jr); sleep(1.6); bot('move %s %s -60 14' % (jr, x))
        e = [q for q in c('ents SteelTrap')['ents'].split(' | ') if q.strip()][-1].split(); log('  trap at', e[1:4])
        bot('move %s %s -60 %s' % (w, float(e[1]) + 8, e[3])); bot('look %s 90 0' % w)
        mc('/tp @s %s -60 %s 0 0' % (e[1], e[3])); t0 = time.time()
        while not cc('me')['rooted'] and time.time() - t0 < 2: sleep(0.02)
        log('  rooted after %.2f s' % (time.time() - t0), sh('me', 'rooted', 'speed', 'x', 'z', 'health')); bot('press %s ability2' % w)
        r = rows('me', 1.8, ['rooted', 'x', 'z', 'phased', 'speed']); shot('g124_trap_%s.png' % mode)
        log('  gripped: rooted reads', span(r, 1), 'x %.2f -> %.2f, z %.2f -> %.2f' % (r[0][2], r[-1][2], r[0][3], r[-1][3]), 'phase', span(r, 4), 'speed end', r[-1][5])
        c('key forward down'); sleep(0.5); c('key forward up'); log('  W held 0.5 s after:', sh('me', 'rooted', 'speed', 'x', 'z')); sleep(3)
    if want('strike'):
        log('--- Hyper Strike floats the ally; a bot Lifeweaver grips them')
        x = spot(); me('mercy', x, 1.0); w = add('wea', 'lifeweaver', 'blue', x + 8, 1.0, 90); g = add('sig', 'sigma', 'red', x + 0.2, -0.5, 0)
        bot('perk %s SIGMA_HYPER_STRIKE' % g); bot('strike %s' % g); sleep(0.2); inp(g, 0, 16); sleep(0.15)
        log('  struck', sh('me', 'asleep', 'gravity', 'y')); bot('press %s ability2' % w)
        r = rows('me', 1.6, ['asleep', 'gravity', 'x', 'y', 'phased']); shot('g124_strike_%s.png' % mode)
        log('  gripped: held reads', span(r, 1), 'gravity', sorted(set(round(q[2], 4) for q in r)), 'x %.2f -> %.2f, y %.2f -> %.2f' % (r[0][3], r[-1][3], r[0][4], r[-1][4])); sleep(2)
    if want('accretion'):
        log('--- Accretion knocks the ally down; a bot Lifeweaver grips them (the knock-down should stay)')
        x = spot(); me('mercy', x); w = add('wea', 'lifeweaver', 'blue', x + 8, 0.5, 90); g = add('sig', 'sigma', 'red', x, 8.5, 180, 4)
        bot('press %s ability2' % g); t0 = time.time()
        while not cc('me')['asleep'] and time.time() - t0 < 3: sleep(0.02)
        log('  knocked after %.2f s' % (time.time() - t0), sh('me', 'asleep', 'x', 'z')); bot('press %s ability2' % w)
        r = rows('me', 1.8, ['asleep', 'x', 'z', 'phased']); shot('g124_accretion_%s.png' % mode)
        log('  gripped: knocked reads', span(r, 1), 'x %.2f -> %.2f' % (r[0][2], r[-1][2]), 'phase', span(r, 4)); sleep(2)
    if want('rally'):
        log('--- #125 Rally: real client Brigitte, real Q (harness key), a bot Mercy ally 3 m off; bot fixoff', bot('fixoff ' + ('on' if mode == 'off' else 'off')))
        x = spot(); me('brigitte', x); a = add('ally', 'mercy', 'blue', x + 3, 0.5, 0)
        mc('/overcraft ult @s 100', 0.3); s = st(); log('  before: hp %s / %s shield %s ult %s' % (s['health'], s['maxHealth'], s.get('shield'), s.get('ult')), absb('@s'))
        c('key ultimate down'); sleep(0.1); c('key ultimate up'); sleep(1.5)
        s = st(); log('  +1.6 s: hp %s / %s shield %s' % (s['health'], s['maxHealth'], s.get('shield')), 'her', absb('@s'), 'ally', absb(a))
        shot('g125_rally_1.png'); sleep(3)
        s = st(); log('  +4.6 s: hp %s / %s shield %s' % (s['health'], s['maxHealth'], s.get('shield')), 'her', absb('@s'), 'ally', absb(a))
        mc('/damage @s 6 minecraft:generic', 0.4); s = st(); log('  after 75 damage: hp %s / %s shield %s' % (s['health'], s['maxHealth'], s.get('shield')))
        shot('g125_rally_2.png'); sleep(6.5)
        s = st(); log('  after Rally: hp %s / %s shield %s' % (s['health'], s['maxHealth'], s.get('shield')), 'her', absb('@s'), 'ally', absb(a))
    if want('lifeline'):
        log('--- a blue cat Lifelines the ally (Interact); a bot Lifeweaver grips them')
        x = spot(); me('mercy', x); w = add('wea', 'lifeweaver', 'blue', x + 8, 0.5, 90); n = add('cat', 'jetpack_cat', 'blue', x - 1, 0.5, 0)
        bot('ability1 %s' % n); sleep(0.4); c('key use down'); sleep(0.2); c('key use up'); sleep(0.4)
        log('  towed?', bot('read ' + n) if False else c('devtowed')); bot('press %s ability2' % w)
        r = rows('me', 1.2, ['x', 'phased']); log('  gripped: x %.2f -> %.2f' % (r[0][1], r[-1][1]), 'towed now', c('devtowed')); sleep(2)
clear(); log('=== done', time.strftime('%H:%M:%S'))

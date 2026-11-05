# #121 and Whip Shot's 8 m. Harness keys and a real client; bots are server-side, not second clients. ONLY: voice, whip, walls, slopes, second.
XSTART = globals().get('XSTART', 300000)
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
ONLY = globals().get('ONLY')
ROUNDS = globals().get('ROUNDS', 2)
def want(k): return ONLY is None or k in ONLY
log('TREE (git at launch):', open(r'E:/claude/mods/overcraft_polish4/run/p4_launch_tree.txt').read().strip(), time.strftime('%H:%M:%S'))
setup(); mc('/gamemode survival @s'); mc('/time set midnight'); mc('/gamerule minecraft:natural_health_regeneration false')
def watch(a, secs=1.8):
    """After the press: when the ally's phase starts and when the line is heard, reading every 50 ms or so."""
    t0 = time.time(); ph = line = None; first = None
    while time.time() - t0 < secs:
        if a and ph is None and cc(a).get('phased'): ph = time.time() - t0
        if line is None and heard('voice.suzu'): line = time.time() - t0
        sleep(0.03)
    return ph, line
def voice_case(name, status, thrower='me', after_press=None):
    """A real Kiriko (or a bot's) throws at a bot ally (or herself: 'me'): is her line heard, and when after the burst?"""
    x = spot(); clear()
    if thrower == 'me':
        me('kiriko', x, 0.5, 0, 0); mc('/time set midnight'); a = None
        if name != 'nobody':
            a = add('ally', 'mercy', 'blue', x + 1, 3.5, 0); c('look 0 62')
        else:
            c('look 0 75')
    else:
        me('mercy', x, 0.5, 0, 0); mc('/time set midnight'); a = 'me'; k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72)
    state = status(x, a) if status else ''
    sleep(0.2); c('heard')
    if thrower == 'me':
        press('ability_2', 0.1)
    else:
        bot('press %s ability2' % k)
    if after_press: after_press()
    ph, line = watch(a)
    log('  %-26s thrower %-4s status before: %s | phase read at +%s, line at +%s, line after burst %s | after: %s' % (name, thrower, state, None if ph is None else round(ph, 2), None if line is None else round(line, 2), None if ph is None or line is None else round(line - ph, 2), sh(a, 'phased', 'health', 'asleep', 'antiHeal', 'silenced', 'speed') if a else ''))
    sleep(9.5)
def s_sleep(x, a):
    n = add('ana', 'ana', 'red', x + 1 if a != 'me' else x, (3.5 if a != 'me' else 0.5) + 7, 180); bot('press %s ability1' % n); sleep(0.7); return sh(a, 'asleep')
def s_grenade(x, a):
    n = add('ana', 'ana', 'red', x + 1, 3.5 + 6, 180, 8); bot('press %s ability2' % n); sleep(1.0); return sh(a, 'antiHeal')
def s_hack(x, a):
    mc('/fill %d -60 5 %d -56 5 minecraft:stone' % (x - 6, x + 6), 0.2); mc('/tp @s %s -60 -11 0 62' % x, 0.1); bot('remove ' + a); a2 = add('ally', 'mercy', 'blue', x + 1, 3.5, 0); assert a2 == a or True
    c('look -2 -11' if False else 'look 0 0'); return 'unused'
def s_claws(x, a):
    n = add('cat', 'jetpack_cat', 'red', x + 1, 3.5 - 1.6, 0); bot('perk %s JETPACK_CAT_CLAWS_OUT' % n); sleep(0.2); inp(n, 0, 16); sleep(0.25); return sh(a, 'speed', 'health')
def s_numbed(x, a):
    # numbed only: the dart's own target asleep 2 m off, then gone, so the Suzu meets the numbness alone
    v = add('vic', 'mercy', 'blue', x + 3, 3.5, 0); n = add('ana', 'ana', 'red', x + 3, 3.5 + 7, 180); bot('perk %s ANA_LOCAL_ANESTHETIC' % n); bot('press %s ability1' % n); sleep(0.7); bot('remove ' + v); sleep(0.1); return sh(a, 'speed', 'asleep', 'health')
def s_poison(x, a): mc('/effect give @e[type=player,name=%s] minecraft:poison 20 0' % a, 0.1); return 'poison given'
def s_hurt(x, a): bot('set %s 6 0' % a); return sh(a, 'health')
def s_accretion(x, a):
    n = add('sig', 'sigma', 'red', x + 7, 3.5, 90, 3); bot('press %s ability2' % n); t0 = time.time()
    while not cc(a).get('asleep') and time.time() - t0 < 2: sleep(0.02)
    return sh(a, 'asleep')
def hack_row():
    x = spot(); clear(); mc('/fill %d -60 5 %d -56 5 minecraft:stone' % (x - 6, x + 6), 0.2)
    pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(x, -60, -11, 0, -2); c('look 0 -2')
    a = add('ally', 'mercy', 'blue', x + 1, 3.5, 0); s = add('som', 'sombra', 'red', x + 10, 3.5, 90); sleep(0.2); bot('charge %s 9999' % s); bot('press %s ultimate' % s); sleep(0.9)
    log('  hack (EMP, real Kiriko past it): before', sh(a, 'silenced', 'glowing')); c('heard'); press('ability_2', 0.1); ph, line = watch(a); log('  hack: phase read +%s, line +%s, line after burst %s | after %s' % (ph, line, None if not ph or not line else round(line - ph, 2), sh(a, 'silenced', 'glowing', 'phased'))); sleep(9.5)
def stun_row(late=False):
    x = spot(); clear(); pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(x, -60, 0.5, 0, 62); a = add('ally', 'mercy', 'blue', x + 1, 3.5, 0); at = (x + 1, 3.5)
    b = add('brig', 'brigitte', 'red', at[0], at[1] - 1.8, 0); sleep(0.3); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.6)
    mc('/tp @s %s -60 0 -63 10' % (x - 6)); c('look -63 10'); sleep(0.25); inp(b, 3, 0); t0 = time.time()
    while not cc(a).get('asleep') and time.time() - t0 < 2: sleep(0.01)
    # the throw first: each request to the client takes 0.1 s or so, and the stun is 0.75 s (several reads first and it's over, and rightly no line: late row below)
    if late: sleep(0.9)
    c('heard'); s0 = sh(a, 'asleep'); press('ability_2', 0.1); ph, line = watch(a); inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b); log('  stun (Rally\'s bash)%s: read just before the throw %s; phase read +%s, line +%s, line after burst %s | after %s' % (', thrown after it ended' if late else '', s0, ph, line, None if not ph or not line else round(line - ph, 2), sh(a, 'asleep', 'phased'))); sleep(9.5)
if want('voice2'):
    log('--- the two rows again, with everything heard printed')
    for i in range(2):
        stun_row(); stun_row(True)
if want('voice'):
    log('### Suzu line, new build: each case, then 9.5 s for her 10 s limit. Expected: none on nobody, healthy, hurt, poison, numbed, knock-down; the line 0.5 s after the burst on sleep, stun, hack, anti-heal, claws')
    for rnd in range(ROUNDS):
        log('--- round', rnd)
        voice_case('nobody', None); voice_case('healthy', None); voice_case('hurt', s_hurt); voice_case('sleep', s_sleep); voice_case('anti-heal', s_grenade); voice_case('claws', s_claws); voice_case('numbed', s_numbed); voice_case('poison', s_poison); voice_case('accretion knock-down', s_accretion); hack_row(); stun_row(); stun_row(True)
    log('--- a bot Kiriko cleanses the real client asleep: the line from her, heard here')
    voice_case('bot Kiriko, me asleep', s_sleep, 'bot')
    log('--- the 10 s limit: a cleanse, a second one 3 s later (cooldown reset), a third 11 s after that')
    x = spot(); me('kiriko', x, 0.5, 0, 62)
    for gap, label in ((3, 'first'), (11, '3 s later'), (0, '14 s later')):
        a = add('ally%s' % gap, 'mercy', 'blue', x + 1, 3.5, 0); n = add('ana%s' % gap, 'ana', 'red', x + 1, 3.5 + 7, 180); bot('press %s ability1' % n); sleep(0.7); mc('/overcraft reset'); c('heard'); press('ability_2', 0.1); ph, line = watch(a); log('  %s: asleep before, phase +%s, line +%s' % (label, ph, line)); bot('remove ' + a); bot('remove ' + n); sleep(max(0, gap - 1.8))
    log('--- she switches hero inside the 0.5 s: no line'); x = spot(); me('kiriko', x, 0.5, 0, 62); sleep(9); a = add('ally', 'mercy', 'blue', x + 1, 3.5, 0); n = add('ana', 'ana', 'red', x + 1, 10.5, 180); bot('press %s ability1' % n); sleep(0.7); c('heard'); press('ability_2', 0.1); t0 = time.time()
    while not cc(a).get('phased') and time.time() - t0 < 1.5: sleep(0.02)
    pick('mercy'); ph, line = watch(None, 1.0); log('  switched: line', line)
def z_of(tag):
    l = mc('/data get entity @e[tag=%s,sort=nearest,limit=1] Pos' % tag, 0.2)[-1:]; m = re.search(r'\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]', l[0]) if l else None; return (float(m.group(2)), float(m.group(3))) if m else (None, None)
def hero_run(label, mode='on', build=None, perk=False, y=-60, during=None, n=3, wait=2.2, zz=0.5):
    """A bot Brigitte 5 m behind a real client, whipping. build(x) sets the ground first."""
    for i in range(n):
        x = spot(); clear()
        if build: build(x)
        pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, y, zz, 180, 0); c('look 180 0'); c('heromode ' + mode); sleep(0.3)
        b = add('brig', 'brigitte', 'red', x, zz - 5, 0, 0, y)
        if perk: bot('perk %s BRIGITTE_WHIPLASH' % b)
        sleep(0.2); s0 = st(); c('trace start'); bot('press %s ability1' % b)
        if during: during(b)
        sleep(wait); tr = c('trace').get('trace', ''); s1 = st(); lines = tr.splitlines()
        speeds = [l.split()[1] for l in lines[:18] if len(l.split()) > 4]; ys = [float(l.split()[4]) for l in lines if len(l.split()) > 4]
        log('  %s, hero mode %s: moved %.2f m (z %.2f -> %.2f), y %.2f -> %.2f top %.3f, health %.1f -> %.1f, ground %s' % (label, mode, abs(s1['z'] - s0['z']), s0['z'], s1['z'], s0['y'], s1['y'], max(ys) if ys else -999, s0['health'], s1['health'], s1.get('onGround')) + (' | speeds ' + ' '.join(speeds) if i == 0 else ''))
    c('heromode on')
def husk_run(label, build=None, y=-60, perk=False, n=3, zz=0.5, kind='husk'):
    for i in range(n):
        x = spot(); clear()
        if build: build(x)
        if perk:
            pick('mercy'); mc('/gamemode creative @s'); tp(x + 8, y, zz, 0, 0)
        else:
            pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, y, zz - 5, 0, 0); c('look 0 0')
        mc('/summon minecraft:%s %s %d %s {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % (kind, x, y, zz), 0.2); sleep(0.2)
        y0, z0 = z_of('hk')
        if perk:
            b = add('brig', 'brigitte', 'red', x, zz - 5, 0, 0, y); bot('perk %s BRIGITTE_WHIPLASH' % b); sleep(0.2); bot('press %s ability1' % b)
        else:
            press('ability_1', 0.1)
        sleep(1.8); y1, z1 = z_of('hk'); log('  %s %s: moved %.2f m (z %.2f -> %.2f), y %.2f -> %.2f, health %s' % (label, kind, abs(z1 - z0), z0, z1, y0, y1, hp('@e[tag=hk,sort=nearest,limit=1]'))); mc('/kill @e[tag=hk]', 0.1)
        if perk: mc('/gamemode survival @s')
def wall(zw):
    def b(x): mc('/fill %d -60 %d %d -56 %d minecraft:stone' % (x - 6, zw, x + 6, zw), 0.2)
    return b
def stairs_at(z0):
    def b(x):
        for i in range(6):
            mc('/fill %d %d %d %d %d %d minecraft:oak_stairs[facing=south]' % (x - 4, -60 + i, z0 + i, x + 4, -60 + i, z0 + i), 0.05)
            if i: mc('/fill %d -60 %d %d %d %d minecraft:stone' % (x - 4, z0 + i, x + 4, -61 + i, z0 + i), 0.05)
    return b
stairs_up = stairs_at(3)
def stairs_down(x):
    mc('/fill %d -60 -8 %d -56 3 minecraft:stone' % (x - 4, x + 4), 0.1)
    for i in range(5):
        mc('/fill %d %d %d %d %d %d minecraft:oak_stairs[facing=north]' % (x - 4, -56 - i, 4 + i, x + 4, -56 - i, 4 + i), 0.05)
        if i < 4: mc('/fill %d -60 %d %d %d %d minecraft:stone' % (x - 4, 4 + i, x + 4, -57 - i, 4 + i), 0.05)
def ledge(x): mc('/fill %d -60 -8 %d -58 3 minecraft:stone' % (x - 4, x + 4), 0.2)
if want('whip'):
    log('### flat ground: W "~8 meter"; a bot Brigitte 5 m from a real client, a real Brigitte (keys) 5 m from a husk and a zombie')
    hero_run('flat'); hero_run('flat', 'off'); husk_run('flat'); husk_run('flat', kind='zombie', n=1)
if want('walls'):
    log('### a wall 3 m behind: the hold stops, nothing more than the whip\'s damage; with the perk, the slam')
    hero_run('wall'  , build=wall(3)); hero_run('wall', 'off', build=wall(3)); husk_run('wall', build=wall(4))
    hero_run('wall + Whiplash', build=wall(3), perk=True); hero_run('no wall + Whiplash', perk=True); hero_run('wall at 7 m + Whiplash', build=wall(7), perk=True); hero_run('wall at 12 m + Whiplash', build=wall(12), perk=True, n=1); husk_run('wall + Whiplash', build=wall(4), perk=True); husk_run('no wall + Whiplash', perk=True, n=1)
if want('slopes'):
    log('### slopes and ledges')
    hero_run('stairs up from 6.2 m', build=stairs_at(7)); hero_run('stairs up from 2.2 m', build=stairs_up); hero_run('stairs up', 'off', build=stairs_up, n=1); husk_run('stairs up', build=stairs_up)
    hero_run('stairs down', build=stairs_down, y=-55); husk_run('stairs down', build=stairs_down, y=-55, n=1)
    hero_run('off a 3 m ledge', build=ledge, y=-57); hero_run('off a 3 m ledge', 'off', build=ledge, y=-57, n=1); husk_run('off a 3 m ledge', build=ledge, y=-57)
if want('second') or want('second2'):
    log('### a second push and other things in the hold')
    def second_bot(b):
        n = add('brig2', 'brigitte', 'red', st()['x'] + 2, st()['z'] - 5, 21.8); bot('press %s ability1' % n)
    if want('second'): hero_run('a second Brigitte whips just after', during=second_bot)
    def again(b):
        sleep(4.6); bot('press %s ability1' % b)
    if want('second'): hero_run('her own second whip, 4.6 s on (the same bot, not moved: it may miss at range)', during=again, n=2, wait=2.2)
    def phase_in(b):
        t0 = time.time(); h0 = st()['health']
        while st()['health'] >= h0 and time.time() - t0 < 1: sleep(0.01)
        bot('phase me 1')
    if want('second'): hero_run('phased as it hits', during=phase_in)
    def jump_in(b): c('key jump down'); sleep(0.1); c('key jump up')
    hero_run('jumping as it comes', during=jump_in)
    def walk_back(b):
        c('key forward down'); sleep(1.0); c('key forward up')
    hero_run('running at her (faces her, W held)', during=walk_back)
    def dart_in(b):
        t0 = time.time(); h0 = st()['health']; n = add('ana', 'ana', 'red', st()['x'], st()['z'] + 6, 180); bot('press %s ability1' % n)
    hero_run('a dart from the other side in the hold', during=dart_in, wait=3.0)
    def die_in(b):
        t0 = time.time(); h0 = st()['health']
        while st()['health'] >= h0 and time.time() - t0 < 1: sleep(0.01)
        mc('/kill @s', 0.1); sleep(1.0); c('respawn'); sleep(1.2); c('close')
    hero_run('dies in the hold, respawns', during=die_in, n=1)
    s = st(); log('  after the respawn: x %.1f z %.1f health %s' % (s['x'], s['z'], s['health']))
if want('overlap'):
    log('### two Brigittes in line whip 0.1 s or so apart: the second lands inside the first\'s hold')
    B2 = [None]
    def line2(x): B2[0] = add('brig2', 'brigitte', 'red', x, -5.5, 0, 0)
    def second(b): bot('press %s ability1' % B2[0])
    hero_run('overlapping whips', build=line2, during=second, wait=3)
    hero_run('overlapping whips', 'off', build=line2, during=second, wait=3)
clear(); log('DONE')

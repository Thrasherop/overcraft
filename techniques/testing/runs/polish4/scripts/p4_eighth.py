# Sixth round. ONLY8: hero start vine g ride step whipang tunnelceil behind hudg tiny falling allymc. Harness keys, bots server-side, no second client.
XSTART = globals().get('XSTART', 40000)
ONLY8 = globals().get('ONLY8', None)
ROWS = globals().get('ROWS', 2)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_third.py", encoding="utf8").read()
if globals().get('OTHER_TREE'): src = src.replace("overcraft_polish4/run/p4_launch_tree", OTHER_TREE + "/run/p4_launch_tree")
ONLY = ('none',); ROUNDS = 0
exec(src.replace("clear(); log('DONE')\n", ""))
def w8(k): return ONLY8 is None or k in ONLY8
HAS_SOFT = 'softLanding' in cc('me')
def snap():
    s = st(); q = cc('me') if HAS_SOFT else {}
    return s, q
def watch_land(secs=7, after=None, need_fall=True):
    """Reads till down after falling: returns a compact series and the read list. Series: y, g(round), s(oft), c(limb), w(ater)."""
    t0 = time.time(); ser = []; low = None; fell = False; top = None
    while time.time() - t0 < secs:
        s, q = snap(); y = s['y']; top = y if top is None else max(top, y); ser.append('%.1f%s%s%s%s' % (y, 'g' if s.get('onGround') else '', 's' if q.get('softLanding') else '', 'c' if q.get('climbable') else '', 'w' if q.get('inWater') else ''))
        if after: after(s, q)
        if not s.get('onGround'): fell = True
        if fell and s.get('onGround') and y <= -59.5: break
        sleep(0.03)
    sleep(0.5); return ser
def pain_read():
    h = heard(''); return [n for n in h if 'pain' in n], h
def result(label, h0, ser):
    s = st(); p, h = pain_read(); log('  %s: health %.1f -> %.1f, end y %.2f, pain %s | %s' % (label, h0, s['health'], s['y'], len(p), ' '.join(ser)[:300]))
def place(hero, x, y, z, yaw=0, pitch=0):
    me(hero, x, z, yaw, pitch); mc('/time set midnight'); mc('/gamerule minecraft:natural_health_regeneration false'); tp(x, y, z, yaw, pitch); c('look %s %s' % (yaw, pitch)); sleep(1.3); c('heard')
    return st()['health']
def ledge(x, top=-49, zmax=3):
    mc('/fill %d -60 -6 %d %d %d minecraft:stone' % (x - 6, x + 6, top - 1, zmax), 0.2)
def ability(hero, x):
    """Off the ledge edge: the hero's own movement ability, ending in the air. x's ledge is z <= 3, top y -49; she stands at z 2.5 facing +z."""
    if hero == 'lifeweaver':
        c('key forward down'); sleep(0.25); c('key jump down'); sleep(0.2); c('key jump up'); sleep(0.2); c('key jump down'); sleep(0.05); c('key jump up'); c('key forward up')
    elif hero == 'sojourn':
        c('key ability_1 down'); sleep(0.1); c('key ability_1 up')
    elif hero == 'sombra':
        c('look 0 -25'); c('key ability_2 down'); sleep(0.05); c('key ability_2 up'); sleep(0.5); c('key ability_2 down'); sleep(0.05); c('key ability_2 up')
    elif hero == 'brigitte':
        c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); sleep(0.1); c('key primary_fire up')
    elif hero == 'kiriko':
        pass
HERO_START = {'lifeweaver': 2.0, 'sojourn': 2.0, 'sombra': 2.0, 'brigitte': 2.0}
if w8('start'):
    log('### an ability that starts on a ladder, in 1-deep water or on vines, up high: does its fall hurt? 11-20 m drops; soft read each read where the build has it')
    # Mercy: GA from where she stands (feet in water, on a ladder, on vines) to an ally 15 m up, launched in mid-air
    for where, how in [(w_, h_) for w_ in ('plain', 'water', 'ladder', 'vine') for h_ in ('launch', 'cancel', 'lost')] * ROWS:
        x = spot(r=24); xi = int(x)
        if where == 'water': mc('/setblock %d -60 0 minecraft:water' % xi, 0.05)
        if where == 'ladder': mc('/setblock %d -60 -1 minecraft:stone' % xi, 0.02); mc('/setblock %d -60 0 minecraft:ladder[facing=south]' % xi, 0.05)
        if where == 'vine': mc('/setblock %d -60 -1 minecraft:stone' % xi, 0.02); mc('/setblock %d -60 0 minecraft:vine[north=true]' % xi, 0.05)
        mc('/fill %d -44 7 %d -44 7 minecraft:stone' % (xi + 1, xi + 1), 0.1); h0 = place('mercy', x, -60, 0.5, 0, 0); a = add('up', 'mercy', 'blue', x + 1, 7.5, 0, 0, -43); sleep(0.4); c('look 7 -62'); sleep(0.2); c('heard')
        q0 = cc('me'); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.5); s1 = st(); q1 = cc('me')
        if how == 'launch': c('key jump down'); sleep(0.05); c('key jump up')
        elif how == 'cancel': c('key ability_1 down'); sleep(0.05); c('key ability_1 up')
        else: bot('remove ' + a)
        ser = watch_land(); result('Mercy GA from %s, %s (in it: climb %s water %s; y %.1f at the release, soft %s)' % (where, how, q0.get('climbable'), q0.get('inWater'), s1['y'], q1.get('softLanding')), h0, ser)
    # Lifeweaver, Sojourn off a ladder high up: dropping off, ability in the air
    for hero in ('lifeweaver', 'sojourn'):
        for where in ('ladder', 'plain') * ROWS:
            x = spot(r=24); mc('/fill %d -60 -2 %d -40 -1 minecraft:stone' % (x - 2, x + 2), 0.1)
            if where == 'ladder': mc('/fill %d -60 0 %d -40 0 minecraft:ladder[facing=south]' % (x - 2, x + 2), 0.1); h0 = place(hero, x, -41, 0.5, 0, 0)
            else: mc('/fill %d -41 0 %d -41 1 minecraft:stone' % (x - 2, x + 2), 0.1); h0 = place(hero, x, -40, 0.5, 0, 0)
            c('key forward down'); sleep(0.25)
            if hero == 'lifeweaver': c('key jump down'); sleep(0.2); c('key jump up'); sleep(0.2); c('key jump down'); sleep(0.05); c('key jump up')
            else: c('key ability_1 down'); sleep(0.08); c('key ability_1 up')
            c('key forward up'); ser = watch_land(); result('%s, ability off a %s 20 m up' % (hero, where), h0, ser)
if w8('brush'):
    log('### brushing a vine in mid-fall: a stone wall at z 8 with one strip of vine at y -53 on its face (z 7): a bash off a 12 m ledge hits the wall and falls down it; a Swift Step to a perch against it, taken away. 7 m below the vine')
    for kind in ('vine', 'none') * ROWS:
        x = spot(r=20); xi = int(x); ledge(x, -48); mc('/fill %d -60 8 %d -44 8 minecraft:stone' % (xi - 6, xi + 6), 0.1)
        if kind == 'vine': mc('/fill %d -53 7 %d -53 7 minecraft:vine[south=true]' % (xi - 6, xi + 6), 0.1)
        h0 = place('brigitte', x, -48, 2.0, 0, 0); ability('brigitte', x); ser = watch_land(); result('bash down a wall, %s' % kind, h0, ser)
    for kind in ('vine', 'none') * ROWS:
        x = spot(r=20); xi = int(x); mc('/fill %d -60 8 %d -36 8 minecraft:stone' % (xi - 6, xi + 6), 0.1); mc('/fill %d -40 7 %d -40 7 minecraft:stone' % (xi, xi), 0.1)
        if kind == 'vine': mc('/fill %d -53 7 %d -53 7 minecraft:vine[south=true]' % (xi - 6, xi + 6), 0.1)
        h0 = place('kiriko', x, -60, 0.5, 0, -60); a = add('high', 'mercy', 'blue', x, 7.5, 0, 0, -39); sleep(0.5); c('look 0 -68'); sleep(0.8); c('heard'); c('key ability_1 down'); mc('/fill %d -40 7 %d -39 7 minecraft:air' % (xi, xi), 0.0); c('key ability_1 up'); t0 = time.time()
        while st()['y'] < -45 and time.time() - t0 < 1: sleep(0.01)
        ser = watch_land(); result('Swift Step down a wall, %s' % kind, h0, ser)
    for kind in ('ladder', 'none') * ROWS:
        x = spot(r=20); h0 = place('lifeweaver', x, -60, 0.5, 0, 0)
if w8('vine'):
    log('### falling after an ability through one layer of vines or scaffolding at y -53, or a sheet of water at z 8: 7 m below it')
    def layer(x, kind):
        if kind == 'vine': mc('/fill %d -53 3 %d -53 26 minecraft:vine[north=true]' % (x - 6, x + 6), 0.1)
        if kind == 'scaffold': mc('/fill %d -53 3 %d -53 26 minecraft:scaffolding[distance=0]' % (x - 6, x + 6), 0.1)
    for hero in ('brigitte', 'sombra', 'lifeweaver', 'sojourn'):
        for kind in ('vine', 'none') * ROWS:
            x = spot(r=30); ledge(x, -48); h0 = place(hero, x, -48, 2.0, 0, 0); layer(x, kind); ability(hero, x); ser = watch_land(); result('%s, its ability off a 12 m ledge, through %s' % (hero, kind), h0, ser)
    for kind in ('scaffold', 'water'):
        x = spot(r=30); ledge(x, -48); h0 = place('brigitte', x, -48, 2.0, 0, 0); layer(x, kind)
        if kind == 'water': mc('/fill %d -60 9 %d -50 9 minecraft:water' % (x - 6, x + 6), 0.02)
        ability('brigitte', x); ser = watch_land(); result('brigitte, bash, through %s' % kind, h0, ser)
    for i in range(ROWS):
        x = spot(r=30); mc('/fill %d -40 7 %d -40 7 minecraft:stone' % (x, x), 0.2); h0 = place('kiriko', x, -60, 0.5, 0, -60); a = add('high', 'mercy', 'blue', x + 0.5, 7.5, 0, 0, -39); sleep(0.5); c('look 0 -68'); sleep(0.8); mc('/fill %d -53 3 %d -53 14 minecraft:vine[north=true]' % (x - 6, x + 6), 0.1); c('heard'); c('key ability_1 down'); mc('/fill %d -40 7 %d -39 7 minecraft:air' % (x, x), 0.0); c('key ability_1 up'); t0 = time.time()
        while st()['y'] < -45 and time.time() - t0 < 1: sleep(0.01)
        ser = watch_land(); result('kiriko, Swift Step, through a vine layer', h0, ser)
if w8('g'):
    log('### G, or a settled climb, after each hero\'s ability: G sent once the read says she\'s off the ledge in the air')
    for hero in ('sombra', 'lifeweaver', 'sojourn', 'brigitte'):
        for mode in ('G', 'none') * ROWS:
            x = spot(r=30); ledge(x, -48); h0 = place(hero, x, -48, 2.0, 0, 0); ability(hero, x); t0 = time.time()
            while st()['y'] > -50 and time.time() - t0 < 2.5: sleep(0.02)
            if mode == 'G': c('heromode off')
            ser = watch_land(); result('%s, %s' % (hero, mode), h0, ser); c('heromode on')
    for mode in ('G', 'none') * ROWS:
        x = spot(r=24); mc('/fill %d -44 7 %d -44 7 minecraft:stone' % (x + 1, x + 1), 0.1); h0 = place('mercy', x + 0.5, -60, 0.5, 0, 0); a = add('up', 'mercy', 'blue', x + 1.5, 7.5, 0, 0, -43); sleep(0.4); c('look 0 -62'); sleep(0.2); c('heard'); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.55); c('key jump down'); sleep(0.05); c('key jump up'); sleep(0.25)
        if mode == 'G': c('heromode off')
        ser = watch_land(); result('mercy, GA launch, %s' % mode, h0, ser); c('heromode on')
    for mode in ('G', 'none') * ROWS:
        x = spot(r=24); h0 = place('sigma', x, -60, 0.5, 0, 0); mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key secondary_fire down'); sleep(0.05); c('key secondary_fire up'); sleep(0.05)
        c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(0.6)
        s1 = st()
        if mode == 'G': c('heromode off')
        ser = watch_land(); result('sigma, Flux, %s (y %.1f when sent)' % (mode, s1['y']), h0, ser); c('heromode on')
if w8('ride'):
    log('### Lucio rides from a platform 9 m up along a wall that runs on past it, over nothing; let go (the wall jump) or crouch (drop off) there. Series: y, g = a read on the ground, s = soft. [CLEAN]: wallride.start heard, off the platform, no ground read from the top till the bottom')
    goal = globals().get('RIDES', 4)
    for leave_ in globals().get('RIDE_HOW', ('release', 'crouch')):
      for gmode in globals().get('RIDE_G', (False, True)):
        done = 0; tries = 0
        while done < goal and tries < goal * 3:
            tries += 1; x = spot(r=30); xi = int(x); top = -51
            mc('/fill %d -60 -3 %d -52 1 minecraft:stone' % (xi - 12, xi - 6), 0.2); mc('/fill %d -60 2 %d -44 2 minecraft:stone' % (xi - 12, xi + 16), 0.2)
            h0 = place('lucio', x - 9, top, 1.2, -90, 0); c('key jump down'); t0 = time.time(); ser = []; off = False; x_at = None
            while time.time() - t0 < 6:
                s_, q = snap(); y = s_['y']; ser.append('%.1f%s%s' % (y, 'g' if s_.get('onGround') else '', 's' if q.get('softLanding') else ''))
                if s_['x'] - x > 1 and not off:
                    off = True; x_at = s_['x'] - x; yt = y
                    if leave_ == 'release': c('key jump up')
                    else: c('key sneak down'); sleep(0.05); c('key sneak up'); c('key jump up')
                    if gmode:
                        t1 = time.time()
                        while time.time() - t1 < 0.6 and st()['y'] >= yt: sleep(0.02)
                        c('heromode off')
                if off and s_.get('onGround') and y < -59.5: break
                sleep(0.02)
            c('key jump up'); sleep(0.5); s1 = st(); p_, h = pain_read(); rode = any('wallride.start' in n for n in h)
            nums = [(float(re.match(r'-?[\d.]+', r).group()), 'g' in r) for r in ser]
            i_top = max(range(len(nums)), key=lambda i: nums[i][0]) if nums else 0; touched = any(g and yy > -59.5 for yy, g in nums[i_top:])
            ok = rode and off and not touched and s1['y'] < -59
            log('  %s, G %s, try %d: rode %s, left at x+%s, ground before the bottom %s, health %.1f -> %.1f, end y %.2f, pain %s%s | %s' % (leave_, gmode, tries, rode, None if x_at is None else round(x_at, 1), touched, h0, s1['health'], s1['y'], len(p_), '  [CLEAN]' if ok else '', ' '.join(ser)[-170:])); c('heromode on')
            if ok: done += 1
if w8('step'):
    log('### Swift Step to a perch 20 m up: y, on-ground and soft read from the press, perch left in place or taken at a delay; G only after a read at arrival')
    for mode, delay in (('none', 0), ('none', 0), ('none', 0.15), ('none', 0.15), ('none', 0.4), ('none', 0.4), ('G', 0), ('G', 0)):
        x = spot(r=20); mc('/fill %d -40 7 %d -40 7 minecraft:stone' % (x, x), 0.2); h0 = place('kiriko', x, -60, 0.5, 0, -60); a = add('high', 'mercy', 'blue', x + 0.5, 7.5, 0, 0, -39); sleep(0.5); c('look 0 -68'); sleep(0.8); c('heard'); c('key ability_1 down')
        if delay == 0: mc('/fill %d -40 7 %d -39 7 minecraft:air' % (x, x), 0.0)
        c('key ability_1 up'); t0 = time.time(); ser = []; arr = None; taken = delay == 0; sent = False
        while time.time() - t0 < 7:
            s, q = snap(); y = s['y']; ser.append('%.1f%s%s' % (y, 'g' if s.get('onGround') else '', 's' if q.get('softLanding') else ''))
            if y > -45 and arr is None: arr = time.time()
            if arr and not taken and time.time() - arr >= delay: mc('/fill %d -40 7 %d -39 7 minecraft:air' % (x, x), 0.0); taken = True
            if arr and mode == 'G' and not sent: c('heromode off'); sent = True
            if arr and y < -59.5 and s.get('onGround'): break
            sleep(0.02)
        sleep(0.5); s1 = st(); p, h = pain_read(); log('  %s, perch taken %.2f s after arrival: arrived %s, health %.1f -> %.1f, end y %.2f, pain %s | %s' % (mode, delay, arr is not None, h0, s1['health'], s1['y'], len(p), ' '.join(ser)[:240])); c('heromode on')
if w8('whipang'):
    log('### Whiplash at an angle into a wall: a real client touching a 2-high tunnel\'s side wall or in the open beside a wall; a bot Brigitte placed on the line at that angle, 5 m off, so the whip reaches them')
    for ang in (3, 10, 20, 30, 45):
        for tunnel in (True, False):
            for i in range(2):
                x = spot(r=20); xi = int(math.floor(x)); a = math.radians(ang); tx = x + 0.2; tz = 0.5
                mc('/fill %d -60 -12 %d -56 26 minecraft:stone' % (xi + 1, xi + 1), 0.2)
                if tunnel: mc('/fill %d -58 -12 %d -58 26 minecraft:stone' % (xi - 8, xi), 0.2)
                pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(tx, -60, tz, 180, 0); c('look 180 0'); sleep(0.3)
                bx = tx - 5 * math.sin(a); bz = tz - 5 * math.cos(a); b = add('brig', 'brigitte', 'red', bx, bz, -ang, 0); bot('perk %s BRIGITTE_WHIPLASH' % b); sleep(0.25); s0 = st(); bot('press %s ability1' % b); sleep(1.8); s1 = st(); ceil_ok = mc('/execute if block %d -58 0 minecraft:stone' % xi, 0.25)[-1:]
                log('  %d degrees, tunnel %s: z %.2f -> %.2f, x %.2f -> %.2f, health %.1f -> %.1f (hit %s), ceiling read %s' % (ang, tunnel, s0['z'], s1['z'], s0['x'] - x, s1['x'] - x, s0['health'], s1['health'], s1['health'] < s0['health'], (ceil_ok[0][-16:] if ceil_ok else None)))
if w8('tunnelceil'):
    log('### straight along the side wall in a tunnel, the ceiling read each run, first row after a new spot: why one row went 8.54 m')
    for i in range(4):
        x = spot(r=20); xi = int(math.floor(x)); mc('/fill %d -58 -8 %d -58 22 minecraft:stone' % (xi - 3, xi + 3), 0.05); mc('/fill %d -60 -8 %d -59 22 minecraft:stone' % (xi + 1, xi + 1), 0.05); r0 = mc('/execute if block %d -58 3 minecraft:stone' % xi, 0.25)[-1:]
        pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x + 0.2, -60, 0.5, 180, 0); c('look 180 0'); sleep(0.3); b = add('brig', 'brigitte', 'red', x, -4.5, 0, 0); sleep(0.2); s0 = st(); c('trace start'); bot('press %s ability1' % b); sleep(2); tr = c('trace').get('trace', '').splitlines(); s1 = st(); ys = [float(l.split()[4]) for l in tr if len(l.split()) > 4]; log('  run %d: ceiling read %s, z %.2f -> %.2f, y top %s' % (i, r0[0][-16:] if r0 else None, s0['z'], s1['z'], max(ys) if ys else None))
if w8('behind6'):
    log('### the zombie behind, after the client return: a phased bot at 5 m, a zombie at z 18, real client; control unphased')
    def shot6(kind, hero, key, hold, pitch=-1):
        for phased in (False, True):
            x = spot(); me(hero, x, 0.5, 0, 0); c('look 0 %s' % pitch); mc('/time set midnight'); t = add('tgt', 'mercy', 'red', x, 5.5, 180)
            mc('/summon minecraft:zombie %s -60 18 {NoAI:1b,Tags:["bz"],Health:200f,attributes:[{id:"minecraft:max_health",base:200}]}' % x, 0.1); sleep(0.2)
            if phased: bot('phase %s 1.8' % t)
            c('key %s down' % key); sleep(hold); c('key %s up' % key); sleep(1.0); log('  %-10s phased %-5s target %s | zombie %s | screen %r' % (kind, phased, sh(t, 'health'), hp('@e[tag=bz,sort=nearest,limit=1]'), st().get('screen'))); mc('/kill @e[tag=bz]', 0.1)
    shot6('ana dart', 'ana', 'secondary_fire', 0.1); shot6('sleep dart', 'ana', 'ability_1', 0.1); shot6('kunai', 'kiriko', 'secondary_fire', 0.15); shot6('railgun', 'sojourn', 'secondary_fire', 0.15); shot6('paw', 'jetpack_cat', 'secondary_fire', 0.15); shot6('thorn', 'lifeweaver', 'primary_fire', 0.3)
if w8('hudg'):
    log('### Rally\'s HUD, and after G on the last code: screenshots p6_rally_on, p6_rally_g')
    x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0); a = add('ally', 'mercy', 'blue', x + 2, 3.5, 0); sleep(0.2); mc('/giveult'); press('ultimate', 0.15); sleep(2.2); c('window 1280 720'); sleep(0.3); c('screenshot p6_rally_on.png'); s = st(); log('  in Rally: health %s of %s, state %s' % (s.get('health'), s.get('maxHealth'), {k: s[k] for k in s if 'rmor' in k or 'bsorb' in k or 'ow' in k.lower()})); sleep(0.2); c('heromode off'); sleep(0.5); c('heromode on'); sleep(0.8); c('screenshot p6_rally_g.png'); s = st(); al = mc('/data get entity @e[name=%s,limit=1] AbsorptionAmount' % a, 0.2)[-1:]; me_ = mc('/data get entity @s AbsorptionAmount', 0.2)[-1:]; log('  after G and back: health %s, her absorption %s, ally %s' % (s.get('health'), me_[0][-12:] if me_ else None, al[0][-12:] if al else None))
if w8('tiny'):
    log('### Whiplash on a silverfish before a slab; a target killed in the look; a target already falling; an ally in Minecraft mode')
    def one(block, z=4):
        def b(x): mc('/fill %d -60 %d %d -60 %d minecraft:%s' % (x - 4, z, x + 4, z, block), 0.2)
        return b
    for kind, bld in (('silverfish', one('oak_slab')), ('silverfish', None), ('silverfish', wall(4))):
        husk_run('tiny, %s' % ('slab' if bld and kind and bld.__closure__ and 'slab' in str(bld.__closure__[0].cell_contents) else 'wall or none'), build=bld, perk=True, n=1, kind=kind)
    # killed in the look: a bot target? the real client, /kill the tick after the wall
    x = spot(); clear(); wall(3)(x); pick('kiriko'); mc('/gamemode survival @s'); mc('/time set midnight'); tp(x, -60, 0.5, 180, 0); c('look 180 0'); sleep(0.3); b = add('brig', 'brigitte', 'red', x, -4.5, 0, 0); bot('perk %s BRIGITTE_WHIPLASH' % b); sleep(0.2); bot('press %s ability1' % b); sleep(0.3); mc('/kill @s', 0.2); sleep(1); c('respawn'); sleep(1.5); c('close'); s = st(); log('  killed about as it reaches the wall: no crash, back as %s, health %s' % (s.get('hero'), s.get('health')))
    # phased in the look
    x = spot(); clear(); wall(3)(x); pick('kiriko'); mc('/gamemode survival @s'); mc('/time set midnight'); tp(x, -60, 0.5, 180, 0); c('look 180 0'); sleep(0.3); b = add('brig', 'brigitte', 'red', x, -4.5, 0, 0); bot('perk %s BRIGITTE_WHIPLASH' % b); sleep(0.2); h0 = st()['health']; bot('press %s ability1' % b); t0 = time.time()
    while st()['health'] >= h0 and time.time() - t0 < 1: sleep(0.005)
    bot('phase me 1'); sleep(1); s = st(); log('  phased as the whip reads hit: z %.2f, health %.1f -> %.1f (9.6 = slammed; no read of whether the phase came before the look)' % (s['z'], h0, s['health']))
    # already falling
    for i in range(2):
        x = spot(); clear(); wall(4)(x); pick('kiriko'); mc('/gamemode survival @s'); mc('/time set midnight'); b = add('brig', 'brigitte', 'red', x, -4.5, 0, 0); bot('perk %s BRIGITTE_WHIPLASH' % b); tp(x, -56, 0.5, 180, 0); c('look 180 0'); sleep(0.05); h0 = st()['health']; bot('press %s ability1' % b); sleep(1.6); s = st(); log('  falling from 4 m as the whip comes, wall at 4: z %.2f, y %.2f, health %.1f -> %.1f' % (s['z'], s['y'], h0, s['health']))
    # hero switch in a landing
    x = spot(r=24); ledge(x, -48); h0 = place('brigitte', x, -48, 2.0, 0, 0); mc('/spawnpoint @s %d -48 2' % x); ability('brigitte', x); sleep(0.1); hs = c('hero mercy'); sleep(0.05); c('close'); ser = watch_land(); s = st(); log('  hero switch in a bash landing: %s, now %s, health %.1f -> %.1f, %s' % (hs, s.get('hero'), h0, s['health'], ' '.join(ser)[:160])); mc('/spawnpoint @s 0 -60 0'); c('close')
    # an ally in Minecraft mode
    x = spot(); me('mercy', x, 0.5, 0, 0); mc('/time set midnight'); c('heromode off'); sleep(0.4); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); b2 = add('brg', 'brigitte', 'blue', x + 2, 3.5, 0); bot('charge %s 9999' % b2); mc('/effect give @s minecraft:poison 20 0', 0.1); s0 = st(); bot('press %s ability2' % k); sleep(0.6); q = cc('me'); pz = mc('/data get entity @s active_effects', 0.2)[-1:]; bot('press %s ultimate' % b2); sleep(2.0); ab = mc('/data get entity @s AbsorptionAmount', 0.2)[-1:]; s1 = st(); log('  a real client ally in Minecraft mode: bot Suzu: effects after %s, phased %s; bot Rally: absorption %s; health %.1f -> %.1f, hero mode %s' % ((pz[0][-30:] if pz else None), q.get('phased'), ab[0][-10:] if ab else None, s0['health'], s1['health'], s1.get('heroMode'))); c('heromode on')
clear(); log('DONE')

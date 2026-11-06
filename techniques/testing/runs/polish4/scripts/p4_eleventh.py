# Round eight. E_: dash mount grip dashg gfall swimjump dashctl ctrl dart transloc. Harness keys; bots server-side.
XSTART = globals().get('XSTART', 40000)
E_ = globals().get('E_', None); ROWS = globals().get('ROWS', 2)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_tenth.py", encoding="utf8").read()
O = ('none',)
exec(src.replace("clear(); log('DONE')\n", ""))
def w9(k): return E_ is None or k in E_
def spx(hero, x, y, z, yaw=0, pitch=0):
    return place(hero, x, y, z, yaw, pitch)
def dash_ready(): return st().get('ability3Cooldown')
if w9('dashctl'):
    log('### the dash\'s control: a jump, Space again 0.25 s into it in the air, forward. z and cd3 read through it. Ground, then a ladder (facing out, jump off), deep water treading')
    for i in range(ROWS):
        x = spot(r=24); h0 = spx('lifeweaver', x, -60, 0.5); s0 = st(); c('key forward down'); c('key jump down'); sleep(0.08); c('key jump up'); sleep(0.2); s1 = st(); c('key jump down'); sleep(0.05); c('key jump up'); sleep(0.35); s2 = st(); c('key forward up'); sleep(0.4); s3 = st(); log('  jump then Space in the air, W: z %.2f, at the press z %.2f y %.2f ground %s; +0.4 s z %.2f; end z %.2f, cd3 %.2f' % (s0['z'], s1['z'], s1['y'], s1.get('onGround'), s2['z'], s3['z'], s3.get('ability3Cooldown', 0)))
if w9('dash'):
    log('### tap Space with the dash ready: treading deep water; high on a ladder facing out; shallow water standing. z, cd3 over 1 s')
    for where in ('deep', 'ladder', 'ladder-hold', 'shallow') * ROWS:
        x = spot(r=24); xi = int(x)
        if where == 'deep':
            mc('/fill %d -63 -4 %d -60 16 minecraft:water' % (xi - 4, xi + 4), 0.15); mc('/fill %d -64 -4 %d -64 16 minecraft:stone' % (xi - 4, xi + 4), 0.1); h0 = spx('lifeweaver', x, -61, 0.5)
        elif where.startswith('ladder'):
            mc('/fill %d -60 -1 %d -44 -1 minecraft:stone' % (xi - 1, xi + 1), 0.1); mc('/fill %d -60 0 %d -45 0 minecraft:ladder[facing=south]' % (xi - 1, xi + 1), 0.1); h0 = spx('lifeweaver', x, -50, 0.5, 0, 0)
        else:
            mc('/setblock %d -60 0 minecraft:water' % xi, 0.05); h0 = spx('lifeweaver', x, -60, 0.5)
        s0 = st(); q0 = cc('me'); c('heard'); c('key jump down'); sleep(0.06); c('key jump up'); sleep(1.0); s1 = st(); p, h = pain_read(); log('  %s (water %s, climb %s): z %.2f -> %.2f, y %.2f -> %.2f, cd3 %.2f, dash sound %s' % (where, q0.get('inWater'), q0.get('climbable'), s0['z'], s1['z'], s0['y'], s1['y'], s1.get('ability3Cooldown', 0), [n for n in h if 'dash' in n][:2]))
if w9('mount'):
    log('### a landing and a mount: bash off 12 m, then /ride onto a NoGravity horse hung 20 m up; read soft 2 s; dismount there: 20 m')
    for how in ('horse', 'none') * ROWS:
        x = spot(r=24); xi = int(x); ledge(x, -48); h0 = spx('brigitte', x, -48, 2.0, 0, 0)
        if how == 'horse': mc('/summon minecraft:horse %d.5 -40 14.5 {NoGravity:1b,NoAI:1b,Tame:1b,Tags:["rd"],Attributes:[]}' % xi, 0.2)
        ability('brigitte', x); t0 = time.time()
        while st()['y'] > -50 and time.time() - t0 < 1.5: sleep(0.01)
        q0 = cc('me')
        if how == 'horse':
            r_ = mc('/ride @s mount @e[tag=rd,limit=1,sort=nearest]', 0.3)[-1:]; sleep(2.0); s1 = st(); q1 = cc('me'); mc('/ride @s dismount', 0.05); ser = bottom(8); result('horse: soft at the mount %s, after 2 s riding %s, y %.1f (%s)' % (q0.get('softLanding'), q1.get('softLanding'), s1['y'], r_[0][-30:] if r_ else None), h0, ser[-10:]); mc('/kill @e[tag=rd]', 0.05)
        else:
            ser = bottom(); result('no horse', h0, ser[-8:])
    log('### the same into lava and a cobweb: soft read after 1 s')
    for kind in ('lava', 'web'):
        x = spot(r=24); xi = int(x); ledge(x, -48); block = 'lava' if kind == 'lava' else 'cobweb'; mc('/fill %d -60 4 %d -56 26 minecraft:%s' % (xi - 6, xi + 6, block), 0.2); mc('/effect give @s minecraft:fire_resistance 30 0', 0.05); h0 = spx('brigitte', x, -48, 2.0, 0, 0); mc('/effect give @s minecraft:fire_resistance 30 0', 0.05); ability('brigitte', x); t0 = time.time()
        while st()['y'] > -56.2 and time.time() - t0 < 2: sleep(0.01)
        sleep(1.5); s, q = snap(); log('  %s: y %.2f, soft %s, health %s' % (kind, s['y'], q.get('softLanding'), s['health'])); mc('/fill %d -60 4 %d -56 26 minecraft:air' % (xi - 6, xi + 6), 0.1)
if w9('grip'):
    log('### Life Grip let go in deep water by Jump; swim to a ladder; climb; step off onto stone. A grip\'s real landing: control. Mob: control')
    for i in range(ROWS):
        x = spot(r=28); xi = int(x)
        mc('/fill %d -63 0 %d -60 2 minecraft:water' % (xi - 12, xi + 4), 0.15); mc('/fill %d -64 0 %d -64 2 minecraft:stone' % (xi - 12, xi + 4), 0.1); mc('/fill %d -44 4 %d -60 4 minecraft:stone' % (xi - 12, xi + 4), 0.1); mc('/fill %d -60 3 %d -45 3 minecraft:ladder[facing=north]' % (xi - 12, xi + 4), 0.2)
        h0 = spx('mercy', x + 2, -61, 1.5, 0, 0); g = add('lw', 'lifeweaver', 'blue', x - 10, 1.5, -90, 0, -59); sleep(0.3); bot('press %s ability2' % g); sleep(0.7); c('key jump down'); sleep(0.08); c('key jump up'); sleep(0.2); s1, q1 = snap(); c('key jump down'); c('key forward down'); t0 = time.time()
        while time.time() - t0 < 9:
            s2, q2 = snap()
            if s2['y'] > -48: break
            sleep(0.05)
        c('key forward up'); c('key jump up'); s3, q3 = snap(); mc('/fill %d -60 0 %d -60 2 minecraft:stone' % (xi - 12, xi + 4), 0.1); c('heard'); h0 = st()['health']; c('key back down'); sleep(0.2); c('key back up'); ser = bottom(); result('let go in water at z %.1f x %.1f; climbed to y %.1f' % (s1['z'], s1['x'] - x, s3['y']), h0, ser[-10:])
    for mode in ('hero', 'minecraft') * ROWS:
        x = spot(r=28); xi = int(x); mc('/fill %d -49 -3 %d -49 3 minecraft:stone' % (xi - 18, xi - 14), 0.1); mc('/fill %d -49 -1 %d -49 2 minecraft:stone' % (xi - 1, xi + 1), 0.1); h0 = spx('mercy', x, -48, 0.5, 90, 0); g = add('lw', 'lifeweaver', 'blue', x - 16, 0.5, -90, 0, -48); sleep(0.3)
        if mode == 'minecraft': c('heromode off'); sleep(0.2)
        h0 = st()['health']; bot('press %s ability2' % g); sleep(0.8); s1 = st(); c('key jump down'); sleep(0.08); c('key jump up'); ser = bottom(); result('control, %s mode: gripped, let go by Jump over the gap at x %.1f, 12 m up' % (mode, s1['x'] - x), h0, ser[-8:]); c('heromode on')
if w9('climbctl'):
    log('### control for the grip row: the same pool, ladder and step, no grip at all')
    for i in range(ROWS):
        x = spot(r=28); xi = int(x); mc('/fill %d -63 0 %d -60 2 minecraft:water' % (xi - 12, xi + 4), 0.15); mc('/fill %d -64 0 %d -64 2 minecraft:stone' % (xi - 12, xi + 4), 0.1); mc('/fill %d -44 4 %d -60 4 minecraft:stone' % (xi - 12, xi + 4), 0.1); mc('/fill %d -60 3 %d -45 3 minecraft:ladder[facing=north]' % (xi - 12, xi + 4), 0.2); h0 = spx('mercy', x, -61, 1.5, 0, 0); c('key jump down'); c('key forward down'); t0 = time.time()
        while time.time() - t0 < 9:
            s2, q2 = snap()
            if s2['y'] > -48: break
            sleep(0.05)
        c('key forward up'); c('key jump up'); s3, q3 = snap(); mc('/fill %d -60 0 %d -60 2 minecraft:stone' % (xi - 12, xi + 4), 0.1); c('heard'); h0 = st()['health']; c('key back down'); sleep(0.2); c('key back up'); ser = bottom(); result('no grip; climbed to y %.1f' % s3['y'], h0, ser[-10:])
if w9('swimjump'):
    log('### a swimmer holding Space through a grip: does it let go at 0.65 s? x, pull reached')
    for hold in (True, False) * ROWS:
        x = spot(r=28); xi = int(x); mc('/fill %d -63 -2 %d -60 2 minecraft:water' % (xi - 12, xi + 4), 0.15); mc('/fill %d -64 -2 %d -64 2 minecraft:stone' % (xi - 12, xi + 4), 0.1); h0 = spx('mercy', x, -61, 0.5, 90, 0); g = add('lw', 'lifeweaver', 'blue', x - 9, 0.5, -90, 0, -59); sleep(0.3)
        if hold: c('key jump down')
        s0 = st(); bot('press %s ability2' % g); ser = []
        for i2 in range(14):
            s = st(); ser.append('%.1f' % (s['x'] - x)); sleep(0.08)
        c('key jump up'); log('  holding Space %s: x %s' % (hold, ' '.join(ser)))
if w9('dashg'):
    log('### a dash cut by G (and back later), by death (respawn): does it resume along the old direction?')
    for how in ('G', 'death', 'none'):
        x = spot(r=24); mc('/fill %d -60 -6 %d -50 3 minecraft:stone' % (x - 6, x + 6), 0.2); h0 = spx('lifeweaver', x, -49, -2.0, 0, 0); c('key forward down'); c('key jump down'); sleep(0.08); c('key jump up'); sleep(0.2); c('key jump down'); sleep(0.02); c('key jump up')
        if how == 'G': c('heromode off'); c('key forward up'); sleep(1.5); s1 = st(); c('heromode on'); sleep(0.5); s2 = st(); log('  G: off at z %.2f, then on, 0.5 s: z %.2f (y %.2f)' % (s1['z'], s2['z'], s2['y']))
        if how == 'death': mc('/kill @s', 0.1); c('key forward up'); sleep(0.8); c('respawn'); sleep(1.2); c('close'); s1 = st(); sleep(0.6); s2 = st(); log('  death: back at x %.1f z %.2f, then z %.2f, y %.2f' % (s1['x'], s1['z'], s2['z'], s2['y']))
        if how == 'none': c('key forward up'); sleep(0.5); log('  control: z %.2f' % st()['z'])
if w9('gfall'):
    log('### G to Minecraft mode, walk off 15 m, G back just before landing')
    for hero in ('juno', 'kiriko', 'brigitte') * ROWS:
        for g_back in (True, False):
            x = spot(r=24); mc('/fill %d -60 -3 %d -46 3 minecraft:stone' % (x - 3, x + 3), 0.2); h0 = spx(hero, x, -45, 2.5, 0, 0); c('heromode off'); sleep(0.4); h1 = st()['health']; c('key forward down'); sleep(0.7); c('key forward up'); t0 = time.time()
            while st()['y'] > -55 and time.time() - t0 < 4: sleep(0.01)
            if g_back: c('heromode on')
            ser = bottom(3); s = st(); log('  %s, G back %s: health %.1f -> %.1f, mode %s' % (hero, g_back, h1, s['health'], s.get('heroMode'))); c('heromode on')
            if s['health'] <= 0: c('respawn'); sleep(1.5); c('close')
if w9('ctrl'):
    log('### on a ladder after a bash: holding Left Ctrl (hero mode\'s crouch) 0.8 s, then off')
    for how in ('ctrl', 'slide') * ROWS:
        x = spot(r=20); xi = int(x); mc('/fill %d -60 8 %d -42 8 minecraft:stone' % (xi - 6, xi + 6), 0.1); mc('/fill %d -60 -6 %d -49 3 minecraft:stone' % (xi - 6, xi + 6), 0.15); mc('/fill %d -60 7 %d -44 7 minecraft:ladder[facing=north]' % (xi - 6, xi + 6), 0.1)
        h0 = place('brigitte', x, -48, 2.0, 0, 0); ability('brigitte', x); t0 = time.time()
        while not cc('me').get('climbable') and time.time() - t0 < 1.5: sleep(0.01)
        if how == 'ctrl': c('key crouch down'); sleep(0.8)
        s0, q0 = snap(); c('key crouch up'); c('look 180 0'); c('key forward down'); sleep(0.3); c('key forward up'); ser = bottom(); result('ladder, %s: at y %.1f soft %s' % (how, s0['y'], q0.get('softLanding')), h0, ser[-10:])
if w9('dart'):
    log('### a real dart from a bot Ana at a Lifeweaver mid-dash fall and Mercy off GA: she aims at the airborne client from below')
    for hero in ('mercy', 'lifeweaver') * ROWS:
        x = spot(r=28); xi = int(x)
        if hero == 'mercy':
            mc('/fill %d -44 7 %d -44 7 minecraft:stone' % (xi + 1, xi + 1), 0.1); h0 = place('mercy', x, -60, 0.5, 0, 0); a = add('up', 'mercy', 'blue', x + 1, 7.5, 0, 0, -43); sleep(0.4); c('look 7 -62'); sleep(0.2); c('heard'); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.55); c('key jump down'); sleep(0.05); c('key jump up')
        else:
            mc('/fill %d -60 -6 %d -41 3 minecraft:stone' % (xi - 4, xi + 4), 0.2); h0 = place('lifeweaver', x, -40, 2.0, 0, 0); ability('lifeweaver', x)
        n = add('anaD', 'ana', 'red', x, 22, 180, 0); sleep(0.05); s = st(); dy = s['y'] + 1 - (-58.4); dz = 22 - s['z']; pitch = -math.degrees(math.atan2(dy, dz)); bot('look %s 180 %.1f' % (n, pitch)); bot('press %s ability1' % n); t0 = time.time(); ever = False; ser = []
        while time.time() - t0 < 5:
            s, q = snap(); ever = ever or q.get('asleep'); ser.append('%.1f%s%s%s' % (s['y'], 'g' if s.get('onGround') else '', 's' if q.get('softLanding') else '', 'A' if q.get('asleep') else ''))
            if s.get('onGround') and s['y'] < -59.5: break
            sleep(0.03)
        sleep(0.4); s = st(); p, h = pain_read(); log('  %s: dart landed %s, health %.1f -> %.1f, pain %s | %s' % (hero, ever, h0, s['health'], len(p), ' '.join(ser)[-160:]))
if w9('transloc'):
    log('### Sombra\'s Translocator off a 12 m ledge: G in the air, through a wall strip of vine, plain')
    for how in ('plain', 'G') * ROWS:
        x = spot(r=30); ledge(x, -48); h0 = place('sombra', x, -48, 2.0, 0, 0); ability('sombra', x); t0 = time.time()
        while st()['y'] > -50 and time.time() - t0 < 2.5: sleep(0.02)
        if how == 'G': c('heromode off')
        ser = bottom(); result('Translocator, %s' % how, h0, ser[-10:]); c('heromode on')
clear(); log('DONE')

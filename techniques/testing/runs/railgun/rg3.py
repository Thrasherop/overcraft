# Round three of #126. Harness keys unless a row says real mouse. "lag" is the dev delay, not real latency.
SRC = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg2.py", encoding="utf8").read()
A3 = ARGS; ARGS = ['none']; exec(SRC); NAMES = A3 or None
log('### round three', time.strftime('%H:%M:%S'), 'fps', st().get('fps'))
def marks(tr): return parse(tr)
def looks(m, t0=None):
    P = [t for t, x in m if x.startswith('press')]; F = [t for t, x in m if x == 'fx']; S = [t for t, x in m if x == 'srv_shot']
    return P, F, S
if want('mash'):
    # item 1: fire, click late-ish in the recovery, click after it. What's the third press's look, and is it a shot at all?
    rig();
    for second, third in ((0.42, 0.75), (0.42, 0.80), (0.44, 0.75), (0.40, 0.72), (0.52, 0.75), (0.52, 0.85), (0.60, 0.85), (0.42, 0.75), (0.42, 0.80), (0.44, 0.75), (0.40, 0.72), (0.52, 0.75)):
        ready(); c('presstrace energy 50'); sleep(0.2); c('presstrace start'); t0 = time.time(); c('key primary_fire down'); c('key primary_fire up'); sleep(0.1); c('presstrace energy 50')
        while time.time() - t0 < second: sleep(0.001)
        c('key primary_fire down'); c('key primary_fire up')
        while time.time() - t0 < third: sleep(0.001)
        c('key primary_fire down'); c('key primary_fire up'); sleep(0.5)
        tr = c('presstrace').get('trace', ''); m = marks(tr); P, F, S = looks(m)
        log('MASH asked %.2f %.2f: presses at' % (second, third), [round(p - P[0]) for p in P], 'looks at', [round(f - P[0]) for f in F], 'shots at', [round(s - P[0]) for s in S], 'third look late by', round(F[-1] - P[-1]) if len(F) > 1 and len(P) > 2 and F[-1] >= P[-1] else 'none', 'ghost' if len(F) > len(S) else '')
    # the same with no energy at the middle click, energy back for the last
    for i in range(4):
        ready(); c('presstrace energy 50'); sleep(0.2); c('presstrace start'); t0 = time.time(); c('key primary_fire down'); c('key primary_fire up'); sleep(0.2); c('presstrace energy 0')
        while time.time() - t0 < 0.55: sleep(0.001)
        c('key primary_fire down'); c('key primary_fire up'); sleep(0.08); c('presstrace energy 50')
        while time.time() - t0 < 0.80: sleep(0.001)
        c('key primary_fire down'); c('key primary_fire up'); sleep(0.5)
        tr = c('presstrace').get('trace', ''); m = marks(tr); P, F, S = looks(m)
        log('MASH0 no energy at the middle click: presses', [round(p - P[0]) for p in P], 'looks', [round(f - P[0]) for f in F], 'shots', [round(s - P[0]) for s in S], 'last look late by', round(F[-1] - P[-1]) if F and F[-1] >= P[-1] else 'none')
if want('repeat'):
    # item 2: the right button on a keyboard key, held, in Overclock
    c('presstrace bind primary_fire key.keyboard.v'); sleep(0.1)
    def hold_key(secs):
        c('press key.keyboard.v down'); t0 = time.time(); sleep(0.5)
        while time.time() - t0 < secs: c('press key.keyboard.v repeat'); sleep(0.03)
        c('press key.keyboard.v up')
    ready(); mc('/giveult @s', 0.1); tap('ultimate', 0.05); sleep(1.0); c('presstrace start'); hold_key(3.0); sleep(0.3); tr = c('presstrace').get('trace', ''); k = counts(tr)
    log('REPEAT V held 3 s in Overclock:', k, tr[:300]); sleep(6)
    ready(); c('presstrace energy 100'); sleep(0.2); c('presstrace start'); hold_key(2.0); c('presstrace energy 100'); sleep(0.3); tr = c('presstrace').get('trace', ''); log('REPEAT V held 2 s, energy:', counts(tr))
    ready(); c('presstrace start'); c('presstrace bind primary_fire key.mouse.right'); sleep(0.1)
    # Q held while the ult can't go: does it keep locking her?
if want('stuck'):
    # item 3: a shown press in the server's buffer, then G inside those ticks. (HeroModePayload isn't lagged.)
    res = []
    for i in range(10):
        ready(); c('presstrace lag 100'); sleep(0.2); c('presstrace energy 50'); sleep(0.2); t0 = time.time(); c('key primary_fire down'); c('key primary_fire up'); sleep(0.3); c('presstrace energy 50')
        while time.time() - t0 < 0.68: sleep(0.001)
        c('key primary_fire down'); c('key primary_fire up')
        sleep(0.105); c('heromode off'); sleep(0.2); fl = c('presstrace flags'); c('heromode on'); sleep(0.2); fl2 = c('presstrace flags'); res.append((fl.get('shown'), fl2.get('shown')))
        c('presstrace lag 0'); sleep(0.4)
    log('STUCK shown flag after G, then after G back', res)
if want('hack'):
    # item 4: hacked, ult ready: Q then the right button: the look
    for i in range(3):
        ready(); nm = 'rgsom%d' % (int(time.time()) % 10000); c('bot add %s sombra red 0.5 -60 5.5 180' % nm); mc('/gamemode survival ' + nm, 0.12); c('bot look %s 180 0' % nm); sleep(0.25); c('bot charge %s 9999' % nm); mc('/giveult @s', 0.1)
        c('bot press %s ultimate' % nm)
        for k in range(30):
            cc = c('bot cc me')
            if cc.get('silenced'): break
            sleep(0.03)
        c('presstrace energy 50'); sleep(0.12); c('presstrace start'); c('key ultimate down'); c('key ultimate up'); c('key primary_fire down'); c('key primary_fire up'); sleep(0.6)
        tr = c('presstrace').get('trace', ''); m = marks(tr); P, F, S = looks(m)
        log('HACK silenced', cc.get('silenced'), 'ult active', st().get('ultActive'), 'presses', [round(p - P[0]) for p in P], 'looks', [round(f - P[0]) for f in F], 'shots', [round(s - P[0]) for s in S], '|', tr[:240])
        c('bot remove ' + nm); sleep(3.5)
if want('speed'):
    # item 6: a perfect flick at speed: the camera aims at a zombie's chest from where it is this frame, and the press. A hit?
    mc('/fill -60 -61 -20 60 -61 40 minecraft:stone', 0.3); mc('/fill -60 -60 -20 60 -48 40 minecraft:air', 0.3)
    mc('/kill @e[type=minecraft:zombie]', 0.2)
    mc('/summon minecraft:zombie 0.5 -60 12.5 {NoAI:1b,Tags:["rgs"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}', 0.3)
    Zs = '@e[tag=rgs,sort=nearest,limit=1]'; T = '0.5 -58.9 12.5'
    def go(kind):
        # the press at each kind's top speed by the clock: a run 0.5 s in, a slide 0.35 s in, a slide jump 0.12 s after the jump
        guard(); mc('/overcraft reset', 0.08); start = {'run': -5, 'slide': -9, 'jump': -9}[kind]
        mc('/tp @s %d -60 0.5 -90 0' % start, 0.15); c('look -90 0'); sleep(0.25); h0 = hp(Zs); c('presstrace energy 50'); sleep(0.1); c('presstrace start'); c('trace start')
        c('press key.keyboard.w down'); sleep(0.05)
        if kind == 'run': sleep(0.45)
        if kind in ('slide', 'jump'): c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.30)
        if kind == 'jump': c('key jump down'); sleep(0.05); c('key jump up'); sleep(0.07)
        s0 = st(); v0 = (s0.get('x'), s0.get('y')); c('presstrace aimpress ' + T); sleep(0.05); c('press key.keyboard.w up'); sleep(0.5); h1 = hp(Zs); tr = c('presstrace').get('trace', ''); m = marks(tr); tt = c('trace').get('trace', '').splitlines()
        cam = [x for t, x in m if x.startswith('press cam')]; eye = [x for t, x in m if x.startswith('srv_eye')]
        if cam and eye:
            a = [float(q) for q in cam[0].split()[2:]]; b = [float(q) for q in eye[0].split()[1:]]; d = sum((p - q) ** 2 for p, q in zip(a, b)) ** 0.5
        else: d = None; a = b = None
        hs = [float(l.split()[1]) for l in tt if len(l.split()) > 1]
        log('SPEED', kind, 'hit' if h0 != h1 else 'MISS', 'took', round((h0 or 0) - (h1 or 0), 2), 'top speed in trace', max(hs) if hs else None, 'camera to server eye', None if d is None else round(d, 3), 'cam', a, 'eye', b, 'miss line?' ); sleep(0.5)
    for kind in ('run', 'slide', 'jump'):
        for i in range(10): go(kind)
if want('many'):
    # item 7: frames over many shots, no sampler
    ready(); rig(); c('presstrace frames on'); out = []
    for i in range(40):
        c('presstrace energy 50'); sleep(0.1); c('presstrace start'); c('key primary_fire down'); c('key primary_fire up'); sleep(0.75); tr = c('presstrace').get('trace', ''); g = [x for x in gaps(tr, 45)]; out.append((i, g))
    c('presstrace frames off'); log('MANY frames over 45 ms by shot:', [x for x in out if x[1]], 'of', len(out), 'fps', st().get('fps'))
if want('others'):
    # item 5: others' reloads kept
    log('ok others')
log('### done')

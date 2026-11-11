# Round four of #126. Rows say their input. "lag" is the dev delay, not real latency.
import subprocess, math
SP = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
SRC = open(SP + "rg2.py", encoding="utf8").read()
A4 = ARGS; ARGS = ['none']; exec(SRC); NAMES = A4 or None
PAY = os.environ.get('RG_PAY', 'eye')   # the payload's shape on this tree: eye (x y z) or partial
log('### round four', time.strftime('%H:%M:%S'), 'fps', st().get('fps'), 'payload', PAY)
def send(held, pressed, yaw=0, pitch=0, x=0, y=0, z=0):
    if PAY == 'eye': return c('presstrace send %d %d %s %s %s %s %s' % (held, pressed, yaw, pitch, x, y, z))
    return c('presstrace send %d %d %s %s %s' % (held, pressed, yaw, pitch, x))  # x is the partial here: whatever a client cares to say
def eyes(tr): return [x for t, x in parse(tr) if x.startswith('srv_eye')]
def summon(tag, x, y, z):
    mc('/summon minecraft:zombie %s %s %s {NoAI:1b,Tags:["%s"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}' % (x, y, z, tag), 0.25)
if want('bank'):
    # 1: fire to 0, a press with an eye at A, walk 50 m, energy, a plain edge: where does the shot leave from?
    mc('/fill 40 -61 -10 100 -61 30 minecraft:stone', 0.3); mc('/fill 40 -60 -10 100 -50 30 minecraft:air', 0.2); mc('/kill @e[tag=rgb]', 0.2); summon('rgb', 44.5, -60, 8.5)
    Zb = '@e[tag=rgb,sort=nearest,limit=1]'
    for i in range(4):
        ready(); mc('/tp @s 44.5 -60 0.5 0 0', 0.2); c('look 0 0'); sleep(0.3); c('presstrace energy 0'); sleep(0.2); h0 = hp(Zb); c('presstrace start')
        A = (44.5, -58.4, 2.5)
        send(2, 32, 0, 0, *( (A) if PAY == 'eye' else (0.5, 0, 0))); sleep(0.1); send(0, 0); sleep(0.1)
        mc('/tp @s 94.5 -60 0.5 0 0', 0.25); c('look 0 0'); sleep(0.2); c('presstrace energy 50'); sleep(0.2)
        send(2, 0); sleep(0.2); send(0, 0); sleep(0.5); tr = c('presstrace').get('trace', ''); h1 = hp(Zb); s = st()
        log('BANK eye claimed', A if PAY == 'eye' else 'none to claim', 'she is at', round(s.get('x'), 1), 'shot', eyes(tr), 'far zombie took', round(h0 - h1, 2)); sleep(0.7)
if want('peek'):
    # 2: behind a wall: a press claiming an eye round its end, 5.9 m off; a zombie straight ahead of the claim, none ahead of her
    mc('/fill 40 -61 -20 100 -61 30 minecraft:stone', 0.3); mc('/fill 40 -60 -20 100 -50 30 minecraft:air', 0.2); mc('/fill 60 -60 -6 70 -50 -6 minecraft:stone', 0.2); mc('/kill @e[tag=rgp]', 0.2); summon('rgp', 71.4, -60, 2.5)
    Zp = '@e[tag=rgp,sort=nearest,limit=1]'
    for i in range(5):
        ready(); mc('/tp @s 65.5 -60 -9.5 0 0', 0.2); c('look 0 0'); sleep(0.5); c('presstrace energy 50'); sleep(0.2); h0 = hp(Zp); c('presstrace start')
        send(2, 32, 0, 0, *((71.4, -58.38, -9.5) if PAY == 'eye' else (1e9, 0, 0))); sleep(0.1); send(0, 0); sleep(0.5); tr = c('presstrace').get('trace', ''); h1 = hp(Zp)
        log('PEEK behind a wall, the zombie round its end took', round(h0 - h1, 2), 'shot', eyes(tr)); sleep(0.7)
if want('garbage'):
    # the partial's bad values, on the new payload
    for bad in ('NaN', '1e9', '-5', '0.5'):
        ready(); mc('/tp @s 0.5 -60 0.5 0 0', 0.2); sleep(0.2); c('presstrace energy 50'); sleep(0.2); c('presstrace start'); send(2, 32, 0, 0, bad); sleep(0.1); send(0, 0); sleep(0.4); tr = c('presstrace').get('trace', ''); s = st(); log('GARBAGE partial', bad, 'shot', eyes(tr), 'she is at', s.get('x'), s.get('y'), s.get('z')); sleep(0.5)
if want('turn'):
    # 4: mash through the recovery's end while turning: the aim shown against the shot's
    rig(); bad = 0; n = 0; buffered = 0
    c('presstrace lag 100'); sleep(0.3)  # (the dev lag holds her press, not her moves: so moves overwrite the rotation while the press waits, as a late buffer would)
    for i in range(30):
        gap = (0.68, 0.70, 0.72)[i % 3]
        ready(); c('presstrace energy 50'); sleep(0.15); c('presstrace start'); t0 = time.time(); c('key primary_fire down'); c('key primary_fire up'); sleep(0.3); c('presstrace energy 50')
        yaw = 0.0
        while time.time() - t0 < gap: sleep(0.001)
        c('key primary_fire down'); c('key primary_fire up')
        for k in range(8):
            yaw += 1.5; c('look %.1f 0' % yaw)
        sleep(0.4); tr = c('presstrace').get('trace', ''); m = parse(tr)
        clicks = [x.split() for t, x in m if x.startswith('click')]; shots = [x.split() for t, x in m if x.startswith('srv_eye')]; sh = [t for t, x in m if x.startswith('srv_eye')]; ck = [t for t, x in m if x.startswith('click')]
        if len(clicks) > 1 and len(shots) > 1:
            n += 1; d = float(shots[1][4]) - float(clicks[1][4]); w = sh[1] - ck[1]; bad += 1 if abs(d) > 0.01 else 0
            log('TURN second click yaw', clicks[1][4], 'shot yaw', shots[1][4], 'off by', round(d, 2), 'waited', round(w));
    log('TURN shot away from what she was shown:', bad, 'of', n); c('presstrace lag 0'); sleep(0.2)
if want('qrep'):
    # 6: Q held with repeats while the ult fills, then the right button within a round trip of it filling, 100 ms: a shot shown and not fired?
    for i, gap in enumerate((0.12, 0.16, 0.20, 0.24, 0.12, 0.16, 0.20, 0.24)):
        ready(); c('presstrace lag 100'); sleep(0.2); c('presstrace energy 50'); sleep(0.3); c('press key.keyboard.q down'); sleep(0.05)
        for k in range(4): c('press key.keyboard.q repeat'); sleep(0.02)
        c('presstrace start'); c('cmd /giveult @s'); t0 = time.time(); pressed = False
        while time.time() - t0 < 0.6:
            c('press key.keyboard.q repeat')
            if not pressed and time.time() - t0 >= gap:
                c('key primary_fire down'); c('key primary_fire up'); pressed = True
            sleep(0.01)
        c('press key.keyboard.q up'); sleep(0.3); tr = c('presstrace').get('trace', ''); k = counts(tr)
        log('QREP gap %.2f' % gap, k, 'GHOST' if k['fx'] > k['srv'] else '', 'ult started' if 'srv_ult' in tr else 'no ult', '|', tr[:200]); c('presstrace lag 0'); sleep(10)
if want('mash'):
    # 5: real mouse: clicks at a rate in Overclock: gaps between shots; refused presses
    for cps, up in ((15, 30), (15, 45), (20, 25), (10, 40)):
        ready(); mc('/giveult @s', 0.1); tap('ultimate', 0.05); sleep(0.85); c('presstrace start')
        r = subprocess.run(['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', SP + 'rg_mouse.ps1', 'mash', str(cps), str(up), '5'], capture_output=True, text=True); sleep(0.2)
        tr = c('presstrace').get('trace', ''); m = parse(tr); S = [t for t, x in m if x == 'srv_shot']; F = [t for t, x in m if x == 'fx']; Rf = [t for t, x in m if x == 'refused']; g = [round(b - a) for a, b in zip(S, S[1:])]
        log('MASH real %d cps, key up %d ms: %s; shots %d, looks %d, refused presses %d, gaps' % (cps, up, r.stdout.strip() or r.stderr.strip()[:80], len(S), len(F), len(Rf)), g, 'fps', st().get('fps')); sleep(6)
if want('mash3'):
    # 1, 5a, as round three
    pass
if want('flickreal'):
    # the real mouse at speed: the harness starts her (W, Shift, keys through the handlers), a real burst and click in one frame. Does the server agree with the ray she pressed on?
    mc('/fill -30 -61 -40 60 -61 20 minecraft:stone', 0.3); mc('/fill -30 -60 -40 60 -50 20 minecraft:air', 0.3); mc('/kill @e[tag=rgf]', 0.2); mc('/kill @e[tag=rgz]', 0.2)
    ZX = [x + 0.5 for x in range(-8, 28, 4)]
    for x in ZX: summon('rgf', x, -60, -12.5)
    def inray(cam, yaw):
        # the client's ray against the zombies' boxes, inflated by the shot's radius, flat (pitch 0, zombies 1.95 m high from -60)
        dx, dz = -math.sin(math.radians(yaw)), math.cos(math.radians(yaw)); best = None
        for zx in ZX:
            lo = (zx - 0.41 - cam[0], -12.5 - 0.41 - cam[2]); hi = (zx + 0.41 - cam[0], -12.5 + 0.41 - cam[2]); t0, t1 = 0.0, 1e9
            for d, l, h in ((dx, lo[0], hi[0]), (dz, lo[1], hi[1])):
                if abs(d) < 1e-6:
                    if l > 0 or h < 0: t1 = -1
                    continue
                a_, b_ = l / d, h / d
                t0, t1 = max(t0, min(a_, b_)), min(t1, max(a_, b_))
            if t0 <= t1: return True, 0
            miss = t0 - t1
            best = miss if best is None else min(best, miss)
        return False, round(best, 2)
    def trial(kind):
        flag = SP + 'rg_fire.flag'
        if os.path.exists(flag): os.remove(flag)
        p = subprocess.Popen(['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', SP + 'rg_mouse.ps1', 'armed', flag, '200'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        guard(); mc('/overcraft reset', 0.05); mc('/tp @s %.1f -60 0.5 -90 0' % (-14 + 4 * (time.time() * 7 % 1)), 0.15); c('look -90 0'); sleep(1.3); c('presstrace energy 50'); sleep(0.12); hs0 = sum(hp('@e[tag=rgf,sort=nearest,limit=1,x=%s,y=-60,z=-12.5,distance=..0.5]' % x) or 0 for x in []) ; c('presstrace start'); c('trace start')
        c('press key.keyboard.w down'); sleep(0.05)
        if kind == 'run': sleep(0.45)
        if kind == 'slide': c('key ability_1 down'); sleep(0.04); c('key ability_1 up'); sleep(0.27)
        open(flag, 'w').close(); sleep(0.12); c('press key.keyboard.w up'); out = p.communicate(timeout=10); sleep(0.5)
        tr = c('presstrace').get('trace', ''); m = parse(tr); tt = c('trace').get('trace', '').splitlines(); hs = [float(l.split()[1]) for l in tt if len(l.split()) > 1]
        ck = [x.split() for t, x in m if x.startswith('click')]; eye = [x.split() for t, x in m if x.startswith('srv_eye')]; hit = sum(1 for t, x in m if x == 'snd:charged.hit')
        if not ck or not eye: log('FLICKREAL', kind, 'no shot', out[1][:80], tr[:120]); return None
        cam = [float(q) for q in ck[0][1:4]]; yaw = float(ck[0][4]); e = [float(q) for q in eye[0][1:4]]; d = sum((a - b) ** 2 for a, b in zip(cam, e)) ** 0.5
        want_hit, off = inray(cam, yaw)
        agree = want_hit == (hit > 0)
        log('FLICKREAL', kind, 'top speed', max(hs) if hs else None, 'yaw', yaw, 'camera to shot eye', round(d, 3), 'her ray', 'on' if want_hit else 'off', 'a box by', off, 'server hit' if hit else 'server miss', 'AGREE' if agree else 'DISAGREE'); sleep(0.4)
        return agree
    for kind in ('slide', 'run'):
        r = [trial(kind) for i in range(14 if kind == 'slide' else 8)]; r = [x for x in r if x is not None]
        log('FLICKREAL', kind, 'agree', sum(r), 'of', len(r))
    c('press key.keyboard.w up')
log('### done')

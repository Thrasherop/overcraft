# The last narrow round of #126: slide-jump flick at 15 fps and normal, Esc then a mash, a buffered shown press and a step back. Harness keys, dev sends, OS mouse events.
import subprocess
SP = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
SRC = open(SP + "rg3.py", encoding="utf8").read()
A6 = ARGS; ARGS = ['none']; exec(SRC); NAMES = A6 or None
log('### last round', time.strftime('%H:%M:%S'))
if want('jump'):
    mc('/fill -60 -61 -20 60 -61 40 minecraft:stone', 0.3); mc('/fill -60 -60 -20 60 -48 40 minecraft:air', 0.3); mc('/kill @e[type=minecraft:zombie]', 0.2)
    mc('/summon minecraft:zombie 0.5 -60 12.5 {NoAI:1b,Tags:["rgs"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}', 0.3)
    Zs = '@e[tag=rgs,sort=nearest,limit=1]'; T = '0.5 -58.9 12.5'
    for fps in (15, 120):
        c('presstrace fps %d' % fps); sleep(1.0); hits = 0; n = 0
        for i in range(5):
            guard(); mc('/overcraft reset', 0.08); mc('/tp @s -9 -60 0.5 -90 0', 0.15); c('look -90 0'); sleep(0.3); h0 = hp(Zs); c('presstrace energy 50'); sleep(0.15); c('presstrace start')
            c('press key.keyboard.w down'); sleep(0.05); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.30); c('key jump down'); sleep(0.05); c('key jump up'); sleep(0.07)
            c('presstrace aimpress ' + T); sleep(0.05); c('press key.keyboard.w up'); sleep(0.5); h1 = hp(Zs); tr = c('presstrace').get('trace', ''); m = parse(tr)
            cam = [x for t, x in m if x.startswith('press cam')]; eye = [x for t, x in m if x.startswith('srv_eye')]
            d = sum((float(p) - float(q)) ** 2 for p, q in zip(cam[0].split()[2:5], eye[0].split()[1:4])) ** 0.5 if cam and eye else None
            n += 1; hits += 1 if h0 != h1 else 0
            log('JUMP fps', fps, 'measured', st().get('fps'), 'hit' if h0 != h1 else 'MISS', 'camera to shot eye', None if d is None else round(d, 3)); sleep(0.4)
        log('JUMP fps', fps, 'hits', hits, 'of', n)
    c('presstrace fps 120'); sleep(0.5)
if want('esc'):
    rig()
    for i in range(2):
        ready(); mc('/giveult @s', 0.1); tap('ultimate', 0.05); sleep(0.9); c('presstrace energy 100'); tap('primary_fire', 0.03); sleep(0.05)
        c('press key.keyboard.escape'); sleep(2.0); s = st(); c('close'); sleep(0.3); rtt = c('presstrace rtt').get('rtt'); c('presstrace start')
        r = subprocess.run(['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', SP + 'rg_mouse.ps1', 'mash', '15', '30', '3'], capture_output=True, text=True); sleep(0.2)
        tr = c('presstrace').get('trace', ''); m = parse(tr); S = [t for t, x in m if x == 'srv_shot']; K = [t for t, x in m if x.startswith('click')]
        log('ESC screen during it', s.get('screen'), 'rtt after', rtt, 'ms; mash', r.stdout.strip() or r.stderr.strip()[:60], ': shots', len(S), 'shown at the press', len(K), 'gaps', [round(b - a) for a, b in zip(S, S[1:])]); sleep(6)
if want('stepback'):
    rig()
    for i in range(2):
        ready(); mc('/tp @s 0.5 -60 0.5 0 0', 0.2); c('look 0 0'); sleep(0.3); c('presstrace energy 50'); sleep(0.15); c('presstrace start'); t0 = time.time(); tap('primary_fire', 0.02); sleep(0.2); c('presstrace energy 50')
        while time.time() - t0 < 0.58: sleep(0.001)
        c('presstrace send 2 32 0 0 1'); mc('/tp @s 6.5 -60 0.5 0 0', 0.05); c('presstrace send 0 0 0 0 1'); sleep(0.6)
        tr = c('presstrace').get('trace', ''); log('STEPBACK pressed at x 0.5, moved to 6.5 while it waited: shots', eyes_ := [x for t, x in parse(tr) if x.startswith('srv_eye')], '|', tr[:200]); sleep(0.5)
log('### done')

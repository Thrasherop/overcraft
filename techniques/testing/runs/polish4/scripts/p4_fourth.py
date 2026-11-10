# Fourth round's repros and afters. ONLY: falls, shots, deploy, los, halo, bash. Harness keys, bots server-side.
XSTART = globals().get('XSTART', 40000)
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
ONLY = globals().get('ONLY')
def want(k): return ONLY is None or k in ONLY
log('TREE (git at launch):', open(r'E:/claude/mods/overcraft_polish4/run/p4_launch_tree.txt').read().strip(), time.strftime('%H:%M:%S'))
if 'error' in bot('cc me'):
    c('close'); sleep(1.5); c('close'); sleep(1); log(c('newworld ' + globals().get('WORLD', 'fourth_a')))
    for i in range(60):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    sleep(1); c('close'); sleep(1)
setup(); mc('/gamemode survival @s'); mc('/time set midnight'); mc('/gamerule minecraft:natural_health_regeneration false')
def land_wait(secs=6):
    t0 = time.time(); top = st()['y']
    while time.time() - t0 < secs:
        s = st(); top = max(top, s['y'])
        if s.get('onGround') and time.time() - t0 > 0.5: break
        sleep(0.05)
    sleep(0.4); return top
def settle(hero, x, z, yaw=0, pitch=0, y=-60):
    me(hero, x, z, yaw, pitch); mc('/gamerule minecraft:natural_health_regeneration false'); tp(x, y, z, yaw, pitch); c('look %s %s' % (yaw, pitch)); sleep(1.6); c('heard')
if want('falls'):
    log('### falls after movement abilities: fall damage shows in the pain line (Lucio heals himself: the line, not the health)')
    def lucio_ride(label, wall_top, off=None):
        for i in range(2):
            x = spot(); settle('lucio', x - 8, 1.2, -90, 0)
            mc('/fill %d -60 2 %d %d 2 minecraft:stone' % (x - 12, x, wall_top), 0.2)
            press_song = False
            s0 = st(); c('key jump down'); t0 = time.time(); top = s0['y']; was = False
            while time.time() - t0 < 2.5:
                s = st(); top = max(top, s['y'])
                if off and s['y'] > s0['y'] + off: break
                sleep(0.03)
            c('key jump up')
            if off: c('key jump down'); sleep(0.08); c('key jump up')
            tt = land_wait(); s1 = st(); h = heard(''); log('  %s: y %.2f, top %.2f, x %.1f -> %.1f, landed y %.2f, health %.1f -> %.1f, pain %s' % (label, s0['y'], max(top, tt), s0['x'], s1['x'], s1['y'], s0['health'], s1['health'], [n for n in h if 'pain' in n or 'fall' in n]))
    lucio_ride('Lucio, ride off the wall\'s end (wall 8 m tall)', -52)
    lucio_ride('Lucio, jump off the wall in the ride', -52, 1.5)
    def ride_high():
        # a platform 9 m up, ending where the wall on it ends: he rides west off both
        for i in range(2):
            x = spot(r=20); mc('/fill %d -60 -3 %d -52 3 minecraft:stone' % (x - 12, x + 4), 0.2); mc('/fill %d -51 2 %d -44 2 minecraft:stone' % (x - 12, x), 0.2); settle('lucio', x - 8, 1.2, -90, 0, -51)
            s0 = st(); c('key jump down'); t0 = time.time(); top = s0['y']; low = s0['x']
            while time.time() - t0 < 3:
                s_ = st(); top = max(top, s_['y']); low = min(low, s_['x'])
                if s_['y'] < -53: break
                sleep(0.03)
            c('key jump up'); land_wait(); s1 = st(); h = heard(''); log('  Lucio, off a wall and platform 9 m up: y %.2f, top %.2f -> %.2f, x %.1f -> %.1f, health %.1f -> %.1f, ride %s, pain %s' % (s0['y'], top, s1['y'], s0['x'], s1['x'], s0['health'], s1['health'], [n for n in h if 'wallride.start' in n], [n for n in h if 'pain' in n or 'fall' in n]))
    ride_high()
    # control: plain drop
    x = spot(); mc('/fill %d -60 -3 %d -55 3 minecraft:stone' % (x - 3, x + 3), 0.2); settle('lucio', x, 0.5, 0, 0, -54); s0 = st(); c('key forward down'); sleep(1.5); c('key forward up'); land_wait(); s1 = st(); log('  control, Lucio walks off a 6 m ledge: y %.2f -> %.2f, health %.1f -> %.1f, pain %s' % (s0['y'], s1['y'], s0['health'], s1['health'], [n for n in heard('') if 'pain' in n]))
    # Jetpack Cat: climb high, let go
    for i in range(2):
        x = spot(); settle('jetpack_cat', x, 0.5, 0, 0); s0 = st(); c('key jump down'); sleep(3.0); c('key jump up'); top = land_wait(10); s1 = st(); log('  Jetpack Cat, climb then let go: top %.1f, y %.2f -> %.2f, health %.1f -> %.1f, pain %s' % (top, s0['y'], s1['y'], s0['health'], s1['health'], [n for n in heard('') if 'pain' in n]))
    # Brigitte: bash off a 6 m ledge
    for i in range(2):
        x = spot(); mc('/fill %d -60 -3 %d -55 3 minecraft:stone' % (x - 4, x + 4), 0.2); settle('brigitte', x, 2.0, 0, 0, -54); s0 = st(); c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); sleep(0.5); c('key primary_fire up'); land_wait(); s1 = st(); log('  Brigitte, bash off a 6 m ledge: z %.1f -> %.1f, y %.2f -> %.2f, health %.1f -> %.1f, pain %s' % (s0['z'], s1['z'], s0['y'], s1['y'], s0['health'], s1['health'], [n for n in heard('') if 'pain' in n]))
    # Kiriko: Swift Step to an ally high up, who then isn't there to stand on
    for i in range(2):
        x = spot(); mc('/fill %d -40 7 %d -40 7 minecraft:stone' % (x, x), 0.2); settle('kiriko', x, 0.5, 0, -60); a = add('high', 'mercy', 'blue', x + 0.5, 7.5, 0, 0, -39); sleep(0.5); c('look 0 -68'); s0 = st(); press('ability_1', 0.1); sleep(0.5); s1 = st(); top = land_wait(8); s2 = st(); log('  Kiriko, Swift Step up to an ally on a one-block perch 20 m up: arrived y %.2f, z %.2f; ended y %.2f, health %.1f -> %.1f, pain %s (cd1 %s)' % (s1['y'], s1['z'], s2['y'], s0['health'], s2['health'], [n for n in heard('') if 'pain' in n], st().get('ability1Cooldown')))
        if s1['y'] > -45:
            c('key forward down'); sleep(0.5); c('key forward up'); land_wait(8); s3 = st(); log('    stepped off: y %.2f, health %.1f, pain %s' % (s3['y'], s3['health'], [n for n in heard('') if 'pain' in n]))
if want('shots'):
    log('### a shot at a phased target: the client copy and the server copy, read together. The real client shoots a bot 5 m off, a zombie 13 m behind it')
    def shot(kind, hero, key, hold, n=2, pitch=-1):
        for phased in (False, True) * n:
            x = spot(); me(hero, x, 0.5, 0, 0); c('look 0 %s' % pitch); mc('/time set midnight')
            t = add('tgt', 'mercy', 'red', x, 5.5, 180)
            mc('/summon minecraft:zombie %s -60 18 {NoAI:1b,Tags:["bz"],Health:200f,attributes:[{id:"minecraft:max_health",base:200}]}' % x, 0.1); sleep(0.2)
            if phased: bot('phase %s 1.8' % t)
            c('key %s down' % key); sleep(hold); c('key %s up' % key); t0 = time.time(); rows_ = []
            while time.time() - t0 < 0.9:
                q = bot('shots'); rows_.append((round(time.time() - t0, 2), q.get('client', ''), q.get('server', ''))); sleep(0.01)
            def far(col):
                zs = [float(p_.split(',')[-1]) for r in rows_ for p_ in r[col].split() if '@' in p_]
                return max(zs) if zs else None
            log('  %-10s phased %-5s client copy reached z %s, server copy z %s | target %s | zombie %s' % (kind, phased, far(1), far(2), sh(t, 'health'), hp('@e[tag=bz,sort=nearest,limit=1]')))
            mc('/kill @e[tag=bz]', 0.1); c('key %s up' % key)
    shot('ana dart', 'ana', 'secondary_fire', 0.1); shot('sleep dart', 'ana', 'ability_1', 0.1); shot('kunai', 'kiriko', 'secondary_fire', 0.15); shot('railgun', 'sojourn', 'secondary_fire', 0.15); shot('sonic', 'lucio', 'secondary_fire', 0.2); shot('thorn', 'lifeweaver', 'primary_fire', 0.3); shot('paw', 'jetpack_cat', 'secondary_fire', 0.15); shot('blaster', 'juno', 'secondary_fire', 0.15, 1)
if want('deploy'):
    log('### Sombra\'s EMP and a deployable whose owner is phased')
    for phased in (False, True, False, True):
        x = spot(); clear(); pick('sombra'); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(x, -60, 0.5, 0, 0)
        l = add('lw', 'lifeweaver', 'red', x + 2, 8, 180, 60); sleep(0.2); r = bot('press %s ability1' % l); sleep(1.6); dep = bot('shots').get('deployables')
        if phased: bot('phase %s 2' % l)
        mc('/giveult'); press('ultimate', 0.1); sleep(1.0); log('  owner phased', phased, 'before', dep, '| after EMP', bot('shots').get('deployables'), '| owner', sh(l, 'phased', 'silenced'))
if want('los'):
    log('### Rally\'s shockwave at a zombie whose feet are in sight and whose eyes aren\'t: a floating wall at eye height')
    for block in (False, True, False, True):
        x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0)
        mc('/summon minecraft:zombie %s -60 2.7 {NoAI:1b,Tags:["a"],Health:20f}' % x, 0.1); mc('/summon minecraft:zombie %s -60 8.5 {NoAI:1b,Tags:["w"],Health:20f}' % (x + 2), 0.1)
        if block: mc('/fill %d -59 7 %d -57 7 minecraft:stone' % (x - 3, x + 6), 0.2)
        mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.7); c('key secondary_fire up'); sleep(0.2); c('key primary_fire up'); sleep(0.2)
        log('  floating wall', block, 'bashed', hp('@e[tag=a,sort=nearest,limit=1]'), 'past the wall', hp('@e[tag=w,sort=nearest,limit=1]')); mc('/kill @e[tag=a]', 0.1); mc('/kill @e[tag=w]', 0.1)
if want('halo'):
    log('### a Rally bash on a sleeper: the stun is refused (Holds); does its sound or halo play?')
    for i in range(2):
        x = spot(); me('mercy', x, 3.5, 180, 0); mc('/time set midnight'); n = add('anaR', 'ana', 'red', x + 1, 9.5, 180 - 8); b = add('brigR', 'brigitte', 'red', x, 1.7, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5)
        bot('press %s ability1' % n); t0 = time.time()
        while not cc('me').get('sleeping') and time.time() - t0 < 1.5: sleep(0.02)
        q0 = sh('me', 'sleeping', 'asleep'); c('heard'); inp(b, 3, 0); sleep(0.5); h = heard(''); held = bot('held me'); inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b); log('  before %s, heard %s, held %s, after %s' % (q0, [n2 for n2 in h if 'stun' in n2 or 'bash' in n2], held, sh('me', 'sleeping', 'asleep', 'health')))
if want('bash'):
    log('### Shield Bash\'s push: a bot Brigitte at a real client; a real one at a husk with AI (speed 0)')
    for mode in ('on', 'on', 'off', 'off'):
        x = spot(); clear(); pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 180, 0); c('look 180 0'); c('heromode ' + mode); sleep(0.3)
        b = add('brig', 'brigitte', 'red', x, -1.3, 0); sleep(0.2); s0 = st(); bot('secondary %s on' % b); sleep(0.6); c('trace start'); inp(b, 3, 0); sleep(0.1); inp(b, 2, 0); inp(b, 0, 0); sleep(1.5); bot('secondary %s off' % b); tr = c('trace').get('trace', '').splitlines(); s1 = st(); log('  bash on hero mode %s: moved %.2f m, y top %s, health %.1f -> %.1f' % (mode, abs(s1['z'] - s0['z']), max(float(l.split()[4]) for l in tr) if tr else None, s0['health'], s1['health']))
    c('heromode on')
    for i in range(2):
        x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0); c('look 0 0')
        mc('/summon minecraft:husk %s -60 3 {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); sleep(0.3)
        c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); sleep(1.0); c('key primary_fire up'); l = mc('/data get entity @e[tag=hk,sort=nearest,limit=1] Pos', 0.2)[-1:]; log('  bash on a husk from 2.5 m: pos', l[0][-50:] if l else None, 'health', hp('@e[tag=hk,sort=nearest,limit=1]')); mc('/kill @e[tag=hk]', 0.1)
clear(); log('DONE')

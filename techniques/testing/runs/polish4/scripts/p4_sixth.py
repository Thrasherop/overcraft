# Fifth round's repros and afters. ONLY: softg tunnel wetdrop whipmc whipmob sight stepair. Harness keys, bots server-side, no second client.
XSTART = globals().get('XSTART', 40000)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_third.py", encoding="utf8").read()
ONLY_X = globals().get('ONLY', None)
ONLY = ('none',)
exec(src.replace("clear(); log('DONE')\n", ""))
def wantx(k): return ONLY_X is None or k in ONLY_X
def land_wait(secs=6):
    t0 = time.time()
    while time.time() - t0 < secs:
        s = st()
        if s.get('onGround') and time.time() - t0 > 0.3: break
        sleep(0.05)
    sleep(0.4)
def pains(): return [n for n in heard('') if 'pain' in n or 'fall' in n]
RODE = [False]
def ride_off(height=9):
    """A platform `height` m up, the same length as the wall on it: Lucio rides off its end, either way. Up to 3 tries till the ride starts and he's 2 m below it."""
    for attempt in range(3):
        x = spot(r=20); top = -60 + height
        mc('/fill %d -60 -3 %d %d 3 minecraft:stone' % (x - 12, x, top - 1), 0.2); mc('/fill %d %d 2 %d %d 2 minecraft:stone' % (x - 12, top, x, top + 7), 0.2)
        me('lucio', x - 6, 1.2, -90, 0); mc('/gamerule minecraft:natural_health_regeneration false'); mc('/time set midnight'); tp(x - 6, top, 1.2, -90, 0); c('look -90 0'); sleep(1.6); c('heard')
        h0 = st()['health']; c('key jump down'); t0 = time.time(); off = False
        while time.time() - t0 < 4:
            if st()['y'] < top - 2: off = True; break
            sleep(0.02)
        c('key jump up')
        RODE[0] = off
        if off: return x, h0
        c('key jump up'); sleep(0.3)
    return x, h0
if wantx('softg'):
    log('### a soft landing and G: ride off a wall 9 m up; G in the air; landing in plain Minecraft. Health is read in Minecraft mode, where the song is off')
    for g in (True,) * 6:
        x, h0 = ride_off()
        if g: c('heromode off')
        land_wait(); s1 = st(); h_ = heard(''); log('  Lucio, G in the air %s: off %s, ride %s, y end %.2f, health %.1f -> %.1f, hero mode %s, pain %s' % (g, RODE[0], [n for n in h_ if 'wallride.start' in n], s1['y'], h0, s1['health'], s1.get('heroMode'), [n for n in h_ if 'pain' in n])); c('heromode on')
    log('  control: plain Minecraft Lucio walks off the same 9 m')
    x = spot(r=20); mc('/fill %d -60 -3 %d -52 3 minecraft:stone' % (x - 12, x + 4), 0.2); me('lucio', x - 11, 0.5, 90, 0); tp(x - 11, -51, 0.5, 90, 0); c('look 90 0'); sleep(1.0); c('heromode off'); sleep(0.5); h0 = st()['health']; c('key forward down'); sleep(1.8); c('key forward up'); land_wait(); s1 = st(); log('  y %.2f, health %.1f -> %.1f, pain %s' % (s1['y'], h0, s1['health'], pains())); c('heromode on')
    for g in (True, False):
        x = spot(); mc('/fill %d -60 -3 %d -52 3 minecraft:stone' % (x - 4, x + 4), 0.2); me('brigitte', x, 2.0, 0, 0); mc('/time set midnight'); tp(x, -51, 2.0, 0, 0); c('look 0 0'); sleep(1.6); c('heard'); h0 = st()['health']; c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); t0 = time.time()
        while st()['y'] > -53 and time.time() - t0 < 2: sleep(0.02)
        c('key primary_fire up')
        if g: c('heromode off')
        land_wait(); s1 = st(); log('  Brigitte, bash off 9 m, G in the air %s: y end %.2f, health %.1f -> %.1f, pain %s' % (g, s1['y'], h0, s1['health'], pains())); c('heromode on')
if wantx('stepair'):
    log('### Swift Step that leaves her in the air: a bot ally on a one-block perch 20 m up; the perch and bot go as she arrives. Then G or not')
    for g in (False, True, False, True, False, False):
        x = spot(); mc('/fill %d -40 7 %d -40 7 minecraft:stone' % (x, x), 0.2); me('kiriko', x, 0.5, 0, 0); mc('/time set midnight'); tp(x, -60, 0.5, 0, -60); a = add('high', 'mercy', 'blue', x + 0.5, 7.5, 0, 0, -39); sleep(0.5); c('look 0 -68'); sleep(1.0); c('heard'); h0 = st()['health']; c('key ability_1 down'); mc('/fill %d -40 7 %d -39 7 minecraft:air' % (x, x), 0.0); c('key ability_1 up'); sleep(0.15); s1 = st()
        if g: c('heromode off')
        land_wait(8); s2 = st(); log('  G %s: arrived y %.2f; end y %.2f, health %.1f -> %.1f, pain %s, cd1 %s' % (g, s1['y'], s2['y'], h0, s2['health'], pains(), st().get('ability1Cooldown'))); c('heromode on')
if wantx('tunnel'):
    log('### Whiplash in a 2-high tunnel: a real client, a bot Brigitte with the perk 5 m behind. Nothing ahead for 20 m but where said')
    def tunnel(endwall=None, side=True):
        def b(x):
            mc('/fill %d -58 -8 %d -58 22 minecraft:stone' % (x - 3, x + 3), 0.2)
            if side: mc('/fill %d -60 -8 %d -59 22 minecraft:stone' % (x + 1, x + 1), 0.2)
            if endwall: mc('/fill %d -60 %d %d -59 %d minecraft:stone' % (x - 3, endwall, x + 3, endwall), 0.2)
        return b
    def run(label, yaw, build, xo=0.2, n=2, mode='on', perk=True, wait=2.0):
        for i in range(n):
            x = spot(); clear(); build(x)
            pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x + xo, -60, 0.5, 180, 0); c('look 180 0'); c('heromode ' + mode); sleep(0.3)
            b = add('brig', 'brigitte', 'red', x, -4.5, yaw, 0)
            if perk: bot('perk %s BRIGITTE_WHIPLASH' % b)
            sleep(0.2); s0 = st(); bot('press %s ability1' % b); sleep(wait); s1 = st()
            log('  %s: x %.2f -> %.2f, z %.2f -> %.2f, health %.1f -> %.1f (14.4 whip, 9.6 with the slam)' % (label, s0['x'] - x, s1['x'] - x, s0['z'], s1['z'], s0['health'], s1['health']))
        c('heromode on')
    run('touching the side wall, whipped straight along it', 0, tunnel())
    run('touching the side wall, whipped 3 degrees into it', -3, tunnel())
    run('whipped 8 degrees into it', -8, tunnel())
    run('no side wall, the ceiling alone, straight', 0, tunnel(side=False), xo=0.0)
    run('control: 2-high, end wall 5 m ahead, Whiplash (a real wall)', 0, tunnel(endwall=5), xo=0.0)
    run('control: 3 degrees into the side wall, no perk', -3, tunnel(), perk=False, n=1)
    husk_run('tunnel, husk against the side, straight, Whiplash', build=tunnel(), perk=True, n=1)
    husk_run('tunnel, end wall at 4, husk, Whiplash', build=tunnel(endwall=4, side=False), perk=True, n=1)
def bash_off(pool=False):
    x = spot(r=24); mc('/fill %d -60 -3 %d -52 3 minecraft:stone' % (x - 4, x + 4), 0.2)
    if pool: mc('/fill %d -60 4 %d -59 24 minecraft:water' % (x - 4, x + 4), 0.2)
    me('brigitte', x, 2.0, 0, 0); mc('/time set midnight'); tp(x, -51, 2.0, 0, 0); c('look 0 0'); sleep(1.6); c('heard'); h0 = st()['health']; c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); t0 = time.time()
    while st()['y'] > -53 and time.time() - t0 < 2: sleep(0.02)
    c('key primary_fire up'); return x, h0
if wantx('wetdrop'):
    log('### a soft landing that ends in water or on a ladder, then a drop (the climb out stood in by /tp 12 m up over a stone floor). Brigitte bashes off a 9 m ledge: 12 m with 3 safe hurts 9 if it counts')
    for where in ('water', 'ladder', 'ground') * 2:
        x, h0 = bash_off(where == 'water')
        if where == 'ladder':
            mc('/fill %d -60 12 %d -50 12 minecraft:stone' % (x, x), 0.05); mc('/fill %d -60 11 %d -50 11 minecraft:ladder[facing=north]' % (x, x), 0.05); mc('/tp @s %s -54 11.5' % x, 0.05); sleep(0.5)
        else:
            t0 = time.time()
            while time.time() - t0 < 2:
                s_ = st()
                if s_.get('onGround') or s_['y'] < -59: break
                sleep(0.03)
            sleep(0.3)
        s_ = st(); mid = (s_['y'], s_.get('onGround'), s_['health'])
        xx = x + 2; mc('/fill %d -61 40 %d -61 44 minecraft:stone' % (xx - 2, xx + 2), 0.05); c('heard'); mc('/tp @s %d -48 42' % xx, 0.1); t0 = time.time()
        while time.time() - t0 < 4 and not (st().get('onGround') and time.time() - t0 > 0.4): sleep(0.04)
        sleep(0.4); s2 = st(); log('  %s: before the drop y %.2f on ground %s, health %.1f; after 12 m: y %.2f, health -> %.1f, pain %s' % (where, mid[0], mid[1], mid[2], s2['y'], s2['health'], pains()))
if wantx('exits'):
    log('### death and dimension change in a soft landing (Brigitte, bash off 9 m)')
    x, h0 = bash_off(); mc('/kill @s', 0.3); sleep(1.0); c('respawn'); sleep(1.5); c('close'); s_ = st(); mc('/fill %d -61 40 %d -61 44 minecraft:stone' % (x, x + 4), 0.05); c('heard'); mc('/tp @s %d -48 42' % (x + 2), 0.1); land_wait(4); s2 = st(); log('  died in it, respawned as %s, then 12 m: y %.2f, health -> %.1f of %.1f, pain %s' % (s_.get('hero'), s2['y'], s2['health'], s_['health'], pains()))
    x, h0 = bash_off(); mc('/execute in minecraft:the_nether run forceload add 5000 5000', 0.1); mc('/execute in minecraft:the_nether run fill 4998 100 4998 5002 100 5002 minecraft:stone', 0.3); mc('/execute in minecraft:the_nether run tp @s 5000 112 5000', 0.2); c('heard'); sleep(0.5); land_wait(5); s2 = st(); log('  Nether in it, 11 m: y %.2f, health %.1f -> %.1f, on ground %s, pain %s' % (s2['y'], h0, s2['health'], s2.get('onGround'), pains())); mc('/execute in minecraft:overworld run tp @s 40 -55 0', 0.5); sleep(2)
if wantx('whipmc'):
    log('### Whiplash on a Minecraft-mode target; on a stair or slab against a mob; a real wall against a mob')
    def one(block, z=3):
        def b(x): mc('/fill %d -60 %d %d -60 %d minecraft:%s' % (x - 4, z, x + 4, z, block), 0.2)
        return b
    hero_run('Minecraft mode, wall at 3 m + Whiplash', 'off', build=wall(3), perk=True, n=2); hero_run('Minecraft mode, no wall + Whiplash', 'off', perk=True, n=1); hero_run('Minecraft mode, one slab + Whiplash', 'off', build=one('oak_slab'), perk=True, n=1)
    husk_run('one stair + Whiplash', build=one('oak_stairs[facing=south]', 4), perk=True, n=2); husk_run('one slab + Whiplash', build=one('oak_slab', 4), perk=True, n=2); husk_run('wall + Whiplash', build=wall(4), perk=True, n=1)
if wantx('sight'):
    log('### Rally\'s wave at a zombie with only its feet in sight: floating wall z 7, zombie z 8.5, x + 2; bot sight read from where she stands')
    for block in (False, True, False, True, True):
        x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0)
        mc('/summon minecraft:zombie %s -60 2.7 {NoAI:1b,Tags:["a"],Health:20f}' % x, 0.1); mc('/summon minecraft:zombie %s -60 8.5 {NoAI:1b,Tags:["w"],Health:20f}' % (x + 2), 0.1)
        if block: mc('/fill %d -59 7 %d -57 7 minecraft:stone' % (x - 3, x + 6), 0.2)
        mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); s0 = bot('sight').get('sight', ''); c('key secondary_fire down'); sleep(0.7); c('key secondary_fire up'); s1 = bot('sight').get('sight', ''); sleep(0.2); c('key primary_fire up'); sleep(0.2)
        log('  floating wall', block, 'bashed', hp('@e[tag=a,sort=nearest,limit=1]'), 'past the wall', hp('@e[tag=w,sort=nearest,limit=1]'), '| sight before', s0, '| after', s1); mc('/kill @e[tag=a]', 0.1); mc('/kill @e[tag=w]', 0.1)
clear(); log('DONE')

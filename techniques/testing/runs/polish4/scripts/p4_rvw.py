XSTART = globals().get('XSTART', 220000)
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
ONLY = globals().get('ONLY')
def want(k): return ONLY is None or k in ONLY
setup(); mc('/gamemode survival @s'); mc('/time set midnight'); mc('/gamerule minecraft:natural_health_regeneration false')
def voice_watch(secs=1.6):
    """When the Suzu line is heard after the throw, reading `heard` every 0.1 s."""
    t0 = time.time(); got = None
    while time.time() - t0 < secs:
        h = heard('voice.suzu')
        if h and got is None: got = round(time.time() - t0, 2)
        sleep(0.05)
    return got
if want('voice'):
    log('### Kiriko\'s Suzu line: a throw that hits nobody, at a healthy ally, at a slept ally; 3 rounds, 11 s apart for its own limit')
    for rnd in range(3):
        for case in ('nobody', 'healthy', 'slept', 'poisoned', 'stunned-no', 'hurt'):
            if case == 'stunned-no': continue
            x = spot(); me('kiriko', x, 0.5, 0, 0); mc('/overcraft reset'); c('heard')
            if case == 'nobody':
                c('look 0 75'); a = None
            else:
                a = add('ally', 'mercy', 'blue', x + 1, 3.5, 0); c('look 0 62')
            if case == 'slept':
                n = add('ana', 'ana', 'red', x + 1, 9, 180); bot('press %s ability1' % n); sleep(0.6); log('  asleep', sh(a, 'asleep'))
            if case == 'poisoned': mc('/effect give %s minecraft:poison 20 0' % a, 0.1)
            if case == 'hurt': bot('set %s 6 0' % a)
            sleep(0.2); c('heard'); press('ability_2', 0.1); g = voice_watch(); log('  round', rnd, case, 'cd2', round(st().get('ability2Cooldown', 0), 1), 'line heard at +', g, sh(a, 'phased', 'health', 'asleep') if a else '')
            sleep(9.5)
def whip_runs(label, mode, tag=None):
    for i in range(3):
        x = spot(); clear(); pick('kiriko'); mc('/gamemode survival @s'); mc('/time set midnight'); tp(x, -60, 0.5, 180, 0); c('look 180 0')
        c('heromode ' + mode); sleep(0.3)
        b = add('brig', 'brigitte', 'red', x, -4.5, 0, 0); sleep(0.2); s0 = st(); c('trace start'); bot('press %s ability1' % b); sleep(2.2); tr = c('trace').get('trace', ''); s1 = st(); log('  %s %s: hero moved %.2f m, z %.2f -> %.2f, y top %s, health %.1f -> %.1f' % (label, mode, abs(s1['z'] - s0['z']), s0['z'], s1['z'], max((float(l.split()[4]) for l in tr.splitlines()), default=None), s0['health'], s1['health']))
    c('heromode on')
def husk_runs(label, n=3):
    for i in range(n):
        x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, -4.5, 0, 0); c('look 0 0')
        mc('/summon minecraft:husk %s -60 0.5 {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); sleep(0.2)
        def z():
            l = mc('/data get entity @e[tag=hk,sort=nearest,limit=1] Pos', 0.2)[-1:]; m = re.search(r'\[([-\d.]+)d, ([-\d.]+)d, ([-\d.]+)d\]', l[0]) if l else None; return float(m.group(3)) if m else None
        z0 = z(); press('ability_1', 0.1); sleep(1.8); z1 = z(); log('  %s husk: moved %.2f m (z %.2f -> %.2f), health %s' % (label, abs(z1 - z0), z0, z1, hp('@e[tag=hk,sort=nearest,limit=1]'))); mc('/kill @e[tag=hk]', 0.1)
if want('whip'):
    log('### Whip Shot\'s push on flat ground: a bot Brigitte at a real client 5 m off, a real Brigitte at a husk')
    whip_runs('hold', 'on'); whip_runs('hold', 'off'); husk_runs('hold')
clear(); log('DONE')

# #128: one hero's first use of each button in a fresh client and world; the frames around each press (presstrace "f" marks).
# py -I fx128.py <hero> <out.txt> [wait seconds after the pick]
import json, socket, sys, time

hero, out = sys.argv[1], sys.argv[2]
hero_list = hero.split('+')
settle = float(sys.argv[3]) if len(sys.argv) > 3 else 8
log_lines = []


def log(*a):
    s = ' '.join(str(x) for x in a)
    print(s, flush=True)
    log_lines.append(s)


def connect():
    for _ in range(400):
        try:
            s = socket.create_connection(('127.0.0.1', 25599), timeout=60)
            return s, s.makefile('r', encoding='utf8')
        except OSError:
            time.sleep(1)
    raise SystemExit('no dev server')


sock, f = connect()


def c(cmd):
    sock.sendall((cmd + '\n').encode('utf8'))
    return json.loads(f.readline())


def st():
    return c('state')


for _ in range(300):
    s = st()
    if s.get('screen') in ('TitleScreen', 'AccessibilityOnboardingScreen'):
        break
    time.sleep(1)
time.sleep(3)
c('newworld fx128_%s_%d' % (hero, int(time.time()) % 100000))
for _ in range(300):
    if st().get('inWorld'):
        break
    time.sleep(1)
time.sleep(6)
c('close')
time.sleep(0.5)
c('fps 60')
c('servercmd time set day')
c('servercmd gamerule doMobSpawning false')
for hero in hero_list:
    c('hero ' + hero)
    t_pick = time.time()
    c('heromode on')
    time.sleep(1)
    c('close')
    c('look 0 0')
    c('servercmd execute at @p run summon minecraft:zombie ~ ~ ~9 {NoAI:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}')
    c('servercmd execute at @p run summon minecraft:villager ~-1.2 ~ ~3.5 {NoAI:1b,PersistenceRequired:1b}')
    c('presstrace frames on')
    # the frames while the pick's loading and any art made ahead settle
    c('presstrace start')
    while time.time() - t_pick < settle:
        time.sleep(0.2)
    tr = c('presstrace').get('trace', '')


    def frames(tr):
        ts = []
        for part in tr.split(' | '):
            bits = part.split(' ', 1)
            if len(bits) == 2 and bits[1] in ('f', 'frame'):
                ts.append(float(bits[0]))
        return ts


    def gaps(ts):
        return [round(b - a, 1) for a, b in zip(ts, ts[1:])]


    g = gaps(frames(tr))
    art = c('presstrace art')
    log('HERO', hero, 'fps', st().get('fps'), 'pick settle %.0f s: frames %d, max %s ms, over 50: %s' % (settle, len(g) + 1, max(g) if g else None, [x for x in g if x > 50]), 'art left', art.get('art'), 'bytes', art.get('artBytes'))

    ACTIONS = [('primary_fire', 0.4), ('secondary_fire', 0.4), ('ability_1', 0.1), ('ability_2', 0.1), ('ultimate', 0.1), ('reload', 0.1), ('quick_melee', 0.1)]
    for name, hold in ACTIONS:
        if name == 'ultimate':
            c('ult 100')
            time.sleep(0.6)
        c('look 0 0')
        time.sleep(0.3)
        c('presstrace start')
        time.sleep(0.2)
        c('key %s down' % name)
        time.sleep(hold)
        c('key %s up' % name)
        if name == 'ultimate':
            # an ult that waits for a confirm (a target, a throw) gets the fire button
            time.sleep(0.5)
            c('key primary_fire down'); time.sleep(0.1); c('key primary_fire up')
        time.sleep(2.0)
        tr = c('presstrace').get('trace', '')
        ts = frames(tr)
        g = gaps(ts)
        misses = [p.split(' ', 1)[1][9:] for p in tr.split(' | ') if ' art_miss:' in p]
        log('  %-14s frames %3d  max %6s ms  over 50: %-28s first-use misses %d' % (name, len(ts), max(g) if g else None, [x for x in g if x > 50], len(misses)))
        time.sleep(1.5 if name != 'ultimate' else 5)
c('presstrace frames off')
open(out, 'a', encoding='utf8').write('\n'.join(log_lines) + '\n')

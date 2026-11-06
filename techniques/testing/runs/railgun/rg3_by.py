# Item 8, on a FRESH client, no Sojourn picked by this player: a Kiriko sees a bot Sojourn's first shot, ally then enemy, then her own first uses. Harness keys, bots.
import json as _j
WORLD = 'rb%d' % (int(time.time()) % 100000)
def parse(tr):
    marks = []
    for p in (tr or '').split(' | '):
        if not p: continue
        a, b = p.split(' ', 1) if ' ' in p else (p, '')
        marks.append((float(a), b))
    return marks
def gaps(tr, least=45):
    out = []; last = None
    for t, x in parse(tr):
        if x in ('f', 'frame'):
            if last is not None and t - last >= least: out.append((round(last), round(t - last)))
            last = t
    return out
for i in range(30):
    if st().get('screen') in ('TitleScreen', 'AccessibilityOnboardingScreen'): break
    sleep(2)
c('close'); sleep(1); log(c('newworld ' + WORLD))
for i in range(60):
    sleep(2)
    if st().get('screen') == 'HeroSelectScreen': break
sleep(1); c('close'); sleep(1)
for k in ('/difficulty normal','/gamerule minecraft:spawn_monsters false','/time set midnight','/kill @e[type=minecraft:slime]','/team add red','/team add blue','/team join blue @s','/gamemode survival @s','/spawnpoint @s 0 -60 0'):
    mc(k, 0.15)
c('window 1280 720'); mc('/fill -20 -61 -20 20 -61 40 minecraft:stone', 0.2); mc('/tp @s 0.5 -60 0.5 0 0', 0.2); c('look 0 0'); sleep(0.5); c('hero kiriko'); sleep(1); c('close'); c('heromode on'); sleep(8)
s = st(); log('hero', s.get('hero'), 'mode', s.get('heroMode'), 'screen', s.get('screen'), 'fps', s.get('fps'))
c('presstrace frames on')
def probe(name, act, wait=1.5, sample=True):
    n0 = len(lines()); c('presstrace start')
    if sample: c('presstrace sample 1200')
    act(); sleep(wait); tr = c('presstrace').get('trace', '')
    log('BY', name, 'frames over 45 ms (at, length):', gaps(tr), 'fps', st().get('fps'), 'misses', tr.count('art_miss'))
    if sample:
        sleep(0.1); ln = lines()[n0:]; j = [i for i, l in enumerate(ln) if 'SAMPLE' in l]
        if j: log('\n'.join(ln[j[0]:j[0] + 12]))
def key(k, t=0.08):
    def f():
        c('key %s down' % k); sleep(t); c('key %s up' % k)
    return f
mc('/summon minecraft:zombie 8.5 -60 14.5 {NoAI:1b,Tags:["rgz"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}', 0.3)
probe('idle', lambda: None, wait=1.0)
nm = 'rgal%d' % (int(time.time()) % 10000)
log(c('bot add %s sojourn blue 4.5 -60 10.5 0' % nm)); mc('/gamemode survival ' + nm, 0.15); c('bot look %s 0 0' % nm); sleep(4); log('art outstanding', c('presstrace art'))
c('presstrace energy 80 ' + nm); sleep(0.2)
probe('ally Sojourn, first Charged Shot seen', lambda: c('bot input %s 2 0 0' % nm)); c('bot input %s 0 0 0' % nm); sleep(1)
c('presstrace energy 80 ' + nm); sleep(0.2)
probe('ally Sojourn, second', lambda: c('bot input %s 2 0 0' % nm), sample=False); c('bot input %s 0 0 0' % nm); sleep(1)
probe('ally Sojourn, first rounds seen', lambda: (c('bot input %s 1 0 0' % nm), sleep(0.5), c('bot input %s 0 0 0' % nm))); sleep(1)
c('bot remove ' + nm); sleep(0.5)
nm = 'rgen%d' % (int(time.time()) % 10000)
log(c('bot add %s sojourn red 4.5 -60 10.5 0' % nm)); mc('/gamemode survival ' + nm, 0.15); c('bot look %s 0 0' % nm); sleep(4); log('art outstanding', c('presstrace art'))
c('presstrace energy 80 ' + nm); sleep(0.2)
probe('enemy Sojourn, first Charged Shot seen', lambda: c('bot input %s 2 0 0' % nm)); c('bot input %s 0 0 0' % nm); sleep(1)
c('bot remove ' + nm); sleep(1)
# her own first uses, as a sample of what other heroes pay
probe('Kiriko first kunai', key('secondary_fire', 0.3))
sleep(1); probe('Kiriko kunai again', key('secondary_fire', 0.3), sample=False)
sleep(1); probe('Kiriko first ofuda', key('primary_fire', 0.4))
sleep(1); probe('Kiriko first Suzu', key('ability_2'))
sleep(2); mc('/giveult @s', 0.1); probe('Kiriko first Kitsune Rush', key('ultimate'), wait=2.0)
c('presstrace frames off'); log('### done')

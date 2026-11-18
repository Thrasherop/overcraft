# Second pass: the cases the first got wrong by design (see record). Helpers from suzu_play.py, without its run.
import sys
src = open(r'C:/Users/ultra/AppData/Local/Temp/claude/14452f9945c49089/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/suzu_play.py', encoding='utf8').read()
exec(src.split("log('=== suzu_play'")[0])
X[0] = 1000
def voices():
    h = c('heard').get('heard', ''); return [p.strip().split(' (')[0] for p in h.split(' | ') if 'kiriko.voice' in p]
log('=== suzu_play2', time.strftime('%H:%M:%S'), sys.argv[1])
# 1. hinder on me, fix on: every voice of hers in the window, to see why no Suzu line
for run in range(2):
    x = spot(); kiriko(x, 89); cas = add('casE', 'cassidy', 'blue', x, 6.5, 180, 12); bot('fix 119 on')
    log('--- hinder on me, fix on, run %d' % run); voices()
    bot('press %s ability2' % cas); t0 = time.time()
    while not bot('cc me').get('hindered') and time.time() - t0 < 1.5: sleep(0.02)
    log('    hindered:', cc('me')); log('    voices after the flash:', voices())
    suzu(); sleep(0.3); log('    after:', cc('me')); sleep(1.2); log('    voices after Suzu:', voices())
    sleep(11)
# 1b. the same with her already free when she throws: hindered, 2 s quiet, then Suzu
x = spot(); kiriko(x, 89); cas = add('casE', 'cassidy', 'blue', x, 6.5, 180, 12)
log('--- hinder on me (dev "cut hinder", no damage), fix on'); voices()
log('    ', c('cut hinder 1.2')); sleep(0.1); log('    hindered:', cc('me'))
suzu(); sleep(0.3); log('    after:', cc('me')); sleep(1.2); log('    voices after Suzu:', voices())
sleep(11)
# 2. flashbang landing inside the phase: flash pressed, Suzu 0.1 s later, read every tick
for delay in (0.05, 0.15):
    x = spot(); kiriko(x)
    ally = add('ally', 'mercy', 'red', x + 1, 3.5, 0); cas = add('casE', 'cassidy', 'blue', x + 1, 9.5, 180, 12)
    log('--- flash, then Suzu %.2f s later, ally read each loop' % delay)
    bot('press %s ability2' % cas); sleep(delay); suzu(); rows = []; t0 = time.time()
    while time.time() - t0 < 1.2:
        r = bot('cc ' + ally); rows.append((r['now'], r['phased'], r['hindered'], round(r['health'], 1)))
    ph = [q[0] for q in rows if q[1]]; hi = [q[0] for q in rows if q[2]]
    log('    phased ticks', ph[:1], ph[-1:], '; hindered ticks', hi[:1], hi[-1:], '; health', rows[0][3], '->', rows[-1][3])
    log('    rows', rows[::3])
    sleep(11)
# 3. Steel Trap: the ally well away until phased, then on it
x = spot(); kiriko(x)
ally = add('ally', 'mercy', 'red', x + 6, 3.5, 0); jr = add('junE', 'junkrat', 'blue', x + 1, 7, 0, 60)
log('--- Steel Trap: ally 5 m aside until phased, then moved onto it')
bot('press %s ability2' % jr); sleep(1.6); bot('move %s %s -60 12' % (jr, x + 1)); sleep(0.2)
log('    before:', cc(ally))
c('look -40 50'); sleep(0.2)
bot('move %s %s -60 3.5' % (ally, x + 3)); suzu(); t0 = time.time()
while not bot('cc ' + ally).get('phased') and time.time() - t0 < 2: sleep(0.01)
bot('move %s %s -60 3.5' % (ally, x + 1)); log('    phased at +%.2f, moved onto the trap' % (time.time() - t0))
timeline(ally, 0.8)
log('    phase over, still on it:'); timeline(ally, 0.6)
c('screenshot suzu_trap_prevent.png')
clear(); log('=== done', time.strftime('%H:%M:%S'))

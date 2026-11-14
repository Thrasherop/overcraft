import sys
src = open(r'C:/Users/ultra/AppData/Local/Temp/claude/14452f9945c49089/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/suzu_play.py', encoding='utf8').read()
exec(src.split("log('=== suzu_play'")[0])
X[0] = 2000
log('=== suzu_probe', time.strftime('%H:%M:%S'))
for phase in (False, True):
    x = spot(); kiriko(x)
    ally = add('ally', 'mercy', 'red', x + 6, 3.5, 0); jr = add('junE', 'junkrat', 'blue', x + 1, 7, 0, 60)
    log('--- trap probe, Suzu first: %s' % phase)
    bot('press %s ability2' % jr); sleep(1.6); bot('move %s %s -60 12' % (jr, x + 1)); sleep(0.2)
    log('    trap', c('ents SteelTrap'))
    if phase:
        c('look -40 50'); sleep(0.2); bot('move %s %s -60 3.5' % (ally, x + 3)); suzu(); t0 = time.time()
        while not bot('cc ' + ally).get('phased') and time.time() - t0 < 2: sleep(0.01)
    bot('move %s %s -60 3.5' % (ally, x + 1)); log('    moved onto it', bot('pos ' + ally))
    timeline(ally, 1.6, 0.15)
    log('    trap', c('ents SteelTrap'))
    sleep(11)
clear(); log('=== done')

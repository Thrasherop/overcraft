import sys
src = open(r'C:/Users/ultra/AppData/Local/Temp/claude/14452f9945c49089/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/suzu_play.py', encoding='utf8').read()
exec(src.split("log('=== suzu_play'")[0])
X[0] = 3000
log('=== suzu_trap3', time.strftime('%H:%M:%S'), 'the trap lands at Junkrat feet (z 7.85): ally kept 4 m off it until phased')
for mode in ('control', 'phased', 'rooted then suzu'):
    x = spot(); kiriko(x, 50)
    ally = add('ally', 'mercy', 'red', x + 1, 3.5, 0); jr = add('junE', 'junkrat', 'blue', x + 1, 7, 0, 60)
    log('--- trap: %s' % mode, bot('fix 119 on'))
    bot('press %s ability2' % jr); sleep(1.6); bot('move %s %s -60 14' % (jr, x + 1)); sleep(0.2)
    log('    trap', c('ents SteelTrap')['ents'].split('|')[-2], '; ally before', cc(ally))
    heard()
    if mode == 'phased':
        suzu(); t0 = time.time()
        while not bot('cc ' + ally).get('phased') and time.time() - t0 < 2: sleep(0.01)
        log('    phased at +%.2f' % (time.time() - t0))
    bot('move %s %s -60 7.85' % (ally, x + 1.5)); log('    moved onto the trap', bot('pos ' + ally))
    if mode == 'rooted then suzu':
        sleep(0.3); log('    ', cc(ally)); c('look 0 75'); sleep(0.2); bot('move %s %s -60 7.85' % (ally, x + 1.5)); suzu(); log('    Suzu thrown')
    timeline(ally, 1.5, 0.12)
    sleep(0.3); log('    her line:', heard())
    c('screenshot suzu_trap3_%s.png' % mode.split()[0])
    sleep(11)
clear(); log('=== done')

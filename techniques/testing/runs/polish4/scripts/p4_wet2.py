XSTART = globals().get('XSTART', 130000)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_sixth.py", encoding="utf8").read()
ONLY = ('none',)
exec(src.replace("clear(); log('DONE')\n", ""))
log('### control and deep water: tp from the air with no contact at all; a 6 m deep pool, afloat. Same drop')
for where in ('air', 'deep', 'ladder', 'air', 'deep', 'ladder'):
    x = spot(r=24); mc('/fill %d -60 -3 %d -52 3 minecraft:stone' % (x - 4, x + 4), 0.2)
    if where == 'deep':
        mc('/fill %d -60 4 %d -55 40 minecraft:stone' % (x - 8, x + 8), 0.1); mc('/fill %d -66 4 %d -55 40 minecraft:water' % (x - 8, x + 8), 0.2); mc('/fill %d -67 4 %d -67 40 minecraft:stone' % (x - 8, x + 8), 0.2)
    me('brigitte', x, 2.0, 0, 0); mc('/time set midnight'); tp(x, -51, 2.0, 0, 0); c('look 0 0'); sleep(1.6); c('heard'); h0 = st()['health']; c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); t0 = time.time()
    while st()['y'] > -53 and time.time() - t0 < 2: sleep(0.02)
    c('key primary_fire up')
    if where == 'ladder':
        mc('/fill %d -60 12 %d -50 12 minecraft:stone' % (x, x), 0.05); mc('/fill %d -60 11 %d -50 11 minecraft:ladder[facing=north]' % (x, x), 0.05); mc('/tp @s %s -54 11.5' % x, 0.05); sleep(0.5)
    if where == 'deep':
        t0 = time.time()
        while time.time() - t0 < 2.5 and st()['y'] > -56: sleep(0.03)
        sleep(0.8)
    s_ = st(); mid = (s_['y'], s_.get('onGround'), s_['health'])
    xx = x + 2; mc('/fill %d -61 60 %d -61 64 minecraft:stone' % (xx - 2, xx + 2), 0.05); c('heard'); mc('/tp @s %d -48 62' % xx, 0.1); t0 = time.time()
    while time.time() - t0 < 4 and not (st().get('onGround') and time.time() - t0 > 0.4): sleep(0.04)
    sleep(0.4); s2 = st(); log('  %s: before the drop y %.2f on ground %s, health %.1f; after 12 m: y %.2f, health -> %.1f, pain %s' % (where, mid[0], mid[1], mid[2], s2['y'], s2['health'], pains()))
clear(); log('DONE')

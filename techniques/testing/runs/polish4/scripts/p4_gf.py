XSTART = 40000
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_eleventh.py", encoding="utf8").read()
E_ = ('none',); ROWS = 1
exec(src.replace("clear(); log('DONE')\n", ""))
for hero in ('kiriko', 'juno', 'kiriko'):
    x = spot(r=24); mc('/fill %d -60 -3 %d -46 3 minecraft:stone' % (x - 3, x + 3), 0.2); h0 = spx(hero, x, -45, 2.5, 0, 0); c('heromode off'); sleep(0.4); h1 = st()['health']; c('key forward down'); sleep(0.7); c('key forward up'); t0 = time.time()
    while st()['y'] > -52 and time.time() - t0 < 4: sleep(0.01)
    s0, q0 = snap(); c('heromode on'); ser = []
    for i in range(30):
        s, q = snap(); ser.append('%.1f%s:%s:%.1f' % (s['y'], 'g' if s.get('onGround') else '', 'H' if q.get('inHeroPlay') else 'p', q.get('fallDistance', -1)))
        if s.get('onGround') and s['y'] < -59.5: break
        sleep(0.01)
    sleep(0.4); s = st(); log('  %s: before G y %.1f fall %.1f; health %.1f -> %.1f | %s' % (hero, s0['y'], q0.get('fallDistance', -1), h1, s['health'], ' '.join(ser)))
clear(); log('DONE')

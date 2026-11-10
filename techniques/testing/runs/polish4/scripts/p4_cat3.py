XSTART=30000
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
log('--- Catnapper landing with the phase over it; offsets: when the bot Kiriko throws after the cat confirms')
for off in (0.0, 0.1, 0.2, 0.3, 0.05, 0.15):
    x = spot(); me('mercy', x); n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40); bot('charge %s 9999' % n); k = add('kiri', 'kiriko', 'blue', x - 3, 0.5, -90, 72); sleep(0.2)
    c('heard'); bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); t0 = time.time(); th = [False]
    def ev(q):
        if not th[0] and time.time() - t0 >= off: th[0] = True; bot('press %s ability2' % k)
    r = rows('me', 2.4, ['phased', 'asleep', 'carriedBy', 'health', 'y'], ev); inp(n, 0, 0)
    a = span(r, 1); b = span(r, 2); car = [q[0] for q in r if q[3]][:1]
    log('offset', off, 'phase', a, 'knocked', b, 'carried from', car, 'health', r[-1][4], 'y end', r[-1][5], 'heard land', heard('jc')[:4]); sleep(5)
clear(); log('DONE')

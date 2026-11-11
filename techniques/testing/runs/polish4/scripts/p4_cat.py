D = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
exec(open(D + "p4_lib.py", encoding="utf8").read())
def add(n, hero, team, x, z, yaw, pitch=0, y=-60):
    r = bot('add %s %s %s %s %s %s %s' % (n, hero, team, x, y, z, yaw)); mc('/gamemode survival ' + n, 0.15); bot('look %s %s %s' % (n, yaw, pitch)); return r
c('key secondary_fire up')
log('=== Catnapper control: the same with and without a Suzu, fix on, tree a20c352')
for k, thrown in enumerate((False, True, False, True)):
    x = 7200 + 100 * k; pick('mercy'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); mc('/fill %d -61 -15 %d -61 15 minecraft:stone' % (x - 15, x + 15), 0.2); mc('/kill @e[type=!minecraft:player]'); tp(x + 0.5, -60, 0.5, 0, 0)
    n = 'catK%d' % k; add(n, 'jetpack_cat', 'red', x + 0.5, 3.5, 180, 40); add('kiK%d' % k, 'kiriko', 'blue', x + 4, 0.5, 90, 70); sleep(0.4); bot('charge %s 9999' % n)
    rows = []; bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); t0 = time.time(); done = False; up = False
    while time.time() - t0 < 4:
        r = bot('cc me'); rows.append((r['now'], r['asleep'], r['phased']))
        if not up and time.time() - t0 > 0.25: up = True; inp(n, 0, 0)
        if thrown and r['asleep'] and not done: done = True; bot('press kiK%d ability2' % k)
    on = [q[0] for q in rows if q[1]]; ph = [q[0] for q in rows if q[2]]
    log('Suzu', thrown, 'held ticks', on[0] if on else None, on[-1] if on else None, '=', on[-1] - on[0] + 1 if on else 0, '(W 1.5 s = 30)', 'phase', ph[0] if ph else None, ph[-1] if ph else None)
    bot('remove ' + n); bot('remove kiK%d' % k)
log('DONE')

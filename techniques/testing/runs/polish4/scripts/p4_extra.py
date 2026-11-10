D = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
exec(open(D + "p4_lib.py", encoding="utf8").read())
def add(n, hero, team, x, z, yaw, pitch=0, y=-60):
    r = bot('add %s %s %s %s %s %s %s' % (n, hero, team, x, y, z, yaw)); mc('/gamemode survival ' + n, 0.15); bot('look %s %s %s' % (n, yaw, pitch)); return r
log('=== extra cases, tree a20c352, harness input and bots')
x = 7000; pick('mercy'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); mc('/fill %d -61 -15 %d -61 15 minecraft:stone' % (x - 15, x + 15), 0.2); mc('/kill @e[type=!minecraft:player]')
mc('/fill %d -62 -5 %d -61 5 minecraft:water' % (x - 5, x + 5), 0.2); tp(x + 0.5, -61.5, 0.5, 0, 0); sleep(0.5)
log('--- asleep in water, a bot Kiriko on the bank; y', st().get('y'))
add('anaW', 'ana', 'red', x + 0.5, 7, 180, 3); add('kiriW', 'kiriko', 'blue', x - 7, 0.5, -90, 30); sleep(0.5)
bot('press anaW ability1'); sleep(0.6); r = bot('cc me'); log('  slept', r['asleep'], r['health'], 'y', st().get('y')); bot('press kiriW ability2')
for i in range(8):
    sleep(0.25); r = bot('cc me'); log('   +%.2f asleep %s phased %s y %s' % (0.25 * (i + 1), r['asleep'], r['phased'], st().get('y')))
bot('remove anaW'); bot('remove kiriW')
log('--- Catnapper\'s knock-down on the real client, a bot Kiriko throwing at the landing')
x = 7100; pick('mercy'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); mc('/fill %d -61 -15 %d -61 15 minecraft:stone' % (x - 15, x + 15), 0.2); mc('/kill @e[type=!minecraft:player]'); tp(x + 0.5, -60, 0.5, 0, 0)
add('catK', 'jetpack_cat', 'red', x + 0.5, 3.5, 180, 40); add('kiriK', 'kiriko', 'blue', x + 4, 0.5, 90, 70); sleep(0.4); bot('charge catK 9999')
bot('press catK ultimate'); sleep(0.4); inp('catK', 1, 0); sleep(0.3); inp('catK', 0, 0); t0 = time.time(); rows = []; thrown = False
while time.time() - t0 < 4:
    r = bot('cc me'); rows.append((r['now'], r['asleep'], r['phased']))
    if r['asleep'] and not thrown: thrown = True; bot('press kiriK ability2'); log('   thrown at tick', r['now'])
on = [q[0] for q in rows if q[1]]; ph = [q[0] for q in rows if q[2]]
log('   held ticks %s..%s = %s (W 1.5 s = 30); phase %s..%s' % (on[0] if on else None, on[-1] if on else None, on[-1] - on[0] + 1 if on else 0, ph[0] if ph else None, ph[-1] if ph else None))
log('   (the carry also holds its target by rope: the read is the knock-down, or nothing if the dive missed)')
bot('remove catK'); bot('remove kiriK'); log('DONE')

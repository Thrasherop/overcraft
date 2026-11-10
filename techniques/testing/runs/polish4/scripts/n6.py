XSTART = 170000
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_eleventh.py", encoding="utf8").read()
E_ = ('none',); ROWS = 1
exec(src.replace("clear(); log('DONE')\n", ""))
log('### a real dart from a bot Ana at a hero on a perch high up; the perch goes as she sleeps, so she falls asleep (real dart, gate as at a fall; not in mid-flight)')
for hero, top in (('juno', -40), ('kiriko', -54), ('juno', -40), ('kiriko', -54)):
    x = spot(r=24); xi = int(x); mc('/fill %d %d 0 %d %d 0 minecraft:stone' % (xi, top - 1, xi, top - 1), 0.1); mc('/fill %d %d 8 %d %d 8 minecraft:stone' % (xi, top - 1, xi, top - 1), 0.1); h0 = place(hero, x, top, 0.5, 0, 0); n = add('anaP', 'ana', 'red', x, 8.5, 180, 0, top); sleep(0.4); bot('press %s ability1' % n); t0 = time.time(); got = False
    while time.time() - t0 < 1.5:
        if cc('me').get('asleep'): got = True; break
        sleep(0.02)
    if got: mc('/fill %d %d 0 %d %d 0 minecraft:air' % (xi, top - 1, xi, top - 1), 0.0)
    ser = watch_land(5); result('%s, dart landed %s, %d m' % (hero, got, top + 60), h0, ser[-10:])
clear(); log('DONE')

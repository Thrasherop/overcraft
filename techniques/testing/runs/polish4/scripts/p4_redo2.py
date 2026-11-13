XSTART = 80000
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
setup(); mc('/gamemode survival @s')
UU = lambda: re.search(r"\[I; ?[-\d, ]+\]", ' '.join(mc('/data get entity @s UUID', 0.4))).group(0)
log('### a burning wolf and a real Suzu, three times, with the Suzu\'s heal as its proof')
for i in range(3):
    x = spot(); me('kiriko', x, 0.5, 0, 62); mc('/summon minecraft:wolf %s -60 3.5 {Tags:["w"],Owner:%s,Health:4f,Sitting:1b,OrderedToSit:1b,Fire:200s}' % (x + 1, UU()), 0.2)
    fire = lambda: re.search(r'data: ([-\d]+)s', ' '.join(mc('/data get entity @e[tag=w,limit=1] Fire', 0.3)))
    h0 = hp('@e[tag=w,limit=1]'); f0 = fire(); press('ability_2', 0.1); log('  cd2', st().get('ability2Cooldown')); sleep(0.5); f1 = fire(); log('  Fire', f0.group(1) if f0 else None, '->', f1.group(1) if f1 else None, 'health', h0, '->', hp('@e[tag=w,limit=1]')); mc('/kill @e[tag=w]')
log('### Translocator: a dart timed to land in its 0.4 s; read by tick')
for lead in (0.0, 0.1, 0.2, 0.3, 0.4, 0.5):
    x = spot(); me('sombra', x); n = add('ana', 'ana', 'red', x, 15.7, 180, 0); sleep(0.2); bot('press %s ability1' % n); sleep(lead); press('ability_2', 0.08); r = rows('me', 1.2, ['phased', 'asleep', 'z']); log('  lead', lead, 'phase', span(r, 1), 'asleep', span(r, 2), 'z', r[-1][3]); sleep(0.5)
log('### Resurrect: a dart in its 2.25 s and one after, a tamed cat')
x = spot(); me('mercy', x, 0.5, 0, 25); mc('/summon minecraft:cat %s -60 2.5 {Tags:["c"],Owner:%s,Sitting:1b,OrderedToSit:1b}' % (x, UU()), 0.2); sleep(0.2); mc('/kill @e[tag=c]', 0.4); a1 = add('ana', 'ana', 'red', x, 5.5, 180, 12); a2 = add('ana', 'ana', 'red', x + 0.3, 5.5, 180, 12); press('ability_2', 0.15); t0 = time.time(); back = None
while time.time() - t0 < 6:
    ex = mc('/execute if entity @e[tag=c]', 0.1)[-1:]
    if ex and 'passed' in ex[0]: back = time.time() - t0; break
log('  back at +%s' % back); bot('press %s ability1' % a1); sleep(0.5); log('  a dart inside: asleep', bot('asleep').get('asleep'), 'health', hp('@e[tag=c,limit=1]')); sleep(2.2); bot('press %s ability1' % a2); sleep(0.5); log('  a dart after it: asleep', bot('asleep').get('asleep'), 'health', hp('@e[tag=c,limit=1]')); clear(); log('DONE')

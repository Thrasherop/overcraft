# Round nine. T_: rider blocks juno cat gdash ability select. Harness keys; bots server-side.
XSTART = globals().get('XSTART', 40000)
T_ = globals().get('T_', None); ROWS = globals().get('ROWS', 2)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_eleventh.py", encoding="utf8").read()
E_ = ('none',)
exec(src.replace("clear(); log('DONE')\n", ""))
def wt(k): return T_ is None or k in T_
def tap_jump(): c('key jump down'); sleep(0.06); c('key jump up')
if wt('rider'):
    log('### Lifeweaver, hurt, on a horse, boat or minecart: tap Space: cd3 and health 1 s on')
    for kind, ent in (('horse', 'horse'), ('boat', 'oak_boat'), ('minecart', 'minecart')) * ROWS:
        x = spot(r=24); xi = int(x); h0 = spx('lifeweaver', x, -60, 0.5)
        mc('/summon minecraft:%s %s -60 2.5 {NoAI:1b,Tame:1b,Tags:["rd"]}' % (ent, x), 0.2); mc('/ride @s mount @e[tag=rd,limit=1,sort=nearest]', 0.3); sleep(0.5); mc('/damage @s 6 minecraft:generic', 0.2); sleep(0.2); s0, q0 = snap(); c('heard'); tap_jump(); sleep(1.0); s1 = st(); p, h = pain_read(); log('  %s (passenger): health %.1f -> %.1f, cd3 %.2f -> %.2f, dash sound %s' % (kind, s0['health'], s1['health'], s0.get('ability3Cooldown', 0), s1.get('ability3Cooldown', 0), [n for n in h if 'dash' in n][:1])); mc('/ride @s dismount', 0.1); mc('/kill @e[tag=rd]', 0.1)
if wt('blocks'):
    log('### Lifeweaver hung 2 m up inside lava, powder snow (leather boots), a cobweb: tap Space: z, cd3. Fire resistance in lava')
    for kind in ('lava', 'snow', 'web') * ROWS:
        x = spot(r=24); xi = int(x); block = {'lava': 'lava', 'snow': 'powder_snow', 'web': 'cobweb'}[kind]; mc('/fill %d -63 -3 %d -56 3 minecraft:%s' % (xi - 3, xi + 3, block), 0.2); h0 = spx('lifeweaver', x, -58, 0.5); mc('/effect give @s minecraft:fire_resistance 30 0', 0.05)
        if kind == 'snow': mc('/item replace entity @s armor.feet with minecraft:leather_boots', 0.1)
        sleep(0.8); s0, q0 = snap(); c('heard'); tap_jump(); sleep(0.9); s1 = st(); p, h = pain_read(); log('  %s: z %.2f -> %.2f, y %.2f -> %.2f, cd3 %.2f, dash sound %s' % (kind, s0['z'], s1['z'], s0['y'], s1['y'], s1.get('ability3Cooldown', 0), [n for n in h if 'dash' in n][:1])); mc('/item replace entity @s armor.feet with minecraft:air', 0.05); mc('/fill %d -63 -3 %d -56 3 minecraft:air' % (xi - 3, xi + 3), 0.1)
def walk_off(hero, top, g_at):
    x = spot(r=28); mc('/fill %d -60 -4 %d %d 3 minecraft:stone' % (x - 4, x + 4, top - 1), 0.2); h0 = spx(hero, x, top, 2.5, 0, 0); c('heromode off'); sleep(0.4); h1 = st()['health']; c('key forward down'); sleep(0.7); c('key forward up'); t0 = time.time()
    while st()['y'] > g_at and time.time() - t0 < 4: sleep(0.01)
    c('heromode on'); return x, h1
if wt('juno'):
    log('### Juno: Minecraft mode, walk off, G on partway, Shift to glide: inHeroPlay read through; let it end and drop. Control: no glide')
    for act in ('glide', 'none') * ROWS:
        x, h1 = walk_off('juno', -30, -36); sleep(0.05)
        if act == 'glide': c('key ability_1 down'); sleep(0.05); c('key ability_1 up')
        ser = []
        for i in range(120):
            s, q = snap(); ser.append('%.1f%s%s' % (s['y'], 'H' if q.get('inHeroPlay') else 'p', 'g' if s.get('onGround') else ''))
            if s.get('onGround') and s['y'] < -59.5: break
            sleep(0.04)
        sleep(0.4); s = st(); log('  %s: health %.1f -> %.1f, end y %.1f | %s' % (act, h1, s['health'], s['y'], ' '.join(ser[:6] + ser[-6:]))); c('heromode on')
        if s['health'] <= 0: c('respawn'); sleep(1.5); c('close')
if wt('cat'):
    log('### Jetpack Cat: the same; Space held to fly once G is on, then let go and come down')
    for act in ('fly', 'none') * ROWS:
        x, h1 = walk_off('jetpack_cat', -35, -40); sleep(0.05)
        if act == 'fly': c('key jump down'); sleep(0.6); c('key jump up'); c('key crouch down')
        ser = []
        for i in range(150):
            s, q = snap(); ser.append('%.1f%s%s' % (s['y'], 'H' if q.get('inHeroPlay') else 'p', 'g' if s.get('onGround') else ''))
            if s.get('onGround') and s['y'] < -59.5: break
            sleep(0.04)
        c('key crouch up'); sleep(0.4); s = st(); log('  %s: health %.1f -> %.1f, end y %.1f | %s' % (act, h1, s['health'], s['y'], ' '.join(ser[:5] + ser[-5:]))); c('heromode on')
        if s['health'] <= 0: c('respawn'); sleep(1.5); c('close')
if wt('gdash'):
    log('### G in the dash: cd3; G back at once: does the client go on?')
    for how in ('G stays off', 'G back at once') * ROWS:
        x = spot(r=24); h0 = spx('lifeweaver', x, -60, 0.5); c('key forward down'); tap_jump(); sleep(0.2); c('key jump down'); sleep(0.02); c('key jump up'); c('heromode off'); s0 = st()
        if how == 'G back at once': c('heromode on')
        c('key forward up'); sleep(1.0); s1 = st(); log('  %s: z at G %.2f -> %.2f, y %.2f, cd3 %.2f' % (how, s0['z'], s1['z'], s1['y'], s1.get('ability3Cooldown', 0))); c('heromode on')
if wt('ability'):
    log('### an ability after G on mid-fall: inHeroPlay read through it. Lifeweaver\'s dash; landing from 20 m')
    for i in range(ROWS):
        x, h1 = walk_off('lifeweaver', -38, -44); c('key jump down'); sleep(0.02); c('key jump up'); ser = []
        for j in range(80):
            s, q = snap(); ser.append('%.1f%s%s%s' % (s['y'], 'H' if q.get('inHeroPlay') else 'p', 's' if q.get('softLanding') else '', 'g' if s.get('onGround') else ''))
            if s.get('onGround') and s['y'] < -59.5: break
            sleep(0.03)
        sleep(0.4); s = st(); log('  dash: health %.1f -> %.1f | %s' % (h1, s['health'], ' '.join(ser)[:220])); c('heromode on')
        if s['health'] <= 0: c('respawn'); sleep(1.5); c('close')
if wt('select'):
    log('### picking a hero mid-fall in Minecraft mode, from a ledge at the spawnpoint: Kiriko 15 m (hero rule 7, plain 12)')
    for i in range(ROWS):
        x = spot(r=24); mc('/fill %d -60 -4 %d -46 3 minecraft:stone' % (x - 4, x + 4), 0.2); h0 = spx('brigitte', x, -45, 2.5, 0, 0); mc('/spawnpoint @s %d -45 2' % x, 0.1); c('heromode off'); sleep(0.4); h1 = st()['health']; c('key forward down'); sleep(0.7); c('key forward up'); t0 = time.time()
        while st()['y'] > -45.8 and time.time() - t0 < 3: sleep(0.01)
        r_ = c('hero kiriko'); q = cc('me'); ser = bottom(5); s = st(); log('  %s: now %s, health %.1f -> %.1f of max?, inHeroPlay at the pick %s' % (r_, s.get('hero'), h1, s['health'], q.get('inHeroPlay'))); c('close'); mc('/spawnpoint @s 0 -60 0', 0.05); c('heromode on')
if wt('steve'):
    log('### owner rule: an ability landing running, G off mid-fall, land as Steve: bash and dash off a 12 m ledge')
    for hero in ('brigitte', 'lifeweaver') * ROWS:
        x = spot(r=30); ledge(x, -48); h0 = place(hero, x, -48, 2.0, 0, 0); ability(hero, x); t0 = time.time()
        while st()['y'] > -50 and time.time() - t0 < 2.5: sleep(0.02)
        q0 = cc('me'); c('heromode off'); ser = bottom(); p, h = pain_read(); s1 = st(); log('  %s: soft before G %s, health %.1f -> %.1f, mode %s, pain %s' % (hero, q0.get('softLanding'), h0, s1['health'], s1.get('heroMode'), len(p))); c('heromode on')
clear(); log('DONE')

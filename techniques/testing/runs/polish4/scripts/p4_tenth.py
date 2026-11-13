# Round seven. O: asleep glide firsthit vines climb whiplash flux ga others rallypush. Harness keys; `cut sleep/stun` is the dev hold, Sleep.put / stun itself, not a thrown dart. Bots server-side.
XSTART = globals().get('XSTART', 40000)
O = globals().get('O', None); ROWS = globals().get('ROWS', 2)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_eighth.py", encoding="utf8").read()
ONLY8 = ('none',)
exec(src.replace("clear(); log('DONE')\n", ""))
def wo(k): return O is None or k in O
log('TREE7', open('E:/claude/mods/overcraft_polish4/run/p4_launch_tree.txt').read().strip(), time.strftime('%H:%M:%S'))
def bottom(secs=7):
    return watch_land(secs)
if wo('asleep'):
    log('### held in the air (cut sleep 4 s or cut stun 4 s, the dev hold) then landing. Asleep, the gate read `controls` false. 0 pain wanted on an ability landing, and from a hero\'s safe height')
    for kind in ('sleep', 'stun') * ROWS:
        x = spot(r=24); xi = int(x); mc('/fill %d -44 7 %d -44 7 minecraft:stone' % (xi + 1, xi + 1), 0.1); h0 = place('mercy', x, -60, 0.5, 0, 0); a = add('up', 'mercy', 'blue', x + 1, 7.5, 0, 0, -43); sleep(0.4); c('look 7 -62'); sleep(0.2); c('heard'); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.55); c('key jump down'); sleep(0.05); c('key jump up'); sleep(0.2); y1 = st()['y']; c('cut %s 4' % kind); ser = bottom(); result('Mercy GA launch, %s at y %.1f' % (kind, y1), h0, ser)
    for kind in ('sleep', 'stun') * ROWS:
        x = spot(r=24); mc('/fill %d -60 -6 %d -41 3 minecraft:stone' % (x - 4, x + 4), 0.2); h0 = place('lifeweaver', x, -40, 2.0, 0, 0); ability('lifeweaver', x); sleep(0.25); y1 = st()['y']; c('cut %s 4' % kind); ser = bottom(); result('Lifeweaver dash off 20 m, %s at y %.1f' % (kind, y1), h0, ser)
    for kind in ('sleep', 'none') * ROWS:
        x = spot(r=24); h0 = place('juno', x, -60, 0.5, 0, 0); tp(x, -40, 0.5, 0, 0); sleep(0.15)
        if kind == 'sleep': c('cut sleep 4')
        ser = bottom(); result('Juno, 20 m, %s' % kind, h0, ser)
    for kind in ('sleep', 'none') * ROWS:
        x = spot(r=24); h0 = place('kiriko', x, -60, 0.5, 0, 0); tp(x, -54, 0.5, 0, 0); sleep(0.05)
        if kind == 'sleep': c('cut sleep 3')
        ser = bottom(); result('Kiriko, 6 m (her safe height is over that), %s' % kind, h0, ser)
    for kind in ('sleep', 'none'):
        x = spot(r=24); h0 = place('brigitte', x, -60, 0.5, 0, 0); tp(x, -52, 0.5, 0, 0); sleep(0.05)
        if kind == 'sleep': c('cut sleep 3')
        ser = bottom(); result('control: Brigitte plain 8 m, %s (hurts from 3 m either way)' % kind, h0, ser)
if wo('glide'):
    log('### a bash along a flat roof that ends short of its edge, 11 m up, her exit glide running off it. Start back from the edge by d m')
    for d in globals().get('DS', (6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 7.0)) * ROWS:
        x = spot(r=24); mc('/fill %d -60 -14 %d -50 3 minecraft:stone' % (x - 4, x + 4), 0.2); z0 = 4.0 - d; h0 = place('brigitte', x, -49, z0, 0, 0); c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); sleep(0.1); c('key primary_fire up'); ser = bottom(4); s = st(); result('d %.1f: end z %.2f' % (d, s['z']), h0, ser[:24])
if wo('firsthit'):
    log('### stepping backwards off a 10 m ledge with the shield up; bash at once: a zombie on the ledge at 2 m, or none')
    for tgt in (True, False) * ROWS:
        x = spot(r=24); mc('/fill %d -60 -6 %d -51 3 minecraft:stone' % (x - 4, x + 4), 0.2); h0 = place('brigitte', x, -50, 3.2, 180, 0)
        if tgt: mc('/summon minecraft:zombie %s -50 3.3 {NoAI:1b,Tags:["bz"],Health:200f,attributes:[{id:"minecraft:max_health",base:200}]}' % x, 0.1)
        c('key primary_fire down'); sleep(0.5); c('key back down'); t0 = time.time()
        while st()['y'] > -50.3 and time.time() - t0 < 1.5: sleep(0.005)
        c('key back up'); c('key secondary_fire down'); sleep(0.08); c('key secondary_fire up'); c('key primary_fire up'); ser = bottom(); z = hp('@e[tag=bz,sort=nearest,limit=1]') if tgt else None; result('target %s (zombie %s)' % (tgt, z), h0, ser[-14:]); mc('/kill @e[tag=bz]', 0.05)
if wo('vines'):
    log('### a bash down a wall past vines 1, 3 tall, 8 m to the ground below them; and sliding down a ladder, climbing up one, holding on one, then stepping off')
    for tall in (1, 3) * ROWS:
        x = spot(r=20); xi = int(x); mc('/fill %d -60 -6 %d -49 3 minecraft:stone' % (xi - 6, xi + 6), 0.15); mc('/fill %d -60 8 %d -42 8 minecraft:stone' % (xi - 6, xi + 6), 0.1); mc('/fill %d -52 7 %d %d 7 minecraft:vine[south=true]' % (xi - 6, xi + 6, -52 + tall - 1), 0.1)
        h0 = place('brigitte', x, -48, 2.0, 0, 0); ability('brigitte', x); ser = bottom(9); result('vines %d tall, y -52 up' % tall, h0, ser[-22:])
    x = spot(r=20); xi = int(x); mc('/fill %d -60 8 %d -42 8 minecraft:stone' % (xi - 6, xi + 6), 0.1); mc('/fill %d -60 -6 %d -49 3 minecraft:stone' % (xi - 6, xi + 6), 0.15); mc('/fill %d -60 7 %d -44 7 minecraft:ladder[facing=north]' % (xi - 6, xi + 6), 0.1)
    for how in ('slide', 'climb', 'hold') * ROWS:
        x = spot(r=20); xi = int(x); mc('/fill %d -60 8 %d -42 8 minecraft:stone' % (xi - 6, xi + 6), 0.1); mc('/fill %d -60 -6 %d -49 3 minecraft:stone' % (xi - 6, xi + 6), 0.15); mc('/fill %d -60 7 %d -44 7 minecraft:ladder[facing=north]' % (xi - 6, xi + 6), 0.1)
        h0 = place('brigitte', x, -48, 2.0, 0, 0); ability('brigitte', x); t0 = time.time()
        while not cc('me').get('climbable') and time.time() - t0 < 1.5: sleep(0.01)
        if how == 'climb': c('key forward down'); sleep(0.8); c('key forward up')
        if how == 'hold': c('key sneak down'); sleep(0.8)
        s0, q0 = snap(); c('look 180 0'); c('key forward down'); sleep(0.35); c('key forward up'); c('key sneak up'); ser = bottom(); result('ladder, %s: at y %.1f soft %s' % (how, s0['y'], q0.get('softLanding')), h0, ser[-14:])
if wo('whiplash'):
    log('### Whiplash, Brigitte gone after the hit. A real Brigitte with the perk at a husk, wall 5 m behind it: control, G 0.27 s after the press. A bot Brigitte with it: killed or removed at 0.27 s')
    def perked():
        pick('brigitte'); mc('/gamemode survival @s'); bot('perk me BRIGITTE_WHIPLASH')
    for how in ('control', 'G') * ROWS:
        x = spot(); xi = int(x); perked(); mc('/time set midnight'); mc('/fill %d -60 6 %d -56 6 minecraft:stone' % (xi - 3, xi + 3), 0.1); mc('/summon minecraft:husk %s -60 0.5 {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); tp(x, -60, -4.5, 0, 0); c('look 0 0'); sleep(0.3); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.22)
        if how == 'G': c('heromode off')
        sleep(1.2); log('  real Brigitte, %s: husk %s (194.4 whip, 189.6 slam)' % (how, hp('@e[tag=hk,sort=nearest,limit=1]'))); mc('/kill @e[tag=hk]', 0.05); c('heromode on')
    for how in ('control', 'kill', 'remove') * ROWS:
        x = spot(); xi = int(x); clear(); pick('mercy'); mc('/gamemode creative @s'); tp(x + 8, -60, 0.5, 0, 0); mc('/fill %d -60 6 %d -56 6 minecraft:stone' % (xi - 3, xi + 3), 0.1); mc('/summon minecraft:husk %s -60 0.5 {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); b = add('brig', 'brigitte', 'red', x, -4.5, 0, 0); bot('perk %s BRIGITTE_WHIPLASH' % b); sleep(0.2); bot('press %s ability1' % b); sleep(0.28)
        if how == 'kill': mc('/kill %s' % b, 0.0)
        if how == 'remove': bot('remove ' + b)
        sleep(1.2); log('  bot Brigitte, %s: husk %s' % (how, hp('@e[tag=hk,sort=nearest,limit=1]'))); mc('/kill @e[tag=hk]', 0.05); mc('/gamemode survival @s', 0.05)
if wo('flux'):
    log('### Flux: cast and clicked on the ground; once he hovers, /tp 12 m up over open ground (a stand-in for a high place: he stays in the Flux); G, or left to end')
    for mode in ('G', 'none') * ROWS:
        x = spot(r=30); h0 = place('sigma', x, -60, 0.5, 0, 0); mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key secondary_fire down'); sleep(0.05); c('key secondary_fire up'); sleep(0.05); c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(0.5); s0 = st(); tp(x, -46, 0.5, 0, 0); sleep(0.2); s1 = st()
        if mode == 'G': c('heromode off')
        ser = bottom(14); s2 = st(); result('Flux, %s: hovered y %.1f, up at y %.1f, hero mode %s' % (mode, s0['y'], s1['y'], s2.get('heroMode')), h0, ser[-14:]); c('heromode on')
if wo('ga'):
    log('### GA from water, a ladder, vines, deep water, to a bot ally: does she fly? (#127) and a landing after it')
    for where in ('plain', 'water', 'ladder', 'vine', 'deep') * ROWS:
        x = spot(r=24); xi = int(x)
        if where == 'water': mc('/setblock %d -60 0 minecraft:water' % xi, 0.05)
        if where == 'deep': mc('/fill %d -63 -2 %d -60 2 minecraft:water' % (xi - 2, xi + 2), 0.1); mc('/fill %d -64 -2 %d -64 2 minecraft:stone' % (xi - 2, xi + 2), 0.1)
        if where == 'ladder': mc('/setblock %d -60 -1 minecraft:stone' % xi, 0.02); mc('/setblock %d -60 0 minecraft:ladder[facing=south]' % xi, 0.05)
        if where == 'vine': mc('/setblock %d -60 -1 minecraft:stone' % xi, 0.02); mc('/setblock %d -60 0 minecraft:vine[north=true]' % xi, 0.05)
        mc('/fill %d -44 7 %d -44 7 minecraft:stone' % (xi + 1, xi + 1), 0.1); h0 = place('mercy', x, -60 if where != 'deep' else -61, 0.5, 0, 0); a = add('up', 'mercy', 'blue', x + 1, 7.5, 0, 0, -43); sleep(0.4); c('look 7 -62'); sleep(0.2); c('heard'); y0 = st()['y']; c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); sleep(0.5); s1 = st(); q1 = cc('me'); c('key jump down'); sleep(0.05); c('key jump up'); ser = bottom(); result('from %s: y %.1f -> %.1f in 0.5 s, soft %s' % (where, y0, s1['y'], q1.get('softLanding')), h0, ser[-10:])
    log('### GA across deep water at her level: 30 reads, soft all along, and at its end stepping off')
    for i in range(ROWS):
        x = spot(r=30); xi = int(x); mc('/fill %d -60 2 %d -58 20 minecraft:water' % (xi - 3, xi + 3), 0.15); mc('/fill %d -61 2 %d -61 20 minecraft:stone' % (xi - 3, xi + 3), 0.1); mc('/fill %d -60 -2 %d -58 1 minecraft:air' % (xi - 3, xi + 3), 0.05); h0 = place('mercy', x, -60, 3.5, 0, 0); a = add('far', 'mercy', 'blue', x, 19.5, 180, 0); sleep(0.4); c('look 0 -1'); sleep(0.2); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); ser = []
        for j in range(16):
            s, q = snap(); ser.append('%.1f%s%s' % (s['z'], 's' if q.get('softLanding') else '', 'w' if q.get('inWater') else '')); sleep(0.05)
        log('  across: ' + ' '.join(ser))
if wo('others'):
    log('### other client-moved abilities from water or a ladder: distance moved in 1 s')
    def spot_in(where):
        x = spot(r=24); xi = int(x)
        if where == 'deep': mc('/fill %d -63 -3 %d -60 16 minecraft:water' % (xi - 3, xi + 3), 0.15); mc('/fill %d -64 -3 %d -64 16 minecraft:stone' % (xi - 3, xi + 3), 0.1)
        if where == 'shallow': mc('/setblock %d -60 0 minecraft:water' % xi, 0.05)
        if where == 'ladder': mc('/setblock %d -60 -1 minecraft:stone' % xi, 0.02); mc('/setblock %d -60 0 minecraft:ladder[facing=south]' % xi, 0.05)
        return x
    for hero, act in globals().get('OTH', (('brigitte', 'bash'), ('sojourn', 'slide'), ('kiriko', 'none'), ('lifeweaver', 'dash'))):
        for where in ('plain', 'shallow', 'deep', 'ladder'):
            if hero == 'kiriko': continue
            x = spot_in(where); h0 = place(hero, x, -60, 0.5, 0, 0); s0 = st()
            if act == 'bash': c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); sleep(0.5); c('key primary_fire up')
            if act == 'slide': c('key ability_1 down'); sleep(0.1); c('key ability_1 up'); sleep(0.9)
            if act == 'dash': c('key forward down'); c('key jump down'); sleep(0.2); c('key jump up'); sleep(0.2); c('key jump down'); sleep(0.05); c('key jump up'); sleep(0.6); c('key forward up')
            s1 = st(); log('  %s %s from %s: z %.2f -> %.2f, y %.2f -> %.2f, cd2 %s cd1 %s cd3 %s' % (hero, act, where, s0['z'], s1['z'], s0['y'], s1['y'], s1.get('ability2Cooldown'), s1.get('ability1Cooldown'), s1.get('ability3Cooldown')))
    for where in ('plain', 'shallow', 'ladder', 'deep'):
        x = spot_in(where); h0 = place('mercy', x, -60, 0.5, 90, 0); g = add('lw', 'lifeweaver', 'blue', x + 9, 0.5, 90, 0); sleep(0.3); s0 = st(); bot('press %s ability2' % g); sleep(2.0); s1 = st(); log('  Life Grip on the real client in %s: x %.2f -> %.2f, y %.2f -> %.2f' % (where, s0['x'] - x, s1['x'] - x, s0['y'], s1['y']))
if wo('rallypush'):
    log('### Rally\'s bash and wave on a mob with AI, speed 0: does it move? Against a real client')
    for i in range(ROWS):
        x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0); mc('/summon minecraft:husk %s -60 3 {Tags:["hk"],Health:200f,attributes:[{id:"minecraft:max_health",base:200},{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.2); mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); sleep(1.0); c('key primary_fire up'); l = mc('/data get entity @e[tag=hk,sort=nearest,limit=1] Pos', 0.2)[-1:]; log('  husk bashed in Rally: %s, health %s' % (l[0][-40:] if l else None, hp('@e[tag=hk,sort=nearest,limit=1]'))); mc('/kill @e[tag=hk]', 0.05)
clear(); log('DONE')

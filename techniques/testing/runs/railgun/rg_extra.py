TAG = os.environ.get('RG_TAG', 'new')
# What the table doesn't: damage, the reload's ending (ammo, sound, clip), hard stops, a flick, the look. Harness keys, not a person.
SRC = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg_table.py", encoding="utf8").read()
ARGS_SAVED = ARGS
ARGS = ['none']
exec(SRC)
NAMES = ARGS_SAVED if ARGS_SAVED else None
log('### extra')
def press_trace(hold=0.06, wait=0.9):
    c('presstrace start'); c('key primary_fire down'); sleep(hold); c('key primary_fire up'); sleep(wait)
    return c('presstrace').get('trace', '')
Z = '@e[tag=rgz,sort=nearest,limit=1]'
if want('damage'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.3)
    for en in (50, 100):
        h0 = hp(Z); c('presstrace energy %d' % en); sleep(0.1); tr = press_trace(); h1 = hp(Z)
        log('damage energy', en, 'zombie', h0, '->', h1, 'took', round(h0 - h1, 3), '| expected HP', 20 + en, '=', round((20 + en) / 12.5, 3), 'less a 2-armor zombie |', tr[:200])
if want('reloadend'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.3)
    c('presstrace ammo 20'); c('presstrace energy 50'); sleep(0.1); c('heard'); tap('reload'); sleep(0.3)
    s = st(); log('in the reload: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'), 'playing', c('bot playing railgun.reload'), 'anim', s.get('anim'))
    tr = press_trace(wait=0.1); s = st()
    log('right after: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'), 'meter', s.get('meter'), 'playing', c('bot playing railgun.reload'), '|', tr[:220])
    sleep(0.3); s = st(); log('+0.4 s: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'))
    sleep(0.8); s = st(); log('+1.2 s: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'), '(a cut reload stays cut: R again or empty)')
    tap('reload'); sleep(0.3); s = st(); log('R again: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload')); sleep(1.1); s = st(); log('then: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'))
    # past the 0.96 s: the magazine is full by then
    guard(); c('presstrace ammo 20'); c('presstrace energy 50'); sleep(0.1); tap('reload'); sleep(0.86); s = st(); log('late cut, before: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'))
    tr = press_trace(wait=0.2); s = st(); log('late cut, after: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'), '|', tr[:120])
    # empty: reload starts again by itself after the recovery
    sleep(1); c('presstrace energy 50'); c('presstrace ammo 0'); sleep(0.25); s = st(); log('empty: ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'))
    t0 = time.time(); tr = press_trace(wait=0.05); rows_ = []
    for i in range(14):
        s = st(); rows_.append((round(time.time() - t0, 2), s.get('primaryAmmo'), round(s.get('primaryReload'), 2))); sleep(0.12)
    log('empty after the shot (t, ammo, rel):', rows_, '|', tr[:120])
if want('hardstop'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
    nm = 'rgana%d' % (int(time.time()) % 10000)
    log(c('bot add %s ana red 0.5 -60 5.5 180' % nm)); mc('/gamemode survival ' + nm, 0.15); c('bot look %s 180 0' % nm); sleep(0.4)
    c('presstrace energy 50'); sleep(0.1); log(c('bot press %s ability1' % nm))
    for i in range(30):
        cc = c('bot cc me')
        if cc.get('asleep'): break
        sleep(0.05)
    log('asleep', cc.get('asleep'), 'after', i, 'polls')
    if cc.get('asleep'):
        c('presstrace energy 50'); tr = press_trace(wait=0.3); log('press asleep:', tr[:220] or '(nothing)', '| meter', st().get('meter'), 'still asleep', c('bot cc me').get('asleep'))
    c('bot remove ' + nm); sleep(8); guard()
    nm = 'rgsom%d' % (int(time.time()) % 10000)
    log(c('bot add %s sombra red 0.5 -60 5.5 180' % nm)); mc('/gamemode survival ' + nm, 0.15); c('bot look %s 180 0' % nm); sleep(0.3); c('bot charge %s 9999' % nm)
    log(c('bot press %s ultimate' % nm))
    for i in range(30):
        cc = c('bot cc me')
        if cc.get('silenced'): break
        sleep(0.05)
    log('hacked', cc.get('silenced'), 'after', i, 'polls')
    c('presstrace energy 50'); tr = press_trace(wait=0.3); log('press hacked (the data: the weapon works):', tr[:200] or '(nothing)')
    c('bot remove ' + nm); sleep(1); guard()
    # dead
    c('presstrace energy 50'); mc('/kill @s', 0.3); tr = press_trace(wait=0.3); log('press dead:', tr[:200] or '(nothing)'); guard()
    # the wheel and the perk menu take the button
    home(); c('presstrace energy 50'); c('key comms down'); sleep(0.2); tr = press_trace(wait=0.2); c('key comms up'); log('press with the comms wheel up (its emote page):', tr[:160] or '(nothing)'); sleep(0.3); c('close')
    # Minecraft mode
    c('heromode off'); sleep(0.3); tr = press_trace(wait=0.2); log('press in Minecraft mode:', tr[:160] or '(nothing)'); c('heromode on'); sleep(0.3)
if want('flick'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
    mc('/summon minecraft:zombie 12.5 -60 0.5 {NoAI:1b,Tags:["rgx"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}', 0.3)
    X = '@e[tag=rgx,sort=nearest,limit=1]'
    hits = 0
    for i in range(6):
        home(); sleep(0.3); c('presstrace energy 50'); sleep(0.1); h0 = hp(X)
        # (yaw -90 faces +x) the look, the next frame the press: a flick
        c('look -90 0'); c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(0.5); h1 = hp(X)
        took = round(h0 - h1, 2); hits += 1 if took > 0 else 0; log('flick', i, 'took', took); sleep(0.4)
    log('flick hits', hits, 'of 6')
    mc('/kill @e[tag=rgx]', 0.2)
if want('look'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2); c('perspective first')
    c('presstrace ammo 20'); c('presstrace energy 100'); sleep(0.1); tap('reload'); sleep(0.35); c('screenshot rg_%s_reload' % TAG)
    c('key primary_fire down'); c('screenshot rg_%s_f1' % TAG); c('screenshot rg_%s_f2' % TAG); c('screenshot rg_%s_f3' % TAG); c('key primary_fire up'); sleep(0.2); c('screenshot rg_%s_02' % TAG); sleep(0.5); c('screenshot rg_%s_07' % TAG)
    s = st(); log('after: anim', s.get('anim'), 'ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'), 'meter', s.get('meter'))
    c('perspective back'); sleep(0.2); c('presstrace ammo 20'); c('presstrace energy 100'); sleep(0.1); tap('reload'); sleep(0.35)
    c('key primary_fire down'); c('screenshot rg_%s_3p1' % TAG); c('key primary_fire up'); sleep(0.15); c('screenshot rg_%s_3p2' % TAG); c('perspective first'); sleep(1)
    # the left button on through it: rounds stop, come back at the recovery's end
    guard(); home(); c('presstrace energy 50'); c('presstrace start'); c('key secondary_fire down'); sleep(0.3); c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(1.0); c('key secondary_fire up'); sleep(0.2)
    log('left held through a shot:', c('presstrace').get('trace', ''))
if want('more'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
    # an emote really playing
    c('presstrace energy 50'); sleep(0.1); log('emote', c('emote 9295')); sleep(0.7); s = st(); log('emoting state', {k: v for k, v in s.items() if 'emote' in k.lower() or k in ('anim','camera')}, c('bot playing emote'))
    tr = press_trace(wait=0.3); s = st(); log('after the press', {k: v for k, v in s.items() if 'emote' in k.lower() or k in ('anim','camera')}, c('bot playing emote'), '|', tr[:200]); sleep(0.6)
    # the buffer: a press in the recovery's last 0.15 s (one frame a harness call: the sleeps are short for it), and before it
    for after in (0.30, 0.38, 0.45):
        guard(); home(); c('presstrace energy 50'); sleep(0.05); tap('primary_fire', 0.03); c('presstrace energy 50'); t0 = time.time(); sleep(after); el = time.time() - t0
        c('presstrace start'); c('key primary_fire down'); pressed = time.time() - t0; sleep(0.05); c('key primary_fire up'); sleep(0.7); log('recovery, pressed about %.3f s after the shot:' % pressed, c('presstrace').get('trace', '')[:260]); sleep(0.3)
    # no energy: the click
    guard(); c('presstrace energy 0'); sleep(0.1); c('heard'); tr = press_trace(wait=0.3); log('no energy heard:', c('heard').get('heard', '')[:200], '|', tr[:120])
    # a double check on the count: 8 taps a second apart in a row: one shot each, energy gone each time
    n = 0
    for i in range(6):
        c('presstrace energy 30'); sleep(0.1); tr = press_trace(hold=0.02, wait=0.8); n += tr.count('srv_shot'); log('tap', i, tr.count('srv_shot'), tr.count(' clip'), 'meter', st().get('meter'))
    log('quick taps, shots', n, 'of 6')
log('### done')

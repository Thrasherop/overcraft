EM='triumph'
SRC = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg_table.py", encoding="utf8").read()
A = ARGS; ARGS = ['none']; exec(SRC); NAMES = A or None
def ptr(hold=0.06, wait=0.5):
    c('presstrace start'); c('key primary_fire down'); sleep(hold); c('key primary_fire up'); sleep(wait)
    return c('presstrace').get('trace', '')
if want('sound'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
    # the reload's sound, uncut, for its length
    c('presstrace ammo 20'); sleep(0.1); tap('reload'); t0 = time.time(); r = []
    for i in range(14): r.append((round(time.time() - t0, 2), c('bot playing railgun.reload').get('playing', '') != '')); sleep(0.1)
    log('uncut', r); sleep(1)
    c('presstrace ammo 20'); c('presstrace energy 50'); sleep(0.1); tap('reload'); sleep(0.3); c('presstrace start'); c('key primary_fire down'); t0 = time.time(); c('key primary_fire up'); r = []
    for i in range(14): r.append((round(time.time() - t0, 2), c('bot playing railgun.reload').get('playing', '') != '')); sleep(0.1)
    log('cut', r, c('presstrace').get('trace', '')[:200])
if want('flick'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
    mc('/kill @e[tag=rgx]', 0.2)
    mc('/summon minecraft:zombie 12.5 -60 0.5 {NoAI:1b,Tags:["rgx"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}', 0.3)
    X = '@e[tag=rgx,sort=nearest,limit=1]'
    for i in range(5):
        home(); sleep(0.4); c('presstrace energy 50'); sleep(0.15); h0 = hp(X); s = st(); y0 = s.get('yaw'); sleep(0.1)
        c('presstrace start'); c('look -90 0'); c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(0.5); tr = c('presstrace').get('trace', ''); h1 = hp(X); s = st()
        log('flick', i, 'took', round(h0 - h1, 2) if h0 and h1 else (h0, h1), 'meter', s.get('meter'), 'state yaw', y0, s.get('yaw'), s.get('x'), s.get('z'), '|', tr[:140]); sleep(0.4)
    mc('/kill @e[tag=rgx]', 0.2)
if want('wheel'):
    guard(); home(); sleep(0.2); c('presstrace energy 50'); sleep(0.1)
    log(c('key comms down')); sleep(0.5); log('screen', st().get('screen')); tr = ptr(wait=0.2); log('wheel held 0.5 s:', tr[:220] or '(nothing)'); c('key comms up'); sleep(0.5); c('close')
if want('emote'):
    guard(); home(); sleep(0.2)
    for i in range(3):
        c('presstrace energy 50'); sleep(0.1); log(c('emote ' + EM)); sleep(0.7); s = st(); log('emoting', s.get('emoteClip'), s.get('emoteCamera'), c('bot playing emote'))
        tr = ptr(wait=0.3); s = st(); log('after', s.get('emoteClip'), s.get('emoteCamera'), c('bot playing emote'), '|', tr[:180]); sleep(0.5)
    guard()
log('### done')

# The final build: the wheel, the flick at 30 fps, a real emote row, energy earned by real rounds, another hero. Harness keys, not a person.
SRC = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg_table.py", encoding="utf8").read()
A = ARGS; ARGS = ['none']; exec(SRC); NAMES = A or None
log('### final; fps', st().get('fps'))
def ptr(hold=0.06, wait=0.5):
    c('presstrace start'); c('key primary_fire down'); sleep(hold); c('key primary_fire up'); sleep(wait)
    return c('presstrace').get('trace', '')
Z = '@e[tag=rgz,sort=nearest,limit=1]'
if want('rows'):
    row('idle', None)
    def p_reload(after):
        def f():
            c('presstrace ammo 20'); sleep(0.1); tap('reload'); sleep(after)
            s = st(); return dict(ammo=s.get('primaryAmmo'), rel=s.get('primaryReload'))
        return f
    def read_ammo():
        sleep(0.1); s = st(); return dict(ammo=s.get('primaryAmmo'), rel=s.get('primaryReload'), meter=s.get('meter'))
    row('mid-reload (0.3 s in)', p_reload(0.3), post=read_ammo, wait=0.9)
if want('wheel'):
    guard(); home(); sleep(0.2)
    for hold in (0.2, 0.5):
        c('presstrace energy 50'); sleep(0.1); c('key comms down'); sleep(hold); log('wheel up %.1f s, press:' % hold, ptr(wait=0.2)[:200] or '(nothing)', 'meter', st().get('meter')); c('key comms up'); sleep(0.5); c('close'); sleep(0.2)
if want('flick'):
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
    mc('/kill @e[tag=rgx]', 0.2)
    mc('/summon minecraft:zombie 12.5 -60 0.5 {NoAI:1b,Tags:["rgx"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}', 0.3)
    X = '@e[tag=rgx,sort=nearest,limit=1]'
    for i in range(8):
        home(); sleep(0.3); c('presstrace energy 50'); sleep(0.1); h0 = hp(X); z0 = hp(Z); s0 = st()
        c('presstrace start'); c('look -90 0'); c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(0.5)
        tr = c('presstrace').get('trace', ''); h1 = hp(X); z1 = hp(Z); s = st()
        log('flick', i, 'side took', round(h0 - h1, 2), 'ahead took', round(z0 - z1, 2), 'meter', s0.get('meter'), '->', s.get('meter'), 'fps', s.get('fps'), 'yaw', s.get('yaw'), '|', tr[:110]); sleep(0.4)
    mc('/kill @e[tag=rgx]', 0.2)
if want('emote'):
    def p_em():
        c('emote triumph'); sleep(0.7); s = st(); return dict(clip=s.get('emoteClip'), cam=s.get('emoteCamera'))
    def p_em_post():
        s = st(); return dict(clip=s.get('emoteClip'), cam=s.get('emoteCamera'))
    r = row('in an emote (playing)', p_em, post=p_em_post)
    log('emote ctx', [(x['ctx'], x['post']) for x in r])
if want('earned'):
    # energy from real rounds at the zombie, a burst then the press with the left still down; again while reloading; a slide and a shot in it
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2); h0 = hp(Z); c('presstrace start')
    c('key secondary_fire down'); sleep(0.6); m0 = st().get('meter'); c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(0.9); c('key secondary_fire up'); sleep(0.2)
    tr = c('presstrace').get('trace', ''); s = st(); log('burst, shot, burst: meter before', m0, 'after', s.get('meter'), 'ammo', s.get('primaryAmmo'), 'shots', tr.count('srv_shot'), 'rounds', tr.count('srv_round'), 'zombie took', round(h0 - hp(Z), 2)); sleep(0.2)
    tap('reload'); sleep(0.2); c('key secondary_fire down'); sleep(1.3); c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(0.8); c('key secondary_fire up'); sleep(0.3); s = st(); log('after reload, burst, shot, burst: anim', s.get('anim'), 'ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'), 'meter', s.get('meter'))
    c('key secondary_fire down'); sleep(0.4); c('key secondary_fire up'); tap('ability_1', 0.05); sleep(0.2); tr = ptr(wait=0.8); s = st(); log('shot in a slide:', tr.count('srv_shot'), 'meter', s.get('meter'), 'anim', s.get('anim'), 'ammo', s.get('primaryAmmo')); sleep(0.5)
    c('screenshot rg_final_after'); s = st(); log('end state', {k: s.get(k) for k in ('anim', 'primaryAmmo', 'primaryReload', 'meter', 'health', 'screen')})
if want('kiriko'):
    guard(); mc('/spawnpoint @s 0 -60 0'); home(); sleep(0.3); c('hero kiriko'); sleep(0.8); c('close'); sleep(0.3); log('hero', st().get('hero'))
    tr = ptr(hold=0.3, wait=0.2); s = st(); log('Kiriko right button:', tr[:120], 'secondary ammo', s.get('secondaryAmmo'), 'anim', s.get('anim'))
    c('hero sojourn'); sleep(0.8); c('close'); sleep(0.3); log('hero', st().get('hero'))
log('### done')

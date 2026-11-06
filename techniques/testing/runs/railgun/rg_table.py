# The table of #126: press to shot, client and server, in each case. Keys: DevServer's KeyMapping.set + click (as the mouse handler does), not a person.
WORLD = 'rg%d' % (int(time.time()) % 100000)
NAMES = ARGS if ARGS else None
def want(k): return NAMES is None or k in NAMES
def setup_world():
    c('close'); sleep(1)
    s = st()
    if not s.get('hero') or 'error' in s:
        log(c('newworld ' + WORLD))
        for i in range(50):
            sleep(2)
            if st().get('screen') == 'HeroSelectScreen': break
        sleep(1); c('close'); sleep(1)
    for k in ('/difficulty normal','/gamerule minecraft:spawn_monsters false','/gamerule minecraft:natural_health_regeneration false','/time set midnight','/weather clear','/kill @e[type=minecraft:slime]','/team add red','/team add blue','/team join blue @s','/gamemode survival @s'):
        mc(k, 0.2)
    c('window 1280 720')
def pick():
    mc('/spawnpoint @s 0 -60 0'); mc('/tp @s 0 -60 0'); sleep(0.5)
    c('hero sojourn'); sleep(0.8); c('close'); c('heromode on'); sleep(0.5)
    for _i in range(10):
        if not st().get('screen'): break
        c('close'); sleep(0.4)
    s = st(); log('hero', s.get('hero'), 'mode', s.get('heroMode'), 'screen', s.get('screen'), 'hp', s.get('health'), 'fps', s.get('fps'))
HOME = (0.5, -60, 0.5)
def home():
    mc('/tp @s %s %s %s 0 0' % HOME, 0.12); c('look 0 0')
def rig():
    mc('/fill -20 -61 -20 20 -61 40 minecraft:stone', 0.2); mc('/fill -20 -60 -20 20 -50 40 minecraft:air', 0.2)
    mc('/kill @e[type=minecraft:zombie]', 0.2)
    mc('/summon minecraft:zombie 0.5 -60 12.5 {NoAI:1b,Tags:["rgz"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}', 0.3)
def tap(k, t=0.06):
    c('key %s down' % k); sleep(t); c('key %s up' % k)
def parse(tr):
    marks = []
    for p in (tr or '').split(' | '):
        if not p: continue
        a, b = p.split(' ', 1); marks.append((float(a), b))
    return marks
def first(marks, pred, t0):
    for t, m in marks:
        if pred(m): return round(t - t0, 1)
    return None
ROWS = []
def shot(pre, hold=0.06, wait=0.9, energy=50, post=None, set_energy=True):
    """pre() puts her in the case, then one press of the right button. Returns the numbers."""
    pre_ctx = pre() if pre else None
    if set_energy is True: c('presstrace energy %s' % energy)
    c('presstrace start')
    c('key primary_fire down'); sleep(hold); c('key primary_fire up'); sleep(wait)
    tr = c('presstrace').get('trace', '')
    m = parse(tr)
    t0 = first(m, lambda x: x == 'press', 0) or 0
    r = dict(srv=first(m, lambda x: x == 'srv_shot', t0), snd=first(m, lambda x: x.startswith('snd:charged.fire'), t0),
             fx=first(m, lambda x: x == 'fx', t0), clip=first(m, lambda x: x == 'clip', t0), frame=first(m, lambda x: x == 'frame', t0),
             sent=first(m, lambda x: x == 'sent', t0), n=sum(1 for t, x in m if x == 'srv_shot'), rounds=sum(1 for t, x in m if x == 'srv_round'),
             raw=tr, ctx=pre_ctx)
    if post: r['post'] = post()
    return r
def guard():
    s = st()
    if s.get('health', 0) <= 6 or s.get('screen'):
        log('GUARD', s.get('health'), s.get('screen'))
        if s.get('health', 0) <= 0: c('respawn'); sleep(1.5)
        pick(); mc('/effect give @s minecraft:instant_health 1 5 true', 0.2)
    if s.get('health', 0) < 12: mc('/effect give @s minecraft:instant_health 1 5 true', 0.2)
def med(v):
    v = [x for x in v if x is not None]
    return ('%6.1f/%6.1f' % (statistics.median(v), max(v))) if v else '      -      '
def row(name, pre, n=5, between=0.2, **kw):
    out = []
    for i in range(n):
        guard(); mc('/overcraft reset', 0.08); home(); sleep(between)
        r = shot(pre, **kw); out.append(r)
        log('RAW', name, i, json.dumps(r))
        c('key secondary_fire up')
        sleep(0.75)
    fired = sum(1 for r in out if r['n'] > 0)
    fps = st().get('fps')
    line = '%-28s %d/%d shots=%s | srv %s | sound %s | look %s | clip %s | frame %s | fps %s' % (name, fired, n, sum(r['n'] for r in out), med([r['srv'] for r in out]), med([r['snd'] for r in out]), med([r['fx'] for r in out]), med([r['clip'] for r in out]), med([r['frame'] for r in out]), fps)
    log('ROW', line); ROWS.append(line)
    return out
log('=== #126 table. TREE', open(r"E:\claude\mods\overcraft_railgun\run\rg_tree.txt").read().strip(), 'at', time.strftime('%H:%M:%S'))
setup_world(); pick(); rig(); home(); sleep(0.5)
if want('idle'): row('idle', None)
def p_reload(after):
    def f():
        c('presstrace ammo 20'); sleep(0.1); tap('reload'); sleep(after)
        s = st(); return dict(ammo=s.get('primaryAmmo'), rel=s.get('primaryReload'))
    return f
def read_ammo():
    sleep(0.1); s = st(); return dict(ammo=s.get('primaryAmmo'), rel=s.get('primaryReload'), meter=s.get('meter'))
if want('reload'):
    row('mid-reload (0.3 s in)', p_reload(0.3), post=read_ammo, wait=0.9)
    row('reload past 0.96 s', p_reload(1.02), post=read_ammo)
    row('reload ending (1.14 s)', p_reload(1.14), post=read_ammo)
def p_hold():
    c('key secondary_fire down'); sleep(0.4)
if want('hold'): row('holding left (firing)', p_hold, post=read_ammo)
def p_between():
    c('key secondary_fire down'); sleep(0.06); c('key secondary_fire up'); sleep(0.08)
if want('between'): row('between left shots', p_between)
def p_rec(t):
    def f():
        c('presstrace energy 50'); sleep(0.05); tap('primary_fire'); sleep(t)
    return f
if want('rec'):
    row('recovery +0.25 s', p_rec(0.25), wait=0.9)
    row('recovery +0.55 s (buffer)', p_rec(0.55), wait=0.6)
    row('recovery +0.72 s (over)', p_rec(0.72), wait=0.6)
def p_g():
    c('heromode off'); sleep(0.3); c('heromode on'); sleep(0.06)
if want('g'): row('just after G back', p_g)
def p_jump():
    tap('jump', 0.08); sleep(0.08)
if want('jump'): row('after a jump, rising', p_jump)
def p_land():
    mc('/tp @s %s %s %s 0 0' % (HOME[0], HOME[1] + 1.6, HOME[2]), 0.02)
    for _ in range(40):
        s = st()
        if s.get('y', 0) <= HOME[1] + 0.05: break
        sleep(0.01)
if want('land'): row('landing', p_land)
def p_slide():
    tap('ability_1', 0.06); sleep(0.25)
if want('slide'): row('sliding', p_slide)
def p_air():
    mc('/tp @s %s %s %s 0 0' % (HOME[0], HOME[1] + 8, HOME[2]), 0.05)
if want('air'): row('in the air', p_air, wait=0.4)
def p_dis():
    c('key ability_2 down'); sleep(0.03); c('key ability_2 up')
if want('dis'): row('in Disruptor cast', p_dis, n=4)
def p_melee():
    tap('quick_melee', 0.05); sleep(0.08)
if want('melee'): row('melee recovery', p_melee)
def p_crouch():
    c('key crouch down'); sleep(0.2)
def p_crouch_post():
    c('key crouch up'); return read_ammo()
if want('crouch'): row('crouched', p_crouch, post=p_crouch_post)
if want('energy'):
    row('low energy (3)', None, energy=3, post=read_ammo)
    row('full energy (100)', None, energy=100, post=read_ammo)
    row('no energy (0)', None, energy=0, post=read_ammo)
def p_empty():
    c('presstrace ammo 0'); sleep(0.25)
    s = st(); return dict(ammo=s.get('primaryAmmo'), rel=s.get('primaryReload'))
if want('empty'): row('empty magazine, reloading', p_empty, post=read_ammo, wait=2.2)
def p_emote():
    c('emote 9295'); sleep(0.6); s = st(); return dict(anim=s.get('anim'), emote=s.get('emote'))
def p_emote_post():
    s = st(); return dict(anim=s.get('anim'), emote=s.get('emote'))
if want('emote'): row('in an emote', p_emote, post=p_emote_post)
if want('oc'):
    def p_oc_cast():
        mc('/giveult @s', 0.1); tap('ultimate', 0.05); sleep(0.25)
    row('Overclock cast (0.3 s)', p_oc_cast, n=3, wait=1.0, set_energy=False, between=9.5)
    def p_oc():
        mc('/giveult @s', 0.1); tap('ultimate', 0.05); sleep(1.0)
    row('in Overclock', p_oc, n=3, wait=1.0, set_energy=False, between=9.5)
if want('held'):
    guard(); sleep(10); mc('/overcraft reset', 0.08); home(); sleep(0.2); c('presstrace energy 100'); sleep(0.2); log('before', st().get('meter')); c('presstrace start')
    c('key primary_fire down'); sleep(2.5); c('key primary_fire up'); sleep(0.2)
    log('RAW held 2.5 s, energy 100:', c('presstrace').get('trace', '')); log('after', st().get('meter')); sleep(0.7)
    mc('/giveult @s', 0.1); tap('ultimate', 0.05); sleep(1.0); log('in Overclock', st().get('ultActive'), st().get('meter')); c('presstrace start')
    c('key primary_fire down'); sleep(3.0); c('key primary_fire up'); sleep(0.2)
    log('RAW held 3 s in Overclock:', c('presstrace').get('trace', '')); sleep(6)
log('=== rows')
for x in ROWS: log(x)

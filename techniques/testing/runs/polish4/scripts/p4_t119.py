exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_lib.py").read())
# Each effect, fix off and on, on a bot ally (real Kiriko's Suzu) and on the real client (a bot Kiriko's Suzu).
# Bots are server-side players: no client, no aim of their own. Keys are KeyMapping.set through DevServer, not a mouse.
log('=== #119 off/on. LABEL:', globals().get('LABEL', 'see file name'))
XX = [0]
def spot():
    XX[0] += 40
    x = XX[0] + 0.5
    mc('/fill %d -61 -30 %d -61 40 minecraft:stone' % (XX[0] - 15, XX[0] + 15), 0.2)
    return x
def add(n, hero, team, x, z, yaw, pitch=0):
    r = bot('add %s %s %s %s -60 %s %s' % (n, hero, team, x, z, yaw))
    if 'error' in str(r): log('ADDERR', r)
    mc('/gamemode survival ' + n, 0.15); bot('look %s %s %s' % (n, yaw, pitch)); return r
def clear():
    for n in ('anaE', 'allyB', 'kiriB', 'brig', 'som', 'catE', 'sigE', 'sojE'):
        bot('remove ' + n)
def cc(n):
    r = bot('cc ' + n)
    return {k: (round(v, 3) if isinstance(v, float) else v) for k, v in r.items() if k in ('asleep','silenced','glowing','antiHeal','healCut','weak','phased','speed','gravity','health','lifted')}
def short(n, keys):
    r = cc(n); return ' '.join('%s=%s' % (k, r.get(k)) for k in keys)
def suzu_real():
    press('ability_2', 0.1); return st().get('ability2Cooldown')
def tl(n, keys, dt=0.1, count=8, label=''):
    t0 = time.time()
    for i in range(count):
        sleep(dt); log('    +%.2f %s' % (time.time() - t0, short(n, keys)))
def world(hero, x, yaw=0, pitch=60, z=0.5):
    pick(hero); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(x, -60, z, yaw, pitch)
# Each case: who = 'allyB' with the real Kiriko throwing, or 'me' with a bot Kiriko throwing.
def begin(who, x):
    clear()
    if who == 'allyB':
        world('kiriko', x, 0, 62, 0.5)
        add('allyB', 'mercy', 'blue', x + 1, 3.5, 0)
        return (x + 1, 3.5)
    world('mercy', x, 0, 0, 0.5)
    add('kiriB', 'kiriko', 'blue', x - 3, 0.5, -90, 72)
    return (x, 0.5)
def throw(who):
    if who == 'allyB':
        return 'real cd2 %s' % suzu_real()
    return 'bot %s' % bot('press kiriB ability2')
def case(name, who, setup, apply, keys, dt=0.1, count=8, wait=0.5, after=None):
    x = spot(); at = begin(who, x)
    log('--- %s on %s' % (name, who))
    setup(at); apply(); sleep(wait)
    log('    applied: %s | held %s' % (short(who, keys), bot('held ' + who)))
    log('    throw', throw(who))
    tl(who, keys, dt, count)
    log('    end: %s | held %s' % (short(who, keys), bot('held ' + who)))
    if after: after()
    c('screenshot p4_%s_%s_%s.png' % (TAG, name.split()[0].lower(), who))
ALL = ['allyB', 'me']
def sleep_case(who):
    def setup(at):
        add('anaE', 'ana', 'red', at[0], at[1] + 7, 180)
    case('Sleep', who, setup, lambda: bot('press anaE ability1'), ['asleep', 'phased', 'health'], 0.15, 6, 0.8)
def stun_case(who):
    for thrown in (False, True):
        x = spot(); at = begin(who, x)
        log('--- Stun on %s, Suzu %s: how long it holds, from the moment the server has it' % (who, 'thrown 0.05 s after' if thrown else 'not thrown (control)'))
        add('brig', 'brigitte', 'red', at[0], at[1] - 1.8, 0); sleep(0.3)
        bot('charge brig 9999'); bot('press brig ultimate'); sleep(1.2); bot('secondary brig on'); sleep(0.6)
        if who == 'allyB': mc('/tp @s %s -60 0 -63 10' % (x - 6)); c('look -63 10'); sleep(0.25)
        else: bot('remove kiriB'); add('kiriB', 'kiriko', 'blue', x - 3, 4.5, -90, 72); sleep(0.2)
        inp('brig', 3, 0); t0 = time.time()
        while not bot('cc ' + who).get('asleep') and time.time() - t0 < 2: sleep(0.01)
        t1 = time.time(); inp('brig', 2, 0); inp('brig', 0, 0); bot('secondary brig off'); hd = bot('held ' + who)
        if thrown:
            sleep(0.05); tr = throw(who)
        else:
            tr = 'none'
        ph = False
        while bot('cc ' + who).get('asleep') and time.time() - t1 < 3:
            ph = ph or bot('cc ' + who).get('phased'); sleep(0.01)
        log('    down at +%.2f, held %s, throw %s; lasted %.2f s (D 1340: 0.75); phased in it: %s; end held %s; %s' % (t1 - t0, hd, tr, time.time() - t1, ph, bot('held ' + who), short(who, ['health', 'phased'])))
        c('screenshot p4_%s_stun_%s_%s.png' % (TAG, who, thrown))
def hack_case(who):
    x = spot()
    log('--- Hack on %s' % who)
    if who == 'allyB':
        clear(); mc('/fill %d -60 5 %d -56 5 minecraft:stone' % (x - 6, x + 6), 0.2)
        world('kiriko', x, 0, -2, -11)
        add('allyB', 'mercy', 'blue', x + 1, 3.5, 0); add('som', 'sombra', 'red', x + 10, 3.5, 90); sleep(0.2); bot('charge som 9999')
        log('   real Kiriko 17.6 m from Sombra, past EMP'); keys = ['silenced', 'glowing', 'phased']
        bot('press som ultimate'); sleep(0.9)
        log('    applied:', short(who, keys), bot('held ' + who), 'me', short('me', ['silenced']), 'throw', throw(who))
        tl(who, keys, 0.1, 8); log('    end:', short(who, keys), bot('held ' + who))
        c('screenshot p4_%s_hack_%s.png' % (TAG, who))
        return
    clear(); pick('ana'); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(x, -60, 0.5, 0, 0)
    add('kiriB', 'kiriko', 'blue', x - 3, 0.5, -90, 72); add('som', 'sombra', 'red', x, 6.5, 180); sleep(0.2); bot('charge som 9999')
    keys = ['silenced', 'glowing', 'phased']
    bot('press som ultimate'); sleep(0.9)
    b = st().get('ability1Cooldown'); press('ability_1'); sleep(0.15); log('    Shift by the real key while hacked: cd1', b, '->', st().get('ability1Cooldown'))
    log('    applied:', short(who, keys), bot('held ' + who), 'throw', throw(who))
    tl(who, keys, 0.1, 6); log('    end:', short(who, keys), bot('held ' + who))
    b = st().get('ability1Cooldown'); press('ability_1'); sleep(0.2); log('    Shift by the real key after Suzu: cd1', b, '->', st().get('ability1Cooldown'))
    c('screenshot p4_%s_hack_me.png' % TAG)
def grenade_case(who):
    def setup(at):
        add('anaE', 'ana', 'red', at[0], at[1] + 6, 180, 8)
    case('AntiHeal', who, setup, lambda: bot('press anaE ability2'), ['antiHeal', 'healCut', 'health', 'phased'], 0.1, 5, 1.0)
def numbed_case(who):
    def setup(at):
        add('anaE', 'ana', 'red', at[0], at[1] + 7, 180); bot('perk anaE ANA_LOCAL_ANESTHETIC')
    case('Numbed', who, setup, lambda: bot('press anaE ability1'), ['asleep', 'speed', 'health'], 0.2, 6, 0.7)
def claws_case(who):
    n = 'cat%d' % XX[0]
    def setup(at):
        add(n, 'jetpack_cat', 'red', at[0], at[1] - 1.6, 0); bot('perk %s JETPACK_CAT_CLAWS_OUT' % n); sleep(0.2)
    def after(): bot('remove ' + n)
    case('Claws', who, setup, lambda: inp(n, 0, 16), ['speed', 'health', 'phased'], 0.1, 8, 0.2, after)
def strike_case(who):
    def setup(at):
        add('sigE', 'sigma', 'red', at[0], at[1] - 1.8, 0); bot('perk sigE SIGMA_HYPER_STRIKE'); bot('strike sigE'); sleep(0.2)
    case('HyperStrike', who, setup, lambda: inp('sigE', 0, 16), ['asleep', 'gravity', 'phased'], 0.1, 8, 0.15)
def decel_case(who):
    def setup(at):
        add('sojE', 'sojourn', 'red', at[0], at[1] + 6, 180, 8); bot('perk sojE SOJOURN_DECELERATION_FIELD')
    case('Deceleration', who, setup, lambda: bot('press sojE ability2'), ['speed', 'phased', 'health'], 0.15, 10, 1.3)
def weaken_case(who):
    def setup(at):
        add('som', 'sombra', 'red', at[0], at[1] + 6, 180, 8)
    case('Weaken', who, setup, lambda: bot('press som ability1'), ['weak', 'phased', 'health'], 0.1, 12, 1.0)
def accretion_case(who):
    x = spot(); at = begin(who, x)
    add('sigE', 'sigma', 'red', at[0], at[1] + 6, 180, 2); sleep(0.2)
    log('--- Accretion on %s: thrown when it lands; how long does the knock-down last?' % who); bot('press sigE ability2')
    t0 = time.time(); 
    while not bot('cc ' + who).get('asleep') and time.time() - t0 < 3: sleep(0.02)
    t1 = time.time(); log('    down at +%.2f; held %s; throw %s' % (t1 - t0, bot('held ' + who), throw(who)))
    ph = False
    while bot('cc ' + who).get('asleep') and time.time() - t1 < 4:
        ph = ph or bot('cc ' + who).get('phased'); sleep(0.02)
    log('    knock-down lasted %.2f s (data v13550 1.1 at least); the Suzu reached it: %s' % (time.time() - t1, ph))
def flux_case(who):
    x = spot(); at = begin(who, x)
    add('sigE', 'sigma', 'red', at[0], at[1] + 5, 180, 18); bot('charge sigE 9999'); sleep(0.2)
    log('--- Flux on %s' % who); bot('press sigE ultimate'); sleep(1.0); inp('sigE', 1, 0); sleep(0.2); inp('sigE', 0, 0)
    t0 = time.time()
    while not bot('cc ' + who).get('lifted') and time.time() - t0 < 3: sleep(0.03)
    log('    lifted at +%.2f, %s; throw %s' % (time.time() - t0, short(who, ['lifted', 'speed', 'health']), throw(who)))
    tl(who, ['lifted', 'phased', 'health', 'speed'], 0.2, 8)
CASES = dict(sleep=sleep_case, stun=stun_case, hack=hack_case, grenade=grenade_case, numbed=numbed_case, claws=claws_case, strike=strike_case, decel=decel_case, weaken=weaken_case, accretion=accretion_case, flux=flux_case)
setup(); mc('/gamemode survival @s')
for fix in FIXES:
    log('################ bot fix 119', fix, bot('fix 119 ' + fix))
    TAG = fix
    for n in NAMES:
        for who in ALL:
            try:
                CASES[n](who)
            except Exception as e:
                log('EXC', n, who, repr(e))
            s = st()
            if s.get('health', 1) <= 0: log('DEAD'); c('respawn'); sleep(1.5)
clear()

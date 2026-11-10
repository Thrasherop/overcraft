# Stun with a lead, what the phase prevents, late viewers. Appended to p4_t119.py's functions by p4_all.py.
def now(n): return bot('cc ' + n)
def stun_case(who):
    for thrown in (False, True):
        x = spot(); at = begin(who, x)
        log('--- Stun on %s, Suzu %s, read by game tick' % (who, 'thrown once the server has it' if thrown else 'not thrown (control)'))
        add('brig', 'brigitte', 'red', at[0], at[1] - 1.8, 0); sleep(0.3)
        bot('charge brig 9999'); bot('press brig ultimate'); sleep(1.2); bot('secondary brig on'); sleep(0.6)
        if who == 'allyB': mc('/tp @s %s -60 -0.5 -41 25' % (x - 2.5)); c('look -41 25'); sleep(0.25)
        else: bot('remove kiriB'); add('kiriB', 'kiriko', 'blue', x - 2, 7, -90, 70); sleep(0.2)
        inp('brig', 3, 0); rows_ = []; t0 = time.time(); stage = 0; tk = None; hd = None
        while time.time() - t0 < 1.5:
            r = now(who); rows_.append((r['now'], r['asleep'], r['phased'], round(r['health'], 2)))
            if r['asleep'] and stage == 0:
                stage = 1; tk = r['now']; inp('brig', 2, 0); inp('brig', 0, 0); bot('secondary brig off'); hd = bot('held ' + who)
                if thrown:
                    if who == 'allyB': c('key ability_2 down')
                    else: bot('press kiriB ability2')
            elif stage == 1 and tk is not None and r['now'] - tk >= 3:
                stage = 2
                if thrown and who == 'allyB': c('key ability_2 up')
        on = [q[0] for q in rows_ if q[1]]; ph = [q[0] for q in rows_ if q[2]]
        log('    ticks read while held', on, '(D 1340: 15 from the first); phase from', ph[:1], '; held at start', hd, '; end', bot('held ' + who), '; health', rows_[-1][3])
        c('screenshot p4_%s_stun_%s_%s.png' % (TAG, who, thrown)); bot('remove brig')
def prevented(name, setup, apply, keys, lead=0.0):
    """The Suzu first; once the server says phased, the effect arrives. Who: a real client, a bot Kiriko throwing."""
    x = spot(); at = begin('me', x); setup(at); sleep(0.3)
    log('--- %s inside the phase, on the real client' % name)
    log('   throw', throw('me')); t0 = time.time()
    while not now('me')['phased'] and time.time() - t0 < 2: sleep(0.01)
    t1 = time.time(); apply(); log('    phased at +%.2f; effect started' % (t1 - t0))
    for i in range(10):
        sleep(0.1); log('    +%.2f %s' % (time.time() - t1, short('me', keys)))
    log('    held', bot('held me'))
def prevent_all():
    def sl(at): add('anaE', 'ana', 'red', at[0], at[1] + 3, 180)
    prevented('Sleep Dart', sl, lambda: bot('press anaE ability1'), ['asleep', 'phased', 'health'])
    def em(at): add('som', 'sombra', 'red', at[0], at[1] + 5, 180); sleep(0.2); bot('charge som 9999')
    # EMP's cast is longer than the phase: started with the throw so its pulse lands in the phase
    x = spot(); at = begin('me', x); em(at); sleep(0.3)
    log('--- EMP inside the phase, on the real client: both pressed, read every tick')
    bot('press som ultimate'); sleep(0.1); log('   throw', throw('me')); rows = []; t0 = time.time()
    while time.time() - t0 < 1.5:
        r = now('me'); rows.append((r['now'], r['phased'], r['silenced'], r['glowing']))
    ph = [q[0] for q in rows if q[1]]; si = [q[0] for q in rows if q[2]]
    log('    phased ticks %s..%s; silenced ticks %s..%s; held %s; me %s' % (ph[0] if ph else None, ph[-1] if ph else None, si[0] if si else None, si[-1] if si else None, bot('held me'), short('me', ['silenced', 'glowing'])))
    log('    (EMP pressed in this order, its pulse time decides: if the silence starts after the phase it isn\'t a test of prevention)')
    def rk(at): add('sigE', 'sigma', 'red', at[0], at[1] + 3, 180, 2); sleep(0.2)
    x = spot(); at = begin('me', x); rk(at); sleep(0.2)
    log('--- Accretion inside the phase, on the real client')
    bot('press sigE ability2'); sleep(0.25); log('   throw', throw('me')); rows = []; t0 = time.time()
    while time.time() - t0 < 2.0:
        r = now('me'); rows.append((r['now'], r['phased'], r['asleep'], round(r['health'], 2)))
    ph = [q[0] for q in rows if q[1]]; si = [q[0] for q in rows if q[2]]
    log('    phased ticks %s..%s; knocked down ticks %s..%s; health %s' % (ph[0] if ph else None, ph[-1] if ph else None, si[0] if si else None, si[-1] if si else None, rows[-1][3]))
def late_viewer():
    """A viewer who arrives after the cleanse must not be told of the state: the real client leaves tracking range, Suzu ends the bot's state, it comes back."""
    x = spot(); at = begin('allyB', x)
    bot('remove kiriB'); add('kiriB', 'kiriko', 'blue', x + 3, 3.5, 90, 70); add('som', 'sombra', 'red', x + 1, 9.5, 180); sleep(0.2); bot('charge som 9999')
    log('--- late viewer: Hack on a bot, Suzu from a bot, the real client away for it')
    mc('/tp @s %s -60 0.5' % (x + 500)); sleep(1.0)
    bot('press som ultimate'); sleep(0.9); log('    hacked', short('allyB', ['silenced', 'glowing']), bot('held allyB'))
    bot('press kiriB ability2'); sleep(0.8); log('    after Suzu', short('allyB', ['silenced', 'glowing']), bot('held allyB'))
    mc('/tp @s %s -60 0.5 0 0' % x); sleep(1.5); log('    back in range: client draws', bot('held allyB'))
    c('screenshot p4_%s_late_viewer.png' % TAG)
    # the sleep the same way
    bot('remove som'); add('anaE', 'ana', 'red', x + 1, 9.5, 180); mc('/tp @s %s -60 0.5' % (x + 500)); sleep(1.0)
    bot('press anaE ability1'); sleep(0.6); log('    asleep', short('allyB', ['asleep']), bot('held allyB'))
    sleep(14) if False else None
    mc('/overcraft reset'); bot('remove kiriB'); add('kiriB', 'kiriko', 'blue', x + 3, 3.5, 90, 70); sleep(0.2)
    bot('press kiriB ability2'); sleep(1.1); log('    after Suzu', short('allyB', ['asleep']), bot('held allyB'))
    mc('/tp @s %s -60 0.5 0 0' % x); sleep(1.5); log('    back in range: client draws', bot('held allyB'))

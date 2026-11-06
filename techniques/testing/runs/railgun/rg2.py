# Round two of #126. Harness keys unless a row says real mouse. Single player: the "lag" rows are a dev delay on her hero payloads, not real latency.
SRC = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg_table.py", encoding="utf8").read()
A = ARGS; ARGS = ['none']; exec(SRC); NAMES = A or None
log('### round two', time.strftime('%H:%M:%S'), 'fps', st().get('fps'))
def counts(tr):
    m = parse(tr)
    t0 = first(m, lambda x: x == 'press', 0) or 0
    return dict(presses=sum(1 for t, x in m if x == 'press'), fx=sum(1 for t, x in m if x == 'fx'), clip=sum(1 for t, x in m if x == 'clip'),
                srv=sum(1 for t, x in m if x == 'srv_shot'), fire=sum(1 for t, x in m if x.startswith('snd:charged.fire')))
def gaps(tr, least=25):
    # the long frames in a "presstrace frames on" trace, from the first press
    m = parse(tr); out = []; last = None; t0 = first(m, lambda x: x == 'press', 0) or 0
    for t, x in m:
        if x in ('f', 'frame'):
            if last is not None and t - last >= least: out.append((round(last - t0), round(t - last)))
            last = t
    return out
def ready():
    guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
if want('hold') or want('rig'):
    rig()
if want('first'):
    # no screenshots anywhere before this; run it as the first thing in a fresh client
    sleep(8); home(); sleep(1); c('presstrace frames on')
    def probe(name, keys, energy=None, wait=1.6):
        if energy is not None: c('presstrace energy %s' % energy); sleep(0.2)
        n0 = len(lines()); c('presstrace start'); c('presstrace sample 1500')
        for k, hold in keys:
            c('key %s down' % k); sleep(hold); c('key %s up' % k)
        sleep(wait); tr = c('presstrace').get('trace', '')
        g = gaps(tr)
        log('FIRST', name, 'long frames (ms after the press, length):', g, '| fps', st().get('fps'))
        log('TRACE', tr.replace(' f |', '')[:500])
        ln = lines()[n0:]; j = [i for i, l in enumerate(ln) if 'SAMPLE' in l]
        if j: log('\n'.join(ln[j[0]:j[0] + 42]))
    probe('idle, nothing pressed', [], wait=1.0)
    probe('first Charged Shot', [('primary_fire', 0.06)], energy=50)
    sleep(1)
    probe('second Charged Shot', [('primary_fire', 0.06)], energy=50)
    sleep(1); probe('first rounds', [('secondary_fire', 0.4)])
    sleep(1); probe('rounds again', [('secondary_fire', 0.4)])
    sleep(1); probe('first reload', [('reload', 0.05)])
    sleep(1.5); probe('first slide', [('ability_1', 0.05)])
    sleep(1); probe('first Disruptor', [('ability_2', 0.05)], wait=2.5)
    sleep(3); mc('/giveult @s', 0.1); probe('first Overclock', [('ultimate', 0.05)], wait=2.5)
    sleep(9); c('presstrace frames off')
if want('clip'):
    # what the animator says frame by frame: each request is one frame
    def anims(n=10):
        out = []
        for i in range(n): s = st(); out.append(s.get('anim'))
        return out
    for name, pre in (('reload', lambda: (c('presstrace ammo 20'), sleep(0.1), tap('reload'), sleep(0.3))),
                      ('melee', lambda: (tap('quick_melee', 0.05), sleep(0.1))),
                      ('disruptor', lambda: (c('key ability_2 down'), sleep(0.02), c('key ability_2 up'))),
                      ('nothing', lambda: None)):
        ready(); c('presstrace energy 80'); sleep(0.1); pre(); b = st().get('anim')
        tag = '%s_%s' % (os.environ.get('RG_TAG', 'x'), name)
        c('key primary_fire down'); a = anims(); c('key primary_fire up')
        log('CLIP', name, 'before', b, 'frames after the press', a); sleep(1.0)
        if want('shots'):
            ready(); c('presstrace energy 80'); sleep(0.1); pre()
            c('screenshot rg2_%s_0' % tag); c('key primary_fire down')
            for i in range(1, 7): c('screenshot rg2_%s_%d' % (tag, i))
            c('key primary_fire up'); sleep(1.2)
if want('lag'):
    LAG = 100
    c('presstrace lag %d' % LAG); sleep(0.4); log('=== dev lag %d ms each way on her hero payloads: not real latency' % LAG)
    def ghost(name, tr, extra=''):
        log('LAG', name, counts(tr), extra, '|', tr[:420])
    def playing(): return c('presstrace sounding railgun.reload').get('sounding')
    # 4: R, then the right button inside the ping
    for i in range(5 if want('lag4') else 0):
        ready(); c('presstrace ammo 20'); c('presstrace energy 50'); sleep(0.4); c('presstrace start')
        c('key reload down'); c('key reload up'); c('key primary_fire down'); c('key primary_fire up'); sleep(0.5)
        s = st(); p = playing(); tr = c('presstrace').get('trace', '')
        log('LAG4 R+press', i, 'at +0.5 s anim', s.get('anim'), 'ammo', s.get('primaryAmmo'), 'rel', round(s.get('primaryReload'), 2), 'playing', p, '|', tr[:260]); sleep(1.2)
    # 4: hold left to empty, the right button as it does
    for i in range(4 if want('lag4') else 0):
        ready(); c('presstrace ammo 3'); c('presstrace energy 50'); sleep(0.4); c('presstrace start')
        c('key secondary_fire down'); sleep(0.3 + 0.02 * i); c('key primary_fire down'); c('key primary_fire up'); c('key secondary_fire up'); sleep(0.5)
        s = st(); p = playing(); tr = c('presstrace').get('trace', '')
        log('LAG4 empty+press', i, 'at +0.5 s anim', s.get('anim'), 'ammo', s.get('primaryAmmo'), 'rel', round(s.get('primaryReload'), 2), 'playing', p, '|', tr[:320]); sleep(2.2)
    # 5a: a press the client can't call in the last of its lock (the tick path; the server fires it, she learns a round trip on), then a click as her lock runs out
    # (energy reset 0.2 s after the first press: the server's shot comes a ping on and would spend it)
    for i, third in enumerate((0.66, 0.70, 0.74, 0.78, 0.82, 0.86) * 2 if want('lag5') else ()):
        ready(); c('presstrace energy 50'); sleep(0.4); c('presstrace start'); t0 = time.time(); tap('primary_fire', 0.02); sleep(0.3); c('presstrace energy 50')
        while time.time() - t0 < 0.56: sleep(0.002)
        tap('primary_fire', 0.02)
        while time.time() - t0 < third: sleep(0.002)
        c('key primary_fire down'); c('key primary_fire up'); sleep(0.9)
        ghost('5a third %.2f' % third, c('presstrace').get('trace', '')); sleep(0.2)
    # 5b: the button stays down through the recovery's end: a shown press buffered, then overwritten?
    for i, gap in enumerate((0.60, 0.62, 0.64, 0.66, 0.68, 0.70, 0.72, 0.74) * 2 if want('lag5') else ()):
        ready(); c('presstrace energy 50'); sleep(0.4); c('presstrace start'); t0 = time.time(); tap('primary_fire', 0.02); sleep(0.3); c('presstrace energy 50')
        while time.time() - t0 < gap: sleep(0.002)
        c('key primary_fire down'); sleep(0.3); c('key primary_fire up'); sleep(0.7)
        ghost('5b gap %.2f' % gap, c('presstrace').get('trace', '')); sleep(0.2)
    # 5c: Q, then the right button inside a round trip
    for i in range(3 if want('lag5c') else 0):
        ready(); mc('/giveult @s', 0.1); c('presstrace energy 50'); sleep(0.4); c('presstrace start')
        c('key ultimate down'); c('key ultimate up'); c('key primary_fire down'); c('key primary_fire up'); sleep(1.2)
        ghost('5c', c('presstrace').get('trace', '')); sleep(9.5)
    c('presstrace lag 0'); sleep(0.3)
if want('buffer'):
    # 7: a press before the recovery's end, by the trace's clock: does it fire? No lag.
    for gap in (0.24, 0.27, 0.30, 0.33, 0.36, 0.39, 0.42, 0.45, 0.48, 0.51, 0.54) * 2:
        ready(); c('presstrace energy 50'); sleep(0.15); c('presstrace start'); c('key primary_fire down'); c('key primary_fire up'); c('presstrace energy 50'); sleep(gap)
        c('key primary_fire down'); c('key primary_fire up'); sleep(0.5)
        tr = c('presstrace').get('trace', ''); m = parse(tr); P = [t for t, x in m if x == 'press']; S = [t for t, x in m if x == 'srv_shot']
        log('BUF press gap %.0f ms after the first shot:' % ((P[1] - S[0]) if len(P) > 1 and S else -1), 'shots', len(S), 'second at +%.0f' % (S[1] - S[0]) if len(S) > 1 else '', 'looks', sum(1 for t, x in m if x == 'fx')); sleep(0.2)
    # the same in Overclock, where it matters
    ready(); mc('/giveult @s', 0.1); tap('ultimate', 0.05); sleep(1.0)
    for gap in (0.24, 0.28, 0.32, 0.36, 0.40, 0.44, 0.48):
        c('presstrace start'); c('key primary_fire down'); c('key primary_fire up'); sleep(gap); c('key primary_fire down'); c('key primary_fire up'); sleep(0.75)
        tr = c('presstrace').get('trace', ''); m = parse(tr); P = [t for t, x in m if x == 'press']; S = [t for t, x in m if x == 'srv_shot']
        log('BUF OC press gap %.0f ms:' % ((P[1] - S[0]) if len(P) > 1 and S else -1), 'shots', len(S), 'looks', sum(1 for t, x in m if x == 'fx'))
    sleep(4)
if want('hold'):
    # 6: real rounds, then both buttons held
    ready(); c('presstrace energy 50'); sleep(0.1); c('presstrace start'); c('key secondary_fire down'); sleep(0.5); c('key primary_fire down'); sleep(1.5); c('key primary_fire up'); sleep(0.3); c('key secondary_fire up'); sleep(0.2)
    log('HOLD both, real rounds:', c('presstrace').get('trace', '')[:700]); s = st(); log('ammo', s.get('primaryAmmo'), 'meter', s.get('meter'))
    ready(); c('presstrace energy 50'); sleep(0.1); c('heard'); c('presstrace start'); c('key primary_fire down'); sleep(1.5); c('key primary_fire up'); sleep(0.2)
    log('HOLD right alone, nothing landing:', c('presstrace').get('trace', '')[:300], '| heard', c('heard').get('heard', '')[:300])
    ready(); mc('/giveult @s', 0.1); tap('ultimate', 0.05); sleep(1.0); c('presstrace start'); c('key primary_fire down'); sleep(2.2); c('key primary_fire up'); sleep(0.2)
    log('HOLD right in Overclock:', c('presstrace').get('trace', '')[:600]); sleep(7)
if want('holdammo'):
    ready(); c('presstrace ammo 9'); c('presstrace energy 50'); sleep(0.2); c('presstrace start'); c('key secondary_fire down'); sleep(0.5); c('key primary_fire down'); sleep(1.5); c('key primary_fire up'); sleep(0.3); c('key secondary_fire up'); sleep(0.2)
    s = st(); log('HOLD both, 9 rounds:', c('presstrace').get('trace', '')[:700], 'ammo', s.get('primaryAmmo'), 'rel', s.get('primaryReload'))
if want('others'):
    # item 8: two Sojourn bots reloading in earshot; one's shot
    ready(); a = 'rgA%d' % (int(time.time()) % 10000); b = 'rgB%d' % (int(time.time()) % 10000)
    log(c('bot add %s sojourn blue 5.5 -60 5.5 0' % a), c('bot add %s sojourn blue -4.5 -60 -3.5 0' % b)); sleep(0.5)
    c('presstrace ammo 20 ' + a) if False else None
    log(c('bot input %s 1 0 0' % a), c('bot input %s 1 0 0' % b)); sleep(0.5); log(c('bot input %s 0 0 0' % a), c('bot input %s 0 0 0' % b)); sleep(0.2)
    c('presstrace energy 50 ' + a); sleep(0.1)
    log(c('bot input %s 0 8 0' % a), c('bot input %s 0 8 0' % b)); sleep(0.25)
    log('both reloading: playing', c('bot playing railgun.reload').get('playing', ''))
    log(c('bot input %s 2 0 0' % a)); sleep(0.15); log('A fired: playing', c('bot playing railgun.reload').get('playing', '')); sleep(0.3); log('+0.3 s', c('bot playing railgun.reload').get('playing', ''))
    c('bot input %s 0 0 0' % a); c('bot remove ' + a); c('bot remove ' + b); sleep(0.5)
if want('soundpoll'):
    def poll(tag, lag, r_gap):
        ready(); c('presstrace lag %d' % lag); sleep(0.3); c('presstrace ammo 20'); c('presstrace energy 50'); sleep(0.3)
        c('key reload down'); c('key reload up')
        if r_gap: sleep(r_gap)
        t0 = time.time(); c('key primary_fire down'); c('key primary_fire up'); r = []
        for i in range(14):
            r.append((round(time.time() - t0, 2), c('presstrace sounding railgun.reload').get('sounding'), st().get('anim'))); sleep(0.05)
        log('CUTSOUND', tag, r); c('presstrace lag 0'); sleep(1.5)
    poll('no lag, 0.3 s in', 0, 0.3); poll('lag 100, crossing', 100, 0); poll('lag 100, crossing', 100, 0)
    ready(); c('presstrace ammo 20'); sleep(0.2); c('key reload down'); c('key reload up'); t0 = time.time(); r = []
    for i in range(14):
        r.append((round(time.time() - t0, 2), c('presstrace sounding railgun.reload').get('sounding'))); sleep(0.08)
    log('CUTSOUND control, not cut', r); sleep(1)
log('### done')

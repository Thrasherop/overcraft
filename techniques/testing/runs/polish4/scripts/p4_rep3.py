XSTART = globals().get('XSTART', 110000)
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
setup(); mc('/gamemode survival @s'); mc('/time set midnight')
log('### repeats, reads running from before the press, no script calls in the loop except where said')
def rig_rally(x, z):
    b = add('brig', 'brigitte', 'red', x, z - 1.8, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5); return b
for who in ('bot', 'me'):
    for suzu in (False, True, False, True, False, True, False, True):
        x = spot(); me('mercy', x, -14 if who == 'bot' else 0.5)
        n = add('ally', 'mercy', 'blue', x, 3.5, 0) if who == 'bot' else 'me'; z = 3.5 if who == 'bot' else 0.5
        k = add('kiri', 'kiriko', 'blue', x, z + 4.5, 180, 72); b = rig_rally(x, z); inp(b, 3, 0)
        if suzu: sleep(0.05); bot('press %s ability2' % k)
        r = rows(n, 1.3, ['asleep', 'phased', 'health']); inp(b, 0, 0); bot('secondary %s off' % b)
        on = span(r, 1); ph = span(r, 2); log('Rally stun on %s, Suzu %s: held %s, phase %s, tick of phase in the stun %s, gap largest %s' % (who, suzu, on, ph, ph[0] - on[0] if on and ph else None, max(r[i + 1][0] - r[i][0] for i in range(len(r) - 1))))
for label, rally, suzu in (('Accretion alone', False, False), ('+ Rally', True, False), ('+ Rally + Suzu', True, True), ('Accretion alone', False, False), ('+ Rally', True, False), ('+ Rally + Suzu', True, True), ('Accretion alone', False, False), ('+ Rally', True, False), ('+ Rally + Suzu', True, True)):
    x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 2, 3.5, -90, 72); b = rig_rally(x, 3.5) if rally else None; g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3); bot('press %s ability2' % g); st_ = [0]
    def ev(q):
        if q['asleep'] and st_[0] == 0 and rally:
            st_[0] = 1; inp(b, 3, 0); st_.append(time.time())
        elif st_[0] == 1 and time.time() - st_[1] > 0.1:
            st_[0] = 2; inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b)
            if suzu: bot('press %s ability2' % k)
    r = rows(a, 3, ['asleep', 'phased', 'health'], ev); log('%s: held %s, phase %s, health %s' % (label, span(r, 1), span(r, 2), r[-1][3])); sleep(0.3)
for suzu in (False, True):
    x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 2, 3.5, -90, 72); g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3); h = add('hyp', 'sigma', 'red', x + 1.8, 3.5, 90); bot('perk %s SIGMA_HYPER_STRIKE' % h); bot('strike %s' % h); bot('press %s ability2' % g); st_ = [0]
    def ev(q):
        if q['asleep'] and st_[0] == 0:
            st_[0] = 1; inp(h, 0, 16)
            if suzu: bot('press %s ability2' % k)
    r = rows(a, 3, ['asleep', 'phased', 'gravity'], ev); log('Accretion + Hyper Strike, Suzu %s: held %s, phase %s, gravity %s' % (suzu, span(r, 1), span(r, 2), sorted(set(round(q[3], 3) for q in r))))
for suzu in (False, True):
    x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 2, 3.5, -90, 72); n = add('hyp', 'sigma', 'red', x + 1.8, 3.5, 90); bot('perk %s SIGMA_HYPER_STRIKE' % n); bot('strike %s' % n); inp(n, 0, 16); t0 = time.time()
    if suzu: sleep(0.05); bot('press %s ability2' % k)
    r = rows(a, 1.5, ['asleep', 'phased', 'gravity']); log('Hyper Strike alone, Suzu %s: held %s, phase %s, gravity reads %s' % (suzu, span(r, 1), span(r, 2), sorted(set(round(q[3], 3) for q in r))))
clear(); log('DONE')

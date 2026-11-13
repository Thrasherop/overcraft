XSTART = globals().get('XSTART', 160000)
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
setup(); mc('/gamemode survival @s'); mc('/time set midnight')
def z(tag): return hp('@e[tag=%s,sort=nearest,limit=1]' % tag)
if False: log('skip')
log('--- #122: a red bot Brigitte square ahead of the wave, 3.5 m past the bashed zombie, facing it, her barrier up or not; 3 each')
for i, barrier in enumerate((False, True, False, True, False, True)):
    x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0)
    mc('/summon minecraft:zombie %s -60 2.7 {NoAI:1b,Tags:["a"],Health:20f,active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % x, 0.1)
    d = add('bri', 'brigitte', 'red', x + 0.8, 6.5, 180)
    if barrier: bot('secondary %s on' % d); sleep(0.6)
    mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5); h0 = cc(d)['health']; c('key secondary_fire down'); r = rows(d, 0.5, ['asleep', 'health']); c('key secondary_fire up'); sleep(0.5); c('key primary_fire up')
    log('  run', i, 'barrier', barrier, 'bashed', z('a'), 'bot', h0, '->', cc(d)['health'], 'bot stunned', span(r, 1), 'her barrier', bot('read ' + d).get('barrierHealth'))
log('--- Rally\'s stun, the Suzu after it lands: reads from before the bash; 4 each, bot and real client')
for who in ('bot', 'me'):
    for suzu in (False, True, False, True, False, True, False, True):
        x = spot(); me('mercy', x, -14 if who == 'bot' else 0.5)
        n = add('ally', 'mercy', 'blue', x, 3.5, 0) if who == 'bot' else 'me'; z0 = 3.5 if who == 'bot' else 0.5
        k = add('kiri', 'kiriko', 'blue', x + 1, z0 + 3, 180, 72); b = add('brig', 'brigitte', 'red', x, z0 - 1.8, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5); inp(b, 3, 0); th = [False]; t0 = time.time()
        def ev(q):
            if suzu and not th[0] and q['asleep']: th[0] = True; bot('press %s ability2' % k)
        r = rows(n, 1.3, ['asleep', 'phased', 'health', 'z'], ev); inp(b, 0, 0); bot('secondary %s off' % b)
        on = span(r, 1); ph = span(r, 2); log('Rally stun on %s, Suzu %s: held %s, phase %s, stun ended %s ticks after the phase began, largest gap %s, z %s' % (who, suzu, on, ph, on[1] - ph[0] if on and ph else None, max(r[i + 1][0] - r[i][0] for i in range(len(r) - 1)), round(r[-1][4], 1)))
log('--- Accretion then Rally\'s stun then a Suzu: with the Kiriko where the target ends up')
for label, rally, suzu in (('Accretion alone', False, False), ('+ Rally', True, False), ('+ Rally + Suzu', True, True), ('Accretion alone', False, True), ('+ Rally', True, False), ('+ Rally + Suzu', True, True), ('Accretion alone', False, False), ('+ Rally', True, False), ('+ Rally + Suzu', True, True)):
    x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 3, 6, -90, 72); b = None
    if rally:
        b = add('brig', 'brigitte', 'red', x, 1.7, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5)
    g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3); bot('press %s ability2' % g); st_ = [0]
    def ev(q):
        if q['asleep'] and st_[0] == 0:
            st_[0] = 1; st_.append(time.time())
            if rally: inp(b, 3, 0)
        elif st_[0] == 1 and time.time() - st_[1] > 0.1:
            st_[0] = 2
            if rally: inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b)
            if suzu: bot('press %s ability2' % k)
    r = rows(a, 3, ['asleep', 'phased', 'health', 'x', 'z'], ev); log('%s, Suzu %s: held %s, phase %s, health %s, at %s %s' % (label, suzu, span(r, 1), span(r, 2), r[-1][3], round(r[-1][4] - x, 1), round(r[-1][5], 1))); sleep(0.3)
clear(); log('DONE')

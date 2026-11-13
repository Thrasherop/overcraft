XSTART = 170000
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
setup(); mc('/gamemode survival @s'); mc('/time set midnight')
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
clear(); log('DONE')

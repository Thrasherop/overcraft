D = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
LABEL = "tree a20c352: Rally's stun on a bot ally, the real key, read without a gap"
src = open(D + "p4_t119.py", encoding="utf8").read()
head, tail = src.split("setup(); mc('/gamemode survival @s')\n")
exec(head)
XX[0] = 4800; setup(); mc('/gamemode survival @s')
for fix in ('off', 'on', 'off', 'on'):
    bot('fix 119 ' + fix)
    x = spot(); at = begin('allyB', x)
    add('brig', 'brigitte', 'red', at[0], at[1] - 1.8, 0); sleep(0.3)
    bot('charge brig 9999'); bot('press brig ultimate'); sleep(1.2); bot('secondary brig on'); sleep(0.6)
    mc('/tp @s %s -60 -0.5 -41 25' % (x - 2.5)); c('look -41 25'); sleep(0.25)
    inp('brig', 3, 0); rows = []; t0 = time.time(); stage = 0
    while time.time() - t0 < 1.5:
        r = bot('cc allyB'); rows.append((r['now'], r['asleep'], r['phased'], round(r['health'], 2)))
        if r['asleep'] and stage == 0:
            stage = 1; inp('brig', 2, 0); inp('brig', 0, 0); bot('secondary brig off'); c('key ability_2 down'); tk = r['now']
        elif stage == 1 and r['now'] - tk >= 3:
            stage = 2; c('key ability_2 up')
    on = [q for q in rows if q[1]]; ph = [q for q in rows if q[2]]
    log('fix %s: real key at tick %s; stun read from %s to %s = %s ticks (D 15); phase from %s; health %s; cd2 %s' % (fix, tk, on[0][0], on[-1][0], on[-1][0] - on[0][0] + 1, ph[0][0] if ph else None, rows[-1][3], st().get('ability2Cooldown')))
    clear()
bot('fix 119 on'); log('DONE')

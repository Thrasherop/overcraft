D = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
LABEL = "tree a20c352, stun read by game tick; looks at 1280x720"
src = open(D + "p4_t119.py", encoding="utf8").read()
head, tail = src.split("setup(); mc('/gamemode survival @s')\n")
exec(head)
c('window 1280 720'); sleep(1); XX[0] = 4000
setup(); mc('/gamemode survival @s')
def now(n): return bot('cc ' + n)
def stun_after(who, fix):
    x = spot(); at = begin(who, x)
    log('--- Stun on %s, fix %s, Suzu thrown once the server has it' % (who, fix))
    add('brig', 'brigitte', 'red', at[0], at[1] - 1.8, 0); sleep(0.3)
    bot('charge brig 9999'); bot('press brig ultimate'); sleep(1.2); bot('secondary brig on'); sleep(0.6)
    if who == 'allyB': mc('/tp @s %s -60 0 -63 10' % (x - 6)); c('look -63 10'); sleep(0.25)
    else: bot('remove kiriB'); add('kiriB', 'kiriko', 'blue', x - 2, 7, -90, 70); sleep(0.2)
    inp('brig', 3, 0); rows = []; t0 = time.time(); done = False
    while time.time() - t0 < 1.5:
        r = now(who); rows.append((r['now'], r['asleep'], r['phased'], round(r['health'], 2)))
        if r['asleep'] and not done:
            done = True; inp('brig', 2, 0); inp('brig', 0, 0); bot('secondary brig off'); log('    throw', throw(who))
    on = [q for q in rows if q[1]]; ph = [q for q in rows if q[2]]
    log('    stun ticks %s..%s = %s (D 1340: 15); phase from tick %s; held %s; health %s' % (on[0][0] if on else None, on[-1][0] if on else None, on[-1][0] - on[0][0] + 1 if on else 0, ph[0][0] if ph else None, bot('held ' + who), rows[-1][3]))
    inp('brig', 0, 0); bot('secondary brig off'); clear()
def look_case(fix, name, setup, apply, wait):
    x = spot(); clear(); pick('mercy'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set noon')
    tp(x, -60, 0.5, 0, 12)
    add('allyB', 'lucio', 'blue', x, 4.5, 180); add('kiriB', 'kiriko', 'blue', x + 4, 4.5, 90, 70); setup(x); sleep(0.4)
    apply(); sleep(wait); log('---', name, 'fix', fix, 'on', short('allyB', ['asleep', 'silenced', 'glowing', 'antiHeal']), bot('held allyB')); c('screenshot p4_look_%s_%s_a.png' % (name, fix))
    bot('press kiriB ability2'); sleep(0.75); log('    after Suzu', short('allyB', ['asleep', 'silenced', 'glowing', 'antiHeal']), bot('held allyB')); c('screenshot p4_look_%s_%s_b.png' % (name, fix))
for fix in ('off', 'on'):
    bot('fix 119 ' + fix); TAG = fix
    for who in ALL:
        stun_after(who, fix)
    look_case(fix, 'hack', lambda x: (add('som', 'sombra', 'red', x - 8, 4.5, -90), bot('charge som 9999')), lambda: bot('press som ultimate'), 0.9)
    look_case(fix, 'sleep', lambda x: add('anaE', 'ana', 'red', x - 8, 4.5, -90), lambda: bot('press anaE ability1'), 0.6)
    look_case(fix, 'grenade', lambda x: add('anaE', 'ana', 'red', x - 7, 4.5, -90, 6), lambda: bot('press anaE ability2'), 1.0)
bot('fix 119 on'); clear(); log('DONE')

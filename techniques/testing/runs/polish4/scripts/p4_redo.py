XSTART = 60000
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
setup(); mc('/gamemode survival @s')
log('### again: runs where the Suzu missed in crit_final')
for i in range(3):
    x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 2, 3.5, -90, 72); b = add('brig', 'brigitte', 'red', x, 1.7, 0); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5)
    g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3); bot('press %s ability2' % g); st_ = [0]
    def ev(q):
        if q['asleep'] and st_[0] == 0: st_[0] = 1; inp(b, 3, 0); st_.append(time.time())
        elif st_[0] == 1 and time.time() - st_[1] > 0.1:
            st_[0] = 2; inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b); bot('press %s ability2' % k)
    r = rows(a, 3, ['asleep', 'phased', 'health'], ev); log('--- 5 Accretion, Rally, Suzu: held', span(r, 1), 'phase', span(r, 2), 'health', r[-1][3], '(Accretion alone is 22 ticks)'); sleep(0.5)
for i in range(3):
    x = spot(); me('mercy', x); n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40); bot('charge %s 9999' % n); k = add('kiri', 'kiriko', 'blue', x - 2, 0.5, -90, 72); sleep(0.2)
    bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); sleep(0.3); inp(n, 0, 0); t0 = time.time()
    while not cc('me')['carriedBy'] and time.time() - t0 < 3: sleep(0.02)
    t = sh('me', 'carriedBy', 'asleep', 'y'); bot('press %s ability2' % k); r = rows('me', 2.5, ['phased', 'carriedBy', 'asleep', 'y']); log('--- Suzu mid-carry: at the throw', t, '; phase', span(r, 1), 'carried', span(r, 2), 'knocked', span(r, 3), 'y end', r[-1][4]); c('screenshot p4_fin_tether.png'); sleep(5)
clear(); log('DONE')

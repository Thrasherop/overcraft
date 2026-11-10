XSTART = 95000
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
setup(); mc('/gamemode survival @s'); c('window 1280 720')
log('### Hyper Strike\'s look on a bot ally, seen by the real client, a Suzu 0.1 s after; fix off and on; a Rally stun\'s halo the same')
for fix in ('off', 'on'):
    bot('fix 119 ' + fix)
    x = spot(); me('mercy', x, 0.5, 0, 12); a = add('ally', 'lucio', 'blue', x, 4.5, 180); k = add('kiri', 'kiriko', 'blue', x + 4, 4.5, 90, 72); n = add('sig', 'sigma', 'red', x + 1.8, 4.5, 90); bot('perk %s SIGMA_HYPER_STRIKE' % n); bot('strike %s' % n); sleep(0.2)
    inp(n, 0, 16); sleep(0.1); log(' ', fix, 'struck', sh(a, 'asleep', 'gravity'), bot('held ' + a)); c('screenshot p4_look_strike_%s_a.png' % fix); bot('press %s ability2' % k); sleep(0.3); log('   +0.3', sh(a, 'asleep', 'gravity', 'phased'), bot('held ' + a)); c('screenshot p4_look_strike_%s_b.png' % fix); sleep(1.5)
    x = spot(); me('mercy', x, 0.5, 0, 12); a = add('ally', 'lucio', 'blue', x, 4.5, 180); k = add('kiri', 'kiriko', 'blue', x + 4, 4.5, 90, 72); b = add('brig', 'brigitte', 'red', x - 1.8, 4.5, -90); bot('charge %s 9999' % b); bot('press %s ultimate' % b); sleep(1.2); bot('secondary %s on' % b); sleep(0.5)
    inp(b, 3, 0); sleep(0.1); inp(b, 2, 0); inp(b, 0, 0); bot('secondary %s off' % b); log(' ', fix, 'stunned', sh(a, 'asleep'), bot('held ' + a)); c('screenshot p4_look_stun_%s_a.png' % fix); bot('press %s ability2' % k); sleep(0.3); log('   +0.3', sh(a, 'asleep', 'phased'), bot('held ' + a)); c('screenshot p4_look_stun_%s_b.png' % fix); sleep(1)
bot('fix 119 on'); clear(); log('DONE')

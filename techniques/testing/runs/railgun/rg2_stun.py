SRC = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg_table.py", encoding="utf8").read()
ARGS = ['none']; exec(SRC); NAMES = None
guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
for i in range(2):
    nm = 'rgsig%d' % (int(time.time()) % 10000)
    c('bot add %s sigma red 0.5 -60 8.5 180' % nm); mc('/gamemode survival ' + nm, 0.15); c('bot look %s 180 0' % nm); sleep(0.3)
    log(c('bot press %s ability2' % nm))
    for k in range(120):
        cc = c('bot cc me')
        if cc.get('asleep'): break
        sleep(0.01)
    log('stunned (Accretion)', cc.get('asleep'), 'after', k, 'polls')
    if cc.get('asleep'):
        c('presstrace energy 50'); c('presstrace start'); c('key primary_fire down'); c('key primary_fire up'); sleep(0.2); tr = c('presstrace').get('trace', '')
        log('press stunned:', tr[:220] or '(nothing)', '| meter', st().get('meter'))
    c('bot remove ' + nm); sleep(3); guard(); home()
log('### done')

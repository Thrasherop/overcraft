# Item 8: two Sojourn bots reloading in earshot, one's shot: is the other's reload still listed? Polled.
SRC = open(r"C:/Users\ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg_table.py".replace('\\','/'), encoding="utf8").read()
ARGS = ['none']; exec(SRC); NAMES = None
guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
def run(fire, tag):
    a = 'rgA%d' % (int(time.time()) % 10000); b = 'rgB%d' % (int(time.time()) % 10000)
    c('bot add %s sojourn blue 5.5 -60 5.5 0' % a); c('bot add %s sojourn blue -4.5 -60 -3.5 0' % b); sleep(0.5)
    c('bot input %s 1 0 0' % a); c('bot input %s 1 0 0' % b); sleep(0.5); c('bot input %s 0 0 0' % a); c('bot input %s 0 0 0' % b); sleep(0.2)
    c('presstrace energy 50 ' + a); sleep(0.1)
    c('bot input %s 0 8 0' % a); c('bot input %s 0 8 0' % b); sleep(0.2)
    t0 = time.time(); c('bot input %s 2 0 0' % a) if fire else None; r = []
    for i in range(16):
        r.append((round(time.time() - t0, 2), c('presstrace sounding reload_3p').get('sounding'))); sleep(0.08)
    log(tag, 'channels of reload_3p running over time:', r)
    c('bot input %s 0 0 0' % a); c('bot remove ' + a); c('bot remove ' + b); sleep(1.2)
run(False, 'control, nobody fires'); run(True, 'A fires'); run(True, 'A fires')
log('### done')

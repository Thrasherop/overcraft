# 5b hunted: a shown press inside the server's last tick of recovery, the button down: her copies doubled? Dev lag 100 ms: not real latency.
SRC = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg2.py", encoding="utf8").read()
ARGS = ['none']; exec(SRC); NAMES = None
c('presstrace lag 100'); sleep(0.4)
dbl = 0; n = 0; ghosts = 0; shown2 = 0
for i, gap in enumerate((0.67, 0.68, 0.69, 0.70, 0.71, 0.72) * 8):
    guard(); mc('/overcraft reset', 0.05); home(); sleep(0.1); c('presstrace energy 50'); sleep(0.3); c('presstrace start'); t0 = time.time(); tap('primary_fire', 0.02); sleep(0.3); c('presstrace energy 50')
    while time.time() - t0 < gap: sleep(0.002)
    c('key primary_fire down'); sleep(0.3); c('key primary_fire up'); sleep(0.55)
    tr = c('presstrace').get('trace', ''); k = counts(tr); n += 1
    if k['fire'] > 2: dbl += 1
    if k['fx'] > k['srv']: ghosts += 1
    log('5b gap %.2f' % gap, k, tr[:500] if k['fire'] > 2 else '')
log('tries', n, 'doubled', dbl, 'ghost', ghosts)
c('presstrace lag 0'); sleep(0.2)
log('### done')

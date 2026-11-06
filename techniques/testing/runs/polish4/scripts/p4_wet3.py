XSTART = globals().get('XSTART', 190000)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_sixth.py", encoding="utf8").read()
ONLY = ('none',)
exec(src.replace("clear(); log('DONE')\n", ""))
log('### bash off a 9 m ledge onto a ladder on a wall ahead, never touching ground; step off it backwards: does the drop hurt? Control: walk off the same ladder with no bash')
for bash in (True, False, True, False, True):
    x = spot(r=24); mc('/fill %d -60 -3 %d -52 3 minecraft:stone' % (x - 4, x + 4), 0.2); mc('/fill %d -60 8 %d -48 8 minecraft:stone' % (x - 4, x + 4), 0.1); mc('/fill %d -60 7 %d -50 7 minecraft:ladder[facing=north]' % (x - 4, x + 4), 0.1)
    me('brigitte', x, 2.0, 0, 0); mc('/time set midnight')
    if bash:
        tp(x, -51, 2.0, 0, 0); c('look 0 0'); sleep(1.6); c('heard'); h0 = st()['health']; c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); sleep(0.6); c('key primary_fire up')
    else:
        tp(x, -52, 7.5, 0, 0); c('look 0 0'); sleep(1.6); c('heard'); h0 = st()['health']
    s0 = st(); c('look 180 0'); c('key forward down'); sleep(0.5); c('key forward up'); t0 = time.time()
    while time.time() - t0 < 3 and not st().get('onGround'): sleep(0.04)
    sleep(0.4); s1 = st(); log('  bash %s: on the ladder at y %.2f z %.2f ground %s; end y %.2f z %.2f, health %.1f -> %.1f, pain %s' % (bash, s0['y'], s0['z'], s0.get('onGround'), s1['y'], s1['z'], h0, s1['health'], pains()))
clear(); log('DONE')

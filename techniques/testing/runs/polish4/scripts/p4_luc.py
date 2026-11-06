XSTART = globals().get('XSTART', 260000)
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
log('TREE (git at launch):', open(r'E:/claude/mods/overcraft_polish4/run/p4_launch_tree.txt').read().strip(), time.strftime('%H:%M:%S'))
setup(); mc('/gamemode survival @s'); mc('/time set midnight'); mc('/gamerule minecraft:natural_health_regeneration false')
def run(height):
    x = spot(r=30)
    # a pillar of ground and a wall on it, ending in the air `height` m up
    mc('/fill %d -60 -2 %d %d 1 minecraft:stone' % (x - 16, x - 1, -61 + height), 0.2)
    mc('/fill %d %d 2 %d %d 2 minecraft:stone' % (x - 16, -60 + height, x - 1, -60 + height + 8), 0.2)
    me('lucio', x - 12, 0.7, -90, 0); tp(x - 12, -60 + height, 0.7, -90, 0); c('look -90 0'); mc('/gamerule minecraft:natural_health_regeneration false'); sleep(1.5); c('heard')
    # walk onto the wall: inside its reach
    mc('/tp @s %s %d 1.2 -90 0' % (x - 12, -60 + height), 0.1); sleep(0.2)
    s0 = st(); c('key jump down'); t0 = time.time(); top = s0['y']; xs = []
    while time.time() - t0 < 4:
        s = st(); top = max(top, s['y']); xs.append(round(s['x'] - x, 1))
        if s['y'] < -60 + height - 3: c('key jump up')
        if s.get('onGround') and s['y'] < -59 and time.time() - t0 > 0.5: break
        sleep(0.03)
    c('key jump up'); sleep(0.5); s1 = st(); h = heard('')
    log('  wall %d m up: y %.2f, top %.2f, x from %s to %s, y end %.2f, health %.1f -> %.1f, ride %s, pain %s' % (height, s0['y'], top, xs[0], xs[-1], s1['y'], s0['health'], s1['health'], [n for n in h if 'wall' in n][:2], [n for n in h if 'pain' in n]))
for h in (9, 9, 15, 15):
    run(h)
clear(); log('DONE')

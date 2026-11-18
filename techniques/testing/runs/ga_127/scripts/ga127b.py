NOWORLD=True
CASES=['lava','lava_deep','water_long']
# #127: Mercy's Guardian Angel to a NoAI villager 12 m ahead, from each start. py -I oc.py ga127.py out.txt
exec(open(r"C:\Users\ultra\AppData\Local\Temp\claude\14452f9945c49089\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad\lib.py", encoding="utf8").read())
import os
TREE = open(r"E:\claude\mods\overcraft_ga\run\ga_launch_tree.txt").read().strip() if os.path.exists(r"E:\claude\mods\overcraft_ga\run\ga_launch_tree.txt") else '?'
log('TREE', TREE, time.strftime('%H:%M:%S'))
if not globals().get('NOWORLD'):
    c('close'); sleep(1.5); c('close'); sleep(1); print(c('newworld ga127'))
    for i in range(60):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    sleep(1); c('close'); sleep(1)
setup(); mc('/time set noon')
X = [int(time.time()) % 100 * 200 + 20000]
CASES = globals().get('CASES') or ['plain', 'air', 'water', 'water_deep', 'lava', 'ladder', 'ladder_high', 'vine', 'scaffolding', 'bubble_up', 'bubble_down']
log('### GA (ability_1 tap) at a NoAI villager 12 m ahead (z 12.5), her at z 0.5. Reads: z/y about every 0.06 s for 1.4 s')
for rep in range(2):
    for where in CASES:
        X[0] += 60; x = X[0]; pick('mercy'); mc('/gamemode survival @s'); mc('/overcraft reset')
        mc('/tp @s %d -55 0' % x, 0.1); sleep(0.7)
        mc('/fill %d -62 -8 %d -50 24 minecraft:air' % (x - 8, x + 8), 0.15); mc('/fill %d -63 -8 %d -61 24 minecraft:stone' % (x - 8, x + 8), 0.15)
        y = -60
        if where == 'water': mc('/setblock %d -60 0 minecraft:water' % x, 0.05)
        if where == 'water_deep':
            mc('/fill %d -62 -1 %d -59 4 minecraft:water' % (x - 1, x + 1), 0.05); y = -61.5
        if where.startswith('lava'): mc('/effect give @s minecraft:fire_resistance 60 0 true', 0.05)
        if where == 'lava': mc('/setblock %d -60 0 minecraft:lava' % x, 0.05)
        if where == 'lava_deep': mc('/fill %d -62 -1 %d -61 14 minecraft:lava' % (x - 1, x + 1), 0.05); y = -62
        if where == 'water_long': mc('/fill %d -62 -1 %d -61 14 minecraft:water' % (x - 1, x + 1), 0.05); y = -62
        if where == 'ladder': mc('/setblock %d -60 -1 minecraft:stone' % x, 0.02); mc('/setblock %d -60 0 minecraft:ladder[facing=south]' % x, 0.05)
        if where == 'ladder_high':
            mc('/fill %d -60 -1 %d -55 -1 minecraft:stone' % (x, x), 0.02); mc('/fill %d -60 0 %d -55 0 minecraft:ladder[facing=south]' % (x, x), 0.05); y = -57
        if where == 'vine': mc('/setblock %d -60 -1 minecraft:stone' % x, 0.02); mc('/setblock %d -60 0 minecraft:vine[north=true]' % x, 0.05)
        if where == 'scaffolding': mc('/fill %d -60 0 %d -58 0 minecraft:scaffolding[distance=0]' % (x, x), 0.05); y = -59
        if where.startswith('bubble'):
            mc('/fill %d -60 -1 %d -57 1 minecraft:glass' % (x - 1, x + 1), 0.02); mc('/fill %d -60 1 %d -57 1 minecraft:air' % (x - 1, x + 1), 0.02)
            mc('/setblock %d -61 0 minecraft:%s' % (x, 'soul_sand' if where == 'bubble_up' else 'magma_block'), 0.02)
            mc('/fill %d -60 0 %d -57 0 minecraft:bubble_column[drag=%s]' % (x, x, 'false' if where == 'bubble_up' else 'true'), 0.05); y = -59
        if where == 'air': y = -55
        mc('/summon minecraft:villager %d.5 -60 12.5 {NoAI:1b,Tags:["vl"]}' % x, 0.2)
        tp(x + 0.5, y, 0.5, 0, 0); c('look 0 -3'); sleep(1.0 if where != 'air' else 0.05)
        s0 = st(); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); t0 = time.time(); ser = []
        while time.time() - t0 < 1.4:
            s = st(); ser.append('%.1f/%.1f' % (s['z'], s['y'])); sleep(0.06)
        s1 = st()
        log('  %s: start z %.2f y %.2f -> end z %.2f y %.2f, hp %s, cd1 %s | %s' % (where, s0['z'], s0['y'], s1['z'], s1['y'], s1.get('health'), s1.get('ability1Cooldown'), ' '.join(ser)))
        mc('/kill @e[tag=vl]', 0.1)
log('DONE')


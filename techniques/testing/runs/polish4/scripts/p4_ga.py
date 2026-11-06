# Mercy's Guardian Angel from water, a ladder, vines, to a NoAI villager 12 m ahead, on whatever build is running. No bots: works on main. TREE typed by the prefix.
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_lib.py", encoding="utf8").read())
log('TREE', globals().get('TREE_TEXT'), time.strftime('%H:%M:%S'))
c('close'); sleep(1.5); c('close'); sleep(1); print(c('newworld ga_w'))
for i in range(60):
    sleep(2)
    if st().get('screen') == 'HeroSelectScreen': break
sleep(1); c('close'); sleep(1)
setup(); mc('/time set noon')
X = 5000
log('### GA at a NoAI villager 12 m ahead at her level: z read every 0.1 s or so from the key. A hit on the villager is not asked')
for where in ('plain', 'water', 'ladder', 'vine', 'plain', 'water', 'ladder', 'vine'):
    X += 60; x = X; pick('mercy'); mc('/gamemode survival @s')
    mc('/tp @s %d -55 0' % x, 0.1); sleep(0.7); mc('/fill %d -60 -8 %d -50 24 minecraft:air' % (x - 8, x + 8), 0.15); mc('/fill %d -61 -8 %d -61 24 minecraft:stone' % (x - 8, x + 8), 0.15)
    if where == 'water': mc('/setblock %d -60 0 minecraft:water' % x, 0.05)
    if where == 'ladder': mc('/setblock %d -60 -1 minecraft:stone' % x, 0.02); mc('/setblock %d -60 0 minecraft:ladder[facing=south]' % x, 0.05)
    if where == 'vine': mc('/setblock %d -60 -1 minecraft:stone' % x, 0.02); mc('/setblock %d -60 0 minecraft:vine[north=true]' % x, 0.05)
    mc('/summon minecraft:villager %d.5 -60 12.5 {NoAI:1b,Tags:["vl"]}' % x, 0.2); tp(x + 0.5, -60, 0.5, 0, 0); c('look 0 -3'); sleep(1.0)
    s0 = st(); c('key ability_1 down'); sleep(0.05); c('key ability_1 up'); t0 = time.time(); ser = []
    while time.time() - t0 < 1.2:
        s = st(); ser.append('%.1f' % s['z']); sleep(0.06)
    log('  %s: z %.2f, reads %s, cd1 %s' % (where, s0['z'], ' '.join(ser), st().get('ability1Cooldown'))); mc('/kill @e[tag=vl]', 0.1)
log('DONE')

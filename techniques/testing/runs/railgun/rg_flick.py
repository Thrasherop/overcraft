SRC = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/rg_table.py", encoding="utf8").read()
A = ARGS; ARGS = ['none']; exec(SRC); NAMES = A or None
log(c('focus'))
sleep(1); log('fps', st().get('fps'))
guard(); mc('/overcraft reset', 0.08); home(); sleep(0.2)
mc('/kill @e[tag=rgx]', 0.2)
mc('/summon minecraft:zombie 12.5 -60 0.5 {NoAI:1b,Tags:["rgx"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}', 0.3)
X = '@e[tag=rgx,sort=nearest,limit=1]'; Z = '@e[tag=rgz,sort=nearest,limit=1]'
for i in range(8):
    home(); sleep(0.3); c('presstrace energy 50'); sleep(0.1); h0 = hp(X); z0 = hp(Z); s0 = st()
    c('presstrace start'); c('look -90 0'); c('key primary_fire down'); sleep(0.05); c('key primary_fire up'); sleep(0.5)
    tr = c('presstrace').get('trace', ''); h1 = hp(X); z1 = hp(Z); s = st()
    log('flick', i, 'side zombie took', round(h0 - h1, 2), 'ahead zombie took', round(z0 - z1, 2), 'meter before', s0.get('meter'), 'after', s.get('meter'), 'fps', s.get('fps'), 'yaw', s.get('yaw'), '|', tr[:150]); sleep(0.4)
mc('/kill @e[tag=rgx]', 0.2)
log('### done')

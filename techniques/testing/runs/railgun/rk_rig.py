# Real-key pass: target ahead, energy, trace armed. The presses themselves come from the real mouse and keys.
mode = ARGS[0] if ARGS else 'arm'
if mode == 'rig':
    mc('/fill -20 -61 -20 20 -61 40 minecraft:stone', 0.2); mc('/fill -20 -60 -20 20 -50 40 minecraft:air', 0.2)
    mc('/kill @e[type=minecraft:zombie]', 0.2)
    mc('/summon minecraft:zombie 0.5 -60 12.5 {NoAI:1b,Tags:["rgz"],Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}],active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}', 0.3)
    mc('/tp @s 0.5 -60 0.5 0 0', 0.2); c('look 0 0')
if mode in ('rig', 'arm'):
    log(c('presstrace energy %s' % (ARGS[1] if len(ARGS) > 1 else 50)))
    log(c('presstrace start'))
if mode == 'read':
    log(c('presstrace'))
    log('zombie', hp('@e[type=minecraft:zombie,limit=1]'))
s = st()
log({k: s.get(k) for k in ('screen', 'hero', 'heroMode', 'health', 'fps', 'anim')})

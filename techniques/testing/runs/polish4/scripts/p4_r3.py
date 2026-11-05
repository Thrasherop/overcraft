D = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
exec(open(D + "p4_lib.py", encoding="utf8").read())
log('=== Rally by her own keys on real zombies, fix rally off and on, tree a20c352. Keys through DevServer, not a mouse.')
def ground(x0, z0, r=25):
    mc('/fill %d -60 %d %d -50 %d minecraft:air' % (x0 - r, z0 - r, x0 + r, z0 + r), 0.2); mc('/fill %d -61 %d %d -61 %d minecraft:grass_block' % (x0 - r, z0 - r, x0 + r, z0 + r), 0.2)
xx=6000
for fix in ('off','on','off','on','off','on'):
    xx+=100; x=xx
    log(bot('fix rally ' + fix)); pick('brigitte'); mc('/gamemode survival @s'); mc('/tp @s %d -60 0 0 0' % x); sleep(1.0); ground(x, 0); mc('/time set midnight'); mc('/kill @e[type=!minecraft:player]'); tp(x + 0.5, -60, 0.5, 0, 0)
    for tag, dz in (('a', 2.2), ('b', 5.5), ('c', -3.5)):
        mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:20f,equipment:{head:{id:"minecraft:iron_helmet",count:1}},active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (x + 0.5, 0.5 + dz, tag), 0.1)
    sleep(0.3); mc('/giveult'); press('ultimate', 0.15); sleep(1.2); c('key primary_fire down'); sleep(0.5)
    press('secondary_fire', 0.15); sleep(0.05); a=bot('asleep'); sleep(1.0); c('key primary_fire up')
    log('fix rally',fix,'a b c',[hp('@e[tag=%s,limit=1]' % t) for t in 'abc'],'stunned right after',a.get('asleep'))
bot('fix rally on')

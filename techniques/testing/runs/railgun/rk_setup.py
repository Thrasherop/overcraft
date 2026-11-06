# Real-key pass of fix/railgun-crisp: a fresh world, stops at the select screen so the hero is picked with the mouse.
WORLD = 'rk%d' % (int(time.time()) % 100000)
for i in range(40):
    s = st()
    if s.get('screen') in ('TitleScreen', 'AccessibilityOnboardingScreen') or i > 20: break
    sleep(2)
log('before', s.get('screen'))
log(c('newworld ' + WORLD))
for i in range(60):
    sleep(2)
    s = st()
    if s.get('screen') == 'HeroSelectScreen': break
log('screen', s.get('screen'), 'hero', s.get('hero'))
for k in ('/difficulty normal', '/gamerule minecraft:spawn_monsters false', '/time set noon', '/weather clear',
          '/kill @e[type=minecraft:slime]', '/gamemode survival @s', '/gamerule send_command_feedback true'):
    mc(k, 0.2)
log(c('window 1180 664'))
log('done', st().get('screen'))

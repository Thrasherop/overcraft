# The pick on a fresh client: frames around it, with and without the art work. Run first in a fresh launch. Frames marked from `hero sojourn`.
WORLD = 'rp%d' % (int(time.time()) % 100000)
for i in range(30):
    if st().get('screen') in ('TitleScreen', 'AccessibilityOnboardingScreen'): break
    sleep(2)
c('close'); sleep(1); log(c('newworld ' + WORLD))
for i in range(60):
    sleep(2)
    if st().get('screen') == 'HeroSelectScreen': break
sleep(1); c('close'); sleep(1)
for k in ('/difficulty normal','/gamerule minecraft:spawn_monsters false','/time set midnight','/gamemode survival @s','/spawnpoint @s 0 -60 0'):
    mc(k, 0.15)
c('window 1280 720'); mc('/tp @s 0.5 -60 0.5 0 0', 0.2); sleep(6)
log('=== TREE', open(r"E:\claude\mods\overcraft_railgun\run\rg_tree.txt").read().strip(), 'hero before', st().get('hero'), 'fps', st().get('fps'))
n0 = len(lines()); c('presstrace frames on'); c('presstrace start'); c('presstrace sample 1500'); c('hero sojourn'); sleep(0.8); c('close'); c('heromode on'); sleep(2.4)
tr = c('presstrace').get('trace', ''); c('presstrace frames off')
F = [float(p.split(' ')[0]) for p in tr.split(' | ') if p and p.split(' ')[1:] in (['f'], ['frame'])]
log('PICK frames over 40 ms (at ms, length):', [(round(a), round(b - a)) for a, b in zip(F, F[1:]) if b - a >= 40], 'total frames', len(F), 'hero', st().get('hero'))
sleep(0.5); ln = lines()[n0:]; j = [i for i, l in enumerate(ln) if 'SAMPLE' in l]
if j: log('\n'.join(ln[j[0]:j[0] + 32]))
sleep(1); log('OWNART', c('presstrace art') if os.environ.get('RG_ART') else 'n/a')
log('### done')

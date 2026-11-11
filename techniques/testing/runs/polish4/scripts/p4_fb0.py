log('TREE: base e5e5f66, detached worktree overcraft_p4base, fresh run dir, unmodified')
print(st().get('screen')); c('close'); sleep(1.5); c('close'); sleep(1); log(c('newworld p4f'))
for i in range(40):
    sleep(2)
    if st().get('screen') == 'HeroSelectScreen': break
sleep(1); c('close'); sleep(1); log(st().get('screen'), st().get('health'))

D = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
LABEL = 'tree: see TREE line'
import os
src = open(D + "p4_t119.py", encoding="utf8").read()
head, tail = src.split("setup(); mc('/gamemode survival @s')\n")
exec(head)
log('TREE (git at launch):', open(r'E:/claude/mods/overcraft_polish4/run/p4_launch_tree.txt').read().strip(), time.strftime('%H:%M:%S'))
exec(open(D + "p4_t119b.py", encoding="utf8").read())
CASES['stun'] = stun_case
c('close'); sleep(1)
for i in range(40):
    sleep(2); s = st()
    if s.get('screen') == 'HeroSelectScreen': break
sleep(1); c('close'); sleep(1)
setup(); mc('/gamemode survival @s')
for fix in ('off', 'on'):
    log('################ bot fix 119', fix, bot('fix 119 ' + fix))
    TAG = fix
    for n in ('sleep', 'stun', 'hack', 'grenade', 'numbed', 'claws', 'strike', 'decel', 'weaken', 'accretion', 'flux'):
        for who in ALL:
            try:
                CASES[n](who)
            except Exception as e:
                log('EXC', n, who, repr(e))
            if st().get('health', 1) <= 0:
                log('DEAD'); c('respawn'); sleep(1.5)
    log('################ prevented, fix', fix)
    try:
        prevent_all()
    except Exception as e:
        log('EXC prevent', repr(e))
    log('################ late viewer, fix', fix)
    clear()
    try:
        late_viewer()
    except Exception as e:
        log('EXC late', repr(e))
clear()
bot('fix 119 on')
log('DONE')

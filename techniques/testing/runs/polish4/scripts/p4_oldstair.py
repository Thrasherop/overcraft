XSTART = 20000
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_third.py", encoding="utf8").read().replace("overcraft_polish4/run/p4_launch_tree", "overcraft_p4old/run/p4_launch_tree")
ONLY = ('none',); ROUNDS = 0
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read().replace("setup()", "pass") if False else "")
c('close'); sleep(1.5); c('close'); sleep(1); print(c('newworld oldstair'))
for i in range(60):
    sleep(2)
    if st().get('screen') == 'HeroSelectScreen': break
sleep(1); c('close'); sleep(1)
exec(src.replace("clear(); log('DONE')\n", ""))
log('### Whiplash on one stair, one slab, on the commit before intoAWall (a84b12b)')
def one(block, z=3):
    def b(x): mc('/fill %d -60 %d %d -60 %d minecraft:%s' % (x - 4, z, x + 4, z, block), 0.2)
    return b
hero_run('one stair + Whiplash', build=one('oak_stairs[facing=south]'), perk=True, n=2); hero_run('one slab + Whiplash', build=one('oak_slab'), perk=True, n=2); hero_run('one slab, no perk', build=one('oak_slab'), n=1); husk_run('one stair + Whiplash', build=one('oak_stairs[facing=south]', 4), perk=True, n=1); husk_run('one slab + Whiplash', build=one('oak_slab', 4), perk=True, n=1)
clear(); log('DONE')

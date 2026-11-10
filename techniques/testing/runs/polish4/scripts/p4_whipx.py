# Whip's pair in game. Each variant: lift mob mc hero. TEMP build reading E:/claude/mods/oc_whip_test.txt on each hit.
XSTART = globals().get('XSTART', 20000)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_third.py", encoding="utf8").read()
ONLY = ('none',)
exec(src.replace("clear(); log('DONE')\n", ""))
if 'error' in bot('cc me'):
    pass
for v in globals().get('VARIANTS', ("2 0.25 0 0", "3 0.25 0 0", "3 0.05 0 0", "3 0.1 0 0", "3 0.15 0 0", "3 0.2 0 0", "3 0.25 0.25 0")):
    open(r"E:/claude/mods/oc_whip_test.txt", "w").write(v)
    log('### lift mob mc hero =', v)
    hero_run('flat', n=2); hero_run('flat', 'off', n=2); husk_run('flat', n=2)
clear(); log('DONE')

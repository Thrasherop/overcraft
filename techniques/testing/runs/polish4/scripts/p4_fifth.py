# Final play of the whip and the rest. ONLY: whip walls slopes overlap newground creeper second2 (p4_third's). Harness keys, bots server-side.
XSTART = globals().get('XSTART', 100000)
ONLY_F = globals().get('ONLY', None)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_third.py", encoding="utf8").read()
ROUNDS = 0
exec(src.replace("clear(); log('DONE')\n", ""))
def wantf(k): return ONLY_F is None or k in ONLY_F
if wantf('newground'):
    log('### Whiplash on one stair, one slab; Minecraft mode at the first stair; water; ladder')
    def one(block):
        def b(x): mc('/fill %d -60 3 %d -60 3 minecraft:%s' % (x - 4, x + 4, block), 0.2)
        return b
    hero_run('one stair at 2.2 m + Whiplash', build=one('oak_stairs[facing=south]'), perk=True); hero_run('one slab at 2.2 m + Whiplash', build=one('oak_slab'), perk=True); hero_run('one slab, no perk', build=one('oak_slab'), n=1); husk_run('one stair at 3.2 m + Whiplash', build=one('oak_stairs[facing=south]'), perk=True, n=1, zz=-1.5) if False else None
    hero_run('stairs at 2.2 m', 'off', build=stairs_at(3)); hero_run('stairs at 6.2 m', 'off', build=stairs_at(7), n=1)
    def water(x):
        mc('/fill %d -61 -8 %d -58 16 minecraft:water' % (x - 4, x + 4), 0.2); mc('/fill %d -62 -8 %d -62 16 minecraft:stone' % (x - 4, x + 4), 0.2)
    hero_run('in water, standing on the bottom', build=water, y=-62 + 1, n=2); hero_run('in water', 'off', build=water, y=-61, n=1); husk_run('in water', build=water, y=-61, n=1)
    def ladder(x):
        mc('/fill %d -60 1 %d -56 1 minecraft:stone' % (x + 1, x + 1), 0.1); mc('/fill %d -60 1 %d -56 1 minecraft:ladder[facing=west]' % (x, x), 0.1)
    hero_run('on a ladder (feet in it)', build=ladder, n=2, zz=1.5); hero_run('on a ladder', 'off', build=ladder, n=1, zz=1.5)
if wantf('creeper'):
    log('### a hacked creeper: at 2.2 m from a bot, speed 0; reads every 0.3 s or so: alive, its target, the bot\'s health. Control and EMP, 3 each')
    for emp in (False, True) * 3:
        mc('/gamerule minecraft:mob_griefing false', 0.05); x = spot(); me('mercy', x, -13); s_ = add('som', 'sombra', 'red', x, 6.5, 0); bot('charge %s 9999' % s_); sleep(0.2)
        mc('/summon minecraft:creeper %s -60 8.7 {Tags:["cr"],attributes:[{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.1); t0 = time.time()
        if emp: bot('press %s ultimate' % s_)
        rows_ = []
        while time.time() - t0 < 8:
            ex = mc('/execute if entity @e[tag=cr]', 0.3)[-1:]; alive = bool(ex and 'passed' in ex[0]); sd = bot('side creeper'); rows_.append('%.1f:%s:%s:%s' % (time.time() - t0, 'alive' if alive else 'gone', 'tgt' if sd.get('target') and 'None' not in str(sd.get('target')) else '-', cc(s_).get('health')));
            if not alive: break
            sleep(0.15)
        log('  emp', emp, ' '.join(rows_)); mc('/kill @e[tag=cr]', 0.05); mc('/gamerule minecraft:mob_griefing true', 0.05)
clear(); log('DONE')

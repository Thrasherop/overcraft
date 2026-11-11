XSTART = globals().get('XSTART', 60000)
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
setup(); mc('/gamemode survival @s'); mc('/time set midnight')
def z(tag): return hp('@e[tag=%s,sort=nearest,limit=1]' % tag)
log('--- a dart on a knock-down, aimed at where the bot has stopped; Suzu or not')
for suzu in (False, True):
    x = spot(); me('mercy', x, -14); a = add('ally', 'mercy', 'blue', x, 3.5, 0); k = add('kiri', 'kiriko', 'blue', x - 2, 3.5, -90, 72); g = add('sig', 'sigma', 'red', x + 6, 3.5, 90, 3); n = add('ana', 'ana', 'red', x - 5, 7, -90, 0); bot('press %s ability2' % g); st_ = [0]
    def ev(q):
        if q['asleep'] and st_[0] == 0: st_[0] = 1; st_.append(time.time())
        elif st_[0] == 1 and time.time() - st_[1] > 0.3:
            st_[0] = 2; dx = q['x'] - (x - 5); dz = q['z'] - 7; d = (dx * dx + dz * dz) ** 0.5; bot('look %s %.1f %.1f' % (n, -math.degrees(math.atan2(dx, dz)), math.degrees(math.atan2(0.6, d)))); bot('press %s ability1' % n); st_.append(time.time()); log('   dart thrown, 0.3 s into it, target at', round(q['x'], 2), round(q['z'], 2))
        elif st_[0] == 2 and suzu and time.time() - st_[2] > 0.3:
            st_[0] = 3; bot('press %s ability2' % k)
    r = []; t0 = time.time()
    while time.time() - t0 < 6.5:
        q = cc(a); r.append((q['now'], q['asleep'], q['sleeping'], q['phased'])); ev(q)
    log('  Suzu', suzu, 'held', span(r, 1), 'sleeping', span(r, 2), 'phase', span(r, 3), sh(a, 'health'))
log('--- a creeper 2.2 m from a bot, EMP or not; target logged, its Fuse not readable while swelling: alive or blown, and the bot\'s health')
for emp in (False, True, True):
    mc('/gamerule minecraft:mob_griefing false', 0.05); x = spot(); me('mercy', x, -13); s_ = add('som', 'sombra', 'red', x, 6.5, 0); bot('charge %s 9999' % s_); sleep(0.2)
    if emp: bot('press %s ultimate' % s_); sleep(0.05)
    mc('/summon minecraft:creeper %s -60 8.7 {Tags:["cr"],attributes:[{id:"minecraft:movement_speed",base:0.0}]}' % x, 0.05); t0 = time.time()
    if emp: sleep(0.6); log('   silenced?');
    for i in range(14):
        sleep(0.45); ex = mc('/execute if entity @e[tag=cr,distance=..40]', 0.25)[-1:]; sd = bot('side creeper'); log('   emp %s +%.1f %s | target %s | bot Sombra %s' % (emp, time.time() - t0, ex[0][-18:] if ex else '?', str(sd.get('target'))[:24], sh(s_, 'health')))
        if ex and 'failed' in ex[0]: break
    mc('/kill @e[tag=cr]', 0.1)
clear(); log('DONE')

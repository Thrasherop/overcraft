# Every hit at a target with a zombie behind it, control and phased. Real keys for the shooter, a bot for the target (phased by `bot phase`, Combat.phase itself).
exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_hlp.py", encoding="utf8").read())
WORLD = globals().get('WORLD', 'p4g')
c('close'); sleep(1)
if 'error' in bot('cc me'):
    log(c('newworld ' + WORLD))
    for i in range(40):
        sleep(2)
        if st().get('screen') == 'HeroSelectScreen': break
    sleep(1); c('close'); sleep(1)
setup(); mc('/gamemode survival @s'); mc('/time set midnight'); mc('/gamerule minecraft:natural_health_regeneration false'); c('window 1280 720')
HL = lambda: hp('@e[tag=bh,sort=nearest,limit=1]')
def rig(hero, phased, dist=5.0, perk=None):
    x = spot(); me(hero, x, 0.5, 0, 0); mc('/time set midnight'); mc('/kill @e[type=minecraft:zombie,distance=..30]', 0.1)
    if perk: mc('/overcraft perks @s 3', 0.1)
    t = add('tgt', 'mercy', 'red', x, 0.5 + dist, 180)
    mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["bh"],Health:200f,attributes:[{id:"minecraft:max_health",base:200}],active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (x, 0.5 + dist + 1.6), 0.15)
    return x, t
def aim(pitch=4):
    c('look 0 %s' % pitch); sleep(0.2)
def result(name, phased, t, h0, b0, extra=''):
    sleep(0.7); r = cc(t); log('%-22s phased=%-5s target %.2f -> %.2f | behind %s -> %s | %s' % (name, phased, h0, r['health'], b0, HL(), extra))
def fire(key, t_hold):
    c('key %s down' % key); sleep(t_hold); c('key %s up' % key)
CASES = [
    ('kunai', 'kiriko', lambda: fire('secondary_fire', 0.3), 4),
    ('kirikos-ofuda-na', None, None, 0),
    ('sonic', 'lucio', lambda: fire('secondary_fire', 0.4), 4),
    ('hypersphere', 'sigma', lambda: fire('secondary_fire', 0.4), 4),
    ('railgun', 'sojourn', lambda: fire('secondary_fire', 0.4), 4),
    ('blaster', 'mercy-blaster', None, 4),
    ('thorn', 'lifeweaver', lambda: fire('primary_fire', 0.5), 4),
    ('paw', 'jetpack_cat', lambda: fire('secondary_fire', 0.4), 4),
    ('disruptor', 'sojourn', lambda: fire('ability_2', 0.12), 9),
    ('anadart', 'ana', lambda: fire('secondary_fire', 0.15), 4),
    ('sleepdart', 'ana', lambda: fire('ability_1', 0.12), 4),
    ('accretion', 'sigma', lambda: fire('ability_2', 0.12), 2),
    ('pistol-hitscan', 'sombra', lambda: fire('secondary_fire', 0.4), 4),
    ('mediblaster-hitscan', 'juno', lambda: fire('secondary_fire', 0.2), 4),
    ('whip', 'brigitte', lambda: fire('ability_1', 0.12), 4),
]
for nm, hero, act, pitch in CASES:
    if act is None or (globals().get('ONLYC') and nm not in ONLYC): continue
    for phased in (False, True):
        try:
            x, t = rig(hero, phased); aim(pitch); h0 = cc(t)['health']; b0 = HL(); c('heard')
            if phased: bot('phase %s 3.5' % t)
            act(); sleep(1.3 if nm in ('accretion',) else 0.5); h = heard(''); result(nm, phased, t, h0, b0, 'asleep ' + str(bot('asleep').get('asleep')) + ' | hit sounds ' + str([s for s in h if 'hit' in s][:4]))
        except Exception as e:
            log('EXC', nm, phased, repr(e))
log('--- Mercy blaster: she needs her weapon out: not switched by this script, not tried')
for phased in (False, True):
    # the bash and the melee at reach: target 1.8 m ahead, zombie just behind it
    x, t = rig('brigitte', phased, 1.8); aim(0); h0 = cc(t)['health']; b0 = HL(); c('key primary_fire down'); sleep(0.5); c('heard')
    if phased: bot('phase %s 3.5' % t)
    fire('secondary_fire', 0.12); sleep(0.5); c('key primary_fire up'); result('bash', phased, t, h0, b0, str(heard('bash')[:3]))
    x, t = rig('mercy', phased, 1.6); aim(0); h0 = cc(t)['health']; b0 = HL(); c('heard')
    if phased: bot('phase %s 3.5' % t)
    fire('quick_melee', 0.12); sleep(0.3); result('melee', phased, t, h0, b0, str(heard('melee')[:3]))
    x = spot(); me('mercy', x, 0.5); n = add('cat', 'jetpack_cat', 'red', x, 3.5, 180, 40); bot('charge %s 9999' % n); mc('/summon minecraft:zombie %s -60 1.8 {NoAI:1b,Tags:["bh"],Health:200f,attributes:[{id:"minecraft:max_health",base:200}],active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (x + 2.5), 0.15); b0 = HL()
    if phased: bot('phase me 3.5')
    bot('press %s ultimate' % n); sleep(0.4); inp(n, 1, 0); sleep(0.3); inp(n, 0, 0); rs = rows('me', 1.0, ['asleep', 'carriedBy', 'health']); log('catnapper              phased=%-5s me knocked %s, carried by %s, health %s | beside me zombie %s -> %s, held %s' % (phased, span(rs, 1), rs[-1][2], rs[-1][3], b0, HL(), bot('asleep').get('asleep'))); sleep(4)
log('--- Hyper Strike by a bot Sigma, charge read: target phased or not, then again at the zombie alone')
for phased in (False, True):
    x, t = rig('mercy', phased, 1.6); bot('remove ' + t); BOTS.remove(t); t = add('tgt', 'mercy', 'blue', x, 1.0, 0); g = add('sig', 'sigma', 'red', x + 0.2, -0.5, 0); bot('perk %s SIGMA_HYPER_STRIKE' % g); bot('strike %s' % g); sleep(0.2); mc('/summon minecraft:zombie %s -60 1.2 {NoAI:1b,Tags:["hz"],Health:200f,attributes:[{id:"minecraft:max_health",base:200}],active_effects:[{id:"minecraft:fire_resistance",duration:9999,show_particles:0b}]}' % (x + 0.8), 0.15)
    if phased: bot('phase %s 3' % t)
    z = lambda: hp('@e[tag=hz,sort=nearest,limit=1]'); z0 = z(); inp(g, 0, 16); sleep(0.25); log('  phased %s: first swing: target %s; zombie %s -> %s %s; charge left %s' % (phased, sh(t, 'health', 'asleep', 'gravity'), z0, z(), sh(g, 'strike'), cc(g)['strike'])); sleep(1.5)
    z0 = z(); inp(g, 0, 16); sleep(0.25); log('    second swing: zombie %s -> %s; charge left %s' % (z0, z(), cc(g)['strike'])); sleep(0.8)
clear(); log('DONE')

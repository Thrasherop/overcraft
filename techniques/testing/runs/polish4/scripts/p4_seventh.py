# Fifth round's after. ONLY_S: trace rally ladder. Harness keys; single player, so ping about 0.
XSTART = globals().get('XSTART', 300000)
ONLY_S = globals().get('ONLY_S', None)
src = open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_sixth.py", encoding="utf8").read()
ONLY = ('none',)
exec(src.replace("clear(); log('DONE')\n", ""))
def ws(k): return ONLY_S is None or k in ONLY_S
if ws('trace'):
    log('### an ordinary hit and a phased pass-through, tick by tick, client copy c, server copy s (z). The real client shoots a bot at z 5.5. In single player: on a remote server, add speed x ping')
    SPEEDS = {}
    def tr(kind, hero, key, hold, pitch=-1, n=2):
        for phased in (False, True) * n:
            x = spot(); me(hero, x, 0.5, 0, 0); c('look 0 %s' % pitch); mc('/time set midnight')
            t = add('tgt', 'mercy', 'red', x, 5.5, 180); sleep(0.2)
            if phased: bot('phase %s 1.8' % t)
            bot('shottrace start'); c('key %s down' % key); sleep(hold); c('key %s up' % key); sleep(1.0)
            rows_ = bot('shottrace').get('trace', '').split(' | ')
            # the first shot only: last client z while a server copy lived, and how far client went
            cs = [float(p[1:]) for r in rows_ for p in r.split()[1:] if p.startswith('c')]; ss = [float(p[1:]) for r in rows_ for p in r.split()[1:] if p.startswith('s')]
            log('  %-10s phased %-5s target z 5.5: client last z %s, server last z %s | target %s | ticks: %s' % (kind, phased, max(cs) if cs else None, max(ss) if ss else None, sh(t, 'health'), ' | '.join(rows_[:9])[:300]))
    tr('ana dart', 'ana', 'secondary_fire', 0.1); tr('sleep dart', 'ana', 'ability_1', 0.1); tr('kunai', 'kiriko', 'secondary_fire', 0.15); tr('thorn', 'lifeweaver', 'primary_fire', 0.3); tr('paw', 'jetpack_cat', 'secondary_fire', 0.15); tr('sonic', 'lucio', 'secondary_fire', 0.2)
if ws('rally'):
    log('### Rally: overhealth on a bot ally, none on herself (#125); AbsorptionAmount read 2 s in')
    for i in range(2):
        x = spot(); clear(); pick('brigitte'); mc('/gamemode survival @s'); mc('/overcraft reset'); mc('/time set midnight'); tp(x, -60, 0.5, 0, 0); a = add('ally', 'mercy', 'blue', x + 2, 3.5, 0); sleep(0.2); mc('/giveult'); press('ultimate', 0.15); sleep(2.2)
        me_ = mc('/data get entity @s AbsorptionAmount', 0.2)[-1:]; al = mc('/data get entity @e[name=%s,limit=1] AbsorptionAmount' % a, 0.2)[-1:]; log('  herself %s | ally %s | her hp %s' % (me_[0][-40:] if me_ else None, al[0][-40:] if al else None, st().get('health')))
if ws('ladder'):
    log('### bash off a 9 m ledge onto a ladder on a wall ahead, never touching ground; step off backwards. 7 m drop with 3 safe')
    for bash in (True, True):
        x = spot(r=24); mc('/fill %d -60 -3 %d -52 3 minecraft:stone' % (x - 4, x + 4), 0.2); mc('/fill %d -60 8 %d -48 8 minecraft:stone' % (x - 4, x + 4), 0.1); mc('/fill %d -60 7 %d -50 7 minecraft:ladder[facing=north]' % (x - 4, x + 4), 0.1)
        me('brigitte', x, 2.0, 0, 0); mc('/time set midnight'); tp(x, -51, 2.0, 0, 0); c('look 0 0'); sleep(1.6); c('heard'); h0 = st()['health']; c('key primary_fire down'); sleep(0.5); c('key secondary_fire down'); sleep(0.1); c('key secondary_fire up'); sleep(0.6); c('key primary_fire up')
        s0 = st(); c('look 180 0'); c('key forward down'); sleep(0.5); c('key forward up'); t0 = time.time()
        while time.time() - t0 < 3 and not st().get('onGround'): sleep(0.04)
        sleep(0.4); s1 = st(); log('  on the ladder at y %.2f z %.2f ground %s; end y %.2f z %.2f, health %.1f -> %.1f, pain %s' % (s0['y'], s0['z'], s0.get('onGround'), s1['y'], s1['z'], h0, s1['health'], pains()))
clear(); log('DONE')

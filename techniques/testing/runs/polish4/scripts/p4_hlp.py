# Helpers for the critique's runs. Bots are server-side players; keys are KeyMapping.set; neither is a person.
import math
SP = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
exec(open(SP + "p4_lib.py", encoding="utf8").read())
log('TREE (git at launch; the client itself can\'t say):', open(r"E:/claude/mods/overcraft_polish4/run/p4_launch_tree.txt").read().strip() if os.path.exists(r"E:/claude/mods/overcraft_polish4/run/p4_launch_tree.txt") else '?')
log('TIME', time.strftime('%H:%M:%S'))
XX = [globals().get('XSTART', 10000)]
NN = [int(time.time()) % 10000]
BOTS = []
def name(b):
    NN[0] += 1; return '%s%d' % (b, NN[0])
def add(b, hero, team, x, z, yaw, pitch=0, y=-60, wait=0.25):
    n = name(b); r = bot('add %s %s %s %s %s %s %s' % (n, hero, team, x, y, z, yaw))
    if 'error' in str(r): log('ADDERR', r)
    mc('/gamemode survival ' + n, 0.12); bot('look %s %s %s' % (n, yaw, pitch)); BOTS.append(n); sleep(wait); return n
def clear():
    for n in BOTS: bot('remove ' + n)
    BOTS.clear()
def spot(block='stone', r=15):
    XX[0] += 60; x = XX[0]
    mc('/tp @s %d -55 0' % x, 0.1); sleep(0.7)
    mc('/fill %d -60 %d %d -50 %d minecraft:air' % (x - r, -r, x + r, r), 0.15); mc('/fill %d -61 %d %d -61 %d minecraft:%s' % (x - r, -r, x + r, r, block), 0.15)
    return x + 0.5
def me(hero, x, z=0.5, yaw=0, pitch=0):
    clear(); pick(hero); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(x, -60, z, yaw, pitch)
def cc(n='me'): return bot('cc ' + n)
def sh(n, *keys):
    r = cc(n); return ' '.join('%s=%s' % (k, round(r[k], 3) if isinstance(r.get(k), float) else r.get(k)) for k in keys)
def heard(text=''):
    h = c('heard').get('heard', '')
    parts = [p.strip() for p in h.split(' | ')]
    return [p.split(' ->')[0] for p in parts if text in p]
def rows(n, secs, keys, every=None):
    out = []; t0 = time.time()
    while time.time() - t0 < secs:
        r = cc(n); out.append(tuple([r['now']] + [r.get(k) for k in keys]))
        if every: every(r)
    return out
def span(rs, i):
    on = [q[0] for q in rs if q[i]]
    return (on[0], on[-1], on[-1] - on[0] + 1) if on else None

"""Tick-by-tick trace of a ride (by its number in rides.py's table) with a few ticks before and after.
Usage: py -I trace.py <log.json> <ride#> [before] [after] [every]"""
import json
import math
import sys

d = json.load(open(sys.argv[1]))
ticks = d['ticks']
want = int(sys.argv[2])
before = int(sys.argv[3]) if len(sys.argv) > 3 else 5
after = int(sys.argv[4]) if len(sys.argv) > 4 else 8
every = int(sys.argv[5]) if len(sys.argv) > 5 else 1
k = 0
for i in range(len(ticks)):
    if ticks[i]['wall'] and (i == 0 or not ticks[i - 1]['wall']):
        k += 1
        if k == want:
            a = i
            break
b = a
while b + 1 < len(ticks) and ticks[b + 1]['wall']:
    b += 1
t0 = ticks[a]['t']
print('ride %d: seg %d, ticks %d..%d (%.2f s)' % (want, ticks[a]['seg'], a, b, ticks[b]['t'] - t0))
print('  dt    W G  pos(x y z)                 h     vy    vel(x z)       in(x z) J C  face(x y z)        N(x z)      D')
for i in range(a - before, b + after + 1):
    x = ticks[i]
    if a <= i <= b and (i - a) % every and i != b:
        continue
    v = x['vel']
    print('%6.3f %s %s (%7.2f %6.2f %7.2f) %5.2f %6.2f (%6.2f %6.2f) %4.1f %4.1f %s %s (%5.2f %5.2f %5.2f) (%5.2f %5.2f) %4.2f'
          % (x['t'] - t0, 'W' if x['wall'] else '.', 'G' if x['ground'] else '.', x['pos'][0], x['pos'][1], x['pos'][2],
             math.hypot(v[0], v[2]), v[1], v[0], v[2], x['inp'][0], x['inp'][2], 'J' if x['jump'] else '-',
             'C' if x['crouch'] else '-', x['face'][0], x['face'][1], x['face'][2], x['n'][0], x['n'][2], x['d']))

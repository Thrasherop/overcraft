"""Direction at attach, back held on the wall, gaps between rides, leaving with jump held.
Usage: py -I check2.py <log.json>"""
import json
import math
import sys

d = json.load(open(sys.argv[1]))
T = d['ticks']


def h(v):
    return math.hypot(v[0], v[2])


def unit(v):
    n = math.hypot(v[0], v[2])
    return (v[0] / n, v[2] / n) if n > 1e-6 else (0.0, 0.0)


def yaw_world(face, inp):
    """the move input turned into the world by the facing (x right? z forward)"""
    fx, fz = unit(face)
    rx, rz = -fz, fx  # one of the two sides; the sign is checked against data below
    return (fx * inp[2] + rx * inp[0], fz * inp[2] + rz * inp[0])


rides = []
i = 0
while i < len(T):
    if T[i]['wall'] and (i == 0 or not T[i - 1]['wall']):
        j = i
        while j + 1 < len(T) and T[j + 1]['wall']:
            j += 1
        rides.append((i, j))
        i = j + 1
    else:
        i += 1

print('== the way he rode vs his run before, his look and his input (rides 0.3 s or longer)')
print('  ride seg  runAlong lookAlong  inputAlong  -> rideWay   (along = dot with the final ride direction)')
agree = {'run': 0, 'look': 0, 'n': 0}
for k, (a, b) in enumerate(rides):
    if T[b]['t'] - T[a]['t'] < 0.3:
        continue
    way = unit(T[min(b, a + 25)]['vel'])
    pre = T[a - 1]
    run = unit(pre['vel'])
    look = unit(pre['face'])
    ra = run[0] * way[0] + run[1] * way[1]
    la = look[0] * way[0] + look[1] * way[1]
    agree['n'] += 1
    agree['run'] += ra > 0
    agree['look'] += la > 0
    if abs(ra) < 0.5 or la < 0:
        print('  %4d %3d  %6.2f    %6.2f   in %4.1f,%4.1f  h_in %.2f' % (k + 1, T[a]['seg'], ra, la, pre['inp'][0], pre['inp'][2],
                                                                   h(pre['vel'])))
print('  rides', agree['n'], 'ran the way of their run before:', agree['run'], ' the way of their look:', agree['look'])

print('\n== ticks on the wall with back held (input z < 0), by ride: speed along the original way')
for k, (a, b) in enumerate(rides):
    back = [x for x in T[a:b + 1] if x['inp'][2] < -0.5]
    side = [x for x in T[a:b + 1] if abs(x['inp'][0]) > 0.5]
    if back or side:
        way = unit(T[a + min(5, b - a)]['vel'])
        alongs = [x['vel'][0] * way[0] + x['vel'][2] * way[1] for x in T[a:b + 1]]
        print('  ride %d seg %d: %d back ticks, %d side ticks; speed along the start way: start %.2f min %.2f end %.2f'
              % (k + 1, T[a]['seg'], len(back), len(side), alongs[0], min(alongs), alongs[-1]))

print('\n== gaps from leaving one wall to the next ride (s), shortest 15')
gaps = []
for (a1, b1), (a2, b2) in zip(rides, rides[1:]):
    gaps.append((T[a2]['t'] - T[b1]['t'], rides.index((a2, b2)) + 1, h(T[a2]['vel'])))
gaps.sort()
print(' ', ', '.join('%.2f (ride %d)' % (g, r) for g, r, _ in gaps[:15]))

print('\n== off the end with jump held: what happened in the next 0.4 s')
for k, (a, b) in enumerate(rides):
    nxt = T[b + 1:b + 26]
    if not T[b + 1]['jump'] or T[b + 1]['ground'] or T[b + 1]['crouch']:
        continue
    rel = next((x for x in nxt if not x['jump']), None)
    rel_t = rel['t'] - T[b]['t'] if rel else None
    vys = ' '.join('%.1f' % x['vel'][1] for x in nxt[:20:2])
    print('  ride %d seg %d: released after %s s; vy every 2 ticks: %s' % (k + 1, T[a]['seg'], '%.2f' % rel_t if rel else '-', vys))

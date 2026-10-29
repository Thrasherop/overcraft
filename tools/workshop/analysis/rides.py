"""Per-ride table from log.json. Usage: py -I rides.py <log.json> [segment ...]"""
import json
import math
import sys

d = json.load(open(sys.argv[1]))
ticks = d['ticks']
want = {int(a) for a in sys.argv[2:]}


def h(v):
    return math.hypot(v[0], v[2])


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def flat_unit(v):
    n = math.hypot(v[0], v[2])
    return [v[0] / n, 0, v[2] / n] if n > 1e-6 else [0, 0, 0]


rides = []
i = 0
while i < len(ticks):
    if ticks[i]['wall'] and (i == 0 or not ticks[i - 1]['wall']):
        j = i
        while j + 1 < len(ticks) and ticks[j + 1]['wall']:
            j += 1
        rides.append((i, j))
        i = j + 1
    else:
        i += 1

print('seg  #  t_on   dur  | before: h    vy  into  in(x,z)  J | on: h0   h.1  h.25  h.5  h1   hEnd  hMax | vy0  vyMid vyEnd rise | '
      'look/ride lookN pitch | end how      | after(+2): h    vy   out  | song')
for k, (a, b) in enumerate(rides):
    seg = ticks[a]['seg']
    if want and seg not in want:
        continue
    pre = ticks[a - 1] if a > 0 else ticks[a]
    on = ticks[a:b + 1]
    t0 = on[0]['t']
    n = on[len(on) // 2]['n'] if any(x['n'] != [0, 0, 0] for x in on) else on[0]['n']
    nn = [x['n'] for x in on if x['n'] != [0, 0, 0]]
    n = nn[len(nn) // 2] if nn else [0, 0, 0]

    def at(dt):
        for x in on:
            if x['t'] - t0 >= dt - 1e-6:
                return h(x['vel'])
        return float('nan')

    ride_dir = flat_unit(on[len(on) // 2]['vel'])
    look = flat_unit(on[len(on) // 2]['face'])
    look_ride = dot(look, ride_dir)
    look_n = dot(look, n)
    pitch = math.degrees(math.asin(max(-1, min(1, on[len(on) // 2]['face'][1]))))
    into = -dot(flat_unit(pre['vel']), n) if n != [0, 0, 0] else float('nan')
    vys = [x['vel'][1] for x in on]
    rise = on[-1]['pos'][1] - on[0]['pos'][1]
    nxt = ticks[b + 1] if b + 1 < len(ticks) else on[-1]
    aft = ticks[b + 3] if b + 3 < len(ticks) else nxt
    if nxt['ground']:
        how = 'landed'
    elif nxt['crouch'] and not on[-1]['crouch'] or nxt['crouch']:
        how = 'crouch'
    elif not nxt['jump'] or not on[-1]['jump']:
        how = 'jump-up'
    else:
        how = 'end/held' if nxt['d'] > 2 else 'held'
    out = dot(flat_unit(aft['vel']), n) if n != [0, 0, 0] else float('nan')
    song = 'ab1' if on[0]['ab1'] else '-'
    print('%3d %3d %6.2f %4.2f | %5.2f %5.2f %5.2f %4.1f,%4.1f %s | %5.2f %5.2f %5.2f %5.2f %5.2f %5.2f %5.2f | %5.2f %5.2f %5.2f %5.2f | '
          '%5.2f %5.2f %4.0f | %-8s | %5.2f %5.2f %5.2f | %s'
          % (seg, k + 1, t0, on[-1]['t'] - t0, h(pre['vel']), pre['vel'][1], into, pre['inp'][0], pre['inp'][2],
             'J' if pre['jump'] else '-', h(on[0]['vel']), at(0.1), at(0.25), at(0.5), at(1.0), h(on[-1]['vel']),
             max(h(x['vel']) for x in on), vys[0], vys[len(vys) // 2], vys[-1], rise, look_ride, look_n, pitch, how,
             h(aft['vel']), aft['vel'][1], out, song))

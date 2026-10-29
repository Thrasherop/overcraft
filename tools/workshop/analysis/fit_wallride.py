"""Fits from log.json: the vertical curve on the wall, the speed ramp at attach, fast entries, the jump off, and
ground speeds. Usage: py -I fit.py <log.json>"""
import json
import math
import sys

d = json.load(open(sys.argv[1]))
T = d['ticks']


def h(v):
    return math.hypot(v[0], v[2])


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

print('== ab1 (Is Using Ability 1) share by segment, and crossfade times')
segs = sorted({t['seg'] for t in T})
print(' '.join('%d:%.2f' % (s, sum(t['ab1'] for t in T if t['seg'] == s) / max(1, sum(1 for t in T if t['seg'] == s))) for s in segs))
print('crossfades at', [round(e['t'], 1) for e in d['events'] if e['kind'] == 'CROSSFADE'])
print('amp at', [round(e['t'], 1) for e in d['events'] if e['kind'] == 'AMP'], 'ult at',
      [round(e['t'], 1) for e in d['events'] if e['kind'] == 'ULT'])

print('\n== vertical speed on the wall from attach (rides over 1.2 s, incoming vy < 2.7): t -> mean vy (n), mean rise')
long = [(a, b) for a, b in rides if T[b]['t'] - T[a]['t'] > 1.2 and T[a - 1]['vel'][1] < 2.7]
print('rides', [rides.index(r) + 1 for r in long])
for step in range(0, 61, 2):
    tt = step * 0.05
    vys, rises = [], []
    for a, b in long:
        for x in T[a:b + 1]:
            if x['t'] - T[a]['t'] >= tt - 1e-6:
                if x['t'] - T[a]['t'] < tt + 0.03:
                    vys.append(x['vel'][1])
                    rises.append(x['pos'][1] - T[a]['pos'][1])
                break
    if vys:
        print('  %.2f  vy %.2f  (%d)  rise %.2f   [%s]' % (tt, sum(vys) / len(vys), len(vys), sum(rises) / len(rises),
                                                       ' '.join('%.2f' % v for v in vys)))

print('\n== incoming vy above 2.7 (rising into the wall): vy for the first ticks')
for a, b in rides:
    if T[a - 1]['vel'][1] > 3:
        print('  ride %d: in %.2f ->' % (rides.index((a, b)) + 1, T[a - 1]['vel'][1]),
              ' '.join('%.2f' % x['vel'][1] for x in T[a:min(b + 1, a + 30)]))

print('\n== speed ramp at attach (entering at 6.8-6.9, along the wall): h per tick')
for a, b in rides:
    if 6.8 < h(T[a - 1]['vel']) < 6.95 and b - a > 12:
        print('  ride %d:' % (rides.index((a, b)) + 1), ' '.join('%.2f' % h(x['vel']) for x in T[a:a + 12]))

print('\n== entering faster than wall speed: h every 2 ticks')
for a, b in rides:
    if h(T[a]['vel']) > 9.3 and b - a > 6:
        print('  ride %d (%.2f s):' % (rides.index((a, b)) + 1, T[b]['t'] - T[a]['t']),
              ' '.join('%.2f' % h(x['vel']) for x in T[a:b + 1:2]))

print('\n== jump off (jump released on the wall): first air ticks; vy0 = vy at the launch tick, dh = launch h - wall h')
print('  ride  pitchY  lookAlong  in(x,z)  wallH  vy0   dh    out')
for a, b in rides:
    nxt = T[b + 1:b + 6]
    if T[b]['jump'] or T[b + 1]['ground'] or T[b + 1]['crouch']:
        continue
    launch = next((x for x in nxt if x['vel'][1] > 4), None)
    if launch is None:
        continue
    face = T[b]['face']
    wv = T[b]['vel']
    wh = h(wv)
    along = (face[0] * wv[0] + face[2] * wv[2]) / max(1e-6, wh * math.hypot(face[0], face[2]))
    lv = launch['vel']
    n = T[b]['n']
    out = (lv[0] * n[0] + lv[2] * n[2]) if n != [0, 0, 0] else float('nan')
    print('  %4d  %6.2f  %6.2f  %4.1f,%4.1f  %5.2f  %5.2f %5.2f %5.2f' % (rides.index((a, b)) + 1, face[1], along,
          T[b]['inp'][0], T[b]['inp'][2], wh, lv[1], h(lv) - wh, out))

print('\n== on the ground with the move keys held: h by ab1')
for flag in (True, False):
    hs = [h(x['vel']) for x in T if x['ground'] and x['ab1'] == flag and abs(x['inp'][2]) + abs(x['inp'][0]) > 0.5]
    hs.sort()
    if hs:
        print('  ab1 %s: n %d  median %.2f  p90 %.2f  max %.2f' % (flag, len(hs), hs[len(hs) // 2], hs[int(len(hs) * 0.9)], hs[-1]))

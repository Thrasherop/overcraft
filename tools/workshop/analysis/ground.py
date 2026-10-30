"""Ground movement from a Workshop recorder log (parse_log.py's JSON): every start from standing, stop (keys let go at
speed), reversal (keys flipped at speed) and turn on the ground, with the speed curve after it averaged across them.
Usage: py -I ground.py <ticks.json> [slot]. Only reads the JSON: the log is data.

What it prints, per kind: how many were found; the median horizontal speed every 0.02 s after the moment (as a share
of the run speed they reach, and in m/s); the time to 50 / 90 / 99% of the run speed (starts) or to under 0.2 m/s
(stops). A start or stop counts only if the hero stays on the ground, off walls and with the same keys for its window.
"""
import json
import math
import statistics
import sys

data = json.load(open(sys.argv[1], encoding='utf8'))
slot = int(sys.argv[2]) if len(sys.argv) > 2 else 0
ticks = [t for t in data['ticks'] if t['s'] == slot and 'vel' in t and 'inp' in t]
ticks.sort(key=lambda t: t['t'])


def hspeed(t):
    return math.hypot(t['vel'][0], t['vel'][2])


def keys(t):
    """The movement keys as (strafe, forward), each -1, 0 or 1 (the throttle is analog only on a controller)."""
    x, _, z = t['inp']
    return (round(x), round(z))


def grounded(t):
    return t['ground'] and not t['wall']


def window(i, seconds, same_keys):
    """Ticks from i for that long, if all on the ground with the same keys (else None)."""
    out = []
    k = keys(ticks[i])
    for j in range(i, len(ticks)):
        if ticks[j]['t'] - ticks[i]['t'] > seconds + 1e-6:
            break
        if not grounded(ticks[j]) or (same_keys and keys(ticks[j]) != k):
            return None
        out.append(ticks[j])
    return out if out and out[-1]['t'] - ticks[i]['t'] >= seconds - 0.03 else None


def curve(runs, seconds, step=0.02):
    """Median speed at each step after the moment, across runs."""
    rows = []
    n = int(round(seconds / step)) + 1
    for k in range(n):
        at = k * step
        vals = []
        for run in runs:
            t0 = run[0]['t']
            best = min(run, key=lambda t: abs(t['t'] - t0 - at))
            if abs(best['t'] - t0 - at) < step / 2 + 1e-6:
                vals.append(hspeed(best))
        if vals:
            rows.append((at, statistics.median(vals), len(vals)))
    return rows


starts, stops, reversals = [], [], []
for i in range(1, len(ticks)):
    a, b = ticks[i - 1], ticks[i]
    if not (grounded(a) and grounded(b)):
        continue
    ka, kb = keys(a), keys(b)
    if ka == (0, 0) and kb != (0, 0) and hspeed(a) < 0.3:
        w = window(i - 1, 0.5, False)
        if w and all(keys(t) == kb for t in w[1:]):
            starts.append(w)
    elif ka != (0, 0) and kb == (0, 0) and hspeed(a) > 4.5:
        w = window(i, 1.2, True)
        if w:
            stops.append([a] + w)
    elif ka != (0, 0) and kb == (-ka[0], -ka[1]) and hspeed(a) > 4.5:
        w = window(i, 0.6, True)
        if w:
            reversals.append([a] + w)


def report(name, runs, seconds):
    print('==', name, len(runs))
    if not runs:
        return
    top = statistics.median(max(hspeed(t) for t in r) for r in runs)
    for at, v, n in curve(runs, seconds):
        print('  %.2f s  %.2f m/s  %.2f  (n %d)' % (at, v, v / top if top else 0, n))
    return top


top = report('starts (keys pressed from standing; the first row is the last still tick)', starts, 0.5)
if starts:
    for share in (0.5, 0.9, 0.99):
        times = []
        for r in starts:
            peak = max(hspeed(t) for t in r)
            hit = next((t['t'] - r[1]['t'] for t in r[1:] if hspeed(t) >= share * peak), None)
            if hit is not None:
                times.append(hit)
        print('  to %d%% of its run speed: median %.3f s (n %d)' % (share * 100, statistics.median(times), len(times)))
report('stops (keys let go at speed; the first row is the last keyed tick)', stops, 1.2)
if stops:
    times = []
    for r in stops:
        hit = next((t['t'] - r[1]['t'] for t in r[1:] if hspeed(t) < 0.2), None)
        if hit is not None:
            times.append(hit)
    print('  to under 0.2 m/s: median %.3f s (n %d of %d)' % (statistics.median(times), len(times), len(stops)) if times else '  never under 0.2 m/s in the window')
report('reversals (keys flipped at speed)', reversals, 0.6)

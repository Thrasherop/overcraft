"""Pharah's measurements from a Workshop recorder log (2026-11-11 analysis: techniques/heroes/pharah_workshop_2026-11-11.md).
Reads parse_log.py's ticks.csv, Pharah's rows only (an AI bot is also tagged S0 in the owner's logs); a tick is 0.016 s.
Run with py -I.

    py -I pharah_measure.py <ticks.csv> rates <row0> <row1>   per-tick dv/dt (vertical and horizontal) between two rows
    py -I pharah_measure.py <ticks.csv> hover                 the hover's vertical acceleration binned by vertical speed
    py -I pharah_measure.py <ticks.csv> gravity               free fall: dv/dt with no key, no ability, in the air
    py -I pharah_measure.py <ticks.csv> walk                  ground speed from a start / stop of the move keys
    py -I pharah_measure.py <ticks.csv> fuel [drain regen delay jet hold decay decayHover airRegen]
                                                               PharahStats' fuel rules run over her keys (the log has no fuel
                                                               meter): each Jump hold's predicted fuel, against where the log's
                                                               rise stops with Jump still held (the tank empty)
    py -I pharah_measure.py <ticks.csv> hoverfit              with fuel: dv/dt against (4.66 - vy), the approach's rate
    py -I pharah_measure.py <ticks.csv> fit                   the refill's rate / delay / air-or-ground fitted to those empties
"""
import csv
import math
import sys

DT = 0.016


def rows(path):
    out = []
    for r in csv.DictReader(open(path, encoding='utf8')):
        if r['hero'] != 'Pharah':
            continue
        out.append({k: (float(v) if v not in ('', None) and k != 'hero' else v) for k, v in r.items()})
    return out


def on(r, k):
    return r[k] in (1, 1.0, '1', 'True')


def rates(rs, a, b):
    print(' row      t      y     vy  dvy/dt     hs  dhs/dt   ix   iz J F2 1 2 Q')
    for i in range(int(a), int(b) + 1):
        r, p = rs[i], rs[i - 1]
        print('%4d %6.2f %6.2f %6.2f %7.1f %6.2f %7.1f %4.1f %4.1f %s %s %s %s %s' % (
            i, r['t'], r['py'], r['vy'], (r['vy'] - p['vy']) / DT, r['hspeed'], (r['hspeed'] - p['hspeed']) / DT,
            r['ix'], r['iz'], 'J' if on(r, 'jump') else '.', 'R' if on(r, 'fire2') else '.', '1' if on(r, 'ab1') else '.',
            '2' if on(r, 'ab2') else '.', 'Q' if on(r, 'ult') else '.'))


def hover(rs):
    """Jump held this tick and the two before, in the air both ticks, no Shift / E: dv/dt binned by the earlier tick's vy."""
    bins = {}
    for i in range(3, len(rs)):
        r, p = rs[i], rs[i - 1]
        if not all(on(rs[j], 'jump') and on(rs[j], 'air') for j in (i - 2, i - 1, i)):
            continue
        if on(r, 'ab1') or on(p, 'ab1') or on(r, 'ab2') or on(p, 'ab2'):
            continue
        a = (r['vy'] - p['vy']) / DT
        k = math.floor(p['vy'])
        bins.setdefault(k, []).append((a, 'Q' if on(r, 'ult') else ''))
    print('vy bin   n   mean dv/dt   min   max   (Barrage ticks)')
    for k in sorted(bins):
        v = [a for a, _ in bins[k]]
        q = sum(1 for _, u in bins[k] if u)
        print('%3d..%-3d %3d %8.1f %8.1f %6.1f %4d' % (k, k + 1, len(v), sum(v) / len(v), min(v), max(v), q))


def gravity(rs):
    acc = []
    for i in range(2, len(rs)):
        r, p, q = rs[i], rs[i - 1], rs[i - 2]
        if not all(on(x, 'air') and not on(x, 'jump') and not on(x, 'ab1') and not on(x, 'ab2') and not on(x, 'ult')
                   and not on(x, 'fire2') for x in (r, p, q)):
            continue
        acc.append(((r['vy'] - p['vy']) / DT, p['vy'], i))
    acc.sort()
    n = len(acc)
    print('free-fall ticks', n, 'median dv/dt %.2f' % acc[n // 2][0], 'quartiles %.2f %.2f' % (acc[n // 4][0], acc[3 * n // 4][0]))
    fast = [a for a in acc if a[1] < -15]
    print('below -15 m/s:', len(fast), 'lowest vy', min((a[1] for a in acc), default=None))


def walk(rs):
    """Each ground start (input from 0 to nonzero) and stop: horizontal speed for the next 12 ticks."""
    for i in range(1, len(rs) - 12):
        r, p = rs[i], rs[i - 1]
        if not (on(r, 'ground') and on(p, 'ground')):
            continue
        pi, ri = math.hypot(p['ix'], p['iz']), math.hypot(r['ix'], r['iz'])
        if (pi == 0) != (ri == 0) and all(on(rs[j], 'ground') for j in range(i, i + 12)):
            print('%s row %d t %.2f:' % ('start' if pi == 0 else 'stop ', i, r['t']),
                  ' '.join('%.2f' % rs[j]['hspeed'] for j in range(i - 1, i + 12)))


def fuel(rs, drain=50, regen=45, delay=0.3, jet=50, hold=5, decay=30, decay_hover=80, air_regen=0, start=100):
    """Hovering: Jump held, pressed in the air or 0.2 s after a press on the ground, not in a jet (a jet spends none). A jet adds +50 over
    0.85 s. air_regen 1: the refill runs in the air too (any time she isn't hovering). The log's empty: Jump held in the air, no jet, vy under 4.4 and falling faster than 3 m/s^2 for 3 ticks."""
    f, jet_left, over_at, air_at, ground_held = start, 0.0, -1e9, -1e9, 0
    held_was, seen_empty, slow, since, pressed_air = False, False, 0, 0, False
    for i, r in enumerate(rs):
        t = r['t']
        if i and on(r, 'ab1') and not on(rs[i - 1], 'ab1'):
            jet_left = jet
        air = on(r, 'air')
        held = on(r, 'jump')
        # the log: a press on the ground jumps, and the hover's push starts 13 ticks (0.2 s) after the press, in the air by
        # then; a press in the air starts it at once
        if held and not held_was:
            ground_held, pressed_air = 0, i > 0 and on(rs[i - 1], 'air')
        ground_held = ground_held + 1 if held else 0
        hovering = held and (pressed_air or ground_held > 13) and not on(r, 'ab1')
        if on(r, 'ult'):
            f = max(f, 100)
        if jet_left > 0:
            add = min(jet_left, jet / 0.85 * DT)
            jet_left -= add
            f = min(150, f + add)
            if f > 100:
                over_at = t
        if hovering:
            f = max(0, f - (decay_hover if f > 100 else drain) * DT)
        elif f > 100 and t - over_at > hold:
            f = max(100, f - decay * DT)
        if (air and not air_regen) or hovering:
            air_at = t
        elif t - air_at >= delay and f < 100:
            f = min(100, f + regen * DT)
        if held and not held_was:
            print('%6.2f press   fuel %6.1f %s' % (t, f, 'air' if air else 'ground'))
            seen_empty, slow, since = False, 0, 0
        if not held and held_was:
            print('%6.2f release fuel %6.1f  vy %.2f' % (t, f, r['vy']))
        since += 1
        # (a jump's own first 0.2 s isn't the hover: skip 0.25 s after a press)
        if since > 15 and held and air and not on(r, 'ab1') and i and r['vy'] < 4.4 and (r['vy'] - rs[i - 1]['vy']) / DT < -3:
            slow += 1
        else:
            slow = 0
        if slow == 3 and not seen_empty:
            seen_empty = True
            print('%6.2f   log: the rise stops (empty), predicted fuel %.1f, vy %.2f' % (rs[i - 2]['t'], f, r['vy']))
        held_was = held


def simulate(rs, drain=50, regen=45, delay=0.3, jet=50, hold=5, decay=30, decay_hover=80, air_regen=0, start=100,
             jet_spends=0):
    """fuel()'s rules, quietly: the times the tank runs dry while she hovers."""
    f, jet_left, over_at, air_at, ground_held, held_was, pressed_air = start, 0.0, -1e9, -1e9, 0, False, False
    empties = []
    for i, r in enumerate(rs):
        t, air, held = r['t'], on(r, 'air'), on(r, 'jump')
        if i and on(r, 'ab1') and not on(rs[i - 1], 'ab1'):
            jet_left = jet
        if held and not held_was:
            ground_held, pressed_air = 0, i > 0 and on(rs[i - 1], 'air')
        ground_held = ground_held + 1 if held else 0
        hovering = held and (pressed_air or ground_held > 13) and (jet_spends or not on(r, 'ab1'))
        if on(r, 'ult'):
            f = max(f, 100)
        if jet_left > 0:
            add = min(jet_left, jet / 0.85 * DT)
            jet_left -= add
            f = min(150, f + add)
            if f > 100:
                over_at = t
        if hovering and not on(r, 'ult'):
            was = f
            f = max(0, f - (decay_hover if f > 100 else drain) * DT)
            if was > 0 and f == 0:
                empties.append(t)
        elif f > 100 and t - over_at > hold:
            f = max(100, f - decay * DT)
        if (air and not air_regen) or hovering:
            air_at = t
        elif t - air_at >= delay and f < 100:
            f = min(100, f + regen * DT)
        held_was = held
    return empties


# where the log's rise stops with Jump held (the hover's push gone, her +30 % speed with it): read off the traces
OBSERVED_EMPTY = [17.50, 27.08, 48.20]


def fit(rs):
    """A grid over the refill's rate, delay and whether it runs in the air: each against the log's three empties."""
    out = []
    for air_regen in (0, 1):
        for regen in (30, 35, 40, 45, 50, 55, 60, 70, 80, 100):
            for delay in (0.3, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0):
                for jet_spends in (0, 1):
                    e = simulate(rs, regen=regen, delay=delay, air_regen=air_regen, jet_spends=jet_spends)
                    err = 0
                    for o in OBSERVED_EMPTY:
                        err += min([abs(x - o) for x in e] + [3]) ** 2
                    extra = sum(1 for x in e if min(abs(x - o) for o in OBSERVED_EMPTY) > 0.3)
                    out.append((err + extra * 4, err, extra, air_regen, regen, delay, jet_spends, e))
    out.sort()
    print('score  err  extra  air  regen  delay  jetSpends  empties')
    for o in out[:15]:
        print('%5.2f %5.2f %4d %4d %6d %6.2f %6d   %s' % (o[:7] + (' '.join('%.2f' % x for x in o[7]),)))
    for o in out:
        if o[3:7] == (0, 45, 0.3, 0):
            print('ours:', '%5.2f %5.2f %4d' % o[:3], ' '.join('%.2f' % x for x in o[7]))


# the stretches where her tank is empty (Jump held, the push gone), and the jump's first 0.2 s: left out of the hover fit
EMPTY_SPANS = [(17.50, 17.70), (27.08, 29.40), (48.20, 48.60)]


def hover_fit(rs, target=4.66):
    """With fuel: dv/dt against (target - vy), binned by vy; k = dv/dt / (target - vy)."""
    bins = {}
    since = 0
    for i in range(1, len(rs)):
        r, p = rs[i], rs[i - 1]
        since = since + 1 if on(r, 'jump') else 0
        pressed_air = on(r, 'jump') and since == 1 and on(p, 'air')
        if since == 1:
            ok_from = 1 if pressed_air else 14
        if not (on(r, 'jump') and on(r, 'air') and on(p, 'air')) or since <= ok_from:
            continue
        if any(on(x, k) for x in (r, p) for k in ('ab1', 'ab2', 'ult', 'fire2')):
            continue
        if any(a <= r['t'] <= b for a, b in EMPTY_SPANS):
            continue
        a = (r['vy'] - p['vy']) / DT
        k = math.floor(p['vy'])
        bins.setdefault(k, []).append((a, a / (target - p['vy']) if abs(target - p['vy']) > 0.3 else None))
    print('vy bin    n  mean dv/dt   mean k (dv/dt over %.2f - vy)' % target)
    for k in sorted(bins):
        a = [x for x, _ in bins[k]]
        ks = [y for _, y in bins[k] if y is not None]
        print('%3d..%-3d %4d %9.1f %9s' % (k, k + 1, len(a), sum(a) / len(a), '%.2f' % (sum(ks) / len(ks)) if ks else '-'))


def main():
    rs = rows(sys.argv[1])
    cmd = sys.argv[2]
    if cmd == 'rates':
        rates(rs, sys.argv[3], sys.argv[4])
    elif cmd == 'hover':
        hover(rs)
    elif cmd == 'gravity':
        gravity(rs)
    elif cmd == 'walk':
        walk(rs)
    elif cmd == 'hoverfit':
        hover_fit(rs)
    elif cmd == 'fit':
        fit(rs)
    elif cmd == 'fuel':
        fuel(rs, *[float(a) for a in sys.argv[3:]])


if __name__ == '__main__':
    main()

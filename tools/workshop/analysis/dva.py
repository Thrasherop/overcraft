"""D.Va's numbers from a movement-recorder log (ticks.csv / events.csv from parse_log.py --csv), for the comparison in
techniques/heroes/dva_workshop_2026-11-11.md. One subject per csv: if two players share a slot tag, split the log first
(split_subjects.py). Only reads csv files: run with py -I.

    py -I dva.py <dir with ticks.csv and events.csv>

Prints, for D.Va's ticks only: her forms over time, walking (mech, pilot; firing, matrix, missiles), acceleration and
stops, jumps and gravity, each Boosters flight, each Defense Matrix use, each Micro Missiles volley, Self-Destruct /
eject / Call Mech, health changes. Tick convention (techniques/game-data/workshop_recordings.md): a tick's velocity is
after that tick's gravity; compare tops of arcs.
"""
import csv
import math
import os
import statistics as st
import sys

GRAV_GUESS = 17.5


def load(d):
    rows = []
    for r in csv.DictReader(open(os.path.join(d, 'ticks.csv'))):
        if r['hero'] != 'D.Va':
            continue
        o = {}
        for k, v in r.items():
            if v == '' or v is None:
                continue
            try:
                o[k] = float(v)
            except ValueError:
                o[k] = v
        rows.append(o)
    evs = [e for e in csv.DictReader(open(os.path.join(d, 'events.csv')))]
    for e in evs:
        e['t'] = float(e['t'])
    return rows, evs


def spd(r):
    return math.sqrt(r['vx'] ** 2 + r['vy'] ** 2 + r['vz'] ** 2)


def ang(a, b):
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(x * x for x in b))
    if na < 1e-6 or nb < 1e-6:
        return float('nan')
    c = sum(x * y for x, y in zip(a, b)) / na / nb
    return math.degrees(math.acos(max(-1, min(1, c))))


def spans(rows, key, pred=None):
    """[(i0, i1)] index ranges where rows[i][key] is true (and pred)."""
    out, start = [], None
    for i, r in enumerate(rows):
        on = r.get(key, 0) and (pred is None or pred(r))
        if on and start is None:
            start = i
        if not on and start is not None:
            out.append((start, i - 1))
            start = None
    if start is not None:
        out.append((start, len(rows) - 1))
    return out


def med(xs):
    xs = [x for x in xs if x == x]
    return st.median(xs) if xs else float('nan')


def forms(rows):
    print('== forms (alt_form 1 = pilot), max health, ult in use')
    prev = None
    for r in rows:
        k = (r['alt_form'], r['max_health'], r['ult'])
        if k != prev:
            print('  t %.2f  pilot %d  max %d  hp %.1f  ult in use %d  ult %% %d' % (r['t'], r['alt_form'], r['max_health'],
                                                                              r['health'], r['ult'], r['ult_pct']))
            prev = k


def walking(rows):
    print('== steady walking on the ground (input held 0.3 s, no Boosters / missiles / ult)')
    groups = {}
    for i in range(20, len(rows)):
        r = rows[i]
        win = rows[i - 19:i + 1]
        if r['t'] - win[0]['t'] > 0.4:
            continue
        if not all(w['ground'] and math.hypot(w['ix'], w['iz']) > 0.5 and not w['ab1'] and not w['ult']
                   and w['alt_form'] == r['alt_form'] for w in win):
            continue
        if not all(abs(w['ix'] - r['ix']) < 0.01 and abs(w['iz'] - r['iz']) < 0.01 for w in win):
            continue
        body = 'pilot' if r['alt_form'] else 'mech'
        iz, ix = r['iz'], r['ix']
        body += ' ' + ('fwd' if iz > 0.5 and abs(ix) < 0.5 else 'back' if iz < -0.5 and abs(ix) < 0.5 else
                       'side' if abs(iz) < 0.5 else 'diag-fwd' if iz > 0 else 'diag-back')
        what = 'missiles' if r['ab2'] else 'matrix up' if r['fire2'] else 'firing' if r['firing1'] else 'plain'
        if not r['ab2'] and not all(bool(w['fire2']) == bool(r['fire2']) and bool(w['firing1']) == bool(r['firing1'])
                                    for w in win):
            continue
        groups.setdefault((body, what), []).append(r['hspeed'])
    for k in sorted(groups):
        xs = groups[k]
        print('  %-15s %-9s n %4d  median %.3f  p10 %.3f  p90 %.3f' % (k[0], k[1], len(xs), med(xs),
                                                                     sorted(xs)[len(xs) // 10], sorted(xs)[9 * len(xs) // 10]))


def accel(rows):
    print('== starts from standing (ground, plain) and stops (input released)')
    for i in range(1, len(rows) - 40):
        a, b = rows[i - 1], rows[i]
        if not (a['ground'] and b['ground'] and not b['ab1'] and not b['ult'] and not b['fire2'] and not b['firing1']):
            continue
        if a['hspeed'] < 0.05 and math.hypot(a['ix'], a['iz']) < 0.1 and math.hypot(b['ix'], b['iz']) > 0.5:
            seq = [round(rows[j]['hspeed'], 2) for j in range(i, i + 8)]
            reach = next((rows[j]['t'] - b['t'] for j in range(i, i + 40) if rows[j]['hspeed'] >= 0.95 * 5.5), None)
            print('  start t %.2f %s  pilot %d  speeds %s  to 95%%: %s' % (b['t'], 'ground', b['alt_form'], seq,
                                                                      'n/a' if reach is None else '%.3f s' % reach))
        if a['hspeed'] > 5.3 and math.hypot(a['ix'], a['iz']) > 0.5 and math.hypot(b['ix'], b['iz']) < 0.1:
            seq = [round(rows[j]['hspeed'], 2) for j in range(i, i + 8)]
            stop = next((rows[j]['t'] - a['t'] for j in range(i, i + 40) if rows[j]['hspeed'] < 0.05), None)
            print('  stop  t %.2f  pilot %d  speeds %s  to 0: %s' % (b['t'], b['alt_form'], seq,
                                                                 'n/a' if stop is None else '%.3f s' % stop))


def jumps(rows, evs):
    print('== jumps (TAKEOFF with Jump held): first-tick vy, top above take-off, air time, landing drop')
    by_t = {round(r['t'], 2): k for k, r in enumerate(rows)}
    lands = [e for e in evs if e['kind'] == 'LAND']
    for e in evs:
        if e['kind'] != 'TAKEOFF' or 'jump True' not in e['text']:
            continue
        k = by_t.get(round(e['t'], 2))
        if k is None:
            continue
        r0 = rows[k]
        land = next((l for l in lands if l['t'] > e['t']), None)
        seg = [r for r in rows[k:k + 200] if land is None or r['t'] <= land['t']]
        clean = all(not r['ab1'] and not r['ult'] for r in seg)
        top = max(r['py'] for r in seg) - rows[max(0, k - 1)]['py']
        print('  t %.2f  pilot %d  vy %.2f  top %.3f m  air %s  %s' % (e['t'], r0['alt_form'], r0['vy'], top,
                                                                    land['text'].split('|')[2].strip() + ' ' + land['text'].split('|')[3].strip() if land else '?',
                                                                    '' if clean else '(ability in it)'))
    print('== gravity: vy change a tick in the air, no ability, vy not 0')
    g = {}
    for a, b in zip(rows, rows[1:]):
        if a['ground'] or b['ground'] or a['ab1'] or b['ab1'] or a['ult'] or b['ult']:
            continue
        dt = b['t'] - a['t']
        if not (0.005 < dt < 0.025) or a['vy'] == 0 or b['vy'] == 0:
            continue
        # one tick apart: t is rounded to 0.01 s, so the step reads 0.01 or 0.02; a tick is 1 / 62.5 s
        g.setdefault((b['alt_form'], 'rising' if b['vy'] > 0 else 'falling'), []).append((a['vy'] - b['vy']) * 62.5)
    for k, xs in sorted(g.items()):
        print('  pilot %d %-7s n %d  median %.2f m/s2' % (k[0], k[1], len(xs), med(xs)))


def boosters(rows, evs):
    print('== Boosters: each in-use span (Ability 1) in the mech')
    for i0, i1 in spans(rows, 'ab1'):
        if rows[i0]['alt_form']:
            continue
        seg = rows[i0:i1 + 1]
        t0 = rows[i0]['t']
        # the flight: from the start until the speed first falls under 11.5 m/s with no wall (3D)
        # the flight: to the last tick at 12 m/s (hitting things dips it for a tick or two mid-flight)
        last = max([j for j in range(i0 + 1, i1 + 1) if spd(rows[j]) > 11.8] or [i0])
        flight = rows[i0:last + 1]
        speeds = [spd(r) for r in flight[1:]]
        angs = [ang((r['vx'], r['vy'], r['vz']), (r['fx'], r['fy'], r['fz'])) for r in flight[1:] if spd(r) > 11.5]
        # horizontal-only angle (pitch blocked by the floor)
        hangs = [ang((r['vx'], 0, r['vz']), (r['fx'], 0, r['fz'])) for r in flight[1:] if r['hspeed'] > 10]
        vyres = [r['vy'] - 12 * r['fy'] for r in flight[1:] if spd(r) > 11.5 and r['vy'] != 0]
        yaw = []
        for a, b in zip(flight, flight[1:]):
            dt = b['t'] - a['t']
            if 0.01 < dt < 0.025:
                yaw.append(ang((a['fx'], 0, a['fz']), (b['fx'], 0, b['fz'])) / dt)
        after = rows[i1 + 1] if i1 + 1 < len(rows) else None
        cd0 = after['cd_ab1'] if after else float('nan')
        cdz = next((r['t'] for r in rows[i1 + 1:] if r['cd_ab1'] == 0), None)
        ult_end = any(r['ult'] for r in rows[i1:i1 + 3])
        presses = [e for e in evs if e['kind'] == 'PRESS ABILITY1' and t0 < e['t'] <= rows[i1]['t'] + 0.02]
        print('  t %.2f  in use %.2f s  flight %.2f s  3D speed median %.2f (min %.2f max %.2f)  v-to-aim angle median %.1f deg max %.1f'
              '  flat %.1f  vy-12*fy median %.2f  facing yaw max %.0f deg/s  cooldown after %.2f -> 0 at +%s s%s%s' % (
                  t0, rows[i1]['t'] - t0 + 1 / 62.5, (flight[-1]['t'] - t0 + 1 / 62.5), med(speeds), min(speeds) if speeds else 0,
                  max(speeds) if speeds else 0, med(angs), max(angs) if angs else float('nan'), med(hangs), med(vyres),
                  max(yaw) if yaw else 0, cd0, '%.2f' % (cdz - rows[i1]['t']) if cdz else '?',
                  '  ended by ult' if ult_end else '', '  re-press at +%.2f' % (presses[0]['t'] - t0) if presses else ''))


def lag(rows):
    """How fast her flight follows the aim: angle between velocity and the facing a tick and two earlier."""
    print('== Boosters turn: velocity vs facing at lag 0/1/2 ticks, ticks where the facing turned > 2 deg in a tick')
    res = {0: [], 1: [], 2: []}
    for j in range(3, len(rows)):
        r = rows[j]
        if not (r['ab1'] and not r['alt_form'] and spd(r) > 11.5 and r['vy'] != 0 and rows[j - 2]['ab1']):
            continue
        turn = ang((rows[j - 1]['fx'], rows[j - 1]['fy'], rows[j - 1]['fz']), (r['fx'], r['fy'], r['fz']))
        if turn < 2:
            continue
        for L in res:
            f = rows[j - L]
            res[L].append(ang((r['vx'], r['vy'], r['vz']), (f['fx'], f['fy'], f['fz'])))
    for L, xs in res.items():
        print('  lag %d ticks: n %d  median angle %.2f deg' % (L, len(xs), med(xs)))


def matrix(rows):
    print('== Defense Matrix: meter (R secondary) while held and after')
    for i0, i1 in spans(rows, 'fire2'):
        r0, r1 = rows[i0], rows[i1]
        # the meter's fall: till it stops falling
        j = i0
        while j + 1 < len(rows) and rows[j + 1]['res_fire2'] < rows[j]['res_fire2']:
            j += 1
        fall_t = rows[j]['t'] - r0['t']
        drop = rows[max(0, i0 - 1)]['res_fire2'] - rows[j]['res_fire2']
        # refill: first rise after j, and its rate
        k = j
        while k + 1 < len(rows) and rows[k + 1]['res_fire2'] <= rows[k]['res_fire2'] and rows[k + 1]['t'] - rows[j]['t'] < 5:
            k += 1
        delay = rows[k]['t'] - rows[j]['t']
        m = k
        while m + 1 < len(rows) and rows[m + 1]['res_fire2'] > rows[m]['res_fire2'] and not rows[m + 1]['fire2']:
            m += 1
        rate = (rows[m]['res_fire2'] - rows[k]['res_fire2']) / (rows[m]['t'] - rows[k]['t']) if m > k else float('nan')
        sp = [r['hspeed'] for r in rows[i0:i1 + 1] if r['ground'] and math.hypot(r['ix'], r['iz']) > 0.5]
        fir2 = sum(1 for r in rows[i0:j + 1] if r['firing2'])
        print('  t %.2f  held %.2f s  meter %.1f -> %.1f, falling %.2f s (%.1f/s)  refill after %.2f s at %.1f/s (to %.1f)'
              '  firing2 ticks %d  walk speed median %s' % (
                  r0['t'], r1['t'] - r0['t'] + 1 / 62.5, rows[max(0, i0 - 1)]['res_fire2'], rows[j]['res_fire2'], fall_t,
                  drop / fall_t if fall_t > 0 else float('nan'), delay, rate, rows[m]['res_fire2'], fir2,
                  '%.2f' % med(sp) if sp else '-'))


def missiles(rows):
    print('== Micro Missiles (Ability 2 in use)')
    for i0, i1 in spans(rows, 'ab2'):
        r0 = rows[i0]
        sp = [r['hspeed'] for r in rows[i0:i1 + 1] if r['ground']]
        before = [r['hspeed'] for r in rows[max(0, i0 - 20):i0] if r['ground']]
        inp = [math.hypot(r['ix'], r['iz']) for r in rows[i0:i1 + 1]]
        print('  t %.2f  in use %.2f s  pilot %d  ground speed median during %s (before %s)  input held %.0f%%  boosting %d ticks'
              '  matrix %d ticks  firing1 %d ticks' % (
                  r0['t'], rows[i1]['t'] - r0['t'] + 1 / 62.5, r0['alt_form'], '%.2f' % med(sp) if sp else '-',
                  '%.2f' % med(before) if before else '-', 100 * sum(1 for x in inp if x > 0.5) / len(inp),
                  sum(1 for r in rows[i0:i1 + 1] if r['ab1']), sum(1 for r in rows[i0:i1 + 1] if r['fire2']),
                  sum(1 for r in rows[i0:i1 + 1] if r['firing1'])))


def ults(rows, evs):
    print('== Self-Destruct / eject and Call Mech (ultimate in use)')
    for i0, i1 in spans(rows, 'ult'):
        r0 = rows[i0]
        press = [e for e in evs if e['kind'] == 'PRESS ULTIMATE' and r0['t'] - 0.4 <= e['t'] <= r0['t'] + 0.05]
        pt = press[0]['t'] if press else None
        prev = rows[i0 - 1]
        if not prev['alt_form'] and r0['alt_form']:
            # the eject: the velocity before, the first ticks after
            seq = ['(%.2f, %.2f, %.2f)' % (r['vx'], r['vy'], r['vz']) for r in rows[i0:i0 + 4]]
            fflat = (prev['fx'], prev['fz'])
            n = math.hypot(*fflat) or 1
            print('  SELF-DESTRUCT t %.2f  in use %.2f s  press %s  boosting before %d  vel before (%.2f, %.2f, %.2f)'
                  '  facing before (%.2f, %.2f, %.2f)  after %s  pos before %s, at the eject %s, next %s' % (
                      r0['t'], rows[i1]['t'] - r0['t'] + 1 / 62.5, '%.2f' % pt if pt else '?', prev['ab1'], prev['vx'], prev['vy'],
                      prev['vz'], prev['fx'], prev['fy'], prev['fz'], ' '.join(seq),
                      (round(prev['px'], 2), round(prev['py'], 2), round(prev['pz'], 2)),
                      (round(r0['px'], 2), round(r0['py'], 2), round(r0['pz'], 2)),
                      (round(rows[i0 + 1]['px'], 2), round(rows[i0 + 1]['py'], 2), round(rows[i0 + 1]['pz'], 2))))
            # the impulse: second tick's velocity minus the one before, gravity added back for 2 ticks, in her facing frame
            r1, r2 = rows[i0 + 1], rows[i0 + 2]
            d = (r2['vx'] - r1['vx'], r2['vy'] - r1['vy'] + GRAV_GUESS / 62.5, r2['vz'] - r1['vz'])
            fwd = (fflat[0] / n, fflat[1] / n)
            along = d[0] * fwd[0] + d[2] * fwd[1]
            side = -d[0] * fwd[1] + d[2] * fwd[0]
            print('      the impulse (2nd tick after minus 1st, a tick of gravity added back): along her flat facing %.2f,'
                  ' sideways %.2f, up %.2f;  1st tick after vs before: %.2f m/s apart' % (
                      along, side, d[1], math.dist((r1['vx'], r1['vy'], r1['vz']), (prev['vx'], prev['vy'], prev['vz']))))
        else:
            link = next((r['t'] for r in rows[i0:] if not r['alt_form']), None)
            moved = max(math.dist((r['px'], r['py'], r['pz']), (r0['px'], r0['py'], r0['pz'])) for r in rows[i0:i1 + 1])
            vmax = max(spd(r) for r in rows[i0:i1 + 1])
            print('  CALL MECH t %.2f  press %s (cast %s)  in use %.2f s  in the mech at %s (%s s after the press)'
                  '  she moved at most %.2f m, speed max %.2f  ground at start %d' % (
                      r0['t'], '%.2f' % pt if pt else '?', '%.2f' % (r0['t'] - pt) if pt else '?',
                      rows[i1]['t'] - r0['t'] + 1 / 62.5, link, '%.2f' % (link - pt) if link and pt else '?', moved, vmax,
                      rows[i0 - 1]['ground']))


def pilot_air(rows):
    print('== pilot in the air after an eject: horizontal speed and its change a tick')
    for i0, i1 in spans(rows, 'alt_form'):
        seg = rows[i0:i0 + 120]
        seq = ['%.2f' % r['hspeed'] for r in seg[:60:6]]
        print('  from t %.2f: hspeed every 6 ticks %s' % (rows[i0]['t'], ' '.join(seq)))


def health(rows):
    print('== health changes (damage taken and regeneration)')
    last = None
    for r in rows:
        if last is not None and r['max_health'] == last['max_health'] and abs(r['health'] - last['health']) > 0.001:
            print('  t %.2f  %.2f -> %.2f  (%+.2f)  pilot %d  matrix %d  boosting %d  missiles %d' % (
                r['t'], last['health'], r['health'], r['health'] - last['health'], r['alt_form'], r['fire2'], r['ab1'], r['ab2']))
        last = r


def main():
    rows, evs = load(sys.argv[1])
    print('D.Va ticks: %d, %.2f to %.2f s' % (len(rows), rows[0]['t'], rows[-1]['t']))
    forms(rows)
    walking(rows)
    accel(rows)
    jumps(rows, evs)
    boosters(rows, evs)
    lag(rows)
    matrix(rows)
    missiles(rows)
    ults(rows, evs)
    pilot_air(rows)
    if '--health' in sys.argv:
        health(rows)


if __name__ == '__main__':
    main()

"""Parses a Workshop recorder log (tools/workshop/lucio_wallride_recorder.txt or movement_recorder.txt, the one for every
hero) into ticks and events, split by MARK. The movement recorder adds the hero, the K, C and X lines and multi-word
events. Only regexes over the text: the log is data (run with py -I).

    py -I parse_log.py <log> <out.json>              everything in one JSON (the older scripts read this)
    py -I parse_log.py <log> --csv <dir> [--sqlite]  ticks.csv and events.csv, written as the log is read (a long
                                                      session never sits in memory), with each hero's named columns
                                                      (heroes.py); --sqlite also loads them into <dir>/log.sqlite
"""
import csv
import json
import math
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import heroes  # noqa: E402

NUM = r'(-?[\d.]+)'
VEC = r'\(' + NUM + r', ' + NUM + r', ' + NUM + r'\)'
RE_T = re.compile(r'S(\d+) T ' + NUM + r' \| (\w+) (\w+) (\w+)')
RE_P = re.compile(r'S(\d+) P ' + VEC + r' \| V ' + VEC + r' \| I ' + VEC)
RE_B = re.compile(r'S(\d+) B (\w+) (\w+) \| F ' + VEC + r' \| N ' + VEC + r' \| D ' + NUM)
RE_A = re.compile(r'S(\d+) A (\w+) (\w+) (\w+) \| U (\w+) (\w+) (\w+) \| X (\d+)')
RE_HERO = re.compile(r'S\d+ T [-\d.]+ \| \w+ \w+ \w+ \| (.+)$')
RE_K = re.compile(r'S(\d+) K (\w+) (\w+) (\w+) \| (\w+) (\w+) \| R ' + NUM + r' ' + NUM + r' ' + NUM)
RE_C = re.compile(r'S(\d+) C ' + NUM + r' ' + NUM + r' \| Q ' + NUM + r' ' + NUM + r' \| CD ' + NUM + r' ' + NUM)
RE_X = re.compile(r'S(\d+) X ' + NUM + r' (\w+) \| W (\w+) (\w+) \| H ' + NUM + r' ' + NUM + r' \| ST (\w+) (\w+) (\w+) (\w+) (\w+)')
RE_EV = re.compile(r'S(\d+) EVENT ([A-Z][A-Z0-9 ]*?)(?: (\d+))? \| (.*)')


def truth(s):
    return s in ('True', '1')


def vec(m, i):
    return [float(m.group(i)), float(m.group(i + 1)), float(m.group(i + 2))]


def read(path):
    """Yields ('tick', dict) once each tick's lines are all in, and ('event', dict) as events come."""
    cur = None
    for line in open(path, encoding='utf8', errors='replace'):
        line = line.rstrip('\n')
        body = line.split('] ', 1)[1] if line.startswith('[') else line
        m = RE_T.match(body)
        if m:
            if cur is not None:
                yield 'tick', cur
            cur = {'s': int(m.group(1)), 't': float(m.group(2)), 'wall': truth(m.group(3)), 'ground': truth(m.group(4)),
                   'air': truth(m.group(5))}
            h = RE_HERO.match(body)
            if h:
                cur['hero'] = h.group(1).strip()
            continue
        if cur is None:
            continue
        m = RE_P.match(body)
        if m:
            cur['pos'] = vec(m, 2)
            cur['vel'] = vec(m, 5)
            cur['inp'] = vec(m, 8)
            continue
        m = RE_B.match(body)
        if m:
            cur['jump'] = truth(m.group(2))
            cur['crouch'] = truth(m.group(3))
            cur['face'] = vec(m, 4)
            cur['n'] = vec(m, 7)
            cur['d'] = float(m.group(10))
            continue
        m = RE_A.match(body)
        if m:
            cur['fire1'] = truth(m.group(2))
            cur['fire2'] = truth(m.group(3))
            cur['crouching'] = truth(m.group(4))
            cur['ab1'] = truth(m.group(5))
            cur['ab2'] = truth(m.group(6))
            cur['ult'] = truth(m.group(7))
            cur['xf'] = int(m.group(8))
            continue
        m = RE_K.match(body)
        if m:
            cur['ab1held'], cur['ab2held'], cur['ultheld'] = truth(m.group(2)), truth(m.group(3)), truth(m.group(4))
            cur['melee'], cur['reload'] = truth(m.group(5)), truth(m.group(6))
            cur['res_jump'], cur['res_fire1'], cur['res_fire2'] = float(m.group(7)), float(m.group(8)), float(m.group(9))
            continue
        m = RE_C.match(body)
        if m:
            cur['res_ab1'], cur['res_ab2'] = float(m.group(2)), float(m.group(3))
            cur['charge_ab1'], cur['charge_ab2'] = float(m.group(4)), float(m.group(5))
            cur['cd_ab1'], cur['cd_ab2'] = float(m.group(6)), float(m.group(7))
            continue
        m = RE_X.match(body)
        if m:
            cur['ult_pct'], cur['alt_form'] = float(m.group(2)), truth(m.group(3))
            cur['firing1'], cur['firing2'] = truth(m.group(4)), truth(m.group(5))
            cur['health'], cur['max_health'] = float(m.group(6)), float(m.group(7))
            cur['stunned'], cur['asleep'], cur['knocked'], cur['hacked'], cur['phased'] = (truth(m.group(i)) for i in range(8, 13))
            continue
        m = RE_EV.match(body)
        if m:
            ev = {'s': int(m.group(1)), 'kind': m.group(2), 'n': m.group(3) or '', 'text': m.group(4)}
            tm = re.search(r't ' + NUM, ev['text'])
            ev['t'] = float(tm.group(1)) if tm else cur['t']
            yield 'event', ev
    if cur is not None:
        yield 'tick', cur


# the CSV's columns: the generic fields (vectors split into x / y / z), a derived horizontal speed, then every hero's named
# columns (empty where the tick's hero doesn't have one)
VECTORS = {'pos': 'p', 'vel': 'v', 'inp': 'i', 'face': 'f', 'n': 'n'}
FIELDS = ['wall', 'ground', 'air', 'jump', 'crouch', 'd', 'fire1', 'fire2', 'crouching', 'ab1', 'ab2', 'ult', 'xf',
          'ab1held', 'ab2held', 'ultheld', 'melee', 'reload', 'res_jump', 'res_fire1', 'res_fire2', 'res_ab1', 'res_ab2',
          'charge_ab1', 'charge_ab2', 'cd_ab1', 'cd_ab2', 'ult_pct', 'alt_form', 'firing1', 'firing2', 'health',
          'max_health', 'stunned', 'asleep', 'knocked', 'hacked', 'phased']
TICK_COLUMNS = (['s', 't', 'seg', 'hero'] + [p + a for p in VECTORS.values() for a in 'xyz'] + ['hspeed'] + FIELDS
                + heroes.columns())
EVENT_COLUMNS = ['s', 't', 'seg', 'kind', 'n', 'text']
# a hero's named column can't reuse a generic one's name (D.Va's 'ult' did: the SQLite load failed on it)
assert len(set(TICK_COLUMNS)) == len(TICK_COLUMNS), [c for c in TICK_COLUMNS if TICK_COLUMNS.count(c) > 1]


def cell(v):
    return int(v) if isinstance(v, bool) else v


def tick_row(tk):
    row = {'s': tk['s'], 't': tk['t'], 'seg': tk['seg'], 'hero': tk.get('hero', '')}
    for key, p in VECTORS.items():
        v = tk.get(key)
        for k, a in enumerate('xyz'):
            row[p + a] = v[k] if v else ''
    row['hspeed'] = round(math.hypot(tk['vel'][0], tk['vel'][2]), 4) if 'vel' in tk else ''
    for f in FIELDS:
        row[f] = cell(tk.get(f, ''))
    for name, v in heroes.named(tk).items():
        row[name] = cell(v)
    return row


def to_csv(log, out_dir, sqlite):
    os.makedirs(out_dir, exist_ok=True)
    seg = 1
    counts = {'ticks': 0, 'events': 0, 'marks': 0}
    seen_heroes = set()
    with open(os.path.join(out_dir, 'ticks.csv'), 'w', newline='', encoding='utf8') as tf, \
            open(os.path.join(out_dir, 'events.csv'), 'w', newline='', encoding='utf8') as ef:
        tw = csv.DictWriter(tf, TICK_COLUMNS)
        ew = csv.DictWriter(ef, EVENT_COLUMNS)
        tw.writeheader()
        ew.writeheader()
        for kind, obj in read(log):
            if kind == 'event':
                if obj['kind'] == 'MARK':
                    # a mark starts the next segment (as the JSON mode's bounds do)
                    seg += 1
                    counts['marks'] += 1
                obj['seg'] = seg
                ew.writerow(obj)
                counts['events'] += 1
            else:
                obj['seg'] = seg
                tw.writerow(tick_row(obj))
                counts['ticks'] += 1
                seen_heroes.add(obj.get('hero', ''))
    print('ticks', counts['ticks'], 'events', counts['events'], 'marks', counts['marks'],
          'heroes', sorted(h for h in seen_heroes if h))
    unknown = sorted(h for h in seen_heroes if h and h not in heroes.HEROES)
    if unknown:
        print('heroes with no entry in heroes.py (generic columns only):', unknown)
    if sqlite:
        load_sqlite(out_dir)


def load_sqlite(out_dir):
    """ticks.csv and events.csv into <dir>/log.sqlite (tables ticks, events), streamed in batches."""
    import sqlite3
    path = os.path.join(out_dir, 'log.sqlite')
    if os.path.exists(path):
        os.remove(path)
    db = sqlite3.connect(path)
    for table in ('ticks', 'events'):
        with open(os.path.join(out_dir, table + '.csv'), encoding='utf8') as f:
            reader = csv.reader(f)
            head = next(reader)
            # NUMERIC affinity: "1" and "6.87" are stored as numbers (ground = 1 matches), words stay text
            db.execute('CREATE TABLE %s (%s)' % (table, ', '.join('"%s" NUMERIC' % h for h in head)))
            sql = 'INSERT INTO %s VALUES (%s)' % (table, ', '.join('?' * len(head)))
            batch = []
            for row in reader:
                batch.append(row)
                if len(batch) >= 5000:
                    db.executemany(sql, batch)
                    batch = []
            if batch:
                db.executemany(sql, batch)
    db.execute('CREATE INDEX ticks_t ON ticks (s, t)')
    db.commit()
    db.close()
    print('sqlite:', path)


def to_json(log, out):
    ticks = []
    events = []
    for kind, obj in read(log):
        (events if kind == 'event' else ticks).append(obj)
    marks = [e['t'] for e in events if e['kind'] == 'MARK']
    bounds = [0.0] + marks + [1e9]
    for item in ticks + events:
        for i in range(len(bounds) - 1):
            if bounds[i] <= item['t'] < bounds[i + 1]:
                item['seg'] = i + 1
                break
    slots = sorted({t['s'] for t in ticks})
    json.dump({'ticks': ticks, 'events': events, 'marks': marks}, open(out, 'w'))
    print('ticks', len(ticks), 'events', len(events), 'marks', len(marks), 'slots', slots)
    print('event kinds', {k: sum(1 for e in events if e['kind'] == k) for k in sorted({e['kind'] for e in events})})
    print('ticks missing fields', sum(1 for t in ticks if not all(k in t for k in ('pos', 'jump', 'xf'))))
    if ticks:
        print('time span', ticks[0]['t'], ticks[-1]['t'])
    dts = sorted(b['t'] - a['t'] for a, b in zip(ticks, ticks[1:]) if a['s'] == b['s'])
    if dts:
        print('tick dt median %.4f  p95 %.4f  max %.3f' % (dts[len(dts) // 2], dts[int(len(dts) * 0.95)], dts[-1]))


if __name__ == '__main__':
    args = sys.argv[1:]
    if '--csv' in args:
        to_csv(args[0], args[args.index('--csv') + 1], '--sqlite' in args)
    else:
        to_json(args[0], args[1])

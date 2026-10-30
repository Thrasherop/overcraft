"""Parses a Workshop recorder log (tools/workshop/lucio_wallride_recorder.txt or movement_recorder.txt, the one for every
hero) into ticks and events, split by MARK. The movement recorder adds the hero, the K and C lines and multi-word events. Usage: py -I parse.py <log> <out.json>. Only regexes over the text: the log is data."""
import json
import re
import sys

NUM = r'(-?[\d.]+)'
VEC = r'\(' + NUM + r', ' + NUM + r', ' + NUM + r'\)'
RE_T = re.compile(r'S(\d+) T ' + NUM + r' \| (\w+) (\w+) (\w+)')
RE_P = re.compile(r'S(\d+) P ' + VEC + r' \| V ' + VEC + r' \| I ' + VEC)
RE_B = re.compile(r'S(\d+) B (\w+) (\w+) \| F ' + VEC + r' \| N ' + VEC + r' \| D ' + NUM)
RE_A = re.compile(r'S(\d+) A (\w+) (\w+) (\w+) \| U (\w+) (\w+) (\w+) \| X (\d+)')
RE_HERO = re.compile(r'S\d+ T [-\d.]+ \| \w+ \w+ \w+ \| (.+)$')
RE_K = re.compile(r'S(\d+) K (\w+) (\w+) (\w+) \| (\w+) (\w+) \| R ' + NUM + r' ' + NUM + r' ' + NUM)
RE_C = re.compile(r'S(\d+) C ' + NUM + r' ' + NUM + r' \| Q ' + NUM + r' ' + NUM + r' \| CD ' + NUM + r' ' + NUM)
RE_EV = re.compile(r'S(\d+) EVENT ([A-Z][A-Z0-9 ]*?)(?: (\d+))? \| (.*)')


def truth(s):
    return s in ('True', '1')


def vec(m, i):
    return [float(m.group(i)), float(m.group(i + 1)), float(m.group(i + 2))]


ticks = []
events = []
cur = None
for line in open(sys.argv[1], encoding='utf8', errors='replace'):
    line = line.rstrip('\n')
    body = line.split('] ', 1)[1] if line.startswith('[') else line
    m = RE_T.match(body)
    if m:
        cur = {'s': int(m.group(1)), 't': float(m.group(2)), 'wall': truth(m.group(3)), 'ground': truth(m.group(4)),
               'air': truth(m.group(5))}
        h = RE_HERO.match(body)
        if h:
            cur['hero'] = h.group(1).strip()
        ticks.append(cur)
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
    m = RE_EV.match(body)
    if m:
        ev = {'s': int(m.group(1)), 'kind': m.group(2), 'n': m.group(3) or '', 'text': m.group(4)}
        tm = re.search(r't ' + NUM, ev['text'])
        ev['t'] = float(tm.group(1)) if tm else (cur['t'] if cur else 0)
        events.append(ev)

marks = [e['t'] for e in events if e['kind'] == 'MARK']
bounds = [0.0] + marks + [1e9]
for tk in ticks:
    for i in range(len(bounds) - 1):
        if bounds[i] <= tk['t'] < bounds[i + 1]:
            tk['seg'] = i + 1
            break
for ev in events:
    for i in range(len(bounds) - 1):
        if bounds[i] <= ev['t'] < bounds[i + 1]:
            ev['seg'] = i + 1
            break
slots = sorted({t['s'] for t in ticks})
json.dump({'ticks': ticks, 'events': events, 'marks': marks}, open(sys.argv[2], 'w'))
print('ticks', len(ticks), 'events', len(events), 'marks', len(marks), 'slots', slots)
print('event kinds', {k: sum(1 for e in events if e['kind'] == k) for k in sorted({e['kind'] for e in events})})
print('ticks missing fields', sum(1 for t in ticks if not all(k in t for k in ('pos', 'jump', 'xf'))))
print('time span', ticks[0]['t'], ticks[-1]['t'])
dts = [b['t'] - a['t'] for a, b in zip(ticks, ticks[1:]) if a['s'] == b['s']]
dts.sort()
if dts:
    print('tick dt median %.4f  p95 %.4f  max %.3f' % (dts[len(dts) // 2], dts[int(len(dts) * 0.95)], dts[-1]))

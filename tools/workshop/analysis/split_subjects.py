"""Splits a movement-recorder log in which two players share one slot tag (the owner's 2026-11-11 log: a Hanzo and a
D.Va, later Pharah, both logged as S0, one per team) into one log per subject, retagged S0 and S1, so parse_log.py and
the rest read each alone. Only regexes over the text: the log is data (run with py -I).

    py -I split_subjects.py <log> <out dir> "<hero A>[,<hero A2>]" ["<hero B>,..."]

A T line's hero picks its subject (a subject's heroes are given as lists: a player who changes hero keeps their
subject). The P/B/A/K/C/X lines after a T line belong to it. An EVENT line is matched to a subject by its velocity
(and position when it has one) against each subject's P line of the same tick; when both or neither match it goes to
the subject of the T block it follows, and is counted as ambiguous.
"""
import os
import re
import sys

NUM = r'(-?[\d.]+)'
VEC = r'\(' + NUM + r', ' + NUM + r', ' + NUM + r'\)'
RE_T = re.compile(r'S\d+ T ' + NUM + r' \| \w+ \w+ \w+ \| (.+)$')
RE_P = re.compile(r'S\d+ P ' + VEC + r' \| V ' + VEC)
RE_EV = re.compile(r'S\d+ EVENT .*?\| t ' + NUM)
RE_EVV = re.compile(r'vel ' + VEC)
RE_EVP = re.compile(r'pos ' + VEC)
RE_SLOT = re.compile(r'S\d+ ')


def main():
    path, out = sys.argv[1], sys.argv[2]
    groups = [g.split(',') for g in sys.argv[3:]]
    os.makedirs(out, exist_ok=True)
    who = {h.strip(): i for i, g in enumerate(groups) for h in g}
    files = [open(os.path.join(out, 'S%d.txt' % i), 'w', encoding='utf8') for i in range(len(groups))]
    def emit(i, line):
        head, _, body = line.partition('] ')
        files[i].write(head + '] ' + RE_SLOT.sub('S%d ' % i, body, count=1) + '\n')

    lines = [raw.rstrip('\n') for raw in open(path, encoding='utf8', errors='replace')]
    bodies = [ln.split('] ', 1)[1] if ln.startswith('[') else ln for ln in lines]
    # pass 1: every subject's P line by tick
    seen = {}  # (subject, t) -> (pos, vel)
    cur = None
    for body in bodies:
        m = RE_T.match(body)
        if m:
            cur = who.get(m.group(2).strip())
            curt = float(m.group(1))
            continue
        pm = RE_P.match(body)
        if pm and cur is not None:
            seen[(cur, round(curt, 2))] = ([float(pm.group(k)) for k in (1, 2, 3)], [float(pm.group(k)) for k in (4, 5, 6)])
    # pass 2: write
    cur = None
    amb = ok = 0
    for line, body in zip(lines, bodies):
        m = RE_T.match(body)
        if m:
            cur = who.get(m.group(2).strip())
            if cur is not None:
                emit(cur, line)
            continue
        e = RE_EV.match(body)
        if e:
            t = round(float(e.group(1)), 2)
            v = RE_EVV.search(body)
            p = RE_EVP.search(body)
            cands = []
            for i in range(len(groups)):
                if (i, t) not in seen or not (v or p):
                    continue
                lp, lv = seen[(i, t)]
                good = True
                if v:
                    good = all(abs(float(v.group(k + 1)) - lv[k]) < 0.015 for k in range(3))
                if good and p:
                    good = all(abs(float(p.group(k + 1)) - lp[k]) < 0.015 for k in range(3))
                if good:
                    cands.append(i)
            if len(cands) == 1:
                ok += 1
                emit(cands[0], line)
            else:
                amb += 1
                if cur is not None:
                    emit(cur, line)
            continue
        if cur is not None:
            emit(cur, line)
    for f in files:
        f.close()
    print('events matched %d, ambiguous (left with the block before) %d' % (ok, amb))


if __name__ == '__main__':
    main()

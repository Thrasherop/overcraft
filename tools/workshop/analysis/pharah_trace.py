"""Pharah, tick by tick: a compact table of a Workshop recorder log's ticks between two times (2026-11-11 analysis,
techniques/heroes/pharah_workshop_2026-11-11.md). Reads the ticks.csv that parse_log.py --csv writes; run with py -I.

    py -I pharah_trace.py <ticks.csv> [t0] [t1] [--every N]

Only Pharah's ticks: the recorder tags each team's first player S0, so the owner's 2026-11-11 logs interleave her with
an enemy Hanzo also logged as S0 (his rows have 250 health). The events carry no hero: tell them apart by position.
The log's times have two decimals; a tick is 0.016 s (62.5 a second), so step by rows, not by the printed time.

Columns: row, t, g(round) / a(ir), height, vertical speed, horizontal speed, the move input (x z), Jump held, LMB / RMB
held and firing, Shift / E / Q in use, the C line's cooldowns, reload held, ult %, health. The K / C resource meters
read 0 all through her logs (the Workshop's Ability Resource doesn't hold her fuel), so they're left out.
"""
import csv
import sys


def f(v, n=2):
    try:
        return ('%.' + str(n) + 'f') % float(v)
    except ValueError:
        return '-'


def b(v, ch):
    return ch if v in ('1', 'True') else '.'


def main():
    argv = sys.argv[1:]
    every = 1
    if '--every' in argv:
        i = argv.index('--every')
        every = int(argv[i + 1])
        del argv[i:i + 2]
    path = argv[0]
    t0 = float(argv[1]) if len(argv) > 1 else -1e9
    t1 = float(argv[2]) if len(argv) > 2 else 1e9
    print('  row      t g      y      vy     hs    ix    iz J LfRf 1 2 Q   cd1   cd2 R ult  hp')
    row = -1
    for r in csv.DictReader(open(path, encoding='utf8')):
        if r['hero'] != 'Pharah':
            continue
        row += 1
        t = float(r['t'])
        if t < t0 or t > t1 or row % every:
            continue
        print('%5d %6s %s %6s %7s %6s %5s %5s %s %s%s%s%s %s %s %s %5s %5s %s %3s %3s' % (
            row, f(r['t']), 'g' if r['ground'] in ('1', 'True') else 'a', f(r['py']), f(r['vy']), f(r['hspeed']),
            f(r['ix']), f(r['iz']), b(r['jump'], 'J'), b(r['fire1'], 'L'), b(r['firing1'], 'f'), b(r['fire2'], 'R'),
            b(r['firing2'], 'f'), b(r['ab1'], '1'), b(r['ab2'], '2'), b(r['ult'], 'Q'), f(r['cd_ab1']),
            f(r['cd_ab2']), b(r['reload'], 'R'), f(r['ult_pct'], 0), f(r['health'], 0)))


if __name__ == '__main__':
    main()

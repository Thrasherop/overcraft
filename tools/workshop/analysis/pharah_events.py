"""Pharah's events from a Workshop recorder log, each tagged by the hero of the tick block it sits in (2026-11-11
analysis, techniques/heroes/pharah_workshop_2026-11-11.md). The owner's logs carry an AI bot (Hanzo) also tagged S0,
so slot alone can't tell whose an event is; an EVENT line follows its own player's T line. Regexes only; run with py -I.

    py -I pharah_events.py <log> [hero]      (hero defaults to Pharah; skips PRESS / RELEASE JUMP unless --jump)
"""
import re
import sys

RE_T = re.compile(r'S\d+ T [-\d.]+ \| \w+ \w+ \w+ \| (.+)$')


def main():
    argv = [a for a in sys.argv[1:] if not a.startswith('--')]
    hero = argv[1] if len(argv) > 1 else 'Pharah'
    jump = '--jump' in sys.argv
    cur = None
    for line in open(argv[0], encoding='utf8', errors='replace'):
        body = line.rstrip('\n').split('] ', 1)[-1]
        m = RE_T.match(body)
        if m:
            cur = m.group(1).strip()
            continue
        if ' EVENT ' in body and cur == hero:
            if not jump and ('PRESS JUMP' in body or 'RELEASE JUMP' in body):
                continue
            print(body)


if __name__ == '__main__':
    main()

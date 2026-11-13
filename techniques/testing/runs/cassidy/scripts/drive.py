"""Drives the dev client over one harness socket and stamps every command and answer into runs.txt.
Usage: py -I drive.py <label> <script file>   (one command a line; 'MARK text', 'sleep s', 'hp <selector> [path]', anything DevServer takes)"""
import datetime
import socket
import sys
import time

RUNS = 'E:/claude/mods/overcraft_cassidy/techniques/testing/runs/cassidy/runs.txt'
LOG = 'E:/claude/mods/overcraft_cassidy/run/logs/latest.log'
KEEP = ('"x"', '"y"', '"z"', '"yaw"', '"health"', '"onGround"', '"hero"', '"heroMode"', '"ult"', '"mode"', '"primaryAmmo"',
        '"primaryReload"', '"meter"', '"secondaryAmmo"', '"ability1Cooldown"', '"ability2Cooldown"', '"vx"', '"vy"', '"vz"', '"anim"',
        '"screen"', '"shield"', '"perkLevel"')

label, script = sys.argv[1], sys.argv[2]
lines = [l.rstrip('\n') for l in open(script, encoding='utf8') if l.strip() and not l.startswith('#')]
out = open(RUNS, 'a', encoding='utf8')


def stamp(text):
    t = datetime.datetime.now().strftime('%H:%M:%S.%f')[:-3]
    out.write(f'{t} {text}\n')
    out.flush()
    print(f'{t} {text}')


def short_state(ans):
    import json
    try:
        d = json.loads(ans)
    except Exception:
        return ans
    return json.dumps({k: (round(v, 3) if isinstance(v, float) else v) for k, v in d.items() if f'"{k}"' in KEEP})


out.write(f'\n=== {label} {datetime.datetime.now():%Y-%m-%d %H:%M:%S}\n')
with socket.create_connection(('127.0.0.1', 25599), timeout=60) as sock:
    f = sock.makefile('r', encoding='utf8')
    for c in lines:
        if c.startswith('sleep '):
            time.sleep(float(c.split()[1]))
            continue
        if c.startswith('MARK'):
            stamp(c)
            continue
        if c.startswith('hp '):
            # a /data get through the chat, its answer read from the game's log
            q = c[3:] if ' ' in c[3:] else c[3:] + ' Health'
            n = len(open(LOG, encoding='utf8', errors='replace').readlines())
            sock.sendall(('cmd /data get entity ' + q + '\n').encode('utf8'))
            f.readline()
            time.sleep(0.4)
            new = [l.strip() for l in open(LOG, encoding='utf8', errors='replace').readlines()[n:] if '[CHAT]' in l]
            stamp(f'{c} -> {new[-1].split("[CHAT]")[-1].strip() if new else "(nothing)"}')
            continue
        sock.sendall((c + '\n').encode('utf8'))
        ans = f.readline().strip()
        if c == 'state':
            ans = short_state(ans)
        elif len(ans) > 1500:
            ans = ans[:1500] + '...'
        stamp(f'{c} -> {ans}')

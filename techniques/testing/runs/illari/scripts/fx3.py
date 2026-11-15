"""Illari fix round 2 (2026-11-14, the coordinator's real-key play on dbcc4e7): S1 a left click during Captive Sun's launch; S2 the ball's
splash on two husks (default armor, as the report's), then two rifle body shots on one within ~2 s (Sunstruck expected); K a walking
husk's hits on her against the same on Kiriko (her z every 0.25 s). Usage: py -I fx3.py <label> [world]"""
import datetime
import json
import socket
import sys
import time

RUNS = 'E:/claude/mods/overcraft_illari/techniques/testing/runs/illari/runs.txt'
LOG = 'E:/claude/mods/overcraft_illari/run/logs/latest.log'
label = sys.argv[1]
world = sys.argv[2] if len(sys.argv) > 2 else None
out = open(RUNS, 'a', encoding='utf8')
out.write('\n=== ' + label + ' ' + datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S') + '\n')
sock = socket.create_connection(('127.0.0.1', 25599), timeout=60)
f = sock.makefile('r', encoding='utf8')


def stamp(t):
    s = datetime.datetime.now().strftime('%H:%M:%S.%f')[:-3] + ' ' + t
    out.write(s + '\n'); out.flush(); print(s)


def send(c):
    sock.sendall((c + '\n').encode('utf8'))
    return f.readline().strip()


def cmd(c):
    send('cmd ' + c)


def hp(sel):
    n = len(open(LOG, encoding='utf8', errors='replace').readlines())
    send(f'cmd /data get entity {sel} Health')
    time.sleep(0.35)
    new = [l.strip() for l in open(LOG, encoding='utf8', errors='replace').readlines()[n:] if '[CHAT]' in l]
    r = new[-1].split('[CHAT]')[-1].strip() if new else '(nothing)'
    stamp(f'hp {sel} -> {r}')


def state(keys=('y', 'z', 'health', 'primaryAmmo', 'mode', 'hero')):
    d = json.loads(send('state'))
    stamp('state ' + json.dumps({k: (round(d[k], 3) if isinstance(d.get(k), float) else d.get(k)) for k in keys}))
    return d


def tap(k, hold=0.12):
    send(f'key {k} down'); time.sleep(hold); send(f'key {k} up')


if world:
    send('window 1920 1080'); time.sleep(3)
    send('newworld ' + world); time.sleep(45)
    cmd('/difficulty peaceful'); cmd('/gamerule spawn_monsters false'); cmd('/time set noon'); cmd('/weather clear')
    send('screenclick 390 216 1'); time.sleep(1.5); send('screenclick 390 216 1'); time.sleep(3)
cmd('/difficulty easy')
cmd('/kill @e[type=!player]')
H = '{NoAI:1b,Tags:["%s"],attributes:[{id:"minecraft:max_health",base:20}],Health:20f}'

stamp('MARK S1 a left click 0.5 s into the launch (report: a rifle shot, ammo 14 -> 13); husks at 0.5 -60 12 and 2.5 -60 13, her at 0.5 -60 -1')
cmd('/tp @s 0.5 -60 -1 0 0')
cmd('/summon husk 0.5 -60 12 ' + H % 'a1')
cmd('/summon husk 2.5 -60 13 ' + H % 'a2')
cmd('/execute as @s at @s anchored eyes facing 0.5 -59 12 run tp @s ~ ~ ~ ~ ~')
send('ult 100'); time.sleep(1)
state()
tap('ultimate', 0.15)
time.sleep(0.4)
cmd('/execute as @s at @s anchored eyes facing 0.5 -59 12 run tp @s ~ ~ ~ ~ ~')
tap('secondary_fire')
time.sleep(0.3)
state()
hp('@e[tag=a1,limit=1]')
stamp('MARK S2 the ball at 1.5 -60 12.5 (between them), then two body shots on a1 within ~2 s (Sunstruck expected 0.8 s after the second)')
time.sleep(0.5)
cmd('/execute as @s at @s anchored eyes facing 1.5 -60 12.5 run tp @s ~ ~ ~ ~ ~')
time.sleep(0.15)
tap('secondary_fire')
time.sleep(0.8)
hp('@e[tag=a1,limit=1]')
hp('@e[tag=a2,limit=1]')
for i in range(2):
    cmd('/execute as @s at @s anchored eyes facing entity @e[tag=a1,limit=1] feet run tp @s ~ ~ ~ ~ ~')
    cmd('/execute as @s at @s run tp @s ~ ~ ~ ~ ~-6')
    time.sleep(0.75)
    tap('secondary_fire')
    time.sleep(0.1)
    hp('@e[tag=a1,limit=1]')
time.sleep(1.5)
hp('@e[tag=a1,limit=1]')
hp('@e[tag=a2,limit=1]')
time.sleep(5)


def knock(name):
    cmd('/kill @e[type=!player]')
    cmd('/difficulty normal')
    cmd('/effect give @s resistance 30 4 true')
    cmd('/tp @s 0.5 -60 0.5 0 0')
    time.sleep(1.5)
    cmd('/summon husk 0.5 -60 3.0 {Tags:["k1"]}')
    t0 = time.time()
    while time.time() - t0 < 6:
        state(('z', 'y', 'health', 'hero'))
        time.sleep(0.25)
    cmd('/kill @e[type=!player]')
    cmd('/difficulty easy')
    cmd('/effect clear @s')


stamp('MARK K1 a walking husk (normal) hits Illari: her z every 0.25 s (report: ~2.7 m a hit)')
knock('illari')
stamp('MARK K2 the same on Kiriko (report: ~0.5 m a hit)')
send('hero kiriko'); time.sleep(1.5)
knock('kiriko')
send('hero illari'); time.sleep(1.5)
state()

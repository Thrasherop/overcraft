"""Illari fix round, the aimed checks (2026-11-14): FX1c her own pylon in the shot's line (the husk 1.5 m behind it, aimed through the
pylon's box at 0.76 m), FX3b the ball through a husk held in mid-air at her height (pass-through: 5 only, no burst there), FX2b a fresh
ball's marks and 40 OW of /damage (no Sunstruck), FX7b a built pylon's hum stopping when it goes. Stamps into runs.txt via drive.py's
format. Usage: py -I fx2.py <label> [world]"""
import datetime
import json
import math
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
    a = f.readline().strip()
    return a


def cmd(c):
    send('cmd ' + c)


def hp(sel, path='Health'):
    n = len(open(LOG, encoding='utf8', errors='replace').readlines())
    send(f'cmd /data get entity {sel} {path}')
    time.sleep(0.4)
    new = [l.strip() for l in open(LOG, encoding='utf8', errors='replace').readlines()[n:] if '[CHAT]' in l]
    r = new[-1].split('[CHAT]')[-1].strip() if new else '(nothing)'
    stamp(f'hp {sel} {path} -> {r}')
    return r


def state():
    d = json.loads(send('state'))
    stamp('state ' + json.dumps({k: d.get(k) for k in ('y', 'primaryAmmo', 'ability2Cooldown', 'mode', 'ult')}))
    return d


def tap(k, hold=0.12):
    send(f'key {k} down'); time.sleep(hold); send(f'key {k} up')


def pylon():
    e = json.loads(send('ents Pylon')).get('ents', '').split('|')[0].split()
    return (float(e[1]), float(e[2]), float(e[3])) if len(e) >= 4 else None


if world:
    send('window 1920 1080'); time.sleep(3)
    send('newworld ' + world); time.sleep(45)
    cmd('/difficulty peaceful'); cmd('/gamerule spawn_monsters false'); cmd('/time set noon'); cmd('/weather clear')
    send('screenclick 390 216 1'); time.sleep(1.5); send('screenclick 390 216 1'); time.sleep(3)
    cmd('/difficulty easy')
cmd('/kill @e[type=!player]')
HUSK = '{NoAI:1b,Tags:["%s"],attributes:[{id:"minecraft:armor",base:0},{id:"minecraft:max_health",base:100}],Health:100f%s}'

stamp('MARK FX1c her own pylon in the shot: husk 1.5 m behind it on its x, aimed at the husk 0.5 m up (the line 0.76 m up at the pylon)')
cmd('/tp @s 0.5 -60 0.5 0 0'); time.sleep(8)
cmd('/execute as @s at @s anchored eyes facing 0.5 -60 6 run tp @s ~ ~ ~ ~ ~'); time.sleep(0.5)
tap('ability_2', 0.15); time.sleep(2)
p = pylon()
stamp(f'pylon at {p}')
cmd(f'/summon husk {p[0]:.2f} -60 {p[2] + 1.5:.2f} ' + HUSK % ('p1', ''))
cmd(f'/execute as @s at @s anchored eyes facing {p[0]:.2f} -59.5 {p[2] + 1.5:.2f} run tp @s ~ ~ ~ ~ ~')
time.sleep(1.2)
state()
tap('secondary_fire')
time.sleep(0.6)
hp('@e[tag=p1,limit=1]')
state()

stamp('MARK FX7b the built pylon hums; killed, its hum stops')
time.sleep(1)
stamp('playing ' + send('playing'))
cmd('/kill @e[type=overcraft:healing_pylon]')
time.sleep(1)
stamp('playing ' + send('playing'))

stamp('MARK FX3b the ball through a husk held at her height 10 m ahead (no gravity): pass-through = 5 only; old = burst there, 55')
cmd('/kill @e[type=!player]')
cmd('/tp @s 0.5 -60 0.5 0 0')
send('ult 100'); time.sleep(1)
tap('ultimate', 0.15)
time.sleep(1.3)
d = state()
cmd(f'/summon husk 0.5 {d["y"]:.2f} 12.5 ' + HUSK % ('u9', ',NoGravity:1b'))
time.sleep(0.3)
cmd(f'/execute as @s at @s anchored eyes facing 0.5 {d["y"] + 1.0:.2f} 12.5 run tp @s ~ ~ ~ ~ ~')
time.sleep(0.2)
tap('secondary_fire')
time.sleep(1.5)
hp('@e[tag=u9,limit=1]')
time.sleep(6)

stamp('MARK FX2b marks, then 40 OW of /damage on one: the ball\'s own 55 doesn\'t count, no Sunstruck (old: it went off, -12.8)')
cmd('/kill @e[type=!player]')
cmd('/tp @s 0.5 -60 0.5 0 0')
cmd('/summon husk 0.5 -60 8.5 ' + HUSK % ('u1', ''))
send('ult 100'); time.sleep(1)
tap('ultimate', 0.15)
time.sleep(1.3)
cmd('/execute as @s at @s anchored eyes facing 0.5 -58.8 8.5 run tp @s ~ ~ ~ ~ ~')
time.sleep(0.2)
tap('secondary_fire')
time.sleep(1.5)
hp('@e[tag=u1,limit=1]')
cmd('/damage @e[tag=u1,limit=1] 3.2 minecraft:generic')
time.sleep(1.5)
hp('@e[tag=u1,limit=1]')
stamp('MARK FX2c then a full rifle shot (75 more: 115 >= 90): Sunstruck goes off (-12.8)')
time.sleep(0.5)
cmd('/execute as @s at @s anchored eyes facing entity @e[tag=u1,limit=1] eyes run tp @s ~ ~ ~ ~ ~')
time.sleep(1.0)
cmd('/execute as @s at @s anchored eyes facing entity @e[tag=u1,limit=1] eyes run tp @s ~ ~ ~ ~ ~')
time.sleep(0.1)
tap('secondary_fire')
time.sleep(1.5)
hp('@e[tag=u1,limit=1]')
time.sleep(5)
state()

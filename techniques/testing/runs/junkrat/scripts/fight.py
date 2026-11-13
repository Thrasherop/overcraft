"""A night fight as Junkrat over the harness (Cassidy's fight.py retargeted): every 0.25 s aim at the nearest hostile (client copies
via 'ents'), a little above it for the frag's arc, hold fire (Frag Launcher) while it's 3-25 m off; a Steel Trap at one within 8 m;
a Concussion Mine at one within 5 m, set off 0.4 s later (a mine jump when it's on him); RIP-Tire when the ult is full and two or
more are within 20 m, driven at the nearest for up to 3 s and set off. Logs a line every 2 s and every death into runs.txt.
Usage: py -I fight.py <seconds> <label>"""
import datetime
import json
import math
import os
import socket
import sys
import time

RUNS = os.environ.get('OC_RUNS', 'E:/claude/mods/overcraft_junkrat/techniques/testing/runs/junkrat/runs.txt')
secs, label = float(sys.argv[1]), sys.argv[2]
out = open(RUNS, 'a', encoding='utf8')
sock = socket.create_connection(('127.0.0.1', 25599), timeout=60)
f = sock.makefile('r', encoding='utf8')


def send(c):
    sock.sendall((c + '\n').encode('utf8'))
    return json.loads(f.readline())


def log(t):
    s = datetime.datetime.now().strftime('%H:%M:%S.%f')[:-3] + ' ' + t
    out.write(s + '\n'); out.flush(); print(s)


def tap(key, hold=0.1):
    send(f'key {key} down'); time.sleep(hold); send(f'key {key} up')


def hostiles():
    mobs = []
    for name in HOSTILE:
        for part in send('ents ' + name).get('ents', '').split('|'):
            p = part.split()
            if len(p) >= 4:
                mobs.append((name, float(p[1]), float(p[2]), float(p[3])))
    return mobs


def aim(px, py, pz, m, lift):
    dx, dz, dy = m[1] - px, m[3] - pz, m[2] + lift - py
    dist = math.hypot(dx, dz)
    send(f'look {math.degrees(math.atan2(-dx, dz)):.1f} {-math.degrees(math.atan2(dy, max(dist, 0.1))):.1f}')
    return dist


out.write(f'\n=== {label} {datetime.datetime.now():%Y-%m-%d %H:%M:%S}\n')
HOSTILE = ['Zombie', 'Skeleton', 'Spider', 'Creeper', 'Husk', 'Drowned', 'Stray', 'Witch', 'Slime', 'Bogged', 'Parched', 'Enderman']
start = time.time()
firing = False
last_log = 0
deaths = mines = traps = tires = jumps = 0
was_dead = False
while time.time() - start < secs:
    st = send('state')
    if st.get('screen') == 'DeathScreen' or st.get('health', 1) <= 0:
        if not was_dead:
            deaths += 1
            log(f'DEATH {deaths} at {st.get("x"):.1f} {st.get("z"):.1f}')
            if firing:
                send('key secondary_fire up'); firing = False
            time.sleep(2.0)
            send('respawn')
            time.sleep(1.5)
            log('respawned: ' + json.dumps({k: st.get(k) for k in ('hero', 'heroMode')}))
        was_dead = True
        continue
    was_dead = False
    mobs = hostiles()
    px, py, pz = st['x'], st['y'] + 1.62, st['z']
    mobs.sort(key=lambda m: (m[1] - px) ** 2 + (m[3] - pz) ** 2)
    if mobs:
        m = mobs[0]
        dist = math.hypot(m[1] - px, m[3] - pz)
        # the frag's arc: aim higher the farther it is (ours, by the 30 m/s and 17 m/s² drop)
        dist = aim(px, py, pz, m, 1.0 + dist * dist * 17 / (2 * 900))
        near20 = sum(1 for x in mobs if math.hypot(x[1] - px, x[3] - pz) < 20)
        if st.get('ult', 0) >= st.get('ultCost', 9999) and near20 >= 2:
            if firing:
                send('key secondary_fire up'); firing = False
            tap('ultimate')
            time.sleep(0.3)
            send('key move_forward down')
            t0 = time.time()
            while time.time() - t0 < 3:
                tires_seen = send('ents RipTire').get('ents', '')
                p = tires_seen.split('|')[0].split()
                ms = hostiles()
                if len(p) >= 4 and ms:
                    tx, tz = float(p[1]), float(p[3])
                    ms.sort(key=lambda x: (x[1] - tx) ** 2 + (x[3] - tz) ** 2)
                    send(f'look {math.degrees(math.atan2(-(ms[0][1] - tx), ms[0][3] - tz)):.1f} 10')
                    if math.hypot(ms[0][1] - tx, ms[0][3] - tz) < 2.0 and time.time() - t0 > 1.7:
                        break
                time.sleep(0.15)
            send('key move_forward up')
            time.sleep(max(0, 1.8 - (time.time() - t0)))
            tap('secondary_fire', 0.15)
            tires += 1
            log(f'RIP-TIRE with {near20} within 20 m, driven {time.time() - t0:.1f} s')
            time.sleep(1.0)
            continue
        if 3 < dist < 25 and not firing:
            send('key secondary_fire down'); firing = True
        elif (dist <= 3 or dist >= 25) and firing:
            send('key secondary_fire up'); firing = False
        if dist < 8 and st.get('ability2Cooldown', 1) <= 0:
            tap('ability_2')
            traps += 1
        if dist < 5 and st.get('secondaryAmmo', 0) > 0:
            tap('ability_1')
            time.sleep(0.4)
            tap('primary_fire', 0.15)
            mines += 1
            if dist < 3.5:
                jumps += 1
    elif firing:
        send('key secondary_fire up'); firing = False
    if time.time() - last_log > 2:
        last_log = time.time()
        near = ', '.join(f'{x[0]} {math.hypot(x[1]-px, x[3]-pz):.1f}' for x in mobs[:4])
        log(f't {time.time()-start:.0f} hp {st["health"]:.2f} ammo {st.get("primaryAmmo")} mines {st.get("secondaryAmmo")} '
            f'trapCd {st.get("ability2Cooldown", 0):.1f} ult {st.get("ult", 0):.0f} | {near}')
    time.sleep(0.25)
if firing:
    send('key secondary_fire up')
log(f'END: {deaths} deaths, {mines} mines ({jumps} close), {traps} traps, {tires} tires')

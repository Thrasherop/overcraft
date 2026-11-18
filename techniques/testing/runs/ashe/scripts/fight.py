"""A night fight as Ashe over the harness (Illari's fight.py retargeted): every 0.25 s aim at the nearest hostile's chest, hold the
left button (The Viper from the hip) while it's 2-40 m off; Coach Gun when one is within 3 m; Dynamite at the nearest's feet when
it's 5-20 m off; B.O.B. when the ult is full and two or more are within 20 m. Logs a line every 2 s and every death into runs.txt. Usage: py -I fight.py <seconds> <label>"""
import datetime
import json
import math
import os
import socket
import sys
import time

RUNS = os.environ.get('OC_RUNS', 'E:/claude/mods/overcraft_ashe/techniques/testing/runs/ashe/runs.txt')
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


out.write('\n=== ' + label + ' ' + datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S') + '\n')
HOSTILE = ['Zombie', 'Skeleton', 'Spider', 'Creeper', 'Husk', 'Drowned', 'Stray', 'Witch', 'Slime', 'Bogged', 'Parched', 'Enderman']
start = time.time()
firing = False
last_log = 0
was_dead = False
deaths = coaches = sticks = bobs = 0
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
            time.sleep(2.5)
            sc = send('state')
            if sc.get('screen') == 'HeroSelectScreen':
                send('press key.keyboard.enter')
                time.sleep(1)
            log('respawned: ' + json.dumps({k: send('state').get(k) for k in ('hero', 'heroMode', 'screen')}))
        was_dead = True
        continue
    was_dead = False
    mobs = hostiles()
    px, py, pz = st['x'], st['y'] + 1.62, st['z']
    mobs.sort(key=lambda m: (m[1] - px) ** 2 + (m[3] - pz) ** 2)
    if mobs:
        m = mobs[0]
        dist = aim(px, py, pz, m, 0.9)
        near20 = sum(1 for x in mobs if math.hypot(x[1] - px, x[3] - pz) < 20)
        if st.get('ult', 0) >= 2450 and near20 >= 2:
            if firing:
                send('key secondary_fire up'); firing = False
            tap('ultimate')
            bobs += 1
            log(f'BOB with {near20} within 20 m')
            time.sleep(0.6)
            continue
        if 5 < dist < 20 and st.get('ability2Cooldown', 1) <= 0:
            aim(px, py, pz, m, 0.0)
            tap('ability_2', 0.1)
            sticks += 1
            log(f'DYNAMITE at {m[0]} {dist:.1f}')
            time.sleep(0.4)
        if 2 < dist < 40 and not firing:
            send('key secondary_fire down'); firing = True
        elif (dist <= 2 or dist >= 40) and firing:
            send('key secondary_fire up'); firing = False
        if dist < 3 and st.get('ability1Cooldown', 1) <= 0:
            tap('ability_1', 0.1)
            coaches += 1
            log(f'COACH GUN at {m[0]} {dist:.1f}')
    elif firing:
        send('key secondary_fire up'); firing = False
    if time.time() - last_log > 2:
        last_log = time.time()
        near = ', '.join(f'{x[0]} {math.hypot(x[1]-px, x[3]-pz):.1f}' for x in mobs[:4])
        log(f't {time.time()-start:.0f} hp {st["health"]:.2f} ammo {st.get("primaryAmmo")} mode {st.get("mode")} '
            f'a1 {st.get("ability1Cooldown", 0):.1f} a2 {st.get("ability2Cooldown", 0):.1f} ult {st.get("ult", 0):.0f} | {near}')
    time.sleep(0.25)
if firing:
    send('key secondary_fire up')
log(f'END: {deaths} deaths, {coaches} coach guns, {sticks} sticks, {bobs} B.O.B.s')

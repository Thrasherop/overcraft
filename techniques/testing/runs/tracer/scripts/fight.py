"""A night fight as Tracer over the harness: every 0.25 s aim at the nearest hostile (client copies via 'ents'), hold fire,
Blink sideways when one is within 3 m, Recall below half health, Pulse Bomb at a close one when the ult is full.
Logs a line a second and every death / kill change into runs.txt. Usage: py -I fight.py <seconds> <label>"""
import datetime
import json
import math
import socket
import sys
import time

RUNS = 'E:/claude/mods/overcraft_tracer/techniques/testing/runs/tracer/runs.txt'
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


out.write(f'\n=== {label} {datetime.datetime.now():%Y-%m-%d %H:%M:%S}\n')
HOSTILE = ['Zombie', 'Skeleton', 'Spider', 'Creeper', 'Husk', 'Drowned', 'Stray', 'Witch', 'Slime', 'Bogged', 'Parched']
start = time.time()
firing = False
last_log = 0
deaths = 0
was_dead = False
blinks = 0
recalls = 0
bombs = 0
while time.time() - start < secs:
    st = send('state')
    if st.get('screen') == 'DeathScreen' or st.get('health', 1) <= 0:
        if not was_dead:
            deaths += 1
            log(f'DEATH {deaths} at {st.get("x"):.1f} {st.get("z"):.1f}')
            if firing:
                send('key secondary_fire up'); firing = False
            time.sleep(1.5)
            send('respawn')
            time.sleep(1.5)
            log('respawned: ' + json.dumps({k: st.get(k) for k in ('hero', 'heroMode')}))
        was_dead = True
        continue
    was_dead = False
    mobs = []
    for name in HOSTILE:
        ents = send('ents ' + name).get('ents', '')
        for part in ents.split('|'):
            p = part.split()
            if len(p) >= 4:
                mobs.append((name, float(p[1]), float(p[2]), float(p[3])))
    px, py, pz = st['x'], st['y'] + 1.62, st['z']
    mobs.sort(key=lambda m: (m[1] - px) ** 2 + (m[3] - pz) ** 2)
    if mobs:
        name, mx, my, mz = mobs[0]
        dx, dz, dy = mx - px, mz - pz, my + 1.0 - py
        dist = math.hypot(dx, dz)
        yaw = math.degrees(math.atan2(-dx, dz))
        pitch = -math.degrees(math.atan2(dy, max(dist, 0.1)))
        send(f'look {yaw:.1f} {pitch:.1f}')
        if dist < 40 and not firing:
            send('key secondary_fire down'); firing = True
        if dist < 3 and st.get('secondaryAmmo', 0) > 0:
            send('key left down'); send('key ability_1 down'); time.sleep(0.1); send('key ability_1 up'); send('key left up')
            blinks += 1
        if st.get('ult', 0) >= st.get('ultCost', 9999) and dist < 6:
            send(f'look {yaw:.1f} {pitch - 15:.1f}')
            send('key ultimate down'); time.sleep(0.1); send('key ultimate up')
            bombs += 1
            log(f'PULSE BOMB at {name} {dist:.1f} m')
    elif firing:
        send('key secondary_fire up'); firing = False
    if st['health'] < st.get('maxHealth', 14) * 0.5 and st.get('ability2Cooldown', 1) <= 0:
        send('key ability_2 down'); time.sleep(0.05); send('key ability_2 up')
        recalls += 1
        log(f'RECALL at health {st["health"]:.2f}')
    if time.time() - last_log > 2:
        last_log = time.time()
        near = ', '.join(f'{m[0]} {math.hypot(m[1]-px, m[3]-pz):.1f}' for m in mobs[:4])
        log(f't {time.time()-start:.0f} hp {st["health"]:.2f} ammo {st.get("primaryAmmo")} blinks {st.get("secondaryAmmo")} '
            f'ult {st.get("ult", 0):.0f} recallCd {st.get("ability2Cooldown", 0):.1f} | {near}')
    time.sleep(0.25)
if firing:
    send('key secondary_fire up')
log(f'END: {deaths} deaths, {blinks} blinks, {recalls} recalls, {bombs} bombs')

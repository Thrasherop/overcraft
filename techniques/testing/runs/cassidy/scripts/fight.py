"""A night fight as Cassidy over the harness (Tracer's fight.py retargeted): every 0.25 s aim at the nearest hostile (client
copies via 'ents'), hold fire (Peacekeeper), fan the hammer (right button) when one is within 4 m, Combat Roll sideways when
one is within 2.5 m, Flashbang at one within 6 m, Deadeye when the ult is full and two or more are within 25 m (fired 2.2 s
in). Logs a line every 2 s and every death into runs.txt. Usage: py -I fight.py <seconds> <label>"""
import datetime
import json
import math
import socket
import sys
import time

RUNS = 'E:/claude/mods/overcraft_cassidy/techniques/testing/runs/cassidy/runs.txt'
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


out.write(f'\n=== {label} {datetime.datetime.now():%Y-%m-%d %H:%M:%S}\n')
HOSTILE = ['Zombie', 'Skeleton', 'Spider', 'Creeper', 'Husk', 'Drowned', 'Stray', 'Witch', 'Slime', 'Bogged', 'Parched']
start = time.time()
firing = False
last_log = 0
deaths = rolls = fans = flashes = deadeyes = 0
was_dead = False
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
        dx, dz, dy = mx - px, mz - pz, my + 1.2 - py
        dist = math.hypot(dx, dz)
        yaw = math.degrees(math.atan2(-dx, dz))
        pitch = -math.degrees(math.atan2(dy, max(dist, 0.1)))
        send(f'look {yaw:.1f} {pitch:.1f}')
        near25 = sum(1 for m in mobs if math.hypot(m[1] - px, m[3] - pz) < 25)
        if st.get('ult', 0) >= st.get('ultCost', 9999) and near25 >= 2:
            if firing:
                send('key secondary_fire up'); firing = False
            tap('ultimate')
            time.sleep(2.2)
            tap('secondary_fire', 0.15)
            deadeyes += 1
            log(f'DEADEYE with {near25} within 25 m')
            time.sleep(1.0)
            continue
        if dist < 40 and not firing:
            send('key secondary_fire down'); firing = True
        if dist < 4 and st.get('primaryAmmo', 0) >= 3:
            tap('primary_fire', 0.15)
            fans += 1
        if dist < 2.5 and st.get('ability1Cooldown', 1) <= 0:
            send('key left down'); tap('ability_1'); send('key left up')
            rolls += 1
        if dist < 6 and st.get('ability2Cooldown', 1) <= 0:
            tap('ability_2')
            flashes += 1
    elif firing:
        send('key secondary_fire up'); firing = False
    if time.time() - last_log > 2:
        last_log = time.time()
        near = ', '.join(f'{m[0]} {math.hypot(m[1]-px, m[3]-pz):.1f}' for m in mobs[:4])
        log(f't {time.time()-start:.0f} hp {st["health"]:.2f} ammo {st.get("primaryAmmo")} ult {st.get("ult", 0):.0f} '
            f'rollCd {st.get("ability1Cooldown", 0):.1f} flashCd {st.get("ability2Cooldown", 0):.1f} | {near}')
    time.sleep(0.25)
if firing:
    send('key secondary_fire up')
log(f'END: {deaths} deaths, {rolls} rolls, {fans} fans, {flashes} flashbangs, {deadeyes} deadeyes')

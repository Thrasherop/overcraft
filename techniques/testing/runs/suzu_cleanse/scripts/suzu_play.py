# Suzu cleanse play (#119 follow-up: Flashbang's hinder, Steel Trap's root). Real Kiriko (keys through KeyMapping), bots for the others.
import json, socket, sys, time, math
OUT = open(sys.argv[2] if len(sys.argv) > 2 else 'suzu_play.txt', 'a', encoding='utf8')
def log(*a):
    s = ' '.join(str(x) for x in a); print(s); OUT.write(s + '\n'); OUT.flush()
sock = socket.create_connection(('127.0.0.1', 25599), timeout=40); F = sock.makefile('r', encoding='utf8')
def c(cmd):
    sock.sendall((cmd + '\n').encode('utf8')); return json.loads(F.readline())
def mc(cmd, w=0.1):
    r = c('cmd ' + cmd); time.sleep(w); return r
def bot(cmd): return c('bot ' + cmd)
def sleep(t): time.sleep(t)
def cc(n, keys=('hindered', 'rooted', 'asleep', 'antiHeal', 'phased', 'speed', 'health')):
    r = bot('cc ' + n); return ' '.join('%s=%s' % (k, round(r[k], 3) if isinstance(r.get(k), float) else r.get(k)) for k in keys)
def heard():
    h = c('heard').get('heard', ''); return [p.strip() for p in h.split(' | ') if 'kiriko' in p.lower() and 'suzu' in p.lower()]
NAMES = []
N = [int(time.time()) % 1000 * 10]
def add(b, hero, team, x, z, yaw, pitch=0):
    N[0] += 1; n = '%s%d' % (b, N[0]); r = bot('add %s %s %s %s -60 %s %s' % (n, hero, team, x, z, yaw))
    if 'error' in str(r): log('ADDERR', r)
    mc('/gamemode survival ' + n); bot('look %s %s %s' % (n, yaw, pitch)); NAMES.append(n); sleep(0.3); return n
def clear():
    for n in NAMES: bot('remove ' + n)
    NAMES.clear()
X = [0]
def spot():
    X[0] += 50; x = X[0]
    mc('/tp @s %d -55 0' % x, 0.6); mc('/fill %d -60 -20 %d -50 20 minecraft:air' % (x - 15, x + 15), 0.15); mc('/fill %d -61 -20 %d -61 20 minecraft:stone' % (x - 15, x + 15), 0.15)
    return x + 0.5
def kiriko(x, pitch=62):
    clear(); mc('/overcraft reset', 0.3); c('heromode on'); mc('/tp @s %s -60 0.5 0 %s' % (x, pitch), 0.2); c('look 0 %s' % pitch); sleep(0.3)
def suzu():
    c('key ability_2 down'); sleep(0.15); c('key ability_2 up')
def timeline(n, secs=1.2, dt=0.1):
    t0 = time.time()
    while time.time() - t0 < secs:
        sleep(dt); log('    +%.2f %s' % (time.time() - t0, cc(n)))

def hinder_case(on, who):
    x = spot(); kiriko(x, 62 if who == 'ally' else 89)
    ally = add('ally', 'mercy', 'red', x + 1, 3.5, 0) if who == 'ally' else 'me'
    tz = 3.5 if who == 'ally' else 0.5; tx = x + 1 if who == 'ally' else x
    cas = add('casE', 'cassidy', 'blue', tx, tz + 6, 180, 12)
    log('--- Flashbang hinder on %s, fix 119 %s' % (who, 'on' if on else 'off'), bot('fix 119 %s' % ('on' if on else 'off')))
    heard(); bot('press %s ability2' % cas); t0 = time.time()
    while not bot('cc ' + ally).get('hindered') and time.time() - t0 < 1.5: sleep(0.02)
    log('    hindered at +%.2f: %s' % (time.time() - t0, cc(ally)))
    suzu(); log('    Suzu thrown (real key ability_2)')
    timeline(ally, 1.3)
    sleep(0.6); log('    Kiriko Suzu line heard:', heard())
    c('screenshot suzu_hinder_%s_%s.png' % (who, 'on' if on else 'off'))

def trap_case(on):
    x = spot(); kiriko(x)
    ally = add('ally', 'mercy', 'red', x + 1, 9, 0)
    jr = add('junE', 'junkrat', 'blue', x + 1, 7, 0, 60)
    log('--- Steel Trap root on ally, fix 119 %s' % ('on' if on else 'off'), bot('fix 119 %s' % ('on' if on else 'off')))
    bot('press %s ability2' % jr); sleep(1.6)
    bot('move %s %s -60 3.5' % (ally, x + 1)); bot('move %s %s -60 12' % (jr, x + 1)); t0 = time.time()
    while not bot('cc ' + ally).get('rooted') and time.time() - t0 < 2: sleep(0.02)
    log('    rooted at +%.2f: %s' % (time.time() - t0, cc(ally)))
    heard(); suzu(); log('    Suzu thrown (real key ability_2)')
    timeline(ally, 1.3)
    sleep(0.6); log('    Kiriko Suzu line heard:', heard())
    c('screenshot suzu_trap_%s.png' % ('on' if on else 'off'))

def prevent_flash():
    x = spot(); kiriko(x)
    ally = add('ally', 'mercy', 'red', x + 1, 3.5, 0); cas = add('casE', 'cassidy', 'blue', x + 1, 9.5, 180, 12)
    bot('fix 119 on'); log('--- Flashbang into the phase (Suzu first)')
    suzu(); t0 = time.time()
    while not bot('cc ' + ally).get('phased') and time.time() - t0 < 2: sleep(0.01)
    log('    phased at +%.2f' % (time.time() - t0)); bot('press %s ability2' % cas)
    timeline(ally, 1.0)

def prevent_trap():
    x = spot(); kiriko(x)
    ally = add('ally', 'mercy', 'red', x + 1, 9, 0); jr = add('junE', 'junkrat', 'blue', x + 1, 7, 0, 60)
    bot('fix 119 on'); log('--- Steel Trap stepped on while phased')
    bot('press %s ability2' % jr); sleep(1.6); bot('move %s %s -60 12' % (jr, x + 1))
    bot('move %s %s -60 5.5' % (ally, x + 1)); sleep(0.1)
    suzu(); t0 = time.time()
    while not bot('cc ' + ally).get('phased') and time.time() - t0 < 2: sleep(0.01)
    log('    phased at +%.2f' % (time.time() - t0)); bot('move %s %s -60 3.5' % (ally, x + 1))
    timeline(ally, 1.0)
    log('    phase over, still on the trap:'); timeline(ally, 0.6)

def sleep_case():
    x = spot(); kiriko(x)
    ally = add('ally', 'mercy', 'red', x + 1, 3.5, 0); ana = add('anaE', 'ana', 'blue', x + 1, 10.5, 180)
    bot('fix 119 on'); log('--- Sleep Dart on ally, then Suzu (regression)')
    heard(); bot('press %s ability1' % ana); sleep(0.8); log('    ', cc(ally))
    suzu(); timeline(ally, 1.2); sleep(0.5); log('    line:', heard())

def antiheal_case():
    x = spot(); kiriko(x)
    ally = add('ally', 'mercy', 'red', x + 1, 3.5, 0); ana = add('anaE', 'ana', 'blue', x + 1, 10.5, 180, 8)
    bot('fix 119 on'); log('--- Biotic Grenade anti-heal on ally, then Suzu (regression, #117)')
    mc('/damage %s 8' % ally, 0.3)
    heard(); bot('press %s ability2' % ana); sleep(0.9); log('    ', cc(ally))
    suzu(); timeline(ally, 1.0); sleep(0.5); log('    line:', heard())

log('=== suzu_play', time.strftime('%H:%M:%S'), sys.argv[1] if len(sys.argv) > 1 else '')
for on in (False, True):
    hinder_case(on, 'ally')
    sleep(10.5)  # her line is at most every 10 s (Voice): a fair listen each time
for on in (False, True):
    hinder_case(on, 'me')
    sleep(10.5)
for on in (False, True):
    trap_case(on)
    sleep(10.5)
prevent_flash(); prevent_trap(); sleep(10)
sleep_case(); sleep(10); antiheal_case()
clear(); log('=== done', time.strftime('%H:%M:%S'))

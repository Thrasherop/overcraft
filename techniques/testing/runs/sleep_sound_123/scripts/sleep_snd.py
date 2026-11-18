# #123: two sleepers, one wakes: does the other's bed (ana.sleep) play on? Real Ana (Sleep Dart by the real key binding),
# two husks as the sleepers, the listener is the client. "Off" = this build with "bot fix 123 off" (the old stop-everything).
import json, socket, sys, time
OUT = open(sys.argv[1] if len(sys.argv) > 1 else 'sleep_snd.txt', 'a', encoding='utf8')
def log(*a):
    s = ' '.join(str(x) for x in a); print(s); OUT.write(s + '\n'); OUT.flush()
sock = socket.create_connection(('127.0.0.1', 25599), timeout=40); F = sock.makefile('r', encoding='utf8')
def c(cmd):
    sock.sendall((cmd + '\n').encode('utf8')); return json.loads(F.readline())
def mc(cmd, w=0.15):
    r = c('cmd ' + cmd); time.sleep(w); return r
def beds():
    p = c('playing').get('playing', ''); return ' '.join(x for x in p.split() if 'ana.sleep' in x) or '-'
def heard():
    h = c('heard').get('heard', ''); return [x.strip() for x in h.split(' | ') if 'ana.sleep' in x]
X = [0]
def spot():
    X[0] += 60; x = X[0]
    mc('/tp @s %d -55 0' % x, 0.6); mc('/fill %d -60 -5 %d -50 20 minecraft:air' % (x - 12, x + 12)); mc('/fill %d -61 -5 %d -61 20 minecraft:stone' % (x - 12, x + 12))
    mc('/tp @s %d.5 -60 0.5 0 0' % x, 0.3); return x
def aim(tag):
    mc('/execute as @s at @s anchored eyes facing entity @e[tag=%s,limit=1] eyes run tp @s ~ ~ ~ ~ ~' % tag, 0.3)
def dart(tag):
    mc('/overcraft reset', 0.2); aim(tag); c('key ability_1 tap'); time.sleep(1.3)
def case(name, on, two=True):
    x = spot(); mc('/kill @e[type=husk]', 0.2)
    log('--- %s, fix 123 %s' % (name, 'on' if on else 'off'), c('bot fix 123 %s' % ('on' if on else 'off')))
    mc('/summon husk %d.5 -60 8.5 {NoAI:1b,PersistenceRequired:1b,Tags:["a"]}' % (x - 3))
    if two:
        mc('/summon husk %d.5 -60 8.5 {NoAI:1b,PersistenceRequired:1b,Tags:["b"]}' % (x + 3))
    time.sleep(0.5); heard()
    dart('a'); log('  dart at A (real key ability_1); beds playing:', beds(), 'heard:', heard())
    if two:
        dart('b'); log('  dart at B; beds playing:', beds(), 'heard:', heard())
    mc('/damage @e[tag=a,limit=1] 1 minecraft:generic', 0); t0 = time.time(); log('  A damaged (wakes 0.5 s later)')
    while time.time() - t0 < 6.5:
        time.sleep(0.25); log('    +%.2f beds playing: %s' % (time.time() - t0, beds()))
    log('  heard:', heard())
    c('screenshot sleep123_%s_%s.png' % (name, 'on' if on else 'off'))

log('=== sleep_snd', time.strftime('%H:%M:%S'))
c('heard'); c('subtitles on')
mc('/time set midnight'); mc('/gamerule spawn_monsters false'); mc('/gamemode survival @s')
case('one sleeper', True, two=False)
case('two sleepers', False)
case('two sleepers', True)
mc('/kill @e[type=husk]'); log('=== done', time.strftime('%H:%M:%S'))

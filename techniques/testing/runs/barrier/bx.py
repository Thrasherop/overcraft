# Barrier sweep (#104, #83): harness keys (KeyMapping.set), aim by /tp facing, bots as the enemy team. py -I bx.py <tag> [scenario...]
import json, socket, sys, time
TAG = sys.argv[1]
WANT = sys.argv[2:]
OUT = open(r"C:\Users\ultra\AppData\Local\Temp\claude\14452f9945c49089\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad\bx_%s.txt" % TAG, "a", encoding="utf8")
sock = socket.create_connection(("127.0.0.1", 25599), timeout=40)
f = sock.makefile("r", encoding="utf8")

def c(cmd):
    sock.sendall((cmd + "\n").encode("utf8"))
    return json.loads(f.readline())

def log(*a):
    s = " ".join(str(x) for x in a)
    print(s); OUT.write(s + "\n"); OUT.flush()

def mc(cmd, wait=0.15):
    c("cmd " + cmd); time.sleep(wait)

def tap(key, hold=0.08):
    c("key %s down" % key); time.sleep(hold); c("key %s up" % key)

def rd(name):
    r = c("bot read " + name)
    return "health %.2f barrier %s up %s asleep %s" % (r.get("health", -1), round(r.get("barrierHealth", -1), 1), r.get("barrierUp"), r.get("asleep"))

def cc(name):
    r = c("bot cc " + name)
    return "health %.2f hindered %s asleep %s x %.2f y %.2f z %.2f" % (r["health"], r.get("hindered"), r["asleep"], r["x"], r["y"], r["z"])

def shot(name):
    c("screenshot bx_%s_%s.png" % (TAG, name))

Y = -60
def setup(hero, brig=(0.5, 6.5, 180), vic=(1.1, 7.3)):
    for n in ("brig", "vic"):
        c("bot remove " + n)
    mc("/kill @e[type=!player]", 0.3)
    c("hero " + hero); time.sleep(0.6); c("heromode on"); time.sleep(0.3); mc("/overcraft reset", 0.3)
    c("bot add brig brigitte blue %s %s %s %s" % (brig[0], Y, brig[1], brig[2])); time.sleep(0.3)
    c("bot secondary brig on")
    if vic:
        c("bot add vic ana blue %s %s %s 180" % (vic[0], Y, vic[1]))
    time.sleep(1.2)
    c("bot set brig 20 300")
    if vic:
        c("bot set vic 20 0")

def world():
    c("newworld bx_%s_%d" % (TAG, int(time.time()) % 100000)); time.sleep(14)
    mc("/gamerule spawn_monsters false"); mc("/gamerule advance_time false"); mc("/time set noon")
    mc("/overcraft team join blue"); mc("/overcraft team join red", 0.5)

def aim(x, y, z, me=(0.5, 0.5)):
    # from the eyes (a /tp ... facing aims from the feet)
    import math
    dx, dy, dz = x - me[0], y - (Y + 1.62), z - me[1]
    yaw = -math.degrees(math.atan2(dx, dz)); pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    mc("/tp @s %s %s %s %.3f %.3f" % (me[0], Y, me[1], yaw, pitch), 0.4)
    c("focus"); time.sleep(0.3)

if "world" in WANT:
    world()

if "dart" in WANT:
    # #104: Sleep Dart at Brigitte behind her up barrier, then at the bot behind it, then with it down (control)
    setup("ana")
    aim(0.5, Y + 1.0, 6.5); log("dart: before", rd("brig")); tap("ability_1"); time.sleep(1.0)
    log("dart at her through it:", rd("brig")); shot("dart_her")
    time.sleep(6.5); c("bot set brig 20 300"); setup("ana")
    aim(1.1, Y + 0.9, 7.3); tap("ability_1"); time.sleep(1.0)
    log("dart at vic behind:", rd("brig"), "| vic", cc("vic"))
    time.sleep(6.5); setup("ana"); c("bot secondary brig off"); time.sleep(0.5)
    aim(0.5, Y + 1.0, 6.5); tap("ability_1"); time.sleep(1.0)
    log("dart, barrier down (control):", rd("brig"))

if "pistol" in WANT:
    # #83: Sombra's pistol at her, then at the bot behind
    setup("sombra")
    aim(0.5, Y + 1.0, 6.5); c("key secondary_fire down"); time.sleep(1.5); c("key secondary_fire up"); time.sleep(0.5)
    log("pistol at her 1.5 s:", rd("brig"))
    setup("sombra")
    aim(1.1, Y + 0.9, 7.3); c("key secondary_fire down"); time.sleep(1.5); c("key secondary_fire up"); time.sleep(0.5)
    log("pistol at vic behind 1.5 s:", rd("brig"), "| vic", cc("vic")); shot("pistol_vic")

if "flash" in WANT:
    setup("cassidy")
    aim(0.5, Y + 1.0, 6.5, me=(0.5, 3.0)); log("flash before: vic", cc("vic")); tap("ability_2"); time.sleep(0.6)
    log("flashbang on her barrier:", rd("brig"), "| vic", cc("vic")); shot("flash")

if "conc" in WANT:
    setup("pharah")
    aim(0.5, Y + 1.0, 6.5, me=(0.5, 1.5)); log("conc before: vic", cc("vic")); tap("ability_2"); time.sleep(1.2)
    log("concussive on her barrier:", rd("brig"), "| vic", cc("vic")); shot("conc")

if "disruptor" in WANT:
    setup("sojourn")
    aim(0.5, Y + 1.0, 6.5, me=(0.5, 1.5)); tap("ability_2"); time.sleep(4.5)
    log("disruptor on her barrier, 4.5 s:", rd("brig"), "| vic", cc("vic")); shot("disruptor")

if "boost" in WANT:
    setup("dva", vic=None)
    aim(0.5, Y + 1.0, 6.5, me=(0.5, 1.0)); log("boost before:", rd("brig")); c("key ability_1 down"); time.sleep(0.8); c("key ability_1 up"); time.sleep(0.5)
    log("boosters into her barrier:", rd("brig")); shot("boost")

if "numb" in WANT:
    # Local Anesthetic's damage over time, then a barrier between her and them
    setup("ana", brig=(3.5, 8.5, 90), vic=(0.5, 10.5)); c("bot perk me ANA_LOCAL_ANESTHETIC")
    aim(0.5, Y + 0.9, 10.5); tap("ability_1"); time.sleep(0.5)
    a = cc("vic"); aim(0.5, Y + 0.9, 10.5, me=(6.5, 6.5)); log("numb: 0.5 s after the dart:", a)
    time.sleep(2.0); log("numb: 2 s later, barrier between me and vic:", cc("vic"), "| brig", rd("brig"))
    c("bot perk me ANA_HEADHUNTER")

if "mine" in WANT:
    setup("junkrat", vic=None)
    aim(0.5, Y + 1.5, 6.5, me=(0.5, 2.5)); tap("ability_1"); time.sleep(0.15)
    for i in range(6):
        log("mine t%d:" % i, c("ents ConcussionMine").get("ents")); time.sleep(0.25)
    log("mine: brig", rd("brig")); shot("mine")

if "ally" in WANT:
    # an allied barrier: Brigitte on my team (red), the bot behind it my enemy: nothing of mine stops at it
    for n in ("brig", "vic"):
        c("bot remove " + n)
    c("hero sombra"); time.sleep(0.6); c("heromode on"); mc("/overcraft reset", 0.3)
    c("bot add brig brigitte red 0.5 %s 6.5 180" % Y); time.sleep(0.3); c("bot secondary brig on")
    c("bot add vic ana blue 1.1 %s 7.3 180" % Y); time.sleep(1.2); c("bot set brig 20 300"); c("bot set vic 20 0")
    aim(1.1, Y + 0.9, 7.3); c("key secondary_fire down"); time.sleep(1.0); c("key secondary_fire up"); time.sleep(0.4)
    log("pistol at an enemy behind an ALLIED barrier:", rd("brig"), "| vic", cc("vic"))
    setup("sojourn"); c("bot remove brig"); c("bot add brig brigitte red 0.5 %s 6.5 180" % Y); time.sleep(0.3); c("bot secondary brig on"); time.sleep(1); c("bot set vic 20 0")
    aim(0.5, Y + 1.0, 6.5, me=(0.5, 1.5)); tap("ability_2"); time.sleep(4.5)
    log("disruptor at an ALLIED barrier, 4.5 s:", rd("brig"), "| vic", cc("vic"))

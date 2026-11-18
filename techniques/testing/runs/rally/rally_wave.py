# #120 / #122: Rally's shockwave. Real client (Brigitte, team red), keys through DevServer's "key" (KeyMapping.set), bots as
# server-side players. Each case with the fix on, then off ("bot fix rally|wave off"), in one build.
# Run: py -I rally_wave.py <worktree>   (writes its lines to stdout)
import json, os, socket, sys, time

TREE = sys.argv[1] if len(sys.argv) > 1 else r"E:/claude/mods/overcraft_rally"
LOG = os.path.join(TREE, "run", "logs", "latest.log")
sock = socket.create_connection(("127.0.0.1", 25599), timeout=40)
f = sock.makefile("r", encoding="utf8")


def c(cmd):
    sock.sendall((cmd + "\n").encode("utf8"))
    return json.loads(f.readline())


def log(*a):
    print(*a, flush=True)


def chat_after(n):
    with open(LOG, encoding="utf8", errors="replace") as fh:
        lines = fh.readlines()
    return [l for l in lines[n:] if "[CHAT]" in l]


def loglen():
    with open(LOG, encoding="utf8", errors="replace") as fh:
        return len(fh.readlines())


def mc(cmd, wait=0.25):
    n = loglen(); c("cmd " + cmd); time.sleep(wait); return chat_after(n)


def hp(sel):
    out = mc("/data get entity %s Health" % sel, 0.4)
    for l in out:
        if "entity data" in l:
            return float(l.rsplit(":", 1)[1].strip().rstrip("f"))
    return None


def bot(cmd):
    return c("bot " + cmd)


XX = [20000]


def spot():
    XX[0] += 60; x = XX[0]
    mc("/tp @s %d -55 0" % x, 0.1); time.sleep(0.8)
    mc("/fill %d -60 -15 %d -50 15 minecraft:air" % (x - 15, x + 15), 0.2)
    mc("/fill %d -61 -15 %d -61 15 minecraft:stone" % (x - 15, x + 15), 0.2)
    mc("/kill @e[type=minecraft:zombie]", 0.2)
    return x + 0.5


def zombie(x, z, tag):
    mc('/summon minecraft:zombie %s -60 %s {NoAI:1b,Tags:["%s"],Health:20f,PersistenceRequired:1b,'
       'active_effects:[{id:"minecraft:fire_resistance",duration:99999,show_particles:0b}]}' % (x, z, tag), 0.15)


NN = [int(time.time()) % 10000]


def add(hero, team, x, z, yaw):
    NN[0] += 1; n = "b%d" % NN[0]
    bot("add %s %s %s %s -60 %s %s" % (n, hero, team, x, z, yaw)); mc("/gamemode survival " + n, 0.15)
    bot("look %s %s 0" % (n, yaw)); time.sleep(0.3); return n


def rally():
    """Q with the charge full. The last case's Rally (10 s) may still run: the charge can't be filled then, and the bash would
    land as it ends (harness trap found here: a wave that never fired). So wait until the charge takes, then press."""
    mc("/overcraft reset", 0.4)
    for _ in range(20):
        mc("/overcraft ult @s 100", 0.3)
        if bot("cc me").get("ult", 0) >= 2700:
            break
        time.sleep(0.7)
    else:
        log("  CHARGE NEVER FILLED")
    c("key ultimate tap"); time.sleep(0.3)
    left = bot("cc me").get("ult")
    if left:
        log("  RALLY DIDN'T START: ult", left)
    time.sleep(0.9)


def rally_bash(shot=None):
    """Q, the cast, the barrier held (right button), the bash (left button); reads after."""
    rally()
    c("key primary_fire down"); time.sleep(0.5)
    c("key secondary_fire down"); time.sleep(0.15); c("key secondary_fire up"); time.sleep(0.25)
    if shot:
        c("screenshot " + shot)
    time.sleep(0.6)
    c("key primary_fire up")


def me_at(x, z=0.5):
    mc("/tp @s %s -60 %s 0 0" % (x, z)); c("look 0 0"); time.sleep(0.3)


def setup():
    for k in ("/difficulty normal", "/gamerule spawn_monsters false", "/gamerule natural_health_regeneration false",
              "/time set noon", "/weather clear", "/team add red", "/team add blue", "/team join red @s",
              "/overcraft team join red", "/gamemode survival @s"):
        log(k, [l.split("[CHAT]")[1].strip() for l in mc(k)][-1:])
    c("hero brigitte"); time.sleep(0.8); c("close"); c("heromode on"); time.sleep(0.5)
    for _ in range(10):
        if not c("state").get("screen"):
            break
        c("close"); time.sleep(0.4)
    s = c("state"); log("hero", s.get("hero"), "mode", s.get("heroMode"), "screen", s.get("screen"), "hp", s.get("health"))
    if not s.get("heroMode"):
        log("NOT IN HERO MODE: stop"); sys.exit(2)


def case_open_and_wall(fix):
    """#120 and #122's wall: a bashed zombie at 2.2 m; one behind a stone wall 6 m ahead; a stone wall 3 m wide at 3.5 m; one in the open 4.5 m to the side, 4 m ahead, its line clear of the wall."""
    x = spot(); me_at(x)
    mc("/fill %d -60 4 %d -57 4 minecraft:stone" % (x - 1.5, x + 0.5), 0.2)
    zombie(x, 2.7, "a"); zombie(x, 6.5, "w"); zombie(x + 4.5, 4.5, "o")
    time.sleep(0.3)
    rally_bash("rw_%s_wall.png" % fix)
    time.sleep(0.3)
    log("  fix", fix, "| bashed", hp("@e[tag=a,limit=1]"), "| behind the wall", hp("@e[tag=w,limit=1]"), "| in the open", hp("@e[tag=o,limit=1]"))


def case_barrier(fix, up):
    """#122's barrier: a blue bot Brigitte facing the wave 3.8 m past the bashed zombie, barrier up or down; a zombie behind her."""
    x = spot(); me_at(x)
    zombie(x, 2.7, "a"); zombie(x - 0.6, 7.6, "b")
    d = add("brigitte", "blue", x, 6.5, 180)
    if up:
        bot("secondary %s on" % d); time.sleep(0.6)
    h0 = bot("cc " + d).get("health"); r0 = bot("read " + d)
    rally_bash("rw_%s_barrier_%s.png" % (fix, "up" if up else "down"))
    stunned = False
    t0 = time.time()
    # the stun is 0.75 s from 0.15 s after the bash: the read above was 0.25 s after the press
    cc = bot("cc " + d); stunned = bool(cc.get("asleep"))
    r1 = bot("read " + d)
    log("  fix", fix, "barrier", up, "| bashed", hp("@e[tag=a,limit=1]"), "| bot", h0, "->", cc.get("health"), "stunned(read late)", stunned,
        "| her barrier", r0.get("barrierHealth"), "->", r1.get("barrierHealth"), "up", r1.get("barrierUp"), "| zombie behind her", hp("@e[tag=b,limit=1]"))
    bot("secondary %s off" % d); bot("remove " + d)


def stun_watch(fix, up):
    """The bot's stun read every call from the press: 'asleep' is Sleep's flag, the stun's lock."""
    x = spot(); me_at(x)
    zombie(x, 2.7, "a")
    d = add("brigitte", "blue", x, 6.5, 180)
    if up:
        bot("secondary %s on" % d); time.sleep(0.6)
    rally()
    c("key primary_fire down"); time.sleep(0.5)
    c("key secondary_fire down"); t0 = time.time(); on = []
    while time.time() - t0 < 1.3:
        q = bot("cc " + d)
        if q.get("asleep"):
            on.append(q.get("now"))
    c("key secondary_fire up"); c("key primary_fire up")
    log("  fix", fix, "barrier", up, "| bot stunned ticks", (on[0], on[-1], on[-1] - on[0] + 1) if on else None, "| bot health", bot("cc " + d).get("health"))
    bot("secondary %s off" % d); bot("remove " + d)


if len(sys.argv) > 2 and sys.argv[2] == "diag":
    setup(); bot("fix rally on"); bot("fix wave on")
    for up in (False, False, False):
        x = spot(); me_at(x)
        zombie(x, 2.7, "a")
        d = add("brigitte", "blue", x, 6.5, 180)
        mc("/overcraft reset"); mc("/overcraft ult @s 100"); u0 = bot("cc me").get("ult")
        c("key ultimate tap"); time.sleep(0.3); u1 = bot("cc me").get("ult"); time.sleep(0.9)
        c("key primary_fire down"); time.sleep(0.5); p0 = c("state"); c("key secondary_fire down"); time.sleep(0.15); c("key secondary_fire up"); time.sleep(0.6)
        p1 = c("state"); c("key primary_fire up")
        log("ult", u0, "->", u1, "| me z", p0.get("z"), "->", p1.get("z"), "yaw", p1.get("yaw"), "| bashed", hp("@e[tag=a,limit=1]"), "| bot", bot("cc " + d).get("health"), bot("read " + d))
        bot("remove " + d)
    sys.exit(0)

if len(sys.argv) > 2 and sys.argv[2] == "wall":
    setup()
    for fix in ("on", "off", "on", "off", "on"):
        bot("fix rally " + fix); bot("fix wave " + fix); case_open_and_wall(fix)
    bot("fix rally on"); bot("fix wave on"); sys.exit(0)

if len(sys.argv) > 2 and sys.argv[2] == "diag2":
    setup(); bot("fix rally on"); bot("fix wave on")
    case_barrier("on", False); case_barrier("on", False); stun_watch("on", False)
    sys.exit(0)

setup()
log("TIME", time.strftime("%H:%M:%S"))
for fix in ("on", "off", "on"):
    bot("fix rally " + fix); bot("fix wave " + fix)
    log("--- fix", fix)
    case_open_and_wall(fix)
    case_barrier(fix, False)
    case_barrier(fix, True)
    stun_watch(fix, False)
    stun_watch(fix, True)
bot("fix rally on"); bot("fix wave on")
log("DONE")

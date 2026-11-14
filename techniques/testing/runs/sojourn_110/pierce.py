# #110: Charged Shot through husks in a line. Real client (Sojourn), the right button through DevServer's "key"
# (KeyMapping.set; the right button is the mapping named primary_fire), aim by /execute anchored eyes facing, health by /data get. Run: py -I pierce.py <worktree>
import json, os, socket, sys, time

TREE = sys.argv[1] if len(sys.argv) > 1 else r"E:/claude/mods/overcraft_sojhs"
LOG = os.path.join(TREE, "run", "logs", "latest.log")
sock = socket.create_connection(("127.0.0.1", 25599), timeout=40)
f = sock.makefile("r", encoding="utf8")


def c(cmd):
    sock.sendall((cmd + "\n").encode("utf8"))
    return json.loads(f.readline())


def log(*a):
    print(*a, flush=True)


def loglen():
    with open(LOG, encoding="utf8", errors="replace") as fh:
        return len(fh.readlines())


def mc(cmd, wait=0.25):
    n = loglen(); c("cmd " + cmd); time.sleep(wait)
    with open(LOG, encoding="utf8", errors="replace") as fh:
        return [l for l in fh.readlines()[n:] if "[CHAT]" in l]


def hp(tag):
    for l in mc("/data get entity @e[tag=%s,limit=1] Health" % tag, 0.4):
        if "entity data" in l:
            return float(l.rsplit(":", 1)[1].strip().rstrip("f"))
    return None


X = [3000]
HP = 1000


def spot(zs):
    """A fresh floor, the player at (x, -60, 0.5) facing +z, a NoAI husk at each z (armor 0, 1000 health)."""
    X[0] += 50; x = X[0] + 0.5
    mc("/tp @s %s -60 0.5 0 0" % x, 0.1); time.sleep(0.8)
    mc("/fill %d -60 -5 %d -50 30 minecraft:air" % (x - 6, x + 6), 0.15)
    mc("/fill %d -61 -5 %d -61 30 minecraft:stone" % (x - 6, x + 6), 0.15)
    mc("/kill @e[type=minecraft:husk]", 0.15)
    for i, z in enumerate(zs):
        mc('/summon minecraft:husk %s -60 %s {NoAI:1b,Tags:["h%d"],Rotation:[180f,0f],PersistenceRequired:1b,'
           'attributes:[{id:"minecraft:armor",base:0},{id:"minecraft:max_health",base:%d}],Health:%df}' % (x, z, i, HP, HP), 0.15)
    time.sleep(0.4)
    return x


def shot(name, zs, aim_z, aim_h, energy=100):
    x = spot(zs)
    mc("/execute as @s at @s anchored eyes facing %s %s %s run tp @s ~ ~ ~ ~ ~" % (x, -60 + aim_h, aim_z), 0.3)
    st = c("state")
    before = [hp("h%d" % i) for i in range(len(zs))]
    c("presstrace energy %d" % energy); time.sleep(0.05)
    c("key primary_fire tap"); time.sleep(0.6)
    after = [hp("h%d" % i) for i in range(len(zs))]
    # back to Overwatch HP (250 OW = 20 MC)
    ow = [None if b is None or a is None else round((b - a) * 12.5, 1) for b, a in zip(before, after)]
    log("| %s | pitch %.2f | %s |" % (name, st.get("pitch", 0), " / ".join(str(o) for o in ow)))
    return ow


log("| case | aim | OW damage per husk, front to back |")
log("|---|---|---|")
# one husk, the control: body (chest) and head
shot("control: one husk, chest", [5.5], 5.5, 1.2)
shot("control: one husk, head", [5.5], 5.5, 1.75)
L3 = [4.5, 9.5, 17.0]
shot("three husks, level at chest (1.2)", L3, 17.0, 1.2)
shot("three husks, level at head (1.75)", L3, 17.0, 1.75)
shot("three husks, front's chest on to back's legs (0.3)", L3, 17.0, 0.3)
shot("three husks, front's legs on up to back's head (1.75)", L3, 17.0, 1.85, )
shot("issue: two husks 12.5 m apart, chest on to legs", [4.5, 17.0], 17.0, 0.4)
for h in (0.2, 0.6, 1.0, 1.3, 1.45, 1.55, 1.7, 1.9):
    shot("two husks 12.5 m apart, back aimed at %.2f" % h, [4.5, 17.0], 17.0, h)
log("### done")

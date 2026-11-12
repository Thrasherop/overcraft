"""POTG replay backend run driver (Refs #44): harness sockets to both dev clients, answers recorded.
Usage: py -I drive.py <phase> [args]   (client 1 on 25599, client 2 on 25600)"""
import json, os, socket, sys, time

ROOT = r"E:\claude\mods\overcraft_potgb"
OUT = os.path.dirname(os.path.abspath(__file__))
LOGS = {25599: os.path.join(ROOT, "run", "logs", "latest.log"), 25600: os.path.join(ROOT, "run2", "logs", "latest.log")}
rec = open(os.path.join(OUT, sys.argv[1] + "_driver.txt"), "a", encoding="utf8")
socks = {}


def say(*a):
    line = " ".join(str(x) for x in a)
    print(line, flush=True)
    rec.write(line + "\n"); rec.flush()


def c(cmd, port=25599):
    if port not in socks:
        s = socket.create_connection(("127.0.0.1", port), timeout=60)
        socks[port] = (s, s.makefile("r", encoding="utf8"))
    s, f = socks[port]
    s.sendall((cmd + "\n").encode("utf8"))
    return json.loads(f.readline())


def st(port=25599):
    s = c("state", port)
    keys = ["screen", "inWorld", "x", "y", "z", "yaw", "pitch", "health", "hero", "heroMode", "ult", "primaryAmmo", "camera",
            "heroAttached", "food"]
    return {k: (round(s[k], 2) if isinstance(s.get(k), float) else s.get(k)) for k in keys}


def loglen(port=25599):
    with open(LOGS[port], encoding="utf8", errors="replace") as h:
        return len(h.readlines())


def potglines(n, port=25599):
    with open(LOGS[port], encoding="utf8", errors="replace") as h:
        return [x.rstrip() for x in h.readlines()[n:] if "[POTG]" in x or "Exception" in x or "ERROR" in x or "potg disk" in x]


def fight(seconds, target, port=25599):
    c("key secondary_fire down", port)
    t0 = time.time(); k = 0
    while time.time() - t0 < seconds:
        c(f"cmd /execute as @s at @s anchored eyes facing entity @e[type={target},sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~ ~", port)
        k += 1
        if k % 30 == 0:
            c("key reload tap", port)
        time.sleep(0.25)
    c("key secondary_fire up", port)


phase = sys.argv[1]
say("=== phase", phase, time.strftime("%H:%M:%S"))
if phase == "world":
    s = c("state")
    if s.get("screen") and "Accessibility" in s["screen"]:
        c("close"); time.sleep(1)
    c("newworld " + sys.argv[2])
    for i in range(90):
        time.sleep(2)
        s = c("state")
        if "HeroSelect" in (s.get("screen") or ""):
            break
    say("select", s.get("screen"))
    time.sleep(2)
    say(c("hero kiriko")); time.sleep(1)
    c("close"); time.sleep(1)
    for cmd in ["/publish true survival 25700", "/gamerule minecraft:send_command_feedback true", "/difficulty normal"]:
        say(cmd, c("cmd " + cmd)); time.sleep(0.5)
    say(st())
elif phase == "join":
    s = c("state", 25600)
    if s.get("screen") and "Accessibility" in s["screen"]:
        c("close", 25600); time.sleep(1)
    say(c("connect 127.0.0.1:25700", 25600))
    for i in range(60):
        time.sleep(2)
        s = c("state", 25600)
        if "HeroSelect" in (s.get("screen") or ""):
            break
    say("select 2", s.get("screen"))
    time.sleep(2)
    say(c("hero " + (sys.argv[2] if len(sys.argv) > 2 else "mercy"), 25600)); time.sleep(1)
    c("close", 25600)
    say(st(25600))
elif phase == "night":
    n = loglen()
    for cmd in ["/time set midnight", "/gamerule minecraft:advance_time false", "/effect give @a minecraft:resistance 99999 2 true",
                "/tp Viewer Kiriko"]:
        say(cmd, c("cmd " + cmd)); time.sleep(0.3)
    c("heromode on"); time.sleep(0.5)
    c("heromode on", 25600)
    x, y, z = [round(v) for v in (c("state")["x"], c("state")["y"], c("state")["z"])]
    for i in range(int(sys.argv[3]) if len(sys.argv) > 3 else 6):
        c(f"cmd /summon minecraft:zombie {x + 5 + i} {y} {z + 3 - i * 2}"); time.sleep(0.1)
    fight(float(sys.argv[2]) if len(sys.argv) > 2 else 40, "minecraft:zombie")
    say(st()); say(st(25600))
    c("cmd /overcraft potg highlights"); c("cmd /overcraft potg disk"); time.sleep(1)
    for x in potglines(n):
        say(x)
elif phase == "end":
    n = loglen()
    for cmd in ["/execute in minecraft:the_end run tp Kiriko 0 80 40", "/execute in minecraft:the_end run tp Viewer 6 80 40",
                "/effect give @a minecraft:resistance 99999 3 true", "/effect give @a minecraft:slow_falling 99999 0 true"]:
        say(cmd, c("cmd " + cmd)); time.sleep(1)
    time.sleep(8)
    for cmd in ["/kill @e[type=minecraft:end_crystal]", "/data merge entity @e[type=minecraft:ender_dragon,limit=1] {Health:30f}"]:
        say(cmd, c("cmd " + cmd)); time.sleep(1)
    say(st()); say(st(25600))
elif phase == "kill":
    # Kiriko's kunai (her own weapon) on the dragon till it dies; then the showing, sampled on both clients
    n = loglen(); n2 = loglen(25600)
    say("before 1", st()); say("before 2", st(25600))
    c("key secondary_fire down")
    t0 = time.time()
    while time.time() - t0 < float(sys.argv[2] if len(sys.argv) > 2 else 40):
        c("cmd /execute as Kiriko at @s anchored eyes facing entity @e[type=minecraft:ender_dragon,limit=1] eyes run tp @s ~ ~ ~ ~ ~")
        time.sleep(0.25)
        if any("the dragon died" in x for x in potglines(n)):
            break
    c("key secondary_fire up")
    say("dragon dead at", round(time.time() - t0, 1))
elif phase == "aim":
    # aim Kiriko at the dragon (server tp) while the real left button is held by potg_mouse.ps1; stops at its death
    n = loglen()
    t0 = time.time()
    while time.time() - t0 < float(sys.argv[2]):
        c("cmd /execute as Kiriko at @s anchored eyes facing entity @e[type=minecraft:ender_dragon,limit=1] eyes run tp @s ~ ~ ~ ~ ~")
        time.sleep(0.2)
        if any("the dragon died" in x for x in potglines(n)):
            break
    say("aim ended at", round(time.time() - t0, 1))
    for x in potglines(n):
        say(x)
elif phase == "watch":
    # sample both clients through a showing: screenshots and states (tag in argv[2])
    tag = sys.argv[2]
    n = loglen(); n2 = loglen(25600)
    t0 = time.time()
    for t in [float(v) for v in sys.argv[3].split(",")]:
        time.sleep(max(0, t - (time.time() - t0)))
        if "tp" in tag and t == float(sys.argv[3].split(",")[-3]):
            say("client 2 perspective back", c("perspective back", 25600))
        c(f"screenshot {tag}_1_{t:g}s.png"); c(f"screenshot {tag}_2_{t:g}s.png", 25600)
        say(f"  {t:g}s 1", st()); say(f"  {t:g}s 2", st(25600))
    if "tp" in tag:
        c("perspective first", 25600)
    for x in potglines(n):
        say(x)
    for x in potglines(n2, 25600):
        say("c2", x)
elif phase == "soak":
    # the disk bound over a long run: both heroes in the overworld, moving on 40 blocks each cycle (new chunks), a
    # fight each cycle, the disk read every cycle
    minutes = float(sys.argv[2])
    for cmd in ["/execute in minecraft:overworld run tp Kiriko 3.5 -60 8.5", "/execute in minecraft:overworld run tp Viewer 6.5 -60 8.5",
                "/time set midnight", "/effect give @a minecraft:resistance 99999 2 true", "/effect give @a minecraft:regeneration 99999 2 true"]:
        c("cmd " + cmd); time.sleep(0.5)
    t0 = time.time(); k = 0
    while time.time() - t0 < minutes * 60:
        k += 1
        x = 3 + 40 * k
        c(f"cmd /tp Kiriko {x} -60 8"); c(f"cmd /tp Viewer {x + 3} -60 8")
        time.sleep(3)
        for i in range(5):
            c(f"cmd /summon minecraft:zombie {x + 5 + i} -60 {11 - i * 2}"); time.sleep(0.1)
        fight(20 + (k % 3) * 10, "minecraft:zombie")
        c("cmd /kill @e[type=minecraft:zombie]")
        n = loglen()
        c("cmd /overcraft potg disk"); time.sleep(1)
        say(f"t {round((time.time() - t0) / 60, 1)} min, cycle {k}")
        for x2 in potglines(n):
            if "potg disk" in x2:
                say(x2[x2.index("potg disk"):])
        time.sleep(max(0, 60 - 0))
elif phase == "disk":
    n = loglen()
    c("cmd /overcraft potg disk"); time.sleep(1)
    for x in potglines(n):
        say(x)
else:
    port = 25600 if phase == "c2" else 25599
    for cmd in sys.argv[2:]:
        say(cmd, c(cmd, port))

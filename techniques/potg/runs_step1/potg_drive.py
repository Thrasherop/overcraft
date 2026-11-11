"""POTG step 1 driver: one harness socket, chat answers read from the game log. Usage: py -I potg_drive.py <phase>"""
import json, os, re, socket, sys, time

LOG = r"E:\claude\mods\overcraft_potgb\run\logs\latest.log"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "potg_runs")
os.makedirs(OUT, exist_ok=True)
sock = socket.create_connection(("127.0.0.1", 25599), timeout=60)
f = sock.makefile("r", encoding="utf8")
rec = open(os.path.join(OUT, sys.argv[1] + ".txt"), "a", encoding="utf8")


def say(*a):
    line = " ".join(str(x) for x in a)
    print(line, flush=True)
    rec.write(line + "\n"); rec.flush()


def c(cmd):
    sock.sendall((cmd + "\n").encode("utf8"))
    return json.loads(f.readline())


def loglines():
    with open(LOG, encoding="utf8", errors="replace") as h:
        return h.readlines()


def chat(cmd, pattern, wait=3.0):
    n = len(loglines())
    c("cmd " + cmd)
    end = time.time() + wait
    while time.time() < end:
        new = loglines()[n:]
        for l in new:
            if re.search(pattern, l):
                return l.strip()
        time.sleep(0.1)
    return None


def st():
    s = c("state")
    keys = ["screen", "fps", "x", "y", "z", "health", "maxHealth", "hero", "heroMode", "ult", "primaryAmmo", "secondaryAmmo", "anim"]
    return {k: (round(s[k], 2) if isinstance(s.get(k), float) else s.get(k)) for k in keys}


def mobs():
    out = {}
    for t in ["zombie", "skeleton", "ender_dragon", "end_crystal"]:
        l = chat(f"/execute if entity @e[type=minecraft:{t}]", r"Test (passed|failed)")
        m = re.search(r"[Cc]ount: (\d+)", l or "")
        out[t] = int(m.group(1)) if m else (0 if l and "failed" in l else None)
    return out


def fight(seconds, target, extra=lambda t: None):
    c("key secondary_fire down")
    t0 = time.time()
    k = 0
    while time.time() - t0 < seconds:
        c(f"cmd /execute as @s at @s anchored eyes facing entity @e[type={target},sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~ ~")
        extra(k)
        k += 1
        time.sleep(0.25)
        if k % 20 == 0:
            say("  t", round(time.time() - t0), st())
    c("key secondary_fire up")


def dump(tag):
    n = len(loglines())
    l = chat("/overcraft potg dump", r"ms/tick mean|isn't armed|nothing recorded", 5)
    say("dump", tag, l)
    time.sleep(3)
    lines = [x.rstrip() for x in loglines()[n:] if "[POTG]" in x]
    with open(os.path.join(OUT, f"{sys.argv[1]}_{tag}_dump.txt"), "w", encoding="utf8") as h:
        h.write("\n".join(lines))
    for x in lines:
        if "ms/tick" in x or "bytes/s" in x or "client decode" in x or "ticks recorded" in x:
            say(x[x.index("[POTG]"):])


phase = sys.argv[1]
say("=== phase", phase, time.strftime("%H:%M:%S"))
if phase == "world":
    s = c("state")
    if s.get("screen") and "Accessibility" in s["screen"]:
        c("close"); time.sleep(1)
    c("newworld potg2")
    for i in range(90):
        time.sleep(2)
        s = c("state")
        if "HeroSelect" in (s.get("screen") or ""):
            break
    say("select", s.get("screen"))
    time.sleep(3)
    c("hero kiriko"); time.sleep(1)
    c("window 1920 1080"); time.sleep(2)
    c("screenclick 240 244 1"); time.sleep(1)
    say(st())
elif phase == "night":
    if c("state").get("health", 1) <= 0:
        c("respawn"); time.sleep(3)
        if "HeroSelect" in (c("state").get("screen") or ""):
            c("screenclick 240 244 1"); time.sleep(1)
    c("cmd /overcraft potg stop"); time.sleep(0.5)
    c("cmd /kill @e[type=!minecraft:player]"); time.sleep(0.5)
    c("cmd /effect give @s minecraft:resistance 600 1 true"); time.sleep(0.3)
    for cmd in ["/gamerule minecraft:send_command_feedback true", "/time set midnight", "/difficulty normal",
                "/gamerule minecraft:advance_time false", "/tp @s 0 -60 0 0 0"]:
        c("cmd " + cmd); time.sleep(0.3)
    time.sleep(1)
    say("before arm", st(), mobs())
    say(chat("/overcraft potg start", r"POTG tap"))
    time.sleep(1)
    say("after arm", st())
    for i in range(5):
        c(f"cmd /summon minecraft:zombie {18 + i * 2} -60 {10 - i * 4}"); time.sleep(0.1)
    for i in range(3):
        c(f"cmd /summon minecraft:skeleton {-12 + i * 4} -60 {24}"); time.sleep(0.1)
    time.sleep(1)
    say("summoned", mobs())

    def extra(k):
        if k == 30: c("key ability_2 tap")      # Protection Suzu
        if k == 60: c("key ability_1 tap")      # Swift Step (no target: may do nothing)
        if k == 90: c("cmd /overcraft ult");
        if k == 95: c("key ultimate tap")
        if k == 130: c("press key.keyboard.g")  # G: out to Minecraft mode
        if k == 150: c("press key.keyboard.g")  # and back
    fight(60, "#minecraft:undead", extra)
    say("after fight", st(), mobs())
    dump("night1")
    say("after dump", st(), mobs())
    # more: skeletons and what's left
    fight(25, "#minecraft:undead")
    say("after skeletons", st(), mobs())
    dump("night2")
    say("after dump2", st(), mobs())
elif phase == "dragon":
    if c("state").get("health", 1) <= 0:
        c("respawn"); time.sleep(4)
        if "HeroSelect" in (c("state").get("screen") or ""):
            c("screenclick 240 244 1"); time.sleep(1)
    say("respawned", st())
    c("cmd /overcraft potg stop"); time.sleep(0.5)
    for cmd in ["/effect give @s minecraft:slow_falling 30 0 true", "/effect give @s minecraft:resistance 900 3 true",
                "/effect give @s minecraft:regeneration 900 2 true", "/overcraft potg start",
                "/execute in minecraft:the_end run tp @s 30 90 0"]:
        c("cmd " + cmd); time.sleep(0.5)
    for i in range(30):
        time.sleep(2)
        s = st()
        if s.get("y") and s["y"] < 80 and not s.get("screen"):
            break
    time.sleep(8)
    m = mobs()
    say("in the end", st(), m)
    if not m.get("ender_dragon"):
        c("cmd /summon minecraft:ender_dragon 0 80 0"); time.sleep(2)
        say("summoned dragon", mobs())
    c("cmd /data modify entity @e[type=minecraft:ender_dragon,limit=1] Health set value 60f")
    fight(40, "minecraft:ender_dragon")
    say("after dragon fight", st(), mobs())
    dump("dragon1")
    c("cmd /data modify entity @e[type=minecraft:ender_dragon,limit=1] Health set value 4f")
    fight(15, "minecraft:ender_dragon")
    say("after kill attempt", st(), mobs())
    time.sleep(11)  # the death: rising, XP, explosions, portal
    say("after death", st(), mobs())
    dump("dragon2")
    say(chat("/overcraft potg stop", r"POTG tap off"))
    time.sleep(1)
    say("after stop", st(), mobs())
elif phase == "dragondeath":
    c("cmd /overcraft potg stop"); time.sleep(0.3)
    c("cmd /overcraft potg start"); time.sleep(0.3)
    c("key secondary_fire down")
    t0 = time.time()
    while time.time() - t0 < 8:
        c("cmd /execute as @s at @s anchored eyes facing entity @e[type=minecraft:ender_dragon,sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~ ~")
        time.sleep(0.25)
    say("before kill", st(), mobs())
    say(chat("/kill @e[type=minecraft:ender_dragon]", r"[Kk]illed", 3))
    t0 = time.time()
    while time.time() - t0 < 14:
        c("cmd /execute as @s at @s anchored eyes facing entity @e[type=minecraft:ender_dragon,sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~ ~")
        time.sleep(0.25)
    c("key secondary_fire up")
    say("after death", st(), mobs())
    dump("death")
    say(chat("/overcraft potg stop", r"POTG tap off"))
    time.sleep(1)
    a = st(); say("after stop", a, mobs())
    c("press key.keyboard.g"); time.sleep(1); say("G after stop", st())
    c("press key.keyboard.g"); time.sleep(1); say("G again", st())
elif phase == "cmd":
    for a in sys.argv[2:]:
        say(a, c(a))
rec.close()

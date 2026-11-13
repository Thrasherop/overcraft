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


def spos():
    l = chat("/data get entity @s Pos", r"has the following entity data", 3)
    return l[l.index("data:") + 5:].strip() if l else None


def gtime():
    l = chat("/time query gametime", r"game time is", 3)
    return l[l.index("game time is"):].strip() if l else None


def replay(tag, pre=None, post=None, shots=10):
    n0 = len(loglines())
    say(tag, "before", st(), "server", spos(), mobs(), gtime())
    if pre:
        pre()
    c("cmd /overcraft potg replay")
    t0 = time.time()
    k = 0
    while time.time() - t0 < 15:
        s = st()
        say(f"  {time.time() - t0:5.1f}s", {x: s[x] for x in ["x", "y", "z", "health", "heroMode", "fps", "screen"]})
        if k < shots:
            c(f"screenshot potg_{tag}_{k:02d}.png")
        k += 1
        time.sleep(0.9)
        if any("[POTG] replay ended" in l and "Server" in l for l in loglines()[n0:]):
            break
    if post:
        post()
    time.sleep(1.5)
    for l in loglines()[n0:]:
        if "[POTG]" in l or "Exception" in l:
            say("  log", l.strip()[:400])
    say(tag, "after", st(), "server", spos(), mobs(), gtime())


phase = sys.argv[1]
say("=== phase", phase, time.strftime("%H:%M:%S"))
if phase == "world":
    s = c("state")
    if s.get("screen") and "Accessibility" in s["screen"]:
        c("close"); time.sleep(1)
    c("newworld " + (sys.argv[2] if len(sys.argv) > 2 else "potg2"))
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
elif phase == "types":
    for cmd in ["/gamerule minecraft:send_command_feedback true", "/time set noon", "/difficulty normal",
                "/gamerule minecraft:spawn_monsters false", "/effect give @s minecraft:resistance 600 4 true",
                "/tp @s 0 -60 0 0 0", "/kill @e[type=!minecraft:player]", "/overcraft potg stop", "/overcraft potg start"]:
        c("cmd " + cmd); time.sleep(0.4)
    steps = ["/setblock 4 -60 6 minecraft:oak_fence",
             "/summon minecraft:cow 6 -60 6 {leash:[I;4,-60,6]}",
             "/summon minecraft:chicken -4 -60 6 {Passengers:[{id:\"minecraft:zombie\",IsBaby:1b,PersistenceRequired:1b}]}",
             "/summon minecraft:enderman 0 -60 10 {PersistenceRequired:1b}",
             "/damage @e[type=minecraft:enderman,limit=1] 1 minecraft:arrow",
             "/summon minecraft:splash_potion 2 -57 4 {Item:{id:\"minecraft:splash_potion\",components:{\"minecraft:potion_contents\":{potion:\"minecraft:regeneration\"}}}}",
             "/particle minecraft:flame 0 -58 4 1 1 1 0 30",
             "/summon minecraft:creeper -6 -60 -8 {Fuse:5s,ignited:1b}",
             "/tp @e[type=minecraft:cow,limit=1] 6 -60 7",
             "/damage @e[type=minecraft:enderman,limit=1] 1 minecraft:arrow"]
    for s in steps:
        say(s, c("cmd " + s)); time.sleep(1.5)
    time.sleep(4)
    dump("types")
elif phase == "r_fight":
    for cmd in ["/gamerule minecraft:send_command_feedback true", "/time set midnight", "/difficulty normal",
                "/gamerule minecraft:advance_time false", "/tp @s 0 -60 0 0 0", "/kill @e[type=!minecraft:player]",
                "/effect give @s minecraft:resistance 600 1 true", "/overcraft potg stop", "/overcraft potg start"]:
        c("cmd " + cmd); time.sleep(0.3)
    for i in range(4):
        c(f"cmd /summon minecraft:zombie {12 + i * 2} -60 {8 - i * 4}"); time.sleep(0.1)
    for i in range(2):
        c(f"cmd /summon minecraft:skeleton {-10 + i * 5} -60 14"); time.sleep(0.1)
    def extra(k):
        if k == 40: c("key ability_2 tap")
        if k == 70: c("cmd /overcraft ult")
        if k == 75: c("key ultimate tap")
        if k == 95: c("press key.keyboard.g")   # weapon goes: Minecraft mode inside the window
        if k == 110: c("press key.keyboard.g")
    fight(34, "#minecraft:undead", extra)
    replay("fight")
elif phase == "r_walk":
    replay("walk", pre=lambda: (c("key forward down"), time.sleep(0.4)), post=lambda: c("key forward up"), shots=0)
elif phase == "r_air":
    replay("air", pre=lambda: (c("cmd /tp @s ~ ~6 ~"), time.sleep(0.3)), shots=0)
elif phase == "r_jump":
    replay("jump", pre=lambda: (c("key jump down"), time.sleep(0.3)), post=lambda: c("key jump up"), shots=0)
elif phase == "r_death":
    for cmd in ["/time set midnight", "/tp @s 0 -60 0 0 0", "/kill @e[type=!minecraft:player]",
                "/gamemode survival", "/overcraft potg stop", "/overcraft potg start"]:
        c("cmd " + cmd); time.sleep(0.3)
    for i in range(3):
        c(f"cmd /summon minecraft:zombie {10 + i * 2} -60 {6 - i * 4}"); time.sleep(0.1)
    fight(14, "#minecraft:undead")
    say("dying", st())
    c("cmd /kill @s"); time.sleep(2.5)
    say("dead", st())
    c("respawn"); time.sleep(3)
    if "HeroSelect" in (c("state").get("screen") or ""):
        c("screenclick 240 244 1"); time.sleep(1)
    say("respawned", st())
    c("cmd /effect give @s minecraft:resistance 600 2 true")
    for i in range(3):
        c(f"cmd /summon minecraft:zombie {8 + i * 3} -60 {4 - i * 3}"); time.sleep(0.1)
    c("cmd /summon minecraft:skeleton -9 -60 12"); time.sleep(0.1)
    fight(6, "#minecraft:undead")
    replay("death", shots=12)
elif phase == "r_dragon":
    for cmd in ["/kill @e[type=!minecraft:player]", "/effect give @s minecraft:slow_falling 20 0 true",
                "/effect give @s minecraft:resistance 900 3 true", "/effect give @s minecraft:regeneration 900 2 true",
                "/execute in minecraft:the_end run tp @s 30 90 0"]:
        c("cmd " + cmd); time.sleep(0.4)
    time.sleep(12)
    say("in the end", st(), mobs())
    c("cmd /overcraft potg stop"); time.sleep(0.3)
    c("cmd /overcraft potg start"); time.sleep(0.3)
    fight(32, "minecraft:ender_dragon")
    replay("dragon", shots=12)
elif phase == "r_keys":
    import subprocess
    def keys():
        time.sleep(1.5)
        r = subprocess.run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                            os.path.join(os.path.dirname(os.path.abspath(__file__)), "rk_keys.ps1")], capture_output=True, text=True)
        say("real keys:", (r.stdout + r.stderr).strip()[:300])
        s = c("state")
        say("  during, after keys", {x: s.get(x) for x in ["x", "y", "z", "heroMode", "ult", "ability2Cooldown", "slot", "screen"]})
    s = c("state")
    say("cooldowns before", {x: s.get(x) for x in ["heroMode", "ult", "ability2Cooldown", "slot"]})
    replay("keys", pre=None, post=None, shots=0) if False else None
    n0 = len(loglines())
    c("cmd /overcraft potg replay")
    keys()
    t0 = time.time()
    while time.time() - t0 < 14:
        time.sleep(1)
        if any("[POTG] replay ended" in l and "Server" in l for l in loglines()[n0:]):
            break
    time.sleep(1.5)
    s = c("state")
    say("after", {x: s.get(x) for x in ["x", "y", "z", "heroMode", "ult", "ability2Cooldown", "slot", "screen", "health"]})
    for l in loglines()[n0:]:
        if "[POTG]" in l:
            say("  log", l.strip()[:300])
elif phase == "cmd":
    for a in sys.argv[2:]:
        say(a, c(a))
rec.close()

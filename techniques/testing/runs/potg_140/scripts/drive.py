"""#140 driver: DevBot add/remove against the POTG recorder. Usage: py -I drive.py <repro|loop|dragon> [world]"""
import json, os, re, socket, subprocess, sys, time, threading

HERE = os.path.dirname(os.path.abspath(__file__))
LOG = r"E:\claude\mods\overcraft_potgcrash\run\logs\latest.log"
sock = socket.create_connection(("127.0.0.1", 25599), timeout=60)
f = sock.makefile("r", encoding="utf8")
rec = open(os.path.join(HERE, "..", sys.argv[1] + ".txt"), "a", encoding="utf8")


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
        for l in loglines()[n:]:
            if re.search(pattern, l):
                return l.strip()
        time.sleep(0.1)
    return None


def disk():
    return chat("/overcraft potg disk", r"potg disk", 3)


def st():
    s = c("state")
    return {k: s.get(k) for k in ["screen", "x", "y", "z", "health", "hero", "heroMode"]}


def world(name):
    c("newworld " + name + " creative")
    for _ in range(60):
        time.sleep(1)
        s = c("state")
        if s.get("x") is not None and not s.get("screen"):
            break
    time.sleep(3)
    for cmd in ["/gamerule minecraft:send_command_feedback true", "/gamerule minecraft:spawn_monsters false", "/kill @e[type=!minecraft:player]", "/time set noon"]:
        c("cmd " + cmd); time.sleep(0.3)
    c("hero kiriko"); time.sleep(1)
    c("heromode on"); time.sleep(1)
    say("world", name, st())


def cycle(n, names):
    s = st()
    x, y, z = round(s["x"]), round(s["y"]), round(s["z"])
    for i in range(n):
        for j, b in enumerate(names):
            say("add", b, c(f"bot add {b} kiriko - {x + 3 + j} {y} {z + 2} 0"))
        time.sleep(1.0)
        for b in names:
            say("remove", b, c(f"bot remove {b}"))
        time.sleep(0.3)
    say("disk after", n, "cycles:", disk())


def sprint(ticks, wait):
    n0 = len(loglines())
    c(f"servercmd tick sprint {ticks}")
    end = time.time() + wait
    while time.time() < end:
        time.sleep(1)
        new = loglines()[n0:]
        bad = [l for l in new if "Tried to get player" in l or "crash" in l.lower() and "report" in l.lower()]
        if bad:
            say("CRASH:", bad[0].strip()[:300])
            return False
        if any("Sprint completed" in l or "sprint" in l.lower() and "completed" in l.lower() for l in new):
            break
    time.sleep(2)
    new = loglines()[n0:]
    for l in new:
        if "Tried to get player" in l or "Exception" in l or "Sprint" in l:
            say("  log", l.strip()[:300])
    say("after sprint", st())
    return not any("Tried to get player" in l for l in new)


phase = sys.argv[1]
say("=== phase", phase, time.strftime("%H:%M:%S"))
if phase == "repro":
    world(sys.argv[2])
    say("fix off", c("bot fix 140 off"))
    cycle(3, ["b1"])
    say("disk", disk())
    ok = sprint(6200, 90)
    say("survived" if ok else "crashed", st())
elif phase == "loop":
    world(sys.argv[2])
    say("fix on", c("bot fix 140 on"))
    cycle(15, ["b1", "b2"])
    cycle(10, ["b1"])
    ok = sprint(12500, 150)
    say("survived" if ok else "crashed", disk())
    # a bot added again after the sweep records again
    s = st()
    say("re-add", c(f"bot add b1 kiriko - {round(s['x']) + 3} {round(s['y'])} {round(s['z']) + 2} 0"))
    time.sleep(3)
    say("disk with b1 back", disk())
    ok2 = sprint(6200, 90)
    say("b1 kept past a snapshot" if ok2 else "crashed", disk())
    c(f"bot remove b1"); time.sleep(1)
    say("disk with b1 gone", disk())
elif phase == "dragon":
    def rk(case, secs=5):
        r = subprocess.run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                            r"E:\claude\mods\overcraft_potgcrash\techniques\potg\runs_step3\rk_potg.ps1", case, str(secs)],
                           capture_output=True, text=True)
        say("real input:", (r.stdout + r.stderr).strip()[:300])
    c("cmd /gamemode survival"); time.sleep(0.3)
    for cmd in ["/effect give @s minecraft:slow_falling 25 0 true", "/effect give @s minecraft:resistance 900 3 true",
                "/effect give @s minecraft:regeneration 900 2 true", "/execute in minecraft:the_end run tp @s 30 90 0"]:
        c("cmd " + cmd); time.sleep(0.4)
    time.sleep(14)
    c("cmd /kill @e[type=minecraft:end_crystal]"); time.sleep(0.5)
    say("in the end", st(), disk())
    n0 = len(loglines())
    c("cmd /data modify entity @e[type=minecraft:ender_dragon,limit=1] Health set value 24f")
    threading.Thread(target=rk, args=("hold", 40), daemon=True).start()
    t0 = time.time()
    while time.time() - t0 < 42:
        c("cmd /execute as @s at @s anchored eyes facing entity @e[type=minecraft:ender_dragon,sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~ ~")
        time.sleep(0.25)
        if any("[POTG] the dragon died" in l for l in loglines()[n0:]):
            break
    say("fight over", st(), "dragon died" if any("[POTG] the dragon died" in l for l in loglines()[n0:]) else "dragon alive")
    t0 = time.time(); shot = 0
    while time.time() - t0 < 40:
        c(f"screenshot p140_dragon_{shot:02d}.png"); shot += 1
        if any("[POTG] replay ended" in l and "Server" in l for l in loglines()[n0:]):
            break
        time.sleep(1.0)
    time.sleep(2)
    say("after", st())
    for l in loglines()[n0:]:
        if "[POTG]" in l or "Exception" in l:
            say("  log", l.strip()[:300])

"""POTG scoring-values run driver (Refs #44): the dev harness on 25599, answers and [POTG] lines recorded.
Usage: py -I drive.py <cmd...>   each argument one harness command; 'wait N' sleeps; 'log' prints new [POTG] lines."""
import json, os, socket, sys, time

ROOT = r"E:\claude\mods\overcraft_scoring"
LOG = os.path.join(ROOT, "run", "logs", "latest.log")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "driver.txt")
MARK = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".logmark")
rec = open(OUT, "a", encoding="utf8")


def say(*a):
    line = " ".join(str(x) for x in a)
    print(line, flush=True)
    rec.write(line + "\n"); rec.flush()


sock = None


def c(cmd):
    global sock
    if sock is None:
        s = socket.create_connection(("127.0.0.1", 25599), timeout=60)
        sock = (s, s.makefile("r", encoding="utf8"))
    s, f = sock
    s.sendall((cmd + "\n").encode("utf8"))
    return json.loads(f.readline())


def newlines():
    n = int(open(MARK).read()) if os.path.exists(MARK) else 0
    with open(LOG, encoding="utf8", errors="replace") as h:
        lines = h.readlines()
    if n > len(lines):
        n = 0
    open(MARK, "w").write(str(len(lines)))
    return [x.rstrip() for x in lines[n:] if "[POTG]" in x or "Exception" in x or "ERROR" in x]


say("===", time.strftime("%H:%M:%S"))
for a in sys.argv[1:]:
    if a.startswith("wait "):
        time.sleep(float(a[5:]))
    elif a == "log":
        for x in newlines():
            say(x)
    elif a.startswith("fight "):
        # 'fight <type> <seconds> [key]': face the nearest of that type each 0.25 s, the fire key held
        _, typ, secs, *k = a.split(" ")
        key = k[0] if k else "primary_fire"
        c(f"key {key} down")
        t0 = time.time()
        while time.time() - t0 < float(secs):
            c(f"cmd /execute as @s at @s anchored eyes facing entity @e[type={typ},sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~ ~")
            time.sleep(0.25)
        c(f"key {key} up")
        say("fought", typ, secs)
    elif a == "st":
        s = c("state")
        say({k: s.get(k) for k in ["screen", "inWorld", "x", "y", "z", "yaw", "pitch", "health", "hero", "heroMode", "dimension"]})
    else:
        say(">", a, json.dumps(c(a))[:300])

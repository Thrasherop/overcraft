"""Juno's movement from a movement recorder log (parse_log.py --csv ... --sqlite first): her double jumps, her hover and her
Glide Boosts, as tables. py -I juno.py <log.sqlite>"""
import math
import sqlite3
import sys

db = sqlite3.connect(sys.argv[1])
rows = list(db.execute("select t, seg, px, py, pz, vx, vy, vz, ground, jump, ab1, ix, iz from ticks where hero = 'Juno' order by t"))
T = {name: i for i, name in enumerate(("t", "seg", "px", "py", "pz", "vx", "vy", "vz", "ground", "jump", "ab1", "ix", "iz"))}


def g(r, k):
    return r[T[k]]


def hs(r):
    return math.hypot(g(r, "vx"), g(r, "vz"))


def truth(v):
    return v in (1, "1", "True", True)


# ---- double jumps: a rise in vertical speed of 3 m/s or more between two ticks in the air (not a take-off) ----
print("DOUBLE JUMPS: t | seg | air s before | vy before -> after | horizontal before -> after | rise to apex m | in glide")
takeoff = None
for i in range(1, len(rows)):
    a, b = rows[i - 1], rows[i]
    if truth(g(a, "ground")) and not truth(g(b, "ground")):
        takeoff = g(b, "t")
    if truth(g(a, "ground")) or truth(g(b, "ground")):
        continue
    if g(b, "vy") - g(a, "vy") >= 3:
        apex = g(b, "py")
        for j in range(i, min(i + 200, len(rows))):
            if truth(g(rows[j], "ground")):
                break
            apex = max(apex, g(rows[j], "py"))
            if g(rows[j], "vy") < 0:
                break
        print(f"{g(b, 't'):7.2f} | {g(b, 'seg')} | {g(a, 't') - takeoff if takeoff else -1:5.2f} | {g(a, 'vy'):6.2f} -> {g(b, 'vy'):6.2f} | "
              f"{hs(a):5.2f} -> {hs(b):5.2f} | {apex - g(a, 'py'):5.2f} | {truth(g(b, 'ab1'))}")

# ---- hover: falling with jump held, not gliding ----
falls = [g(r, "vy") for r in rows if not truth(g(r, "ground")) and truth(g(r, "jump")) and not truth(g(r, "ab1")) and g(r, "vy") < 0]
free = [g(r, "vy") for r in rows if not truth(g(r, "ground")) and not truth(g(r, "jump")) and not truth(g(r, "ab1")) and g(r, "vy") < 0]
print("\nHOVER: falling with jump held: ticks", len(falls), "slowest-fall floor (min vy)", round(min(falls), 2) if falls else None,
      "| vy counts:", sorted({round(v, 1) for v in falls})[:12])
print("       falling without jump: min vy", round(min(free), 2) if free else None)
# hover sessions: runs of jump-held falling, with height above the run's lowest point and where the cap starts
run = []
for r in rows + [None]:
    if r is not None and not truth(g(r, "ground")) and truth(g(r, "jump")) and not truth(g(r, "ab1")):
        run.append(r)
        continue
    if len(run) > 15:
        capped = [x for x in run if abs(g(x, "vy") + 3) < 0.05]
        print(f"  held {g(run[0], 't'):7.2f}-{g(run[-1], 't'):7.2f}: vy {g(run[0], 'vy'):6.2f} .. min {min(g(x, 'vy') for x in run):6.2f}, "
              f"{len(capped)} ticks at -3, dropped {g(run[0], 'py') - g(run[-1], 'py'):5.2f} m")
    run = []

# ---- Glide Boost: each use ----
print("\nGLIDES: start t | lasted | horizontal speed at 0, .05, .1, .2, .4, .8, 1.5, 3 s | vy values | rose m | input")
i = 0
while i < len(rows):
    if truth(g(rows[i], "ab1")) and (i == 0 or not truth(g(rows[i - 1], "ab1"))):
        j = i
        while j < len(rows) and truth(g(rows[j], "ab1")):
            j += 1
        seg = rows[i:j]
        t0 = g(seg[0], "t")

        def at(dt):
            best = min(seg, key=lambda r: abs(g(r, "t") - t0 - dt))
            return f"{hs(best):5.2f}"
        vys = sorted({round(g(r, "vy"), 1) for r in seg})
        inputs = sorted({(round(g(r, "ix")), round(g(r, "iz"))) for r in seg})
        print(f"{t0:7.2f} | {g(seg[-1], 't') - t0:4.2f} | {' '.join(at(d) for d in (0, .05, .1, .2, .4, .8, 1.5, 3))} | {vys[:8]} | "
              f"{g(seg[-1], 'py') - g(seg[0], 'py'):5.2f} | {inputs[:6]}")
        i = j
    i += 1

"""Dev harness helper: the look (yaw, pitch) from the player's eyes to a point on the nearest entity of a type.
py -I tools/aim.py <type> <height above its feet> [eye height]: prints "yaw pitch" for `dev look`. Reads positions with
/data through tools/dev.py and the game log (the dev client's stdout, OC_LOG or the scratch log path given)."""
import math, os, re, subprocess, sys, time

def main():
    kind, height = sys.argv[1], float(sys.argv[2])
    eye = float(sys.argv[3]) if len(sys.argv) > 3 else 1.62
    log = os.environ["OC_LOG"]
    here = os.path.dirname(os.path.abspath(__file__))
    def pos(selector):
        subprocess.run([sys.executable, "-I", os.path.join(here, "dev.py"), f"cmd data get entity {selector} Pos"],
                       capture_output=True)
        time.sleep(0.4)
        with open(log, encoding="utf8", errors="replace") as f:
            line = [l for l in f if "entity data: [" in l][-1]
        return [float(v) for v in re.findall(r"(-?[\d.]+)d", line.split("data: ")[1])]
    me = pos("@s")
    it = pos(f"@e[type={kind},limit=1,sort=nearest]")
    dx, dy, dz = it[0] - me[0], it[1] + height - (me[1] + eye), it[2] - me[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    print(f"{yaw:.2f} {pitch:.2f} {math.sqrt(dx*dx+dy*dy+dz*dz):.2f}")

main()

# #110: Charged Shot from a 3 m ledge down through two husks (the shot's line, steep). Same harness as pierce.py.
import sys, time
sys.argv = sys.argv[:2]
exec(open(__file__.replace("steep.py", "pierce.py"), encoding="utf-8-sig").read().split('log("| case')[0])


def steep(name, zs, aim_z, aim_h):
    x = spot(zs)
    mc("/fill %d -60 -2 %d -58 1 minecraft:stone" % (x - 1, x + 1), 0.15)
    mc("/tp @s %s -57 0.5 0 0" % x, 0.4)
    mc("/execute as @s at @s anchored eyes facing %s %s %s run tp @s ~ ~ ~ ~ ~" % (x, -60 + aim_h, aim_z), 0.3)
    st = c("state")
    before = [hp("h%d" % i) for i in range(len(zs))]
    c("presstrace energy 100"); time.sleep(0.05)
    c("key primary_fire tap"); time.sleep(0.6)
    after = [hp("h%d" % i) for i in range(len(zs))]
    ow = [None if b is None or a is None else round((b - a) * 12.5, 1) for b, a in zip(before, after)]
    log("| %s | pitch %.2f | %s |" % (name, st.get("pitch", 0), " / ".join(str(o) for o in ow)))


log("| case (eye 4.62 m up) | aim | OW damage, front / back |")
log("|---|---|---|")
steep("husks at 4 and 6 m, aimed at the back one's feet (0.2)", [4.5, 6.5], 6.5, 0.2)
steep("husks at 4 and 6 m, aimed at the back one's waist (0.9)", [4.5, 6.5], 6.5, 0.9)
steep("husks at 4 and 6 m, aimed at the front one's waist (0.9)", [4.5, 6.5], 4.5, 0.9)
steep("husks at 4 and 6 m, aimed at the front one's legs (0.4)", [4.5, 6.5], 4.5, 0.4)
log("### done")

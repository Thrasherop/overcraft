# Real mouse flicks onto the target: turned 30 degrees away by command, then 200 counts back (0.15 degrees a count at the default sensitivity), the right button after.
# Cases: flick = all in one burst, the click in that frame; flickwait = the burst, 30 ms, the click; flickhuman = 200 counts over about 100 ms, 15 ms, the click.
import subprocess
SCRIPT = ARGS[0]
for case in ARGS[1:]:
    for i in range(6):
        mc('/tp @s 0.5 -60 0.5 30 0', 0.2); c('look 30 0'); sleep(0.5)
        z0 = hp('@e[type=minecraft:zombie,limit=1]')
        c('presstrace energy 50'); c('presstrace start'); sleep(0.3)
        r = subprocess.run(['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', SCRIPT, case], capture_output=True, text=True)
        sleep(1.0)
        tr = c('presstrace').get('trace', '')
        z1 = hp('@e[type=minecraft:zombie,limit=1]')
        s = st()
        log(case, i, r.stderr.strip()[:120], 'hit' if z1 != z0 else 'MISS', 'zombie', z0, '->', z1, 'yaw', round(s.get('yaw'), 2), '|', tr[:110])
        sleep(0.4)

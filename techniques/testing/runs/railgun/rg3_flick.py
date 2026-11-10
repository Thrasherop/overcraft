# rk_flick with the frame rate read as it happens. Real OS mouse events; turned 30 degrees away by command, 200 counts back.
import subprocess
SCRIPT = ARGS[0]
for case in ARGS[1:]:
    hits = 0; n = 0
    for i in range(10 if case == 'flick' else 6):
        mc('/tp @s 0.5 -60 0.5 30 0', 0.2); c('look 30 0'); sleep(0.5)
        z0 = hp('@e[type=minecraft:zombie,limit=1]')
        c('presstrace energy 50'); c('presstrace start'); sleep(0.3); f0 = st().get('fps')
        r = subprocess.run(['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', SCRIPT, case], capture_output=True, text=True)
        sleep(0.3); f1 = st().get('fps'); sleep(0.7)
        tr = c('presstrace').get('trace', '')
        z1 = hp('@e[type=minecraft:zombie,limit=1]')
        s = st(); n += 1; hits += 1 if z1 != z0 else 0
        log(case, i, r.stderr.strip()[:120], 'hit' if z1 != z0 else 'MISS', 'fps before the script', f0, 'after its focus', f1, 'yaw', round(s.get('yaw'), 2), '|', tr[:90])
        sleep(0.4)
    log('FLICK', case, hits, 'of', n)

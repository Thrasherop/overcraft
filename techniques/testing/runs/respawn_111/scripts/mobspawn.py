"""Zombies standing on the spawnpoint: py mobspawn.py <hero> <deaths>. /kill, respawn among them, health over 3 s."""
import json, socket, sys, time

with socket.create_connection(("127.0.0.1", 25599), timeout=40) as sock:
    f = sock.makefile("r", encoding="utf8")
    def send(c):
        sock.sendall((c + "\n").encode()); return json.loads(f.readline())
    send("hero " + sys.argv[1]); time.sleep(1.5)
    send("cmd /kill @e[type=!player]")
    send("cmd /tp @s 3.5 -60 -4.5"); send("cmd /spawnpoint @s 3 -60 -5")
    for z in ("~1.5 ~ ~", "~-1.5 ~ ~", "~ ~ ~1.5"):
        send("cmd /summon zombie " + z + " {PersistenceRequired:1b}")
    time.sleep(0.5)
    for i in range(int(sys.argv[2])):
        send("cmd /kill @s"); time.sleep(0.3)
        send("respawn")
        rows = []; t0 = time.time()
        for wait in (0.05, 0.5, 1, 1.5, 2, 3):
            while time.time() - t0 < wait:
                time.sleep(0.01)
            s = send("state"); rows.append((wait, round(s.get("health", -1), 2)))
        print(i, rows, flush=True)
    send("cmd /kill @e[type=zombie]")

"""After a respawn (or not): take 5 damage, then watch health for 5 s. py regen.py <hero> <respawn first: 0/1>"""
import json, socket, sys, time

with socket.create_connection(("127.0.0.1", 25599), timeout=40) as sock:
    f = sock.makefile("r", encoding="utf8")
    def send(c):
        sock.sendall((c + "\n").encode()); return json.loads(f.readline())
    send("cmd /kill @e[type=!player]")
    send("hero " + sys.argv[1]); time.sleep(1.5)
    if sys.argv[2] == "1":
        send("cmd /kill @s"); time.sleep(0.3); send("respawn"); time.sleep(0.3)
    s = send("state"); print("food", s.get("food"), "sat", s.get("saturation"), "health", s.get("health"))
    send("cmd /damage @s 5 minecraft:generic")
    rows = []; t0 = time.time()
    while time.time() - t0 < 5:
        s = send("state"); rows.append((round(time.time() - t0, 1), round(s["health"], 2), round(s["saturation"], 1)))
        time.sleep(0.25)
    print(rows)

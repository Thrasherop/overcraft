"""Tracer: stick a Pulse Bomb at her feet, die before it goes off, respawn on the spot; health over the next 2 s."""
import json, socket, sys, time

with socket.create_connection(("127.0.0.1", 25599), timeout=40) as sock:
    f = sock.makefile("r", encoding="utf8")
    def send(c):
        sock.sendall((c + "\n").encode()); return json.loads(f.readline())
    def st():
        s = send("state"); return (round(s.get("health", -1), 2), round(s.get("maxHealth", -1), 2), s.get("hero"), s.get("screen"))
    send("hero tracer"); time.sleep(1.5)
    for i in range(int(sys.argv[1]) if len(sys.argv) > 1 else 3):
        send("look 0 89"); time.sleep(0.3)
        send("cmd /overcraft ult"); time.sleep(0.2)
        print(i, "before", st(), send("ents").get("ents", "")[:0] if False else "")
        send("press key.keyboard.q"); time.sleep(0.25)
        send("cmd /kill @s"); time.sleep(0.05)
        send("respawn")
        t0 = time.time(); rows = []
        while time.time() - t0 < 2.5:
            rows.append((round(time.time() - t0, 2),) + st()[:1]); time.sleep(0.1)
        print(i, rows, flush=True)
        time.sleep(1)

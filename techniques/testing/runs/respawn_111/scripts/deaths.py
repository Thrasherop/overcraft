"""Kill-respawn loop: python deaths.py <hero> <count>. Reads health right after each respawn and after 1 s and 3 s."""
import json, socket, sys, time

def main():
    hero, count = sys.argv[1], int(sys.argv[2])
    pre = sys.argv[3:]  # extra commands before each death
    with socket.create_connection(("127.0.0.1", 25599), timeout=40) as sock:
        f = sock.makefile("r", encoding="utf8")
        def send(c):
            sock.sendall((c + "\n").encode()); return json.loads(f.readline())
        def st():
            s = send("state"); return (round(s.get("health", -1), 2), round(s.get("maxHealth", -1), 2), round(s.get("shield", 0), 1), s.get("food"), s.get("hero"), s.get("screen"))
        if hero != "-":
            send("hero " + hero); time.sleep(1.5)
        print("start", st())
        for i in range(count):
            for c in pre:
                if c.startswith("sleep "):
                    time.sleep(float(c[6:]))
                else:
                    send(c)
            send("cmd /kill @s"); time.sleep(0.6)
            print(i, "dead", st())
            send("respawn"); time.sleep(0.05)
            a = st(); time.sleep(1); b = st(); time.sleep(2); c = st()
            print(i, "0.05s", a, "1s", b, "3s", c, flush=True)

main()

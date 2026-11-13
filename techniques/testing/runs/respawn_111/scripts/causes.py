"""Death causes with no mobs: py causes.py <hero>. Each cause: setup commands, wait for death, respawn, read health 0.05 s..3 s."""
import json, socket, sys, time

CAUSES = {
    "lava": ["cmd /setblock ~ ~ ~ lava"],
    "fall": ["cmd /tp @s ~ ~60 ~"],
    "fire": ["cmd /setblock ~ ~ ~ fire", "cmd /effect give @s minecraft:wither 60 4"],
    "wither": ["cmd /effect give @s minecraft:wither 60 9"],
    "poison_hunger": ["cmd /effect give @s minecraft:hunger 60 50", "cmd /effect give @s minecraft:poison 60 5", "cmd /damage @s 1000 minecraft:generic"],
    "mob_attack": ["cmd /summon zombie ~ ~ ~5 {NoAI:1b,Tags:[\"z\"]}", "cmd /damage @s 1000 minecraft:mob_attack by @e[tag=z,limit=1]", "cmd /kill @e[tag=z]"],
    "zombie_hits": ["cmd /summon zombie ~1 ~ ~ {Tags:[\"z\"]}", "cmd /summon zombie ~-1 ~ ~ {Tags:[\"z\"]}", "cmd /summon zombie ~ ~ ~1 {Tags:[\"z\"]}"],
    "drown": ["cmd /fill ~-1 ~ ~-1 ~1 ~2 ~1 water", "cmd /effect give @s minecraft:wither 60 9"],
    "kill_mc_mode": ["press key.keyboard.g", "sleep 0.5", "cmd /kill @s"],
}

with socket.create_connection(("127.0.0.1", 25599), timeout=40) as sock:
    f = sock.makefile("r", encoding="utf8")
    def send(c):
        sock.sendall((c + "\n").encode()); return json.loads(f.readline())
    def st():
        s = send("state"); return s
    hero = sys.argv[1]
    names = sys.argv[2:] or list(CAUSES)
    send("hero " + hero); time.sleep(1.5)
    for name in names:
        send("cmd /tp @s 3.5 -60 -4.5"); send("cmd /kill @e[type=!player]"); send("cmd /fill ~-3 ~ ~-3 ~3 ~3 ~3 air"); time.sleep(0.5)
        s = st()
        if not s.get("heroMode"):
            send("press key.keyboard.g"); time.sleep(0.5)
        for c in CAUSES[name]:
            if c.startswith("sleep "):
                time.sleep(float(c[6:]))
            elif c.startswith("press"):
                send(c)
            else:
                send(c)
        t0 = time.time()
        while st().get("screen") != "DeathScreen" and time.time() - t0 < 30:
            time.sleep(0.1)
        died = round(time.time() - t0, 1)
        if name not in ("zombie_hits",):
            send("cmd /kill @e[type=!player]")
        send("cmd /fill 0 -60 -8 7 -55 -1 air")
        time.sleep(0.2)
        send("respawn")
        rows = []
        t0 = time.time()
        for wait in (0.05, 0.5, 1, 2, 3):
            while time.time() - t0 < wait:
                time.sleep(0.01)
            s = st()
            rows.append((wait, round(s.get("health", -1), 2), s.get("maxHealth"), s.get("shield"), s.get("food"), s.get("heroMode")))
        print(name, "died after", died, "s:", rows, flush=True)
        send("cmd /kill @e[type=!player]")
        send("cmd /effect clear @s")
        time.sleep(0.5)

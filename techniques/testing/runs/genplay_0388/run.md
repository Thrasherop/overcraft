# General play before main, 2026-11-17 (0.3.81 -> 0.3.88)

A fresh client from `next` (31706a5), a new survival world (`GenPlay088`, Normal), real mouse and keys unless marked. Builds
on [genplay_0381](../genplay_0381/run.md) and covers what it left out: the Nether, ladders, a boat, a crafted chest; plus
what's new since: enemy heroes at night (#46) and sprays (#41). Screenshots `run/screenshots/gp88_*.png`. Up:
[testing](../../README.md).

**Every hero picked and fired** at a still husk 6 m off (held left button 1.5 s unless noted; husk health read from the server):
- **On join:** the select screen came up; Ana picked with two real clicks; two shots 20 -> 8.82.
- **Mid-game, with H:** "Go back to your bed to change heroes" on the sky platform (by design, `HeroManager.canChangeHero`);
  after `/spawnpoint` there (the bed stand-in), H opened the select screen. The last three supports were picked with the
  harness `screenclick` (the dead click strip at x >= ~935, as in genplay_0381).

| Hero | Result | Hero | Result |
|---|---|---|---|
| Sigma | 20 -> 1.31 | Lucio | killed |
| D.Va | killed | Jetpack Cat | killed |
| Sojourn | killed | Mercy (staff) | 20 -> 20 (the control: a heal beam does no harm) |
| Pharah | 20 -> 1.11 | Mercy (Blaster, key 2) | killed |
| Tracer (4 quick clicks) | 20 -> 9.44: 0.3.81's fix holds | Lifeweaver (4 right clicks) | 20 -> 14.59 |
| Cassidy | 20 -> 8.98 | Sombra | killed |
| Junkrat | killed | Brigitte | 20 -> 9.2 |
| Torbjorn | killed | Illari | 20 -> 2.29 |
| Ashe | killed | Juno | killed |
| Widowmaker | killed | Kiriko | killed |

The run of kills was checked: each is "[POTG] Kiriko: +250 (kill minecraft:husk)" in the log, the husk stood untouched at 20
for 6 s without fire, and Mercy's staff left it at 20.

- **Heal:** Kiriko's Healing Ofuda (3 right clicks) took a villager from 6 to 15.6.
- **Deaths and respawns:** three husks with AI took Kiriko 20 -> 14 -> 5 -> 0 in ~4.5 s; Respawn brought her back as Kiriko.
  (My `/kill` of the husks was sent while dead and dropped: they killed her twice more on respawn. Commands while dead don't run.)
  Later a fall in the Nether and lava (setup) killed her: each Respawn put her back on the bed as Kiriko at full health, hero mode on.
- **Leave and rejoin:** Save and Quit, the world list, the world: the select screen came up with Kiriko chosen; Ready.
- **Sprays (#41):** a real Y tap put Kiriko's first spray on the floor. The loadout's SPRAYS tab was played just before
  (0.3.88's commit): pixel in LEFT, shown on the wheel, sprayed, still equipped after a restart.
- **Night (#46):** in 90 s on Normal, enemy Sojourn, Torbjorn, Ashe, Widowmaker and Junkrat replaced spawns among ~15-22
  zombies, 82-127 m off (the cap's 64 m held). Walking at Junkrat in a dark forest, something brought Kiriko to 12 and a
  spider killed her. A summoned enemy Cassidy on the platform: 20 -> 18.32 on Kiriko, and two kunai bursts killed him
  ("[POTG] +250 (kill overcraft:enemy_cassidy)"). (A first Cassidy, summoned past the platform's edge, fell: setup.)
- **Minecraft mode (G)**, real keys and mouse throughout except where marked:
  - Chopped 3 dark oak logs by hand (picked up); 2 more given by command, as a chest, ladders and a boat need 6 logs.
  - Dug a grass block by hand, picked up the dirt.
  - 2x2 grid: 20 planks, a crafting table; placed it (right click), opened it.
  - At the table: a log to planks, **a crafted chest**, sticks, **3 ladders**, **a dark oak boat**.
  - Placed the chest (a right click on the table opens the table: vanilla, aimed at the ground instead), opened it, put the
    dirt and 3 planks in with real clicks.
  - **Ladders:** placed 3 on a trunk; the first climb walked off them (not lined up). On a ladder column (set by command),
    real W climbed y 62 -> 64.8 in 2.5 s.
  - **Boat:** in the forest a placed boat fell into a pool with the player (they sank; healed by command). On a pool on the
    platform: placed, boarded (right click), paddled with W, steered with A. The pool was at the platform's edge, so the
    boat went off it, 41 m down onto the trees; the boat survived and so did the player at 20 (a boat's fall, vanilla).
  - **The Nether:** an obsidian frame (by command), lit with flint and steel (real click), walked in, stood ~4 s: "Loading
    terrain", then the Nether. (A first frame one block up the floor needed a jump, and the jump went straight through.)
    G to hero mode there: Kiriko's HUD 250/250, 10/10, 15/15.
  - **A Nether finding (vanilla, not ours):** standing in the exit portal (portal block at eye height), 4 + 3 kunai at a hoglin and
    a zombie 5 m off did nothing: a projectile born inside a portal block is carried through it (arrows too). Out of the portal,
    on a platform, one burst took a hoglin 40 -> 1.6.
- **Not tried:** a second player (still blocked on memory: harness.md), the End, a raid, Minecraft-mode combat with a sword.

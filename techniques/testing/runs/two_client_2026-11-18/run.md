# Two-client pass, 2026-11-18 (0.3.88): the second-player leftovers of #146, #138, #142

Up: [Techniques](../../../README.md) · [harness](../../harness.md) · [previous pass](../two_client_2026-11-14/run.md)

Base `next` c137545 (0.3.88). Overwatch and Battle.net checked closed before each launch; one lock holder
(`E:\claude\mods\.oc_client_lock`). Client 1 `runClient` ("Kiriko", host), client 2 `runClient2` ("Viewer"), flat survival
world `tc88`, Easy, `spawn_mobs false`, time 6000, opened with `/publish false 25565`. Input: harness only (two.py), as in
the last pass.

**Why it fits now: no pack.** Measured on one client in a world: 8.5 GB committed with the full local pack (heap 3 GB, near
full; the JVM tracks 3.6 GB, the rest is the graphics driver's copies of the textures and buffers of all 344 models, loaded at
every reload), 2.7 GB with an empty pack. Two no-pack clients fit beside the owner's other apps (5.9 GB free with one up).
Client 2 later ran on a slim pack: the pack's `pack.mcmeta` and its 87 `textures/effect/cat_*` images (1.9 MB), copied to
the session scratchpad, never the repo: enough for Jetpack Cat's rope.

## #146: Torbjörn's turret leaves an allied player alone
Torbjörn (client 1) threw the turret (Shift) to land 2 m from Cassidy (client 2), no teams (co-op), a still husk 10 m off.
Over 6 s Cassidy stayed at 20/20 (read at 0, 2, 4 s; `tc88_turret_c2.png`), and the turret took the husk 20 -> 10.08. The
turret isn't drawn without the pack (its model is the pack's): its shots are the server's, which is what was under test.

## #138: Infra-Sight seen by an ally
Widowmaker (client 1) and Cassidy (client 2) both faced a 4-high stone wall with a still husk behind it. Infra-Sight (Q, the
ult filled with the harness `ult 100`): client 2 drew the husk's red outline through the wall (`run2/screenshots/tc88_infra_c2d.png`).
(Two misfires first, both setup: `ult 100` right after `changehero` used the old hero's cost, so Q found the ult short; then
the turret had already killed the husk, so there was nothing to reveal.)

## #142: an enemy's Catnapper rope seen by the carried player
Teams red (client 1) and blue (client 2). Jetpack Cat cast Catnapper (Q, then the left button) aimed at Cassidy's feet 6 m
off, then held jump. The landing hit Cassidy (20 -> 19) and the carry lifted client 2 to y -45 under her (its own `state`).
- With no pack, client 2 saw her overhead and no rope: the rope's strips are drawn with her effect textures from the pack
  (`cat_rope_braid`, `cat_glow_noise`, `cat_soft`, `cat_cloud`), so nothing to draw: not a bug.
- On the slim pack, client 2 in third person (`run2/screenshots/tc88_rope_slim1.png`): the rope from her feet to a hook glow over
  its head, in the enemy palette from her data (`CAT_TETHER_ENEMY`: red through orange to a yellow-white core), and the
  landing's shockwave on the ground.
- (A first cast aimed 5° down dove ~18 m, past Cassidy: the dive point is where the view meets the ground.)

**Not tried:** a full pack on either client (doesn't fit), real mouse and keys on client 2, a dedicated server for the carry.

## #142: Widowmaker's Grappling Hook off a Jetpack Cat's rope (fixed in 0.3.89)
No teams. Jetpack Cat (client 1) switched Lifeline on (Shift); Widowmaker (client 2) 2 m off caught it (Interact); the cat
flew up (jump held) towing her, then she grappled a stone wall 12 m ahead (Shift). The cat's synced `mode` reads the tow
(`MODE_LIFELINE` 16 + `MODE_TOWING` 8).
- **Old build (0.3.88):** mode stayed 24 through the pull; she reached x 7.9, held back by the rope.
- **Fixed (`JetpackCat.letGo` as the pull starts, `Widowmaker` hook attach):** mode 24 -> 16 (Lifeline still on, the tow
  ended) and she reached the wall, x 13.7.

## #142: Lifeline on a dedicated server, two real clients (0.3.91)
`runServer` (world `potgserver`, driven over RCON) with both clients connected (no pack). A stone platform at y 149. Jetpack
Cat (client 1) switched Lifeline on, allied Kiriko (client 2) caught it (Interact), then the cat held forward and Frenetic
Flight (right button) for 4.2 s. Positions polled every ~0.3 s on each client:
- The towed player went 19-29 m/s for about 3 s (23 m/s sustained), 70 m across and up to y 158. Full health throughout.
- Then she let go (jump) at y 160: fell 85 m to the ground (y 75) and landed unhurt (20/20); the cat's tow ended (mode 24 -> 16).
- The server log since the start of the tow: no kick, no "moved too quickly", no "moved wrongly".

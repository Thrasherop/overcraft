# #109: every speed the server sets on a player goes through Combat.send (2026-11-13)

Up: [Techniques](../../../README.md) · [polish3's #109 pass](../../polish3.md#109-pushes-on-a-real-client) · lesson:
[minecraft_26](../../../minecraft/minecraft_26.md)

Branch `fix/push-sync` from `next` 60de166 (0.3.43). One dev client (`runClient`, `OC_ASSET_PACK` the main checkout's pack),
fresh flat worlds `push109a` (base build), `push109b` and `push109c` (fixed builds), survival, peaceful, slimes killed. The real
player is the one moved: keys through the harness's key mappings (`key ultimate`, `key forward`, `key secondary_fire`), `trace`
per client tick. Trace files here keep the first seven columns: tick, horizontal m/s, (unused), vertical m/s, y, ground, keys.

## The sweep
Since #93 every push on another creature already goes `Combat.push` / `launch` / `send` (Soundwave, Concussive Blast and
Pharah's rockets, Junkrat's mines and frags (his own jump too), Accretion, the spheres' pull, Hyper Strike, Shield Bash and
Rally, Whip Shot's hold, D.Va's Boosters, Call Mech's landing and Self-Destruct, Purr, Catnapper's landing push, Lifeline's
Jump, quick melee, Junkrat's root, Tracer's Recall). polish3 played them on a real client. Left outside it:

| Place | Was | Now |
|---|---|---|
| Lúcio, Sound Barrier's leap (`Lucio.java`) | server's own speed, `syncVelocity` | `Combat.motion` + `Combat.send` |
| D.Va, eject (`Dva.java`) | her own copy of the packet, `syncVelocity` left set | `Combat.send` |
| D.Va, Call Mech's stop and the mech's arrival | `needsSync` only (never reaches her own client) | `Combat.send` (the stop from `Combat.motion`) |
| Sigma, Gravitic Flux's lift and hold | `Accretion.sendSpeed`, a second copy of `Combat.send` | `Combat.send`; `sendSpeed` removed |
| Jetpack Cat, Catnapper's landing stop | `syncVelocity` | `Combat.send` |

Left as they are, on purpose: Life Grip (its own route, `GripPayload`: 25 m/s in polish3), Lifeline's tow of a player (their own
client pulls them, `ClientTow`), Catnapper's windup, dive and carry (a hold every tick on `syncVelocity`: played in polish3 and
here), Sombra's Translocator (a teleport, whose packet carries the speed), projectiles' own `needsSync` (not players), Minecraft's
own knockback (its hurt sets `syncVelocity`; `LivingEntityMixin` starts a hero's from `getKnownMovement`). Juno, Kiriko, Sojourn,
Cassidy and Lifeweaver's Petal Platform set no player's speed on the server.

## Reproduced (base build, `push109a`)
- **Lúcio's leap while running** (W held 0.8 s, then Q): expected his 5.5 m/s run kept through the 6 m/s leap. Seen
  ([lucio_before2](lucio_before2.txt)): **5.50 -> 0.64 m/s** the tick he left the ground, back to 5.50 only 6 ticks later
  (1.60, 2.57, 3.53, 4.49, 5.36). The server's copy of his speed has no run in it (his client moves him), and that copy went out.
  Standing, the leap was right (5.13 m/s up the first tick, top y -58.97: [lucio_before1](lucio_before1.txt)).
- D.Va's eject: right already (5.97 back, 6.12 up: [dva_eject_before](dva_eject_before.txt)). Call Mech while running: stop in
  2 ticks (5.50, 2.75, 0: [dva_call_before](dva_call_before.txt)), the second being her client's own friction.

## After (fixed build, `push109b`; Lúcio picked by clicking his tile twice on the select screen)
- **Lúcio's leap while running, twice:** 5.50 m/s every tick through the leap, same rise (5.13, 4.25 ... top -58.97)
  ([run1](lucio_after_run1.txt), [run3](lucio_after_run3.txt)). Standing: unchanged ([still](lucio_after_still.txt)). A second
  try in run 2 didn't fire (Sound Barrier pressed again too soon); not counted.
- D.Va's eject: identical, 5.97 / 6.12 ([after](dva_eject_after.txt)). Call Mech while running: 5.50 then **0** the next tick
  ([after](dva_call_after.txt)); the mech arrived (mode 0, 54 health).
- Gravitic Flux from a bot Sigma (team blue, the player red, `bot press sig2 ultimate`, cast by its own 5 s): the player rose
  from y -60 at tick 44, still rising at -55.9 when the trace ended ([flux_after](flux_after.txt)). (Played as Lúcio: a hero
  change after a `/tp` away from spawn was refused silently, `state` showed it.)
- Catnapper from the air (jetpack up 1.2 s, aim 40 down, Q, left button): dive 24.6 m/s across and 17.2 down; landing with
  `syncVelocity` slid on 2 ticks (24.64, 24.37: [cat_final_air](cat_final_air.txt)); with `Combat.send` 1 tick (24.59, then 0:
  [cat_after_air](cat_after_air.txt)).

## Not settled
- **A Catnapper confirmed while standing on the ground went nowhere** in both of today's builds ([cat_final_ground](cat_final_ground.txt)
  is the syncVelocity one), though once on the base build in `push109a` it moved 3 m (2 ticks at 30 m/s). Not from this change;
  not looked into (the server's `onGround` ends the dive at once?).
- No second real client: pushes on another player were read on the real player only, from a bot.
- Minecraft mode isn't involved (none of these abilities run there).

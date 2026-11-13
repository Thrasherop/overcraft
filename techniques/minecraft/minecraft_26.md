# Minecraft 26.3: engine facts that cost time

Read Minecraft's decompiled 26.3 sources for every API (`gradlew genSources`; the sources jar is under
`.gradle/loom-cache/minecraftMaven/`): names and signatures changed a lot (GuiGraphicsExtractor, RenderPipelines,
`PoseStack.mulPose` takes a matrix, knockback takes a damage source). Up: [Techniques](../README.md) · drawing:
[rendering](../rendering/rendering.md) · building: [building](../building/building.md)

## Input
- **26.3's input codes are SDL's, not GLFW's** (A = 4, W = 26, Space = 44, right mouse = 3): always use
  InputConstants' names, never remembered numbers (a `<= 1` mouse-button check silently failed).
- **A second key set:** KeyMappings made but not registered with Options still get key events, stay out of the
  Controls screen, and can be silenced or swapped with a mixin on isDown/consumeClick.

## Gameplay
- **A per-hero body box** (Sigma, 2026-11-02: [sigma](../heroes/sigma.md)): override `Avatar.getDefaultDimensions(Pose)`
  (players don't override it; the eye height rides in `EntityDimensions.withEyeHeight`), and call `refreshDimensions()` from
  `Player.tick` whenever the box Minecraft holds isn't the one wanted. Two traps: the client's own player kept Minecraft's box on
  rejoin and after G while the server's switched (the refresh only stuck once a crouch changed the pose), so compare the held
  box with the wanted one every tick and read the local player's hero from the client's own state, not only the synced
  attachments. And `InventoryScreen.extractEntityInInventoryFollowsMouse` centres the entity on half its *current* box: a taller
  box moves every previewed hero down unless the offset is corrected by `(height - 1.8) / 2`. `Player.updatePlayerPose` stands
  a player only where the standing box fits, else crouches, else swims: grow a box only where its crouched size fits, or a
  switch in a tunnel suffocates.
- **Explosions push apart from their damage:** `ServerExplosion.hurtEntities` pushes with `pushFromExplosion` and, for a
  player, through `hitPlayers` (their own client applies it), scaled by `EXPLOSION_KNOCKBACK_RESISTANCE`, not
  `LivingEntity.knockback`. A knockback resistance has to scale both (`ServerExplosionMixin`), and a mob's melee hit goes
  through `knockback` (0.4, vertical capped 0.4 on the ground).
- **Slows are speed modifiers in Minecraft's order** (`AttributeInstance.calculateValue`: base + flat, then the base's shares,
  then each total share multiplied): Slowness is -0.15 a level of the total, Local Anesthetic a share of the base. Resisting
  "slows received" means recomputing the value with the negative ones scaled (`Tank.resistedValue`), keeping a hero's own.
- **Synced Fabric attachments** must be registered at mod start, not lazily from an event.
- **Minecraft's projectile hits ignore the projectile's size:** `ProjectileUtil.computeMargin` widens the target boxes
  from 0 to 0.3 m over the first 8 ticks. An Overwatch radius needs that margin replaced (`ProjectileUtilMixin`). Test
  with a still mob and shots aimed just inside and just outside the radius.
- **Projectiles pass through blocks with no collision box** (buttons, plates): the hit lands on the block behind.
- **The Ender Dragon is hit through its parts** (EnderDragonPart, not a LivingEntity), and only with AI on: a summoned
  NoAI dragon never places its parts.
- **The hurt cooldown is `LivingEntity.damageCooldownTime` in 26.x** (public), not `invulnerableTime`, which is now
  spawn and portal protection. Clearing the old field let every hit after the first in half a second drop: Jetpack
  Cat's 5-shot volley did 4 to a zombie and full healing to a villager (2026-10-28). `Combat.damage` clears the new one.
  Test any multi-hit weapon on a mob (not just an ally), one volley at a time, health read before and after.
- **The server's velocity for a player is stale:** the client moves the player, so `getDeltaMovement()` on a
  `ServerPlayer` isn't the real motion; `getKnownMovement()` is (what the client reported). Two bugs from it: a rope
  pulling an ally by her stale speed barely lifted it, and Minecraft's airborne knockback keeps the target's stored
  vertical speed, so a zombie's hit threw a flying hero down at 22 m/s (`LivingEntityMixin` now starts a hero's
  knockback from the known movement).
- **A speed set on a player reaches their own client only through `syncVelocity`** (the old `hurtMarked`): 26.3's
  `ServerEntity.sendChanges` sends a `needsSync` speed to the others watching (`sendToTrackingPlayers`), and only
  `syncVelocity` goes `...AndSelf`. Sigma's Gravitic Flux lifted a mob but left a lifted player's own client on the
  ground (y -60.00 for 4 s, the lift's sounds heard), found in his release play (2026-11-02, [sigma](../heroes/sigma.md)).
  Vanilla hurt knockback sets it for you; a lift or push on a no-knockback hit doesn't. **Setting `syncVelocity` wasn't
  enough either** (played): the server's copy of a player's speed is their client's to set (each movement packet on the
  ground zeroes its fall), so a lift that reads it back never builds up, and a hit's own `syncVelocity` sends that stale
  copy a tick after your push and undoes it. What worked (`Accretion.sendSpeed`): send `ClientboundSetEntityMotionPacket`
  to the player at once, clear `syncVelocity`, and keep a lifted player's speed in your own record. Since #93 every hero's
  push goes that way; played on a real client, per blow, in [polish3](../testing/polish3.md). (`LifeGrip` has its own route, `GripPayload`.
  A steady hold each tick is another case: Catnapper's carry lifts a real client on `syncVelocity` alone.)
- **NoAI mobs don't move at all** (no `travel` without effective AI): velocity, pulls and knockback do nothing to them.
  Test anything that carries, pulls or pushes on a mob with AI.
- **Hunger and saturation regeneration** run in hero mode unless turned off (#43); `natural_health_regeneration` is
  26.x's gamerule name.
- **A per-tick fix can lose to the frame:** F5 was undone each client tick, but frames drew between the key and the
  tick. Fix it at the source (the Options setter).

## The HUD
Fabric splits Minecraft's XP/locator bar in two: `INFO_BAR` (the background) and `INFO_BAR_FOREGROUND` (the XP fill and
the locator bar's dot for each other player). Hiding only the first left a coloured square by the ult meter that
showed only with a second player on LAN. Test HUD hiding with two players.

## A camera of our own
(The emote orbit.) Inject at the TAIL of `Camera.alignWithEntity` and redo what vanilla does for third person with our
angles: `setRotation`, `setPosition` at the pivot, `move(-getMaxZoom(distance), 0, 0)` (the block collision comes
free). The camera type must still be a detached one, or Minecraft doesn't draw the player's own model. The mouse
reaches the orbit through the same `turnPlayer` redirect as the wheel's pointer.

**Aiming from third person** (2026-10-28). Minecraft's third-person camera sits on the player's line of sight, so the
point they aim at is always behind their own head: Catnapper's marker was drawn and never seen. While an ability holds
the view (`AbilityCamera`), the camera rises 1 m in its own up off that line (cut short under a ceiling), backs off as
vanilla does, then turns to the point the line meets. The target shows above the head, under the crosshair; plain F5
is left alone. Mind which point the server aims from: still the eyes, so the camera has to look at the eyes' point.

26.3's window is SDL3 (`org.lwjgl.sdl.SDLVideo`, `Window.handle()`), not GLFW: LWJGL's GLFW isn't on the classpath.

## What reaches the other clients (2026-10-29)
- **A plain field on an entity stays on the server.** Jetpack Cat's centre shot kept "centre" in a field; every
  client built it from the spawn packet as a paw shot, so its look never showed (since she was added). Anything a
  client draws from goes in `SynchedEntityData` (`PawEntity.CENTRE`, as `OfudaEntity.TARGET`).
- **A shot's velocity arrives a frame after the shot:** its first client frame has zero motion. Anything drawn on
  that frame from the motion (a muzzle facing along the shot) needs another source (her aim).
- **A state sent only as events misses whoever wasn't watching.** Jetpack Cat's Lifeline was announced on and off;
  a player who joined, respawned or came into range never heard of it (no prompt, no rope). On Fabric's
  `EntityTrackingEvents.START_TRACKING`, send that player the current state (`JetpackCat.showState`). It fires
  before the entity's own packets; send ids, not looked-up entities.

## A dead player keeps no chunk loaded (2026-11-13, [junkrat](../heroes/junkrat.md))
26.x has no spawn chunks: a dead player's chunk unloads about a second after the death, and an entity type built with `noSave()` is
lost with it, with no `remove()` call (Junkrat's mine and trap were gone after every death). Anything a hero places that should outlive
a death has to be saved with its chunk (its state in add/readAdditionalSaveData), and then needs its own rule for when it's stale.

## A plain vanilla server
For "the mod stays invisible" checks (#32):
1. Copy Loom's `~/.gradle/caches/fabric-loom/26.3/minecraft-server.jar` to a folder outside the repo.
2. Run it once with the Java 25 Gradle downloaded (`~/.gradle/jdks/...25.../bin/java -jar server.jar --nogui`).
3. Set `eula=true` (the owner accepted Mojang's EULA for this, 2026-10-26), `online-mode=false` and a port in
   server.properties.
4. Add an ops.json with the dev player ("Kiriko"; its offline UUID is the name-based MD5 of "OfflinePlayer:Kiriko").
5. Join with `dev "connect localhost:<port>"`.

## A fall is checked after the ground
A "this landing is safe" flag cleared when the server first sees the player on the ground can be gone by the time
Minecraft applies that fall's damage (it can come a tick later, from the move packets). Clear the fall distance while
the flag is up rather than relying on the flag at the damage event (Mercy's landings: [heroes/mercy](../heroes/mercy.md)).
Again 2026-11-02 (D.Va's Boosters, [heroes/dva](../heroes/dva.md)): the server took her client's "on the ground" 2 m above
it, two ticks before she landed, and the cleared flag let a 12.8 m fall through (8.44 lost). Every new soft landing should
reset the fall each tick from the start; check this note before writing one.

## A ledge assist must also carry the hero over
Lifting a hero who presses into a ledge in the air (OwMovement.ledge) isn't enough: the ledge's face has already
zeroed the speed into it, so she falls back past the edge and is lifted again every tick, hanging there (#56). Lift,
step her over the edge, and give back the speed the face took. Measured on a 1-block step: every running jump from
0.05 s on gets over it.

## Damage over time on players: use a type in no_impact (#82, 2026-11-02)
- `LivingEntity.hurtServer` marks a hurt entity (`markHurt`) unless the damage type is in `minecraft:no_impact` (only
  `drown` in vanilla), and a marked player is sent the server's view of their motion, which for a player moved by
  their own client is stale: their speed snaps to it for a tick. A field hurting every 0.2 s with `minecraft:magic`
  (in `no_knockback`, so no push) still jolted a walking player 3-4 times in 3 s (5.5 -> 0 m/s for a tick; averaged,
  the 3.5 m/s seen in Sojourn's release play).
- Overcraft's own `overcraft:field` (data/overcraft/damage_type/field.json, added to bypasses_armor, no_knockback and
  no_impact) is the Disruptor's now: walking under it every 0.2 s never drops below 5.5 m/s. Same damage per hit as
  magic. Any later damage-over-time ability on players should use it (or a type like it).
- Testing it with one client: `/damage @s <n> <type> by <entity>` every 0.2 s while walking, speed read each tick.

## A held mob must be moved by hand (Sigma, 2026-11-02)
- `LivingEntity.aiStep` calls `travel` only when `isEffectiveAi()`, which is false for a NoAI mob: switching a mob's AI
  off (Sleep, a stun) also switches off Minecraft's movement, knockback and gravity included (the harness's "NoAI mobs
  can't be moved" trap, from the engine's side). A stun that should carry a push (Accretion's knockdown:
  `Sleep.knockdown`) or a lift (Gravitic Flux) moves the mob itself each tick: `mob.move(MoverType.SELF, v)`, then
  Minecraft's own drag (x0.91, x0.98 vertical, the block's friction x0.91 on the ground, `getGravity()`).
- `Entity.hurtMarked` is gone in 26.3: `needsSync = true` is what sends an entity's (and a player's own) motion to clients.
- A one-off hit that must ignore armour, Resistance and Protection: a damage type in `bypasses_armor` and
  `bypasses_effects` (the latter skips both the effect and enchantments: `getDamageAfterMagicAbsorb`), plus
  `no_knockback` / `no_impact` when the ability moves the target itself (`overcraft:gravitic_slam`).

## Pushing a hero player from the server (D.Va's eject, 2026-11-02)
- `setDeltaMovement` + `needsSync = true` on a **ServerPlayer in hero mode** did not move her: the eject's 6 back / 7 up
  launch left D.Va standing (`state` vy 0, same z), though the line above holds for mobs. Sending
  `player.connection.send(new ClientboundSetEntityMotionPacket(player))` right after setting the motion did: 1.4 m up,
  4.8 m back, 0.8 s in the air (7 m/s at 17.5 gravity, 6 m/s along). Use the packet whenever the server launches the
  player's own client (M: why needsSync didn't arrive wasn't traced). Up: [Techniques](../README.md) ·
  [heroes/dva](../heroes/dva.md)

- **A projectile's entity hit is where its centre is, not where it touches** (2026-11-02, D.Va's missiles): `EntityHitResult.getLocation()`
  for a thrown projectile is its own position, a radius off the target's box. A splash measured from there loses its
  full-damage ring on a direct hit (131 of 135 a volley); burst at the box point nearest that position instead. Block hits
  are fine (the centre ray's point on the face). [dva](../heroes/dva.md)
## Endermen dodge anything tagged is_projectile (#92, 2026-11-02)
- `Enderman.hurtServer` teleports away and takes nothing from a damage type in `minecraft:is_projectile`, whatever
  made it. Our hitscan weapons dealt `minecraft:arrow` (tagged), so endermen dodged Ana's rifle, Sombra's pistol and
  Sojourn's Charged Shot. They use `overcraft:hitscan` now (`Combat.hitscan`: the arrow type's other settings, not a
  projectile); real projectiles (Juno's rounds and torpedoes, kunai, darts) keep the dodge.
- Played: an enderman at 8 m took Sombra's pistol (40 -> 27.5), Ana's scoped shot (-> 34) and the Charged Shot
  (-> 25.6) in place; Juno's Mediblaster, the control, sent it 26 m away untouched.


## Hero hits don't push (#93, 2026-11-03; second pass after [critique_systems](../testing/critique_systems.md), 2026-11-04)
- **The cause:** every hero hit went through `LivingEntity.hurtServer`, which adds `dealDefaultKnockback` (0.4 and a hop on the ground) unless the damage type is in
  `minecraft:no_knockback`. A hit with no position (`overcraft:hitscan` with no direct entity) has `xd = zd = 0`, and `LivingEntity.knockback` then picks a **random** direction. Heroes use Minecraft's
  own types (`thrown`, `player_attack`), so tagging types couldn't fix it without changing vanilla skeletons' arrows. (The flail wasn't in it: `magic` is in `no_knockback`, measured 0.000 m before the
  fix too. The critique's "flail" was a reading.)
- **The rule (data):** the wiki's Status effect page, Knockback, lists what knocks enemies back; for our heroes Brigitte's Shield Bash and Whip Shot, Jetpack Cat's Purr and Catnapper, Lúcio's Soundwave, Sigma's
  Hyperspheres, Accretion and Hyper Strike, plus quick melee's "very small knockback". Its Headbutt was removed (BZ 2026-08-11, `tools/reference/jetpack_cat.md`). Lúcio's Sonic rounds aren't on it, but BZ 2024-07-12 says
  the knockback exists: their existence is sourced, their 0.6 m/s is ours. So: **a hit dealt through `Combat.damage` takes no Minecraft knockback and no Minecraft impact** (`Combat.heroHit`, asked by two
  `@WrapWithCondition`s in `LivingEntityMixin`). A hit player's view still tilts toward the shooter (`indicateDamage`). Minecraft's own hits (a sword in Minecraft mode, a zombie) are untouched.
- **One way to push (the second pass):** `Combat.push` (on top of what it's doing) and `Combat.launch` (replacing it). Both scale by `Combat.pushShare` (knockback resistance, a Stalwart's, Flux's, Valkyrie's, Glide Boost's, Jetpack
  Cat's: they read differently per ability before, so a resistant mob was thrown by Whip Shot), do nothing while phased, send it (`Combat.send`) and remember it for the kill credit (`Combat.pushed`). **A new ability that pushes uses these.**
  Whip Shot holds its own speed, and quick melee goes through Minecraft's `knockback`: each calls `send` and `pushed` itself.
- **Kill credit** (found by the critique, reproduced): `Combat.deal` credited a push by seeing the hurt move the target, which only Minecraft's knockback ever did. So after the first pass quick melee, Hypersphere pulls and the rock's splash
  lost it. Reproduced: a zombie quick-meleed off a 25 m ledge died with no feed row, no ELIMINATED, no line (`sys_rep_ledge1`). After: feed row and ELIMINATED (same picture, new build). The rock's burst off the ledge: eliminated, his elimination line. Whip Shot: the same.
- **A force on a mob, every tick (#89, `JetpackCat.tow`):** set `setDeltaMovement` from your own stored speed, not from where it really moved: without Minecraft's friction its AI's walk (`moveRelative`, 40 m/s² on the ground) adds up and walks it against the force. Take back only what a collision took. A force on a *player* belongs on their client (`ClientTow`): the server's each-tick push overwrites their keys.
- **Pushes on a player: verified on a real client with a bot attacker** ([polish3](../testing/polish3.md), 2026-11-05; not two real clients). `Combat.send` uses what Sigma's two-client play found: `ClientboundSetEntityMotionPacket` to the player at once, `syncVelocity` cleared (the tracker's goes a tick later, after their next movement
  packet reset the server's copy). `motion` starts from `getKnownMovement`, so Sonic rounds shouldn't bring back the #82 jolt, and Whip Shot sends a player its push once (each tick's send would overwrite their vertical speed with a stale one). All that is
  from the code. Not one push has been seen on a second player's screen since the first pass.
- **Measured** (a 200-health zombie with AI and `movement_speed` 0, `Pos` read through `/data`, three hits each; keys through DevServer):

| | pre-#93 (9f1c001) | now |
|---|---|---|
| Kunai | 1.99 m each | 0.000 m |
| Sombra's pistol | 3.4 m sideways with a hop in 0.1 s, then missing | 0.000 m over 1 s |
| Flail | 0.000 m | 0.000 m |
| Quick melee | 1.74 m (Minecraft's 0.4 and its own 0.15) | 0.50 m |
| Soundwave, Whip Shot, Purr | not measured | 3.91, 7.75, 3.41 m |
| Whip Shot, Purr at knockback resistance 1.0 | (2.34 m on a ravager, which walks: not a clean number) | 0.00 m, 0.00 m |

- **What a critic should check:** the two-client pass above; the Hypersphere pull: its burst on a wall 2.8 m past a ledge didn't take the zombie off it, so its credit is by code, not seen. A fight after this: see [hand_play](../testing/hand_play.md#a-fight-after-93-2026-11-04).

## Holding a mob still without its saved NoAI flag (#97, 2026-11-04)
- **The cause:** Sleep, stuns, knockdowns and Gravitic Flux called `Mob.setNoAi(true)` and put it back from a map in memory.
  `NoAI` is saved with the mob (`Mob.addAdditionalSaveData`), so a mob unloaded or saved mid-sleep (Save and Quit, its chunk
  left behind, a server stop, even an autosave followed by a crash) stayed a statue for good: `wake` ran on the removed object,
  after its save. Reproduced: dart a zombie, Save and Leave, rejoin: `NoAI: 1b`, 10 s after its sleep should have ended.
- **The fix:** the flag isn't touched. `MobMixin` answers `Mob.isNoAi` with `Sleep.aiHeld` (the very object put to sleep, by
  identity: the same mob loaded again is a new object, so it's awake; the client's copy shares the UUID and, in single player, the
  JVM, so the mixin is also server-side only). `isNoAi` rather than `isEffectiveAi` because a dozen mobs read it directly: the
  dragon's flight (`EnderDragon.aiStep`), the warden, shulkers, conversions in progress.
- **Three places read `isNoAi` to save or copy it**, and read the mob's own flag now (`Sleep.ownAi`): its save (`MobMixin`),
  a bucket (`BucketableMixin`, an interface mixin with `@WrapMethod`: the read is in a lambda inside the static method) and a
  conversion (`ConversionTypeMixin`). Grep `isNoAi()` in the sources when Minecraft updates: a new reader that copies it leaks the hold.
- **Played** (fresh world, a zombie with AI): asleep it stands (0.000 m in 2 s) and walks after its time (3.27 m), `NoAI` absent
  throughout. Save and Leave mid-sleep, rejoin: absent, it walks (4.45 m in 2.5 s; one rejoin it had killed Ana on the select
  screen, which is awake enough). Accretion's knockdown slid it and let it go. Gravitic Flux: Save and Leave at y -58.24 mid-lift,
  rejoin: absent, on the ground, walks 6.18 m. A mob NoAI of its own stays `1b` through a sleep. A slept pig struck by lightning: its
  zombified piglin has none. A slept axolotl caught in a bucket: `bucket_entity_data` has none. (Asleep confirmed by its bed
  sound in `heard`: `ana.sleep ->`, not `ana.sleep.cast`, which is only the shot.)
- **Threads (the critique's S1):** `Mob.addAdditionalSaveData` also runs on worldgen threads (`ProtoChunk.addEntity` saves a placed mob), so a plain counter for "read the own flag" could stick and stop every hold until a restart. It's a `ThreadLocal`, and the
  maps it reads are concurrent. Played on normal terrain (`sysN`): 8 places, 400 m apart in new chunks, a zombie slept at each while the world generated: asleep (bed sound), moved 0.000 m in 2 s, `NoAI` absent, every time. That shows the hold keeps working while exploring; the race itself can't be shown, its removal is by construction.
- **What a critic should check:** a world saved by an older build keeps its statues: nothing tells a leaked `NoAI` from a
  map maker's (`/data merge entity <mob> {NoAI:0b}` frees one). A sleep doesn't survive a reload by design: it just ends. Stun on a
  mob through Rally's bash shares this path and wasn't played on its own. Autosave mid-sleep followed by a crash: same save
  path, not played. A dedicated server: same code, not played.

## Who learns what when they start to see someone (#96, fix/polish-2, 2026-11-05)
A player far off gets no packet: `Effects.play` reaches 96 m and `Combat.animate` reaches those tracking. Anyone arriving later (walking up, joining, respawning) must be told, and there is one way in: `EntityTrackingEvents.START_TRACKING` -> `HeroManager.showState`. Every state others see is one of three kinds:
- **An attachment** (`HeroManager.HERO`, `SONG`, `MERCY`, `SIGMA_STATE`, `ANA`, `ASLEEP` ...): Fabric sends it on tracking by itself. Nothing to do; a new state should be this if it can.
- **A timed effect**, told once with its length in its variant (Nano's glow, a hack, a stun, Sound Barrier on an ally, Cyberspace, the Disruptor's field, Kitsune Rush's corridor): name it in `Effects.HELD` and, if something ends it early, in `Effects.ENDS`. `showHeld` resends it with what's left and the time it has run, under its state-only name: see the critique's answers below.
- **A held action or an emote**: `HeroKit.showState` (Brigitte's barrier, Jetpack Cat's Lifeline, tow, Catnapper's aim and Purr's wall, Juno's lock pose and ray); `Comms.showEmote` sends `emote:<id>@<ticks>`, so it joins the emote where it is (its sounds too, by `EmoteSounds` reading the time: read, not heard).
- Left out on purpose, over before anyone arrives or starting from nothing: bursts, muzzle flashes, Repair Pack's arrival (`brigitte_pack_arrive`: it is a burst and a held heal at once; replaying it would burst again), Mercy's launch, whips.
- **Played (2026-11-05, dev harness):** the viewer the real client, the hero a bot running its kit's own code, 400 m off so no packet could reach. With the rule off (`bot showstate off`) and on: an emote (`off_96_emote_arrive` standing, `a_96_emote_arrive` in it; `bot seen`: 5.10 s run after 3 + 2 s; gone once it had moved); Nano Boost (0 particles off, 10 on, `a_96_nano_arrive`); Catnapper's aim (`sinceAim` from the arrival); the barrier (base shot `b_96_barrier_arrive`: it already worked, from #88).
- **What needs two real clients and wasn't tried:** the machine holds one client. A hero played by a real second client, a viewer who is a real second client joining mid-state, and every kind of `HELD` effect one by one (only Nano was played).
- **Still not sent:** a viewer never sees a wall ride at all: `OwMovement.wallRiding` is the local player's only (`HELD_WALL_RIDE` goes to the server and stops there). That's not a catch-up: another player's Lúcio doesn't play it even in plain view: #115.
- **The critique's answers (2026-11-05, after the first version; raw output [g2](../testing/runs/polish2/g2_replay_ring.txt), before the fix [r2](../testing/runs/polish2/r2_before_replay_ring.txt)).** The first version resent each effect under its own name. Reproduced: two arrivals made two client rays (`bot rays` 2) and the ray restarted at its landing point; the rush's gates and tiles doubled (dome_noise 6 to 12, talisman 66 to 132). Baseline showed nothing there, so that was a regression. Now:
  - **Juno's ray** isn't in the list: it walks, so `OrbitalRay.showTo` sends where it is now and its age (`juno_ray_state`: no landing, its ramp from its age), keyed by Juno. Two arrivals: `rays` 1 and 1, `f_b1_ray_1`, `_2`.
  - **The rush:** `rush_gate_state`, `rush_tiles_state` draw only what this client hasn't laid (`KirikoFx.rushLaid`, by kind and place), no petals or sparkles. Two arrivals: 6 and 6 gates, 66 and 66 tiles, `f_b1_rush_1`, `_2`.
  - **On an entity:** a map by entity, so a repeat replaces; the time run comes in dx, so a client's fade-in doesn't start over.
  - **Each entry:** played with a real kit: Nano (first round), the ray, the rush. Played by `bot fx`, the kit's `Effects.play` call made by hand on a bot (two arrivals each, g2): ana_asleep, ana_boosted, ana_anti_healed, ana_nano, ana_numbed, brigitte_stun, juno_ring_boost, juno_ray_ally, sombra_hotfixed, sombra_hacked, sombra_revealed, sombra_weakened, sound_barrier_ally, sombra_cyberspace, sojourn_disruptor_field. **Those counts are particles within 4-8 m and carry over from the entry before** (each ran within seconds): they show no doubling on the second arrival (never more than a few over the first), not a clean count of each. Screenshots `f_held_*`, at thumbnail size only. **Not played:** sombra_hacked_thing: it needs a deployable, and its handler is the same line as sombra_hacked.
  - **Listed as not covered, on purpose:** sombra_strands (a 0.53 s channel, `STRAND_TIME`: over before anyone arrives, and it has no time in its variant), brigitte_pack_arrive (burst and held heal in one), bursts.
  - **Juno's lock pose:** `Juno.showState` sends `torpedo_lock`; `f_96_torpedo_pose` was taken in the ray and doesn't show it well: sent, not seen well. **Purr's wall:** `purr_state`; played with a bot Cat's press, read from fxnear only (`f_96_purr`), not looked at closely.
  - **A looping emote** replays from its time on a return within 0.15 m (g2: 2.30 then 4.65 s): that's what the rule means to do. Past 0.15 m the record goes.
  - **`off_112_grip_*`** are the same script's Life Grip shots from the session with #96's rule off; the rule has nothing to do with the grip. Ignore them.
- **What a critic should check:** an effect whose client holds a state by its variant and isn't in `HELD` (grep `e.variant() / 20f` in `*Fx.java`); `Effects.note` keeps a strong reference to entities until `over`: fine for its seconds, but check `over` covers a removed one; an emote replay's first frames (no ease in).

## A float or a lift a player must feel (polish3, 2026-11-05)
A stun's gravity scale reached mobs only (`Sleep.slide`). What changes how a player falls belongs on their client: Minecraft's `Attributes.GRAVITY` as a transient modifier syncs to it, and hero mode's `OvercraftClient.gravityScale` reads it against the default 0.08. Hyper Strike: top y -59.52 to -56.19. Mind what it does to the landing: a longer float is a longer fall (`Sleep.FLOATED`).

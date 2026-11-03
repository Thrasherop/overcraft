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

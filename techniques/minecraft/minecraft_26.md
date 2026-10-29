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

## A ledge assist must also carry the hero over
Lifting a hero who presses into a ledge in the air (OwMovement.ledge) isn't enough: the ledge's face has already
zeroed the speed into it, so she falls back past the edge and is lifted again every tick, hanging there (#56). Lift,
step her over the edge, and give back the speed the face took. Measured on a 1-block step: every running jump from
0.05 s on gets over it.

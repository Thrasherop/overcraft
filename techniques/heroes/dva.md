# D.Va: what she needs from fix/systems (#93, #97, #98, #100)

Written from `fix/systems` for whoever merges `feature/dva` (worktree `overcraft_dva`, branched before these commits, not edited from here). Her branch has its own
`techniques/heroes/dva.md`, her hub: on merge keep both, and put this list under it until it's done. From the critique's M8
([critique_systems](../testing/critique_systems.md)), read against `overcraft_dva` on 2026-11-04. Up: [Techniques](../README.md) · the rules:
[minecraft_26](../minecraft/minecraft_26.md#hero-hits-dont-push-93-2026-11-03) · [hand_play](../testing/hand_play.md#leaving-play-one-hook-four-doors-100-2026-11-04)

## The list
- **`leavePlay` on her kit** (`HeroKit.leavePlay`, `HeroManager.outOfPlay`): today's body of `Dva.heroMode(player, s, false)` (`stopMechKit`, the mech's health stored, Call Mech cut), and
  `Dva.clear` when `why.forGood()`. On death: the pilot's state, and a flying Self-Destruct (`dvaSelfDestructId`). The rule, ours and open for the owner: G ends whatever is running and charge
  already spent stays spent, so a Call Mech or Self-Destruct under way ends on G.
- **`HeroManager` conflicts:** her G hook in `setHeroMode` (lines 207-209 on her branch) and her two `if (state.hero == Hero.DVA)` branches (291, 403) go: `select` and `onLeave` call `outOfPlay` only.
  The G hook for "on" (getting back in her mech) has no `leavePlay` to go to: keep it, for `on` alone.
- **`DVA_FORM` goes in `HeroManager.HERO_LOOKS`,** so a switch or a leave takes it off.
- **Every push goes through `Combat.push` (or `launch`):** it applies knockback resistance and every state's share (`Combat.pushShare`), none while phased, sends it to a player's own client
  (`Combat.send`) and records the kill credit (`Combat.pushed`). Don't hand-roll `Tank.knockbackFactor`, `setDeltaMovement` and `needsSync`:
  - `Dva.java:426-427` (Call Mech's landing): `needsSync` only; it relied on `markHurt`, which hero hits no longer do, so a player landed on wouldn't move.
  - `Boosters.java:124`, `SelfDestruct.java:116`: the same transport question; and no `Combat.pushed` anywhere in `dva/`, while Boosters, Self-Destruct and Call Mech are all on the wiki's knockback
    list. Without it, a fall or the void after her push isn't her kill (#84).
- **`MicroMissiles.java:140, 167` "undo Minecraft's push" is dead:** hero hits take no Minecraft knockback now. Remove it.
- **The `Blow` argument:** next's `Combat.damage(target, source, owDamage, blow)` takes what dealt it (kill feed); her calls (`FusionCannons:169`, `Dva:422`, `Boosters:115`, `SelfDestruct:104`,
  `MicroMissiles:138, 166`) don't pass one yet.

## What a critic should check
A mech pushed off a ledge and an ejected pilot through G, death, a switch and a leave, each one played; her body box after each (`HeroBody`). Whether a held AI (`Sleep.aiHeld`) on her `WRECK` makes sense: she's a player, so it doesn't apply.

## What she needs from the reticle widgets (#85, fix/reticle-ui)
Written from `fix/reticle-ui`, not built here: her widgets are on her own branch. The system is `client/hud/ReticleHud` ([reticle](../hud/reticle.md)); add a `case DVA` to its `render` and `crosshair`, and her values to `Dva.widgets` (`HeroKit.widgets`). From the audit, D in `research\dva\work_kit`:
- **Defense Matrix under the crosshair:** 04F0 with resource 02CF (`decomp_02CC.txt` 557, 888); today a weapon panel. `resourceBar` with its share.
- **Boosters:** 0237 and 00DB, "[Shift] cancel" (`decomp_047E.txt` 720, 733): `timedBar` from `activeLeft` (`Dva.java:486`, synced, unread) and `prompt`.
- **Self-Destruct warning:** 0031 at the mech + 2 m for anyone within 21 m, texts 947F / AAD9, texture 4EA7 (`decomp_0B2F.txt` 476-553): a world widget, for every player near, not only hers: not in `ReticleHud`; project it as `targetMarker` does.
- **Crosshairs:** separate in the mech and as the pilot: Fusion CF, Light Gun E0; the Light Gun's ammo widget 02AB (`decomp_050A.txt` 382, `decomp_02B8.txt` 516 / 533). `crosshair` falls to the "+" until she has a case.
- **As the pilot:** the mech's saved Self-Destruct charge (a second 01EE, `decomp_026A.txt` 1264) and the Call Mech 1 s lock.

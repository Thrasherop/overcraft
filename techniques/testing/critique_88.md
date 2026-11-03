# Critique of the #88 fix (Brigitte's Barrier Shield), commit aa22aaa on fix/brigitte-shield

Independent QA read, 2026-11-02. Read-only: the diff, `techniques/heroes/brigitte.md` ("The barrier fixed"), every
`run2/screenshots/brig88_*` image the record cites (and the ones it doesn't), the harness scripts `brig88_*.py` and the
client logs `brig88_client*.log` in the session scratchpad, and 0EE3 in `E:\Overwatch Archive\research\brigitte\graphs`
(Overwatch and Battle.net checked not running first). The game was not started. Up: [Techniques](../README.md);
the hero: [brigitte](../heroes/brigitte.md); the method: [hand_play](hand_play.md).

Ranked by severity. **Blocks** = should be settled before fix/brigitte-shield merges into `next`.

## 1. A creeper killed her after the fix, and the record doesn't say so (BLOCKS)
`brig88_client4.log` (the fixed build: the camera work at 20:33 already ran on it), the first night-fight run:
- 20:40:04 three creepers, two skeletons, a zombie summoned 10-18 m ahead; the script holds the right button.
- 20:40:10 **"Viewer was blown up by Creeper"**, six seconds in. The last position the script read was the zombie at
  z 198.16, *behind* her (she stood at z 200.5 facing +z), so she was aimed backwards at the zombie and a creeper reached
  her from the side or front. Whether the barrier was up, broken, or dropped by the script (it lowers it when something
  is within 3.5 m and the barrier is under 120) is unknown.
- 20:40:38 **"Viewer was slain by Enderman"** (the aim-at-nearest script likely stared one down), 20:41:58 "shot by
  Skeleton" (`brig88_night_death.png`), `brig88_weird.png` (the enderman death screen).

The record mentions only that the first try "aimed at dropped dirt items, not mobs, and she was shot from the side".
A creeper death is exactly the bug being fixed; it has to be reproduced and explained (barrier state, creeper position
relative to her facing and the 1.51 m rectangle, crater/fall) before "creepers no longer get through" is claimed.
The write-up rule (CLAUDE.md: "must see what was *not* tried", every step, what was seen) is broken by leaving three
deaths out.

## 2. The "between her and the barrier" rule protects people the barrier doesn't cover (should fix before merge)
`Barrier.crosses`: when the source `a` is behind the barrier's plane but ahead of her feet (`da <= 0`, in front of
her), it takes `at = a` and checks only `a` against the rectangle. The victim `b` only has to be behind the plane
(`db <= 0`): an infinite half-space within 10 m of her. So a creeper going off 0.5 m in front of her protects a villager,
wolf or player standing 5 m to her *side* (or behind-left) whose line to the blast never touches the barrier. With the
new once-per-blast rule (`sameBlast`), every such ally gets 0 and no push. Needs a check that `b` is behind *her* and
that the a->b way stays inside the rectangle's footprint (or at least that `b` is within the rectangle's lateral span).

Also on that rule: `up = a.y - brig.getY() >= -0.2`. A creeper's explosion centre is its feet; one a block lower than
her (she on a step, a slope, stairs, a slab) fails the height test and goes through. Not tried on any slope.

## 3. The blast is charged by whoever the explosion hits first (should fix)
`absorb` charges the barrier `Ow.toOw(damage)` for the first covered victim `hurtEntities` iterates, then 0 for the
rest. Minecraft's damage for a victim depends on that victim's distance and seen-percent, so the barrier pays what the
blast would have done to the first entity in the list: a villager 8 m behind (small) or her (large), in an order nobody
controls. The barrier should be charged from its own distance to the centre (or the largest of the covered victims'),
not list order. Not tested with an ally behind her at all (every barrier case in the table is her alone).

## 4. Wind charges / breezes now throw her through the barrier (regression, should fix)
The old `blocksBlast(entity, center)` zeroed the push from geometry alone. The new code zeroes it only if `absorb` ran
and set `Barrier.blocked`, which needs a hurtServer call with damage > 0. A wind charge's explosion
(`SimpleExplosionDamageCalculator` with entity damage off) never calls hurtServer, so its push now goes straight through
the barrier. Same for a blast whose damage rounds to 0 at its edge. Not tested (no breeze, no trial chamber).

## 5. Sigma's branch can no longer merge as written (blocks Sigma's merge, not this one)
`feature/sigma` (not yet in `next`) changes `Barrier.blocksBlast(entity, center, cause)`, calls
`dev.overcraft.hero.Barriers.blocksBlast` from it, adds `Barriers.absorb` at the top of `Barrier.absorb`, and its own
`ServerExplosionMixin` (HEAD `hurtEntities` -> `Barriers.blast`, Stalwart knockback scaling in both push wraps). This
commit deletes `blocksBlast` and replaces the push wraps with the `overcraft$shielded` set. Merged in either order it
conflicts in three files, and a mechanical resolution silently loses behaviour:
- Sigma's barrier never sets `Barrier.blocked`, so his barrier would stop a blast's damage but not its push.
- `covering()` (the new armor skip) knows only Brigitte, so a hit Sigma's barrier takes is still armor-shrunk on her.
- Two once-per-blast mechanisms (`Barriers.blast` at HEAD and `barrierBlast` per Brigitte) need one owner.
Whoever merges second must rewrite these together and replay Sigma's and Brigitte's blast cases. Note it on #68 and #88.

## 6. The barrier camera's framing hasn't been compared with Overwatch, and the data hints differ (not blocking, owner call)
- `brig88_after_back_held_9.png` / `brig88_after_back_turned.png`: 2 m back and lifted 1 m, she is almost off the
  bottom of the screen (only the top of her head and a shoulder), and the bottom-centre ult ring sits on her head. The
  barrier fills the middle third. Overwatch's shield view shows her upper body below the barrier. Nobody watched a
  Brigitte replay side by side.
- Node 857 (checked in 0EE3): the distance expression `m_6A79BD98` reads v44373 (shield-up stack), v6054 **and a
  constant 8.0**; the record treats v6054 alone as the distance. Another expression `m_D8142002` holds **1.04** with
  v41764 / v67256 and was not read: it may be the vertical offset, which the code sets to 1 m "ours". Fields 6.67 and
  2.0 are also unread. Confirmed: v6054 = v1261 (2), v1262 (4) in Rally (node 868), StateChaseVar #869/#870 with 0.4,
  StateWait #909 0.35, the 0.3 blends, `m_clientOnly` 1.
- `StateChaseVar`'s 0.4 is read as a duration ("over 0.4 s"); if it is a rate (units per second, as chase nodes often
  are) the Rally pull-back takes 5 s, not 0.4. Mark it M until settled.
- Mid-blend the view passes through her head: `brig88_cam3_in_40394.png` is her ponytail filling the screen,
  `brig88_night_3.png` the same at night. Each raise and each release shows ~0.1 s of the inside of her model.

## 7. Turn-in-place by measurement: one regression risk unchecked (cheap check, BLOCKS until done)
The name list held `idle_scoped` / `crouch_idle_scoped` (Ana's Take Aim, which spun her 120 degrees before they were
listed). The list is gone; the record's `turnclips` summary names Brigitte's, Sombra's, Mercy's, Lifeweaver's, Juno's and
Sojourn's clips but **not Ana's scoped stands**, and no screenshot shows Ana scoped (`brig88_stand_ana_*` is her plain
stand). Run `turnclips ana` and scope in from behind before merging.
Other points:
- The test measures first frame against last only. A sheet that turns and comes back, or turns past 180, reads small.
  Taking the largest angle over all frames is as cheap.
- Any base clip that reaches the branch is tested (jump/fall/land variants, `_shield`/`_stealth`/`_blaster` variants,
  Lúcio's `skate_stop*`): a clip that happens to turn 45 degrees is now frozen at frame 0 without anyone choosing it.
  The `turnclips` output per hero should be pasted into the record so a reader can see the list.
- `brig88_stands.png` shows each stand twice 1.2 s apart from the front: a clip that loops near 1.2 s looks identical
  either way. Weak evidence; Sombra, Mercy and Lifeweaver weren't played through their kits after (the record admits it).
- `TURNS` (static IdentityHashMap keyed by Clip) is never cleared: each resource reload leaks the old models' clips.

## 8. Evidence that doesn't show what is claimed
- **Not real input.** Every step went through the harness (`key primary_fire down`, `look`, `mouse 200 0`,
  `screenclick`, `/summon` with `NoAI`/`ignited`), and the night fight was a script aiming for her. CLAUDE.md asks for
  the real mouse and keys and play "like a player". There is no hand play in this record.
- `brig88_after_front_held.png` was taken on an earlier HUD build: it shows the crosshair in the F5 *front* view. The
  later front shots (`brig88_cam2_f5_1/3`) have none. The cited image doesn't show the committed code.
- `brig88_cam2_in_*` (the blend-in series): the world hadn't loaded after the teleport, all sky, no terrain. They
  show nothing about the blend.
- The bash's drop to first person isn't in any image: `brig88_cam*_bash_0.1.png` are all still from behind, and
  `_0.7` from behind again. Only the harness's `camera` trace supports "first person by 0.24 s".
- `brig88_respawn.png` is Sombra at night, not Brigitte (the record correctly cites `brig88_respawn_back.png`).
- "Creeper 3 m ahead, shield down: pushed 2.65 m before, 2 m after": the shield-down push changed and nobody explained
  why. Either it's noise (then say the spread) or something else changed.
- No "fix switched off" run for the camera or the armor change. The blast table's "before" column was the old build;
  fine for those.

## 9. Camera and input details found reading the code (not blocking)
- `ClientBrigitte.barrierView` foresees the barrier for 0.5 s from any right press with barrier health > 0 and not
  locked. Where the server refuses (an emote, the bash's lockout, a stun, an open screen if `input` still runs), the
  view pops out and back. Not tried.
- `HeroHud` now draws the crosshair whenever `AbilityCamera.active()` and the view is from behind: that includes
  Jetpack Cat's Lifeline and Catnapper views. Another hero's HUD changed; not looked at.
- F5 during the blend-out (`leaving`): `allowed()` lets it through, then `end()` sets the view back to `before` and
  throws the player's choice away.
- `Barrier.passing` is never set any more, so `LivingEntityMixin.overcraft$barrier`'s "the rest lands as a hit of its
  own" branch and the `passing ||` in the armor check are dead, and their comments are wrong.
- `Barrier.clear` now sends `shield_lower` on hero switch too. If the hero attachment changes before it lands, the new
  hero's animator gets a `shield_lower` action. Switching hero (H) with the shield held wasn't tried; only G was.
- Hits are charged at `hurtServer` HEAD, before Minecraft's invulnerability frames: two zombies hitting in the same
  half-second both cost the barrier where vanilla would ignore the second. (Older than this commit, but the new
  "stops whole" rule makes the barrier the only thing paying.)

## 10. What a player will hit that wasn't tried
- **A second player** (two clients): the START_TRACKING raise (compile-checked only), a second player behind her
  barrier taking a creeper blast, a teammate on a scoreboard team vs no teams (with no teams every player is her ally).
- **Two Brigittes:** `covering()` returns the first in `level.players()` order, not the barrier the hit really meets
  first. Each charged once per blast, separately.
- **Her own TNT / bed / anchor:** `source.getEntity() == brig` skips the barrier for every victim, so her own TNT hurts
  her allies behind her barrier.
- **Slopes, stairs, slabs** (the -0.2 height rule), **water** (barrier and camera underwater), **riding** (boat,
  horse, minecart: the camera's lift and distance, the barrier's facing from her yaw vs the vehicle's), **mid-air**
  (a creeper blast while falling), **ladders**, **the Nether** (ghasts in volume, a portal with the shield held: her own
  client re-creates her and only *other* watchers get the raise), **night and darkness** framing.
- **Improved Transparency / Fabulous graphics:** the translucent barrier 2 m from the camera, and the mid-blend frames.
- **Low frame rate and F1** (hidden HUD) with the camera on; **mouse sensitivity** in the shield view (the aim point is
  turned to; was aiming at a moving mob by hand tried?).
- **Creative / spectator**, **Totem of Undying** death with the shield up.
- **Breeze wind charges** (item 4), **charged creepers**, **end crystals** (a nested explosion inside the wrap resets
  `Barrier.blocked` for the outer entity), **Withers'** skull volleys.
- **Melee** still stopped at the barrier against the wiki's "melee goes through barriers" (the record flags it for the
  owner).

## Answers (2026-11-02, the rework; details in [brigitte](../heroes/brigitte.md#the-barrier-reworked-after-the-critique-88-2026-11-02-critique_88))
1. The 20:40:10 creeper death: she was Sombra (the switch hadn't taken; the script's output says so). The enderman and
   skeleton deaths are written down too. Not a barrier gap.
2. Fixed (behind and within the width only; the bottom 1 m under her feet); played with villagers beside and behind,
   on a step and on stairs.
3. Fixed: charged once by the barrier's own exposure before anyone is hurt (as Sigma's); played with a villager behind.
4. Fixed (push by geometry), and blast projectiles now burst on the barrier: a live breeze, faced, never moved her.
5. `Barrier.blocksBlast(entity, centre, cause)` is back with Sigma's signature; the merge steps are in the record.
6. Re-read: 1.04 is a field of view, 8.0 a conditional distance, 0.4 a time (M); rise 0.5 m (ours), her model hidden
   mid-blend. Not compared with Overwatch.
7. Ana's scoped stands are held (`turnclips ana`, scoped from behind); the test takes the largest turn over 16 samples;
   the cache is weak per model.
8. Replaced or withdrawn as listed; the push spread measured (2.65 / 2.00 / 2.00); hero switch, a portal and Improved
   Transparency played. No real-hand play (the tool can't hold the right button).
9. Foresight 0.3 s and not when emoting / asleep / in a screen; Jetpack Cat's crosshair looked at; F5 in the blend-out
   kept; the passing branch removed.
Also the owner's call: melee goes through barriers.

## Second pass (2026-11-02, the rework 50c8148..c548424)

Independent read-only check of the five commits on aa22aaa: the diff, the record's rework section, every
`run3/screenshots/brig88_r_*` image cited (and the two contact sheets), the scripts `brig88_t11-t15.py` and logs
`brig88_client4/6/7.log` in the scratchpad, Minecraft 26.3's sources (`ServerExplosion`, `Guardian`, `Ravager`,
`AbstractWindCharge`, `Player.hurtServer`), the wiki through its MediaWiki API (Barrier, Jetpack Cat, Lúcio; no personal
info sent), and `E:\Overwatch Archive\research\jetpack_cat\` (Overwatch and Battle.net checked not running). Not built
(another agent may use the worktree), not played.

**Verdict: don't merge yet.** Two blockers, both small; the rest of the rework holds up.

### Blockers, ranked
1. **`Barrier.isMelee` lets non-melee area abilities through, against the wiki.** It counts every `PLAYER_ATTACK` and
   every hit whose direct entity is its living attacker as melee. In the mod `playerAttack(player)` is also the source of
   Jetpack Cat's **Purr** (`JetpackCat.java:512`) and **Catnapper's landing** (`:670`), and of Lúcio's **Soundwave**
   (`Lucio.java:280`). The wiki's Barrier page lists what pierces barriers (melee, and named abilities); none of these
   three is on it, and their ability boxes carry no `ignores_barrier` (Jetpack Cat's Lifeline and **Claws Out** do, so
   Claws Out's wound ticks, `:284`, are right to pass). In Overwatch a barrier blocks Purr, Catnapper and Soundwave. Her
   data doesn't say otherwise: Purr's cast (5A7E node 210) is a plain `STUStatescriptModifyHealth` with knockback rings,
   no readable barrier field. The record noticed Purr and the ult and accepted it; Soundwave isn't mentioned. Vanilla is
   hit the same way: a guardian's beam (`indirectMagic(guardian, guardian)` then `doHurtTarget`), a ravager's roar (AoE
   `mobAttack`), a warden's sonic boom now all pass. Before this branch the barrier took those hits' damage. Fix: drop the
   `PLAYER_ATTACK` shortcut, mark quick melee the way the flail is marked (`meleeHit`, or an `overcraft:melee` tag), and
   give Purr / Catnapper / Soundwave a source with no direct entity (as `Combat.hitscan`), so the "direct is the
   attacker" rule doesn't catch them. (Their pushes, `e.push` / `setDeltaMovement`, never asked the barrier: true before
   this branch too; worth a line in the record.) PvP with teams only, but it changes two other heroes' kits with no
   source behind it.
2. **Still no play with real input** (CLAUDE.md: the real mouse and keys before a merge). The record says the desktop
   tool can't hold the right button. It can hold a key: rebind the barrier's mapping to a keyboard key in Controls and
   hold it with `hold_key`, with mobs walking at her so no mouse turn is needed; or the owner holds the shield for five
   minutes. Until one of those, the hand-play gate isn't met.

### The first critique's items
1. **Creeper death: accepted, not independently provable.** The log shows a Sombra session (20:36) before the 20:40
   fight and no hero switch in between; the `'hero': 'sombra'` line was the script's console output, not kept. The
   enderman and skeleton deaths are now written down. **New:** `brig88_client7.log` 21:39:19 "Third was slain by
   Breeze", 12 s after the x 3000 live-breeze run ended (21:39:07: the script cleared Resistance and released the button,
   the breeze still alive). Not in the record. Probably the barrier-down aftermath, but it has to be written down like
   the others.
2. **Resolved.** "Against it" now needs `b` within the barrier's width; the lowest crossing is 1 m under her feet.
   Played with villagers beside/behind, a step, stairs. `brig88_r_slope.png` shows only the stone platform before the
   test (no stairs, no creeper, barrier down): the set-up, not the result. The numbers carry it.
3. **Resolved.** `Barrier.blast` charges each covering barrier once at `hurtEntities`' head by `ServerExplosion`'s own
   formula at the nearest point (checked against 26.3's source). 294 at 3 m checks out (2.1 m from the barrier: 23.5
   Minecraft health). Is it Overwatch's rule? No source gives how a barrier takes splash; the wiki says only that
   barriers block blasts (Rip-Tire, Total Mayhem). A barrier closer to the blast than she is taking more than she would
   is what any distance falloff gives, so 294 vs 209 is consistent. Where it over-charges: (a) **full exposure ignores
   blocks**: a creeper going off behind a wall in front of her still drains it; (b) **no difficulty scaling**:
   `Player.hurtServer` scales `scalesWithDifficulty` sources before our HEAD hook, so on Hard she'd take 1.5x a blast
   unshielded while the barrier pays the Normal amount, and on Easy it pays more than she would; (c) any blast within 2r
   (6 m for a creeper) anywhere in front, even far off to the side, costs it at least 12.5 (Minecraft's "+1"). A near
   miss off the edge costs both the barrier (by its edge) and her (uncovered): right for Overwatch, where splash hits
   every exposed target. (a) and (b) are cheap (a block clip from the centre to the nearest point; the difficulty factor
   as `Player.hurtServer` applies it). Not blocking.
4. **Resolved.** The push is judged by geometry (`blocksBlast`), and explosions that hurt no one charge nothing
   (`shouldDamageEntity` false). Played with a live breeze.
5. **Resolved on this side.** `Barrier.blocksBlast(entity, centre, cause)` matches feature/sigma's call; the merge steps
   are in the record. Whoever merges second still owns the replay.
6. **Answered** (1.04 a field of view, 8.0 conditional, 0.4 M; rise 0.5 m ours). `brig88_r_frame.png`: head and
   shoulders under the crosshair, the ult ring on her back. `brig88_r_blend_in_1.png`: no hair, the barrier fills the
   screen from close. Side by side with Overwatch: still the owner's call.
7. **Resolved.** 16 samples, a weak per-model cache, the `turnclips` list pasted. `brig88_r_ana_scoped_*` show Ana held
   still in a lowered stance; the image itself can't show she is scoped. The list now holds every hero's plain `idle`
   (Lúcio's too), consistent with Kiriko's sheets (H), but Lúcio's stand wasn't looked at before/after.
8. **Partly.** Withdrawn images named; the push spread measured. Real input: blocker 2.
9. **Mostly.** Foresight 0.3 s; F5 in the blend-out keeps the player's choice; the passing branch is gone. But
   `ClientBrigitte.barrierView`'s "asleep or stunned (Sleep's attachment)" checks `mc.player.isSleeping()` (a bed), not
   the synced `HeroManager.ASLEEP` the server refuses on (`HeroManager.controls` -> `Sleep.asleep`): under Ana's Sleep
   Dart or Rally's stun a right press still pops the view out for 0.3 s. `mc.player.hasAttached(HeroManager.ASLEEP)`
   would do. Not blocking.
10. Not tried, as the record says: two players, two Brigittes, her own TNT, riding, water (a guardian's beam now passes:
    blocker 1), charged creepers, end-crystal chains (the state stack reads right; not played).

### What the rework added, checked
- **`Barrier.tickProjectiles`:** the owner goes through `stops`, so her and her allies' fireballs and wind charges pass
  and ownerless ones (a dispenser's) are stopped; only a step from in front of the plane to behind it, inside the
  rectangle, counts, so projectiles fired from behind her (allies' or enemies') never meet it. Fine. Small: the burst
  goes through `onHitBlock` at `BlockPos.containing(hit)`, which with the 1 m bottom can be the ground block (a burning
  fireball lights TNT there, a target block or bell reacts): harmless in practice. A small fireball's 62.5 isn't
  difficulty-scaled (as 3b).
- **Nested explosions:** `overcraft$outer` is pushed at HEAD and popped at every RETURN of `hurtEntities` (the
  radius-0 one too). An exception inside would leave it one deep: acceptable.
- **`s.barrierBlast`** keeps the last `ServerExplosion` alive (one object per Brigitte): negligible.
- **Compile by reading:** each commit stands alone (50c8148 server only; b18f449's camera API with both call sites;
  1948b3f self-contained). The shadowed fields (`level`, `center`, `source`, `radius`, `damageCalculator`),
  `Projectile.onHit`, the `hurtingprojectile` package and `Direction.getApproximateNearest` exist in 26.3. No unit test
  touches the changed code.
- **`build.gradle`'s `client3`:** a Loom run config only (`runClient3`, run dir `run3`, gitignored). It doesn't touch the
  jar, `remapJar` or `checkNoOverwatchAssets`: harmless for the shipped build.
- **The fall out of the world at x 5000** (21:46:53): the same session's log has 17 "Insufficient system resources"
  failures storing chunks (and one screenshot). Likely the machine, not the mod; the record should say so.

### To merge
Fix 1 (narrow `isMelee`, change the three abilities' sources, replay Purr and Soundwave on a Brigitte with teams, and
check a zombie and quick melee still go through), meet 2 (the barrier held on a real key against a creeper and a
skeleton), write down the 21:39:19 death. Then it can go into `next`. 3(a)/(b) and 9's sleep check can follow as issues.

## Answers to the second pass (2026-11-02, [brigitte](../heroes/brigitte.md#the-barrier-after-the-second-critique-88-2026-11-02-critique_88))
1. Melee is now only a marked blow (mobs' doHurtTarget but a guardian's, a player's own attack, quick melee, the flail);
   Soundwave, Purr and Catnapper's pushes and lockouts stop at a barrier with their damage. Played: a zombie through
   (30.5 each), an arrow, a guardian's beam and a warden's sonic booms on the barrier. The three hero abilities: not
   played (one client).
2. Real input: tried and blocked (no app match for the dev client; asked by path: denied). Needs the owner.
3. The 21:39:19 breeze death and the 17 chunk-save errors at the x 5000 fall are in the record.
4. The foresight checks Sleep's attachment. Lucio's stand: unchanged (`idle` was on the old list), looked at.

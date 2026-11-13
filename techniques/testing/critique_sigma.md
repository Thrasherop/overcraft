# Critique: Sigma (feature/sigma, #68) before merge, 2026-11-02

Up: [Techniques](../README.md) · the hero: [sigma](../heroes/sigma.md) · earlier audits: `qa_critique_2026-11-02.md`,
`qa_audit_2026-11-02.md` (on `next`) · Brigitte's: [critique_88](critique_88.md) (on fix/brigitte-shield)

An independent read of branch `feature/sigma` at 78e97d4 (22 commits over `next`, 115 files). Read-only: code, the step record,
the screenshots it cites, the wiki through its MediaWiki API (Sigma rev 408635, Barrier rev 406358, and the Kiriko, Sombra,
Lifeweaver, Ana, Juno, Jetpack Cat pages; a generic user agent, no personal data), and trial merges with `git merge-tree`
against `next`, `feature/dva`, `fix/brigitte-shield` and `fix/killfeed-hud`. No game was run and `E:\Overwatch Archive` was not
read. Every finding below is from the code or a screenshot, with its file and line; none of them was reproduced in the game,
so each needs a repro before its fix (CLAUDE.md).

**Verdict: do not merge yet.** Findings 1-4 make his barrier and Grasp wrong against most of the existing roster (they were
never played against those heroes), and the merges with Brigitte's #88 rework and the kill-feed branch will silently drop
behaviour or fail to compile unless they are planned.

## Ranked findings

### Blockers (wrong in the first real fight against another hero)

1. **Hitscan goes straight through his barrier.** Every hitscan weapon builds its damage source with no direct entity:
   `new DamageSource(ARROW, null, owner)` (`sombra/Sombra.java:588-591`, `juno/Mediblaster.java:147-151`,
   `sojourn/Railgun.java:360-364` (Charged Shot), `ana/Ana.java:636` (scoped shot and its dose ticks)). `Barriers.absorb` only
   treats a hit as a shot when `direct == attacker` (`hero/Barriers.java:315`), so `null != owner` and the hit passes. Sombra's
   pistol, Juno's Mediblaster, Ana's scoped rifle and Sojourn's Charged Shot all ignore Sigma's barrier from either side.
   Brigitte's barrier judges these from the attacker's eyes (`brigitte/Barrier.java` `from`), so it blocks them; his doesn't.
   The record's "blocks her rounds" (sigma.md:486, :508-517) is Sojourn's primary fire, which is a projectile entity, plus
   Kiriko's kunai and mobs' projectiles. No hitscan weapon was ever fired at his barrier.

2. **Most heroes' projectiles do 0 damage to his barrier, and Grasp gives 0 overhealth for them.** `Barriers.projectileDamage`
   asks `OwProjectile.barrierDamage()`, which returns 0 by default (`entity/OwProjectile.java:43-45`). Only Kunai, RailgunShot,
   Hypersphere and Accretion override it. These deal damage but return 0: `SonicEntity` (Lúcio's primary), `AnaDartEntity` (Ana
   unscoped, 75), `ThornEntity` (Lifeweaver), `BlasterShotEntity`, `PawEntity` (Jetpack Cat), `SleepDartEntity`,
   `BioticGrenadeEntity` and `DisruptorShotEntity`'s direct hit. Against Lúcio, unscoped Ana, Lifeweaver and Jetpack Cat the
   barrier takes their shots, never loses health and never breaks. Kinetic Grasp eats the same shots and converts 60% of 0. The
   wiki names Thorn Volley as "one of the highest damage outputs for support weapons" against barriers.

3. **Kinetic Grasp ignores hitscan.** Grasp only scans `Projectile` entities in its box (`sigma/KineticGrasp.java:72-110`), and
   nothing in `LivingEntityMixin` or `Combat` checks it. In Overwatch Grasp negates projectiles and hitscan alike: it is the
   standard answer to Deadeye and Tactical Visor, and the wiki lists only "Beam, Melee, Extended Melee or AoE attacks or Strong
   Projectiles" as what it can't block. Here, Sombra, Juno, scoped Ana and the Charged Shot hit him in full during Grasp. The
   record's "Grasp eats her rounds" (sigma.md:491) is Sojourn's projectile primary again.

4. **Grasp and the barrier eat "strong projectiles" and pass-through projectiles, and the owners never learn it.**
   - Grasp absorbs every enemy `OwProjectile`: `AccretionEntity` (another Sigma's rock; the wiki: "Accretion cannot" be absorbed),
     `PetalThrowEntity` (Petal Platform) and `TranslocatorEntity`. The wiki lists all three as strong projectiles Grasp can't take.
   - The barrier stops every enemy `OwProjectile` through the default `hitBarrier` = `discard()` (`OwProjectile.java:48-50`). The
     wiki's Barrier page says the Translocator beacon "can travel through barriers" and Petal Platform "acts as if it has hit a
     wall, bouncing off and deploying". Here both vanish.
   - The Translocator's removal hook only tells Sombra when the reason isn't `DISCARDED`
     (`TranslocatorEntity.java:127-128`), and Grasp and the barrier both call `discard()`. Sombra's state may keep pointing at a
     beacon that's gone, and the Lifeweaver's at a thrown platform that's gone. Their state after this hasn't been read or
     played.

5. **Gravitic Flux is stopped by enemy barriers. It should go through them and not damage them.** The lift and slam use
   `new DamageSource(type, owner)`, which makes Sigma both the direct and the causing entity (`GraviticFlux.java:289, :421`). From
   more than 3 m away, `Barriers.absorb`'s shot branch (`Barriers.java:315-324`) sends the lift's 50 and the slam's 50% into any
   enemy Sigma barrier between his eyes and the target. Brigitte's barrier also judges from the attacker's eyes, so it takes them
   too. The wiki: "Gravitic Flux ignores barriers but cannot damage them" (Barrier page), and its keywords include "barrier
   piercing".

### High (soon in normal play; or a merge that silently loses behaviour)

6. **The merge with `fix/brigitte-shield` (#88) will drop one side's barrier rules unless it's planned.** Conflicting files:
   `brigitte/Barrier.java`, `brigitte/Brigitte.java`, `hero/QuickMelee.java`, `jetpackcat/JetpackCat.java`, `lucio/Lucio.java`,
   `mixin/ServerExplosionMixin.java`, `overcraft.mixins.json`, and the notes `minecraft_26.md` and `rendering.md`. What can be
   lost:
   - **Which sides block.** Sigma's 8cec2ca makes Brigitte's `crosses` work from both sides. Brigitte's branch keeps front-only
     (`db > 0 -> false`) plus an "against it" rule (a source between her and the plane) and `BOTTOM = -1.0`. The two shapes don't
     merge line by line. Pick one rule (the wiki says both sides), then replay both heroes.
   - **Sigma's barrier in Brigitte's absorb and blocksBlast.** Her rewritten `absorb` (`covering` -> `return 0`) and
     `blocksBlast` -> `blocking` no longer call `Barriers.absorb` or `Barriers.blocksBlast`. If her version is taken whole, his
     barrier stops blocking blasts, blast pushes and shots (only the projectile interception in the entity's own tick is left).
     Nothing fails to compile.
   - **Overflow.** Brigitte's barrier now stops the whole hit that breaks it. Sigma's shot branch passes the overflow through
     (`Barriers.java:318-323`). One rule is needed for both.
   - **Melee.** Brigitte marks real melee (`meleeDepth`, `MeleeHitMixin`, `PlayerMeleeMixin`). Sigma uses "a player hit within
     3 m" (`MELEE_REACH`). With the 3 m rule, Soundwave, Purr and hitscan within 3 m pass his barrier while hers stops them.
   - **Area pushes.** Brigitte's `Barrier.shields` (no push or lockout behind it for Soundwave, Purr, Catnapper, Bash) checks only
     her own barrier. Soundwave's push still goes through his.
   - **Stalwart's knockback in explosions.** Sigma's `ServerExplosionMixin` scales both push wraps by
     `Tank.knockbackFactor * GraviticFlux.knockbackFactor` and calls `Barriers.blast` at the head. Brigitte's version has neither.
     Taking hers loses his 40% blast-knockback resistance and his barrier's blast damage, and both are silent.
   - **Wind charges.** Brigitte's `hurts` flag stops a breeze's wind charge from charging her barrier. `Barriers.blast` has no
     such flag: a wind charge that bursts beside his barrier (the ground next to it, not crossing it) charges it up to
     ((2/2)·7·2.4 + 1) = 17.8 health = 222 points. A wind charge hurts no one.
   - **Knockback scaling at merge points.** Brigitte's Bash push, Lúcio's Soundwave and Purr / Catnapper / Claws Out carry
     `Tank.knockbackFactor` on this branch. Her branch changes the same lines. If those multipliers are lost, Stalwart is
     half-built again without any test failing.

7. **The merge with `fix/killfeed-hud` won't compile.** That branch removes `Combat.damage(target, source, ow)` and
   `damagePart(...)` and requires a `Blow` (`Combat.java:84, :104` on that branch). Sigma has 6 such calls (Hyperspheres x2,
   Accretion x2, GraviticFlux.hurt, SigmaPerks). Git reports no textual conflict there, so the build breaks after a "clean" merge.
   Text conflicts are also listed in Ana, Anesthetic, Brigitte, SleepDartEntity, QuickMelee, JetpackCat, Mediblaster, Torpedoes,
   Lifeweaver, Lúcio, Disruptor, Railgun, Cyberspace, Sombra, `minecraft_26.md` and `harness.md`: almost all are Sigma's
   `credit(..., target)` against the kill feed's `Blow` argument on the same lines. Each needs both changes, or a tank's
   -40% ult factor (or the kill-feed tag) is dropped from that hero. His abilities still need their own tags (the audit's "every
   Sigma kill is a fist").

8. **The merge with `feature/dva` conflicts in `hero/Barriers.java`, `HeroManager.java`, `HeroState.java` and `harness.md`.** D.Va
   merged an older Sigma (9f35f8e, step 9). Her `Barriers.java` is the front-only version (`if (da <= 0 || db > 0)`), with her
   `Box` / `Eater` / `absorbBox` added for Defense Matrix. A careless resolve either reverts 8cec2ca (both sides) or drops
   `absorbBox` (and with it Defense Matrix). `HeroManager`: her `refreshHealth` / `owHealth(player)` / `layer(player)` rewrite of
   `applyKit`, against Sigma's additions. `HeroState`: field blocks side by side. Afterwards, Grasp and Defense Matrix should
   share one loop (D.Va's comment says Grasp's is "its own until Sigma's step 10 lands"; it landed and still has its own copy),
   and the hitscan gap (finding 3) applies to Defense Matrix's design too.

9. **A hack doesn't touch the barrier or cancel his channels.** `HeroManager.java:474-477` masks only the *presses* of ability 1,
   ability 2 and the ult. The barrier is driven by `secondaryHeld` (`SigmaBarrier.tick`), so a hacked Sigma can still send,
   stop and recall it. A Grasp, gather or Flux already running continues. The wiki: the barrier "is automatically recalled if
   Sigma is ... hacked", and Grasp, Accretion and Flux are channels that a hack interrupts.

10. **Asleep, stunned or knocked down, he loses the barrier.** `SigmaBarrier.tick`: `if (!controls ...) stow(...)`
    (`SigmaBarrier.java:50-52`), and `controls` is false whenever `Sleep.asleep` is true (`HeroManager.java:173-175`). Ana's
    Sleep Dart, Brigitte's Rally stun, an enemy's Accretion and Hyper Strike's float therefore *recall* the barrier and put it on
    its 1 s cooldown. The wiki: "Being stunned or knocked down stops the barrier at its current location, but will not cause an
    active barrier to be retracted." Only death and hack recall it.

11. **Mobs can be saved permanently NoAI by Flux, Accretion and Hyper Strike.** This is the critique's #8 (Sleep) pattern again,
    with new callers. Flux sets `setNoAi(true)` on every lifted mob every tick (`GraviticFlux.java:303, :394`). `tickAll`
    releases a removed entity *after* it was saved (`:350-352`), and `SERVER_STOPPED` calls `clearAll` after the final save
    (`Overcraft.java:237-240`). Save and Quit during an ult, or a lifted mob's chunk unloading, leaves it frozen for good.
    Accretion's knockdown and Hyper Strike's float add more callers to the same Sleep path.

### Medium (kit rules that differ from Overwatch's; visible to a player who knows him)

12. **Accretion and Flux don't cancel Grasp; they're refused.** `Accretion.press` returns if `KineticGrasp.active`
    (`Accretion.java:49`), and so does `GraviticFlux.press` (`GraviticFlux.java:84`). The wiki: "Accretion or Gravitic Flux can
    be activated to cancel Kinetic Grasp" (with no overhealth).
13. **He fires Hyperspheres while pushing the barrier.** `busy` = Grasp | gather | Flux (`Sigma.java:77`). Sending the barrier
    (`SENT` with the button held) isn't in it, so the left button fires during the send. The wiki: "Sigma cannot use his primary
    fire or normal abilities while deploying the barrier."
14. **Lifted players keep their abilities after 0.4 s.** A lifted player isn't in `Sleep.SLEEPERS`, so the server still gives them
    `controls`. Only their client's `ASLEEP` is set, and only for the hold (`GraviticFlux.java:302-306, :359-361`). The wiki:
    lifted targets are "rooted, disabling all movement abilities": no Swift Step, dash, slide or Guardian Angel out of it.
    (D's "0.4 s no abilities" is read as the hold. The rest of the 2 s should stay rooted.)
15. **The door rule is beaten by G.** `HeroBody.update` grows the box only where his crouched 1.4 m box fits, and keeps
    Minecraft's 0.6 m box otherwise (`HeroBody.java:96-108`). In a 1-wide doorway: G (Minecraft mode), step into the door, G
    (hero mode). He is Sigma with a 0.6 x 1.8 box and walks through, and along any 1-wide corridor, until he reaches open space.
    That undoes the owner's "Sigma should be unable to fit through a door". The record only tried G in tunnels he was already
    standing in. Possible fixes: push him out to the nearest spot that fits, or refuse the G until he's somewhere he fits (a
    decision for the owner).
16. **Levitation lifts him 28% higher than the wiki says.** It measured 6.38 m at 3.00 m/s (sigma.md:258-259). The wiki gives 2.3
    m/s and "up to a 5 meter height". The data reading is marked M (25 m/s² to a 3 m/s cap). A Workshop measurement, or W's
    numbers, before it ships.
17. **His splash falloff uses two different rules.** The spheres keep D's x0.5 at 2.85 m: 20 at the edge. W says 40 -> 12 at 2.9 m.
    Accretion uses W's x0.3 (12). Pick one source for both (`SigmaStats.java:39`, `:114`).
18. **The slam ignores temporary health.** `slamDamage(maxHealth)` (`GraviticFlux.java:422`). W: "50% of the target's maximum HP,
    which is increased by temporary HP, such as overhealth or armor". Overcraft's absorption (overhealth) and the heroes' armor
    layers are left out.
19. **Accretion has no self-damage or self-knockback.** "Never him" (`Accretion.java:113-115`). W: 20 -> 6 to himself and 5.8 m/s.
    The plan marks this Open. Say so in the HUD or code note, or decide it.
20. **His barrier's shape and size.** A flat 6.70 x 4.18 m rectangle, full width at every height (`SigmaStats.java:69`). W: 4.5 m
    tall, 5 m at the poles and 6.5 m in the middle. The corners block shots the real barrier wouldn't. A barrier sent into the
    void is never destroyed (W: "destroyed if it goes out of bounds"), and a barrier sent downward goes into the ground (open in
    the record).
21. **Blasts and bursts damage the barrier through walls.** `Barriers.blast` and the bursts' `enemyBarriers` loops
    (`Hyperspheres.java:134-139`, `Accretion.java:146-151`) charge any enemy barrier within the radius without a line-of-sight
    check, unlike the creatures they hurt (`inSight`).
22. **The Grasp box doesn't reach his feet.** It goes 1.6 m under his eyes, which is 0.46 m above his feet. W: "heightwise it
    extends downwards to the level of Sigma's feet." Projectiles aimed low (a Disruptor at his feet) aren't absorbed.

### HUD, kill feed, look (the audit's items are confirmed here and not repeated in full)

23. **The screenshots don't show what the record says they show:**
    - `run2/screenshots/v8_spheres_incoming.png`, cited as "her view of his spheres in flight" with "he drew all red in that
      frame, not looked into". It isn't Sigma. It's **the viewer's own Sojourn** in the front third-person view (no crosshair,
      her HUD), in Minecraft's red hurt tint. No sphere and no Sigma is in the frame. So her view of his spheres in flight is still
      unseen, and the "red Sigma" in the record's "Not yet" list is a misreading.
    - `run/screenshots/env_water_barrier.png`, cited for "the barrier deployed from the water". **No barrier is in the frame.** Only
      his empty left hand, the 650 on the panel and an orange Grasp box. Not shown.
    - `run2/screenshots/v8_flux_column.png`, cited for "Flux's rings and column" in her view. It shows the ground ring and a lifted
      husk, **no column** (compare the host's `h8_flux_lift_3p.png`, where the column is strong). Either the enemy copy of the
      column isn't drawn, or it's too faint to see.
    - `v8_emote_maestro.png`: Sigma is a few pixels tall at the far edge. It's no evidence of how his emote looks to others.
24. **The enemy-coloured barrier is lavender-pink with a yellow rim and quite opaque** (`v_barrier2.png`, `v8_kunai_barrier.png`:
    Kiriko's view is washed to near white-yellow over half the screen). Overwatch's enemy barriers are orange-red and readable.
    The record calls it "warm yellow-orange".
25. **The first-person sleeve covers the weapon panels.** In first person his forearm fills the bottom-right corner under the
    barrier and sphere panels (`r_join_hud.png`, `k_bar_1p.png`). The panels have see-through backgrounds, so "650" and the
    icons sit on a busy white and teal arm.
26. **The Swift Step flash after Grasp** (audit X5) is fixed only on `fix/killfeed-hud` (994a6a2). Until that merges, every Grasp
    flashes Kiriko's teal edge about 5 s later.
27. **Already in the audit (#84, #85, #91), still true on this branch:**
    - generic reticle, and no barrier health at the crosshair;
    - no Flux aim prompt and no 5 s timer; no Hyper Strike counter; Grasp's synced hold time never drawn;
    - every Sigma kill shows a fist (and see finding 7);
    - the hand spheres never play their clips (`HeroWeapon` names);
    - the 3P volley always plays `fire_b`;
    - the barrier's held orbs aren't on his hand;
    - the EXTRA sound layers (`move_3p`, `ambient.loop`, `hold_3p`, ...) are unwired;
    - his art exists only in `overcraft_ana`'s pack.
    - In third person (`k_hs_3p_fire.png`) the hand spheres can't be seen at all while he fires.
28. **The Flux reticle in first person reads as a grey slab** (`f_flux_aim_1p.png`): the cylinder's walls are a flat
    pale-grey plane across the screen, and the ring is grey-white, not Sigma's blue-violet.

### Multiplayer

29. **What a player who starts watching late gets:** the barrier entity (spawn packet plus `HEALTH`/`STATE`/`OUT_AT` data,
    tracking range 10 chunks), `SIGMA_STATE` (Grasp, gather, flight, aim, Levitation, Hyper Strike ready) and the box (worked out
    on each client from synced attachments). The one-shot effects aren't replayed: Flux's reticle and column, the lift's rings on
    each target, and an emote in progress (the shared #6 problem). An enemy who walks in mid-ult sees bodies rising with no
    effect.
30. **Not played with two players:**
    - an ally's shots through his barrier (both rounds put the players on opposite teams; `stops()` passes allies only while
      they're alive (`Allies.isAlly` checks `isAlive`), so an ally's arrow in flight is blocked if the ally just died);
    - his 1.4 x 2.5 box from the other client (hit tests run on the server's box; only a far-off frame of his model);
    - Hyper Strike on a player (known broken: friction eats the knock, the float is mobs-only);
    - Accretion's damage number on Normal;
    - his emotes' "others" sounds and props;
    - Brigitte's shield from behind with an ally in front.
    A lifted player rises 4.2 m against a mob's 6 (open).

### Process: what wasn't played the way CLAUDE.md asks

31. **No real input.** Every step drove the game through the harness: `key <action>` sets Minecraft's key mappings, `mouse` and
    rotation come by command, and the fights are scripts (`sig12\oc.py`, `r*.py`, `n*.py`). "Real keys" in the night fight means
    harness key events, not the OS keyboard or mouse. CLAUDE.md requires the real mouse and keys before a merge (Brigitte's #88
    at least recorded an attempt).
32. **The setups were dev setups.**
    - Perks were granted with `overcraft perks @s 3`, never earned from the 1955 / 4890 XP.
    - Ult charge was set by command (`f_flux_aim_1p.png`'s chat: "Set the ult charge of 1 hero(es) to 100%").
    - Mobs were NoAI, `movement_speed` 0 or summoned arrows.
    - Slows were applied with `/attribute`, not received from a hero.
    - Most emotes were played by the dev `emote` command.
    Nothing shows Flux charged in a real fight, perks earned, or Grasp used against a mob that chose to shoot.
33. **The interactions that matter most for a tank weren't played at all:** his barrier and Grasp against each existing hero
    (Lúcio, Ana scoped and unscoped, Lifeweaver, Mercy, Juno, Sombra's pistol and Translocator, Jetpack Cat, Sojourn's Charged
    Shot and Disruptor), and Flux against an enemy barrier. Findings 1-5 sit exactly there.

## What the record gets right (checked)
Health 200 + 150 + 250 = 600 (24 pips: 14 + 10); shields first; every number in `SigmaStats` names its source, and the
D-vs-W choices are written down; Stalwart's 0.6 knockback and slow math (`TankMathTest`); barriers blocking from both sides
(correct per the wiki); allies' shots passing (`Barriers.stops`); the bed-respawn fix (`HeroBody.update`); the sphere-core glow
fix (material 7C98); the late `syncVelocity` fix for a lifted player's own client.

## Before merging, at least
1. Hitscan into his barrier (judge null-direct hero shots from the attacker's eyes) and into Grasp.
2. `barrierDamage()` for every damaging hero projectile.
3. Strong and pass-through projectiles (Accretion, Petal Platform, Translocator) excluded from Grasp, and passed or bounced by
   the barrier, with their owners told.
4. Flux ignoring barriers.
5. Hack, sleep and stun rules for the barrier and his channels; Accretion and Flux cancel Grasp; no fire while sending.
6. A planned three-way merge with fix/brigitte-shield and fix/killfeed-hud (and later feature/dva), using the list in 6-8.
   Then replay both heroes' barrier cases against creepers, wind charges, Soundwave and a hitscan hero.
7. Play it with the real mouse and keys against each hero above, two clients, one as his ally and one as his enemy.

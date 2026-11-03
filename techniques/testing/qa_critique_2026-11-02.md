# QA critique, 2026-11-02 (working note)

A read-only critic's pass after the owner's play ("the QA is just sub-par in general ... it needs polish"; #84-#92).
Code, notes and play records only: no game was run, nothing under `E:\Overwatch Archive` was read. Every finding says
where it comes from; **(unverified)** marks a reading that still needs reproducing in game (CLAUDE.md: reproduce first).
Up: [Techniques](../README.md) · [hand_play](hand_play.md) · [visual_review](visual_review.md)

Severity: **High** = a player meets it in a normal session, or it breaks Overwatch feel / multiplayer / saves;
**Medium** = a common case or a visible miss; **Low** = edge case or polish.

## #88 Brigitte's shield: the three causes, read from the code

1. **"In third person she starts looking around while holding it."** Her shield stands are turn-in-place sheets, not
   loops: `tools/clipmaps/brigitte.txt:13-14` `idle_shield` (4FE7, "rootyaw -120") and `idle_shield_2` (+120). Overwatch
   drives a stand's frame by turn progress, so `HeroAnimator` holds plain stands at frame 0 (`rate = 0`), but only for
   names it lists: `HeroAnimator.java:1663` `base.equals("idle") || "crouch_idle" || "idle_scoped" || "crouch_idle_scoped"`.
   With the shield up, `HeroAnimator.java:1626-1628` turns `idle` into `idle_shield`. That name isn't on the list, so it
   falls to `rate = 1` (`:1669`) and the sheet plays on the clock, looping a 120-degree turn every second: she
   "looks around". Ana's scoped stand had the same bug and was fixed by name (comment at `:1664-1666`: "on the clock they
   turned her 120 degrees away (found playing)"). The fix was never made general. Her shield stance's aim sheet
   `add_aim_shield` (4FEF) and breath `add_breathe_shield` (506B) are never played either (see X3).
2. **"Holding it doesn't switch to Overwatch's third-person shield camera."** There's nothing to switch to.
   `AbilityCamera` (third person while an ability lasts) is driven from one call, `ClientHero.java:194-195`, for Jetpack
   Cat's Lifeline / Catnapper only. No Brigitte branch exists, so holding the barrier stays in first person. The data
   already has a hint: her hero option "Relative Aim Sensitivity During Barrier Shield" v49156 (`brigitte-map.md`). A
   follow-on: `HeroHud.java:183-186` draws the crosshair only in first person, so once a shield camera exists it needs a
   crosshair too.
3. **"It didn't block a creeper explosion."** From `Barrier.java`, `LivingEntityMixin.java:123-146`, Minecraft's
   `Creeper.explodeCreeper` and `Explosion.getDefaultDamageSource`, three ways the blast gets through, worst first:
   - **A close blast breaks the barrier and the rest reaches her.** Minecraft's blast damage is
     `((i*i+i)/2)*7*6+1` with `i = 1 - dist/6`. In Overwatch points (x12.5): about 209 at 3 m, 357 at 1.5 m and 413 at
     1 m on Normal, and x1.5 on Hard (`Player.hurtServer` scales it before our hook). Her barrier has 300. The play
     record tested one case only: "a creeper's blast 3 m ahead: absorbed whole (300 -> 98)" (`brigitte.md:94`), at
     full barrier, with every mob killed first. A real creeper usually goes off 1-2 m away, often after skeletons have
     worn the barrier down. Then `absorb` breaks it and `LivingEntityMixin` lands the rest "as a hit of its own"
     (`:140-145`). (Unverified against Overwatch: I believe the hit that breaks a barrier doesn't carry through. Check
     the wiki before choosing the fix.)
   - **A creeper within 0.9 m is behind the plane.** A creeper's blast has the creeper as its direct entity (a
     LivingEntity), so `Barrier.from` (`:184`) skips the blast-centre branch and uses `attacker.getEyePosition()`.
     `crosses` (`:196-198`) needs that point *in front of* the rectangle, 0.9 m ahead of her feet (`BARRIER_AHEAD`). A
     creeper pressed against her (centres 0.6 m apart) is at `da < 0`, so nothing is blocked. Its push is checked
     against the blast centre instead (`blocksBlast`, `ServerExplosionMixin`), so the damage and the push can even
     disagree.
   - **The coverage cone is narrow.** The rectangle is 1.51 m wide at 0.9 m. A creeper 2 m away and about 50 degrees
     off her facing makes a path that crosses the plane outside it. A creeper walking round her side gets through, just
     as the zombie did in the record (`brigitte.md:93`).
   - Number errors on the way: her **armor reduces the hit before the barrier takes it**. The record shows it: 209 - 7
     = 202 absorbed, "300 -> 98" (the armour step `armorReduce` runs in the `hurtServer` HEAD ModifyVariable,
     `LivingEntityMixin.java:107-116`). Then **the passed-through rest is hurt again through `hurtServer`** (`:142`),
     which dispatches to `Player.hurtServer`. That re-applies difficulty scaling (Easy halves, Hard x1.5) and runs
     `armorReduce`, Mercy's boost and Nano's factor a second time.

## Top 15 by severity

| # | Where | Flaw | Evidence | How a player hits it | Sev |
|---|---|---|---|---|---|
| 1 | Systems: knockback | Every hero hit also deals Minecraft's default knockback (0.4 and a hop if grounded). Hitscan and Brigitte's flail have no source position, so the push goes in a **random direction**. | `LivingEntity.dealDefaultKnockback` / `knockback` (random xd/zd when 0); sources `DamageSource(ARROW/MAGIC, null, owner)` in `Sombra.java:588`, `Mediblaster.java:148`, `Railgun.java:361`, `Ana.java:636`, `Brigitte.java:610`; `juno.md:306` "a kunai's knockback pushing her 3.4 m (Minecraft's, any hero)" noted, never fixed | Sombra's 20 shots/s make a zombie hop and jitter; in PvP every kunai shoves the target 3.4 m; the hurt tilt points nowhere | High |
| 2 | Systems: endermen | **Every** ranged hero ability uses an `IS_PROJECTILE` type (`thrown` or `ARROW`), so endermen teleport and take nothing. #91 names hitscan; this covers kunai, sonic, blaster, paws, thorns, darts, torpedoes and railgun too. | `Enderman.hurtServer:370`; damage-source list (`grep damageSources()` / `DamageTypes.ARROW`) | Any ranged hero against an enderman: it blinks away unhurt | High |
| 3 | Brigitte (#88a), Sombra | Turn-stand variants play on the clock: `idle_shield`, and **Sombra's `idle_stealth`** (3F46, "df 0xB7 default", a turn stand like her idle) | `HeroAnimator.java:1663` name list; `sombra.txt:14-15`; `brigitte.txt:13-14` | Stand still with the shield up, or stealthed: she turns 120 degrees again and again | High |
| 4 | Systems: 3P aim | The aim pose sheet (spine, arms and weapon follow pitch) is applied only to a **62-frame** sheet: Kiriko's. Ana (84), Brigitte (20), Sombra (41), Juno (6) and Lúcio's sheets, and every variant (`add_aim_shield`, `_scoped`, `_stealth`, `add_aim_run*`), are never used | `HeroAnimator.java:1319-1326` (`Math.round(aim.duration()*AIM_FPS) == 62`); `grep add_aim` gives one use; the clipmaps | In third person, or as another player watching, a Sombra shooting up holds her gun level and only her head tilts | High |
| 5 | Brigitte (#88c) | A creeper's blast gets through: a close blast beats 300 and the rest lands; a creeper within 0.9 m is behind the plane; narrow cone; armor taken before the barrier; double scaling on pass-through | above | A creeper at 1-2 m while the shield is up | High |
| 6 | Multiplayer | A player who **starts tracking** a hero mid-state never learns it. `START_TRACKING` sends Jetpack Cat's state only, and Brigitte's barrier is worked out from action events (`shieldUp = since(raise) < since(lower)`), with no attachment | `Overcraft.java:182-186`; `HeroAnimator.java:155-157`; no BRIGITTE attachment in `HeroManager.java:44-125` | An enemy walks into range (or respawns, or rejoins) while she holds the barrier: no barrier drawn and no shield stance, yet it still blocks their shots. Emotes in progress are missed the same way | High |
| 7 | Brigitte (#88b) | No third-person barrier camera | `ClientHero.java:194-195` (Jetpack Cat only) | Hold right mouse as Brigitte | High |
| 8 | Ana (Sleep), Brigitte (Rally stun) | Sleep and stun set `setNoAi(true)` (saved to NBT) and undo it from a static map. If the mob unloads or the server stops mid-sleep, it is saved NoAI and **frozen for good**: `wake` runs on the removed entity after its save | `Sleep.java:53-55, 122-126, 154-156`; `Overcraft.java:237` clears the map on stop | Dart a zombie and walk away (or save and quit) within 5 s: a statue mob forever | High |
| 9 | Mercy | Resurrect rebuilds a mob from its saved data **after** its loot dropped: iron golems drop iron again; a chested donkey or llama's inventory (`AbstractHorse.dropEquipment` drops without clearing) comes back full | `Souls.java` (mob tag at AFTER_DEATH, header comment) | Kill your iron golem, resurrect it, repeat: an iron farm, or a duplicated chest | High (exploit) |
| 10 | Systems: allies | Iron and snow golems are **always** allies, even one attacking the player (after hitting a villager, low reputation, or a player-built golem made hostile). Heroes can't hurt it (`canHurt` false), and heal it | `Allies.java:30-31, 45-46` | A village golem turns on a hero: the hero can't fight back in hero mode | Medium |
| 11 | Systems: enemies | Everything not an ally is an enemy: cows, sheep, horses, armor stands, bats. Juno's torpedoes lock them, EMP, Purr, Soundwave, Catnapper and Sombra's pistol hit them, ult charge farms on them, and **armor stands break** (two quick hits). Only `Railgun.java:210` excludes armor stands | `Allies.isEnemy`; `Torpedoes.java:78`; `Sombra.java:177`; `JetpackCat.java:508, 667` | Fight near your farm or armor stands: torpedoes wasted on sheep, armor stands smashed, displayed armor scattered | Medium |
| 12 | Jetpack Cat | Lifeline towing and Catnapper's carry run with no `controls` and no alive check. `lifeline()` and `catnapper` ULT_CARRY tick in Minecraft mode (G) and after she dies. Nothing clears her on a hero switch or leave (`HeroManager.select` / `onLeave` have no Jetpack Cat, Kiriko or Lúcio branch, and Mercy has none on leave) | `JetpackCat.java:146-150, 425-468, 633-648`; `HeroManager.java:251-272, 340-366` | Tow a friend and press G, or die: they keep being pulled to her or to her corpse | Medium |
| 13 | Jetpack Cat | Catnapper's "knock-down" is Slowness X plus Weakness X while a real stun exists (`Sleep.stun`, used by Rally). Skeletons still shoot, creepers still swell, flyers fly; the carry picks the nearest "enemy": a cow, the Wither | `JetpackCat.java:674-676` ("Minecraft has no stun"); `Sleep.java:66` | Ult into skeletons: they keep firing while "knocked down" | Medium |
| 14 | Systems: kill feed (beyond #84) | Only Kiriko's kunai can show a headshot (`KunaiEntity.headshot()`). Sombra, Juno and Sojourn crits never do. Abilities never get an icon: `weapon` returns `"weapon"`, `"melee"` or `""` | `KillFeed.java:36, 56-64` | A Railgun headshot kill shows no headshot mark | Medium |
| 15 | Systems: Overwatch rules | The global passive "damage cuts the target's healing by 30% (15% on tanks) for 3 s" (baseline since BZ 2026-02-10, per `tools/reference/sojourn.md:177-178`, marked M) isn't implemented anywhere | `grep` for a healing reduction finds nothing in `src/main` | Any fight: healing out-trades damage | Medium (source M) |

## All findings, by hero

### Brigitte (10)
- B1-B3: #88's three causes above (High, High, High).
- B4: the barrier isn't synced to late observers (top 15, #6) (High).
- B5: `add_aim_shield`, `add_breathe_shield` and the shield stops' and lands' aim sheets are unused. The barrier and her torso
  don't pitch with her aim in third person (X3) (Medium).
- B6: **non-damage effects pass the barrier.** `SleepDartEntity.java:65-74` calls `Combat.damage` (absorbed: 0) and then
  `Sleep.put` anyway while the target lives. An enemy Ana sleeps an ally standing behind her barrier. The same
  applies to anything that hits first and applies an effect after (Juno's lock, Hack's sight test) (unverified for
  those) (Medium).
- B7: flail, Whip Shot and Bash are `DamageTypes.MAGIC` (`Brigitte.java:610-613`). Magic ignores mob armor
  (`BYPASSES_ARMOR`), a witch takes 15% of it (`WITCH_RESISTANT_TO`), the knockback direction is random (X1), and kill
  messages read "killed by magic" (Medium).
- B8: the passed-through rest is hurt twice: difficulty scaling and `armorReduce` / boosts applied again
  (`LivingEntityMixin.java:142`) (Medium).
- B9: the barrier behind stained glass, water or ice isn't drawn (`rendering.md:165-169`, effects behind translucents)
  (Low).
- B10: records: "Not tried: a second player as Brigitte; the Nether; water and lava; boats" (`brigitte.md:198, 220`).
  The creeper case was tested at a single 3 m distance against a full barrier (process).

### Sombra (5)
- S1: `idle_stealth` (and `idle_stealth_2`) play on the clock (top 15, #3) (High). Related to #87.
- S2: **stealth survives Minecraft mode.** `Stealth.tick` runs whatever `controls` is (`Sombra.java:70-72`), and only
  her hero actions call `Stealth.end`. After G she's invisible with +60% speed and can sword mobs that keep losing her
  (`dropTargets` every 0.5 s) (Medium).
- S3: pistol: endermen (X2) and random knockback, 20 per second (X1) (High, shared).
- S4: `add_aim` (41 frames) and `add_aim_stealth` unused (X3) (Medium).
- S5: `Stealth.tick` sets `setInvisible` while vanilla's `updateInvisibilityStatus` also writes it on every effect
  change, so a one-tick flicker when any effect starts or ends (unverified) (Low).

### Ana (5)
- A1: `add_aim` (84 frames) and `add_aim_scoped` (106) unused. Scoped in third person, her rifle doesn't follow her
  pitch (Medium).
- A2: Sleep's NoAI saved permanently on unload or stop (top 15, #8) (High).
- A3: Sleep goes through an enemy Brigitte's barrier (B6) (Medium).
- A4: a dart at an enderman: the damage is refused (it teleports), then `Sleep.put` sleeps it where it landed (Low).
- A5: scoped hitscan: `AABB.clip` from inside the inflated box returns empty, so a target hugging her can't be shot
  (`Ana.java:444`; same in Sombra, Juno, Railgun) (unverified) (Medium).

### Juno (6)
- J1: torpedoes lock passive animals and armor stands (`Torpedoes.java:78`) (Medium).
- J2: her own notes list gaps that were never filed (`juno.md:118, 137`): the torpedo hold layer and the lock aren't
  synced to others; the lock marker is a world ring where the data's is HUD 0727.116; no 1P glide screen hex; no
  reload gun-piece swap; the hover's ending layer isn't played; enemies' red rows never seen. These belong with #86
  (Medium).
- J3: Mediblaster: endermen and random knockback (X1, X2) (High, shared).
- J4: `add_aim` (0.2 s) unused (X3) (Medium).
- J5: torpedo damage goes through `Combat.damage` without Ender Dragon part handling (`Torpedoes.java:232`) (Low).
- J6: "Not tried: a second player as Juno; boats, ladders, minecarts" (`juno.md:308`) (process).

### Sojourn (4)
- So1: the global healing reduction is missing (top 15, #15) (Medium).
- So2: 1P slide additives (`add_slide_start` / `_end`) and the 3P `add_overclock` stance aren't played
  (`sojourn.md:81, 194`) (Low).
- So3: Railgun damage is `ARROW` (endermen) (X2) (High, shared).
- So4: "Not tried: barriers (+1 energy)", a second player as her enemy, lava (`sojourn.md:145, 192`): the barrier
  interaction (`Barrier.took`) has never run (Medium).

### Mercy (3)
- M1: Resurrect duplicates loot and inventories (top 15, #9) (High).
- M2: `idle_blaster` (0A15) is played on the clock, as `idle_shield` was. Whether it's a turn stand isn't stated in
  `mercy.txt:13`: check (unverified) (Low).
- M3: blaster shots are `thrown` (endermen) (X2).

### Lifeweaver (2)
- L1: thorns and Superbloom are `thrown` (`ThornEntity.java:75-96`, `Lifeweaver.java:547`): endermen (X2).
- L2: no `add_aim` in `lifeweaver.txt` at all: his torso never follows aim (check the data for his sheet) (Low).

### Kiriko (2)
- K1: kunai are `thrown`: endermen teleport and take nothing (X2). Kiriko is the flagship, and #91 names only hitscan
  (Medium).
- K2: PvP: every kunai knocks an enemy 3.4 m (X1, `juno.md:306`) (High, shared).

### Lúcio (4)
- Lu1: **his dreads aren't drawn**: "hair cards (material 8052) come out pure black ... Not drawn" (`lucio.md:43`). This
  is the "missing parts" class of #92 (Medium).
- Lu2: wall-ride lean sheets (2927 / 29B3, 1P 2519 / 251E: the camera rolls -20 to +20 degrees) aren't sampled
  (`lucio.md:40-41`): no lean, no 1P camera roll on a wall ride (Medium).
- Lu3: Soundwave is `playerAttack` (`Lucio.java:280`), so it shows the melee icon (#84), and it breaks armor stands (X7)
  (Medium).
- Lu4: sonic projectiles are `thrown` (endermen) (X2).

### Jetpack Cat (4)
- C1: Lifeline and carry ignore G and death; no clear on a switch or leave (top 15, #12) (Medium).
- C2: Catnapper's knock-down isn't a stun (top 15, #13) (Medium).
- C3: the carry can grab bosses or animals, and its `setDeltaMovement` on the Wither works (Low).
- C4: paws are `thrown` (endermen) (X2); Purr and ult `playerAttack` show the melee icon (#84).

## Systems (cross-hero)
- X1: default knockback on every hero hit, random for hitscan and flail (top 15, #1) (High).
- X2: endermen immune to every ranged hero (top 15, #2) (High).
- X3: the aim sheet only works for Kiriko (top 15, #4) (High).
- X4: turn stands on the clock (top 15, #3). The rule is name-based (`HeroAnimator.java:1663`). It should come from the
  clipmap ("turn stand" / "df 0xB7") rather than a growing list (High).
- X5: late trackers miss state (top 15, #6) (High).
- X6: golems always allies (top 15, #10) (Medium).
- X7: passive mobs and armor stands are enemies (top 15, #11) (Medium).
- X8: **Minecraft armor and enchantments apply to hero health** in hero mode: nothing in `src/main` touches armor
  values. A Brigitte in netherite takes a fraction of a hit, Protection IV halves an enemy hero's numbers, and Thorns
  reflects. Every "Overwatch numbers" check assumes bare skin (owner decision) (Medium).
- X9: Minecraft's hurt cooldown still guards **heroes** against mobs. `Combat.damage` zeroes it only for hero damage.
  Two zombies hitting within 0.5 s: the second hit only does its excess over the first, and a hero's own hit resets
  the victim's cooldown in between (Low-Medium).
- X10: point-blank hitscan misses (`AABB.clip` from inside) (A5) (Medium, unverified).
- X11: Ender Dragon: Sombra's pistol maps a part to `parentMob` and damages the dragon directly (`Sombra.java:526`,
  then `Combat.damage(best...)`), so a head hit counts at the body rate. Ana's shot, torpedoes and quick melee have no
  part handling (Low).
- X12: emotes are accepted by the server in any state: in water, mid-air, riding, in Minecraft mode
  (`Comms.java:85-93`: only hero and alive) (unverified client gate) (Low-Medium).
- X13: quick melee knocks back twice: Minecraft's own from `playerAttack`, then `target.knockback` again
  (`QuickMelee.java:71-79`) (Low).
- X14: hero movement replaces `travelInAir`, so ice and blue ice don't slide (`LivingEntityMixin.java:21-28`) (Low).
- X15: kill feed: headshots for kunai only, no ability icons (top 15, #14) (Medium).
- X16: global healing reduction missing (top 15, #15) (Medium).

## Why these got through: the records
- **Dev input stands in for the player.** `hand_play.md` says mouse moves can't turn the view, "can't send F5 or two
  held keys at once", and abilities go through the harness's `key <ability>` taps. So turning while holding the
  shield, standing still in third person, and aiming up or down, the moments where #88a, X3 and X4 show, are exactly
  what the harness can't do.
- **One clean case per mechanic.** "Every mob killed first, the barrier refilled between cases"
  (`brigitte.md:89`): a single creeper at 3 m with a full barrier. Overwatch players meet the creeper at 1 m after two
  skeleton volleys.
- **The same "Not tried" list in every hero's record:** a second player, the Nether, water and lava, boats
  (`brigitte.md:198, 220`; `juno.md:308`; `sojourn.md:83, 145, 192, 270, 419`; `jetpack_cat.md:101, 157`). X5 and
  B4 only show with a second client that walks into range *during* a state.
- **Known gaps stay as notes, not issues.** Lúcio's missing dreads, his wall-ride lean, Juno's HUD lock and glide hex,
  Juno's torpedo layer for others, the kunai's 3.4 m PvP push: all are written down in `techniques/heroes/*.md`, and
  none were filed as `bug` issues.
- **Fixes by name, not by rule.** Ana's scoped turn stand was fixed by adding its name (`HeroAnimator.java:1663`).
  Brigitte's and Sombra's twins were missed. The Kiriko-only aim sheet (`== 62` frames) is the same pattern.

## Count per hero
Brigitte 10 · Sombra 5 · Ana 5 · Juno 6 · Sojourn 4 · Mercy 3 · Lifeweaver 2 · Kiriko 2 · Lúcio 4 · Jetpack Cat 4 ·
systems 16 (the shared X items also count once in each hero they reach).

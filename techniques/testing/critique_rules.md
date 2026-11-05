# Critique of fix/rules (#99, #101, #104, #110, #111, #83, DevBot), 2026-11-04

Up: [Techniques](../README.md) · the records it checks: [allies](../minecraft/allies.md), [barriers](../minecraft/barriers.md),
[jetpack_cat](../heroes/jetpack_cat.md), [sojourn](../heroes/sojourn.md), [hand_play](hand_play.md), [harness](harness.md)

An independent read of commits 985f73c..32aa1dd against `next` (1feb188) and Minecraft 26.3's decompiled source. **Code and records only: no game,
no build.** So every finding below is by reading; "predicted" means nobody has seen it happen. Reproduce each before fixing it (CLAUDE.md).

**Verdict: don't merge yet.** Findings 1 and 2 are regressions this branch adds, and 3 means the required play hasn't been done for its own list.
Most of the barrier findings need teams: with no teams every player is an ally, so solo play never reaches them. 6 to 11 are what solo survival play reaches.

## High

### 1. A Biotic Grenade on an enemy barrier now bursts through it (f294618, never played)
`BioticGrenadeEntity.hitBarrier` now calls `burst`, and `Ana.grenadeBurst` has no barrier check of its own: only `inSight`, which looks at blocks.
- **Sigma's barrier: full damage behind it.** The burst's source has the grenade as its direct entity. `Barriers.absorb` then asks whether its *last step* crossed the barrier (`Barriers.java:343`). `hitBarrier` has just put it on the near side, so that's false: no absorb, and everyone within 4 m behind takes all of it. The barrier also paid `barrierDamage` first.
- **Brigitte's: the barrier pays again per victim, and the status still lands.** `Barrier.from` works from 6 m back along the flight, so damage is absorbed, once for each victim behind her barrier, on top of the `barrierDamage` already taken. `Combat.antiHeal(e, ...)` (`Ana.java:376`) runs whatever `dealt` was, so she and those behind her are anti-healed through it. That is #104's own bug shape (a status past a barrier) on another projectile.
- **It reverses a recorded decision without saying so.** Left standing six lines from the new method: `// a burst on an enemy barrier: the barrier takes it and it's gone, no splash on either side: ours, the wiki's "barriers block the explosion"`. The commit says it was "dropped silently", but that was the written rule. The wiki's Barrier page does say barriers block such explosions (read through the API today). `Accretion` and `Hyperspheres` do ask `Barriers.between` before a burst; the grenade doesn't.
- At Sigma's, with Biotic Bounce, the first burst sends it on along its old way, into the same barrier again (predicted). At Brigitte's, the first absorbed hit discards the grenade (LivingEntityMixin) in the middle of the burst's loop; the loop goes on.

**Check:** Ana (red) at Sigma (blue) with his barrier up: read his health and `antiHealed`. The same at Brigitte, reading her barrier's drop against 1 + the number behind it.

### 2. Brigitte's barrier now eats shots at her back (c5e2af1, never played)
`heroProjectiles` looks one step ahead: `meets(now, now + delta)`, from either side, at the end of the tick (`Overcraft.java:179`, after entities tick). The barrier stands 0.9 m *in front of* her. A shot from behind her whose next step crosses that plane is stopped there, before its own tick can touch her body.

From directly behind, the last position before she's reached is up to one step from her back, and the plane is about 1.2 m past her back. So the share eaten is about (step - 1.2) / step:

| Shot | m/s | Step (blocks/tick) | Eaten from behind |
|---|---|---|---|
| Sleep Dart | 60 | 3.0 | about 60% |
| Kunai | 90 | 4.5 | about 73% |
| Ana's dart | 125 | 6.25 | about 81% |
| Sojourn's round | 150 | 7.5 | about 84% |

The old rule let these through: `Barrier.crosses` is false when both ends are behind the plane. Flanking is the counter to a barrier, and a flanker's Sleep Dart now mostly hits a barrier facing away from them. The same look-ahead also takes a shot at anything standing just in front of her barrier (a zombie pressed against it, an ally), which the shot should reach first. `Barriers.intercept` has the same look-ahead, but Sigma's barrier doesn't stand against his body.

**Check:** bot Brigitte with her barrier up, Sombra or Kiriko behind her, 20 shots at her back, count hits on her against hits on the barrier. Compare `next`.

### 3. "Played" is much less than the records read as
- **6 of the 15 `areaHits` sites have no played row and no mention in any `rl_*` script:** Cyberspace, Biotic Grenade's burst, Local Anesthetic, Rally's shockwave, Hypersphere's burst, Gravitic Flux. The diff is right by reading, but CLAUDE.md asks for play, and allies.md's list reads as if all were covered. Its "Not played" list doesn't name them.
- **Not real input.** The "real attack key" is `KeyMapping.set` through DevServer (`DevServer.java:307`), aiming is `look` or `/execute ... tp`. CLAUDE.md: "real input: the real mouse and keys", and "scripted tests ... are not evidence".
- **The "fight" isn't one.** `rl_fight.py`: three heroes of eleven, standing still, teleport-aimed at the nearest zombie, a key loop, 40 s. The bystanders' numbers were lost (admitted). No survival: no hunting, no farm, no village, no Nether (piglins), no ocean. Those are what #99 is about, and the owner plays survival.
- **Catnapper was played under Resistance V**, so what the carried creeper does to her was not measured (finding 12).
- **The old column can't be checked.** `rl_old_*` went with a temporary worktree (32aa1dd). Only 22 `rl_new_*` pictures exist, none for the Soundwave, Disruptor, EMP, Purr or Accretion rows.
- **No Overwatch side by side** for Catnapper's stun or a barrier's stop (the QA rule: `memory/qa-polish-and-critique`).
- **Issues:** all commits say `Refs`, and none of #99, #101, #104, #110, #111, #83 has a comment. #111 and #83's findings, and #110's unreproduced steps, must be said in the issues (CLAUDE.md). `qa_critique_2026-11-02.md` says "reproduced and fixed" for J1/X6/X7 with these open points in them.

## Medium

### 4. Sphere and rock bursts at Brigitte's up barrier push through it (c5e2af1 opens this; not played at full barrier)
Now that they meet her barrier in flight, `hitBarrier` runs `Accretion.impact` / `Hyperspheres.burst` at its front. Their guard is `Barriers.between`, which knows only entity barriers. So each victim behind her: the damage is absorbed (and the barrier pays for each, on top of `barrierDamage`), but `push` runs regardless (the rock's burst push, the sphere's pull). Soundwave, Purr and Catnapper all ask `Barrier.shields` for exactly this; these two don't. The record's only rock case is the 22-left one.

### 5. #83's "a barrier doesn't ask what's behind it" stops at 10 m
`covering` and `shields` keep `brig.distanceToSqr(victim) <= 100`. A hitscan shot (Sombra's pistol, Ana's scoped shot, Charged Shot) or an arrow crossing her barrier at a target more than 10 m past her isn't stopped; Sigma's has no such limit. The record's case is a zombie close behind her. Blasts (`blocking`) still ask that the victim is hers (admitted).

### 6. Bystanders still pay out, against the fix's own reason
`Combat.credit`'s comment says a chicken farm mustn't fill an ult. By reading, these bypass it:
- **Accretion's direct hit: +25 ult** by `addUltCharge` (`Accretion.java:140`). A cow farm does fill Sigma's ult.
- **Sojourn:** +5 energy a round on a cow (+ more on a crit), `Railgun.java:243`, so free Charged Shots, and Sharpshooter's refund on a cow's "head".
- **`SigmaPerks.sphereDealt`** on a direct hit (Hyper Regeneration, Hyper Strike's count), **`addFireForDamage`** everywhere, **`Boosts.java:64`** (Mercy's credit for a boosted ally's hit, two-argument `credit`).
- A crit on a bystander still sets `critAt`, and still shows a crit hit marker.

allies.md admits some and calls them "not changed", while its rule table says "A bystander also gives no ult charge". It also says a killed bystander gets a "kill line": `Voice.onKill` requires `Enemy` or `Player`, so no. The same gate means a turned golem or an angry wolf gets **no** kill line, though the new rule calls them enemies: a second definition of "enemy" outside "the one place".

### 7. `hostile` doesn't see what the javadoc says it sees
It says "a brain's attack target", but `Mob.getTarget` returns the goal-system field. Only Piglin, Hoglin, Zoglin, Warden, Breeze, Creaking, Axolotl and Frog override it to the brain. By mob:

| Mob | Class | What happens |
|---|---|---|
| **Zombie Nautilus** (and a wild Nautilus provoked) | `AbstractNautilus`, not `Enemy`; brain `ATTACK_TARGET`, no override | **Bystander while ramming the hero.** No area ability, lock or charge. Its drowned rider is an enemy, it isn't. allies.md doesn't mention it |
| Goat | Animal, brain ram | The same (admitted) |
| Llama, trader llama | `didSpit` ends `LlamaHurtByTargetGoal` (`Llama.java:486`), not `NeutralMob` | **Flickers** to bystander after each spit |
| Panda, killer rabbit | target only, no anger | Enemy only while the target holds |
| Bee | `stopBeingAngry` on the sting | Bystander after stinging; fine |
| Anything put to sleep by a dart | `Sleep.put` does `setTarget(null)` | A non-`NeutralMob` turns bystander asleep (a panda, a llama); AoE then skips it |
| Zombie Horse, Skeleton Horse, Camel Husk | `AbstractHorse`, not `Enemy` | Bystanders though undead and ridden by enemies: AoE strips the rider, leaves the mount |
| Angry `NeutralMob`, gamerule `universalAnger` | `getPersistentAngerTarget() == null` | Not hostile unless targeting |
| Golem whose anger is at a co-op friend, target lost | anger path is `matches(hero)` only | An ally to the other hero, though the javadoc says "or the hero's side" |

### 8. Calm neutrals that Minecraft calls `Enemy` stay enemies: the rule isn't the rule it says
"Bystander" rests on behaviour for wolves, bees and polar bears, and on class for the rest. **Piglins** (a player in gold armor bartering), **zombified piglins** (`Zombie implements NeutralMob`), **endermen** and spiders by day are always enemies. Soundwave, Purr, EMP, Flux or Juno's lock near calm piglins or a zombified-piglin group starts the fight the player was avoiding, and fills the ult from them. Not new (they were enemies before), but #99 asks "decide per mob from its behaviour" and allies.md doesn't list them. **Owner's call; raise it.**

### 9. The creeper rule doesn't cover every hold
The fuse fix lives in `Sleep.tick`, which walks `SLEEPERS`. A mob lifted by **Gravitic Flux** is held through `GraviticFlux.holds` with no `Sleeper` (no `Sleep.stun` in that file). `SwellGoal` is off, so a swelling creeper's dir stays +1 and it goes off in the air. `jetpack_cat.md` says "for Sleep Dart, Rally's stun and all": not all. Predicted, not played.

### 10. A knocked-down flyer falls about 30 blocks (predicted; the record: "Flyers: not tried")
`Sleep.slide` applies `mob.getGravity()`, and only `FlyingMoveControl` and `Vex` set no-gravity. A dart's hold doesn't move them; a knockdown does. With gravity 0.08 and drag 0.98, 30 ticks is about 30 blocks:
- **Ghast, phantom:** they drop out of the sky (their `checkFallDamage` is empty: no damage).
- **Ender Dragon:** `noPhysics = true`, no gravity override, and bosses take the knockdown: **it sinks through the island** on each Catnapper (about 16 blocks on Accretion's 1.1 s, already on `next`). #101 multiplies who gets this.
- A blaze's own `aiStep` slows its fall. In water, `slide` has no water drag.

### 11. #110: the head box improves zombies and worsens everything not built like one
The change fixes the steep-shot bug, by reading and by its table. The geometry is right for 0.6 m humanoids: a zombie, a player (1.37 to 1.91 m, +-0.25 m), a skeleton. But 5/12 of the width from the middle (with vanilla sizes):

| Mob | Head box | Its real head |
|---|---|---|
| Spider, 1.4 x 0.9, eye 0.65 | 0.40 to 1.01 up, +-0.58 m: the whole upper body | At its front. A shot at its abdomen from behind or the side crits |
| Cow, ravager, hoglin, horse | Mid-body column | Sticks out at the front, partly outside the box: aiming at it gives body or a miss |
| Iron golem, 1.4 wide | +-0.58 m | Its head is about 0.5 m wide: the shoulders crit |
| Ghast, 4 x 4, eye 2.6 | 2.35 to 4.11 up, +-1.67 m | Nearly half the creature |
| Wither | The middle | Three heads; the two side ones don't count |

Before, a front-headed mob's real head at least counted (the band ignored width). **From the side, a spider's visible head can now be a body hit where it was a crit: a regression, not played.**

Also:
- **`OVER`'s comment is wrong:** "the fattest shot's own radius (Charged Shot's 0.11 m)", but `RAILGUN_RADIUS` is 0.175. The line ignores shot radius on every side, and adds 0.11 at the top only. A thick round (Sojourn's, Lúcio's) whose edge meets the head, its centre just past it, was a crit and is a body hit. Overwatch counts a projectile's size.
- **The waist from 77 degrees still crits.** The line really does cross the head box, so this is right by geometry and defensible; a 45 degree shot at chest height (1.24 m and up) crits too. Say so to the owner.
- **The issue's steps stay unexplained.** The record is honest about this. `kf_soj6` is the critique's top-face reading, and it may simply be this. A level false crit remains unproven either way: the scan used NoAI zombies, one direct aim, no moving target. Only Charged Shot and kunai were played; eight sites are by code.
- Moving targets and a muzzle offset (a shot's way isn't the eye's): not tried.

### 12. Landing on a creeper is still a likely death for her
With the stun on, a carried creeper's AI returns at 1.5 s, 1 m under her feet: inside `SwellGoal`'s 3 m. It goes off under her at about 3 s (the record's own "goes off by 4.3 s", under Resistance V, so the damage to her wasn't read; hand_play says 12 to 17 health from a creeper). The carry should hold a fuse, drop it or skip creepers. **Owner's call**, but nobody has looked at what it does to her.

## Low
- **Nearest may be dead:** `land` picks `nearest` without `isAlive` after its own 25. If the landing kills the nearest, the carry ends at once though others stand in reach. On `next` too.
- **Carries that can't carry:** a sleeper (admitted), a shulker (`move` doesn't move it), a jockey or anything riding (`isPassenger`): Cat hangs on a tether that doesn't move for up to 4 s. The boss list misses nothing obvious, but a Giant or a ravager goes up (ours, fine: tell the owner).
- **Client mismatch (admitted, confirmed):** `rl_new_play_junolock` shows the turned golem with the villager's ring. Also its ally bar (`HeroHud:1635/1703`), name tag rules, Lifeline's and Mercy's previews.
- **A dead or offline friend's shot:** `stops` asks `isSelfOrAlly`, whose `isAlive` is false for a dead owner, and null owner stops too: an ally's kunai in flight when they die hits her barrier and charges it.
- **Cyberspace on a barrier** opens in mid-air, and `Cyberspace.tick` has no barrier check: it weakens those behind. Suzu on an enemy barrier: no source was looked for (real-data rule).
- A dart stopped at a barrier makes no sound or mark (admitted).
- **Riding:** hitscan rays include the hero's own unowned mount (a pig, a happy ghast): a bystander is `canHurt`. Was so before; the rule now says "aimed", and the player isn't aiming at it.
- **Brigitte can't hunt with her primary** (the flail), nor can bash hit a cow, while whip and quick melee can. Sigma's sphere does only its direct 20. Owner's call (admitted). A copper golem, which a player builds, is a bystander while an iron one is an ally (admitted).
- **Accretion "aimed beside them" hit the cow** (allies.md): its contact radius takes bystanders it wasn't aimed at, for 80 and a knockdown. A grenade, Disruptor shot or Cyberspace also stops on a cow (`canHit`) and then doesn't affect it: a sheep in the way eats them.

## What holds
- **Can a hero kill a cow? Yes,** with every aimed weapon (kunai, sonic, darts, paws, thorns, blaster, Mediblaster, pistol, rounds), and Minecraft's drops come since each source has the player as attacker. Only Brigitte's primary can't.
- **Healing:** `isAlly` is the old list less a mob currently hostile. Villagers, golems, allays and pets of allies heal as before. Pets of an offline or other-dimension owner: bystander instead of enemy, an improvement.
- **`areaHits` adoption:** I grepped every entity scan. All 15 are right as area abilities, and I found none missed: Hyper Strike, whip, quick melee and the rounds are aimed; Kitsune Rush, Lifeline, Rally's buff and the like are allies only. `enemyDeployable` keeps `canHurt` on its owner, a player: fine. (Bash as "area" is the one debatable: it's aimed.)
- **Golem calming:** target, then anger's end time, then ally again; a respawned hero (a new object) still matches by UUID. Sound.
- **`Sleep.knockdown` with its push:** moves by `slide`, and the carry overrides each tick. A player keeps server-sent speed (`KeyboardInputMixin` zeroes only keys), so the carried-and-stunned player probably works; still unplayed, as admitted.
- **Creeper rule for sleep, stun, knockdown:** the tick order is right (set at the end of a tick, read by the next `Creeper.tick`); flint and steel is left alone, as in vanilla.
- **Performance:** one 8-block scan per up barrier per tick: nothing. The fastest shot (7.5 blocks/tick) can't jump the box.
- **DevBot is dev-only.** It's reached only through `DevServer`, whose thread starts only with `-Dovercraft.dev=true` (set only by build.gradle's run configs). The class ships in the jar like the rest of `client.dev`, with no static hook; it registers its tick on first `add`. A shipped jar never reaches it. Its leftovers (`BOTS` across worlds) are dev-only.
- **#111 was enough for "not reproduced",** about 130 respawns over the right variables, and `applyKit(fresh)` does set full health. But "isn't a respawn bug" is stronger than the evidence: mobs at the spawn is a good explanation that reproduces the number, not proof it was that sighting. Not tried: Mercy's resurrect (`onRespawn(resurrected)`), a hero change at the respawn, a real second player. Leaving spawn protection to the owner is right, and the record is correct that vanilla has only the client-load wait (`ServerPlayer.isInvulnerableTo`). Say all this in the issue.
- **#83:** the pistol at her held on `next`, per the record and by reading `Barriers.absorb`.

## Trial merges (`git merge-tree`, today)
- **Into `next` (1feb188):** clean. `next` is the branch's base, so a fast-forward.
- **With `fix/weapon-parts` (ea8923a, merge base 257dae0):** 3 conflicts, **all text in techniques:** `heroes/ana.md`, `heroes/jetpack_cat.md`, `heroes/sojourn.md` (both append at the end; keep both). The only Java file both touch is `DevServer.java`, in separate `case`s, auto-merged.
- **Semantic overlap: none found.** weapon-parts is rendering, clips and dev commands; it touches none of `Allies`, `Barrier`, `Sleep`, `Headshot`. weapon-parts branches from before the reticle merge, so it needs `next` merged in regardless.

## What wasn't played that a player hits
A night in real survival with each of the eleven heroes, moving, around animals; a farm; a village with a real reputation drop; a bastion in gold armor; endermen; an ocean monument and a drowned on a nautilus; bees at a hive; a wolf pack; llamas; a goat on a mountain; phantoms, ghasts and blazes under Catnapper or Accretion; the Ender Dragon and the Wither under both; a creeper under the carry without Resistance; water, lava, ladders and boats with Catnapper; any barrier case on two real clients; a grenade, a Suzu, a Cyberspace, a sphere or a rock on a barrier at full health; a shot at Brigitte's back; a target more than 10 m behind her.

## What should change before merge
1. Finding 1: back to no splash, or give the burst a barrier check, status included. Fix the comment either way.
2. Finding 2: don't stop a shot that would reach a creature on its way first (or no look-ahead from behind).
3. Finding 4: `shields` on the two bursts' pushes.
4. Finding 6: at least Accretion's +25 and Sojourn's energy, or change the record's rule.
5. Finding 9: the creeper rule under Flux.
6. Play the 6 unplayed sites and the barrier cases, by hand, plus a survival session: finding 3.
7. Comment on the issues; put 8, 11 and 12 to the owner.

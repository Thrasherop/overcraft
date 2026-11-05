# Allies, enemies and bystanders

Whose side a creature is on, for a hero. Overwatch has two teams and no bystanders; a Minecraft world is full of them. The rule
lives in one place, `dev.overcraft.hero.Allies`, and every rule here is **ours, open for the owner** (#99, 2026-11-04). Up:
[Techniques](../README.md) · engine: [minecraft_26](minecraft_26.md) · shots at barriers: [barriers](barriers.md) · found by:
[qa_critique](../testing/qa_critique_2026-11-02.md) (X6, X7, J1)

## The rule
| Who | What | How it's asked |
|---|---|---|
| **Ally** | players with no teams or on the hero's team, pets of the hero or an allied player, villagers, iron and snow golems, allays. Healed, buffed; shots pass through. | `isAlly`, `wasAlly` |
| **Enemy** | players on another team, a pet of a player not allied, anything Minecraft calls an `Enemy`, and any mob **fighting the hero's side now**: its target (`Mob.getTarget`: `DefendVillageTargetGoal`, `HurtByTargetGoal`, a brain's target), or `NeutralMob` anger at the hero. | `isEnemy` |
| **Bystander** | everything else: cows, sheep, horses with no owner, a calm wolf, a copper golem, an armor stand or mannequin. | `isBystander` |

Two questions use those:
- **`canHurt`: an aimed attack** (a shot along the aim, quick melee, Whip Shot): lands on anything alive that isn't an ally, bystanders too, so a player can hunt
  without G, and a cow in front of a zombie doesn't let the shot through. An armor stand takes it as Minecraft's arrow or punch (two quick hits break it). Friendly fire adds allies.
- **`areaHits`: what takes everyone in reach without aiming** (bursts, fields, waves, swings, locks): enemies only (friendly fire adds allies). The list: EMP, Cyberspace, Biotic Grenade's burst, Local
  Anesthetic, Purr, Catnapper's landing, Brigitte's flail, bash target and Rally's shockwave, Juno's lock, Accretion's and Hypersphere's bursts, Gravitic Flux, the Disruptor, Soundwave.

A bystander pays nothing: ult charge, perk progress, On Fire, Sojourn's energy and Sharpshooter, Accretion's +25, Hyper Regeneration, Mercy's share of a boosted hit, a crit marker. (Round 2, the critique's 6; a farm filled an ult.) A hit on one still deals its damage. A golem is an enemy for as long as Minecraft keeps it so, then an ally again.

## Played (2026-11-04; old = 985f73c, one commit before; same scripts; `rl_new_*` in `overcraft_rules/run/screenshots`, `rl_old_*` were read and went with their temporary worktree)
| Case | Old | New |
|---|---|---|
| Kunai at an angered iron golem | 99 -> 99 (passes through) | 99 -> 94.2 |
| The same, a calm golem | 100 -> 100 | 100 -> 100 |
| A golem punched with the real attack key in Minecraft mode, then G | not played | it threw her and killed her in two hits (20 -> 8.5 -> 0); before she died, a kunai back: 94 -> 89.2 on a run where it stood still |
| Soundwave: cow, stand, zombie | cow 10 -> 6.4, zombie 20 -> 16.5 | cow 10, zombie 20 -> 16.5 |
| Disruptor field, 3 s | cow and zombie dead | cow 10, zombie dead |
| Accretion aimed beside them | cow 0.4, **stand broken**, zombie 16.9 | cow 3.6 (its direct hit: aimed at it), stand standing, zombie 16.9 |
| EMP | cow 10 -> 8, zombie 20 -> 16 | cow 10, zombie 20 -> 16 |
| Purr | cow 10 -> 7.6 | cow 10 |
| Juno's lock with only a cow in view | locked: 10 s cooldown, cow 10 -> 3.2 | none: 2 s cooldown, cow 10 (`rl_new_play_junolock`: villager and turned golem ringed, cow not) |
| Juno's lock: turned golem, villager, cow | not played | golem 99 -> 92.2, villager healed 10 -> 20, cow 10 |
| Kunai on a cow, 1.4 s | 5.2 left, ult +68 | 5.2 left, ult +7 (the passive 5 a second) |
| Kunai through a villager to a zombie | not played | villager 20, zombie 15.3 |
| Mercy's beam: a calm golem, then turned | not played | 50 -> 55.3; then 54.9 -> 54.9 |
| Flail x4 between a cow, a stand and a zombie; then Whip Shot at the cow | not played | cow 10, stand standing, zombie 5.6; whip 10 -> 4.4 |
| Lúcio, Kiriko and Jetpack Cat fighting 40 s at night on Hard, cows and a stand around | not played | no crash; respawned 20 and 18 full (the bystanders' numbers weren't read: the script had command feedback off) |

## Traps
- **Wolves call each other:** `HurtByTargetGoal.setAlertOthers`: a "calm wolf" 1 m from one you angered is angry too. Soundwave hit it, rightly.
- **Who a mob targets is known only on the server.** `getTarget` and the anger's fields aren't synced: on a client, a turned golem is still an ally: its ally health bar shows and Juno's lock
  ring has the ally look (`rl_new_play_junolock`). Open.

## Round 2, after [critique_rules](../testing/critique_rules.md) (2026-11-05)
**What "played" means here (the critique's 3):** every run is the dev harness: keys through `KeyMapping.set` (`key` in DevServer), aim by `look` or `/execute ... facing`, not a real mouse or keyboard; the bot is a server-side player, no second client. Mobs, terrain, damage and hits are the real game's. Old = b88a2fa, new = the build after the critique; pictures: `overcraft_rules/run/screenshots` (`rl2_new_*`, `old/rl2_old_*`), gitignored.

**Who's hostile now:** a mob's target, its brain's `ATTACK_TARGET` (a Zombie Nautilus's: `Mob.getTarget` doesn't read it), its anger at the hero or anyone on their side, universal anger, and **a short memory** (ours, 5 s: the blow it just landed, or what it was after when a hold cleared its target), so a llama doesn't flicker between spits. A goat counts from its first ram (a ram's target is a position), not while it lines up. **An unowned mount carrying an enemy** is an enemy (a skeleton's horse, a drowned's Nautilus). **Kill lines** ask `Allies.wasEnemy`: a turned golem and an angry wolf get them; a killed cow never did (the first version of this note was wrong).

**Calm by nature (the owner's default for now, ours, open):** piglins, zombified piglins and endermen are bystanders to area abilities until they target or anger at the hero's side, so a Soundwave doesn't start a bastion's fight. Spiders by day stay enemies, not asked. Pings: the client can't see targets, so a calm piglin pings red as before.

| Case | Old | New |
|---|---|---|
| Accretion on a cow, ult | 9.5 -> 43.2 (+25) | 9.2 -> 18.2 (passive) |
| Railgun rounds on a cow, 1.2 s, energy | 0 -> 95; head: crit | 0 -> 0; no crit flag |
| Kill line on a turned golem (`heard`) | none | `kiriko.voice.elimination` |
| Llama after a hit, side each 0.5 s | `EEEEbbbbbbbbbb` | `EEEEEEEEEEEEEb` |
| Bee after its sting | `EEEEEEEbbbbbbb` | `EEEEEEEEEEEEEE` |
| Zombie Nautilus in water | bystander (not run in water; on land it has no target) | `EEEE...`, brain target her |
| A skeleton horse with a skeleton riding / alone | not run / bystander | enemy / bystander |
| Soundwave on a zombified piglin and an enderman, calm | 20 -> 16.5, 40 -> 36.4 | 20, 40 |
| A piglin with no gold | enemy | enemy (it targets her); hit |
| Six unplayed sites: Cyberspace, grenade burst, Local Anesthetic, Rally's shockwave, sphere burst, Flux; cow, stand, zombie, villager in reach | not run | cow 10 and stand up in all six; zombie hurt in all six (Flux 200 -> 96); villager healed by Cyberspace, grenade, Rally's Inspire |

## What a critic should check
- **Round 2's open points:** a village golem turned by low reputation (not done: the golem was punched); a real goat on a mountain; a drowned riding a Nautilus (the rule by a skeleton's horse); a bastion in gold: the piglin stayed a bystander until a brute (an enemy) took the Soundwave, and then the group angered, as Minecraft's alert does, so "doesn't start a bastion's fight" holds only with no brute in the wave.
- **The owner's call:** cows take an aimed shot at all? Flail can't hit a cow or break a stand (quick melee and Whip Shot can: chosen because a swing takes everyone in its arc). A copper golem is a bystander, not an ally.
  Armor stands take aimed shots and give nothing; Sojourn's +1 on an object is for boats and end crystals now.
- **A bystander still gets:** a plain hit marker and a kill feed row if killed; no kill line. Not changed.
- **Not played:** two players with real clients (teams, a pet of a player on another team: code only); a village golem turned by low reputation; a pet of an offline owner (it falls through to bystander); llamas, bees, polar bears
  (the same `NeutralMob` or target path as the wolf: bees, llamas and polar bears now played on flat ground).
- **`Combat.credit` on a target just killed:** asked after the death, so `isBystander` has no alive check. An angered wolf killed in one hit: its target field is still set, so it counts.

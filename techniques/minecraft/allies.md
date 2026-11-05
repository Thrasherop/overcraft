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

## The healing reduction (#103, 2026-11-05)
Damage cuts the target's healing by 30%, a tank's by 15%, for 3 s. Read first, in this order: **BZ** 2026-02-10 ("In-Combat Healing Reduction on Damage: -30%", "Reduced effectiveness against Tanks: -15%", baseline for all heroes), every BZ page to 2026-11-03 (only Quick Play Hacked's temporary 15%, 2026-07-30; Stadium's items are their own); **D** 01C7 #1308 (v25267 = 1A33 0.3 x 1CAC 0.5 on category 0850), 1E68 3 s on #881; **W** Global Passive Abilities rev 287616 (the box is flagged "removed", yet its exemptions table was edited for months after; D and BZ settle it), whose rules we took: no stacking, multiplicative with other modifiers, no self-damage, cleansable, health packs in full. Exempt abilities on W's table: none of ours.
- In `Combat`: `onHurt` (from `AFTER_DAMAGE`) starts or restarts a timer per target; `heal` scales; `clearHealing` on death (Biotic Grenade's two with it: they survived death under the UUID); `cleanse` clears it, and Suzu and Life Grip now cleanse **before** they heal.
- **The trap:** `Allies.wasAlly(mob, player)` is true with no teams (`playersAllied` compares their teams, null and null). Asking it of the attacker made every mob "an ally" and the first build cut nothing: the unit test passed, the beam read 55 HP/s after a husk's hit. Only a player or a pet's owner has a side to ask about.
- **Which hits count, from the graph** (01C7, read 2026-11-05 after the critique; `Combat.onHurt` names the nodes): #1281 message 0011 (damage taken; v18513 the attacker, v35367 its tags) -> #1202 the attacker or its owner is a player hero on the other side (filter m_84065FD1 1, m_66747E7D 2: H nodes, M meaning) -> #882 not tag 191F (Local Anesthetic's damage over time: **built**, `Sleep.quiet`) -> #2712 / #2702 under a mode flag only a Damage hero's (M: Stadium's; not ours) -> #881 the 3 s wait. Read and **undecoded**: the stop #1186 {v6604 or v8440} (nothing in the graph sets them), message 2525 (#1191 -> #1185 -> a 0.25 s wait -> #1184), #610's m_7724EC51 and m_5BF820E4 (its op67(v25267), kind x9007B042 is the healing scale, M, as Ana's B064). That the wait restarts on a hit (m_0DBAFD7F 1) is M.
- **Decisions, ours unless said:**
  - A mob counts as the graph's attacker: Overcraft's fights are against mobs.
  - No attacker, no cut (the graph's filter): a fall, lava, drowning; and in Minecraft burning, poison and wither, which name no attacker, even right after a blaze's hit.
  - A no-team hit from another player or their pet doesn't cut: `Allies` has them as allies, and the graph says enemies only. PvP with teams set cuts.
  - Global regeneration is cut (W Healing: "all forms of healing"): it starts 6 s after a hit and the cut lasts 3, so they meet only in Mercy's Valkyrie, whose regeneration never stops.
  - Medic's self-heal (Mercy, Lifeweaver, Kiriko) is a share of what the ally really got, then cut again if the healer is hurt too: two creatures' reductions, x0.49 at worst. No source says otherwise.
  - Food and potions don't pass `Combat.heal`. Overhealth (`Combat.overhealth`) isn't scaled: no source says. No HUD sign for it: D has effects on the node (3D2F, 7202) not read.
- **Played (dev harness; a bot as target, the real client's Mercy beam, real husks; final merged branch, raw output [g3](../testing/runs/polish2/g3_numbers_chain_mercy.txt)):** a bot Kiriko after a fall 4.39 /s (55 HP/s); just hit by a husk 3.07 /s (38, x0.70). A bot Sigma 4.39, then 3.74 (47, x0.85). The baseline read 55 after the hit (no raw file: that run's output wasn't saved, one more reason this folder exists). By itself: no cut (the second round of the first session, not in g3). From a `/damage ... by husk`: the cut lasted 2.84 s read from outside (3 s, less the first read), and a blow at 1.5 s started it again (3.01 s). On the client, under a bot Lúcio's song: 1.60 /s after a fall, 1.11 after a blow (x0.70); blow, `/kill`, respawn, 1.1 s later: 1.60, so it's cleared on death. Real husks aren't reliable: "husk never hit" in some rows, whose next rates carry regeneration and mean nothing.
- **Sigma's "6.8/s" at 3.3 s after a hit is shields:** `Shields.REGEN` 30 OW/s = 2.4/s from `DELAY` 3 s, 4.39 + 2.39 = 6.78 (W Hit points; `Shields.tick`, which doesn't pass `Combat.heal`). Matches to the digit; not otherwise tested.
- **Suzu and Biotic Grenade:** `cleanse` doesn't clear `antiHealUntil`, pre-existing; W Kiriko rev 381285 says Suzu removes anti-heal: filed, #117.
- **What a critic should check:** a pet's hit on its owner's enemy; a player in Minecraft mode hit by another; a Local Anesthetic tick (built from the graph, not played).

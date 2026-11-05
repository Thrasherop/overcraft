# Polish 4 (fix/polish-4, 2026-11-05): the record

Up: [Techniques](../README.md) · [hand_play](hand_play.md) · [harness](harness.md) · the data: [status_and_cleanse](../game-data/status_and_cleanse.md) · before: [polish3](polish3.md) · issues #119, #120, #121, #122, #123, #124.

**Input: `KeyMapping.set` through DevServer and `look`, not a mouse. Bots are server-side players with no client: not a second player, and they don't move when pushed by a client. Nobody has played this by hand, and there is no Overwatch side by side for any of it.** Marks: D data, BZ patch notes, W wiki, R recording; H/M/L. Overwatch and Battle.net checked closed before each launch and each archive read. Wiki through its API: Kiriko rev 408632, Status effect rev 408841, Brigitte rev 408812.

**This file was rewritten after each of two independent critiques.** Their claims were read, and every bug they ranked was reproduced first ([crit_repro_abde763](runs/polish4/crit_repro_abde763.txt), [crit2_repro_48b1120](runs/polish4/crit2_repro_48b1120.txt)). Their sections below say what each came to. **The last code is 2416667; anything older below says its tree, and what later code changed is said.**

## Which tree ran what
A client can't report its commit. Since abde763, `scripts/p4_launch.ps1` writes `git rev-parse` and a count of dirty `src` files to `run/p4_launch_tree.txt` as it launches, and each script logs that line (`TREE`). **Files before that have a label typed by hand or script, not read:** they're marked "typed".

| Evidence | Tree |
|---|---|
| [p119_before_base](runs/polish4/p119_before_base.txt), [rally_before_base](runs/polish4/rally_before_base.txt), `p_rally_trace_base_*` | base e5e5f66, typed |
| [p119_table](runs/polish4/p119_table.txt), [p119_stun_looks](runs/polish4/p119_stun_looks.txt), [rally_after](runs/polish4/rally_after.txt), [rally_keys](runs/polish4/rally_keys_a20c352.txt), [p4_play](runs/polish4/p4_play.txt), [p4_extra](runs/polish4/p4_extra.txt), [p4_fights](runs/polish4/p4_fights.txt), `p_rally_trace_off_*`, `_on_*` | a20c352, typed. **Superseded by the rows below where those exist;** kept as what they were |
| [crit_repro_abde763](runs/polish4/crit_repro_abde763.txt) | abde763 (a20c352 + dev reads), TREE line |
| [crit_after](runs/polish4/crit_after.txt) | ffb4c49, TREE line |
| [crit_final](runs/polish4/crit_final.txt), [p119_table_final](runs/polish4/p119_table_final.txt), [p4_fights_final](runs/polish4/p4_fights_final.txt) | e9dea81, TREE line. **Not the final code**, kept as the first critique's after |
| [p4_fights_base](runs/polish4/p4_fights_base.txt) | base e5e5f66, detached worktree `../overcraft_p4base`, fresh run dir |
| [crit2_repro_48b1120](runs/polish4/crit2_repro_48b1120.txt) | 48b1120 (e9dea81 + dev reads), TREE line: the second critique's bugs before the fix |
| [crit2_after](runs/polish4/crit2_after.txt), [p119_table_e299f07](runs/polish4/p119_table_e299f07.txt), [third_before_e299f07](runs/polish4/third_before_e299f07.txt), `fights_final_fresh{,2,3}`, [fights_why5](runs/polish4/fights_why5.txt) | e299f07, TREE lines |
| `fights_base_fresh{2,3}`, [fights_ana_base](runs/polish4/fights_ana_base.txt) | base e5e5f66, `../overcraft_p4base`, fresh launch and world each, TREE lines |
| [third_after](runs/polish4/third_after.txt), [behind_2416667](runs/polish4/behind_2416667.txt), [crit2_final_2416667](runs/polish4/crit2_final_2416667.txt), [p119_table_2416667](runs/polish4/p119_table_2416667.txt), [fights_ana_final](runs/polish4/fights_ana_final.txt) | **2416667, the last code**, TREE line, client launched clean. First tries on the uncommitted tree were deleted: `third_after` holds only clean runs |
| [mutations_2416667](runs/polish4/mutations_2416667.txt) | 2416667, throwaway worktree `../overcraft_p4mut` |
| [final_build](runs/polish4/final_build.txt) | `gradlew clean build` at the head of the branch: no code after 2416667, tests and `checkNoOverwatchAssets` pass |

"Off" is the same build with `bot fix 119 off` / `bot fix rally off`. Scripts: [runs/polish4/scripts](runs/polish4/scripts/) (their paths point at this session's scratchpad; `p4_lib`, `p4_hlp` are the helpers). Screenshots: `run/screenshots/`, gitignored (Overwatch models). **27 shots of the first passes, some before a20c352 with no raw file, were moved to `run/screenshots/old_before_a20c352/`: not evidence.** `p4_off_*` and `p4_on_*` now are e9dea81's. `p4_play_rally_off_*` are suspect, see below.

## #119: Protection Suzu
**The data does hold it**, against #117's comment: [status_and_cleanse](../game-data/status_and_cleanse.md). The list is `Combat.cleanse`'s table.

**Reproduced first (base):** dart on a bot ally and on the real client: asleep through a Suzu to the sleep's end; EMP on the real client: glowing and Shift locked (cooldown 0 -> 0). The base file's stun section is void (its Suzu was never thrown).

**Off and on, e9dea81, [p119_table_final](runs/polish4/p119_table_final.txt); again at e299f07 and 2416667, [p119_table_2416667](runs/polish4/p119_table_2416667.txt).** Bot ally = the real key thrown at a bot; real client = a bot Kiriko's Suzu. Each cell one run unless said. +t is from the throw. The float's row changed at e299f07 (below): the rest read the same in the later files' reading of them, not cell by cell in this table.

| State | Bot ally, off | Bot ally, on | Real client, off | Real client, on |
|---|---|---|---|---|
| Sleep | stays | **read awake at +0.49** (asleep at +0.32) | stays | **awake at +0.55** (asleep at +0.35) |
| Rally's stun (reads by tick; controls 15 ticks, 4 of 4 here) | 15, the phase landing inside | **read at the first tick, free at the next read, the phase at tick 12** | 15 | **the same** |
| Hack (silence, glowing, looks) | stay | **gone at the first read, +0.10** | stay; Shift 0 -> 0 | **gone at +0.10; Shift 0 -> 14.45** |
| Anti-heal, healing cut | gone (#117 isn't under this switch) | gone; health 13.2 -> 18.0, **capped at max: at most +60 shown of 80** | gone | gone, the same |
| Local Anesthetic (speed, base 0.1; health) | 0.072, 16.68 | **0.1, 18.0** | 0.07, 16.92 | **0.1, 18.0** |
| Claws Out (speed; health at the end) | 17.36 | **0.1 at +0.10; 18.0** | 17.36 | **0.1 at +0.10; 18.0** |
| Hyper Strike's float (gravity; `bot held`'s look) | runs its time | **Until e299f07: 0.08 at +0.10. Now: stays.** This table can't show it: over by +0.10 both off and on in `p119_table_2416667`, the script's gap. By ticks, `crit2_after:213` on: 13 without a Suzu, 11-13 with | as the bot | **The same** |
| Deceleration Field (speed) | 0.075 | **0.1 in the phase, 0.075 again inside** | 0.075 | **the same** |
| Weaken (damage factor) | 0.6 | **1.0 in the phase, 0.6 after** | 0.6 | **the same** |
| Accretion's knock-down | 1.10 s | **1.10 s: stays** | 1.09 s | **1.13 s: stays** |
| Gravitic Flux | lifted | **stays lifted** | lifted | **stays lifted** |

The stun rows: the read loop stalls about 10 ticks while the script presses, so "free at the next read" means within that gap. Off holds its 15 through the same gap. **Void:** `p119_stun_looks` line 14, a fix-off read of 1 tick: the first version of that script stalled longer than the stun, so "1 read" alone doesn't tell off from on; the evidence is reads at tick +10 to +14, asleep off, free on. **`p119_table` line 37, a control read 13 ticks, not 15:** the span between reads; every control since reads 15.

**Catnapper:** 30 of 30 ticks in `p4_extra`'s control (a20c352), with or without a Suzu, 2 of 2 each. **`p4_extra` line 16 is a third Suzu run reading 22:** that script waits 0.7 s before its first read; its later version reads from the confirm. In [crit_final](runs/polish4/crit_final.txt) mid-carry reads 28 for the same reason.

**Prevented, real client, by tick, off / on:** dart in the phase: asleep / never. EMP (phase 24165-24174): silenced from 24170 / never (phase 34517-34525). Accretion (phase 24336-24345): down from 24344 / never (34727-34735).

**Late viewers** (real client 500 m away, back after the Suzu; the target a bot): off: told `revealed` / `asleep`; on: nothing.

**Sounds, as `bot playing` (SoundEngine's running list) shows:** `sombra.hacked` ran 2.5 s and more past a cleanse at ffb4c49; at e9dea81 gone by +0.75 (the stop lands late, about +0.5: read, not explained), others' `_3p` runs out by +1.25. `brigitte.stun` and `_3p`: run out by +0.75 and past +1.2 at ffb4c49; left alone, short. Two numbed, one cleansed: at abde763 the other's own `anesthetic.dot` was cut; since ffb4c49 it runs on past +2.27.

**Looks:** `crit_final:342-354`'s block is **void, all of it**: its pictures can't tell off from on. The a20c352 ones (`p4_look_hack_*`, `p4_look_sleep_*`) were opened but prove little. Nothing here rests on a look.

**Ours or inferred:** M v8440 means phased; M mask 20; Weaken's and the field's "out of the filter"; a knock-down prevented at our door rather than each hit's filter; Hyper Strike, Claws, Anesthetic on W with no graph read; Hack's own sound stopped (ours; no data on it); glowing from another source goes with the reveal (ours).

## The critique, claim by claim
Reproduced in [crit_repro_abde763](runs/polish4/crit_repro_abde763.txt), after in [crit_final](runs/polish4/crit_final.txt) (same steps) unless said.

| # | Claim | Reproduced | Fix | After |
|---|---|---|---|---|
| 1 | Catnapper tethers the phased | phase 4693-4702, not knocked, carried from 4699 | `land` skips them: 5B7B's mask 20 (M), W | phase over the landing: 1 in crit_final, 6 offsets in crit_after: none knocked, none carried, 18.0 |
| 1b | Suzu mid-carry | - | **Decided: stays.** W Status effect "Catnapper: Cannot be cleansed". Row in the table | 3 of 3: phased, carried on 50 ticks |
| 2 | Soundwave's lockout on the phased | slowness, speed 0 | not hit at all (W) | no effect, speed 0.1, z unmoved, 18.0 |
| 2b | Flux's slow has no row | - | row: part of the Flux W calls unaffected; ours not to apply W's "slows" | - |
| 3 | EMP on a phased Sombra | stealth read 4 ticks into the phase, then ended; `emp.hit` heard | not hit: 0B31's mask 20 (M) | stealth holds 30 ticks read, no `emp.hit` |
| 4 | Hyper Strike's look and sound | phased: look and sound; cleansed: look to +0.5 | not hit, charge not spent (ours); look ends | no sound, no look, gravity 0.08; look gone at +0.1 |
| 4b | Its sound after a cleanse | runs 1.9 s | **left:** it's the blow's impact, already struck | - |
| 5 | Rally overwrites a knock-down | Suzu ended it at 4 ticks; without a Suzu 18 (the stun also cut Accretion's 22) | stays a knock-down, the longer of the two | 22 of 22 ticks with the phase inside, 3 of 3 |
| 6 | Anesthetic's stop | the other's `dot` cut | their own to them alone; 3p only with no other numbed in the 45 m it plays at (cited in the code; the 53 m gone) | the other's `dot` runs on |
| 7 | Stun branch drops Flux's hold | held ended at tick 5 of 8 | `Sleep.release`: only if nothing holds | 8 of 8 |
| 7b | **Also found:** Flux's end frees a stunned player | control: held 8 ticks, stun 15 | the same | held 16, stun 14 reads |
| 8 | Stale comments | read | Sleep's type now 4EB6; Disruptor's javadoc in place | - |
| - | Shockwave through wall, barrier | zombie behind a wall stunned and hit, bot behind her barrier stunned and hit, barrier 300 | **issue #122**, predates this | not fixed |
| - | Dart spent on the phased | burst played | passes through (`canHitEntity`) | no burst; health 18 |
| - | Bash picks the phased | read in the code | filter | passes; 18.0, z unmoved |
| - | **Also:** whip on the phased | `whip.hit_3p` heard | filter | no hit sound |
| - | `phaseStops` through other phases | - | documented on `phaseStops` | below |

**The other phases.** Resurrect (2.25 s): D 00CA #677 sets v8440. Translocator (0.4 s): D 0C19 #330 sets v8440. Swift Step (0.25 s): no v8440 in her graphs read but the shared 1065; W "phased"; W Status effect "prevents most negative effects"; ours that it's this list. None calls cleanse (W for Swift Step; ours for the other two: the data's readers would end a status there). **Not shown in game:** the scripts couldn't land a dart in those windows: Swift Step's mover took her out of the line; Translocator's leads either missed or hit before she left; Resurrect's dart flew over the cat. [crit_final](runs/polish4/crit_final.txt) keeps them as tries.

**Other corrections to this file's first version:** "heal +80" was capped; "gone by +0.3" etc. were the wrong rows; 0E34 as Rally's type was wrong and the "M" with it: **224E's public schema default for 6BD6 is 4EB6** (H, `dump_ss` `m_publicSchema`), which `kit_shield_bash.md`'s "L" hadn't read. Accretion's filter: 1EB9 #338 mask 20 now read, 224E #575 lets 6BBE through. **"Both stunned" in `rally_keys` wasn't in that file:** one zombie per line, its last-read list; [crit_after](runs/polish4/crit_after.txt) reads two at +0.2 s; the distances are in that block's header and the scripts.

**`p4_play`'s "off a 14.4" and `crit_after:164`'s [14.4, 14.4, 10.0], settled:** a script bug, not the game. The tagged zombies were read with `limit=1` and no sort, so a stale tagged entity from an earlier block answered (shown: `limit=1` 10.0, `sort=nearest` 19.0, the 10 being a cat). The scripts now always read `sort=nearest`; [harness](harness.md).

## #121: Kiriko's Suzu line, as the data has it (owner, 2026-11-05: "follow the data")
**Re-read first** (scratch decompiles of 180C, 30D8, 224E, 0B1F, 0C72, 5BAF, 01C7, 6123): the graph says what the issue says.

- A status reading v8440 sends itself **1C7D** on its way out. 180C (the Suzu's status 031A) #93 hears it -> #131 on v7310, which #67 -> #129 (0.65 s) -> #130 sets -> **#88 sends 1955 to v40**, the sender of #67, her. 30D8 #88 hears it -> #90 adds the ally to v1839 -> **#93 waits 0.5 s -> #89 plays FC5**, 10 s between (Voice's table, FC5's cooldown). Also on #88: #99 a counter, #100 -> #103 an achievement's criteria: nothing to hear. The throw's own #85 is FC1, which has no lines.
- **An actual cleanse = a status that sends 1C7D ending by it.** Of the mod's: Sleep Dart 224E #418, Rally's stun 224E #416, Hack 0B1F #611, Biotic Grenade 0C72 #275, Claws Out 5BAF #91 (H nodes). Silent: healing reduction (01C7 #1186 ends with no 1C7D), Local Anesthetic (6123 has neither v8440 nor 1C7D), the zones' slows (1443, 6102, trackers), knock-downs and floats, which stay. **Ours:** Minecraft's effects and fire have no graph: no line, following the data, the owner's to change.
- **Prevented isn't told, mostly:** 224E #96 / #575 end a sleep or stun on a phased one with no 1C7D. But 0C72's and 0B1F's Entry goes to the same switch, so a grenade or Hack **starting** on a phased one would tell it, which is what the 0.65 s window is for. Whether either starts there wasn't found, and the mod's phase refuses them first, so here the line comes only from the burst and the window is always met. Not built; L.
- Life Grip's cleanse (21EE) sends the same 1BC5, so the statuses tell, but she has no 180C listening. 51F4 #439 hears 1C7D for Lifeweaver: not read; no change here.

**Reproduced first, e299f07, 3 rounds, [third_before_e299f07](runs/polish4/third_before_e299f07.txt):** the line on every throw: at nobody, a healthy ally, a slept, a poisoned, a hurt one, each heard at +0.1. **After, 2416667, 2 rounds, 11 s apart, [third_after](runs/polish4/third_after.txt):**

| Throw | Line | After the burst |
|---|---|---|
| Hits nobody; healthy ally; hurt ally (healed all the same) | **none**, 6 of 6 | |
| Poison (removed); numbed only (removed; the dart's own target taken away first); Accretion's knock-down (stays) | **none**, 6 of 6 | |
| Sleep; Biotic Grenade's anti-heal; Claws Out | **the line** | 0.3-0.4 by the reads, real key; the press's own gap is in that |
| Hack (EMP on the ally; a real Kiriko past it) | **the line**, silence and glow gone | 0.5 |
| Rally's stun, thrown as it reads | **the line** | 0.4-0.5 |
| Rally's stun, thrown after it ended by itself | **none**, 2 of 2: there's nothing to cleanse | |
| A bot Kiriko cleansing the real client asleep | the line, heard here | 0.5 |
| A cleanse, another 3 s later (cooldown reset), another 14 s on | line, **none**, line: her 10 s | |
| She switches hero inside the 0.5 s | none | |

**A trap found on the way:** the stun row first read "no line" because five requests went between the stun's read and the throw, at 0.1 s or so each, and it was over: right, and the script's fault. So the throw goes first now, and the late row stays as its control. **Not tried:** a Kiriko dying inside the 0.5 s; cleansing herself; a person listening; Overwatch side by side.

## The second critique, claim by claim
Before: [crit2_repro_48b1120](runs/polish4/crit2_repro_48b1120.txt) and the `p4_r*` runs in it. After on e299f07: [crit2_after](runs/polish4/crit2_after.txt); again on the last code: [crit2_final_2416667](runs/polish4/crit2_final_2416667.txt), [behind_2416667](runs/polish4/behind_2416667.txt).

| # | Claim | Reproduced | Fix | After |
|---|---|---|---|---|
| 1 | Accretion's rock lands on the phased | rock spent, ult +38, target hit; behind 200 | one rule (4) | target untouched, the zombie behind 190.55 |
| 2 | A knock-down removable under a stacked hold | Hyper on Accretion freed at 1 read; a dart on a knock-down slept 95 ticks | `Holds.stacked`, a table, **ours** (224E's replace rule isn't read): never makes a hold cleansable that wasn't, never shortens one; a dart doesn't land on a knock-down or float; a stun on a sleeper doesn't land; a dart on a stun does (it wakes the same way) | Accretion + Hyper + Suzu 22 / 21 ticks, gravity back to 0.08 only; dart refused |
| 3 | Quick melee lands on the phased | Claws spent, `melee.hit` | goes through the rule | Claws cd 0, `melee.swing`; Hyper's charge goes to the zombie (192.9), not spent on the phased |
| 4 | Every hit stops on the phased | kunai, sonic, sphere, railgun, thorn, paw, disruptor, Ana's dart, sleep dart, rock, pistol: stopped | `OwProjectile.canHitEntity`, `Allies.canHurt`, `areaHits`: all ask `Combat.phaseStops`; door tests gone | [behind_2416667](runs/polish4/behind_2416667.txt): those, **plus mediblaster, whip, bash, Catnapper**: control hits the target, phased: 18 -> 18 and what's behind takes it. Melee doesn't reach behind (200): `melee.swing`. Blaster not run |
| 4b | Whiplash's marker, push on a phased mob | read | PUSHED drops the phased | below, Whip: not shown |
| 5 | `/kill`, the void | "Killed", 18.0 | `refusesRule`: what beats Minecraft's invulnerability gets through (ours); vanilla's tag holds `generic_kill`, `out_of_world` | `/kill` dies, void dies; blast on the phased 18 -> 18, control 18 -> 0 (an ignited creeper: a bed by the key wouldn't explode for the harness, **#27's own case not shown**) |
| 6 | `canHitEntity` on the client | yes: `ThrowableProjectile.tick` runs it on the client's copy | `phaseStopsRule`: the client never asks | the server's word decides; the client's copy may look to stop for a tick |
| 7 | #122 | zombie behind a wall stunned and hit; bot behind her barrier stunned and hit | `hasLineOfSight`, `Barrier.shields` (0EE3 #877's tracker, 002C) | wall 20.0; barrier square-on 20 -> 20, no stun, against 14.96 and stunned, 3 each, barrier 300 |
| 7b | "the shockwave's 70" against the raw reads | noon sun burn explains 10.4 / 11.4 against 14.4; the bot's 3.04 was wrong: 20 -> 14.96 = 63 OW (armor 7) | - | comment on #122 |

| Evidence asked for | Came to |
|---|---|
| Hyper's charge | logged: spent on the first swing, second swing nothing extra; phased target: goes to the zombie |
| Rally's 18 -> 22 | Reads started properly: Accretion alone 22; + Rally 22 (health 2.8); + Rally + Suzu 22 with the phase inside, 3 of 3 (health 9.2 with the heal). An earlier "22 with Rally" had no bash landing (9.2): void |
| Stun's cleanse timing | controls 15 (once 14); Suzu 1 read, then the phase read 4 ticks later, the script's press gap, 7 of 7. Earlier flakes were a Kiriko too far away |
| Hold exits, from stun, knock-down, Flux | death, G, leave all free, walks. Hero switch: **refused once while stunned** (the hero menu's own mercy), knock-down and Flux switch. G inside Flux stays lifted to its end |
| Anesthetic's third listener | a real client not numbed, two others numbed: hears the other's 3p go on |
| Float cleansed? | **No, decided.** W's list means its damage (W Sigma rev 409089: "50 over 0.37 seconds"); W "never removes Knockback"; the float is the knock-up's. Ours; `Holds`, `Combat.cleanse`'s table. Held 11-13 reads through a Suzu, gravity stays |
| `SigmaPerks` charge | cites W Sigma rev 409089 now |
| #124 | repro steps and W against D posted on the issue |

## #120: Rally's double hit
0EE3 #877 -> #890 STU_DA54F8F7 {v1184, v375} -> #886, #887 on v1184. H nodes, M that it removes (add and clear siblings).

| Rally, bot Brigitte | Base | a20c352, fix off | a20c352, fix on |
|---|---|---|---|
| Real client, hero: damage, peak speed, moved | 140, 30.4, 19.7 m | 140, 30.4, 19.7 m | **70, 17.2, 7.9 m** |
| Real client, Minecraft mode | 140, 26.1, 11.8 m | 140, 26.1, 11.8 m | **70, 18.0, 4.1 m** |
| NoAI husk | 140 | 140 | **70** |

Own keys on real zombies, e9dea81: above. A phased bot in the wave: no stun, 18 -> 18; control without a Suzu: stun 10 reads, 18 -> 12.4; the bashed zombie 14.4 both. Unit-tested: `BrigitteRulesTest` (`shockwaveHits`, used by the shockwave; fails if the exclusion or the slot goes).

## Whip Shot's 8 m (owner, 2026-11-05: "If wiki says 8m then we should use 8m")
W Brigitte rev 408812: "~8 meter knockback distance on flat ground". D 133E keeps its 25 m/s and its one push (#994 -> #638); the hold's length is ours, chosen to reach W. Comment: `BrigitteStats`; reference: `tools/reference/brigitte.md`.

**Why a player fell short:** a player got one send at the first tick and the push ended when they landed; the lift is 2 m/s, so about 0.24 s of flight. The server can't send a hold each tick (its copy of their vertical speed is stale: critique_systems S4), so **their client holds it now** (`WhipPushPayload`, `ClientBrigitte.whipped` and `tick`): 0.3 s in hero mode, whose ground stops them at once, and 0.25 s in Minecraft mode, as a mob. Bots are server-side players and don't run this: no distance here is a bot's.

Bot Brigitte 5 m from a real client, or a real Brigitte by keys 5 m from a husk. Before, e299f07, 3 of 3, [third_before_e299f07](runs/polish4/third_before_e299f07.txt); after, 2416667, 3 of 3, [third_after](runs/polish4/third_after.txt):

| Target | Before | After |
|---|---|---|
| Hero, flat | 5.88 m | **8.48 m**, speeds 25 for 5 ticks, then 23.7, 13.4, 5.6, 1.9 |
| Minecraft mode, flat | 4.25 m | **7.75 m** |
| Husk, flat (zombie 1 run) | 7.75 m (the old record's "about 5.6" was an older read) | **7.75 m**, unchanged |

| Around it, after | Seen |
|---|---|
| Wall 3 m behind (hero, Minecraft mode; husk at 4) | stops at it, 2.2 m (husk 3.2), the whip's damage only, no slide or bounce in the speeds |
| Whiplash (Major Perk), wall at 3 m, 7 m | 60 more: 20 -> 9.6, at 2.2 m and 6.2 m; husk 200 -> 189.6 |
| Whiplash, no wall; wall at 12 m (past the push) | no slam: 14.4, 8.48 m |
| Stairs ahead, hero | **stops at the first stair**: 2.2 m and 6.2 m, y unchanged. Husk: climbs one, stops at 3.2 m. Not shown against the old build; the lift has them in the air, where Minecraft doesn't step up |
| Stairs down, hero / husk | 17.65 m / 11.67 m, no fall damage on the hero (14.4); 192.4 on the husk: flying on at 25 m/s until they land |
| Off a 3 m ledge | hero 16.1 m, Minecraft mode 10.97 m, husk 10.97 m, the same way |
| Jumping as it comes | 12.9 m: longer in the air |
| Running at her, W held | 3.6-4.15 m, then walks on |
| A second whip landing in the hold (two bots in line) | both hit, 20 -> 8.8. Hero 12.93 m twice, 8.48 once; Minecraft mode 11.25 m, 3 of 3. The second re-arms the hold, as the mob's list does |
| Phased as it hits (`bot phase me 1` after the health read) | 8.48 m: **a read too late to stop the hold**, so this shows nothing about `letGo`; it's in the code, not shown working |
| `/kill` in the hold, respawn | back at the spawnpoint, stands still |

**The mod has no Deflect** (W lists the whip among what can be reflected: nothing here reflects). **Not tried:** a real target's hold when stunned in it (the dart row's dart didn't land); a person's feel of it; a second client watching; Rally's bash just after; water, ladders. Stairs aren't a real slope: the mod's world has no others.
## Tests
The first version's tests were three pure helpers: the critic removed the fixes and they still passed. **The decisions the shipped code uses now live in `Holds` and small rule methods**, and `HoldsTest`, `PhaseRulesTest`, `ana/AnestheticRuleTest`, `brigitte/BrigitteRulesTest` test those. **Each fix removed in a throwaway worktree, [mutations_2416667](runs/polish4/mutations_2416667.txt) (`scripts/p4_mut.py`): 18 of 18 removals fail a test.** Three call sites pass with their line changed, and were expected to: whether `Sleep.cleanse` asks `Holds`, whether `Allies.canHurt` passes the phase, whether the Suzu's line asks `told`. **Those need a live world and can't be tested offline;** the game runs above are their evidence.
## Played like a player, as far as a script goes (e9dea81, crit_final)
- **Poison, fire, a beneficial potion:** real client: poison, wither, slowness gone; strength and speed kept. Burning: 157 -> -20 (out), the fire block removed first; a fire block still under her re-lights her. Bot ally: poison gone, regeneration and fire resistance kept. Wolf: speed kept, poison gone, Fire 186 -> 0, 2 of 3 (the third Suzu didn't reach it: Fire 187 -> 166, health down).
- **Right after a cleanse:** G: free, walks, Minecraft speed. Hero switch: free, walks. Leave and rejoin by the pause menu: free, nothing left, walks. Death: a **phased hero survives `/kill`** (pre-existing: the phase cancels all damage), so killed 0.3 s later, after the phase: back free, 18.0, walks.
- **Life Grip moving a slept real client:** awake at tick 9, first moved the next tick; not slept: first moved tick 8. z 8.5 -> -1.75 both. A bot isn't moved: no client.
- **Mob allies:** tamed cat slept and woken, health 5.6 -> 10. Wolf (`p4_play`, a20c352). A villager: the dart passes it (everyone's ally), so it never arises.
- **Creeper: settled, e299f07.** A hacked creeper does go off: control blows at about 1.7 s, EMP'd about 5 s, 2 runs each with its target logged: the 3 s silence, then its fuse. The earlier "no blast in 7 s" was out of its distance and filtered by team (`p4_final2.py:39`). A Suzu never reaches a creeper: Allies calls it an enemy.
- **Places, asleep then woken by a bot's Suzu:** ladder (at its foot, y -60; Ana to the side), boat (y -60.41, seated by y; the passenger read failed), lava (fire resistance), shallow water (`p4_fin_place_water`, at y -61): asleep, then free in 10-11 ticks, each one run. **Under deep water: not shown:** a dart doesn't reach. Nether: poison gone, night vision kept, by her own key (bots live in the overworld).
- **Fights, rerun like for like:** fresh launch, fresh world, TREE line, the same script, seed fixed, midnight, 30 s a round, harness input, not a person. The old table (`p4_fights_*`) is withdrawn: its final ran late in a long session, and its count was off. **"mobs 5" was the script:** `count()` read after 0.08 s in a slow session; by type it was 3 + 2 + 1 = 6 ([fights_why5](runs/polish4/fights_why5.txt)); read after 0.3 s, every round says 6. `fights_final_fresh` predates that fix. The script's death count also misses some: `fights_final_fresh2` has an Ana round at hp 0.0 with 0 deaths, and a Kiriko round with two death lines counted 1. Counts are counts of `died at`, so low.

| Deaths / rounds | Base e5e5f66 | e299f07 | 2416667 |
|---|---|---|---|
| Kiriko, Brigitte, Sombra (`fights_{base,final}_fresh*`) | 5 / 12 (2 runs) | 7 / 18 (3 runs) | - |
| Ana, in those sets | **0 / 8** | **5 / 12** | - |
| Ana alone, ten rounds from a fresh launch ([fights_ana_base](runs/polish4/fights_ana_base.txt), [fights_ana_final](runs/polish4/fights_ana_final.txt)) | **3 / 9** (the 10th cut off by the run's time limit) | - | **4 / 10** |

Causes are creepers and skeletons on both trees. **Ana: the 0 of 8 against 5 of 12 didn't hold up when she was run on her own (3 of 9 against 4 of 10), and pooled it's 3 of 17 against 9 of 22.** So a difference isn't shown and isn't excluded: small counts, and no code path from this branch into a fight with no Suzu, no ally, no sleep was found by reading. Scripted aim. Not called noise; say if you want more rounds. No crash in any of them.

## What a critic should check
- Most cells are one run; timing reads have gaps; two rows lean on `bot held`.
- A bot's `press` skips the silence check; in the Hack row on a real client the thrower had been EMPed.
- Bot Kiriko's Suzu sometimes doesn't reach (crit_final says when): those runs read as misses, not rows.
- #122 fixed here (`Refs`); #123 (a wake stops every bed), #124 (Life Grip and tether, gravity: decision W against D on the issue) are filed, not fixed. #121 and Whip Shot's distance are done, on the owner's word.
- No second client saw any look end. No person, no Overwatch side by side. Real-key play is the coordinator's, not in here.
- The Suzu line's "heard" is the real client's sound list read every 0.1 s or so; its 0.5 s is timed against the phase read, whose first read lags the burst (0.3-0.5 shown with a real key, 0.5 with a bot's press).
- Minecraft's effects and fire say no line: ours. Local Anesthetic says none though the mod cleanses it (W): the data has no reader of the cleanse there at all, so which half is right isn't known.
- The bots' shots at the real client in the fights are a script's, and the Ana counts are small.
- Blaster behind a phased target: not run (needs her weapon out). Translocator: don't lean on `crit_final:222`, a reading after a timed-out wait; Resurrect's, Translocator's and Swift Step's stops are D or W, not shown here.

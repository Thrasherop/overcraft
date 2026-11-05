# Polish 4 (fix/polish-4, 2026-11-05): the record

Up: [Techniques](../README.md) · [hand_play](hand_play.md) · [harness](harness.md) · the data: [status_and_cleanse](../game-data/status_and_cleanse.md) · before: [polish3](polish3.md) · issues #119, #120, #121, #122, #123, #124.

**Input: `KeyMapping.set` through DevServer and `look`, not a mouse. Bots are server-side players with no client: not a second player, and they don't move when pushed by a client. Nobody has played this by hand, and there is no Overwatch side by side for any of it.** Marks: D data, BZ patch notes, W wiki, R recording; H/M/L. Overwatch and Battle.net checked closed before each launch and each archive read. Wiki through its API: Kiriko rev 408632, Status effect rev 408841, Brigitte rev 408812.

**This file was rewritten after an independent critique.** The critique's claims were all read, and every bug it ranked was reproduced first ([crit_repro_abde763](runs/polish4/crit_repro_abde763.txt)). Its section below says what each came to.

## Which tree ran what
A client can't report its commit. Since abde763, `scripts/p4_launch.ps1` writes `git rev-parse` and a count of dirty `src` files to `run/p4_launch_tree.txt` as it launches, and each script logs that line (`TREE`). **Files before that have a label typed by hand or script, not read:** they're marked "typed".

| Evidence | Tree |
|---|---|
| [p119_before_base](runs/polish4/p119_before_base.txt), [rally_before_base](runs/polish4/rally_before_base.txt), `p_rally_trace_base_*` | base e5e5f66, typed |
| [p119_table](runs/polish4/p119_table.txt), [p119_stun_looks](runs/polish4/p119_stun_looks.txt), [rally_after](runs/polish4/rally_after.txt), [rally_keys](runs/polish4/rally_keys_a20c352.txt), [p4_play](runs/polish4/p4_play.txt), [p4_extra](runs/polish4/p4_extra.txt), [p4_fights](runs/polish4/p4_fights.txt), `p_rally_trace_off_*`, `_on_*` | a20c352, typed. **Superseded by the rows below where those exist;** kept as what they were |
| [crit_repro_abde763](runs/polish4/crit_repro_abde763.txt) | abde763 (a20c352 + dev reads), TREE line |
| [crit_after](runs/polish4/crit_after.txt) | ffb4c49, TREE line |
| [crit_final](runs/polish4/crit_final.txt), [p119_table_final](runs/polish4/p119_table_final.txt), [p4_fights_final](runs/polish4/p4_fights_final.txt) | **e9dea81, the last code commit**, TREE line (`p119_table_final`'s and the fights' reading it or the same client session) |
| [p4_fights_base](runs/polish4/p4_fights_base.txt) | base e5e5f66, detached worktree `../overcraft_p4base`, fresh run dir |
| [final_build](runs/polish4/final_build.txt) | `gradlew clean build`, no code since e9dea81: tests and `checkNoOverwatchAssets` pass |

"Off" is the same build with `bot fix 119 off` / `bot fix rally off`. Scripts: [runs/polish4/scripts](runs/polish4/scripts/) (their paths point at this session's scratchpad; `p4_lib`, `p4_hlp` are the helpers). Screenshots: `run/screenshots/`, gitignored (Overwatch models). **27 shots of the first passes, some before a20c352 with no raw file, were moved to `run/screenshots/old_before_a20c352/`: not evidence.** `p4_off_*` and `p4_on_*` now are e9dea81's. `p4_play_rally_off_*` are suspect, see below.

## #119: Protection Suzu
**The data does hold it**, against #117's comment: [status_and_cleanse](../game-data/status_and_cleanse.md). The list is `Combat.cleanse`'s table.

**Reproduced first (base):** dart on a bot ally and on the real client: asleep through a Suzu to the sleep's end; EMP on the real client: glowing and Shift locked (cooldown 0 -> 0). The base file's stun section is void (its Suzu was never thrown).

**Off and on, e9dea81, [p119_table_final](runs/polish4/p119_table_final.txt).** Bot ally = the real key thrown at a bot; real client = a bot Kiriko's Suzu. Each cell one run unless said. +t is from the throw.

| State | Bot ally, off | Bot ally, on | Real client, off | Real client, on |
|---|---|---|---|---|
| Sleep | stays | **read awake at +0.49** (asleep at +0.32) | stays | **awake at +0.55** (asleep at +0.35) |
| Rally's stun (reads by tick; controls 15 ticks, 4 of 4 here) | 15, the phase landing inside | **read at the first tick, free at the next read, the phase at tick 12** | 15 | **the same** |
| Hack (silence, glowing, looks) | stay | **gone at the first read, +0.10** | stay; Shift 0 -> 0 | **gone at +0.10; Shift 0 -> 14.45** |
| Anti-heal, healing cut | gone (#117 isn't under this switch) | gone; health 13.2 -> 18.0, **capped at max: at most +60 shown of 80** | gone | gone, the same |
| Local Anesthetic (speed, base 0.1; health) | 0.072, 16.68 | **0.1, 18.0** | 0.07, 16.92 | **0.1, 18.0** |
| Claws Out (speed; health at the end) | 17.36 | **0.1 at +0.10; 18.0** | 17.36 | **0.1 at +0.10; 18.0** |
| Hyper Strike's float (gravity; `bot held`'s look) | runs its time | **0.08 at +0.10** | runs its time | **0.08 at +0.30** (still 0.001 at +0.10) |
| Deceleration Field (speed) | 0.075 | **0.1 in the phase, 0.075 again inside** | 0.075 | **the same** |
| Weaken (damage factor) | 0.6 | **1.0 in the phase, 0.6 after** | 0.6 | **the same** |
| Accretion's knock-down | 1.10 s | **1.10 s: stays** | 1.09 s | **1.13 s: stays** |
| Gravitic Flux | lifted | **stays lifted** | lifted | **stays lifted** |

The stun rows: the read loop stalls about 10 ticks while the script presses, so "free at the next read" means within that gap. Off holds its 15 through the same gap. **Void:** `p119_stun_looks` line 14, a fix-off read of 1 tick: the first version of that script stalled longer than the stun, so "1 read" alone doesn't tell off from on; the evidence is reads at tick +10 to +14, asleep off, free on. **`p119_table` line 37, a control read 13 ticks, not 15:** the span between reads; every control since reads 15.

**Catnapper:** 30 of 30 ticks in `p4_extra`'s control (a20c352), with or without a Suzu, 2 of 2 each. **`p4_extra` line 16 is a third Suzu run reading 22:** that script waits 0.7 s before its first read; its later version reads from the confirm. In [crit_final](runs/polish4/crit_final.txt) mid-carry reads 28 for the same reason.

**Prevented, real client, by tick, off / on:** dart in the phase: asleep / never. EMP (phase 24165-24174): silenced from 24170 / never (phase 34517-34525). Accretion (phase 24336-24345): down from 24344 / never (34727-34735).

**Late viewers** (real client 500 m away, back after the Suzu; the target a bot): off: told `revealed` / `asleep`; on: nothing.

**Sounds, as `bot playing` (SoundEngine's running list) shows:** `sombra.hacked` ran 2.5 s and more past a cleanse at ffb4c49; at e9dea81 gone by +0.75 (the stop lands late, about +0.5: read, not explained), others' `_3p` runs out by +1.25. `brigitte.stun` and `_3p`: run out by +0.75 and past +1.2 at ffb4c49; left alone, short. Two numbed, one cleansed: at abde763 the other's own `anesthetic.dot` was cut; since ffb4c49 it runs on past +2.27.

**Looks opened at 1280x720:** `p4_look_hack_off_b`, `_on_a`, `_on_b`, `p4_look_sleep_on_a`, `_on_b` (a20c352, those ends unchanged since). `p4_look_strike_on_a` (e9dea81): the bot ally in the lifted look, red, pushed away. **`_on_b` proves nothing: he has left the frame.** Hyper Strike's look ending rests on `bot held`'s client map: lifted to +0.5, gone at +0.7 at abde763; gone at +0.1 at ffb4c49 and e9dea81. On oneself it can't be seen first person (`p4_fin_strike_after`, `p4_rep_strike_after` show nothing either way).

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

**`p4_play`'s "off a 14.4":** that run logged no reply. `crit_after` logged replies: two more passes read off and on a 14.4, with c at 10 where c should be untouched, both right after the creeper block. Five runs in `crit_final` with replies, off first in a fresh session: off a 8.8, 3 of 3; on 14.4, 2 of 2; c 20; `rally_keys` 3 of 3 the same. **Not explained:** something else was hitting zombies there. On is never affected. Treat `p4_play_rally_off_*` and those two passes as suspect.

## #120: Rally's double hit
0EE3 #877 -> #890 STU_DA54F8F7 {v1184, v375} -> #886, #887 on v1184. H nodes, M that it removes (add and clear siblings).

| Rally, bot Brigitte | Base | a20c352, fix off | a20c352, fix on |
|---|---|---|---|
| Real client, hero: damage, peak speed, moved | 140, 30.4, 19.7 m | 140, 30.4, 19.7 m | **70, 17.2, 7.9 m** |
| Real client, Minecraft mode | 140, 26.1, 11.8 m | 140, 26.1, 11.8 m | **70, 18.0, 4.1 m** |
| NoAI husk | 140 | 140 | **70** |

Own keys on real zombies, e9dea81: above. A phased bot in the wave: no stun, 18 -> 18; control without a Suzu: stun 10 reads, 18 -> 12.4; the bashed zombie 14.4 both. Unit-tested: `BrigitteRulesTest` (`shockwaveHits`, used by the shockwave; fails if the exclusion or the slot goes).

## Whip Shot's 8 m
**The brief's "no source" was wrong: W has it** (Brigitte rev 408812). Comment: D 133E 25 m/s, one push; the 0.25 s hold is ours, aimed at W's 8 m, reaches 5.88 m on a hero, about 5.6 m on a husk. Behaviour unchanged; the owner's. W's V:3 against the mod's 2 up: D's vector has 2 (#638), `m_583C208B` = 3 unread, L.

## Tests
`SleepRulesTest` (`Sleep.cleansing`, `slidesAfter`: what `cleanse` and `stun` branch on) and `BrigitteRulesTest`. **The doors themselves (`phaseStops` in each kit) can't be tested offline: they need a live world.** Run with the fix removed: not done; they fail by construction on those functions only.

## Played like a player, as far as a script goes (e9dea81, crit_final)
- **Poison, fire, a beneficial potion:** real client: poison, wither, slowness gone; strength and speed kept. Burning: 157 -> -20 (out), the fire block removed first; a fire block still under her re-lights her. Bot ally: poison gone, regeneration and fire resistance kept. Wolf: speed kept, poison gone, Fire 186 -> 0, 2 of 3 (the third Suzu didn't reach it: Fire 187 -> 166, health down).
- **Right after a cleanse:** G: free, walks, Minecraft speed. Hero switch: free, walks. Leave and rejoin by the pause menu: free, nothing left, walks. Death: a **phased hero survives `/kill`** (pre-existing: the phase cancels all damage), so killed 0.3 s later, after the phase: back free, 18.0, walks.
- **Life Grip moving a slept real client:** awake at tick 9, first moved the next tick; not slept: first moved tick 8. z 8.5 -> -1.75 both. A bot isn't moved: no client.
- **Mob allies:** tamed cat slept and woken, health 5.6 -> 10. Wolf (`p4_play`, a20c352). A villager: the dart passes it (everyone's ally), so it never arises.
- **Creeper: not reachable.** Allies calls a creeper an enemy even on the Kiriko's team, so a Suzu never takes it for an ally; the line in `Hack.cleanse` was dead and is gone. Hacked with and without a Suzu: alive 7 s, no blast, in both (`p4_fin_creeper_True_b` opened: it stands, outlined): why it didn't light after the 3 s isn't read.
- **Places, asleep then woken by a bot's Suzu:** ladder (at its foot, y -60; Ana to the side), boat (y -60.41, seated by y; the passenger read failed), lava (fire resistance), shallow water (`p4_fin_place_water`, at y -61): asleep, then free in 10-11 ticks, each one run. **Under deep water: not shown:** a dart doesn't reach. Nether: poison gone, night vision kept, by her own key (bots live in the overworld).
- **Fights, same script, seed fixed, midnight, 30 s, harness input, not a person:** mobs left of 6, deaths, lowest hp:

| Hero | Base e5e5f66 | e9dea81 |
|---|---|---|
| Kiriko | 2, 1, 3.0 / 1, 0, 13.6 | 2, 0, 16.0 / 2, 0, 1.9 |
| Brigitte | 2, 0, 16.6 / 3, 0, 16.6 | 2, 0, 13.1 / 2, 1, 2.0 |
| Ana | 3, 0, 17.0 / 5, 0, 2.0 | 3, 1, 5.0 / 2, 1, 1.7 |
| Sombra | 2, 0, 9.0 / 2, 0, 10.5 | 4, 0, 10.3 / 4, 0, 8.1 |

Eight rounds a side: base 1 death, final 3; Sombra's mobs left are worse here. Scripted aim, and nothing in the change should act in these fights (no Suzu, no ally, no sleep): read it as noise, or check it: not explained further. No crash. An earlier cut-off Ana round is in the file. Each came back the same hero. Log: start-up errors only. Shots not all opened.

## What a critic should check
- Most cells are one run; timing reads have gaps; two rows lean on `bot held`.
- A bot's `press` skips the silence check; in the Hack row on a real client the thrower had been EMPed.
- Bot Kiriko's Suzu sometimes doesn't reach (crit_final says when): those runs read as misses, not rows.
- #122, #123 (a wake stops every bed), #124 (Life Grip and tether, gravity) are filed, not fixed. #121 and Whip Shot's distance are the owner's.
- No second client saw any look end. No person, no Overwatch side by side.

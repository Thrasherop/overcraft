# Polish 4 (fix/polish-4, 2026-11-05): the record

Up: [Techniques](../README.md) · [hand_play](hand_play.md) · [harness](harness.md) · the data: [status_and_cleanse](../game-data/status_and_cleanse.md) · before: [polish3](polish3.md) · issues #119, #120, #121.

**Input: `KeyMapping.set` through DevServer and `look`, not a mouse. Bots are server-side players with no client: not a second player. Nobody has played this by hand, and there is no Overwatch side by side for any of it.** Marks: D data, BZ patch notes, W wiki, R recording; H/M/L. Overwatch and Battle.net checked closed before each launch and each read of the archive. Wiki read through its API: Kiriko rev 408632, Status effect rev 408841, Brigitte rev 408812.

## Which tree ran what
| Evidence | Tree |
|---|---|
| [p119_before_base](runs/polish4/p119_before_base.txt), [rally_before_base](runs/polish4/rally_before_base.txt), `p_rally_trace_base_*` | **base e5e5f66**, unmodified |
| [p119_table](runs/polish4/p119_table.txt), [p119_stun_looks](runs/polish4/p119_stun_looks.txt) (client session 1) | **a20c352** |
| [rally_after](runs/polish4/rally_after.txt), [rally_keys](runs/polish4/rally_keys_a20c352.txt), [p4_play](runs/polish4/p4_play.txt), [p4_extra](runs/polish4/p4_extra.txt), [p4_fights](runs/polish4/p4_fights.txt), `p_rally_trace_off_*`, `_on_*` (session 2, fresh client, same code) | **a20c352** |
| [final_build](runs/polish4/final_build.txt): `gradlew clean build`, tests and `checkNoOverwatchAssets` pass | **a20c352** |

a20c352 is the last code commit; only notes follow it. "Off" is the same build with `bot fix 119 off` / `bot fix rally off`. Screenshots: `run/screenshots/p4_*`, gitignored (they show Overwatch models). Scripts: this session's scratchpad, `p4_*`.

## #119: Protection Suzu
**The data does hold it**, against what #117's comment said: [status_and_cleanse](../game-data/status_and_cleanse.md). W agrees everywhere it speaks; the exceptions are labelled in `Combat.cleanse`'s table, which is the list.

**Reproduced first (base):** dart on a bot ally and on the real client: asleep through a real and a bot Suzu to the sleep's end (`player@1` until +4.5 s); EMP on the real client: glowing and keys locked after a bot's Suzu (Ana's Shift: cooldown 0 -> 0). The base's stun line is void: its Suzu was never thrown, the real client stood in the shockwave.

**Off and on, tree a20c352.** Bot ally = the real key thrown at a bot (with one marked exception); real client = a bot Kiriko's Suzu. Rows read from p119_table unless said.

| State | Bot ally, off | Bot ally, on | Real client, off | Real client, on |
|---|---|---|---|---|
| Sleep (asleep, 1 s after) | stays | **awake by +0.48 s, look gone** | stays | **awake by +0.65 s, look gone** |
| Rally's stun (ticks read, D 15) | 15, phase at tick 10 in it ([stun file](runs/polish4/p119_stun_looks.txt), 2 of 2) | **1 read, then free at the next read (tick 10), 2 of 2** | 15, phase at tick 10 | **1 read, then free at tick 10** |
| Hack (silence, glowing, looks) | stay | **all gone by +0.3 s** | stay, Shift 0 -> 0 | **gone by +0.1 s, Shift 0 -> 14.65** |
| Anti-heal, healing cut | gone (#117 is not under this switch) | gone, heal +80 | gone | gone |
| Local Anesthetic (speed, base 0.1; damage) | 0.071, health falling to 16.8 | **0.1, stops, health 18** | the same | **0.1, stops, 18** |
| Claws Out (speed, damage) | 0.07 to its end, 17.36 | **0.1 at once, 18.0** | 0.07, 17.36 | **0.1, 18.0** |
| Hyper Strike's float (gravity) | 0.001 to its end | **0.08 at the first read** | 0.001 to its end | **0.08 by +0.3 s** |
| Deceleration Field (speed) | 0.075 | **0.1 in the phase, 0.075 again inside** | 0.075 | **the same** |
| Weaken (damage dealt factor) | 0.6 | **1.0 in the phase, 0.6 again inside** | 0.6 | **the same** |
| Accretion's knock-down | 1.12 s | **1.08 s: stays** | 1.10 s | **1.12 s: stays** |
| Gravitic Flux (lifted) | lifted | **stays lifted** | lifted | **stays lifted** |

- **Catnapper's knock-down, on only** (real client, [p4_extra](runs/polish4/p4_extra.txt)): 30 ticks without a Suzu, 30 with, 2 of 2 each, the phase inside it.
- **Fix-off anomaly:** "Sleep on allyB, off", the first case of the file: health 15.6 and the look's entry gone while still reading asleep. Something hurt it (a new world's slime, not checked), so it was waking. Doesn't touch the fix-on rows.
- **Prevented, on the real client, fix off / on, by tick:** dart in the phase: asleep / no. EMP pulse in the phase (phase ticks read beside it, in file): silenced from tick 4 of the phase / never. Accretion in the phase: knocked down from tick 7 / never. Rally's stun, by accident of timing, on a bot ally: phase at 929, stun 15 ticks / phase at 6235, stun 0.
- **Late viewers** (a real client 500 m away for it, back after, a bot as the target): off: the client is told `revealed` / `asleep` on return; on: nothing, server list empty.
- **Looks, opened at 1280x720:** `p4_look_hack_off_b`: Lúcio still in the hacked box, skull over the name, white outline, 0.75 s after a Suzu. `p4_look_hack_on_a`: the same; `_on_b`: plain. `p4_look_sleep_on_a`: the red sleep look; `_on_b`: plain. Those on_b frames also show the bot Kiriko plain: she was caught by the EMP and by her own Suzu.

**Decisions, per effect:** in the table. **Ours or inferred:** M that v8440 means phased; M 0E34 as Rally's type (its default wasn't traced; 6BBE is ruled out by Accretion setting it and leaving Rally unset, and W's stuns are removed); M the mask 20 reading; Weaken's and the field's "out of the filter" (M); that a knock-down is prevented at our one door rather than at each hit's filter; Hyper Strike, Claws Out, Local Anesthetic rest on W with no graph read. Glowing from another source goes too while Hack's reveal runs (ours). Sound: anesthetic's dot and the bed stop; Hack's own sound isn't stopped (not checked if it's long).

## #120: Rally's double hit
The data: `kit_shield_bash.md` had it right, and I read the graph: 0EE3 #877 -> #890 STU_DA54F8F7 {v1184, v375} -> #886, #887 on v1184. H nodes. M that DA54F8F7 removes: same family as add and clear on the field ([status_and_cleanse](../game-data/status_and_cleanse.md#list-ops-for-reading-any-graph)). W says nothing that disagrees. It still fills one of the tracker's 10 (D's order).

| Rally, bash + shockwave, bot Brigitte | Base | a20c352, fix off | a20c352, fix on |
|---|---|---|---|
| Real client, hero: damage, peak speed, moved | 140, 30.4, 19.7 m | 140, 30.4, 19.7 m | **70, 17.2, 7.9 m** |
| Real client, Minecraft mode | 140, 26.1, 11.8 m | 140, 26.1, 11.8 m | **70, 18.0, 4.1 m** |
| NoAI husk | 140 | 140 | **70** |

By her own keys (Q, right button, left button; [rally_keys](runs/polish4/rally_keys_a20c352.txt)), real zombies in iron helmets, 2.2 m and 5.5 m ahead and 3.5 m behind, 3 runs each: off a 8.8, b 14.4, c 20; on **a 14.4, b 14.4, c 20**, both stunned right after. [p4_play](runs/polish4/p4_play.txt)'s first case read off a 14.4 as well: a first Rally of the session with the switch off; not explained, dev switch only, on is unaffected.

## Whip Shot's 8 m
**The brief's "no source" was wrong: W has it.** Brigitte rev 408812: "~8 meter knockback distance on flat ground" (and `tools/reference/brigitte.md` line 85 cites it). The data has a speed, no distance. Comment now says: D 133E 25 m/s, one push; the 0.25 s hold is ours, aimed at W's 8 m, and reaches 5.88 m on a hero and about 5.6 m on a husk. **Behaviour unchanged; whether to hold it longer is the owner's.** Also written down: W's V:3 against the mod's 2 m/s up: D's push vector has 2 (#638's `m_1376B4DF`); its `m_583C208B` = 3 is unread, L.

## Played like a player (as far as a script goes)
[p4_play](runs/polish4/p4_play.txt), survival, normal, midnight:
- **Night, real Kiriko:** a tamed sitting wolf slept by a bot Ana (Zzz in `p4_play_night_burst`, the HUD's Suzu at 14), Suzu by the key: awake by +0.5 s, wolf 7.6 -> 8.0 (its full), villager 10 -> 16.4. The villager was never slept: **Ana's dart passes a villager** (Allies: everyone's ally), so a slept villager doesn't arise. Then 14 s of kunai at two walking zombies: both dead, Kiriko 20, in the file.
- **Real Sombra asleep, two zombies coming, bot Kiriko:** awake at +0.6 s; bitten 18 -> 15 -> 12 after; Cyberspace by the key at once (`p4_play_sombra_awake`).
- **Minecraft mode (G off):** asleep, Suzu, awake at +0.6 s.
- **Life Grip (bot Lifeweaver):** asleep, grip, phased at once, awake at +0.6 s. Corrects the note that it had no cleanse node.
- **Scripted fights** ([p4_fights](runs/polish4/p4_fights.txt), p3_fight2 unchanged, midnight, 3 zombies, 2 skeletons, a creeper, 30 s): Kiriko, Brigitte, Ana, two rounds each: mobs left 3, 2 / 3, 1 / 3, 3, deaths 0, 1 / 0, 1 / 0, 2; each came back the same hero. **Sombra's rounds cut off by my own time limit.** No baseline on this tree, so a smoke test, nothing more. Shots `p4_p4_fight_*` not all opened.
- No errors in the log beyond the machine's start-up ones.

## What a critic should check
- The stun's "1 read" is coarse: reading while pressing leaves a 10-tick gap in those rows, so the moment it ends is known only to within that gap. Off holds its 15 through the phase in the same script.
- **Asleep in water: not shown.** Two tries: the dart didn't reach under water; a Suzu burst with nothing to remove.
- A bot's `press` skips the silence check: on Hack's "real client" row the throwing bot had been EMPed too. The bot-ally row's real Kiriko stood 17.6 m from Sombra, out of it.
- Bots don't move: the pushed victims here are real clients only. No second client saw any look end; the seeing end is one client drawing a bot.
- `p4_look_*` are noon, third person of a bot. Not looked at: Hyper Strike's and Claws' looks after the cleanse; Weaken's look (variant 0.75 s) runs out on its own, not ended.
- Hack on a mob ally (creeper's fuse), Sleep on a boss: read, not played.
- Kitsune Rush, Valkyrie and other running abilities of a slept hero aren't touched by waking: unchanged.
- Anti-heal's D reading rests on `work_voice/decomp_0C72`; `dart_grenade/` copy same id, not diffed.
- #121 (her line on every throw) is filed, not fixed: the owner should hear it.
- Nothing of this has been compared with Overwatch. W's "Stuns" and BZ's knock-down rule are the only checks from outside the data.

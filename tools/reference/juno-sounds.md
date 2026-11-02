# Juno's sounds and voice lines, from Overwatch's game data (build 154088)

Read 2026-11-01 offline and read-only through TankLib / DataTool 2.25.0.0 (`Online=false`) on the archived build
`E:\Overwatch Archive\build-154088`, with `Get-Process Overwatch, Battle.net` checked empty before every TankLib / DataTool run
(the scripts' `owinit.py` refuses on its own too). Nothing was decrypted beyond DataTool's own reading, and nothing went into
the repo but text. Nothing here has been listened to by a person or played in game yet. Confidence: **H** read in the data
(a node, an edge, a field, a file), **M** inferred (one link guessed, or a reading taken from another hero's map), **L** guess.

- **The sound map** (what the pack builder reads): `tools/soundmaps/juno.txt`. It has every rule's layers, gains, delays and
  sources, the "Not mapped" list, the voice section and the Voice.java stimulus lines. It is written by
  `E:\Overwatch Archive\research\juno\scripts\sounds\juno_gen.py` (Brigitte's `brigitte_gen.py`, retargeted).
- **The kit it hangs on:** [juno-map.md](juno-map.md) (graphs, nodes, numbers). Method: [sounds](../../techniques/sound/sounds.md#juno-sounds).
- **Research folder:** `E:\Overwatch Archive\research\juno\`. It holds:
  - `scripts\sounds\`: Brigitte's sound scripts, copied and retargeted, plus the new `juno_gen.py` and `clip_sounds_juno.py`.
  - `sounds\`: the decode `juno_ww.json`, `banks_raw.json`, `juno_measure.json`, `rtpc_defaults.json`, `contacts.json`, the
    projectile and child-entity effects `fx_proj.json`, `voice_lines.json`, `overview.txt` (one block per 02C: range,
    audience, layers) and `gen_print.txt` (every rule's layers and what was dropped).
  - `work\`: `graph_effects.json`, `fx_sounds_timed.json` / `_out.txt`, `fx_nodes_out.txt` (the node that plays each
    effect), `clip_sounds.txt` (the clip animation effects that hold a sound) and the voice set `voiceset_0278.05F.json`
    with `stim078.json` / `stim079.json`.
- **Extracted audio:** her skin and voice extraction are in `research\juno\extract\` (from the data pass). The 28 banks it
  lacked were extracted for this pass into `research\juno\sounds\ExtraSounds\Juno\` (`extract_banks.py`, DataTool's own
  sound export; `manifest.json` there). They are grouped as `MediblasterContact` (10), `TorpedoContact` (15) and
  `Projectiles` (3: B497, BAFF, and B39F, which has no files).
- **In the pack:** 105 `juno.*` events with 512 .ogg files (9.0 MB), in
  `E:\claude\mods\overcraft_ana\local-assets\pack\assets\overcraft\sounds\juno\` (gitignored). What was added there is listed
  at the end.

## How to read the tables
- **Who:** *own* means the player the effect plays on, heard in their head: Juno, or the ally the row names. *3p* means
  everyone else, at her, at the target or at the hit point. *both* means one rule for everyone.
  - Audience-1 STUSounds with no range are hers (H, by Lúcio's pairs).
  - Audience-2 copies with no range are read as the player the effect runs on (M, as in Ana's, Sombra's and Brigitte's maps).
  - Where there are two ranged copies with the same files, the louder is built and the other is noted (L which team's copy).
- **1P / 3P:** where the sound comes from a clip, the row says which model's clip. Effect sounds are on the hero entity,
  which is the same for both views.
- **Length:** the built file's length. For a loop, the row gives its **LOOP LENGTH**: start the loop again every LOOP LENGTH
  and stop it when the state ends.
- **Ranges** come through the volume (metres / 16, as `HeroSounds.split` does). The range is given where the copy has one.

## 1. Mediblaster (weapon graph 4445; projectile 538E; contact set 07DC)
Each volley is 12 projectiles at 34 per second (0.353 s), one volley every 0.80 s. 4445 #913 StateWeaponVolley holds #1114
-> #1211 **effect 5C2D** for the length of the volley. Its 02C are **self-repeating streams**: trigger-rate random containers
paced by the shared fire-rate parameter 4066808683. That gives 0.06 s per repeat at its default of 100 (H). It is M that her
game leaves the parameter at its default. The stream's time modulators are read at their start, because a volley is short.

| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `juno.mediblaster.volley` | 02C B3DC (aud 1, no range) of 5C2D: first-shot sets once, stream sets repeated across 0.353 s | each volley's start | own | 0.98 s | H data, M period |
| `juno.mediblaster.volley_end` | B3DC: the 2.08-2.72 s ring-out set once, plus its Stop-event set | the volley's end (0.353 s) | own | 1.71 s | H |
| `juno.mediblaster.volley_3p` | B92C (150 m; B3DF is the same containers 2-12 dB quieter, L) | each volley | 3p | 0.50 s | H / L copy |
| `juno.mediblaster.volley_end_3p` | B92C ring-out set (no Stop sound) | volley end | 3p | 1.64 s | H |
| `juno.mediblaster.projectile` | 538E's `m_2170A015` "sound only" slot, effect 5C5F, 02C B497 (60 m); built as 12 shots 1/34 s apart | the volley's start (each projectile as it is made) | both | 0.62 s | H slot, M role |
| `juno.mediblaster.hit` | 538E's `m_A18274A3` "on a target" slot 51DA -> sub-effect 4010 -> 8DE7 (the shared crack) | any character hit, ally or enemy | own (the shooter's confirmation) | 0.48 s | H / M (Ana's reading) |
| `juno.mediblaster.hit_enemy` | 07DC type 0, material 0001 -> 60EC -> B38E (no range) | an enemy hit | own | 0.28 s | H data, M type 0 = characters |
| `juno.mediblaster.hit_enemy_3p` | 9058 (500 m; 9054 is 12 m, the same files 7 dB quieter) | the same, at the hit point | 3p | 0.46 s | H / L copy |
| `juno.mediblaster.hit_block` | 07DC type 2, 0001 -> 652D -> 899F (90 m) + 89A1 (32 m), both play | a hit on the world | both | 0.30 s | H |
| `juno.mediblaster.reload` | **1P clip E9B6** effect 5FA4: B734 at 0, B735 / B74E at 0.167, B739 at 0.533, B736 at 0.9, B737 at 1.133, B738 at 1.3 s | RELOAD, or the magazine empty (#888 / #1266; 1.5 s) | own (1P) | 2.0 s | H |
| `juno.mediblaster.reload_3p` | **3P clip F6C3** effect 60BA: B73A, B755, B73C, B754, B73B, B73D, B73E, B73F at the clip's times (45 m) | the same | 3p | 2.1 s | H |

- **Empty / dry fire: none in the data** (H). 4445 plays only 5C2D and two UI data-flow effects (2B22 / 2B44) that have no
  Sound. An empty magazine starts the same reload.
- **Not built:**
  - the stream's 3.19 s bed (B3DC 607594979 / B92C 249843728). It is a Sound loop held only for the 0.353 s volley, and one
    file cannot be cut to that length (L).
  - the other contact materials (008D / 008E / 00EC and type 1).
  - the barrier hit 1833, which is the shared barrier impact and is already built as `brigitte.hit_barrier`.
- **Faster Blaster** (gliding: continuous fire at 22/s) would want the stream played and stopped with the trigger, not per
  volley. That would be a loop of the `volley` file's stream (not built).

## 2. Pulsar Torpedoes (4695 on her; 4B99 per target; 47DD per torpedo; 4795 on a healed ally; contact set 07E9)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `juno.torpedo.lock_start` | effect 520F, 02C B16D: its 3.71-3.92 s one-shot set | SECONDARY: the lock state starts (#175 -> #1111 !(v173) -> #819 holds 520F while locking, up to 4 s) | own | 3.92 s | H |
| `juno.torpedo.lock.loop` | B16D's 4.73 s loop at +700 cents | while locking | own | LOOP 3.158 s | H |
| `juno.torpedo.lock_start_3p` / `lock.loop_3p` | B91C (70 m; B17D is 2 dB quieter, L) | the same | 3p | 3.92 s / LOOP 3.158 s | H / L copy |
| `juno.torpedo.lock_end_3p` | B91C's Stop event (0.82 s); her own B16D has no Stop sound | the lock state ends | 3p | 0.82 s | H |
| `juno.torpedo.locking.loop` | 4B99 #226 -> #220 effect 5C17, 02C B3B7 (65 m, a 0.02 s trigger stream); built as 1.0 s of it | while each target's lock fills (0.35-1.0 s); played for Juno (v40) at the target | own (M) | LOOP 1.0 s (stop it at the lock) | H nodes, M who |
| `juno.torpedo.lock_target` | 56DC, 02C B16E | each target locked (4B99 message 227D -> 4695 #803 -> #805 -> #1123 -> #1121) | own (no copy for others) | 0.27 s | H |
| `juno.torpedo.fire` | 520D, 02C B257 | the fire state starts (#938 onBegin -> #1132 false) | own | 0.93 s | H nodes, **L** which branch is the usual one |
| `juno.torpedo.fire_3p` | B91B (70 m; B2F0 1 dB quieter) | the same | 3p | 0.93 s | H / L copy |
| `juno.torpedo.fire_alt` | 5B68, 02C B24B | #1132 true (v24080 > 0) | own | 0.78 s | **L** |
| `juno.torpedo.launch` | 5199, 02C B16F | each torpedo leaves the pod (volley #815 ally / #778 enemy onBegin; one per target every 0.08 s) | own | 2.58 s | H |
| `juno.torpedo.launch_3p` | B17E (70 m) | the same | 3p | 2.58 s | H |
| `juno.torpedo.flight.loop` | ally torpedo 534F -> child 5350 effect 51E8: B400 + B3FE + B401 (30 m each, all play) | while an ally torpedo flies (24 m/s, at most 5 s) | both, at the torpedo | LOOP 6.301 s | H |
| `juno.torpedo.flight_enemy.loop` | enemy torpedo 54CB -> child 5A5B effect 5940: B3FC + B3FD + B3FF | while an enemy-bound torpedo flies | both | LOOP 6.576 s | H |
| `juno.torpedo.hit` | 07E9 type 0, 0001 -> 62C7 -> B3C5 (no range) | a torpedo hits an enemy (85) | own | 0.92 s | H data, M |
| `juno.torpedo.hit_3p` | B3E2 (500 m; B231 is 12 m, the same files quieter) | the same, at the hit point | 3p | 0.99 s | H / L copy |
| `juno.torpedo.hit_block` | 07E9 type 2, 0001 -> 62CC -> 8E90 (90 m; 8E92 is 32 m, quieter) | a torpedo hits the world | both | 1.08 s | H |
| `juno.torpedo.heal` | 4795 #376 (the 2.5 s heal) -> #395 false -> #434 effect 56DD, 02C B22E (no range) | an ally torpedo lands (85 + 50 over 2.5 s), on every client but Juno's | own (the healed ally) | 1.41 s | H nodes, M #395 = "the local player is Juno" |
| `juno.torpedo.heal_3p` | 56DD's B232 (12 m, other files) | the same, near the ally | 3p | 1.81 s | H |
| `juno.torpedo.heal_confirm` | #395 true -> #394 effect 5A90, 02C B170 (500 m) | the same, on Juno's own client | own (Juno) | 1.90 s | H nodes, M reading |

- **Cancel:** no sound of its own (H). The lock phase's other path (#943 false -> #1131 -> #1139 -> the second node #1132, an
  anim-category test) picks the same 520D / 5B68 pair, so a cancelled lock ends on `fire` or `fire_alt` (L).
- **Missing: the "torpedo incoming" warning.** 47DD #392 (the local player is the torpedo's target v28592) holds #390 effect
  5C04, whose 02C is B39F (30 m). B39F's Play target 235791481 is in none of the build's 1148 banks (`find_object.py`: hits
  0), so the sound does not exist. 
- **Not built:**
  - 4795's 5F46 (BB5C: a barrier-like set plus B170's set, when the ally carries tag 003A: L a torpedo stopped short).
  - the other contact materials (008D / 008E / 00EC / 0043 / 00E3).

## 3. Hyper Ring (4857 on her; 4858 on the ring entity 5530)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `juno.ring.cast` | 52FE's AF2A (aud 1) + 5D47's B8CE (aud 1, 0.25 s late) + **1P clip E9BA** 6297's B503 | ABILITY 2 pressed (#807's activation plug m_4E22B5C4 -> #1112 / #1205) | own (1P) | 2.24 s | H |
| `juno.ring.cast_3p` | B2E6 (70 m; B916 is the same at the same gain, L) + **3P clips F01A / F09E** 628A / 629A's B4FF (30 m) | the same | 3p | 1.24 s | H |
| `juno.ring.launch` | 5530's `m_2170A015` slot, effect 5F2F, 02C BAFF (60 m) | the ring is made, 0.25 s into the 0.55 s cast (#1100 -> #802) | both | 0.53 s | H slot, M role |
| `juno.ring.appear` | 4858 #618 (both branches) -> 541C, 02C AD67 (70 m; B919 6 dB quieter) | as the ring forms (541C is its model and particles) | both | 2.38 s | H nodes, M moment |
| `juno.ring.loop` | #618 -> #542 effect 5A5C, 02C B16C (70 m: three 10-11 s beds; B918 9 dB quieter) | while the ring lasts (6 s) | both, at the ring | LOOP 10.344 s | H |
| `juno.ring.pass` | #437 / #438 per ally -> ... -> #554 false -> #432 effect 5A8B, 02C B194 (no range) | an ally passes through (+50 % speed for 3 s) | own (the boosted ally) | 3.98 s | H nodes, M on whom |
| `juno.ring.pass_3p` | B193 (45 m; B915 2 dB quieter) | the same, at the ally | 3p | 3.98 s | H / L copy |
| `juno.ring.end` | #463 (6 s chase) -> #544 op71(v215, 0.15) -> #508 effect 53CA, 02C B192 (70 m; B917 13 dB quieter) | 0.15 s before the ring's 6 s run out | both | 1.99 s | H |

- **Flight:** no separate flight loop exists (5530's `m_A18274A3` 5AE7 has no Sound); the hum `ring.loop` covers it (H).
- **Not built (Stadium, L):** 5D54 (B8E1, the v55341 branch of #554) and 7AD4 / 7AD5 (FF69-FF6D, on v41.v76384 / v78580
  with JUMP).

## 4. Orbital Ray (4A44 on her; 501A on the ray 5CCA; 54A6 the +30 % damage amp on allies inside)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `juno.ult.cast` | 54F8's B1A5 (4.19 s file + a 1.17 s file 1.55 s late) + **1P clip EA1C** 602F's B4FC | ULTIMATE: #414 -> #429 -> #430 (0.5 s) holds #433 54F8; the ray lands at 0.5 s | own (1P) | 4.19 s | H |
| `juno.ult.cast_3p` | B1A6 (75 m, B1A5's 4.19 s file) + B920 (75 m, its own 4.27 s file), built together | the same | 3p | 4.27 s | H data, M together / L team copies |
| `juno.ult.ray.hum` | 501A #33 holds #32 effect 54F7, 02C B198 (75 m: five endless beds with loop lengths of 7.4-22 s), built as one long one-shot | from the landing; **stop it when the ray ends (8 s)** | both, at the ray | 22.4 s file | H; B91F (75 m, the same beds an octave lower) is L the other team's copy |
| `juno.ult.ray.ending` | #22 op71(v29029, 2.7) -> #23 effect 5AD2, 02C B1D4 (75 m; B91E 5 dB quieter) | 2.7 s of the ray left | both | 4.78 s | H |
| `juno.ult.ray.inside.loop` | #398 -> #397 per tracked ally -> #396 effect 7AFA, 02C 7532 (no range) | an ally inside the ray | own (the ally) | LOOP 9.885 s | H nodes, M |
| `juno.ult.amp.start` | 54A6 #298 onBegin -> #301 5D45's B8CD + #297 3169's 12B2 one-shots | the damage amp starts on an ally inside | own (the ally) | 1.81 s | H |
| `juno.ult.amp.loop` | #296 5E86's B9A2 beds + 3169's 12B2 beds | while amplified | own (the ally) | LOOP 7.923 s | H |
| `juno.ult.amp.end` | 12B2's Stop event (0.68 s) | the amp ends | own (the ally) | 0.68 s | H |

- **Landing:** no separate landing sound (H). The ray's start effect 5527, the ground ring 54F9 and 7E01 hold no Sound;
  54F8's 4.2 s cast sound carries the call-down.
- **Ult lines:** all three are raised together at the press (#853 STU_37D754C8 -> graph 545C):
  - **`juno.ult.friendly`** (16A9, key F1AB, allies and herself: "Welcome to orbit!")
  - **`juno.ult.enemy`** (16AA, key F1AA, enemies: "Locking satellite vector!")
  - **`juno.voice.ultimate`** (184D, key F1AC, her own: "Welcome to orbit!")

  With `HeroSounds.ultLine` playing 16A9 / 16AA, 184D is her own copy of 16A9's text: pick one. Their skin / celebration
  lines are left out (H).
- **Not built (Stadium):** 2E8D (5454, behind power 0DEF.134) and the 5540 copy.

## 5. Martian Overboots (4441) and Glide Boost (47B7)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `juno.boots.double_jump` | #570 the jump action -> #274 effect 53EB, 02C AD51 | JUMP in the air (0.1 s after leaving the ground; once, twice with Lift Off) | own | 1.19 s | H |
| `juno.boots.double_jump_3p` | AD52 (60 m) | the same | 3p | 1.19 s | H |
| `juno.boots.hover_start` | #382 (the 3 m/s cap) -> #380 false -> #378 5C0F, 02C B4F6 | the hover starts (JUMP held, 2.5 m of air below) | own | 1.32 s | H, M #380 |
| `juno.boots.hover_start_3p` | B921 (60 m; B4F8 10 dB quieter) | the same | 3p | 1.32 s | H / L copy |
| `juno.boots.hover.loop` | #380 false -> #586 5D29, 02C B4F5 (three 10.32 s beds) | while hovering | own | LOOP 10.321 s | H |
| `juno.boots.hover.loop_3p` | B923 (60 m; B4F7 quieter) | the same | 3p | LOOP 10.321 s | H / L copy |
| `juno.boots.hover_end` | #380 onEnd -> #383 5C10, 02C B4FA | the hover ends | own | 0.96 s | H |
| `juno.boots.hover_end_3p` | B922 (60 m; B4F9 10 dB quieter) | the same | 3p | 0.96 s | H / L copy |
| `juno.boots.idle.loop_3p` (EXTRA) | #344 true -> #345 59EC, 02C B106 (20 m); her own B105 is 61 dB down | neither hovering nor gliding | 3p | LOOP 7.317 s | H nodes, M condition |
| `juno.glide.start` | #805 -> #924 (no Stadium 0F28) false -> #527 effect 59F5 held for the glide, 02C B10F (aud 1): its one-shot sets | ABILITY 1 (up to 4 s) | own | 2.51 s | H, M #805 spans the glide |
| `juno.glide.loop` | B10F's 5.54 s loop at +600 cents | while gliding | own | LOOP 3.917 s | H |
| `juno.glide.end` | B10F's Stop event | the glide ends (4 s, or ABILITY 1 again after 0.4 s) | own | 0.91 s | H |
| `juno.glide.ending` | #642 -> #544 op71(v215, 0.4) -> #545 effect 5843, 02C B500 | 0.4 s of the glide left | own | 1.51 s | H |
| `juno.glide.start_3p` / `loop_3p` / `end_3p` | B924 (60 m; B110 is the same containers 1-4 dB quieter, L) | the same moments | 3p | 2.51 s / LOOP 3.917 s / 0.91 s | H / L copy |
| `juno.glide.ending_3p` | B924's running-out set, 3.0 s into its Play event (built without the delay) | **3.0 s into the glide** (the data's delay) | 3p | 1.51 s | H |
| `juno.jump` / `juno.jump_3p` / `juno.land` (EXTRA) | 1P jump E9A8 628D B8D6 at 0.1 s; 3P jumps (ECDE, F332..F36A) B755 at 0.067 s; 1P land E9AA 628E B74E | a plain jump / landing | own (1P) / 3p (3P) / own | 0.44 / 0.35 / 0.30 s | H |
| `juno.melee` (EXTRA) | **1P clip E9B8** 5F7F: B2CF (aud 1) + B8BF | quick melee (the 3P melee clips carry no sound) | own (1P) | 0.69 s | H |

- **Glide loop, double jump while gliding, the glide's landing:** her glide and hover clips carry no sound (H), and the
  double jump in a glide plays the same 53EB.
- **Not built:**
  - the boots' 5985 / 5D10 / 5A40 jets (no Sound).
  - 5D31 (57-67 dB down) and 5D60 (123 dB down).
  - 5D4F (B8D0: a 1.61 s one-shot inside the Overboots' UX presenter: L a HUD cue).
  - the Stadium charge effects 7523-7528 / 752C.

## 6. Perks (522E; EXTRA)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `juno.perk.locked_on` | #360 onFinished -> #364 effect 68A8, 02C C852 | Locked On (25E4): the torpedo cooldown refund after a fire with enemy hits | own | 0.31 s | H |
| `juno.perk.ring_refund` | #431 (message 27F1) ... #435 -> #451 effect 67AC, 02C C79C | each new ally through her ring (the -1 s refund) | own | 1.13 s | H nodes, L live |

## 7. Her voice (voice set 0278.05F; 269 stimuli, 828 lines)
These moments were chosen the way Brigitte's were: the shared moments the mod already uses, plus her own ability callouts.
Lines with criteria (hero interactions, skins, maps, scripted conditions) are left out and counted in the map. Events are
`juno.voice.<moment>`. The table gives stimulus numbers as priority / radius / cooldown, and the number of files built.

**Shared moments** (stimuli global; Voice.java's shared table has their numbers):

| Moment | Stimulus | Prio / radius / cooldown | Files |
|---|---|---|---|
| elimination | 0008 | 70 / 30 m / 15 s | 10 |
| multikill | 00C9 | 70 / 35 m / 15-20 s | 5 |
| melee_kill | 0032 | 65 / 30 m / 10-14 s (Brigitte has none; Juno does) | 4 |
| on_fire | 0583 | 60 / 2-5 s | 3 |
| respawn | 002C | 40 / 15 s | 10 |
| melee | 0023 | 35 / 0.5 s | 9 |
| select | 002B | 100 / 1 s ("One small step at a time!") | 1 |
| death | 0001 | 90 / 60 m / 2-5 s | 6 |
| pain | 0003 + 0004 | 40 / 0.5-2.5 s | 19 |
| resurrected | 005A | 75 / 10 s | 2 |
| ult_ready | 02E7 | 70, interrupts (wheel line) | 2 |
| call_ult_charging | 0574 | 70, interrupts | 3 |
| call_ult_almost | 05A7 | 70, interrupts | 2 |
| call_hello | 01CF | 70, interrupts | 4 |
| call_thanks | 01D0 | 70, interrupts | 4 |
| call_sorry | 05E1 | 70, interrupts | 3 |
| call_no | 01D2 | 70, interrupts | 3 |
| call_group_up | 02C6 | 70, interrupts | 2 |
| call_need_help | 0427 | 70, interrupts | 3 |
| call_need_healing | 0045 | 70, interrupts | 2 |
| ping | 11D9 | 70, interrupts | 1 |
| ping_warning | 11DA | 70 / 75 m / 3 s | 3 |
| ping_enemy | 1117 | 70 / 75 m / 3 s | 3 |
| damage_boosted | 00CC | 60 / 30 s | 3 |
| nano_boosted | 05E2 | 95 / 80 m / 2 s, interrupts | 3 |
| call_good_job (ours) | Teammate/KillSeen lines with no criteria | - | 2 |

**Her own moments.** The map's integration section has the `Map.entry` lines for Voice.java.

| Event | Stimulus | When (graph node) | Prio / cooldown | Files | Conf. |
|---|---|---|---|---|---|
| `juno.voice.reload` | 176F ("Reloading!") | 4445 #1334 / #1336 -> #1331 / #1332: the reload (the shared 0E2C is not in her set) | 60 / 0 s, 60 m | 6 | H |
| `juno.voice.heal` | 1727 ("Let me help!") | 4445 #1216 -> #1217 -> #1213 | 60 / 20 s | 9 | H node, M: healing an ally with the Mediblaster |
| `juno.voice.torpedo_ally` | 16AF ("Support inbound!") | 4695 #1183 -> #1239 op73(v9293, v58608) true -> #1165 | 60 / 20 s | 8 | H node, M: more allies than enemies locked |
| `juno.voice.torpedo` | 1761 ("Pulsars inbound!") | #1239 false -> #1227 | 60 / 20 s | 7 | H node, M |
| `juno.voice.ring` | 16B0 ("Hyper ring deployed!") | 4857 #1097 (the 0.55 s cast) onBegin -> #1203 | 60 / 15 s, +2 dB | 9 | H |
| `juno.voice.ping_ring` | 16DF ("Hyper Ring here!") | no graph of hers raises it | 70, interrupts | 2 | M: a ping on her ring |
| `juno.voice.ultimate` | 184D ("Welcome to orbit!") | 4A44 #853, key F1AC | 95 / 1 s, 80 m, +8 dB | 1 | H |
| `juno.ult.friendly` | 16A9 ("Welcome to orbit!") | #853, key F1AB | 95 / 1 s, 60 m, +6 dB | 1 | H |
| `juno.ult.enemy` | 16AA ("Locking satellite vector!") | #853, key F1AA | 95 / 1 s, 80 m, +8 dB | 1 | H |

- **Glide Boost, the Overboots and Orbital Ray's landing raise no voice line** (H: no VoiceStimulus node in 4441 / 47B7 /
  501A / 54A6, none in her effects).
- Not mapped:
  - 1893 / 1894 ("Wonderful!", "How extraordinary!": raised by no graph of hers, L).
  - the PVE / Retribution / objective / emote / Voicelines stimuli.
  - HealthRestoredFriendly, EnemyDetected, Discorded and UnderAttack (said by others or about others).

## Missing, and what was tried
- **The torpedo "incoming" warning** (47DD 5C04 -> B39F): its Play target is in none of the 1148 banks
  (`find_object.py 235791481`). It does not exist in the build.
- **A Mediblaster dry-fire / empty sound, a Hyper Ring flight loop, an Orbital Ray landing sound, glide-, hover- or
  double-jump clip sounds:** none in the data. Checked against every effect of 4441 / 4445 / 47B7 / 4695 / 4857 / 4858 /
  4A44 / 501A, her projectiles' visual slots and child entities (`fx_proj.json`) and every clip animation effect
  (`work\clip_sounds.txt`).
- **The Mediblaster stream's pacing:** the fire-rate parameter's value in her game is not in the data. The volley is built
  at its default (0.06 s per repeat, M).
- **Which of two ranged copies is which team's** (L everywhere it is noted). Two cross-checks are still needed: a second
  client on another team, and the Workshop or a recording.
- **Nothing has been listened to or played in game.** Balance, loudness against the other heroes, and every M / L reading
  above still need the in-game pass.

## What was added to the local pack (`E:\claude\mods\overcraft_ana\local-assets\pack\`)
- **Built** with `build_asset_pack.py sounds` (run with `py -I`) from a scratch root holding only `tools/soundmaps/juno.txt`,
  into a scratch pack. Only the Juno part was copied over. This was done so the sounds step, which deletes and rebuilds
  the whole `sounds\` folder from the worktree's maps, would not rewrite the other heroes' entries.
- `assets\overcraft\sounds\juno\**`: **512 .ogg files** for 105 events (9.0 MB). Each event's folder holds `0.ogg` ... `n.ogg`,
  up to 8 variants for a mix and every line for a voice pool.
- `assets\overcraft\sounds.json`: **105 `juno.*` entries** were added (`"replace": true`, `"subtitle":
  "subtitles.overcraft.juno.*"`). Nothing else was changed (1231 -> 1336 entries).
- `assets\overcraft\lang\en_us.json`: **105 `subtitles.overcraft.juno.*` entries** were added (1216 -> 1321).
- The files as they were before are backed up in the session scratchpad (`sounds.json.bak`, `en_us.json.bak`).
- To rebuild it all the normal way later: `OC_ASSET_PACK=<pack> py -I tools/build_asset_pack.py sounds "E:\Overwatch
  Archive\extracted\154088"` from a worktree whose maps are current (it now finds `juno.txt`).

## The event list by ability (wiring checklist)
- **Mediblaster:**
  - `juno.mediblaster.volley`, `volley_end`, `volley_3p`, `volley_end_3p`
  - `projectile`
  - `hit`, `hit_enemy`, `hit_enemy_3p`, `hit_block`
  - `reload`, `reload_3p`
- **Pulsar Torpedoes:**
  - `juno.torpedo.lock_start`, `lock.loop`, `lock_start_3p`, `lock.loop_3p`, `lock_end_3p`
  - `locking.loop`, `lock_target`
  - `fire`, `fire_3p`, `fire_alt`
  - `launch`, `launch_3p`
  - `flight.loop`, `flight_enemy.loop`
  - `hit`, `hit_3p`, `hit_block`
  - `heal`, `heal_3p`, `heal_confirm`
- **Hyper Ring:** `juno.ring.cast`, `cast_3p`, `launch`, `appear`, `loop`, `pass`, `pass_3p`, `end`
- **Orbital Ray:**
  - `juno.ult.cast`, `cast_3p`
  - `ray.hum`, `ray.ending`, `ray.inside.loop`
  - `amp.start`, `amp.loop`, `amp.end`
  - the lines `juno.ult.friendly`, `juno.ult.enemy`, `juno.voice.ultimate`
- **Martian Overboots:**
  - `juno.boots.double_jump`, `double_jump_3p`
  - `hover_start`, `hover_start_3p`
  - `hover.loop`, `hover.loop_3p`
  - `hover_end`, `hover_end_3p`
  - `idle.loop_3p` (EXTRA)
- **Glide Boost:**
  - `juno.glide.start`, `loop`, `end`, `ending`
  - `start_3p`, `loop_3p`, `end_3p`, `ending_3p`
- **Movement / melee (EXTRA):** `juno.jump`, `juno.jump_3p`, `juno.land`, `juno.melee`
- **Perks (EXTRA):** `juno.perk.locked_on`, `juno.perk.ring_refund`
- **Voice:**
  - shared moments: `juno.voice.` + `elimination`, `multikill`, `melee_kill`, `on_fire`, `respawn`, `melee`, `select`,
    `death`, `pain`, `resurrected`, `damage_boosted`, `nano_boosted`
  - wheel and pings: `ult_ready`, `call_ult_charging`, `call_ult_almost`, `call_hello`, `call_thanks`, `call_sorry`,
    `call_no`, `call_group_up`, `call_need_help`, `call_need_healing`, `call_good_job`, `ping`, `ping_warning`,
    `ping_enemy`
  - her own: `reload`, `heal`, `torpedo_ally`, `torpedo`, `ring`, `ping_ring`, `ultimate`
- **Already registered for every hero** (`OcSounds.init()`): the wheel lines, `resurrected` and `damage_boosted`. Do not
  list them again in her own list: a duplicate registry key crashes the game at start ([sounds](../../techniques/sound/sounds.md)).

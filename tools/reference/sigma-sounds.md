# Sigma's sounds and voice lines, from Overwatch's game data (build 154088)

Read 2026-11-02 offline and read-only through TankLib / DataTool 2.25.0.0 (`Online=false`) on the archived build
`E:\Overwatch Archive\build-154088`, with `Get-Process Overwatch, Battle.net` checked empty before every TankLib / DataTool run
(the scripts' `owinit.py` refuses on its own too). Nothing was decrypted beyond DataTool's own reading, and nothing went into
the repo but text. Nothing here has been listened to by a person or played in game yet. Confidence: **H** read in the data
(a node, an edge, a field, a file), **M** inferred (one link guessed, or a reading taken from another hero's map), **L** guess.

- **The sound map** (what the pack builder reads): `tools/soundmaps/sigma.txt` (128 events: 88 rules, 40 voice pools; 768
  files; `check_map.py`: 0 problems, no clash with the other maps). It has every rule's layers, gains, delays and sources, the
  "Not mapped" list, the voice section and the Voice.java stimulus lines. It is written by
  `E:\Overwatch Archive\research\sigma\scripts\sounds\sigma_gen.py` (Sojourn's `sojourn_gen.py`, retargeted) from the event
  table `sigma_table.py` beside it.
- **The kit it hangs on:** [sigma-map.md](sigma-map.md) (graphs, nodes, numbers). Method:
  [sounds](../../techniques/sound/sounds.md#sigma-sounds).
- **Research folder:** `E:\Overwatch Archive\research\sigma\`. It holds:
  - `scripts\sounds\`: Sojourn's sound scripts, copied and retargeted (`fx_graph_sigma.py`, `fx_sounds2_sigma.py`,
    `clip_sounds_sigma.py`, `measure_sigma.py`, `voiceset_sigma.py` (voice set 01E5.05F), `sigma_voice.py`, `sigma_events.py`,
    `sigma_layers.py`, `extract_banks.py` (allowed root `research\sigma\sounds`), the shared `ww.py`, `dump_banks.py`,
    `contacts.py`, `fx_timed.py`, `fx_nodes.py`, `stmg.py`, `overview.py`, `find_object.py`, `check_map.py`), plus
    `sigma_table.py` (the event table), `sigma_gen.py` and a new `node_up.py` (a node of `work_kit\c_<g>.txt` and the chain
    of nodes into it, to read what triggers an effect).
  - `sounds\`: the decode `sigma_ww.json` (260 02Cs, `sids.txt`), `banks_raw.json` (27 banks with the Init bank 1E818),
    `sigma_measure.json`, `rtpc_defaults.json`, `contacts.json` (6 contact sets), the entities' slot effects `fx_proj.json`,
    `voice_lines.json` (287 stimuli, 904 lines), `overview.txt` (one block per 02C: range, audience, layers), `gen_print.txt`
    (every rule's layers and what was dropped), `fx_nodes_out.txt` (the node that plays each effect), `clip_sounds.txt` (the
    clip animation effects that hold a sound), `find_object_925962713.txt`.
  - `work\`: `graph_effects.json`, `fx_sounds_timed.json` / `_out.txt` (365 effects) and the voice set
    `voiceset_01E5.05F.json` with `stim078.json` / `stim079.json`.
- **Extracted audio:** his skin and voice extraction are in `research\sigma\extract\` (from the data pass). The 29 banks it
  lacked were extracted for this pass into `research\sigma\sounds\ExtraSounds\Sigma\` (`extract_banks.py`, DataTool's own
  sound export; `manifest.json` there): `Hyperspheres` (5: the bursts), `HypersphereContact` (7), `Accretion` (5: the rock's
  slots and 5E81, Stadium), `AccretionContact` (5), `FluxContact` (3), `Perks` (3), `Clips` (590A, which lists no file).
- **Not in any pack yet.** The map is written and checked; building it into the local pack is the caller's step.

## How to read the tables
- **Who:** *own* means the player the effect plays on, heard in their head: Sigma, or the player the row names (the shooter
  whose shot he absorbs, a lifted or struck target). *3p* means everyone else, at him, the sphere, the rock, the barrier, the
  target or the hit point. *both* means one rule for everyone.
  - Audience-1 STUSounds with no range are his (H, by Lúcio's pairs). Five of his carry a range (5E0A / 5E09 the bursts, 591A /
    591B the sphere wall hits, 54CB the barrier's standing hum) because they sound at the sphere, the hit point or the barrier
    (Sombra's lesson): play them there, for him.
  - Audience-2 copies with no range are read as the player the effect runs on (M, as in Juno's and Sojourn's maps).
  - Where there are two ranged copies with the same files, the louder is built and the other is noted (L which team's copy).
- **1P / 3P:** where the sound comes from a clip, the row says which model's clip (his 1P arms 106F6, his 3P body 106F7, the
  hand spheres 106B4).
- **Length:** the built file's length. For a loop, the row gives its **LOOP LENGTH**: start it again every LOOP LENGTH and stop
  it when the state ends. No loop of his has a Stop-event tail (H: every Stop event only stops).
- **Ranges** come through the volume (metres / 16, as `HeroSounds.split` does).

## 1. Hyperspheres (weapon graph 187F; spheres 2D35 then 32B4, 0.2 s apart; contact sets 6CB / 6CC (world), 6D3 (characters))
PRIMARY: 187F #98 StateLogicalButton -> #191 -> #453 (the Stadium power 25F9 off) -> **#207 effect 2CE0, once per volley** (H).

| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sigma.sphere.volley` | 2CE0's 5E06 + 64F3 (aud 1, no range) | each volley (one a 1.5 s) | own | 1.14 s | H |
| `sigma.sphere.volley_3p` | 5461 (100 m) + D8EB (38 m) | the same | 3p | 1.11 s | H |
| `sigma.sphere.flight.loop` | child 311A, effect 2BDE: 5562 (10 m) | each sphere in flight (0.43 s) | both, at the sphere | LOOP 4.579 s | H |
| `sigma.sphere.burst` / `burst_3p` | first sphere: 5E0A (aud 1, 200 m) / 5496 (100 m) | the first sphere bursts | own (at the sphere) / 3p | 0.58 / 0.48 s | H data, M / L slot roles |
| `sigma.sphere.burst2` / `burst2_3p` | second sphere: 5E09 (aud 1, 200 m) / 5582 (100 m) | the second sphere bursts | own / 3p | 1.81 / 1.73 s | H / M / L |
| `sigma.sphere.impact_3p` | 59C8 (100 m), only in the m_AED73588 slot | add to `burst_3p` / `burst2_3p` when it bursts on contact | 3p | 0.56 s | H data, M moment |
| `sigma.sphere.hit` | slot m_A18274A3 effect 2BE1 -> 4010 -> 8DE7 (the shared crack) | a sphere hits a character | own (the shooter) | 0.48 s | H / M |
| `sigma.sphere.hit_enemy` | 6D3 type 0, 0001 -> 397A -> 8A83 (no range) | it hits an enemy | own | 0.63 s | H data, M type 0 |
| `sigma.sphere.hit_enemy_3p` | 90E0 (500 m; 909B 12 m) | the same, at the hit point | 3p | 0.62 s | H / L copy |
| `sigma.sphere.hit_block` / `_3p` | first sphere: 6CB type 2, 0001 -> 38CB -> 591A (aud 1, 70 m) / 591C (30 m) | it hits the world | own (at the hit point) / 3p | 0.73 / 0.85 s | H |
| `sigma.sphere.hit_block2` / `_3p` | second sphere: 6CC -> 38CC -> 591B / 591D | the same | own / 3p | 0.49 / 0.52 s | H |
| `sigma.sphere.fire` (EXTRA) | 1P fire clips 70F6 / 6F5E / 7114: 653D at 0 | each volley (the arm) | own (1P) | 0.55 s | H |
| `sigma.sphere.fire_3p` (EXTRA) | 3P clip 6FC7: 5A19 at 0, 5A28 at 0.60, 5A26 at 1.11 s (30 m) | the same | 3p | 1.60 s | H |
| `sigma.sphere.reform` (EXTRA) | hand-sphere clips (714F, 6F78, 70F7, 7115, 71CD, 7020): 64F4 at 0.61 s | 0.61 s after a volley | own | 1.09 s | H clip, M meaning |
| `sigma.sphere.reform_3p` (EXTRA) | 3P fire clips: 5A1E at 0.80 s (45 m) | 0.80 s after a volley | 3p | 0.89 s | H clip, M meaning |

- **The burst slots (L):** each sphere's slots `m_BAB9824B` (2911 / 2D0D) and `m_AED73588` (2E4B / 2E4A) both hold the burst
  02C; only `m_AED73588` adds 59C8. Read `m_AED73588` as the burst on contact (Jetpack Cat's "world impact / expire") and
  `m_BAB9824B` as the burst at the end of the 21.7 m range. Jetpack Cat's notes (and the effects pass) read `m_BAB9824B` as a
  spawn slot, but here it holds the burst set: if it is a spawn, the burst would sound 0.43 s early. Play `burst` / `burst2`
  where the sphere bursts in both cases.
- **No reload, no dry fire** (H: the spheres have no ammunition). **Barrier hits** (6D3 material 0015 -> 1833 / 1834) are the
  shared barrier impact: `brigitte.hit_barrier`.
- Stadium: #454 6C27 (25F9, the third sphere) is not built.

## 2. Experimental Barrier (1B55 on him; the pet 2D25 runs 1A2A: states v59 1 held, 2 sent, 3 stopped, 4 recalled, #624)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sigma.barrier.deploy` | 1P clip 6F66 (barrier_cast, alias 0x1B7) 31CC: 59D8 (aud 1) | the barrier brought out (1B55 #879) | own (1P) | 0.61 s | H |
| `sigma.barrier.deploy_3p` | 3P additive clip 738E (alias 0x634, #1000) 32EE: 5A95 (40 m) + 5A94 (30 m) | the same | 3p | 0.61 s | H |
| `sigma.barrier.send` | 1P clip 7135 (alias 0x40) 3161: 5E04 (aud 1) | SECONDARY sends it (#717 / #1050) | own (1P) | 1.25 s | H clip, M take |
| `sigma.barrier.send_3p` | 3P send clip 6E84 (0x40 cat 1F8, #861) 3163: 595D + 54C9 (40 m) + 5A93 (30 m) | the same | 3p | 1.33 s | H |
| `sigma.barrier.move.loop` | state 2: #636 (the 20 m/s move) -> #714 holds 2E34: 54EC (aud 1) | while it travels | own | LOOP 9.072 s | H nodes, M pitch |
| `sigma.barrier.move.loop_3p` | 2E34's 54ED (40 m; 5A4A 6 dB quieter) | the same, at the barrier | 3p | LOOP 5.218 s | H / L copy |
| `sigma.barrier.stop` / `stop_3p` | effect 2C96: 5E03 (aud 1) / 54E7 (40 m) | it stops in place (state 3, #696), the recall starts (state 4, #886) and the recall arrives (#929) | own / 3p | 1.03 / 1.07 s | H |
| `sigma.barrier.hum.loop` / `_3p` | state 3: #642's subgraph #715 holds 2C78: 54CB (aud 1, 30 m) / 54D0 (40 m) | while it stands in place | own (at the barrier) / 3p | LOOP 4.490 s | H nodes, M subgraph |
| `sigma.barrier.return` / `_3p` | v59 = 1 with health > 1 (#643 / #694 -> #675 / #691) effect 2C90: 54CA / 54C8 (40 m) | it is back with him whole | own / 3p | 0.46 s | H |
| `sigma.barrier.break` | health 1 or less (#677 / #690 false, #925): 457A's 49F6 + 2E66's 1696 | the barrier is destroyed | own | 3.98 s | H |
| `sigma.barrier.break_3p` | 49F8 (75 m) + 1697 (40 m) | the same, at the barrier | 3p | 3.98 s | H |
| `sigma.barrier.low` | #462 / #463 (< 50 %, < 25 %) onTrue -> #464 457B: 3CBE (120 m) | it falls under 50 % and 25 % | both | 0.76 s | H |
| `sigma.barrier.recall` (EXTRA) | 1P recall clip 6F65 (0x1B6) 3160: 59D6 (aud 1) | the recall (SECONDARY while out, or RELOAD with the option) | own (1P) | 0.66 s | H |
| `sigma.barrier.hold_3p` (EXTRA) | 3P hold clips 6E85 / 73D1 / 73C9 (0x40 cat 82): 5A92 + 5A96 | the barrier held | 3p | 0.51 s | H |

- **The travel loop's pitch:** 54EC / 54ED follow time modulators (1020256101 / 667552234) that rise from -600 (3p -300)
  cents at their start to +1000 at their end. The loop rule reads a modulator at its end (+1000: a 3.6 s loop); built at its
  start, since the barrier usually travels briefly (M). If it sounds too low, rebuild at the end (drop `scen=MOVE`).
- **No regeneration sound** (1A2A #658 / #664 heal it with no effect, H) and **no hit sound of its own** (the attackers' contact
  sets play the shared 1833 / 1834 on material 0015, H). Its 2 % ult charge and Hyper Regeneration have no sound either.
- The barrier pet 6866 (the Stadium 2657 one) is not built.

## 3. Kinetic Grasp (1D02; ABILITY 1 #156; the 2 s chase #489; 1D03 the overhealth pool)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sigma.grasp.start` / `start_3p` | #489 -> #751 (Stadium 2603 off) -> #484 holds 2C26: 5E05 (aud 1) / 54C2 (70 m) | ABILITY 1 | own / 3p | 2.69 s | H |
| `sigma.grasp.loop` / `loop_3p` | 2C26's 5428 (aud 1) / 542B (70 m) | the 2 s | own / 3p | LOOP 4.652 s | H |
| `sigma.grasp.layer` (EXTRA) | #156 m_B969AFC9 -> #564 holds 2E19: 5969 (no range) | the ability's active state | own | 2.73 s | H nodes, M role |
| `sigma.grasp.absorb` / `absorb_3p` | each absorb (#513 / #684) -> #567 / #693 local player is not the shooter -> 2C46: 54AF (no range) / 54B0 (30 m) | each absorbed projectile | own (Sigma, M) / 3p | 0.28 s | H nodes, M who |
| `sigma.grasp.absorb_shooter` | #567 / #693 true (the local player is the shooter v41) -> 2F31 (54AF + 5A98) + 2894 (52BE) | the same, on the shooter's client | own (the shooter) | 0.28 s | H |
| `sigma.grasp.overhealth` / `_3p` | the end (#593 -> #466 ActionPlayScript 1D03) -> 1D03 #207 onBegin -> #209 (amount > 0) -> #208 2D3E at hp 0001: 1A1E / 1A17 (70 m) | the overhealth is given | own / 3p | 1.63 s | H |

- No end sound of its own (5428's Stop event plays nothing, H); the overhealth's 2D3E is the end. Cut short (onAbort), there
  is no overhealth (sigma-map.md) and so no 2D3E.
- 1D02 #456 (222B / 222F: 405A no range, 40E2 60 m, 46F3 20 m) plays per absorbed hit only for damage tag 0412, with copies
  picked by the local player: L which projectile, not built.
- Stadium: #750 6C74 (2603) is not built.

## 4. Accretion (1BBB; ABILITY 2 #447; the 0.65 s cast #535; volley #809, the rock 18B7; contact sets 6DC (characters), 0245 (world))
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sigma.accretion.gather` | #535 (the cast) holds #639 2C32: 5E0B (aud 1) | ABILITY 2 (the 0.65 s gather) | own | 1.11 s | H |
| `sigma.accretion.gather_3p` | 2C32's 54A0 + 964B (70 m, different files: both) | the same | 3p | 1.13 s | H |
| `sigma.accretion.throw` | #809 onBegin -> #879 -> #640 2C33: 5E0C (aud 1) | the throw | own | 2.16 s | H |
| `sigma.accretion.throw_3p` | 2C33's 964C (70 m; 54A2 the same files 14 dB quieter) | the same | 3p | 1.74 s | H / L copy |
| `sigma.accretion.start` (EXTRA) | #447 m_B969AFC9 -> #671 holds 2E1A: 5967 (no range) | the ability's active state | own | 1.93 s | H nodes, M role |
| `sigma.accretion.throw_crack` (EXTRA) | #879 -> #576 1C9C -> 4046 -> 8DE7 | at the throw | own | 0.48 s | H nodes, M meaning |
| `sigma.accretion.flyby` | the rock's slot m_2170A015, 2E9B: 964D (60 m; 59FC the same files) | as the rock passes | both | 1.95 s | H slot, M role |
| `sigma.accretion.flight.loop` | child 3330, effect 2D99: 964E (25 m; 59BF / 5A87 / 964F quieter) | the rock in flight | both, at the rock | LOOP 3.152 s | H |
| `sigma.accretion.impact` | the rock's slot m_6CB564CC, 2C07 (with decals): 54A4 (80 m; 5A7E the same files) | the rock bursts | both | 2.11 s | H data, M slot role, L copy |
| `sigma.accretion.hit_enemy` / `_3p` | 6DC type 0, 0001 -> 39EF -> 8C30 (no range) / 90DC (500 m; 9097 12 m) | it hits a character | own (the shooter) / 3p | 3.02 s | H data, M / L |
| `sigma.accretion.hit_block` | 0245 type 2, 0001 -> 3FBA -> 858A (90 m) + 85C7 (32 m) | it hits the world (material 0001; the shared world set) | both | 3.46 s | H |

- The knockdown on the target is the shared status graph 224E (its sounds are shared, not his, H).
- Stadium: the whole rock graph 1EB9 is gated by 25F6 (#388): 64C7 (C2B8, 80 m) and 69AB (5E81, 200 m) are not built.
- The slot roles (`m_2170A015` a fly-by, `m_6CB564CC` the burst) are other heroes' readings (M). If the fly-by turns out to fire
  as the rock is made, play `flyby` at the throw instead.

## 5. Gravitic Flux (1E41; ULTIMATE #477; 1DBC on each lifted target)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sigma.flux.aim.loop` | reticle cosmetic 31A2 (#1287) child 31A3, 2C9E: 5A15 (30 m) | the aim (up to 5 s), at the reticle | own (M) | LOOP 3.348 s | H nodes, M who |
| `sigma.flux.start` / `start_3p` | #964 v59 = 1 -> #954 -> #967 (0.6 s) holds #1052 2BEF: 5E0D / 5453 (75 m) | ULTIMATE pressed (he rises) | own / 3p | 4.60 s | H |
| `sigma.flux.cast` / `cast_3p` | #1103 -> #972 -> #1117 (the 1 s cast) holds #986 2C6B at the aimed point: 5E0E / 58FD (75 m) | the confirm | own / 3p (at the point) | 2.14 s | H |
| `sigma.flux.flight.loop` / `_3p` | #477 m_B969AFC9 -> #1112 holds 2D85: 5900 / 54F7 (70 m) | the whole ult | own / 3p | LOOP 5.456 / 11.693 s | H |
| `sigma.flux.layer` (EXTRA) | #1070 holds 2E18: 5968 | the ult's active state | own | 7.30 s | H nodes, M role |
| `sigma.flux.end` / `end_3p` | #477 m_6EEEC0AA -> #1069 2E12: 596B / 596C (75 m) | the ult ends | own / 3p | 1.16 s | H |
| `sigma.flux.lift` | 1DBC #675 -> #593 -> #610 holds #619 2E8D at the target's hp 0001: 5454 (75 m) | each target is lifted | both, at the target | 2.72 s | H |
| `sigma.flux.lifted` | 1DBC #599 (the 2 s lift) -> #636 holds #589 2C35: 5502 (no range) | while lifted | own (the target) | 2.64 s | H nodes, M who |
| `sigma.flux.slam` / `slam_3p` | #599 onFinished -> #582 (gravity x22) -> #579 -> #590 2CA2: 550A / 550B (75 m) | the target is slammed down | own (the target) / 3p | 2.31 / 2.25 s | H / M |
| `sigma.flux.slam_land` | #577 (the slam's damage) onFinished -> #576 onFalse -> #609 2CA3: 5E0F (70 m; 550D 75 m) | the slam lands | both | 3.50 s | H / L copy |
| `sigma.flux.slam_hit` / `_3p` | #577 onBegin -> #638 message 1CC9 to him -> 1E41 #1149 -> #1148 (0.5 s) -> #1147 a sweep with 075D: 8DF8 / 90E4 (500 m; 90BD 12 m) | the slam hits each target | own (Sigma) / 3p | 3.50 s | H nodes, M who |
| `sigma.flux.lift_foley` (EXTRA) | 1P lift clip 703D (0x175): 5A85 | the 2.5 s lift | own (1P) | 2.56 s | H |
| `sigma.flux.lift_foley_3p` (EXTRA) | 3P lift clip 6FBC: 5A88 (30 m) | the same | 3p | 2.52 s | H |

- **5E0D Plays an object that exists nowhere:** its event's second Play target 925962713 is in none of the build's 1148 banks
  (`find_object.py`). The layer does not exist; nothing to hunt.
- **Ult lines:** all three are raised together at the press (#477 m_4E22B5C4 -> #1322 STU_37D754C8):
  - **`sigma.ult.friendly`** (0BA4, key F1AB, allies and himself: "What is that melody?")
  - **`sigma.ult.enemy`** (0BA3, key F1AA, enemies: "(Dutch): The universe sings to me!")
  - **`sigma.voice.ultimate`** (185B, key F1AC, his own: "(Dutch): The universe sings to me!")

  As Sojourn's, his own copy has **the enemy line's text**. With `HeroSounds.ultLine` playing 0BA4 / 0BA3, leave 185B
  unplayed (or pick one). Their skin / celebration lines (the "Reversed Dutch" takes) are left out (criteria, H).
- The flight's rise / fall on jump / crouch has no sound of its own (H).

## 6. Perks (51E0; Hyper Strike's target graph 54A5) and the rest
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sigma.perk.levitation.loop` | Levitation (2577): #383 (1.75 s) holds #407 652D: C2F6 (no range) | while levitating | own | LOOP 3.288 s | H |
| `sigma.perk.levitation_3p` | 652D's C2F7 (70 m, a one-shot) | the same | 3p | 3.29 s | H |
| `sigma.perk.hyper_strike.ready` | Hyper Strike (25AF): #389 op73(v11, 5) onTrue -> #409 6545: C320 | the fifth direct sphere hit | own | 0.47 s | H |
| `sigma.perk.hyper_strike.hit` | 54A5 on the target: #396 (0.8 s) -> #532 6550 (C322) + #530 -> #535 67C0 (C648) | the empowered melee lands | own (the target) | 2.30 s | H nodes, M who |
| `sigma.perk.hyper_strike.hit_3p` | 6551's C323 (500 m; C324 12 m) + 67C0's 8060 (45 m) | the same | 3p | 2.30 s | H / L copy |
| `sigma.perk.kinetic_cycle` | Kinetic Cycle (2578): 1D02 #735 -> #738 -> #737 message 2623 -> #741 6731: C6D9 | the Grasp's end cuts Accretion's cooldown | own | 0.31 s | H |
| `sigma.melee` (EXTRA) | 1P melee clip 6FCD 3086: 59F1 + 5A6C (aud 1) | quick melee | own (1P) | 0.41 s | H |
| `sigma.melee_3p` (EXTRA) | 3P melee clip 6F89 306A: 59F7 (30 m) | the same | 3p | 0.33 s | H |
| `sigma.move_3p` (EXTRA) | 3P move clip 70DB 3126: 5909 (30 m) | each 1.23 s move cycle | 3p | 1.12 s | H |
| `sigma.ambient.loop` / `_3p` (EXTRA) | 1869 #49 entry -> #122 holds 2D94: 5907 / 9E0D (30 m) | all the time he is Sigma | own / 3p | LOOP 33.064 / 7.049 s | H nodes, M role |

- **Hyper Regeneration has no sound** (51E0 #431-#440 play no effect, H).
- The quick-melee swing is the shared 0043 6BD4 (5D97); his melee clips add the foley above.
- **No jump or landing sound of his** (H): his hero entity's contact set 06DE is footsteps / landings by material (Minecraft's own
  steps serve), and no graph of his plays a jump effect.
- **The ambient hum's layers follow his ult charge:** 1869 #126 sets a Wwise state (identifiers 59A4 / 59A5 / 59A2 at 0-33 %,
  33-66 %, 66 %+ of 2300), which picks 2D94's layers. States are not reproduced: built at the default (M).

## 7. His voice (voice set 01E5.05F; 287 stimuli, 904 lines)
Chosen as Sojourn's were: the shared moments the mod already uses, plus his own ability callouts. Lines with criteria are left
out and counted in the map. Events are `sigma.voice.<moment>`.

**Shared moments** (stimuli global; Voice.java's shared table has their numbers): elimination 0008 (13 files), multikill 00C9
(3), melee_kill 0032 (4), on_fire 0583 (4), respawn 002C (9), melee 0023 (9), select 002B (1: "Why seek answers when we do not
know the question?"), death 0001 (6), pain 0003 + 0004 (19), resurrected 005A (2), ult_ready 02E7 (2), call_ult_charging 0574
(2), call_ult_almost 05A7 (2), call_hello 01CF (6), call_thanks 01D0 (4), call_sorry 05E1 (3), call_no 01D2 (2), call_group_up
02C6 (4), call_need_help 0427 (1), call_need_healing 0045 (2), ping 11D9 (1), ping_warning 11DA (3), ping_enemy 1117 (3),
damage_boosted 00CC (3), nano_boosted 05E2 (2), call_good_job (ours: his four Teammate/KillSeen 001B lines with no criteria).

- **No reload line exists** for him (H): neither the shared 0E2C nor a stimulus of his own is in 01E5.05F (his spheres have no
  ammunition). So there is no `sigma.voice.reload`.

**His own moments** (the map's integration section has the `Map.entry` lines for Voice.java):

| Event | Stimulus | When (graph node) | Prio / cooldown / interrupt | Files | Conf. |
|---|---|---|---|---|---|
| `sigma.voice.hyperspheres` | 0B99 (exerts) | 187F #98 -> #0 -> #237 (v233: set by #230 after #231's 0.5 s) -> #221 | 60 / 5 s / no | 9 | H nodes, M #0 and v233 |
| `sigma.voice.grasp` | 0B9A (exerts) | 1D02 #156 activation -> #556 | 60 / 20 s / no | 3 | H |
| `sigma.voice.grasp_absorb` | 0B9B ("Hold it together...!") | each absorb #447 -> #453 -> #450 false (damage tag not 0535 / 0412) -> #560, 0.2 s late | 60 / 5 s / no | 5 | H nodes, M tags |
| `sigma.voice.accretion` | 0B9C (exerts) | 1BBB #447 active state -> #0 -> #664 | 60 / 1 s / no | 3 | H nodes, M #0 |
| `sigma.voice.accretion_kill` | 0B9D ("Collision!") | a kill with damage id 53A7 (#636 / #722) -> #723 message 1B27 carrying the line | 60 / 10 s / no | 9 | H nodes |
| `sigma.voice.barrier` | 0B9E ("Barrier initialized!") | 1A2A state 2 begins (#628 onBegin -> #711, to v40 = him) | 60 / 40-50 s / no | 3 | H |
| `sigma.voice.barrier_recall` | 0B9F ("Return to me!") | the barrier back whole (#675 -> #712) | 60 / 30-45 s / no | 3 | H |
| `sigma.voice.barrier_broken` | 0BA2 ("The barrier has fallen!") | destroyed (#678 -> #713; #910 -> #911) | 60 / 15-20 s / **yes** | 4 | H |
| `sigma.voice.barrier_damaged` | 0BA0 ("Barrier is sustaining damage!") | 1B55 #971 each 1 s -> #969 lost 10 % of its max in that second -> #975 | 60 / 40-80 s / no | 3 | H |
| `sigma.voice.barrier_low` | 0BA1 ("The barrier is weakened!") | 1B55 #967 each 1 s -> #973 under 34 % onTrue -> #976 | 60 / 5-12 s / no | 4 | H |
| `sigma.voice.flux_kill` | 0BA5 ("To the void!") | a kill with damage id 53A8 (the slam, 1DBC #577) on a hero (#944 -> #943) -> #1132 (v37204 + 0.5 s) -> #1134 the line in a message | 50 / 5 s / no | 4 | H nodes |
| `sigma.voice.ultimate` | 185B ("(Dutch): The universe sings to me!") | #1322 key F1AC | 95 / 1 s, 80 m, +8 dB | 1 | H |
| `sigma.ult.friendly` | 0BA4 ("What is that melody?") | #1322 key F1AB | 95 / 1 s, 60 m, +6 dB | 1 | H |
| `sigma.ult.enemy` | 0BA3 ("(Dutch): The universe sings to me!") | #1322 key F1AA | 95 / 1 s, 80 m, +8 dB | 1 | H |

- **0BB4 has no event:** absorbing an enemy ultimate (1D02 #450 true -> #572 -> #565 0.75 s -> #451, the absorbed hero as the
  line's hero) has only HeroInteraction lines ("Reach for the stars!" for Cassidy, "The storm has been weathered" for Pharah...).
  With heroes in play it could be wired from those criteria; not built.
- The kill lines 0B9D and 0BA5 travel in game messages (Sojourn's lesson): grep `.078` anywhere in a node.
- **Voice.java stimuli** (`Stimulus(priority, cooldownMin, cooldownMax, interrupts)`, from the map):
  ```
  java.util.Map.entry("sigma.nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2, 80 m, +3 dB
  java.util.Map.entry("sigma.melee_kill", new Stimulus(65, 10, 14, false)), // 0032, 30 m, +2 dB
  java.util.Map.entry("sigma.hyperspheres", new Stimulus(60, 5, 5, false)), // 0B99, 35 m, +0 dB
  java.util.Map.entry("sigma.grasp", new Stimulus(60, 20, 20, false)), // 0B9A, 35 m, +3 dB
  java.util.Map.entry("sigma.grasp_absorb", new Stimulus(60, 5, 5, false)), // 0B9B, 35 m, +0 dB
  java.util.Map.entry("sigma.accretion", new Stimulus(60, 1, 1, false)), // 0B9C, 55 m, +3 dB
  java.util.Map.entry("sigma.accretion_kill", new Stimulus(60, 10, 10, false)), // 0B9D, 35 m, +0 dB
  java.util.Map.entry("sigma.barrier", new Stimulus(60, 40, 50, false)), // 0B9E, 35 m, +3 dB
  java.util.Map.entry("sigma.barrier_recall", new Stimulus(60, 30, 45, false)), // 0B9F, 35 m, +2 dB
  java.util.Map.entry("sigma.barrier_broken", new Stimulus(60, 15, 20, true)), // 0BA2, 35 m, +2 dB
  java.util.Map.entry("sigma.barrier_damaged", new Stimulus(60, 40, 80, false)), // 0BA0, 35 m, +2 dB
  java.util.Map.entry("sigma.barrier_low", new Stimulus(60, 5, 12, false)), // 0BA1, 35 m, +3 dB
  java.util.Map.entry("sigma.flux_kill", new Stimulus(50, 5, 5, false)), // 0BA5, 35 m, +0 dB
  java.util.Map.entry("sigma.ultimate", new Stimulus(95, 1, 1, false)), // 185B, 80 m, +8 dB
  ```
  `sigma.ult.friendly` / `ult.enemy` go through `HeroSounds.ultLine` (0BA4 prio 95, 60 m, +6 dB; 0BA3 prio 95, 80 m, +8 dB;
  cooldown 1 s), not Voice.
- **Voice-line unlocks:** 43 collectible lines in `research\sigma\extract\Unlocks\Heroes\Sigma\VoiceLine\<season>\<name>\`
  (43 unlock folders; his `HeroVoice\Sigma\Voicelines` holds 86 files for the spoken text). For `build_asset_pack.py`:
  `VOICELINE_HEROES["sigma"] = "Sigma"` and `VOICELINE_ROOTS["sigma"] = "../../research/sigma/extract"`.
- Not mapped: 0BB4 (above); the other category-4 lines no graph of his raises; the PVE / mission / objective / emote stimuli.

## Missing, and what was tried
- **A reload or dry fire, a barrier regeneration or hit sound of its own, a Grasp end sound, a knockdown sound of his, a Hyper
  Regeneration sound, a jump / land sound:** none in the live data. Checked against every effect of 187F / 1B55 / 1A2A / 1D02 /
  1D03 / 1BBB / 1EB9 / 1E41 / 1DBC / 1869 / 51E0 / 54A5 / 206C / 3DCE / 0043 (`work\fx_sounds_timed_out.txt`, expressions
  included: every effect their decompiles name), his entities' slots and child entities (`sounds\fx_proj.json`) and every clip
  animation effect (`sounds\clip_sounds.txt`).
- **Which burst slot is which** (`m_BAB9824B` / `m_AED73588`): L. The in-game pass should check that a burst sounds at the
  sphere where it bursts.
- **Which of two ranged copies is which team's** (L everywhere noted).
- **Nothing has been listened to or played in game.** Balance, loudness against the other heroes, and every M / L reading
  above still need the in-game pass.

## The event list by ability (wiring checklist)
- **Hyperspheres:** `sigma.sphere.volley`, `volley_3p`, `flight.loop`, `burst`, `burst_3p`, `burst2`, `burst2_3p`, `impact_3p`,
  `hit`, `hit_enemy`, `hit_enemy_3p`, `hit_block`, `hit_block_3p`, `hit_block2`, `hit_block2_3p`; `fire`, `fire_3p`, `reform`,
  `reform_3p` (EXTRA)
- **Experimental Barrier:** `sigma.barrier.deploy`, `deploy_3p`, `send`, `send_3p`, `move.loop`, `move.loop_3p`, `stop`,
  `stop_3p`, `hum.loop`, `hum.loop_3p`, `return`, `return_3p`, `break`, `break_3p`, `low`; `recall`, `hold_3p` (EXTRA); hits on it:
  `brigitte.hit_barrier`
- **Kinetic Grasp:** `sigma.grasp.start`, `start_3p`, `loop`, `loop_3p`, `absorb`, `absorb_3p`, `absorb_shooter`,
  `overhealth`, `overhealth_3p`; `layer` (EXTRA)
- **Accretion:** `sigma.accretion.gather`, `gather_3p`, `throw`, `throw_3p`, `flyby`, `flight.loop`, `impact`, `hit_enemy`,
  `hit_enemy_3p`, `hit_block`; `start`, `throw_crack` (EXTRA)
- **Gravitic Flux:** `sigma.flux.aim.loop`, `start`, `start_3p`, `cast`, `cast_3p`, `flight.loop`, `flight.loop_3p`, `end`,
  `end_3p`, `lift`, `lifted`, `slam`, `slam_3p`, `slam_land`, `slam_hit`, `slam_hit_3p`; `layer`, `lift_foley`, `lift_foley_3p`
  (EXTRA); the lines `sigma.ult.friendly`, `sigma.ult.enemy`, `sigma.voice.ultimate`
- **Perks:** `sigma.perk.levitation.loop`, `perk.levitation_3p`, `perk.hyper_strike.ready`, `perk.hyper_strike.hit`,
  `perk.hyper_strike.hit_3p`, `perk.kinetic_cycle`
- **Melee / movement / ambient (EXTRA):** `sigma.melee`, `melee_3p`, `move_3p`, `ambient.loop`, `ambient.loop_3p`
- **Any ability not ready:** the shared 0186 -> 0FCB click, `kiriko.ability.denied`.
- **Voice:**
  - shared moments: `sigma.voice.` + `elimination`, `multikill`, `melee_kill`, `on_fire`, `respawn`, `melee`, `select`,
    `death`, `pain`, `resurrected`, `damage_boosted`, `nano_boosted`
  - wheel and pings: `ult_ready`, `call_ult_charging`, `call_ult_almost`, `call_hello`, `call_thanks`, `call_sorry`, `call_no`,
    `call_group_up`, `call_need_help`, `call_need_healing`, `call_good_job`, `ping`, `ping_warning`, `ping_enemy`
  - his own: `hyperspheres`, `grasp`, `grasp_absorb`, `accretion`, `accretion_kill`, `barrier`, `barrier_recall`,
    `barrier_broken`, `barrier_damaged`, `barrier_low`, `flux_kill`, `ultimate`
- **Already registered for every hero** (`OcSounds.init()`): the wheel lines, `resurrected` and `damage_boosted`. Do not list
  them again in his own list: a duplicate registry key crashes the game at start ([sounds](../../techniques/sound/sounds.md)).

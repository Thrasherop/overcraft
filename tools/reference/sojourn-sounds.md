# Sojourn's sounds and voice lines, from Overwatch's game data (build 154088)

Read 2026-11-02 offline and read-only through TankLib / DataTool 2.25.0.0 (`Online=false`) on the archived build
`E:\Overwatch Archive\build-154088`, with `Get-Process Overwatch, Battle.net` checked empty before every TankLib / DataTool run
(the scripts' `owinit.py` refuses on its own too). Nothing was decrypted beyond DataTool's own reading, and nothing went into
the repo but text. Nothing here has been listened to by a person or played in game yet. Confidence: **H** read in the data
(a node, an edge, a field, a file), **M** inferred (one link guessed, or a reading taken from another hero's map), **L** guess.

- **The sound map** (what the pack builder reads): `tools/soundmaps/sojourn.txt`. It has every rule's layers, gains, delays and
  sources, the "Not mapped" list, the voice section and the Voice.java stimulus lines. It is written by
  `E:\Overwatch Archive\research\sojourn\scripts\sounds\sojourn_gen.py` (Juno's `juno_gen.py`, retargeted) from the event
  table `sojourn_table.py` beside it.
- **The kit it hangs on:** [sojourn-map.md](sojourn-map.md) (graphs, nodes, numbers). Method:
  [sounds](../../techniques/sound/sounds.md#sojourn-sounds).
- **Research folder:** `E:\Overwatch Archive\research\sojourn\`. It holds:
  - `scripts\sounds\`: Juno's sound scripts, copied and retargeted (labels, ids, voice set 01C2.05F, the ExtraSounds root),
    plus `sojourn_table.py` (the event table) and `sojourn_gen.py`.
  - `sounds\`: the decode `sojourn_ww.json` (103 02Cs, `sids.txt`), `banks_raw.json` (20 banks with the Init bank 1E818),
    `sojourn_measure.json`, `rtpc_defaults.json`, `contacts.json` (10 contact sets), the entities' effects `fx_proj.json`,
    `voice_lines.json` (355 stimuli, 1196 lines), `overview.txt` (one block per 02C: range, audience, layers) and
    `gen_print.txt` (every rule's layers and what was dropped).
  - `work\`: `graph_effects.json`, `fx_sounds_timed.json` / `_out.txt` (324 effects), `fx_nodes_out.txt` / `fx_nodes_out2.txt`
    (the node that plays each effect), `clip_sounds.txt` (the clip animation effects that hold a sound) and the voice set
    `voiceset_01C2.05F.json` with `stim078.json` / `stim079.json`.
- **Extracted audio:** her skin and voice extraction are in `research\sojourn\extract\` (from the data pass). The 36 banks it
  lacked were extracted for this pass into `research\sojourn\sounds\ExtraSounds\Sojourn\` (`extract_banks.py`, DataTool's own
  sound export; `manifest.json` there): `RailgunProjectile` (2), `RailgunContact` (6), `ChargedShot` (2), `ChargedShotContact`
  (5), `PowerSlide` (11: the slide's contacts and the Dual Thrusters copies), `DisruptorContact` (5), `OverclockContact` (5,
  Stadium, unused).
- **In the pack:** 119 mapped `sojourn.*` events plus 29 voice-line unlocks, 470 .ogg files (8.6 MB), in
  `E:\claude\mods\overcraft_ana\local-assets\pack\assets\overcraft\sounds\sojourn\` (gitignored). What was added there is listed
  at the end.

## How to read the tables
- **Who:** *own* means the player the effect plays on, heard in their head: Sojourn, or the enemy the row names. *3p* means
  everyone else, at her, at the projectile, field or hit point. *both* means one rule for everyone.
  - Audience-1 STUSounds with no range are hers (H, by Lúcio's pairs). Two of hers carry a range (54EB the Disruptor's flight,
    9210 the field) because they sound at the projectile / field (Sombra's lesson): played at it, for her.
  - Audience-2 copies with no range are read as the player the effect runs on (M, as in Juno's and Brigitte's maps).
  - Where there are two ranged copies with the same files, the louder is built and the other is noted (L which team's copy).
- **1P / 3P:** where the sound comes from a clip, the row says which model's clip; the Railgun's reload and energy cues are on
  **the gun's own clips** (set 0AB6), not hers.
- **Length:** the built file's length. For a loop, the row gives its **LOOP LENGTH**: start the loop again every LOOP LENGTH
  and stop it when the state ends.
- **Ranges** come through the volume (metres / 16, as `HeroSounds.split` does). The range is given where the copy has one.

## 1. Railgun (weapon graph 1A23; projectile 2F98; contact set 0701)
PRIMARY held: 1A23 #527 StateLogicalButton -> #526 (v2355 false) -> #571 StateStack, whose top subgraph #639 **holds effect
2BF5** on the gun at the muzzle hp 0006 while she fires (H). 2BF5's 02C are **self-repeating streams**, as Sombra's pistol:
trigger-rate random containers paced by the shared fire-rate parameter 4066808683 (0.071 s at its default 100, H). One Play
keeps re-triggering until the Stop. Built **per shot** (16 a second, volley #502), with its first-shot sets split off and its
ring-out with the Stop sets as the burst's end.

| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sojourn.railgun.fire_first` | 643C (aud 1, no range): the two once-only first-shot sets + one trigger of the stream sets | the first shot of a burst | own | 0.83 s | H data, M per shot |
| `sojourn.railgun.fire` | 643C's stream sets only (38-file report, -1040 click, +200 ticks, a 0.63 s body at -480) | each later shot (16/s) | own | 0.83 s | H / M |
| `sojourn.railgun.fire_end` | 643C's 2.8-3.1 s ring-out set + its Stop-event sets (3.3-3.6 s, 2.2 s) | the burst ends (PRIMARY up, empty, a state taking the gun) | own | 3.72 s | H |
| `sojourn.railgun.fire_first_3p` / `fire_3p` / `fire_end_3p` | 8AF0 (100 m: the same containers 5-11 dB lower, no 0.63 s body) | the same moments | 3p | 0.11 / 0.11 / 3.72 s | H |
| `sojourn.railgun.projectile` | 2F98's `m_2170A015` "sound only" slot, effect 3412: 8F0D (60 m; 603E the same 1 dB quieter) | each projectile as it is made | both | 0.65 s | H slot, M role |
| `sojourn.railgun.hit` | 2F98's `m_A18274A3` "on a target" effect 2837 -> 4010 -> 8DE7 (the shared crack) | a character hit | own (the shooter) | 0.48 s | H / M |
| `sojourn.railgun.hit_enemy` | 0701 type 0, 0001 -> 3E84 -> 8D36 (no range) | an enemy hit | own | 0.46 s | H data, M type 0 = characters |
| `sojourn.railgun.hit_enemy_3p` | 3E84's 9058 (500 m; 9054 12 m, 7 dB quieter) | the same, at the hit point | 3p | 0.46 s | H / L copy |
| `sojourn.railgun.hit_block` | 0701 type 2, 0001 -> 3E90 -> 5F13 (90 m) + 5F21 (32 m) | a hit on the world | both | 0.84 s | H |
| `sojourn.railgun.reload` | **gun's 1P clip 6FCB** effect 30A6: 921C at 0, 921D at 0.566, 921E at 0.812, 921F at 1.106 s | RELOAD (#80), or empty (#1133); her reload is 1.2 s, the clip 1.3 s | own (1P) | 1.34 s | H |
| `sojourn.railgun.reload_3p` | **gun's 3P clip 70D5** effect 30C4: 962A-962D (45 m) at the same times | the same | 3p | 1.34 s | H |
| `sojourn.railgun.idle.loop` (EXTRA) | #1172 -> #624 effect 28EB held from the weapon's start: 5189 (a 9.30 s bed, 33 dB down) | all the time she holds the Railgun | own | LOOP 11.719 s | H nodes, M meaning |
| `sojourn.railgun.idle.loop_3p` (EXTRA) | 28EB's 5E88 (38 m) | the same | 3p | LOOP 11.986 s | H |

- **Empty / dry fire: none in the data** (H). An empty magazine starts the same reload (#1133).
- The reload's clip times are the clip's own (1.3 s): her reload is 1.2 s, so the gun's clip plays a little fast (M). The 3P
  clip 70D5 also holds the 1P set 921C-921F at other times (L a spectating view): not built.
- **Barrier hits** (0701 material 0015 -> 1833 / 1834) are the shared barrier impact: `brigitte.hit_barrier`.

## 2. Energy (1A23 v87)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sojourn.energy.gain` | #586 effect 2C9A: 5505 (no range) | each gain from railgun damage (#597 / #1480 / #650 / #1481 / #777 / #1539 / #1732) | own | 0.37 s | H |
| `sojourn.energy.fill` | effect 32DC: 5E23 (no range) | energy filling by itself: Friction Generators' slide (51EB #448 -> #442) and Overclock's 100/s fill (#739 -> #762 -> #1498 -> #755) | own | 1.62 s | H nodes, M the Overclock moment |
| `sojourn.energy.glow` (EXTRA) | the gun's 1P energy machine (82AD806B), transition clip 7D87 (0 -> 3) effect 3569: 5D1A | L: crossing 65 (the glow; #610's effect 2833 has no sound) | own (1P) | 1.32 s | H clip, **L** meaning |
| `sojourn.energy.full` (EXTRA) | transition clip 7D88 (3 -> 2) effect 355D: 5D12 | L: reaching 100 | own (1P) | 1.32 s | H clip, **L** |
| `sojourn.energy.spent` (EXTRA) | transition clip 7D86 (2 -> 0) effect 3624: 5ECC at 0.367 s | L: a Charged Shot spending it | own (1P) | 0.80 s | H clip, **L** |

- The gun's energy state machine's conditions are not decoded (sojourn-map.md), so which state is which is **L**. Its 3P
  transitions (8437 / 82C6 / 847B) carry no sound.

## 3. Charged Shot (1A23; SECONDARY with energy > 0; hitscan entity 2484; contact set 06FD)
SECONDARY #421 -> #422 -> #451 `op74(v87, 0)` true -> volley #450 makes 2484, whose STUEffectComponent 2835 holds the shot's
sounds (H). Its containers follow **game parameter 2139477060**, a 0-1 value read as the energy spent (M): at 0 they are up
to 10 dB quieter and +300 / +500 cents higher. Built in **three bins** like Lifeweaver's blossom: full 1.0 (energy over 90),
`_mid` 0.6 (30-90), `_low` 0.15 (30 or less): pick with `OcSounds.byCharge(bins, energy / 100)`.

| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sojourn.charged.fire` / `fire_mid` / `fire_low` | 2835's 517B (aud 1, no range) at 1.0 / 0.6 / 0.15 | the shot | own | 3.36 / 3.24 / 3.24 s | H data, M parameter |
| `sojourn.charged.fire_3p` / `fire_mid_3p` / `fire_low_3p` | 517D (100 m) | the same | 3p | 3.36 / 3.03 / 2.70 s | H / M |
| `sojourn.charged.shot` | 2484's `m_2170A015` slot, effect 3349: 8F11 + 551F (60 m each, different files), at full charge | the shot, along its path | both | 2.63 s | H slot, M role |
| `sojourn.charged.hit` | 2835's sub-effect 4010 -> 8DE7 | a character hit | own (the shooter) | 0.48 s | H / M (Sombra's reading) |
| `sojourn.charged.hit_enemy` | 06FD type 0, 0001 -> 3DBE -> 8D93 (no range) | an enemy hit | own | 1.26 s | H data, M |
| `sojourn.charged.hit_enemy_3p` | 8FA4 (500 m; 8FBF 12 m) | the same, at the hit point | 3p | 1.29 s | H / L copy |
| `sojourn.charged.hit_block` | 06FD type 2, 0001 -> 3DC0 -> 5517 (90 m) + 5518 (32 m), at full charge | a hit on the world | both | 3.22 s | H |

- **No energy:** #451 false -> #632 plays **0186 = 0FCB, the shared "ability not available" click**: wire it to
  `kiriko.ability.denied` (H).
- A pierced second or third target: the contact plays per hit (M); no separate pierce sound (H by the effects).
- No charge-up sound (the shot is instant, H).

## 4. Power Slide (1E6F; ABILITY 1 #155; the slide #368, 1 s)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sojourn.slide.start` | 5185 (aud 1): in 2905 (#1039 -> #239, held for the slide) and in 28E8 (#674, on the cosmetic 387D), read as one play (retrigger guard) | ABILITY 1 | own | 1.49 s | H, M one play |
| `sojourn.slide.start_3p` | 2905's 5186 (no range, aud 2, the same files 10-14 dB down) | the same | 3p | 1.49 s | H data, **L** whose |
| `sojourn.slide.loop` | 2905's E56A (a 3.0 s endless bed) | while sliding | own | LOOP 3.001 s | H |
| `sojourn.slide.loop_3p` | E56B (70 m) | the same | 3p | LOOP 3.001 s | H |
| `sojourn.slide.end` / `end_3p` | E56A's / E56B's Stop event (0.68 s) | the slide ends (1 s, a wall, the jump) | own / 3p | 0.68 s | H |
| `sojourn.slide.jump` | #411 the launch -> #277 effect 2C2E: 549E (aud 1) | the slide jump (JUMP while sliding, ground within 2 m or 0.15 s) | own | 1.51 s | H |
| `sojourn.slide.jump_3p` | 2C2E's 54FC (70 m) | the same | 3p | 1.51 s | H |
| `sojourn.slide.scrape.loop` | the ground ray #272 (1.5 m down) -> #273 held contact set 06C1 (0001 -> 3794 -> 549B: 2.19 s and 3.07 s beds) | while the slide touches ground | own | LOOP 2.922 s | H |
| `sojourn.slide.scrape.loop_3p` | 3794's 5579 (50 m) | the same | 3p | LOOP 2.922 s | H |
| `sojourn.slide.touch` / `_3p` (EXTRA) | #322 (v545) -> #283 a sweep, contact set 06C4: 549D / 5577 (50 m) | M: the slide meeting the ground | own / 3p | 0.53 s | H nodes, M moment |
| `sojourn.slide.lift` / `_3p` (EXTRA) | #272 ends -> #327 0.2 s -> #320 a sweep, contact set 06C7: 549C / 5578 | M: 0.2 s after the slide leaves the ground | own / 3p | 0.94 s | H nodes, M moment |
| `sojourn.slide.wall` / `_3p` | #962 wall rays -> #961 -> #641 a sweep, contact set 0702: 5EDD / 5FEC (50 m) | the slide runs into a wall (as it is ended, #963) | own / 3p | 0.51 s | H nodes, M moment |

- The slide's surface sounds come from **contact sets on its own rays and sweeps** (material 0001 built; the other materials
  have their own in `contacts.json`).
- With Friction Generators the slide holds 6BB7 (no sound) instead of 2905, so the start / loop / end above don't play
  then (H by #1039; M that the energy fill is all it sounds).

## 5. Disruptor Shot (1441 on her; projectile 31EF, graph 5342, child 31F0; field 23E6, graph 1443; contact set 076A)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sojourn.disruptor.cast` | #84's activation plug -> #123 effect 322C: 54E9 (aud 1: 1.38 s + 1.46 s 0.16 s late) | ABILITY 2 pressed (the projectile leaves at 0.17 s) | own | 1.62 s | H |
| `sojourn.disruptor.cast_3p` | 322C's 54FB (70 m, the first file) | the same | 3p | 1.38 s | H |
| `sojourn.disruptor.flight.loop` | 31EF's child 31F0, effect 2C8D: 54EB (aud 1, 60 m: at the projectile) | while it flies (40 m/s, up to 0.75 s) | own, at the projectile | LOOP 3.688 s | H |
| `sojourn.disruptor.flight.loop_3p` | 2C8D's 54FF (30 m, listed twice) | the same | 3p, at the projectile | LOOP 4.922 s | H |
| `sojourn.disruptor.hit` | 076A type 0, 0001 -> 5197 -> 5D51 (no range) | it hits a character (and opens) | own | 0.86 s | H data, M |
| `sojourn.disruptor.hit_3p` | 9050 (500 m; 9059 12 m) | the same | 3p | 0.72 s | H / L copy |
| `sojourn.disruptor.hit_block` | 076A type 2, 0001 -> 519A -> 892E (90 m) + 8930 (32 m) | it hits the world (and opens) | both | 2.02 s | H |
| `sojourn.disruptor.field.open` | 1443 #160 -> #312 the expression `{[!v27044] 2228; ...; [Stadium] 632A}` -> #278 holds 2228: 9210's one-shots (aud 1, 70 m) | the field starts (0.1 s after it opens) | own, at the field | 3.54 s | H |
| `sojourn.disruptor.field.open_3p` | 2228's 5402 (70 m; 605F a third copy 2-4 dB quieter) one-shot | the same | 3p | 1.52 s | H / L copy |
| `sojourn.disruptor.field.loop` | 9210's 5.76 s bed | the 3.5 s field | own, at the field | LOOP 5.757 s | H |
| `sojourn.disruptor.field.loop_3p` | 5402's bed + its 2.17 s set (a loop there) | the same | 3p | LOOP 5.757 s | H |
| `sojourn.disruptor.field.end` / `end_3p` | 9210's / 5402's Stop event (1.17 s) | the field ends (3.5 s) | own / 3p | 1.17 s | H |
| `sojourn.disruptor.burn.start` | #157 StateTrackTargets (4 m, enemies) -> #167 per target -> #365 effect 4DA2: 55AB's 0.91 s one-shot | an enemy enters the field | own (the enemy) | 0.91 s | H nodes, M who |
| `sojourn.disruptor.burn.loop` | 55AB's beds (3.01 s at +466, 7.38 s) + its ticks (a random 10 every 1.0 s) | while they burn (90/s) | own (the enemy) | LOOP 7.375 s | H / M |
| `sojourn.disruptor.burn.end` | 55AB's Stop event (0.68 s) | they leave, or the field ends | own (the enemy) | 0.68 s | H |

- **No "opening" sound of the projectile's own** (H): 5342's only sound effect, #56 64DC (C2BF / C2C0 / C2C1), is behind #60,
  the Stadium sticky power 2561's 3.5 s branch. The contact hit and the field's open set carry the moment.
- **The burn has no copy for others** (55AB has no ranged twin, H). 1443 #181 holds 2D44 (58D3 70 m / 55AD 45 m loops) per
  target of a different filter (#182, tag 111B): L whom, not built.

## 6. Overclock (2159; ULTIMATE #363)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sojourn.ult.cast` | #457 (v59 1 / 2 / 3) holds #782 effect 6FE3: E539's 2.53 s one-shot | ULTIMATE (the 0.75 s cast starts) | own | 2.53 s | H |
| `sojourn.ult.cast_3p` | E53A (75 m; E53B 5 dB quieter) | the same | 3p | 2.53 s | H / L copy |
| `sojourn.ult.loop` | E539's 3.44 s bed at -600 cents | from the cast through the 8 s and the 0.45 s tail | own | LOOP 4.869 s | H |
| `sojourn.ult.loop_3p` | E53A's bed at +600 cents | the same | 3p | LOOP 2.435 s | H |
| `sojourn.ult.end` / `end_3p` | E539's / E53A's Stop event (0.98 s) | v59 back to 0 (0.45 s after the 8 s) | own / 3p | 0.98 / 0.90 s | H |
| `sojourn.ult.ending` | #370 -> #795 (v215 > 5) -> #796 `op71(v215, 1.3)` -> #797 effect 6FE2 on the gun: E53C | 1.3 s of the 8 s left | own | 1.90 s | H |
| `sojourn.ult.ending_3p` | E546 (75 m; E547 3 dB quieter) | the same | 3p | 1.90 s | H / L copy |

- **Ult lines:** all three are raised together at the press (#363 m_4E22B5C4 -> #771 STU_37D754C8):
  - **`sojourn.ult.friendly`** (0B19, key F1AB, allies and herself: "It's GO time!")
  - **`sojourn.ult.enemy`** (0D2F, key F1AA, enemies: "This ends now!")
  - **`sojourn.voice.ultimate`** (185C, key F1AC, her own: "This ends now!")

  Unlike Juno's (her own copy has the friendly line's text), Sojourn's own line has **the enemy line's text**. With
  `HeroSounds.ultLine` playing 0B19 / 0D2F, pick one for herself. Their skin / celebration lines are left out (H).
- **Energy during Overclock:** `energy.fill` as it fills (M moment); no gain ticks (no on-hit gain, H).
- **Not built (Stadium):** 2159's beam and blast effects (twelve of them are not even in the build: TankLib's OpenFile fails),
  the beam's contact set 05E2 and #671 39E1 (5BB9).

## 7. Quick melee and perks (EXTRA)
| Name | Ids | When | Who | Length | Conf. |
|---|---|---|---|---|---|
| `sojourn.melee` | **1P clip 6B94** effect 36E7: 5ECD (aud 1) at 0 + 5F24 at 0.567 s | quick melee | own (1P) | 0.91 s | H |
| `sojourn.melee_3p` | **3P clip 7EF3** effect 372E: 5FE7 (30 m) at 0.033 + 5FF4 (45 m) at 0.567 s | the same | 3p | 0.91 s | H |
| `sojourn.perk.thrusters.start` | Dual Thrusters: #1027 true -> #1028 effect 65B6: C3C7 (5185's files +150 cents) | the slide start, with the perk (in place of 28E8; 2905's 5185 still plays) | own | 1.37 s | H |
| `sojourn.perk.thrusters.jump` / `jump_3p` | #1031 -> #1029 effect 65B7: C3C9 / C3CA (70 m) (549E's files +300 cents) | the slide jump, with the perk | own / 3p | 1.27 s | H |
| `sojourn.perk.thrusters.recharge` | #1022 (charges went up) -> #1024 case 1 -> #1026 effect 009B: 0FCF (a shared chime) | a slide charge comes back | own | 1.35 s | H nodes, M moment |
| `sojourn.perk.thrusters.recharge_full` | #1024 case 2 -> #1025 effect 5F58: BB32 | both charges back | own | 0.41 s | H nodes, M moment |

- With Dual Thrusters both `slide.start` (2905's 5185) and `perk.thrusters.start` (65B6) start the slide; the slide jump
  plays 2C2E (#411) only on the base path and 65B7 (#1031) with the perk (H nodes): play `perk.thrusters.jump` instead of
  `slide.jump` then.
- Overcharged and Deceleration Field have no sound of their own (H: 65D3 and 6C02 are particles). Friction Generators: the
  `energy.fill` cue.

## 8. Her voice (voice set 01C2.05F; 355 stimuli, 1196 lines)
Chosen as Juno's were: the shared moments the mod already uses, plus her own ability callouts. Lines with criteria are left
out and counted in the map (two kept on purpose, named in the map). Events are `sojourn.voice.<moment>`. Stimulus numbers are
priority / radius / cooldown, and the number of files built.

**Shared moments** (stimuli global; Voice.java's shared table has their numbers): elimination 0008 (10 files), multikill
00C9 (4), melee_kill 0032 (3), on_fire 0583 (3), respawn 002C (11), melee 0023 (9), select 002B (1: "We're in this
together, all of us"), death 0001 (6), pain 0003 + 0004 (19), resurrected 005A (2), ult_ready 02E7 (2), call_ult_charging
0574 (3), call_ult_almost 05A7 (3), call_hello 01CF (3), call_thanks 01D0 (3), call_sorry 05E1 (3), call_no 01D2 (3),
call_group_up 02C6 (3), call_need_help 0427 (2), call_need_healing 0045 (4), ping 11D9 (1), ping_warning 11DA (3), ping_enemy
1117 (3), damage_boosted 00CC (3), nano_boosted 05E2 (3), call_good_job (ours: her three Teammate/KillSeen lines with no
criteria).

- **No reload line exists** for her (H): neither the shared 0E2C nor a stimulus of her own is in 01C2.05F (as Sombra). So there
  is no `sojourn.voice.reload`.

**Her own moments** (the map's integration section has the `Map.entry` lines for Voice.java):

| Event | Stimulus | When (graph node) | Prio / cooldown | Files | Conf. |
|---|---|---|---|---|---|
| `sojourn.voice.slide` | 0B90 (exerts) | 1E6F #155 activation -> #964 -> #697: the slide starts | 45 / 1 s | 6 | H |
| `sojourn.voice.slide_jump` | 0B91 (exerts) | #193 the jump allowed -> #879 -> #883 | 45 / 1 s | 5 | H |
| `sojourn.voice.disruptor` | 0B92 ("Disrupting!") | 1441 #84 activation -> #124 | 60 / 10 s | 6 | H |
| `sojourn.voice.railgun_charged` | 0E7C ("Railgun charged!") | 1A23 #1181 energy == 100 onTrue -> #1303 -> #1300 not in Overclock -> #1182 | 60 / 15-25 s | 2 | H nodes, M #1303 |
| `sojourn.voice.railgun_holding` | 0E7D ("Railgun still at full power!") | #1181 true -> #1183 wait 10 s -> #1184 | 60 / 20-30 s | 2 | H |
| `sojourn.voice.charged_kill` | 0B8F ("Vaporized") | kill message 0093, an enemy hero, damage id 0980, not in Overclock, not after a slide jump -> #1187 (message 1B27 carries the line) | 60 / 15 s | 6 | H nodes, M 0980 = the Charged Shot |
| `sojourn.voice.charged_kill_air` | 0E7A ("How's that for a fly-by?") | the same after a slide jump (v41319) -> #1192 | 60 / 15 s | 8 | H nodes |
| `sojourn.voice.overclock_kill` | 0B93 ("Target down") | a kill during Overclock: #1278 -> #1307 v842 -> #1282 count + 1 -> #1283 case 1 | 50 / 2 s | 9 | H |
| `sojourn.voice.overclock_kill_2` | 113C ("Two down") | case 2 | 60 / 2 s | 4 | H |
| `sojourn.voice.overclock_kill_3` | 113D ("On a roll") | cases 3 and 4 | 60 / 0 s | 3 | H |
| `sojourn.voice.overclock_kill_5` | 113E ("Canadian hospitality") | case 5 | 60 / 10 s | 1 | H |
| `sojourn.voice.overclock_end` | 0E87 ("Cooling down") | 2159 #363's m_6EEEC0AA plug -> #709 (0.5 s delay) | 50 / 20 s | 4 | H node, M the plug = the ult's end |
| `sojourn.voice.ultimate` | 185C ("This ends now!") | #771, key F1AC | 95 / 1 s, 80 m, +8 dB | 1 | H |
| `sojourn.ult.friendly` | 0B19 ("It's GO time!") | #771, key F1AB | 95 / 1 s, 60 m, +6 dB | 1 | H |
| `sojourn.ult.enemy` | 0D2F ("This ends now!") | #771, key F1AA | 95 / 1 s, 80 m, +8 dB | 1 | H |

- The Overclock kill counter (v41356) is a count of kills of enemy heroes while v842 is on; what resets it was not read (M:
  each Overclock).
- **Voice.java stimuli** (`Stimulus(priority, cooldownMin, cooldownMax, interrupts)`, from the map):
  ```
  java.util.Map.entry("sojourn.nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2, 80 m, +3 dB
  java.util.Map.entry("sojourn.melee_kill", new Stimulus(65, 10, 14, false)), // 0032, 30 m, +2 dB
  java.util.Map.entry("sojourn.slide", new Stimulus(45, 1, 1, false)), // 0B90, 35 m, +0 dB
  java.util.Map.entry("sojourn.slide_jump", new Stimulus(45, 1, 1, false)), // 0B91, 35 m, +0 dB
  java.util.Map.entry("sojourn.disruptor", new Stimulus(60, 10, 10, false)), // 0B92, 35 m, +0 dB
  java.util.Map.entry("sojourn.railgun_charged", new Stimulus(60, 15, 25, false)), // 0E7C, 35 m, +0 dB
  java.util.Map.entry("sojourn.railgun_holding", new Stimulus(60, 20, 30, false)), // 0E7D, 35 m, +0 dB
  java.util.Map.entry("sojourn.charged_kill", new Stimulus(60, 15, 15, false)), // 0B8F, 35 m, +0 dB
  java.util.Map.entry("sojourn.charged_kill_air", new Stimulus(60, 15, 15, false)), // 0E7A, 35 m, +0 dB
  java.util.Map.entry("sojourn.overclock_kill", new Stimulus(50, 2, 2, false)), // 0B93, 35 m, +3 dB
  java.util.Map.entry("sojourn.overclock_kill_2", new Stimulus(60, 2, 2, false)), // 113C, 35 m, +3 dB
  java.util.Map.entry("sojourn.overclock_kill_3", new Stimulus(60, 0, 0, false)), // 113D, 35 m, +3 dB
  java.util.Map.entry("sojourn.overclock_kill_5", new Stimulus(60, 10, 10, false)), // 113E, 35 m, +3 dB
  java.util.Map.entry("sojourn.overclock_end", new Stimulus(50, 20, 20, false)), // 0E87, 35 m, +0 dB
  java.util.Map.entry("sojourn.ultimate", new Stimulus(95, 1, 1, false)), // 185C, 80 m, +8 dB
  ```
  `sojourn.ult.friendly` / `ult.enemy` go through `HeroSounds.ultLine` (0B19 prio 95, 60 m, +6 dB; 0D2F prio 95, 80 m, +8 dB;
  cooldown 1 s), not Voice.
- **Voice-line unlocks:** 29 collectible lines (`sojourn.voiceline.<slug>`, `assets/overcraft/voicelines/sojourn.json`) from
  `research\sojourn\extract\Unlocks\Heroes\Sojourn\VoiceLine` (`build_asset_pack.py`'s `VOICELINE_HEROES` / `VOICELINE_ROOTS`).
- Not mapped: 0E7B ("Their barrier's down!", raised by no graph of hers: L shared); 1893 / 1894; the other category-4 lines no
  graph of hers raises (05EA, 0EC6, 0ED9, 0DC4, 0DDB, 0E35, 11C2 ...); the PVE / mission / objective / emote stimuli.

## Missing, and what was tried
- **A Railgun dry fire, a charge-up, a projectile flight loop, an Overclock cast clip sound, slide clip sounds, an Overcharged
  or Deceleration Field sound, a Disruptor "opening" sound of its own:** none in the live data. Checked against every effect
  of 1A23 / 1E6F / 1441 / 5342 / 1443 / 2159 / 51EB (`fx_sounds_timed_out.txt`, effects in expressions included: every effect
  their decompiles name), her entities' visual slots and child entities (`fx_proj.json`) and every clip animation effect
  (`work\clip_sounds.txt`).
- **Which energy state is which** on the gun (`energy.glow` / `full` / `spent`): L until the machine's conditions are decoded.
- **The charge parameter 2139477060's source:** read as energy / 100 (M); not in her graphs as a write.
- **Which of two ranged copies is which team's** (L everywhere noted).
- **Nothing has been listened to or played in game.** Balance, loudness against the other heroes, and every M / L reading
  above still need the in-game pass.

## What was added to the local pack (`E:\claude\mods\overcraft_ana\local-assets\pack\`)
- **Built** with `build_asset_pack.py sounds` (`py -I`) from a scratch root `E:\Overwatch Archive\research\sojourn\pack_build`
  holding only `tools/build_asset_pack.py` (this worktree's, with Sojourn in the voice-line tables) and
  `tools/soundmaps/sojourn.txt`, into a scratch pack there. Only Sojourn's part was copied over, so the sounds step's rebuild
  of the whole `sounds\` folder did not touch the other heroes.
- `assets\overcraft\sounds\sojourn\**`: **470 .ogg files** (8.6 MB) for 148 events (119 mapped + 29 voice-line unlocks).
- `assets\overcraft\sounds.json`: **148 `sojourn.*` entries** added (1575 -> 1723 entries); nothing else changed.
- `assets\overcraft\lang\en_us.json`: **177 entries** added (148 `subtitles.overcraft.sojourn.*` + 29
  `overcraft.voiceline.sojourn.*`) (1730 -> 1907).
- `assets\overcraft\voicelines\sojourn.json`: her 29 unlocks.
- Backups beside them: `sounds.json.before_sojourn`, `lang\en_us.json.before_sojourn`.
- To rebuild it all the normal way later: `OC_ASSET_PACK=<pack> py -I tools/build_asset_pack.py sounds "E:\Overwatch
  Archive\extracted\154088"` from a worktree whose maps are current (it finds `sojourn.txt`).

## The event list by ability (wiring checklist)
- **Railgun:** `sojourn.railgun.fire_first`, `fire`, `fire_end`, `fire_first_3p`, `fire_3p`, `fire_end_3p`; `projectile`;
  `hit`, `hit_enemy`, `hit_enemy_3p`, `hit_block`; `reload`, `reload_3p`; `idle.loop`, `idle.loop_3p` (EXTRA)
- **Energy:** `sojourn.energy.gain`, `fill`; `glow`, `full`, `spent` (EXTRA, L meanings)
- **Charged Shot:** `sojourn.charged.fire`, `fire_mid`, `fire_low` (+ `_3p` each, by `OcSounds.byCharge`); `shot`; `hit`,
  `hit_enemy`, `hit_enemy_3p`, `hit_block`; no energy -> `kiriko.ability.denied`
- **Power Slide:** `sojourn.slide.start`, `start_3p`, `loop`, `loop_3p`, `end`, `end_3p`, `jump`, `jump_3p`, `scrape.loop`,
  `scrape.loop_3p`, `wall`, `wall_3p`; `touch`, `touch_3p`, `lift`, `lift_3p` (EXTRA)
- **Disruptor Shot:** `sojourn.disruptor.cast`, `cast_3p`, `flight.loop`, `flight.loop_3p`, `hit`, `hit_3p`, `hit_block`,
  `field.open`, `field.open_3p`, `field.loop`, `field.loop_3p`, `field.end`, `field.end_3p`, `burn.start`, `burn.loop`, `burn.end`
- **Overclock:** `sojourn.ult.cast`, `cast_3p`, `loop`, `loop_3p`, `end`, `end_3p`, `ending`, `ending_3p`; the lines
  `sojourn.ult.friendly`, `sojourn.ult.enemy`, `sojourn.voice.ultimate`
- **Melee / perks (EXTRA):** `sojourn.melee`, `melee_3p`; `sojourn.perk.thrusters.start`, `jump`, `jump_3p`, `recharge`,
  `recharge_full`
- **Voice:**
  - shared moments: `sojourn.voice.` + `elimination`, `multikill`, `melee_kill`, `on_fire`, `respawn`, `melee`, `select`,
    `death`, `pain`, `resurrected`, `damage_boosted`, `nano_boosted`
  - wheel and pings: `ult_ready`, `call_ult_charging`, `call_ult_almost`, `call_hello`, `call_thanks`, `call_sorry`, `call_no`,
    `call_group_up`, `call_need_help`, `call_need_healing`, `call_good_job`, `ping`, `ping_warning`, `ping_enemy`
  - her own: `slide`, `slide_jump`, `disruptor`, `railgun_charged`, `railgun_holding`, `charged_kill`, `charged_kill_air`,
    `overclock_kill`, `overclock_kill_2`, `overclock_kill_3`, `overclock_kill_5`, `overclock_end`, `ultimate`
- **Already registered for every hero** (`OcSounds.init()`): the wheel lines, `resurrected` and `damage_boosted`. Do not list
  them again in her own list: a duplicate registry key crashes the game at start ([sounds](../../techniques/sound/sounds.md)).

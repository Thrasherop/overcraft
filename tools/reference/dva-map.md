# D.Va data map, from Overwatch's game data (build 154088)

Read 2026-11-02 offline and read-only through TankLib/DataTool 2.25.0.0 (`Online=false`) on the archived CASC copy
`E:\Overwatch Archive\build-154088` (2026-10-06), with Overwatch and Battle.net checked closed (`Get-Process Overwatch, Battle.net`)
before every DataTool and TankLib run (`scripts\owinit.py` and `scripts\extract_dva.ps1` refuse on their own too). Nothing was
decrypted beyond DataTool's own reading, nothing was run but DataTool / TankLib, nothing went into the repo but text. Confidence:
**H** read in the data (a node, field, value, edge, string), **M** inferred (one link guessed, or a field read by analogy with
[sigma-map.md](sigma-map.md), `juno-map.md`, `sojourn-map.md`), **L** guess. Node refs are `graph #uid`; vars `vNNN`; values by their
`.130` id. Public numbers: [dva.md](dva.md) (its `D` entries come from this pass). Plan: [dva-plan.md](dva-plan.md). Research folder:
`E:\Overwatch Archive\research\dva\` (paths below are relative to it).

Detail behind this map: `work_kit\kit_dva_mech.md` and `work_kit\kit_dva_pilot.md` (the two node-by-node readings this map is made
from), `work_kit\kit_dva.md` (the pipeline), `work_kit\c_<g>.txt` / `decomp_<g>.txt` / `edges_<g>.txt` (112 decompiled graphs;
`scripts\kit\lean.sh <g>`), `work\clipmap_dva\compact_<set>.txt` (every clip by state), `work\animrefs.txt` (which node plays which
alias), `work\ents_dva_out.txt` (every entity), `work\gamevalues_out.txt` (every value with its readers), `work\varvalues_33D9_out.txt` /
`work\varvalues_348C_out.txt` (the mech's and the pilot's values graphs), `work\lostats_out.txt` (the loadouts' stat rows).
Machine-readable: `gamevalues.json`, `gvpair.json`, `loadout_stats.json`, `work\varvalues*.json`, `work\clipmap.json`,
`work_kit\herosettings_dva.json`.

Scripts: `scripts\` = Sigma's, copied and retargeted 2026-11-02 (every `*_sigma.py` renamed `*_dva.py`, paths to `research\dva`); changed:
`hero_dva.py` (finds "D.Va"), `extract_dva.ps1`, `varvalues.py` (G from the environment: 33D9 or 348C, one output each),
`kit\herosettings_dva.py` (hero 007A.075), **`clipmaps_dva.py`** (her alias names, five maps, and clips a set names on another model
listed as such); new: `bounds_dva.py` (a model's rest bounds and hardpoints from the .owmdl). TankLib scripts run with the kiriko-anim
venv's interpreter (`-I`).

Extraction (offline, DataTool 2.25.0.0, logs in `logs\`), into `extract\`: `extract-unlocks "D.Va|skin=Overwatch" --extract-refpose`
-> `Heroes\D.Va` (60 model folders, 413 clips referenced, 400 extracted, 111 effects; 6.9 GB), `extract-hero-voice "D.Va"
--subtitles-with-sounds` -> `HeroVoice\D.Va` (98 stimulus folders, 1129 lines with subtitles), `"D.Va|emote=*"` -> `Unlocks\Heroes\D.Va\Emote`
(15 emotes: Mic Drop, Listen Up, Salty, Heroic, heartbreaker, Party Time, Bunny Hop, ^o^, Game On, Choreography, Make it Rain, Crazy
Choreography, Bow, Dance, Heart Power) and `"D.Va|voiceline=*"` -> `\VoiceLine` (41 files). Every run clean the first time (4.5 GB free).
Hero **`007A.075`** (name 02E2.07C, category 0850 = Tank), hero colour (0.988, 0.475, 0.741).

## Two bodies: the hero is the pilot, the MEKA is her pet (H)
- **The hero entity is the pilot:** Gameplay3P **040A** (mover 2.0 m tall, radius 0.5, crouch 1.25: the hero default in every field),
  1P **040B**, her Light Gun **0982** on hp 000A (var 376A). Graphs: **348C** (her values), **026A** (Call Mech, the link to the mech,
  the eject), **0B49** (plays 514D on the mech), **514D** (perks), weapon graph **02B8** (Light Gun), shared 0033 / 0043 / 004B / 08B6.
- **The MEKA is a pet entity**, **0407** (STUPetComponent, name 29B3.07C; RayCast / health / falling-damage components of its own;
  mover **2.3 m tall, radius 0.7** (1.4 m wide), crouch 1.25 unused), 1P **041F**. Graphs: **33D9** (values), **0276** (spawn, death,
  the wreck window), **02CC** Defense Matrix, **047E** Boosters, **1128** Micro Missiles, **0B2F** Self-Destruct, weapon manager 0015
  with weapon graph **050A** Fusion Cannons; shared 0033 / 004B / 0043 / 08B6. Call Mech spawns it (026A #94, STU_B6E3C77D).
- **The link** (026A #98): control passes to the pet (#100 STU_CA861E44, M), she is attached to it (#95 / #99) with gravity off (#411),
  her view slaved to its (#96), untargetable and posed seated (category 0283, #794 / #832, M). Mech death or Self-Destruct sends
  message 016C, which ends the link: **the eject** (#97: a 9999 heal, knockback 6 m/s back / 7 up from a point 1 m ahead of the
  mech's facing; #370 for 1 s with 0.4 s untargetable, #880). Detail in "The link" below.
- 01C7 (the shared role graph) runs on both (004B #398 -> #396). Graphs 56xx / 57xx / 5016 and entities 66FA / 66FC / 69xx / 6Axx belong
  to another hero the crawl reached through 01C7 (Soldier-like "Pulse Rifle" values), as in Sigma's pass: not hers. F9C failed to open.
- Loadouts (H, `lostats_out.txt`): CF Fusion Cannons, D0 Boosters, D1 Defense Matrix, D2 Self-Destruct, DE Eject!, DF Call Mech, E0
  Light Gun, 16C Micro Missiles, 0347 Role: Tank, 0DA7 Subrole: Initiator, 0DB0 Hero Stats; perks B11 Bunny Power, C84 Extended
  Boosters, B13 Shield System, D41 Precision Fusion. Perk list **0042.177** = [253B, 25E6, 253E, 273E].134 (minor, minor, major, major:
  each file's m_4DDE5023 1 / 1 / 2 / 2).

## Models (`work\ents_dva_out.txt`, the skin's `.owentity` files, rest bounds from the `.owmdl` with `scripts\bounds_dva.py`)
| | entity | model (skin) | base model | set | notes |
|---|---|---|---|---|---|
| MEKA 3P | pet **0407** | **FBB0.00C** (250 bones, 19 meshes; **2.62 m wide (cannons out), 2.43 m tall, 2.03 m deep** at rest) | 11CE / look 14B0 | **0282** | the Fusion Cannons are part of it (bones 08BC / 08BD) (H) |
| MEKA 1P | **041F** | **10354.00C** (122 bones: cockpit, arms, cannons) | 18B4 / 1DE4 | **019B** | eye 1.74 m (m_E29993A5, both standing and crouched) (H field, M meaning) |
| Pilot 3P | **040A** (the hero) | **F2FF.00C** (285 bones, 9 meshes, 1.61 m tall) | 1064 / 1301 | **048A** | (H) |
| Pilot 1P | **040B** | **F32B.00C** | 1D50 / 234A | **0476** | eye 1.32 m, crouched 0.8 (H field, M meaning) |
| Light Gun | **0982** (child of 040A / 040B, hp 000A, var 376A) | **10246.00C** (11 bones, 0.25 m long) | 1E16 / 2455 | **048B** | muzzle hp 0006 on it (M) |
| Light Gun shot | **1000** (+ effect child FEC) | effects only | | | contact set 49D (H) |
| Fusion pellets / missiles / Boosters / Self-Destruct | C18, 408B (projectile visuals), 231B, 133E (model 2D71), 1E8A / 408E / 65DE (model 84C) | | | | which is which: not mapped (L) |
| Select / showcase / intro / gallery | sets 0F0F (select alias 0x477), 0F3B, 124A, 0CA0, 1167, 42FF / 44D7 (intros), 068B (gallery, mech and pilot) | | | | hero file fields (H; roles L) |

- **Hardpoints on the MEKA FBB0** (H, model empties, rest pose, Y up): **0076** (Spine_1) **2.128 m** (its top), **0012 Head** 1.388 m
  (the pilot's head in the cockpit, M), **000A / 000B** (bone_08BD / 08BC, the cannons) 1.197 m up, 1.344 m ahead, 0.4 m out, **0005 /
  0007** the cannon tips (1.45 m ahead), **007C** (Spine_1) 1.721 m up, 0.995 m ahead (missiles / matrix origin, L), 007B / 007D / 007E /
  0080 (the shoulder pods, 1.5-1.75 m up, behind: the Micro Missile tubes, L), 0079 / 0159 (2.0 m up, 0.73 m ahead), 000E 2.62 m, 0520 2.6 m.
- **Hardpoints on the pilot F2FF:** **000A Weapon_R** 0.916 m up (the gun), 000B Weapon_L, **0076 Head** 1.375 m, 0012 Head 1.425 m,
  000E 1.608 m (her top).
- **Movers** (field diff of the dumps against Juno's 5106 and Sigma's 2AB1, H): the pilot is the hero default in all 52 fields (5.5 m/s,
  backward 90 %, crouch 3.0, accel 35, air 3.5, jump 5.72 m/s, gravity 17.5, terminal 30, step 0.5, slope 45); **the MEKA the same but
  height 2.3 and the two radii 0.7**. No crouch: no crouch button in any mech graph, and 0276 #246 sets mover flag x292F571E = 0 (M:
  crouch off). Boosters swap in a flight mover (047E #158: height 2.0, air accel 35, speed factor 1285 = 1.18, so 5.5 x 2.18 = 12 m/s).
  Both carry the default falling-damage component (m_43A3CE29 0.49); no fall node in her graphs (H by search).

## Clip map (`work\clipmap_dva\`; repo `tools/clipmaps/dva.txt` 139, `dva_1p.txt` 75, `dva_pilot.txt` 100, `dva_pilot_1p.txt` 52, `dva_weapon.txt` 11)
| State | MEKA 3P (0282) | MEKA 1P (019B) | Pilot 3P (048A) | Pilot 1P (0476) |
|---|---|---|---|---|
| Stand | alias 1 **3451 / 3452 / 344F / 3450** (1.37 s, dfB7 two states) + add 3419 | stance 33F0 / 33F6 | 322F / 322D (0.7 s) + adds | 30B6 / 30AB |
| Move | alias 5, **8-way 3048 ..** (1.07 s) and a second set **30F8 ..** (0.73 s, dfE5 default) | a98 / a99 move 3190 / 318B | **8-way 3229 ..** (0.67 s) + 0.2 s adds | 30C0 |
| Crouch | none (the shared crouch trees 55 / 56 are bound to the stand / move: no crouch) | | crouch 168A0 / 169CD, crouch-move **8-way 16896 ..** (0.53 s) | 30A9, 318C |
| Jump | 8-way take-off 3465 .. (0.07 s) -> apex 3466 .. -> fall 3467 .. (0.5 s), two variants (dfE5 cat1A5 / default); still 3496 / 3494 / 3495 | | take-off 3216 -> apex 3217 | 30B3 / 30B4 |
| Fusion Cannons | alias 0xA5: seq **ABE1** (0.67 s) -> **AACD** (2.43 s) -> **13525** (0.67 s) (start / loop / end, M) + additive DA AA9D / AA9E (050A #349 / #352) | 352D / 334B | | |
| Defense Matrix | not in 0282 (L: the matrix is the effect and the 1P arms) | alias 0x3F6 **334E / 334D / 331C / 331D** + adds (02CC #355 / #356) | | |
| Boosters | alias 0x3BB **34A6** (0.53 s), seq **34BB** -> **307C** (1.33 s) (start -> flight, M), adds 307D / 3390 / 5577 (047E #145 / #194) | 338D, 3313 -> 318A | | |
| Micro Missiles | alias 0x517 additive **33B7 / 33B6 -> 33B8** (1128 #375 / #376) | 517 (18 clips) | | |
| Self-Destruct | alias 0x175 additive **3644** (5 s, 0B2F #300) | **33CF** (0.6 s) | | |
| Destroyed | alias 0x8 **31C3** (6 s, 0276 #482); 0x4BA 237B after (#363) | **334A** (7.67 s) | | |
| Mech spawn / Call Mech | 0x435 (1P 3329, 0276 #176); 0x3BA **313C** (2 s, #306); 0xE5 **311F** (1.67 s, #309 / #316) | 3329 | 0xE5 **3120** (2 s, 026A #405) | 0x433 **3531**; 0xE5 **3134** |
| Eject | | | 0x439 **3382** (0.567 s, 026A #366) | |
| Light Gun reload | | | 0x4 **3234** (1.5 s, 02B8 #141) | **30BB** (1.53 s) |
| Quick melee | 0xD 341B / 3580 / 357F (2 s, aim triple) | | 3232 stand, 363B / 3231 / 3233 move (0.8 s) | 30B9 |
| Hello / select | 34D5 + 34D7 | 3532 | 36BB + 36BA; select 0x477 **6247 / 6249**, C1A5 / C1A6 | 3650 |

Played by (H, `work\animrefs.txt`): Fusion 050A #349 / #352 (0xA5); matrix 02CC #354 (0x22A start, in no set read) / #355 / #356 (0x3F6);
Boosters 047E #145 / #194 (0x3BB); missiles 1128 #375 / #376 (0x517); Self-Destruct 0B2F #300 (0x175); destroyed 0276 #482 (0x8), #363
(0x4BA); spawn 0276 #176 (0x435), #306 (0x3BA), #309 / #316 (0xE5); pilot 026A #347 (0x433), #405 (0xE5), #366 (0x439), seated pose
category 0283 (#832); Light Gun reload 02B8 #141 (0x4); quick melee 0043 #117 (0xD); weapon draw 0015 #34 (0xA9, L). Not extracted
(both 3P maps): D5DF, D5DE, D7B3, 3A52 and the intro clips 19EE2-19EE5, 1A129, 1A755, 1ACEC / 1ACED (sets 42FF / 44D7 / 0F3B). Clips
under each model in none of the sets read are listed at the end of each map (MEKA 20, pilot 34).

## The kit, node by node (values from the mech's values graph **33D9** and the pilot's **348C**, and the graphs that read them)

The sections below are the two readings (`work_kit\kit_dva_mech.md`, `kit_dva_pilot.md`), kept as written; where they disagreed the
pilot pass, which read 026A whole, settles it: **the eject has a launch** (026A #97: knockback 6 horizontal / 7 vertical with a 9999
heal), which the mech pass had not found (its "no launch impulse node" line is superseded).

**The MEKA:**

### Values graph 33D9 (vars = the JSON keys of `varvalues_33D9.json`; each is `value x (1 + perk/config sums)`)

| var | .130 | value (6v6 alt node) | used by | real meaning |
|---|---|---|---|---|
| (#119 params) | 0377 / 0376 | health **200**, armor **325** (#195 under v66788: 110E **250** / 110F **300**) | 3361 | H |
| v38382 | 0398 | 2 | 050A #421 v476 | pellet damage (H) |
| v38383 | 0399 | 3.375 (#197: 1111 **4**) | 050A #425 | spread deg (H) |
| v38385 / v38389 | 039B / 039C | 20 / 10 (x(1 + v37644)) | 050A #447 / #448 | falloff end / start (H) |
| v38402 | 03A6 | 1 | - | Fusion crit multiplier (no reader in 050A: L) |
| v38654 | 0403 'Fusion Cannon Crit Multiplier' | 1 x **v38610** | 050A #422 v8421 | shots per second in a volley (H) |
| v61446 | **1185** | **11** | 050A #444 v46887 | **pellets per volley** (H; not the missile rate) |
| v56791 | 1DB9 | 0.3 | 050A #376 | Fusion move penalty (H) |
| v38405 | 03A9 | 4 (6v6 1110 4 has no 33D9 reader) | 047E #221 | Boosters cooldown (H) |
| v38411 | 03AB | 25 (x(1 + v73021...)) | 047E #365 | Boosters impact (H) |
| v38421 / v38417 | 03AC / 03AD | 17 / 4 | 047E #369 / #370 | Boosters knockback h / v (H) |
| v38422 | 03AF | 2 | 047E #367 | Boosters max duration (H) |
| v61677 | 1285 | 1.18 | 047E #408 v1002 | Boosters +118 % speed (H) |
| v71038 | 1E09 | -0.9 | 047E #467 v57804 | knockback received in flight (H) |
| v69942 / v69944 | 1B57 / 1B58 'Boosters Duration' | 1.0 / 0.75 | 0276 #590 / #593 | **mech death -> eject 1.0 s, eject -> despawn 0.75 s** (H) |
| v38429 / v38430 | 03B0 / 03B1 | 2 / 5.5 (03B1 x v38680 only with Stadium 02C5) | 1128 #635 / #636 | missile impact / splash (H) |
| v38434 / v38435 | 03B4 / 03B6 | 1.5 / 0.1 | 1128 #633 / #634 | splash outer / inner radius (H) |
| v69935 | 1B51 'Micro Missile Impact Damage' | 0.25 | 1128 #761 v69590 | splash factor at 1.5 m (H) |
| v69940 | 1B56 'Micro Missile Impact Damage' | 18 | 1128 #762 v18199 | missiles per cast (H) |
| v38663 | 03B5 | 50 | 1128 #651 v6368 | missile speed (H) |
| v38664 | **0404** 'Micro Missile Cooldown' | **14** x v38611 | 1128 #645 v8421 | **missiles per second** (H) |
| v38431 | 03B3 | 7 (#199: 1112 **8**) | 1128 #372 v472 | missile cooldown (H) |
| v38670 / v38671 / v38679 | 0406 / 0408 / 040A | 60 (alt 50) / 7 / 30 | 1128 #644 / #647 / #643 | Stadium 02C5 rocket volley (H gating) |
| v67947 | 1864 | 100 | 02CC #647 v67949 | DM energy max (H) |
| v67948 | 1865 | 1 | 02CC #648 v12209 | DM cooldown after use (H) |
| v69921 | 1B49 | 33 (#206 under v66788: 1B4A **50**) | 02CC #652 v12097 | DM drain / s (H) |
| v38439 | 03B7 | 16 x self.v9777 (#231 6v6: 21AE **14**) | 02CC #493 v12096 | DM regen / s (H) |
| v69930 | 1B4E 'Defense Matrix Regen' | 10.25 | 02CC #653 / #205 | DM range (H) |
| v69933 | 1B50 'Defense Matrix Regen' | 0.75 | 02CC #654 v69932 | DM regen delay (H) |
| v38445 / v38447 / v38448 | 03B9 / 03BA / 03BB | 50 / 1000 / 1000 | 0B2F #396-#398 | SD min / at-inner-edge / inside-inner damage (H) |
| v38449 / v38450 | 03BC / 03BD | 6 / 21 | 0B2F #360 / #359 | SD inner / outer radius (H) |
| v56820 | 0C6A 'Self-Destruct Outer Radius' | 3 | 0B2F #410 v215 | **SD fuse 3 s** (H) |
| v69949 / v69950 | 1B59 / 1B5A 'Self-Destruct Outer Radius' | 20 / 10 | 0B2F #458 / #459 | SD knockback h / v (H) |
| v69953 / v69954 | 1B5B / 1B5C | 0.75 / 0.25 | 0B2F #461 / #460 | SD knockback ring factors at 21 m (H values, M which axis) |
| v61928 | 12C3 / 12C4 | 1615 / 2325 | 004B? | perk XP minor / major (H values) |
Not read by any mech graph here: 0AE4 'Ultimate Charge Max' **1700** (alt 3080), 0AE3 280 (Call Mech), 04F0 / 02D0 40 (read by 3361 #54 / #16
and 0043 #? v37583), 03BE 250 / alt 500 and 03BF 3 (Call Mech, pilot 026A). 6v6 switch: `<STU_A3DEF368{}>.v66788` (#196, #198, #200, #205, #230).

### Fusion Cannons (050A)

- PRIMARY StateLogicalButton #364 (buffer 0.15) -> #265 / #311 / #399: muzzles alternate hp **0005 / 0007** (#325-#327) (H).
- **Volley #273** (projectile entity C18, a tracer): **pellets v46887 = 1185 11**, **damage v476 = 0398 2**, falloff **start v41493 = 10 m,
  end v33006 = 20 m**, min fraction m_72A81154 = v37674 (not written in any graph read; 3361 #132's gv 0303 = 0.3 is the likely source: L;
  W 30 %), pellet radius m_57DAE1B5 = **v54891 = 28A3 0.02** (01C7 #1198) (H). Pattern m_5EC1EB38: **1 pellet at 0, 4 at 0.5, 6 at 1.0 of
  the spread** (1 + 4 + 6 = 11), spread **v243 = 0399 3.375 deg x (1 - v75966)** (v75966 > 0 only with Stadium 02CA), jitter 0.7 (H values,
  M that rings are fractions of the spread cone). numShotsPerSecond v8421 = 0403 1 x v38610 (one shot per volley) (H).
- **Rate: StateWait #269 v173 = 0.15 s (#411) at speed v38610 between volleys -> 6.67 volleys/s, 146.7 DPS at 2 x 11** (H). Fire anim
  0xA5 (#349 loop, length 2 / v38610; #352 end cat 82, 2.2 s) (H).
- **Move penalty: #362 mover mod (xF808ACE8 = 10) + #376 v23184 += v56791 0.3 (-30 %)** while firing (v233) (H); gated off by v87 (#363),
  which Boosters sets to 1 for the flight (047E #241): **no penalty while boosting** (M: entity-shared var, and 02CC also uses a v87).
- Hits on enemy heroes (message 0012, identifier 2D4C) -> STU_CEBDAF17 v499 to the owner (#369-#372: perk XP, M). Ammo: none (H by search).
- **Precision Fusion** (273E, 514D #594): RELOAD StateAbility #615; on use (#616) **duration v567 = 3 + 0.1 s**, during #606: **v38610 += 1**
  (#635: fire rate x2 = **13.33/s**), **v37055 -= 0.5** (#637: damage x0.5 = **1/pellet**), **v37644 += 0.5** (#632: falloff x1.5 = **15-30
  m**), **v37527 = chase 0 -> 0.75 in 0.1 s** (#636 / #639: **spread -75 % = 0.84 deg**), back to 0 in 0.15 s at the end (#643), v69686 = 1
  (damage credited to loadout D41, 050A #273 m_loadout) (H). RELOAD again after **0.4 s** ends it early (#650 / #647) (H). **Cooldown
  v61 = v472 = 7.5 s** from the end (#610 / #613) (H). Nothing touches the DM drain (H by search).
- Stadium: 02CA (135D +20 m falloff, 135E spread, 0DF9 +10 % dmg, projectile 408B, #409), 02B0, 02BD.

### Defense Matrix (02CC)

- Energy **v87**, max **v971 = v67949 100 x owner.v9778** (#277 / #340) (H). SECONDARY StateAbility #175 (usable when v87 > 0 and not
  self-destructing v842); active #276 (v87 > 0): **drain #395 chase to 0 at v12097 = 33/s (3.03 s)**, 6v6 50/s (2 s) (H).
- **Minimum 0.5 s** (#321), then release (#320) or, with **Toggle Defense Matrix** (v8175), the next press (#325) ends it -> **cooldown v61
  = v472 = 1865 1 s** (#326 / #377 / #88) (H nodes, M flow).
- **Regen: when v87 < max (#399) wait v69932 = 0.75 s (#284), then chase to max at v12096 = 16/s** (#389): **6.25 s from empty**; 6v6
  14/s (H).
- **Volume StateDeflectProjectiles #205: four boxes making one box x -1.25..1.25, y -1..1, z 0.8..10.25 m** (2.5 m wide, 2 m tall, from
  0.8 to 10.25 m ahead), x entity scale x v57568 (= 1, #462) (H values, M origin = her aim/camera frame). Filter tag 145.06C, team filter 2;
  m_025D0C39 = 180, m_546DFAFF = 3.0 (L: follow / turn params). Which projectiles count is set per projectile (tag 145), not here (M).
- On each absorbed hit (v6584 = damage): **ult STU_008D30B1 v6584 x 0.04 to v27** (#255: **4 % of blocked damage**, M that 008D30B1 is ult
  charge, as Sigma's barrier 2 %); perk XP message 1E87 (#584); **message 2638 (v6584) to self and owner** (#615 / #638) -> **Shield System
  restores shields v30968 x 0.25** (514D #402, H); achievement criterion 1C3D at 1250 blocked (#264 / #266); voice 06C1 0.75 s after
  blocking tags 535 / 412 (#344 / #345); effect at the hit point hp 0228 / 0229 (#208) (H).
- Anims: **0x3F6** active (#355 cat 8F, onBegin #354 **0x22A** 0.5 s), end #356 0x3F6 cat 82 / 94 at 0.3 s (H). 0x96C (#602) = Stadium.
- Stadium: 02C1, 02C3 (#412 half cooldown), 02C8, 02C9 (#455 +50-150 % energy), 02CD (#444 xBB2ADBF2 refill, 60 s), 02CF, 02D2 (#480
  10 m pulse), 0BFC, 0C9C.

### Boosters (047E)

- ABILITY 1 StateAbility #78 (buffer 0.15; not while self-destructing v842) (H). Flight = **#166 chase v215 from v567 = 2 s down at 1/s**;
  **ends early on a second press after 0.4 s** (#179 -> #180), or on release with **Hold to use Boosters** (v12259, #235) (H nodes, M flow).
- **Mover override #158** = the mech mover with height **2.0** and air accel **35** (m_E631BA84; 3.5 on foot) + flags x7FA25A61 1,
  xF808ACE8 1 (prio 1), xD3421CDE 0, x4E596ED2 0, **x786680F6 = v1002 = 1285 1.18 -> 5.5 x 2.18 = 12.0 m/s** (Sojourn's speed-bonus
  field, H by 01C7) (H values, L flag meanings; no gravity field set here, the "along aim" is the flag set: L).
- **Knockback received x0A04B51B = v57804 = -0.9** (#243, under v30820 / v19414: M condition) (H value, Juno/Mercy meaning).
- **Impact #172 StateTrackTargets: a 1.5 m sphere centred 1 m up, 2 m ahead** (x scale), enemies, each once per flight (v375) -> #173
  **damage v476 = 25**, knockback **v29209 17 horizontal / v22029 4 vertical** x owner.v10611, direction from her velocity (m_A4F87630 =
  velocity x 0.5: L), identifier 2D4E (H). Props: StatePhysicsExplosion #216 r 2 m, 1.8 m ahead (H). #253 (0.75 m sphere, tag 9D2) removes
  tagged objects (L). A hit enemy gets identifier 2456 for 2 s (#384, assist credit: L).
- **Cooldown: v61 = v472 = 4 s, written 0.3 s after the flight ends** (#168 -> #171 -> #37) (H nodes, M timing: 4.3 s from landing).
- Messages at the start: **1E66** (#380, Juno's double jump sends it too) and **2B3B** (#487) -> 01C7 #2953 = **the Initiator trigger**
  (H edge; the heal is 01C7's). 0785 (#223). Cooldown refunds: message 2623 (#430), 2757 (#429 starts the ability) (H).
- **Extended Boosters** (25E6): 047E #454-#463: each Booster hit (2D4E) on an enemy hero adds **+0.5 s to v215, capped at 2 s remaining**
  (#462 / #463); 514D #310 **v73021 += 0.4** -> 33D9 #139: **25 x 1.4 = 35** (H).
- Anim **0x3BB** (#145 cat 1B9 for the flight, #194 end cat 82 / 94) (H). Camera / aim: STU_75D1C308 1.12 (#191) and v2400 1 -> 0.9 in
  0.15 s (#204) (L: FOV / turn). No turn-rate node found.
- Stadium: 02B2 (#444 16 m pulse -> 312D), 02CB (#336 duration), 0C94 (#477 -> 4EFE), 0D88 (#492 +0.5 s).

### Micro Missiles (1128)

- ABILITY 2 StateAbility #175 (#693: #594 with Stadium 02C5, #716 with Heavy Rockets 253D). One charge (v181 = 1, v11) (#722) (H).
- **Cast wait v670 = 0.25 s** (#652, #363) then **volley #357**: projectile **1E8A**, **speed v6368 = 50**, **v18199 = 18 missiles at v8421 =
  0404 14/s (1.29 s)**, muzzles alternate hp **0075 / 0077** (H). Impact **v2577 = 2**, radius m_57DAE1B5 = **28D6 0.1 + v54937 (28A6
  0.075) = 0.175 m** (H). Explosion **v11863 = 5.5** with rings **0.1 m (factor 1)** and **1.5 m (factor v69590 = 0.25: 1.375)** (H values,
  M factor-at-ring); knockback **0** (m_E33EFEE1 / m_10399AE7 = v56897, a config sum, 0 base; ring v57017 likewise) (H). Self-damage: no
  field identified (m_D2D11CE9 flags A3C7F66A 0, 43D015B3 / 7F5CC8D3 / B15688ED / 21CEF653 1: L).
- **Cooldown v472 = 7 s** (8 in 6v6) chased to 0 from the volley's end (#412 -> #407 / #408) (H nodes, M timing). Refunds 2623 / 21E2 /
  2751 / 2758 (#677-#698), reset by 187C (#384) (H).
- Hits (identifier 2E7A) -> perk XP (#368-#371, #660-#663) (M). Anim **0x517** (#376 start, #375 end cat 82 at v173) (H).
- **Heavy Rockets (253D, removed perk, unreachable): #713: 7 rockets at 6/s, 5 impact, 22 explosion (2 m full, 3 m 50 %), gravity 18,
  projectile 65DE** = BZ 2025-07-22's numbers (H). Stadium 02C5 (#590: 0406 60 m/s, 0408 7/s, 0C8A 6-10 rockets, 0222 x3.75), 02BB, 02C2.

### Self-Destruct (0B2F)

- ULTIMATE StateAbility #239 (buffer 0.15, needs v342 == v338: full charge) **or message 0592** (#286; sent by 0276 #215 when ULTIMATE is
  pressed in the death window) -> **State #287**: **v842 = 1** (#253: blocks Boosters / DM / Missiles), **v342 = 0** (#294), **message 016C
  to the pilot = eject** (#271), anim **0x175** (#300, 3.5 s clip over v215), voice (#443) (H).
- **Fuse StateWait #270 = v215 = 0C6A 3 s** (H). During it the indicator (#315, radius 21), enemy warnings within **21 x 1.25 = 26.25 m**
  (#343 / #341) and HUD ranges **v3133 21 / v3132 26** (#288 / #289 / #339) (H). No mover node in 0B2F: the mech keeps whatever motion it has
  (M; W "keeps moving if cast during Boosters").
- **At 3 s:** message **0592 to the pilot** (#274) -> **026A #887: pilot v342 = v338 (Call Mech fully charged)** (H). Targets #259: sphere
  **v6252 = 21 m** (x scale) centred **1.3 m up**, filter enemies (64) with condition STU_A58C1E63 (target, source self) = a **line-of-sight
  test** (M). If STU_88687E54 (position + 2 m) is true no damage is dealt (#364 -> #265) (M: out of the map / kill volume).
- **Damage per target (#280): within v1657 = 6 m: v2577 = 1000; beyond: 50 + 950 x (21 - d) / 15**, i.e. **linear 1000 at 6 m -> 50 at 21 m**
  (H expression). Knockback **20 horizontal / 10 vertical** (22.4 m/s) x owner.v10640 with rings **6 m (1 / 1 / 1)** and **21 m (0.75 / 0.75
  / 0.25)** (H values, M axis order). StateModifyHealth mode x60671788 here deals flat 1000: **x60671788 is not "percent"** (contradicts the
  Sigma reading note; M).
- Props: StatePhysicsExplosion #314 (16.8 m), #337 (4.5 m, inward), #340 (8.4 m) (H). Then **0.3 s** (#265) and the mech is removed
  (#266) (H). D.Va herself: not targeted (enemy filter, M).
- Stadium: 02C0, 0C97, 0E57, 2A59 (-25 % Call Mech charge, 026A #887), 01DB (+100 % range).

### Mech base, death and eject (0276; pilot side 026A)

- **Death:** message 0011 with v33688 and not self-destructing (#55 / #384) -> effect 1572, **message 068F (mech ult v342, v842, cost v338)
  to the pilot** (#297, receiver not in the graphs read), perk XP +1 (#282), then (#507):
  - **#79 the dead state** (999 s): mover flags x493C433E / x4E596ED2 / x7FA25A61 / x5C571F43 = 0, x292F571E = 1, priority 99 (#75: frozen,
    M); **ULTIMATE (#213, buffer 0.15) with full charge -> message 0592 = Self-Destruct from the wreck** (#214 / #215); anims **0x8**
    (#482, 7 s clip, inside #313's 3 s) then **0x4BA** (#363) (H).
  - **#84 wait v69941 = 1B57 1.0 s -> message 016C to the pilot = eject** (#303 / #59), then **#60 wait v69943 = 1B58 0.75 s** (messages
    26BA, 2A1B) -> **#112 the mech is removed** (H). So: **1.0 s from mech death to eject (the Self-Destruct window), wreck gone 0.75 s
    later (1.75 s)** (H).
- **Pilot on 016C (026A):** the in-mech state #98 ends (#123 / #102 stop plugs): v42067 (in mech) -> 0 (Bunny Power fires, 514D #440), exit
  #99 -> #644 (if alive) -> **#370 1 s eject**: anim **0x439** (0.567 s), a 2 m ceiling ray (#658), **#880 0.4 s STU_9028E035 type
  x45AB6711** (M: the damage immunity, = BZ's 0.4 s); **Call Mech locked 1 s** (#640 v7145) and abilities 0.15 s (#747) (H nodes). No launch
  impulse node found here (superseded: 026A #97, below) (L: the eject's rise may be animation / exit-point driven, #657 STU_CFCE55DC to +0.5 m).
- **Ult charge kept:** a new mech's SD charge starts from the pilot's stored v2511 = max(previous mech's v342, v29327) (0276 #88 / #386,
  026A #775), unless v13024 (#341: full) (H nodes, M meaning: **the Self-Destruct charge carries over between mechs**). The pilot's Call Mech
  charge is set to 0 on entering a mech (026A #152 -> #85) (H).
- Other 0276: on spawn messages A43 / 0BB7; Call Mech landing anim **0xE5** (#309 / #316, 1.66 s, on message 007A from the pilot) and
  **0x435** (#176, 1.2 s), **0x3BA** (#306, 2 s, for the spawner's view: L); 1C44 (#369) = a camera offset (-1.25, -1.0, 0.25) (L), 41DB
  (#577) = a game-mode ground tracker, 3BA0 (#539) = a pilot UI widget (H nodes, L roles). Voice / rumble 1.8 s after a spawn (#561).

### Quick melee and perks on the mech

- Quick melee: shared 0043 (anim **0xD** #117, 0x22A #145), damage **02D0 40** (0043 v37583; 3361 #16) and **04F0 40** (3361 #54) (H).
- **Shield System** (253E, 514D #282): v37090 -= **100** / v37091 += **100** (health -> shields, under v6700 = 1 from 0276 #241: mech only,
  M), DM refill **25 %** (#402) (H).
- **Bunny Power** (253B, 514D #440): when v42067 drops (eject): **75** overhealth pool DA18 (#424 / #427), **after 10 s decays 25** (x v9527,
  #425 / #426; per second: M) (H).

### Value names that mislead (read graphs, not rows)

0404 'Micro Missile Cooldown' = missiles per second; 1185 (no name) = Fusion pellets; 0403 'Fusion Cannon Crit Multiplier' = shots per
volley rate; 1B4E / 1B50 'Defense Matrix Regen' = range 10.25 / regen delay 0.75; 1B51 / 1B56 'Micro Missile Impact Damage' = splash edge
factor / missile count; 1B57 / 1B58 'Boosters Duration' = death -> eject / eject -> despawn; 0C6A 'Self-Destruct Outer Radius' = the 3 s
fuse; 1B59-1B5C 'Self-Destruct Outer Radius' = knockback 20 / 10 and its ring factors; 03BE 'Self-Destruct Damage Min' 250 = Call Mech.

**The pilot, Call Mech, the link, perks, the role:**

### Pilot values (348C; var <- value, readers)

| var | value | reader | meaning | conf |
|---|---|---|---|---|
| (#119) | **0268 175** health, 64E7 5.5 (move speed) -> graph 3361 | 3361 | pilot health / speed | H |
| v37583 | **02D0 40** x (1 + mods) (#102) | 0043 #306 / #308 -> v43490 | quick melee damage | H |
| v38396 | **03A2 14** (#131) | 02B8 #326 -> v476 | Light Gun damage | H |
| v38397 | **03A3 1.4** x v38631 (reload speed) (#132) | 02B8 #323 -> v581 | reload time | H |
| v38400 | **03A4 7** x v38610 (#133) | 02B8 #327 -> v8421 | shots / s | H |
| v38401 | **03A5 50** (#134) | 02B8 #328 / #94 | projectile speed | H |
| v38403 | 03A7 1.0 (#136) | **none** in any graph | "crit multiplier" scalar | H (unread) |
| v38404 | **03A8 20** x v10265 (#137) | 02B8 #325 -> v198 | magazine | H |
| v38451 | **03BE 250** (alt 500) (#156) | 026A #833 -> v2577 | **Call Mech damage** (name "Self-Destruct Damage Min" misleads) | H |
| v38452 | **03BF 3** x (1 + v38703) (#157) | 026A #834 -> v11861 | **Call Mech radius** | H |
| v38710 | 0412 60 (#158) | none | "Mega Blaster" (Stadium) | H |
| v61928 | 12C3 / 12C4 (#165) | 527A | perk XP | H |
| v69955 / v69956 | **1B5D 15 / 1B5E 3** (#166 / #167) | 026A #949 / #950 -> v29209 / v22029 | **Call Mech knockback h / v** | H |

### Light Gun (02B8; H unless said)

- Fires on the **Light Gun Input** option v51472 (#371): default -> PRIMARY xD128396E (#372); id C911 -> SECONDARY xB42D90B7 (#373); C912 ->
  both (#83 + #361); buffer 0.15 (H nodes; which id is "Secondary" / "Both": M by entry order).
- **StateWeaponVolley #94**: projectile **1000.003**, **1 per shot at v8421 = 7/s**, speed **50 m/s**, lifetime **5 s** (250 m), damage
  v476 = **14** (loadout E0), hit size **28D5 0.2 + 28A6 0.075 = 0.275 m**, environment size 0 (m_D8905532), from **hp 0006** on the gun. **No
  falloff fields** (m_EAD7F104 / m_157E5BC5 / m_72A81154 absent; Sombra's pistol has them) and **no spread** (v229 = v230 = 0, #100 / #101).
  Crit: no D.Va value is read (03A7 unread) -> the generic headshot x2 (M). m_FEC435C6 100 (L: a hitscan range cap, unused by a projectile).
- **Magazine 20**, 1 a shot (v53). **Reload** on RELOAD x5D2A8B7F when v53 < v198 (#80 / #88), or when empty (#75, M): anim **0x4** over
  **v581 = 1.4 s** (#141), **ammo full at 0.65 x 1.4 = 0.91 s** (#354 v6884 0.65 -> #215 -> #220), then v414 = 1 for the last 0.49 s (#218, M:
  a fire-cancel window); a magazine cosmetic **133E** drops from hp 0011 at **0.285 s** (#206 / #207). Message **007D** refills it at once
  (#175): sent by 026A #152 each time she links to a mech (so she always ejects with a full gun) (H).
- Stadium only: loadouts 02BD / 02B6 / 02C3 (#388 / #392 / #393), 0412 Mega Blaster. Rate limiter #359 / #375-#377 (STU_B793E49F 4 x v8421,
  buttons x00B15014 / xB42D90B7): L (BZ 2023-06-13's mouse-wheel fix).
- 0B49 #158 STU_3716E1D4: **Relative Light Gun Aim Sensitivity v50442 / gyro v50443** (1 when unset) (H node, M: the aim scale as pilot).

### Quick melee (pilot)

Shared 0043 on 040A (contact set 05EA; the mech uses 0744): **40** (02D0 via 348C #102 -> 0043 #306 / #308), anim **0xD** (0043 #117,
0.76 s) and 0x22A (#145) (H). Cooldown / range are the shared graph's (Sigma's pass).

### Call Mech (026A)

- **Cost: 0AE3 280** (alt 420), given to 004B by the pilot entity's schema 0152 (H; the alt column is another mode, L: 6v6?). Charge v342 /
  cost v338. Passive and damage charge are not in 026A (engine / 004B: Open).
- **StateAbility #87**: ULTIMATE x2DFEDC52, buffer 0.15, error effect 0186; usable when **v342 == v338**, **not v7145** (the 1 s lock after an
  eject, #640) and STU_3ACE35FB(v7605) (M: not stunned / hacked) (H). On activate (4E22B5C4): HUD #753, rumble #881 (x Call Mech rumble
  v54179), #956, #979 (H).
- **Placement** (#115, while active, if !v478 && !v6868): v956 = **1.27 m** x scale (#451) -> **#256 placement probe**: from her + 1.5 m up +
  1.5 m along her flat facing, cast **4 m down**, radius **0.9**, slope **45°**, clearances 2 m / 0.75 m, ignoring the old mech v38, v49,
  v1624 -> v1626 (H fields, M meanings). Found -> v680 = 1.27 (#446). Not found -> if grounded (v545, M) or **ground within 2 m below her**
  (#455: ray from 1 m up, 3 m long) -> v680 = 0 (#445: drop it on her own spot); else **error effect 0186 and no cast** (#462 -> #461) (H).
  Then #707: **no cast if an entity tagged 02FF.06C is within 3 m** (v12053, #706; L what carries the tag). Only then **v342 = 0** (#715): a
  failed placement keeps the charge (H flow).
- **Cast** (#715 -> #91 / #441): **inputs off** for the whole call (#105: 493C433E / 4E596ED2 / 7FA25A61 / 292F571E / 5C571F43 jump /
  D3421CDE / F95352E8 = 0, priority 99; M meanings from Lúcio / Sojourn / Lifeweaver), velocity x57643451 / x796661A6 = 0 at the start (#397,
  M: stopped), view pitch 0 / own yaw (#400, then #106 eases pitch back over 2 s via v176 #443 and tints the screen grey 0.18 a 0.78: M).
  **0.2 s** (#91; anim **0x433** for those 0.2 s unless v41764, #943 / #347) ->
- **Spawn** (#796 -> #401 -> #418 / #782 / #94): v6472 = the probe spot v1626 if v680 > 0, else **her position - 0.75 m** (#418); the old mech
  gets **303B** (#782: removed at once, or after its Self-Destruct ends, 10 s max + 3 s) (H); **STU_B6E3C77D #94 spawns pet 0407** at v6472,
  owner = her player (m_D749D7D8 STU_F7262B36{self}), facing hers, -> v38 (H; it is a spawn node, not StatePet). Message 007A to the mech
  and her (#348): the mech plays its landing **0xE5** (0276 #309 / #316, 1.66 s) (H). Effect 1573 + rumble at the spot (#417 / #471).
- **Landing hit at spawn + v12209 = 0.7 s** (#869 / #639; a warning sphere #868 of radius v11861 for the 0.7 s) -> **#241 StateTrackTargets**:
  sphere **v11861** centred on the mech + 0.5 m, filter m_84065FD1 64 (enemies, M) -> per target:
  - base (#1001 -> **#242**): **v2577 = 250** to all in **3 m**, knockback **15 horizontal / 3 vertical** (= 15.3 m/s) from a point **2 m ahead
    of her** (m_C0139FA6 = her + facing x 2; M: about the mech's centre), knockback ring 3 m factor 1, x STU_3832D36C(v10876) (M: a mode
    switch), loadout DF, identifiers 078D / 2D50 / 4D2A / 3FCE / BEC9 / EC44 (H).
  - **Bunny Power** (253B, #898 -> #907-#909) or Stadium 2A5B (#998 -> #992-#997): radius v11861 = 3 x 1.5 = **4.5 m** (#941 / #969 v38703 =
    0.5) with **falloff: 250 inside 03BF 3 m, then 250 x (4.5 - d) / 1.5, 0 at 4.5 m** (#908 / #993) (H expression).
  - So the hit lands **0.9 s after the press** (0.2 + 0.7; W 0.896) (H). Damage it deals is counted for her (#682-#684, stat; L).
- **Getting in** (#119, if the new mech is alive and !v38.v478): **#104 2 s** (v842 = 1 "calling", #780; Call Mech stat +1 #465):
  - spawn + 0.7 s when dropped on her spot (#447 -> #768 / #772 / #448): she **backs off 1.27 m** over 0.7 s (MoveTo, M field meanings).
  - spawn + **0.8 s** (#350 -> #351 / #352 / #353): STU_D07EBF01 self, **x45AB6711** (M: untargetable), **gravity x5B83ECB5 = 0**, then
    MoveTo to **1.27 m behind the mech** (0.3 s #434 then #766 0.5 s, unless already there #771 / #767) (H nodes, M meanings).
  - her anim **0xE5 for 2 s** from the spawn (#405, with a MoveTo to the spot's height #951, criteria 198F; then 198E at 0.5 s of #366) (H).
  - spawn + 1.5 s: v6392 / v6400 = 1 (#432, M: HUD); spawn + **1.75 s** (#431) -> **link state #98** (below) = she drives the mech **1.95 s
    after the press** (H).
  - **#431 onAbort -> #121 removes the new mech** (H); the charge was already spent at the press, so a cancelled call loses it (H flow; M that
    stun / knockdown / hack is what aborts it: W).
- The warning-ring effect size (dataflow B8C): 1.6, **2.6 with Bunny Power** / 2A5B (#920 / #923 / #975) (H values, L meaning).
- **Spawning in a mech** (respawn): message 0013 (#73: v342 = 0) -> #371 / #633 -> #673 -> #838 -> #837 -> #716 (unless v12428, L: a
  pilot-only mode) -> **#781 303B on the old mech + #75 spawn 0407 at her position - 0.75 m** -> #763 -> **#98 at once** (no drop) (H nodes,
  M meanings).
- Self-Destruct's detonation: 0B2F #274 message **0592** -> 026A #421 / #422 (she controls herself, !v478) -> **#887 v342 = v338 = Call Mech
  full** (x 299F 0.75 with Stadium 2A59) (H). Another mode flag v51313 fills it too (#875 / #892 / #894: L).

### The link: how the two bodies work (026A #98, 0276, 0B2F)

- **#98 State "linked"** begins at Call Mech + 1.95 s (#431) or at a respawn spawn (#763); **stopped** (plug m_F198FD3A) by message **016C**
  (#123) or by #655 (H edges). While it runs (H nodes, meanings M):
  - **#100 STU_CA861E44 (self -> v38)**: control of the pet (M: input and camera go to the mech; the mech's 1P 041F, eye 1.74, takes over).
  - **#95 STU_5AFB02C7 (self, v38)** -> after a tick (#433) **#99 STU_72DDE005 = attach to v38** (Brigitte's reading of 72DDE005, M):
    the pilot body rides inside the mech; #411 **gravity x5B83ECB5 = 0** and x89AC2608 = 0; #643 STU_45D3C742 identifier **074E** from the
    mech to her (L: shared damage credit; 074E is also the mech's self-kill identifier, 0276 #63).
  - **#96 her view = the mech's pitch / yaw** (STU_9935FBC8 with STU_40AC5AE4 / STU_C8C4C20D of v38): when she ejects she faces where the
    mech faced (M).
  - #152 message **007D** to herself = **Light Gun refilled** (02B8 #175) -> **#85 her v342 = 0** (Call Mech charge starts at 0 after any
    eject) (H). #125 v2171 = v38; #406 identifier 191E on her (the mech gets 191E too, 0276 #302); **#839 v42067 = 1** (the "linked" flag
    Bunny Power watches) (H).
  - When the player no longer controls the pilot (#66 false -> #794): **STU_D07EBF01 self** (v6604 -> v1273 = anim category **0283**: her
    seated pose, M), **x45AB6711 (untargetable, M)**, xD3421CDE = 0, message 08A5 (#65 / #383 / #701), HUD v6828 = 0 (H nodes).
- **The mech dies** (0276, other pass): lethal message 0011 (#55 / #384) -> wreck #79 (999 s, still controlled: **ULTIMATE -> Self-Destruct
  from the wreck**, #213 / #215 0592) and **#84 1B57 1.0 s -> #303 / #59 message 016C to her**, wreck removed 0.75 s later (#60 1B58,
  #112) (H). Self-Destruct sends **016C at its cast** (0B2F #271) (H).
- **Eject** (016C -> #98 stops -> #95 / #99 end): (H nodes)
  - **#97 StateModifyHealth (heal type xD52DFCA4, amount 9999) on her with knockback 6 horizontal / 7 vertical** away from a point 1 m ahead
    along the mech's facing (m_C0139FA6 = her + v38 forward), ring 100 m factor 1, m_10399AE7 1.15: **a full heal (175) and a launch back and
    up, 9.2 m/s** (fires at the end of #965, under #99's #939 / #959; H node, M that it runs at the detach).
  - #644 (if her health > 0, M) -> **#370 1 s**: anim **0x439** (17/30 s = 0.567 s), **#880 0.4 s of x45AB6711 = BZ's 0.4 s damage
    immunity** (M meaning), a 2 m ceiling ray from + 0.5 m (#658) -> if clear #657 STU_CFCE55DC to her + 0.5 m with the mech as reference
    (L: an exit-point move), then xD3421CDE = 0 (#430, M: an input off); 0.4 s STU_9028E035 also at #879.
  - On 016C also: **Call Mech locked 1 s** (#640 v7145) and abilities held 0.15 s (#747 v33) (H).
  - Control returns to her (#66 true) -> **#775 v2511 = max(dead mech's v342, v29327)**: the **Self-Destruct charge is stored** and **+5 a
    second** (x v9532, default 1) while the round runs (v2426, 01C7 #266 / #366), it is below the cost and she is not calling (#652 / #649 /
    #650); the next mech copies it 0.1 s after spawning (0276 #229 -> #341 -> **#88 / #386 v342 = pilot.v2511**, or full under v13024) (H
    nodes, M meaning: the Self-Destruct charge carries over and grows while she's a pilot). 0276 #297 also sends 068F (charge, v842, cost)
    to her at death (receiver not found).
  - **No Call Mech charge is given on eject** (no v342 write on 016C; H by search): she starts from 0 (#85).
- If her own entity dies while linked (message 003E, #645): **#654 the mech is killed too** (9999999, identifier 2406) -> 0618 -> link stops
  (#655) (H; M: falls / kill volumes, W "dies instantly in the mech").
- A **new mech each call** (#94), so it always starts full (its pools from 33D9); the old wreck goes through 303B (H).

### Perks (514D on the pilot; 0B49 #172 also plays 514D on the mech v2171)

- **Bunny Power 253B / B11 (minor)**: 514D #440 / #421 / #422: **when v42067 drops (the link ends = eject)** -> **75 overhealth** (#424 v469 /
  v43330 = 75, StateHealthPool #427 id DA18), **after 10 s it decays 25** (#425 / #426 StateModifyHealth 25 x v9527 repeating, M per second)
  (H); and **Call Mech radius x1.5 with the 3 -> 4.5 m falloff** (026A #932 / #934 / #941 v38703 = 0.5; #898 / #908) (H).
- **Extended Boosters 25E6 / C84 (minor)**: 514D #310 -> #656 **v73021 += 0.4** (the +40 % Boosters damage); the +0.5 s and its 2 s cap in
  047E (kit_dva_mech.md) (H).
- **Shield System 253E / B13 (major)**: #406 v221 = **100**, v176 = **0.25**; on the mech (v6700, 0276 #241) **health -100 / shields +100**
  (#400 / #399); message 2638 (Defense Matrix absorbed v30968) -> **#402 restores v30968 x 0.25** (loadout B13, identifiers 025A / 103B5);
  effect 6420 0.5 s on heals with 103B5 (#429-#434) (H).
- **Precision Fusion 273E / D41 (major)**: #615 StateAbility on **RELOAD** (only on the mech: #605 v6700) -> **3.1 s** (v567 = 3 + 0.1),
  Fusion fire rate +100 % (v38610 += 1), damage -50 % (v37055 -= 0.5), falloff +50 % (v37644 += 0.5), spread -75 % (v70440 chased to 0.75
  in 0.1 s); **cooldown v472 = 7.5 s** from the end (#613) (H; details in kit_dva_mech.md).

### Role: Tank and Initiator (01C7, shared)

- **01C7 runs on both bodies**: 004B #398 Entry -> #396 plays 01C7 unconditionally, and 004B is on the pilot's and the mech's lists (H).
- **+150 health** (1CA9): 01C7 #971 -> #2883 (v74502) -> #2502 -> #1056 -> **#2428 v70468 = 150** -> #1566 -> #970 (hero category; tank
  0850 -> #2701) -> #2700 -> #1370 -> #1567 (**not 6v6**, v66788) -> #1107 / #1111 / #1112 (Role Queue mode ids A2F8 / BD77 / BD6D / BD78)
  -> **#1106 STU_C0A6D3B5 adds v70468 to self** (H nodes, M meanings). **Nothing in that path tells the pilot from the mech** (no pet or
  entity test; v74502 / v45870 have no writer in the graphs read) (H by search). Public: the mech 200 + 150, the pilot 175 with no bonus:
  where the pilot is excluded is **Open** (an engine-side var, or the health type STU_C0A6D3B5 targets: L).
- **Enemy ult from damaging her x0.6** (x0.8 in 6v6), 01C7 #2432 / #2431 -> #1066 (H, Sigma's pass); healing reduction #1308 (0.3 x 0.5 for
  tanks, Sigma) and regeneration #963 / #966 (10 + 5 % after 6 s; DB0 "Health Regeneration Delay" 0B0F 6) — same graph, same nodes (H nodes,
  M meanings, from sigma-map.md).
- **Initiator** (tag 116D; 01C7 #2571 tank branch -> #2556 (116D) -> #2517 -> #2871 / #2954): values **v74464 = 23D0 50** (#2871), **v74465 =
  23D1 1 s** (#2872). **Trigger: Boosters' on-activate** (047E StateAbility #78 4E22B5C4 -> #380 message 1E66 -> **#487 message 2B3B to the
  mech**) -> 01C7 #2953 -> #2954 (hero's sub-role type 0257.114) -> **#2955 only if v74592 = 0** -> **#2857 only below max health** ->
  **#2539 heal 50 over 1 s** (heal type xD52DFCA4, duration field m_95F1FB0B, identifiers 025A / 128A4; effect 7447) and **#2889 v74592
  chased from 257B 5 to 0 = a 5 s internal cooldown** (H). The pilot has no Boosters, so only the mech heals this way (H).
  - Not Initiator's (H by reader): 23D2 0.2 (#2910 / #2906: +20 % speed below 50 % health), 23D3 50 (#2874 -> #2563, a heal on message 0043
  / identifier 04AB), 23D5 0.5 + 23D6 3 (#2875 / #2877 -> #2525 / #2521: +50 % attack speed 3 s after a kill), 23D7 0.0075 (Sharpshooter,
  sojourn-map.md), 23D9 0.4 / 23DA 0.25 (#2881 / #2882 -> #2576), 23C7 0.4 / 23CA 0.25 (Stalwart, #2557 / #2558).

### Hero options (`work_kit\herosettings_dva.json`, file 0126.054; H)

| var | id | option | type | default |
|---|---|---|---|---|
| v8175 | 1FEF | Toggle Defense Matrix | bool | off |
| v12259 | 2FE3 | Hold To Use Boosters | bool | off |
| v49149 | BFFD | Hide Boosters Cancel Text | bool | off |
| v49148 | BFFC | Hide Boosters Timer | bool | off |
| v51472 | C910 | **Light Gun Input**: Primary Fire / Secondary Fire / Both | id | 6161 (Primary) (02B8 #371) |
| v50442 | C50A | Relative Light Gun Aim Sensitivity | float 0.01-5.0 | 1.0 (0B49 #158) |
| v50443 | C50B | Relative Light Gun Gyro Aim Sensitivity | float 0.01-5.0 | 1.0 |
| v53156 | CFA4 | Weapon Mode: Fusion Cannons / Light Gun / Both | id | D0CA (L: which weapon the per-weapon sliders follow) |
| v54020 / 54023 / 54024 / 54028 / 54030 / 54179 | D304-D3A3 | rumble: Primary / Secondary / Ability 1 / Ability 2 / Ultimate / **Call Mech** | float 0-2 | 1.0 (02B8 #185, 026A #471 / #881 / #882) |
| v67805 | 08DD | Camera perspective while in the MEKA: Default / First / Third Person | id | 6161 |

### Nuances a port must not miss

1. **Two entities**: the pilot is the hero (5.5 m/s default mover, 175, eye 1.32 / 0.8 crouched), the mech a pet she controls; while linked
   she rides attached, untargetable, gravity off, her view slaved to the mech's.
2. **Call Mech: 0.2 s cast (no movement), mech spawns, 250 in 3 m with 15 + 3 knockback at 0.9 s, she's driving at 1.95 s**; placement
   ahead (1.5 m) or on her spot (she steps back 1.27 m); fails without cost if no ground within 2 m.
3. **Eject = full heal + 6 m/s back / 7 m/s up + 0.4 s immunity**, 1.0 s after the mech dies (Self-Destruct window) or at Self-Destruct's
   cast; Call Mech locked 1 s, charge from 0; Light Gun already refilled.
4. **Self-Destruct charge is kept for the next mech and grows 5/s while she's a pilot** (M); Self-Destruct's detonation fills Call Mech.
5. **Light Gun: 14, 7/s, 20 rounds, 1.4 s reload (full at 0.91 s), 50 m/s, 0.275 m, no falloff, no spread**, 5 s lifetime.
6. **Initiator only through Boosters (the mech)**: 50 over 1 s, 5 s lockout, not at full health.

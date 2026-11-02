# Sigma data map, from Overwatch's game data (build 154088)

Read 2026-11-02 offline and read-only through TankLib/DataTool 2.25.0.0 (`Online=false`) on the archived CASC copy
`E:\Overwatch Archive\build-154088` (2026-10-06), with Overwatch and Battle.net checked closed (`Get-Process Overwatch, Battle.net`)
before every DataTool and TankLib run (`scripts\owinit.py` and `scripts\extract_sigma.ps1` refuse on their own too). Nothing was
decrypted beyond DataTool's own reading, nothing was run but DataTool / TankLib, nothing went into the repo but text. Confidence:
**H** read in the data (a node, field, value, edge, string), **M** inferred (one link guessed, or a field read by analogy: mover fields
from Juno's `juno-map.md` and Sojourn's `sojourn-map.md`, velocity entries from theirs), **L** guess. Node refs are `graph #uid`; vars
`vNNN`; values by their `.130` id. Public numbers: [sigma.md](sigma.md) (its `D` entries come from this pass). Plan:
[sigma-plan.md](sigma-plan.md). Research folder: `E:\Overwatch Archive\research\sigma\` (paths below are relative to it).

Detail behind this map: `work_kit\kit_sigma.md` (the pipeline and the ability notes), `work_kit\c_<g>.txt` / `decomp_<g>.txt` /
`edges_<g>.txt` (110 decompiled graphs; `scripts\kit\lean.sh <g>` for a lean view), `work\clipmap_sigma\compact_<set>.txt` (every clip
by state), `work\animrefs.txt` (which node plays which alias), `work\ents_sigma_out.txt` (every entity), `work\gamevalues_out.txt`
(every value with its readers), `work\varvalues_out.txt` (values graph **34C3**), `work\lostats_out.txt` (the loadouts' stat rows).
Machine-readable: `gamevalues.json`, `gvpair.json`, `loadout_stats.json`, `work\varvalues.json`, `work\clipmap.json`,
`work_kit\herosettings_sigma.json`.

Scripts: `scripts\` = Sojourn's, copied and retargeted 2026-11-02 (`owinit.py`, `crawl.py`, `crawl_sigma.py`, `hero_sigma.py`,
`ents_sigma.py`, `gamevalues.py`, `lostats.py`, `tl_nodes.py`, `gvpair.py`, `varvalues.py` (G = 34C3), `kit\decomp_out.py`,
`kit\compact.py`, `kit\lean.sh`, `kit\herosettings_sigma.py` (hero 023B.075), `resolve_sigma.py`, `flatten_sigma.py`,
`clipinfo_sigma.py`, `clipmap_sigma.py`, `animrefs_sigma.py`, `owmread_sigma.py`); new: `gvsummary_sigma.py` (the value table with main
/ alt values and readers) and **`clipmaps_sigma.py`** (writes the repo's clip maps from the compacted sets, naming each clip by the
alias that holds it; Sojourn's hand-listed writer is kept aside as `clipmaps_sojourn_copy_unused.py`). The `fx\` and `sounds\` copies
were left out (not run). TankLib scripts run with the kiriko-anim venv's interpreter (`-I`).

Extraction (offline, DataTool 2.25.0.0, logs in `logs\`), into `extract\`: `extract-unlocks "Sigma|skin=Overwatch" --extract-refpose`
-> `Heroes\Sigma` (39 model folders, 358 clips under his models, effects, sounds, entities; 4.1 GB), `extract-hero-voice "Sigma"
--subtitles-with-sounds` -> `HeroVoice\Sigma` (96 stimulus folders, 1293 subtitled lines), `"Sigma|emote=*"` -> `Unlocks\Heroes\Sigma\Emote`
(14 emotes: Applause, Heroic, Inconsequential, Laugh, Maestro, Observations, Relaxation, Listen Up, Mic Drop, Salty, Dans, Magic
Pumpkins, Stepping Stones, Constellation) and `"Sigma|voiceline=*"` -> `\VoiceLine` (43 files). The first skin and voice runs failed
with "Insufficient system resources" (290 MB free with two game clients open): rerun, clean. Hero **`023B.075`** (name 9E9E.07C,
category 0850 = Tank), Gameplay3P **`2AB1.003`**, Gameplay1P **`2AB0.003`**, hero colour (0.486, 0.545, 0.549), m_heroSize 2 (H).

## Ids and graphs
- Gameplay3P 2AB1: statescript **34C3** values, 0033, 004B (shared ult), **1B55** Experimental Barrier, 1869 (ult-charge voice tips),
  **1D02** Kinetic Grasp, **1BBB** Accretion, **1E41** Gravitic Flux, 0043 quick melee, 08B6, 3DCE (ult callout), **51E0** perks;
  weapons manager 0015 with weapon graph **187F** Hyperspheres (H). Played on others: **1A2A** the barrier pet (from 1B55 #671), **1D03**
  the overhealth pool (1D02 #466), **1DBC** a lifted target (1E41 #982), **54A5** Hyper Strike's target (51E0 #393), **1EB9** the
  Accretion rock (18B7), 206C (33E8, a rock cosmetic). 01C7 is the shared role graph (Tank / Stalwart / regeneration). The 56xx / 57xx
  graphs and entities 66FA / 66FC / 69xx / 6Axx belong to another hero the crawl reached (Soldier-like values "Pulse Rifle"), not his (H).
  Graph F9C failed to open (missing in the build).
- Loadouts (H, `lostats_out.txt`): 0289 Kinetic Grasp, 028A Accretion, 028B Gravitic Flux, 028C Hyperspheres, 028D Experimental
  Barrier, 0347 Role: Tank, 0DA0 Hero Stats, 0DA5 Subrole: Stalwart; perks B77 Kinetic Cycle, CE8 Hyper Regeneration, C1F Hyper Strike,
  B92 Levitation. Perk list **004B.177** = [2578, 26B7, 25AF, 2577].134 (minor, minor, major, major).
- Buttons (H): PRIMARY **xD128396E** (Hyperspheres; Flux confirm), SECONDARY **xB42D90B7** (barrier; Flux confirm option), ABILITY 1
  **xCBF0F815** (Grasp), ABILITY 2 **x2ADDA7B1** (Accretion), ULTIMATE **x2DFEDC52** (Flux), RELOAD **x5D2A8B7F** (barrier recall
  option), JUMP **x4F81E1BD** (Levitation). Ability buffers 0.15 s.
- Hero options (`herosettings_sigma.json`, H): **Experimental Barrier UI** (on), **Reload Recalls Experimental Barrier** (off),
  **Toggle Experimental Barrier** (off), **Gravitic Flux Confirmation Input** (Primary / Secondary, default Primary), **Hide Gravitic
  Flux Timer** (off), **Rise During Gravitic Flux With Ability 3** (off), and five aim sensitivities.

## Models (`work\ents_sigma_out.txt`, the skin's `.owentity` files, model bounds from the `.owmdl`)
| | entity | model (skin) | base model | set | notes |
|---|---|---|---|---|---|
| 3P | **2AB1** | **106F7.00C** (274 bones, 9 meshes, 2.14 m tall) | D75A / look EB49 | **0A7E** | the classic skin replaces D75A (H) |
| 1P | **2AB0** | **106F6.00C** | D7C1 / EBBF | **0AA1** | separate arms model (H) |
| Hand spheres | **31A7** (child of both, var 54CE) | **106B4.00C** (48 bones, 0.32 m) | D7DD / EBE7 | **0AD7** | on **hp 000A Weapon_R** in 3P (00C9 in 1P / under v1956), spawned by 187F #78; muzzle **hp 007C** on it (H) |
| Barrier emitters | **30EB** x4 | **FC88.00C** (a 0.14 m orb) | D7C9 / EBC9 | 0AF0 | his **hp 02FD / 02FF / 02FE / 0300** (four bones by his left hand) while held; the barrier's **0032 / 0029 / 0033 / 002A** when sent, scaled 2.5 x the barrier (1B55 #816-#819) (H) |
| Barrier | pet **2D25** (RayCastReceiver, health) | **C015.00C** (6.70 x 4.18 m, 0.73 m deep, 6 bones, hp 0001 / 0029 / 0032 / 002A / 0033) | look DD58 | 0AEF | graph 1A2A (H; size from the rest mesh, M) |
| Hyperspheres | **2D35** (1st), **32B4** (2nd) | effects only | | | contact sets 6CB / 6CC (H) |
| Accretion | **18B7** (graph 1EB9; cosmetic 18BF / 324F) | **C5CA.00C** (about 2 m across, 9 clips in no set) | look E20A | | (H) |
| Flux | cosmetic 31A2 (1E41 #1287), 33E8 (graph 206C) | effects | | | (H) |
| Select / showcase / intro | sets 0DF1 (select alias 0x477), 0F34, 0B0E, 0B28, 0C91, 430C, 0ADF, 0B1C on 106F7 | | | | hero file fields (H; roles L) |

- **Hardpoints on 106F7** (H, model empties): **000A Weapon_R** / **000B Weapon_L** (hands, 1.153 m up, 0.795 m out), **0002 Wrist_L**
  (the barrier / Grasp effects, 1B55 #781 / #792), **01B6 bone_097F** (Accretion's launch, 1.344 m centre), **00C9 bone_0606** (1.344 m),
  **0076 Head** (2.079 m; effect 33CA), 02FD-0300 (the emitter orbs), 02A8 / 02A9, 00A3 / 00A4.
- **Mover (2AB1, STUCharacterMoverComponent): field-for-field Juno's 5106 (all 53 fields) but four: height m_1972C687 2.5 (2.0),
  radius m_7A70E3E3 0.7 (0.5), m_915FF368 0.7 (0.5), crouch height m_03E04168 2.05 (1.25)** (H). So: 5.5 / 4.95 m/s, crouch 3.0,
  accel 35, air 3.5, friction 15, jump 5.72 m/s, gravity 17.5, terminal 30, step 0.5, slope 45, all the hero default. **His float is the
  animation only**: no hover node, no mover override on foot (H by search). Default falling-damage component (m_43A3CE29 0.49), no fall
  node (H by search).

## Clip map (`work\clipmap_sigma\`; repo `tools/clipmaps/sigma.txt` 147, `sigma_1p.txt` 105, `sigma_weapon.txt` 41, `sigma_props.txt` 12)
| State | 3P (0A7E) | 1P (0AA1) | spheres (0AD7) |
|---|---|---|---|
| Stand | **6E65** (2.3 s; stance df18 default) + layers | 6F60 (move tree df18 cat9) | stance (alias 85) |
| Move | **70DB** (1.23 s; **one clip for every direction**, df18 cat5) | **6F5D** (1.33 s) | |
| Jump | 8-way take-off 7232.. (0.2 s) -> apex 700F -> fall 7374.. (0.47 s); still: 700D / 700E | 7094 -> 7095 | |
| Hyperspheres | alias 0xD3 / 0xD5 / 0xD6 = **7193 / 6FC7 / 7026** pitch triple (1.93 s), cycled v59 0-1-2 (187F #217) | 6F5E / 70F6 / 7114 (2.13 s) + additives 7101 / 7100 / 7102 | fire_a / b / c |
| Barrier | 0x40: send cat 1F8 **73D0 / 6E84 / 73C8** (1.0 s), hold **73D1 / 6E85 / 73C9** (0.83 s); deploy 0x1B7 **6F6A** + 0x634 738E | 0x40 7245 / 7135 / 6F68 / 6F67; recall 0x1B6 6F65 .. 7183 | barrier |
| Kinetic Grasp | 0x60A start **7106 / 6F6F / 70FC** (0.5 s); hold 0x609 **7103 / 6F6E / 70FB** (2.27 s) | 6FE4; 6FCC | |
| Accretion | 0x608 gather **7225 / 6F6C / 7194** (0.77 s); 0x60E throw **7226 / 6F6D / 7195** (0.5 s) | 6F61 -> 7201; 701E | accretion_cast / throw |
| Gravitic Flux | 0x1B8 start **6FB8** (0.47); 0x60F aim **6FBA** (2.0); 0x691 cast **6FBB** (0.87) + 0x692 739E; 0x175 / 0x689 lift **6FBC** (2.67) + 0x5A7 74AB; 0x1B9 slam **7253** (0.47) | 6F80; 703B; 7489; 703D; 71D4 | flux_* |
| Quick melee | 0xD **7248 / 6F89 / 7244** (1.2 s) | 6FCD | melee |
| Hello / select | 7462 + 7470; select 0x477 B1E6 / B1EA | 7463 + 7471 | |

Played by (H, `work\animrefs.txt`): Hyperspheres 187F #206 / #209 / #216 (0xD5 / 0xD6 / 0xD3); barrier 1B55 #812 / #861 (0x40), #879
(0x1B7), #885 / #920 / #921 (0x1B6), #1000 (0x634); Grasp 1D02 #398 (0x60A), #531 (0x64A), #746 / #748 (0x609); Accretion 1BBB #627
(0x608), #641 (0x60E); Flux 1E41 #1020 (0x1B8), #1274 (0x60F), #1089 / #1105 / #1102 (0x691 / 0x693 / 0x692), #1095 / #1096 / #1097
(0x175 / 0x689 / 0x5A7), #1037 / #1050 (0x1B9 / 0x68B); quick melee 0043 #117; Levitation 51E0 #408 (0xA66, in no set). Not
extracted: BAC9, 74B8 and the intro set 430C's 19EFE-1A129 (sets 0C91 / 0B28 / 430C). 31 3P clips are extracted but in none of the
sets read (listed at the end of `sigma.txt`).

## The kit, node by node (numbers from values graph **34C3** and the graphs that read them)

### Hyperspheres (187F)
- PRIMARY #98 (buffer 0.15) -> StateStack #138: **volley #175** (projectile **2D35**) -> **wait 0.2 s** (#199) -> **volley #204** (32B4);
  then **v173 = v581 = 1.3 s** (0479 "Reload Time") chased to 0 at the attack-speed scalar v21541 (#99 / #102): **one volley a 1.5 s**
  (H).
- Each sphere: **speed v6368 = 50** (0478), **lifetime v18409 = v7056 / v6368**, v7056 = **range 21** (1AAB) + 0.7 x his scale (#240 /
  #241): **0.43 s, about 21.7 m, then it bursts** (H formula, M meaning). Direct part (m_6396149F): **v2577 = 20** (0474), size **0.2**
  (28DA) **+ v54935** (the global 28A4 0.12, M) = 0.32 m, environment 0.075 (H). Explosion (m_D2D11CE9): **v11863 = 40** (0477), rings
  **v31805 1.0 m** (factor 1) and **v31806 2.85 m** (factor v33006 = 1AF5 0.5) (H values, M that the factor is the damage at the ring);
  knockback horizontal **v29209 = -0.75** (1B64, inward), vertical **v22029 = 1.0** (1B65) with knockback rings 0.5 m (0.8) / 2.8 m
  (0.2) (H values, M reading). Motion m_599AB8D7 = v7389 = 5.5 (L: a gravity, as Accretion's 18 is). Muzzle hp **007C** on the spheres
  entity v49 (H). Effect 2CE0 per volley; voice B99 (H).
- The three fire anims cycle v59 0 -> 1 -> 2 (#210 / #217), each played at 1.2 s / attack speed (H).
- Stadium only: 25F9 (a third half-damage sphere 0.4 s later, #417-#419), 2605 (+15 % range), 25F5 (Accretion cooldown message) (H by
  gating).

### Experimental Barrier (1B55 on him, 1A2A on the pet 2D25)
- SECONDARY StateAbility #1062 (cooldown v61). On first use the pet **2D25** spawns (#669 StatePet; 6866 with the Stadium 2657) and runs
  **1A2A** with (health v586, owner, speed **v1002 = 20** (0F04), max **v587 = 650** (048F)) (#671) (H).
- His state v19777 / the pet's v59: **1 held** (with him), **2 sent** (moving), **3 stopped**, **4 recalled** (1A2A #624; H). While the
  button is held it is sent (#717 / #1050), released it stops (#1017 / #1051); **Toggle Experimental Barrier** (v23619) makes one press
  send and the next stop (#1024 / #1026 / #1030); a press while it is out **recalls** it (#721 / #916), as **Reload** does with **Reload
  Recalls Experimental Barrier** (v23583, #1014 / #1033) (H nodes, M flow).
- **Sent:** STU_EF4C12FB #636 moves it along his aim (v40.v2083) at **v1002 = 20 m/s**, fields 1.75, 2.0, **9999** (max distance), 4.5
  (L meanings: a hover height and the size?); a **0.9 m ray ahead** (#638, filter 11D) stops it at a wall (v2413); state 3 holds it
  (H nodes, M meanings). While deploying (0.05 s, #834) it tracks a 3.2 m ray from him (#740) (M).
- **Recall:** v61 = **v38941 = 1 s** (0493) (#1041) (H). **Destroyed:** v472 = **1BC1 5 s** (#1099 / #1100) (H). On death / hack it goes
  home (W; 1A2A #468 owner checks: M).
- **Health pool** StateHealthPool #607 = v587 (650). **Regeneration:** after **2 s** (#658) **while held** (state 1, #660), StateModifyHealth
  #664 with **v658 = 0490 0.122** in percent mode (x60671788): **0.122 x 650 = 79.3/s** (34C3 #15 computes the same: 0.122 x max) (H).
  Low-health effects at 50 % / 25 % (#462 / #463), voice BA0 / BA1 at < 34 % (#973) (H).
- **Hits on it** (message 0011, #685): **2 % of the damage to his ult** (#683 STU_008D30B1 `v337 x 0.02`, M that 008D30B1 is ult
  charge, as Brigitte's barrier) and the damage to his perk progress (#684) (M).
- **Hyper Regeneration** (26B7): message 2961 heals it by **amount x 0.3** (#1033-#1039, v39266 = 0.3) (H).
- Emitter orbs: four 30EB at his hp 02FD-0300 when held; at the barrier's 0029 / 0032 / 0033 / 002A when sent, scale 2.5 x the barrier
  (#816-#819) (H).
- Anims: 0x40 cat 82 hold / cat 1F8 send (#812 / #861), deploy 0x1B7 (#879, 0.53 s) with 0x634; recall 0x1B6 by category (H).

### Kinetic Grasp (1D02, 1D03)
- ABILITY 1 StateAbility #156 (buffer 0.15, cooldown v61). **Duration v567 = 04AD 2 s** (#489 chase) with **StateDeflectProjectiles
  #274**: a box **(-1, -1.6, 0.4) -> (1, 1.1, 2.6) x his scale** (2 m wide, 2.7 m tall, from 0.4 to 2.6 m ahead, about his eye: M
  origin), each absorbed hit adds its damage (v6584 + v51856) to **v21312** (#464) and **5 % of it to his ult** (#432, M) (H).
- **At the end** (#593 onFinished): **v26369 = v21312 x v39266 (0.6)** -> ActionPlayScript **1D03** (#466) with max **v11218 = 400**: a
  health pool (id 5847) of **min(400 - pool, amount)** (1D03 #201-#207) (H). **No overhealth if it is cut short** (the onAbort branch
  #554 skips #593: M). **Decay: after v38833 = 1D53 3 s** (#546) **5 a second** (#545, absolute mode x00943466, M rate) (H nodes).
- **Cooldown 12 s** (0494) written at the end, at least 0.2 s after (#424 / #425 / #242) (H).
- Kinetic Cycle (2578): at the end, **message 2623 to ABILITY 2 with v21312 x 0.02** (#735-#738): Accretion's cooldown -2 s per 100
  absorbed (Accretion 1BBB #865-#867 takes it) (H).
- Anims 0x60A (#398, 0.5 s) -> 0x64A (#531); hold 0x609 (#746 / #748, its 2 s); voice B9A / B9B, BB4 (H).

### Accretion (1BBB)
- ABILITY 2 StateAbility #447 (buffer 0.15). **Cast v670 = 0.65 s** (#535, anim 0x608 at 0.65 s), with **v23184 += 1B69 0.75** (-75 %
  move speed, #651 / #681) and the jump off (x5C571F43 / x292F571E = 0, #651) (H nodes, M meanings).
- Then **volley #809**: projectile **18B7**, **speed v6368 = 0499 37**, **gravity v7389 = 18** (#869), lifetime 30 s, hero size **v76077
  = 265F 0.9**, environment 0.3, from **hp 01B6** (H). Impact (m_6396149F) **v476 = 80** (049F), knockback **6 / 3** (1B67 / 1B68;
  v29209 / v22029) (H). Explosion (m_D2D11CE9) **v11863 = 40** (0495), rings **1.5 m** (0496) and **2.5 m** (0497), both factor 1;
  knockback v69969 / v69970 (M: 1B6A 5 / 1B6B 3); m_792F0B4D 20 (H values).
- **Knockdown** (graph 224E on the target, #585): **v13550 = max(1.1, min(v567, distance x 0.11))** (#590); v567 is set only by the
  removed Massive Impact (257A: 3.3, #871), so **1.1 s** (04A0) (H).
- **Cooldown 10 s** (0498) from #669: a chase from **0.45 s** to 0 after the volley, then v61 = 10 (#570) (H nodes, M timing).
- **+25 ult** on a hit (#683 STU_008D30B1 25, M). Anims 0x608 (#627), 0x60E throw (#641) (H).

### Gravitic Flux (1E41, 1DBC)
- ULTIMATE StateAbility #477 (cost v338 = 2300). Phases v59 (#957): **1 aim** (#1268 target reticle: **35 m**, a cone 0.1745 rad;
  **v567 = 04AE 5 s** chase #1271, then it fires itself #1294); confirm on **PRIMARY** (#1269) or **SECONDARY** by the option v51476
  (#1296 / #1297) (H). **2 cast**: **v670 = 1.0 s** (#961 / #1103, anims 0x691 / 0x693 / 0x692) with StateTrackTargets #1292 over a
  cylinder **radius v11861 = 1867 7.5 m** (alt 5.0), **height v1165 = 1DFB 15 m**, its base **5 m below** the aimed point v2478, a line
  of sight from v2478 + 1 m (#977 m_47AB1887) (H). **3 lift**: **v68035 = 1868 2.5 s** (#987, anims 0x175 -> 0x689 -> 0x5A7); each
  target runs **1DBC** (#982) with (him, **50**, **0.5**, **0.4**). **4 slam**: 0.5 s (#1039, anims 0x1B9 -> 0x68B) (H).
- **His flight** (#976 / #977): gravity x5B83ECB5 = 0 and a flying mover (2.3 m tall, 0.7 m: his size; speeds 5.5 / 3.0; L fields) with
  **+30 % speed** (v27756 = 1B66 0.3; **7.15 m/s**, W's), **-50 % knockback** (x0A04B51B -0.5, #1071), rise / fall on jump / crouch;
  option **Rise During Gravitic Flux With Ability 3** (v51849, #1304: hold ULTIMATE = jump) (H nodes, M fields). Physics pushes for
  props (#1024 / #1025 / #1113) (H).
- **1DBC (a lifted target):** damage **v476 = 50** at the lift with knockback -1 (pulled in) (#575), **held still for v74536 = 23F2 0.4 s**
  (#586: jump, abilities, movement off), gravity **x0.1** and speed x0.55 (#571 / #573), pulled up **(0, 18, 0)** then (0, 5, 0) near
  the end (#615 / #604); the lift lasts **v21244 = 2.0 s** (+0.2) (#597 / #599); at the end a ray finds the ground within **5 m**
  (#644) and **#577 deals max health x v71023 (1AAD 0.5)** with m_FB3F6CA9 = 1 (no damage modifiers) (H); then **high gravity**
  (#581): gravity **x22**, speed **x0.5** (1DFA -0.5) for **v21245 = 1DFC 0.6 s** (H values, M meanings).

### Perks (51E0) and the rest
- **Hyper Strike** (25AF): v181 = 5, v11 counts direct hits (id 9C8B, #398-#402); at 5 the next quick melee hit (message 13EC, #394)
  runs #392: knockback **v11272 = 14** along his aim + 0.3 up (horizontal 14), and graph **54A5** on the target (the lift; its +50 is
  not read) (H nodes, M meaning).
- **Levitation** (2577): JUMP (#370) in the air (v417), **0.2 s** after leaving the ground (#375), once a landing (v1843): **1.75 s**
  (#383) of STU_A9C4FF83 **(0, 25, 0), cap 3** (#382) and xDF7A3726 0.35 (#369), anim 0xA66 (in no set), v23184 += 0.1 (L); ends on
  release (H nodes, M fields).
- **Hyper Regeneration** (26B7): his Hypersphere damage to heroes (id 5916, #431-#440) sums in v21312 and goes to the barrier as
  message 2961 (#436) (H).
- **Kinetic Cycle** (2578): in 1D02 (above).
- Role / sub-role (01C7, shared): +150 health (#1106), enemy ult x0.6 (x0.8 under v66788) (#1066), healing reduction 0.3 x 0.5 for 3 s
  (#1308 / #881), regeneration 10 + 5 % after 6 s (#963 / #966), Stalwart (tag 116B, #2570 -> #2557): knockback x(1 - 0.4) (#969),
  slows x(1 - 0.4) (#2866) (H nodes, M meanings).

## Nuances a port must not miss
1. **600 total (200 + 150 Role Queue + 250 shields)**, a **2.5 m x 1.4 m** body (crouched 2.05 m), the stock walk: his float is the
   animation, and **one move clip** serves every direction.
2. **Tank:** enemies get **40 % less ult charge** from him; **healing reduction 15 %** on him; **Stalwart: 40 % knockback and 40 % slow
   resistance**; **no crit reduction** (full headshots).
3. **Hyperspheres: two 50 m/s spheres 0.2 s apart every 1.5 s**, 20 direct + 40 burst (full to 1 m, half at 2.85 m), bursting after
   0.43 s (21.7 m), an inward pull, no crits, no self-damage.
4. **The barrier is an entity** (650, 79.3/s regen while back with him after 2 s; 20 m/s along his aim, no range, stops at walls;
   recall 1 s, broken 5 s), toggle / reload-recall options; 2 % of what it absorbs to his ult.
5. **Grasp: 2 s box, 60 % of absorbed damage as overhealth (max 400) at the end only**, decaying after 3 s; 12 s from the end;
   projectiles only.
6. **Accretion: 0.65 s cast at -75 % speed**, 37 m/s with 18 m/s² gravity, 80 + 40, **knockdown 1.1 s** on the one it hits, cooldown 10
   s from 0.45 s after the throw.
7. **Flux: 5 s aim (35 m), 1 s cast, 7.5 m x 15 m cylinder, 2.5 s lift (50 damage, held 0.4 s), slam 50 % of max health ignoring
   modifiers, 0.6 s heavy gravity**; he flies at 7.15 m/s with half knockback.
8. Value names mislead: 0479 "Hypersphere Reload Time" = the 1.3 s between volleys; 0490 "Barrier Recharge Rate" = a share of max per
   second; 02FF 25 in Accretion's "max falloff" row is a shared value. Read graphs, not rows.

## Not found
The +50 damage and 1 s lift of Hyper Strike (graph 54A5, loadout 02F5: not read); Levitation's exact rise speed (the fields' meanings);
the overhealth decay rate's unit (5 read as per second, M); the explosion falloff rule between two rings (factor at the ring, M);
which mode flag v66788 is (6v6: M); v69403 0.25's reader condition (#1280); the barrier mover's 1.75 / 2.0 / 4.5 fields; the
hypersphere motion field 5.5; Flux's flying-mover fields one by one; the eight-way slot directions on his body (not measured); the 31
3P clips in no set read; the intro set's clips (not extracted); his voice set 1E5.05F, sounds, effects and emotes (extracted, not mapped).

# Ana data map, from Overwatch's game data (build 154088)

Read 2026-10-29 offline and read-only through TankLib/DataTool 2.25.0.0 (`Online=false`) on `build-154088` (2026-10-06), with
Overwatch and Battle.net checked closed (`Get-Process Overwatch, Battle.net`) before every DataTool and TankLib run
(`scripts\owinit.py` and `scripts\extract_ana.ps1` refuse otherwise). Nothing was decrypted beyond DataTool's own reading,
nothing was run but DataTool / TankLib, nothing went into the repo. Confidence: **H** read in the data (a node, field, value,
edge, string), **M** inferred (one link guessed or read by analogy with Mercy / Lifeweaver / Lúcio), **L** guess. Public
numbers: [ana.md](ana.md); the build plan: [ana-plan.md](ana-plan.md). Node refs are `graph #uid`; vars `vNNN`; values by
their `.130` id.

Detail behind this map (each finding there has its node ids): `work_kit\kit_rifle.md` (Biotic Rifle, Take Aim, reload, weapon
manager, quick melee, hero settings), `work_kit\kit_dart_grenade.md` (Sleep Dart, the sleep, Biotic Grenade, Local
Anesthetic, Biotic Bounce), `work_kit\kit_nano_perks.md` (Nano Boost, ult cost, perks, Tactician, health), `work\assets.md` +
`work\clipmap.md` / `clipmap.json` (models, hardpoints, every clip by state), `work\voice_fx.md` (voice, effects, sound banks),
`work\public_sources.md` (BZ / W notes). Dumps: `dump` (hero, entities, sets), `dump_ss` (graphs, entities), `dump_lo`
(loadouts), `dump_misc` (looks), `graphs\` (reflected graphs), `resolved\` (sets, clip headers, `statescript.json`),
`work_kit\decomp_<g>.txt` + `edges_<g>.txt` (decompiled graphs), `work_kit\dart_grenade\` and `work_kit\nano\` (status graphs
224E, 0BDE, 0C71, 0B99, 0C72, 0B9F, 01C7 ... decompiled this pass), `work_voice\` (hit and status graphs for the voice
search). Machine-readable: `models.json`, `gamevalues.json`, `gvpair.json`, `loadout_stats.json`, `voice.json`,
`work\clipmap.json`, `work\graph_effects.json`, `work\fx_sounds_timed.json` (use this one: `fx_sounds.json` has every start /
duration / hardpoint at 0, a bug inherited from Lifeweaver's script), `work_kit\herosettings_ana.json`. Scripts: `scripts\`
(Lifeweaver's copies, plus new Ana ones: `ents_ana.py`, `looks_ana.py`, `lookhp_ana.py`, `bonepos_ana.py`, `pose_ana.py`,
`rootyaw_ana.py`, `clipmap_ana.py`, `clipmap_build_ana.py`, `voiceset_ana.py`, `stimscan2_ana.py`, `decomp_voice_ana.py`,
`vctx_ana.py`, `voice_ana.py`, `fx_graph_ana.py`, `fx_sounds2_ana.py`, `kit\herosettings_ana.py`, `kit\decomp_out.py`,
`kit\strings07c.py`, `kit\nano_tl*.py`, `kit\rifle_*.py`). TankLib scripts run with the kiriko-anim venv's interpreter (`-I`),
which has pythonnet.

Extraction (offline, DataTool 2.25.0.0, logs in `logs\`), into `extract\` here: `extract-unlocks "Ana|skin=Overwatch"
--extract-refpose` -> `Heroes\Ana` (44 models, 289 clips, 84 effects with 123 sound banks / 879 sound files, 41 entities; 3 min),
`extract-hero-voice "Ana" --subtitles-with-sounds` -> `HeroVoice\Ana` (1177 .ogg), `extract-unlocks "Ana|emote=*"` /
`"Ana|voiceline=*"` -> `Unlocks\Heroes\Ana\Emote` (16) and `\VoiceLine` (41 .ogg). Hero **`013B.075`**. Clip ids are .006
indexes (`3C2B` = `000000003C2B.006`), files under `extract\Heroes\Ana\Skin\Overwatch\Overwatch\Models\<model>\Animations\
<prio>\<group>\` (3P `10562.00C`, 1P `10561.00C`, rifle `10B96.00C`).

## Models (`work\assets.md`, `ents_ana_out.txt`, `looks_ana_out.txt`, `models.json`)
The entities name base models / looks; the Overwatch skin swaps them (DataTool's `.owentity` names the swapped ones, H):
| | entity | model (base -> skin) | look (base -> skin) | set |
|---|---|---|---|---|
| 3P | **1424** (hero `m_gameplayEntity`; mover, health, voice set 012A, weapons) | 3686 -> **10562.00C** (505 bones, 253 cloth) | 45C5 -> **13124** | **05F5** |
| 1P | **1425** (child of 1424 via STUFirstPersonComponent) | 36AB -> **10561.00C** (97 bones: arms, torso, camera; no head or legs) | 45EE -> **13123** | **05F9** |
| Biotic Rifle | **159C** (on hp **00C9** of both 1424 and 1425; 0B94 #199) | 34E6 -> **10B96** (20 bones) | 4346 -> 13A93 | **05FC** |
| Sleep Dart pistol | **15CA** (0BDD #105, on hp 0143 / 00A6 during the cast) | 3562 -> **1175E** (3 bones) | 4457 -> 14EF3 | - |
| Rifle shot, unscoped | **15B7** (0B94 volley #17) | 37B1 -> 11741 | 4729 -> 14ED6 | - |
| Rifle shot, scoped | **1615** (0B94 #167; hitscan tracer, effect 1A80) | none | | - |
| Sleep dart | **15E8** (0BDD #108); Local Anesthetic's **7773** (#297) | 3678 -> **11740** (1 bone; idle anim 3959) | 45B7 -> 14ED5 | - |
| Biotic Grenade | **1426** (0B97 #21; graphs 5341 + 5791) | 3563 -> **11763** (2 bones) | 4459 -> 14EF8 | **0074** (clip AE8) |
| Nano syringe | 15E9 (0B9E #93; a visual only: effects 1A5E / 1A39 / 1A5F) | none | | - |
| Vial (rifle ammo) | 1607 (rifle hp 017E; Ana's hp 000B during reload, M) | 3677 -> 1173F | 45B5 -> 14ED4 | - |
| Chambered dart | 1605 (rifle hp 017D) | 3678 -> 11740 | 45B7 -> 14ED5 | - |
| Teacup / teabag / saucer | 1611 (hp 00C9; showcase 000A) / 1610 (000B) / 1609 (00A5) | 372A / 373A / 3730 | 4688 / 46A2 / 4693 | - |
| Showcase / gallery / intro / preview emote | **3A7E** / 14AB / 15D8 / **15F1** (+ 490D) | 10562 | 13124 | 0C85 / 0600 / 05FF / 0639 (+ 42EA) |
| Preview rifle | 15DD (on 00C9 in the menus) | 10B96 | 13A93 | - |

- **No look holds a hardpoint table** (H: all 12 look dumps have `m_hardPoints` null): Ana is Mercy's case (older models),
  not Lifeweaver's: **the model's own STU table is the one** (`models.json` `hardpoints`: 3P 58, 1P 49, rifle 7, pistol 2).
- **1P is a separate model** (H), as Mercy's: entity 1425, model 10561, its own look and set 05F9, one LOD, no cloth. Camera
  bone_007D at 1.573 m in both models; 1P adds 000F / 051F on bone_007E (0.428 m ahead), 0491, 0538, 0076 / 0077.
- **Hardpoints the graphs use** (rest T-pose places from `lookhp_10562.txt` / `lookhp_10561.txt`; +X = her left, +Z forward):
  - **00C9** = bone_0606 under the pelvis (0, 1.066, -0.003): **the rifle's carrier**, keyed by every clip (3P idle
    (-0.152, 1.046, 0.093); 1P idle 0.135 m below the camera, right of centre) (H). Also the teacup's slot.
  - **00A6** = Weapon_R in 3P (the root in 1P), **0143** = Weapon_R in both, **00A5** = Weapon_L, 000A / 000B = bones under
    Weapon_R / Weapon_L (vial, teabag, showcase cup) (H).
  - **The pistol is in her right hand in both views** (H places, M reading: 0BDD #105 names `m_E591AF01` 0143 and
    `m_9E54CED9` 00A6; 1P clip 3BBC puts Weapon_R before the camera, 3P clip 3C2C Weapon_R at 1.357 m): so E591AF01 = 1P,
    9E54CED9 = 3P (M). The 1P entity's own child slot says 00A5 (left): not what 0BDD uses (L).
  - **Rifle muzzle = its 0006** on bone_009F, (0, 0.036, 0.900) in rifle space (H: volleys #17 / #167 and effects 19FE / 1A92).
    Rifle 017D chambered dart, 017E vial (rotated -30° about X), 0170 (L: sight). The rifle has no 000A / 000B; 0B94 #199's two
    arm IK lists aim Wrist_R at hp 000A and Wrist_L at hp 000B (weights df 0x16E / 0x16D), so the grips are her own points (L).
  - **Pistol muzzle = its 0006** (0, 0.101, 0.165) (H). **Grenade: 0002 = Wrist_L** (0.419, 1.073, -0.026) (H). **Nano
    Boost: 0078** on her left forearm (0.398, 1.176, -0.043), aimed at the ally's **0001** (Spine_3 + offset, 1.306 m) (H).
    Head 0012 1.570 m; 01E6 (right clavicle) the showcase emitter.
- **Skeleton (H):** humanoid with a face rig; rest **Pelvis 1.066, Head 1.529, eyes 1.568, camera 1.573 m** (Mercy 1.128 /
  1.655). Her **gameplay stance is low** (pelvis key 0.83 m in the idle stands, 0.91 running, 0.71 scoped, 0.40 crouched;
  head about 1.25-1.29 m). Mover height 2.0, radius 0.5, crouch 1.25 (the hero defaults).
- **Geosets (H):** none (every submesh -1): nothing hidden by a look. 3P LOD 2 (finest) 18516 v, 6 submeshes / materials.
- **Cloth (H counts, M roles):** two Domino pieces: **275 points**, 41 drivers (spine, arms, hips, legs, a chain down her
  back bone_00A3-00A6): the long robe; **140 points**, 8 drivers (Spine_3, arms, clavicles): the shoulder cape / hood. No cloth
  in 1P or props.
- Other entities: 5162, 6121, 612A, 5B18 (11763: L Biotic Bounce), 1CBB (the pistol again) with no dumped reference (L). Her
  shared graphs also reach entity **66FA / 66FC** (model 045D, sets 2171 / 2172, voice 02B9, weapon 56D6: Heavy Pulse Rifle,
  Cyber Frag ...): **not Ana's kit** (another playable kit, L which).

## Clip map (`work\clipmap.md`, `clipmap.json`)
30 fps (quick melee 60) (H). One weapon: **no weapon split** (no df 0xE4). **Scoped = df 0xE5 cat 0x81** (0B94 stacks
category 0x81 on v1273, #516 / #518: H nodes, M that df 0xE5 carries v1273); df 0x18 locomotion (5 run, 6 jump, 0x65 / 0x66
crouch, 0x1ED crouched, 9 still). "u" unscoped, "s" scoped. Pitch triples are 1D blends at -1 / 0 / +1 (M: aim pitch).
| State | 3P (05F5) | 1P (05F9) | How it plays |
|---|---|---|---|
| Idle | u **3C2B / 3C2A** (0.9 s turn-in-place stands, df 0xB7, root yaw 0 <-> -120°) + 3B7D spine add + 3C44; s **3C5E / 3C5D** + 3B81 + 3C44 | u **3B96** + adds 3B9E / 3B85 / 3B86, layer 3B87; s hold **3B92** + 3B95 | rifle: **3C59** (1.0 s) (H) |
| Run 8-way | u 0.6 s **3B71** F, 3BD3 FR, 3BC9 R, 3BCE BR, 3BCA B, 3BCD BL, 3BC8 L, 3BCF FL + turns 3BF5 / 3BF6 / 3BD8 / 3BD9 + leans; s 1.0 s **3D78** F, 3D85, 3D80, 3D81, 3D7D, 3D83, 3D82, 3D84 + 3B81 | u **3B9D** (all directions); s 3B92 | slots by field as on Kiriko (H); stop 3C01 / 3C05 / 3C06 / 3C07 (4 slots, L) |
| Crouch | u idle **3C20 / 3C31**, walk 3C22 F, 3C49, 3C46, 3C5C, 3C48, 3C5B, 3C45, 3C4A + 3C42; s idle 3C9C / 3C9D, walk 3C6E ... 3C72 + 3C79 | none | (H) |
| Jump / land | u take-off **3B6D** / 3C17 / 3C0F / 3C13 -> air **3B6E** / 3C18 / 3C10 / 3C14 + 3C1E; s 3CF3 ... -> 3CF2 ... + 3CF9; land u **3B70** / 3C1A, s 3CF4, run 3D79 | s 3B92; none unscoped | 4 slots, directions L |
| Rifle fire | additive layer weighted **df 0x102**: **3C3F** (0.8 s) | **3B8D** / **3C9A** (1.267 s) | rifle 3CEB (3P) / 3C9B (1P), 3C55 missing; no StateAnim plays it in 0B94 (M fire, L which is scoped) |
| Scope in / out | none (cross-fade, blend 6.0) | in **3B84** (0.167 s, time df 0x6D) -> hold 3B92; out 3B93 -> 3B94 (M) | 0B94 #81 / #77 categories 003D / 003E (H) |
| Reload (0B94 #33: 0x004 empty, 0x084 ammo left) | **3CE3** (1.5 s) standing, **3CA0** crouched | **3B99** / **3C98** (1.5 s) + layers | rifle 3CE4 (3C5A missing), 3C99 / 3D87 (H) |
| Sleep Dart (0BDD #115, 0x47B, 1.0 s) | pitch **3C35 / 3C2C / 3C36** (0.9 s: draw, shot, holster) + 1028 | **3BBC** (1.0 s) | pistol in the right hand (H) |
| Biotic Grenade (0B97 #89, 0x479) | pitch **3C39 / 3BBD / 3C38** (0.833 s) + 1028 | **3BCB** (0.867 s) | thrown from Wrist_L (H) |
| Nano Boost (0B9E #141, 0x175) | pitch **3CEA / 3BBF / 3CE9** (0.8 s) | **3BDB** (0.767 s) + 3D61 | from hp 0078 (H) |
| Quick melee (0043 #117 0x00D, #145 0x22A) | pitch **3C1C / 3BC3 / 3C1B** (0.8 s, 60 fps) | **3B98** + a 0x140 layer | over 0.76 s (H) |
| Asleep (224E #181 / #177 / #179: 0x064 / 0x065 / 0x066) | **3C93** fall (1.0 s) / **3C92** loop / **3C94** wake (1.333 s) | 3BFA / 3BFB / 3BFC | the shared knock-down aliases: every hero has them; no Ana-only sleep pose (H) |
| Hit / stun (0x070) | **3C76** (0.433 s) | 3BF9 | (H) |
| Draw on spawn (0015 #34, 0x0A9, 0.5 s) | not mapped | not mapped | the alias is played (H); its clips weren't looked up (L) |
| Showcase / select (0C85) | **BACC** (8.67 s) -> BC6F, BC6E, BC70, BC6D, then random | - | preview emote 0639: Heroic 3C3B (H) |
| Grenade (0074) | AE8 | - | spin fields on 1426 too (L) |

**Missing from the extract (22, H):** rifle 3C55 / 3C5A; 3C3B (present in the Heroic emote extract); the intro's eight
(3C03, 3C3C, 3CEC, 4804, 86ED, DFA1, EB6F, F69A); 42EA's 19EA5-19EAB and 1A129; 0EC0's BF7C / BF7D; the hero's BCE0 / BF7F.
Everything 05F5 / 05F9 / 0C85 / 0074 / 0F27 plays is extracted.

## Abilities (numbers from her values graph **4BE4** and the graphs that read them)
Graphs on Gameplay3P 1424 (H): weapon manager **0015** with weapon **0B94** (Biotic Rifle + Take Aim; the only weapon),
**4BE4** values (+ shared 3361), **0BDD** Sleep Dart (+ 6123), **0B97** Biotic Grenade (grenade 1426 runs 5341 / 5791), **0B9E**
Nano Boost (+ 545C voice picks, 605E ability flag), **51F0** perks, 0043 quick melee, 004B shared ult (-> 01C7 role / sub-role /
regen, 01CA, 01CB, 01CF, 01D8), 0033 health bars, 08B6 damage feedback, 0C6B hints and friendly aim assist, **5016** the Mythic
weapon "Midnight Sun" level-ups (+ 51C1; not perks). Status graphs run on the target (H): rifle hit statuses **0266** (0B96) /
**027B** (0C67) -> script **0B95** (variant 541F); sleep **026F** -> **0BDE** -> shared CC **224E**; grenade **0267** -> **0C71** ->
**0B99** (allies) / **0C72** (enemies); nano **0269** -> **0B9F**; Local Anesthetic **6123**.
Buttons (H): PRIMARY **xD128396E** (0B94 #16, buffer 0.167 s), SECONDARY **xB42D90B7** (#187 hold / #185 toggle), ABILITY 1
**xCBF0F815** (0BDD #84), ABILITY 2 **x2ADDA7B1** (0B97 #84), ULTIMATE **x2DFEDC52** (0B9E #92, buffer 0.15 s), RELOAD
x5D2A8B7F (#10), QUICK MELEE xD19BBFFC (0043 #127), JUMP x4F81E1BD. **Health 250** (0BA7, 4BE4 #3 -> 3361; shield slot 8FEC
empty, no armor), melee 40 (02D0), 5.5 m/s (202B), **ult cost 2300** (0ADD "Ultimate Charge Max" `m_3E5360A1`, passed by 1424
as 0152.01C = v338 to 004B; its `m_1B39F279` 3000 = the slider max, L), perk XP **2025 then 3035 more** (4BE4 #55: 12B2 / 1291
= W's 2025 / 5060). Hero options (file 0126.054, `herosettings_ana.json`, H): Toggle Zoom v8173 **off**, Relative Aim
Sensitivity While Zoomed v8174 **0.3** (gyro v24100 0.3, Gyro Only In Ironsights v27204 off), Recoil Recovery Aim
Compensation v10563 **on**, Friendly Aim Assist v13274 1.0 / threshold v72648 0.99, Weapon Mode v53548 (reticle), vibration
v54139 / v54140, camera while scoping in 3P v70565, **Nano Boost Requires Target Confirmation v29052 off**, **Nano Boost
Sensitivity v9602 1.0** (0.1-1). Lobby reads (L names): v10541 no unscoped fire, v10542 no scope, v10265 clip scalar.
- **Biotic Rifle, unscoped** (0B94 #16 -> #143 -> #210 -> #62 -> volley **#17**; H unless said): PRIMARY held fires
  automatically after each recovery. Projectile **15B7** at **125 m/s** (2934, #390), lifetime **5 s**, **no motions = no
  gravity** (M), no spread block, one per shot, from **the rifle entity v49 hp 0006**. Sizes: **enemies `m_57DAE1B5` = (2933
  0.1 + v54936 = 28A5 0.05) x v59554 = 0.15 m**; **allies `m_71A4B759` = v10601 = 2935 0.3 m** (0.4 under an unread condition,
  #244); field meanings from the Take Aim panel row (M). **Hit filter**: an allied hero only when hurt (`health < max`), so
  **full-health allies are passed through**; any enemy (M meaning, H expression). Cost **1** round (#17), **15** rounds (106E,
  4BE4 #41), recovery **v8085 = 0.8 s** (#390 literal; #2 sets it, #64 counts it down at the weapon-speed rate v21541), paused
  while quick melee runs (#63) and while scoping in / out (#172). Aim pull toward an ally under 99% (friendly aim assist, 0C6B
  #20, M). Recoil block `m_C5143E2E` (fields L). On each shot: muzzle effect **19FE** (rifle hp 0006), rumble, v689 "just
  fired" (read by 004B / 5016, M).
- **What a hit does** (status 0266 unscoped / 027B scoped -> **0B95** on the target; H): numbers re-read from Ana at the hit:
  **v476 = 75** (1037 x (1 + damage mods)), **v420 = 75** (1038 x (1 + heal mods)), **v215 = 0.6 s** (1A88). Ally (#204 same
  team): StateWait 0.6 s with **#193 heal 75, `m_95F1FB0B` 0.5** (= **75 over 0.5 s**, M field), effects 1A18 (ally hp 0001),
  1A14, **1A88 on Ana** (hit confirm), 1A43 Ana -> ally, 1A36; criterion **1F00** (#217: the HealingDart line); ally under
  40% -> message 1DA0 to Ana (#250). Enemy: **#205 damage 75 over 0.5 s**, effects 1A89 on Ana, 1A37, 19FC (target hp 0001),
  criterion 1F02 (#216). **Crits: only with Headhunter** (#269 `has perk 2599` -> head regions 000D / 0015.05C (M) -> **#272 x2
  = 150**, or x2 x (1 - v69403) on targets tagged 0037 / class 0850 (L: tanks), effect 7C77); no ally crit. No self path (M).
  An unknown flag v78411 makes the damage instant and adds an instant ally heal (#283 / #286, L Stadium); 541F (v61631) crits
  allies too (L Stadium).
- **Take Aim** (0B94; H unless said): **hold by default** (#188 -> #187), toggle with v8173 (#185; a reload un-toggles, #189).
  Gate #135: **not while reloading, not with an empty clip**, not while v462 (the first 0.6 s of a reload, an ability holding
  the weapon stack, Nano's v41217). Progress v169: **in at v65556 = 1366 4.0/s = 0.25 s** (#80; categories 003D, effects
  **1A2F** + held **1A87**), **out at 6/s = 0.167 s** (#79; 003E, effect **1A30**); **no shot during either** (#475 fire lock).
  **Fully scoped = v169 >= 1** (#285): only then the scoped volley **#167** (entity **1615**: no speed, no lifetime =
  **hitscan**, M; **enemies 28A2 0.04 m**, **allies 0.3 m**; cost 1; same 0B95 payload; same 0.8 s), the zoom **v172 = 30**
  (#83 -> #84 `m_30F5EB94`, first person only; unit L), the sensitivity scale v8174 (#193), the **slow: #239 v23184 += 1E61
  0.65** -> 01C7 #1158 strongest slow wins (cap 0.75) on mover property x786680F6: **-65%** (M channel reading). Scope overlay
  0613.116 from v169 >= 0.8 (#278). 1P rifle hidden while fully scoped (#199, M). Muzzle effect **1A92** on hp 0006 (#145).
  Unscopes: release, reload, clip at 0, another state taking the stack (#91), death; **not a shot**. Magnifier Scope (Stadium
  2BEE: zoom 30 -> 15 over 2 s, +25% damage) sits here too (#553-#557).
- **Reload** (0B94 #4 -> #60; H): automatic at 0 rounds after the recovery, or RELOAD with the clip not full (#10); anim
  **0004** empty / **0084** not (#33) over **1.5 s** (102D x v38631); **the clip refills at 0.4 x 1.5 = 0.6 s** (#72 -> #70),
  then 0.9 s more with fire locked (#68); scope blocked for its first 0.6 s (#200). Cancelled when a higher state takes the
  weapon stack (quick melee, abilities: #61): before 0.6 s no ammo. Criteria 05D7 (empty) / 05D6; voice 0E2C (below).
- **Weapon manager 0015** (H, M base speed): one weapon, so the weapon keys do nothing; a **0.5 s draw** (anim 00A9) at spawn
  (#33 / #34); category 007F while she holds it (#25). Rifle entity 159C on hp 00C9 (#199, handle v49 = the muzzle owner);
  its ammo counter shows on the model through dataflows **0B9A tens / 0B9B units** of v53 (#542 / #543, M), df 004A = ammo
  fraction.
- **Sleep Dart** (0BDD, H unless said): ABILITY 1, StateAbility #84 (input buffer 0.1 s, fail effect 0186), can cast when the
  ability stack is free at 0x15. A wait of at most 1 s (#208, normally none: L) -> **#107, a 1.0 s cast** (v670 = 0F0F 1.0):
  t = 0 effect **1A00**, pistol **15CA** on hp 0143 / 00A6 (#105), anim **047B** over 1.0 s (#115); **t = 0.3 s the dart
  fires** (#109, volley **#108**); t = 0.7 s pistol gone (#116), effect 5335; **t = 1.0 s cooldown v61 = v472** (#87). Cooldown
  4BE4 #59: `v66788 ? 16E7 12 : 0F77 14` x (1 - cooldown reduction) x setting v9536 = **14 s** (v66788 a game-wide rule flag:
  L 6v6). Dart **15E8**: **60 m/s** (0F0E x (1 + mods), #179), no gravity field (M), lifetime **20 s**, size **0F0D 0.2**, from
  the pistol's hp 0006; no target filter; payload **5 damage** (0F22) + status **026F**. Variants: Local Anesthetic **#297**
  (dart 7773 + splash r 4 running 6123), Stadium "Artsy Dartsy" #261 (80 / 75 damage) and a hitscan ally-heal #202 / #288 (L).
- **The sleep** (026F -> 0BDE #337 -> shared **224E**; H unless said): duration **Ana.v59521 = 0F10 5 s** x (1 - the target's
  CC reduction v58402) (224E #346); **tanks** (role 0850 or tags 0037 / 0DE2, #455; rule flag off, #562) **Ana.v61628 = 1245
  3.5 s** (#554; literal fallback 3.5; the panel string "3.5 s (tank)"). Timer #170. **Waking**: any damage (messages 0011 /
  07FB, #205 / #202) except tag **191F** (Local Anesthetic, #203), only once **more than 1D80 0.5 s** has passed (#332) and
  more than 0.5 s remains, sets the remaining time to **1D7F 0.5 s** (#304): she wakes 0.5 s after the hit. Any amount wakes;
  early damage is dropped. Natural end: the wake anim over the last 1 s (#210, M). While asleep (#141 / #176): ability stack
  held at 0x15, input properties 493C433E / 4E596ED2 / 7FA25A61 / 292F571E / 5C571F43 / D3421CDE = 0 (inputs, M), running
  abilities cancelled (#193, M), "ULT WAS SLEPT" to the source (#446), assist credit 3 s, effects **1A1F** + **1A58**
  (0BDE's overrides), anims **0064 / 0065 / 0066** on the sleeper (#181 / #177 / #179), kill-feed text "PUT ... TO SLEEP".
  0BDE also holds an older, unreached copy of the sleep (H: no begin edge). Criteria: 1F96, **1EFE** (Sleep lines), 1FEB.
- **Biotic Grenade** (0B97, H unless said): ABILITY 2, StateAbility #84 (fail 0186). A 0.7 s state #41, anim **0479** (#89);
  **volley #21 about 2 ticks in** (#160, M), from Ana's **hp 0002** (Wrist_L), effects **19FA** (hp 0002), **19FF**, **6FC5**;
  **cooldown at 0.7 s: 1246 12 s** (#87, 4BE4 #44). Grenade **1426**: **30 m/s** (0F13 x (1 + mods), #162), gravity **9.8**
  (`m_599AB8D7`, M), lifetime 60 s, `m_FEC435C6` 300 (L), contact radius **2936 0.2 + v54937 (28A6 0.075) = 0.275 m**; contact
  filter: an allied hero only when hurt, so **full-health allies are passed through** (M); bounce block on only with Biotic
  Bounce (#176). **Burst** `m_D2D11CE9`: radius **119B 4.0** (no falloff field), amount **0F25 90**, status **0267**, sight
  profile 0019.097 (L), effect **1874**. 0267 -> **0C71** splits by team (#252): **allies -> 0B99**: heal **90 at once** (#214),
  0.05 s later (not if anti-healed, #260) **+1DE6 0.5 healing received** (#318) for **0F12 3 s** (#310), effects 19F5 / held
  **5485**, criterion **1EFF** 0.1 s in (grenade heal line); Ana included (self branch, M). **Enemies -> 0C72**: **90 damage at
  once** (#213), **anti-heal B064: healing x 0** (#262, M = 100%) for **3 s x (1 - CC reduction)**, effect held **19F4**,
  criterion **1F01** 0.2 s in. The second burst (index v22138 > 1) uses **1549 60** (0B99 #313 / 0C72 #309). 5341 stores the
  burst point (#29); 5791 handles manual detonation (Stadium, v78653) and the Stadium bounce power 2BF0.
- **Nano Boost** (0B9E caster, 0B9F on the target; H unless said): ULTIMATE (#92), needs **v342 >= v338** (charge full), the
  stack free at 0x15. Start: v215 = **0F17 8 s**, v1648 = **1B4F 40 m**, v670 = **0.75 s**. Self-nano test #286: **`(team
  size <= 1 or FFA) and not the Practice Range`** -> target herself (no voice, no projectile). Else **TrackTargets #137**
  once at the press: cone **10 x v9602** (sensitivity, default 1), range **40 m**, **3 sight rays** (to hp 0001, centre,
  feet) through occlusion 002C, allied heroes, **not one with v8193** (already boosted); none -> effect 0186, **no charge
  spent** (#140). Option v29052 on: a 3 s confirm window (#327: PRIMARY or ULTIMATE confirms, SECONDARY cancels; sight blocked
  0.5 s, the target dead, or a stun cancels; prompt "NANO BOOST" / "CANCEL"). Cast **#187**: **v342 = 0 at t = 0** (#352),
  v842 "ult active" (#370), voice 545C (#451), anim **0x175** (#141); **t = 0.1 s** message 1D17 to the target; **t = 0.13 s the
  boost starts** (#176 -> #483), effect **1A12** on Ana, syringe **15E9** from hp 0078 (visual); recovery 0.62 s (#175): **0.75 s
  in all**. Pushed off by a stun before 0.13 s: charge gone, no boost. The target holds status **0269** for **8 s** (#485 /
  #479) running **0B9F**: **heal 250 at once** (125C, #265), **CombatModFilter AC2F +0.5 damage dealt** (125B, #257 -> v8242),
  **AC2B -0.5 damage taken** (125A, #256 -> v8243), **v8193 = 1** (#250), Speed Serum **+0.3 move speed** (#317), effects
  **5FA8**, held **5FAA**, **1A16** (others' view) / **7A23** (the target's own view), criterion **1F03** at 1.4 s (#227: the
  target's own NanoBoosted line). Ends: the 8 s, the target dying (#510), the status removed (#489); not Ana's death (M); no
  refund after #352. Dead **Shrike** branch #495-#503 (nano on Ana 6 s; perk file 254E gone). Stadium branches: Eye of Horus
  (no sight test), Falconer (effects 708A / 7089).
- **Tactician** (shared 01C7, H nodes): Ana's tag **1172** -> #2596. Store cap **v69619 = 0.25 x v338 = 575** (23DB, #2587 /
  #2586); stores increases of v342 above v338 when not in her ult (#2591) and the game is live (#2592) (#2593); gain rate
  **v74197 = 23DC 0.75** while v342 >= v338 (#2839; read by the engine, M); after the ult starts (v842) **#2835 refills v342 to
  the store over 1 s**. The old support passive (regen delay x 0.5) runs only with sub-roles off (#2703 -> #1184). Regen:
  after **0B0F 6 s**, `max x 1CAD 0.05 + 1CAE 10` a second (#966 / #963) = 22.5 HP/s.
- **Perks** (live list **0061.177** = hero `m_ECBDC792`, overriding the hero's own `m_B25192D9` = [254C Groggy, 26F0, 254D,
  2599]; tier `m_4DDE5023` 1 minor / 2 major, H): minor **Local Anesthetic 2AB5** (-> loadout 0F9C) and **Speed Serum 26F0**
  (0D0A); major **Biotic Bounce 254D** (0BB5) and **Headhunter 2599** (0BF1). **Local Anesthetic**: 0BDD #298 swaps the dart
  to #297 (7773, splash **4 m** running 6123); 51F0 #441: on the dart's damage (tag 2BF1) to an enemy hero, **0.15 s** later
  (#482) TrackTargets **4 m** around it with sight 002B (#451) -> **3 s** of 6123 (#488 / #484), effect 3D2F. **6123**: **30
  ticks of 1.5 every 0.1 s = 45 over 3 s** (#374 / #372 / #373, tag 191F), **slow v59987 0.4 -> 0.2 over 3 s** into v23186
  (#380 / #381, M: 40% easing to 20%), effects 1A37, 19FC, 7E70, 7E60. Whether the two paths double up on one target: L.
  **Speed Serum**: 51F0 #399 -> #400 (v842) -> #401 Ana **v27756 += 0.3**; 0B9F #314 -> #317 the target **+0.3**.
  **Biotic Bounce**: 0B97 #176 v58965 / v60545 / v60546 = 1 -> bounce block on, second burst **60** (4BE4 #56 1549), new 3 s
  effects (M); bounce fields (0.5 / 0.45 ...) L. **Headhunter**: 0B95 #269 (above); 51F0 #385 tests 2599 with no children.
  **Groggy 254C**: no node reads it anywhere (removed). Perk XP 4BE4 #55 (no graph reads it: engine, M).
- Not hers but in her data (H): 01C7 #1457 lists six role kits (Role: Tank, Regenerative Burst, Snap Kick, Adaptive Shield,
  Take a Breather, Steadfast); entity 66FA's kit (Heavy Pulse Rifle, Synthetic Burst Rifle, Cyber Frag, Siphon Blaster,
  Override Protocol, Altered Vitals); Resurrect, Hack, Venom Mine hang off shared graphs. 5016 / 51C1 are the Mythic weapon
  "Midnight Sun" (level thresholds [0, 0, 3000, 8000], unlocks 7836 Kill Effect / 7837 Reactivity / 7838 Flourish).

## Mover (`movement\movers.json` entry 013B.075, H)
The hero default, unchanged: walk 5.5 / 4.95 / 5.5, ground accel 35, air accel 3.5, max air 5.5, air drag 0.35, gravity 1.75
(x10), terminal 30, height **2.0**, radius **0.5**, crouch **1.25**; no extra block (Lifeweaver's STU_8956CED6 is absent).
Overrides from her kit: the scoped slow (-65% via the 01C7 slow channel, M), Speed Serum's +0.3 (v27756), Local Anesthetic's
slow on enemies, the sleep's input lock (224E). Falling damage: the shared component (`m_43A3CE29` 0.49).

## Voice (`voice.json`, `work\voice_lines.txt`, `work\voice_fx.md`)
Voice set **012A.05F**: 307 stimuli, 1067 line instances; extract 1177 .ogg in 224 folders (PVE 165, MatchTalk 120, Unknown
88, Elimination 78, Junkenstein 58, Teammate 47, HealingDart 36, Sleep 35, BioticGrenade 34, Ultimate 30, ...) (H). Lines are
said either straight from a graph (`STU_40E05F6B`) or by a scripted event (.01C) a stimulus waits for (H). Ability moments:
- **05F1 HealingDart** (cooldown 10 s): event **1F00** from 0B95 #217 when a dart heals an ally ("Take your medicine", "Get
  back in the fight!") (H; the ally branch M).
- **0E2C reload**: 0B94 #302 / #303 at reload start, at most once per team per 15 s ("Reloading. I can't heal right now")
  (H, M the cap).
- **05F5 Sleep** (cooldown 2 s): event **1EFE** from 224E #211 0.5 s into the sleep, about the sleeper, so the line names
  them ("Naptime", "Lights out", "Quiet, Jack") (H); **0688** for a sleeping Pharah (1FEB, #213: "justice could use a little
  nap") (H, L when). The waking hero says its own Knockdown 00B6 (0BDE #231 -> 0600, M). **11EA** "I slept a target!" is a ping
  line (M).
- **05F3** grenade heal (event **1EFF**, 0B99 #230, 0.1 s in: "Heal up!", "Walk it off!") and **05F4** grenade damage
  (**1F01**, 0C72 #229, 0.2 s in: "This is going to hurt", "Salt for your wounds") (H).
- **Nano cast**: 0B9E #451 -> **545C**: **0678** ("You're powered up! Get in there!"), **05F6** ("Nano boost administered!"),
  **1836** (new copies of 0678), **05F7** (Arabic "Unleash your inner strength!", gendered by the hearer; "Go get them, tiger"),
  one tick apart; which audience hears which: L (`m_DF647B95` 0 / 0 / 2 / 1). No line on a self-nano (#290).
- **The boosted ally**: event **1F03** on the target 1.4 s into 0B9F (#227) -> its own **05E2 NanoBoosted** (every hero has
  it; Ana's own "I cannot be stopped!") (H). 0B9F #244 raises 1F97 on Ana after the boosted ally's kills (L).
- Unexplained (L): **0679** ("Incoming!", "Highly effective") named by no graph or effect; events 0561 (nano cancel), 0564,
  05D6 / 05D7, 1F02, 1F96, 1F97 have no line of hers. Ult ready / charging: comms stimuli 02E7 / 0574 / 05A7 (M).

## Sounds and effects (`work\fx_sounds_timed.json`, `fx_sounds_timed_out.txt`, `graph_effects.json`; ids only, no visual decode)
290 effects read for their components. Sound banks (02C) per ability (H ids; files in the skin export
`Effects\<effect>\Sounds\<02C>\`; components start at 0 and last 1 s unless said): rifle unscoped fire **19FE** (5DDA / 6D0C
/ BC90), scoped fire **1A92** (5DD9 / 6D0D / BC91), scope in **1A2F** (386B / 39BF), scoped loop **1A87** (38E7), scope out
**1A30** (386A / 39C1) (in vs out by categories 003D / 003E, M); reload clips 3B99 / 3C98 effects 1CF5 / 1D86.08F (38EB / 39B1
/ 9190-9193, M); ally heal hit **1A14** (3804), held **1A18** (3803, 4 s), **1A88** on Ana (38EE), 1A36 (387B), 1A43 (388B);
enemy hit **1A89** on Ana (38ED), held **1A37** (387C), **19FC** (388D, 4 s), 1A9A (1834) on targets tagged 003A, crit 7C77
(0110); Sleep Dart fire **1A00** (5DD7 / 6D0B / 96F3); asleep **1A1F** (3866 / 5DDB, 5 s) + 1A58 (particles); no wake effect
(0BDE #231 sends 065D); grenade throw **19FF** (3850 / 3851), 19FA (hp 0002, no components), 6FC5 (E4EF); burst **1874** (384E /
384F / 387A); heal boost held **5485** (3881 / 3882 / B729, 5 s) + 19F5; anti-heal held **19F4** (387F / 5DDC, 5 s); Nano cast
**1A12** (3856 / 5DD6); on the target start **5FA8** (BBFB / BBFC / BBFD), held **5FAA** (3857 / 38E6 / 5DD8), glows 1A16 /
1A5B / 7A23 (particles, by viewer); Local Anesthetic (6123) 19FC / 1A37 reused + **7E60** (387F / 04CE, hp 00A4) + 7E70 (04B0 /
04CA) + 3D2F; fail **0186** (0FCB); melee **6BD4** (5D97), 0A2C (voice 0023); shared ult 004B 483D (95DE), 483F (95DF), 7ADC
(FF72), 7ADD (FF71). Of 201 clips naming an 08F, 62 hold sound banks (`fx_sounds_timed.json` `used_by`). Graphs 56D2 / 56D3 /
56D6 / 56ED / 56FB / 5732 / 57A9 / 60DA in `graph_effects.json` belong to entity 66FA, not Ana.

## Emotes, intros, poses (unlock ids from `research\kiriko-anim\unlocks\unlocks_all_heroes.json`, .0A5, H)
Emotes (16, all extracted under `extract\Unlocks\Heroes\Ana\Emote`): Heroic A58, Protector A55, Tea Time A56, Disapproving A57,
Take a Knee BEF, Not impressed BF5, Candy C24, Dance FF8, Beach Ball 117A, Yut Nori 24C8, Retro Hero 6FDA, Thumbs Down 8410,
Thumbs Up 8453, Mic Drop 9E34, Listen Up AB0C, Salty B4C0. Own props extracted for Mic Drop (1A146), Salty / Protector / Retro
Hero (0F5B), Candy (3A5B-3A64, 3FA2), Yut Nori (89B9), Beach Ball (4943); Tea Time uses her teacup 1611, teabag 1610 and
saucer 1609 (M). Intros (9 + random): Heroic A5A, Guardian A7A, Locked On A7B, Shh... A7D, Under Fire 1271, Vacation 4A74, At
Ease 635C, Grenade Toss 6A44, Cauldron Bubble 7581 (not extracted). Victory poses (17 + random, not extracted). Voice-line
unlocks: 41 .ogg in 13 folders. Their clips, props and blend trees are not mapped here (method:
`techniques/emotes/emote_reproduction.md`).

## Nuances a port must not miss
1. Two models: a separate 1P model (10561) with its own set, as Mercy's; a rifle entity (159C, its own set 05FC) on hp 00C9
   in both views; the pistol (15CA) only during the Sleep Dart, in her right hand; the model's own hardpoint table (no look
   tables) (H).
2. The rifle's heal and damage both land over 0.5 s, inside a 0.6 s hit status; never instant (H values, M field).
3. Full-health allies don't stop her darts or her grenade; hurt allies and enemies do (H expressions, M meaning).
4. Unscoped: a 125 m/s projectile, 0.15 m vs enemies, 0.3 m vs allies. Scoped: hitscan, 0.04 vs enemies, 0.3 vs allies, only
   once fully scoped (0.25 s), no shots during the transitions, -65% speed, zoom 30, sensitivity x0.3 (H/M).
5. Sleep: up to 5 s (3.5 on tanks in the data), any damage after 0.5 s wakes them 0.5 s later, damage in the first 0.5 s is
   dropped, Local Anesthetic's ticks never wake; abilities and inputs locked; the shared knock-down clips (H).
6. The grenade: an arc at 30 m/s (gravity 9.8), instant 90 / 90 in 4 m, then +50% healing (3 s) on allies not anti-healed and
   no healing at all (3 s) on enemies; it heals Ana (H/M).
7. Nano Boost: picked at the press, 10° cone x sensitivity, 40 m, sight; nothing spent without a target; the charge goes at the
   cast start, the boost at 0.13 s; the syringe is cosmetic; +0.5 dealt / -0.5 taken, 250 at once, 8 s; self only when she is
   alone on her team (H).
8. Cooldowns start at the end of the cast states (dart 1.0 s, grenade 0.7 s), not on the hit (H).
9. Tactician: the excess (up to 25% of 2300) comes back over 1 s right after Nano Boost starts (H nodes, M timing).
10. Hero-info panel rows point at other values (the nano Range row holds 03FA 12, read by nothing; Tactician's rows hold 125A):
    never read a number from a panel row without its graph (Mercy's lesson, again).

## Disagreements with ana.md (the data's build is 2026-10-06; the last patch touching her, 10-06, is in it)
- **Tank sleep: data 3.5 s** (1245, panel text) vs BZ 2024-07-09 3 s.
- Rifle sizes: data **0.15 / 0.3** (enemy / ally) vs W 0.05 / 0.15.
- Rifle heal and damage: data **75 over 0.5 s** vs W 0.592 s in 5 ticks.
- Scope times **0.25 / 0.167 s** vs W 0.24 / 0.16. Nano cast **0.13 + 0.62** vs W 0.128 + 0.608. Nano confirmation's sight
  cancel **0.5 s** vs W 1 s. Nano syringe: data cosmetic, W homing.
- Local Anesthetic slow **40% -> 20%** (M) vs W 50%; its 4 m radius (data; no public number).
- Tactician's refill: data **over 1 s after the ult starts**; BZ / W only say the excess is kept.
- Agreements (H): health 250, ult 2300, perk XP 2025 / 5060, rifle 75 / 75 / 15 / 1.5 s / 0.8 s / 125 m/s / hitscan 0.04 /
  ally 0.3 / -65% / 30% sensitivity, dart 14 s / 5 / 60 m/s / 0.2 / 5 s / 0.5 s minimum / 0.5 s wake, grenade 12 s / 90 / 4 m /
  +50% / 3 s / full anti-heal / 30 m/s / 0.275, nano 40 m / +50% / -50% / 250 / 8 s / self-nano rule, Tactician +25% and x0.75,
  regen 6 s / 10 + 5%, Speed Serum 30%, Biotic Bounce 60, Headhunter x2, Local Anesthetic 45 over 3 s, hero options and their
  defaults.

## Not found
Barrier interaction of darts and grenade (collision profiles 0108 / 0019.097 not decoded); the scoped hitscan's range
(`m_FEC435C6` 100: L); the tick pattern of the 0.5 s health changes; the rifle's fire animation trigger (no StateAnim in 0B94)
and which fire layer is scoped; the zoom value's unit; the recoil fields; whether the grenade burst needs sight; the bounce
physics fields; Biotic Bounce's bounce distance; who v66788 is (6v6: L) and other flags set outside her graphs (v78411,
v61631, v41764, v545, v77464, v69403, v55226); the hit regions 000D / 0015.05C (don't open); the cone angles' half or full
reading; the engine side of ult charge and the 0.75 rate; Groggy's implementation (removed); which audience hears each nano
line and what triggers 0679; the 4-slot clip directions, the 1P scope-out pair, df 0xFA / 0x8A; entities 5162 / 6121 / 612A /
5B18 / 1CBB; the cloth pieces' exact garments; Wwise event names (02C ids only); intros and victory poses (not extracted).

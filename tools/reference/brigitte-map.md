# Brigitte data map, from Overwatch's game data (build 154088)

Read 2026-11-01 offline and read-only through TankLib/DataTool 2.25.0.0 (`Online=false`) on the archived CASC copy
`E:\Overwatch Archive\build-154088` (2026-10-06, the copy every earlier pass used; `extracted\154088\` is an older DataTool
export, not a CASC), with Overwatch and Battle.net checked closed (`Get-Process Overwatch, Battle.net`) before every DataTool
and TankLib run (`scripts\owinit.py` and `scripts\extract_brigitte.ps1` refuse on their own too). Nothing was decrypted beyond
DataTool's own reading, nothing was run but DataTool / TankLib, nothing went into the repo but text. Confidence: **H** read in
the data (a node, field, value, edge, string), **M** inferred (one link guessed, or read by analogy with Ana / Sombra / Mercy),
**L** guess. Node refs are `graph #uid`; vars `vNNN`; values by their `.130` id. Public numbers: `brigitte.md` (a separate
pass). Research folder: `E:\Overwatch Archive\research\brigitte\` (paths below are relative to it).

Detail behind this map (each finding there has its node ids): `work_kit\kit_flail_inspire_perks.md` (Rocket Flail, quick
melee, weapon manager, Inspire, perks, Survivor, hero stats), `work_kit\kit_shield_bash.md` (Barrier Shield, the barrier pet,
Shield Bash, what Rally changes there, the Story Mission copy, Shield Throw), `work_kit\kit_whip_repair_rally.md` (Whip Shot,
Whiplash, Repair Pack, Rally), `work\assets.md` + `work\clipmap.md` / `clipmap.json` (entities, models, hardpoints, every clip by
state). Dumps: `dump` (hero 0195.075), `dump_ss` (crawl from 0195.075 following 003 / 01B: 117 graphs, 51 entities),
`dump_lo` (9 hero loadouts, 4 perk loadouts, perk files .134, the live perk list 0063.177), `dump_anim` + `resolved\` (anim
pass), `graphs\` (104 reflected graphs, `tl_nodes.py`), `work_kit\decomp_<g>.txt` + `edges_<g>.txt` + `c_<g>.txt` (104
decompiled graphs), `work_kit\shield\` (game mode 0042.0C5, .134 files, raycast receiver 002A.098). Machine-readable:
`gamevalues.json`, `gvpair.json`, `loadout_stats.json`, `work\varvalues.json` (values graph **4411**), `work\clipmap.json`,
`work_kit\herosettings_brigitte.json`. Scripts: `scripts\` (copies of Sombra's, retargeted: `owinit.py`, `crawl.py`,
`gamevalues.py`, `gvpair.py`, `varvalues.py`, `tl_nodes.py`, `lostats.py`, `kit\decomp.py`, `kit\decomp_out.py`,
`kit\compact.py`, `kit\lean.sh` ...; new: `hero_brigitte.py`, `extract_brigitte.ps1`, `kit\herosettings_brigitte.py` and the
`*_brigitte.py` asset / clip-map scripts, `clipmaps_brigitte.py` writes the clip maps). TankLib scripts run with the
kiriko-anim venv's interpreter (`-I`).

Extraction (offline, DataTool 2.25.0.0, logs in `logs\`), into `extract\`: `extract-unlocks "Brigitte|skin=Overwatch"
--extract-refpose` -> `Heroes\Brigitte` (58 models, 365 clip files, 116 effects with 899 sound files, 36 entities),
`extract-hero-voice "Brigitte" --subtitles-with-sounds` -> `HeroVoice\Brigitte` (1229 .ogg), `extract-unlocks
"Brigitte|emote=*"` / `"Brigitte|voiceline=*"` -> `Unlocks\Heroes\Brigitte\Emote` (16) and `\VoiceLine` (35 .ogg). Hero
**`0195.075`**, Gameplay3P **`1B1B.003`**, hero colour (0.447, 0.200, 0.165) (H). Clip ids are .006 indexes, files under
`extract\Heroes\Brigitte\Skin\Overwatch\Overwatch\Models\<model>\Animations\<prio>\<group>\`.

## Models (`work\assets.md`)
The entities name base models; the Overwatch skin swaps them (DataTool's `.owentity`, H):
| | entity | model (base -> skin) | set | notes |
|---|---|---|---|---|
| 3P | **1B1B** (hero `m_gameplayEntity`; mover, health, weapons, graphs) | 5DC4 -> **10C10** (392 bones, 130 cloth) | **0800** | (H) |
| 1P | **1B1C** (child of 1B1B) | 5DC5 -> **10C11** (104 bones) | **0802** | only child: the flail; **no shield in 1P** (H) |
| Flail handle | **2282** on hp **000A** (Weapon_R) | 5CF0 -> **10648** (7 bones) | **087B** | its hp 0254 = head socket (0, 0.5, 0), 0076 = chain start (H) |
| Flail head (flying) | **2283** (1248 #640 draws it at v432: 3P hp 0257, 1P the handle's 0254) | 5CF1 -> **10647** (23 bones) | **088C** | its 0004 = chain end, 00A4 = sparks (H) |
| Flail head (seated) | 2378 on the handle's 0254 (var 33C0) | 10647 | 088C | (H) |
| Barrier (pet) | **2284** "Barrier Field" (StatePet 0EE3 #635, handle v13214, runs **0EE4**) | 5DA0 -> **1064F** (21 bones), look 13917 | **060A** | on her bone 002C; target tag 003A; raycast receiver 002A.098 (H) |
| Shield in hand | 24C5 on hp **000B** (Weapon_L, var 3816) | 1064F | 060A | (H) |
| Shield folded | 1B25 on hp **01D8** (bone_0F74, var 339E) | 1064F | tree 0235 (no clip) | (H) |
| Repair Pack | **22DB** -> child 22DC (effect 218D) | none | - | (H) |
| Whip anchor | 0463 (empty, placed at the hit point) | none | - | (H) |
| Shield Throw (Stadium) | 6A33 (graphs 5955 / 5957) | - | - | not the barrier (H) |
| Showcase / preview / gallery / intro | 3A90 / 2344 + 490E / 22EE / 2346; preview weapon 2351 | 10C10 | 0C97 / 08AD + 4303 / 088B / 08AC | (H) |

- **One shield model, three looks** (H): the barrier, the hand shield and the folded shield are all 1064F; the entity's look
  and set decide which. The barrier pet exists while the shield is down too (created by 0EE3 #338's always-on subgraph, M):
  that is where it regenerates.
- **Hardpoints the graphs use** (the models' own STU tables; rest places in `work\assets.md`): 000A Weapon_R (flail), 000B
  Weapon_L (hand shield), 01D8 (folded shield, bash hit effect), 0257 (3P flail head), 0256 (1P head orientation), 0075 / 0077
  (bash trail), **0002 Wrist_L** (Repair Pack leaves here, arrives at the ally's **0014**), 0001 (pack outline, sight rays),
  0012 (the flail's sight point on targets) (H).
- **Skeleton** (H): rest Pelvis 1.126, Head 1.668, eyes 1.709, camera bone 007D 1.760 m; in play the pelvis key is 1.0
  standing, 0.789 behind the shield, 0.633 crouched. Mover height 2.0, radius 0.5, crouch 1.25 (hero defaults).
- **Cloth** (3P only; H counts, L/M roles): 54 points on a back chain (0982 / 12D2-12D6: L hair or back piece), 115 points
  below the hips (M skirt flaps).
- The barrier's **dimensions are not in any graph or entity** (H by search): they live in the 1064F mesh / look 13917, not
  measured (Not found).

## Clip map (`work\clipmap.md`, `clipmap.json`; repo `tools/clipmaps/brigitte.txt`, `brigitte_1p.txt`, `brigitte_flail.txt`, `brigitte_flail_head.txt`, `brigitte_shield.txt`)
Switches (H): **df 0xE5 cat 0xCA = shield up** (0x1E1 in Rally; 0EE3 #476 stacks 0x00CA on v1273); **df 0x583 cat 0x8E =
Rally**; ability categories ride on df 0xC4 (3P) / df 0x1A0 (1P). No weapon split. Crouch and the whole 1P set have no shield
split.
| State | 3P (0800) | 1P (0802) | props |
|---|---|---|---|
| Idle | **4E89 / 4E88** turn stands (root yaw -120 / +120) + 4E77 + 4E8A; shield **4FE7 / 4FE5** + 4FEF + 506B | 4FB6 + 4FEB | |
| Run 8-way | **4E78-4E7F** (0.8 s; 5.5 / 4.95 m/s) | | |
| Walk behind the shield | 4FD1 / 5054 / 5061 / 505E / 505B / 505D / 5060 / 5055 at **3.85 / 3.46 m/s = 0.7 x run** (the 30% slow, H) | | |
| Stops | 5253 / 5254 / 5256 / 5255; shield 507B / 5082 / 5081 | | |
| Crouch | stand 4E9B / 4E9A; walk 4E8B-4E92 (3.0 m/s) | | |
| Jump / land | 4E74 -> 4E76 -> 4E75; moving 502F.. -> 502D.. -> 502E..; land 4E72 + 8 slots; shield 5044 / 5046 / 5045, land 5047 | 4FA2 -> 4FA3, land 4FA1 | |
| Rocket Flail (alias 0x71) | windup **4E9D** (507E, cat 0x22A, from the third swing on); forehand **4E9E** (0x6F), backhand **4E9F** (0x70); 4EA2 (0x71) / 4EA3 (0xA1) at swing begin / end (role L); release 0x112 / 0x113 = **4EA0 / 4EA1**; pitch triples | 4F54 / 504E / 4F53 / 4F52 / 4F3A / 4FB2 / 4F37 / 4F55 | flail + head have their own 1P (sm[0], df 0x1A0) / 3P (sm[1], df 0xC4) clips (M) |
| Whip Shot (0x2B) | launch **4F30**; return 51CA hit / 5056 blocked / 51EB miss / 51EC close | 4F56 / 503E / 513D / 5169 / 5040 | |
| Barrier raise / lower | **4FF2** (0x1B6) / **5067** (0x1B7) | none | shield deploy 4FF3, fold 5024, held 5118, Rally D591 / D5C4 / D5C5 / D711 / D712 |
| Shield Bash | **5073** (0x568), hit **50AE** (0x571) | none | shield 50B3 / D705 |
| Repair Pack | 5062 / 5063 / 5064 (0x505); no 3P 0x4AA | 4F5A; 0x4AA 5119 | |
| Rally | 4FE3 / 5237 (0x1B8); end adds 4FE4 / 52CA (0x101); layer 535A -> 4FE2 / 52CC -> 535B | 4FD9 | head 4FDB / 50B0 |
| Stun (0x070) / knock-down / pulled | 70C3 / 4ED7-4ED9 / 4ED2-4ED6 | 5010 | shield 4ECD-4ED1 |
| Hello / showcase | 52D4 / 65DD / 65DC; showcase C06C -> C07D -> C0B8 + C0B9 | 511C | |

**No clip** in her sets (H): the draw (0x0A9, played by 0015), a 3P 0x4AA, any 1P barrier or bash anim, a death of her own
(the shared knock-down), a separate quick melee (it is the flail swing). **Not extracted (16, H):** 527B (preview emote, M
Heroic), showcase C06C / C07D / C0B8, intro 50E7 / 12680 / 1A292, C0CE / C0CF (0EF3), C0E2 / C1DE (hero file), 19F63-19F65 /
1A062 / 1A129 (4303). The clip-map script writes a clip only if that model's own export has it.

## Abilities (numbers from her values graph **4411** and the graphs that read them)
Graphs on Gameplay3P 1B1B (H): weapon manager **0015**; **1248** Rocket Flail (+ quick melee; Stadium 594E / 617E); **0EE3**
Barrier Shield + Shield Bash (+ **0EE4** on the barrier pet, 2152, 3C51, 224E shared CC, 5957); **1259** Whip Shot (+ **551F**
Whiplash on the target, 3C51); **0EDD** Repair Pack (+ **0EE1** on the target, 61C6 Stadium); **12BE** Rally (+ **12C1** on
each ally, 545C voice, 605E); **12D8** Inspire (-> **11B9** on each ally; 5999 Stadium); **51F2** perks (+ 59E4); **4411**
values (+ shared 3361); **145B** Story Mission switch (0EE3 -> 463B, 0EE4 -> 463A); 004B shared ult, 01C7 role / sub-role /
regen, 0033, 08B6. **No 0043**: the shared quick-melee graph is not on 1B1B (only 66FA, another kit, reaches it) (H).
Buttons (H): PRIMARY **xD128396E** (flail; with the shield up, Shield Bash), SECONDARY **xB42D90B7** (Barrier Shield), ABILITY
1 **xCBF0F815** (Whip Shot, buffer 0.15 s), ABILITY 2 **x2ADDA7B1** (Repair Pack), ULTIMATE **x2DFEDC52** (Rally), QUICK MELEE
**xD19BBFFC** (1248 #267: the same flail swing). Loadouts (H): 01A6 Rocket Flail, 01DA Barrier Shield, 01DB Shield Bash, 01A3
Whip Shot, 01A4 Repair Pack, 01A5 Rally (+ 001A), 01A7 Inspire, 0DA9 Subrole: Survivor, 0DCA Hero Stats.
**Health 175 + armor 75 = 250** (0B1E / 0B1F, 4411 #4 -> 3361; no shields), melee 0B36 **45**, 5.5 m/s (202B), the hero-default
mover, **ult cost 0AE1 2700** ("Ultimate Charge Max", passed to 004B as 0152), perk XP **12C9 2035 then 12CA 3055 more** (H).
Hero options (file 0126.054, `herosettings_brigitte.json`, H): **Toggle Barrier Shield v14673 off**, Relative Aim Sensitivity
During Barrier Shield **v49156 1.0** (gyro v49158 1.0; 0.01-5), **Repair Pack Sensitivity v16597 1.0** (0.01-1), Repair Pack UI
v49150 on, Inspire UI v75478 on, vibration v54074-v54078.

- **Rocket Flail** (1248; H values, M where said): damage **v476 = 0B36 45** (#780), range **v1648 = 113A 6 m** x hero scale
  (#805); windup v670 **0.2 s**, recovery v567 **0.4 s** -> **one swing per 0.6 s** (100/min); every time / attack speed
  v21541 (default 1, clamp 0.5-3, 01C7 #586 / #587). PRIMARY held (#580) or quick melee held (#267) set v34620; #266 is the
  swing state (v33 priority 2). **Combo** (#636 on v59): 0 / 2 forehand (#807 -> #345), 1 backhand (#808 -> #346), held it
  steps 0, 1, 2, 1, 2...; release (#639) plays 0x112 (0.7 s) or 0x113 (0.63 s) recovery, effect 223A. **Hit window 0.2-0.4 s**
  (one frame #679 / #680, then a 0.2 s sweep #81 / #80, M on the chain): TrackTargets #715 / #717 sweep a **35° eased arc**
  (yaw -17.5 + 35·t², backhand mirrored; origin 0.5 m to the side closing to centre, H expressions, one radius operand L),
  sight 002B.097 to the target's hp 0012 (M), enemies (filter 64 / 20 / 1, M), **no target cap: it pierces**, each target once
  per swing (v375 / v23530, M). Damage tags 03BF (Inspire), **3629** (flail: Combat Medic), 09A3, 20EC, 4D2A, EC60, 10130.
  Ground ray 6 m ahead at 0.3 / 0.35 s (effect 220B). Effects 219B, 219C, 22C4, 3612, chain beam 216C / 33C1; voice 094B on
  the backhand (#675). An ally-heal variant (#836-#843, v77284 / v77285) is set by no graph (L off). **The shield up blocks
  swings** (0EE3 #670 holds v33 at 0x79; 1248 v689 = !v690) (M).
- **Quick melee = the flail swing** (H: no 0043 on 1B1B and the button in 1248; M behaviour): 45, same timing, same combo.
- **Weapon manager 0015** (shared, H): 0.5 s draw, alias 00A9 (no clip in her sets).
- **Inspire** (12D8 -> 11B9; H unless said): 12D8 #408 on damage-dealt message 0012 whose tags hold **03BF** and not 4FFB
  (#437 / #409), victim not tagged 003A / 047F / 0D74 / 0EA2 (#423): **flail, quick melee, Whip Shot (tag 3679), Shield Bash
  (tag 7851)**. Cooldown **2ABF 1.25 s** (#451 / #427) between pulses, but **Whip Shot and Shield Bash hits trigger it even
  on cooldown** (#428 -> #443). Pulse #388: **20 m** (1345) around her, allied heroes, **no sight test** (M), plays 11B9 on each:
  **12 at once (2A2C, #450) + 45 over 4 s (0B24 / 1344, #412)**; **she heals herself too** (#424, M that she passes the filter).
  Effects 2182 / 2253, on hurt allies 21EA / 2222, voice 09BE, UI message 2AD8 (v75478). Refresh vs stack: policy x8715BB77
  not decoded (L).
- **Barrier Shield** (0EE3 on her, 0EE4 on pet 2284; H unless said): StateAbility #338 on SECONDARY, **hold by default**
  (#511; release lowers it, not during a bash, #512 `!v596`), toggle with v14673 (#729 / #720 / #730, re-raise #1087). Raise:
  anim **01B6** 0.23 s (#471), voice 0945 (#662), effect 21B6 (6F8D in Rally), fade-in v6728 over 0.075 (0EE4 #245, M
  duration); **no deploy delay before it blocks** (M). Lower: 0.1 s wait then anim **01B7** 0.27 s (#473 / #474), fade 0.25.
  **Health 300** (0EE4 #135 StateHealthPool, 0EEF). **Regen: shield down, 2.0 s (0EF0) then 85/s (0EF1)** (0EE4 #208 -> #204
  -> #209, M per second); nothing resets the delay on damage (H). **Break** (health 1, 0EE3 #641 / 0EE4 #132, M): cooldown
  **v61 = 1136 5 s** (#639), effect 268C, voice 09C1; a normal lower costs no cooldown (M). Damage taken: 2% of absorbed to her
  (0EE4 #149, M ult charge), voice 09BF (10 s lockout), below 50% / 25% effect 0540 + voice 09C0. **Self slow: v23184 += 0.3**
  (#444 -> #796; the walk clips are exactly 0.7 x run) = **-30%** (H, M channel); the panel's 02FF "25 / max falloff" is read
  by no graph (H by grep). Attached to her bone 002C (during a bash bone 0002 + (0, 0.25, 1.5)), **x1.2 in Rally** (#873 v31296;
  0EE4 #265 / #240). While up: flail off (M); Whip Shot and Repair Pack allowed, not mid-bash (`!v31950`, M); aim sensitivity
  v49156 (#902).
- **Shield Bash** (0EE3; H unless said): only with the shield up (#670 -> #681 -> #491 PRIMARY -> #707 cooldown -> **#495**);
  on cooldown, fail effect 0186; it does not lower the shield. **Dash: #495 StateWait 0.3 s**, property 786680F6 = **1C9C 4.0**
  (speed x4: about 22 m/s, about 6.6 m, M), forward input forced (7FA25A61 = 1, L), jump off the first 0.25 s (#660), velocity
  set to 15 at the end (#828-#830, L cap); stops early on an enemy or a wall (v488 -> #772, M); **no StateMoverOverride** (H).
  Sends message **26FD** (#1094: Survivor). **Hit #501**: radius 2.4775 x scale + v9339 x 0.5225 (v9339 = STU_ED807E07(self) / 6,
  L), shape op89(1.73 x scale, r) (L: a ~35° cone), enemy hero units, **one target**, sight 002C.097; **#503: 70 damage (0B20;
  alt 80), knockback 6 m/s (0B21) x STU_3832D36C(v14130)** with `m_583C208B` = 3 x the scale: the vertical of the pair (M; the mod once read it as 3 x the 6), tag **7851**, effect 2206
  on hp 01D8, 3C51 assist 2 s, anim 0571 to the pet, voice 09C2 (L 50%), criterion 3678. Also #732 (90° cone, 70 to
  deployables, M) and #762 (1.5 m ray, r 0.5, tag 0385: walls / barriers, M). **Cooldown 1135 5 s at t = 0.3 s** (#495 onEnd ->
  #656 -> #504 v13062); v596 clears 0.05 s later (#704). No input cancels it; the barrier breaking does (#708, M). **No stun
  without Rally** (H). **Clash** (#736, M meaning): an enemy bashing (v596) or charging (v55490) and facing her -> message 2652 ->
  both stunned 1.7 s (224E, type 6BBE, #942 / #945). Anims 0568 (0.2 s), 0571 (0.2 s) on a hit; effects 21BB + voice 094A at
  the start, 2207 during, 2284.00D on hp 0075 / 0077 from 0.06 s to 1 s after; PhysicsExplosion #654 r 1.25 (M props).
- **Whip Shot** (1259; H unless said): StateAbility #581 (ABILITY 1, buffer 0.15 s, cooldown v61). Values: damage **v476 = 0B38
  70** (alt 85), cooldown **v472 = 133C 4 s**, range **v1648 = 133D 20 m** x scale, knockback **v9275 = 133E 25** x v14131,
  speed **v681 = 2B27 80 m/s**, radius **v27951 = 2B26 0.5 m**. **Windup #593 StateWait 0.2 s** (anim 002B cat 006E 0.5 s, effect
  22AD; physics push #621 r 0.6 at 0.15 s, L). **Launch at 0.2 s**: from eye + facing x 0.5 m (#623) to eye + aim x 20 m (#617),
  #619 a **straight chase at 80 m/s: no gravity** (full range 0.244 s). Hit: world ray #620 (0129.097); enemy sphere #596
  **r 0.5 m, one target** (filter 64, M); #986 (r 1, filter 128) 50 to objects (M). **Blocked**: #610, target.v4112 and facing
  her -> no damage (H expr, M barrier). **#638: 70 damage**, tags 3679 + 03BF. **Knockback = her horizontal aim at launch x 25 +
  2 m/s up** by `m_1376B4DF`, while the pair says 25 / 3 (`m_E33EFEE1` v9275, `m_583C208B` 3 x scale): the mod plays 3, chosen to reach W's 8 m (`polish4.md`), not settled here; airborne target (v417, L) 3D aim x 25.08; **the direction is her aim, not away from her** (H). Return (alias
  002B): hit beyond 5 m 0.3 s (cat 006A 0.583 s); wall / blocked 0.3 s (00C9 0.4 s); miss 0.3 s (0141 0.625 s); anything under
  5 m 0.175 s (0106 0.4 s); **no hits on the way back** (H). **Cooldown when the return anim ends** (#1033 -> #688 -> #689), also
  on interrupt (#1054 -> #894): a hit at 10 m is ready about 4.9 s after the press (M). Head entity 2283 lerped from the hand
  hp 0257 / 0254 toward the hit point; chain effects 216E / 22CF (33C2 / 33C3), return 22AE; voice 0A03 on a hero hit (#922);
  criterion 3677 when the target has no ground within 2 m (#794, M airborne), 05AD on each return. 3C51: assist mark 2456 for 2 s.
- **Repair Pack** (0EDD on her, 0EE1 on the target; H unless said): ABILITY 2, StateAbility #321. Targeting (#116 UI every
  0.1 s, #391 while held #518): **cone 10° x v16597**, **25 m** (1CB5), sight 002C.097, one target, scores angle 1 / distance
  0.01 / health -0.05 (M), **allies, not herself** (filter 4, M; Rally uses 6). Cast: voice 0948, anims 0505 0.57 s + 04AA 1.0
  s, effects 2EC3 / 4 / 5 by charges left. Projectile #461 (several can fly, M): #337 chases from her **hp 0002 to the target's
  hp 0014 at 55 m/s** (v6368), the end moves: **it homes**; cosmetic 22DB -> 22DC -> 218D. **Charges 3** (133B): one spent 0.25 s
  after launch (#402 -> #420), **recharged one at a time, 5 s each** (133A, #423 / #424), effects 2EC6 / 7 / 8; refunds by
  messages 2623 (seconds), 2871, 2973; empty press voice 15A6. **On the target (0EE1)**: **25 at once** (#556, 1337 via v61915;
  +35 when the target is under 50%, #569 / #570, behind perk test 25C5 #561, L retired) and **v61897 = 1338 50 x 1339 2.0 = 100
  over 2 s** (4411 #39; 0EE1 #567 v12280 = v420 / v22716 = 100 / 2 = **50/s**; #460 counts 2 s; #368) (H chain; the panel row
  "Total Healing Over Time 50" shows 1338 alone, so **125 in all, not 75**: check against patch notes). A second pack on the
  same target (message 0F6F, #458): +2 s remaining (#466) and another 25 (#555). **No overhealth in the base kit** (H: the pools
  in 0EE1 / 61C6 are behind perk 2652 (L retired) and Stadium powers 26D4 / 2C3E / 2C45). Effects 2074 (over time), 217E /
  2ECF / 2ED0, impact 2F2E. Instant -0.5 on a mover property in #364 (L).
- **Rally** (12BE on her, 12C1 on each ally; H unless said): ULTIMATE, StateAbility #150 (v342 >= v338, M), **charge to 0 at
  the press** (#152), cast lock #88 **0.7 s**, the **10 s** state #77 (0B13; alt 15; x modifiers and v13032) from t = 0. While
  active: **self armor pool 100** (#204, type xA6B6AFE9, id 32B3, no decay field, ends with Rally, M); **move speed +0.15**
  (#148, M +15%: not on the panel); v842 = 1 (read by 0EE3 / 0EE4 / 0EDD); **pulse #158 every 0.5 s** (#161): **8 m** (1342),
  sight tag 02A0, filter 6 (M: allies and herself? BZ says not her: #125) -> 12C1: if the ally's 32B3 overhealth is under **100** (1341) grant
  **min(100 - current, 15)**, each chunk its own pool inside a **30 s** StateWait (#195 / #174): **30/s up to 100, each chunk
  lasting 30 s, outliving Rally** (whether it decays after: L). 3B8E credits absorbed damage to her (M). In 0EE3 / 0EE4: barrier
  **300 + 450 = 750, refilled** (0EE4 #299 / #302 / #295), x1.2 size; **bash stuns 0.75 s** (1340, #889 -> #888, 224E, effect
  220D); **0.15 s after a bash hit a shockwave** (#878 -> #877): r 8, op89(2.25, 8), up to 10 targets, each 70 + knockback 6 +
  the 0.75 s stun (M shockwave); first hit test x1.1 / x1.6; **Rally start resets both cooldowns** (0EE3 #893 v61 = 0, #903
  v13062 = 0). Whip Shot and flail unchanged (v842 not read in 1259 / 1248). Anims 01B8 1.2 s -> 00A9 1.0 s, category 008E
  while active, end 0101 0.27 s; effects 541F at start, 21C5, after 0.5 s 219D + 22AB (her and the barrier), 5420 under 3 s
  left; allies 2202 / 21DE; voice 0946 / 0947 / 183A (#221), 545C hero replies 183F / 1840 / 1841 / 184A / 1849 (M).
- **Survivor** (01C7; H nodes, M meaning): her tag **1173** opens **#2564** (Ana's 1172 is Tactician). On message **26FD**
  (#2582 / #2583) a StateWait **v12209 = 23DA 0.25 s** (#2602, #2882) holds v42467 = 0 (#2603), the regen gate (#958 -> #963):
  regen runs at least 0.25 s; effect 7447, pet 7399, messages 2727 / 261C. **Only Shield Bash sends 26FD** (0EE3 #1094) (H):
  not Whip Shot, Repair Pack or Rally. The panel's "Minimum Duration 2 s" shows 1339, which is Repair Pack's duration; the graph
  reads 23DA 0.25 (H). Regen (shared, M from Ana's): after 0B0F 6 s, max x 1CAD 0.05 + 1CAE 10 per second (22.5 HP/s at 250).
- **Perks** (live list **0063.177** = hero `m_B25192D9` = [274D, 2554, 274C, 25C2]: all four live; tier `m_4DDE5023`, H):
  minor **Combat Medic 274D** (loadout D47): 51F2 #465 -> #447 on tag **3629 (flail hits only)**, #466 sends 2623 (0.75) ->
  0EDD #482 cuts the running Repair Pack recharge by **0.75 s** (floor 0), per damaged target (M). Minor **Morale Boost 2554**
  (B74): 12D8 #447 sets v29410 on tag 3679 (Whip Shot); 11B9 #440 / #438 multiply **duration and total by 1.75 = 7 s / 78.75**.
  Major **Inspiring Strike 274C** (D46): after the dash (v31950 falls) **+0.3 speed for 2 s** (51F2 #455 / #456, v27756); a bash
  hit (tag 7851, #464) pulses **20 m** (#468) and #459 heals **57 at once** (0B24 45 + 2A2C 12), 12D8 #469 skips the normal
  heal-over-time; effects 6E17 / 6ED4 / 6E95 / 65DE. Major **Whiplash 25C2** (B76): 1259 #997 plays **551F** on the target with
  the aim direction for **0.5 s**; contact set 0894 / tracker #529; on contact **#526: 60 damage**, effects 02B1 / 6582 / 6584,
  0.15 s hard stop (M). Dead code (unlinked begin plugs): 51F2 #353 / #410 / #422 (100 to her barrier; entity 6AB6 -> 59E4).
  Retired / Stadium: 2652, 25C5, 26B8 (no file), powers 26C5, 2B45, 26CB, 26FA, 2C3C, 26CD / 2B47 Shield Throw.
- **Story Mission copy** (145B #53 / #54 on game mode 0042.0C5): 463B / 463A with hard-coded 300, 85/s, 450, 5 s, bash 70 or 50,
  clash 2.0 s, Rally stun 0.75; no shockwave (H). Not the PvP kit.
- **Shield Throw** (Stadium 26CD / 2B47): 0.2 s windup, projectile 6A33 from the pet's hp 0001 at 30 m/s, 70 x 0.4 = 28 (M),
  return via 5957 at 35 m/s (H). Not PvP.

## Mover
The hero default (5.5 / 4.95 m/s, height 2.0, radius 0.5, crouch 1.25) (H, from 1B1B and the hero-stats row). Overrides from
her kit: shield up -30% (v23184), Shield Bash x4 for 0.3 s (property 786680F6), Rally +15% (M), Inspiring Strike +30% for 2 s.
Falling damage: the shared component.

## Voice, sounds, effects
Extracted (`extract\HeroVoice\Brigitte` 1229 .ogg; skin export 116 effects with their sound banks), **not mapped in this
pass**: the voice set, stimuli, and effect-to-sound tables are for the effects / voice pass. Ids the kit graphs name are in
each ability above (voice .078 stimuli 0945 / 094A / 094B / 0948 / 09BE / 09BF / 09C0 / 09C1 / 09C2 / 0A03 / 0946 / 0947 /
183A / 15A6; criteria 3677 / 3678 / 05AD; effects by id).

## Emotes (16, extracted under `extract\Unlocks\Heroes\Brigitte\Emote`)
Cheer, Heroic, Hilarious, Kneel, Recalibrate, Taunt, Dance, Power Flail, Listen Up, Mic Drop, Salty, Katt, Choreography, Flail
Tricks, Guard Duty, Bunny Hop (names from DataTool's folders, H). Clips, props and blend trees not
mapped here (method: `techniques/emotes/emote_reproduction.md`). Voice-line unlocks: 35 .ogg.

## Nuances a port must not miss
1. Health is **175 + 75 armor** (not 250 health); ult 2700; Survivor, not Tactician (H).
2. **Quick melee is the flail swing** (45, same timing and combo counter); there is no separate melee (H/M).
3. The flail **pierces** (no target cap) along a 35° eased sweep 0.2-0.4 s into each 0.6 s swing, 6 m, sight-checked; forehand
   / backhand alternate and a release anim plays when PRIMARY is let go (H/M).
4. Inspire triggers on flail, quick melee, Whip Shot and Shield Bash hits (tag 03BF); whip and bash ignore the 1.25 s cooldown;
   12 at once + 45 over 4 s, 20 m, **no sight test**, Brigitte included (H/M).
5. Whip Shot is a straight 80 m/s, 0.5 m, 20 m shot, one target, no hits on the return; knockback follows **her aim** (25
   horizontal + 2 up), not the line from her to the target; the cooldown starts **when the flail is back** (H).
6. The barrier is a **pet** that always exists; it regenerates 85/s after 2 s down, only while lowered; it breaks into a 5 s
   cooldown; lowering normally costs nothing; there is no raise delay; -30% speed while up; no flail while up (H/M).
7. Shield Bash needs the shield up and leaves it up; 0.3 s at x4 speed, one target, 70 + 6 m/s knockback, **no stun outside
   Rally**; cooldown at the end of the dash; a bash against a charging / bashing enemy stuns both 1.7 s (H/M).
8. Repair Pack **homes** (55 m/s), has **3 charges recharged one at a time, 5 s each**, never targets herself, 25 at once +
   (data) 100 over 2 s, and gives **no overhealth** in the base kit; a second pack adds 2 s and 25 (H).
9. Rally: charge 0 at the press, 0.7 s cast, 10 s: 100 self armor, +15% speed, 15 overhealth every 0.5 s to allies within 8 m
   (and her, M) up to 100, each chunk lasting 30 s past Rally; barrier 750 and x1.2; bash 0.75 s stun + an 8 m shockwave;
   both cooldowns reset at the start (H/M).
10. Survivor's regen trigger is **Shield Bash only** (message 26FD), minimum 0.25 s (H).
11. Hero-panel rows mislead again (Survivor's "2 s" is Repair Pack's 1339; the Repair Pack total row shows the per-second 50;
    02FF's "max falloff" is read by nothing): read numbers from graphs, not panel rows.

## Disagreements to check against brigitte.md (data build 2026-10-06)
- Repair Pack heal over time: data **100 over 2 s** (50 x 2) vs the panel's 50.
- Shield self slow: data **30%** (v23184 0.3, the walk clips 0.7 x run) vs the panel's 02FF 25.
- Rally's **+15% speed** is in the data, not on the panel.
- Survivor's minimum regen: data 0.25 s vs the panel's 2 s; only Shield Bash triggers it.

## Not found
The barrier's dimensions (mesh 1064F / look 13917, not measured); STU_ED807E07 (bash radius term) and op77 (flail sweep
radius); `m_583C208B` (the pair's vertical, M; against #638's 2 up: `polish4.md`); v14131 / v14130 (the mod takes them as 1) / v417 / v4112 / v6194 / v69292 / v78714 / v69279 / v3652; the 02FF
falloff; the Rally stun's CC type; whether Rally armor and the ally overhealth decay after their 30 s / Rally end; the pool
type hashes (xA6B6AFE9 armor, xCBEFD125 overhealth: M from panel text); Inspire's refresh-vs-stack policy x8715BB77; whether
Brigitte's own Rally overhealth (BZ 2023-04-11 and W no; filter 6 and 12C1 #178 lean yes, unexplained: #125); the draw clip (0x0A9 has none in her sets); the roles of flail cats
0x71 / 0xA1 and the 1P whip states; the voice set, voice lines' text and sounds (not mapped in this pass); intros, victory poses
and the 16 unextracted clips.

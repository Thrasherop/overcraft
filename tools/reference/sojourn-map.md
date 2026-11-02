# Sojourn data map, from Overwatch's game data (build 154088)

Read 2026-11-01 offline and read-only through TankLib/DataTool 2.25.0.0 (`Online=false`) on the archived CASC copy
`E:\Overwatch Archive\build-154088` (2026-10-06), with Overwatch and Battle.net checked closed (`Get-Process Overwatch, Battle.net`)
before every DataTool and TankLib run (`scripts\owinit.py` and `scripts\extract_sojourn.ps1` refuse on their own too). Nothing was
decrypted beyond DataTool's own reading, nothing was run but DataTool / TankLib, nothing went into the repo but text. Confidence:
**H** read in the data (a node, field, value, edge, string), **M** inferred (one link guessed, or a field read by analogy: mover
properties from Juno's `juno-map.md` and Mercy's readings, velocity entries from Juno's / Lúcio's), **L** guess. Node refs are
`graph #uid`; vars `vNNN`; values by their `.130` id. Public numbers: [sojourn.md](sojourn.md) (a separate pass, its `D` fields
filled from this one). Plan: [sojourn-plan.md](sojourn-plan.md). Research folder: `E:\Overwatch Archive\research\sojourn\` (paths
below are relative to it).

Detail behind this map: `work_kit\kit_sojourn.md` (every ability node by node), `work\assets.md` (entities, models, hardpoints),
`work\clipmap.md` + `work\clipmap.json` (every clip by state), `work\animrefs.txt` (which graph node plays which alias / category).
Dumps: `dump` (hero 01EC.075), `dump_ss` (crawl from 2280.003 following 003 / 01B: 141 graphs, 49 entities), `dump_lo` (loadouts,
perk files .134, perk list 0059.177), `dump_anim` + `resolved\` (10 sets), `graphs\` (141 reflected graphs), `work_kit\decomp_<g>.txt`
+ `edges_<g>.txt` + `c_<g>.txt` (141 decompiled graphs). Machine-readable: `gamevalues.json`, `gvpair.json`, `loadout_stats.json`,
`work\varvalues.json` (values graph **3505**), `work\clipmap.json`, `work_kit\herosettings_sojourn.json`, `work\models.json`.
Scripts: `scripts\` (Juno's, retargeted: `owinit.py`, `crawl.py`, `gamevalues.py`, `gvpair.py`, `varvalues.py` (G = 3505),
`tl_nodes.py`, `lostats.py`, `kit\decomp_out.py`, `kit\compact.py`, `kit\lean.sh`, `kit\herosettings_sojourn.py`, the `*_sojourn.py`
asset scripts; new: `clipmaps_sojourn.py`, `animrefs_sojourn.py`, `slideyaw_sojourn.py`). The `fx\` and `sounds\` copies were not
run and still name some of Juno's paths. TankLib scripts run with the kiriko-anim venv's interpreter (`-I`).

Extraction (offline, DataTool 2.25.0.0, logs in `logs\`), into `extract\`: `extract-unlocks "Sojourn|skin=Overwatch"
--extract-refpose` -> `Heroes\Sojourn` (41 model folders, 357 clip files under her three models, effects, sounds, entities),
`extract-hero-voice "Sojourn" --subtitles-with-sounds` -> `HeroVoice\Sojourn`, `"Sojourn|emote=*"` / `"Sojourn|voiceline=*"` ->
`Unlocks\Heroes\Sojourn\Emote` and `\VoiceLine`. Hero **`01EC.075`** (name 7975.07C), Gameplay3P **`2280.003`**, Gameplay1P
**`2281.003`**, hero colour (0.843, 0.243, 0.173) (H).

## Models (`work\assets.md`)
| | entity | model | look | set | notes |
|---|---|---|---|---|---|
| 3P | **2280** (hero `m_gameplayEntity`; mover, health, weapons manager 0015 + weapon graph 1A23, 12 graphs) | **DCA7.00C** (288 bones, no cloth) | **F27A** | **08C2** | (H) |
| 1P | **2281** (2280's first person) | **DCA8.00C** (138 bones: arms) | **F27B** | **08C1** | a separate arms model, unlike Juno's one model (H) |
| **Railgun** | **2D20** (child of 2280 and 2281 on hp **00C9**, var 4E30) | **D7B4.00C** (26 bones, 6 materials) | **EBB2** | **0AB6** | its own clips; same entity in both views (H) |
| Projectiles / deployables | railgun shot **2F98**; charged shot visual 2484 + tracer **3216** (250 m/s, no damage); Disruptor **31EF** (graph 5342) -> field **23E6** (graph 1443); Power Slide cosmetic 387D (694C with Friction Generators) | effects only | - | - | (H) |
| Select / showcase / intro / preview | select set **0F4F** (alias 0x477); 35F3 showcase (cloth config); 360E preview emote (set 0E76 + 4306); 4B1B intro (set 0FFA); set 0F42 (a second locomotion, L role); preview gun 484E / 4470 (D7B4) | DCA7 / D7B4 | F27A / EBB2 | | (H; roles M) |

- **The Railgun is its own model** (H), unlike Juno's body geosets: it rides **hp 00C9 = bone_0606**, a carrier bone under
  bone_0605 / Pelvis (rest (0, 1.157, 0.014) m), that her own clips move: not her hand. On top, its own set 0AB6 plays (a two-state
  machine per alias, sm[0] 1P / sm[1] 3P: M; the charged-shot lengths match each view, 0.733 s / 0.8 s). So it has its own clip map,
  `tools/clipmaps/sojourn_weapon.txt`.
- **Skeleton** (H, `.smd` rest pose): Pelvis 1.157, Head 1.557, Neck 1.467, camera bone 007D **1.605 m** (1P too); mover 2.0 / 0.5 /
  crouch 1.25.
- **Hardpoints the graphs use** (H): **0006** the muzzle on the gun (bone_0001, (0, 0.017, 0.413): railgun volleys x11, Overclock
  effects x9), 0007 the gun's origin (Overclock), **00C9** the gun's attach, **0002 her left wrist (Wrist_L): Disruptor Shot's launch**
  (1441 #353), 01DA / 001C pelvis / left foot (Power Slide effects), 000A / 000B weapon bones, 02A8 / 02A9 (L her left arm's parts),
  00A4 / 0012 / 0001 / 0004 (targets, root).
- **Geosets**: none on her body (no hide masks); the gun has one tiny group 1 (36-60 vertices: L an energy read-out or sight).
  **Cloth**: none in the models (H; the showcase 35F3 has a cloth config: L the line-up coat).

## Clip map (`work\clipmap.md`, `clipmap.json`; repo `tools/clipmaps/sojourn.txt` 235 + 2, `sojourn_1p.txt` 79, `sojourn_weapon.txt` 23)
Switches (H): df 0x18 locomotion; **df 0xE5 cat 0x24A = Power Slide** (1E6F #265 / #408): reload, melee, Disruptor and the Overclock
cast have **slide copies** keyed on it, with a 1D on **df 0x5E3** (-0.95 .. +0.8, M her lean) and a pitch triple on df 0x6BE; **df 0xC3
cat 0x169 / 0x155** the slide start's side (1E6F #303 / #245 by her move angle v7092: which side L); **alias 0x72D** the slide body in
eight direction categories 0x211-0x214 / 0x25D-0x260 (#690, which is which L); **df 0x1AC = Overclock** (2159 #374 = v842: the 3P
stance additive 834A, the gun's Overclock layer, empty); **df 0xF6 = energy / 100** (1A23 #430: the 1P energy additive 7D6C); df 0xB7
cat 0x68 an alternative idle (no setter of hers: L).
| State | 3P (08C2) | 1P (08C1) | Railgun (0AB6) |
|---|---|---|---|
| Stand | **6EE2** (1.0 s; 6EE1 alt) + 6EE0 / 84EF; held BCEC | **6A21** + 7D40 | **6E4D** (every branch) |
| Run 8-way / crouch | **68E8** .. 7FB7 (0.567 s) + adds; stop 83F8 / 7CEC, walk **7CED** .. (0.667 s) | **6C40** (0.533) + start / stop 6CA2 / 6CA1 | 6E4D at df 0x1A / 5.5 (3.0) |
| Jump | 6CFF -> 6D01 -> 6D00; land 6D02 | 6BD4 -> 6BD5; land 6BD8 | 6E4D |
| **Power Slide** | start **6C05** (0.8 s, 0x5E4) + 6CA9 / 6CA5, follow-ups 865B .. 866A; body 0x72D **8587 / 8599 / 85DB / 85DA / 85E1 / 85E2 / 85ED / 85EE** (0.333 s) | 0x5E4: 80ED (0.5) -> 70AB (0.167) -> 80F4 (0.333) | - |
| **Slide jump** | 0x5F3 **6D6E** (0.2) -> 6D70 -> 6D6F (0.4); land 8936 | 8958 (0.233) -> 8959; land 6BD8 | - |
| Railgun fire | layer 0x00A > 0x092: **6F09** first, **6F08** repeats (0.5 s) | **6C0B** first, 6C0F / 6C11 / 8490 repeats (0.533) | 1P 7E24 / 7077; 3P **848B** |
| Charged Shot | 0x093 **6F0A** (0.8 s) | 0x16F **6C4A** (0.733 s) | 1P 7031; 3P **8480** |
| Energy | - | 7D6C weighted df 0xF6 | 4-state machine 7030 / 8096 / 6E8F / 7D71 (1P), 82C2-82C4 (3P): conditions L |
| Reload (0x004) | **7058** (1.3 s); slide 8678 .. 8676 | **6B33** (1.3) + 8113 / 8114 air, end 6B34 | 1P 6FCB / 3P **70D5** (1.3) |
| Quick melee (0x00D) | 7EF1 / 7EF3 / 7EEF (pitch) + 7EF2; slide 875C .. 8731 | **6B94** (0.7) + 6B95 | - |
| Disruptor (0x479) | 849D / 83CD / 849C (0.667 s) + 83D0; slide 86C7 .. 8697 | **6BD7** / **84A0** sliding (1.0); end 6F93 | - |
| Overclock cast (0x175) | **6FA9**; moving 83AC .. 83DE; slide 86E3 .. 86CE (1.0 s); stance add **834A** | none | Overclock layer (empty) |
| Hello / select | 7F35 + 7F4F; **C278** (3.83 s) / **C279** (14.7 s) | 7EFA + 84DB | - |

Played by (H, `work\animrefs.txt`): reload 1A23 #249 (#144 alias 0x004), quick melee 0043 #117, Disruptor 1441 #354 (0x479), slide
1E6F #245 / #303 (0x5E4), #670 (0x72D), slide jump #656 (0x5F3), Overclock 2159 #542 (0x175); **no node plays a fire clip**: the shots
play from the weapon layers (first shot child 0, repeats random from child 2: M, the shared fire template); Overclock's 8 s active
alias **0x8D1** (#567 / #568 / #644) is in **no** set: nothing plays during it but the stance and gun layers.

## Abilities (numbers from her values graph **3505** and the graphs that read them; detail in `work_kit\kit_sojourn.md`)
Graphs on Gameplay3P 2280 (H): weapons manager **0015**, weapon **1A23** Railgun + Charged Shot + energy; **1E6F** Power Slide; **1441**
Disruptor Shot (+ **5342** on the projectile 31EF, **1443** on the field 23E6); **2159** Overclock; **51EB** perks; **3505** values;
0033, 004B shared ult, 0043 quick melee, 08B6, 142E, 3DCE ult callout, 5016; **01C7** role / Sharpshooter / regen. The 56xx / 57xx
graphs and entity 66FA belong to another kit the crawl reaches (as in Juno's), not hers (H).
Buttons (H): PRIMARY **xD128396E** (Railgun, buffer 0.1 s), SECONDARY **xB42D90B7** (Charged Shot, 0.15 s), ABILITY 1 **xCBF0F815**
(Power Slide), ABILITY 2 **x2ADDA7B1** (Disruptor Shot), ULTIMATE **x2DFEDC52** (Overclock), RELOAD x5D2A8B7F, **JUMP x4F81E1BD** (the
slide jump, 1E6F #192); ability buffers 0.15 s. Loadouts (H): 01C7 Power Slide, 01C8 Disruptor Shot, 01C9 Railgun, 01CA Charged Shot,
01CB Overclock, 0DDD Subrole: Sharpshooter, 0D9F Hero Stats. **Health 225, armor 0, shields 0** (3505 #106: 04D8 / 04D7 / 04D9), melee
02D0 **40**, 5.5 m/s, regen delay 0B0F 6, **the hero-default mover**, **ult cost 0AFB 2300** (alt 4200), perk XP 12D5 1695 then 12D6
2540 (= 4235) (H values, M meaning). Hero options (file 0126.054, `herosettings_sojourn.json`, H): **only five per-button aim
sensitivities** v54211-v54215 (Primary, Secondary, Ability 1, Ability 2, Ultimate; 0-2, default 1.0): **no toggles or hold options**.

### Movement first (the owner's priority)
- **Base mover = the hero default** (STUCharacterMoverComponent on 2280 field-for-field identical to Juno's 5106, all 53 fields, H):
  5.5 / 4.95 / 5.5 m/s, crouch 3.0, ground accel 35, air accel 3.5, friction 15, jump field 6.2 (5.72 m/s measured, `movement.md`),
  gravity 17.5 m/s², terminal 30, max air 5.5, step 0.5, slope 45. **Nothing about her walking, running or plain jump differs from any
  hero; all of her feel is Power Slide** (H). Default falling-damage component, **no fall-damage node** in her graphs (H by search).
- **Power Slide (1E6F)**, ABILITY 1 (StateAbility #155, cooldown var v61):
  - **Duration v9110 = 04E2 1 s** (0.75 s with Dual Thrusters, 51EB #422) (H). Usable **on the ground or in the air** (#178 through
    v9570, M). Ends early on a wall: rays #684 / #685 set v2823 / v2824, and after 0.3 s #961 -> #963 end it (H nodes, M purpose).
  - **Speed: mover override #311** for the whole slide: **x786680F6 (the mover's speed-bonus field, H by 01C7 #2011) = 1CFC 1.6 x
    v7779** -> **+160 %, 5.5 x 2.6 = 14.3 m/s** at full v7779 (M). **v7779 ramps 0.4 -> 1.0 over the first 0.2 s** (#354 / #334), then
    **from 0.3 s before the end down to 0.3** (#305 / #306): 9.0 m/s at the start, 14.3 from 0.2 s, 8.1 m/s at the end (M; m_5BEFF040
    read as the chase time). About 1 s x ~13 m/s = **~13 m** on the flat (L, ours; W's 13 m).
  - **Fall capped at 2 m/s** while sliding (override x978C5453 = 2.0, Juno's hover field: a slide off a ledge sinks slowly, M);
    **the normal jump is off** (x5C571F43 = 0, M) and JUMP is the slide jump. Other override fields x7FA25A61 1, x4E596ED2 0,
    x493C433E 0, x9E374BEA 1, x292F571E 0, xD3421CDE 0 (meanings L).
  - Direction: movement node #292 along v2083 (her move-input direction on the ground, #289 / #296; v28958 in the air), m_81759372 4,
    with slope / step rays #496 (20 m), #610 (H nodes, M purposes). Forward with no input (M).
  - **In the air** (v417 airborne): #383 the fly-mode node STU_C1BA7545 (L) with **#387 x786680F6 = 1CFD 1.2** (H). Additive to the
    1.6 (**+280 %**, W's number exactly) or replacing it (+120 %): **M additive**.
  - **Cooldown v472 = 1CE9 7 s, written at the slide's end** (#368 onEnd -> #159 -> #826 -> #1035) (H nodes, M timing).
  - **Slide jump**: JUMP #192 while sliding (v699, #201 -> #193) (H):
    - **allowed when ground is within 2 m below** (#337: a ray 2 m from 0.5 m above her along the reversed ground normal, filter
      11D) **or within 0.15 s of losing it** (v568 counts up at 100/s while not grounded, #409) (M: the ray's sense);
    - **#411: vertical speed cancelled (x57643451 = 0, L) and the velocity entry xA634D7B9 = (dir.x x 6.5, 1CFA 17, dir.z x 6.5)**
      (H values) -> read as **added** (Juno's reading, M): **17 m/s up** and **+6.5 m/s along the slide** (14.3 + 6.5 = 20.8 m/s,
      W's 20.5). Dual Thrusters #1031: **(dir x 11, 13.75, dir x 11)** (14.3 + 11 = 25.3, W's 25) (H values);
    - **#190 / #200 for 0.25 s: gravity scale x5B83ECB5 = 1.4** (H meaning from Juno), xF808ACE8 1.15, x0A04B51B 1.25 (L meanings),
      then **the slide ends** (#209 / #430 v215 -> 0 within 0.25 s) and the slide's cooldown starts. Anim 0x5F3 (#656);
    - **rise** (L, ours): 17 m/s at 24.5 m/s² for 0.25 s (3.5 m) then 17.5 m/s²: about **6.9 m** (8.3 m if the 1.4 were ignored);
    - #993: 0.5 s with xD3421CDE = 0 after the jump (L).
  - Effects / anims: 0x5E4 start by side (#245 / #303), 0x72D body (#670); cosmetic 387D under her (#416 / #1044); physics push #689
    (props). Voice B90 / B91.
  - **Sharpshooter** hooks: message **2623** (#1087: v61 -= amount; with Dual Thrusters the recharge timer v38468 -= amount) and 21E2
    (#1082) for ABILITY 1 (H).
  - Stadium-only branches (gated by .134 not in her perk list): 0420 (#858), 266A (#1062 / #1070), 266B (#1121), 2666 (#1049), 2674;
    graphs 5F34 / 4562 (H by gating).

### The rest of the kit
- **Railgun** (1A23), PRIMARY held: volley #502, **one projectile a shot at 04C6 16/s**, entity 2F98, **150 m/s** (04D3), lifetime
  10 s, **9 damage** (04C5), size **2901 0.1 + global 28A6 0.075 = 0.175 m**, **no falloff fields** (the charged shot's are on
  #450), crit **x2** (1CB4 2 / 04EE 1.0, M), from the gun's muzzle hp 0006 (H). **Spread 0 -> 04C7 1.6 degrees over 1E55 12 shots**,
  recovering after 0.3 s in 0.25 s (#629 / #628; H values, M meaning); no vertical / horizontal split in her graph. **45 rounds**
  (04D4), **reload 1.2 s** (04C8): the magazine is full at **0.96 s** (#287: 1.2 x 0.8), firing locked to 1.2 s (#283); auto reload
  on empty (#1133) (H). After a railgun volley a **0.16 s** gate (#513 / #230) before the charged shot (M).
- **Energy** (1A23, H): **v87**, cap **v971 100** (#601), shown as v176 = v87 / 100 (df 0xF6). **Gain only from railgun damage (id
  47BE, #607) and never during Overclock** (#655 / #656 / #776): **+5 a hero hit, +10 a crit** (#597 / #1480); a non-hero / deployable
  **+1, +2 a crit** (5 x 1CB3 0.2, #649 / #650 / #1481); a barrier (tag B87, M) **+1** (#775 / #777); times a combat-mod multiplier
  v23105 (1, M), capped. **Decay**: each gain restarts #596 **wait 1E15 9 s**, then #590 **-191F 15/s down to 0**; paused (v38399,
  #933) during Overclock and a Friction Generators slide. Messages 2161 (+5) and 2B08 add energy but nothing of hers sends them (L
  Stadium). 04DE 0.5 and 1084 2.0 ("Energy Gain Ratio", "Plasma Grenade Stick Energy Gain Ratio"): set, read by nothing.
- **Charged Shot** (1A23), SECONDARY: **only with energy > 0** (#451; else the fail effect 0186); volley #450 **hitscan** (no speed:
  M), **damage 20 + energy** (#745: 1CE8 20 + (04C9 120 - 20) x v87 / 100: 21 .. 120, H formula), **falloff 1CE6 40 m to 1CE5 60 m,
  to x 1CE7 0.5** (H fields), size **2902 0.07 + global 28A2 0.04 = 0.11 m** (H), crit **x1.5** (04EF 0.5, M), a **pierce block** on
  #450 (count v14468 = 3: L meaning), the same with or without Overclock (H); damage attributed to 01CA, or **01CB during Overclock**.
  **Spends all energy** (#637 / #1656); then **v2447 = 0.65 s** before either fire can shoot again (#438 / #615 / #617, M). At 65+
  energy the gun glows (#610, effect 2833 at hp 0006). Tracer 3216 at 250 m/s (visual only). Max alt 28B1 115 under v61743 (Stadium, L).
- **Disruptor Shot** (1441), ABILITY 2: **cast 0.17 s** (#114 / #361), anim 0x479 (0.667 s); projectile #353 **31EF at 04D5 40 m/s**
  (H), size **2905 0.2 m**, from **hp 0002 (her left wrist)**; its graph **5342** opens the field **23E6** after **10FC 0.75 s or on
  contact** (#53 / #5, messages C2E / 59D / 5D4): **30 m at most** (H). **Field 1443**: starts **0.1 s** after it opens (#158), lasts
  **04DD 3.5 s** (#160), **radius 04D6 4 m** (#331), **90/s** (04CB, alt 100 not live) to enemies inside (#163, loadout 01C8; #165 the
  same for tag-3A targets: M non-hero), m_FB3F6CA9 = 1 (L: ignores armor, W). **No slow in the base kit** (H by search). **Cooldown 1CE4
  15 s, written 0.5 s after the launch** (#360 -> #355: M timing). Sticking to a target (#20 / #41 / #48) is Stadium (2561.134, H by
  gating). Sharpshooter's 2623 for ABILITY 2 is listened to (#507) but never sent to her (H).
- **Overclock** (2159), ULTIMATE (#363): phases v59 (#450): **1 cast, wait 0.75 s** (#364 / #385; anim 0x175 1 s) -> **2 active,
  04CC 8 s** (#370) -> **3 tail, 0.45 s** (#401) (H). **v842 = 1 through all three** (#457 / #373): energy **fills at 04DF 100/s** to the
  cap (1A23 #739 / #738), **no on-hit gain**, **no decay**, charged shots attributed to 01CB (H). #765 a combat-mod filter in the active
  phase (L: hack immunity). **No piercing tied to it** (H by search). Ult cost **0AFB 2300** (H). Stadium branches (0425 / 0441 / 296E:
  a 400 m beam, a 25 m blast, +s per hero hit) left out (H by gating).
- **Sharpshooter** (01C7, sub-role; H): on damage dealt to a hero (message 0012 #2829, filter 1) **on a crit** (#2827): send 2623 for
  **ABILITY 1** with **v242 x 23D7 0.0075** (#2832): **0.0075 s off Power Slide per point of crit damage** (a 180 headshot charged shot:
  1.35 s). JUMP (tag 0FB, #2825) and ABILITY 2 (tag 1157, #2953) need tags she doesn't have: **Power Slide only** (H). Whether v242 is
  the crit-boosted amount: M yes. No Survivor tag (1173), no old Damage passive (only behind `v61743 && !v76560`) (M).
- **Perks** (live list **0059.177** = [2642, 2560, 2562, 2679].134, H): minor **Overcharged 2560** (BC4) cap +50 in Overclock (v971 150,
  1A23 #1485), above 100 **drains to 100 at 20/s** after (#1487 / #1488): up to **170** damage; minor **Deceleration Field 2679** (CC6)
  **+0.25 move-speed reduction** on heroes in the field (1443 #319, M meaning) and status 2456 (#363): **25 % slow**; major **Dual
  Thrusters 2562** (BC6) **2 charges** (1E6F #1033), each recharging over 7 s (#822), **slide x0.75 = 0.75 s** (51EB #422), **jump
  (dir x 11, 13.75 up)** (#1031); major **Friction Generators 2642** (C9E) **+1 energy 75 times over the slide** (51EB #468 / #470:
  75 in a full slide, fewer if cut short, M), decay paused meanwhile (#446). Not live: 0420, 0425, 0426, 0428, 0441, 2561, 265F,
  2660, 2664, 2665, 2666, 266A, 266B, 2674, 2760, 296E (Stadium) and graph 0B1F (H by gating).

## Nuances a port must not miss
1. **225 health, no armor or shields; ult 2300**; the stock hero mover: all of her feel is Power Slide (H).
2. **Railgun: projectiles** (not hitscan): 9 a shot at 16/s, 0.175 m, **150 m/s, no falloff**, spread to 1.6 degrees over 12 shots;
   45 rounds, reload 1.2 s with the magazine full at 0.96 s (H).
3. **Energy only from railgun hits**: +5 hero / +10 crit / +1 object (+2 crit) / +1 barrier, cap 100, **none during Overclock**,
   decaying **15/s after 9 s** without a gain (H).
4. **Charged Shot: hitscan, 0.11 m, 20 + energy**, x1.5 crit, falloff 40-60 m to 50 %, piercing, needs > 0 energy and spends all of
   it, then a 0.65 s lockout of both fires (H/M).
5. **Power Slide: +160 % with a ramp** (0.4 -> 1 in 0.2 s, easing to 0.3 over the last 0.3 s), **fall capped at 2 m/s**, **1 s**,
   ground or air (air **+1.2 more**, M additive), ends on walls; **cooldown 7 s from its end** (H/M).
6. **Slide jump: 17 m/s up + 6.5 m/s added along the slide** (Dual Thrusters 13.75 / 11), only with ground within 2 m or 0.15 s
   coyote, gravity x1.4 for 0.25 s, ends the slide (H values / M meanings).
7. **Disruptor: from her left wrist**, 0.17 s cast, 40 m/s, opens on contact or at 0.75 s (30 m), then **0.1 s, 3.5 s, 4 m, 90/s**, no
   base slow; cooldown 15 s written 0.5 s after launch (H/M).
8. **Overclock: 0.75 s cast + 8 s + 0.45 s tail**, energy 100/s the whole time from the cast, no on-hit gain, cap 150 with Overcharged
   (H). No clip plays during its 8 s (0x8D1 in no set): only the stance and gun layers (H).
9. **Sharpshooter: 0.0075 s per crit damage point, Power Slide only** (H).
10. **The Railgun is a separate model on a carrier bone (hp 00C9)** with its own two-state clips per alias; no fire StateAnim: shots
    play from the weapon layers; every other action has **slide copies** keyed on df 0xE5 cat 0x24A (H).
11. **No hero options** beyond aim sensitivities: no toggles to reproduce (H).
12. Value names mislead: "Plasma Grenade ..." = Disruptor Shot; 1CE5-1CE7 "Weapon Secondary Crit Multiplier" = the charged shot's
    falloff (60 m, 40 m, 0.5); 1CB2 "Weapon Primary Crit Multiplier" = 5 energy a hit; 2902 "Weapon Secondary Crit Multiplier" = the
    charged shot's 0.07 m size; 2905 "Plasma Grenade Projectile Speed" = the Disruptor's 0.2 m size; 10FC "Maximum Travel Distance" =
    0.75 **seconds**; 191F "Energy Gain Ratio" = the 15/s decay. Read graphs, not rows.

## Disagreements against sojourn.md (data build 2026-10-06)
- **Settled by D**: Railgun **150 m/s** (W changed 160 -> 150 on 2026-03-12, no BZ); Disruptor **3.5 s, 4 m** (W's unsourced edits are
  right); ult **2300** (BZ's +10 % on 2100 = 2310 is rounding); Charged Shot **0.11 m** ("large" hitscan, W right); Sharpshooter **per
  crit damage point** (BZ / W's table right, W's infobox "of base cooldown" wrong); piercing is base kit (W's old Overclock text stale);
  Railgun **no falloff**.
- **D adds what no public source has**: the slide's speed ramp and 2 m/s fall cap, wall end, cooldown from the end; the slide jump's
  2 m / 0.15 s window and 0.25 s x1.4 gravity; Overclock's 0.75 s cast and 0.45 s tail with energy filling from the cast; Disruptor's
  0.17 s cast, 0.1 s field delay and cooldown from 0.5 s after launch; the 0.16 s / 0.65 s fire lockouts; the energy decay pausing.
- **Read differently**: the slide jump **adds** 6.5 m/s along the slide (D, M) where W gives a fixed 20.5 m/s forward (the sum at full
  speed); the air slide **1.6 + 1.2** (M additive) is W's +280 %. A Workshop recording would settle both (Open).
- **Not settled**: the Railgun's 1.6 / 0.64 vertical / horizontal spread (only 1.6 in her graph: a weapon-side field, M); her hit
  volumes (not read).

## Not found
The Railgun's vertical / horizontal spread split; the meanings of the slide override's x7FA25A61 / x9E374BEA / x4E596ED2 / x493C433E /
x292F571E / xD3421CDE and the jump override's x0A04B51B 1.25 / xF808ACE8 1.15, and the fly-mode node STU_C1BA7545 (L); whether the
air bonus 1.2 adds to or replaces 1.6 (both write x786680F6); the pierce block's exact meaning (m_C5143E2E, count 3 / 1 under v66597);
the senders of energy messages 2161 / 2B08; v61743 (L Stadium) and v232 (energy forced full); whether she can shoot during the slide
(no gate either way in 1A23; W says yes); the ult charge rates (shared 004B, not read); which of the eight slide-body categories is which
direction and which side 0x155 / 0x169 is (no yaw in the clips, `slideyaw_sojourn.py`); the gun's energy-state conditions; df 0xB7 cat
0x68's setter; the 3P aliases 0x764 / 0x72B / 0xA33 and 1P 0x6D1 / 0x6D2 / 0x726 (played outside her kit or by nothing); set 4306's
clips (19F7B-1A251) and the hero file's C395 / C0E2 (not extracted); 20 extracted 3P clips in no set (listed in `work\clipmap.md`);
slot directions on her feet (not measured); her hit volumes; effects and emotes (extracted, not mapped). Her sounds and voice set
(01C2.05F) are mapped since 2026-11-02: [sojourn-sounds.md](sojourn-sounds.md).

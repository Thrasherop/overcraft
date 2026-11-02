# Juno data map, from Overwatch's game data (build 154088)

Read 2026-11-01 offline and read-only through TankLib/DataTool 2.25.0.0 (`Online=false`) on the archived CASC copy
`E:\Overwatch Archive\build-154088` (2026-10-06), with Overwatch and Battle.net checked closed (`Get-Process Overwatch, Battle.net`)
before every DataTool and TankLib run (`scripts\owinit.py` and `scripts\extract_juno.ps1` refuse on their own too). Nothing was
decrypted beyond DataTool's own reading, nothing was run but DataTool / TankLib, nothing went into the repo but text. Confidence:
**H** read in the data (a node, field, value, edge, string), **M** inferred (one link guessed, or a field read by analogy: mover
properties from Mercy's `gaps2.md` table, velocity entries from Lúcio's `wallride-data.md`), **L** guess. Node refs are `graph #uid`;
vars `vNNN`; values by their `.130` id. Public numbers: `juno.md` (a separate pass). Research folder:
`E:\Overwatch Archive\research\juno\` (paths below are relative to it).

Detail behind this map: `work_kit\kit_juno.md` (every ability node by node), `work\assets.md` (entities, models, hardpoints),
`work\clipmap.md` + `work\clipmap.json` (every clip by state). Dumps: `dump` (hero 0365.075), `dump_ss` (crawl from the hero following
003 / 01B: 136 graphs, 46 entities), `dump_lo` (12 loadouts, 4 perk files .134, perk list 0065.177), `dump_anim` + `resolved\` (8 sets,
70 trees), `graphs\` (136 reflected graphs, `tl_nodes.py`), `work_kit\decomp_<g>.txt` + `edges_<g>.txt` + `c_<g>.txt` (136 decompiled
graphs). Machine-readable: `gamevalues.json`, `gvpair.json`, `loadout_stats.json`, `work\varvalues.json` (values graph **4448**),
`work\clipmap.json`, `work_kit\herosettings_juno.json`, `work\models.json`. Scripts: `scripts\` (copies of Brigitte's, retargeted:
`owinit.py`, `crawl.py`, `gamevalues.py`, `gvpair.py`, `varvalues.py`, `tl_nodes.py`, `lostats.py`, `kit\decomp_out.py`,
`kit\compact.py`, `kit\lean.sh`, `kit\herosettings_juno.py`, the `*_juno.py` asset scripts; new: `hero_juno.py`, `extract_juno.ps1`,
`uniq_juno.py`, `clipmaps_juno.py`). TankLib scripts run with the kiriko-anim venv's interpreter (`-I`).

Extraction (offline, DataTool 2.25.0.0, logs in `logs\`), into `extract\`: `extract-unlocks "Juno|skin=Overwatch" --extract-refpose`
-> `Heroes\Juno` (72 models, 424 clip files, 118 effects, 2948 sound files, 21 entities + the named ones), `extract-hero-voice "Juno"
--subtitles-with-sounds` -> `HeroVoice\Juno` (865 .ogg), `"Juno|emote=*"` / `"Juno|voiceline=*"` -> `Unlocks\Heroes\Juno\Emote` (22)
and `\VoiceLine` (27 .ogg). Hero **`0365.075`**, Gameplay3P **`5106.003`**, Gameplay1P **`5108.003`**, hero colour (0.447, 0.122,
0.639) (H).

## Models (`work\assets.md`)
| | entity | model | set | notes |
|---|---|---|---|---|
| 3P | **5106** (hero `m_gameplayEntity`; mover, health, weapon 4445, graphs) | **0159.118** look **0939.119** (362 bones: 290 + 72 cloth) | **12AF** | (H) |
| 1P | **5108** | the same **0159.118**, look **093A.119** | **12B2** | one model, two looks, as Kiriko / Lifeweaver (H) |
| Mediblaster | **part of the body**: geoset group 4 (= the preview model 167A8's mesh); small piece 5ADB (167A9, 1 bone) on hp **01B5** (var E709) | | | muzzle hp **0006** (H) |
| Torpedo pod | **part of the body**: geoset group 2 (= preview 16686's mesh) | | | launch hp **048D / 048E** on bones 0182 / 0183 at (+-0.038, **1.351**, **-0.191**): her shoulders, behind her back (H) |
| Projectiles / deployables | Mediblaster 538E; torpedoes 534F (ally) / 54CB (enemy; Stadium 65C3); Hyper Ring **5530** (+ cosmetic 5BAE x3); Orbital Ray **5CCA** | none: effects only | - | (H) |
| Showcase / gallery / intro / preview emote | 5C50 / 5A2D (+ 6D85 @01AB) / 5917 / 5B06; preview weapons 5B28 (167A8) / 5B29 (16686) | 0159.118 look 0939 | 1311 / 1310 / 12B1 / 1311 | (H; DataTool names M) |

- **No separate weapon or prop model to animate** (H by vertex counts): the gun and the pod are her model's geosets, moved by her own
  clips. 167A8 / 16686 are the select-screen copies. So there is no `juno_weapon.txt`-style clip map.
- **Skeleton** (H, `.smd` rest pose): Pelvis 1.099, Head 1.469, camera bone 007D 1.521 m; mover 2.0 / 0.5 / crouch 1.25.
- **Hardpoints the graphs use** (H): 0006 muzzle, 048D / 048E torpedoes, 0019 / 001A boots (Overboots effects), 001B / 001C (hover jets),
  002C / 002E (glide jets), 0002 (Hyper Ring launch: not in her model's table, L engine default), 01B5 (the gun piece), 0001 (targets).
- One cloth piece, 124 points (L: coat tails / skirt).

## Clip map (`work\clipmap.md`, `clipmap.json`; repo `tools/clipmaps/juno.txt`, `juno_1p.txt`)
Switches (H): df 0x18 locomotion; **df 0x583 cat 0x1E3 = after a double jump** (4441 #537), **cat 0x24B = hovering** (M node), default a
plain jump; **df 0xE5 cat 0x257 = Glide Boost** (47B7 #806; 1P df 0x583 cat 0x1B9, #689); df 0xAE7 / 0x1A0 the torpedoes' 0x6E / 0x1F8 /
0x82; no df 0xE4 (one weapon).
| State | 3P (12AF) | 1P (12B2) |
|---|---|---|
| Idle / run 8-way / crouch | **E94E** / **E954**-EBCB (0.933 s) / EBE1 + F1B2-F1B5 (2.0 s) | **E98C** / **E9A6** / F262 |
| Run reversal (0xA50-0xA53) / stop | EEF3 / EEF5 / EF21 / EF22 (0.467) / F4A9 / F4BC / F4BE / F4BD | EE91 / EE92 |
| Jump | ECDA -> ECDC -> **ECDB**; moving 8-way F32F.. / F331.. / F330.. | E9A8 -> E9A9 |
| **Double jump** | **EE48** -> EE76 -> **EE75** (1.0 s loop); moving 8-way F432.. / F434.. / F433.. | EB92 -> EB93 (M) |
| **Hover** | start 0x9FF 2D **F3C0** + 8 (0.667); loop 0x13D 2D **F3BE** + 8 (2.0) | 0x5CC F286 (additive) |
| Land | 0x01A additives EB8B / F15D / F15E / F15C (no base clip) | E9AA |
| **Glide Boost** | start 0xE5 2D F12B.. (0.567); loop 0x02F **F124** / 8-way **ED20**.. (2.0); stops F5AB.. (0.5); double jump in glide 0x13E F105.. (0.667) | 0xE5 ED27-ED2A, 0x606 ED2B, 0x13E ED2C |
| Mediblaster | **no fire clip** (0x0A5 has no item); reload **F6C3** (1.567) | reload E9B6 |
| Quick melee | F1DD (ground, 0.767) / F087 (air, 1.0), pitch triples | E9B8 |
| Torpedoes | base adds F0AF (lock start) / F0E7 + F788 (lock) / F0B1 (fire) | 0x165 EA1D / EA1E / EA20; pods EF94 / EF95 |
| Hyper Ring / Orbital Ray | F01A (+ F09E / F09F) / F67A ground, F153 air (0.833) | E9BA / EA1C |
| Hello / select | F309 / F393 intro, F394 hold | EEA9 |

## Abilities (numbers from her values graph **4448** and the graphs that read them; detail in `work_kit\kit_juno.md`)
Graphs on Gameplay3P 5106 (H): weapon manager **0015**, weapon **4445** Mediblaster; **4441** Martian Overboots; **47B7** Glide
Boost; **4695** Pulsar Torpedoes (+ **4795** heal on allies, **47DD** per torpedo, **4B99** lock per target, 4F0A); **4857** Hyper Ring
(+ **4858** on the ring); **4A44** Orbital Ray (+ **501A** on the ray, **54A6** damage amp, 5540 Stadium); **522E** perks; **4448**
values (+ 3361); 3DCE ult callout, 004B shared ult, 0033, 0043 quick melee, 08B6, 5016 (shared, also on the 66FA kit); 01C7 role /
Survivor / regen (reached from 004B). The 56xx graphs belong to entity 66FA's kit (another hero's / NPC kit), not hers (H).
Buttons (H): PRIMARY **xD128396E** (Mediblaster; fires the torpedoes when locked), SECONDARY **xB42D90B7** (Pulsar Torpedoes), ABILITY 1
**xCBF0F815** (Glide Boost), ABILITY 2 **x2ADDA7B1** (Hyper Ring), ULTIMATE **x2DFEDC52** (Orbital Ray), RELOAD x5D2A8B7F, QUICK MELEE
xD19BBFFC, **JUMP x4F81E1BD** (double jump, hover), CROUCH xB0EA7E79 (optional hover); every ability buffer 0.15 s. Loadouts (H): 03E2
Mediblaster, 0408 Pulsar Torpedoes, 0417 Glide Boost, 041A Martian Overboots, 041E Hyper Ring, 042F Orbital Ray, 0DA9 Subrole:
Survivor, 0DB5 Hero Stats. **Health 75 + shields 150 = 225** (4448 #119: 0BA4 / 0BA6; armor 0BA5 0), melee 02D0 **40**, 5.5 m/s,
**the hero-default mover**, **ult cost 0B1B 2900** (alt 5200; to 004B as 0152), perk XP 12B9 2110 then 12BA 3165 (M).
Hero options (file 0126.054, `herosettings_juno.json`, H): **Toggle Glide Boost v52286 ON**, **Toggle Martian Overboots Hover v58213
off** (hold), **Crouch Activates Martian Overboots Hover v58214 off**, **Toggle Pulsar Torpedoes v53180 ON**, Swap Pulsar Torpedoes
Toggle Button Inputs v55397 off, Hide Glide Boost Cancel Text v56587 off, Friendly Aim Assist Strength v58446 1.0 / Health Threshold
v72851 0.99, vibrations v56793-v56801.

### Movement first (the owner's priority)
- **Base mover = the hero default** (STUCharacterMoverComponent on 5106, every field, H): 5.5 / 4.95 / 5.5 m/s, crouch 3.0, ground
  accel 35, **air accel 3.5**, friction 15, **jump field 6.2** (5.72 m/s measured, `movement.md`), **gravity 17.5 m/s²** (1.75 x 10),
  terminal 30, max air speed 5.5, above-max decay 2.0, air drag 0.35, step 0.5, slope 45. **Nothing about Juno's walking, running or
  plain jump differs from any hero** (H).
- **Martian Overboots (4441)**:
  - **Double jump**: JUMP pressed in the air (**from 0.1 s after leaving the ground**, #244: M), once per airtime (v181 = 1; **2 with Lift
    Off**, #652 H), refilled on landing (#466 on the mover's airborne output v417: M). The action #570 / #248: **vertical speed cancelled
    (x57643451 = 0, L) and a jump at x1.8 (xDF7A3726 = 1.8, H value; Lúcio's "jump multiplier" reading M)**: about **10.3 m/s up** (1.8 x
    5.72), **~3 m of rise** from rest (M). Horizontal speed: no node changes it (H; v22437 = velocity x (0.6, 0, 1) is written, never
    read). Anim state 0x1E3 until she lands; messages 1E66 / 260A.
  - **Hover**: **hold JUMP in the air** (default; toggle and crouch options above) -> **fall speed capped at 1A5E 3 m/s** (#382: mover
    property x978C5453 = Mercy's Angelic Descent fall cap, H meaning). It only engages when **no ground is within 2.5 m below her**
    (#376 ray 2.5 m down from 0.25 m up, radius 0.4: M on the ray's sense). It does not stop upward speed (no node: M). Anims 0x9FF start
    (skipped within 0.5 s of an earlier hover) -> 0x13D loop; effects on hp 001B / 001C.
- **Glide Boost (47B7)**, ABILITY 1:
  - **Start**: a **14 m/s horizontal impulse** in her move-input direction (forward with no input) is **added** (#589: vector entry
    xA634D7B9 = norm(dir) x v9229 14, H expression; "adds" M), vertical speed cancelled (x57643451 = 0, L); **0.15 s** input lock
    (#590 / #591); **Survivor message 26FD** (#882); anim 0xE5 0.567 s.
  - **During (up to 13BB 4 s)**: a mover override (#528) = the default with **air accel 12**, **max air speed 9.0**, m_142DA8D4 **3.0**
    (H); **+40 % move speed** (v27756 += 1A38 0.4, #742: 7.7 m/s, M channel); **knockback received -50 %** (x0A04B51B = 1DF3 -0.5,
    #774, H value, Mercy's table H meaning); jump input off (x5C571F43 = 0, M); **gravity off**: x5B83ECB5 (gravity scale, H meaning) =
    0 for the first **0.7 s** (#780) and whenever there is no move input (#761), then a fly-mode node (#757, L) and an **altitude hold**
    at the height she started (#711 y + height / 2) steered by terrain probes (#812-#816) through x830FF3AA **+3 / -3** (rise over a step
    / follow a drop: H values, L meaning).
  - **Double jump while gliding** works (#732 on 260A): she rises, then the glide resumes at the new height after the ceiling check (#783:
    M).
  - **End**: 4 s, or **ABILITY 1 again after 0.4 s** (toggle on, default; hold mode: release) (#793 / #795 / #799). **Cooldown 1625 6 s
    from the end** (#538, M timing). Anim 0x8E3 (no clip).
- **Fall damage**: her own STUFallingDamageComponent (heavy effect 01E4, m_43A3CE29 0.49: L meaning); no fall-damage node in her graphs.

### The rest of the kit
- **Mediblaster** (4445): PRIMARY held; **12-shot volleys at 34 shots/s (0.353 s) + 0.45 s recovery = one volley per 0.80 s** (0.3 s
  after the last round); per projectile **7.5 damage / 6 heal** (1381 / 1382: 90 / 72 per volley); size **enemy 0.02 m, ally 0.22 m**;
  crit x2 (M); **180 rounds**, **reload 1.5 s with the magazine full at 0.75 s**; **falloff 25-35 m exists as values but no node reads
  them** (H by search); speed not in her graphs (Not found); spread 0 -> 0.8 over 5 shots (M). **Faster Blaster**: while gliding,
  continuous fire at **22 shots/s**, 0.1 s recovery (H).
- **Pulsar Torpedoes** (4695), SECONDARY: lock-on toggle by default; tracks heroes (allies and enemies) within **40 (+5) m** in sight, **no
  target cap** (M); **lock time per target lerp(0.35 s at <= 5 m, 1.0 s at 40 m)** (4B99 #193, H formula); **Familiar Vitals** x0.65 on
  allies; **max lock window 4 s**; fire with PRIMARY (or the toggle / release), **one torpedo per target every 0.08 s** from hp 048D /
  048E; **allies: 24 m/s homing, 85 on hit + 50 over 2.5 s** (4795); **enemies: launched at 24 x 0.3 = 7.2 m/s** (expression H, meaning
  M) then a homing motion, **85 on hit**, crit x2 (M); the burst carries 0 damage outside Stadium. **Cooldown 10 s, 2 s if nothing was
  locked**; **Locked On**: -1 s per enemy hit (522E, H).
- **Hyper Ring** (4857 / 4858), ABILITY 2: **0.55 s cast** (anim 0x479), the ring at **0.25 s** from hp 0002, flying **35 m/s for 0.5 s
  = 17.5 m** (or to a wall), then still for the rest of **6 s**; **radius 4.5 m**, facing her aim (M); **allies through it: +50 % move
  speed for 3 s**, each once; **cooldown 14 s** from the cast's end (M); ungated 522E #431: -1 s per new ally through it (H nodes, L
  live).
- **Orbital Ray** (4A44 / 501A / 54A6), ULTIMATE: **charge to 0 at the press**; 0.8 s cast (0x175), the ray at **0.5 s, 4 m ahead**,
  moving forward along her facing (toward 100 m) at **up to 2.25 m/s** (0.5 s ramp up, slowing over the last 1.25 s), following the
  ground; **8 s**, **radius 8 m**, a column of any height (200 m, M); **allies inside: 85 HP/s and +30 % damage**; Juno herself: filter
  allies (M yes).
- **Survivor** (01C7 tag 1173): **only Glide Boost** sends 26FD (H by search): regen starts with a 0.25 s minimum (23DA); the double jump
  doesn't trigger it.
- **Perks** (live list 0065.177, H): minor **Familiar Vitals 2590** (BD1) ally lock 35 % faster; minor **Locked On 25E4** (C7E) -1 s
  torpedo cooldown per enemy hit; major **Faster Blaster 265D** (CB7) continuous Mediblaster in Glide Boost (22/s); major **Lift Off
  2721** (D2A) triple jump.

## Nuances a port must not miss
1. Health is **75 + 150 shields** (225), ult **2900** (H).
2. Her ground movement and plain jump are the **stock hero mover**: all of Juno's feel is in the Overboots and Glide Boost (H).
3. The double jump **resets vertical speed and jumps at 1.8 x** a normal jump (~10.3 m/s, M), only from 0.1 s into the air, once
   (twice with Lift Off), and doesn't touch horizontal speed (H/M).
4. Hover is **a 3 m/s fall-speed cap while JUMP is held** (hold by default), not a stop; it needs **2.5 m of air below her** (M) and does
   not slow a rise (M).
5. Glide Boost **adds a 14 m/s horizontal burst** in the input direction, then **flies level with gravity off** for up to 4 s at **+40 %
   speed** (air max 9, air accel 12), **-50 % knockback**, steering over steps; a double jump mid-glide lifts her and the glide goes on;
   cancel after 0.4 s with the same key; **cooldown starts at the end** (H values / M meanings).
6. Glide Boost is her **only Survivor trigger** (H).
7. The Mediblaster is a **12-round volley every 0.8 s**, hits allies (heal 6, bigger 0.22 m projectiles) and enemies (7.5, 0.02 m);
   its falloff values are read by nothing (H).
8. Torpedoes: **no target cap**, lock time by distance (0.35-1.0 s), 4 s window, fire with PRIMARY; allies get 85 + 50 over 2.5 s,
   enemies 85; 2 s cooldown if nothing locked (H).
9. Hyper Ring travels **17.5 m in 0.5 s** then hangs for 6 s; +50 % speed 3 s per ally pass (H).
10. Orbital Ray moves forward slowly (2.25 m/s, M) for 8 s, 8 m radius, 85/s and +30 % damage (H).
11. The gun and the torpedo pod are **geosets of her body**: no prop models, no fire clip; reload, melee and abilities have glide /
    hover / double-jump variants keyed on df 0xE5 / 0x583 (H).
12. Value names and panel rows mislead again: 1AF0 is named "Pulsar Torpedoes Range" but is the 4 s lock window (v567); 295F
    "Mediblaster Damage" is the ally projectile's +0.2 m size; 2AEF "Ally Projectile Size" sizes the enemy torpedo; the panel's
    "Damage Per Volley 7.5" is per projectile (12 per volley, M); the falloff row is applied by nothing. Read graphs, not rows.

## Disagreements to check against juno.md (data build 2026-10-06)
Not compared in this pass (juno.md is being written in parallel). Points to check first: the double-jump height (1.8 x), the 3 m/s
hover cap and its 2.5 m ground clearance, the 14 m/s glide burst and +40 % speed, Glide Boost's 6 s cooldown from the end, the
Mediblaster falloff (data: unused), torpedo damage / heal (85 / 85 + 50), Hyper Ring 14 s, ult cost 2900, and whether Hyper Ring's
"-1 s per ally" (522E #431) is live.

## Not found
The Mediblaster projectile's speed (volley vars v36284 / v22138 unset in her graphs); whether any falloff applies (1A54 / 1A55 read by
nothing); the hover ray's sense (hit vs clear: M); the meanings of x830FF3AA, xB50D6922, x57643451 and the fly-mode node STU_C1BA7545
(L); the glide's exact resulting speed after the 14 m/s burst (mover decay above max air 9: L); the enemy torpedo's speed after its
first motion; whether Juno herself gains Hyper Ring speed / Orbital Ray healing (filters read as allies: M yes); the ult charge rates;
the voice set, voice lines' text and sounds (extracted, not mapped); the emotes' clips and props (22 extracted, not mapped); the
showcase / intro clips' roles; slot directions on her feet (not measured).

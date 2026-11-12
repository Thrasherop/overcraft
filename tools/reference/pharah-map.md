# Pharah data map, from Overwatch's game data (build 154088)

Read 2026-11-11 offline and read-only through TankLib/DataTool 2.25.0.0 (`Online=false`) on the archived CASC copy
`E:\Overwatch Archive\build-154088` (2026-10-06), with Overwatch and Battle.net checked closed (`Get-Process Overwatch, Battle.net`)
before every DataTool and TankLib run (`scripts\owinit.py` and `scripts\extract_pharah.ps1` refuse on their own too). Nothing was
decrypted beyond DataTool's own reading, nothing was run but DataTool / TankLib / Blender (headless, no game client running), nothing
went into the repo but text. Confidence: **H** read in the data (a node, field, value, edge, string), **M** inferred (one link guessed, or
a field read by analogy with [juno-map.md](juno-map.md), [dva-map.md](dva-map.md)), **L** guess. Node refs are `graph #uid`; vars `vNNN`;
values by their `.130` id. Public numbers: [pharah.md](pharah.md). Research folder: `E:\Overwatch Archive\research\pharah\` (paths below
are relative to it).

Detail behind this map: `work_kit\kit_pharah_weapon.md` (Rocket Launcher, Barrage, Concussive Blast, perks) and `work_kit\kit_pharah_move.md`
(Jump Jet, Jet Dash, Hover Jets, mover, Recon), the two node-by-node readings this map is made from; `work_kit\c_<g>.txt` / `decomp_<g>.txt`
/ `edges_<g>.txt` (18 decompiled graphs; `scripts\kit\lean.sh <g>`), `graphs\` (all 109 crawled graphs flattened), `work\clipmap_pharah\
compact_<set>.txt` (every clip by state), `work\animrefs.txt` (which node plays which alias), `work\ents_pharah_out.txt` (every entity),
`work\gamevalues_out.txt` (every value with its readers), `work\varvalues_3B6E_out.txt`, `work\lostats_out.txt` (the loadouts' stat rows),
`work\intro_clips_out.txt` (the highlight intros' clips). Machine-readable: `gamevalues.json`, `gvpair.json`, `loadout_stats.json`,
`work\varvalues*.json`, `work\clipmap.json`.

Scripts: `scripts\` = D.Va's, copied and retargeted 2026-11-11 (every `*_dva.py` renamed `*_pharah.py`, paths to `research\pharah`); changed:
`hero_pharah.py` (finds "Pharah"), `extract_pharah.ps1`, **`clipmaps_pharah.py`** (her aliases, a RENAME table to Overcraft's state names,
the hover's diagonal 2D points); new: `intro_clips_pharah.py` (highlight intro clip headers), `export_models.sh` (the four Blender exports).

Extraction (offline, DataTool 2.25.0.0, logs in `logs\`), into `extract\`, every run clean the first time: `extract-unlocks
"Pharah|skin=Overwatch" --extract-refpose` -> `Heroes\Pharah` (39 model folders, 242 clips referenced, 227 extracted; 3.3 GB),
`extract-hero-voice "Pharah" --subtitles-with-sounds` -> `HeroVoice\Pharah` (2.6 GB), `"Pharah|emote=*"` -> `Unlocks\Heroes\Pharah\Emote`
(15 emotes: Listen Up, Mic Drop, Salty, Rocket Guitar, Flair, Cheer, Chuckle, Flourish, Heroic, Knuckles, Take a knee, Strut, Shuffle,
Justice Reloaded, Tutting), `"Pharah|voiceline=*"` -> `\VoiceLine`, and `"Pharah|highlightintro=*"` -> `\HighlightIntro` (6 intros, below).
6083 files, 6.2 GB. Hero **`0008.075`** (name 0027.07C, category 084E = Damage), hero colour (0.0, 0.345, 0.737), m_30A61B59 1.8 (L: her
height for the UI).

## Ids, entities, models (H)
- **Hero entity 0032.003** (Gameplay3P): mover, health, falling damage, weapons manager **0015** -> weapon graph **0023**; statescript
  **3B6E** (values), **0025** Hover Jets, **0027** Jump Jet, **4B92** Jet Dash, **002B** Barrage, **007B** Concussive Blast, **51E9** perks,
  shared 0033 (health pools / spawn), 004B (ult charge), 0043 (quick melee), 08B6, 0074 (hints). Base model 06A0, set **001B**.
- **1P entity 00B3** (Gameplay1P): base model 06A4, set **003D**; eye fields **m_E29993A5 1.35** standing, **m_816B830F 0.945** crouched (M:
  Juno's 1.45 / 1.015 read the same way), m_CF5055F1 0.7.
- **Rocket Launcher 003B.003** on hp **000A** (Weapon_R), var 1B9F (0023 #2 StateCosmeticEntity on 000A / 000B, handle v49): base 06A1, set
  **003F**; its own model.
- Skin "Overwatch" swaps them to: **3P F2A5.00C** (228 bones, 14 meshes; Gameplay3P.owentity), **1P F2B9.00C** (63 bones; Gameplay1P),
  **launcher F131.00C** (18 bones; "Rocket Launcher-5441"), **rocket 12F5B.00C** (no bones, 366 verts, 0.03 x 0.109 m; on projectile entities
  0033 and 7769).
- Projectiles: rocket **0033.003** (0023 #17), Barrage mini-rocket **0052.003** (002B #21), Concussive Blast **010F.003** (007B #52 / #356),
  Rocket Salvo mini-rocket **7769.003** (51E9 #691). 6520 / 5340 / 59AC: a pull variant gated on 250A.134, in no live perk list (not live).
- Loadouts: Rocket Launcher 000F, Jump Jet 000C, Hover Jets 000D, Concussive Blast 000E, Jet Dash 0445, Barrage 000B, Hero Stats 0D9E,
  Subrole Recon 0DAD; perks (0057.177 list): minor **28E7 -> E80 Concussive Force**, **2507 -> BAE Helix Shields**; major **2509 -> BB0 Fuel
  Stores**, **28C6 -> E66 Rocket Salvo**.
- Buttons: PRIMARY xD128396E (0023 #16), RELOAD x5D2A8B7F, ABILITY 1 xCBF0F815 Jump Jet (0027 #82), SECONDARY xB42D90B7 Jet Dash (4B92 #520),
  ABILITY 2 x2ADDA7B1 Concussive (007B #83), ULTIMATE x2DFEDC52 Barrage (002B #20), hold JUMP Hover Jets (0025 #21); every ability has the
  0.15 s input buffer. Options (M): swap Jet Dash / Hover Jets (v53835), toggle Hover Jets (v49184).
- Entity 66FA / 66FC (graphs 56xx / 57xx, voice 2B9) came in through the shared role graph 01C7: another hero's, not hers. 0C90 / 1882 are
  on 004F, a match-level entity.

## Base (H unless noted)
- **Health 0925 = 225**, shields 0B94 = 0, no armor (3B6E #7). Quick melee 02D0 40. Regen delay 0B0F 6 s, 125A -0.5 (Hero Stats rows).
- **Mover = the hero default in all 54 fields** (diff against Juno's 5106): **2.0 m tall, radius 0.5, crouch 1.25**, move 5.5 (202B), jump
  5.72 m/s, gravity 17.5 (field meanings from juno-map: M). No graph of hers changes gravity outside the hover push.
- **Falling damage**: STUFallingDamageComponent **m_43A3CE29 = 0.48** (Juno and D.Va 0.49; meaning L). **No safe-fall height and no fall node**
  in her graphs (H by search). Model height 1.795 m (F2A5 bounds), head hp 0012 at 1.637 m, 0081 (face) 1.71 m.
- **Ult cost 2200**: 0AF5 "Ultimate Charge Max" (alt 3500) (H value, M that it's hers: no reader in her graphs).
- Perk XP 12D1 1730 / 12D2 2600 -> minor 1730, major 4330 (3B6E #64) (M).

## Rocket Launcher (0023, values from 3B6E)
- **Splash 80** (0926; 3B6E #4) with rings **[0] 0.5 m (2390) x1, [1] 2.5 m (0B66) x0.308 (0B1D)** -> 80 full inside 0.5 m, **24.6 at 2.5 m**,
  linear between (M: the two-ring form). Tooltip "0.5 - 2.5 m" agrees (H).
- **Direct 40** (0927; impact component `m_6396149F`) -> 120 on a direct hit. Impact radius **28F8 0.2 + v54935** (the global large-projectile
  bonus, 0.12: M) = 0.32 m.
- **Self-damage share 0B68 0.25** (`m_8A3E3883`, M field) -> 20 -> 6.2.
- **Knockback** 1D9B **5** horizontal (`m_E33EFEE1`) / 1D9C **8** vertical (`m_10399AE7`, not m_583C208B: M), rings x0.8 (1D9D / 1D9E) at 0.5 m
  -> 0 at 2.5 m: max (4, 6.4) = **7.55 m/s**; **self x1.875** (0B69, `m_2FE97560`, M) = **14.15 m/s**.
- **Projectile 40 m/s** (28F9), lifetime 5 s (200 m); m_FEC435C6 100 (L: range). No gravity, no spread.
- **Recovery 0.8 s** between shots (#771 -> #2 v173 = 0.8), **0.55 s** after the shot that empties the magazine (0.8 - v71594 0.25), then the
  auto-reload (#4) (H).
- **Magazine 6** (0928; 3B6E #6). **Reload 1D94 1.5 s**: StateAnim #33 alias 0004 for 1.5 s; **#72 Wait 1.5 x 0.4 = 0.6 s -> #70 ammo = 6**;
  the reload state is left only when the anim ends (#33 onFinished -> #31) (M): **ammo in at 0.6 s, no firing until 1.5 s**.
- Ammo refill messages 007D (Barrage's cast) / 0013 (H).
- **Muzzle: the launcher's hp 0006** (`m_muzzles[0]` = v49 hp 0006.03C) (H). On F131 at (x -0.001, y 0.106, z 0.702) from its root, i.e.
  0.70 m out along the barrel and 0.11 m above the grip axis (OW axes: y up, z the barrel; H the empty). Where that is in her 1P view: below.
- No fire anim node; the recoil is the volley's (`m_5E4A4E85 x71E73343`, L). Unknown fields m_792F0B4D 30, m_E03B193A 50, m_1DB42474 20 (L).

## Jump Jet (0027, ABILITY 1)
- **A press, not a held charge** (H): StateAbility #82, no charge / hold state, no ChaseVar; it fires on activation.
- **Impulse #36**: **(1D9F 7, 1DA0 17, 1DA1 7)** x vec(input.x, 1, input.z): **17 m/s up + 7 m/s along the move input; straight up with no
  input** (H). x57643451 0.1 rising / 0 falling (M: vertical speed kept, a fall cancelled).
- **Duration 1E1E 0.85 s** (#109 Wait). **Cooldown 092C 14 s**, set when the jet ends (#379 / #380) (H; 092C is named "Rocket Boost Cooldown").
- **Fuel: +50 over 0.85 s** into overfuel (0025 #764 / #762: rate (max / 2) / 0.85 = 58.8/s per fill) (H), **no drain while it runs** (0025 #29).
- During it the hover push is 80 with fuel, 30 without (0025 #698 / #694). Message 1E66 (#325) = "movement ability used", arming Rocket Salvo (M).
  Side nodes (L): a 75 ModifyHealth in 4 m with filter 128 and a physics push r 2 force 15 at 0.25 s (#67-#69, #106: breakables / props, L).
- Anims: #345 picks **#65 alias 0030** (blend 0.5) or **#346 alias 002F**, category 01E3; **#366 alias 0671** (1P) (H nodes).

## Jet Dash (4B92, SECONDARY)
- **Press** (#520), or JUMP onGoingDown when swapped (#499).
- **Impulse #36: (1E29 19, 1E2A 8, 1E2B 19)**: **19 m/s along the move input + 8 m/s up; forward with no input** (#545) (H). x796661A6 0.3 (L).
- **Duration 0.85 s** (v9110 = Jump Jet's v215) (M). At 0.425 s (#501) a mover override #500: xD3421CDE 0 (input off, M), x02A51BD8 0.05,
  xED8D6672 18, then #539 xB5941741 9999 (L).
- **Cooldown 1A89 9 s**, set when the dash ends (#523) (H). 1A89 is also called "Rocket Boost Cooldown" and is a cooldown, not a speed.
- **Fuel: none** in the base kit (H); with Fuel Stores (2509, 0025 #758) +50 over 0.85 s. During it the hover state is on (v699) and the push 80 / 30.
- Anims: the same choice as Jump Jet (#65 0030 / #346 002F / #509 0671) (H nodes).

## Hover Jets (0025, hold JUMP)
- **Fuel max 0B18 100** (#491). **Overfuel max 150** (100 + 100 x 0BCB 0.5), **200 with Fuel Stores** (#730) (H expression).
- **Drain 1A6A 50/s** (#30, 2.0 s from full). **Regen 1E4F 45/s after 1E52 0.3 s**, on the ground only (#28 / #33, entry #129 on v545 =
  grounded, M). The tooltip's "Energy" row parsed only 1E52, which the graph uses as the delay (H).
- **Overfuel decay** (M): held **5 s** (#682), then drains to 100 at **30/s** (#681), 80/s while hovering (#688).
- **Push**: #650 / #63 STU_A9C4FF83, **0B9A 50 m/s² up with limit 0B99 2.9 m/s** (M meaning: adds only while she rises slower than 2.9); 55
  while falling with fuel; 80 / 30 during the jets. **Empty**: the push ramps to **7.5 over 0.35 s** (#655) and the **fall cap 1E54 8 m/s**
  (x978C5453, #160) (H values).
- **Speed +30 %** while she has fuel (1E36 on x786680F6, #160) = 7.15 m/s (H value, M field). x0A04B51B 0.2, xF808ACE8 0.1 (L).
- **Engaging**: in the air at once; from the ground after **0.2 s** of holding (#22) (M). Hover blend v1358 0 -> 1 at 2/s, back at 1/s;
  **> 0.1 anim category 022E** (#777), **> 0.25 alias 002F** (#776) (H).
- Barrage holds fuel at max (#634 / #635, v232) (M). Message 0013 fills it (respawn, M).

## Concussive Blast (007B, ABILITY 2)
- **Cooldown 092B 8 s** (3B6E #15), 1 charge (H).
- **Cast**: State #85: StateAnim **001E 0.5 s** + ChaseVar #257 over **0.1667 s** -> the volley; then #61 Wait **0.5 s** (H) = **0.167 s before
  it leaves, 0.5 s recovery**.
- **Projectile 28FB 60 m/s**, lifetime 5 s, size **28FC 0.15 + v54937** (0.075 global: M) = 0.225 m. **Muzzle hp 0038** (her left wrist; H).
- **Radius 0B7B 8 m**. **Direct 092E 30**; **splash 0B17 0** (+30 with Concussive Force) (H).
- **Knockback, explosion 1E2D 20 / 1E2E 5** (20.6 m/s), one ring at 8 m, **no falloff** (M); **direct hit + 1E32 10 / 1E33 3 = 30 / 8** (31.1 m/s) (H).
- **Self**: no self-damage (`m_8A3E3883` 0, H), self-knockback x1 (`m_2FE97560` 1, M).

## Barrage (002B, ULTIMATE)
- **Cast time 0** (#54 Wait v215 straight after the button) (M). **Duration 1A5D 2.5 s** (StateAnim #43 alias 0031 for it) (H).
- **30 rockets/s** (1E46; volley #21, m_A55BC904 999) -> 75 rockets (H).
- **Per rocket: direct 092A 10, splash 0929 25**, rings **0.5 m (1E37) x1 -> 1.5 m (1E38) x0.3 (1E47)** = 7.5 at the edge (H). Self share
  0.5 (0074 #331, M). **No knockback** (H).
- **45 m/s** (28FA), lifetime 5 s, size v54937 only (0.075, M). **Spread 0B7D 8°**. Spiral-like motion params (L).
- **Muzzles** cycle **0034, 0035, 0037, 0036, 0037, 0035, 0034, 0036** (her shoulder pods 0034 / 0035 and hip pods 0036 / 0037; H).
- **Movement**: x786680F6 = **24A8 0.8 = -20 %** (#377) -> 4.4 m/s (M field); #370 xB62AF71E = 0 (the node quick melee also uses: L, a lock).
- **Refills ammo on cast** (#149 sends 007D -> 0023 #32) (H); fuel held at max (0025 #634, M).

## Perks (51E9)
- **Helix Shields (2507)**: health **-125**, shields **+125** (#331 / #332) (H). Regen trigger: a rocket direct hit (component 06FF) on an
  enemy sends 0ED1 to self (#361 / #366) -> 0033 #131 (M); the rate in 0033 is not read.
- **Concussive Force (28E7)**: Concussive splash **+30** (#707), and the blast uses volley #356: **30 full to 4 m, 15 at 8 m** (H).
- **Fuel Stores (2509)**: in 0025: Jet Dash **+50 fuel over 0.85 s** (#758) and **overfuel max 200** (#730) (H).
- **Rocket Salvo (28C6)**: armed by Jump Jet (0027 #367) or Jet Dash (4B92 #108), not Concussive (M); a **5 s** window (M); the next rocket
  fires, 0.1 s later (#77), **2 mini-rockets (7769): direct 10 + splash 20 = 30 each**, 0.5 m x1 -> 1.5 m x0.3 (6), **40 m/s**, lifetime 10 s,
  spread 0.1, from the launcher's hp 0006 (H). Spiral 0.6 / 0.8 / 0.2 (L meaning).
- Orphans in 51E9 (no edge in): +25 fuel on a rocket direct hit, reload anim rate +0.5 for 3 s after a kill: removed perks or Stadium (L).

## Recon (sub-role passive, 0DAD)
- Reveal **23D8 3.5 s**: `graphs\0000000001CF.01B.json` #1197 StateWait (client-only), chain #1206 -> #1207 -> #1202 -> #1197 -> #1191 / #1190
  / #1187 (H edges; the half-health test not read). Not in 01C7.

## Muzzles and hardpoints (H: the .owmdl empties, `scripts\bounds_pharah.py`)
OW axes, metres, her rest pose (T-pose: the 1P F2B9's empties are the same numbers as the 3P's, so its rest pose is no help here, unlike D.Va's
missiles): x+ her left, y up, z+ forward.
- Weapon_R **000A** (-0.584, 1.001, -0.017), Weapon_L 000B (0.584, 1.001, -0.017).
- Launcher **0006** (the rocket muzzle) (-0.001, 0.106, 0.702) in the launcher's frame; 000B (-0.002, 0.028, 0.275) its fore grip (L).
- Concussive **0038** Elbow_L (3P 0.531, 1.183, -0.039).
- Barrage pods: **0034 / 0035** shoulders (3P ±0.273, 1.564, -0.052), **0036 / 0037** hips (3P ∓0.159, 0.886, 0.051) (on the 1P model 0034 / 0035
  sit at ±0.719, 1.939 and 0036 / 0037 at ±0.45, 0.886: the 1P model's pods are placed for the view, M).
- **Rocket muzzle in her 1P view** (M; `scripts\muzzle_1p_pharah.py`, Blender on pharah_1p.glb posed at `idle` 0234 frame 0): the
  launcher's grip, bone **Weapon_R, sits 0.151 m right, 0.253 m ahead, 1.346 m up** (at her 1.35 eye height); its frame's axes are OW's
  (y up, z the barrel, along her view within 12°). Hp 0006's offset (-0.001, 0.106, 0.702) in that frame puts **the muzzle 0.075 m right,
  0.945 m ahead, 1.483 m up: 0.13 m above the eye**. M: the launcher's root taken at Weapon_R with no offset (hp 000A sits on the bone's
  head in the rest pose) and the bone frame read as OW axes; check it against the drawn barrel in game.

## Clips by state (tools/clipmaps/pharah.txt, pharah_1p.txt, pharah_weapon.txt)
Each clip is named by the alias holding it (graph nodes above, H) and renamed to Overcraft's states (`was ...` in the comment).
| State | 3P (F2A5) | 1P (F2B9) | Launcher (F131) | Source |
|---|---|---|---|---|
| idle | 051A 0.7 s (dfB7 default; 0433 is cat68) | 0234 0.167 s | 1176 0.667 s | alias 1 / A0E (M default branch) |
| run 8-way | 0343 F, 03D7 FR, 03D6 R (strafe_right), 03D9 BR, 03BD B, 03DA BL, 03DB L (strafe_left), 03D8 FL; 0.667 s; add 03D2-03EE 0.2 s | 0236 0.667 s | - | alias 5 (H tree) |
| jump / fall | 04D0 0.1 s take-off, 04D2 apex, 04D1 0.5 s fall loop (still); 4-slot 06B9-06D8 | 03B5 0.233 s / 03B7 0.433 s | - | alias 6 / A0E cat6 |
| hover | 0391 centre + 0399 f, 03A2 rf, 039A r, 03A4 rb, 0398 b, 03A3 lb, 0397 l, 03A5 lf (2D blend), 2.0 s loops; add 03F6-03FE 0.433 s | 0435 1.5 s | - | alias 2F (0025 #776) H |
| Jump Jet / Jet Dash | 0674 0.667 s (alias 30) or the hover 2F tree | 069A 0.5 s (alias 671, 4 slots 069A / EA45 / EA3E / EA41) | 069B 0.5 s | 0027 / 4B92 #65 / #346 / #366, #509 H |
| Concussive Blast | 0491 0.6 s (leaves at 0.167 s) | 0514 0.733 s | - | alias 1E (007B #57) H |
| fire | add 3B54 0.367 s (alias A, L) | add 03AF 0.667 s | add 10D64 0.3 s (L) | no StateAnim (L) |
| reload | 0462 1.8 s (ammo at 0.6 s, state 1.5 s) | 03C9 1.5 s | 3B53 1.8 s (3P), 03CB 1.5 s (1P) | alias 4 (0023 #33) H |
| Barrage | 0384 4.33 s (the ult lasts 2.5 s) | 0507 3.97 s | 0580 4.33 s | alias 31 (002B #43) H |
| melee | 0555 0.8 s | 03E5 0.667 s | - | alias D (0043 #117) H |
| draw | - | - | - | alias A9 (0015 #34, 0.5 s): no clip in her sets |
| death | **no death clip in her sets** (H by absence); held pose 11DF 2.0 s (alias 8, df8A cat4, M) | - | - | - |
120 / 55 / 6 clips mapped; 9 / 0 / 6 named but not extracted under the model (the 42F3 intro clips, 3A74; the launcher's 3B2, 116F-1173).
Clip lengths are the clips'; ability timings come from the graphs (the 3P reload clip is 1.8 s against a 1.5 s state; Barrage's 4.33 s
clip covers a 2.5 s ult plus its end).

## Highlight intros (H: `work\intro_clips_out.txt`)
From `extract-unlocks "Pharah|highlightintro=*"`, each with its animation effect (08F, not read yet):
Heroic (Overwatch) BE1B / BE1C / BE1D / BF1D 3.867 s and C0CD 3.133 s (set 42F3's alias B33 shape: several picks, M); Barrage 226F 5.0 s (props
on models 17DB, 1942); Touchdown 1F77 4.9 s; Mission Complete 2364 5.0 s; Slam Dunk (Summer Games) 459F / 45D2 3.733 s (prop 4A01); Target
Practice (Halloween Terror) 8E32 5.0 s (props 34DE, 3B4F). The intro set 42F3's own clips (19F70-19F75, 1A22C, 1A129, 1A454) are not
extracted under F2A5.

## Data vs public (pharah.md)
| Item | Data | Public | Note |
|---|---|---|---|
| Rocket full-damage radius | **0.5 m** (2390, tooltip) | 0.8 m (W) | **disagree**: data and its own tooltip say 0.5 |
| Rocket splash / direct / edge | 80 / 40 / 24.6 at 2.5 m | same | agree; shape linear (M) |
| Rocket self-damage | 25 % | 25 % | agree |
| Rocket knockback enemy / self | 7.55 / 14.15 m/s | 7.5 / 14.2 | agree |
| Rocket speed / radius | 40 m/s / 0.2 + 0.12 | 40 / 0.32 | agree |
| Recovery | 0.8 s, 0.55 s on the emptying shot | 0.8 s; empty reload 0.25 s sooner | agree (the 0.25 is v71594) |
| Reload | 1.5 s state, **ammo in at 0.6 s** | 1.5 s, "0.6 s" unclear | settles gap 5 |
| Jump Jet | press; 17 up + 7 along input, 0.85 s, 14 s, +50 fuel over **0.85 s** | press; same; fill **0.9 s** | 0.85 vs 0.9 |
| Jump Jet overfill | holds **5 s**, then -30/s (~6.7 s for +50) (M) | lasts 6 s | shape differs |
| Jet Dash | 19 + 8, 0.85 s (M), 9 s, forward with no input | same | agree |
| Concussive | 8 s, 8 m, 30 direct / 0 splash, 60 m/s, 0.225 m, 20/5 and 30/8, cast 0.167 + 0.5 | same (cast 0.16 + 0.496) | agree; data adds no knockback falloff, self x1 (M) |
| Hover fuel | 100, drain 50/s, regen 45/s after 0.3 s, ground only | same | agree |
| Hover rise | push 50 m/s², limit **2.9 m/s** (M) | **4.66 m/s** | **disagree** (M reading; measure in play) |
| Hover horizontal / empty fall | +30 %, 8 m/s cap | same | agree |
| Overfuel cap | 150, 200 with Fuel Stores | "may overfill" | data adds the cap |
| Barrage | 2200, 2.5 s, **30/s**, 10 + 25 -> 7.5, 0.5-1.5 m, 45 m/s, 8°, -20 % move, ammo refill on cast | same; W also 0.032 s | settles gaps 1, 2, 4 |
| Helix Shields | 125 health -> shields, regen on rocket direct hits | same | agree |
| Concussive Force | 30, **full to 4 m, 15 at 8 m** | up to 30 | data adds the falloff |
| Rocket Salvo | 2 x (10 + 20), 40 m/s, 0.5-1.5 m, armed by Jump Jet / Jet Dash, 5 s window (M) | 2 x 30 | data adds the split; "180 if all land" does add up (120 + 60) |
| Recon | 3.5 s | 3.5 s | agree |
| Fall damage | m_43A3CE29 0.48 (others 0.49), no safe height | no source | data only |

## Open
- Meanings of Jet Dash's x796661A6 0.3, x02A51BD8 0.05, xED8D6672 18, xB5941741 9999; hover x0A04B51B 0.2, xF808ACE8 0.1; x57643451.
- The hover's rise (2.9 m/s limit read vs the wiki's 4.66): needs a play measurement against a clip of the real game.
- Helix Shields' regen rate (graph 0033 #131 not read); Recon's half-health test (01CF not read whole).
- The fire additive's trigger (no StateAnim; alias A by its place, L); the intros' animation effects (08F) and props.

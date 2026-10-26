# Kiriko animation map, from Overwatch's game data

Build: prometheus-2_25_0_0-154088 (archived copy `E:\Overwatch Archive\build-154088`). Read offline and
read-only through overtools' TankLib/DataTool 2.25.0.0 (`--online=false`), with Overwatch and Battle.net not
running. Nothing was decompiled or disassembled. The only look at the executable was a static strings scan,
and it found nothing useful (see "Needs the binary").

The machine-readable map is `E:\Overwatch Archive\research\kiriko-anim\map.json` (2.2 MB). It is not in the
repo because it lists Blizzard asset ids and paths. This file is the summary.

Clip ids are the 12-hex-digit index of the `.006` asset, so `AC1F` means `00000000AC1F.006`. The files are
`...\Models\00000000003A.118\Animations\<priority>\<group>\<id>.owanimclip`, which is where our `0-32_AC1F`
style names come from. Some ids have a 1 in front (`000000014DD3`, `000000017B54`): map.json always gives
the full id.

## How Overwatch picks a clip (what the data says)

- Each entity's `STUModelComponent` points to an **anim blend tree set** (`.021`). For Kiriko that's
  Gameplay3P `3A46.003` → set `0B3F`, and Gameplay1P `3A47.003` (same model `3A.118`, look `7A5.119`) → set
  `0D78`. Also HeroGallery `3E8A` → `0E0D`, Showcase `4B18` → `0FF7`, HighlightIntro `3E8B` → `0EFB`,
  PreviewEmote `3E8C` → `0E27` + `430A`. The hero STU adds `0F09` (souvenirs) and `0FA7` (PvE knock-down and
  interact). Each emote unlock has its own set.
- A set is a list of items keyed by **anim alias** (`.014` ids, kept in `m_0F898B40` in the same order as
  `m_blendTreeItems`). Each item runs a shared **template blend tree** (`.020`, e.g. `0011` = biped
  locomotion, `000B` = idle, `0054` = 3-way pitch blend). It binds the hero's clips, floats, categories and
  sub-aliases to the template's slots (`STU_3B150012`, where a templated value's `m_63DEDAC0` is a 1-based
  index into the matching array).
- Template nodes include StateMachine, Sequence, Random, BranchByCategory, Strafe (4 directions), an 8-way
  directional node, Blend1d/2d, AnimationPose2d, layers (LayerAdditive / LayerOverride with bone masks and
  weight dataflows), and AnimBlendTreeProxy (play another alias). That last one is how the tree nests.
- The set's default alias is `m_1C7DF2B9` = `0xA0E`: the root that always runs (locomotion).
- **Abilities** play aliases from statescript graphs (`.01B`, attached to the entity through
  `STUStatescriptComponent`). Their `STUStatescriptStateAnim` nodes hold an alias, an anim category,
  priority, blend-in, speed and start time. The same alias is defined in both the 3P and 1P sets, so one
  state plays the matching 3P and 1P clips together.
- Locomotion state comes from **anim categories** (`.015`) on a BranchByCategory driven by dataflow `0x18`.
  On the ground: `006` = standing and moving, `065` = crouched and moving, `066` = crouched and still,
  `005` = unknown (see below), and the default child = standing and still. Higher up, `004` = airborne.

## Idle recipes (corrected 2026-10-25: A6F0 / A804 / AEE7 / AEE8 are not played as loops)

Every Animation node has a time controller (`m_881F239A`), and that decides how its clip is played:
- `STU_AFE10726`: on the clock.
- `STU_13CB18F6`: on the clock, with the rate scaled by a dataflow (e.g. speed / 6 m/s).
- **`STU_018A2E44`: the clip's time is sampled from a dataflow.** It is phase-driven: the clip moves only
  when the parameter moves.
- `AnimationPose2d` (and its subclass `STU_B391A063`): a pose sheet, sampled at a frame picked by one or two
  parameters.

The bad renders came from playing A6F0 and the others on the clock.

**(1) Standing still, 3P** (alias 0x001, tree 000B):
- **Root:** a direction-IK node (`STU_BFFF3D5E`) turns bone 0x001 toward dataflow 0xB6 (the aim
  direction). The body yaw is procedural.
- **Base, full body:** a category switch on df 0xB7. Category 068 gives **A6F0**, with its time sampled from
  **df 0xB5**. Anything else gives **A804**, with its time from **df 0xB8**. Both use scale 12, mode 1 and
  the wrap flag. These clips do not loop on the clock. The ~1.15 m of footwork is scrubbed by the parameter,
  and the pose is frozen whenever the parameter doesn't change.
  - My reading: 0xB5/0xB8 are turn-in-place progress, left and right. The evidence is the direction-IK
    parent, the set's turn thresholds (120/80/170/190 degrees) and the stepping content. They are not named
    in the data.
- **Additive breathing:** AFDB, on the clock, looping, blend 8.
- **Additive aim:** **A71F is a pose sheet** (63 frames, sampled by two aim springs, not played). It sits on
  bones 0x003-0x005, weighted by df 0xB5 through a curve.
- **Additive slot node:** code fills it from the onFinished table (slots 2-9 → B2CB/B2CD/B2CC/B2CE, 0.83 s).
  With 8 slots these are most likely hit reactions by direction, not idle breaks.
- **To reproduce:** A6F0 sampled at one fixed time (frame 0; if that isn't neutral, frame 28, since the
  clip wraps), plus AFDB looping, plus A71F at the frame for the current aim, plus your own body yaw.

**(2) Crouched still, 3P** (alias 0x056): the same tree with **AEE7** (df 0xB5) / **AEE8** (df 0xB8) sampled
the same way, breathing **60F8** (2.67 s, looping) instead of AFDB, and the same A71F sheet. To reproduce:
AEE7 at a fixed frame, plus 60F8 looping, plus A71F.

**(3) 1P idle, hands in front:**
- **Base:** **A3A7**, a 6-frame held pose on 156 bones (the arms rig). It is played with end mode enum 3,
  where the loops use 1, so probably hold/clamp, and gated by condition t15 i1. This is the hands-in-front
  idle.
- **Breathing:** **A45D** (1.57 s, looping) on the inner movement layer when standing still. Crouched it's
  AB7F + AC79.
- **Look sway:** pose sheets AB5C / AB50 / AB56, sampled by spring drivers on df 0x08 / 0x24 / 0x96.
- **Walk bob:** A524 → A525. A525's time comes from df 0x2B (distance), scale 2.0.
- **To reproduce:** hold A3A7, add A45D looping, and optionally the sway sheets at frame 0.

The 0-44 one-frame clips AF4F..AF53 are not the base stand. They form a 5-point Blend2d (alias 0x174)
played by the generic graph 02C5 for one scripted state.

Correction to the table below: the "3 pitch variants" groups sit on a Blend1d at -1/0/+1 whose driver
dataflow is unbound in the data. "Aim pitch" is a guess.

## The answers

| State | 3P clip(s) | 1P clip(s) | Certainty |
|---|---|---|---|
| **Standing idle** | **A6F0** (alt **A804**) sampled at a parameter-driven time, NOT looped (see the recipes above), + AFDB breathing loop + A71F aim pose sheet | held pose **A3A7** + A45D breathing | high for the mechanism |
| **Crouch idle** | **AEE7** / **AEE8**, sampled the same way, + 60F8 breathing | AB7F + AC79 | high for the mechanism |
| Crouch walk, 8 directions | AEEB AEED AEF0 AEF3 AEF2 AEF4 AEEF AEEE | A520 / B1D0 (additive) | high (the owner confirmed 3P by eye) |
| Standing move, 4 directions (start, loop, alt) | AFCB/AFCC/AFCD, AFE9/AFEA/AFEB, AFCF/AFD0/AFD1, AFD7/AFD6/AFD5 | walk bob A524 → A525 (rate driven by speed) | high for structure. Which Strafe slot is fwd/back/left/right isn't named |
| Stop (standing) | AA37 → AA38 / AA39 | | medium |
| Airborne stance | B033 | | high (owner confirmed) |
| Jump / fall loop / land | candidates AF3C (alias 0x64), AF33 (0x65, the only loop), AF32 (0x66) | ABEB / ABED / ABEE | low: no dumped graph plays these aliases |
| Unknown 8-direction set (ground category 005) | A8C7 A9AD A8F3 A9B0 A8F9 A9AF A8F4 A9AE + A9B4 A9B5 A9B6 A9B8, additives AFC4 B1EB AFC6 B1EC AFC9 B1ED AFC8 B1EE AA1D | | low (maybe landing or take-off by direction) |
| Wall climb | AC1F (alias 0xA0, graph 30D9) | AA7E (+AA94) | high |
| Wall-climb hop over the top | AC20 (0x9C) | AA7C | high |
| Melee | AFB8 / AF57 / AFB9 (pitch down/level/up, 0.733 s, alias 0x0D, generic graph 0043) | A529 (+0C63 5687 3B4x) | high |
| Protection Suzu throw | AD83 / AB96 / AD82 (alias 0x7F5, category 253) + additives ADCE ADD2 ADD0 | A50A | high |
| Suzu follow-through | AD85 / AB95 / AD84 (0x807) | A84C | medium |
| Swift Step depart | ABBA (0x7F8, start 0.15 s) | A591 | medium |
| Swift Step arrive | ABBB + AEE4 (0x818) | A9A9 | medium |
| Kitsune Rush cast | B021 / ABF0 / B01E (0x7FC, 0.933 s) | A5F6 | high |
| Kitsune Rush end | B022 / ABF1 / B01F (0x7FD) | (none bound) | medium |
| Weapon script 2171 (Ofuda + Kunai) | see below | see below | low for which button |

Three-clip groups are the pitch variants (down, level, up) of a Blend1d on dataflow `0xB6` (aim pitch).
A71F is an additive lean/aim layer used under every locomotion clip. The `4\*` folder clips are additive
layers (priority 4). That's why they collapse the skeleton when played alone.

**Weapon script 2171** (Ofuda `22C` and Kunai `2C8` both point at it). Order in the graph:
`0x16F` (4 alternating × 3 pitches, 0.7 s: AE3F AB5D AE05 / AE42 AB62 AE09 / AE41 AB61 AE08 / AE40 AB60
AE07; 1P A46E, A6DA) → then `0x84E` (1P only: A6F8) or `0x40A` (4×3, 0.6 s: AE3E AB94 AE06 ... AB91 AE0A; 1P
B088). A second chain runs `0x875` (B105/B011/B104, 0.2 s; 1P B1D6) or `0x16E` (AE1C/AD90/AE1B, 0.2 s; 1P
A49C) → `0x874` (B109/B00F/B108, 0.63 s; 1P B0E1) or `0x812` (AE22/AD94/AE21, 0.8 s; 1P A49D) → `0x813`
(B107/**B00B**/B106, 1.07 s; 1P A8F5 + C534-C537) / `0x876` (B3B1/B0C9/B3B0, 0.87 s; 1P A8F6). `0x004`
(ABCF 1.33 s; 1P A810) is probably equip.

My reading: the 4-variant alias that fits the 4-talisman volley is the Ofuda one, the other is Kunai, and
the start-middle-end chain looks like the reload. That makes B00B (the owner's "hackles up" arms spread) the
reload's middle. This is a guess: confirm by eye.

**First person.** The 1P set has 69 aliases and 81 clips, and all 81 are already extracted. They sit in the
same `Models\00000000003A.118\Animations` folder as the 3P clips, because 1P uses the same model, so
extract-unlocks just mixed them in. The earlier note "all 330 clips are third-person" is wrong: about 80 of
them are 1P (A3A7, AB5C, A524, A5F6, ...). To tell them apart, use `sets["000000000D78.021"]` in map.json.
The 1P "idle" is the static base pose A3A7 with additive sway (AB5C AB50 AB56 AB51 AEF6-AEF9, driven by
look-velocity dataflows) and the movement layer on top. **There is no clip named or structured as a
"hands-crossed" idle.** The best candidates for a 1P idle fidget are the aliases nothing visible plays:
`0x1EE` (1P AF02 1.8 s, 3P twin AF68) and `0x2DE` (1P AFCE 1.8 s, 3P B2C8/B0CA/AF69). Checking these by
eye is the next step.

## Sounds, voice, effects (map.json `abilities`, `clips[].sounds`, `voice_by_stimulus`)

- Clip sounds: 45 clips have sound files under
  `Models\00000000003A.118\AnimationEffects\<clip>\Sounds\<02C>\*.ogg`. Every clip lists its own in
  `clips[id].sounds`, and the header also gives the clip's AnimationEffect (`.08F`).
- Ability effects: each graph's `STUStatescriptActionEffect` effect ids, with the extracted .ogg files under
  `Entities\...\Effects\<effect>\Sounds` where they exist (99 effects have sounds).
- **Ultimate voice.** The ult graph 30DA passes three stimuli into the shared voice subgraph 545C:
  - `FC3`: "Let the Kitsune guide you!", "Just aim for the head!"
    (`HeroVoice\Kiriko\Ultimate\00000005EB8B.0B2-...`, `0000000627CE.0B2-...`)
  - `FC2`: "(Japanese) O fox spirit, unleash your claws!" (`000000062D15.0B2`), "(fox noises)" (`0642FE`)
  - `184E`: newer copies of the two English lines (`06982F`, `069830`)

  From how Overwatch plays it, FC3 is what you and your allies hear and FC2 is what enemies hear. The
  routing inside 545C (unnamed STU classes) doesn't confirm it. `069B69` "Let my sunlight guide you!" is a
  skin line.
- Other ability voice: Suzu `FC5` ("Cleansed", "Purified", "Protecting!"...), Swift Step `FC0` ("I'm here",
  "I'm with you"...), healing `FC4` (graph 2128). All lines of every stimulus, with their .ogg paths, are in
  `voice_by_stimulus` (from voice set `215.05F`, 882 lines).
- Timings in the data: per-state blend-in times, start times, duration expressions and speed-scalar
  variables are in `abilities[].state_anims`. The entity's schema overrides hold weapon timing floats (0.36,
  0.75, 0.5, 0.57, 0.9, 0.86 on vars 118CB-118D0). Turn-in-place thresholds are in the set's
  `STUAnimParamUpdater_Biped3P` (120/80/170/190 degrees).

## Cosmetics (map.json `unlocks`)

23 emotes (set, alias, clips), 21 victory poses (`STULineupPose` → clip, e.g. Heroic BFB5, Healing Ofuda
B96F, Glare B96E), 18 highlight intros (clip on the unlock, e.g. Kitsune Rush C460, Protector of Kanezaka
C2C2), and 42 voice-line unlocks (voice effect; 38 matched by name to `HeroVoice\Kiriko\Voicelines`). The
gallery, showcase and default intro sets take their clip from a dataflow: the UI supplies it. The cosmetics'
clips (158) were extracted to `E:\Overwatch Archive\research\kiriko-anim\extract_unlocks\`
(`extract-unlocks "Kiriko|emote=*" "Kiriko|victorypose=*" "Kiriko|highlightintro=*"`). Only 13 clips have
no extracted file.

## Needs the binary (protected), so not attempted

- Names of anim parameters and dataflows (`.013`), categories (`.015`), aliases (`.014`) and condition types
  (`STU_8C127DE2` m_type 2/5/7/8/9/14/15/17/18/22/25). The data only has ids. A static strings scan of the
  archived Overwatch.exe (`exe_strings_anim.txt`) found 75 generic hits and none of these names: they are in
  protected code, or nowhere.
- Which button triggers which weapon state. The logical-button nodes are in shared graphs (0015 etc.) I did
  not walk to the end. That one is doable from data with more graph tracing.
- Who hears which ult stimulus (subgraph 545C logic), and what anim header group 32 vs 36 means (it looks
  like non-loop vs loop).

## How it was done (repeat for any hero)

Everything is in `E:\Overwatch Archive\research\kiriko-anim\scripts\`:

1. **Runtime.** No .NET SDK is needed. DataTool's bundled .NET 10 runtime is copied into
   `research\...\dotnet\shared\Microsoft.NETCore.App\10.0.12` (plus `host\fxr`), with a generated
   `Microsoft.NETCore.App.deps.json` (`mkdeps.py`). Python + `pythonnet` (in a venv) hosts it and loads
   TankLib/TACTLib/DataTool from the DataTool folder. `owinit.py` opens the build offline: it refuses to run
   if Overwatch or Battle.net is running, uses `Online=False`, the local keyring and no remote keyring URL,
   then dumps any STU to JSON with Newtonsoft (shared references kept).
2. `crawl.py <roots>`: breadth-first STU dump following chosen asset types (`FOLLOW=003,020,021,075` for the
   hero → entities → sets → trees; `FOLLOW=01B` for statescript; `0A5`/`0BF` for unlocks and poses).
3. `resolve.py <set>`: binds each item's template tree to its clips, aliases and categories. `flatten.py`
   then gives every clip with its node path, following proxies through aliases. `render.py` and `aliases.py`
   make readable dumps.
4. `analyze_ss.py`: StateAnim and ActionEffect nodes per graph, with graphs tied to abilities by the loadout
   GUIDs they mention (from `heroes.json`). `ctx.py <graph>` traces the graph links around each anim state.
5. `clipinfo.py`: reads each clip's header (priority, group, duration, fps, bones, effect).
   `assemble.py` + `build_map.py` write map.json. `t9.py` maps the voice set to the HeroVoice files.

For another hero: take the hero GUID from `heroes.json`, crawl from it, read `m_animBlendTreeSet` of the
Gameplay3P entity and of its `STUFirstPersonComponent.m_entity`, then run steps 3-5. The template trees and
the category ids (`004/005/006/065/066`) are shared by all heroes, so the locomotion reading above carries
over.

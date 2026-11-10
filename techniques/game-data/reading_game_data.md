# Reading the game data

How to get answers out of Overwatch's data, offline and safely. Up: [Techniques](../README.md) · inside a model:
[model_data](model_data.md) · why it matters: [principles](../principles.md)

## Safely (CLAUDE.md, the owner's rules)
- Overwatch and Battle.net must be closed: `Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue` first.
  The research setup (`owinit.py`) refuses to run otherwise.
- Read-only and offline, through DataTool and TankLib. Never touch the client, its memory or its protected
  executable. Nothing extracted goes into the repo: data files we commit hold ids and numbers only.
- **Archive builds and extractions on E:, read-only, with checksums**, outside any game library, so updates can't
  undo the work (`E:\Overwatch Archive\README.txt`). The current build's DataTool output is
  `E:\Overwatch Archive\extracted\154088`.

## The setup
- **Display texts (.07C):** `DataTool.exe --online=false "E:\Overwatch Archive\build-154088" dump-strings` lists every one (`research\strings\strings-154088.txt`, 67,000 lines). Read a prompt's words there before writing any ([reticle](../hud/reticle.md)).
- TankLib/TACTLib loaded with DataTool's bundled .NET runtime from Python (pythonnet), no SDK needed:
  `E:\Overwatch Archive\research\kiriko-anim\venv\Scripts\python.exe`, helpers in `...\scripts\owinit.py`
  (`o.guid(index, type)`, `o.u64`, `o.TH.OpenFile`, `o.instances(guid)` for an STU's instances as JSON-like dicts).
- Tools that run there: `tools/emote_data.py`, `tools/cloth_data.py`; scratch readers like `dumpstu.py` (STU
  instances to JSON), `modelhp.py`, `clothdump.py` (reflection dumps of TankLib's parsed chunks; pythonnet returns
  primitives as Python values, so check `hasattr(v, "GetType")`).
- A model's full id: DataTool exports models as `<idx>.<type>` folders (Kiriko's `00000000003A.118`). Find the full
  GUID among `DataTool.Program.TrackedFiles` with that index and type, taking the variant with a
  `teModelChunk_Skeleton` (there are siblings without).
- DataTool's unlock list (list-unlocks, JSON: `research\kiriko-anim\unlocks\unlocks_all_heroes.json`) names every
  hero's unlocks, nested in groups: walk it for `"Type": "Emote"` to get each emote unlock's id.

## Ids
- Ids in effect headers and STU fields carry type bits (a hardpoint 03C shows as 0xDC0...): mask with
  `& 0xFFFFFFFFFFFF`.
- Some ids are virtual (no file): hardpoints (03C), anim aliases (014), statescript vars (01C).

## Animation effects: what a clip spawns
A clip's header names an animation effect (`teAnimation.Header.Effect`, an 08F), a chunked file read with
`TankLib.teChunkedData`. Each `teEffectChunkComponent` comes right before its component and holds StartTime, Duration
and `Header.Unk`, the hardpoint. The components:
- `teEffectComponentModel`: a model and its own clip;
- `teEffectComponentEntity`: an entity, whose definition names the model. A `STUProjectileVisualComponent` in it means
  it's thrown or dropped;
- `teEffectComponentVoiceStimulus`, `Sound`, `EntityControl`.

Used by [emote_reproduction](../emotes/emote_reproduction.md).

## Blend trees, unlocks
- An emote unlock (`STUUnlock_Emote`, 0A5) names its anim alias (`m_emote`) and its blend-tree set
  (`m_emoteBlendTreeSet`, 021).
- The set's `STU_B38E3226` holds one `STUAnimGameData_PoseData` per clip (`m_animationID` = the clip id) with
  `m_geoSetFlags`: hide masks over time ([model_data](model_data.md#geosets)).
- The items' `m_onFinished` chain isn't decoded yet.
- Dumps dedupe with `$id` / `$ref`: resolve refs before walking.

## Statescript answers timing questions
The kunai's ammo point came from her weapon graph (statescript 0x2171, in
`E:\Overwatch Archive\research\kiriko-anim\dump_ss`). Find the graph that plays the action's anim alias, then its Wait
nodes and the SetVar literals their expressions read. There it was 0.65, the "65%" of the patch notes. The match
music was found the same way (graph 0D3B: [music-round9](../../tools/reference/music-round9.md)).

**TankLib leaves expression operands unmapped** (raw fields like EDD5CEE2), so a node's expression reads as opcodes with
holes. For Mercy (2026-10-28) a raw STU v2 reader and a small decompiler that resolve the operands and follow the
branches turned her whole kit into formulas with node ids: `E:\Overwatch Archive\research\mercy\scripts\kit\`
(stuv2.py, operands.py, decomp.py), outputs in `work_kit\`. The hero-settings file (0126) names the options and their
defaults (Mercy's "prefer beam target", "crouch activates Angelic Descent"), which tells you which branch is the
default game. **A value that matches a public number isn't identified until its reader is found:** her 1BE6 = 1.3
matched the wiki's 1.3 s beam linger and was Valkyrie's launch factor
([heroes/mercy](../heroes/mercy.md#what-her-graphs-said-that-the-wiki-didnt-kit_detailsmd-2026-10-28)).

## Ability effects: what sets their size and colour (Sombra's pass, 2026-11-01)
From [sombra-effects](../../tools/reference/sombra-effects.md), read with Ana's scripts (`research\sombra\scripts\fx\`):
- **A big effect can be a small mesh with an over-life size row.** EMP's 15 m pulse spawns its band and spheres at size
  2.05; a 12BB LUT row multiplies them x1 -> x6.95 -> x14.5 -> x15 over life (30 m across in a quarter of its life). No
  radius comes from the graph. Before scaling a shape by the ability's range, read the renderer's 12BB rows
  (`fx_digest.txt`, the `.../12BB ... MAP` lines): the size curve may already be there.
- **Team twins can live in the colour strip, not the LUT.** Most of her renderers come in pairs identical but for a
  256 x 8 BC1 / BC7 strip (purple A06C / red-orange ADD0); the LUT rows are white masks or tints. Sample the strips
  (`fx_sombra_strips.py`) before reading a renderer as colourless.
- **An effect played on targets sits in the branch that runs per target.** Cyberspace's weakened look is in the zone
  graph's per-target branch (condition and targets `self` = each tracked enemy), next to the -50% it applies. Look for the
  effect node beside the status the ability applies, not at the ability's start.
- **One ability, two looks, split by graph state.** Hotfix's State #2197 plays gold strands (the heal on an ally) and
  #2198 purple ones (the hack of a pack or deployable): when an effect family comes in near-identical copies, check which
  state plays each before calling them twins.
- **Truncated ids on the contact sheets:** the sheets print the last four hex digits; `E4FB` is `2E4FB`. Glob by suffix.

## Where a melee hero's effects live (Brigitte's pass, 2026-11-01)
From [brigitte-effects](../../tools/reference/brigitte-effects.md) (scripts `research\brigitte\scripts\fx\fx_brigitte_*`):
- **Most of her ability graphs' effects are sounds only.** Of 72 effects her graphs and entities name, the flail swing, Rally's start,
  Inspire on allies, the barrier raise and the whip launch are all sound banks. **The visuals are the clips' animation
  effects** (08F): the swing arc is clip 4E9E's effect 26EB, the bash flash clip 5073's 26A8, Rally's flail-head burst
  clip 50B0's 27F1. `resolved\clipinfo.json` already lists each clip's `effect`: add them to the extract list as
  `N(id, "08F")` (the decoder and extractor take them like 00D). Check the clips before saying "no effect".
- **Contact sets (.049) are impact sounds.** The flail / whip / bash contact sets (061B, 061D, 0620, 075A) map surface
  materials (03A) to .04A entries whose only refs are sounds (.02C). Hit visuals on creatures come from the shared damage
  system, not the hero (no hero-owned hit effect was found).
- **A barrier is a look, not an effect.** Her barrier field is the shield model 1064F drawn with look 13917, whose
  submesh material 5B2C (shader 0x32) holds the hex masks and a 3-row float LUT: row 0 blue (her team), row 1 red-orange
  (enemies). Map submeshes to materials by the look's .owmat keys and the .owmdl submesh names
  (`fx_brigitte_submeshes.py`); dump a look material's LUT with `fx_brigitte_lut.py`.
- **1P and 3P can play different effects from one node pair.** Her chain and the whip use a gold / blue energy ribbon
  under `v28985` and a plain chain ribbon otherwise; the 1P swing clip uses only the gold-to-blue row. When the same
  slot has two looks under a graph input variable, check whether it's the view (M for her: `v28985`).
- **PowerShell `Get-Content -Raw | Set-Content -Encoding utf8` corrupts UTF-8 files** (reads them as ANSI, writes a
  BOM): `build_asset_pack.py`'s "Lúcio" came out double-encoded. Edit repo files with the Edit tool or Python.

## A ranged healer's effects (Juno's pass, 2026-11-01)
From [juno-effects](../../tools/reference/juno-effects.md) (scripts `research\juno\scripts\fx\fx_juno_*`):
- **A marker on targets can be a HUD widget, not an effect.** Her torpedo lock-on marker is a `StateUXPresenter` (graph 4B99 #158)
  showing UX element 0727.116 at the target's hp 0001 with the lock progress; the graph's only effect is a sound. Before looking for
  a marker's effect, grep the per-target graph for `StateUXPresenter`.
- **An effect mesh can carry its team twin as a second look.** The Hyper Ring is mesh 15AD6 drawn twice, look 1BABB (blue LUT) and
  1BC0D (red). The extractor keys models by id and keeps only the first look: save the second with `fx_juno_savelook.py` and dump both
  materials (`fx_juno_owmats.py`, `fx_juno_matdump.py`).
- **Sizes and heights can sit in the hardpoint offsets.** The Orbital Ray's column is a 5.6 m tube placed at +200 / +250 m and rings at
  +50 to +196 m above the ground point: read the offsets on each renderer's `hp` line in the cards, not just the mesh bounds.
- **A held effect's length isn't the ability's.** Her 8 s ray is a 2 s effect held by a `StateTrackRay`; her 6 s ring a 2 s one.
- **Per-target looks come from the per-target graph's begin.** The torpedo heal (4795 #390 at the heal wait's onBegin) and the ray's
  ally look (54A6 #297 next to the +30 % damage) sit beside the status they apply, as Sombra's Cyberspace did.
- **List the clips' effects up front** with `fx_juno_clipfx.py` (159 of her clips name one): most are footstep / camera-shake ones,
  so pick the ability clips by the clip map's state names.

## Values, modes and roles (Sigma's pass, 2026-11-02)
From [heroes/sigma](../heroes/sigma.md) (scripts `research\sigma\scripts\`, Sojourn's retargeted):
- **A value's main number is `value_m_3E5360A1`;** its `values` row can lead with another mode's (Sigma's health 047C: 200, row 300).
  `gvsummary_sigma.py` prints main, alt and readers together.
- **Check a StateModifyHealth's mode before reading a rate:** `m_DD792FF0 = x60671788` is a share of max, `x00943466` an absolute amount.
  Sigma's barrier regenerates 0.122 of its max a second: 79.3/s at 650, where the patch notes still say 85 (0.122 x the old 700).
- **Knockback is a pair:** `m_E33EFEE1` horizontal, `m_583C208B` vertical (Accretion 6 / 3 = the wiki's 6.7 m/s). Whip Shot's 1259 #638 alone has a vector beside it: `m_1376B4DF` says 25 and 2 up against the pair's 25 / 3, unsettled; the mod plays 3 because it reaches W's 8 m in our mover ([polish4](../testing/polish4.md)). No rule to take from that. Filter `m_89831602`: 1 enemies, 4 allies not the caster, 5 both, 6 allies and something more, read as the caster, against BZ on Rally (M-L, [polish4](../testing/polish4.md) on #125).
- **Role and sub-role passives are in the shared graph 01C7,** keyed on the hero's category (0850 Tank, 084E / 0851 the others) and
  sub-role tags (116B Stalwart, 1173 Survivor): read them there for any hero of that role.
- **A skin can swap every model** an entity names: map clips under the skin's `.owentity` models (106F7 for Sigma's D75A).

## Research agents
- **Map a hero's data with a sub-agent before guessing.** Guessing clips by eye cost several rounds of owner
  corrections: the "hackles up" idle, a phone emote as the crouch idle, a recovery as the throw. A research sub-agent
  mapped every clip to its state, layer, timing and alias in ~40 minutes (`research\kiriko-anim\map.json`,
  `tools/clipmaps/kiriko-research.md`). Ask it to map **everything** (emotes, intros, sounds, effects): future work
  reuses it.
- **Then check the research by eye, and send it back when it disagrees.** Its first idle pick (A6F0) stepped 1.15 m
  played on the clock. Asked how the node plays it, it found the time is sampled from a game parameter. Render every
  claim before wiring it in.

## UI textures
To find one of Overwatch's icons in the 21k-texture UI dump:
- filter by size, alpha share and whiteness, then sheet the survivors with their ids (scratchpad `iconsheet.py`);
- don't match a 20 px crop from a screenshot by shape: at that size every blob scores well;
- small glyphs may live in atlases. The chat channel icons weren't found as textures, so we drew our own
  (`tools/gen_chat_icons.py`).

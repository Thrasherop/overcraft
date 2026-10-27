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

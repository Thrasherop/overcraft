# Inside a model

What a model file holds beyond its mesh, and how to read it. Up: [Techniques](../README.md) · how to read files:
[reading_game_data](reading_game_data.md) · used by: [emote_reproduction](../emotes/emote_reproduction.md),
[cloth](../animation/cloth.md)

A model is a chunked file (`TankLib.teChunkedData`). Kiriko's chunks:
- `teModelChunk_Cloth`;
- `teModelChunk_Model`;
- `teModelChunk_RenderMesh`;
- `teModelChunk_STU`;
- `teModelChunk_Skeleton`.

## Skeleton order
The bone indices the model's own data uses (cloth followers, drivers) follow the skeleton's order. That is the order
of `refPoseBones` in the Blender add-on's `OWModelReader` over the exported .owmdl, which also gives the names
(`Wrist_L` for known ids, `bone_XXXX` / `cloth_XXXX` with the hex id otherwise).

## Hardpoints
- The full table is in the STU chunk: `teModelChunk_STU` -> `STUModel.m_hardPoints`. Its fields are `m_EDF0511C` (the
  hardpoint id, type bits included), `m_888BCD8B` (the bone, an 012 id), `m_position` and `m_rotation`, the offset in
  the bone's frame.
- **DataTool's .owmdl export writes only some of them.** Kiriko's model has 39 and no 000A / 000B. Lúcio's defines
  both ([kiriko_emotes](../emotes/kiriko_emotes.md#hardpoints)).
- `STUModel.m_37ED05D0` names another model (Kiriko's: 107C5); it holds no hardpoints.

## Geosets
- Each render submesh's descriptor (`teModelChunk_RenderMesh.SubmeshDescriptors`) carries one geoset bit (`m_40`:
  1, 2, 4 ... 128), and LOD flags.
- An emote's blend tree hides geosets over time ([reading_game_data](reading_game_data.md#blend-trees-unlocks)).
- Matching our .glb's pieces to geosets: sum each geoset's vertices at its most detailed LOD. A .glb mesh is the
  geoset with the most vertices not above its own: the export splits vertices along seams and never merges them.

## Cloth (Domino)
- `teModelChunk_Cloth`, which TankLib parses. `Header.Count` gives the pieces. Per piece:
  - a `Descriptor` (`ClothDesc` with `m_dominoData`: byte offsets to sections);
  - `Vertices` (`ClothVertex`: `FollowerBone`, `VerticalParent`, `DriverWeights` of `DriverIdx` and `Weight`);
  - `BoneToDriverMap` (skeleton index -> driver index).
- A point follows a bone (-1: a top-row anchor) and is placed by its drivers' weighted transforms: see
  [cloth](../animation/cloth.md).
- **Not decoded yet:** `PositionsOffset`, `SectionOffset3`-`11` (constraints, limits, collision?), which hold the
  physics Domino runs on top.

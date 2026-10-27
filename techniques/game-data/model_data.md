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
- **The sections TankLib doesn't interpret** (decoded 2026-10-27; decoder and checks in
  `E:\Overwatch Archive\research\domino-cloth\domino_decode.py`, all 78 checks pass on Kiriko and Lúcio's models).
  - **Layout:** the chunk's data starts 16 bytes after the tag; every descriptor offset is relative to that. Each
    section holds the 6 pieces' arrays back to back, 16-byte aligned. Per-vertex arrays are padded to a multiple of 8
    (dummies).
  - **Per point:** rest position (Positions, vec4, model space; equals the follower bones' rest to 1.6e-7 m), rest
    normal (`Ow220_1`), inverse mass (S3; 0 on the anchors, which come first), a rest frame [t, n x t, n] (S6), depth
    (S7), tether length up the chain (S8) and tether root (S10), frame neighbour (S11). S5 is a per-row weight of
    unknown meaning.
  - **Triangles** (S15), and **distance constraints** (S16 pairs, S17 rest lengths) batched by S20-S23 into vertical,
    horizontal, shear and skip-one bend sets. Each batch is an independent set: graph-coloured Gauss-Seidel, 8 wide.
  - **Bending triples** (S18 a, b, c; S19 weights from the inverse masses).
  - **Colliders** (S24, 80 bytes each): tapered capsules along a bone's local X (radii, length), e.g. Spine_3, Thigh,
    Hips, Arm, Elbow, Knee, Foot.
  - **Driver bind poses** (S26).
- **Settings:** the descriptor's `UnknownGuidx81` is a .081 asset (`STU_51B148E0`). It holds `m_dampingFactor`, a
  gravity vector (likely `m_599AB8D7`), likely an iteration count (2) and four floats that look like the constraint
  groups' stiffnesses (unproven; the field names are hashed).
- **What Domino likely runs:** anchors skinned to their drivers; moving points integrate gravity with damping; about 2
  Gauss-Seidel passes over the distance batches, the bending triples and tethers; collision with the capsules; each
  cloth bone takes its point's position and a turn rebuilt from its frame.

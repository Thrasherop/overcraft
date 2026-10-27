# Cloth: sometimes animated, sometimes simulated

How a hero's cloth (coats, capes, scarves, hair ribbons) moves, and how to reproduce it from the data. Up:
[Techniques](../README.md) · inside the model: [model_data](../game-data/model_data.md#cloth-domino) · per hero:
[kiriko_cloth](kiriko_cloth.md) · used by: [emote_reproduction](../emotes/emote_reproduction.md)

## What Overwatch does
- **No clip keys the cloth bones themselves** (`cloth_*`): Kiriko's clips key 2 of her 278, in every clip.
- **That doesn't make the cloth physics.** Overwatch's cloth system ("Domino": the model's CLTH chunk) places each
  cloth bone from up to four **driver bones** with weights, as skinning places a vertex. The drivers are the body
  (hips, thighs, knees, feet, arms) and cloth driver bones of their own, which the clips **key by hand**. Where an
  animator keyed them, the cloth is authored: Kiriko's coat in Kneeling and Fox Cuddles is exact to the pixel.
- On top of the driven positions, Domino runs light physics (the swing as she runs). It's a position-based solver
  whose every input is in the chunk, now decoded ([model_data](../game-data/model_data.md#cloth-domino)): rest
  positions, inverse masses, anchors, distance constraints batched for Gauss-Seidel, bending triples, tethers,
  capsule colliders on bones, and per-piece damping, gravity and (likely) iterations.
- So per hero and per clip, cloth is **animated** (drivers keyed), **follows the body** (body drivers only), and
  **simulated** around both.

## How to reproduce it
1. `tools/cloth_data.py <hero> <model .owmdl>` (research venv) writes `assets/overcraft/cloth/<hero>.json`: each cloth
   bone's drivers and weights, and its point above in the cloth's grid.
2. `HeroModels` gives the model its drivers (`SkinnedModel.clothDrivers`). `ClothSim` places each cloth bone, parents
   first: its rest transform taken into each driver's rest frame, carried by the driver as posed, blended by weight
   (`Matrix4f.fma4x3`, not `scale`: JOML's `scale` is a scale transform and leaves the translation unweighted), then
   written back as the bone's local turn and offset.
3. Without cloth data (another model), `ClothSim` falls back to a free simulation:
   - each long chain hanging from the body pinned at its top and pulled toward its drape;
   - gravity, lag behind her motion, bone lengths kept;
   - capsules around the legs, a floor.

   All by eye: it was "super wonky" (the owner), so use the data wherever it exists.

## Lessons
- **"Never keyed" isn't "not animated": look for what drives it.** For two days Kiriko's coat was our own pendulum,
  because no clip keys her cloth bones. The owner, watching the emotes, guessed it was hand-animated; the cloth chunk
  answered it. Measure the skin weights too: only 9% of her body mesh's weight was on cloth bones, which pointed at
  the other bones.
- **Two callers of one per-frame clock starve each other.** Her pose is worked out more than once a frame. The free
  simulation took its time from the animator's shared `lastNow`, so every call after the first got 0 and restarted
  it: the coat never moved in emotes. A dev readout found it in one try where screenshots hadn't: the cloth's lowest
  point, stuck at its initial MAX_VALUE (`state`'s `clothLowest`). Give a simulation its own clock, and treat a repeat
  at the same time as a redraw.
- **A floor for a free simulation.** Without one, the free coat sank 0.50-0.66 m under the floor when she knelt or sat
  (`clothfloor on|off` shows it). The driven coat needs none: the animators kept it above the floor.

# Animation import: clips out of the data and into the game

Getting a hero's clips out of Overwatch, finding the right one, and playing it right. Up: [Techniques](../README.md)
· the data: [reading_game_data](../game-data/reading_game_data.md) · emotes: [emote_reproduction](../emotes/emote_reproduction.md)
· cloth: [cloth](cloth.md)

## Export
- `tools/owm_survey.py` (Blender, headless, with the io_scene_owm add-on) imports a model and its clips and exports a
  .glb.
  - `--keep=<clip map>` picks and renames clips (all ~330 take the glTF exporter more than 30 minutes).
  - `--emotes=<dir>` adds the hero's emotes as `emote_<slug>_<n>`.
- **Clips are 30 or 60 fps**: export them onto one timeline.
- **`4-*` folder clips are additive deltas**: they collapse alone; export neutral keys for unkeyed channels.
- **Additive clips can carry big offsets, not just motion around zero**: add_walk lifted the arms 85 px for as long
  as she moved. Measure a pose against still, not only how it moves.
- **The exporter writes every bone into every clip.** A bone a clip doesn't drive gets its rest pose, constant:
  partial clips need those channels left out (`HeroAnimator.driven`,
  [emote_reproduction](../emotes/emote_reproduction.md#7-her-own-weapons-the-blend-trees-geosetflags)).
- First-person arms only get materials from the Gameplay1P entity's model look (`--entity=`).
- Props (Kiriko's kunai x3, ofuda, Suzu) are model pieces parked at the origin until a clip puts them in a hand.
- **Renaming clips in a .glb doesn't need a re-export** (`tools/rename_glb_clips.py`); keep `tools/clipmaps` in step.

## Our bones keep Overwatch's frames
World rotations from the raw .owmdl (`OWModelReader.refPoseBones`, chained) and from our glb's nodes match to three
digits. So offsets from the data (hardpoints, cloth drivers) apply to our nodes as they are. **Blender's armature
doesn't keep them**: its bones point Y down the bone, which showed every hand hardpoint "turned a quarter about X",
and a fix by eye was built on that. Read frames from the raw reference pose or the glb, never from Blender's bones.

## Finding the right clip
- **Contact sheets.** Render clips as frame strips and look at many at once (`tools/owm_survey.py --survey --shots=N`,
  `--side` for leg cycles; scratchpad `sheets.py` stacks strips). The owner spotted the wall climb on one; send the
  owner sheets when stuck, since a human recognizes poses instantly.
- **Render first-person clips from the Camera bone with its whole transform** (`tools/owm_fpsurvey.py`): position
  only puts the arms at the top of the frame, because the clips turn and dip the camera.
- **Measure, don't eyeball:**
  - direction of travel from the planted foot's slide (run clips);
  - pose features (wrists above the head, pelvis height) to find climbs and crouches;
  - which clips move the held-prop bones (found the Suzu throw);
  - root bone height (found climb_top's root motion).
- **When a clip "looks wrong" to the owner, re-identify it from the game's own graph**: alias chains, which sounds
  sit on it, its length against the stat. Render strips. A whole action can be mislabelled: the "reload" was an
  ofuda throw, and the real reload was filed as "equip".
- **Glitches that come "at random"** may be a layer under everything: the leg kick was the idle breathing clip's
  broken keys. Read a clip's keys (glb animation channels in Python) for single-frame jumps, and scan the data for
  every case, not just the one fixed ([hand_play](../testing/hand_play.md)).

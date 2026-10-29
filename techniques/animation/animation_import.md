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
- **`4-*` folder clips are additive deltas**: they collapse alone; export neutral keys for unkeyed channels. `132-*`
  are additive too (Jetpack Cat's quick melee). Name them `add_*` in the clip map and play them as layers: a hover
  hero's third-person actions (her reload, Purr, melee, Catnapper's aim) are all additive over the flight blend, and
  played as whole clips they would have replaced her pose.
- **Additive clips can carry big offsets, not just motion around zero**: add_walk lifted the arms 85 px for as long
  as she moved. Measure a pose against still, not only how it moves.
- **Actions play over the pose under them, like emotes:** a clip's undriven bones keep that pose
  (`HeroAnimator.driven`). Over the rest pose, Lúcio's left-hand Crossfade dropped his right arm and gun out of view.
- **The exporter writes every bone into every clip.** A bone a clip doesn't drive gets its rest pose, constant:
  partial clips need those channels left out (`HeroAnimator.driven`,
  [emote_reproduction](../emotes/emote_reproduction.md#7-her-own-weapons-the-blend-trees-geosetflags)).
- First-person arms only get materials from the Gameplay1P entity's model look (`--entity=`).
- Props (Kiriko's kunai x3, ofuda, Suzu) are model pieces parked at the origin until a clip puts them in a hand.
- **An entity's child entities come in as rigs of their own** (Lúcio's Gameplay1P brings his gun): with two rigs the
  glTF exporter wrote no clips at all. `owm_survey.py` keeps the hero's rig (the most bones); a weapon is exported from
  its own model and hung on its hardpoint in game (`HeroWeapon`, [lucio](../heroes/lucio.md)).
- **A pure black, opaque colour image is a layer the shader colours** (Lúcio's hair cards): drawn, it's a black slab.
- **"OWM_MATERIAL ...: no colour texture" is a missing body, not a warning.** The add-on only links the colour
  textures its shader metadata knows: Jetpack Cat's fur (shader 0x10D, material 15E60) came out with only its normal and
  PBR maps, so her whole cat body exported colourless and wasn't drawn; she shipped in 0.3.16 as an empty jetpack with
  eyes and a tongue (the owner: "there was never a cat"). `owm_survey.py`'s `COLOUR_KEYS` maps such shaders to the
  colour texture's type key, read from the material's own .owmat (15E60: 86DAAC6B = her tabby albedo 6F76A). After
  every export, read its OWM_MATERIAL lines and look at the model from every side; a material with only a mask or no
  textures (an eye overlay, a glow card) is fine, one with Normal and PBR but no colour is a body part.
- **A colour linked to the engine's blank texture is no colour either** (2026-10-29, the owner: "LW's petals on his back
  aren't pink correctly"). Lifeweaver's back petals (shader 0x26, material C971) came out white: the add-on linked
  `000A` (a blank) to a colour input, so no OWM_MATERIAL warning showed. `owm_survey.py` now treats `BLANK_TEXTURES` as
  unlinked and takes the shader's `COLOUR_KEYS` slot (0x26: 58FF5009, the petal art 3EE14). To find a slot, render the
  material's textures side by side (shader, keys and paths from the .owmat) and pick the one that is the part's own art.
  After an export, list each material's base-colour image in the .glb: a lone `00000000000A` is a part with no colour.
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
- **Check a state's name against its clips' feet** before building on it: the research map's "airborne" was
  Lúcio's skating (feet on the ground, 0.17 m, as standing) and its "skating" his jump (the feet 0.7-1 m up); in game
  he skated half a metre above his shadow. A research agent names states from the graph's shape; one measurement
  settles it ([lucio](../heroes/lucio.md)).
- **Directional slots are named by field hash, the same for every hero**: Kiriko's measured runs name the 8-way
  node's fields (m_FAE5598F forward ... m_B6B5D7AB forward-left) for anyone's 8-way clips; the 4-slot node's fields
  (m_09FE31A5, m_7A2F0A64, m_CDA1FD6A, m_E15BD392) are forward, back, left, right (measured on Lúcio's jump: feet
  trailing, hips turning as the 8-way clips' do).
- **Some clips carry the hero's lift in the pelvis** (Lúcio's jump rise, Sound Barrier's 1.63 m leap): Minecraft
  moves the player already, so take the lift off the pose, and where the move is ours, match its height to the clip.
- **When a clip "looks wrong" to the owner, re-identify it from the game's own graph**: alias chains, which sounds
  sit on it, its length against the stat. Render strips. A whole action can be mislabelled: the "reload" was an
  ofuda throw, and the real reload was filed as "equip".
- **Glitches that come "at random"** may be a layer under everything: the leg kick was the idle breathing clip's
  broken keys. Read a clip's keys (glb animation channels in Python) for single-frame jumps, and scan the data for
  every case, not just the one fixed ([hand_play](../testing/hand_play.md)).

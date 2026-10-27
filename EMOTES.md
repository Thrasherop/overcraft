# Emotes: how to port them (living document)

What porting Kiriko's 23 emotes taught, as rules for every later hero. Kiriko is where the method is learned
(the owner, 2026-10-27: "Focus your modeling on *learning* how to port the emotes, not focusing on the emotes
itself"). Keep this up to date: each emote fixed adds what it taught here, each rule says where it came from.

- **Code:**
  - `client/model/EmotePlan.java`: her clips chained.
  - `EmoteData.java`: the data file read.
  - `EmoteProps.java`: props placed from the data.
  - `HeroAnimator.poseEmote`, which exposes the clip playing, its time and the posed skeleton.
  - `client/comms/EmoteCamera.java`.
- **Tools:**
  - `tools/emote_data.py` writes `assets/overcraft/emotes/<hero>.json` from the game data.
  - `tools/build_asset_pack.py props` exports every prop it names.
  - `tools/emote_contacts.py` checks an offset against a contact.
- How the data was got at: TECHNIQUES.md, "Reading an emote from the game data".

## Rules

### 0. The data first, always (owner's rule and requirement, 2026-10-27)
"WHENEVER POSSIBLE, ALWAYS TRY TO USE THE REAL DATA ... Before you implement, assume, or infer anything, try to find
the answer directly in the pulled data. Don't try to re-assemble emotes if the data has an exact cheat sheet."

The cheat sheet exists, and it's per clip. Each of her emote clips names an animation effect, and the effect lists
what it spawns. For each prop:
- **which model or entity it is**, and for an entity whether it's a projectile (thrown or dropped);
- **which hardpoint it hangs on** (a bone plus an offset, from the hero's model);
- **from when and for how long in that clip it shows**;
- **the prop's own clip**, where it names one.

So placement, timing, visibility and the prop's animation all come from the data. `tools/emote_data.py` reads it; the
mod plays it (`EmoteData`, `EmoteProps`) with no per-emote code. A new hero is: run the tool, run the pack's props
step, look.

The blend-tree set (`STUUnlock_Emote.m_emoteBlendTreeSet`, 021) holds, per clip, a `STUAnimGameData_PoseData`
(by `m_animationID`) whose `m_geoSetFlags` say which of her geosets are hidden at each moment: rule 7. Its clip chain
(`m_onFinished`) is not read yet: `EmotePlan` finds it from poses instead.

### 1. Say what the emote *is*, then check the data against it (owner: "use common sense and critical thinking")
Write one line per emote of what happens in it, and check every frame against it:
- Skateboard: she rides a board, sipping a bubble tea. The board is under her feet; the cup is upright and reaches
  her mouth.
- Donut Break: she eats a donut. It is gone once eaten.
- Mic Drop: she drops a mic. It leaves her hand and falls.
- Rainy Stroll: she strolls under an umbrella. The handle is in her fist and the canopy is over her.
- Friendly Kitty: a cat jumps into her lap. The cat is in front of her and her hands rest on it.

Every one of these was wrong at first, placed by eye. Data plus this check got them right. When the data seems wrong
("the donut is gone at 1.0 s, too early"), check it with a contact (rule 3) before overriding it: the donut is 3.3 cm
from her mouth at exactly 1.0 s. She pops it in whole.

### 2. Not all props are in hands (owner, the skateboard)
A prop hangs on whatever hardpoint the data names. On Kiriko:
- **01B6**, on `bone_097F` (under `Pelvis`): the clips move it to the floor for the skateboard (under her feet), into
  her lap for the cat, beside her for the fox and the pod.
- **000A / 000B**, on `Weapon_R` / `Weapon_L`: in a hand (the tea, the donut, the mic).
- **none**: the entity's own origin (the Whirlwind swirl).

### 3. Hardpoints: from the hero's model; borrowed ones checked by contact
- A model's hardpoint table is in its STU chunk (`STUModel.m_hardPoints`). DataTool's .owmdl export writes only some.
- Our exported bones keep Overwatch's own frames (to three digits), so an offset applies to our glb's bone as it is.
  Blender's armature turns every bone (Y down the bone): reading hardpoints there gave a false "quarter turn about X"
  on every hand hardpoint.
- **When the hero's model lacks one** (Kiriko has no 000A / 000B), take the bone another model hangs it on, with no
  offset. Lúcio's 000A is `Weapon_R` turned 180 degrees about X, and on Kiriko that turned her tea upside down:
  - with that offset, the straw's top never came nearer her mouth than 0.55 m;
  - on the bone itself, it reaches 3 cm from her mouth 7.27 s into the ride (her sip), the cup upright.

  (`tools/emote_contacts.py`.) Check every borrowed hardpoint so: find a contact the emote must make.
- **When no model defines one** (Rainy Stroll's umbrella, 02A9): inferred from her clip, labelled as such in the data
  file (`"inferred"`): the prop bone her clip keeps at her right hand while the umbrella shows (`bone_2BAE`).

### 4. Root motion
- We keep her on her spot: the emote's root motion is taken off `bone_0001`.
- A prop on one of her bones moves with her by itself. That covers every hardpoint prop: a hardpoint is a bone.
- Only a prop at the entity's origin (no hardpoint: Whirlwind) needs the motion that was taken off her
  (`HeroAnimator.emoteRootShift`).
- Before the hardpoints were read, the cat was placed from her feet and landed behind her as she sat (she moves
  0.6 m).

### 5. Props with clips of their own
- A model spawn names its prop's clip. An entity spawn doesn't: its clip is the one in the emote's folder that
  animates another skeleton and lasts as long as the window (starting with it) or as her clip (running with it: the
  pod's intro).
- A clip is hers when at least 80% of its bones are her skeleton's (91-100%). Props' clips are 8-62%.
- The pack builder exports each prop with its clips named by id. The mod samples the clip at her clip's time minus
  where the prop's clip starts.

### 6. Visibility and release: the spawn window
- A prop shows from its start to its end in her clip: the donut 0.27-1.0 s in Donut Break's clip; the tea and board
  through each skateboard clip.
- A projectile entity (Mic Drop's mic, window 0-0.93 s) falls from where it was when its window ends. The fall itself
  (9.8 m/s² down to her feet's level) is ours: the data says it's thrown, not how it moves.

### 7. Her own weapons: the GeoSetFlags, and partial clips over the pose under them
- **Each clip's GeoSetFlags** are a timeline of masks, sampled evenly over the clip (at most 45). A set bit hides that
  geoset, and each of her model's submeshes carries one geoset bit (`SubmeshDescriptor.m_40`).
  - Her ofuda is 1, her Suzu 2, her three kunai 4: matched to our .glb's pieces by vertex count, the geoset with the
    most vertices not above the piece's (the export splits vertices, never merges).
  - Kneeling goes 1 -> 3 -> 7: she puts away the ofuda, then the Suzu, then the kunai at about 1.6 s. In our frames
    the kunai is in her hand until then.
  - Laugh is 7. Before the masks, her kunai stuck out of her mouth as she covered it.
  - Sign: Thanks is 7 through its middle. Without the stray ofuda, it reads as what it is: sign language, her hand
    from her chin outward. A critique had read the ofuda as something she writes on.
  - Bit 8 (only Pod Pet and Friendly Kitty) is her body's geoset, which can't be what it means there: not applied.
- **Partial clips:** Listen Up, Salty and Mic Drop drive 167 to 223 of her 295 bones. Played over her rest pose, the
  bones they don't drive put her kunai a metre away and her ofuda in the sky. They play over the pose under the emote
  instead: only the bones a clip drives are taken from it. Our export writes every bone into every clip, filling the
  undriven ones with their rest pose, so a channel constant at its bone's rest is left out (`HeroAnimator.driven`).
- Listen Up and Salty have no GeoSetFlags, and their clips still park the ofuda's bones a few metres up. Overwatch
  layers two shared hand clips over them (12625, 12638: Open). Until those are read, a weapon piece more than 0.3 m
  from both wrists in a clip with no masks isn't drawn (ours, labelled in `EmoteProps.PARKED`).

### 7b. Look like Overwatch, not like our other effects
- Draw a prop with its own texture. The fox was first drawn as Kitsune Rush's see-through spirit and came out
  glowing white (owner: "The fox shouldn't be glowing white"). Its texture is the blue kitsune.
- **Her coat in an emote:** its simulation restarted every frame of an emote and never fell (found with a dev
  readout of its lowest point). Her pose is worked out more than once a frame, and the cloth took its time from the
  animator's shared clock, so every call after the first got no time. It keeps its own clock now. With the cloth
  moving, the floor constraint matters: off, the coat sinks 0.50-0.66 m under the floor kneeling or sitting; on, it
  lies on it.

### 8. Review every emote critically, at every angle
- **Frames at exact times:** hold the emote there with the harness (`emoteat <s>`), at the data's windows. Sleeps
  between screenshots drift. Cropped close to her: at full frame an upside-down cup read as "a cup in her hand".
- **Zoom on every contact:** hands on the prop, feet on the board, the prop at her mouth. Then measure them
  (`tools/emote_contacts.py`).
- **Have a separate agent critique** the frames and the code without being told what's already "fixed" (owner:
  "have a sub agent critique your implementation and screenshots of every emote").
- **Send the owner** before and after images of anything fixed.

## Kiriko's spawn table (from the data, build 154088)
| Emote | Prop (model) | Hardpoint | When (clip: seconds) | Its own clip |
|---|---|---|---|---|
| Donut Break | donut 15C94 | 000A | F327: 0.27-1.0 | none |
| Sweet Treat | donut 15C94 | 01B6 | 1823C: 1.07-4.83 | none |
| Mic Drop | mic 1A146 (projectile entity) | 000B | 15C8B: 0-0.93, then falls | none |
| Skateboard | tea 863C | 000A | 14DBF: 0.2-1.0, then every ride clip | none |
| Skateboard | board 197FB (entity) | 01B6 | 14DBF: 0.27-1.0, then every ride clip | none |
| Friendly Kitty | cat 1CBA0 | 01B6 | 1A710: 0.17-4.37; 1A711 loop | 1A712; 1A713 |
| Rainy Stroll | umbrella 181A6 | 02A9 (inferred) | 11FE2: 0.63-2.83; 11FE3 loop | 1227E; 1227F |
| Fox Cuddles | fox 105FB (entity) | 01B6 | E79B: 0.77-4.5; E79D 0-0.17; E79C loop | E921; E91F; E920 |
| Pod Pet | pod 1B7DD (entity) | 01B6 | 18A05: 0.07-4.4; 18A06 loop | 19C45; 19C46 |
| Whirlwind | swirl 118F1 | none | C216 / C2E6: whole clip | C217 / C2E7 |

## Open
- **The hand layers** 12625 (125 bones) and 12638 (43), listed in Listen Up's, Salty's and Mic Drop's blend trees:
  how they layer over those emotes. Reading them would replace the 0.3 m "parked" rule.
- **The camera** orbits at 0.6 of her standing eye height: in every emote her feet are behind the ult meter, and
  sitting or kneeling her lap is too (second critique).
- **The blend-tree chain** (`m_onFinished`): her clips' order from the data, replacing EmotePlan's pose matching.
- **Outros** (Fox Cuddles' E79D, Friendly Kitty's 1AB09): played when the emote ends early. Not played yet.
- **02A9**, the umbrella's hardpoint: in no extracted model. Look for it in other heroes' models when they're
  extracted.

## Log
- 2026-10-27:
  - First props on the weapon bones by guess. The owner found the cat behind her, the umbrella stuck to her hand,
    the board in her hand and the donut never eaten.
  - Fixed by eye with hardpoints read through Blender, a root-motion shift and contacts.
  - Then the game data: spawn tables per clip, hardpoints from the models, prop clips paired by length.
    `EmoteProps` became data-driven, and the old hand-written table, the root shift for hand props and the wrist
    quarter turn were removed.
  - The tea's borrowed hardpoint was checked by contact (the sip) and corrected.

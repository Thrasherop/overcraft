# Emote reproduction: porting a hero's emotes from the game data

How to port any hero's emotes so they play as Overwatch's do. It's the method; what one hero taught, with their
numbers, is in the per-hero notes next to this one ([kiriko_emotes](kiriko_emotes.md)). Kiriko was where the method
was learned (the owner, 2026-10-27: "Focus your modeling on *learning* how to port the emotes, not focusing on the
emotes itself").

Up: [Techniques](../README.md) · built on: [principles](../principles.md), [reading_game_data](../game-data/reading_game_data.md),
[model_data](../game-data/model_data.md), [animation_import](../animation/animation_import.md) · the coat:
[cloth](../animation/cloth.md) · review: [visual_review](../testing/visual_review.md)

## A new hero, step by step
1. Extract their emotes (DataTool `extract-unlocks "<Hero>|emote=*"`) and their model with its clips
   ([animation_import](../animation/animation_import.md)).
2. Run `tools/emote_data.py <hero> ...` (the research venv: [reading_game_data](../game-data/reading_game_data.md)):
   it writes `assets/overcraft/emotes/<hero>.json` with every clip's spawns, windows, geoset masks, the hardpoints
   and the weapon pieces' geosets.
3. Run `tools/cloth_data.py <hero> ...` if the model has cloth: `assets/overcraft/cloth/<hero>.json`
   ([cloth](../animation/cloth.md)).
4. `py tools/build_asset_pack.py props <DataTool output>`: every prop the data names, as `hero_models/prop_<id>.glb`.
5. Look at every emote ([visual_review](../testing/visual_review.md)), check every borrowed or inferred number with a
   contact, and have a separate agent critique the frames.

The mod's code is the same for every hero: `EmotePlan` (her clips chained), `EmoteData` (the data file),
`EmoteProps` (props and weapons from it), `HeroAnimator.poseEmote` (the clip playing, its time, the posed skeleton),
`ClothSim` (the coat), `EmoteCamera`.

## 1. Say what the emote *is*, then check the data against it
(The owner: "use common sense and critical thinking".) Write one line per emote of what happens in it, and check every
frame against it: a board ridden is under her feet; a donut eaten is gone; a dropped mic falls; an umbrella's handle
is in her fist. Every one of Kiriko's was wrong at first, placed by eye; data plus this check got them right
([kiriko_emotes](kiriko_emotes.md)).

## 2. What each clip spawns: the animation effect
Each emote clip names an animation effect (an 08F) listing what it spawns, and when
([reading_game_data](../game-data/reading_game_data.md#animation-effects-what-a-clip-spawns)). For each prop:
- **which model or entity it is**; an entity with a projectile component is thrown or dropped;
- **which hardpoint it hangs on**;
- **from when and for how long in that clip it shows**: that's its visibility. Nothing scales to zero when a donut is
  eaten; the window ends;
- **the prop's own clip**, where a model spawn names one.

## 3. Not all props are in hands
A prop hangs on whatever hardpoint the data names: a weapon bone (a hand), a prop bone the clips move (under her feet
for a skateboard, into her lap for a cat), or none (the entity's own origin). The owner, on the skateboard: "not all
props are in hands."

## 4. Hardpoints: from the hero's model; borrowed ones checked by contact
- Read them from the model's STU chunk, not the exported .owmdl, which drops some
  ([model_data](../game-data/model_data.md#hardpoints)).
- Our exported bones keep Overwatch's own frames, so an offset applies to our glb's bone as it is. Never read frames
  from Blender's armature ([animation_import](../animation/animation_import.md#our-bones-keep-overwatchs-frames)).
- **When the hero's model lacks one**: hang it on the bone another model uses, with no offset, then check it with a
  contact. Kiriko lacks the standard weapon hardpoints 000A / 000B. Lúcio's 000A offset turned her tea upside down,
  and the sip contact found it.
- **When no model defines one**: infer it from the clip (the prop bone the clip keeps at the right place), and label
  it "inferred" in the data.
- **Contacts:** `tools/emote_contacts.py <glb> <bone> <point on the prop> <offset> <clip...>` measures how near a
  point on the prop comes to her mouth over the clip, and which way the prop's +Y points. For a held thing, also
  measure its axis against the knuckle line (index to pinky): a fist holds a handle across the palm (the mic: 9 degrees
  off the knuckle line, as the verified tea is). Run it before overriding the data on a hunch, and before trusting a
  frame: a front view along her arm made a correct mic look like it covered her face.

## 5. Props with clips of their own
- A model spawn names its prop's clip. An entity spawn doesn't: its clip is the one in the emote's folder that animates
  another skeleton and lasts as long as the window (starting with it) or as her clip (running with it). Lengths agree
  to the frame.
- **Which clips are a prop's:** a clip whose bones are at least 80% hers (the union of her full-body clips' bones) is
  hers. Bone count alone misleads.
- The pack builder exports each prop with its clips named by id; the mod samples the prop's clip at her clip's time
  minus where the prop's clip starts.

## 6. Root motion
We keep her on her spot: the emote's root motion is taken off the mover bone (`bone_0001`). A prop on a hardpoint is on
one of her bones, so it moves with her by itself. Only a prop at the entity's origin needs the motion that was taken
off her added back (`HeroAnimator.emoteRootShift`).

## 7. Her own weapons: the blend tree's GeoSetFlags
- The emote's blend-tree set holds, per clip, a timeline of geoset masks: a set bit hides that geoset
  ([model_data](../game-data/model_data.md#geosets)). This is how Overwatch puts weapons away and brings them back.
  Before it was read, Kiriko's kunai stuck out of her mouth in Laugh.
- Map each weapon piece of our .glb to its geoset bit (the tool does it by vertex count). Apply only to stowed weapon
  pieces; a bit that would hide the body can't mean that.
- **Partial clips** drive only some of her bones (upper-body emotes). Play them over the pose under the emote, taking
  only the channels the clip drives; our export fills undriven bones with their rest pose, so a channel constant at
  rest is left out (`HeroAnimator.driven`).
- Overwatch layers shared hand clips over some partial emotes. Until those are read, a weapon piece no wrist holds
  isn't drawn there (ours, labelled).

## 8. The coat: sometimes animated, sometimes simulated
A hero's cloth bones are never keyed in a clip, but that doesn't make the cloth physics. Overwatch's cloth (Domino)
places each cloth bone from driver bones the clips animate by hand, and adds light physics on top. For Kiriko the
drivers are everything: in Kneeling and Fox Cuddles the coat is authored to the pixel. Read the hero's cloth chunk
before simulating anything: [cloth](../animation/cloth.md), [kiriko_cloth](../animation/kiriko_cloth.md).

## 9. Chaining a hero's clips
An emote's `Animations/` folder isn't a playlist: it mixes the body's clips (`0/*`), additive or partial layers
(`4/*`; `16/*` holds both layers and long loops) and props' clips, and id order isn't play order. `EmotePlan` chains
them by pose: it compares first and last frames over the core bones (mean 1 - |q.q'|).
- a one-shot starts and ends in the idle pose;
- an intro starts there;
- a loop's first frame is its last;
- an outro ends there.

Joins measure 0.000 to 0.007, non-joins 0.013 and up. The blend tree's own chain (`m_onFinished`) would replace this
when read.

## 10. The camera
Overwatch's emote camera leaves her eyes, swings round to her front and orbits freely with the mouse. Ours orbits 2.5
blocks out at 0.4 of her eye height: at 0.6 her feet sat behind the ult meter (a critique). Mixin notes:
[minecraft_26](../minecraft/minecraft_26.md#a-camera-of-our-own).

## 11. Review every emote critically, at every angle
- **Frames at exact times:** hold the emote at a time with the harness (`emoteat <s>`), at the data's windows, front,
  side and back, cropped close ([visual_review](../testing/visual_review.md)).
- **Zoom on every contact**, then measure it (section 4).
- **Have a separate agent critique** the frames and the code without being told what's already "fixed" (owner: "have
  a sub agent critique your implementation and screenshots of every emote"). Check its claims against the data:
  critics misread a pose from one angle too.
- **Send the owner** before and after images of anything fixed.

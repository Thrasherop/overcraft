# Ana's emotes

What Ana's 16 emotes taught (2026-10-31). Up: [Techniques](../README.md) · the method:
[emote_reproduction](emote_reproduction.md) · her hub: [heroes/ana](../heroes/ana.md) · the hero before her:
[mercy_emotes](mercy_emotes.md)

## Her rifle is an entity in slot 1EC3
- Her hero entity 1424's children (`research\ana\work\ents_ana_out.txt`) give the rifle 159C in slot **1EC3** on 00C9
  (bone_0606), the teacup 1611 in 1F90, the teabag 1610 in 1F8F, the saucer 1609 in 1F66 (H).
- To find a new hero's slots, run `tools/emote_data.py` with `EMOTE_CONTROLS=1`: it prints every EntityControl (clip,
  slot, action, clip played, window). The slot that her own emotes put away (action 1) in most clips is the weapon's.
- Tea Time puts the rifle away from 0.067 s and plays two clips on it (3D7E going away, 3E90 coming back at 3.3 s):
  exported into `ana_weapon.glb` from a working copy of its folder (`tools/clipmaps/ana_rifle.txt`), as Mercy's staff
  clips were.

## Props that are children of the hero (Tea Time, Protector)
- Tea Time's cup, teabag and Protector's saucer are spawned by no 08F: they're children of her hero entity, shown by an
  EntityControl that plays a clip on their slot (action 4). `emote_data.py`'s `CHILD_PROPS` turns those into spawns on
  the child's hardpoint; `build_asset_pack.py props` finds the model in the hero's own skin folder.

## One clip per stick (Yut Nori)
- Yut Nori spawns four copies of the same stick at once, each with its own clip (640C-640F). The control-to-spawn
  matching gave all four clips to one stick; among copies of one model it now gives each control its own (other heroes'
  pairs, two different entities, match as before).

## What a prop hangs from is posed at rest where the clip is silent
- The mod keeps a bone the emote clip doesn't drive in the pose under the emote (Kiriko's kunai,
  HeroAnimator.poseEmote). But a prop's attachment chain is keyed relative to its rest:
  - the teabag (000B: bone_002E under bone_002D under Weapon_L) kept her rifle grip's place from her idle: 0.2 m off
    her fingers, floating;
  - the rifle's carrier bone_0606 is keyed in Listen Up and Salty under bone_0605, which those clips leave alone: the
    rifle floated 0.35 m off her hand (the owner: "the gun just kinda floating");
  - the mic in Mic Drop (000B again) was 0.13 m off her hand.
- With those links at rest, every one lands on her hand to the millimetre over the whole window
  (`tools/emote_bones.py`, which prints bones' places and distances over a clip). `EmoteProps.atRestUndriven` does it
  for the chain from the carrying bone up to the first bone the clip drives, for props and her weapon. A bone that is
  the hand itself (Weapon_R / Weapon_L) is left as it was: the hand's chain is the clip's, and Lúcio's amplifier,
  Mercy's staff and Kiriko's tea were checked in play that way.
- A general version (every undriven parent of a driven bone back to rest) was measured first and dropped: it would have
  moved elbows, spines and hips in dozens of other heroes' emotes with no evidence for any of them.
- Clips export every bone, the undriven ones at rest: "driven" means `HeroAnimator.driven`'s channels, not the file's.

## The gallery preview tree (Heroic)
- Her Heroic emote's blend tree set (0639) is also her hero gallery's preview set. It holds two items: the emote's on
  tree 006B (3C3B with the shared additive 3B7D, as every one of her emotes) and the gallery idle on tree **05C6** (3C2B,
  3C44, 3C1F, 3B6F). 3C2B starts and ends in her stance and comes first in id order, so the plan played it: a 0.9 s
  twitch, no sounds.
- `emote_data.py` marks clips found only in tree 05C6 items `"preview"`; `EmotePlan` leaves them out. Kiriko's, Lúcio's
  and Jetpack Cat's emote data predate the mark (regenerate to apply it).

## Listen Up and Salty put the weapon away for the whole emote (tree 058F)
- Decoded 2026-10-31 (`E:\Overwatch Archive\research\emote-trees\tree_058F.md`): the tree's parameter indices count
  from 1, so 178AD (clip #3, 0 bones, 0.3 s) is not in the Sequence Mercy's note put it in: it plays as its own Animation
  node on the top state machine's additive layer, with no weight, driver or condition (H).
- 178AD is Mic Drop's put-away block (the same 38 slot identifiers with action 1) moved into a clip of its own; Mic Drop
  uses the same tree with clip #3 empty because its main clip carries the block over its whole length. Lúcio's 185DE is
  the same pairing for his 12CC (H). Mercy's hands in Listen Up and Salty never come near her staff's grip (12-41 cm).
- So the window covers the whole emote. **M:** whether node 58 holds past 178AD's 0.3 s isn't read (its time
  controller has no loop flag). `emote_data.py` (`CARRIER_TREE`) gives the main clip a stow window over its whole length,
  marked `"stowBy"` with the M.
- Seen: Ana's rifle away through Listen Up and Salty, back in her hands after.

## Open
- Mercy's and Lúcio's emote data predate this rule (regenerate them to put their weapons away in these two emotes);
  Kiriko's and Lifeweaver's weapon slots aren't in 178AD at all (research note, "Per hero").
- Nothing listened to in her emote sounds' builds (the map: `tools/soundmaps/emotes.txt`, Ana's section).

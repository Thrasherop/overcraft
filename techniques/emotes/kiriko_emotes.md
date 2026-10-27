# Kiriko's emotes: what her 23 emotes taught

Kiriko's numbers and quirks, the worked example of [emote_reproduction](emote_reproduction.md). Up:
[Techniques](../README.md) · hero hub: [kiriko](../heroes/kiriko.md) · her coat: [kiriko_cloth](../animation/kiriko_cloth.md)

Data: `assets/overcraft/emotes/kiriko.json` (tools/emote_data.py, build 154088).

## What each emote is (rule 1), and what was wrong at first
- **Skateboard:** she rides a board, sipping a bubble tea. First placed with the board in her hand; the data puts it
  on hardpoint 01B6, which her clips move under her feet. The tea was upside down on a borrowed hardpoint until the
  sip contact fixed it.
- **Donut Break:** she eats a donut. First it was never eaten; the data's window ends at 1.0 s, when it's 3.3 cm from
  her mouth.
- **Mic Drop:** she holds a mic out and drops it (a projectile entity: it falls when its window ends at 0.93 s).
- **Rainy Stroll:** she strolls under an umbrella, held by its hooked handle.
- **Friendly Kitty:** a cat walks up and settles in her lap under her hands. First it landed behind her (placed from
  her feet while she moved 0.6 m sitting).
- **Fox Cuddles:** the fox bounds in, she kneels and hugs it. **Pod Pet:** a pod flies down to be patted.
  **Whirlwind:** a swirl of ofuda papers (a model with no hardpoint).
- **Sign: Thanks** is sign language (her hand from her chin outward). A critique read it as writing on an ofuda that
  shouldn't have been there (the geoset masks hide it).

## Spawn table
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

## Hardpoints
- **01B6** on `bone_097F` (under Pelvis), identity: from her own model.
- **000A / 000B** on `Weapon_R` / `Weapon_L`: her model has neither (its STU table has 39 others). Lúcio's model
  (DB3F) defines them, 000A turned 180 degrees about X.
  - Borrowed as the bones with no offset.
  - The tea's straw then reaches 3 cm from her mouth 7.27 s into the ride (her sip), upright. With Lúcio's turn: never
    nearer than 0.55 m, upside down.
  - The mic's axis is 9 degrees off her knuckle line, across her palm, as the tea's is.
- **02A9** (the umbrella): in no extracted model. Inferred as `bone_2BAE` (the prop bone her clip keeps at her right
  hand); it looks right.
- Her wrist hardpoints (04E2 on Wrist_L; 04E3 / 0522 on Wrist_R) have identity offsets. The "quarter turn about X"
  first read was Blender's bone convention.

## Clips
- A clip is hers at 91-100% of its bones in her skeleton. Her props' clips are 8-62% (fox 141 bones, cat 138,
  umbrella 11, pod 13, swirl 26). One of her own clips has 295 bones where the others have 296.
- Pairing by length: the fox's E921 is as long as its window (3.733 s); the pod's 19C45 is as long as her clip (4.4 s,
  starting with it).
- Orion Dance's way in is its second clip (by pose).

## Weapons (GeoSetFlags)
- Her geosets: **ofuda 1, Suzu 2, kunai 4** (by vertex count: 372, 1371 and 7458 in our glb against 200, 1054 and 6779
  in the data). Bit 8 is her body: set only in Pod Pet and Friendly Kitty, and not applied.
- Kneeling: 1 -> 3 -> 7 (ofuda, then Suzu, then the kunai at about 1.6 s). Laugh: 7 (the kunai was in her mouth).
  Heroic, Challenge: 1 (the kunai shows, as meant).
- Partial clips: Listen Up (167 bones), Salty (208) and Mic Drop (223) of her 295. Listen Up and Salty have no masks.
  Their ofuda's bones are parked a few metres up, so a weapon piece more than 0.3 m from both wrists isn't drawn there
  (ours).

## Her coat in the emotes
Driver bones keyed per emote (of the 38 coat drivers, [kiriko_cloth](../animation/kiriko_cloth.md)):
| Driver bones keyed | Emotes |
|---|---|
| All 38 | Kneeling, Fox Cuddles, Blooming, Choreography, Friendly Kitty, Headbanging |
| 24-36 | Laugh 36, Donut Break 34, Isn't This Fun 30, Cheer 29, Orion Dance 28, Challenge 27, Heroic, Pod Pet and Skateboard 26, Sign: Thanks 25, Killer Beam 24 |
| 8-20 | Whirlwind 20, Rainy Stroll 18, Sweet Treat 8 |
| None | Listen Up, Mic Drop, Salty (the partial emotes) |

The owner, on Kneeling and Fox Cuddles driven: "spot on ... perfect matches".

## Open
- **The hand layers** 12625 (125 bones) and 12638 (43), in Listen Up's, Salty's and Mic Drop's blend trees: how they
  layer. Reading them would replace the 0.3 m "parked" rule.
- **Orion Dance's way in** (1AA47) has masks 7,7,7,0, the shape of a way out: her weapons flash for its last 0.1 s.
  The blend-tree chain would settle it.
- **Pod Pet at 0.3 s:** the pod sits behind her head as it starts (a critique, not certain it's wrong).
- **Outros** (Fox Cuddles' E79D, Friendly Kitty's 1AB09) aren't played when an emote ends early.
- **Domino's physics on top of the coat** ([kiriko_cloth](../animation/kiriko_cloth.md)).

## Log
- 2026-10-27:
  - First props on the weapon bones by guess. The owner found the cat behind her, the umbrella stuck to her hand,
    the board in her hand and the donut never eaten.
  - Fixed by eye with hardpoints read through Blender, a root-motion shift and contacts.
  - Then the data: per-clip spawn tables, the model's hardpoints, clips paired by length. The hand-written tables
    were removed. The tea's borrowed hardpoint was checked by its sip and corrected.
  - Then the geoset masks (her weapons) and partial clips over the pose underneath.
  - Then the coat from its drivers.

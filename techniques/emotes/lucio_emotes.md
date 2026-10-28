# Lúcio's emotes: what his 17 emotes taught

Lúcio's numbers and quirks, the second worked example of [emote_reproduction](emote_reproduction.md) after
[kiriko_emotes](kiriko_emotes.md). Up: [Techniques](../README.md) · hero hub: [lucio](../heroes/lucio.md) · sounds:
[sounds](../sound/sounds.md)

Data: `assets/overcraft/emotes/lucio.json` (tools/emote_data.py, build 154088). Research:
`E:\Overwatch Archive\research\lucio\emotes.md` (the blend trees, the spawn table, the cosmetic sets).

## His weapon is an entity, not a geoset (the nuance that matters)
- Kiriko's kunai, ofuda and Suzu are pieces of her own model, hidden by GeoSetFlags. Lúcio's Sonic Amplifier is an
  entity of its own on hardpoint 000A (`HeroWeapon`). Emotes put it away with an **EntityControl that names no clip**
  (action byte 204), over a window: `"stow"` in the data. Heroic, Listen Up and Salty have none, so he holds it
  (Heroic: on his shoulder).
- Listen Up, Salty and Mic Drop carry controls with other bytes (254 / 16 / 99). Taking every clip-less control as a
  stow put the gun away in Listen Up, which the research said keeps it: filter on 204.
- The gun's place in an emote is the emote's pose (`animator.emotePose()`), as the props'. Taken from the last
  gameplay pose, it hung where his hand was before the emote.
- **Geoset 1 goes with it:** a cable on his weapon arm, weighted to Weapon_R. The masks hide it with the gun. Not
  hidden, it stretched out to wherever the stowed gun's bone was parked (seen in In the Groove).
  - It isn't a "stowed piece" (small, parked at the origin) like Kiriko's, so the vertex-count match missed it. The
    exported .glb's `Submesh_<i>` is the model's submesh i: match by order, against the model variant whose counts
    agree (DB3F has two variants with the same index; a few counts grow along seams, 4506 to 4510).
  - The renderer hides it by piece (`HeroModelRenderer.PIECE` + its index), not only by its first joint.
  - Where a clip has no mask (Heroic), a weapon-entity hero's pieces follow the stow windows; Kiriko's fallback rule
    (a piece far from both wrists isn't drawn) would have hidden his cable while he holds the gun.

## Props named by the data
- An EntityControl that names a clip plays it on the entity spawned with it. When several spawn at once (A Suave
  Gift's F559 and F698 at 0.433 s), the clip goes to the one whose skeleton holds its bones (B4D9, 7 bones, fits
  F559). Kiriko's length pairing is now the fallback; her Pod Pet's pod clip moved from 0 to 0.067 s with its control.
- **Nested spawns aren't placed yet:** Dance Party's DJ booth (67A7) has clips of its own whose effects spawn his
  amplifier on the booth's hardpoint 01B6. Our props only spawn from the hero's clips.

## Sounds
Every emote has sounds and lines in its clips' effects ([sounds](../sound/sounds.md#emote-sounds)). Dance Party is
interactive music: only its first segment is built, and the same music plays on two buses at once in the data (one
of each pair plays: `DUPLICATES` in emote_data.py).

## Log
- 2026-10-27: all 17 from the data in one pass (model export with `--emotes`, `emote_data.py lucio`, `props ...
  lucio`), played in game with `emote <id>` and once from the wheel. Found playing: the gun stowed in Listen Up (the
  byte filter), the gun hung at the pre-emote hand (the emote pose), the stretched cable (geoset 1).

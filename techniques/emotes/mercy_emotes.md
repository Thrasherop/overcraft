# Mercy's emotes

What Mercy's emotes taught. Up: [Techniques](../README.md) · the method: [emote_reproduction](emote_reproduction.md) ·
her hub: [heroes/mercy](../heroes/mercy.md) · the hero before her: [lucio_emotes](lucio_emotes.md)

## Two weapons, each an entity: per-slot stow windows (2026-10-28)
- Her staff (185F) and blaster (025E) are child entities of her hero entity, in slots **12A5** and **206C**
  (`STUChildEntityDefinition.m_49F782CE` of 00E4). Each emote clip's animation effect (08F) has EntityControls naming a
  slot: **action 1 with no clip puts that weapon away over the window; action 4 with a clip keeps it out and plays the
  clip on it** (Caduceus 1627, Parasol 4DC8, Flurry 852E: the staff's own wings and head). No control: the weapon stays
  drawn. Research: `E:\Overwatch Archive\research\mercy\emote_weapons.md`.
- **TankLib reads the chunk wrong.** Its EntityControl structure predates a field: the chunk is 0x40 bytes, the
  identifier at 0x10 and the action at 0x20. What TankLib calls the "action" byte is the slot id's low byte. Lúcio's
  "action 204" was his slot 12CC (0xCC), which is why filtering on 204 worked for him and found nothing of hers. Read the
  chunk raw (`research\mercy\scripts\emote_weapons\ecec.py`).
- `tools/emote_data.py` reads the controls raw (`raw_controls`) and sorts them by slot (`WEAPON_SLOTS`): `"stow"`
  holds the staff's windows, `"stowBlaster"` the blaster's, `"weaponClips"` the clips a weapon plays;
  `HeroWeapon.inEmote` uses the one for the weapon she has out.
- The check that backs "action 1 = put away": inside the windows a held staff would pass through her head and arms
  (up to 1147 vertices); at every window edge her hand is back on it exactly. Heroic 3A7F's window is the one the
  contacts don't support (the grip stays): the data's window is kept.
- **The staff's own clips** (action 4): exported into `mercy_staff.glb` from the emotes' folders (added to
  `tools/clipmaps/mercy_staff.txt`; the model re-exported from a working copy of its folder with the three clips copied
  into its Animations, since owm_survey.py takes clips from the model's own folder), named in the emote data's
  `"weaponClips"` by the clip map, and played by `HeroWeapon` over their windows in place of the held loop. Seen in
  Caduceus: the staff's head wings swing out flat at 2.5 s and fold back by 3.7 s.
- **Checked against Lúcio:** regenerating his emote data with the raw reader (slot 12CC) gave exactly his old stow
  windows, so "204" was his slot all along and nothing of his changed (2026-10-28).

## Open
- Listen Up and Salty: the 0.3 s clip 178AD hides the staff, but when 178AD plays (an additive layer's Sequence in
  tree 058F) isn't decoded.

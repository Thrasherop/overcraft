# Mercy (hub)

Everything learned about Mercy, by subject. The fourth hero, and the first whose abilities were read node by node from
her statescript graphs rather than taken from the wiki. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · her emotes: [mercy_emotes](../emotes/mercy_emotes.md)

## Her notes and sources
- **The data map:** `E:\Overwatch Archive\research\mercy\mercy-map.md` (states, clips, abilities, game values),
  corrected by `kit_details.md` (2026-10-28: her graphs decompiled with their operands; every finding with its node
  ids, H/M/L). Weapons: `weapon_attach.md`; emote weapons: `emote_weapons.md`; effects `fx\`; sounds `sounds\`.
- **Numbers:** `MercyStats`, each with its source (BZ / W / D and the value id); `tools/reference/mercy.md` (the public
  sources), `tools/reference/mercy-plan.md` (the port's plan and its open items).

## What her graphs said that the wiki didn't (kit_details.md, 2026-10-28)
- **Guardian Angel isn't "fly to the ally".** The destination is worked out once, as the flight starts: the ally's
  feet 0.5 m up, pulled back toward her 3.7 m (less half the height between them), raised a tenth of any drop. She
  stops beside the ally, not in them, and doesn't follow them if they move. No end for a lost target, no time limit.
  She won't start toward anyone within 4 m. Her beam's target is taken first with no cone or sight test.
- **The launches are one formula** (006E #1027): (11 + 6 m) m/s by the momentum m (full after 1 s of flight, 0.68 s in
  Valkyrie), x1.3 in Valkyrie, x(1 + 0.2 m) for Crouch's straight-up launch, x(1 + 0.2 m |pitch| / 89) for Jump's.
  Jump goes where she looks plus 0.5 + 0.1 m up, normalized; back held reverses the look and is 20% slower. The checks
  that settled it: 17 x 1.3 x 1.2 = 26.52 against the wiki's 26.5 for Valkyrie's super jump. Jump or Crouch still
  launches for 0.35 s after she arrives.
- **A number that matched for the wrong reason:** the map read 1BE6 = 1.3 as the beam's 1.3 s linger (the wiki's
  number). It's Valkyrie's launch factor; the beam lock's linger field holds 1.0. Matching a public number isn't proof
  that a value is that thing: find its reader.
- Flash Heal casts on the staff's live lock (so it needs the beam held); Valkyrie's blaster costs no ammo; the
  reload gives the ammo back 0.91 s in (1.4 x 0.65); Crouch also triggers Angelic Descent (an option, on by default).
- Not in the data: the half-or-whole angle of her three cones (30 / 40 / 60; read as the angle from the aim, as the
  wiki's "maximum view angle" wording suggests), what the engine's move leaves her velocity at, Valkyrie's vertical
  speed, the soul's lifetime.

## The second gaps pass (gaps2.md, 2026-10-28)
- **Guardian Angel's target marker is Swift Step's:** the game's generic ability-target preview (013C.116, 84 graphs use
  it) on the ally's chest, shown while it's ready and she isn't flying or just launched. Drawn by the same HUD code.
- **After a launch nothing damps her for 1 s** (F808ACE8 set 0, read as air damping by where else it's set); in
  Valkyrie 0.5 s, with her speed x2.4444 (22 m/s) and knockback x0.2. A Valkyrie crouch launch rises at 26.5 m/s for
  0.5 s, 13.3 m: measured in game, the data's number.
- **Property names from the custom-game settings:** a property the Workshop's settings name (Movement Speed,
  Movement Gravity, Jump Vertical Speed) pins its meaning; its adds and sets stack as base x set x (1 + adds), checked
  against the wiki's 2.25 m/s Resurrect in Valkyrie.
- **The hero select carries its own weapon:** her select entity (3A80) holds the staff on Weapon_R for every clip;
  Lúcio's puts his amplifier away in his intro and out in his hold. Drawn on the select pose (`HeroWeapon.placeShowcase`).

## Found playing (2026-10-28)
- **A flat survival test world spawns slimes,** and their knockback looked exactly like a movement bug (a launch
  killed after one tick, then a backward fling). Set `difficulty peaceful` before measuring movement.
- **An ability's landing hurt her.** The "landed safely" flag cleared the tick the server saw her on the ground, and
  Minecraft's fall check could come a tick later: a crouch launch's landing took half her health, Valkyrie's end from
  40 m killed her. While the flag is up the fall distance is now cleared each tick
  ([minecraft_26](../minecraft/minecraft_26.md#a-fall-is-checked-after-the-ground): the general point).
- **Valkyrie drifts.** Her mover's air drag is every hero's 0.35/s, so with no input she coasts (9 to 4.7 m/s in
  1.9 s), and a super jump keeps rising for most of the ult (about 40 m). That is the data; whether it matches the game
  is for the owner's play.
- **Her shots leave her first-person blaster's muzzle** (0.16 m right, 0.11 down, 0.71 ahead of the eyes, measured
  from the posed view model, the same at every pitch): her data launches each volley from the blaster's hp 0006.

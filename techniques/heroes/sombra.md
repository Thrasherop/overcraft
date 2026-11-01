# Sombra (hub)

Everything learned about Sombra, by subject. The seventh hero (issue #64), third on the owner's 2026-10-28 list: a Support
since her 2026-10-06 rework (Tactician). Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · built the way Ana was: [ana](ana.md)

## Her notes and sources
- **Numbers:** `tools/reference/sombra.md` (Blizzard's patch notes to 2026-11 and the wiki's revision 408595, each number
  with its history; her move to Support on BZ 2026-10-06).
- **The data map:** `tools/reference/sombra-map.md` (her kit from her graphs, node by node, H / M / L); the detail in
  `E:\Overwatch Archive\research\sombra\` (`work_kit\`, `work\assets.md`, `work\clipmap.md`, `extract\`, build 154088).
- **The plan:** `tools/reference/sombra-plan.md` (what maps onto the mod, the Minecraft decisions, the order, open questions).

## What step 1 taught
- **A patch after the data build:** BZ 2026-10-13 changed three of her numbers after build 154088 (pistol 7 -> 6.5,
  Cyberspace 40 -> 20 and 12 -> 13 s). Check the patch notes after the data build's date before trusting the data's
  numbers; `SombraStats` says which source each number takes.
- **The data names her keys:** Cyberspace is ABILITY 1 (Shift) and Translocator ABILITY 2 (E, again to teleport), the
  reverse of her old Stealth / Translocator; read the loadout's button ids (`sombra-map.md`, "Buttons") before wiring.
- **Her pistol is an entity of its own** (1221, model ECA1) on 000A = Weapon_R with no offset, as Mercy's blaster: it needs
  no idle clip to sit in her hand (`HeroWeapon`), her own idle holds it up by her shoulder.
- **Her view model has Ana's shape of clips** (idle, one walk, jump, fall), so the animator's first-person rules for Ana
  serve her too (`HeroAnimator`, `sombra`).
- **Two research agents shared one scripts folder** and overwrote seven of each other's copies (the same code, paths
  differing): give parallel research passes separate folders.

Played (step 1, dev client, creative): picked from the select screen (her model, pistol, kit row, 225 health); in play,
first person (the pistol in her right hand, the HUD with 60 rounds and her two abilities) and third person front and back
(the pistol by her shoulder), running, jumping, crouching; Hello from the communication wheel (her left hand waves in first
person; the third-person wave).

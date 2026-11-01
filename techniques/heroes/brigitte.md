# Brigitte (hub)

Everything learned about Brigitte, by subject. The eighth hero (issue #65), fourth on the owner's 2026-10-28 list: a Support,
Survivor (BZ 2026-02-10). Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · built the way Sombra was: [sombra](sombra.md)

## Her notes and sources
- **Numbers:** `tools/reference/brigitte.md` (Blizzard's patch notes to 2026-11 and the wiki's revision 408525, each number
  with its history). No patch has touched her since the data build (BZ 2026-09-08 was the last), so the data's numbers
  are the live ones.
- **The data map:** `tools/reference/brigitte-map.md` (her kit from her graphs, node by node, H / M / L); the detail in
  `E:\Overwatch Archive\research\brigitte\` (`work_kit\`, `work\assets.md`, `work\clipmap.md`, `extract\`, build 154088).
- **The plan:** `tools/reference/brigitte-plan.md` (what maps onto the mod: armor and a barrier are new; the order).
- **Clip maps:** `tools/clipmaps/brigitte.txt` (3P), `brigitte_1p.txt`, `brigitte_flail.txt` (the handle, exported as
  `hero_models/brigitte_weapon.glb`), `brigitte_flail_head.txt`, `brigitte_shield.txt`.

## What step 1 taught
- **The `--entity` import crashed headless Blender** for every one of her models (the add-on raised an error popup,
  `popup_menu_end` with no window: an access violation). Without it every model exported; her first-person arms got
  their materials anyway (no "no colour texture" line). Run one export alone first when a hero's exports all fail.
- **Three models besides her body:** the flail's head sits on the handle's socket 0254 (bone_0FCD), and her shield
  folded on her back (01D8, bone_0F74). `HeroWeapon.extras` now returns every extra model (Ana's sidearm was one).
- **Her view model has no walk clip:** idle, jump, fall and land, with movement additives; Ana's first-person rule
  fits (the idle while she moves).
- **No showcase clips extracted** (C06C / C07D / C0B8): the select screen poses her in her idle, and the weapon and
  extras are drawn from that pose as her showcase entity has them (the flail on 000A, the shield folded on 01D8).

Played (step 1, dev client, fresh creative worlds `brig1`-`brig3`): picked from the select screen (her model, "Support,
Survivor, 250 health", her seven kit icons; after the fix the flail in her hand and the shield on her back); in play, first
person (the flail in her right hand, Repair Pack's 3 charges, Whip Shot and Shield Bash icons), third person front and
back (the flail and its head, the shield folded on her back), running, jumping, crouching. Lucio's and Jetpack Cat's
select screens unchanged. Screenshots b_*.png, sel_*.png, not committed.

## What step 2 (armor) taught
- **Armor is a layer like shields** (`Hero.layer()`, kept in `HeroState.shield`): hits come off it first, heals fill the
  health under it first; it never regenerates; a hit while any armor is left is 7 smaller, at most halved
  (`Shields.armorReduce`, in the damage mixin after the boosts). Ours: the whole hit is reduced where Overwatch reduces only
  the part landing on armor (M); beams and damage over time aren't told apart yet.
- **A fresh flat survival world has mobs from generation** (205 killed, slimes among them): `spawn_mobs false` stops new
  ones only. A slime's unseen hits looked like a bug in the armor until `kill @e[type=!player]` ran first.
- **Adding a hero shifts the select screen's portrait row** (9 portraits now: Kiriko 142 ... Brigitte 266 in GUI
  coordinates at 854x480): the harness clicks for each hero moved.

Played (step 2, dev client, survival, flat world `armor1`, natural regeneration off, every mob killed first; health polled
every 50 ms): a zombie's 37.5 hits landed as 30.5, 30.5, 30.5 (armor 75 -> 44.5 -> 14 -> 0), then 37.5, 37.5; a
skeleton's arrows 62.5 and 50 as 55.5 and 43; `/damage 12` (150) as 143. HUD: seven white and three yellow pips at full;
after a heal to 158, the health pips filled and the armor's stayed empty; full again, yellow back. The select screen says
"175 health, 75 armor". Lifeweaver after the change: a hit of 37.5 in full, his shields back from 3 s at 30 a second.

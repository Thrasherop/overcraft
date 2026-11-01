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

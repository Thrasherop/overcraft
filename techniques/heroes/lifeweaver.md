# Lifeweaver (hub)

Everything learned about Lifeweaver, by subject. The fifth hero (issue #39), the first of the owner's 2026-10-28 list,
and the first with shields. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · read the way Mercy's kit was: [mercy](mercy.md)

## His notes and sources
- **The data map:** `E:\Overwatch Archive\research\lifeweaver\lifeweaver-map.md` (the 2026-10-29 research pass: his
  graphs decompiled node by node, each finding H / M / L), its detail in `work_kit\` (weapons, platform and dash, grip
  and tree) and `work\clipmap.md` / `clipmap.json`; his extraction in `extract\` (build 154088).
- **Numbers:** `tools/reference/lifeweaver.md` (the public sources: patch notes to 2026-10-20, the wiki's revision
  406788), `tools/reference/lifeweaver-plan.md` (the port's plan and its open items).

## Facts that come up often
- **One model, two looks:** `00BB.118`, third person look `0472.119`, first person `07BC.119` (Jetpack Cat's
  arrangement). No weapon entity: Healing Blossom's flower is geoset 2 of his body, Thorn Volley his gauntlet. Which
  weapon is out is the clips' category (df 0xE4: 7F the Blossom, 7D Thorn Volley).
- **Muzzles** from the look's own hardpoint table: 000A (right hand) for both weapons, 0002 (left wrist) for the
  platform and the seed.
- **His props are models of their own** with their own clips: Petal Platform `12D32`, the tree `1267E`, the seed and
  thrown platform `126CC`.
- **Health 200 + 50 shields:** the first hero with shields (they regenerate after 3 s at 30/s).
- **Where the data and the wiki differ** (the data wins): the platform's throw (50 m/s), the dash (0.25 s at
  28.875 m/s), the Blossom's passive charge (5.175 s), Life Grip's far pull, the seed's 3 m pickup. A thorn-to-blossom
  bonus in his graphs is in no public source: left out until the owner says.

## Open
The plan's "Open" list: the platform's collision and its riders, the tree's canopy, his fall-damage height, and what
ends the platform when he dies.

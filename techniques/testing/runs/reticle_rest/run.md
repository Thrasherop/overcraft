# #85 rest: the world widgets (fix/reticle-rest, from next f842610)

Up: [reticle](../../../hud/reticle.md) · [techniques map](../../../README.md)

## Audit of every playable hero against #85's list, on f842610
Already drawn by earlier rounds (ReticleHud, read and checked against the issue's list): Sojourn, Sigma (barrier health at the
reticle while out, on its panel otherwise with the broken / recall seconds; Flux bar and CONFIRM), Brigitte, Mercy's reticle
panels, Sombra, Juno (bars, prompts; her 0727 lock markers are JunoFx's), Jetpack Cat, Lifeweaver, Lúcio, Ana, D.Va, Pharah, Tracer,
Cassidy, Junkrat. Kiriko: her "+", ofuda counter and Swift Step target were there; the data shows no other widget for her (30D8
not decompiled). Illari isn't playable. Left: the world widgets.

## Drawn this round (each from its StateUXPresenter node; looks ours)
- **Brigitte, 011F over each ally her packs heal** (0EE1 #611: for the caster alone, icon loadout 01A4, hardpoint 0E + 0.4 m, 9A
  = v22729 / v69439). The pack's icon on a dark disc, the ally-cyan ring draining with the heal left (2 s, +2 s per extra pack).
  Server: `RepairPack.healing`, two allies newest first, through `Widgets.target / target2 / share / share2`.
- **Sombra, 011F over each Hotfixed ally** (60FB #371: for the caster, icon F5E, no share bound): Hotfix's icon, full ring, on
  Widgets.target / target2 (her 0D81 slots).
- **Mercy, 08F3 on the soul Resurrect would take** (00CA #1274: for her, at v78337[0] + 1 m, holding 0693's portrait, name, team
  colour, button): the portrait in the cyan ring, the Resurrect key under it. `Widgets.at` carries the soul's place (no entity on
  the client).
- `HeroSyncPayload.Widgets` gained `share`, `share2`, `at` (a 10-argument constructor keeps every kit's call unchanged).

## Played (one dev client, harness keys through dev.py, flat survival worlds rt85d / rt85e / rt85f, OC_ASSET_PACK)
Screenshots in `overcraft_reticle3/run/screenshots`, read full and as enlarged crops.
| Step | Expected | Seen |
|---|---|---|
| Brigitte, E at a NoAI villager, day (`rr_brig_before`, `rr_brig_repair1`, `_repair2`, `_after`) | icon over the villager, ring draining, gone after 2 s | as expected; ring at 2.5 thick read as a hairline at 854 wide, so 4 thick |
| Same, night, two villagers, three packs (`rr2_brig_two`, `_extend`, `_one`, `_none`) | two icons with different shares; an extended one fuller; none after | two icons, the newer fuller; after the third pack one left, fuller; none at the end |
| Sombra, Hotfix (RMB) at the villager (`rr_sombra_preview`, `_hotfix1`, `_hotfix2`, `_after`) | Hotfix icon over the healed villager | icon over it, follows it (it moved off screen to the left: worldWidgets x -21) |
| Mercy, villager killed by /kill (`rr_mercy_vill`, `rr_mercy_soul`, `_soul_side`, `_soul_behind`) | portrait marker 1 m over the soul, E under it; none behind | villager face in a cyan ring with E, and the reticle's RESURRECT VILLAGER; side view follows; behind: none |

Found playing (not bugs of this change): the superflat's slimes killed the player and the villagers between steps of the first
world (`rr_probe`: summoned mobs "vanished"); later worlds used /difficulty easy, killed slimes and resistance. `/damage ... 100`
didn't kill a villager (the mod's ally rules), /kill did. A wild wolf isn't an ally (no pack goes to it).

**Not tried:** the physical mouse (harness keys only); a second player (a player's soul and their hero face on the marker; another
Brigitte's packs, which mustn't show: the data's v40 == local, ours by sync, so reasoned); third person; GUI scales; water, Nether;
Combat Medic. No Overwatch side by side (no frames of these widgets).

## Still missing, and why
- Jetpack Cat's 0C3D / 011F over the Cat for her allies while she seeks: on other players' HUDs only, needs a second client.
- Sombra's 0D86 band on a Hotfixed ally's health bar: the mod draws no allies' health bars.
- Sigma's barrier as an ability box with the broken icon (0241, texture 1FC8A, not in the pack): an ability-bar widget, not the
  reticle; his panel already shows the broken seconds.
- Kiriko: nothing more in the data to draw (30D8 undecompiled).

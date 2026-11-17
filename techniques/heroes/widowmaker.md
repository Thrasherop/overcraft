# Widowmaker (hub)

Everything learned about Widowmaker, by subject. Issue #138: **Damage, Sharpshooter** (D tag 1188), the last hero, built after
[Ashe](ashe.md) the way she was. Up: [Techniques](../README.md) · the method: [reading_game_data](../game-data/reading_game_data.md) · no
Workshop recording of her: her movement is the data's default mover.

## Her notes and sources
- **Public numbers:** [widowmaker.md](../../tools/reference/widowmaker.md): BZ 2025-09 to 2026-11 (her last number change BZ 2026-04-14,
  perk costs; nothing for her in 2026-11), W rev 409087.
- **The data map:** [widowmaker-map.md](../../tools/reference/widowmaker-map.md): hero 000A, entity 001C; graphs 001B (Kiss, Take Aim) /
  005A (hook) / 0056 + 0057 + 0073 (mine, poison) / 59BF + 59C0 (Seeker Mine) / 0067 (Infra-Sight) / 51EF (perks) / 4BE6 (values). Node by
  node: `E:\Overwatch Archive\research\widowmaker\work_kit\kit_widowmaker.md`.
- **Assets:** [widowmaker-assets.md](../../tools/reference/widowmaker-assets.md) (models, clip maps `tools/clipmaps/widowmaker*.txt`, sound
  maps `tools/soundmaps/widowmaker*.txt`, 19 emotes `emotes/widowmaker.json`); **effects:** [widowmaker-effects.md](../../tools/reference/widowmaker-effects.md).
- **Code:** `dev.overcraft.widowmaker` (`Widowmaker` the kit, `WidowmakerStats` every number with its source, `WidowmakerState`,
  `WidowmakerSounds`), `entity.GrappleHookEntity`, `entity.VenomMineEntity`, `client.effect.WidowmakerFx` / `WidowmakerPalettes` /
  `WidowReveal` (outlines through walls by the ping outline mixins), her scope in `ClientAna` (30 degrees) and Ashe's scoped path in
  `HeroAnimator` (`HeroManager.WIDOWMAKER_LOOK`), `WidowmakerMathTest`.

## Where the data and the public sheet disagreed (WidowmakerStats says which won)
- Reload ammo at 0.825 s (D) vs 0.816 (W); scope 0.333 s (D) vs 0.32 (W); mine arms 1 s after it sticks (D) vs W's 1.4 s (likely from the
  press); the mine leaves the hand 0.2 s in (D) vs W's "0 s cast"; Widow's Bite pierces above 100 % (D) vs W's "at 125 %": the data's kept
  each time. The hip falloff floor isn't readable in the data (shared stats graph): W's 30 % kept (L).
- 51EF #325 (a scoped hit refunding up to 4 s of the hook, no perk gate, in no public source): not built (L, maybe not live).
- [ashe-map.md](../../tools/reference/ashe-map.md)'s "scoped move x0.6" is likely the zoomed aim sensitivity (the same node type as Widow's
  #115, which her hero settings name): Widow's real slow is a +0.65 on the self-slow variable (1E5A). Ashe's not re-read.

## Ours (no source gives it)
- Scoped fire on the press (L against holding); the pull's end keeps 0.35 of its speed (the data's 0.2 s end push, M); a 1.5 s cap on a
  pull; the ledge test (a standable top with two blocks of room within 3 m above the hook's point); Seeker Mine darts as timed hits drawn
  by her clients; a health potion (Minecraft's health pack) clears the poison (W); Infra-Sight ends on any way out of play (the data: on
  death); the charge's percent past 100 with Widow's Bite drawn 100 -> 125 over 10 -> 12.5.
- Reveals are Minecraft's outline in red for her (mine) or her side (Infra-Sight); Overwatch also shows their health bars: not built.
- Her scope's lens is Ana's (not read from her data); the select screen has no pose of hers (her select clips C0C5 / C188 weren't extracted).

## Played
**Not yet (2026-11-17).** `gradlew build` passes with `WidowmakerMathTest`. The client lock was held by another client (an Ashe world,
paused, from the main checkout) the whole session, so neither the game nor the model exports (`research\widowmaker\ascripts\export_models.sh`,
which rightly refuses while the lock exists) could run: **her models aren't in the pack yet** (sounds, icons and effect masks are). Owed:
the exports, `build_asset_pack.py add` for each glb, `props` and `glows`, then the whole hand play (every ability against mobs, numbers
read, the select screen, G, death, switch, rejoin) and the real mouse and keys.

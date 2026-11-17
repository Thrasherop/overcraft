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
- Scoped fire on the press or held (held fires at each recovery: Overwatch weapons fire while held; it was press-only and a held
  button fired once in 2 s); a jump out of the pull keeps the same 0.35 of its speed plus one jump (was 0.8 of it and 0.3 up: ~13 m
  of rise on a climb, now ~4.7 m); a stuck mine or a held hook whose block goes falls / lets go (the pull ends with its cooldown);
  the pull's end keeps 0.35 of its speed (the data's 0.2 s end push, M); a 1.5 s cap on a
  pull; the ledge test (a standable top with two blocks of room within 3 m above the hook's point); Seeker Mine darts as timed hits drawn
  by her clients; a health potion (Minecraft's health pack) clears the poison (W); Infra-Sight ends on any way out of play (the data: on
  death); the charge's percent past 100 with Widow's Bite drawn 100 -> 125 over 10 -> 12.5.
- Reveals are Minecraft's outline in red for her (mine) or her side (Infra-Sight); Overwatch also shows their health bars: not built.
- Her scope's lens is Ana's (not read from her data); the select screen has no pose of hers (her select clips C0C5 / C188 weren't extracted).

## Played
**2026-11-17, real mouse and keys** (world `widow1`, flat stone arena; damage read from husks' `Health`, a husk's 2 armor taken off):
- Picked from the select screen on join, after a rejoin, and from H at her spawn point (Ashe and back: fresh kit). Respawned after two
  deaths (a spider in the dark arena; a zombie rush at night) straight back as her. HUD: 225 health (18), 35 rounds, five buttons.
- Hip fire: 14 a shot (1.0555 off a husk), 11 of 11 hit point blank. Scoped, full charge: 120 body (9.446), 5 rounds; a headshot killed
  a husk in one. Melee 40. Reload R back to 35. Venom Mine: sticks to floor and wall, arms, a husk 2 m away took 125 over 5 s (9.28).
  Grappling Hook: to a wall's top and over the ledge, from water to a wall, past a bar at head height (scrapes ~0.5 s, then slides over),
  jump cancel. Infra-Sight at night: two zombies and a skeleton behind a dirt wall outlined in red; the charge was spent; it ended at 15 s.
- Reproduced on the unfixed build, then replayed fixed (critic's list): G twice in the mine's throw gave a free mine at once (now its
  15 s); a scoped button held 2 s fired once (now 5 times); the hook kept pulling into the air after its wall was removed (now lets go,
  cooldown from there); the hook cooldown stood at 10.0 for 0.5 s after a pull (now runs at once); E and Q worked in a boat (now not).
  The stuck mine with its block removed: my first test removed the wrong block (the mine's position is the bottom of its box: the block
  is the one at its middle); the fix then showed the mine vanished as it fell, because OwProjectile expires anything older than its
  flight life (10 s) and a stuck mine is. Now its stuck time doesn't count: after 12 s on a wall it fell, stuck to the floor and burst.
- Perks (picked from the perk menu: Alt held, a click): Scoped Efficiency, a scoped shot 3 rounds (35 -> 32) at full 120; Seeker Mine,
  a husk 6.6 m from the mine (outside the plain mine's 3 m) took 75 over 5 s (5.55) and the mine stayed. The critic read W's prose as
  "trigger at 3 m, then darts within 10 m"; the data's 59BF has a 10 m radius and no burst, so the data's reading stands.
- Not tried: a second player (her Infra-Sight shown to an ally, the pull with ping), the Nether, Sniper's Instinct and Widow's Bite,
  Toggle Zoom (not built). Not reproduced: one real-key run lost a mine after G twice; two
  repeats didn't, and that window had a spider death in it.

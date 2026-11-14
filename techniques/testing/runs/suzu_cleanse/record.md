# Protection Suzu: Flashbang's hinder and Steel Trap's root (2026-11-13, fix/suzu-cleanse)

Up: [Techniques](../../../README.md) · the data: [status_and_cleanse](../../../game-data/status_and_cleanse.md) · before: [polish4](../../polish4.md) · [heroes/kiriko](../../../heroes/kiriko.md) · issues #119, #117, #121

## What was found
#119 (sleep, stuns, hack, ...), #117 (anti-heal) and #121 (her line only after a cleanse) were fixed and played in polish-4, merged into `next` (48cd86c) but not yet on `main`, so they stayed open. Since then Cassidy and Junkrat came in, and **their debuffs had no row in `Combat.cleanse`**:
- **Flashbang's hinder** (status 0079.053, graph 0228): the data cleanses it and it tells (#155 1BC5 -> #154 1C7D -> #125); W Status effect rev 380062 "Can be cleansed". The mod left it on: **the bug.**
- **Steel Trap's root** (status 01F5.053, graph 09BC): no v8440, 1BC5 or 1C7D in its graph; W Junkrat rev 380944: the phase "will prevent a trap from triggering, but will not free a player that is already trapped". The mod already did that (Allies.areaHits): no change.

Fix: `Hinder.cleanse` (ends it and its slow, returns true so her line plays), called from `Combat.cleanse`; two rows in the table. Dev: `bot cc` now reads `rooted`.

## The play (build: this branch uncommitted on 009950b; one client, fresh flat worlds `suzu1113`, `suzu1113b`)
Real client: Kiriko on team red, Suzu by the real key binding (`key ability_2`, KeyMapping, not a person). Bots (server-side players): a Mercy ally on red, a Cassidy, Junkrat or Ana on blue. "Off" = the same build with `bot fix 119 off` (the whole status list switched off, as polish-4 did). Raw: [suzu_play_1](suzu_play_1.txt), [suzu_play_2](suzu_play_2.txt), [suzu_probe_trap3](suzu_probe_trap3.txt); scripts in [scripts](scripts/). Screenshots `run/screenshots/suzu_*.png` (gitignored).

| Case | Off (before) | On (after) |
|---|---|---|
| Flashbang on the ally, then Suzu | hindered through the phase, speed 0.05, until its own 1.2 s ran out (+0.75) | hindered=False at the first read (+0.10), speed 0.1; her line FC5 heard (`kiriko/voice/suzu/11`) |
| Flashbang on Kiriko, Suzu at her feet | hindered until +1.04 | free at +0.10; **no line**: she was saying her pain line from the flash, and Voice plays a non-interrupting line only when she's free. With the hinder put on by `cut hinder` (no damage), the line plays (`suzu/5`) |
| Steel Trap holding the ally, then Suzu | rooted, speed 0 | rooted, speed 0 (as the data and W); no line |
| Flash landing inside the phase (flash, Suzu 0.05 s / 0.15 s after) | - | never hindered, no damage |
| Ally walks onto an armed trap while phased | - | not caught while phased (2 reads); **caught as the phase ended** (+0.39, 100 damage): the trap stays armed, as W says |
| Control: onto the trap, no Suzu | - | caught at once, 18 -> 10 |
| Sleep Dart (#119), Biotic Grenade (#117): regression | - | woke at +0.37; anti-heal gone at +0.10, healed 6.2 -> 12.6; her line both times |

Voided along the way (kept in the raw files, not evidence): suzu_play_1's "Flashbang into the phase" (Suzu thrown before the flash: the flash landed after the 0.5 s phase), its "Steel Trap stepped on while phased" and suzu_play_2's trap case (the trap lands at Junkrat's feet, z 7.85, so the ally was caught at spawn, or never stood on it; found with `ents SteelTrap`). The last trap3 case's Suzu missed the ally (aimed too steep): suzu_play_1's trap rows stand for that case.

## Seen, not this fix
- A server crash mid-run: `ReplayPlayerRecorder.takeSnapshot` "Tried to get player before player joined" from PotgRecorder.tick, as a bot was added: already #140.
- Not tried: a real second player, Life Grip's cleanse of the hinder (same `Combat.cleanse`, so the same row), mobs, water, the Nether.
- Phased "removes sticky projectiles" (W Status effect): Pulse Bomb stuck to an ally isn't removed by Suzu here. Its damage is refused during the phase. Not looked into in the data.

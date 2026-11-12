# POTG scoring values: in-game check (2026-11-11/12, Refs #44)

Up: [design note, "Progression events"](../design.md) · map: [techniques/README.md](../../README.md)

The owner's progression values (2026-11-11, all in `PotgValues`) played in a dev client (`runClient`, `-Xmx3G`),
Overwatch and Battle.net checked closed first. Driven through the dev harness (127.0.0.1:25599) with `drive.py` here:
real key mappings (kunai `secondary_fire`, `quick_melee` V, `interact` F, `use`/`attack` mouse in Minecraft mode, G),
commands only to set the scene (summons, frames, `tp` onto a portal). Full transcript: `driver.txt`. Screenshots are
in the worktree's `run/screenshots/` (not committed): `sc_1`, `sc_2`, `sc_eye1..3`, `sc_portal`, `sc_nether_portal*`,
`sc_nether*`, `sc_end*`, `sc_mcmode`, `sc_rejoin`.

Fresh survival superflat world `potg_scoring2`, Kiriko picked on join, hero mode on unless said.

| # | Step | Expected | Seen (raw log line) |
|---|------|----------|---------------------|
| 1 | Blaze (NoAI) killed with kunai | 250 health + 750 = 1000 | `[23:59:12] [POTG] Kiriko: +1000 High Score (kill minecraft:blaze)` |
| 2 | Enderman (AI on, 6 health) killed by 3 quick melees | 500 + 500 = 1000 | `[00:00:35] [POTG] Kiriko: +1000 High Score (kill minecraft:enderman)` |
| 3 | Wither skeleton killed with kunai | 250 + 200 = 450 (~700 with its damage) | `[00:00:49] [POTG] Kiriko: +450 High Score (kill minecraft:wither_skeleton)`; its highlight `High Score 700` |
| 4 | Zombie killed with kunai | 250, no bonus (~500 with damage) | `[00:00:58] [POTG] Kiriko: +250 High Score (kill minecraft:zombie)` |
| 5 | Walk over 2 blaze rods, 3 pearls, 1 eye of ender | 500, 450, nothing | `[00:01:17] ... +500 High Score (pickup 2 minecraft:blaze_rod)`, `... +450 High Score (pickup 3 minecraft:ender_pearl)`; no eye line |
| 6 | One end crystal shot with kunai | 400 | `[00:01:25] [POTG] Kiriko: +400 High Score (end crystal)` |
| 7 | Three crystals side by side, one shot (the blast chains) | 3 x 400 | three `[00:01:30] ... +400 High Score (end crystal)` |
| 8 | A crystal `/kill`ed | nothing (no one) | no line |
| 9 | Frame of 12, 10 eyes in: one eye placed with F | 100 | `[00:02:03] [POTG] Kiriko: +100 High Score (eye of ender placed)` |
| 10 | The last eye with F: the portal opens | 200 (not 300) | `[00:02:09] [POTG] Kiriko: +200 High Score (eye of ender, portal opened)`; `/execute if block 20 -60 20 end_portal` true |
| 11 | Obsidian frame lit with flint and steel (F), stood in it | Nether first entry 500 | `[00:03:29] [POTG] Kiriko: +500 High Score (first entry minecraft:the_nether)` |
| 12 | Back to the overworld, into the same portal again | nothing | no line (arrived at 0.5 37 -11.1 in the Nether) |
| 13 | Into the opened End portal | End first entry 1000 | `[00:04:09] [POTG] Kiriko: +1000 High Score (first entry minecraft:the_end)` |
| 14 | `players.json` | `entered` has both | `"entered": ["minecraft:the_nether", "minecraft:the_end"]` |
| 15 | G (Minecraft mode): zombie and blaze killed with a sword, a crystal punched, 2 rods and a pearl picked up, an eye placed with right click | nothing | no `[POTG]` line between 00:04:31 and 00:05:25 (each kill and the crystal checked gone with `/execute if entity`, the eye with `/execute if block`) |
| 16 | Save and Quit, quit the game, rebuild, relaunch, `openworld potg_scoring2`, Kiriko, hero mode | | |
| 17 | Into the Nether portal, then the End portal | nothing (saved once) | no line; arrived in both (0.5 37 -11.1; 100.5 49 0.5) |
| 18 | Calm enderman (NoAI, 3 health) killed by one quick melee | 1000 (after the fix below) | `[00:09:04] [POTG] Kiriko: +1000 High Score (kill minecraft:enderman)`, then `+150 ... (pickup 1 minecraft:ender_pearl)` |
| 19 | Calm cow (1 health) killed by a quick melee | nothing (bystander) | no line, cow gone |

## Found and fixed
- **A calm enderman killed in one blow scored 0** (first session, 23:59:57: the kill made no line, only its pearl's
  `+150`). It was a bystander (`Allies.isBystander`: neutral, not yet angry), and bystander kills score nothing. In a
  run endermen are hit while calm for their pearls, so `PlayScore.kill` now scores a bystander when its type has a
  `KILL_BONUS` (only the enderman is ever calm among them); other bystanders (a cow, step 19) still score nothing.
  Before: no line; after: step 18.

## Seen, not changed
- Dropped items re-scored: after a death the player's own blaze rods, dropped and picked back up, scored again
  (`[00:02:56] +500 (pickup 2 minecraft:blaze_rod)`). Per the owner's "no cap", left as is; the owner may want a
  first-pickup-only rule.
- A crystal's blast killed the Minecraft-mode player in survival (vanilla: 6 power at arm's length).

## Not tried
- The real dragon fight's crystals on their pillars (shot from below, arrows from a bow); the elder guardian and the
  Wither (values unit-tested only: `PotgValuesTest`); a second player; a projectile from a non-Kiriko hero.

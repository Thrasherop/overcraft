# Overwatch's perks: how they work (research, 2026-10-26)

For Overcraft's perk system (#36). Raw sources were saved in the session's scratchpad (`perks\`); the quotes below
carry their URLs. **BZ** = Blizzard patch notes, **W** = the wiki (fandom's MediaWiki API; overwatch.weirdgloop.org is
a copy with the same text; wiki.gg answers 401).

## Progression
- BZ 2025-02-18 (https://overwatch.blizzard.com/en-us/news/patch-notes/live/2025/02/): "Perk progress is gained by
  dealing damage, securing eliminations, and healing your allies." "Throughout the match, the rate at which you gain
  Perks will gradually increase." An elimination of an enemy "at a higher perk level than you" is worth more.
- W (Perks page): "each point of damage and healing granting a point toward the next perk level"; "players do not
  passively gain perk progress"; "After 3 minutes, the amount of XP gained starts to increase until 7 minutes, where it
  reaches the maximum 2x increase"; +15% for eliminating a hero at a higher perk level. The ult charge's limits apply
  (nothing from or during ultimates, nothing from self-damage, less from overhealth and the tank passive).
- Levels: 1 at the start, a minor perk at 2, a major perk at 3, two options each (Blizzard's hero pages: "Minor Perk
  (Level 2)", "Major Perk (Level 3)").
- Kiriko's thresholds (W infobox): minor 1900, major 4800 (2090 / 5225 until a wiki edit of 2026-04-21, source
  unknown). Blizzard only publishes changes in percent. Lúcio's (W): 2020 / 5050.
- Swaps (BZ 2025-02-18): "your new hero will be set to level 1 with zero perk progress"; progress is "saved on any
  heroes that you play throughout the round"; switching back restores the level "with your perks equipped".
- Competitive Escort/Hybrid: reset each round (W). Late joiners get the least-progressed teammate's XP (W).

## HUD (third parties only; Blizzard describes none of it)
- PCGamesN: "below your hero's health bar, you'll now find a level bar."
- TheGamer: "a blue bar slowly filling"; when a perk is ready "an orange pop-up will appear in that corner".
- Deltia's: the blue meter "becomes full and glows orange". Aftermath: level-ups come with "a subtle chime and
  animation"; perks show "as you pick it or as it appears on the in-match scoreboard".

## Selection
- BZ 2025-02-18: "Hold down Left Alt on keyboard ..., then aim at the left or right perk ... to select." Alec Dawson
  (Xbox Wire, 2025-02-12): "Hold down Alt ..., then use Left/Right click (or bumpers) to select."
- Optional quick-select bindings and a toggle instead of hold exist (BZ); no defaults given.
- Real time; a pick can wait (bug fixes mention picking while dead and in overtime); a pick is for good (Director's
  Take AMA: "no plans to add changing perks mid game").
- Blizzard's hero page marks Urgent Care and Ready Step "left", Fortune Teller and Foxtrot "right".

## Kiriko's perks (Blizzard's hero page, 2026-10-26)
| Tier | Perk | Text | History |
|---|---|---|---|
| Minor | Urgent Care | "Healing Ofuda projectile speed is increased by 50% when seeking allies below half health." | unchanged |
| Minor | Fortune Teller | "Kunai hits launch 2 Healing Ofuda to an ally in front of you." | unchanged |
| Major | Ready Step | "Swift Step grants Kiriko 40% increased attack and reload speed for 3 seconds." | 2 s (2025-08-26), 3 s (2025-10-14) |
| Major | Foxtrot | "Protection Suzu grants allies 30% increased movement speed for 4 seconds." | 4 s, cooldown buff removed (2026-02-10) |

W notes: Fortune Teller "Does not trigger when hitting barriers or other deployables, except B.O.B."

## Not given by any source (Overcraft's own choices)
- Whether 4800 counts from the start or after 1900 (ours: from the start). What an elimination is worth on its own
  (ours: only its damage counts). The ramp's shape (ours: straight from 3 to 7 minutes of play as the hero).
- Death (ours: progress kept; no source names a reset). Every HUD detail (ours: the line under the name fills blue,
  goes orange when a perk waits, the box at its start shows the level; picked perks as white discs above the weapon
  panels, as seen in the reference video). The chime (ours). Fortune Teller's cone (ours: Swift Step's).

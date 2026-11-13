# Junkrat: the coordinator's real-input pass on 24d22a1 (2026-11-13)

Up: [Junkrat hub](../../../heroes/junkrat.md) · [map](../../../README.md). Before it: the builder's play (`runs.txt`) and
the critic's fixes ([fix_1.md](fix_1.md)).

The real mouse and keys via computer-use (not the harness's key mappings). The harness was used only for set-up
commands and reading state (`tools/dev.py`, a 40 ms state logger). Fresh world `jr_real`, survival, Normal, a stone pad,
`spawn_monsters false`. Targets were NoAI husks with 100 MC health.

| Step | Expected | Seen |
|---|---|---|
| Junkrat clicked on the select screen, then Ready | Junkrat, Damage Specialist, his HUD | As expected (frag ammo, MINE LSHIFT, TRAP E, ult %) |
| One left click at a husk 8 m ahead | 125 OW direct, less its 2 armor; no self-damage | Husk 100 → 90.16; Junkrat 20 → 20; ammo 5 → 4 |
| E (Steel Trap) thrown ahead; an AI **baby** husk walks in (the critic's #2) | Held for 2.5 s, 100 OW | Held at z 2.61 for two to three 1 s polls, then walked on; 100 → 92.13 |
| Shift at his feet (looking down), right click 0.5 s later: mine jump | Launched, no fall damage | Rose 6.4 m and went about 14 m sideways in 1.7 s; landed with 20 → 20 |
| `/overcraft ult`, Q, W held 1.2 s, then left click at a husk | Tire drives, about 600 OW up close, body stays put, ult recharges | Husk 100 → 52.77 (≈590 OW); body stayed at z -1.5; ult 0 → 43.75 by the next read |
| `/tp` 20 m up, Q 0.1 s into the fall (the critic's #1) | His body falls, and the fall counts like a plain 20 m fall | His body fell at once and landed at 1.5 s: 20 → 3 (the same as a plain 20 m fall). Ult spent |
| Q on the ground, G 1 s later, W, G again | Tire gone without a blast, first-person camera, plain Minecraft HUD; back to Junkrat | As expected; walked 2.35 m in Minecraft mode; back as Junkrat with 5 frags, ult recharging |
| Esc → Save and Leave → Quit Game | Clean exit | As expected |

Incidents (not Junkrat): the flat world had slimes from before the gamerule, and one killed him. After the respawn
he was still Junkrat with full ammo and mines. Later, the AI baby husk from the trap step killed him once I stopped
watching it. 17 leftover mobs were cleared with `/kill @e[type=!player]`.

Not tried here: perks, Total Mayhem by hand, slopes, water, the Nether, a second player, the dragon. The first-person
launcher showed as a bare arm, because this worktree's dev client had no local asset pack; the builder checked the
launcher with the pack.

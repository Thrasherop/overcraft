# Life Grip frees tethers and gravity (#124); Rally's armor on Brigitte (#125) (2026-11-13, fix/grip-rally)

Up: [Techniques](../../../README.md) · the data and the list: [status_and_cleanse](../../../game-data/status_and_cleanse.md) · [heroes/lifeweaver](../../../heroes/lifeweaver.md) · [heroes/brigitte](../../../heroes/brigitte.md)

## Sources
- #124, the owner: "yes life grip should pull people out of tethers and gravity effects". W Status effect rev 380062: Life Grip "Removes knockback, tether, hinder and gravity effects". W Lifeweaver rev 380626: a gripped Jetpack Cat's tether breaks; out of Gravitic Flux. W Junkrat rev 380944: "Lifeweaver can pull the target free" (Steel Trap). W Sigma rev 381123: Hyper Strike's targets "ignore gravity". The data (21EE #605 / #606) has no reader in these statuses: W followed.
- #125, the owner: "yes she gets more health. Armor iirc." BZ 2023-04-11: "Brigitte now gains 100 restorable armor health pool for the duration of Rally" and "Brigitte no longer gradually builds temporary health like her nearby allies". No Rally change in BZ since (read on the wiki's changelog and BZ's own pages for 2026-08 to 2026-11). W Brigitte rev 381389: "armor = 100 (self)", "Brigitte herself does not gain overhealth over time". The owner and the sources agree: armor, 100, which the mod already gave. Found in play: what's left of it was taken off wrong when Rally ended.

## The play
One client from this worktree (04147b9 plus this change, uncommitted), fresh flat worlds `grip1113`, `grip1113b`. The real client is the ally (Mercy, blue) or Brigitte; bots (server-side players) for the rest; Q by the harness key (KeyMapping). Before / after in one build: `bot fix 124 off|on` (LifeGrip.devFreeOff), `bot fixoff on|off` for Rally's end. Raw: [grip_play_124](grip_play_124.txt), [rally_play_125](rally_play_125.txt); script [scripts/grip124.py](scripts/grip124.py). Screenshots `run/screenshots/g124_*.png`, `g125_*.png` (gitignored). A bot Lifeweaver 8 m off presses Life Grip; reads every ~50 ms for 1.8 s.

| Case | Before (fix off) | After (fix on) |
|---|---|---|
| Catnapper carrying the ally | `carriedBy` on for the whole 1.8 s; pulled 0.78 m of 8 | carry ended at once; pulled to him (6.75 m); still knocked down (as meant) |
| Gravitic Flux lifting the ally | `lifted` on 35 ticks while pulled; then slammed: 14 -> 9 health | not lifted from the first read; no slam: 14 -> 18 (the grip's 75) |
| Steel Trap rooting the ally | `rooted` on 36 ticks, speed 0, pulled anyway; W after: 0.08 m | root gone at once, speed 0.1; W after: 2.9 m |
| Hyper Strike's float | held 11 ticks, gravity 0.0008 | free at once, gravity 0.08 |
| Accretion's knock-down (control) | knocked 17 ticks, pulled 6.75 m | the same: knock-downs stay |
| Rally on Brigitte (real Q) | 350/350, armor 175, her AbsorptionAmount 0, the ally's 3.6 at 1.6 s and 8 (100) at 4.6 s; 75 damage: 22.56/28, armor 107; **after Rally 250/250 with armor 43**, so health 207 over a 175 top | the same during Rally; **after: 250/250, armor 75** (the bonus, on top, took the hit; what's left of it goes) |

The Rally end: `endRally` clamped her health through `Shields.onSetHealth`, which counted the clamp as a hit and took it off her own armor too. Now the bonus left is taken off her health and armor together past the split (`Shields.setRaw`).

## Not tried
A real second player as the ally; a mob ally (villager) in a carry or a lift; a friendly cat's Lifeline tow under the grip, and gripping a cat who carries (code paths in `JetpackCat.untether`, not played); water, the Nether. Which of Rally's armor or her own goes first in Overwatch isn't sourced: ours spends the bonus first.

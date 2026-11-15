# Ashe in live Overwatch: numbers and sources (research, 2026-11-14)

**BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/2026/10/` and `/11/`, read 2026-11-14
by plain HTTP, parsed to text; older history from the wiki's log). **W** = the Overwatch wiki on Weird Gloop through its MediaWiki API
(`overwatch.weirdgloop.org/api.php?action=parse&page=Ashe&prop=wikitext`), rev **408621**, read 2026-11-14. Requests carried a generic
user agent and no personal data; the raw pages stayed outside the repo. The game data: [ashe-map.md](ashe-map.md) (build 154088,
2026-10-06). Up: [Techniques](../../techniques/README.md) · hub: [heroes/ashe](../../techniques/heroes/ashe.md).

Confidence: **H** BZ / W and the data agree; **M** one source; **L** they disagree or a guess.

## Latest changes
- **BZ 2026-11 (11-03 / 11-05): nothing for Ashe.** BZ 2026-10-06: torso hit volume 22.4 -> 25, legs 16.7 -> 19, the global large
  hitscan 0.07 -> 0.04 m. Both after or in the data build, so **the data's numbers are the live ones**.
- Her last number changes (W log): 2026-06-16 Double-Barreled's second shot keeps its knockback, Viper's Sting no ammo, Airburst 3 -> 6
  ammo; 2026-04-14 Double-Barreled window 1 -> 2 s; 2026-02-10 Sidewinder removed, Double-Barreled added, sub-roles (Sharpshooter);
  2026-01-08 scoped falloff 40-60 -> 35-55 m; 2025-08-26 Remote Detonator in, Rapid Fire out.
- **Passives:** her slot holds only the sub-role's, **Sharpshooter** (W `{{Sub-Role passive|Sharpshooter}}`; D tag 1188, 01C7
  #2832: crits take damage x 0.0075 s off Coach Gun). The Damage role's healing reduction is every hero's in the mod (Combat).

## Kit (W rev 408621 against the data)
| | W | Data | Used |
|---|---|---|---|
| Health | 250 | 0538 250 | 250 H |
| Viper hip | 35 - 10.5, 20-40 m, 0.256 s, 0.04 m, crit ✓ | 35, 20-40, 0.25 s, 0.04, spread 0-2° | data H |
| Viper scoped | 75 - 22.5, 35-55 m, 0.656 s, move -25 % | 75, 35-55, 0.65 s; move x0.6 (M meaning) | data; **move W x0.75 (L: the data's 0.6 unread)** |
| Ammo, reload | 12; 0.5 s + 0.25 s a round (scoped "0.2", L) | 12; 0.5 + 0.25, end 0.3 s | data H |
| Coach Gun | 6 x 15 = 90, no crit, 20 m/s enemy, 18 m/s self, 15 m, 10 s | 6 x 15, 20° rings, 20 / 5, 18, 8-15 m, 10 s | data; 20° as the cone's width (M) |
| Dynamite | 50-20 over 1.5-5 m, burn 100 / 5 s, self 25-10 and 50, 25 m/s, 0.1 m, 2 s on the ground, 12 s | the same, gravity 8 | data H |
| B.O.B. | 1000 (1150 with the Tank passive), 120 charge, 17 x 8/s, 15 m/s, 4 s charge, 10 s fire, 1.5 / 0.5 s immobilize, 1.45°, 40 m; cost 2450 | 1000 (alt 1500), the same; cost 0ADE 2450 (M) | 1150 (W, role queue) M; rest H |
| Perks | Remote Detonator 0.5 s; Double-Barreled 2 s; Viper's Sting 25 over ~1 s; Airburst 5 -> 7 m, +6 ammo | the same | H |
| Perk XP | 1750 / 4380 | 1296 1750 / 1297 2630 | H |

Left out: Stadium, April Fools, 6v6 tabs, B.O.B.'s Stadium commands (1536 #314-#471).

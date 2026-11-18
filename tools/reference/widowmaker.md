# Widowmaker in live Overwatch: numbers and sources (research, 2026-11-14)

**BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`), every month 2025-09 to
2026-11 read 2026-11-14 for her lines and for role / sub-role / global lines (2016 to 2025-08 through W's dated log, which agrees with
BZ line for line where both were checked: 2025-09-16, 2025-10-14, 2026-02-10, 2026-04-14, 2026-09-08, 2026-10-06). 2026-11 holds no
Widowmaker line. **Her last live number change is BZ 2026-04-14** (perk costs: minor -6 %, major -4 %); the last kit change is
BZ 2026-02-10 (Seeker Mine 50 -> 75, Escape Plan out, Widow's Bite in, Sharpshooter sub-role); the last line naming her is BZ
2026-10-06 (bug fix: "reload speed reductions not applying to Widow's Kiss"). **W** = the Overwatch wiki through its MediaWiki API
(`overwatch.weirdgloop.org/api.php`): Widowmaker rev **409087** (2026-11-05T03:44:10Z); Sub-Roles page and
Template:Sharpshooter_value_table read the same day. Requests carried a generic user agent and no personal data; the raw pages stayed
in the scratchpad (`widow_web/`).

Confidence: **H** BZ and W agree (or W gives it and BZ's history leads to it); **M** one source; **L** sources disagree.

Left out (not the live game): **Stadium**, **Community Crafted arcade** (BZ 2026-06-30: 2 Grappling Hook charges, a charge per final
blow, scoped charge -70 %, hip spread -40 %, 2 Venom Mines with a 15 m tripwire for 180, single mine 75 + knockback, Vengeful Web),
**April Fools / Underwatch** (BZ 2026-04-01: Take Aim charges to 150 %, 80 % slower above 100 %, extra zoom), 6v6, PvE. BZ 2026-09-08
(Take Aim charge sound louder above 50 % charge) is audio only.

**Patches that touched her (live)**: **2023-06-13** scoped falloff 70-100 -> 40-60 m, min 30 -> 50 %; **2023-10-31** hip shots to max
spread 3 -> 7, ult -10 %; **2024-02-13** global hitscan sizes; **2024-02-21** hip fire uses the small 0.05 m modifier, scoped falloff
40-60 -> 50-70 m; **2024-12-10** small hitscan 0.05 -> 0.04 m; **2025-01-07** scoped shots lose the global size bonus; **2025-02-18**
perks (Scoped Efficiency, Focused Aim, Escape Plan, Deadly Deux); **2025-03-25** health 200 -> 225; **2025-05-20** hip 13 -> 14 (BZ also
said max spread 3 -> 2 degrees; W: "never went through", shots to max spread 7 -> 11 instead); **2025-07-22** ult -10 %; **2025-08-26**
Focused Aim / Deadly Deux out, Sniper's Instinct / Seeker Mine in; **2025-09-16** Grappling Hook 12 -> 10 s, Venom Mine 75 -> 125;
**2025-10-14** Seeker Mine 25 -> 50; **2026-02-10** Seeker Mine 50 -> 75, Escape Plan out, Widow's Bite in; Damage role passive removed
(its -30 % healing on damage and -15 % vs tanks made global); sub-role **Sharpshooter**; **2026-04-14** perk costs; **2026-10-06** global
"large" hitscan 0.07 -> 0.04 m (hers is the small one, already 0.04: no change), reload fix.

## The kit now (W rev 409087 unless noted)
| | Number | Conf |
|---|---|---|
| Health | 225, no armor (BZ 2025-03-25); 5.5 m/s (default); no passive of her own | H |
| Sharpshooter (sub-role) | critical hits cut movement ability cooldowns (hers: Grappling Hook) by 0.75 % of the crit's damage, i.e. 0.0075 s per point: a hip headshot (28) 0.21 s, a full scoped headshot (300) 2.25 s (BZ 2026-02-10; W Sharpshooter_value_table) | H |
| Perk XP | minor 1410, major 3330 (in all; after BZ 2026-04-14) | M |
| Widow's Kiss (hip, auto) | hitscan, 14 -> 4.2 (30 %) over 20-40 m, headshot 2x, 10 shots/s, 1 ammo, 35 ammo, reload 1.5 s (ammo back at 0.816 s), spread to 3° max after 11 shots, 0.04 m (0 + 0.04 small global) | H (spread: L, BZ 2025-05-20 said 2°) |
| Take Aim (scoped) | hold secondary; 0.32 s zoom in/out; hitscan, no spread, 0 m radius; 0.9 s to full charge, linear 12 (0 %) -> 120 (100 %), +1.08 per %, so it fires at 0 charge; headshot 2.5x; falloff 50-70 m to 50 % (60 at full); 5 ammo a shot; 0.5 s recovery; move -65 % (1.925 m/s); jumping or an ability drops the scope and charge (can rescope in the air); under 50 % charge a softer sound and thinner trail | H |
| Grappling Hook | 10 s cooldown (BZ 2025-09-16), 20 m range, pulls at 20 m/s, projectile hook then movement; jump cancels; a miss returns the hook with no cooldown; keeps some pull momentum at the end, also on walls without a ledge; attaches to Lifeweaver's platforms | H (range/speed: M) |
| Venom Mine | 15 s cooldown; thrown arc 20 m/s, 0.2 m; 0 s cast + 0.992 s recovery; arms 1.4 s; 1 health; trigger 3 m, burst 4 m; poison 125 over 5 s (4.8 per 0.192 s, 25/s; BZ 2025-09-16), no headshot, through armor; enemies only; reveals triggerers to her through walls; one mine (a new one replaces it); sticks to surfaces, not to players, barriers or deployables; a health pack clears the venom | H (reveal time: not given) |
| Infra-Sight | 1400 points; 0.64 s cast; 15 s; global; her whole team sees all enemies and their health bars through walls; ends only on her death or the timer | H |
| Quick melee | 40, 1 s cooldown, 2.5 m, active 0.128 s, 0.528 s recovery (W Quick_Melee) | H |
| Scoped Efficiency (minor) | scoped shots cost 3 ammo instead of 5 | H |
| Sniper's Instinct (minor) | scoped shots charge 100 % faster for 2 s after Grappling Hook | H |
| Widow's Bite (major) | scoped charge goes to 125 % (0.9 -> 1.125 s), 150 -> 75 at 125 %, headshot 2.5x; a fully (125 %) charged shot pierces enemies | H |
| Seeker Mine (major) | Venom Mine stays after triggering and fires a poison dart at each enemy hero within 10 m (not through barriers); 75 over 5 s (BZ 2026-02-10), reveals like the mine; re-arms 8.2 s. W's per-tick line (1.92 per 0.192 s = 50) predates the 75 buff | L (tick rate) |

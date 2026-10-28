# Mercy in live Overwatch: numbers and sources (research, 2026-10-28)

**BZ** = Blizzard patch notes (https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/, read 2026-01 to
2026-10, all of 2025, and 2022-12, 2023-02/04/12, 2024-02/05/08/12 for values nobody changed since; "via W" = an
older patch read only as quoted in W's change log), **HP** = Blizzard's hero page
(https://overwatch.blizzard.com/en-us/heroes/mercy/: kit text and perks only; its ability list still lacks Flash Heal
and Swift Recovery), **W** = the Overwatch wiki on Weird Gloop through its MediaWiki API
(`overwatch.weirdgloop.org/api.php`: Mercy revision 405106 of 2026-10-15; Sub-Roles 406777 of 10-20; Global Passive
Abilities 402069 of 10-09; Roles 401659 of 10-07; Movement speed 402311 of 10-10; Critical hit 404694 of 10-14),
**LQ** = Liquipedia's API (Mercy revision 1124295 of 2026-04-28: stale, still has Flash Heal as a perk, 20 ammo, 1 s
reload). Fandom not used (stale). Left out: Stadium (her powers, including a Stadium-only "Double Dose" power unlike
the perk), Community Crafted (2026-06-30: Remote Resurrection, Guardian Angel on enemies with a 30-over-3 s bleed),
Underwatch (2026-04-01), Quick Play Hacked (2026-07-30: Medic 30%), Overwatch Classic. No Stadium power became core:
Flash Heal came from a perk. Game data (build of 2026-10-06) agrees on the kit, Medic and the four perks.

Patches that touched her in 2026: 02-10 (Season 1), 02-24 (fix), 03-10 (Medic), 03-31 (skin fix), 04-14 (Season 2),
04-23 (perk fix), 05-12, 06-16 (Season 3), 07-28 (Medic fix), 09-08 (challenge fix), 10-06 (hit volumes). Nothing for
her in January (Stadium only), March 12, August, 10-13 or 10-20.

## Base
- Health 225, no armor or shields (BZ 2024-08-20: 250 -> 225; W; data). Support, sub-role Medic (BZ 2026-02-10; HP;
  W). Base speed 5.5 m/s, backward 90% (W Movement speed). Data: her base mover is the hero default (air accel 3.5);
  the 8.0 in `tools/reference/movement.md` is Valkyrie's override (graph 10DA; research\mercy\mercy-map.md).
- Hit volumes (BZ 2026-10-06): head 14.4 -> 17, torso 22.4 -> 24.5, legs 22.4 -> 20 (units not given).

## Caduceus Staff (primary: heal beam; secondary: damage boost beam)
- Heal 55/s (BZ 2026-04-14: 60 -> 55), ticking 1.76 every 0.032 s (W). W's infobox line "hps = 60" is a leftover.
- Damage boost +25% (BZ 2026-06-16: 30 -> 25; W). Doesn't stack with another Mercy's (BZ 2016-07-19 via W). Applied
  when a projectile is fired, so it stays boosted if the beam drops before it lands (BZ 2019-03-19 via W); damage over
  time is boosted while it's being applied (W). Mercy gets ult charge equal to the damage she amplifies (W).
- Range 15 m (W; data). Locks on to the ally nearest the crosshair; she needn't keep looking at them (W). Ignores
  barriers (W). If the target leaves line of sight or moves out of range the beam lingers 1.3 s, then breaks; coming
  back into sight resets that timer (W). No tether range beyond 15 m is given.
- Switching heal <-> boost keeps the target (BZ 2016-04-05 via LQ). Options (W): Toggle beam connection (off: releasing
  ends the beam), Beam sensitivity (narrows the view angle for picking a target; the angle isn't given).
- Swapping to the staff reloads the blaster: it reloads itself after 1.4 s holstered (W).

## Caduceus Blaster
- 20 a shot, 5 shots/s, 25 ammo (BZ 2022-12-06: 20 -> 25), reload 1.4 s, 50 m/s (W; data). No spread, no falloff (W).
  Projectile radius 0.275 m: 0.2 base (BZ 2024-02-21) + 0.075 global (BZ 2024-03-12 via W). Headshots count (W), at the
  usual 2x (W Critical hit). 100 a second firing, 78 with reloads (W).
- Weapon swap: "reduced from 0.5 to 0.35 seconds" (BZ 2022-12-06); W: a 0.51 s recovery after switching. Conflict.

## Guardian Angel (ability 1)
- Fly to an ally, alive or a dead ally's soul, up to 30 m away, at 17 m/s (W). Cooldown 1.5 s (BZ 2023-04-11: 2.5 ->
  1.5; W, LQ), starting when Guardian Angel ends (W), so a long flight only delays it; nothing scales with distance.
- Cancel by pressing it again (Toggle Guardian Angel, on by default; off = releasing cancels) (W); the cooldown starts.
- While flying (HP: "Jump launches you forward, and Crouch launches you upward"): Jump launches her the way she faces at
  17 m/s; Jump + back launches her away from the target, 20% slower (BZ 2023-02-07; 13.6 m/s, ours); Crouch launches her
  straight up (BZ 2022-10-04 via W) at 20.4 m/s (W): the "superjump".
- A momentum meter charges while she flies: the fuller it is, the stronger the launch (BZ 2022-07-11 beta via W; W).
  A launch sets the active state to 1 s (BZ 2024-02-13: 1.5 -> 1), which can't be cancelled to start the cooldown
  early (BZ 2023-04-11; W). The cancel boost is disabled while she's stunned (BZ 2022-12-06 bug fixes).
- Launch speed -10% (BZ 2026-04-14), reverted (BZ 2026-05-12): today's speeds are the 2025 ones (W has kept 17 / 20.4
  throughout, e.g. revision 273410 of 2025-12-22).
- Target priority option: prefer beam target (default), prefer facing target, facing target only; Guardian Angel
  sensitivity option (W). Breaks fences and breakables on the way (W).
- Angelic Descent during a launch ("slingshot") gains a little height and momentum (W strategy).

## Angelic Descent (passive, Jump)
- Hold Jump in the air to fall at 2 m/s (W). Releasing ends it; it can start again any time airborne (W). Crouch works
  too (BZ 2022-10-04 via W; "Crouch activates Angelic Descent" option, on by default). Options, both off by default:
  toggle instead of hold, and turn it on automatically after Guardian Angel (W). A 2022 beta note says holding it also
  slows ascent (BZ 2022-06-28 beta via W): not confirmed for live.

## Resurrect (ability 2)
- Target a dead ally's soul within 5.5 m (W; LQ and BZ 2017-09-19 say 5). Sight isn't needed during the cast (W); it is
  to start (implied by W, and by Community Crafted's perk removing it). Cast 1.75 s (BZ 2017-11-16 via W; W; data),
  cooldown 30 s (BZ 2017-09-19 via W; W; data), not reset by respawning (W). Holding the key while it's on cooldown casts it when ready (BZ 2022-01-25 via W).
- While casting she moves 75% slower (BZ 2017-11-16 via W): 1.375 m/s, 2.25 m/s in Valkyrie (W); no other abilities
  (W). A Guardian Angel already under way keeps carrying her (W strategy). She isn't invulnerable (BZ 2017-09-19 via W).
- Interrupted, with the cooldown starting and nobody revived, if she's stunned, knocked down or hacked, or gets more
  than 7 m from the soul (W; BZ 2017-11-16 said "knocked back, stunned, or hacked"). Damage alone doesn't interrupt it.
- The ally comes back at full health where they died, invulnerable 2.25 s, unable to shoot or use abilities until it
  ends; they can move after 1.5 s (W). No ult charge for it (W). A soul falling into a pit can be revived and freezes in
  place for the cast (BZ 2022-01-25 via W). Valkyrie doesn't change Resurrect (no instant cast: BZ 2018-01-30; no
  cooldown reset: BZ 2017-10-17; both via W).

## Flash Heal (Reload)
- Baseline since BZ 2026-04-14 (a major perk from BZ 2025-02-18). "Heal Caduceus Staff's active target, healing is
  doubled on allies below half health." (data; W). 60, or 120 below 50% health (BZ 2026-04-14: 50 -> 60, x3 -> x2).
  Cooldown 12 s (BZ 2026-04-14: 15 -> 12). Cast 0.4 s (BZ 2025-03-18: 0.875 -> 0.4; W "0.4 + 0"). One charge.
- Needs a staff target; works whether the beam is healing or boosting (W). Ignores barriers (W). Range is the beam's
  (ours: no source says).

## Swift Recovery (passive) and regeneration
- "Begin regenerating health more quickly." (data; W). Starts regeneration 3 s after damage instead of 6 (W; BZ
  2026-02-10 only says "Health regeneration begins sooner"). The same as the removed Support role passive (W Sub-Roles).
- Everyone's out-of-combat regeneration: 10 HP/s + 5% of max health a second (BZ 2024-05-14; W) after 6 s without
  damage (BZ 2025-06-24: 5 -> 6; BZ 2026-02-10: "remains unchanged"). Mercy: 21.25 HP/s, from 3 s (ours).

## Valkyrie (ultimate)
- 2150 points (W, raised from 2000 on 2026-06-17 after BZ 2026-06-16's "+7%"; 2000 x 1.07 = 2140). 15 s (BZ
  2018-01-30 via W; W; data). Cast animation 0.496 s (W). Keeps the staff's target (BZ 2023-12-05). Resets Guardian
  Angel's cooldown (BZ 2022-12-06 bug fixes: "now correctly resets its cooldown if Valkyrie is used").
- Free flight at 9 m/s (+63.6%; W), outside the speed cap (W). Hold Jump to rise, Crouch to sink (W; an option puts rise
  on Ability 3). Horizontal and vertical share the 9: aiming level with both held gives 6.36 each, with a strafe too
  4.5 vertical and 7.8 horizontal (W). Knockback taken -50% (W). Roots and hinders stop the flight for a moment
  without ending it (W).
- Staff: 65/s to each target (1.04 every 0.016 s; W; data), range doubled (30 m, ours from W's +100%). The beam
  branches from its target to every ally within 10 m in that target's sight (W; LQ), no cap given; boost branches too.
- Regeneration is on throughout and damage doesn't stop it (BZ 2023-04-11; W). Medic self-healing stacks for each ally
  healed (W). Blaster: unlimited ammo, 100 m/s projectiles (W). Guardian Angel: 60 m, 25 m/s, 26.5 m/s on a crouch
  launch (W). Shows the health bars of damaged enemies (BZ 2018-02-27 via W). No heal or refill on cast is given.

## Role passives
- Support role passive removed (BZ 2026-02-10); global regeneration after 6 s stays (BZ 2026-02-10).
- Medic (BZ 2026-02-10): "Healing allies with your weapon also heals you." 40% of healing dealt (BZ 2026-03-10: 25 ->
  40; W). Stacks per ally, counts healing modifiers on both her and the target, gives no ult charge (W). About 22 HP/s
  beaming one ally, 26 per ally in Valkyrie (ours). BZ 2026-07-28 fixed Mercy getting more from it than intended.
- Global since BZ 2026-02-10 (the old Damage passive): damaging an enemy cuts their healing received 30% (15% on
  tanks) for 3 s (W Roles).

## Perks (HP, data; minor at level 2, major at level 3; W perk XP 1835 / 4590)
- Minor, Angelic Resurrection: "Mercy gains 100 overhealth after casting Resurrect." (BZ 2025-04-22). Lasts 2 s, then
  decays 5 HP/s; given even if the cast is interrupted (W).
- Minor, Winged Reach: "Guardian Angel's range is increased by 30%." (BZ 2026-05-12, back after its removal on
  2025-08-26). 39 m, 78 m in Valkyrie (W).
- Major, Chain Boost: "Caduceus Staff's damage boost is increased by 5% and links to a second nearby ally." (HP; BZ
  2025-02-18; +5% BZ 2026-06-16). 30% (W). How near isn't given.
- Major, Double Dose: "Flash Heal gains an additional charge but its base healing is reduced by 10." (BZ 2026-04-14;
  a bug that kept the full healing fixed by 04-23). 50, or 100 below half health (W).
- Removed: Angelic Recovery (15 HP/s beaming a full-health ally; 2025-02-18 to 04-22), the Flash Heal perk (50, x3;
  15 s; to 2026-04-14), Divine Momentum (+15% speed after cancelling Guardian Angel; 2025-08-26 to 2026-05-12).

## Voice, lore
- Lucie Pohl does her English voice (W, LQ). Dr. Angela Ziegler, Swiss, 39, born May 13 (HP). Orphaned by the Omnic
  Crisis, a child prodigy who pioneered applied nanobiology, she became Overwatch's head of medical research and its
  best-known field medic, flying a Valkyrie suit; a family friend of Torbjörn Lindholm (HP). Line on her W card:
  "Don't worry, I'll be right behind you."

## Not given by any source (ours when used)
The beam's acquisition angle and any tether range past 15 m; Flash Heal's range, its per-charge cooldown with Double
Dose and whether Medic counts it; Guardian Angel's meter fill time and launch speed below a full meter, its path round
obstacles, what happens when the target moves or dies mid-flight, its longest flight time; Angelic Descent's air
control; Resurrect's soul lifetime and size; Valkyrie's flight acceleration, its chain boost today, whether Flash Heal
reaches chain targets, ammo when it ends; Chain Boost's link range and how the second ally is chosen; hit volume units.

## Disagreements
- Ult cost: W 2150, though 2000 x 1.07 = 2140. The data's two values are perk XP, not the ult: 1835 is W's minor perk
  XP and 1835 + 2755 = 4590, W's major. So the data agrees with W on perks and gives nothing to check 2150 against.
- Flash Heal cast: BZ and W 0.4 s; the data 0.5 s (perhaps cast plus recovery).
- Weapon swap: BZ 0.35 s; W 0.51 s.
- Valkyrie healing: the last BZ number is 60 (2018-11-13); W and the data say 65, an undocumented change: 65 used.
- Valkyrie's chained damage boost: W "30% per target", from before BZ 2026-06-16 cut the boost to 25% (the data's 25%).
- Resurrect: range BZ 2017 and LQ 5 m, W 5.5 m; interrupts: BZ 2017 knockback, W knockdown. W used.
- Medic: BZ 2026-02-10 also lists "Reduced Ultimate Charge Gain Rate: -25% while above 100%" under Medic, a copy of
  Tactician's line; W doesn't have it. Left out.
- The data's "regen 10" is W's 10 HP/s base; W and BZ add 5% of max health a second.

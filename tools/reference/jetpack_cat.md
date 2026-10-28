# Jetpack Cat in live Overwatch: numbers and sources (research, 2026-10-27)

For #38. **BZ** = Blizzard patch notes (https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/, read
2026-02 to 2026-10), **HP** = Blizzard's hero page (https://overwatch.blizzard.com/en-us/heroes/jetpack-cat/), **W** =
the Overwatch wiki on Weird Gloop through its MediaWiki API (`overwatch.weirdgloop.org/api.php`, Jetpack Cat revision
408445 of 2026-10-26; the Fandom copy answered 402 and is stale), **LQ** = Liquipedia's API (revision of 2026-07-05:
launch numbers, stale), **ML** = Mobalytics guide (snippets only; 403). Stadium, Community Crafted (2026-06-30 to
07-13: Zoomies, 10 s Lifeline, grenade-launcher Catnapper, turrets on her), Underwatch and other limited modes are left
out. Game data (DataTool heroes.txt, build 154088 of 2026-10-06) agrees on the kit, the passive and the four perks.

Released 2026-02-10 (Season 1). Patches that touched her: 02-13, 02-24, 03-10, 03-12, 04-14, 06-16, 08-11, 08-14, 09-08,
09-10, 10-06. Nothing for her on 10-13 or 10-20.

## Base
- Health 225, no armor or shields (W, LQ). Support, sub-role Tactician (BZ 2026-02-10; data). Base speed 5.5 m/s (BZ
  2026-08-14, back from 6 set on 08-11). Air acceleration 9.5 (BZ 2026-08-11, from 7.7; the data holds 9.5).
- Knockback taken -40% ("flight resistance", W). Hit volumes (BZ 2026-10-06): head 18.5, torso 17, waist 26, legs 12
  (from 15.8 / 14.8 / 19.5 / 9.7; units not given). Data size: Normal.

## Jetpack (passive): permanent flight
- "Permanent flight" (HP, data). Moving forward follows her aim (look up to climb, down to dive); moving backward
  reverses it. Hold Jump to rise, Crouch to sink (W). Plain flight costs no fuel; only Frenetic Flight does (W).
- Not affected by EMP. Falls to the ground when hindered, knocked down, frozen or hit by a fall-causing ability
  (W names Downpour, Binding Chain, Palatine Fang's overhead strike). Can't use jump pads or elevators unless made to
  fall (W). She has jump and landing voice clips, so she can touch down (W quotes).
- Data (`tools/reference/movement.md`): base mover gravity scale 0.3 (others 1.75); flight state (graph 59DC,
  priority 99) ground accel 12, air accel 9.5, max air speed 12, air drag 8/s, flying flag. Two more states in graph
  59E0: priority 100 (forward/back/strafe 6/5.5/6, air accel 6, max air 10, drag 1.25) and priority 101 (air accel 2,
  max air 16, drag 2). movement.md read 59E0's 6.0 as the 2026-08-11 base speed, but that was reverted on 08-14 and
  this build is from 10-06, so 59E0 is something else (Lifeline or the boost?): not confirmed.

## Biotic Pawjectiles (primary fire)
- 5 projectiles a volley: 1 centre pellet below the reticle, 4 "paw" pellets in an arc above (W). Each heals allies
  and damages enemies for the same amount.
- Paw pellet 4 (BZ 2026-09-08, down from 4.5 set on 06-16; launch 4). Centre pellet 8 (W, LQ). 24 per volley (W).
  Headshots x2 on paw pellets only (W): 40 with all four paws critting (ours; W's "45" is from the 4.5 era, which
  implies the centre was 9 then, so it probably scales with the paws).
- 4.48 volleys/s (0.224 s; W; LQ 4.5), 24 ammo (one a volley), reload 1.25 s (W, LQ). 60 m/s (W, LQ). Range 40 m, then
  the projectile is gone (W; ML: "no falloff"). Blizzard's Weekly Recall says the primary has "a falloff range":
  conflict, probably meaning the 40 m limit.
- Spread 0 deg centre, 0.3 deg near paws, 0.9 deg far paws (W). Projectile radius: paws 0.19 m against enemies, 0.31 m
  for allies; centre 0.37 m / 0.62 m (W). At close range the paws land wide of the reticle (W).
- 107.5 damage or healing a second while firing; about 87 with reloads (ours, from 24 x 4.48 and 24 ammo + 1.25 s; W's
  DPS line is mid-edit).

## Frenetic Flight (secondary fire, held)
- "Accelerate in your movement direction" (HP), any direction (W). Fuel: costs 40%/s (2.5 s of boost on a full tank, ours),
  refills 25%/s (BZ 2026-02-13: 30 -> 25) after a 0.75 s delay, 4 s from empty (W).
- While carrying anyone: refill 3x slower (BZ 2026-06-16, was 2x), so 8.3%/s (W rounds to 8; LQ's 15 was launch).
- Top speed 17.8 m/s, written as "+230.1%" (W; 5.5 x 3.301 = 18.2: doesn't match, flag). Acceleration -11% (BZ
  2026-04-14, no base number given). Catnapper refills the tank (W).

## Lifeline (ability 1, a toggle)
- Toggle into transport mode; an ally inside a cylinder 8.5 m radius x 15 m high can choose to be towed (W): they
  press Interact (ML). Allies over a death plane accept automatically (BZ 2026-09-08). B.O.B. is taken automatically
  (W). Rope 4 m (W). Cooldown 2 s (W, LQ). No time limit in the live game (the 10 s was Community Crafted only).
- Her speed +30% while towing, 7.15 m/s (BZ 2026-08-11: 40% -> 30%; W). Heals the towed ally 25/s (4.8 every 0.192
  s; W, LQ). The towed ally can shoot and use abilities (ML; Blizzard's two-seater intent), except ground-only ones
  in the air (RIP-Tire, Teleporter; W). Shown "Grappled" on their screen (W).
- Ends when she fires her primary, casts her ultimate or presses the key again; she keeps Frenetic Flight and Purr
  (W). Purr is her only healing while towing (W).
- The ally leaves with Jump or a movement ability; leaving with Jump launches them at 13.2 m/s (W; LQ mislabels it as
  her speed), momentum kept. A cancel or forced break (stun, mobility lock: BZ 2026-09-08; Break abilities; Cassidy's
  Deadeye: BZ 2026-10-06; Orisa's Terra Surge: W) drops them with no launch. Breaks when they're pulled through
  terrain or get too far away (W).
- Carrying a self-destructing D.Va keeps the mech, not the pilot (W). Scoring: a last-second tether off a cliff
  counts as a Save (BZ 2026-08-11).

## Purr (ability 2)
- 12 s cooldown, 4 s long (W, LQ). On cast: 30 damage and a knockback of 16.6 m/s (15 horizontal, 7 vertical) to
  enemies within 10 m, once each (W). Doesn't go through enemy barriers (BZ 2026-09-10 fix).
- Heals in 4 pulses of 1, 2, 3 then 4 waves (10 waves), about 0.95 s apart, 10 m radius (W): 30 a wave to allies, 18 to
  herself (self-healing penalty 40%: BZ 2026-08-14, was 25%). 300 allied / 180 self over the cast (ours). Most of it
  comes at the end (W).
- W flags it (and Catnapper) as ignoring Defense Matrix, deflect and "window".

## Catnapper (ultimate)
- 2575 points (W). Conflict: the launch cost was 2400 (Fandom, by search snippet), +7% on BZ 2026-08-11 gives 2568
  (NamuWiki snippet). 2575 used.
- Aim at a ground spot up to 25 m away (a wall targets the floor below it; past range, the floor at 25 m), then dive at
  30 m/s (W). Players describe it as press the ult, then confirm (forum, 2026-02-13).
- On landing, enemies within 6 m take 25 damage, are knocked down (stun 1.5 s) and pushed 2.8 m/s; the nearest is
  tethered to her (W; "can't be cleansed" only in a search snippet). Lifeline switches on automatically, and stays on
  if nobody was hit (W).
- She carries them for up to 4 s, healing herself 30/s (5.76 every 0.192 s), 30% slower with a tank (W; Blizzard's
  Weekly Recall: enemy tanks are pulled more slowly "due to their inherent weight"). She can use
  Purr, Frenetic Flight and quick melee, not her primary (W). They can shoot her once the stun ends (W, ML). They can't
  wriggle free: only Life Grip, Charge, Chain Hook, Overrun or an enemy Catnapper break it (W).
- Pressing the ult again or Lifeline cancels it; primary fire doesn't (W). A cliff drop gives no ult charge, but a Purr
  after it does (W). Keywords: Tether and Break (BZ 2026-02-10).

## Role passives
- Support role passive removed (BZ 2026-02-10); global regeneration after 6 s stays (BZ 2026-02-10; see lucio.md).
- Tactician (BZ 2026-02-10): excess ult charge kept after the ult, up to +25%, earned at -25% above 100%.

## Perks (HP, data; minor at level 2, major at level 3; W costs 2100 / 5200)
- Minor, Ulterior Motive: "15% of Biotic Pawjectiles healing recovers fuel." (BZ 2026-03-10). About 16%/s of fuel
  while healing nonstop (ours; W's 18.15% is from the 4.5 era).
- Minor, Transport Shielding: "Gain up to 50 extra shield health while carrying another hero." Lasts 3 s (BZ
  2026-06-16: 75 -> 50, 5 -> 3 s). Carrying an enemy counts (W).
- Major, Purrfect Form: "Each pulse of Purr contains 3 waves." (BZ 2026-08-11): 12 waves, 360 allied / 216 self (W).
- Major, Claws Out: "Quick melee becomes empowered every 6 seconds, wounding enemies for 40 damage and slowing them by
  30% for 1 second." (BZ 2026-03-10 moved to major, 8 -> 6 s; 03-12 fix; 04-14 cooldown reduction applies). The wound
  is 40 over 1 s (3.84 every 0.096 s), cleansable, ignores armor (W).
- Removed: Territorial (Purr damage for 50%, then 33% of its healing, no knockback; removed BZ 2026-03-10) and
  Headbutt (boost into an enemy at 14 m/s or more: 50 damage, knockback 17.27 m/s; removed BZ 2026-08-11).

## Voice, lore, personality
- Fika (FEE-ka) Lindholm, a stray orange tabby Brigitte found at Watchpoint: Gibraltar and adopted (HP, W). After
  Talon hit Gibraltar, Brigitte built her a jetpack; Sojourn hasn't approved her for the field yet (HP). Fika is
  Swedish for a coffee-and-pastry break. Formerly of Gothenburg (W). The webcomic hints she spies for Lucheng
  Interstellar (W, Wikipedia).
- Jennifer Hale (also Ashe) does her voice, all meows, purrs and mews (Blizzard via press, 2026-02). The jetpack talks
  in Swedish: "Terminalen aktiveras" on pick, "Laddar om" reloading, "Hälsa ges" healing, "Acceptera" on Lifeline,
  "Tjuder aktivt" on tether, "Varning! Varning!" at low health (W quotes).
- Playful chaos: built on real cat behaviour, knocking things over and playing with string; her emotes knock items off
  things (Blizzard Weekly Recall; W). Allies can ping her to call a pick-up voice line (BZ 2026-02-10).

## Not given by any source (ours when used)
Vertical speed on Jump / Crouch, idle hover (drift or hold), any height limit, how fast she reaches 5.5 m/s and 17.8 m/s
in m/s²; fuel in absolute units; the paw arc's exact layout; Purr's wave timing inside a pulse and when its cooldown
starts; Lifeline's cooldown start, how she picks between several accepting allies, the rope's physics; Catnapper's
cast time, aim indicator and drop behaviour; units of the hit volumes; her collision size.

# Kiriko vs real Overwatch: picky visual critique, round 5 (2026-10-26)

This fifth pass covers only the captures in `run/screenshots/` taken after round 4:
- HUD: `hud7_rest`, `hud7_cast`, `hud7_cooldown1`, `hud7_cooldown2`, `hud7_ready`, `hud7_reload`, `hud7_rushing`,
  `hud7_spent`; `ult_rejoin`.
- Effects: `fx9_ofuda_a..e`, `fx9_kunai_a..c`, `fx9_suzu_a..d`; `fx10_step_a..c`, `fx10_rush_a..c` (third person);
  `fx10_fp_a..c`, `fx12_c1..c6` (Kitsune Rush in first person); `st_a..c` (Swift Step in first person).
- Night and hit markers: `hm6_body`, `hm6_head`, `hm6_kill`.
- Kill feed: `feed1_a..c`, `feed2_c`. Options: `opt_r2`, `opt_r4`, `opt_hov1`, `opt_hov2`.

The Overwatch frames are the same as in the earlier rounds: `ref1080` (the owner's mid-Swift-Step frame), `ref_owner_02`
and `ref_queued_00..03`, all at 1920x1080. They stay in the session scratchpad, with this round's crops in its
`critique\round5\` folder. The other sources are `kiriko-reference.md` and `kiriko-effects.md`. This report is text
only (repo rule).

**Method:** crops at the same 1080p coordinates, zooms of 2-8x, Pillow pixel samples, hue statistics over effect
pixels, bounding boxes and line profiles. The reference frames are blurred, so their edges run 1-2 px soft. Distances
are estimated from known sizes (a villager or zombie about 1.95 m, Kiriko about 1.7 m, a block 1 m) with a focal length
of about 771 px (Minecraft's 70° vertical FOV at 1080p), so metres are approximate.

**Confidence:**
- **H:** measured, or clearly seen in both.
- **M:** seen once, or partly inferred.
- **L:** general knowledge, or seen poorly.

**References to earlier rounds:** "R1 #n" to "R4 #n" are items from the earlier rounds. Items those rounds marked done,
not ours or left as is are repeated only where these captures show them still wrong: items 1 and 3 below are R4 items
marked done that still fail in part, and item 8 is what is left of R4 #7.

**Not counted, per the brief:**
- The flat test world, villagers, zombies and pigs as stand-ins, Minecraft's sky and blocks, burning mobs and dropped
  items in the distance, the death poofs.
- Red command-feedback text in chat, the "Advancement Made" and "New Recipe" toasts.
- Ofuda on the right mouse button and kunai on the left; G for hero mode; Esc opening the Overwatch-style menu.
- The hero select screen's ability icons.
- Overlapping effects: each effect is judged on its own.

## Status (kept up to date)
- **Done** (CHANGES.md 2026-10-26): 3 (above the box's highest top corner, `r5_st`), 7 (fainter rings, fading as they
  open), 8 (no glints within 3 m of the camera, `hm7_head`; body-hit sparks kept, ours), 11 (gold ring, `r5_suzu`).
- **Already done before this round's captures:** the chat (R4 #12) and the cooldown splash (R4 #14) in "Earlier items":
  `hud7_*` were taken before those changes.
- **Open:** 1, 2, 4, 5, 6, 9, 10, 12.

## Confirmed fixed (seen in these captures)
- **Terrain-hit glow (R4 #1):** the impact glows in `fx9_ofuda_d` and `hud7_spent` are soft, with no straight edge.
  The trail ribbons still have one (item 4).
- **Homing ofuda colour (R4 #2, the colour only):** the hottest pixels are #E1BC1A-#E4BE20 (hue about 49°, 88%
  saturation): gold, no longer cream. The paper and the trail width are item 1.
- **Kitsune Rush gates (R4 #3, the material):**
  - `fx10_rush_c`: the gates are a translucent white-cyan (#ACDAE6-#BAEAEB over the sky). Only 17% of the gate pixels
    clip, on the bright top edges.
  - The near posts are no longer saturated cyan slabs. What's left is item 6.
- **Night arms (R4 #5):** in `hm6_*` the forearm is #684338-#7B4B3E (it was #302438) and the glove #75232C (it was
  #170710). The arms read clearly at night, as in `ult_rejoin`.
- **Kill feed (R4 #6, part):**
  - A glyph is always drawn: kunai, fist or skull.
  - The ">" is 10 x 15 px with a 4 px stroke (round 4 asked for about 9 x 14 with a 3 px stroke).
  - Mob faces sit at the victim's inner end.
  - What's left is item 10 and the white frame (below).
- **Bubbles gone (R4 #7):**
  - `fx9_kunai_a`/`b`: a wall hit shows only 6 radial cyan streak sparks. Each is 15-25 px, about 0.1 m at the wall's
    roughly 4.4 m, against the data's 0.05-0.06 m: twice the size, not worth an item.
  - `fx10_step_a`: the Swift Step arrival has no sphere.
- **"+" sparkles (R4 #8):** soft glowing crosses of about 35 px (`fx10_rush_a`), some of them gold in
  `hud7_rushing`.
- **Swift Step arcs (R4 #9):** no slash arcs over the first-person view (`st_a..c`, `hud7_cast`).
- **Ally bar placement (R4 #10, part):** it is no longer pinned to the top of the screen. What's left is item 3.
- **Ally pips (R4 #11):** about 9 x 8 px at an 11 px pitch (`hud7_cast`, 6x zoom).
- **Volley bar (R4 #13):**

  | | Ours | Overwatch (`ref1080`) |
  |---|---|---|
  | Amber span, x (unbroken) | 887-1032 | 889-1032 |
  | Amber span, y | 662-689 | 663-687 |

  Ours is 2-3 px taller (27 rows against 24). Otherwise, row by row (2 px steps through one tile), the outline is
  within about 2 px of the reference's. Not worth an item.
- **Ult after rejoining:** `ult_rejoin` shows 61 with its ticks.
- **Hit markers:**
  - On a body hit, the four diagonal strokes are white-grey (#8B8D95 while fading).
  - On a head hit they are red (#F63938).
  - On a kill they are red (#FB3B3B) and longer: they reach 18 px from the centre, against 12-13 px.

---

## Ranked list (most noticeable first)

### 1. Homing ofuda: gold now, but still comets with "wings", and no paper [R4 #2 marked done, still wrong in part] (H)
- **Where:** `fx9_ofuda_a`, `fx9_ofuda_b`: two ofuda locking onto the villager, about 10 m away (it is about 150 px
  tall).
- **Overwatch:**
  - Effect 7E2D is the paper talisman (mesh 10942) plus a ribbon, with a gold core glow behind the paper.
  - In the owner's frame (R1 #1) the gold trails are thin streaks, 3-4 px wide.
- **Ours:**
  - **The heads:** each is a gold teardrop of about 45 x 15 px. No pixel near either head is brighter than #E4BE20, so
    nothing white (no paper) shows at all. At 10 m, a 0.25 x 0.75 m paper would be about 19 x 58 px.
  - **The tails:** two broad tails come in from the left and the right and meet on the villager's chest. They read as
    wings, like R2 #2's "gold smears on both sides".
  - **Tail width:** 18-20 px near the heads, widening to 36-40 px 200 px back (left tail: y 554-593 at x 760; right
    tail: y 549-584 at x 1140). That is about 0.25 m, against the 0.07 m round 4 asked for.
- **Fix:**
  - Draw the paper card unlit, in front of the glow: #F4F1EA with the red seal, 0.25 x 0.75 m, velocity-aligned.
  - Keep the gold glow behind it at 1 m or less.
  - Cut the ribbon to 0.07 m or less, with its alpha fading to 0 over its length.

### 2. Kiriko's teal turns mint-green over grass (H measured; M on the cause)
- **Where:** almost every effect, since the world is grass.
- **Overwatch:** Kiriko teal is #00BCB1 / #00FFF0, a hue of 176°. Kunai and Kitsune cyan is #00FFFF (180°).
- **Ours:** median hue over the effect pixels:

  | Effect, capture | Median colour | Hue |
  |---|---|---|
  | No-target ofuda trails, `fx9_ofuda_d` | #6BCD84 | 135° |
  | Swift Step petals, `fx10_step_c` | #72DF9B | 143° |
  | Swift Step arrival rings, `fx10_step_a` | #5CD996 | 148° |
  | Ofuda ground flowers, `hud7_spent` | #40B07E | 153° |
  | Kitsune Rush path tiles, `hud7_rushing` (G clipped in 73% of tile pixels) | #5AFFCE | 162° |
  | Fox halo, `fx10_rush_a` | #5BC98C / #59F1B1 | about 150° |
  | Speed lines, `fx10_rush_c` | #62C780 / #4C9861 | about 138° |

  Over the sky the same effects read cyan.
- **Cause (M):** the faint parts of the effects add a little teal to the grass (#556E34). Grass is mostly green, so
  the sum leans green: for example, #556E34 plus (0, 60, 55) gives a hue of about 136°.
- **Fix:**
  - Blend the faint body of each effect over the scene (alpha), so it replaces the grass instead of adding to it.
    60-70% of #00BCB1 over #556E34 lands at a hue of about 165-170°.
  - Keep additive only for the white-hot cores.
  - Or shift the faint end of the colour ramps toward blue (about #00A8D0), so the sum lands near 176°.
  - Stop the tiles clipping to #4AFFFF.
  - Check on grass, stone and sky.

### 3. The ally health bar sits on a close ally's forehead [R4 #10 marked done, still wrong in a new way] (H)
- **Where:**
  - `st_c`: the villager's head top is at y 253 and the pips at y 254-261.
  - `hud7_cooldown1`, `hud7_cast`: the head top is at y 190, the pips at y 193-200 and the chevron at y 207-212, all
    painted on the face.
  - At range it's right: `fx10_step_b` has the head top at y 475 and the bar at y 444-452.
- **Overwatch** (`ref_queued_02`/`03`): the white pips sit clear above the head, and the cyan "v" sits above the
  ally's icon.
- **Ours:**
  - Whenever the ally is close and its head is above eye level (every Swift Step arrival), the bar is drawn 1-3 px
    inside the head's top edge.
  - The likely cause: it is anchored at the bounding box's top centre, which lies behind the face and so projects
    lower on screen than the head's near top edge.
  - The chevron is 10 x 6 px with 1-2 px strokes, #389ECA. Overwatch's in `ref1080` is about 14 x 8 px, about 3 px
    thick, in a brighter #29A8E0.
- **Fix:**
  - Take the smallest screen y of the bounding box's four top corners and draw the bar's bottom at least 4 px above it.
  - Make the chevron about 14 x 8 px with 3 px strokes.

### 4. No-target ofuda: the trail ribbons end in straight hard edges in the ground (H)
- **Where:** `fx9_ofuda_d`: two ofuda landing about 4 m ahead.
- **Overwatch:** the ribbon trail (strip 0797, texture 30182) is soft, eroded by noise and fades to the tail. Nothing
  has straight edges.
- **Ours:** the measure is the blue excess over the grass.
  - The left trail's top edge fades over 10-12 px.
  - Its lower edge is cut in one pixel: +8 to -10 between y 629 and 632 at x 730, and between y 638 and 641 at x 710.
  - Its end is a vertical cut at x 676-680 (y 620-640).
  - The right trail (x 1100-1250) does the same.
  - The trails read as translucent glass wedges stuck in the grass. It is R4 #1's depth clip, now on the ribbon.
- **Fix:**
  - Fade the ribbon by its distance to the surface behind it (a soft-particle depth fade), or lift the last trail
    points as the ofuda nears a surface.
  - Fade the tail over its last 20-30% instead of cutting it.

### 5. Kitsune Rush: the fox is a small dim figure inside a mint glow ball (M)
- **Where:**
  - `fx10_rush_a` (third person): the fox is about 20 m ahead and about 16 x 34 px, pale blue (#5099C5-#68B9DB).
  - The glow around it is about 80 px across (about 2 m), mint (#5BC98C-#59F1B1).
  - A flat white ellipse of about 34 x 10 px sits over its head.
  - `fx10_fp_a`: the same ball, about 130 px across, sits right on the crosshair.
- **Overwatch:** the fox itself is the bright thing: a translucent, glowing, crystalline ice-blue body, 2.9 m nose to
  tail. There is no separate halo ball.
- **Fix:**
  - Put the glow on the body: a fresnel rim plus some bloom no bigger than the silhouette, in cyan (item 2).
  - Drop the flat ellipse, or turn it into a low wake on the ground behind the fox.

### 6. Kitsune Rush in first person: the nearest gate frames the view like a glass doorframe (M on ours; L on Overwatch)
- **Where:** the first gate is about 2.6 m ahead.
  - `fx10_fp_a`: its posts are at x 375-465 and 1440-1555, from y 0 down to about y 830. They are #BAEAFF-#BFF7FF over
    #91BDFF sky. The crossbeam is a band across y 0-90.
  - `fx12_c2..c6`: the same frame for the first 0.8 s.
  - `hud7_rushing`: the posts are about 240 px wide (x 230-470 and 1440-1690), #A4C7CA over #5A696C stone.
- **Overwatch (BLZ-KitsuneRush, `kiriko-reference.md` §2):** after the cast, the gates recede ahead and a white-cyan
  mist sits at the edges. No frame around the view is recorded.
- **Ours:** R4's fade (nothing at 1.5 m, full at 4 m) leaves this gate at about 45%, so a pale rectangle surrounds the
  screen until she runs through it.
- **Fix:**
  - Start the first gate 5 m or more ahead of the caster, or fade gates out to about 6 m in the caster's first person.
  - Check against the BLZ capture before choosing.

### 7. Swift Step arrival (third person): crisp neon hoops (M)
- **Where:** `fx10_step_a`.
- **Overwatch (3CC5):** 2 rings (0F5F with the streak band 2E59A), soft and broken by the streak mask, in the teal map,
  lasting 0.3-0.35 s.
- **Ours:**
  - Two flat, unbroken rings about 460 px across (about 2.2 m), one at her waist and one on the ground.
  - Each has a 2-4 px core of #80FFC0-#9FFFC3 with its green clipped.
  - They read as solid hula hoops (the size is fine).
- **Fix:**
  - Mask the ring with the streak or noise band, so it is broken and soft.
  - Cap the core at about #00FFF0 at 70% or less.
  - Fade it while it expands over 0.3 s.

### 8. Kunai hit: the glint lingers as a long streak across the target (M)
- **Where:**
  - `hm6_head`: a faint teal band at y 529-547 from x 726 to 1188 (about 460 px), +7 to +14 in green and blue over
    the sky. The zombie is about 5.7 m away, so that is about 3.4 m.
  - `hm6_kill`: 216 px.
- **Overwatch:**
  - The glint (20065) is 0.42-0.46 m (about 60 px at that range) and lives 0.1-0.17 s.
  - The 6 cyan sparks (3BF4) are for terrain hits only. A hit on a target shows the game's generic hit effect.
- **Ours:**
  - R4 #7 limited glints to flight, but the last glint outlives the hit and is 7-8 times too long.
  - Body hits also throw the cyan streak sparks (`hm6_head`, `hm6_kill`, `hm6_body`) (L).
- **Fix:**
  - Cap the glint at 0.45 m, and remove the glints when the kunai hits.
  - Spawn the cyan sparks only on blocks.

### 9. Swift Step cast: the seal rises to the crosshair and hides the target (M)
- **Where:**
  - `st_b`: the index fingertips reach about y 530 and cover the target's LSHIFT label.
  - `hud7_cast`: about y 580.
- **Overwatch (BLZ-SwiftStep, `kiriko-reference.md` §2):** the hands clasp at the bottom centre, x 0.45-0.55, y 0.6-1.0
  (y 648 and below), and the target stays in view.
- **Fix:** lower the seal pose by about 80-110 px.

### 10. Kill feed details: small square portraits, the accent in the wrong place, a pixel shadow (M-L)
- **Where:** `feed1_a..c`, `feed2_c`, against `ref_queued_00` row 1.

  | | Ours | Overwatch |
  |---|---|---|
  | Killer portrait | 34 x 34 px | about 52 px wide (x 1606-1657), cropped hero art filling the row's height |
  | Accent stripe | a 3 px stripe (#5DA8C6) between the name box and the portrait (x 1677-1679) | a 4-6 px bright edge (#30B1DD on the ally box, #AA3F66 on the enemy's) at the inner end, after the portrait, next to the middle |
  | Victim side | the same, mirrored (#8A8296) | the same, mirrored |
  | Name text | Minecraft's hard 1 px drop shadow | no shadow |
  | Kunai glyph | an outlined sketch that reads as a scribble at 1x | solid white silhouettes (our fist and skull are solid, which is right) |

- **Unsure (L):** both of `ref_queued_00`'s rows (Winston's primary-fire kills) show only ">", with no glyph. The glyph
  rows in `ref1080` and `ref_queued_03` may be ability or specific-weapon kills. Check a video before keeping "always a
  glyph" for kunai kills.
- **Fix:**
  - Portraits about 50 x 34 px.
  - Move the stripe to the inner end, 4-5 px wide and brighter.
  - A soft shadow (or none) on the names.
  - A solid silhouette for the kunai glyph.

### 11. Suzu's 5 s ground ring is pale khaki, not gold (L-M)
- **Where:** `fx9_suzu_a..d`.
- **Overwatch:** decal 3684, (3.2, 2.17, 0.1) = #FFD732 bright: a hue of 49° at 80% saturation.
- **Ours:** a core of #CEC885 / #D1C87F with #AEA946 at its edges: a hue of 55° at 44-59% saturation. On grass it reads
  as an olive-cream line.
- **Fix:** blend #FFD732 at about 80% for the core, so it stays gold over grass.

### 12. No "ELIMINATED" notice on a kill (L, [K])
- **Where:** `hm6_kill`: only the larger red hit marker.
- **Overwatch [K]:** a final blow also shows a short "ELIMINATED <name>" line under the crosshair.
- **Fix:** if wanted, a 0.8-1 s caption under the volley bar: white caps, the victim's name (the mob's name for mobs).
  Check a video for its size and position first.

---

## Earlier items seen again, still open (not re-ranked)
- **R4 #4 (kunai throw hand), with new numbers:**
  - The hand no longer covers the crosshair. It now reaches up and left of it: x 430-800 with its top at y 550
    (`fx9_kunai_a`/`b`), and x 600-830, y 545-750 (`hm6_body`).
  - Overwatch's left hand ends just right of the crosshair, at x 960-1250, y 594-756 (BLZ-Kunai, §2). Ours is now
    about 350-450 px too far left.
- **R3 #11 (where Swift Step lands):** in `fx10_step_b`/`c` her torso is still inside the villager. In `st_c` and
  `hud7_cooldown1` the villager fills the screen.
- **R4 #14 (cooldown splash):** a round blob with drips behind "4" and "13" (`hud7_cooldown2`) and "6"
  (`hud7_cooldown1`), against `ref1080`'s ragged outline hugging "12".
- **R4 #12 (chat):**
  - Minecraft's pixel font starting at x 8, with the 4 px tag bar at x 0-3 (`hud7_rest`, `feed1_*`, `hm6_*`).
  - Overwatch: a smooth sans at about x 90.
- **R4 #6 (white frame on rows with you in them):** a 2 px #EEF0EB frame (x 1561-1885 in `feed1_b`). No reference row
  has one; kept by the owner until a video settles it.
- **R2 #24 (reload line):** the white line along the kunai panel's bottom line in `hud7_reload`.
- **R1 #27, #28:** the portrait is still a flat-bottomed octagon with no wings or badge, and the name row has no
  light-blue number box (`hud7_*`).
- **R1 #43 (options layout) (L):**
  - Ours: a separate HERO tab and one binding column (`opt_r2`, `opt_r4`).
  - Overwatch [K]: hero-specific settings sit under CONTROLS, behind a hero drop-down, with two binding columns per
    action.
  - Only matters if the owner wants Overwatch's exact layout. Hover descriptions work (`opt_hov1`, `opt_hov2`).

## Not checked
- **Healing received:** no frame shows a homing ofuda hitting. `fx9_ofuda_c` comes after it, so the gold flower (40C0)
  isn't judged.
- **Suzu:**
  - `fx9_suzu_a..d` come after the burst (E at 14 and 13), so the dome, flower and wave aren't judged.
  - No white glow shows on the first-person arms either, but these frames may be past its 0.65-1 s.
- **Swift Step's flash in the first-person cast:**
  - `hud7_cast`'s edge smoke is a brighter teal than `ref1080`'s: #1A868B in the corners, against #066664 to #0D4D4D.
  - The reference is a dark interior, so its reach can't be compared. Not ranked.
- **Kitsune Rush cast hands:**
  - `fx12_c2` (palms down, spread) and `fx10_fp_a` (thrust forward, backs of the hands to the camera) match BLZ's steps
    2 and 3.
  - The prayer seal (step 1) isn't in any frame.
- **Quick melee, low health, wall climb, enemy-view colours, a second Kiriko:** no capture.

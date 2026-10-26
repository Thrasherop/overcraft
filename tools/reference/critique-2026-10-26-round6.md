# Kiriko vs real Overwatch: picky visual critique, round 6 (2026-10-26)

This sixth pass covers only the captures taken after round 5:
- Thrown ofuda: `pj2_homing`, `pj2_homing3p`, `pj2_free`, `pj2_kunai`, `pj2_suzu`, `ofuda_now`, `rib_1..3`.
- Effects: `fx13_ofuda_a..e`, `fx13_kunai_a..c`, `fx13_suzu_a..d`, `fx13_step_a..c`, `fx13_rush_a..c` (third person),
  `fx13_fp_a`/`b` (Kitsune Rush cast in first person).
- Swift Step landing: `ss_beside1` (first person), `ss_beside2` (third person).
- Kill feed and final blow: `feed3_a..d`, `elim2`. Cooldown splash: `br2_a`, `br2_b`.
- Chat: `chat1` (not typing), `chat4` (typing). Death in hero mode: `death2_a..c`.
- Enemy view (second client, 1280x720): `run2/screenshots/en2_*`, `en3_rush*`.
- Minecraft mode's arm: `mcarm2_hand`, `mcarm2_swing`, `mcarm2_hero`, `mcarm_compare`.

The Overwatch frames are the same as before: `ref1080` (the owner's mid-Swift-Step frame), `ref_owner_02` and
`ref_queued_00..03`, all 1920x1080, in the session scratchpad. This round's crops are in its `critique\round6\` folder.
The other sources are `kiriko-reference.md` and `kiriko-effects.md`. This report is text only (repo rule).

**Method:**
- Crops at the same 1080p coordinates, zooms of 2-8x, Pillow pixel samples, row and column extents.
- Hue statistics over effect pixels, picked by colour: teal where B > R + 25 and G > R + 25; gold over grass where
  R - 0.8 G > 12 and B < 0.6 G (grass is about #556E34, so a gold tint shows as red above the grass's).
- Distances come from known sizes (a villager about 1.95 m, Kiriko about 1.7 m) with a focal length of about 771 px
  (Minecraft's 70° vertical FOV at 1080p), so metres are approximate.
- The reference frames are video stills: their edges run 1-2 px soft.
- The enemy-view captures are 1280x720, and their coordinates are in their own pixels.
- To judge the thrown ofuda "against the mask", the four `talisman_flight_0..3` frames were viewed from the local asset
  pack (`local-assets/pack`), after checking that Overwatch and Battle.net were not running.
  - What the mask is: a wavy paper strip about 1:3. One end is square and bright, and the other fades and frays.

**Confidence:**
- **H:** measured, or clearly seen in both.
- **M:** seen once, or partly inferred.
- **L:** general knowledge, or seen poorly.

**References to earlier rounds:** "R1 #n" to "R5 #n" are items from the earlier rounds. Items marked done, not ours or
left as is come back only where these captures show them still wrong:
- Items 1, 2, 4 and 5 below are R5 items marked done that still fail in part.
- Item 3 is R2 #7, which was marked fixed.

**Not counted, per the brief:**
- The flat test world, villagers, zombies, husks and pigs as stand-ins, Minecraft's sky and blocks.
- Dropped items (the porkchops and leather that fly through `rib_*`, `fx13_ofuda_*`, `fx13_suzu_*`, `elim2`), XP orbs,
  the burning zombie in `feed3_c`, and the dying husk's red tint and death poofs.
- Red command-feedback chat text, the "New Recipe" and "Advancement Made" toasts, and the second client's tutorial
  toast.
- These owner decisions:
  - A thrown ofuda is a glowing talisman silhouette (mask 3A1EE), gold when locked on and blue without a target, two
    per burst.
  - Swift Step lands at the ally's side.
  - Kunai are on the left mouse button and ofuda on the right.
  - G gives plain Minecraft with a Kiriko arm.
- Each effect is judged on its own, even where others overlap it.

## Status (kept up to date)
- **Done** (CHANGES.md 2026-10-26, played): 1 (it was the next pair near the camera: smaller talismans, near fades,
  ribbons from the talisman), 2 (blended blue-leaning teal, more orange gold), 3 (the breath clip's second broken key:
  reproduced and gone), 4 (no glow ball, stronger body glow), 5 (gates gone within 3 m), 6 (behind the death screen;
  while typing it stays Minecraft's on purpose), 7 (lighter mist, fainter smoke), 9 (shorter puff), 11 (enemy red),
  12 (blended petals), 13 (red glove).
- **Not changed:** 8 (the mask is Overwatch's own), 10 (Blizzard's first-person paper), 14 (the kunai now draws its
  model; at 90 m/s a frame shows little).
- **Open:** 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 (all of them; nothing has been acted on yet).

## Confirmed fixed (seen in these captures)
- **Swift Step lands beside the ally (R3 #11, the owner's correction):**
  - In `ss_beside2` the villager stands at her left, shoulder to shoulder (villager x 650-850, Kiriko x 900-1030).
  - In `ss_beside1` the first-person view ahead is clear.
- **Ally bar above the head (R5 #3, the position):**
  - `pj2_homing`: pips at y 375-382, head top at 408.
  - `ss_beside2`: pips at y 432-441, head top at 467.
  - `chat4`: a villager close on the left, pips at y 360-369 above the head's top edge at 378.
  - Nothing sits on a face any more. The chevron is in "Earlier items".
- **Ribbon ends in the ground (R5 #4):**
  - `rib_1..3`: the unaimed ofuda land as soft teal flowers, with no straight cut.
  - `fx13_ofuda_d` still shows the old cuts (a vertical edge at about x 790, y 540-650). It was taken at 08:22, before
    the fix (`rib_*` are from 08:42).
- **Swift Step arrival rings (R5 #7):** in `fx13_step_a` the two rings are faint and translucent, no longer neon hoops.
  They are still unbroken (no streak mask) and mint over grass (item 2).
- **Kunai glints (R5 #8):** no long teal band in `fx13_kunai_a..c` or `elim2`.
  - A wall hit shows 5-6 cyan streak sparks of 25-35 x 6-8 px (median #4EB5C4, hue 187°).
  - The two sparks over the husk in `elim2` are the body-hit sparks the owner kept.
- **Kill feed (R5 #10):**
  - Kiriko's face crop is about 50 px wide (`elim2`, x 1665-1714).
  - A bright 4 px edge (#30B1DD) sits at the portrait's inner end.
  - Names have no shadow.
  - The enemy fill is #8A364F against #8D2841-#8D2845 in `ref_queued_00`, and the ally fill #3081AE against
    #2B7CA4 there. Both are translucent over different backgrounds: close enough.
- **Suzu ground ring (R5 #11):** in `fx13_suzu_a` it is gold.
  - Median #B7A630 (53°, 72% saturation); core #D9C449 to #E8D86C (#FFD732 is 49°).
  - The remaining olive lean over grass belongs to item 2.
- **"ELIMINATED" caption (R5 #12):** in `elim2`, "ELIMINATED" in white and "HUSK" in red.
  - Caps 13 px tall, x 887-1033, y 732-744: 43 px under the volley bar, centred.
  - Its look isn't judged: no reference frame shows one.
- **Chat when not typing (R4 #12):** Futura at x 60 (`chat1`, `feed3_c`). `ref_queued_00` has it at about x 90.
- **Cooldown boxes (R4 #14 and the states):**
  - The digits get a black brush stroke with drips (`br2_a`, `br2_b`), close to `ref1080`'s "12".
    `ref_queued_00`'s "6" has only a thin 2-3 px outline, so Overwatch's splash varies too.
  - The ability icon shows dimly behind the number, as in `ref_queued_00`'s "6".
  - A ready ability's key label turns light with a dark letter, as `ref_queued_00`'s E does.
  - The orange fill tracks the cooldown: LSHIFT at 3 of 7 s fills 59% of the box (`fx13_rush_a`, y 934-972), where
    57% have elapsed.
- **Death in hero mode keeps her HUD and arms:** `death2_c`. The death screen's chat is item 6.
- **Enemy colours exist:** `en2_suzu` shows a thin red ground ring (#AE503C), and `en2_step` a red ring and wisps. Item 11
  covers the rest.

---

## Ranked list (most noticeable first)

### 1. Homing ofuda: the ribbons still read as two broad gold wings, detached from the talismans [R5 #1 marked done, the tails still fail] (H)
- **Where:**
  - `pj2_homing` (also `ofuda_now`, top): two gold talismans by a villager about 6 m away (its height on screen is
    about 252 px).
  - `fx13_ofuda_a`: just after the throw. Mask of the gold: `round6\pj2h_goldmask.png`.
- **Overwatch:**
  - Effect 7E2D is the talisman plus a ribbon (strip 0797, texture 30182). The ribbon is brightest at the head, so it
    starts on the paper.
  - In `ref1080`, near the ally the gold trails are thin streaks, 3-4 px wide and about 45 px long (box x 919-951,
    y 518-552). Their median is #E4B754 and their brightest #F2DB6B.
- **Ours:**
  - **The talismans:** gold crescents of about 25 x 60 px at x 902-930 and 993-1021, y 535-592, each in a soft glow
    about 45 px across.
  - **Where the ribbons start:** about 90 px behind each talisman, which is about 0.9 m at their depth. Nothing joins a
    talisman to its ribbon.
  - **Their front edges are straight cuts:**
    - Left ribbon: x 794-811 over y 585-715.
    - Right ribbon: x 1108-1125 over y 600-735.
  - **What they cover:** flat translucent gold sheets.
    - Left: x 450-811, y 555-776. Right: x 1108-1490, y 540-735.
    - 100-190 px tall per column (140 rows at x 760, 193 at x 1200), narrowing to 30-50 px at the outer ends.
  - **Why they look like wings:**
    - The ribbon runs back along the whole flight path, to within about 1 m of the camera (a 0.2 m strip is 140 px
      tall at 1.1 m).
    - The two ofuda leave with a wide sideways spread, so the near ends fan out to x 450 and x 1490.
  - `fx13_ofuda_a`: the near talismans are 80 x 120 px fins with soft gold fans spreading toward the screen sides.
- **Fix:**
  - Start the ribbon on the talisman (0.1 m or less), brightest there.
  - Limit it to about 0.5-1 m (`ref1080`'s streak is about 45 px), with alpha fading to 0 at the tail.
  - Keep it 0.07-0.1 m wide, and fade any ribbon segment within 2-3 m of the camera, as the gates do.
  - (L) Check the launch spread against a video: both ofuda leave the right hand.

### 2. Over grass, teal still reads mint and gold reads olive [R5 #2 marked done, still fails] (H measured)
- **Where:** every effect that sits on grass.
- **Overwatch:** Kiriko teal is #00BCB1 / #00FFF0 (176°), Kunai and Kitsune cyan #00FFFF (180°), seeking gold
  #DCB900 (50°; `ref1080` measures #E4B754, 45°), the Suzu mark #FFD732 (49°).
- **Ours:** the median of the effect pixels (R5's values for comparison):

  | Effect, capture | Median | Hue | R5 |
  |---|---|---|---|
  | Ofuda ground flowers, `rib_3` / `fx13_ofuda_d` | #3E9166 / #3E976E | 149° / 152° | 153° |
  | Unaimed ofuda ribbons, `fx13_ofuda_d` | #559964 | 133° | 135° |
  | Swift Step rings and arcs, `fx13_step_a` | #5ABD9A | 159° | 148° |
  | Swift Step petals on the grass, `ss_beside2` | #63B282 | 149° | 143° |
  | Rush path tiles, `fx13_rush_a` | #58D6AB-#5EE1B2 | 158-159° | 162° (clipped) |
  | Rush ground ring / speed lines, `fx13_rush_a` / `_b` | #57BB8F / #53B287-#55BE91 | 150° / 152-153° | 138° |
  | Fox glow ball, `fx13_fp_a` | #57BF96 | 154° | about 150° |
  | Gold ribbons, `pj2_homing` | #9E9B30-#A29C30 | 56-58° | |
  | Gold talismans near the camera, `fx13_ofuda_a` | #ADA024-#AFA125 | 54° | |
  | Throw puff, `rib_2` / `pj2_free` | #A69A19 / #888923 | 55° / 61° | |
  | Rush "+" sparkle over grass, `fx13_rush_a` | #AEB93B | 66° | |

  - Over the sky the same materials read right: the gate posts are #C3EBFF (200°).
  - The blueward ramps moved the Swift Step and rush colours 5-14° toward teal. The ofuda flowers and ribbons barely
    moved, and every teal is still 17-43° short of 176°.
  - Gold has the same problem in the other direction: added to green it turns olive and khaki.
- **Fix:**
  - Draw the faint body of each effect blended (alpha) instead of added, and keep additive only for the white-hot
    cores.
    - 60% of #00B5C8 over #556E34 gives (34, 153, 141), about 174°.
    - 60% of #E0A000 over #556E34 gives (168, 140, 21), about 49°.
  - Shift the faint ends further: teal toward #00B5C8, gold toward #E0A000.
  - Check on grass, stone and sky after each change.

### 3. Swift Step in third person: her leg kicks straight up over her head [R2 #7 "her leg no longer kicks", still fails] (H)
- **Where:** `fx13_step_b` (arrival, LSHIFT box coral). Zooms: `round6\stepb_top6.png`, `stepb_pose2.png`.
- **Overwatch:** the step is a hand seal, a vanish and a reappearance (3CC7/3CC5, R2 #7). No raised leg. (M on
  Overwatch.)
- **Ours:**
  - A sneaker sole (grey-white, with tread ovals) and the red trouser cuff are at x 975-1045, y 505-545. That is above
    her head, whose hair top is at about y 555.
  - The leg runs straight up from the hip beside her torso.
  - Only one sneaker is on the ground (x 905-965, y 845-880).
  - `fx13_step_a` and `_c` have both feet down, so it lasts a few frames.
- **Fix:**
  - Step through the third-person step clip frame by frame and find the key or blend that swings the right thigh to
    about 180°, as R2's breathing-clip key did.
  - Then check every pose that blends into the step.

### 4. Kitsune Rush fox: a matte sky-blue model under a separate mint glow ball [R5 #5 marked done, still wrong in part] (H ours, M Overwatch)
- **Where:** `fx13_fp_a` (zoom `fpa_fox2.png`), `fx13_rush_a` (zoom `rusha_fox4.png`).
- **Overwatch:**
  - The fox itself is the bright thing: translucent, glowing, crystalline ice-blue.
  - It has no separate halo.
- **Ours:**
  - **The body does not glow:**
    - In first person its median is #72C8E6 (195°), and its brightest #91DAFE is no brighter than the sky beside it
      (#8ACEFB).
    - It is lit like a solid model: the belly and fur cards shade down to #11436C.
  - **The glow is a separate mint ball:**
    - It is about 120 px across at x 900-1020, y 530-600, median #57BF96 (154°).
    - Its centre is about 30 px ahead of and above the fox's head (ball at y 575, head top at y 605).
    - In third person it is a 50 px ball at (960, 485) above a 16 x 36 px fox.
  - Two hues side by side: a blue fox under a green orb.
- **Fix:**
  - Draw the fox unlit (full-bright) with an additive cyan rim, so its brightest parts pass the sky.
  - Centre any glow on the body, no wider than the silhouette, in the fox's own hue, or drop it.

### 5. Kitsune Rush in first person: the first gate still frames the view [R5 #6 marked done, still visible] (H ours, M Overwatch)
- **Where:**
  - `fx13_fp_b`: posts at x 335-460 and 1465-1590, from the top of the screen to about y 880.
  - `fx13_fp_a`: posts at x 370-480 and 1430-1550.
- **Overwatch (BLZ-KitsuneRush):** after the cast the gates recede ahead, with mist at the screen edges. Nothing
  frames the view.
- **Ours:**
  - **`fx13_fp_b`:** each post is about 125 px wide and raises the background by 24-31 per channel.
    - Over the sky: #ACD5FF against #94BAFF.
    - Over the grass: #6D8C54 against #546D35.
    - Edge lines: #AEFAFF.
  - **`fx13_fp_a`:** the posts add 10-15.
  - **Distance:** that post width puts the gate about 3-4 m away, which is about halfway through the new 2-6 m fade, so
    a pale glass doorframe stands around the screen for the first second.
- **Fix:**
  - In the caster's first person, fade gates to 0 inside about 5 m, or skip the first gate.
  - Or fade fragments by their screen x, so nothing is drawn in the outer 25% while the gate is close.

### 6. Chat while typing, and on the death screen: Minecraft's pixel chat over the HUD (H ours, L Overwatch)
- **Where:** `chat4` (typing), `death2_a`/`b` (the death screen, which is also a Minecraft screen).
- **Overwatch [K]:**
  - While typing, chat keeps its sans in a translucent box at the left (`ref_queued_00` shows the lines at about
    x 90).
  - The input line sits under the messages.
- **Ours:**
  - **Font:** opening chat switches the lines from Futura (`chat1`) to Minecraft's pixel font at about twice the size
    ("CLICK ME to suggest": glyph rows 836-867, starting at x 16).
  - **Message band:** dark, x 0-1327, y 760-867, 69% of the screen width.
  - **Input box:** x 8-1911, y 1024-1071, across the portrait, the name, the ult "Q" and both mouse icons.
  - **Death screen (`death2_a`):** the same pixel chat, and its second line (about y 885-915) runs across the top of
    the portrait (top at about y 890).
- **Fix:**
  - Use the `hud_chat` face in the chat screen too.
  - Limit the band to the message width (about x 52-700).
  - Keep the input box in the same column above the health row (it ends at y 870).
  - Lift the death screen's chat the same way.

### 7. Kitsune Rush cast: the whole frame is graded mint (M)
- **Where:** `fx13_fp_a` (the fox just out), against `fx13_rush_a` and `pj2_kunai`.
- **Overwatch (BLZ-KitsuneRush):** a cyan spirit burst between the palms, then a white-cyan mist at the edges.
- **Ours:** the whole image is tinted, most strongly at the bottom centre:
  - Grass at the sides: #546E35 (87°) becomes #5F9060 (121°).
  - Grass at the bottom centre: becomes #6BAF8F (152°).
  - Sky: #8AB1F9 becomes #8ACEFB.
  - In `fx13_fp_b` it eases off: grass #5F7E4E (99°).
- **Fix:**
  - Keep the burst local, between the palms and on the fox.
  - Make the screen effect an edge mist (white-cyan, the outer 15-20% of the screen) instead of a full-frame grade.

### 8. Thrown talismans read as flames and blades, not as paper strips (M)
- **Where:**
  - `pj2_homing3p` (zoom `pj2h3p_4.png`): 4 gold talismans by the villager.
  - `rib_2` (zoom `rib2_blue3.png`): 2 blue talismans.
  - `fx13_ofuda_a`: near the camera.
- **Overwatch (mask 3A1EE):** a wavy strip about 1:3 with one square bright end, fluttering through 4 frames.
- **Ours:**
  - In third person each gold talisman is a jagged yellow flame about 15 x 30 px, like a small lightning bolt.
  - The blue ones in `rib_2` are V-shaped shards of 60 x 100 px: a bright white crease (#D6FFFF) with a soft cyan fan
    (median #50FBE3-#5CFFF7) opening to one side.
  - Near the camera the gold ones are curved fins with a knife-sharp bright edge.
  - The square end never shows, and the soft glow (40-50 px) blurs the outline.
- **Fix:**
  - Draw the mask on a velocity-aligned quad of about 0.25 x 0.75 m, square end leading.
  - Keep any glow behind it within about 1.2 times the strip.
  - If two quads cross to make the V, use one, or make the second much fainter.

### 9. The ofuda throw puff: an olive smudge left behind in the world (M)
- **Where:**
  - `rib_2` (x 1097-1153, y 621-678), `rib_3`.
  - `pj2_free` (x 1125-1181, y 707-773).
  - `fx13_ofuda_a`, `_d`.
  - It shows in unaimed throws as well.
- **Overwatch (BLZ-Ofuda, effect 2074):** a soft golden puff pops at the flicking fingertips and goes at once.
- **Ours:**
  - **Size and colour:** a blob 55-65 px across, #A69A19 to #888923 (55-62°): olive over grass.
  - **Where it stays:** after the hand has gone back to its pose. In `rib_2`, `rib_3` and `pj2_free` it is 400-500 px
    from the only visible hand, the fan hand on the left.
- **Fix:**
  - Attach it to the hand (view-model space) in first person.
  - Give it a life of about 0.2-0.3 s, in a gold that survives grass (item 2).

### 10. The paper in the flicking hand is a blank grey card (M)
- **Where:** `fx13_ofuda_e` (zoom `ofe_hand3.png`): a card of about 60 x 55 px between the fingers, at x 790-850,
  y 618-680.
- **Overwatch:** the paper is white with the red seal and black kanji (atlas 31E3B), the same as the idle fan.
- **Ours:**
  - Flat mid-grey (#9B9A9A-#A1A1A0) with no seal or kanji. It reads as a small grey plastic block.
  - The idle fan's paper is #BFBBBA-#D8D8D8, with its print.
- **Fix:**
  - Texture that card with the paper (its back face too), unlit or lightly lit, so it stays near #E8E4DE.

### 11. Enemy view: the gates are almost invisible, the tiles are brick-brown, the fox stays pale blue (M)
- **Where:** `en3_rush`, `en3_rush2` (1280x720, from the second client).
- **Overwatch:** enemies see the same effects in the enemy ramp: #893F3F / #BC5959 / #FF7E7E (about 0°), gates
  included.
- **Ours:**
  - **Gate posts (`en3_rush2`, x 300-380):** +3 to +5 over the sky (#8FB4FB against #8AB3FF) and +5 to +15 over
    grass. Only their outlines show.
  - **Path tiles:** median #894D2D (20°, 66% saturation): brick-brown over grass.
  - **The fox (`en3_rush`, zoom `en3_fox5.png`):** a pale blue-white body; only its wake is red.
- **Fix:**
  - Give the enemy gates a red-white rim at the strength of the allies' cyan one.
  - Pull the tiles' faint end toward #C04050 so the sum over grass lands near 0-10°.
  - Tint the fox's body red for enemies.

### 12. Swift Step petals: flat, saturated cyan ovals (M-L)
- **Where:**
  - `fx13_step_a`/`_b` (zoom `petals8.png`): petals of about 10 x 16 px.
  - `ss_beside1`: near the camera at x 865-942, y 1025-1079, about 35 x 24 px (zoom `ssb1_petals6.png`).
- **Overwatch:** petal cards (0D06 with sprite 3267E), teal-white with a pale core. They tumble, so they flash and
  thin as they turn.
- **Ours:**
  - One flat fill (#82F8EF, 175°) with a hard edge and a 2 px notch at one end.
  - No core or shading, and no change as they spin. They read as cyan confetti.
- **Fix:**
  - Use the sprite's own falloff (a white-teal core to a teal edge).
  - Scale the width by the cosine of the spin, so they flicker like cards.

### 13. Minecraft mode's arm: the glove is charcoal; Kiriko's is red (M-H)
- **Where:** `mcarm2_hand`, `mcarm2_swing` (zoom `mcarm2_z2.png`).
- **Kiriko** (our own hero arm, `fx13_ofuda_b`): a red glove back (#862636) with black knuckle pads, a black
  wristband and a red forearm guard.
- **Ours:**
  - Skin fingers with finger lines (#DDA483), a thin red trim, then a 100 px charcoal band (#2F2F33, the glove), a
    darker band (#1C1C20, the wristband), then the red sleeve (#731924).
  - It's clean and sharp, but the glove reads black, so at a glance it's "red sleeve, black glove", not Kiriko.
- **Fix:**
  - Make the glove band red (#8A2433) with a black knuckle strip along its top edge.
  - Keep the black wristband below it and the red guard.

### 14. Kunai in flight: two 4 px dots, no blade and no streak (L-M)
- **Where:** `pj2_kunai` (zoom `pj2k_kunai8.png`).
- **Overwatch (3BF2):** the 0.28 m blade, a short cyan tube trail (about 1-1.5 m) and a lens glint of 0.42-0.46 m.
- **Ours:**
  - Two cyan dots of about 3 x 4 px, 10 px apart, at x 1089-1104, y 512-515 (#B1F4FF-#C3FFFF). There is nothing
    between them.
  - The kunai may be flying almost straight away from the camera (L), but even end-on a 1 m trail should give a
    short streak.
- **Fix:** check a frame from the side. Keep a minimum on-screen length for the trail (about 20 px) and draw the blade.

---

## Earlier items seen again, still open or kept (not re-ranked)
- **R4 #4 (the kunai throw's hand):**
  - `fx13_kunai_a`: the hand is at x 605-860, y 545-760, left of the crosshair.
  - Overwatch's is at x 960-1250, y 594-756.
  - Not changed: needs frame-matched footage.
- **R5 #3, the chevron:** the position is fixed (above), but the chevron is still 10 x 7 px with 2 px strokes
  (`pj2_homing` x 955-964, y 388-394). R5 asked for about 14 x 8 px with 3 px strokes.
- **R5 #7:** the arrival rings are soft now, but still unbroken circles: no streak or noise mask.
- **R4 #6 (the white frame on rows with you in them):**
  - A 2 px white frame in `feed3_c` (row 2) and `elim2`.
  - Kept by the owner until a video settles it.
- **R5 #10, the kunai glyph:** still an outlined sketch at 1x (`elim2`, x 1722-1745). Kept by the owner.
- **R1 #27, #28:** a flat-bottomed octagon portrait with no wings or badge, and no light-blue number box.
- **R1 #37 / R3 (the Suzu bell in flight):**
  - `pj2_suzu`: a matte tan sphere with a red tassel and white paper streamers, by the thumb.
  - It is the game's own model; its matte look against a brass bell isn't judged.
- **Kunai body-hit sparks:** two cyan streaks over the husk in `elim2` (x 925-975, y 465-500). Kept by the owner
  (R5 #8).

## Not checked
- **Healing received:** no frame shows a homing ofuda hitting. `fx13_ofuda_b` comes after it.
- **Suzu:** `fx13_suzu_a..d` come after the burst (E at 14-11), so the dome, the flower, the wave and the allies' white
  glow aren't judged.
- **Swift Step's first-person cast (R5 #9):** no cast frame in this set. `ss_beside1` is after the landing.
- **"ELIMINATED":** Overwatch's own final-blow caption (size, font, place) has no reference frame. Only that ours is
  present and centred is checked.
- **Enemy-view ofuda and kunai in flight:**
  - `en2_ofuda` shows only a small red puff.
  - `en2_kunai` shows nothing in flight.
  - `en2_rush` and `en2_rush2` show an empty field.
- **Death screen:** Overwatch's death cam isn't comparable to Minecraft's respawn screen. Only the chat on top of it
  is judged (item 6).
- **Not captured:** quick melee, wall climb, low health, night, water, and a second Kiriko on the same team.

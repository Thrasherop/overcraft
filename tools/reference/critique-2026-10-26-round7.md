# Kiriko vs real Overwatch: picky visual critique, round 7 (2026-10-26)

This seventh pass covers only the captures taken after round 6:
- Flat test world: `hud8_*` (rest, cast, cooldown1/2, ready, reload, rushing, spent, wall); `fx16_ofuda_a..e`,
  `fx16_kunai_a..c`, `fx16_suzu_a..d`, `fx16_step_a..c`, `fx16_rush_a..c` (third person), `fx16_fp_a..c` (Kitsune Rush
  in first person); `hm8_body/head/kill` (kunai at night); `feed4_a..d` (kill feed).
- Minecraft mode's arm: `mcarm3_day`, `mcarm3_night`.
- A normal-terrain survival world: `ppd3_*` (the tamed wolf as the ally), `ppn2_*` (a night fight on Normal),
  `pp_death_sheet`, `pp_select`, `pp_join`, `pp_inv`. The brief's `pp_inv_c` is not in the folder.

The Overwatch frames are the same as before: `ref1080` (the owner's mid-Swift-Step frame), `ref_owner_02` and
`ref_queued_00..03`, all 1920x1080, in the session scratchpad. This round's crops are in its `critique\round7\` folder.
The other sources are `kiriko-reference.md` and `kiriko-effects.md`. This report is text only (repo rule).

**Method:**
- **Two sizes of capture.** The flat-world and Minecraft-mode captures are 854 x 480 (Minecraft's default window); the
  survival-world ones are 1920 x 1080.
  - The small ones were upscaled 2.25x for the side-by-side crops. Their coordinates below are given in 1080p
    equivalents (x 2.248).
  - They carry 2.25 times less detail: 1-2 px strokes, the talisman silhouettes and petal shading can't be judged in
    them.
- Crops at 2-8x, Pillow pixel samples, row and column extents, connected components for small sparks.
- Hue statistics over effect pixels, picked by colour: teal where B > R + 25 and G > R + 25; gold where R - 0.8 G > 12
  and B < 0.6 G.
- Distances from known sizes (a villager about 1.95 m; a block's 16 texels per metre on a wall) with a focal length of
  about 771 px at 1080p (343 px at 480p), so metres are approximate.
- The reference frames are video stills: their edges run 1-2 px soft.
- Two behaviours were explained by reading the code (nothing changed): the Swift Step box's states (`Kiriko.java`,
  `HeroHud.ability`) and the ally pips (`HeroHud.allyMarker`). No Overwatch files were opened this round.

**Confidence:**
- **H:** measured, or clearly seen in both.
- **M:** seen once, or partly inferred.
- **L:** general knowledge, or seen poorly.

**References to earlier rounds:** "R1 #n" to "R6 #n" are items from the earlier rounds. Items marked done, not ours or
left as is come back only where these captures show them still wrong:
- Items 1, 4, 5 and 9 below are R6 items marked done that still fail in part.
- Item 6 re-raises R6 #10 ("not changed: Blizzard's first-person paper"): the grey now shows on the select screen and in
  the Suzu throw, and it is the paper's shading, not the model, that is wrong.
- Item 8 is R1 #27 / #28, done today, which still differ in part.

**Not counted, per the brief:**
- The flat test world, villagers, zombies, pigs and the wolf as stand-ins; Minecraft's sky, blocks, night lighting.
- Red command-feedback chat, the "Advancement Made" toast (it hides the kill feed in `hm8_kill` and `feed4_a`), dropped
  items, burning mobs, the dying zombie's death poofs (purple at night in `hm8_kill`).
- A stale "ELIMINATED ..." caption after the first kill (a bug fixed since).
- Also not counted:
  - The white volley bar in `hud8_cast`: the 0.25 s refill flash (`HeroHud`, `ofudaRefilledAt`), Overwatch's own.
  - White and grey square pixel particles round the healed villager (`fx16_ofuda_a..c`): the mod spawns no vanilla
    particles, so they are Minecraft's own.
  - Rush gates and path tiles from an earlier cast standing in `fx16_ofuda_*`: each effect is judged on its own.
  - The 854 x 480 chat's blocky Futura, Minecraft mode's hotbar, hearts and small ult meter, and "KIRIKO" on the name
    plate (it is the test player's name).
- These owner decisions:
  - A thrown ofuda is a glowing talisman silhouette, gold when locked on and blue without a target, two per burst.
  - Swift Step lands at the ally's side.
  - Kunai are on the left mouse button and ofuda on the right.
  - G gives plain Minecraft with a Kiriko-styled arm.

## Status (kept up to date)
- **Done, played at 1080p** (each reproduced on the old build first; captures `r7a`-`r7n` in `run/screenshots`):
  - **1:**
    - The unaimed pair is blended: 166° median in flight (`r7f_level_0.1`), against 119°.
    - The "+" sparkles are blended gold: 52-58°, against 71-74° (`r7c`/`r7i_tp*`).
    - The path tiles read 170-181°, against 150-156° (`r7e_fp060`, `r7e_tp025`).
    - Gold's faint end is brighter and more orange. The cores read 44° (`r7j_ofuda_gold`); faint edges over grass
      still drift to about 56-58°, which partial alpha over green does to any gold.
  - **2:** the 5 s ring lies on the ground in pieces, each on the surface under it, none in the air
    (`r7l_suzu_*` before, `r7n_suzu_*`/`r7o_*` after).
  - **3:** every empty pip is its own slot (dark, lighter rim). A bar that would cross a screen side isn't drawn:
    at 48.5° the villager's bar spans x 8-115, at 49.5° it's gone, both sides (`r7k_edge_*`).
  - **4:** in her own first-person view the cast is a small cyan burst between the palms, not the smoke cloud round
    the camera. Under the hands the grass stays #546E35 (87°, as with no cast; it was 128°), and the sky 218°
    (`r7e_fp020`).
  - **5:**
    - The glow ball in each gate is gone (it was the gate's rising flash).
    - The fox is added light: median #E7FFFF over grass, brightest #FFFFFF (`r7i_fp020`).
    - Its blue body had been drawn over its own glow, hiding it. The earlier "white fox" frames were the ball behind it.
  - **7:** the LSHIFT box is coral from the key press through the seal (`r7k_s30`). The step's screen flash now
    comes with the step (0.4 s, `r7k_s40`), not at the press.
  - **8:**
    - The frame is a tall hexagon: sides from about 932 to 990, gable peak near 900, point near 1015.
    - The mask's ears and hair break over its top edges.
    - The party box is 30 x 44 with a thin pale "1" (`r7f_portrait`).
  - **9:** in first person the puff follows the flicking hand's fingertip bone, drawn where the arms are. Frozen
    mid-throw it sits at the talismans' top edge, by the fingers (`r7j_puff_frozen`).
  - **10:** at night the edge mist is cyan, 185-190° at 37-45% saturation (`r7j_night_rush2`), against 196° at
    16%.
- **Investigated, left as is: 6.** The paper is two layers (368 triangles on the front, 208 on the back). Its
  back is textured with Blizzard's own baked shade, #CDCCCB fading to #756664 at the bottom (atlas 31E3B), so it isn't
  a back face lit the wrong way round. A two-sided-lighting try changed nothing on it and was taken out.
- **Owner's call: 11** (Minecraft's blob shadow under heroes): in HANDOFF's questions.

## Confirmed fixed (seen in these captures)
- **Gold "wings" on homing ofuda (R6 #1):**
  - `fx16_ofuda_a` (zoom `ofa_center5.png`): a villager about 9 m away (164 px tall). The gold is two soft patches of
    about 60 x 58 px (x 812-870, y 522-578 and x 1048-1102, y 508-567), about 1.4 m either side of it, plus two small
    ones about 0.55 m out.
  - Nothing near the camera fans out toward the screen sides.
  - The talisman shape inside the glow can't be judged at 854 px.
- **Teal over grass (R6 #2, the teal half):**

  | Effect, capture | Median | Hue | R6 |
  |---|---|---|---|
  | Swift Step slash arcs, `fx16_step_a` | #77D4BA (bright parts #8DE6D9) | 171° (176°) | 159° |
  | Swift Step petals, `fx16_step_b` / `_c` | #93E5DC / #81C4B2 | 174° / 168° | 149° |
  | Ofuda ground flowers, `fx16_ofuda_d`, `hud8_spent` | #2F7F71-#34816E | 162-170° | 149-152° |
  | Kunai terrain sparks, `ppd3_kunai` | #229687-#339394 | 172-180° | 187° |

  What is still short of 176° is in item 1.
- **Her leg kicking up on Swift Step (R6 #3):**
  - Both feet are down in `fx16_step_a..c` (zooms `stepa3.png`, `stepb3.png`), `ppd3_step3p` and `ppd3_rush3p`.
  - Confidence M: the kick was one tick in 64, so four frames prove little. CHANGES.md's 66-tick step-through is the
    real test.
- **The first gate in first person (R6 #5):**
  - The near posts now raise the sky by 6-14 in G, against R6's 24-31: at x 303-438 and 1472-1619 in `fx16_fp_c`, and
    at x 80-285 in `ppd3_hud` (edge lines #B0EAFE-#BAE7FE).
  - They read as faint glass columns, no longer as a doorframe.
- **Death screen (R6 #6):** in `pp_death_sheet` nothing crosses the portrait. No chat line is on screen, so its font
  isn't checked.
- **Minecraft mode's glove (R6 #13):** `mcarm3_day` (zoom `mcarm_pair.png`), from the fingertips back:
  - skin fingers #E0A684;
  - a black knuckle strip #1F1F23;
  - the red glove #881E2B;
  - a black wristband #1D1E21;
  - the red sleeve #751A24 (#57111B on its shaded side).

  It reads as Kiriko's glove.
- **Swift Step lands beside the ally (R3 #11):**
  - `fx16_step_a..c`: the villager at her left, shoulder to shoulder (villager x 652-847, Kiriko x 899-1027).
  - `ppd3_rush3p`: the wolf beside her on a ledge.
- **Ally bar above the head (R5 #3):** `ppd3_rush3p` has the pips at y 626-633 over the wolf's ears, and `ppd3_ofuda`
  and `ppn2_*` have them above the target. The close-camera case is in item 3.
- **Kill feed:**
  - `feed4_a..c` (zoom `feed_pair.png`) lay rows out as Overwatch does: `NAME [face] glyph > [face] NAME`.
  - The zombie's box is enemy red and Kiriko's ally blue. Her own death (`feed4_c`) reads `ZOMBIE [face] fist >
    [face] KIRIKO`.
- **"ELIMINATED ZOMBIE":** centred under the volley bar in `feed4_a` (x 877-1045, y about 735), the name in red.
- **Night:**
  - The arms stay readable (`hm8_*`, `ppn2_*`).
  - A head hit shows the red marker (`hm8_head`, zoom `hm8_center.png`).
- **Ult meter:**
  - Ready in `hud8_ready` (zoom `ready_ult6.png`): white disc, black fox, blue ring with halo, triangles at 3 and 9,
    inverted Q.
  - `0` with no ticks after the cast (`ppd3_hud`, `hud8_rushing`), and ticks while charging (`ult_pair.png` against
    `ref1080`'s 59 and `ref_queued_02`'s 17).
- **Cooldown fill:** `ppd3_step` shows LSHIFT "7" with a sliver of orange and E "11" about 25% filled (21% of Suzu's
  14 s gone).

---

## Ranked list (most noticeable first)

### 1. Over grass, the near blue talismans read green and every faint gold reads olive [R6 #2 marked done: the teal half is fixed, these still fail] (H)
- **Where:**
  - `fx16_ofuda_d` (zoom `ofd_fins4.png`): an unaimed throw. Its next pair, still near the camera, sits either side of
    the crosshair at x 674-830 and 1101-1230, y 517-672.
  - `fx16_ofuda_a`: the gold pair at the villager.
  - Also `fx16_suzu_a` (zoom `suzua_ring2.png`), `ppd3_ofuda` (zoom `ppd3of_puff4.png`), `ppn2_ult`, `hud8_rushing`,
    `fx16_rush_a`/`b` and `fx16_fp_b`.
- **Overwatch:** Kiriko teal #00BCB1 / #00FFF0 (176°). Seeking gold #DCB900 (50°; `ref1080` measures #E4B754, 45°). The
  Suzu mark #FFD732 (49°).
- **Ours:**

  | Effect, capture | Median | Hue |
  |---|---|---|
  | Unaimed talismans near the camera, `fx16_ofuda_d` | #5A8E59 / #548C58 (brightest #7DBC86) | 119° |
  | Gold talismans' glow at the villager, `fx16_ofuda_a` | #807E2A / #7E7E2B | 59-60° (R6: 56-58°) |
  | Suzu ground ring, faint edge, `fx16_suzu_a` | #707D2D | 67° |
  | Ofuda throw puff over leaves, `ppd3_ofuda` | #5A500F | 52°, dark khaki |
  | Rush "+" sparkles at night, `ppn2_ult` | #675F0D | 55° |
  | Rush path tiles and ground ring, `hud8_rushing`, `fx16_rush_a`/`b`, `fx16_fp_b` | #488C7E, #3B836A-#3C856E, #418E7F | 158-164° (R6: 158-159°) |

  - **The cores are right:** gold #E1B327-#EBC955 (45-46°), the Suzu ring's core #C3B03A (52°), and the ring over a
    tree trunk #9B8528 (47°, item 2).
  - **The worst is the near blue pair:** after every unaimed throw, two pale green leaves about 155 px across sit either
    side of the crosshair. Per CHANGES.md the talismans are added teal, and round 6's fix blended only the faint
    effects.
  - The path tiles barely moved since round 6.
- **Fix:**
  - Draw the teal talisman silhouettes blended, or blended under a small additive core, with the faint end at #00B5C8.
    60% of it over #556E34 lands at about 174°.
  - Move gold's faint end toward #E0A000 (60% over grass is about 49°) for the glow, the throw puff, the sparkles and
    the Suzu ring's soft edge.
  - Move the path tiles' faint end another 10-15° toward blue.
  - Check on grass, leaves, stone and night.

### 2. Protection Suzu's 5 s ring floats as a flat circle through hilly ground (H ours, M Overwatch)
- **Where:** `ppd3_suzu` (zoom `suzu_ring3.png`): Suzu thrown down a wooded slope.
- **Overwatch:** the 5 s mark is two ring decals (3684, effect 6089 in `kiriko-effects.md`). A decal is projected onto
  the surface under it, so the gold circle lies on the ground and follows it. Confidence M: no capture shows one on a
  slope.
- **Ours:**
  - A gold line 4-6 px thick runs straight from (2, 300) to (385, 208). It hangs in the air across the leaves and
    crosses a tree trunk at about half its height (x 190-385). Then it vanishes into the grass block in front.
  - It reads as a gold wire strung through the trees.
  - On the flat test world (`fx16_suzu_*`) it lies right; in any hilly survival world it won't.
- **Fix:**
  - Lay the ring on the ground: for each segment, find the top surface below it (within about ±2 blocks of the impact
    height) and draw the segment 0.02 m above it. Or project the ring onto the depth buffer, as a decal.
  - Fade out any segment with no ground within reach, instead of drawing it in the air.

### 3. Ally health bars: missing health is one flat dark slab, and long bars run off the screen (H ours, L Overwatch)
- **Where:** the tamed wolf (40 health: 500 Overwatch HP, 20 pips):
  - `ppd3_step3p` (zoom `step3p_bar6.png`);
  - `ppd3_rush3p` (zoom `rush3p_bar5.png`);
  - `ppn2_ult`, `ppn2_r11` (zoom `r11_bar5.png`).
- **Overwatch:** the bar is a row of separate pips. `ref_queued_02` has 10 at an 11 px pitch, each with its own dark
  edge. Confidence L: lost health shows as empty slots, still separate; no reference frame shows a hurt ally up close.
- **Ours:**
  - **The slab** (`ppd3_step3p`): 10 white pips at x 202-310, y 490-499, then one continuous band to x 420. Over the
    sky's #81ABFE it is #475E8B (45% black), with no gaps.
    - The cause: each empty pip's backing is 11 px wide at an 11 px pitch, so it touches the next
      (`HeroHud.allyMarker`).
    - Over the sky it reads as a blue-grey slab as long as the pips; over leaves (`ppd3_rush3p`, x 765-880) as a
      smudge.
  - **The chevron** is centred on the whole 218 px bar. With half the bar empty, it sits at the end of the white
    (x 306-316 in `ppd3_step3p`; about x 765 in `ppd3_rush3p`), not under the pips.
  - **Off the screen:** `ppn2_ult`'s full 20-pip bar runs off the right edge (x 1717-1920, y 181-187), cut mid-pip.
- **Fix:**
  - Draw each empty pip as its own slot: a 9 x 8 box at about 45% black with a 1 px lighter edge, keeping the 2 px
    gaps.
  - Hide a bar that would cross a side of the screen, as one off the top already is.

### 4. Kitsune Rush cast in first person: the frame still turns mint [R6 #7 marked done: the sides improved, the centre didn't] (H)
- **Where:** `fx16_fp_a` (the prayer seal, before the fox is out), against `fx16_rush_a` (the same field with no grade).
  Pair: `fpa_grade_pair_half.png`.
- **Overwatch (BLZ-KitsuneRush):** a cyan burst between the palms, then a white-cyan mist at the edges.
- **Ours:**

  | Area | `fx16_fp_a` | No grade (`fx16_rush_a`) | R6 (`fx13_fp_a`) |
  |---|---|---|---|
  | Grass at the sides (x 45-270, y 674-809) | #63845B, 108° | #546D34, 87° | 121° |
  | Grass below the hands (x 742-854, y 742-854) | #3E8163, 153° | #536C34, 87° | 152° |
  | Grass mid-left (x 562-742, y 517-584) | #477C55, 136° | #567037, 87° | |
  | Sky (x 854-1079, y 135-202) | #7DBEFB, 209° | #87B2FF, 218° | #8ACEFB |

  - It has eased by `fx16_fp_b` (sides 93°).
- **Fix:** as in R6:
  - Keep the burst local, between the palms.
  - No full-frame grade: a white-cyan mist only in the outer 15-20% of the screen.

### 5. The fox is still a small matte-blue figure, and a glow ball hangs in the first gate [R6 #4 marked done, still fails in part] (H ours, M Overwatch)
- **Where:** `fx16_fp_b` (zoom `fpb_fox8.png`), `fx16_rush_a` (zoom `rusha_center4.png`), `fx16_rush_b` (zoom
  `rushb_c6.png`).
- **Overwatch:**
  - The fox itself is the bright thing: a translucent, glowing, crystalline ice-blue spirit with no separate halo.
  - The cast's burst is at the palms.
- **Ours:**
  - **First person (`fx16_fp_b`):** the fox is about 25 x 54 px (x 946-971, y 544-598). Its median is #68B7E2 (201°),
    and its brightest pixel #8ED8FB is no brighter than the sky (#87B3FF-#9BC3FF). It doesn't glow.
  - **Third person (`fx16_rush_b`):** a 14 x 25 px blue animal (#559DC2) running between the gates.
  - **The glow ball (`fx16_rush_a`):**
    - A white-cyan ball about 133 x 122 px (x 899-1032, y 303-425; median #B2F3FF, core #D6FFFF) hangs in the first
      gate's opening, 2-3 m up.
    - It is about 150 px above the fox, which is just over Kiriko's head (x 949-971, y 510-540).
    - `fx16_rush_b` has a smaller one at the far end of the gates.
    - It reads as a sun in the gate: the bright thing is not the fox.
- **Fix:**
  - Draw the fox full-bright, with an additive cyan-white rim pushed past 1.0, so its brightest pixels pass the sky
    (aim for about #D8FFFF).
  - Find what draws the ball. If it is the gates' appearance flash, keep it under 0.5 s and 1 m. If it is the cast
    burst, put it at the palms.

### 6. Paper talismans go grey on their back faces [R6 #10 "not changed" (Blizzard's first-person paper); raised again: the select screen shows it, and it is the shading, not the model] (M)
- **Where:** `pp_select` (zoom `sel_ofuda4.png`), `fx16_suzu_b` (zoom `suzub_hand6.png`).
- **Overwatch:** the talisman paper is white with the red seal and black kanji (weapon atlas 31E3B). It is thin paper,
  light on both sides.
- **Ours:**
  - **`pp_select`:** in the hand she raises beside her face, the rear talisman is a dark grey slab (#463D3D-#494747,
    x 1055-1110, y 225-320). The front one is #CDCCCC with its seal. Only about 45 x 60 px of each shows, so the pair
    reads as two small grey-and-white blocks, centre stage, on every join.
  - **`fx16_suzu_b`:** the card in the right hand coming back from the throw is #857070-#8F8988 (x 629-663, y 636-674),
    its seal faintly pink.
  - R6 measured the flicking hand's card at #9B9A9A-#A1A1A0 (`fx13_ofuda_e`).
- **Fix:** light the paper from both sides (flip the normal on back faces) or draw it unlit, so it stays near #E8E4DE
  whichever side faces the camera.

### 7. Swift Step: the LSHIFT box stays white ("ready") through the hand seal (H)
- **Where:** `hud8_cast` (zooms `cast_br4.png`, `cast_center4.png`). The seal is up, fingertips at about y 582. The
  LSHIFT box is white #F0F0F0 (x 1279-1344, y 906-971), its label filled as when ready.
- **Overwatch:**
  - `ref1080` is the owner's mid-Swift-Step frame, and its LSHIFT box is coral.
  - `kiriko-reference.md` §1.3: coral is "pressed / casting (just activated, cooldown not started yet)".
- **Ours:**
  - The cooldown starts when the 0.2 s seal ends (`Kiriko.finishSwiftStep`), and the HUD draws coral only for the
    cooldown's first 0.35 s (`HeroHud.ability`, `CAST_TIME`).
  - So the box is white while her hands are up, and turns coral once she has landed (`fx16_step_a`).
- **Fix:** send the cast state (`castTicks >= 0`) to the HUD. Draw the coral box from the key press, through the seal,
  until 0.35 s into the cooldown.

### 8. HUD portrait: a flat-topped badge where Overwatch's frame is a tall hexagon, and a dark digit in a short party box [R1 #27 / #28 done today, still differ in part] (M frame, H box)
- **Where:** `ppd3_hud`, `pp_join` (zooms `portrait4.png`, `partybox_pair.png`), against `ref_queued_00` / `02` / `03`
  and `ref1080`.
- **Overwatch:**
  - **Frame:**
    - Vertical blue sides at x 55-153, from y about 932 down to about 990.
    - From there the top edges rise about 37° toward the middle (x 58: y 931-934; x 64: y 929; x 70: y 925), behind the
      head.
    - The head breaks out over the top, the hair reaching about y 880-890.
    - The bottom ends in a point behind the level badge.
  - **Party box:**
    - x 192-222, y 970-1014 (29-31 x 44-45 px), #35CCFC-#44D4FD.
    - Its "1" is thin and paler than the box (#96ECFF-#C3E6FF), at x 205-209, y 983-998.
    - In `ref_queued_00` / `02` a glowing tail runs down its left edge to y 1023-1028.
- **Ours:**
  - **Frame:**
    - A flat top at y 892 (x 64-143) with 45° corners.
    - Vertical sides at x 54-153, from y 898 to 958, then a 48 px V down to y 1006.
    - The sides start 34 px higher than Overwatch's and the V about 30 px higher. It reads as a badge pointing down;
      Overwatch's reads as a tall gem.
    - Only the ponytail breaks the top (to about y 868); the mask's ears stay inside.
  - **Party box:** 24 x 38 (x 196-219, y 969-1006), with a bold dark navy "1" (#0B2A3A), 5 x 13 px.
- **Fix:**
  - **Frame:** vertical sides from about y 932 to about 990, gabled top edges rising about 37° to a peak near y 900
    (under the head), and the bottom point near y 1015. Let the hair and the mask's ears overlap the top edge.
  - **Party box:** 30 x 44 at x 192, with a thin pale digit (#A9E5FF at about 70%).

### 9. Ofuda throw puff: still olive, and not always at the fingertips [R6 #9 marked done (shorter)] (M)
- **Where:** `ppd3_ofuda` (zoom `ppd3of_puff4.png`), `fx16_ofuda_d` (zoom `ofd_fins4.png`).
- **Overwatch (BLZ-Ofuda, effect 2074):** a soft golden puff pops at the flicking fingertips and goes at once.
- **Ours:**
  - **`ppd3_ofuda`:**
    - A smudge of 55 x 47 px (x 1162-1216, y 648-694), #5A500F: khaki-brown over the leaves.
    - Its centre is about 140 px right of and 35 px below the index fingertip (about 1050, 637). The hand moved on and
      the puff stayed in the world.
  - **`fx16_ofuda_d`:** at the fingertips this time (x 1079-1117, y 600-645), but olive.
- **Fix:**
  - In first person, attach it to the fingertip in view-model space.
  - Draw it blended gold (item 1).

### 10. Kitsune Rush at night: the edge mist reads as grey fog (M)
- **Where:** `ppn2_ult`, `ppn2_r9`, `ppn2_r10` (crop `ppn2ult_corner.png`).
- **Overwatch:** a white-cyan misty vignette at the edges (`kiriko-reference.md` §2).
- **Ours:**
  - The corners are #4C575B (196°, 16% saturation) and the sides #2E3335-#2E3737.
  - The night scene is #040605-#0B0808 outside the rush (`ppn2_r14`), and #110C0F at this frame's centre.
  - The top-right corner is a pale grey haze about 400 px across.
  - By day (`hud8_rushing`) the same mist barely shows.
- **Fix:** give the mist more colour, about #7FE6F5 at its edge. Added at 35% over black that is about 185° at 57%
  saturation, so a dark scene gets a cyan haze, not grey fog.

### 11. Third person: Minecraft's round blob shadow under Kiriko (L)
- **Where:** `ppd3_rush3p` (zoom `rush3p_shadows2.png`): a dark ellipse of about 140 x 60 px under her (x 885-1025,
  y 770-830). Also under her in `fx16_step_a..c` and `fx16_rush_b`.
- **Overwatch [K]:** no blob. Heroes cast shadows shaped like their bodies.
- **Ours:** Minecraft's entity shadow, the same disc as under the wolf. Its round edge shows clearly round her shoes.
- **Fix:** for hero models, set the shadow radius to 0, or shrink it to the feet (about 0.3 m) and make it fainter. It is
  Minecraft's look, so this is the owner's call.

---

## Earlier items seen again, still open or kept (not re-ranked)
- **R4 #4 (the kunai throw's hand), not changed:** frame timing decides it.
  - `fx16_kunai_a` and `hm8_head`: the hand is at x 885-1245, y 607-753. That is Overwatch's spot (x 960-1250,
    y 594-756).
  - `ppd3_kunai`, after the hit: x 510-770, y 555-700, left of the crosshair.
  - Still needs frame-matched footage.
- **R5 #9 (the seal's height), not changed:** in `hud8_cast` the fingertips reach about y 582, touching the bottom of the
  target's LSHIFT label (y 559-578).
- **R5 #8 (kunai sparks), the size kept:**
  - In `ppd3_kunai` the dirt wall is about 1.2 m away (its texels are about 40 px).
  - The sparks are lens shapes of 55-65 x 16-25 px, about 0.1 m (for example x 1062-1095, y 304-358 and x 1163-1217,
    y 502-518), scattered over x 1040-1220, y 300-790.
  - They have cyan rims and grey middles, and read as cyan seeds. That is still about twice the data's 0.05-0.06 m.
- **R5 #3 (the chevron):** still about 11 x 7 px with 2 px strokes (`ppd3_rush3p`).
- **R6 #8 (the talisman's shape), not changed:** the gold pair in `fx16_ofuda_a`/`b` is two soft flames of about 60 px.
  The silhouette can't be judged at 854 px.
- **R6 #12 (petals):** near the camera at night (`ppn2_ult`) they are flat pale-cyan teardrops of 25 x 40 px (x 990-1015,
  y 205-245 and x 880-910, y 735-765) and read as water drops (L).
- **R6 #14 (kunai in flight), not changed:** `fx16_kunai_b` shows two tiny cyan dots near the crosshair.
- **R4 #6 (the white frame on rows with you in them):** `feed4_c` row 2 (`KIRIKO > PIG`). Kept by the owner.
- **R2 #24 (the reload line):** the white line along the ofuda panel's bottom line in `fx16_ofuda_e`.
- **R1 #35 (the ready-again flash):** `fx16_ofuda_b`'s cyan E box keeps a black icon.
- **R2 #13 (the select pose), marked done, the pose part still differs (L):**
  - `pp_select` holds the talismans at face height (the hand at y about 230-480, the head's top at about 180), with the
    index finger up.
  - Overwatch's idle (§3) holds the ofuda overhead.
  - The inventory preview (`pp_inv`) does use the overhead idle.

## Not checked
- **Thrown talismans in flight:**
  - Gold up close: only the 854 px frames, with the pair at 9 m.
  - Blue away from the camera:
    - `fx16_ofuda_d` and `hud8_spent` show the landing flowers, flat five-petal shapes on the grass
      (`spent_blobs6.png`), not the flight.
    - `ppd3_ofuda`'s pair is blue because the wolf was inside leaves: no line of sight, so no lock. Swift Step's diamond
      still showed, and Overwatch's Swift Step also targets through walls.
- **Healing received:** `fx16_ofuda_c` shows only a small gold flower on the villager's chest, at 854 px.
- **Suzu:**
  - `fx16_suzu_*` come after the burst (E at 14-12).
  - `ppn2_suzu` shows the cleanse's white arm and no burst.
- **`hm8_kill`:**
  - It shows neither "ELIMINATED" nor the kill marker, although the zombie's death poof is out: at least 1 s after the
    kill, within the caption's 1.6 s.
  - The `hm8_*` frames (11:34) predate the caption fix in CHANGES.md, whose own captures (`el_*`) are from 11:38.
    Re-shoot them on the fixed build.
- **Third-person ofuda:** in `ppd3_ofuda3p` the leaves push the camera against her head.
- **`pp_inv_c`:** not in the folder. `pp_inv` is Minecraft mode's inventory; only the hero item in slot 1 was looked at.
- **Not captured:** the enemy view, a second player, quick melee, wall climb (`hud8_wall` shows her standing at a wall),
  water, the Nether.
- **Death screen:** Overwatch's death cam isn't comparable to Minecraft's respawn screen.
- **The HUD's other elements:** the volley bar, panels, ammo, ability boxes and ult agree with earlier rounds. Only the
  bottom-right pair (`br_pair.png`) and the ult pair (`ult_pair.png`) were looked at again.

# Kiriko vs real Overwatch: picky visual critique, round 8 (2026-10-27)

This eighth pass is the first with crisp Overwatch game captures, so it is mostly about the HUD and the first-person
arms at rest, measured against them.

**Captures:**
- **Ours** (1920 x 1080, a flat test world, daylight), in the session scratchpad's `critique\round8\ours\`:
  - `r8_rest` (ult 31%, level 1), `r8_ult_ready`, `r8_perk_ready` (perk level 2 waiting, ult 47%), `r8_perk_menu`
    (Left Alt held).
  - `r8_ally_close` (a villager 5 m ahead), `r8_cooldown_suzu`, `r8_cooldown_both`.
  - `r8_reload` (kunai reloading), `r8_ofuda_volley`, `r8_chat`.
  - `r8_ofuda_volley` shows `0 | 10`, every volley segment spent and the reload line starting, so it was judged as the
    start of the ofuda reload, not mid-volley.
- **Overwatch, new:** the owner's own Overwatch 2 screenshots `owner_2` .. `owner_6`.
  - Sizes: 2000 x 1125, compared at 1080p (x 0.96).
  - `owner_4` is a 2000 x 1175 crop with its left edge cut. It was matched on the pips: x(1080p) = 0.917 x + 82,
    y = 0.917 y (pips then land within 1 px of `owner_2`'s).
  - Normalised copies are in `critique\round8\ow1080\`.
  - Skins: `owner_2/3` wear a blue skin; `owner_4/5/6` the default red outfit. All five have the same blue weapon
    skin (glowing plumed talismans, a curved kunai with a gold fox guard).
  - Binds: the owner's Swift Step is LCONTROL and his ult key is 9; kunai is on the left button and ofuda on the right,
    as ours.
- **Overwatch, older** (1920 x 1080 video stills, as in earlier rounds): `ref1080`, `ref_owner_02`,
  `ref_queued_00`..`03` (Aspen's stream).

**Method:**
- Pillow pixel samples, row and column runs on colour thresholds, bounding boxes, radial profiles round the ult meter
  (centre 960, 930), and medians over regions.
- Crops at 2-10x, in `critique\round8\crops\` (named under each item).
- Coordinates are 1080p pixels.
- Code was read to explain three things (nothing changed): `HeroHud.health`, `HudText.draw`'s shadow, and the font
  definitions in `tools/build_asset_pack.py`. No Overwatch files were opened.

**Confidence:**
- **H:** measured, or clearly seen in both.
- **M:** seen once, or partly inferred.
- **L:** general knowledge, or seen poorly.

**References to earlier rounds:** "R1 #n" to "R7 #n" are items from earlier rounds. Several were measured on Aspen's
video and are now contradicted by the owner's crisp frames:
- R1 #46 (kunai hand placement, "references disagree") is settled by item 1.
- R7 #8's party box (30 x 44), measured on Aspen's frame with a rank emblem, is item 3.
- R3 #14's 4 px panel line is item 6.
- R1 #24 (the mouse), marked done, still differs (item 8).

Where Aspen's frames and the owner's disagree, the owner's are followed: they are his own HUD, with no rank cosmetics,
which is what Minecraft has (R2 #22/#23).

**Not counted:**
- The flat world, the villager, Minecraft's sky and the red command-feedback line.
- Minecraft's white square particles: the 30 px squares near the crosshair in `r8_rest` (x 855-895, y 520-580, and
  in the sky at x 895-1030, y 185-215), and the white clusters on the horizon.
- The look of thrown talismans (owner's decision).
- The "PERK READY [LALT]" tab in `r8_perk_ready`, `r8_perk_menu`, `r8_ally_close`, `r8_cooldown_*`, `r8_reload`,
  `r8_ofuda_volley` and `r8_chat`: the orange "[LALT] SELECT PERK" row is being implemented (per the brief;
  `HeroHud.selectPerkBar` is already in the code).
- **Skin- or setup-specific in the owner's frames, not ours to copy:**
  - The teal curls either side of the ult meter. They sit in the same place, symmetric about x 960, in all five
    frames and in none of Aspen's, so they belong to the weapon skin.
  - The faint dotted ring at about r 70 round the ult meter (seen with the curls only).
  - The glowing blue weapons.
  - The voice-chat icons at the right edge, the FPS counter, and LCONTROL / 9.
  - The green crosshair: a custom setting (see "Owner's call").

## Status (kept up to date)
- **Done, 2026-10-27** (captures `r8after_rest`, `r8after_chat`; side by side with `o2`/`o4`: `r8a_bl`, `r8a_br`,
  `r8a_ult`, `r8a_chat` in the scratchpad):
  - **2:** the block starts at x 168; the pips are a fixed row, pitch 248/n, width pitch - 4.
  - **3:** the 31 x 30 #3AD5FE level box with a white digit, a uniform 28% plate to the pips' end, the name #C9CFD6;
    the level bar has no track (the fill and its glow only, a dot at zero).
  - **4:** at full health a white rounded "+" square and a dark tail; hurt, all dark.
  - **5, in part:** hero chat in BlizzardGlobal-Regular (rendered against `owner_2`'s line first: it matches), its
    capitals Overwatch's 15 px, no shadow, a 22 px line pitch, the newest line at about y 650, the owner's orange
    kept; it wraps at x 425 in that face (lines split as they're added, the history split again on a mode switch):
    "Too many messages! Please wait a few / seconds..." as in `owner_2` (`chatwrap2`). Not yet: the channel icon
    (our text starts at x 63, Overwatch's at 91 after it), Minecraft's one-space indent on wrapped lines,
    "Name (Hero): text" for plain chat (callouts already read so).
  - **6:** no fill; 2.5 px #4DA8F0 lines with a ~3 px glow and inward hooks at the ends; a 9 px gap (the ofuda panel
    8 px right); the kunai icon 5 px further in and 5 px wider.
  - **7, in part:** no offset shadow (light text gets a soft even edge, dark text none); lit key labels #D6D9DD,
    rounded, #343639 text. Not yet: the jagged glyph edges (the font atlas sampling).
  - **8:** the mouse redrawn: 19 x 27, a squarer top, a #111 outline, the button divider, an outlined wheel pill, the
    bound button filled.
  - **9:** the inner ring at r 33, 3 px, #E0703F at 50%, two 8° gaps turning once in 10 s; the number #E6E8EE with a
    1.5 px #1C2233 outline; the lit ticks at 72%.
  - **10:** the border's core #22D0FE with a soft #1B82CA glow outside and a #0270BB fade inside, no black edge; the
    frame 6% larger (point near y 1015, less gable beside the mask).
  - **11:** segments at 82% and 10% smaller; the bracket a 6 px #C8D0E0 dot, a 1.5 px line from 5 px out to dotY + 19,
    then 11 px flat.
  - **12:** icons #2A2A2C at 90%; the underline 2 px, 7 px clear of the box.
- **Open: 1** (the hands): the owner's frames (weapon skin) and Blizzard's 2022 capture disagree by ~150 px; asked the
  owner for a screenshot with the default weapons before moving them.
- **Owner's call:** a crosshair colour option (he plays with a custom green one).

## Confirmed matching (against the owner's frames)
- **Ability boxes:**
  - **Boxes:** x 1279-1342 and 1357-1419 in both. Swift Step is 64 px tall in Overwatch (y 904-967 at x 1335),
    ours 65 (907-971).
  - **Fill:** the gradient matches: Overwatch #F7F7F8 → #DBDBDA, ours #EEEFEE → #D6D7D7.
  - **Tilt:** about 3 px across a box in both.
  - **Labels:** under the box at y 990-1014 in both. The E box is 21-22 x 20-22 in both.
- **Ammo text:**

  | Panel | Overwatch | Ours |
  |---|---|---|
  | Ofuda `10\|10` | x 1516-1579, y 947-966 | x 1514-1576, y 948-966 |
  | Kunai | x 1775-1837, y 966-984 | x 1775-1835, y 966-985 |
- **Panel lines and mice:** the line heights match (top line at y 923-925 at x 1500 in Overwatch, 922-925 ours). The
  mice are centred at (1565, 1007) under the ofuda and (1823, 1026) under the kunai, in both.
- **Ult meter:**
  - The number is 34 px tall in Overwatch (y 913-946 in `owner_2`, `owner_3`, `ref1080`; `owner_5`'s 47 at
    914-947), ours 36 (912-947). Visually it looked bigger, so it was measured: it isn't.
  - Ticks: lit from 12 o'clock clockwise, r 38-48 in Overwatch, ours 37-47.
  - Outer thin ring: r 54 in both.
  - Key box: y 994-1015 in Overwatch, ours 998-1018.
  - Ready state (`r8_ult_ready`, zoom `ultready4.png`): white disc, black fox, blue ring with halo, inward triangles at
    3 and 9, Q inverted. No new Overwatch frame of it, so only checked against R3.
- **Volley bar:** its box is x 889-1033, y 662-687 in Overwatch (`owner_2/3`, `ref1080` alike), ours x 887-1032,
  y 662-689. The rest is item 11.
- **Health readout sizes:**
  - Current `250`: 48 x 23 in Overwatch, ours 46 x 22.
  - Maximum: 39 x 17 in both.
  - Pips: 30 px tall in both, rising 15 px across the row in both (3.6-3.9°).
  - What's off is where they are (item 2).
- **Crosshair:** our white "+" (2 px ticks, 7 px long, a 4 px gap, dark outline, zoom `xhair10.png`) matches Aspen's
  default one to the pixel.

---

## Ranked list (most noticeable first)

### 1. First person at rest: both hands sit too close to the middle [settles R1 #46] (H that ours differs from the owner's five frames; M on the exact target)
- **Where:** every hero-mode frame: `r8_rest`, `r8_perk_ready`, `r8_ult_ready`, `r8_ally_close`, `r8_cooldown_*`.
  - Zooms: `fp_pair_half.png` (whole frames side by side), `fp_ofudahand.png`, `fp_kunaihand.png`, `fp_o4.png`,
    `fingertip_grid.png`, `kunai_grid.png` (gridded every 20 and 50 px).
- **Overwatch:**
  - **The owner's frames** (`owner_2/3/4/6`; `owner_5` agrees), all the crossed-arms idle:
    - The talisman hand's index fingertip is at x 480-540, y 450-560.
    - The kunai is gripped at the guard at about (1410-1430, 775-875). The kunai forearm crosses the bottom from about
      x 900-1000 at the screen's foot, and the hand sits behind the ability boxes.
    - The hands are about 900 px apart and frame the view: one at about 0.27 W, the other at about 0.74 W.
  - **Blizzard's 2022 capture with the default weapons** (`kiriko-reference.md` §2):
    - The talisman fan spans x 288-672, y 410-670 (384 x 260 px).
    - The kunai hand is at (1267, 929), the blade tip at (1651, 724): a 28° rise.
    - It agrees in direction: the kunai hand right of ours and lower, the fan reaching further left.
- **Ours (`r8_rest`):**
  - **Talisman hand:** index fingertip at about (625, 540). The fan spans x 400-690, y 455-640 (290 x 185, about 25%
    smaller than Blizzard's), the glove x 595-780, y 625-800.
  - **Kunai hand:** gripped at about (1200, 850), the glove at x 930-1180, y 780-1010. The tip is at (1535, 715): a
    21° rise.
  - The two hands are about 575 px apart, bunched round the ult meter.
  - The glove sizes are about the same as Overwatch's (150-185 px for the talisman glove, about 250 px for the kunai
    glove). So it is the arms' placement that differs, not the field of view: a field-of-view change would move both
    hands outward in proportion and enlarge them.
- **Fix:**
  - **Kunai hand:** move it about 150-220 px right and 0-60 px down, the grip toward (1300-1420, 870-920). Tilt the
    blade to about 28°, so it points toward the right edge at about y 700-720.
  - **Talisman hand:** move it about 100 px left, the fingertip near (520, 520). Check the fan's on-screen size
    against Blizzard's 384 px.
  - **How:** the hands move in opposite directions, so a single camera offset can't do it. Check the camera bone and
    the idle clip/frame the view model uses before nudging each arm.
  - One owner screenshot with the default weapons would pin the exact spot (the weapon skin could, in principle, bring
    its own idle).

### 2. Bottom left: the whole health block sits 26 px right of Overwatch's, and the pip row runs 44 px long (H)
- **Where:** `r8_rest` and every frame. Zooms `bl_pair.png` (2x, gridded), `hp_num4.png`.
- **Overwatch (`owner_2/3/4/5/6`, identical to the pixel):**
  - The "+" box starts at x 166-170, the current `250` spans x 222-269 (y 892-914), the divider is at x 276-280, and
    the maximum is at x 286-324.
  - **Pips:** 10 at x 171-415, each 21 x 30 at a 24.8 px pitch.
  - **Aspen's 225-health frames:** 9 pips, 23 px wide at a 27.3 pitch, spanning 196-437 (241 px). Her block starts
    25 px further right, where her rank wings sit.
  - So the row is about 242-244 px long for both 225 and 250 health: the pips narrow as maximum health grows (H on
    the two measurements, M on the rule).
- **Ours:**
  - The "+" box starts at x 194, the `250` spans x 250-295 (y 896-917), the divider is at x 304 and the maximum at
    x 314-350.
  - Pips: x 196-459, 24 wide at a 26.6 pitch (`HeroHud.health`: `left = 194`, `26.6f`, width 24).
  - The sizes match (see "Confirmed matching"). Only the positions and the pip width are off.
- **Fix:**
  - **Position:** `left` 194 → 168, moving the "+" box, the numbers, the pips and the name row together. Raise the
    numbers' baseline 3 px (y 914 for the current value).
  - **Pips:** a fixed row of 244 px, with pitch = 248 / n and width = pitch - 4. At 10 pips that is 20.8 / 24.8, the
    row ending at about x 415.

### 3. Name row: a tall party box and a short, fading dark plate where Overwatch has a 31 x 30 box and a full-width translucent slab; a track line Overwatch doesn't draw [R7 #8's box, measured on Aspen's frame] (H)
- **Where:** `r8_rest` (zooms `namerow3.png`, `levelbar5.png`).
- **Overwatch (owner's):**
  - **Party box:** x 166-196, y 971-1000 (31 x 30), #3AD5FE, with a medium-weight white "1" (about #E8F2FA, clearly
    readable).
  - **Plate:**
    - A uniform translucent dark slab from the box's right edge to the pips' right end (x 196-418), about 32 px tall,
      tilted with the curve: y 968-1000 at x 220, 956-987 at x 400.
    - It darkens what's behind by about 25%: #393F6B over #4D568C in `owner_4`; #687C8E over #8CA5B9 in `owner_2`.
    - The name is in light grey #C9CFD6, about 15 px caps, starting about 14 px into the plate.
  - **Level bar at no progress:** no track line at all. A glowing cyan dot sits at the box's bottom-left corner (about
    x 168, y 1000-1012), with a short bright edge along the box's foot.
  - **With progress** (Aspen's `ref_owner_02`): a blue line runs along the plate's foot to a bright diamond.
- **Ours:**
  - **Party box:** x 192-221, y 970-1013 (30 x 44), with a thin "1" at 70% that barely reads.
  - **Plate:** a dark plate (56% black at its left) fading out over about 120 px (x 226-345), with the name in pure
    white.
  - A faint white track line (`0x33FFFFFF`) runs under it to x 345, plus the dot.
- **Fix:**
  - **Party box:** 31 x 30 at x 166, y 971 (after item 2's shift). Draw the digit white at about 90% in the body face.
  - **Plate:** #000000 at about 28% from x 197 to the last pip's right edge, 32 px tall, no fade. Name in #C9CFD6.
  - **Level bar:** drop the track. Draw only the filled part and its glowing end. At zero progress, only the dot at the
    box's bottom-left corner.

### 4. Health "+" box: Overwatch's turns into a white square with a dark "+" at full health; ours is always dark (H seen, M on the rule)
- **Where:** `r8_rest` (zooms `plusbox6.png`, `plusbox_q.png`).
- **Overwatch:**
  - **Full health:** `owner_2/3/4` at 250/250, `ref_queued_00` and `_01` at 225/225.
    - The left part is a white rounded square (x 166-197, about 26 x 26) with a dark "+" (#2A2A2A).
    - A dark translucent rounded tail follows to about x 212.
  - **Damaged:** `ref1080` at 199, `ref_queued_03` at 173, `ref_queued_02` at 27. The whole box is dark with a white
    "+"; at 27 there is also a magenta dot in the tail.
  - Every full-health frame is white and every damaged one dark. Whether regeneration under full health also lights it
    isn't seen.
- **Ours:** a dark box (`SHADE`, 45% black) with a white "+" at 250/250, which Overwatch shows only when damaged.
- **Fix:** at full health, draw a 26 x 26 #F4F4F4 rounded square (radius 3) with a #222 "+", then the dark tail.
  Below full health, keep today's dark box.

### 5. Chat: Minecraft's hard shadow on bold Futura, no wrapping, 40 px low (H look; M on the font's identity)
- **Where:** `r8_chat` (zooms `chat3.png`, `chat6.png`), against `owner_2` and `ref_queued_00` (`chat_q0.png`).
- **Overwatch:**
  - **Font:** a regular-weight neo-grotesque, not the HUD's Futura: double-storey "a", single-storey "g", straight
    "y", smooth edges. It has a faint dark edge and no offset shadow.
  - **Size:** caps about 15 px, x-height about 11.
  - **Line spacing:** about 22 px between lines, about 4 px more between messages.
  - **Channel icon:** a small team icon (the three-heads glyph) at x 60-80. The text starts at x 91.
  - **Wrapping:** at about x 425: "...Please wait a few" / "seconds..." in `owner_2`.
  - **Position:** the newest line's baseline is at about y 657 in both `owner_2` and `ref_queued_00`.
  - **Format:** a player line reads `AceOFSpades (Winston): Enemy Sombra!` (name, hero in brackets, colon;
    `ref_queued_00`).
  - **Colour:** system lines take the channel's colour (team #66D2F6 in `owner_2`).
- **Ours:**
  - **Font:** Futura demibold, orange #F7A54A. The owner asked for orange: kept.
  - **Shadow:** each glyph carries a hard dark-brown copy 2-3 px down-right (#3D2912: Minecraft's text shadow), with
    jagged glyph edges at 6x.
  - **Size:** caps 16 px. Line pitch 19 px, no gap between messages.
  - **No wrapping:** one line runs to x 490.
  - **Position:** the bottom line's baseline is at about y 697.
  - **Format:** `[Kiriko] Group up at the gate`.
- **Fix:**
  - **Font:** a regular grotesque for hero-mode chat. The extracted BlizzardGlobal-Regular (`fonts.md`) is the likeliest
    match for its Latin letters: compare it with `owner_2`'s "Too many messages!" before using it.
  - **Shadow:** none. If the text needs help over bright skies, use a 1 px dark edge at about 35%.
  - **Layout:** wrap at x 425, line pitch about 22, 4 px between messages, the last baseline at about y 657.
  - **Icon:** a small icon in the line's colour at x 60, the text from x 91.
  - **Format (M, seen once):** `NAME (Hero): text`.

### 6. Weapon panels: lines twice as thick and darker, square ends, and a dark fill that boxes each panel [R3 #14 measured on video] (H)
- **Where:** `r8_rest` (zooms `br_pair.png`, `lineends5.png`).
- **Overwatch:**
  - **Line:**
    - The core is 2-3 px, brighter and lighter than #2D96DA: #49A5EC-#52ADF8 over a dark scene (`owner_2`),
      #6CB9FA-#76B7F9 over a light one (`owner_3`).
    - A soft glow falls off over about 3 px each side.
  - **Ends:** every line ends in a small rounded hook turning toward the panel's inside, about 4 px long with a 3 px
    radius. The top lines bend down and the bottom lines up, so each panel reads as a pair of long brackets.
  - **Gap:** about 9 px between the two panels.
  - **No fill:** inside a panel the background is unchanged (`owner_3`: #8DA7C9 inside, #89A3C6 above).
  - **Icons:** the ofuda card is at x 1454-1489 and the kunai icon at x 1643-1744.
- **Ours:**
  - **Line:** a 4 px flat #2D96DA core (y 922-925 at x 1500, 935-938 at x 1700) with a 3 px halo. It reads heavier
    and darker.
  - **Ends:** square, with a 16 px gap between the panels.
  - **Fill:** a 15% black fill (`0x26000000`) makes each panel a visible darker box over grass: #405328 inside against
    #546E35 around it.
  - **Icons:** the ofuda card is 8 px left of Overwatch's (x 1446-1481), and the kunai icon is 5 px narrower and ends
    10 px short (x 1638-1734).
- **Fix:**
  - **Line:** a 2.5 px core in #4DA8F0, the glow kept at about 3 px.
  - **Ends:** a 4 px rounded hook toward the inside at each end, and a 9 px gap between panels.
  - **Fill:** drop it.
  - **Icons:** move the ofuda card 8 px right. Draw the kunai icon 102 px wide, ending at x 1744.

### 7. Small HUD text: the offset shadow doubles dark letters, glyph edges are jagged, and labels are pure white/black with square corners (H seen; M on the cause)
- **Where:** key labels and the name (zoom `text8.png`), the ult number (zoom `ult_num6.png`), the ult's key box
  (`ult31_pair.png`).
- **Overwatch:**
  - **Labels:** fill #D3D7DB (slightly see-through), text #343639 in a regular weight, rounded corners (about 3 px).
    The ult's key box is rounded too.
  - **Edges:** all small text is smooth, with no offset shadow.
- **Ours:**
  - **Shadow:** `HudText.draw` lays a 40% black copy 0.8 px right and 1.1 px down under every string.
    - On the white labels the black letters get a doubled right and bottom edge (`LSHIFT`).
    - On white text it reads as a hard 1 px drop shadow: `KIRIKO`, and the ult number's right side (#3D1119 beside
      #FFFFFF).
  - **Edges:** jagged (stair steps visible at 8x). The likely cause is the font atlas sampled nearest while scaled down
    1.5-2x: `hud` is size 32 at oversample 1.5, drawn at 14-19 px caps.
  - **Labels:** fill #E2E4E0, text #111111 bold, square corners.
- **Fix:**
  - **Shadow:** drop it for dark text on light fills. For light text, draw a soft even edge (1 px all round, about
    30%) instead of the offset copy.
  - **Edges:** rasterise the fonts at about the size they're drawn at 1080p (`hud`: oversample so size x oversample is
    about 28-30 px per em), or draw the HUD text with a linear-filtered render type.
  - **Labels:** fill #D6D9DD at 92%, text #343639, 3 px rounded corners.

### 8. Mouse icons: a soft egg instead of Overwatch's crisp outlined mouse [R1 #24 marked done, still differs] (H)
- **Where:** under both weapon panels, zoom `mice8.png` (8x).
- **Overwatch:**
  - **Size:** 19 x 27 px.
  - **Outline:** a black outline about 1.5-2 px.
  - **Shape:** the top is squarish with rounded corners. A black line separates the button row from the body.
  - **Wheel:** a white pill outlined black between the buttons.
  - **Bound button:** fills its whole button area orange (#E8632A).
  - **Body:** white, round at the bottom.
- **Ours:** 21 x 32 px, an egg shape with a faint outline. The orange quarter has no dividing line and the wheel is a
  dark slot. It reads as a soft blob next to Overwatch's icon.
- **Fix:** redraw the mask at 19 x 27:
  - a squarer top;
  - a 1.5 px #111 outline, a 1.5 px button divider and a 4 x 9 outlined wheel pill;
  - the bound button filled orange up to the divider.

### 9. Ult meter: the inner ring is a faint brown hairline; the number has a one-sided shadow where Overwatch outlines it [R1 #26 in part] (M-H)
- **Where:** `r8_rest` at 31% against `owner_4` at 31% (`ult31_pair.png`); `r8_perk_ready` at 47% against `owner_5`
  at 47% (`ult47_pair.png`); every Overwatch meter in `ult_all.png`; the number in `ult_num6.png`.
- **Overwatch:**
  - **Inner ring:**
    - An orange line about 3 px wide at r 33: #8F5F51 over #45414C in `owner_2`, #874F39 over dark in `owner_4`,
      about #E07040 at 50%.
    - Every owner frame shows two short gaps (about 8° each) opposite each other, at a different angle in each frame:
      `owner_2` about 2 and 8 o'clock, `owner_4` about 10 and 4, `owner_5` about 1 and 7. It probably turns slowly
      (M).
  - **Number:**
    - Fill not pure white: #D3D6E1 in `owner_4`, #D5D7D1 in `ref1080` (about 88% white).
    - An even dark outline about 1.5 px all round (#2D3350 beside the stroke).
  - **Ticks:** see-through, about 60%: #E49A83 over a white wall in `owner_4`, where #F18449 would stay #F18449.
- **Ours:**
  - **Inner ring:** `RING_THIN` at r 35, #7A3A2A at 70%, about 1 px. Over the glove it vanishes (#5D1F24 on #5C1A25);
    over grass it reads as #564C29.
  - **Number:** pure white with a dark copy on the right only (item 7).
  - **Ticks:** opaque.
- **Fix:**
  - **Inner ring:** r 33, 3 px, #E0703F at 50%, with two 8° gaps 180° apart, turning slowly (one turn in about 10 s;
    ours).
  - **Number:** fill #E6E8EE, with a 1.5 px #1C2233 outline at about 70%.
  - **Ticks:** lit ticks at about 70% opacity.

### 10. Portrait frame: a black edge where Overwatch's glows, a paler core, and the gable shows above the head's sides [R7 #8 done; details] (M)
- **Where:** `r8_rest` (zoom `portrait4.png`, 3x, against `owner_2`, `owner_6`, `owner_4`).
- **Overwatch:**
  - **Border:** the core is 3-4 px of saturated cyan (#15D2FF-#2CC5FF, #57DFFF over light), with a blue glow fading
    over about 5 px on both sides. Outside: #1B82CA → #1D3E79 → the background. Inside: #0270BB → #063E89 → the dark
    interior.
  - **Top:** the vertical sides start at about y 933. The head covers the top almost at once: only a few pixels of the
    gable show beside the mask's ears.
  - **Point:** the bottom point is at about y 1015.
- **Ours:**
  - **Border:** a 4 px #68CCFF core with a hard 2 px black edge outside (#29361A over grass, the `0x80000000` shadow)
    and no glow.
  - **Top:** the gable shows from y 904 to 928 on both sides of the mask.
  - **Point:** about y 1008.
- **Fix:**
  - **Border:** #22D0FE for the core. Swap the black shadow for a soft #1B82CA glow fading over 5 px outside, and add a
    3 px #0270BB fade inside.
  - **Top:** start the sides at about y 932, or enlarge the bust about 8%, so that only a few pixels of gable show.
  - **Point:** lower it to about y 1015.

### 11. Volley bar: opaque, slightly fat segments, and the bracket's drop is short [R4 #13's family] (M; L on visibility)
- **Where:** `r8_rest` (zooms `volley5.png`, `center3.png`, `bracket8.png`).
- **Overwatch:**
  - **Segments:** see-through, about 80%. Tracer's legs show through the third segment in `owner_4`, and the medians
    shift with the background: #EFBB80 over the hangar in `owner_2`, #DFAB2F over dark in `ref1080`.
  - **Size:** each segment is 25 px tall and 20 px wide on its middle row (y 675), with 9 px gaps.
  - **Bracket:**
    - The end dot is about 6 px across (#C8D0E0).
    - The line starts about 5 px from the dot, runs 14 px at 45° down to y 701, then 11 px flat.
    - It is about 1.5 px wide.
- **Ours:**
  - **Segments:** opaque #E6AA27, 28 px tall and 23 px wide, with 7 px gaps.
  - **Bracket:**
    - The dot is 8 px (#9A9A9A).
    - The line leaves the dot itself, runs 12 px at 45° to y 693, then 24 px flat.
- **Fix:**
  - **Segments:** amber at 82%, drawn about 10% smaller (25 x 20 at the middle row), keeping the 29 px pitch.
  - **Bracket:** a 6 px #C8D0E0 dot. Start the line 5 px out, run it to dotY + 19, then 11 px flat, 1.5 px wide.

### 12. Ability boxes: pure-black icons and a tight underline (L)
- **Where:** `abil4.png` (4x).
- **Overwatch:**
  - **Icons:** dark grey and slightly see-through: #2A2A2C median, and `owner_2`'s Suzu icon picks up the blue weapon
    behind it.
  - **Underline:** 2 px, 6-8 px under the box (Suzu box bottom at y 969, line at 977-978).
- **Ours:**
  - **Icons:** #111111, opaque.
  - **Underline:** 3 px, 3-4 px under the box (y 977-979 under a bottom at 973).
- **Fix:**
  - **Icons:** #2A2A2C at about 90%.
  - **Underline:** 2 px, 7 px clear of the box.

---

## Owner's call
- **Crosshair colour:** the owner plays Overwatch with a green crosshair: four 3 px green ticks and a centre dot
  (`owner_2/3`, `xhair10.png`). Ours copies Overwatch's default white one exactly. A crosshair-colour option in the
  hero options would let him match his own.

## Perk picker (`r8_perk_menu`; no Overwatch reference, so not ranked)
- **What it is:** two navy cards (x 610-940 and 980-1310, y 287-500) with an orange top line, a white disc icon,
  white caps names, grey descriptions, and a mouse at each foot.
  - The title "CHOOSE A MINOR PERK" sits at y 255.
  - It reads in Overwatch's language, and the crosshair and volley bar stay clear.
- **What to fix in it now:**
  - Its mice are item 8's egg.
  - Its descriptions use the jagged small face (item 7).
- **Overlap:** the left card's lower edge covers the tips of the held talismans (x 610-690, y 455-500). That is
  harmless, since the HUD is drawn over the view model in Overwatch too.

## Earlier items seen again (not re-ranked)
- **R2 #24 (the reload line):** the white line fills along the bottom line in `r8_reload` (kunai, x 1613-1820) and in
  `r8_ofuda_volley` (ofuda, starting). No Overwatch reload frame is in this set.
- **R1 #45 / R2 #4 (bent talismans, Blizzard's own):** the owner's skinned talismans bend the same way.
- **Swift Step target (`r8_ally_close`):** chevron tips at x 899-1020 and y 456-578 (about 121 x 122), with LSHIFT at
  y 593-612. There is no Overwatch frame of the diamond in this set.
- **Ally pips on the villager:** at y 334-342, x 905-1015, with the chevron under them. A villager has no name, so
  none shows.

## Not checked
- **Cooldown and cast states** against the owner's frames: none of them has an ability cooling. Ours (`abil4.png`:
  "7", "11", the ghost icon, the brush, the rising orange) still looks like Aspen's `ref1080` E 12.
- **The kunai reload pose** (`r8_reload`: the left hand at the bottom left holding two kunai, behind the portrait) and
  **the ofuda reload pose** (`r8_ofuda_volley`: an empty hand palm-down over the ult meter): no Overwatch frame of
  either.
- **The chat's colour:** Overwatch's match-chat orange has no measured value. Ours, #F7A54A, is the owner's request.
- **The comms wheel and pings** (`owner_2/3/5/6`): described in `comms-reference.md`. Ours isn't in these captures.
- **Talisman and kunai size** against the default weapons: the owner's frames carry a weapon skin. Only Blizzard's 2022
  capture covers the default ones (item 1).

## Crops (all in `critique\round8\crops\`)
- **Bottom left:** `bl_pair.png`, `hp_num4.png`, `plusbox6.png`, `plusbox_q.png`, `namerow3.png`, `levelbar5.png`,
  `portrait4.png`.
- **Ult meter:** `ult31_pair.png`, `ult47_pair.png`, `ult_all.png`, `ult_num6.png`, `ultready4.png`.
- **Bottom right:** `br_pair.png`, `abil4.png`, `mice8.png`, `lineends5.png`, `text8.png`.
- **Middle:** `center3.png`, `xhair10.png`, `volley5.png`, `bracket8.png`.
- **Chat:** `chat3.png`, `chat6.png`, `chat_q0.png`.
- **First person:** `fp_pair_half.png`, `fp_ofudahand.png`, `fp_kunaihand.png`, `fp_o4.png`, `fingertip_grid.png`,
  `kunai_grid.png`, `q1q3_half.png`.
- **Normalised Overwatch frames:** `critique\round8\ow1080\o2..o6.png`. The measuring scripts are in
  `critique\round8\scripts\`.

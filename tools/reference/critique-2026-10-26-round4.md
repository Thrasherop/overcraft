# Kiriko vs real Overwatch: picky visual critique, round 4 (2026-10-26)

This fourth pass covers only the newest captures in `run/screenshots/`:
- HUD: `hud6_*` and `hud6_sheet`.
- Effects: `fx8_sheet_a`, `fx8_sheet_b` and the `fx8_*` frames behind them.
- Kitsune Rush in first person: `fox4_*`; path tiles: `tal2_z`.
- Hit markers: `hm4_*`, `hm5z_kill`.
- Hero-mode chat: `chat_sheet`. Kill feed: `r3d_feed`, `r3d_feedz`.
- Suzu cleanse: `cg_sheet`, `cg_1pz`. Hero select: `tri_sheet`, `tri_select`.

The Overwatch frames are the same as in the earlier rounds: `ref1080` (the owner's mid-Swift-Step frame), `ref_owner_02`
and `ref_queued_00..03`, all at 1920x1080. They stay in the session scratchpad. The other sources are
`kiriko-reference.md` and `kiriko-effects.md`. This report is text only (repo rule).

**Method:** crops at the same 1080p coordinates, zooms up to 6x, Pillow pixel samples, bounding boxes and line profiles.
The reference frames are blurred, so their edges run 1-2 px soft. Distances in metres are estimated from the size of
known things on screen (Kiriko about 1.7 m tall, a block pixel 1/16 m), so they are approximate.

**Confidence:**
- **H:** measured, or clearly seen in both.
- **M:** seen once, or partly inferred.
- **L:** general knowledge, or seen poorly.

**References to earlier rounds:** "R1 #n", "R2 #n" and "R3 #n" are items from the earlier rounds. Items those rounds
marked done, not ours or checked are repeated only where these captures show them still wrong (items 2 and 8).

**Not counted, per the brief:**
- The flat test world, villagers, zombies and Steve as stand-ins, and Minecraft's sky and blocks.
- The red command-feedback text, the white death poofs, dropped items (the floating rotten flesh), XP orbs, toasts and
  the subtitle box.
- Ofuda on the right mouse button with kunai on the left, and the ability icons under the name on the select screen.
- Rank cosmetics.
- Overlapping effects from the test scripts (an earlier rush's gates standing in the ofuda shots): each effect is
  judged on its own.

**Capture dates:** `cg_*` (04:48) and `tal2_z` (05:11) were taken before round 3's fixes: their volley bar is still the
old hazard-stripe one. They can't confirm or refute round 3's items. Everything else is from 05:21 or later.

## Status (kept up to date)
- **Open:** all items, 1-15.

## Confirmed fixed (seen in these captures)
- **Ammo text (R3 #15):**

  | | Ours | Overwatch |
  |---|---|---|
  | Kunai `11\|15` | x 1775-1835, y 966-985 | x 1775-1835, y 965-983 |
  | Ofuda `10\|10` | x 1514-1576, y 948-966 | x 1517-1577, y 948-965 |

- **Health number (R3 #13):** "250" spans y 896-917 (21 px), against "199" at y 896-916 in `ref1080`.
- **Weapon panel lines (R3 #14):** the line is a 3 px #2D96DA core with about 3 px of falloff on each side (about 9 px in
  all), like Overwatch's. The halo is still a little lighter (#275C84 against #103C6A), which isn't worth an item.
- **Volley bar (R3 #7):**
  - The segments are rounded tiles, rotated about 40°, and the row reads as a chain.
  - Spent tiles are grey outlines over a dark fill.
  - The bar drains from the right: at `6|10`, 3 tiles are lit.
  - What's left is item 13.
- **Cooldown boxes (R3 #8):** the icon is a faint ghost (box about #56614E over grass). The only black is the splash.
- **Kill feed (R3 #6, part):**
  - The colours are right: ally #2D7AA0 (Overwatch #277CA6 / #29759D), enemy #852D3E (Overwatch #7C2335-#8B293C).
  - The killer's portrait sits on the inside, next to the middle.
  - What's left is item 6.
- **Chat panel (R3 #12):** gone in hero mode; only the text is drawn. What's left is item 12.
- **Fox (R3 #1):** translucent ice blue, and the grass shows through it (`fox4_b`).
- **Kitsune Rush cast (R3 #5) (M):** no 3 m ring round her. `fx8_rush_a` shows only a ground ring of about 1.4 m.
- **Path tiles (R2 #17):** talisman cards with glowing kanji (`hud6_rushing`, `fx8_rush_*`).
- **Ult meter:** all three states are right:
  - charging (`59`, ticks);
  - `0` with no ticks after the cast;
  - ready, with the halo, the triangles and the inverted Q.
- **Hero select:** the model is shown hips up, large, and fades into the dark band with no cut (`tri_select`).
- **Swift Step corner smoke (R3 #2, part):** present in `hud6_cast`. What's left is item 9.

---

## Ranked list (most noticeable first)

### 1. Ofuda hitting the ground: the glow is cut off by the ground in a straight line [BROKEN] (H)
- **Where:** `fx8_ofuda_d` (`fx8_sheet_a` row 2, tile 1): two ofuda landing about 4 m ahead.
- **Overwatch:** the terrain hit (4111) is a soft glow of 0.3-1.3 m lasting 0.08-0.4 s, plus the flower decal. It has
  no straight edges.
- **Ours:**
  - The glow is a camera-facing quad half sunk into the ground. It ends in a dead-straight horizontal edge at y
    563-564, about 370 px long (x 800-1170).
  - Across that edge the colour goes from clipped cyan (#3AFFFF, #B5FFFF) to grass (#398358) in one pixel row.
  - The glow above the edge is about 370 x 120 px (roughly 1.5-2 m) and clipped to pure cyan.
  - This is the depth clip R1 #8 fixed on the Suzu burst. It shows on every volley that misses.
- **Fix:**
  - Treat it like the Suzu burst: soft-particle fade against scene depth, or lift the billboard toward the camera, or
    turn depth testing off for this one sprite.
  - Keep it to 1 m or less and 0.4 s or less.
  - Blend it, don't clip it to #00FFFF.

### 2. Ofuda locked on an ally: still cream-khaki comets, no gold and no paper (H, measured)
- **Where:** `fx8_ofuda_a`, `fx8_ofuda_b`: two ofuda curving onto the villager from about 5 m away.
- **Earlier rounds:** R3 #4 (gold) and R2 #2 (paper) are marked done. These frames come after round 3's fixes (the
  volley bar is the new one) and show no change.
- **Overwatch:**
  - The colour is gold: #DCB900 / #A18700, up to white-gold.
  - Effect 7E2D is the paper talisman plus a ribbon, with a gold core glow behind it.
- **Ours:**
  - The hottest pixels are #DCDEB6: G is at least R, at about 18% saturation, so they read as cream.
  - The trails are #A0AB79 and #839261, olive over the grass.
  - Each ofuda is a soft blob about 35 px across with a broad tail about 120 px long. At 4x no card shape shows
    anywhere in it.
- **Fix:**
  - Alpha-blend the core (not additive) at about #E8BE18, up to 80%, so the hue stays gold over green.
  - Draw the paper card (the teal ofuda's fix) in front of the glow.
  - Make the trail ribbon about 7 cm wide.
  - Check it against grass and against sky.

### 3. Kitsune Rush gates: flat, clipped cyan, and the near posts flood the view (H measured; M on Overwatch up close)
- **Where:**
  - `hud6_rushing`: the right post, x 1490-1830.
  - `fox4_c`: both posts (x 380-500 and 1430-1540) and the beam across the top.
  - `tal2_z`, and `fx8_rush_a`/`fx8_rush_c` in third person.
- **Overwatch:**
  - Look 12FF1 is white/grey through a gradient ramp: a translucent gate glowing white-cyan.
  - R1 #12 also asked for the gates to fade within about 3 m of the camera.
- **Ours:**
  - One flat cyan tint everywhere: #8EF5F5 / #94FFFF over the sky, #5CFFFF over the grass, with G and B clipped.
  - There is no white core and no edge glow.
  - A post about 1 m away is a 340 px slab of #5EFDFF-#ACFEFF.
  - At the cast, the path floor between the posts clips to #50FEEE / #4DFFC1 (`fox4_c`).
- **Fix:**
  - Shade the gate white to cyan with a fresnel or edge ramp: a white core (about #E8FFFF at about 35%) and a cyan rim.
    Never let it clip.
  - Fade each fragment by its distance from the camera: invisible at 1.5 m or less, full at 4 m.
  - Cap the floor glow under the cast.

### 4. Kunai throw: the hand lands on the crosshair (M)
- **Where:**
  - `fx8_kunai_a`: the fingers span x 909-1025, y 525-615, over the crosshair at (960, 540).
  - `hm4_head`: the index finger crosses the zombie's head at the moment of the hit.
- **Overwatch (BLZ-Kunai):** the arm whips out straight, aimed just right of the crosshair, with the hand at about x
  0.5-0.65, y 0.55-0.7 (x 960-1250, y 594-756).
- **Ours:**
  - At the peak of the throw the hand is about 80-100 px higher and 50-150 px further left than Overwatch's.
  - It covers the target just as the kunai hits: at 90 m/s the hit lands during the reach.
  - Later in the throw (`fx8_kunai_b`) the hand is at x 1000-1300, y 610-760, where Overwatch has it.
- **Fix:**
  - If the clip is Blizzard's, check the camera first. Overwatch's default 103° horizontal FOV is about 70.5°
    vertical, and Minecraft's FOV slider is vertical.
  - Otherwise lower and move the peak by about +80 px in x and +90 px in y in the first-person action.

### 5. At night the first-person arms go almost black (M)
- **Where:** `hm4_head`, `hm4_body`, `hm4_kill`.
- **Overwatch [K]:** the view model is always lit and readable, even on night maps.
- **Ours:** the arms take Minecraft's light level. The night lightmap also shifts them toward purple.

  | | Night | Day |
  |---|---|---|
  | Forearm | #302438 | #BA8975 (night is about 25% of it) |
  | Glove | #170710 | #3F141A |

  The arms take up a quarter of the screen and merge into the dark grass (#121815).
- **Fix:**
  - In hero mode, give the view model a light floor: for example a packed light of at least sky 10, or a fixed 0.6
    brightness. It is submitted with vanilla's `light` in `HeroModelRenderer.submitFirstPerson`.
  - The HUD is not affected.

### 6. Kill feed: a tiny thin ">", no weapon glyph, no victim portrait, and a white frame (M-H)
- **Where:** `r3d_feed`, `r3d_feedz`: the player kills a zombie, probably with a kunai (`13|15` on the panel).
- **Overwatch** (`ref1080`, `ref_queued_00` and `_03`):
  - The middle holds the weapon or ability glyph, then a bold ">" of about 9 x 14 px with a 3 px stroke.
  - The victim box has its portrait on the inside end.
  - None of those rows has an outline.
- **Ours:**
  - The middle box (#1F262E) holds only a ">" of 5 x 8 px with a 1 px stroke (#F1F2F2 / #C8CACC), and no glyph.
  - ZOMBIE has no portrait.
  - The whole row has a 2 px #F0F0F0 frame (x 1599-1885, y 138-175).
- **Fix:**
  - Make the chevron bold: about 9 x 14 px with a 3 px stroke.
  - Always draw the glyph: kunai, ofuda, melee, or a generic one for environment kills.
  - Give mobs a portrait on the victim's inner end, from the mob's face texture.
  - Drop the frame, unless a video shows it is Overwatch's "you were in this" highlight (R3 left that open).

### 7. Two crisp-rimmed bubbles are left: the kunai impact and the Swift Step arrival (M)
- **Where:** `fx8_kunai_a` (wall hit), `hm4_head` (zombie hit), `fx8_step_a` (arrival, third person).
- **Overwatch:**
  - **Kunai on terrain (3BF4):** 6 tiny cyan sparks (0.05-0.06 m) and nothing else. A hit on a target is sound plus
    the game's generic hit effect.
  - **Swift Step arrival (3CC5):** a soft teal puff (EFC0) of 0.6-0.8 m, lasting 0.24-0.48 s.
- **Ours:**
  - **Kunai:** a teal sphere of about 94 px (about 0.6 m) with a crisp rim, plus a horizontal lens streak of about
    185 px (about 1.2 m), both centred on the crosshair. At night it's a dark-teal disc.
  - **Swift Step:** a sphere of about 255 px (about 1.3 m at her distance) with a fresnel rim, inside the slash arcs.
    R2 #16 shrank it from 3.5 m, but it still reads as a shield bubble.
- **Fix:**
  - **Kunai:** drop the sphere and keep the sparks. Keep the lens streak to 0.45 m or less, and only in flight.
  - **Arrival:** a soft radial puff with no rim, 0.8 m or less, 0.4 s or less.

### 8. Kitsune Rush "+" sparkles: hard flat stickers (M)
- **Where:**
  - `fox4_c`: a pale yellow cross (#ECFFB7) of about 65 px with a lighter outline at x 1055-1120, y 465-535, about
    3-4 m away, and a white one at x 400-450, y 585-630.
  - `fx8_rush_a`: white crosses on the gate tops.
- **Overwatch:** 3DC7, "+"-shaped sparkles in HDR gold-white, 0.43-0.6 m, lasting 0.9-1.2 s. They glow.
- **Ours:** hard-edged, opaque-looking blocky crosses that read like a Minecraft health icon. Round 3's change made
  them "smaller and dimmer", but up close they are still stickers.
- **Fix:**
  - A soft cross sprite: each arm with a gaussian falloff, and a bright core.
  - Additive gold-white, with a twinkle (a scale and alpha pulse).
  - Fade them out within 2 m of the camera.

### 9. Swift Step cast: wide white arcs sweep across the middle of the screen (M-L)
- **Where:** `hud6_cast`.
- **Overwatch:**
  - During the 0.2 s seal: wisps beside the hands.
  - Then streaks rush in from the screen edges, and the centre stays readable.
  - Mid-step, dark teal smoke in the corners (`ref1080`).
- **Ours:**
  - The corner smoke is there now.
  - But 4-5 long white-cyan arcs, about 40-60 px wide, sweep from the bottom centre to the upper right, across the
    target's head and the crosshair.
- **Fix:**
  - Keep the streaks in the outer 30% of the screen.
  - Point them at the centre, keep them thin (8 px or less), and fade them out before the middle third.

### 10. The ally's health bar is pinned to the top of the screen when the ally is close (M; L on Overwatch's behaviour)
- **Where:** `hud6_cooldown1`, `hud6_cast`: the bar and its chevron sit at x 920-1000, y 27-37, while the villager's head
  top is at about y 190.
- **Overwatch [K]:** the bar sits over the ally's head. I haven't seen it clamped to a screen edge.
- **Ours:** it is clamped to the top centre, where Overwatch's objective bar sits.
- **Fix:** don't clamp it to the screen. When its anchor is off-screen, let it leave the screen, or hide it.

### 11. The ally health bar is small, with tall pips (L-M)
- **Where:** `fx8_step_a`/`fx8_step_b` and `hud6_rest`, over the villager.
- **Overwatch** (`ref_queued_02`, `ref_queued_03`):
  - About 110 px wide for 9 pips.
  - Each pip is about 10 x 9 px with about 2 px gaps: white, with a thin dark edge.
  - The name is under the bar in blue caps.
- **Ours:** 80 px for 10 pips (x 920-1000, y 408-419). Each pip is about 7 x 11 px (taller than wide), inside a dark
  outlined box.
- **Fix:** squarer pips of about 10 x 9 px at an 11-12 px pitch (about 120 px for 10). Show the name for players.

### 12. Chat in hero mode: Minecraft's pixel font, flush against the edge, with the vanilla tag bar (L-M)
- **Where:** `hud6_rest`, `hud6_wall`, `chat_sheet` (left).
- **Overwatch** (`ref_queued_00`, `_01`): a smooth sans of about 15 px, team lines in light blue, starting at about x 90
  after a small team icon at x 62.
- **Ours:**
  - Minecraft's pixel font with a hard 2 px shadow, at an 18 px line pitch.
  - The text starts at x 8.
  - Minecraft's 4 px #D0D0D0 message-tag bar sits at x 0-3 on each line.
- **Fix:** in hero mode, chat in the HUD font (the one the names and ammo use), with a left margin of about 60 px, a
  soft shadow and no tag bar.

### 13. Volley bar: the tiles are a little short and have gaps between them (L, measured)
- **Overwatch (`ref1080`):**
  - The amber runs unbroken from x 889 to 1032: every column has amber in it, because neighbouring tiles overlap in x.
  - At its thickest, each tile's columns are 19-20 px tall.
- **Ours:** five separate tiles of 26 px (x 889-914, 918-943 ... 1005-1030), with 3 px empty columns between them and
  18 px at the thickest.
- **Fix:** tiles of about 30 x 15 px (now about 25 x 14) at the same 29 px pitch, so neighbours overlap by 2-4 px in x.

### 14. Cooldown splash: a round blob, not a ragged outline (L)
- **Overwatch:**
  - `ref1080`'s "12": a ragged black brush stroke hugging the digits 3-6 px out, with a couple of drips.
  - `ref_queued_00`'s "6": only a thin dark edge.
- **Ours:** a round blot of about 40 x 45 px behind the digits, with drips (`hud6_cooldown2`'s "13", and "6", "4", "3",
  "11").
- **Fix:** build the splash from the digits' outline, grown by 3-5 px and roughened with noise, plus 2-3 drips.

### 15. Kitsune Rush cast: a grey blade at the left edge (L)
- **Where:** `fox4_b`, x 0-210, y 690-900: a large grey-blue curved shape with a hard edge.
- **What it might be:** either the fox's tail passing the camera without the spirit material, or the kunai being put
  away.
- **Fix:** find out what it is. If it's the fox, give it the same translucent cyan as the body; if it's the kunai, hide
  it with the other weapons.

---

## Earlier items seen again, still open (not re-ranked)
- **R3 #11 (where Swift Step lands):**
  - In third person (`fx8_step_b`, `fx8_step_c`) she ends up overlapping the villager: her torso sits between its
    folded arms.
  - In first person (`hud6_cooldown1`) the villager fills the screen.
- **R2 #24:** the white reload line, on the ofuda panel (`hud6_spent`) and on the kunai panel (`hud6_reload`, x
  1615-1790).
- **R1 #27 and #28:** the portrait is still a flat-bottomed frame with the head clipped inside, and the name row still
  has no light-blue number box.
- **R1 #35:** the "ready again" cyan flash keeps a black icon (`fx8_ofuda_a`/`b`, the E box).

## Not checked
- **Suzu cleanse (R3 #10):** `cg_*` were taken before that fix, so they can't show it. Re-shoot them.
- **Wall climb:** `hud6_wall` shows her standing at a wall (idle weapons, level camera), not climbing. CHANGES.md says
  `cl2_sheet` shows Blizzard's climb clip, but that capture is not in this set.
- **Low health:** no capture this round.
- **Ofuda with no target, in flight:** no capture. `fx8_ofuda_d` is the landing.
- **Kunai in flight:** too fast to show in any frame; only the impacts are visible.
- **Hit markers:** none of the Overwatch reference frames shows one. Ours follow the brief: white on the body, red on
  the head, a larger red on a kill (`hm4_*`, `hm5z_kill`).
- **The Suzu burst itself:** the `fx8_suzu_*` frames come after it. The Suzu in flight (R1 #37) isn't captured either.
- **Kitsune Rush cast hand poses:** `fox4_b` (crossed wrists) and `fox4_c` (palms down) against the reference's prayer
  seal and then palms out. Two frames of what may be Blizzard's clip are not enough to judge.
- **Enemy-view colours, quick melee, a second Kiriko.**

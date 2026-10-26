# Kiriko vs real Overwatch: picky visual critique, round 2 (2026-10-26)

This is the second pass, on the new captures in `run/screenshots/`: `hud4_*`, `sel3`, `menu_v3`/`p2_sheet`, `fx7_sheet_a/b`,
`p1_burst`, `cl1_*`, `of_sheet`, `rl1p_sheet`, `ofh_sheet`, `st2_sheet`, `mpn_sheet`, `mp1_sheet_v/k` and `al1_near_far`.
They are compared with the same Overwatch frames as round 1 (`ref_owner_02`, `ref_queued_00..03`, normalised to
1920x1080, in the session scratchpad) and with `kiriko-reference.md` / `kiriko-effects.md`. Text only (repo rule).

**Method:** crops were taken at the same 1080p coordinates and compared side by side. Sizes come from pixel bounding
boxes and colour profiles (Pillow). The reference frames are blurred, so their boxes run 1-2 px small. Sheets are
1/3 or 1/4 scale, so sizes measured on them are approximate. Confidence: **H** = measured or clearly seen in both;
**M** = seen once or partly inferred; **L** = general knowledge or seen poorly. **"R1 #n"** points to the round-1 item.

Not counted, per the brief: the flat test world, villagers or Steve as allies, Minecraft's sky and blocks, the red
command-feedback text, the white death-poof squares, the subtitle box, the tutorial toast, ofuda on the right mouse
button with kunai on the left, and the kit row under the name on the select screen.

## Confirmed fixed since round 1 (no action)

- **Block outline:** gone (R1 #3).
- **Cooldown boxes:** the darkened icon, ink splash, number, orange rise and shorter box all match `ref_queued_00`/`02`
  and the owner's frame (R1 #4, #22).
- **Ult ticks:** pitch and fill direction match, and the ticks span r 38.5-46.2 px against the reference's r 40-45.5
  (R1 #5).
- **Ult number:** the "59" glyphs are 40 x 36-37 px, the same as the reference.
- **Key labels:** they invert when ready (R1 #7).
- **Ult states:** the ult reads `0` with no ticks after the cast (R1 #13); the triangles and the inverted Q are there
  when ready (R1 #14).
- **Health pips:** 24 x 31 px at a 26.6 px pitch (reference 24 x 30, pitch 27). Partial and empty pips (cl1_lowhp:
  38 HP shows 1 full pip, a half pip, then dark empties) match `ref_queued_02` at 27 HP (R1 #29).
- **Other round-1 fixes:**
  - A low-health vignette exists (R1 #20).
  - Ally pips and the chevron exist (R1 #21).
  - Petals are small (R1 #15).
  - The Suzu ground mark is a thin gold line (R1 #38).
  - There is no white HUD disc on Swift Step (R1 #9) and no hard cut through the Suzu glow (R1 #8).
  - No hotbar shows behind the select screen (R1 #34).
  - The Esc menu shows the sub-role (R1 #42).
  - The Swift Step seal pose is right.

## Status (kept up to date)

- **Done, 2026-10-26 (CHANGES.md, "The critique's second round"):** 1 (the fox was there: larger, a wake, the gate
  glow fainter), 3, 5, 6, 8, 9, 10, 11, 12, 13, 14 (flower; no dome yet), 15, 16, 21, 25, 26, 27 (kill feed).
- **Found:** 7's sideways leg was a broken key in the breathing clip, fixed for every pose ("Her leg no longer kicks").
- **Not ours:** 4 (hooked talismans) and 7's high-knee strafe are in Blizzard's own clips (survey renders A3A7, A8F3).
- **Done after:** 2 (the paper faces the viewer, thin trail), 18 (a glow pass).
- **Open:** 17 (path slabs' glyphs, in-path streaks),
  19 (cloth), 20 (the ofuda flick's fist), 22, 23, 24 (check a reload in a video).

---

## Ranked list (most noticeable first)

### 1. Kitsune Rush still has no fox [MISSING] (H)
- **Where:** `hud4_rushing.png`, `fx7_sheet_b.png` rows 2-3, `mp1_sheet_v.png` tiles 6-7 (what the ally sees).
- **Overwatch:** a large translucent ice-blue crystalline fox bounds about 32 m down the path, shedding 33-66 petals per
  second. In model units it is 2.9 m nose to tail and 0.85 m tall (`kiriko-effects.md`, entity 3F54, model 105FB). It
  is the centrepiece of the ult, and both teams see it.
- **Ours:** a white glowing orb (about 60-80 px) with white "+" sparkles at the end of the gate row. The subtitle says
  "Fox spirit summoned", but nothing fox-shaped is on screen.
- **Fix:** render model 105FB from the local asset pack with an additive cyan material and a run loop (AE68, AE6A or
  AE6B), moving 32 m. Interim placeholder: a stretched cyan streak sprite with a 2-3 m ribbon tail in place of the orb.
  R1 #12 (still open).

### 2. Healing Ofuda in flight: still no paper; comets, big halos and gold "wings" [MISSING] (H)
- **Where:** `ofh_sheet.png` tiles 1-3, 7 and 8 (no target); `of_sheet.png` tiles 1 and 4, and `fx7_sheet_a.png` row 1
  (locked onto the villager).
- **Overwatch:** a white paper talisman (mesh 10942, about 0.25 x 0.75 m) that reads as paper, with a thin ribbon
  trail. Its glow hugs the paper: teal with no target, gold when seeking. Gold trails are thin streaks that spiral
  around the ally.
- **Ours, no target:** each ofuda is a cyan teardrop or comet about 100 px long inside a round teal halo about 150 px
  across, at about 10 m. No paper is visible in any frame.
- **Ours, locked on:** soft gold blobs on the villager, plus broad horizontal gold smears about 250-300 px wide on both
  sides, like wings.
- **Fix:**
  - Draw the paper quad: velocity-aligned, a slight flutter, white with the seal.
  - Cut the halo to 1 m or less at about 40% alpha.
  - Make the ribbon about 0.2 m wide, fading to the tail and following the real (spiral) path, not a fan.
  - R1 #2 (open) and #1 (only partly fixed).

### 3. Volley bar: thin slashes, not fat lozenges (H, measured)
- **Where:** HUD, under the crosshair, always on screen (`hud4_rest`, `hud4_mid_pair`, `hud4_spent`).
- **Overwatch** (`ref_owner_02`, `ref_queued_00`/`02`): five chunky rounded parallelograms, each about 31 x 22 px and
  15-17 px thick, with about 3 px gaps. They read as a row of tiles, and the top and bottom edges tilt up to the right.
- **Ours:**
  - Each segment's bounding box is 26 x 22 px (x 889-914, 918-943 ... 1005-1030; y 665-686).
  - But each one is a capsule only about 7-8 px thick, leaning about 55°, with about 12 px of background between
    them, so the row reads as `/////`.
  - Spent segments are thin grey capsule outlines with no dark fill.
- **Fix:**
  - Shape: about 22 px wide and 20 px tall, sheared 45°, then rotated about -10°, with 4 px corners. Keep the 29 px
    pitch.
  - Spent: a 1.5 px #C8C8C8 outline over about 35% black, in the same fat shape.
  - R1 #25 (marked done, but the shape went to capsules).

### 4. Idle talismans are bent into claws (H)
- **Where:** first person, in every frame (`hud4_*`, `cl1_*`, `al1_near_far`, `rl1p` tiles 6-8, and even blurred
  behind `menu_v3`).
- **Overwatch:** a fan of 2-3 flat paper strips, fluttering slightly at most (§2).
- **Ours:** both strips curve about 60-70° over their length into a hook pointing down-left, like a scythe or claw.
  It is always on screen.
- **Fix:** limit the paper bones' bend to about 10° in the idle and reload poses. R1 #45 (open).

### 5. Swift Step cast: a broad flat teal wash instead of wisps and edge streaks (H)
- **Where:** `hud4_cast.png`; `fx7_sheet_b` tile 2 shows it even in third person.
- **Overwatch:** cyan spirit-flame wisps flare beside the clasped hands, which glow; then cyan/teal streaks rush in from
  the screen edges. The centre stays readable.
- **Ours:**
  - Most of the frame is tinted teal: the sky goes from #8AB3FF to #3DD7D9 and the grass turns teal-green.
  - There are no streaks and no hand wisps.
  - The tint also shows in a third-person camera.
- **Fix:**
  - An edge-only overlay of 20-30 thin radial streaks over 0.25 s.
  - Two wisp sprites at the hands.
  - Drop the full-frame grade, or keep it as a soft edge vignette only.
  - First-person camera only.
  - R1 #16 (open).

### 6. Swift Step puts the camera inside the ally (M-H)
- **Where:**
  - `mp1_sheet_k.png` tiles 3-4: Steve's face fills the screen, then the view looks down through his body.
  - `hud4_cooldown2.png` and `p2_sheet.png` tiles 2-3: the villager's body fills the frame from about 0.3 m.
  - `mp1_sheet_v.png` tile 3: from the ally's side, Kiriko's torso fills their screen.
- **Overwatch:** the arrival effect plays "at a computed position" (3CC5). She lands beside or just behind the ally,
  with the ally in view, not inside their model.
- **Ours:** the destination appears to be the ally's own position.
- **Fix:** arrive about 1 m from the ally, along the ally-to-Kiriko direction (or behind them relative to their
  facing), checked for collision; use the exact spot only when blocked. Not purely visual, but it is what the player
  sees on every Swift Step.

### 7. Third person: high-knee strafing, and a sideways leg on Swift Step [BROKEN] (M-H)
- **Where:** other players see it all the time (`st2_sheet.png`, `fx7_sheet_b.png` tile 2, `mpn_sheet.png`,
  `mp1_sheet_v.png` tile 1).
- **Overwatch:** strafing is a compact side-step; she stands upright on the Swift Step arrival.
- **Ours:**
  - **Strafing:** in `st2` rows 1-2 the knee lifts to waist height with the thigh horizontal; row 3 is a bounding
    stride with the back leg almost horizontal.
  - **Swift Step arrival** (`fx7_sheet_b` tile 2): one leg sticks straight out to the side at hip height.
  - **Torso:** in `mp1_v` tile 1 and `mpn` tiles 1 and 3 it arches about 30° back or forward.
- **Fix:**
  - Recheck the strafe clips' hip retarget (the flexion looks doubled).
  - Clamp hip pitch and roll to ±45°.
  - Blend the arrival to idle instead of freezing on a strafe frame.
  - Halve the aim-pitch lean.
  - R1 #10 (reported fixed; this is either a regression or other clips).

### 8. Ally markers: Minecraft name tag, filled triangle, and the bar sits on a near ally's head (M-H)
- **Where:** world HUD (`mp1_sheet_k.png` tile 1, `al1_near_far.png`).
- **Overwatch:** from the top: a segmented white health bar, the **name in blue caps** under it, then a small **open
  "v"** chevron. All of it sits clear above the head, and there is no other name tag.
- **Ours:**
  - On a player: the bar, then a filled cyan ▼, then **vanilla Minecraft's grey name tag** ("Viewer"), then the head.
  - In `al1_near_far` the near villager's bar crosses its forehead (bar at y 465-475, head top about y 450), with the
    ▼ on its face.
- **Fix:**
  - In hero mode, hide the vanilla name tag for allies and draw the name in blue caps (#5CC6DC, about 12 px) under the
    pips.
  - Replace ▼ with an open v of about 10 px and 2 px strokes.
  - Anchor at the bounding-box top plus about 0.35 m, and never let the bar's bottom fall below the head's projected
    top.
  - R1 #21 (partly fixed).

### 9. Chat: huge and low, reads as Minecraft (M)
- **Where:** HUD layout, whenever there is any chat. This is about the box's size and position, not the red text.
- **Overwatch** (`ref_queued_00`): chat is at the left, about x 60-500, around y 640-660, with about 18 px text, and it
  fades.
- **Ours** (`hud4_rest`, `cl1_lowhp`, `al1_near_far`):
  - The box is 1328 px wide with 36 px pixel-font lines.
  - Its bottom edge is at y 870, right on top of the HUD and over the arms.
  - It no longer covers the HUD, which is better.
- **Fix:** in hero mode, render chat at about 0.5 scale, at most about 500 px wide, with its bottom edge at about y 700.
  R1 #19 (partly fixed).

### 10. Ult meter at rest: a solid black puck instead of a see-through ring (H, measured)
- **Where:** HUD, bottom centre, always on screen.
- **Overwatch** (`ref_queued_02`, "17"):
  - The inside (r 30-34) is #6A5A67 against the background's #6C6072: about 5% darker.
  - Only the tick band (r 38-46, #403543) is about 40% darker, and it is a smooth band with no unfilled ticks.
- **Ours** (`hud4_rest`, glove behind):
  - The inside is #080203-#130508 against #41121A: about 85-90% darker.
  - Unfilled ticks show as separate dark ticks.
- **Fix:** an inner fill of about 10-15% black; a smooth ring of about 40% black over r 37-47; no unfilled ticks.
  R1 #26 overshot the fix.

### 11. Weapon panels 10-15 px too tall; lines with no glow (H, measured)
- **Where:** HUD, bottom right (`hud4_rest` vs `ref_owner_02` and `ref_queued_00`).

| | Overwatch | Ours |
|---|---|---|
| Ofuda panel at x 1520: top / bottom line centre | y 925 / 986 (61 px apart) | y 920 / 991 (71 px apart) |
| Kunai panel at x 1740: top / bottom line centre | y 940 / 1002 (62 px apart) | y 933 / 1008 (75 px apart) |
| Line profile | 5-7 px: bright core #3B92D3, falloff #195B98 / #114A7B | exactly 3 px of solid #2D96DA, no falloff |
| Gap between the panels | 16 px | 9 px |
| Mouse icon centres | ofuda y 1008, kunai y 1025 | ofuda y 1017, kunai y 1033 (8-9 px low, because the panels are taller) |

- **Fix:**
  - Move the top lines down 4-6 px and the bottom lines up 5-7 px.
  - Add a 2 px soft falloff on each side.
  - Raise the mouse icons 8 px.
  - Widen the gap to 16 px.
  - R1 #23 (partly fixed).

### 12. Low-health vignette: a smooth pink gradient instead of mottled red (M)
- **Where:** screen effect (`cl1_lowhp.png` vs `ref_queued_02`).
- **Overwatch:** blotchy, smoky red patches (highlights #F41217, dark patches #71141C) hug the edges, heaviest in the
  corners. It stays red over blue scenery, and the top band is about 150 px deep.
- **Ours:**
  - A smooth gradient.
  - The top band reaches about y 300 and turns the sky pink-magenta (#967DA7 at x 1100, y 150).
  - The sides are #901D30.
- **Fix:** a scrolling smoke or noise mask with a slow pulse; blend it so it stays red over sky; keep the top band
  under about 150 px.

### 13. Hero select: the model is cut by a hard line, a bit small, and the pose is low (M-H)
- **Where:** menu (`sel3.png`).
- **Overwatch:** the hero is hips up, about 70% of the screen height (about 760 px), and fades into the roster's dark
  gradient. The idle pose holds the ofuda **overhead** with the index finger up (§3, §6).
- **Ours:**
  - The skirt ends in a dead-straight horizontal edge at y 800, the top of the "SUPPORT" header, over x 820-1115.
  - The visible model runs y 205-800 (595 px, 55% of the height).
  - The talismans are held at face height and the kunai at the waist.
- **Fix:**
  - Let the model continue behind the roster under a dark gradient (0 to about 80% black between y 700 and 860).
  - Scale it up about 25%.
  - Use the overhead idle.
  - R1 #32 (partly fixed).

### 14. Suzu burst: no dome, and the flower is emerald and pixelated (M)
- **Where:** effect (`p1_burst.png`, `cl1_suzu1p_a.png`, `cl1_sheet.png` tile 6).
- **Overwatch:** a cyan hemisphere of light filling about 4 m for 0.3 s (mesh 963D), a teal flower glow for 0.5 s,
  and a bright ring #33D5FF.
- **Ours:**
  - A bright ground ring and a vertical column of glow, with no dome.
  - Then a large translucent **emerald** flower (#56D291 / #4EC58B) with the grass pixels showing through. It reads
    as a green clover lying on the grass.
- **Fix:** render the flower additive in Kiriko's teal (#00BCB1 to #00FFF0), so it brightens the grass instead of
  greening it. Add the 0.3 s dome. Keep the flower to 0.5 s.

### 15. Ammo text: still too big and too tight to the icon (H, measured)
- **Where:** HUD, weapon panels.

| | Overwatch (`ref_queued_00`) | Ours (`hud4_rest`) |
|---|---|---|
| Ofuda `10\|10` span | x 1516-1578 (63 px) | x 1505-1575 (71 px, +13%) |
| Ofuda digit height | 15-16 px (blurred; about 16-17 true) | 18 px |
| Gap from the icon to the text | 28 px | 16 px |
| Kunai `15\|15` / `14\|15` span | x 1775-1836 (62 px), 15 px tall | x 1763-1832 (70 px), 18-19 px tall |
| "1" to "0" spacing | 7 px | 9 px |

- **Fix:** about 10% smaller, -1 px tracking, the ofuda text starting at x 1516, the kunai text ending at x 1836. R1 #6
  (partly fixed).

### 16. Swift Step arrival: a big soap-bubble sphere (M)
- **Where:** effect (`fx7_sheet_b.png` tile 2, third person).
- **Overwatch:** 1.4 m rings expanding over 0.3 s, petals, a **small** sphere puff (EFC0, size 0.6-0.8, 0.24-0.48 s)
  and 5-7 slash arcs (3CC5). Nothing like a shield bubble.
- **Ours:** a sphere about 3.5 m across with a crisp fresnel rim and two horizontal rings. It reads as Zarya's or
  Baptiste's bubble.
- **Fix:** drop the sphere, or cap it at 0.8 m and 0.3 s. Keep the rings and petals, and add the crescent arcs.

### 17. Kitsune Rush path: blank glass tiles, tube pillars, no in-path effect (M)
- **Where:** effect (`hud4_rushing`, `mp1_sheet_v` tiles 6-7, `hud4_wall`).
- **Overwatch:**
  - Floating talisman slabs (4013/4014, rounded cards) and teal "glyph tiles".
  - Allies inside the path get vertical speed streaks (3D2F, 40/s) and rising teal cylinders (3C65), plus a misty
    white-cyan edge vignette with light-blue streaks.
- **Ours:**
  - **Tiles:** about 15 plain cyan rounded rectangles with bright rims, with no talisman or glyph.
  - **Pillars:** the near gate's legs are hollow tubes whose open bottom ends show as ellipses (`hud4_rushing` x 320-470,
    y 780-840).
  - **In the path:** no streaks or cylinders, and the ally (`mp1_v`) sees nothing on their screen. `hud4_wall` shows
    only a faint, flat teal edge haze.
- **Fix:**
  - Texture the slabs with the talisman mask, tinted teal at about 50%.
  - Cap the pillars, or fade them by depth near the ground.
  - Add the streak quads and cylinder wisps around allies in the path, and an edge-streak overlay for players inside.
  - R1 #12 (the rest of it).

### 18. Suzu cleanse: a faded ghost, not a bright white silhouette (M)
- **Where:** effect (`mp1_sheet_v.png` tile 4, `cl1_suzu_b.png`, `cl1_sheet.png` tiles 3-4).
- **Overwatch:** for about 1 s the model reads solid but **glows bright white**, with a noise and tile-grid overlay
  (03C3, effect 3398).
- **Ours:** the model goes desaturated and about 50% transparent; the sky shows through her torso.
- **Fix:** keep the model opaque and add an additive white/teal pass (fresnel plus noise). Don't lower the alpha.

### 19. Cloth panels: stiff flaring boards down to the ankles (M)
- **Where:** third person (`mpn_sheet.png`, `mp1_sheet_v.png` tiles 1-2, `st2_sheet.png`).
- **Overwatch:** hip-wide front and back panels to mid-calf, which hang, trail and flap.
- **Ours:** wide panels with sawtooth lower edges. They flare rigidly sideways about 30°, as a straight plank, and
  reach the ankles.
- **Fix:** narrow and shorten them to mid-calf, and drive them with velocity-based swing. R1 #31 (open).

### 20. Ofuda flick: the hand leaves the right half and balls into a fist (M)
- **Where:** first person (`ofh_sheet.png`).
- **Overwatch:** the right arm reaches from the bottom right, back of the hand up, and the index and middle fingers flick
  at about x 0.5-0.6, y 0.55-0.75, every 0.15 s.
- **Ours:**
  - Tile 3: a closed fist at the lower left (about x 0.17-0.30 of the frame, y 0.7-0.8).
  - Tiles 4 and 7: the hand swings to the left edge, holding the fan sideways.
  - Tiles 5, 6 and 8: the open-hand reach at about (0.4, 0.5), which is close to Overwatch.
  - The gold puff at the fingertips is now right.
- **Fix:** keep every flick frame in the 5/6/8 pose, moved right to x 0.5-0.6; drop the fist and the far-left sweep.
  R1 #17 (open).

### 21. Ability boxes, underlines and pips are built from stepped slices (M; H that it is visible when zoomed)
- **Where:** HUD (`hud4_rest` zoomed 6x).
- **Overwatch:** smooth sheared quads with soft anti-aliased edges, and one continuous underline.
- **Ours:**
  - Each ready box's top edge has two 1 px steps.
  - The 3 px underline is three separate pieces, each 1 px lower than the last.
  - Each health pip's top edge has a 1 px step.
  - At 1x the underline looks broken.
- **Fix:** draw each element as one 4-vertex sheared quad, not three column slices. The same applies to the pips.

### 22. Portrait: an octagon with a flat bottom; no head pop-out or frame (L-M)
- **Where:** HUD, bottom left, always on screen.
- **Overwatch:** a shield shape that comes to a point, with the head breaking the top edge, plus wings, a crest and the
  level badge.
- **Ours:** unchanged since round 1 (`hud4_rest`).
- **Fix:** as in R1 #27 (open).

### 23. Name row: the light-blue number box is missing (L)
- **Where:** HUD, bottom left.
- **Ours:** still a dark plate with `PLAYER336` and the blue underline.
- **Fix:** as in R1 #28 (open).

### 24. Reload progress line on the ammo panel [EXTRA?] (L)
- **Where:** HUD (`hud4_br_sheet` tiles 2 and 4, `hud4_reload`, `rl1p_sheet` tile 1).
- **Ours:** a white line grows along the bottom line of the reloading panel.
- **Overwatch:** none of the reference frames shows a reload, and I don't remember this bar in Overwatch.
- **Fix:** check a reload in an Overwatch video before keeping it.

### 25. Ofuda terrain-hit flowers: opaque, saturated mint (L)
- **Where:** effect (`hud4_spent.png`, `fx7_sheet_a.png` row 2).
- **Overwatch:** a small glow (0.3-1.3 m) and a flower decal that fades over 0.73 s.
- **Ours:**
  - Two flowers of about 1 m, flat #57FFFF (clipped cyan), fully opaque.
  - In the first frame, a teal haze about 2 blocks wide surrounds them, so R1 #39's haze is back for a frame or two.
- **Fix:** start the flowers at about 70% alpha and fade them to 0 over 0.73 s; keep the glow at 1 m or less from the
  first frame.

### 26. Ult-ready halo is thin (L)
- **Where:** HUD (`hud4_ready`).
- **Overwatch:** a soft blue halo outside the ring (§1.2).
- **Ours:** the ring colours match (#3FCEFD inner to #2D3FC0 outer), but the glow outside r 57 is only about 2 px
  (#503F79) before the triangles.
- **Fix:** a 6-8 px falloff from #2C40C5 to transparent.

### 27. No kill feed [MISSING] (L)
- **Where:** HUD, top right.
- **Fix:** as in R1 #44.

---

## Quick index
- **Looks broken:** 3, 4, 6, 7, 13 (the hard cut), 21.
- **Missing:** 1, 2, 17, 27; parts of 5, 8 and 14.
- **Extra in ours:** 8 (vanilla name tag), 16 (bubble), 24 (reload line?).
- **Measured mismatches:** 3, 10, 11, 15.

## Not checked (no capture, or ambiguous)
- **Wall-climb first person:** `hud4_wall` shows the idle weapons in front of a wall, but it isn't clear she is
  climbing. Overwatch hides the weapons and does hand-over-hand slaps.
- **Kunai:** in flight and on hitting a mob.
- **Quick melee.**
- **The volley bar's refill flash.**
- **Enemy-view (red) colours.**
- **The Esc menu against Overwatch's own:** I have no reference frame.
- **What a second Kiriko looks like:** the ally markers on a hero player.

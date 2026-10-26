# Kiriko in real Overwatch: visual reference

What real Overwatch Kiriko looks like (HUD, first person, third person, VFX, sounds, hero select). Use it to
check Overcraft's version against the real game. Text only: no Overwatch images are stored here (repo rule).
Compiled 2026-10-26 from the sources below. Gameplay numbers belong in `KirikoStats` with their own sources;
this file only uses them to explain what the HUD shows.

## Sources and conventions

| Tag | Source |
|---|---|
| **[OWN]** | Owner's phone screenshot of a stream (Kiriko mid Swift Step). It shows Aspen's video "Kiriko carry: 20k healing!", Overwatch DAILY, https://www.youtube.com/watch?v=OKWBrSV3oR8 (May 2026 build). The video frame was cropped out of the phone screenshot and measured at 1968x1107. |
| **[ASP]** | Frames from that same video at 1080p, timestamps in seconds (`t=150`). They show the HUD in other states: abilities ready, ult ready, cooldowns, and the select screen at t=500. |
| **[BLZ-x]** | Blizzard's first-person ability captures (1920x1080) from the hero page https://overwatch.blizzard.com/en-us/heroes/kiriko/. The files are `...CHARACTER_CAPTURE_Kiriko{HealingOfuda,Kunai,SwiftStep,ProtectionSuzu,KitsuneRush}...mp4` on blz-contentstack-assets.akamaized.net. They have no HUD (2022 capture). |
| **[V7]** | "Overwatch - Kiriko Details, Animations, Sounds [4K]", VIK7ORIO, https://www.youtube.com/watch?v=aqMNw7MLrPw. Its "First Person Abilities" chapter runs 2:40-3:00 (t=160-180), wall climb is at 1:39. |
| **[WIKI]** | Overwatch wiki through its MediaWiki API: `api.php?action=parse&page=Kiriko` and `page=Kiriko/Quotes`. |
| **[FONT]** | https://madegooddesigns.com/overwatch-font/ ("Big Noodle Too" is the UI face, a commissioned version). |
| **[K]** | General knowledge of Overwatch that I did not verify against a source. Treat it as a hint only. |

Confidence: **H** = seen clearly or quoted from the source; **M** = seen, but partly inferred or seen only once;
**L** = guessed or seen poorly.

**Coordinates:** fractions of a 16:9 frame, `x` from the left and `y` from the top. Pixel sizes are for 1920x1080.
**Colours** are hex values sampled from compressed video. Treat them as close, not exact: dark translucent fills
take on the scene behind them.

---

## 1. HUD at 16:9

### 1.0 Overall look
- **The HUD is drawn on a slightly curved surface, not flat** [OWN, ASP] H. The bottom-left and bottom-right
  groups tilt so that their inner ends, toward the screen centre, sit higher. The tilt grows toward the screen
  edges: the ofuda panel's top line slopes about 2.6° and the kunai panel's about 4.4°. On the left, the row of
  health pips rises about 3.5° from left to right.
- There are no big background plates. Elements float over the world, with thin dark translucent fills at most.
  The fixed palette is white, a bright blue (#2D96DA), orange (#F77232) and the team colours: ally blue/cyan and
  enemy red [OWN] H.
- Nothing like a hotbar, XP bar or hunger bar. The bottom centre holds only the ultimate meter.

### 1.1 Bottom left: portrait, health, name [OWN, ASP] H
| Element | Position / size | Look |
|---|---|---|
| Portrait frame | x 0.011–0.098, y 0.818–0.965 | Hero portrait (Kiriko's face, matching her equipped skin) inside a shield-shaped frame with a blue (#68CCFF) inner border. The account-level frame is around it: gold/teal "wings" left and right with a gold crest below. A small dark hexagon at the bottom holds a yellow number (#F4FD70), the endorsement/level badge (meaning M). |
| Health icon box | x 0.100–0.123, y 0.832–0.855 | Small dark translucent box with a white "+". |
| Health number | "199" at x 0.128–0.152, y 0.829–0.850 | Format **`199 │ 225`**: current health large, bold, upright, white, about 23 px cap height. A thin vertical divider line follows, then the maximum, smaller (about 16 px) and dimmer white. No "HP" label. |
| Health pips | x 0.102→, top y ≈0.864 (left) to 0.854 (right) | One pip per **25 HP**, so 225 HP = 9 pips. Pips are white (#FFFFFF), upright rectangles about **22 x 30 px** (taller than wide) with about 4 px gaps. At 199/225, 8 pips show (the 8th at 96% still reads as full) and the 9th is absent or invisible, not drawn as a dark empty box. The row ends near x 0.213 with 8 pips and would end near 0.227 with 9. |
| Name row | x 0.100–0.224, y 0.897–0.940 | Light-blue box (#41CCFD) holding a number (the player's slot/group), then a red name plate "ASPEN" with a rank-like emblem and a glowing orange flourish. It is cosmetic (name card), and a thin blue glowing line under it ends in a bright blue dot. |
- Health colours when present [K] M: health is white, armor yellow-orange, shields light blue. Kiriko has
  neither armor nor shields, so on her the pips are all white.
- In the spawn room, an orange **"CHANGE HERO"** button appears near the bottom [ASP t=520] H.

### 1.2 Bottom centre: ultimate meter [OWN, ASP] H
- **Position:** centre (0.500, 0.861). The outer ring is about 0.058 W across (≈112 px). The **"Q"** key label sits
  below, centred at (0.500, 0.933), a box of about 24 px.
- **Charging:** a dark translucent disc with a thin grey inner circle. The **percentage number** (no "%" sign) sits
  in the centre: white, **italic, tall condensed (Big Noodle style)**, about 36 px tall, e.g. `59`. Around it is a
  ring of about 60 **radial orange tick marks** (#F18449 bright, #C65B1E darker), from about r 37 to r 47 px. The
  ticks fill **clockwise from 12 o'clock** in proportion to the charge (59% reaches about 7 o'clock). Behind them
  is a faint grey full track.
- **Just used:** shows `0` with no ticks [ASP t=560, 880].
- **Ready** [ASP t=330, 720]: the number is replaced by a **white disc (#EAEAE9) with a black fox-head (kitsune
  mask) emblem**. A thick, glowing **electric-blue ring** surrounds it: inner edge #36ACFF to #3FCEFD, outer edge
  #3355E7 to #2C40C5, with a soft blue halo. Small white triangles at 9 and 3 o'clock point inward. The **"Q" label
  turns solid white with a dark letter** (inverted from its usual outline).

### 1.3 Bottom right: ability boxes [OWN, ASP] H
- Two square boxes side by side, then the two weapon panels:
  - **Swift Step:** x 0.666–0.698, y 0.836–0.900.
  - **Protection Suzu:** x 0.706–0.740, y 0.838–0.906 (lower because of the tilt).
  - Each box is about **64 x 68 px** with about 14 px between them. Under each, a key label: **`LSHIFT`** at x
    0.669–0.695, y 0.915–0.938, and **`E`** at x 0.716–0.729, y 0.920–0.938. Labels are white caps in a thin
    light-outlined box with a dark translucent fill.
- Icons: Swift Step is a **running shoe/foot with motion lines and a swirl under it**. Protection Suzu is a
  **pinwheel/whirl of six curved blades** (the bell's crest).

| State | Box fill | Content | Seen |
|---|---|---|---|
| **Ready** | Off-white, a slight vertical gradient from #F4F4F4 at the top to #D4D4D6 at the bottom, with a thin light line just under the box. | Black icon (#111) | ASP t=150 H |
| **Pressed / casting** (just activated, cooldown not started yet) | **Coral/salmon** #E6876C, fading to pale pink #D9AFA3 | Black icon | ASP t=640, OWN (Swift Step, mid-cast) H, meaning M |
| **Cooldown** | Dark translucent grey (#575C59 over a dark scene). An **orange fill (#FF7F2D / #F77232) rises from the bottom** in proportion to elapsed cooldown, with a thin pale line (#EAD8D7) on top of the fill. | Remaining **whole seconds**, large, white, **italic condensed**, with a ragged black brush-stroke outline or splash behind it (e.g. `12`, `3`). No icon. | OWN (E at 12: bottom ≈15% orange), ASP t=330 (E at 3: ≈80% orange), t=680, 760 H |
| **Flash** (probably "just came off cooldown") | Bright cyan #B4FEFF with a royal-blue (#2B58DD) border and glow | White-ish icon | ASP t=800 M (meaning L) |
- Once picked, **perk icons** (white discs about 0.025 W with dark glyphs) appear above the right end of the
  weapon panels at y ≈0.80–0.84 [ASP t=330, 420] M.

### 1.4 Bottom right: two weapon/ammo panels [OWN, ASP] H
- **Healing Ofuda panel:** x 0.747–0.836, y 0.851–0.919. The icon (white cards with a "+", motion lines on the
  left) is at x 0.755–0.777. The text **`10 │ 10`** is at x 0.788–0.823, y 0.876–0.896.
- **Kunai panel:** x 0.840–0.971, y 0.863–0.935. The icon (a white kunai: leaf blade, wrapped grip, ring pommel,
  pointing left) is at x 0.853–0.910. The text **`9 │ 15`** is at x 0.929–0.957, y 0.896–0.912.
- **Panel shape:** each panel has only a **bright blue line along its top and its bottom** (#2D96DA–#2E9AE2,
  about 3 px, soft glow #153858, a small step near the ends). There are no side borders, and the inside is dark
  translucent. The lines break briefly between the two panels.
- **Ammo text:** current and maximum are the **same size**, white, upright, bold geometric sans, with a thin grey
  or teal divider. The ofuda count drops 2 per volley.
- **Mouse icon under each panel:** a small white mouse with the bound button painted orange.
  - Ofuda's icon sits at (0.815, 0.937) and kunai's at (0.950, 0.949).
  - **On the owner's screenshot, ofuda shows the RIGHT button and kunai the LEFT.** That is this streamer's
    remapping.
  - **Default binds are Healing Ofuda = Primary Fire (LMB) and Kunai = Secondary Fire (RMB)** [WIKI] H, so a
    default HUD shows LMB under ofuda and RMB under kunai.
  - The panel order (ofuda left, kunai right) is weapon order, not binding order (M).

### 1.5 Crosshair and reticle elements
- **Crosshair** at (0.5, 0.5): a small white "+" made of four short ticks with a gap in the middle, and a thin dark
  outline [OWN, ASP] H. Players can customise it, so this may be Aspen's setting (M).
- **Healing Ofuda volley bar** [OWN, ASP t=114–134] H:
  - **Position:** under the crosshair at x 0.463–0.540, y 0.613–0.639.
  - **Segments:** **5 slanted parallelograms** shaped like "/", each about 0.012 W x 0.025 H with about 0.003 W
    gaps, **bright amber #E6AA27** when loaded.
  - **Brackets:** a thin grey line starts at a small dot at each end (x 0.454 and x 0.548, at bar height), angles
    down and inward, then runs flat under the bar ("╲_ … _╱").
  - **Count:** 1 segment = 2 ofuda (one volley). At `4|10` there are 2 bright segments and 3 dim, so they drain
    from the right. Spent segments become dim translucent dark-amber outlines; at `0|10` all 5 are dim during the
    reload. When the clip refills, the segments flash pale or white.
  - This element was not in the 2022 Blizzard captures, which have no HUD, so it may be a newer addition (M).
- **Swift Step target indicator** [ASP t=114] H shape, M colour:
  - **Brackets:** four **cyan corner chevrons forming a diamond** (a square rotated 45°) about 0.07 W by 0.13 H
    (≈135 px), **centred on the targeted ally**, not on the crosshair.
  - **Labels:** a small cyan **`LSHIFT`** key label in an outlined box just under the diamond, and a small cyan
    "v" chevron above the ally's icon.
  - **Colour:** about #29A8E0, the same family as the ally chevron #5CC6DC.
  - The ally's own marker shows inside. In this case it was a red-and-white "+" icon, probably the low-health
    marker (L).
- **Ally markers** (always shown) [ASP t=116] H: a **cyan "v" chevron** above the ally, the **name in blue caps**
  above it (e.g. `FATE`), and above that a **small segmented white health bar**.
- **Objective marker:** a blue diamond with a white "P" inside a thin grey ring, plus a blue triangle on the ring
  pointing toward the payload [OWN] H.

### 1.6 Top of the screen [OWN] H
- **Objective, top centre:** x 0.379–0.620, y 0.029–0.074, in one row:
  - Our score `0`: dark box #0B1915 with a **cyan underline #31C4EF**, which marks our team.
  - Stacked time/progress `00:00` / `0.0%` on dark boxes.
  - Mode text `STOP THE PAYLOAD`: white caps on a dark box #0E1511.
  - Enemy box `4:45` / `29.78m`: **red #BA2B46**.
  - Enemy score `1`: red #BA2B46.
- **Payload bar below it:** y 0.087–0.142.
  - A white check mark in a diamond outline on the left (x 0.36–0.389).
  - The bar itself runs x 0.42–0.638: enemy progress in **red #F9355D**, the rest **teal-blue #107096**.
  - Checkpoint diamonds sit under the bar: cyan, and orange at the end.
  - Above the progress, a `②»P` icon shows the number of attackers pushing.
- **Kill feed, top right:** the first row is at x 0.707–0.985, y 0.032–0.065 (row ≈36 px); new rows stack
  downward.
  - **Killer box:** team colour fill (enemy **#9C283E**, ally **#13769E**) with the name in white caps and small
    hero portraits for the killer and assists.
  - **Middle:** a dark box with the white weapon/ability glyph and a `>` chevron.
  - **Victim box:** the victim's team colour, with portrait and name.
- **Performance overlay** (an option, tiny white text): `FPS: 289  LATENCY: 53 MS  3:12 PM` at the very top
  right, y < 0.015.

### 1.7 Fonts [FONT, OWN] M
- **Big numbers** (ult %, cooldown seconds, hero names and mode titles on the select screen) use a **tall
  condensed italic**: Big Noodle Too Oblique, the Overwatch face. It leans about 10–12°.
- **Everything else** (health and ammo numbers, `STOP THE PAYLOAD`, key labels, names) uses an **upright bold
  geometric sans in Futura style**, all caps.
- Blizzard has not published the exact UI faces. The free stand-ins are "Big Noodle Titling Oblique" (close) and
  Futura PT Bold/Demi or a similar face.

---

## 2. First-person view
FP viewmodels are their own animations and **look mirrored compared with third person** (see §3).

| Action | What the hands do | Src / conf |
|---|---|---|
| **Idle (arms crossed)** | **Right hand** is raised on the left of centre, fingers up, holding a **fan of 2–3 ofuda** pinched between thumb and index/middle finger. The talismans are white paper strips with a red round seal at the top and black brush kanji, at about x 0.15–0.35, y 0.38–0.62. The right forearm rises from the bottom centre. **Left forearm** lies **across the bottom** (x ≈0.37–0.68, y ≈0.75–0.95) with the **left hand at about (0.66, 0.86)** holding the **kunai**, thumb on the flat, blade pointing up-right to about (0.86, 0.67). Gloves are red, fingerless, with hard red back plates, black knuckle pads and black wrist bands; red forearm guards have a strap/flap; nails are red. There is a gentle breathing sway. | BLZ-SwiftStep 0–2 s, BLZ-Suzu, V7 t=170 · M-H (handedness M) |
| **Kunai throw (LMB/RMB as bound)** | Thrown by the **left hand**, the **same hand every throw** (no alternating), one throw per 0.5 s. **Wind-up:** the left hand pulls back to the lower left (x 0.05–0.2), open, back of the hand to the camera. **Throw:** the arm whips forward along a low arc to fully straight, aimed just right of the crosshair (hand ≈ x 0.5–0.65, y 0.55–0.7), palm down, fingers spread. The kunai leaves as a short **cyan streak**. While the kunai is being thrown, the ofuda hand drops out of view. | BLZ-Kunai 1.6–2.75 s, 3.6–4.6 s; V7 t=171–177 · H |
| **Healing Ofuda (toggle)** | **Right arm** reaches from the bottom right toward the target (hand ≈ x 0.5–0.6, y 0.55–0.75), back of the hand up. **Index and middle fingers flick** about every 0.15 s and a **soft golden smoke puff** pops at the fingertips each time. The right hand may first sweep across to take the fan from its raised pose. While firing, the kunai hand is out of view. | BLZ-Ofuda 1.75–2.7 s; V7 t=166.5–168.5 · H |
| **Swift Step (0.2 s cast)** | Both hands empty (the weapons vanish) and come up from the sides with fingers spread. They **clasp at the bottom centre** (x 0.45–0.55, y 0.6–1.0) in a **seal: fingers interlocked, both index fingers pressed together pointing straight up**. **Cyan spirit-flame wisps** flare on both sides and the hands glow white-cyan. Then the **whole screen floods with cyan/teal streaks** from the edges as you teleport. On arrival the idle pose returns with a pale cyan tint on the hands and talismans that fades in about 0.3 s. | BLZ-SwiftStep 2.3–3.1 s · H |
| **Kitsune Rush (cast ≈0.9 s)** | **(1)** The hands meet at the chest in a prayer-like seal (palms together, fingers up, bottom centre). **(2)** They spread apart sideways, palms down. **(3)** Both **thrust forward, palms out, fingers spread**, wrists bent back. **(4)** A **cyan spirit burst** erupts between the palms and the fox leaps out. Afterwards, while standing in the path, a **white-cyan misty vignette** fills the screen edges, light-blue streaks fly past, and teal glyph tiles cover the ground ahead. | BLZ-KitsuneRush 0.3–1.2 s, 3.5–4.1 s · H |
| **Protection Suzu** | Not captured in first person. Third-person and VFX details are in §4. | not verified |
| **Wall climb** | The camera pitches up at the wall and the **weapons are hidden**. **Empty gloved hands alternately slap or grab the wall** in the lower half of the screen: left hand upper left (x 0.1–0.3), right hand right of centre (x 0.4–0.6), about 0.15 s per hand. At the top she mantles over and the idle pose returns. | V7 t=100.7–101.9 · H |
| **Quick melee / reload** | No clips are labelled, but V7's first-person chapter has three unexplained moves. **(a)** t=161.2–162.6 (≈1.4 s, close to the kunai's 1.3 s reload): the right hand rises in a **one-hand seal** (index and middle fingers up) left of centre, then both hands drop out of view and come back. Possibly the reload. **(b)** t=163.9–164.4 (≈0.5 s): the right arm swings out to the right edge, then **jabs a closed fist straight forward** at the centre. Possibly the quick melee. **(c)** t=178.6–179.6: the hands drop away, then the **right hand sweeps right to left across the centre with a kunai, blade first**. A slash, so possibly the melee instead. | V7 · L (watch those timestamps to settle it) |

---

## 3. Third person
- **Outfit (default skin)** [Blizzard hero page, BLZ, V7] H:
  - Top: a white short-sleeved kimono-style top with red inner collar and trim, a red pinwheel crest on the left
    chest, and a dark harness/backpack strap over the shoulders.
  - Waist: a dark obi under a red cylindrical waist pouch, wrapped with a row of large white beads.
  - Lower half: **long red front and back panels** (the "skirt": a hakama-like tabard split at the sides) from
    the waist to mid-calf, over red trousers, and red-and-white sneakers.
  - Head and hands: a **red fox mask worn up on the head** (ears up, white markings), black hair with a teal sheen
    in a high bun, and red fingerless gloves with forearm guards.
- **Idle stance** [V7 t=3, 112] H:
  - She stands with her weight on one hip.
  - One hand (her **right**) is raised **overhead** with an ofuda between index and middle finger, index finger
    pointing up; the paper flutters and its edges glow faint yellow-green.
  - Her other hand (her **left**) is low by the hip with the **kunai pointing down and outward**.
  - There is also a crouched idle with the talisman held forward [V7 t=34].
- **Throws** [BLZ-Ofuda 0–1.4 s, BLZ-Kunai] M:
  - Ofuda: the right arm thrusts straight at the target with a finger flick.
  - Kunai: an overhand or sidearm whip; the kunai also appears in her right hand in some shots.
  - So in third person the hands are not strictly fixed. First person is consistent (§2).
- **Cloth** [V7 1:24–1:39, BLZ-KitsuneRush 8.2 s] M:
  - The front and back panels **trail behind and flap hard** when she runs, and swing on turns. At idle they hang
    and sway a little.
  - The bun and the fox-mask ears are rigid.
- **Wall climb** [K + FP evidence] L: like Genji and Hanzo, she faces the wall and climbs **hand over hand**, legs
  scrambling, with the weapons stowed. The panels hang down and back.

---

## 4. Projectiles and VFX
- **Team colours** [BLZ-Suzu, V7 t=106–111] H: from your own or an ally's view, Kiriko's ability effects are
  **cyan/teal**. Enemies see the same effects **red/magenta**.
- **Kunai** [BLZ-Kunai, Blizzard hero art, HUD icon] H/M:
  - **Model:** a dark gunmetal, **leaf/diamond-shaped double-edged blade** about the length of a hand, with
    **cyan edge highlights**, a cord-wrapped grip and a **ring pommel**.
  - **In flight:** a fast, small, mostly straight streak with a **short cyan/teal trail** (90 m/s in [WIKI], so
    almost hitscan at short range).
  - **On hit:** small red impact sparks on the enemy.
- **Healing Ofuda** [WIKI H, BLZ-Ofuda H]:
  - **Paper:** white strips about 1:3, with a red round seal and black kanji.
  - **Throw:** a puff of golden smoke at the hand.
  - **Locked onto an ally:** the talismans glow **yellow/gold** and leave **swirling yellow spiral trails** that
    wrap around the ally. Heal hits pop small **yellow "+" sparks**.
  - **No target:** they glow **light blue/cyan** and fly straight ("yellow = seeking, blue = no target").
  - **Flight:** "breezy and slow". The wiki disagrees with itself on speed: the infobox says 18 m/s and the
    Urgent Care perk text says 30 m/s. Check the patch notes before using either. They shatter on terrain.
- **Protection Suzu** [WIKI, BLZ-Suzu] M:
  - **Bell:** a small **shrine bell (suzu)** thrown in an arc (37 m/s, gravity).
  - **On impact (ground or ally):** a **burst of cyan light filling a sphere of 4 m radius**, with teal wind
    swirls along the ground.
  - **Affected allies:** for 0.5 s they turn into **bright white glowing silhouettes** (phased look) with a few
    yellow ribbons/sparks. Enemies see the burst and the glow in red/pink.
  - I did not see the bell's exact colours up close (L).
- **Kitsune Rush** [BLZ-KitsuneRush] H:
  - **The fox:** a large **translucent ice-blue/cyan spirit fox**, faceted or crystalline, with a long flowing
    tail. It bounds forward along the ground for about 32 m.
  - **The path:** about 10 m wide, marked by **teal glyph tiles on the ground** and a row of **translucent cyan
    torii gates** along it. It lasts 8 s.
  - **Allies in the path:** cyan wisps around them, and the screen effects from §2.
- **Swift Step from outside** [BLZ, K] M: a cyan flash and spirit wisps where she leaves and where she arrives at
  the ally.

---

## 5. Sounds
I could not listen to the audio. **Voice lines are quoted from the wiki (H); the sound-effect descriptions are
general recollection (L).** Check them against [V7], which has a "Sounds" focus and in-game audio.
- **Kunai** (L): a short, sharp metal "fwip" on the throw and a dull thunk on a body hit. **Headshot:**
  Overwatch's standard **critical-hit ding**, a bright metallic "tink". Kiriko's kunai crits are known for it.
- **Ofuda** (L): soft paper flutters and a breezy whoosh, a light chime/shimmer on each heal hit, and a quick
  papery reload.
- **Protection Suzu** (L): a **bell jingle** as it flies and a large chime/gong-like ring with a cleansing whoosh
  on impact.
- **Swift Step** (L): a quick spirit whoosh with a crystalline shimmer, and a small chime on arrival.
- **Kitsune Rush** (L): a fox cry/yip, rushing wind, and a deep shimmer as each gate appears.
- **Voice lines** [WIKI Kiriko/Quotes] H:
  - **Ultimate**, heard by herself and allies: "**Let the Kitsune guide you!**"
  - **Ultimate**, heard by enemies: "**Kitsune no kagizume o tokihanate!**" (狐の鉤爪を解き放て！, "O fox
    spirit, unleash your claws").
  - **Swift Step:** "*chuckle* Hey.", "I'm here.", "I'm with you.", "What's up?", "Daijōbu?", "Koko yo."
  - **Suzu:** "Hear my bell.", "Be purified!", "Cleansed.", "You're all good!", "Jōka suru!" (Cleansed!),
    "Kanezaka-jinja protects you."
  - **Healing Ofuda:** "Be healed!", "Carried on the wind.", "Like a gentle rain.", "Restoring balance."

---

## 6. Hero select screen (2026 build) [ASP t=500] H
- **Background:** the selected hero's **3D model, large and centred** (hips up, about 70% of the screen height),
  in front of a blurred view of the map.
- **Top left:** a big translucent white italic condensed title **`COMPETITIVE`** (the mode), with a mode icon and
  red italic **`ATTACK`** under it, then the map name `KING'S ROW` in small white caps.
- **Top right:** the hero name **`KIRIKO`** in huge white italic condensed type, with the sub-role
  **`MEDIC`** and its purple icon under it. Below that, `SELECT SKIN` and a dropdown showing the skin name in
  orange.
- **Team row:** y ≈0.51 (names at ≈0.60). The 5 teammates' portraits sit in rank-emblem frames (flaming frames
  for high ranks), with player names in blue under them.
- **Roster:** y ≈0.68–0.87. Hero portrait squares (≈0.029 W x 0.055 H each) in three role groups, each with a
  role icon, a caps header and a thin rule:
  - **TANK** (x 0.02–0.30), sub-roles INITIATOR, BRUISER, STALWART.
  - **DAMAGE** (x 0.315–0.68), sub-roles SPECIALIST, SHARPSHOOTER, FLANKER, RECON.
  - **SUPPORT** (x 0.695–0.975), sub-roles TACTICIAN, MEDIC, SURVIVOR.
  - Sub-role labels sit under each cluster.
  - The selected hero gets an **orange glowing frame** with its name tag under it.
  - Banned heroes show dark red squares with a red "no" symbol.
- **`READY`**: an orange button at about (0.50, 0.91).
- **Bottom bar:** hotkeys in small dark key boxes. `NUMPADENTER CHAT` and `F1 HERO DETAILS` on the left,
  `LCONTROL PLAYER CARDS` and `ESC MENU` on the right.
- **Abilities are not listed on this screen.** They are behind **F1 "HERO DETAILS"**.
- **After locking in** [ASP t=508–515]: the roster disappears and an orange `CHANGE HERO` button sits at the
  bottom centre.

---

## 7. Comparison checklist (what a Minecraft-style HUD will most likely get wrong)
1. A flat HUD versus Overwatch's **curved one**, where both side groups tilt with their inner ends higher.
2. **Health:** hearts, or a plain bar, versus **white 25-HP pips** (taller than wide) plus `199 │ 225` (large
   current value, small maximum).
3. **Abilities:** a Minecraft cooldown overlay (a grey sweep) versus Overwatch's four states:
   - **white box with a black icon** when ready;
   - **coral** while casting;
   - **grey box with an orange fill rising from the bottom and a large italic seconds number** on cooldown;
   - a **cyan flash** when it comes back.
4. **Ultimate:** a number or bar versus a **circle of radial orange ticks filling clockwise with an italic %**,
   which turns into a **blue-ringed white disc with a fox emblem** when ready. The `Q` label inverts.
5. **Ammo:** an item count or durability versus **two blue-lined panels** (icon + `10 │ 10`, icon + `9 │ 15`),
   each with a **mouse icon whose bound button is orange**. Default binds are ofuda **LMB**, kunai **RMB**; the
   owner's screenshot has them swapped.
6. **Missing near-reticle elements:** the **5-segment amber ofuda volley bar** under the crosshair, and the
   **cyan diamond brackets with an `LSHIFT` label on the Swift Step target**. Ally markers are a cyan chevron,
   the name in blue, and small health pips.
7. **First person:** Minecraft's single held item versus Kiriko's **crossed-arms idle** (right hand up-left with
   the talisman fan, left hand low-right with the kunai). Kunai are thrown by the **left** hand, ofuda flicked
   by the **right**, and both hands form the **seals** for Swift Step and the ult.
8. **Type:** Minecraft's pixel font versus **italic condensed** numbers (ult, cooldowns) and a **bold
   geometric sans** for everything else.
9. **Clutter:** the hotbar, XP, hunger and armor bars must be hidden in hero mode. The bottom centre holds only
   the ult circle.
10. **Effect colours:** friendly Kiriko effects are **cyan/teal** (the yellow is only on ofuda locked onto a
    target). Enemies see red.

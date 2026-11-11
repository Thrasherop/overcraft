# Play of the Game: the reference clip, measured

The spec for recreating Overwatch's Play of the Game, measured frame by frame from the owner's recording of a real one
(2026-11-11). Up: [Techniques](../README.md) · the design: [design](design.md) (#44) · method:
[visual_review](../testing/visual_review.md).

- **Source:** `local-assets/raw/potg_clips/kiri_potg_2026-11-11.mp4` in the main checkout: the owner's own footage, 32.5 s,
  2560x1440, 60 fps, stereo 48 kHz. Gitignored; never commit it or frames from it. A Kiriko POTG on Eichenwalde.
- **Frames, crops and measurements** (not committed): `E:\claude\mods\scratch_potgclip\`. Key crops:
  `crop_intro_16.png` (intro text), `crop_banner_22.png` (replay banner), `crop_card_full.png` (player card),
  `full/t_*.png` (full frames by time), `spec.png` (spectrogram), `yavg_summary.txt` (mean luma per frame).
- **Placeholders:** `<PLAYER>` is the POTG player's name, `<TITLE>` their player title, `<HERO>` the hero,
  `<ALLY1..>`/`<ENEMY1..>` other players. No real names are copied here.
- **How measured:** times from ffmpeg's frame times (frame = time x 60); positions are pixel bounding boxes from colour
  thresholds, as fractions of width W = 2560 and height H = 1440; colours are median pixel values (hex). Sizes are
  capital heights. Easing is read from per-frame luma or bounding boxes and is approximate.
- **The owner's input, not the UI:** the mouse cursor, and the endorsement panel 16.5 to 19.2 s (they pressed N). The
  scoreboard 10.95 to 12.85 s is probably the owner holding Tab: it's drawn over a dimmed screen with the
  "STAY AS TEAM?" prompt faint beneath it. Treat it as input, not part of the sequence.

## Timeline
| Time (s) | Frames | Phase | What's seen |
|---|---|---|---|
| 0.00-2.75 | 0-164 | Last moments of the match | First person, the full HUD. |
| 2.75-3.40 | 165-204 | VICTORY | A white-blue flash fills the screen (luma 69 to 215 in 4 frames). A huge "VICTORY!" scales down onto its place with horizontal light streaks. Settled by 3.4. |
| 3.40-7.35 | 204-441 | VICTORY held | "VICTORY!" held over live first-person play; the chat opens on the left. |
| 7.35-8.00 | 441-480 | Dip | Luma 102 to 43 to 102: a dip to dark (bottom at 7.6), then up. |
| 8.00-10.95 | 480-656 | Team lineup | The winning team posed in the map, each with their name and rank icon above their head. |
| 10.95-12.85 | 657-770 | (Scoreboard: probably the owner's Tab) | Full scoreboard over a dark backdrop. |
| 12.85-13.03 | 771-782 | Dip to black | One bright frame (771), then black (776-782, luma 17). |
| 13.03-13.30 | 782-798 | Intro fades in | Black to full in about 0.27 s. |
| 13.05 | 783 | **Audio: announcer and hit** | Broadband hit and the announcer's "Play of the game." (subtitled `[ATHENA] Play of the game.`), 13.1 to about 14.3 s. |
| 13.15-17.97 | 789-1077 | **Highlight intro** with the title text | The hero's highlight intro in a set (third person, a still-ish camera on the hero). Text builds in on the left (below). |
| 16.50-17.95 | 990-1077 | (Endorsement panel: the owner's N) | Scene dims to about 45% luma; panel at the bottom. |
| **17.967** | **1078** | **Cut to white** | Hard cut from the intro to full white (luma 235, pure white) in 2 frames, over everything. |
| 17.97-18.27 | 1078-1096 | White held | 0.3 s. |
| 18.27-18.77 | 1096-1126 | **White fades into the play** | Ease-in: slow at first, fast at the end (per-frame luma drop 6 at the start, 20 at the end). |
| 18.40 | 1104 | Audio: the play's game sound starts | RMS -42 to -34 dB. |
| 18.77-30.25 | 1126-1815 | **The play** | First person from the POTG player, their full HUD, plus the POTG overlay (banner, player card). 11.5 s visible (12.3 s counting the white). |
| about 19.1-19.25 | 1143-1155 | Banner fades in | Text fades in over about 0.2 s (the dark band and its rule are already there at 19.0). |
| about 29.5 | 1770 | "5 PLAYER KILL STREAK!" | The game's own streak callout, top centre, part of the replayed HUD. |
| **30.267** | **1816** | **Hard cut to black** | No fade, no pull-back. Black held 0.6 s (1816-1851). |
| 30.87-31.10 | 1852-1866 | Fade into the end screen | Black to full in about 0.2 s. |
| 31.10-32.20 | 1866-1932 | End screen cards reveal | Four cards, one every 0.3 s, each with a team-coloured light streak. |
| 32.2-32.5 | 1932-1950 | End screen held | (The clip ends.) |

So the POTG proper runs **13.03 to 30.27 = 17.2 s**: the intro 4.93 s (13.03 to 17.97, as the intro clips' 4.97 to 5.0 s
in [design](design.md#what-the-viewer-sees-and-hears)), white 0.3 s, then 12.0 s of play from the white's fade.

## 1. The highlight intro and its title text (13.03-17.97)
The scene is the hero's highlight intro in 3D, not a card on a band. No dark band, no backdrop. All text is
**left-aligned, stacked, each line stepped a little right** (following the italic's slant). Positions at 16.0 s:

| Element | Words, case | Font look | Size (cap height) | Left edge | Top, baseline | Colour |
|---|---|---|---|---|---|---|
| Title | `PLAY OF THE GAME`, caps | Big Noodle, oblique | 56 px = **3.9% H** | x 198 = **7.7% W** | y 266-322 = 18.5%-22.4% H | `#CFDAE5` (light blue-grey) |
| Name | `<PLAYER>`, caps | Big Noodle, oblique | 99 px = **6.9% H** (6.1% at 14.0 s) | x 268 = **10.5% W** | y 349-447 = 24.2%-31.0% H | `#FF9721` (orange) |
| Player title | `<TITLE>` ("Firecracker"), mixed case | Futura demibold-like sans, upright | 18 px x-height+caps, about **1.3% H** | x 285 = 11.1% W | y 459-477 = 31.9%-33.1% H | `#FF963B` (orange) |
| Hero line | `AS <HERO>`, caps | Big Noodle, oblique | 44 px = **3.0% H** | x 324 = **12.7% W** | y 507-550 = 35.2%-38.2% H | `#CFDAE6` |
| Rank emblem | the player's competitive rank icon | image | about 80x100 px (5.5%x6.9% H) | x 520, right after `<HERO>` | y 480-578, centred on the hero line | silver and blue |

Light shadow under all of it (soft, dark, a few pixels down-right); no outline.

**How it builds in** (frame numbers, 60 fps):
- 782-798: the scene fades up from black.
- 789: `PLAY OF THE GAME` starts to fade in (faint at 789, readable at 792).
- 792-801 (0.15 s): a **blue-white lens-flare streak** sweeps left to right across the title line, a thin horizontal
  line about the full width with a bright glow where it crosses the text.
- **813: the name pops in**, about **1.5x size** (height 138 px against 88 settled) and motion-blurred to the right,
  then **shrinks to size by 827** (0.23 s), ease-out (the width falls 719, 675, 639, 579, 539, 496, 464 px on frames
  814 to 827, fast then slow). Its left edge moves right 42 px as it shrinks.
- About 834-840 (13.9-14.0 s): `<TITLE>` fades in under the name.
- 14.4 s: `AS` appears; 14.5-14.6 s: `<HERO>` appears after it (word by word, each a quick fade).
- 14.8 s: the rank emblem appears.
- **All the way through, it drifts**: the block slides right about 11 px/s (0.43% W/s) and grows slowly, the title about
  2%/s, the name about 5%/s (the title's width 394 px at 14.0 s, 410 at 16.0; the name's 457 at 13.9 s, 507 at 16.0).
- **Out:** none. The text is on screen until the cut to white at 17.967.

## 2. The cut into the play (17.967-18.77)
- A hard cut to **pure white over everything** (the HUD and the panel included), held **0.3 s**, then a **0.5 s fade**
  out of white into the play, ease-in (white leaves slowly, then fast).
- The play starts already running: no "replay" title, no rewind effect, no letterbox.

## 3. The play (18.77-30.27)
**Camera: first person throughout**, the POTG player's own view with their whole HUD: health and portrait bottom left,
ult meter bottom centre, abilities and ammo bottom right, the crosshair, hit markers, the kill feed, the objective
timer, chat and subtitles, the game's own callouts (`5 PLAYER KILL STREAK!`). **No pull-back, no third person, no slow
motion** in this play. It ends on a kill streak (the last kill about 29.3 s, the callout 29.5) about 0.8 s before the
cut; the hero doesn't die. It ends with a **hard cut to black**, mid-action.

**The POTG overlay**, drawn over the replayed HUD (measured at 22.0 s):
- **Top band:** full width, y 0-167 (**11.6% H**), the scene darkened to about 30% (black at about 70%). A **light rule**
  under it, y 168-173 (0.35% H), the scene brightened by about +35 luma (white at about 25 to 30%). No gradient: flat.
  The objective's own timer ("DEFEND OBJECTIVE A 2:35") sits inside the band, top centre.
- **Banner line:** `PLAY OF THE GAME <HERO>` + rank emblem, top left.
  - `PLAY OF THE GAME`: caps, Big Noodle oblique, **cap 46 px = 3.2% H**, `#FFFFFF`, left x 57 = **2.2% W**, cap top
    y 44 (3.1% H), baseline y 89 (6.2% H). A soft dark drop shadow.
  - `<HERO>` (`KIRIKO`): same size and baseline, one space after, orange **`#FF7712`** (redder than the intro's
    `#FF9721`), x 407-532.
  - Rank emblem: right after the hero's name, about 60 px tall, centred on the line.
- **By line:** `BY <PLAYER>`, caps, Futura demibold-like, **cap 16 px = 1.1% H**, `#FFFFFF`, left x 55, y 113-128.
- **Capture prompt:** `PRESS F9 TO CAPTURE PLAY OF THE GAME`, caps, Futura demibold-like, cap 19 px = 1.3% H, white,
  **right-aligned at x 2504 = 97.8% W**, y 74-92. (An Overwatch-client feature; the mod has nothing to capture with.)
- **Player card**, right side, the whole play: x 2131-2494 (**83.2%-97.4% W**, 14.2% W wide), y 427-990
  (**29.7%-68.8% H**). Dark navy `#09101E` fill, a **cyan border `#17BDEA`** with a glow, rounded corners. Top to
  bottom: the player's portrait (their icon) in a flame-and-gold frame with a star crest, a `+` (add friend) and an
  endorsement-level badge at its upper corners; `LEVEL <n>` (white, Futura, small); `<PLAYER>` (Big Noodle oblique,
  cyan `#06BDEE`, cap 26 px = 1.8% H); `▲ <TITLE>` (cyan, small sans); three most-played hero portraits in circles with
  a number under each.
- **Timing:** the band, rule and card are there on the play's first frames (under the white); the banner's text fades in
  about 19.1-19.25 s (0.35 s after the white clears), with a slight drift right. Nothing animates out: the cut to
  black ends it all.

## 4. The end screen, the teams presented (30.87 on)
Not a 3D map shot: a flat void.
- **Fade from black**, about 0.2 s (frames 1852-1866), after 0.6 s of black.
- **Header bar:** y 0-180 (**12.5% H**), dark navy `#0A1020` to `#0F1727`, a dark rule `#051435` at y 180.
  - `VICTORY` caps, Futura-like bold, **`#00ADFF`**, cap 53 px (3.7% H), left x 67 (2.6% W), y 62-115.
  - To its right, two small lines: the map name, `MATCH TIME: 5:00`.
  - Right: `LEAVING GAME IN: <n>` with a large count, `LEAVE GAME`, `REQUEUE` buttons.
- **Background:** a flat blue-grey `#5B76A9` to `#6985B6`, a faint large triangle pattern at the top, a glossy floor
  with reflections.
- **Four cards, two per team: allies left, enemies right.** Card centres at **16.4%, 36.4%, 64.5%, 83.6% W**; each about
  14.8% W wide, y about 1000-1250 (69%-87% H), a translucent dark panel with a **team-coloured bottom rule** (allies
  `#037ABE` blue, enemies `#A64C61` red). In each, top to bottom:
  - `VOTE` button and a team-coloured round vote counter (just above the card).
  - `<NAME>` caps, Big Noodle oblique, white, cap 37 px (2.6% H).
  - The stat: `6,596`-style number, Big Noodle oblique, white, about 72 px (5.0% H).
  - The stat's name, caps, Futura-like, **orange `#D87800`**, cap 18 px (1.25% H): `HEALING DONE`,
    `NEGATIVE EFFECTS CLEANSED`, `HERO DAMAGE DONE`.
  - Above the card, the player's hero standing in a pose (their emote or victory pose), full size, in the void.
- **The reveal:** cards come in **one at a time, 0.3 s apart, left to right** (31.1, 31.4, 31.7, 32.0 s), each with a
  **horizontal light streak in the team's colour** (cyan for allies, red for enemies) across the card for about 0.2 s,
  the hero materialising with it. All four settled by about 32.2 s.
- **The rest of the teams:** small, faded portraits of the other players in a column at each edge (x about 3% and 97% W),
  five a side.

## 5. Audio
From the spectrogram (`spec.png`) and RMS in 50 ms windows. Not compared with the extracted files (nobody has heard
those): the matches below are by time and shape only.
- **Music:** tonal bands (about 550-900 Hz) run continuously from about 3.5 s (after VICTORY) to the end, under the
  lineup, the intro, the play and the end screen. No clear new start at the intro. The cue the mod plays from the card,
  5B1F (16.84 s), started at the intro (13.03) would end at 29.9, 0.4 s before the cut: consistent, not proven.
- **Announcer "Play of the game."** (A8C4, 1.66 s): starts at **13.05-13.1 s**, with the intro's first frames, voice energy
  to about 14.3 s (1.2-1.6 s). The subtitle `[ATHENA] Play of the game.` shows then. So the line starts with the
  fade-in, not after the text.
- **A hit/whoosh** with the line at 13.05 (broadband, low end), and a second low hit about 14.3 s.
- **The play's own sound** (gunfire, voices) starts at **18.40 s**, during the white's fade, not at the cut.
- The intro's own sound (`2F6C`, 2.83-3.67 s into the intro: [design](design.md#what-the-viewer-sees-and-hears)) would land
  at 15.9-16.7 s; nothing distinct stands out there in this clip (the dimming panel is over it).
- Through the black (30.27-30.87) the sound carries on (-39 dB): no silence at the cut.
- The end screen: whoosh-like peaks with the card reveals (31.4, 32.1 s).

## 6. What the mod has against each (`PotgCard`, `ClientPotg`, as of `83cb038`)
| Reference | The mod now |
|---|---|
| A 3D highlight intro of the hero, 4.93 s | A 2D card on a dark band (`PotgCard`), about 4.8 s; the intro is "the next step" in `ClientPotg` (`SOLO`). |
| Text left-aligned at 7.7% W, stacked and stepped right | Centred at 50% W, 42% H. |
| `PLAY OF THE GAME` 3.9% H, `#CFDAE5` | Big Noodle (`NUMBERS`), size 120 in GUI units, white. |
| `<PLAYER>` alone on its own line, 6.9% H, `#FF9721`, popping in at 1.5x and settling in 0.23 s | `<PLAYER> AS <HERO>` on one line, size 52, `#F7A54A`, sliding 40 units in 0.3 s. |
| `<TITLE>` (player title) small orange sans | None (no player titles in the mod). |
| `AS <HERO>` its own line, 3.0% H, `#CFDAE6`, word by word, then the rank emblem | Joined to the name line; no emblem. |
| No category word anywhere | A category line (`HIGH SCORE`, `LIFESAVER`), Futura, white 80%. |
| No band; a lens-flare streak across the title | A black band (60%) and an orange rule (`#F7A54A`). |
| A slow drift right and growth all through | Static after the slide-in. |
| No fade out: a cut to white 0.3 s, a 0.5 s ease-in fade into the play | A 0.5 s fade out, then the replay. |
| Play: first person, the player's full HUD, no camera moves | The replay is from the hero's first person through a replay backend (design step 3; the camera is in progress elsewhere). |
| Banner: top band 11.6% H, rule, `PLAY OF THE GAME <HERO>` + emblem, `BY <PLAYER>` | None during the replay. |
| Player card on the right | None. |
| Hard cut to black, 0.6 s black, 0.2 s fade into the end screen | The replay ends and the world returns; the exit portal opens. |
| End screen: four cards, staggered reveal | None (VICTORY comes before, from `VictoryHud`). |
| Announcer at the intro's first frame, music under all of it | Both start with the card (`PotgCard.show`): the right moment. |

## 7. What's missing (to build, in order of what a player sees)
1. **The intro scene:** the hero in third person playing a highlight intro, 4.93 s, in place of the black band.
2. **The intro text** as in §1: the left stack, the sizes and colours, the name's pop-in, the word-by-word hero line, the
   flare across the title, the slow drift. Drop the category line and the band. Keep `" AS "` (D 0DC6) but on its own
   line.
3. **The cut to white** (0.3 s held, 0.5 s ease-in fade) between the intro and the play.
4. **The replay overlay:** the top band and rule, `PLAY OF THE GAME <HERO>`, `BY <PLAYER>`, fading in 0.35 s after the
   white; the hero's name in `#FF7712`.
5. **The player card** on the right: ours would need a stand-in for level, portrait and most-played heroes (the
   player's skin head, the hero icons the mod has, hero-mode time per hero?): the owner's call.
6. **The end:** hard cut to black, 0.6 s, fade 0.2 s.
7. **The end screen** (team presented): a header bar with `VICTORY`, up to four cards staggered 0.3 s with team-coloured
   streaks, each hero posed above its card with one stat. Minecraft has one team, so who fills the enemy side (nobody?
   the dragon?) is the owner's call.
8. Optional, the owner's call: the rank emblem (no ranks in Minecraft), `<TITLE>` (no titles), the capture prompt (no
   capture), the team lineup before the POTG (8-11 s), endorsements.

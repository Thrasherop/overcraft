# Play of the Game: the look, built from the clip

What a player sees around the replay, built from the owner's clip ([potg_reference](potg_reference.md) §1-§4, §7 items
1-7) on 2026-11-11 (#44). Up: [Techniques](../README.md) · the design: [design](design.md) · the clip, measured:
[potg_reference](potg_reference.md) · animations from the data: [emote_reproduction](../emotes/emote_reproduction.md).

## What plays, in order (POTG at the dragon's tick 199)
| s from the card | What | Where it comes from |
|---|---|---|
| 0-2.95 | **Team lineup**: every viewer's hero posed (the select screen's showcase hold, each at its own point), the name over the head in light blue; a 0.25 s lift from 60% black | the clip 8.00-10.95 s; pose and spacing ours |
| 2.95-3.13 | black | the clip's dip 12.85-13.03 |
| 3.13-8.06 | **Hero intro**: the hero's highlight intro clip from the data (`potg_intro`), a still low camera, feet below the frame; text on the left: `PLAY OF THE GAME` (3.9% H, `#CFDAE5`, 7.7% W), the name popping in at 1.5x and settling in 0.23 s (6.9% H, `#FF9721`), `AS` then the hero word by word (3.0% H), the flare 0.17-0.32 s, the block drifting 0.43% W/s and growing; announcer and music start here | §1, frame by frame |
| 8.06 | **Cut to white**, held 0.3 s (longer while the replay's login screen is up), then 0.5 s ease-in (1-u²) | §2 |
| play | **Overlay**: band 11.6% H black 70%, rule 0.35% H white 27%; `PLAY OF THE GAME <HERO>` 3.2% H at 2.2% W, the hero `#FF7712`; `BY <PLAYER>` 1.1% H; text fades in 0.33 s after the white clears over 0.2 s. **Player card** 83.2-97.4% W, 29.7-68.8% H, `#09101E` fill, `#17BDEA` border and glow: the hero's portrait, the name (cyan `#06BDEE`), MOST PLAYED with three heroes and the hours | §3; card contents the owner's stand-ins |
| play's end | **Hard cut to black** (the server's CUT), 0.6 s (longer while the return's loading screen is up), 0.2 s fade | §2/§6 |
| then | **End screen** 6 s + 0.3 s fade (ours): blue-grey void `#5B76A9`-`#6985B6`, header 12.5% H with `VICTORY` `#00ADFF` and `THE END`; cards 14.8% W at 16.4/36.4 \| 64.5/83.6% W, one every 0.3 s with a team-coloured streak; the figure above each card; name, stat, label `#D87800` | §4; THE END, the floor tint and the hold ours |

A highlight watched alone: no lineup, no end screen; `HIGHLIGHT` (ours) in place of `PLAY OF THE GAME`; black, then back.

## How it's built
- **`PotgCard`** draws it all, from **`GuiPotgMixin`**: last in `Gui.extractRenderState`, after the screen, before toasts,
  so the white and the black cover the replay's `LevelLoadingScreen` (found in play: under the old screen-drawn card the
  loading screen showed through). The holder screen only takes input now.
- **Figures** are the select screen's preview: `InventoryScreen.extractEntityInInventoryFollowsMouse` on the local
  player with `HeroModelRenderer.preview(hero)`; `previewClip`/`previewClipAt` play a named clip instead of the showcase
  (the intro), `previewShowcaseAt` sets the hold's time. Picture-in-picture boxes are raw GUI coordinates (the pose
  matrix doesn't reach them). Mobs: a client entity per type, **given an id** (`setId`): a fresh entity has none and the
  zombie renderer read it, which crashed both clients (found in play). Draws are wrapped so a figure can't take the
  game down.
- **The HUD and hand** are hidden as F1 hides them (`Hud.toggle`) except during the play.
- **The server** (`PotgReplay`): CARD carries JSON (`showData`): player, hero, most played (`PlayerStats`), lineup,
  cards. The replay starts at 8.06 s (lineup + dip + intro); solo at 5.11 s. **CUT goes through the viewer's own
  channel** (`ReplayViewable.arcade_sendReplayViewerPacket`): a viewer isn't in the player list while watching, and a CUT
  sent there never came (found in play). The world stays frozen through the end screen (`OUTRO_MS`, thawed in `tick`).
- **`PlayerStats`** (`overcraft/potg/players.json`): seconds in hero mode per hero per player (the card's most played), and
  the run's damage, healing and eliminations since the last POTG (counted where PlayScore counts; no scoring value
  changed). Each player's card stat: healing if above damage, else damage, else eliminations (ours).
- **Enemy cards**: from `mobs_last.json` (`MobStats.last`): the dragon always, then the mob with most players killed (then
  damage), if it did anything.
- **Highlights menu** (`HighlightsScreen`): `K` (ours) or hero mode's Esc menu; the server answers with the player's own top
  5, or "locked" until the world's `EnderDragonFight.hasPreviouslyKilledDragon()`. Enter or a click plays one alone.

## The intro from the data
Kiriko's intros were already extracted (`E:\Overwatch Archive\research\kiriko-anim\extract_unlocks\Heroes\Kiriko\HighlightIntro`,
build 154088). A survey (`owm_survey.py --survey --clips-from`) of the single-clip ones: all 150 frames at 30 fps (5.0 s).
**Healing Ofuda** (DF8F) was picked: she stands in place (root travel 0.23 m) and fans her own ofuda (her model's pieces),
so no prop is missing. Exported clips-only as `hero_models/kiriko_intro_clips.glb` (`tools/clipmaps/kiriko_intro_clips.txt`
has the command), merged by `HeroModels` like `_clips`. Its two models (0797, 13CA) and its animation effect (sounds,
`2F6C`) aren't read yet. **Every other hero has no intro extracted**: they fall back to the select screen's showcase
(intro then hold), which is from the data but isn't their highlight intro: ours as a stand-in. Lúcio's are extracted
(`research\lucio\extract\unlocks\Heroes\Lúcio\HighlightIntro`), not exported.

## Played (2026-11-11, two dev clients over LAN, Overwatch and Battle.net checked closed, idle 28 min, the lock held)
Raw driver output: [runs_look](runs_look/). Screenshots in the worktree's `run/screenshots` and `run2/screenshots`
(`potg2_*`, `solo1_*`, `solo2_*`, `hl_menu_*`, `solo_potg4_*`), not committed (Overwatch models in them).
- Kiriko (host) and Mercy (LAN) fought zombies; a zombie and a slime killed players (mob stats); a named zombie
  "Gerald" killed Mercy by `/damage ... by` it (not a real fight). The dragon (health set to 12-30) was killed by the
  **real left button** (`potg_mouse.ps1`), aim by server `tp`, twice.
- Run 1: lineup, intro, white, play all right; the CUT never arrived (loading screen under the banner), then the end
  screen crashed both clients on the zombie's card. Both fixed.
- Run 2 (`look_potg2_out.txt`): on **both clients** the lineup (Kiriko and Viewer as Mercy), the intro, white, the play
  with the banner and card, the cut to black, the end screen (Kiriko 137 damage, Viewer 0, Ender Dragon, Gerald 1 PLAYER
  KILLED), the world thawed 7.1 s after; both back.
- Highlights by the K key: the menu, Enter on the first: intro, white, the overworld play, black, back in the End
  (client 2 untouched, `solo1_2_9s`). Saved and quit, client restarted, world reopened: K, the second: plays.
- The dragon fitted to its card was a speck: widened (`solo_potg4_1_24.5s`, via `/overcraft potg now`).

## Not shown, or not done
- **First-person arms, weapon and HUD inside the replay**: not drawn. The viewer spectates the scorer
  (`ClientboundSetCameraPacket`): Minecraft draws a plain arm for a camera entity that isn't the local player, and the hero
  HUD reads the local player's `ClientHero` state, which the replay doesn't feed (the scorer's own hero payloads went to
  the scorer). What it would take: route the replayed `hero_sync`/ammo/cooldown payloads for the camera entity into a
  second `ClientHero` view state, make `HeroHud` and `FirstPersonHandsMixin` read "the camera's hero" instead of the
  player's, and drive the first-person animator from the replayed actions. A few days, not a round.
- The lineup is drawn over the frozen world as menu figures (flat UI lighting), not entities placed in the world; ours.
- The intro's set, its props' models (0797, 13CA), its sounds; intros for any hero but Kiriko.
- Pixel-measured comparison against the clip frames: the fractions were set from potg_reference and checked by eye in the
  screenshots, not measured back out.
- Sound by ear: nobody listened.
- A dedicated server; the two-client pass covered one LAN guest.
- No critique agent has read this step.

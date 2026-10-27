# Music research, round 9: Overwatch's match-end music (for #45, the dragon fight)

Offline and read-only, build 154088. Overwatch and Battle.net were checked closed before every run. Scripts and outputs
are archived in `E:\Overwatch Archive\research\music-round9\` (`scripts\wwmusic.py` decodes Wwise music objects,
`msgscan.py` finds which graphs send a message, `cases.py`/`chain2.py` read graph switches, `chans.py` measures
channel levels). It builds on `sounds-round8.md` section 3. **H** = read in the data, **M** = inferred, **L** = weak.

## What Overwatch does
Overwatch has no "your team is about to win" music: its end-phase music plays for both teams.
- **The music director:** mode graphs 1B47 (Escort, Hybrid, Assault, Flashpoint, Clash), 1B5C (Control) and 1EF8
  (Push) keep a music state in var 18968. Each state sends message D06 with an effect, and the player graph 01E5
  plays it while the state lasts (H). There's no team branch (M).
- **30 seconds left:** the mode graph 0F4A sends 0D18 while the round timer is ≤ 30 s and > 0. After a 2 s wait it
  plays effect 30C8 (H). Blizzard's patch notes of 2016-08-02 call this the "30-seconds-remaining music". Control has
  no timer and no 0D18.
- **Overtime:** message 075E, from the branch that sets a 5 s fuse, plays effect 30C9 (H). It's the same in Control
  and Push.
- **Score modes** (Deathmatch, TDM, Clash, Lúcioball): message 0665 plays effect 3B91. Halloween Eichenwalde
  swaps in 296D.
- **Push:** the robot-progress messages 1033 and 1034 play 2FCC (5B26, 79.9 s) and 2FCD (5B27, 39.2 s).
- **`intensity_states` is not the PvP mechanism.** Its states intensity_00 to intensity_11 are used by 24 adaptive
  cues that no PvP mode graph reaches (H).

## The cues
| Cue | STUSound | Stream | Length | Shape |
|---|---|---|---|---|
| 30 s left | 69B8.02C (bank 1E6EF) | 2A794B.03F | 34.52 s | once (H) |
| Overtime | 69B7.02C | 2A7946.03F | 83.16 s | a 15.6 s intro (source 0.60-14.31 s), then a loop of 18.18-79.89 s (61.7 s) until it ends; a 0.5 s fade out (H) |
| Score modes | 69C3.02C | 2AE1A7.03F | 52.3 s | a 16 s intro, then a 34.7 s loop (H) |
| Victory | A1BA (variant) | 2B0DE6 (8.75 s) at 0, then 2B0DEA + 2B0DEB + 2B0E50 (30.53 s each) together from 2.5 s | 33.03 s | layers (M); which variant plays is an expression not decoded (`1d 00 1d 01 4b 00`) |
| Defeat (reference) | A1B9 | 2B0DED (8.28 s), then three 18.89 s layers from 2.0 s | 20.89 s | |
| Play of the Game (candidate) | 5B1F.02C (bank 1E5C8) | 2B0E13.03F | 16.84 s, stereo | builds to a peak at 7-12 s, then fades. Message 19C8 plays it in 0D3B, and the end-of-match graph 1B4C plays it directly (M-L). Its length fits the wiki's 17 s POTG. **Confirm by ear.** |

The end-phase streams are 12-channel Opus, probably a 7.1.4 mix with an empty centre (M). Minecraft needs a stereo
downmix.

Not identified: 8B82 (12.75 s) plays when the match-music graph starts, and 19B4/526C (44 s) on message 022B
(round start?). No music was found for the screens after POTG, and the VICTORY banner's art still isn't found (round
8's list of strings stands).

## For the dragon (#45)
Two ready-made shapes, both data:
- The 30 s cue: one 34.5 s piece that builds.
- The overtime cue: an intro, then a loop until it ends.

The owner's ask, "starting quiet at 1/3 health", fits the overtime cue's intro-then-loop: start it at 1/3 of the
dragon's health and raise its volume as the health falls (ours). Then on the kill: VICTORY's sting (in now) with
A1BA's layers, then the POTG cue.

Extract with round 8's `extract_banks.py`: `"EndPhase=69B8,69B7,69C3,5220,5B26,5B27" "Potg=5B1F"`, into a new folder.

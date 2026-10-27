# Sound research, round 8: Kitsune Rush's corridor loop, Swift Step, Victory

Three questions from the owner's playtest of 2026-10-26, answered from the archived build 154088
(prometheus-2_25_0_0-154088), offline and read-only: TankLib/DataTool hosted from Python (`owinit.py`, `Online=false`),
the extracted files in `E:\Overwatch Archive\extracted\154088\`, and the decoded graph dumps of the earlier rounds.
`Get-Process Overwatch, Battle.net` returned nothing before every run that opened the archive; no file of
`build-154088` changed (checked by modification time afterwards). Nothing from the game is in this repo. Read
`kiriko-sounds.md` first (banks, audiences 1/2, how effects carry sounds); ids are written the same way.

Confidence on every claim: **H** = measured in the data, **M** = inferred from the data, **L** = recollection or guess.

New in this round: the Wwise objects' own properties were decoded (positioning, volume, pitch, loop, fades,
attenuation curves; prop-id table at the end). Earlier rounds only read the trees.

## 1. Kitsune Rush: the sound you hear while standing in the corridor

### What plays it

The chain, read from the graphs (H unless marked):

1. Ult graph 30DA creates the rush entity and plays graph **33DB** on it (`ActionPlayScript`, target var 512).
2. 33DB plays graph **2169** (`ActionPlayScript` with graph-with-overrides 2169, duration param var 14669).
3. 2169 is the corridor's area tracker: `StateTrackTargets#203` (radius from config var 70015) tracks allies; for each
   tracked ally a server-only branch plays graph **2071 on that ally** (`ActionPlayScript#216`) and holds a stack
   `+1` on the ally's var **36447** for as long as the ally is tracked (`StateStack#217`). So 36447 = "inside how
   many rush corridors".
4. 2071, running on the ally: `STU_B591EBFE#297` switches on `36447 > 0` with two 0.2 s values (M: the enter and
   leave delays).
   - While true: `State#249` holds effect **3C65** (the teal particles on the ally, every client sees them) and, inside
     a `STU_BD02E168` block, a **client-only** held effect **2E87** (`STU_691BFA55#279`, `m_clientOnly = 1`).
     `STU_BD02E168` wraps only client-side "for this player" things in every graph it appears in (the "not ready"
     click 0186 in Swift Step's graph, the heal confirmation 3EC4 in the weapon graph), so it is most likely the
     local-player block (M): only the player whose hero runs 2071 plays it.
   - While false: `StateWait#309` (8 s), then the script ends (M).
5. 33DB also holds 2E87 from its own `Entry#691` on the rush entity, not client-only. Who hears that copy is not
   resolved (L): with the 2D setup below and a range field of 0 it is most likely nobody but a player at distance 0.

So the loop is effect **2E87** = sound bank **7924** (the "corridor bed" of `kiriko-sounds.md`), played **once per
player inside the corridor, for that player only**, started when they enter and stopped when they leave (H for the
graph chain, M for "that player only").

### The sound

STUSound 7924: audience 2, Wwise bank 1E87F (id 3630743417), play event 3710023914, stop event 3354277864 (so a loop
the game stops), range field `m_ACB34EFC = 0` (H). Effect 2E87 is 8.0 s long and its sound component runs 0-8 s (H).

The play event does four things (H, decoded actions):
- Play the **bed layer** (Wwise layer 719461219) with a **0.3 s fade-in** (action TransitionTime 300 ms).
- **Seek** the bed layer, and separately bed 2B31B1, to a **random point between 0 and 100 %** of their length.
- Play the **opening layer** (920696300), from the start.

The stop event stops **only the bed layer, with a 2.5 s fade-out** (TransitionTime 2500 ms, curve 7); the opening
one-shots play out (H).

| File (`.03F.ogg`) | Layer | Length s | Loop | Pitch (cents) | Delay s | Net gain dB | File peak dB |
|---|---|---|---|---|---|---|---|
| 2B3279 | bed | 10.77 | yes (infinite) | **+1010** (x1.79) | | -10 | -7.4 |
| 2B31B1 | bed | 10.68 | yes | +80 (x1.05) | | +8 | -19.4 |
| 2B3206 | bed | 8.34 | yes | | | +9 | -22.1 |
| 2B3248 | bed | 3.96 | no (one-shot) | | 0.2 | +10 | -15.4 |
| 2B31B3 | opening | 4.79 | no | **+630** (x1.44) | | -2 | -1.5 |
| 2B3277 | opening | 4.39 | no | | | +5 | -20.6 |
| 2B3243 | opening | 8.31 | no | | | -1 | +3.2 |
| 2B3250 | opening | 2.90 | no | | 0.03 | +2 | -10.6 |

- Loop = Wwise Loop property 0 (infinite) on the three beds (H). Lengths: ffprobe (H).
- Net gain = sum of the Volume and MakeUpGain properties from the sound up to the root mixer: sound + layer (beds
  +12, opening +9) + mixer 917513219 (-10) + root mixer 881630477 (-4 volume, -2 make-up) (H for the values, M that
  they add like this; bus volumes not decoded, so relative levels only). The bed layer also has a high-pass of 15.
  With the file peaks, the looping beds sit around -11 to -17 dB, the opening hits around -4 to +2: a quiet bed
  under a louder start, which matches "continual audio playing quietly".
- **Positioning: 2D** (H). The root mixer 881630477 overrides positioning with listener-relative routing and **3D
  spatialization "None"**, and no attenuation; nothing below it turns 3D on. 2B31B1 and 2B3248 add their own
  speaker panning (front/back -100) but are still not spatialized. The sound is "in the listener's head", the same
  level anywhere in the corridor.
- **Pitch** (M-H, prop 0x02, see the table at the end): in the game 2B3279 plays 10 semitones higher and 1.79x
  faster than the extracted file (one loop pass 6.0 s instead of 10.8 s), 2B31B3 6.3 semitones higher. Played raw,
  our copies sound much lower and slower than Overwatch.
- **Extracted: yes, all 8 files** (H), in two copies:
  `E:\Overwatch Archive\extracted\154088\Heroes\Kiriko\Skin\Overwatch\Overwatch\Entities\Gameplay3P\Effects\000000002E87.00D\Sounds\000000007924.02C\`
  and the same under `...\Overwatch\Effects\000000002E87.00D\...`.

### Start, stop, length

- Enter the corridor: after ~0.2 s (M) the in-corridor state starts: the opening one-shots play from the start and
  the beds fade in over 0.3 s from a random point. Re-entering does it again, which is the "restart" the owner hears.
- Leave: after ~0.2 s (M) the state ends: the beds fade out over 2.5 s; the opening one-shots finish on their own.
- Length: as long as you stay inside, at most the effect's 8 s (= the corridor's 8 s life), plus the 2.5 s fade (M).
- Who hears: each player inside hears their own copy (Kiriko too: the corridor buffs her as well), 2D. Nobody hears
  another player's copy; enemies are never tracked (M).

### What Overcraft does now

`KitsuneRush.java` plays `kiriko.rush.summon` and `rush.cast`/`cast_3p` at the cast and one chime per gate. **No
corridor sound exists in the code** (`OcSounds` has no `rush.path`), so nothing plays per player in the corridor
(H). `soundmaps/kiriko.txt` maps the 4 bed files as random variants of `kiriko.rush.path` and the 4 opening files as
variants of `rush.path_start`; in Overwatch they are layers that all play at once.

What it should be (for whoever implements it):
- Pack: one **stereo** (2D: do not mono-downmix) loop `kiriko.rush.path` = 2B3279 at pitch x1.79 (-10 dB) + 2B31B1
  x1.05 (+8) + 2B3206 (+9), made seamless; one stereo one-shot `kiriko.rush.path_start` = the four opening files
  (2B31B3 at x1.44) + 2B3248 at 0.2 s, at the gains above.
- Game: in `buffAllies`, track each player's in/out with a 4-tick (0.2 s) delay both ways. On enter, tell that
  player only (a payload); their client starts `path_start` and a looping tickable sound instance that is
  listener-relative (`relative = true`, `Attenuation.NONE`), fading in over 0.3 s. On leave, or when the corridor
  ends, fade it out over 2.5 s and stop. Mobs need nothing.

## 2. Swift Step

### The game's sounds, per phase and audience

Order in Swift Step's graph 30DB (H for the links; times from `KirikoStats`/the wiki: 0.2 s cast, 0.25 s phased):

| Phase | When | Effect | Bank: audience | Files (length s) | 2D / 3D | Range |
|---|---|---|---|---|---|---|
| Hand seal (cast) | start of the depart animation (alias 0x7F8, clip from 0.15 s) | 3D44 | **76F7: 1 (Kiriko)** | random 2B3245 (2.67, -50 cents), 2B31AD (2.32), 2B3269 (2.67, +180 cents) + always 2B31B4 sub (1.38, mono, -10 dB) | **2D** (spatialization None, no attenuation) | unlimited |
| Hand seal (cast) | same | 3D44 | 76E6: 2 (others) | random 2B0CF4 (2.71), 2B0D04 (2.66), 2B0D4A (2.92) | 3D | 70 m |
| Start of the transit | after the 0.2 s seal (graph branch #377, with `StateWait#91`) | 3CC5, at Kiriko, oriented along the travel direction | DA8D + 76E5: 2 | random 2B3257 (2.17), 2B31B5 (2.20), 2B3268 (2.12) | 3D | 70 m |
| Transit | `StateWait#59` (time from var 1723) | 4491 (dissolve overlay 0.3 s) | none | - | - | - |
| Arrival | end of `StateWait#59`, **together with the Swift Step voice line (stimulus FC0)** | 3CC7, on Kiriko | DA8E + 76E6 (the same Wwise container twice: two random picks) + DA8C (own container, own curve, +1 dB): 2 | the same 3 files as 76E6 | 3D | 70 m |
| No ally in view | client-only | 0186 | - | 2AA253 (0.20) | - | - |

- 3D sounds use attenuation 493025999: full volume to ~20.6 m, falling to silence at 70 m; DA8C's own curve
  (761353640) is a little softer from 20 m (H, decoded curves).
- All files are stereo in the archive (H).
- **Correction to `kiriko-sounds.md`/`kiriko-effects.md` (M):** they call 3CC7 the departure and 3CC5 the arrival. In
  the graph, 3CC7 fires at the end of the transit, in the same plug as the arrival voice line ("I'm here", "Howdy"),
  and 3CC5 fires as the transit starts with an orientation along the travel direction. So 3CC7's files (our
  `swift_step.depart`) are most likely what others hear **where she arrives**, and 3CC5's (our `swift_step.arrive`)
  what they hear **where she leaves**. Others also get the 3CC7 files at the start, through 3D44's 76E6. 3CC5 sits on
  a second branch of the graph (#334/#377) that runs alongside the hand-seal branch (#623/#570); whether both run on
  every cast was not resolved. Worth checking against a third-person video before swapping.
- The previous open question "is 76F7 a separate hand-sign sound?" (H/M): 76F7 is Kiriko's own 2D version of the whole
  step. It starts with the seal and its 2.3-2.7 s covers the seal, the transit and the arrival. **Kiriko hears only
  76F7** (M: she is excluded from audience-2 sounds; certain for 3D44's pair, inferred for 3CC5/3CC7, which have no
  audience-1 copy).

### Why the owner hears only part of it

`Kiriko.startSwiftStep` calls `HeroSounds.split(player, CAST, DEPART, player.position())`. `HeroSounds.toOwner` sends
the owner a `ClientboundSoundPacket` at that position. In Minecraft 26.3's client (decompiled, H):
`ClientPacketListener.handleSoundEvent` -> `ClientLevel.playSeededSound(x, y, z...)` -> a `SimpleSoundInstance` at a
**fixed world position**, `Attenuation.LINEAR`, not relative, over `Sound.getAttenuationDistance(volume)` = 16 blocks
at volume 1. The pack makes the cast mix **mono**, so it is spatialized. Four ticks later `finishSwiftStep` teleports
her up to 30 m. The rest of the 2.3-2.7 s whoosh keeps playing at the start point: from behind her, at
`1 - distance/16`, silent past 16 blocks. Then `level.playSound(null, arrive, ARRIVE)` plays the 2.1 s arrival for
**everyone including her**, at full volume where she now stands. So she hears ~0.2 s of the cast, then a different
sound: exactly "it plays where you first were".

### What each should be

- **Kiriko's own player: `swift_step.cast` (76F7 + sub), from the key press, attached to her.** Either the vanilla
  route: send her a `ClientboundSoundEntityPacket(sound, PLAYERS, player, ...)`; her client then plays an
  `EntityBoundSoundInstance` that moves to her position every tick (`ClientLevel.playSeededSound(except, entity...)`)
  (H, decompiled). Or, closer to Overwatch's 2D: a payload and, on the client, a sound instance with `relative = true`
  and `Attenuation.NONE`, with the cast left **stereo** in the pack.
- **She should not get `depart` or `arrive`.** Pass her as the excluded player: `level.playSound(player, ...)`
  (`ServerLevel.playSeededSound` sends to everyone but `except`).
- **Everyone else:** the depart set (2B0CF4/2B0D04/2B0D4A) at the start point when the seal starts (as now). Then,
  per the correction above (M), the 3CC5 set at the start point as she leaves, and the 3CC7 set (the same files as
  depart) at the end point when she arrives. All positional and mono. Overwatch's range is 70 m: volume ~4.4 in
  Minecraft (70/16 blocks; Minecraft only does linear falloff, not "flat to 20 m").

## 3. Victory (and Defeat)

### The announcer's line

Voice set **001D.05F** (874 lines) is the match announcer ("Next round beginning soon.", "Traveling to Route 66.",
training lines, hero names) (H). One line per stimulus, no variants (H):

| Stimulus | Subtitle | Voice file (.0B2) | Length s | Extracted as |
|---|---|---|---|---|
| 019E.078 | "Victory." | A8C8 | 1.66 | `ExtraSounds\Announcer\00000000A8C8.0B2-Victory..ogg` |
| 019F.078 | "Defeat." | A8C7 | 1.08 | `...\00000000A8C7.0B2-Defeat..ogg` |
| 01A0.078 | "Draw." | A8C6 | 0.98 | `...\00000000A8C6.0B2-Draw..ogg` |
| 0066.078 | "Overtime!" | A8CA | 1.54 | `...\00000000A8CA.0B2-Overtime!.ogg` |

Stereo as converted, peak about -10 to -13 dB (H). The stimuli are category 0006.079, priority 80, triggered by the
statescript identifiers 05BF.01C (victory), 0841.01C (defeat), 084A.01C (draw) (H). That 001D is the announcer for
ordinary matches is M: it is the big general set. Event/mode announcers have their own sets: 0138, 0151, 0175, 0176,
022F, 0238, 0249, 0256 and 026C each have Victory/Defeat lines (H). The double dot in the file names comes from
DataTool appending `.ogg` to the subtitle.

### The music sting

Graph **0D3B** (a subgraph of the client graph 041A) is the match-music logic: its `STU_E0687628` nodes play music
effects on game messages (H). Two mirrored chains, each with three variants. The variant is picked by an opaque
condition, checked twice, whose decoded data shows nothing that tells the checks apart (unresolved); the last one in
each chain is the fallback (H):

| Chain | Reached by | Variant 1 | Variant 2 | Fallback |
|---|---|---|---|---|
| **B: victory (M)** | message 0881; or var 1777 true (set by message 0783); or var 4914 true | 4F01 -> A1BA.02C: 2B0DE6 + 2B0DEA, 2B0DEB, 2B0E50 (30.54 s each) | 2036 -> 4439.02C: **2B2E4D 8.42 s** | 4422 -> 8B7F.02C: **2B0DE6 8.76 s** |
| **A: defeat (M)** | message 0880; or var 1777 / 4914 false | 4F02 -> A1B9.02C: 2B0DED + 2B0CC2, 2B0E02, 2B0DF2 (18.90 s each) | 203A -> 443E.02C: 2B2921 8.54 s | 4421 -> 8B7E.02C: 2B0DED 8.30 s |

- All stereo, streamed, no stop events: one-shot stings (H). The fallbacks are in bank 1E5C8, variant 2 in bank
  1E52C, whose Wwise bank id is the hash of "music" (H).
- **Which chain is victory (M):** both B stings end on a clear major chord (C# major: C#, F, G#), and the fallback is
  loud (rms -16.6 dB, peak -0.6). A's end without a clear major third (D/G) at rms -22.1, and its variant 2 starts in E
  minor (chroma of the last part, measured; mode read from it). No names: Wwise event names are hashes. A dictionary
  attack recovered only "music" (bank 1E52C), "music_lobby" and "intensity_states".
- For the dragon kill: **2B0DE6** (8.76 s, `ExtraSounds\MatchEnd\ChainB\Sounds\000000008B7F.02C\`) under the
  announcer's "Victory." is the best pick (M). Defeat would be 2B0DED.
- The same graph plays other match stingers, not identified (L: round won/lost or match start): message 0193 picks one
  of five by var 07EE.01C (F52-F56 = 253A/253C 2B291F 6.36 s, 253B/2538 2B2E45 4.30 s, 2537 2B28CF 10.48 s + 2B2E22
  4.30 s); message 0194 -> F57 = 2536 2B2903 6.0 s; message 0633 picks by game mode, default 19A9 = 378B 2B2920 4.92
  s. Extracted under `ExtraSounds\MatchEnd\Msg0193`, `Msg0194` and `Msg0633`.

### The VICTORY screen's visual

- The end-screen word is a localized string, not a texture found so far (M). The enUS strings that read exactly
  Victory/Defeat/Draw come in sets: 1760F/17610/17611, C082/C083/C07B, 1FCB7/1FCB8/1FCB9 ("Victory!"),
  25787/25788/25789 ("Victory!"), 250CB/250CC, plus 9305, C530, 21762 and 4AC6 ("DRAW") (H). Which one the end
  screen shows was not resolved.
- Fonts: `E:\Overwatch Archive\extracted\154088\Fonts\` (see `fonts.md`; `ttf-converted\Industry-Black.ttf` is the OW2
  heading face; that the VICTORY banner uses it is L).
- UI textures: `E:\Overwatch Archive\extracted\154088\UITextures\UITextureDump\` (21567 unnamed images). No victory
  banner or background was identified among them.

## Side findings

- **Gate chimes are pitched up in the game** (M-H): the six gate sounds sit under mixer 106782798 with Pitch +660
  cents (x1.46). The pitch ladder in `kiriko-sounds.md` (1042 to 2411 Hz) is about 1525 to 3530 Hz in Overwatch. The
  gates are 3D with a curve flat to 15 m, silent at 75 m.
- The heal confirmation 3EC4 (open question in `kiriko-sounds.md`) sits in the weapon graph's local-player block, so it
  is most likely the healer's (Kiriko's) sound (M).
- STUSound `m_573D3605` is 1 on 219 of 220 one-shots and 0 on 28 of 31 loops (H, the 251 banks of `banks.json`): a
  "fire and forget" flag (M). `m_ACB34EFC = 0` appears only on loops played on the listener's own hero (the corridor bed, the "being
  healed" shimmer 8E5B/792F): most likely "only the hero's own player" (M).

### Wwise property ids in these banks (bank version 145)

Derived from the value patterns (random ranges, ids of attenuation objects, ms values on actions) and Wwise's own
order. It matches wwiser's older list shifted by +2 from 0x09 on (M-H):
0x00 Volume (dB), 0x02 Pitch (cents), 0x03 LPF, 0x04 HPF, 0x06 MakeUpGain (dB), 0x07 Priority, 0x08 priority
distance offset, 0x0C/0x0D pan left-right / front-back, 0x0E center %, 0x10 transition time (ms, on actions: fades),
0x13/0x14 user aux sends, 0x17 game aux send, 0x3A Loop (0 = infinite), 0x3B initial delay (s), 0x46 attenuation
id. Positioning byte: bit 0 override parent, bit 1 listener-relative routing, then a 3D byte whose bits 0-1 are the
spatialization (0 none = 2D, 1 position).

## Extraction (round 8)

New folders (the archive's rule: a new folder, never into an existing one), all files set read-only, 25 files,
3.8 MB, decoded by ffmpeg without errors:
- `E:\Overwatch Archive\extracted\154088\ExtraSounds\MatchEnd\` (`ChainA`, `ChainB`, `Msg0193`, `Msg0194`, `Msg0633`,
  each `Sounds\<bank>.02C\<file>.03F.ogg`, plus `manifest.json`).
- `E:\Overwatch Archive\extracted\154088\ExtraSounds\Announcer\` (the four voice lines).
- Not updated: `ExtraSounds\ExtraSounds.sha256.txt` and the archive README.

Commands (PowerShell; the process check comes first, and `owinit.py` checks again):

```powershell
$p = Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue
if ($p) { "Overwatch or Battle.net is running: stop" } else {
  & "E:\Overwatch Archive\research\kiriko-anim\venv\Scripts\python.exe" `
    "E:\Overwatch Archive\research\kiriko-sounds\scripts\extract_banks.py" `
    "E:\Overwatch Archive\extracted\154088\ExtraSounds\MatchEnd" `
    "ChainA=8B7E,A1B9,443E" "ChainB=8B7F,A1BA,4439" "Msg0193=2537,2538,253B,253C,253A" "Msg0194=2536" "Msg0633=378B"
  & "E:\Overwatch Archive\research\kiriko-anim\venv\Scripts\python.exe" -I `
    "E:\Overwatch Archive\research\sounds-round8\scripts\extract_voice.py" `
    "E:\Overwatch Archive\extracted\154088\ExtraSounds\Announcer" "A8C8=Victory." "A8C7=Defeat." "A8C6=Draw." "A8CA=Overtime!"
}
```

`extract_voice.py` uses DataTool's `SaveLogic.Combo.SaveSoundFile` (the routine `extract-hero-voice` uses), refuses
to write outside `extracted\` and skips existing files.

## Scripts and raw outputs

`E:\Overwatch Archive\research\sounds-round8\` (outside the repo). `scripts\`:
- graphs: `gd.py`, `shown.py`, `anc.py`, `reach.py`, `refs.py`.
- Wwise: `wwdec.py` (objects, props, positioning, actions), `attn2.py` (attenuation curves), `propscan.py`,
  `banktab.py`, `musicscan.py`, `musichash.py`, `musichash2.py`, `mitm.py`.
- voice: `stimscan.py`, `vsscan.py` (every voice set's lines with subtitles), `announcer.py`.
- reverse lookups: `sndscan.py` (STUSounds per bank), `refscan2.py` (effects using them), `graphscan.py` (graphs using
  effects, through DataTool's `Combo.Find`); `dumpstu.py`, `strings.py`, `reflect*.py`.
- audio: `keyest.py` and `endchord.py` (levels and chord estimate, ffmpeg + pure Python).

The JSON next to them holds the results (`vsscan.json`: every voice line of every voice set; `refscan_effects.json`:
the effect for each music sound; `graphscan.json`; `stu\` the decoded 041A/0D3B graphs and stimuli). Raw-byte
searches only work on effects (chunked files); STU files must be decoded first (their GUIDs are not stored as plain
bytes).

## Still open

- Whether both Swift Step branches (#623 seal/animation, #334 logic with 3CC5) run on every cast, and so whether 3CC5
  is heard every time (section 2).
- Who hears the 33DB copy of 2E87 on the rush entity.
- The music variant condition in 0D3B (which of the three victory stings plays when), and which chain is victory:
  confirm by ear (Overwatch closed, listening to the extracted files).
- The VICTORY screen's string and face.
- The 0.2 s values on `STU_B591EBFE` are read as enter/leave delays, not confirmed.

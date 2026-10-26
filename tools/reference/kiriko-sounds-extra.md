# Kiriko's extra sound banks (the ones extract-unlocks missed)

Extracted 2026-10-26 from the archived build 154088 (prometheus-2_25_0_0-154088, `E:\Overwatch Archive\build-154088`),
offline and read-only: TankLib `ClientCreateArgs.Online=false`, DataTool `ToolFlags.Online=false`, Overwatch and
Battle.net checked closed (`Get-Process Overwatch, Battle.net` returned nothing) before every run that opened the
archive. The audio was converted by DataTool 2.25.0.0's own code (no converter of ours), and it stays in the owner's
archive: nothing from the game is in this repo. This file only says what is where. It follows the "Not extracted"
table of `kiriko-sounds.md`; read that file for what each bank means in the game.

Ids are written as in `kiriko-sounds.md`: banks `.02C` (`76FD` = `0000000076FD.02C`), sound files `.03F` (last 6 digits,
`2B3253` = `0000002B3253.03F.ogg`).

## Where the files are

Root: `E:\Overwatch Archive\extracted\154088\ExtraSounds\` (a new folder, as the archive README asks). The layout
mirrors DataTool's own (`...\Sounds\<bank>.02C\<file>.03F.ogg`), with one folder per gameplay event above it:

| # | Event | Bank | Folder (under `ExtraSounds\Kiriko\`) | Files |
|---|---|---|---|---|
| 1 | Suzu burst, 1P | 76FD | `SuzuBurst\Sounds\0000000076FD.02C\` | 5 |
| 1 | Suzu burst, 3P | 96F8 | `SuzuBurst\Sounds\0000000096F8.02C\` | 5 |
| 1 | Suzu burst, far | BDE2 | `SuzuBurst\Sounds\00000000BDE2.02C\` | 5 |
| 2 | Kunai hit on a target | 8DE7 | `KunaiHitTarget\Sounds\000000008DE7.02C\` | 33 |
| 3 | Ofuda heal hit, 1P | 770E | `OfudaHealHit\Sounds\00000000770E.02C\` | 20 |
| 3 | Ofuda heal hit, 3P | 8258 | `OfudaHealHit\Sounds\000000008258.02C\` | 39 |
| 3 | Ofuda heal hit, 3P | 78F3 | `OfudaHealHit\Sounds\0000000078F3.02C\` | 39 |
| 4 | Kunai on terrain (default material) | 826E | `KunaiHitTerrain\Sounds\00000000826E.02C\` | 11 |
| 4 | Kunai on terrain (default material) | 826F | `KunaiHitTerrain\Sounds\00000000826F.02C\` | 11 |
| 5 | Kunai whiz-by (probably) | 7CCB | `KunaiWhizBy\Sounds\000000007CCB.02C\` | 6 |
| 6 | Kitsune fox footsteps (probably) | 8240 | `KitsuneFoxSteps\Sounds\000000008240.02C\` | 31 |
| 6 | Kitsune fox footsteps (probably) | 8241 | `KitsuneFoxSteps\Sounds\000000008241.02C\` | 31 |

236 files (145 different sounds: paired banks share their files, and each bank folder holds a full copy, as DataTool
does), 3.2 MB. Every bank in priority 1-6 came out complete: no file missing, no raw `.wem` fallback.

Next to them:
- `ExtraSounds\Kiriko\manifest.json`: per bank, its Wwise bank (.043) and file list.
- `ExtraSounds\Kiriko\measure.json`: per file, codec, channels, duration, peak/mean dB, size, sha256.
- `ExtraSounds\ExtraSounds.sha256.txt`: checksums, like the archive's other `.sha256.txt` lists.
- `ExtraSounds\extract-kiriko-extra-sounds.log`: the commands and their output.
- All files are set read-only, like the rest of the archive.

## What came out, per bank

All Vorbis, 48 kHz, decoded by ffmpeg with no errors. Levels are of the file as stored (ffmpeg volumedetect; the Wwise
objects' own volume offsets were not decoded). Structure = the Wwise tree (event -> Play action -> containers), from
`research\kiriko-sounds\wwmap_condensed.txt` (8240/8241: `wwmap.py 8240 8241`, read this time). **Random** = Overwatch
picks one file per play (Minecraft variants); **layer** = its parts play together. Audience 1 = Kiriko's own player,
2 = everyone else (see `kiriko-sounds.md`). Range = square root of the STUSound's `m_ACB34EFC`; guard = `m_ACA7FBC8`.

### 1. Suzu burst (76FD 1P, 96F8 3P, BDE2 far): the real sound, replaces the souvenir stopgap

Wwise bank 1E880 (Wwise id 3762787232). All three banks are **one random container of the same 5 stereo files**:

| File | Duration (s) | Channels | Peak dB | Mean dB |
|---|---|---|---|---|
| 2B3253 | 1.807 | 2 | -5.4 | -26.4 |
| 2B31AE | 2.132 | 2 | -4.0 | -27.0 |
| 2B324C | 1.913 | 2 | -3.9 | -26.7 |
| 2B3247 | 1.983 | 2 | -7.3 | -25.8 |
| 2B3255 | 1.919 | 2 | -5.3 | -25.8 |

76FD: audience 1, 70 m. 96F8: audience 2, 70 m. BDE2: audience 2, 150 m, 0.3 s guard (the bell heard across the map).
The three folders hold the same audio (see "Duplicates"). For Minecraft: one sound id with 5 variants; stereo, so it
needs a mono downmix to be positioned in the world.

### 2. Kunai hit on a target (8DE7)

Wwise bank 1E6C8 (Wwise id 22156203), audience 2, no range limit, 0.05 s guard. A **layer of three random sets**, one
file of each playing together on every hit:
- A: 10 mono, 0.54-0.60 s, median peak -3.2 dB, mean -15.0 dB (2A5AB1 2A5C06 2A595A 2A5C66 2A5C02 2A5C77 2A5C08
  2A598A 2A5BAE 2A5C12).
- B: 16 stereo, 0.41-0.61 s, median peak -5.9 dB, mean -21.0 dB (2A5CA0 2A599D 2A5953 2A59AF 2A5955 2A599C 2A5BD4
  2A596A 2A59AD 2A597C 2A5C20 2A5C17 2A5C61 2A5C7A 2A5BEA 2A5970).
- C: 7 stereo, 0.49-0.56 s, **peak -0.1 dBFS** (at full scale), mean -15.6 dB (2A5C29 2A595B 2A5BB3 2A5C4F 2A5BAA
  2A5C60 2A5C2D).
This is Kiriko's own kunai impact (effect 13B2 -> 4057), so it can replace the generic `kunai.hit`/`hit_sub` punches.
Played together A+B+C will clip: lower them a few dB at pack build.

### 3. Ofuda heal hit (770E 1P; 8258 and 78F3 3P)

Wwise bank 1E464 (Wwise id 3111927474).
- 770E: audience 1, no range limit. **One random set of 20 stereo files**, 0.46-0.55 s, quiet (median peak -16.4 dB,
  mean -36.5 dB).
- 8258 and 78F3: audience 2, 70 m. The same **layer of two random sets** in both (different file order, same files):
  - A: 14 mono, 0.41-0.67 s, median peak -15.1 dB, mean -41.2 dB (2B08AA 2B0980 2B0A15 2B0A23 2B089F 2B0A1A 2B098F
    2B088E 2B0A24 2B09A5 2B0A1F 2B08E7 2B099F 2B08C4).
  - B: 25 stereo, 1.01-1.57 s, median peak -14.0 dB, mean -31.3 dB (2B0989 2B08FA 2B0A3E 2B09FB 2B08C3 2B09B7 2B08B1
    2B09A0 2B089E 2B08E9 2B0A6D 2B0A09 2B0A0F 2B0A36 2B0A28 2B0A5C 2B0A27 2B08D9 2B0A0D 2B0A05 2B09A3 2B0A26 2B0890
    2B08B7 2B098D).
- 770E shares no file with 8258/78F3. Effects 4004 (teal) / 40C0 (gold) are the projectile's hit effects; which of
  8258/78F3 belongs to which colour was not checked here.

### 4. Kunai on terrain, default material (826E, 826F)

Wwise bank 1E733 (Kiriko's impact bank, Wwise id 1758105412), audience 2, 0.1 s guard. Both are **one random set of the
same 11 mono files**, 0.54-0.79 s, median peak -1.5 dB (max -0.5), mean -29.5 dB: sharp transients with a tail. The two
banks differ only in range: **826E 32 m, 826F 90 m** (most likely a near and a far copy). The 22 per-surface contact
effects (47DD-4FED) were not extracted (out of scope, see "Still not extracted").

### 5. Kunai whiz-by, probably (7CCB)

Wwise bank 1E464, audience 2, 60 m. **One random set of 6 mono files**, 0.42-0.67 s, median peak -2.5 dB, mean
-22.4 dB (2B0A54 2B0A6A 2B0A5A 2B0A22 2B09A1 2B08EA). "Whiz-by" is still the inference of `kiriko-sounds.md` (effect
3E87 in the projectile's `m_2170A015`); nobody has listened to them yet.

### 6. Kitsune fox footsteps, probably (8240, 8241)

Wwise bank 1E499 (Wwise id 2268190355), audience 2, 50 m, 0.1 s guard. 31 mono files, all short and very quiet
(0.32-0.52 s, median peak -24.6 dB, mean -46.5 dB), in **two random sets that play together**:
- A: 16 files, 0.32-0.38 s: 2AE789 2AE775 2AE821 2AE787 2AE839 2AE830 2AE78C 2AE844 2AE80E 2AE77F 2AE754 2AE779 2AE838
  2AE782, plus 2AE78A and 2AE80D (their size matched no Wwise object; placed in A by elimination, as A has 2 unmatched
  slots and B none).
- B: 15 files, 0.32-0.52 s: 2AE752 2AE753 2AE756 2AE829 2AE7E8 2AE81A 2AE81C 2AE82A 2AE7E6 2AE827 2AE7EB 2AE83E 2AE755
  2AE847 2AE820.
8241 plays A+B once. 8240 wraps them in one more layer that holds **two** A+B layers (so four files per step: maybe two
paws at once, or a heavier step); why was not resolved. Contact set 0740 types 10/11 point at them; which type is
which was not decoded.

## Duplicates and a conversion quirk

- Paired banks hold the same audio: 76FD = 96F8 = BDE2, 8258 = 78F3, 826E = 826F, 8240 = 8241. Their copies have the
  same size and differ in at most **one byte, the last one of the file**.
- That byte comes from DataTool's converter itself: every file (ours and DataTool.exe's own from 2026-10-25) ends with
  one stray byte after the last Ogg page, and its value changes from run to run. All Ogg page CRCs are valid and
  decoders ignore it. Checked on three files DataTool.exe had already extracted (2B0A3F, 2AF401, 2AE2A0): converting
  them in memory through the same code gave the same size and **bit-identical decoded PCM**, only that last byte
  differing. So the files here are what `DataTool.exe extract-*` would have written, but **compare decoded audio, not
  sha256**, when checking duplicates.

## How it was done (repeat for another hero)

Code: `E:\Overwatch Archive\research\kiriko-sounds\scripts\extract_banks.py` (outside the repo; the kiriko-anim venv
Python, which hosts DataTool's .NET 10 assemblies with pythonnet through `research\kiriko-anim\scripts\owinit.py`).
owinit opens the build with TankLib offline (`Online=false`, no remote keyring, DataTool's bundled `Overwatch.keyring`)
and **exits if `overwatch.exe` or `battle.net.exe` is in the task list**. For each bank the script runs DataTool's own
`FindLogic.Combo.Find` on the `.02C` (reads the STUSound and its WEM list), then `SaveLogic.Combo.SaveSound` with
default `ExtractFlags` (the routine DataTool uses for every effect sound: WEM -> OGG through ww2ogg's
`WwiseRIFFVorbis` + `Revorb`, Opus through `WwiseRIFFOpus`; a failed conversion would leave a `.wem`, which the script
counts). It refuses to write outside `E:\Overwatch Archive\extracted\`, skips a bank folder that already exists, and
writes `manifest.json`.

One thing it has to do: under pythonnet the .NET app base directory is empty, and DataTool looks for
`Third Party\packed_codebooks_aoTuV_603.bin` there, so the script sets `APP_CONTEXT_BASE_DIRECTORY` to the DataTool
folder before touching `SaveLogic.Combo`. Without it, every Vorbis conversion fails and falls back to raw `.wem`.

Commands (PowerShell; the check is repeated before every run):

```powershell
$p = Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue
if ($p) { "Overwatch or Battle.net is running: stop" } else {
  & "E:\Overwatch Archive\research\kiriko-anim\venv\Scripts\python.exe" `
    "E:\Overwatch Archive\research\kiriko-sounds\scripts\extract_banks.py" `
    "E:\Overwatch Archive\extracted\154088\ExtraSounds\Kiriko" `
    "SuzuBurst=76FD,96F8,BDE2" "KunaiHitTarget=8DE7" "OfudaHealHit=770E,8258,78F3" `
    "KunaiHitTerrain=826E,826F" "KunaiWhizBy=7CCB" "KitsuneFoxSteps=8240,8241"
}
```

(Run here in two goes: SuzuBurst first, then the rest. Most of each run is TankLib opening the archive; the
conversion itself takes seconds.) For another hero: find the bank ids the same way `kiriko-sounds.md` did (`banks.py`, `soundsof.py`,
`contact.py` under `research\kiriko-sounds\scripts\`), then run the script with
`"<Hero>"` in the output path and `<Event>=<bank>,<bank>` groups. `wwmap.py <bank> ...` prints the Wwise tree (which
files are random variants and which are layers); it writes nothing.

Checks run afterwards (system Python with `-I`, ffmpeg 6.0 from `C:\ProgramData\chocolatey\bin`): ffprobe + a full
ffmpeg decode of every file (0 errors), volumedetect for the levels, an Ogg page CRC check (all valid), the in-memory
comparison above, and `find build-154088 -newermt 2026-10-26` (no build file changed).

## What failed or is still open

- Nothing in priority 1-6 failed.
- Combo.Find leaves `SoundInfoNew.SoundBank` empty, so the first runs wrote no Wwise bank into `manifest.json`. It was
  filled in afterwards from `research\kiriko-sounds\banks\banks.json`, and the script now reads it from the STUSound
  itself (that lookup was tested on 76FD; the full script was not run again).
- Nobody has listened to the files: the event names (especially 5 and 6, "probably") come from `kiriko-sounds.md`'s
  data reading, not from hearing them.
- Not mapped: `tools/soundmaps/kiriko-proposed.txt` and the asset pack still use the stopgaps. Suggested ids:
  `kiriko.suzu.burst` (76FD for the owner, 96F8 or BDE2 for the others), `kiriko.kunai.hit` + two layers from 8DE7,
  `kiriko.ofuda.heal` from 770E (owner) and 8258 for others, `kiriko.kunai.hit_block` from 826E, `kiriko.kunai.whiz`
  from 7CCB, `kiriko.rush.fox_step` from 8241.
- Still not extracted (outside this task; the same command does them in seconds): 8BCE/8BD2's missing Kiriko layers
  (2ABC4B..., 2AC663...), 8FEB / 8FF3 / 8FE8 / 8FEF, the 22 per-surface kunai contact effects 47DD-4FED, the fiery
  perk explosion BBE4/BBE5/BBE6, Ofuda material 0033 (4870/4871), Suzu material 00E1 (4356/435C).
- The archive's `README.txt` doesn't mention `extracted\154088\ExtraSounds\` yet (left alone: outside this task).

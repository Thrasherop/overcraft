# Sounds

Playing Overwatch's sounds the way Overwatch does, and checking what actually played. Up: [Techniques](../README.md)
· the data: [reading_game_data](../game-data/reading_game_data.md) · hero hub: [kiriko](../heroes/kiriko.md)

- **A hero's own sounds should be bound to them** (`ClientboundSoundEntityPacket`, an EntityBoundSoundInstance on the
  client), not placed at a point: a point sound stays put while they teleport or dash, and fades behind them.
- **In-the-head sounds** (Overwatch's 2D ones): a client sound instance with `relative = true` and
  `Attenuation.NONE`.
- **To check sounds, ask the client what it played:** the harness's `heard` lists each sound since the last call, the
  file picked and its kind (fixed, bound to an entity, in the head). Minecraft re-announces looping sounds to
  listeners every 10 ticks, so repeats in the list aren't restarts. Subtitles on (`subtitles on`) show it in a
  screenshot.
- **Voice lines come from the data:**
  - the voice stimulus (078) groups a moment's lines;
  - `tools/soundmaps/<hero>.txt` maps our sound events to the extracted files;
  - the pack's `sounds` step copies them.

  Kiriko's ping lines are stimuli 11D9 ("Over there!"), 11DA (warnings) and 1117 (enemies).
- **When a line may play: the stimulus says** (the owner, 2026-10-27, on lines landing back to back; `Voice`).
  - Every moment is a voice stimulus (.078) with a priority, a cooldown range in seconds (the unnamed pair
    `m_A73F2EBE` / `m_DD27AC95`) and an interrupt mode, read for all 5,117.
  - Each stimulus names a category (.079): the game's automatic chatter is category 4 with interrupt 0; the
    communication wheel and pings are 31 with interrupt 1. The categories hold only small integers, no times.
  - There is no cooldown shared by all lines in the data, or in Blizzard's notes, which only mention per-line
    cooldowns. Don't invent one.
  - The rule we play by: a moment waits out its own cooldown; a hero says one line at a time; only an interrupting
    line that ranks at least as high cuts in, stopping the first (`ClientboundStopSoundPacket`).
  - "Ult ready" is a wheel line (02E7, category 31), not an announcement when the ult fills.
  - Test it with the same script before and after (`heard` polled with timestamps). Run tests at night: zombies
    burning in the sun die before the kunai land, and the kills aren't the hero's.
- **A callout isn't chatter:** a plain ping's line is limited to one a second (ours: its stimulus has no cooldown, and
  a press each tick would talk over itself).
- <a id="emote-sounds"></a>**Emote sounds (#48):** each emote clip's animation effect lists Sound components (a 02C,
  when it starts, how long it's held, its hardpoint) and VoiceStimulus components (a line and when).
  - `tools/emote_data.py` writes them per clip; `tools/soundmaps/emotes.txt` builds each 02C as
    `<hero>.emote.s<id>` (and each line as `v<id>`) from its Wwise Play event, with subtitles (`$<event> = text`).
  - How each plays comes from the same decode (`research/emote-sounds/sounds.json`): loops restart every loop length
    while held (a built file can run past its loop: Dance Party's is 8.94 s against 7.934 s); some are cut by their
    own event; some sound only some plays (chance, every nth); a Wwise state picks the 2D copy for the emoting player
    and the 3D twin for everyone else ("own" / "others").
  - `EmoteSounds` (client) follows each emoting hero's clip and time each tick (`EmotePlan.at`, shared with the
    animator, so a hero off screen still sounds) and starts each one as the clip reaches it.
  - One shared sound can be in many emotes (944B, a cloth rustle, in seven of Kiriko's).
- **Which copy is the hero's own** (Jetpack Cat, 2026-10-28; `tools/soundmaps/jetpack_cat.txt`, research in
  `research\jetpack_cat\sounds.md`): Lúcio's sounds came in pairs marked audience 1 (his) and 2 (others), but every one
  of hers is audience 2. Read the copy with no range as hers and the ranged copies as everyone else's (inferred); where
  there are two ranged copies they look like an ally and an enemy copy.
- **Loops from the data have a loop length, not a file length:** their layers differ in length, so the map states each
  rule's LOOP LENGTH and some built files run past it. `JetpackCatSounds` (client) starts each loop again when its file
  ends, and stops it as the state ends (hum, boost, refill, tow, aim, dive, tether).
- **Volume curves:** her hum and boost loop follow speed curves in the data; they're built at one point of the curve
  (hover; 10 m/s), read as straight lines.
- **Not every sound is extracted:** her enemy-hit and wall-hit sounds (73A4; the centre shot's contact sets) were never
  extracted, so they're left out until an extraction pass adds them to the archive.
- **Music:** the match-end music was traced through the music-director graphs (0D3B, 1B47):
  [sounds-round8](../../tools/reference/sounds-round8.md), [music-round9](../../tools/reference/music-round9.md). Its
  streams are 12-channel Opus and need a stereo downmix.
- **Ranges come through the volume** (Lifeweaver, 2026-10-29): `sounds.json` sets none, and a volume over 1 only
  widens Minecraft's 16 blocks without making it louder. `HeroSounds.split(..., metres)` and `HeroSounds.at(...)` pass
  the game copy's range as metres / 16; client loops do the same (`LifeweaverSounds.on`).
- **Team copies:** where an effect has an ally and an enemy copy (his platform's landing, the grip's cast, the tree's
  hum and pulses, the ult line), `HeroSounds.at(level, friendly, enemy, hero, at, metres, except...)` picks per
  listener. Check them from a second client on another team (`/overcraft team join red` on it).
- **Charge-scaled sounds come in bins:** the game pitches the blossom's release, throw and heal by the charge
  (an RTPC); the map builds three bins (21C6 #954's: 0.3, 0.9) and `OcSounds.byCharge` picks one.
- **Some voice moments are every hero's:** `OcSounds.init()` registers each hero's wheel lines, `resurrected` and
  `damage_boosted`. Listing them again in a hero's own list crashed the game at start (a duplicate registry key).
- **Check both ends of every sound:** his own `heard` on the host, the "_3p" copies and the ally's own (gripped,
  healed by the tree, the seed taken) on `runClient2` (`OC_DEV_PORT=25600`). A second listener caught nothing wrong
  that the first had missed, but it is the only way to see the others' copies at all.
- **Testing sounds finds other bugs:** reading health around his dash's heal showed shields that never took healing
  (`Shields`) and a dash that healed 66 of its 55; the kill lines showed Superbloom's 40 counted as a melee blow.

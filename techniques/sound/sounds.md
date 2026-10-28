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
- **Music:** the match-end music was traced through the music-director graphs (0D3B, 1B47):
  [sounds-round8](../../tools/reference/sounds-round8.md), [music-round9](../../tools/reference/music-round9.md). Its
  streams are 12-channel Opus and need a stereo downmix.

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
  - <a id="sombra-emote-sounds"></a>**Sombra's (2026-11-01; `emotes.txt`, Sombra's section; scripts
    `research\sombra\scripts\emote_sounds\`):** 53 02Cs and 11 voice stimuli over 19 of her 20 emotes (Groove has
    none; Choreography only its three lines). How it was run, every step offline:
    `emote_fx.py` on `emotes/sombra.json` -> `ww.py` on its 02Cs -> her `dump_banks.py` / `extract_banks.py` (43 own
    banks into `ExtraSounds\Sombra\Emotes`) -> `measure_emotes.py` (shared ones as `Lúcio:` / `Kiriko:` / `Mercy:<02C>`)
    -> `sombra_what.py` -> `gen_sombra.py` -> `apply_emote_sounds.py sombra <repo>` -> `build_asset_pack.py sounds`.
    - **Run the shared generator without editing it:** `gen_sombra.py` imports `gen_emotes.py` from
      `research\emote-sounds\scripts`, adds her `HERO` and calls its `main(["--hero", ...])`; her data files sit beside
      the other heroes' in `research\emote-sounds\` (where `emote_events.py` looks for them).
    - **An emote's foley can be a kit:** most of her own 02Cs are Layer containers of the same short files (ten high
      swishes, low thumps, four bright taps) at fixed delays; Marioneta's 4658 is 29 layers. Each file's delay and gain
      differ inside a layer; the builder keeps one per layer, so read the rule's "some files differ" note.
    - **Cloth rustles cross heroes:** her Dance's 3BEC / 3BFF use the same 17 cloth-rustle files as Ana's 3901 / 3956
      (by 03F id), on the shared cloth mixer 43599842.
    - **A Stop event can Play:** Sit's 3FB1 (the equaliser hologram's hum, its window the hologram's) has a Stop event
      that stops the looping hiss and Plays a 0.9 s file (2A8C7A). The builder reads only Stops there, so that closing
      sound isn't made (open).
    - **Sounds on a prop's clip:** nine of Pet Slicer's 02Cs (A6C4-A6C9, A6D1, A7CA) are on the robot pet's clip DC9C,
      a `"skeleton": "prop"` clip. `EmoteSounds` plays the events of the hero's plan clips only (`data.events(emote,
      clip)` for `EmotePlan.at`'s clip), so by reading the code these never play; the same holds for Kiriko's fox
      (E921) and whirlwind (C217, C2E7) sounds. Not checked in game (open).
    - A Silence source again: Dance's held B1BC (bank 1E79B) lists no file, like Ana's F8A7: an empty rule.
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
- <a id="sombra-sounds"></a>**Sombra's map (2026-11-01; `tools/soundmaps/sombra.txt`, scripts and data in
  `research\sombra\scripts\sounds\` and `research\sombra\sounds\`; hub: [sombra](../heroes/sombra.md)):**
  - **An automatic weapon's sound is a self-repeating stream.** Her pistol's 02C (5DCC, 680F) holds continuous random
    containers in trigger-rate mode paced by the fire-rate parameter 4066808683 (0.05 s at its default 100 = 20 a
    second): one Play keeps re-triggering until the Stop event breaks it (its tail rings out, a click plays). Built as one
    trigger of each container for the game to play per shot, the 2.5-3 s tail apart (`pistol.fire_end`), else 60 tails pile up.
  - **Audience 1 can carry a range:** the beacon's hum (3B20) and its landing (3B85) are hers (audience 1) but ranged,
    because they sound at the beacon, not in her head.
  - **Some Play targets exist nowhere:** Hotfix's 10552 / 1042B Play four objects that are in none of the build's 1148 banks
    (`find_object.py`): those layers don't exist; don't hunt for them.
  - **A hero's voice set can lack a shared moment:** hers has no Resurrected (005A) and no reload (0E2C) stimulus, so
    `sombra.voice.resurrected` stays empty. Check `voice_lines.json` before promising a line.
  - **Graph voice nodes are only half the story:** Translocator/Use (0698) and Uncloak (06E7) are raised through their
    criteria's scripted events (2061 from 0C19 #147, 2263 from 420C #16); grep the decompiles for the event id. A
    stimulus whose event no graph raises (StealthActivate 069D, 205B) is the old kit's: leave it out.
  - **Building into the dev client's pack from another worktree:** `OC_ASSET_PACK=<pack> py tools/build_asset_pack.py sounds
    <root>`; the step rebuilds every map of the worktree it runs from, so that worktree's maps must be current.
- <a id="brigitte-sounds"></a>**Brigitte's map (2026-11-01; `tools/soundmaps/brigitte.txt`, scripts and data in
  `research\brigitte\scripts\sounds\` (Sombra's pipeline, `brigitte_gen.py` writes the map) and `research\brigitte\sounds\`;
  hub: [brigitte](../heroes/brigitte.md); kit ids: `tools/reference/brigitte-map.md`):**
  - **A melee or projectile's hit sounds live in its contact set, not in an effect.** Her flail's sets 061B / 061D (the
    swing's `STU_23A00A53` nodes) and Whip Shot's 0620 hold them: contact type 0 = characters (a no-range copy, read as
    hers, plus two ranged copies with the same files), type 2 = the world (two different files, both play), material
    0015 = a barrier. 0015 points at the same 1833 / 1834 pair in Sombra's 05A7: it is the shared barrier impact, so a
    barrier's "being hit" sound is the attacker's, and her barrier pet (0EE4) has none of its own. Grep the decompiles
    for `m_contactSet`; the hero entity's or a prop's own set (the flail head 2283's 0089) may be a different one.
  - **Effects can hide in expressions.** The shield-up effect is `{[!Rally] 21B6; [Rally] 6F8D}` in one field (0EE3 #545):
    `fx_graph`'s whole-value match misses it, so its sounds never reached `fx_sounds_timed.json`. Grep the decompiles for
    `.00D` inside expressions and read those effects on their own (`fx_timed.py`, `sounds\fx_proj.json`).
  - **A counter can pick the copy.** The Repair Pack's launch (2EC3 / 4 / 5), recharge (2EC6 / 7 / 8) and landing are
    `ActionSwitch`es on the charges v11: built as `throw_3 / _2 / _1` and `recharge_1 / _2 / _3` (the landings all hold
    the same two 02Cs, so one rule).
  - **A big make-up gain can be a filter's answer.** Whip Shot's hit 87F2 has one container at +23 dB make-up behind a
    low-pass 18 / high-pass 22; the builder doesn't filter, so built as is it sat 16 dB above everything else and pulled
    the whole own family 17 dB down. Left out (the rule says why). Before trusting a family offset, look at what sets it.
  - **A prop's own state machine can carry the sound.** The Rally shield's deploy / fold clanks (A095 / A097) are on the
    shield model's transition clips D5C4 (2 -> 0) / D5C5 (0 -> 2) in set 060A, not on her clips or graphs.
  - **Audience 1 on a 3P clip:** Whip Shot's catch 6347 is audience 1 with no range but only on her 3P return clips
    (51CA / 51EB / 5056 / 51EC); built once for everyone (M).
  - **Her emote sounds** (`emotes.txt`, Brigitte's section; `research\brigitte\scripts\emote_sounds\`): as Sombra's, but her
    Taunt's 4790 is a random container of three Layer takes, which the shared `emote_events.py` stops on. Patched at
    import (`brigitte_patch.py`: the first take is built) so the shared generator still runs unedited.
- <a id="juno-sounds"></a>**Juno's map (2026-11-01; `tools/soundmaps/juno.txt`, read for wiring in
  `tools/reference/juno-sounds.md`; scripts `research\juno\scripts\sounds\` (Brigitte's pipeline, `juno_gen.py` writes the
  map), data `research\juno\sounds\`; hub: [juno](../heroes/juno.md)):**
  - **A volley's stream is built per volley, its time modulators at their start.** The Mediblaster's 5C2D is held for each
    0.353 s volley. Its containers are trigger-rate repeats (the shared fire-rate parameter, 0.06 s at its default). Its
    time modulators would pitch the stream 527 cents down at their end, the value a loop is read at, which is far past
    anything a volley reaches. So `juno_gen.py` passes them as 0 (a scenario) and repeats each trigger-rate container at
    its period across the volley (`tile=`). The same `tile` builds the torpedo lock tick, 0.02 s repeats, as 1.0 s of it.
  - **The loop rule only counts `Sound` beds.** The torpedo's flight loop is three single-file *random containers*. The
    rule saw no bed and took the loudest layer's period: a 2 s pitched layer. That dropped the two 6 s beds as "longer
    than the loop". Check `--print`'s dropped lines on every loop; `looplen=` sets the length. A loop that lives a fixed
    time with beds of 7-22 s, like the Orbital Ray's 8 s hum, is better built as one long one-shot that the game stops.
  - **An effect can pick its copy by the local player, not by audience.** 4795 #395 asks whether the local player is Juno:
    on her client it plays 5A90 (the heal confirm), and on everyone else's 56DD (the healed ally's own sound plus a 12 m
    copy). The same holds for 47DD's "you are the torpedo's target".
  - **Projectile slots, again:** `m_2170A015` is the "sound only" slot as the projectile is made (the Mediblaster's 5C5F, and
    the ring's 5F2F). `m_A18274A3` is "on a target", with the shared 4010 -> 8DE7 crack, as in Ana's map. Child entities
    of a projectile carry its flight loop (the torpedoes' 5350 / 5A5B).
  - **The decompiler can print two nodes with one uid** (4695 #1132, 4441 #586, 4857 #1205). Their edges are listed
    together, so a branch read from them is L until the node types are told apart.
  - **A Play target can exist nowhere** (as Sombra's Hotfix): the torpedo "incoming" warning's B39F plays an object in none of
    the 1148 banks. The moment is in the graph, but the sound is not in the build.
  - **Adding one hero to another worktree's pack without touching the rest.** The sounds step deletes the whole
    `sounds\` folder and rebuilds it from the running worktree's maps. To avoid that:
    1. Copy `build_asset_pack.py` into a scratch root that holds only `tools/soundmaps/<hero>.txt`.
    2. Build there with `OC_ASSET_PACK=<scratch pack>`.
    3. Copy `sounds\<hero>\` into the target pack.
    4. Merge only the `<hero>.*` keys into its `sounds.json` and `lang\en_us.json`.

    Back the two JSON files up first.
  - **Where the extra banks went:** for this pass the owner asked for extracted audio under `research\juno\sounds\`. The
    banks her export lacked are in `research\juno\sounds\ExtraSounds\Juno`, reached from the build root as
    `../../research/juno/sounds/ExtraSounds/Juno/...`. `measure_juno.py`'s `TOPS` points there.
  - **Wiring it (step 7c, [juno](../heroes/juno.md)):** a stop names the sound, not one copy: the torpedo lock's tick plays at
    each target, so when one locks all stop and the ones still filling start again (`Torpedoes.ticking`). Another player's
    motion arrives in held steps: a double jump read from it was missed or doubled, so her own client tells the server
    (`EXTRA_JUMP`), and a hover is read over 4 ticks. Loops that follow something in flight are client instances that move
    with it (`client.JunoSounds.Following`).
  - <a id="juno-emote-sounds"></a>**Her emote sounds and voice lines (2026-11-01; `emotes.txt`, Juno's section; scripts
    `research\juno\scripts\emote_sounds\`; played: [juno step 8b](../heroes/juno.md#step-8b-emote-sounds-voice-lines-played)):**
    48 02Cs and 21 voice stimuli over her 22 emotes, Brigitte's pipeline with no patch (no random-of-layers case):
    `emote_fx.py` -> `ww.py` -> `dump_banks.py` (banks 1E828, 1E7FE, 1E787, 1E79B) -> her `extract_banks.py` (35 own banks
    into `ExtraSounds\Juno\Emotes`) -> `measure_emotes.py Juno ... Mercy:C440 Kiriko:FEC7 Lúcio:E548 ...` ->
    `juno_what.py` -> `gen_juno.py` -> `apply_emote_sounds.py juno <repo>`, then the scratch-root build and merge above.
    - **The TankLib scripts need the research venv:** `py -I` has no pythonnet (it skips the user site-packages, where
      `clr_loader` isn't anyway). Run them with `E:\Overwatch Archive\research\kiriko-anim\venv\Scripts\python.exe -I`;
      `-I` still sees the venv's own packages. Text-only steps (measure, gen, apply) run under plain `py -I`.
    - **Shared 02Cs keep showing up under new emotes:** Orion Dance's music FEC7 / FEC8 is Kiriko's, Crazy Choreography's
      C440 / C441, Shuffle's E23F and Bunny Hop's F869 are Mercy's. Check every id against the earlier heroes'
      `<hero>_ids.txt` before extracting; measure the shared ones from that hero's folder and give them the same
      audience (the music: "own", group 8E muting it for the others).
    - **Her voice stimuli read straight from `voice_lines.json`:** `juno_what.py` builds `VOICE_IDS` / `VOICE_W` / `STIM`
      from it instead of copying lists by hand; each emote stimulus has its own `Unknown\<078>` folder.
    - **Voice-line unlocks were only ever built for Kiriko, Lúcio, Mercy (and Jetpack Cat's table):** Ana, Sombra,
      Brigitte and Lifeweaver have none in the pack yet. A hero extracted under `research\<hero>\extract` needs
      `VOICELINE_ROOTS` (`build_asset_pack.py`), its path from the build root as the hero's soundmap uses.
    - **The dev `key <action> tap` doesn't end an emote:** it releases the key before `ClientHero` reads the input, so the
      emote (and its loops) kept going. Hold it (`key forward down`, 0.25 s, `key forward up`), as a player does.
- <a id="sojourn-emote-sounds"></a>**Sojourn's emote sounds and voice lines (2026-11-02; `emotes.txt`, Sojourn's section; scripts
  `research\sojourn\scripts\emote_sounds\`; played: [sojourn step 9](../heroes/sojourn.md#step-9-her-emotes-from-the-data-played)):**
  17 02Cs and 5 voice stimuli over her 11 emotes, Juno's pipeline unchanged: `emote_fx.py` -> `ww.py` (`sojourn_ids.txt`) ->
  `dump_banks.py work\sojourn_banks_raw.json 00000001E5FA.043 00000001E599.043` -> her `extract_banks.py` (10 own banks into
  `ExtraSounds\Sojourn\Emotes`) -> `measure_emotes.py Sojourn ... Lúcio:E548 ...` (the 7 shared) -> `facts.py` / `summary.py` ->
  `sojourn_what.py` -> `gen_sojourn.py` -> `apply_emote_sounds.py sojourn <repo>`, then the scratch-root build and merge above
  (the scratch root's `tools/soundmaps/` held only her section, as `emotes.txt`).
  - **No id shared with another hero's emotes** but the achievement ones; each of hers is one long mono bed held over its
    clip at 15 m (no loops). The one named stimulus folder: Guffaw's laugh 06A1 is in `HeroVoice\Sojourn\Laugh` (with other
    laughs), so `VOICE_IDS` keeps only its line; the others are `Unknown\<078>`.
  - **6BDB plays just before a stow window ends** (Heroic 4.17 against 4.07, Triumph 5.83 against 6.0): read as the gun
    returning to her hands (M), a use of the stow windows to name a sound.
- <a id="sojourn-sounds"></a>**Sojourn's map (2026-11-02; `tools/soundmaps/sojourn.txt`, read for wiring in
  `tools/reference/sojourn-sounds.md`; scripts `research\sojourn\scripts\sounds\` (Juno's pipeline; the event table is its own
  file, `sojourn_table.py`, and `sojourn_gen.py` writes the map), data `research\sojourn\sounds\`; hub: [sojourn](../heroes/sojourn.md)):**
  - **The order the pipeline runs in** (Juno's, written down): `fx_graph_<hero>.py` (text) -> `fx_sounds2_<hero>.py` (TankLib:
    every effect the graphs, clips and decompiles name) -> `clip_sounds_<hero>.py` / `fx_nodes.py` (text) -> `fx_timed.py` on the
    entities' slot effects and `contacts.py` on the contact sets (TankLib) -> `ww.py` on the 02C list, then `dump_banks.py` on
    the banks it names plus 1E818 (TankLib) -> `extract_banks.py` for the 02Cs her export lacks (DataTool) -> `measure_<hero>.py`,
    `overview.py`, `stmg.py --offline` (the Init bank is the build's: copy `stmg_1E818.bin` from any hero) -> `voiceset_<hero>.py`
    (TankLib) -> `<hero>_voice.py` -> `<hero>_gen.py --print`, then `--write` and `check_map.py`.
  - **Pass `dump_banks.py` the banks' full names** (`00000001E7A3.043`): its keys must match `ww.py`'s `bank` field, which
    `emote_events.node()` compares; short names (`1E7A3.043`) leave every node unfound. Sojourn's were re-keyed after the run.
  - **A held automatic stream, split three ways.** The Railgun's 2BF5 (643C / 8AF0) is Sombra's kind of stream, held for the
    whole burst (1A23 #571), not per volley as Juno's. Its "once" containers sound only on the first trigger, so it is built as
    `fire_first` (once + one stream trigger), `fire` (one stream trigger, every later shot) and `fire_end` (the ring-out and the
    Stop sets).
  - **A shot can carry a charge parameter.** The Charged Shot's containers follow game parameter 2139477060 (0-1, default 0):
    at 0 up to 10 dB quieter and +300 / +500 cents. Read at its default it would build the weakest shot. Built in
    Lifeweaver's three bins (`fire`, `fire_mid`, `fire_low`) through the rule's scenario; M that it is energy / 100.
  - **A weapon model's own clips carry its sounds:** the reload is on the Railgun's 1P / 3P clips 6FCB / 70D5 (set 0AB6), and
    the gun's energy state machine's transition clips 7D86 / 7D87 / 7D88 carry the only energy-state cues; her body's clips
    have none. `clipinfo.json` covers the gun's clips because it was resolved over all three models.
  - **A movement ability's surface sounds are contact sets on its rays.** Power Slide's ground ray (1E6F #272 StateTrackRay)
    holds a contact set while it finds ground (#273 STU_B63ECEB8: the scrape loop) and fires sweeps (STU_23A00A53) with their
    own sets as it lands, lifts and hits a wall. Grep the decompiles for `.049` as well as for `.00D`.
  - **A voice line can travel in a message.** Her Charged Shot kill lines are raised by ActionSendGameMessage (1B27) with the
    stimulus as a parameter, and her Overclock kill lines through an ActionSwitch on a kill counter that sets a stimulus
    variable (1A23 #1283 -> #1288 `m_stimulus = v45531`). Grep for `.078` anywhere in a node, not only in `m_stimulus`.
  - **The own ult copy can repeat either line.** Her own 185C (key F1AC) has the enemy line's text ("This ends now!"), where
    Juno's own copy has the friendly one's.
  - **Stadium effects may not exist in the build at all:** twelve of 2159's effects (behind 0425 / 0441) fail TankLib's
    OpenFile. A gated branch's missing effect isn't a reading error.
  - **The decompiler's `#0` is not one node:** graph entries and some STU nodes all print as `#0`, so an edge "from #0" names no
    one node (1E6F's 7601 and 7ABE, left out); duplicate uids (#1124 there) as in Juno's.
  - **`extract_banks.py` writes only under its allowed roots;** Sojourn's copy allows `research\sojourn\sounds` beside
    `extracted\`.
  - **No reload line at all:** her 01C2.05F has neither 0E2C nor a reload stimulus of her own (as Sombra).
- **What is still sounding:** the dev `playing` lists the overcraft sounds the engine still plays; a stopped channel stays in
  its map until 1 s after its start, so it asks each channel whether it stopped. Poll it with `heard` to time each loop's end.
- **Testing sounds finds other bugs:** reading health around his dash's heal showed shields that never took healing
  (`Shields`) and a dash that healed 66 of its 55; the kill lines showed Superbloom's 40 counted as a melee blow.

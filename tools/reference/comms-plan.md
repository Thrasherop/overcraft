# Communication wheel, voice lines, emotes, pings (#3, #4, #5, #40): the plan

The owner's spec: `comms-reference.md`. Priority 2 (after Kiriko's polish, before Lúcio). Hero-agnostic in code: a
`dev.overcraft.comms` package that any hero plugs into with its voice set and emotes, nothing Kiriko-specific.

## Pieces
- **Callouts** (`Callout`): the wheel's eight (NO, HELLO, NEED HEALING/HELP, GROUP UP, ULTIMATE STATUS, NEED HELP,
  SORRY, THANKS) and the tap's GOOD JOB. Each names a voice category (`<hero>.voice.<category>`, from the hero's
  HeroVoice folder: No, Hello, NeedHealer, GroupUp, Ultimate (charge %), AssistanceRequired, Sorry, Thanks, ...) and a
  chat line ("Kiriko: Need healing").
  - Server: `Comms.callout(player, callout)`: the chat line (hero's colour, a small icon) to teammates (scoreboard team,
    or every player when there are no teams) at any distance; the voice line in the world (Voice, its range); a
    cooldown against spam (Overwatch: "Too many messages! Please wait a few seconds...").
  - ULTIMATE STATUS says the charge ("Ultimate charge: 47%", or "My ultimate is ready!" lines).
- **Voice lines** (`VoiceLines`): a hero's collectible lines (HeroVoice/<Hero>/Voicelines: 96 for Kiriko, file names
  carry the subtitles); four equipped, chosen in a settings screen; the wheel's left page plays them (no chat, the
  subtitle shows over the hero).
- **Emotes**: all of a hero's emotes (their clips from the game data, the PreviewEmote entity's), chosen in the same
  settings screen; the wheel's right page plays them in third person (the camera pulls out, as Overwatch's does).
- **The wheel** (client screen-less overlay, like the perk menu): hold T, aim with the mouse (the cursor offset
  from the centre picks the slice; the view doesn't turn while it's up), release to play; centre = nothing; a tap
  (< 0.25 s, ours) = GOOD JOB. Left/right click while held: voice lines page / emotes page / back.
- **Pings** (`Ping`): the ping key (Overwatch PC default: middle mouse; rebindable) pings what the crosshair hits
  (a ray to 100 m, ours): a spot (blue diamond, distance, leader line to "CANCEL [key]"), a double press within 0.4 s
  (ours) a warning (red circle, danger glyph), an enemy (red, its portrait or face, its name; follows it; outlined
  for teammates), a "health pack" (to decide). Teammates see it; it lasts ~5 s (ours until measured); pressing the
  key on your own ping cancels it.
- **Sprays** (#41): hold Y. Later.

## Assets
- Voice categories per hero go in `tools/soundmaps/<hero>.txt` as `<hero>.voice.<category>` pools (as Kiriko's
  ability lines are); the pack rebuild picks them up. Icons for the wheel and pings: from UITextures (the wheel's
  glyphs) when found, else ours.

## Order
1. Callouts end to end with Kiriko (wheel, chat, voice), as a hero-agnostic package.
2. Pings. 3. Voice lines page and its settings. 4. Emotes (clips, third-person camera, settings).

# Techniques: how Overcraft gets built

The methods that paid off (and the ones that didn't), so they aren't rediscovered every session (owner, 2026-10-26).
It's a linked graph, like an Obsidian vault: start here, follow the links, and every note links back up. Read it
with [CLAUDE.md](../CLAUDE.md) (the rules) and [HANDOFF.md](../HANDOFF.md) (the state of play).

## How it's laid out (owner, 2026-10-27)
- One folder per subject. In it, a **general note** on the method (how to do it for any hero), and next to it
  **per-hero notes** (what that hero taught, with its numbers): [emote_reproduction](emotes/emote_reproduction.md)
  beside [kiriko_emotes](emotes/kiriko_emotes.md).
- A general note links to its per-hero examples; a per-hero note links back to the method and to the hero's hub
  ([heroes/kiriko](heroes/kiriko.md)).
- **Adding to it:** put a lesson in the note it belongs to (or a new note in the right folder), say where it came from
  (the owner, a critique, a measurement, the data), and link it from the notes that should lead to it. Lessons that
  hold for every hero go in the general note; numbers and quirks of one hero go in that hero's note.

## The map
- **Principles**: [principles](principles.md). The data first, contact checks, play before claiming, the owner's
  decisions.
- **Reading the game data**
  - [reading_game_data](game-data/reading_game_data.md): DataTool and TankLib offline, safely; ids; effects;
    statescript; research agents; the archive.
  - [model_data](game-data/model_data.md): inside a model: hardpoints, geosets, skeleton order, the cloth chunk.
  - [workshop_recordings](game-data/workshop_recordings.md): measuring movement in the real game with a Workshop
    script, when no source has the numbers (Lúcio's wall ride).
- **Animation**
  - [animation_import](animation/animation_import.md): getting clips out and into the game; frames; additive and
    partial clips; finding the right clip.
  - [critique_parts](testing/critique_parts.md): the independent read of weapon parts, aim sheets and glows, 2026-11-04.
  - [additive_clips](animation/additive_clips.md): which clips are additive (bit 0x4 of the folder), what the
  - [aim_sheets](animation/aim_sheets.md): how the body and gun follow the aim, every hero's sheet layout; [weapon_parts](animation/weapon_parts.md): the data's magazines and vials, a table; weapon clips by rule; on-gun displays.
    exporter does with them, and adding them in the bone's own frame (#75).
  - [cloth](animation/cloth.md): cloth is sometimes animated and sometimes simulated. For Kiriko, see
    [kiriko_cloth](animation/kiriko_cloth.md).
- **Emotes**
  - [emote_reproduction](emotes/emote_reproduction.md): the method, from the data: props, hardpoints, weapons, clip
    chains, the coat, review.
  - For Kiriko, see [kiriko_emotes](emotes/kiriko_emotes.md); for Lúcio (a weapon that's an entity of its own),
    [lucio_emotes](emotes/lucio_emotes.md); for Mercy (two weapon entities, controls read raw by slot),
    [mercy_emotes](emotes/mercy_emotes.md); for Ana (child-entity props, carriers posed at rest, the gallery preview tree),
    [ana_emotes](emotes/ana_emotes.md); for Sombra (a prop's clip starting in her stance, hologram materials),
    [sombra_emotes](emotes/sombra_emotes.md); for Juno (weapons that are geosets of her body, mask bits that aren't the
    geoset bits, an intro just off her stance), [juno_emotes](emotes/juno_emotes.md); for Sojourn (weapon clips whose
    root is a bake of her carrier bone, a gun laid down and kicked back up), [sojourn_emotes](emotes/sojourn_emotes.md); for
    Sigma (a weapon clip keyed in his space that isn't his hand, props spawned by a prop's clip, a prop two heroes share),
    [sigma_emotes](emotes/sigma_emotes.md).
- **Testing and review**
  - [hand_play](testing/hand_play.md): playing it like a player, catching glitches, test worlds.
  - [harness](testing/harness.md): the dev harness, its commands and its traps.
  - [visual_review](testing/visual_review.md): comparing with the real game, frames at exact times, critique agents.
- **HUD**: [reticle](hud/reticle.md): every hero's widgets at the crosshair, what the data binds and what's ours (#85). [kill_feed](hud/kill_feed.md). The kill feed's icons from the wiki's rule, what dealt each hero's
  damage (`Blow`), the kill marker from the death itself, the flashes and markers around a kill.
- **Rendering**: [rendering](rendering/rendering.md). Minecraft 26.3's renderer, effects, glows, shadows, held items,
  fonts.
- **Sound**: [sounds](sound/sounds.md). Bound and in-the-head sounds, checking what played, voice and music from the
  data.
- **Minecraft**: [minecraft_26](minecraft/minecraft_26.md). Engine facts that cost time: input codes, projectiles,
  the dragon, the HUD bars, cameras. [barriers](minecraft/barriers.md): barriers as entities, what stops Minecraft's
  projectiles, blasts and hits (Sigma's, Brigitte's).
- **Building**: [building](building/building.md). Gradle, editing safely, git on this checkout, resource names.
- **Heroes**: [kiriko](heroes/kiriko.md), her hub; [lucio](heroes/lucio.md), his;
  [jetpack_cat](heroes/jetpack_cat.md), hers; [mercy](heroes/mercy.md), hers (her kit read node by node);
  [lifeweaver](heroes/lifeweaver.md), his (the fifth); [ana](heroes/ana.md), hers (the sixth); [sombra](heroes/sombra.md), hers (the seventh); [brigitte](heroes/brigitte.md), hers (the eighth); [juno](heroes/juno.md), hers (the ninth); [sojourn](heroes/sojourn.md), hers (the tenth and the first Damage hero,
  being built); [sigma](heroes/sigma.md), his (the first Tank, being built: the Tank role, his body box).

## Reference material (research outputs, not techniques)
`tools/reference/`:
- [kiriko-reference](../tools/reference/kiriko-reference.md), [kiriko-sounds](../tools/reference/kiriko-sounds.md),
  [kiriko-voice](../tools/reference/kiriko-voice.md), [kiriko-effects](../tools/reference/kiriko-effects.md),
  [lucio-effects](../tools/reference/lucio-effects.md).
- [sounds-round8](../tools/reference/sounds-round8.md), [music-round9](../tools/reference/music-round9.md).
- [movement](../tools/reference/movement.md), [perks](../tools/reference/perks.md),
  [fonts](../tools/reference/fonts.md), [comms-reference](../tools/reference/comms-reference.md),
  [lucio](../tools/reference/lucio.md).
- The critiques (`critique-*.md`).

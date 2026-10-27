# Principles

The rules of thumb under every other note. Up: [Techniques](README.md).

## The data first, always (owner's rule and requirement, 2026-10-27)
"WHENEVER POSSIBLE, ALWAYS TRY TO USE THE REAL DATA ... Before you implement, assume, or infer anything, try to find
the answer directly in the pulled data. Don't try to re-assemble emotes if the data has an exact cheat sheet."
- **It works.** Emote props placed by eye were wrong round after round; read from the data they came out right, and
  the owner called the result "SO much higher quality ... Always do that for emotes and animations if at all
  possible". Kiriko's coat, simulated by our own guesses, was "super wonky"; driven by the data's cloth drivers it was
  "spot on ... perfect matches". See [emote_reproduction](emotes/emote_reproduction.md) and [cloth](animation/cloth.md).
- **Search the data before measuring.** Measuring by eye is the fallback, and says it's one in the code and the data
  file ("inferred", "ours").
- **"Not in the clip" isn't "not in the data."** Look for what drives a thing: a cloth chunk's driver bones, an
  effect's spawns, a blend tree's geoset flags, a statescript's waits ([model_data](game-data/model_data.md),
  [reading_game_data](game-data/reading_game_data.md)).
- **When the data surprises you, check it before overriding it.** "The donut is gone at 1.0 s, too early": it was
  3.3 cm from her mouth at exactly 1.0 s, so she pops it in whole.

## Check a guess against a contact the motion must make
Anything borrowed or inferred (a hardpoint another model defines, a bone picked from a clip) gets a test the
animation must pass: a sip reaches her mouth, a hand rests on the cat, a fist closes across a handle.
`tools/emote_contacts.py` measures it ([emote_reproduction](emotes/emote_reproduction.md)).

## Play before claiming (CLAUDE.md)
Nothing is done until it has been played in the real game and looked at: [hand_play](testing/hand_play.md),
[visual_review](testing/visual_review.md).

## Before changing behaviour
- **Read HANDOFF.md's "owner's decisions" first.** A default that looks like a mistake may be the owner's choice: the
  fire buttons (LMB kunai, RMB ofuda) are swapped from Overwatch's on purpose, and were nearly "fixed" back.
- **Check the wiki before "fixing" a fact from memory**: Sombra looked misfiled as a support, but the wiki has her as
  one since 2026.
- **Ask what the real thing looks like before building to a critique's guess**: the critic wanted the ofuda's paper
  card shown; the owner (who plays) said thrown ofuda are glowing gold or blue talismans, and the effects survey
  agreed.
- **When a research note recommends a model, re-read its source sentence before building it**: the movement note
  said "keep the approach model, it matches Goodman" while Goodman's post says the opposite (speed isn't slowed in a
  turn).

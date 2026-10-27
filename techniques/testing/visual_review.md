# Visual review: comparing with the real game

How to look at what we made closely enough to see what a player sees. Up: [Techniques](../README.md) · playing:
[hand_play](hand_play.md) · emotes: [emote_reproduction](../emotes/emote_reproduction.md#11-review-every-emote-critically-at-every-angle)

## The owner's screenshots are the spec
- They live in the session transcript (`~/.claude/projects/<project>/<session>.jsonl`): images in user messages, and in
  `attachment.prompt` for messages queued while working. A short script decodes them to files (scratchpad
  `owner_imgs3.py`) so they can be cropped and compared, not just remembered.
- Read them closely: HUD layout, the ofuda pips under the crosshair, which arm throws, how the arms sway. Zoom in,
  compare side by side with ours.
- **Lay the HUD out in pixels of the reference screenshot** (1920x1080) and scale to the window. Then crop the same
  region of ours and the reference at 2-3x and compare side by side. Check other window shapes too: 5:4 and 21:9 found
  the HUD overlapping itself.
- **Crop and enlarge** (nearest-neighbour at 2-6x) at native 1080p to judge edges: a faint box round the volley bar
  only showed against the sky, zoomed.

## Frames at exact times
- **Hold an animation at a time** (`emoteat <s>`) rather than sleeping between screenshots: sleeps drift, and missed
  a 0.73 s prop window.
- **Several angles at the same time:** front, side and back (`mouse 600 0` turns the emote camera a quarter). A front
  view along her arm made a correct mic look like it covered her face; the side view and a measurement showed it
  right.
- **Crop close to the subject:** at the full frame an upside-down cup read as "a cup in her hand". The scratchpad's
  `emote_frames.sh` takes 8 frames per emote, crop 720,180-1200,900, as a contact sheet.
- **Before and after**, side by side, with what changed written on the image: the owner judges from them
  (`run/share/`).

## Critique agents
- **Nitpick agents:** give one our screenshots and the references, and ask for every difference, however small (the
  owner: "many things you'll see as a nit are a big deal for humans").
- **Critique without the answers:** don't tell the agent what's already "fixed" (the owner: "have a sub agent
  critique your implementation and screenshots of every emote"). It found kunai in her mouth, weapons in the sky and
  coat problems.
- **Check its claims before acting:** it misread a mic's grip from one angle. Measure it.
- **A reference agent** collects how real gameplay looks (the web, the wiki's MediaWiki API) into
  `tools/reference/kiriko-reference.md`.

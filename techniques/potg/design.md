# Play of the Game with replay: the design

A design, not a build: nothing here has run in the game yet. Up: [Techniques](../README.md) · issues: #44 (the outro), #45
(the music) · builds on: [minecraft_26](../minecraft/minecraft_26.md), [kill_feed](../hud/kill_feed.md),
[rendering](../rendering/rendering.md), [emote_reproduction](../emotes/emote_reproduction.md),
[sounds](../sound/sounds.md), [music-round9](../../tools/reference/music-round9.md). Written 2026-11-06, on the owner's
"try to design a POTG system with replay".

## The recommendation
**When:** after the Ender Dragon dies, as #44 asks. VICTORY as now, the dragon's own 10 s death, then the POTG. It's the
one moment Minecraft has that really ends something. A dev command replays the last 12 s from the first day, because
that's how the replay gets tested without killing a dragon each time.

**How the replay works:** record what the server sends each hero, redraw it with stand-in entities ("ghosts") in the
world that's already loaded. The live world stays where it is, keeps running, and is hidden for 17 s:
- The server keeps a rolling record per hero of the packets that hero's client is sent (entities, sounds, particles,
  block changes, and our own hero and effect payloads), plus the hero's own position and look, which the server never
  sends back to them. Chunks aren't recorded.
- Every 5 s it writes a keyframe, using the calls Minecraft and the mod already make for a player who has just come to
  see something.
- Scoring runs on the server beside it, and the best play of each hero is kept when it scrolls out of the buffer.
- To play it, each client feeds the recorded packets through Minecraft's own handlers, pointed at the ghosts. Only the
  ghosts are drawn, and the camera sits on the hero's ghost.

**Why not the way replay mods do it** (a second world): swapping `Minecraft.level` rebuilds every chunk mesh on the way
in and out, needs a chunk snapshot in the buffer, and leaves the live world with nobody to apply its packets. That's
the heavy road on a machine that hard-crashed under load on 2026-11-04. See [the options](#the-options).

**Cost, estimated and not measured:** about 0.3 to 1 MB per hero for the rolling buffer, up to about 4 MB per hero with
kept plays. Nothing is recorded outside the fight in the first version.

**Smallest playable first step:** in single player, a dev command replays your last 12 s from a chase camera, with sound
and hero animations, while the world carries on. No card, no scoring, no first person. It answers the
questions that can sink the design ([what needs a spike](#what-reading-settled-and-what-needs-a-spike)).

## Questions only the owner can answer
1. **Is the dragon the right first moment?** And is a raid's victory the second? (Recommended: yes to both.)
2. **Is a key for "replay my last 12 s" wanted in normal play**, or only as a dev command? It would mean recording all
   the time in hero mode, not only in fights. (Recommended: dev command first, decide after playing it.)
3. **Does the world keep running during a POTG?** Recommended yes, with viewers protected: pausing stops our animators
   and effects, which run on the game's clock. The cost is that 17 s of the live world passes unseen. After a dragon
   that's harmless. In a raid, things may have moved when it ends.
4. **Everyone in the End watches, skippable, with an off switch in the options** (as #44 says). Can a viewer skip alone
   while the others watch on? (Recommended: yes.)
5. **Does a kill in Minecraft mode count?** Recommended no: the POTG is a hero's. If no hero scored anything, there's no POTG.
6. **Can you send a clip or two of a real POTG**, for the side by side? The sources don't describe the camera well enough
   ([what wasn't found](#what-wasnt-found)), and #44's "pulled back" needs a reference. What the camera does when the
   hero dies mid-play is the main unknown.
7. **Which highlight intro for each hero?** Kiriko has 18. Her default "Heroic" gets its clip from a UI dataflow not yet
   traced, so the first version may need one you pick from the named ones. Later: a choice per player, like emotes?
8. **What plays after the POTG?** #44 asks for "the rest of the outro". No music was found for the screens after it
   ([music-round9](../../tools/reference/music-round9.md)), and those screens need a scoreboard Minecraft doesn't have.
   Is ending on the POTG, then back to the game, enough?

## 1. What Overwatch's POTG is
Marks: **D** the pulled data (build 154088), **BZ** Blizzard, **W** the wiki, **ours**; **H/M/L** confidence. The wiki was
read through its API on 2026-11-06. Blizzard's patch notes page (newest entry November 5 2026) doesn't mention POTG.

### How the play is chosen
| Fact | Source |
|---|---|
| The server logs events during the match, each with a type, timestamps, participants, who caused it, and details (damage, kills, saves, distance, airborne, headshot, enemy action prevented). Some details are filled in later: a heal can be scored by what the healed teammate then does. | BZ H: patent application US15/374,523, granted as US10456680B2 |
| Each event is scored once per category. A sliding window passes over the match in increments, adding up each category's scores in the window, per player. The window matches the replay's length. The patent's example is 2 minutes; the game shows 12 s. | BZ H (method); W H (12 s) |
| Picks the category whose best window scores highest. Optionally, if High Score is under a threshold, the best of the others. Not random. | BZ H: an embodiment, so the live game may differ (M) |
| **High Score:** kills, total damage. **Life Saver:** damage prevented, healing or shielding, a teammate's killer interrupted, how many saved. **Sharpshooter:** distance, airborne players, elevation, cover, headshot, time from sight to shot, target speed; distance counts less for snipers, airborne less for heroes who fly. **Shutdown:** stopping a powerful ability, mainly an ult: its type, teammates nearby, how fast it was stopped, whether it was used to kill. | BZ H; W H agrees on the four names; W adds that High Score includes environmental kills |
| Ult events may be reduced by a set amount or share. | BZ M: "may" |
| No weights are public. Blizzard's lead engineer described reading real picks for patterns, and tools that re-run a recorded match under new numbers. | BZ M, through KitGuru's summary of a GameSpot interview: the original was blocked (403) |
| Retuned in May 2020 to favour active participation in fights; credit bugs fixed in April 2020. | BZ, through W: patch notes |
| A player who quits can't get it; the winning team needn't have it. Bots can't earn it. | W H; bots W L (the wiki marks it "citation needed") |

### What the viewer sees and hears
| Fact | Source |
|---|---|
| 17 s in all: the hero's highlight intro for 5 s, then 12 s from the player's point of view. | W H |
| The intro clips are that long: Kiriko's sixteen with a clip run 4.33 to 5.0 s, almost all 4.97 or 5.0. | D H: `research\kiriko-anim\resolved\clipinfo.json` |
| An intro plays on its own entity (Kiriko: `3E8B`, set `0EFB`), with the clip on the unlock. Her "Heroic" and "Random from Favorites" have no clip on the unlock: the UI supplies it. | D H: [kiriko-research](../../tools/clipmaps/kiriko-research.md) |
| 495 intro unlocks across the roster. | D H: `research\kiriko-anim\unlocks\unlocks_all_heroes.json` |
| An intro's animation effect (08F) holds its sounds, props and particles, read like an emote's. Six of Kiriko's read: 2 to 5 sounds each, props in some (Night to Day and Clean Sweep each have model components, Kitsune Rush an entity with its own clip). | D H: read for this note |
| **The six share one sound, `2F6C`,** starting between 2.83 and 3.67 s and held to the end. Probably the rise into the replay. | D H that it's there; what it is ours L: listen to it |
| A voice line in an intro is the exception: of the six, only Kitsune Rush has one (stimulus `1276` at 2.08 s, a shout). So heroes don't announce their own POTG. | D H for the six; M for all |
| Title strings: `0DC5` "PLAY OF THE GAME", `474A` "PLAY OF THE MATCH", and next to them `0DC6` " AS ", so probably a line naming player and hero. | D H (the strings); M (their use) |
| The card shows the player's name and hero; the patent adds the category as an option; a player's Signature shows too. | BZ M; W M |
| Skip and capture strings exist: `1DFA8` "Skip Highlight", `67EC` a prompt to capture the POTG. | D H |
| The announcer says the line: voice set `001D` (researchnnouncer) has one each for game and match, and the wiki has them as Athena's. Events swap the announcer (Sigma, Junker Queen, Mei and others have an "Event-Specific" line), and the Stadium announcer has one. | D H; W H |
| Music: candidate cue `5B1F` (16.84 s, stereo, peak at 7 to 12 s), played by message `19C8` in graph `0D3B`, and directly by the end-of-match graph `1B4C` (node 1367). Its length fits the 17 s. The wiki calls it the victory theme. | D M-L: [music-round9](../../tools/reference/music-round9.md), confirm by ear |
| First person from the player who caused it, or third person on where it happened. Combined plays may swap views; filters, slow motion, tracing a bullet are options. | BZ M: embodiments |
| The real thing replays the game, not a video: sprays, skins, team colours and voice lines have all had POTG bugs, and it had "kill cameras" improved in May 2019, jitter above 60 fps fixed. | BZ, through W, H. The patent also covers a reel of network events with per-tick positions, which is the approach below |

### What wasn't found
- **The camera's moves.** Nothing public or in the data read says when it leaves first person. #44's "pulled back" is the
  owner's memory, and it needs a clip (question 6).
- **The card's art and layout.** No POTG texture was picked out of the 21,567 UI images. The strings are all there is.
- **The intro's camera and backdrop.** Whether the clips carry a camera wasn't checked, nor where the hero stands.
- **What `2F6C` and `5B1F` sound like.** Not extracted or heard. Nothing was extracted for this note.
- **Blizzard's original GameSpot interview** (blocked), and the patent past its first 100,000 characters.
- **Graph 1B4C's timings.** It has waits of 3.75 and 5.25 s, but nothing ties them to the POTG. Not decoded.

## 2. When it happens in Overcraft
There's no match. A POTG needs an end, or it's an interruption.

| Moment | For | Against | Verdict |
|---|---|---|---|
| **Dragon kill** | The owner asked for it (#44, #45); a real end; VICTORY and music are there or coming; everyone's together; nothing's left to attack the viewer | Only once or a few times a world | **First** |
| **Raid won** | A real end, a real fight, lots of kills. `Raid.isVictory` | Mobs may remain; villagers need the player | Second |
| **A key, on demand** | Tests everything; Overwatch has Highlights too | Not an event; needs recording always | Dev command first (question 2) |
| **A death** | Overwatch's kill cam, on the same machinery | From the killer's view it needs the killer's record: a mob has none. Happens often | Later, perhaps |
| **A night survived** | Survival's natural beat | Nothing climactic; often no fight at all; players are asleep | No |
| **Wither or Warden kill** | Same as the dragon | Needs its own victory first | Later, a one-liner once the dragon works |

**The dragon's timeline** (read in `EnderDragon.tickDeath` and `Overcraft`, `VictoryHud`; the sequence is ours):
- 0 s: the dragon's `AFTER_DEATH`. VICTORY shows for 5.65 s, with its music (`Overcraft`, `VictoryHud`). Scoring stops.
- 0 to 10 s: Minecraft's death, 200 ticks: rising, explosions from tick 180, XP from tick 150.
- 10 s: it's removed, the portal and egg appear. The POTG starts about here, so the player sees the death. Victory's 33 s
  music fades as the POTG music starts.
- 10 to 27 s: the announcer's line, the intro (5 s), the replay (12 s), cue `5B1F`.
- 27 s: back in the live game; everything that's dropped is waiting. The hero's XP is theirs anyway.

Plays from the whole fight count, as in Overwatch, so a 20 s buffer alone isn't enough: best plays are kept
([below](#what-gets-recorded)). A dragon fight has one more rule, the owner's in #44: the last hits count. They'd score
anyway since they're fresh; the kill gets a bonus (ours).

## 3. Scoring from what the mod knows
Scoring is server-side, where the mod already has what it needs at a few choke points:

| What | Where it is now | What's to add |
|---|---|---|
| All hero damage, attacker, target, amount, `Blow` (kind, icon, crit, hero) | `Combat.deal`, under `Combat.damage` and `damagePart`; the dragon's parts included | One call to a new `PlayScore` |
| Kills, killer, environment kills after a push, headshot | `KillFeed.onDeath`, `Combat.pushDeath`, `recentPush` | One call |
| Multikill chain (80 ticks, ours) | `HeroState.killChain`, `Voice.onKill` | Read it, or count within the window |
| Who counts | `Allies.isBystander` (no chicken farms, #99), `Allies.wasAlly` | Use the same rule |
| Healing by a hero | `Combat.addFireForHealing(hero, healed)`, called across the kits; `Combat.heal` itself doesn't know the healer | Hook `addFireForHealing`; check every heal reaches it |
| Ult in progress | `HeroState.ultActive` per hero | Ult *starts* have no choke point: 10 `Combat.afterUlt` sites take no player. Add one |
| On Fire score | `HeroState.fireScore` | Nothing: a check to compare the score against |
| Shutdown | Victim's `ultActive` for a hero | A "was dangerous" rule for mobs: a lit creeper, an evoker casting (ours) |
| Sharpshooter | Crit from `Blow`, positions at the kill | Distance and air on the blow, target's speed. No cover or sight time: skip those |
| Saves | Only ingredients: `Combat.phase`, barriers' `took`, overhealth | The hard one. A new rule: an ally under 25% health with an enemy on them, healed or protected, and alive 3 s later (ours) |

**Method** (the patent's, weights ours):
- Each hero has four per-second rows, one per category. A 12 s window slides over them in 1 s steps.
- Every step: if a window beats that hero and category's best, keep it.
- At the end: the category with the highest best window wins, High Score under a threshold falling to the others.
- A kill is worth the victim's health in Overwatch points, capped, so a zombie is worth little and an enemy hero or
  the dragon a lot, and bystanders nothing. Ult events scaled down, 0.75 to start with (ours; the patent allows it).
- **Every weight is ours, with a comment saying so**, and no public source will change that.
- Tune as Blizzard did: dump a fight's events as JSON, and let `gradlew test` re-pick from them under new numbers. The
  logic sits apart from Minecraft, like `KillFeed.iconTokens`, so it can be unit-tested.

Unit tests choose among plays; only the owner can say the play was the right one. Ask after real fights.

## 4. Replay
Everything marked **read** was read in the 26.3 sources (`.gradle\loom-cache\minecraftMaven` in the main checkout; no
need for `genSources` here). Nothing was run.

### The options
| | A. Second world, as replay mods do | B. Own state, redrawn by hand | **C. Recorded packets, ghosts in the loaded world** |
|---|---|---|---|
| Captured | All packets, chunks included | Our own format per entity type | The packets that matter, keyframes, own track |
| Shown in | A new `ClientLevel`, `Minecraft.setLevel` | The live world | The live world |
| New code | A fake connection, a second listener | An encoder and decoder for every entity's state: dragon phases, crystal beams, equipment, poses, every mob | One tap, about five choke points, a keyframer |
| Fidelity | Best | Whatever we remembered to record | Close: what a client saw, through vanilla's handlers |
| Load | Chunk snapshots; meshes rebuilt twice | Low | Low |
| Live world | Its packets have nowhere to go | Fine | Fine |
| Blocks | Right by construction | Rewind needed | Rewind needed |

**A, read:** `Minecraft.setLevel` goes to `updateLevelInEngines`, which stops all sounds, clears the camera and hands the
new level to the extractor, the particle engine and the renderer. A rolling buffer also needs a full chunk snapshot at
its start, every time it rolls. `ClientboundLevelChunkPacketData` copies each chunk's sections to build its buffer, on
the order of 12 MB a snapshot at view distance 12 (625 chunks, 20 KB each, ours). `ClientPacketListener.level` and
`Minecraft.player` are single fields, so a second world and the live one can't both be served without a second
listener. As far as I know replay mods get away with this because they leave the game to replay, but that's from memory: Replay
FPS's page confirms only that Replay Mod captures server-to-client packets, and neither Flashback's nor Replay Mod's
code was read. We can't leave.

**B, read:** nothing stops it, and the render path would be the same as C's. It loses because of what a mob looks like:
`SynchedEntityData`, equipment, passengers, attributes, the dragon's phase. Vanilla already says all that in packets.

**The patent's third way, a rendered video,** isn't practical here. Also rejected: a real recording connection on the
server, which is A's cost on the server's side.

### What gets recorded
A tap on `ServerCommonPacketListenerImpl.send` per hero (**read**: every packet to a player goes through it). Each kept
packet is encoded on the spot with the game protocol's codec (`GameProtocols.CLIENTBOUND_TEMPLATE`, bound with the
registries, as `Connection` has it) into a byte ring, with its tick. Encoding at once means no mutable `ItemStack` is held.
A netty handler after `"encoder"` is the other choice, off the main thread (`Connection.configureSerialization` **read**: the
same encoder runs even for single player's in-memory connection, so one path serves both). Start with `send`, simpler;
move to netty if the spike says so.

- **Entities:** `AddEntity`, `RemoveEntities`, `MoveEntity.*`, `EntityPositionSync`, `TeleportEntity`, `RotateHead`,
  `SetEntityMotion`, `SetEntityData`, `SetEquipment`, `SetPassengers`, `SetEntityLink`, `UpdateAttributes`, `Animate`,
  `EntityEvent`, `DamageEvent`, `HurtAnimation`, `UpdateMobEffect`, `RemoveMobEffect`.
- **World:** `BlockUpdate`, `SectionBlocksUpdate`, `BlockEvent`, `BlockDestruction`, `BlockEntityData`, `LevelEvent`,
  `LevelParticles`, `Explode`, `Sound`, `SoundEntity`.
- **Ours:** `EffectPayload`, `HeroActionPayload`, `OwnSoundPayload`, `VoiceLinePayload`, `HeroSyncPayload`, the attachment sync
  (`HeroManager.HERO`, `HERO_MODE`, `SONG`, ...).
- **Not:** chunks, chat, stats, inventory, abilities, `PlayerPosition`, `SetHealth`, boss bars, time, weather. Time and weather go in the reel's header.
- **The hero's own track:** the server doesn't send a player's own position to them (**read**: `ServerEntity` pairs with
  others). So every tick: position, yaw, pitch, pose, on-ground, swing, about 32 bytes. `Combat.animate` does send the
  hero's own clips to them (**read**), and `OwnSoundPayload` their own sounds.
- **Keyframes every 5 s**, or a rolling stream has no start: an entity already there never had an `AddEntity`.
  `ServerEntity.sendPairingData(player, consumer)` (**read**) gives exactly what a new viewer needs, for each entity the hero
  is tracking. `Effects.showHeld` and `Comms.showEmote` (**read**: the late-arrivals work of polish-2) do the same for held
  effects and running emotes; the tap takes those in capture-only mode, written to the ring and not sent.
- **Rolling buffer:** segments of 5 s, each starting with its keyframe; the last 25 s kept, so any 12 s window has a keyframe
  before it, and a run-in.
- **Kept plays:** when a window sets a new best, its segments are pinned, one per category per hero, at most 4.

**Memory, estimated from the packets' fields (ours, M-L; the spike measures it):**
| | bytes/s |
|---|---|
| 30 mobs moving, about 7 updates/s, 12 bytes each | 2,500 |
| 10 projectiles, 20/s, 12 bytes | 2,400 |
| Entity data, 30/s, 30 bytes | 900 |
| Sounds, 20/s, 40 bytes | 800 |
| Effects and hero actions, 30/s, 80 bytes | 2,400 |
| Own track and players | 1,500 |
| **A heavy fight** | **about 10 to 20 KB/s; a raid perhaps 30** |

Per hero: 25 s is 250 to 750 KB, keyframes add about 50 KB (40 entities, 250 bytes, 5 of them), and 4 kept plays at most
add about 3 MB. So under 1 MB rolling and about 4 MB in all per hero. Two heroes, under 10 MB. For contrast, A's one chunk snapshot is about 12 MB.

**Only while armed:** the first version records only heroes in the End with a dragon alive. Elsewhere it costs nothing.

### How the world is shown while the game goes on
**Does single player pause? Not with this design.** `IntegratedServer.tickServer` (**read**) pauses when `Minecraft.isPaused()`, which
is `gui.isPausing()` while not published. So a pausing screen stops the server, and a published LAN world never pauses.
But on pause `Minecraft` (**read**) stops the level's and particles' ticks and calls `soundManager.pauseAllExcept(MUSIC, UI)`. The tick manager's freeze isn't better: `ClientLevel.tick` runs `tickTime` only when `runsNormally()` (**read**).
Our hero animators take `now` from `level.getGameTime()` (`HeroModelRenderer.extract`, `submitArms`), and `Fx` runs on the game's clock. Both would stop. So
it'd take a second clock through all of it. Instead the world runs, in single player and multiplayer alike, one behaviour
to test. The viewer's own Esc still pauses a single-player world, and the replay with it, since it's driven by level ticks.

**Ghosts** are real entities built by vanilla's own `createEntityFromPacket` (a `RemotePlayer` for a player: **read**) with the
live `ClientLevel` as their level, kept in a map of ours, not in its entity storage. They read blocks and light from the live
level as they should. A new `Replay` class holds them, and a flag says "this call comes from the replay". The choke
points in `ClientLevel` (**read**), by mixin:
- `getEntity(int)`: under the flag, the ghost.
- `addEntity`, `removeEntity`: under the flag, the ghost map.
- `isTickingEntity`: under the flag, true (the handlers then interpolate, not snap).
- `entitiesForRendering()`: while replaying, only the ghosts. `LevelExtractor.extractVisibleEntities` (**read**) iterates exactly
  this, then checks `isEntityVisible`, including `isSectionCompiledAndVisible`, and applies the camera rule. So the live
  entities and the local player vanish, and the camera's own ghost isn't drawn in first person, with no more code.

**Playing a tick:** under the flag, decode the tick's packets and call `packet.handle(listener)`, the real
`ClientPacketListener`. Its handlers look up through `level.getEntity`, so they land on ghosts unchanged, as do our Fabric
receivers (`OvercraftClient`: `level.getEntity(payload.entityId())`). Then tick each ghost, which walks limbs, runs
interpolation, and moves the dragon's parts and latency ring. Then the hero's ghost gets its track.

**Handlers that reach past the level** need auditing, against `minecraft.player` mostly. Seen so far: `handleExplosion` pushes the
real player with `playerKnockback` (**read**), so strip that. `handleEntityEvent` only shows a totem when the entity is the real player, which a ghost isn't (**read**). Boss bars are left out.
This list is a spike item.

**Live packets during a replay** go on as normal: they update the live level, invisibly. Only what's global must be stopped:
- Sound: `SoundManager.play` drops anything not under the flag (music and UI excepted). `Camera` drives the listener, so
  recorded sounds come from the right place.
- Particles: `ParticleEngine.add` the same. `Fx` the same: live effect payloads are dropped.
- `HeroAnimator.BY_ENTITY` and `HeroModelRenderer.LAST_HERO` are keyed by entity id, and a ghost has the live entity's id. Swap them for fresh maps at the start and swap back at the end. Effects' statics are all listed in `OvercraftClient`'s DISCONNECT handler; `clear()` them at the start and at the end, and the server then re-sends what's held with `Effects.showHeld`, which is just a late arrival.
- HUD: `HudMixin` hides ours and vanilla's, drawing the POTG's instead.
- Input: all blocked, through `KeyboardInputMixin` and `KeyMappingMixin` as with the select screen; G and the hero menu too. One key skips.

**The viewer's own body keeps ticking**, with no input, so the client goes on sending its moves. If it stopped, a player in
mid-air would be kicked: `ServerGamePacketListenerImpl.tick` (**read**) counts ticks with `clientIsFloating`, and only a new move resets it. It keeps falling and lands.

### Camera
`Minecraft.setCameraEntity(ghost)` (**read**), the same thing spectating does. `Camera.update` aligns with whatever entity it holds;
`CameraMixin` already overrides position and rotation for `EmoteCamera` and `AbilityCamera`, so chase camera and pull-back go there.

- **Phase 1, chase:** behind the hero's ghost. Simple, and it shows the body animations work.
- **Phase 2, first person:** the hard bit of the camera. `HeroModelRenderer.submitArms` hard-wires `mc.player` and
  `ClientHero.hero()`, and first-person code all over assumes the viewer is the player (186 uses of the local player in 45 client files, not all relevant). It needs a "view subject". 20 Hz look with interpolation may look choppy: Replay FPS exists because of that problem with Replay Mod. If it does, the client sends its look at higher rate for the record.
- **Phase 3, pull-back:** to the side when the hero dies or as the clip's end nears, per a real clip (question 6).
- **Chunks:** only what the viewer has loaded can be drawn, since the server loads chunks around the viewer. In the End that's fine. A far-off play from a viewer's point of view shows holes: the server compares the play's position to the viewer's view distance, and sends the card alone if it's too far, or in another dimension (ours).
- Sections that aren't compiled hide their entities (`isSectionCompiledAndVisible`), so a cut to a far camera needs a moment; start the camera during the intro's fade.

### Blocks changed since
The play may be minutes old, and the dragon and crystals break things. Phase 1 ignores this and shows today's blocks.
Later, a log per world: position, old and new state, tick, from `BlockUpdate` and `SectionBlocksUpdate`, capped at 20,000 and off past that. At the start of a replay, write the old states into the client level only (`ClientLevel.setBlock`-style, which redraws just those sections), and step forward with the reel.
The local player's physics reads the same blocks, so don't touch anything within 3 blocks of them.
At the end, restore from a map of the true state, which the live block packets keep up to date during the replay. Block entities are left alone at first.

### Hero animators, effects and sounds under replay
- **Animators:** `HeroActionPayload` lands in `HeroAnimator.trigger` with live time, and `HeroAnimator.of(entity)` gives a ghost its
  own by id, after the swap. Pose comes from movement and aim like any remote player. (**read**, M: not run.)
- **Hero and mode:** `HeroModelRenderer.extract` reads `getAttached(HeroManager.HERO)` and the main hand's item: both in the keyframe and the stream, if the attachment sync lands on ghosts. If Fabric's handler resolves entities differently, set them directly. Spike.
- **Effects:** `EffectPayload` runs through `KirikoFx.play` etc., and `Fx` renders as usual. Its `enemy` field is relative to the hero recorded, so viewers see the hero's colours. Fine in a dragon fight, where everyone's on one side. (Overwatch had bugs on this, too: W.)
- **Sounds:** recorded ones through the same handlers. Own first-person sounds come in `OwnSoundPayload`, and `ClientHero.playOwn` needs to play them as the view's.
- **Voice:** `VoiceLinePayload` plays; live voice is dropped.
- **Run-in:** from the keyframe to the window's start, up to 5 s, apply packets with sound and particles off, then start. So what began just before the window looks right. A few things will show oddly for the first half second (an action begun 0.1 s before a keyframe); ours to judge in play.
- **What's only on the client:** the viewer's own predictions (a hit marker) don't exist. Hit markers reach the shooter as payloads (**read**: `Effects`), so they're in the stream. The reticle would be drawn from `HeroSyncPayload`. Whether the POTG draws a HUD at all: W mentions a reticle in one patch bug. Decide from the clip.

### Getting the reel to viewers
A viewer other than the hero receives the reel from the server at the POTG's start: one reel, compressed, in parts (`ClientboundCustomPayloadPacket`: 1 MiB limit, **read**). About 300 KB of parts in the 10 s before it starts. On a slow link the POTG starts for those who have it, and the others get the card (ours). In single player, it's the same path with no network.

### What reading settled, and what needs a spike
**Settled by reading (H):** every choke point above exists with that signature; `entitiesForRendering` is the only entity list `LevelExtractor` reads; keyframes can come from `sendPairingData`; pausing stops the things we'd need; the floating kick; the 1 MiB limit; the serializer runs in single player too.

**Needs a spike, in rough order of danger:**
1. **Do vanilla handlers work under the flag, without side effects?** The whole design rests on it. Look for leaks into the real player, and crashes when a ghost is touched by something assuming storage (`EntityTickList`, `TransientEntitySectionManager`).
2. **Do ghosts tick and draw right off storage?** The dragon especially: `ClientLevel.dragonParts` is filled from the entity callbacks (**read**), which ghosts skip. That list only matters for hits, but its renderer's latency ring needs ticks.
3. **Do the hero animators and `Fx` work with the swap?** First-person arms are a known no until phase 2.
4. **What does it cost?** Milliseconds per tick for the tap (target under 0.5 ms, ours), MB in the ring, the frame hitch at the camera cut (`SectionOcclusionGraph` on a big jump), against the estimates above.
5. **Decoding on the client:** getting a bound `ProtocolInfo` with the registries, and Fabric payload decode from it. Not found ready-made in what was read.
6. **Does 20 Hz look good enough in first person?** Only play can say.
7. **Do live packets touch what they shouldn't?** Say a live `RemoveEntities` while the flag is off is fine; a Fabric receiver that reads a static map keyed by id, not so sure.

### The plan
Each phase gets hand play by the rules in [CLAUDE.md](../../CLAUDE.md), a critique agent, and an Overwatch side by side where it applies ([visual_review](../testing/visual_review.md)).

1. **Spike, the smallest playable step:** single player, hero mode, `/overcraft replay`, dev-only. Tap, ring, keyframes, ghosts, chase camera, sound; skip key. No card or scoring, no block rewind. Done when: zombies and a skeleton fought for 30 s replay recognisably, the numbers in spike 4 are written down, and the game afterwards is exactly as before (health, position, mobs, hero, G). If spike 1 or 2 fails, stop and rethink.
2. **Dragon POTG, one player:** `PlayScore`, kept plays, the dragon's timeline, card (`0DC5`, `0DC6`, drawn with `HudText`), announcer line, `5B1F` (#45's extraction), skip and option. Still chase camera. A dragon kill now feels like an ending, and the owner can judge it here.
3. **First person:** the view subject in `submitArms` and friends, the reticle. The pull-back by the clip.
4. **Highlight intros:** per hero from the data, via the emote pipeline ([emote_reproduction](../emotes/emote_reproduction.md)): clip, 08F props and sounds, `2F6C`, contact checks. `extract-unlocks "<Hero>|highlightintro=*"`; assets stay in the local pack.
5. **Second player:** viewer protection, reel sending, the two-client pass (under one lock holder, as the owner approved for that pass). Then a dedicated server.
6. **Block rewind**, raid trigger, a kill cam if wanted, slow motion if the clips show any.

## 5. Risks
**Performance on the owner's machine** (a hard crash on 2026-11-04 under heavy load):
- Armed only in a dragon fight, so normal play pays nothing. Bytes bounded per hero: the rolling ring is fixed, the oldest goes first, with a hard cap of 2 MB past which that hero's window drops out of the running; kept plays stop at 4 (ours).
- No chunk work, no second level, no mesh rebuild, apart from section rebuilds from block rewind (phase 6, capped).
- During a replay, the client draws fewer entities than normal, but runs the same ones invisibly. It still does both ticks. Spike 4 measures.
- Testing stays one client at a time under the lock, as ever. Don't test this while something else is running.

**A second player:**
- Viewer safety. `canBeSeenAsEnemy` (**read**: `!isInvulnerable` for players) and damage, gated by a runtime state, not the saved `invulnerable` flag: a crash mid-POTG mustn't leave anyone immortal. `Combat.phase` handles damage and pushes; mobs' targeting needs a `canBeSeenAsEnemy` mixin. Lava and drowning count too, and the void still kills (`BYPASSES_INVULNERABILITY`). End protection 1 s after the POTG (ours).
- Someone who has left: their ghost needs `getPlayerInfo` (**read**: null means no ghost). Keep the profile in the reel's header. A hero who left before the end has lost the POTG, as Overwatch has it (W).
- Someone who joins during it: they just aren't in the POTG.
- Viewer far off or in another dimension: card only.
- A second player's actions aren't in the first one's stream if they're out of tracking range; they're not in the shot either.

**Dedicated servers:**
- The tap and scorer are common code; decoding and ghosts are client only. Bound to the mod's server side being there, as all of this is.
- `allow-flight=false` with a viewer in mid-air: not an issue since the body keeps ticking. Worth seeing once.
- The server pays for N heroes' encoding, armed only: if a raid trigger comes, count it again.

**Hero switches, G and death:**
- **G or hero switch in a window:** `Blow` fixes the hero at the time of each hit, so scores stay right. A window that spans a switch shows it, as it happened, and the card names the hero who scored most in it. A window out of hero mode can't be first person; the chase camera holds it (ours).
- **The hero dies in a window:** the ghost dies as it did, with the death animation (`LAST_HERO` handles it, after the swap). Respawn in the stream appears as remove and add. Then the camera has nothing: pull back and hold (needs the clip).
- **The viewer is dead when it starts:** a common end to a dragon fight. Respawning in the End's case means the overworld, a new `ClientLevel`, and the chunks gone. So it plays over a hidden death screen and respawn waits. If they respawn anyway, abort.
- **Any level change during a replay** (falling in the portal: the live world runs): `updateLevelInEngines` clears the camera, so a mixin there ends the replay cleanly, restores maps, clears ghosts. Same for disconnect (its handler is the list to join).
- **Live `setCameraEntity`** at respawn (`handleRespawn`, **read**) must not fight the replay's camera: set it again each tick while replaying.
- **Bad reel:** a decode error or exception drops the replay, not the game: catch per tick, log, restore. A broken POTG must never crash a world.

**Overwatch's rules:** nothing here touches the client. Intro clips, `2F6C` and `5B1F` come through the usual pipeline into the local pack, never into the repo, and are read offline with Overwatch closed. `checkNoOverwatchAssets` stands.

## Classes read for this note
Minecraft 26.3, decompiled: `Minecraft`, `Camera`, `client.multiplayer.ClientLevel`, `ClientPacketListener`, `ClientChunkCache`,
`client.server.IntegratedServer`, `client.renderer.extract.LevelExtractor`, `network.Connection`, `ProtocolInfo`,
`protocol.PacketUtils`, `protocol.game.GameProtocols`, `ClientboundLevelChunkPacketData`, `protocol.common.ClientboundCustomPayloadPacket`,
`server.MinecraftServer`, `server.level.ServerEntity`, `ChunkMap` (ranges), `server.network.ServerGamePacketListenerImpl`,
`ServerCommonPacketListenerImpl`, `PlayerChunkSender`, `world.TickRateManager`, `ServerTickRateManager`, `world.entity.LivingEntity`,
`player.Player`, `Entity`, `boss.enderdragon.EnderDragon`.

Overcraft: `Combat`, `Blow`, `KillFeed`, `Voice`, `HeroState`, `Effects`, `Comms`, `Overcraft`, `OvercraftClient`, `VictoryHud`,
`HeroAnimator`, `HeroModelRenderer`, `Fx`, `LevelExtractorMixin`, `CameraMixin`, net payloads.

Sources:
[the patent](https://patents.google.com/patent/US20180161675A1/en),
[Game Developer on it](https://gamedeveloper.com/programming/blizzard-applied-for-a-patent-on-i-overwatch-i-s-play-of-the-game-highlight-system),
[KitGuru on the GameSpot interview](https://www.kitguru.net/gaming/matthew-wilson/overwatch-dev-explains-how-the-play-of-the-game-system-works),
[Blizzard's patch notes](https://overwatch.blizzard.com/en-us/news/patch-notes/),
[Replay FPS on Replay Mod](https://modrinth.com/mod/replay-fps); the wiki's Play of the Game, Highlight intros, Cosmetics,
quote and patch pages through its API. Data read: `E:\Overwatch Archive\research\` (strings, announcer, music-round9,
kiriko-anim, kiriko-voice), and Kiriko's six intro effects with TankLib, offline with Overwatch and Battle.net closed.

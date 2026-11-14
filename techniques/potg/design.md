# Play of the Game with replay: the design

A design, built in steps (2026-11-11: [step 1, measured](#step-1-measured-2026-11-11); step 4, [the replay
engine](#step-4-the-replay-engine-2026-11-11), vendored from ServerReplay). The owner's answers of 2026-11-11 are [under the questions](#questions-only-the-owner-can-answer) and
override the text below where they differ. Up: [Techniques](../README.md) · issues: #44 (the outro), #45
(the music) · builds on: [minecraft_26](../minecraft/minecraft_26.md), [kill_feed](../hud/kill_feed.md),
[rendering](../rendering/rendering.md), [emote_reproduction](../emotes/emote_reproduction.md),
[sounds](../sound/sounds.md), [music-round9](../../tools/reference/music-round9.md). The real thing, measured from
the owner's clip: [potg_reference](potg_reference.md). Written 2026-11-06, on the owner's
"try to design a POTG system with replay", and revised the same day after [a critique](#the-critique-and-what-it-changed).

## The recommendation
**When:** after the Ender Dragon's death, as #44 asks. VICTORY as now, the dragon's own death, then the POTG just before
its last tick, so the exit portal opens after the POTG and nobody falls in during it. It's the one moment Minecraft has
that really ends something.

**How the replay works:** record what the server sends each hero, and redraw it with stand-in entities ("ghosts") in the
world that's already loaded:
- The server keeps a rolling record per hero of the packets that hero's client is sent, plus what the server never
  sends a player about themselves: position, look, equipment, hero state, death. Chunks aren't recorded.
- Every 5 s it writes a keyframe, from the calls Minecraft and the mod already make for someone who has just come to see
  something.
- Scoring runs beside it, and each hero's best plays are kept when they scroll out of the buffer.
- To play it, the client runs the recorded packets through Minecraft's own handlers. While a replay runs, the client's
  entity lookups answer with the ghosts, and only the code that keeps the real game alive still sees the real entities.
- The viewer's real body stays the camera's entity, so it goes on sending its moves; the view is placed elsewhere, as our
  emote and ability cameras do.

**The world holds still:** for the 17 s the server freezes everything but players, with the mechanism behind Minecraft's
`/tick freeze`, while the viewers' clients are kept from hearing about it, so their animations and effects run on. This
removes most of what a running world would need (question 3).

**Why not the way replay mods do it** (a second world): swapping `Minecraft.level` rebuilds every chunk mesh on the way
in and out, needs a chunk snapshot in the buffer, and leaves the live world with nobody to apply its packets. That's
the heavy road on a machine that hard-crashed under load on 2026-11-04. See [the options](#the-options).

**Cost, measured for the tap and ring (step 1):** 5.5 to 12.4 KB/s, a rolling ring of 185 to 360 KB per hero, the tap
0.05 to 0.13 ms a tick on average; about half the estimate below. Kept plays aren't built yet. Nothing is recorded
outside the fight in the first version.

**Smallest first step:** no ghosts at all. A tap, a ring, and a decode round trip that only logs bytes and packet types,
in single player. That settles cost and decoding first. Then ghosts, then the camera. See [the plan](#the-plan).

**How sure:** the approach survived a critique, but not as first written: twelve corrections, all found by reading, are
listed [at the end](#the-critique-and-what-it-changed). Step 1 has run: everything recorded decodes, and the tap had to
move (Fabric sends round the listener's `send`). Ghosts and the camera haven't.

## Questions only the owner can answer
**The owner's answers (2026-11-11), which decide these:**
- **Only the dragon kill triggers it.** No raid trigger (question 1).
- **The world pauses during it:** the still world (question 3), so only that one is built.
- **Nobody can skip it:** no skip key and no off switch (question 4). The note's skip key, skip option and "skipping
  alone" are dropped everywhere below.
- **The rest of the defaults stand:** the replay key stays a dev command (2), Minecraft-mode kills don't count (5), a
  highlight intro picked for the first version (7), it ends on the replay and then the exit portal opens (8).
- **Clips (6):** the owner will send real POTG clips for the camera, into `local-assets/raw/`. The camera comes last,
  so nothing waits on them.

The questions as first asked:
1. **Is the dragon the right first moment?** And is a raid's victory the second? (Recommended: yes to both.)
2. **Is a key for "replay my last 12 s" wanted in normal play**, or only as a dev command? It would mean recording all
   the time in hero mode, not only in fights. (Recommended: dev command first, decide after playing it.)
3. **What should the world do during the 17 s?**
   - **It holds still (recommended).** Everything but players stops, everywhere on the server: mobs, arrows, the dragon,
     furnaces, crops. When the POTG ends, it all carries on exactly where it was. Nothing can sneak up on you, and
     nothing has changed when you come back. It's the simplest to build. The cost: a friend on your LAN who's off mining
     stands in a stopped world for 17 s, and on a public server it would be rude, so there it would be switched off.
   - **It runs on, hidden.** Mobs keep moving and raids keep going while you watch, and viewers are made untouchable and
     ignored. You come back to a changed scene, and in a raid that may matter. It costs more to build and test, and we'd
     only want it for servers with people who aren't in the fight.
   - **What I need from you:** is 17 s of stopped world acceptable for someone not in the fight? And do you mean to play
     this on a server with strangers? If yes to the first and no to the second, only the first way gets built.
4. **Everyone in the End watches, skippable, with an off switch in the options** (as #44 says). Can a viewer skip alone?
   (Recommended: yes, and then they stand in the still world until the others finish.)
5. **Does a kill in Minecraft mode count?** Recommended no: the POTG is a hero's. If no hero scored anything, there's no POTG.
6. **Can you send a clip or two of a real POTG**, for the side by side? The sources don't describe the camera well enough
   ([what wasn't found](#what-wasnt-found)), and #44's "pulled back" needs a reference. What the camera does when the
   hero dies mid-play is the main unknown.
7. **Which highlight intro for each hero?** Kiriko has 18. Her default "Heroic" gets its clip from a UI dataflow not yet
   traced, so the first version may need one you pick from the named ones. Later: a choice per player, like emotes?
8. **What plays after the POTG?** #44 asks for "the rest of the outro". No music was found for the screens after it
   ([music-round9](../../tools/reference/music-round9.md)), and those screens need a scoreboard Minecraft doesn't have.
   Is ending on the POTG, then the portal opening and back to the game, enough?

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
| **Dragon kill** | The owner asked for it (#44, #45); a real end; VICTORY and music are there or coming; everyone's together | Only once or a few times a world | **First** |
| **Raid won** | A real end, a real fight, lots of kills. `Raid.isVictory` | Happens more often, so a stopped world is felt more | Second |
| **A key, on demand** | Tests everything; Overwatch has Highlights too | Not an event; needs recording always; stopping the world on a key is odd | Dev command first (question 2) |
| **A death** | Overwatch's kill cam, on the same machinery | From the killer's view it needs the killer's record: a mob has none. Happens often | Later, perhaps |
| **A night survived** | Survival's natural beat | Nothing climactic; often no fight at all; players are asleep | No |
| **Wither or Warden kill** | Same as the dragon | Needs its own victory first | Later |

**The dragon's timeline** (read in `EnderDragon.tickDeath`, `EnderDragonFight.setDragonKilled`, `EndPortalBlock`,
`Overcraft`, `VictoryHud`; the sequence is ours):
- 0 s: the dragon's `AFTER_DEATH`. VICTORY shows for 5.65 s, with its music. Scoring stops.
- 0 to 9.95 s: Minecraft's death, seen live: rising, XP from tick 150, explosions from tick 180.
- 9.95 s, `dragonDeathTime` 199: the world freezes, one tick before the dragon is removed. Victory's 33 s music fades.
- 10 to 27 s: the announcer's line, the intro (5 s), the replay (12 s), cue `5B1F`.
- 27 s: the world thaws. On the next tick the dragon goes and `setDragonKilled` places the active portal and the egg,
  with the viewer watching.

**Why the hold at tick 199.** `setDragonKilled` runs at tick 200 and places portal blocks in the fountain. A melee hero who
killed it from the fountain could then be standing in them. In `EndPortalBlock.entityInside`, a player who hasn't seen the
credits gets `showEndCredits` (a `WinScreen` over everything), the others `setAsInsidePortal` and a trip home. Either
would end the POTG. Holding the last tick means the portal opens only when people can see it. The same hold works with a
running world: a mixin that doesn't let `tickDeath` pass 199 until the POTG ends. Walking into the portal afterwards is
Minecraft's own ending, left as it is.

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
need for `genSources` here), or for Fabric's attachments in the class files of its jar. Nothing was run.

### The options
| | A. Second world, as replay mods do | B. Own state, redrawn by hand | **C. Recorded packets, ghosts in the loaded world** |
|---|---|---|---|
| Captured | All packets, chunks included | Our own format per entity type | The packets that matter, keyframes, the hero's own track |
| Shown in | A new `ClientLevel`, `Minecraft.setLevel` | The live world | The live world |
| New code | A fake connection, a second listener | An encoder and decoder for every entity's state: dragon phases, crystal beams, equipment, poses, every mob | A tap, a keyframer, one entity-view switch, an audit of the client's static state |
| Fidelity | Best | Whatever we remembered to record | Close: what a client saw, through vanilla's handlers |
| Load | Chunk snapshots; meshes rebuilt twice | Low | Low |
| Live world | Its packets have nowhere to go | Fine | Fine |
| Blocks | Right by construction | Rewind needed | Rewind needed |

**A, read:** `Minecraft.setLevel` goes to `updateLevelInEngines`, which stops all sounds, clears the camera and hands the
new level to the extractor, the particle engine and the renderer. A rolling buffer also needs a full chunk snapshot at
its start, every time it rolls. `ClientboundLevelChunkPacketData` copies each chunk's sections to build its buffer, on
the order of 12 MB a snapshot at view distance 12 (625 chunks, 20 KB each, ours). `ClientPacketListener.level` and
`Minecraft.player` are single fields, so a second world and the live one can't both be served without a second
listener. As far as I know replay mods get away with this because they leave the game to replay, but that's from memory:
Replay FPS's page confirms only that Replay Mod captures server-to-client packets, and neither Flashback's nor Replay
Mod's code was read. We can't leave.

**B, read:** nothing stops it, and the render path would be the same as C's. It loses because of what a mob looks like:
`SynchedEntityData`, equipment, passengers, attributes, the dragon's phase. Vanilla already says all that in packets.

**The patent's third way, a rendered video,** isn't practical here. Also rejected: a real recording connection on the
server, which is A's cost on the server's side.

### What gets recorded
A tap on `Connection.send(packet, listener, flush)` for each hero's connection (**run**, step 1). The first draft had
`ServerCommonPacketListenerImpl.send`, and that misses Fabric: its `PacketSender.sendPacket`
(`AbstractChanneledNetworkAddon`) goes straight to the `Connection`, so attachment bundles went round the tap, and a
keyframe's captured attachments reached the client every 5 s instead of the ring. Each kept packet is encoded on the spot with the game
protocol's codec (`GameProtocols.CLIENTBOUND_TEMPLATE`, bound with the registries) into a byte ring, with its tick.
Encoding at once means no mutable `ItemStack` is held.

**The tap must unbundle.** `ServerEntity.addPairing` sends new entities as one `ClientboundBundlePacket`; the protocol
registers only the delimiter (`ProtocolInfoBuilder.withBundlePacket`), bundles are split later in netty
(`PacketBundleUnpacker`), and `IdDispatchCodec.encode` throws on an unregistered type (**read**). So walk the bundle's
sub-packets and record each.

- **Entities:** `AddEntity`, `RemoveEntities`, `MoveEntity.*`, `EntityPositionSync`, `TeleportEntity`, `RotateHead`,
  `SetEntityMotion`, `SetEntityData`, `SetEquipment`, `SetPassengers`, `SetEntityLink`, `UpdateAttributes`, `Animate`,
  `SwingAnimation` (`LivingEntity.swing`), `EntityEvent`, `DamageEvent`, `HurtAnimation`, `UpdateMobEffect`,
  `RemoveMobEffect`, `MoveMinecart`, `ProjectilePower`, `TakeItemEntity`.
- **World, recorded but not applied at first:** `BlockUpdate`, `SectionBlocksUpdate`, `BlockEvent`, `BlockDestruction`,
  `BlockEntityData`. Their handlers write the live client level (`setServerVerifiedBlockState`, **read**): stale blocks
  the real body collides with, cracks left behind. So there's an allow-list per phase, and until
  [block rewind](#blocks-changed-since) these stay out.
- **World, applied:** `LevelEvent`, `LevelParticles`, `Explode` (with its knockback stripped), `Sound`, `SoundEntity`.
- **Ours:** `EffectPayload`, `HeroActionPayload`, `OwnSoundPayload`, `VoiceLinePayload`, the attachment sync.
  `HeroSyncPayload` is recorded but **never goes through its receiver**: it comes every tick (`HeroManager`), the receiver
  sets `ClientHero.sync`, and 85 calls of `ClientHero.hero()` and `sync()` read it, so a viewer of another hero would
  change identity tick by tick. It goes to a separate slot, for the reticle in the first-person phase.
- **Not:** chunks, chat, stats, inventory, abilities, `PlayerPosition`, `SetHealth`, boss bars, `Respawn`, `GameEvent`,
  `ResurrectPayload`, `GripPayload`. Time and weather go in the reel's header.

**The hero's own track.** The server tells a player almost nothing about their own entity (**read**):
`TrackedEntity.updatePlayer` skips `player == this.entity`, so no `AddEntity` or moves;
`LivingEntity.updatePlayersWithNewEquipment` uses `sendToTrackingPlayers`, not self; the inventory isn't recorded;
attachments come only on change or start of tracking. `HeroModelRenderer.extract` needs a weapon or `HERO` and otherwise
returns early, so without this the hero's own ghost would draw plain and empty-handed. So each tick, for the hero:
position, yaw, pitch, pose, on-ground, equipment when it changes, attachments when they change, alive or dead, and a
respawn (the hero's own stream has none of the "remove and add" that other people's deaths show). About 32 bytes on most
ticks. `Combat.animate` does send the hero's own clips to them, and `OwnSoundPayload` their own sounds (**read**).

**Keyframes every 5 s**, or a rolling stream has no start: an entity already there never had an `AddEntity`. Each keyframe
holds, for every entity the hero tracks **and the hero's own `ServerEntity`**:
- `ServerEntity.sendPairingData(player, consumer)` (**read**): add, data, attributes, equipment, passengers, leash.
- Attachments, by our own list. Fabric sends them from `EntityTrackingEvents.START_TRACKING` (`AttachmentSync`, fired by a
  mixin on `addPairing`: **read in the class files**), not from `sendPairingData`, so a keyframe would otherwise lack
  `HERO`, `HERO_MODE`, `SONG`, `MERCY`, `LIFEWEAVER`, `SIGMA_STATE` and the others.
- `HeroManager.showState` in capture-only mode, not just `Effects.showHeld`: it also calls `Comms.showEmote` and
  `kit.showState` (**read**), which is what the mod itself does at `START_TRACKING`.
- The hero's own mob effects: not in pairing (**read**).

Keyframes sit beside the stream, not in it. Playback uses only the first one in a reel; applying a later one's
`AddEntity` would recreate every ghost every 5 s.

**Rolling buffer:** segments of 5 s, each with its keyframe; the last 25 s kept, so any 12 s window has a keyframe before
it, and a run-in. **Kept plays:** when a window sets a new best, its segments are pinned, one per category per hero, at
most 4.

**Memory: the estimate from the packets' fields (ours, M-L), kept for comparison; step 1's measurements are
[below](#step-1-measured-2026-11-11) and came in at about half.**
| | bytes/s |
|---|---|
| 30 mobs moving, about 7 updates/s, 12 bytes each | 2,500 |
| 10 projectiles, 20/s, 12 bytes | 2,400 |
| Entity data, 30/s, 30 bytes | 900 |
| Sounds, 20/s, 40 bytes | 800 |
| Effects and hero actions, 30/s, 80 bytes | 2,400 |
| Particles, 20/s, 50 bytes | 1,000 |
| `HeroSync`, 20/s, about 100 bytes (a string, 20-odd numbers, widgets) | 2,000 |
| Own track and players | 1,500 |
| **A heavy fight** | **about 15 to 25 KB/s; a raid perhaps 40** |

Per hero: 25 s is 400 KB to 1 MB; keyframes about 100 to 400 KB (5 of them, 40 entities at 250 bytes, but a player with
attributes, equipment and attachments is perhaps 1 to 2 KB). So 0.5 to 1.5 MB rolling. 4 kept plays add at most about
3.5 MB: about 5 MB in all per hero, about 10 MB for two heroes. It could still be off by 2 or 3 times either way. A's one
chunk snapshot is about 12 MB.

**Only while armed:** the first version records only heroes in the End with a dragon alive. Elsewhere it costs nothing.

### The world during a replay: still, or running
**Does single player pause? Not with this design, and pausing wouldn't help.** `IntegratedServer.tickServer` (**read**)
pauses when `Minecraft.isPaused()`, which is `gui.isPausing()` while not published. But on pause `Minecraft` (**read**)
stops the level's and particles' ticks and calls `soundManager.pauseAllExcept(MUSIC, UI)`. Our hero animators take `now`
from `level.getGameTime()`, and `Fx` runs on the game's clock. A published LAN world can't pause at all.

**Freezing, with the news kept from the viewer** (the critic's idea; **read**): `ServerTickRateManager.setFrozen` stops all
non-players (`TickRateManager.isEntityFrozen`, `ServerLevel`'s entity ticks), block and fluid ticks, chunk ticks, the
dragon fight (`ServerLevel`: `runs`) and time. The client learns of it from `ClientboundTickingStatePacket`. **And, which
the critic missed, it's reached by time packets:** `MinecraftServer` sends `ClientboundSetTimePacket` every 20 ticks,
frozen or not, and `handleSetTime` sets the client's game time, so a viewer's clock would be yanked back every second.
Both packets are withheld from viewers during a POTG, in the same `send` hook as the tap. Then the client's level runs
normally: clock, particles, animators, `Fx`.

| | **Still world (recommended)** | Running world |
|---|---|---|
| Mobs attacking viewers | Can't | Needs a damage gate and a `canBeSeenAsEnemy` mixin |
| Viewer's own hazards (lava, drowning, a landing) | Players still tick, so a small damage gate is still needed: `Combat.phase(player, 1)` at the start. Its time is game time, which is stopped, so a second is left at the thaw | Same gate, for 17 s |
| "Things have moved" | No | Yes, 17 s worth |
| Live traffic while replaying | Almost none, so fewer collisions and less load | Normal |
| Portal timing | Held by the freeze | Needs the `tickDeath` hold |
| Reach | **World-wide**: someone elsewhere has their furnaces and mobs stopped for 17 s. Bad on a public server: there, off | Viewers only |
| Mod's ticks | `END_SERVER_TICK` still runs: `HeroManager.tick` counts down `ultActive`, adds passive charge, runs kits and sends `HeroSync`. Needs one gate for viewers, and a check of the kits | Unchanged, or the same gate |
| Clock | The client's clock runs on up to 340 ticks, and snaps back at the thaw. Live stamps from before fit again; the replay's are thrown away. Anything live stamped during the POTG looks to be in the future: audit (56 `getGameTime` uses in the client) | None |
| Timers on our side | The POTG's own timer must count server ticks, not game time. A watchdog thaws after 22 s, and on the last viewer's leaving | Same, less critical |
| After a crash | Nothing saved: no save code reads `tickRateManager`, so a crash can't leave a world frozen | Must not use the saved `invulnerable` flag |
| A `/tick freeze` already on | Remember it and put it back | Nothing |
| Moves while frozen | Accepted, with the too-fast check skipped (`handleMovePlayer` under `runsNormally`) | Normal |
| Skipping alone, or opting out | They get control in a still world. Opt-outs and players elsewhere get the real frozen packet, as with `/tick freeze`; a joiner too (`updateJoiningPlayer`) | They go on playing |

**Verdict:** still, for one code path and much less to go wrong, as long as question 3 allows it. The running world is
built only if the owner wants servers with people outside the fight.

### How the world is shown
**Ghosts** are real entities built by vanilla's own `createEntityFromPacket` (a `RemotePlayer` for a player: **read**) with
the live `ClientLevel` as their level, kept in a map of ours, not in its entity storage. They read blocks and light from
the live level as they should.

**One switch, inverted from the first draft.** While a replay runs, the *default* on the client thread is the ghost world;
"live" is a narrow scope around:
- `packetProcessor.processQueuedPackets()` and `level.tickEntities()` (`Minecraft`, **read**), where the real game keeps going;
- the handlers that move or act for the viewer's real hero: `ClientHero.tick` and `endTick`, `ClientTow.tick` (which sets
  the body's velocity from `getEntity(catId)`: **read**), `ClientJuno`, `ClientSojourn`, `ClientSigma`. An audit, not a
  closed list yet.

The first draft had it the other way, with a flag around packet handling, and that left everything else reading live
entities: `Fx::render`, `MercyFx.frame`, `LifeweaverFx.frame`, and the `END_CLIENT_TICK` handlers (`MercyFx.tick`,
`AnaFx.tick`, `LucioFx.shields`, `JetpackCatFx.tethers`) all ask the level for entities: 71 `getEntity` calls in the
client, and 28 uses of `players()` or `entitiesForRendering()`.

**The choke point is smaller than the first draft thought.** `ClientLevel.getEntities()` (**read**: line 1056) is what
`getEntity`, `entitiesForRendering` and, through `Level`, the box and class searches and `getPushableEntities` all go
through. A `LevelEntityGetter` of ghosts returned from a mixin there, outside the live scope, covers them all. So:
- `LevelExtractor.extractVisibleEntities` draws only the ghosts, and the sound and effect handlers see them.
- **Ghosts don't push the viewer.** `LivingEntity.pushEntities` and `RemotePlayer.aiStep` find targets through
  `getPushableEntities`, and `EntitySelector.pushableBy` matches the local player on a client (**read**), but in the ghost
  view the local player isn't there.
- `EnderDragon.checkCrystals` finds the ghost crystals, not today's.

`players()` is a field of its own, and `isTickingEntity`, `addEntity` and `removeEntity` need the same switch.
`ClientLevel.tickPassenger` reads `tickingEntities` directly, so ghosts are ticked by our own loop, riders included.
`dragonParts` is filled from entity callbacks ghosts skip; it matters for hits, not for drawing.

**Playing a tick:** decode the tick's packets, and call `packet.handle(listener)` on the real `ClientPacketListener`. Its
handlers look up through `level.getEntity`, so they land on ghosts unchanged, as do our Fabric receivers. Then tick each
ghost, for limbs, interpolation, the dragon's parts and latency ring. Then the hero's ghost gets its track.

**Handlers that reach past the level** need auditing, against `minecraft.player` mostly. Seen so far: `handleExplosion`
pushes the real player with `playerKnockback` (**read**), so strip that. `handleEntityEvent` only shows a totem when the
entity is the real player, which a ghost isn't (**read**). `handleSetEntityPassengersPacket` is one to read.

**Statics.** The first draft said effects' statics were all listed in the DISCONNECT handler. They aren't. Counting
`static` maps and sets keyed by `Integer` or `UUID` in the client: 69 in 23 classes (the critic's count: 76 in 24). 7 of
those classes have no `clear()` at all (`JetpackCatFx`, `MercyFx`, `MercySounds`, `SigmaSounds`, `JunoSounds`,
`SojournSounds`, `ClientJuno`). So it takes an explicit list, kept in one place: for each, swap for a fresh one at the
start and back at the end. Ghosts have the same ids as the live entities, so nothing else separates them. Held effects
are sent again with `HeroManager.showState` at the end, like a late arrival.

**Live mod payloads** are dropped during a replay, or they'd land in the swapped maps under a ghost's id: `EffectPayload`,
`HeroActionPayload`, `OwnSoundPayload`, `VoiceLinePayload`, `PingPayload`, `KillFeedPayload`. `HeroSyncPayload` and
`ResurrectPayload` go on as live.

**The real player in the render path.** `LevelExtractor.extractPlayerState` extracts `mc.player` every frame, whatever is
on screen, which poses the same `HeroAnimator` as the hero's ghost when the viewer is that hero (**read**). So
`HeroModelRenderer.extract` returns early for `mc.player` during a replay. Also from `mc.player`, and to be gated:
`renderItemInHand` and arms (`FirstPersonHandsMixin`, `submitArms`), screen overlays (fire, water, in a wall), nausea and
portal effects, FOV, block outline.

**Sound** is filtered in `SoundEngine`, not `SoundManager.play`: `playDelayed` and `queueTickingSound` go around that
(**read**). The rule follows the switch: what's started inside the live scope is dropped, what's started outside plays.
So `MercySounds`, `LucioMusic`, `SigmaSounds` and the rest, which start loops from the client tick reading `players()`,
see the ghosts and play the ghosts' loops. Sounds already going at the start are stopped, music and UI excepted, and
their classes' maps swapped. `Camera` drives the listener, so positions come out right. Vanilla particles and `Fx` follow
the same rule.

**HUD and input:** `HudMixin` hides ours and vanilla's, drawing the POTG's instead. Input is blanked, through
`KeyboardInputMixin` and `KeyMappingMixin`; G and the hero menu too. No key skips it (owner, 2026-11-11).

### Camera
**Not `setCameraEntity(ghost)`.** The first draft had it, and claimed the body would go on sending its moves. The opposite
is true (**read**): `LocalPlayer.sendPosition` and `applyInput` are gated by `isControlledCamera()`,
`getCameraEntity() == this`. With the camera elsewhere there'd be no moves for 17 s, so the server's copy stops, and a
viewer in mid-air on a server with `allow-flight` off is kicked (`ServerGamePacketListenerImpl.tick`, `clientIsFloating`).
Input falls to `LivingEntity.applyInput`, shrinking by 0.98 a tick with `jumping` left as it was, so a walking hero walks
on, slowing, and `OwMovement.travel` takes that input like any other.

**Instead:** the camera entity stays `mc.player`, and the view goes where `CameraMixin` already puts it for `EmoteCamera`
and `AbilityCamera`, at `alignWithEntity`'s tail: `prepareCullFrustum` and the listener then use it (**read**). That also
ends any fight with `handleRespawn`, which sets the camera entity back. To handle:
- `Camera.detached` comes from the options in `alignWithEntity` (**read**): set it to what the shot wants.
- `extractVisibleEntities` hides only `camera.entity()`, now the real player. A first-person hero's ghost must be left
  out by our view.

The shots:
- **Chase, first:** behind the hero's ghost. Simple, and it shows the body animations work.
- **First person:** `HeroModelRenderer.submitArms` hard-wires `mc.player` and `ClientHero.hero()`, and first-person code
  all over assumes the viewer is the player (186 uses of the local player in 45 client files, not all relevant). It needs
  a "view subject". 20 Hz look with interpolation may look choppy: Replay FPS exists because of that problem with Replay
  Mod. If it does, the client sends its look at a higher rate for the record.
- **Pull-back:** to the side when the hero dies or as the clip's end nears, per a real clip (question 6).
- **Chunks:** only what the viewer has loaded can be drawn, since the server loads chunks around the viewer. In the End
  that's fine. A far-off play shows holes: the server compares the play's position to the viewer's view distance, and
  sends the card alone if it's too far, or in another dimension (ours).
- Sections that aren't compiled hide their entities (`isSectionCompiledAndVisible`), so a cut to a far camera needs a
  moment; start the camera during the intro's fade.

### Blocks changed since
The play may be minutes old, and the dragon and crystals break things. The first phases don't apply block packets at all
and show today's blocks. Later, a log per world: position, old and new state, tick, capped at 20,000 and off past that. At
the start of a replay, write the old states into the client level only, redrawing just those sections, and step forward
with the reel. The real body's physics reads the same blocks, so nothing within 3 blocks of it changes. At the end,
restore from a map of the true state. Block entities and cracks stay out.

### Hero animators, effects and sounds under replay
- **Animators:** `HeroActionPayload` lands in `HeroAnimator.trigger` with the running clock, in the swapped map. Pose comes
  from movement and aim like any remote player. (**read**, M: not run.)
- **Hero and mode:** `HeroModelRenderer.extract` reads `getAttached(HeroManager.HERO)` and the main hand's item: both come
  from the keyframes and the own track. Whether a client-side `setAttached` on an entity outside storage is allowed, and
  whether Fabric's receiver finds the ghost: spike.
- **Effects:** `EffectPayload` runs through `KirikoFx.play` etc., and `Fx` renders as usual. Its `enemy` field is relative
  to the hero recorded, so viewers see the hero's colours. Fine in a dragon fight, where everyone's on one side. (Overwatch
  had bugs on this, too: W.)
- **Sounds:** recorded ones through the same handlers. Own first-person sounds come in `OwnSoundPayload`, and
  `ClientHero.playOwn` needs to play them as the view's.
- **Run-in:** from the keyframe to the window's start, up to 5 s, apply packets with sound and particles off, then start. A
  few things will show oddly for the first half second (an action begun 0.1 s before a keyframe); ours to judge in play.
- **What's only on the client:** the viewer's own predictions don't exist. Hit markers reach the shooter as payloads
  (**read**: `Effects`), so they're in the stream. The reticle would be drawn from the recorded `HeroSync`. Whether the
  POTG draws a HUD at all: W mentions a reticle in one patch bug. Decide from the clip.

### Getting the reel to viewers
A viewer other than the hero receives the reel from the server: compressed, in parts (`ClientboundCustomPayloadPacket`:
1 MiB limit, **read**), in the 10 s of the dragon's death. About 500 KB. On a slow link those without it get the card
(ours). In single player, the same path with no network.

### What reading settled, and what needs a spike
**Settled by reading (H):**
- `getEntities()` is what entity lookups, rendering and pushes go through on the client.
- `LocalPlayer` sends moves only while it's the camera's entity; so the camera entity stays `mc.player`.
- A hero's own entity is nearly absent from their stream; bundles must be unbundled; block packets write the live level.
- What a keyframe needs: `sendPairingData`, attachments, `HeroManager.showState`, the hero's own effects.
- Freeze: what it stops, that players go on, that two packets must be withheld, that nothing is saved.
- Pausing stops what we'd need. The 1 MiB limit. The serializer runs in single player too, and
  `ensureRunningOnSameThread` is fine inline on the render thread.
- The portal's order of events, and that holding tick 199 keeps it shut.

**Needs a spike, in rough order of danger:**
1. ~~**Encoding and decoding outside the pipeline**~~ **Settled by step 1 (run):** `GameProtocols.CLIENTBOUND_TEMPLATE.bind(
   RegistryFriendlyByteBuf.decorator(registries))` on each side encodes and decodes everything seen, Fabric payloads
   included, with no pipeline. Costs [below](#step-1-measured-2026-11-11).
2. **Do vanilla handlers work on ghosts, without side effects?** The whole design rests on it. Look for leaks into the
   real player, and crashes where something assumes storage (`EntityTickList`, `TransientEntitySectionManager`).
3. **Do ghosts tick and draw right off storage?** The dragon especially: its renderer's latency ring needs ticks.
4. **Is the ghost view really airtight?** That the scope around live code holds, and the audit of what's inside it
   (`ClientHero`, `ClientTow`, ...) is complete. A wrong guess moves the real body.
5. **Do the hero animators and `Fx` work with the swap, and does everything come back?** First-person arms are a known no
   until that phase.
6. **Freeze in practice:** the clock's snap back at the thaw, `HeroManager`'s gate, kits that assume time moves.
7. **The frame hitch at a camera cut** (`SectionOcclusionGraph` on a big jump).
8. **Does 20 Hz look good enough in first person?** Only play can say.

### The plan
Each phase gets hand play by the rules in [CLAUDE.md](../../CLAUDE.md), a critique agent, and an Overwatch side by side
where it applies ([visual_review](../testing/visual_review.md)).

1. **Tap and round trip, no ghosts, the smallest first step:** dev-only, single player. Tap, unbundling, ring, keyframes,
   then decode everything again on the client, and log only bytes and packet types. Settles costs, decoding, bundles,
   Fabric payload encoding. If this is expensive or won't decode, nothing else matters. **Done 2026-11-11:**
   [measured](#step-1-measured-2026-11-11); it's cheap and it all decodes.
2. **Ghosts, then camera, the first replay:** dev-only `/overcraft replay`. Ghost view, the hero's own track, the freeze
   (the owner's choice: the world pauses), the statics list, chase camera last, sound. No skip key (owner). No card or
   scoring, no block packets. **Done when**, each by hand with real keys:
   - zombies and a skeleton fought for 30 s replay recognisably, and so does a summoned dragon;
   - it starts while walking, while in mid-air, and while holding jump, and the body does the right thing throughout;
   - with a block placed inside the window, the world's blocks are right during and after;
   - a hero switching weapon or hero inside the window draws right;
   - client and server position are compared afterwards and agree;
   - the game afterwards is exactly as before: health, mobs, hero, G, loops, held effects;
   - the costs are written down. If handlers or ghosts fail, stop and rethink.
   **Done 2026-11-11, from the dev command:** [measured](#step-2-the-first-replay-2026-11-11); what's still open is listed there.
3. **Dragon POTG, one player:** the dragon kill is the only trigger (owner). **Built 2026-11-11:** [step 3](#step-3-the-dragons-play-of-the-game-2026-11-11); the owner then moved plays to the whole run and the replay toward an existing replay mod. `PlayScore`, kept plays, the tick 199
   timeline, the viewer's protection, card (`0DC5`, `0DC6`, drawn with `HudText`), announcer line, `5B1F` (#45's
   extraction). No skip and no off switch (owner). Still the chase camera. A dragon kill now feels like an ending, and
   the owner can judge it here.
4. **First person:** the view subject in `submitArms` and friends, the reticle. The pull-back by the owner's clips
   (`local-assets/raw/`).
5. **Highlight intros:** per hero from the data, via the emote pipeline
   ([emote_reproduction](../emotes/emote_reproduction.md)): clip, 08F props and sounds, `2F6C`, contact checks.
   `extract-unlocks "<Hero>|highlightintro=*"`; assets stay in the local pack. One picked for the first version (owner).
6. **Second player:** reel sending, the two-client pass (under one lock holder, as the owner approved for that pass).
   Then a dedicated server; the freeze is world-wide there, so it needs the owner's word again before shipping on one.
7. **Block rewind**, a kill cam if wanted, slow motion if the clips show any. No raid trigger (owner).

### Step 1, measured (2026-11-11)
Built on `feature/potg`: `PotgTap` (server: the tap, ring, keyframes, own track), `PotgSendMixin` on `Connection.send`,
`ChunkMapAccessor` and `TrackedEntityAccessor` for the keyframe, and `PotgDecode` on the client. `/overcraft potg
start|stop|dump` exists only with `overcraft.dev` (runClient). The dump logs the server's numbers and hands a copy to the
client in the same JVM, which decodes every record with its own bound protocol, checks every byte is used, encodes it
again and compares. Nothing is handled or drawn. Raw rows: [runs_step1](runs_step1/) (`*_dump*.txt`, the driver
`potg_drive.py` and its `*_driver.txt` logs).

**How it ran.** One dev client (lock held, Overwatch and Battle.net checked closed, 5.4 to 7.6 GB free throughout),
Kiriko, the harness: kunai held with `key secondary_fire`, aim set from the server (`/execute ... facing entity ... run
tp`) every 0.25 s, Suzu and the ult by `key`, G by `press key.keyboard.g` (the keyboard handler). Not the real mouse:
this step changes nothing a player sees, so it had no hand play with real keys; step 2 must. Tree: the commit after
`72bc2a4` (the tap on `Connection`); the `tap1_*` files ran on `72bc2a4` (the tap on the listener) and only show the
bypass.
- **Night:** a fresh flat survival world, midnight, normal, Resistance II. 5 zombies and 3 skeletons summoned 18 to 26 m
  off, and the night's own spawns: the last keyframe held 145 entities (49 slimes, 11 spiders, 4 creepers, 3 zombies, 2
  skeletons, 33 XP orbs, 21 items, animals). Kunai for 85 s, Suzu, Kitsune Rush, G out and back at 37 s.
- **Dragon:** the End's own dragon and 10 crystals, about 70 endermen, Resistance IV and Regeneration III, alive
  throughout, 85 s of kunai at the dragon (few hits: it flies off).
- **The death:** `/kill` on the dragon 8 s after arming, then 15 s of its death and the exit portal (`/kill` because
  `/damage` on the dragon's body does nothing: it's hurt only through its parts). So no hero is credited; the packets are
  the same.

| | Estimate (above) | Night, 64 s | Night, 93 s | Dragon, 54 s | Dragon, 85 s | Death, 24 s |
|---|---|---|---|---|---|---|
| Tap, ms/tick mean | under 0.5 (target, ours) | 0.127 | 0.113 | 0.060 | 0.058 | 0.052 |
| p50 / p99 | | 0.089 / **0.680** | 0.078 / 0.666 | 0.051 / 0.375 | 0.050 / 0.336 | 0.041 / 0.331 |
| Worst tick | | **8.02** (first keyframe) | 8.02 | 1.33 | 1.33 | 0.77 |
| Keyframe ms, mean / max | | 1.22 / 7.29 | 1.06 / 7.29 | 0.41 / 1.24 | 0.37 / 1.24 | 0.42 / 0.71 |
| Bytes/s (stream + own track) | 15,000 to 25,000 heavy | **12,378** | 11,040 | 6,412 | 6,311 | 5,460 |
| Own track, B/s | 1,500 with players | 680 | 680 | 680 | 680 | 681 |
| Keyframe, average | 20,000 to 80,000 | 13,194 B | 13,171 B | 8,599 B | 8,319 B | 7,992 B |
| Ring (5 s segments, 25 to 30 s) | 0.5 to 1.5 MB | **361 KB** | 327 KB | 275 KB | 260 KB | 189 KB |
| Server tick, average | | 4.45 ms | 4.63 ms | 2.42 ms | 2.31 ms | 2.68 ms |
| Client decode of the whole ring | | 23.5 ms | 8.7 ms | 9.1 ms | 7.4 ms | 3.4 ms |

What the numbers say:
- **About half the estimate**, so the 2 MB cap per hero has room, and no compression is needed in single player.
  `HeroSync` is the largest single item, as estimated: 112 bytes, 20 a second, 2.2 KB/s (estimate 2.0), about a fifth of
  the night's stream. Then mob movement: `move_entity_pos`, `set_entity_motion`, `entity_position_sync`, `rotate_head`.
- **Keyframes cost by entity count, 90 to 160 bytes each** (not the estimate's 250), mostly `add_entity` and
  `update_attributes`. A flat world's night (slimes, XP orbs, dropped items) made the largest. Dropping XP orbs and items
  from keyframes would cut a third of that, if it ever matters.
- **The tap's mean is a quarter of the target; its p99 in the night (0.68 ms) is over it**, and the first keyframe of a
  run took 7 to 8 ms with 145 to 160 entities (a cold start: the next ones took about 1). Server ticks stayed under 5 ms,
  so nothing showed. If it matters later, spread a keyframe over a few ticks.
- **Every packet decodes.** 0 encode errors, 0 decode errors, 0 bytes left over, in every run, for every type seen,
  kept or probed. Re-encoding gives the same bytes for every kept type; only `level_chunk_with_light` (all) and
  `update_advancements` (some) differ, both probes and never kept (their maps aren't ordered).
- **Bundles split:** `addPairing`'s bundles came apart in the tap (112 `add_entity` in one night window); an unsplit
  bundle would have thrown in the encoder, and none did.
- **Fabric payloads decode outside the pipeline**, to their own classes, none to `DiscardedPayload`: ours (`effect`,
  `hero_action`, `own_sound`, `hero_sync`, `kill_feed`) and Fabric's `attachment_sync_v1`.
- **Types seen and decoded, kept:** `add_entity`, `remove_entities`, `move_entity_pos`, `_pos_rot`, `_rot`,
  `entity_position_sync`, `rotate_head`, `set_entity_motion`, `set_entity_data`, `set_equipment`, `update_attributes`,
  `swing_animation`, `entity_event`, `damage_event`, `hurt_animation`, `update_mob_effect`, `remove_mob_effect`,
  `projectile_power`, `take_item_entity`, `block_update`, `section_blocks_update`, `block_entity_data`, `level_event`,
  `sound`, the five payloads above, and the own track's `set_equipment`. **Probed:** `boss_event`, `chunk_batch_start`,
  `_finished`, `level_chunk_with_light`, `forget_level_chunk`, `player_combat_enter`, `_end`, `_kill`,
  `player_info_update`, `player_position`, `set_chunk_cache_center`, `set_experience`, `set_health`, `set_time`,
  `system_chat`, `update_advancements`.
- **Never seen, so not shown to decode:** `explode`, `level_particles`, `sound_entity`, `animate`, `block_event`,
  `block_destruction`, `set_passengers`, `set_entity_link`, `teleport_entity`, `move_minecart_along_track`,
  `voice_line`. The dragon's death sent no `explode` or particles: the client makes those itself from the dying dragon.
- **Found:** the design's tap point (`ServerCommonPacketListenerImpl.send`) misses Fabric's `PacketSender`, which writes to
  the `Connection` (read in `AbstractChanneledNetworkAddon`'s bytecode). On that tree, keyframes held no attachments, and
  the attachment bundles a keyframe meant to capture went to the client instead, every 5 s (inferred from the code and
  the missing payloads; not watched on the client). On `Connection`, each keyframe holds them (2 bundles a keyframe).
- **Found:** a dead hero's keyframes are empty, 0 bytes, own entity included, and the stream holds only `HeroSync` and
  the own track (`dragon_try1_died_dump2.txt`: an 85 KB ring of nothing; the `tap1_` nights too). So 25 s after a death the ring holds no play.
  Why the dead player's own entity wasn't found wasn't read.
- **Minecraft mode doesn't count:** G out stopped the recording (102 ticks counted out of play), and G back started a
  new segment with a keyframe.

**Nothing changed for the player** (as far as shown): position, health, hero and hero mode read the same just before and
after arming, each of the six dumps and the stop; G worked while armed and after; she stayed Kiriko through two deaths and
respawns. **Not shown:** the mob counts (the driver's reads picked up other commands' answers: void); that the keyframe
capture sends nothing to the client (it cancels by construction; not watched on the client); a fight with and without the
tap compared for damage or timings; real mouse and keys; a second player; a dedicated server; a hero other than Kiriko;
two heroes recording at once.

**What step 2 (ghosts) needs now:**
- Nothing about cost: keep the ring as built. Decoding a tick's packets during a replay costs well under a millisecond.
- Get the ring to the client properly: today the dump hands it over through a static in one JVM. Single player can keep
  that for step 2; step 6 needs the reel payload.
- Death: the own track's alive flag and kept plays must cover it. A hero who dies at the end of a dragon fight (common)
  has an empty ring 25 s later, so the play must be pinned before then, or kept as it scrolls out.
- Fights that make the unseen types: a creeper (`explode`), a witch or potion (`level_particles`), a rider
  (`set_passengers`), an enderman teleporting, a lead, a minecart. Check those decode before ghosts lean on them.
- The keyframe's capture stays on `Connection`, and step 2 should watch on the client that no captured packet arrives.
- The first-keyframe spike: measure again with ghosts; spread it if a tick goes over.
### Step 2, the first replay (2026-11-11)
Built on `feature/potg`: `/overcraft potg replay` (dev only) plays the hero's last 12 s from the ring. Server:
`PotgReplay` freezes the world (`setFrozen`), the viewer's own player isn't ticked (`PotgFreezeMixin` on
`ServerPlayer.doTick`), the mod's `END_SERVER_TICK` work and the viewer's input, mode and hero payloads are skipped, and
`ticking_state` and `set_time` are held back from the viewer; it thaws when the client is done, or after a 15 s watchdog,
and sends held looks again (`HeroManager.showState` for everything the viewer tracks). Client: `PotgReplayClient` with
`PotgClientLevelMixin` (getEntities, addEntity, removeEntity and getPushableEntities answer with the ghosts),
`PotgReplayRenderMixin` (the ghost view is on during `GameRenderer.extract` and `render`, and in the replay's own step),
`PotgReplayHoldMixin` (the local player doesn't tick), `PotgReplayKeysMixin` (no `handleKeybinds`),
`PotgReplaySoundMixin` (the run-in is silent), `HeroAnimator.swapAll` (a map of its own while it plays), the chase view in
`CameraMixin`, the HUD hidden as F1 does. Raw rows: [runs_step2](runs_step2/).

**How it ran.** One dev client under the lock, Overwatch and Battle.net checked closed, Kiriko, the harness for the fights
(kunai by `key`, aim from the server), and **real keys** for one check (the PC idle 76 min; `rk_keys.ps1`, the
foreground window guarded before every event, after `rk_mouse.ps1`). The hero model needs the local pack:
`OC_ASSET_PACK` pointed at the main checkout's (E: has no links), as `build.gradle` says. Screenshots are in the
worktree's `run/screenshots/potg_*` and aren't committed (they show Overwatch models).

**The types step 1 never saw** (`types_dump.txt`, a creeper, a splash potion, an enderman hit by `/damage ... arrow`, a
chicken jockey, a leashed cow): `explode`, `level_particles`, `set_passengers`, `set_entity_link` came and decode, 0
errors. The potion's splash is a `level_event`; the enderman's teleport is an `entity_position_sync`, not
`teleport_entity`. Still never seen: `sound_entity`, `animate`, `block_event`, `block_destruction`, `teleport_entity`, a
minecart, `voice_line`.

**What replays** (12 s windows; screenshots looked at):
- **The night fight** (`r_fight`): Kiriko's ghost in her own model with her kunai and her throw clips, the kunai's trails
  (our effects), zombies, skeletons, slimes, arrows, XP orbs, dropped items. The chase view follows her.
- **Her death in the window** (`r_death`): she dies where she fell, then stands at her respawn point; the run-in held one
  ghost (the window's keyframe was taken while she was dead, as step 1 found) and the respawn's own `add_entity`s
  brought back the rest (168 ghosts).
- **The dragon** (`r_dragon`, the End's own dragon, 32 s of kunai): the dragon flying and flashing red when hit, the
  crystals and their beams to it, about 70 endermen, the pillars.
- **Leaving hero mode in the window** (G at 24 and 28 s of the fight): it played without an error; whether she showed
  it isn't in a screenshot.
- **Errors in every replay: none** (decode, handlers, ghost ticks), 1,900 to 11,200 packets applied.

**The world pauses and comes back as it was:**
| Check | Seen |
|---|---|
| Game time across a replay | 1493 -> 1556 ticks with 12 s of replay and about 3 s of reads: it didn't move while it played |
| Hero's ult charge | about +16 over the reads around a 12 s replay (it would be +60 running): the mod's tick waits |
| Health, hero, hero mode, position, client and server | the same before and after in each of the 8 replays on the last two trees (fight twice, walk, mid-air, jump, death, dragon, real keys), except the real-key run before its fix, which left hero mode off (below) |
| Mob counts | the same before and after each (natural spawns between the runs) |
| The live level, entity by entity, client | all unchanged but 4 to 43 that moved under 0.5 m with nothing else changed, every one: interpolations finishing after the freeze arrived; 0 new, none changed otherwise |
| Nothing captured reaches the client (step 1's question) | sent against received by type over two replays and 22 keyframes: 39 types equal, none differ |

**The body stays where it was** (client position read every second during the replay, server position before and after):
- Walking (W held from before): it moved in the tick before the replay began, then held at the same point for 12 s, and
  walked on after; client and server agreed after.
- Mid-air (6 m up): held at y -55.07 for 12 s, then fell and landed (`land`); no kick.
- Holding jump: held mid-jump at y -59.02, then landed.

**Real keys during a replay** (`r_keys`): W held, Space, E, Q, G, 1, the left button. Nothing moved or fired, and the
replay played through (no key skips it). **A bug found by them:** G turned hero mode off after the replay ended, because
the hero's own keys aren't in the options' list, so their clicks waited. Fixed by draining `HeroControls`' mappings too,
and the same keys again left hero mode, ult (not spent), Suzu (not on cooldown), slot and position as they were.

**Costs:**
| | Measured |
|---|---|
| Start (keyframe and run-in of 2 to 73 ticks) | 2 to 31 ms |
| Replay step (a server tick of packets, then the ghosts' tick) | mean 0.23 to 0.55 ms, max 0.7 to 8.2 ms |
| Ghosts | 81 to 200 |
| Frame time while it plays, window in front | mean 8.5 to 10.4 ms; worst frame 15 to 254 ms, at the start |
| The tap, first keyframe again (server side, ghosts don't touch it) | worst tick 7.5 ms (a keyframe of 6.7 ms with 157 entities); mean 0.142 ms/tick |
| Memory | the client at 4.7 GB with Kiriko's model; free fell to 1.8 GB with gradle daemons up: tight, watch it |

**Not shown, or not done:**
- **First person, and the hero's own sounds as the view's**; the chase view only (step 4).
- **A frozen world for anyone else**: one player; a second player, a LAN guest, a dedicated server: not tried.
- **The statics list**: only `HeroAnimator` is swapped and `Fx` cleared at both ends (live held effects are sent again
  by the server at the end). The other maps keyed by entity id (`MercyFx`, the sound classes and the rest) aren't
  swapped; with Kiriko and mobs nothing showed, but a Mercy or Lúcio window would test it.
- **Escape during a replay**: it opens the pause menu and the replay waits (by design of the step); not tried.
- **The worst frame at the start** (up to 254 ms) wasn't traced: probably the first frame of 180 ghosts' render states.
- **Sounds** weren't listened to (the harness has `heard`; not used): they go through the handlers, not checked by ear.
- **A G inside the window**: no screenshot of that moment.
- **Block packets** aren't applied (by design until block rewind), so blocks broken in the window stay broken.
- Fights were scripted (harness keys, server aim), apart from the real-key check.

**What step 3 needs:** the replay holds; next are `PlayScore`, kept plays (a death empties the ring in 25 s, and a
keyframe taken while dead is empty: the replay then leans on the respawn's own `add_entity`s), the tick 199 hold, the
card, the announcer and `5B1F`. The statics list should be finished before a support hero's window is shown.
### Step 3, the dragon's Play of the Game (2026-11-11)
**The owner's turns during it** (2026-11-11), which this section follows:
- "Do the whole time": plays come from the whole run, not just the dragon fight. If the best play was elsewhere (the
  overworld), everyone is taken there to watch, as Overwatch's POTG changes submaps.
- Replay through an existing replay mod, from the hero's first-person view, instead of our ghosts: that "fixes any
  potential issues" and needs no block rewind. So the ghost engine (step 2) gets nothing more: no more map swaps, no first
  person, no block rewind. It's the stand-in behind an interface until the replay mod's backend is chosen.
- "Are you datamining?": [what the data says](#what-the-game-data-says-about-scoring) is below.
- Scoring values are the owner's to tune: one table (`PotgValues`), no progression weighting until the owner says.

**Built** (on `feature/potg`):
- **`PlayScore`:** the patent's sliding window, 12 s in 1 s steps, over four categories named as the game names them
  (D). It's fed by `Combat.deal` (damage, crits with distance), `KillFeed.onDeath` (kills, push credit included; the
  Shutdown rule), `addFireForHealing` (Lifesaver) and `Player.take` (item pickups: the table is empty). Ult starts are
  seen where the charge is spent: one place for every kit, since the kits' own `afterUlt` calls take no player.
  - Each hero's best window per category is pinned as a copy of the ring when it's set.
  - It's taken again a second after a death that follows it (within 3 s), so the kept play shows the death.
  - Only heroes in hero mode score. The tap and scoring arm themselves the whole run once a hero is in play, in any
    dimension (owner: "the whole time").
- **`PotgValues`:** every number in one table, each marked: the window, the step, the floor, the per-event values, the
  ult share, the death reach, `KILL_BONUS` by mob id (only the dragon: #44), `PICKUP` by item id (empty).
- **The trigger:** the dragon's `AFTER_DEATH` closes scoring and decides whether the killer allows a POTG. A player out
  of hero mode doesn't (owner); a hero, the world or a command does. At `tickDeath`'s tick 199 (`PotgDragonMixin`) the
  best play is chosen, the world freezes, and the showing starts for everyone in the dragon's dimension, the dead
  included. The exit portal is placed at tick 200, after the thaw. Scoring then starts again from nothing.
- **The backend interface:** `PotgReplay.Backend` (`canReplay(where, here)`, `play(show, viewers)`; the backend calls
  `clientDone`). `GHOSTS` is step 2's engine and replays only a play from the viewers' own dimension; for another it
  shows the card alone. A replay-mod backend would take the viewers there (the owner's submap rule). The card, the line,
  the music, the freeze and the choice don't depend on the backend.
- **Client:**
  - `PotgCard`: "PLAY OF THE GAME" (D 0DC5), "<NAME> AS <HERO>" (D 0DC6 " AS "), the category, drawn with `HudText` on
    a band like VICTORY's. Layout and timings ours.
  - `PotgCard.Holder`: the screen held open for the whole showing. It takes every key and click and no key closes it
    (owner: nobody skips).
  - The client's id-keyed static maps (65 found by type in 29 classes) are emptied while the replay plays and restored
    after, as is HeroAnimator's.
  - World sounds are stopped at both ends.
- **Sounds:** the announcer's "Play of the game." (voice set 001D, file A8C4, 1.66 s) and cue 5B1F (bank 1E5C8, file
  2B0E13, Opus stereo, 16.84 s). Extracted offline with Overwatch and Battle.net checked closed: round 8's
  `extract_voice.py` and kiriko-sounds' `extract_banks.py "Potg=5B1F"`, into the archive's `extracted\154088`. They're
  converted as the pack tool does (mono Vorbis q5) into the main checkout's local pack, with two entries added to its
  `sounds.json`. The sound map entries are in `tools/soundmaps/kiriko.txt`, so the pack tool makes them next time. Never
  committed.
- **5B1F by proxy** (not heard; nobody listened):
  - Its length, 16.84 s, is the card's 4.8 s plus the replay's 12 s, and the wiki's 17 s.
  - Its loudness by second: -17 -17 -20 -23 -24 -16 -16 **-9 -10 -10** -12 -12 -23 -26 -30 -36 -47 dB. That rises to
    a peak at 7 to 10 s and fades out by the end: music shaped like a highlight, not an effect.
  - Where the data plays it: graph 0D3B's message 19C8, and graph 1B4C directly (music-round9: M-L).
  - Identity M: the wiki calls the match-end cue the victory theme, and no POTG string ties 5B1F to the POTG.

**What played** (one client, Overwatch and Battle.net checked closed, raw rows in [runs_step3](runs_step3/); screenshots
in the worktree's `run/screenshots/` and not committed):
- **The real dragon kill on the final code** (`final_*`):
  - The overworld: Kiriko fought zombies at night. Her best High Score window there was 2500, pinned.
  - The End: crystals removed, the dragon's health set to 24, then killed by her kunai with the left mouse button held
    by real OS input. The input was guarded on the game window being in front; the aim was set from the server.
  - VICTORY, then the dragon's death, then at tick 199 the card "KIRIKO AS KIRIKO / HIGH SCORE". Her End play (2615,
    the kill's bonus in it) beat her overworld one. Then the 12 s replay with the dragon, endermen and her kunai.
  - Then the exit portal opened (an end portal block at y 61 after; none before).
  - Real keys during the card and the replay (W held, Space, E, Q, G, 1, Escape, Enter, H, left button): nothing
    skipped, opened or changed.
  - At the card against after: position 30.5 62.0 0.5, client and server; health 20; hero Kiriko; hero mode on; ult
    full: the same.
  - Scoring cost: mean 0.015 ms a tick, worst 2.9 ms (a tick with a pin's copy).
- **Two heroes, Mercy shown, a death in the kept play** (`mercy_two_heroes_death_driver.txt`; the overworld, since
  DevBot's bots live there; a summoned NoAI dragon killed by a server explosion):
  - Mercy healed the bot Ana with her beam (the left button) for 10 s and scored Lifesaver 644. Ana's grenades on
    husks scored High Score 177.
  - Mercy died. Her Lifesaver play was taken again with the death in it.
  - The dragon's death chose Mercy's play: card "KIRIKO AS MERCY / LIFESAVER", then the replay. Her ghost was in her
    model, Ana and the husks were there, and she dies at its end.
  - Real keys at the card left nothing changed.
- **The first real kill** (`kiriko_dragon_driver.txt`, an earlier tree): the same chain in the End, screenshots looked
  at.
- **Errors in every replay: none.**

**Found in play, and fixed:**
- The tap's statics carried a world's state into the next (its bound protocol: `damage_event` failed to encode).
- A dragon whose chunks unload ended the fight: the End unloads when its only player dies.
- The dead weren't viewers (they aren't in `level.players()`).
- The card showed the last replay's stale camera.
- Scoring missed the dragon's own kill second.

**Not shown, or not done:**
- **Card only** (a best play in another dimension than the viewers'): built, never triggered in a run.
- **A Minecraft-mode killer** taking the POTG away: the rule is in code, not played.
- Mercy's beam isn't visible in the replay frames: probably `HeroSync`, which the replay doesn't apply (step 2). Left
  for the replay-mod backend.
- The `PICKUP` hook ran (a blaze rod picked up, no error, no score: the table is empty), but no value is set.
- Sounds not listened to. A second player, a server, other heroes.
- Most fights were scripted. The dragon's health was lowered and its crystals removed so a kunai could finish it.
- No critique agent has read this step.

#### What the game data says about scoring
Searched offline with Overwatch and Battle.net closed (`runs_step3/potg_scan*.py`, `potg_types.py`, their json):
- **Names (D H):** strings 38F4 SHARPSHOOTER, 405D LIFESAVER, 362B SHUTDOWN, 4AA5 ULTIMATE SHUTDOWN, 38D0 HIGHLIGHT,
  C27F "High Score", 0DC5 PLAY OF THE GAME, 474A PLAY OF THE MATCH, "Today's Top 5". The categories are named as the
  game names them (Lifesaver, one word). Ultimate Shutdown is a name the data has and we don't score separately yet.
- **Structures (D, TankLib's schema):**
  - Statescript action `STU_0FC6DAEA` carries a `PlayOfTheGameData`: a highlight type, id, value, targets and
    description, as config vars.
  - A highlight type `STU_C25281C3` holds a description, a list, and two floats defaulting to 1 and 8.
  - The highlight file (`tePlayerHighlight`) records the kind (POTG, Top 5, manual), hero, intro, skin, position and
    highlight type.
  - `POTGType` is the intro's choice: Normal, Default, SombraHack, Random.
- **Not in this build's client data:** none of the 5,687 statescript graphs holds a `STU_0FC6DAEA`, and no asset type's
  root is a highlight type. So the events, values, window and selection aren't in what the client ships. They run on
  Blizzard's servers (BZ: the patent describes server-side logging). The two floats' meaning is unknown (a weight and 8 s
  is a guess, not used).
- **So:** the names are D, the method is BZ, the window W, and every number is ours (in `PotgValues`).

#### Progression events: the owner's decisions (2026-11-11)
All ours (no source), all to High Score, all in `PotgValues`, only for heroes in hero mode. In-game check:
`runs_scoring/scoring_values.md`.
- **Kills** (`KILL_BONUS`, on top of the victim's capped health): blaze 750, enderman 500, wither skeleton 200 (~450 in
  all with its health, ~700 with the damage that killed it: "about 1.4 of a zombie", a zombie ~500 with its damage),
  elder guardian 1000, the Wither 2000; the dragon's 1500 unchanged (#44).
- **Pickups** (`PICKUP`, per item, no cap): blaze rod 250, ender pearl 150. An eye of ender picked up: nothing.
- **End crystal** destroyed by a hero (a hit, their projectile, or a chain from a crystal they set off): 400
  (`END_CRYSTAL`; "there is like 9 of them ... not the most exciting POTG"). Choke point `EndCrystal.onDestroyedBy`
  (`PotgCrystalMixin`): `hurtServer` passes the cause on to a chained crystal's blast.
- **Eye of ender** placed in a frame: 100 (`EYE_PLACED`); the eye that completes and opens the portal 200 instead, not on
  top (`PORTAL_OPENED`). Choke point `EnderEyeItem.useOn` (`PotgEyeMixin`: level event 1503 placed, 1038 opened).
- **First entry** into the Nether 500, the End 1000 (`FIRST_NETHER`, `FIRST_END`): once per player per world, saved in
  `players.json` (`entered`). Fabric's `AFTER_PLAYER_CHANGE_LEVEL` and `AFTER_RESPAWN`. The first entry is the first
  whatever the mode: one in Minecraft mode scores nothing and spends it (our reading of "first").
- **No anti-farm cap** (owner: "there won't be farms. This is a one-off speedrun").
- **Not decided:** whether a crystal counts for Shutdown (it doesn't).
- **Fixed on the way:** a pickup scored nothing even with a value: `Player.take` runs after the inventory took the stack,
  so the entity's stack was already empty (air). The pickup hook moved to `ItemEntity.playerTouch`'s locals. And a calm
  enderman killed in one blow scored 0 (a bystander): a type with a `KILL_BONUS` now scores even calm; a cow still doesn't.
### Step 4, the replay engine (2026-11-11)
**The owner's decisions** (2026-11-11): vendor ServerReplay's engine and keep it in Kotlin, taking only what POTG needs;
each player keeps their top 5 highlights, saved with the world, rewatchable alone; the end screen's enemy side is mobs.
The spike that chose it: `techniques/potg/server_replay.md` on branch `spike/server-replay` (not merged here).

**Vendored** (`src/main/kotlin/dev/overcraft/vendor/arcade`, mixins in `src/main/java/dev/overcraft/vendor/arcade`; what
and why in `VENDORED.txt` there): Arcade's `arcade-replay` at `CasualChampionships/Arcade` 26.3 `0325e1b`, MIT,
Copyright (c) 2024 senseiwells. 59 files, 6,206 lines (5,595 Kotlin, 611 Java), 203 classes and 741 KB in the jar.
Kept: the player recorder (chunks included), the Flashback writer and reader (snapshot-and-seek), the server-side viewer
and the rejoin code that puts a player back, and the few `arcade-utils` pieces they call. Left out: ReplayStudio and the
ReplayMod format (GPL parts), chunk recorders, voice chat, packs, compat layers, the event bus, commands and config.
The MIT text sits beside the files (`LICENSE-arcade`), in the jar (`META-INF/LICENSE-arcade`) and the README's credits.
Needs `fabric-language-kotlin` 1.14.1+kotlin.2.4.20 or later (8.1 MB, the players install it). Packages moved to
`dev.overcraft.vendor.arcade`, so a ServerReplay install beside it can't clash.

**The spike's fixes** (each marked "Overcraft" in the vendored code):
1. The tap is on `Connection.send` (`PotgSendMixin` -> `ReplayTap`), and a recorder's snapshot also records what Fabric's
   `START_TRACKING` listeners send (the attachments, `HeroManager.showState`) without sending it. Replayed heroes are in
   their models: Mercy seen from Kiriko's eyes (`now2_*_10s`), Kiriko from behind by client 2 (`ow_tp_2_24s`).
2. Disk: `PotgRecorder` records every player with a hero in 5-minute segments (one Flashback chunk each: a seek
   fast-forwards at most a segment). A finished segment is zipped only if a kept highlight is in it, else thrown away;
   a kept one is deleted when its last highlight is replaced. Numbers below.
3. Boss bars: the viewer passes updates only for bars the replay added, and removes those at the end. Two viewers in the
   End over the dragon's bar: nobody kicked.
4. On the replay's login and the return: no Hero Select, no asset-pack line, no toasts (`ClientPotg`, CARD/SOLO to END);
   recorded chat isn't replayed; no progress bar (a `ServerBossEvent` starts visible: found in play, hidden).
5. Camera on the scorer (`ClientboundSetCameraPacket` when the replay adds them and each second at the window: the add
   alone didn't hold, found in play); the viewer's input is dropped (keep-alives pass); every viewer starts in one tick.
   While seeking, sounds, particles and our one-shot payloads (`own_sound`, `kill_feed`) are skipped.
6. Timing by wall clock: the card's end, the watchdog, and each viewer's end (its stream reaching the window's end), so
   a paused integrated server holds nothing up.
7. `PotgReplay.REPLAY` is the backend. The dragon's tick 199: the best highlight of anyone's since the last POTG; the
   card, line and music for everyone online, the world frozen (its news kept from viewers), recorders stopped and zipped
   under the card, then all viewers into the play's dimension through the scorer's eyes and back. **`GHOSTS` is
   removed**, with `PotgTap` and the client ghost code (about 1,500 lines): this backend plays in single player, on a LAN
   and (by the same path) on a server, any dimension, so no fallback is needed.

**Highlights** (`Highlights`): per player the 5 best 12 s windows by score, any category (the window's category is the
patent's pick); a new one replaces the lowest; two that overlap (same segment, ends under 12 s apart) keep only the
higher. Each keeps its score, category, hero, time (epoch ms), dimension, segment, end tick and the scorer's entity id,
in `<world>/overcraft/potg/<uuid>/highlights.json` beside its segment zips. They survive leaving and the server closing
(segments close on `SERVER_STOPPING` and on disconnect; stray zips are tidied at start). From code:
`Highlights.of(server, uuid)`, and `PotgReplay.watchHighlight(player, owner, highlight)` plays one to its owner alone:
only they are taken out and put back, the world isn't held (single player pauses itself). Dev: `/overcraft potg
highlights|watch [n]|now|disk`. The menu (after a dragon kill) and the hero's intro before each are the next step: the
intro goes where `PotgReplay.watchHighlight` sends `PotgPayload.SOLO` (the client's `ClientPotg` SOLO case).
The POTG chooses among highlights made after the last POTG (`world.txt`), so a later dragon doesn't show an old play.

**Mobs for the end screen** (`MobStats`): per mob, over the run, players killed and damage dealt to players (Overwatch
HP), its type and custom name; the top 8 and every dragon, in `overcraft/potg/mobs.json`; at a dragon's decision copied
to `mobs_last.json` and cleared. Data only.

**What played** (two dev clients over LAN, Overwatch and Battle.net checked closed before each launch, `-Xmx3G`, the lock
held; harness-driven except the kill; raw rows in [runs_backend](runs_backend/); screenshots in the worktree's `run/` and
`run2/screenshots/`, not committed):
- **The real dragon kill:** Kiriko (client 1, host) and Mercy (client 2) in the End, crystals removed, the dragon's
  health set to 30. Kiriko's kunai by the **real left button** (`potg_mouse.ps1`: OS input, refusing unless client 1's
  window was in front, idle 29 min first), aim set by server `tp`. Then VICTORY's chain: at tick 199 the card
  "KIRIKO AS KIRIKO / HIGH SCORE" on both clients (`dragon_tp_2_8s`), then the replay of the kill from Kiriko's eyes on
  both, and both back: client 1 at 0.5 64 12.5, yaw 115.74 (-604.26), pitch -28.49 before and after; client 2 at
  7.80 62 28.56, health 18, Mercy, hero mode, the same. Nobody kicked.
  (That run's client 2 had the pause menu over the replay: fixed, and the next two runs had none.)
- **Again after the fixes** (`kill2_*`): a summoned dragon, health 12, killed by the real left button: from Kiriko's eyes
  on both clients, the dragon overhead (`kill2_tp_1_7s`, `kill2_tp_2_4s`), no menu over the replay; VICTORY was still
  up on return (`kill2_tp_2_10s`); both back where they were.
- **A play from the overworld at the dragon's death:** both in the End, the best play since the last POTG Kiriko's
  overworld fight; a summoned dragon killed by explosion damage (no hero's kill: the world allows a POTG). The card,
  then both viewers taken to the overworld at night, Kiriko in her model throwing kunai (`ow_tp_2_24s`, client 2 in
  third person), from her eyes on client 1 (`ow_tp_1_17s`), then both back in the End exactly (0.5 62 30.5 and
  6.5 62 30.5, yaw and pitch the same).
- **Solo, across dimensions, after a restart:** the world closed (window closed: `SERVER_STOPPING`) and reopened; her
  4 highlights were read back; `watch 0` (an overworld play) from the End took only her to the overworld and back to
  0.5 64 12.5, yaw 115.74 (`solo_9s`).
- **Disk:** a 31-minute run (`soak_driver.txt`, `soak_out.txt`) with both heroes in the overworld, moving on 40
  blocks a cycle (new chunks each time), a fight every 1.5 minutes, the disk read every cycle, on top of 45 minutes
  already recorded in that world. The whole `potg` folder stayed between **21 and 27 MB** for the two players (50 MB
  in the first minutes, a long first segment); per player the folder peaked at **15.7 MB** with 5 highlights (the raw
  segment being written, 8 to 12 MB, plus 5 kept zips, 5.6 MB) and **11 MB** with none (the raw segment alone). It
  rolled every 5 minutes (segments 5 to 11), unkept segments were dropped, and a kept one was deleted when its
  highlight was replaced (seg-8 by seg-11 at the end). A kept 5-minute segment zips to 0.6 to 2.8 MB (about 10 to 1);
  raw, a segment is 8 to 26 MB, mostly chunks. **The bound per player:** one raw segment (about 10 to 25 MB, more while
  exploring) plus at most 5 zips (at most about 14 MB); counted raw, 6 segments, about 70 MB, the brief's estimate.
  For 6 players over 2 hours that's about 90 MB on disk as measured (at most about 240 MB), against about 2 GB raw
  unbounded (the spike's figure). Kept highlights survive any length of run: only 5 per player.

**Not shown, or not done:**
- A dedicated server (the LAN guest takes the same network path); a dead viewer at the start.
- Kiriko's first-person arms, weapon and HUD in the replay: spectating draws none (the spike's finding; client work).
- The card's music and line by ear: they play from the local pack as in step 3; nobody listened.
- The kill run's aim came from the server; only the button was real. The overworld fights were harness-driven.
- A play that straddles two segments is cut at the segment's start (at most once in 5 minutes).
- The menu, the hero intro, the end screen and the POTG's look from [potg_reference](potg_reference.md): next steps.
- A mob killing a player for the mob stats (the heroes had resistance): only damage was counted in play.
- No critique agent has read this step.

**Lesson: a recorder must not outlive its player in the player list (#140, 2026-11-13).** The vendored recorder finds
its player by UUID each time (`server.playerList.getPlayer`); with nobody there, `level` quietly falls back to the
overworld but `takeSnapshot` throws, and a snapshot comes every 5 minutes or on a dimension change, so an orphan sits
silent for up to 5 minutes and then stops the server. Closing it only on `onDisconnect` missed every removal without a
disconnect (DevBot's `bot remove` is `PlayerList.remove` alone; fake-player mods do the same). So `PotgRecorder.tick`
first closes any recorder whose player is no longer listed, before the engine ticks. Reproduce such crashes with
`/tick sprint 6200` instead of waiting 5 minutes. Run: [testing/runs/potg_140](../testing/runs/potg_140/record.md).

## 5. Risks
**Performance on the owner's machine** (a hard crash on 2026-11-04 under heavy load):
- Armed only in a dragon fight, so normal play pays nothing. Bytes bounded per hero: the rolling ring is fixed, the oldest
  goes first, with a hard cap of 2 MB past which that hero's window drops out of the running; kept plays stop at 4 (ours).
- No chunk work, no second level, no mesh rebuild, apart from section rebuilds from block rewind (last phase, capped).
- During a replay the world's still, so the server does less than usual, and the client draws fewer entities. The first
  two phases measure it.
- Testing stays one client at a time under the lock, as ever. Don't test this while something else is running.

**A second player:**
- Someone who has left: their ghost needs `getPlayerInfo` (**read**: null means no ghost). Keep the profile in the reel's
  header. A hero who left before the end has lost the POTG, as Overwatch has it (W).
- Someone who joins during it: they just aren't in the POTG, and see a still world until it ends.
- Viewer far off or in another dimension: card only, in a still world.
- A second player out of tracking range isn't in the first one's stream; they're not in the shot either.
- If the running world is ever built, viewer protection must not use the saved `invulnerable` flag: a crash mid-POTG
  mustn't leave anyone immortal. `canBeSeenAsEnemy` (**read**) needs a mixin.

**Dedicated servers:**
- The tap and scorer are common code; decoding and ghosts are client only.
- The freeze is world-wide, so an option, off by default on a dedicated server, until question 3 says otherwise. With it
  off and no running world built, there's no POTG there.
- The server pays for N heroes' encoding, armed only: if a raid trigger comes, count it again.

**Hero switches, G and death:**
- **G or hero switch in a window:** `Blow` fixes the hero at the time of each hit, so scores stay right. The own track
  carries the change, so a window that spans a switch shows it as it happened, and the card names the hero who scored most
  in it. A window out of hero mode can't be first person; the chase camera holds it (ours).
- **The hero dies in a window:** the own track says so, and the ghost dies with the death animation (`LAST_HERO` handles it,
  after the swap). Their respawn is also in the track, not the stream. The camera pulls back and holds (needs the clip).
- **The viewer is dead when it starts:** a common end to a dragon fight. Respawning from the End means the overworld, a new
  `ClientLevel`, and the chunks gone. So it plays over a hidden death screen and respawn waits.
- **Any level change during a replay:** should now be rare, since the portal's shut. `updateLevelInEngines` is hooked to end
  the replay cleanly, restore maps, clear ghosts. Same for disconnect. The server thaws when the last viewer's done.
- **Bad reel:** a decode error or exception drops the replay, not the game: catch per tick, log, restore. A broken POTG must
  never crash a world, or leave it frozen: the watchdog is on the server, and holds even if the client does nothing.

**Overwatch's rules:** nothing here touches the client. Intro clips, `2F6C` and `5B1F` come through the usual pipeline into
the local pack, never into the repo, and are read offline with Overwatch closed. `checkNoOverwatchAssets` stands.

## Classes read for this note
Minecraft 26.3, decompiled: `Minecraft`, `Camera`, `client.multiplayer.ClientLevel`, `ClientPacketListener`,
`ClientChunkCache`, `client.player.LocalPlayer`, `RemotePlayer`, `client.server.IntegratedServer`,
`client.renderer.extract.LevelExtractor`, `GameRenderer`, `client.sounds.SoundManager`, `SoundEngine`, `network.Connection`,
`ProtocolInfo`, `PacketBundleUnpacker`, `codec.IdDispatchCodec`, `protocol.ProtocolInfoBuilder`, `PacketUtils`,
`protocol.game.GameProtocols`, `ClientboundLevelChunkPacketData`, `protocol.common.ClientboundCustomPayloadPacket`,
`server.MinecraftServer`, `ServerTickRateManager`, `server.level.ServerEntity`, `ServerLevel`, `ServerPlayer`,
`ServerChunkCache`, `ChunkMap`, `server.network.ServerGamePacketListenerImpl`, `ServerCommonPacketListenerImpl`,
`PlayerChunkSender`, `world.TickRateManager`, `world.clock.ServerClockManager`, `world.entity.EntitySelector`, `LivingEntity`,
`player.Player`, `Entity`, `boss.enderdragon.EnderDragon`, `world.level.Level`, `entity.LevelEntityGetter`,
`block.EndPortalBlock`, `dimension.end.EnderDragonFight`. Fabric, from the jar's classes (no sources): `AttachmentSync`, the
networking `ServerEntityMixin`.

Overcraft: `Combat`, `Blow`, `KillFeed`, `Voice`, `HeroState`, `HeroManager`, `Effects`, `Comms`, `OwMovement`, `Overcraft`,
`OvercraftClient`, `ClientHero`, `ClientTow`, `VictoryHud`, `HeroAnimator`, `HeroModelRenderer`, `Fx`, `MercyFx`, `MercySounds`,
`LevelExtractorMixin`, `CameraMixin`, net payloads.

Sources:
[the patent](https://patents.google.com/patent/US20180161675A1/en),
[Game Developer on it](https://gamedeveloper.com/programming/blizzard-applied-for-a-patent-on-i-overwatch-i-s-play-of-the-game-highlight-system),
[KitGuru on the GameSpot interview](https://www.kitguru.net/gaming/matthew-wilson/overwatch-dev-explains-how-the-play-of-the-game-system-works),
[Blizzard's patch notes](https://overwatch.blizzard.com/en-us/news/patch-notes/),
[Replay FPS on Replay Mod](https://modrinth.com/mod/replay-fps); the wiki's Play of the Game, Highlight intros, Cosmetics,
quote and patch pages through its API. Data read: `E:\Overwatch Archive\research\` (strings, announcer, music-round9,
kiriko-anim, kiriko-voice), and Kiriko's six intro effects with TankLib, offline with Overwatch and Battle.net closed.

## The critique, and what it changed
An independent critic read this note against the sources, 2026-11-06, with nothing run. Its verdict: the approach stands,
not as written. Each claim was checked in the source. None was wrong; one was incomplete, and one led somewhere simpler.

| # | The critic said | Checked | What changed |
|---|---|---|---|
| 1 | A ghost camera stops the viewer's moves | Right: `LocalPlayer` 298, 672, 723; `LivingEntity.applyInput` 3144 | Camera entity stays `mc.player`; view placed in `CameraMixin` |
| 2 | The hero's own ghost has almost nothing in the stream | Right: `ChunkMap`'s `player != this.entity`; `sendToTrackingPlayers`; `HeroModelRenderer.extract` | Own track widened; keyframer runs on the hero's own `ServerEntity` |
| 3 | The flag leaves mod code reading live entities; statics aren't all listed; two collisions | Right. My count is 69 statics in 23 classes to its 76 in 24, by a narrower pattern; the point stands | Switch inverted; statics by explicit list; live mod payloads dropped; `mc.player` no longer feeds animators |
| 4 | The tap must unbundle | Right: `addPairing`, `ProtocolInfoBuilder`, `IdDispatchCodec` | Unbundle |
| 5 | Replaying block packets writes the live level | Right | Allow-list per phase; none applied at first |
| 6 | `sendPairingData` isn't a full keyframe; missing packets; later keyframes must be skipped | Right; attachments read from the jar's classes, not sources | Keyframe widened; four packets added; only a reel's first keyframe applied |
| 7 | Ghosts leak through ticks; hand, overlays and `detached`; sound bypasses | Right. **Pushes and crystals have a simpler answer than patching each:** `ClientLevel.getEntities()` is the one choke point, so in the ghost view neither finds the live world | One entity view; render gates listed; sound filtered in `SoundEngine` |
| 8 | `HeroSync` through its receiver flips the viewer's identity | Right: 85 call sites | Kept in a slot of its own |
| 9 | Protection comes too late; the POTG starts as the portal opens | Right: `EndPortalBlock.entityInside`, `showEndCredits`, `WinScreen`, `setDragonKilled` | Portal held behind the POTG at tick 199; protection in the dragon phase |
| 10 | A freeze, with `ClientboundTickingStatePacket` withheld, is simpler | Right on what it stops and on nothing being saved. **Incomplete:** `MinecraftServer` also sends `ClientboundSetTimePacket` every 20 ticks, frozen or not, which would drag the client's clock back each second. Withhold both | Freeze recommended; question 3 rewritten |
| 11 | Memory low by 2 to 3 times at most | Right | Table corrected; still an estimate |
| 12 | The first step can be smaller | Right | Tap and round trip first; a stricter "done when" |

Left alone, as the critic found them clean: `Entity.setId`, `isEntityVisible`, `ensureRunningOnSameThread` inline on the
render thread, no deferred receivers, encoding in `send`, option A's rejection, the sourced facts it spot-checked.

The lesson for the next design done from reading alone: a claim about what code will do ("the body keeps ticking and
sending") needs its gates read, line by line, before it's written. All twelve were readable without a client.

# Critique: fix/systems (#93, #97, #98, #100), 2026-11-04

An independent critic's pass over commits 1820831, decf856, 86438d7 and c5dcff9. Code, Minecraft's decompiled source, the records,
the client logs, two screenshots and the wiki only. No game, no build, nothing under `E:\Overwatch Archive`. So every finding marked
**(read)** is a reading of the code that still needs reproducing in game before it is fixed (CLAUDE.md).
Up: [Techniques](../README.md) · [hand_play](hand_play.md) · [qa_critique_2026-11-02](qa_critique_2026-11-02.md) · [minecraft_26](../minecraft/minecraft_26.md) · [mercy](../heroes/mercy.md)

Severity: **High** = a player meets it in a normal session, or it breaks saves, multiplayer or an owner's rule. **Medium** = a common case or a
visible miss. **Low** = an edge case or a record to correct.

## Verdict
**Don't merge the branch as it stands.** #98 alone could go. #93, #97 and #100 each need the small fixes below, and hand play of those fixes.

| Fix | Merge? | What blocks it |
|---|---|---|
| #93 | No | S2 (kill credit lost), S4 (a second player never tried, and the repo's own note says the transport isn't enough) |
| #97 | No | S1 (`savedView` is an unsynchronized counter that worldgen threads write) |
| #98 | Yes | Nothing blocking: L4 and L5 are notes for the owner |
| #100 | No | S3 (ult casts pause and resume on G back), M1 (Brigitte's whip and flail carry on), M3 (the owner should choose the ult rule) |

## Serious

**S1. #97: `Sleep.savedView` is a plain `static int` changed from several threads. If it ever sticks at non-zero, no mob can be held until the game restarts. (read)**
- `Sleep.java:97-112`: `savedView++`, `savedView--`, and `holdApplies` returns `savedView == 0 && aiHeld(mob)`.
- `MobMixin` runs `ownAi` inside `Mob.addAdditionalSaveData`, and that isn't main-thread only:
  `ChunkStatusTasks.generateSpawn:167` -> `NoiseBasedChunkGenerator.spawnOriginalMobs:470` -> `NaturalSpawner.spawnMobsForChunkGeneration` ->
  `WorldGenRegion.addFreshEntity:371` -> `ProtoChunk.addEntity:193` `entity.save(output)`. Those tasks run on `ChunkMap`'s worldgen executor
  (`Util.backgroundExecutor()`, `MinecraftServer:355`). Mobs placed by structures take the same `addFreshEntity`.
- A lost `++` or `--` leaves the count off for good. It's static, try/finally can't repair it, and nothing resets it on stop, so it outlives the world.
  Then Sleep Dart, Rally's stun, Accretion's knockdown and Flux stop holding mobs, silently, until the client restarts (Sleep.tick still zeroes their speed, but the AI runs).
  A negative count does the same.
- Even without a lost update, while any worker is saving a mob the main thread's `isNoAi` answers false for held mobs: a flicker of AI during exploration.
- Why play didn't find it: every test world is superflat, where `FlatLevelSource.spawnOriginalMobs:131` isn't the noise generator's spawner (the village seen in the screenshots is the only worldgen mob source). The windows are short, so it is rare and permanent, which is the worst kind to find later. Frequency unknown.
- Fix: don't use a global at all. In the three wrappers read the real flag directly (`DATA_MOB_FLAGS_ID` through an accessor), or at least a `ThreadLocal`.

**S2. #93: quick melee, Hypersphere pulls and Accretion's splash lost their kill credit (the #84 rule). (read)**
- `Combat.deal` (`Combat.java:~108`) records a push when the hurt moved the target. That only ever fired because of Minecraft's default knockback, which #93 removed. So each ability that pushes must now call `Combat.pushed` itself.
- Those that do: Brigitte, Soundwave, Sonic rounds, Purr, Catnapper, the rock's direct hit, Flux, Hyper Strike.
- Those that **don't**: `QuickMelee.java:87` (`target.knockback` + `pushedNow`, no `pushed`), `Hyperspheres.java:162` (the wiki lists Hyperspheres as knockback), `Accretion.java:~181` (the burst's push on everyone but the direct target).
- Result: quick-melee a mob off a ledge or into lava or the void: `KillFeed.onDeath` -> `Combat.recentPush` finds nothing, no killer, skull, no kill marker, no voice line. The classic boop.
- The commit's table measures distance only. Nothing checks a fall after a push. The javadoc on `Combat.pushed` ("its knockback inside the hurt, seen by Combat.damage") and the check in `deal` are now dead.

**S3. #100: casts on Juno, Sombra and Lifeweaver pause on G and resume on G back, the same class as the reproduced Swift Step. (read)**
- The kit's tick returns early when `!controls`, and `leavePlay` doesn't clear these:
  - Juno: `junoRayCast`, `junoRingCast` (`Juno.java:68-73`, not in `leavePlay` :236). Ult charge is spent at the press (:139). Press Q, G within 0.5 s, walk around, G again minutes later: the Orbital Ray lands then, wherever she stands.
  - Sombra: `empCast`, `castTicks` (Cyberspace) (`Sombra.java:87-94`, not in `leavePlay`). EMP is charged at the press (:135). It pulses on G back.
  - Lifeweaver: `treeCast`, `treeAt` (`Lifeweaver.java:113-125`, reset list omits them, as does `leavePlay`). The tree sprouts at the old `treeAt` on G back, in whatever dimension he's in by then.
- `HeroKit.leavePlay`'s contract says "a cast under way" ends. The played list covers Juno's glide, Sojourn's slide and Sigma's Grasp; no ult cast on these three.

**S4. #93: every push on a player is unplayed, and the repo's own note says the transport used isn't enough. (read, plus the record's own "not tried")**
- `Combat.pushedNow` sets `syncVelocity`. `Accretion.java:212-216` says, from two-client play: "syncVelocity alone wasn't enough": the tracker sends it a tick later, and the player's next movement packet has zeroed the vertical by then. Same transport as markHurt, so not worse, but `minecraft_26.md` calls this "reaching their own client" without having seen it. Expect Shield Bash's 0.25 and Whip Shot's lift to be lost on a grounded player.
- Whip Shot is new behaviour. `Brigitte.java:474-475` now sends every tick, with `v.y` from the server's copy of a player's speed, which is stale. Each send overwrites the client's vertical speed for `WHIP_PUSH_TIME`. Before, only the first tick's reached them. Risk: a float or a stutter in mid-air.
- **Sonic Amplifier brings back the jolt #93 removed.** `SonicEntity.java:95-96`: `target.push` + `syncVelocity` per round on a player, sent from the stale server motion. That's the #82 jolt, four rounds a volley. Same concern for Soundwave and Purr, but those are rare and strong; the rounds are the weapon.
- The memory note says a second client crashes this machine. Then say the PvP half of #93 is **unverified**, and don't merge it as done. The owner's list asks for a second player by name.

## Medium

**M1. #100: Brigitte's whip, flail and shockwave keep running in Minecraft mode. (read)** `Brigitte.tick` (:76-80) calls `shockwave`, `whip`, `swing`, `bash` with no `controls` check. `Brigitte.clear` only resets the bash, Rally and strike. So G mid-swing or with the whip flying deals `Combat.damage`, plays sounds and pushes after G. Played list: "barrier and Rally" only.

**M2. #100: Kiriko keeps +30% speed after G. (read)** `KitsuneRush.java:178` re-applies `Combat.rush` each tick to anyone in the corridor with no `controls` check, and `Combat.java:586-591` puts the speed on. 4.32 -> about 5.6 m/s in Minecraft mode for up to 8 s: the same number as the Lifeline bug, renewed each tick like Lúcio's was. The kit's doc allows effects on *others* to run; this is on the hero. Juno's own run through her ring is the same at a smaller scale. Not in the played list.

**M3. #100: "One rule" isn't one rule for ults. The owner should choose.** Today, G in an ult: Rally, Sound Barrier's leap, Flux, Nano's cast: **cut, charge lost**. EMP, Orbital Ray, Tree: **paused, then resume** (S3). Overclock, Valkyrie, Rush: **run on**. Disruptor Shot: "nothing spent". G is one key next to WASD, and losing a full ult to it is harsh; the note's "ours, as dying would" is a guess about a choice that belongs to the owner.

**M4. #100: looks that stay on after G, despite "nothing of the hero's play carries over". (read)** Sojourn: `overclock()` ignores `controls` and :86 keeps writing `SYNC_OVERCLOCK`, so her gun's glow and loop stay for every client through Minecraft mode, and on a corpse (no tick while dead, `leavePlay` only clears on `forGood`). Mercy: `flags()` keeps `FLAG_VALKYRIE` while its time runs (:105-115 publishes it). Not seen in any screenshot; check in third person.

**M5. #93: no fight after the one change that alters every fight.** Mobs no longer stagger. Melee mobs reach you faster and a creeper can't be kept off. The scripts run with `spawn_monsters false`, on superflat, with a 200-health zombie at `movement_speed` 0. A search of `sys_*.py` finds no skeleton, no creeper and no fight. CLAUDE.md asks for "a real fight (a night in survival...)". This is a balance question the owner will feel first.

**M6. Knockback rules read differently per ability. (read)**
- Knockback resistance: Soundwave, Accretion, quick melee and D.Va's honour it. Shield Bash, Rally's shockwave, Whip Shot, Purr, Catnapper, Sonic rounds and Hypersphere pulls don't (`Entity.push` or `setDeltaMovement`). A warden or ravager (resistance 1.0) gets thrown by Whip Shot. The 7.75 m was measured on a zombie only.
- The wiki's reduced-knockback table (Jetpack, Glide Boost, Valkyrie, Flux) is coded only in `LivingEntityMixin.overcraft$stateKnockback`, on `knockback()`. After #93 that's quick melee and mob hits. Soundwave or Whip Shot on a gliding Juno or a Valkyrie Mercy takes all of it.
- Not caused by #93 (it was so before), but #93 asked for this comparison and nobody wrote it down.

**M7. "Real keys" and "the real thing" overstate it.** `DevServer` `key` is `KeyMapping.set` in the client, not the mouse and keyboard. All hero deaths in the scripts are `/kill @s` (`sys_lib.die`), never a mob. The golem dies by `/damage` ("Applied 500.0 damage": `sys98_golem_back.png`), not a hero's hit (so no player-kill path, unless the command named one: not checked). `overcraft reset` refills. All fine for measuring, and it's what the harness can do, but CLAUDE.md says "real mobs hitting the player... not a dev command". The record should say which it was.

**M8. D.Va (`feature/dva`) on top of this.** She branches from before all four commits.
- `leavePlay` needs: `stopMechKit`, today's body of `Dva.heroMode(false)` (mech health stored, call cut), `clear` when `forGood`. On death: the pilot's state, and a flying Self-Destruct (`dvaSelfDestructId`) under whichever rule M3 settles.
- `DVA_FORM` goes in `HERO_LOOKS`. `HeroManager` conflicts: her G hook at `setHeroMode`, and the two removed `if (state.hero == ...)` chains.
- **Call Mech's landing**, `Dva.java:426-427`: `setDeltaMovement` + `needsSync` only. It relied on markHurt, so a player landed on won't move. Use `sendSpeed`, as her Boosters and Self-Destruct do.
- **No `Combat.pushed` anywhere in `dva/`**: Boosters, Self-Destruct and Call Mech are all on the wiki's list, and S2 applies to all three.
- `MicroMissiles.java:140, 167` "undo Minecraft's push" is dead after #93: remove it. Her `Combat.damage` calls also lack the `Blow` argument.

## Low
- **L1. `Sleep.SLEEPERS` isn't cleared on stop.** `Overcraft.java`'s `SERVER_STOPPED` handler clears `GraviticFlux` and others, not Sleep. An entry pins its mob and closed `ServerLevel` until the next world's first tick. The older critique's "`Overcraft.java:237` clears the map" is wrong. Add it, and zero `savedView` with it unless S1 removes it.
- **L2. Mobs keep stale modifiers.** `dropStaleModifiers` is players only. An ally saved mid-buff under an older build keeps `juno_hyper_ring` or the overhealth cap as permanent modifiers for good, like the statues: same "older worlds" caveat, unmentioned.
- **L3. Headbutt.** `minecraft_26.md` says it's among abilities that "already push". Nothing in `src` is called that, and `tools/reference/jetpack_cat.md:106` says it was removed in BZ 2026-08-11. The wiki copy I could read still lists it, so the list is older than that note. Sonic rounds: the wiki's list omits them, but `tools/reference/lucio.md:15` cites BZ 2024-07-12 "Knockback exists, no number". So the existence is sourced, the 0.6 m/s is ours: say that, rather than "by our own choice".
- **L4. #98's rule, for the owner.** "Once" also removes the XP from a mob that gave none at its first death (a wolf a skeleton killed). The mark isn't `copyOnDeath`, so a conversion drops it (a resurrected villager struck by lightning: its witch gives loot; a villager cured or zombified isn't worth a farm). Neither is a dupe. Saddles and armor stay on the ground after Resurrect, which a player may read as a bug.
- **L5. An exploit left standing.** `Souls.resurrect` fires `ZOMBIE_VILLAGER_CURED` on every villager resurrection (#52, the owner's own request), so kill-and-resurrect stacks the cure discount. It's the same family as #98's farm. Ask whether it should also be once.
- **L6. #98's "not tried: copper golems" is moot.** `Allies` has no copper golem, so it leaves no soul. Same for slimes and zombie villagers. Only `AbstractVillager`, iron and snow golems, allays and owned pets. So the slime-split and zombie-villager questions don't arise.
- **L7. Lifeweaver's record disagrees with itself.** The commit says the platform is "gone on ... death"; `HeroKit` says what's placed "stays through G and death", and `leavePlay` keeps it. `PetalPlatformEntity:274` ends it when the owner is removed, i.e. at the respawn. A player waiting on the death screen keeps it up.
- **L8. Spectator is not a door.** `/gamemode spectator` skips `outOfPlay`, so Sombra's stealth and others run through tick branches only. The record says so; it's still a gap in "one rule".
- **L9. Cost.** `isNoAi` does two UUID map lookups per call, with no `isEmpty` shortcut, on every server mob many times a tick. Cheap, but it's one line.
- **L10. Leftovers.** `Accretion.java` "Minecraft's blast knockback undone" and its `before` are dead too. `logs/` is untracked in the worktree: don't `git add -A`.

## Checked and found sound
- **`heroHit`'s lifetime:** `dealingWhile` is try/finally and restores the outer blow, so nesting and delayed damage are right (each DoT tick is its own `Combat.damage`). No hero hit on a living target bypasses it: every other `hurtServer` in `src` is behind a non-living branch (kunai, blaster, paw, thorn, sonic, rock, sphere, suzu with 0 damage, Railgun's armor stand, whose `hurtServer` has no knockback). Barrier-deflected fireballs explode as Minecraft's, as they should.
- **Removed markHurt:** in 26.3 it only sets `syncVelocity`. Health is the player's own tick, the hurt animation is `broadcastDamageEvent`, both unaffected. A hit with no push loses nothing. Minecraft-mode swords, mob bites and vanilla shields' branches are untouched. Boats and minecarts: not pushed before or after, as abilities take `LivingEntity` only (not played).
- **`isNoAi` readers:** I went through every vanilla reader. Only three copy it (`Mob`'s save, `Bucketable`, `ConversionType`), all three wrapped. `/data`, portals (`restoreFrom`) and souls go through the save. The others (goals, `ConversionTracker`, leash, dragon, warden, shulker) behave as under the old `setNoAi(true)`. No new statues found.
- **The map:** identity check right. Unload, despawn or dimension change: `isRemoved`, gone next tick. Flux + sleep: an OR, each ends itself. The leak is bounded to one tick, except L1.
- **#98:** the donkey correction is right (`AbstractChestedHorse:73-85`). `Mob.dropCustomDeathLoot` empties what it drops. `AFTER_DEATH` follows `dropAllDeathLoot`. Players never get the mark (`soul.mob == null` returns first), so resurrecting a player is unchanged.
- **#100 doors:** one call per door. Double calls (G then death, death then leave) are safe: Flux, Grasp, gather and Rally's sound all guard. `dropStaleModifiers` can't remove anything legitimate: no item or gear carries an `overcraft:` modifier, and `hero_health` is kept. Transient max absorption is safe on rejoin: `LivingEntity:1131` clamps at the tick. Hero health stays permanent. Sigma, Sojourn and Juno are all included.
- **The numbers:** recomputed from the raw `Pos` replies in the client logs. Before: kunai 5.97 m over 3 hits (1.99 each), pistol 3.43. After: eleven runs at 0.00, then 0.50, 3.91, 7.75, 3.41, and 3.27 for #97's walk. They hold. But the `log()` output went to stdout and isn't kept, so `sys_client_*.log` holds only raw replies, and a reader must recompute. The speeds (0.115, 0.035, 0.13) are in `sys_client_100.log`. I did not recompute 4.45, 6.18, the XP or the drop counts. "Flail before the fix" was never measured, so its 0.000 has no before. `sys100_sombra_G.png` agrees with 0.16 -> 0.1 and shows her solid.

## What a player hits that nobody tried
Quick-melee off a ledge (S2). Exploring new terrain with Ana for an hour (S1). G in any ult cast but Rally and Barrier (S3). A night fight, with skeletons and creepers (M5). Whip Shot on a mob with knockback resistance. A second player anywhere (S4). G during Kitsune Rush (M2). Sojourn's Overclock in third person after G (M4). The Nether and the End with any of this. Lifeline or Sleep through a portal. Water and lava. Autosave then crash (the record says so).

## Sources
Minecraft 26.3 decompiled source (`sys_src`). The wiki's Status effect page through the MediaWiki API, on overwatch.fandom.com, rev 380062 of 2026-08-10: its lists match the record's for our heroes. The cited rev 408792 I could not see: overwatch.wiki answered a Cloudflare 301 and I didn't chase it.

## Answered in code and play, 2026-11-04
S1 (`ThreadLocal`, concurrent maps; normal terrain played), S2 (`Combat.push`; quick melee and the rock's burst off a ledge credited, the sphere's pull by code), S3 and M3 (G ends everything, charge spent, ours and open for the owner), S4 (`Combat.send`, `motion`; **PvP unverified**), M1, M2, M4, M6, M7, M8 ([dva](../heroes/dva.md)), M5 ([hand_play](hand_play.md#a-fight-after-93-2026-11-04)), L1, L3, L5, L6, L7, L9, L10. Left: L2 (mobs' stale modifiers), L8 (spectator).
Corrected by play: the flail never pushed (`magic` is in `no_knockback`: 0.000 m on the pre-#93 build).

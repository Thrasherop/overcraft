# #140: the POTG recorder crashing the server after `bot remove` (2026-11-13, fix/potg-crash from next 009950b, 0.3.44)

Up: [harness](../../harness.md) · [POTG design, step 4](../../../potg/design.md#step-4-the-replay-engine-2026-11-11).

**Cause:** DevBot's `bot remove` takes the bot out with `PlayerList.remove` alone; the connection's `onDisconnect`
(where `PotgRecorder.disconnected` closes a player's recorder) never runs. The bot's recorder stays in
`ReplayPlayerRecorders` and `PotgRecorder`'s map and keeps ticking with no player. At its next Flashback chunk (5 minutes,
or at once if the bot had been in another dimension: `level` falls back to the overworld) `takeSnapshot` ->
`getPlayerOrThrow` throws and the integrated server stops. **Fix:** each tick, before the engine ticks, `PotgRecorder`
closes the recorder of anyone no longer in the player list (`finish`: kept only if a highlight is in it).
`bot fix 140 off` switches the sweep off in a dev build.

Setup: one dev client (`OC_ASSET_PACK` = the main checkout's pack), Overwatch and Battle.net checked closed, the lock
held; fresh flat creative worlds; me as Kiriko in hero mode; driven by `scripts/drive.py` (raw rows beside it).
`/tick sprint` stands in for the 5 minutes.

| Step | Expected | Seen |
|---|---|---|
| `repro` (world p140a), `bot fix 140 off`: `bot add b1 kiriko` / `bot remove b1` x3, then `potg disk` | | b1's recorder (f189a118) still recording, seg-1, 8 MB raw, after removal: the orphan |
| then `tick sprint 6200` | the crash | **server crash**, `IllegalStateException: Tried to get player before player joined` at `takeSnapshot` <- `FlashbackWriter.startNewReplayChunk` <- `tickAll` <- `PotgRecorder.tick`: the issue's trace ([crash_fix_off.txt](crash_fix_off.txt)) |
| `loop` (fresh launch, world p140b), fix on: b1 + b2 added and removed together x15, then b1 alone x10 (40 add/remove in all) | no crash; gone bots not recorded | both bots `seg null` (closed); mine recording seg-1 |
| `tick sprint 12500` (two snapshot rolls) | no crash; mine rolls | no crash; mine rolled seg-1 -> seg-3; the bots' closed raw segments tidied to 0 KB |
| b1 added again, `tick sprint 6200`, removed | records again, survives a snapshot, closes | b1 seg-26 -> seg-27 (its numbering kept), then `seg null`; mine seg-4 |
| `dragon`: survival, End, crystals killed, dragon health 24, the **real left button** held 40 s (`rk_potg.ps1 hold`), aim by server `tp` | the dragon dies, POTG plays | `the dragon died, killer Kiriko`; tick 199 card; `replay started for 1 viewer(s)`; `showing ended (replay done) after 21171 ms`. `p140_dragon_20.png`: Kiriko lined up; `p140_dragon_30.png`: "PLAY OF THE GAME KIRIKO" over the replayed End, dragon alive in it |

Log: no exception besides the dev environment's Realms/oshi noise. Screenshots in the worktree's `run/screenshots/`
(not committed: Overwatch model).

**Checked in code, not played:** a real disconnect (`ServerGamePacketListenerImpl.onDisconnect`: `PlayerList.remove`
then `super.onDisconnect`, our hook at its tail, same server tick) closes the recorder before any tick, as before; a
respawn swaps the entity under the same UUID within one packet's handling, and the recorder finds the player by UUID; a
dimension change snapshots with the player present. A join adds the player before the tick that starts the recorder.
**Not tried:** a real second client joining and leaving (one client only today); a fake-player mod other than DevBot.

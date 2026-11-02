# Hand play: playing it like a player

CLAUDE.md requires playing every change in the real game before claiming it. This is how that play works and what it
catches. Up: [Techniques](../README.md) · the tools: [harness](harness.md) · looking closely: [visual_review](visual_review.md)

## Catching what screenshots miss
- **Reproduce the owner's report first** (stuck on a step only when strafing; Alex in Minecraft mode), then show the fix
  with the same steps. Where possible, also show the fix switched off still fails (a dev toggle: `clothfloor off`,
  `emotef5 off`).
- **Bursts of screenshots** across an animation (0.05-0.3 s apart) and side-by-side sheets: one screenshot hides most
  animation bugs.
- **Step a frozen game a tick at a time to catch a one-frame glitch:** `cmd /tick freeze`, then `cmd /tick step 1`
  and a screenshot, 60-70 times (one animation cycle), and a thumbnail sheet of them all. The leg kick at the end of
  the breath clip lasted one tick in 64. Random screenshots missed it for hours; the stepped sheet showed it at once.
- **Freeze the game for fast effects** the same way: the effects run on the game clock, so they stop with it. The
  harness's `fx <effect>` plays one effect alone.
- **When a glitch shows up "at random" under different actions, look for something that runs underneath all of
  them.** The leg kick was blamed on three different clips; it was the idle breathing layer, broken every 3 s. Bursts
  of 40+ screenshots across a whole loop catch periodic glitches.
- **A dev readout beats screenshots for "is it even running?"** The coat's lowest point, stuck at its initial value,
  found the cloth clock bug in one try ([cloth](../animation/cloth.md)). A dev counter of bad frames found F5's
  one-frame flash. A per-tick fix can lose to the frame: fix it at the source.
- **A "feel" report (floaty, mushy) becomes numbers with a per-tick trace**: `trace start`, hold keys with
  `press key.keyboard.w down` / `up`, then `trace` (horizontal speed, vertical speed, height, on ground). Run the same
  script on the old build and the new one. For the camera, find the sky's edge in a few screenshots a frame apart: a
  still horizon in every frame means no bob.
- **Measure frame rate as part of play**:
  - `state` has `fps`;
  - `timing reset` then `timing` gives our milliseconds a frame;
  - `timing stride N` draws every Nth triangle.

  Scale the suspect (triangles, effects) and watch the frame time: it shows whether the cost is ours (CPU) or
  downstream (upload, GPU).
- **Suspect the test before the mod**: white pixel squares round a healed villager were Minecraft's death poofs from
  the villager the test script had just killed; burning "mobs" on the horizon were zombie horses in daylight.

## A second player
`gradlew runClient2` (a second dev client, harness port 25600), then `/publish false 25565` on the first and `connect
localhost:25565` on the second. See the hero as others do:
- third-person animation;
- the sounds others hear (subtitles on);
- effects;
- ally markers on a real player.

Only the client drawing the change needs a restart for client-side work. Server-side changes restart the host, and
the second client rejoins.

## Real input
With desktop control, when the owner approves it at the PC. Computer-use screenshots show the game as blank: capture
the screen with GDI instead (scratchpad `burst.ps1`, timed bursts for fast animations). It can't send F5 or two held
keys at once, and Windows' text-input overlay can block clicks.

To bring the dev client forward for real keys (2026-10-28): `(New-Object -ComObject WScript.Shell).AppActivate(<pid>)`
with its PID (found as for killing it). `open_application` and SDL's raise didn't get past Windows' focus lock, and
clicking the taskbar needs a separate grant. Then `hold_key` reaches the game; read the result with `dev state` and the
game's own screenshots. Mouse moves can't turn the view: SDL reads raw mouse input and the tool only places the
cursor. Turning with the real mouse still needs the owner.
- `hold_key` holds one key per call: no W with Space. Find a test one key can do (Lúcio's wall ride: Space held
  beside the wall, so he catches it square on and sets off at 0.3 s), and play the combinations through dev keys.
- After the owner types in Claude, Claude's window keeps the keyboard: `AppActivate` the game again before each key.
- "Textinputhost is in front" kept refusing keys even after `AppActivate` returned True, until the owner clicked the
  game by hand. The owner suspects the desktop tool's own front-window check rather than a real overlay; ask them to
  click the game rather than granting textinputhost.exe.
- Check the screen is clear before a key run (`dev state`'s "screen"): a menu (OverwatchMenuScreen) was open once
  and swallowed the first try.

## Test worlds
- **Summon test mobs** with `Tags:["x"]` and select `@e[tag=x]`.
- **While setting up,** use `/difficulty peaceful` (a slime killed the test once). Gamerules are snake_case in 26.3
  (`send_command_feedback`).
- **Damage tests:** a full hunger bar heals behind your back. Turn off `natural_health_regeneration`, or read health
  within a second of a hit.
- **Nights in a survival world swarm the harness,** which reacts a second late. Fight one summoned mob at a time
  (`/gamerule spawn_mobs false`); keep wild nights for the general play.
- **Zombies burn in daylight and iron golems kill them:** fight on a platform at sea, at night or with a helmeted
  zombie.
- **Superflat:** the player's feet at y -60 stand on blocks at y -61.
- **Build test floors with the floor first and the air above it second**, and only where the chunk is loaded (a fill
  "not loaded" does nothing, and she fell to her death). Clearing first drops the player a tick before the floor goes
  in. A floor in the sky (`fill` at y 119, standing at 120) makes a clean backdrop for frames.
- **A plain vanilla server** for "the mod stays invisible" checks: [minecraft_26](../minecraft/minecraft_26.md#a-plain-vanilla-server).

## Release play 0.3.29 (2026-11-02)
`next` at c62bc9a (#78, #79, #80 since 0.3.28), main checkout, `runClient2` ("Viewer", run dir `run2`, harness port
25600, `OC_ASSET_PACK` = the Ana checkout's pack). Overwatch and Battle.net checked not running first. Worlds: `rel329`
(flat survival: bed at -4 -60 -6, a NoAI 200-health zombie and a villager at 4 health; later a pool with a glass wall,
an ice patch and a lava pool at x 10-35) and `rel329n` (normal terrain, survival, a birch forest). Health read with
`data get`, speeds with `trace`. Screenshots in `run2/screenshots/`: `q_*` (heroes), `q80_*`, `k80_*`, `a80_*` (#80),
`m78_*` (#78), `n_*` (survival), `boat*`, and `sheet_*` (contact sheets of the same frames); each looked at.

**Every hero, every way in.** Kiriko from the select screen on joining (READY; HUD 250, kunai 15, ofuda 10, cooldown
icons: `q_kiriko_hud`); every other hero from H at the bed (`q_hmenu_*`); after each death back as the same hero in
hero mode; after Save and Leave and `openworld` (twice): the select screen with the last hero chosen and "ESC keep
hero", and Esc kept the hero and the mode the world was left in (`q_rejoin_select`, `q_rejoin2`).
- **Kiriko:** kunai at the zombie -4.72 body, -9.45 head; ofuda held 1 s on the villager 4 -> 13.6; Swift Step to the
  villager (8.6 m, 6.75 s cooldown); Suzu (12 s cooldown); Kitsune Rush (the torii gates: `q_kiriko_rush`). Killed by a
  superflat slime between scripts (a real mob), then by four zombies with health read every 0.1 s: 20, 17, 14 ... 0, a
  hit about every 1.5 s; back at the bed as Kiriko in hero mode, she killed the last zombie with kunai.
- **Lucio:** amp volleys -13.45, Soundwave -3.54; the aura 4 -> 14.9 -> 18.7 in 2 s; Amp It Up; Sound Barrier 839 ->
  637 over 2 s (`sheet_l2`).
- **Jetpack Cat:** Pawjectiles -10.7 to the zombie and 4 -> 11.7 to the villager; Lifeline on and off; Purr beside the
  villager 4 -> 8.8 -> 13.6 -> 20 (the first try was out of range: she had flown up fighting a slime); Catnapper's dive
  on the zombie (-1.92: `q_cat_nap_*`).
- **Mercy:** left beam heals 4 -> 10.6 in 1.5 s (gold), right beam no heal (blue); blaster -10.66 for 5 taps; Guardian
  Angel 8 m to the villager; Valkyrie. Resurrect not tried (no dead ally).
- **Lifeweaver:** Blossom 4 -> 11.2, Thorns -8.9, Petal Platform, the dash, Life Grip pulled the villager (and healed
  it 4 -> 10), Tree of Life (Q, then a left click to plant it): 350 of 250 and the villager full (`sheet_w2`).
- **Ana:** a shot heals 6 (75) and damages 5.59, scoped too; Sleep Dart (the zombie's Zzz); Biotic Grenade 4 -> 11.2;
  Nano Boost on the villager.
- **Sombra:** pistol -14 a second; Hotfix 4 -> 14.1; Cyberspace -7.6; Translocator 12 m; EMP -17.2.
- **Brigitte:** flail -3.6, barrier, Whip Shot -5.6, Repair Pack 4 -> 12.6, Rally 364 of 350. Repair Pack on a wolf
  tamed with one bone in Minecraft mode: 4 -> 11.4 (`wolf_healed`).
- **Juno:** a volley heals 5.76 and damages 8.7; torpedoes locked on the zombie and the villager: -11.7 and 4 -> 13.9;
  Hyper Ring; Orbital Ray 4 -> 18.3; Glide Boost on land 13.15 -> 7.70 m/s.

**#79 (Glide Boost in liquids):** in a 2-deep pool, swimming at 1.85 m/s, then Shift: 14.54 the next tick, 7.70 by
0.35 s, held level in the water (`q_juno_glide_inwater`). In lava (fire resistance on): 0.78 -> 3.47 -> 7.32 -> 7.70: her
mover runs, but the first ticks' boost above 7.7 is lost in lava (in water and on land it starts at 13-18). Minor; not
fixed.

**#80 (effects after translucent terrain):**
- Orbital Ray cast on the pool: the circle at -59.94 (the water's block top), on the water and over the bank in one
  piece (`q_juno_ray_water3p`).
- Every hero's effects over water, first person, nothing translucent in front (`sheet_a80_all1`, `sheet_a80_all2`,
  `sheet_k80`): ofuda and Suzu glows, Kitsune Rush's gates and decals on the water, Lucio's Soundwave and Sound Barrier,
  Pawjectiles, Mercy's gold and blue beams, Blossom, Thorns, Cyberspace, Brigitte's barrier and pack: all drawn over
  the water. On land at night (the hero pass above): as before.
- **Behind stained glass: hidden (not fixed; the owner's call).** Seen through a light-blue glass wall (third person,
  the camera behind the glass): Brigitte's barrier, Whip Shot, Repair Pack and Rally, Kiriko's Suzu and the torii gates
  are not drawn at all. With the barrier up and the glass removed, it's there; glass back, gone (`sheet_b80b`). The
  effects are now in the after-terrain phase and depth-tested against the glass (vanilla particles behave so without
  Improved Transparency); before #80 they drew first and the glass tinted them. Effects seen through water or ice are
  hidden the same way. To reproduce: `rel329`, stand at 15.5 -60 14.5 past the glass wall at z 12, F5 (back), raise the
  barrier.
- **Brigitte's barrier flickered at 10 fps (fixed).** At 10 fps (the window idle) her field was drawn in about half the
  frames (`sheet_b3`, `sheet_b4`: 3 of 6 front views), and in every frame with `fxslow 0.4` (`sheet_b5`). Not #80: an
  attached particle (`Fx.attach`) is held at `life x (1 - fadeOut)` (0.1 s of the barrier layers' 0.2 s) when fed each
  client tick, and a frame longer than that ended it in `simulate` before the next feed. `Fx.simulate` now keeps a
  particle lit while `attach` fed it in the last 0.1 s (`Particle.fedAt`, `ATTACH_FED`). Played after the fix: 6 of 6
  with the effect clock sped up four times (`fxslow 4` at 22 fps, an effective 0.18 s frame: `sheet_fix4`), 8 of 8 at
  16-23 fps (`sheet_lowfps`), 9 of 9 at 23-30 fps (`sheet_fix`); let go, the field is gone within 0.5 s.
- **Improved Transparency crashes the game (not fixed; found by accident).** F3+X (or the video option) with a hero:
  "Render type overcraft_hero_shadow_mark does not have OIT pipelines set up"
  (`run2/crash-reports/crash-2026-11-02_09.55.13-client.txt`). The shadow mark dates from 9982f02, and our blended
  effect types likely lack OIT pipelines too. The option is saved, so the game then crashes joining any world until
  `improvedTransparency:false` is set in options.txt (reset in run2).

**#78 (Minecraft mode first person)**, G off, with an iron pickaxe, cobblestone, bread, a bow and arrows, and a shield
in the off hand, every hero (`sheet_m78_<hero>`, `sheet_m78_six`): the pickaxe in her own right-of-view hand for Kiriko,
Mercy, Lifeweaver, Ana, Sombra, Brigitte and Juno; the shield in Kiriko's left hand and drawn Minecraft's way for the
others (no left hand in view); the swing turns the arm with the pickaxe; cobblestone placed from the hand (the count
went down); bread eaten, the bow drawn and fired (arrows down), the shield raised: Minecraft draws those, without arms,
as designed. Lucio's hand and item sit in the view's bottom-right corner, half under the HUD (as on #78's own
screenshots). Jetpack Cat uses Minecraft's arm by design. A pickaxe just selected sits low for its 0.8 s attack recharge
(vanilla's equip height: `m78c_*`). Using an item with an empty hand shows Minecraft's plain arm (only Kiriko has a
dressed one, `HeroArmSkin`). Mined a block of the flat world with the pickaxe (the window focused).

**Fire taps (0.3.28's held quick clicks):** `key <fire> tap` (down and up in one frame), 1 s apart: Kiriko 4 of 4 kunai,
Ana 4 of 4, Mercy's blaster 4 of 4, Sombra 5 rounds, Lifeweaver 6 thorns, Lucio 4 ammo a tap 4 of 4, Juno 5 of 5 volleys
at 1.1 s (at 0.6 s apart 3 of 5: her fire rate, not lost clicks).

**Minecraft-mode survival (`rel329n`, Juno, Minecraft mode, an empty inventory):** walked to a birch with W (no
auto-jump: a one-block step stops her, as in Minecraft); punched two trunks (4 logs, about 3 s a log); planks in the
inventory grid; a crafting table; at the table planks, sticks, a wooden pickaxe and a chest (`sheet_n_table`); dug down
with the pickaxe (dirt, then 7 cobblestone and 2 diorite at y 60); pillared out with dirt and cobblestone; placed
blocks; placed the chest, stored planks and sticks, closed it, and reopened it with them inside (`sheet_n_chest`). The
night (time 14000): 80 s in Minecraft mode (zombies 10 m off, none reached her), G to hero mode mid-night, about 88
volleys at zombies and skeletons, a zombie killed (the kill feed, "Monster Hunter"), killed by a zombie and then shot by
a skeleton; each respawn back as Juno in hero mode (`sheet_night`).

**The new case: boats** (a pond in `rel329n`). F (interact) boarded a boat in hero mode in 2 of 4 tries; both misses
were from under the water with the crosshair on the boat's underside (not called a bug). Rowing for 1 s: 4.60 m in hero
mode, 4.83 m in Minecraft mode; G switched modes while seated; LCtrl (crouch) got out in hero mode, Shift in Minecraft
mode.

Not ours: "Failed to save chunk" toasts in `rel329n` (Windows' "Insufficient system resources" on E:, as in 0.3.28's
play); superflat slimes (`spawn_monsters false` doesn't stop them) hurting heroes between scripts.

Not tried: a second client; ladders, the Nether; Mercy's Resurrect; perks; emotes and voice lines; the old build at
10 fps side by side (the flicker's cause is read from the code and shown with `fxslow`); effects through ice from below;
Improved Transparency on `main`.

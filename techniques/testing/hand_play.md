# Hand play: playing it like a player

CLAUDE.md requires playing every change in the real game before claiming it. This is how that play works and what it
catches. Up: [Techniques](../README.md) · the tools: [harness](harness.md) · looking closely: [visual_review](visual_review.md)

## A row in a table needs a trace with the thing in it (critique of #89, 2026-11-05)
Open the raw file before writing the row. A "before" of 2-8° at 21 m/s came from a trace with nobody on the rope; "0.5 m ahead of her" was the test's own 3 m offset shrinking; a "night fight" was four daylight shots of summoned mobs. If no trace has it, the row says not measured. And check where a number's label came from: an old guess read again is still a guess (L stays L until something is decoded).

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

## Release play 0.3.29, second pass (2026-11-02)
`next` at 3e304d4 (the fx-glass, voice-line and additive-convention merges since the first pass), main checkout,
`runClient2` (port 25600, `OC_ASSET_PACK` = the Ana checkout's pack), one client. Overwatch and Battle.net checked not
running first. Fresh worlds: `rel329b` (flat survival: spawn point and bed at -4 -60 -6; a NoAI 200-health zombie in an
iron helmet with fire resistance at 0.5 -60 10.5 and a NoAI villager at 4 health at 4.5 -60 10.5; walls across x -6..6:
light-blue glass at z 16, a water tank between glass panes at z 30-32, ice at z 46, stained glass at z 60; a pool at x
10-30 z 20-30 with ice and lava strips; an ice and a glass roof at y -58, z 90-115; a ladder at 50 -60 1; powered rail at
x 60) and `rel329bn` (normal terrain, survival, a taiga). Health with `data get`, speeds from `state`. Scripts in the
scratchpad's `rel329b`. Screenshots in `run2/screenshots/`: `b_*`, `b75_*` (animations), `bfx_*` (effects through
walls), `b_vl_*` (voice lines), `n_*` (survival), and `sheet_*` of them; each looked at.

**Every hero, every way in.** Kiriko from the select screen joining the new flat world (HUD 250, kunai 15, ofuda 10:
`b_kiriko_hud`); a superflat slime killed her before setup (the respawn showed the select screen, READY kept Kiriko);
every other hero from H at the spawn bed (`pick`, each `sheet_b_<hero>`); Lucio from the select screen joining
`rel329bn`. Brigitte killed by four zombies (health every 0.1 s: 20, 17.56 ... 0.12, 0, hits about 1.5 s apart: the death
screen, the kill feed "Zombie > Viewer"), respawned as Brigitte in hero mode at 20 (`sheet_b_death`). Save and Leave
then `openworld` three times: the select screen with the world's last hero (Kiriko, then Juno), Esc kept it and hero
mode.
- **Kiriko:** kunai body -4.49, head -9.29; ofuda held 1 s on the villager 4 -> 13.6; Swift Step 8.32 m (cooldown 6.35
  at 0.8 s); Suzu (13.4 at 0.6 s of 14); Kitsune Rush's gates 1P and 3P (`sheet_b_kiriko`).
- **Lucio:** amp volley -6.16 (a partial hit), Soundwave -3.28; the heal aura 4.32 -> 6.24 in 1.3 s, Amp It Up 6.24 ->
  10.46; crossfade (mode 1); Sound Barrier 18 -> overhealth (797-813 shown); wall ride on the glass wall both ways
  (`wallride_right` / `wallride_left`, rising to -57: `sheet_b_lucio_wall`, leaning into the wall).
- **Jetpack Cat:** Pawjectiles -10.13 to the zombie, 4 -> 9.76 to the villager; Lifeline on (mode 24) and off; Purr 4 ->
  6.4 -> 11.2; Catnapper's dive on the zombie -6.92.
- **Mercy:** gold beam 4 -> 11.26 in 1.5 s, blue beam no heal; blaster 5 taps -12.56; Guardian Angel 9.4 m to the
  villager; **Resurrect (first time in a release play):** a second villager killed beside her, E: it stood up again
  (cooldown 28.75: `b_mercy_res*`); Valkyrie (see the bug below).
- **Lifeweaver:** Blossom 4 -> 11.2, Thorns -4.08, Petal Platform out (one entity), the dash 9.58 m, Life Grip pulled the
  villager 10.94 m and healed it 4 -> 10, Tree of Life planted (left click) and the villager 4 -> 20 (`sheet_b_lw`).
- **Ana:** a shot heals 4 -> 10 and hits -5.11, scoped the same; Sleep Dart (cooldown 13.8); Biotic Grenade 4 -> 11.2;
  Nano Boost on the villager. Scoped, the view is the bare zoom (no scope overlay: known, `ana.md`).
- **Sombra:** pistol -8.84 in 1 s; Hotfix needs the right button held through its 0.5 s channel (a 0.15 s tap does
  nothing, as designed), held: 4 -> 15 -> 18, one charge spent; Cyberspace -4.9; Translocator 12.6 m; EMP -31.
- **Brigitte:** flail -3.6, barrier up (`idle_shield`), Whip Shot -5.6, Repair Pack 4 -> 13.78, Rally 28 (350 of 250).
- **Juno:** a volley heals 5.76 and hits -6.71; torpedoes locked and fired: zombie -6.69, villager 4 -> 14.16; Hyper Ring;
  Glide Boost 7.7 m/s; Orbital Ray 4 -> 19.64 (`sheet_b_juno`), on the pool its circle whole across water and bank
  (`sheet_b_juno_pool`).

**#75 animations** (`sheet_b75_<hero>_1p` / `_3p`: 3 idle frames 0.5 s apart, both fire buttons, melee and a jump with
its landing, 0.12-0.15 s apart, first person and third person front, each hero): nothing jumps, floats or collapses.
Lifeweaver 1P thorns, blossom and landing; Sombra's 1P burst; Mercy's blaster kick 1P and 3P (`sheet_b75_mercy_blaster`);
Jetpack Cat's melee 3P (`catc`: a paw swipe, face and knees in place); Kiriko's breathing (her left-wrist prop holder sits
under the floor either way, unseen); Juno's torpedo pods 3P float above both shoulders, mirror images; Lucio's wall ride
leans. The only odd frames are the clips themselves (Kiriko's 3P melee is a fast blur at 0.12 s).

**Effects behind glass, water and ice (e73fe79), Improved Transparency off** (`sheet_bfx_noit_<hero>_<key>`, the hero
2.5 m past the wall, the camera behind her on the near side, then in the open): Suzu, Kitsune Rush, Brigitte's barrier,
Sound Barrier, Purr, Petal Platform, Ana's grenade, Valkyrie, EMP and Orbital Ray all tinted by the glass, the water and
the glass panes, none gone. First person through the ice wall and the glass (`fx suzu_burst`, `fx rush_gate`:
`sheet_b_fx_ice`) and Kitsune Rush's gates seen up through an ice roof and a glass roof (`sheet_b_roof`): tinted, there.
With the camera under water (the rush cast inside the tank) the gates and talismans are there (`fxnear`) and drawn dim
blue through the water (`brw`). Orbital Ray on the pool, 1P and 3P: whole (`sheet_b_juno_pool`).

**Attached effects through slow frames:** Brigitte's barrier held with `fxslow 4`: 8 of 8 frames (`sheet_b_lowfps`, 4
live particles each), gone when let go. (At 75 fps: no real 10 fps run this pass.)

**Improved Transparency (955e13c): two bugs, both fixed in the working tree.**
- **It crashed (reproduced, fixed).** F3+X (the chat: "Improved transparency: enabled"), Save and Leave, `openworld`, the
  select screen, Esc, then the first third-person frame with the hero's body shadow: "Render pass color attachment count
  must match pipeline color target state count" in the solid pass (`run2/crash-reports/crash-2026-11-02_11.42.18-client.txt`;
  options.txt kept `improvedTransparency:true`). 955e13c's `HERO_SHADOW_PLAIN` was built from `ENTITY_SHADOW_SNIPPET`,
  which has no colour target, so Minecraft counted the type unblended and drew it in the solid pass. Fix: the
  pipeline gets `BlendFunction.TRANSLUCENT` as vanilla's `ENTITY_SHADOW` does
  ([rendering](../rendering/rendering.md)). After: joined with it saved on, the same step (Kiriko, third person past the
  glass wall: `b_it_kiriko3p`, her shadow drawn), then seven heroes, no crash.
- **Every additive effect drew as a dark box (reproduced, fixed).** With it on, each effect quad covered the scene with
  its black texels: Orbital Ray a black square with a ring, Suzu, the barrier and Purr dark boxes (`sheet_b_it_on`,
  `sheet_b_it_ray`). 955e13c gave the additive type `OIT_ENTITY_EMISSIVE`, an alpha-blended set. Fix: its own set with
  Minecraft's `OIT_ADDITIVE` define (`FxRender.ADDITIVE_OIT`). After (`sheet_b_it_on_fixed`): Suzu's dome, the barrier,
  Sound Barrier, Orbital Ray on the ground and through glass, Valkyrie's wings, Purr, EMP, all glowing over the scene,
  tinted through glass. Then F3+X off: the chat "disabled", options.txt `improvedTransparency:false`, and Orbital Ray as
  before with it off (`sheet_bfx_juno_ultimate`).
- Not checked: its look against the old build (it crashed), every hero's every effect with it on (seven heroes, one
  ability each).

**Mercy's Valkyrie flew at 22 m/s (reproduced, fixed).** Valkyrie cast, W held 1 s: 22.0 m/s forward, Space 20.4 m/s up
(the data's 9). `ClientMercy.launchedAt` starts at `Long.MIN_VALUE`, and `now - launchedAt` overflowed negative, so
until her first Guardian Angel launch she was always "just launched": the 0.5 s undamped window at x2.4444. Fix: the
start value is tested first (and `askedAt`'s too). After: 8.98 m/s forward, 8.97 up; a Guardian Angel launch inside
Valkyrie still peaks at 25.8 m/s ([mercy](../heroes/mercy.md)). Every Mercy in every earlier build had it until her
first launch of a session.

**#77 voice lines** (Esc -> Voice Lines; right click a line to hear it; a slot then a line; T held, left button, the
pointer up, T let go): Ana ("Mother knows best"), Sombra ("Did you mean to do that"), Brigitte ("Trust me"), Lifeweaver
("Sigh"): each tab lists the hero's lines, the preview plays it in the head (`heard`), the slot shows the new line, the
wheel shows the four slots with it on top, and saying it plays it with its subtitle ("Ana: Mother knows best.":
`sheet_b_vl_<hero>`).

**Minecraft-mode survival (`rel329bn`, Lucio from the select screen, G to Minecraft mode, an empty inventory):** a
first try aimed at a trunk below the hill she stood on dug two dirt blocks out from under her and she fell 11 blocks
into the hole (aim, not a bug); out again. Punched two spruce trunks (3 logs each, 4-6 s a log by the harness's polled
timing); in the inventory grid 3 logs -> 12 planks and 4 planks -> a crafting table (clicks, a right click a plank);
the table placed from the hand; at it sticks, a wooden pickaxe and a chest (`sheet_n_survival`); dug a staircase down
with the pickaxe (dirt, then stone: 9 cobblestone); took out three wall blocks and put cobblestone back from the hand
(3 of 3 placed, the count down); placed the chest, put the dirt and the sticks in it, closed it, read its contents
(14 dirt, 2 sticks), reopened it; climbed the stairs out with W and jumps. The night (13500): 45 s in Minecraft mode
(no monster came within 16 m: a taiga under spruces), G to hero mode, then wild monsters (two zombies summoned 12 m off
with their AI, a skeleton and a spider; caves round about held a creeper): Lucio's volleys (45), his aura healing him
between hits, a kill ("Monster Hunter"), and killed four times (zombies, a skeleton's arrows); each respawn back as
Lucio in hero mode (`sheet_n_night`). One respawn read 15.2 two seconds after: the zombies were at the spawn.

**The new cases: ladders and a minecart** (`rel329b`). A 10-high ladder: W held climbs it at 2.35 m/s in hero mode and
Minecraft mode alike (Kiriko and Brigitte, `sheet_b_ladder`); let go half way she slides down as in Minecraft; the
third-person pose on the ladder is the jump or fall clip (there's no climbing clip). A minecart on powered rail: F
(interact) boarded it in hero mode (Juno seated in it, `sheet_b_cart`), right click in Minecraft mode; W held 1.5 s
started it, 10.1 m in 2.3 s, the same in both modes (a 0.3 s W is too short for Minecraft's push in either); G while
seated switched modes; Ctrl (crouch) got out in hero mode, Shift in Minecraft mode, no damage.

`gradlew build` after the fixes: tests and `checkNoOverwatchAssets` pass.

Not tried: a second client; the Nether; a tamed wolf (the villager was every ally); perks; emotes; a real 10 fps run (the
window stayed at 30-75 fps); Improved Transparency with every effect; the view from under the pool (the teleport put the
camera in the bank).

## Leaving play: one hook, four doors (#100, 2026-11-04; second pass after [critique_systems](critique_systems.md))
- **The hook:** every hero leaves play in one place: `HeroManager.outOfPlay(player, state, why)` (G, death, a hero switch, leaving), which calls `HeroKit.leavePlay` before anything resets the state, then shares the flight permission and the synced looks. A new kit's
  states end there. Each kit's tick still ends what it does while `controls` is false (asleep, spectator): no substitute, since a dead hero's tick doesn't run, and a switch or a leave never reaches one.
- **The rule, ours and open for the owner (critique M3):** G ends whatever is running, for every hero, nothing resumes on G back, and charge already spent stays spent. So Overclock, Valkyrie and Kitsune Rush end on G; EMP, Orbital Ray and Tree casts are cancelled. An ability whose effect has gone out
  still costs its cooldown (Purr, a thrown ring, the whip). What has landed in the world (a ray, a tree, a platform) goes on through G and death, and goes on a switch or a leave. Through a death Kitsune Rush runs on (ours: the wiki doesn't say). G is one key from WASD and can cost a full ult: the owner should decide.
- **Saved modifiers are the trap:** `addOrReplacePermanentModifier` writes the speed into the player's save. Lifeline, Rally, Inspiring Strike, the Hyper Ring and overhealth's cap used it; all are transient now (only the hero's own health is saved), and joining takes off any stale `overcraft:` modifier an older build saved. Mobs aren't swept: an ally saved mid-buff under an older build keeps it.
- **Reproduced before the fix (keys through DevServer):** Sombra's stealth after G: see-through and 6.91 m/s (Minecraft's walk is 4.32) until its 7 s ran out. Lúcio after G: +25%, still renewed 3.5 s on. Jetpack Cat to Mercy with Lifeline on: +30% for good (5.61 m/s, a save carrying it). Kiriko's Swift Step cast, G inside its 0.2 s: she landed by her ally. Second pass (`sys_rep_old.txt`, build c5dcff9): Sombra's EMP pulsed on G back; a Tree
  sprouted at its old spot on G back; Juno's ray landed on G back; Kitsune Rush kept 0.13 (5.59 m/s) after G; Overclock's `sojourn.ult.loop` played on after G; Valkyrie resumed on G back (`mayfly` 1b); the whip thrown as G landed hit a zombie 5.6 health and threw it 5.7 m.
- **The critique was partly wrong the first time round:** Lifeline and the carry already ended on G and death (`JetpackCat.keepFlight`) and on a switch, except to Mercy, whom its guard skips. Read the running path, and reproduce.
- **Played after, same steps** (`speed` = `/attribute` value, base 0.1):
  - Sombra: G 0.1, walk 4.31, solid (`sys100_sombra_G`); switch, death, leave 0.1. EMP + G: no pulse on G back.
  - Lúcio: G: no song speed after its linger; a hurt villager gets no healing (hero mode +1.9 in 1 s, Minecraft mode 0 in 2 s). Amp It Up + G: cooldown 11.45 s.
  - Kiriko: G in the cast: stayed, 3 of 3; without G she lands. Rush + G: 0.1, walk 4.30.
  - Jetpack Cat: Lifeline on G, death, to Mercy, leave: 0.1, rope off; carry on G, death: phase 0. Purr + G: cooldown 11.5 s.
  - Ana: scoped 0.035, G 0.1 with the button held. Brigitte: Rally (0.115, 28 max) G, switch, leave, death: 0.1, 20 max; barrier 0.07, G 0.1; whip + G: zombie untouched, cooldown 2.4 s; flail + G: untouched.
  - Juno: glide G: ended, 5.25 s cooldown; ring thrown + G: cooldown 13.5 s; Q then G: nothing lands on G back, ult stays spent. Sojourn: slide G ended; Overclock + G: `sojourn.ult.end`, no loop. Sigma: Grasp G: cooldown starts; gather G: 0.1; Flux G: down unhurt.
  - Mercy: Resurrect cast G: 0.1, villager not back, cooldown runs; Valkyrie G, switch, leave: `mayfly` off; G back: idle. Lifeweaver: Tree cast + G: no tree on G back; platform stays on G, gone on a switch and a leave, and at the respawn after a death (`PetalPlatformEntity` ends it when its owner is removed: a player waiting on the death screen keeps it up).
  - A planted stale modifier (0.13): 0.1 on rejoin, hero health intact.
- **How it was played, honestly (critique M7):** "keys" here are `KeyMapping.set` through DevServer in the client, not the mouse and keyboard (desktop control was denied); aims are `look`. Hero deaths in these lists are `/kill @s`, except in the fight below. Kills on resurrected mobs are `/damage ... by @s`. `overcraft reset` and `giveult` refill. Good for measuring; not what CLAUDE.md calls playing as a player: the owner's hands haven't been on this.
- **What a critic should check:** a second player (one client only: the machine crashed under memory): the towed ally's rope ending, a watcher seeing Sombra appear, Valkyrie's and Overclock's looks on another client. Falls: G in mid-air (Valkyrie, Flux, Jetpack) is Minecraft mode, so the fall hurts as Minecraft's. Sigma's `grasp_hold` plays out up to 2.2 s after G in third person (the data has no end clip; the same on any early end). Sombra's beacon stays through a death: Overwatch's rule not checked. Another Kiriko's Rush or Lúcio's song on an ally in Minecraft mode still applies: effects on others. Spectator goes through tick branches only.

## A fight after #93 (2026-11-04)
The change alters every fight: mobs no longer stagger. So: normal terrain at midnight, spawned 12-16 m round the hero: 3 zombies, 2 skeletons, 1 creeper, all with AI, two rounds each. A script aims at the nearest mob and holds fire (`look`, DevServer keys: not a player; no abilities, no strafing, so this is a floor). Numbers below, `sys_fight_<build>.txt` in the session's scratchpad.

| Two rounds each | pre-#93 (9f1c001): cleared, deaths, lowest health | now: cleared, deaths, lowest health |
|---|---|---|
| Kiriko (20) | 0 of 2, 0, 20 (never reached: mobs left 2) | 1 of 2 (13.7 s), 1, 0 |
| Sombra (18) | 0 of 2, 1, 0 | 0 of 2, 2, 0 |
| Brigitte (20) | 1 of 2 (20.6 s), 0, 2.2 | 0 of 2, 0, 6.5 |
| Juno (18) | 0 of 2, 1, 0 | 1 of 2 (31.6 s), 1, 0 |
| Sigma (48) | 0 of 2, 0, 28.5 | 0 of 2, 1, 0 |
| **All ten rounds** | **1 cleared, 2 deaths** | **2 cleared, 5 deaths** |

- **Read it as a direction, not a measurement:** ten rounds, different terrain each build (three pre-#93 rounds had mobs that never reached the hero at all, 6 left and no hit taken), and the script doesn't strafe, retreat or use an ability.
- **What it shows:** first bites land at about the same time (2-5 s), but after #93 they keep coming: zombies aren't thrown back between hits, and the rounds with most hits are all after it (Brigitte: 14+ hits in 15 s; before, 5-7). Shots no longer miss a mob they threw aside, so clears are quicker where the hero lives (Kiriko 13.7 s).
- **The creeper is what kills:** a hit of 12-17 Minecraft health on the small heroes and 18.8-28.7 on Sigma (Minecraft's blast; a full-health 250 HP hero is 20), and nothing keeps it off now. Most deaths have one such hit in them. Before #93 a kunai pushed it 2 m each.

Open for the owner: whether melee mobs reaching heroes sooner is right. Overwatch's rule says yes (no stagger); Minecraft's mobs weren't built for it.

## Respawning at 14-16 of 20 (#111, 2026-11-04)
Up: [Techniques](../README.md) · [harness](harness.md). **No bug found in the respawn; reported, nothing changed.** Mobs at the spawn reproduce the number; that isn't proof it was that sighting (critique_rules).
- **Tried, reading health every 0.2-0.25 s after each respawn, about 130 respawns (the first three lists on the old build, the states and modes on the new):** `/kill` five times each on all eleven heroes on next; a zombie's kill far from spawn; fall, lava, void, magic, a creeper's blast and burning, twice each, on Kiriko, Brigitte (armor) and Lifeweaver (shields); dying after Rally's +100, in Sound Barrier, in Nano; four in Minecraft mode; six hurt to 5 first; from the Nether. **Full every time**, at once. Food stayed 20.
- **The one way to see 14-16:** mobs left at the spawn. Two zombies and a skeleton beside it: 20 for 3-5 s, then 20 -> 17 -> 14 -> 12 (Normal's zombie 3): the issue's number, and read a few seconds after a respawn it looks like coming back hurt. The same trap is in [harness](harness.md) from 2026-10-28.
- **Open for the owner:** Overwatch has spawn rooms; Minecraft has only the wait while a client loads. Whether a hero should get a moment of protection on respawn is a design call, not done.
- **What a critic should check:** the session of the original sighting can't be repeated (its setup isn't recorded). Not tried: a death to another hero's blow (the bot can't fire), Mercy's resurrect, a hero change at the respawn, Save and Leave mid-death, a real second player, easy difficulty.

## A survival session (critique_rules 3, 2026-11-05)
Up: [Techniques](../README.md) · the rules: [allies](../minecraft/allies.md). A new world on normal terrain, survival, the final build. **Input: DevServer's `KeyMapping.set`; aim by `look` or `/execute ... facing`; places reached by `/locate` and `/tp`; mobs added by `/summon` where said. Not a real mouse, and no one's judgement of feel.** The owner still has to play it.
- **Hunt:** a wandering cow by Kiriko's kunai: dead in 5, 2 items on the ground (`rl2_new_surv_hunt`). Ult 16 -> 343 in that time: a first-time advancement's share (`Combat.onAdvancement`), not the cow; on flat ground, with advancements done: +39 to +84 in 7-15 s, the passive 5 a second.
- **A night, Hard:** 35 s each of Kiriko, Lúcio and Sigma, with six zombies, a skeleton, a creeper and a spider summoned around them and natural spawns, moving, G once: deaths 5, 3 and 2. No crash. Respawn health read 0.8 s after each: full, but one **15.5 of 20** with mobs at the spawn: [#111](#respawning-at-14-16-of-20-111-2026-11-04)'s number, again.
- **A village:** plains, 9 villagers, 1 iron golem. Mediblaster at the calm golem: 100 -> 100. Punched in Minecraft mode (the attack key): enemy; she went 18 -> 3.5 -> 0 in about 2 s (`rl2_new_surv_golem`). **Not tried:** turning it by low reputation; shooting back on normal terrain before it killed her (done on flat ground: 99 -> 94.2).
- **A bastion:** 22 piglins, 7 brutes. In gold, a calm piglin 16 health: Soundwave 16 -> 16, still a bystander; but the picture (`rl2_new_surv_piglin_gold`) shows her facing a wall, so this read proves little: the clean case is on flat ground (a zombified piglin and an enderman, no wave damage; [allies](../minecraft/allies.md)). On the earlier world's run, the wave hit a brute and the group angered (Minecraft's alert): calm only holds while nothing enemy is in the wave. Out of gold, 3 s later: still calm in that one read (no line of sight, not looked into).
- **Not played:** the other eight heroes in survival; a farm; bees at a hive, a wolf pack, an ocean monument, a mountain goat; water, lava, boats and ladders with Catnapper; two real clients; an Overwatch side by side for a stun or a barrier's stop.

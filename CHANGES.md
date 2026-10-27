# Overcraft: changes by day

A day-by-day log of development: releases, features, bugs fixed, investigations, and what's waiting on the owner.
Newest day first. Issue numbers are on https://github.com/Thrasherop/overcraft/issues. From 0.2.0 on, every build
handed out has its own version (`gradle.properties`), shown in the pause menu next to the asset pack's.

## 2026-10-27

### After 0.3.7 (on `feature/kiriko`)
- **Hero chat's team channel icon** (round 8 #5's rest, the owner's frame `o2`): a team line (callouts, "Too many
  messages!") starts with a three-person icon in team blue, its text after it; wrapped lines start under the text,
  as Overwatch's do, where Minecraft indented them one space. The icon is ours (`tools/gen_chat_icons.py`, in the
  jar's `font/chat_icons.json`): Overwatch's isn't in the UI texture dump (searched by shape against `o2`).
  Measured at 1080p against `o2`: the icon 19 px wide (theirs 19), 12 tall (13), 2-3 px right of theirs; the text at
  x 89-90 (90). The first try drew the icon as boxes: the hero chat face was forced onto every glyph
  (ChatLinesMixin now replaces only the default font). Played: five T taps and a long `/say` in a fresh world
  (`chaticon_1080`, `chaticon2_1080`, `chaticon3_1080`); plain orange lines wrap flush, no icon.
- TECHNIQUES.md: tonight's lessons (emote clip chaining, Fabric's info bar halves, outlines for custom geometry,
  glyph fonts in chat, harness buttons and timing).

### Build 0.3.7 (the communication wheel, pings, emotes; Kiriko's HUD to the owner's frames)
Needs the asset pack rebuilt (`build_asset_pack.py sounds`, `fonts`, and Kiriko's model with her emotes:
`tools/owm_survey.py ... --glb --keep=tools/clipmaps/kiriko.txt --emotes=<extracted>/Unlocks/Heroes/Kiriko/Emote`,
after DataTool's `extract-unlocks <out>/Unlocks "Kiriko|voiceline=*"` and `"Kiriko|emote=*"`).
- **The communication wheel (#3), for every hero** (`dev.overcraft.comms`, `tools/reference/comms-plan.md`, the
  owner's spec and screenshots in `comms-reference.md`):
  - Hold T: the view dims, eight callouts round a ring (NO, HELLO, NEED HEALING/HELP, GROUP UP, ULTIMATE STATUS, NEED
    HELP, SORRY, THANKS), the VOICE LINES / EMOTES tabs at the sides. The mouse moves a pointer, not the view; release
    T to play the callout under it, nothing from the centre. A quick tap of T: "Good job".
  - A callout: a team-blue chat line ("Kiriko (Kiriko): I need healing!") to teammates at any distance, and the
    hero's own line in the world (her set: No, Hello, Healing, Group Up, Assistance Required, Sorry, Thanks; the
    ult's Charging / Almost Ready / Ready with the charge in chat). Four in 8 s, then "Too many messages!".
  - In hero mode chat moves to Enter (Overwatch's), T being the wheel's.
- **Voice lines (#5):** DataTool's voice-line unlocks by name (41 for Kiriko, "Tarot Reading" with all its cards);
  the wheel's voice page (left click while holding T): the four equipped at the compass points; VOICE LINES in the
  Overwatch menu equips them (right click: hear one).
- **Emotes (#4):** her 23 emotes from DataTool's emote unlocks, exported onto her model; the wheel's emote page (right
  click); the same screen's Emotes tab equips four. The view goes to third person meanwhile. Their props (the bike,
  the skateboard...) aren't in yet.
  - How they play is found from the clips' poses (`EmotePlan`): a one-shot (Cheer, Laugh, Heroic, Thanks...) starts
    and ends in her stance, plays once, and she's back in first person; a dance or a kneel goes in, then loops its
    loop until she moves or acts. The first try played every clip of an emote in id order, the last looping: that
    order holds Overwatch's additive layers and the prop's own clips, so after Cheer she sank to the waist with her
    arms out, for good (found playing on LAN: the second player saw it too).
- **Heroes get outlines:** a hero pinged as an enemy got the red ring but no red outline (found on LAN: our hero models
  are custom geometry, which has no outline of its own), and the Glowing effect or a spectator's view didn't outline
  them either. Their shape now goes into Minecraft's outline pass as vanilla's dragon fireball does.
- The Emotes tab's hint no longer offers "right click: hear it", and "ISN'T THIS FUN" has its apostrophe back.
- **Minecraft's locator bar is hidden in hero mode** with the rest of its bars: each other player's dot (a pink or
  team-coloured square beside the ult meter, found on LAN) came from the info bar's foreground, which Fabric names
  apart from the bar.
- **Played on LAN with a second client** (before the fixes above, then again after): callouts reach teammates and
  not the other team, both ways (Red vs no team, then both teamless); pings and emotes seen by the other player; an
  enemy ping on a player (ring, portrait, "KIRIKO", now the outline); Cheer, Heroic, Choreography, Orion Dance and
  Kneeling watched from the other client (`fix_v_*`, `out_*`).
- **General play of `next` before main** (fresh client, fresh survival world, savanna, Normal; `gp_*`): picked
  Kiriko on the select screen; at night zombies hit 3 a hit and skeleton arrows 2 to 4, the night swarm killed her
  twice (death screen, respawn with the kit and ult kept); kunai 60 a hit, quick melee 40 (39 through a zombie's
  armour), reload 11 to 15; Protection Suzu heals her 80 at her feet and forward (two misses at first were the
  harness turning the view and throwing in the same tick: the server threw at the old pitch); Healing Ofuda on a
  hurt villager 10 to 19.6; Swift Step to it (7 s cooldown); Kitsune Rush cast with its torii gates; wall climb up
  a 7-high wall, cancelling a reload (ammo stayed 10); swimming; a minor perk picked with the click throwing
  nothing; T tap "Good job", a ping; Save and Leave, rejoin through the select screen. Minecraft mode (G): punched
  acacia logs and dirt by hand, crafted planks, a crafting table and a chest by clicking the grids, placed them,
  pillared out of a hole with dirt, stored logs in the chest and found them there on reopening; G back to hero
  mode; H away from a bed: "Go back to your bed to change heroes".
  Not tried this time: a tamed wolf as the ally, lava, ladders, boats, the Nether, eating. Seen once and not
  reproduced: 112 HP lost between leaving a pool and opening the perk menu (no damage message; the same steps
  again lost nothing). Found: vanilla's saturation regeneration and hunger still run in hero mode with the food bar
  hidden (#43).
- **Pings (#40):** the middle button pings the spot under the crosshair (a blue diamond, its distance, your own with
  a leader line to "CANCEL [key]"); a double click makes it a warning (a red circle); an enemy gets a red ring that
  follows them with their portrait (a mob: its initial) and name, and a red outline. Teammates see them. Aiming at
  your own and pressing again takes it down.
- **Kiriko's HUD to the owner's own Overwatch frames** (critique round 8, `critique-2026-10-27-round8.md`): the health
  block's position, pips, "+" box, level box and plate; the portrait's glowing border; the weapon panels; the mouse
  icon; the ult meter's inner ring and number; text without offset shadows; the volley bar; the ability icons.
  Hero chat in Overwatch's chat face (BlizzardGlobal), wrapping as Overwatch's does. The perk-waiting state is
  Overwatch's orange "[LALT] SELECT PERK" bar.
- **#9 closed:** death, deep water, the climb's top, the ability clips all play.
- Played through the dev harness (captures `wheel_*`, `loadout_*`, `ping_*`, `r8after_*`, `chatwrap2`). Not yet:
  real mouse and keys, a second player receiving callouts and pings, the first-person hands (round 8 #1: waiting on
  the owner's screenshot with the default weapons).

### Build 0.3.6 (the owner's night list, Kiriko first)
- **Suzu: no gold ring on the ground (#22).** The owner: "the golden ring *shouldn't* be in this, since there is not
  that in ow". The game data has a 5 s gold ring decal (6089) but Overwatch shows none in play; gone. The burst itself
  stays (the white-blue wave on the ground, the flower, the glow on those it touches).
  - Before (`suzu_ring_old_1s`, `_3s`): a gold ring round the villager at 1 s and 3 s. After (`suzu_new_*`,
    `suzu_frames`, frozen and stepped 2 ticks at a time): the burst, then nothing on the ground.
- **Perks also come passively, a point a second** (the owner: "1 point a second if nothing is being done. But
  naturally, killing should be much much faster"): a zombie's 250 points are four minutes of it. Played: 20.2 points
  in 20 s standing still.
- **Changing teams** (the owner: "a good easy way for players to change teams" and a game rule for non-operators,
  on by default):
  - CHANGE TEAM in the Overwatch menu: No team (everyone together), Blue, Red (made on first use, friendly fire off,
    as in Overwatch) and any other scoreboard team; yours marked. `/overcraft team join <team>` / `leave` too.
  - `/gamerule overcraft:allow_team_change` (default true): off, only operators may change teams.
  - `/overcraft`'s other commands are still for operators only.
  - Played: the menu and picker (`team_menu`, `team_screen`), Red then Blue by menu and command (the scoreboard
    agrees). A second client (Viewer, not an operator, LAN without commands): joined Blue with the rule on;
    `/overcraft reset` refused; with the rule off, both the command and the menu refused with the explanation in chat
    (`viewer_team_refused`).
- **A wall climb interrupts the kunai reload (#42, the owner).** Reproduced on 0.3.5: 3 kunai, R, a climb 0.1 s later:
  the ammo came back (12 to 15) partway up the wall, the reload clip playing through the climb. Now the climb stops
  it (the clip fades out, `reload_cancel`): 12 stay 12; an empty clip starts reloading again at the top (0 to 15
  0.85 s after).
- Pings, the communication wheel, voice lines, emotes and sprays: the owner's spec and screenshots are written up in
  `tools/reference/comms-reference.md` (#3, #4, #5, #40 pings, #41 sprays).

## 2026-10-26

### Build 0.3.5
Works with asset pack 0.3.4 (the perk icons are already in it).
- **Perks (#36, the owner: "The charge bar for perks aren't working. like it isn't charging").** The blue line under
  the name is Overwatch's perk level bar; ours was drawn at a fixed length and perks didn't exist. Now Overwatch's
  perk system (`tools/reference/perks.md` has the sources):
  - Progress: a point per point of damage dealt or healing given, with the ult charge's limits (none during Kitsune
    Rush, none for the Medic self-heal), none passively; the rate ramps from 3 minutes of play as the hero to double
    at 7 (the wiki).
  - Level 2 at 1900, level 3 at 4800 (the wiki's numbers for Kiriko; from the start, ours).
  - Hold Left Alt while a perk waits: the two choices, picked with the left or right mouse button (Overwatch's
    keys; a new "Choose a perk" hero control). A pick is for good; the click that picks fires nothing.
  - The HUD: the box at the bar's start shows the level; the bar fills blue toward the next level, goes full and
    orange with a "PERK READY" tab while a perk waits, and a chime plays at each level (ours). Picked perks show as
    white discs above the weapon panels.
  - Kept through death, leaving and rejoining, and per hero (swapping back restores them, as in Overwatch).
  - `/overcraft perks [players] [level]` (operators) sets the level with nothing picked, for testing.
  - Kiriko's perks: Urgent Care (ofuda 50% faster to allies below half health), Fortune Teller (kunai hits send 2
    ofuda to an ally in front), Ready Step (Swift Step: +40% attack and reload speed for 3 s), Foxtrot (Suzu: allies
    +30% speed for 4 s).
- **Hero-mode chat is orange**, like Overwatch's match chat (the owner). Text with a colour of its own keeps it (red
  errors); Minecraft mode's chat is unchanged.
- **Played** (a fresh flat survival world, through the dev harness's input: key presses and mouse buttons through
  Minecraft's own handlers; screenshots `perk_*`, `chat_*`, `mc_*`, `rejoin_*`):
  - Reproduced on 0.3.4 first: kunai volleys took the ult charge from 11 to 1263 and the line didn't move
    (`perk_old_before/after`).
  - Kiriko picked on the select screen. Husks killed with kunai: the bar at about 40% after 3 (750 of 1900).
  - A slime killed her mid-run; after the respawn the bar was where it was.
  - Level 2 by kills: orange bar, "2", PERK READY. Alt showed the minor perks.
    - The first pick by left click also threw a kunai (13 to 12): the menu closed under the held button, which
      then fired. Fixed; the next pick (right button) left the ofuda at 10.
    - The cards sat on the ofuda counter: moved up above the crosshair.
  - Urgent Care: the first ofuda to a villager at 4 of 20 health flew at 1.348 blocks a tick (27.0 m/s); to one at
    full health 0.899 (18.0).
  - Level 3 by kills. Save and Leave, a restarted client, rejoin: level 3, Urgent Care kept, the major perk waiting.
  - Foxtrot (right click): the villager's speed 0.5 to 0.65, back to 0.5 by 4.8 s; Kiriko's own unchanged.
  - `/overcraft perks @s 3`, then Fortune Teller and Ready Step through the menu.
    - Fortune Teller: one kunai on a husk healed the villager behind it from 4 to 5.92 (two ofuda of 12); her ofuda
      ammo stayed 10.
    - Ready Step: kunai held 2.5 s threw 6 without a step, 8 right after Swift Step (one per 0.36 s).
  - G to Minecraft mode: plain HUD, Alt does nothing; placed 2 dirt (8 to 6), placed and opened a chest, opened the
    inventory; chat there stays white. Hero-mode chat orange, idle and while typing; a red line stays red.
  - Not tried: real mouse and keys (all of this went through the harness), mining by holding the button (Minecraft
    only keeps mining while the window has the mouse, which it didn't), crafting, a second player, swapping heroes
    (only Kiriko is playable), the ramp in play (unit-tested), the chime by ear.

### Build 0.3.4
Its asset pack must be rebuilt (`py tools/build_asset_pack.py ui "E:\Overwatch Archive\extracted\154088"`, then
`zip`) for Overwatch's melee fist in the kill feed. An older pack shows our own fist there instead.
- **Kill feed icons (#33, the owner's call):**
  - Her weapon fire shows no icon, only the ">". (Kiriko's kunai are her weapon's secondary fire in Overwatch; her
    primary, the ofuda, can't kill.)
  - Abilities show their icon. Melee shows Overwatch's Quick Melee fist (UI texture `E613`, now in the pack).
  - A headshot adds a small red target after the icon, in the crit hit marker's red. The wiki's Elimination Feed
    page describes Overwatch's as "a small red target". It isn't among the UI textures extracted so far, so this
    one is ours.
  - A mob's kill still shows a fist, a death with no killer a skull.
  - Played with tagged husks and real throws: a body-shot kunai kill, a headshot kill, a melee kill, a husk killing
    her, and /kill (`r15_new3_*`, `r15_new2_*`).
- **Found on the way:** a killing headshot was marked only after its damage, so the kill feed and Voice's "headshot
  elimination" line never saw it. The kunai now marks the hit first.
- **Kunai and Suzu hit things the way arrows do (#34):**
  - End crystals: the kunai hurts anything that isn't a creature with its damage, so boats and minecarts take damage
    and paintings break too. Suzu hits them for no damage, as a snowball does, and bursts there.
  - Both are in Minecraft's impact_projectiles tag now, so they break chorus flowers and decorated pots, as arrows
    and snowballs do.
  - Both press wooden buttons. A button has no collision box, so the hit lands on the block behind it; the button
    in front of the face hit is pressed when the hit point is on it.
  - Targets, bells and the rest already reacted: both projectiles pass block hits on to Minecraft.
  - Played, the same lanes before and after the change: an end crystal, a wooden button, a target block, a
    decorated pot and a chorus flower, each 5.5 m ahead.
    - Before: nothing happened, except that the kunai powered the target.
    - After, for both kunai and Suzu: the crystal exploded, the button pressed, the target powered, and the pot and
      the flower broke.
    - A stone button stays unpressed by a kunai, as by an arrow.
  - Not tried: boats, minecarts, item frames, bells.
- **Fall damage in hero mode again, from the hero's own safe height (#35, the owner's call):**
  - Kiriko's safe height is her climb height, 8.3 m (Minecraft's is 3 blocks). A fall counts from the highest point
    reached, so climbing a wall starts it again, as a ladder does.
  - An ability can make a fall safe: the hook for Winston's leap and Juno's glide later; Kiriko has none.
  - CLAUDE.md is updated.
  - Played in a fresh flat world, with natural regeneration off, walking off stone pillars:

    | Fall | Damage expected | Health after |
    |---|---|---|
    | Minecraft mode, 10 blocks | 7 (Minecraft's) | 13 |
    | Hero mode, 8 blocks | none | 20 |
    | Hero mode, 12 blocks | 3 | 17 |
    | Hero mode, 20 blocks | 11 | 9 |

    - She caught a wall after a 3.1 m fall and climbed to a top 17.3 m up. She took 8 damage, counted from the top;
      the two falls added up would have been 12.
    - Swift Step to a villager after 11.7 m of fall: unhurt (the teleport ends the fall).
    - 30 m with no catch: she died, as Minecraft's 23-block fall kills Steve.
  - The old build took no damage from 20 blocks in hero mode.

### Build 0.3.3
Its asset pack must be rebuilt (`py tools/build_asset_pack.py sounds "E:\Overwatch Archive\extracted\154088"`, then
`zip`). An older pack still works, with a silent Victory.
- **Victory sounds (#18):** the Ender Dragon's death now plays Overwatch's own. The match announcer says "Victory."
  (voice set 001D, file A8C8), over the match-end music's victory sting (graph 0D3B, chain B's fallback: 2B0DE6,
  8.76 s). Both were identified in tools/reference/sounds-round8.md and are now in the sound map. They play in the
  head, in stereo.
  - Played: in the End, a dragon at 3 health hovering 14 m away, kunai until it died. `heard` lists
    `announcer/victory/0` and `music/victory/0`, both in the head, as VICTORY comes up (`r13_victory`). The kill feed
    shows "ELIMINATED ENDER DRAGON". The pause menu reads "0.3.3 · ASSETS 0.3.3". Not judged: the two sounds'
    balance against each other (played at Overwatch's file levels).

### Build 0.3.2
Same asset pack as 0.3.1.
- **Movement feels like Overwatch's, not floaty (#31).** Research (tools/reference/movement.md) found the jump
  already right and everything around it slow or Minecraft's. Overwatch's own mover (each hero's CharacterMover in the
  game data, read offline; its numbers agree with the Workshop measurements) now drives hero mode, three steps a tick:
  - On the ground: friction 15/s on at least 4.5 m/s, acceleration 35 x the input speed. Full speed in one tick (was
    two), a stop in about two, a strafe reversed in one (was four: ADAD felt mushy).
  - In the air: 3.5 x the input speed (19 m/s² at a run, was 12). Input within 90° of the velocity turns it without
    slowing it (Geoff Goodman's 2018 description; a 90° turn takes 0.45 s), input against it brakes: holding back
    after take-off now turns the jump around before landing. Speed above the run speed (buffs included) decays by
    2/s in the air.
  - Falling follows the wiki's measured curve (v = 30 tanh(17.5 t / 30)): 15.8 m/s after 1 s (was 13.3); a 20-block
    drop lands in 1.63 s (was about 1.75).
  - The camera no longer bobs and rolls while running, and the view no longer widens with speed (Kitsune Rush zoomed
    it out 15%). Both only in hero mode; Minecraft mode keeps the player's View Bobbing and FOV Effects.
  - Played (harness keys through the keyboard handler, a per-tick `trace`, old build against new): from rest to 5.5
    in 1 tick (was 2); stop 3.0, 0.25, 0; ADAD reversal 1 tick each way (was 4); W then S after take-off reverses
    in the air (old: slowed 0.6 m/s a tick, never reversed); A instead of W after take-off turns 90° at 5.49-5.50 m/s,
    W+A turns 45° the same way; a standing jump then W reaches 5.5 in 0.3 s; jump apex still 0.98 m; falling 15.85
    m/s at 1 s (old 14.8 at 1 s from rest). The horizon stays on the same rows in six frames of a run (old: moved
    and tilted 1-4 px at 854x480: `r12_run_*`, `r12n_run_*`); in Kitsune Rush a house right of centre stays at 180 px
    from the centre (old: 148 px, `r12_fov_*`). The course: into a wall and along it (stops, slides at 5.3 m/s with
    W+D), the 1-block ledge (still lifted on), four oak stairs and a slab step at full speed, water and a ladder
    (Minecraft's own, unchanged), the wall climb (8.3 m/s for 10 ticks, over the top), crouch 3.0 m/s, a zombie's
    hits (20 to 15 health, knockback 4.4 m/s carried in the air, stopped in two ticks on landing), TNT throwing her
    at 10 m/s (decays to 5.7 over 1.6 s in the air), 7.15 m/s in Kitsune Rush, and Minecraft mode (G off: sprint 5.6
    m/s and the sprint-jump boost as before).
  - Not done: the 20 Hz tick's latency (the camera drawn a tick behind; an experiment for later) and crouch stopping
    at ledges like Minecraft's sneak. Not tried with a second player.
- Dev harness: `trace start` / `trace` (per tick: speeds, height, on ground) and `press <key> down|up` (hold a key
  through the keyboard handler).

### Build 0.3.1
Its asset pack must be rebuilt: Kiriko's model gains two clips (re-export her with `tools/owm_survey.py ... --glb
--keep=tools/clipmaps/kiriko.txt`, copy it to the pack as `hero_models/kiriko.glb`, then `zip`). An older pack still
works, with her plain standing pose on the select screen.
- **Hero select pose (#29):** Overwatch's own hero select animation, from her showcase entity in the game data:
  `B5DE`, the intro (2.43 s), plays once when the screen opens; then `B9E2`, the hold (3.87 s, only breathing), loops.
  Both are in `tools/clipmaps/kiriko.txt` as `select_intro` and `select_hold`.
- **She looks straight ahead on the select screen (#30):** not at the mouse, and not tilted by the player's pitch.
  - Played: opened hero select with H after respawning; screenshots at 0.3, 1.0, 1.8, 2.6, 4.0 and 6.0 s show the
    intro (kunai raised, hands together) then the hold (ofuda up by her face), with 2.6 s and 6.0 s the same pose
    (`r12_sel_*`). Closed and reopened: the intro plays again (`r12_reopen_0.3`). Through the pause menu's Change
    Hero: intro at 0.4 s, hold at 3.4 s (`r12_change_*`). In-game third person keeps her usual stance (`r12_3p`).
    The pause menu reads "0.3.1 · ASSETS 0.3.1", not flagged (`r12_pause`). Not tried: the select screen on a first
    join, and switching between two heroes (only Kiriko is playable yet).

### Build 0.3.0
Same asset pack as 0.2.0.
- **Hero mode by the G key alone (#26, the owner's call).** The hotbar no longer decides it: hero mode is a flag G
  switches, saved with the player through death and rejoining. Picking a hero turns it on. In hero mode the hotbar
  keys and scroll do nothing (there's no hotbar there), and the hero weapon item that held slot 1 is gone
  (removed from old inventories). CLAUDE.md's rule is updated.
  - Played: picked Kiriko (hero mode on, slot unchanged); G off and on with the real keys; key 4 in Minecraft mode
    (slot 4, still Minecraft mode); key 7 in hero mode (no change); death and respawn (still hero mode); no weapon
    item left in the inventory (`r10_pair`).
- **Kunai ammo returns at 65% of the reload (#28):** 0.845 s, Blizzard's patch note (May 9, 2023), not the wiki
  infobox's 0.65 s. A throw could cut the reload short 0.2 s too early. Played: a throw 0.7 s into the reload is
  refused, one at 1.0 s ends the reload's tail (Overwatch's animation cancel). Ofuda still interrupt the reload with
  "Healing Ofuda cancels reload" on (Overwatch's default).
- **On a server without Overcraft (#32):** no hero select on joining, no asset-pack message, and G/H do nothing (the
  rest was already off: such a server never sends a hero).
  - Played on a real vanilla 26.3 server (local, offline mode, flat; with build 0.3.3). The steps:
    - Joined: Minecraft's HUD and hand, no hero select, no Overcraft chat (`r14_vanilla_join`).
    - G and H: nothing happened (`r14_g`, `r14_h`). Esc opened Minecraft's own Game Menu (`r14_esc`). Chat was plain.
    - Sprint ran at 5.61 m/s with the 9.6 m/s sprint-jump, Minecraft's numbers.
    - A zombie took her from 20 to 16 health.
    - /kill: Minecraft's death screen; respawned with no hero select (`r14_respawned`).
    - Disconnected and rejoined: still plain (`r14_rejoin`).
    - Then the Overcraft world: hero select on joining, and G switched hero mode off and on.
    - Logs: the client logged nothing from Overcraft. The server logged no unknown-packet complaints and no kicks.
  - What still shows: Overcraft's key bindings in Minecraft's Controls screen (any client mod's do).
- **Suzu's ring (#22)** finds the ground up to 8 blocks below the burst (it was 2), for bursts on an ally in the air.

### Build 0.2.0
The first versioned build (#21). Everything before it in this log was 0.1.0. Its asset pack must be rebuilt
(`py tools/build_asset_pack.py sounds <extracted>`, then `zip`): an older pack is flagged in chat on joining, and in
the pause menu.
- **Versions (#21):**
  - The pause menu shows the build ("OVERCRAFT 0.2.0") and the asset pack's version, in red when the pack is older
    than the build needs (`Overcraft.ASSETS_NEEDED`).
  - `build_asset_pack.py zip` writes the build's version into the pack (`assets/overcraft/asset_pack.json`).
  - Joining with an old pack says so in chat.
- **Her own sounds in her head (#17):** Swift Step's sounds were still left behind (bound to her feet, they also came
  from a direction as she turned). Every sound she makes for herself now plays in her head, with no position, as
  Overwatch's own sounds do:
  - the cast, throws, reloads and hit confirmations;
  - her voice lines and quick melee;
  - the ult line and the climb.

  Others hear her voice lines following her. Her Swift Step line now plays once she has arrived, not at the key
  press.
  - Reproduced: the cast placed at her feet, `at 80.5 -61.0 80.5`.
  - Played: every own sound "in the head"; the line after the cast, once she was 12 m away.
- **Quick melee in first person every time (#24):** the server picks one of three melee clips; the first-person model
  has one. Two melees in three played nothing in first person (6 of 6 in the reproduction). A missing numbered
  variant now plays the plain clip.
- **Wall climb hover (#25):** in Creative, a double tap of jump while climbing switched on flight, and our movement
  under Minecraft's flight damping floated her at vertical speed 0, sinking 0.5 m/s. Hero mode now has no Creative
  flight; Minecraft mode keeps it.
- **Protection Suzu blocks knockback too (#27):** phased allies are out of explosions altogether, and take no
  knockback. A creeper's blast inside Suzu's 0.5 s: no damage, no movement; the same blast without Suzu moved her
  1.3 m.
- **`/overcraft reset [players]` and `/overcraft ult [players] [percent]` (#23):** operators only. They ready the
  cooldowns and refill the ammo, or set the ult charge.

**The owner's playtest, first batch** (issues #11-#20, from build b480e53; each reproduced in game first, then played
again on the fix):
- **#11, reload played the wrong animation** (quick melee in third person, an ofuda throw in first person). The clips
  we called the reload were a variant of the ofuda throw. Her real reload is a single clip (alias 0x004, which we had
  called "equip"): a fresh fan of three kunai, drawn and fanned (third person ABCF, 1.33 s; first person A810,
  1.47 s). Clip maps fixed, and the two models' clips renamed in place (`tools/rename_glb_clips.py`): **the asset zip
  needs rebuilding** (`py tools/build_asset_pack.py zip`). Played: R in both views (`r8a_rl1_sheet`, `r8a_rl3_sheet`).
- **#12, hold-mode ofuda looked like they all left at once on release.** Overwatch does refill the ofuda when you
  stop (the wiki: "automatically reload when they are not in use ... all 10 restored"). But ours played the wrong
  reload clip (#11), dimmed the whole counter, and blocked a new press until the refill. Now:
  - the counter keeps the ofuda left;
  - a new press throws them at once;
  - the refill has no animation of its own: the volley's end clip shows the new ofuda.

  Played: released at 6, pressed again (6 -> 2), refilled to 10 (`r8a_of_sheet`).
- **#14, first-person arms:**
  - 10% bigger on screen.
  - No Minecraft walking bob: it swung them like Steve's hand (`h14_sheet`). Also dropped: Overwatch's add_walk
    layer, played as raw deltas, which lifted them about 85 px while moving.
  - They now drift a little against the movement: 21 px aside strafing, 27 px up falling, within 3 px still while
    running (`r8a_*`).
- **#15, projectile sizes:**
  - Minecraft widens every projectile's hits from 0 to 0.3 m over its first 8 ticks, whatever its size. Ours now hit
    with their own radius from the first tick.
  - Kunai 0.195 m (base 0.12 plus Overwatch's 0.075 for projectiles over 50 m/s; the wiki: 0.19), ofuda 0.3, Suzu
    0.2. Played on a husk 6 m away: 0.15 m outside its box hits, 0.25 m misses.
  - The thrown ofuda's look is back to the effect data's 0.75 m.
- **#19, her eyes looked dead:**
  - Overwatch's eye-shadow shell (a flat near-black texture, faded by the game's shader) was drawn solid over her
    eyes. It's no longer drawn.
  - The eyeballs' images have only the iris opaque (the shader adds the white), so the eye white now goes in under
    them.
  - Select screen before/after: `eyes_pair2`.
- **#20, friendly fire:** a new option (Options > HERO > ALL HEROES, off by default). On, your kunai and quick melee
  hurt allies (players on your team, pets, villagers). Played: kunai at a villager off (20 -> 20) and on (20 ->
  10.4), quick melee on (10.4 -> 7.2).

**The owner's playtest, second batch:**
- **#18, Victory.** Killing the Ender Dragon shows Overwatch's VICTORY to everyone in the End: big gold letters over a
  dark band for 5.6 s, from the moment the dragon starts to die. The announcer's line and the music are sound events
  waiting for the pack (identified in `tools/reference/sounds-round8.md`, not mapped yet).
  - Found on the way: kunai went straight through the dragon. Its hitboxes are part entities, so the check for what
    a kunai can hurt refused them. A hit on a part now hurts the dragon through it, and the head counts as a crit.
  - Played: a dragon at 3 health killed with kunai (`vic_f_sheet`). Note: a NoAI dragon can't be hit by anything
    (it never places its parts).
- **#13, voice lines match what's happening:**
  - "Now I'm having fun" is a multikill line in the game data and on the wiki, not On Fire's. Multikill fired on any
    two kills of anything within 4 s, on every kill of a chain. Now it fires only on enemies (hostile mobs, enemy
    players), once per burst (the 2nd kill), at most every 15-20 s (the data's cooldown).
  - **On Fire** exists now. Points come from damage (100 per victim's full health) and healing (1 per 5 HP), decay
    5 a second, and catch fire at 250. These are the wiki's numbers; Blizzard never published OW2's. Blizzard's
    3 s wait before catching fire again is in too. On Fire plays its own lines ("I'm on fire") and sets the HUD
    portrait alight: an orange border, glow and flames (`r8x_fire_pair`).
  - **Hero-specific lines** only play for their hero: "Rolling with you" when Swift Stepping to Wrecking Ball,
    "Howdy" to Ashe or Cassidy, a Reaper healing line... (13 Swift Step heroes, 3 for healing), with Overwatch's
    weights. Headshot and in-air kills have their own elimination lines.
  - Skin and seasonal lines are out of the generic pools. Pools are no longer cut to 8 lines (the heal pool has 20),
    and each line keeps Overwatch's weight (sounds.json `weight`).
  - From `tools/reference/kiriko-voice.md`: criteria and weights read from the game data, offline.
  - Played:
    - three husks killed in a burst: the multikill line on the 2nd, nothing on the 3rd;
    - a cow: no line;
    - On Fire reached, with its line and the burning portrait.
- **#17, Swift Step's sound.**
  - The hand seal's whoosh (2.7 s) played at the spot she cast from. She teleported away and heard it fade behind
    her, then the arrival played where she landed.
  - Now her own sounds follow her (bound to her). She hears the whole whoosh and no arrival: the game plays her only
    the cast. Others hear the arrival out to Overwatch's 70 m.
  - Played: the harness's new `heard` command lists her client's sounds: the cast bound to her, then the voice line.
- **#16, Kitsune Rush's corridor sound.** Each player in the corridor hears it in their head:
  - the opening hits on entering, and three looping beds at the game's pitch (one 1.79x);
  - a 0.3 s fade-in, and a 2.5 s fade-out after leaving;
  - entering again starts it over.

  Played: in, out (silent about 3 s later), back in (the start plays again).
- The asset zip needs rebuilding for all of this (`py tools/build_asset_pack.py sounds <extracted>` then `zip`).

**Seventh critique round** (`tools/reference/critique-2026-10-26-round7.md`). Each item was reproduced on the old build
at 1080p, then fixed and played again:
- **Kitsune Rush in first person no longer turns the view mint.** The cast's smoke cloud spawned around her own camera.
  Her own view now gets a small cyan burst between the palms; others still see the smoke at her hands. Under the hands
  the grass keeps its colour (87°, it was 128°).
- **No glow ball in the gates.** Each gate's rising flash hung in it like a sun, brighter than the fox. Without it the
  fox showed matte blue: its body was drawn over its own glow. It is now all added light, glowing white-cyan, brighter
  than the sky.
- **Colours over grass:**
  - The unaimed talisman pair is blended, teal in flight (166°; it read pale green, 119°).
  - The rush's "+" sparkles are blended gold (52-58°; they were lime, 71-74°).
  - The path tiles are bluer (170-181°).
  - Faint gold is brighter and more orange.
- **Protection Suzu's 5 s ring lies on the ground.** It is laid in short pieces, each on the surface under it, over
  slopes, steps and leaves. Before, one flat circle hung in the air through the trees (`r7l_suzu_*`, `r7n_suzu_*`).
- **The ofuda throw's golden puff follows the flicking fingertip** in first person. Before, it stayed at a world point
  right of centre while the hand moved on.
- **Swift Step's LSHIFT box turns coral from the key press**, through the hand seal, as in Overwatch. Its screen flash
  now comes with the step, not the press.
- **Ally health bars:**
  - Lost health shows as separate empty slots, no longer one dark slab.
  - A bar that would cross a screen side is hidden, as one off the top already was.
- **HUD portrait:**
  - The frame is Overwatch's tall hexagon (a gabled top, the point below), with her hair and the mask's ears over its
    top edges.
  - The party box is 30 x 44 with a thin pale digit.
- **Kitsune Rush at night:** the edge mist is cyan (185-190°), no longer grey fog.
- **Left as is:** the held talisman's grey back is Blizzard's baked texture, on a separate layer of the paper.
- **The owner's call:** Minecraft's blob shadow (both in HANDOFF's questions).
- **Harness:** `state` reports the first-person fingertip (`fingertip`).

**Minecraft's other body poses on her model** (found playing; the hero renderer drew her upright through all of them):
- **Crawling** under a one-high gap: she stood in it, the camera inside her legs (`ps1_sheet`); now prone, along her
  box (`ps2_crawl`).
- **Sprint-swimming** (Minecraft mode): now horizontal, swimming away (`sw2_sheet`); hero mode has no sprint, so she
  wades upright, as in Overwatch.
- **Elytra gliding**, in both modes: now pitched into the glide, head first (`gl3_sheet`).
- **Riding** a boat or a horse: crouched (Overwatch has no sitting clip; crouched is the nearest).
- The pitch and offsets are Minecraft's own (AvatarRenderer.setupRotations), her legs straightened as in bed.
- **A ridden animal's hearts** sat on the ability boxes in hero mode: they now sit above the health readout, where
  the air bubbles go (`ps2_sheet`).

**Fixed: asleep in a bed she stood upright on it** (found playing a night in survival). The hero renderer drew her
at her yaw like any other moment; Minecraft lays a sleeping player flat. She now lies on her back, head on the pillow,
along the bed's direction (Minecraft's offset), her legs and hips straightened (her idle stance's tilted hips and bent
leg pointed one leg at the sky, `sl2_sheet`), no aim lean or head turn. Played: F on a bed in hero mode, north- and
east-facing beds, front and back views (`sl3_sheet`, `sl3z`); the first tries: upright (`bedp_z`), legs up (`sl2z`).

**Fixed: status-effect icons covered the kill feed** (found playing: a weakness icon hid a line's victim). The feed
already moved below Minecraft's toasts; it now also starts below the effect icons' rows (one row for benefits, two
with a harmful one). Played: a kill with no effects, with Speed, with Speed and Weakness (`kf_effects`). Also seen:
the dismount hint names the hero crouch key while riding a horse in hero mode, and drowning works as in Minecraft.

**Underwater, the air bubbles join the hero HUD** (found playing in a lake): Minecraft's bubbles drew in their own
place, against the ult meter and the ability boxes. In hero mode they sit just above the health readout, as wide as
its pips and tilted with them (`air2`: full, then draining); Minecraft mode keeps vanilla's. The first try overlapped
the health number (`air`). Also played in water: she floats in her fall pose, the HUD and arms as usual (`sw_sheet`).
Checked: every other screen shape still fits (21:9, 4:3, 5:4: `asp2`); the model costs about 3 ms a frame in third
person in the forest world, 1 ms in first person; Kiriko's numbers match the wiki's patch log (its ability box is stale
on three of them: seek range, fire rate, kunai size).

**Fixed: "ELIMINATED <name>" could stay on screen for good** (found in the play pass: "ELIMINATED PIG" over every
frame in the survival world, minutes after a kill in the test world). The caption and the hit marker time themselves
on the world's clock; a kill in an older world, read in a younger one, had a negative age, clamped to "just now".
They're cleared on leaving a world, and an age well below zero reads as long past. Reproduced (`elim_now_c`); after:
the caption at the kill, gone 2.5 s later, nothing in the next world (`el_fix`).

**Fixed: Swift Step could put her inside blocks** (found in the full play pass below: the tamed wolf was in a hill,
and she landed in it, suffocating, 250 to 238). With no room beside the ally it fell back to the ally's own spot,
which fits a wolf but not her. Now: beside, either side, the near side, on them, on top of them, and if she fits
nowhere, no step (the "denied" click, no cooldown spent). Reproduced first on the old build: a wolf in a one-high
tunnel, she landed in the stone (`tun_after`). After: one-high tunnel, she stays, cooldown 0 (`tun1_aim_c`: the
target was found); two-high tunnel, she lands just in front of it (x -13.4, the wolf at -14.5).

**Played: a full pass on a fresh normal-terrain survival world** (CLAUDE.md's pre-release play; harness `newworld
<name> normal`): Kiriko picked on join with real clicks on the select screen; Minecraft mode: punched a log out by
hand (0 to 1), crafted 4 planks in the inventory grid by clicking, tamed a wolf with bones; night on Normal with
natural spawns (12 hostiles within 64 blocks, the forest kept them back; kunai at range took them from 7 to 2); a
zombie killed her (3 per hit, natural regeneration between), death screen, respawn with the kit and ult kept; Save
and Leave, rejoin: hero select, Enter kept Kiriko, ult kept (`pp_*`, `ppn2_sheet`, `pp_death_sheet`).

**Fixed: her leg kicked out for a moment every 3 seconds, again** (round 6 #3, a Swift Step arrival caught it).
Overwatch's breath clip has two broken right-thigh keys; the load-time mend caught the one at 0.167 s but not the one
at 1.967 s, whose neighbours are 23 degrees apart (the mend wanted under 22.5). Now it takes neighbours within 45.
Reproduced first: idle, the game frozen and stepped a tick at a time through a breath cycle: one tick in 64 with the
leg kicked up (`br_sheet`, `kick_repro`). After: 66 ticks, none (`bf_sheet`).

**Round 6 of the critique** (`tools/reference/critique-2026-10-26-round6.md`), played after each:
- **The "wings" on homing ofuda** were the next pair just thrown (toggle throws the whole clip): each 0.75 m talisman
  a metre from the camera filled the sides of the view. Talismans are 0.5 m and fade in from 2.5 m; their ribbons
  start on them, about 0.7 m long and 5 cm wide, and fade in from 2.5 m too (`of2_sheet`, `of2az`).
- **Teal over grass read mint, gold olive**: the faint teal effects (ofuda trails and flowers, healing glows, Swift
  Step rings, petals and puff, the rush's smoke, rings, tiles and speed lines, the fox's wake) are blended in a
  blue-leaning teal instead of added; blended gold more orange (`of2cz`, `fx15_sheet`).
- **The fox**: no separate glow ball over it; its own glow stronger, so the body is the bright thing (`fx15_fp_b`).
- **The first gate in first person**: gates are gone within 3 m and full from 8 m.
- **The rush view's mist** tinted the whole screen mint: lighter. The cast's smoke fainter.
- **Chat behind the death screen** was Minecraft's, crossing the portrait: it's hero chat there too (`dchat`).
- **An enemy's effects**: tiles and petals red, not brick brown; gates stronger; the fox red, not lilac-grey (with a
  second client on another team, `en6_sheet`).
- **Minecraft mode's glove** is red with black knuckle pads, as hers (`glove2z`).
- **The ofuda throw's puff** is shorter (it stayed in the world after the hand moved on).
- Not changed: the kunai glyph and in-flight look, the first-person paper in the flicking hand (Blizzard's), chat's
  typing look (Minecraft's while typing, so clicks land).

**The HUD portrait like Overwatch's** (round 1 #27 and #28, open since): the frame ends in a point, and the portrait is
Overwatch's own HUD head art (the pack's `head.png`) with her ponytail breaking out over the frame's top, drawn over
the border (it was the square portrait clipped inside); the Esc menu's portrait too. Left of the name, a tall
light-blue "1" box (Overwatch's party slot, #41CCFD) with the underline running from it. Played: `pf_cmp` (against
`ref_queued_02`), `pf_menu2c`. (The wings and level badge round Overwatch's are rank cosmetics: not ours.)

Also played: changing hero with H (a bed that isn't her spawn: "Go back to your bed"; the Esc menu's CHANGE HERO
greyed; made her spawn: hero select opens, "near your bed", Esc keeps Kiriko: `hs_sheet`, `hs_bed2`). Suzu's cleanse on fresh frames (round 4 couldn't judge it: its captures predated the fix): Suzu at her
feet, her first-person arms flash white and her body glows white in third person, fading (`cg2_sheet`); low-health
edges at 50 of 250. Wall climb in first and third person (`climb_sheet`: her hands reach up the wall in turn, over the top
into idle) and quick melee on a husk (two punches, 20 to 13.7 health: Overwatch's 40 each, `melee_sheet`).

**Played: plain Minecraft in Minecraft mode** (survival, Easy; CLAUDE.md's slots 2-9 check after tonight's mixins on
chat, the HUD, the hands and the death screen): mined stone with a pickaxe (cobblestone 0 to 1), placed planks (8 to
7), opened a chest, the inventory (its preview shows Kiriko) and a crafting table, ate bread when hungry (food 0 to 5,
bread 4 to 3), G back to hero mode (`mcp4_*`, `mcp5_*`). Nothing broken; the misses on the way were the test's own
aim (`execute facing` aims from the feet unless anchored at the eyes).

**Ribbons no longer end in the ground in straight lines** (round 5 #4): a projectile's last position is inside what it
hit, and the ribbon's end there was cut off by the surface. Trails skip points inside solid blocks. Played: unaimed
ofuda thrown at the grass, frozen at three moments (`rib_sheet`, `rib3z`: soft impacts, no edges).

**Swift Step lands beside the ally** (owner: "Swift step should put you beside, yes"; the wiki: "at their side"). She
landed between the ally and where she came from, facing them, so they filled her screen. Now she lands shoulder to
shoulder: at the ally's side across the line she came along, her right first, then her left, then (no room either
side) the side she came from. Played (villager 10 m ahead, Swift Step): x 39.4 beside the villager's 40.5, same z, the
view ahead clear (`ss_beside_sheet`); a wall on the right: x 41.6, the left; walls on both sides: z 11.4, the near
side. With a second client: onto a player, x 39.4 beside their 40.5, neither view blocked (`mpss_sheet`).

**Minecraft mode's first-person arm is Kiriko's** (owner: "Let me see what first person kiri in minecraft mode would
look like"). With a hero picked her body is Kiriko in third person, but first person showed her own skin's arm. The
arm now wears a generated Kiriko skin (HeroArmSkin: her red sleeve and raised cuff, black wristband, dark fingerless
glove with red trim, skin fingers), painted row by row so it fits wide and slim arms, at four times Minecraft's
resolution, sharp like a skin. Held items draw as Minecraft's. Played: empty hand idle, punching, looking down, at
night, and G back to hero mode (`mcarm_compare`, `mcarm2_z`). A first try was soft-filtered and striped (`mcarm_z`).

**Kill feed rows closer to Overwatch's, and "ELIMINATED" on a final blow** (round 5 #10, #12). Portraits are 52 px
wide face crops of the hero art (were 34 px squares), with a bright 4 px edge on their inner end next to the middle
(#30B1DD ally, #AA3F66 enemy); names have no shadow (`feed3`). A final blow shows "ELIMINATED <name>" under the ofuda
counter for 1.6 s, the name in red (its size and place are ours: no reference frame shows one). Played: a kunai kill
on a husk (`elim2`; a zombie burned in the daylight first, so no caption: correct, the kill wasn't ours).

**Thrown ofuda are glowing talismans, gold or blue** (owner, this morning: "Two talismans do get thrown with each
burst. But they arent standing straight up like that. There is a different model; a glowing gold type thing for thrown
ofuda, or blue when it isnt tracking"). The effects survey agrees: in flight an ofuda is effect 2F93 (no target) or 7E2D
(locked on), a glowing talisman silhouette fluttering through four frames (mask 3A1EE, now in the pack as
`talisman_flight`), with a ribbon and a glow. Each ofuda draws that silhouette along its flight, head leading, added
teal or blended gold (and the enemy red); no model. Played (`pj2_sheet`): two gold talismans curving onto a villager,
two blue ones unaimed.

**Projectile models never drew** (found chasing round 5's "no paper on the ofuda"): the hero renderer hides props
parked within 0.3 m of a model's origin, and a projectile's model is small and centred there, so every triangle was
skipped: the kunai and the Suzu bell flew as glows alone. Projectile models now skip that rule (the bell shows in
flight, `pj2_suzu`). On the way: a card billboard for the ofuda's paper (first two tries in `pj1_sheet`, the paper pair
from her hand standing upright) was wrong, per the owner, and is gone.

**More of round 5:**
- Teal read mint over grass (hues 135-162 degrees against Kiriko's 176): the faint ends of the teal and cyan ramps move
  toward blue, the petals' tint too; the rush's path tiles no longer clip to flat cyan (`fx13_sheet`).
- The fox was a small dim figure in a 2 m mint glow ball with its wake seen end-on as a white ellipse: its glow is
  1.1 m and fainter, its wake low on the ground, its body brighter (`fx13_fp_a`: the fox reads).
- The first gate framed the caster's first-person view like a glass doorframe: gates are gone within 2 m of the camera
  and full from 6 m (`fx13_fp_a/b`).
- The ofuda ribbon starts behind the projectile, not on it.
- Not changed: the Swift Step seal and the kunai throw's hand (Blizzard's own first-person clips; the differences
  are in which moment was captured, and need frame-matched Overwatch footage), the kill feed portraits and an
  "ELIMINATED" caption (next).

**An enemy Kiriko's effects are red** (HANDOFF's "enemy-red view", Overwatch's rule: you see an enemy's effects in the
enemy colour). Only with scoreboard teams (players are allies otherwise). The server marks each effect for each player
who gets it (`EffectPayload.enemy`: the hero who made it is on another team); flights (ofuda, kunai, Suzu, the fox and
its petals and wake) check the projectile's or fox's owner on the client (the fox now carries its owner). Enemy effects
are blended, in a red picked on screen: added, the game's red ramp washed out pink-white over sky and grass (first try,
`en_sheet`). Played with two clients on two teams, the second watching (`en2_sheet`: Suzu's ring, the ofuda puff, the
Swift Step ring in red; `en3_sheet`: an enemy's rush, red talisman tiles, petals and sparkles, faint red gates, while
her own view stays cyan). Harness: `fx <effect> [variant] enemy`.

**Round 5 of the critique** (`tools/reference/critique-2026-10-26-round5.md`), first four:
- The ally bar sat on a close ally's forehead (anchored at the box's top centre, behind the face): it now sits above
  the highest of the box's top corners on screen (`r5_st`).
- A kunai's glint spawned half a metre from the camera stretched over the screen as a faint band, outliving the hit
  (`hm6_head`): no glints within 3 m of the camera (`hm7_head`).
- Suzu's 5 s ground ring was pale khaki; it's blended in a gold picked on screen (#FFD732) (`r5_suzu`).
- Swift Step's arrival rings were crisp neon hoops: fainter, fading as they open.

**Dying in hero mode keeps the hero's screen** (found playing a night raid: about 19 zombies from natural spawns
swarmed her in three rounds). Hero mode required being alive, so at death the HUD flipped to Minecraft's hotbar and
empty hearts behind "You Died!", and her arms to Minecraft's blocky arm. The HUD and the view model now follow the
hero and slot 1 alone (input still needs her alive): the death screen shows her HUD and arms (`death2_sheet`); dying in
Minecraft mode still shows Minecraft's (`death_sheet`, right). Also played around tonight's toggle default: G, a slot
change or a death mid-volley each end the volley, and it doesn't resume.

**Hero-mode chat in the HUD's sans, and out of the health's way while typing** (round 4 #12, and a bug found doing
it). Not typing: chat lines in Futura at Minecraft's capital height (a new `hud_chat` face; letters it lacks, such as
Japanese, fall back to Minecraft's font), starting 52 px in, without Minecraft's message-tag bar (`chat1c`, `chat2c`).
Typing: Minecraft draws chat from the chat screen, not the HUD, so the hero-mode lift never applied and the bottom
lines covered the health number (`chat3c`); it's now lifted inside the chat component, which also moves the clicks:
a suggest-command link clicked on its lifted line filled the input (`chat4c`, `chat5c`).

**The cooldown splash is a brush stroke round the digits** (round 4 #14): the digits' own shape grown 3-7 px in black
with a ragged edge and two short drips, instead of one round blot behind any number (`br2_all`: ours beside
Overwatch's "12").

**The saved ult charge also survives a death before a crash**: ult 70%, Save and Leave, rejoin, die, `/save-all`, kill
the game: on the next start 1957 (the 1911 saved, plus the passive charge).

**Round 4 of the critique** (`tools/reference/critique-2026-10-26-round4.md`, a sub-agent on fresh captures; its Status
section says what's done). Played in game after each change, before and after:
- **Ofuda hitting the ground or a wall**: the glow sank half into the surface and ended in a straight line, clipped to
  flat cyan (reproduced on a wall: cut on a diagonal). It's lifted off the surface just enough for the view angle, and
  blended (`fx9_ofuda_d`: soft, no edge).
- **Homing ofuda were cream comets**: blended gold took the game ramp's top, near-white. A gold picked on screen
  (#DCB900 to #A18700, the critique's samples) and the glow put behind the paper (`fx9_ofuda_b`: gold trails).
- **Kunai hits drew a teal bubble**: the in-flight lens glint, spawned while the client's kunai rested at the wall a
  tick before removal, its faint halo turned into a disc by gamma. Glints now only in flight, the halo cut: a hit is
  sparks (`fx9_kunai_a`; at night `hm6_head`).
- **Arms nearly black at night**: the view model now has a light floor (`hm6_sheet`: before left, after right).
- **Kitsune Rush gates**: white-cyan, faint where they face you and cyan at their edges, gone within 1.5 m and full
  at 4 m (a post a metre away was a slab of cyan); the "+" sparkles are soft glowing crosses that twinkle; the cast's
  smoke fainter. Also found playing: at the cast a 4.6 m flash two metres away covered the whole view in cyan; big
  sprites now fade over a reach scaled to their size (`fx11_sheet` before, `fx12_sheet` after).
- **Swift Step**: the slash arcs aren't drawn round her own camera (they swept over the crosshair; the screen's
  streaks do that job); the arrival's camera-facing ring (a bubble) is gone, its flash 0.8 m (`st_sheet`, `fx10_*`).
- **Ally health bar**: it floated 0.35 m above the head, near the top of the screen for an ally close by; it now sits
  a few pixels above the head (`st_c`). Pips 9 x 8 at an 11 px pitch (were 6 x 10 at 8).
- **Ofuda counter tiles** 30 x 15 px, overlapping like Overwatch's (were 25 x 14 with gaps).
- **Kill feed**: always a glyph (the weapon's art, else a fist for melee, a skull for the world's doing), a bold ">",
  and faces for mobs (read off Minecraft's own textures: zombie, skeleton, creeper, spider, villager, pig and 20 more)
  and for players without a hero (their skin). Played: a kunai kill on a zombie, a melee kill on a pig, killed by a
  zombie, killed by the void (`feed1_all`, `feed2_cz`); with a second client, a hero-less player killed by a zombie,
  seen by Kiriko with their skin's face (`mpf_kz`). (The feed is part of the hero HUD: hero-less players and
  Minecraft mode don't show it.)
- Not reproduced: a grey blade at the left edge during the rush cast (six cast frames). Open: the kunai throw's
  hand over the crosshair, the chat font, the cooldown splash's shape.

**The ult's charge survives leaving the world** (found playing: rejoining kept the hero but lost a 52% ult). Saved
with the player when they leave. Played: 60% (1638), Save and Leave, rejoin: 1680 with the passive charge, the HUD
says 61 (`ult_rejoin`).

**Kiriko's hero options** (Overwatch's "Hero-specific options", wiki table, with Overwatch's defaults): a HERO tab on
the Overwatch options screen (Esc in hero mode, OPTIONS). Healing Ofuda sensitivity and Swift Step sensitivity (10-100%,
narrow the lock-on cones), Toggle Healing Ofuda (**on by default, as in Overwatch: one press of the ofuda button throws
until the clip is empty or you press again**; off, it throws while held), Automatically climb walls (off), Healing
Ofuda cancels reload (on), Healing Ofuda UI (the counter under the crosshair, on). Hovering a row says what it does;
RESTORE DEFAULTS resets the page. Saved in `config/overcraft-hero-options.json`; the server gets the three it acts on.
Played through the harness (`opt_hero_s`, `opt_r4c`, `opt_hov1c`):
- Toggle: one press emptied the clip (10, 4, 0, then the refill to 10); press-press stopped at 6.
- Ofuda sensitivity, a hurt villager (10 HP) 12 m away, looking 5 degrees off it: 100% healed it to 19.6, 10% missed
  (10.0); 10% aimed straight at it healed it.
- Swift Step sensitivity 12 degrees off: the target diamond at 100% (`ss100c`), none at 10% (`ss10c`).
- Auto climb at a 13-block wall, forward held and one 0.1 s jump: on, climbed to the top; off, a plain jump.
- Cancels reload, three kunai thrown, reload, ofuda 0.2 s later: on, the reload stopped (kunai stay 12) and the ofuda
  threw at once; off, the ofuda waited until the kunai were back (15), then threw.
- Ofuda UI off: no counter under the crosshair (`opt_hudz`). Restore defaults: the file back to the defaults.
Harness: `hover x y` moves the game's cursor only (hover states, without the real mouse).

**The real Suzu burst and Kiriko's own kunai impacts** (the sound research's biggest gaps). A sound sub-agent extracted
the banks the first extraction missed, from the archived build, offline and read-only, with DataTool's own conversion
(`tools/reference/kiriko-sounds-extra.md`; the files stay in the archive's `ExtraSounds\`): 236 files. Now in the
pack: the Suzu burst's own sound (bank 76FD/96F8, 5 variants; it was a stopgap from a souvenir clip), the kunai's own
hit on a body (bank 8DE7, three layers mixed; it was a generic flesh hit) and on terrain (826E; it was a Minecraft
stand-in). Played with subtitles on (`snd2_sheet`, `snd3s_block`): "Kunai hits", "Kunai strikes" on a wall, "Suzu
rings"; `soundinfo` shows each event backed by the pack. Not used yet: the ofuda heal-hit banks (the current heal mapping
is medium confidence, the new ones unheard), the probable kunai whiz-by and fox footsteps (labels still guesses).

**Fixed: Kitsune Rush washed her whole view cyan at the cast** (found looking at fresh first-person captures, after
the fox got its glow tonight). The fox starts inside Kiriko, so its 2 m glow sat on the camera. The fox fades out within
4.5 m of the camera and its glow and wake start once it's past that; in front view it no longer passes through the
camera as a white shape either. Played: the cast in first person (`fox3_sheet` before, `fox4_sheet` after: the fox seen
running out ahead).

**Hit markers** (missing: no capture had shown a hit). Every kunai and quick melee that deals damage flashes Overwatch's
X of four strokes round the crosshair for 0.2 s: white on a hit, red on a critical hit, larger and red for 0.45 s on a
kill. The server tells only the attacker (`Effects.hitMarker`). Shapes and colours are ours, after the game's; no
reference measures them. On the game clock, so they pause with the game. Played frozen 0.15-0.2 s after each throw
(`hm4_sheet`, `hm5z_kill`): body, head, kill.

**Low-health edges from 30%**, the wiki's number ("~25-30% or lower ... blood appears around the borders of the
screen"); it was 35% (ours).

**Played: other screen shapes** (`asp_w`, `asp_pair`): 21:9, 4:3 and 5:4 with the new HUD; nothing overlaps.

**Played: a night fight on Normal** (CLAUDE.md: "a real fight"): three skeletons and three zombies against Kiriko in
open ground, the harness aiming kunai at the nearest of each, Suzu, ofuda at her feet, a strafe-jump, the ult. She dies
in the fourth round both times (250 to 0 in about 6 s once both groups reach her; her ult charge climbs from her hits
throughout, so the kit works). Six mobs at once is a lot for a support with scripted aim; no bug found. (`fg1_sheet`.)

**Her model draws faster** (performance). Found by playing: third person ran at 31 FPS against 115 in Minecraft mode
(this machine's dev client runs on its integrated GPU). Measured with new harness timers (`timing`, `timing stride N`,
`timing cull on|off`, `state` now has `fps`): posing her took 0.6 ms a frame and writing her vertices 5-6 ms; the rest
of the 23 ms was moving her 67,500 triangles to the GPU, and frame time scaled with the triangle count (all 31 FPS, half
62, a quarter 83, none 115).
- Triangles facing away from the camera on her nearly closed pieces (the body: under 3% of their edges open) aren't
  sent: they're hidden behind the piece's near side. Open pieces (hair and cloth cards, paper) show both sides and are
  sent whole. A first version culled everything, believing Minecraft's cutout entity pipeline culls; it doesn't, and
  her hair showed the sky through it from below (`caw`: caught comparing culling on and off from four angles).
- Each vertex is put in the camera's space once, not through two allocating calls.
- Drawn as triangles through a copy of Minecraft's cutout entity pipeline (`FxRender.heroCutout`), not as quads with a
  corner doubled: a quarter fewer vertices to send.
- Third person 31 -> 50 FPS, first person 70 -> 99. Not on the select screen (Minecraft's GUI view of a model has
  no camera at the origin). Checked: the same pictures with culling on and off, from four angles and close on her head
  (`ca_sheet`, `caw2`), first person and the select screen.

**Kiriko speaks for the game's moments** (issue #5): her own recorded lines from the archive's voice categories, the
unconditional ones (lines whose criteria name a hero or map are left out; "NOT (scripted event)" is the normal case),
English first, up to 8 a moment (`tools/soundmaps/kiriko.txt`, picked by a script):
- "My ultimate is ready" the moment the ult fills; a shout on every quick melee; a remark after a melee kill, a taunt
  after other kills (by chance, spaced out), "Fox in a hen house" on a second kill within 4 s; a respawn line ("Let's
  get back to it") most respawns; "I'll find my own path" on picking her.
- Her hurt and death sounds are her own grunts and dying cry instead of Steve's (a Player mixin, on every side; burning,
  drowning and freezing keep Minecraft's), so they show as "Kiriko grunts", "Kiriko cries out".
- Healing lines now and then ("Be healed", "Like a gentle rain": the sound research's 0.15 chance per heal, at most one
  every 20 s, ours). The heal event existed but was never played.
- Played with subtitles on (`vo1_sheet`, `vo2_sheet`, `vo3s`): melee, ult ready, a kill, hurt, three deaths and
  respawns, healing a hurt villager ("Kiriko: Healing" on the first volley).

**Fixed: the ult's charge was lost on death.** Overwatch keeps it. Reproduced: 1638 points (60%) before dying, 11
after respawning; now the same before and after (2725 and 2725).

**Played: the coat from the side** (R2 #19 said "rigid boards to the ankles"): a second player watching her run past
from 5 m, the coat splits at her legs and trails behind as she runs, hanging when she stops (`cs2_sheet`, `cs2z`).
Left as is.

**Hero-mode chat is bare text** (round 3's #12): no dark panel behind the lines while nobody's typing, as
Overwatch's; the panel comes back while typing, and Minecraft mode keeps its usual chat (`chat_sheet`). Also seen:
Suzu's dome is there (an 8 m hemisphere for 0.3 s), but thrown at her own feet she's inside it and it reads as a
cyan wash over the whole view (`dome_sheet`).

**The critique's third round** (`tools/reference/critique-2026-10-26-round3.md`: 15 items on the newest captures).
- The volley bar's tiles are rounded rectangles about 25 x 14 px with their long sides rising 40 degrees, as measured at
  12x on Overwatch's frame (the earlier flat-topped shape read as hazard stripes, #7).
- Cooling down, the ability icon is a faint ghost behind the splash, not black (#8). Health number 8% smaller (#13);
  panel lines a 4 px core with a darker halo (#14); kunai ammo ends at x 1836 (#15).
- Low health darkens the corners and lays crimson smoke over them, deep in the corners (about 450 x 280 px), so it
  stays red over the sky (#3). Swift Step's flash is dark mottled teal smoke in the corners (#2).
- The kill feed in Overwatch's colours (teal-blue, crimson), portraits on the inside next to the weapon, always a thin
  ">" after it, a light inner edge on each name box (#6). Played: a player kill (`r3d_feedz`).
- Gold ofuda blended strongly enough to stay gold over grass (they mixed to khaki, #4); teal ofuda with a fainter trail
  and glow so the paper reads (#9).
- Kitsune Rush's cast: a few small rings, no 3 m ring round her (a bubble again, #5). The gates' "+" sparkles smaller
  and dimmer (big white crosses up close).
- The fox is a spirit: its texture at 60% opacity, full bright, with a cyan glow over it (#1: it read as a plush toy).
- Suzu's cleanse keeps her shape: a softer overlay and glow, in third and first person (#10: a flat white cut-out).
- Played (`r3_sheet_a/b`, `r3b_sheet`, `r3b_vol`, `r3b_cd`), and the first-person wall climb (`cl2_sheet`: Blizzard's
  hand-over-hand clip plays; round 2 hadn't been able to check it).

**Kitsune Rush's path laid with Kiriko's own talismans** (round 2's #17: "blank glass tiles"). The tiles are now her
paper talisman, its seal and glyphs glowing teal: cut at load from the ofuda model's texture in the local pack
(`EffectArt.keepTalisman`; plain cards without the pack). The torii gates fade in over their bottom 0.6 m: their posts
are open tubes, and their ends showed as rings on the ground. Played: the rush in first person, level and looking down
(`tal_sheet`, `tal2_z`).

**The ofuda flick no longer shows the redraw fist mid-volley** (round 2's #20). The throw clip (B0E1) is the flick,
then a fist drawing a fresh fan from the belt; while fire is held only the flick repeats now, the redraw plays when she
stops. Checked with the harness's new `anim1p` (the first-person action step) while holding fire: the 10 ofuda go in
about 0.7 s, mostly during the 0.2 s start clip (B1D6: from the idle fan pose out to the flick), then the reload; the
critique's "hand at the left edge holding the fan" frames are those two of Blizzard's clips, kept (`ofj_sheet`).

**Played: water, a ladder, a boat in hero mode** (CLAUDE.md's untried cases; `env1_sheet`). In a 3-deep pool she
treads water (the fall pose) and gets out with jump and forward; up a ladder 4.4 m in 2 s; F gets her into a boat,
crouch (LCtrl) gets her out. Fixed: the boat said "Press Left Shift to dismount", and in hero mode Left Shift is Swift
Step; it now names the hero's crouch key ("Press Left Control to dismount"), Minecraft mode keeps Left Shift
(`boat2_sheet`). The Nether (`nether_sheet`): hero mode and the HUD work there; set on fire, she burned (the flames
over the view), and Suzu at her feet put it out (Fire back to -20 ticks) and healed her to 250.

**Played: the hero's life cycle** (CLAUDE.md's general play, the parts not covered above; `lf1_sheet`, `lf1_sheet2`).
H 80 blocks from any bed: "Go back to your bed to change heroes", no screen. H beside a bed set as spawn: the select
screen ("near your bed: pick a new hero"), Enter keeps Kiriko in hero mode. Esc: the Overwatch menu (sub-role shown);
Save and Leave goes to the title screen; reopening the world shows the select screen with Kiriko already picked, Enter
puts her back in hero mode with her HUD.

**Allies feel Kitsune Rush on their own screen** (round 2's #17, in part: the ally "sees nothing"). The rush's
in-path mist and streaks are their own HUD element, shown to any player the rush is speeding up (the server's speed
lines on an ally name them), not only to heroes. Found on the way: a player without a hero had no HUD masks made, so the
first try drew Minecraft's magenta missing texture (`rv_sheet`); made on demand now. Played with two clients
(`rv2_sheet`: the second player, no hero, standing in the path).

**Suzu's cleanse glows white** (round 2's #18: the overlay alone only paled her, "a ghost"). Hero models are drawn a
second time as pure white-cyan light while cleansed, fading over 0.65 s (`HeroModelRenderer.submitGlow`); mobs keep
Minecraft's white overlay. Played: Suzu at her feet from the front (`cg_sheet`: a solid glowing silhouette, then
fading) and in first person (`cg_1pz`: her arm glows). The paleness the critique saw a moment later is the burst's
dome lighting the whole view (the sky too), gone within 0.4 s.

**The ofuda read as paper in flight** (both critiques' #2). The projectile is a flat sheet, and the renderer laid it
along its flight with its face level: edge-on to the thrower, so only the trail and glow showed. Now it turns its face
to whoever watches about its line of flight, drawn twice its model's size (ours, about 0.36 m), with a thin ribbon
(7 cm) and a small glow. Played: talismans summoned standing still, then throws at the ground and onto a villager
(`pq_sheet`, `pr_2z`: pale cards with a light teal glow; before, cyan comets in halos).

**Her leg no longer kicks out at idle** (a bug the critiques saw three times: "a leg up to shoulder height" in the
Suzu throw, "one leg sticks straight out sideways" at the Swift Step arrival, and once in a frozen reload frame). Not
the throws: Overwatch's standing breath (AFDB, as exported) has two broken keys on the right thigh, each turned 180
degrees for one frame, so every 3 seconds at idle the leg flung up for about 60 ms, whatever else was playing.
Reproduced: 40 idle screenshots 0.08 s apart on the old build, frame 16 has the leg straight up beside her head
(`br_before`). Fix: clips are mended at load, a rotation key turned over 90 degrees from both neighbours that agree
with each other takes their average (`SkinnedModel.dropRotationSpikes`). After: 80 frames over two loops, no kick
(`br_after`); the ult cast no longer kicks either.

**The critique's second round** (`tools/reference/critique-2026-10-26-round2.md`: a fresh critique sub-agent's 27
items on the new captures, measured against the same Overwatch frames).
- HUD: the volley bar's segments are fat tiles again, as on Overwatch's frames (they had become thin slashes, #3); the
  ult meter at rest is see-through, only its tick band darkened, no unfilled ticks (#10); the ready halo wider (#26);
  weapon panels as tall as Overwatch's (lines 61-62 px apart), 16 px between them, mice raised, ammo text 16 px (#11,
  #15); lines with a soft falloff; ability boxes, underlines and pips drawn as single smooth quads (they stepped in
  1 px slices where the HUD slopes, #21).
- Chat in hero mode is Overwatch's size and place: small text at the left above the health, its bottom near y 700
  (#9); while typing it stays full size.
- Low health: mottled red edges, deepest in the corners and about 150 px deep, red over the sky (#12).
- Swift Step's first-person flash: teal only at the edges with 26 streaks rushing in, the centre clear, and only in
  first person (it tinted the whole frame, also in third person, #5). She lands 0.9 m clear of the ally (#6). The
  arrival's ring is 1.4 m (a 3.2 m ring read as a shield bubble, #16). Her own arrival's arcs fainter.
- Ally markers: the name in blue caps under the pips (players and named mobs), an open "v", clear above the head;
  allies' Minecraft name tags hidden in hero mode (#8).
- Suzu's flower and the ofuda's ground flowers blended teal at 70% (they were emerald and opaque, #14, #25).
- Hero select: the model larger, its legs running on behind the roster under a dark band, no hard edge (#13).
- Kitsune Rush's fox (#1): it was there all along, but low, fast (20 m/s) and behind a bright glow where each gate rose.
  Drawn 1.4 times its model's size (ours), with a cyan wake and glow; the gates' rising flash much fainter. Played: a
  fox summoned standing (`foxs_sheet`), the rush from the front (`foxh_sheet`).
- Not ours: the hooked idle talismans (#4) and the high-knee strafe (#7) are in Blizzard's own clips (the survey's
  renders of A3A7 and A8F3 show the same).
- Played (dev harness, 1080p): every HUD state (`hud5_*`), the select screen (`sel4`), Swift Step, low health and
  allies (`r2_sheet`), chat (`hud5_rest`).

**Swift Step's slash arcs** (HANDOFF's missing effects; the effects research: 7 arcs of the half-cylinder shell 10ADC
with streak bands, size 3-3.7, 0.24-0.4 s, random rotation). A new effect shape, a curved band round part of a
cylinder in any orientation, spinning about its axis and fading toward its ends, drawn with the game's band mask:
seven at each arrival, each turned its own way. Played with the game frozen at the arrival (`arc_sheet`): swirls
round the arrival point among the petals, gone in a quarter second; her own arrival in first person stays clear.

**A kill feed** (critique #44; Overwatch's, top right). Who killed whom with what, for kills by a player playing a hero
and every player's death, seen within 64 blocks: names on their side's colour as each player sees it (blue theirs, red
the other), a hero's portrait beside their name, the weapon's icon between (the kunai's; an arrow for quick melee or a
mob's hit), this player's own kills framed white; six lines at most, six seconds each. Minecraft's toasts live in the
same corner: the feed starts below any showing. Played with two clients (`kf2_sheet`, `kf3_z`): Kiriko's kunai kill
on a zombie and quick melee on a pig (her portrait, framed), a zombie killing the other player ("ZOMBIE > VIEWER", red
and blue), the feed stepping down under the advancement and recipe toasts.

**She leans with her aim, seen from another player** (HANDOFF's missing third-person aim lean), **and a second dev
client to check it.**
- Overwatch's aim pose sheet (A71F, additive) under her: its data turned out to be three rows of 21 frames (aim yaw
  left, centre, right; each sweeping aim pitch, frame 31 neutral). Her body faces her aim, so only the centre row is
  used, on her named upper-body bones (its unnamed helper bones swing up to 177 degrees and its legs a little: left
  alone). The head turns only the pitch the sheet leaves (about 70%), or it bowed too far.
- Played with a second player: the first try had the row backwards (looking down raised her ofuda hand overhead);
  flipped, she leans back with the hand up looking up and bends forward looking down (`mpm_sheet`, `mpn_sheet`).
- `gradlew runClient2` starts a second dev client ("Viewer", its own `run2/` folder and harness port 25600:
  `OC_DEV_PORT=25600 py tools/dev.py ...`); harness `connect host:port` joins a server or LAN world (open one with
  `/publish true 25565`), `players` lists the players a client sees with their yaws.
- Played with Viewer as her ally (`mp1_sheet_v`, `mp1_sheet_k`): Viewer sees and hears her kunai and ofuda ("Ofuda
  thrown"), is healed from half health to full, sees her Swift Step arrive ("Kiriko appears", "Kiriko vanishes"), her
  Suzu flash white ("Suzu rings", "Kiriko: Protecting"), and her Kitsune Rush with the friendly line ("Kiriko: Let the
  Kitsune guide you!"), the gates and tiles; Kiriko sees Viewer's health pips over its name tag. A misreading on the
  way: from the side her idle looked turned 90 degrees, but her idle is a sideways fighting stance (`players` showed
  the right yaws; `mpgz_sheet`).
- Swift Step lands half a metre clear of the ally (it was 0.1 m: her camera was in the other player's face), closer
  only when there's no room. Played: onto Viewer from 9 m (`mp2_step`).

**A reload animation** (HANDOFF's missing list; seen every 15 kunai). The animation research had left one
start-middle chain unplayed (aliases 0x16E then 0x812, 0.2 s + 0.8 s) and guessed it was the reload; checked by eye
on the first-person survey renders: A49C flicks the held ofuda away, A49D brings the hands back crossed as fresh kunai
and ofuda appear. Its third-person twins (AD90, AD94) bring the arms in from wide to the hold. Added to both clip maps,
both models re-exported from the archived build (Overwatch closed), and played on R, on an empty clip, and when only the
ofuda run out (one animation for both, as in Overwatch). Played: first person stepped 3 ticks at a time with the game
frozen (`rl1p_sheet`), third person from the front frozen and in real time (`rl3p_sheet`, `rlrt_sheet`). One frozen
frame showed a leg raised; real time never did (the frozen stepping, not play).

**Allies' health over their heads** (critique #21; Overwatch shows its teammates' health the same way, white pips
over a cyan chevron). Players and pets always (40 m), villagers, golems and allays only when hurt or within 8 m (a
village would otherwise be a wall of bars); one pip per 25 health up to 20 (a golem's 1250 shares them out), smaller
further off, through walls as Overwatch's are, hero mode only. Played (`al1_*`): a villager near and full (shown),
one far and full (hidden), one far and hurt (half its pips), a tamed wolf 26 m off (4 pips), a hurt golem behind a wall
(20 pips, 60% lit); from 14 m further back (smaller, the near one gone at full health); in Minecraft mode none.

**Critique, second round: how she stands, low health, Suzu's white flash.**
- She faces her aim in third person (#10). Minecraft turns a player's body up to 75 degrees toward where they move; on
  top of Overwatch's strafe clip (legs stepping sideways under a body facing forward) that read as a split from the
  side. Overwatch's heroes face their aim and step in any direction with the directional clips, so her model now turns
  with her head (on a wall, still the wall). Reproduced first: strafing seen from the front showed her in profile
  (`st_front`); after, she faces the camera and skips sideways (`st2_sheet`, right, left and back). The Suzu throw's
  high kick from the critique's capture didn't come back (`st_sheet`, second row): the throw plays on her upper body only.
- Low on health (under 35%) the screen's edges glow red, stronger the lower, pulsing slowly (#20). Played: 38 of 250
  (`cl1_lowhp`), gone once healed.
- Protection Suzu turns whoever it cleanses white for 0.65 s, fading (#36; the effects survey's overlay 03C3): any mob
  through Minecraft's white overlay (a creeper's flash), the hero model, and her own arms in first person. The server
  sends each cleansed entity (`suzu_cleansed`). Played: a villager beside her and Kiriko herself, front view
  (`cl1_suzu_a`..`c`, `cl1z`), and first person (`cl1_suzu1p_a`).

**The critique's list, first round** (`tools/reference/critique-2026-10-26.md`: a critique sub-agent's 46 findings,
our screenshots measured against Overwatch frames; numbers below are its items).
- HUD:
  - No Minecraft block outline in hero mode (#3: it framed the crosshair at every wall climbed).
  - Cooling down: the icon stays, darkened, behind a ragged ink splash with drips, the seconds thinly outlined (#4).
  - Ult meter: 80 smooth ticks shading dark to bright orange, the unfilled ring almost bare, a fine dark outer circle, a
    brownish inner one, the disc 75% dark (#5, #26). Ready: the blue ring in four bands with a soft halo and white
    triangles at 3 and 9 o'clock (#14). It empties the moment Kitsune Rush starts; the "KITSUNE RUSH" caption is gone
    (#13).
  - Ammo text 18 px, anchored clear of the icons (#6). Key labels lower and narrower, filled white under a ready
    ability (#7). Ready and pressed boxes taller with a white underline, cooling-down boxes shorter and lower (#22).
  - The panels' sloped lines are smooth (soft-edged textures, not stepped fills), with a visible glow (#23). Bigger mice,
    white with a dark outline and the button split showing (#24).
  - The volley bar's segments lean 45 degrees and tilt 12; spent ones are light outlines over a dark fill (#25).
  - Health number smaller, pips 24 px wide, a partial pip filling from the left (#29). HUD text has a soft shadow
    instead of Minecraft's hard grey one.
  - Found on the way: Minecraft's mipmapper treats any texture with transparency as a cutout and adds 2.5% alpha to
    every pixel of the smaller mips, which drew a faint box round each HUD shape (seen round the volley bar against the
    sky). HUD and effect masks now average plainly (`HeroTexture` takes a mipmap strategy).
- Hero select: the model large from mid-thigh up, its legs cut off above the roster (#32); no hotbar, HUD or held hand
  behind the screen (#34). The Esc menu shows the sub-role too (#42).
- Effects:
  - The homing ofuda's gold is blended rather than added (added over grass it turned lime), its glows smaller; the same
    for its heal flower (#1). The ground hit's glow kept under a metre (#39). New: soft (blended) effect textures and
    trails (`EffectArt.get(.., soft)`, `Fx.soft`).
  - Suzu's glows lifted off the ground: a camera-facing glow centred on it was cut off flat (#8). Its 5 s ring soft
    gold (#38).
  - Her own Swift Step arrival in first person: no flash or puff on the camera (the white disc by LSHIFT), the petals
    flung out from her feet (#9). Petals tinted teal and narrowed (they were round white dots, #15).
  - Torii gates translucent, fading out within about 3 m of the camera, per corner (#12, in part: no fox yet).
  - Climb dust in the game's light warm colour (#40: it came out dark brown).
- Not bugs: the white pixel squares round the healed villager in the critique's captures are Minecraft's death poofs
  from the villager the effects test kills just before (seen again, then checked with the heal effect alone). #41
  asked for ofuda on the left button: the owner chose the right (HANDOFF). #33 (abilities shown under the name): ours on
  purpose, Overwatch's details are still behind F1.
- Played (dev harness, 1920x1080): every HUD state (`hud4_br_sheet`, `hud4_ult_pair`, `hud4_mid_pair`: rest, spent,
  cast, cooling down, both cooling down, reload, ult ready, rushing); every effect (`fx7_sheet_a/b`); Swift Step and
  Suzu in first person with the game frozen mid-effect (`p1_sheet`, `p1_burst`); a fresh survival world (`pb1_*`,
  `pb2_*`): the select screen, joining, a night fight with two zombies on Normal (killed one with kunai, the other
  killed Kiriko: health read 204, 67, 9, then dead; respawned with the hero HUD back), Suzu at her feet, ofuda, Swift Step, Minecraft
  mode's HUD and pause menu, the Overwatch menu, a wall up close in both modes (outline only in Minecraft mode).

**The game's own sounds** (owner: "it should use actual assets every time there is one in overwatch"). A sound
sub-agent traced which of Kiriko's effects plays which sound bank in the archived build, which banks are Wwise layer
containers (several files played at once) and which audience hears each (her own player or everyone else):
`tools/reference/kiriko-sounds.md`; the sources and how they combine are in `tools/soundmaps/kiriko.txt`.
- `build_asset_pack.py sounds` mixes the layers Overwatch plays together into one file per variant with ffmpeg (each
  layer's delay and gain from the map, mono so Minecraft can place it, kept below full scale: the research found hits
  and Swift Step's cast clipping), up to 8 variants an event: 44 events, 199 files in the local pack. Without ffmpeg it
  copies the first layer only.
- Kiriko's own player hears the first-person recordings and everyone else the third-person ones (throws, the kunai
  reload, Kitsune Rush's cast); the headshot ding only reaches her. Swift Step: she hears her hand sign, the others her
  departure.
- New: each of the six torii gates chimes its own note, a "not now" click when Swift Step has no ally in view, the
  climb's start and its pull over the top, the third-person kunai reload. The ofuda's reload sound is gone: Overwatch
  has none (one reload for both weapons).
- Still Minecraft stand-ins (their banks weren't extracted): a kunai hitting a block, the melee swing. Not yet: the
  looping sounds (kunai and ofuda in flight, the rush's path).
- Harness: `soundinfo <event>` lists the files behind an event (checked: kunai.throw, kunai.throw_3p, ofuda.heal,
  rush.gate_3 and voice.swift_step come from the pack).
- Played with subtitles on (`snd_real2`, `snd_real3`, `snd_real_sheet`): kunai (left button), ofuda (right), Suzu,
  Swift Step with no ally (the click) and onto a villager ("Kiriko appears", "Kiriko: I'm here"), a reload ("Kiriko
  reloads kunai"), Kitsune Rush ("Kiriko gathers spirit", "Torii gate rises" six times). No sound errors in the log.

**Kitsune Rush's path is paved with talismans** (the effects research: two tile effects, a rounded card about
0.56 x 0.29 lying flat for 11 s; the reference video: "teal glyph tiles cover the ground ahead"): three glowing card
tiles across the corridor every 1.5 m of the fox's path, held while the rush lasts. Played: the rush in third
person from behind and in first person (`rush_tiles_sheet`).

**The hero HUD rebuilt like Overwatch's** (#8; spec: the owner's gameplay screenshots plus
`tools/reference/kiriko-reference.md`, a reference sub-agent's measurements from the same video at 1080p).
- Laid out in pixels of a 1080-high screen and scaled to the window, so it looks the same at any GUI scale; both side
  groups curve up toward the centre as Overwatch's do (2.6-4.4 degree tilts, one curve formula).
- Bottom left: the portrait cut to a shield with a light-blue border, a "+" box, `250 | 250` (max smaller and dimmer),
  one white pip per 25 health (a nearly full pip reads full), a name plate with a glowing blue underline.
- Bottom centre: the ult meter: a dark disc, 60 radial ticks filling clockwise from 12 in orange, the charge in
  italic Big Noodle; ready: a white disc with the fox emblem in a glowing blue ring, the Q label inverted; running:
  white ticks counting down the rush.
- Bottom right: ability boxes (ready off-white with a black icon, just used coral, cooling down dark with an orange
  fill rising and the whole seconds in an outlined italic number, a cyan flash when back), key labels under them;
  the ofuda and kunai panels between bright blue lines, `ammo | clip`, and a mouse with the fire button in orange.
- Middle: Overwatch's "+" crosshair instead of Minecraft's (and its attack indicator); the five-segment ofuda counter
  under it (one segment a volley, spent ones from the right as dim outlines, a white flash on the refill); Swift
  Step's target as cyan corner brackets in a diamond on the ally, with its key.
- Minecraft mode keeps Minecraft's HUD plus a small ult meter right of the hotbar and "G: HERO MODE".
- Chat moves up above the portrait in hero mode (it sat on the health).
- Fonts: Overwatch's own (Futura No.2 D DemiBold for numbers and labels, Big Noodle Too Oblique for big numbers),
  read from the player's install into the local pack (`build_asset_pack.py fonts`; a fonts sub-agent found them in
  the game data: `tools/reference/fonts.md`). The jar's font files only point at Minecraft's font; `.ttf`/`.otf` are
  now on the build's forbidden-assets list.
- Shapes (discs, rings, the ofuda segment, the mouse) are drawn at load as smooth masks (HudShapes): original, not
  from the game.

**A sound for every moment** (owner: "Sounds should also very much use the real assets"). 22 new sound events:
kunai throw / hit / headshot / hit on a block / reload, ofuda throw / heal / hit on a block / reload, Suzu throw / burst,
Swift Step cast / departure / arrival, the wall climb's scrape, Kitsune Rush's summon and each torii gate, quick melee
swing and hit, and voice lines on Swift Step, Suzu and healing (spaced 3 s apart, by chance). The jar plays vanilla
stand-ins (the old pitches); the local pack replaces them with the game's recordings (being mapped by a sub-agent; the
voice lines stay silent until then). Subtitles for each, and the subtitle box moves up in hero mode (it covered the ammo).
- Played with subtitles on: kunai, ofuda and Suzu throws show "Kunai thrown", "Kunai strikes", "Ofuda thrown",
  "Kiriko draws ofuda", "Ofuda scatters", "Suzu thrown", "Suzu rings" (`snd_subs*`).

**Hero select like Overwatch's 2026 screen** (#7; spec: the reference's section 6, from the same video at t=500).
- The hero itself large in the middle: the real 3D model (the player drawn as the previewed hero, following the mouse)
  when the pack has it; otherwise the hero's card art fading out at the knees, or the bust (Sombra and Illari have no card
  art in the extraction). "HERO SELECT" top left in Big Noodle, the name huge top right with role, sub-role and health
  (sub-roles from the wiki's infoboxes: Kiriko medic, Lucio and Sombra tactician, Illari survivor; the wiki lists Sombra
  as a support since 2026, so her role stays as it was).
- The roster along the bottom in role groups with a role badge, header and rule; the pick framed in orange with its
  name tag; heroes not playable yet dimmed "SOON"; an orange READY button (grey "COMING SOON" for those). Key hints along
  the bottom: F1 hero details, Esc keep hero / play as yourself.
- Overwatch hides the abilities behind F1; here their icons and keys show under the name, and F1 adds names and
  descriptions in a panel.
- Played: the screen on joining (screenshots `select_v2`..`v6`), F1 details on and off, arrows through all four heroes
  (Kiriko's model, Lucio's faded card, Sombra's bust), READY by click and by Enter picks Kiriko and enters hero mode.

**First-person screen effects** (the reference, section 2):
- Swift Step: the screen's edges flash teal and streaks rush in as she steps.
- In Kitsune Rush's path: a cyan mist at the screen's edges, pulsing, and light streaks flying out past.
- Each ofuda throw: a small soft golden puff at her fingers.
- Effects fade out as they come near the camera (a petal from her own arrival covered the HUD); the hand puff opts out.
- Played: Swift Step and rush in first person (`scr_sheet`), ofuda in the open (`puff2`: first try was a big yellow blob
  with the talismans' own glow blazing at the hand; now small puffs and faded talismans near the camera).

**Her coat no longer clips through her legs** (owner: "kiri's fabric things being clipped into legs"). Overwatch moves
cloth bones only with physics, never with clips, so her 24 coat-tail chains (7-8 bones each, hanging from the hips)
stayed rigid while the thighs swung through them. New ClothSim: per frame, each chain is pinned at the waist, pulled
back toward its drape (firm at the top, loose at the hem), swings with gravity, trails behind her motion, keeps its bone
lengths, and is pushed out of capsules around the thighs and shins, lifting outward over a leg rather than parting around
it. The trouser cuffs (5-bone chains on the knees) are left as they are. Stiffness, damping and radii are ours (the
game's cloth settings aren't in what we read).
- Reproduced first: strafing in front view, her thigh passed through the coat panel (`cloth0_g`). After: the coat
  swings and trails while running (`cloth1_*`), and the strafing leg goes under the coat, not through it
  (`cloth2_f/g`). A first try pushed points straight off the leg and the chains parted around it: still clipping.
- **The Suzu bell no longer sits in her hand while she runs, strafes, jumps or falls**: those clips carry it (the props
  survey), the game hides it until the throw; now it shows only during the throw (seen in `cloth2_*`).
- Suzu's flash is smaller (a burst at her own feet whited out the screen in third person).

**Real Overwatch effects instead of Minecraft particles** (owner: "I noticed you are still using minecraft assets like
particle effects for ofuda trails. Thats not how ofuda look in game"). An effects sub-agent read Kiriko's effect files
from the archived build (their particles, ribbons, meshes, timings, colour ramps: `tools/reference/kiriko-effects.md`;
DataTool can't export effects, so it parsed them itself); `build_asset_pack.py effects` copies the masks and converts
the torii gate mesh (new `tools/owm_static.py`) into the local pack.
- New client effect system (client/effect): camera-facing sprites, flat decals and rings, streaks, a dome, meshes and
  ribbon trails, drawn additively through a registered pipeline after the world, on the game clock. Masks coloured at
  load with the game's gradient maps (teal, homing gold, kunai cyan, Suzu blue and gold).
- The server sends effect events (EffectPayload) instead of particles. Every Minecraft particle the mod used is gone
  (ofuda dust, kunai crits, Suzu dust/end rods, Swift Step cherry leaves, Kitsune Rush dust and soul fire, the melee
  sweep).
- Ofuda: a ribbon and a glow on each talisman, teal or gold when homing; a five-petal flower on the healed ally, a
  flower decal where it hits the ground. Kunai: cyan ribbon and lens glints; six cyan sparks off a wall. Suzu: ribbon
  and sparkles; the burst's dome, blue ground wave, flower, flash, and a thin gold ring held 5 s. Swift Step: teal rings,
  a petal burst, a flash at both ends. Kitsune Rush: teal smoke at the cast, **six glowing torii gates (the game's own
  mesh) rising as the fox passes**, ground ripples, speed lines on allies, petals from the fox, a puff when it vanishes.
  Wall climb: dust puffs at the hands.
- Swift Step now lands beside the ally on Kiriko's side (it put the camera inside the villager's head) when there's room.

Played through the harness (screenshots `run/screenshots/fx2_*`, `fx3_*`, `fx4_*`, `fx5_*`): ofuda homing on a
villager (gold), ofuda at the ground (teal flowers), kunai into a wall (sparks), Suzu at the ground (dome, wave, gold
ring), Swift Step to the villager from behind, Kitsune Rush from behind (gates, ripples), wall climb (dust). Fixed on the
way: glows stacked into a blown-out yellow blob (one glow per projectile now), Suzu's ring lit its whole 8 m disc (rings
use only their bright edge now), the gates' beams stood upright, the climb dust spawned inside the wall.
- Not yet: real sounds (being mapped), the effects' enemy-red view, Suzu's white outline on allies, the floating talisman
  slabs on the rush path, slash arcs at Swift Step's arrival.

**Overwatch's controls and menus in hero mode** (owner: "Hitting G toggles between all keybinds. So esc in minecraft
mode opens minecraft settings. Esc in hero mode opens overwatch settings").
- Hero mode has its own key set (HeroControls, saved in `config/overcraft-hero-controls.json`): move, jump, crouch,
  primary/secondary fire, abilities, ult, reload, quick melee, interact, change hero, Minecraft mode. In hero mode
  Minecraft's gameplay keys are silenced (moving, attack, use, pick, drop, inventory, offhand, sneak, sprint); in
  Minecraft mode the hero's are. Chat, menus, F-keys and the hotbar's number keys work in both. The scroll wheel no
  longer changes slots in hero mode (it dropped you out of hero mode).
- Defaults are Overwatch's keys, with the owner's fire buttons (2026-10-25): **LMB kunai, RMB ofuda**. The options name
  them Overwatch's way (Primary fire = Healing Ofuda, on RMB; Secondary fire = Kunai, on LMB). For a few hours of this
  night the defaults were Overwatch's own (LMB ofuda) by mistake; put back before anyone played it.
- New Interact key (F, Overwatch's): doors, chests, buttons and villagers in hero mode, since the fire buttons don't.
- Esc in hero mode opens Overwatch's menu (big italic items: Resume, Options, Change Hero, Minecraft Menu, Save and
  Leave); Esc in Minecraft mode opens Minecraft's. Options: tabs (Controls; Video, Sound and Mouse open Minecraft's
  own screens), the hero's actions by section with the hero's names ("PRIMARY FIRE  HEALING OFUDA"), click a key box
  then press a key or mouse button (Esc cancels, Delete clears), shared keys in red, Restore Defaults.
- Minecraft's Controls screen now lists only G (hero mode) and H (change hero) under Overcraft.

Played in the dev client at 1920x1080 through the dev harness (the desktop was locked, so no real mouse; new harness
commands `press <key>` (a real key event through Minecraft's keyboard handler), `screenclick x y`, `window w h`,
`tutorial`, `ult <percent>`), screenshots `run/screenshots/hud_v2_*`, `menu_v1`, `options_v2`, `hud_rebound`:
- HUD at rest, ult 59% (ticks to 7 o'clock), Suzu cooling down (12, orange fill low), ofuda 2/10 (one segment lit,
  four outlines), health 175/250 (7 pips white, 3 dark), Minecraft mode (small meter, hint). Compared region by
  region with the owner's screenshots at 2-3x zoom.
- Keys: W moves 5.5 m/s; E throws Suzu with no inventory; LMB throws ofuda (10 to 4), RMB a kunai (15 to 14); G to
  Minecraft mode and back, staying put both ways; E in Minecraft mode opens the inventory and throws nothing;
  crouch; wall climb up an 8-block wall and over.
- Menus: Esc in hero mode opens the Overwatch menu, Options, rebinding Ability 2 to C (saved; C throws Suzu, E does
  nothing, the HUD label reads C), Restore Defaults, the Video tab and back, Minecraft Menu (vanilla pause), Change
  Hero (select screen); Esc in Minecraft mode opens vanilla pause; F opens a chest while RMB at it throws a kunai.
- Not tried: the real mouse and keyboard, scroll wheel, a second player, a controller, other GUI scales and window
  shapes than 1920x1080 and 854x480.

## 2026-10-25

**Kiriko's real model in game** (branch `feature/kiriko`; the model itself stays local, #6). Overwatch build 154088
downloaded to the archive drive and read with DataTool (offline, Overwatch closed); Kiriko's model, 330 clips, the
Kitsune fox, her weapons and 2265 voice lines extracted (`E:\Overwatch Archive\README.txt`). New tools:
`tools/owm_survey.py` (Blender + overtools' io_scene_owm: survey clips, export a .glb with picked clips) and
`tools/owm_previews.py`; which clip is which: `tools/clipmaps/kiriko.txt` (the owner spotted the wall climb).
- HeroAnimator: finds Overwatch skeletons' bones (`Spine_1`, `Head`) as well as ValveBiped; 8-direction run and
  crouch-walk (falling back to 4 directions); run/crouch clips play at a rate that matches the ground speed (the
  clips cover 3.4 / 2.7 m/s, heroes move 5.5 / 3); climbing draws the model 0.3 m back from the wall (Overwatch's
  climb has the hands 0.6 m ahead of the feet: in Minecraft they were inside the blocks).
- HeroModelRenderer: held props the current clip doesn't use (Kiriko's Suzu, ofuda) stay hidden instead of lying at
  her feet. HeroTexture: model textures filtered smoothly with mipmaps (DynamicTexture's nearest-neighbour sampling
  made a 2048 px texture look like pixel art).
- Exports at Overwatch's 30 fps (Blender's 24 made every clip 25% slow).
- Dev harness `state` reports the playing clip (`anim`).

Played in the dev client through the dev harness (keys, camera, screenshots in `run/screenshots/k_*.png`; desktop
control was declined from the phone, so **not yet played with the real mouse**), fresh flat world "KiriTest":
- Join: hero select shown; Kiriko picked (harness `hero kiriko`). Renders textured, right way round (front: face and
  mask; back: hair), feet on the ground, idle holding a kunai, nothing at her feet (k_41/k_42).
- Run forward, back, left, forward-right: each plays its own direction's clip mid-stride (k_sheet_run).
- Ctrl crouch: crouch idle, then crouch-walk lower than the run, higher than the crouch idle (k_sheet_heights).
- Jump and fall from 12 blocks: feet-tucked air pose, then `land`; no fall damage (health 20 before and after).
- LMB kunai: arm throw over the legs' pose, kunai flies, ammo 15 to 14. RMB ofuda: arm up with talismans, ofuda fly.
  E Suzu: flies, 12.9 s cooldown, no body animation (clip not found yet).
- Wall climb into a 10-13 block stone wall: `climb` steady for the whole climb (15 ticks sampled), hands reaching
  overhead at the wall (k_sheet_climb4); before the 0.3 m shift the arms were hidden in the wall (k_sheet_climb3).
- Water: wades with the run cycle (no swim pose). /kill: death screen; the body falls as the Minecraft player (#9).
- Rejoin: hero select again and the hero reset (#9 asks whether that's intended).
- Not tried: mobs, allies (Swift Step, ofuda healing), Kitsune Rush, respawn, slots 2-9, second player, the Nether.
- Filed: #7 (select screen like Overwatch's; its cards also overlap the kit text), #8 (HUD like Overwatch's), #9
  (animations still missing: death, swim, Suzu, Swift Step, ...).

**Later the same day** (still uncommitted, waiting for the owner's real-mouse play), played through the harness:
- Death (#9): the renderer remembers each player's hero, so a dying hero keeps the model and plays `death` (the weapon
  is gone the moment they die). Seen: she falls backward, then Minecraft's death puff (k_sheet_death2). Respawn
  (new harness command `respawn`, what the death screen's button does): back to `idle` as Kiriko, hero select opens.
- Climb over the top (#9): running out of wall while still rising plays `climb_top` (5-block wall: 13 ticks `climb`,
  then ~0.3 s `climb_top`, then airborne).
- Suzu throw and Swift Step clips (#9): found by which clips move the held props' bones (`throw_suzu` = AD82, an
  overhand lob; `swift_step` = A50A, arm thrust forward: both guesses until compared with footage).
- Kiriko's kit against real targets: kunai on a husk 20.0 -> 15.28 health (60 Overwatch damage less its 2 armor:
  exactly Minecraft's armor formula); ofuda heal a hurt villager 10.0 -> 20.0 in 2 s (the first try read a village
  villager by mistake: not a bug); Swift Step z -29.5 -> -23.5 next to the villager, 7 s cooldown; Suzu 14 s
  cooldown; Kitsune Rush with the ult filled by advancements (2725 = the cost): 7.15 m/s in the corridor (5.5 +30%).
- Kitsune Rush's fox: a KitsuneEntity (not saved, not hittable) follows the corridor's head at 20 m/s and vanishes in
  a puff when the run ends; clients draw it with the fox's Overwatch model galloping (hero_models/kitsune.glb, clips
  in tools/clipmaps/kitsune.txt), full bright. Seen from the side running out of Kiriko (k_sheet_rush).
- UI from the game's own art (#7, #8; owner: "Ui should use game assets as well"): `build_asset_pack.py ui` copies
  each hero's portrait, card, full-body art, ability and perk icons (DataTool extract-hero-icons) into the local pack,
  with .mcmeta blur for smooth scaling. Select screen: card art large on the right, ability icons in the kit, real
  portraits in the roster ("soon" greyed), key column sized to the longest key, descriptions only when there's room
  (k_102). HUD: portrait by the health, ability icons dimmed with the seconds left, kunai icon by the ammo (k_101).
  Without the pack, the old drawn placeholders.
- Seen, not fixed: Hero.SOMBRA is listed as a Support; she's a Damage hero in Overwatch.
- Quick melee, every hero's (key V, Overwatch's default): 40 damage to every enemy within 2.5 m in front (45 degree
  half-angle: ours), very small knockback, 1 s cooldown from the press, 0.528 s weapon recovery (wiki "Melee").
  Played on a husk: 20.0 -> 16.85 (40 less its armor), a second V 0.5 s later did nothing (cooldown), a third after
  the cooldown 16.85 -> 13.70; the animator played `melee_2`.
- Role badges from dump-ui-textures (tank, support, damage) beside the role on the select screen (k_105).
- Treading water plays the air pose (Overwatch has no swimming).
- First person: Overwatch's first-person clips aren't in the extraction (all 330 are 3rd-person; see HANDOFF).
- Filed #10: Mercy (owner).

**Owner's play notes, fixed** (owner played the jar; then real mouse/keys via desktop control where Windows allowed,
the harness otherwise; screenshots run/screenshots/k_12x-k_13x and the h_* captures in the session scratchpad):
- Projectiles drawn as flat camera-facing sprites: now the kunai/ofuda/Suzu Overwatch models (projectile_*.glb),
  long side along the flight. Suzu seen as its 3D bell in flight; kunai too fast to catch on camera.
- Climbing a short wall launched the model above it: climb_top's root motion (bone_0001 rises 2.2 m) is now ignored
  (Minecraft moves the player). 3-block wall: climb, climb_top, lands on top, model stays with the player.
- Kunai throw "jumps into her": the clip starts at the release (arm cocked behind, left hand); actions now swing out
  to their first frame over 0.1 s, then play. Seen: arm out, then sweeps forward.
- Idle was the "hackles up" pose (B00B); B033 tried next is airborne (feet 0.15-0.4 m up). Neither is right: a
  research agent is mapping her animation data (tools/clipmaps/kiriko-research.md when done).
- Crouch idle (CFF8) is a phone-typing emote (owner): dropped; standing crouched holds the crouch-walk pose.
- Minecraft mode (slots 2-9) showed Alex: the chosen hero now syncs to every client (attachment registered at mod
  start: Fabric agrees on synced attachments while connecting), so players keep their hero's model out of hero mode.
- Couldn't jump onto a 1-block step except facing it (wall climb): Overwatch's jump peaks at 0.98 m. Ledge assist
  (ours): in the air, pushing into a ledge at most 0.3 m above the feet lifts the hero onto it. Before: strafing
  into a step stuck at x 41.7; after: strafing, backing up and facing all cross it.
- Ofuda throw made her look up: that clip (B104, a guess) is dropped until the research says which is right.
- Real sounds: OcSounds + the jar's sounds.json (vanilla stand-ins), replaced by the local pack (`build_asset_pack.py
  sounds`, tools/soundmaps/). Kitsune Rush plays "Let the Kitsune guide you!" to Kiriko and allies and the Japanese
  line to enemies, from 64 blocks. Seen as the subtitle "Kiriko: Let the Kitsune guide you!"; not heard (no audio).
- First person (owner: "should look like gameplay"): hero mode draws Overwatch's first-person view model instead of
  Steve's hand. It's Kiriko's own model imported through the Gameplay1P entity (its model look textures only the
  arms/hands, which the 3P export dropped as material-less), exported as hero_models/kiriko_1p.glb with the 1P clips
  (tools/clipmaps/kiriko_1p.txt, from the research), placed with its Camera bone at the eye. Seen: both hands in
  front, ofuda fanned in one hand (k_150); kunai throw plays (k_153-155). Not yet: breathing/sway layers, walk bob,
  Minecraft's view bob/sway on the arms.
- Standing and crouched idles: A6F0 / AEE7 held at frame 0 (the research: their frame follows a game parameter,
  turn-in-place, and holds otherwise). Owner: "much much better".
- Not working with desktop control: F5 and two keys at once (W+Space) can't be sent; computer-use screenshots show
  the game as blank (plain screen captures work); Windows' text-input overlay can block clicks.

**Kiriko, first pass** (branch `feature/kiriko`, not on `main` yet: not played in-game). Compiles, unit tests pass
(units, run speeds, jump apex 0.98 m, 30 m/s fall cap, 0.1 s ground acceleration, Suzu's 7 s straight-up flight).
The dev client launched with the mod loaded, but couldn't be played this session (desktop control needs the owner's
approval in a live message), so **none of this has been played by hand yet**.
- Hero select screen on joining a world (Kiriko playable; Lúcio, Sombra, Illari shown as coming soon); H within 5
  blocks of your bed (or world spawn without one) to change heroes (owner); the pick on joining is free.
- Hero mode: G toggles (owner), or hotbar slot 1. The hero weapon lives in slot 1; vanilla hotbar, hearts, food,
  armor and XP bar give way to the hero HUD (health in Overwatch numbers, ammo, cooldowns, ult %).
- Overwatch movement in hero mode: 5.5 m/s (90% back, 93.3% diagonal back), ~0.1 s accel/stop, jump 5.72 m/s
  (0.98 m), gravity 16.7 up / 17.5 down easing to 30 m/s, Ctrl crouch 3 m/s, no sprint, no fall damage. Air control
  value is ours (none published). The jump uses trapezoid integration: Minecraft's gravity-then-move order peaked at
  0.83 m (caught by the unit test).
- Kiriko (numbers from the Oct 2026 patch notes and the wiki, cited in KirikoStats): kunai LMB (60, 2x head, 0.5 s,
  15 ammo, 90 m/s), Healing Ofuda RMB (12 x2, 7/s, 10 ammo, 0.9 s reload, 18 m/s, 30 m homing lock), Medic 40%
  self-heal, Swift Step (30 m, 7 s, 0.2 s cast, through walls), Protection Suzu (14 s, 4 m, 80 heal, cleanse, 0.5 s
  invulnerable), wall climb (8.3 m/s, 1 s, once per airtime), Kitsune Rush (2725 points, 32.2 x 10 m corridor for 8 s:
  +30% speed, +50% attack/reload, 2x cooldowns, 1 s linger). Global regen after 6 s.
- Ult charge: 5/s, 1 per damage/healing, and advancements (owner): task 10%, goal 25%, challenge 50% of the cost.
- Placeholder art (tools/gen_textures.py) and vanilla sounds until hero assets come from the owner's install.
- Local Overwatch asset pack (#6): the mod mounts `<game dir>/overcraft/overcraft-assets.zip` (dev: `local-assets/pack/`)
  as an always-on resource pack; `tools/build_asset_pack.py` converts a model with Blender into it (checked with a
  test cube); `gradlew build` fails if the jar contains a model/mesh/audio file (checked: a .glb in resources failed
  the build). Nothing renders the hero model yet: that comes with the first real model. DataTool v2.25.0.0 can't read
  the installed build 154504 yet (supports up to 154088): waiting on upstream.
- Issues #1-#5 filed (Juno, Sojourn, communication wheel, emotes, voice lines).

Project started. Fabric for Minecraft 26.3 (Loader 0.20.1, Fabric API 0.165.0+26.3, Loom 1.19); 26.x ships
unobfuscated, so there are no mappings to pick. CLAUDE.md carries over Lethalcraft's working rules.

# Brigitte (hub)

Everything learned about Brigitte, by subject. The eighth hero (issue #65), fourth on the owner's 2026-10-28 list: a Support,
Survivor (BZ 2026-02-10). Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · built the way Sombra was: [sombra](sombra.md)

## Her notes and sources
- **Numbers:** `tools/reference/brigitte.md` (Blizzard's patch notes to 2026-11 and the wiki's revision 408525, each number
  with its history). No patch has touched her since the data build (BZ 2026-09-08 was the last), so the data's numbers
  are the live ones.
- **The data map:** `tools/reference/brigitte-map.md` (her kit from her graphs, node by node, H / M / L); the detail in
  `E:\Overwatch Archive\research\brigitte\` (`work_kit\`, `work\assets.md`, `work\clipmap.md`, `extract\`, build 154088).
- **Her effects:** [brigitte-effects](../../tools/reference/brigitte-effects.md) (each moment's recipe, the barrier field's
  look, palettes `BRIGITTE_*`, pack masks `brigitte_*`); the lesson (her visuals are clip effects, not graph effects):
  [reading_game_data](../game-data/reading_game_data.md#where-a-melee-heros-effects-live-brigittes-pass-2026-11-01).
- **Her sounds and voice:** `tools/soundmaps/brigitte.txt` (every event, its trigger, own / 3p, the Voice.java stimulus
  lines) and her section of `tools/soundmaps/emotes.txt`; the lessons (hit sounds in contact sets, effects hidden in
  expressions, charge-picked copies): [sounds](../sound/sounds.md#brigitte-sounds).
- **The plan:** `tools/reference/brigitte-plan.md` (what maps onto the mod: armor and a barrier are new; the order).
- **Clip maps:** `tools/clipmaps/brigitte.txt` (3P), `brigitte_1p.txt`, `brigitte_flail.txt` (the handle, exported as
  `hero_models/brigitte_weapon.glb`), `brigitte_flail_head.txt`, `brigitte_shield.txt`.

## What step 1 taught
- **The `--entity` import crashed headless Blender** for every one of her models (the add-on raised an error popup,
  `popup_menu_end` with no window: an access violation). Without it every model exported; her first-person arms got
  their materials anyway (no "no colour texture" line). Run one export alone first when a hero's exports all fail.
- **Three models besides her body:** the flail's head sits on the handle's socket 0254 (bone_0FCD), and her shield
  folded on her back (01D8, bone_0F74). `HeroWeapon.extras` now returns every extra model (Ana's sidearm was one).
- **Her view model has no walk clip:** idle, jump, fall and land, with movement additives; Ana's first-person rule
  fits (the idle while she moves).
- **No showcase clips extracted** (C06C / C07D / C0B8): the select screen poses her in her idle, and the weapon and
  extras are drawn from that pose as her showcase entity has them (the flail on 000A, the shield folded on 01D8).

Played (step 1, dev client, fresh creative worlds `brig1`-`brig3`): picked from the select screen (her model, "Support,
Survivor, 250 health", her seven kit icons; after the fix the flail in her hand and the shield on her back); in play, first
person (the flail in her right hand, Repair Pack's 3 charges, Whip Shot and Shield Bash icons), third person front and
back (the flail and its head, the shield folded on her back), running, jumping, crouching. Lucio's and Jetpack Cat's
select screens unchanged. Screenshots b_*.png, sel_*.png, not committed.

## What step 2 (armor) taught
- **Armor is a layer like shields** (`Hero.layer()`, kept in `HeroState.shield`): hits come off it first, heals fill the
  health under it first; it never regenerates; a hit while any armor is left is 7 smaller, at most halved
  (`Shields.armorReduce`, in the damage mixin after the boosts). Ours: the whole hit is reduced where Overwatch reduces only
  the part landing on armor (M); beams and damage over time aren't told apart yet.
- **A fresh flat survival world has mobs from generation** (205 killed, slimes among them): `spawn_mobs false` stops new
  ones only. A slime's unseen hits looked like a bug in the armor until `kill @e[type=!player]` ran first.
- **Adding a hero shifts the select screen's portrait row** (9 portraits now: Kiriko 142 ... Brigitte 266 in GUI
  coordinates at 854x480): the harness clicks for each hero moved.

Played (step 2, dev client, survival, flat world `armor1`, natural regeneration off, every mob killed first; health polled
every 50 ms): a zombie's 37.5 hits landed as 30.5, 30.5, 30.5 (armor 75 -> 44.5 -> 14 -> 0), then 37.5, 37.5; a
skeleton's arrows 62.5 and 50 as 55.5 and 43; `/damage 12` (150) as 143. HUD: seven white and three yellow pips at full;
after a heal to 158, the health pips filled and the armor's stayed empty; full again, yellow back. The select screen says
"175 health, 75 armor". Lifeweaver after the change: a hit of 37.5 in full, his shields back from 3 s at 30 a second.

## What step 3 (Rocket Flail, Inspire) taught
- **The flail is a sweep, not a ray:** each tick of the hit window (0.2-0.4 s) widens the arc covered (-17.5 + 35 t^2
  degrees, the backhand mirrored), and every enemy within 6 m (nearest point of its box) whose side is in the arc, in
  sight, takes 45 once per swing. Her quick melee starts the same swing. Magic damage: Overwatch's flail pushes no one.
- **An iron golem is an ally** in the mod (`Allies`): a flail test on one did nothing, rightly; use a ravager for a big
  target.
- **Open for step 8:** the flail's head flies out on its chain in a swing (entity 2283 flung, chain beam 216C / 33C1); the
  swing clips play on her, not yet on the head.

Played (step 3, dev client, survival, flat world `flail1`, every mob killed first, NoAI targets; health read with `data
get`): one swing at a zombie 4 m ahead: 45; one swing at three zombies in the arc (4-5 m): 45 each, a zombie at 7.5 m and
one 90 degrees to her side untouched; the button held 3 s on a ravager: 225 (five swings, one per 0.6 s); a zombie behind
a stone wall: untouched; the quick-melee key: 45. Inspire from one swing: a villager 10 m off at 10 health gained 57
(12 at once, 45 over 4 s), one 25 m off nothing, she healed herself too. First-person and third-person swings looked at.

## What step 4 (Barrier Shield, Shield Bash) taught
- **The barrier is her state, not an entity:** a rectangle 0.9 m ahead, square to her facing, 1.51 x 2.64 m (the deployed
  shield mesh measured: no number in any graph). A hit on her or her ally crosses it when the way from its source to them
  passes through: a projectile's way in (6 m back along its flight), a blast's centre, an attacker's eyes.
- **A fully stopped hit must not happen at all:** with its damage cut to 0 at `hurtServer`'s head, Minecraft still knocked
  her 3 m back, and the next arrow reached the villager behind her. Now a cancellable hook takes it first: all of it, no
  hit and the projectile gone; part of it, the rest lands as its own hit (`Barrier.passing` stops it counting twice).
- **TNT is a blast with a direct entity and no attacker:** it went through until any non-creature hit with a source
  position counted as a blast.
- **Explosions push apart from their damage** (`ServerExplosion.hurtEntities`: `pushFromExplosion`, and a player's push
  sent to their client through `hitPlayers`): `ServerExplosionMixin` zeroes both behind an up barrier.
- **Broken, it refills through its lockout** (from 2 s after, at 85/s: 0EE4 has no other condition): it first came back
  up with 5 health.
- **Shown where it blocks:** hung on her hand's bone it stood along her arm over her head; drawn at the rectangle instead,
  upright, the frame unfolding (its deploy clip then idle_up). In first person drawn in the camera's frame, ahead.
- **Shield Bash on her client** (as Lifeweaver's dash, `OwMovement.dash` now asks both): 0.3 s at 22 m/s; the server ends
  it early on a hit ("brigitte_bash_end"). Measured 5.3 m on open ground (D reading about 6.6 m, M; W 8.5 m).

Played (step 4, dev client, survival, flat world `shield1`, every mob killed first, the barrier refilled between cases):
- Arrows from a skeleton 12 m ahead, shield up: absorbed (300 -> 257), no knockback; down: hit (43 after armor) and
  pushed back; from behind with it up: hit (earlier run). An allied villager behind her: untouched over 9 s (two arrows
  absorbed).
- A zombie walking up: its hits into the barrier (37.5 each); once it walked round to her side, its hits reached her.
- A creeper's blast 3 m ahead: absorbed whole (300 -> 98), no push after the fix (4 m before it); down: 202 to her.
- Two TNT ahead: through it before the fix; after, the barrier broke and the rest killed her (beyond 300).
- `/damage 30 explosion at` 3 m ahead (375): the barrier 300 -> 0 and broken, 68 to her (75 less armor); held after: down
  for 5 s while refilling (56, 145, 234), up at 5 s with 255.
- Speed: 5.48 m/s down, 3.89 up (30% slower: 3.85).
- Shield Bash at a NoAI zombie 4 m ahead: she stopped on it, 70 to it, 5 s cooldown; a press on cooldown: nothing; into open
  air: 5.3 m; at a live husk: 70 and pushed 2.8 m.
- Seen: the unfolded frame before her in third person (front) and first person.

## What step 5 (Whip Shot, Repair Pack) taught
- **Whip Shot** is stepped on the server along a straight line from her eyes (80 m/s, 4 m a tick), the first enemy within
  0.5 m of it before a block taking 70; it comes back 0.3 s and its cooldown runs from then.
- **One push loses to Minecraft's ground friction:** a single 25 m/s push moved a husk 1.9 m (Overwatch's: about 8 m). The
  push is held 0.25 s now (`WHIP_PUSH_TIME`, ours): 4-7.6 m.
- **NoAI mobs don't move by velocity:** a push test on one shows nothing; use live mobs.
- **The harness's `key <ability>` must be a tap** (a click counted), not `down` / `up`: held, an ability press never reaches
  the server.
- **Repair Pack** has no projectile entity yet: its flight is its time (distance / 55 m/s, homing on the ally); its look
  comes with her effects.

Played (step 5, dev client, survival, flat world `shield1`, every mob killed first):
- Whip Shot at a husk 10 m ahead: 70, and pushed (1.9 m before the fix; 5.7 m after); at 4 m: 7.6 m; at a skeleton 8 m: 4 m;
  at a husk 24 m ahead (past 20): nothing; behind a stone wall: nothing; a second press at once: on cooldown.
- Repair Pack at a villager 12 m ahead at 4 health: 6.8 at 0.4 s, 12.2 at 1.2 s, 14.0 (25 + 100); a villager to her side
  untouched; she isn't healed herself. Three packs (3 -> 0), a fourth press with none: nothing; back one at a time 5 s apart
  (1 at 5 s, 2 at 10 s, 3 at 15 s).

## What step 6 (Rally) taught
- **Rally's +100 armor grows her health's top:** a max-health modifier of 8 and 100 more in her armor layer
  (`HeroState.extraArmor`, `Shields.grant`); at the end both go and her health is clamped. The HUD counts anything over a
  hero-with-armor's base as armor (yellow).
- **A stun is Sleep's lock without the sleep:** `Sleep.stun` holds a mob still (NoAI) and locks a player's hero keys for
  0.75 s, damage doesn't end it, and none of the sleep's Zs or sounds play.
- **Overhealth pieces outlive Rally:** each 15 lasts 30 s (ours: one pool held 30 s from the last piece).

Played (step 6, dev client, survival, flat world `shield1`, every mob killed first): Q at 100%: ult 2700 -> 0; 1.5 s in her
health 28 / 28 (350: 100 armor on top), the barrier 750; her overhealth 30 and the villager's 3 m away 45 at 1.5 s, both 100
at 4 s, a villager 12 m away none; her speed 6.33 m/s (5.5 x 1.15); a bash on a husk: 70 and stunned (NoAI 1b for its
0.75 s, then free); the shockwave: 70 to the nearest of three husks behind it; after the 10 s: max health 20, the barrier
300, the villager's overhealth kept.

## Step 7 (perks)
Each perk against its control, picked through the perk menu (Alt held, a click), dev client, flat world `perksb`:
- **Combat Medic:** a pack thrown (recharge running), then one swing at three ravagers: the charge back after 2.2 s, against
  4.4 s with Morale Boost instead (3 x 0.75 off the 5 s).
- **Morale Boost:** Whip Shot at a husk, a villager 6 m away at 4 health: 5.6 at 1 s, 8.6 at 4 s, 10.6 at 6 s, 11.3 (12 + 78.75
  over 7 s); without it 8.6 at 4 s and no more (12 + 45).
- **Inspiring Strike:** a bash hit: the villager 4 -> 8.56 at once (57), her speed 7.1 m/s for the next second (x1.3); without
  it 5.2 at 0.3 s rising over 4 s, 5.46 m/s.
- **Whiplash:** Whip Shot at a live husk 6 m ahead with a wall 2.5 m behind it: 20 -> 9.6 (70 + 60); without it 14.4 (70).

## What step 8 (effects) taught
- **Her flung flail head is a bone of her body, not of the flail** (assets.md): in third person the head is drawn at her
  hp 0257 = `bone_0606` (< 0605 < Pelvis), which her swing clips carry 4.7-4.9 m out; the flail's own 3P clips keep its
  socket still. In first person it's the other way round: the flail's `_1p` clips fling its socket (`swing_1p` 4 m at
  0.2 s). The action layer played only under `Spine_1`, so her pelvis-hung chain bones (0605 / 0606 / 100E / 12E2 /
  12E6) never moved and the head stayed at her hip: `HeroAnimator.UpperBody.ACTION_CHAINS` (found playing; measured with
  the dev `weapon` command: the head at 2.4 / 4.4 / 4.9 / 4.7 / 3.5 m through a swing). Whip Shot's head goes 20 m, past
  its clip's 5: BrigitteFx flies it along the server's path (`brigitte_whip` / `brigitte_whip_back`).
- **Check a texture's kind before giving it a palette.** Her dust and smoke puffs are grey with the shape in the alpha; her
  heal swirl and dots, the stun halo and stars, the shards are coloured: through a palette by brightness they drew squares.
  `Palette.NATURAL` (with `tint` for the dust). A quick per-pixel look (grey or not, alpha used or not) tells which.
- **A LUT read by raw brightness can wash a field:** the barrier's row 0 starts at a dim red, so the frame mask's dark-grey
  ground turned the whole field pink. Ours: the masks through her sky-blue energy row (M).
- **The swing arc is a U-shaped band 1.4 m tall,** fixed on her pelvis pivot `bone_12E2` (hp 0079), which doesn't turn
  through the swing (measured): the arc stands still in front of her while its masks flow.

Played (step 8, dev client, flat world `brigemo`, effects slowed with `fxslow` for the frames): the swing from behind
(fx_swing_back1-3.png: the arc, the head out on its chain, dust and sparks where the 6 m point meets the ground); the
barrier front and first person (fx_bar3_front.png, fx_bar3_1p.png: pale blue, cyan edges); Whip Shot at a husk 12 m
ahead (fx_whip_a-c.png: the chain out, the rocket sparks); a bash on a husk (fx_bash_a/b.png: jets, the shield imprint,
8 streaks); Repair Pack at a villager 10 m ahead (fx_pack_a-c.png: the core and sparks in flight, the arrival's dome,
glow and lens, the heal swirl and dots); Rally (fx_rally_a-c.png: the burst over her flail, the dome, ring, armour shell,
decals; the villager's gold glow) and its bash's stun (fx_stun2.png: the halo over the husk). Each moment's particles
listed with `fxnear`. The white pixel clouds in some frames are Minecraft's own death puffs from killed test mobs.

## What step 9 (emotes) taught
- **Her pieces are three slots of her hero entity:** the flail 3572, the hand shield 3816 (24C5 on Weapon_L) and the
  seated head 33C0; the folded shield 339E (1B25 on 01D8) no emote touches. A weapon clip may live only in an emote's
  folder: `owm_survey.py --clips-from` (Cheer's 5265 on the flail).
- **A second stow key needs the loader to read it:** `EmoteData` now keeps every `stow*` key's windows; a `stow*` key that
  isn't windows (Lúcio's `stowBy` note) broke every hero's data until skipped (found playing: the flail missing in Cheer).
- **An emote hides a piece by parking its bone:** Flail Tricks puts 0F74 over 80 m away; the folded shield isn't drawn then.
- **Her shield's rest pose is already folded** (its bones 0.58 x 0.91 m, the deploy clip's first frame).

## Step 10 (two players, a night, the general play)
Two dev clients (host "Kiriko" as Brigitte, `runClient2` "Viewer" joining over LAN: `cmd publish true 25565`, then
`connect localhost:25565`; a joined player starts in the world's game mode and isn't op):
- **Allies (no teams):** Repair Pack on Viewer 16 -> full by 1.2 s (the landing, heal start / loop / end in their head);
  Inspire 10 -> 14.83 (57, plus Kiriko's own support regeneration); Rally's overhealth on them 75 by 3 s; they hear the
  friendly Rally line.
- **Enemies (teams red / blue):** Whip Shot 70 (20 -> 14.4), pushed 5.9 m, her whip-hit line; Viewer's bow (Minecraft
  mode) into her barrier 300 -> 127 with her health untouched, with it down 20 -> 11.56; the enemy Rally line. **Found:**
  Rally's stun let a player walk (only their hero keys were locked): `Sleep.stun` now sets the asleep attachment their
  client holds them still by, and its end clears it (the first fix forgot that and froze them for good). After: held
  still through the stun, walking from 0.62 s.
- **A night in survival** with mobs spawning (zombies, bow skeletons, a spider, a creeper, a husk) and Viewer beside her:
  the flail, the barrier against the skeletons, packs to Viewer when hurt (6.2 -> 13.8), G out to Minecraft mode and back
  mid-fight, Rally once charged; she died twice and Viewer once (a scripted fighter, not a player's tactics), each
  respawn back in hero mode.
- **The general play:** each of the eight heroes picked from the hero menu (H) mid-game and fired at a 200-health zombie
  (each did damage; Mercy with her blaster on 2); a death and respawn as Brigitte (full health, her kit); Minecraft mode
  (stone mined with a pickaxe, a chest placed and opened, the inventory); Save and Leave from the pause menu, rejoin,
  the select screen, Brigitte picked again with her barrier and packs full.
- Not tried: a second player as Brigitte; the Nether; water and lava; boats.

## The Workshop recording (2026-11-01: applied, #76)
Shield Bash in the real game (two, `E:\Overwatch Archive
esearch\movement\workshop-2026-11-01b\`): 25.85 m/s for 0.256 s
(16 ticks at 62.5 a second: 6.6 m), then she carries 14.7 m/s, which bleeds off in the air (about 19 m/s²) until she lands.
Ours: 22 m/s for 0.3 s (the same 6.6 m), stopping dead. Walking with the shield up: 3.85 m/s (ours, the 30 % slow: the same).

## Shield Bash as recorded (#76)
The owner's Workshop recording (2026-11-01, `E:\Overwatch Archive\research\movement\workshop-2026-11-01b\`, three
bashes from a jump): 25.85 m/s for 0.256 s (16 ticks at 62.5 a second) with gravity on her throughout, then her speed cut
to 15 m/s with its direction kept. Ours had flown level at 22 m/s for 0.3 s and carried the whole 22 m/s on (12.97 m
from a jump, 8.57 m from the ground: reproduced in game first). Now as recorded; OwMovement's air decay and ground
friction take the 15 m/s off and already matched the recording (about 18 m/s/s at 14.7 in the air; x0.79 a server tick
on the ground against its x0.81).
Played (dev client, flat world `bash1`, the shield held, the left button), each a trace of her speed per tick:
- From a jump: 25.85 m/s while her fall went on (vy -1.88 to -4.5), then 13.7 / -3.4 a tick after the cap; 8.38 m in
  all (the recording works out to about 8.1).
- From the ground: 25.85 m/s, then friction: 7.13 m.
- Into a NoAI zombie 4 m ahead: she stopped on it at 3.88 m, 70 to it (20 to 14.4).
- Into a stone wall 3.5 m ahead: stopped at it, 3.2 m.
- Off a 3-block ledge: the bash falling with gravity, then 13.9 m/s bleeding to 7.5 by the landing, 12.49 m.
- Not tried: slopes, water, a second player.

## The barrier fixed (#88, 2026-11-02)
The owner's playtest: "brig shield is super buggy. Like, go into third person, hold shield, and brig starts looking
around. Also, holding shield doesn't go into third person mode. it also didn't do anything for creeper explosion
protection". Each reproduced first (dev client `runClient2`, flat survival worlds `brig88a`-`brig88d`, Normal, the shield on
the right button through the key mappings), then fixed and played again.

- **She spun with the shield up.** Her shield-up stand `idle_shield` (4FE7) is a turn-in-place sheet (root yaw 0 -> -120
  over its 31 frames, `rootyaw_3p.txt`); the animator held such sheets at frame 0 only by a name list, and played this one
  on the clock: from behind she turned side-on and back every second (`brig88_before_back_held_0/4/6.png`). Now any stand
  whose root turns over 45 degrees is held ([animation_import](../animation/animation_import.md#finding-the-right-clip));
  with the shield up her stand is still and square to her aim from behind, the front and turned 30 degrees
  (`brig88_after_back_held_9.png`, `brig88_after_front_held.png`, `brig88_after_back_turned.png`). Her stand's own breathing
  (`add_breathe_shield`) now plays under it. The rule also stills Sombra's stealthed stand and Mercy's blaster stand
  (`brig88_sheet_sombra.png`, `brig88_sheet_mercy.png`); every hero's stand twice 1.2 s apart: `brig88_stands.png`.
- **No third-person view.** Her data has one: 0EE3 #857, on while the shield is up for her own view, 2 m back (4 in
  Rally, chased over 0.4 s), a 0.3 s blend, off for the first 0.35 s of a bash; no hero setting turns it off
  ([rendering](../rendering/rendering.md#ability-cameras-2026-11-02-88)). Traced (the harness's `state` `camera`): in 0.3 s
  after the raise, 2 m; a bash: first person by 0.24 s, back from 0.44 s; released: out over 0.3 s to first person; Rally:
  4 m. F5 while held cycles front and back, never first person. G (Minecraft mode) or her death: first person at once. The
  crosshair is drawn from behind and sits on what she aims at (`brig88_after_center_zoom.png`, `brig88_night2_2.png`).
- **Creepers went through.** Three causes, each seen in numbers (health in Overwatch HP, the barrier's from the HUD):
  - a hit bigger than what the barrier had left broke it and the rest landed as a hit of its own, with the blast's push:
    a creeper 1.5 m ahead broke it, 43 to her, thrown 5.4 m; two TNT broke it and 54 reached her. Overwatch's barriers
    stop a hit whole (M: the wiki, "barriers block the explosion"; no source gives a carry-over). Now: 0 to her, no push.
  - a creeper's blast was judged from the creeper's eyes, and one nearer than the barrier's plane (0.9 m) wasn't in
    front of it at all: at 0.7 m ahead with the shield up she died (250). Now explosions are judged from their centre
    (`ServerExplosionMixin` hands it to `Barrier`), and a source between her front and the plane counts as at the
    barrier (ours: in Overwatch there's no room there): 0 to her, the barrier broken.
  - her armor took 7 off each hit before the barrier did: a zombie's 37.5 cost the barrier 30.5. Now 37.5.
  A blast is charged to the barrier once, and stops for everyone behind it.
- **Found playing:** dying with the shield up left her in its stance after the respawn, the field drawn on her (the lower
  was never sent): the barrier now goes down on death and on leaving her, and a player who comes into range while it's up
  is sent its raise (not played with two clients: one client only on 25600).

Played after (each with the shield up unless said; health before -> after, barrier before -> after):

| Case | Before the fix | After |
|---|---|---|
| Creeper 3 m ahead (ignited) | 0 to her, 300 -> 98 | 0, 300 -> 91 |
| Creeper 1.5 m ahead | 43 to her, 300 -> 0, pushed 5.4 m | 0, 300 -> 0, no push |
| Creeper 0.7 m ahead | dead (250), barrier untouched | 0, 300 -> 0 |
| Live creeper walking up from 8 m | 0, 300 -> 66 | 0, 300 -> 59 |
| Creeper 3 m behind | 202 | 202 (as it should) |
| Creeper 3 m ahead, shield down | 202, pushed 2.65 m | 202, pushed 2 m |
| Creeper 2 m ahead 45 degrees right | dead | dead: outside the 1.51 m barrier's width |
| TNT 3 m ahead | 54 to her, 300 -> 0, pushed 1.95 m | 0, 300 -> 0, no push |
| TNT 3 m behind | dead | dead |
| Ghast fireball from 8 m ahead | 0, 300 -> 175, pushed 3.8 m | 0, 300 -> 161, no push |
| Ghast fireball, shield down | 74, pushed 9 m | 68, pushed 9 m |
| Wither skull (owned) from 8 m ahead | not tried | 0, 300 -> 136 (100 impact + 64 blast) |
| Wither skull (owned), shield down | not tried | 118, pushed 6.9 m |
| Zombie hits | barrier -30 each, one reached her (30.5) | barrier -37.5 each, none reached her |

Screenshots above are in the first run's `run2/screenshots`; the rework's in `run3/screenshots` (`brig88_r_*`). Not cited
on purpose: `brig88_after_front_held.png` (an earlier HUD build: it still drew the crosshair in the front view; the
committed code's front view is `brig88_r_front_held.png`), `brig88_cam2_in_*` (the world hadn't loaded after a teleport:
all sky), `brig88_respawn.png` (Sombra, from a hero switch that hadn't taken).

**The first night fights, every death written down.** Three runs on the fixed build (`brig88_client4.log`):
- 20:40:10 "blown up by Creeper" six seconds in: **she was Sombra, not Brigitte** (the script's own output: `'hero':
  'sombra'`, max health 18, "barrier" 60 -> 47 was her ammo). The dev `hero brigitte` after a Sombra test hadn't taken,
  and the script never checked. Not a barrier case; the fights after check the hero first.
- 20:40:38 "slain by Enderman": dead at spawn on the hero menu at night (spawning was on in that world), before the fight.
- 20:41:58 "shot by Skeleton": the script aimed her at the nearest entity, which was dropped dirt from the craters; she
  faced away from the skeleton (`brig88_night_4.png`: dirt's data in the chat). Its selector now leaves out items.
- The fourth (fixed targeting) is the one below: 45 s, lowest 13.1, no hit through the barrier.

A night fight (spawning off, three creepers, two skeletons and a zombie summoned 10-18 m ahead, live; the barrier up,
dropped for the flail when something was within 3.5 m and the barrier under 120; a script aiming at the nearest mob):
45 s, lowest health 13.1 of 20, every hit on her taken while the barrier was broken or refilling, two creeper blasts taken
whole by it. (Before the melee rule below: zombies' hits then went into the barrier.)

## The barrier reworked after the critique (#88, 2026-11-02, [critique_88](../testing/critique_88.md))
- **Melee goes through** (the owner, 2026-11-02: "Melee goes through"; the wiki's Barrier page: every melee attack
  pierces barriers). `Barrier.isMelee`: Minecraft's mob and player attack types, a hit whose direct entity is the
  attacker (not a projectile), and her own flail's swing. Ranged hits, projectiles and blasts from the front stay
  blocked. **The same rule applies to Sigma's barrier** once it uses the shared code (feature/sigma's `Barriers`).
- **A blast is charged by the barrier's own exposure**, before anyone is hurt: what an entity at its nearest point
  would take at full exposure (Minecraft's rule, as feature/sigma's `Barriers.blast`), once, whoever stands behind it.
  Before, it paid what the blast did to whichever covered entity Minecraft's loop met first.
- **The push is judged by where people are**, not by whether they were hurt (`Barrier.blocksBlast`, the signature
  feature/sigma calls): a wind charge hurts no one, so its push had gone through.
- **"Against the barrier" only covers what's behind it within its width**: an ally 5 m to her side of a creeper at her
  chest is hit as Minecraft would hit them. The barrier's lowest crossing is 1 m under her feet (was 0.2): the ground of
  a step or a slope ahead (ours).
- **Blast-carrying projectiles go off on the barrier** (`Barrier.tickProjectiles`): a ghast's fireball, a wither skull,
  a wind charge about to pass through it from the front bursts there; a blaze's small fireball is gone and the barrier
  takes its 5 (62.5). Found playing: a breeze's wind charge that missed her went off at her back and threw her 10 m up
  and over her own barrier (the fall killed her).
- **The camera** (re-read #857, [rendering](../rendering/rendering.md#ability-cameras-2026-11-02-88)): 1.04 is Stadium's
  third-person field of view, not a height; 8.0 the distance while v44373 (game message 1DC5: unknown, L); 0.4 a time (M:
  the rate field is a different one, m_9B0385B4, 30 m/s on #699). The rise is 0.5 m (ours): her head and shoulders under
  the crosshair (`brig88_r_frame.png`); at 1 m she sat on the screen's bottom edge. Her model isn't drawn while the view
  is under 1 m from her (mid-blend her ponytail had filled the screen: `brig88_r_blend_in_1.png` now shows the field
  and no hair). F5 in the blend-out keeps the player's choice (traced: released, F5 at once -> front, stays). The
  foresight is 0.3 s and not when emoting, asleep or a screen is open.
- **Turn-in-place by all of a clip**, the largest turn from its first frame over 16 samples; the cache goes with a
  reloaded model. Held now (`turnclips`, 2026-11-02): Brigitte idle(_2), idle_shield(_2), crouch_idle(_2); Sombra
  idle(_2), idle_stealth(_2), crouch_idle(_2); Kiriko idle, crouch_idle; Ana idle(_2), idle_scoped(_2), crouch_idle(_2),
  crouch_idle_scoped(_2); Lucio idle, idle_alt, crouch_idle(_alt); Mercy idle(_2), idle_blaster(_2), crouch_idle; Juno
  crouch_shared; Sojourn idle(_alt), f42_idle(_alt), crouch_idle(_alt), crouch_shared; Lifeweaver idle(_2),
  idle_charging(_2), idle_thorn(_2), idle_firing_thorn(_2), crouch_idle(_2); Jetpack Cat none. (The list also names
  additive and emote clips, which never play as a stand and aren't affected.)
- The dead "rest lands as its own hit" branch is gone (nothing passes on any more).

Played on the rework (dev client `runClient3`, port 25601, flat survival worlds `brig88e` / `brig88f`, Normal; the
harness's mapped keys, not the hand; scratchpad `brig88_t11.py` - `t15.py`):

| Case | Seen |
|---|---|
| A zombie hitting her, barrier up | 30.5 to her each hit (37.5 less armor), the barrier untouched (300) |
| A skeleton 12 m ahead, barrier up | the arrow on the barrier (300 -> 250), none to her |
| Creeper 0.7 m ahead, villagers 5 m to her right and 1.5 m behind | her 0, the one behind 20 -> 20, the one beside 20 -> 15.1; barrier 300 -> 0 |
| Creeper 3 m ahead, a villager 1.5 m behind / her alone (twice each) | the barrier 300 -> 6 every time, her and the villager 0 |
| Her a block up, creeper below 1.5 m / 3 m ahead | 0, barrier 300 -> 0 / 300 -> 6 |
| Her on top of stairs, creeper two steps down 2.5 m ahead (`brig88_r_slope.png`) | 0, 300 -> 82 |
| A breeze's wind charge from 6 m ahead, barrier down / up | pushed 11 m and 6.2 / not moved, 0 |
| A live breeze, she turning to face it (three runs of 30 s, 5-7 wind charges seen in flight each) | not moved once; barrier down: launched 4 times |
| Ghast fireball from ahead / one passing 0.5 m beside her chest | 0, barrier 300 -> 156 / 0, 300 -> 156 (it burst on the barrier) |
| Blaze small fireball from ahead | 0, 300 -> 238 (62.5) |
| Wither skull (owned) from ahead | 0, 300 -> 156 |
| Creeper 3 m ahead, barrier down, three runs | 202.4 each; pushed 2.65, 2.00, 2.00 m: the first run's "2.65 -> 2" was this run-to-run spread, not a change |
| Switching hero (H) with the shield held | the menu lowered it; Kiriko picked with the button held: her plain stand, first person |
| A Nether portal with the shield held | lowered as the loading screen opened (Minecraft releases every held key there); pressed again in the Nether: up, from behind |
| Improved Transparency on (F3+X) | the barrier and her drawn as before (`brig88_r_transparency_held.png`) |
| Ana scoped from behind, 1.2 s | `idle_scoped` held still (`brig88_r_ana_scoped_0/2.png`) |
| Jetpack Cat's Lifeline | from behind, Minecraft's 4 m, the crosshair now drawn over the tether's mark (`brig88_r_cat_lifeline.png`) |

A night fight on the rework (the same six mobs, the script now aims at mobs only and checks the hero first): 45 s,
lowest 7.7 of 20. Zombies' hits came through the raised barrier (2.44 each: melee goes through), a creeper blast broke
it (227 -> 0) with nothing to her, an arrow hit her while it was refilling; arrows on it otherwise (255 -> 205 -> 155 ->
105). Before it, a teleport to x 5000 left her falling out of the world ("fell out of the world" 0.7 s after the
teleport, at y -172) where 1000-4100 had been fine: not reproduced or looked into (the hero's own movement in a chunk
not yet loaded?). That session's log (`brig88_client7.log`) has 17 "Insufficient system resources" errors saving
chunks around it: the machine was out of memory (0.4-0.5 GB free that evening with three clients up), which may be the
cause.

One more death in that session, not written down at first: 21:39:19 "Third was slain by Breeze", 12 s after the last
x 3000 breeze run ended (21:39:07: the script cleared her Resistance and let the button go, so the barrier was down) and
the breeze, never killed, kept shooting; "standing on air" at 21:39:12 is her thrown up. The barrier wasn't up: not a
barrier case.

## The barrier after the second critique (#88, 2026-11-02, [critique_88](../testing/critique_88.md#second-pass-2026-11-02-the-rework-50c8148c548424))
- **Melee only when marked.** `Barrier.isMelee` had counted every player-attack hit and every hit by its own attacker, so
  Lucio's Soundwave, Jetpack Cat's Purr and Catnapper's landing (all `playerAttack`), a guardian's beam, a ravager's roar
  (a mob attack, but no blow) and a warden's sonic boom passed the barrier, though the wiki lists none of them among
  what pierces barriers. Now a blow is melee only while one is being dealt (`Barrier.meleeDepth`): a mob's
  `doHurtTarget` (Mob's, and the bee's, iron golem's, hoglin's and zoglin's, which strike without it; not a guardian's,
  whose beam ends in it), a player's own attack (Minecraft mode), a hero's quick melee, her flail's swing. A damage-type
  test can't tell these apart; the marker sits outside `Combat.damage`, so fix/killfeed-hud's `Blow` merges beside it.
- **Area abilities stop at a barrier whole.** Soundwave's push and lockout, Purr's push and Catnapper's knock-down were
  pushes and effects the barrier never saw (true before #88 too): `Barrier.shields(victim, attacker)` now skips them
  for anyone a barrier covers from the attacker's eyes, as their damage is.
- **The camera's foresight** checks Sleep's synced attachment (`HeroManager.ASLEEP`, what the server refuses on), not
  a bed: under a Sleep Dart or Rally's stun a right press no longer pulls the view out for 0.3 s (compile-checked; not
  played under a dart).

Played (dev client `runClient3`, port 25601, flat survival world `brig88g`, Normal, midnight; `brig88_t16.py`):
| Case | Seen |
|---|---|
| A zombie 2 m ahead, barrier up | 30.5 to her each hit (three hits), barrier 300 throughout (`brig88_r2_zombie.png`) |
| A skeleton 12 m ahead, barrier up | the arrow on the barrier (300 -> 263), none to her |
| A guardian in a pool 7 m ahead, barrier up | its beam on the barrier (300 -> 213 -> 125: the beam's magic and its attack both), none to her |
| A warden 9 m ahead below her 5-block pillar, angered, barrier up | two sonic booms on the barrier (300 -> 175 -> 50), none to her |
| Lucio's stand from the front, 2.1 s (`brig88_r2_lucio_sheet.png`) | still; his stand is `idle`, which the old name list held too, so it is unchanged (his `idle_alt` isn't a stand any state plays) |

Not played: Soundwave, Purr and Catnapper against her barrier (they need a second hero: one client only); a ravager's
roar (vanilla roars only after a shield stuns it).

**What a critic should check (not done or not certain):**
- **Hand play with the real mouse and keys:** none yet. Tried (2026-11-02): the barrier bound to B in the run's
  `config/overcraft-hero-controls.json` (so `hold_key` could hold it, mobs walking at her, no mouse turn needed); the
  desktop tool's `request_access` found no app for the dev client under "java", "OpenJDK Platform binary", "OpenJDK
  Platform 25.0.4.1" or the window title "Minecraft* 26.3 - Singleplayer" (only the installed Minecraft Launcher and
  Minecraft for Windows); asked for the client's java.exe by path, it came back "user_denied" (nobody at the PC to
  approve it). The owner has to approve the access or hold the shield by hand.
- **The bash's drop to first person** is only in the harness trace (first person from 0.24 s, back from 0.44 s): with a
  0.3 s blend each way inside a 0.35 s window it is a dip, and no screenshot caught it (the game's screenshot lands
  0.2-0.3 s late). Whether Overwatch's view really dips there (v68007, M) needs a replay.
- **Overwatch side by side:** the 0.5 m rise, #857's 6.67 and 2.0, the 8.0 distance's condition. The ult ring still sits
  on her back, as Overwatch's sits over a hero in third person.
- **The barrier's place:** Overwatch hangs it on her hand (bone 002C); ours is a flat 1.51 x 2.64 m rectangle 0.9 m
  ahead. A creeper 45 degrees off at 2 m passes its edge (by 0.15 m) and hurts her fully. The charge from its nearest
  point makes it take more than she would have (294 at 3 m, against her 209).
- **Hero blasts:** Juno's torpedoes and Sojourn's Disruptor have no position: judged from the shooter's eyes. (Purr,
  Catnapper and Soundwave are blocked: see the second rework.)
- **Merging with feature/sigma** (#68): both branches rewrote `ServerExplosionMixin` and `Barrier`. This branch keeps
  Sigma's entry points (`Barrier.blocksBlast(entity, centre, cause)`, the same full-exposure charge at the head of
  `hurtEntities`); the merge must: call `Barriers.blast` beside `Barrier.blast` at the head, keep Sigma's
  Stalwart / Gravitic Flux knockback scaling in the two push wraps around `Barrier.blocksBlast`, keep his
  `Barriers.absorb` first in `Barrier.absorb` (which now returns 0 for a covered hit, no passing branch), make
  `covering` (the armor skip) and `isMelee` cover his barrier too, and replay both heroes' blast cases.
- **Multiplayer:** the raise sent to a player who starts watching her is compile-checked only (one client).
- **Two Brigittes** (the first in the player list is asked), **her own TNT** (skips the barrier for her allies too),
  **riding, water, ladders, mid-air, Totem of Undying, charged creepers, end crystals' chains** (the blast state is
  stacked for nested explosions, not played): not tried.
- **Invulnerability frames:** two hits in the same half-second both cost the barrier where vanilla ignores the second
  (older than #88).

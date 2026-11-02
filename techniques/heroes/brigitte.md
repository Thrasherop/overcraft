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

## The Workshop recording (2026-11-01: to apply)
Shield Bash in the real game (two, `E:\Overwatch Archiveesearch\movement\workshop-2026-11-01b\`): 25.85 m/s for 0.256 s
(16 ticks at 62.5 a second: 6.6 m), then she carries 14.7 m/s, which bleeds off in the air (about 19 m/s²) until she lands.
Ours: 22 m/s for 0.3 s (the same 6.6 m), stopping dead. Walking with the shield up: 3.85 m/s (ours, the 30 % slow: the same).

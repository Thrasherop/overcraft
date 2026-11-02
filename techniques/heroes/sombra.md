# Sombra (hub)

Everything learned about Sombra, by subject. The seventh hero (issue #64), third on the owner's 2026-10-28 list: a Support
since her 2026-10-06 rework (Tactician). Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · built the way Ana was: [ana](ana.md)

## Her notes and sources
- **Numbers:** `tools/reference/sombra.md` (Blizzard's patch notes to 2026-11 and the wiki's revision 408595, each number
  with its history; her move to Support on BZ 2026-10-06).
- **The data map:** `tools/reference/sombra-map.md` (her kit from her graphs, node by node, H / M / L); the detail in
  `E:\Overwatch Archive\research\sombra\` (`work_kit\`, `work\assets.md`, `work\clipmap.md`, `extract\`, build 154088).
- **The plan:** `tools/reference/sombra-plan.md` (what maps onto the mod, the Minecraft decisions, the order, open questions).

## What step 1 taught
- **A patch after the data build:** BZ 2026-10-13 changed three of her numbers after build 154088 (pistol 7 -> 6.5,
  Cyberspace 40 -> 20 and 12 -> 13 s). Check the patch notes after the data build's date before trusting the data's
  numbers; `SombraStats` says which source each number takes.
- **The data names her keys:** Cyberspace is ABILITY 1 (Shift) and Translocator ABILITY 2 (E, again to teleport), the
  reverse of her old Stealth / Translocator; read the loadout's button ids (`sombra-map.md`, "Buttons") before wiring.
- **Her pistol is an entity of its own** (1221, model ECA1) on 000A = Weapon_R with no offset, as Mercy's blaster: it needs
  no idle clip to sit in her hand (`HeroWeapon`), her own idle holds it up by her shoulder.
- **Her view model has Ana's shape of clips** (idle, one walk, jump, fall), so the animator's first-person rules for Ana
  serve her too (`HeroAnimator`, `sombra`).
- **Two research agents shared one scripts folder** and overwrote seven of each other's copies (the same code, paths
  differing): give parallel research passes separate folders.

## The Translocator rebuilt (2026-10-31): the data map read a mode's version
- **Check a data reading of a mechanic against the patch notes and the wiki before building it.** The map's Translocator
  (E again to teleport, a beacon lasting 15 s with 5 health) was built and played in step 5, but the live one, in both
  BZ 2026-10-06 and the wiki, teleports her by itself on touching terrain or 0.17 s out (12.25 m), can't be damaged,
  and starts its cooldown at the teleport. What the map read matches the Community Crafted mode's beacon (BZ 2026-06-30),
  which the same build holds. `sombra.md` (the public notes) had it right all along; the map won because it was "the data".
  When they disagree on how something works, the live patch notes decide, and the map line gets a note
  (`sombra-map.md`, Translocator).
- **What she doesn't do any more** (the owner, 2026-10-31): her Hack ability is gone since the rework (BZ: being locked
  out was "jarring, frustrating"; Weaken replaced it). **EMP still hacks** (the owner confirmed), and Hotfix hacks only
  health packs and enemy deployables.
- **An ability that can leave her high in the air needs the soft landing** (#35): a throw straight up stops 12.25 m
  along its path, and the fall took 11 of her 18 health until the teleport set `softLanding` (cleared on the ground,
  from a few ticks after the arrival: the ground she left still counts right after a teleport).
- **The Workshop movement recorder** (`tools/workshop/movement_recorder.txt`, [workshop_recordings](../game-data/workshop_recordings.md))
  has a Sombra section (Translocator thrown and used; walking and jumping in stealth). No Sombra log exists yet; one
  would measure the beacon's arc (our gravity 20 is the data's, M) and her stealth speed.

Played (dev client, survival, fresh world `tl1`, then `tl2` for the fix): a level throw on open ground took her 12.7 m
(12.25 along the path from her wrist, 0.45 m ahead) in under 0.2 s with no second press, 0.5 m above the beacon's spot,
then she dropped to the ground unhurt; the cooldown 7.5 s from the teleport. At a stone wall 6 m off: in front of its face,
stealthed (see-through in F5). Thrown at the ground: there, 1.1 m ahead. Straight up: 14.6 m up, then the fall: 11 health
lost on the old build, none on the fix; an ordinary 8 m fall afterwards still took 5 (past the 3 m). Through a zombie 4.5 m
ahead: the full 12.25 m.

## What step 7 (perks, Conspirator) taught
- **A per-hero switch with a `default` hides missing heroes.** The HUD's ready ult picked its icon with
  `default -> "kitsune_rush"`, so Ana's, Lifeweaver's and Sombra's ready ult showed a blank white disc (Kiriko's file isn't
  in their folders). One exhaustive switch now names each hero's ultimate (`HeroArt.ultimate`), and the compiler flags
  the next hero left out. Look for other `default ->` branches over `Hero` when adding a hero.
- **Data Packet without health packs:** the data counts any hack of a non-ally (FBC: health pack or deployable), so in
  Minecraft it's her hacks of enemy deployables (Lifeweaver's platform and tree); the cut carries over into the next charge
  (`Sombra.dataPacket`, `DataPacketTest`).
- **Conspirator isn't in her graphs** (the engine's or the UI's), so it's ours from BZ / W: the server sends her the enemy
  players whose ult is ready twice a second, and her HUD draws their ult's icon over their heads, through walls.
- **The harness's `key primary_fire` is the right mouse button** (Hotfix, the perk menu's right side); `secondary_fire` is
  the left one (her pistol). A `key ... tap` is shorter than a tick: the perk menu needs `down`, a short sleep, `up`.
- **`/overcraft perks <player> <level>` clears the picks:** set the level once, then pick through the menu.
- **A Lifeweaver platform thrown at someone's feet lifts them** (correct): throw it away from both players to test a hack.

Played (step 7, dev client and a second client on LAN, survival, fresh world `perks2`; Sombra on team red, Lifeweaver
("Viewer") on blue; the perks picked through the perk menu, Alt held and a click):
- **CTRL ALT ESC:** 18 -> 7 health, teleport: 7.68 at 0.6 s, 9.38, 11.08 (1.7 a second = 21.25 HP/s, at once). Teleporting
  at 13 (above half): no regen in the next 2 s. At 7 with no teleport: none for 6 s, then it starts.
- **Data Packet:** Hotfix on a villager (2 -> 1 charge), then a hack of the enemy's platform 12 m off at once: the charge
  back ~4 s after the Hotfix. The same with Cybersecurity instead: ~8 s. The hacked platform didn't lift Lifeweaver standing
  on it for its 10 s, then rose 9 m with him.
- **Life Hack:** villager at 8, Sombra at 14 of 18; EMP: villager 15 at 1.2 s, 19.1 at 3.2 s, then 20; Sombra to 18.
- **Cybersecurity:** Cyberspace at a villager at 6 (below half): 13.7 at 0.7 s, then +1.2 per 0.5 s above half. Without
  the perk: 5.6 -> 10.1 at 0.7 s.
- **Conspirator:** enemy Lifeweaver at 99%: nothing; at 100%: the Tree of Life icon over their head (`consp_big.png`); at 40%:
  gone; the same player on her team at 100%: no marker (their ally bar instead); back on blue: shown; behind a stone wall:
  shown; the enemy's G (Minecraft mode): gone, G again: back; her own G hides it with her HUD.
- **The ready-ult icon:** reproduced on the old build (Sombra's ready ult a blank disc); the fix: EMP's icon in it, and
  Lifeweaver's tree in the second client's.

## What step 6 (EMP, hacked) taught
- **A stale dev client answers the harness.** Stopping `gradlew runClient`'s task left the old game's Java process
  running; the new client couldn't bind the dev port (`DevServer stopped ... BindException` in its log) and every harness
  command went to the old build, where nothing new worked. After stopping a client, check for `java.exe` processes whose
  command line names the worktree and end them before starting the next.
- **A summoned skeleton may carry no bow** (`/data get ... equipment` empty): it then walks up and fights in melee, which a
  hack doesn't stop (by design). To test anything about arrows, summon it with `equipment:{mainhand:{id:"minecraft:bow",count:1}}`.
- **A projectile's owner is set before it's added**, so `ServerEntityEvents.ENTITY_LOAD` can drop whatever a hacked mob
  launches (arrows, potions, fireballs, fangs) without a mixin per mob.
- **The ult's 5 s without charge** reuses `HeroState.ultActive` (HeroManager counts it down; `Combat.addUltCharge` refuses
  while it's above 0).

Played (step 6, dev client, survival, flat world, fresh world `emp2`, mobs frozen with NoAI for the numbers): EMP at 100%
with zombies at 6 m and 13.5 m, a skeleton at 8 m, a creeper at 9 m: each 20 -> 16 health (20% of current, 250 -> 200 OW)
and glowing 8 s; a zombie at 17.5 m and one behind a stone wall: untouched; a villager and an iron golem: untouched; the
charge 3100 -> 0. Live mobs: a skeleton with a bow: its arrow during the 3 s removed as it launched, its next one after the
3 s flew; a creeper hacked 0.6 s into its fuse: alive for 5 s, then the fuse again and the explosion. Tactician: EMP at 120%
left 620; the charge didn't move for the first seconds after the cast and rose again by ~7 s. First-person (her hand cast)
and third-person (the cast pose) clips. Not played yet: a hacked player (needs the second client: step 10), Hotfix and EMP on
an enemy Lifeweaver's platform or tree (step 10; a Sombra's beacon flies 0.17 s and can't be touched, so it isn't one), EMP ending her stealth (the same `Stealth.end`
Cyberspace uses, played in step 5), Life Hack (step 7).

Played (step 1, dev client, creative): picked from the select screen (her model, pistol, kit row, 225 health); in play,
first person (the pistol in her right hand, the HUD with 60 rounds and her two abilities) and third person front and back
(the pistol by her shoulder), running, jumping, crouching; Hello from the communication wheel (her left hand waves in first
person; the third-person wave).

## Her sounds and voice read (2026-11-01)
- **The map:** `tools/soundmaps/sombra.txt` (each event's 02C, effect, graph node and confidence; the live kit only), built
  by `E:\Overwatch Archive\research\sombra\scripts\sounds\sombra_gen.py` (Ana's pipeline, retargeted). What it taught:
  [sounds](../sound/sounds.md#sombra-sounds) (the pistol's self-repeating stream, ranged audience-1 copies, missing
  voice moments).

## Her effects read (2026-11-01)
- **What they're made of:** `tools/reference/sombra-effects.md` (each moment: renderers, meshes, textures by id, colours,
  sizes, lifetimes, hardpoints, and a "rebuild in Minecraft" recipe); the files in `E:\Overwatch Archive\research\sombra\fx`
  and `work\fx_*`, the scripts in `research\sombra\scripts\fx\` (Ana's, retargeted). Masks and meshes are in the dev pack
  as `sombra_*` (`tools/build_asset_pack.py` EFFECT_MASKS / EFFECT_MODELS).
- **Her colour is in the strips:** purple (A06C / A06B) for her team, red-orange (ADD0 / ADCF) for enemies (M), gold for
  her heals (Hotfix, Cyberspace's allied side). The lessons it taught (over-life size rows, strip twins, per-target
  branches): [reading_game_data](../game-data/reading_game_data.md#ability-effects-what-sets-their-size-and-colour-sombras-pass-2026-11-01).
## Her effects drawn (step 8, 2026-11-01)
- `SombraFx` (client) draws each moment from [sombra-effects](../../tools/reference/sombra-effects.md): the pistol's
  muzzle, tracer and hits; Hotfix's strands (gold to an ally, purple to a hacked object) and the healed ally's skull and
  glow; Cyberspace's launch, flight (cubes, ribbon), burst and 3 s zone; the weakened enemy's arrows and chevrons; the
  teleport's bursts and stealth's sparkles; EMP's cast rings and its 30 m pulse; hacked creatures' skulls and code panels,
  hacked deployables' rings. Her palettes are in `EffectArt` (SOMBRA_*), most straight from `fx_palettes.txt`.
- **A palette needs black at 0.** Mine from the report's hex strips started at the strip's own first colour, and every
  additive mask drew its whole quad in it: a magenta square at her muzzle and black squares at each hit (played). The
  masks carry their shape in their brightness; the strip starts at 0.1 over black now (`palettes.py` in the scratchpad's
  pass). A colour sprite on black (the hit spark) is added in a tint, not drawn NATURAL.
- **Two effects were invisible before this:** the server already sent "sombra_cyberspace" and no client drew it.

Played (dev client, creative, fresh world `fx1`, at night, a NoAI zombie at 8 m and a villager): the pistol in first and
third person (tracer to the zombie, hit sparks, the block hit); Hotfix on the villager (strand during the 0.5 s channel,
then the gold skull and glow while it heals); Cyberspace at the zombie (launch flash, cube in flight, the burst, the zone's
sphere, gold dome, code rain and pluses, the ground disc); EMP (cast rings at 0.15 s, the dome and streaks at 0.42 s, the
circuit sphere at 1 s; `fxnear` listed the band, dome, ring and streaks); the hacked zombie seen from the side (skull,
code at its feet, Minecraft's glow); a teleport (the burst where she left and where she landed, then the sparkles).

## Two players (step 10's checks, 2026-11-01)
- **Time a two-client check from one script.** Pressing Q on one client and Shift on the other from two harness runs
  put seconds between them (each run starts Python and connects), and the hacked Ana's Sleep Dart "fired during the
  silence": it was after it. `hacktest.py` (the scratchpad: both sockets in one script, the delay measured) showed the
  silence holding at 0.67, 0.86, 1.06, 1.28 and 2.07 s and the dart firing at 3.87 s (the silence ends at 3.35 s).
- **A cooldown can hide in a check:** E pressed 7 s after a teleport did nothing (7.5 s cooldown), and the "stealthed"
  Sombra stood plain in the enemy's view. Read the cooldown in `state` before a check that needs the ability.

Played (dev client and a second client on LAN, flat world `fx1`; Sombra on red, Ana ("Viewer") on blue, then red):
- EMP on the enemy Ana at 6 m: 20 -> 16 health, glowing 7 s; her Shift blocked during the silence, working after.
- Stealth as the enemy Ana sees it: hidden at 8 m (three frames over 2.4 s), see-through within 4 m (3 m and 3.3 m).
- Hotfix on the allied Ana at 8 m: 10 -> 17.5 at once, 19.9 a second later, then 20.

## Her sounds wired (step 8, 2026-11-01)
- The map is `tools/soundmaps/sombra.txt` ([sounds](../sound/sounds.md#sombra-sounds)); `SombraSounds` plays her events
  (registered from `OcSounds.SOMBRA_SOUNDS` / `SOMBRA_VOICE`), and `Voice` has her stimuli.
- **Her pistol's fire is a stream** (5DCC re-triggers 20 times a second until a Stop): one shot's worth per shot,
  the 2.5-3 s tail (`pistol.fire_end`) once when the burst ends (`HeroState.sombraFiring`, 2 ticks without a shot), else
  60 tails would pile up (M: the map's reading).
- Loops are stopped when their state ends (stealth's on its end; the zone's at 3 s; Hotfix's channel on a cancel).

Played (dev client, fresh world `snd1`; then a second client as an enemy Ana; the harness's `heard`): her side heard
pistol fire / shot / hit / fire_end; reload; Hotfix cast, channel, end, heal and her "hotfix" line; Cyberspace cast,
shot, zone and zone end; EMP cast, pulse, "EMP activated!", the hit and the zombie's hacked sound; the throw, depart,
arrive, stealth start / loop / end and her "translocate" and "uncloak" lines; a Hotfix charge back. The enemy Ana heard
the `_3p` copies, the Spanish ult line and `hacked` in her own head, and not the stealth loop. Not built in the data:
`voice.resurrected` (her set has none).

## The 0.3.26 general play (2026-11-01)
Dev client, a fresh normal-terrain survival world `gp0326`, at a player's inventory (no god mode):
- **Minecraft mode** (Escape on the select): two dirt blocks mined by hand and picked up; four logs into planks in the
  inventory grid; a chest placed, opened, the logs moved in with the mouse.
- **G with no hero** says to press H; **H at spawn** opened the select.
- **Every hero picked there in turn** (Kiriko, Lúcio, Jetpack Cat, Mercy, Lifeweaver, Ana, Sombra), each one's left
  button on a zombie 6 m off (each took damage; Lifeweaver's killed it; third-person screenshots `gp_<hero>.png`).
- **Death and respawn:** killed 25 m away, back at a random point near the world spawn with Jetpack Cat kept. **Found:**
  H there said "go back to your bed" (11 m from the world spawn point, no bed): fixed, the respawn point counts too.
- **Leave and rejoin:** the free pick on the select with Jetpack Cat and her charge kept.
- **The Nether as Sombra:** through a portal; her pistol killed a blaze; EMP took a piglin brute 50 -> 40 and a blaze
  20 -> 16, both hacked; Cyberspace's zone drawn; a teleport worked (cooldown 7 s after).
- **A night fight** (two zombies, a skeleton with a bow, a creeper): standing still she lost it in 11 s (two zombies'
  hits, 3 each, Minecraft's normal numbers); teleporting away when hurt, she won it with her stealth and regeneration and
  EMP charged by the fight. **Harness lessons:** aim from her eyes (`anchored eyes`), not her feet (over a zombie's
  head); a creeper's crater swallowed every shot until she moved out of it.
- A second player was played earlier the same day (hacked Ana, stealth seen, Hotfix on a player, the tree hacked).

- Emotes: [sombra_emotes](../emotes/sombra_emotes.md).

## The Workshop recording (2026-11-01)
The owner's third log (`E:\Overwatch Archiveesearch\movement\workshop-2026-11-01c\`): two Translocator teleports, 12.7 and
13.5 m from her feet (one aimed 7.6 m up), each 0.22-0.23 s after the press (our 12.25 m at 72 m/s: 0.17 s of flight plus the
throw), then stealth at 8.80 m/s throughout (5.5 x 1.6: the +60% our numbers had as M). The earlier two logs stopped writing
before her part (the recorder's Inspector recording: [workshop_recordings](../game-data/workshop_recordings.md)).

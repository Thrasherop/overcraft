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
an enemy Lifeweaver's platform / tree or another Sombra's beacon (step 10), EMP ending her stealth (the same `Stealth.end`
Cyberspace uses, played in step 5), Life Hack (step 7).

Played (step 1, dev client, creative): picked from the select screen (her model, pistol, kit row, 225 health); in play,
first person (the pistol in her right hand, the HUD with 60 rounds and her two abilities) and third person front and back
(the pistol by her shoulder), running, jumping, crouching; Hello from the communication wheel (her left hand waves in first
person; the third-person wave).

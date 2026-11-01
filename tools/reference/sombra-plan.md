# Sombra: the plan

Numbers: [sombra.md](sombra.md) (BZ, W per number) and the game data map [sombra-map.md](sombra-map.md) (D, with its detail
in `E:\Overwatch Archive\research\sombra\work_kit\`: `kit_pistol_hotfix.md`, `kit_translocator_stealth.md`,
`kit_cyberspace_emp_perks.md`) -> `SombraStats`, each number with its source. Branch `feature/sombra` in its own worktree
(issue #64, hero 3 of 12). Written 2026-10-31 from build 154088 (2026-10-06).

## Her role and the patch after the data
- **Support, Tactician** since BZ 2026-10-06 ("Moved from Damage to Support role", "Moved from Recon subrole to Tactician
  subrole"); the data agrees (her loadouts 012E.075). Hack (as an ability), Virus and Opportunist are gone.
- **BZ 2026-10-13** came after the data build: Machine Pistol 7 -> 6.5, Cyberspace's opening damage 40 -> 20, its cooldown
  12 -> 13 s. Those three use the patch; everything else the data.

## Where the data differs from the wiki (the data is used)
- Reload: the clip is full 0.48 s into 1.2 s (W 0.544 s).
- Cyberspace's weaken lingers 0.25 s on enemies (W 0.5 s).
- Stealth: damage doesn't end it (it reveals her for a moment); enemies within 4 m see her (the panel's 12 m is Lúcio's).
- The Translocator's cooldown starts 1 s after landing.

## What maps onto what Overcraft has
- **Hero entry:** `Hero.SOMBRA` (already listed, "soon"), support / tactician / 225; `HeroKit` `dev.overcraft.sombra.Sombra`;
  Tactician through the shared sub-role code (Ana, Lúcio); `Perks` XP 2375 / 5710; four `Perk` entries.
- **Machine Pistol:** hitscan like Ana's scoped shot (`Ana.hitscan`), with falloff, spread and crits (Kiriko's head test).
- **Hotfix:** `Targeting.allyInView` (Ana's Nano Boost) and the heal-over-time Ana's rifle uses (`Ana.dose`).
- **Cyberspace:** a straight `OwProjectile` (Ana's dart) opening a zone entity (Lifeweaver's tree's area loop); weaken as a
  damage-dealt factor beside Nano's (`LivingEntityMixin`, `Combat.healModifier`).
- **Translocator:** a thrown entity like Lifeweaver's petal throw; the teleport moves the player on their own client
  (`GripPayload`'s way); stealth hides her (see below).
- **EMP:** an area pass like Ana's grenade burst; the hack status beside `Sleep`.

## Minecraft decisions (ours; defaults in place, the owner may change them: "Open")
- **Stealth against mobs:** mobs drop her as a target and can't pick her up again unless within 4 m with a line of sight
  (the data's detection). Against players: invisible (no model, no name, no shadow) beyond 4 m to enemies; allies see her
  translucent, as Overwatch shows a stealthed ally.
- **Hacked mobs:** revealed (glowing) 8 s, and "silenced" for the hack's time: their special moves stop (a creeper's fuse, a
  skeleton's or pillager's shot, an evoker's or witch's cast, a blaze's fireballs, a ghast's), they still walk and hit.
  Bosses too. Hacked players: their hero's abilities locked (`HeroManager.controls`, as asleep), revealed to her team.
- **Health packs:** Minecraft has none. Hotfix hacks enemy deployables (Lifeweaver's platform and tree, Jetpack Cat's
  Lifeline... any hero deployable an enemy owns). No health-pack stand-in until the owner picks one (Open).
- **Weaken on mobs:** their melee and projectiles deal 50% less while weakened.
- **Conspirator:** a marker over enemy players whose ultimate is full, for her only (mobs have no ultimates).

## Order (each step played by hand before the next; CLAUDE.md's hand play rules)
1. Hero entry, models and clip maps, select screen and HUD shell: pick her, look at her from every side in both views,
   the pistol in her hand in every clip, her Hello.
2. Machine Pistol (hitscan, falloff, spread, crits, ammo, reload) against zombies, skeletons, at range.
3. Hotfix on a villager, a wolf, a second player; its charges; hacking a deployable.
4. Translocator and stealth: throw, teleport (walls, water, ledges, the Nether's roof), stealth against mobs and a player.
5. Cyberspace: the zone's damage, healing and weaken against mobs and a second player.
6. EMP and the hack status on every mob kind, a boss, a player; Tactician.
7. Perks (Life Hack, CTRL ALT ESC, Data Packet, Cybersecurity); Conspirator.
8. Sounds, voice and effects from her data.
9. Emotes from the data (techniques/emotes/emote_reproduction.md, ana_emotes.md).
10. The general play of the whole build.

## Open (2026-10-31)
- A health-pack stand-in for Hotfix's hack (a potion, a cake, a beacon?), or none.
- Hacked mobs: is stopping their special moves the right "silence"?
- Stealth against players: fully invisible beyond 4 m (Overwatch's), or a faint shimmer?
- The data's ungated -3 s Translocator cut on a Cyberspace elimination (no public text mentions it): left out for now.

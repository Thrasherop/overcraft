# Brigitte: the plan

Numbers: [brigitte.md](brigitte.md) (BZ, W per number) and the game data map [brigitte-map.md](brigitte-map.md) (D, with its
detail in `E:\Overwatch Archive\research\brigitte\work_kit\`: `kit_flail_inspire_perks.md`, `kit_shield_bash.md`,
`kit_whip_repair_rally.md`) -> `BrigitteStats`, each number with its source. Branch `feature/brigitte` in its own worktree
(issue #65, hero 4 of 12). Written 2026-11-01 from build 154088 (2026-10-06).

## Her role and the patches
- **Support, Survivor** (BZ 2026-02-10; D 01C7 tag 1173): Survivor's passive starts the global regeneration on a
  movement ability: hers is Shield Bash (message 26FD), held on at least 0.25 s.
- **No change since the data build** (BZ 2026-09-08 was the last: 175 health + 75 armor; nothing on 10-06, 10-13, 10-20),
  so the data's numbers are the live ones. Where the data and the public text disagree (below), the patch notes decide.

## Where the data and the public sources disagree (to settle before building that part)
- Repair Pack: D 25 + 100 over 2 s; the panel and W 25 + 100 over 2 s too, the panel's "50" being per second: the same.
- Shield Bash's reach: D 0.3 s at x4 speed (about 6.6 m, M); W 8.5 m (changed without a patch note). D, played.
- Rally's speed: +15% (W's tooltip's 3.625 m/s is arithmetic gone wrong).
- Inspiring Strike's instant heal: D 57 (12 + 45); W's 60 is the old total.
- Barrier slow: D 30% (and her shield-walk clips are 0.7 x run); the panel's 25% (02FF) is read by nothing.

## What maps onto what Overcraft has
- **Hero entry:** `Hero.BRIGITTE` (new), support / survivor / 175 health + 75 armor; `HeroKit` `dev.overcraft.brigitte.Brigitte`;
  perks XP 2035 / 5090; four `Perk` entries.
- **Armor (new to the mod):** a 75-point pool at the top of her health like Lifeweaver's shields (`Shields`), with
  Overwatch's rule: each hit's damage less 7, at most halved (W Armor; M how Minecraft damage maps per hit). Healing
  fills health first, then armor. Rally's +100 armor sits on top.
- **Rocket Flail:** a melee sweep, not Minecraft's attack: 6 m, a 35 degree arc, sight needed, every enemy in it, 45 each,
  one swing per 0.6 s (hit window 0.2-0.4 s), forehand and backhand alternating. Also her quick melee (the V key swings it).
- **Inspire:** a heal-over-time like Hotfix's (`HotfixHeal`'s shape) on allies within 20 m and herself; 12 + 45 over 4 s;
  the flail triggers it at most every 1.25 s, Whip Shot and Shield Bash always.
- **Whip Shot:** an `OwProjectile` (Ana's dart) out and back: 80 m/s, 0.5 m, 20 m, one target, 70, knockback 25 along her
  aim + 2 up; 4 s cooldown from its return.
- **Repair Pack:** Hotfix's targeting (`Targeting.allyInView`, 10 degrees, 25 m, sight) and a homing projectile at 55 m/s;
  25 + 100 over 2 s; a second on the same ally adds 2 s and 25; 3 charges, 5 s each.
- **Barrier Shield (new to the mod):** an entity in front of her (the data's pet 2284) with 300 health, blocking
  projectiles and hitscan from the front (`ProjectileImpactMixin`-style); regenerates 85/s after 2 s lowered; broken: 5 s.
  -30% speed while up; right button held.
- **Shield Bash:** a 0.3 s dash (Kiriko's Swift Step / Lifeweaver's dash mover), the first enemy hit takes 70 and 6 m/s.
- **Rally:** 10 s: her armor +100, +15% speed; allies within 8 m get 15 overhealth every 0.5 s up to 100 (Lúcio's Sound
  Barrier overhealth code, `Combat.overhealth`), each lasting 30 s; the barrier 750 and larger; the bash stuns 0.75 s
  (`Sleep`'s lock, as a stun) and adds an 8 m shockwave.

## Minecraft decisions (ours; defaults in place, the owner may change them: "Open")
- **Armor and Minecraft's armor items:** her Overwatch armor is her own pool; worn Minecraft armor still works as Minecraft's
  (as for every hero).
- **What the barrier blocks:** arrows, tridents, fireballs, potions, every hero projectile and hitscan shot from the front;
  not melee (Overwatch's barriers don't stop melee either), not explosions' blast (they do in Overwatch: Open).
- **Stun on mobs:** their AI paused 0.75 s (as Sleep holds them, no wake on damage).
- **Whiplash's wall:** a block in the knockback's path within 0.5 s.

## Order (each step played by hand before the next; CLAUDE.md's hand play rules)
1. Hero entry, models and clip maps (3P, 1P, flail and its head, the shield), select screen and HUD shell: pick her, look
   at her from every side in both views, the flail in her hand and the shield folded on her back.
2. Armor: hits from zombies, skeletons, a creeper; healing back through armor; the HUD's armor segment.
3. Rocket Flail and Inspire: one zombie, a crowd, at 6 m, behind a wall; allies healed within 20 m.
4. Barrier Shield and Shield Bash: arrows and fireballs blocked from the front, not the back; breaking it; the dash.
5. Whip Shot (knockback off ledges, into walls) and Repair Pack (charges, homing, a second pack) on a villager, a wolf.
6. Rally: armor, overhealth on allies, the big barrier, the stun.
7. Perks (Combat Medic, Morale Boost, Inspiring Strike, Whiplash) and Survivor.
8. Sounds, voice and effects from her data.
9. Emotes from the data.
10. Two players, then the general play of the whole build.

## Open (2026-11-01)
- Explosions against the barrier (Overwatch's barriers stop them; Minecraft's blast goes through shields).
- The barrier's size: no number in the data (it is the mesh 1064F's): measured from the mesh.

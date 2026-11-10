# Critique: fix/killfeed-hud (6fc07e4, 994a6a2), 2026-11-02

Up: [techniques map](../README.md) · reviews: [kill_feed](../hud/kill_feed.md) · beside:
[qa_critique_2026-11-02](qa_critique_2026-11-02.md), [qa_audit_2026-11-02](qa_audit_2026-11-02.md), [hand_play](hand_play.md),
[harness](harness.md)

An independent read-only review: the diff, every contact sheet the record cites, the play scripts
(`kf_*.txt`, `kf_*.py` in the session scratchpad), issues #33/#84/#102/#106/#107/#108, and trial merges
(`git merge-tree`) against `feature/sigma` and `feature/dva`. No game was run. The Overwatch wiki's
"Elimination Feed" page couldn't be re-read (the MediaWiki API answered HTTP 402 on 2026-11-02), so claims that
rest on that page alone are marked **unverified** below.

**Verdict: don't merge yet.** The code idea is sound: one `Blow` per damage, the kill marker sent from the death.
But the play that the record calls hand play was a scripted harness run. Brigitte's Quick Melee kills show the wrong
icon. The world-death credit fires on more than knockbacks. And merging the branch breaks both pending hero
branches in a known way that nobody has planned for.

## Ranked findings

### 1. Blocker (process): the "play" was a script, against stand-ins (CLAUDE.md: hand play before merge)
- Every step in `kf_*.txt` is harness input: `target 8 1` (summons NoAI zombies with 1 health at fixed spots),
  `look 0 5.8`, `hold secondary_fire 0.08`, `cmd /overcraft ult`, `cmd /overcraft perks @s 3`, `hero sojourn`.
  No real mouse, no mob that moves or fights back, no night fight, no slope, wall, water, villager or wolf.
- "A zombie killing the hero" (`kf_death_respawn.png`) was `/damage @s 1000 minecraft:mob_attack by @e[tag=z]` on a
  NoAI zombie (`kf_death.txt`). It wasn't a bite. The record and the commit message both present it as a real one.
- Nine of ten heroes were picked with the dev `hero` command. Only Kiriko came from the select screen.
- The `/kill` exclusion (GENERIC_KILL) isn't shown to work. The record itself says the Sonic shot's hit wasn't
  confirmed, so "no line" may just mean the shot missed.
- The Minecraft-mode kill "didn't land", so nothing was seen there.
- File names were reused: `kf_soj_charged_0/1.png` were overwritten by the `kf_soj6` run, and `kf_sheet_soj3`
  (cited as after-evidence) shows empty frames from the first, failed charged-shot attempt.
- The catnapper (`kf_cat_catnapper*.png`) and the first charged shot (`kf_soj_charged*.png` in `kf_sheet_soj`)
  attempts show no kill. Later sheets (`kf_sheet_cat2`, `kf_sheet_soj6/11`) do show them.
- `kf_sheet_107.png` mixes the before frame (the teal flash, ult 14) with the after frames, with no labels. Read
  alone, it seems to show the flash still firing.

### 2. Major: Brigitte's Quick Melee kills show only the arrow, not the fist
`kf_sheet_brig.png`, `kf_brig_melee.png`: "VIEWER > ZOMBIE" with no icon. Her quick melee goes through the flail
code (`Brigitte.java` "Her quick melee is the flail's swing") tagged `Blow.WEAPON`. In the feed, Quick Melee is the
fist for every hero (the record's own rule). Her data having no *shared* melee clip says how the swing looks, not how
the kill is told. The record lists "(the flail)" in the melee column as if that settled it. This also contradicts
#33's accepted table ("Melee: the fist"). Fix: tag her quick-melee swing `Blow.MELEE`, and keep `WEAPON` for the
primary.

### 3. Major: the merge into next breaks feature/sigma and feature/dva (not planned, not recorded)
`git merge-tree HEAD feature/sigma` and `... feature/dva`: **14 files conflict in each** (Ana, Anesthetic, Brigitte,
SleepDartEntity, QuickMelee, JetpackCat, Mediblaster, Torpedoes, Lifeweaver, Lucio, Disruptor, Railgun, Cyberspace,
Sombra). Both branches changed the `Combat.credit(hero, dealt)` lines next to every `Combat.damage` line (to
`credit(hero, dealt, target)`, the Tank ult factor). The Catnapper line is the same line in both. Once those are
resolved, these 3-argument `Combat.damage` / `damagePart` calls no longer compile:
- **Sigma** (`overcraft_sigma`): `Hyperspheres.java` 118 (direct, `damagePart` and `damage`) and 149 (burst);
  `Accretion.java` 126 (impact, both) and 162 (burst); `GraviticFlux.java` 314 (both); `SigmaPerks.java` 77
  (Hyper Strike).
- **D.Va** (`overcraft_dva`, which also carries the Sigma files above at slightly different lines):
  `FusionCannons.java` 169; `Boosters.java` 115; `Dva.java` 422 (Call Mech landing); `SelfDestruct.java` 104;
  `MicroMissiles.java` 138 (both) and 166; `LightGunShotEntity.java` 78-79 (both).

Each needs the right kind:
- **Sigma:** Hyperspheres = **WEAPON** (his primary fire: no icon), Accretion = ability, Gravitic Flux = ult (and its
  slam is the main case for world credit), Hyper Strike = perk.
- **D.Va:** Fusion Cannons = WEAPON with crit, Light Gun = WEAPON with crit, Boosters / Micro Missiles = ability,
  Self-Destruct and the Call Mech landing = ult.

Neither branch has `ult:`/`ability:` art slugs checked against `BlowTest`'s 32-character limit. Sigma's
`Voice`/`Sigma.killed(killer, source)` reads the source to choose "Collision!" or "To the void!". The void line
needs the world-credit path, which `Voice` doesn't see (see 5). The record should name these call sites, and so
should the merge commit or the issues for #68 and #69.

### 4. Major: inconsistent kinds in the blow table
- **Rally's bash shockwave** (`Brigitte.shockwave`, which only exists during her ult) is tagged
  `ability:shield_bash`. Sojourn's charged shots during Overclock are tagged with the ult icon. The same rule ("dealt
  only because the ult is up") gives opposite answers for the two heroes. Pick one rule and write it in the record.
- **Biotic Bounce's second burst** (a perk) is tagged `ability:biotic_grenade`. Whiplash (the perk on Whip Shot) and
  Superbloom (the perk on the thorns) get perk icons. By the record's own rule ("a perk that deals damage of its own:
  its icon"), the bounce should be `perk:biotic_bounce`, unless the wiki's "(and its perk)" means otherwise. That's
  **unverified**, and the record doesn't quote the line.
- **Sojourn's Charged Shot** is tagged `ABILITY`. It is her Railgun's secondary fire, a weapon mode. The record says
  the wiki gives it its own icon. That is **unverified** here, and it conflicts with the record's own quoted rule:
  "Alternate fire ... will not show a unique icon". If the icon is real, `Blow` can't say so honestly: a WEAPON has
  no icon slot, so a weapon mode with an icon has to pass itself off as an ability. Anything that branches on
  `kind()` (Voice today, ult charge or stats tomorrow) will then treat it as an ability. Either quote the wiki
  sentence that lists it, or give `Blow` a weapon-with-icon form.
- **Claws Out** is now a PERK, so `Voice` no longer says `melee_kill` on a Claws Out melee kill. Before this branch
  it did (direct entity == killer). That's a voice change nobody listened to, and it isn't recorded.

### 5. Moderate: the world-death credit is wider than "a knockback sent them into it"
`Combat.remember` runs *before* `hurt`, on every hero `Combat.damage`, whatever happens next:
- A hit that Brigitte's barrier absorbs completely (`LivingEntityMixin.overcraft$barrier` returns false), that
  `ALLOW_DAMAGE` cancels, or that lands on an invulnerable target still records a blow. A fall within 5 s then
  credits that hero, with ELIMINATED and the feed line, for a hit that did 0 damage.
- *Any* death with no attacker counts, not only falls and the void after a push: a zombie **burning in the sun**,
  drowning, suffocation, starvation, cramming, a dispenser arrow (null owner), `/damage ... minecraft:fall`. A
  kunai body shot followed by sunrise reads "VIEWER > ZOMBIE" plus ELIMINATED. The record's own source sentence
  ("when a hero's stun or knockback sent them into it") is narrower than the code.
- The window ignores a later non-hero attacker. Hero hits a player, a skeleton's arrow knocks them off a cliff 3 s
  later: the hero gets the kill. (Minecraft's own `getKillCredit` would name the skeleton.)
- The hero who gets the credit is looked up *at death*. If they switched hero in those 5 s, the line shows the new
  hero's portrait with the old hero's blow. `HeroArt.ability(newHero, oldSlug)` is null, so the old ability's name
  is drawn as text ("KIRIKO [SOUNDWAVE] > ZOMBIE"). If they pressed G meanwhile, there's no kill marker but the line
  still goes out.
- `Voice.onDeath` still reads only `source.getEntity()`, so a world-credited kill gets no elimination voice line,
  doesn't add to the multikill chain, and doesn't count for Sojourn's Overclock kill lines (`Sojourn.killed`). The
  feed and ELIMINATED say "your kill" while every other kill system says "not yours".
- Delayed damage after a hero switch (Ana's dose ticks, torpedoes in flight, the Disruptor field, Cyberspace)
  credits the *current* hero's portrait. It's the same mismatch as above and wasn't tried.

### 6. Moderate: a false headshot on Charged Shot, with a damage multiplier, no issue filed
`kf_soj6` shows a Charged Shot crit on a zombie 12.5 m behind the first target, at leg height. The record writes it
off as "not explained, pre-existing, worth a look". The feed's red target is the visible part. The same `crit`
multiplies damage (`CHARGED_CRIT`) and feeds `Sojourn.sharpshooter`, so it's a balance bug, not cosmetic. A likely
cause to check: the hit point is the entry into the box *inflated by* `CHARGED_RADIUS` (`Railgun.java` 280). A ray
going downward enters that box through its raised top face, above `getEyeY() - HEAD_DEPTH`. It needs an issue
before this merges, since the branch now shows crits in the feed for every hero.

### 7. Moderate: an owner-accepted behaviour changed without the owner (#33)
#33 (closed, with the owner's table) has "A mob's kill: a fist" and "No killer: a skull". This branch:
- shows a skeleton's arrow kill (a mob's ranged kill) as the arrow alone. `weapon()` gives "weapon" whenever the
  direct entity isn't the killer.
- shows a death with no killer as the arrow alone, with no skull.

The wiki may support it (**unverified**), but the record doesn't mention that it overturns a table the owner
accepted. Neither case was screenshotted: there's no skeleton kill and no lone fall death of a player in the sheets.

### 8. Minor: code and HUD details
- `Combat.dealing` is a single static variable, so it can't tell two servers apart. That's fine for one integrated
  server, but `Combat.reset` must run on world change; it's in the diff. `blow()` matches only the innermost
  damage. A death of a *different* target, triggered inside another target's hurt (a listener that hurts a
  neighbour with the same attacker), reads null and falls back to "melee" or "weapon". There's no case today, and
  no test.
- The Ender Dragon dies through its DYING phase, not inside `hurt`, so `blow()` is null when it finally dies, and
  its 100-tick memory (`remember`) runs out during the 200-tick death animation. A dragon kill, the one kill the mod
  celebrates (VICTORY, #18), will show the fallback icon. That wasn't tried.
- `eliminatedCaption` looks the victim up by entity id on the client. Past tracking range, or for a world-credited
  victim in another dimension, the kill marker shows without "ELIMINATED".
- ELIMINATED at +250 reference px: checked in `kf_after_respawn_kill.png` (Lúcio). It clears the song indicator
  (+191) and sits about 85 px above the ult ring. The +250 is ours, from no source. In the Lúcio shots taken before
  the move (`kf_sheet_lucio.png`) it ran straight through the crossfade bar, which confirms the old bug. Nobody
  checked Kiriko's ofuda counter (+136) together with Swift Step's marker, or Juno's or Jetpack Cat's meters, at the
  new height.
- Ult icon pulse: a colour tint lerped from gold to white at 1.5 Hz, seen only in still frames. Whether it reads as
  Overwatch's animated ult icon is unchecked (the record says so).
- The perk icons (Superbloom, Local Anesthetic) are drawn noticeably smaller than the ability icons in the same slot
  (`kf_sheet_lw2`, `kf_sheet_ana6`). Check the perk art's padding.
- The text fallback ("PULSAR TORPEDOES" at 9 px) makes the middle block of a line as wide as a name plate. Nobody
  saw it, because every tested icon had art.
- The newest line goes on top (`addFirst`) and lines are drawn top-down. That's pre-existing. Whether Overwatch adds
  at the top or the bottom was never sourced in the record.
- The white "by you" frame is drawn only when you're the killer, not on your own death line. Whether Overwatch
  frames your deaths too isn't sourced.

### 9. Minor: #107 and #108
- #107 is fixed as described: `kf_107_ana_after_*` / `kf_107_sombra_after_*` show no flash, and Kiriko's still
  flashes. The issue also names Sigma's Kinetic Grasp, which only exists on feature/sigma. Check it after that
  merge.
- #108 changes nothing a player can see. Both callers still pass `Action.ABILITY_1`, which is correct for Swift
  Step and Guardian Angel. Closing it is fair, but "Fixes #108" claims a fix for a bug no player could hit. No
  third caller was tried (Life Grip's or Nano Boost's preview).

### 10. Minor: BlowTest is shallow
It checks `Blow.feed()` strings, and that each listed slug appears somewhere in `src/main/java` and fits 32
characters. It doesn't test:
- that a *particular* damage path deals the right kind (the Brigitte melee bug passes it);
- `Combat.blow()` and its save and restore when nested;
- the `recentBlow` window edge (100 vs 101 ticks);
- the GENERIC_KILL exclusion;
- `KillFeed.weapon()` for non-hero killers;
- the client's `icons()` (fist, perk path, text fallback, crit order).

All of those are pure logic and could be unit tests.

## What still hasn't been tried that a player will hit
- A second player:
  - the ally and enemy colours from their side;
  - the frame for your own kill;
  - a hero killing a hero, with both portraits;
  - an assist.
- A real fight:
  - mobs with AI, at night;
  - a skeleton's arrow killing the hero (now the arrow alone; it was the fist);
  - a creeper;
  - lava, the void, drowning after a boop;
  - Sombra's Cyberspace kill on a moving mob.
- Minecraft mode (G):
  - a sword or bow kill by a hero in Minecraft mode, as another hero sees it;
  - a hero who presses G within 5 s of a blow, then the victim falls.
- A hero switch within 5 s of a blow, or with a dose, torpedo or field still running.
- A kill while emoting, and a delayed kill (dose or field) after the killer died.
- The Ender Dragon's death.
- Each hero picked from the select screen and the hero menu, not the dev command.

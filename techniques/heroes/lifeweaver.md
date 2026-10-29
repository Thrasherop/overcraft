# Lifeweaver (hub)

Everything learned about Lifeweaver, by subject. The fifth hero (issue #39), the first of the owner's 2026-10-28 list,
and the first with shields. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · read the way Mercy's kit was: [mercy](mercy.md)

## His notes and sources
- **The data map:** `E:\Overwatch Archive\research\lifeweaver\lifeweaver-map.md` (the 2026-10-29 research pass: his
  graphs decompiled node by node, each finding H / M / L), its detail in `work_kit\` (weapons, platform and dash, grip
  and tree) and `work\clipmap.md` / `clipmap.json`; his extraction in `extract\` (build 154088).
- **Numbers:** `tools/reference/lifeweaver.md` (the public sources: patch notes to 2026-10-20, the wiki's revision
  406788), `tools/reference/lifeweaver-plan.md` (the port's plan and its open items).

## Facts that come up often
- **One model, two looks:** `00BB.118`, third person look `0472.119`, first person `07BC.119` (Jetpack Cat's
  arrangement). No weapon entity: Healing Blossom's flower is geoset 2 of his body, Thorn Volley his gauntlet. Which
  weapon is out is the clips' category (df 0xE4: 7F the Blossom, 7D Thorn Volley).
- **Muzzles** from the look's own hardpoint table: 000A (right hand) for both weapons, 0002 (left wrist) for the
  platform and the seed.
- **His props are models of their own** with their own clips: Petal Platform `12D32`, the tree `1267E`, the seed and
  thrown platform `126CC`.
- **Health 200 + 50 shields:** the first hero with shields (they regenerate after 3 s at 30/s).
- **Where the data and the wiki differ** (the data wins): the platform's throw (50 m/s), the dash (0.25 s at
  28.875 m/s), the Blossom's passive charge (5.175 s), Life Grip's far pull, the seed's 3 m pickup. A thorn-to-blossom
  bonus in his graphs is in no public source: left out until the owner says.

## Building him (2026-10-29: what the first pass learned)
- **Read the notes first:** [animation_import](../animation/animation_import.md) already said `4-*` clips are additive
  and named `add_*`; his clip maps missed it, and thorn fire, the dash and landings collapsed his model onto the camera
  until renamed (`tools/rename_glb_clips.py`, no re-export). His third-person Thorn Volley locomotion clips are upper-body
  overrides (their legs don't move): played over the base run with the upper-body mask (`HeroAnimator.lifeweaverLayers`).
- **The props' clips are bound in the alias tables** (`workliases_0FCC.txt` platform, `aliases_0FFC.txt` tree: alias,
  then the graph node that plays it), which the clip map's "L" guesses predate. Read those, not the guesses.
- **A moving platform in Minecraft** (`PetalPlatformEntity`): its height is a function of its synced phase on both
  sides (no position packets: they made it stutter); it pushes riders as Shulker does (`MoverType.SHULKER`); a player
  collides with it on their own client only (the server's copy runs a tick or two ahead while it moves, rose into the
  player and refused the client's lifted position); phase time never below 0 (a client behind took its rider into the
  ground); riders stay on the ground while carried (else they slid off); riders' landings are guarded until they've
  been in the air off it and landed (the edge tick counts as "on the ground"), and the guard is ticked server-wide (a
  platform gone mid-fall would drop it). Floating on it, a player's "above ground" count is reset (the server's kick).
- **Shields** (`hero/Shields`): every health change passes `setHealth`, so a loss comes off the shields first and a
  gain fills the health under them, then them; their own regeneration bypasses it. The first version stopped healing
  under them, from a misreading: the wiki's Hit points page says shields are recoverable HP, health filled first. A
  dash's heal after a hit on his shields alone healed nothing (2026-10-29, found while checking his sounds).
- **Movement hooks go before the mover reads velocity:** the dash's end speed was overwritten by the step already
  computed from the dash's speed (`OwMovement.travel`: the dash hook is first now).
- **A player pulled by Life Grip moves on their own client** (`GripPayload`, `ClientLifeweaver.grip`), held still for
  the 0.35 s vine; their Jump lets go on their client at once (waiting for the server's word took 0.4 s).

## Open
The plan's "Open" list: the platform's collision and its riders, the tree's canopy, his fall-damage height, and what
ends the platform when he dies.

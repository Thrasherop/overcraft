# Sigma (hub)

Everything learned about Sigma, by subject. Hero 7 of the owner's 2026-10-28 list (issue #68), after Sojourn: a **Tank, Stalwart**
(BZ 2026-02-10), **the first Tank in Overcraft**. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · researched the way Sojourn's was (commit 879578d, branch feature/sojourn) and
Juno's ([juno](juno.md)) · his barrier builds on Brigitte's ([brigitte](brigitte.md))

## His notes and sources (research only so far: nothing built or played)
- **Numbers:** [sigma.md](../../tools/reference/sigma.md): Blizzard's patch notes to 2026-11 and the wiki's revision 408635, each number
  with its game-data value. The last change to him is BZ 2026-07-14, inside the data build (154088, 2026-10-06), so the data's numbers
  are the live ones.
- **The data map:** [sigma-map.md](../../tools/reference/sigma-map.md): his kit and movement node by node (H / M / L); the detail and the
  pipeline in `E:\Overwatch Archive\research\sigma\` (`work_kit\kit_sigma.md`, `work_kit\c_<g>.txt`, `work\clipmap_sigma\`, `extract\`).
- **The plan:** [sigma-plan.md](../../tools/reference/sigma-plan.md): where the sources disagree, **what the Tank role needs that the mod
  lacks** (the role's +150 health, enemy ult -40 %, the 15 % healing reduction, Stalwart's 40 % knockback / slow resistance, a 1.4 x 2.5 m
  body, a barrier that is an entity), what maps onto Overcraft, the order.
- **Clip maps:** `tools/clipmaps/sigma.txt` (3P, model 106F7), `sigma_1p.txt` (106F6), `sigma_weapon.txt` (the hand spheres 106B4),
  `sigma_props.txt` (the barrier C015, its emitter orbs FC88, the Accretion rock C5CA). Written by
  `research\sigma\scripts\clipmaps_sigma.py`, which names each clip by the alias holding it instead of a hand list (below).

## What his data taught (2026-11-02)
- **The public numbers lag the data where a change was indirect.** The barrier's regeneration is a share of its max (0.122 a second, in
  StateModifyHealth's percent mode `m_DD792FF0 = x60671788`): 85/s at 700 health, **79.3/s since the 2026-02-10 cut to 650**, which no
  patch line mentions (BZ still says 85; the wiki's 79.3 was right). Before taking a "per second" number, check the node's mode
  (x00943466 = absolute).
- **A value's main number is `value_m_3E5360A1`;** `values[1]` can be another mode's (his health 047C: 200, values[1] 300; Flux's radius
  1867: 7.5, alt 5.0). `research\sigma\scripts\gvsummary_sigma.py` prints main, alt and readers side by side.
- **Knockback comes as a horizontal / vertical pair** (`m_E33EFEE1` / `m_583C208B`): Accretion's 6 / 3 is the wiki's 6.7 m/s, the
  spheres' -0.75 / 1 their 1.3 m/s inward pull.
- **A skin can swap every model.** The entities name D75A / D7C1 / D7DD / D7C9, the classic skin's `.owentity` files 106F7 / 106F6 /
  106B4 / FC88: map the clips under the skin's model folders (clipinfo's "extracted" prefixes), not the entity's base model.
- **Floating is animation, not movement.** His mover is the default hero's in every field but its size (2.5 m tall, 0.7 m radius,
  crouched 2.05 m), and his locomotion has **one move clip for every direction** (no 8-way run); only the jump is 8-way.
- **The role and sub-role passives live in the shared graph 01C7,** keyed on the hero category (0850 Tank) and sub-role tags (116B
  Stalwart): +150 health, enemy ult x0.6 (x0.8 under a mode flag: 6v6, M), healing reduction x0.5, knockback and slows x(1 - 0.4). The
  next tank (D.Va) reads the same nodes.
- **Writing the clip map from the tree, not by hand:** `clipmaps_sigma.py` names each clip by the innermost alias on its path (the graph
  node that plays it, from `work\animrefs.txt`) plus its path (8-way slot, aim pitch, state child, jump phase), once per clip; the
  shared crouch-walk tree's unbound slots (alias 55) are skipped. Fast and complete; names for the L aliases (the additive layers) still
  want a look at the clips before the code plays them.

## Not yet
Everything in game: the Tank role shell, the hero, every step of the plan's order. Effects, sounds, voice (set 1E5.05F, 1293 subtitled
lines extracted) and his 14 emotes are extracted but not mapped. A Workshop recording (Levitation's rise, Flux flight, the barrier's
glide over slopes) would settle the M items.

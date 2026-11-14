# #127: Guardian Angel from water, ladders, vines (2026-11-13, next 60de166, 0.3.43)

Up: [Mercy](../../../heroes/mercy.md) · [harness](../../harness.md).
**Already fixed on this build: nothing changed.** The issue was filed from `main` 21b0d22; polish 4's c83ddc3 (merged in
48cd86c, before 0.3.43) added `LivingEntityMixin.overcraft$abilityTravel`: an inject at the head of `LivingEntity.travel`
that hands the move to `OwMovement.travel` while `OwMovement.abilityMoving` (GA's glide among them) holds, so water's
`travelInFluid` and the ladder's `handles()` = false never get a say. Every start below flew.

Setup: one dev client (`OC_ASSET_PACK` = the main checkout's pack), fresh flat survival world `ga127`, Normal,
`spawn_monsters false`. Mercy in hero mode, a NoAI villager (an ally to her: `Allies`) 12 m ahead at z 12.5, her at
z 0.5 looking at it, GA by the harness key (`key ability_1` tap: KeyMapping, not the physical key). Position read every
~0.06 s for 1.4 s. Scripts in `scripts/` (`py -I scripts/oc.py scripts/ga127.py out.txt`); raw: `before_60de166*.txt`.

| Start (2 runs each) | Expected | Seen |
|---|---|---|
| plain ground (control) | flies to ~z 8 at 17 m/s | z 0.5 -> 7.9 in ~0.45 s (0.85 m a tick), cooldown runs |
| air (tp 5 m up, GA at once) | the same | -> 8.6; 17.0 hp after (the same drop without GA: 14.0; GA's 1.0 not traced, open) |
| 1-deep water, feet in | flies | -> 7.9/8.0, the same curve as plain |
| submerged, pool 5 deep (`water_deep`, its edge at z 4) | flies, the walls stop her | flew, met the pool's edge at z 4.7, slid up it, went on to 8.9 |
| submerged, long pool (`water_long`) | flies | -> 8.9, rising out of the water |
| lava at the feet, no fire resistance | | she burns to death within ~0.2 s of the press (18 hp, 1 s in lava first): not a GA result |
| lava at the feet / submerged in lava, fire resistance | flies | -> 7.9 / -> 8.9 |
| ladder at the feet; 6-high ladder, her at its foot | flies | -> 7.9 both |
| vines | flies | -> 7.9 |
| scaffolding (3 high, her inside at y -59) | flies | -> 8.4 |
| bubble column up (soul sand) / down (magma) | flies | -> 10.4 (the column had already lifted her to y -55 before the press) / -> 8.0 |

**Checked in code (26.3):** `LivingEntity.travel` picks `travelInFluid` / `travelFallFlying` / `travelInAir`;
`handleOnClimbable` is called only inside `travelInAir`'s relative-move path. The head inject skips all three. What
still runs is outside `travel` (fluid push in `baseTick`, `jumpInLiquid` in `aiStep`, a bubble column's push from
`entityInside`): each only changes the delta movement, which `OwMovement.guardianAngel` overwrites every tick.

Not tried: the physical keyboard (harness keys only); a second player; the Nether; GA from a boat or while riding.
Small unexplained losses of ~0.5 hp (7 Overwatch HP) appeared in some runs of any case, plain included: from the
setup (respawn, tp and fill), not traced.

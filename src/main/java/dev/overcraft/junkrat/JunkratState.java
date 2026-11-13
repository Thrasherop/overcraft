package dev.overcraft.junkrat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Junkrat's live state on the server (HeroState.junkrat; timers in ticks, game times in game ticks). One object, so his kit's fields
 * don't spread through HeroState.
 */
public final class JunkratState {
	/** Concussion Mine: charges, the next charge's progress (ticks), ticks into the throw (-1 none). */
	public int mineCharges = JunkratStats.MINE_CHARGES;
	public double mineRecharge;
	public int mineCast = -1;
	/** The mine out (one at a time), and when it was thrown (game time). */
	public @Nullable UUID mine;
	public long mineThrownAt = -1000;
	/** Steel Trap: the trap out (one at a time), ticks until its cooldown starts (-1 none). */
	public @Nullable UUID trap;
	public int trapCooldownIn = -1;
	/** RIP-Tire: the tire out, and when it came (game time). */
	public @Nullable UUID tire;
	public long tireAt = -1000;
	/** Its time left (ticks), for his HUD. */
	public int tireLeft;
	/** Nitro Boost used on this tire, and when (game time). */
	public boolean nitroUsed;
	public long nitroAt = -1000;
	/** Bomb Voyage's attack speed until (game time). */
	public long voyageUntil = -1000;
	/** Specialist's reload speed until (game time), and whether a reload took it (the reload under way runs faster). */
	public long specialistUntil = -1000;
	public boolean reloadFast;
	/** The fire buttons last tick (the right's press sets the mine off; either's sets the tire off). */
	public boolean rightWas, leftWas;
	/** Hacked now (no abilities: the mine's remote too, W). */
	public boolean silenced;
	/** The dimension his mine, trap and tire are in: a change ends them (they're out of his reach). */
	public @Nullable ResourceKey<Level> level;

	public void clear() {
		mineCharges = JunkratStats.MINE_CHARGES;
		mineRecharge = 0;
		mineCast = -1;
		mine = null;
		mineThrownAt = -1000;
		trap = null;
		trapCooldownIn = -1;
		tire = null;
		tireAt = -1000;
		nitroUsed = false;
		nitroAt = -1000;
		voyageUntil = -1000;
		specialistUntil = -1000;
		reloadFast = false;
		rightWas = leftWas = false;
		silenced = false;
		level = null;
	}
}

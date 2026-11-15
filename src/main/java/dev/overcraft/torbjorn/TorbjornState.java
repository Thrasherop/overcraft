package dev.overcraft.torbjorn;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Torbjörn's kit state on the server (HeroState.torbjorn). Times are game ticks; counters are ticks left or ticks in. */
public final class TorbjornState {
	/** The Forge Hammer is out (else the Rivet Gun); ticks left of a weapon swap. */
	public boolean hammer;
	public int swapTicks;
	/** A hammer swing: ticks into it (-1 none), and whether this swing's hit has landed. */
	public int swing = -1;
	public boolean swingHit;
	/** Ticks of his attack rate into the swing. */
	public double swingAt;
	/** His turret (the entity's id), the world it's in. */
	public @Nullable UUID turret;
	public @Nullable ResourceKey<Level> level;
	/** Deploy Turret's cast: ticks into it (-1 none); whether this cast's throw has left. */
	public int turretCast = -1;
	public boolean thrown;
	/** Overload: ticks left (0 off), its cast's ticks left (his weapons held). */
	public int overload;
	public int overloadCast;
	/** Overload started by Pre-Heated (no cooldown after it). */
	public boolean preHeated;
	/** Molten Core: its cast's ticks into it (-1 none), slag shots left, ticks left of firing, ticks to the next shot. */
	public int ultCast = -1;
	public int slag;
	public int ultLeft;
	public double slagRecovery;
	/** Specialist: its +50 % reload speed until (game time), and whether the reload under way has it. */
	public long specialistUntil = -1000;
	public boolean reloadFast;
	public boolean leftWas;

	public void clear() {
		hammer = false;
		swapTicks = 0;
		swing = -1;
		swingHit = false;
		turret = null;
		level = null;
		turretCast = -1;
		thrown = false;
		overload = 0;
		overloadCast = 0;
		preHeated = false;
		ultCast = -1;
		slag = 0;
		ultLeft = 0;
		slagRecovery = 0;
		specialistUntil = -1000;
		reloadFast = false;
		leftWas = false;
	}
}

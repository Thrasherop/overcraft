package dev.overcraft.illari;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Illari's live state on the server (HeroState.illari; timers in ticks, game times in game ticks). */
public final class IllariState {
	/** The rifle's charge (0..1) and the ticks left before it starts charging again after a shot. */
	public double charge;
	public double chargeWait;
	/** When she last fired and last beamed (game time), for the data's gaps (#544). */
	public long shotAt = -1000, beamAt = -1000;
	/** Solar energy (0..100), the ticks before it regenerates, and whether it was emptied (the beam locked until full). */
	public double energy = IllariStats.ENERGY;
	public double energyWait;
	public boolean emptied;
	/** The beam on, its target (entity id, -1 none). */
	public boolean beaming;
	public int beamTarget = -1;
	/** Healing Pylon: ticks into the throw (-1 none), the pylon out. */
	public int pylonCast = -1;
	public @Nullable UUID pylon;
	/** Outburst: ticks into its cast (-1 none), whether it was held (higher), the move it was cast with. */
	public int outburstCast = -1;
	public boolean outburstHeld;
	public double outburstX, outburstZ;
	/** Captive Sun: ticks into it (-1 none), the ball fired, the flight's length (ticks). */
	public int sun = -1;
	public boolean ballFired;
	public int flightTicks;
	/** Solar Flare: its cooldown (ticks) and its cast (ticks in, -1 none). */
	public double flareCooldown;
	public int flareCast = -1;
	/** The fire buttons last tick. */
	public boolean leftWas;
	/** A launch still counts as moving her (the soft landing) until the server sees her off the ground. */
	public int launchGrace;
	/** Hacked now (no abilities). */
	public boolean silenced;
	/** The dimension her pylon is in. */
	public @Nullable ResourceKey<Level> level;

	public void clear() {
		charge = 0;
		chargeWait = 0;
		shotAt = beamAt = -1000;
		energy = IllariStats.ENERGY;
		energyWait = 0;
		emptied = false;
		beaming = false;
		beamTarget = -1;
		pylonCast = -1;
		pylon = null;
		outburstCast = -1;
		outburstHeld = false;
		outburstX = outburstZ = 0;
		sun = -1;
		ballFired = false;
		flightTicks = 0;
		flareCooldown = 0;
		flareCast = -1;
		leftWas = false;
		launchGrace = 0;
		silenced = false;
		level = null;
	}
}

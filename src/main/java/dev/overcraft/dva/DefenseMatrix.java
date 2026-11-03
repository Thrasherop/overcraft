package dev.overcraft.dva;

import dev.overcraft.Ow;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.dva.DvaStats.*;

/**
 * Defense Matrix (02CC; dva-plan.md "What's new" 7): the mech's right button, held. Up, it drains its 100 energy at 33 a
 * second (3.03 s) and eats every enemy projectile in a box along her aim (2.5 m wide, 2 m tall, 0.8 to 10.25 m ahead: D #205)
 * through {@link Barriers#absorbBox}, 4 % of what it eats to her ult (#255). Once raised it stays up 0.5 s; let go (or
 * empty) it goes down for 1 s (its cooldown), and 0.75 s after it's down the energy comes back at 16 a second. No Fusion
 * meanwhile. Not explosions, beams, melee or hitscan: only projectiles (what Barriers.takes). Stand-in look and sound.
 */
public final class DefenseMatrix {
	private DefenseMatrix() {
	}

	static final Barriers.Box BOX = new Barriers.Box(MATRIX_SIDE, MATRIX_HALF_HEIGHT, MATRIX_HALF_HEIGHT, MATRIX_NEAR, MATRIX_FAR);

	/** Each tick in hero mode (Dva.tick); {@code can}: she's in her mech with her controls. */
	static void tick(ServerPlayer player, HeroState s, boolean can) {
		long now = player.level().getGameTime();
		if (s.dvaMatrixCooldown > 0) {
			s.dvaMatrixCooldown--;
		}
		if (!s.dvaMatrix) {
			if (can && s.secondaryHeld && s.dvaMatrixCooldown <= 0 && s.dvaMatrixEnergy > 0) {
				start(player, s);
			} else {
				regen(s, now);
				return;
			}
		}
		if (!can) {
			// the wreck, G, a stun: down at once (its cooldown all the same)
			end(player, s, now);
			return;
		}
		s.dvaMatrixTicks++;
		s.dvaMatrixEnergy = Math.max(0, s.dvaMatrixEnergy - MATRIX_DRAIN / 20.0);
		absorb(player, s);
		boolean held = s.secondaryHeld || s.dvaMatrixTicks < Ow.ticks(MATRIX_MIN);
		if (!held || s.dvaMatrixEnergy <= 0) {
			end(player, s, now);
		}
	}

	/** The energy back at 16 a second from 0.75 s after it went down (#284 / #389). */
	static void regen(HeroState s, long now) {
		if (s.dvaMatrixEnergy < MATRIX_ENERGY && now - s.dvaMatrixDownAt >= Ow.ticks(MATRIX_REGEN_DELAY)) {
			s.dvaMatrixEnergy = Math.min(MATRIX_ENERGY, s.dvaMatrixEnergy + MATRIX_REGEN / 20.0);
		}
	}

	private static void start(ServerPlayer player, HeroState s) {
		s.dvaMatrix = true;
		s.dvaMatrixTicks = 0;
		// stand-in sound (hers: step 11); its first-person arms' clips (0x3F6: 334E ..) come with the effects
		player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.8f);
	}

	static void end(ServerPlayer player, HeroState s, long now) {
		if (!s.dvaMatrix) {
			return;
		}
		s.dvaMatrix = false;
		s.dvaMatrixDownAt = now;
		s.dvaMatrixCooldown = Ow.ticks(MATRIX_COOLDOWN);
		player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.4f, 1.8f);
	}

	private static void absorb(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Barriers.absorbBox(player, BOX, (p, at, damage) -> {
			// 4 % of what it eats to her ult (#255, M)
			Combat.addUltCharge(player, damage * MATRIX_ULT_SHARE, false);
			level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 4, 0.05, 0.05, 0.05, 0.02);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 1.6f);
		});
		// stand-in look: a few sparks through the box (past 3 m, out of her face)
		Vec3 eye = player.getEyePosition();
		Vec3 f = player.getViewVector(1.0f);
		Vec3 r = f.cross(new Vec3(0, 1, 0));
		r = r.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : r.normalize();
		Vec3 u = r.cross(f).normalize();
		var random = player.getRandom();
		for (int i = 0; i < 3; i++) {
			double z = 3 + (MATRIX_FAR - 3) * random.nextDouble();
			double x = (random.nextDouble() * 2 - 1) * MATRIX_SIDE;
			double y = (random.nextDouble() * 2 - 1) * MATRIX_HALF_HEIGHT;
			Vec3 at = eye.add(f.scale(z)).add(r.scale(x)).add(u.scale(y));
			level.sendParticles(ParticleTypes.GLOW, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		}
	}
}

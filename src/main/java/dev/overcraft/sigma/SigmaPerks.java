package dev.overcraft.sigma;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Sleep;
import dev.overcraft.net.HeroInputPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Sigma's perks (51E0: sigma-map.md "Perks"): Kinetic Cycle and Hyper Regeneration (minor), Hyper Strike and Levitation
 * (major). Levitation moves him on his own client (OwMovement, ClientSigma); its landing is safe here. "Enemy heroes" in
 * the data's filters are every creature he can hurt (sigma-plan.md "Minecraft decisions"). Stand-in look and sound.
 */
public final class SigmaPerks {
	private SigmaPerks() {
	}

	/** Kinetic Cycle: Grasp ended normally having absorbed {@code absorbed}: Accretion's cooldown 0.02 s shorter a point. */
	static void graspEnded(HeroState s, double absorbed) {
		if (s.has(Perk.SIGMA_KINETIC_CYCLE) && absorbed > 0) {
			s.ability2Cooldown = Math.max(0, s.ability2Cooldown - absorbed * KINETIC_CYCLE_PER_POINT * 20);
		}
	}

	/** His spheres dealt {@code dealt} to a creature ({@code direct}: a direct hit): Hyper Regeneration and Hyper Strike's count. */
	static void sphereDealt(ServerPlayer owner, double dealt, boolean direct) {
		if (dealt <= 0 || !(HeroManager.state(owner) instanceof HeroState s)) {
			return;
		}
		if (s.has(Perk.SIGMA_HYPER_REGENERATION) && s.sigmaNow >= s.barrierBrokenUntil) {
			// 30% of it to the barrier, out or with him, unless it's broken (no barrier to tell: #436 -> 1A2A #1039)
			s.barrierHealth = Math.min(BARRIER_HEALTH, s.barrierHealth + dealt * HYPER_REGENERATION);
		}
		if (direct && s.has(Perk.SIGMA_HYPER_STRIKE) && !s.sigmaStrikeReady) {
			s.sigmaDirectHits = Math.min(HYPER_STRIKE_HITS, s.sigmaDirectHits + 1);
			if (s.sigmaDirectHits >= HYPER_STRIKE_HITS) {
				s.sigmaStrikeReady = true;
				owner.level().playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 2f);
			}
		}
	}

	/**
	 * His quick melee landed on {@code target} (the first one it hit): with Hyper Strike charged, 50 more, knocked away 14 m/s
	 * along his aim lifted 0.3, floating 0.8 s with its keys off; the count starts again.
	 */
	public static void meleeHit(ServerPlayer hero, HeroState s, LivingEntity target) {
		if (!s.sigmaStrikeReady || !s.has(Perk.SIGMA_HYPER_STRIKE)) {
			return;
		}
		s.sigmaStrikeReady = false;
		s.sigmaDirectHits = 0;
		ServerLevel level = hero.level();
		Vec3 before = target.getDeltaMovement();
		DamageSource source = hero.damageSources().playerAttack(hero);
		float dealt = Combat.damage(target, source, HYPER_STRIKE_DAMAGE);
		Combat.credit(hero, dealt, target);
		Combat.addFireForDamage(hero, dealt, Ow.toOw(target.getMaxHealth()));
		if (dealt > 0) {
			Effects.hitMarker(hero, target, false);
		}
		if (target.isAlive()) {
			Vec3 dir = hero.getViewVector(1.0f).add(0, HYPER_STRIKE_LIFT, 0).normalize();
			Accretion.push(target, Vec3.ZERO, dir, HYPER_STRIKE_SPEED, 0);
			Sleep.knockdown(target, HYPER_STRIKE_FLOAT, HYPER_STRIKE_GRAVITY);
		}
		level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 1, 0, 0, 0, 0);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 1f, 1.2f);
	}

	/** His client's extra input: EXTRA_JUMP, Levitation began (OwMovement lifts him): its landing is safe (#35). */
	static void extra(ServerPlayer player, HeroState s, int extra) {
		if ((extra & HeroInputPayload.EXTRA_JUMP) != 0 && s.has(Perk.SIGMA_LEVITATION) && player.isAlive()) {
			s.softLanding = true;
			player.resetFallDistance();
		}
	}
}

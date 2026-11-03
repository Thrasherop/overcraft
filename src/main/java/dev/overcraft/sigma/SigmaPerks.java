package dev.overcraft.sigma;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Sleep;
import dev.overcraft.net.HeroInputPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Sigma's perks (51E0: sigma-map.md "Perks"): Kinetic Cycle and Hyper Regeneration (minor), Hyper Strike and Levitation
 * (major). Levitation moves him on his own client (OwMovement, ClientSigma); its landing is safe here. "Enemy heroes" in
 * the data's filters are every creature he can hurt (sigma-plan.md "Minecraft decisions"). Their looks and sounds from his
 * data (step 10: tools/reference/sigma-effects.md "Perks", sigma-sounds.md 6).
 */
public final class SigmaPerks {
	private SigmaPerks() {
	}

	/** Kinetic Cycle: Grasp ended normally having absorbed {@code absorbed}: Accretion's cooldown 0.02 s shorter a point. */
	static void graspEnded(ServerPlayer player, HeroState s, double absorbed) {
		if (s.has(Perk.SIGMA_KINETIC_CYCLE) && absorbed > 0) {
			s.ability2Cooldown = Math.max(0, s.ability2Cooldown - absorbed * KINETIC_CYCLE_PER_POINT * 20);
			// 1D02 #735 -> #738 -> #737 message 2623 -> #741 6731 (C6D9): his own
			SigmaSounds.own(player, "perk.kinetic_cycle");
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
			// its heal on the barrier out (1A2A #1043 6E81, no sound), once a 0.5 s (its length)
			var b = SigmaBarrier.out(owner, s);
			long now = owner.level().getGameTime();
			if (b != null && now - s.sigmaHealFxAt >= 10) {
				s.sigmaHealFxAt = now;
				Effects.play(owner.level(), "sigma_barrier_heal", b.barrierCentre(), b.barrierFacing(), null, 0, owner);
			}
		}
		if (direct && s.has(Perk.SIGMA_HYPER_STRIKE) && !s.sigmaStrikeReady) {
			s.sigmaDirectHits = Math.min(HYPER_STRIKE_HITS, s.sigmaDirectHits + 1);
			if (s.sigmaDirectHits >= HYPER_STRIKE_HITS) {
				s.sigmaStrikeReady = true;
				// #389 -> #409 6545 (C320): his own; its look (65C7 at his left wrist) from SIGMA_STATE's SYNC_STRIKE (SigmaFx)
				SigmaSounds.own(owner, "perk.hyper_strike.ready");
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
		dev.overcraft.hero.Blow blow = dev.overcraft.hero.Blow.perk("hyper_strike").by(dev.overcraft.hero.Hero.SIGMA);
		float dealt = Combat.damage(target, source, HYPER_STRIKE_DAMAGE, blow);
		Combat.credit(hero, dealt, target);
		Combat.addFireForDamage(hero, dealt, Ow.toOw(target.getMaxHealth()));
		if (dealt > 0) {
			Effects.hitMarker(hero, target, false);
		}
		if (target.isAlive()) {
			Vec3 dir = hero.getViewVector(1.0f).add(0, HYPER_STRIKE_LIFT, 0).normalize();
			Accretion.push(target, Vec3.ZERO, dir, HYPER_STRIKE_SPEED, 0);
			Combat.pushed(target, hero, blow);
			Sleep.knockdown(target, HYPER_STRIKE_FLOAT, HYPER_STRIKE_GRAVITY);
		}
		// 54A5 on the target: 2BE7 (the lifted look, its 0.8 s float) and 6550 / 6551 / 67C0's sounds (the target's own, others' 500 m)
		Vec3 mid = target.position().add(0, target.getBbHeight() / 2, 0);
		Effects.play(level, "sigma_strike", mid, Vec3.ZERO, target, 0, hero);
		if (target instanceof ServerPlayer struck) {
			SigmaSounds.own(struck, "perk.hyper_strike.hit");
		}
		SigmaSounds.at(level, "perk.hyper_strike.hit_3p", mid, 500,
			target instanceof ServerPlayer p ? new net.minecraft.world.entity.player.Player[] {p} : new net.minecraft.world.entity.player.Player[0]);
	}

	/** His client's extra input: EXTRA_JUMP, Levitation began (OwMovement lifts him): its landing is safe (#35). */
	static void extra(ServerPlayer player, HeroState s, int extra) {
		if ((extra & HeroInputPayload.EXTRA_JUMP) != 0 && s.has(Perk.SIGMA_LEVITATION) && player.isAlive()) {
			s.softLanding = true;
			player.resetFallDistance();
			// others see it (65BF) and hear 652D's C2F7 once, 70 m (his own loop is his client's)
			s.sigmaLevitateUntil = player.level().getGameTime() + Ow.ticks(LEVITATION_TIME);
			SigmaSounds.at(player.level(), "perk.levitation_3p", player.position(), 70, player);
		}
	}

	/** Each tick of his kit: Levitation, as others see it, ends with its 1.75 s or a landing (ours: his release isn't sent). */
	static void tick(ServerPlayer player, HeroState s) {
		if (s.sigmaLevitateUntil >= 0 && (player.onGround() || !player.isAlive() || player.level().getGameTime() >= s.sigmaLevitateUntil)
			&& player.level().getGameTime() - (s.sigmaLevitateUntil - Ow.ticks(LEVITATION_TIME)) > 2) {
			s.sigmaLevitateUntil = -1;
		}
	}
}

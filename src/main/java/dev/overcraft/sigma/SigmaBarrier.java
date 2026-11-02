package dev.overcraft.sigma;

import dev.overcraft.Ow;
import dev.overcraft.entity.SigmaBarrierEntity;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Experimental Barrier (1B55 on him, 1A2A on the barrier: sigma-map.md): with him it is put away (state 1, regenerating
 * 79.3 a second from 2 s after it came back); the right button sends it out (2), 2.3 m ahead of his eyes along his aim
 * (less before a wall), moving on along his aim at 20 m/s while the button is held, stopping when let go (3) or when a
 * block is 0.9 m ahead; a press while it's out recalls it (4), and it can go out again 1 s later. Broken (its 650 gone), it
 * comes back to him and can't go out for 5 s. 2% of what it takes goes to his ult. Options (Toggle, Reload recalls) not
 * yet: Overwatch's defaults (both off). Stand-in look and sound for its moments (his own: step 10).
 */
public final class SigmaBarrier {
	private SigmaBarrier() {
	}

	/** His barrier out in the world, or null (put away). */
	public static @Nullable SigmaBarrierEntity out(ServerPlayer player, HeroState s) {
		if (s.sigmaBarrierId == null) {
			return null;
		}
		if (player.level().getEntity(s.sigmaBarrierId) instanceof SigmaBarrierEntity b && b.isAlive()) {
			return b;
		}
		s.sigmaBarrierId = null;
		return null;
	}

	/** Each tick of his kit. {@code locked}: Kinetic Grasp holds his hands (no sending). */
	static void tick(ServerPlayer player, HeroState s, boolean controls, boolean locked) {
		long now = player.level().getGameTime();
		boolean pressed = s.secondaryHeld && !s.sigmaRightWas;
		s.sigmaRightWas = s.secondaryHeld;
		SigmaBarrierEntity b = out(player, s);
		if (b != null) {
			if (!controls || !player.isAlive()) {
				// G, death: it comes back to him
				stow(player, s, b);
			} else if (pressed) {
				// a press while it's out recalls it (1B55 #721 / #916)
				stow(player, s, b);
				Combat.animate(player, "barrier_recall");
				player.level().playSound(null, b.getX(), b.getY() + 2, b.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
			} else if (b.state() == SigmaBarrierEntity.SENT && !s.secondaryHeld) {
				// let go: it stops where it is (#1017 / #1051)
				b.setState(SigmaBarrierEntity.STOPPED);
			}
		} else if (pressed && controls && player.isAlive() && !locked && now >= s.sigmaBarrierReadyAt && now >= s.barrierBrokenUntil
			&& s.barrierHealth > 0) {
			deploy(player, s);
		}
		if (out(player, s) == null && now - s.barrierDownSince >= Ow.ticks(BARRIER_REGEN_DELAY)) {
			s.barrierHealth = Math.min(BARRIER_HEALTH, s.barrierHealth + BARRIER_REGEN / 20);
		}
	}

	/** It goes out: its centre along a 3.2 m ray from his eyes, 0.9 m short of what the ray hits (1A2A #740 / #738). */
	private static void deploy(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 aim = player.getViewVector(1.0f);
		HitResult hit = level.clip(new ClipContext(eye, eye.add(aim.scale(BARRIER_DEPLOY_RAY)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		double reach = hit.getType() == HitResult.Type.MISS ? BARRIER_DEPLOY_RAY : hit.getLocation().distanceTo(eye);
		Vec3 centre = eye.add(aim.scale(Math.max(0.3, reach - BARRIER_DEPLOY_BACK)));
		SigmaBarrierEntity b = new SigmaBarrierEntity(level, player, centre, player.getYRot());
		level.addFreshEntity(b);
		s.sigmaBarrierId = b.getId();
		// his deploy clip (0x1B7, 1B55 #879) and a stand-in sound
		Combat.animate(player, "barrier_cast");
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
	}

	/**
	 * While sent: along his aim now (v40.v2083, his live aim: M) at 20 m/s, facing the way he faces, until a block is 0.9 m
	 * ahead of its centre (#638); no range limit (9999 m).
	 */
	public static void move(ServerPlayer owner, SigmaBarrierEntity b) {
		Vec3 dir = owner.getViewVector(1.0f);
		Vec3 centre = b.barrierCentre();
		double step = Ow.speed(BARRIER_SPEED);
		HitResult hit = b.level().clip(new ClipContext(centre, centre.add(dir.scale(BARRIER_STOP_RAY + step)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, b));
		double room = hit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : hit.getLocation().distanceTo(centre) - BARRIER_STOP_RAY;
		double go = Math.max(0, Math.min(step, room));
		b.setYRot(owner.getYRot());
		if (go > 0) {
			b.setPos(b.position().add(dir.scale(go)));
		}
		if (room <= step) {
			b.setState(SigmaBarrierEntity.STOPPED);
		}
	}

	/** It takes {@code ow} at {@code at}: off its 650, 2% to his ult (1A2A #683, M) and to his perk progress (#684, M); broken at 0. */
	public static double take(ServerPlayer owner, SigmaBarrierEntity b, double ow, Vec3 at) {
		HeroState s = dev.overcraft.hero.HeroManager.state(owner);
		if (s == null || ow <= 0) {
			return 0;
		}
		double took = Math.min(ow, s.barrierHealth);
		s.barrierHealth -= took;
		Combat.addUltCharge(owner, took * BARRIER_ULT_SHARE, false);
		Perks.add(owner, took);
		ServerLevel level = owner.level();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.1);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
		if (s.barrierHealth <= 0.0001) {
			s.barrierHealth = 0;
			long now = level.getGameTime();
			s.barrierBrokenUntil = now + Ow.ticks(BARRIER_BROKEN_COOLDOWN);
			Vec3 c = b.barrierCentre();
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 120, 2.5, 1.6, 0.2, 0.05);
			level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f);
			b.discard();
			s.sigmaBarrierId = null;
			s.barrierDownSince = now;
		}
		return took;
	}

	/** Back to him: 1 s before it can go out again (0493, #1041), its regeneration from 2 s on. */
	private static void stow(ServerPlayer player, HeroState s, SigmaBarrierEntity b) {
		long now = player.level().getGameTime();
		b.discard();
		s.sigmaBarrierId = null;
		s.sigmaBarrierReadyAt = now + Ow.ticks(BARRIER_RECALL_COOLDOWN);
		s.barrierDownSince = now;
	}

	/** Leaving him (another hero, leaving the world, death): it goes. */
	public static void clear(ServerPlayer player, HeroState s) {
		SigmaBarrierEntity b = out(player, s);
		if (b != null) {
			b.discard();
		}
		s.sigmaBarrierId = null;
	}

	/** Seconds before it can go out again (the recall's 1 s or the break's 5 s), or -1. */
	static float cooldown(long now, HeroState s) {
		long ready = Math.max(s.sigmaBarrierReadyAt, s.barrierBrokenUntil);
		return s.sigmaBarrierId == null && ready > now ? (ready - now) / 20f : -1;
	}
}

package dev.overcraft.sigma;

import dev.overcraft.Ow;
import dev.overcraft.entity.SigmaBarrierEntity;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Perks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * yet: Overwatch's defaults (both off). Its look and sounds from his data (step 10: sigma-effects.md "Experimental Barrier",
 * sigma-sounds.md 2; its travel and hum loops are his clients', its field and corner motes SigmaBarrierRenderer / SigmaFx).
 */
public final class SigmaBarrier {
	private SigmaBarrier() {
	}

	/** His barrier out in the world, or null (put away). */
	/** He's pushing it out now (sent, the button held): his hands are on it (no spheres, no Grasp, no rock: the wiki). */
	static boolean sending(ServerPlayer player, HeroState s) {
		SigmaBarrierEntity b = out(player, s);
		return b != null && b.state() == SigmaBarrierEntity.SENT && s.secondaryHeld;
	}

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
		boolean held = Sleep.asleep(player) && player.isAlive();
		boolean hacked = dev.overcraft.sombra.Hack.silenced(player);
		if (b != null) {
			if (held) {
				// asleep, stunned or knocked down: it stops where it is, still out (the wiki: "Being stunned or knocked down
				// stops the barrier at its current location, but will not cause an active barrier to be retracted"; the
				// critique's #10: it had been recalled)
				if (b.state() == SigmaBarrierEntity.SENT) {
					b.setState(SigmaBarrierEntity.STOPPED);
					halted(player, b);
				}
			} else if (!controls || !player.isAlive() || hacked) {
				// G, death, a hack (the wiki: "automatically recalled if Sigma is ... hacked"): it comes back to him
				stow(player, s, b);
			} else if (pressed) {
				// a press while it's out recalls it (1B55 #721 / #916): its halt 2C96 as the recall starts (1A2A #886), his
				// arm's recall foley (1P 6F65's 59D6), the barrier's 2C90 flying back to him (#675 / #691, health >= 1) and his
				// hand's 2DD7; back whole: 54CA his, 54C8 others' (40 m) and "Return to me!" (0B9F, #712)
				Vec3 c = b.barrierCentre();
				Effects.play(player.level(), "sigma_barrier_recall", c, b.barrierFacing(), player, 0, player);
				SigmaSounds.splitAt(player, "barrier.stop", c, 40, 40);
				SigmaSounds.own(player, "barrier.recall");
				SigmaSounds.split(player, "barrier.return", player.getEyePosition(), 40);
				dev.overcraft.hero.Voice.say(player, "barrier_recall", 1);
				stow(player, s, b);
				Combat.animate(player, "barrier_recall");
			} else if (b.state() == SigmaBarrierEntity.SENT && !s.secondaryHeld) {
				// let go: it stops where it is (#1017 / #1051)
				b.setState(SigmaBarrierEntity.STOPPED);
				halted(player, b);
			}
		} else if (pressed && controls && player.isAlive() && !locked && !hacked && now >= s.sigmaBarrierReadyAt && now >= s.barrierBrokenUntil
			&& s.barrierHealth > 0) {
			deploy(player, s);
		}
		if (out(player, s) == null && now - s.barrierDownSince >= Ow.ticks(BARRIER_REGEN_DELAY)) {
			s.barrierHealth = Math.min(BARRIER_HEALTH, s.barrierHealth + BARRIER_REGEN / 20);
		}
		calls(player, s, now);
	}

	/**
	 * His barrier's lines, checked each second (1B55 #967 / #971): "The barrier is weakened!" (0BA1) as it falls under 34 %
	 * (#973 onTrue), "Barrier is sustaining damage!" (0BA0) when it lost 10 % of its max in that second (#969). Its cracking
	 * (457B, #462 / #463) as it falls under 50 % and 25 %: SigmaBarrier.take.
	 */
	private static void calls(ServerPlayer player, HeroState s, long now) {
		if (now % 20 != 0) {
			return;
		}
		boolean under = s.barrierHealth < BARRIER_HEALTH * 0.34;
		if (s.sigmaBarrierSample >= 0 && s.sigmaBarrierSample - s.barrierHealth >= BARRIER_HEALTH * 0.1) {
			dev.overcraft.hero.Voice.say(player, "barrier_damaged", 1);
		}
		if (under && !s.sigmaBarrierUnder && s.barrierHealth > 0) {
			dev.overcraft.hero.Voice.say(player, "barrier_low", 1);
		}
		s.sigmaBarrierUnder = under;
		s.sigmaBarrierSample = s.barrierHealth;
	}

	/** It stopped in place (state 3, 1A2A #696): its halt 2C96, 5E03 his at it (40 m: ours, its 3P copy's), 54E7 others' 40 m. */
	static void halted(ServerPlayer owner, SigmaBarrierEntity b) {
		SigmaSounds.splitAt(owner, "barrier.stop", b.barrierCentre(), 40, 40);
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
		// his deploy clip (0x1B7, 1B55 #879: 1P 6F66's 59D8 his, 3P 738E's 5A95 + 5A94 others' 40 m) and the send (his 1P
		// 7135's 5E04, the 3P 6E84's 595D + 54C9 + 5A93 40 m), his line "Barrier initialized!" (0B9E, 1A2A #711 as it's sent)
		Combat.animate(player, "barrier_cast");
		SigmaSounds.split(player, "barrier.deploy", player.getEyePosition(), 40);
		SigmaSounds.split(player, "barrier.send", player.getEyePosition(), 40);
		dev.overcraft.hero.Voice.say(player, "barrier", 1);
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
			halted(owner, b);
		}
	}

	/** It takes {@code ow} at {@code at}: off its 650, 2% to his ult (1A2A #683, M) and to his perk progress (#684, M); broken at 0. */
	public static double take(ServerPlayer owner, SigmaBarrierEntity b, double ow, Vec3 at) {
		HeroState s = dev.overcraft.hero.HeroManager.state(owner);
		if (s == null || ow <= 0) {
			return 0;
		}
		double took = Math.min(ow, s.barrierHealth);
		double before = s.barrierHealth / BARRIER_HEALTH;
		s.barrierHealth -= took;
		double after = s.barrierHealth / BARRIER_HEALTH;
		Combat.addUltCharge(owner, took * BARRIER_ULT_SHARE, false);
		Perks.add(owner, took);
		ServerLevel level = owner.level();
		// a hit on it: no effect of its own (1A2A #685 only feeds his ult, H); the attackers' contact on barrier material
		// 0015 (1833 / 1834), the shared barrier impact
		dev.overcraft.hero.HeroSounds.at(level, dev.overcraft.brigitte.BrigitteSounds.of("hit_barrier"), at, 100);
		if (after > 0 && (before >= 0.5 && after < 0.5 || before >= 0.25 && after < 0.25)) {
			// its cracking 457B (3CBE, 120 m) under 50 % and 25 % (#462 / #463 onTrue -> #464)
			SigmaSounds.at(level, "barrier.low", b.barrierCentre(), 120);
		}
		if (s.barrierHealth <= 0.0001) {
			s.barrierHealth = 0;
			long now = level.getGameTime();
			s.barrierBrokenUntil = now + Ow.ticks(BARRIER_BROKEN_COOLDOWN);
			Vec3 c = b.barrierCentre();
			// broken: 4E26 stopped or held, 2E66 while moving (its barrier-shaped flash: SigmaFx), 457A + 2E66's sounds (49F6 +
			// 1696 his, 49F8 75 m + 1697 others'), "The barrier has fallen!" (0BA2, #713: it interrupts)
			Effects.play(level, "sigma_barrier_break", c, b.barrierFacing(), null, b.state() == SigmaBarrierEntity.SENT ? 1 : 0, owner);
			SigmaSounds.splitAt(owner, "barrier.break", c, 75, 75);
			dev.overcraft.hero.Voice.say(owner, "barrier_broken", 1);
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

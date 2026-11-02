package dev.overcraft.juno;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static dev.overcraft.juno.JunoStats.*;

/**
 * Juno's Pulsar Torpedoes (her graphs 4695, 4B99 per target, 4795 on allies, 47DD on the torpedo; juno-map.md). The right
 * button starts the lock (a toggle: her data's default); for up to 4 s every ally or enemy creature in sight within 40 m
 * (45 once it's being locked) near her aim gathers a lock, 0.35 s at 5 m to 1.0 s at 40 m; the left button (or the right
 * again, or the 4 s up) fires one homing torpedo at each locked target 0.08 s apart from her back pod. Allies: 85 at once and
 * 50 over 2.5 s; enemies 85, stopped by blocks. 10 s cooldown, 2 s with nothing locked. While locking she can't shoot.
 */
public final class Torpedoes {
	/** One torpedo in flight: from whom, at whom, where it is, healing or not. */
	private record Flying(ServerPlayer juno, LivingEntity target, Vec3[] at, boolean heal, long launched) {
	}

	/** A heal over time on an ally: how much a tick, until when. */
	private record Heal(ServerPlayer juno, LivingEntity ally, double perTick, long until) {
	}

	private static final List<Flying> FLYING = new ArrayList<>();
	private static final List<Heal> HEALS = new ArrayList<>();

	private Torpedoes() {
	}

	/** Each tick of her kit (after the buttons' edges are known). */
	static void tick(ServerPlayer player, HeroState s, boolean rightPressed, boolean leftPressed) {
		if (s.junoFiring.size() > 0) {
			launch(player, s);
			return;
		}
		if (s.junoLock < 0) {
			if (rightPressed && s.ability3Cooldown <= 0) {
				s.junoLock = 0;
				s.junoLockProgress.clear();
				s.junoLocked.clear();
				s.junoVolley = 0;
				Combat.animate(player, "torpedo_lock");
			}
			return;
		}
		s.junoLock++;
		track(player, s);
		if (leftPressed || rightPressed || s.junoLock >= Ow.ticks(LOCK_WINDOW)) {
			fire(player, s);
		}
	}

	/** Every candidate in sight near her aim gathers its lock; out of sight or range it pauses (W), never resets. */
	private static void track(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		double cos = Math.cos(Math.toRadians(LOCK_CONE / 2));
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(LOCK_RANGE + LOCK_FORGIVE),
			e -> e != player && e.isAlive() && !e.isSpectator() && (Allies.isAlly(player, e) || Allies.canHurt(player, e)))) {
			int id = e.getId();
			if (s.junoLocked.contains(id)) {
				continue;
			}
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			double d = to.length();
			boolean started = s.junoLockProgress.containsKey(id);
			if (d > (started ? LOCK_RANGE + LOCK_FORGIVE : LOCK_RANGE) || d < 1.0E-3 || to.scale(1 / d).dot(look) < cos
				|| level.clip(new ClipContext(eye, e.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
					.getType() != HitResult.Type.MISS) {
				continue;
			}
			// lock time by distance (4B99 #193): 0.35 s at 5 m or nearer, 1.0 s at 40 m; Familiar Vitals x0.65 on allies (step 6)
			double need = Math.min(LOCK_MAX, LOCK_MAX - (LOCK_MAX - LOCK_MIN) * (40 - d) / 35)
				* (Allies.isAlly(player, e) && dev.overcraft.hero.HeroManager.hasPerk(player, dev.overcraft.hero.Perk.JUNO_FAMILIAR_VITALS)
					? FAMILIAR_VITALS : 1);
			int progress = s.junoLockProgress.getOrDefault(id, 0) + 1;
			s.junoLockProgress.put(id, progress);
			if (progress >= Ow.ticks(need)) {
				s.junoLocked.add(id);
				Effects.play(level, "juno_locked", e.position(), Vec3.ZERO, e, 0, player);
			}
		}
	}

	/** The lock ends: with targets their torpedoes go (0.08 s apart) and 10 s of cooldown; with none, 2 s. */
	private static void fire(ServerPlayer player, HeroState s) {
		s.junoLock = -1;
		s.junoLockProgress.clear();
		if (s.junoLocked.isEmpty()) {
			s.ability3Cooldown = Ow.ticks(TORPEDO_COOLDOWN_NONE);
			Combat.animate(player, "torpedo_cancel");
			return;
		}
		s.junoFiring.addAll(s.junoLocked);
		s.junoLocked.clear();
		s.junoFireClock = 1;
		s.junoLockedOn = 0;
		s.ability3Cooldown = Ow.ticks(TORPEDO_COOLDOWN);
		Combat.animate(player, "torpedo_fire");
	}

	/** One torpedo every 0.08 s from the pod on her back (hp 048D / 048E: 1.35 m up, 0.19 m behind, M sides). */
	private static void launch(ServerPlayer player, HeroState s) {
		s.junoFireClock += 1 / (TORPEDO_GAP * 20);
		while (s.junoFireClock >= 1 && !s.junoFiring.isEmpty()) {
			s.junoFireClock -= 1;
			Entity e = player.level().getEntity(s.junoFiring.remove(0));
			if (!(e instanceof LivingEntity target) || !target.isAlive()) {
				continue;
			}
			float yaw = player.getYRot() * (float) (Math.PI / 180);
			Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(0.19);
			Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale(s.junoFiring.size() % 2 == 0 ? 0.04 : -0.04);
			Vec3 from = player.position().add(0, 1.35, 0).add(back).add(side);
			boolean heal = Allies.isAlly(player, target);
			FLYING.add(new Flying(player, target, new Vec3[] {from}, heal, player.level().getGameTime()));
			Effects.play(player.level(), "juno_torpedo", from, Vec3.ZERO, target, heal ? 1 : 0, player);
		}
	}

	/** Each server tick: the torpedoes home in at 24 m/s (the enemy's from 7.2 m/s: D 2960 0.3, M), and the heals tick. */
	public static void tickAll(MinecraftServer server) {
		for (Iterator<Flying> it = FLYING.iterator(); it.hasNext(); ) {
			Flying f = it.next();
			ServerLevel level = f.juno.level();
			long age = level.getGameTime() - f.launched;
			if (!f.target.isAlive() || f.target.isRemoved() || f.juno.isRemoved() || age > Ow.ticks(TORPEDO_LIFE)) {
				it.remove();
				continue;
			}
			Vec3 at = f.at[0];
			Vec3 aim = f.target.getBoundingBox().getCenter();
			// the enemy's starts slow (24 x 0.3) and reaches full speed over its first 0.3 s (ours, M: its curve motion)
			double speed = f.heal ? TORPEDO_SPEED : TORPEDO_SPEED * Math.min(1, ENEMY_LAUNCH + (1 - ENEMY_LAUNCH) * age / Ow.ticks(0.3));
			Vec3 to = aim.subtract(at);
			double step = Ow.speed(speed);
			Vec3 next = to.length() <= step ? aim : at.add(to.normalize().scale(step));
			if (!f.heal && level.clip(new ClipContext(at, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, f.juno)).getType()
				!= HitResult.Type.MISS) {
				// damage torpedoes are stopped by terrain (W); healing ones follow their ally through it
				it.remove();
				continue;
			}
			f.at[0] = next;
			if (next.distanceTo(aim) > 0.6) {
				continue;
			}
			it.remove();
			if (f.heal) {
				float healed = Combat.heal(f.target, TORPEDO_HEAL);
				credit(f.juno, healed);
				HEALS.removeIf(h -> h.ally == f.target);
				HEALS.add(new Heal(f.juno, f.target, TORPEDO_HEAL_OVER_TIME / Ow.ticks(TORPEDO_HEAL_TIME),
					level.getGameTime() + Ow.ticks(TORPEDO_HEAL_TIME)));
			} else {
				float dealt = Combat.damage(f.target, source(f.juno), TORPEDO_DAMAGE);
				Combat.credit(f.juno, dealt);
				Combat.addFireForDamage(f.juno, dealt, Ow.toOw(f.target.getMaxHealth()));
				if (dealt > 0) {
					Effects.hitMarker(f.juno, f.target, false);
					lockedOn(f.juno);
				}
			}
			Effects.play(level, "juno_torpedo_hit", next, Vec3.ZERO, f.target, f.heal ? 1 : 0, f.juno);
		}
		HEALS.removeIf(h -> {
			if (!h.ally.isAlive() || h.ally.isRemoved() || h.ally.level().getGameTime() >= h.until) {
				return true;
			}
			credit(h.juno, Combat.heal(h.ally, h.perTick));
			return false;
		});
	}

	/** Locked On: an enemy hit takes 1 s off the torpedoes' cooldown, 5 s at most a firing. */
	private static void lockedOn(ServerPlayer juno) {
		HeroState s = dev.overcraft.hero.HeroManager.state(juno);
		if (s == null || !dev.overcraft.hero.HeroManager.hasPerk(juno, dev.overcraft.hero.Perk.JUNO_LOCKED_ON)) {
			return;
		}
		double take = Math.min(Ow.ticks(LOCKED_ON), Ow.ticks(LOCKED_ON_MAX) - s.junoLockedOn);
		if (take > 0) {
			s.junoLockedOn += take;
			s.ability3Cooldown = Math.max(0, s.ability3Cooldown - take);
		}
	}

	private static void credit(ServerPlayer juno, float healed) {
		if (healed > 0) {
			Combat.credit(juno, healed);
			Combat.addFireForHealing(juno, healed);
		}
	}

	/** Damage from her torpedo (an explosion's type would push: a projectile's). */
	private static DamageSource source(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(DamageTypes.ARROW), null, owner);
	}

	/** Her lock and her torpedoes gone (another hero, death). */
	static void clear(HeroState s) {
		s.junoLock = -1;
		s.junoLockProgress.clear();
		s.junoLocked.clear();
		s.junoFiring.clear();
	}

	public static void clearAll() {
		FLYING.clear();
		HEALS.clear();
	}
}

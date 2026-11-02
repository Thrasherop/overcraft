package dev.overcraft.juno;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static dev.overcraft.juno.JunoStats.*;

/**
 * Juno's Orbital Ray (4A44, 501A on the ray, 54A6 the damage amp): 0.5 s into its cast a ray lands 4 m ahead of her and moves
 * along her facing at up to 2.25 m/s (up to speed over 0.5 s, slowing over its last 1.25 s), on the ground, for 8 s: allies
 * within 8 m of it (any height; her too) healed 85 a second and dealing 30% more, the boost lingering 1 s after they leave.
 */
public final class OrbitalRay {
	private static final class Ray {
		final ServerPlayer juno;
		Vec3 at;
		final Vec3 along;
		final long born;

		Ray(ServerPlayer juno, Vec3 at, Vec3 along, long born) {
			this.juno = juno;
			this.at = at;
			this.along = along;
			this.born = born;
		}
	}

	private static final List<Ray> RAYS = new ArrayList<>();

	private OrbitalRay() {
	}

	/** It lands 4 m ahead of her on the ground, moving the way she faced. */
	static void land(ServerPlayer juno) {
		float yaw = juno.getYRot() * (float) (Math.PI / 180);
		Vec3 along = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 at = ground(juno.level(), juno.position().add(along.scale(RAY_AHEAD)), juno);
		RAYS.add(new Ray(juno, at, along, juno.level().getGameTime()));
		Effects.play(juno.level(), "juno_ray", at, along, juno, Ow.ticks(RAY_TIME), juno);
	}

	public static void tickAll(MinecraftServer server) {
		for (Iterator<Ray> it = RAYS.iterator(); it.hasNext(); ) {
			Ray r = it.next();
			ServerLevel level = r.juno.level();
			double t = (level.getGameTime() - r.born) / 20.0;
			if (t >= RAY_TIME || r.juno.isRemoved()) {
				it.remove();
				continue;
			}
			// its speed: up over its first 0.5 s, down over its last 1.25 s (D 501A, M curve: ours linear)
			double speed = RAY_SPEED * Math.min(1, Math.min(t / RAY_RAMP_UP, (RAY_TIME - t) / RAY_RAMP_DOWN));
			r.at = ground(level, r.at.add(r.along.scale(Ow.speed(speed))), r.juno);
			long now = level.getGameTime();
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(r.at, r.at)
				.inflate(RAY_RADIUS, 200, RAY_RADIUS), e -> e.isAlive() && Allies.isSelfOrAlly(r.juno, e)
				&& Math.hypot(e.getX() - r.at.x, e.getZ() - r.at.z) <= RAY_RADIUS)) {
				float healed = Combat.heal(e, RAY_HEAL / 20);
				if (healed > 0) {
					Combat.credit(r.juno, healed);
					Combat.addFireForHealing(r.juno, healed);
				}
				// +30% damage while inside and for 1 s after (W): Mercy's boost's slot (ours: the two don't add up yet)
				dev.overcraft.mercy.Boosts.boost(e, 1 + RAY_DAMAGE_BOOST, now + Ow.ticks(RAY_LINGER), r.juno.getUUID());
			}
		}
	}

	/** The ground under a point (from 3 m above it down to 20 m below), so the ray follows slopes and steps. */
	private static Vec3 ground(net.minecraft.world.level.Level level, Vec3 at, ServerPlayer juno) {
		var hit = level.clip(new ClipContext(at.add(0, 3, 0), at.add(0, -20, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, juno));
		return hit.getType() == HitResult.Type.MISS ? at : hit.getLocation();
	}

	public static void clearAll() {
		RAYS.clear();
	}
}

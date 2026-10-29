package dev.overcraft.lifeweaver;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.net.GripPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.lifeweaver.LifeweaverStats.*;

/**
 * Life Grip on the ally (his graph 21EE): at once 75 health and a cleanse (BZ 2026-04-14), held still and protected (no
 * damage, no knockback: Combat.isPhased); 0.35 s after the cast, pulled to a point fixed then, just in front of where he
 * stood, at 25 m/s from within 15 m, else 12 m/s for 0.4 s, 12 -> 35 over 0.6 s, then 35. It ends on arrival, at 2.5 s,
 * within 3 m of him for 0.4 s, or on the ally's Jump from 0.65 s (Overwatch's option, on by default). A player's own
 * client moves them (GripPayload: the server can't move a player smoothly), a mob is moved here; the landing never hurts.
 */
public final class LifeGrip {
	private record Pull(LivingEntity ally, ServerPlayer caster, Vec3 to, long startedAt, boolean far) {
	}

	private static final Map<UUID, Pull> PULLS = new HashMap<>();
	/** Allies a pull let go, not yet landed: their fall doesn't count (the owner's rule for ability landings, #35). */
	private static final Map<UUID, LivingEntity> LANDING = new HashMap<>();
	private static final Map<UUID, Integer> CLOSE = new HashMap<>();

	private LifeGrip() {
	}

	public static boolean gripped(LivingEntity entity) {
		return PULLS.containsKey(entity.getUUID());
	}

	/**
	 * Where the pull takes the ally (D 21EF, computed once): from his feet 0.95 m up, then ahead by min(the distance to
	 * the ally, 1.1) m and 0.15 m right, then 0.475 m more up. Airborne, the ground under him (within 9 m: D's safe-ground
	 * search, ours simplified to straight down), else his own point.
	 */
	private static Vec3 destination(ServerPlayer caster, LivingEntity ally) {
		Vec3 feet = caster.position();
		if (!caster.onGround()) {
			var down = caster.level().clip(new ClipContext(feet, feet.add(0, -9, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, caster));
			if (down.getType() == HitResult.Type.BLOCK) {
				feet = down.getLocation();
			}
		}
		Vec3 look = caster.getViewVector(1.0f);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 right = new Vec3(-flat.z, 0, flat.x);
		double ahead = Math.min(ally.distanceTo(caster), GRIP_DEST_AHEAD);
		Vec3 to = feet.add(flat.scale(ahead)).add(right.scale(GRIP_DEST_RIGHT));
		// the ally's feet go there (the data's point is its centre's: 1.425 m up for a hero; ours, a touch above the ground)
		Vec3 up = to.add(0, 0.1, 0);
		return Lifeweaver.clear(caster, caster.getEyePosition(), up.add(0, 1, 0)) ? up : feet.add(0, 0.1, 0);
	}

	public static void start(ServerPlayer caster, LivingEntity ally) {
		Vec3 to = destination(caster, ally);
		boolean far = ally.position().distanceTo(to) > GRIP_NEAR;
		PULLS.put(ally.getUUID(), new Pull(ally, caster, to, caster.level().getGameTime(), far));
		CLOSE.remove(ally.getUUID());
		float healed = Combat.heal(ally, GRIP_HEAL);
		if (healed > 0) {
			Combat.credit(caster, healed);
			Combat.addFireForHealing(caster, healed);
		}
		Combat.cleanse(ally);
		ally.setDeltaMovement(Vec3.ZERO);
		ally.needsSync = true;
		if (ally instanceof ServerPlayer player) {
			ServerPlayNetworking.send(player, new GripPayload(true, to.x, to.y, to.z, far));
		}
		Effects.play(caster.level(), "lifeweaver_grip", to, Vec3.ZERO, ally, 0, caster);
	}

	/** The pull's speed (m/s), this long (ticks) after it started moving the ally. */
	public static double speed(boolean far, int ticks) {
		if (!far) {
			return GRIP_SPEED;
		}
		double t = ticks / 20.0;
		if (t < GRIP_FAR_SLOW) {
			return GRIP_FAR_SPEED;
		}
		return GRIP_FAR_SPEED + (GRIP_FAR_TOP - GRIP_FAR_SPEED) * Math.min(1, (t - GRIP_FAR_SLOW) / GRIP_FAR_RAMP);
	}

	public static void tick(MinecraftServer server) {
		for (Pull p : new ArrayList<>(PULLS.values())) {
			LivingEntity ally = p.ally;
			long t = ally.level().getGameTime() - p.startedAt;
			int moving = (int) (t - Ow.ticks(GRIP_PULL_FROM));
			boolean jumped = ally instanceof ServerPlayer player && t >= Ow.ticks(GRIP_JUMP_CANCEL) && player.getLastClientInput().jump();
			int close = ally.distanceTo(p.caster) <= GRIP_CLOSE && moving > 0 ? CLOSE.merge(ally.getUUID(), 1, Integer::sum) : 0;
			if (close == 0) {
				CLOSE.remove(ally.getUUID());
			}
			boolean arrived = moving > 0 && ally.position().distanceTo(p.to) <= GRIP_ARRIVE;
			if (!ally.isAlive() || ally.isRemoved() || ally.level() != p.caster.level() || moving > Ow.ticks(GRIP_MAX) || jumped
				|| arrived || close >= Ow.ticks(GRIP_CLOSE_TIME)) {
				end(ally);
				continue;
			}
			ally.resetFallDistance();
			if (!(ally instanceof ServerPlayer)) {
				// a mob: moved here, held still until the pull starts
				Vec3 step = Vec3.ZERO;
				if (moving > 0) {
					Vec3 to = p.to.subtract(ally.position());
					double dist = to.length();
					step = dist < 1.0E-4 ? Vec3.ZERO : to.scale(Math.min(Ow.speed(speed(p.far, moving)), dist) / dist);
				}
				if (ally instanceof Mob mob) {
					mob.getNavigation().stop();
				}
				ally.setDeltaMovement(Vec3.ZERO);
				ally.move(MoverType.SELF, step);
				ally.needsSync = true;
			}
		}
		LANDING.values().removeIf(e -> {
			e.resetFallDistance();
			return !e.isAlive() || e.isRemoved() || e.onGround();
		});
	}

	private static void end(LivingEntity ally) {
		PULLS.remove(ally.getUUID());
		CLOSE.remove(ally.getUUID());
		LANDING.put(ally.getUUID(), ally);
		if (ally instanceof ServerPlayer player) {
			ServerPlayNetworking.send(player, GripPayload.OFF);
		}
	}

	public static void clear() {
		PULLS.clear();
		LANDING.clear();
		CLOSE.clear();
	}
}

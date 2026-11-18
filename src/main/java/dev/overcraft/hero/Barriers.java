package dev.overcraft.hero;

import dev.overcraft.Ow;
import dev.overcraft.entity.OwProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Barriers that are entities (sigma-plan.md "What the Tank role needs": a barrier that is an entity, not Brigitte's
 * state-bound rectangle; Sigma's Experimental Barrier first, Reinhardt's, Winston's, Orisa's and Ramattra's later). Each is
 * a {@link Shape}: a flat, upright rectangle with a front (the way it was put up), its owner's. What it stops, from either
 * side, only from its owner's enemies: Overwatch's barriers "only affect the opposing team" (the wiki's Barrier page, rev
 * 406358) and block them both ways: an enemy inside Winston's dome "can still hit Winston or his allies if they are also
 * inside", so "move outside of it so that the enemy has to exit the bubble to attack you" (Winston, rev 408465); Sigma's
 * own page has players "dance around your Experimental Barrier to block his shots" (rev 408635). Until the 2026-11-02
 * release play ours blocked from its front only (an enemy on its back side shot Sigma through it):
 * <ul>
 * <li>projectiles crossing it ({@link #intercept}: arrows, tridents, fireballs, potions, shulker bullets, every hero
 * projectile), each charging it with the damage it would have dealt ({@link #projectileDamage});</li>
 * <li>blasts on either side of it, as a block in the way would (ServerExplosionMixin: {@link #blast}, {@link #blocksBlast});</li>
 * <li>a hero's hit without a projectile (a hitscan shot) from more than a melee's reach ({@link #absorb}).</li>
 * </ul>
 * Melee and creatures pass (Overwatch's barriers don't block movement or melee but Reinhardt's hammer). Brigitte's
 * barrier (dev.overcraft.brigitte.Barrier) asks this too, so both go through the same LivingEntityMixin /
 * ServerExplosionMixin paths.
 */
public final class Barriers {
	/** A barrier as a shape in the world. */
	public interface Shape {
		@Nullable LivingEntity barrierOwner();

		Level barrierLevel();

		/** Its centre (half its height up from its foot). */
		Vec3 barrierCentre();

		/** The way its front faces (horizontal, unit). */
		Vec3 barrierFacing();

		double barrierWidth();

		double barrierHeight();

		/** Takes {@code ow} damage (Overwatch points) at {@code at}; returns what it took (it may break on it). */
		double barrierTake(double ow, Vec3 at);

		boolean barrierAlive();
	}

	private static final Set<Shape> LIVE = Collections.newSetFromMap(new java.util.IdentityHashMap<>());

	/** Hits being dealt inside {@link #pierce} (more than 0). */
	private static int pierceDepth;

	/**
	 * Damage that goes through every barrier and never hurts one: Gravitic Flux's lift and slam, and any hit dealt inside
	 * {@link #pierce} (a damage over time already on its target, a field that pierces barriers).
	 */
	public static boolean piercing(DamageSource source) {
		return pierceDepth > 0 || source.is(dev.overcraft.sigma.GraviticFlux.LIFT_TYPE) || source.is(dev.overcraft.sigma.GraviticFlux.SLAM_TYPE);
	}

	/**
	 * Deals {@code hit} as a hit no barrier stops (#104's sweep, 2026-11-13). For what was already applied when it ticks (Silver
	 * Bullet's bleed, Local Anesthetic's numbing: a barrier blocks the shot or burst, not its damage over time once it's on
	 * someone; ours, as no source lists a damage over time a barrier stops) and for what the wiki says pierces barriers
	 * (Disruptor Shot's field: "the AoE pierces barriers", the Sojourn page rev 409400). Their hitscan-type sources would
	 * otherwise be judged from the shooter's eyes ({@link #absorb}) and stopped by any barrier between.
	 */
	public static <T> T pierce(java.util.function.Supplier<T> hit) {
		pierceDepth++;
		try {
			return hit.get();
		} finally {
			pierceDepth--;
		}
	}

	private Barriers() {
	}

	public static void add(Shape shape) {
		LIVE.add(shape);
	}

	public static void remove(Shape shape) {
		LIVE.remove(shape);
	}

	private static List<Shape> in(Level level) {
		if (LIVE.isEmpty()) {
			return List.of();
		}
		LIVE.removeIf(s -> !s.barrierAlive());
		List<Shape> out = new ArrayList<>();
		for (Shape s : LIVE) {
			if (s.barrierLevel() == level) {
				out.add(s);
			}
		}
		return out;
	}

	/** Whether {@code shape} stops what {@code attacker} sends (its owner's enemies, or nobody's: a dispenser's arrow). */
	public static boolean stops(Shape shape, @Nullable Entity attacker) {
		LivingEntity owner = shape.barrierOwner();
		return owner != null && (attacker == null || attacker != owner && !Allies.isSelfOrAlly(owner, attacker));
	}

	/** Where the way from {@code a} to {@code b} crosses {@code shape}, from either side, or null. */
	public static @Nullable Vec3 crossing(Shape shape, Vec3 a, Vec3 b) {
		Vec3 c = shape.barrierCentre(), f = shape.barrierFacing();
		double da = a.subtract(c).dot(f), db = b.subtract(c).dot(f);
		// from one side to the other (or onto it): the signs differ
		if (da == db || da > 0 && db > 0 || da < 0 && db < 0) {
			return null;
		}
		Vec3 at = a.add(b.subtract(a).scale(da / (da - db)));
		Vec3 side = new Vec3(f.z, 0, -f.x);
		return Math.abs(at.subtract(c).dot(side)) <= shape.barrierWidth() / 2 && Math.abs(at.y - c.y) <= shape.barrierHeight() / 2 ? at : null;
	}

	/** The first barrier of {@code attacker}'s enemies the way from {@code a} to {@code b} crosses. */
	public static @Nullable Shape between(Level level, Vec3 a, Vec3 b, @Nullable Entity attacker) {
		for (Shape s : in(level)) {
			if (stops(s, attacker) && crossing(s, a, b) != null) {
				return s;
			}
		}
		return null;
	}

	// ---- projectiles ----

	/**
	 * The damage a projectile would deal (Overwatch points; Minecraft's own numbers): an arrow its speed times its base
	 * damage, rounded up (AbstractArrow.onHitEntity); a blaze's fireball 5, a ghast's 6 and its blast's middle (power p:
	 * 7 x 2p + 1 = 15 at 1), a wither skull 8 and its blast's, a shulker bullet 4, a trident 8, llama spit 1 (their
	 * onHitEntity / onHit); a hero projectile its own ({@link OwProjectile#barrierDamage}); anything else 0 (potions,
	 * snowballs, eggs, pearls, wind charges: ours, sigma-plan.md "Grasp against non-damage projectiles").
	 */
	public static double projectileDamage(Projectile p) {
		double mc;
		if (p instanceof OwProjectile ow) {
			return ow.barrierDamage();
		} else if (p instanceof ThrownTrident) {
			mc = 8;
		} else if (p instanceof AbstractArrow arrow) {
			mc = Math.ceil(arrow.getDeltaMovement().length() * ((dev.overcraft.mixin.AbstractArrowAccessor) arrow).overcraft$baseDamage());
		} else if (p instanceof SmallFireball) {
			mc = 5;
		} else if (p instanceof LargeFireball big) {
			mc = 6 + blastMiddle(((dev.overcraft.mixin.LargeFireballAccessor) big).overcraft$explosionPower());
		} else if (p instanceof WitherSkull) {
			mc = 8 + blastMiddle(1);
		} else if (p instanceof ShulkerBullet) {
			mc = 4;
		} else if (p instanceof LlamaSpit) {
			mc = 1;
		} else {
			mc = 0;
		}
		return Ow.toOw(mc);
	}

	/** Minecraft's blast damage at its centre for a power (ServerExplosion.hurtEntities, full exposure): 7 x 2p + 1. */
	private static double blastMiddle(float power) {
		return 7 * 2 * power + 1;
	}

	/** Whether a barrier or Kinetic Grasp takes this projectile at all (not a fishing line, a firework's blast, or one at rest). */
	public static boolean takes(Projectile p) {
		return p.isAlive() && !(p instanceof FishingHook) && !(p instanceof FireworkRocketEntity) && p.getDeltaMovement().lengthSqr() > 1.0E-4;
	}

	/**
	 * A box ahead of a hero's eyes along the aim that eats enemy projectiles (Sigma's Kinetic Grasp, D.Va's Defense Matrix):
	 * {@code side} m either side of the aim, {@code down} below to {@code up} above it, from {@code near} to {@code far} ahead.
	 */
	public record Box(double side, double down, double up, double near, double far) {
		/** Whether {@code rel} (from the eyes) is in it, in the aim's frame (forward f, right r, up u). */
		public boolean contains(Vec3 rel, Vec3 f, Vec3 r, Vec3 u) {
			double x = rel.dot(r), y = rel.dot(u), z = rel.dot(f);
			return Math.abs(x) <= side && y >= -down && y <= up && z >= near && z <= far;
		}
	}

	/** What a box does with each projectile it eats (where, and the damage it would have dealt: {@link #projectileDamage}). */
	public interface Eater {
		void eaten(Projectile p, Vec3 at, double damage);
	}

	/**
	 * One tick of a projectile-eating box (dev.overcraft.dva.DefenseMatrix; Kinetic Grasp's is the same loop, its own until
	 * Sigma's step 10 lands): every enemy projectile whose last or next step passes through it (sampled: an arrow covers 3 m
	 * a tick) is stopped there ({@link #stop}, eaten whole) after {@code onEaten}. Returns what it ate (Overwatch points).
	 */
	public static double absorbBox(net.minecraft.server.level.ServerPlayer player, Box box, Eater onEaten) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 f = player.getViewVector(1.0f);
		Vec3 r = f.cross(new Vec3(0, 1, 0));
		r = r.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : r.normalize();
		Vec3 u = r.cross(f).normalize();
		double total = 0;
		double reach = Math.max(box.far(), Math.max(box.side(), Math.max(box.up(), box.down()))) + 6;
		for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(eye, eye).inflate(reach), Barriers::takes)) {
			Entity owner = p.getOwner();
			if (owner == player || owner != null && Allies.isSelfOrAlly(player, owner)) {
				continue;
			}
			Vec3 now = p.position(), before = new Vec3(p.xo, p.yo, p.zo), next = now.add(p.getDeltaMovement());
			Vec3 in = null;
			for (int i = 0; i <= 8 && in == null; i++) {
				Vec3 at = i <= 4 ? before.lerp(now, i / 4.0) : now.lerp(next, (i - 4) / 4.0);
				if (box.contains(at.subtract(eye), f, r, u)) {
					in = at;
				}
			}
			if (in == null) {
				continue;
			}
			double damage = projectileDamage(p);
			total += damage;
			onEaten.eaten(p, in, damage);
			stop(p, in, false);
		}
		return total;
	}

	/**
	 * Each tick of an entity barrier (its own tick): the projectiles whose last or next step crosses it (either side) are
	 * stopped there, on the side they came from, and it takes their damage.
	 */
	public static void intercept(Shape shape) {
		if (!(shape.barrierLevel() instanceof ServerLevel level)) {
			return;
		}
		Vec3 c = shape.barrierCentre();
		double reach = Math.max(shape.barrierWidth(), shape.barrierHeight()) / 2 + 8;
		for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(reach), Barriers::takes)) {
			if (!stops(shape, p.getOwner()) || p instanceof OwProjectile ow && ow.passesBarriers()) {
				continue;
			}
			Vec3 now = p.position(), before = new Vec3(p.xo, p.yo, p.zo);
			Vec3 at = crossing(shape, before, now);
			if (at == null) {
				at = crossing(shape, now, now.add(p.getDeltaMovement()));
				if (at != null && !reaches(p, now, at)) {
					continue;
				}
			}
			if (at == null) {
				continue;
			}
			double damage = p instanceof LargeFireball || p instanceof WitherSkull ? 0 : projectileDamage(p);
			if (damage > 0) {
				shape.barrierTake(damage, at);
			}
			double from = before.subtract(shape.barrierCentre()).dot(shape.barrierFacing());
			Vec3 normal = shape.barrierFacing().scale(from < 0 ? -1 : 1);
			if (p instanceof OwProjectile ow) {
				// a hero projectile does its own (bursts, bounces: Petal Platform), on the side it came from
				ow.hitBarrier(at.add(normal.scale(0.3)), normal);
				continue;
			}
			stop(p, at.add(normal.scale(0.3)), true);
		}
	}

	/**
	 * Whether {@code p}, on its next step from {@code now}, gets as far as {@code at} (a barrier it's about to cross): not if a
	 * block, or a creature it would hit, comes first (the critique of #104: looking a step ahead stopped a shot before its own tick
	 * reached the Brigitte it was fired at, from behind; a zombie pressed to a barrier's front was out of reach the same way).
	 */
	public static boolean reaches(Projectile p, Vec3 now, Vec3 at) {
		if (p.level().clip(new net.minecraft.world.level.ClipContext(now, at, net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, p)).getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
			return false;
		}
		double r = p instanceof OwProjectile ow ? ow.hitRadius() : 0.3;
		for (Entity e : p.level().getEntities(p, new AABB(now, at).inflate(r + 1))) {
			if (((dev.overcraft.mixin.ProjectileInvoker) p).overcraft$canHitEntity(e) && e.getBoundingBox().inflate(r).clip(now, at).isPresent()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * A projectile stopped at {@code at} (a barrier's front, or inside Kinetic Grasp): a ghast's fireball or a wither skull
	 * bursts there at a barrier (its own blast, which the barrier then takes: {@link #blast}), eaten whole by Grasp; a
	 * player's arrow or trident drops there (picked up again, as one off a shield); a hero projectile does its own
	 * ({@link OwProjectile#hitBarrier}); anything else is gone.
	 */
	public static void stop(Projectile p, Vec3 at, boolean barrier) {
		if (barrier && p instanceof OwProjectile ow) {
			ow.hitBarrier(at);
			return;
		}
		if (barrier && (p instanceof LargeFireball || p instanceof WitherSkull)) {
			float power = p instanceof LargeFireball big ? ((dev.overcraft.mixin.LargeFireballAccessor) big).overcraft$explosionPower() : 1;
			boolean grief = p.level() instanceof ServerLevel level && level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
			p.discard();
			p.level().explode(p, at.x, at.y, at.z, power, p instanceof LargeFireball && grief, Level.ExplosionInteraction.MOB);
			return;
		}
		if ((p instanceof ThrownTrident || p instanceof AbstractArrow arrow && arrow.pickup == AbstractArrow.Pickup.ALLOWED)
			&& p.getOwner() instanceof Player) {
			p.setPos(at);
			p.setDeltaMovement(p.getDeltaMovement().scale(-0.05));
			p.needsSync = true;
			return;
		}
		p.discard();
	}

	// ---- blasts ----

	/**
	 * A Minecraft blast (ServerExplosionMixin, before it hurts anyone): each enemy barrier near the blast's centre (either
	 * side) takes what an entity at its nearest point would (ServerExplosion.hurtEntities' rule with full exposure: impact
	 * (1 - d / 2r), (impact² + impact) / 2 x 7 x 2r + 1).
	 */
	public static void blast(Level level, Vec3 centre, float radius, @Nullable Entity cause, boolean hurts) {
		if (!hurts) {
			// a breeze's wind charge hurts no one: its barrier takes nothing either (Brigitte's rule, #88); what it pushes
			// behind a barrier is stopped by where they stand (blocksBlast)
			return;
		}
		double reach = radius * 2;
		for (Shape s : in(level)) {
			if (!stops(s, cause)) {
				continue;
			}
			Vec3 nearest = nearest(s, centre);
			double d = nearest == null ? reach : nearest.distanceTo(centre);
			if (d >= reach) {
				continue;
			}
			double impact = 1 - d / reach;
			double mc = (impact * impact + impact) / 2 * 7 * reach + 1;
			s.barrierTake(Ow.toOw(mc), nearest);
		}
	}

	/** The point of {@code shape} nearest {@code at} (on either side of it). */
	public static Vec3 nearest(Shape shape, Vec3 at) {
		Vec3 c = shape.barrierCentre(), f = shape.barrierFacing();
		Vec3 side = new Vec3(f.z, 0, -f.x);
		double across = Math.clamp(at.subtract(c).dot(side), -shape.barrierWidth() / 2, shape.barrierWidth() / 2);
		double up = Math.clamp(at.y - c.y, -shape.barrierHeight() / 2, shape.barrierHeight() / 2);
		return c.add(side.scale(across)).add(0, up, 0);
	}

	/** The enemy barriers of {@code attacker} in {@code level} (a hero's own burst: Hyperspheres). */
	public static List<Shape> enemyBarriers(Level level, @Nullable Entity attacker) {
		List<Shape> out = new ArrayList<>();
		for (Shape s : in(level)) {
			if (stops(s, attacker)) {
				out.add(s);
			}
		}
		return out;
	}

	/** Whether an enemy barrier stands between a blast's centre and {@code entity} (no push and no damage behind it). */
	public static boolean blocksBlast(Entity entity, Vec3 centre, @Nullable Entity cause) {
		Vec3 to = entity.getBoundingBox().getCenter();
		for (Shape s : in(entity.level())) {
			LivingEntity owner = s.barrierOwner();
			if (owner != null && stops(s, cause) && crossing(s, centre, to) != null) {
				return true;
			}
		}
		return false;
	}

	// ---- hits ----

	/**
	 * Every hit passes here first (Brigitte's Barrier.absorb, from LivingEntityMixin): what's left of {@code damage}
	 * (Minecraft health) after the entity barriers between it and {@code victim}. A projectile whose last step crossed one
	 * (a point-blank shot the barrier's own tick hadn't seen) is stopped whole; a blast's damage behind one is gone (the
	 * barrier took the blast itself: {@link #blast}); a shot without a projectile crossing one is taken by it, whole.
	 */
	public static float absorb(LivingEntity victim, DamageSource source, float damage) {
		if (damage <= 0 || !(victim.level() instanceof ServerLevel level)) {
			return damage;
		}
		// melee goes through every barrier (Brigitte's marked blows: dev.overcraft.brigitte.Barrier.isMelee; the owner,
		// 2026-11-02: "Melee goes through"), and so does Gravitic Flux (the wiki's Barrier page: it "ignores barriers but
		// cannot damage them")
		if (dev.overcraft.brigitte.Barrier.isMelee(source) || piercing(source)) {
			return damage;
		}
		Entity direct = source.getDirectEntity(), attacker = source.getEntity();
		Vec3 to = victim.getBoundingBox().getCenter();
		if (attacker != null && attacker != victim && direct == null && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
			&& dev.overcraft.sigma.KineticGrasp.absorbsShot(victim, source, damage)) {
			// a hitscan shot (no direct entity: Combat.hitscan, Juno's rounds) into an enemy Sigma's Kinetic Grasp; an area
			// ability's hit (Soundwave, Purr: the hero is its direct entity) isn't one ("AoE attacks" pass Grasp: the wiki)
			return 0;
		}
		for (Shape s : in(level)) {
			if (!stops(s, attacker)) {
				continue;
			}
			if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && source.getSourcePosition() != null) {
				// a blast (Minecraft's, a hero's burst): its damage behind the barrier is gone, the barrier took the blast
				if (crossing(s, source.getSourcePosition(), to) != null) {
					return 0;
				}
				continue;
			}
			if (direct instanceof Projectile p) {
				if (p instanceof OwProjectile ow && ow.passesBarriers()) {
					continue;
				}
				if (crossing(s, new Vec3(p.xo, p.yo, p.zo), p.position()) != null) {
					s.barrierTake(projectileDamage(p), p.position());
					return 0;
				}
				continue;
			}
			if (attacker != null && attacker != victim && (direct == null || direct == attacker)) {
				// a shot with no projectile (hitscan: Sombra's pistol, Juno's Mediblaster, Ana's scoped shot, the Charged Shot
				// build their source with no direct entity; the critique's #1) or a hero's own hit, judged from the
				// shooter's eyes; the whole hit stops at it, even one bigger than it has left (it breaks: Brigitte's rule,
				// #88, the wiki's "barriers block the explosion")
				Vec3 at = crossing(s, attacker.getEyePosition(), to);
				if (at != null) {
					s.barrierTake(Ow.toOw(damage), at);
					return 0;
				}
			}
		}
		return damage;
	}
}

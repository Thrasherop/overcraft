package dev.overcraft.sojourn;

import dev.overcraft.Ow;
import dev.overcraft.entity.DisruptorShotEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.Perk;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static dev.overcraft.sojourn.SojournStats.*;

/**
 * Sojourn's Disruptor Shot (her graph 1441): 0.17 s after the press a projectile leaves along her aim (DisruptorShotEntity) and
 * opens a field (23E6, graph 1443) on contact or after 0.75 s: 4 m round, hurting every enemy inside 90 a second for 3.5 s from
 * 0.1 s after it opens, through armor; with Deceleration Field they're 25 % slower while inside. Her cooldown is written 0.5 s
 * after the launch (Sojourn.tick).
 */
public final class Disruptor {
	private static final Identifier SLOW = dev.overcraft.Overcraft.id("sojourn_deceleration_field");

	private static final class Field {
		final ServerPlayer owner;
		final ServerLevel level;
		final Vec3 centre;
		final long born;
		final boolean slows;
		/** Steps of 0.192 s owed (ticks / 3.84). */
		double clock;

		Field(ServerPlayer owner, ServerLevel level, Vec3 centre, long born, boolean slows) {
			this.owner = owner;
			this.level = level;
			this.centre = centre;
			this.born = born;
			this.slows = slows;
		}
	}

	private static final List<Field> FIELDS = new ArrayList<>();
	/** Each creature a field slows: until when (game time; the next tick it's still inside renews it). */
	private static final Map<LivingEntity, Long> SLOWED = new HashMap<>();

	private Disruptor() {
	}

	/** The launch, at the cast's end: from her eyes along her aim (D: her left wrist hp 0002; ours, so the crosshair is where it goes). */
	static void launch(ServerPlayer player) {
		DisruptorShotEntity shot = new DisruptorShotEntity(player, player.level());
		shot.launch(player.getEyePosition(), player.getViewVector(1.0f), Ow.speed(DISRUPTOR_SPEED));
		player.level().addFreshEntity(shot);
	}

	/** The projectile stopped (DisruptorShotEntity): its field there. */
	public static void open(ServerPlayer owner, Vec3 at) {
		ServerLevel level = owner.level();
		FIELDS.add(new Field(owner, level, at, level.getGameTime(), HeroManager.hasPerk(owner, Perk.SOJOURN_DECELERATION_FIELD)));
		// its stand-in look for its whole life (SojournFx: her real effects are step 8)
		Effects.play(level, "sojourn_disruptor_field", at, Vec3.ZERO, owner, Ow.ticks(FIELD_DELAY + FIELD_TIME), owner);
	}

	public static void tickAll(MinecraftServer server) {
		for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
			Field f = it.next();
			long age = f.level.getGameTime() - f.born;
			int delay = Ow.ticks(FIELD_DELAY);
			if (age >= delay + Ow.ticks(FIELD_TIME) || f.owner.isRemoved()) {
				it.remove();
				continue;
			}
			if (age < delay) {
				continue;
			}
			// the 90 a second in the data's steps: 17.28 every 0.192 s (3.84 ticks), the first a step after it starts
			f.clock += 1 / (FIELD_STEP * 20);
			boolean step = f.clock >= 1;
			if (step) {
				f.clock -= 1;
			}
			if (!step && !f.slows) {
				continue;
			}
			DamageSource source = fieldSource(f.owner);
			for (LivingEntity e : f.level.getEntitiesOfClass(LivingEntity.class, new AABB(f.centre, f.centre).inflate(FIELD_RADIUS),
				e -> e.isAlive() && e != f.owner && Allies.canHurt(f.owner, e) && inside(e, f.centre))) {
				if (step) {
					float dealt = Combat.damage(e, source, FIELD_DPS * FIELD_STEP);
					Combat.credit(f.owner, dealt);
					Combat.addFireForDamage(f.owner, dealt, Ow.toOw(e.getMaxHealth()));
				}
				if (f.slows) {
					slow(e, f.level.getGameTime());
				}
			}
		}
		SLOWED.entrySet().removeIf(s -> {
			if (s.getKey().isRemoved() || !s.getKey().isAlive() || s.getKey().level().getGameTime() > s.getValue()) {
				AttributeInstance speed = s.getKey().getAttribute(Attributes.MOVEMENT_SPEED);
				if (speed != null) {
					speed.removeModifier(SLOW);
				}
				return true;
			}
			return false;
		});
	}

	/** Inside its 4 m: the nearest point of the creature's box (Overwatch's capsule against the sphere: ours, the box). */
	private static boolean inside(LivingEntity e, Vec3 centre) {
		AABB box = e.getBoundingBox();
		double x = Math.clamp(centre.x, box.minX, box.maxX), y = Math.clamp(centre.y, box.minY, box.maxY),
			z = Math.clamp(centre.z, box.minZ, box.maxZ);
		return centre.distanceToSqr(x, y, z) <= FIELD_RADIUS * FIELD_RADIUS;
	}

	/** Deceleration Field: 25 % slower while inside, renewed each tick there, gone the tick after leaving. */
	private static void slow(LivingEntity e, long now) {
		AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		if (!speed.hasModifier(SLOW)) {
			speed.addTransientModifier(new AttributeModifier(SLOW, -DECELERATION_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		SLOWED.put(e, now);
	}

	/** The field's damage: from her, through armor and pushing no one (magic, as Sombra's zone). */
	private static DamageSource fieldSource(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(DamageTypes.MAGIC), null, owner);
	}

	/** Slowed by a field now (tests, the harness). */
	public static boolean slowed(LivingEntity e) {
		return SLOWED.containsKey(e);
	}

	public static void clearAll() {
		FIELDS.clear();
		for (LivingEntity e : SLOWED.keySet()) {
			AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.removeModifier(SLOW);
			}
		}
		SLOWED.clear();
	}
}

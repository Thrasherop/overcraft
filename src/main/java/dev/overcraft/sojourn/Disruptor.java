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
		boolean slows = HeroManager.hasPerk(owner, Perk.SOJOURN_DECELERATION_FIELD);
		FIELDS.add(new Field(owner, level, at, level.getGameTime(), slows));
		// its look for its whole life (SojournFx: 2228, 6C02 with Deceleration Field), its loops and end at it, the burn on
		// whoever is inside (dev.overcraft.client.SojournSounds); the variant carries its ticks and the perk
		Effects.play(level, "sojourn_disruptor_field", at, Vec3.ZERO, owner, Ow.ticks(FIELD_DELAY + FIELD_TIME) | (slows ? FX_SLOWS : 0),
			owner);
		// 2228's one-shots: 9210 hers (aud 1, 70 m: at the field), 5402 for others (70 m)
		SojournSounds.forOne(owner, "disruptor.field.open", at, 70);
		SojournSounds.at(level, "disruptor.field.open_3p", at, 70, owner);
	}

	/** The field effect's variant flag over its ticks: Deceleration Field (6C02's sphere). */
	public static final int FX_SLOWS = 1 << 12;

	/**
	 * The shot hit a creature (076A type 0: 5197's 5D51 in her head, 9050 500 m at it for others) or a block (type 2: 519A,
	 * 892E 90 m + 8930 32 m, one copy for everyone).
	 */
	public static void struck(ServerPlayer owner, Vec3 at, boolean block) {
		if (block) {
			SojournSounds.at(owner.level(), "disruptor.hit_block", at, 90);
		} else {
			SojournSounds.own(owner, "disruptor.hit");
			SojournSounds.at(owner.level(), "disruptor.hit_3p", at, 500, owner);
		}
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
				e -> e.isAlive() && e != f.owner && Allies.areaHits(f.owner, e) && inside(e, f.centre)
					&& !dev.overcraft.brigitte.Barrier.shields(e, f.owner, f.centre))) {
				if (step) {
					float dealt = Combat.damage(e, source, FIELD_DPS * FIELD_STEP, dev.overcraft.hero.Blow.ability("disruptor_shot").by(dev.overcraft.hero.Hero.SOJOURN));
					Combat.credit(f.owner, dealt, e);
					Combat.addFireForDamage(f.owner, dealt, e);
				}
				if (f.slows && !Combat.phaseStops(e)) {
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

	/** Protection Suzu's cleanse (Combat.cleanse's table): the slow stops now; still inside once the phase ends, it comes back. */
	public static void cleanse(LivingEntity target) {
		if (SLOWED.remove(target) != null) {
			AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.removeModifier(SLOW);
			}
		}
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

	/**
	 * The field's damage: from her, through armor, pushing no one and not jolting a player's movement (our own type,
	 * overcraft:field, in the bypasses_armor, no_knockback and no_impact tags). Magic jolted it: a player hurt is sent the
	 * server's view of their motion, and one walking through an enemy field slowed to 3.5 m/s on every damage step (#82).
	 */
	private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> FIELD =
		net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, dev.overcraft.Overcraft.id("field"));

	private static DamageSource fieldSource(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(FIELD), null, owner);
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

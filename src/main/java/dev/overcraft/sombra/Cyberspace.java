package dev.overcraft.sombra;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.sombra.SombraStats.*;

/**
 * Cyberspace's zone (graph 6102): 5 m around where it opened, for 3 s. Enemies inside take 20 as it opens (BZ 2026-10-13)
 * and 25 a second, and are weakened: they deal 50% less damage while inside and 0.25 s after leaving (D; healing dealt too,
 * which the mod's heals don't carry a healer for yet: Open). Allies inside are healed 40 as it opens and 30 a second
 * (Cybersecurity: x2 below half health), the effect lasting 0.75 s after leaving. Inside: within 5 m of its centre (a
 * sphere, M).
 */
public final class Cyberspace {
	private record Zone(ServerLevel level, ServerPlayer sombra, Vec3 at, long opened, long until) {
	}

	private static final List<Zone> ZONES = new ArrayList<>();
	/** Who is weakened until when (game time). */
	private static final Map<UUID, Long> WEAKENED = new HashMap<>();

	private Cyberspace() {
	}

	public static void open(ServerLevel level, ServerPlayer sombra, Vec3 at) {
		long now = level.getGameTime();
		Zone zone = new Zone(level, sombra, at, now, now + Ow.ticks(ZONE_TIME));
		ZONES.add(zone);
		Effects.play(level, "sombra_cyberspace", at, Vec3.ZERO, null, Ow.ticks(ZONE_TIME), sombra);
		apply(zone, ZONE_DAMAGE_NOW, ZONE_HEAL_NOW);
	}

	/** One pass over who's inside: this much damage to enemies, this much healing to allies, and the weaken. */
	private static void apply(Zone z, double damage, double heal) {
		long now = z.level.getGameTime();
		for (LivingEntity e : z.level.getEntitiesOfClass(LivingEntity.class, new AABB(z.at, z.at).inflate(ZONE_RADIUS + 1))) {
			if (!e.isAlive() || e.getBoundingBox().distanceToSqr(z.at) > ZONE_RADIUS * ZONE_RADIUS) {
				continue;
			}
			if (Allies.isSelfOrAlly(z.sombra, e)) {
				if (heal > 0) {
					boolean low = e.getHealth() < e.getMaxHealth() * CYBERSECURITY_BELOW
						&& dev.overcraft.hero.HeroManager.hasPerk(z.sombra, dev.overcraft.hero.Perk.SOMBRA_CYBERSECURITY);
					float healed = Combat.heal(e, heal * (low ? CYBERSECURITY : 1));
					if (healed > 0) {
						Combat.credit(z.sombra, healed);
						Combat.addFireForHealing(z.sombra, healed);
					}
				}
			} else if (Allies.canHurt(z.sombra, e)) {
				WEAKENED.merge(e.getUUID(), now + Ow.ticks(ZONE_LINGER_ENEMY), Math::max);
				if ((now - z.opened) % 10 == 0) {
					// its look on them (7AE1), refreshed every half second while they're inside
					Effects.play(z.level, "sombra_weakened", e.position(), Vec3.ZERO, e, 10 + Ow.ticks(ZONE_LINGER_ENEMY), z.sombra);
				}
				if (damage > 0) {
					float dealt = Combat.damage(e, zoneSource(z.sombra), damage);
					Combat.credit(z.sombra, dealt);
					Combat.addFireForDamage(z.sombra, dealt, Ow.toOw(e.getMaxHealth()));
					if (dealt > 0) {
						Effects.hitMarker(z.sombra, e, false);
					}
				}
			}
		}
	}

	/**
	 * The zone's damage: from her, of a type that pushes no one (magic: in Minecraft's no_knockback tag). With the pistol's
	 * arrow type every one of its 20 ticks a second knocked the enemy away from her, and a zombie inside never reached her
	 * (found playing).
	 */
	static net.minecraft.world.damagesource.DamageSource zoneSource(ServerPlayer sombra) {
		return new net.minecraft.world.damagesource.DamageSource(sombra.level().registryAccess()
			.lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC), null, sombra);
	}

	public static void tick(MinecraftServer server) {
		ZONES.removeIf(z -> {
			if (z.level.getGameTime() >= z.until || z.sombra.isRemoved()) {
				return true;
			}
			apply(z, ZONE_DAMAGE_RATE / 20, ZONE_HEAL_RATE / 20);
			return false;
		});
		WEAKENED.values().removeIf(until -> server.overworld().getGameTime() >= until);
	}

	/** The factor on the damage this attacker deals now: halved while weakened. */
	public static double dealtFactor(@Nullable Entity attacker) {
		if (attacker == null) {
			return 1;
		}
		Long until = WEAKENED.get(attacker.getUUID());
		return until != null && attacker.level().getGameTime() < until ? 1 - WEAKEN : 1;
	}

	public static void clear() {
		ZONES.clear();
		WEAKENED.clear();
	}
}

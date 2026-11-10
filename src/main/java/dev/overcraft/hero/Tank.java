package dev.overcraft.hero;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The Tank role and its sub-roles, for every tank (Sigma the first, #68; D.Va next). Overwatch keeps them in the shared role
 * graph 01C7, keyed on the hero category 0850 (Tank) and the sub-role tags (116B Stalwart): tools/reference/sigma.md "The
 * Tank role" and "Sub-role: Stalwart", sigma-plan.md "What the Tank role needs".
 * <ul>
 * <li>+150 health, always (Overwatch's Role Queue bonus; Overcraft has no queue): added by {@link Hero}'s constructor.</li>
 * <li>Enemies gain 40% less ult charge (and perk progress) from what they do to a tank: {@link Combat#credit(Player, double,
 * Entity)}.</li>
 * <li>Stalwart: 40% less knockback (LivingEntityMixin, ServerExplosionMixin, the heroes' own pushes) and 40% less of a slow
 * received from others (OwMovement.speedFactor), in hero mode.</li>
 * <li>Initiator: a movement ability's start heals 50 over 1 s, at most every 5 s, not at full health ({@link #initiatorReady}).</li>
 * <li>The healing reduction a hit starts is halved on a tank, 15% for the global 30% (D 01C7 #1308): {@link Combat#heal}.</li>
 * </ul>
 */
public final class Tank {
	private Tank() {
	}

	/** Every tank's bonus health (D 01C7 #1106 v70468 = 1CA9 150; BZ 2026-02-10 "Bonus Health +150"; W Roles) H. */
	public static final int BONUS_HEALTH = 150;
	/**
	 * Enemies' ult charge from damage and healing on a tank (D 01C7 #1066 v46219 = 1CAA 0.6; 1CAB 0.8 under the mode flag
	 * v66788, M: 6v6, not ours; BZ 2024-07-09 30 -> 40 %, 2026-02-10; W "40% less") H. Perk progress the same (W Roles) M.
	 */
	public static final double ENEMY_ULT_FACTOR = 0.6;
	/** Stalwart's knockback resistance (D 01C7 v69402 = 23C7 0.4, read by #969 as the knockback scale x(1 - 0.4); BZ 2026-02-10; W Sub-Roles) H. */
	public static final double STALWART_KNOCKBACK = 0.4;
	/** Stalwart's slow resistance (D 01C7 #2866: a slow v23186 x (1 - 23C9 0.4); BZ 2026-02-10; W Sub-Roles) H. */
	public static final double STALWART_SLOW = 0.4;

	/**
	 * Speed modifiers a hero puts on themselves (their own ability's slow, not one received): a Stalwart resists none of
	 * them. Each hero's kit adds its own (Sigma's Accretion cast, later).
	 */
	private static final Set<Identifier> OWN_SLOWS = new HashSet<>(Set.of(
		// Sigma's Accretion gather (-75%: dev.overcraft.sigma.Accretion.CAST_SLOW; here, so his own client knows it too)
		Identifier.fromNamespaceAndPath("overcraft", "sigma_accretion_cast")));

	public static boolean tank(@Nullable Hero hero) {
		return hero != null && hero.role == Hero.Role.TANK;
	}

	public static boolean stalwart(@Nullable Hero hero) {
		return hero != null && hero.subRole.equals("stalwart");
	}

	/** Initiator (D.Va the first, D 01C7 tag 116D): a movement ability's start heals (D.Va's Boosters: dev.overcraft.dva.Dva.initiator). */
	public static boolean initiator(@Nullable Hero hero) {
		return hero != null && hero.subRole.equals("initiator");
	}

	/**
	 * Whether Initiator's heal may land now: its 5 s since the last are over (D 01C7 #2955 / #2889) and the tank isn't at full
	 * health (#2857; BZ 2026-03-10 "not at full health").
	 */
	public static boolean initiatorReady(long now, long readyAt, float health, float maxHealth) {
		return now >= readyAt && health < maxHealth;
	}

	/** A speed modifier that is the hero's own (not resisted). */
	public static void ownSlow(Identifier modifier) {
		OWN_SLOWS.add(modifier);
	}

	// ---- the ult charge ----

	/** The share of the charge earned on a target: x0.6 when it's a tank and the earner is not on its side. */
	public static double ultFactor(boolean targetIsTank, boolean earnerIsAlly) {
		return targetIsTank && !earnerIsAlly ? ENEMY_ULT_FACTOR : 1;
	}

	/** The same for these two: the target a player in hero mode as a tank, the earner neither the target nor its ally. */
	public static double ultFactor(Player earner, @Nullable Entity target) {
		if (!(target instanceof Player tank) || tank == earner || !HeroManager.controls(tank) || !tank(HeroManager.hero(tank))) {
			return 1;
		}
		return ultFactor(true, Allies.isAlly(earner, tank));
	}

	// ---- Stalwart ----

	/** The share of a knockback a Stalwart keeps (0.6), anyone else all of it. */
	public static double knockbackFactor(boolean stalwart) {
		return stalwart ? 1 - STALWART_KNOCKBACK : 1;
	}

	/** The share of a knockback this entity keeps: a Stalwart player in hero mode 0.6 (every source passes here). */
	public static double knockbackFactor(@Nullable Entity entity) {
		return knockbackFactor(entity instanceof Player player && HeroManager.controls(player) && stalwart(HeroManager.hero(player)));
	}

	/** A slow received, as a Stalwart takes it: 40% of it resisted (a 50% slow is 30%). */
	public static double resistedSlow(double slow) {
		return slow * (1 - STALWART_SLOW);
	}

	/**
	 * A speed attribute's value with every slow received from others resisted (a negative modifier not in {@link #OWN_SLOWS}
	 * scaled x0.6), in Minecraft's own order (AttributeInstance.calculateValue: the base plus the flat ones, then the base's
	 * shares added, then each total share multiplied in). A Slowness potion is a total share (-15 % a level), Local
	 * Anesthetic a base one.
	 */
	public static double resistedValue(double base, Collection<AttributeModifier> modifiers, Set<Identifier> own) {
		double value = base;
		for (AttributeModifier m : modifiers) {
			if (m.operation() == AttributeModifier.Operation.ADD_VALUE) {
				value += resisted(m, own);
			}
		}
		double result = value;
		for (AttributeModifier m : modifiers) {
			if (m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
				result += value * resisted(m, own);
			}
		}
		for (AttributeModifier m : modifiers) {
			if (m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
				result *= 1 + resisted(m, own);
			}
		}
		return result;
	}

	private static double resisted(AttributeModifier m, Set<Identifier> own) {
		return m.amount() < 0 && !own.contains(m.id()) ? -resistedSlow(-m.amount()) : m.amount();
	}

	/** This entity's speed with its received slows resisted (Minecraft's sanitized range kept). */
	public static double resistedSpeed(AttributeInstance speed) {
		return speed.getAttribute().value().sanitizeValue(resistedValue(speed.getBaseValue(), speed.getModifiers(), OWN_SLOWS));
	}

	/** Whether a Stalwart's resistance applies to this entity's slows (set by the client for its own player). */
	public static java.util.function.Predicate<LivingEntity> resistsSlows = entity -> entity instanceof Player player
		&& HeroManager.controls(player) && stalwart(HeroManager.hero(player));
}

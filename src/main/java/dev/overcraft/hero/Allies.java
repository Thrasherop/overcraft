package dev.overcraft.hero;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * Whose side a creature is on, for a hero: the one place for it (#99). Overwatch has two teams and no bystanders; a
 * Minecraft world is full of them, so these rules are ours, open for the owner:
 * <ul>
 * <li><b>Allies</b> (healed, buffed, shots pass through): players unless scoreboard teams say otherwise (no teams: co-op),
 * pets of the hero or of an allied player, villagers, iron and snow golems and allays, so a support has someone to heal
 * alone.</li>
 * <li><b>Enemies</b> (what area abilities, locks and auto-aim go for): players on another team, pets of a player not allied,
 * anything Minecraft calls an {@link Enemy} (monsters, slimes, the dragon) but those calm by nature ({@link #calmByNature}: piglins,
 * zombified piglins, endermen), any creature fighting the hero or the hero's side right now ({@link #hostile}: its target, its
 * brain's, its anger, the blow it landed in the last 5 s): an iron golem the hero has angered or that defends its village
 * against them, an angry wolf, bee, llama, goat or Nautilus, and an unowned mount carrying an enemy. A golem turned on the hero is an
 * enemy for as long as Minecraft keeps it so and an ally again after: it had been an ally even while killing them, and heroes
 * couldn't hit back. Who a mob targets is known only on the server: a client still sees a golem as an ally.</li>
 * <li><b>Bystanders</b> (everything else: cows, sheep, horses, a wolf not angry, a copper golem, an armor stand, a calm piglin): neither.
 * No healing or lock, no area ability touches them, and they pay nothing: ult charge, perk progress, On Fire, energy. A hero's shot aimed straight at one
 * still lands ({@link #canHurt}), so a player can hunt without pressing G, and a shot doesn't fly through a cow standing
 * in front of a zombie, while a fight near a farm or a display stays off them ({@link #areaHits}).</li>
 * </ul>
 */
public final class Allies {
	private Allies() {
	}

	public static boolean isAlly(LivingEntity hero, Entity other) {
		return other != hero && other instanceof LivingEntity living && living.isAlive() && !other.isSpectator() && wasAlly(hero, other);
	}

	/** Whether other is (or, just dead, was) on the hero's side: isAlly without its alive check (the kill feed). */
	public static boolean wasAlly(LivingEntity hero, Entity other) {
		return other != hero && other instanceof LivingEntity && friendly(hero, other) && !hostile(hero, other);
	}

	/** On the hero's side by what it is, before asking what it's doing (players, their pets, the friendly kinds). */
	private static boolean friendly(LivingEntity hero, Entity other) {
		if (other instanceof Player player) {
			return playersAllied(hero, player);
		}
		if (other instanceof OwnableEntity ownable && ownable.getOwner() instanceof LivingEntity owner) {
			return owner == hero || (owner instanceof Player p && playersAllied(hero, p));
		}
		return other instanceof AbstractVillager || other instanceof IronGolem || other instanceof SnowGolem
			|| other instanceof Allay;
	}

	/**
	 * Whether this mob is fighting the hero or the hero's side now: its target (DefendVillageTargetGoal, HurtByTargetGoal, a
	 * brain's attack target), or the anger Minecraft keeps after losing sight of them (NeutralMob: an iron golem's, a
	 * wolf's, a bee's). Players and their pets answer to their owner's side, never to this.
	 */
	private static boolean hostile(LivingEntity hero, Entity other) {
		if (!(other instanceof Mob mob) || other instanceof OwnableEntity ownable && ownable.getOwner() != null) {
			return false;
		}
		long now = mob.level().getGameTime();
		LivingEntity target = mob.getTarget();
		if (target == null && mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
			// a brain's own target, which Mob.getTarget doesn't read for every mob: a Zombie Nautilus's, a provoked Nautilus's
			target = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
		}
		if (target != null && (target == hero || friendly(hero, target))) {
			remember(mob, target);
			return true;
		}
		if (mob.level() instanceof ServerLevel level && other instanceof NeutralMob neutral && neutral.isAngry()) {
			if (hero instanceof Player && neutral.isAngryAtAllPlayers(level)) {
				return true;
			}
			EntityReference<LivingEntity> angryAt = neutral.getPersistentAngerTarget();
			if (angryAt != null && (angryAt.matches(hero) || EntityReference.getLivingEntity(angryAt, level) instanceof LivingEntity at && friendly(hero, at))) {
				return true;
			}
		}
		// it hit the hero's side a moment ago, or was at them when it was put to sleep: a llama between spits (its goal ends on each,
		// Llama.didSpit), a goat between rams (a position in its brain, not a target), a held panda
		Fight fight = FIGHTS.get(mob.getUUID());
		if (fight != null) {
			if (now < fight.at || now - fight.at > MEMORY) {
				FIGHTS.remove(mob.getUUID());
			} else if (fight.with.equals(hero.getUUID()) || mob.level().getPlayerByUUID(fight.with) instanceof Player p && friendly(hero, p)) {
				return true;
			}
		}
		return false;
	}

	/** Whom a mob was last fighting and when (game time), so the rule doesn't flicker between its blows. */
	private record Fight(java.util.UUID with, long at) {
	}

	private static final java.util.Map<java.util.UUID, Fight> FIGHTS = new java.util.concurrent.ConcurrentHashMap<>();
	/** How long it counts (ours): 5 s, longer than a llama's spit or a goat's ram takes to come round, and than a sleep's hold. */
	private static final long MEMORY = 100;

	/** {@code mob} is fighting {@code victim} now (its target, a blow it just landed: Overcraft's AFTER_DAMAGE; before a hold clears its target). */
	public static void remember(Mob mob, Entity victim) {
		if (!mob.level().isClientSide()) {
			if (FIGHTS.size() > 512) {
				long now = mob.level().getGameTime();
				FIGHTS.values().removeIf(f -> now - f.at > MEMORY);
			}
			FIGHTS.put(mob.getUUID(), new Fight(victim.getUUID(), mob.level().getGameTime()));
		}
	}

	/** The world stopped. */
	public static void clear() {
		FIGHTS.clear();
	}

	/**
	 * Calm until they're fighting, whatever Minecraft's class says (the owner's default for now, ours, open, #99 and the critique's 8):
	 * a piglin around gold, a zombified piglin, an enderman. An area ability doesn't start a bastion's fight; one that is
	 * after the hero's side is an enemy like any other.
	 */
	private static boolean calmByNature(Entity other) {
		return other instanceof Piglin || other instanceof ZombifiedPiglin || other instanceof Enderman;
	}

	/** An unowned mount carrying someone against the hero (a drowned's Nautilus, a skeleton's horse): the same fight. */
	private static boolean carriesAnEnemy(LivingEntity hero, Entity other) {
		if (other instanceof Player) {
			return false;
		}
		for (Entity rider : other.getPassengers()) {
			if (rider instanceof Player player ? !friendly(hero, player) : rider instanceof Enemy && !calmByNature(rider) || hostile(hero, rider)) {
				return true;
			}
		}
		return false;
	}

	/** The hero themself or an ally: what support abilities like Protection Suzu affect. */
	public static boolean isSelfOrAlly(LivingEntity hero, Entity other) {
		return other == hero || isAlly(hero, other);
	}

	/** An enemy, alive and here. See the class comment: not everything that isn't an ally is one. */
	public static boolean isEnemy(LivingEntity hero, Entity other) {
		return other != hero && other instanceof LivingEntity living && living.isAlive() && !other.isSpectator() && opposed(hero, other);
	}

	/** Against the hero, alive or just dead. */
	private static boolean opposed(LivingEntity hero, Entity other) {
		if (wasAlly(hero, other)) {
			return false;
		}
		return other instanceof Player || other instanceof Enemy && !calmByNature(other) || hostile(hero, other)
			|| carriesAnEnemy(hero, other)
			// a pet of a player who isn't on the hero's side (friendly said no, and it has an owner)
			|| other instanceof OwnableEntity ownable && ownable.getOwner() != null;
	}

	/** Against the hero or, just dead, was: the one definition (kill lines ask this, Voice.onKill). */
	public static boolean wasEnemy(LivingEntity hero, Entity other) {
		return other != hero && other instanceof LivingEntity && opposed(hero, other);
	}

	/**
	 * Neither ally nor enemy: a cow, a calm wolf, an armor stand. Doing something to one earns nothing (Combat.credit), or a
	 * chicken farm would fill an ult. Without an alive check: a kill's credit is asked after the death.
	 */
	public static boolean isBystander(LivingEntity hero, Entity other) {
		return other != hero && other instanceof LivingEntity && !wasAlly(hero, other) && !opposed(hero, other);
	}

	/**
	 * Whether the hero's aimed attacks (a shot along their aim, quick melee, Whip Shot) hurt other: anything alive that isn't
	 * an ally, or an ally with the hero's friendly fire on (a hero option, off by default: Overwatch has none, #20). A
	 * bystander too: see the class comment. An armor stand takes it as Minecraft's own arrow or punch (it breaks in
	 * two quick hits), and gives nothing back.
	 */
	public static boolean canHurt(LivingEntity hero, Entity other) {
		// (not what the hero is riding: a hitscan's way starts inside their mount, and they aren't aiming at it)
		boolean here = other != hero && other != hero.getVehicle() && other instanceof LivingEntity living && living.isAlive() && !other.isSpectator();
		boolean ally = isAlly(hero, other);
		return hurtRule(here, ally, hero instanceof Player player && HeroManager.options(player).friendlyFire(), other instanceof LivingEntity living && Combat.phaseStops(living));
	}

	/** Who a shot's copy takes (OwProjectile.canHitEntity): never the client's; through the phased, unless they're the owner's side. Tested: ShotRuleTest. */
	public static boolean shotRule(boolean clientSide, boolean vanilla, boolean phased, boolean ownSide) {
		return !clientSide && vanilla && (!phased || ownSide);
	}

	/** canHurt without the phase: is it against this hero at all? For what's theirs rather than them (Sombra's hack of a deployable): a phased owner still owns it. */
	public static boolean againstHero(LivingEntity hero, Entity other) {
		boolean here = other != hero && other != hero.getVehicle() && other instanceof LivingEntity living && living.isAlive() && !other.isSpectator();
		return hurtRule(here, isAlly(hero, other), hero instanceof Player player && HeroManager.options(player).friendlyFire(), false);
	}

	/**
	 * canHurt's rule. The phased aren't hit at all: W Status effect rev 408841 "unable to be hit by enemy attacks", W Kiriko rev 408632 "pass through them": a hit passes on to whatever is behind them,
	 * and spends nothing. D: the trackers' filters, m_4EA686DB 20 or 148 (M that bits 16 and 4 are this). This is the one place every aimed hit asks (the kits, OwProjectile for what flies). Package-private for PhaseRulesTest.
	 */
	static boolean hurtRule(boolean here, boolean ally, boolean friendlyFire, boolean phased) {
		return !phased && (here && !ally || friendlyFire && ally);
	}

	/**
	 * Whether the hero's abilities that take everyone in reach, without aiming at them (a burst, a field, a wave, a swing, a
	 * lock), touch other: an enemy, or an ally with friendly fire on. Never a bystander (#99).
	 */
	public static boolean areaHits(LivingEntity hero, Entity other) {
		return !(other instanceof LivingEntity living && Combat.phaseStops(living)) && areaTouches(hero, other);
	}

	/** The same whether or not it's phased: Gravitic Flux's lift alone: its filter (1E41) is mask 16, no bit 4 (M); W "completely unaffected". */
	public static boolean areaTouches(LivingEntity hero, Entity other) {
		return isEnemy(hero, other) || hero instanceof Player player && HeroManager.options(player).friendlyFire() && isAlly(hero, other);
	}

	/**
	 * A boss is too big to carry off (#101: Catnapper's tether). Ours: Overwatch has no bosses and its data has no such rule,
	 * only tanks pulled more slowly (W; no Minecraft mob counts as a tank: Sleep). The same bosses sleep and stun.
	 */
	public static boolean isBoss(Entity other) {
		return other instanceof EnderDragon || other instanceof WitherBoss || other instanceof Warden || other instanceof ElderGuardian;
	}

	private static boolean playersAllied(LivingEntity hero, Player other) {
		if (hero.getTeam() == null && other.getTeam() == null) {
			return true;
		}
		return hero.isAlliedTo(other);
	}
}

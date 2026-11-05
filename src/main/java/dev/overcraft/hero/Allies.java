package dev.overcraft.hero;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
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
 * anything Minecraft calls an {@link Enemy} (monsters, slimes, the dragon), and any creature fighting the hero or the
 * hero's side right now ({@link #hostile}): an iron golem the hero has angered or that defends its village against them,
 * an angry wolf, bee, llama or polar bear. A golem turned on the hero is an enemy for as long as Minecraft keeps it so
 * (its target, then its anger's time) and an ally again after: it had been an ally even while killing them, and heroes
 * couldn't hit back. Who a mob targets is known only on the server: a client still sees a golem as an ally.</li>
 * <li><b>Bystanders</b> (everything else: cows, sheep, horses, a wolf not angry, a copper golem, an armor stand): neither.
 * No healing or lock, no area ability touches them, and they give no ult charge. A hero's shot aimed straight at one
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
		LivingEntity target = mob.getTarget();
		if (target != null && (target == hero || friendly(hero, target))) {
			return true;
		}
		if (other instanceof NeutralMob neutral && neutral.isAngry()) {
			EntityReference<LivingEntity> angryAt = neutral.getPersistentAngerTarget();
			return angryAt != null && angryAt.matches(hero);
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
		return other instanceof Player || other instanceof Enemy || hostile(hero, other)
			// a pet of a player who isn't on the hero's side (friendly said no, and it has an owner)
			|| other instanceof OwnableEntity ownable && ownable.getOwner() != null;
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
		boolean here = other != hero && other instanceof LivingEntity living && living.isAlive() && !other.isSpectator();
		return here && !isAlly(hero, other) || hero instanceof Player player && HeroManager.options(player).friendlyFire() && isAlly(hero, other);
	}

	/**
	 * Whether the hero's abilities that take everyone in reach, without aiming at them (a burst, a field, a wave, a swing, a
	 * lock), touch other: an enemy, or an ally with friendly fire on. Never a bystander (#99).
	 */
	public static boolean areaHits(LivingEntity hero, Entity other) {
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

package dev.overcraft.hero;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * Who counts as a hero's teammate. Other players are allies unless scoreboard teams say otherwise (no teams: co-op).
 * Pets owned by an ally, villagers, golems and allays are allies too, so a support has someone to heal alone.
 */
public final class Allies {
	private Allies() {
	}

	public static boolean isAlly(LivingEntity hero, Entity other) {
		if (other == hero || !(other instanceof LivingEntity living) || !living.isAlive() || other.isSpectator()) {
			return false;
		}
		if (other instanceof Player player) {
			return playersAllied(hero, player);
		}
		if (other instanceof OwnableEntity ownable && ownable.getOwner() instanceof LivingEntity owner) {
			return owner == hero || (owner instanceof Player p && playersAllied(hero, p));
		}
		return other instanceof AbstractVillager || other instanceof IronGolem || other instanceof SnowGolem
			|| other instanceof Allay;
	}

	/** Whether other is (or, just dead, was) on the hero's side: isAlly without its alive check (the kill feed). */
	public static boolean wasAlly(LivingEntity hero, Entity other) {
		if (other == hero || !(other instanceof LivingEntity)) {
			return false;
		}
		if (other instanceof Player player) {
			return playersAllied(hero, player);
		}
		if (other instanceof OwnableEntity ownable && ownable.getOwner() instanceof LivingEntity owner) {
			return owner == hero || (owner instanceof Player p && playersAllied(hero, p));
		}
		return other instanceof AbstractVillager || other instanceof IronGolem || other instanceof SnowGolem
			|| other instanceof Allay;
	}

	/** The hero themself or an ally: what support abilities like Protection Suzu affect. */
	public static boolean isSelfOrAlly(LivingEntity hero, Entity other) {
		return other == hero || isAlly(hero, other);
	}

	public static boolean isEnemy(LivingEntity hero, Entity other) {
		return other != hero && other instanceof LivingEntity living && living.isAlive() && !other.isSpectator()
			&& !isAlly(hero, other);
	}

	/**
	 * Whether the hero's attacks (kunai, quick melee) hurt other: an enemy, or an ally with the hero's friendly fire on
	 * (a hero option, off by default: Overwatch has none, #20).
	 */
	public static boolean canHurt(LivingEntity hero, Entity other) {
		return isEnemy(hero, other) || hero instanceof Player player && HeroManager.options(player).friendlyFire() && isAlly(hero, other);
	}

	private static boolean playersAllied(LivingEntity hero, Player other) {
		if (hero.getTeam() == null && other.getTeam() == null) {
			return true;
		}
		return hero.isAlliedTo(other);
	}
}

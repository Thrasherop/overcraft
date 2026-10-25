package dev.overcraft.hero;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Overwatch's combat rules on top of Minecraft's: damage and healing in Overwatch units, being phased out (immune to
 * damage), the ult charge rules, the global regeneration, and Kitsune Rush's buff.
 */
public final class Combat {
	/** Passive ult charge after spawn (points per second). */
	public static final double ULT_PASSIVE_PER_SECOND = 5;
	/** Global self-regeneration: starts after 6 s without damage (Jun 2025), 10 HP/s + 5% of max health per second (wiki). */
	public static final int REGEN_DELAY = Ow.ticks(6);
	public static final double REGEN_FLAT = 10;
	public static final double REGEN_PERCENT = 0.05;

	private static final Identifier RUSH_SPEED = Overcraft.id("kitsune_rush_speed");
	private static final Identifier RUSH_ATTACK = Overcraft.id("kitsune_rush_attack");

	private static final Map<UUID, Long> phasedUntil = new HashMap<>();
	private static final Map<UUID, Long> rushedUntil = new HashMap<>();
	private static final Map<UUID, LivingEntity> rushedEntities = new HashMap<>();

	private Combat() {
	}

	public static long now(Entity entity) {
		return entity.level().getGameTime();
	}

	/** Deals Overwatch damage; Minecraft's hurt cooldown doesn't apply (Overwatch has none). Returns damage dealt (OW). */
	public static float damage(LivingEntity target, DamageSource source, double owDamage) {
		float before = target.getHealth() + target.getAbsorptionAmount();
		target.setInvulnerableTime(0);
		target.hurt(source, Ow.hp(owDamage));
		target.setInvulnerableTime(0);
		float dealt = before - (target.isAlive() ? target.getHealth() + target.getAbsorptionAmount() : 0);
		return Math.max(0, Ow.toOw(dealt));
	}

	/** Heals in Overwatch units. Returns the healing actually done (OW), which is what ult charge counts. */
	public static float heal(LivingEntity target, double owHeal) {
		float before = target.getHealth();
		target.heal(Ow.hp(owHeal));
		return Ow.toOw(target.getHealth() - before);
	}

	/** Removes what Protection Suzu cleanses: harmful effects, burning, freezing. */
	public static void cleanse(LivingEntity target) {
		List<Holder<net.minecraft.world.effect.MobEffect>> harmful = new ArrayList<>();
		for (MobEffectInstance effect : target.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				harmful.add(effect.getEffect());
			}
		}
		harmful.forEach(target::removeEffect);
		target.clearFire();
		target.setTicksFrozen(0);
	}

	public static void phase(LivingEntity target, double seconds) {
		long until = now(target) + Ow.ticks(seconds);
		phasedUntil.merge(target.getUUID(), until, Math::max);
	}

	public static boolean isPhased(LivingEntity target) {
		Long until = phasedUntil.get(target.getUUID());
		return until != null && now(target) < until;
	}

	/** Kitsune Rush: in (or just out of) a corridor until {@code until}. */
	public static void rush(LivingEntity target, long until) {
		rushedUntil.merge(target.getUUID(), until, Math::max);
		rushedEntities.put(target.getUUID(), target);
	}

	public static boolean isRushed(LivingEntity target) {
		Long until = rushedUntil.get(target.getUUID());
		return until != null && now(target) < until;
	}

	/** Rate a hero's weapon recovery and reloads tick at (Kitsune Rush: +50% attack and reload speed). */
	public static double attackRate(LivingEntity hero) {
		return isRushed(hero) ? 1 + dev.overcraft.kiriko.KirikoStats.RUSH_ATTACK_SPEED : 1;
	}

	/** Rate a hero's cooldowns tick at (Kitsune Rush: +100% cooldown recharge). */
	public static double cooldownRate(LivingEntity hero) {
		return isRushed(hero) ? 1 + dev.overcraft.kiriko.KirikoStats.RUSH_COOLDOWN_RATE : 1;
	}

	public static void addUltCharge(Player hero, double points) {
		HeroState state = HeroManager.state(hero);
		if (state == null || state.hero == null || state.ultActive > 0 || points <= 0) {
			return;
		}
		state.ultCharge = Math.min(ultCost(state.hero), state.ultCharge + points);
	}

	/**
	 * Ult charge for completing a Minecraft advancement, as a share of the hero's ult cost (owner's rule; Overwatch
	 * has nothing like it, so the shares are ours): task 10%, goal 25%, challenge 50%. Recipe unlocks have no display
	 * and don't count.
	 */
	public static void onAdvancement(Player player, AdvancementType type) {
		Hero hero = HeroManager.hero(player);
		if (hero == null) {
			return;
		}
		double share = switch (type) {
			case TASK -> 0.10;
			case GOAL -> 0.25;
			case CHALLENGE -> 0.50;
		};
		addUltCharge(player, share * ultCost(hero));
	}

	public static int ultCost(Hero hero) {
		return switch (hero) {
			case KIRIKO -> dev.overcraft.kiriko.KirikoStats.ULT_COST;
			default -> 1;
		};
	}

	/** Every server tick: Kitsune Rush's attribute buffs on and off, expired entries dropped. */
	public static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, LivingEntity>> it = rushedEntities.entrySet().iterator();
		while (it.hasNext()) {
			LivingEntity entity = it.next().getValue();
			boolean on = entity.isAlive() && !entity.isRemoved() && isRushed(entity);
			setModifier(entity, Attributes.MOVEMENT_SPEED, RUSH_SPEED, on ? dev.overcraft.kiriko.KirikoStats.RUSH_MOVE_SPEED : 0);
			setModifier(entity, Attributes.ATTACK_SPEED, RUSH_ATTACK, on ? dev.overcraft.kiriko.KirikoStats.RUSH_ATTACK_SPEED : 0);
			if (!on) {
				rushedUntil.remove(entity.getUUID());
				it.remove();
			}
		}
		if (server.getTickCount() % 200 == 0) {
			long time = server.overworld().getGameTime();
			phasedUntil.values().removeIf(until -> until < time);
		}
	}

	private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (amount == 0) {
			instance.removeModifier(id);
		} else if (!instance.hasModifier(id)) {
			instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	public static void clear() {
		phasedUntil.clear();
		rushedUntil.clear();
		rushedEntities.clear();
	}
}

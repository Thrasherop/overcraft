package dev.overcraft.hero;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.net.HeroActionPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
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

	/**
	 * The same through one of the Ender Dragon's parts (its head and neck take it all, the rest a quarter plus one:
	 * Minecraft's rule, EnderDragon.hurt).
	 */
	public static float damagePart(ServerLevel level, EnderDragonPart part, DamageSource source, double owDamage) {
		LivingEntity dragon = part.parentMob;
		float before = dragon.getHealth() + dragon.getAbsorptionAmount();
		dragon.setInvulnerableTime(0);
		part.hurtServer(level, source, Ow.hp(owDamage));
		dragon.setInvulnerableTime(0);
		float dealt = before - (dragon.isAlive() ? dragon.getHealth() + dragon.getAbsorptionAmount() : 0);
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

	/**
	 * Rate a hero's weapon recovery and reloads tick at: Kitsune Rush's +50% attack and reload speed, and Ready Step's
	 * +40% (#36). Together they add up (ours: no source says how Overwatch stacks them).
	 */
	public static double attackRate(LivingEntity hero) {
		double rate = isRushed(hero) ? 1 + dev.overcraft.kiriko.KirikoStats.RUSH_ATTACK_SPEED : 1;
		HeroState state = hero instanceof Player player ? HeroManager.state(player) : null;
		if (state != null && now(hero) < state.attackBuffUntil) {
			rate += state.attackBuff;
		}
		// Accelerando (Lúcio's perk): +15% with the speed song, tripled during Amp It Up
		if (state != null && state.speedSong && hero instanceof Player player && HeroManager.hasPerk(player, Perk.LUCIO_ACCELERANDO)) {
			rate += dev.overcraft.lucio.LucioStats.ACCELERANDO * (state.ampUp > 0 ? 3 : 1);
		}
		return rate;
	}

	/** Rate a hero's cooldowns tick at (Kitsune Rush: +100% cooldown recharge). */
	public static double cooldownRate(LivingEntity hero) {
		return isRushed(hero) ? 1 + dev.overcraft.kiriko.KirikoStats.RUSH_COOLDOWN_RATE : 1;
	}

	/**
	 * On Fire (#13): it still exists in Overwatch 2 (back June 13, 2023), but Blizzard never gave its numbers; these
	 * are the wiki's OW1 ones (the On Fire page: 250 points to catch fire, 5 a second decay, damage worth 100 points
	 * per victim's full health, healing 1 per 5 HP) and Blizzard's 3 s before it can start again (July 11, 2023).
	 */
	public static final double FIRE_AT = 250;
	public static final double FIRE_MAX = 350;
	public static final double FIRE_DECAY_PER_SECOND = 5;
	public static final int FIRE_COOLDOWN_TICKS = 60;

	/** Points toward On Fire for damage dealt (Overwatch HP) to a victim of this max health (Overwatch HP). */
	public static void addFireForDamage(Player hero, double dealt, double victimMaxHealth) {
		if (victimMaxHealth > 0) {
			addFire(hero, dealt / victimMaxHealth * 100);
		}
	}

	/** Points toward On Fire for healing done (Overwatch HP). */
	public static void addFireForHealing(Player hero, double healed) {
		addFire(hero, healed / 5);
	}

	private static void addFire(Player hero, double points) {
		HeroState state = HeroManager.state(hero);
		if (state != null && state.hero != null && points > 0) {
			state.fireScore = Math.min(FIRE_MAX, state.fireScore + points);
		}
	}

	/** Each tick: the score decays; reaching FIRE_AT sets the hero On Fire (with its line), falling below ends it. */
	public static void tickFire(ServerPlayer player, HeroState state) {
		state.fireScore = Math.max(0, state.fireScore - FIRE_DECAY_PER_SECOND / 20);
		long now = player.level().getGameTime();
		if (!state.onFire && state.fireScore >= FIRE_AT && now >= state.fireReadyAt) {
			state.onFire = true;
			Voice.onFire(player, state);
		} else if (state.onFire && state.fireScore < FIRE_AT) {
			state.onFire = false;
			state.fireReadyAt = now + FIRE_COOLDOWN_TICKS;
		}
	}

	/** Damage dealt or healing given (Overwatch points): ult charge and perk progress alike. */
	public static void credit(Player hero, double points) {
		addUltCharge(hero, points);
		Perks.add(hero, points);
	}

	public static void addUltCharge(Player hero, double points) {
		addUltCharge(hero, points, false);
	}

	/**
	 * Ult charge. A Tactician (Lúcio) keeps charging past 100%, to 125%, at 75% of the rate above 100% (Feb 10 2026);
	 * passive charge stops at 100% (the wiki).
	 */
	public static void addUltCharge(Player hero, double points, boolean passive) {
		HeroState state = HeroManager.state(hero);
		if (state == null || state.hero == null || state.ultActive > 0 || points <= 0) {
			return;
		}
		double cost = ultCost(state.hero);
		boolean wasReady = state.ultCharge >= cost;
		if (tactician(state.hero) && !passive) {
			double below = Math.max(0, Math.min(points, cost - state.ultCharge));
			double above = (points - below) * dev.overcraft.lucio.LucioStats.TACTICIAN_RATE;
			state.ultCharge = Math.min(cost * dev.overcraft.lucio.LucioStats.TACTICIAN_CAP, state.ultCharge + below + above);
		} else if (state.ultCharge < cost) {
			state.ultCharge = Math.min(cost, state.ultCharge + points);
		}
		if (!wasReady && state.ultCharge >= cost && hero instanceof ServerPlayer player) {
			Voice.say(player, "ult_ready", 1, false);
		}
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

	/** Overwatch's Tactician sub-role (Lúcio): ult charge kept past 100% and after casting. */
	public static boolean tactician(Hero hero) {
		return hero.subRole.equals("tactician");
	}

	/** What's left of the ult charge after casting: nothing, or a Tactician's excess. */
	public static double afterUlt(Hero hero, double charge) {
		return tactician(hero) ? Math.max(0, charge - ultCost(hero)) : 0;
	}

	public static int ultCost(Hero hero) {
		return switch (hero) {
			case KIRIKO -> dev.overcraft.kiriko.KirikoStats.ULT_COST;
			case LUCIO -> dev.overcraft.lucio.LucioStats.ULT_COST;
			default -> 1;
		};
	}

	/** Overhealth given (Sound Barrier), decaying: what's left of it (Minecraft health), the decay a tick, the end. */
	private record Overhealth(LivingEntity entity, double perTick, long until) {
	}

	private static final Map<UUID, Overhealth> overhealth = new HashMap<>();
	private static final Map<UUID, Double> overhealthLeft = new HashMap<>();
	private static final Identifier OVERHEALTH = Overcraft.id("overhealth");

	/**
	 * Gives temporary overhealth (Overwatch points) that decays to nothing over {@code seconds}, as Minecraft
	 * absorption (golden hearts): Sound Barrier's. A new one replaces what's left of an old one.
	 */
	public static void overhealth(LivingEntity entity, double owAmount, double seconds) {
		double amount = Ow.hp(owAmount);
		clearOverhealth(entity);
		AttributeInstance max = entity.getAttribute(Attributes.MAX_ABSORPTION);
		if (max != null) {
			max.addOrReplacePermanentModifier(new AttributeModifier(OVERHEALTH, amount, AttributeModifier.Operation.ADD_VALUE));
		}
		entity.setAbsorptionAmount(entity.getAbsorptionAmount() + (float) amount);
		overhealth.put(entity.getUUID(), new Overhealth(entity, amount / Ow.ticks(seconds), now(entity) + Ow.ticks(seconds)));
		overhealthLeft.put(entity.getUUID(), amount);
	}

	private static void clearOverhealth(LivingEntity entity) {
		Double left = overhealthLeft.remove(entity.getUUID());
		overhealth.remove(entity.getUUID());
		if (left != null) {
			entity.setAbsorptionAmount((float) Math.max(0, entity.getAbsorptionAmount() - left));
		}
		AttributeInstance max = entity.getAttribute(Attributes.MAX_ABSORPTION);
		if (max != null) {
			max.removeModifier(OVERHEALTH);
		}
	}

	private static void tickOverhealth() {
		for (Overhealth o : new ArrayList<>(overhealth.values())) {
			UUID id = o.entity.getUUID();
			double left = Math.min(overhealthLeft.getOrDefault(id, 0.0), o.entity.getAbsorptionAmount());
			if (!o.entity.isAlive() || o.entity.isRemoved() || now(o.entity) >= o.until || left <= 0) {
				overhealthLeft.put(id, left);
				clearOverhealth(o.entity);
				continue;
			}
			double decay = Math.min(o.perTick, left);
			o.entity.setAbsorptionAmount((float) (o.entity.getAbsorptionAmount() - decay));
			overhealthLeft.put(id, left - decay);
		}
	}

	/** A timed attribute buff on an entity (Foxtrot's speed), until a game time. */
	private record Buff(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount, long until) {
	}

	private static final Map<String, Buff> buffs = new HashMap<>();

	/**
	 * Gives an entity a timed attribute buff (a share of its base: 0.3 = +30%), or renews it. Kept on while it lasts and
	 * taken off after, as Kitsune Rush's are.
	 */
	public static void buff(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount, double seconds) {
		long until = now(entity) + Ow.ticks(seconds);
		buffs.merge(entity.getUUID() + "/" + id, new Buff(entity, attribute, id, amount, until),
			(old, fresh) -> fresh.until > old.until ? fresh : old);
		setModifier(entity, attribute, id, amount);
	}

	/** Every server tick: Kitsune Rush's attribute buffs on and off, expired entries dropped. */
	public static void tick(MinecraftServer server) {
		tickOverhealth();
		Iterator<Buff> buffIt = buffs.values().iterator();
		while (buffIt.hasNext()) {
			Buff buff = buffIt.next();
			if (!buff.entity.isAlive() || buff.entity.isRemoved() || now(buff.entity) >= buff.until) {
				setModifier(buff.entity, buff.attribute, buff.id, 0);
				buffIt.remove();
			}
		}
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

	/** Tells the hero and everyone who can see them to play an animation (a throw, a cast). */
	public static void animate(ServerPlayer hero, String clip) {
		HeroActionPayload payload = new HeroActionPayload(hero.getId(), clip);
		ServerPlayNetworking.send(hero, payload);
		for (ServerPlayer watcher : PlayerLookup.tracking(hero)) {
			ServerPlayNetworking.send(watcher, payload);
		}
	}

	public static void clear() {
		phasedUntil.clear();
		rushedUntil.clear();
		rushedEntities.clear();
		buffs.clear();
		overhealth.clear();
		overhealthLeft.clear();
	}
}

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
	/**
	 * Being hurt by an enemy cuts the healing received (the role graph 01C7, everyone's since BZ 2026-02-10 "In-Combat Healing
	 * Reduction on Damage: -30%", "Reduced effectiveness against Tanks: -15%"; BZ 2026-07-30's 15% was Quick Play Hacked's alone,
	 * and the notes to 2026-11-03 change nothing else of it): D #1308 v25267 = 1A33 0.3, x 1CAC 0.5 for a tank, for 1E68 3 s (#881).
	 * W's rules: a new hit doesn't stack; no self-damage; it multiplies with other healing modifiers; it's cleansable. W's box is
	 * flagged "removed" and was edited on after that (its table of exempt abilities, none of them ours): D and BZ settle it
	 * (tools/reference/sigma.md, Disagreements 11).
	 */
	public static final double HEAL_CUT = 0.3, HEAL_CUT_TANK_SHARE = 0.5, HEAL_CUT_TIME = 3;

	private static final Identifier RUSH_SPEED = Overcraft.id("kitsune_rush_speed");
	private static final Identifier RUSH_ATTACK = Overcraft.id("kitsune_rush_attack");

	private static final Map<UUID, Long> phasedUntil = new HashMap<>();
	/** Ana's Biotic Grenade on a creature (game time it ends): healing received +50% (an ally), none (an enemy). */
	private static final Map<UUID, Long> healBoostUntil = new HashMap<>();
	private static final Map<UUID, Long> antiHealUntil = new HashMap<>();
	/** Dev only (DevBot's "fixoff"): #117's cleanse and #109's float switched off, to show the fixes are what made the difference. */
	public static boolean devFixOff;
	/** Dev only (DevBot's "fix 119"): #119's list (below) and what the phase prevents switched off, the rest of the cleanse as it was. */
	public static boolean devCleanseOff;
	/** The healing reduction on a creature hurt by an enemy (game time it ends). */
	private static final Map<UUID, Long> healCutUntil = new HashMap<>();
	private static final Map<UUID, Long> rushedUntil = new HashMap<>();
	private static final Map<UUID, LivingEntity> rushedEntities = new HashMap<>();

	private Combat() {
	}

	public static long now(Entity entity) {
		return entity.level().getGameTime();
	}

	/**
	 * Deals Overwatch damage; Minecraft's hurt cooldown doesn't apply (Overwatch has none). Returns damage dealt (OW).
	 * In 26.x that cooldown is LivingEntity.damageCooldownTime: invulnerableTime is now spawn and portal protection, and
	 * clearing it let the cooldown drop every hit after the first in half a second (a volley's 5 shots did 4: found
	 * playing Jetpack Cat).
	 */
	/**
	 * A hitscan shot's damage from a hero (Ana's rifle, Sombra's pistol, Sojourn's Charged Shot): overcraft:hitscan, not a
	 * projectile, so endermen take it instead of teleporting away (the owner's balance call, #92); otherwise as the arrow
	 * type they used (armor; no knockback, as no hero hit has: heroHit). No direct entity, so the kill feed doesn't take it for melee.
	 */
	public static DamageSource hitscan(net.minecraft.server.level.ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
				dev.overcraft.Overcraft.id("hitscan"))), null, owner);
	}

	/**
	 * A hero's explosion (Pharah's rockets, D.Va's missiles and Self-Destruct, Sigma's spheres and rock): overcraft:blast,
	 * Minecraft's player explosion but scaled by the difficulty only when a mob causes it, as arrows are. Minecraft's own
	 * explosion types always scale, so on Easy a hero's splash on herself or another player came off smaller and on Hard
	 * larger (found playing Tracer: 51.8 came off as 38 on Easy). Tagged as an explosion (blast protection, panic,
	 * armor stands), no knockback (the kits push themselves).
	 */
	public static DamageSource blast(net.minecraft.world.level.Level level, @org.jspecify.annotations.Nullable Entity direct,
			@org.jspecify.annotations.Nullable Entity owner) {
		return new DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
				dev.overcraft.Overcraft.id("blast"))), direct, owner);
	}

	/**
	 * Deals a hero's damage, saying what dealt it ({@code blow}: the weapon, melee, an ability, a perk, the ult, whether
	 * it was critical, and the hero who dealt it, filled from the attacker's hero when not given). A death it causes is
	 * told by it (KillFeed, which also calls Voice: they run inside the hurt). The hit itself moves no one (heroHit): what
	 * pushes does so through push or launch, which remember it for the kill credit (#84).
	 */
	public static float damage(LivingEntity target, DamageSource source, double owDamage, Blow blow) {
		return deal(target, target, source, blow, () -> target.hurt(source, Ow.hp(owDamage)));
	}

	/**
	 * The same through one of the Ender Dragon's parts (its head and neck take it all, the rest a quarter plus one:
	 * Minecraft's rule, EnderDragon.hurt). The dragon's death runs inside this hurt too (LivingEntity.die calls its
	 * handleKillingBlow, which starts the DYING phase), so its kill is told with the blow like any other.
	 */
	public static float damagePart(ServerLevel level, EnderDragonPart part, DamageSource source, double owDamage, Blow blow) {
		return deal(part.parentMob, part, source, blow, () -> part.hurtServer(level, source, Ow.hp(owDamage)));
	}

	private static float deal(LivingEntity target, Entity hit, DamageSource source, Blow blow, Runnable hurt) {
		blow = withHero(blow, source.getEntity());
		float before = target.getHealth() + target.getAbsorptionAmount();
		dealingWhile(source.getEntity(), target, blow, () -> {
			target.damageCooldownTime = 0;
			hurt.run();
			target.damageCooldownTime = 0;
		});
		float dealt = before - (target.isAlive() ? target.getHealth() + target.getAbsorptionAmount() : 0);
		// Play of the Game's scoring: every hero hit passes here (dev.overcraft.potg.PlayScore)
		dev.overcraft.potg.PlayScore.damage(source.getEntity(), target, Math.max(0, Ow.toOw(dealt)), blow);
		return Math.max(0, Ow.toOw(dealt));
	}

	/** The blow with its hero: as given, else the attacker's hero now (a hit that lands as it's made). */
	public static Blow withHero(Blow blow, @org.jspecify.annotations.Nullable Entity attacker) {
		return blow.hero() == null && attacker instanceof Player player ? blow.by(HeroManager.hero(player)) : blow;
	}

	/** The hero damage being dealt right now (inside Combat.damage's hurt): who, to whom, with what. */
	private record Dealt(@org.jspecify.annotations.Nullable Object attacker, Object target, Blow blow) {
	}

	private static @org.jspecify.annotations.Nullable Dealt dealing;

	/**
	 * Runs {@code hurt} with this blow as the one being dealt, then puts back whatever was being dealt before (a hurt
	 * that deals hero damage of its own, nested). Package-private for BlowTest.
	 */
	static void dealingWhile(@org.jspecify.annotations.Nullable Object attacker, Object target, Blow blow, Runnable hurt) {
		Dealt outer = dealing;
		dealing = new Dealt(attacker, target, blow);
		try {
			hurt.run();
		} finally {
			dealing = outer;
		}
	}

	/**
	 * Whether the hurt running on {@code victim} with {@code source} is a hero's hit (dealt through Combat.damage, #93).
	 * Overwatch hits don't push: weapon fire and most abilities have no knockback at all (the wiki's Status effect page,
	 * Knockback, rev 408792, lists the few that do: for our heroes Brigitte's Shield Bash and Whip Shot, Jetpack Cat's Purr
	 * and Catnapper, Lúcio's Soundwave, Sigma's Hyperspheres, Accretion and Hyper Strike, and quick melee's "very small"
	 * one), and those push the target themselves after the hit. So a hero's hit takes none of Minecraft's default knockback
	 * (0.4 and a hop, in a random direction when the hit has no position: hitscan, the flail) and no impact either (a hurt
	 * player is sent the server's stale copy of their motion, a jolt: minecraft_26.md #82); an ability that pushes does it
	 * through push or launch. LivingEntityMixin asks this inside LivingEntity.hurtServer.
	 */
	public static boolean heroHit(LivingEntity victim, DamageSource source) {
		Dealt d = dealing;
		return d != null && d.target == victim && d.attacker == source.getEntity();
	}

	/**
	 * What share of a push {@code target} takes from its state (M6): a Stalwart's (Tank), Sigma's in Flux, Mercy in Valkyrie and
	 * just after a launch, Juno gliding, Jetpack Cat flying (the wiki's table of who takes less: Status effect, Knockback). Not
	 * its knockback resistance, which Minecraft's own knockback applies itself.
	 */
	public static double knockbackTaken(@org.jspecify.annotations.Nullable Entity target) {
		double state = 1;
		if (target instanceof Player player && HeroManager.controls(player) && HeroManager.state(player) instanceof HeroState s) {
			if (s.hero == Hero.MERCY && dev.overcraft.mercy.Mercy.flying(s)) {
				// half in Valkyrie (W; her data's 0A04B51B -0.5), a fifth of that just after a launch (006E #477: 0A04B51B x0.2)
				state = dev.overcraft.mercy.MercyStats.VALKYRIE_KNOCKBACK
					* (s.gaPhase == dev.overcraft.mercy.Mercy.GA_LAUNCHED ? dev.overcraft.mercy.MercyStats.GA_LAUNCH_KNOCKBACK_VALKYRIE : 1);
			} else if (dev.overcraft.juno.Juno.gliding(s)) {
				// half in Glide Boost (her data's 0A04B51B = 1DF3 -0.5, 47B7 #774)
				state = dev.overcraft.juno.JunoStats.GLIDE_KNOCKBACK;
			} else if (s.hero == Hero.JETPACK_CAT) {
				state = dev.overcraft.jetpackcat.JetpackCatStats.KNOCKBACK_TAKEN;
			}
		}
		// D.Va in her Boosters: a tenth (047E #243)
		double boosters = target instanceof LivingEntity living ? dev.overcraft.dva.Boosters.knockbackFactor(living) : 1;
		return state * boosters * Tank.knockbackFactor(target) * dev.overcraft.sigma.GraviticFlux.knockbackFactor(target);
	}

	/** The share of a hero's push that reaches it: its state's, and its knockback resistance (a warden, a ravager: none). */
	public static double pushShare(LivingEntity target) {
		return knockbackTaken(target) * Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
	}

	/** What it is moving at now: a player's client moves them, so the speed it reports (the server's copy is stale). */
	public static net.minecraft.world.phys.Vec3 motion(LivingEntity target) {
		return target instanceof ServerPlayer player ? player.getKnownMovement() : target.getDeltaMovement();
	}

	/**
	 * A hero's push on top of what {@code target} is doing: the one way an ability knocks something back (#93). Scaled by
	 * pushShare, none while it is phased (Protection Suzu, Life Grip), sent to its own client when it is a player, and remembered
	 * for the kill credit.
	 */
	public static void push(LivingEntity target, net.minecraft.world.phys.Vec3 impulse, @org.jspecify.annotations.Nullable Entity attacker, Blow blow) {
		if (!isPhased(target)) {
			target.setDeltaMovement(motion(target).add(impulse.scale(pushShare(target))));
			send(target);
			pushed(target, attacker, blow);
		}
	}

	/** The same, replacing what it was doing: it leaves at {@code velocity} (Soundwave). */
	public static void launch(LivingEntity target, net.minecraft.world.phys.Vec3 velocity, @org.jspecify.annotations.Nullable Entity attacker, Blow blow) {
		if (!isPhased(target)) {
			target.setDeltaMovement(velocity.scale(pushShare(target)));
			send(target);
			pushed(target, attacker, blow);
		}
	}

	/**
	 * Sends a speed just set on {@code target}: to the others watching (needsSync), and at once to its own client when it is a
	 * player, who moves on it. The tracker's syncVelocity isn't enough: played with two clients, it goes a tick later, after
	 * their next movement packet has reset the server's copy (Accretion's first version: the rock's knockback reached a player as
	 * nothing; minecraft_26.md). So the packet goes now, and syncVelocity is cleared for it not to undo this.
	 */
	public static void send(LivingEntity target) {
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
			player.syncVelocity = false;
		}
	}

	/** The blow being dealt to {@code victim} by {@code attacker} right now, or null. Package-private for BlowTest. */
	static @org.jspecify.annotations.Nullable Blow dealtTo(Object victim, @org.jspecify.annotations.Nullable Object attacker) {
		Dealt d = dealing;
		return d != null && d.target == victim && d.attacker == attacker ? d.blow : null;
	}

	/**
	 * A creature's last push by a hero: by whom, with what, when (game time), and its own tickCount then (to compare with
	 * a later mob's blow, LivingEntity.lastHurtByMobTimestamp).
	 */
	public record LastPush(UUID attacker, Blow blow, long at, int victimTick) {
	}

	private static final Map<UUID, LastPush> lastPushes = new HashMap<>();

	/**
	 * How long a hero's push still counts for a death by the world (a fall, the void, lava, a wall): Minecraft's own kill
	 * credit window (LivingEntity.lastHurtByPlayerMemoryTime, 100 ticks).
	 */
	public static final int CREDIT_TICKS = 100;

	/**
	 * A hero's blow moved {@code target} (push and launch call this; Flux, Whip Shot, quick melee and Accretion's rock call it
	 * for their own moves): if the world kills it within CREDIT_TICKS, the kill is that hero's, with that blow.
	 */
	public static void pushed(LivingEntity target, @org.jspecify.annotations.Nullable Entity attacker, Blow blow) {
		if (attacker instanceof Player hero && hero != target) {
			lastPushes.put(target.getUUID(), new LastPush(hero.getUUID(), withHero(blow, hero), now(target), target.tickCount));
		}
	}

	/**
	 * What dealt the blow that is killing {@code victim} with {@code source}: the hero damage being dealt to it right
	 * now, or null (not a hero's: a zombie's bite, a sword in Minecraft mode, the world).
	 */
	public static @org.jspecify.annotations.Nullable Blow blow(LivingEntity victim, DamageSource source) {
		return dealtTo(victim, source.getEntity());
	}

	/**
	 * The deaths the world deals that a push can cause: a fall, the void, lava, a wall it was pushed into (suffocation, or
	 * flying into one). Not sunburn, drowning, starving, a dispenser's arrow, cramming or /kill.
	 */
	public static boolean pushDeath(DamageSource source) {
		return source.getEntity() == null && (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
			|| source.typeHolder().unwrapKey().map(key -> pushDeathType(key.identifier().toString())).orElse(false));
	}

	/** The world's damage types (besides falls) a push can kill with, by id. Package-private for BlowTest. */
	static boolean pushDeathType(String id) {
		return switch (id) {
			case "minecraft:out_of_world", "minecraft:lava", "minecraft:in_wall", "minecraft:fly_into_wall", "minecraft:fall" -> true;
			default -> false;
		};
	}

	/** Whether a push made at {@code at} still counts at {@code now} (game times). Package-private for BlowTest. */
	static boolean inCreditWindow(long now, long at) {
		return now - at <= CREDIT_TICKS;
	}

	/**
	 * The hero push that a death by the world is credited to: within CREDIT_TICKS, and not overtaken by a later blow from
	 * someone else (a skeleton's arrow after the hero's boop takes the kill, as Minecraft's own kill credit would);
	 * null otherwise.
	 */
	public static @org.jspecify.annotations.Nullable LastPush recentPush(LivingEntity victim) {
		LastPush last = lastPushes.get(victim.getUUID());
		if (last == null || !inCreditWindow(now(victim), last.at)) {
			return null;
		}
		LivingEntity other = victim.getLastHurtByMob();
		if (other != null && !other.getUUID().equals(last.attacker) && victim.getLastHurtByMobTimestamp() > last.victimTick) {
			return null;
		}
		return last;
	}

	/** It died: its last push has been told, and is forgotten. */
	public static void forget(LivingEntity victim) {
		lastPushes.remove(victim.getUUID());
	}

	/**
	 * Heals in Overwatch units. Returns the healing actually done (OW), which is what ult charge counts. Ana's Biotic
	 * Grenade scales it: none on an anti-healed creature, +50% on a boosted one; a hurt one's takes the healing reduction too.
	 * Minecraft's own healing (food, potions) doesn't pass here and isn't scaled, as Overwatch's health packs heal for their
	 * full amount (W Global Passive Abilities; ours: the owner's open question, ana-plan.md).
	 */
	public static float heal(LivingEntity target, double owHeal) {
		double scale = healScale(antiHealed(target), healBoosted(target), healCut(target), healCutTank(target));
		if (scale <= 0) {
			return 0;
		}
		float before = target.getHealth();
		target.heal(Ow.hp(owHeal * scale));
		return Ow.toOw(target.getHealth() - before);
	}

	/** What healing received is multiplied by. Package-private for HealScaleTest. */
	static double healScale(boolean antiHealed, boolean boosted, boolean cut, boolean tank) {
		if (antiHealed) {
			return 0;
		}
		double scale = boosted ? 1 + dev.overcraft.ana.AnaStats.GRENADE_BOOST : 1;
		return cut ? scale * (1 - HEAL_CUT * (tank ? HEAL_CUT_TANK_SHARE : 1)) : scale;
	}

	/** Whether a hurt is the kind that cuts healing: by someone else, not on their side. Package-private for HealScaleTest. */
	static boolean cutsHealing(boolean hasAttacker, boolean self, boolean ally) {
		return hasAttacker && !self && !ally;
	}

	/**
	 * It was hurt (Overcraft's AFTER_DAMAGE: a hurt whose amount, before armor, is over 0, a mob's as much as a hero's): the graph's path into the
	 * reduction, 01C7 #1281 (message 0011: damage taken, v35367 its tags, v18513 the attacker) -> #1202 -> #882 -> #2712 -> #881:
	 * <ul>
	 * <li>#1202: the attacker, or its owner, passes a filter of a player hero (m_84065FD1 1) on the other side (m_66747E7D 2) (H nodes, M the filter's meaning, by
	 * Ana's and Lifeweaver's graphs). So nothing with no attacker cuts (a fall, lava, drowning, and in Minecraft burning, poison and wither, which name
	 * none), nor your own, nor your side's. Ours: a mob counts as that attacker, as Overcraft's fights are against mobs.</li>
	 * <li>#882: not a hit tagged 191F, Local Anesthetic's damage over time (Sleep.quiet: the only damage that runs under it).</li>
	 * <li>#2712: {v61743, not v76560} only a Damage hero's (#2702, category 084E): a mode's rule (M: Stadium's, by W Passive ability). Not ours.</li>
	 * <li>#881: the 3 s wait, from the start again on each hit (m_0DBAFD7F 1: M, and W "doesn't stack").</li>
	 * <li>Its stop, #1186 {v6604 or v8440} -> #881's stop plug: v8440 is the flag Protection Suzu's status (180C #60) and Life Grip (21EE #606) set, so the phase ends it (H nodes, M the flag's meaning, #119); v6604: not read. Message 2525 (#1191 -> #1185 -> a 0.25 s wait
	 * -> #1184): read, undecoded. #610 StateCombatModFilter x9007B042, op67(v25267): the healing scale itself (M, as Ana's B064); m_7724EC51 and m_5BF820E4: undecoded.</li>
	 * </ul>
	 * A no-team hit from another player or their pet counts as their own side's (Allies: with no teams everyone is an ally): it cuts nothing: decision, ours.
	 */
	public static void onHurt(LivingEntity target, DamageSource source) {
		Entity by = source.getEntity();
		// a player's own side, or a pet's owner's (a mob asked about a player answers "no teams, so allied", Allies.playersAllied: played,
		// a husk's hit cut nothing)
		LivingEntity side = by instanceof Player player ? player
			: by instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwner() instanceof LivingEntity owner ? owner : null;
		boolean ally = side != null && (side == target || Allies.wasAlly(side, target));
		if (!Sleep.quiet() && cutsHealing(by != null, by == target, ally)) {
			healCutUntil.put(target.getUUID(), now(target) + Ow.ticks(HEAL_CUT_TIME));
		}
	}

	public static boolean healCut(LivingEntity target) {
		Long until = healCutUntil.get(target.getUUID());
		return until != null && now(target) < until;
	}

	/** Whether the reduction on this one is a tank's (a player in hero mode as a tank, as Tank.ultFactor reads it). */
	private static boolean healCutTank(LivingEntity target) {
		return target instanceof Player player && HeroManager.controls(player) && Tank.tank(HeroManager.hero(player));
	}

	/** It died: nothing that scales its healing comes back with it (a player respawns under the same UUID). */
	public static void clearHealing(LivingEntity target) {
		healCutUntil.remove(target.getUUID());
		healBoostUntil.remove(target.getUUID());
		antiHealUntil.remove(target.getUUID());
	}

	/** Healing received +50% for {@code seconds} (an ally in Biotic Grenade's burst), not on an anti-healed one. */
	public static void boostHealing(LivingEntity target, double seconds) {
		if (!antiHealed(target)) {
			healBoostUntil.merge(target.getUUID(), now(target) + Ow.ticks(seconds), Math::max);
		}
	}

	/** No healing at all for {@code seconds} (an enemy in Biotic Grenade's burst); a boost it had ends. Not on the phased (W Kiriko rev 408632). */
	public static void antiHeal(LivingEntity target, double seconds) {
		if (isPhased(target)) {
			return;
		}
		antiHealUntil.merge(target.getUUID(), now(target) + Ow.ticks(seconds), Math::max);
		healBoostUntil.remove(target.getUUID());
	}

	/**
	 * Sight from {@code from} to any part of it: eyes, middle or feet, blocks in the way (0B31: several hit points; ours these three). What area abilities ask: EMP, the Suzu, the
	 * numbness, Rally's pulse and wave. A one-line eye-to-eye test missed a target whose head alone was behind something.
	 */
	public static boolean inSight(net.minecraft.world.level.Level level, net.minecraft.world.phys.Vec3 from, LivingEntity target, Entity viewer) {
		for (double y : new double[] {target.getEyeY(), target.getY() + target.getBbHeight() / 2, target.getY() + 0.1}) {
			if (level.clip(new net.minecraft.world.level.ClipContext(from, new net.minecraft.world.phys.Vec3(target.getX(), y, target.getZ()),
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, viewer)).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	public static boolean healBoosted(LivingEntity target) {
		Long until = healBoostUntil.get(target.getUUID());
		return until != null && now(target) < until;
	}

	public static boolean antiHealed(LivingEntity target) {
		Long until = antiHealUntil.get(target.getUUID());
		return until != null && now(target) < until;
	}

	/**
	 * What Protection Suzu and Life Grip cleanse (#119, techniques/game-data/status_and_cleanse.md). The data's cleanse: Suzu's status 031A
	 * -> graph 180C sets v8440 (#60) and sends itself message 1BC5 (#92); Life Grip does the same (21EE #606, #605). Each status answers one of those
	 * itself, so the list is their readers. H nodes; M that v8440 means "phased". W = Kiriko rev 408632 and Status effect rev 408841. Every negative state in the mod:
	 * <pre>
	 * state                     prevented in the phase                 removed                                  source
	 * Sleep Dart                yes (Sleep.put)                        wakes: its 0.5 s waking, no 0.5 s gate   D 224E #96 / #575; #401 -> #418 -> #204 -> #304
	 * Rally's stun              yes (Sleep.stun)                       at once                                  D 224E #96 / #575; #402 -> #416 -> #297 (its public schema's default type, 4EB6)
	 * Hack: silence, reveal     yes (Hack.creature)                    at once, both                            D 224E #402 (type 4EB6); 0B1F #453 -> #610 -> #611 -> #609
	 * Biotic Grenade anti-heal  yes (antiHeal)                         at once                                  D 0C72's stop {v6604, v8440, v478}, then v8440 -> 1C7D; W
	 * Healing reduction         no hurt, no reduction                  at once                                  D 01C7 #1186; W
	 * Local Anesthetic          yes (Anesthetic)                       at once: damage, slow                    W damage over time, slows; 6123 reads no flag: M
	 * Claws Out                 yes (JetpackCat.wound)                 at once: damage, slow                    W names it; no graph read: M
	 * Hyper Strike's float      yes: not hit                           NOT removed                              Holds.Cleansing.STAYS: 54A5 reads no flag (M); W's list means its damage ("50 over 0.37 s"), W never removes Knockback: ours
	 * Deceleration Field slow   yes: out of the field's filter         at once; back after, if still inside     W slows; D 1443's tracker, mask 20: M
	 * Weaken (Cyberspace)       yes: out of the zone's filter          not by this: its 0.25 s linger runs out  D 6102's tracker, mask 20: M; W silent
	 * Accretion, Catnapper      yes (Sleep.stun): the hit's filter     NOT removed                              D 224E #400 goes nowhere (type 6BBE); W; 1EB9 #338 and 5B7B's filters, mask 20: M
	 * Catnapper's tether        yes (JetpackCat.land skips them)       NOT removed mid-carry                    5B7B's mask 20: M; W Status effect "Catnapper: Cannot be cleansed"
	 * Soundwave's push, lockout yes (Lucio: not hit)                   the lockout goes (Minecraft's Slowness)  W prevented, not removed; the lockout itself is ours (LucioStats)
	 * Gravitic Flux: the lift   no: its filter, mask 16, has no bit 4  NOT removed                              1E41 #1292's filter, M; W "completely unaffected"; no flag read in his graphs: M
	 * Gravitic Flux: its slow   no                                     NOT removed: it ends with the slam       part of the Flux W calls unaffected (HEAVY_SLOW, GraviticFlux); W's "slows" not applied: ours
	 * Minecraft's effects, fire no                                     removed                                  ours
	 * </pre>
	 * The mod has no Discord, Hinder or Groggy. A knock-back is kept off by Combat.push's and launch's own phase test. 224E #575 lets type 6BBE through when v530 and v8440 are set:
	 * what stops a knock-down on the phased is the hit's filter (1EB9 #338, 5B7B; mask 20, M). Every hit passes through the phased, whatever it is: Allies.canHurt and areaHits say no, OwProjectile.canHitEntity for what flies. Flux's lift alone doesn't ask (areaTouches). Sleep, Hack, antiHeal keep their own door for what isn't a hit.
	 * Not done: W Status effect says Life Grip also removes tether and gravity (#124).
	 * <p>
	 * Returns whether a status that says so was cleansed: the one thing Kiriko's Suzu line answers (#121, SuzuEntity.burst). The data's senders of
	 * 1C7D, each gated on v8440: Sleep Dart 224E #418, Rally's stun 224E #416, Hack 0B1F #611, Biotic Grenade 0C72 #275, Claws Out 5BAF #91: H nodes.
	 * Removed and silent: the healing reduction (01C7 #1186 ends without it), Local Anesthetic (6123 has neither v8440 nor 1C7D), the zones' slows
	 * (1443, 6102 trackers). Minecraft's effects and fire: no graph, so nothing says so (ours, following the data; the owner's to change).
	 */
	public static boolean cleanse(LivingEntity target) {
		boolean told = false;
		if (!devCleanseOff) {
			told |= Sleep.cleanse(target);
			told |= dev.overcraft.sombra.Hack.cleanse(target);
			dev.overcraft.ana.Anesthetic.cleanse(target);
			told |= dev.overcraft.jetpackcat.JetpackCat.cleanse(target);
			dev.overcraft.sojourn.Disruptor.cleanse(target);
		}
		List<Holder<net.minecraft.world.effect.MobEffect>> harmful = new ArrayList<>();
		for (MobEffectInstance effect : target.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				harmful.add(effect.getEffect());
			}
		}
		harmful.forEach(target::removeEffect);
		// (W Global Passive Abilities: "cleansable")
		healCutUntil.remove(target.getUUID());
		boolean antiHealEnded = !devFixOff && antiHealUntil.remove(target.getUUID()) != null;
		told |= antiHealEnded;
		if (antiHealEnded && target.level() instanceof net.minecraft.server.level.ServerLevel level) {
			// its pluses go with it (Effects' ENDS: a player who starts seeing them now isn't told either)
			Effects.play(level, "ana_anti_healed_end", target.position(), net.minecraft.world.phys.Vec3.ZERO, target, 0, null);
		}
		target.clearFire();
		target.setTicksFrozen(0);
		return told;
	}

	public static void phase(LivingEntity target, double seconds) {
		long until = now(target) + Ow.ticks(seconds);
		phasedUntil.merge(target.getUUID(), until, Math::max);
	}

	/**
	 * Whether the phase keeps a new negative state off it now (Combat.cleanse's table; off with the dev switch). Every phase here, not only Suzu's. Suzu: 180C. Life Grip: 21EE #606.
	 * Resurrect (Souls, 2.25 s): D 00CA #677 sets the same v8440. Translocator (Sombra, 0.4 s): D 0C19 #330 too. Swift Step (Kiriko, 0.25 s): no v8440 in her graphs read but the shared 1065; W "phased"; W Status effect
	 * "its attack ignoring property prevents most negative effects": ours that it's this same list. None of those three calls cleanse: W "No longer cleanses on use" for Swift Step; the data's readers would end a status on the other two, not done, ours.
	 */
	public static boolean phaseStops(LivingEntity target) {
		return phaseStopsRule(target.level().isClientSide(), devCleanseOff, !target.level().isClientSide() && isPhased(target));
	}

	/**
	 * Only the server knows: these maps are per side, and a remote client's is empty. A client-side caller gets "not phased". Shots don't ask there: OwProjectile.canHitEntity returns first on the client. Package-private for PhaseRulesTest.
	 */
	static boolean phaseStopsRule(boolean clientSide, boolean devOff, boolean phased) {
		return !clientSide && !devOff && phased;
	}

	/**
	 * Whether the phase refuses this damage. Not what beats Minecraft's own invulnerability (the bypasses_invulnerability tag: /kill, the void): ours, or a phased hero read "Killed" and stood there,
	 * and fell through the void until the phase was out. A bed's blast isn't in it, so #27's stays refused. Life Grip's and Resurrect's the same.
	 */
	public static boolean refusesDamage(LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
		return refusesRule(isPhased(target), source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY));
	}

	/** Package-private for PhaseRulesTest. */
	static boolean refusesRule(boolean phased, boolean bypassesInvulnerability) {
		return phased && !bypassesInvulnerability;
	}

	public static boolean isPhased(LivingEntity target) {
		// Life Grip's pull protects its ally the same way: no damage, no knockback (W)
		if (dev.overcraft.lifeweaver.LifeGrip.gripped(target)) {
			return true;
		}
		Long until = phasedUntil.get(target.getUUID());
		return until != null && now(target) < until;
	}

	/** Out of a Kitsune Rush at once (hers, when she leaves play: #100); the tick takes its buffs off. */
	public static void endRush(LivingEntity target) {
		rushedUntil.remove(target.getUUID());
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

	/** Points toward On Fire for damage dealt (Overwatch HP) to a victim, worth 100 for all its health; a bystander none (Allies). */
	public static void addFireForDamage(Player hero, double dealt, LivingEntity victim) {
		double max = Ow.toOw(victim.getMaxHealth());
		if (max > 0 && !Allies.isBystander(hero, victim)) {
			addFire(hero, dealt / max * 100);
		}
	}

	/** Points toward On Fire for healing done (Overwatch HP). */
	public static void addFireForHealing(Player hero, double healed) {
		dev.overcraft.potg.PlayScore.heal(hero, healed);
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

	/**
	 * The same, done to {@code target}: an enemy tank (a player in hero mode as a tank, not on the earner's side) gives 40%
	 * less of both (the Tank role: Tank.ultFactor). Healing a tank ally counts in full. A bystander (a cow, an armor stand:
	 * Allies) gives nothing, or a chicken farm would fill an ult (#99, ours).
	 */
	public static void credit(Player hero, double points, @org.jspecify.annotations.Nullable Entity target) {
		if (target != null && Allies.isBystander(hero, target)) {
			return;
		}
		credit(hero, points * Tank.ultFactor(hero, target));
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
		double cost = ultCost(state);
		boolean wasReady = state.ultCharge >= cost;
		if (tactician(state.hero) && !passive) {
			double below = Math.max(0, Math.min(points, cost - state.ultCharge));
			double above = (points - below) * dev.overcraft.lucio.LucioStats.TACTICIAN_RATE;
			state.ultCharge = Math.min(cost * dev.overcraft.lucio.LucioStats.TACTICIAN_CAP, state.ultCharge + below + above);
		} else if (state.ultCharge < cost) {
			state.ultCharge = Math.min(cost, state.ultCharge + points);
		}
		// no line when it fills: Overwatch's "ult ready" lines are the communication wheel's (stimulus 02E7, category 31,
		// with Thanks and the pings), said when the player asks (Comms); said on filling, it came right after the kill
		// that filled it
	}

	/**
	 * Ult charge for completing a Minecraft advancement, as a share of the hero's ult cost (owner's rule; Overwatch
	 * has nothing like it, so the shares are ours): task 10%, goal 25%, challenge 50%. Recipe unlocks have no display
	 * and don't count.
	 */
	public static void onAdvancement(Player player, AdvancementType type) {
		HeroState state = HeroManager.state(player);
		if (state == null || state.hero == null) {
			return;
		}
		double share = switch (type) {
			case TASK -> 0.10;
			case GOAL -> 0.25;
			case CHALLENGE -> 0.50;
		};
		addUltCharge(player, share * ultCost(state));
	}

	/** Overwatch's Tactician sub-role (Lúcio): ult charge kept past 100% and after casting. */
	public static boolean tactician(Hero hero) {
		return hero.subRole.equals("tactician");
	}

	/** What's left of the ult charge after casting: nothing, or a Tactician's excess. */
	public static double afterUlt(Hero hero, double charge) {
		return tactician(hero) ? Math.max(0, charge - ultCost(hero)) : 0;
	}

	/** The ult cost now: the hero's, or for D.Va her body's (Self-Destruct 1700 in the mech, Call Mech 280 as the pilot). */
	public static int ultCost(HeroState state) {
		return state.hero == Hero.DVA ? dev.overcraft.dva.Dva.ultCost(state) : state.hero == null ? 1 : ultCost(state.hero);
	}

	public static int ultCost(Hero hero) {
		return switch (hero) {
			case KIRIKO -> dev.overcraft.kiriko.KirikoStats.ULT_COST;
			case LUCIO -> dev.overcraft.lucio.LucioStats.ULT_COST;
			case JETPACK_CAT -> dev.overcraft.jetpackcat.JetpackCatStats.ULT_COST;
			case MERCY -> dev.overcraft.mercy.MercyStats.ULT_COST;
			case LIFEWEAVER -> dev.overcraft.lifeweaver.LifeweaverStats.ULT_COST;
			// (missing until Nano Boost: her charge stopped at 1 point)
			case ANA -> (int) dev.overcraft.ana.AnaStats.ULT_COST;
			case SOMBRA -> (int) dev.overcraft.sombra.SombraStats.ULT_COST;
			case BRIGITTE -> (int) dev.overcraft.brigitte.BrigitteStats.ULT_COST;
			case JUNO -> (int) dev.overcraft.juno.JunoStats.ULT_COST;
			case SIGMA -> (int) dev.overcraft.sigma.SigmaStats.ULT_COST;
			case SOJOURN -> (int) dev.overcraft.sojourn.SojournStats.ULT_COST;
			case PHARAH -> (int) dev.overcraft.pharah.PharahStats.ULT_COST;
			case TRACER -> (int) dev.overcraft.tracer.TracerStats.ULT_COST;
			case DVA -> (int) dev.overcraft.dva.DvaStats.MECH_ULT_COST;
			default -> 1;
		};
	}

	/**
	 * Overhealth given (Sound Barrier), decaying: what's left of it (Minecraft health), the decay a tick from holdUntil
	 * (Tree of Life's and Sow the Seed's hold a while first), the end.
	 */
	private record Overhealth(LivingEntity entity, double perTick, long until, long holdUntil) {
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
			max.addOrUpdateTransientModifier(new AttributeModifier(OVERHEALTH, amount, AttributeModifier.Operation.ADD_VALUE));
		}
		entity.setAbsorptionAmount(entity.getAbsorptionAmount() + (float) amount);
		overhealth.put(entity.getUUID(), new Overhealth(entity, amount / Ow.ticks(seconds), now(entity) + Ow.ticks(seconds), now(entity)));
		overhealthLeft.put(entity.getUUID(), amount);
	}

	/**
	 * Adds overhealth to a pool (Overwatch points) that holds {@code hold} s, then goes {@code decay} a second: Tree of
	 * Life's (each pulse adds to it, up to its max, and starts the hold again) and Sow the Seed's. What's left of an
	 * earlier pool joins it (one pool an entity: ours).
	 */
	public static void overhealthPool(LivingEntity entity, double owAdd, double owMax, double hold, double decay) {
		double left = Math.min(overhealthLeft.getOrDefault(entity.getUUID(), 0.0), entity.getAbsorptionAmount());
		double amount = Math.min(Math.max(Ow.hp(owMax), left), left + Ow.hp(owAdd));
		if (amount <= 0) {
			return;
		}
		clearOverhealth(entity);
		AttributeInstance max = entity.getAttribute(Attributes.MAX_ABSORPTION);
		if (max != null) {
			max.addOrUpdateTransientModifier(new AttributeModifier(OVERHEALTH, amount, AttributeModifier.Operation.ADD_VALUE));
		}
		entity.setAbsorptionAmount(entity.getAbsorptionAmount() + (float) amount);
		long holdUntil = now(entity) + Ow.ticks(hold);
		double perTick = Ow.hp(decay) / 20;
		overhealth.put(entity.getUUID(), new Overhealth(entity, perTick, holdUntil + (long) Math.ceil(amount / perTick) + 1, holdUntil));
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
			if (now(o.entity) < o.holdUntil) {
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
			healCutUntil.values().removeIf(until -> until < time);
			lastPushes.values().removeIf(last -> time - last.at > CREDIT_TICKS);
		}
	}

	private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(id);
		if (amount == 0) {
			instance.removeModifier(id);
		} else if (current == null || current.amount() != amount) {
			// a changed amount replaces it (Local Anesthetic's slow eases from 40% to 20%: it stayed at 40% when only
			// a missing one was added)
			instance.removeModifier(id);
			instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	/** {@link #animate} for everyone but the hero, whose own client has already played it (Sojourn's Charged Shot: ClientSojourn). */
	public static void animateOthers(ServerPlayer hero, String clip) {
		dev.overcraft.comms.Comms.animated(hero, clip);
		HeroActionPayload payload = new HeroActionPayload(hero.getId(), clip);
		for (ServerPlayer watcher : PlayerLookup.tracking(hero)) {
			if (watcher != hero) {
				ServerPlayNetworking.send(watcher, payload);
			}
		}
	}

	/** Tells the hero and everyone who can see them to play an animation (a throw, a cast). */
	public static void animate(ServerPlayer hero, String clip) {
		dev.overcraft.comms.Comms.animated(hero, clip);
		HeroActionPayload payload = new HeroActionPayload(hero.getId(), clip);
		ServerPlayNetworking.send(hero, payload);
		for (ServerPlayer watcher : PlayerLookup.tracking(hero)) {
			ServerPlayNetworking.send(watcher, payload);
		}
	}

	public static void clear() {
		phasedUntil.clear();
		healCutUntil.clear();
		healBoostUntil.clear();
		antiHealUntil.clear();
		rushedUntil.clear();
		rushedEntities.clear();
		buffs.clear();
		overhealth.clear();
		overhealthLeft.clear();
		lastPushes.clear();
		dealing = null;
	}
}

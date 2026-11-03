package dev.overcraft.brigitte;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.brigitte.BrigitteStats.*;

/**
 * Barrier Shield (0EE3 on her, the barrier pet 2284 running 0EE4): held up on the right button (the data's default), it
 * stands in front of her and takes what would cross it: hits on her or an ally behind it from in front, arrows, blasts,
 * but not melee, which goes through as in Overwatch (the owner, 2026-11-02: "Melee goes through"). 300 health; down, it comes back at 85 a second
 * after 2 s; broken, 5 s before it can go up again. Up, she's 30% slower and her flail is still (0EE3 #670). Its size is the
 * deployed shield's (1.51 m x 2.64 m, measured on the mesh 1064F's idle_up: no number in any graph), from the ground up,
 * 0.9 m ahead of her, square to her facing (ours). Here it's her state, not an entity.
 */
public final class Barrier {
	private static final Identifier SLOW = dev.overcraft.Overcraft.id("brigitte_barrier");
	/** A hit was taken by a barrier since this was last cleared (Sojourn's Railgun: +1 energy on a barrier, not +5). */
	public static boolean took;

	private Barrier() {
	}

	/** Each tick of her kit: up while the right button is held (and not broken), regenerating while down. */
	public static void tick(ServerPlayer player, HeroState s, boolean controls) {
		long now = player.level().getGameTime();
		s.barrierLocked = now < s.barrierBrokenUntil;
		boolean want = controls && player.isAlive() && s.secondaryHeld && now >= s.barrierBrokenUntil && s.barrierHealth > 0;
		if (want && !s.barrierUp) {
			s.barrierUp = true;
			dev.overcraft.hero.Combat.animate(player, "shield_raise");
			boolean rally = s.rallyTicks > 0;
			// in Rally its own raise and loop (6F8D, 2.083 s) replace the plain one's
			BrigitteSounds.split(player, rally ? "barrier.rally_raise" : "barrier.raise", player.getEyePosition(), rally ? 75 : 40);
			BrigitteSounds.split(player, rally ? "barrier.rally.loop" : "barrier.hold.loop", player.getEyePosition(), rally ? 75 : 40);
			s.barrierLowCalled = s.barrierHealth / maxHealth(s) <= 0.25 ? 2 : s.barrierHealth / maxHealth(s) <= 0.5 ? 1 : 0;
			dev.overcraft.hero.Voice.say(player, "barrier_raise", 1);
		} else if (!want && s.barrierUp && s.bashTicks < 0) {
			s.barrierUp = false;
			s.barrierDownSince = now;
			dev.overcraft.hero.Combat.animate(player, "shield_lower");
			lowered(player, s);
		}
		// (broken too: it refills through its 5 s lockout, from 2 s after: 0EE4 #208 has no other condition)
		if (!s.barrierUp && now - s.barrierDownSince >= Ow.ticks(BARRIER_REGEN_DELAY)) {
			s.barrierHealth = Math.min(maxHealth(s), s.barrierHealth + BARRIER_REGEN / 20.0);
		}
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			if (!s.barrierUp) {
				speed.removeModifier(SLOW);
			} else if (!speed.hasModifier(SLOW)) {
				speed.addTransientModifier(new AttributeModifier(SLOW, -BARRIER_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
	}

	/** Its lower sound, its loops stopped. */
	private static void lowered(ServerPlayer player, HeroState s) {
		stopLoops(player);
		BrigitteSounds.split(player, s.rallyTicks > 0 ? "barrier.rally_lower" : "barrier.lower", player.getEyePosition(),
			s.rallyTicks > 0 ? 75 : 40);
	}

	private static void stopLoops(ServerPlayer player) {
		BrigitteSounds.stop(player.level(), "barrier.hold.loop", "barrier.hold.loop_3p", "barrier.rally.loop", "barrier.rally.loop_3p");
	}

	public static double maxHealth(HeroState s) {
		return s.rallyTicks > 0 ? BARRIER_HEALTH_RALLY : BARRIER_HEALTH;
	}

	/**
	 * Leaving her (another hero, dying): down, its slow off, and everyone told (her stance and the field are drawn from
	 * the raise and lower they heard: without the lower she respawned holding it, #88).
	 */
	public static void clear(ServerPlayer player, HeroState s) {
		if (s.barrierUp) {
			stopLoops(player);
			dev.overcraft.hero.Combat.animate(player, "shield_lower");
			s.barrierDownSince = player.level().getGameTime();
		}
		s.barrierUp = false;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SLOW);
		}
	}

	/**
	 * The explosion being dealt out now (ServerExplosionMixin, from the head of its hurtEntities to its end): its centre,
	 * the explosion itself, and who set it off. Saved and restored around a blast set off inside another (end crystals).
	 */
	public static @Nullable Vec3 blast;
	public static @Nullable Object blastKey;
	public static @Nullable Entity blastCause;
	/** Her flail's swing is being dealt (Brigitte.swingHit): melee, which goes through barriers. */
	public static boolean meleeHit;

	/** The lowest a way may cross her barrier, from her feet (m, ours: below her feet ahead is the ground of a step or slope). */
	private static final double BOTTOM = -1.0;

	/**
	 * Before an explosion hurts anyone (ServerExplosionMixin): each Brigitte's up barrier with the blast's centre in front
	 * of it takes what an entity at its nearest point would (ServerExplosion.hurtEntities' rule at full exposure: impact
	 * 1 - d / 2r, (impact² + impact) / 2 x 7 x 2r + 1), once, whoever stands behind it. Charged from whoever the explosion
	 * happened to hurt first, a villager 8 m behind her cost it little and she a lot (#88's critique). Sigma's barriers
	 * (feature/sigma's Barriers.blast) take blasts the same way. {@code hurts}: whether this explosion hurts at all (a
	 * breeze's wind charge doesn't).
	 */
	public static void blast(ServerLevel level, Vec3 centre, float radius, @Nullable Entity cause, boolean hurts) {
		double reach = radius * 2;
		for (ServerPlayer brig : level.players()) {
			HeroState s = HeroManager.state(brig);
			if (s == null || s.hero != Hero.BRIGITTE || !s.barrierUp || !stops(brig, cause)) {
				continue;
			}
			Vec3 nearest = nearestFront(brig, s, centre);
			if (nearest == null) {
				continue;
			}
			// it stops this blast for all behind it, even if the blast breaks it
			s.barrierBlast = blastKey;
			double d = nearest.distanceTo(centre);
			if (!hurts || d >= reach) {
				continue;
			}
			double impact = 1 - d / reach;
			double mc = (impact * impact + impact) / 2 * 7 * reach + 1;
			take(level, brig, s, Ow.toOw(mc), nearest);
		}
	}

	/**
	 * Each server tick: a blast-carrying projectile (a ghast's fireball, a wither skull, a breeze's or a player's wind
	 * charge; a blaze's small fireball) about to pass through a Brigitte's up barrier from the front meets it there and
	 * goes off on it, as on a wall. Minecraft lets it fly through to burst behind her: a breeze's wind charge that missed
	 * her went off at her back and threw her 10 m up and forward over her barrier (#88's rework, found playing). A small
	 * fireball has no blast: the barrier takes its 5 and it's gone (BLAZE_FIREBALL, ours from its damage).
	 */
	public static void tickProjectiles(net.minecraft.server.MinecraftServer server) {
		for (ServerPlayer brig : server.getPlayerList().getPlayers()) {
			HeroState s = HeroManager.state(brig);
			if (s == null || s.hero != Hero.BRIGITTE || !s.barrierUp || !(brig.level() instanceof ServerLevel level)) {
				continue;
			}
			Vec3 facing = facing(brig);
			for (var p : level.getEntitiesOfClass(net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile.class,
				brig.getBoundingBox().inflate(8), e -> e.isAlive() && stops(brig, e.getOwner()))) {
				Vec3 from = p.position(), to = from.add(p.getDeltaMovement());
				Vec3 at = meets(brig, s, from, to);
				if (at == null) {
					continue;
				}
				Vec3 hit = at.add(facing.scale(0.05));
				p.setPos(hit.add(facing.scale(0.3)));
				if (p instanceof net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball) {
					take(level, brig, s, Ow.toOw(BLAZE_FIREBALL), hit);
					p.discard();
					continue;
				}
				net.minecraft.core.Direction face = net.minecraft.core.Direction.getApproximateNearest(facing.x, facing.y, facing.z);
				((dev.overcraft.mixin.ProjectileInvoker) p).overcraft$onHit(new net.minecraft.world.phys.BlockHitResult(hit.add(facing.scale(0.3)),
					face, net.minecraft.core.BlockPos.containing(hit), false));
				if (p.isAlive()) {
					p.discard();
				}
			}
		}
	}

	/** A blaze's small fireball (Minecraft health: SmallFireball.onHitEntity's 5). */
	private static final float BLAZE_FIREBALL = 5;

	/** Where the step from {@code a} to {@code b} goes through her barrier from the front, or null. */
	private static @Nullable Vec3 meets(ServerPlayer brig, HeroState s, Vec3 a, Vec3 b) {
		Vec3 facing = facing(brig);
		Vec3 side = new Vec3(facing.z, 0, -facing.x);
		Vec3 centre = brig.position().add(facing.scale(BARRIER_AHEAD));
		boolean rally = s.rallyTicks > 0;
		double halfWidth = (rally ? BARRIER_WIDTH_RALLY : BARRIER_WIDTH) / 2, height = rally ? BARRIER_HEIGHT_RALLY : BARRIER_HEIGHT;
		double da = a.subtract(centre).dot(facing), db = b.subtract(centre).dot(facing);
		if (da <= 0 || db > 0) {
			return null;
		}
		Vec3 at = a.add(b.subtract(a).scale(da / (da - db)));
		double up = at.y - brig.getY();
		return Math.abs(at.subtract(centre).dot(side)) <= halfWidth && up >= BOTTOM && up <= height ? at : null;
	}

	/** Whether her barrier stops what {@code cause} sets off: not hers, not her ally's (Overwatch's stop only enemies). */
	private static boolean stops(ServerPlayer brig, @Nullable Entity cause) {
		return cause != brig && !(cause instanceof LivingEntity living && Allies.isSelfOrAlly(brig, living));
	}

	/**
	 * Every hit passes here (LivingEntityMixin, before armor): what's left of {@code damage} (Minecraft health) after any
	 * Brigitte's barrier that stands between the hit's source and {@code victim} took it. The victim is her or her ally.
	 * A hit the barrier takes stops there, all of it, even one bigger than the barrier has left (it breaks): Overwatch's
	 * barriers block a hit, they don't pass on what's over (the wiki's Barrier page: of Rip-Tire and Total Mayhem, "barriers
	 * block the explosion"; no source gives a carry-over: M). Ours had passed the rest on as a hit of its own, so a creeper
	 * 1.5 m ahead broke it and still hurt her and threw her 5 m back (#88). A blast's hit behind it is 0: the barrier took
	 * the blast itself (blast). Melee goes through (the owner, 2026-11-02: "Melee goes through"; the wiki's Barrier page).
	 */
	public static float absorb(LivingEntity victim, DamageSource source, float damage) {
		if (damage <= 0 || !(victim.level() instanceof ServerLevel level)) {
			return damage;
		}
		ServerPlayer brig = covering(victim, source);
		if (brig == null) {
			return damage;
		}
		if (blast != null) {
			return 0;
		}
		HeroState s = HeroManager.state(brig);
		Vec3 from = from(source, victim);
		Vec3 to = victim.getBoundingBox().getCenter();
		take(level, brig, s, Ow.toOw(damage), from == null ? to : to.add(from.subtract(to).normalize().scale(1)));
		return 0;
	}

	/** {@code ow} on her barrier at {@code at}: its sounds, calls, ult charge, and its break. */
	private static void take(ServerLevel level, ServerPlayer brig, HeroState s, double ow, Vec3 at) {
		double took = Math.min(ow, s.barrierHealth);
		s.barrierHealth -= took;
		Barrier.took = true;
		double after = s.barrierHealth / maxHealth(s);
		// a hit on it (her flail's barrier contact 0015, M: anything hitting her barrier), and its calls
		BrigitteSounds.at(level, "hit_barrier", at, 100);
		dev.overcraft.hero.Voice.say(brig, "barrier_damaged", 1);
		if (after > 0 && (after <= 0.5 && s.barrierLowCalled < 1 || after <= 0.25 && s.barrierLowCalled < 2)) {
			// below 50 %, then 25 % (0540)
			s.barrierLowCalled = after <= 0.25 ? 2 : 1;
			BrigitteSounds.split(brig, "barrier.low", brig.getEyePosition(), 40);
			dev.overcraft.hero.Voice.say(brig, "barrier_low", 1);
		}
		// 2% of what it absorbs is charge for her ult (0EE4 #149, M)
		dev.overcraft.hero.Combat.addUltCharge(brig, took * BARRIER_ULT_SHARE, false);
		if (s.barrierHealth <= 0) {
			s.barrierHealth = 0;
			s.barrierBrokenUntil = level.getGameTime() + Ow.ticks(BARRIER_BROKEN);
			s.barrierUp = false;
			s.barrierDownSince = level.getGameTime();
			dev.overcraft.hero.Combat.animate(brig, "shield_lower");
			stopLoops(brig);
			BrigitteSounds.split(brig, "barrier.break", brig.getEyePosition(), 40);
			dev.overcraft.hero.Voice.say(brig, "barrier_broken", 1);
			// 268C at the barrier: its outline's flash, hexes and shards
			Vec3 facing = facing(brig);
			dev.overcraft.hero.Effects.play(level, "brigitte_barrier_break", brig.position().add(facing.scale(BARRIER_AHEAD)).add(0, 1.2, 0),
				facing, brig, s.rallyTicks > 0 ? 1 : 0, brig);
		}
	}

	/**
	 * The Brigitte whose barrier stands between the hit's source and {@code victim} (her or her ally), or null: up, or
	 * already struck by the blast being dealt. Asked before armor too (LivingEntityMixin): her armor takes 7 off a hit on
	 * her, not off a hit on her barrier (it took 30.5 of a zombie's 37.5: #88). Melee goes through.
	 */
	public static @Nullable ServerPlayer covering(LivingEntity victim, DamageSource source) {
		if (!(victim.level() instanceof ServerLevel level)) {
			return null;
		}
		if (blast != null) {
			return blocking(victim, blast, blastCause);
		}
		if (isMelee(source)) {
			return null;
		}
		Vec3 from = from(source, victim);
		if (from == null) {
			return null;
		}
		Vec3 to = victim.getBoundingBox().getCenter();
		for (ServerPlayer brig : level.players()) {
			HeroState s = HeroManager.state(brig);
			if (s != null && s.hero == Hero.BRIGITTE && s.barrierUp && brig.distanceToSqr(victim) <= 100
				&& Allies.isSelfOrAlly(brig, victim) && source.getEntity() != brig && crosses(brig, s, from, to)) {
				return brig;
			}
		}
		return null;
	}

	/**
	 * Whether a Brigitte's barrier stands between a blast's centre and {@code entity}, her or her ally: no damage and no
	 * push behind it (ServerExplosionMixin). By where things are only, so a breeze's wind charge, which hurts no one,
	 * doesn't throw her through it either (#88's critique). feature/sigma calls it with the same arguments.
	 */
	public static boolean blocksBlast(Entity entity, Vec3 centre, @Nullable Entity cause) {
		return entity instanceof LivingEntity victim && blocking(victim, centre, cause) != null;
	}

	private static @Nullable ServerPlayer blocking(LivingEntity victim, Vec3 centre, @Nullable Entity cause) {
		if (!(victim.level() instanceof ServerLevel level)) {
			return null;
		}
		Vec3 to = victim.getBoundingBox().getCenter();
		for (ServerPlayer brig : level.players()) {
			HeroState s = HeroManager.state(brig);
			if (s != null && s.hero == Hero.BRIGITTE && (s.barrierUp || blastKey != null && s.barrierBlast == blastKey)
				&& brig.distanceToSqr(victim) <= 100 && Allies.isSelfOrAlly(brig, victim) && stops(brig, cause)
				&& crosses(brig, s, centre, to)) {
				return brig;
			}
		}
		return null;
	}

	/**
	 * Melee: a mob's or a player's own blow (the hit's direct entity is the attacker, not a projectile), Minecraft's melee
	 * types, or her flail's swing. Overwatch's barriers let every melee attack through (the wiki's Barrier page), and the
	 * owner chose it (2026-11-02: "Melee goes through"); ours had stopped zombies at it.
	 */
	public static boolean isMelee(DamageSource source) {
		if (meleeHit || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)
			|| source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK_NO_AGGRO)
			|| source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
			return true;
		}
		Entity direct = source.getDirectEntity();
		return direct != null && direct == source.getEntity() && direct instanceof LivingEntity
			&& !(direct instanceof net.minecraft.world.entity.projectile.Projectile);
	}

	/** Where a hit came from: a projectile's way in, a blast's centre, an attacker's eyes. */
	private static @Nullable Vec3 from(DamageSource source, LivingEntity victim) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof net.minecraft.world.entity.projectile.Projectile p) {
			Vec3 v = p.getDeltaMovement();
			return v.lengthSqr() < 1.0E-6 ? p.position() : p.position().subtract(v.normalize().scale(6));
		}
		// a blast not dealt by an explosion (`/damage ... explosion at`; real explosions set `blast`): its centre. A primed
		// TNT is the hit's direct entity with no attacker, and went through the barrier while only direct-less hits counted
		// as blasts (found playing)
		if (source.getSourcePosition() != null && !(direct instanceof LivingEntity)) {
			return source.getSourcePosition();
		}
		Entity attacker = source.getEntity();
		return attacker != null && attacker != victim ? attacker.getEyePosition() : null;
	}

	private static Vec3 facing(ServerPlayer brig) {
		float yaw = brig.getYRot() * (float) (Math.PI / 180);
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	/**
	 * The point of her barrier nearest {@code at} when {@code at} is in front of it, or against it (between her and it,
	 * within its width: crosses); else null.
	 */
	private static @Nullable Vec3 nearestFront(ServerPlayer brig, HeroState s, Vec3 at) {
		Vec3 facing = facing(brig);
		Vec3 side = new Vec3(facing.z, 0, -facing.x);
		Vec3 centre = brig.position().add(facing.scale(BARRIER_AHEAD));
		boolean rally = s.rallyTicks > 0;
		double halfWidth = (rally ? BARRIER_WIDTH_RALLY : BARRIER_WIDTH) / 2, height = rally ? BARRIER_HEIGHT_RALLY : BARRIER_HEIGHT;
		double da = at.subtract(centre).dot(facing);
		double lateral = at.subtract(centre).dot(side);
		if (da <= 0 && (at.subtract(brig.position()).dot(facing) <= 0 || Math.abs(lateral) > halfWidth)) {
			return null;
		}
		double across = Math.clamp(lateral, -halfWidth, halfWidth);
		double up = Math.clamp(at.y - brig.getY(), BOTTOM, height);
		return centre.add(side.scale(across)).add(0, up, 0);
	}

	/**
	 * Whether the way from {@code a} to {@code b} crosses her barrier's rectangle (square to her facing, BARRIER_AHEAD
	 * ahead, from BOTTOM under her feet to its height), or starts against it: a source in front of her but nearer than the
	 * barrier, within its width, counts as at the barrier for {@code b} behind the barrier within its width too. In
	 * Overwatch there's no room between her and the shield she holds; Minecraft lets a creeper walk through it up to her
	 * chest and go off there (ours, #88). Not for an ally off to her side: the blast's way to them never meets it.
	 */
	static boolean crosses(ServerPlayer brig, HeroState s, Vec3 a, Vec3 b) {
		Vec3 facing = facing(brig);
		Vec3 side = new Vec3(facing.z, 0, -facing.x);
		Vec3 centre = brig.position().add(facing.scale(BARRIER_AHEAD));
		boolean rally = s.rallyTicks > 0;
		double halfWidth = (rally ? BARRIER_WIDTH_RALLY : BARRIER_WIDTH) / 2, height = rally ? BARRIER_HEIGHT_RALLY : BARRIER_HEIGHT;
		double da = a.subtract(centre).dot(facing), db = b.subtract(centre).dot(facing);
		if (db > 0) {
			// what it would hit is in front of it
			return false;
		}
		Vec3 at;
		if (da > 0) {
			// from in front of it to behind it: where the way meets its plane
			at = a.add(b.subtract(a).scale(da / (da - db)));
		} else if (a.subtract(brig.position()).dot(facing) > 0 && Math.abs(b.subtract(centre).dot(side)) <= halfWidth) {
			// between her and it (a creeper pressed to her chest, a fireball going off on her front), hitting what's behind
			// it within its width: against it
			at = a;
		} else {
			return false;
		}
		double across = Math.abs(at.subtract(centre).dot(side));
		double up = at.y - brig.getY();
		return across <= halfWidth && up >= BOTTOM && up <= height;
	}
}

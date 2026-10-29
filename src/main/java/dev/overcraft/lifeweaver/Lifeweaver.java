package dev.overcraft.lifeweaver;

import dev.overcraft.Ow;
import dev.overcraft.entity.BlossomEntity;
import dev.overcraft.entity.PetalPlatformEntity;
import dev.overcraft.entity.PetalThrowEntity;
import dev.overcraft.entity.ThornEntity;
import dev.overcraft.entity.TreeOfLifeEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.lifeweaver.LifeweaverStats.*;

/**
 * Lifeweaver: his kit on the server, from his game data (E:\Overwatch Archive\research\lifeweaver\lifeweaver-map.md) and
 * the public numbers (tools/reference/lifeweaver.md; plan: tools/reference/lifeweaver-plan.md). Overwatch's layout: the
 * left button charges Healing Blossom and throws it on release (with Thorn Volley out, it swaps back and charges); the
 * right fires Thorn Volley (with the Blossom out, it swaps and fires); 1 / 2 or the scroll wheel pick the weapon; R
 * reloads both. Shift: Petal Platform (again: destroy it). E: Life Grip. Space in the air: Rejuvenating Dash (his client
 * moves him: OwMovement). Q: Tree of Life (aim; the left button places it, the right cancels; Q again later fells it).
 * V: quick melee, or with Sow the Seed and the Blossom out, the seed.
 */
public final class Lifeweaver implements HeroKit {
	/** The synced state's flags (HeroManager.LIFEWEAVER [0], and the sync's mode). */
	public static final int FLAG_THORNS = 1, FLAG_CHARGING = 2, FLAG_SWAP = 4, FLAG_TREE_AIM = 8, FLAG_TREE_CAST = 16,
		FLAG_TREE = 32, FLAG_PLATFORM = 64, FLAG_DASH = 128, FLAG_GRIP = 256, FLAG_FIRING = 512, FLAG_SEED = 1024;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = BLOSSOM_AMMO;
		s.secondaryAmmo = THORN_AMMO;
		s.primaryReload = s.secondaryReload = -1;
		s.thornsOut = false;
		s.charging = false;
		s.bloomCharge = 0;
		s.bloomTarget = null;
		s.treeAiming = false;
		s.treeCast = -1;
		s.gripCast = -1;
		s.dashTicks = -1;
		publish(player, s);
	}

	private static boolean has(ServerPlayer player, Perk perk) {
		return HeroManager.hasPerk(player, perk);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		double cooldown = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - cooldown);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - cooldown);
		s.ability3Cooldown = Math.max(0, s.ability3Cooldown - cooldown);
		s.seedCooldown = Math.max(0, s.seedCooldown - cooldown);
		s.abilityLock = Math.max(0, s.abilityLock - 1);
		if (s.swapTicks > 0) {
			s.swapTicks--;
		}
		s.bloomRecovery = Math.max(0, s.bloomRecovery - attack);
		// the weapon not in hand refills itself after 2 s (BZ 2025-11-11)
		if (++s.holsterTicks >= Ow.ticks(HOLSTER_RELOAD)) {
			if (s.thornsOut) {
				s.primaryAmmo = BLOSSOM_AMMO;
				s.primaryReload = -1;
			} else {
				s.secondaryAmmo = THORN_AMMO;
				s.secondaryReload = -1;
			}
		}
		// the charge fills by itself, slower, whenever it isn't being held (in hand or put away: D 1C5A)
		if (!(s.charging && s.primaryHeld && !s.thornsOut && controls)) {
			s.bloomCharge = Math.min(1, s.bloomCharge + 1 / (CHARGE_TIME * CHARGE_PASSIVE * 20));
		}
		s.airTicks = player.onGround() ? 0 : s.airTicks + 1;
		reloads(player, s, attack);
		if (!controls || !player.isAlive()) {
			// out of play (dead, Minecraft mode): what he was doing ends; the platform and the tree go on by themselves
			s.charging = false;
			s.bloomTarget = null;
			s.treeAiming = false;
			s.gripCast = -1;
			s.dashTicks = -1;
			s.platformThrow = -1;
			s.primaryWas = s.secondaryWas = false;
			publish(player, s);
			return;
		}
		platformThrow(player, s);
		lifeGrip(player, s);
		dash(player, s);
		tree(player, s);
		boolean primaryPressed = s.primaryHeld && !s.primaryWas;
		boolean secondaryPressed = s.secondaryHeld && !s.secondaryWas;
		boolean primaryReleased = !s.primaryHeld && s.primaryWas;
		s.primaryWas = s.primaryHeld;
		s.secondaryWas = s.secondaryHeld;
		if (s.treeAiming) {
			// aiming the tree: the left button places it, the right cancels (D 36A4's defaults); the weapons are quiet
			if (primaryPressed) {
				confirmTree(player, s);
			} else if (secondaryPressed) {
				s.treeAiming = false;
				Effects.play(player.level(), "lifeweaver_tree_cancel", player.position(), player);
			}
			s.bloomTarget = null;
		} else if (s.treeCast < 0 && s.gripCast < 0) {
			weapons(player, s, attack, primaryReleased);
		}
		publish(player, s);
	}

	// ---- the weapons ----

	/** Which weapon is in hand, by the fire buttons: the left wants the Blossom, the right Thorn Volley (D 21C6 #1277 / #1279). */
	private static void weapons(ServerPlayer player, HeroState s, double attack, boolean primaryReleased) {
		if (s.thornsOut && s.primaryHeld && !s.secondaryHeld) {
			swap(player, s, false);
		} else if (!s.thornsOut && s.secondaryHeld && !s.primaryHeld && !s.charging) {
			swap(player, s, true);
		} else if (!s.thornsOut && s.secondaryHeld && s.charging && !s.primaryHeld) {
			// the right button while a blossom is drawn: swapping keeps the charge (W)
			swap(player, s, true);
		}
		if (s.thornsOut) {
			thorns(player, s, attack);
		} else {
			blossom(player, s, attack, primaryReleased);
		}
	}

	/** Weapon keys (Overwatch's 1 / 2, the scroll wheel), and Rejuvenating Dash's Jump in the air. */
	public static void extra(ServerPlayer player, HeroState s, int extra) {
		if (!HeroManager.controls(player)) {
			return;
		}
		boolean want = s.thornsOut;
		if ((extra & HeroInputPayload.EXTRA_STAFF) != 0) {
			want = false;
		} else if ((extra & HeroInputPayload.EXTRA_BLASTER) != 0) {
			want = true;
		} else if ((extra & HeroInputPayload.EXTRA_SWAP) != 0) {
			want = !s.thornsOut;
		}
		if (want != s.thornsOut && s.treeCast < 0 && s.gripCast < 0) {
			swap(player, s, want);
		}
		if ((extra & HeroInputPayload.EXTRA_JUMP) != 0) {
			startDash(player, s);
		}
	}

	private static void swap(ServerPlayer player, HeroState s, boolean thorns) {
		if (s.thornsOut == thorns) {
			return;
		}
		s.thornsOut = thorns;
		// the swap (0.125 s), then the draw delay before either fires (0.175 s: D 21C6)
		s.swapTicks = Ow.ticks(Math.max(SWAP, DRAW_DELAY));
		s.holsterTicks = 0;
		s.bloomTarget = null;
		s.weaponRecovery = 0;
		Combat.animate(player, thorns ? "draw_thorn" : "draw");
	}

	/**
	 * Either weapon's reload, and the automatic one of an empty clip in hand. A reload of either refills both (D: its
	 * messages 20B9); the ammo is back three quarters in.
	 */
	private static void reloads(ServerPlayer player, HeroState s, double attack) {
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		if (s.primaryReload >= 0) {
			s.primaryReload += attack;
			if (s.primaryReload >= Ow.ticks(BLOSSOM_RELOAD * BLOSSOM_RELOAD_AMMO)) {
				s.primaryAmmo = BLOSSOM_AMMO;
				s.secondaryAmmo = THORN_AMMO;
			}
			if (s.primaryReload >= Ow.ticks(BLOSSOM_RELOAD)) {
				s.primaryReload = -1;
			}
		}
		if (s.secondaryReload >= 0) {
			s.secondaryReload += attack;
			if (s.secondaryReload >= Ow.ticks(THORN_RELOAD * THORN_RELOAD_AMMO)) {
				s.primaryAmmo = BLOSSOM_AMMO;
				s.secondaryAmmo = THORN_AMMO;
			}
			if (s.secondaryReload >= Ow.ticks(THORN_RELOAD)) {
				s.secondaryReload = -1;
			}
		}
	}

	private static boolean reloading(HeroState s) {
		return s.primaryReload >= 0 || s.secondaryReload >= 0;
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		if (reloading(s) || s.primaryAmmo >= BLOSSOM_AMMO && s.secondaryAmmo >= THORN_AMMO) {
			return;
		}
		if (s.thornsOut) {
			s.secondaryReload = 0;
			Combat.animate(player, s.secondaryFiredSinceReload ? "reload_thorn_after_fire" : "reload_thorn");
		} else {
			// a reload resets the charge (W: a manual one; the drawn blossom goes back)
			if (s.charging) {
				s.primaryAmmo = Math.min(BLOSSOM_AMMO, s.primaryAmmo + 1);
			}
			s.charging = false;
			s.bloomCharge = 0;
			s.primaryReload = 0;
			Combat.animate(player, "reload");
		}
		s.secondaryFiredSinceReload = false;
	}

	/**
	 * Healing Blossom: holding the left button draws a blossom (its ammo is spent then) and charges it 0 -> 1 in 1.15 s
	 * from where the passive charge had it; letting go throws it at the ally it's aimed at, or loses it with no ally.
	 */
	private static void blossom(ServerPlayer player, HeroState s, double attack, boolean released) {
		if (s.primaryAmmo == 0 && !s.charging && !reloading(s) && s.reloadHeld == 0) {
			startReload(player, s);
		}
		if (s.swapTicks > 0 || reloading(s)) {
			return;
		}
		if (s.primaryHeld && !s.charging && s.bloomRecovery <= 0 && s.primaryAmmo > 0) {
			s.primaryAmmo--;
			s.charging = true;
			s.holsterTicks = 0;
			Combat.animate(player, "charge");
		}
		if (!s.charging) {
			s.bloomTarget = null;
			return;
		}
		if (s.primaryHeld) {
			// held: the active rate, sped up as his weapons are (Kitsune Rush's attack speed: D 1C54's weapon-speed var)
			s.bloomCharge = Math.min(1, s.bloomCharge + attack / (CHARGE_TIME * 20));
			track(player, s);
		} else if (released) {
			throwBlossom(player, s);
		}
	}

	/**
	 * The blossom's target while charging (D 21C6's tracker): the ally nearest the crosshair within 20° and 30 m, aimed at
	 * by their head, kept 0.15 s after leaving the cone. His data has no sight rays (an occlusion filter, M): ours, they
	 * must be in sight, as Mercy's beam.
	 */
	private static void track(ServerPlayer player, HeroState s) {
		LivingEntity current = s.bloomTarget == null ? null : player.level().getEntity(s.bloomTarget) instanceof LivingEntity l ? l : null;
		LivingEntity best = pick(player, TARGET_CONE, TARGET_RANGE);
		if (best != null) {
			s.bloomTarget = best.getId();
			s.bloomLinger = 0;
		} else if (current != null && current.isAlive() && ++s.bloomLinger <= Ow.ticks(TARGET_LINGER)) {
			return;
		} else {
			s.bloomTarget = null;
		}
	}

	/** The ally nearest the crosshair (the angle to their head) within the cone and range, in sight. */
	static @Nullable LivingEntity pick(ServerPlayer player, double cone, double range) {
		LivingEntity best = null;
		double bestAngle = cone;
		for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))) {
			if (!Allies.isAlly(player, other) || other.distanceTo(player) > range) {
				continue;
			}
			double angle = Math.min(angleTo(player, other.getEyePosition()), angleTo(player, other.getBoundingBox().getCenter()));
			if (angle < bestAngle && sees(player, other)) {
				best = other;
				bestAngle = angle;
			}
		}
		return best;
	}

	/** Degrees between his aim and the point. */
	static double angleTo(ServerPlayer player, Vec3 point) {
		Vec3 to = point.subtract(player.getEyePosition());
		if (to.lengthSqr() < 1.0E-6) {
			return 0;
		}
		double dot = player.getViewVector(1.0f).dot(to.normalize());
		return Math.toDegrees(Math.acos(Math.clamp(dot, -1, 1)));
	}

	static boolean sees(ServerPlayer player, LivingEntity other) {
		return player.hasLineOfSight(other) || clear(player, player.getEyePosition(), other.getBoundingBox().getCenter());
	}

	static boolean clear(Entity from, Vec3 a, Vec3 b) {
		return from.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)).getType() == HitResult.Type.MISS;
	}

	/**
	 * Where his right hand throws from, from his eyes in his look's frame (right, up, ahead, m): both weapons leave his
	 * hardpoint 000A (bone_004A, the right hand: D). Ours until measured on his first-person model, as Mercy's was.
	 */
	private static final double HAND_RIGHT = 0.22, HAND_UP = -0.2, HAND_AHEAD = 0.55;
	/** The left wrist (hp 0002): Petal Platform and the seed. Ours, as the hand. */
	private static final double WRIST_RIGHT = -0.25, WRIST_UP = -0.22, WRIST_AHEAD = 0.45;

	static Vec3 fromEyes(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 upv = side.cross(look).normalize();
		Vec3 at = eye.add(side.scale(right)).add(upv.scale(up)).add(look.scale(ahead));
		return clear(player, eye, at) ? at : eye;
	}

	/** What his crosshair is on (a block, else 100 m out). */
	static Vec3 aimPoint(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(player.getViewVector(1.0f).scale(100));
		Vec3 hit = player.level().clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		return hit.distanceTo(eye) < 1 ? far : hit;
	}

	private static void throwBlossom(ServerPlayer player, HeroState s) {
		LivingEntity target = s.bloomTarget == null ? null : player.level().getEntity(s.bloomTarget) instanceof LivingEntity l ? l : null;
		double charge = s.bloomCharge;
		s.charging = false;
		s.bloomCharge = 0;
		s.bloomTarget = null;
		s.bloomRecovery = Ow.ticks(BLOSSOM_RECOVERY);
		s.holsterTicks = 0;
		if (target == null || !target.isAlive()) {
			// no ally: the blossom is lost (D: effect 30E6)
			Combat.animate(player, "release");
			Effects.play(player.level(), "lifeweaver_blossom_lost", fromEyes(player, HAND_RIGHT, HAND_UP, HAND_AHEAD), player);
			return;
		}
		ServerLevel level = player.level();
		Vec3 from = fromEyes(player, HAND_RIGHT, HAND_UP, HAND_AHEAD);
		BlossomEntity blossom = new BlossomEntity(player, level, target, charge, heal(charge));
		blossom.launch(from, aimPoint(player).subtract(from), Ow.speed(blossomSpeed(charge)));
		level.addFreshEntity(blossom);
		Combat.animate(player, "release");
		Effects.play(level, "lifeweaver_blossom_throw", from, Vec3.ZERO, player, (int) Math.round(charge * 100), player);
	}

	/**
	 * Heals landing over time: a blossom's, a tree's pulse (Overwatch points a tick, ticks left; medic: weapon healing, of
	 * which his Medic sub-role heals him 40%).
	 */
	private record Heal(ServerPlayer owner, LivingEntity target, double perTick, int ticks, boolean medic) {
	}

	private static final List<Heal> HEALS = new ArrayList<>();

	/**
	 * A blossom reaches its ally (BlossomEntity): its heal lands over 0.3 x its charge s (at once with none: D 1C58). His
	 * Medic sub-role heals him 40% of it (no ult charge for that part).
	 */
	public static void blossomHeals(ServerPlayer owner, LivingEntity target, double heal, double over, double charge) {
		healOver(owner, target, heal, over, true);
		if (target instanceof ServerPlayer ally) {
			dev.overcraft.hero.Voice.say(owner, "heal", 1, dev.overcraft.hero.Voice.heroOf(ally));
		}
	}

	/** Heals {@code heal} over {@code over} s (at once for none). */
	public static void healOver(ServerPlayer owner, LivingEntity target, double heal, double over, boolean medic) {
		int ticks = Math.max(1, Ow.ticks(over));
		HEALS.add(new Heal(owner, target, heal / ticks, ticks, medic));
	}

	/** Every server tick: the heals in flight land. */
	public static void tickHeals() {
		List<Heal> next = new ArrayList<>();
		for (Heal h : HEALS) {
			if (h.target.isAlive()) {
				float healed = Combat.heal(h.target, h.perTick);
				if (healed > 0) {
					Combat.credit(h.owner, healed);
					Combat.addFireForHealing(h.owner, healed);
					if (h.medic && h.owner.isAlive()) {
						Combat.heal(h.owner, healed * MEDIC_SELF_HEAL);
					}
				}
			}
			if (h.ticks > 1 && h.target.isAlive()) {
				next.add(new Heal(h.owner, h.target, h.perTick, h.ticks - 1, h.medic));
			}
		}
		HEALS.clear();
		HEALS.addAll(next);
		tickBlooms();
	}

	/**
	 * Thorn Volley: 11 volleys a second of two thorns while held, 2 ammo a volley. The first thorn spreads 0 -> 0.9°, the
	 * second 0.2 -> 1.2°, over 10 volleys; not firing, the spread comes back 2 volleys a second.
	 */
	private static void thorns(ServerPlayer player, HeroState s, double attack) {
		if (s.secondaryAmmo < THORN_AMMO_PER_VOLLEY && !reloading(s) && s.reloadHeld == 0) {
			startReload(player, s);
		}
		s.weaponRecovery -= attack;
		boolean firing = false;
		if (s.secondaryHeld && s.swapTicks == 0 && s.weaponRecovery <= 0 && s.secondaryAmmo >= THORN_AMMO_PER_VOLLEY && !reloading(s)) {
			s.secondaryAmmo -= THORN_AMMO_PER_VOLLEY;
			s.weaponRecovery += 20 / VOLLEYS_PER_SECOND;
			s.secondaryFiredSinceReload = true;
			s.holsterTicks = 0;
			double t = Math.min(1, s.thornSpread / SPREAD_VOLLEYS);
			volley(player, SPREAD_A_MAX * t, SPREAD_B_MIN + (SPREAD_B_MAX - SPREAD_B_MIN) * t);
			s.thornSpread = Math.min(SPREAD_VOLLEYS, s.thornSpread + 1);
			Combat.animate(player, "fire_thorn");
			firing = true;
		}
		if (!firing && !s.secondaryHeld) {
			s.thornSpread = Math.max(0, s.thornSpread - SPREAD_DECAY / 20);
		}
		if (s.weaponRecovery < 0) {
			s.weaponRecovery = 0;
		}
	}

	private static void volley(ServerPlayer player, double spreadA, double spreadB) {
		ServerLevel level = player.level();
		Vec3 from = fromEyes(player, HAND_RIGHT, HAND_UP, HAND_AHEAD);
		Vec3 aim = aimPoint(player).subtract(from).normalize();
		for (double spread : new double[] {spreadA, spreadB}) {
			ThornEntity thorn = new ThornEntity(player, level);
			thorn.launch(from, spread(player, aim, spread), Ow.speed(THORN_SPEED));
			level.addFreshEntity(thorn);
		}
		Effects.play(level, "lifeweaver_thorn_fire", from, aim, player, 0, player);
	}

	/** A direction within {@code degrees} of the aim, evenly over the cone's disc. */
	private static Vec3 spread(ServerPlayer player, Vec3 aim, double degrees) {
		if (degrees <= 0) {
			return aim;
		}
		Vec3 side = aim.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(aim).normalize();
		double r = Math.tan(Math.toRadians(degrees)) * Math.sqrt(player.getRandom().nextDouble());
		double a = player.getRandom().nextDouble() * Math.PI * 2;
		return aim.add(side.scale(r * Math.cos(a))).add(up.scale(r * Math.sin(a))).normalize();
	}

	// ---- Superbloom (major perk) ----

	/** Thorn hits per enemy (game times), for Superbloom. */
	private static final Map<UUID, List<Long>> BLOOMS = new HashMap<>();

	/** A thorn hit an enemy (ThornEntity): with Superbloom, 16 in 2.5 s burst for 40. */
	public static void thornHit(ServerPlayer player, LivingEntity target) {
		if (!has(player, Perk.LIFEWEAVER_SUPERBLOOM)) {
			return;
		}
		long now = player.level().getGameTime();
		List<Long> hits = BLOOMS.computeIfAbsent(target.getUUID(), id -> new ArrayList<>());
		hits.removeIf(at -> now - at > Ow.ticks(SUPERBLOOM_TIME));
		hits.add(now);
		if (hits.size() >= SUPERBLOOM_THORNS) {
			hits.clear();
			float dealt = Combat.damage(target, player.damageSources().playerAttack(player), SUPERBLOOM_DAMAGE);
			Combat.credit(player, dealt);
			Combat.addFireForDamage(player, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(player, target, false);
			}
			Effects.play(player.level(), "lifeweaver_superbloom", target.getBoundingBox().getCenter(), Vec3.ZERO, target, 0, player);
		}
	}

	private static void tickBlooms() {
		if (BLOOMS.isEmpty()) {
			return;
		}
		Iterator<List<Long>> it = BLOOMS.values().iterator();
		while (it.hasNext()) {
			if (it.next().isEmpty()) {
				it.remove();
			}
		}
	}

	// ---- Petal Platform ----

	private static @Nullable Entity platform(ServerPlayer player, HeroState s) {
		return s.platformId == null ? null : player.level().getEntity(s.platformId);
	}

	/**
	 * Shift: a platform thrown from his left wrist 0.1 s after the press, at 50 m/s with gravity (D 2219), the cooldown
	 * starting 0.25 s after the press. Shift again, from 0.4 s after the throw, destroys the one he has; a new throw
	 * destroys the old one.
	 */
	private static void startPlatform(ServerPlayer player, HeroState s) {
		Entity old = platform(player, s);
		if (old != null && old.isAlive()) {
			if (player.level().getGameTime() - s.platformAt >= Ow.ticks(PLATFORM_DESTROY_FROM)) {
				destroyPlatform(old);
				s.platformId = null;
			}
			return;
		}
		if (s.ability1Cooldown > 0 || s.abilityLock > 0 || s.platformThrow >= 0 || s.treeCast >= 0 || s.treeAiming) {
			return;
		}
		s.platformThrow = Ow.ticks(PLATFORM_WINDUP);
		s.ability1Cooldown = Ow.ticks(PLATFORM_COOLDOWN + PLATFORM_COOLDOWN_FROM);
		s.abilityLock = Ow.ticks(PLATFORM_LOCK);
		Combat.animate(player, "throw");
		dev.overcraft.hero.Voice.say(player, "platform", 1);
	}

	private static void platformThrow(ServerPlayer player, HeroState s) {
		if (s.platformThrow < 0 || --s.platformThrow > 0) {
			return;
		}
		s.platformThrow = -1;
		ServerLevel level = player.level();
		Vec3 from = fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD);
		PetalThrowEntity thrown = new PetalThrowEntity(player, level, false);
		thrown.launch(from, aimPoint(player).subtract(from), Ow.speed(PLATFORM_THROW_SPEED));
		level.addFreshEntity(thrown);
		s.platformId = thrown.getId();
		s.platformAt = level.getGameTime();
	}

	/** The thrown platform landed and deployed (PetalThrowEntity): it's his platform now. */
	public static void deployed(ServerPlayer player, PetalPlatformEntity platform) {
		HeroState s = HeroManager.state(player);
		if (s != null && s.hero == Hero.LIFEWEAVER) {
			s.platformId = platform.getId();
		}
	}

	/** It couldn't deploy where it landed (no room): the cooldown has 1 s left (the 11 s refund: D 2219). */
	public static void blocked(ServerPlayer player) {
		HeroState s = HeroManager.state(player);
		if (s != null && s.hero == Hero.LIFEWEAVER) {
			s.ability1Cooldown = Math.min(s.ability1Cooldown, Ow.ticks(PLATFORM_BLOCKED_COOLDOWN));
			s.platformId = null;
		}
	}

	private static void destroyPlatform(Entity platform) {
		if (platform instanceof PetalPlatformEntity deployed) {
			deployed.destroy();
		} else {
			platform.discard();
		}
	}

	// ---- Rejuvenating Dash ----

	/**
	 * Jump in the air, from 0.1 s after leaving the ground: his client has already set him dashing (OwMovement), 0.25 s
	 * at 28.875 m/s along his keys; here its heal (55 over the dash) and its cooldown (5 s from its end).
	 */
	private static void startDash(ServerPlayer player, HeroState s) {
		if (s.ability3Cooldown > 0 || s.dashTicks >= 0 || player.onGround() || LifeGrip.gripped(player)) {
			return;
		}
		s.dashTicks = Ow.ticks(DASH_TIME);
		s.softLanding = true;
		Combat.animate(player, "dash");
		Effects.play(player.level(), "lifeweaver_dash", player.position(), player);
		dev.overcraft.hero.Voice.say(player, "dash", 0.6f);
	}

	private static void dash(ServerPlayer player, HeroState s) {
		if (s.dashTicks >= 0) {
			Combat.heal(player, DASH_HEAL / Ow.ticks(DASH_TIME));
			if (--s.dashTicks < 0) {
				s.ability3Cooldown = Ow.ticks(DASH_COOLDOWN);
			}
		}
		// an ability's landing never hurts (the owner's rule, #35): the dash's fall doesn't count until he's down
		if (s.softLanding) {
			player.resetFallDistance();
			if (s.dashTicks < 0 && player.onGround()) {
				s.softLanding = false;
			}
		}
	}

	// ---- Life Grip ----

	/**
	 * E: the ally nearest the crosshair within 20° and 30 m (picked once), cast 0.3 s; the cooldown (18 s) from the cast,
	 * none with no ally. Not while aiming the tree, nor in the lock it shares with the platform.
	 */
	private static void startGrip(ServerPlayer player, HeroState s) {
		if (s.ability2Cooldown > 0 || s.abilityLock > 0 || s.gripCast >= 0 || s.treeAiming || s.treeCast >= 0) {
			return;
		}
		LivingEntity target = pick(player, GRIP_CONE, GRIP_RANGE);
		if (target == null || LifeGrip.gripped(target)) {
			Effects.play(player.level(), "lifeweaver_grip_none", player.position(), player);
			return;
		}
		s.gripCast = Ow.ticks(GRIP_CAST);
		s.gripTarget = target.getId();
		s.ability2Cooldown = Ow.ticks(GRIP_COOLDOWN);
		s.abilityLock = Ow.ticks(GRIP_LOCK);
		s.interruptReload(GRIP_CAST);
		LifeGrip.start(player, target);
		Combat.animate(player, "grip");
		dev.overcraft.hero.Voice.say(player, "grip", 1);
	}

	private static void lifeGrip(ServerPlayer player, HeroState s) {
		if (s.gripCast >= 0 && --s.gripCast < 0) {
			s.gripTarget = null;
		}
	}

	// ---- Tree of Life ----

	private static @Nullable TreeOfLifeEntity treeOf(ServerPlayer player, HeroState s) {
		return s.treeId != null && player.level().getEntity(s.treeId) instanceof TreeOfLifeEntity tree && tree.isAlive() ? tree : null;
	}

	/**
	 * Where the tree would go: the ground his crosshair is on within 30 m (D 36A4's ground finder): a block's top, or the
	 * ground under a wall or the range's end (within 10 m down: ours). Null where there's none.
	 */
	public static @Nullable Vec3 treeSpot(net.minecraft.world.entity.player.Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(TREE_RANGE));
		BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == net.minecraft.core.Direction.UP) {
			return hit.getLocation();
		}
		Vec3 from = hit.getType() == HitResult.Type.BLOCK
			? hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(0.3)) : end;
		BlockHitResult down = player.level().clip(new ClipContext(from, from.add(0, -10, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		return down.getType() == HitResult.Type.BLOCK && down.getDirection() == net.minecraft.core.Direction.UP ? down.getLocation() : null;
	}

	private static void confirmTree(ServerPlayer player, HeroState s) {
		Vec3 at = treeSpot(player);
		if (at == null) {
			return;
		}
		s.treeAiming = false;
		s.treeAt = at;
		s.treeCast = 0;
		// an old tree stops (D 36A4)
		TreeOfLifeEntity old = treeOf(player, s);
		if (old != null) {
			old.end();
		}
		Combat.animate(player, "tree_cast");
	}

	private static void tree(ServerPlayer player, HeroState s) {
		if (s.treeCast < 0) {
			return;
		}
		s.treeCast++;
		if (s.treeCast == Ow.ticks(TREE_SPAWN_AT) && s.treeAt != null) {
			// 0.15 s in: the charge goes and the tree sprouts (D 36A4)
			s.ultCharge = Combat.afterUlt(Hero.LIFEWEAVER, s.ultCharge);
			s.ultActive = Ow.ticks(TREE_TIME);
			TreeOfLifeEntity tree = new TreeOfLifeEntity(player.level(), player, s.treeAt);
			player.level().addFreshEntity(tree);
			s.treeId = tree.getId();
			dev.overcraft.hero.Voice.say(player, "ultimate", 1);
		}
		if (s.treeCast >= Ow.ticks(TREE_CAST)) {
			s.treeCast = -1;
			s.treeAt = null;
		}
	}

	/** The tree ended (its time, its health, felled): no ult in progress any more. */
	public static void treeEnded(ServerPlayer player, TreeOfLifeEntity tree) {
		HeroState s = HeroManager.state(player);
		if (s != null && s.hero == Hero.LIFEWEAVER && s.treeId != null && s.treeId == tree.getId()) {
			s.treeId = null;
			s.ultActive = 0;
		}
	}

	// ---- Sow the Seed (major perk) ----

	/** Quick melee with Sow the Seed and the Blossom in hand throws the seed instead (ours: no swing with it, L). */
	public static boolean sows(ServerPlayer player, HeroState s) {
		return s.hero == Hero.LIFEWEAVER && has(player, Perk.LIFEWEAVER_SOW_THE_SEED) && !s.thornsOut && s.seedCooldown <= 0
			&& s.treeCast < 0 && !s.treeAiming;
	}

	public static void sow(ServerPlayer player, HeroState s) {
		s.seedCooldown = Ow.ticks(SEED_COOLDOWN);
		ServerLevel level = player.level();
		Vec3 from = fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD);
		PetalThrowEntity seed = new PetalThrowEntity(player, level, true);
		seed.launch(from, player.getViewVector(1.0f), Ow.speed(SEED_SPEED));
		level.addFreshEntity(seed);
		Combat.animate(player, "throw_blossom");
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		boolean casting = s.treeCast >= 0;
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && !casting) {
			startPlatform(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && !casting) {
			startGrip(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && !casting && !s.treeAiming) {
			startReload(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && !casting) {
			TreeOfLifeEntity tree = treeOf(player, s);
			if (tree != null) {
				// Q again fells it, from 0.5 s after it sprouted (D 36A4; Overwatch's option, on by default)
				if (tree.tickCount >= Ow.ticks(TREE_DESTROY_FROM)) {
					tree.end();
				}
			} else if (s.treeAiming) {
				// ours: Q again stops aiming, as the right button does
				s.treeAiming = false;
			} else if (s.ultCharge >= ULT_COST) {
				s.treeAiming = true;
				s.charging = false;
				s.bloomTarget = null;
			}
		}
	}

	// ---- what everyone sees ----

	/** His state for every client (HeroManager.LIFEWEAVER): [flags, the charge (%), the blossom's target, Life Grip's ally]. */
	private static void publish(ServerPlayer player, HeroState s) {
		List<Integer> state = new ArrayList<>(4);
		state.add(flags(s));
		state.add((int) Math.round(s.bloomCharge * 100));
		state.add(s.bloomTarget == null ? -1 : s.bloomTarget);
		state.add(s.gripTarget == null ? -1 : s.gripTarget);
		if (!state.equals(player.getAttached(HeroManager.LIFEWEAVER))) {
			player.setAttached(HeroManager.LIFEWEAVER, state);
		}
	}

	private static int flags(HeroState s) {
		return (s.thornsOut ? FLAG_THORNS : 0) | (s.charging ? FLAG_CHARGING : 0) | (s.swapTicks > 0 ? FLAG_SWAP : 0)
			| (s.treeAiming ? FLAG_TREE_AIM : 0) | (s.treeCast >= 0 ? FLAG_TREE_CAST : 0) | (s.treeId != null ? FLAG_TREE : 0)
			| (s.platformId != null ? FLAG_PLATFORM : 0) | (s.dashTicks >= 0 ? FLAG_DASH : 0) | (s.gripCast >= 0 ? FLAG_GRIP : 0)
			| (s.thornsOut && s.secondaryHeld && s.secondaryAmmo >= THORN_AMMO_PER_VOLLEY && !reloading(s) ? FLAG_FIRING : 0)
			| (s.seedCooldown <= 0 ? FLAG_SEED : 0);
	}

	/** Leaving Lifeweaver (another hero, leaving the world): his platform and tree go, and his synced state. */
	public static void clear(ServerPlayer player, HeroState s) {
		Entity platform = platform(player, s);
		if (platform != null) {
			destroyPlatform(platform);
		}
		TreeOfLifeEntity tree = treeOf(player, s);
		if (tree != null) {
			tree.end();
		}
		s.platformId = s.treeId = null;
		player.removeAttached(HeroManager.LIFEWEAVER);
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float reload = s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(BLOSSOM_RELOAD)) : -1;
		float thornReload = s.secondaryReload >= 0 ? (float) Math.min(1, s.secondaryReload / Ow.ticks(THORN_RELOAD)) : -1;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ULT_COST, s.treeId != null || s.treeCast >= 0,
			s.primaryAmmo, reload, s.secondaryAmmo, thornReload, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), (float) (s.ability3Cooldown / 20.0), flags(s), s.ultActive / 20f, (float) s.bloomCharge);
	}
}

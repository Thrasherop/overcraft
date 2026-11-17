package dev.overcraft.widowmaker;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.GrappleHookEntity;
import dev.overcraft.entity.VenomMineEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.overcraft.widowmaker.WidowmakerStats.*;

/**
 * Widowmaker's kit (#138; numbers in {@link WidowmakerStats}, the data map tools/reference/widowmaker-map.md). Her buttons by her data's
 * ids: the left button Widow's Kiss (PRIMARY: automatic from the hip, or a charged shot while the right is held: SECONDARY, Take Aim),
 * R the reload, Shift Grappling Hook (ABILITY 1: {@link GrappleHookEntity}), E Venom Mine (ABILITY 2: {@link VenomMineEntity}), Q
 * Infra-Sight (ULTIMATE: her side sees her enemies through walls). Her passive is the sub-role's, Sharpshooter (crits take time off
 * Grappling Hook). The server owns all of it; her client zooms with the synced scope (ClientAna) and outlines what it's told
 * (widow_reveal, WidowmakerFx).
 */
public final class Widowmaker implements HeroKit {
	/** The synced mode's bits: fully scoped, reloading, a mine out, hooked (wind-up to the end), casting the ult, Infra-Sight on. */
	public static final int MODE_SCOPED = 1, MODE_RELOAD = 2, MODE_MINE_OUT = 4, MODE_HOOK = 8, MODE_CASTING = 16, MODE_INFRA = 32;

	static final Blow HIP_BLOW = Blow.weapon("widow_s_kiss", false).by(Hero.WIDOWMAKER);
	static final Blow SCOPED_BLOW = Blow.weapon("widow_s_kiss", false).by(Hero.WIDOWMAKER);
	public static final Blow MINE_BLOW = Blow.ability("venom_mine").by(Hero.WIDOWMAKER);
	static final Blow SEEKER_BLOW = Blow.perk("seeker_mine").by(Hero.WIDOWMAKER);

	private static final Identifier SCOPE_SPEED = Overcraft.id("widowmaker_scoped");

	/** The shot effect's variants (WidowmakerFx "widow_shot"): from the hip, scoped under 50 %, 50-99 %, full (D 004C, 0107 / 004B / 0108). */
	public static final int SHOT_HIP = 0, SHOT_SOFT = 1, SHOT_MID = 2, SHOT_FULL = 3;
	/** The hit effect's variants ("widow_hit"): on a creature, on a block, a scoped shot on a creature. */
	public static final int HIT_CREATURE = 0, HIT_BLOCK = 1, HIT_SCOPED = 2;
	/** The poison effect's variants ("widow_poison" on the target): the mine's, a Seeker Mine dart's. */
	public static final int POISON_MINE = 0, POISON_DART = 1;

	/** Poison under way: on what, by whom, per step, steps left, ticks to the next. */
	private record Dot(LivingEntity target, UUID owner, double step, int[] left, Blow blow) {
	}

	private static final Map<String, Dot> DOTS = new ConcurrentHashMap<>();

	/** A Seeker Mine dart in flight (ours: a timed hit, the dart drawn by its clients): to whom, by whom, ticks left. */
	private record Dart(LivingEntity target, UUID owner, int[] left) {
	}

	private static final List<Dart> DARTS = new ArrayList<>();

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.widowmaker.clear();
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		speed(player, 0);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		WidowmakerState w = s.widowmaker;
		ServerLevel level = player.level();
		long now = level.getGameTime();
		w.silenced = dev.overcraft.sombra.Hack.silenced(player);
		double rate = Combat.cooldownRate(player);
		if (w.hookPhase == 0 || w.hookPhase == 5) {
			// the hook's cooldown is set when the pull ends (D 005A #160): nothing runs down meanwhile
			s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		}
		if (w.mineCast < 0 || w.mineCast >= Ow.ticks(MINE_COOLDOWN_AT)) {
			s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		}
		boolean able = controls && player.isAlive() && !Sleep.asleep(player);
		boolean jump = player.getLastClientInput().jump();
		boolean jumped = jump && !w.jumpWas;
		w.jumpWas = jump;
		hook(player, s, w, able, jumped, now);
		mineCast(player, s, w, able);
		ultCast(player, s, w, able, now);
		weapon(player, s, w, able, jumped, now);
		if (w.checkTick++ % 20 == 0 && w.mine != null && mine(player, w) == null) {
			w.mine = null;
		}
		if (w.ultUntil >= 0) {
			if (now >= w.ultUntil) {
				endInfra(player, s, w);
			} else {
				s.ultActive = Math.max(s.ultActive, 2);
				if ((now - w.ultUntil) % 10 == 0) {
					infraSight(player);
				}
			}
		} else if (w.ultCast >= 0) {
			s.ultActive = Math.max(s.ultActive, 2);
		}
		// the pull's end carries her on: she lands unhurt (CLAUDE.md, #35: "not at all when an ability lands them")
		boolean launched = w.launchGrace > 0 || w.hookPhase == 3 || w.hookPhase == 4;
		if (w.launchGrace > 0) {
			w.launchGrace = player.onGround() ? w.launchGrace - 1 : w.launchGrace;
		}
		HeroManager.softLanding(player, s, launched, false);
		// every client's look of her: Take Aim's scope (%), for her scoped poses (HeroAnimator) and the pull's (+1000 while pulled)
		int look = (int) Math.round(w.scope * 100) + (w.hookPhase == 3 ? 1000 : w.hookPhase == 4 ? 2000 : 0);
		Integer was = player.getAttached(HeroManager.WIDOWMAKER_LOOK);
		if (was == null ? look != 0 : was != look) {
			player.setAttached(HeroManager.WIDOWMAKER_LOOK, look);
		}
	}

	// ---- Widow's Kiss and Take Aim (001B) ----

	/** The scope after one more tick toward {@code wanted} (0.333 s each way: D v169 rate 3). */
	public static double scopeStep(double scope, boolean wanted) {
		return wanted ? Math.min(1, scope + 1 / (20 * SCOPE_IN)) : Math.max(0, scope - 1 / (20 * SCOPE_OUT));
	}

	/** The charge after one tick of {@code rate} (x2 with Sniper's Instinct), up to {@code max} (10; 12.5 with Widow's Bite). */
	public static double chargeStep(double charge, double max, boolean instinct) {
		return Math.min(max, charge + CHARGE_RATE * (instinct ? INSTINCT_RATE : 1) / 20.0);
	}

	/** A scoped shot's damage at this charge before falloff and crit: 12 x the charge (12 .. 120, 150 with Widow's Bite). */
	public static double scopedDamage(double charge) {
		return SCOPED_DAMAGE_PER * charge;
	}

	/** The charge as Overwatch shows it: 0 % at none, 100 % full (125 % with Widow's Bite). */
	public static double chargePercent(double charge) {
		if (charge > CHARGE_MAX) {
			// Widow's Bite's last part: 100 -> 125 % over 10 -> 12.5 (the perk's text; ours, L: the data has only v174's range)
			return 100 + (charge - CHARGE_MAX) / (BITE_CHARGE_MAX - CHARGE_MAX) * 25;
		}
		return (charge - CHARGE_MIN) / (CHARGE_MAX - CHARGE_MIN) * 100;
	}

	/** The hip spread's angle after {@code shots} shots in a row (0 -> 3 degrees over 11). */
	public static double hipSpread(double shots) {
		return HIP_SPREAD * Math.min(1, shots / HIP_SPREAD_SHOTS);
	}

	private static void weapon(ServerPlayer player, HeroState s, WidowmakerState w, boolean able, boolean jumped, long now) {
		double attack = Combat.attackRate(player);
		w.hipWait = Math.max(0, w.hipWait - attack);
		w.recovery = Math.max(0, w.recovery - attack);
		if (w.spreadWait > 0) {
			w.spreadWait--;
		} else {
			// 4 a second of the whole spread (D #69): all of it in 0.25 s
			w.spreadShots = Math.max(0, w.spreadShots - HIP_SPREAD_SHOTS * HIP_SPREAD_RECOVERY / 20.0);
		}
		if (w.jumpUnscope > 0) {
			w.jumpUnscope--;
		}
		reload(player, s, w, able, attack);
		boolean busy = !able || w.hookPhase != 0 && w.hookPhase != 5 || w.mineCast >= 0 || w.ultCast >= 0;
		if (jumped && w.scope > 0) {
			// a jump drops the scope for 0.5 s (D #188 / #189 / #192); she can scope again in the air after it
			w.jumpUnscope = Ow.ticks(JUMP_UNSCOPE);
		}
		boolean reloadBlocks = w.reloadTicks >= 0 && w.reloadTicks < Ow.ticks(RELOAD_NO_SCOPE);
		boolean wanted = able && s.secondaryHeld && !busy && !reloadBlocks && w.jumpUnscope <= 0 && !player.isPassenger();
		double before = w.scope;
		w.scope = scopeStep(w.scope, wanted);
		if (before == 0 && w.scope > 0) {
			WidowmakerSounds.split(player, "ads.in", player.getEyePosition(), 20);
			Combat.animate(player, "ads_in");
		} else if (before > 0 && w.scope < before) {
			if (before >= 1) {
				WidowmakerSounds.split(player, "ads.out", player.getEyePosition(), 20);
				Combat.animate(player, "ads_out");
			}
		}
		if (w.scope >= 1 && w.recovery <= 0) {
			if (w.charge <= CHARGE_MIN) {
				// the charge's sound from its start (ads.charge 10C9, a 1.55 s one-shot)
				WidowmakerSounds.own(player, "ads.charge");
			}
			double max = s.has(Perk.WIDOWMAKER_WIDOWS_BITE) ? BITE_CHARGE_MAX : CHARGE_MAX;
			boolean wasFull = w.charge >= CHARGE_MAX;
			w.charge = chargeStep(w.charge, max, w.instinctUntil >= 0 && now < w.instinctUntil);
			if (!wasFull && w.charge >= CHARGE_MAX) {
				WidowmakerSounds.own(player, "ads.charged");
			}
		} else if (w.scope < 1) {
			// back to none whenever the charge stops (D #125)
			w.charge = CHARGE_MIN;
		}
		speed(player, w.scope >= 1 ? -SCOPE_SLOW : 0);
		if (busy || w.reloadTicks >= 0) {
			return;
		}
		if (s.primaryAmmo <= 0) {
			// empty: the reload starts by itself (D #4)
			startReload(player, s, w);
			return;
		}
		if (w.scope >= 1) {
			// scoped: a shot on the press, once fully up and its recovery over (D #54; L the press against holding)
			if (s.primaryFresh && w.recovery <= 0) {
				scopedShot(player, s, w);
			}
		} else if (w.scope <= 0 && s.primaryHeld && w.hipWait <= 0) {
			s.primaryAmmo--;
			w.hipWait = 20.0 / HIP_RATE;
			Vec3 dir = dev.overcraft.sombra.Sombra.cone(player.getViewVector(1.0f), hipSpread(w.spreadShots), player.getRandom());
			w.spreadShots = Math.min(HIP_SPREAD_SHOTS, w.spreadShots + 1);
			w.spreadWait = Ow.ticks(HIP_SPREAD_WAIT);
			w.recovery = Math.max(w.recovery, Ow.ticks(HIP_RECOVERY));
			WidowmakerSounds.split(player, "kiss.fire", player.getEyePosition(), 120);
			Combat.animate(player, "fire");
			shot(player, s, dir, HIP_DAMAGE, HIP_FALLOFF_START, HIP_FALLOFF_END, HIP_FALLOFF_MIN, HIP_CRIT, HIP_RADIUS, false, false, SHOT_HIP);
		}
	}

	private static void scopedShot(ServerPlayer player, HeroState s, WidowmakerState w) {
		int cost = s.has(Perk.WIDOWMAKER_SCOPED_EFFICIENCY) ? SCOPED_COST_EFFICIENT : SCOPED_COST;
		s.primaryAmmo = Math.max(0, s.primaryAmmo - cost);
		double share = (w.charge - CHARGE_MIN) / (CHARGE_MAX - CHARGE_MIN);
		int variant = share >= 1 ? SHOT_FULL : share >= CHARGE_SOFT ? SHOT_MID : SHOT_SOFT;
		WidowmakerSounds.split(player, variant == SHOT_FULL ? "kiss.fire_ads" : variant == SHOT_MID ? "kiss.fire_ads_mid" : "kiss.fire_ads_low",
			player.getEyePosition(), 250);
		Combat.animate(player, "ads_fire");
		// Widow's Bite: above 100 % it pierces enemies (D #740 / #746 -> volley #52)
		boolean pierce = s.has(Perk.WIDOWMAKER_WIDOWS_BITE) && w.charge > CHARGE_MAX;
		shot(player, s, player.getViewVector(1.0f), scopedDamage(w.charge), SCOPED_FALLOFF_START, SCOPED_FALLOFF_END, SCOPED_FALLOFF_MIN,
			SCOPED_CRIT, SCOPED_RADIUS, true, pierce, variant);
		w.charge = CHARGE_MIN;
		w.recovery = Ow.ticks(SCOPED_RECOVERY);
	}

	private static void startReload(ServerPlayer player, HeroState s, WidowmakerState w) {
		if (w.reloadTicks >= 0 || s.primaryAmmo >= AMMO || w.hookPhase != 0 && w.hookPhase != 5 || w.mineCast >= 0 || w.ultCast >= 0) {
			return;
		}
		Combat.animate(player, w.scope > 0 ? "reload_ads" : "reload");
		w.reloadTicks = 0;
		w.reloadFilled = false;
		w.scope = 0;
		w.charge = CHARGE_MIN;
		WidowmakerSounds.split(player, "reload", player.getEyePosition(), 30);
	}

	private static void reload(ServerPlayer player, HeroState s, WidowmakerState w, boolean able, double attack) {
		if (w.reloadTicks < 0) {
			return;
		}
		if (!able) {
			// a stun or a sleep ends it (ours, as Ashe's); the rounds come back only if it got that far
			w.reloadTicks = -1;
			return;
		}
		w.reloadTicks += attack;
		if (!w.reloadFilled && w.reloadTicks >= Ow.ticks(RELOAD_AMMO_AT)) {
			// the rounds back at 0.825 s (D #273 -> #271)
			s.primaryAmmo = AMMO;
			w.reloadFilled = true;
		}
		if (w.reloadTicks >= Ow.ticks(RELOAD)) {
			w.reloadTicks = -1;
		}
	}

	/** A falloff share at {@code metres}: full to {@code start}, {@code min} from {@code end}, linear between (D; M the line). */
	public static double falloff(double metres, double start, double end, double min) {
		if (metres <= start) {
			return 1;
		}
		if (metres >= end) {
			return min;
		}
		return 1 - (1 - min) * (metres - start) / (end - start);
	}

	/** The hip shot's damage at {@code metres}, x2 on the head. */
	public static double hipDamage(double metres, boolean crit) {
		return HIP_DAMAGE * falloff(metres, HIP_FALLOFF_START, HIP_FALLOFF_END, HIP_FALLOFF_MIN) * (crit ? HIP_CRIT : 1);
	}

	/** A scoped shot's damage at {@code metres} and this charge, x2.5 on the head. */
	public static double scopedDamage(double charge, double metres, boolean crit) {
		return scopedDamage(charge) * falloff(metres, SCOPED_FALLOFF_START, SCOPED_FALLOFF_END, SCOPED_FALLOFF_MIN) * (crit ? SCOPED_CRIT : 1);
	}

	/**
	 * One hitscan shot from her eyes along {@code dir}: the first creature she can hurt within {@code radius} before a block (each one
	 * on the line when it pierces), its falloff, its crit on the head.
	 */
	private static void shot(ServerPlayer player, HeroState s, Vec3 dir, double damage, double start, double end, double min, double critX,
							 double radius, boolean scoped, boolean pierce, int variant) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(dir.scale(RANGE));
		BlockHitResult blockHit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 block = blockHit.getLocation();
		List<Hit> hits = hits(level, player, eye, block, radius);
		Vec3 muzzle = fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Vec3 stop = block;
		boolean any = false;
		for (Hit hit : hits) {
			LivingEntity target = hit.target;
			boolean crit = dev.overcraft.hero.Headshot.crossed(target, hit.at, dir, Math.max(radius, 0.01));
			double dealtOw = damage * falloff(hit.metres, start, end, min) * (crit ? critX : 1);
			Blow blow = (scoped ? SCOPED_BLOW : HIP_BLOW).crit(crit);
			float dealt = hit.part != null ? Combat.damagePart(level, hit.part, Combat.hitscan(player), dealtOw, blow)
				: Combat.damage(target, Combat.hitscan(player), dealtOw, blow);
			Combat.credit(player, dealt, target);
			Combat.addFireForDamage(player, dealt, target);
			WidowmakerSounds.split(player, scoped ? crit ? "kiss.hit_crit_ads" : "kiss.hit_enemy_ads" : "kiss.hit_enemy", hit.at, 500);
			if (dealt > 0) {
				Effects.hitMarker(player, target, crit);
				if (crit) {
					s.critAt = level.getGameTime();
					if (Allies.isEnemy(player, target)) {
						// on an enemy only (D 01C7 #2832's filter)
						sharpshooter(s, dealt);
					}
				}
			}
			Effects.play(level, "widow_hit", hit.at, dir.scale(-1), target, scoped ? HIT_SCOPED : HIT_CREATURE, player);
			any = true;
			stop = hit.at;
			if (!pierce || !Allies.isEnemy(player, target)) {
				break;
			}
		}
		if (!any || pierce) {
			stop = block;
		}
		Effects.play(level, "widow_shot", muzzle, stop.subtract(muzzle), player, variant, player);
		if ((!any || pierce) && blockHit.getType() == HitResult.Type.BLOCK) {
			WidowmakerSounds.at(level, "kiss.hit_world", block, 32);
			Effects.play(level, "widow_hit", block, Vec3.atLowerCornerOf(blockHit.getDirection().getUnitVec3i()), null, HIT_BLOCK, player);
		}
	}

	/** A creature on a shot's line: what, where, how far, the dragon's part it crossed. */
	record Hit(LivingEntity target, Vec3 at, double metres, @Nullable EnderDragonPart part) {
	}

	/** The creatures {@code shooter} can hurt within {@code radius} of the line {@code from -> to}, nearest first. */
	static List<Hit> hits(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 to, double radius) {
		List<Hit> found = new ArrayList<>();
		Set<LivingEntity> seen = new HashSet<>();
		for (Entity e : level.getEntities(shooter, new AABB(from, to).inflate(1))) {
			LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
			if (target == null || !target.isAlive() || target == shooter || !Allies.canHurt(shooter, target)) {
				continue;
			}
			java.util.Optional<Vec3> on = e.getBoundingBox().inflate(radius).clip(from, to);
			if (on.isPresent()) {
				double d = on.get().distanceTo(from);
				found.add(new Hit(target, on.get(), d, e instanceof EnderDragonPart part ? part : null));
			}
		}
		found.sort(java.util.Comparator.comparingDouble(Hit::metres));
		found.removeIf(h -> !seen.add(h.target));
		return found;
	}

	/** Sharpshooter (01C7 #2832 -> 005A #405): crit damage dealt takes 0.0075 s a point off Grappling Hook's cooldown (ticks). */
	public static double sharpshooterCut(double critDamage) {
		return Math.max(0, critDamage) * SHARPSHOOTER * 20;
	}

	private static void sharpshooter(HeroState s, double critDamage) {
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - sharpshooterCut(critDamage));
	}

	/** A point from her eyes in her look's frame (right, up, ahead), or her eyes when a wall is between. */
	static Vec3 fromEyes(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 top = side.cross(look).normalize();
		Vec3 at = eye.add(side.scale(right)).add(top.scale(up)).add(look.scale(ahead));
		if (player.level().clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return eye;
		}
		return at;
	}

	// ---- Grappling Hook (005A) ----

	private static void startHook(ServerPlayer player, HeroState s, WidowmakerState w) {
		if (w.hookPhase != 0 || s.ability1Cooldown > 0 || w.mineCast >= 0 || w.ultCast >= 0 || w.silenced || player.isPassenger()
			|| Sleep.asleep(player)) {
			return;
		}
		w.hookPhase = 1;
		w.hookTicks = 0;
		w.scope = 0;
		w.charge = CHARGE_MIN;
		Combat.animate(player, "grapple_fire");
		WidowmakerSounds.split(player, "grapple.fire", player.getEyePosition(), 40);
	}

	private static void hook(ServerPlayer player, HeroState s, WidowmakerState w, boolean able, boolean jumped, long now) {
		if (w.hookPhase == 0) {
			return;
		}
		w.hookTicks++;
		GrappleHookEntity hook = hookEntity(player, w);
		switch (w.hookPhase) {
			case 1 -> {
				if (!able || w.silenced) {
					w.hookPhase = 0;
					return;
				}
				if (w.hookTicks >= Ow.ticks(HOOK_WINDUP)) {
					// thrown 0.15 s in, 40 m/s to 20 m (D #101 / #15)
					GrappleHookEntity thrown = new GrappleHookEntity(player, player.level());
					thrown.launch(fromEyes(player, HOOK_RIGHT, HOOK_UP, HOOK_AHEAD), player.getViewVector(1.0f), Ow.speed(HOOK_SPEED));
					player.level().addFreshEntity(thrown);
					w.hook = thrown.getUUID();
					w.hookPhase = 2;
					w.hookTicks = 0;
				}
			}
			case 2 -> {
				if (hook == null) {
					// gone without holding (out of the world): as a miss
					w.hookPhase = 0;
				}
			}
			case 3 -> pull(player, s, w, hook, able, jumped, now);
			case 4 -> {
				if (w.hookTicks >= Ow.ticks(PULL_END) || player.onGround() && w.hookTicks > 2) {
					w.hookPhase = 0;
				}
			}
			default -> {
				// reeling back: free again once it's home
				if (hook == null) {
					w.hookPhase = 0;
				}
			}
		}
	}

	/**
	 * The hook held by a block: the pull's point is the top of a ledge within 3 m above it if there's one (D #138), else the point
	 * itself (her chest to it); nearer than 2 m it reels back as a miss (D #266).
	 */
	public static void hookHeld(ServerPlayer owner, GrappleHookEntity hook, BlockHitResult hit) {
		HeroState s = HeroManager.state(owner);
		if (s == null || s.hero != Hero.WIDOWMAKER || !hook.getUUID().equals(s.widowmaker.hook) || s.widowmaker.hookPhase != 2) {
			hook.discard();
			return;
		}
		WidowmakerState w = s.widowmaker;
		Vec3 point = ledge(owner.level(), hit);
		Vec3 feet;
		if (point != null) {
			feet = point.add(0, 0.05, 0);
		} else {
			Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			feet = hit.getLocation().add(n.scale(owner.getBbWidth() / 2 + 0.05)).add(0, -1.1, 0);
		}
		if (owner.position().distanceTo(feet) < PULL_MIN) {
			hook.reel();
			return;
		}
		w.pullX = feet.x;
		w.pullY = feet.y;
		w.pullZ = feet.z;
		w.hookPhase = 3;
		w.hookTicks = 0;
		WidowmakerSounds.split(owner, "grapple.attach", hook.position(), 40);
		WidowmakerSounds.split(owner, "grapple.pull", owner.position(), 30);
		Combat.animate(owner, "grapple_pull");
	}

	/** The top of a ledge within 3 m above where the hook holds: a standable face with two blocks of room over it, or null. */
	static @Nullable Vec3 ledge(ServerLevel level, BlockHitResult hit) {
		if (hit.getDirection() == Direction.DOWN) {
			return null;
		}
		BlockPos base = hit.getBlockPos();
		Vec3 at = hit.getLocation();
		for (int up = 0; up <= (int) Math.ceil(LEDGE_REACH); up++) {
			BlockPos pos = base.above(up);
			BlockPos over = pos.above();
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
				&& level.getBlockState(over).getCollisionShape(level, over).isEmpty()
				&& level.getBlockState(over.above()).getCollisionShape(level, over.above()).isEmpty()) {
				double top = pos.getY() + level.getBlockState(pos).getCollisionShape(level, pos).max(Direction.Axis.Y);
				if (top - at.y > LEDGE_REACH) {
					return null;
				}
				// onto its top, a little in from the face she came at
				Vec3 in = hit.getDirection().getAxis().isHorizontal() ? Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(-0.4) : Vec3.ZERO;
				return new Vec3(at.x, top, at.z).add(in);
			}
		}
		return null;
	}

	private static void pull(ServerPlayer player, HeroState s, WidowmakerState w, @Nullable GrappleHookEntity hook, boolean able,
							 boolean jumped, long now) {
		Vec3 point = new Vec3(w.pullX, w.pullY, w.pullZ);
		Vec3 to = point.subtract(player.position());
		double step = Ow.speed(PULL_SPEED);
		boolean cancel = jumped && w.hookTicks >= Ow.ticks(PULL_JUMP_FROM);
		if (hook == null || !able || w.silenced || player.isPassenger() || cancel || to.length() <= PULL_ARRIVE + step * 0.5
			|| w.hookTicks > Ow.ticks(PULL_MAX_TIME)) {
			endPull(player, s, w, hook, cancel);
			return;
		}
		Vec3 v = to.length() <= step ? to : to.normalize().scale(step);
		w.pullDX = v.x;
		w.pullDY = v.y;
		w.pullDZ = v.z;
		player.setDeltaMovement(v);
		player.fallDistance = 0;
		Combat.send(player);
	}

	/** The pull over (arrived, a jump, stopped): the cooldown starts (D #160 / #179 / #187), some of its speed carries on (M), the hook goes. */
	private static void endPull(ServerPlayer player, HeroState s, WidowmakerState w, @Nullable GrappleHookEntity hook, boolean jumpCancel) {
		if (hook != null) {
			hook.discard();
		}
		w.hook = null;
		w.hookPhase = 4;
		w.hookTicks = 0;
		s.ability1Cooldown = Ow.ticks(HOOK_COOLDOWN);
		if (s.has(Perk.WIDOWMAKER_SNIPERS_INSTINCT)) {
			// Sniper's Instinct: twice the charge for 2 s from the pull's end (D 51EF #354 / #355)
			w.instinctUntil = player.level().getGameTime() + Ow.ticks(INSTINCT_TIME);
		}
		Vec3 last = new Vec3(w.pullDX, w.pullDY, w.pullDZ);
		if (!jumpCancel) {
			// the end's push: a share of the pull's speed, a little up over a ledge (D STU_1F5ED928 0.2, M; ours the share)
			Vec3 keep = last.scale(PULL_KEEP);
			player.setDeltaMovement(new Vec3(keep.x, Math.max(keep.y, 0.12), keep.z));
		} else {
			player.setDeltaMovement(last.scale(0.8).add(0, 0.3, 0));
		}
		Combat.send(player);
		w.launchGrace = 4;
		WidowmakerSounds.split(player, "grapple.end", player.position(), 30);
		Combat.animate(player, "grapple_end");
	}

	/** The hook missed (a creature, its reach, under 2 m): it reels back, no cooldown (D #288 / #162). */
	public static void hookMissed(ServerPlayer owner, GrappleHookEntity hook) {
		HeroState s = HeroManager.state(owner);
		if (s != null && s.hero == Hero.WIDOWMAKER && hook.getUUID().equals(s.widowmaker.hook)) {
			s.widowmaker.hookPhase = 5;
			s.widowmaker.hookTicks = 0;
		}
	}

	/** Whether this hook is the one she has out (else it goes). */
	public static boolean hookIsHers(ServerPlayer owner, GrappleHookEntity hook) {
		HeroState s = HeroManager.state(owner);
		return s != null && s.hero == Hero.WIDOWMAKER && hook.getUUID().equals(s.widowmaker.hook);
	}

	private static @Nullable GrappleHookEntity hookEntity(ServerPlayer player, WidowmakerState w) {
		return w.hook != null && player.level().getEntity(w.hook) instanceof GrappleHookEntity h && h.isAlive() ? h : null;
	}

	// ---- Venom Mine (0056 / 0057 / 0073) ----

	private static void startMine(ServerPlayer player, HeroState s, WidowmakerState w) {
		if (w.mineCast >= 0 || s.ability2Cooldown > 0 || w.hookPhase != 0 && w.hookPhase != 5 || w.ultCast >= 0 || w.silenced
			|| Sleep.asleep(player)) {
			return;
		}
		w.mineCast = 0;
		w.scope = 0;
		w.charge = CHARGE_MIN;
		Combat.animate(player, "mine_throw");
		WidowmakerSounds.split(player, "mine.throw", player.getEyePosition(), 30);
	}

	private static void mineCast(ServerPlayer player, HeroState s, WidowmakerState w, boolean able) {
		if (w.mineCast < 0) {
			return;
		}
		w.mineCast++;
		if (w.mineCast == Ow.ticks(MINE_RELEASE)) {
			if (!able || w.silenced) {
				w.mineCast = -1;
				return;
			}
			// one at a time: the old one goes (D messages 0040 / 003F)
			VenomMineEntity old = mine(player, w);
			if (old != null) {
				old.discard();
			}
			VenomMineEntity mine = new VenomMineEntity(player, player.level());
			mine.launch(fromEyes(player, THROW_RIGHT, THROW_UP, THROW_AHEAD), player.getViewVector(1.0f), Ow.speed(MINE_SPEED));
			player.level().addFreshEntity(mine);
			w.mine = mine.getUUID();
		}
		if (w.mineCast == Ow.ticks(MINE_COOLDOWN_AT)) {
			// 15 s from 1.0 s after the press (D #42 -> #87)
			s.ability2Cooldown = Ow.ticks(MINE_COOLDOWN);
		}
		if (w.mineCast >= Ow.ticks(MINE_CAST)) {
			w.mineCast = -1;
		}
	}

	static @Nullable VenomMineEntity mine(ServerPlayer player, WidowmakerState w) {
		if (w.mine == null) {
			return null;
		}
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			if (level.getEntity(w.mine) instanceof VenomMineEntity m && m.isAlive()) {
				return m;
			}
		}
		return null;
	}

	/** Whether this mine is the one she has out (else it goes: replaced, or she's someone else now). */
	public static boolean stillHers(Entity placed, @Nullable Entity owner) {
		if (!(owner instanceof ServerPlayer player)) {
			return true;
		}
		HeroState s = HeroManager.state(player);
		return s != null && s.hero == Hero.WIDOWMAKER && placed.getUUID().equals(s.widowmaker.mine);
	}

	public static void mineStuck(ServerPlayer owner, VenomMineEntity mine) {
		WidowmakerSounds.all((ServerLevel) mine.level(), "mine.stick", mine.position(), 20);
	}

	public static void mineArmed(ServerPlayer owner, VenomMineEntity mine) {
		WidowmakerSounds.all((ServerLevel) mine.level(), "mine.beep", mine.position(), 20);
	}

	public static void mineBroken(ServerPlayer owner, VenomMineEntity mine) {
		HeroState s = HeroManager.state(owner);
		if (s != null && mine.getUUID().equals(s.widowmaker.mine)) {
			s.widowmaker.mine = null;
		}
	}

	/** The enemies {@code owner} can see from {@code from} within {@code metres} (a creature in sight, not through a wall). */
	static List<LivingEntity> enemiesNear(ServerPlayer owner, ServerLevel level, Vec3 from, double metres, Entity viewer) {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(metres))) {
			if (e.isAlive() && e != owner && Allies.isEnemy(owner, e) && e.getBoundingBox().getCenter().distanceTo(from) <= metres + e.getBbWidth() / 2
				&& Combat.inSight(level, from, e, viewer)) {
				out.add(e);
			}
		}
		return out;
	}

	/** Each tick of an armed mine: an enemy within 3 m sets it off; with Seeker Mine it fires darts instead. */
	public static void mineTick(ServerPlayer owner, VenomMineEntity mine) {
		HeroState s = HeroManager.state(owner);
		if (s == null || s.hero != Hero.WIDOWMAKER) {
			return;
		}
		ServerLevel level = (ServerLevel) mine.level();
		Vec3 at = mine.burstPoint();
		if (s.has(Perk.WIDOWMAKER_SEEKER_MINE)) {
			seekerTick(owner, mine, level, at);
			return;
		}
		if (enemiesNear(owner, level, at, MINE_TRIGGER, mine).isEmpty()) {
			return;
		}
		mineBurst(owner, mine, level, at);
	}

	/** The burst: poison on every enemy within 4 m in sight, each revealed to her (D 0057 #39 / 0073 #48 / #62). */
	public static void mineBurst(ServerPlayer owner, VenomMineEntity mine, ServerLevel level, Vec3 at) {
		WidowmakerSounds.all(level, "mine.trigger", at, 40);
		Effects.play(level, "widow_mine_burst", at, Vec3.ZERO, mine, 0, owner);
		for (LivingEntity e : enemiesNear(owner, level, at, MINE_BURST, mine)) {
			poison(owner, e, POISON_TOTAL, POISON_MINE, MINE_BLOW);
		}
		HeroState s = HeroManager.state(owner);
		if (s != null && mine.getUUID().equals(s.widowmaker.mine)) {
			s.widowmaker.mine = null;
		}
		mine.spent();
	}

	/** Seeker Mine (59BF): armed, a dart at each enemy within 10 m in sight every so often, re-armed 8 s after it fires. */
	static void seekerTick(ServerPlayer owner, VenomMineEntity mine, ServerLevel level, Vec3 at) {
		if (mine.seekerWait > 0) {
			mine.seekerWait--;
			return;
		}
		List<LivingEntity> near = enemiesNear(owner, level, at, SEEKER_RANGE, mine);
		if (near.isEmpty()) {
			return;
		}
		WidowmakerSounds.all(level, "mine.trigger", at, 40);
		for (LivingEntity e : near) {
			Vec3 to = e.getBoundingBox().getCenter();
			Effects.play(level, "widow_dart", at, to.subtract(at), e, 0, owner);
			synchronized (DARTS) {
				DARTS.add(new Dart(e, owner.getUUID(), new int[] {Math.max(1, (int) Math.round(to.distanceTo(at) / Ow.speed(SEEKER_DART_SPEED)))}));
			}
		}
		mine.seekerWait = Ow.ticks(SEEKER_REARM);
	}

	/** The poison on {@code target}: {@code total} over 5 s in 0.2 s steps (again restarts it, D v11844: it doesn't add), revealed to her. */
	static void poison(ServerPlayer owner, LivingEntity target, double total, int variant, Blow blow) {
		int steps = poisonSteps();
		DOTS.put(target.getUUID() + "/" + owner.getUUID(), new Dot(target, owner.getUUID(), total / steps, new int[] {steps, Ow.ticks(POISON_STEP)}, blow));
		ServerLevel level = (ServerLevel) target.level();
		Effects.play(level, "widow_poison", target.position(), Vec3.ZERO, target, variant, owner);
		WidowmakerSounds.split(owner, "mine.poison", target.position(), 30);
		reveal(owner, target, Ow.ticks(MINE_REVEAL));
		if (target instanceof net.minecraft.world.entity.Mob mob) {
			// mobs know who did it (as a hit would)
			mob.setLastHurtByMob(owner);
		}
	}

	/** The poison's steps: 5 s in steps of 0.2 s (25). */
	public static int poisonSteps() {
		return Math.max(1, (int) Math.round(POISON_TIME / POISON_STEP));
	}

	/** Every server tick (Overcraft's): the poisons' steps, the darts' arrivals. */
	public static void tickAll(MinecraftServer server) {
		DOTS.values().removeIf(d -> {
			if (!d.target.isAlive() || d.target.isRemoved() || Combat.isPhased(d.target)) {
				return true;
			}
			if (--d.left[1] > 0) {
				return false;
			}
			d.left[1] = Ow.ticks(POISON_STEP);
			ServerPlayer owner = server.getPlayerList().getPlayer(d.owner);
			if (owner != null) {
				float dealt = dev.overcraft.hero.Barriers.pierce(() -> Combat.damage(d.target, Combat.blast(d.target.level(), null, owner), d.step, d.blow));
				if (d.target != owner) {
					Combat.credit(owner, dealt, d.target);
					Combat.addFireForDamage(owner, dealt, d.target);
				}
			}
			return --d.left[0] <= 0;
		});
		synchronized (DARTS) {
			DARTS.removeIf(d -> {
				if (!d.target.isAlive() || d.target.isRemoved()) {
					return true;
				}
				if (--d.left[0] > 0) {
					return false;
				}
				ServerPlayer owner = server.getPlayerList().getPlayer(d.owner);
				if (owner != null && !Combat.isPhased(d.target)) {
					poison(owner, d.target, SEEKER_TOTAL, POISON_DART, SEEKER_BLOW);
				}
				return true;
			});
		}
	}

	/** A health pack (a potion of Healing: HealOrHarmMixin) clears her venom (W). */
	public static void healthPack(LivingEntity target) {
		DOTS.values().removeIf(d -> d.target == target);
	}

	// ---- Revealed through walls (Venom Mine to her, Infra-Sight to her side) ----

	/** {@code target} outlined for {@code viewer} alone for {@code ticks} (WidowmakerFx keeps it: widow_reveal, the variant its ticks). */
	static void reveal(ServerPlayer viewer, LivingEntity target, int ticks) {
		Effects.sendState(viewer, "widow_reveal", target.position(), Vec3.ZERO, target, ticks, null);
	}

	/** Infra-Sight, every half second: each enemy within 200 m of her, outlined for her and every ally of hers (D #13 / #38 / #116, M whom). */
	private static void infraSight(ServerPlayer widow) {
		ServerLevel level = widow.level();
		List<ServerPlayer> side = new ArrayList<>();
		for (ServerPlayer p : level.players()) {
			if (Allies.isSelfOrAlly(widow, p)) {
				side.add(p);
			}
		}
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, widow.getBoundingBox().inflate(ULT_RANGE))) {
			if (e == widow || !e.isAlive() || !Allies.isEnemy(widow, e) || e.distanceTo(widow) > ULT_RANGE) {
				continue;
			}
			for (ServerPlayer viewer : side) {
				reveal(viewer, e, 12);
			}
		}
	}

	// ---- Infra-Sight (0067) ----

	private static void startUlt(ServerPlayer player, HeroState s, WidowmakerState w) {
		if (w.ultCast >= 0 || w.ultUntil >= 0 || w.silenced || Sleep.asleep(player) || s.ultCharge < Combat.ultCost(s)) {
			return;
		}
		// the charge is spent at the cast's start (D #105)
		s.ultCharge = Combat.afterUlt(Hero.WIDOWMAKER, s.ultCharge);
		w.ultCast = 0;
		w.scope = 0;
		w.charge = CHARGE_MIN;
		Combat.animate(player, "ult_cast");
		WidowmakerSounds.split(player, "ult.cast", player.getEyePosition(), 60);
		ultLine(player);
	}

	private static void ultCast(ServerPlayer player, HeroState s, WidowmakerState w, boolean able, long now) {
		if (w.ultCast < 0) {
			return;
		}
		w.ultCast++;
		if (w.ultCast >= Ow.ticks(ULT_CAST)) {
			// on at the cast's end, 15 s (D #76 / #58)
			w.ultCast = -1;
			w.ultUntil = now + Ow.ticks(ULT_TIME);
			s.ultActive = Ow.ticks(ULT_TIME);
			infraSight(player);
			WidowmakerSounds.own(player, "ult.reveal");
		}
	}

	private static void endInfra(ServerPlayer player, HeroState s, WidowmakerState w) {
		w.ultUntil = -1;
		s.ultActive = 0;
	}

	/** Infra-Sight's cast line: hers in her head, the allies' and the enemies' from her (Ashe's way). */
	private static void ultLine(ServerPlayer hero) {
		net.minecraft.sounds.SoundEvent own = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("widowmaker.voice.ultimate"));
		net.minecraft.sounds.SoundEvent friendly = WidowmakerSounds.of("ult.friendly"), enemy = WidowmakerSounds.of("ult.enemy");
		if (own != null) {
			HeroSounds.toOwner(hero, own);
		}
		for (ServerPlayer listener : hero.level().players()) {
			net.minecraft.sounds.SoundEvent line = Allies.isSelfOrAlly(hero, listener) ? friendly : enemy;
			if (listener != hero && line != null) {
				listener.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
					net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(line), net.minecraft.sounds.SoundSource.PLAYERS,
					hero.getX(), hero.getEyeY(), hero.getZ(), 5, 1.0f, hero.getRandom().nextLong()));
			}
		}
	}

	/** A modifier on her speed while scoped (ADD_MULTIPLIED_TOTAL, so it multiplies the others), none at 0. */
	private static void speed(ServerPlayer player, double amount) {
		AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(SCOPE_SPEED);
		if (amount == 0) {
			instance.removeModifier(SCOPE_SPEED);
		} else if (current == null || current.amount() != amount) {
			instance.removeModifier(SCOPE_SPEED);
			instance.addTransientModifier(new AttributeModifier(SCOPE_SPEED, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	// ---- presses ----

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		WidowmakerState w = s.widowmaker;
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			startHook(player, s, w);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0) {
			startMine(player, s, w);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0) {
			startUlt(player, s, w);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			startReload(player, s, w);
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		WidowmakerState w = s.widowmaker;
		int mode = (w.scope >= 1 ? MODE_SCOPED : 0) | (w.reloadTicks >= 0 ? MODE_RELOAD : 0) | (w.mine != null ? MODE_MINE_OUT : 0)
			| (w.hookPhase != 0 && w.hookPhase != 5 ? MODE_HOOK : 0) | (w.ultCast >= 0 || w.mineCast >= 0 ? MODE_CASTING : 0)
			| (w.ultUntil >= 0 ? MODE_INFRA : 0);
		float reload = w.reloadTicks < 0 ? -1 : (float) Math.min(1, w.reloadTicks / Ow.ticks(RELOAD));
		// the charge's percent in the second ammo's reload share (HeroHud / ReticleHud read it as Widowmaker's): -1 unscoped
		float charge = w.scope >= 1 ? (float) chargePercent(w.charge) : -1;
		float ultLeft = s.ultActive > 0 && w.ultUntil >= 0 ? s.ultActive / 20f : 0;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, s.ultActive > 0,
			s.primaryAmmo, reload, 0, charge, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed,
			s.canChangeHero, s.onFire, dev.overcraft.hero.Perks.level(s), dev.overcraft.hero.Perks.progress(s),
			dev.overcraft.hero.Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), 0, mode, ultLeft, (float) w.scope);
	}

	/**
	 * Out of play (#100): the scope, the reload, the hook and the casts end, her speed goes; Infra-Sight ends on her death (D #113) and
	 * with every other way out (ours). Her mine stays through G and a death (what she put in the world, as every kit's), and goes on a
	 * switch or leaving (HeroKit.OutOfPlay.forGood).
	 */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, HeroKit.OutOfPlay why) {
		WidowmakerState w = s.widowmaker;
		w.scope = 0;
		w.charge = CHARGE_MIN;
		w.reloadTicks = -1;
		w.mineCast = -1;
		w.ultCast = -1;
		if (w.hookPhase == 3 || w.hookPhase == 4) {
			// a pull cut short still costs its cooldown (D #187: an abort)
			s.ability1Cooldown = Ow.ticks(HOOK_COOLDOWN);
		}
		w.hookPhase = 0;
		GrappleHookEntity hook = hookEntity(player, w);
		if (hook != null) {
			hook.discard();
		}
		w.hook = null;
		if (w.ultUntil >= 0) {
			endInfra(player, s, w);
		}
		speed(player, 0);
		player.setAttached(HeroManager.WIDOWMAKER_LOOK, 0);
		if (why.forGood()) {
			clearWorld(player);
			w.mine = null;
		}
	}

	/** Her mine and hook, everywhere: gone. */
	public static void clearWorld(ServerPlayer player) {
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (VenomMineEntity m : level.getEntities(dev.overcraft.registry.OcEntities.VENOM_MINE, m -> m.getOwner() == player)) {
				m.discard();
			}
			for (GrappleHookEntity h : level.getEntities(dev.overcraft.registry.OcEntities.GRAPPLE_HOOK, h -> h.getOwner() == player)) {
				h.discard();
			}
		}
	}
}

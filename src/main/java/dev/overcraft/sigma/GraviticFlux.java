package dev.overcraft.sigma;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.hero.Sleep;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Gravitic Flux (his ult, 1E41; each target runs 1DBC: sigma-map.md). Q: he rises for 0.6 s and flies (gravity off,
 * 7.15 m/s every way, jump up / crouch down, half the knockback: his client's OwMovement) until it ends; the reticle (a
 * ring on the ground where he aims, out to 35 m) waits up to 5 s for the left button, then goes itself; a 1 s cast; then
 * every enemy in a 7.5 m cylinder, 15 m tall from 5 m under the point, in sight of the point, is lifted: 50 damage, held
 * still 0.4 s, lifted 2 s (about 6 m); then slammed down (gravity x22) for half their max health once the ground is
 * within 5 m (no armour, no Resistance, no Protection: the data's "no modifiers"; at most 200 on the three bosses: ours),
 * and 0.6 s at -50% speed after. His lift phase is 2.5 s, then 0.5 s of his slam. Stand-in look and sound (step 10).
 */
public final class GraviticFlux {
	private GraviticFlux() {
	}

	public static final int NONE = 0, START = 1, AIM = 2, CAST = 3, LIFT = 4, SLAM = 5;

	/** The lifted creatures' heavy slow after the lift (not his own: a Stalwart target resists it as any slow). */
	public static final Identifier HEAVY_SLOW = Overcraft.id("sigma_flux_heavy");
	/** The lift's damage (pushing no one: the lift does) and the slam's (no armour, effects or enchantments). */
	private static final ResourceKey<DamageType> LIFT_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Overcraft.id("gravitic_flux"));
	private static final ResourceKey<DamageType> SLAM_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Overcraft.id("gravitic_slam"));

	static boolean active(HeroState s) {
		return s.sigmaFluxPhase != NONE;
	}

	/** He flies (from its start to the end of his slam phase: 1E41 #974 / #977 under every phase). */
	public static boolean flying(HeroState s) {
		return s.sigmaFluxPhase != NONE;
	}

	/** The share of a knockback this entity keeps: Sigma flying in his Flux half (#1071), anyone else all of it. */
	public static double knockbackFactor(@Nullable Entity entity) {
		return entity instanceof Player player && HeroManager.controls(player) && HeroManager.hero(player) == Hero.SIGMA
			&& HeroManager.state(player) instanceof HeroState s && flying(s) ? FLUX_FLIGHT_KNOCKBACK : 1;
	}

	/** Q: it starts, with the ult full and his hands free. */
	static void press(ServerPlayer player, HeroState s) {
		if (active(s) || s.ultCharge < ULT_COST || Accretion.casting(s) || KineticGrasp.active(s) || Sleep.asleep(player)
			|| !player.isAlive()) {
			return;
		}
		s.ultCharge = Combat.afterUlt(Hero.SIGMA, s.ultCharge);
		s.sigmaFluxPhase = START;
		s.sigmaFluxTicks = 0;
		s.sigmaFluxPoint = null;
		s.ultActive = Ow.ticks(FLUX_START + FLUX_AIM + FLUX_CAST + FLUX_LIFT_PHASE + FLUX_SLAM_PHASE);
		// its start (alias 0x1B8, #1020)
		Combat.animate(player, "flux_start");
		player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.6f);
	}

	/** Its end (done, or cut short: G, death, another hero, a stun): he lands safely, those lifted go on falling on their own. */
	static void end(ServerPlayer player, HeroState s) {
		if (!active(s)) {
			return;
		}
		s.sigmaFluxPhase = NONE;
		s.sigmaFluxPoint = null;
		s.ultActive = 0;
		// an ability's landing never hurts (the owner's rule, #35)
		s.softLanding = true;
	}

	static void tick(ServerPlayer player, HeroState s, boolean controls) {
		if (s.softLanding && !active(s)) {
			player.resetFallDistance();
			if (player.onGround()) {
				s.softLanding = false;
			}
		}
		if (!active(s)) {
			return;
		}
		if (!controls || !player.isAlive() || Sleep.asleep(player)) {
			end(player, s);
			return;
		}
		player.resetFallDistance();
		s.sigmaFluxTicks++;
		ServerLevel level = player.level();
		switch (s.sigmaFluxPhase) {
			case START -> {
				if (s.sigmaFluxTicks >= Ow.ticks(FLUX_START)) {
					phase(s, AIM);
					// the targeting loop (0x60F, #1274)
					Combat.animate(player, "flux_aim");
				}
			}
			case AIM -> {
				Vec3 at = aim(player);
				reticle(player, at);
				// the left button (PRIMARY, #1269; the option's right button is not built: Open), or after 5 s by itself (#1294)
				if (s.primaryFresh || s.sigmaFluxTicks >= Ow.ticks(FLUX_AIM)) {
					s.sigmaFluxPoint = at;
					phase(s, CAST);
					// the cast (0x691, #1089)
					Combat.animate(player, "flux_cast");
					level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2f, 0.5f);
				}
			}
			case CAST -> {
				Vec3 at = s.sigmaFluxPoint;
				if (at != null && s.sigmaFluxTicks % 2 == 0) {
					ring(level, null, at, FLUX_RADIUS, ParticleTypes.REVERSE_PORTAL, 48);
				}
				if (s.sigmaFluxTicks >= Ow.ticks(FLUX_CAST)) {
					List<LivingEntity> targets = at == null ? List.of() : targets(player, at);
					for (LivingEntity e : targets) {
						lift(player, e, at);
					}
					// none caught: straight to his slam phase (#995 -> #996 v59 = 4)
					phase(s, targets.isEmpty() ? SLAM : LIFT);
					Combat.animate(player, targets.isEmpty() ? "flux_slam" : "flux_lift");
					if (at != null) {
						level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.6f);
					}
				}
			}
			case LIFT -> {
				if (s.sigmaFluxTicks >= Ow.ticks(FLUX_LIFT_PHASE)) {
					phase(s, SLAM);
					Combat.animate(player, "flux_slam");
				}
			}
			case SLAM -> {
				if (s.sigmaFluxTicks >= Ow.ticks(FLUX_SLAM_PHASE)) {
					end(player, s);
				}
			}
			default -> end(player, s);
		}
	}

	private static void phase(HeroState s, int phase) {
		s.sigmaFluxPhase = phase;
		s.sigmaFluxTicks = 0;
	}

	/**
	 * Where he aims (#1268): the first block along his aim out to 35 m; past it, the ground under the ray's end (within its
	 * 15 m height; else the end itself: ours, M).
	 */
	static Vec3 aim(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(FLUX_RANGE));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.MISS) {
			return hit.getLocation();
		}
		BlockHitResult down = level.clip(new ClipContext(end, end.subtract(0, FLUX_HEIGHT, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		return down.getType() != HitResult.Type.MISS ? down.getLocation() : end;
	}

	/** The reticle, his alone: a ring the cylinder's size where it would go, and its centre (stand-in: his own effect 2C68 in step 10). */
	private static void reticle(ServerPlayer player, Vec3 at) {
		if (player.tickCount % 2 == 0) {
			ring(player.level(), player, at.add(0, 0.1, 0), FLUX_RADIUS, new DustParticleOptions(0x8A7CFF, 1.5f), 40);
			player.level().sendParticles(player, ParticleTypes.END_ROD, true, true, at.x, at.y + 0.2, at.z, 2, 0.1, 0.1, 0.1, 0);
		}
	}

	private static void ring(ServerLevel level, @Nullable ServerPlayer only, Vec3 at, double radius,
							 net.minecraft.core.particles.ParticleOptions particle, int count) {
		for (int i = 0; i < count; i++) {
			double a = Math.PI * 2 * i / count;
			double x = at.x + Math.cos(a) * radius, z = at.z + Math.sin(a) * radius;
			if (only != null) {
				level.sendParticles(only, particle, true, true, x, at.y, z, 1, 0, 0, 0, 0);
			} else {
				level.sendParticles(particle, x, at.y, z, 1, 0, 0.05, 0, 0);
			}
		}
	}

	/**
	 * Every enemy in the cylinder (7.5 m about the point, from 5 m under it to 10 m over: #979 / #1292) with a clear line
	 * from 1 m over the point (#979 m_47AB1887), not already lifted.
	 */
	static List<LivingEntity> targets(ServerPlayer owner, Vec3 at) {
		ServerLevel level = owner.level();
		double bottom = at.y - FLUX_BELOW, top = bottom + FLUX_HEIGHT;
		List<LivingEntity> out = new ArrayList<>();
		Vec3 eye = at.add(0, 1, 0);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at.x - FLUX_RADIUS - 3, bottom, at.z - FLUX_RADIUS - 3,
			at.x + FLUX_RADIUS + 3, top, at.z + FLUX_RADIUS + 3))) {
			if (!e.isAlive() || e == owner || !Allies.canHurt(owner, e) || e.isSpectator() || LIFTED.containsKey(e.getUUID())) {
				continue;
			}
			AABB box = e.getBoundingBox();
			if (box.maxY < bottom || box.minY > top) {
				continue;
			}
			double dx = Math.max(0, Math.max(box.minX - at.x, at.x - box.maxX)), dz = Math.max(0, Math.max(box.minZ - at.z, at.z - box.maxZ));
			if (dx * dx + dz * dz > FLUX_RADIUS * FLUX_RADIUS) {
				continue;
			}
			boolean seen = false;
			for (Vec3 to : new Vec3[] {box.getCenter(), e.getEyePosition(), e.position().add(0, 0.1, 0)}) {
				if (level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
					seen = true;
					break;
				}
			}
			if (seen) {
				out.add(e);
			}
		}
		return out;
	}

	// ---- the lifted (1DBC on each target) ----

	/** One lifted creature: by whom, toward where, ticks since its lift, whether slammed yet and when, its AI before. */
	private static final class Lifted {
		final LivingEntity entity;
		final ServerPlayer owner;
		final Vec3 centre;
		int ticks;
		int slamAt = -1;
		final boolean hadNoAi;

		Lifted(LivingEntity entity, ServerPlayer owner, Vec3 centre, boolean hadNoAi) {
			this.entity = entity;
			this.owner = owner;
			this.centre = centre;
			this.hadNoAi = hadNoAi;
		}
	}

	private static final Map<UUID, Lifted> LIFTED = new HashMap<>();

	/** Being lifted or slammed by a Gravitic Flux now (Sleep leaves its movement to this). */
	public static boolean lifted(LivingEntity entity) {
		return LIFTED.containsKey(entity.getUUID());
	}

	private static void lift(ServerPlayer owner, LivingEntity e, Vec3 at) {
		ServerLevel level = owner.level();
		boolean hadNoAi = e instanceof Mob mob && Sleep.hadNoAi(mob);
		LIFTED.put(e.getUUID(), new Lifted(e, owner, at, hadNoAi));
		Vec3 before = e.getDeltaMovement();
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LIFT_TYPE), owner);
		float dealt = hurt(level, e, source, FLUX_LIFT_DAMAGE);
		Combat.credit(owner, dealt, e);
		Combat.addFireForDamage(owner, dealt, Ow.toOw(e.getMaxHealth()));
		if (dealt > 0) {
			Effects.hitMarker(owner, e, false);
		}
		// pulled 1 m/s in toward the point (#575 m_E33EFEE1 -1, M)
		Vec3 in = new Vec3(at.x - e.getX(), 0, at.z - e.getZ());
		in = in.lengthSqr() < 1.0E-6 ? Vec3.ZERO : in.normalize();
		e.setDeltaMovement(new Vec3(before.x, 0, before.z).add(in.scale(Ow.speed(FLUX_LIFT_PULL))));
		if (e instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setTarget(null);
		}
		if (e instanceof ServerPlayer p) {
			// held still 0.4 s (#586: no move, no jump, no abilities): their client, as asleep
			p.setAttached(HeroManager.ASLEEP, true);
		}
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
	}

	/** Deals Overwatch damage to a creature (the Ender Dragon through its head: its body takes a quarter). */
	private static float hurt(ServerLevel level, LivingEntity e, DamageSource source, double ow) {
		return e instanceof EnderDragon dragon ? Combat.damagePart(level, dragon.head, source, ow) : Combat.damage(e, source, ow);
	}

	/** The three bosses (the slam's cap: ours). */
	static boolean boss(LivingEntity e) {
		return e instanceof EnderDragon || e instanceof WitherBoss || e instanceof Warden;
	}

	/** The slam's damage on a creature of {@code maxOw} Overwatch health: half of it, at most 200 on a boss. */
	public static double slamDamage(double maxOw, boolean boss) {
		double d = maxOw * FLUX_SLAM_SHARE;
		return boss ? Math.min(FLUX_BOSS_CAP, d) : d;
	}

	/** The vertical speed (m/s) of a lifted creature after one tick at {@code t} s into its lift, from {@code vy}. */
	public static double liftSpeed(double vy, double t) {
		double rise = FLUX_LIFT_TIME - t < FLUX_RISE_END_TIME ? FLUX_RISE_END : FLUX_RISE;
		vy += (rise - OwMovement.GRAVITY_DOWN * FLUX_LIFT_GRAVITY) * 0.05;
		return t >= FLUX_HOLD ? Math.min(FLUX_RISE_CAP, vy) : vy;
	}

	/** Every server tick (HeroManager): the lifted rise, then fall heavily and are slammed, and let go. */
	public static void tickAll(MinecraftServer server) {
		if (LIFTED.isEmpty()) {
			return;
		}
		for (Lifted l : new ArrayList<>(LIFTED.values())) {
			LivingEntity e = l.entity;
			if (e.isRemoved() || !e.isAlive()) {
				release(l);
				continue;
			}
			double t = l.ticks * 0.05;
			l.ticks++;
			e.resetFallDistance();
			Vec3 v = e.getDeltaMovement();
			double vy;
			double hx = v.x, hz = v.z;
			if (t < FLUX_LIFT_TIME) {
				vy = Ow.speed(liftSpeed(v.y * 20, t));
				// drifting: no input for a mob (its AI is off), air drag as Minecraft's
				hx *= 0.91;
				hz *= 0.91;
				if (l.ticks == Ow.ticks(FLUX_HOLD) && e instanceof ServerPlayer p) {
					p.removeAttached(HeroManager.ASLEEP);
				}
			} else {
				// the slam: gravity x22 (#582) to Overwatch's terminal speed; damage once ground is within 5 m below (#644)
				if (l.slamAt < 0 && t <= FLUX_LIFT_TIME + 0.05) {
					Combat.buff(e, Attributes.MOVEMENT_SPEED, HEAVY_SLOW, -FLUX_HEAVY_SLOW, 4);
				}
				vy = Math.max(-Ow.speed(OwMovement.TERMINAL_SPEED), v.y - Ow.accel(OwMovement.GRAVITY_DOWN * FLUX_HEAVY_GRAVITY));
				hx *= 0.91;
				hz *= 0.91;
				if (l.slamAt < 0 && groundWithin(e, FLUX_SLAM_GROUND)) {
					slam(l);
				}
				// held 0.6 s after the slam (#585), at most 4 s with no ground found (#594)
				if (l.slamAt >= 0 && l.ticks - l.slamAt >= Ow.ticks(FLUX_HEAVY_TIME) || t >= FLUX_LIFT_TIME + 4) {
					release(l);
					continue;
				}
			}
			e.setDeltaMovement(hx, vy, hz);
			if (e instanceof Mob mob) {
				// its AI is off, and with it Minecraft's movement: moved here
				mob.setNoAi(true);
				if (!(e instanceof EnderDragon)) {
					mob.move(MoverType.SELF, e.getDeltaMovement());
				}
				mob.needsSync = true;
			} else {
				// a player moves on their own client: sent their speed every tick
				e.needsSync = true;
			}
		}
	}

	private static boolean groundWithin(LivingEntity e, double metres) {
		if (e.onGround()) {
			return true;
		}
		Vec3 from = e.position();
		return e.level().clip(new ClipContext(from, from.subtract(0, metres, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, e))
			.getType() != HitResult.Type.MISS;
	}

	private static void slam(Lifted l) {
		LivingEntity e = l.entity;
		l.slamAt = l.ticks;
		ServerLevel level = (ServerLevel) e.level();
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SLAM_TYPE), l.owner);
		float dealt = hurt(level, e, source, slamDamage(Ow.toOw(e.getMaxHealth()), boss(e)));
		Combat.credit(l.owner, dealt, e);
		Combat.addFireForDamage(l.owner, dealt, Ow.toOw(e.getMaxHealth()));
		if (dealt > 0) {
			Effects.hitMarker(l.owner, e, false);
		}
		level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
		level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1f, 0.8f);
	}

	private static void release(Lifted l) {
		LIFTED.remove(l.entity.getUUID());
		LivingEntity e = l.entity;
		Combat.buff(e, Attributes.MOVEMENT_SPEED, HEAVY_SLOW, 0, 0);
		if (e instanceof Mob mob && !Sleep.asleep(mob)) {
			mob.setNoAi(l.hadNoAi);
		}
		if (e instanceof ServerPlayer p && !Sleep.asleep(p)) {
			p.removeAttached(HeroManager.ASLEEP);
		}
		e.resetFallDistance();
	}

	/** Leaving the world: nothing held over. */
	public static void clearAll() {
		for (Lifted l : new ArrayList<>(LIFTED.values())) {
			release(l);
		}
	}
}

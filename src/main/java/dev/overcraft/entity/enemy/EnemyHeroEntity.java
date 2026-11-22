package dev.overcraft.entity.enemy;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Function;

/**
 * An enemy hero (#46, tools/reference/enemy-heroes-plan.md): a real Minecraft monster drawn with a hero's model and fighting with
 * a cut-down kit, on Minecraft's own pathing and targeting (Monster, the goal system). A subclass per hero gives its kit.
 * <ul>
 * <li><b>Side:</b> an {@link Enemy} with no owner and no team, so every hero ability takes it as an enemy, and its kills pay ult
 * charge and reach the kill feed. Its own shots take players, their pets, villagers and golems, never another monster
 * (Allies.friendly's enemy-hero branch, ours).</li>
 * <li><b>Look:</b> it carries the hero's id in HeroManager.HERO (synced), so the client's HeroModelRenderer and HeroAnimator draw
 * it as that hero (EnemyHeroRenderer); its one-shot clips go out through Combat.animate(Mob, clip). Every other user of
 * HeroManager.HERO reads it off a Player only (audited, #46).</li>
 * <li><b>Damage:</b> the hero's Overwatch damage, x the difficulty's factor on a player (EnemyHeroStats.playerDamageFactor),
 * through Combat.damage with the hero named on the blow. Its own damage types (overcraft:enemy_shot, overcraft:enemy_blast)
 * never scale with the difficulty again: the factor is the whole scaling.</li>
 * </ul>
 */
public abstract class EnemyHeroEntity extends Monster {
	protected EnemyHeroEntity(EntityType<? extends EnemyHeroEntity> type, Level level) {
		super(type, level);
		xpReward = EnemyHeroStats.XP;
	}

	/** Its health (Overwatch's, at the mod's 250 = 20 scale), its run and its sight. */
	public static AttributeSupplier.Builder attributes(int owHealth, double runMetresPerSecond) {
		return attributes(owHealth, runMetresPerSecond, EnemyHeroStats.FOLLOW_RANGE);
	}

	/** The same with its own sight ({@code followRange} m: Widowmaker's, EnemyHeroStats.WIDOW_FOLLOW_RANGE). */
	public static AttributeSupplier.Builder attributes(int owHealth, double runMetresPerSecond, double followRange) {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Ow.hp(owHealth))
			.add(Attributes.MOVEMENT_SPEED, EnemyHeroStats.speedAttribute(runMetresPerSecond))
			.add(Attributes.FOLLOW_RANGE, followRange);
	}

	public abstract Hero hero();

	/** The range it keeps from its target (m): it closes in past far, backs off inside near. */
	protected abstract double near();

	protected abstract double far();

	/** Each tick of a fight, after the goal has moved and turned it: its weapon and abilities. */
	protected abstract void fight(ServerLevel level, LivingEntity target, double distance, boolean sees);

	/** Something hurt it (a reaction: a Blink away, a Combat Roll). */
	protected void hurtBy(ServerLevel level, LivingEntity attacker) {
	}

	/** Its themed drop (the plan's: redstone, iron nuggets, gunpowder, firework rockets). */
	protected abstract Item dropItem();

	/** True while it moves itself and the ground navigation would fight it (Pharah in the air). */
	protected boolean steersItself() {
		return false;
	}

	/** Its sound "hero." + name (the 3p copy when there is one), or null. */
	protected abstract @Nullable SoundEvent sound(String name);

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new FightGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, EnemyHeroStats.WANDER_SHARE));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		// as a skeleton's: whoever hurt it, the nearest player, an iron golem; and a player's pet (ours: they're the player's side)
		targetSelector.addGoal(1, new HurtByTargetGoal(this, Enemy.class));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
		targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
			(target, level) -> target instanceof OwnableEntity pet && pet.getOwner() instanceof Player));
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && !hero().id.equals(getAttached(HeroManager.HERO))) {
			// the hero it's drawn as, synced to every client (the renderer reads it as it reads a player's)
			setAttached(HeroManager.HERO, hero().id);
		}
		if (!level().isClientSide()) {
			// a stealthed Sombra it can't see goes at once, whatever set her (owner, 2026-11-22, #185: an Ashe kept shooting her
			// from 10 m after her reveal ended)
			if (getTarget() != null && dev.overcraft.sombra.Stealth.hiddenFrom(getTarget(), this)) {
				setTarget(null);
			}
			noteTarget();
			leaveAtDay();
		}
	}

	/**
	 * They came with the night, and leave once day is here (owner, 2026-11-18, #46: "they should leave once the day is here"; a
	 * zombie burns, these go). Checked once a second: in the Overworld, when it's no longer dark outside (Level.isDarkOutside, the
	 * same test their spawn makes, so a thunderstorm's dark keeps them), a puff of smoke and gone. Not one kept on purpose (a name
	 * tag, /summon with PersistenceRequired). His turret follows him 2 s later (EnemyTurretEntity).
	 */
	private void leaveAtDay() {
		if (tickCount % 20 != 0 || !(level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
			|| level.isDarkOutside() || isPersistenceRequired() || !isAlive()) {
			return;
		}
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 20,
			getBbWidth() / 2, getBbHeight() / 3, getBbWidth() / 2, 0.02);
		discard();
	}

	/** The target it had last tick (server; by identity: a cast or an aim belongs to one). */
	private @Nullable LivingEntity lastTarget;

	/** Its target changed or went: a kit drops its cast or aim (Coach Gun's, the turret's throw, an aim down the sights). */
	protected void targetChanged() {
	}

	/** A new target, or none: whatever was cast or aimed at the old one stops (the critique of 29c1797). Before each fight tick too. */
	private void noteTarget() {
		if (getTarget() != lastTarget) {
			lastTarget = getTarget();
			targetChanged();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		// (a monster's stray arrow isn't a fight: HurtByTargetGoal ignores it too, critique of c728bea)
		// not while it's held (asleep, stunned, knocked down: Sleep) or hacked (no abilities: Hack): a Blink or a Combat Roll out of
		// them was the critique of #46's (reproduced: a hacked enemy Cassidy rolled when hit from close)
		if (hurt && isAlive() && source.getEntity() instanceof LivingEntity attacker && attacker != this
			&& !(attacker instanceof net.minecraft.world.entity.monster.Enemy)
			&& !dev.overcraft.hero.Sleep.asleep(this) && !dev.overcraft.sombra.Hack.silenced(this)) {
			hurtBy(level, attacker);
		}
		return hurt;
	}

	/** Its name over its head: the hero's in red capitals ("TRACER", the lang file), always shown (ours). */
	@Override
	protected Component getTypeName() {
		return Component.translatable(getType().getDescriptionId()).withStyle(ChatFormatting.RED);
	}

	@Override
	public boolean shouldShowName() {
		return true;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		int count = EnemyHeroStats.DROP_MIN + random.nextInt(EnemyHeroStats.DROP_MAX - EnemyHeroStats.DROP_MIN + 1);
		spawnAtLocation(level, new ItemStack(dropItem(), count));
	}

	// ---- what the kits share ----

	/** Its Overwatch damage on {@code target}: x the difficulty's factor on a player (EnemyHeroStats), as dealt. */
	public double scaled(LivingEntity target, double owDamage) {
		return target instanceof Player ? owDamage * EnemyHeroStats.playerDamageFactor(level().getDifficulty()) : owDamage;
	}

	/** Deals {@code owDamage} (before the difficulty's factor) to {@code target}, the blow its hero's. Returns what was dealt (OW). */
	public float deal(LivingEntity target, DamageSource source, double owDamage, Blow blow) {
		double d = scaled(target, owDamage);
		if (d <= 0 || !target.isAlive()) {
			return 0;
		}
		return Combat.damage(target, source, d, blow.by(hero()));
	}

	/** A shot of its (hitscan, or a projectile's direct hit when {@code direct}): overcraft:enemy_shot, never difficulty-scaled. */
	public DamageSource shotSource(@Nullable Entity direct) {
		return source("enemy_shot", direct);
	}

	/** Its explosions' splash: overcraft:enemy_blast (an explosion, never difficulty-scaled). */
	public DamageSource blastSource(@Nullable Entity direct) {
		return source("enemy_blast", direct);
	}

	private DamageSource source(String type, @Nullable Entity direct) {
		return new DamageSource(level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
			.getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, Overcraft.id(type))), direct, this);
	}

	/** The hero's own spread plus the difficulty's (EnemyHeroStats.extraSpread), degrees. */
	protected double spread(double own) {
		return own + EnemyHeroStats.extraSpread(level().getDifficulty());
	}

	/** Where it aims a shot at {@code target}: the middle of its box, leading nothing (a skeleton's aim, ours). */
	protected static Vec3 bodyOf(LivingEntity target) {
		return target.getBoundingBox().getCenter();
	}

	/** A point from its eyes in the frame of {@code look} (right, up, ahead), or its eyes when a wall is between. */
	protected Vec3 fromEyes(Vec3 look, double right, double up, double ahead) {
		Vec3 eye = getEyePosition();
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 top = side.cross(look).normalize();
		Vec3 at = eye.add(side.scale(right)).add(top.scale(up)).add(look.scale(ahead));
		return dev.overcraft.hero.Rays.clip(level(), new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
			== net.minecraft.world.phys.HitResult.Type.MISS ? at : eye;
	}

	/** A hitscan line's end: the creature it took (null: none) at {@code at}, {@code metres} out, and the block it stopped at. */
	public record Traced(@Nullable LivingEntity target, Vec3 at, double metres, BlockHitResult block) {
	}

	/** {@link #trace(ServerLevel, Vec3, Vec3, double, double, java.util.function.DoubleUnaryOperator)} with no damage: a look, not a shot. */
	protected Traced trace(ServerLevel level, Vec3 from, Vec3 dir, double range, double radius) {
		return trace(level, from, dir, range, radius, null);
	}

	/**
	 * One hitscan line from {@code from} along {@code dir}: the first creature it may hurt (Allies.canHurt: not another monster)
	 * whose box, grown by {@code radius}, it crosses before a block. A player's barrier on the way stops it there and takes the
	 * shot: {@code damageAt} of the metres to it (Overwatch points), as a player's hitscan charges one (Barriers.absorb) and the
	 * enemy turret's does. Before, the line only dropped a creature behind a barrier: the barrier took nothing, so it held for
	 * ever (the critique of #46, reproduced: an enemy Cassidy at a Sigma barrier for 12 s left it at 650 of 650).
	 */
	protected Traced trace(ServerLevel level, Vec3 from, Vec3 dir, double range, double radius,
						   java.util.function.@Nullable DoubleUnaryOperator damageAt) {
		Vec3 end = from.add(dir.scale(range));
		BlockHitResult block = dev.overcraft.hero.Rays.clip(level, new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		Vec3 stop = block.getLocation();
		LivingEntity best = null;
		Vec3 bestAt = stop;
		double bestDist = from.distanceToSqr(stop);
		for (Entity e : level.getEntities(this, new AABB(from, stop).inflate(1))) {
			if (!(e instanceof LivingEntity living) || !living.isAlive() || !Allies.canHurt(this, living)) {
				continue;
			}
			java.util.Optional<Vec3> hit = dev.overcraft.hero.Combat.clipFrom(e.getBoundingBox().inflate(radius), from, stop);
			if (hit.isPresent() && hit.get().distanceToSqr(from) < bestDist) {
				best = living;
				bestAt = hit.get();
				bestDist = hit.get().distanceToSqr(from);
			}
		}
		// a barrier before whatever the line reached (a creature, or the block when there's none) stops it there
		Barriers.Shape barrier = Barriers.between(level, from, bestAt, this);
		if (barrier != null) {
			Vec3 on = Barriers.crossing(barrier, from, bestAt);
			if (on != null) {
				if (damageAt != null) {
					barrier.barrierTake(damageAt.applyAsDouble(on.distanceTo(from)), on);
				}
				bestAt = on;
				bestDist = on.distanceToSqr(from);
			}
			best = null;
		}
		return new Traced(best, bestAt, Math.sqrt(bestDist), block);
	}

	/** Its sound {@code name} at {@code at} for everyone out to {@code metres} (null-safe: a sound not in the pack is silent). */
	protected void play(ServerLevel level, String name, Vec3 at, double metres) {
		SoundEvent sound = sound(name);
		if (sound != null) {
			HeroSounds.at(level, sound, at, metres);
		}
	}

	/** The 3p copy of a hero's split sound when it has one, else the sound itself. */
	protected static @Nullable SoundEvent thirdPerson(Function<String, @Nullable SoundEvent> of, String name) {
		SoundEvent others = of.apply(name + "_3p");
		return others != null ? others : of.apply(name);
	}

	/** Pauses between bursts, 0.5 to 1 s (EnemyHeroStats.pauseTicks). */
	protected int pause() {
		return EnemyHeroStats.pauseTicks(random.nextDouble());
	}

	/**
	 * Its fight, as a skeleton's bow goal moves one (RangedBowAttackGoal, Minecraft's own): it walks to its target while it's past
	 * {@link #far} or out of sight, else stands and strafes, backing off inside {@link #near}, turning to face it; the hero's kit
	 * then fires ({@link #fight}).
	 */
	private static final class FightGoal extends Goal {
		private final EnemyHeroEntity mob;
		private int strafeTime = -1;
		private boolean clockwise;
		private boolean backwards;

		FightGoal(EnemyHeroEntity mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = mob.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void start() {
			mob.setAggressive(true);
		}

		@Override
		public void stop() {
			mob.setAggressive(false);
			strafeTime = -1;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = mob.getTarget();
			if (target == null || !(mob.level() instanceof ServerLevel level)) {
				return;
			}
			double distance = mob.distanceTo(target);
			boolean sees = mob.getSensing().hasLineOfSight(target);
			if (mob.steersItself()) {
				mob.getNavigation().stop();
				mob.lookAt(target, 30, 30);
				mob.getLookControl().setLookAt(target, 30, 30);
			} else if (distance > mob.far() || !sees) {
				mob.getNavigation().moveTo(target, 1.0);
				mob.getLookControl().setLookAt(target, 30, 30);
				strafeTime = -1;
			} else {
				mob.getNavigation().stop();
				if (++strafeTime >= 20) {
					// as RangedBowAttackGoal: now and then the other way round, or back and forth
					if (mob.getRandom().nextFloat() < 0.3f) {
						clockwise = !clockwise;
					}
					strafeTime = 0;
				}
				if (distance < mob.near()) {
					backwards = true;
				} else if (distance > (mob.near() + mob.far()) / 2) {
					backwards = false;
				}
				mob.getMoveControl().strafe(backwards ? -0.5f : 0.5f, clockwise ? 0.5f : -0.5f);
				mob.lookAt(target, 30, 30);
			}
			mob.noteTarget();
			mob.fight(level, target, distance, sees);
		}
	}
}

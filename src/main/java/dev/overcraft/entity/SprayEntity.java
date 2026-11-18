package dev.overcraft.entity;

import dev.overcraft.registry.OcEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A spray (#41): a hero's spray image flat on a block face, put down by a player. One per player: a new one replaces their last.
 * The image is the player's own from their Overwatch install, in the local asset pack (textures/sprays/<hero>/<name>.png); a
 * client without it draws a plain stand-in. Its numbers are ours until tools/reference/sprays.md gives Overwatch's.
 */
public class SprayEntity extends Entity implements SurfaceStuck {
	/** How far a spray reaches (ours: no public source or the data gives it, tools/reference/sprays.md). */
	public static final double RANGE = 4.5;
	/** Its larger side on the wall, metres (ours, as RANGE); the other side keeps the image's ratio. */
	public static final float SIZE = 1.25f;

	private static final EntityDataAccessor<String> HERO = SynchedEntityData.defineId(SprayEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> NAME = SynchedEntityData.defineId(SprayEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(SprayEntity.class, EntityDataSerializers.INT);

	private @Nullable UUID owner;
	private int age;
	/** The block it's on (a fence's or wall's collision box reaches above its block: the hit point alone isn't in it). */
	private @Nullable BlockPos on;
	/** When it was put down (the overworld's game time, saved with the world): the newest one of its owner's stays. */
	private long placed;
	/**
	 * Each owner's newest spray, by when it was put down. A spray in a chunk or dimension that wasn't loaded when its owner sprayed
	 * again wasn't erased then (a critique, reproduced: spray, travel 1,100 m, spray, return: both there); it goes when it next
	 * loads and meets a newer one (and the newest goes on with it across a restart, since each spray saves its time).
	 */
	private static final java.util.Map<UUID, Long> NEWEST = new java.util.HashMap<>();
	/** The least time between two sprays of one player (ours: a client sending the packet in a loop made and broadcast a spray a packet). */
	private static final long SPRAY_GAP = 10;
	private static final java.util.Map<UUID, Long> LAST = new java.util.HashMap<>();

	public SprayEntity(EntityType<? extends SprayEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(HERO, "");
		builder.define(NAME, "");
		builder.define(FACE, Direction.NORTH.get3DDataValue());
	}

	public String hero() {
		return entityData.get(HERO);
	}

	public String sprayName() {
		return entityData.get(NAME);
	}

	@Override
	public @Nullable Direction stuckFace() {
		return SurfaceStuck.decode(entityData.get(FACE));
	}

	/**
	 * {@code player} sprays {@code name} of {@code hero} on the block they look at within reach: their last spray goes. False when
	 * nothing's in reach (nothing put down).
	 */
	public static boolean place(ServerPlayer player, String hero, String name) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		if (!player.isAlive() || player.isSpectator()) {
			return false;
		}
		long now = level.getServer().overworld().getGameTime();
		Long last = LAST.get(player.getUUID());
		if (last != null && now - last >= 0 && now - last < SPRAY_GAP) {
			return false;
		}
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(RANGE)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		erase(player);
		SprayEntity spray = new SprayEntity(OcEntities.SPRAY, level);
		Direction face = hit.getDirection();
		Vec3 n = Vec3.atLowerCornerOf(face.getUnitVec3i());
		// just off the face, so it doesn't fight the block for the same pixels
		Vec3 at = hit.getLocation().add(n.scale(0.01));
		spray.setPos(at.x, at.y, at.z);
		spray.setYRot(face.getAxis().isHorizontal() ? face.toYRot() : player.getYRot());
		spray.owner = player.getUUID();
		spray.on = hit.getBlockPos();
		spray.placed = now;
		NEWEST.put(player.getUUID(), now);
		LAST.put(player.getUUID(), now);
		spray.entityData.set(HERO, hero);
		spray.entityData.set(NAME, name);
		spray.entityData.set(FACE, face.get3DDataValue());
		level.addFreshEntity(spray);
		return true;
	}

	/** {@code player}'s spray goes (one each: a new one, or a new hero, erases it). */
	public static void erase(ServerPlayer player) {
		// every dimension's loaded sprays; an unloaded one goes when it loads (NEWEST)
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (SprayEntity old : level.getEntities(OcEntities.SPRAY, e -> player.getUUID().equals(e.owner))) {
				old.discard();
			}
		}
		// a hero change erases it with nothing newer placed: no spray of theirs from before now stays when it loads
		NEWEST.put(player.getUUID(), player.level().getServer().overworld().getGameTime());
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		// no timer: it stays until its player sprays again or changes hero (Overwatch's, tools/reference/sprays.md)
		age++;
		if (owner != null) {
			Long newest = NEWEST.get(owner);
			if (newest != null && newest > placed) {
				discard();
				return;
			}
			if (newest == null) {
				NEWEST.put(owner, placed);
			}
		}
		// the block it's on went: so does the spray
		Direction face = stuckFace();
		if (face != null && age % 10 == 0) {
			BlockPos on = this.on != null ? this.on : BlockPos.containing(position().subtract(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.05)));
			if (level.getBlockState(on).getCollisionShape(level, on).isEmpty()) {
				discard();
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	/** Pistons don't carry it off its face (it stays, or goes with the block). */
	@Override
	public net.minecraft.world.level.material.PushReaction getPistonPushReaction() {
		return net.minecraft.world.level.material.PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(HERO, input.getStringOr("SprayHero", ""));
		entityData.set(NAME, input.getStringOr("SprayName", ""));
		entityData.set(FACE, input.getIntOr("SprayFace", Direction.NORTH.get3DDataValue()));
		age = input.getIntOr("SprayAge", 0);
		placed = input.getLongOr("SprayPlaced", 0);
		long block = input.getLongOr("SprayBlock", Long.MIN_VALUE);
		on = block == Long.MIN_VALUE ? null : BlockPos.of(block);
		owner = input.getString("SprayOwner").map(s -> {
			try {
				return UUID.fromString(s);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}).orElse(null);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putString("SprayHero", hero());
		output.putString("SprayName", sprayName());
		output.putInt("SprayFace", entityData.get(FACE));
		output.putInt("SprayAge", age);
		output.putLong("SprayPlaced", placed);
		if (on != null) {
			output.putLong("SprayBlock", on.asLong());
		}
		if (owner != null) {
			output.putString("SprayOwner", owner.toString());
		}
	}
}

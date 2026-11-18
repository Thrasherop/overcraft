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
	/** How far a spray reaches (ours, pending the reference). */
	public static final double RANGE = 4.5;
	/** Its width and height on the wall, metres (ours, pending the reference). */
	public static final float SIZE = 1.25f;
	/** How long it stays, seconds (ours, pending the reference). */
	public static final double LIFE = 600;

	private static final EntityDataAccessor<String> HERO = SynchedEntityData.defineId(SprayEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> NAME = SynchedEntityData.defineId(SprayEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(SprayEntity.class, EntityDataSerializers.INT);

	private @Nullable UUID owner;
	private int age;

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
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(RANGE)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		for (SprayEntity old : level.getEntities(OcEntities.SPRAY, e -> player.getUUID().equals(e.owner))) {
			old.discard();
		}
		SprayEntity spray = new SprayEntity(OcEntities.SPRAY, level);
		Direction face = hit.getDirection();
		Vec3 n = Vec3.atLowerCornerOf(face.getUnitVec3i());
		// just off the face, so it doesn't fight the block for the same pixels
		Vec3 at = hit.getLocation().add(n.scale(0.01));
		spray.setPos(at.x, at.y, at.z);
		spray.setYRot(face.getAxis().isHorizontal() ? face.toYRot() : player.getYRot());
		spray.owner = player.getUUID();
		spray.entityData.set(HERO, hero);
		spray.entityData.set(NAME, name);
		spray.entityData.set(FACE, face.get3DDataValue());
		level.addFreshEntity(spray);
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (++age > LIFE * 20) {
			discard();
			return;
		}
		// the block it's on went: so does the spray
		Direction face = stuckFace();
		if (face != null && age % 10 == 0) {
			BlockPos on = BlockPos.containing(position().subtract(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.05)));
			if (level.getBlockState(on).getCollisionShape(level, on).isEmpty()) {
				discard();
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
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
		if (owner != null) {
			output.putString("SprayOwner", owner.toString());
		}
	}
}

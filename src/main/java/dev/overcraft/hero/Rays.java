package dev.overcraft.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Block rays that never load a chunk (#178). Level.clip reads each block it passes through, and on a server a block in a
 * chunk that isn't loaded loads it there and then, on the server thread, generating it when it's new: a Cassidy shot
 * fired level into fresh land (400 m) cost 140 to 225 ms, and every hitscan out to 200-300 m could do the same. Here a
 * chunk that isn't loaded reads as air, so a ray through it is a miss, as if nothing were there: the server hasn't sent
 * those chunks to anyone, so nobody can see a block there to hit.
 */
public final class Rays {
	private Rays() {
	}

	/** {@code level.clip(context)}, with unloaded chunks read as air. */
	public static BlockHitResult clip(Level level, ClipContext context) {
		return new LoadedOnly(level).clip(context);
	}

	/** The level's loaded blocks; air and no fluid where the chunk isn't loaded. Never loads or generates a chunk. */
	private static final class LoadedOnly implements BlockGetter {
		private final Level level;
		private @Nullable LevelChunk last;

		LoadedOnly(Level level) {
			this.level = level;
		}

		private @Nullable LevelChunk chunk(BlockPos pos) {
			int x = SectionPos.blockToSectionCoord(pos.getX());
			int z = SectionPos.blockToSectionCoord(pos.getZ());
			LevelChunk chunk = last;
			if (chunk == null || chunk.getPos().x() != x || chunk.getPos().z() != z) {
				chunk = level.getChunkSource().getChunkNow(x, z);
				if (chunk != null) {
					last = chunk;
				}
			}
			return chunk;
		}

		@Override
		public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
			return chunk(pos) == null ? null : level.getBlockEntity(pos);
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			if (level.isOutsideBuildHeight(pos)) {
				return Blocks.VOID_AIR.defaultBlockState();
			}
			LevelChunk chunk = chunk(pos);
			return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			if (level.isOutsideBuildHeight(pos)) {
				return Fluids.EMPTY.defaultFluidState();
			}
			LevelChunk chunk = chunk(pos);
			return chunk == null ? Fluids.EMPTY.defaultFluidState() : chunk.getFluidState(pos);
		}

		@Override
		public int getHeight() {
			return level.getHeight();
		}

		@Override
		public int getMinY() {
			return level.getMinY();
		}
	}
}

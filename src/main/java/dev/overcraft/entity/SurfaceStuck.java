package dev.overcraft.entity;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * A thrown thing that sets down on a block (Junkrat's mine and trap, Illari's pylon): the face it's on, synced, so its
 * clients draw it lying on that face, its own up along the face's normal (#154, the owner: "deployables aren't
 * positioned like they're on something"). Null while it flies.
 */
public interface SurfaceStuck {
	@Nullable Direction stuckFace();

	/** The synced value: a face's 3D index (Direction.get3DDataValue), or -1 for none. */
	static int encode(@Nullable Direction face) {
		return face == null ? -1 : face.get3DDataValue();
	}

	static @Nullable Direction decode(int value) {
		return value < 0 || value > 5 ? null : Direction.from3DDataValue(value);
	}
}

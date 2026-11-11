package dev.overcraft.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The tracked entities, for Play of the Game (PotgReplay): what each player is being sent about. */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
	@Accessor("entityMap")
	Int2ObjectMap<?> overcraft$entityMap();
}
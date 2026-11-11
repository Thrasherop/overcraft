package dev.overcraft.mixin;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/** One tracked entity's ServerEntity and who sees it, for a Play of the Game keyframe (PotgTap). */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface TrackedEntityAccessor {
	@Accessor("serverEntity")
	ServerEntity overcraft$serverEntity();

	@Accessor("entity")
	Entity overcraft$entity();

	@Accessor("seenBy")
	Set<ServerPlayerConnection> overcraft$seenBy();
}
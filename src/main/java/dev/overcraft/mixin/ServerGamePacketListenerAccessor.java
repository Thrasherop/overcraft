package dev.overcraft.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Minecraft's count of ticks a player has floated with no block under them (it kicks them past 80 on a server without
 * flight allowed): a player riding Lifeweaver's Petal Platform stands on an entity, which it doesn't count
 * (PetalPlatformEntity resets it).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {
	@Accessor("aboveGroundTickCount")
	void overcraft$setAboveGroundTickCount(int ticks);
}

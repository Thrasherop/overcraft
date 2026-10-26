package dev.overcraft.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Minecraft's own right-click, for the hero's interact key. */
@Mixin(Minecraft.class)
public interface MinecraftInvoker {
	@Invoker("startUseItem")
	void overcraft$startUseItem();
}

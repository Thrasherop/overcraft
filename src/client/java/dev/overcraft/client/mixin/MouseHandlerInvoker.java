package dev.overcraft.client.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Dev only (DevServer's "mouse"): mouse movement handed to Minecraft's own turnPlayer, as if the window had it, so the
 * harness's view turns go the real way (sensitivity, invert, the wheel's pointer, the emote camera).
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
	@Accessor("accumulatedDX")
	void overcraft$setAccumulatedDX(double dx);

	@Accessor("accumulatedDY")
	void overcraft$setAccumulatedDY(double dy);

	@Invoker("turnPlayer")
	void overcraft$turnPlayer(double mousea);
}

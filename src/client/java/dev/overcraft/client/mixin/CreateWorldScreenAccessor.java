package dev.overcraft.client.mixin;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Dev only: lets the DevServer press "Create New World". */
@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenAccessor {
	@Invoker("onCreate")
	void overcraft$onCreate();
}

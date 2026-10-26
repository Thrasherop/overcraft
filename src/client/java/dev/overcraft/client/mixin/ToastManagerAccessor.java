package dev.overcraft.client.mixin;

import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.BitSet;

/** Which toast slots are showing: the kill feed starts below them (both live at the top right). */
@Mixin(ToastManager.class)
public interface ToastManagerAccessor {
	@Accessor("occupiedSlots")
	BitSet overcraft$occupiedSlots();
}

package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * In hero mode, chat lines have no dark panel behind them while nobody's typing: Overwatch's chat is bare text over the
 * game (the third critique). The text keeps its shadow; the panel comes back while typing.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
		at = @At("STORE"), ordinal = 2)
	private float overcraft$noPanelInHeroMode(float backgroundOpacity) {
		return ClientHero.controls() && Minecraft.getInstance().gui.screen() == null ? 0 : backgroundOpacity;
	}
}

package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.HeroHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * In hero mode, chat lines have no dark panel behind them while nobody's typing: Overwatch's chat is bare text over the
 * game (the third critique). The text keeps its shadow; the panel comes back while typing. While typing, chat is drawn
 * by the chat screen rather than the HUD, so it's lifted here, clear of the health (HeroHud.chatLift): it covered the
 * health number. The same lift reaches the screen's click tests (they run through the same method).
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
		at = @At("STORE"), ordinal = 2)
	private float overcraft$noPanelInHeroMode(float backgroundOpacity) {
		return ClientHero.chatQuiet() ? 0 : backgroundOpacity;
	}

	@ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int overcraft$liftWhileTyping(int screenHeight) {
		Minecraft mc = Minecraft.getInstance();
		if (!ClientHero.controls() || !(mc.gui.screen() instanceof ChatScreen)) {
			return screenHeight;
		}
		return screenHeight - (int) Math.ceil(HeroHud.chatLift(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()));
	}
}

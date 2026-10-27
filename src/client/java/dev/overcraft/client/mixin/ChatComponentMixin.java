package dev.overcraft.client.mixin;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;
import net.minecraft.network.chat.FontDescription;
import dev.overcraft.client.hud.HudText;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.injection.Redirect;

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
	/**
	 * Hero-mode chat wraps where Overwatch's does, at about x 425 at 1080p (the eighth critique), measured in
	 * Overwatch's chat face: the lines are split in that face here, as they're added (the history is split again when
	 * hero mode turns on or off: ClientHero). In GUI units at the hero chat's scale (HeroHud.chatPlace).
	 */
	@Redirect(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
	private List<FormattedCharSequence> overcraft$heroWrap(GuiMessage message, Font font, int maxWidth) {
		FontDescription chat = ClientHero.heroView() ? HudText.chatFont() : null;
		if (chat == null) {
			return message.splitLines(font, maxWidth);
		}
		Component styled = message.content().copy().withStyle(style -> style.withFont(chat));
		double scale = Minecraft.getInstance().options.chatScale().get();
		// a team line gets Overwatch's channel icon; wrapped lines start under the text, not one space in
		return HudText.chatLines(styled, Mth.floor(HeroHud.CHAT_WRAP / Math.max(0.1, scale)), font);
	}

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

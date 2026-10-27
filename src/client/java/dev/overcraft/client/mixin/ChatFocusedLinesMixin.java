package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.hud.HudText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Chat while typing in hero mode: Minecraft's own lines (so clicks on them land), but orange like the rest of hero
 * mode's chat (ChatLinesMixin, Overwatch's match chat). Only text without a colour of its own.
 */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess")
public abstract class ChatFocusedLinesMixin {
	@ModifyVariable(method = "handleMessage", at = @At("HEAD"), argsOnly = true)
	private FormattedCharSequence overcraft$matchChatColour(FormattedCharSequence message) {
		if (!ClientHero.heroView()) {
			return message;
		}
		return sink -> message.accept((index, style, codePoint) ->
			sink.accept(index, style.getColor() == null ? style.withColor(HudText.MATCH_CHAT) : style, codePoint));
	}
}

package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.hud.HudText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chat in hero mode while nobody's typing (OvercraftClient places it small at the left, as Overwatch's): its lines in
 * the HUD's sans (HudText's chat face) and without Minecraft's message-tag bar at their left edge (the fourth
 * critique: the pixel font and the tag bar read as Minecraft's chat beside Overwatch's HUD). Letters the face doesn't
 * have fall back to Minecraft's font. While typing, chat is Minecraft's own, so clicks on its lines still land.
 *
 * <p>In hero mode the text is orange, as Overwatch's match chat (the owner, 2026-10-26: "Chat should be orange to look
 * like ow match chat"; every Minecraft player is in the one "match"). Only text without a colour of its own: red errors
 * and coloured names keep theirs. The shade is ours, the HUD's orange: Blizzard gives no number.
 */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess")
public abstract class ChatLinesMixin {
	@ModifyVariable(method = "handleMessage", at = @At("HEAD"), argsOnly = true)
	private FormattedCharSequence overcraft$heroFont(FormattedCharSequence message) {
		if (!ClientHero.heroView()) {
			return message;
		}
		FontDescription font = heroChat() ? HudText.chatFont() : null;
		return sink -> message.accept((index, style, codePoint) -> {
			Style styled = style.getColor() == null ? style.withColor(HudText.MATCH_CHAT) : style;
			// no drop shadow: Overwatch's chat has none (the eighth critique: Minecraft's hard brown copy read as ours)
			return sink.accept(index, font == null ? styled : styled.withFont(font).withoutShadow(), codePoint);
		});
	}

	@Inject(method = "handleTag", at = @At("HEAD"), cancellable = true)
	private void overcraft$noTagBar(int x0, int y0, int x1, int y1, float opacity, GuiMessageTag tag, CallbackInfo ci) {
		if (heroChat()) {
			ci.cancel();
		}
	}

	private static boolean heroChat() {
		return ClientHero.chatQuiet();
	}
}

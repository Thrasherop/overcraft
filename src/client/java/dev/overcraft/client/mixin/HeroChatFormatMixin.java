package dev.overcraft.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import dev.overcraft.client.ClientHero;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A player's chat message in hero view reads as Overwatch's match chat: "NAME (Hero): text" (the eighth critique, #5's
 * rest, the owner's frame), as callouts already do, where Minecraft writes "<NAME> text". Only plain player chat
 * (Minecraft's "chat" type: not /say, /me, whispers or team messages), only when the sender's hero is known here (they
 * are loaded and picked one), else "NAME: text". Formatted as the message arrives: lines already in the history keep
 * the form they came in.
 */
@Mixin(ChatListener.class)
public abstract class HeroChatFormatMixin {
	@WrapOperation(method = "handlePlayerChatMessage", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/network/chat/ChatType$Bound;decorate(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"))
	private Component overcraft$matchChat(ChatType.Bound bound, Component content, Operation<Component> original,
										  @Local(argsOnly = true) GameProfile sender) {
		if (!ClientHero.heroView() || !bound.chatType().is(ChatType.CHAT)) {
			return original.call(bound, content);
		}
		MutableComponent line = Component.empty().append(bound.name());
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.level == null ? null : mc.level.getPlayerByUUID(sender.id());
		String chosen = player == null ? null : player.getAttached(HeroManager.HERO);
		Hero hero = chosen == null ? null : Hero.byId(chosen);
		if (hero != null) {
			line.append(" (" + hero.displayName + ")");
		}
		return line.append(": ").append(content);
	}
}

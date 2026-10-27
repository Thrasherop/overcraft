package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * "Press Left Shift to dismount" names Minecraft's sneak key; in hero mode the hero's crouch key gets you out (Left
 * Shift there is Swift Step), so the hint names that instead.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Redirect(method = "handleSetEntityPassengersPacket",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;getTranslatedKeyMessage()Lnet/minecraft/network/chat/Component;"))
	private Component overcraft$dismountKey(KeyMapping key) {
		return ClientHero.controls() ? HeroControls.get(Action.CROUCH).getTranslatedKeyMessage() : key.getTranslatedKeyMessage();
	}
}

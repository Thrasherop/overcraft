package dev.overcraft.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overcraft.client.hud.PotgCard;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public abstract class PotgBossBarMixin {
	@Shadow
	@Final
	private Map<UUID, LerpingBossEvent> events;

	/** No boss bar under the Play of the Game's top band (the coordinator, 2026-11-11: the dragon's bar sat in the band). */
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void overcraft$noBarUnderPotg(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (PotgCard.showing()) {
			ci.cancel();
		}
	}

	/**
	 * An update for a bar this client doesn't have is dropped, not a crash: the POTG's replay and return change the level,
	 * which clears the client's bars, while the server still has the dying dragon's bar on them; its next update was a
	 * NullPointerException in Minecraft's handler and a disconnect ("Network Protocol Error", found 2026-11-16 killing a
	 * second dragon after a POTG). Adding and removing a bar go through as before.
	 */
	@WrapOperation(method = "update", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/network/protocol/game/ClientboundBossEventPacket;dispatch(Lnet/minecraft/network/protocol/game/ClientboundBossEventPacket$Handler;)V"))
	private void overcraft$knownBarsOnly(ClientboundBossEventPacket packet, ClientboundBossEventPacket.Handler handler, Operation<Void> original) {
		Map<UUID, LerpingBossEvent> known = events;
		original.call(packet, new ClientboundBossEventPacket.Handler() {
			@Override
			public void add(UUID id, Component name, float progress, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay,
				boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
				handler.add(id, name, progress, color, overlay, darkenScreen, playMusic, createWorldFog);
			}

			@Override
			public void remove(UUID id) {
				handler.remove(id);
			}

			@Override
			public void updateProgress(UUID id, float progress) {
				if (known.containsKey(id)) {
					handler.updateProgress(id, progress);
				}
			}

			@Override
			public void updateName(UUID id, Component name) {
				if (known.containsKey(id)) {
					handler.updateName(id, name);
				}
			}

			@Override
			public void updateStyle(UUID id, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
				if (known.containsKey(id)) {
					handler.updateStyle(id, color, overlay);
				}
			}

			@Override
			public void updateProperties(UUID id, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
				if (known.containsKey(id)) {
					handler.updateProperties(id, darkenScreen, playMusic, createWorldFog);
				}
			}
		});
	}
}

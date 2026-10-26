package dev.overcraft.client;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.client.dev.DevServer;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.client.model.HeroModelRenderer;
import dev.overcraft.client.model.HeroModels;
import dev.overcraft.client.model.KitsuneRenderer;
import dev.overcraft.client.model.ProjectileModelRenderer;
import dev.overcraft.net.HeroActionPayload;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.registry.OcEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;

public class OvercraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientHero.init();
		DevServer.startIfEnabled();

		ClientPlayNetworking.registerGlobalReceiver(HeroSyncPayload.TYPE, (payload, context) -> ClientHero.onSync(payload));
		ClientPlayNetworking.registerGlobalReceiver(HeroActionPayload.TYPE, (payload, context) -> {
			if (context.client().level != null) {
				HeroAnimator.trigger(payload.entityId(), payload.clip(), context.client().level.getGameTime() / 20f);
			}
		});
		HeroModels.init();
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> ClientHero.onJoin());
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
			ClientHero.onDisconnect();
			HeroAnimator.clear();
			HeroModelRenderer.clear();
		});
		ClientTickEvents.START_CLIENT_TICK.register(ClientHero::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientHero.endTick(client);
			if (client.level != null && client.level.getGameTime() % 200 == 0) {
				HeroAnimator.prune(client.level.getGameTime() / 20f);
			}
		});

		EntityRendererRegistry.register(OcEntities.KUNAI, ctx -> new ProjectileModelRenderer<>(ctx, "kunai", 1.0f, false));
		EntityRendererRegistry.register(OcEntities.OFUDA, ctx -> new ProjectileModelRenderer<>(ctx, "ofuda", 0.75f, false));
		EntityRendererRegistry.register(OcEntities.SUZU, ctx -> new ProjectileModelRenderer<>(ctx, "suzu", 1.0f, false));
		EntityRendererRegistry.register(OcEntities.KITSUNE, KitsuneRenderer::new);

		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Overcraft.id("hero_hud"), HeroHud::render);
		// hero mode replaces Minecraft's hotbar and status bars with the hero's HUD
		for (Identifier id : new Identifier[] {VanillaHudElements.HOTBAR, VanillaHudElements.HEALTH_BAR,
			VanillaHudElements.ARMOR_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
			VanillaHudElements.INFO_BAR, VanillaHudElements.HELD_ITEM_TOOLTIP}) {
			HudElementRegistry.replaceElement(id, vanilla -> hiddenInHeroMode(vanilla));
		}

		Minecraft mc = Minecraft.getInstance();
		OwMovement.active = entity -> entity == mc.player && ClientHero.controls();
		OwMovement.wallClimb = entity -> entity == mc.player && ClientHero.hero() == Hero.KIRIKO;
		OwMovement.jumpHeld = entity -> entity == mc.player && mc.options.keyJump.isDown();
	}

	private static HudElement hiddenInHeroMode(HudElement vanilla) {
		return (graphics, delta) -> {
			if (!ClientHero.controls()) {
				vanilla.extractRenderState(graphics, delta);
			}
		};
	}
}

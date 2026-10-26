package dev.overcraft.client;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.client.dev.DevServer;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.effect.Cleansed;
import dev.overcraft.client.effect.Fx;
import dev.overcraft.client.effect.FxRender;
import dev.overcraft.client.effect.KirikoFx;
import dev.overcraft.client.hud.KillFeedHud;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.client.model.HeroModelRenderer;
import dev.overcraft.client.model.HeroModels;
import dev.overcraft.client.model.KitsuneRenderer;
import dev.overcraft.client.model.ProjectileModelRenderer;
import dev.overcraft.net.EffectPayload;
import dev.overcraft.net.HeroActionPayload;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.net.KillFeedPayload;
import dev.overcraft.registry.OcEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class OvercraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FxRender.init();
		ClientHero.init();
		DevServer.startIfEnabled();

		ClientPlayNetworking.registerGlobalReceiver(HeroSyncPayload.TYPE, (payload, context) -> ClientHero.onSync(payload));
		ClientPlayNetworking.registerGlobalReceiver(EffectPayload.TYPE, (payload, context) -> KirikoFx.play(payload));
		ClientPlayNetworking.registerGlobalReceiver(KillFeedPayload.TYPE, (payload, context) -> KillFeedHud.add(payload));
		LevelRenderEvents.COLLECT_SUBMITS.register(Fx::render);
		ClientPlayNetworking.registerGlobalReceiver(HeroActionPayload.TYPE, (payload, context) -> {
			if (context.client().level != null) {
				HeroAnimator.trigger(payload.entityId(), payload.clip(), context.client().level.getGameTime() / 20f);
				if (payload.clip().startsWith("throw_ofuda") && context.client().level.getEntity(payload.entityId()) instanceof LivingEntity hero) {
					// the puff at her throwing fingers: a little right of and below the eyes, ahead (first person's hand)
					Vec3 look = hero.getViewVector(1);
					Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
					KirikoFx.ofudaHandPuff(hero.getEyePosition().add(look.scale(0.7)).add(right.scale(0.18)).add(0, -0.16, 0));
				}
			}
		});
		HeroModels.init();
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> ClientHero.onJoin());
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
			ClientHero.onDisconnect();
			HeroAnimator.clear();
			HeroModelRenderer.clear();
			Fx.clear();
			Cleansed.clear();
			KillFeedHud.clear();
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
		// and the chat moves up out of the hero HUD's way (it sits above the hotbar, where the health now is)
		HudElementRegistry.replaceElement(VanillaHudElements.CHAT, vanilla -> (graphics, delta) -> {
			boolean lift = ClientHero.controls();
			if (lift) {
				graphics.pose().pushMatrix();
				graphics.pose().translate(0, -HeroHud.chatLift(graphics.guiWidth(), graphics.guiHeight()));
			}
			vanilla.extractRenderState(graphics, delta);
			if (lift) {
				graphics.pose().popMatrix();
			}
		});

		Minecraft mc = Minecraft.getInstance();
		OwMovement.active = entity -> entity == mc.player && ClientHero.controls();
		OwMovement.wallClimb = entity -> entity == mc.player && ClientHero.hero() == Hero.KIRIKO;
		OwMovement.jumpHeld = entity -> entity == mc.player && HeroControls.get(HeroControls.Action.JUMP).isDown();
	}

	private static HudElement hiddenInHeroMode(HudElement vanilla) {
		return (graphics, delta) -> {
			if (!ClientHero.controls()) {
				vanilla.extractRenderState(graphics, delta);
			}
		};
	}
}

package dev.overcraft;

import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroWeaponItem;
import dev.overcraft.kiriko.KitsuneRush;
import dev.overcraft.net.HeroActionPayload;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.net.SelectHeroPayload;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Overcraft implements ModInitializer {
	public static final String MOD_ID = "overcraft";
	public static final Logger LOG = LoggerFactory.getLogger("Overcraft");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		OcItems.init();
		OcEntities.init();

		PayloadTypeRegistry.serverboundPlay().register(SelectHeroPayload.TYPE, SelectHeroPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(HeroInputPayload.TYPE, HeroInputPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HeroSyncPayload.TYPE, HeroSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HeroActionPayload.TYPE, HeroActionPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SelectHeroPayload.TYPE, (payload, context) -> {
			Hero hero = Hero.byId(payload.heroId());
			HeroManager.select(context.player(), hero);
			if (hero == null) {
				HeroManager.syncNone(context.player());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(HeroInputPayload.TYPE,
			(payload, context) -> HeroManager.onInput(context.player(), payload));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			KitsuneRush.tickAll(server);
			Combat.tick(server);
			HeroManager.tick(server);
		});
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> HeroManager.onJoin(listener.player));
		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> HeroManager.onLeave(listener.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> HeroManager.onRespawn(newPlayer));

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (Combat.isPhased(entity)) {
				return false;
			}
			// Overwatch has no fall damage: none while playing the hero (slot 1)
			return !(source.is(DamageTypeTags.IS_FALL) && entity instanceof Player player && HeroManager.controls(player));
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (entity instanceof Player player && damage > 0) {
				HeroManager.onDamaged(player);
			}
		});
		// a hero's weapon never lies about in the world (dropped on death, thrown out of the inventory)
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item && item.getItem().getItem() instanceof HeroWeaponItem) {
				entity.discard();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			HeroManager.clear();
			Combat.clear();
			KitsuneRush.clear();
		});
	}
}

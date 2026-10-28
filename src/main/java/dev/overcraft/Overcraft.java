package dev.overcraft;

import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Teams;
import dev.overcraft.hero.HeroWeaponItem;
import dev.overcraft.hero.KillFeed;
import dev.overcraft.hero.OvercraftCommand;
import dev.overcraft.hero.Voice;
import dev.overcraft.kiriko.KitsuneRush;
import dev.overcraft.net.EffectPayload;
import dev.overcraft.net.KillFeedPayload;
import dev.overcraft.net.HeroActionPayload;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroModePayload;
import dev.overcraft.net.HeroOptionsPayload;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.net.PickPerkPayload;
import dev.overcraft.net.TeamPayload;
import dev.overcraft.net.CalloutPayload;
import dev.overcraft.net.VoiceLinePayload;
import dev.overcraft.net.EmotePayload;
import dev.overcraft.net.PingPayload;
import dev.overcraft.comms.Callout;
import dev.overcraft.comms.Comms;
import dev.overcraft.net.SelectHeroPayload;
import dev.overcraft.net.OwnSoundPayload;
import dev.overcraft.net.VictoryPayload;
import dev.overcraft.mercy.Souls;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcItems;
import dev.overcraft.registry.OcSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Overcraft implements ModInitializer {
	public static final String MOD_ID = "overcraft";
	public static final Logger LOG = LoggerFactory.getLogger("Overcraft");

	/**
	 * The oldest asset pack this build works with (tools/build_asset_pack.py writes the build's version into the zip,
	 * assets/overcraft/asset_pack.json): raise it whenever the pack must be rebuilt for a build (clips renamed, sounds
	 * added), so an old zip is flagged in game (#21).
	 */
	public static final String ASSETS_NEEDED = "0.3.18";

	/** This build's version (gradle.properties, through fabric.mod.json), as "0.2.0". */
	public static String version() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("?");
	}

	/** Compares dotted versions ("0.2.0" against "0.10.1") by their numbers: negative, zero or positive. */
	public static int compareVersions(String a, String b) {
		String[] x = a.split("[.-]");
		String[] y = b.split("[.-]");
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int p = i < x.length ? parseOr0(x[i]) : 0;
			int q = i < y.length ? parseOr0(y[i]) : 0;
			if (p != q) {
				return Integer.compare(p, q);
			}
		}
		return 0;
	}

	private static int parseOr0(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		OcItems.init();
		OcEntities.init();
		OcSounds.init();
		HeroManager.init();
		Teams.init();

		PayloadTypeRegistry.serverboundPlay().register(SelectHeroPayload.TYPE, SelectHeroPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(HeroInputPayload.TYPE, HeroInputPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(HeroOptionsPayload.TYPE, HeroOptionsPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(HeroModePayload.TYPE, HeroModePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PickPerkPayload.TYPE, PickPerkPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeamPayload.TYPE, TeamPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CalloutPayload.TYPE, CalloutPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VoiceLinePayload.TYPE, VoiceLinePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EmotePayload.TYPE, EmotePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PingPayload.TYPE, PingPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PingPayload.TYPE, PingPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HeroSyncPayload.TYPE, HeroSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HeroActionPayload.TYPE, HeroActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EffectPayload.TYPE, EffectPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(KillFeedPayload.TYPE, KillFeedPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VictoryPayload.TYPE, VictoryPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(dev.overcraft.net.ResurrectPayload.TYPE, dev.overcraft.net.ResurrectPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OwnSoundPayload.TYPE, OwnSoundPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SelectHeroPayload.TYPE, (payload, context) -> {
			Hero hero = Hero.byId(payload.heroId());
			HeroManager.select(context.player(), hero);
			if (hero == null) {
				HeroManager.syncNone(context.player());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(HeroModePayload.TYPE,
			(payload, context) -> HeroManager.setHeroMode(context.player(), payload.on()));
		ServerPlayNetworking.registerGlobalReceiver(HeroOptionsPayload.TYPE,
			(payload, context) -> HeroManager.setOptions(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(HeroInputPayload.TYPE,
			(payload, context) -> HeroManager.onInput(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(CalloutPayload.TYPE, (payload, context) -> {
			Callout callout = Callout.byIndex(payload.callout());
			if (callout != null) {
				Comms.callout(context.player(), callout);
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(PingPayload.TYPE,
			(payload, context) -> dev.overcraft.comms.Pings.ping(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(EmotePayload.TYPE,
			(payload, context) -> Comms.emote(context.player(), payload.id()));
		ServerPlayNetworking.registerGlobalReceiver(VoiceLinePayload.TYPE,
			(payload, context) -> Comms.voiceLine(context.player(), payload.id()));
		ServerPlayNetworking.registerGlobalReceiver(TeamPayload.TYPE,
			(payload, context) -> context.player().sendSystemMessage(Teams.join(context.player(), payload.team())));
		ServerPlayNetworking.registerGlobalReceiver(PickPerkPayload.TYPE,
			(payload, context) -> Perks.pick(context.player(), Perk.byIndex(payload.perk())));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			KitsuneRush.tickAll(server);
			Combat.tick(server);
			HeroManager.tick(server);
			dev.overcraft.mercy.Boosts.tick(server);
			Souls.tick(server, dev.overcraft.mercy.MercyStats.SOUL_TIME);
		});
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> HeroManager.onJoin(listener.player));
		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
			HeroManager.onLeave(listener.player);
			Comms.forget(listener.player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			boolean resurrected = Souls.resurrecting(newPlayer);
			HeroManager.onRespawn(newPlayer, resurrected);
			// a Mercy's Resurrect puts them back where they fell, with its health (after the kit, which sets the health)
			Souls.onRespawn(oldPlayer, newPlayer);
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (Combat.isPhased(entity)) {
				return false;
			}
			// a fall in hero mode hurts from the hero's own safe height (LivingEntityMixin), unless an ability lands it (#35)
			return !(source.is(DamageTypeTags.IS_FALL) && entity instanceof Player player && HeroManager.controls(player)
				&& HeroManager.fallIsSafe(player));
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(KillFeed::onDeath);
		ServerLivingEntityEvents.AFTER_DEATH.register(Voice::onDeath);
		ServerLivingEntityEvents.AFTER_DEATH.register(Souls::onDeath);
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> OvercraftCommand.register(dispatcher));
		// killing the Ender Dragon wins the match: everyone in the End sees Overwatch's VICTORY as it starts to die (#18)
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof EnderDragon && entity.level() instanceof ServerLevel level) {
				for (ServerPlayer player : level.players()) {
					ServerPlayNetworking.send(player, new VictoryPayload());
				}
			}
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
			Souls.clear();
			dev.overcraft.mercy.Boosts.clear();
		});
	}
}

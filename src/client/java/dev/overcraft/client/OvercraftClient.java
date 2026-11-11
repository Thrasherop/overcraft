package dev.overcraft.client;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.client.dev.DevServer;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroOptions;
import dev.overcraft.client.effect.Cleansed;
import dev.overcraft.client.effect.Fx;
import dev.overcraft.client.effect.FxRender;
import dev.overcraft.client.effect.KirikoFx;
import dev.overcraft.client.hud.KillFeedHud;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.client.model.HeroModelRenderer;
import dev.overcraft.client.model.HeroModels;
import dev.overcraft.client.model.KitsuneRenderer;
import dev.overcraft.client.hud.VictoryHud;
import dev.overcraft.net.OwnSoundPayload;
import dev.overcraft.net.VictoryPayload;
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
		dev.overcraft.client.dev.PotgDecode.init();

		ClientPlayNetworking.registerGlobalReceiver(HeroSyncPayload.TYPE, (payload, context) -> dev.overcraft.client.dev.DevLag.in(() -> ClientHero.onSync(payload)));
		ClientPlayNetworking.registerGlobalReceiver(EffectPayload.TYPE, (payload, context) -> dev.overcraft.client.dev.DevLag.in(() -> KirikoFx.play(payload)));
		ClientPlayNetworking.registerGlobalReceiver(KillFeedPayload.TYPE, (payload, context) -> KillFeedHud.add(payload));
		ClientPlayNetworking.registerGlobalReceiver(dev.overcraft.net.ConspiratorPayload.TYPE,
			(payload, context) -> dev.overcraft.client.hud.ConspiratorHud.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(VictoryPayload.TYPE, (payload, context) -> VictoryHud.show());
		// a Mercy resurrected us: respawn as the death screen's button does (the server puts us back where we fell)
		ClientPlayNetworking.registerGlobalReceiver(dev.overcraft.net.GripPayload.TYPE, (payload, context) ->
			ClientLifeweaver.onGrip(payload));
		ClientPlayNetworking.registerGlobalReceiver(dev.overcraft.net.ResurrectPayload.TYPE, (payload, context) -> {
			if (context.player() != null && context.player().isDeadOrDying()) {
				context.player().respawn();
				context.client().gui.setScreen(null);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(OwnSoundPayload.TYPE, (payload, context) -> dev.overcraft.client.dev.DevLag.in(() ->
			ClientHero.playOwn(Identifier.parse(payload.sound()), payload.volume(), payload.pitch(), payload.seed())));
		// Mercy's beams follow her staff and their targets frame by frame, before the effects are drawn
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> dev.overcraft.client.effect.MercyFx.frame());
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> dev.overcraft.client.effect.LifeweaverFx.frame());
		LevelRenderEvents.COLLECT_SUBMITS.register(Fx::render);
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> dev.overcraft.hero.PressTrace.frame());
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> dev.overcraft.client.effect.EffectArt.frame());
		ClientPlayNetworking.registerGlobalReceiver(dev.overcraft.net.PingPayload.TYPE,
			(payload, context) -> dev.overcraft.client.comms.Pings.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(HeroActionPayload.TYPE, (payload, context) -> dev.overcraft.client.dev.DevLag.in(() -> {
			if (context.client().level != null) {
				if (payload.clip().startsWith("emote:")) {
					// "emote:<id>", or "emote:<id>@<ticks>": already that far on, for one who has just come to see it (Comms.showEmote, #96)
					String[] emote = payload.clip().substring("emote:".length()).split("@");
					float ran = emote.length > 1 ? Integer.parseInt(emote[1]) / 20f : 0;
					HeroAnimator.emote(payload.entityId(), emote[0], context.client().level.getGameTime() / 20f - ran);
					return;
				}
				HeroAnimator.trigger(payload.entityId(), payload.clip(), context.client().level.getGameTime() / 20f);
				if (payload.clip().startsWith("throw_ofuda") && context.client().level.getEntity(payload.entityId()) instanceof LivingEntity hero) {
					if (hero == context.client().player && ClientHero.heroView() && context.client().options.getCameraType().isFirstPerson()
						&& HeroModelRenderer.viewFingertipFresh()) {
						// her own view: at the flicking fingertip of her arms, following it (a world point stayed behind when
						// the hand moved on, the seventh critique)
						KirikoFx.ofudaHandPuffInView();
					} else {
						// the puff at her throwing fingers: a little right of and below the eyes, ahead
						Vec3 look = hero.getViewVector(1);
						Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
						KirikoFx.ofudaHandPuff(hero.getEyePosition().add(look.scale(0.7)).add(right.scale(0.18)).add(0, -0.16, 0),
							KirikoFx.enemyOf(hero));
					}
				}
			}
		}));
		HeroModels.init();
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> ClientHero.onJoin());
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
			ClientSojourn.reset();
			ClientHero.onDisconnect();
			HeroAnimator.clear();
			HeroModelRenderer.clear();
			ClientLifeweaver.clear();
			dev.overcraft.client.effect.LifeweaverFx.clear();
			dev.overcraft.client.effect.AnaFx.clear();
			dev.overcraft.client.effect.SombraFx.clear();
			dev.overcraft.client.effect.BrigitteFx.clear();
			dev.overcraft.client.effect.JunoFx.clear();
			dev.overcraft.client.effect.SojournFx.clear();
			dev.overcraft.client.effect.SigmaFx.clear();
			ClientBrigitte.clear();
			Fx.clear();
			Cleansed.clear();
			KillFeedHud.clear();
			dev.overcraft.client.hud.ConspiratorHud.clear();
			HeroHud.clearMoments();
			VictoryHud.clear();
			RushSound.clear();
			LucioMusic.clear();
			dev.overcraft.client.comms.EmoteSounds.clear();
			dev.overcraft.client.effect.LucioFx.clear();
		});
		ClientTickEvents.START_CLIENT_TICK.register(ClientHero::tick);
		ClientTickEvents.START_CLIENT_TICK.register(ClientTow::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientHero.endTick(client);
			RushSound.tick(client);
			LucioMusic.tick(client);
			JetpackCatSounds.tick(client);
			MercySounds.tick(client);
			LifeweaverSounds.tick(client);
			AnaSounds.tick(client);
			dev.overcraft.client.comms.EmoteSounds.tick(client);
			if (client.level != null) {
				dev.overcraft.client.effect.LucioFx.shields(client.level);
				dev.overcraft.client.effect.MercyFx.tick(client.level);
				dev.overcraft.client.effect.JetpackCatFx.aim(client);
				dev.overcraft.client.effect.JetpackCatFx.tethers(client.level);
				dev.overcraft.client.effect.LifeweaverFx.tick(client);
				dev.overcraft.client.effect.AnaFx.tick(client);
				dev.overcraft.client.effect.SombraFx.tick(client);
				ClientJuno.tick(client);
				ClientSojourn.tick(client);
			ClientSigma.tick(client);
				dev.overcraft.client.effect.BrigitteFx.tick(client);
				dev.overcraft.client.effect.JunoFx.tick(client);
				dev.overcraft.client.effect.SojournFx.tick(client);
				dev.overcraft.client.effect.SigmaFx.tick(client);
			}
			// (after JunoFx's, which moves what her loops follow; with no world, it stops them)
			JunoSounds.tick(client);
			SojournSounds.tick(client);
			SigmaSounds.tick(client);
			DvaSounds.tick(client);
			if (client.level != null && client.level.getGameTime() % 200 == 0) {
				HeroAnimator.prune(client.level.getGameTime() / 20f);
			}
		});

		EntityRendererRegistry.register(OcEntities.KUNAI, ctx -> new ProjectileModelRenderer<>(ctx, "kunai", 1.0f, false));
		EntityRendererRegistry.register(OcEntities.OFUDA, ctx -> new ProjectileModelRenderer<>(ctx, "ofuda", 0.75f, false, 2, true));
		EntityRendererRegistry.register(OcEntities.SUZU, ctx -> new ProjectileModelRenderer<>(ctx, "suzu", 1.0f, false));
		EntityRendererRegistry.register(OcEntities.KITSUNE, KitsuneRenderer::new);
		// Lúcio's sonic rounds: a bright green orb (our own sprite), full bright
		// the round has no model: its flight effect is its look (LucioFx), its item only without the local pack
		EntityRendererRegistry.register(OcEntities.SONIC, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx, "sonic", 0.6f,
			false, 1, true));
		// Jetpack Cat's shots: no model in her data (particles only), so drawn by their flight effect
		EntityRendererRegistry.register(OcEntities.PAW, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx, "paw", 0.4f,
			false, 1, true));

		// Mercy's blaster shots: drawn by their flight effect (MercyFx), the item only without the local pack
		EntityRendererRegistry.register(OcEntities.BLASTER_SHOT, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"blaster_shot", 0.3f, false, 1, true));
		// Sojourn's Railgun rounds: no model in her data (2F98 is effects only), drawn by their flight effect (SojournFx)
		EntityRendererRegistry.register(OcEntities.RAILGUN_SHOT, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"railgun_shot", 0.2f, false, 1, true));
		// Sojourn's Disruptor Shot: drawn by its flight effect (SojournFx), the item only without the local pack
		EntityRendererRegistry.register(OcEntities.DISRUPTOR_SHOT, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"disruptor_shot", 0.3f, false, 1, true));
		// Lifeweaver's: the blossom and the thorns have no model in his data, their flight effects are their look
		// (LifeweaverFx: the item only without the local pack); the thrown platform and seed are his model 126CC; the
		// platform, the seed on the ground and the tree their own models with their clips
		EntityRendererRegistry.register(OcEntities.BLOSSOM, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"blossom", 0.6f, false, 1, true));
		EntityRendererRegistry.register(OcEntities.THORN, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"thorn", 0.3f, false, 1, true));
		// Ana's dart: its model 11741 isn't exported yet: the item stands in (her effects are a later step)
		EntityRendererRegistry.register(OcEntities.ANA_DART, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"ana_dart", 0.3f, false, 1, false));
		EntityRendererRegistry.register(OcEntities.SLEEP_DART, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"sleep_dart", 0.3f, false, 1, false));
		EntityRendererRegistry.register(OcEntities.BIOTIC_GRENADE, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"biotic_grenade", 0.4f, false, 1, false));
		EntityRendererRegistry.register(OcEntities.TRANSLOCATOR, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"sombra_beacon", 0.4f, false, 1, false));
		EntityRendererRegistry.register(OcEntities.CYBERSPACE, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"sombra_cyberspace", 0.4f, false, 1, false));
		EntityRendererRegistry.register(OcEntities.PETAL_THROW, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"lifeweaver_seed", 0.5f, false, 1, false));
		EntityRendererRegistry.register(OcEntities.PETAL_PLATFORM, dev.overcraft.client.model.LifeweaverPropRenderer::platform);
		EntityRendererRegistry.register(OcEntities.SEED, dev.overcraft.client.model.LifeweaverPropRenderer::seed);
		EntityRendererRegistry.register(OcEntities.TREE_OF_LIFE, dev.overcraft.client.model.LifeweaverPropRenderer::tree);
		// Sigma's: the Hyperspheres have no model in his data (2D35 / 32B4 are effects only): the item stands in until his
		// effects (step 10); the barrier is his model C015 with its four orbs FC88
		EntityRendererRegistry.register(OcEntities.HYPERSPHERE, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"hypersphere", 0.6f, false, 1, false));
		EntityRendererRegistry.register(OcEntities.SIGMA_BARRIER, dev.overcraft.client.model.SigmaBarrierRenderer::new);
		EntityRendererRegistry.register(OcEntities.DVA_MECH, dev.overcraft.client.model.DvaMechRenderer::new);
		// D.Va's missiles and Light Gun rounds: stand-in items until her effects (1E8A / 1000, step 11)
		EntityRendererRegistry.register(OcEntities.DVA_MISSILE, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"dva_missile", 0.35f, false, 1, false));
		EntityRendererRegistry.register(OcEntities.LIGHT_GUN_SHOT, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"dva_light_gun_shot", 0.3f, false, 1, false));
		// his Accretion rock: its model C5CA (hero_models/sigma_rock.glb), the stone item without the pack
		EntityRendererRegistry.register(OcEntities.ACCRETION, ctx -> new dev.overcraft.client.model.ProjectileModelRenderer<>(ctx,
			"=sigma_rock", 1.2f, false, 1, false));

		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Overcraft.id("hero_hud"), HeroHud::render);
		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, Overcraft.id("rush_view"), HeroHud::rushView);
		// over everything else, in either mode
		HudElementRegistry.addLast(Overcraft.id("victory"), VictoryHud::render);
		// hero mode replaces Minecraft's hotbar and status bars with the hero's HUD. The info bar's foreground too: the
		// experience fill, and the locator bar's dot for each other player (a pink square by the ult meter, found
		// playing on LAN; the hero HUD marks allies itself)
		for (Identifier id : new Identifier[] {VanillaHudElements.HOTBAR, VanillaHudElements.HEALTH_BAR,
			VanillaHudElements.ARMOR_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
			VanillaHudElements.INFO_BAR, VanillaHudElements.INFO_BAR_FOREGROUND, VanillaHudElements.HELD_ITEM_TOOLTIP}) {
			HudElementRegistry.replaceElement(id, vanilla -> hiddenInHeroMode(vanilla));
		}
		// the air bubbles join the hero HUD, above the health (HeroHud.airPlace)
		HudElementRegistry.replaceElement(VanillaHudElements.AIR_BAR, vanilla -> (graphics, delta) -> {
			if (!ClientHero.heroView()) {
				vanilla.extractRenderState(graphics, delta);
				return;
			}
			graphics.pose().pushMatrix();
			HeroHud.airPlace(graphics);
			vanilla.extractRenderState(graphics, delta);
			graphics.pose().popMatrix();
		});
		// and a ridden animal's hearts with them
		HudElementRegistry.replaceElement(VanillaHudElements.MOUNT_HEALTH, vanilla -> (graphics, delta) -> {
			if (!ClientHero.heroView()) {
				vanilla.extractRenderState(graphics, delta);
				return;
			}
			graphics.pose().pushMatrix();
			HeroHud.mountPlace(graphics);
			vanilla.extractRenderState(graphics, delta);
			graphics.pose().popMatrix();
		});
		// and the chat moves out of the hero HUD's way: as Overwatch's, small text at the left above the health (while
		// typing, just lifted, so clicks on its lines still land)
		HudElementRegistry.replaceElement(VanillaHudElements.CHAT, vanilla -> (graphics, delta) -> {
			boolean lift = ClientHero.heroView();
			if (lift) {
				graphics.pose().pushMatrix();
				if (ClientHero.chatQuiet()) {
					HeroHud.chatPlace(graphics);
				} else {
					graphics.pose().translate(0, -HeroHud.chatLift(graphics.guiWidth(), graphics.guiHeight()));
				}
			}
			vanilla.extractRenderState(graphics, delta);
			if (lift) {
				graphics.pose().popMatrix();
			}
		});

		Minecraft mc = Minecraft.getInstance();
		// a Life Grip moves whoever it has, hero or not (their own client: ClientLifeweaver.grip)
		OwMovement.active = entity -> entity == mc.player && (ClientHero.controls() || ClientLifeweaver.gripped());
		OwMovement.wallClimb = entity -> entity == mc.player && ClientHero.hero() == Hero.KIRIKO;
		// the client's own player's body box follows the client's own hero mode (HeroBody)
		dev.overcraft.hero.HeroBody.local = player -> player != mc.player ? null
			: java.util.Optional.ofNullable(ClientHero.hero() != null && ClientHero.heroModeOn() && !player.isSpectator()
				? ClientHero.hero() : null);
		// a Stalwart tank's own client resists the slows it moves under (Tank)
		dev.overcraft.hero.Tank.resistsSlows = entity -> entity == mc.player && ClientHero.controls()
			&& dev.overcraft.hero.Tank.stalwart(ClientHero.hero());
		OwMovement.wallRide = entity -> entity == mc.player && ClientHero.hero() == Hero.LUCIO;
		OwMovement.roped = entity -> entity == mc.player && ClientTow.taut();
		OwMovement.gravityScale = entity -> (entity == mc.player && ClientHero.hero() == Hero.LUCIO && ClientHero.barrierSlam(mc)
			? dev.overcraft.lucio.LucioStats.BARRIER_GRAVITY : 1)
			// Minecraft's gravity attribute on them, against its default 0.08 (Hyper Strike's float, Sleep.stun: x0.01)
			* entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY) / 0.08;
		OwMovement.mover = entity -> entity == mc.player && ClientHero.hero() == Hero.LUCIO ? OwMovement.Mover.LUCIO : OwMovement.Mover.DEFAULT;
		OwMovement.jumpKeyHeld = entity -> entity == mc.player && HeroControls.get(HeroControls.Action.JUMP).isDown()
			&& !ClientMercy.heldForLaunch(HeroControls.Action.JUMP);
		OwMovement.flight = entity -> entity == mc.player && (ClientHero.hero() == Hero.JETPACK_CAT
			|| ClientHero.hero() == Hero.MERCY && ClientMercy.valkyrie(entity));
		OwMovement.valkyrie = entity -> entity == mc.player && ClientHero.hero() == Hero.MERCY && ClientMercy.valkyrie(entity);
		OwMovement.glide = entity -> {
			OwMovement.Glide grip = ClientLifeweaver.grip(entity);
			return grip != null ? grip : ClientMercy.glide(entity);
		};
		// Lifeweaver's dash, else Brigitte's Shield Bash
		OwMovement.dash = entity -> {
			net.minecraft.world.phys.Vec3 lw = ClientLifeweaver.dash(entity);
			return lw != null ? lw : ClientBrigitte.bash(entity);
		};
		OwMovement.abilityMoving = entity -> ClientLifeweaver.grip(entity) != null || ClientMercy.glide(entity) != null || ClientLifeweaver.dashing(entity) || ClientBrigitte.bashing(entity) || ClientSojourn.slide(entity) != null;
		OwMovement.descent = ClientMercy::descent;
		// Sigma: Accretion's gather holds his jump; Gravitic Flux's flight (its rise first)
		OwMovement.levitating = ClientSigma::levitating;
		OwMovement.noJump = entity -> entity == mc.player && ClientHero.hero() == Hero.SIGMA
			&& (ClientHero.sync().mode() & dev.overcraft.sigma.Sigma.MODE_GATHER) != 0;
		OwMovement.sigmaFlight = entity -> entity != mc.player || ClientHero.hero() != Hero.SIGMA
			|| (ClientHero.sync().mode() & dev.overcraft.sigma.Sigma.MODE_FLUX_FLIGHT) == 0 ? 0
			: (ClientHero.sync().mode() & dev.overcraft.sigma.Sigma.MODE_FLUX_RISE) != 0 ? 1 : 2;
		// D.Va's Boosters: the server's flight, her client's move
		OwMovement.dvaBoosters = entity -> entity == mc.player && ClientHero.hero() == Hero.DVA
			&& (ClientHero.sync().mode() & dev.overcraft.dva.Dva.MODE_BOOSTING) != 0;
		// Juno's Martian Overboots (double jump, hover) and Glide Boost
		OwMovement.airJumps = ClientJuno::airJumps;
		OwMovement.doubleJumped = ClientJuno::doubleJumped;
		OwMovement.junoGlide = ClientJuno::glide;
		// Sojourn's Power Slide and its jump
		OwMovement.slide = ClientSojourn::slide;
		OwMovement.slideEnded = ClientSojourn::ended;
		OwMovement.undamped = ClientMercy::undamped;
		OwMovement.boosting = entity -> entity == mc.player ? ClientHero.boostTicks() : -1;
		OwMovement.driven = entity -> entity == mc.player && ClientHero.hero() == Hero.JETPACK_CAT && ClientHero.catnapperDriven();
		OwMovement.crouchHeld = entity -> entity == mc.player && HeroControls.get(HeroControls.Action.CROUCH).isDown()
			&& !ClientMercy.heldForLaunch(HeroControls.Action.CROUCH);
		// jump held, or (Overwatch's "Automatically climb walls") just being in the air
		OwMovement.jumpHeld = entity -> entity == mc.player && (HeroControls.get(HeroControls.Action.JUMP).isDown()
			|| HeroOptions.get().autoClimb && !entity.onGround());
	}

	private static HudElement hiddenInHeroMode(HudElement vanilla) {
		return (graphics, delta) -> {
			if (!ClientHero.heroView()) {
				vanilla.extractRenderState(graphics, delta);
			}
		};
	}
}

package dev.overcraft.hero;

import com.mojang.serialization.Codec;
import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.kiriko.Kiriko;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.registry.OcItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.overcraft.net.HeroOptionsPayload;

/**
 * Which hero each player is, their kit (health, the hero's weapon in hotbar slot 1) and the server side of the hero's
 * controls. Hotbar slot 1 selected = the hero's keys are live; any other slot plays plain Minecraft.
 */
public final class HeroManager {

	/**
	 * The chosen hero, saved with the player and kept through death. Synced to every client: a player looks like
	 * their hero to everyone, in hero mode and out of it.
	 */
	public static final AttachmentType<String> HERO = AttachmentRegistry.create(Overcraft.id("hero"),
		builder -> builder.persistent(Codec.STRING).copyOnDeath()
			.syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));

	/**
	 * Hero mode on (the hero's controls, movement and HUD) or off (plain Minecraft): switched by the G key alone, saved
	 * with the player and kept through death, synced so the client agrees after a respawn or a rejoin. It used to be
	 * "hotbar slot 1 held" (owner, 2026-10-26: "drop the hotbar-based mode selection entirely", #26). Absent: on.
	 */
	public static final AttachmentType<Boolean> HERO_MODE = AttachmentRegistry.create(Overcraft.id("hero_mode"),
		builder -> builder.persistent(Codec.BOOL).copyOnDeath()
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	/**
	 * Lúcio's song, for every client to play it at him (LucioMusic): 0 none (not Lúcio, not in hero mode, dead), 1 Healing
	 * Boost, 2 Speed Boost, plus 4 while Amp It Up is on. Not saved: the kit sets it each tick.
	 */
	public static final AttachmentType<Integer> SONG = AttachmentRegistry.create(Overcraft.id("song"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

	/**
	 * Mercy's state, for every client to draw and animate her (MercyFx, HeroAnimator): [flags (Mercy.FLAG_*), Guardian
	 * Angel's target (-1 none, -2 a soul), its destination x, y, z in cm, the beam's mode (0 none, 1 heal, 2 boost), its
	 * target, then the Valkyrie chain's or Chain Boost's targets]. Absent or empty: not Mercy in play. Not saved: the kit sets it each tick it changes.
	 */
	public static final AttachmentType<java.util.List<Integer>> MERCY = AttachmentRegistry.create(Overcraft.id("mercy"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.all()));

	/**
	 * Lifeweaver's state, for every client to draw and animate him (LifeweaverFx, HeroAnimator): [flags (Lifeweaver.FLAG_*),
	 * Healing Blossom's charge (%), its target (-1 none), Life Grip's ally (-1 none)]. Absent: not Lifeweaver in play.
	 */
	public static final AttachmentType<java.util.List<Integer>> LIFEWEAVER = AttachmentRegistry.create(Overcraft.id("lifeweaver"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.all()));

	/** Juno gliding (Glide Boost): for every client to animate her glide (HeroAnimator). */
	public static final AttachmentType<Boolean> JUNO_GLIDE = AttachmentRegistry.create(Overcraft.id("juno_glide"),
		builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	/** Juno locking Pulsar Torpedoes: for every client to play her lock's sounds (JunoSounds: 520F, held while she locks). */
	public static final AttachmentType<Boolean> JUNO_LOCK = AttachmentRegistry.create(Overcraft.id("juno_lock"),
		builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	/**
	 * Lúcio on a wall (#115): for every other client to pose him (HeroAnimator's clip and lean, LucioFx's sparks): OwMovement.wallCode's
	 * side and ride yaw, 0 off it. Only his own client knows his ride (it moves him), so it says, in its input.
	 */
	public static final AttachmentType<Integer> LUCIO_WALL = AttachmentRegistry.create(Overcraft.id("lucio_wall"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

	/** Sojourn sliding (Power Slide): for every client to animate her slide (HeroAnimator), and her own to move her. */
	public static final AttachmentType<Boolean> SOJOURN_SLIDE = AttachmentRegistry.create(Overcraft.id("sojourn_slide"),
		builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	/**
	 * Sojourn's Railgun as every client draws and hears it (SojournFx's glow on her gun by her energy, Overclock's look and
	 * loop, her stance additive): her energy in whole points, plus Sojourn.SYNC_OVERCLOCK while she is in Overclock.
	 */
	public static final AttachmentType<Integer> SOJOURN_GUN = AttachmentRegistry.create(Overcraft.id("sojourn_gun"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

	/**
	 * D.Va's form (dev.overcraft.dva.Dva.MECH .. WRECK), for every client to give her the body's box, eye, model and keys.
	 * Absent: not D.Va. Not saved: a fresh kit (joining, respawning) is in her mech.
	 */
	public static final AttachmentType<Integer> DVA_FORM = AttachmentRegistry.create(Overcraft.id("dva_form"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

	/**
	 * D.Va's held states as every client hears them (client.DvaSounds: Defense Matrix's and Boosters' loops, Precision Fusion's):
	 * Dva.MODE_MATRIX / MODE_BOOSTING / MODE_PRECISION bits. Absent or 0: none.
	 */
	public static final AttachmentType<Integer> DVA_STATE = AttachmentRegistry.create(Overcraft.id("dva_state"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

	/**
	 * Sigma's state as every client draws and hears it (SigmaFx, client.SigmaSounds): Sigma.SYNC_* bits (Kinetic Grasp, the
	 * gather, Gravitic Flux's flight and its aim, Levitation, Hyper Strike charged). Absent or 0: none of them.
	 */
	public static final AttachmentType<Integer> SIGMA_STATE = AttachmentRegistry.create(Overcraft.id("sigma_state"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

	/**
	 * Sombra's stealth as every client draws it (HeroModelRenderer's phased-out look, her stealthed clips): the game tick her
	 * stealth began (Stealth.start: its 0.75 s fade-in counts from there) while it lasts, -1 once it's over (#87). Whether she
	 * is hidden or seen through is still Minecraft's invisibility (Stealth.tick).
	 */
	public static final AttachmentType<Long> SOMBRA_STEALTH = AttachmentRegistry.create(Overcraft.id("sombra_stealth"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.all()));

	/** Asleep or stunned (Sleep): set on a sleeping or stunned player, for their client to hold them still. */
	public static final AttachmentType<Boolean> ASLEEP = AttachmentRegistry.create(Overcraft.id("asleep"),
		builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	/**
	 * Ana's state, for every client to draw and animate her (HeroAnimator, her zoom): [flags (Ana.FLAG_*), how far scoped
	 * in (%)]. Absent: not Ana in play.
	 */
	public static final AttachmentType<java.util.List<Integer>> ANA = AttachmentRegistry.create(Overcraft.id("ana"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.all()));

	/**
	 * The ult's charge, saved with the player when they leave: leaving and rejoining kept the hero but lost a nearly
	 * full ult (found playing). Overwatch has no rejoining mid-match; a Minecraft world is played over many sessions.
	 */
	public static final AttachmentType<Double> ULT_CHARGE = AttachmentRegistry.create(Overcraft.id("ult_charge"),
		builder -> builder.persistent(Codec.DOUBLE).copyOnDeath());

	/**
	 * Each hero's perk progress and picks (#36), saved with the player: kept through deaths, sessions and hero swaps
	 * (swapping back restores them, as in Overwatch). Written on leaving, swapping and picking.
	 */
	public static final AttachmentType<Map<String, Perks.Progress>> PERKS = AttachmentRegistry.create(Overcraft.id("perks"),
		builder -> builder.persistent(Perks.Progress.MAP).copyOnDeath());

	private static final Identifier HEALTH_MODIFIER = Overcraft.id("hero_health");
	private static final Map<Hero, HeroKit> KITS = new EnumMap<>(Hero.class);
	private static final Map<UUID, HeroState> STATES = new HashMap<>();

	static {
		KITS.put(Hero.KIRIKO, new Kiriko());
		KITS.put(Hero.LUCIO, new dev.overcraft.lucio.Lucio());
		KITS.put(Hero.JETPACK_CAT, new dev.overcraft.jetpackcat.JetpackCat());
		KITS.put(Hero.MERCY, new dev.overcraft.mercy.Mercy());
		KITS.put(Hero.LIFEWEAVER, new dev.overcraft.lifeweaver.Lifeweaver());
		KITS.put(Hero.ANA, new dev.overcraft.ana.Ana());
		KITS.put(Hero.SOMBRA, new dev.overcraft.sombra.Sombra());
		KITS.put(Hero.BRIGITTE, new dev.overcraft.brigitte.Brigitte());
		KITS.put(Hero.JUNO, new dev.overcraft.juno.Juno());
		KITS.put(Hero.SIGMA, new dev.overcraft.sigma.Sigma());
		KITS.put(Hero.DVA, new dev.overcraft.dva.Dva());
		KITS.put(Hero.SOJOURN, new dev.overcraft.sojourn.Sojourn());
	}

	private HeroManager() {
	}

	public static @Nullable HeroKit kit(Hero hero) {
		return KITS.get(hero);
	}

	public static @Nullable HeroState state(Player player) {
		return STATES.get(player.getUUID());
	}

	/** The player plays this perk's hero and picked the perk (#36). */
	public static boolean hasPerk(Player player, Perk perk) {
		HeroState state = STATES.get(player.getUUID());
		return state != null && state.hero == perk.hero && state.has(perk);
	}

	public static @Nullable Hero hero(Player player) {
		HeroState state = STATES.get(player.getUUID());
		return state == null ? null : state.hero;
	}

	/** The hero's keys are live: a hero, hero mode on, alive and not a spectator. */
	public static boolean controls(Player player) {
		return hero(player) != null && heroMode(player) && player.isAlive() && !player.isSpectator() && !Sleep.asleep(player);
	}

	/** A hero in hero mode, alive, whether or not their keys are live: what a fall asks. Asleep or stunned in the air, an ability's landing and their own safe height still count (#35). A fall begun in Minecraft mode stays Minecraft's (PLAIN_FALLS). */
	public static boolean inHeroPlay(Player player) {
		return hero(player) != null && heroMode(player) && player.isAlive() && !player.isSpectator() && !PLAIN_FALLS.contains(player.getUUID());
	}

	/** Falling when G turned hero mode on: plain Minecraft stays plain, so this fall lands as Minecraft's: G just before the ground dodged it. Till the fall's over. */
	private static final java.util.Set<UUID> PLAIN_FALLS = new java.util.HashSet<>();

	/** Coming into hero mode (G, a pick): a fall under way stays Minecraft's. */
	private static void startPlainFall(Player player) {
		if (player.fallDistance > 0) {
			PLAIN_FALLS.add(player.getUUID());
		}
	}

	/** In what holds them and stops a fall: water, lava, a cobweb. */
	public static boolean heldInBlock(net.minecraft.world.entity.LivingEntity e) {
		return e.isInWater() || e.isInLava() || e.level().getBlockState(e.blockPosition()).is(net.minecraft.world.level.block.Blocks.COBWEB);
	}

	/** Their Jump is Minecraft's or the mount's here, not an air ability's: riding, in what holds them, on a ladder, in powder snow with the boots to climb it (LivingEntity's own test). Client and server ask this. */
	public static boolean jumpIsNotAirborne(net.minecraft.world.entity.LivingEntity e) {
		return e.isPassenger() || heldInBlock(e) || e.onClimbable()
			|| e.getInBlockState().is(net.minecraft.world.level.block.Blocks.POWDER_SNOW) && net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(e);
	}

	/** The ticks in a row a landing has stayed somewhere, this tick's included (Holds.stayed). */
	public static int landingTouch(net.minecraft.world.entity.LivingEntity e, int touch) {
		return Holds.stayed(heldInBlock(e), e.onClimbable(), e.getKnownMovement().y) ? touch + 1 : 0;
	}

	/** A landing not tied to hero mode (Life Grip's, on anyone) is over: riding, or stayed. */
	public static boolean landingOver(net.minecraft.world.entity.LivingEntity e, int touch) {
		return Holds.landingEnds(true, touch, e.isPassenger());
	}

	/**
	 * A fall that hurts nobody right now, whatever its height: the hook for heroes whose abilities land them safely (the owner, #35). Without one, a hero's falls count from their safe
	 * height (Hero.safeFall): Kiriko's plain fall from her climb's height, her Swift Step's by its soft landing.
	 */
	public static boolean fallIsSafe(Player player) {
		HeroState state = STATES.get(player.getUUID());
		// Mercy: Guardian Angel's flight and launch, and Valkyrie's flight, land her (Angelic Descent clears the fall itself);
		// Lifeweaver: Rejuvenating Dash lands him; Sombra: the Translocator's teleport (it can leave her 12 m up in the air)
		// Sigma: Gravitic Flux's flight (and Levitation) land him; D.Va: the eject's launch lands her
		// Juno: no fall hurts her (W; her data has no fall damage node: juno-map.md); Sojourn: Power Slide and its jump land her
		// Lucio: a wall ride and whatever follows it; Brigitte: Shield Bash; Kiriko: Swift Step (softLanding, below)
		return state != null && ((state.hero == Hero.MERCY || state.hero == Hero.LIFEWEAVER || state.hero == Hero.SOMBRA
			|| state.hero == Hero.SOJOURN || state.hero == Hero.SIGMA || state.hero == Hero.DVA || state.hero == Hero.LUCIO
			|| state.hero == Hero.BRIGITTE || state.hero == Hero.KIRIKO)
			&& state.softLanding || state.hero == Hero.JUNO);
	}

	/**
	 * A movement ability's fall doesn't count until they're down after it (CLAUDE.md, #35): each tick while {@code moving} or still falling from it. {@code grace}: not down yet whatever the
	 * ground says, just after a teleport, when the server's onGround is the place she left.
	 */
	public static void softLanding(Player player, HeroState s, boolean moving, boolean grace) {
		boolean was = s.softLanding;
		s.softLanding = Holds.softLanding(was, moving, player.onGround(), grace);
		// (also on the tick they land: Minecraft can check the fall a tick after the server sees them down: Mercy's note)
		if (was || s.softLanding) {
			player.resetFallDistance();
		}
	}

	/** Hero mode is on (on unless switched off with G). */
	public static boolean heroMode(Player player) {
		return player.getAttachedOrElse(HERO_MODE, true);
	}

	/** G: hero mode on or off (only with a hero picked). */
	public static void setHeroMode(ServerPlayer player, boolean on) {
		if (hero(player) != null) {
			if (on != heroMode(player)) {
				// switching is doing something: an emote ends, as any action ends one (it kept kneeling in Minecraft mode)
				dev.overcraft.comms.Comms.endEmote(player);
				if (!on) {
					// plain Minecraft from here: nothing of the hero's play carries over (#100)
					outOfPlay(player, STATES.get(player.getUUID()), HeroKit.OutOfPlay.MINECRAFT_MODE);
				} else {
					startPlainFall(player);
				}
			}
			player.setAttached(HERO_MODE, on);
			HeroState state = STATES.get(player.getUUID());
			if (on && state != null && state.hero == Hero.DVA) {
				// back in hero mode she's in the mech she left (the plan's default 9; G off is her leavePlay)
				dev.overcraft.dva.Dva.heroMode(player, state, true);
			}
		}
	}

	/**
	 * The hero leaves play (#100): G, death, a hero switch, leaving the world. The one place it happens, called with the
	 * hero's state as it was, before anything resets it. The kit ends what the hero is doing (HeroKit.leavePlay); then the
	 * rules every hero shares: Minecraft's flight permission goes back to the game mode (Jetpack Cat's flight, Mercy's
	 * Valkyrie), and, when the hero is gone for good, the synced looks every client draws them by.
	 */
	public static void outOfPlay(ServerPlayer player, @Nullable HeroState state, HeroKit.OutOfPlay why) {
		if (state == null || state.hero == null) {
			return;
		}
		HeroKit kit = kit(state.hero);
		if (kit != null) {
			kit.leavePlay(player, state, why);
		}
		// whatever ult was running has ended (its charge stays spent: HeroKit.leavePlay)
		state.ultActive = 0;
		dev.overcraft.jetpackcat.JetpackCat.keepFlight(player, state, false);
		if (why.forGood()) {
			for (AttachmentType<?> look : HERO_LOOKS) {
				player.removeAttached(look);
			}
		}
	}

	/** The synced states clients draw a hero in play by (not the hero itself, nor sleep, which others put on them). */
	private static final java.util.List<AttachmentType<?>> HERO_LOOKS = java.util.List.of(SONG, MERCY, LIFEWEAVER, JUNO_GLIDE,
		JUNO_LOCK, SOJOURN_SLIDE, SOJOURN_GUN, SIGMA_STATE, ANA, DVA_FORM, DVA_STATE, LUCIO_WALL);

	/** Dev only (DevServer's "bot showstate off"): the rule off, to show what it does. */
	public static boolean devNoShowState;

	/**
	 * The one rule of #96: a player who starts seeing {@code tracked} learns the state it's in. Three kinds carry every state
	 * others see: attachments (HERO_LOOKS and the like) go by themselves on tracking; timed effects go from Effects' list; and here
	 * the hero's emote, with the time it has run, and whatever its kit holds (HeroKit.showState).
	 */
	public static void showState(net.minecraft.world.entity.Entity tracked, ServerPlayer viewer) {
		if (devNoShowState) {
			return;
		}
		Effects.showHeld(tracked, viewer);
		if (tracked instanceof ServerPlayer hero) {
			dev.overcraft.comms.Comms.showEmote(hero, viewer);
			HeroState state = STATES.get(hero.getUUID());
			HeroKit kit = state == null || state.hero == null ? null : kit(state.hero);
			if (kit != null) {
				kit.showState(hero, state, viewer);
			}
		}
	}

	/** A player died (AFTER_DEATH): their hero leaves play, as at a switch, but stays theirs. */
	public static void onDeath(ServerPlayer player) {
		outOfPlay(player, STATES.get(player.getUUID()), HeroKit.OutOfPlay.DEATH);
	}

	/** How close to their bed (respawn point) a player must be to change heroes (owner, 2026-10-25). */
	public static final double CHANGE_HERO_RANGE = 5;

	/**
	 * Changing heroes: the free pick on joining, no hero yet, or within 5 blocks of their bed (like a spawn room); with no
	 * bed, of the world spawn's area.
	 */
	public static boolean canChangeHero(ServerPlayer player, HeroState state) {
		return state.freePick || state.hero == null || nearRespawn(player);
	}

	/**
	 * Near the player's bed or respawn anchor, or the world spawn when they have none, or where they last respawned: with
	 * no bed Minecraft puts them anywhere up to the spawn radius (10 blocks) from the world spawn, and the select said "go
	 * back to your bed" where they stood (found playing, 0.3.26).
	 */
	private static boolean nearRespawn(ServerPlayer player) {
		HeroState state = STATES.get(player.getUUID());
		if (state != null && state.respawnedAt != null && state.respawnedIn == player.level().dimension()
			&& player.position().distanceTo(state.respawnedAt) <= CHANGE_HERO_RANGE + 0.5) {
			return true;
		}
		ServerPlayer.RespawnConfig config = player.getRespawnConfig();
		if (config == null) {
			// no bed: Minecraft spawns them anywhere in a square respawn_radius (10) around the world spawn
			// (PlayerSpawnFinder), first join or death alike, so that whole square is the spawn room, and 5 blocks round it
			GlobalPos world = player.level().getRespawnData().globalPos();
			int radius = Math.max(0, player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS));
			Vec3 to = player.position().subtract(Vec3.atBottomCenterOf(world.pos()));
			double reach = radius + CHANGE_HERO_RANGE + 0.5;
			return world.dimension() == player.level().dimension() && Math.abs(to.x) <= reach && Math.abs(to.z) <= reach
				&& Math.abs(to.y) <= CHANGE_HERO_RANGE + 16;
		}
		GlobalPos pos = config.respawnData().globalPos();
		return pos.dimension() == player.level().dimension()
			&& player.position().distanceTo(Vec3.atCenterOf(pos.pos())) <= CHANGE_HERO_RANGE + 0.5;
	}

	public static void select(ServerPlayer player, @Nullable Hero hero) {
		if (hero != null && (!hero.playable || kit(hero) == null)) {
			return;
		}
		HeroState state = STATES.computeIfAbsent(player.getUUID(), id -> new HeroState());
		if (!canChangeHero(player, state)) {
			player.sendOverlayMessage(Component.translatable("overcraft.change_hero.too_far"));
			return;
		}
		boolean same = state.hero == hero;
		boolean wasInPlay = state.hero != null && heroMode(player);
		state.freePick = false;
		if (same) {
			return; // picking the hero you already are changes nothing (no free refill)
		}
		savePerks(player, state);
		outOfPlay(player, state, HeroKit.OutOfPlay.SWITCH);
		state.reset(hero);
		loadPerks(player, state);
		if (hero == null) {
			player.removeAttached(HERO);
		} else {
			player.setAttached(HERO, hero.id);
		}
		applyKit(player, state, true);
		if (hero != null) {
			// picking a hero puts you in hero mode
			if (!wasInPlay) {
				startPlainFall(player);
			}
			player.setAttached(HERO_MODE, true);
			Voice.say(player, "select", 1);
		}
	}

	/**
	 * The hero's max health for the body they're in (D.Va's mech 675 or pilot 175); {@code full} also fills it and the top
	 * layer (her eject's full heal, a new mech at full). Her forms call it as they change.
	 */
	public static void refreshHealth(ServerPlayer player, HeroState state, boolean full) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(HEALTH_MODIFIER);
			if (state.hero != null) {
				double target = Ow.hp(state.owHealth(player));
				maxHealth.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, target - maxHealth.getBaseValue(),
					AttributeModifier.Operation.ADD_VALUE));
			}
		}
		if (player.getHealth() > player.getMaxHealth()) {
			// (the layer can't be more than what's left: a body with less of it)
			state.shield = Math.min(state.shield, state.layer(player));
			Shields.setRaw(player, player.getMaxHealth());
		}
		if (full && state.hero != null) {
			state.shield = state.layer(player);
			state.armorLost = 0;
			Shields.setRaw(player, player.getMaxHealth());
		}
		state.shield = Math.min(state.shield, state.layer(player));
	}

	/** Health; {@code fresh} also refills health, ammo and cooldowns (a new pick or a respawn). */
	private static void applyKit(ServerPlayer player, HeroState state, boolean fresh) {
		refreshHealth(player, state, false);
		// the hero weapon item that once held slot 1 (hero mode was "slot 1 held" until #26): taken out of old saves
		removeWeapons(player.getInventory());
		if (state.hero == null) {
			return;
		}
		HeroKit kit = kit(state.hero);
		if (fresh) {
			state.shield = state.layer(player);
			player.setHealth(player.getMaxHealth());
			if (kit != null) {
				kit.equip(player, state);
			}
		}
	}

	/** Takes every hero weapon item out of the inventory. */
	private static void removeWeapons(Inventory inventory) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).getItem() instanceof HeroWeaponItem) {
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}
	}

	public static void onJoin(ServerPlayer player) {
		String saved = player.getAttached(HERO);
		Hero hero = saved == null ? null : Hero.byId(saved);
		HeroState state = STATES.computeIfAbsent(player.getUUID(), id -> new HeroState());
		dropStaleModifiers(player);
		state.reset(hero != null && hero.playable ? hero : null);
		state.freePick = true;
		Double charge = player.getAttached(ULT_CHARGE);
		if (charge != null && state.hero != null) {
			state.ultCharge = charge;
		}
		loadPerks(player, state);
		applyKit(player, state, true);
	}

	public static void onLeave(ServerPlayer player) {
		PLAIN_FALLS.remove(player.getUUID());
		HeroState state = STATES.get(player.getUUID());
		if (state != null && state.hero != null) {
			player.setAttached(ULT_CHARGE, state.ultCharge);
			savePerks(player, state);
			// what they were doing and what they placed leave with them (#100)
			outOfPlay(player, state, HeroKit.OutOfPlay.LEAVE);
		}
		STATES.remove(player.getUUID());
		OPTIONS.remove(player.getUUID());
	}

	/**
	 * On joining: an Overcraft attribute modifier saved with the player from an older build (Lifeline's speed, Rally's
	 * health and speed, Inspiring Strike's, the Hyper Ring's, overhealth's cap: they were permanent, so saved, and outlived
	 * the ability, #100) is taken off. Every such modifier is transient now; only the hero's health is saved, and applyKit
	 * sets it again.
	 */
	private static void dropStaleModifiers(ServerPlayer player) {
		net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().forEach(attribute -> {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance == null) {
				return;
			}
			for (AttributeModifier modifier : java.util.List.copyOf(instance.getModifiers())) {
				if (modifier.id().getNamespace().equals(Overcraft.MOD_ID) && !modifier.id().equals(HEALTH_MODIFIER)) {
					instance.removeModifier(modifier.id());
				}
			}
		});
	}

	/** Each player's hero options the server acts on (sent by their client: HeroOptionsPayload). */
	private static final Map<UUID, HeroOptionsPayload> OPTIONS = new HashMap<>();

	public static void setOptions(ServerPlayer player, HeroOptionsPayload options) {
		OPTIONS.put(player.getUUID(), options);
	}

	public static HeroOptionsPayload options(Player player) {
		return OPTIONS.getOrDefault(player.getUUID(), HeroOptionsPayload.DEFAULTS);
	}

	/** After a respawn; resurrected: a Mercy brought them back (dev.overcraft.mercy.Souls), and they say so. */
	public static void onRespawn(ServerPlayer player, boolean resurrected) {
		HeroState state = STATES.get(player.getUUID());
		if (state != null) {
			Hero hero = state.hero;
			// the ult's charge and the perks survive dying, as in Overwatch (the charge was being reset with everything else)
			double charge = state.ultCharge;
			Perks.Progress perks = Perks.save(state);
			state.reset(hero);
			state.ultCharge = charge;
			Perks.load(state, perks);
			state.respawnedAt = player.position();
			state.respawnedIn = player.level().dimension();
			applyKit(player, state, true);
			Voice.say(player, resurrected ? "resurrected" : "respawn", resurrected ? 1 : 0.7f);
		}
	}

	public static void onDamaged(Player player) {
		HeroState state = STATES.get(player.getUUID());
		if (state != null) {
			state.lastDamagedTick = Combat.now(player);
			if (state.hero == Hero.SOMBRA && player instanceof ServerPlayer sombra) {
				// damage doesn't end her stealth: it shows her for 1.3 s (420C)
				dev.overcraft.sombra.Stealth.reveal(sombra, state, 1.3);
			}
		}
	}

	public static void onInput(ServerPlayer player, HeroInputPayload input) {
		HeroState state = STATES.get(player.getUUID());
		if (state == null || state.hero == null) {
			return;
		}
		// the buttons held are kept even while the keys aren't live (asleep: Ana's Sleep Dart), so a button held through it
		// fires again as it ends, as Overwatch's do; the client only sends them as they change (found playing)
		// A quick click's press and release can both arrive between two server ticks (at a low frame rate the client runs
		// two ticks in one frame and sends both back to back), and the ticks would never see it held: Juno's torpedo lock
		// and fire were lost at 10 fps (found playing, the release play). The release then waits for the next tick.
		boolean primary = (input.held() & HeroInputPayload.HELD_PRIMARY) != 0;
		boolean secondary = (input.held() & HeroInputPayload.HELD_SECONDARY) != 0;
		if (primary) {
			state.primaryFresh |= !state.primaryHeld;
			state.primaryHeld = true;
			state.primaryLetGo = false;
		} else if (state.primaryFresh) {
			state.primaryLetGo = true;
		} else {
			state.primaryHeld = false;
		}
		if (secondary) {
			state.secondaryFresh |= !state.secondaryHeld;
			state.secondaryHeld = true;
			state.secondaryLetGo = false;
		} else if (state.secondaryFresh) {
			state.secondaryLetGo = true;
		} else {
			state.secondaryHeld = false;
		}
		state.wallRiding = (input.held() & HeroInputPayload.HELD_WALL_RIDE) != 0;
		state.wallRide = state.wallRiding ? input.wall() : 0;
		state.wallClimbing = (input.held() & HeroInputPayload.HELD_WALL_CLIMB) != 0;
		if (!controls(player)) {
			return;
		}
		if ((input.pressed() & HeroInputPayload.PRESS_MELEE) != 0) {
			if (dev.overcraft.lifeweaver.Lifeweaver.sows(player, state)) {
				dev.overcraft.lifeweaver.Lifeweaver.sow(player, state);
			} else if (state.hero == Hero.BRIGITTE) {
				// her quick melee is the flail's swing (1248 #267)
				dev.overcraft.brigitte.Brigitte.melee(player, state);
			} else {
				QuickMelee.use(player, state);
				if (state.hero == Hero.SOMBRA) {
					// quick melee ends her stealth (420C)
					dev.overcraft.sombra.Stealth.end(player, state);
				}
			}
		}
		HeroKit kit = kit(state.hero);
		int pressed = input.pressed();
		if ((pressed & HeroInputPayload.PRESS_CHARGED_SHOWN) != 0) {
			dev.overcraft.sojourn.Railgun.pressedShown(player, state, input);
		}
		if (dev.overcraft.sombra.Hack.silenced(player)) {
			// hacked by an enemy Sombra's EMP: no abilities, no ultimate (0B1F); the weapon and reload still work
			pressed &= ~(HeroInputPayload.PRESS_ABILITY_1 | HeroInputPayload.PRESS_ABILITY_2 | HeroInputPayload.PRESS_ULTIMATE);
		}
		if (kit != null && pressed != 0) {
			kit.press(player, state, pressed);
		}
		if (state.hero == Hero.MERCY && input.extra() != 0) {
			dev.overcraft.mercy.Mercy.extra(player, state, input.extra());
		}
		if (state.hero == Hero.JUNO && input.extra() != 0) {
			dev.overcraft.juno.Juno.extra(player, state, input.extra());
		}
		if (state.hero == Hero.SOJOURN && input.extra() != 0) {
			dev.overcraft.sojourn.Sojourn.extra(player, state, input.extra());
		}
		if (state.hero == Hero.SIGMA && input.extra() != 0) {
			dev.overcraft.sigma.Sigma.extra(player, state, input.extra());
		}
		if (state.hero == Hero.LIFEWEAVER && input.extra() != 0) {
			dev.overcraft.lifeweaver.Lifeweaver.extra(player, state, input.extra());
		}
	}

	public static void tick(MinecraftServer server) {
		dev.overcraft.jetpackcat.JetpackCat.tickLetGo(server.getTickCount());
		dev.overcraft.lifeweaver.Lifeweaver.tickHeals();
		dev.overcraft.ana.Ana.tickDoses(server);
		Sleep.tick(server);
		dev.overcraft.sigma.GraviticFlux.tickAll(server);
		dev.overcraft.lifeweaver.LifeGrip.tick(server);
		dev.overcraft.entity.PetalPlatformEntity.tickLanding();
		dev.overcraft.comms.Comms.tick();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			HeroState state = STATES.get(player.getUUID());
			if (state == null || state.hero == null) {
				if (server.getTickCount() % 10 == 0) {
					syncNone(player);
				}
				continue;
			}
			state.canChangeHero = canChangeHero(player, state);
			HeroKit kit = kit(state.hero);
			boolean controls = controls(player);
			// Jetpack Cat's flight needs Minecraft's permission to fly; handed back as soon as she isn't in play
			dev.overcraft.jetpackcat.JetpackCat.keepFlight(player, state, controls);
			if (!controls && !Sleep.asleep(player)) {
				// (asleep, they're kept: a button held through the sleep fires again as it ends)
				state.primaryHeld = state.secondaryHeld = state.wallClimbing = false;
				state.primaryLetGo = state.secondaryLetGo = false;
			}
			if (player.isAlive()) {
				// over by the distance, not onGround (which read true in mid-air after G): Minecraft clears it on landing, in water, on a ladder. An ability used ends it directly: its landing, Juno's glide (a level glide clears nothing), though the next packet adds to the distance again
				if (player.fallDistance <= 0 || !heroMode(player) || player.isPassenger() || state.softLanding || state.junoGlide >= 0) {
					PLAIN_FALLS.remove(player.getUUID());
				}
				tickRules(player, state);
				// a soft landing doesn't carry over to Minecraft mode, and staying in water or on a ladder has landed them; not what the ability started in or brushes past, nor while it runs (Holds.landingEnds, stayed)
				if (state.softLanding) {
					boolean running = state.gaPhase != 0 || state.dashTicks >= 0 || state.sojournSlide >= 0 || state.bashTicks >= 0 || state.wallRiding || state.sigmaFluxPhase != 0;
					state.softLandingTouch = running ? 0 : landingTouch(player, state.softLandingTouch);
					if (Holds.landingEnds(heroMode(player), state.softLandingTouch, player.isPassenger())) {
						state.softLanding = false;
						state.softLandingTouch = 0;
					}
				} else {
					state.softLandingTouch = 0;
				}
				if (kit != null) {
					kit.tick(player, state, controls);
				}
			} else {
				Integer song = player.getAttached(SONG);
				if (song != null && song != 0) {
					// a hero's tick doesn't run while dead: Lúcio's song ends here (his skates glowed on as he lay dead)
					player.setAttached(SONG, 0);
				}
			}
			if (kit != null) {
				ServerPlayNetworking.send(player, kit.sync(state).withShield((float) state.shield).withWidgets(kit.widgets(player, state)));
			}
			held(state);
		}
	}

	/** After a tick: the buttons it saw pressed are no longer fresh, and a click let go before it is let go now. */
	private static void held(HeroState state) {
		state.primaryFresh = state.secondaryFresh = false;
		if (state.primaryLetGo) {
			state.primaryHeld = state.primaryLetGo = false;
		}
		if (state.secondaryLetGo) {
			state.secondaryHeld = state.secondaryLetGo = false;
		}
	}

	/** Rules every hero shares: passive ult charge and the global regeneration. */
	private static void tickRules(ServerPlayer player, HeroState state) {
		state.rushed = Combat.isRushed(player);
		state.perkTicks++;
		Perks.tick(state);
		QuickMelee.tick(state);
		Combat.tickFire(player, state);
		if (state.ultActive > 0) {
			state.ultActive--;
		} else {
			Combat.addUltCharge(player, Combat.ULT_PASSIVE_PER_SECOND / 20.0, true);
		}
		// Mercy's Swift Recovery starts it sooner; in Valkyrie it never stops (BZ 2023-04-11)
		int regenDelay = state.hero == Hero.MERCY ? dev.overcraft.mercy.MercyStats.REGEN_DELAY : Combat.REGEN_DELAY;
		boolean always = state.hero == Hero.MERCY && dev.overcraft.mercy.Mercy.valkyrie(state);
		if ((always || Combat.now(player) - state.lastDamagedTick >= regenDelay) && player.getHealth() < player.getMaxHealth()) {
			double maxOw = Ow.toOw(player.getMaxHealth());
			Combat.heal(player, (Combat.REGEN_FLAT + Combat.REGEN_PERCENT * maxOw) / 20.0);
		}
		Shields.tick(player, state);
	}

	/** Saves the state's perk progress under its hero (#36). */
	public static void savePerks(ServerPlayer player, HeroState state) {
		if (state.hero != null) {
			player.setAttached(PERKS, Perks.with(player.getAttached(PERKS), state.hero, Perks.save(state)));
		}
	}

	private static void loadPerks(ServerPlayer player, HeroState state) {
		Map<String, Perks.Progress> saved = player.getAttached(PERKS);
		if (state.hero != null && saved != null) {
			Perks.load(state, saved.get(state.hero.id));
		}
	}

	/** Sends "no hero" so the client's HUD clears. */
	public static void syncNone(ServerPlayer player) {
		ServerPlayNetworking.send(player, HeroSyncPayload.NONE);
	}

	/**
	 * At mod start: registers {@link #HERO}. A synced attachment has to be registered before players connect (Fabric
	 * agrees on them while connecting); left to the first event that touches this class, it never syncs.
	 */
	public static void init() {
	}

	public static void clear() {
		STATES.clear();
		PLAIN_FALLS.clear();
	}
}

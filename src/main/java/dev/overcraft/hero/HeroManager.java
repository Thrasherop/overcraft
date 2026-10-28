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
		return hero(player) != null && heroMode(player) && player.isAlive() && !player.isSpectator();
	}

	/**
	 * A fall that hurts nobody right now, whatever its height: the hook for heroes whose abilities land them safely
	 * (later: Winston's Jump Pack, Juno's glide; the owner, #35). Kiriko has none: her falls count from her safe height.
	 */
	public static boolean fallIsSafe(Player player) {
		return false;
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
			}
			player.setAttached(HERO_MODE, on);
		}
	}

	/** How close to their bed (respawn point) a player must be to change heroes (owner, 2026-10-25). */
	public static final double CHANGE_HERO_RANGE = 5;

	/** Changing heroes: the free pick on joining, no hero yet, or within 5 blocks of their bed (like a spawn room). */
	public static boolean canChangeHero(ServerPlayer player, HeroState state) {
		return state.freePick || state.hero == null || nearRespawn(player);
	}

	/** Near the player's bed or respawn anchor, or the world spawn when they have none. */
	private static boolean nearRespawn(ServerPlayer player) {
		ServerPlayer.RespawnConfig config = player.getRespawnConfig();
		GlobalPos pos = config != null ? config.respawnData().globalPos() : player.level().getRespawnData().globalPos();
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
		state.freePick = false;
		if (same) {
			return; // picking the hero you already are changes nothing (no free refill)
		}
		savePerks(player, state);
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
			player.setAttached(HERO_MODE, true);
			Voice.say(player, "select", 1);
		}
	}

	/** Health; {@code fresh} also refills health, ammo and cooldowns (a new pick or a respawn). */
	private static void applyKit(ServerPlayer player, HeroState state, boolean fresh) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(HEALTH_MODIFIER);
			if (state.hero != null) {
				double target = Ow.hp(state.hero.owHealth);
				maxHealth.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, target - maxHealth.getBaseValue(),
					AttributeModifier.Operation.ADD_VALUE));
			}
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		// the hero weapon item that once held slot 1 (hero mode was "slot 1 held" until #26): taken out of old saves
		removeWeapons(player.getInventory());
		if (state.hero == null) {
			return;
		}
		HeroKit kit = kit(state.hero);
		if (fresh) {
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
		HeroState state = STATES.remove(player.getUUID());
		if (state != null && state.hero != null) {
			player.setAttached(ULT_CHARGE, state.ultCharge);
			savePerks(player, state);
		}
		OPTIONS.remove(player.getUUID());
	}

	/** Each player's hero options the server acts on (sent by their client: HeroOptionsPayload). */
	private static final Map<UUID, HeroOptionsPayload> OPTIONS = new HashMap<>();

	public static void setOptions(ServerPlayer player, HeroOptionsPayload options) {
		OPTIONS.put(player.getUUID(), options);
	}

	public static HeroOptionsPayload options(Player player) {
		return OPTIONS.getOrDefault(player.getUUID(), HeroOptionsPayload.DEFAULTS);
	}

	public static void onRespawn(ServerPlayer player) {
		HeroState state = STATES.get(player.getUUID());
		if (state != null) {
			Hero hero = state.hero;
			// the ult's charge and the perks survive dying, as in Overwatch (the charge was being reset with everything else)
			double charge = state.ultCharge;
			Perks.Progress perks = Perks.save(state);
			state.reset(hero);
			state.ultCharge = charge;
			Perks.load(state, perks);
			applyKit(player, state, true);
			Voice.say(player, "respawn", 0.7f);
		}
	}

	public static void onDamaged(Player player) {
		HeroState state = STATES.get(player.getUUID());
		if (state != null) {
			state.lastDamagedTick = Combat.now(player);
		}
	}

	public static void onInput(ServerPlayer player, HeroInputPayload input) {
		HeroState state = STATES.get(player.getUUID());
		if (state == null || state.hero == null || !controls(player)) {
			return;
		}
		state.primaryHeld = (input.held() & HeroInputPayload.HELD_PRIMARY) != 0;
		state.secondaryHeld = (input.held() & HeroInputPayload.HELD_SECONDARY) != 0;
		state.wallRiding = (input.held() & HeroInputPayload.HELD_WALL_RIDE) != 0;
		state.wallClimbing = (input.held() & HeroInputPayload.HELD_WALL_CLIMB) != 0;
		if ((input.pressed() & HeroInputPayload.PRESS_MELEE) != 0) {
			QuickMelee.use(player, state);
		}
		HeroKit kit = kit(state.hero);
		if (kit != null && input.pressed() != 0) {
			kit.press(player, state, input.pressed());
		}
	}

	public static void tick(MinecraftServer server) {
		dev.overcraft.jetpackcat.JetpackCat.tickLetGo(server.overworld().getGameTime());
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
			if (!controls) {
				state.primaryHeld = state.secondaryHeld = state.wallClimbing = false;
			}
			if (player.isAlive()) {
				tickRules(player, state);
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
				ServerPlayNetworking.send(player, kit.sync(state));
			}
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
		if (Combat.now(player) - state.lastDamagedTick >= Combat.REGEN_DELAY && player.getHealth() < player.getMaxHealth()) {
			double maxOw = Ow.toOw(player.getMaxHealth());
			Combat.heal(player, (Combat.REGEN_FLAT + Combat.REGEN_PERCENT * maxOw) / 20.0);
		}
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
	}
}

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
	public static final int HERO_SLOT = 0;

	/**
	 * The chosen hero, saved with the player and kept through death. Synced to every client: a player looks like
	 * their hero to everyone, in hero mode and out of it.
	 */
	public static final AttachmentType<String> HERO = AttachmentRegistry.create(Overcraft.id("hero"),
		builder -> builder.persistent(Codec.STRING).copyOnDeath()
			.syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));

	private static final Identifier HEALTH_MODIFIER = Overcraft.id("hero_health");
	private static final Map<Hero, HeroKit> KITS = new EnumMap<>(Hero.class);
	private static final Map<UUID, HeroState> STATES = new HashMap<>();

	static {
		KITS.put(Hero.KIRIKO, new Kiriko());
	}

	private HeroManager() {
	}

	public static @Nullable HeroKit kit(Hero hero) {
		return KITS.get(hero);
	}

	public static @Nullable HeroState state(Player player) {
		return STATES.get(player.getUUID());
	}

	public static @Nullable Hero hero(Player player) {
		HeroState state = STATES.get(player.getUUID());
		return state == null ? null : state.hero;
	}

	/** The hero's keys are live: a hero, hotbar slot 1, alive and not a spectator. */
	public static boolean controls(Player player) {
		return hero(player) != null && player.getInventory().getSelectedSlot() == HERO_SLOT && player.isAlive()
			&& !player.isSpectator();
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
		state.reset(hero);
		if (hero == null) {
			player.removeAttached(HERO);
		} else {
			player.setAttached(HERO, hero.id);
		}
		applyKit(player, state, true);
		if (hero != null) {
			Voice.say(player, "select", 1, false);
		}
	}

	/** Health and the slot-1 weapon; {@code fresh} also refills health, ammo and cooldowns (a new pick or a respawn). */
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
		removeWeapons(player.getInventory(), null);
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
		enforceWeapon(player, state.hero);
	}

	/** Keeps the hero's weapon in slot 1 (whatever was there moves to a free slot, or is dropped). */
	private static void enforceWeapon(ServerPlayer player, Hero hero) {
		Inventory inventory = player.getInventory();
		ItemStack inSlot = inventory.getItem(HERO_SLOT);
		if (inSlot.getItem() == OcItems.weapon(hero)) {
			removeWeapons(inventory, HERO_SLOT);
			return;
		}
		removeWeapons(inventory, null);
		inSlot = inventory.getItem(HERO_SLOT);
		if (!inSlot.isEmpty()) {
			inventory.setItem(HERO_SLOT, ItemStack.EMPTY);
			if (!inventory.add(inSlot)) {
				player.drop(inSlot, false, Prediction.SERVER_ONLY);
			}
		}
		inventory.setItem(HERO_SLOT, new ItemStack(OcItems.weapon(hero)));
	}

	/** Takes every hero weapon out of the inventory except the one in {@code keepSlot}. */
	private static void removeWeapons(Inventory inventory, @Nullable Integer keepSlot) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if ((keepSlot == null || i != keepSlot) && inventory.getItem(i).getItem() instanceof HeroWeaponItem) {
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
		applyKit(player, state, true);
	}

	public static void onLeave(ServerPlayer player) {
		STATES.remove(player.getUUID());
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
			// the ult's charge survives dying, as in Overwatch (it was being reset with everything else)
			double charge = state.ultCharge;
			state.reset(hero);
			state.ultCharge = charge;
			applyKit(player, state, true);
			Voice.say(player, "respawn", 0.7f, false);
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
		if ((input.pressed() & HeroInputPayload.PRESS_MELEE) != 0) {
			QuickMelee.use(player, state);
		}
		HeroKit kit = kit(state.hero);
		if (kit != null && input.pressed() != 0) {
			kit.press(player, state, input.pressed());
		}
	}

	public static void tick(MinecraftServer server) {
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
			if (!controls) {
				state.primaryHeld = state.secondaryHeld = false;
			}
			if (player.isAlive()) {
				tickRules(player, state);
				if (kit != null) {
					kit.tick(player, state, controls);
				}
				if (server.getTickCount() % 10 == 0) {
					enforceWeapon(player, state.hero);
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
		QuickMelee.tick(state);
		if (state.ultActive > 0) {
			state.ultActive--;
		} else {
			Combat.addUltCharge(player, Combat.ULT_PASSIVE_PER_SECOND / 20.0);
		}
		if (Combat.now(player) - state.lastDamagedTick >= Combat.REGEN_DELAY && player.getHealth() < player.getMaxHealth()) {
			double maxOw = Ow.toOw(player.getMaxHealth());
			Combat.heal(player, (Combat.REGEN_FLAT + Combat.REGEN_PERCENT * maxOw) / 20.0);
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

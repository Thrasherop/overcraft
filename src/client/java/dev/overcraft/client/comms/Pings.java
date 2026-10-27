package dev.overcraft.client.comms;

import dev.overcraft.client.HeroHud;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.hud.HudShapes;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.client.hud.MobFaces;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.PingPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.image;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * Pings, the client's half (#40; the owner's spec and Overwatch 2 screenshots, tools/reference/comms-reference.md).
 * The ping key (Overwatch's PC default, the middle button; rebindable on the Overwatch options screen) pings what the
 * crosshair is on, out to 256 m: a spot gets a small blue diamond with its distance; a second press within 0.35 s
 * makes it a warning (a red circle with a danger mark); an enemy gets a red ring that follows them, their name under
 * it. Notable things (owner, 2026-10-27) get a blue ring with their icon and name: a dropped item, a creature that isn't
 * an enemy (villagers, pets, animals), a vehicle, a notable block (chests, ores, spawners, portals...), a generated
 * structure seen from outside it (the server names those). Your own ping has a leader line to "CANCEL [key]"; pressing
 * the key while aiming at it takes it down. Teammates see them (comms.Pings on the server).
 */
public final class Pings {
	/** How long a ping lasts (ms, ours: Overwatch gives no number). */
	private static final long LIFE_MS = 6500;
	/** A second press this soon is a warning (ms: Overwatch's "Double-press speed" default, 0.35 s, the wiki's Ping page). */
	private static final long DOUBLE_MS = 350;
	private static final double REACH = dev.overcraft.comms.Pings.REACH;
	/**
	 * How far off the crosshair a thing may be and still be pinged (degrees, ours): Overwatch's pings are "more
	 * sensitive to heroes than objects" (the wiki), so enemies get the wider cone.
	 */
	private static final double ENEMY_CONE = 2.5;
	private static final double THING_CONE = 1.5;
	/**
	 * The most the cone widens a thing, however far (blocks, ours): at 100 m the cone alone spans 2.6 blocks, and a
	 * village ping took a cow standing by the house aimed at.
	 */
	private static final double ENEMY_MARGIN = 1.5;
	private static final double THING_MARGIN = 0.6;
	private static final int BLUE = 0xFF29C4F5;
	private static final int RED = 0xFFE8384F;

	private static final class Ping {
		final int owner;
		final int kind;
		final Vec3 at;
		final int entity;
		final String what;
		final long until;
		/** Its entity has been loaded here: once it's gone again (picked up, killed, despawned), so is the ping. */
		boolean seen;

		Ping(PingPayload ping) {
			owner = ping.owner();
			kind = ping.kind();
			at = new Vec3(ping.x(), ping.y(), ping.z());
			entity = ping.entity();
			what = ping.what();
			until = Util.getMillis() + LIFE_MS;
		}
	}

	private static final List<Ping> PINGS = new ArrayList<>();
	private static long lastPress = -10000;
	/** The spot just pinged, for a quick second press to make a warning of it. */
	private static @Nullable Vec3 lastSpot;

	private Pings() {
	}

	/** A ping from the server (a teammate's or the player's own), or a cancel. */
	public static void receive(PingPayload ping) {
		PINGS.removeIf(p -> p.owner == ping.owner());
		if (ping.kind() != PingPayload.CANCEL) {
			PINGS.add(new Ping(ping));
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null && mc.player != null) {
				// ours: a short chime (Overwatch's ping sound isn't in the pack yet)
				mc.level.playLocalSound(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(),
					net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.PLAYERS,
					ping.kind() == PingPayload.WARNING ? 0.5f : 0.35f, ping.kind() == PingPayload.WARNING ? 0.8f : 1.6f, false);
			}
		}
	}

	/** Each client tick in hero mode: the ping key. */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		boolean pressed = false;
		while (HeroControls.get(Action.PING).consumeClick()) {
			pressed = true;
		}
		if (!pressed || player == null || mc.level == null || !ClientPlayNetworking.canSend(PingPayload.TYPE)) {
			return;
		}
		long now = Util.getMillis();
		// a second press soon after placing a spot: that spot, as a warning (Overwatch's double click)
		if (lastSpot != null && now - lastPress < DOUBLE_MS) {
			send(PingPayload.WARNING, lastSpot, -1);
			lastSpot = null;
			lastPress = -10000;
			return;
		}
		lastPress = now;
		lastSpot = null;
		Ping mine = own(player);
		// aiming at your own ping: take it down
		if (mine != null && aimedAt(player, anchor(mine, mc))) {
			send(PingPayload.CANCEL, mine.at, -1);
			return;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1).scale(REACH));
		BlockHitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		boolean missed = block.getType() == HitResult.Type.MISS;
		Entity thing = pick(player, missed ? REACH : eye.distanceTo(block.getLocation()));
		if (thing != null && hostile(player, thing)) {
			send(PingPayload.ENEMY, thing.position(), thing.getId());
		} else if (thing != null && !(thing instanceof Player)) {
			lastSpot = thing.position();
			send(thing instanceof ItemEntity ? PingPayload.ITEM : PingPayload.THING, thing.position(), thing.getId());
		} else if (thing != null) {
			// an ally: the spot they stand on
			lastSpot = thing.position();
			send(PingPayload.SPOT, lastSpot, -1);
		} else if (!missed) {
			BlockPos pos = block.getBlockPos();
			if (mc.level.getBlockState(pos).is(dev.overcraft.comms.Pings.NOTABLE)) {
				lastSpot = Vec3.atCenterOf(pos);
				send(PingPayload.BLOCK, lastSpot, -1);
			} else {
				// the server makes it a structure's ping when it's on one seen from outside
				lastSpot = block.getLocation();
				send(PingPayload.SPOT, lastSpot, -1);
			}
		}
	}

	/**
	 * The thing nearest the crosshair within its cone (enemies first), in front of the first block the view hits: a
	 * creature, a dropped item or a vehicle, not the player or what they ride.
	 */
	private static @Nullable Entity pick(LocalPlayer player, double blockDistance) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1);
		Vec3 end = eye.add(look.scale(Math.min(REACH, blockDistance + 1)));
		Entity best = null;
		double bestScore = Double.MAX_VALUE;
		for (Entity e : mc.level.getEntities(player, new AABB(eye, end).inflate(4),
			e -> e.isAlive() && !e.isSpectator() && !e.isInvisibleTo(player) && e != player.getVehicle()
				&& (e instanceof LivingEntity || e instanceof ItemEntity || e instanceof VehicleEntity))) {
			Vec3 centre = e.getBoundingBox().getCenter();
			double distance = eye.distanceTo(centre);
			boolean enemy = hostile(player, e);
			double cone = Math.toRadians(enemy ? ENEMY_CONE : THING_CONE);
			double margin = Math.min(distance * Math.tan(cone), enemy ? ENEMY_MARGIN : THING_MARGIN);
			Optional<Vec3> hit = e.getBoundingBox().inflate(Math.max(0.05, margin)).clip(eye, end);
			if (hit.isEmpty() || eye.distanceTo(hit.get()) > blockDistance + 0.3) {
				continue;
			}
			// the angle off the crosshair, enemies ahead of everything else
			double angle = Math.acos(Math.clamp(look.dot(centre.subtract(eye).normalize()), -1, 1));
			double score = angle + (enemy ? 0 : 10);
			if (score < bestScore) {
				bestScore = score;
				best = e;
			}
		}
		return best;
	}

	/**
	 * An enemy to ping red: an enemy player, a monster, or a creature after a player (an angry wolf, golem, bee); the
	 * rest of what Allies calls enemies (cows, sheep: things a hero may hit) are notable things.
	 */
	private static boolean hostile(LocalPlayer player, Entity e) {
		return Allies.isEnemy(player, e) && (e instanceof Player || e instanceof Enemy || e instanceof Mob mob && mob.getTarget() instanceof Player);
	}

	private static void send(int kind, Vec3 at, int entity) {
		ClientPlayNetworking.send(new PingPayload(-1, kind, at.x, at.y, at.z, entity, ""));
	}

	private static @Nullable Ping own(LocalPlayer player) {
		for (Ping ping : PINGS) {
			if (ping.owner == player.getId()) {
				return ping;
			}
		}
		return null;
	}

	/** The crosshair is within a few degrees of the spot (ours: 4°). */
	private static boolean aimedAt(LocalPlayer player, Vec3 at) {
		Vec3 to = at.subtract(player.getEyePosition());
		return to.lengthSqr() > 1.0E-4 && to.normalize().dot(player.getViewVector(1)) > Math.cos(Math.toRadians(4));
	}

	/** The ping's entity here, if it follows one and it's loaded. */
	private static @Nullable Entity target(Ping ping, Minecraft mc) {
		return PingPayload.follows(ping.kind) && ping.entity >= 0 && mc.level != null ? mc.level.getEntity(ping.entity) : null;
	}

	/** Where the marker sits: over the thing it follows (where it was, if it isn't loaded here), else the spot. */
	private static Vec3 anchor(Ping ping, Minecraft mc) {
		Entity target = target(ping, mc);
		if (target != null) {
			return target.position().add(0, target.getBbHeight() + 0.6, 0);
		}
		return PingPayload.follows(ping.kind) ? ping.at.add(0, 1.2, 0) : ping.at;
	}

	/** Drawn over the view in HeroHud's reference pixels. */
	public static void render(GuiGraphicsExtractor g, float w, float h) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			return;
		}
		long now = Util.getMillis();
		for (Iterator<Ping> it = PINGS.iterator(); it.hasNext(); ) {
			Ping ping = it.next();
			Entity target = target(ping, mc);
			if (target != null) {
				ping.seen = true;
			}
			boolean gone = PingPayload.follows(ping.kind) && (target == null ? ping.seen : !target.isAlive());
			if (now > ping.until || gone) {
				it.remove();
				continue;
			}
			Vec3 at = anchor(ping, mc);
			float[] screen = HeroHud.project(at, w, h);
			if (screen == null) {
				continue;
			}
			float x = Math.clamp(screen[0], 40, w - 40);
			float y = Math.clamp(screen[1], 40, h - 40);
			int distance = (int) Math.round(player.getEyePosition().distanceTo(at));
			boolean mine = ping.owner == player.getId();
			if (ping.kind == PingPayload.ENEMY) {
				enemyMarker(g, target instanceof LivingEntity enemy ? enemy : null, ping.what, x, y, distance);
			} else if (ping.kind == PingPayload.WARNING) {
				image(g, HudShapes.DISC, x - 22, y - 22, 44, 44, 0xE6B0202F);
				image(g, HudShapes.RING, x - 23, y - 23, 46, 46, RED);
				HudText.draw(g, Face.BODY, "!", x, y + 9, 22, 0xFFFFFFFF, Align.CENTER);
				HudText.draw(g, Face.SMALL, distance + "m", x, y - 30, 11, 0xFFFFFFFF, Align.CENTER);
				if (mine) {
					cancelLabel(g, x + 14, y - 14, RED);
				}
			} else if (ping.kind == PingPayload.SPOT) {
				// the blue diamond: a 15 px square turned 45° (about 22 px corner to corner)
				g.pose().pushMatrix();
				g.pose().translate(x, y);
				g.pose().rotate((float) Math.toRadians(45));
				fill(g, -7.5f, -7.5f, 15, 15, BLUE);
				outline(g, -7.5f, -7.5f, 15, 15, 1, 0xFFB9EEFF);
				g.pose().popMatrix();
				HudText.draw(g, Face.SMALL, distance + "m", x, y - 20, 11, 0xFFFFFFFF, Align.CENTER);
				if (mine) {
					cancelLabel(g, x + 10, y - 10, BLUE);
				}
			} else {
				notableMarker(g, ping, target, x, y, distance);
				if (mine) {
					cancelLabel(g, x + 16, y - 16, BLUE);
				}
			}
		}
	}

	/** An enemy: a red ring holding their hero's portrait (a mob: its face, else its name's first letter), the name. */
	private static void enemyMarker(GuiGraphicsExtractor g, @Nullable LivingEntity enemy, String type, float x, float y, int distance) {
		image(g, HudShapes.DISC, x - 27, y - 27, 54, 54, 0xE61A1015);
		Hero hero = enemy instanceof Player p ? HeroManager.hero(p) : null;
		Identifier portrait = hero == null ? null : dev.overcraft.client.HeroArt.portrait(hero);
		String name = (hero != null ? hero.displayName : enemy != null ? enemy.getName().getString() : entityName(type))
			.toUpperCase(Locale.ROOT);
		MobFaces.MobFace face = portrait == null ? MobFaces.get(type) : null;
		if (portrait != null) {
			image(g, portrait, x - 24, y - 24, 48, 48, 0xFFFFFFFF);
		} else if (face != null) {
			MobFaces.draw(g, face, x - 15, y - 15, 30, 30, 0xFFFFFFFF);
		} else {
			HudText.draw(g, Face.NUMBERS, name.isEmpty() ? "?" : name.substring(0, 1), x, y + 12, 30, 0xFFFFFFFF, Align.CENTER);
		}
		image(g, HudShapes.RING, x - 28, y - 28, 56, 56, RED);
		HudText.draw(g, Face.SMALL, distance + "m", x, y - 34, 11, 0xFFFFFFFF, Align.CENTER);
		HudText.draw(g, Face.BODY, name, x, y + 46, 13, RED, Align.CENTER);
	}

	/**
	 * A notable thing (ours, in the enemy marker's shape and the spot's blue: Overwatch's own are its health packs): a
	 * blue ring holding its icon (an item's or block's, a creature's face or spawn egg, a structure's emblem), the
	 * distance over it, its name under it.
	 */
	private static void notableMarker(GuiGraphicsExtractor g, Ping ping, @Nullable Entity target, float x, float y, int distance) {
		image(g, HudShapes.DISC, x - 23, y - 23, 46, 46, 0xE60B2236);
		String name;
		MobFaces.MobFace face = null;
		ItemStack icon = ItemStack.EMPTY;
		switch (ping.kind) {
			case PingPayload.ITEM -> {
				ItemStack stack = target instanceof ItemEntity item ? item.getItem() : new ItemStack(item(ping.what));
				icon = stack;
				name = stack.getHoverName().getString() + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
			}
			case PingPayload.THING -> {
				face = MobFaces.get(ping.what);
				ItemStack pick = target != null ? target.getPickResult() : null;
				icon = pick != null ? pick : spawnEgg(ping.what);
				name = target != null ? target.getName().getString() : entityName(ping.what);
			}
			case PingPayload.BLOCK -> {
				Block block = BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(ping.what));
				icon = blockIcon(block);
				name = block.getName().getString();
			}
			default -> {
				icon = new ItemStack(structureIcon(ping.what));
				name = structureName(ping.what);
			}
		}
		if (face != null) {
			MobFaces.draw(g, face, x - 14, y - 14, 28, 28, 0xFFFFFFFF);
		} else if (!icon.isEmpty()) {
			g.pose().pushMatrix();
			g.pose().translate(x - 14, y - 14);
			g.pose().scale(1.75f, 1.75f);
			g.item(icon, 0, 0);
			g.pose().popMatrix();
		}
		image(g, HudShapes.RING, x - 24, y - 24, 48, 48, BLUE);
		HudText.draw(g, Face.SMALL, distance + "m", x, y - 30, 11, 0xFFFFFFFF, Align.CENTER);
		// a dark edge (ours) keeps the blue name readable over the bright thing it names and the sky
		HudText.outlined(g, Face.BODY, name.toUpperCase(Locale.ROOT), x, y + 40, 12, BLUE, 0xB3101820, 1.2f, Align.CENTER);
	}

	private static Item item(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
	}

	private static @Nullable EntityType<?> entityType(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
	}

	private static String entityName(String id) {
		EntityType<?> type = entityType(id);
		return type == null ? "" : type.getDescription().getString();
	}

	private static ItemStack spawnEgg(String id) {
		EntityType<?> type = entityType(id);
		return type == null ? ItemStack.EMPTY : SpawnEggItem.byId(type).map(ItemStack::new).orElse(ItemStack.EMPTY);
	}

	/** A block's own item; portals, which have none, their frame's. */
	private static ItemStack blockIcon(Block block) {
		Item item = block.asItem();
		if (item != Items.AIR) {
			return new ItemStack(item);
		}
		String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
		return new ItemStack(id.equals("nether_portal") ? Items.OBSIDIAN : id.startsWith("end_") ? Items.END_PORTAL_FRAME : Items.MAP);
	}

	/** A vanilla structure's family: its variants (village_plains, ruined_portal_swamp, ...) share a name and emblem. */
	private static String family(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key == null || !key.getNamespace().equals("minecraft")) {
			return "";
		}
		String path = key.getPath();
		for (String family : List.of("village", "ruined_portal", "abandoned_camp", "ocean_ruin", "shipwreck", "mineshaft")) {
			if (path.startsWith(family)) {
				return family;
			}
		}
		return path;
	}

	/** Each structure's emblem (ours): something it's known for. */
	private static final Map<String, Item> STRUCTURE_ICONS = Map.ofEntries(
		Map.entry("village", Items.BELL),
		Map.entry("pillager_outpost", Items.CROSSBOW),
		Map.entry("mansion", Items.TOTEM_OF_UNDYING),
		Map.entry("desert_pyramid", Items.CHISELED_SANDSTONE),
		Map.entry("jungle_pyramid", Items.MOSSY_COBBLESTONE),
		Map.entry("igloo", Items.SNOW_BLOCK),
		Map.entry("swamp_hut", Items.CAULDRON),
		Map.entry("shipwreck", Items.OAK_BOAT),
		Map.entry("ocean_ruin", Items.PRISMARINE_BRICKS),
		Map.entry("ruined_portal", Items.CRYING_OBSIDIAN),
		Map.entry("stronghold", Items.ENDER_EYE),
		Map.entry("mineshaft", Items.RAIL),
		Map.entry("monument", Items.PRISMARINE),
		Map.entry("fortress", Items.NETHER_BRICKS),
		Map.entry("bastion_remnant", Items.GILDED_BLACKSTONE),
		Map.entry("end_city", Items.PURPUR_BLOCK),
		Map.entry("ancient_city", Items.SCULK_SHRIEKER),
		Map.entry("trail_ruins", Items.BRUSH),
		Map.entry("trial_chambers", Items.TRIAL_KEY),
		Map.entry("buried_treasure", Items.HEART_OF_THE_SEA),
		Map.entry("nether_fossil", Items.BONE_BLOCK),
		Map.entry("abandoned_camp", Items.CAMPFIRE));

	private static Item structureIcon(String id) {
		return STRUCTURE_ICONS.getOrDefault(family(id), Items.MAP);
	}

	/** A structure's name: ours for Minecraft's (it names none), else its id's path in words. */
	private static String structureName(String id) {
		String family = family(id);
		String key = "overcraft.structure." + family;
		if (!family.isEmpty() && Language.getInstance().has(key)) {
			return Component.translatable(key).getString();
		}
		Identifier parsed = Identifier.tryParse(id);
		String path = parsed == null ? id : parsed.getPath();
		StringBuilder words = new StringBuilder();
		for (String word : path.replace('/', '_').split("_")) {
			if (!word.isEmpty()) {
				words.append(words.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
		}
		return words.toString();
	}

	/** Your own ping's leader line up to the right and its "CANCEL [key]" label. */
	private static void cancelLabel(GuiGraphicsExtractor g, float x, float y, int colour) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().rotate((float) Math.toRadians(-33));
		fill(g, 0, -0.75f, 62, 1.5f, colour);
		g.pose().popMatrix();
		float lx = x + 52;
		float ly = y - 34 - 13;
		String label = Component.translatable("overcraft.ping.cancel").getString().toUpperCase(Locale.ROOT);
		String key = HeroHud.shortKey(HeroControls.get(Action.PING));
		float labelW = HudText.width(Face.BODY, label, 12);
		float keyW = HudText.width(Face.SMALL, key, 10) + 8;
		float width = 10 + labelW + 6 + keyW + 5;
		fill(g, lx, ly, width, 26, colour == RED ? 0xE6601020 : 0xE60B3A5C);
		outline(g, lx, ly, width, 26, 1, colour);
		HudText.draw(g, Face.BODY, label, lx + 10, ly + 13 + 6, 12, 0xFFFFFFFF, Align.LEFT);
		fill(g, lx + 10 + labelW + 6, ly + 4, keyW, 18, 0xF2E6EEF0);
		HudText.draw(g, Face.SMALL, key, lx + 10 + labelW + 6 + keyW / 2, ly + 13 + 5, 10, 0xFF15181C, Align.CENTER);
	}

	/**
	 * The outline (PingOutlineMixin) a live ping puts on this entity: red on a pinged enemy, blue on a pinged item or
	 * creature (ours); -1 for none.
	 */
	public static int outlineColour(Entity entity) {
		if (PINGS.isEmpty()) {
			return -1;
		}
		long now = Util.getMillis();
		for (Ping ping : PINGS) {
			if (PingPayload.follows(ping.kind) && ping.entity == entity.getId() && now <= ping.until) {
				return (ping.kind == PingPayload.ENEMY ? RED : BLUE) & 0xFFFFFF;
			}
		}
		return -1;
	}

	public static void clear() {
		PINGS.clear();
	}
}

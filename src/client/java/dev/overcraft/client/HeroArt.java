package dev.overcraft.client;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The heroes' Overwatch art for the UI (portraits, full-body art, ability icons), from the player's local asset pack
 * (tools/build_asset_pack.py ui: never shipped). Every getter is null without the pack: the UI then draws its own
 * placeholder shapes.
 */
public final class HeroArt {
	private static final Map<Identifier, Boolean> PRESENT = new HashMap<>();

	private HeroArt() {
	}

	/** Square head-and-shoulders portrait (256 px). */
	public static @Nullable Identifier portrait(Hero hero) {
		return art(hero, "portrait");
	}

	/**
	 * Overwatch's HUD head (512 px, the head cut out on a clear ground, its shoulders cut to a V): the one its HUD
	 * frame shows, hair breaking out over the top.
	 */
	public static @Nullable Identifier head(Hero hero) {
		return art(hero, "head");
	}

	/** Head and shoulders in a circle (200 x 280). */
	public static @Nullable Identifier bust(Hero hero) {
		return art(hero, "bust");
	}

	/** Lit art from the knees up (368 x 500). */
	public static @Nullable Identifier card(Hero hero) {
		return art(hero, "card");
	}

	/** Full-body art (652 x 1080), dark and tinted: a backdrop more than a picture. */
	public static @Nullable Identifier fullBody(Hero hero) {
		return art(hero, "fullbody");
	}

	/** An ability's icon, by its in-game name's slug ("kunai", "swift_step", ...). */
	public static @Nullable Identifier ability(Hero hero, String ability) {
		// Mercy's staff has one icon for both its beams (her data's loadouts)
		return art(hero, "abilities/" + (ability.equals("damage_boost") ? "caduceus_staff" : ability));
	}

	/** Lúcio's song icon under the crosshair (#61): the heal song's plus or the speed song's chevrons, white on clear. */
	public static @Nullable Identifier songGlyph(boolean speed) {
		return art(Hero.LUCIO, "song/" + (speed ? "speed" : "heal"));
	}

	/** A perk's icon (a white glyph on clear), by its id ("urgent_care"). */
	public static @Nullable Identifier perk(Hero hero, String perk) {
		return art(hero, "perks/" + perk);
	}

	/** One of the kill feed's own icons ("melee": Overwatch's Quick Melee fist), when the local pack has it. */
	public static @Nullable Identifier killFeed(String name) {
		return present(Overcraft.id("textures/gui/killfeed/" + name + ".png"));
	}

	/** A role's badge (tank, damage, support). */
	public static @Nullable Identifier role(Hero.Role role) {
		return present(Overcraft.id("textures/gui/roles/" + role.name().toLowerCase(Locale.ROOT) + ".png"));
	}

	private static @Nullable Identifier art(Hero hero, String name) {
		return present(Overcraft.id("textures/gui/heroes/" + hero.id + "/" + name + ".png"));
	}

	/** Whether the resource packs have this file (the local pack's fonts, say). */
	public static boolean has(Identifier id) {
		return present(id) != null;
	}

	private static @Nullable Identifier present(Identifier id) {
		boolean present = PRESENT.computeIfAbsent(id, i -> Minecraft.getInstance().getResourceManager().getResource(i).isPresent());
		return present ? id : null;
	}

	/** Resource packs changed: look again. */
	public static void clear() {
		PRESENT.clear();
	}
}

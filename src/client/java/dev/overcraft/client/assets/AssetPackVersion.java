package dev.overcraft.client.assets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.overcraft.Overcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.util.Optional;

/**
 * Which build the local asset pack was made for (assets/overcraft/asset_pack.json, written by build_asset_pack.py), so
 * a jar run with an older zip says so instead of quietly playing the wrong clips or no sounds (#21).
 */
public final class AssetPackVersion {
	private static @Nullable String version;
	private static boolean read;

	private AssetPackVersion() {
	}

	/** The pack's build version, or null without a pack or from before packs carried one. */
	public static @Nullable String get() {
		if (!read) {
			read = true;
			version = null;
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Overcraft.id("asset_pack.json"));
			if (resource.isPresent()) {
				try (Reader in = resource.get().openAsReader()) {
					JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
					version = json.has("version") ? json.get("version").getAsString() : null;
				} catch (Exception e) {
					Overcraft.LOG.warn("Couldn't read the asset pack's version: {}", e.toString());
				}
			}
		}
		return version;
	}

	/** Resource packs changed: read it again. */
	public static void clear() {
		read = false;
	}

	/** Whether a pack is installed at all. */
	public static boolean installed() {
		return LocalAssetPack.find() != null;
	}

	/** The pack is older than this build needs. */
	public static boolean outdated() {
		String v = get();
		return installed() && (v == null || Overcraft.compareVersions(v, Overcraft.ASSETS_NEEDED) < 0);
	}

	/** The chat line telling the player their pack is too old, or null when it's fine. */
	public static @Nullable Component warning() {
		if (!outdated()) {
			return null;
		}
		String v = get();
		return v == null ? Component.translatable("overcraft.assets.unknown", Overcraft.ASSETS_NEEDED)
			: Component.translatable("overcraft.assets.outdated", v, Overcraft.version(), Overcraft.ASSETS_NEEDED);
	}
}

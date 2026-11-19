package dev.overcraft.client.assets;

import dev.overcraft.Overcraft;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The player's local Overwatch assets (hero models, textures, sounds), mounted as an always-on resource pack on top
 * of the mod's placeholders. They never come with the mod: the player makes the pack on their own machine
 * (tools/build_asset_pack.py) and it's read from outside the jar.
 *
 * Where: {@code -Dovercraft.assetPack=<zip or folder>}, else {@code <game dir>/overcraft/overcraft-assets.zip}, else
 * the folder {@code <game dir>/overcraft/overcraft-assets/}, else {@code <game dir>/mods/<modsZipName()>} (owner,
 * 2026-11-18: beside the jar is the easiest install; Fabric loads only .jar files from mods/, so the zip is left alone
 * there). Missing: placeholders only.
 */
public final class LocalAssetPack implements RepositorySource {
	public static final String ID = "overcraft:local_assets";

	@Override
	public void loadPacks(Consumer<Pack> result) {
		Path path = find();
		if (path == null) {
			Overcraft.LOG.info("No local Overwatch asset pack (looked for overcraft/overcraft-assets.zip): placeholders only");
			return;
		}
		Pack.ResourcesSupplier resources = Files.isDirectory(path)
			? new PathPackResources.PathResourcesSupplier(path)
			: new FilePackResources.FileResourcesSupplier(path);
		// labelled with the mod's own display name (fabric.mod.json), not a fixed one: a build can go by another name
		String name = FabricLoader.getInstance().getModContainer("overcraft").map(m -> m.getMetadata().getName()).orElse("Overcraft");
		PackLocationInfo location = new PackLocationInfo(ID, Component.literal(name + ": local assets"), PackSource.BUILT_IN, Optional.empty());
		Pack.Metadata metadata = new Pack.Metadata(Component.literal(path.getFileName().toString()), PackCompatibility.COMPATIBLE,
			FeatureFlagSet.of(), List.of());
		result.accept(new Pack(location, resources, metadata, new PackSelectionConfig(true, Pack.Position.TOP, false)));
		Overcraft.LOG.info("Local Overwatch asset pack: {}", path);
	}

	/**
	 * The zip's name in mods/: the mod's display name (fabric.mod.json) in lower case, spaces as dashes, then "-assets.zip":
	 * overcraft-assets.zip, or mystery-mod-assets.zip for a build named "Mystery Mod" (owner, 2026-11-18: one name to tell
	 * the players).
	 */
	public static String modsZipName() {
		String name = FabricLoader.getInstance().getModContainer("overcraft").map(m -> m.getMetadata().getName()).orElse("Overcraft");
		return name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-") + "-assets.zip";
	}

	public static @Nullable Path find() {
		String override = System.getProperty("overcraft.assetPack");
		if (override != null && !override.isBlank()) {
			Path path = Path.of(override);
			if (Files.exists(path)) {
				return path;
			}
			// a missing override falls through to the usual places
		}
		Path dir = FabricLoader.getInstance().getGameDir().resolve("overcraft");
		for (Path candidate : new Path[] {dir.resolve("overcraft-assets.zip"), dir.resolve("overcraft-assets")}) {
			if (Files.exists(candidate)) {
				return candidate;
			}
		}
		Path inMods = FabricLoader.getInstance().getGameDir().resolve("mods").resolve(modsZipName());
		if (Files.exists(inMods)) {
			return inMods;
		}
		return null;
	}
}

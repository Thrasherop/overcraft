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
 * the folder {@code <game dir>/overcraft/overcraft-assets/}. Missing: placeholders only.
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
		PackLocationInfo location = new PackLocationInfo(ID, Component.literal("Overcraft: local Overwatch assets"), PackSource.BUILT_IN, Optional.empty());
		Pack.Metadata metadata = new Pack.Metadata(Component.literal(path.getFileName().toString()), PackCompatibility.COMPATIBLE,
			FeatureFlagSet.of(), List.of());
		result.accept(new Pack(location, resources, metadata, new PackSelectionConfig(true, Pack.Position.TOP, false)));
		Overcraft.LOG.info("Local Overwatch asset pack: {}", path);
	}

	public static @Nullable Path find() {
		String override = System.getProperty("overcraft.assetPack");
		if (override != null && !override.isBlank()) {
			Path path = Path.of(override);
			return Files.exists(path) ? path : null;
		}
		Path dir = FabricLoader.getInstance().getGameDir().resolve("overcraft");
		for (Path candidate : new Path[] {dir.resolve("overcraft-assets.zip"), dir.resolve("overcraft-assets")}) {
			if (Files.exists(candidate)) {
				return candidate;
			}
		}
		return null;
	}
}

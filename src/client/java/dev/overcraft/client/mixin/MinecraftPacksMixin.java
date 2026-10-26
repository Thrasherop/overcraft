package dev.overcraft.client.mixin;

import dev.overcraft.client.assets.LocalAssetPack;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;

/** Adds the player's local Overwatch asset pack to the client's resource packs (see {@link LocalAssetPack}). */
@Mixin(Minecraft.class)
public abstract class MinecraftPacksMixin {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"))
	private RepositorySource[] overcraft$localAssets(RepositorySource[] sources) {
		RepositorySource[] more = Arrays.copyOf(sources, sources.length + 1);
		more[sources.length] = new LocalAssetPack();
		return more;
	}
}

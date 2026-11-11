/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package dev.overcraft.vendor.arcade.replay.mixins.common;

import com.llamalad7.mixinextras.sugar.Local;
import dev.overcraft.vendor.arcade.replay.recorder.player.ReplayPlayerRecorders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerLevel.class)
// Overcraft: only the player recorder's half is vendored (chunk recorders aren't)
public abstract class ServerLevelMixin extends Level {
    protected ServerLevelMixin(
        WritableLevelData levelData,
        ResourceKey<Level> dimension,
        RegistryAccess registryAccess,
        Holder<DimensionType> dimensionTypeRegistration,
        boolean isClientSide,
        boolean isDebug,
        long biomeZoomSeed,
        int maxChainedNeighborUpdates
    ) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Shadow
    @Nullable
    public abstract Entity getEntity(int id);

	@Inject(
        method = "destroyBlockProgress",
        at = @At("TAIL")
    )
    private void broadcastBlockDestroyProgressToRecorders(int id, BlockPos blockPos, int progress, CallbackInfo ci) {
        Entity breaker = this.getEntity(id);
        if (breaker instanceof ServerPlayer player) {
			ReplayPlayerRecorders.record(player, new ClientboundBlockDestructionPacket(id, blockPos, progress));
        }
    }
}

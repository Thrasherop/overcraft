/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
// Vendored into Overcraft from Arcade (arcade-utils): see ../../../LICENSE-arcade.
package dev.overcraft.vendor.arcade.replay.mixins.utils;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.TrackedEntity.class)
public interface TrackedEntityAccessor {
    @Accessor("entity")
    Entity arcade_getEntity();

    @Accessor("serverEntity")
    ServerEntity arcade_getServerEntity();

    @Invoker("getEffectiveRange")
    int arcade_getRange();
}

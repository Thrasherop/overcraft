/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
// Vendored into Overcraft from Arcade (arcade-utils): see ../../../LICENSE-arcade.
package dev.overcraft.vendor.arcade.replay.mixins.utils;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerCommonPacketListenerImpl.class)
public interface ServerCommonPacketListenerAccessor {
    @Accessor("connection")
    Connection arcade_getConnection();
}

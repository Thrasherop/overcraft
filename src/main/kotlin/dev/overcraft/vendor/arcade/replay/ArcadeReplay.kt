/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package dev.overcraft.vendor.arcade.replay

import dev.overcraft.vendor.arcade.utils.Identifier
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import net.fabricmc.loader.impl.metadata.AbstractModMetadata
import net.minecraft.resources.Identifier

// Overcraft: not a mod entrypoint here; Overcraft ticks the recorders itself (ReplayPlayerRecorders.tickAll).
public object ArcadeReplay {
    public const val MOD_ID: String = "arcade-replay"

    public fun id(path: String): Identifier {
        return Identifier(MOD_ID, path)
    }

    internal fun getLoadedMods(): Map<String, String> {
        return FabricLoader.getInstance().allMods
            .filter { it.origin.kind != ModOrigin.Kind.NESTED }
            .filter { it.metadata.type != AbstractModMetadata.TYPE_BUILTIN }
            .associateBy({ it.metadata.id }, { it.metadata.version.friendlyString })
    }
}
/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package dev.overcraft.vendor.arcade.replay.viewer

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import dev.overcraft.vendor.arcade.replay.ducks.ReplayViewable
import dev.overcraft.vendor.arcade.replay.mixins.viewer.ClientboundPlayerInfoUpdatePacketAccessor
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.ProtocolInfo
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.configuration.ConfigurationProtocols
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry
import net.minecraft.server.network.ServerGamePacketListenerImpl
import java.util.*

public object ReplayViewerUtils {
    public fun ServerGamePacketListenerImpl.sendReplayPacket(packet: Packet<*>) {
        (this as ReplayViewable).arcade_sendReplayViewerPacket(packet)
    }

    public fun ServerGamePacketListenerImpl.startViewingReplay(viewer: ReplayViewer) {
        (this as ReplayViewable).arcade_startViewingReplay(viewer)
    }

    public fun ServerGamePacketListenerImpl.stopViewingReplay() {
        (this as ReplayViewable).arcade_stopViewingReplay()
    }

    public fun ServerGamePacketListenerImpl.getViewingReplay(): ReplayViewer? {
        return (this as ReplayViewable).arcade_getViewingReplay()
    }

    public fun createClientboundPlayerInfoUpdatePacket(
        actions: EnumSet<Action>,
        entries: List<Entry>
    ): ClientboundPlayerInfoUpdatePacket {
        val packet = ClientboundPlayerInfoUpdatePacket(actions, listOf())
        @Suppress("KotlinConstantConditions")
        (packet as ClientboundPlayerInfoUpdatePacketAccessor).arcade_setEntries(entries)
        return packet
    }
}
/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package dev.overcraft.vendor.arcade.replay.recorder.rejoin

import io.netty.channel.ChannelFutureListener
import dev.overcraft.vendor.arcade.replay.mixins.rejoin.ServerConfigurationPacketListenerImplAccessor
import dev.overcraft.vendor.arcade.utils.ArcadeUtils
import dev.overcraft.vendor.arcade.utils.server
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerboundPongPacket
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ConfigurationTask
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl
import net.minecraft.server.network.config.SynchronizeRegistriesTask
import java.util.*

public class RejoinConfigurationPacketListener(
    private val replay: RejoinedReplayPlayer,
    connection: Connection,
    cookies: CommonListenerCookie
): ServerConfigurationPacketListenerImpl(replay.server, connection, cookies) {
    @Suppress("CAST_NEVER_SUCCEEDS")
    private val tasks: Queue<ConfigurationTask>
        get() = (this as ServerConfigurationPacketListenerImplAccessor).arcade_getTasks()

    private var handledPong = false

    override fun startConfiguration() {
        super.startConfiguration()

        if (!this.handledPong) {
            this.handledPong = true
            // Fabric api pings the client before doing any more
            // configuration checks.
            // We must manually pong.
            this.handlePong(ServerboundPongPacket(0))
        }
    }

    public fun runConfigurationTasks() {
        // We do not have to wait for the client to respond
        for (task in this.tasks) {
            @Suppress("CAST_NEVER_SUCCEEDS")
            (this as ServerConfigurationPacketListenerImplAccessor).arcade_setCurrentTask(task)
            task.start(this::send)
            if (task is SynchronizeRegistriesTask) {
                task.handleResponse(listOf(), this::send)
            }
        }
    }

    override fun send(packet: Packet<*>, sendListener: ChannelFutureListener?) {
        try {
            this.replay.recorder.record(packet)
        } catch (e: Exception) {
            val name = this.replay.original.scoreboardName
            ArcadeUtils.logger.error("Failed to record rejoin configuration packet {} for {}", packet, name, e)
        }
    }
}
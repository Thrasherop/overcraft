/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package dev.overcraft.vendor.arcade.replay.viewer

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSets
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSets
import kotlinx.coroutines.*
import dev.overcraft.vendor.arcade.replay.io.reader.ReplayReader
import dev.overcraft.vendor.arcade.replay.mixins.viewer.EntityInvoker
import dev.overcraft.vendor.arcade.replay.recorder.rejoin.RejoinedReplayPlayer
import dev.overcraft.vendor.arcade.replay.util.ReplayMarker
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewerUtils.getViewingReplay
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewerUtils.sendReplayPacket
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewerUtils.startViewingReplay
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewerUtils.stopViewingReplay
import dev.overcraft.vendor.arcade.utils.ArcadeUtils
import dev.overcraft.vendor.arcade.utils.DateTimeUtils.formatHHMMSS
import dev.overcraft.vendor.arcade.utils.server
import net.minecraft.core.UUIDUtil
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.ProtocolInfo
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.game.*
import net.minecraft.network.protocol.game.ClientboundGameEventPacket.CHANGE_GAME_MODE
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.BossEvent.BossBarColor
import net.minecraft.world.BossEvent.BossBarOverlay
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.entity.Relative
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import java.io.InputStream
import java.util.*
import java.util.function.Supplier
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrElse
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public class ReplayViewer internal constructor(
    supplier: (ReplayViewer) -> ReplayReader,
    public val connection: ServerGamePacketListenerImpl
) {
    private val reader = supplier.invoke(this)
    private val markers by lazy { this.reader.readMarkers() }


    private var started = false
    private var teleported = false

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val coroutineContext: ExecutorCoroutineDispatcher = newSingleThreadContext("replay-viewer")
    private val coroutineScope = CoroutineScope(this.coroutineContext + Job())

    private var tickSpeed = 20.0F
    private var tickFrozen = false

    private val chunks = LongSets.synchronize(LongOpenHashSet())
    private val entities = IntSets.synchronize(IntOpenHashSet())
    private val players = Collections.synchronizedList(ArrayList<UUID>())
    private val objectives = Collections.synchronizedCollection(ArrayList<String>())

    private val bossbar = ServerBossEvent(UUID.randomUUID(), Component.empty(), BossBarColor.BLUE, BossBarOverlay.PROGRESS)

    // Overcraft (POTG): the entity whose eyes the viewer looks through, a recorded tick to stop at, and what to do then
    // (wall clock: the stream's own delays, never server ticks, since the integrated server pauses), boss bars the
    // replay has added (an update to one it never added kicked the client: the spike's End run)
    @Volatile public var cameraEntity: Int = -1
    @Volatile public var stopAt: Duration = Duration.INFINITE
    @Volatile public var onFinished: Runnable? = null
    private var finished = false
    private val bossBars = Collections.synchronizedSet(HashSet<UUID>())

    private var lastSentProgress = Duration.INFINITE
    private var progress = Duration.ZERO
    private var target = Duration.ZERO

    private var position = Vec3.ZERO

    public val gameProtocol: ProtocolInfo<ClientGamePacketListener> = GameProtocols.CLIENTBOUND_TEMPLATE.bind(
        RegistryFriendlyByteBuf.decorator(this.server.registryAccess())
    )

    public val server: MinecraftServer
        get() = this.player.server

    public val player: ServerPlayer
        get() = this.connection.player

    public var speedMultiplier: Float = 1.0F
        private set
    public var paused: Boolean = false
        private set

    public fun start() {
        if (this.started) {
            return
        }
        if (this.connection.getViewingReplay() != null) {
            ArcadeUtils.logger.error("Player ${this.player.scoreboardName} tried watching 2 replays at once?!")
            return
        }

        this.started = true
        this.setForReplayView()

        this.restart()
    }

    public fun stop() {
        this.close()

        this.removeReplayState()
        this.addBackToServer()
    }

    public fun close() {
        this.coroutineScope.coroutineContext.cancelChildren()
        this.coroutineScope.launch {
            reader.close()
        }
        this.coroutineContext.close()
        this.connection.stopViewingReplay()

        ReplayViewers.remove(this.player.uuid)
    }

    public fun jumpTo(timestamp: Duration): Boolean {
        if (timestamp.isNegative() || timestamp > this.reader.duration) {
            return false
        }

        this.coroutineScope.launch {
            if (reader.jumpTo(timestamp) || progress > timestamp) {
                restart()
            }
            target = timestamp
        }
        return true
    }

    /**
     * Overcraft (POTG): from Java, without Kotlin's Duration: seek to [fromMs] of the recording, play to [toMs], the camera
     * locked to entity [camera] (the scorer), then [done] runs on the server thread (or at the recording's end).
     */
    public fun playWindow(fromMs: Long, toMs: Long, camera: Int, done: Runnable) {
        this.cameraEntity = camera
        this.stopAt = toMs.milliseconds
        this.onFinished = done
        if (fromMs > 0) {
            this.jumpTo(minOf(fromMs.milliseconds, this.reader.duration))
        }
    }

    public fun jumpToMarker(name: String?, offset: Duration): Boolean {
        val markers = this.markers[name].sortedBy { it.timestamp }
        if (markers.isEmpty()) {
            return false
        }
        val marker = markers.firstOrNull { it.timestamp > this.progress } ?: markers.first()
        return this.jumpTo(marker.timestamp + offset)
    }

    public fun getMarkers(): List<ReplayMarker> {
        return this.markers.values().sortedBy { it.timestamp }
    }

    public fun setSpeed(speed: Float) {
        if (speed <= 0) {
            throw IllegalArgumentException("Cannot set non-positive speed multiplier!")
        }
        this.speedMultiplier = speed
        this.sendTickingState()
    }

    public fun setPaused(paused: Boolean): Boolean {
        if (this.paused == paused) {
            return false
        }
        this.paused = paused
        this.sendTickingState()
        this.updateProgress(this.progress)
        return true
    }

    public fun showProgress(): Boolean {
        if (!this.bossbar.isVisible) {
            this.bossbar.isVisible = true
            this.send(ClientboundBossEventPacket.createAddPacket(this.bossbar))
            return true
        }
        return false
    }

    public fun hideProgress(): Boolean {
        if (this.bossbar.isVisible) {
            this.bossbar.isVisible = false
            this.send(ClientboundBossEventPacket.createRemovePacket(this.bossbar.id))
            return true
        }
        return false
    }

    public fun resetCamera() {
        this.send(ClientboundPlayerPositionPacket(
            0, PositionMoveRotation(this.position, Vec3.ZERO, 0.0F, 0.0F), setOf()
        ))
    }

    public fun markForTeleportation() {
        this.teleported = false
    }

    private fun handleServerboundPacket(packet: Packet<*>): Boolean {
        // This is run async!
        // Overcraft: the viewer has no replay commands; everything they send is dropped (but keep-alives)
        return false
    }

    private fun restart() {
        if (!this.started) {
            return
        }
        this.removeReplayState()
        this.coroutineScope.coroutineContext.cancelChildren()
        this.teleported = false
        this.target = Duration.ZERO

        this.sendViewerPlayerInfo()
        if (this.bossbar.isVisible) {
            this.send(ClientboundBossEventPacket.createAddPacket(this.bossbar))
        }

        this.coroutineScope.launch {
            // Un-lazy the markers
            markers

            try {
                streamReplay { this.isActive }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ArcadeUtils.logger.error("Exception while viewing replay", e)
                server.execute {
                    stop()
                    player.sendSystemMessage(
                        Component.literal("Exception while viewing replay, see logs for more info").withStyle(net.minecraft.ChatFormatting.RED)
                    )
                }
            }
        }
    }

    private suspend fun streamReplay(active: Supplier<Boolean>) {
        var lastTime = Duration.ZERO
        val iterator = this.reader.readPackets().iterator()
        while (active.get() && iterator.hasNext()) {
            val element = iterator.next()

            element.use { (protocol, packet, time) ->
                if (protocol == ConnectionProtocol.PLAY && time > this.stopAt) {
                    this.finish()
                    return
                }
                if (protocol == ConnectionProtocol.PLAY && time > this.target) {
                    delay((time - lastTime) / this.speedMultiplier.toDouble())
                }

                while (this.shouldPauseStreaming()) {
                    delay(50.milliseconds)
                }

                this.playbackPacket(protocol, packet, time, active)

                if (protocol == ConnectionProtocol.PLAY) {
                    this.updateProgress(time)
                    lastTime = time
                }
            }
        }
        if (active.get()) {
            this.finish()
        }
    }

    /** Overcraft: the stream reached stopAt or the recording's end: tell the owner, once, on the server thread. */
    private fun finish() {
        if (this.finished) {
            return
        }
        this.finished = true
        val callback = this.onFinished ?: return
        this.server.execute(callback)
    }

    private fun playbackPacket(
        protocol: ConnectionProtocol,
        packet: Packet<*>,
        time: Duration,
        active: Supplier<Boolean>
    ) {
        // We don't reconfigure the client, so we just ignore config packets,
        // this should probably be reworked at some point...
        if (protocol == ConnectionProtocol.CONFIGURATION) {
            return
        }

        if (this.shouldSendPacket(packet, time)) {
            val modified = modifyPacketForViewer(packet)
            this.onSendPacket(modified)
            if (!active.get()) {
                return
            }
            this.send(modified)
            this.afterSendPacket(modified)
            this.lockCamera(modified)
        }
    }

    private fun updateProgress(progress: Duration) {
        if (abs((this.lastSentProgress - progress).inWholeMilliseconds) < 500) {
            return
        }
        this.lastSentProgress = progress

        val title = Component.empty()
            .append(Component.literal(this.reader.path.nameWithoutExtension))
            .append(" ")
            .append(Component.literal(progress.formatHHMMSS()))
        this.bossbar.name = title

        this.progress = progress
        this.bossbar.progress = progress.div(this.reader.duration).toFloat()

        if (this.bossbar.isVisible) {
            this.send(ClientboundBossEventPacket.createUpdateProgressPacket(this.bossbar))
            this.send(ClientboundBossEventPacket.createUpdateNamePacket(this.bossbar))
        }
    }

    private fun shouldPauseStreaming(): Boolean {
        return this.paused || (this.server.isSingleplayer && dev.overcraft.potg.ReplayTap.clientPaused())
    }

    private fun sendTickingState() {
        this.send(this.getTickingStatePacket())
    }

    private fun getTickingStatePacket(): ClientboundTickingStatePacket {
        return ClientboundTickingStatePacket(this.tickSpeed * this.speedMultiplier, this.paused || this.tickFrozen)
    }

    private fun setForReplayView() {
        this.removeFromServer()
        this.connection.startViewingReplay(this)

        this.removeServerState()
    }

    private fun addBackToServer() {
        val player = this.player
        val server = player.server
        val playerList = server.playerList
        val level = player.level()

        playerList.broadcastAll(
            ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(listOf(player))
        )
        playerList.players.add(player)

        RejoinedReplayPlayer.place(player, this.connection, afterLogin = {
            this.synchronizeClientLevel()
        })
        playerList.sendPlayerPermissionLevel(player)

        (player as EntityInvoker).arcade_removeRemovalReason()
        level.addNewPlayer(player)

        player.inventoryMenu.sendAllDataToRemote()
        this.connection.send(ClientboundSetHealthPacket(
            player.health,
            player.foodData.foodLevel,
            player.foodData.saturationLevel
        ))
        this.connection.send(ClientboundSetExperiencePacket(
            player.experienceProgress,
            player.totalExperience,
            player.experienceLevel
        ))
    }

    private fun removeFromServer() {
        val player = this.player
        val playerList = player.server.playerList
        playerList.broadcastAll(ClientboundPlayerInfoRemovePacket(listOf(player.uuid)))
        player.level().removePlayerImmediately(player, Entity.RemovalReason.CHANGED_DIMENSION)
        playerList.players.remove(player)
    }

    private fun removeServerState() {
        val player = this.player
        val server = player.server
        this.send(ClientboundPlayerInfoRemovePacket(server.playerList.players.map { it.uuid }))
        player.chunkTrackingView.forEach {
            this.send(ClientboundForgetLevelChunkPacket(it))
        }
        for (slot in DisplaySlot.entries) {
            this.send(ClientboundSetDisplayObjectivePacket(slot, null))
        }
        for (objective in server.scoreboard.objectives) {
            this.send(ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE))
        }
        for (bossbar in server.customBossEvents.events) {
            if (bossbar.players.contains(player)) {
                this.send(ClientboundBossEventPacket.createRemovePacket(bossbar.id))
            }
        }

    }

    private fun removeReplayState() {
        synchronized(this.players) {
            this.send(ClientboundPlayerInfoRemovePacket(ArrayList(this.players)))
        }
        synchronized(this.entities) {
            this.send(ClientboundRemoveEntitiesPacket(IntArrayList(this.entities)))
        }
        synchronized(this.chunks) {
            for (chunk in this.chunks.iterator()) {
                this.connection.send(ClientboundForgetLevelChunkPacket(ChunkPos.unpack(chunk)))
            }
        }
        synchronized(this.objectives) {
            for (objective in this.objectives) {
                val dummy = Objective(
                    Scoreboard(),
                    objective,
                    ObjectiveCriteria.DUMMY,
                    Component.empty(),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
                )
                this.send(ClientboundSetObjectivePacket(dummy, ClientboundSetObjectivePacket.METHOD_REMOVE))
            }
        }

        synchronized(this.bossBars) {
            for (id in this.bossBars) {
                this.send(ClientboundBossEventPacket.createRemovePacket(id))
            }
            this.bossBars.clear()
        }

        if (this.bossbar.isVisible) {
            this.send(ClientboundBossEventPacket.createRemovePacket(this.bossbar.id))
        }
    }

    private fun sendViewerPlayerInfo() {
        this.players.add(this.player.uuid)

        val entry = ClientboundPlayerInfoUpdatePacket.Entry(
            this.player.uuid,
            this.player.gameProfile,
            false,
            0,
            GameType.SPECTATOR,
            null,
            true,
            0,
            null
        )
        this.send(
            ReplayViewerUtils.createClientboundPlayerInfoUpdatePacket(
            EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_GAME_MODE),
            listOf(entry)
        ))
    }

    private fun shouldSendPacket(packet: Packet<*>, time: Duration): Boolean {
        return when (packet) {
            // We don't want the viewing player to be able to die
            is ClientboundSetHealthPacket -> false
            // Overcraft: the recorded chat isn't replayed (the whole fast-forward's command feedback flooded it)
            is ClientboundSystemChatPacket, is ClientboundPlayerChatPacket, is ClientboundDisguisedChatPacket -> false
            // Overcraft: only updates to boss bars the replay added (the dragon's bar was added before the recording)
            is ClientboundBossEventPacket -> this.knownBossBar(packet)
            is ClientboundBundlePacket -> true
            is ClientboundGameEventPacket -> packet.event != CHANGE_GAME_MODE
            is ClientboundPlayerPositionPacket -> {
                if (!packet.relatives.containsAll(setOf(Relative.X, Relative.Y, Relative.Z))) {
                    this.position = packet.change.position
                }
                // We want the client to teleport to the first initial position
                // later positions will teleport the viewer which we don't want
                val teleported = this.teleported
                this.teleported = true
                !teleported || time < this.target
            }
            // Overcraft: what's only heard or seen once (sounds, particles, our sounds and kill feed) is skipped while seeking
            else -> time >= this.target || !dev.overcraft.potg.ReplayTap.dropWhileSeeking(packet)
        }
    }

    /** Overcraft: an add is kept and remembered; anything else only for a bar the replay added. */
    private fun knownBossBar(packet: ClientboundBossEventPacket): Boolean {
        var keep = false
        packet.dispatch(object: ClientboundBossEventPacket.Handler {
            override fun add(id: UUID, name: Component, progress: Float, color: BossBarColor, overlay: BossBarOverlay,
                             darkenScreen: Boolean, playMusic: Boolean, createWorldFog: Boolean) {
                bossBars.add(id)
                keep = true
            }

            override fun remove(id: UUID) {
                keep = bossBars.remove(id)
            }

            override fun updateProgress(id: UUID, progress: Float) {
                keep = bossBars.contains(id)
            }

            override fun updateName(id: UUID, name: Component) {
                keep = bossBars.contains(id)
            }

            override fun updateStyle(id: UUID, color: BossBarColor, overlay: BossBarOverlay) {
                keep = bossBars.contains(id)
            }

            override fun updateProperties(id: UUID, darkenScreen: Boolean, playMusic: Boolean, createWorldFog: Boolean) {
                keep = bossBars.contains(id)
            }
        })
        return keep
    }

    /** Overcraft: the camera follows [cameraEntity] whenever the replay (re)adds it or (re)logs the viewer in. */
    private fun lockCamera(packet: Packet<*>) {
        val id = this.cameraEntity
        if (id < 0) {
            return
        }
        val adds = when (packet) {
            is ClientboundAddEntityPacket -> packet.id == id
            is ClientboundBundlePacket -> packet.subPackets().any { it is ClientboundAddEntityPacket && it.id == id }
            else -> false
        }
        if (adds) {
            this.sendCamera()
        }
    }

    public fun sendCamera() {
        val id = this.cameraEntity
        if (id < 0) {
            return
        }
        val buf = net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer())
        try {
            buf.writeVarInt(id)
            this.send(ClientboundSetCameraPacket.STREAM_CODEC.decode(buf))
        } finally {
            buf.release()
        }
    }

    private fun onSendPacket(packet: Packet<*>) {
        // We keep track of some state to revert later
        when (packet) {
            is ClientboundLevelChunkWithLightPacket -> this.chunks.add(ChunkPos.pack(packet.x, packet.z))
            is ClientboundForgetLevelChunkPacket -> this.chunks.remove(packet.pos.pack())
            is ClientboundAddEntityPacket -> this.entities.add(packet.id)
            is ClientboundRemoveEntitiesPacket -> this.entities.removeAll(packet.entityIds)
            is ClientboundSetObjectivePacket -> {
                if (packet.method == ClientboundSetObjectivePacket.METHOD_REMOVE) {
                    this.objectives.remove(packet.objectiveName)
                } else {
                    this.objectives.add(packet.objectiveName)
                }
            }
            is ClientboundPlayerInfoUpdatePacket -> {
                for (entry in packet.newEntries()) {
                    this.players.add(entry.profileId)
                }
            }
            is ClientboundPlayerInfoRemovePacket -> this.players.removeAll(packet.profileIds.toSet())
            is ClientboundRespawnPacket -> this.teleported = false
        }
    }

    private fun afterSendPacket(packet: Packet<*>) {
        when (packet) {
            is ClientboundLoginPacket -> {
                this.synchronizeClientLevel()
                this.send(ClientboundGameEventPacket(CHANGE_GAME_MODE, GameType.SPECTATOR.id.toFloat()))
            }
            is ClientboundRespawnPacket -> {
                this.send(ClientboundGameEventPacket(CHANGE_GAME_MODE, GameType.SPECTATOR.id.toFloat()))
            }
        }
    }

    private fun modifyPacketForViewer(packet: Packet<*>): Packet<*> {
        if (packet is ClientboundLoginPacket) {
            // Give the viewer a different ID to not conflict
            // with any entities in the replay
            return ClientboundLoginPacket(
                VIEWER_ID,
                packet.hardcore,
                packet.levels,
                packet.maxPlayers,
                packet.chunkRadius,
                packet.simulationDistance,
                packet.reducedDebugInfo,
                packet.showDeathScreen,
                packet.doLimitedCrafting,
                packet.commonPlayerSpawnInfo,
                packet.onlineMode,
                packet.enforcesSecureChat
            )
        }
        if (packet is ClientboundPlayerInfoUpdatePacket) {
            val copy = ArrayList(packet.entries())
            if (packet.actions().contains(Action.INITIALIZE_CHAT)) {
                val iter = copy.listIterator()
                while (iter.hasNext()) {
                    val entry = iter.next()
                    iter.set(ClientboundPlayerInfoUpdatePacket.Entry(
                        entry.profileId,
                        entry.profile,
                        entry.listed,
                        entry.latency,
                        entry.gameMode,
                        entry.displayName,
                        entry.showHat,
                        entry.listOrder,
                        null
                    ))
                }
            }

            val index = packet.entries().indexOfFirst { it.profileId == this.player.uuid }
            if (index >= 0) {
                val previous = copy[index]
                copy[index] = ClientboundPlayerInfoUpdatePacket.Entry(
                    VIEWER_UUID,
                    previous.profile,
                    previous.listed,
                    previous.latency,
                    previous.gameMode,
                    previous.displayName,
                    previous.showHat,
                    previous.listOrder,
                    null
                )
            }
            return ReplayViewerUtils.createClientboundPlayerInfoUpdatePacket(packet.actions(), copy)
        }
        if (packet is ClientboundAddEntityPacket && packet.uuid == this.player.uuid) {
            return ClientboundAddEntityPacket(
                packet.id,
                VIEWER_UUID,
                packet.x,
                packet.y,
                packet.z,
                packet.xRot,
                packet.yRot,
                packet.type,
                packet.data,
                packet.movement,
                packet.yHeadRot.toDouble()
            )
        }
        if (packet is ClientboundPlayerChatPacket) {
            // We don't want to deal with chat validation...
            val message = packet.unsignedContent.getOrElse { Component.literal(packet.body.content) }
            val decorated = packet.chatType.decorate(message)
            return ClientboundSystemChatPacket(decorated, false)
        }
        if (packet is ClientboundTickingStatePacket) {
            this.tickSpeed = packet.tickRate
            this.tickFrozen = packet.isFrozen
            return this.getTickingStatePacket()
        }

        return packet
    }

    private fun synchronizeClientLevel() {
        this.send(ClientboundRespawnPacket(
            this.player.createCommonSpawnInfo(this.player.level()),
            ClientboundRespawnPacket.KEEP_ALL_DATA
        ))
    }

    internal fun send(packet: Packet<*>) {
        this.connection.sendReplayPacket(packet)
    }

    public companion object {
        private const val VIEWER_ID = Int.MAX_VALUE - 10
        private val VIEWER_UUID: UUID = UUIDUtil.createOfflinePlayerUUID("-ViewingProfile-")

        /** Overcraft (Connection.channelRead0): what a viewer sends is dropped, but keep-alives. */
        @JvmStatic
        fun dropServerbound(listener: ServerGamePacketListenerImpl, packet: Packet<*>): Boolean {
            val viewer = listener.getViewingReplay() ?: return false
            return !viewer.handleServerboundPacket(packet) && !ReplayViewerPackets.serverboundBypass(packet)
        }
    }
}
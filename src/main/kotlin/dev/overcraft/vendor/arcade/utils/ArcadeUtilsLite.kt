/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
// Vendored into Overcraft from Arcade (arcade-utils), trimmed to what the replay engine needs: see ../LICENSE-arcade.
@file:JvmName("ArcadeUtilsLite")
package dev.overcraft.vendor.arcade.utils

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import dev.overcraft.vendor.arcade.replay.mixins.utils.ChunkMapAccessor
import dev.overcraft.vendor.arcade.replay.mixins.utils.TrackedEntityAccessor
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ChunkMap
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.EnumMap
import kotlin.time.Duration

public object ArcadeUtils {
    public val logger: Logger = LoggerFactory.getLogger("overcraft-replay")
}

public object DateTimeUtils {
    public fun Duration.formatHHMMSS(): String {
        val seconds = this.inWholeSeconds
        return "%02d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
    }
}

public object EnumUtils {
    public inline fun <reified E: Enum<E>, V> mapOf(): EnumMap<E, V> {
        return EnumMap(E::class.java)
    }
}

public fun Identifier(namespace: String, path: String): net.minecraft.resources.Identifier {
    return net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, path)
}

public fun ClientboundAddEntityPacket(entity: Entity): ClientboundAddEntityPacket {
    return ClientboundAddEntityPacket(
        entity.id, entity.uuid, entity.x, entity.y, entity.z, entity.xRot, entity.yRot,
        entity.type, 0, entity.deltaMovement, entity.yHeadRot.toDouble()
    )
}

public fun Packet<*>.getDebugName(): String {
    return if (this is ClientboundCustomPayloadPacket) {
        "CustomPayload(${this.payload.type().id})"
    } else {
        this.type().id.toString()
    }
}

/** ChunkMap.TrackedEntity is package-private: wrapped. */
@JvmInline
public value class WrappedTrackedEntity(public val tracked: ChunkMap.TrackedEntity) {
    public fun getEntity(): Entity = (this.tracked as TrackedEntityAccessor).arcade_getEntity()
    public fun getServerEntity(): ServerEntity = (this.tracked as TrackedEntityAccessor).arcade_getServerEntity()
    public fun getRange(): Int = (this.tracked as TrackedEntityAccessor).arcade_getRange()
}

public fun Entity.getServerEntity(): ServerEntity? {
    val map = (this.level() as ServerLevel).chunkSource.chunkMap as ChunkMapAccessor
    val tracked = map.arcade_getEntityMap().get(this.id) ?: return null
    return WrappedTrackedEntity(tracked).getServerEntity()
}

public fun ServerLevel.getTrackedEntities(): List<WrappedTrackedEntity> {
    val map = this.chunkSource.chunkMap as ChunkMapAccessor
    return map.arcade_getEntityMap().values.map { WrappedTrackedEntity(it) }
}

public val ServerPlayer.server: MinecraftServer
    get() = this.level().server

public val ServerLevel.server: MinecraftServer
    get() = this.getServer()

/** "PascalCase" to "snake_case". */
public fun String.pascalToSnake(): String {
    return this.replace(Regex("(?<=[a-z0-9])([A-Z])"), "_$1").lowercase()
}

public fun <A: Any> Codec<A>.encodedOptionalFieldOf(name: String, defaultValue: A): MapCodec<A> {
    return this.optionalFieldOf(name, defaultValue)
}

public object ArcadeExtraCodecs {
    public val DURATION: Codec<Duration> = Codec.STRING.comapFlatMap(
        { s -> runCatching { Duration.parse(s) }.fold({ DataResult.success(it) }, { DataResult.error { "Bad duration $s" } }) },
        Duration::toString
    )
}

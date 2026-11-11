/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package dev.overcraft.vendor.arcade.replay.io

import dev.overcraft.vendor.arcade.utils.pascalToSnake
import com.mojang.serialization.Codec
import dev.overcraft.vendor.arcade.replay.io.reader.ReplayReader
import dev.overcraft.vendor.arcade.replay.io.reader.flashback.FlashbackReader
import dev.overcraft.vendor.arcade.replay.io.writer.ReplayWriter
import dev.overcraft.vendor.arcade.replay.io.writer.flashback.FlashbackWriter
import dev.overcraft.vendor.arcade.replay.recorder.ReplayRecorder
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewer
import net.minecraft.util.StringRepresentable
import java.nio.file.Path

public enum class ReplayFormat(
    public val supported: Boolean,
    public val stable: Boolean,
): StringRepresentable {
    Flashback(true, true);

    public fun writer(recordings: Path): (ReplayRecorder) -> ReplayWriter {
        return when (this) {
            // Overcraft: the recording goes exactly where it's told (a segment's folder), not in a dated one
            Flashback -> ({ FlashbackWriter(it, recordings) })
        }
    }

    public fun reader(path: Path): (ReplayViewer) -> ReplayReader {
        return when (this) {
            Flashback -> ({ FlashbackReader(it, path)})
        }
    }

    public fun warn(consumer: (String) -> Unit) {
        if (!this.stable) {
            consumer.invoke("${this.name} support is currently unstable: ${this.name} hasn't released yet, your replays may not be compatible in the future, you have been warned!")
        }
    }

    public fun id(): String {
        return this.name.pascalToSnake()
    }

    override fun getSerializedName(): String {
        return this.id()
    }

    public companion object {
        public val CODEC: Codec<ReplayFormat> = StringRepresentable.fromEnum(ReplayFormat::values)

        public fun formatOf(path: Path): ReplayFormat? {
            return when {
                FlashbackIO.isFlashbackFile(path) -> Flashback
                else -> null
            }
        }
    }
}
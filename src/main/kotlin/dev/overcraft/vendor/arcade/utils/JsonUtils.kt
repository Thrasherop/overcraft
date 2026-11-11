/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
// Vendored into Overcraft from Arcade (arcade-utils), trimmed to what the replay engine needs: see ../LICENSE-arcade.
package dev.overcraft.vendor.arcade.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.mojang.serialization.DataResult
import com.mojang.serialization.Decoder
import com.mojang.serialization.Encoder
import com.mojang.serialization.JsonOps
import java.io.Reader
import java.nio.file.Path
import kotlin.io.path.reader
import kotlin.io.path.writer

public object JsonUtils {
    public val GSON: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create()

    public inline fun <reified T: Any> decodeRaw(element: JsonElement): T {
        return GSON.fromJson(element, object: TypeToken<T>() { })
    }

    public inline fun <reified T: Any> decodeRaw(reader: Reader): T {
        return GSON.fromJson(reader, object: TypeToken<T>() { })
    }

    public inline fun <reified T: Any> decodeRaw(path: Path): T {
        return path.reader().use { decodeRaw<T>(it) }
    }

    public fun <T: Any> decodeWith(decoder: Decoder<T>, reader: Reader): DataResult<T> {
        return decoder.parse(JsonOps.INSTANCE, decodeRaw<JsonElement>(reader))
    }

    public fun encodeRaw(any: Any?, appendable: Appendable) {
        GSON.toJson(any, appendable)
    }

    public fun encodeRaw(any: Any?, path: Path) {
        path.writer().use { this.encodeRaw(any, it) }
    }

    public fun <T: Any> encodeWith(any: T, encoder: Encoder<T>): DataResult<JsonElement> {
        return encoder.encodeStart(JsonOps.INSTANCE, any)
    }

    public fun <T: Any> encodeWith(any: T, encoder: Encoder<T>, appendable: Appendable) {
        GSON.toJson(this.encodeWith(any, encoder).getOrThrow(::IllegalArgumentException), appendable)
    }

    public fun <T: Any> encodeWith(any: T, encoder: Encoder<T>, path: Path) {
        path.writer().use { this.encodeWith(any, encoder, it) }
    }
}

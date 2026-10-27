package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to a hero's player: one of their own sounds, played in their head (no position, no falloff), as Overwatch
 * plays a hero's own sounds. Minecraft's sound packets always place a sound in the world: placed at the hero, a sound
 * was left behind when she moved and came from a direction when she turned (#17). sound is the sound event's id.
 */
public record OwnSoundPayload(String sound, float volume, float pitch, long seed) implements CustomPacketPayload {
	public static final Type<OwnSoundPayload> TYPE = new Type<>(Overcraft.id("own_sound"));
	public static final StreamCodec<FriendlyByteBuf, OwnSoundPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeUtf(p.sound, 128);
			buf.writeFloat(p.volume);
			buf.writeFloat(p.pitch);
			buf.writeLong(p.seed);
		}, buf -> new OwnSoundPayload(buf.readUtf(128), buf.readFloat(), buf.readFloat(), buf.readLong()));

	@Override
	public Type<OwnSoundPayload> type() {
		return TYPE;
	}
}

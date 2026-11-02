package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player's hero options the server acts on (the rest stay on the client). Sensitivities scale
 * the targeting cones (0.1 to 1, Overwatch's 10%-100%); ofudaCancelsReload lets Healing Ofuda interrupt a reload;
 * friendlyFire lets the player's attacks hurt their allies; hidePurrRing is Jetpack Cat's "Hide Purr Range Ring" (her
 * graph 5A7E #267 doesn't make the range wall at all when it's on, so nobody sees it: the server tells the clients).
 */
public record HeroOptionsPayload(float ofudaSensitivity, float swiftStepSensitivity, boolean ofudaCancelsReload,
								 boolean friendlyFire, boolean hidePurrRing) implements CustomPacketPayload {
	public static final HeroOptionsPayload DEFAULTS = new HeroOptionsPayload(1, 1, true, false, false);
	public static final Type<HeroOptionsPayload> TYPE = new Type<>(Overcraft.id("hero_options"));
	public static final StreamCodec<FriendlyByteBuf, HeroOptionsPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeFloat(p.ofudaSensitivity);
			buf.writeFloat(p.swiftStepSensitivity);
			buf.writeBoolean(p.ofudaCancelsReload);
			buf.writeBoolean(p.friendlyFire);
			buf.writeBoolean(p.hidePurrRing);
		}, buf -> new HeroOptionsPayload(Math.clamp(buf.readFloat(), 0.1f, 1), Math.clamp(buf.readFloat(), 0.1f, 1),
			buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

	@Override
	public Type<HeroOptionsPayload> type() {
		return TYPE;
	}
}

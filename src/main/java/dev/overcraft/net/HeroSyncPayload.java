package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client, every tick while the player has a hero: what the HUD shows. Cooldowns are seconds left;
 * reloads are 0..1 progress (-1 when not reloading). canChangeHero: near your bed (or no hero yet), H opens the
 * select screen.
 */
public record HeroSyncPayload(String heroId, float ultCharge, int ultCost, boolean ultActive,
							  int primaryAmmo, float primaryReload, int secondaryAmmo, float secondaryReload,
							  float ability1Cooldown, float ability2Cooldown, boolean rushed,
							  boolean canChangeHero) implements CustomPacketPayload {
	public static final HeroSyncPayload NONE = new HeroSyncPayload("", 0, 1, false, 0, -1, 0, -1, 0, 0, false, true);

	public static final Type<HeroSyncPayload> TYPE = new Type<>(Overcraft.id("hero_sync"));
	public static final StreamCodec<FriendlyByteBuf, HeroSyncPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeUtf(p.heroId, 32);
			buf.writeFloat(p.ultCharge);
			buf.writeVarInt(p.ultCost);
			buf.writeBoolean(p.ultActive);
			buf.writeVarInt(p.primaryAmmo);
			buf.writeFloat(p.primaryReload);
			buf.writeVarInt(p.secondaryAmmo);
			buf.writeFloat(p.secondaryReload);
			buf.writeFloat(p.ability1Cooldown);
			buf.writeFloat(p.ability2Cooldown);
			buf.writeBoolean(p.rushed);
			buf.writeBoolean(p.canChangeHero);
		}, buf -> new HeroSyncPayload(buf.readUtf(32), buf.readFloat(), buf.readVarInt(), buf.readBoolean(),
			buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
			buf.readBoolean(), buf.readBoolean()));

	@Override
	public Type<HeroSyncPayload> type() {
		return TYPE;
	}
}

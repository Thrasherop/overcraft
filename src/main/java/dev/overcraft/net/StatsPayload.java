package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client, once a second: every online player's run so far (PlayerStats: since the last dragon), for the Tab
 * scoreboard (ScoreboardHud; owner, 2026-11-21: "is it possible to add a tab UI for stats?"). Overwatch HP, its counts.
 */
public record StatsPayload(List<Row> rows) implements CustomPacketPayload {
	public static final Type<StatsPayload> TYPE = new Type<>(Overcraft.id("stats"));

	/** One player: name, hero id ("" none), whether in hero mode, eliminations, deaths, damage done, healing, mitigated. */
	public record Row(String name, String hero, boolean heroMode, int kills, int deaths, double damage, double healing,
					  double mitigated) {
	}

	public static final StreamCodec<FriendlyByteBuf, StatsPayload> CODEC = CustomPacketPayload.codec((p, buf) -> {
		buf.writeVarInt(p.rows.size());
		for (Row r : p.rows) {
			buf.writeUtf(r.name, 64);
			buf.writeUtf(r.hero, 32);
			buf.writeBoolean(r.heroMode);
			buf.writeVarInt(r.kills);
			buf.writeVarInt(r.deaths);
			buf.writeDouble(r.damage);
			buf.writeDouble(r.healing);
			buf.writeDouble(r.mitigated);
		}
	}, buf -> {
		int n = Math.min(buf.readVarInt(), 256);
		List<Row> rows = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			rows.add(new Row(buf.readUtf(64), buf.readUtf(32), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readDouble(),
				buf.readDouble(), buf.readDouble()));
		}
		return new StatsPayload(rows);
	});

	@Override
	public Type<StatsPayload> type() {
		return TYPE;
	}
}

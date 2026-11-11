package dev.overcraft.client.dev;

import dev.overcraft.Overcraft;
import dev.overcraft.potg.PotgTap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GameProtocols;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Play of the Game, phase 1 (techniques/potg/design.md): the client's half of the round trip. Each recorded packet is
 * decoded with the client's own bound game protocol, checked to use every byte, and encoded again to compare. Nothing
 * is handled: no packet reaches a listener, and only bytes and types are logged (and written to run/potg/).
 */
public final class PotgDecode {
	private PotgDecode() {
	}

	/** Hooks the server's dump to this side (single player: one JVM, so no reel is sent yet). */
	public static void init() {
		if (!Boolean.getBoolean("overcraft.dev")) {
			return;
		}
		PotgTap.clientDecoder = dump -> Minecraft.getInstance().execute(() -> decode(dump));
	}

	private static final class Row {
		long count;
		long bytes;
		long ok;
		long leftover;
		long reencodeDiffers;
		long errors;
		String firstError;
		String decodedAs;
	}

	private static void decode(PotgTap.Dump dump) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			return;
		}
		long t0 = System.nanoTime();
		ProtocolInfo<ClientGamePacketListener> protocol =
			GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(connection.registryAccess()));
		Map<String, Row> rows = new TreeMap<>();
		long[] kinds = new long[3];
		long[] kindBytes = new long[3];
		int[] bad = new int[1];
		for (byte[] seg : dump.keyframes()) {
			records(seg, protocol, rows, kinds, kindBytes, bad, "kf ");
		}
		for (byte[] seg : dump.streams()) {
			records(seg, protocol, rows, kinds, kindBytes, bad, "");
		}
		for (byte[] probe : dump.probes()) {
			one(probe, protocol, rows, "probe ");
		}
		double ms = (System.nanoTime() - t0) / 1e6;
		StringBuilder s = new StringBuilder();
		s.append(dump.serverReport());
		s.append(String.format(Locale.ROOT, "[POTG] client decode of %s: %.1f ms; records: packets %d (%d B), own track %d (%d B), own packets %d (%d B); framing errors %d%n",
			dump.player(), ms, kinds[0], kindBytes[0], kinds[1], kindBytes[1], kinds[2], kindBytes[2], bad[0]));
		s.append("[POTG] decoded (count, bytes, ok, leftover bytes, re-encode differs, errors, decoded as):\n");
		for (Map.Entry<String, Row> e : rows.entrySet()) {
			Row r = e.getValue();
			s.append(String.format(Locale.ROOT, "[POTG]   %-54s %7d %9d %7d %4d %4d %4d %s%s%n", e.getKey(), r.count, r.bytes, r.ok,
				r.leftover, r.reencodeDiffers, r.errors, r.decodedAs == null ? "" : r.decodedAs,
				r.firstError == null ? "" : " ERR " + r.firstError));
		}
		for (String line : s.toString().split("\n")) {
			Overcraft.LOG.info(line);
		}
		try {
			Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("potg");
			Files.createDirectories(dir);
			Files.writeString(dir.resolve("dump-" + System.currentTimeMillis() + ".txt"), s.toString());
		} catch (Exception e) {
			Overcraft.LOG.warn("[POTG] couldn't write the dump: {}", e.toString());
		}
	}

	private static void records(byte[] seg, ProtocolInfo<ClientGamePacketListener> protocol, Map<String, Row> rows, long[] kinds,
								long[] kindBytes, int[] bad, String prefix) {
		ByteBuf buf = Unpooled.wrappedBuffer(seg);
		while (buf.isReadable()) {
			try {
				VarInt.read(buf); // tick offset
				int kind = buf.readByte();
				int len = VarInt.read(buf);
				byte[] bytes = new byte[len];
				buf.readBytes(bytes);
				kinds[kind]++;
				kindBytes[kind] += len;
				if (kind == PotgTap.KIND_TRACK) {
					continue;
				}
				one(bytes, protocol, rows, kind == PotgTap.KIND_OWN ? "own " : prefix);
			} catch (Throwable t) {
				bad[0]++;
				Overcraft.LOG.warn("[POTG] framing broke: {}", t.toString());
				return;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void one(byte[] bytes, ProtocolInfo<ClientGamePacketListener> protocol, Map<String, Row> rows, String prefix) {
		ByteBuf in = Unpooled.wrappedBuffer(bytes);
		Packet<? super ClientGamePacketListener> packet;
		try {
			packet = protocol.codec().decode(in);
		} catch (Throwable t) {
			int id = VarInt.read(Unpooled.wrappedBuffer(bytes));
			Row r = rows.computeIfAbsent(prefix + "id " + id + " (undecodable)", k -> new Row());
			r.count++;
			r.bytes += bytes.length;
			r.errors++;
			if (r.firstError == null) {
				r.firstError = t.toString();
			}
			return;
		}
		Row r = rows.computeIfAbsent(prefix + PotgTap.typeName(packet), k -> new Row());
		r.count++;
		r.bytes += bytes.length;
		if (in.isReadable()) {
			r.leftover++;
		} else {
			r.ok++;
		}
		if (r.decodedAs == null) {
			r.decodedAs = packet instanceof ClientboundCustomPayloadPacket custom
				? (custom.payload() instanceof DiscardedPayload ? "DISCARDED (not decoded)" : custom.payload().getClass().getSimpleName())
				: packet.getClass().getSimpleName();
		}
		try {
			ByteBuf out = Unpooled.buffer(bytes.length);
			protocol.codec().encode(out, packet);
			byte[] again = new byte[out.readableBytes()];
			out.readBytes(again);
			if (!Arrays.equals(again, bytes)) {
				r.reencodeDiffers++;
			}
		} catch (Throwable t) {
			r.errors++;
			if (r.firstError == null) {
				r.firstError = "re-encode: " + t;
			}
		}
	}
}

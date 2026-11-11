package dev.overcraft.potg;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.mixin.ChunkMapAccessor;
import dev.overcraft.mixin.TrackedEntityAccessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Play of the Game, phase 1 (techniques/potg/design.md, "The plan" step 1): a dev-only tap on what the server sends each
 * hero, encoded at once with the game protocol's own codec into a rolling ring of 5 s segments, each with a keyframe,
 * plus the hero's own track. No ghosts, no camera: {@link #dump} hands a copy to the client, which decodes it all again
 * and logs only bytes and packet types. Armed by {@code /overcraft potg start} (dev builds only); costs nothing otherwise.
 */
public final class PotgTap {
	private PotgTap() {
	}

	/** Segment length and how many whole segments are kept (design: 5 s segments, the last 25 s). */
	public static final int SEGMENT_TICKS = 100;
	public static final int SEGMENTS_KEPT = 5;
	/** Samples of each type not kept, encoded once for the decode check only (ours). */
	private static final int PROBES_PER_TYPE = 3;

	/** Record kinds in a segment: a packet the hero was sent, the hero's own track, a packet about the hero we made. */
	public static final int KIND_PACKET = 0;
	public static final int KIND_TRACK = 1;
	public static final int KIND_OWN = 2;

	/** The packet types kept in the stream: the design's lists ("What gets recorded"). */
	private static final java.util.Set<String> KEPT = java.util.Set.of(
		// entities
		"add_entity", "remove_entities", "move_entity_pos", "move_entity_pos_rot", "move_entity_rot", "entity_position_sync",
		"teleport_entity", "rotate_head", "set_entity_motion", "set_entity_data", "set_equipment", "set_passengers",
		"set_entity_link", "update_attributes", "animate", "swing_animation", "entity_event", "damage_event", "hurt_animation",
		"update_mob_effect", "remove_mob_effect", "move_minecart_along_track", "projectile_power", "take_item_entity",
		// world, recorded but not applied at first
		"block_update", "section_blocks_update", "block_event", "block_destruction", "block_entity_data",
		// world, applied
		"level_event", "level_particles", "explode", "sound", "sound_entity");
	/** Our payloads and Fabric's attachment sync that are kept; the rest of the custom payloads are only probed. */
	private static final java.util.Set<String> KEPT_PAYLOADS = java.util.Set.of(
		"overcraft:effect", "overcraft:hero_action", "overcraft:own_sound", "overcraft:voice_line", "overcraft:hero_sync",
		"fabric:attachment_sync_v1");

	private static boolean armed;
	private static long armedAt;
	private static @Nullable ProtocolInfo<ClientGamePacketListener> protocol;
	private static final Map<UUID, Recorder> RECORDERS = new HashMap<>();
	/** While a keyframe is being made for a player, what's sent to them goes here instead, and not to them. */
	private static final ThreadLocal<Capture> CAPTURING = new ThreadLocal<>();
	/** Set by the client in single player: takes a dump and decodes it on the client thread. */
	public static volatile @Nullable Consumer<Dump> clientDecoder;

	private record Capture(ServerPlayer player, Consumer<Packet<?>> sink) {
	}

	/** One 5 s segment: its keyframe and its stream, as records [tick-offset, kind, length, bytes]. */
	public static final class Segment {
		public final int startTick;
		public final ByteBuf keyframe = Unpooled.buffer(4096);
		public final ByteBuf stream = Unpooled.buffer(16384);
		public int keyframePackets;
		public int keyframeEntities;
		public double keyframeMs;

		Segment(int startTick) {
			this.startTick = startTick;
		}

		public int bytes() {
			return keyframe.writerIndex() + stream.writerIndex();
		}
	}

	/** What's counted per packet type: [count, bytes, encode errors]. */
	public static final class TypeStats {
		public long count;
		public long bytes;
		public long errors;
		public boolean kept;
		public @Nullable String firstError;
	}

	private static final class Recorder {
		final UUID id;
		final String name;
		int playerId;
		final ArrayDeque<Segment> segments = new ArrayDeque<>();
		@Nullable Segment current;
		final Map<String, TypeStats> types = new TreeMap<>();
		final List<byte[]> probes = new ArrayList<>();
		final Map<String, Integer> probed = new HashMap<>();
		/** Nanoseconds spent this tick, and per tick over the run (last 12000 ticks). */
		long tickNanos;
		final long[] perTick = new long[12000];
		int ticks;
		long streamBytes;
		long trackBytes;
		long keyframeBytes;
		int keyframes;
		double keyframeMsMax;
		double keyframeMsSum;
		long keyframeEntitiesSum;
		int keyframeEntitiesMax;
		Map<String, Integer> lastKeyframeTypes = new TreeMap<>();
		int ticksOutOfPlay;
		int equipmentHash;
		final ByteBuf scratch = Unpooled.buffer(1024);

		Recorder(ServerPlayer player) {
			this.id = player.getUUID();
			this.name = player.getName().getString();
			this.playerId = player.getId();
		}
	}

	public static boolean armed() {
		return armed;
	}

	/**
	 * The leak check: what the server let go to players since arming, and what clients received (Connection.channelRead0,
	 * PotgSendMixin), by type, bundles split. A packet a keyframe captured and still reached a client shows as a surplus.
	 */
	private static final Map<String, long[]> SENT = new java.util.concurrent.ConcurrentHashMap<>();
	private static final Map<String, long[]> RECEIVED = new java.util.concurrent.ConcurrentHashMap<>();

	public static void onReceived(Packet<?> packet) {
		if (armed) {
			count(RECEIVED, packet);
		}
	}

	private static void count(Map<String, long[]> into, Packet<?> packet) {
		if (packet instanceof ClientboundBundlePacket bundle) {
			for (Packet<?> sub : bundle.subPackets()) {
				count(into, sub);
			}
			return;
		}
		into.computeIfAbsent(typeName(packet), k -> new long[1])[0]++;
	}

	/** The types whose counts differ, sent against received (a few in flight at either end are expected). */
	public static String leakReport() {
		StringBuilder s = new StringBuilder();
		java.util.Set<String> all = new java.util.TreeSet<>(SENT.keySet());
		all.addAll(RECEIVED.keySet());
		int same = 0;
		for (String type : all) {
			long sent = SENT.getOrDefault(type, new long[1])[0];
			long got = RECEIVED.getOrDefault(type, new long[1])[0];
			if (sent != got) {
				s.append(' ').append(type).append(" sent ").append(sent).append(" got ").append(got).append(';');
			} else {
				same++;
			}
		}
		return same + " types equal;" + (s.isEmpty() ? " none differ" : s.toString());
	}

	/** Starts recording every player now in hero play, and anyone who enters it, until {@link #stop}. */
	public static String start(MinecraftServer server) {
		protocol = GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(server.registryAccess()));
		RECORDERS.clear();
		SENT.clear();
		RECEIVED.clear();
		armed = true;
		armedAt = System.nanoTime();
		PlayScore.open();
		PotgReplay.armed();
		Overcraft.LOG.info("[POTG] armed at tick {}", server.getTickCount());
		return "POTG tap armed";
	}

	public static String stop() {
		armed = false;
		autoArmed = false;
		PlayScore.clear();
		int n = RECORDERS.size();
		RECORDERS.clear();
		Overcraft.LOG.info("[POTG] disarmed, {} recorders dropped", n);
		return "POTG tap off (" + n + " recorders dropped)";
	}

	private static boolean recording(ServerPlayer player) {
		return HeroManager.hero(player) != null && HeroManager.heroMode(player);
	}

	/**
	 * The send hook (PotgSendMixin): true if the packet must not go out (it was made for a keyframe). Otherwise records
	 * it for that player when armed, and lets it go unchanged.
	 */
	public static boolean onSend(ServerPlayer player, Packet<?> packet) {
		Capture capture = CAPTURING.get();
		if (capture != null && capture.player == player) {
			capture.sink.accept(packet);
			return true;
		}
		if (PotgReplay.viewing(player)
			&& (packet instanceof net.minecraft.network.protocol.game.ClientboundTickingStatePacket
			|| packet instanceof net.minecraft.network.protocol.game.ClientboundSetTimePacket)) {
			// the world is frozen for the replay, and the viewer's client mustn't hear it (design: "Freezing, with the news
			// kept from the viewer"), or its clock and animators stop
			return true;
		}
		if (!armed) {
			return false;
		}
		count(SENT, packet);
		Recorder r = RECORDERS.get(player.getUUID());
		if (r == null || r.current == null || PotgReplay.viewing(player)) {
			return false;
		}
		synchronized (r) {
			long t0 = System.nanoTime();
			int tick = player.level().getServer().getTickCount();
			record(r, packet, tick, KIND_PACKET, r.current.stream, true);
			r.tickNanos += System.nanoTime() - t0;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static void record(Recorder r, Packet<?> packet, int tick, int kind, ByteBuf into, boolean stream) {
		if (packet instanceof ClientboundBundlePacket bundle) {
			for (Packet<? super ClientGamePacketListener> sub : bundle.subPackets()) {
				record(r, sub, tick, kind, into, stream);
			}
			return;
		}
		String type = typeName(packet);
		TypeStats stats = r.types.computeIfAbsent(type, k -> new TypeStats());
		stats.count++;
		boolean keep = kept(packet, type);
		stats.kept = keep;
		if (!keep) {
			// not kept: count it, and encode the first few once so the client can say whether it decodes
			int seen = r.probed.merge(type, 1, Integer::sum);
			if (seen <= PROBES_PER_TYPE) {
				long t0 = System.nanoTime();
				byte[] bytes = encode(r, (Packet<? super ClientGamePacketListener>) packet, stats);
				if (bytes != null) {
					r.probes.add(bytes);
				}
				r.tickNanos -= System.nanoTime() - t0; // probes aren't the tap's cost
			}
			return;
		}
		byte[] bytes = encode(r, (Packet<? super ClientGamePacketListener>) packet, stats);
		if (bytes == null) {
			return;
		}
		stats.bytes += bytes.length;
		Segment seg = r.current;
		writeRecord(into, tick - (seg == null ? tick : seg.startTick), kind, bytes);
		if (stream) {
			r.streamBytes += bytes.length;
		}
	}

	private static void writeRecord(ByteBuf into, int dt, int kind, byte[] bytes) {
		VarInt.write(into, dt);
		into.writeByte(kind);
		VarInt.write(into, bytes.length);
		into.writeBytes(bytes);
	}

	private static byte @Nullable [] encode(Recorder r, Packet<? super ClientGamePacketListener> packet, TypeStats stats) {
		ByteBuf buf = r.scratch;
		buf.clear();
		try {
			protocol.codec().encode(buf, packet);
			byte[] bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes);
			return bytes;
		} catch (Throwable t) {
			stats.errors++;
			if (stats.firstError == null) {
				stats.firstError = t.toString();
				Overcraft.LOG.warn("[POTG] encode failed for {}: {}", typeName(packet), t.toString());
			}
			return null;
		}
	}

	public static String typeName(Packet<?> packet) {
		String id = packet.type().id().getPath();
		if (packet instanceof ClientboundCustomPayloadPacket custom) {
			return "custom_payload/" + custom.payload().type().id();
		}
		return id;
	}

	private static boolean kept(Packet<?> packet, String type) {
		if (packet instanceof ClientboundCustomPayloadPacket custom) {
			return KEPT_PAYLOADS.contains(custom.payload().type().id().toString());
		}
		return KEPT.contains(type);
	}

	/** Armed by a dragon fight (not the dev command), so disarmed when it's over. */
	private static boolean autoArmed;
	private static int lastFightTick;
	/**
	 * A dragon out of sight isn't a fight over: its chunks unload when the End's last player dies or leaves, and it comes
	 * back with them, so the plays wait for it this long (ours: 5 min), or until its POTG has been shown.
	 */
	private static final int NO_DRAGON_TICKS = 6000;

	/**
	 * Armed only while a dragon is in a level with players in it (design: "Only while armed: the first version records
	 * only heroes in the End with a dragon alive"), from its first sight to its removal after the POTG; elsewhere nothing
	 * is recorded or scored. Checked once a second.
	 */
	private static void autoArm(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		boolean fight = false;
		for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
			// a dead hero isn't in level.players() but still watches: the dragon alone decides
			if (!level.getEntities(net.minecraft.world.entity.EntityTypes.ENDER_DRAGON, d -> !d.isRemoved()).isEmpty()) {
				fight = true;
				break;
			}
		}
		if (fight) {
			lastFightTick = server.getTickCount();
		}
		if (fight && !armed) {
			start(server);
			autoArmed = true;
			Overcraft.LOG.info("[POTG] a dragon fight: recording and scoring");
		} else if (!fight && armed && autoArmed && !PotgReplay.active()
			&& (PotgReplay.shownSinceArmed() || server.getTickCount() - lastFightTick > NO_DRAGON_TICKS)) {
			stop();
			Overcraft.LOG.info("[POTG] the dragon fight is over: nothing recorded any more");
		}
	}

	/** END_SERVER_TICK: the own track, the segment roll and its keyframe, and the tick's cost. */
	public static void tick(MinecraftServer server) {
		autoArm(server);
		if (!armed) {
			return;
		}
		int tick = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Recorder r = RECORDERS.get(player.getUUID());
			if (r == null) {
				r = new Recorder(player);
				RECORDERS.put(player.getUUID(), r);
			}
			synchronized (r) {
				long t0 = System.nanoTime();
				if (PotgReplay.viewing(player)) {
					// nothing is recorded while they watch: the world's still
				} else if (!recording(player)) {
					// Minecraft mode doesn't count (owner, 2026-11-11): nothing recorded, the ring keeps what it had
					r.ticksOutOfPlay++;
					if (r.current != null) {
						finishSegment(r);
						r.current = null;
					}
				} else {
					if (r.current == null || tick - r.current.startTick >= SEGMENT_TICKS) {
						if (r.current != null) {
							finishSegment(r);
						}
						r.current = new Segment(tick);
						keyframe(r, player, r.current);
					}
					ownTrack(r, player, tick);
				}
				r.tickNanos += System.nanoTime() - t0;
				if (r.current != null) {
					r.perTick[r.ticks % r.perTick.length] = r.tickNanos;
					r.ticks++;
				}
				r.tickNanos = 0;
			}
		}
	}

	private static void finishSegment(Recorder r) {
		r.segments.addLast(r.current);
		while (r.segments.size() > SEGMENTS_KEPT) {
			Segment old = r.segments.removeFirst();
			old.keyframe.release();
			old.stream.release();
		}
	}

	/**
	 * A keyframe: for every entity the hero tracks and the hero's own, what a newcomer would be sent (pairing data, then
	 * Fabric's START_TRACKING listeners: the attachments and HeroManager.showState), captured, not sent; and the hero's own
	 * effects. Every packet made goes through the same encoder.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void keyframe(Recorder r, ServerPlayer player, Segment seg) {
		long t0 = System.nanoTime();
		ServerLevel level = player.level();
		var map = ((ChunkMapAccessor) level.getChunkSource().chunkMap).overcraft$entityMap();
		List<Packet<?>> packets = new ArrayList<>();
		Consumer<Packet<?>> sink = packets::add;
		int entities = 0;
		Map<String, Integer> kinds = new TreeMap<>();
		CAPTURING.set(new Capture(player, sink));
		try {
			for (Object o : map.values()) {
				TrackedEntityAccessor tracked = (TrackedEntityAccessor) o;
				ServerEntity serverEntity = tracked.overcraft$serverEntity();
				var entity = tracked.overcraft$entity();
				boolean own = entity == player;
				if (!own && !tracked.overcraft$seenBy().contains(player.connection)) {
					continue;
				}
				entities++;
				kinds.merge(net.minecraft.world.entity.EntityType.getKey(entity.getType()).getPath(), 1, Integer::sum);
				serverEntity.sendPairingData(player, (Consumer) sink);
				net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.invoker().onStartTracking(entity, player);
			}
		} finally {
			CAPTURING.remove();
		}
		for (MobEffectInstance effect : player.getActiveEffects()) {
			packets.add(new ClientboundUpdateMobEffectPacket(player.getId(), effect, false));
		}
		for (Packet<?> p : packets) {
			record(r, p, seg.startTick, KIND_PACKET, seg.keyframe, false);
		}
		seg.keyframePackets = packets.size();
		seg.keyframeEntities = entities;
		seg.keyframeMs = (System.nanoTime() - t0) / 1e6;
		r.keyframes++;
		r.keyframeEntitiesSum += entities;
		r.keyframeEntitiesMax = Math.max(r.keyframeEntitiesMax, entities);
		r.lastKeyframeTypes = kinds;
		r.keyframeBytes += seg.keyframe.writerIndex();
		r.keyframeMsSum += seg.keyframeMs;
		r.keyframeMsMax = Math.max(r.keyframeMsMax, seg.keyframeMs);
	}

	/** Each tick: what the server never tells a player about themselves. Equipment as a packet when it changes. */
	private static void ownTrack(Recorder r, ServerPlayer player, int tick) {
		Segment seg = r.current;
		ByteBuf buf = r.scratch;
		buf.clear();
		buf.writeDouble(player.getX());
		buf.writeDouble(player.getY());
		buf.writeDouble(player.getZ());
		buf.writeFloat(player.getYRot());
		buf.writeFloat(player.getXRot());
		buf.writeByte(player.getPose().ordinal());
		buf.writeByte((player.onGround() ? 1 : 0) | (player.isAlive() ? 2 : 0));
		byte[] bytes = new byte[buf.readableBytes()];
		buf.readBytes(bytes);
		writeRecord(seg.stream, tick - seg.startTick, KIND_TRACK, bytes);
		r.trackBytes += bytes.length;
		int hash = 1;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = player.getItemBySlot(slot);
			hash = hash * 31 + ItemStack.hashItemAndComponents(stack) * 7 + stack.getCount();
		}
		if (hash != r.equipmentHash) {
			r.equipmentHash = hash;
			List<com.mojang.datafixers.util.Pair<EquipmentSlot, ItemStack>> slots = new ArrayList<>();
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				slots.add(com.mojang.datafixers.util.Pair.of(slot, player.getItemBySlot(slot).copy()));
			}
			TypeStats stats = r.types.computeIfAbsent("own/set_equipment", k -> new TypeStats());
			stats.count++;
			stats.kept = true;
			byte[] eq = encode(r, new ClientboundSetEquipmentPacket(player.getId(), slots), stats);
			if (eq != null) {
				stats.bytes += eq.length;
				writeRecord(seg.stream, tick - seg.startTick, KIND_OWN, eq);
				r.trackBytes += eq.length;
			}
		}
	}

	/** A copy of one hero's ring for the client to decode, with the server's numbers. */
	public record Dump(String player, List<byte[]> keyframes, List<byte[]> streams, List<byte[]> probes, String serverReport,
					   List<Integer> starts, int nowTick, int heroId) {
	}

	/** Reports the numbers, and hands each hero's ring to the client's decoder (single player). */
	public static String dump(MinecraftServer server) {
		if (!armed) {
			return "POTG tap isn't armed";
		}
		StringBuilder chat = new StringBuilder();
		double seconds = (System.nanoTime() - armedAt) / 1e9;
		for (Recorder r : RECORDERS.values()) {
			Dump d = build(r, server, chat, seconds);
			for (String line : d.serverReport().split("\n")) {
				Overcraft.LOG.info(line);
			}
			Consumer<Dump> decoder = clientDecoder;
			if (decoder != null) {
				decoder.accept(d);
			}
		}
		return chat.isEmpty() ? "POTG: nothing recorded yet" : chat.toString();
	}

	private static Dump build(Recorder r, MinecraftServer server, StringBuilder chat, double seconds) {
		Dump d;
		synchronized (r) {
			StringBuilder s = new StringBuilder();
			int n = Math.min(r.ticks, r.perTick.length);
			long[] sorted = java.util.Arrays.copyOf(r.perTick, n);
			java.util.Arrays.sort(sorted);
			long sum = 0;
			for (long v : sorted) {
				sum += v;
			}
			double mean = n == 0 ? 0 : sum / 1e6 / n;
			double p50 = n == 0 ? 0 : sorted[n / 2] / 1e6;
			double p99 = n == 0 ? 0 : sorted[Math.min(n - 1, (int) (n * 0.99))] / 1e6;
			double max = n == 0 ? 0 : sorted[n - 1] / 1e6;
			int ringBytes = 0;
			List<byte[]> keyframes = new ArrayList<>();
			List<byte[]> streams = new ArrayList<>();
			List<Integer> starts = new ArrayList<>();
			List<Segment> all = new ArrayList<>(r.segments);
			if (r.current != null) {
				all.add(r.current);
			}
			int maxKeyframe = 0;
			for (Segment seg : all) {
				ringBytes += seg.bytes();
				maxKeyframe = Math.max(maxKeyframe, seg.keyframe.writerIndex());
				keyframes.add(copy(seg.keyframe));
				starts.add(seg.startTick);
				streams.add(copy(seg.stream));
			}
			int recordedTicks = r.ticks;
			double recSeconds = recordedTicks / 20.0;
			s.append(String.format(java.util.Locale.ROOT,
				"[POTG] %s: %.1f s armed, %d ticks recorded in hero play (%d out of it), server tick avg %.2f ms%n",
				r.name, seconds, recordedTicks, r.ticksOutOfPlay, server.getAverageTickTimeNanos() / 1e6));
			s.append(String.format(java.util.Locale.ROOT,
				"[POTG] tap ms/tick: mean %.3f p50 %.3f p99 %.3f max %.3f (keyframe each 5 s: mean %.2f max %.2f ms)%n",
				mean, p50, p99, max, r.keyframes == 0 ? 0 : r.keyframeMsSum / r.keyframes, r.keyframeMsMax));
			long recordedBytes = r.streamBytes + r.trackBytes;
			s.append(String.format(java.util.Locale.ROOT,
				"[POTG] bytes/s: stream %.0f + own track %.0f = %.0f; keyframes %d, avg %.0f B, max in ring %d B; ring now %d B in %d segments%n",
				r.streamBytes / Math.max(recSeconds, 1e-9), r.trackBytes / Math.max(recSeconds, 1e-9),
				recordedBytes / Math.max(recSeconds, 1e-9), r.keyframes,
				r.keyframes == 0 ? 0 : (double) r.keyframeBytes / r.keyframes, maxKeyframe, ringBytes, all.size()));
			s.append(String.format(java.util.Locale.ROOT, "[POTG] keyframe entities: avg %.1f, max %d; last: %s%n",
				r.keyframes == 0 ? 0 : (double) r.keyframeEntitiesSum / r.keyframes, r.keyframeEntitiesMax, r.lastKeyframeTypes));
			s.append("[POTG] types seen (count, kept bytes, encode errors):\n");
			for (Map.Entry<String, TypeStats> e : r.types.entrySet()) {
				TypeStats t = e.getValue();
				s.append(String.format(java.util.Locale.ROOT, "[POTG]   %-48s %s %8d %10d %d%s%n", e.getKey(),
					t.kept ? "KEPT " : "probe", t.count, t.bytes, t.errors, t.firstError == null ? "" : " " + t.firstError));
			}
			s.append("[POTG] leak check, sent against received: ").append(leakReport()).append('\n');
			d = new Dump(r.name, keyframes, streams, new ArrayList<>(r.probes), s.toString(), starts, server.getTickCount(), r.playerId);
			chat.append(String.format(java.util.Locale.ROOT, "%s: %.3f ms/tick mean, %.0f B/s, ring %d KB. ",
				r.name, mean, recordedBytes / Math.max(recSeconds, 1e-9), ringBytes / 1024));
		}
		return d;
	}

	/** The ring of one hero, for the replay (PotgReplay), or null if there's none. */
	public static @Nullable Dump snapshot(ServerPlayer player) {
		Recorder r = RECORDERS.get(player.getUUID());
		if (r == null || (r.segments.isEmpty() && r.current == null)) {
			return null;
		}
		return build(r, player.level().getServer(), new StringBuilder(), (System.nanoTime() - armedAt) / 1e9);
	}

	private static byte[] copy(ByteBuf buf) {
		byte[] bytes = new byte[buf.writerIndex()];
		buf.getBytes(0, bytes);
		return bytes;
	}
}

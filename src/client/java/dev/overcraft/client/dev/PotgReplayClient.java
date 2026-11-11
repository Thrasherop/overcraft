package dev.overcraft.client.dev;

import dev.overcraft.Overcraft;
import dev.overcraft.potg.PotgReplay;
import dev.overcraft.potg.PotgTap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Play of the Game, step 2 (techniques/potg/design.md, "How the world is shown"): the client's replay. The recorded
 * packets go through Minecraft's own handlers on the real ClientPacketListener, onto ghosts: entities of the loaded level
 * kept in a map of ours, not in its storage. While it plays, the level's entity lookups answer with the ghosts in two
 * places only, the frame's drawing and the replay's own step (PotgClientLevelMixin); the rest of the client tick
 * (packets, the real player, the mod's client ticks) still sees the live world. The view is placed behind the hero's ghost
 * (CameraMixin), the camera entity staying the local player, which doesn't tick while it plays.
 */
public final class PotgReplayClient {
	private PotgReplayClient() {
	}

	private record Rec(int tick, int kind, byte[] bytes) {
	}

	private static boolean active;
	/** In the replay's step (packets onto ghosts, the ghosts' ticks), or drawing a frame. */
	private static boolean stepping;
	private static boolean rendering;
	/** Run-in: packets applied with no sound, particles or our effects (design: "Run-in"). */
	private static boolean quiet;
	private static final Int2ObjectLinkedOpenHashMap<Entity> GHOSTS = new Int2ObjectLinkedOpenHashMap<>();
	private static final List<Rec> RECORDS = new ArrayList<>();
	private static int cursor;
	private static int index;
	private static int endTick;
	private static int heroId;
	private static @Nullable ProtocolInfo<ClientGamePacketListener> protocol;
	private static @Nullable Map<Integer, dev.overcraft.client.model.HeroAnimator> liveAnimators;
	private static boolean hideGuiWas;
	private static boolean heroAlive = true;
	private static final Map<String, int[]> ERRORS = new HashMap<>();
	// numbers for the record
	private static long stepNanos;
	private static long stepMax;
	private static int steps;
	private static double runInMs;
	private static int runInTicks;
	private static int maxGhosts;
	private static int applied;
	private static int frames;
	private static long frameNanosMax;
	private static long frameNanosSum;
	private static long lastFrame;
	/** The chase view's last placing, held when the hero's ghost is gone. */
	public static double camX, camY, camZ;
	public static float camYaw, camPitch;

	public static void init() {
		if (!Boolean.getBoolean("overcraft.dev")) {
			return;
		}
		PotgReplay.clientPlayer = dump -> Minecraft.getInstance().execute(() -> start(dump));
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> step(mc));
	}

	public static boolean active() {
		return active;
	}

	/** Whether the level's lookups answer with ghosts now (PotgClientLevelMixin). */
	public static boolean ghostView() {
		return active && (stepping || rendering);
	}

	public static void rendering(boolean on) {
		rendering = on;
		if (active && on) {
			long now = System.nanoTime();
			if (lastFrame != 0) {
				long d = now - lastFrame;
				frameNanosSum += d;
				frameNanosMax = Math.max(frameNanosMax, d);
				frames++;
			}
			lastFrame = now;
		}
	}

	public static boolean quiet() {
		return active && quiet;
	}

	public static LevelEntityGetter<Entity> ghosts() {
		return GETTER;
	}

	/** ClientLevel.addEntity in the ghost view. */
	public static void addGhost(Entity entity) {
		Entity old = GHOSTS.remove(entity.getId());
		if (old != null) {
			old.setRemoved(Entity.RemovalReason.DISCARDED);
		}
		GHOSTS.put(entity.getId(), entity);
		maxGhosts = Math.max(maxGhosts, GHOSTS.size());
	}

	/** ClientLevel.removeEntity in the ghost view. */
	public static void removeGhost(int id, Entity.RemovalReason reason) {
		Entity e = GHOSTS.remove(id);
		if (e != null) {
			e.setRemoved(reason);
			e.onClientRemoval();
		}
	}

	private static void start(PotgTap.Dump dump) {
		Minecraft mc = Minecraft.getInstance();
		ClientPacketListener connection = mc.getConnection();
		if (connection == null || mc.level == null || active) {
			PotgReplay.clientDone();
			return;
		}
		long t0 = System.nanoTime();
		protocol = GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(connection.registryAccess()));
		heroId = dump.heroId();
		endTick = dump.nowTick();
		int windowStart = endTick - PotgReplay.WINDOW_TICKS;
		// the segment whose keyframe comes last before the window opens; the oldest if the ring is shorter
		int seg = 0;
		for (int i = 0; i < dump.starts().size(); i++) {
			if (dump.starts().get(i) <= windowStart) {
				seg = i;
			}
		}
		RECORDS.clear();
		int segStart = dump.starts().get(seg);
		read(dump.keyframes().get(seg), segStart, RECORDS);
		int keyframeRecords = RECORDS.size();
		for (int i = seg; i < dump.streams().size(); i++) {
			read(dump.streams().get(i), dump.starts().get(i), RECORDS);
		}
		// later keyframes aren't applied: their add_entity would make every ghost again (design: "Keyframes sit beside")
		GHOSTS.clear();
		ERRORS.clear();
		steps = 0;
		stepNanos = stepMax = 0;
		maxGhosts = 0;
		applied = 0;
		frames = 0;
		frameNanosMax = frameNanosSum = 0;
		lastFrame = 0;
		heroAlive = true;
		active = true;
		liveAnimators = dev.overcraft.client.model.HeroAnimator.swapAll(new HashMap<>());
		dev.overcraft.client.effect.Fx.clear();
		// the HUD hides, as F1 does (the hand with it)
		hideGuiWas = mc.gui.hud.isHidden();
		if (!hideGuiWas) {
			mc.gui.hud.toggle();
		}
		// the run-in: the keyframe, then everything up to the window's start, quiet, with the ghosts ticked as it goes
		stepping = true;
		quiet = true;
		try {
			index = 0;
			for (; index < keyframeRecords; index++) {
				apply(RECORDS.get(index), connection);
			}
			cursor = segStart;
			int from = cursor;
			while (cursor < windowStart) {
				playTick(connection);
			}
			runInTicks = cursor - from;
		} finally {
			quiet = false;
			stepping = false;
		}
		runInMs = (System.nanoTime() - t0) / 1e6;
		Entity hero = GHOSTS.get(heroId);
		Overcraft.LOG.info("[POTG] replay: {} records ({} in the keyframe), run-in {} ticks in {} ms, {} ghosts, hero ghost {}",
			RECORDS.size(), keyframeRecords, runInTicks, String.format(java.util.Locale.ROOT, "%.1f", runInMs), GHOSTS.size(),
			hero == null ? "missing" : hero.getClass().getSimpleName() + (hero instanceof LivingEntity l ? " " + l.getMainHandItem().getItem() : ""));
	}

	private static void read(byte[] seg, int start, List<Rec> into) {
		ByteBuf buf = Unpooled.wrappedBuffer(seg);
		while (buf.isReadable()) {
			int dt = VarInt.read(buf);
			int kind = buf.readByte();
			byte[] bytes = new byte[VarInt.read(buf)];
			buf.readBytes(bytes);
			into.add(new Rec(start + dt, kind, bytes));
		}
	}

	/** END_CLIENT_TICK: one server tick of the reel, then the ghosts' tick. Waits while the game's paused. */
	private static void step(Minecraft mc) {
		if (!active) {
			return;
		}
		ClientPacketListener connection = mc.getConnection();
		if (connection == null || mc.level == null) {
			end(mc, "left the level");
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		long t0 = System.nanoTime();
		stepping = true;
		try {
			playTick(connection);
		} finally {
			stepping = false;
		}
		long d = System.nanoTime() - t0;
		stepNanos += d;
		stepMax = Math.max(stepMax, d);
		steps++;
		// keys pressed while it played do nothing afterwards (the held ones stay held)
		for (KeyMapping key : mc.options.keyMappings) {
			while (key.consumeClick()) {
			}
		}
		if (cursor > endTick) {
			end(mc, "window played");
		}
	}

	private static void playTick(ClientPacketListener connection) {
		while (index < RECORDS.size() && RECORDS.get(index).tick() <= cursor) {
			apply(RECORDS.get(index), connection);
			index++;
		}
		for (Entity ghost : new ArrayList<>(GHOSTS.values())) {
			if (ghost.isRemoved() || ghost.isPassenger()) {
				continue;
			}
			tickGhost(ghost);
		}
		cursor++;
	}

	private static void tickGhost(Entity ghost) {
		try {
			ghost.commonTick();
			ghost.tick();
			for (Entity passenger : ghost.getPassengers()) {
				if (GHOSTS.get(passenger.getId()) == passenger && !passenger.isRemoved()) {
					passenger.commonTick();
					passenger.rideTick();
				}
			}
		} catch (Throwable t) {
			error("tick " + net.minecraft.world.entity.EntityType.getKey(ghost.getType()).getPath(), t);
		}
	}

	@SuppressWarnings("unchecked")
	private static void apply(Rec rec, ClientPacketListener connection) {
		if (rec.kind() == PotgTap.KIND_TRACK) {
			track(rec.bytes());
			return;
		}
		Packet<? super ClientGamePacketListener> packet;
		try {
			packet = protocol.codec().decode(Unpooled.wrappedBuffer(rec.bytes()));
		} catch (Throwable t) {
			error("decode", t);
			return;
		}
		String type = PotgTap.typeName(packet);
		if (skip(type)) {
			return;
		}
		if (packet instanceof ClientboundExplodePacket e && e.playerKnockback().isPresent()) {
			// the knockback was the hero's, and handleExplosion gives it to the real player (design: "strip that")
			packet = new ClientboundExplodePacket(e.center(), e.radius(), e.blockCount(), java.util.Optional.empty(),
				e.explosionParticle(), e.explosionSound(), e.blockParticles(), e.playSound());
		}
		try {
			((Packet<ClientGamePacketListener>) packet).handle(connection);
			applied++;
		} catch (Throwable t) {
			error(type, t);
		}
	}

	/** Not applied: the world's blocks (no rewind yet), HeroSync (its own slot, later), and in the run-in, what's heard or seen. */
	private static final Set<String> NEVER = Set.of("block_update", "section_blocks_update", "block_event", "block_destruction",
		"block_entity_data", "custom_payload/overcraft:hero_sync");
	private static final Set<String> LOUD = Set.of("sound", "sound_entity", "level_event", "level_particles", "explode",
		"custom_payload/overcraft:effect", "custom_payload/overcraft:own_sound", "custom_payload/overcraft:hero_action",
		"custom_payload/overcraft:voice_line", "damage_event", "hurt_animation", "animate", "swing_animation");

	private static boolean skip(String type) {
		return NEVER.contains(type) || (quiet && LOUD.contains(type));
	}

	/** The hero's own track: where they were and looked, and whether alive (the server sends a player nothing of that). */
	private static void track(byte[] bytes) {
		Entity hero = GHOSTS.get(heroId);
		if (!(hero instanceof LivingEntity living)) {
			return;
		}
		ByteBuf buf = Unpooled.wrappedBuffer(bytes);
		double x = buf.readDouble();
		double y = buf.readDouble();
		double z = buf.readDouble();
		float yaw = buf.readFloat();
		float pitch = buf.readFloat();
		buf.readByte(); // pose: the entity data carries it
		int flags = buf.readByte();
		hero.moveOrInterpolateTo(new Vec3(x, y, z), yaw, pitch);
		living.lerpHeadTo(yaw, 3);
		hero.setOnGround((flags & 1) != 0);
		boolean alive = (flags & 2) != 0;
		if (alive && !heroAlive) {
			// a respawn: the stream has no "remove and add" for the hero's own body (design: "The hero dies in a window")
			living.deathTime = 0;
			living.setHealth(living.getMaxHealth());
			living.setPose(Pose.STANDING);
		}
		heroAlive = alive;
	}

	private static void error(String what, Throwable t) {
		int[] n = ERRORS.computeIfAbsent(what, k -> new int[1]);
		if (n[0]++ == 0) {
			Overcraft.LOG.warn("[POTG] replay: {} failed: {}", what, t.toString());
		}
	}

	private static void end(Minecraft mc, String why) {
		stepping = true;
		try {
			for (Entity ghost : new ArrayList<>(GHOSTS.values())) {
				ghost.setRemoved(Entity.RemovalReason.DISCARDED);
				ghost.onClientRemoval();
			}
		} catch (Throwable t) {
			error("remove", t);
		} finally {
			stepping = false;
		}
		GHOSTS.clear();
		active = false;
		if (liveAnimators != null) {
			dev.overcraft.client.model.HeroAnimator.swapAll(liveAnimators);
			liveAnimators = null;
		}
		dev.overcraft.client.effect.Fx.clear();
		if (mc.gui.hud.isHidden() != hideGuiWas) {
			mc.gui.hud.toggle();
		}
		Overcraft.LOG.info(String.format(java.util.Locale.ROOT,
			"[POTG] replay ended (%s): %d steps, step ms mean %.3f max %.3f; frames %d, frame ms mean %.1f max %.1f; most ghosts %d; packets applied %d; errors %s",
			why, steps, steps == 0 ? 0 : stepNanos / 1e6 / steps, stepMax / 1e6, frames, frames == 0 ? 0 : frameNanosSum / 1e6 / frames,
			frameNanosMax / 1e6, maxGhosts, applied, errorsText()));
		PotgReplay.clientDone();
	}

	private static String errorsText() {
		if (ERRORS.isEmpty()) {
			return "none";
		}
		StringBuilder s = new StringBuilder();
		ERRORS.forEach((k, v) -> s.append(k).append(" x").append(v[0]).append(", "));
		return s.toString();
	}

	/** The hero's ghost, for the chase view (CameraMixin). */
	public static @Nullable Entity heroGhost() {
		return GHOSTS.get(heroId);
	}

	private static final LevelEntityGetter<Entity> GETTER = new LevelEntityGetter<>() {
		@Override
		public @Nullable Entity get(int id) {
			return GHOSTS.get(id);
		}

		@Override
		public @Nullable Entity get(UUID id) {
			for (Entity e : GHOSTS.values()) {
				if (e.getUUID().equals(id)) {
					return e;
				}
			}
			return null;
		}

		@Override
		public Iterable<Entity> getAll() {
			return new ArrayList<>(GHOSTS.values());
		}

		@Override
		public <U extends Entity> void get(EntityTypeTest<Entity, U> type, AbortableIterationConsumer<U> consumer) {
			for (Entity e : new ArrayList<>(GHOSTS.values())) {
				U u = type.tryCast(e);
				if (u != null && consumer.accept(u).shouldAbort()) {
					return;
				}
			}
		}

		@Override
		public void get(AABB bb, Consumer<Entity> output) {
			for (Entity e : new ArrayList<>(GHOSTS.values())) {
				if (e.getBoundingBox().intersects(bb)) {
					output.accept(e);
				}
			}
		}

		@Override
		public <U extends Entity> void get(EntityTypeTest<Entity, U> type, AABB bb, AbortableIterationConsumer<U> consumer) {
			for (Entity e : new ArrayList<>(GHOSTS.values())) {
				U u = type.tryCast(e);
				if (u != null && e.getBoundingBox().intersects(bb) && consumer.accept(u).shouldAbort()) {
					return;
				}
			}
		}
	};
}

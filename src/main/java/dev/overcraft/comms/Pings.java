package dev.overcraft.comms;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.Allies;
import dev.overcraft.net.PingPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

/**
 * Pings (#40, tools/reference/comms-reference.md: the owner's Overwatch 2 screenshots): what a player pings goes to
 * them and their teammates (Allies: the scoreboard team, or every player with no teams), as Overwatch's do. The client
 * picks the target and draws them (client.comms.Pings); the server checks it, names what was pinged (so a teammate too
 * far away to have it loaded still can), and turns a spot on a generated structure seen from outside into that
 * structure's ping (owner, 2026-10-27: "structures (unless you're close/inside them)"), as only the server knows
 * structures.
 */
public final class Pings {
	/** How far a ping reaches (m, ours: Overwatch gives no number; it pings across maps, and structures want range). */
	public static final double REACH = 256;
	/** Blocks that get their own ping (chests, ores, spawners, portals, beds, workstations; data/overcraft/tags). */
	public static final TagKey<Block> NOTABLE = TagKey.create(Registries.BLOCK, Overcraft.id("notable_pings"));
	/** A spot this near a structure's piece counts as on it: paths and gardens around a village's houses (blocks; ours). */
	private static final int ON_STRUCTURE = 2;
	/** Nearer the structure than this, or inside it, a spot is just a spot (owner; blocks, ours). */
	private static final int AT_STRUCTURE = 24;
	/** The least time between two ping lines (ticks; ours): pinging again sooner says nothing. */
	private static final int PING_LINE_TICKS = 20;

	private Pings() {
	}

	public static void ping(ServerPlayer player, PingPayload ping) {
		if (ping.kind() < PingPayload.CANCEL || ping.kind() > PingPayload.BLOCK || !player.isAlive()) {
			return;
		}
		if (ping.kind() != PingPayload.CANCEL && player.getEyePosition().distanceToSqr(ping.x(), ping.y(), ping.z()) > REACH * REACH) {
			return;
		}
		PingPayload out = named(player, ping);
		// the hero calls it out, as Overwatch's do ("your teammates will hear Tracer call out that Reaper's location":
		// Blizzard's ping article), by the game's rules for a ping's line (Voice: they may cut into chatter; the warning's
		// and the enemy's wait 3 s, and a double press's warning cuts off the first press's "Over there!"); a plain ping's
		// line at most once a second (ours: its own stimulus has no cooldown, and a press a tick would talk over itself)
		dev.overcraft.hero.HeroState state = dev.overcraft.hero.HeroManager.state(player);
		long now = player.level().getGameTime();
		long last = state == null ? -100 : state.saidAt.getOrDefault("ping", -100L);
		boolean warning = out.kind() == PingPayload.WARNING;
		if (out.kind() != PingPayload.CANCEL && state != null && state.hero != null && (warning || now - last >= PING_LINE_TICKS)) {
			String moment = out.kind() == PingPayload.ENEMY ? "ping_enemy" : warning ? "ping_warning" : "ping";
			state.saidAt.put("ping", now);
			dev.overcraft.hero.Voice.say(player, moment, 1);
		}
		for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
			if (other.level() == player.level() && (other == player || Allies.isAlly(player, other))) {
				ServerPlayNetworking.send(other, out);
			}
		}
	}

	/** The ping as teammates get it: from this player, its thing named by the server's own world, or a plain spot. */
	private static PingPayload named(ServerPlayer player, PingPayload ping) {
		ServerLevel level = player.level();
		int kind = ping.kind();
		String what = "";
		if (PingPayload.follows(kind)) {
			Entity entity = level.getEntity(ping.entity());
			if (entity == null || !entity.isAlive()) {
				kind = PingPayload.SPOT;
			} else if (entity instanceof ItemEntity item) {
				kind = PingPayload.ITEM;
				what = BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString();
			} else {
				kind = kind == PingPayload.ENEMY ? PingPayload.ENEMY : PingPayload.THING;
				what = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			}
		} else if (kind == PingPayload.BLOCK) {
			BlockState state = level.getBlockState(BlockPos.containing(ping.x(), ping.y(), ping.z()));
			if (state.is(NOTABLE)) {
				what = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
			} else {
				kind = PingPayload.SPOT;
			}
		}
		if (kind == PingPayload.SPOT) {
			Identifier structure = structureSeenAt(player, BlockPos.containing(ping.x(), ping.y(), ping.z()));
			if (structure != null) {
				kind = PingPayload.STRUCTURE;
				what = structure.toString();
			}
		}
		int entity = PingPayload.follows(kind) ? ping.entity() : -1;
		return new PingPayload(player.getId(), kind, ping.x(), ping.y(), ping.z(), entity, what);
	}

	/**
	 * The generated structure with a piece at (or just by) this spot, if the player is outside it and not right by it;
	 * only from chunks already loaded, so a ping never loads or generates any.
	 */
	private static @Nullable Identifier structureSeenAt(ServerPlayer player, BlockPos at) {
		ServerLevel level = player.level();
		int chunkX = SectionPos.blockToSectionCoord(at.getX());
		int chunkZ = SectionPos.blockToSectionCoord(at.getZ());
		if (!level.hasChunk(chunkX, chunkZ)) {
			return null;
		}
		for (StructureStart start : level.structureManager().startsForStructure(chunkX, chunkZ, s -> true)) {
			boolean on = false;
			for (StructurePiece piece : start.getPieces()) {
				if (piece.getBoundingBox().inflatedBy(ON_STRUCTURE).isInside(at)) {
					on = true;
					break;
				}
			}
			BoundingBox whole = start.getBoundingBox();
			if (on && !whole.inflatedBy(AT_STRUCTURE).isInside(player.blockPosition())) {
				return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
			}
		}
		return null;
	}
}

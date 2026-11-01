package dev.overcraft.brigitte;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

import static dev.overcraft.brigitte.BrigitteStats.*;

/**
 * Repair Pack (0EDD on her, 0EE1 on the target): a pack flies from her to the ally at 55 m/s, homing (#337: its end moves
 * with them), and heals 25 when it lands and 100 over the next 2 s (50 a second). A second pack on an ally already being
 * healed adds 2 s to what's left and another 25 (#458 / #466 / #555). Here the pack's flight is its time (distance / 55 m/s,
 * the ally's distance when it lands); its look comes with her effects.
 */
public final class RepairPack {
	private record Flying(ServerPlayer brigitte, LivingEntity ally, long launched) {
	}

	private static final class Heal {
		final ServerPlayer brigitte;
		final LivingEntity ally;
		long until;

		Heal(ServerPlayer brigitte, LivingEntity ally, long until) {
			this.brigitte = brigitte;
			this.ally = ally;
			this.until = until;
		}
	}

	private static final List<Flying> FLYING = new ArrayList<>();
	private static final List<Heal> HEALS = new ArrayList<>();

	private RepairPack() {
	}

	public static void launch(ServerPlayer brigitte, LivingEntity ally) {
		FLYING.add(new Flying(brigitte, ally, brigitte.level().getGameTime()));
		// in flight (218D): from her left wrist (hp 0002) to the ally's hips at its speed, drawn by her clients
		dev.overcraft.hero.Effects.play(brigitte.level(), "brigitte_pack", brigitte.getEyePosition().add(0, -0.5, 0),
			net.minecraft.world.phys.Vec3.ZERO, ally, 0, brigitte);
	}

	private static void land(ServerPlayer brigitte, LivingEntity ally) {
		give(brigitte, ally, REPAIR_HEAL_NOW);
		BrigitteSounds.on(ally, "repair.land", 40);
		if (ally instanceof ServerPlayer healed) {
			BrigitteSounds.own(healed, "repair.heal.start");
			BrigitteSounds.own(healed, "repair.heal.loop");
		}
		// the arrival at the ally's hips (2F2E) and its heal over time (2074, the 2 s)
		dev.overcraft.hero.Effects.play(brigitte.level(), "brigitte_pack_arrive", ally.position(), net.minecraft.world.phys.Vec3.ZERO, ally,
			Ow.ticks(REPAIR_TIME), brigitte);
		long now = ally.level().getGameTime();
		for (Heal h : HEALS) {
			if (h.ally == ally) {
				h.until += Ow.ticks(REPAIR_EXTEND);
				return;
			}
		}
		HEALS.add(new Heal(brigitte, ally, now + Ow.ticks(REPAIR_TIME)));
	}

	private static void give(ServerPlayer brigitte, LivingEntity ally, double amount) {
		float done = Combat.heal(ally, amount);
		if (done > 0) {
			Combat.credit(brigitte, done);
			Combat.addFireForHealing(brigitte, done);
		}
	}

	public static void tick(MinecraftServer server) {
		FLYING.removeIf(f -> {
			if (!f.ally.isAlive() || f.ally.isRemoved() || f.brigitte.isRemoved()) {
				return true;
			}
			double flown = Ow.speed(REPAIR_SPEED) * (f.ally.level().getGameTime() - f.launched);
			if (flown >= f.brigitte.distanceTo(f.ally)) {
				land(f.brigitte, f.ally);
				return true;
			}
			return false;
		});
		HEALS.removeIf(h -> {
			if (!h.ally.isAlive() || h.ally.isRemoved() || h.ally.level().getGameTime() >= h.until) {
				if (h.ally instanceof ServerPlayer healed) {
					healed.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
						dev.overcraft.Overcraft.id("brigitte.repair.heal.loop"), net.minecraft.sounds.SoundSource.PLAYERS));
					BrigitteSounds.own(healed, "repair.heal.end");
				}
				return true;
			}
			give(h.brigitte, h.ally, REPAIR_HEAL_RATE / 20);
			return false;
		});
	}

	public static void clear() {
		FLYING.clear();
		HEALS.clear();
	}
}

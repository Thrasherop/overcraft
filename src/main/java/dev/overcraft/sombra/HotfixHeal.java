package dev.overcraft.sombra;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

import static dev.overcraft.sombra.SombraStats.*;

/**
 * Hotfix on an ally (its heal graph 60FB): 65 at once, then 25 a second, until 175 healed or 12 s (or their death). A
 * new Hotfix on the same ally starts its own (ours: 60FB's stacking rule isn't read; each heals on its own).
 */
public final class HotfixHeal {
	private record Heal(ServerPlayer sombra, LivingEntity ally, long until, double[] healed) {
	}

	private static final List<Heal> HEALS = new ArrayList<>();

	private HotfixHeal() {
	}

	/** The allies her Hotfix is healing now (her 0D81 slots). */
	public static java.util.List<Integer> healing(ServerPlayer sombra) {
		java.util.List<Integer> ids = new ArrayList<>();
		for (Heal heal : HEALS) {
			if (heal.sombra == sombra) {
				ids.add(heal.ally.getId());
			}
		}
		return ids;
	}

	public static void start(ServerPlayer sombra, LivingEntity ally) {
		double[] healed = {0};
		HEALS.add(new Heal(sombra, ally, ally.level().getGameTime() + Ow.ticks(HOTFIX_HEAL_TIME), healed));
		give(sombra, ally, healed, HOTFIX_HEAL_NOW);
		// it lands: heard by her and by the ally in their heads, and by everyone else at the ally
		SombraSounds.own(sombra, "hotfix.heal");
		ServerPlayer allyPlayer = ally instanceof ServerPlayer p && p != sombra ? p : sombra;
		if (allyPlayer != sombra) {
			SombraSounds.own(allyPlayer, "hotfix.heal");
		}
		dev.overcraft.hero.HeroSounds.at((net.minecraft.server.level.ServerLevel) ally.level(), SombraSounds.of("hotfix.heal_3p"),
			ally.position(), 45, sombra, allyPlayer);
		// its look on them while it heals (6570)
		dev.overcraft.hero.Effects.play((net.minecraft.server.level.ServerLevel) ally.level(), "sombra_hotfixed", ally.position(),
			net.minecraft.world.phys.Vec3.ZERO, ally, Ow.ticks(HOTFIX_HEAL_TIME), sombra);
	}

	/** Heals up to {@code amount}, never past the 175 in all; counts what it gave. */
	private static void give(ServerPlayer sombra, LivingEntity ally, double[] healed, double amount) {
		double left = HOTFIX_HEAL_TOTAL - healed[0];
		if (left <= 0) {
			return;
		}
		float done = Combat.heal(ally, Math.min(amount, left));
		healed[0] += done;
		if (done > 0) {
			Combat.credit(sombra, done);
			Combat.addFireForHealing(sombra, done);
		}
	}

	public static void tick(MinecraftServer server) {
		HEALS.removeIf(h -> {
			if (!h.ally.isAlive() || h.ally.isRemoved() || h.ally.level().getGameTime() >= h.until || h.healed[0] >= HOTFIX_HEAL_TOTAL) {
				if (h.ally.isAlive() && HEALS.stream().filter(o -> o.ally == h.ally).count() == 1) {
					dev.overcraft.hero.Effects.play((net.minecraft.server.level.ServerLevel) h.ally.level(), "sombra_hotfixed_end",
						h.ally.position(), net.minecraft.world.phys.Vec3.ZERO, h.ally, 0, h.sombra);
				}
				return true;
			}
			give(h.sombra, h.ally, h.healed, HOTFIX_HEAL_RATE / 20);
			return false;
		});
	}

	/** Whether this ally has a Hotfix on them now (its effects). */
	public static boolean on(LivingEntity ally) {
		return HEALS.stream().anyMatch(h -> h.ally == ally);
	}

	public static void clear() {
		HEALS.clear();
	}
}

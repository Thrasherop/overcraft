package dev.overcraft.sombra;

import dev.overcraft.Ow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.sombra.SombraStats.*;

/**
 * Hacked (the status 0B1F): from EMP, enemies are silenced 3 s and revealed 8 s; an enemy deployable stops working 10 s
 * (EMP's, Hotfix's). In Minecraft (ours, sombra-plan.md "Minecraft decisions"):
 * - an enemy hero's abilities are locked (their ability and ultimate keys do nothing: HeroManager), their weapon still fires;
 * - a mob's special moves stop: a creeper's fuse is cut, and whatever it launches is gone at once (a skeleton's or a
 *   pillager's arrow, a witch's potion, a blaze's or a ghast's fireball, an evoker's fangs); it still walks and hits;
 * - revealed: Minecraft's glowing (seen through walls by everyone, where Overwatch shows them to her side only: ours).
 */
public final class Hack {
	/** Who is silenced until when (game time); deployables hacked until when. */
	private static final Map<UUID, Long> SILENCED = new HashMap<>();
	private static final Map<UUID, Long> DEPLOYABLES = new HashMap<>();
	/** Who Hack's reveal is on, until when. */
	private static final Map<UUID, Long> REVEALED = new HashMap<>();
	private static final java.util.Set<Creeper> CREEPERS = new java.util.HashSet<>();

	private Hack() {
	}

	/** A creature hacked: silenced this long, revealed 8 s. */
	public static void creature(LivingEntity target, double silence) {
		creature(target, silence, null);
	}

	/** ... by this Sombra (her effects: the hacked look for the silence, the reveal's skull after it: 1704, 1B2F). */
	public static void creature(LivingEntity target, double silence, @Nullable Entity by) {
		if (dev.overcraft.hero.Combat.phaseStops(target)) {
			return;
		}
		long now = target.level().getGameTime();
		if (target.level() instanceof net.minecraft.server.level.ServerLevel level) {
			dev.overcraft.hero.Effects.play(level, "sombra_hacked", target.position(), net.minecraft.world.phys.Vec3.ZERO, target,
				Ow.ticks(silence), by);
			// the hacked one hears it in their head, everyone else at them
			if (target instanceof net.minecraft.server.level.ServerPlayer p) {
				SombraSounds.own(p, "hacked");
				dev.overcraft.hero.HeroSounds.at(level, SombraSounds.of("hacked_3p"), target.getEyePosition(), 45, p);
			} else {
				SombraSounds.at(level, "hacked_3p", target.getEyePosition(), 45);
			}
			dev.overcraft.hero.Effects.play(level, "sombra_revealed", target.position(), net.minecraft.world.phys.Vec3.ZERO, target,
				Ow.ticks(HACK_REVEAL), by);
		}
		SILENCED.merge(target.getUUID(), now + Ow.ticks(silence), Math::max);
		REVEALED.merge(target.getUUID(), now + Ow.ticks(HACK_REVEAL), Math::max);
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, Ow.ticks(HACK_REVEAL), 0, false, false));
		if (target instanceof Creeper creeper) {
			creeper.setSwellDir(-1);
			CREEPERS.add(creeper);
		}
	}

	/**
	 * Protection Suzu's cleanse (Combat.cleanse's table): the whole status ends, silence and reveal (0B1F #453 {v6604, v8440} -> #610 -> #611 -> #609),
	 * and its looks go with it. Only a glowing Hack gave is taken off: one from elsewhere (a spectral arrow) can't be told apart while the reveal runs, and goes too.
	 */
	public static void cleanse(LivingEntity target) {
		boolean silenced = SILENCED.remove(target.getUUID()) != null;
		boolean revealed = REVEALED.remove(target.getUUID()) != null;
		if (!silenced && !revealed) {
			return;
		}
		if (revealed) {
			target.removeEffect(MobEffects.GLOWING);
		}
		if (target instanceof net.minecraft.server.level.ServerPlayer player) {
			// their own hacked sound (2.5 s and more of it played on after a cleanse: runs/polish4/crit_after.txt) goes; ours. A stop names a sound, so to them alone: others' 3p runs out
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(dev.overcraft.Overcraft.id("sombra.hacked"), net.minecraft.sounds.SoundSource.PLAYERS));
		}
		if (target.level() instanceof net.minecraft.server.level.ServerLevel level) {
			dev.overcraft.hero.Effects.play(level, "sombra_hacked_end", target.position(), net.minecraft.world.phys.Vec3.ZERO, target, 0, null);
			dev.overcraft.hero.Effects.play(level, "sombra_revealed_end", target.position(), net.minecraft.world.phys.Vec3.ZERO, target, 0, null);
		}
	}

	/** An enemy deployable (Lifeweaver's platform or tree) hacked: it stops working 10 s. */
	public static void deployable(Entity deployable) {
		deployable(deployable, null);
	}

	/** ... by this Sombra (its hacked look, 7D35). */
	public static void deployable(Entity deployable, @Nullable Entity by) {
		if (deployable.level() instanceof net.minecraft.server.level.ServerLevel level) {
			dev.overcraft.hero.Effects.play(level, "sombra_hacked_thing", deployable.position(), net.minecraft.world.phys.Vec3.ZERO,
				deployable, Ow.ticks(HACK_DEPLOYABLE), by);
			SombraSounds.at(level, "hack.object_3p", deployable.position(), 45);
		}
		DEPLOYABLES.merge(deployable.getUUID(), deployable.level().getGameTime() + Ow.ticks(HACK_DEPLOYABLE), Math::max);
	}

	public static boolean silenced(@Nullable Entity entity) {
		Long until = entity == null ? null : SILENCED.get(entity.getUUID());
		return until != null && entity.level().getGameTime() < until;
	}

	public static boolean hacked(@Nullable Entity deployable) {
		Long until = deployable == null ? null : DEPLOYABLES.get(deployable.getUUID());
		return until != null && deployable.level().getGameTime() < until;
	}

	/** Whatever a silenced mob launches is gone at once (Overcraft's entity-load hook). */
	public static boolean launchedBySilenced(Entity entity) {
		return entity instanceof Projectile projectile && !(projectile.getOwner() instanceof net.minecraft.world.entity.player.Player)
			&& silenced(projectile.getOwner())
			|| entity instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs && silenced(fangs.getOwner());
	}

	public static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		SILENCED.values().removeIf(until -> now >= until);
		REVEALED.values().removeIf(until -> now >= until);
		DEPLOYABLES.values().removeIf(until -> now >= until);
		// a silenced creeper's fuse stays cut (its swell goal lights it again each tick it's near its target)
		CREEPERS.removeIf(creeper -> {
			if (creeper.isRemoved() || !silenced(creeper)) {
				return true;
			}
			creeper.setSwellDir(-1);
			return false;
		});
	}

	public static void clear() {
		SILENCED.clear();
		REVEALED.clear();
		DEPLOYABLES.clear();
		CREEPERS.clear();
	}
}

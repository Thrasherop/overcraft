package dev.overcraft.ana;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Sleep;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ana's Local Anesthetic perk (minor): her Sleep Dart bursts where it hits. 0.15 s after, every enemy within 4 m and in
 * sight of it (51F0 #451) takes 45 over 3 s in 30 ticks of 1.5 (6123 #374 / #372) and is slowed 40%, easing to 20% over the
 * 3 s (6123 #380: M; the wiki's 50% not used). Its damage never wakes a sleeper, and it puts no one to sleep: only the
 * dart's own target sleeps (tools/reference/ana.md "Perks").
 */
public final class Anesthetic {
	private static final Identifier SLOW = dev.overcraft.Overcraft.id("ana_local_anesthetic");
	private static final double SEARCH_DELAY = 0.15;

	private record Burst(ServerLevel level, ServerPlayer ana, Vec3 at, long when) {
	}

	private record Dosed(LivingEntity entity, ServerPlayer ana, long start) {
	}

	private static final List<Burst> BURSTS = new ArrayList<>();
	private static final Map<UUID, Dosed> DOSED = new HashMap<>();

	private Anesthetic() {
	}

	/** Her Sleep Dart hit something (a creature or a block) at {@code at}. */
	public static void burst(ServerLevel level, ServerPlayer ana, Vec3 at) {
		BURSTS.add(new Burst(level, ana, at, level.getGameTime() + Ow.ticks(SEARCH_DELAY)));
		// the dart's burst (7E5F's 104AC / 104AE: everyone in earshot, 90 m)
		dev.overcraft.hero.HeroSounds.at(level, dev.overcraft.registry.OcSounds.ANA_ANESTHETIC, at, 90);
		dev.overcraft.hero.Effects.play(level, "ana_anesthetic", at, ana);
	}

	public static void tick(MinecraftServer server) {
		for (Burst b : new ArrayList<>(BURSTS)) {
			if (b.level.getGameTime() < b.when) {
				continue;
			}
			BURSTS.remove(b);
			double r = AnaStats.ANESTHETIC_RADIUS;
			for (LivingEntity e : b.level.getEntitiesOfClass(LivingEntity.class, new AABB(b.at, b.at).inflate(r + 2))) {
				if (e.isAlive() && Allies.canHurt(b.ana, e) && e.getBoundingBox().distanceToSqr(b.at) <= r * r && inSight(b.level, b.at, e)) {
					// a second burst on the same one starts its 3 s over (ours)
					DOSED.put(e.getUUID(), new Dosed(e, b.ana, b.level.getGameTime()));
					// numbed (7E60 / 7E70 on the enemy): theirs, and at them for everyone else, Ana too (she has no copy of
					// her own: left out, she heard nothing of it, found playing)
					ServerPlayer numbed = e instanceof ServerPlayer p ? p : null;
					if (numbed != null) {
						dev.overcraft.hero.HeroSounds.toOwner(numbed, dev.overcraft.registry.OcSounds.ANA_ANESTHETIC_DOT);
					}
					dev.overcraft.hero.HeroSounds.at(b.level, dev.overcraft.registry.OcSounds.ANA_ANESTHETIC_DOT_3P,
						e.getBoundingBox().getCenter(), 45, numbed);
					dev.overcraft.hero.Effects.play(b.level, "ana_numbed", e.position(), Vec3.ZERO, e,
						Ow.ticks(AnaStats.ANESTHETIC_TIME), b.ana);
				}
			}
		}
		int ticks = AnaStats.ANESTHETIC_TICKS;
		int length = Ow.ticks(AnaStats.ANESTHETIC_TIME);
		for (Dosed d : new ArrayList<>(DOSED.values())) {
			long t = d.entity.level().getGameTime() - d.start;
			if (!d.entity.isAlive() || d.entity.isRemoved() || t > length) {
				DOSED.remove(d.entity.getUUID());
				continue;
			}
			// its ticks spread over the 3 s (30 in 60 game ticks: one every 2)
			if (t > 0 && t * ticks / length != (t - 1) * ticks / length) {
				float dealt = Sleep.quietly(() -> Combat.damage(d.entity, Ana.shotSource(d.ana), AnaStats.ANESTHETIC_DAMAGE / ticks, dev.overcraft.hero.Blow.perk("local_anesthetic")));
				Combat.credit(d.ana, dealt);
			}
			double slow = AnaStats.ANESTHETIC_SLOW_FROM + (AnaStats.ANESTHETIC_SLOW_TO - AnaStats.ANESTHETIC_SLOW_FROM) * t / length;
			Combat.buff(d.entity, Attributes.MOVEMENT_SPEED, SLOW, -slow, 0.1);
		}
	}

	private static boolean inSight(ServerLevel level, Vec3 at, LivingEntity e) {
		for (Vec3 to : new Vec3[] {e.getBoundingBox().getCenter(), e.getEyePosition()}) {
			if (level.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	public static void clear() {
		BURSTS.clear();
		DOSED.clear();
	}
}

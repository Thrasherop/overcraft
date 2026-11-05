package dev.overcraft.juno;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Effects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.overcraft.juno.JunoStats.*;

/**
 * Juno's Hyper Ring (4857, 4858 on the ring): thrown 0.25 s into its 0.55 s cast, it flies 35 m/s for 0.5 s (17.5 m, or to a
 * wall) then hangs where it stopped until 6 s are up; a 4.5 m ring facing her aim. Every ally passing through it (her too)
 * is 50% faster for 3 s, once a ring.
 */
public final class HyperRing {
	private static final Identifier FAST = dev.overcraft.Overcraft.id("juno_hyper_ring");

	private static final class Ring {
		final ServerPlayer juno;
		Vec3 centre;
		final Vec3 normal;
		final long born;
		final Set<Integer> boosted = new HashSet<>();
		final Map<Integer, Vec3> last = new HashMap<>();

		Ring(ServerPlayer juno, Vec3 centre, Vec3 normal, long born) {
			this.juno = juno;
			this.centre = centre;
			this.normal = normal;
			this.born = born;
		}
	}

	private static final List<Ring> RINGS = new ArrayList<>();
	/** Each sped-up entity: when its 3 s end. */
	private static final Map<LivingEntity, Long> FASTER = new HashMap<>();

	private HyperRing() {
	}

	/** The throw, 0.25 s into the cast, from her left hand (hp 0002: L, ours just ahead of her chest), along her aim. */
	static void thrown(ServerPlayer juno) {
		Vec3 look = juno.getViewVector(1.0f);
		Vec3 from = juno.getEyePosition().add(look.scale(0.5)).add(0, -0.3, 0);
		RINGS.add(new Ring(juno, from, look, juno.level().getGameTime()));
		Effects.play(juno.level(), "juno_ring", from, look, juno, 0, juno);
		// the ring made: its "sound only" slot (5530 m_2170A015 -> 5F2F, 60 m; H slot, M role) and its forming (4858 #618 ->
		// 541C, 70 m; M the moment), for everyone. Its hum while it lasts follows it on her clients (JunoSounds)
		JunoSounds.at(juno.level(), "ring.launch", from, 60);
		JunoSounds.at(juno.level(), "ring.appear", from, 70);
	}

	public static void tickAll(MinecraftServer server) {
		for (Iterator<Ring> it = RINGS.iterator(); it.hasNext(); ) {
			Ring r = it.next();
			ServerLevel level = r.juno.level();
			long age = level.getGameTime() - r.born;
			if (age >= Ow.ticks(RING_LIFE) || r.juno.isRemoved()) {
				it.remove();
				continue;
			}
			if (age == Ow.ticks(RING_LIFE - RING_END_SOUND)) {
				// its end, 0.15 s before the 6 s are up (#544 -> #508 53CA, 70 m), for everyone
				JunoSounds.at(level, "ring.end", r.centre, 70);
			}
			if (age < Ow.ticks(RING_FLIGHT)) {
				// in flight: 35 m/s along her aim, stopping at a wall
				Vec3 next = r.centre.add(r.normal.scale(Ow.speed(RING_SPEED)));
				var hit = level.clip(new ClipContext(r.centre, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, r.juno));
				r.centre = hit.getType() == HitResult.Type.MISS ? next : hit.getLocation().subtract(r.normal.scale(0.3));
			}
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(r.centre, r.centre)
				.inflate(RING_RADIUS + 3), e -> e.isAlive() && Allies.isSelfOrAlly(r.juno, e))) {
				Vec3 now = e.getBoundingBox().getCenter().subtract(r.centre);
				Vec3 before = r.last.put(e.getId(), now);
				if (before == null || r.boosted.contains(e.getId())) {
					continue;
				}
				// through it: from one side of its plane to the other, within its radius where it crossed
				double a = before.dot(r.normal), b = now.dot(r.normal);
				if (a == b || Math.signum(a) == Math.signum(b)) {
					continue;
				}
				Vec3 at = before.add(now.subtract(before).scale(a / (a - b)));
				if (at.length() <= RING_RADIUS) {
					r.boosted.add(e.getId());
					speedUp(e, level.getGameTime());
				}
			}
		}
		FASTER.entrySet().removeIf(f -> {
			if (f.getKey().isRemoved() || f.getKey().level().getGameTime() >= f.getValue()) {
				AttributeInstance speed = f.getKey().getAttribute(Attributes.MOVEMENT_SPEED);
				if (speed != null) {
					speed.removeModifier(FAST);
				}
				return true;
			}
			return false;
		});
	}

	private static void speedUp(LivingEntity e, long now) {
		AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(FAST, RING_BOOST, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		FASTER.put(e, now + Ow.ticks(RING_BOOST_TIME));
		// through it (#437 per ally -> #432 5A8B): in the ally's head, its 45 m copy at them for the rest (M on whom)
		JunoSounds.on(e, "ring.pass", 45);
		Effects.play((ServerLevel) e.level(), "juno_ring_boost", e.position(), Vec3.ZERO, e, Ow.ticks(RING_BOOST_TIME), null);
	}

	public static void clearAll() {
		RINGS.clear();
		for (LivingEntity e : FASTER.keySet()) {
			AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.removeModifier(FAST);
			}
		}
		FASTER.clear();
	}
}

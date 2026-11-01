package dev.overcraft.sombra;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import static dev.overcraft.sombra.SombraStats.*;

/**
 * Sombra's stealth (graph 420C), after every teleport: 0.75 s fading in, 6.5 s stealthed, +60% speed from the landing;
 * firing, Cyberspace, EMP and quick melee end it; damage doesn't (it reveals her 1.3 s); an enemy within 4 m in sight sees
 * her. Here: Minecraft's invisibility hides her (its name tag and shadow with it), and two mixins decide who still sees her:
 * mobs only pick her up within 4 m in sight (TargetingConditionsMixin), players see her see-through when on her side or
 * within 4 m in sight (EntityMixin.isInvisibleTo).
 */
public final class Stealth {
	private static final Identifier SPEED = dev.overcraft.Overcraft.id("sombra_stealth");

	private Stealth() {
	}

	/** Her landing: stealth starts, mobs after her lose her. */
	public static void start(ServerPlayer player, HeroState s) {
		long now = player.level().getGameTime();
		s.stealthFrom = now;
		s.stealthUntil = now + Ow.ticks(STEALTH_FADE_IN + STEALTH_TIME);
		s.revealedUntil = -1;
		dropTargets(player);
	}

	/** Mobs after her that can't see her any more (further than 4 m, or out of sight) lose her. */
	private static void dropTargets(ServerPlayer player) {
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(48))) {
			if (mob.getTarget() == player && !detects(mob, player)) {
				mob.setTarget(null);
			}
		}
	}

	/** Firing, Cyberspace, EMP, quick melee: it ends (its 0.3 s fade-out: shown at once, ours). */
	public static void end(ServerPlayer player, HeroState s) {
		if (s.stealthUntil > player.level().getGameTime()) {
			s.stealthUntil = player.level().getGameTime();
		}
	}

	/** Seen for a moment without ending it: taking damage (1.3 s), Hotfix (its channel). */
	public static void reveal(ServerPlayer player, HeroState s, double seconds) {
		if (stealthed(player, s)) {
			s.revealedUntil = Math.max(s.revealedUntil, player.level().getGameTime() + Ow.ticks(seconds));
		}
	}

	/** Stealth under way (fading in or stealthed). */
	public static boolean stealthed(LivingEntity entity, HeroState s) {
		long now = entity.level().getGameTime();
		return s.stealthUntil > now && now >= s.stealthFrom;
	}

	/** Each tick of her kit: the speed while stealthed, and her invisibility once faded in, unless revealed. */
	public static void tick(ServerPlayer player, HeroState s) {
		long now = player.level().getGameTime();
		boolean on = stealthed(player, s);
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			if (!on) {
				speed.removeModifier(SPEED);
			} else if (!speed.hasModifier(SPEED)) {
				speed.addTransientModifier(new AttributeModifier(SPEED, STEALTH_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}
		boolean hidden = on && now >= s.stealthFrom + Ow.ticks(STEALTH_FADE_IN) && now >= s.revealedUntil;
		if (on && now >= s.revealedUntil && (now - s.stealthFrom) % 10 == 0) {
			// a mob still on her (a melee goal keeps its target without looking again) loses her, every half second
			dropTargets(player);
		}
		boolean potion = player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
		if (player.isInvisible() != (hidden || potion)) {
			player.setInvisible(hidden || potion);
		}
	}

	/** Leaving Sombra or dying: all of it off. */
	public static void clear(ServerPlayer player, HeroState s) {
		s.stealthUntil = -1;
		tick(player, s);
	}

	/** Server: a stealthed Sombra no mob can see from this far (TargetingConditionsMixin). */
	public static boolean hiddenFrom(LivingEntity target, LivingEntity looker) {
		if (!(target instanceof ServerPlayer player)) {
			return false;
		}
		HeroState s = HeroManager.state(player);
		// from her landing (fading in too: a mob that saw her land would pick her straight back up), unless revealed
		if (s == null || s.hero != Hero.SOMBRA || !stealthed(player, s) || player.level().getGameTime() < s.revealedUntil) {
			return false;
		}
		return !detects(looker, target);
	}

	/** Within 4 m and in sight (420C's detection). */
	public static boolean detects(Entity looker, Entity target) {
		return looker.distanceTo(target) <= STEALTH_DETECT && looker instanceof LivingEntity living && living.hasLineOfSight(target);
	}

	/**
	 * Both sides: whether this viewer sees a stealthed Sombra (see-through): her own side, or an enemy within 4 m in sight.
	 * Null when this isn't her (Minecraft's own rule applies).
	 */
	public static @org.jspecify.annotations.Nullable Boolean seenBy(Entity entity, Player viewer) {
		if (!(entity instanceof Player sombra) || !entity.isInvisible()
			|| !"sombra".equals(entity.getAttached(HeroManager.HERO))) {
			return null;
		}
		return viewer == sombra || Allies.isAlly(sombra, viewer) || detects(viewer, sombra);
	}
}

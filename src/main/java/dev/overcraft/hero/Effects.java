package dev.overcraft.hero;

import dev.overcraft.net.EffectPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Plays an ability effect for every player near it. The effects are drawn client-side from Overwatch's own effect
 * textures (client/effect); the server only says which, where and in which direction (they replaced Minecraft's
 * particles: the owner, 2026-10-26: "Thats not how ofuda look in game").
 */
public final class Effects {
	/** How far away an effect is still sent (blocks). */
	private static final double RANGE = 96;

	private Effects() {
	}

	public static void play(ServerLevel level, String effect, Vec3 at) {
		play(level, effect, at, Vec3.ZERO, null, 0, null);
	}

	/** An effect made by owner (the hero): its enemies see it in red. */
	public static void play(ServerLevel level, String effect, Vec3 at, @Nullable Entity owner) {
		play(level, effect, at, Vec3.ZERO, null, 0, owner);
	}

	/**
	 * The thrower's hit marker on their crosshair: 0 a hit, 1 a critical hit, 2 a kill. Sent to them alone (others
	 * don't see your hit markers).
	 */
	public static void hitMarker(ServerPlayer shooter, LivingEntity target, boolean crit) {
		int kind = !target.isAlive() || target.getHealth() <= 0 ? 2 : crit ? 1 : 0;
		ServerPlayNetworking.send(shooter, new EffectPayload("hit_marker", target.getX(), target.getY(), target.getZ(), 0, 0, 0,
			target.getId(), kind, false));
	}

	public static void play(ServerLevel level, String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, int variant) {
		play(level, effect, at, direction, entity, variant, null);
	}

	/** The same, made by owner: each player near it gets it in their own team's view of the owner. */
	public static void play(ServerLevel level, String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, int variant,
							@Nullable Entity owner) {
		for (ServerPlayer player : PlayerLookup.around(level, at, RANGE)) {
			boolean enemy = owner != null && owner != player && !Allies.wasAlly(player, owner);
			ServerPlayNetworking.send(player, new EffectPayload(effect, at.x, at.y, at.z, (float) direction.x,
				(float) direction.y, (float) direction.z, entity == null ? -1 : entity.getId(), variant, enemy));
		}
	}
}

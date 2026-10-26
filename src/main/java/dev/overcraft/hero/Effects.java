package dev.overcraft.hero;

import dev.overcraft.net.EffectPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
		play(level, effect, at, Vec3.ZERO, null, 0);
	}

	public static void play(ServerLevel level, String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, int variant) {
		EffectPayload payload = new EffectPayload(effect, at.x, at.y, at.z, (float) direction.x, (float) direction.y,
			(float) direction.z, entity == null ? -1 : entity.getId(), variant);
		for (ServerPlayer player : PlayerLookup.around(level, at, RANGE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}

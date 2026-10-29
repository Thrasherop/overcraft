package dev.overcraft.client;

import dev.overcraft.client.effect.KirikoFx;
import dev.overcraft.client.effect.LifeweaverFx;
import dev.overcraft.entity.PetalPlatformEntity;
import dev.overcraft.entity.PetalThrowEntity;
import dev.overcraft.entity.TreeOfLifeEntity;
import dev.overcraft.lifeweaver.Lifeweaver;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Lifeweaver's sounds that follow a state (tools/soundmaps/lifeweaver.txt), for every Lifeweaver and prop this client
 * sees: the blossom's charging hum, the tree's aim preview, the dash's heal (his own, 0.8 s), the thrown platform or seed
 * in flight, the platform's beds (armed, rising, raised), the vine, the tree. His own in his head; the others' on what
 * makes them, heard as far as the game's copy reaches. The one-shots come from the server (HeroSounds).
 */
public final class LifeweaverSounds {
	/** The dash's heal sound lasts this long (3DEB's sound component: 0.8 s, then Overwatch fades it). */
	private static final double DASH_HEAL_TIME = 0.8;

	/** What each loop is playing, by what it follows ("charge/<id>", "platform/<phase>/<id>"...). */
	private static final Map<String, SoundInstance> PLAYING = new HashMap<>();
	private static boolean dashWas;
	private static @Nullable SoundInstance dashHeal;
	private static int dashHealLeft;

	private LifeweaverSounds() {
	}

	/** Each client tick. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			PLAYING.values().forEach(s -> mc.getSoundManager().stop(s));
			PLAYING.clear();
			dashHeal = null;
			return;
		}
		Map<String, Supplier<@Nullable SoundInstance>> wanted = new HashMap<>();
		for (Player player : mc.level.players()) {
			ClientLifeweaver.State s = ClientLifeweaver.state(player);
			if (s == null || !player.isAlive()) {
				continue;
			}
			boolean own = player == mc.player;
			if (s.has(Lifeweaver.FLAG_CHARGING)) {
				wanted.put("charge/" + player.getId(), () -> own ? inHead(mc, OcSounds.LW_CHARGE_LOOP) : on(mc, OcSounds.LW_CHARGE_LOOP_3P, player, 45));
			}
			if (own && s.has(Lifeweaver.FLAG_TREE_AIM)) {
				wanted.put("preview", () -> inHead(mc, OcSounds.LW_TREE_PREVIEW));
			}
		}
		dashHeal(mc);
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof PetalThrowEntity) {
				// the platform's egg in flight, and the seed (the same sound: 4216 / 7404)
				wanted.put("flight/" + e.getId(), () -> on(mc, OcSounds.LW_PLATFORM_FLIGHT, e, 40));
			} else if (e instanceof PetalPlatformEntity platform) {
				SoundEvent bed = switch (platform.phase()) {
					case PetalPlatformEntity.ARMED -> OcSounds.LW_PLATFORM_ARMED;
					case PetalPlatformEntity.RISING -> OcSounds.LW_PLATFORM_RISE_LOOP;
					case PetalPlatformEntity.RAISED -> OcSounds.LW_PLATFORM_RAISED_LOOP;
					default -> null;
				};
				if (bed != null) {
					wanted.put("platform/" + platform.phase() + "/" + e.getId(), () -> on(mc, bed, e, 40));
				}
			} else if (e instanceof TreeOfLifeEntity tree) {
				// his own (on his own tree mixer, M), his side's, the enemies' (A099: the same file lower)
				Entity owner = mc.level.getEntity(tree.ownerId());
				SoundEvent loop = owner == mc.player ? OcSounds.LW_TREE_LOOP
					: KirikoFx.enemyOf(owner) ? OcSounds.LW_TREE_LOOP_ENEMY : OcSounds.LW_TREE_LOOP_3P;
				wanted.put("tree/" + e.getId(), () -> on(mc, loop, e, 75));
			}
		}
		LifeweaverFx.vines().forEach((ally, at) -> wanted.put("vine/" + ally, () -> at(mc, OcSounds.LW_GRIP_VINE, at, 70)));

		for (Iterator<Map.Entry<String, SoundInstance>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, SoundInstance> entry = it.next();
			if (!wanted.containsKey(entry.getKey())) {
				mc.getSoundManager().stop(entry.getValue());
				it.remove();
			}
		}
		wanted.forEach((key, make) -> {
			SoundInstance playing = PLAYING.get(key);
			if (playing != null && mc.getSoundManager().isActive(playing)) {
				return;
			}
			// (again as its file ends: the map builds each loop one loop length long)
			SoundInstance sound = make.get();
			if (sound != null) {
				mc.getSoundManager().play(sound);
				PLAYING.put(key, sound);
			}
		});
	}

	/** His dash while hurt (36A2 #321 -> 3DEB): its sound in his head, stopped 0.8 s on. */
	private static void dashHeal(Minecraft mc) {
		ClientLifeweaver.State mine = ClientLifeweaver.state(mc.player);
		boolean dashing = mine != null && mine.has(Lifeweaver.FLAG_DASH);
		if (dashing && !dashWas && mc.player.getHealth() < mc.player.getMaxHealth()) {
			if (dashHeal != null) {
				mc.getSoundManager().stop(dashHeal);
			}
			dashHeal = inHead(mc, OcSounds.LW_DASH_HEAL);
			if (dashHeal != null) {
				mc.getSoundManager().play(dashHeal);
				dashHealLeft = dev.overcraft.Ow.ticks(DASH_HEAL_TIME);
			}
		}
		dashWas = dashing;
		if (dashHeal != null && --dashHealLeft <= 0) {
			mc.getSoundManager().stop(dashHeal);
			dashHeal = null;
		}
	}

	/** Null without the local pack (the event names no file). */
	private static boolean available(Minecraft mc, SoundEvent event) {
		var registered = mc.getSoundManager().getSoundEvent(event.location());
		return registered != null && registered.getSound(SoundInstance.createUnseededRandom()) != null;
	}

	private static @Nullable SoundInstance inHead(Minecraft mc, SoundEvent event) {
		return available(mc, event) ? new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1,
			SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true) : null;
	}

	/** On an entity, heard out to {@code metres} (a volume over 1 only widens Minecraft's 16 blocks). */
	private static @Nullable SoundInstance on(Minecraft mc, SoundEvent event, Entity entity, double metres) {
		return available(mc, event) ? new EntityBoundSoundInstance(event, SoundSource.PLAYERS, (float) Math.max(1, metres / 16), 1,
			entity, entity.getRandom().nextLong()) : null;
	}

	private static @Nullable SoundInstance at(Minecraft mc, SoundEvent event, Vec3 at, double metres) {
		return available(mc, event) ? new SimpleSoundInstance(event, SoundSource.PLAYERS, (float) Math.max(1, metres / 16), 1,
			SoundInstance.createUnseededRandom(), at.x, at.y, at.z) : null;
	}
}

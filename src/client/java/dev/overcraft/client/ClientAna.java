package dev.overcraft.client;

import dev.overcraft.ana.Ana;
import dev.overcraft.ana.AnaStats;
import dev.overcraft.hero.HeroManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Ana on the client: her synced state (HeroManager.ANA), read for every Ana (her scoped animations), and for the local
 * one Take Aim's view: the zoom to 30° over the scope's 0.25 s, and the aim at 30% sensitivity fully scoped (the
 * option's default: D v8174), in first person only (third person's scope camera isn't built: ours).
 */
public final class ClientAna {
	/** An Ana's synced state, parsed. */
	public record State(int flags, float scope) {
		public boolean has(int flag) {
			return (flags & flag) != 0;
		}
	}

	private ClientAna() {
	}

	public static @Nullable State state(@Nullable Entity entity) {
		List<Integer> s = entity == null ? null : entity.getAttached(HeroManager.ANA);
		if (s == null || s.size() < 2) {
			return null;
		}
		return new State(s.get(0), s.get(1) / 100f);
	}

	/** How far the local player is scoped in first person (0-1), 0 when not Ana or not in first person. */
	private static float viewScope() {
		Minecraft mc = Minecraft.getInstance();
		State s = state(mc.player);
		return s == null || !mc.options.getCameraType().isFirstPerson() || !ClientHero.heroView() ? 0 : s.scope();
	}

	/** The field of view's multiplier: from the player's own setting toward 30° as the scope comes up. */
	public static float fovModifier() {
		float scope = viewScope();
		if (scope <= 0) {
			return 1;
		}
		float fov = Minecraft.getInstance().options.fov().get();
		return 1 - scope * (1 - (float) AnaStats.SCOPED_FOV / Math.max(fov, 1));
	}

	/** The mouse's turn while fully scoped: 30%. */
	public static double sensitivity() {
		State s = state(Minecraft.getInstance().player);
		return s != null && s.has(Ana.FLAG_SCOPED) && viewScope() >= 1 ? AnaStats.SCOPED_SENSITIVITY : 1;
	}
}

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

	/** The last scope seen, for scopeMoving. */
	private static float lastScope;

	/**
	 * The local player's scope is up or moving in their own first person (or just went down): the zoom then follows it
	 * tick by tick, not eased (ScopeFovMixin).
	 */
	public static boolean scopeMoving() {
		float scope = viewScope();
		boolean moving = scope > 0 || lastScope > 0;
		lastScope = scope;
		return moving;
	}

	/** How far the local player is scoped in their own first person (0-1): the scope overlay's and view model's (#90). */
	public static float ownScope() {
		return viewScope();
	}

	private static float eased;
	private static long easedAt;

	/** ownScope, moved at the scope's own speed between the sync's ticks (0.2 a tick left the overlay's fade two steps). */
	public static float ownScopeEased() {
		long now = net.minecraft.util.Util.getMillis();
		float dt = Math.clamp((now - easedAt) / 1000f, 0, 0.1f);
		easedAt = now;
		float target = viewScope();
		float step = dt * (target >= eased ? (float) (1 / AnaStats.SCOPE_IN) : (float) (1 / AnaStats.SCOPE_OUT));
		eased = target <= 0 && step > eased ? 0 : eased + Math.clamp(target - eased, -step, step);
		return eased;
	}

	/**
	 * The field of view's multiplier: from the player's own setting toward 30° as the scope comes up. Vertical degrees, as
	 * Minecraft's: the data's v172 = 30 (0B94 #83 -> the camera's #84) is vertical too. Overwatch keeps the vertical view
	 * and widens the horizontal with the screen (Hor+), so its engine's angles are vertical: its 103 setting is horizontal
	 * at 16:9 (70.5 vertical), and the community's measured scoped view for Ana and Widowmaker is 51 degrees horizontal at
	 * 16:9, which is exactly 30 vertical (2 atan(tan 15 x 16/9) = 50.9) (#90: checked against the "17 vertical if 30 were
	 * horizontal" reading, which would zoom twice as far as the game).
	 */
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

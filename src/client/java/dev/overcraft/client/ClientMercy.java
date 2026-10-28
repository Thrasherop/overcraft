package dev.overcraft.client;

import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.mercy.Mercy;
import dev.overcraft.mercy.MercyStats;
import dev.overcraft.net.HeroInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Mercy on the client: her synced state (HeroManager.MERCY), read for every Mercy (her beam, her poses), and for the
 * local one her movement's inputs: Guardian Angel's flight and launches, Angelic Descent, and her weapon keys
 * (Overwatch's 1 / 2 and the scroll wheel, which hero mode keeps from the hotbar).
 */
public final class ClientMercy {
	/** A Mercy's synced state, parsed. */
	public record State(int flags, int gaTarget, @Nullable Vec3 gaTo, int beamMode, int beamTarget, List<Integer> chain) {
		public boolean has(int flag) {
			return (flags & flag) != 0;
		}
	}

	/** The launch she made this Guardian Angel (until the server's flight flag goes): no more gliding. */
	private static boolean launched;
	/** Her momentum meter as her client counts it: ticks of this Guardian Angel so far (the synced one lags a tick or two). */
	private static int flown;
	private static boolean jumpWas;
	private static boolean crouchWas;
	private static int scrolled;

	private ClientMercy() {
	}

	public static @Nullable State state(@Nullable Entity entity) {
		List<Integer> s = entity == null ? null : entity.getAttached(HeroManager.MERCY);
		if (s == null || s.size() < 7) {
			return null;
		}
		Vec3 to = s.get(1) == -1 ? null : new Vec3(s.get(2) / 100.0, s.get(3) / 100.0, s.get(4) / 100.0);
		return new State(s.get(0), s.get(1), to, s.get(5), s.get(6), s.subList(7, s.size()));
	}

	private static @Nullable State local() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player == null || ClientHero.hero() != Hero.MERCY ? null : state(mc.player);
	}

	/** In hero mode the scroll wheel picks her other weapon (MouseHandlerMixin). */
	public static void scroll(double amount) {
		if (ClientHero.hero() == Hero.MERCY && amount != 0) {
			scrolled++;
		}
	}

	/** Valkyrie's flight is on (after its cast). */
	public static boolean valkyrie(Entity entity) {
		State s = entity == Minecraft.getInstance().player ? local() : state(entity);
		return s != null && s.has(Mercy.FLAG_VALKYRIE);
	}

	/** Guardian Angel's move for the local Mercy: to the point the server fixed as she set off (beside the ally). */
	public static OwMovement.@Nullable Glide glide(Entity entity) {
		State s = local();
		if (entity != Minecraft.getInstance().player || s == null || !s.has(Mercy.FLAG_GA) || launched || s.gaTo() == null) {
			return null;
		}
		double speed = s.has(Mercy.FLAG_VALKYRIE) ? MercyStats.GA_SPEED_VALKYRIE : MercyStats.GA_SPEED;
		// a little inside the arrival distance, so the server (which sees where her client put her) counts it arrived
		return new OwMovement.Glide(s.gaTo(), speed, MercyStats.GA_ARRIVE * 0.9);
	}

	/** Angelic Descent: Jump or Crouch held in the air, not flying. */
	public static boolean descent(Entity entity) {
		State s = local();
		return entity == Minecraft.getInstance().player && s != null && !entity.onGround()
			&& !s.has(Mercy.FLAG_VALKYRIE) && !s.has(Mercy.FLAG_GA)
			&& (HeroControls.get(Action.JUMP).isDown() || HeroControls.get(Action.CROUCH).isDown());
	}

	/** The momentum meter, 0-1: full after 1 s of Guardian Angel (0.68 s in Valkyrie), counted from her flight's start. */
	private static double momentum(State s) {
		double fill = MercyStats.GA_MOMENTUM_FILL * (s.has(Mercy.FLAG_VALKYRIE) ? MercyStats.GA_SPEED / MercyStats.GA_SPEED_VALKYRIE : 1);
		return Math.min(1, flown / (fill * 20));
	}

	/**
	 * Each tick in hero mode: her extra presses for the server (HeroInputPayload.EXTRA_*). A Jump or Crouch press in
	 * Guardian Angel's flight, or in the 0.35 s after she arrives, launches her here at once, as her graph 006E does
	 * (kit_details.md 2): (11 + 6 m) m/s by the momentum m built, x1.3 in Valkyrie. Jump: the way she looks plus
	 * 0.5 + 0.1 m up, x(1 + 0.2 m |pitch| / 89); back held reverses the look and is 20% slower. Crouch: straight up,
	 * x(1 + 0.2 m) (20.4 m/s full, 26.5 in Valkyrie). Her velocity is replaced by the launch.
	 */
	public static int input(LocalPlayer player) {
		State s = local();
		boolean jump = HeroControls.get(Action.JUMP).isDown();
		boolean crouch = HeroControls.get(Action.CROUCH).isDown();
		boolean jumpPressed = jump && !jumpWas;
		boolean crouchPressed = crouch && !crouchWas;
		jumpWas = jump;
		crouchWas = crouch;
		int extra = 0;
		while (HeroControls.get(Action.WEAPON_1).consumeClick()) {
			extra |= HeroInputPayload.EXTRA_STAFF;
		}
		while (HeroControls.get(Action.WEAPON_2).consumeClick()) {
			extra |= HeroInputPayload.EXTRA_BLASTER;
		}
		if (scrolled > 0) {
			extra |= HeroInputPayload.EXTRA_SWAP;
			scrolled = 0;
		}
		if (s == null) {
			launched = false;
			return 0;
		}
		if (!s.has(Mercy.FLAG_GA) && !s.has(Mercy.FLAG_GA_WINDOW)) {
			launched = false;
			flown = 0;
		} else if (!launched && (jumpPressed || crouchPressed)) {
			double m = momentum(s);
			double speed = (MercyStats.GA_LAUNCH_MIN + MercyStats.GA_LAUNCH_MOMENTUM * m)
				* (s.has(Mercy.FLAG_VALKYRIE) ? MercyStats.GA_LAUNCH_VALKYRIE : 1);
			// move input back (her graph's s = -1): the look reversed, 20% slower, for either launch
			boolean back = HeroControls.get(Action.MOVE_BACK).isDown() && !HeroControls.get(Action.MOVE_FORWARD).isDown();
			double sign = back ? -1 : 1;
			Vec3 v;
			if (crouchPressed) {
				v = new Vec3(0, speed * (1 + MercyStats.GA_LAUNCH_PITCH * m), 0);
				extra |= HeroInputPayload.EXTRA_CROUCH;
			} else {
				Vec3 aim = player.getLookAngle();
				double pitch = Math.min(89, Math.abs(player.getXRot()));
				speed *= 1 + MercyStats.GA_LAUNCH_PITCH * m * pitch / 89;
				Vec3 dir = new Vec3(sign * aim.x, sign * aim.y + MercyStats.GA_LAUNCH_UP + MercyStats.GA_LAUNCH_UP_MOMENTUM * m,
					sign * aim.z).normalize();
				v = dir.scale(speed);
				extra |= HeroInputPayload.EXTRA_JUMP;
			}
			if (back) {
				v = v.scale(MercyStats.GA_LAUNCH_BACK);
				extra |= HeroInputPayload.EXTRA_BACK;
			}
			player.setDeltaMovement(v.scale(1 / 20.0));
			launched = true;
		} else if (!launched) {
			flown++;
		}
		return extra;
	}
}

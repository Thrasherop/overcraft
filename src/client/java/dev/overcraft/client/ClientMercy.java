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

	/** Guardian Angel's move for the local Mercy: to her ally (followed here) or the soul. */
	public static OwMovement.@Nullable Glide glide(Entity entity) {
		State s = local();
		if (entity != Minecraft.getInstance().player || s == null || !s.has(Mercy.FLAG_GA) || launched) {
			return null;
		}
		Vec3 to = s.gaTo();
		if (s.gaTarget() >= 0 && entity.level().getEntity(s.gaTarget()) instanceof net.minecraft.world.entity.LivingEntity ally) {
			to = ally.position().add(0, ally.getBbHeight() * 0.5, 0);
		}
		if (to == null) {
			return null;
		}
		double speed = s.has(Mercy.FLAG_VALKYRIE) ? MercyStats.GA_SPEED_VALKYRIE : MercyStats.GA_SPEED;
		return new OwMovement.Glide(to, speed, MercyStats.GA_ARRIVE * 0.8);
	}

	/** Angelic Descent: Jump or Crouch held in the air, not flying. */
	public static boolean descent(Entity entity) {
		State s = local();
		return entity == Minecraft.getInstance().player && s != null && !entity.onGround()
			&& !s.has(Mercy.FLAG_VALKYRIE) && !s.has(Mercy.FLAG_GA)
			&& (HeroControls.get(Action.JUMP).isDown() || HeroControls.get(Action.CROUCH).isDown());
	}

	/** The time Guardian Angel has flown (the momentum meter: the server's, as synced). */
	private static double momentum() {
		return ClientHero.sync().meter();
	}

	/**
	 * Each tick in hero mode: her extra presses for the server (HeroInputPayload.EXTRA_*). A Jump or Crouch press in
	 * Guardian Angel's flight launches her here at once: Jump the way she looks at 11-17 m/s by the momentum built (back
	 * held: away from her target, 20% slower), Crouch straight up at 20.4 m/s (26.5 in Valkyrie).
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
		if (!s.has(Mercy.FLAG_GA)) {
			launched = false;
		} else if (!launched && (jumpPressed || crouchPressed)) {
			boolean valkyrie = s.has(Mercy.FLAG_VALKYRIE);
			Vec3 v;
			if (crouchPressed) {
				v = new Vec3(0, valkyrie ? MercyStats.GA_SUPER_JUMP_VALKYRIE : MercyStats.GA_SUPER_JUMP, 0);
				extra |= HeroInputPayload.EXTRA_CROUCH;
			} else {
				double speed = MercyStats.GA_LAUNCH_MIN + (MercyStats.GA_LAUNCH_MAX - MercyStats.GA_LAUNCH_MIN) * momentum();
				Vec3 dir = player.getLookAngle();
				boolean back = HeroControls.get(Action.MOVE_BACK).isDown();
				if (back && s.gaTo() != null) {
					Vec3 away = player.position().subtract(s.gaTo());
					dir = new Vec3(away.x, 0, away.z).normalize();
					speed *= MercyStats.GA_LAUNCH_BACK;
					extra |= HeroInputPayload.EXTRA_BACK;
				}
				// ours: at least 15° up, a launch rather than a dive (the data's direction isn't decoded)
				double flat = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
				double up = Math.max(dir.y, Math.tan(Math.toRadians(15)) * flat);
				dir = new Vec3(dir.x, up, dir.z).normalize();
				v = dir.scale(speed);
				extra |= HeroInputPayload.EXTRA_JUMP;
			}
			player.setDeltaMovement(v.scale(1 / 20.0));
			launched = true;
		}
		return extra;
	}
}

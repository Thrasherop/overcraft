package dev.overcraft.client;

import dev.overcraft.Ow;
import dev.overcraft.hero.Hero;
import dev.overcraft.pharah.Pharah;
import dev.overcraft.pharah.PharahStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Pharah on her own client: her Hover Jets and her jets move her here (OwMovement.pharahHover; the jets' launches), the server
 * owning her fuel and cooldowns (the sync's meter and mode: dev.overcraft.pharah.Pharah). Her client says when she hovers
 * (HeroInputPayload.HELD_HOVER), for the server to spend the fuel.
 */
public final class ClientPharah {
	/** Ticks the jump key has been held since a press on the ground (the hover takes over 0.2 s after it: PharahStats.HOVER_FROM_GROUND). */
	private static int groundHeld;
	/** The jump key's press was on the ground (else in the air: the hover at once). */
	private static boolean groundPress;
	/** Barrage cast in the air: she flies it (PharahStats.BARRAGE_UP), set on its first tick. */
	private static boolean barrageFlight;
	private static boolean barrageWas;
	/** The hover this tick (Pharah.HOVER_*), worked out once a tick. */
	private static int hover;
	private static long hoverTick = Long.MIN_VALUE;
	/** The jets' mode bits last tick, for a new one's first tick. */
	private static int jetsWas;

	private ClientPharah() {
	}

	public static void reset() {
		groundHeld = 0;
		groundPress = false;
		barrageFlight = false;
		barrageWas = false;
		hover = 0;
		hoverTick = Long.MIN_VALUE;
		jetsWas = 0;
	}

	private static boolean mine(LivingEntity entity) {
		return entity == Minecraft.getInstance().player && ClientHero.hero() == Hero.PHARAH && live(Minecraft.getInstance().player);
	}

	/** Her keys move her: hero mode, and not asleep, stunned, knocked down or held by Gravitic Flux (the synced ASLEEP, as KeyboardInputMixin's block): the hover stops with them (W: sleep and stun interrupt her; critic_1 #1). */
	private static boolean live(LocalPlayer player) {
		return player != null && ClientHero.controls() && !Boolean.TRUE.equals(player.getAttached(dev.overcraft.hero.HeroManager.ASLEEP));
	}

	/** Each client tick: a jet that just started launches her (D 0027 / 4B92 #36). */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || ClientHero.hero() != Hero.PHARAH || !live(player)) {
			jetsWas = 0;
			groundHeld = 0;
			groundPress = false;
			barrageFlight = barrageWas = false;
			return;
		}
		boolean barrage = (ClientHero.sync().mode() & Pharah.MODE_BARRAGE) != 0;
		if (barrage && !barrageWas) {
			// Barrage cast in the air: her speed zeroed, then she flies it (WS 2026-11-11: vy -4.81 -> 0 at the cast)
			barrageFlight = !player.onGround();
			if (barrageFlight) {
				player.setDeltaMovement(Vec3.ZERO);
			}
		} else if (!barrage) {
			barrageFlight = false;
		}
		barrageWas = barrage;
		int jets = ClientHero.sync().mode() & (Pharah.MODE_JUMP_JET | Pharah.MODE_JET_DASH);
		int fresh = jets & ~jetsWas;
		jetsWas = jets;
		if (fresh == 0) {
			return;
		}
		Vec3 v = player.getDeltaMovement();
		Vec3 along = inputDirection(player);
		if ((fresh & Pharah.MODE_JUMP_JET) != 0) {
			// 17 m/s up (a fall cancelled, a rise kept a tenth: D x57643451, M) and 7 along the move input, none without
			double up = Ow.speed(PharahStats.JUMP_JET_UP) + Math.max(0, v.y) * 0.1;
			Vec3 add = along == null ? Vec3.ZERO : along.scale(Ow.speed(PharahStats.JUMP_JET_ALONG));
			player.setDeltaMovement(v.x + add.x, up, v.z + add.z);
		} else {
			// 19 m/s along the move input (forward without) and 8 up (D 4B92 #36 / #545), plus 0.3 of her horizontal velocity
			// from before (WS 2026-11-11: PharahStats.JET_DASH_KEEP)
			Vec3 dir = along != null ? along : forward(player);
			Vec3 dash = dir.scale(Ow.speed(PharahStats.JET_DASH_ALONG)).add(new Vec3(v.x, 0, v.z).scale(PharahStats.JET_DASH_KEEP));
			player.setDeltaMovement(dash.x, Ow.speed(PharahStats.JET_DASH_UP), dash.z);
		}
		player.resetFallDistance();
	}

	private static Vec3 forward(LocalPlayer player) {
		double yaw = Math.toRadians(player.getYRot());
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	/** Her move keys as a level direction in the world, or null with none held. */
	private static Vec3 inputDirection(LocalPlayer player) {
		Vec2 move = player.input.getMoveVector();
		if (move.lengthSquared() < 1.0E-6f) {
			return null;
		}
		double yaw = Math.toRadians(player.getYRot());
		Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		return fwd.scale(move.y).add(left.scale(move.x)).normalize();
	}

	/** OwMovement.pharahHover: her hover this tick. */
	public static int hover(LivingEntity entity) {
		if (!mine(entity)) {
			return Pharah.HOVER_NONE;
		}
		long tick = entity.level().getGameTime();
		if (tick == hoverTick) {
			return hover;
		}
		hoverTick = tick;
		Minecraft mc = Minecraft.getInstance();
		dev.overcraft.net.HeroSyncPayload s = ClientHero.sync();
		boolean fuel = s.meter() > 0.001f;
		boolean barrage = (s.mode() & Pharah.MODE_BARRAGE) != 0;
		boolean jet = (s.mode() & (Pharah.MODE_JUMP_JET | Pharah.MODE_JET_DASH)) != 0;
		boolean held = dev.overcraft.client.controls.HeroControls.get(dev.overcraft.client.controls.HeroControls.Action.JUMP).isDown() && mc.gui.screen() == null;
		// counted here, the tick's first ask (from travel, before the move): the press tick still sees her on the ground
		if (!held) {
			groundHeld = 0;
			groundPress = false;
		} else if (groundHeld++ == 0) {
			groundPress = entity.onGround();
		}
		// from a ground press 0.2 s from the press, airborne or not; from an air press at once (WS 2026-11-11: HOVER_FROM_GROUND)
		boolean engaged = held && (!groundPress || groundHeld > Ow.ticks(PharahStats.HOVER_FROM_GROUND));
		if (jet) {
			// a jet gives the hover only with Jump held, spending no fuel (D 0025 #29; WS 2026-11-11: none without the key)
			hover = !held ? Pharah.HOVER_NONE : fuel ? Pharah.HOVER_JET : Pharah.HOVER_JET_EMPTY;
		} else if (engaged && !dev.overcraft.hero.HeroManager.jumpIsNotAirborne(entity)) {
			// (not where Jump is Minecraft's or the mount's: water, lava, a cobweb, a ladder, powder snow, a boat or a horse:
			// critic_1 #2 / #3; ours, Overwatch has none of them)
			hover = fuel || barrage ? Pharah.HOVER_FUEL : Pharah.HOVER_EMPTY;
		} else {
			hover = Pharah.HOVER_NONE;
		}
		return hover;
	}

	/** OwMovement.pharahBarrage: she flies her Barrage (cast in the air). */
	public static boolean barrageFlight(LivingEntity entity) {
		return mine(entity) && barrageFlight;
	}

	/** Her held input: HELD_HOVER while the hover holds her (its fuel spent on the server). */
	public static int held(LocalPlayer player) {
		int h = hover(player);
		return h == Pharah.HOVER_FUEL || h == Pharah.HOVER_EMPTY ? dev.overcraft.net.HeroInputPayload.HELD_HOVER : 0;
	}
}

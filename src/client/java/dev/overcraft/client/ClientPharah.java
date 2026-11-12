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
	/** Ticks the jump key has been held on the ground (the hover starts there after 0.2 s: D 0025 #22, M). */
	private static int groundHeld;
	/** The hover this tick (Pharah.HOVER_*), worked out once a tick. */
	private static int hover;
	private static long hoverTick = Long.MIN_VALUE;
	/** The jets' mode bits last tick, for a new one's first tick. */
	private static int jetsWas;

	private ClientPharah() {
	}

	public static void reset() {
		groundHeld = 0;
		hover = 0;
		hoverTick = Long.MIN_VALUE;
		jetsWas = 0;
	}

	private static boolean mine(LivingEntity entity) {
		return entity == Minecraft.getInstance().player && ClientHero.hero() == Hero.PHARAH && ClientHero.controls();
	}

	/** Each client tick: a jet that just started launches her (D 0027 / 4B92 #36). */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || ClientHero.hero() != Hero.PHARAH || !ClientHero.controls()) {
			jetsWas = 0;
			groundHeld = 0;
			return;
		}
		int jets = ClientHero.sync().mode() & (Pharah.MODE_JUMP_JET | Pharah.MODE_JET_DASH);
		int fresh = jets & ~jetsWas;
		jetsWas = jets;
		if (fresh == 0) {
			groundHeld = dev.overcraft.client.controls.HeroControls.get(dev.overcraft.client.controls.HeroControls.Action.JUMP).isDown() && player.onGround() ? groundHeld + 1 : 0;
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
			// 19 m/s along the move input (forward without) and 8 up, in place of what she was doing (D 4B92 #36 / #545)
			Vec3 dir = along != null ? along : forward(player);
			Vec3 dash = dir.scale(Ow.speed(PharahStats.JET_DASH_ALONG));
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
		boolean engaged = held && (!entity.onGround() || groundHeld >= Ow.ticks(PharahStats.HOVER_FROM_GROUND));
		if (jet) {
			// the jets turn the hover on, key or not, and spend no fuel (D 0025 #698 / #694 / #29)
			hover = fuel ? Pharah.HOVER_JET : Pharah.HOVER_JET_EMPTY;
		} else if (engaged && !entity.isInWater()) {
			hover = fuel || barrage ? Pharah.HOVER_FUEL : Pharah.HOVER_EMPTY;
		} else {
			hover = Pharah.HOVER_NONE;
		}
		return hover;
	}

	/** Her held input: HELD_HOVER while the hover holds her (its fuel spent on the server). */
	public static int held(LocalPlayer player) {
		int h = hover(player);
		return h == Pharah.HOVER_FUEL || h == Pharah.HOVER_EMPTY ? dev.overcraft.net.HeroInputPayload.HELD_HOVER : 0;
	}
}

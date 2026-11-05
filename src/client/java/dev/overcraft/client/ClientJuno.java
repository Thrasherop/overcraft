package dev.overcraft.client;

import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Perk;
import dev.overcraft.juno.Juno;
import dev.overcraft.juno.JunoStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Juno on her own client: her Martian Overboots and Glide Boost move her here (OwMovement's airJumps / junoGlide), the
 * server starting and ending the glide (Juno: the synced mode's glide flag).
 */
public final class ClientJuno {
	/** Ticks into her glide as this client moves her (-1 not gliding). */
	private static int glide = -1;

	private ClientJuno() {
	}

	/** Each client tick: the glide follows the server's flag. */
	public static void tick(Minecraft mc) {
		boolean on = mc.player != null && ClientHero.hero() == Hero.JUNO && ClientHero.controls()
			&& (ClientHero.sync().mode() & Juno.MODE_GLIDE) != 0;
		glide = on ? glide + 1 : -1;
	}

	public static int glide(LivingEntity entity) {
		return entity == Minecraft.getInstance().player && ClientHero.hero() == Hero.JUNO && ClientHero.controls() ? glide : -1;
	}

	/** When her double jump last went off (game time, -1 told: OwMovement), for the server to tell the others (its 3p copy). */
	private static long jumped = -1;

	/** OwMovement's double jump: hers sounds in her head at once (JunoSounds), the server told with her next input. */
	public static void doubleJumped(LivingEntity entity) {
		if (entity == Minecraft.getInstance().player) {
			jumped = entity.level().getGameTime();
			JunoSounds.doubleJumped(Minecraft.getInstance());
		}
	}

	/** Her extra input: EXTRA_JUMP once after her double jump. */
	public static int input(net.minecraft.client.player.LocalPlayer player) {
		if (jumped < 0) {
			return 0;
		}
		// (not sent while a screen held her input back, found playing: the jump told late, long after)
		boolean fresh = player.level().getGameTime() - jumped <= 2;
		jumped = -1;
		return fresh && ClientHero.hero() == Hero.JUNO ? dev.overcraft.net.HeroInputPayload.EXTRA_JUMP : 0;
	}

	/** Her jumps in the air: one, two with Lift Off; none for anyone else. */
	public static int airJumps(LivingEntity entity) {
		if (entity != Minecraft.getInstance().player || ClientHero.hero() != Hero.JUNO || !ClientHero.controls()) {
			return 0;
		}
		return Perk.byIndex(ClientHero.sync().majorPerk()) == Perk.JUNO_LIFT_OFF ? JunoStats.AIR_JUMPS_LIFT_OFF : JunoStats.AIR_JUMPS;
	}

	/** How far her gun's screen reads what she aims at (ours: her Mediblaster's long range). */
	private static final double FACE_RANGE = 40;
	private static long faceTick = -1;
	private static boolean faceAngry;

	/**
	 * Her gun's screen: happy on an ally or nothing, angry on an enemy (the wiki, tools/reference/juno.md; the two faces are the green and
	 * blue channels of its image 4E48C, chosen by no node read, so ours from her aim), checked once a tick.
	 */
	public static boolean faceAngry() {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		net.minecraft.client.player.LocalPlayer player = mc.player;
		if (player == null) {
			return false;
		}
		long tick = player.level().getGameTime();
		if (tick != faceTick) {
			faceTick = tick;
			net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
			net.minecraft.world.phys.Vec3 to = eye.add(player.getViewVector(1).scale(FACE_RANGE));
			net.minecraft.world.phys.AABB box = player.getBoundingBox().expandTowards(player.getViewVector(1).scale(FACE_RANGE)).inflate(1);
			net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, eye, to, box,
				e -> e instanceof net.minecraft.world.entity.LivingEntity && !e.isSpectator(), FACE_RANGE * FACE_RANGE);
			faceAngry = hit != null && dev.overcraft.hero.Allies.isEnemy(player, hit.getEntity())
				&& player.hasLineOfSight(hit.getEntity());
		}
		return faceAngry;
	}
}

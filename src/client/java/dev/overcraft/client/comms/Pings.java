package dev.overcraft.client.comms;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.HeroHud;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.hud.HudShapes;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.PingPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.image;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * Pings, the client's half (#40; the owner's spec and Overwatch 2 screenshots, tools/reference/comms-reference.md).
 * The ping key (Overwatch's PC default, the middle button) pings what the crosshair is on, out to 120 m: a spot gets a
 * small blue diamond with its distance; a second press within 0.4 s makes it a warning (a red circle with a danger
 * mark); an enemy gets a red ring that follows them, their name under it. Your own ping has a leader line to
 * "CANCEL [key]"; pressing the key while aiming at it takes it down. Teammates see them (comms.Pings on the server).
 */
public final class Pings {
	/** How long a ping lasts (ms, ours: Overwatch gives no number). */
	private static final long LIFE_MS = 6500;
	/** A second press this soon is a warning (ms, ours). */
	private static final long DOUBLE_MS = 400;
	private static final double REACH = dev.overcraft.comms.Pings.REACH;
	private static final int BLUE = 0xFF29C4F5;
	private static final int RED = 0xFFE8384F;

	private record Ping(int owner, int kind, Vec3 at, int entity, long until) {
	}

	private static final List<Ping> PINGS = new ArrayList<>();
	private static long lastPress = -10000;
	/** The spot just pinged, for a quick second press to make a warning of it. */
	private static @Nullable Vec3 lastSpot;

	private Pings() {
	}

	/** A ping from the server (a teammate's or the player's own), or a cancel. */
	public static void receive(PingPayload ping) {
		PINGS.removeIf(p -> p.owner == ping.owner());
		if (ping.kind() != PingPayload.CANCEL) {
			PINGS.add(new Ping(ping.owner(), ping.kind(), new Vec3(ping.x(), ping.y(), ping.z()), ping.entity(),
				Util.getMillis() + LIFE_MS));
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null && mc.player != null) {
				// ours: a short chime (Overwatch's ping sound isn't in the pack yet)
				mc.level.playLocalSound(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(),
					net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.PLAYERS,
					ping.kind() == PingPayload.WARNING ? 0.5f : 0.35f, ping.kind() == PingPayload.WARNING ? 0.8f : 1.6f, false);
			}
		}
	}

	/** Each client tick in hero mode: the ping key. */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		boolean pressed = false;
		while (HeroControls.get(Action.PING).consumeClick()) {
			pressed = true;
		}
		if (!pressed || player == null || mc.level == null || !ClientPlayNetworking.canSend(PingPayload.TYPE)) {
			return;
		}
		long now = Util.getMillis();
		// a second press soon after placing a spot: that spot, as a warning (Overwatch's double click)
		if (lastSpot != null && now - lastPress < DOUBLE_MS) {
			send(PingPayload.WARNING, lastSpot, -1);
			lastSpot = null;
			lastPress = -10000;
			return;
		}
		lastPress = now;
		lastSpot = null;
		Ping mine = own(player);
		// aiming at your own ping: take it down
		if (mine != null && aimedAt(player, mine.at)) {
			send(PingPayload.CANCEL, mine.at, -1);
			return;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1).scale(REACH));
		HitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 reach = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, reach, new AABB(eye, reach).inflate(1),
			e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player, eye.distanceToSqr(reach));
		if (hit != null && Allies.isEnemy(player, hit.getEntity())) {
			send(PingPayload.ENEMY, hit.getEntity().position(), hit.getEntity().getId());
		} else if (block.getType() != HitResult.Type.MISS || hit != null) {
			lastSpot = hit != null ? hit.getLocation() : block.getLocation();
			send(PingPayload.SPOT, lastSpot, -1);
		}
	}

	private static void send(int kind, Vec3 at, int entity) {
		ClientPlayNetworking.send(new PingPayload(-1, kind, at.x, at.y, at.z, entity));
	}

	private static @Nullable Ping own(LocalPlayer player) {
		for (Ping ping : PINGS) {
			if (ping.owner == player.getId()) {
				return ping;
			}
		}
		return null;
	}

	/** The crosshair is within a few degrees of the spot (ours: 4°). */
	private static boolean aimedAt(LocalPlayer player, Vec3 at) {
		Vec3 to = at.subtract(player.getEyePosition());
		return to.lengthSqr() > 1.0E-4 && to.normalize().dot(player.getViewVector(1)) > Math.cos(Math.toRadians(4));
	}

	/** Drawn over the view in HeroHud's reference pixels. */
	public static void render(GuiGraphicsExtractor g, float w, float h) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			return;
		}
		long now = Util.getMillis();
		for (Iterator<Ping> it = PINGS.iterator(); it.hasNext(); ) {
			Ping ping = it.next();
			Entity target = ping.entity >= 0 ? mc.level.getEntity(ping.entity) : null;
			if (now > ping.until || ping.kind == PingPayload.ENEMY && (target == null || !target.isAlive())) {
				it.remove();
				continue;
			}
			Vec3 at = target != null ? target.position().add(0, target.getBbHeight() + 0.6, 0) : ping.at;
			float[] screen = HeroHud.project(at, w, h);
			if (screen == null) {
				continue;
			}
			float x = Math.clamp(screen[0], 40, w - 40);
			float y = Math.clamp(screen[1], 40, h - 40);
			int distance = (int) Math.round(player.getEyePosition().distanceTo(at));
			boolean mine = ping.owner == player.getId();
			if (ping.kind == PingPayload.ENEMY && target instanceof LivingEntity enemy) {
				enemyMarker(g, enemy, x, y, distance);
			} else if (ping.kind == PingPayload.WARNING) {
				image(g, HudShapes.DISC, x - 22, y - 22, 44, 44, 0xE6B0202F);
				image(g, HudShapes.RING, x - 23, y - 23, 46, 46, RED);
				HudText.draw(g, Face.BODY, "!", x, y + 9, 22, 0xFFFFFFFF, Align.CENTER);
				HudText.draw(g, Face.SMALL, distance + "m", x, y - 30, 11, 0xFFFFFFFF, Align.CENTER);
				if (mine) {
					cancelLabel(g, x + 14, y - 14, RED);
				}
			} else {
				// the blue diamond: a 15 px square turned 45° (about 22 px corner to corner)
				g.pose().pushMatrix();
				g.pose().translate(x, y);
				g.pose().rotate((float) Math.toRadians(45));
				fill(g, -7.5f, -7.5f, 15, 15, BLUE);
				outline(g, -7.5f, -7.5f, 15, 15, 1, 0xFFB9EEFF);
				g.pose().popMatrix();
				HudText.draw(g, Face.SMALL, distance + "m", x, y - 20, 11, 0xFFFFFFFF, Align.CENTER);
				if (mine) {
					cancelLabel(g, x + 10, y - 10, BLUE);
				}
			}
		}
	}

	/** An enemy: a red ring holding their hero's portrait (a mob: its name's first letter), the distance, the name. */
	private static void enemyMarker(GuiGraphicsExtractor g, LivingEntity enemy, float x, float y, int distance) {
		image(g, HudShapes.DISC, x - 27, y - 27, 54, 54, 0xE61A1015);
		Hero hero = enemy instanceof Player p ? HeroManager.hero(p) : null;
		Identifier portrait = hero == null ? null : dev.overcraft.client.HeroArt.portrait(hero);
		String name = (hero != null ? hero.displayName : enemy.getName().getString()).toUpperCase(Locale.ROOT);
		if (portrait != null) {
			image(g, portrait, x - 24, y - 24, 48, 48, 0xFFFFFFFF);
		} else {
			HudText.draw(g, Face.NUMBERS, name.substring(0, 1), x, y + 12, 30, 0xFFFFFFFF, Align.CENTER);
		}
		image(g, HudShapes.RING, x - 28, y - 28, 56, 56, RED);
		HudText.draw(g, Face.SMALL, distance + "m", x, y - 34, 11, 0xFFFFFFFF, Align.CENTER);
		HudText.draw(g, Face.BODY, name, x, y + 46, 13, RED, Align.CENTER);
	}

	/** Your own ping's leader line up to the right and its "CANCEL [key]" label. */
	private static void cancelLabel(GuiGraphicsExtractor g, float x, float y, int colour) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().rotate((float) Math.toRadians(-33));
		fill(g, 0, -0.75f, 62, 1.5f, colour);
		g.pose().popMatrix();
		float lx = x + 52;
		float ly = y - 34 - 13;
		String label = Component.translatable("overcraft.ping.cancel").getString().toUpperCase(Locale.ROOT);
		String key = HeroHud.shortKey(HeroControls.get(Action.PING));
		float labelW = HudText.width(Face.BODY, label, 12);
		float keyW = HudText.width(Face.SMALL, key, 10) + 8;
		float width = 10 + labelW + 6 + keyW + 5;
		fill(g, lx, ly, width, 26, colour == RED ? 0xE6601020 : 0xE60B3A5C);
		outline(g, lx, ly, width, 26, 1, colour);
		HudText.draw(g, Face.BODY, label, lx + 10, ly + 13 + 6, 12, 0xFFFFFFFF, Align.LEFT);
		fill(g, lx + 10 + labelW + 6, ly + 4, keyW, 18, 0xF2E6EEF0);
		HudText.draw(g, Face.SMALL, key, lx + 10 + labelW + 6 + keyW / 2, ly + 13 + 5, 10, 0xFF15181C, Align.CENTER);
	}

	/** The outline's red (the ping's). */
	public static final int OUTLINE_RED = 0xE8384F;

	/** This entity is a live enemy ping's target: it's outlined in red (PingOutlineMixin). */
	public static boolean outlined(Entity entity) {
		if (PINGS.isEmpty()) {
			return false;
		}
		long now = Util.getMillis();
		for (Ping ping : PINGS) {
			if (ping.kind == PingPayload.ENEMY && ping.entity == entity.getId() && now <= ping.until) {
				return true;
			}
		}
		return false;
	}

	public static void clear() {
		PINGS.clear();
	}
}

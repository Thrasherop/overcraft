package dev.overcraft.client.hud;

import dev.overcraft.client.AbilityCamera;
import dev.overcraft.client.ClientAna;
import dev.overcraft.client.ClientLifeweaver;
import dev.overcraft.client.ClientMercy;
import dev.overcraft.client.HeroArt;
import dev.overcraft.client.HeroHud;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.effect.JetpackCatFx;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.client.hud.MobFaces.MobFace;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.image;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * Every hero's widgets at the crosshair (#85), from the HUD layers the data names: the hero's reticle container (v11821, or
 * v40842 for Jetpack Cat and Kiriko) and the centre layer (v40843), with the shared view models 0237 (a timed ability's bar),
 * 0275 (a resource's bar), 00DB (button prompts) and 00CF (a prompt on whoever it happens to), and each hero's own: 044D, 0C7A,
 * 00F3, 0258, 052B, 0277 / 008B, 0D81, 00B1, 0BA3, 072E, 0263.
 *
 * <p>The data gives what each widget binds (a fraction, a time, a portrait, a count, a button) and which layer holds it; the
 * view models themselves hold no art or layout (their .116 assets are empty at every depth: the 2026-11-02 audit). So what's
 * bound, when it shows and what prompts say (DataTool's dump-strings, build 154088) are the data's, and every shape, size, colour
 * and place here is OURS. The notes on what's what: techniques/hud/reticle.md.
 *
 * <p>All laid out in reference pixels (1080 high), the crosshair at (w / 2, h / 2). Widgets stack downward from ROW_TOP, so two
 * shown together can't overlap, and target markers keep out of what the stack held last frame (HeroHud.targetMarker: markerRoom).
 * ELIMINATED's caption is at +250 and the kill marker's strokes end within 28 of the centre (HeroHud). Drawn only in hero mode
 * (HeroHud.render returns earlier in Minecraft mode), and not while an emote holds the view.
 */
public final class ReticleHud {
	/** Where the first widget below the crosshair starts (ours: clear of the kill marker's strokes, which end at 28). */
	private static final float ROW_TOP = 46;
	/** Where the bars start, and their pitch (ours). */
	private static final float TIMER_BELOW = 92, TIMER_GAP = 30;
	private static final float TIMER_W = 180, TIMER_H = 6;
	/** Prompts' first row, below two bars (ours). The last row stays above ELIMINATED's caption, whose letters top at +237. */
	private static final float PROMPT_BELOW = 160;
	/** The stack's half width as far as a marker should care (a bar, its icon and its seconds: ours). */
	private static final float STACK_HALF = 140;
	/** How far round the scope's centre a marker may go and stay on the lens (its reach, plus the rim's margin: ours). */
	private static final float SCOPE_MARKER_ROOM = 90;

	private static final int WHITE = 0xFFFFFFFF;
	/** The edge on a target's portrait: 0252 binds the team's colours, whose values aren't in the data's text; ours, the ally cyan. */
	private static final int ALLY = 0xFF29C8F0;
	/** Mercy's beams, as her beams read: the heal gold, the boost blue (ours, from MercyFx's look). */
	private static final int BEAM_HEAL = 0xFFF5D226, BEAM_BOOST = 0xFF4FB8FF;
	private static final int STEALTH = 0xFFB46CFF;
	/** Saturated, so it reads on a barrier's own pale colour and on a bright sky (a pale blue vanished on Sigma's: the critique). */
	private static final int BARRIER = 0xFF1FA2F2;
	private static final int RAILGUN = 0xFFE8553F;
	private static final int OVERCLOCK = 0xFF7FE8FF;
	private static final int OVERCHARGED = 0xFFFFC23A;
	private static final int BLOSSOM = 0xFFFF8FC8;
	private static final int WARN = 0xFFF2A23C;
	private static final int FUEL = 0xFFF0A23C, FUEL_LOW = 0xFFFF4A4A;
	/** Behind a widget's text, so it reads on anything (ours). */
	private static final int PLATE = 0x8C000000;
	/** Crosshairs' dark edge: heavier than the "+"'s own, for shapes this thin on a bright sky (the critique, H6). */
	private static final int EDGE = 0xCC000000;

	/** The stack's cursor this frame: the next widget's top (reference pixels from the screen's top). */
	private static float y;
	/** The stack's top this frame, -1 till something's drawn; the floor for prompts (a hero's own fixed widgets in the way); the middle. */
	private static float first, promptFloor, midX;
	/** What last frame's stack held, for the target markers drawn before it: its top and bottom (0 and 0: nothing). */
	private static float roomTop, roomBottom, roomHalf;

	/** A hero's own widgets, fixed places the stack works round: Kiriko's ofuda counter, Lúcio's song circles. */
	private static final float KIRIKO_TOP = 118, KIRIKO_BOTTOM = 168, LUCIO_TOP = 160, LUCIO_BOTTOM = 222;

	private ReticleHud() {
	}

	/** The crosshair and what's round it show: in first person, and from behind while an ability holds the view (as HeroHud's). */
	public static boolean atReticle() {
		var mc = Minecraft.getInstance();
		return mc.options.getCameraType().isFirstPerson() || AbilityCamera.active() && !dev.overcraft.client.comms.EmoteCamera.active()
			&& mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_BACK;
	}

	/** One frame's widgets for this hero. */
	public static void render(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float w, float h, float partial) {
		if (!player.isAlive() || dev.overcraft.client.comms.EmoteCamera.active()) {
			roomTop = roomBottom = roomHalf = 0;
			return;
		}
		float cx = w / 2;
		float cy = h / 2;
		midX = cx;
		y = cy + ROW_TOP;
		first = -1;
		promptFloor = cy + (hero == Hero.KIRIKO ? KIRIKO_BOTTOM + 4 : hero == Hero.LUCIO ? 104 : PROMPT_BELOW);
		boolean reticle = atReticle();
		switch (hero) {
			case SOJOURN -> sojourn(g, s, cx, cy, reticle);
			case SIGMA -> sigma(g, hero, s, cx, cy, reticle);
			case BRIGITTE -> brigitte(g, player, hero, s, cx, cy, w, h, partial, reticle);
			case MERCY -> mercy(g, player, hero, s, cx, cy, reticle);
			case SOMBRA -> sombra(g, player, s, cx, cy, w, h, partial, reticle);
			case JUNO -> juno(g, player, hero, s, cx, cy, partial);
			case JETPACK_CAT -> jetpackCat(g, player, hero, s, cx, cy, reticle);
			case LIFEWEAVER -> lifeweaver(g, player, s, cx, cy, w, h, partial, reticle);
			case LUCIO -> lucio(g, hero, s, cx, cy, reticle);
			case ANA -> ana(g, player, s, w, h, partial);
			default -> {
			}
		}
		towed(g, player);
		if (JetpackCatFx.lifelineOffer(player) >= 0) {
			// a Jetpack Cat's Lifeline in reach: her graph 5A8F, 3A94 #1253: Interact, "Lifeline"; Interact takes it
			prompt(g, Action.INTERACT, "overcraft.hud.prompt.lifeline", null, null);
		}
		if (dev.overcraft.client.ClientLifeweaver.gripJumpLive(player)) {
			// held by a Lifeweaver's Life Grip: 21EE #617, 00DB: Jump, 31330 "CANCEL", from when Jump lets go (#621 -> #620 -> #614, 0.65 s)
			prompt(g, Action.JUMP, "overcraft.hud.prompt.cancel", null, null);
		}
		// what a marker keeps out of next frame (a hero's own are drawn above, and go by the last frame's)
		roomTop = roomBottom = roomHalf = 0;
		if (first >= 0) {
			roomTop = first;
			roomBottom = y;
			roomHalf = STACK_HALF;
		}
		if (hero == Hero.KIRIKO && reticle) {
			roomTop = first >= 0 ? Math.min(first, cy + KIRIKO_TOP) : cy + KIRIKO_TOP;
			roomBottom = Math.max(roomBottom, cy + KIRIKO_BOTTOM);
			roomHalf = STACK_HALF;
		} else if (hero == Hero.LUCIO && reticle) {
			roomTop = first >= 0 ? Math.min(first, cy + LUCIO_TOP) : cy + LUCIO_TOP;
			roomBottom = Math.max(roomBottom, cy + LUCIO_BOTTOM);
			roomHalf = 240;
		}
	}

	/**
	 * For a target marker of this reach centred at (x, my): the lowest centre that clears the stack, if it would land on the stack,
	 * else NaN. An ally near the aim has their marker right where the stack is: the stack wins (the critique's H5; "what wins" is ours),
	 * and the marker closes up over them, above it.
	 */
	public static float markerRoom(float x, float my, float reach, float cx) {
		if (roomBottom <= roomTop || Math.abs(x - cx) > roomHalf + reach || my - reach > roomBottom || my + reach < roomTop) {
			return Float.NaN;
		}
		return roomTop;
	}

	// ---- each hero ----

	/**
	 * Sojourn. Railgun energy, 044D in her reticle (1A23 #1179): the fraction v87 / v38252, the number 60D3 (v87, rounded up) and
	 * the Overclock state 63F4. Ours: two arcs round the crosshair filling upward, the number under them, on a plate (it was lost on
	 * her gun in first person); with Overcharged's 150 (Railgun.cap) the arcs go over in gold from the 100. Overclock's time, 0237 in
	 * her reticle (2159 #727): its 8 s (OVERCLOCK_TIME), so neither the cast before nor the tail after: activeLeft counts all three.
	 */
	private static void sojourn(GuiGraphicsExtractor g, HeroSyncPayload s, float cx, float cy, boolean reticle) {
		float energy = s.meter();
		boolean overclock = s.ultActive();
		if (reticle) {
			int colour = overclock ? OVERCLOCK : energy >= 0.999f ? WHITE : RAILGUN;
			float share = Math.clamp(energy, 0, 1);
			float over = Math.clamp((energy - 1) * 2, 0, 1);
			float r = 34;
			float sweep = 140;
			for (int side = -1; side <= 1; side += 2) {
				float low = side < 0 ? 196 : 164 - sweep;
				float high = low + sweep;
				HeroHud.arc(g, cx, cy, r + 1, 7, low, high, 0x59000000);
				// up from the bottom on both sides
				if (share > 0) {
					HeroHud.arc(g, cx, cy, r, 4, side < 0 ? low : high - sweep * share, side < 0 ? low + sweep * share : high, colour);
				}
				if (over > 0) {
					HeroHud.arc(g, cx, cy, r, 4, side < 0 ? low : high - sweep * over, side < 0 ? low + sweep * over : high, OVERCHARGED);
				}
			}
			mark(cy + 40);
			platedNumber(g, String.valueOf((int) Math.ceil(energy * 100)), cx, cy + 70, 22, over > 0 ? OVERCHARGED : overclock ? OVERCLOCK : WHITE);
			y = cy + 78;
		}
		if (overclock) {
			float tail = (float) dev.overcraft.sojourn.SojournStats.OVERCLOCK_TAIL;
			float time = (float) dev.overcraft.sojourn.SojournStats.OVERCLOCK_TIME;
			float left = Math.clamp(s.activeLeft() - tail, 0, time);
			timedBar(g, HeroArt.ultimate(Hero.SOJOURN), left / time, left, cy);
		}
	}

	/**
	 * Sigma. His barrier's health, 0275 on the centre layer (2155 #12: the bar's share C7, the health v586 in the text 5FCE, a whole
	 * number; his hero option "Experimental Barrier UI", on by default) while it's out. Gravitic Flux's aim: 0237 (1E41 #1152) and
	 * 00DB (#1283: primary, 4ED9 "CONFIRM"; #1299's secondary is also "CONFIRM" there, 1DE77, but the mod takes the left only).
	 * Kinetic Grasp's hold: its time, as the other timed abilities (the data draws none: ours). Hyper Strike's count (his perk): 0243 in the
	 * ability bar (51E0 #425: quick melee's icon and button, 01D3 the hits over 5); ours: five pips at the reticle, which is where his hands
	 * are looking, and when it's charged the key and the perk's name (25111 "Hyper Strike", which the data doesn't put on this widget).
	 */
	private static void sigma(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float cx, float cy, boolean reticle) {
		HeroSyncPayload.Widgets strike = s.widgets();
		if (reticle && strike.flag()) {
			mark(y);
			// (narrower than Brigitte's: at 22 wide the row touched his spheres in view at 720p)
			pips(g, (int) strike.left2(), dev.overcraft.sigma.SigmaStats.HYPER_STRIKE_HITS, cx, y, 14, 4);
			y += 16;
		}
		y = Math.max(y, cy + TIMER_BELOW);
		if ((s.mode() & dev.overcraft.sigma.Sigma.MODE_BARRIER_OUT) != 0) {
			resourceBar(g, HeroArt.ability(hero, "experimental_barrier"), s.meter(), String.valueOf(s.secondaryAmmo()), BARRIER);
		}
		if ((s.mode() & dev.overcraft.sigma.Sigma.MODE_GRASP) != 0) {
			timedBar(g, HeroArt.ability(hero, "kinetic_grasp"), s.activeLeft() / (float) dev.overcraft.sigma.SigmaStats.GRASP_TIME,
				s.activeLeft(), cy);
		}
		HeroSyncPayload.Widgets d = s.widgets();
		if (d.count() > 0) {
			timedBar(g, HeroArt.ultimate(hero), d.left() / d.total(), d.left(), cy);
			prompt(g, Action.SECONDARY_FIRE, "overcraft.hud.prompt.confirm", null, null);
		}
		if (reticle && d.flag() && d.left2() >= dev.overcraft.sigma.SigmaStats.HYPER_STRIKE_HITS) {
			prompt(g, Action.QUICK_MELEE, "overcraft.hud.prompt.hyper_strike", null, null);
		}
	}

	/**
	 * Brigitte. Repair Pack's charges, 00F3 in her reticle (0EDD #472: v11); Inspire, 0C7A in her reticle (12D8 #479: its icon, 7CCD
	 * its state stack v31949, 067C the allies it's healing; her hero option Inspire UI, on by default), so its icon with Inspire on
	 * her alone too; the barrier's health, 0275 on the centre layer (2152 #7) with 00DB (0EE3 #848: primary, 161DC "BASH") while it's
	 * up; Rally's time, 0237 (12BE #188); Repair Pack's target, 013C (0EDD #469).
	 */
	private static void brigitte(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy, float w,
								 float h, float partial, boolean reticle) {
		HeroSyncPayload.Widgets d = s.widgets();
		marker(g, player, d.preview(), w, h, partial, Action.ABILITY_2);
		if (reticle) {
			mark(y);
			pips(g, s.secondaryAmmo(), dev.overcraft.brigitte.BrigitteStats.REPAIR_CHARGES, cx, y);
			y += 16;
			if (d.flag()) {
				Identifier icon = HeroArt.ability(hero, "inspire");
				String count = d.count() > 0 ? String.valueOf(d.count()) : "";
				float textW = count.isEmpty() ? 0 : HudText.width(Face.NUMBERS, count, 16) + 4;
				float x = cx - (22 + textW) / 2;
				fill(g, x - 3, y - 2, 22 + textW + 6, 26, PLATE);
				if (icon != null) {
					image(g, icon, x, y, 22, 22, 0xFFF7C43A);
				}
				if (!count.isEmpty()) {
					number(g, count, x + 26 + (textW - 4) / 2, y + 19, 16, 0xFFF7C43A);
				}
				y += 28;
			}
		}
		y = Math.max(y, cy + TIMER_BELOW);
		boolean up = (s.mode() & dev.overcraft.brigitte.Brigitte.MODE_BARRIER) != 0;
		if (up) {
			float max = (float) (d.left() > 0 ? dev.overcraft.brigitte.BrigitteStats.BARRIER_HEALTH_RALLY : dev.overcraft.brigitte.BrigitteStats.BARRIER_HEALTH);
			resourceBar(g, HeroArt.ability(hero, "barrier_shield"), s.primaryAmmo() / max, String.valueOf(s.primaryAmmo()), BARRIER);
		}
		if (d.left() > 0) {
			timedBar(g, HeroArt.ultimate(hero), d.left() / d.total(), d.left(), cy);
		}
		if (up && s.ability2Cooldown() <= 0) {
			prompt(g, Action.SECONDARY_FIRE, "overcraft.hud.prompt.bash", null, null);
		}
	}

	/**
	 * Mercy. Her beam's target, 0258 in her reticle (006B #1263): 2E4B the name, 4447 the portrait, 406 the team's colours, 063E2 boost
	 * or heal, and 019C its health (006B #1315 in the table, into v7126). Resurrect's, 052B in her reticle (00CA #306, shown while it's off
	 * cooldown with a soul in reach), its text 1894B "Resurrect %1$s". Guardian Angel's momentum in flight, 0275 (006E #910: v87 / v971, her
	 * wing icon 12F3); Valkyrie's time, 0237 (10DA #486). Resurrect's cast has a panel in the data (0693, 00CA #1072) and no fraction: its
	 * bar's ours.
	 */
	private static void mercy(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy, boolean reticle) {
		ClientMercy.State m = ClientMercy.state(player);
		HeroSyncPayload.Widgets d = s.widgets();
		if (reticle) {
			if (m != null && m.beamTarget() >= 0 && player.level().getEntity(m.beamTarget()) instanceof LivingEntity beamed && beamed.isAlive()) {
				boolean boost = m.beamMode() == dev.overcraft.mercy.Mercy.BEAM_BOOST;
				panel(g, beamed, displayName(beamed), "", "", health(beamed), boost ? BEAM_BOOST : BEAM_HEAL, null, cx);
			}
			if (!d.name().isEmpty()) {
				// (067C: with more than one soul in reach, how many)
				String who = Component.translatable("overcraft.hud.prompt.resurrect", d.name()).getString().toUpperCase(Locale.ROOT)
					+ (d.count() > 1 ? " (" + d.count() + ")" : "");
				panel(g, null, who, d.face(), d.name(), -1, 0, Action.ABILITY_2, cx);
			}
		}
		y = Math.max(y, cy + TIMER_BELOW);
		if ((s.mode() & dev.overcraft.mercy.Mercy.FLAG_RESURRECT) != 0 && d.total() > 0) {
			timedBar(g, HeroArt.ability(hero, "resurrect"), d.left() / d.total(), d.left(), cy);
		}
		if ((s.mode() & dev.overcraft.mercy.Mercy.FLAG_VALKYRIE) != 0) {
			timedBar(g, HeroArt.ultimate(hero), s.activeLeft() / (float) dev.overcraft.mercy.MercyStats.VALKYRIE_TIME, s.activeLeft(), cy);
		}
		if ((s.mode() & dev.overcraft.mercy.Mercy.FLAG_GA) != 0) {
			resourceBar(g, HeroArt.ability(hero, "guardian_angel"), s.meter(), null, 0xFFF5D226);
		}
	}

	/**
	 * Sombra. Her stealth, 0277 with its ring 008B (420C #1057: its share v215 / v567): in the layer of her reticle itself (v29677.v1897,
	 * as 00A3), so it takes her crosshair's place: see crosshair. 0D81 in her reticle, two slots (614C #1798, #1831: v3544 / v11652, each
	 * Hotfix's ally v77182[n], else what she's hacking v536; the portrait, and its health 019C); the lockout, 00B1 (#2775: v6358 / v45433);
	 * Hotfix's preview 013C (#593).
	 */
	private static void sombra(GuiGraphicsExtractor g, LocalPlayer player, HeroSyncPayload s, float cx, float cy, float w, float h,
							   float partial, boolean reticle) {
		HeroSyncPayload.Widgets d = s.widgets();
		marker(g, player, d.preview(), w, h, partial, Action.PRIMARY_FIRE);
		if (reticle) {
			if (d.left() > 0 && d.total() > 0) {
				float r = 22;
				HeroHud.arc(g, cx, cy, r + 0.5f, 6, 0, 360, 0x73000000);
				HeroHud.arc(g, cx, cy, r, 3.5f, 0, 360 * Math.clamp(d.left() / d.total(), 0, 1), STEALTH);
			}
			for (int id : new int[] {d.target(), d.target2()}) {
				Entity target = id < 0 ? null : player.level().getEntity(id);
				if (target != null) {
					panel(g, target, displayName(target), "", "", target instanceof LivingEntity l ? health(l) : -1, 0, null, cx);
				}
			}
			if (d.left2() > 0) {
				mark(y);
				float share = Math.clamp(d.left2() / (float) dev.overcraft.sombra.SombraStats.HOTFIX_HACK_LOCKOUT, 0, 1);
				float bw = 90;
				fill(g, cx - bw / 2 - 1, y - 1, bw + 2, 6, 0x80000000);
				fill(g, cx - bw / 2, y, bw * share, 4, WARN);
				y += 12;
			}
		}
	}

	/**
	 * Juno. Her timed abilities, 0237: Glide Boost's on the centre layer (47B7 #641: v215 / v567, op109(v215), its icon 024D), the
	 * torpedo lock window's in her reticle (4695 #1225 / #1283). Their prompts, 00DB: the glide's own button, 1B3C0 "CANCEL" (47B7 #637);
	 * the lock's two buttons (4695 #1051, the default of the swap option v55397, off: the Mediblaster's button (the left, its Action named
	 * for Kiriko's secondary) 177F0 "FIRE", the torpedoes' (the right) 04CC "CANCEL"; Torpedoes.tick does what they say, #112).
	 */
	private static void juno(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy, float partial) {
		boolean gliding = (s.mode() & dev.overcraft.juno.Juno.MODE_GLIDE) != 0;
		boolean locking = (s.mode() & dev.overcraft.juno.Juno.MODE_LOCK) != 0;
		if (gliding) {
			float total = (float) dev.overcraft.juno.JunoStats.GLIDE_TIME;
			timedBar(g, HeroArt.ability(hero, "glide_boost"), s.activeLeft() / total, s.activeLeft(), cy);
		}
		if (locking) {
			HeroAnimator a = HeroAnimator.of(player);
			float now = (player.level().getGameTime() + partial) / 20f;
			if (a.junoLocking(now)) {
				float total = (float) dev.overcraft.juno.JunoStats.LOCK_WINDOW;
				float left = Math.clamp(total - a.since("torpedo_lock", now), 0, total);
				timedBar(g, HeroArt.ability(hero, "pulsar_torpedoes"), left / total, left, cy);
			}
		}
		if (gliding) {
			prompt(g, Action.ABILITY_1, "overcraft.hud.prompt.cancel", null, null);
		}
		if (locking) {
			prompt(g, Action.SECONDARY_FIRE, "overcraft.hud.prompt.fire", Action.PRIMARY_FIRE, "overcraft.hud.prompt.cancel");
		}
	}

	/**
	 * Jetpack Cat. Lifeline's tow, 0BA3 in her reticle (5A8F #1097: the towed ally's portrait v59143, with 019C their health, #1098, into
	 * v2390). Her fuel, 0275 on the centre layer (214F #5: 9A min(1, v154), 27A3 shown under full, 9A0 its text F7A "FUEL"). Purr's time,
	 * 0237 (5A7E #320); Catnapper's carry, 0237 (5B7B #63), and its aim's prompts, 00DB (5B7B #34: primary 2B2F6 "POUNCE", secondary
	 * 2B2F7 "CANCEL": in the mod the ult key cancels). A carried enemy has no panel: 0BA3 is Lifeline's alone.
	 */
	private static void jetpackCat(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy, boolean reticle) {
		HeroSyncPayload.Widgets d = s.widgets();
		if (d.target() >= 0 && reticle && player.level().getEntity(d.target()) instanceof LivingEntity held) {
			panel(g, held, displayName(held), "", "", health(held), 0, null, cx);
		}
		y = Math.max(y, cy + TIMER_BELOW);
		if (s.meter() < 0.999f) {
			String label = Component.translatable("overcraft.hud.prompt.fuel").getString();
			resourceBar(g, HeroArt.ability(hero, "frenetic_flight"), s.meter(), label, s.meter() < 0.2f ? FUEL_LOW : FUEL);
		}
		int phase = s.mode() & 7;
		if (phase == dev.overcraft.jetpackcat.JetpackCat.ULT_CARRY) {
			timedBar(g, HeroArt.ultimate(hero), s.activeLeft() / (float) dev.overcraft.jetpackcat.JetpackCatStats.ULT_CARRY, s.activeLeft(), cy);
		} else if (phase == dev.overcraft.jetpackcat.JetpackCat.ULT_NONE && s.activeLeft() > 0) {
			timedBar(g, HeroArt.ability(hero, "purr"), s.activeLeft() / (float) dev.overcraft.jetpackcat.JetpackCatStats.PURR_TIME, s.activeLeft(), cy);
		}
		if (phase == dev.overcraft.jetpackcat.JetpackCat.ULT_AIM) {
			prompt(g, Action.SECONDARY_FIRE, "overcraft.hud.prompt.pounce", Action.ULTIMATE, "overcraft.hud.prompt.cancel");
		}
	}

	/**
	 * Lifeweaver. Healing Blossom's charge, 072E in his reticle (21C6 #1122: 9A the charge v1604, 19B2 the heal, v420 + v65598). Its
	 * condition STU_9D240539{self} isn't understood: drawn while he holds it (ours: at rest it filled by itself and sat there always,
	 * under the crosshair like a smile: the critique's H4). A ring round the crosshair, the number on a plate. Its target's preview, 013C
	 * (21C6 #913), and Life Grip's (21EF #704, the server's pick: Lifeweaver.widgets). Tree of Life's aim, 00DB (36A4 #901: primary 1701B
	 * "BUILD", secondary 1701C "CANCEL").
	 */
	private static void lifeweaver(GuiGraphicsExtractor g, LocalPlayer player, HeroSyncPayload s, float cx, float cy, float w, float h,
								   float partial, boolean reticle) {
		ClientLifeweaver.State l = ClientLifeweaver.state(player);
		boolean thorns = (s.mode() & dev.overcraft.lifeweaver.Lifeweaver.FLAG_THORNS) != 0;
		boolean held = (s.mode() & dev.overcraft.lifeweaver.Lifeweaver.FLAG_CHARGING) != 0;
		float charge = s.meter();
		if (held && l != null && l.bloomTarget() >= 0) {
			marker(g, player, l.bloomTarget(), w, h, partial, Action.SECONDARY_FIRE);
		} else {
			marker(g, player, s.widgets().preview(), w, h, partial, Action.ABILITY_2);
		}
		if (reticle && !thorns && held) {
			int colour = charge >= 0.999f ? WHITE : BLOSSOM;
			float r = 24;
			HeroHud.arc(g, cx, cy, r + 0.5f, 7, 0, 360, 0x59000000);
			HeroHud.arc(g, cx, cy, r, 4, 0, 360 * Math.clamp(charge, 0, 1), colour);
			mark(cy + 34);
			platedNumber(g, String.valueOf((int) Math.round(dev.overcraft.lifeweaver.LifeweaverStats.heal(charge))), cx, cy + 58, 20, WHITE);
			y = cy + 66;
		}
		if ((s.mode() & dev.overcraft.lifeweaver.Lifeweaver.FLAG_TREE_AIM) != 0) {
			prompt(g, Action.SECONDARY_FIRE, "overcraft.hud.prompt.build", Action.PRIMARY_FIRE, "overcraft.hud.prompt.cancel");
		}
	}

	/**
	 * Lúcio. The allies his song reaches, 067C on 0263 (0266 #374: count(v375) - 1), between the song circles HeroHud draws at +191, on a
	 * disc (a thin "1" alone read as a stray tick: the critique). Amp It Up's time, 0237 (0277 #396).
	 */
	private static void lucio(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float cx, float cy, boolean reticle) {
		if (reticle && s.widgets().count() > 0) {
			float at = cy + HeroHud.SONG_BELOW;
			image(g, HudShapes.DISC, cx - 15, at - 15, 30, 30, 0xA6000000);
			HudText.draw(g, Face.BODY, String.valueOf(s.widgets().count()), cx, at + 8, 16, WHITE, Align.CENTER);
		}
		if (s.activeLeft() > 0) {
			// (Beat Drop's 4 s runs the same bar a little over: it shows full, then counts down)
			timedBar(g, HeroArt.ability(hero, "amp_it_up"), s.activeLeft() / (float) dev.overcraft.lucio.LucioStats.AMP_UP_TIME, s.activeLeft(), cy);
		}
		if (reticle && s.meter() > 0) {
			// Soundwave Rider's charge, 0275 on the centre layer (5162 #450, under perk gate #437: v66249 / 100, chased to 100 at 20 a second,
			// #439, the 5 s the wiki gives; #443's 1.25 is its knockback; shown once it isn't 0, 27A3). The note on #85 called this "Accelerando's
			// meter": Accelerando (the same graph's #391 -> #393) has no widget. The mod's charge is s.meter(); the colour's ours, gold when full.
			y = Math.max(y, cy + TIMER_BELOW);
			resourceBar(g, HeroArt.perk(hero, "soundwave_rider"), s.meter(), null, s.meter() >= 1 ? 0xFFF7C43A : WHITE);
		}
	}

	/**
	 * Ana. Nano Boost's preview with it ready, 013C (0B9E #377: v1691[0], the key): the server's own pick (Ana.widgets: not one already
	 * boosted). The panel 0257 (#381), the prompt 00DB (#384: 16195 "NANO BOOST" / 16196 "CANCEL") and #373 bind v8070, the target after the
	 * press: they're the Requires Target Confirmation flow (option v29052, off by default, not in the mod), so not drawn. Scoped, only on
	 * the lens: over the scope's black surround it marks nothing to be seen, as her ally marks (HeroHud, critique_hero_polish M6). The data
	 * has no scope test: ours.
	 */
	private static void ana(GuiGraphicsExtractor g, LocalPlayer player, HeroSyncPayload s, float w, float h, float partial) {
		int id = s.widgets().preview();
		if (id < 0 || !(player.level().getEntity(id) instanceof LivingEntity target)) {
			return;
		}
		Vec3 at = target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0);
		if (ClientAna.ownScope() > HeroHud.SCOPE_SHOWN) {
			float[] screen = HeroHud.project(at, w, h);
			if (screen == null || Math.hypot(screen[0] - w / 2, screen[1] - h / 2) > Math.min(w, h) / 2 - HeroHud.SCOPE_MARGIN - SCOPE_MARKER_ROOM) {
				return;
			}
		}
		HeroHud.targetMarker(g, at, w, h, Action.ULTIMATE);
	}

	/**
	 * Being towed by a Jetpack Cat's Lifeline: 2A226 "GRAPPLED" and, 00CF on the towed one's HUD (5A8F #1225: x4F81E1BD jump, 2A472
	 * "Detach", 2D604 "%1$s Detach"). Letting go is JetpackCat.lifeline's: the jump key, from 0.6 s into the tow, so its prompt from then.
	 * Catnapper's carry has neither: jump does nothing there (JetpackCatFx.towedFor).
	 */
	private static void towed(GuiGraphicsExtractor g, LocalPlayer player) {
		float seconds = JetpackCatFx.towedFor(player);
		if (seconds < 0) {
			return;
		}
		y = Math.max(y, promptFloor - 26);
		mark(y);
		String state = Component.translatable("overcraft.hud.prompt.grappled").getString();
		float stateW = HudText.width(Face.BODY, state, 14);
		fill(g, midX - stateW / 2 - 6, y, stateW + 12, 22, PLATE);
		HudText.draw(g, Face.BODY, state, midX, y + 17, 14, 0xFF9FE6FF, Align.CENTER);
		y += 26;
		if (seconds >= dev.overcraft.jetpackcat.JetpackCatStats.LIFELINE_DETACH_AFTER) {
			float saved = promptFloor;
			promptFloor = y;
			prompt(g, Action.JUMP, "overcraft.hud.prompt.detach", null, null);
			promptFloor = saved;
		}
	}

	// ---- the widgets ----

	private static void mark(float top) {
		if (first < 0) {
			first = top;
		}
	}

	/** A target marker on this entity, if there is one (HeroHud.targetMarker). */
	private static void marker(GuiGraphicsExtractor g, LocalPlayer player, int id, float w, float h, float partial, Action key) {
		if (id >= 0 && player.level().getEntity(id) instanceof LivingEntity target) {
			HeroHud.targetMarker(g, target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0), w, h, key);
		}
	}

	private static float health(LivingEntity who) {
		return who.getHealth() / Math.max(1, who.getMaxHealth());
	}

	/** A timed ability, 0237: the share left as a white bar shrinking toward its left end, the seconds at its right, its icon at its left. */
	private static void timedBar(GuiGraphicsExtractor g, @Nullable Identifier icon, float share, float seconds, float cy) {
		y = Math.max(y, cy + TIMER_BELOW);
		mark(y - 8);
		share = Math.clamp(share, 0, 1);
		float x = midX - TIMER_W / 2;
		fill(g, x - 1, y - 1, TIMER_W + 2, TIMER_H + 2, 0x80000000);
		fill(g, x, y, TIMER_W, TIMER_H, 0x40FFFFFF);
		fill(g, x, y, TIMER_W * share, TIMER_H, 0xF2FFFFFF);
		number(g, String.format(Locale.ROOT, "%.1f", Math.max(0, seconds)), x + TIMER_W + 30, y + TIMER_H + 6, 18, WHITE);
		icon(g, icon, x, WHITE);
		y += TIMER_GAP;
	}

	/** A resource, 0275: a bar in its colour, as the timed bars, its text (a barrier's health, FUEL) at its right, its icon at its left. */
	private static void resourceBar(GuiGraphicsExtractor g, @Nullable Identifier icon, float share, @Nullable String text, int colour) {
		mark(y - 8);
		share = Math.clamp(share, 0, 1);
		float x = midX - TIMER_W / 2;
		fill(g, x - 1, y - 1, TIMER_W + 2, TIMER_H + 2, 0x99000000);
		fill(g, x, y, TIMER_W, TIMER_H, 0x40FFFFFF);
		fill(g, x, y, TIMER_W * share, TIMER_H, colour);
		if (text != null) {
			if (Character.isDigit(text.charAt(0))) {
				number(g, text, x + TIMER_W + 30, y + TIMER_H + 6, 18, WHITE);
			} else {
				HudText.outlined(g, Face.BODY, text, x + TIMER_W + 8, y + TIMER_H + 3, 11, WHITE, 0xB3000000, 1, Align.LEFT);
			}
		}
		icon(g, icon, x, colour);
		y += TIMER_GAP;
	}

	private static void icon(GuiGraphicsExtractor g, @Nullable Identifier icon, float x, int colour) {
		if (icon != null) {
			float s = 26;
			image(g, icon, x - s - 9, y + TIMER_H / 2 - s / 2 + 1, s, s, 0x99000000);
			image(g, icon, x - s - 10, y + TIMER_H / 2 - s / 2, s, s, colour);
		}
	}

	/** A count of what's left (Repair Pack's), as a row of short bars: full and empty (00F3 binds the count alone: the shape ours). */
	private static void pips(GuiGraphicsExtractor g, int have, int max, float cx, float top) {
		pips(g, have, max, cx, top, 22, 6);
	}

	private static void pips(GuiGraphicsExtractor g, int have, int max, float cx, float top, float pw, float gap) {
		float x = cx - (max * pw + (max - 1) * gap) / 2;
		for (int i = 0; i < max; i++) {
			float px = x + i * (pw + gap);
			fill(g, px - 1, top - 1, pw + 2, 8, 0xA6000000);
			// (an empty one a touch brighter: at 0x33 they were lost at night, on Sigma's and on Brigitte's)
			fill(g, px, top, pw, 6, i < have ? 0xF2F7C43A : 0x66FFFFFF);
		}
	}

	/**
	 * A target's panel (0258, 052B, 0BA3, 0D81): their portrait in a square, edged in the ally cyan, their name beside it on a plate; a
	 * health bar under the portrait if {@code health} isn't negative; a strip in {@code accent} under the name if it isn't 0 (Mercy's beam,
	 * ours); an ability's key before all that if it has one. {@code who} null: {@code type} and {@code playerName} say whose face.
	 */
	private static void panel(GuiGraphicsExtractor g, @Nullable Entity who, String name, String type, String playerName, float health, int accent,
							  @Nullable Action key, float cx) {
		mark(y - 2);
		float size = 34;
		float cap = 12;
		float nameW = HudText.width(Face.BODY, name, cap);
		float keyW = key == null ? 0 : 28;
		float total = keyW + size + 8 + nameW;
		float x = cx - total / 2;
		float top = y;
		if (key != null) {
			HeroHud.keyLabel(g, HeroControls.get(key), x + 11, top + size / 2, 0, false);
			x += keyW;
		}
		fill(g, x + size + 3, top + size / 2 - 11, nameW + 10, 22, PLATE);
		fill(g, x - 2, top - 2, size + 4, size + 4, 0x8C000000);
		fill(g, x, top, size, size, 0xFF1B2A4A);
		face(g, who, type, playerName, x + 2, top + 2, size - 4);
		outline(g, x - 1, top - 1, size + 2, size + 2, 2, ALLY);
		HudText.draw(g, Face.BODY, name, x + size + 8, top + size / 2 + cap / 2, cap, WHITE, Align.LEFT);
		if (accent != 0) {
			fill(g, x + size + 3, top + size / 2 + 11, nameW + 10, 3, accent);
		}
		if (health >= 0) {
			fill(g, x - 2, top + size + 3, size + 4, 6, 0x99000000);
			fill(g, x - 1, top + size + 4, (size + 2) * Math.clamp(health, 0, 1), 4, WHITE);
		}
		y = top + size + (health >= 0 ? 16 : 8);
	}

	/**
	 * A target's face: a hero's art for a player with a hero, a player's skin, a mob's face from Minecraft's textures (MobFaces), else
	 * a plain plus. The data's 1BDC2 (a non-hero's portrait) isn't in the local pack. A soul has no entity: its type comes "hero:id" for
	 * a player with a hero, or their entity type (Mercy.widgets).
	 */
	private static void face(GuiGraphicsExtractor g, @Nullable Entity who, String type, String playerName, float x, float y, float size) {
		if (who instanceof Player player) {
			Hero hero = Hero.byId(player.getAttachedOrElse(HeroManager.HERO, ""));
			if (heroFace(g, hero, x, y, size) || skin(g, player.getName().getString(), x, y, size)) {
				return;
			}
		}
		if (type.startsWith("hero:") && heroFace(g, Hero.byId(type.substring(5)), x, y, size)) {
			return;
		}
		if ((type.startsWith("hero:") || type.equals("minecraft:player")) && skin(g, playerName, x, y, size)) {
			return;
		}
		String id = who != null ? BuiltInRegistries.ENTITY_TYPE.getKey(who.getType()).toString() : type;
		MobFace mob = MobFaces.get(id);
		if (mob != null) {
			MobFaces.draw(g, mob, x, y, size, size, WHITE);
		} else {
			image(g, HudShapes.PLUS, x + size * 0.15f, y + size * 0.15f, size * 0.7f, size * 0.7f, WHITE);
		}
	}

	private static boolean heroFace(GuiGraphicsExtractor g, @Nullable Hero hero, float x, float y, float size) {
		Identifier art = hero == null ? null : HeroArt.head(hero);
		if (art == null) {
			return false;
		}
		image(g, art, x, y, size, size, WHITE);
		return true;
	}

	private static boolean skin(GuiGraphicsExtractor g, String name, float x, float y, float size) {
		var connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection == null || name.isEmpty() ? null : connection.getPlayerInfo(name);
		if (info == null) {
			return false;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(size / 8, size / 8);
		PlayerFaceExtractor.extractRenderState(g, info.getSkin(), 0, 0, 8, WHITE);
		g.pose().popMatrix();
		return true;
	}

	/**
	 * A button prompt, 00DB (00CF for a towed ally): "[KEY] WHAT" under the bars on a plate, a second pair beside it (6441 / 6442, 6672 /
	 * 6673). The words are the data's (dump-strings); a mouse button is the HUD's mouse glyph (HeroHud.mouse).
	 */
	private static void prompt(GuiGraphicsExtractor g, Action key, String text, @Nullable Action key2, @Nullable String text2) {
		y = Math.max(y, promptFloor);
		mark(y);
		float cap = 12;
		String a = Component.translatable(text).getString();
		String b = text2 == null ? "" : Component.translatable(text2).getString();
		float widthA = keyWidth(key) + 6 + HudText.width(Face.BODY, a, cap);
		float widthB = key2 == null ? 0 : keyWidth(key2) + 6 + HudText.width(Face.BODY, b, cap);
		float width = widthA + (key2 == null ? 0 : 22 + widthB);
		float x = midX - width / 2;
		fill(g, x - 6, y - 3, width + 12, 28, PLATE);
		x = pair(g, key, a, x, y + 11, cap);
		if (key2 != null) {
			pair(g, key2, b, x + 22, y + 11, cap);
		}
		y += 32;
	}

	private static float keyWidth(Action key) {
		String label = HeroHud.shortKey(HeroControls.get(key));
		return label.equals("LMB") || label.equals("RMB") ? 19 : Math.max(22, HudText.width(Face.SMALL, label, 10.5f) + 8);
	}

	private static float pair(GuiGraphicsExtractor g, Action key, String text, float x, float mid, float cap) {
		float kw = keyWidth(key);
		HeroHud.mouse(g, HeroControls.get(key), x + kw / 2, mid);
		HudText.draw(g, Face.BODY, text, x + kw + 6, mid + cap / 2, cap, WHITE, Align.LEFT);
		return x + kw + 6 + HudText.width(Face.BODY, text, cap);
	}

	private static void number(GuiGraphicsExtractor g, String text, float x, float baseline, float cap, int colour) {
		HudText.outlined(g, Face.NUMBERS, text, x, baseline, cap, colour, 0xB3000000, 1.5f, Align.CENTER);
	}

	/** A number on a dark plate: on a brown villager or her own gun, bare digits were lost (the critique's H3, H4). */
	private static void platedNumber(GuiGraphicsExtractor g, String text, float x, float baseline, float cap, int colour) {
		float w = HudText.width(Face.NUMBERS, text, cap);
		fill(g, x - w / 2 - 6, baseline - cap - 3, w + 12, cap + 7, PLATE);
		number(g, text, x, baseline, cap, colour);
	}

	private static String displayName(Entity entity) {
		return entity.getName().getString().toUpperCase(Locale.ROOT);
	}

	// ---- the crosshairs ----

	/**
	 * Each hero's crosshair. The data names one per hero (below) but no art (each .116 is empty): the shapes are OURS, from each
	 * weapon's nature. Kiriko's "+" is measured from Overwatch's (the reference's 1.5).
	 */
	public static void crosshair(GuiGraphicsExtractor g, @Nullable Hero hero, HeroSyncPayload s, float cx, float cy) {
		if (hero == null) {
			plus(g, cx, cy, 4, 7, 2);
			return;
		}
		switch (hero) {
			// 0BA2 (59DC #43): her volley's five shots round the aim's own dot: the centre one below it, four paws in an arc above (the wiki)
			case JETPACK_CAT -> {
				dot(g, cx, cy, 3);
				dot(g, cx, cy + 11, 2.6f);
				for (int deg : new int[] {-54, -18, 18, 54}) {
					double a = Math.toRadians(deg);
					dot(g, cx + (float) Math.sin(a) * 16, cy - (float) Math.cos(a) * 16, 2.6f);
				}
			}
			// 0256 (0071 #443): the staff's beam locks round the aim; the blaster fires straight
			case MERCY -> {
				if ((s.mode() & dev.overcraft.mercy.Mercy.FLAG_BLASTER) != 0) {
					plus(g, cx, cy, 4, 6, 2);
				} else {
					ring(g, cx, cy, 14);
					dot(g, cx, cy, 2);
				}
			}
			// 072D (21C6 #844): the blossom a ring; the thorns a tight "+"
			case LIFEWEAVER -> {
				if ((s.mode() & dev.overcraft.lifeweaver.Lifeweaver.FLAG_THORNS) != 0) {
					plus(g, cx, cy, 3, 5, 2);
				} else {
					ring(g, cx, cy, 11);
					dot(g, cx, cy, 2);
				}
			}
			// 0987 (0C6B #34): the rifle unscoped: a ring and its dot
			case ANA -> {
				ring(g, cx, cy, 10);
				dot(g, cx, cy, 2);
			}
			// 00A3 (018D #595): the machine pistol's spread. Stealthed, 0277 takes its place in the same layer (420C #1057): the dot in
			// its ring (the ring: sombra, above)
			case SOMBRA -> {
				if (s.widgets().left() <= 0) {
					ring(g, cx, cy, 16);
				}
				dot(g, cx, cy, 2);
			}
			// 00F2 (145B #51): a flail's reach: a dot and two side ticks, heavy enough for her own bright barrier
			case BRIGITTE -> {
				dot(g, cx, cy, 2.6f);
				for (int side = -1; side <= 1; side += 2) {
					fill(g, cx + side * 15 - 6, cy - 3, 12, 6, EDGE);
					fill(g, cx + side * 15 - 5, cy - 1.5f, 10, 3, WHITE);
				}
			}
			// 091C (4441 #563), 027B (010C #311), 023F (his loadout 028C): a ring and its dot, each their own size
			case JUNO -> {
				ring(g, cx, cy, 9);
				plus(g, cx, cy, 11, 4, 2);
			}
			case LUCIO -> {
				ring(g, cx, cy, 12);
				dot(g, cx, cy, 2);
			}
			case SIGMA -> {
				ring(g, cx, cy, 11);
				dot(g, cx, cy, 2);
			}
			// Kiriko's 05BF and Sojourn's 044C (142E #418): the "+"
			default -> plus(g, cx, cy, 4, 7, 2);
		}
	}

	/** A white "+" of four ticks round a gap, outlined dark (the reference's 1.5). */
	private static void plus(GuiGraphicsExtractor g, float cx, float cy, float gap, float len, float t) {
		for (int pass = 0; pass < 2; pass++) {
			float o = pass == 0 ? 1 : 0;
			int color = pass == 0 ? 0x99000000 : WHITE;
			fill(g, cx - t / 2 - o, cy - gap - len - o, t + 2 * o, len + 2 * o, color);
			fill(g, cx - t / 2 - o, cy + gap - o, t + 2 * o, len + 2 * o, color);
			fill(g, cx - gap - len - o, cy - t / 2 - o, len + 2 * o, t + 2 * o, color);
			fill(g, cx + gap - o, cy - t / 2 - o, len + 2 * o, t + 2 * o, color);
		}
	}

	private static void dot(GuiGraphicsExtractor g, float cx, float cy, float r) {
		image(g, HudShapes.DISC, cx - r - 1.5f, cy - r - 1.5f, 2 * r + 3, 2 * r + 3, EDGE);
		image(g, HudShapes.DISC, cx - r, cy - r, 2 * r, 2 * r, WHITE);
	}

	private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r) {
		HeroHud.arc(g, cx, cy, r, 4.5f, 0, 360, EDGE);
		HeroHud.arc(g, cx, cy, r, 2.2f, 0, 360, WHITE);
	}
}

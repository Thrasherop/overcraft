package dev.overcraft.client.hud;

import dev.overcraft.Ow;
import dev.overcraft.client.AbilityCamera;
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
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.Targeting;
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
 * 0275 (a resource's bar), 00DB (button prompts) and 00CF (a prompt with a state over it), and each hero's own: 044D, 0C7A, 00F3,
 * 0258, 052B, 0277 / 008B, 0D81, 00B1, 0BA3, 072E, 0263.
 *
 * <p>The data gives what each widget binds (a fraction, a time, a portrait, a count, a button) and which layer holds it; the
 * view models themselves hold no art or layout (their .116 assets are empty at every depth: the 2026-11-02 audit). So what's
 * bound and when it shows are the data's, and every shape, size, colour and place here is OURS, built from Overwatch's look.
 * The notes on what's what: techniques/hud/reticle.md.
 *
 * <p>All laid out in reference pixels (1080 high), the crosshair at (w / 2, h / 2). Widgets stack downward from ROW_TOP, so two
 * shown together can't overlap; ELIMINATED's caption is at +250 and the kill marker's strokes end within 28 of the centre
 * (HeroHud), both clear of them. Drawn only in hero mode (HeroHud.render returns earlier in Minecraft mode).
 */
public final class ReticleHud {
	/** Where the first widget below the crosshair starts (ours: clear of the kill marker's strokes, which end at 28). */
	private static final float ROW_TOP = 46;
	/** Where the bars start, the place fix/hero-polish's Juno timers use too (HeroHud.TIMER_BELOW there, and TIMER_GAP). */
	private static final float TIMER_BELOW = 92, TIMER_GAP = 30;
	private static final float TIMER_W = 180, TIMER_H = 6;
	/** Prompts' first row, below two bars (ours). The last row stays above ELIMINATED's caption, whose letters top at +237. */
	private static final float PROMPT_BELOW = 160;

	private static final int WHITE = 0xFFFFFFFF;
	/** The team-colour edge on a target's portrait (0252's three colours, its values not in the data's text: ours, the ally cyan). */
	private static final int ALLY = 0xFF29C8F0;
	/** Mercy's beams on her target's panel, as her beams read: the heal gold, the boost blue (ours, from MercyFx's look). */
	private static final int BEAM_HEAL = 0xFFF5D226, BEAM_BOOST = 0xFF4FB8FF;
	private static final int STEALTH = 0xFFB46CFF;
	private static final int BARRIER = 0xFF9FE6FF;
	private static final int RAILGUN = 0xFFE8553F;
	private static final int BLOSSOM = 0xFFFF8FC8;
	private static final int WARN = 0xFFF2A23C;

	/** The stack's cursor this frame: the next widget's top (reference pixels from the screen's top). */
	private static float y;

	private ReticleHud() {
	}

	private static int alpha(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}

	/** The crosshair and what's round it show: in first person, and from behind while an ability holds the view (as HeroHud's). */
	private static boolean atReticle() {
		var mc = Minecraft.getInstance();
		return mc.options.getCameraType().isFirstPerson() || AbilityCamera.active() && !dev.overcraft.client.comms.EmoteCamera.active()
			&& mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_BACK;
	}

	/** One frame's widgets for this hero. */
	public static void render(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float w, float h, float partial) {
		if (!player.isAlive()) {
			return;
		}
		float cx = w / 2;
		float cy = h / 2;
		y = cy + ROW_TOP;
		boolean reticle = atReticle();
		switch (hero) {
			case SOJOURN -> sojourn(g, s, cx, cy, reticle);
			case SIGMA -> sigma(g, hero, s, cx, cy);
			case BRIGITTE -> brigitte(g, player, hero, s, cx, cy, w, h, partial, reticle);
			case MERCY -> mercy(g, player, hero, s, cx, cy, reticle);
			case SOMBRA -> sombra(g, player, hero, s, cx, cy, w, h, partial, reticle);
			case JUNO -> juno(g, s, cx, cy);
			case JETPACK_CAT -> jetpackCat(g, player, hero, s, cx, cy);
			case LIFEWEAVER -> lifeweaver(g, player, hero, s, cx, cy, w, h, partial, reticle);
			case LUCIO -> lucio(g, hero, s, cx, cy, reticle);
			case ANA -> ana(g, player, hero, s, w, h, partial);
			default -> {
			}
		}
		towed(g, player, hero, cx, cy);
	}

	// ---- each hero ----

	/**
	 * Sojourn. Railgun energy, 044D in her reticle (1A23 #1179): the fraction v87 / v38252, the number 60D3 (v87, rounded up) and
	 * the Overclock state 63F4. Ours: two arcs round the crosshair filling upward, the number under them. Overclock's time, 0237 in
	 * her reticle (2159 #727).
	 */
	private static void sojourn(GuiGraphicsExtractor g, HeroSyncPayload s, float cx, float cy, boolean reticle) {
		float energy = s.meter();
		boolean overclock = s.ultActive();
		if (reticle) {
			int colour = overclock ? 0xFF7FE8FF : energy >= 0.999f ? WHITE : RAILGUN;
			float share = Math.clamp(energy, 0, 1);
			float r = 34;
			float sweep = 70;
			for (int side = -1; side <= 1; side += 2) {
				float low = side < 0 ? 180 + 16 : 180 - 16 - sweep * 2;
				float high = low + sweep * 2;
				HeroHud.arc(g, cx, cy, r + 1, 7, low, high, 0x59000000);
				if (share > 0) {
					// up from the bottom on both sides
					if (side < 0) {
						HeroHud.arc(g, cx, cy, r, 4, low, low + sweep * 2 * share, colour);
					} else {
						HeroHud.arc(g, cx, cy, r, 4, high - sweep * 2 * share, high, colour);
					}
				}
			}
			number(g, String.valueOf((int) Math.ceil(energy * 100)), cx, cy + 66, 20, overclock ? 0xFF7FE8FF : WHITE);
			y = cy + 74;
		}
		if (overclock) {
			timedBar(g, HeroArt.ultimate(Hero.SOJOURN), s.activeLeft() / (float) dev.overcraft.sojourn.SojournStats.OVERCLOCK_TIME,
				s.activeLeft(), cx, cy);
		}
	}

	/**
	 * Sigma. His barrier's health, 0275 on the centre layer (2155 #12: the bar's share C7, the health v586 in the text 5FCE, his
	 * hero option "Experimental Barrier UI", on by default) while it's out. Gravitic Flux's aim: 0237 (1E41 #1152) and 00DB (#1283:
	 * primary, 4ED9). Kinetic Grasp's hold: its time, as the other timed abilities (the data draws none: ours).
	 */
	private static void sigma(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float cx, float cy) {
		y = Math.max(y, cy + TIMER_BELOW);
		if ((s.mode() & dev.overcraft.sigma.Sigma.MODE_BARRIER_OUT) != 0) {
			resourceBar(g, HeroArt.ability(hero, "experimental_barrier"), s.meter(), String.valueOf(s.secondaryAmmo()), BARRIER, cx);
		}
		if ((s.mode() & dev.overcraft.sigma.Sigma.MODE_GRASP) != 0) {
			timedBar(g, HeroArt.ability(hero, "kinetic_grasp"), s.activeLeft() / (float) dev.overcraft.sigma.SigmaStats.GRASP_TIME,
				s.activeLeft(), cx, cy);
		}
		HeroSyncPayload.Widgets d = s.widgets();
		if (d.count() > 0) {
			timedBar(g, HeroArt.ultimate(hero), d.left() / d.total(), d.left(), cx, cy);
			prompt(g, cx, cy, Action.SECONDARY_FIRE, "overcraft.hud.prompt.confirm", null, null);
		}
	}

	/**
	 * Brigitte. Repair Pack's charges, 00F3 in her reticle (0EDD #472: v11); Inspire, 0C7A in her reticle (12D8 #479: its icon, and
	 * 067C the allies it's healing; her hero option Inspire UI, on by default); the barrier's health, 0275 on the centre layer (2152
	 * #7) with 00DB (0EE3 #848: primary, 161DC) while it's up; Rally's time, 0237 (12BE #188); Repair Pack's target, 013C (0EDD #469).
	 */
	private static void brigitte(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy, float w,
								 float h, float partial, boolean reticle) {
		HeroSyncPayload.Widgets d = s.widgets();
		int charges = s.secondaryAmmo();
		// (behind the barrier, none: the bash's prompt is where the marker would land. Ours)
		if (charges > 0 && (s.mode() & dev.overcraft.brigitte.Brigitte.MODE_BARRIER) == 0) {
			LivingEntity target = Targeting.allyInView(player, dev.overcraft.brigitte.BrigitteStats.REPAIR_RANGE,
				dev.overcraft.brigitte.BrigitteStats.REPAIR_CONE, true);
			if (target != null) {
				HeroHud.targetMarker(g, target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0), w, h, Action.ABILITY_2);
			}
		}
		if (reticle) {
			pips(g, charges, dev.overcraft.brigitte.BrigitteStats.REPAIR_CHARGES, cx, y);
			y += 16;
			if (d.count() > 0) {
				Identifier icon = HeroArt.ability(hero, "inspire");
				String count = String.valueOf(d.count());
				float textW = HudText.width(Face.NUMBERS, count, 16);
				float x = cx - (22 + 4 + textW) / 2;
				if (icon != null) {
					image(g, icon, x + 1, y + 1, 22, 22, 0x99000000);
					image(g, icon, x, y, 22, 22, 0xFFF7C43A);
				}
				number(g, count, x + 26 + textW / 2, y + 19, 16, 0xFFF7C43A);
				y += 26;
			}
		}
		y = Math.max(y, cy + TIMER_BELOW);
		boolean up = (s.mode() & dev.overcraft.brigitte.Brigitte.MODE_BARRIER) != 0;
		if (up) {
			float max = (float) (d.left() > 0 ? dev.overcraft.brigitte.BrigitteStats.BARRIER_HEALTH_RALLY : dev.overcraft.brigitte.BrigitteStats.BARRIER_HEALTH);
			resourceBar(g, HeroArt.ability(hero, "barrier_shield"), s.primaryAmmo() / max, String.valueOf(s.primaryAmmo()), BARRIER, cx);
		}
		if (d.left() > 0) {
			timedBar(g, HeroArt.ultimate(hero), d.left() / d.total(), d.left(), cx, cy);
		}
		if (up && s.ability2Cooldown() <= 0) {
			prompt(g, cx, cy, Action.SECONDARY_FIRE, "overcraft.hud.prompt.shield_bash", null, null);
		}
	}

	/**
	 * Mercy. Her beam's target, 0258 in her reticle (006B #1263): 2E4B the name, 4447 the portrait, 063E2 boost or heal. Resurrect's,
	 * 052B in her reticle (00CA #306, shown while it's off cooldown with a soul in reach) and its cast. Guardian Angel's momentum in
	 * flight, 0275 (006E #910: v87 / v971, her wing icon 12F3); Valkyrie's time, 0237 (10DA #486).
	 */
	private static void mercy(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy, boolean reticle) {
		ClientMercy.State m = ClientMercy.state(player);
		HeroSyncPayload.Widgets d = s.widgets();
		if (reticle) {
			if (m != null && m.beamTarget() >= 0 && player.level().getEntity(m.beamTarget()) instanceof LivingEntity beamed && beamed.isAlive()) {
				boolean boost = m.beamMode() == dev.overcraft.mercy.Mercy.BEAM_BOOST;
				panel(g, beamed, displayName(beamed), "", boost ? BEAM_BOOST : BEAM_HEAL, -1, null, cx);
			}
			if (!d.name().isEmpty()) {
				// (067C: with more than one soul in reach, how many)
				String who = d.name().toUpperCase(Locale.ROOT) + (d.count() > 1 ? " (" + d.count() + ")" : "");
				panel(g, null, who, d.face(), 0xFFF5F0D0, -1, Action.ABILITY_2, cx);
			}
		}
		y = Math.max(y, cy + TIMER_BELOW);
		if ((s.mode() & dev.overcraft.mercy.Mercy.FLAG_RESURRECT) != 0 && d.total() > 0) {
			timedBar(g, HeroArt.ability(hero, "resurrect"), d.left() / d.total(), d.left(), cx, cy);
		}
		if ((s.mode() & dev.overcraft.mercy.Mercy.FLAG_VALKYRIE) != 0) {
			timedBar(g, HeroArt.ultimate(hero), s.activeLeft() / (float) dev.overcraft.mercy.MercyStats.VALKYRIE_TIME, s.activeLeft(), cx, cy);
		}
		if ((s.mode() & dev.overcraft.mercy.Mercy.FLAG_GA) != 0) {
			resourceBar(g, HeroArt.ability(hero, "guardian_angel"), s.meter(), null, 0xFFF5D226, cx);
		}
	}

	/**
	 * Sombra. Her stealth, 0277 in her reticle with the ring 008B (420C #1057: its share v215 / v567). Hotfix's target, 0D81 in her
	 * reticle (614C #1798: portrait, health bar 019C) and its preview 013C (614C #593); the hack lockout, 00B1 (#2775: v6358 / v45433).
	 */
	private static void sombra(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy, float w, float h,
							   float partial, boolean reticle) {
		HeroSyncPayload.Widgets d = s.widgets();
		if (s.secondaryAmmo() > 0 && d.target() < 0) {
			LivingEntity target = Targeting.allyInView(player, dev.overcraft.sombra.SombraStats.HOTFIX_RANGE,
				dev.overcraft.sombra.SombraStats.HOTFIX_CONE, true);
			if (target != null) {
				HeroHud.targetMarker(g, target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0), w, h, Action.PRIMARY_FIRE);
			}
		}
		if (reticle) {
			if (d.left() > 0 && d.total() > 0) {
				float r = 40;
				HeroHud.arc(g, cx, cy, r + 0.5f, 6, 0, 360, 0x59000000);
				HeroHud.arc(g, cx, cy, r, 3.5f, 0, 360 * Math.clamp(d.left() / d.total(), 0, 1), STEALTH);
				y = Math.max(y, cy + 50);
			}
			if (d.target() >= 0 && player.level().getEntity(d.target()) instanceof LivingEntity target) {
				panel(g, target, displayName(target), "", ALLY, target.getHealth() / Math.max(1, target.getMaxHealth()), null, cx);
			}
			if (d.left2() > 0) {
				float share = Math.clamp(d.left2() / (float) dev.overcraft.sombra.SombraStats.HOTFIX_HACK_LOCKOUT, 0, 1);
				float bw = 90;
				fill(g, cx - bw / 2 - 1, y - 1, bw + 2, 6, 0x80000000);
				fill(g, cx - bw / 2, y, bw * share, 4, WARN);
				y += 12;
			}
		}
	}

	/**
	 * Juno's prompts: Glide Boost's (47B7 #637: its own button, 1B3C0) and the torpedoes' (4695 #867: fire to launch). Her timers,
	 * 0237 (47B7 #641, 4695 #1225 / #1283), are fix/hero-polish's (HeroHud.junoTimers), in this stack's bar rows.
	 */
	private static void juno(GuiGraphicsExtractor g, HeroSyncPayload s, float cx, float cy) {
		y = Math.max(y, cy + TIMER_BELOW);
		if ((s.mode() & dev.overcraft.juno.Juno.MODE_GLIDE) != 0) {
			prompt(g, cx, cy, Action.ABILITY_1, "overcraft.hud.prompt.end_glide", null, null);
		}
		if ((s.mode() & dev.overcraft.juno.Juno.MODE_LOCK) != 0) {
			prompt(g, cx, cy, Action.SECONDARY_FIRE, "overcraft.hud.prompt.launch", null, null);
		}
	}

	/**
	 * Jetpack Cat. Lifeline's tow, 0BA3 in her reticle (5A8F #1097: the towed ally's portrait v59143). Purr's time, 0237 (5A7E #320);
	 * Catnapper's carry, 0237 (5B7B #63), and its aim's prompts, 00DB (5B7B #34: primary confirms, the ult key cancels here).
	 */
	private static void jetpackCat(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy) {
		HeroSyncPayload.Widgets d = s.widgets();
		if (d.target() >= 0 && atReticle() && player.level().getEntity(d.target()) instanceof LivingEntity held) {
			panel(g, held, displayName(held), "", ALLY, -1, null, cx);
		}
		y = Math.max(y, cy + TIMER_BELOW);
		int phase = s.mode() & 7;
		if (phase == dev.overcraft.jetpackcat.JetpackCat.ULT_CARRY) {
			timedBar(g, HeroArt.ultimate(hero), s.activeLeft() / (float) dev.overcraft.jetpackcat.JetpackCatStats.ULT_CARRY, s.activeLeft(), cx, cy);
		} else if (phase == dev.overcraft.jetpackcat.JetpackCat.ULT_NONE && s.activeLeft() > 0) {
			timedBar(g, HeroArt.ability(hero, "purr"), s.activeLeft() / (float) dev.overcraft.jetpackcat.JetpackCatStats.PURR_TIME, s.activeLeft(), cx, cy);
		}
		if (phase == dev.overcraft.jetpackcat.JetpackCat.ULT_AIM) {
			prompt(g, cx, cy, Action.SECONDARY_FIRE, "overcraft.hud.prompt.confirm", Action.ULTIMATE, "overcraft.hud.prompt.cancel");
		}
	}

	/**
	 * Lifeweaver. Healing Blossom's charge, 072E in his reticle (21C6 #1122: 9A the charge v1604, 19B2 the heal, v420 + v65598). Its
	 * target's preview, 013C (21C6 #913), and Life Grip's (21EF #704). Tree of Life's aim, 00DB (36A4 #901: primary places, secondary
	 * cancels).
	 */
	private static void lifeweaver(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float cx, float cy, float w, float h,
								   float partial, boolean reticle) {
		ClientLifeweaver.State l = ClientLifeweaver.state(player);
		boolean thorns = (s.mode() & dev.overcraft.lifeweaver.Lifeweaver.FLAG_THORNS) != 0;
		float charge = s.meter();
		if (l != null && l.has(dev.overcraft.lifeweaver.Lifeweaver.FLAG_CHARGING) && l.bloomTarget() >= 0
			&& player.level().getEntity(l.bloomTarget()) instanceof LivingEntity target) {
			HeroHud.targetMarker(g, target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0), w, h, Action.SECONDARY_FIRE);
		} else if (s.ability2Cooldown() <= 0 && (s.mode() & (dev.overcraft.lifeweaver.Lifeweaver.FLAG_TREE_AIM
			| dev.overcraft.lifeweaver.Lifeweaver.FLAG_TREE_CAST)) == 0) {
			// (not while he aims the tree: startGrip refuses, and the marker's key landed in the tree's prompts: found playing)
			LivingEntity target = Targeting.allyInView(player, dev.overcraft.lifeweaver.LifeweaverStats.GRIP_RANGE,
				dev.overcraft.lifeweaver.LifeweaverStats.GRIP_CONE, true);
			if (target != null) {
				HeroHud.targetMarker(g, target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0), w, h, Action.ABILITY_2);
			}
		}
		if (reticle && !thorns && charge > 0) {
			boolean held = (s.mode() & dev.overcraft.lifeweaver.Lifeweaver.FLAG_CHARGING) != 0;
			int colour = charge >= 0.999f ? WHITE : held ? BLOSSOM : 0xB3F3A7C8;
			float r = 30;
			float half = 62;
			HeroHud.arc(g, cx, cy, r + 0.5f, 7, 180 - half, 180 + half, 0x59000000);
			HeroHud.arc(g, cx, cy, r, 4, 180 - half, 180 - half + half * 2 * Math.clamp(charge, 0, 1), colour);
			number(g, String.valueOf((int) Math.round(dev.overcraft.lifeweaver.LifeweaverStats.heal(charge))), cx, cy + 58, 18, colour | 0xFF000000);
			y = cy + 66;
		}
		if ((s.mode() & dev.overcraft.lifeweaver.Lifeweaver.FLAG_TREE_AIM) != 0) {
			prompt(g, cx, cy, Action.SECONDARY_FIRE, "overcraft.hud.prompt.place", Action.PRIMARY_FIRE, "overcraft.hud.prompt.cancel");
		}
	}

	/**
	 * Lúcio. The allies his song reaches, 067C on 0263 (0266 #374: count(v375) - 1), between the song circles HeroHud draws at +191.
	 * Amp It Up's time, 0237 (0277 #396).
	 */
	private static void lucio(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float cx, float cy, boolean reticle) {
		if (reticle && s.widgets().count() > 0) {
			number(g, String.valueOf(s.widgets().count()), cx, cy + HeroHud.SONG_BELOW + 8, 18, WHITE);
		}
		if (s.activeLeft() > 0) {
			y = Math.max(y, cy + TIMER_BELOW);
			// (Beat Drop's 4 s runs the same bar a little over: it shows full, then counts down)
			timedBar(g, HeroArt.ability(hero, "amp_it_up"), s.activeLeft() / (float) dev.overcraft.lucio.LucioStats.AMP_UP_TIME, s.activeLeft(), cx, cy);
		}
	}

	/**
	 * Ana. Nano Boost's target with it ready: the preview 013C (0B9E #373) and the panel 0257 on the centre layer (0B9E #381: portrait,
	 * name 67DC). The confirm prompt 00DB (#384) goes with Requires Target Confirmation, not in the mod (its default, off).
	 */
	private static void ana(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, HeroSyncPayload s, float w, float h, float partial) {
		if (s.ultCharge() < s.ultCost() || s.ultActive()) {
			return;
		}
		LivingEntity target = Targeting.allyInView(player, dev.overcraft.ana.AnaStats.NANO_RANGE, dev.overcraft.ana.AnaStats.NANO_CONE, true);
		if (target != null) {
			HeroHud.targetMarker(g, target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0), w, h, Action.ULTIMATE);
			y = h / 2 + TIMER_BELOW;
			panel(g, target, displayName(target), "", ALLY, -1, null, w / 2);
		}
	}

	/**
	 * Being towed by a Jetpack Cat: "GRAPPLED" and "[Jump] DETACH", 00CF on the towed one's HUD (5A8F #1225: x4F81E1BD, the texts
	 * 2A472 / 2D604; the wiki: shown "Grappled"). Letting go is JetpackCat.lifeline's: the jump key, from 0.6 s into the tow.
	 */
	private static void towed(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, float cx, float cy) {
		if (!JetpackCatFx.towed(player)) {
			return;
		}
		// clear of Kiriko's ofuda counter (+136) and Lúcio's song circles (+161 to +221): below the one, above the other
		float top = hero == Hero.KIRIKO ? cy + 172 : hero == Hero.LUCIO ? cy + 104 : Math.max(y, cy + 134);
		String state = Component.translatable("overcraft.hud.prompt.grappled").getString().toUpperCase(Locale.ROOT);
		HudText.outlined(g, Face.BODY, state, cx, top + 14, 14, 0xFF9FE6FF, 0xB3000000, 1, Align.CENTER);
		String text = Component.translatable("overcraft.hud.prompt.detach").getString().toUpperCase(Locale.ROOT);
		float width = keyWidth(Action.JUMP) + 6 + HudText.width(Face.BODY, text, 12);
		pair(g, Action.JUMP, text, cx - width / 2, top + 34, 12);
	}

	// ---- the widgets ----

	/** A timed ability, 0237: the share left as a white bar shrinking toward its left end, the seconds at its right, its icon at its left. */
	private static void timedBar(GuiGraphicsExtractor g, @Nullable Identifier icon, float share, float seconds, float cx, float cy) {
		y = Math.max(y, cy + TIMER_BELOW);
		share = Math.clamp(share, 0, 1);
		float x = cx - TIMER_W / 2;
		fill(g, x - 1, y - 1, TIMER_W + 2, TIMER_H + 2, 0x80000000);
		fill(g, x, y, TIMER_W, TIMER_H, 0x40FFFFFF);
		fill(g, x, y, TIMER_W * share, TIMER_H, 0xF2FFFFFF);
		number(g, String.format(Locale.ROOT, "%.1f", Math.max(0, seconds)), x + TIMER_W + 30, y + TIMER_H + 6, 18, WHITE);
		if (icon != null) {
			float s = 26;
			image(g, icon, x - s - 9, y + TIMER_H / 2 - s / 2 + 1, s, s, 0x99000000);
			image(g, icon, x - s - 10, y + TIMER_H / 2 - s / 2, s, s, WHITE);
		}
		y += TIMER_GAP;
	}

	/** A resource, 0275: a bar in its colour, as the timed bars, its text (a barrier's health) at its right, its icon at its left. */
	private static void resourceBar(GuiGraphicsExtractor g, @Nullable Identifier icon, float share, @Nullable String text, int colour, float cx) {
		share = Math.clamp(share, 0, 1);
		float x = cx - TIMER_W / 2;
		fill(g, x - 1, y - 1, TIMER_W + 2, TIMER_H + 2, 0x80000000);
		fill(g, x, y, TIMER_W, TIMER_H, 0x40FFFFFF);
		fill(g, x, y, TIMER_W * share, TIMER_H, colour);
		if (text != null) {
			number(g, text, x + TIMER_W + 30, y + TIMER_H + 6, 18, WHITE);
		}
		if (icon != null) {
			float s = 26;
			image(g, icon, x - s - 9, y + TIMER_H / 2 - s / 2 + 1, s, s, 0x99000000);
			image(g, icon, x - s - 10, y + TIMER_H / 2 - s / 2, s, s, colour);
		}
		y += TIMER_GAP;
	}

	/** A count of what's left (Repair Pack's), as a row of short bars: full and empty (00F3 binds the count alone: the shape ours). */
	private static void pips(GuiGraphicsExtractor g, int have, int max, float cx, float top) {
		float pw = 22;
		float gap = 6;
		float x = cx - (max * pw + (max - 1) * gap) / 2;
		for (int i = 0; i < max; i++) {
			float px = x + i * (pw + gap);
			fill(g, px - 1, top - 1, pw + 2, 8, 0x80000000);
			fill(g, px, top, pw, 6, i < have ? 0xF2F7C43A : 0x33FFFFFF);
		}
	}

	/**
	 * A target's panel (0258, 052B, 0BA3, 0D81, 0257): their portrait in a square, the edge in {@code colour}, their name beside it,
	 * and a health bar under the portrait if {@code health} isn't negative. An ability's key before all that if it has one.
	 */
	private static void panel(GuiGraphicsExtractor g, @Nullable LivingEntity who, String name, String type, int colour, float health,
							  @Nullable Action key, float cx) {
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
		fill(g, x - 2, top - 2, size + 4, size + 4, alpha(0, 0.55f));
		fill(g, x, top, size, size, 0xFF1B2A4A);
		face(g, who, type, x + 2, top + 2, size - 4);
		outline(g, x - 1, top - 1, size + 2, size + 2, 2, colour);
		HudText.outlined(g, Face.BODY, name, x + size + 8, top + size / 2 + cap / 2, cap, WHITE, 0xB3000000, 1, Align.LEFT);
		if (health >= 0) {
			fill(g, x - 2, top + size + 3, size + 4, 5, 0x80000000);
			fill(g, x - 1, top + size + 4, (size + 2) * Math.clamp(health, 0, 1), 3, WHITE);
		}
		y = top + size + (health >= 0 ? 16 : 8);
	}

	/**
	 * A target's face: a hero's art for a player with a hero, a player's skin, a mob's face from Minecraft's textures (MobFaces), else
	 * a plain plus. The data's 1BDC2 (a non-hero's portrait) isn't in the local pack.
	 */
	private static void face(GuiGraphicsExtractor g, @Nullable LivingEntity who, String type, float x, float y, float size) {
		if (who instanceof Player player) {
			Hero hero = Hero.byId(player.getAttachedOrElse(HeroManager.HERO, ""));
			Identifier art = hero == null ? null : HeroArt.head(hero);
			if (art != null) {
				image(g, art, x, y, size, size, WHITE);
				return;
			}
			var connection = Minecraft.getInstance().getConnection();
			PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player.getUUID());
			if (info != null) {
				g.pose().pushMatrix();
				g.pose().translate(x, y);
				g.pose().scale(size / 8, size / 8);
				PlayerFaceExtractor.extractRenderState(g, info.getSkin(), 0, 0, 8, WHITE);
				g.pose().popMatrix();
				return;
			}
		}
		String id = who != null ? BuiltInRegistries.ENTITY_TYPE.getKey(who.getType()).toString() : type;
		MobFace mob = MobFaces.get(id);
		if (mob != null) {
			MobFaces.draw(g, mob, x, y, size, size, WHITE);
		} else {
			image(g, HudShapes.PLUS, x + size * 0.15f, y + size * 0.15f, size * 0.7f, size * 0.7f, WHITE);
		}
	}

	/**
	 * A button prompt, 00DB (00CF for a towed ally): "[KEY] WHAT" under the bars, and a second pair beside it (6441 / 6442, 6672 / 6673).
	 * What they say is ours: the data's texts are ids (.07C) not readable here. So each prompt names what the button does in the mod.
	 */
	private static void prompt(GuiGraphicsExtractor g, float cx, float cy, Action key, String text, @Nullable Action key2, @Nullable String text2) {
		y = Math.max(y, cy + PROMPT_BELOW);
		float cap = 12;
		String a = Component.translatable(text).getString().toUpperCase(Locale.ROOT);
		String b = text2 == null ? "" : Component.translatable(text2).getString().toUpperCase(Locale.ROOT);
		float widthA = keyWidth(key) + 6 + HudText.width(Face.BODY, a, cap);
		float widthB = key2 == null ? 0 : keyWidth(key2) + 6 + HudText.width(Face.BODY, b, cap);
		float x = cx - (widthA + (key2 == null ? 0 : 22 + widthB)) / 2;
		x = pair(g, key, a, x, y + 11, cap);
		if (key2 != null) {
			pair(g, key2, b, x + 22, y + 11, cap);
		}
		y += 28;
	}

	private static float keyWidth(Action key) {
		return Math.max(22, HudText.width(Face.SMALL, HeroHud.shortKey(HeroControls.get(key)), 10.5f) + 8);
	}

	private static float pair(GuiGraphicsExtractor g, Action key, String text, float x, float mid, float cap) {
		float kw = keyWidth(key);
		HeroHud.keyLabel(g, HeroControls.get(key), x + kw / 2, mid, 0, false);
		HudText.outlined(g, Face.BODY, text, x + kw + 6, mid + cap / 2, cap, WHITE, 0xB3000000, 1, Align.LEFT);
		return x + kw + 6 + HudText.width(Face.BODY, text, cap);
	}

	private static void number(GuiGraphicsExtractor g, String text, float x, float baseline, float cap, int colour) {
		HudText.outlined(g, Face.NUMBERS, text, x, baseline, cap, colour, 0xB3000000, 1.5f, Align.CENTER);
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
			// 0BA2 (59DC #43): her volley's five shots: the centre one below the aim, four paws in an arc above it (the wiki)
			case JETPACK_CAT -> {
				dot(g, cx, cy, 2.2f);
				dot(g, cx, cy + 10, 2.2f);
				for (int deg : new int[] {-54, -18, 18, 54}) {
					double a = Math.toRadians(deg);
					dot(g, cx + (float) Math.sin(a) * 15, cy - (float) Math.cos(a) * 15, 2.2f);
				}
			}
			// 0256 (0071 #443): the staff's beam locks round the aim; the blaster fires straight
			case MERCY -> {
				if ((s.mode() & dev.overcraft.mercy.Mercy.FLAG_BLASTER) != 0) {
					plus(g, cx, cy, 4, 6, 2);
				} else {
					ring(g, cx, cy, 14);
					dot(g, cx, cy, 1.6f);
				}
			}
			// 072D (21C6 #844): the blossom a ring; the thorns a tight "+"
			case LIFEWEAVER -> {
				if ((s.mode() & dev.overcraft.lifeweaver.Lifeweaver.FLAG_THORNS) != 0) {
					plus(g, cx, cy, 3, 5, 2);
				} else {
					ring(g, cx, cy, 11);
					dot(g, cx, cy, 1.6f);
				}
			}
			// 0987 (0C6B #34): the rifle unscoped: a ring and its dot
			case ANA -> {
				ring(g, cx, cy, 10);
				dot(g, cx, cy, 1.6f);
			}
			// 00A3 (018D #595): the machine pistol's spread
			case SOMBRA -> {
				ring(g, cx, cy, 16);
				dot(g, cx, cy, 1.6f);
			}
			// 00F2 (145B #51): a flail's reach: a dot and two side ticks
			case BRIGITTE -> {
				dot(g, cx, cy, 2);
				fill(g, cx - 19, cy - 2, 9, 4, 0x99000000);
				fill(g, cx + 10, cy - 2, 9, 4, 0x99000000);
				fill(g, cx - 18, cy - 1, 7, 2, WHITE);
				fill(g, cx + 11, cy - 1, 7, 2, WHITE);
			}
			// 091C (4441 #563), 027B (010C #311), 023F (his loadout 028C): a ring and its dot, each their own size
			case JUNO -> {
				ring(g, cx, cy, 9);
				plus(g, cx, cy, 11, 4, 2);
			}
			case LUCIO -> {
				ring(g, cx, cy, 12);
				dot(g, cx, cy, 1.6f);
			}
			case SIGMA -> {
				ring(g, cx, cy, 8);
				dot(g, cx, cy, 1.6f);
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
		image(g, HudShapes.DISC, cx - r - 1, cy - r - 1, 2 * r + 2, 2 * r + 2, 0x99000000);
		image(g, HudShapes.DISC, cx - r, cy - r, 2 * r, 2 * r, WHITE);
	}

	private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r) {
		HeroHud.arc(g, cx, cy, r, 3.5f, 0, 360, 0x99000000);
		HeroHud.arc(g, cx, cy, r, 1.8f, 0, 360, WHITE);
	}
}

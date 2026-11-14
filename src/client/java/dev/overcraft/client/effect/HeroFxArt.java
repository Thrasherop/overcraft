package dev.overcraft.client.effect;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Each hero's effect art made ahead of its first use (#128, from Sojourn's #126): a second after the hero is picked (or the world
 * loads with one), and for each hero another player in the world plays. Made on the art worker, a few textures a frame
 * ({@link EffectArt#prepare}); without it, each effect's first use read and coloured its masks in that frame (Kiriko's first
 * Ofuda 305 ms). The lists are each Fx class's ART: what the hero's effects asked for, from the client log's "made on first use"
 * lines (techniques/testing/runs/fx_prewarm_128).
 */
public final class HeroFxArt {
	/** Ticks a pick's own first draw is left alone: started with it, Sojourn's pick's longest frame went from 240 ms to 340 (#126). */
	private static final int WAIT_TICKS = 20;
	/** Dev only: OC_FX_AHEAD=0 turns it off, to measure the first use without it. */
	private static final boolean ON = !"0".equals(System.getenv("OC_FX_AHEAD"));
	private static int wait;
	/**
	 * What a pick itself shows (Jetpack Cat's jetpack flame and sparks, Lúcio's song aura, Sigma's hand cores): 217, 82 and 40 ms of the pick's own frames,
	 * from the log. Made as the world loads, under the select screen, since no wait after the pick comes before them.
	 */
	private static final List<EffectArt.Art> ON_PICK = List.of(
		new EffectArt.Art("cat_flame_tube#8", Palette.CAT_FLAME, false),
		new EffectArt.Art("cat_flame_tube#8", Palette.CAT_FLAME_GOLD, true),
		new EffectArt.Art("cat_flame_tube#8", Palette.CAT_FLAME_TEAL, false),
		new EffectArt.Art("cat_soft", Palette.CAT_FLAME, false),
		new EffectArt.Art("cat_blob#4", Palette.CAT_SPARK_GOLD, false),
		new EffectArt.Art("cat_blob#4", Palette.CAT_SPARK_PALE, false),
		new EffectArt.Art("cat_ring", Palette.CAT_FLAME, false),
		new EffectArt.Art("lucio_trail", Palette.HEAL_AMBER, false),
		new EffectArt.Art("lucio_spark", Palette.HEAL_AMBER, false),
		new EffectArt.Art("cat_glow_noise", Palette.SIG_HAND_EMISSION, false));
	private static boolean onPickMade;
	private static int generation = -1;
	private static final Set<Hero> MADE = EnumSet.noneOf(Hero.class);
	/** Made for another player's hero (Sojourn's adds her enemy colours). */
	private static final Set<Hero> MADE_OTHERS = EnumSet.noneOf(Hero.class);

	private HeroFxArt() {
	}

	/** A hero's art, as its allies (and the hero) see it. */
	static List<EffectArt.Art> of(Hero hero) {
		return switch (hero) {
			case KIRIKO -> KirikoFx.ART;
			case LUCIO -> LucioFx.ART;
			case JETPACK_CAT -> JetpackCatFx.ART;
			case MERCY -> MercyFx.ART;
			case LIFEWEAVER -> LifeweaverFx.ART;
			case ANA -> AnaFx.ART;
			case SOMBRA -> SombraFx.ART;
			case BRIGITTE -> BrigitteFx.ART;
			case JUNO -> JunoFx.ART;
			case SIGMA -> SigmaFx.ART;
			case SOJOURN -> SojournFx.ART;
			// D.Va, Pharah, Tracer, Cassidy, Junkrat: no effect art (none made on first use, every button)
			default -> List.of();
		};
	}

	/** The same in an enemy's colours, as far as the lists go: the _ENEMY palettes named literally, not those picked through a variable. */
	static List<EffectArt.Art> enemy(Hero hero) {
		return switch (hero) {
			case KIRIKO -> KirikoFx.ART_ENEMY;
			case JETPACK_CAT -> JetpackCatFx.ART_ENEMY;
			case MERCY -> MercyFx.ART_ENEMY;
			case LIFEWEAVER -> LifeweaverFx.ART_ENEMY;
			case SOMBRA -> SombraFx.ART_ENEMY;
			case BRIGITTE -> BrigitteFx.ART_ENEMY;
			case JUNO -> JunoFx.ART_ENEMY;
			case SIGMA -> SigmaFx.ART_ENEMY;
			case SOJOURN -> SojournFx.ART_ENEMY;
			default -> List.of();
		};
	}

	/** Each client tick. */
	public static void tick(Minecraft mc) {
		if (!ON || mc.level == null || mc.player == null) {
			wait = 0;
			return;
		}
		int now = EffectArt.generation();
		if (now != generation) {
			// resource packs reloaded: the textures are gone
			generation = now;
			MADE.clear();
			MADE_OTHERS.clear();
			onPickMade = false;
		}
		if (!onPickMade) {
			onPickMade = true;
			EffectArt.prepare(ON_PICK);
		}
		Hero own = ClientHero.hero();
		wait = own != null && !MADE.contains(own) ? wait + 1 : 0;
		if (wait >= WAIT_TICKS) {
			make(own, false);
		}
		if (mc.level.getGameTime() % 20 == 0) {
			for (AbstractClientPlayer other : mc.level.players()) {
				String id = other == mc.player ? null : other.getAttached(HeroManager.HERO);
				Hero hero = id == null ? null : Hero.byId(id);
				if (hero != null && !MADE_OTHERS.contains(hero)) {
					make(hero, true);
				}
			}
		}
	}

	private static void make(Hero hero, boolean other) {
		(other ? MADE_OTHERS : MADE).add(hero);
		// the worker skips what's made or on its way: the two sets' overlap costs nothing
		EffectArt.prepare(of(hero));
		if (other) {
			// another player's: an ally or an enemy, not known ahead (Sojourn's first shot seen cost 209 ms as an ally, 153 as an enemy, #126)
			EffectArt.prepare(enemy(hero));
		}
	}
}

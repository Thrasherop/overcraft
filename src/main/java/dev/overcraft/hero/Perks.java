package dev.overcraft.hero;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overcraft.kiriko.KirikoStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Overwatch's perk progression (#36), from Blizzard's notes of 2025-02-18 and the wiki's Perks page
 * (tools/reference/perks.md):
 * <ul>
 * <li>progress comes from damage dealt and healing given, a point per point, under the ult charge's rules (none during
 * the ultimate; the medic self-heal doesn't count); Overwatch gives none passively, but a Minecraft world has long
 * quiet stretches, so a point a second comes anyway (the owner, 2026-10-26: "1 point a second if nothing is being
 * done. But naturally, killing should be much much faster": a zombie's 250 points are four minutes of it);</li>
 * <li>its rate ramps up from 3 minutes into the match to double at 7;</li>
 * <li>level 2 offers a choice of two minor perks, level 3 of two major perks; a pick is for good;</li>
 * <li>a swap starts the new hero at level 1, and swapping back restores the old hero's level and perks.</li>
 * </ul>
 * A Minecraft world has no match: the "match" is the time played as the hero, and the progress is kept with the player,
 * per hero, across deaths and sessions (ours).
 */
public final class Perks {
	/** The ramp: 1x until 3 minutes in, 2x from 7 (the wiki); straight between them (ours: its shape isn't given). */
	public static final double RAMP_START = 180;
	public static final double RAMP_END = 420;
	public static final double RAMP_MAX = 2;
	/** Passive progress a second (the owner's, Overwatch has none), not ramped. */
	public static final double PASSIVE_PER_SECOND = 1;

	private Perks() {
	}

	/** One hero's perk progress, as saved: progress points, time played as the hero (ticks), the perks picked. */
	public record Progress(double xp, long ticks, String minor, String major) {
		public static final Progress NONE = new Progress(0, 0, "", "");
		public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.DOUBLE.fieldOf("xp").forGetter(Progress::xp),
			Codec.LONG.fieldOf("ticks").forGetter(Progress::ticks),
			Codec.STRING.fieldOf("minor").forGetter(Progress::minor),
			Codec.STRING.fieldOf("major").forGetter(Progress::major)
		).apply(i, Progress::new));
		public static final Codec<Map<String, Progress>> MAP = Codec.unboundedMap(Codec.STRING, CODEC);
	}

	/** The progress for level 2 (minor perk). */
	public static double minorXp(Hero hero) {
		return switch (hero) {
			case KIRIKO -> KirikoStats.PERK_MINOR_XP;
			case LUCIO -> dev.overcraft.lucio.LucioStats.PERK_MINOR_XP;
			default -> Double.MAX_VALUE;
		};
	}

	/** The progress for level 3 (major perk), counted from the start. */
	public static double majorXp(Hero hero) {
		return switch (hero) {
			case KIRIKO -> KirikoStats.PERK_MAJOR_XP;
			case LUCIO -> dev.overcraft.lucio.LucioStats.PERK_MAJOR_XP;
			default -> Double.MAX_VALUE;
		};
	}

	/** Progress for damage dealt or healing given (Overwatch points), at the ramp's current rate. */
	public static void add(Player player, double points) {
		HeroState state = HeroManager.state(player);
		if (state == null || state.hero == null || state.ultActive > 0 || points <= 0) {
			return;
		}
		state.perkXp = Math.min(majorXp(state.hero), state.perkXp + points * rate(state.perkTicks / 20.0));
	}

	/** Each tick of play: the passive point a second, up to the major perk's level. */
	public static void tick(HeroState state) {
		if (state.hero != null) {
			state.perkXp = Math.min(majorXp(state.hero), state.perkXp + PASSIVE_PER_SECOND / 20);
		}
	}

	/** The ramp's multiplier this many seconds into the "match". */
	public static double rate(double seconds) {
		double t = Math.clamp((seconds - RAMP_START) / (RAMP_END - RAMP_START), 0, 1);
		return 1 + (RAMP_MAX - 1) * t;
	}

	/** 1 to 3. */
	public static int level(HeroState state) {
		if (state.hero == null) {
			return 1;
		}
		return 1 + (state.perkXp >= minorXp(state.hero) ? 1 : 0) + (state.perkXp >= majorXp(state.hero) ? 1 : 0);
	}

	/** The tier waiting to be picked: 0 none, 1 minor, 2 major. The minor comes first when both are waiting. */
	public static int ready(HeroState state) {
		int level = level(state);
		if (level >= 2 && state.minorPerk == null) {
			return 1;
		}
		if (level >= 3 && state.majorPerk == null) {
			return 2;
		}
		return 0;
	}

	/** How far toward the next level, 0..1 (1 at level 3). */
	public static float progress(HeroState state) {
		if (state.hero == null) {
			return 0;
		}
		double minor = minorXp(state.hero);
		double major = majorXp(state.hero);
		if (state.perkXp < minor) {
			return (float) (state.perkXp / minor);
		}
		if (state.perkXp < major) {
			return (float) ((state.perkXp - minor) / (major - minor));
		}
		return 1;
	}

	/** A pick from the client: only a perk of the tier that's waiting, for the hero being played. */
	public static void pick(ServerPlayer player, @Nullable Perk perk) {
		HeroState state = HeroManager.state(player);
		if (perk == null || state == null || state.hero != perk.hero) {
			return;
		}
		int ready = ready(state);
		if (ready == 1 && perk.tier == Perk.Tier.MINOR) {
			state.minorPerk = perk;
		} else if (ready == 2 && perk.tier == Perk.Tier.MAJOR) {
			state.majorPerk = perk;
		} else {
			return;
		}
		HeroManager.savePerks(player, state);
	}

	/** The state's progress, to save. */
	public static Progress save(HeroState state) {
		return new Progress(state.perkXp, state.perkTicks, state.minorPerk == null ? "" : state.minorPerk.id,
			state.majorPerk == null ? "" : state.majorPerk.id);
	}

	/** Puts a hero's saved progress into the state (the state's hero must be set). */
	public static void load(HeroState state, @Nullable Progress saved) {
		if (state.hero == null || saved == null) {
			return;
		}
		state.perkXp = saved.xp;
		state.perkTicks = saved.ticks;
		state.minorPerk = byId(state.hero, saved.minor);
		state.majorPerk = byId(state.hero, saved.major);
	}

	private static @Nullable Perk byId(Hero hero, String id) {
		for (Perk perk : Perk.values()) {
			if (perk.hero == hero && perk.id.equals(id)) {
				return perk;
			}
		}
		return null;
	}

	/** A copy of a saved map with one hero's entry replaced. */
	public static Map<String, Progress> with(@Nullable Map<String, Progress> saved, Hero hero, Progress progress) {
		Map<String, Progress> map = saved == null ? new HashMap<>() : new HashMap<>(saved);
		map.put(hero.id, progress);
		return map;
	}
}

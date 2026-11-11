package dev.overcraft.potg;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Play of the Game's scoring (techniques/potg/design.md, "Scoring from what the mod knows"): the patent's method, US
 * 10,456,680 B2 (BZ H): each hero's events scored per category, a 12 s window (W H: the replay's length) slid over them
 * in 1 s steps, each hero and category's best window kept, and the category whose best window scores highest wins, High
 * Score falling to the others under a threshold. **Every weight here is ours**: no public source gives Blizzard's. Each
 * time a window sets a new best, the hero's ring is copied then (PotgTap.snapshot), so the play survives the ring's 25 s and
 * a death (whose keyframes are empty: step 1). Only heroes in hero mode score (owner, 2026-11-11: Minecraft-mode kills
 * don't count), and only while the tap is armed (a dragon fight).
 */
public final class PlayScore {
	private PlayScore() {
	}

	public enum Category {
		HIGH_SCORE("High Score"), LIFE_SAVER("Life Saver"), SHARPSHOOTER("Sharpshooter"), SHUTDOWN("Shutdown");

		public final String title;

		Category(String title) {
			this.title = title;
		}
	}

	/** Seconds in a window (W: the 12 s replay). */
	public static final int WINDOW = 12;
	// ---- weights, all ours ----
	/** A kill is worth the victim's health in Overwatch points, capped. */
	static final double KILL_CAP = 1000;
	/** The Ender Dragon's kill on top (ours: #44, "the last hits count"). */
	static final double DRAGON_KILL_BONUS = 1500;
	/** An ult's start (ours), scaled down as the patent allows ("may be reduced by a set amount or share"). */
	static final double ULT_START = 200;
	static final double ULT_SHARE = 0.75;
	/** A critical hit's damage again, more with distance: 1 + distance / 15 m (ours). */
	static final double SHARP_PER_METRE = 1 / 15.0;
	/** Stopping a lit creeper, or a hero in their ult (ours: the note's "was dangerous" rule for mobs). */
	static final double SHUTDOWN_KILL = 500;
	/** High Score under this gives way to the best of the others, if any scored (the patent's option; the number ours). */
	static final double HIGH_SCORE_FLOOR = 300;

	/** One hero's rows: this second's points, the last WINDOW seconds, the best window and its copy of the ring. */
	static final class Rows {
		final double[] now = new double[4];
		final double[][] seconds = new double[4][WINDOW];
		int head;
		final double[] best = new double[4];
		final PotgTap.Dump[] pinned = new PotgTap.Dump[4];
		final String[] heroAt = new String[4];
		String name = "";
		double lastUlt;
		/** The server tick each best was pinned at, and when a death asks for the pins to be taken again. */
		final int[] pinnedAt = new int[4];
		int diedAt = -1;
	}

	private static final Map<UUID, Rows> ROWS = new HashMap<>();
	private static boolean open;
	private static long nanos;
	private static long ticks;
	private static long maxTickNanos;
	private static long tickNanos;

	/** Scoring runs while the tap is armed, until the dragon's death closes it ("Scoring stops", the note's timeline). */
	public static void open() {
		ROWS.clear();
		open = true;
		nanos = ticks = maxTickNanos = tickNanos = 0;
	}

	/** The fight is over: the second under way is counted (the dragon's kill is in it), and nothing more. */
	public static void close(MinecraftServer server) {
		if (open) {
			for (Map.Entry<UUID, Rows> e : ROWS.entrySet()) {
				step(server, e.getKey(), e.getValue());
			}
		}
		open = false;
	}

	public static void clear() {
		ROWS.clear();
		open = false;
	}

	private static @Nullable Rows rows(@Nullable Entity who) {
		if (!open || !(who instanceof ServerPlayer player) || !PotgTap.armed()) {
			return null;
		}
		if (HeroManager.hero(player) == null || !HeroManager.heroMode(player)) {
			return null;
		}
		Rows r = ROWS.computeIfAbsent(player.getUUID(), k -> new Rows());
		r.name = player.getName().getString();
		return r;
	}

	/** Combat.deal: a hero's damage landed (Overwatch HP). */
	public static void damage(@Nullable Entity attacker, LivingEntity target, double dealt, Blow blow) {
		long t0 = System.nanoTime();
		Rows r = rows(attacker);
		if (r != null && dealt > 0 && target != attacker && !Allies.isBystander((LivingEntity) attacker, target)) {
			r.now[Category.HIGH_SCORE.ordinal()] += dealt;
			if (blow.crit()) {
				r.now[Category.SHARPSHOOTER.ordinal()] += dealt * (1 + attacker.distanceTo(target) * SHARP_PER_METRE);
			}
		}
		tickNanos += System.nanoTime() - t0;
	}

	/** KillFeed.onDeath: a kill, with the hero who made it. */
	public static void kill(@Nullable Entity killer, LivingEntity victim) {
		long t0 = System.nanoTime();
		Rows r = rows(killer);
		if (r != null && killer != victim && !Allies.isBystander((LivingEntity) killer, victim)) {
			double worth = Math.min(KILL_CAP, Ow.toOw(victim.getMaxHealth()));
			if (victim instanceof EnderDragon) {
				worth += DRAGON_KILL_BONUS;
			}
			r.now[Category.HIGH_SCORE.ordinal()] += worth;
			if (dangerous(victim)) {
				r.now[Category.SHUTDOWN.ordinal()] += SHUTDOWN_KILL;
			}
		}
		tickNanos += System.nanoTime() - t0;
	}

	private static boolean dangerous(LivingEntity victim) {
		if (victim instanceof Creeper creeper) {
			return creeper.getSwellDir() > 0 || creeper.isIgnited();
		}
		return victim instanceof ServerPlayer p && HeroManager.state(p) instanceof HeroState s && s.ultActive > 0;
	}

	/** Combat.addFireForHealing: a hero healed someone (Overwatch HP). */
	public static void heal(Player hero, double healed) {
		long t0 = System.nanoTime();
		Rows r = rows(hero);
		if (r != null && healed > 0) {
			r.now[Category.LIFE_SAVER.ordinal()] += healed;
		}
		tickNanos += System.nanoTime() - t0;
	}

	/**
	 * END_SERVER_TICK: an ult's start (the charge spent: the one place that sees every kit's, since the kits' own
	 * afterUlt calls take no player), and each second the window's step.
	 */
	public static void tick(MinecraftServer server) {
		if (!open) {
			return;
		}
		long t0 = System.nanoTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Rows r = rows(player);
			HeroState s = HeroManager.state(player);
			if (r == null || s == null || s.hero == null) {
				continue;
			}
			double cost = Combat.ultCost(s);
			if (r.lastUlt >= cost && s.ultCharge < r.lastUlt - cost * 0.5) {
				r.now[Category.HIGH_SCORE.ordinal()] += ULT_START * ULT_SHARE;
			}
			r.lastUlt = s.ultCharge;
		}
		for (Map.Entry<UUID, Rows> e : ROWS.entrySet()) {
			repinAfterDeath(server, e.getKey(), e.getValue());
		}
		if (server.getTickCount() % 20 == 0) {
			for (Map.Entry<UUID, Rows> e : ROWS.entrySet()) {
				step(server, e.getKey(), e.getValue());
			}
		}
		long d = System.nanoTime() - t0 + tickNanos;
		tickNanos = 0;
		nanos += d;
		ticks++;
		maxTickNanos = Math.max(maxTickNanos, d);
	}

	private static void step(MinecraftServer server, UUID id, Rows r) {
		for (int c = 0; c < 4; c++) {
			r.seconds[c][r.head] = r.now[c];
			r.now[c] = 0;
		}
		r.head = (r.head + 1) % WINDOW;
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		for (int c = 0; c < 4; c++) {
			double sum = windowSum(r.seconds[c]);
			if (sum > r.best[c] && player != null) {
				PotgTap.Dump dump = PotgTap.snapshot(player);
				if (dump != null) {
					r.best[c] = sum;
					r.pinned[c] = dump;
					r.pinnedAt[c] = server.getTickCount();
					Hero hero = HeroManager.hero(player);
					r.heroAt[c] = hero == null ? "" : hero.id;
					Overcraft.LOG.info(String.format(Locale.ROOT, "[POTG] %s: new best %s %.0f (pinned %d KB)", r.name,
						Category.values()[c].title, sum, size(dump) / 1024));
				}
			}
		}
	}

	private static int size(PotgTap.Dump d) {
		int n = 0;
		for (byte[] b : d.keyframes()) {
			n += b.length;
		}
		for (byte[] b : d.streams()) {
			n += b.length;
		}
		return n;
	}

	/** How long after a best window a death still belongs to it, and how long after the death its pin is taken again (ours). */
	static final int DEATH_REACH = 60;
	static final int DEATH_TAIL = 20;

	/**
	 * A hero died (AFTER_DEATH). A play that ended just before it is taken again a second after it, so the kept play shows
	 * the death, as Overwatch's often do; its first seconds give way. The copy is made from the ring as it is then: its
	 * keyframes from before the death are whole (a dead hero's are empty: step 1).
	 */
	public static void died(ServerPlayer player) {
		Rows r = ROWS.get(player.getUUID());
		if (r != null && open) {
			r.diedAt = player.level().getServer().getTickCount();
		}
	}

	private static void repinAfterDeath(MinecraftServer server, UUID id, Rows r) {
		int now = server.getTickCount();
		if (r.diedAt < 0 || now < r.diedAt + DEATH_TAIL) {
			return;
		}
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		for (int c = 0; c < 4; c++) {
			if (r.pinned[c] != null && r.pinnedAt[c] >= r.diedAt - DEATH_REACH && player != null) {
				PotgTap.Dump dump = PotgTap.snapshot(player);
				if (dump != null) {
					r.pinned[c] = dump;
					Overcraft.LOG.info("[POTG] {}: {} taken again to show the death ({} ticks after the best)", r.name,
						Category.values()[c].title, r.diedAt - r.pinnedAt[c]);
				}
			}
		}
		r.diedAt = -1;
	}

	/** The sum of a category's seconds in the window. */
	static double windowSum(double[] seconds) {
		double s = 0;
		for (double v : seconds) {
			s += v;
		}
		return s;
	}

	/**
	 * Which category wins from the bests (the patent's rule): the highest, unless it's High Score under the floor and
	 * another scored. -1 when nothing scored.
	 */
	static int pick(double[] bests) {
		int top = -1;
		for (int c = 0; c < bests.length; c++) {
			if (bests[c] > 0 && (top < 0 || bests[c] > bests[top])) {
				top = c;
			}
		}
		if (top == Category.HIGH_SCORE.ordinal() && bests[top] < HIGH_SCORE_FLOOR) {
			int other = -1;
			for (int c = 0; c < bests.length; c++) {
				if (c != top && bests[c] > 0 && (other < 0 || bests[c] > bests[other])) {
					other = c;
				}
			}
			if (other >= 0) {
				return other;
			}
		}
		return top;
	}

	/** The play chosen: whose, which category, its score and its copy of the ring. */
	public record Choice(String player, String hero, Category category, double score, PotgTap.Dump dump, String table) {
	}

	/** Across every hero: each one's winning category, then the highest of those. Null when no hero has a play. */
	public static @Nullable Choice choose() {
		Choice best = null;
		StringBuilder table = new StringBuilder();
		for (Rows r : ROWS.values()) {
			table.append(String.format(Locale.ROOT, " %s[HS %.0f, LS %.0f, SS %.0f, SD %.0f]", r.name, r.best[0], r.best[1], r.best[2], r.best[3]));
			int c = pick(r.best);
			if (c < 0 || r.pinned[c] == null) {
				continue;
			}
			if (best == null || r.best[c] > best.score()) {
				best = new Choice(r.name, r.heroAt[c], Category.values()[c], r.best[c], r.pinned[c], "");
			}
		}
		return best == null ? null : new Choice(best.player(), best.hero(), best.category(), best.score(), best.dump(), table.toString());
	}

	public static String costReport() {
		return String.format(Locale.ROOT, "scoring ms/tick: mean %.4f max %.3f over %d ticks", ticks == 0 ? 0 : nanos / 1e6 / ticks,
			maxTickNanos / 1e6, ticks);
	}
}

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
 * in 1 s steps; each window's category is the patent's pick (the highest, High Score falling to the others under a
 * threshold), and each player's 5 best windows are kept as their highlights (Highlights: owner, 2026-11-11, "the top 5
 * highlight scores from each player"), with their place in the player's recording (PotgRecorder). **Every weight here is
 * ours**: no public source gives Blizzard's. Only heroes in hero mode score (owner, 2026-11-11: Minecraft-mode kills
 * don't count), the whole run, in any dimension (owner, 2026-11-11: "Do the whole time").
 */
public final class PlayScore {
	private PlayScore() {
	}

	public enum Category {
		// the names as the game has them (D: strings C27F "High Score", 405D LIFESAVER, 38F4 SHARPSHOOTER, 362B SHUTDOWN)
		HIGH_SCORE("High Score"), LIFE_SAVER("Lifesaver"), SHARPSHOOTER("Sharpshooter"), SHUTDOWN("Shutdown");

		public final String title;

		Category(String title) {
			this.title = title;
		}
	}

	/** Seconds in a window: PotgValues (W: the 12 s replay). */
	public static final int WINDOW = PotgValues.WINDOW_SECONDS;
	static final double HIGH_SCORE_FLOOR = PotgValues.HIGH_SCORE_FLOOR;

	/** One hero's rows: this second's points and the last WINDOW seconds. */
	static final class Rows {
		final double[] now = new double[4];
		final double[][] seconds = new double[4][WINDOW];
		int head;
		String name = "";
		double lastUlt;
		/** The server tick of a death whose kept window is taken again a second later. */
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
		if (!open || !(who instanceof ServerPlayer player)) {
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
			r.now[Category.HIGH_SCORE.ordinal()] += dealt * PotgValues.DAMAGE_PER_HP;
			PlayerStats.damage((ServerPlayer) attacker, dealt);
			if (blow.crit()) {
				r.now[Category.SHARPSHOOTER.ordinal()] += dealt * (1 + attacker.distanceTo(target) / PotgValues.SHARP_METRES);
			}
		}
		tickNanos += System.nanoTime() - t0;
	}

	/** KillFeed.onDeath: a kill, with the hero who made it. */
	public static void kill(@Nullable Entity killer, LivingEntity victim) {
		long t0 = System.nanoTime();
		Rows r = rows(killer);
		if (r != null && killer != victim && !Allies.isBystander((LivingEntity) killer, victim)) {
			double worth = Math.min(PotgValues.KILL_CAP, Ow.toOw(victim.getMaxHealth()))
				+ PotgValues.KILL_BONUS.getOrDefault(net.minecraft.world.entity.EntityType.getKey(victim.getType()).toString(), 0.0);
			r.now[Category.HIGH_SCORE.ordinal()] += worth;
			PlayerStats.kill((ServerPlayer) killer);
			if (dangerous(victim)) {
				r.now[Category.SHUTDOWN.ordinal()] += PotgValues.SHUTDOWN_KILL;
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

	/** Player.take: a hero picked up an item; worth what PotgValues.PICKUP says (nothing yet: the owner's call). */
	public static void pickup(Player hero, net.minecraft.world.item.ItemStack stack) {
		Double worth = PotgValues.PICKUP.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		Rows r = worth == null ? null : rows(hero);
		if (r != null) {
			r.now[Category.HIGH_SCORE.ordinal()] += worth * stack.getCount();
		}
	}

	/** Combat.addFireForHealing: a hero healed someone (Overwatch HP). */
	public static void heal(Player hero, double healed) {
		long t0 = System.nanoTime();
		Rows r = rows(hero);
		if (r != null && healed > 0) {
			r.now[Category.LIFE_SAVER.ordinal()] += healed * PotgValues.HEAL_PER_HP;
			PlayerStats.healing((ServerPlayer) hero, healed);
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
				r.now[Category.HIGH_SCORE.ordinal()] += PotgValues.ULT_START * PotgValues.ULT_SHARE;
			}
			r.lastUlt = s.ultCharge;
		}
		for (Map.Entry<UUID, Rows> e : ROWS.entrySet()) {
			repinAfterDeath(server, e.getKey(), e.getValue());
		}
		if (server.getTickCount() % 20 == 0) {
			MobStats.flush(server);
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
		double[] sums = new double[4];
		for (int c = 0; c < 4; c++) {
			r.seconds[c][r.head] = r.now[c];
			r.now[c] = 0;
		}
		r.head = (r.head + 1) % WINDOW;
		for (int c = 0; c < 4; c++) {
			sums[c] = windowSum(r.seconds[c]);
		}
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		int c = pick(sums);
		if (c < 0 || player == null) {
			return;
		}
		Hero hero = HeroManager.hero(player);
		Highlights.Highlight kept = Highlights.offer(player, Category.values()[c], sums[c], hero == null ? "" : hero.id);
		if (kept != null) {
			Overcraft.LOG.info(String.format(Locale.ROOT, "[POTG] %s: highlight %s %.0f (%s)", r.name, kept.category, kept.score, kept.segment));
		}
	}

	/** How long after a best window a death still belongs to it, and how long after the death its pin is taken again (ours). */
	static final int DEATH_REACH = PotgValues.DEATH_REACH_TICKS;
	static final int DEATH_TAIL = PotgValues.DEATH_TAIL_TICKS;

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
		if (player != null) {
			Highlights.extend(player, DEATH_REACH + DEATH_TAIL);
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

	/** The play chosen: whose, which category, its score, the highlight (its place in a recording) and where. */
	public record Choice(String player, UUID owner, String hero, String category, double score, Highlights.Highlight highlight,
						 String table) {
	}

	/** The best highlight of anyone's made since {@code sinceMs} (owner: the POTG is the best across everyone). */
	public static @Nullable Choice choose(MinecraftServer server, long sinceMs) {
		Highlights.Highlight best = Highlights.best(server, sinceMs);
		if (best == null || Highlights.bestOwner == null) {
			return null;
		}
		return new Choice(best.player, Highlights.bestOwner, best.hero, best.category, best.score, best, Highlights.table(server));
	}

	public static String costReport() {
		return String.format(Locale.ROOT, "scoring ms/tick: mean %.4f max %.3f over %d ticks", ticks == 0 ? 0 : nanos / 1e6 / ticks,
			maxTickNanos / 1e6, ticks);
	}
}

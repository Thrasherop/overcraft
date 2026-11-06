package dev.overcraft.hero;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev only (the harness's {@code presstrace}, #126): from a key's press to what came of it, one clock for client and server, so a
 * "crisp" report becomes milliseconds. Costs nothing unless a run turned it on. In a real game nothing turns it on.
 */
public final class PressTrace {
	private static volatile boolean on;
	private static volatile boolean wantFrame;
	private static volatile boolean allFrames;
	private static long t0;
	private static final List<String> MARKS = new ArrayList<>();

	private PressTrace() {
	}

	public static synchronized void start() {
		MARKS.clear();
		t0 = System.nanoTime();
		on = true;
		wantFrame = false;
	}

	public static synchronized String dump() {
		String out = String.join(" | ", MARKS);
		MARKS.clear();
		on = false;
		wantFrame = false;
		return out;
	}

	public static void mark(String what) {
		if (on) {
			add(what);
		}
	}

	/** Something the player sees was made: the next frame drawn is when it shows. */
	public static void seen(String what) {
		if (on) {
			add(what);
			wantFrame = true;
		}
	}

	/** Each frame drawn. */
	public static void frame() {
		if (on && wantFrame) {
			wantFrame = false;
			add("frame");
		} else if (on && allFrames) {
			add("f");
		}
	}

	/** Every frame marked as "f" as well, to see a hitch between them. */
	public static void allFrames(boolean all) {
		allFrames = all;
	}

	private static synchronized void add(String what) {
		MARKS.add(String.format(java.util.Locale.ROOT, "%.1f %s", (System.nanoTime() - t0) / 1.0e6, what));
	}
}
